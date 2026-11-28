# 01 · Opción A y elegibilidad

**Orden:** cierre del registro para el estudio, correo y preparación operativa (28/11/2026). **Base:** `generation` en `41c498ab`.
**PR:** [#96](https://github.com/Fitsense-Real/Backend/pull/96) (Backend-C1), **fusionada en `generation`** el 28/11/2026 (merge `434a9dc0`, cabeza `401ca56`), tras la revisión final (§6).

## 1. La decisión

**Decisión de producto: opción A.** Kora solo se usa por personas inscritas en el estudio de tesis de 12 semanas. No es una aplicación abierta.

Lo que **no** cambia esta decisión:
- la creación de una cuenta no es un consentimiento de investigación y no inscribe a nadie;
- la participación tiene que ser informada, voluntaria, registrada y revocable, según el protocolo **que todavía no está aprobado** (`08`).

La opción B (cualquier cuenta recibe planes) queda **fuera del alcance y desactivada**. Su código se conserva porque, con la propiedad en `true`, no supone ningún riesgo.

| Propiedad | Valor | Efecto |
|---|---|---|
| `fitsense.study.plans-require-participation` | `true` (por omisión y aprobado) | Solo un participante activo y elegible recibe planes |
| Con el perfil `prod` y `false` | — | **La aplicación no arranca** (`GuardasDeSeguridadEnProduccion`), salvo que se declare a propósito en `fitsense.security.guard-overrides` |

## 2. Quién es «participante activo y elegible»

Con la configuración actual, todas estas condiciones a la vez:
1. cuenta `ACTIVE`;
2. `is_study_participant = true`;
3. `consent_granted_at` no nulo;
4. `withdrawn_at` nulo.

Con el **registro definitivo** (`fitsense.consent.enforce-on-signup=true`, que no se activa en esta orden), hace falta además:

5. un consentimiento `STUDY_PARTICIPATION` **válido y no retirado**, con evidencia `CASILLA_REGISTRO`. Esa evidencia solo existe si había un documento aprobado vigente y la app confirmó qué versión mostró.

La casilla de antes (`MVP-1.0`, evidencia `CASILLA_SIN_DOCUMENTO_APROBADO`) **no** cuenta como consentimiento informado.

La edad mínima de 18 años se comprueba al crear el perfil (`fitsense.profile.minimum-age`). Sin perfil no hay plan.

## 3. Verificación, ruta por ruta

| Requisito de la orden | Comportamiento | Prueba |
|---|---|---|
| El registro no inscribe a nadie automáticamente | `acceptsStudyParticipation` es obligatorio en el JSON (sin él, 400 y ninguna cuenta). Con `false`, la cuenta no participa, no tiene código P-xxx y no tiene ninguna aceptación de investigación | `OpcionAExclusivaIT.registroNoInscribe` |
| Una cuenta sin participación no genera planes | 422 con `details[0]="PARTICIPACION_NO_VIGENTE"` | `OpcionAExclusivaIT.noParticipanteNiSincronaNiAsincrona` |
| Una cuenta con la participación retirada no genera rutinas nuevas | Igual, por las dos rutas | `OpcionAExclusivaIT.retiradoNoGenera` |
| Participación activa y elegible: sí genera | Síncrona 201; asíncrona 202 y `COMPLETED` | `OpcionAExclusivaIT.participanteSiGenera` |
| Síncrona y asíncrona aplican las mismas restricciones | Mismo criterio (`IamContextFacade.isEligibleForPlanGeneration`) y mismo código. **El perfil se comprueba antes:** sin perfil, la asíncrona da 422 «Completa tu perfil» y la síncrona 404 | Las anteriores y `02` §3 |
| El cierre semanal no genera para personas retiradas | No entran en la lista de la zona. Si se llama al ciclo directamente, cierra y mide su última semana, pero no publica la nueva | `OpcionAExclusivaIT.cicloNoPublicaParaRetirados`, `ParticipacionOpcionAIT` |
| Las operaciones pendientes no publican después de una retirada | **Nuevo en C1.** La elegibilidad se vuelve a comprobar **al publicar** (`persist` y `publishAdapted`), dentro de la transacción que publica, con la fila de la cuenta `FOR SHARE` | `OpcionAExclusivaIT.pendienteNoPublicaTrasRetirarse`, `retiradaDuranteLaGeneracion` y `retiradaSimultaneaEspera` |
| La analítica considera únicamente participantes autorizados | Panel y exportación filtran `is_study_participant AND role='USER'`. Los retirados se conservan con `study_status=WITHDRAWN`: sus semanas anteriores son datos del estudio hasta que se decida D-1 | `OperacionYExportacionIT` (exportación y detalle de no participante: 404) |

### 3.1 Por qué hace falta la comprobación al publicar

La fase 1 ya comprobaba la participación. La IA tarda hasta minutos (fase 2), y sin una segunda comprobación una retirada en ese intervalo acababa en un plan nuevo, porque la publicación (fase 3) no volvía a mirar.

Ahora la fase 3 lee la fila de `users` con `FOR SHARE` en su propia transacción:
- una retirada simultánea (que actualiza esa fila) **espera** a que la publicación termine;
- o bien la publicación espera a la retirada y la ve.

No hay ningún orden de los dos sucesos en el que se publique un plan para alguien ya retirado. Si se publicó antes de la retirada, era legítimo: la persona aún participaba.

La comprobación exige la transacción de la publicación (`Propagation.MANDATORY`): fuera de ella el bloqueo no protegería nada.

En la asíncrona, una operación pendiente cuya persona se retira termina en `FAILED` con `reason_code=NO_GENERABLE`. No se añade un `reason_code` nuevo, para no tocar el esquema ni el contrato de `/plan/generation`.

## 4. Lo que hay que comprobar antes de inscribir a nadie

1. En la base del estudio, **ninguna cuenta participante sin consentimiento válido**:

   ```sql
   SELECT count(*) FROM users u
    WHERE u.is_study_participant AND u.withdrawn_at IS NULL
      AND NOT EXISTS (SELECT 1 FROM user_consents c WHERE c.user_id = u.user_id
                       AND c.consent_type = 'STUDY_PARTICIPATION' AND c.withdrawn_at IS NULL
                       AND c.affirmative_action = 'CASILLA_REGISTRO');
   -- 0 antes de abrir la inscripción
   ```

   Con el registro definitivo, esas cuentas no reciben planes, pero seguirían contando como participantes en el panel.
2. El usuario demo 48 y las cuentas de QA **no** deben existir en la base del estudio. Esta orden no los toca. Si el registro definitivo se encendiera en el entorno actual, el demo 48 dejaría de recibir planes sin que cambien sus datos.
3. `REGISTRO_NO_DEFINITIVO` **no** debe aparecer en el log de arranque del entorno del estudio.

## 5. Estado

| Punto | Clasificación |
|---|---|
| Opción A como comportamiento del backend | **RESUELTO** (integrado: #96) |
| Opción B bloqueada en `prod` | **RESUELTO** (integrado: #96) |
| Registro definitivo de la opción A (participación obligatoria y consentimiento válido para planes) | **PREPARADO SIN ACTIVAR** |
| Protocolo y consentimiento informado aprobados | **BLOQUEADO POR APROBACIÓN** |
| Destino de los datos de quien se retira (D-1) | **BLOQUEADO POR APROBACIÓN** |

## 6. Revisión final antes del merge (orden del 28/11/2026)

Escenarios obligatorios y prueba que cubre cada uno (`OpcionAExclusivaIT`, salvo que se indique otra):

| # | Escenario | Prueba | Resultado |
|---|---|---|---|
| 1 | Participación activa | `participanteSiGenera` | Síncrona 201; asíncrona 202 y `COMPLETED` |
| 2 | Ausencia de participación | `noParticipanteNiSincronaNiAsincrona` | 422 `PARTICIPACION_NO_VIGENTE` por las dos rutas |
| 3 | Participación retirada | `retiradoNoGenera` | Ídem; fuera del cierre semanal |
| 4 | Retirada durante una generación | `retiradaDuranteLaGeneracion` | La fase 3 no publica |
| 5 | Publicación después de la retirada | `pendienteNoPublicaTrasRetirarse`, `cicloNoPublicaParaRetirados` | `FAILED NO_GENERABLE`; el ciclo no publica |
| 6 | Dos operaciones concurrentes | `dosPublicacionesConcurrentes`, `dosPublicacionesYRetiradaConcurrentes`, `retiradaSimultaneaEspera` | Publica una sola. Nunca un plan después de la retirada ni dos planes. La retirada espera al `FOR SHARE` |
| 7 | Reintentos de publicación | `reintentosDePublicacion` | Tres reintentos y una petición nueva: todos rechazados, 0 planes |
| 8 | Síncrona y asíncrona | 1–3 y `mensajesSinIdInterno` | Mismo criterio y mismo código |
| 9 | Perfil ausente | `perfilAusente` | Asíncrona 422 `PERFIL_INCOMPLETO` (nuevo en la revisión); síncrona 404; ninguna operación ni plan |
| 10 | Participante no elegible | `cuentaNoActivaNoElegible`; `ConsentimientoExigidoIT.elegibilidadPorConsentimientoValido` | Cuenta `INACTIVE` y casilla `MVP-1.0` con el registro definitivo: no elegibles al pedir ni al publicar |

**Seguridad del consentimiento (§3 de la orden):**

| Comprobación | Evidencia |
|---|---|
| No se registra participación automáticamente | `registroNoInscribe` |
| `MVP-1.0` no es evidencia válida | `casillaSinDocumentoNoEsValida`, `codigosSinDocumentos` (`DOCUMENTO_NO_PUBLICADO`) |
| Versiones existentes y vigentes; cambio durante el registro | `ConsentimientoExigidoIT.registroExigido` (`VERSION_NO_VIGENTE` con versión anterior e inventada) |
| Sin cuentas a medias | Todos los rechazos comprueban 0 cuentas |
| Documentos aceptados individualmente | `POST /users/me/consents` por tipo (`ConsentimientoYEdadIT`); la participación solo al registrarse |
| El servidor no confía en Mobile; una petición directa no elude nada | `ConsentimientoExigidoIT.peticionDirectaNoEludeNada`: los campos falsificados (`studyParticipant`, `role`, `participantCode`, `consentVersion`, `affirmativeAction`) se ignoran |
| La generación comprueba además la participación activa | Escenarios 1–10 |
| Sin cambios en datos ni evidencias históricas | Sin migraciones. Las filas `CASILLA_SIN_DOCUMENTO_APROBADO` existentes no se tocan; solo dejan de contar con el registro definitivo |

**Hallazgo anterior a esta orden.** Algunos errores de generación que ya existían llevan el id interno **de la propia cuenta** en `message`, por ejemplo 404 «Perfil del usuario no encontrado: 5» o 422 «El usuario 5 ya tiene un plan activo…». No exponen datos de otras personas, y los errores nuevos de C1 no los llevan (`mensajesSinIdInterno`). La corrección está en [#103](https://github.com/Fitsense-Real/Backend/pull/103) (C8), abierta y sin fusionar (`08`).
