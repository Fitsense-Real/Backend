# C-2: herencia limitada de D8 en V8C

## 1. La inconsistencia (campaña p123, N03 S4)

1. N03 (LOSE_WEIGHT principiante, 30 min) acortó en S3 su sesión del lunes con ACORTAR: retiró el relleno 1117 y la sesión quedó en 15 min, con una autorización D8 válida (exenta de V20).
2. En S4 esa sesión se omitió por motivación y se señaló con INVESTIGAR_CAMBIO_MINIMO.
3. La IA (simulada) devolvió **exactamente** la misma sesión.
4. V8C no le dio la exención D8. Solo se la daba a las señaladas cuya intervención permite acortar, o a las copias no señaladas. V20 la rechazó 3 veces.
5. El respaldo publicó **esa misma sesión**, con D8, porque como copia no señalada sí la heredaba.

**Resultado:** el mismo contenido era válido por un camino e inválido por el otro. En producción serían hasta 3 llamadas pagadas desperdiciadas.

## 2. La corrección (mínima, en `ConservationValidator`)

Una sesión señalada **hereda** la reducción autorizada de su origen (no se crea ninguna autorización nueva) solo si se cumple **todo** esto:

| Condición | Cómo se comprueba |
|---|---|
| Existe una autorización D8 previa con linaje reconstruible | `previous.reduccionAutorizada()` contiene la sesión de origen (V31 + V32, `ReduccionAutorizadaResolver`) |
| La propuesta es idéntica | La sesión queda **SIN_EFECTO**: mismos ejercicios, dosis (tipo, series, repeticiones y segundos), descansos, fecha, enfoque, minutos y carga; ningún problema |
| Mismo orden de ejercicios | `mismoOrden` (la comparación de V8C no miraba el orden) |
| Ninguna subida de dosis rechazada | `!dosisRechazada` |
| Sin dolor en la sesión | Ni dolor de ejercicio ni de sesión registrado (`segura` y la ausencia de dolor) |
| Ningún ejercicio excluido por seguridad | Ni por el catálogo, ni por retenciones, ni por P-02 |
| Ninguna sustitución por seguridad en la sesión | `admitidas` vacía para ese origen |
| La intervención admite SIN_EFECTO | No es SEGURIDAD_ANTE_DOLOR (`exigeRevisionDeSeguridad`) |

**Lo que no cambia:** V20, el resto de V8C, la progresión, la reincorporación y las dosis.

## 3. Casos

| Caso | Resultado | Prueba |
|---|---|---|
| A: D8 previa, señalada y propuesta idéntica (INVESTIGAR, REVISAR_CARGA, VARIANTE) | Conserva D8; SIN_EFECTO, publicable | `HerenciaReduccionV20Test.identicaConservaD8` |
| D8 previa y respaldo idéntico | Conserva D8 (como antes) | `respaldoIdenticoConservaD8` |
| B: una repetición menos | Sin excepción: V20 rechaza | `repeticionCambiada` |
| B: descanso distinto | Sin excepción | `descansoCambiado` |
| B: ejercicio cambiado | Sin excepción | `ejercicioCambiado` |
| B: mismo contenido en otro orden | Sin excepción | `ordenCambiado` |
| C: sin D8 previa, sesión corta idéntica | V20 actúa: no se crea exención | `sinD8PreviaRigeV20` |
| D: dolor en un ejercicio conservado | Rechazada (V8C, lo que dolió no se repropone); sin D8 | `dolorEnConservado` |
| E: SEGURIDAD_ANTE_DOLOR con copia idéntica | No aceptada | `intervencionQueExigeCambio` |
| Copia multisemana de la sesión acortada | Linaje válido y D8 en cada copia | `copiaAutorizadaSePublica`, `HerenciaD8IT` |
| Validación independiente | VALIDA_CON_AUTORIZACIONES | `C2ReproduccionN03IT`, `HerenciaD8IT` |

## 4. Reproducción del caso original

`C2ReproduccionN03IT` recrea a N03 con el **mismo `user_id`** (4), así que usa la misma semilla del motor de reglas. Ejecuta S1–S4 con la IA simulada de la campaña.

| | Sin la corrección | Con la corrección |
|---|---|---|
| Estado V32 de S4 | RESPALDO | **SIN_EFECTO** |
| Llamadas a la IA | 3 | **1** |
| D8 en la copia | Sí (por el respaldo) | Sí (por identidad) |
| Validación independiente | — | VALIDA_CON_AUTORIZACIONES |

**Ambas regresiones fallan sin la corrección** (`identicaConservaD8` y `C2ReproduccionN03IT`, comprobado desactivando la rama) y pasan con ella.

**Pruebas actualizadas al comportamiento aprobado:**
- `HerenciaReduccionV20Test.intervencionNuevaInterrumpe` se sustituyó por los casos A–E: codificaba justo la regla que la orden cambia.
- `HerenciaD8IT.propuestaIdenticaConservaD8` pasa de RESPALDO a **SIN_EFECTO**.

## 5. Efecto en la campaña (repetición determinista completa)

**La única diferencia en las 139 semanas es N03 S4:** RESPALDO_PROPUESTA_IA_INVALIDA con 3 llamadas pasa a SIN_CAMBIOS_VALIDOS con 1. La semana publicada es la misma.

Respuestas simuladas: de 16 a 14 en total, de 14 a 12 en las 100 semanas.
