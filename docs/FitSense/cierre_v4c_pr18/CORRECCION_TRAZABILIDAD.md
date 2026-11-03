# Corrección de la trazabilidad de V4-C

**Commit:** a52e35e en `feature/v4c-versionado-aislado`.

**Archivo de código modificado:** solo `PlanningPolicyTrace.java`.

**Sin migración:** todos los campos van dentro del JSONB existente `planning_policy_trace` (V44, sin cambios).

## 1. Campos añadidos

| Campo | Contenido | Fuente (la misma del validador) |
|---|---|---|
| `minutos_perfil` | Minutos configurados del perfil | `profile.sessionMinutes()` |
| `minutos_efectivos` | Minutos efectivos de la semana (igual a `minutos_disponibles`) | `effectiveSessionMinutes()` |
| `parametros.limite_v4_aplicado.regla` | `V4C_SEGUNDOS` o `V4_MINUTOS` | V4-C si la política tiene `v4Segundos` **y** hay bloque `prescription`; si no, la regla de minutos. Es la misma condición que `PlanDraftValidator.validateSessions` |
| `parametros.limite_v4_aplicado.minutos_base` | Minutos con los que se calcula el límite aplicado | `profile.sessionMinutes()` |
| `parametros.limite_v4_aplicado.segundos` | Solo con `V4C_SEGUNDOS` | `PlanningPolicy.limiteV4Segundos(perfil)` |
| `parametros.limite_v4_aplicado.minutos_declarados` | Solo con `V4_MINUTOS` | `profile.maxSessionMinutes()` |
| `parametros.limite_v4_minutos_base` | Minutos sobre los que se calcula `limite_v4` (los efectivos) | `effectiveSessionMinutes()` |
| `sesiones[].dentro_limite_v4_aplicado` | La decisión de V4 sobre esa sesión con la regla y el límite aplicados | V4-C: `!politica.excedeTecho(segundos, declarados, perfil)`; minutos: `declarados <= profile.maxSessionMinutes()` |

## 2. Campos históricos conservados

| Campo | Significado (sin cambios) |
|---|---|
| `minutos_disponibles` | Minutos efectivos |
| `parametros.limite_v4` | Límite **informativo** calculado sobre los minutos efectivos. Ahora `limite_v4_minutos_base` lo declara explícitamente |
| `parametros.v4`, `piso_v20_pct`, `piso_v20_segundos`, `tolerancia_v4_pct` | Sin cambios |
| `sesiones[].fecha`, `minutos_declarados`, `bajo_piso_v20`, `exenta_v8c` | Sin cambios |
| `sesiones[].pct_de_m` | Sin cambios con bloque `prescription` |

**Cambio explícito, no silencioso:**
- **Antes:** sin bloque `prescription`, `segundos_estimados` valía `0` y `pct_de_m` se calculaba con ese 0.
- **Ahora:** los dos son `null`. Sin estimación no se inventa un valor.
- **Consumidores afectados:** ninguno en la rama aislada. El ensamblador de duración del PR #17 ya trata la ausencia de número.

## 3. Lo que no se ha cambiado

| No se ha cambiado | Comprobación |
|---|---|
| El validador | `PlanDraftValidator` no aparece en el diff de a52e35e |
| La fórmula de V4-C (`segundos × 100 ≤ perfil × 60 × 115`) | Ídem |
| El tiempo base del perfil, la estimación de duración y el techo del 115 % | Ídem |
| Los generadores y el contrato con la IA | Ídem |
| V20, V24, V8C, 4A, 4C, las dosis 4D, la progresión y la reincorporación | Ídem |
| V44 | SHA-256 `640b0169…e2708400` |

**Diff de a52e35e en `src/main`:**
```
src/main/java/.../planning/domain/services/PlanningPolicyTrace.java | 35 ++++++++++++++++++++++++++++++++---
```

## 4. Ejemplo de traza tras la corrección

Persistida en PostgreSQL efímero (`PoliticaV4CCicloIT.acortarTrazaDiferenciada`): perfil de 60 min, orden ACORTAR a 30 y PLAN-V4-0.1.

```json
{
  "politica": "PLAN-V4-0.1", "contexto": "GENERACION", "origen": "RULE_ENGINE",
  "minutos_perfil": 60, "minutos_efectivos": 30, "minutos_disponibles": 30,
  "parametros": {
    "v4": "SEGUNDOS", "tolerancia_v4_pct": 15, "piso_v20_pct": 70, "piso_v20_segundos": 1260,
    "limite_v4": {"segundos": 2070}, "limite_v4_minutos_base": 30,
    "limite_v4_aplicado": {"regla": "V4C_SEGUNDOS", "minutos_base": 60, "segundos": 4140}
  },
  "sesiones": [
    {"fecha": "2026-11-16", "segundos_estimados": 1560, "minutos_declarados": 26, "pct_de_m": 86.7,
     "bajo_piso_v20": false, "exenta_v8c": false, "dentro_limite_v4_aplicado": true}
  ]
}
```
Se muestra solo la primera de las 3 sesiones; las otras dos tienen 1.440 s.

**Cómo leerla:**
- El validador aplicó **4.140 s** (60 configurados).
- La semana se construyó para **30 min efectivos**: 1.440–1.560 s por sesión.
- `limite_v4` (2.070 s) es solo informativo.

## 5. Integración futura en el PR #17

El PR #17 tiene su propia versión de `PlanningPolicyTrace` (con los campos de C2-D y EF). Al actualizarlo desde `generation`, después de fusionar el PR #18:
- En el conflicto *add/add* de `PlanningPolicyTrace.java` **no basta** con conservar la versión del PR #17: hay que **llevarle estos campos**, con el mismo código.
- `TrazaV4CTest` y `TrazaHistoricaIT` entrarán en el PR #17 con la fusión y **fallarían** si se pierden. La regresión lo detectaría.
- Esto actualiza la regla de resolución de `preparacion_integracion_pr17/DEPENDENCIAS_VERSIONADO_V44.md`, que decía «conservar el PR #17» en todos los conflictos.
