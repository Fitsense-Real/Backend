# Contrato: `DELETE /api/v1/users/me`

Elimina la cuenta de la persona autenticada y todos sus datos (alternativa A de
[POLITICA_ELIMINACION_PROPUESTA](POLITICA_ELIMINACION_PROPUESTA.md)). Es irreversible.

## Petición

```http
DELETE /api/v1/users/me
Authorization: Bearer <jwt>
Content-Type: application/json

{"password": "<contraseña actual>", "confirm": true}
```

| Campo | Obligatorio | Regla |
|---|---|---|
| `password` | sí | Contraseña actual de la cuenta. Reautenticación: un JWT robado o un móvil desbloqueado no bastan |
| `confirm` | sí | Debe ser `true` |

- No hay identificador en la ruta: solo se puede eliminar la cuenta del token.
- Ni la contraseña ni el cuerpo se registran en logs.

## Respuestas

En todas las respuestas de éxito: `Cache-Control: no-store`.

**200: eliminación completada.** No queda ninguna fila de la persona.

```json
{"status": "DELETED", "deletionId": "3f0c…", "requestedAt": "2026-11-19T20:07:57Z", "completedAt": "2026-11-19T20:07:57Z"}
```

**202: aceptada, en proceso.**
- La cuenta ya está cerrada: no admite login ni tokens, y no hay generaciones en curso.
- El borrado falló de forma transitoria (por ejemplo, un bloqueo) y se completa automáticamente; la tarea se reintenta cada 5 minutos.
- No se afirma la eliminación: falta `completedAt`.

```json
{"status": "PENDING", "deletionId": "3f0c…", "requestedAt": "2026-11-19T20:07:57Z"}
```

**200 o 202 con `"alreadyRequested": true`: ya estaba registrada.** Solo ocurre con dos solicitudes simultáneas. Devuelve la misma `deletionId` y no crea otra.

**Errores.** El cuerpo es el `ApiErrorResource` de siempre, y `details[0]` lleva el código estable.

| HTTP | `details[0]` | Cuándo | ¿Cambió algo? |
|---|---|---|---|
| 400 | `CONFIRMACION_REQUERIDA` | Falta `confirm` o es `false` | No |
| 400 | — | Cuerpo ausente o JSON inválido | No |
| 401 | — | Sin token, token inválido o caducado, o cuenta ya eliminada o en eliminación | No |
| 403 | `REAUTENTICACION_FALLIDA` | Contraseña incorrecta o ausente | No |
| 429 | `DEMASIADOS_INTENTOS` | 5 contraseñas incorrectas en 15 minutos; cabecera `Retry-After` | No |
| 404 | `CUENTA_INEXISTENTE` | Carrera: otra solicitud la eliminó entre el filtro y el bloqueo | Ya estaba eliminada |
| 503 | `ELIMINACION_NO_REGISTRADA` | Falló el registro (base no disponible, bloqueo de más de 5 s) | No: se puede repetir |

Ejemplo de error:

```json
{"timestamp": "2026-11-19T20:07:58Z", "status": 403, "error": "Forbidden",
 "message": "La contrasena no es correcta. La cuenta no se ha eliminado.", "details": ["REAUTENTICACION_FALLIDA"]}
```

## Qué debe hacer la app (Flutter)

1. Pantalla de confirmación con el texto aprobado. Debe separar "retirarme del estudio" de "eliminar mi cuenta y mis datos".
2. Pedir la contraseña y enviar `confirm: true` solo tras la acción explícita.
3. Con **200** o **202**:
   - borrar el JWT y los datos locales;
   - volver a la pantalla de inicio;
   - con 202, el mensaje dice que la eliminación se está completando, no que ya terminó.
4. Con **401** tras la solicitud: tratarlo como sesión terminada.
5. Con **403** o **429**: mostrar el mensaje; con 429, esperar `Retry-After`.
6. Con **503**: ofrecer reintentar.

## Configuración

| Propiedad | Por omisión | Qué controla |
|---|---|---|
| `fitsense.account-deletion.max-failed-reauth` | 5 | Contraseñas incorrectas permitidas por ventana |
| `fitsense.account-deletion.reauth-window-minutes` | 15 | Ventana del límite (en memoria: una instancia) |
| `fitsense.account-deletion.retry-delay-ms` | 300000 | Cada cuánto se reanudan las eliminaciones PENDIENTE |
| `fitsense.account-deletion.retry-initial-delay-ms` | 60000 | Primera reanudación tras el arranque |

## Pruebas

`EliminacionDeCuentaIT`, con PostgreSQL 16 efímero, datos sintéticos y sin Replicate:

| Caso | Qué se comprueba |
|---|---|
| `eliminacionCompleta` | Una cuenta con plan, sesiones, dolor, métricas, adaptación, recuperación y libro de predicciones queda con 0 filas en todas las tablas con `user_id` (leídas del esquema) y en sus dependientes. Además: <ul><li>el JWT da 401 y el login también;</li><li>la otra cuenta queda intacta;</li><li>el registro no guarda datos personales.</li></ul> |
| `confirmacionYReautenticacion` | <ul><li>400 sin confirmación o sin cuerpo;</li><li>403 con contraseña mala;</li><li>429 al quinto fallo, incluso con la buena;</li><li>nada cambia.</li></ul> |
| `soloLaPropia` | 401 sin token o con un token inválido; un token solo elimina su propia cuenta |
| `concurrentes` | 4 solicitudes a la vez: una sola eliminación, al menos un 200 y ningún error distinto de 202, 401 o 404 |
| `interrumpidaYReanudada` | El borrado falla (disparador de prueba): <ul><li>202 PENDING y la cuenta ya no entra;</li><li>la generación en curso queda FAILED / `CUENTA_ELIMINADA` y `execute` no publica;</li><li>la reanudación lo completa.</li></ul> |
| `codigoDeParticipanteNoSeReutiliza` | Tras eliminar cuentas, los P-xxx nuevos no repiten ni heredan códigos |
| `ningunaTablaSinCubrir` | Guarda del esquema contra tablas nuevas sin cubrir |
