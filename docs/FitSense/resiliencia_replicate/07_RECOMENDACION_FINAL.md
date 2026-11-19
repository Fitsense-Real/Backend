# 07 · Recomendación final

## ¿Está la integración de Replicate preparada para un entorno controlado de pruebas?

**Sí, técnicamente, una vez fusionadas las PR #49, #50, #51 y #52**, y con las condiciones operativas de la §3.
- **No** para usuarios reales sin las validaciones pendientes.
- **No** para despliegue mientras siga abierto el riesgo de Neon (trabajo independiente).

| Problema | Estado | Evidencia |
|---|---|---|
| R-1 Respuestas inválidas | **Corregido** (#49) | 9 pruebas fallaban antes; una respuesta ilegible llega al respaldo validado; ningún plan inválido se publica |
| R-2 Predicciones tras el timeout | **Corregido dentro de lo que permite Replicate** (#50) | Cancelación única, registrada y sin respuestas tardías. Coste cero **no** garantizado |
| R-3 Timeouts HTTP | **Corregido** (#49) | Límites explícitos y probados con servidores locales; espera máxima conocida (~293 s) |
| R-4 Estados de adaptación | **Corregido** (#51) | `RESPALDO_SIN_IA` cuando el proveedor no devolvió nada; sin contrato nuevo |
| R-5 Duplicados y consumo | **Corregido** (#52), con un límite declarado | Exclusión entre instancias (PostgreSQL); límite de frecuencia por instancia |

## Compatibilidad con Railway (revisada, sin desplegar)

| Aspecto | Estado |
|---|---|
| Configuración HTTP | Propiedades nuevas con valores por omisión y variables `FITSENSE_AI_CONNECT_TIMEOUT_SECONDS`, `FITSENSE_AI_READ_TIMEOUT_SECONDS`, `FITSENSE_AI_POLL_READ_TIMEOUT_SECONDS`. Detrás de un proxy que corte antes de 90 s hay que bajar `FITSENSE_AI_PREFER_WAIT_SECONDS`: el límite de lectura del POST se ajusta solo |
| Variables de entorno | Nueva: `FITSENSE_AI_GENERATE_MAX_PER_HOUR` (5). Ninguna obligatoria |
| Identificadores de predicción | Solo en el log (`REPLICATE_TIMING`, `REPLICATE_CANCEL`). No se persisten: no hace falta migración |
| Persistencia de generaciones | Sin cambios de esquema |
| Concurrencia entre instancias | Exclusión con bloqueo consultivo de PostgreSQL. Requiere la URL **directa** de la base, no el *pooler* en modo transacción, igual que el ciclo semanal (`fitsense.cycle.lock.url`) |
| Manejo de errores | 409, 429 (con `Retry-After`) y 503 nuevos en `POST /users/me/plan/generate`. La app cliente debe tratarlos (reintentar más tarde) |
| Circuit breaker | Sin cambios: 3 fallos y 10 min, por instancia y en memoria. `INVALID_RESPONSE` cuenta |
| Control de gasto | Solo fuera de la aplicación: límite y alertas en la cuenta de Replicate (`05`) |
| Sin `REPLICATE_API_TOKEN` | Sin cambios: la IA no se llama; motor de reglas y respaldo |

## Lo que queda antes del despliegue (limitaciones reales)

1. **Fusionar las PR**, en el orden: #49 → #50 (redirigida a `generation`) → #52 y #51. Después, regresión y CI sobre `generation`.
2. **Riesgo de Neon** (independiente; ver `preparacion_despliegue/01`): la URL de la base fijada en el código y Flyway activo. **Bloquea el despliegue** hasta corregirlo.
3. **Cuenta de Replicate:** límite de gasto, alertas y un token exclusivo del entorno de pruebas. La aplicación **no** puede garantizar un tope de gasto.
4. **Límite de frecuencia por instancia:** con varias instancias el límite se multiplica. Si se escala, añadir un contador común (tabla y migración), en una orden aparte.
5. **Cancelación:** Replicate puede facturar lo ya procesado. Si el POST se corta antes de devolver el identificador, no se puede cancelar.
6. **POST que envía datos muy despacio:** el límite de lectura no acota un servidor que manda un byte cada pocos segundos indefinidamente. El sondeo sí está acotado.
7. **La app móvil** debe mostrar los nuevos 409, 429 y 503 de forma comprensible. No se ha tocado `mobileapp`.
8. **Validación con Replicate real:** ninguna de estas correcciones se ha probado contra Replicate (USD 0). La primera ronda del entorno de pruebas debería revisar las líneas `REPLICATE_TIMING` y `REPLICATE_CANCEL`.
