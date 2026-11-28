# 08 · Pendientes, clasificados

Clasificación de la orden:
- **RESUELTO**: en una PR de esta orden o ya en `generation`;
- **PREPARADO SIN ACTIVAR**;
- **BLOQUEADO POR APROBACIÓN**;
- **BLOQUEADO POR INFRAESTRUCTURA**;
- **PENDIENTE**.

**Actualizado el 28/11/2026, tras la integración final.** «RESUELTO (integrado)» quiere decir fusionado en `generation` (#96, #97, #98 y #100). «PREPARADO SIN ACTIVAR (PR …)» quiere decir que está en una PR abierta, sin fusionar.

## A. Registro, documentos y consentimiento

| # | Pendiente | Clasificación | Qué falta | Quién |
|---|---|---|---|---|
| A-1 | Opción A: solo participantes reciben planes, por todas las rutas, también al publicar | **RESUELTO** (integrado: #96) | — | — |
| A-2 | Opción B desactivada y bloqueada en `prod` | **RESUELTO** (integrado: #96) | — | — |
| A-3 | Términos y condiciones, política de privacidad y consentimiento informado **aprobados** | **BLOQUEADO POR APROBACIÓN** | Texto final; aprobación del comité de ética para el consentimiento informado; referencia de aprobación; URL `https` estable con un contenido que no cambie sin cambiar de versión | Equipo del estudio y comité |
| A-4 | Protocolo del estudio aprobado | **BLOQUEADO POR APROBACIÓN** | Sin él no se inscribe a nadie | Comité |
| A-5 | Contrato de registro y errores con códigos, `requiredForSignup` y `PERFIL_INCOMPLETO` | **RESUELTO** (integrado: #96) | — | — |
| A-5b | Errores anteriores con el id interno de la propia cuenta («Perfil del usuario no encontrado: 5»…) | **PREPARADO SIN ACTIVAR** ([#103](https://github.com/Fitsense-Real/Backend/pull/103)) | Revisión y merge | Backend |
| A-5c | CORS: un navegador no puede leer `Retry-After` del 429 | **PREPARADO SIN ACTIVAR** ([#101](https://github.com/Fitsense-Real/Backend/pull/101)) | Revisión y merge; mientras tanto, mensaje genérico en la app | Backend y Mobile |
| A-6 | Registro definitivo (`enforce-on-signup=true`): participación obligatoria y solo consentimiento válido para planes | **PREPARADO SIN ACTIVAR** | A-3, A-7 y una fecha | Equipo y Mobile |
| A-7 | Compilación de Mobile que lea `/legal-documents`, envíe las tres versiones y trate los códigos | **BLOQUEADO POR APROBACIÓN** (depende de A-3) | Pantallas y pruebas en Mobile. No se asume que estén fusionadas | Mobile |
| A-8 | Re-consentimiento si cambia la versión del consentimiento informado durante el estudio | **BLOQUEADO POR APROBACIÓN** | Decidir si las personas ya inscritas tienen que volver a aceptar. Hoy un consentimiento válido sigue valiendo con una versión posterior, y la participación solo se acepta al registrarse | Comité |
| A-9 | Verificación de correo, contra la enumeración en el registro | **PENDIENTE** (P1-10) | Necesita una migración | Backend |
| A-10 | D-1..D-7 del protocolo de datos: retirada, eliminación y evidencias, datos enviados a Replicate, `health_notes`, cribado de aptitud, criterios de exclusión, acceso seudonimizado | **BLOQUEADO POR APROBACIÓN** | `cierre_preproduccion/03_CONSENTIMIENTOS_Y_CONTRATOS.md` | Comité |

## B. Cierre semanal y medición

| # | Pendiente | Clasificación | Qué falta | Quién |
|---|---|---|---|---|
| B-1 | Detección y alerta de cierres de más de 24 h, también semana medida sin plan nuevo | **RESUELTO** (integrado: #98) | — | — |
| B-2 | Qué hacer con una semana `FUERA_DE_VENTANA`: no medida, medir tarde o publicar tarde | **BLOQUEADO POR APROBACIÓN** | Decisión metodológica (`04` §3). Mientras tanto: «no medida» y acta | Equipo del estudio |
| B-3 | Acción manual de cierre o publicación tardía | **PENDIENTE** | Solo si B-2 elige medir o publicar tarde | Backend |
| B-4 | Historial de recálculos (P1-09) | **BLOQUEADO POR APROBACIÓN** (#99, borrador; V67 **no** está en `generation`) | Validación metodológica (`06` §4 y §5) | Equipo del estudio |

## C. Correo

| # | Pendiente | Clasificación | Qué falta | Quién |
|---|---|---|---|---|
| C-1 | Flujo de recuperación y texto del correo | **RESUELTO** (integrado: #97) | — | — |
| C-1b | Solo vale el código de recuperación más reciente (hallazgo de Mobile) | **PREPARADO SIN ACTIVAR** ([#102](https://github.com/Fitsense-Real/Backend/pull/102)) | Revisión y merge | Backend |
| C-2 | Contratar el proveedor (Resend recomendado), verificar el dominio y publicar SPF, DKIM y DMARC | **BLOQUEADO POR APROBACIÓN** (P-10) | Autorización, titular de la cuenta y dominio | Equipo |
| C-3 | Prueba de entrega real con un buzón sintético | **BLOQUEADO POR INFRAESTRUCTURA** | C-2 | Operación |
| C-4 | Cola de correo persistente (se pierde al reiniciar) | **PENDIENTE** (riesgo aceptado) | Migración, si se decide | Backend |

## D. Preproducción, copias y migraciones

| # | Pendiente | Clasificación | Qué falta | Quién |
|---|---|---|---|---|
| D-1 | Servicio y base de preproducción aislados | **BLOQUEADO POR INFRAESTRUCTURA** | Autorización de despliegue; crearlos (`05` §1) | Operación |
| D-2 | V66 en un entorno persistente | **PREPARADO SIN ACTIVAR** | Ensayado sobre una copia sintética, con fallo y recuperación (`05` §2). Falta aplicarla en preproducción, tras una copia verificada | Operación |
| D-3 | Copias: cifrado, retención, acceso y eliminación | **BLOQUEADO POR APROBACIÓN** (P-14) | Aprobar la propuesta de `05` §3 y el almacén | Equipo |
| D-4 | `VALIDATE CONSTRAINT ck_pgo_lease` (P2-04) | **PENDIENTE** | Ejecutarlo en preproducción | Operación |
| D-5 | Proyecto de Firebase de pruebas y push en un dispositivo real (P1-13) | **BLOQUEADO POR INFRAESTRUCTURA** | Proyecto y credenciales de pruebas | Equipo |
| D-6 | Tope global de gasto de IA (P1-07) | **BLOQUEADO POR APROBACIÓN** | Presupuesto. Mientras tanto, IA apagada | Equipo |
| D-7 | Verificar el proxy de Railway y los rangos de confianza de Tomcat | **BLOQUEADO POR INFRAESTRUCTURA** | Acceso al panel | Operación |

## E. Fuera de esta orden (sin cambios)

| # | Pendiente | Clasificación |
|---|---|---|
| E-1 | *Logout* y renovación de JWT (P1-03) | **PENDIENTE** |
| E-2 | Cambio de contraseña autenticado (P1-12) | **PENDIENTE** |
| E-3 | IA real reevaluada (después de H-1 y de C1) | **BLOQUEADO POR APROBACIÓN** (gasto) |
| E-4 | Pruebas físicas con personas y validación operativa del estudio | **BLOQUEADO POR APROBACIÓN** (A-4) |

**Observaciones del contrato, sin cambio en esta orden** (`09` §3.2):
- la generación síncrona no se excluye mientras una asíncrona espera `PENDING`; la app debe usar una sola ruta;
- una operación asíncrona anterior a la retirada se devuelve con 200 y termina en `FAILED NO_GENERABLE`.

Las dos existían antes y ninguna publica un plan indebido.

**El estudio no está listo.** Faltan, como mínimo, A-3, A-4, A-6/A-7, C-2/C-3 y D-1/D-2, además de las pruebas físicas y operativas. Que el backend tenga la opción A y el contrato preparados es condición necesaria, no suficiente.
