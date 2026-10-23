# Paso 3: factibilidad por objetivo (modo observación)

**Fecha:** 23/10/2026 · **Política:** `planning/domain/services/GoalFeasibilityPolicy.java` · **Observador:** `planning/application/internal/commandservices/GoalFeasibilityObserver.java` · **Matriz:** 4A v6.

**Ninguna regla de rechazo cambia.** V23 (4C), V24 (4D), los filtros y P2-D siguen como estaban. El paso solo mide y registra.

## 1. Qué responde

**Nivel A, pool.** ¿Hay suficientes ejercicios seguros, distintos y verificados para cada capacidad **principal** del objetivo? Los ejercicios se cuentan después de todos los filtros del perfil.

| Capacidad | Ruta (4B) | Cómo se mide |
|---|---|---|
| `HYPERTROPHY`, `STRENGTH` | los cuatro slots de 4C | ids distintos por slot; manda el peor |
| `MUSCULAR_ENDURANCE` | trabajo resistido de cualquier patrón | ids distintos. **Provisional:** la distingue la dosis (4D) |
| `CARDIO` | locomoción directa (`LOCOMOTION_CARDIO`) **o** circuito | ids distintos; **el circuito siempre es `NO_VERIFICABLE`** |
| `MOBILITY` | `MOBILITY` | ids distintos |
| `BALANCE` (secundaria oportunista) | `BALANCE_CONTROL` | ids distintos |
| `POWER` (secundaria condicional) | ninguna acreditable | `NO_APLICA` en principiantes; `NO_VERIFICABLE` en el resto |

Veredicto de una ruta: 0 = `NO_FACTIBLE`, 1–2 = `FRAGIL`, ≥3 = `ROBUSTO`. Una capacidad toma su mejor ruta; si la única alternativa es no verificable, queda `NO_VERIFICABLE`: no se declara ni cobertura ni inviabilidad por suposición. El objetivo toma la **peor principal**. Las secundarias se informan y nunca cambian el veredicto.

- `LOSE_WEIGHT` **no** exige los cuatro slots.
- **Los 21 ejercicios con clasificación en revisión** (incidencia del Paso 2) se cuentan aparte y **no son evidencia**.

**Nivel B, semana.** ¿La semana generada **contiene** cada principal? Respuestas posibles:

- `PRESENTE_PENDIENTE_4D`: nunca «válida»; dosis, tiempo e intensidad son el Paso 4;
- `NO_VERIFICABLE`: solo hay ejercicios en revisión;
- `AUSENTE`.

Una sesión formada solo por complementos no se rechaza, pero tampoco acredita el objetivo completo.

**Líneas de log** (generación inicial; la adaptación de P2-D no pasa por aquí):

- `FACTIBILIDAD_POOL`: antes del primer generador;
- `ESCASEZ`: si alguna principal no es robusta, qué restricción la provoca. Se recuenta levantando una sola cada vez; los conjuntos hipotéticos nunca llegan a un generador;
- `DIVERGENCIA_V23`: 4A v6 no exige los slots y V23 sí;
- `COBERTURA_SEMANAL`: sobre el borrador aceptado.

Si la observación falla, se registra y no impide generar.

## 2. Resultados con el catálogo real (`FactibilidadPorObjetivoIT`, 432 perfiles; `target/paso3/factibilidad.csv`)

| Objetivo | Pool `ROBUSTO` | `FRAGIL` | `NO_VERIFICABLE` | `NO_FACTIBLE` |
|---|---|---|---|---|
| `GAIN_MUSCLE` | 87 | 21 | 0 | 0 |
| `INCREASE_STRENGTH` | 87 | 21 | 0 | 0 |
| `LOSE_WEIGHT` | 44 | 26 | **38** | 0 |
| `GENERAL_FITNESS` | 42 | 28 | **38** | 0 |

- **En 38 perfiles, `CARDIO` no tiene ningún candidato verificado.** Su único candidato está entre los 21 en revisión. Corrige la lectura del Paso 2, cuyo «0 perfiles sin cardio» contaba esos ejercicios.
- En `GENERAL_FITNESS`, `MOBILITY` y `MUSCULAR_ENDURANCE` son robustas en los 108 perfiles; lo que limita es `CARDIO` (y `STRENGTH`, frágil en 21).

**Semana del motor de reglas (nivel B):**

| Objetivo | Principales presentes (dosis pendiente) | Principal ausente | Solo no verificable |
|---|---|---|---|
| `GAIN_MUSCLE` | 108 | 0 | 0 |
| `INCREASE_STRENGTH` | 32 | **76** | 0 |
| `LOSE_WEIGHT` | 0 | **88** | 20 |
| `GENERAL_FITNESS` | 0 | **108** | 0 |

El motor de reglas solo elige por slots en `GAIN_MUSCLE` y no elige complementos. Por eso ninguna semana de `LOSE_WEIGHT` contiene cardio verificado, aunque en 60 perfiles el pool de cardio sea robusto o frágil. Es exactamente la diferencia entre pool y semana. Corregirlo es el Paso 5 (generadores).

**Mismas restricciones, distinto veredicto según el objetivo.** En los datos reales, 66 de las 108 combinaciones dan dos veredictos distintos (`FRAGIL`/`ROBUSTO`: 28; `NO_VERIFICABLE`/`ROBUSTO`: 19; `FRAGIL`/`NO_VERIFICABLE`: 19). **Ninguna da tres a la vez en esta matriz.** El caso de tres veredictos (robusto, frágil y no factible) está cubierto con datos sintéticos (`GoalFeasibilityPolicyTest.mismoPoolTresVeredictosSegunElObjetivo`), no con datos reales.

**El caso del informe (776).** Principiante de 30 años, en casa, solo peso corporal, sin suelo ni brazos arriba, con 776 bloqueado:

- `GAIN_MUSCLE` → `NO_FACTIBLE`, `LOWER_HIP=0`. Causas registradas: `NO_FACTIBLE(0)→ROBUSTO(3)` si se levantara la restricción de suelo, y `→FRAGIL(1)` si se levantara el bloqueo.
- `LOSE_WEIGHT` → `NO_VERIFICABLE`: no le afecta la cadera, pero no tiene cardio verificado.

**Nada se relaja automáticamente.**

## 3. Pruebas

- `GoalFeasibilityPolicyTest` (23):
  - umbrales 0/1/2/3 y 7;
  - filas repetidas;
  - clasificaciones en revisión;
  - `LOSE_WEIGHT` sin slots;
  - slot vacío con motivo;
  - secundarias que no cambian el veredicto (`STRENGTH`, `POWER`, `BALANCE`);
  - tres veredictos con el mismo pool;
  - el peor de las principales;
  - objetivo desconocido;
  - pool robusto con semana sin cardio;
  - cardio solo en revisión;
  - semana sin un slot;
  - sesión solo de complementos;
  - id fuera del conjunto elegible.
- `FactibilidadPorObjetivoIT` (3, PostgreSQL efímero):
  - censo de 432 perfiles con invariantes (el veredicto es la peor principal; `LOSE_WEIGHT` sin slots; el circuito nunca acredita);
  - caso 776 con su explicación;
  - observador inyectado en `generate()` y sus cuatro líneas de log.

## 4. Regresión de P2-D en base aislada

Las IT de P2-D se ejecutaron **sin modificarlas** contra un PostgreSQL 16 temporal. La base se preparó con V1–V33 aplicadas (cada una en su transacción, como Flyway), la marca `fitsense_entorno = 'PRUEBAS'` y el usuario 1:

| Clase | Resultado |
|---|---|
| `InvariantesV33IT` | 3/3 |
| `PublicacionAtomicaIT` | 2/2 |
| `DisparadorDiferidoIT` | 2/2 |
| `CorreccionReplicateEvidenciaIT` | **saltada** |

`CorreccionReplicateEvidenciaIT` exige `fitsense.evidencia.replicate=si`, que llama a Replicate y cuesta dinero; su modo de prevuelo necesita además datos sembrados del escenario real. Queda pendiente, no aprobada. Las tres clases limpiaron sus filas (0 residuales).

Para repetirlo, con Docker:

```bash
docker run -d --name fs-p2d -p 55432:5432 -e POSTGRES_PASSWORD=pruebas -e POSTGRES_DB=fitsense_p2d postgres:16-alpine
for f in $(ls src/main/resources/db/migration/*.sql | sort -V); do
  docker exec -i fs-p2d psql -q -1 -v ON_ERROR_STOP=1 -U postgres -d fitsense_p2d < "$f"; done
docker exec fs-p2d psql -U postgres -d fitsense_p2d -c "CREATE TABLE fitsense_entorno(entorno text primary key); INSERT INTO fitsense_entorno VALUES ('PRUEBAS'); INSERT INTO users (email, password_hash, first_name) VALUES ('participante1@pruebas.invalid','x','Prueba');"
./mvnw verify -Pintegration -Dtest=NINGUNA -Dsurefire.failIfNoSpecifiedTests=false \
  -Dit.test='InvariantesV33IT,PublicacionAtomicaIT,DisparadorDiferidoIT' \
  -Dfitsense.pruebas.url=jdbc:postgresql://127.0.0.1:55432/fitsense_p2d \
  -Dfitsense.pruebas.usuario=postgres -Dfitsense.pruebas.clave=pruebas
```

Las pruebas nuevas de P2-D que pediste (sesión conservada con un ejercicio reclasificado, adaptación con complementos, ejercicio bloqueado) **no están hechas todavía**.

## 5. Decisiones abiertas

1. **V23 y `LOSE_WEIGHT`.** V23 (`ENFORCE_AI_ONLY`) sigue rechazando borradores de IA de `LOSE_WEIGHT` que omiten un slot, aunque 4A v6 no los exige. Se registra `DIVERGENCIA_V23`; cambiarlo es una decisión de enforcement.
2. **Los 21 ejercicios en revisión**, en particular los cardio: en 38 perfiles son lo único que hay.
3. **`MUSCULAR_ENDURANCE` como «trabajo resistido de cualquier patrón»**: mapeo 4B provisional.
4. **El motor de reglas no cubre `CARDIO` ni `MOBILITY`** y solo cubre los slots en `GAIN_MUSCLE` (Paso 5).
