# 07 · Verificación completa sin Replicate (fase 8)

**Commit verificado:** `7f2da39`, que incluye AUD-G1 y el prototipo P-G2 apagado.
**Condiciones:**
- base efímera con Testcontainers (`postgres:16-alpine`);
- 46 migraciones;
- catálogo `425fc958…`;
- **0 llamadas a Replicate**.

Evidencia: `evidencia/k20/{D,Dp,S}_{U,J,K}/`, `evidencia/barrido*/` y los registros de Maven resumidos en cada carpeta.

## 1. Generación inicial: barrido de 3.780 perfiles

| Versión | Válidos | Sin plan | Por regla | Comentario |
|---|---|---|---|---|
| Base (`2dc4abe`, = congelada) | 3.626 | **154** | V20 139, V15 15 | Reproducible: 3.780/3.780 idénticos en dos ejecuciones |
| Base + 10 semillas por inválido | — | 152 estructurales | — | Solo 2 perfiles se rescatan con otra semilla (GF principiante en casa, 75 min) |
| **AUD-G1** (`7f2da39`) | **3.641** | **139** | V20 139 | +15 (LW de 1 día). **0** perfiles empeoran. Duraciones mínima y máxima idénticas en los 3.626 válidos de base. |
| AUD-G1 + P-G2 solo en el validador (laboratorio) | 3.641 | 139 | V20 139 | Sin efecto: el selector aplica su propio tope de la mitad |
| AUD-G1 + P-G2 completo (laboratorio) | 3.671 | 109 | V20 109 | +30 (LW principiante en casa, 65–75 min; J03 incluido), pero **168 planes válidos de LW cambian** |

### 1.1 Prototipo P-G2 completo (laboratorio; no forma parte de la versión candidata)

Prototipo encendido en el validador (`PlanDraftValidator`), el constructor (`CapabilityWeekBuilder.respetaMitad`) y el selector (`ExerciseSelector.halfCap`), con `-Dfitsense.lab.v15-mitad-prescribibles=true`. Código: commit de laboratorio `a87bccb` (rama local `investigacion/k20-post-rrp-lab`).

- **Rescata 30 perfiles:** LOSE_WEIGHT principiante, casa (con o sin material), 4, 5 o 7 días, 65–75 min. Entre ellos está la familia de J03; el diagnóstico de J03 da un plan VÁLIDO.
- **No es neutral:** **al menos 168 perfiles que ya eran válidos cambian de plan** (detectados por un cambio en su duración mínima o máxima de sesión), todos de LOSE_WEIGHT (principiantes 114, intermedios y avanzados 54). Con la mitad relajada, el selector concentra más trabajo resistido en pierna en las sesiones LOWER.
- **Calidad del plan de J03 con P-G2:** la sesión LOWER queda con 6 de fuerza de pierna (15 series) + 2 cardios. Es válida, pero el volumen de pierna solo existe para llegar al 70 % de V20 (documento 03 §2).
- **No se integra** en la versión candidata: cambia el contrato de V15 y la composición de planes válidos. Su decisión es tuya (P-G2), preferiblemente junto con P-G1.

## 2. Regresión completa

_Se completa al terminar `verify -Pintegration` (§6)._

## 3. D y D′ (modo DETERMINISTA, sin IA) sobre U, J y K

| Corrida | Cohorte | Resultado del oráculo v2 | Huella normalizada | ¿Igual a la congelada? |
|---|---|---|---|---|
| D_U | U01–U20 | **20/20 PASS** | `f9f67973…` | **Sí** (`f9f67973…` en `89a1e4f`) |
| Dp_U | U01–U20 | **20/20 PASS** | `f9f67973…` | Sí |
| D_J | J01–J16 | **15/16 PASS, J03 FAIL** | `276d535d…` | **Sí** (`276d535d…` en `89a1e4f`) |
| Dp_J | J01–J16 | 15/16, J03 FAIL | `276d535d…` | Sí |
| D_K | **K01–K16 (nueva)** | **16/16 PASS** | `54a5bda0…` | — (primera ejecución) |
| Dp_K | K01–K16 | 16/16 PASS | `54a5bda0…` | D = D′ |

**Lectura:**
- AUD-G1 **no cambia nada** en U ni en J: las huellas son idénticas a las de la versión congelada.
- **J03 sigue fallando.** Corregirlo exige P-G1 o P-G2, que están pendientes de aprobación. No se ha forzado.
- La cohorte K, construida con una regla independiente y publicada antes de ejecutarla (`cohorte_K/K_MANIFIESTO.md`, commit `28422fe`), genera los 16 planes iniciales. Ningún perfil de K cae en las familias sin plan del barrido: la regla no lo buscó ni lo evitó.

## 4. Adaptación con IA de prueba (modo SIMULADA, sin red) sobre U, J y K

El camino de la IA es el real: orden, contexto, interpretación, V8C, validadores y publicación. Solo el proveedor es un doble sin red. Intento 1: propuesta que el backend debe rechazar. Intento 2: la mejor candidata válida según la sonda. Intento 3: una sola sesión.

| Corrida | Oráculo v2 | Huella | Llamadas reales | Sesiones no factibles según la sonda (0 candidatas válidas) |
|---|---|---|---|---|
| S_U | **20/20 PASS** | `c0eab17e…` | 0 | U03 S3 (GM, fatiga), U04 S3 (GM, desmotivación) |
| S_J | **15/16, J03 FAIL** (sin plan inicial) | `eed24540…` | 0 | J01 S4 (GM, fatiga), J09 S4 (GM, desmotivación), J13 S3 (GM, desmotivación) |
| S_K | **16/16 PASS** | `afb57214…` | 0 | **K01 S4** (GM, fatiga), **K09** (GM, desmotivación), **K13** INVESTIGAR (GM, desmotivación), **K10** (**INCREASE_STRENGTH** intermedio en casa, 44 min, desmotivación: V9 = 8 y V20 = 4) |

**Lectura:**
- En las tres cohortes, con IA de prueba, **ningún incumplimiento de seguridad** en el oráculo y ninguna publicación inválida.
- Las 5 sesiones no factibles de U y J **se reproducen exactamente**. Son estructurales: no dependen del modelo.
- **La cohorte independiente K confirma el hallazgo y lo amplía:**
  - 3 sesiones no factibles más de GAIN_MUSCLE, por el mismo mecanismo;
  - **1 fuera de GAIN_MUSCLE** (K10, INCREASE_STRENGTH): INVESTIGAR_CAMBIO_MINIMO solo permite reducir la dosis, y las reducciones chocan con el mínimo de series (V9) y con V20.
  - El problema de A2 no es exclusivo de GM, lo que refuerza P-A2.
- **Factibles por cohorte (sesiones con IA):**
  - U: 10 de 12 sesiones sondeadas, más 2 conservadas sin IA (U07 y U12; CAUSA_NO_REGISTRADA);
  - J: 18 de 21 sesiones sondeadas (las 3 restantes no factibles);
  - K: 18 de 22 sesiones sondeadas (4 no factibles).
- **Todas las factibles se adaptan y se publican** con la IA simulada (oráculo, `oraculo/sesiones.csv`):
  - U: 10/10 (10 ACC);
  - J: 18/18 (17 ACC, 1 ALT);
  - K: 18/18 (15 ACC, 3 ALT).
- La categoría la da una IA simulada que elige la mejor candidata de la sonda. **No mide la calidad de un modelo real.**

## 5. Puerta para Replicate (fase 9)

### 5.1 Evaluación de la puerta

| Condición de la orden | Estado |
|---|---|
| Sin regresiones | Ver §2. D y D′ dan huellas idénticas a las de la versión congelada. |
| Sin planes inválidos publicados | **Cumple**: 0 incumplimientos del oráculo en D, D′ e IA simulada sobre U, J y K. |
| Sin contradicciones sin resolver | **NO CUMPLE.** Siguen abiertas: J03 (V15/V20), 9 sesiones no adaptables por política (5 en U/J y 4 en K) y 139 perfiles sin plan (V20). Todas dependen de decisiones de política (P-G1, P-A1, P-A2) que **requieren tu aprobación**. |
| Presupuesto nuevo autorizado | **No** hay autorización nueva. |

**Veredicto: la puerta NO se supera para evaluar con Replicate una versión «corregida».**
- Con la versión actual, una evaluación real reproduciría las mismas limitaciones de R/R′: AUD-G1 no cambia nada en U ni en J.
- Solo añadiría la cohorte K con el modelo real.
- **No se ha hecho ninguna llamada a Replicate.**

### 5.2 Configuración propuesta para la fase 9 (para cuando decidas)

| Elemento | Valor |
|---|---|
| Modelo | Replicate `openai/gpt-5-structured`, versión `9f4cd9ec…` (la misma de R/R′), variante `gpt-5-mini`, razonamiento `medium` |
| Prompt | V2.1 `FIX-PR-2.1-EXP` (`SessionFixPromptBuilder` sha256 `1095e4e9…`), **sin cambios** |
| Código | El commit que resulte de las decisiones: (a) `7f2da39` + P-H1 si no se aprueba ninguna política; (b) una versión nueva y congelada si se aprueban P-G1, P-A1 o P-A2, tras repetir esta fase 8 |
| Cohortes | U01–U20, J01–J16 y **K01–K16** (nueva; regla y CSV publicados antes, `28422fe`) |
| Corridas | R y R′ (dos corridas completas por cohorte) |
| Predicciones máximas | U 24 + J 48 + K 48 = **120 por corrida**, 240 en total |
| Tope de coste | U 1,20 + J 2,40 + K 2,40 = **USD 6,00 por corrida, USD 12,00 en total**. Tarifa por tokens 0,25/2,00 USD por millón; reserva de 0,05 USD por predicción. Gasto real esperado: unos USD 1,2–1,5, según R/R′ (USD 0,763 por 58 predicciones). |
| Reintentos | Los del sistema (3 intentos por ciclo), sin cambios |
| Incidencias | **P-H1 (pendiente de aprobar):** `-Dfitsense.evaluacion.k20.sin-metricas=RESERVA_SIN_BLOQUEO`. Una predicción **fallida** sin métricas carga la reserva y no bloquea; una que responde sin métricas sí bloquea. Una corrida con fallo técnico antes del oráculo se repite completa una sola vez (como en `08` §6). |
| Criterios | Los aprobados (07 §3 / 08 §5 del PR #41), sin cambios. Para K, los mismos que para J: ≥ 80 % de factibles efectivas y pertinentes, Wilson ≥ 63 %, ninguna corrida < 75 %, 0 de seguridad. |
| Reglas | Sin cambios de prompt ni de código tras ver los resultados de K. Sin repeticiones selectivas. Las sesiones no factibles se informan aparte. |
