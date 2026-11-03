# Evidencia del experimento C1/C2

| Carpeta | Qué es |
|---|---|
| `diagnostico/` | `factib.py` y `factib.txt`: reconstrucción de las 8 adaptaciones de la etapa C (fase 2) |
| `GUION_v1/`, `GUION_v2/` | Integración sin red con el laboratorio y la base real. La respuesta del modelo la escribe un guion (`06`) |
| `C1_r1/`, `C2_r1/`, `C2_r2/`, `C1_r2/` | Las 4 corridas con Replicate real, **tal como salieron**: configuración, trazas, prompts, registro de Maven, CSV del laboratorio y `predicciones_replicate/` (entrada, salida y métricas de cada predicción, con `indice.json`) |
| `descargar_predicciones.py` | Descarga por GET (gratuito) de las predicciones de una corrida. La credencial la inyecta el proxy: el script no la ve |
| `analisis.py` | Genera `08`–`13` y `analisis_resumen.json` a partir de las carpetas anteriores (solo lectura) |

**Reproducir el análisis:** `python3 -I evidencia/analisis.py .`, desde `experimento_prompt_v2/`.
