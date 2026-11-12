# Paso 2: catálogo funcional por objetivo y coherencia con V15

**Fecha:** 23/10/2026 · **Política:** `planning/domain/services/CatalogEligibilityPolicy.java` · **Matriz:** 4A v6.

## 1. Auditoría de `ExternalCatalogService`

| Familia | Qué excluye | Dónde | Cambio en el Paso 2 |
|---|---|---|---|
| Seguridad | alto impacto, suelo, carga axial, rodillas, suspensión, brazos arriba, inversión (por edad, nivel y lo declarado) | consulta JPQL | ninguno |
| Nivel | `difficulty_level` > máximo del nivel | consulta | ninguno |
| Equipamiento | grupos de requisitos N:M (V23); en casa, implementos `requires_gym` | consulta | ninguno |
| Preferencias | `blockedExerciseIds` | consulta | ninguno |
| Retenidos INC-4C-03 | 813, 858, 861, 873, 885, 1192, 1298 | ACL | ninguno |
| Aprobaciones V27/V28 | apagado | `ApprovedDifficultyGate` | ninguno |
| **Funcional** | zonas `neck`, `lower arms`, `cardio`; nombre con «estiramiento» | ACL | **excepción por capacidad** |

## 2. Regla nueva

Las exclusiones funcionales anteriores se mantienen. Hay una única excepción: si el `movement_pattern` del ejercicio es la ruta de una capacidad del objetivo, el ejercicio entra como **complemento** (`ADMITIDO_POR_CAPACIDAD`):

- `LOCOMOTION_CARDIO` → `CARDIO` (`LOSE_WEIGHT`, `GENERAL_FITNESS`);
- `MOBILITY` → `MOBILITY` (`GENERAL_FITNESS`).

La admisión la decide el patrón, nunca el nombre ni la zona. Un complemento:

- **V15** no lo trata como grupo ajeno de ningún enfoque y tampoco lo cuenta para cubrir el enfoque: un estiramiento no cuenta como trabajo de piernas;
- **el motor de reglas** no lo elige como trabajo de su enfoque.

Todo lo que ya era elegible sigue siéndolo y se trata igual que antes, en los cuatro objetivos. Así no cambia ninguna semana existente ni el camino de adaptación (P2-D), donde V1 rechazaría una semana conservada con un ejercicio que dejara de ser elegible.

## 3. Evidencia

- `CatalogoElegibleIT` (PostgreSQL 16 efímero, V1–V33): **432 perfiles** = 4 objetivos × 4 entornos (casa solo peso corporal, casa básico, casa con todo declarado, gimnasio completo) × 3 niveles × 3 edades (30, 62, 72) × 3 juegos de restricciones declaradas (ninguna; sin suelo ni brazos arriba; todas). En cada perfil, la salida del ACL es **idéntica, id por id**, a un oráculo SQL escrito aparte desde las reglas. Además:
  - nada de lo que ya era elegible se pierde;
  - lo nuevo es exactamente lo admitido por capacidad;
  - el motor de reglas produce la misma semana con y sin complementos;
  - un bloqueo individual gana siempre a la admisión por capacidad;
  - el cardio que recibe un principiante no tiene alto impacto.
- Mutación de control: si se fuerza la admisión de cardio para todos los objetivos, el oráculo falla en el primer perfil y nombra los ids (253, 1054, 1093).
- `CatalogEligibilityPolicyTest` (32) y `CatalogoV15CoherenciaTest` (19). Mutaciones de control: sin la exclusión de complementos en V15 fallan 7 pruebas; sin el filtro del motor de reglas falla 1.

## 4. Conteos antes → después (432 perfiles; detalle en `target/paso2/conteos.csv`)

| Objetivo | Elegibles antes | Elegibles después | Cardio añadido | Movilidad añadida |
|---|---|---|---|---|
| `GAIN_MUSCLE` | 51–1118 | **idéntico** | 0 | 0 |
| `INCREASE_STRENGTH` | 51–1118 | **idéntico** | 0 | 0 |
| `LOSE_WEIGHT` | 51–1118 | 51–1138 | 0–20 (0 en 38 perfiles) | 0 |
| `GENERAL_FITNESS` | 51–1118 | 68–1176 | 0–20 (0 en 38 perfiles) | 17–38 |

Ruta directa total por perfil (incluye lo que ya era elegible), con el umbral 0 / 1–2 / ≥3:

| Ruta | Antes | Después |
|---|---|---|
| Cardio (`LOSE_WEIGHT`, `GENERAL_FITNESS`) | 0: 0 · 1–2: 76 · ≥3: 32 | 0: 0 · 1–2: 52 · ≥3: 56 |
| Movilidad (`GENERAL_FITNESS`) | 0: 0 · 1–2: 84 · ≥3: 24 | 0: 0 · 1–2: 0 · ≥3: 108 |

Los 38 perfiles sin cardio añadido son principiantes, mayores de 60 o personas que declararon no permitir impacto o suelo: la seguridad sigue mandando. Esto mide el **pool**, no una semana válida (pasos 3 y 4).

> **Corrección del Paso 3.** El «0: 0» de la ruta de cardio cuenta los 21 ejercicios con clasificación en revisión. Sin ellos, **38 de 108 perfiles no tienen ningún candidato de cardio verificado** (ver `PASO3_FACTIBILIDAD.md`). Estos conteos no son evidencia de factibilidad.

## 5. Impacto sobre la generación existente

- `GAIN_MUSCLE` e `INCREASE_STRENGTH`: conjunto elegible, V15 y motor de reglas idénticos.
- `LOSE_WEIGHT` y `GENERAL_FITNESS`:
  - **Más ejercicios en `available_exercises`**, tanto en el prompt inicial como en la proyección del prompt de corrección de P2-D. El código de P2-D no cambia, pero su entrada sí, para estos dos objetivos.
  - **El prompt no explica los complementos.** Su tabla de enfoques sigue diciendo que no se metan grupos ajenos, así que la IA probablemente no los usará hasta alinear generadores (paso 5).
  - **V15 se relaja solo para complementos.** Una sesión formada únicamente por complementos pasa V15; la limitan V5 y, más adelante, la organización de sesiones (paso 4).
  - **V18 y V22 sí cuentan los complementos.** Un estiramiento de espalda el día siguiente a un remo se rechaza por recuperación. Solo afecta a planes que los incluyan.
- 4C: los complementos caen fuera de los cuatro slots y solo suben `fuera_de_slot`.

## 6. Pendiente de decisión (no se corrige en silencio)

Hay ejercicios cuyo patrón contradice su nombre o su zona y que siguen con el tratamiento anterior:

- **`LOCOMOTION_CARDIO` ya elegibles para todos los objetivos como trabajo normal (8):** 59 Bicicleta de aire, 93 Salto hacia atrás, 298 Puente con escalador cruzado, 876 Salto hacia adelante, 902 Gusano caminando, 903 Inchworm v. 2, 1117 Pies rápidos v. 2, 1374 Sprints con viento. Parte de la ruta de cardio «ya elegible» descansa en clasificaciones discutibles (59 es un crunch; 902 y 903 son inchworms).
- **`MOBILITY` ya elegibles sin «estiramiento» en el nombre (8):** 55, 312, 465, 809, 1192 (retenido), 1194, 1257, 1310.
- **Zona `cardio` con patrón de fuerza, excluidos en todos los objetivos (5):** 54, 853, 1174, 1295 (`KNEE_DOMINANT`) y 1294 (tronco).

Pasar a un criterio solo por patrón cambiaría el conjunto elegible de `GAIN_MUSCLE` e `INCREASE_STRENGTH`. Requiere decidir antes cómo se trata, en P2-D, una semana conservada que contiene un ejercicio que deja de ser elegible.
