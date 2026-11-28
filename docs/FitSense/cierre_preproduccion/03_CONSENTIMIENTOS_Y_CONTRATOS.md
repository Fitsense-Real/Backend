# 03 · Consentimientos, participación y contratos (Backend-B2)

- **Rama:** `preproduccion/b2-consentimientos-participacion`, sobre `generation` (`77b9df3`).
- **Auditoría de referencia:** P0-03, P0-04 y P0-07.
- **Sin migraciones.** Se reutiliza `user_consents` (V63): `affirmative_action` no tiene `CHECK`, así que distinguir la evidencia no exige cambiar el esquema.
- **No se ha redactado ni aprobado ningún documento legal.** Todos los documentos de las pruebas son **sintéticos** y lo dicen en su referencia de aprobación (`SINTETICO-SOLO-PRUEBAS`).

## 1. B2.1 · Contrato de consentimientos sobre V63: **PREPARADO SIN ACTIVAR**

| Campo del registro (`POST /api/v1/auth/register`) | Qué hace |
|---|---|
| `acceptedTermsVersion` | Ya existía. Se valida contra la versión vigente de las condiciones. |
| `acceptedPrivacyVersion` | Ya existía. Se valida contra la versión vigente de la política de privacidad. |
| `acceptsStudyParticipation` | Ya existía. **Nunca se marca sola**: si es `false`, `is_study_participant=false` y no hay fila de investigación (`ConsentimientoExigidoIT.sinParticipar`). |
| **`acceptedStudyConsentVersion`** (nuevo, ≤ 20) | Versión del consentimiento informado que la app mostró. Solo con `acceptsStudyParticipation=true`. Se valida contra la vigente. |

**Evidencia en `user_consents.affirmative_action` para `STUDY_PARTICIPATION`:**

| Valor | Cuándo | ¿Prueba válida? |
|---|---|---|
| `CASILLA_REGISTRO` | La app envió la versión y coincide con un documento **publicado y aprobado** | Sí |
| `CASILLA_SIN_VERSION_CONFIRMADA` | Hay documento aprobado, pero la app no envió qué versión mostró (app anterior, con la exigencia apagada) | No: no consta qué texto vio |
| `CASILLA_SIN_DOCUMENTO_APROBADO` | No hay documento aprobado: la versión es la etiqueta `MVP-1.0` (`fitsense.study.consent-version`) | **No** |

Se cumple así el requisito de la orden: **`MVP-1.0` ya no puede pasar por prueba válida de un consentimiento**. La fila se conserva por compatibilidad, porque las cuentas y los análisis existentes la usan. También se mantiene `users.consent_version`, que ahora lleva la misma versión registrada.

**Ya existía y sigue igual:**
- `granted_at`, que pone el servidor;
- `source`;
- la relación con la cuenta;
- la retirada (`withdrawn_at`);
- el historial en `GET /api/v1/users/me/consents`.

Esa respuesta añade ahora `approvedDocuments` (tipo → booleano).

## 2. B2.2 · Documentos aprobados y consulta pública: **PREPARADO SIN ACTIVAR**

- **Registro por configuración** (`DocumentosLegales`, `fitsense.legal-documents.items[i].*`). Cada documento declara tipo, versión, título, URL, estado (`DRAFT`, `PUBLISHED` o `RETIRED`), `effective-from` y `approval-reference`.
  - Se eligió configuración y no una tabla para no necesitar una migración mientras V66 está reservada.
  - Además, cada cambio de documento queda en el historial de despliegues.
- **Un documento solo puede estar `PUBLISHED`** si tiene `approval-reference` (quién lo aprobó y cuándo), una URL `https`, título y fecha. Si falta algo, la aplicación **no arranca**. Los `DRAFT` nunca se publican.
- **Vigente** es el `PUBLISHED` de `effective-from` más reciente que ya empezó. Pruebas: `DocumentosLegalesTest` (U) y `ConsentimientoExigidoIT.documentosPublicos` (una versión anterior y un borrador no salen).
- **`GET /api/v1/legal-documents`** es público y no necesita JWT:

  ```json
  { "enforcedOnSignup": true, "privacyRecord": "ACEPTACION",
    "documents": [ { "type": "TERMS_OF_SERVICE", "version": "T-1.0", "title": "…",
                     "url": "https://…", "status": "PUBLISHED", "effectiveFrom": "2026-12-01" } ] }
  ```

  - Lista vacía significa que todavía no hay documentos aprobados. Es la situación actual: **en el repositorio no se declara ninguno**.
  - La referencia de aprobación es interna y no se publica.
- **Compatibilidad:** si un tipo no está en el registro, se usan las propiedades anteriores (`FITSENSE_CONSENT_TERMS_VERSION`, `…_PRIVACY_VERSION`). Si las dos fuentes existen y difieren, la aplicación no arranca.

## 3. B2.3 · Exigencia real en el registro: **PREPARADO SIN ACTIVAR**

`FITSENSE_CONSENT_ENFORCE_ON_SIGNUP=true` se ha probado en un contexto aislado con documentos sintéticos (`ConsentimientoExigidoIT`):

| Caso de la orden | Resultado | Prueba |
|---|---|---|
| Sin versión de condiciones válida | 422, sin cuenta | `registroExigido` |
| Sin privacidad | 422, sin cuenta | `registroExigido` |
| Versión desactualizada (T0 tras T1) | 422, sin cuenta | `registroExigido` |
| Participación sin versión del consentimiento informado, con otra versión o con un borrador | 422, sin cuenta | `participacionExigeConsentimientoInformado` |
| Versión de consentimiento sin aceptar participar | 422 | ídem |
| Todo correcto | 201; 3 filas con las versiones enviadas y `CASILLA_REGISTRO`; login posible | `registroCompleto` |
| No participa | 201; 2 filas; `is_study_participant=false` | `sinParticipar` |
| Registro rechazado | Ninguna cuenta a medias (misma transacción) | Las tres primeras |

**Con la exigencia encendida, ahora también se requiere un consentimiento informado publicado.** Si falta, la aplicación no arranca. Es un cambio intencionado respecto a V63, donde bastaban condiciones y privacidad. Por eso se actualizó `ConsentimientoExigidoIT`, y `ConsentimientoYEdadIT` espera ahora `CASILLA_SIN_DOCUMENTO_APROBADO`, que es el nuevo comportamiento pedido.

**Privacidad.** `FITSENSE_CONSENT_PRIVACY_RECORD` admite dos valores:
- `ACEPTACION` (por omisión): se registra `CASILLA_REGISTRO`.
- `CONSTANCIA`: se registra `CONSTANCIA_DE_LECTURA`, la constancia de que se informó.

El fundamento jurídico lo decide asesoría legal. **Ninguno de los dos sustituye a otro consentimiento que la ley pudiera exigir.**

**Para activarlo en un entorno con participantes, por este orden:**
1. Documentos aprobados.
2. Declararlos como `PUBLISHED` con su referencia.
3. Mobile enviando las tres versiones.
4. `FITSENSE_CONSENT_ENFORCE_ON_SIGNUP=true`.

**No activarlo antes de coordinar Mobile:** una app que no envía `acceptedStudyConsentVersion` no podría inscribir a nadie.

## 4. B2.4 · Participación en el estudio (P0-04): **BLOQUEADO POR DECISIÓN (ambas opciones preparadas)**

> **Actualización 28/11/2026:** la elección funcional está **hecha: opción A**. Kora solo es para personas inscritas en el estudio (`docs/FitSense/integracion_final_estudio/01_OPCION_A_Y_ELEGIBILIDAD.md`, PR C1). Sigue pendiente la **aprobación del protocolo y del consentimiento informado**. El texto de abajo se conserva como registro de la decisión.

Interruptor `fitsense.study.plans-require-participation`, que se aplica en `IamContextFacade`. El dominio (`User`) no cambia.

| Opción | Valor | Comportamiento | Prueba |
|---|---|---|---|
| **A** — Kora exclusivo del estudio | `true` (**por omisión**: es el comportamiento que encontró la auditoría) | Sin participar, o tras retirarse, no hay plan inicial (422) ni cierre semanal | `ParticipacionOpcionAIT` |
| **B** — Kora también para no participantes | `false` | Cualquier cuenta activa recibe plan inicial y semanal; la retirada no quita el servicio; la participación no cambia sola; la población de análisis sigue siendo `is_study_participant AND role='USER'` | `ParticipacionOpcionBIT` |

**No se ha cambiado el valor por omisión.** Hace falta la decisión formal del equipo del estudio y, en su caso, del comité:
- **Si se elige A:** el registro y el consentimiento tienen que decir claramente que el servicio es parte del estudio, y hay que corregir `CONSENTIMIENTO_Y_EDAD.md`, que hoy afirma lo contrario. La casilla de Mobile no puede seguir presentándose como opcional.
- **Si se elige B:** basta con `FITSENSE_STUDY_PLANS_REQUIRE_PARTICIPATION=false`. Antes conviene restringir el panel a participantes (P1-05, en `07`), porque el detalle de administración muestra datos de cualquier cuenta.

## 5. B2.5 · Datos del estudio (P0-07): **BLOQUEADO POR DECISIÓN (especificación)**

No se ha introducido ningún cambio de retención ni de tratamiento. Decisiones que necesitan aprobación, con la especificación técnica de cada alternativa:

| # | Decisión | Hoy | Alternativas y cómo se implementarían |
|---|---|---|---|
| D-1 | Datos tras **retirar** el consentimiento | Se conservan; no hay más planes (opción A) | (a) Conservar y usar. (b) Conservar sin usar: los análisis filtran `withdrawn_at IS NULL` o por fecha. (c) Borrar: reutilizar `EliminacionDeCuentaService` sin borrar la cuenta. Ninguna necesita migración |
| D-2 | **Eliminación de cuenta** y evidencias | Se borra todo, consentimientos incluidos (alternativa A, P-1/P-4) | Conservar una prueba mínima del consentimiento sin identidad (tipo, versión, fecha, `deletion_id`): `account_deletions.rows_deleted` ya admite JSON; una tabla nueva exigiría migración, **coordinada con V66** |
| D-3 | Qué se envía a Replicate | Edad, sexo, medidas, nivel, objetivo, días, minutos, equipamiento, restricciones, catálogo elegible, **`goal_text` y `health_notes`** (`PlanInputSnapshot`) | (a) Mantener y declararlo en la privacidad. (b) No enviar el texto libre: es un cambio en el contexto de la IA (`PlanInputSnapshot`) que **requiere una orden aparte**, porque altera lo que ve el modelo y podría cambiar la calidad de los planes ya evaluados |
| D-4 | `health_notes` y `goal_text` | Texto libre de hasta 500 caracteres; no se interpretan | Limitar el contenido (aviso en la app), dejarlos de enviar (D-3b) o eliminarlos |
| D-5 | Cribado de aptitud física | **No existe** | Si el protocolo lo exige: un cuestionario estructurado (tipo PAR-Q) con su versión en `user_consents` (tipo nuevo, con migración) y que la generación lo exija; o un cribado fuera de la app y registrado por el equipo |
| D-6 | Criterios de exclusión o derivación | No existen en el backend | Lo mismo que D-5: estado de la cuenta (`INACTIVE`) o una marca de exclusión fijada por el equipo, nunca por la app |
| D-7 | Acceso a datos seudonimizados | El detalle de administración muestra nombre y correo de cualquier cuenta USER | Rol de investigador sin datos de contacto, o restringir el detalle a participantes (P1-05, en `07`) |

## 6. Contrato para Mobile (implementado en B2)

| # | Pregunta de la orden | Respuesta |
|---|---|---|
| 1 | ¿Cómo obtener términos y privacidad vigentes? | `GET /api/v1/legal-documents` (sin JWT). `type` es `TERMS_OF_SERVICE` o `PRIVACY_POLICY`. Mostrar `title` y abrir `url` |
| 2 | ¿Cómo obtener el consentimiento informado vigente? | El mismo endpoint, `type=STUDY_PARTICIPATION` |
| 3 | ¿Qué campos envía el registro? | `email`, `password`, `firstName`, `lastName?`, `timezone?`, `acceptsStudyParticipation`, `acceptedTermsVersion`, `acceptedPrivacyVersion` y, si participa, `acceptedStudyConsentVersion`. Hay que enviar exactamente la `version` que devolvió el endpoint |
| 4 | ¿Cómo se representa la participación? | `acceptsStudyParticipation` (casilla **sin marcar por omisión**). En la respuesta, `studyParticipant` y el historial de `GET /users/me/consents` |
| 5 | ¿Qué ocurre si se rechaza o se retira? | Rechazar: no se inscribe. Retirar: `POST /users/me/consents/STUDY_PARTICIPATION/withdraw` (o `POST /users/me/withdraw`). Con la opción A, sin plan desde entonces; con la B, el servicio sigue |
| 6 | ¿Cómo interpretar un 422 por versión desactualizada? | 422 con `message` del tipo "La version aceptada de X no es la vigente (V)". Volver a pedir `/legal-documents`, mostrar el documento nuevo y repetir el registro |
| 7 | ¿Cómo interpretar un 429? | Lo añade B1 (`02`): `Retry-After` en segundos; no reintentar de forma automática |
| 12 | Errores de seguridad | 401 (sin sesión o JWT revocado), 403 (sin permiso), 404 (recurso ajeno o inexistente, sin distinguir), 422 (regla de negocio), 429 (límite) |

## 7. Pendiente

- **Documentos aprobados y su referencia (P-6).** Sin ellos, todo B2 queda **preparado sin activar**.
- ~~Decisión A/B (P0-04)~~ (resuelta el 28/11/2026: opción A) y decisiones D-1 a D-7.
- **Mobile:** pantallas de documentos, envío de `acceptedStudyConsentVersion` y casilla sin marcar por omisión.
