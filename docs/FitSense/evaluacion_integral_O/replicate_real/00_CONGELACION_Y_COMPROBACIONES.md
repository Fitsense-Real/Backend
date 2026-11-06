# Evaluación integral O con Replicate REAL · 00 · Congelación y comprobaciones gratuitas

**Orden:** «FitSense: evaluación integral de 20 usuarios sintéticos con Replicate REAL».

Este documento se commitea **antes de la primera llamada pagada**. Nada de lo que aquí figura se cambia después de ver resultados.

## Versión evaluada

| Elemento | Valor |
|---|---|
| Código de producción | Versión A `f1c6c02`: `git diff f1c6c02 HEAD -- src/main` está vacío. **Sin cambios en el backend** |
| Catálogo y política | V46 (Flyway 46) · PLAN-REV-2.0 · P15-C apagado · aviso E2-a activo |
| Cohorte | O01–O20, `cohorte/O_PERFILES.csv`, SHA-256 `9b996825…2106` (congelada en `cb18381`). Sin cambios |
| Escenario complementario | O11-F35: `replicate_real/cohorte/O11F35_COMPLEMENTARIO.csv`, SHA-256 `1720227e9191c54af67b08669fcfcaf38444334d8fcc4c245832cdb531b93b54`. Generado por `generar_O11F35.py` |
| Arnés | `EvaluacionK20IT`, solo pruebas. Cambio único: permitir el modo integral con REPLICATE con una autorización propia (`-Dfitsense.evaluacion.k20.integral-replicate=SI`). En REPLICATE no se usa ninguna respuesta simulada; la sonda de factibilidad solo mide y nunca publica |
| Oráculo | **v1.2** (`herramientas/oraculo_integral_O_v1_2.py`). Ver el apartado siguiente |
| Evaluación anterior | Cerrada en `9ae4c49` (IA simulada, oráculo v1.1: 142/142). No se modifica |

**Escenario O11-F35:**
- Es O11 con 35 min en lugar de 30.
- O11 no tiene guion porque se esperaba sin plan. O11-F35 usa cumplimiento total («H») en las 4 semanas, el guion neutro (no provoca adaptaciones).
- Se informa **por separado**: no sustituye a O11, que sigue siendo el control negativo esperado.

## Oráculo v1.2: por qué y qué cambia

**Por qué:**
- v1.1 (congelado en `cc54a3c`) exigía en A5 y A10 que «el intento 1, inválido a propósito, exista y no sea publicable».
- Eso describe a la IA **simulada** del laboratorio. Con Replicate real no hay intento inválido a propósito, y un intento 1 válido es el caso normal.
- Aplicado a respuestas reales, v1.1 daría FALLO precisamente cuando el modelo acierta a la primera. La prueba R1-V lo demuestra.

**Qué cambia (R-1)** — solo cuando la corrida declara `modo: REPLICATE`:
- Se sustituye esa exigencia por esta otra: si el plan publicado proviene de la IA (`generation_source = AI`), al menos un intento de ese ciclo tiene que haber sido juzgado **publicable** por el backend. Nunca se publica una propuesta rechazada.
- Todo lo demás de A5/A10 se aplica igual al plan publicado, sea cual sea la respuesta del modelo:
  - sin más trabajo y sin añadidos;
  - sustitutos del mismo patrón y grupo, con dificultad ≤;
  - el ejercicio con dolor fuera;
  - sesiones no señaladas idénticas;
  - techo, tope de dificultad y prohibidos.

| Elemento | Valor |
|---|---|
| SHA-256 v1.2 | Ver `SHA256SUMS.txt` de esta carpeta en el commit de congelación |

**Pruebas** (`pruebas/pruebas_oraculo_v1_2.csv`, todas pasan):
- **Identidad:** v1.2 = v1.1, byte a byte (matriz, detalle, semanas, adaptaciones, pasos y huella), en las 6 corridas existentes: S_O, S_O′, D_O y HIST originales, y S_O y S_O′ de la repetición.
- **Las 19 pruebas negativas** de v1.1 pasan con v1.2.
- **R1-C** (control): corrida declarada REPLICATE con sus intentos tal cual → A10 APROBADO.
- **R1-V:** intento 1 real válido y publicable → APROBADO con v1.2 y FALLO con v1.1.
- **R1-N** (negativa): adaptación de la IA publicada sin ningún intento publicable → FALLO A10 con v1.2.

**Criterios sin cambios:**
- Los 10 aspectos, los estados y los denominadores.
- La pertinencia (ACC, ALT, CONS-J, INC) se informa aparte, como siempre.
- REQUIERE REVISIÓN, separado de las aprobaciones.

## Comprobaciones gratuitas (arnés `0bbe1c8`, 06/11/2026)

| Comprobación | Resultado |
|---|---|
| Cuenta de Replicate (`GET /v1/account`, gratuito, credencial inyectada por el proxy y no visible) | HTTP 200 |
| Modelo | `openai/gpt-5-structured`, público. **Versión observada antes de ejecutar:** `9f4cd9ec1133f55d442aeb426e42df5180a56e79a33183623611d62d4c3b44ae` (creada el 21/01/2026), la misma de la evaluación U/N |
| ¿Se puede fijar la versión? | **No sin cambiar el backend:** usa el endpoint del modelo (`/v1/models/openai/gpt-5-structured/predictions`), que sirve la última versión. Se registra la versión antes de cada corrida pagada y después de la última (`versiones.txt`), y se advertirá cualquier cambio |
| Parámetros | `gpt-5-mini`, razonamiento `medium` (`application.properties`), prompt V2.1 `FIX-PR-2.1-EXP`, `prefer-wait` 25 s, tiempo máximo 150 s. Los mismos de U/N |
| Unitarias (`mvn test`) | 1306 ejecutadas, 0 fallos, 0 errores, 1 omitida. BUILD SUCCESS |
| Regresión U/N (huellas) | REV2 D U `a662cb4d` · REV2 D N `11f51d44` · HIST D U `f9f67973` · REV2 IA simulada U `84dfb6d2` · REV2 IA simulada N `da34db35`: **las 5 iguales** a la referencia |
| S_O con IA simulada y el arnés nuevo | Huella `7eb13353…`, **igual** a la evaluación cerrada (0 FALLO con v1.2) |
| D_O con el arnés nuevo | Huella `d46b3f50…`, **igual** (0 FALLO) |
| O11-F35 sin IA (D e IA simulada) | Plan publicado de 1 sesión, aviso DOSIS_LIMITADA. v1.2: A1 REQUIERE REVISIÓN (catálogo) y el resto APROBADO o NO APLICA |

## Presupuesto y control de costes

| Elemento | Regla |
|---|---|
| Límite global | **168 predicciones y USD 8,40** (intentos y reintentos incluidos), compartido por todas las corridas pagadas |
| Reparto | El script (`evidencia/correr_RO.sh`) suma lo gastado en las corridas anteriores (predicciones y USD medidos) y pasa a la siguiente solo el **remanente**: `max-llamadas` y `max-usd` |
| Comprobación antes de cada predicción | Gasto medido + reserva de peor caso (USD 0,05) ≤ remanente; si no se cumple, no se llama. Clase pura `ControlPresupuestoK20`, ya probada |
| Gasto real | Leído de las métricas de cada predicción (GET gratuito), a 0,25 / 2,00 USD por millón de tokens |
| Predicción sin métricas | `RESERVA_SIN_BLOQUEO`, la regla de U/N: se carga la reserva. Una predicción que respondió sin métricas bloquea |
| Orden de ejecución | R_O (bloques G y P, luego O11-F35) y después R′_O (igual), solo si queda presupuesto. Si se agota, se detiene y se informa lo ejecutado. **Nunca se completa con simulación** |
| Prohibido | Llamadas extra para mejorar un resultado. Repetir selectivamente |

## Tratamiento de resultados desfavorables

Se investiga cada FALLO, rechazo, SIN_EFECTO inesperado, incumplimiento de seguridad o discrepancia, y se clasifica según la orden (§8):

| Clase | Qué significa | Qué se hace |
|---|---|---|
| A | Defecto del sistema | Corrección mínima con pruebas, versión nueva y corrida nueva si el presupuesto lo permite. Si cambia reglas de negocio: **parar y proponer** |
| B | Defecto del instrumento | Se conserva el resultado y se corrige en otra versión |
| C | Comportamiento válido del modelo | No se toca el sistema |
| D | Limitación del catálogo | REQUIERE REVISIÓN, sin correcciones deportivas |
| E | Problema del proveedor | Se registra; no es un defecto del backend |

**Las corridas originales, sus fallos y sus costes se conservan siempre.**
