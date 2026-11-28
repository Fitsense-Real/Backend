# Consentimiento y edad (PR D)

> **Este documento no es un documento de consentimiento.**
> - No afirma que exista una aprobación de un comité de ética.
> - No introduce ningún texto legal.
>
> Lo que deja preparado es la parte técnica: un registro trazable de consentimientos y la validación de edad. **La exigencia de aceptar condiciones y privacidad queda apagada** hasta que existan documentos aprobados.

## 1. Edad mínima: 18 años

| | |
|---|---|
| **Dónde se valida** | En el backend, al crear el perfil (`UserProfileCommandServiceImpl`). La fecha de nacimiento ya no se puede cambiar después, porque `update` no la modifica |
| **Respuesta** | 422 *"Debes tener al menos 18 anos para usar la aplicacion."*; no se crea el perfil |
| **Configuración** | `fitsense.profile.minimum-age=18`. Un valor inferior a 18 impide arrancar |
| **Cuentas existentes** | No se tocan: la regla solo se aplica a perfiles nuevos. Si ya existe un perfil de un menor, no se detecta automáticamente (ver el apartado 4) |
| **Sin la fecha** | Hoy, sin perfil no hay rutina. La cuenta puede existir sin perfil, pero no recibe planes |

**Minimización de la fecha de nacimiento: propuesta, no implementada.**

- La IA solo recibe la edad (`age`), nunca la fecha (`PlanInputSnapshot.User.age`).
- El backend usa la fecha completa para calcular la edad en cada momento.

| Alternativa | Qué cambia | Coste |
|---|---|---|
| **M0** (actual) | Fecha completa | Es el dato más preciso, y también el más identificativo |
| **M1** | Mes y año (día fijo en 1) | Migración: `birth_date = date_trunc('month', …)`. La edad puede desviarse un mes. Habría que cambiar la app |
| **M2** | Solo el año, más la declaración "tengo 18 o más" | Migración y cambio en la app. Con el año solo no se puede verificar los 18 exactos alrededor del cumpleaños; eso es lo que cubre la declaración |

Las tres son compatibles con el esquema, porque la columna `birth_date DATE` se mantiene. Cualquiera de las tres requiere una migración de datos y un cambio en la app coordinados: **no se aplica sin aprobación** (ver [PENDIENTES_APROBACION](PENDIENTES_APROBACION.md)).

## 2. Consentimientos versionados (V63 `user_consents`)

| Columna | Qué registra |
|---|---|
| `consent_type` | `STUDY_PARTICIPATION` (consentimiento informado de investigación), `TERMS_OF_SERVICE` o `PRIVACY_POLICY` |
| `document_version` | La versión exacta que se aceptó |
| `granted_at` | Cuándo |
| `affirmative_action` | Qué hizo la persona: `CASILLA_REGISTRO`, `BOTON_ACEPTAR` o `REGISTRO_ANTERIOR_A_V63` |
| `source` | `APP`, `WEB` o `MIGRACION` |
| `withdrawn_at` | Cuándo se retiró. La fila no se borra mientras exista la cuenta |

**Investigación frente a uso del servicio.** El consentimiento de investigación es independiente de aceptar las condiciones o la privacidad:
- Se puede usar la app sin participar en el estudio (`acceptsStudyParticipation=false`).
- La participación solo se acepta al registrarse.
- Una reincorporación la gestiona el equipo del estudio.

**Retirada:**

| Tipo | Retirada | Qué ocurre después |
|---|---|---|
| `STUDY_PARTICIPATION` | `POST /api/v1/users/me/consents/STUDY_PARTICIPATION/withdraw` o el `POST /api/v1/users/me/withdraw` de siempre. Ambos dejan `withdrawn_at` | <ul><li>No se generan más planes.</li><li>La app sigue accesible.</li><li>**Los datos ya recogidos se conservan**: es el comportamiento anterior, sin cambios.</li><li>Si se pueden usar en el análisis tras la retirada es una **decisión pendiente** del estudio y del comité.</li><li>La persona puede además eliminar su cuenta y sus datos (PR A).</li></ul> |
| `TERMS_OF_SERVICE`, `PRIVACY_POLICY` | No se retiran (422) | Quien no los acepta deja de usar el servicio eliminando su cuenta |

**Migración.**
- Las participaciones existentes (`users.consent_*`) se copian con su fecha, su versión (`MVP-1.0`) y su retirada, y la acción queda como `REGISTRO_ANTERIOR_A_V63`.
- Las columnas antiguas de `users` se conservan por compatibilidad.
- Lo comprueba `MigracionCuentasV60aV63IT`.

**Al eliminar la cuenta** (alternativa A), los consentimientos se borran con todo lo demás. Conservar una prueba mínima del consentimiento después de la eliminación es una de las decisiones de [PENDIENTES_APROBACION](PENDIENTES_APROBACION.md).

## 3. Contrato

**Registro.** Dos campos nuevos, opcionales mientras la exigencia esté apagada:

```json
{"email": "…", "password": "…", "firstName": "…", "timezone": "America/Lima",
 "acceptsStudyParticipation": true,
 "acceptedTermsVersion": "<versión vigente>", "acceptedPrivacyVersion": "<versión vigente>"}
```

- Si se envía una versión que no es la vigente, o cuando no hay ninguna configurada, responde 422 y no se crea la cuenta.
- Con `fitsense.consent.enforce-on-signup=true`, los dos campos son obligatorios.
- La cuenta y sus consentimientos se guardan en la misma transacción.

**Consulta**

```http
GET /api/v1/users/me/consents
```

```json
{"currentVersions": {"STUDY_PARTICIPATION": "MVP-1.0", "TERMS_OF_SERVICE": null, "PRIVACY_POLICY": null},
 "enforcedOnSignup": false,
 "history": [{"type": "STUDY_PARTICIPATION", "documentVersion": "MVP-1.0", "grantedAt": "2026-11-19T21:10:00Z",
              "affirmativeAction": "CASILLA_REGISTRO", "source": "APP", "withdrawnAt": null}]}
```

**Aceptar una versión nueva** (por ejemplo, tras actualizar la política):

```http
POST /api/v1/users/me/consents
{"type": "PRIVACY_POLICY", "documentVersion": "<versión vigente>"}
```

Responde 201. Si la versión no es la vigente, si no hay documento aprobado o si el tipo es `STUDY_PARTICIPATION`, responde 422.

**Retirar el consentimiento de investigación**

```http
POST /api/v1/users/me/consents/STUDY_PARTICIPATION/withdraw
```

Responde 204. Para los otros dos tipos responde 422.

## 4. Configuración y activación

| Propiedad | Por omisión | Activación (requiere aprobación) |
|---|---|---|
| `FITSENSE_CONSENT_TERMS_VERSION` | vacía: no hay documento aprobado | Identificador de la versión aprobada de las condiciones |
| `FITSENSE_CONSENT_PRIVACY_VERSION` | vacía | Identificador de la versión aprobada de la política de privacidad |
| `FITSENSE_CONSENT_ENFORCE_ON_SIGNUP` | `false` | `true` solo con las dos versiones anteriores (si faltan, la aplicación no arranca) y con la app ya mostrando los textos |
| `fitsense.study.consent-version` | `MVP-1.0` | La versión del consentimiento informado que apruebe el comité |
| `fitsense.profile.minimum-age` | 18 | — |

**La captación real de participantes no se habilita en el backend.** Depende de publicar la app y de tener los documentos aprobados. Hasta entonces:
- no hay textos;
- no hay versiones configuradas;
- la exigencia sigue apagada.

**Perfiles de menores ya existentes.** Antes de abrir la captación, el equipo puede revisarlos con una consulta de solo lectura:

```sql
SELECT count(*) FROM user_profiles WHERE birth_date > (current_date - interval '18 years');
```

Qué hacer con los que aparezcan es una decisión del estudio.

## 5. Pruebas

| Prueba | Qué comprueba |
|---|---|
| `ConsentimientoYEdadIT`, con la configuración por omisión | <ul><li>A un día de cumplir 18 años: 422 y sin perfil.</li><li>Con 18 años cumplidos hoy: 201.</li><li>La participación queda registrada con versión, acción y origen; sin participación no se registra nada.</li><li>Sin documento aprobado no se acepta nada.</li><li>La retirada se puede hacer por los dos caminos y queda en el historial.</li><li>Condiciones y privacidad no se pueden retirar.</li><li>Al eliminar la cuenta se borran sus consentimientos.</li></ul> |
| `ConsentimientoExigidoIT`, con versiones **sintéticas** `PRUEBA-T1` y `PRUEBA-P1` (no son documentos reales) | <ul><li>Responde 422 si falta una aceptación o la versión no es la vigente, y no se crea ninguna cuenta a medias.</li><li>Con las dos aceptaciones: 201 y tres filas.</li></ul> |
| `MigracionCuentasV60aV63IT` | <ul><li>Cuentas existentes: la secuencia de P-xxx continúa.</li><li>`token_version` queda en 0.</li><li>El consentimiento se copia con su retirada.</li><li>Un reinicio de la aplicación no ejecuta ninguna migración nueva.</li></ul> |
