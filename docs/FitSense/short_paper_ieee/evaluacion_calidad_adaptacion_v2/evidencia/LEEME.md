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

**Sin llamadas a Replicate en ninguna carpeta.**

`12_RESULTADOS_C1_COMPLEMENTARIOS.csv` y `13_RESULTADOS_C2_COMPLEMENTARIOS.csv` **no existen** porque la fase con Replicate espera tu aprobación del presupuesto (`09`).
