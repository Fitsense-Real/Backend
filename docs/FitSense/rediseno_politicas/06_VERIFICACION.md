# 06 · Verificación sin Replicate (commit de código 30f1647)

**Alcance:**
- Sin Replicate y sin IA real: `llamadas_proveedor=0` y `usd_medido=0.0000` en todas las corridas K20.
- Sin Neon.
- Base: PostgreSQL efímero 16 (Testcontainers), Flyway 44.3, 47 migraciones (checksum `666694b7…`), catálogo `425fc958…` con 1.219 ejercicios activos.
- La configuración de cada corrida está en su `00_configuracion.txt`, dentro de `evidencia/k20/<corrida>.tar.gz`.

## 1. Pruebas

| Bloque | Resultado |
|---|---|
| Unitarias completas (`mvn test`) | **1.263 ejecutadas, 0 fallos**, 1 omitida |
| Pruebas nuevas | `PoliticaRedisenoTest` 8/8 (catálogo, V20, V4, V26, V26 en semana derivada, V27, división P-G3, dosis). `RedisenoAdaptacionTest` 17/17 (P-A1, P-A2, P-C2 en V8C, P-P1, H7, H14). `PromptRev2Test` 5/5. `PromptHistoricoHuellaTest` 18/18 (prompt histórico idéntico byte a byte). `ImpactoPolicyTest`. `ControlPresupuestoK20Test` 7/7 (P-H1) |
| Regresión completa (`verify -Pintegration`, política por defecto `PLAN-HIST-1`) | Ver §6 |
| IT de progresión y linaje **con REV2** (progresión encendida y apagada) | 63 ejecutadas, **59 OK y 4 fallos explicados** (§4) |

## 2. Reproducibilidad e identidad de la política histórica

| Comprobación | Resultado |
|---|---|
| Barrido HIST de 3.780 perfiles sobre el commit final, frente a la evidencia | **Idéntico byte a byte** (3.641 válidos; los mismos planes) |
| Barrido REV2 de 3.780 perfiles sobre el commit final, frente a la evidencia | **Idéntico** (3.776 válidos; los mismos 76.105 renglones de planes) |
| K20 HIST, modo D, en U, J y K: huella frente a la congelada | U `f9f67973…` (20 PASS), J `276d535d…` (15 PASS y J03 FAIL, el histórico), K `54a5bda0…` (16 PASS): **iguales a las anteriores** |
| K20 REV2, D frente a D′ (dos corridas) | U `cdea34d7…`, J `7b1b94c6…`, K `6f6256f5…`: **iguales entre sí**, e iguales a las de antes de integrar adaptación y progresión |
| Prompt histórico | 18 huellas SHA-256 idénticas |

## 3. K20 por política y cohorte

Oráculos: el histórico, sin modificar, y el versionado (`03_ORACULO_VERSIONADO.md`).

| Corrida | Oráculo histórico | Oráculo REV2 | Huella |
|---|---|---|---|
| HIST D · U | 20 PASS | 20 PASS | f9f67973… |
| HIST D · J | 15 PASS · 1 FAIL (J03 sin plan) | 10 PASS · 6 FAIL (J03; J05, J06, J09, J12 y J15 por volumen V26O) | 276d535d… |
| HIST D · K | 16 PASS | 14 PASS · 2 FAIL (K06 y K13 por volumen) | 54a5bda0… |
| **REV2 D/D′ · U** | 10 PASS · 10 FAIL (solo V20) | **20 PASS** | cdea34d7… |
| **REV2 D/D′ · J** | 7 PASS · 9 FAIL (solo V20) | **16 PASS** (J03 incluido) | 7b1b94c6… |
| **REV2 D/D′ · K** | 10 PASS · 6 FAIL (solo V20) | **16 PASS** | 6f6256f5… |
| HIST IA simulada · U | 20 PASS | 20 PASS | c0eab17e… |
| HIST IA simulada · J | 15 PASS · 1 FAIL | 10 · 6 | eed24540… |
| HIST IA simulada · K | 16 PASS | 14 · 2 | afb57214… |
| **REV2 IA simulada · U** | 10 · 10 (solo V20) | **20 PASS** | 5825140e… |
| **REV2 IA simulada · J** | 7 · 9 (solo V20) | **16 PASS** | ef996d97… |
| **REV2 IA simulada · K** | 9 · 7 (solo V20) | **16 PASS** | 7666ed3d… |

**Nota sobre K:** se usó para corregir en la auditoría anterior, así que **no se presenta como cohorte no vista**. La cohorte independiente es L (`07_COHORTE_L.md`).

### 3.1 Los 9 escenarios antes no factibles (IA simulada)

| Escenario | HIST: sesión no factible (motivos de la sonda) | REV2: adaptación aplicada |
|---|---|---|
| U03 S3 · GM · fatiga | 0 válidas (V20 = 7, V24 = 14) | **MANTENIMIENTO**: 6 estructurales 3 → 2 series (P-A1) |
| U04 S3 · GM · desmotivación | 0 (V20 = 7, V24 = 14) | **1 sustitución** lateral 1058 → 72 (P-A2) |
| J01 S4 · GM · fatiga | 0 (V16, V20, V24, V9) | MANTENIMIENTO: 1265 y 567 3 → 2 |
| J09 S4 · desmotivación | 0 (V16, V20, V24, V9) | 1 sustitución 1196 → 327 |
| J13 S3 · desmotivación | 0 (V24 = 13, V9 = 2) | 1 sustitución 1262 → 460 |
| K01 · GM · fatiga | 0 (V16, V20, V24, V9) | MANTENIMIENTO: 877 y 1264 3 → 2 |
| K09 · desmotivación | 0 (V16, V20, V24, V9) | 1 sustitución 754 (d3) → 163 (d3) |
| K10 · desmotivación | 0 (V20 = 4, V9 = 8) | 1 sustitución 56 → 103 |
| K13 · desmotivación | 0 (V20 = 8, V24 = 16) | 1 sustitución 1058 → 460 |

**Verificación independiente** (script sobre las tablas exportadas, no sobre el backend): las **37 sustituciones** aplicadas por REV2 con IA simulada en U, J y K tienen el **mismo patrón** y el **mismo grupo**, **no suben la dificultad** y **no suben el impacto**. Ninguna incumple.

### 3.2 Sustituir 1 ejercicio frente a quitar 1 repetición (P-A2)

Sesiones `INVESTIGAR_CAMBIO_MINIMO` de U, J y K con REV2 (sonda de factibilidad: todas las candidatas simples, juzgadas por el productor real):

| | Sesiones | Con al menos 1 sustitución válida | Con al menos 1 «−1 rep» válida |
|---|---|---|---|
| U | 4 | 4 | 1 |
| J | 8 | 8 | 3 |
| K | 8 | 8 | 2 |
| **Total** | **20** | **20** | **6** |

- Ningún sustituto **más difícil** se valida: 0 de 512 propuestos.
- En GAIN_MUSCLE, V24 rechaza «−1 rep» ante desmotivación.
- **Conclusión:** la sustitución es la opción factible en todos los casos y la que tiene respaldo (`02_EVIDENCIA_DOSIS.md` §5). Quitar una repetición solo es posible en 6 de 20 y no cambia la experiencia.

### 3.3 Pruebas negativas de la IA simulada (intento 1, siempre inválido)

En cada usuario con IA, el primer intento propone una de tres cosas a propósito: subir una serie, tocar una sesión no señalada o un sustituto más difícil.

**Resultado:** en U, J y K, con las dos políticas, **ninguna se publicó**: 79 de 79 intentos 1 dieron `publicable=false`. La publicación final viene del intento 2, que es válido (79 de 79).

## 4. IT de progresión y linaje con REV2

Con REV2 pasan:
- ProgresionIT: 24/24, progresión encendida.
- ProgresionCienPorCienIT: 5/5.
- ProteccionPlanesPublicadosIT.
- ReparacionLinajeIT: 1/1 (P-P2 y H6).
- RedisenoFatigaGmIT: 2/2, progresión apagada (recuperación en las semanas 4, 6 y 8; avisos a las 2 y 3 semanas).

Los 4 fallos se analizaron uno a uno. **No son defectos de REV2.** Con la política por defecto pasan, y no se han modificado para que pasen con REV2.

| Prueba | Qué espera | Qué pasa con REV2 | Clasificación |
|---|---|---|---|
| PendientesProgresionIT.sinProgresionNoReincorpora (GYM y CASA) | LW principiante de 30 min «no puede dar ningún paso» tras acortar: la sesión se queda igual | Con la progresión encendida da pasos `PROGRESION` 8 → 9 → 10 repeticiones dentro del techo (1,10 × base) | Premisa de HIST (sesiones al límite de V4 por el piso del 70 %). REV2 deja margen. Sin reincorporación ni restauración |
| LinajeIntegridadIT.linaje (punto F) | Sin evidencia V32, **V20 (70 %)** rechaza la copia de un acortado | REV2 no tiene piso del 70 % y publica la copia | Esperado: es la regla que P-G1 retira. El resto de la prueba (detección de la derivación sin intento) pasa |
| CopiaSinIaIT.retiradoNoElegibleNoVuelve | Desactiva en el catálogo el ejercicio retirado | En el plan REV2 ese ejercicio (253, cardio) sigue en **otra** sesión. La copia falla V1 y queda NO_FACTIBLE, sin publicar nada inactivo | Premisa de la composición HIST. Además destapa una limitación previa (L7, `08_LIMITACIONES_Y_PROPUESTAS.md`) |

## 5. P-H1 (arnés; sin consumir Replicate)

| Caso | Resultado de `ControlPresupuestoK20Test` |
|---|---|
| `failed` sin métricas | Protocolo congelado: bloquea. P-H1: carga la reserva y sigue |
| `canceled` sin métricas (caso J08) | P-H1 sigue; con BLOQUEAR, bloquea |
| Timeout con la predicción en curso o en estado desconocido | Bloquea siempre (coste no acotado). El arnés hace `POST /cancel` |
| Respuesta recibida sin métricas | Bloquea aun con P-H1 |
| Con métricas | Coste por tokens; por encima de la reserva, bloquea |
| Gasto cerca del límite | No llama si el gasto medido más la reserva supera el tope, ni por encima del tope de predicciones |
| Dos fallidas seguidas | Cargan 0,10 USD de reserva; la tercera llamada solo si cabe |

**Replicate sigue NO AUTORIZADO.** Ninguna prueba hace red.

## 6. Regresión completa

`sh ./mvnw -o -B -ntp verify -Pintegration` sobre `3701b8c` (código idéntico a `30f1647`), con la política por defecto `PLAN-HIST-1`:

| Fase | Resultado |
|---|---|
| Unitarias (surefire) | **1.263 ejecutadas, 0 fallos, 0 errores**, 1 omitida |
| Integración (failsafe, Testcontainers) | **528 ejecutadas, 0 fallos, 0 errores**, 43 omitidas (laboratorios opcionales: barridos, K20 y campañas que exigen su propiedad) |
| Construcción | **BUILD SUCCESS** |
