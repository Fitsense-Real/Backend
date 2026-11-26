# Sensibilidad: G08 réplica 2, brazo B (informativo, NO decide)

- En la corrida REAL, la llamada de G08 r2 B terminó en `ERROR_PROVEEDOR` en el backend (`AiProviderUnavailableException: … Error while extracting response for type [java.lang.String] and content type [application/octet-stream]`), sin id de predicción. Según la regla previa (`04` §7), **cuenta en K6, sale de P2, conserva su reserva y no se repite**.
- Un listado gratuito (`GET /v1/predictions`) muestra que la predicción **sí se creó y terminó** en Replicate: `g40bmfgq8nrzr0d236ktnedbfm`, creada a las 20:58:30.597 (la reserva es de las 20:58:30.117), `succeeded`, `predict_time` 54,96 s, mismo modelo y misma entrada (33.653 + 2.189 = 35.842 caracteres, los de G08 B con PR-2.2). Hay que contarla como **probablemente facturada**.
- Su salida se recuperó por GET (sin coste) y se validó con el mismo validador (modo `VALIDAR`): **VÁLIDA**.
- Con ella dentro: válidas 30 frente a 46; perfiles con V15 de la mitad o V26 8 frente a 3; mediana de `predict_time` 66,3 frente a 75,2 s (límite 72,9); errores 0 frente a 0. **El veredicto no cambia: DESFAVORABLE (P1 y K2).**
