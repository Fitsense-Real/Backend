# FitSense — Dictamen técnico propuesto para aprobación del equipo

**Fecha:** 28 de octubre de 2026  
**Objeto:** revisión de nueve ejercicios del catálogo y resolución técnica propuesta del conflicto entre V28 y V36 para el ejercicio 872.  
**Autoría de la evaluación:** asistencia de IA sobre fuentes documentales.  
**Aprobación humana del proyecto:** **PENDIENTE**.  
**Alcance probatorio:** se examinaron los datos y migraciones de `Backend-develop(10).zip`, el informe `CATALOGO_V36.md` y la auditoría de nueve ejercicios. **No se inspeccionaron directamente los nueve GIF animados**; sus movimientos se describen a partir de la revisión visual informada por el desarrollador. Las afirmaciones marcadas *condicionadas al GIF* requieren cotejo del archivo antes del cierre documental.

## 1. Matriz de decisiones propuestas

| ID | Movimiento observado según auditoría | Hallazgo sustentado | Dictamen técnico para FitSense | Estado de aprobación propuesto |
|---:|---|---|---|---|
| **1267** | Stalder press gimnástico invertido | La base de origen (`3291`) describe erróneamente una sentadilla; en V36 aparece como nivel 1, sin inversión y patrón de rodilla | **Desactivar (`is_active=false`)** para planes nuevos, sin borrar histórico. No es necesario implementar su ficha avanzada para MVP | **APROBAR**, condicionada la identificación del GIF |
| **1296** | Superman push-up con despegue de manos y pies | El origen (`0803`) describe una elevación alterna; dificultad/impacto no reflejan una flexión pliométrica | **Dificultad 3**, `high_impact=true`; mantener `HORIZONTAL_PUSH` y corregir instrucciones de ejecución | **APROBAR**, condicionado a confirmar la fase pliométrica |
| **1333** | Carrera con zancadas amplias/sprint | El origen (`3637`) registra `body weight` pero instrucciones de rueda abdominal; V35 exige erróneamente `ab wheel` | **Peso corporal**, sin rueda, `requires_floor=false`, `requires_overhead=false`; corregir nombre e instrucciones; `high_impact=true` solo si el GIF confirma sprint explosivo | **APROBAR corrección de objeto/equipo**; impacto condicionado |
| **872** | Encogimiento abdominal invertido | Origen (`0484`): `waist/abs`, pero texto describe puente de glúteos. V28 lo validó en `LOWER_HIP` como `HIP_EXTENSION` sobre esa identidad antigua | **`TRUNK_FLEXION`**, `waist/abs`, nombre «Encogimiento invertido», instrucciones abdominales verificadas; no usar V28 como evidencia de aprobación LOWER_HIP del ejercicio corregido | **APROBAR sujeto a cotejo visual** y registrar rectificación metodológica |
| **1059** | Zancada con rotación de tronco | Texto de origen (`1688`) describe zancada + giro; `waist/abs` no refleja el patrón principal usado por el planificador | **`upper legs/quads`**, `KNEE_DOMINANT`, `abs` sinergista | **APROBAR** para planificación |
| **527** | Zancada con curl y giro tipo boliche | Origen (`1651`) combina trabajo de piernas, bíceps y giro; clasificado como bíceps permite entrada incorrecta en PULL | **`upper legs/quads`**, `KNEE_DOMINANT`, bíceps sinergista | **APROBAR** para planificación; vigilar componente de giro |
| **609** | Zancada con curl | Origen (`1658`) combina zancada y curl; entra indebidamente en PULL como bíceps | **`upper legs/quads`**, `KNEE_DOMINANT`, bíceps sinergista | **APROBAR** para planificación |
| **725** | Curl seguido de press de hombros sentado | Origen (`3547`) pide sentarse en banco; el catálogo no garantiza el requisito de banco | **`shoulders/delts`**, `VERTICAL_PUSH`, bíceps sinergista, conservar `requires_overhead=true`; **verificar exigencia de banco además de mancuernas** | **APROBAR clasificación**; equipamiento pendiente de prueba |
| **760** | Curl alterno y press de pie | Origen (`3560`) contiene press overhead; clasificarlo como PULL por bíceps es funcionalmente incorrecto | **`shoulders/delts`**, `VERTICAL_PUSH`, bíceps sinergista, conservar `requires_overhead=true`; no exigir banco | **APROBAR** para planificación |

**Criterio común:** para el MVP, `body_part`, `target_muscle` y `movement_pattern` son categorías principales de planificación, no una afirmación de que solo participa ese músculo. No crear categoría `MIXED`, ni relajar V15/V24/V25 para justificar los cambios.

## 2. Resolución técnica propuesta — discrepancia del ejercicio 872 entre V28 y V36

### 2.1. Evidencia histórica constatada

1. La migración `V28__approve_lower_hip_difficulty.sql` registra un evento `APPROVED` para el ejercicio **872**, dificultad **nivel 1**, con los revisores históricos **Juan y Daniel**, dentro del grupo `LOWER_HIP`. La precondición identifica su estado histórico como `source_code='0484'`, `name_en='hip raise (bent knee)'` y `movement_pattern='HIP_EXTENSION'`.
2. Las notas de esa misma migración indican que la decisión de nivel 1 era previa y que la revisión de octubre coincidió; **no fue evidencia nueva**. También compara las instrucciones con el ejercicio 1073 *low glute bridge* (92,8 % de similitud textual).
3. La entrada de origen del catálogo tiene `body_part='waist'` y `target='abs'`, pero sus instrucciones en español describen acostarse con los pies apoyados y levantar la cadera hasta formar una línea entre hombros y rodillas (puente de glúteos).
4. La auditoría visual del desarrollador concluyó que el GIF correspondiente representa **un encogimiento abdominal invertido**, es decir, movimiento distinto al descrito en el texto.

### 2.2. Explicación del conflicto

No es una discrepancia ordinaria de dificultad entre evaluadores. La evidencia de V28 y la propuesta de V36 **describen movimientos distintos bajo un mismo identificador**. Por eso el evento de aprobación de V28 **no demuestra** que el encogimiento abdominal invertido haya sido revisado y aprobado como ejercicio `LOWER_HIP` ni que su nivel de dificultad haya sido reevaluado visualmente como movimiento abdominal.

### 2.3. Dictamen y rectificación propuesta

**Dictamen:** siempre que el GIF original confirme el encogimiento inverso, mantener el ejercicio 872 activo con `movement_pattern='TRUNK_FLEXION'`, `body_part='waist'`, `target_muscle='abs'` y nombre español «Encogimiento invertido». Reescribir `instructions_es` e `instruction_steps_es` para describir la flexión y retroversión pélvica que se observe realmente, sin copiar las instrucciones del puente de glúteos.

**Tratamiento de V28:**

- **Conservar inalterada** la migración V28, el evento histórico `APPROVED`, los nombres de revisores y sus respuestas históricas.
- Incorporar un **anexo/fe de erratas metodológica** al expediente de V36 que indique que la aprobación de V28 se emitió sobre una identidad funcional/documental que no coincide con el GIF atribuido al ejercicio 872.
- **No contar el evento V28 como evidencia de cobertura aprobada `LOWER_HIP` después de V36**; esa cobertura disminuye de cuatro a tres casos según el reporte del desarrollo.
- **No transferir automáticamente la aprobación histórica de nivel 1 a la nueva identidad funcional** para fines de futuras aprobaciones humanas. Si en el futuro se activa la selección basada en aprobación humana, deberá existir una revisión apropiada para la nueva identidad. El valor `difficulty_level` operativo puede mantenerse como estaba en V36 hasta que se autorice una revisión específica.
- No emitir eventos nuevos, revocaciones o modificaciones en `exercise_difficulty_approvals` sin verificar antes el contrato de V27 y tener aprobación del equipo. Para el MVP actual, basta con la rectificación documental y la prueba de que el filtro global de aprobaciones no está activado.

**Texto propuesto para el expediente:**

> Durante la revisión del catálogo para V36 se identificó una discrepancia de identidad funcional en el ejercicio 872 (`source_code 0484`). La evaluación histórica de V28 lo incluyó en `LOWER_HIP` como `HIP_EXTENSION`, sobre la base de instrucciones textuales asociadas a un puente de glúteos. La auditoría posterior del material animado reportó un encogimiento abdominal invertido, correspondiente a `TRUNK_FLEXION`. En consecuencia, una vez confirmada visualmente la animación original, el equipo adopta la clasificación abdominal para la generación futura. Se conservan íntegros los registros originales de V28 como trazabilidad histórica, pero no se interpretan como aprobación de la nueva identidad funcional ni se incluyen como evidencia vigente de cobertura `LOWER_HIP`. Esta corrección no altera retroactivamente las valoraciones de los revisores.

## 3. Condiciones técnicas antes de cerrar V36

1. Cotejar y registrar fotogramas o enlace interno verificable de los nueve GIF originales (en especial 872, 1296 y 1333). No presentar como observación visual propia de IA lo que procede del informe del desarrollador.
2. Ajustar las instrucciones contradichas por la animación de **1296, 1333 y 872**.
3. Verificar si el ejercicio **725** puede requerir banco dentro del equipamiento ya modelado. Si no, presentar una medida mínima sin añadir campos o lógica nueva innecesaria.
4. Probar la migración V36 en PostgreSQL aislado; conservar la referencia histórica V35 y contrastar las 72 candidatas V36.
5. Validar con PR #3: 72/72 perfiles factibles, 0/288 generaciones sin plan, y copia semanal correcta para **planes nuevos creados con V36**. Los planes incompatibles antiguos son sintéticos y no exigen desarrollar un sistema de compatibilidad retrospectiva.
6. No ejecutar migraciones en Neon, no desplegar, no fusionar automáticamente, no gastar Replicate.

## 4. Estado académico y aprobación

- **Evaluación técnica asistida por IA:** preparada y documentada; no equivale a observación clínica o biomecánica certificada ni a revisión humana independiente.
- **Revisión del responsable del proyecto:** PENDIENTE.
- **Segundo dictamen humano independiente (si el protocolo académico lo exige):** PENDIENTE. Una aprobación del responsable sobre este dictamen no demuestra por sí sola dos evaluaciones humanas ciegas.
- **Consenso ciego R1/R2 para V36:** NO REGISTRADO. No fabricarlo.

**Opción de aprobación del responsable:**

- [ ] **Apruebo** este dictamen técnico y la rectificación propuesta del ejercicio 872, condicionados a comprobar los GIF y el banco del 725.
- [ ] **Apruebo con observaciones** (especificar IDs y cambios).
- [ ] **No apruebo** (indicar motivo).

**Responsable:** __________________________  
**Fecha de aprobación:** __________________________  
**Observaciones:** __________________________

## 5. Fuentes consultadas

- `Backend-develop(10).zip`: `V9__catalog_full.sql`, `V28__approve_lower_hip_difficulty.sql`.
- `CATALOGO_V36 (1).md`: comparación V35/V36, resultados de generación y conflicto V28.
- `Revision_tecnica_nueve_ejercicios_FitSense_V36.md`: evaluación preliminar de los nueve ejercicios.
- Auditoría previa «Revisión de 9 ejercicios del catálogo».

**No se han realizado cambios en el código, repositorio ni datos.**
