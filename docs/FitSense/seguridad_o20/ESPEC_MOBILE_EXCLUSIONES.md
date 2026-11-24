# Especificación para Mobile · exclusiones por dolor directas y por equivalencia (O20)

**Origen:** «ORDEN — AJUSTE FINAL DE SEGURIDAD O20», punto 4.
**Alcance:** contrato del backend en la PR de seguridad O20. **Esta PR no modifica Mobile.** Este documento describe lo que Mobile puede mostrar cuando decida adoptarlo.
**Compatibilidad:**
- Todos los cambios son aditivos.
- Los dos campos nuevos del historial van al final.
- Los dos endpoints son nuevos.
- No cambia ningún campo, código de estado ni ruta existente.

Una versión de Mobile que no conozca los campos nuevos sigue funcionando, siempre que su parser ignore los campos desconocidos, como ya exigió `MATRIZ_CONTRATOS_API`.

## 1. Cómo distingue el backend cada caso

| Caso | Qué lo origina | Qué se excluye | Hasta cuándo |
|---|---|---|---|
| **Directa, confirmada** | La persona marcó **ese ejercicio** con dolor (`DOLOR_IDENTIFICADO`), o lo identificó después | El ejercicio | Hasta que la persona pida reactivarlo (`/reactivation`) |
| **Directa, sin identificar** | La sesión se omitió o se cerró con dolor **sin decir qué ejercicio** (`DOLOR_SESION_SIN_IDENTIFICAR`) | Cada ejercicio de esa sesión (no se sabe cuál fue) | Hasta su reactivación. Nada se reactiva solo |
| **Equivalente, permanente** | Variante equivalente de una directa **confirmada** | La variante | Lo que dure la directa de la que deriva |
| **Equivalente, provisional** | Variante equivalente de una directa **sin identificar** | La variante | Mientras la directa siga sin confirmar. **No caduca sola**: a las dos semanas con plan se pide confirmación (`confirmationRequired`) |
| **Precaución por ángulo** (`PRECAUTION`) | El mismo movimiento con otro ángulo o apoyo (inclinado, declinado, tumbado, de rodillas) que una directa. No es equivalente, pero no se da por seguro tras dolor | La variante de otro ángulo | Lo mismo que la directa de la que deriva: permanente si está confirmada; provisional, con petición de confirmación, si no |

**Qué es «equivalente»** (`equivalenceLevel`):
- **`VARIANTE_MISMO_EJERCICIO`:** el mismo nombre salvo el sufijo `v. N`. Por ejemplo, «dumbbell front raise» y «… v. 2».
- **`MISMO_GESTO`:** está en el mismo grupo de la lista explícita y revisada del backend (`equivalenceGroup`, con su `equivalenceJustification`). Por ejemplo, «dumbbell front raise» y «dumbbell seated front raise», del grupo «G2-0xx front raise».
- **`PRECAUCION_OTRO_ANGULO`** (origen `PRECAUTION`): mismo gesto, otro ángulo, de la lista de precauciones (`equivalenceGroup` «PA-0xx …»). Por ejemplo, «lever incline chest press» y «lever chest press».

Coincidir en patrón y músculo **no** basta para ser equivalente.

## 2. Endpoints

Todos van sobre `/api/v1/users/me` y requieren autenticación. Nadie ve ni modifica lo de otra persona: lo ajeno responde 404.

### 2.1 `GET /users/me/pain-exclusions` (existente: historial de filas)

Es el mismo contrato de siempre, con **dos campos nuevos al final**:

| Campo | Tipo | Significado |
|---|---|---|
| `confirmed` | boolean | `false` en un dolor de sesión aún sin identificar (un reporte general); `true` si se marcó ese ejercicio o se identificó después |
| `identifiedAt` | fecha-hora ISO o `null` | Cuándo se identificó después |

El campo `reason` conserva el motivo **original**. Una fila identificada después sigue siendo `DOLOR_SESION_SIN_IDENTIFICAR`, pero con `confirmed: true`.

### 2.2 `GET /users/me/pain-exclusions/effective` (nuevo)

Devuelve lo que **hoy** no se prescribe por dolor: una entrada por ejercicio.

```json
[
  {
    "exerciseId": 537, "exerciseName": "Elevación frontal con mancuerna",
    "origin": "DIRECT", "confirmed": false, "provisional": false, "confirmationRequired": false,
    "derivedFromExerciseId": null, "derivedFromExerciseName": null,
    "equivalenceLevel": null, "equivalenceGroup": null, "equivalenceJustification": null,
    "sourcePlannedWorkoutId": 9123, "eventDate": "2026-01-09",
    "canIdentify": true, "canReactivate": true
  },
  {
    "exerciseId": 707, "exerciseName": "Elevación frontal sentado con mancuerna",
    "origin": "EQUIVALENT", "confirmed": false, "provisional": true, "confirmationRequired": false,
    "derivedFromExerciseId": 537, "derivedFromExerciseName": "Elevación frontal con mancuerna",
    "equivalenceLevel": "MISMO_GESTO", "equivalenceGroup": "G2-0xx front raise",
    "equivalenceJustification": "mismo gesto (front raise), cambia material, lateralidad, apoyo sentado/de pie",
    "sourcePlannedWorkoutId": null, "eventDate": null,
    "canIdentify": false, "canReactivate": false
  }
]
```

- **`origin`:** `DIRECT`, `EQUIVALENT` o `PRECAUTION`.
- **`confirmationRequired`:** `true` en lo que deriva de un dolor de sesión sin identificar con dos semanas con plan cumplidas. Sigue excluido: es la señal para pedir la confirmación (§2.5).
- **`canIdentify`:** es `true` solo en una entrada `DIRECT` sin confirmar.
- **`canReactivate`:** es `true` solo en una entrada `DIRECT`.
- **Las `EQUIVALENT` y `PRECAUTION` no se reactivan una a una:** vuelven al reactivar el ejercicio del que derivan, o al declararlo no relacionado (§2.3). Nunca vuelven solas por el paso del tiempo.
- **Nombres:** `exerciseName` y `derivedFromExerciseName` van en español, del catálogo.
- **Uso del texto técnico:** `equivalenceGroup` y `equivalenceJustification` son técnicos, pensados para soporte o para un «¿por qué?» secundario, no para el texto principal.

### 2.3 `POST /users/me/pain-exclusions/{exerciseId}/identification` (nuevo)

La persona indica **después** que ese ejercicio de una sesión con dolor fue el que le dolió.

**Cuerpo:** `{"confirmPain": true, "plannedWorkoutId": 9123, "unrelatedExerciseIds": [578, 150]}`.
- `plannedWorkoutId` es opcional; sin él, se confirman todas las filas aún sin identificar de ese ejercicio.
- `unrelatedExerciseIds` es opcional: los demás ejercicios de **esa** sesión que la persona confirma que no tuvieron que ver. Solo esos se levantan, cada uno con el control de seguridad de la reactivación. Si uno no es de esa sesión, o es el propio identificado, la petición se rechaza con 422 y no cambia nada.

**Respuestas:**

| Código | Cuándo |
|---|---|
| 200 | Devuelve `exerciseId`, `exclusionesConfirmadas`, `identificadaEn`, `equivalentesExcluidos` (equivalentes y precauciones, ahora permanentes), `noRelacionadosLevantados`, `noRelacionadosNoLevantados` (id → motivo: choca con una restricción vigente, o sigue fuera por ser equivalente o precaución del identificado), `otrosDeLaSesionSiguenExcluidos` y `nota`, en español |
| 400 | Sin cuerpo, o con `confirmPain: null` |
| 422 | `confirmPain: false`; o un `unrelatedExerciseIds` que no es de esa sesión o es el identificado |
| 404 | Nada pendiente de identificar: otra persona, otra sesión, o ya confirmado |

**Qué cambia al identificar:**
- **Solo levanta lo que se nombra en `unrelatedExerciseIds`.** Los no nombrados siguen excluidos, porque también pudieron doler. Pueden declararse después con su propia `/reactivation`.

### 2.4 `POST /users/me/pain-exclusions/{exerciseId}/reactivation` (existente, sin cambios)

Al reactivar un ejercicio directo, sus equivalentes derivados vuelven a ser candidatos en la misma lectura. No hay que reactivarlos aparte.

### 2.5 `GET /users/me/pain-exclusions/confirmation-requests` (nuevo): cómo se pide la confirmación

Devuelve una entrada por sesión con dolor sin identificar que espera respuesta:

```json
[{"plannedWorkoutId": 9123, "eventDate": "2026-01-09", "confirmationRequired": true,
  "exercises": [{"exerciseId": 537, "exerciseName": "Elevación frontal con mancuerna"}, {"exerciseId": 578, "exerciseName": "…"}]}]
```

- **`confirmationRequired: false`:** se puede preguntar ya, pero sin insistir (justo después de la omisión).
- **`confirmationRequired: true`:** ya pasaron dos semanas con plan. Mobile debe **pedirlo de forma activa**, por ejemplo al abrir la app o el plan: «¿Recuerdas qué ejercicio te dolió el viernes 9? ¿Y cuáles no tuvieron que ver?».
- **Respuesta:** `/identification` con `unrelatedExerciseIds`, o `/reactivation` ejercicio a ejercicio si no lo recuerda pero ya no tiene molestias.
- **Mientras no conteste,** nada de la sesión vuelve a prescribirse. El backend no lo levanta por el paso del tiempo.

### 2.6 Planes abiertos: qué ya no se puede ejecutar

- **Tras una omisión por dolor** (y tras `/identification` o `/reactivation`), Mobile debe **volver a leer el plan** (`GET /users/me/plan/current` o `/plan/{id}`). La respuesta de la omisión no trae el plan entero.
- **En cada prescripción**, `safetyBlockReason` no nulo (para el dolor, `dolor_registrado`) significa que ya no puede ejecutarse. Solo puede registrarse como omitida: `PUT /sessions/{s}/exercises/{pe}` con `actualSets: 0` y un `skipReason`. Con trabajo, la API responde 422 `SESION_BLOQUEADA_POR_SEGURIDAD`.
- **El resto de la sesión** se puede hacer: `safetyBlocked` de la sesión sigue en `false` por un ejercicio con dolor.
- **Para explicar por qué**, se cruza el `exerciseId` de la prescripción con `/effective`: `DIRECT`, `EQUIVALENT` (variante de…) o `PRECAUTION` (mismo movimiento con otro ángulo que…). Las prescripciones marcadas son exactamente las de `/effective`, como comprueba `CicloCompletoHttpIT`.

## 3. Presentación sugerida

Es solo una propuesta; Mobile decide.

1. **«Ejercicios que te dolieron»:** `DIRECT` con `confirmed: true`. Acción: «Ya no me duele» (`/reactivation`).
2. **«Sesión con dolor: ¿qué ejercicio fue?»:** `DIRECT` con `confirmed: false`, agrupados por `sourcePlannedWorkoutId` y `eventDate`. Acción principal: «Fue este» (`/identification` con `plannedWorkoutId`). Secundaria: «Este no me dolió» (`/reactivation`).
3. **«También evitamos, por ser el mismo movimiento»:** `EQUIVALENT`, con el texto «Variante de *derivedFromExerciseName*», y `PRECAUTION`, con «Mismo movimiento con otro ángulo que *derivedFromExerciseName*».
   - Si `provisional: true`, añadir «mientras confirmamos qué te dolió (próximas semanas)».
   - Sin acción propia.

Textos que conviene evitar:
- «diagnóstico» o «lesión»: el backend no guarda ni pide afirmaciones médicas;
- «para siempre»: todo se puede reactivar de forma explícita.

## 4. Lo que Mobile no necesita calcular

- Ni equivalencias, ni precauciones, ni plazos: el backend devuelve la lista ya resuelta y cuándo pedir confirmación.
- Ni la seguridad del plan: un ejercicio excluido no llega a un plan publicado. Si ya estaba en una sesión abierta, la API lo marca con `safetyBlockReason`, como hasta ahora.

## 5. Pruebas del contrato en el backend

| Prueba | Qué cubre |
|---|---|
| `CicloCompletoHttpIT.dolorYReincorporacion` | Sesión omitida por dolor. El historial marca `confirmed: false`; `/effective` separa `DIRECT` de `EQUIVALENT` provisional; el plan siguiente no trae nada de `/effective`. `/identification`: 422 sin confirmar, 200 con confirmación y 404 si se repite; solo el identificado pasa a `confirmed: true` y todos siguen `ACTIVA`. Después, la `/reactivation` de siempre |
| `CicloCompletoHttpIT` (cierre final) | Plan releído tras la omisión: marcado = `/effective`, sin bloquear la sesión entera. `/confirmation-requests` lista la sesión. `/identification` con `unrelatedExerciseIds`: solo ese pasa a `REACTIVADA` |
| `ContratoErroresApiIT.reincorporacionSinConfirmacion` | `/identification` con `confirmPain: null` → 400; sin nada pendiente → 404 |
| `DolorFamiliaEquivalenteIT.dolorDeSesionSinIdentificar` | Ciclo real: la familia provisional durante la regeneración inmediata y la semana siguiente; después, no. Identificar la hace permanente; solo la reactivación explícita la devuelve |
| `PainMemoryFamiliaTest` | Las reglas, una a una, sin base de datos |
