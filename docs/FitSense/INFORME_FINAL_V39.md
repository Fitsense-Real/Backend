# V38 + V39 · E1/E2 · Informe final

**Fecha:** 30/10/2026. **Rama:** `catalogo-v37-1`. **Sin fusionar.**

**Orden aplicada:** «Decisión sobre V38 y autorización E1/E2» (decisiones E, F, G, H e I). Continúa `INFORME_V38.md`.

**Qué no se ha tocado:**
- Neon, Railway, `main` y producción. Ninguna migración contra bases reales.
- Replicate: ninguna llamada. La progresión sigue apagada.
- V15 y sus umbrales, V18, V20, V23, V24, V25, las dosis y los validadores.
- V37 y las migraciones históricas.
- Las referencias históricas (V35, V36, V37 con el motor anterior) y la de V37 con el motor V20.

## 1. Resumen

| | V37 | V37 + V37.1 | **V38 + V39 oficiales + E1/E2** |
|---|---|---|---|
| Escenarios (de 192) con un implemento que, **según su GIF**, el usuario no declaró | 126 | 105 | **0** |
| 72 referencias válidas | 72 | 72 | **72** |
| Barrido de 72 (4–6 días) con plan | 72 | 72 | **72** |
| Barrido con todas las capacidades del objetivo cubiertas | 72 | 72 | **72** |
| Robustez de 288 | 288 | 288 | **288** |
| Continuidad de 96 | 96 | 96 | **96** |
| 192 restricciones publicadas, sin violaciones | 192 | 192 | **192** |
| Sesiones bajo el 70 % exacto | 0 | 0 | **0** |

Sin E1/E2, la V38 candidata dejaba 24 perfiles sin plan por V15 (`INFORME_V38.md` §5). Con E1/E2 no queda ninguno, y **no se ha restaurado ningún dato incorrecto** para conseguirlo.

Evidencia: `evidencia/v39/final/` (`RESUMEN.md` y los TSV de la simulación).

## 2. E1/E2 (decisión E)

**Dónde:** `CapabilityWeekBuilder.asegurarGrupos`.

**Cuándo actúa:** solo si la sesión **incumple V15**:
- un solo grupo muscular, también en FULL_BODY (**E1**, motivo `GRUPOS`);
- o un grupo por encima de la mitad (**E2**, motivo `MITAD`).

Una sesión válida no se toca.

**Qué hace:** sustituye un ejercicio del grupo sobrante por otro de un grupo distinto del mismo enfoque. El sustituto tiene que cumplir todo esto:

| Condición de la orden | Cómo se garantiza |
|---|---|
| Conservar la capacidad | El sustituto se coloca con la **misma capacidad** que el quitado (`col.capacidad() == quitado.capacidad()`); si no, se descarta |
| No cambiar fuerza o cardio por otra cosa | Los candidatos salen solo de los resistidos y estructurales elegibles; nunca movilidad, equilibrio ni relleno abdominal |
| Equipamiento, dificultad, impacto | Los candidatos son los **elegibles** del usuario: el filtro de equipamiento, dificultad, impacto y restricciones ya está aplicado |
| Enfoque, duración y recuperación | Mismo enfoque; techo de minutos y piso de V20; V18 con los bloqueados de la semana |
| No perder slots exigibles | En la última sesión de la semana, si el quitado era el único que cubría un slot pendiente, la sustitución se descarta |
| V15 | La sesión resultante tiene que tener ≥ 2 grupos y ninguno por encima de la mitad |
| Sin sustitución factible | La sesión queda **como estaba** y el validador rechaza el plan con su motivo. No se fuerza nada |

Cada cambio se registra (`CambioGrupo`: fecha, enfoque, motivo, quitado y elegido con su grupo y capacidad).

**Pruebas:**
- `ComposicionE1E2IT`: los casos que originaron el problema, con 4 semillas cada uno.
  - GENERAL_FITNESS principiante, 4, 5 y 6 días, gimnasio y casa (pierna con tres `upper legs`: E2).
  - LOSE_WEIGHT intermedio y avanzado, 3 días en casa (FULL_BODY de un solo grupo: E1).
  - Comprueba en cada cambio:
    - misma capacidad;
    - el sustituto entra como resistido o estructural, y una fuerza solo como estructural;
    - el grupo es distinto;
    - el plan se publica con todos los validadores.
  - Comprueba también que han actuado los dos mecanismos. **Pasa.**
- `V15CambioDeGrupoIT`: pasa.
- `GeneracionReferenciaV35IT`, `V36IT` y `V37IT`: el motor con E1/E2 reproduce **byte a byte** las referencias de V35, V36 y V37. En esas 72 referencias de cada catálogo anterior E1/E2 no cambia nada.

## 3. Migraciones (decisión F)

| Flyway | Contenido | Archivo |
|---|---|---|
| V37 | Sin cambios | `V37__catalog_safety_corrections.sql` |
| **V38** | V37.1 (clasificación y equipamiento) | `V38__catalog_classification_equipment.sql` |
| **V39** | La candidata V38: clasificación funcional, cardio de casa y equipamiento real (6 implementos nuevos y 54 cambios en 43 ejercicios) | `V39__catalog_functional_equipment.sql` |

**Reproducción exacta** de las candidatas aisladas:
- La exportación del catálogo (1413 líneas) es idéntica con las candidatas y con las oficiales (sha256 `cf30b8bb…8fd0`).
- Las referencias, el barrido, la robustez y las 192 restricciones también son idénticas.
- Las migraciones oficiales solo difieren en la cabecera, los mensajes y el nombre de los objetos temporales.

Las copias de `docs/…/v37/separables` y `docs/…/v38/candidata` quedan como históricas. **No deben combinarse con `db/migration`**, porque los cambios se aplicarían dos veces; lo explica un `LEEME.md` en cada carpeta.

`CatalogoV39IT` (antes `CatalogoV38IT`) corre ahora siempre. Comprueba el cardio, la clasificación, el equipamiento y los perfiles de solo peso corporal.

## 4. Referencias (decisión G)

| Archivo | Estado |
|---|---|
| `generacion_referencia_v35.txt`, `_v36.txt`, `_v37_motor_e14da35.txt` | Históricas, intactas |
| `generacion_referencia_v35_motor_v20.txt`, `_v36_motor_v20.txt` | Intactas; el motor con E1/E2 las reproduce |
| **`generacion_referencia_v37_motor_v20.txt`** (nueva) | La oficial vigente antes de las migraciones, congelada (`00951b53…59e5`). El motor con E1/E2 la reproduce (`GeneracionReferenciaV37IT`) |
| **`generacion_referencia.txt`** | Oficial nueva: V38 + V39 con E1/E2 (`8ee26d78…2077`), 72/72 válidas |

`ReferenciasHistoricasInmutablesTest` protege ahora también la de V37 con el motor V20.

**Las 72 diferencias:** `evidencia/v39/DIFERENCIAS_REFERENCIAS_V39.md`. No se aceptaron automáticamente:
- Se revisaron **todos** los ejercicios de las 72 (no solo los nuevos) con estas comprobaciones, y dieron **0 problemas**:
  - equipamiento de casa según los requisitos de V39;
  - dificultad del nivel;
  - impacto en principiante;
  - movilidad sin dosis de fuerza;
  - GAIN_MUSCLE solo con resistido o estructural;
  - LOSE_WEIGHT con cardio en cada sesión.
- Lo que entra es coherente con cada objetivo:
  - en LOSE_WEIGHT, cardio de casa (853, 1117, burpee, mountain climber);
  - en GAIN_MUSCLE e INCREASE_STRENGTH, variantes de flexión y tracción con banda;
  - en GENERAL_FITNESS, una mezcla con movilidad y equilibrio.
- **Riesgo encontrado:** algunos ejercicios tienen un grupo y un patrón que no casan, y ahora se usan más. No se corrigen aquí: son los criterios C4/C6 y necesitan tu decisión.

  | Ejercicio | Grupo / patrón | Usos en V37 → V39 |
  |---|---|---|
  | 1225 smith hip raise | waist / HIP_EXTENSION | 8 → 31 |
  | 439 cable squat row | back / KNEE_DOMINANT | 0 → 17 |
  | 959 kettlebell figure 8 | waist / HIP_HINGE | 5 → 15 |
  | 962 kettlebell sumo high pull | back / KNEE_DOMINANT | 0 → 6 |
  | 927 kettlebell bent press | waist / VERTICAL_PUSH | 4 → 6 |
  | 1223 smith incline shoulder raises | chest / HORIZONTAL_PULL | 7 → 7 |
  | 674 dumbbell one arm snatch | upper legs / VERTICAL_PUSH | 2 → 3 |
  | 162 barbell clean and press | upper legs / VERTICAL_PUSH | 1 → 1 |

  Ninguno es inseguro para el perfil que lo recibe: el filtro de seguridad y de equipamiento ya se aplicó. El problema es de **clasificación**, que hace que cuente para V15 o para un slot de forma discutible.

## 5. Equipamiento en la app (decisión H)

**Backend:**
- `GET /api/v1/catalog/equipment-types` ya publica todos los códigos, también los nuevos: `wall bars`, `anchor point`, `ghd bench`, `hyperextension bench`, `squat rack`, `preacher bench` y `step platform`.
- `LimitacionesPlanIT.espalderaPorElContratoDeLaApp` lo comprueba de punta a punta:
  1. El perfil se actualiza con el mismo `PUT /users/me/profile` que usa la app, añadiendo `wall bars`.
  2. El código se guarda tal cual.
  3. Desaparece la limitación de tracción.
  4. El plan generado incluye ejercicios con espaldera.

**Flutter:**
- Pantalla `EquipmentSelectionPage` (ruta `/profile/equipment`).
  - Lista lo que devuelve el backend, sin códigos escritos en la app.
  - Guarda con `PUT /users/me/profile` partiendo del perfil actual.
  - Comprueba que el backend devolvió los mismos códigos.
  - Muestra el aviso de limitaciones.
- `flutter analyze` sin problemas y 3/3 pruebas (modelo del PUT, orden, y una prueba de pantalla con espaldera, punto de anclaje, banco GHD y step).
- **Bloqueo de entrega:** el push a `FitSense-2-0/mobileapp` lo rechaza GitHub (403): la app de Claude no está instalada en esa organización.
  - El cambio está como parche en `evidencia/v39/flutter/0001-…patch`; se aplica con `git am` sobre `main` de la app.
  - Lo resuelve instalar la app en la organización, o aplicar el parche a mano.

**Experiencia de usuario: NO completa todavía.**
- La app no tiene aún las pantallas de perfil ni de onboarding donde enlazar esta.
- Falta probarla contra el backend real desde un dispositivo.
- No bloquea las pruebas del backend.

## 6. Capacidades no factibles (decisión I)

**Nuevo endpoint:** `GET /api/v1/users/me/plan/limitations`.
- Es de solo lectura.
- Mide el mismo conjunto elegible que usará la generación: equipamiento, dificultad y restricciones declaradas y por edad.
- Devuelve:
  - los slots vacíos (por ejemplo `UPPER_PULL`);
  - las capacidades principales `NO_FACTIBLE` del objetivo.
- Cada limitación lleva un título y un mensaje en español para la app.

**Mensaje de tracción:**
> Con el equipamiento que indicaste no hay ejercicios de tracción (remos o dominadas) seguros para ti. Tu plan trabaja empuje, piernas y core. Si ya tienes una banda elástica, una espaldera o un punto de anclaje, puedes añadirlo en tu perfil. No necesitas comprar nada.

Los `optionalEquipmentCodes` (`band`, `wall bars`, `anchor point`) solo se muestran como «si ya tienes alguno».

**Qué no cambia:**
- V23 no se ha tocado: no se fabrica cobertura.
- `LimitacionesPlanIT` lo comprueba: con solo peso corporal, el plan se publica **sin ningún ejercicio de tracción**.

## 7. Pruebas

| Prueba | Resultado |
|---|---|
| Suite completa `mvn verify -Pintegration` con V38 + V39 oficiales | §7.1 |
| `ComposicionE1E2IT` | Pasa |
| `LimitacionesPlanIT` (3) | 3/3 |
| `CatalogoV39IT` (4) | 4/4 |
| `GeneracionReferenciaV35IT`, `V36IT`, `V37IT` | Reproducen sus referencias |
| `ReferenciasHistoricasInmutablesTest` | Pasa |
| Simulación: 72 referencias, barrido, 288 de robustez, 96 de continuidad y 192 restricciones con comprobación del equipamiento real | §1 |
| Flutter: `flutter analyze` y `flutter test` del módulo de equipamiento | Sin problemas, 3/3 |

### 7.1 Suite completa

**`mvn verify -Pintegration` con V38 + V39 oficiales: BUILD SUCCESS.**
- 855 unitarias y 296 de integración, **0 fallos**.
- 24 omitidas, todas opt-in: Neon, Replicate real, lectura, progresión y simulaciones largas, que se ejecutan aparte (§1).
- El CI del PR también pasa (`verify`).

**Primera ejecución: 5 fallos en 3 clases.** Todos eran premisas ligadas a datos de V37 que V39 corrige a propósito. Se ajustaron **sin tocar políticas ni validadores**:

| Prueba | Por qué fallaba | Ajuste |
|---|---|---|
| `Usuario31V37IT` | Es evidencia del catálogo V37. Con V39, 301 exige espaldera | Fijada en `spring.flyway.target=37` |
| `GeneracionPorObjetivoIT`: `condicionGeneralSinCardio`, `perderPesoSinCardio`, `unUnicoCardio` | Su premisa era que en casa sin suelo no había cardio, o solo 1325. Con V39 hay cardio de pie (853, 1117, 1093) | La premisa se recrea bloqueando esos ejercicios con el mecanismo del usuario (`blocked_exercise_ids`) |
| `GeneracionPorObjetivoIT`: prueba nueva `casaSinSueloConV39` | — | Comprueba la mejora: LOSE_WEIGHT en casa sin suelo queda COMPLETO, sin cardio de suelo |
| `AdaptacionRegresionIT` (`Bloqueos`) | El sustituto se elegía por SQL sin mirar el equipamiento. Con V39 elegía 301, que el perfil ya no puede recibir | El sustituto se elige entre los elegibles reales del participante (`prepare`) |

## 8. Revisión humana (F)

- **Estado: pendiente.** El paquete está en `auditoria_catalogo/v38/REVISION_HUMANA_V39.md` y `revision_humana_v39.tsv`.
  - Son 43 filas en tres prioridades. La prioridad 1 es movimiento, equipamiento y dificultad: 13 filas.
  - Las columnas de revisor y veredicto están **vacías**: no se ha inventado ningún revisor.
- Hasta que se complete, los datos de V39 cuentan como **revisados por IA y pendientes de confirmación humana**.
- **1106:** dificultad 2 provisional. No hay ningún evento en `exercise_difficulty_approvals`.
- Si la revisión cambia algo:
  1. corregir `cambios_v38.tsv`;
  2. regenerar con `generar_v38.py` (como V40 si V39 ya se aplicó en algún entorno);
  3. repetir las pruebas afectadas.

## 9. Pendientes que siguen abiertos y si bloquean la integración

| Pendiente | ¿Bloquea integrar en `generation`? | Por qué |
|---|---|---|
| Doble revisión humana de las 43 filas | **No para `generation`; sí antes de desplegar en entornos reales** | La integración es reversible y trazable, y cada fila comprueba su estado previo. Pero los datos no deben llegar a usuarios sin confirmación humana |
| C1: convención de los 74 ejercicios sentados (banco o cajón) | No | No cambia ninguna dosis ni restricción. Afecta a si un perfil de casa sin banco recibe un ejercicio sentado. Es un criterio previo a V38 + V39; no se ha medido si su uso aumenta con V39 |
| C2: balón apoyado en el suelo | No | Mismo motivo: es un criterio de clasificación previo, sin cambios en V38 + V39 |
| Dificultades sin consenso (1106 provisional; flexiones 1111, 1113, 1115 y 1262 sin instrumento) | No para `generation`; sí para una validación formal del catálogo | Los valores actuales son conservadores (nivel 1–2) y están documentados como provisionales |
| C4/C6: grupo o patrón incoherente (1225, 439, 959, 962…, §4) | No, pero conviene decidirlo antes del estudio | No es un problema de seguridad: el ejercicio cuenta para V15 o para un slot de forma discutible |
| Flutter: integración en el perfil y el onboarding, y push del parche | No | La decisión H lo separa del backend; la experiencia no está completa |
| 298 y 854 (D2) | No | Pendientes de una revisión adicional, sin cambios en V39 |
| `min_session_seconds` | No | Sigue pendiente, como antes |
| Respuestas de Replicate de los usuarios 32 y 33 | No | No están en el repositorio |

## 10. Riesgos residuales

- **Revisión de IA, no humana:** los datos del catálogo corregido no tienen todavía confirmación humana (§8).
- **Clasificación:** los ejercicios de §4 se usan más con V39 y su grupo o patrón es discutible.
- **E1/E2 sin sustituto:** si en algún perfil futuro no existe una sustitución que cumpla todo, la semana se rechaza por V15 en vez de forzarse. Es lo que pide la orden, pero significa que un catálogo más pobre puede dejar a alguien sin plan; lo detectaría el barrido.
- **Perfiles de solo peso corporal:**
  - no tienen tracción;
  - es correcto según V23, y ahora se comunica;
  - el aviso solo llega al usuario cuando la app integre la pantalla.
- **App:** el parche no está en el repositorio de la app por el permiso de GitHub.

## 11. Fusión

**No se ha fusionado.** Hay un PR borrador hacia `generation` para obtener el CI, con:
- la evidencia de capacidades (§2 y `evidencia/v39/final/RESUMEN.md`);
- las referencias revisadas (§4).

Queda a tu decisión.
