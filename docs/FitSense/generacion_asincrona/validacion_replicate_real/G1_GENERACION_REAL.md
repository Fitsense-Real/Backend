# G1 · Generación normal con Replicate real

**Resultado: NO SUPERADO, bloqueado por el proveedor.** El backend no tiene ningún defecto en este escenario: se
publicó el respaldo, que la orden excluye expresamente como criterio de éxito.

## 1. Ejecuciones

| | G1 | G1 (repetición justificada) |
|---|---|---|
| Usuario sintético | `vr-g1-…@pruebas.invalid` (id 1) | `vr-g1b-…@pruebas.invalid` (id 2) |
| Respuesta del POST `/plan/generation` | 202, PENDING | 202, PENDING |
| Operación | `d13991ff-bf4a-4a8e-b7ac-7d7fcc2fd055` | `05b47d64-807a-4606-b064-e78e0342dfd4` |
| Predicción (id guardado en el libro antes de seguirla) | `7dxtdb57znrmy0d1x9kvp0dppc` | `1chmxnn085rmy0d1x9mbgxdhrg` |
| Respuesta de Replicate | `failed` E002 en 0,28 s, 0 tokens | `failed` E002 en 0,25 s, 0 tokens |
| Decisión del pipeline | Fallo del proveedor: no gasta los intentos 2 y 3 y genera el respaldo | igual |
| Estado final | COMPLETED, plan 1 `RULE_ENGINE` | COMPLETED, plan 2 `RULE_ENGINE` |
| Tiempo (solicitud → terminal) | 4,1 s | 2,9 s |
| `GET /plan/current` | devuelve el plan | devuelve el plan 2, con 3 sesiones |
| Predicciones nuevas | 1 | 1 |
| Reintentos de la IA | 0 | 0 |
| Rechazos de validadores | 0 (no hubo propuesta de la IA) | 0 |
| Coste estimado | USD 0,00 | USD 0,00 |

Por qué se repitió una vez:

- el primer fallo fue un error genérico del proveedor;
- en el historial de la cuenta hay fallos transitorios aislados (E9243 y «prediction failed», el 10/11);
- un respaldo no cuenta como superado.

La repetición devolvió el **mismo error con la misma referencia**, así que se dejó de enviar predicciones del backend
(§5 de la orden: un resultado inesperado que hay que investigar).

## 2. Investigación sin coste

Se hizo con consultas gratuitas y una sonda trivial.

1. La entrada que envió FitSense se leyó de Replicate (`GET /v1/predictions/{id}`):
   - campos `model = gpt-5-mini`, `reasoning_effort = medium`, `prompt` y `json_schema`, todos admitidos por el
     esquema publicado del modelo;
   - prompt de 177.198 caracteres, sin caracteres de control ni sustitutos.
2. El tamaño no es la causa: campañas anteriores tuvieron predicciones correctas de hasta 78.776 tokens de entrada.
3. **Sonda mínima.** Mismo modelo y misma variante, el prompt «Devuelve el objeto JSON {"ok": true}», esquema de una
   propiedad y razonamiento mínimo. **Falla igual:** E002, misma referencia, 0,27 s y 0 tokens.
4. **La misma sonda, 31 minutos después.** Sigue fallando.

**Conclusión:** el bloqueo es externo (el modelo en Replicate o la cuenta) y no depende de la entrada de FitSense.

## 3. Lo que G1 sí demuestra con el proveedor real

- **Envío y libro.** El POST sin `Prefer: wait` devuelve el id y queda anotado en el libro (ENVIADA) antes de seguirla.
  El libro coincide 1 a 1 con Replicate.
- **Sondeo.** Un GET por predicción. El estado `failed` se registra como FAILED con el mensaje del proveedor.
- **Ante un proveedor caído:**
  - no se gastan más intentos;
  - la operación no se queda colgada;
  - el participante recibe un plan válido del motor de reglas en ~3 s;
  - el cortacircuitos cuenta los fallos (1 y 2 de 3 seguidos).

## 4. Lo que G1 no demuestra

- que se publique un plan generado por la IA;
- la latencia real de una generación con IA;
- el número de rechazos de validadores con propuestas reales.

Queda **pendiente** hasta que el proveedor responda.
