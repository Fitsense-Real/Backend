# Migraciones y versionado del cierre del catálogo

## 1. Orden de las migraciones del catálogo

| Versión | Contenido | PR | Estado |
|---|---|---|---|
| V45 | Reservada por la PR #17 | #17 | No se usa aquí |
| V47 | Banderas de seguridad (auditoría integral) | [#56](https://github.com/Fitsense-Real/Backend/pull/56) | Abierta, sin fusionar |
| V48 | Dificultad conservadora (solo subidas) | #56 | Abierta, sin fusionar |
| V49 | Nombres e instrucciones según el GIF | [#57](https://github.com/Fitsense-Real/Backend/pull/57) | Abierta, sin fusionar |
| V50 | Clasificación y equipamiento | [#58](https://github.com/Fitsense-Real/Backend/pull/58) | Abierta, sin fusionar |
| — | Controles, documentación y Excel | [#59](https://github.com/Fitsense-Real/Backend/pull/59) | Abierta, sin fusionar |
| — | Contención INC-CAT-C39 reducida a 8 | [#55](https://github.com/Fitsense-Real/Backend/pull/55) | Abierta, sin fusionar |
| **V51** | `impact_level` e `is_stretch` explícitos; el nombre deja de decidir | Cierre, PR A | Nueva |
| **V52** | Correcciones verificadas del cierre: 561 cambios en 442 ejercicios | Cierre, PR A | Nueva |
| **V53** | `requires_suspended_support` en 16 ejercicios (**decisión de significado**) | Cierre, PR B | Nueva |

Cadena de fusión obligatoria: #56 → #57 → #58 → #59 → #55 → PR A (`cierre/v51-v52-correcciones`: V51 + V52) → PR B (`cierre/catalogo-final`: V53). Cada PR tiene como base la anterior. V51 y V52 van juntas porque V52 corrige atributos que V51 crea, y sus pruebas (paridad, huella) dependen de las dos. V53 va aparte porque cambia el significado de un permiso del usuario: puede rechazarse sin tocar la PR A. Fusionar fuera de orden rompe Flyway (`outOfOrder` está desactivado) o hace fallar las protecciones de estado previo de V52.

## 2. Protecciones de cada migración

| Migración | Protección |
|---|---|
| V51 | Rellena desde una tabla temporal con los 1.376 ids. Falla si falta algún id o si queda un activo sin valor. Añade `CHECK`: un activo debe tener impacto y estiramiento, y lo marcado `high_impact` nunca es `BAJO`. |
| V52 | Un único `UPDATE` por ejercicio que exige el **valor anterior exacto** de cada columna; los textos se comparan por md5 normalizado. Si no actualiza nada y el ejercicio no está ya en el estado nuevo, **la migración falla**: nunca se salta un cambio en silencio. El equipamiento exige la cadena de requisitos anterior exacta y que existan los códigos. Es idempotente frente a una base ya corregida. |
| V53 | Columna `NOT NULL DEFAULT FALSE`. Comprueba que los 16 ids coinciden con su nombre de la fuente antes de marcar. |

V52 lo genera `cierre/herramientas/gen_v52.py` a partir de `cierre/adjudicacion/decisiones_v52.tsv`, y no se edita a mano. El generador rechaza:
- bajar una dificultad;
- un patrón fuera del dominio;
- un `name_en` que no coincide con la línea base;
- un texto sin la frase reglamentaria de su prescripción.

## 3. Contratos que el cierre respeta

- `ck_exercises_hanging_is_overhead` (colgarse implica OH). Por este contrato se vetó quitar OH en 913, colgado boca abajo: la migración lo habría violado. Queda como decisión de definición.
- `tg_requisitos_principal` (el implemento principal está entre los requisitos): V52 cambia el principal cuando cambia los requisitos (1043 a `cable`, 1184 a `captains chair`, 300 a `pull-up bar`).
- `ck_exercises_movement_pattern`, `ck_exercises_activo_clasificado`, y en V51 `ck_exercises_active_impact_stretch` y `ck_exercises_high_impact_not_bajo`.

## 4. Huella y trazabilidad

`src/test/resources/catalogo/huella_catalogo.tsv` (control permanente `CatalogoCalidadIT.huella`) incluye clasificación, dificultad, prescripción, requisitos, banderas, impacto, estiramiento, apoyo suspendido y una firma md5 del nombre y las instrucciones. Al aplicar V52 y V53 cambia la huella de **exactamente** los 442 ejercicios decididos, y el apoyo suspendido queda en los 16 de V53 (comprobado con un script, ver `REGRESION_FINAL.md`).

La procedencia por atributo y por versión (V1–V50 en `cierre/linea_base/`, V1–V53 en `cierre/auditoria_final/`) la calcula `ProcedenciaCatalogoIT`, que aplica las migraciones una a una sobre una base efímera.

## 5. Lo que no se hace

- No se ejecuta ninguna migración en producción.
- No se modifican datos de usuarios.
- No se fusiona nada.
- No se reescribe la historia de ninguna rama compartida.
