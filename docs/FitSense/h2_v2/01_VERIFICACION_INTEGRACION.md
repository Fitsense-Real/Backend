# 01 · Verificación de la integración actual (`generation` en `98276a93`)

| Comprobación | Resultado | Cómo |
|---|---|---|
| `generation` contiene #82, #83 y #84 | **Sí**: `e46dbe18` (#82), `b9c197de` (#84) y `98276a93` (#83) | `git merge-base --is-ancestor` y la API de GitHub (`merged=true`) |
| V65 integrada | **Sí**: `V65__dolor_identificacion_posterior.sql` | Probada sobre base existente y nueva (`MigracionV65IT`) |
| Modelo por omisión | **`gpt-5-mini`**: endpoint `…/models/openai/gpt-5-mini/predictions`, formato `direct` deducido | `application.properties` y `ReplicateProperties.modeloEfectivo()` |
| Razonamiento | **`reasoning_effort=medium`**, `verbosity=low`, `max_completion_tokens=16000` | Ídem; cada petición se comprueba en `Gpt5MiniConO20IT` y `H2v2PipelineSimuladoIT` |
| Parámetros de `gpt-5-structured` | **No se envían** (`model`, `json_schema`, `messages`) | Ídem |
| `FITSENSE_AI_BASE_URL` | Sin definir en el repositorio; nadie la fija. Si un entorno la define con `gpt-5-structured`, manda sobre el valor por omisión | Revisar en el panel de Railway: no tengo acceso desde aquí. Procedimiento en `migracion_gpt5_mini/07_CONFIGURACION_EFECTIVA_Y_O20.md` §4 |
| Entornos local y Railway | Documentados (§4 del documento anterior y `cierre_definitivo_backend/MIGRACIONES_Y_DESPLIEGUE.md` §2) | Revisado |
| Respuesta fragmentada | Se une en orden y el texto no se reinterpreta | `ReplicateGpt5MiniDirectoTest` («La salida fragmentada se une en orden») |
| Respaldo frente a IA | Una rutina de la IA queda con `generation_source = AI` y `model_name = gpt-5-mini`. El respaldo, con `RULE_ENGINE` y `model_name` **nulo** | `H2v2PipelineSimuladoIT`: tres rechazos y después reglas, que no constan como IA. También `Gpt5MiniConO20IT` |

**No se cambió el modelo ni sus parámetros.** PR-2.0 solo cambia el texto y la instantánea cuando se activa.
