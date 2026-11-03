# Evidencia de la evaluación de calidad de V2

| Carpeta o archivo | Qué es |
|---|---|
| `catalogo_exercises_v44_2.csv` | El catálogo con las 46 migraciones aplicadas a una base local efímera (1.219 activos), usado en la auditoría |
| `auditoria_sustituciones.py` | Genera `02_AUDITORIA_SUSTITUCIONES.csv` a partir de las evidencias de los PR #28 y #29 (solo lectura) |
| `seguridad.py` | Comprobaciones de seguridad y lista de sesiones en el mínimo V20, para cualquier corrida (una o dos semanas) |
| `GUION_COMBINADOS_2S/` | U01–U20, sin red. Semana 1 con los 12 escenarios combinados y semana 2 con el guion V2 (`07`, `06`) |
| `GUION_COMBINADOS_2S_fallo_laboratorio/` | Ejecución interrumpida por un defecto del laboratorio (H9), conservada tal cual |
| `R_DETERMINISTA_2S/` | R01–R08 con la IA apagada, dos semanas |
| `R_GUION_2S/` | R01–R08 con el guion V2, dos semanas, con los prompts V2 reales |
| `RC1_r1/` | R01–R08, dos semanas, **Replicate real**, `prompt=v1` (22 predicciones, US$ 0,3295). Incluye `predicciones_replicate/` (JSON de cada predicción e `indice.json`) y los prompts enviados |
| `RC2_r1/` | Ídem con `prompt=v2` (13 predicciones, US$ 0,1749) |
| `analisis_complementario.py` | Genera `12`, `13` y `auditoria_sustituciones_RC.csv` a partir de `RC1_r1` y `RC2_r1` (solo lectura) |
| `auditoria_sustituciones_RC.csv` | Cada sustitución y cada cambio de dosis de RC1_r1 y RC2_r1, semanas 1 y 2: patrón, músculo, dificultad y clase Q |
| `matriz_calidad_RC.py` / `matriz_calidad_RC.csv` | Las 12 preguntas de la fase 10 para las 22 sesiones adaptadas en RC1_r1/RC2_r1. Sus filas se **añadieron** al final de `14_MATRIZ_CALIDAD_POR_ADAPTACION.csv`; las 32 filas anteriores (PR #29) no se tocaron |

**Llamadas a Replicate:** solo en `RC1_r1` y `RC2_r1`, después de tu aprobación de la opción A (anexo 1 de `09`). El resto de carpetas son sin red.

**Reproducir:** `python3 -I analisis_complementario.py ..`, `python3 -I matriz_calidad_RC.py ..` y `python3 -I seguridad.py RC1_r1 ../10_PERFILES_SINTETICOS_R01_R08.csv` (y lo mismo con `RC2_r1`).
