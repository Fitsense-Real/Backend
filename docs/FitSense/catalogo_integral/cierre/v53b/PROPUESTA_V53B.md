# V53-B · Permiso propio «apoyo suspendido» (opción B, propuesta independiente)

**Estado: PROPUESTA. El interruptor nuevo `fitsense.seguridad.permiso-apoyo-suspendido` está en `false` por defecto. No se ha activado nada, no se ha tocado ningún dato real y no hay cambios en la API ni en Flutter.**

Prepara la opción B de `cierre/V53_DEFINITIVO.md` §2: un permiso nuevo, `user_profiles.allow_suspended_support`, que decide si llegan los 15 ejercicios de la situación 2 (`exercises.requires_suspended_support`, V53), **sin cambiar lo que significa «no colgarse»** (`allow_hanging_exercises`). La lista de 15 y el criterio siguen siendo los de V53. Esta propuesta no valida nada desde el punto de vista profesional: la lista, el criterio y que 1301 quede fuera siguen pendientes de esa revisión.

## 1. Qué hace

| Pieza | Cambio |
|---|---|
| `V55__perfil_permiso_apoyo_suspendido.sql` | Solo esquema: `allow_suspended_support BOOLEAN NULL` y `suspended_support_confirmed_at TIMESTAMPTZ NULL` en `user_profiles`, con `COMMENT`. No reescribe ningún dato y no hace backfill. Registrada en `SOLO_ESQUEMA` (`EsquemaVigenteSobreCatalogoAnterior`) para las pruebas que fijan `spring.flyway.target`. |
| `UserProfile` | Mapea las dos columnas como campos propios, anulables (no dentro de `MovementAllowances`, que no cambia). Crear y actualizar el perfil no las tocan. Única escritura: el método de dominio `confirmarApoyoSuspendido(boolean)`, que fija el valor y la fecha de confirmación. Hoy solo lo usan las pruebas. |
| `ProfileSnapshot` | Un componente nuevo, `allowSuspendedSupport`, con el valor tal cual está guardado. Profiling no lo interpreta. |
| `ModoApoyoSuspendido` (planning) | `APAGADO` (por defecto), `NO_COLGARSE` (opción A) y `PERMISO_PROPIO` (opción B), a partir de los dos interruptores. Con los dos encendidos lanza `IllegalStateException` y la aplicación no arranca. |
| `SafetyProfile` | Un octavo componente, `excludeSuspendedSupport`. El **constructor de siete** que ya existía lo iguala a `excludeHanging`, así que todos los sitios que ya construían un `SafetyProfile` dan el mismo resultado. `Declaradas` gana `allowSuspendedSupport`, y el constructor de cinco lo deja en `null`. `forAgeLevelAndDeclared`: sin permiso propio, hereda «no colgarse»; con él, manda el permiso. La sobrecarga con `ModoApoyoSuspendido` explícito ignora el permiso fuera de la opción B. |
| `ExternalProfilingService` (planning) | Lee los dos interruptores y resuelve el modo, que falla si los dos están encendidos. Solo con la opción B pasa el permiso guardado a `Declaradas`; con la A o apagado lo pasa en `null`. |
| `CatalogContextFacade` | La firma de once argumentos de `fetchEligibleExercises` hace lo mismo que antes: delega con `excludeHanging && opciónA`. Hay una sobrecarga de doce con `excludeSuspendedSupport` explícito. `fetchSafetyFlags` entrega `requiresSuspendedSupport` con la opción A **o** la B encendida, y en `false` si están apagadas. Con los dos interruptores no se construye. Se conserva el constructor de cuatro argumentos, que deja la B apagada. |
| `ExternalCatalogService` | Llama a la sobrecarga de doce **solo** con la opción B encendida (`permisoApoyoSuspendidoActivo()`). Apagada, la llamada es la de siempre, y las pruebas con Mockito sobre la firma de once siguen valiendo sin cambios. |
| `SafetyConflictPolicy` | `apoyo_suspendido` sale de `s.excludeSuspendedSupport() && f.requiresSuspendedSupport()`. Apagado es idéntico a lo anterior, porque el hecho llega en `false`. Con la opción A también, porque el componente vale lo mismo que `excludeHanging`. La causa se llama igual. |
| `GoalFeasibilityObserver` | Las hipótesis de escasez («con esta restricción levantada habría N»), que solo explican, conservan el componente nuevo. Solo lo levantan cuando sigue a «no colgarse» y se levanta «no colgarse». Apagado o con la A, el resultado es idéntico al del constructor de siete. |

**Decisión sobre el texto del prompt:** `describe()` y `hasRestrictions()` no cambian. El conjunto elegible ya retira el apoyo suspendido cuando corresponde. Añadir una frase cambiaría también el texto que recibe la IA con la opción A, y «Evita suspenderse de una barra» sigue ligado a «no colgarse». Una prueba unitaria fija que el texto es el mismo en los tres modos.

## 2. Resolución

Con `excludeSuspendedSupport` se decide si salen los 15. «Efecto» indica si eso cambia algo de verdad.

| Modo | «No colgarse» | Permiso propio | ¿Salen los 15? |
|---|---|---|---|
| Apagado (por defecto) | cualquiera | cualquiera | **No**: el atributo no tiene efecto en ningún sitio. Igual que hoy |
| A (`apoyo-suspendido-en-no-colgarse=true`) | sí | se ignora | Sí |
| A | no | se ignora | No |
| **B** (`permiso-apoyo-suspendido=true`) | sí | `NULL` (sin responder) | **Sí: hereda «no colgarse»** |
| B | sí | `true` (confirmado) | No: vuelven los que ninguna otra restricción excluye |
| B | sí | `false` | Sí |
| B | no | `NULL` | No |
| B | no | `true` | No |
| B | no | `false` | **Sí**, aunque pueda colgarse |
| A y B | — | — | **La aplicación no arranca**: el significado sería ambiguo |

La opción B no levanta ninguna restricción existente, salvo el atributo nuevo. Con «no colgarse» y el permiso confirmado:
- 1069 (muscle-up) sigue fuera por `requires_hanging`.
- 1153, 1303, 1304 y 1306 siguen contenidos por INC-CAT-C39 (`ContencionTextoBandera`, que sigue leyendo `excludeHanging`).
- Lo que cuelga sigue fuera.

Lo mismo vale para INC-4C-03, INC-CAT-1374, la política por edad y las demás declaraciones.

## 3. Transición

| Ámbito | Qué pasa con la opción B encendida |
|---|---|
| **Usuarios nuevos** | Se crean con las dos columnas en `NULL`, porque la API no cambia. Heredan «no colgarse»: quien lo declara tampoco recibe la situación 2; quien no, sí. Es lo conservador hasta que la app haga la pregunta |
| **Usuarios existentes** | **No se reinterpreta nada en silencio y no hay backfill.** La herencia se calcula al leer el perfil (`coalesce(allow_suspended_support, allow_hanging_exercises)`), sin copiar ningún valor a la columna. Todos quedan *pendientes de confirmar* (`suspended_support_confirmed_at IS NULL`). Para medir el alcance real, el propietario puede ejecutar `cierre/v53b/impacto_permisos_v53b_solo_lectura.sql`: es de solo lectura, corre dentro de una transacción `READ ONLY`, requiere V55 y no se ha ejecutado contra datos reales. El script de la opción A (`cierre/v53/impacto_permisos_v53_solo_lectura.sql`) no se ha modificado |
| **Preferencias guardadas** | `allow_hanging_exercises` y los otros cuatro permisos no cambian de valor ni de significado. Actualizar el perfil no escribe el permiso nuevo |
| **Confirmación explícita** | Existe el método de dominio `UserProfile.confirmarApoyoSuspendido(boolean)`, que fija el valor y `confirmed_at`. La pregunta en Flutter, y el punto de la API que la recoja, quedan para otra orden |
| **Generación** | El conjunto elegible aplica el permiso (sobrecarga de doce en la fachada). La IA y el motor de reglas no ven lo excluido |
| **Adaptación** | Los sustitutos salen del conjunto elegible, que ya aplica el permiso. V24 y V8C no cambian. La sustitución obligatoria (SUST-SEG-1) ya admite `apoyo_suspendido` como causa (`CAUSAS_DE_SUSTITUCION`) |
| **Planes publicados** | `PublishedPlanSafetyService` resuelve la seguridad del perfil por el mismo ACL. `SafetyConflictChecker` devuelve `apoyo_suspendido` para las sesiones abiertas que lleven alguno de los 15 cuando la opción B los excluye, y el mecanismo existente las bloquea. No se modifica ningún plan. Con la propuesta V54 (PR #64, aparte) quedaría constancia de la causa. Igual que en V53_DEFINITIVO §3, conviene activar la opción solo con la reparación localizada disponible |

## 4. Mediciones (datos sintéticos, base efímera)

La rejilla de 168 perfiles es la de `CatalogoIntegralComparacionIT`: objetivo × nivel × lugar × restricción, con el motor de reglas y sin IA. Se mide en los tres modos, cada uno con el permiso sin responder, confirmado que sí y confirmado que no. Dentro de cada modo, cada pasada usa los **mismos ids de usuario**, y por tanto la misma semilla del motor. La referencia es la de hoy: apagado y permiso sin responder.

- Clases: `MedicionPermisoApoyoSuspendido{Apagado,OpcionA,OpcionB}IT`. Son opt-in: `-Dfitsense.v53b.medicion=true`, con la salida en `target/v53b`.
- Comparación: `python3 -I -B medir_v53b.py target/v53b <esta carpeta>`.
- Resultados: `medicion_v53b_resumen.tsv`, `medicion_v53b_perfiles_con_cambios.tsv` y `medicion_v53b_contraste.tsv`.

| Modo / permiso | Con plan | Perfiles con elegibles distintos | Ejercicios que salen | Cambios fuera de los 15 | Planes con alguno de los 15 (hoy: 13) |
|---|---|---|---|---|---|
| Apagado, cualquier permiso | 168/168 | 0 | — | ninguno | 13 |
| A, cualquier permiso | 168/168 | 16 (8 «sin colgarse», 8 «sin todo») | Los 10: 457, 458, 459, 462, 777, 920, 934, 1184, 1317, 1335 | ninguno | 10 |
| **B, sin responder** | 168/168 | 16, los mismos que con la A | Los mismos 10 | ninguno | 10 |
| B, confirmado que sí | 168/168 | 0 | — | ninguno | 13 |
| B, confirmado que no | 168/168 | 56 (8 por cada una de las 7 restricciones) | Los 15 que fueran elegibles en cada perfil | ninguno | 0 |

Contraste, perfil a perfil (`medicion_v53b_contraste.tsv`):
- **Apagado con el permiso `true` o `false` es idéntico a hoy** en los 168, tanto en elegibles como en planes.
- **La B sin responder es idéntica a la A** en los 168.
- **La B confirmada que sí es idéntica a apagado.**

En ninguno de los tres modos falta plan en ningún perfil. Los perfiles afectados son los de gimnasio de nivel intermedio y avanzado: los demás no tenían elegible ninguno de los 15. Que cambie el conjunto elegible cambia también la semana generada en 14 perfiles (A o B sin responder) y en 49 (B confirmado que no), aunque el plan de hoy no llevara ninguno de los 15: es la selección determinista sobre otro conjunto.

## 5. Pruebas

- **Unitarias (nuevas):**
  - `PermisoApoyoSuspendidoResolucionTest`: la tabla de la §2 (los tres modos, «no colgarse» sí o no, permiso `null`, `true` o `false`), los dos interruptores a la vez, la equivalencia del constructor de siete en las 128 combinaciones, `Declaradas` de cinco y que la B no relaja nada más.
  - `PermisoApoyoSuspendidoAclTest`: apagado se usa la firma de once y la de doce nunca; con la B viaja la de doce. El permiso solo se traduce con la B. El arranque con `ApplicationContextRunner`: por defecto apagado, y con A y B no arranca.
  - `PermisoApoyoSuspendidoCausaTest`: la causa con los perfiles de siempre y con la B, y que C39 no cambia.
  - `PermisoApoyoSuspendidoPerfilTest`: crear y actualizar no tocan el permiso; la confirmación lo fija.
- **Integración: `PermisoApoyoSuspendidoIT`** (Testcontainers, contexto con la B encendida; las referencias apagado y A se construyen a mano sobre los mismos beans):
  - El esquema de V55.
  - Apagado: elegibles idénticos a hoy, con cualquier permiso guardado.
  - B sin responder: salen los 10, igual que la A.
  - B confirmado: vuelven los 10, y 1069, 1153, 1303, 1304 y 1306 siguen fuera, igual que lo que cuelga.
  - B sin apoyo pero pudiendo colgarse: salen los 15 y lo que cuelga sigue.
  - `apoyo_suspendido` para el 457 en la comprobación de planes publicados.
  - El script de alcance es de solo lectura y coincide con la resolución en Java.
- **Sin cambios:** ninguna prueba existente se ha modificado. La única línea nueva en un fichero de pruebas existente es el registro de V55 en `SOLO_ESQUEMA`.

## 6. Cómo se activaría (tras la aprobación)

1. Aprobar la opción B, la lista de 15 y la transición («sin responder» hereda «no colgarse»; confirmación explícita después).
2. Medir el alcance con `cierre/v53b/impacto_permisos_v53b_solo_lectura.sql` (solo lectura).
3. Confirmar que la opción A sigue apagada. Con las dos encendidas la aplicación no arranca.
4. Fijar `fitsense.seguridad.permiso-apoyo-suspendido=true`, preferiblemente con la reparación localizada de sesiones bloqueadas disponible.

## 7. Qué NO cambia

- El significado y el valor de «no colgarse» y de los otros cuatro permisos.
- `MovementAllowances`, la API REST y Flutter.
- `requires_hanging`, INC-4C-03, INC-CAT-1374, INC-CAT-C39 y la política por edad y nivel.
- El texto del prompt (`describe()`).
- V24, V8C y la adaptación.
- Ningún plan publicado ni ningún dato: V55 solo añade columnas `NULL`.
- La opción A sigue disponible con su interruptor, también apagado.
- Con los dos interruptores apagados (por defecto), el comportamiento es el de antes de esta propuesta. La medición de la §4 da 168 de 168 idénticos.
