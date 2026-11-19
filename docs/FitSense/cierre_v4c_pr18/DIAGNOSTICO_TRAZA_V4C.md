# Diagnóstico de la traza de V4-C: tiempo configurado frente a tiempo efectivo

**Rama:** `feature/v4c-versionado-aislado` (PR borrador Fitsense-Real/Backend#18). Estado inspeccionado: d1de25c.

## 1. Dos minutos distintos en el contexto

| Fuente | Qué es | Cuándo cambia |
|---|---|---|
| `profile.sessionMinutes()` | Minutos **configurados** en el perfil (congelado en la semana) | Solo si la persona cambia su perfil |
| `effectiveSessionMinutes()` | `adjustment.forcedSessionMinutes()` si la orden lo trae; si no, los del perfil | Una orden con minutos forzados (por ejemplo, ACORTAR a 30) |

## 2. Qué minutos usa cada decisión (inspección del código)

| Decisión | Minutos | Código |
|---|---|---|
| V4 histórico (validador) | Perfil: `profile.maxSessionMinutes()` = `round(M × 1,15)` | `PlanDraftValidator.validateSessions` |
| V4-C (validador) | Perfil: `PlanningPolicy.limiteV4Segundos(profile.sessionMinutes())`, solo si hay bloque `prescription`; sin él, la regla de minutos | `PlanDraftValidator.validateSessions` |
| Techo de los generadores | Perfil: `profile.maxSessionMinutes()` (histórica) o `excedeTecho(…, profile.sessionMinutes())` (V4-C) | `CapabilityWeekBuilder`, `RuleBasedTrainingPlanGenerator` |
| Contrato con la IA (`max_session_seconds`) | Perfil | `PlanInputSnapshot`, `PlanPromptBuilder`, `SessionFixPromptBuilder` |
| Piso de V20 y contenido de la semana | **Efectivos** | `PlanDraftValidator.minimumSessionSeconds`, generadores |

## 3. Qué registraba la traza (`PlanningPolicyTrace`, antes de la corrección)

| Campo | Minutos |
|---|---|
| `minutos_disponibles` | **Efectivos** |
| `parametros.limite_v4` | **Efectivos**: `{segundos: efectivos × 69}` o `{minutos_declarados: round(efectivos × 1,15)}` |
| `parametros.piso_v20_segundos` | Efectivos (correcto: V20 usa los efectivos) |
| `sesiones[].pct_de_m` | Efectivos |
| `sesiones[].segundos_estimados` | `0` cuando no hay bloque `prescription` (valor inventado) |

**Consecuencia:**
- Con una orden que reduce los minutos efectivos, `limite_v4` registraba un límite que **el validador no aplicó**.
- Faltaba en la traza el límite que sí aplicó.

**Ejemplo real** (comprobado en `PoliticaV4CCicloIT.acortarTrazaDiferenciada`): perfil de 60 min y orden ACORTAR a 30.

| | Valor |
|---|---|
| Límite que aplica el validador V4-C | **4.140 s** (60 × 69) |
| `limite_v4` en la traza | **2.070 s** (30 × 69) |

Con la histórica, en el mismo caso: el validador aplica **69 min** y la traza registraba **35**.

**Otro caso:** sin bloque `prescription`, la traza mostraba `segundos_estimados = 0` y `limite_v4.segundos`, aunque el validador no aplica V4-C en ese caso.

## 4. Qué no estaba mal

- El **validador**, los **generadores** y el **contrato de IA** usan todos el mismo tiempo base, el del perfil. Son coherentes entre sí.
- El defecto era **solo de la evidencia**: ningún plan se aceptó ni rechazó por la traza, porque la traza se escribe después de validar.

**Hasta dónde llega el defecto:**
- Solo aparece cuando los minutos efectivos difieren de los del perfil, es decir, con una orden con `forcedSessionMinutes`.
- El decisor actual del ciclo semanal **no fija minutos** (`AdaptacionCompletaIT` lo documenta). En la prueba real, una semana con todas las sesiones omitidas por falta de tiempo se adaptó con los minutos efectivos iguales a los del perfil.
- Las órdenes con minutos forzados llegan por la vía de generación con ajuste (como en `DuracionForzadaIT`).
