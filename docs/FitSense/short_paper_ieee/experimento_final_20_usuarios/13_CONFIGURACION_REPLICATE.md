# Configuración de Replicate (etapa C)

## Historial

| Momento | Estado |
|---|---|
| 2026-11-03 01:2x | **Bloqueada:** la sesión no tenía `REPLICATE_API_TOKEN`. El modo REPLICATE falló en la guarda previa con 0 llamadas (`evidencia/C_bloqueo`) |
| Después | Se añadió la credencial como **secreto de red**: el proxy del entorno la inyecta en las peticiones a `api.replicate.com`. El laboratorio admite este modo (anexo 1 del protocolo) |
| Intento 1 (01:36) | **Error del laboratorio:** el espía lanzaba `ClassCastException` antes de llamar. **0 predicciones** (`evidencia/C_intento1_error_laboratorio`) |
| Intento 2 (01:42) | 12 predicciones; **7 respuestas perdidas** por un 502 del proxy a los 30 s, con la espera síncrona de 60 s (`evidencia/C_intento2_proxy_502`) |
| **Intento 3 (01:50–02:29)** | **Válido.** Con `prefer-wait-seconds=25` (anexo 2): 35 predicciones, 0 errores del proveedor (`evidencia/C`) |

## Configuración del intento válido

| Elemento | Valor |
|---|---|
| Backend | `generation` `0974fb3`, sin cambios |
| Laboratorio | `d289dce` |
| Endpoint | `https://api.replicate.com/v1/models/openai/gpt-5-structured/predictions` |
| Modelo | `gpt-5-mini` (`model_variant` en las métricas de Replicate) con esfuerzo `medium`. **Sin cambios durante el experimento** |
| *Prompt* y contratos | Los vigentes del backend; no se tocaron |
| Credencial | Secreto de red inyectado por el proxy. El laboratorio no la ve; el backend recibe un marcador. Antes de empezar, la consulta gratuita `GET /v1/account` dio 200 |
| Espera síncrona | `fitsense.ai.prefer-wait-seconds=25` (anexo 2; por defecto 60), después el sondeo del backend (cada 3 s, hasta 150 s) |
| Intentos por operación | 3, política del backend. El laboratorio no reintenta nada |
| Límite | 78 predicciones en esta ejecución: el total autorizado (90) menos las 12 del intento 2. El laboratorio cuenta antes de llamar |
| Base | PostgreSQL 16 efímero en `localhost`, comprobado antes de la primera llamada |
| Comparación justa | U01–U20 se crean con la generación por IA desactivada. **Su plan inicial es idéntico byte a byte al de A1** (400 filas de `plan_inicial.csv`), de modo que la adaptación parte de la misma copia. Los 20 gemelos (`U01G`–`U20G`) miden la generación inicial con IA |
| Evidencia del proveedor | `evidencia/C/predicciones_replicate/`: las 35 predicciones con su id, estado, salida y métricas (tokens y tiempo) |
