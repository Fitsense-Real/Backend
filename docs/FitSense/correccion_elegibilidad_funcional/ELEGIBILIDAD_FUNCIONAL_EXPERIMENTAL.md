# Elegibilidad funcional experimental (EF-0.1)

**Estado:**
- Implementada y probada.
- **Desactivada por defecto.**
- Solo actúa con una versión de política que la incluye.
- No cambia el catálogo, las dosis 4D, 4A, 4C, V23, V24 ni V25.

## 1. Versiones

| Versión | C2-D | V4-C | EF | Uso |
|---|---|---|---|---|
| `PLAN-HIST-1` | — | — | — | Por defecto (sin cambios) |
| `PLAN-C2D-0.1`, `PLAN-V4-0.1`, `PLAN-C2D-V4-0.1` | según la versión | según la versión | **no** | Sin cambios de significado |
| `PLAN-EF-0.1` | — | — | **sí** | Histórica + EF |
| `PLAN-C2D-EF-0.1` | sí | — | **sí** | C2-D + EF |
| `PLAN-C2D-V4-EF-0.1` | sí | sí | **sí** | C2-D + V4-C + EF |

- Las tres versiones nuevas son **candidatas**: exigen `fitsense.planning.policy.allow-candidates=true`.
- Ningún identificador se reutiliza. `PLAN-C2-0.1` sigue sin usarse.
- **V45** amplía la lista cerrada de la restricción de V44:
  - no toca datos;
  - comprobado en `MigracionV45IT`, solo en PostgreSQL efímero;
  - **no se ha aplicado a Neon ni a Railway**.
- La traza registra `parametros.elegibilidad_funcional = "EF-0.1"` **solo** en las versiones EF. La traza de las demás no cambia.

## 2. Reglas (`planning/domain/services/ElegibilidadFuncional.java`)

### 2.1 Compatibilidad patrón-enfoque

Solo afecta a los ejercicios **resistidos** (clase ESTRUCTURAL o RESISTIDO) que **no son complementos**. Cardio, movilidad, equilibrio y complementos no se juzgan por patrón.

| Enfoque | Patrones admitidos |
|---|---|
| PULL | HORIZONTAL_PULL, VERTICAL_PULL, SHOULDER_EXTENSION, SCAPULAR_ELEVATION, ELBOW_FLEXION, WRIST_FOREARM, TRUNK_EXTENSION |
| PUSH | HORIZONTAL_PUSH, VERTICAL_PUSH, HORIZONTAL_ADDUCTION, SHOULDER_RAISE, ELBOW_EXTENSION |
| LEGS | KNEE_DOMINANT, HIP_HINGE, HIP_EXTENSION, HIP_ABDUCTION_ADDUCTION, ANKLE_PLANTAR_FLEXION |
| LOWER_BODY | Los de LEGS + tronco (TRUNK_FLEXION, TRUNK_ROTATION_LATERAL_FLEXION, CORE_STABILITY, TRUNK_EXTENSION) |
| UPPER_BODY | Los de PULL + los de PUSH |
| FULL_BODY, CORE | Todos |

**Patrón nulo:** no se juzga. No hay dato y no se inventa.

### 2.2 Ancla obligatoria (solo validación)

| Enfoque | Necesita al menos uno de |
|---|---|
| PULL | HORIZONTAL_PULL, VERTICAL_PULL |
| PUSH | HORIZONTAL_PUSH, VERTICAL_PUSH |
| LEGS | KNEE_DOMINANT, HIP_HINGE, HIP_EXTENSION |

**Sin ancla, el validador (VEF) responde de una de dos maneras:**

| Situación | Mensaje |
|---|---|
| El catálogo elegible no tiene ninguna | «… no tiene ningún ejercicio de tracción … y el catálogo elegible con el equipamiento declarado no tiene ninguno (**NO_FACTIBLE por catálogo o equipamiento insuficiente**; no se sustituye por otro movimiento)» |
| Hay candidatas que no se eligieron | «… hay N candidatos elegibles de ese patrón» |

**El backend no añade material** que el usuario no declaró (mesa, puerta, barra, anclaje) **ni ejercicios externos**.

### 2.3 Registro no computable como trabajo resistido (en cualquier enfoque)

| Causa | Ejercicios |
|---|---|
| **ZONA_INCOHERENTE**: TRUNK_FLEXION, TRUNK_ROTATION_LATERAL_FLEXION o CORE_STABILITY con zona distinta de `waist` | 1259, 1261, 1308 y los del escaneo (`ESCANEO_CATALOGO_ACTIVO.csv`) |
| **DATOS_INCOHERENTES**: lista cerrada de inconsistencias **comprobadas** en el registro | Solo **315**: equipo «peso corporal», pero las instrucciones piden mancuerna y banco |

- **No se reclasifica nada en la base de datos.** Solo se deja de contar como trabajo resistido mientras la versión EF esté activa.
- **320** (flexión de codo sin carga) **no se excluye**: su patrón es coherente; lo dudoso es su capacidad (requiere revisión profesional).
- **775 no se excluye globalmente**: en FULL_BODY cuenta como lo que dice su patrón (UPPER_PUSH). En PULL deja de entrar.

## 3. Dónde se aplica

| Pieza | Cambio (solo con EF) |
|---|---|
| `ExerciseSelector.pool` | `admite(focus, c)` = V22 + compatibilidad |
| `CapabilityWeekBuilder` | `resistidos` sin los no computables. Las cinco comprobaciones de enfoque (rellenos, cardio pegado, complementos, sustituciones) usan `admite` |
| `RuleBasedTrainingPlanGenerator` (GAIN_MUSCLE) | `seleccionables` sin los no computables; selector con EF |
| `PlanDraftValidator` | Regla **VEF**, después de V22 |
| `SaturacionContenidoV20` (C2-D) | Usa el mismo validador: un movimiento que rompe VEF no demuestra «mejora posible» y una sesión VEF no puede ser excepción |
| `PlanDraftValidator.aplicaC2D` | Sin cambios: solo actúa si **todos** los problemas son V20, así que **una sesión funcionalmente inválida nunca se publica por C2-D** |
| Prompt de la IA (generación y corrección) | Con EF se añade `ElegibilidadFuncional.reglaParaIA()`, el mismo conjunto de patrones que aplica VEF (`ElegibilidadFuncionalTest.reglaDeLaIACoincideConElValidador`). Sin EF, el texto y el JSON son idénticos |

## 4. Pruebas

**`ElegibilidadFuncionalTest`** (8 casos, con el validador real):
- con la histórica y con C2-D sin EF, VEF no existe;
- el PULL de los casos del 46 % (775, 1261, 320, 1066) da NO_FACTIBLE por catálogo;
- un PULL sin tracción con tracción elegible dice «hay 1 candidatos»;
- un PULL con remo pasa;
- 315 no cuenta;
- C2-D no exime VEF;
- la regla de la IA coincide con el validador;
- las versiones EF son propias.

**Otras:**
- `PoliticaPlanificacionTest`: catálogo de 7 versiones.
- `MigracionV45IT`.
- Laboratorios: `CorreccionFuncionalMatrizIT`, `CicloRealC2DEFIT` y `CicloRealC2DV4EFIT` (ver `REGRESION_FINAL.md`).

## 5. Límites conocidos

- Las familias de patrones son una **regla técnica explícita, no una validación profesional**. Por ejemplo, ¿TRUNK_EXTENSION pertenece a PULL? ¿SHOULDER_RAISE a PUSH?
- El ancla exige **un** ejercicio del patrón, no una dosis mínima.
- EF no mide la calidad del estímulo (carga externa, proximidad al fallo). 320 sigue admitido.
