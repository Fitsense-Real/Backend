# Estado del PR #17 y de la evaluación profesional (fase 4)

**PR:** Fitsense-Real/Backend#17, **en borrador y sin fusionar**. Todos sus indicadores están apagados.

## 1. Dos decisiones distintas

| Decisión | Qué la respalda | Estado |
|---|---|---|
| **El código está técnicamente probado** | Regresión automatizada: unitarias, integración, 4 baterías por política, ciclo real, 960 perfiles × 5 políticas, REPLAY | Sí (ver `REGRESION_CIERRE_BACKEND.md`) |
| **Sus políticas son adecuadas para personas reales** | **Solo** la evaluación de profesionales del entrenamiento | **No evaluado.** Ningún paquete enviado y ninguna respuesta |

**La regresión no sustituye la evaluación profesional.** No se declara validado C2-D, EF, LW-PREF ni REP-SUST.

## 2. Aprobación formal de A1–A6

Los criterios son los de `cierre_backend_pre_mvp/CRITERIOS_EVALUACION_A1_A6.md`, que se mantienen **sin cambios** (huella SHA-256 del paquete en `PREPARACION_EVALUADORES.md`). Para aprobarlos, rellena y firma esta tabla. **Nada se envía con esta aprobación.**

| # | Criterio (resumen; texto completo en el documento) | Aspecto | ¿Aprobado? | Cambios |
|---|---|---|---|---|
| A1 | Plan aceptable: media ≥ 3 en c1–c11, ningún c9 ≤ 2, duración favorable o condicionada, sin rechazo en e2 ni e3 | Aceptabilidad, seguridad, duración, función, tracción | ☐ Sí ☐ No | |
| A2 | Los 4 planes cortos, solo como **exploración**. Antes de usar C2-D con participantes, **≥ 10 planes** con un criterio definido de antemano (ver §3) | Duración | ☐ Sí ☐ No | |
| A3 | ≥ 70 % de acuerdo en ±1, con revisión documentada de las discrepancias | Acuerdo | ☐ Sí ☐ No | |
| A4 | D01 y D02: si los dos evaluadores coinciden, se mantiene no generar; si discrepan, revisión sin cambiar el catálogo | NO_FACTIBLE | ☐ Sí ☐ No | |
| A5 | Punto de anclaje como única tracción: no se recomienda sin una evaluación de seguridad favorable | Equipamiento y tracción real | ☐ Sí ☐ No | |
| A6 | Dos profesionales independientes, paquete ciego, criterios registrados antes, resultados originales conservados, consenso con acta; un desacuerdo de seguridad no se aprueba automáticamente; **ninguna activación por la media** | Procedimiento | ☐ Sí ☐ No | |

| Campo | Valor |
|---|---|
| Fecha de aprobación | |
| Aprobado por | |
| Evaluador 1 (cualificación) | |
| Evaluador 2 (cualificación) | |
| Plazo | |
| **Autorización de envío** (aparte) | ☐ Autorizo enviar solo `revision_profesional/paquete_evaluador/` |

## 3. Ampliación de planes cortos de C2-D (preparada; no enviada)

Para A2: antes de usar C2-D con participantes hacen falta **al menos 10 planes cortos** evaluados.

**Criterio definido ahora**, antes de cualquier respuesta, y reproducible:
1. **Universo:** los perfiles de la matriz de 960 (la última verificada) en los que **solo C2-D publica**: PLAN-HIST-1 inválido y PLAN-C2D-0.1 válido.
2. **Exclusiones:** se quitan los que ya están en la muestra ciega.
3. **Sesiones cortas:** la duración media de las sesiones es menor que los minutos configurados.
4. **Selección:**
   - quedan 67 perfiles: 62 de GAIN_MUSCLE, 2 de LOSE_WEIGHT, 3 de INCREASE_STRENGTH y **0 de GENERAL_FITNESS**;
   - se toman **todos** los de LOSE_WEIGHT e INCREASE_STRENGTH;
   - de GAIN_MUSCLE, uno por estrato (nivel × equipamiento), en orden y con la semilla **20261102**, hasta llegar a **12**.

**Lista interna:** `AMPLIACION_C2D_PROPUESTA_INTERNA.csv`.
- Contiene solo perfiles y atributos, sin contenido de planes.
- **No se entrega** al evaluador; el material ciego se generaría con el mismo procedimiento que la muestra actual.

**Limitaciones:**
- no hay ningún plan de GENERAL_FITNESS en el universo;
- GAIN_MUSCLE domina el universo;
- con 12 planes, el resultado sigue siendo **exploratorio por objetivo**.

## 4. Después de la evaluación

1. Calcular A1–A6 con los criterios aprobados. **No se cambian después de ver las respuestas.**
2. Por componente:

| Componente | Decide | Si no supera |
|---|---|---|
| C2-D | A1, A2 y la duración | Sigue apagado |
| EF | e1–e3, G1, G2 | Sigue apagado |
| LW-PREF-0.2 | e7, G4 | Sigue apagado |
| REP-SUST-0.1 | G5 | Sigue apagado |
| Punto de anclaje | G3, A5 | No se recomienda |
| NO_FACTIBLE D01/D02 | A4 | Revisión aparte |

3. Una observación individual **no** cambia el algoritmo: va a una propuesta que tú apruebas.
4. **Recomendación sobre fusionar el PR #17 con los indicadores apagados:**
   - técnicamente es posible: sin cambios de comportamiento con `PLAN-HIST-1` (960 perfiles idénticos);
   - **no es necesario para el MVP**;
   - fusionarlo antes de la evaluación añade V45 al esquema y código candidato al despliegue, sin beneficio para los participantes.

   **Se recomienda esperar a la evaluación.** La decisión es tuya.

## 4 bis. Ajustes técnicos para cuando el PR #17 absorba `generation`

Con #22 y #25 fusionados, el PR #17 tendrá que incorporar dos cambios. Ambos están **probados en la combinación local C** (`REGRESION_CIERRE_BACKEND.md` §3) y **no se han subido**, porque la orden no autoriza tocar el PR #17.

| # | Ajuste | Por qué |
|---|---|---|
| 1 | `MigracionV45IT` parte de la última versión **anterior a 45**, no de `"44"` | Con V44_1 y V44_2 delante, migrar de 44 a 45 aplica 3 migraciones, no 1 |
| 2 | `EsquemaVigenteSobreCatalogoAnterior.SOLO_ESQUEMA` con 44.1, 44.2 y 45, por ese orden (conflicto de fusión en la misma línea) | Las pruebas con catálogo anclado aplican aparte las migraciones de solo esquema; sin ellas fallan 37 IT |

Con los dos ajustes, la regresión del PR #17 + 5 PR queda en verde:
- 1.059 unitarias;
- 514 IT, con 57 omitidas;
- 4 × 225 en las baterías;
- ciclo real 6/6;
- 960 × 5 idéntico a la referencia;
- REPLAY idéntico.

## 5. Cierre con la política histórica

| Estado | Qué incluye |
|---|---|
| **Backend histórico listo para la integración** | Todo el MVP con `PLAN-HIST-1`: registro, perfil (P-01), generación con el motor de reglas, plan, ejecución, dolor (P-02, P-03), omisión, adherencia, adaptación localizada con respaldo, progresión (implementada, apagada por defecto: `FITSENSE_PROGRESSION_ENABLED`) y reincorporación, NO_FACTIBLE estructurado (#22), recuperación de contraseña (#25, sin proveedor) |
| **Políticas candidatas pendientes de evaluación** | C2-D, EF-0.1, V4-C con C2-D, LW-PREF-0.2, REP-SUST-0.1, NOFACT-0.1, duración por contenido y métrica aeróbica (todo en el PR #17, apagado) |
| **Capacidades que necesitan autorización antes de usarse con personas** | IA real (Replicate), progresión encendida (`FITSENSE_PROGRESSION_ENABLED`), despliegue en Railway, migraciones en Neon, proveedor de correo, alojamiento y licencia de los medios, cualquier política candidata |

**El backend puede funcionar con `PLAN-HIST-1` mientras se evalúan las candidatas:** ningún flujo del MVP depende del PR #17.
