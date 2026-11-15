# Contrato API de la generación asíncrona tras V59

**El contrato HTTP con Flutter no cambia.** No se cambian rutas, códigos, campos ni estados.
`PlanGenerationOperationResource` es idéntico al de #71. Lo nuevo es interno: columnas de lease y la tabla del
libro de predicciones.

## Rutas (sin cambios)

| Método y ruta | Respuesta |
|---|---|
| `POST /api/v1/users/me/plan/generation` | **202** operación nueva `PENDING`. **200** si ya había una activa (la misma) o el plan ya existe (`COMPLETED` con `planId`). **422** sin perfil o no elegible. |
| `GET /api/v1/users/me/plan/generation/{operationId}` | **200** estado. **404** si no existe o es de otro usuario. **400** si el id no es válido. |
| `GET /api/v1/users/me/plan/generation/latest` | **200** última operación. **404** si nunca pidió una. |

## Cuerpo (sin cambios)

```json
{
  "operationId": "uuid",
  "status": "PENDING | PROCESSING | COMPLETED | FAILED | NO_FACTIBLE",
  "terminal": false,
  "weekStartDate": "2026-11-16",
  "planId": null,
  "reasonCode": null,
  "message": null,
  "retryable": false,
  "createdAt": "…", "startedAt": "…", "finishedAt": null,
  "pollAfterSeconds": 3
}
```

No se exponen porcentajes, tiempos estimados, `lease_owner`, ids de Replicate ni el libro.

## Cambios de semántica (mismos valores, distinto momento)

| Aspecto | #71 | V59 |
|---|---|---|
| `FAILED / TIEMPO_EXCEDIDO` en `PROCESSING` | a los 600 s de la ejecución actual | a los 1.800 s desde la **primera** ejecución, nunca por debajo del peor caso |
| `FAILED / EJECUCION_INTERRUMPIDA` | tras 2 ejecuciones interrumpidas | tras 3 |
| `startedAt` | inicio de la ejecución actual | igual. El inicio de la primera ejecución queda interno (`first_started_at`). |
| Un reintento tras `FAILED` reintentable | volvía a pagar todo | reutiliza las predicciones de la misma petición (24 h) |

Flutter ya trata `PENDING`/`PROCESSING` como «sigue esperando» y `FAILED` con `retryable=true` como «puedes
reintentar», así que no necesita cambios.

## Esquema interno (V59)

### `plan_generation_operations`, columnas añadidas

- `lease_owner VARCHAR(80)`;
- `lease_expires_at TIMESTAMPTZ`;
- `first_started_at TIMESTAMPTZ`;
- `ck_pgo_lease` (`NOT VALID`, para no fallar sobre filas antiguas);
- `ix_pgo_lease_vencido`.

### `plan_generation_predictions`, tabla nueva

Contiene una fila por predicción: id interno, operación, usuario, intento, huella, estado, id de Replicate, origen
reutilizado, salida, error, marcas de tiempo y consultas. Restricciones:

- `ENVIADA`/`SUCCEEDED` exigen un id;
- `SUCCEEDED` exige salida;
- índice único del intento vigente.

La migración es aditiva. No toca V1..V58, el catálogo ni V57. Probada desde una PostgreSQL vacía en el CI
(Testcontainers) y en local.
