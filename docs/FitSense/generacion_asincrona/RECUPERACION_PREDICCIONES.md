# Registro y recuperación de predicciones (V59)

**No se declara exactamente-una-vez.** Lo que se demuestra con pruebas:

- **como máximo una predicción nueva por intento y operación** mientras su envío conste en la base;
- **ninguna predicción reenviada** cuando el envío quedó incierto.

El caso en que Replicate acepta y el id nunca llega es un riesgo residual acotado y contabilizado, no eliminado.

## 1. El libro (`plan_generation_predictions`)

Una fila por predicción. Guarda:

- la operación (FK), el usuario (FK), el intento y la huella SHA-256 de la petición;
- el estado y el `replicate_prediction_id`;
- `reused_from_row_id`, cuando se adopta;
- la salida, el error (≤ 500 caracteres), las marcas de tiempo y el número de consultas.

No guarda el token, las cabeceras ni el prompt.

| Estado | Significado |
|---|---|
| `SOLICITANDO` | Anotada **antes** del POST. No se sabe aún si Replicate la creó. |
| `ENVIADA` | Replicate devolvió su id (guardado antes de sondear) |
| `SUCCEEDED` | Salida recibida (la base exige id y salida) |
| `FAILED`, `CANCELED`, `TIMEOUT` | Final del proveedor o de nuestra espera |
| `ERROR_ENVIO` | No llegó a crearse: 4xx, o un fallo de conexión antes de enviar |
| `NO_CONFIRMADA` | Pudo crearse y no se sabe su id. **No se reenvía.** |
| `DESCARTADA` | La petición del intento cambió (otro perfil): queda como historial |

Restricciones en la base:

- `ux_pgp_intento_vigente (operation_id, attempt)`: un intento vigente por operación;
- `ck_pgp_enviada_con_id`;
- `ck_pgp_succeeded_con_salida`.

## 2. Escenario obligatorio de la orden

Prueba `ReanudacionConReplicateSimuladoIT.dosInstanciasNoDuplicanElTrabajo`, junto con `reanudacionNoPagaOtraVez`.

| Paso | Qué ocurre | Evidencia |
|---|---|---|
| 1. FitSense crea una operación | `POST` y luego `PENDING` | — |
| 2. Se pide una predicción | Fila `SOLICITANDO` y POST sin `Prefer: wait` | `nuevaPrediccionAnotadaAntesDeEnviar` |
| 3. Se persiste su id | `ENVIADA` con `p1` antes de sondear | ídem |
| 4. El backend se reinicia | El lease de A vence | — |
| 5. Otro worker la recupera | B barre y reclama: `PENDING` y después `PROCESSING` de B | — |
| 6. Consulta la predicción existente | B hace GET a `p1`; **0 POST nuevos** para el intento 1 | `GET_ESPERANDO = 2`, `POSTS = 1` |
| 7. Recupera el resultado | `SUCCEEDED` | `terminoDuranteElReinicio`, `reanudaPorIdentificador` |
| 8. Continúa la validación | Los validadores de siempre; intentos 2 y 3 | — |
| 9. Publica un solo plan válido | 1 plan, `COMPLETED`, sin lease | `planesDe(u) = 1` |

**No se inicia otra predicción si hay una recuperable.** Lo confirman:

- `reanudacionNoPagaOtraVez`: 3 POST, no 6;
- `dosReiniciosSiguenSiendoTres`: 3, no 9;
- `muereDuranteLaPublicacion`: 3;
- `reintentoManualAdopta`: 0 nuevos.

## 3. Caso crítico: el resultado del POST es incierto

### 3.1 ¿Admite Replicate idempotencia o una reconciliación fiable?

- **Idempotencia al crear.** No se pudo verificar: `api.replicate.com` y `replicate.com` no son accesibles desde
  este entorno, y la documentación consultada en órdenes anteriores no describe ninguna cabecera de idempotencia
  para `POST /v1/models/{owner}/{model}/predictions`. No se asume que exista.
- **Reconciliación.** Replicate ofrece `GET /v1/predictions`, el listado paginado de las predicciones de la cuenta
  con su `created_at`. **No** está verificado que devuelva la entrada (`input`) de cada predicción, que sería
  necesario para asociar con certeza una predicción a nuestra fila. Por eso no se automatiza: asociar por la hora
  podría adoptar la predicción de otra persona.

### 3.2 Recuperación conservadora (implementada y probada)

| Requisito | Cómo se cumple | Prueba |
|---|---|---|
| Registrar el estado incierto | 5xx, lectura cortada, 2xx sin id o un proceso muerto en `SOLICITANDO` pasan a `NO_CONFIRMADA`, con `requested_at` y el motivo | `cincoXXEsIncierto`, `corteDeLecturaEsIncierto`, `aceptadaSinIdNoSeReenvia` (unitaria y E2E) |
| No reenviar automáticamente | `NO_CONFIRMADA` se repite como fallo del proveedor y el pipeline publica su respaldo validado | E2E: 1 POST antes y después del reinicio |
| Permitir la conciliación | Si el id llega tarde (el proceso anterior seguía vivo), se guarda en la fila **sin** cambiar su estado. La fila y su hora quedan para conciliarla con el panel de Replicate. | `JdbcDiarioDePredicciones.enviada` |
| Exponerlo de forma trazable | Log `REPLICATE_NO_CONFIRMADA` (WARN), recuento `no_confirmadas` por operación (`GENERACION_ASINCRONA_PREDICCIONES`) y la consulta SQL de `RIESGO_COSTOS_DUPLICADOS.md` | — |
| Carrera entre dos ejecuciones | Si otra ejecución anotó el mismo intento con la misma petición entre la lectura y la anotación, el `INSERT` choca con `ux_pgp_intento_vigente` y se **sigue la suya**: GET si ya tiene id; `NO_CONFIRMADA` si aún no, y entonces no se envía otra | `carreraConOtraEjecucionEnviada`, `carreraConOtraEjecucionSinConfirmar`, `libroNoAdmiteDosVigentesDeLaMismaPeticion` |

### 3.3 Cuándo puede autorizarse un nuevo intento

- **Dentro de la misma operación: nunca de forma automática.** Un `NO_CONFIRMADA` se repite en cada reanudación.
- **En una operación nueva:** solo por una acción explícita del usuario sobre una operación `FAILED` reintentable.
  Está acotada por el límite de 5 generaciones por hora (R-5) y los 3 intentos.
  - En la práctica, un `NO_CONFIRMADA` termina en `COMPLETED` con el respaldo validado. Con un plan ya publicado no
    hay segunda generación.
  - Solo si además el respaldo falla (`FAILED`), el reintento del usuario puede pagar una predicción nueva.
- **Conciliación manual** (pendiente de autorizar): con la hora de la fila, buscar en el panel o la API de Replicate
  la predicción aceptada y anotar su id. No hace falta para que el usuario reciba su rutina.

## 4. Resultados tardíos

- **La operación venció con su predicción en curso** (`venceDeVerdadDuranteUnaPrediccion`):
  - el worker antiguo anota la salida (`SUCCEEDED`), pero no publica ni paga más;
  - el reintento del usuario la adopta y solo paga los intentos que faltan (1 + 2 = 3).
- **Una fila ya cerrada recibe otro resultado** (`caminoNormalYRepeticiones`): no cambia. Un `SUCCEEDED` es
  inmutable.
- La adopción entre operaciones exige el mismo usuario, la misma huella y menos de 24 h
  (`noSeReutilizaDeOtroUsuarioNiOtraPeticion`, `libroReutilizable`).
