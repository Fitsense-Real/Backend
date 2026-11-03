# Anexo · rectificación metodológica del ejercicio 872 frente a V28

**Fecha:** 29/10/2026.

**Origen:** dictamen técnico asistido por IA (`Dictamen_revision_V36_y_rectificacion_872_para_aprobacion.md`, §2), **aprobado por el responsable del proyecto** en la orden de cierre de V36 (28/10/2026).

**Alcance:** documental. No modifica V28 ni crea eventos en `exercise_difficulty_approvals`.

## 1. Lo que registró V28 (se conserva íntegro)

`V28__approve_lower_hip_difficulty.sql` registró un evento `APPROVED` para el ejercicio **872**:
- `source_code` 0484, `name_en` «hip raise (bent knee)»;
- nivel **1**, dentro del grupo `LOWER_HIP`, patrón `HIP_EXTENSION`;
- revisores **Juan y Daniel**, el 10/10/2026.

El material de la revisión era **texto** (`revision-LOWER_HIP.txt`). Las instrucciones del catálogo de 872 describen un **puente de glúteo**: «levanta las caderas del suelo hasta que el cuerpo forme una línea recta desde las rodillas hasta los hombros». V28 anota además una coincidencia del 92,8 % con las instrucciones de 1073 (*low glute bridge*).

## 2. Lo que muestra el GIF original (cotejado)

`videos/0484-196HJGw.gif`, 12 fotogramas: tumbado boca arriba con las rodillas flexionadas, despega los pies, lleva las rodillas al pecho, enrolla la pelvis y la despega del suelo (fotogramas 5–7), y vuelve.

Es un **encogimiento abdominal invertido**: flexión de tronco (`TRUNK_FLEXION`), con el abdomen como músculo principal. La cadera no se extiende.

Detalle en `VERIFICACION_GIF_V36.md`.

## 3. Texto de la rectificación (aprobado)

> Durante la revisión del catálogo para V36 se identificó una discrepancia de identidad funcional en el ejercicio 872 (`source_code 0484`). La evaluación histórica de V28 lo incluyó en `LOWER_HIP` como `HIP_EXTENSION`, sobre la base de instrucciones textuales asociadas a un puente de glúteos. La auditoría posterior del material animado reportó un encogimiento abdominal invertido, correspondiente a `TRUNK_FLEXION`. En consecuencia, una vez confirmada visualmente la animación original, el equipo adopta la clasificación abdominal para la generación futura. Se conservan íntegros los registros originales de V28 como trazabilidad histórica, pero no se interpretan como aprobación de la nueva identidad funcional ni se incluyen como evidencia vigente de cobertura `LOWER_HIP`. Esta corrección no altera retroactivamente las valoraciones de los revisores.

La animación original quedó confirmada (§2).

## 4. Cómo se aplica

| Condición de la orden | Cómo se cumple | Comprobación |
|---|---|---|
| V28 intacta: migración, evento, revisores y resultados | V36 no toca V28 ni `exercise_difficulty_approvals` | `CatalogoV36IT`: la huella de la tabla es idéntica antes y después de V36, y sigue el evento `APPROVED` de nivel 1 de 872 |
| V28 no prueba la nueva identidad | Este anexo | — |
| 872 no cuenta como aprobado de `LOWER_HIP` después de V36 | La vista `v_exercises_manually_approved` toma el patrón del catálogo: 872 aparece como `TRUNK_FLEXION`. LOWER_HIP pasa de **4 a 3** aprobados (122, 776, 878) | `CatalogoV36IT`: antes `{HIP_EXTENSION=3, HIP_HINGE=1}`, después `{HIP_EXTENSION=2, HIP_HINGE=1, TRUNK_FLEXION=1}` |
| No transferir la aprobación de nivel a la nueva identidad | El filtro global de aprobación humana (`fitsense.approvals.filter-enabled`) está **apagado**, así que la aprobación no tiene efecto en la selección | `CatalogoV36IT`: `ApprovedDifficultyGate.isEnabled()` es falso |
| Nivel operativo vigente | `difficulty_level` de 872 sigue en 1 | Matriz de V36 |
| Sin eventos falsos | Ninguna revisión, consenso, revocación ni aprobación nueva | — |

## 5. Pendiente, sin inventar evidencia

- **Revisión humana independiente de la nueva identidad.** Ninguna persona ha revisado todavía 872 como encogimiento invertido. Queda **PENDIENTE**.
- **Condición para encender en el futuro el filtro de aprobaciones.** Mientras no exista esa revisión, el evento de V28 seguiría admitiendo 872, ahora abdominal, con nivel 1. Antes de encender el filtro habrá que registrar, con el contrato de V27 y aprobación del equipo, una revisión de 872 o un evento REVOKED. Hoy no se registra nada.
