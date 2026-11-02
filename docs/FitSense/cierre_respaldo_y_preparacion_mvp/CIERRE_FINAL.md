# Cierre: respaldo, PR #20 y preparación del MVP

**La prioridad se mantiene:**
- preservar la continuidad del entrenamiento y proteger al usuario;
- sin regeneraciones innecesarias;
- sin forzar una rutina cuando no hay una solución segura.

## Condición de cierre

| Condición | Estado |
|---|---|
| PR #20 integrado en `generation` | **Sí:** 8f112d5, CI verde |
| PR #17 actualizado, probado y en borrador | **Sí:** cc97045 (+ documentación), regresión completa sin fallos y CI verde |
| Aserción de respaldo corregida en un PR independiente, pendiente de tu aprobación | **Sí:** Fitsense-Real/Backend#21, CI verde, **sin fusionar** |
| Mecanismo de respaldo documentado y probado | **Sí:** `RespaldoConDolorIT` (A, B y C) e invariantes en `ValidacionMultisemanaIT` |
| Diseño de P-03 localizado preparado, sin implementar | **Sí**, con medición |
| Caso NO_FACTIBLE 1373 explicado | **Sí** |
| Ninguna política nueva activada | **Sí** |
| Ningún despliegue ni modificación de producción | **Sí:** `src/main` sin cambios en los PR #20, #21 y #17 |

## Entregables (`docs/FitSense/cierre_respaldo_y_preparacion_mvp/`)

1. `FUSION_PR20.md`
2. `CORRECCION_TEST_RESPALDO.md`
3. `PRUEBAS_RESPALDO_DOLOR.md`
4. `COMPARACION_P03_ACTUAL_VS_LOCALIZADA.md`
5. `DISENO_P03_LOCALIZADO.md`
6. `ANALISIS_NO_FACTIBLE_1373.md`
7. `REGRESION_FINAL.md`
8. `ACTUALIZACION_PR17.md`
9. `DECISIONES_EVALUACION_PROFESIONAL.md`
10. `ESTADO_CONTRATOS_FLUTTER.md`
11. `DECISIONES_PENDIENTES.md`
12. `CIERRE_FINAL.md`

**Evidencia** en `evidencia/`:
- `analisis_1373.md` y `candidatos_1373.csv`;
- `prototipo_localizado.md` y `prototipo_localizado.csv`.

Las evidencias anteriores se conservan.

## Informe final (20 preguntas)

**1. ¿Se fusionó el PR #20?**
- **Sí,** con *merge commit*, comprobando la cabeza d4717b9.
- Antes se verificaron sus 9 condiciones: CI verde, sin conflictos, solo pruebas, `src/main` intacto, escenarios iguales, REPLAY igual, ninguna prueba relajada y sin despliegue.
- También que el reloj solo escribe `expires_at` de los usuarios creados por cada prueba en su PostgreSQL de Testcontainers.

**2. ¿Cuál es el commit final de `generation`?**
**8f112d5deb2bdef428a054a4be824bdf570deca2**.

**3. ¿Su CI está verde?**
**Sí:** #109 (`push`) y #110 (PR #1), los dos *success*.

**4. ¿Quedaron corregidos los laboratorios opcionales?**
- **Sí:**
  - `CampanaP123Cohorte02IT`, `CampanaMvp8UsuariosIT` y `ReincorporacionIaRealIT`, con el reloj de pruebas;
  - además, `LinajeIntegridadIT` y `PoliticasP123IT`.
- Ya están en `generation`.
- **Sin verificar:** las ejecuciones con IA real (no autorizado) y el conjunto «estrés» de Mvp8 (perfil de 15 min incompatible con P-01).

**5. ¿Se corrigió la aserción D1?**
- **Sí, en el PR #21, pendiente de tu aprobación.** La igualdad absoluta pasa a ser las invariantes reales de la política:
  - **A:** copia, salvo las sustituciones SUST-SEG-1 trazadas;
  - **B:** solo la posición de lo que dolió, sin subir dosis ni trabajo, con la nota en V32;
  - **C:** sin semana segura, NO_FACTIBLE sin publicar y exclusiones intactas.
- No se ha eliminado ninguna aserción de seguridad.

**6. ¿Existe una prueba explícita de RESPALDO con dolor?**
**Sí:** `RespaldoConDolorIT`.
- Fuerza el respaldo con una IA que siempre falla.
- Es determinista y cubre B, A y C.
- B: una sola sustitución (456 → 773), las demás sesiones idénticas, V32 y API correctas, y la semana siguiente sin reincorporar.

**7. ¿Las pruebas funcionan con diferentes semillas?**
**Sí:**
- 26-oct, 9-nov y 2-nov: 0 fallos en `ValidacionMultisemanaIT` (antes, 2 / 1 / 0) y 3/3 en `RespaldoConDolorIT`.
- Cambian los ejercicios concretos; las invariantes se cumplen.

**8. ¿Se modificó algún archivo de producción?**
**No.** `src/main` sin cambios en el PR #20, en el PR #21 y en el PR #17 (desde 73b7008).

**9. ¿Se actualizó el PR #17?**
- **Sí,** por *merge* de `generation` (cc97045), sin conflictos y sin *rebase*.
- Sigue en borrador, con `PLAN-HIST-1` y los indicadores apagados.
- El PR #21 se incorporará cuando se fusione.

**10. ¿Su CI está verde?**
**Sí:** cc97045, c00e6e7 y 379b768. El commit final de documentación se comprueba en el PR.

**11. ¿Los 960 perfiles mantienen el comportamiento anterior?**
- **Sí,** idénticos byte a byte en las 5 políticas: `planes.csv` `c582fce2…` y `colocaciones.csv` `ee9d2111…`.
- Siguen 875, 951, 951, 902 y 902 válidos, y los 48 perfiles identificados por VEF.

**12. ¿La REPLAY mantiene los resultados históricos?**
**Sí:** idéntica salvo dos marcas de tiempo de `exclusiones_dolor.csv`, que se revirtieron.

**13. ¿Cómo funciona exactamente P-03 nivel 2?**
1. **Cuándo entra:** si la adaptación por **dolor** queda NO_FACTIBLE (IA sin propuesta usable y SUST-SEG-1 sin sustituto), `SafeRegenerationService`, en el mismo ciclo, regenera **la semana entera** con el motor de reglas (sin IA).
2. **Con qué:** el conjunto elegible que excluye lo que dolió, la dosis base 4D y sin linaje.
3. **Qué comprueba:**
   - V1–V25;
   - que no haya ningún excluido;
   - que **no suba ni las repeticiones ni los segundos totales** de la semana medida, por separado.
4. **Resultado:** si pasa, publica (`REGENERACION_SEGURA_REGLAS`); si no, registra NO_FACTIBLE y no publica nada.
5. **Idempotencia:** es idempotente por medición.

**14. ¿Es viable una alternativa localizada?**
- **Sí, técnicamente.** Con un prototipo de medición sobre 40 casos (solo pruebas):
  - pasa V1–V25 en 40/40 y no trae excluidos;
  - es publicable en 35/40, frente a 30/40 del nivel 2;
  - cambia **solo** el día con dolor, mientras que el nivel 2 cambia **120/120** sesiones.
- Con las dos en cadena (localizada y, si no, nivel 2): 36/40.
- **Requiere:**
  - un modo «una sesión con contexto semanal» en el generador;
  - una excepción nueva en V8C;
  - evidencia y API nuevas.
- **No implementado.**

**15. ¿En qué casos podría ser insuficiente?**
- Cuando la sesión reconstruida sube el trabajo, sobre todo al pasar de repeticiones a duración (el caso real del 1373: segundos 360 → 420).
- Con dolor en varios días, se acerca al nivel 2 y aumenta el riesgo de conflictos entre días.
- Sin equipamiento ni candidatos compatibles, igual que el nivel 2.
- La sesión reconstruida pierde su progresión previa.

**16. ¿Por qué el ejercicio 1373 no encontró sustituto?**
- 1373, «Círculos de muñeca», es el **único** complemento de movilidad prescrito por **repeticiones** en el catálogo activo; los otros 27 son de **duración**.
- SUST-SEG-1 exige el **mismo tipo** y la misma condición de complemento. Tras ese filtro quedan 0 candidatos, también sin el filtro de perfil.
- **Existen candidatos, pero incumplen la restricción de tipo.** Uno es 1178, «Estiramiento de muñeca lateral», de la misma zona y músculo.
- No se demuestra un error de datos. Es una decisión funcional (D3).

**17. ¿Se mantiene NO_FACTIBLE cuando corresponde?**
**Sí.** Sin sustituto seguro y sin una regeneración dentro del trabajo, no se publica nada y la exclusión sigue activa. Lo comprueban `RespaldoConDolorIT` C y las invariantes de `ValidacionMultisemanaIT`.

**18. ¿Qué decisiones profesionales faltan?**
- Aprobar A1–A6 con las recomendaciones finales: dos evaluadores, «4 de 4» en A2, ningún c9 ≤ 2 y consenso como desempate.
- Elegir a los profesionales.
- Autorizar el envío de `paquete_evaluador/`, que se conserva sin cambios.

**19. ¿Qué contratos necesita Flutter?**
- **Disponibles en `generation`:** perfil, catálogo, generación, plan semanal, inicio y registro, omisión con motivo, dolor por ejercicio, exclusiones y reactivación, adherencia, y resultado de la adaptación con `AdaptationOutcome`.
- **Detrás de indicadores** (PR #17): duración estimada, actividad aeróbica y NOFACT-0.1.
- **Falta:** un contrato de NO_FACTIBLE de **adaptación** con código de motivo, sesión afectada y estado de publicación. Hoy solo hay texto técnico.

**20. ¿Qué trabajo técnico es realmente necesario antes de probar el MVP con usuarios?**
1. **Fusionar el PR #21** (D1). Es solo pruebas.
2. **Decidir el despliegue de `generation`** y confirmar Railway. Al arrancar se aplicará V44 en Neon: hay que preparar la copia de seguridad y la vuelta atrás.
3. **Contrato de NO_FACTIBLE de adaptación** para la app: código de motivo y textos. Es pequeño, pero toca `src/main` y necesita aprobación.
4. **Decidir** si se activa la duración estimada, que requiere el PR #17 o portar solo ese contrato.
5. **La app Flutter** con el flujo de `ESTADO_CONTRATOS_FLUTTER.md` §4.
6. **D3-a, el dato del 1373** (opcional para el MVP): los casos son poco frecuentes y hoy se resuelven sin riesgo (NO_FACTIBLE).

**Lo que no hace falta para el MVP:**
- P-03 localizado, que es una mejora de continuidad;
- C2-D y EF, que esperan a la evaluación profesional.

## Lo que no se ha hecho (sin autorización)

- Fusionar el PR #17 ni el PR #21.
- Desplegar ni aplicar migraciones remotas.
- Activar V4-C, C2-D, EF ni ninguna política experimental.
- Implementar P-03 localizado ni modificar P-03 nivel 2.
- Cambiar el validador, el generador, V24, V8C, 4A, 4C o 4D.
- Reincorporar ejercicios, cambiar el catálogo o tocar Flutter.
- Consumir Replicate o publicar planes NO_FACTIBLE.
- Inventar evaluaciones.
