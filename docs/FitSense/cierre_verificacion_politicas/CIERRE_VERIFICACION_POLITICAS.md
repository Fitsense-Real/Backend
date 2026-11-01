# Cierre de la verificación de políticas

**Rama:** `feature/modelo-e-planificacion`, sobre `generation` (`afef98a`, PR #16 fusionado).

**Estado sin cambios**
- P-01 30–75; V4 +15 %; V20 70 %.
- V24 y las dosis 4D; P-02, P-03, D8 y C-2.
- Progresión y reincorporación.
- V43 y las migraciones históricas.
- El modelo E sigue **desactivado**.

**No se ha activado, fusionado ni desplegado nada.**

## Resumen ejecutivo

1. **C2 regenerado (C2-A, C2-B, C2-C) recupera planes empeorándolos.** El motor rellena hasta el piso: si el piso baja, toda la semana pierde contenido (R0800: de 138 a 120 series). C2-C da **942**, no 950.
2. **C2-D (mejor esfuerzo saturado) es la alternativa más defendible.**
   - **954 válidos.**
   - GAIN_MUSCLE a 75 min: **60/60**.
   - **0 planes alterados** (960 de 960 idénticos a C1).
   - No inventa porcentajes: el límite lo pone el contenido.
3. **No hay otra composición válida al 70 %** (C2-E: 0 de 79). Las semanas rechazadas ya estaban saturadas por catálogo, V18, V22, V15 y V24.
4. **V4 tiene dos defectos:** dos redondeos distintos, y que compara lo **declarado**. Un borrador de IA puede declarar 35 min para un contenido del 142 % de M. Se recomienda V4-C (segundos estrictos), versionada.
5. **El cardio de LOSE_WEIGHT** se programa en 13–160 min de bloque por semana (media 57). Falta lo que la persona hace fuera de la app y la intensidad. **Medir antes de cambiar la dosis.**
6. **Ningún texto científico completo fue accesible.** Las 42 clases se mantienen y 11 afirmaciones se matizan.
7. **No ha habido revisión profesional.** El instrumento y la muestra ciega están listos.

## Entregables

`docs/FitSense/cierre_verificacion_politicas/`

| # | Archivo | Contenido |
|---:|---|---|
| 1 | `VERIFICACION_C2_POR_OBJETIVO.md` | Porcentajes, reproducción y los 8 discrepantes |
| 2 | `MATRIZ_C2_VERIFICADA.csv` | 960 × 6 caminos |
| 3 | `CALIDAD_PLANES_RECUPERADOS.md` | Contenido de los planes recuperados |
| 4 | `AUDITORIA_V4_REDONDEOS.md` | Redondeos de V4 |
| 5 | `REGRESIONES_V4.md` | Pruebas de V4 |
| 6 | `REVISION_CIENTIFICA_ACTUALIZADA.md` | Revisión científica |
| 7 | `TRAZABILIDAD_CIENTIFICA_ACTUALIZADA.csv` | Trazabilidad de las 42 reglas |
| 8 | `AUDITORIA_CARDIO_LOSE_WEIGHT.md` | Cardio y fuerza en LOSE_WEIGHT |
| 9 | `PROPUESTA_METRICAS_AEROBICAS.md` | Métrica aeróbica |
| 10 | `INSTRUMENTO_REVISION_PROFESIONAL.md` | Instrumento de revisión |
| 11 | `VERSIONADO_POLITICAS_PROPUESTA.md` | Versionado |
| 12 | `SIMULACIONES_Y_REGRESIONES_CIERRE.md` | Simulaciones y regresiones |
| 13 | `DECISION_FINAL_C2_V4_CARDIO.md` | Decisiones |
| 14 | `CIERRE_VERIFICACION_POLITICAS.md` | Este documento |

**Anexos de datos**
- `PLANES_RECUPERADOS_DETALLE.csv`
- `REFERENCIA_MENOR_TIEMPO.csv`
- `DIAGNOSTICO_DISCREPANTES.csv`
- `LOSE_WEIGHT_FUERZA_Y_CARDIO.csv`
- `LONGITUDINAL_VERIFICACION.csv`
- `MUESTRA_REVISION_PROFESIONAL.csv`
- `CLAVE_MUESTRA_NO_ENTREGAR_AL_EVALUADOR.csv`
- `REFERENCIAS_CORRECCIONES.md`

**Código (experimental y desactivado)**
- `PisoV20Acotado`
- `TechoV4Versionado`
- `VersionPoliticaPlanificacion`
- `MetricaAerobica`
- `VerificacionPoliticasIT` (opt-in)
- `PoliticasCandidatasTest`
- `V4DuracionDeclaradaTest`

No se ha sobrescrito ningún archivo de `auditoria_cientifica_entrenamiento/` ni ninguna evidencia histórica.

## Las 30 preguntas

### 1. ¿Cuántos perfiles acepta C2-A?

**950 de 960.** Reproduce exactamente el laboratorio anterior.

### 2. ¿Cuántos acepta C2-B?

**950.** Nadie necesitaba el 50 %.

### 3. ¿Cuántos acepta C2-C?

**942.** Pierde los 8 discrepantes.

### 4. ¿Cuántos acepta C2-D?

**954.**
- C2-D no usa porcentajes: deja de bloquear solo las sesiones saturadas.
- No hay evidencia para fijar mínimos distintos por objetivo o nivel.

### 5. ¿Qué casos siguen sin plan?

| Variante | Sin plan |
|---|---|
| C2-D | Los 6 de LOSE_WEIGHT de 1 día y 45 min, por **V15** (no es V20) |
| C2-C | Esos 6, más los 8 discrepantes y 4 GAIN_MUSCLE con peso corporal, 6–7 días y 75 min (sesión PULL al 46 %) |
| C2-A / C2-B | Los 6 de V15 y los 4 de GAIN_MUSCLE |

### 6. ¿Por qué los ocho discrepantes necesitan pisos menores?

- **4 de INCREASE_STRENGTH** (peso corporal, 6–7 días): el catálogo de tracción sin material se agota. Lo que queda del enfoque son tríceps, que V22 no admite en PULL, y todo ya está a 3 series. PULL queda al 58,5 %.
- **4 de LOSE_WEIGHT** (principiante, casa, 75 min): en tren inferior, otro ejercicio de pierna rompe V15 (la mitad), y hay pocos ejercicios de principiante con dosis admisible. Queda al 60,1 %.

### 7. ¿Pueden recuperarse con otra composición sin reducir más V20?

**No, dentro de las reglas vigentes.** C2-E y el diagnóstico probaron todos los candidatos y todas las series: 0 aceptables. La búsqueda es voraz, no exhaustiva.

### 8. ¿Los planes recuperados contienen una dosis razonable?

**Las dosis son las aprobadas en 4D** (3×10/90, 3×13, 3×6/150, 3×8/120, cardio 3×90 s).
- Con C2-D, cada plan es el mismo que el motor construye al 70 %.
- **La valoración de suficiencia queda pendiente de la revisión profesional.** Hay preguntas abiertas: los ejercicios de «espalda» del PULL sin material y el volumen elevado de fuerza.

### 9. ¿Algún plan recuperado presenta volumen excesivo?

**Señalado, no juzgado.** INCREASE_STRENGTH con peso corporal y 6–7 días tiene:
- 138 series por semana, 42 de pierna;
- **21 series de un grupo en una sesión**.

El mismo patrón aparece en 120 planes vigentes válidos, de modo que **no lo introduce C2**. El techo por sesión solo tiene un preprint [S03]. Va a la revisión profesional.

### 10. ¿Qué duración real tienen los entrenamientos de GAIN_MUSCLE a 75 min?

**42,5 min (2.550 s; 56,7 %).** Es el mismo plan que con 60 min. Con peso corporal y 6–7 días, PULL baja al 46 % (34,5 min).

### 11. ¿Cuál es la alternativa V20 más defendible?

**C2-D**, con la duración estimada visible y versionada.

### 12. ¿Qué ocurre si conservamos V20 al 70 %?

- 79 perfiles válidos por P-01 se quedan sin plan, entre ellos todos los GAIN_MUSCLE a 75 min.
- En catálogos pequeños, una semana con dolor puede dejar a la persona sin plan durante semanas (L5 y L6 del longitudinal, de S4 a S10).

### 13. ¿Cuál debe ser la interpretación futura de V4?

**V4-C:** `segundos_estimados ≤ M × 60 × 1,15`, sin redondear y con el mismo límite en el generador. Versionada y no retroactiva.

### 14. ¿Qué diferencia hay entre truncar minutos y comparar segundos?

| M | V4-B (truncar) | V4-C (segundos) |
|---:|---|---|
| 30 | Rechaza 2.070 s (exactamente el 115 %) | Admite hasta 2.070 s |
| 45 | Rechaza 3.090–3.105 s | Admite hasta 3.105 s |
| 60 | Admite hasta 4.169 s (115,8 %) | Admite hasta 4.140 s |
| 75 | Admite hasta 5.189 s (115,3 %) | Admite hasta 5.175 s |

V4-C es la única exacta. V4-B rechazaría 6 planes vigentes válidos de GENERAL_FITNESS a 30 min; V4-C, ninguno.

### 15. ¿Qué planes históricos podrían verse afectados?

- **Campaña:** 0 de 456 sesiones con ambas versiones.
- **Potencialmente:**
  - con V4-B, las sesiones del motor justo en el 115 %;
  - con V4-C, los borradores de IA que declaren menos de lo que estiman.
- **No se reinterpretan:** un plan sin versión se valida como histórico.

### 16. ¿Qué evidencia científica adicional se pudo verificar?

**Ningún texto completo.** WebFetch falló en los 8 hosts probados.

Solo se verificaron fragmentos de resumen y metadatos:
- el PMID de ACSM 2026;
- los autores de S04, S05, S07, S38 y S48;
- el DOI de S08 y de S48;
- el resumen de ACSM 2024 (≥ 150 min, dosis-respuesta);
- la OMS (todos los grandes grupos ≥ 2 d/semana);
- Singer 2024 (sin diferencias apreciables con más de 90 s);
- Halperin 2022 (error de unas 0,95 repeticiones al estimar el RIR).

### 17. ¿Qué reglas de 4A, 4C y 4D conservan su clasificación?

**Todas (42 de 42).**
- **Reforzadas:** LW2 (E), IS5 y GF6 (B), GM1 (B) e IS7 (D).
- **Matizadas:** IS2, T5, LW1 y T15.

### 18. ¿Qué afirmaciones anteriores deben corregirse o matizarse?

1. ACSM 2009 sobre descansos: tres redacciones («≥ 2–3 min», «≥ 3 min», «3–5 min»). 150 s solo cumple la primera.
2. «≥ 10 series/semana» del ACSM 2026: la infografía oficial dice «~10», que no es un mínimo.
3. El talk test tiene validez limitada en la zona alta.
4. La equivalencia «vigoroso = 2 × moderado» es una inferencia de los rangos de la OMS.
5. LW2 es E solo si el cardio de la app es la única actividad aeróbica.
6. «67 estudios / 2.058 participantes» de Pelland: sin verificar, no citar.
7. S19 es una revisión narrativa.
8. La frecuencia del ACSM 2009 aparece con versiones discrepantes.

(Lista completa en `REVISION_CIENTIFICA_ACTUALIZADA.md` §6.)

### 19. ¿Cómo debemos contabilizar el cardio de LOSE_WEIGHT?

Separar:
- **trabajo programado** (series × segundos);
- **bloque programado** (más pausas);
- **ejecutado**;
- **externo**, solo si se registra.

Equivalentes **solo con intensidad conocida**. El aviso es informativo y no juzga.

### 20. ¿Qué información falta para evaluar la actividad aeróbica total?

- La actividad fuera de la app.
- La intensidad por bloque (RPE o talk test).
- La duración ejecutada por bloque.

### 21. ¿Es necesario aumentar el cardio programado?

**No está demostrado.** Primero hay que medir. Cambiarlo afecta a la dosis 4D, a la fuerza, a V20 y a V24, y requiere revisión profesional.

### 22. ¿Hay problemas relevantes de cobertura de fuerza en LOSE_WEIGHT?

**Sí, como brecha de programa combinado, no de seguridad.**
- 148 de 230 planes tienen algún grupo grande a 0 (hombros, el más frecuente).
- Solo 14 de esos 148 tienen restricciones.
- Es efecto de 4A v6: fuerza secundaria, sin V23.
- En 104 de los 148 se cerraría añadiendo un ejercicio con la dosis aprobada, sin tocar 4A, 4C ni 4D. Requiere aprobación.

### 23. ¿Qué debería validar un profesional del ejercicio?

- La muestra ciega de 28 planes (ficha de 12 criterios), con la pregunta específica sobre si C2 deja sesiones suficientemente completas.
- Además:
  - G1: cardio de LOSE_WEIGHT;
  - G2: 180 s de descanso;
  - G3: fuerza en LOSE_WEIGHT;
  - G4: V18.
- En particular: las sesiones del 46 %, el PULL sin material y las 21 series por sesión.

### 24. ¿El versionado de políticas funciona?

**Como modelo, sí.** Probado de forma aislada:
- solo la histórica es activable;
- un plan sin versión se lee como histórico;
- una versión desconocida es un error;
- la traza contiene los parámetros efectivos.

**No está cableado** y la migración es solo una propuesta.

### 25. ¿Las adaptaciones y progresiones mantienen su comportamiento?

**Sí.**
- 426 IT ejecutadas sin fallos.
- Campaña idéntica.
- Longitudinal: 0 violaciones del 5 % por paso y del 10 % acumulado.
- Sin subidas tras dolor, fatiga o baja adherencia. El matiz de L7 está en `SIMULACIONES_Y_REGRESIONES_CIERRE.md` §4.
- Un piso menor no aumenta la dosis.

### 26. ¿Cuántas pruebas pasaron y cuántas se omitieron?

| Batería | Pasaron | Omitidas | Fallos |
|---|---:|---:|---:|
| Unitarias | 975 | 0 | 0 |
| Integración | 466 en total, 426 ejecutadas | 40 (opt-in) | 0 |
| Laboratorio `VerificacionPoliticasIT` | 5/5 | — | 0 |
| Campaña REPLAY | 1 | 1 | 0 |

### 27. ¿Hubo modificaciones en Neon o Railway?

**No.**

### 28. ¿Hubo llamadas reales a Replicate?

**Cero.**

### 29. ¿Se mantuvo E desactivado?

**Sí.** `habilitado = false`, y la prueba de aislamiento pasa.

### 30. ¿Qué cambios concretos recomiendo aprobar?

Por este orden:
1. **Revisión profesional** de la muestra ciega. Es un requisito previo.
2. **Versionado de políticas:** migración probada en base efímera y aplicada después, con orden explícita.
3. **V4-C**, en versión nueva.
4. **C2-D**, con la duración estimada visible. Decidir si se añade un suelo del 50 % tras la revisión (P1).
5. **Métrica aeróbica informativa**, sin cambiar la dosis.
6. **Preferencia por el grupo grande no cubierto** en la fuerza de LOSE_WEIGHT.
7. **Acceso de red a los textos completos** (pmc.ncbi.nlm.nih.gov, www.who.int / iris.who.int, link.springer.com, journals.lww.com) para cerrar la revisión científica.

**No se recomienda cambiar 4A, 4C, 4D, V24, P-01 ni la dosis de cardio.**
