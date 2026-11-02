# Decisiones pendientes

Cada punto necesita tu aprobación. **No se ha aplicado ninguno.** Ninguno cambia la regla central sin tu aprobación expresa.

## D-1. Aserción de respaldo de `ValidacionMultisemanaIT` (Resultado 2)

**Situación:**
- La aserción «RESPALDO ⇒ semana idéntica» es correcta **sin dolor**.
- **Con dolor** contradice P-03 (aprobada) y la propia aserción «lo que dolió no vuelve» de la prueba (ver `DIAGNOSTICO_RESPALDO_MULTISEMANA.md` §4).

| Opción | Qué supone |
|---|---|
| **a (recomendada)** | Mantener la igualdad salvo en `RESPALDO` con `SEGURIDAD_ANTE_DOLOR`. Ahí, comprobar las invariantes del §4: no señaladas idénticas; en la señalada solo cambian las posiciones de lo que dolió, por SUST-SEG-1 y sin subir dosis ni trabajo; nota y sustitución en V32. Solo pruebas |
| b | Dejarla como está. Es determinista con la semana fija, pero sigue afirmando algo que la política no exige: un cambio del catálogo o de la generación inicial puede volver a romperla sin defecto real |
| c | Además de (a), añadir un escenario explícito de RESPALDO con dolor, para que el camino P-03 nivel 1 quede cubierto sin depender de la semilla |

**Relacionado (solo documentación, pero en `src/main`):** el comentario de `AdaptedWeekProducer.produce` («RESPALDO es una copia exacta») es anterior a P-03. Propuesta: «copia exacta salvo las sustituciones de seguridad SUST-SEG-1».

## D-2. P-03 nivel 2 frente a la regla central (5.1 y 5.5)

**Situación:**
- `SafeRegenerationService` existe desde el PR #16 y fue aprobado el 31/10/2026.
- Tras un NO_FACTIBLE **por dolor**, regenera **toda** la semana con el motor de reglas: dosis base y sin linaje.
- Está acotado: sin IA, sin lo excluido, con V1-V25, sin subir repeticiones ni segundos e idempotente.
- **No es el respaldo**, pero no es localizado.

| Opción | Qué supone |
|---|---|
| a | Mantenerlo: es la política aprobada; la alternativa es dejar a la persona sin semana |
| b | Restringirlo a regenerar **solo las sesiones con dolor** y conservar las demás. Es un cambio funcional y requiere diseño y aprobación |
| c | Desactivarlo: vuelve el comportamiento anterior, NO_FACTIBLE sin semana |

**Recomendación:** decidirlo antes de la evaluación profesional. Esta orden no lo cambia.

## D-3. Semana sin publicar cuando SUST-SEG-1 no encuentra sustituto

**Situación:** en el laboratorio (2 de 72 recorridos, GENERAL_FITNESS) dolió 1373, un relleno de series y repeticiones en FULL_BODY:
- **SUST-SEG-1** no encontró sustituto;
- **P-03 nivel 2** se descartó porque subía los segundos totales de 1.140 a 1.260, aunque bajaba las repeticiones de 436 a 396;
- **resultado:** NO_FACTIBLE y nada publicado.

Es el comportamiento aprobado («ante dolor no se añade trabajo», con las dos magnitudes por separado).

**Qué decidir:**
- **a.** Si se acepta esa consecuencia (actual).
- **b.** Si se quiere estudiar, **antes** de cambiar nada, por qué el catálogo no ofrece un sustituto SUST-SEG-1 para 1373. Es una investigación, no un cambio.

## D-4. Fusión del PR de laboratorios

**Situación:** el PR de laboratorios queda **en borrador y sin fusionar**, como pide la orden. Contiene, solo en `src/test`:
- el reloj de pruebas para `CampanaP123Cohorte02IT`, `CampanaMvp8UsuariosIT` y `ReincorporacionIaRealIT`;
- `DiagnosticoRespaldoMultisemanaIT`;
- la propiedad de diagnóstico `fitsense.pruebas.semana-referencia`.

**Por qué importa:** sin fusionarlo, esos tres laboratorios *opt-in* fallarán por vencimiento a partir del 2026-11-09 si se vuelven a ejecutar desde `generation`.

## D-5. Pendientes anteriores que siguen abiertos

1. **Railway:** confirmar en su panel que no despliega desde `generation`. Desde aquí no hay acceso.
2. **Evaluación profesional de C2-D + EF:** aprobar A1–A6 (`integracion_controlada_pr18_pr17/DECISIONES_PROFESIONALES_PENDIENTES.md`) y elegir a los evaluadores. Enviar **solo** `revision_profesional/paquete_evaluador/`.
3. **Flutter:**
   - autorizar el trabajo y dar acceso al repositorio;
   - decidir qué contrato se enciende primero: `fitsense.api.duracion-estimada.enabled` o `fitsense.api.no-factible-estructurado.enabled`;
   - aprobar la lista de equipamiento unificada.
4. **Despliegue de `generation`:** aplicaría V44 en Neon al arrancar. Es una decisión aparte de activar cualquier política.

## Observación menor (sin decisión urgente)

**Qué pasa:** en la evidencia V32 de un RESPALDO con dolor sin sustituto, el motivo de V8C dice «la sesión N registró dolor **y no se señaló para adaptarla**», aunque sí estaba señalada.

**Por qué:** el respaldo reutiliza la comprobación de continuidad, que trata todas las sesiones como no señaladas.

**Impacto:** solo afecta al texto del motivo, no a la decisión. Corregirlo tocaría `src/main`.
