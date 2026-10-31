# Errores encontrados y correcciones

## E1 · El motor de reglas ocultaba sesiones por encima del techo (corregido)

| | |
|---|---|
| **Evidencia** | U07, ensayo seco: la sesión declara 17 min y su contenido dura 21 (estimador de V17). Reproducido sin el arreglo en `PerfilesLimiteIT.quinceMinutosNuncaPorEncimaDelTecho`: LOSE_WEIGHT de 15 min **publicaba** una sesión real de 19 min con techo 17,25 |
| **Causa raíz** | `RuleBasedTrainingPlanGenerator.withEstimatedDurations` declaraba `min(techo, estimado)`. V4 compara el techo con la duración **declarada**, y V17 tolera un 20 %, así que una sesión hasta un 25 % por encima del techo se publicaba como válida |
| **Tipo** | Defecto del backend (no de política: V4 no cambia) |
| **Corrección** | Se declara la duración del contenido, sin recortarla |
| **Regresión** | `PerfilesLimiteIT` (4 objetivos a 15 min): o hay plan y toda sesión dura de verdad ≤ techo, o no hay plan |
| **Impacto** | Las 40 IT de generación (72 referencias, barridos y 4D) **sin cambios**. Solo cambian los casos que de verdad se pasaban: LOSE_WEIGHT y GENERAL_FITNESS a 15 min dejan de publicar |
| **Estado** | Corregido (`06f33a9`) |

## E2 · La regeneración tras un NO_FACTIBLE por dolor podía volver a prescribir lo que dolió (corregido)

| | |
|---|---|
| **Evidencia** | U06 S5 y U08 S2 en el ensayo seco: dolor, NO_FACTIBLE y una semana sin plan; el ciclo siguiente genera desde cero |
| **Causa raíz** | La generación inicial no consulta el historial de dolor. Solo lo hacen la adaptación (V8C) y la progresión, sobre la semana medida, y aquí estaba vacía. Que no volviera era cuestión de azar |
| **Tipo** | Defecto de seguridad (la política ya prohíbe republicar el ejercicio que dolió) |
| **Corrección** | `prepare()` excluye del conjunto elegible los ejercicios del dolor que dejó el intento NO_FACTIBLE inmediatamente anterior. Si el dolor no se identificó, todos los de la sesión, como hace V8C |
| **Regresión** | `AdaptacionCompletaIT.dolorSinIa`: contexto y semana regenerada sin los 6 ejercicios. Sin el arreglo, el contexto los ofrecía |
| **Comprobación real** | U06: `DOLOR_SIN_RESOLVER ... se genera sin [151]`; S7 no lleva 151 |
| **Estado** | Corregido (`d9853d9`). **Pendiente de decisión:** cuánto tiempo se recuerda un dolor más allá de este caso |

## E3 · El simulador (solo pruebas)

| Problema | Corrección |
|---|---|
| Sin presupuesto, el usuario se detenía (PENDIENTE_PRESUPUESTO) y se perdían sus semanas | En estrés, la operación sigue sin llamar (SIN_PRESUPUESTO), con la contabilidad aparte |
| Las generaciones competían con adaptaciones tempranas por el presupuesto | Barrera: las 8 generaciones primero. La reserva espera mientras otras tengan reservas en curso |
| Tras una semana sin plan, el simulador detenía al usuario | Como en producción, el ciclo siguiente regenera |
| Sin medición de las fases de la generación | Espías de solo lectura: `prepare`, `generate`, `validar`, `normalizar` y `persist` |

## Lo que no es un defecto

| Caso | Por qué no lo es |
|---|---|
| **U07** (GENERAL_FITNESS, 15 min) sin plan | Inviable con las políticas actuales: conflicto de políticas, no de implementación |
| **U02 S5** en el 80,00 % exacto, sin adaptación | Comportamiento correcto del umbral: se adapta **por debajo** del 80 % |
| **U01** sin reincorporación en 10 semanas | La ruta exige unos 10 ciclos después de la retirada |
