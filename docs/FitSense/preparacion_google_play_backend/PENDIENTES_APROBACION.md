# Decisiones pendientes de aprobación

Estas decisiones son del equipo responsable del estudio y, cuando corresponda, del comité de ética o de quien asesore en lo legal. **Hasta que se tomen, el backend se queda en la opción conservadora**, indicada en la columna "Mientras tanto".

| # | Decisión | Alternativas | Mientras tanto | Dónde |
|---|---|---|---|---|
| P-1 | Qué ocurre con los datos del estudio cuando alguien **elimina su cuenta** | **A.** Borrado completo. **B.** Anonimización irreversible con los requisitos documentados (P-xxx **no** cuenta como anonimización) | **A, activa.** No se conserva nada identificable | POLITICA_ELIMINACION_PROPUESTA |
| P-2 | Qué ocurre con los datos ya recogidos cuando alguien **se retira del estudio** sin eliminar la cuenta | Conservarlos y usarlos; conservarlos sin usarlos; borrarlos | Se conservan, como hasta ahora, y no se generan más planes. La persona puede eliminar su cuenta si quiere que se borren | CONSENTIMIENTO_Y_EDAD |
| P-3 | Exportaciones para el análisis que ya existen | Excluir a quien elimine su cuenta, y desde qué fecha | No se ha tocado ninguna exportación | POLITICA §4 |
| P-4 | Conservar una prueba mínima del consentimiento después de la eliminación | No conservar nada; conservar tipo, versión y fecha sin identidad | No se conserva (se borra con la cuenta) | CONSENTIMIENTO_Y_EDAD |
| P-5 | Plazo de `account_deletions` (sin datos personales) | Indefinido; N meses | Se conserva | POLITICA |
| P-6 | Textos de condiciones, privacidad y consentimiento informado, sus versiones y la aprobación ética | — | **No hay ninguno.** Versiones vacías y exigencia apagada | CONSENTIMIENTO_Y_EDAD |
| P-7 | Activar `enforce-on-signup` y abrir la captación real de participantes | — | Apagado. No se capta a nadie | CONSENTIMIENTO_Y_EDAD |
| P-8 | Minimizar la fecha de nacimiento | M0 (actual), M1 (mes y año), M2 (año y declaración) | M0. Las dos alternativas requieren una migración coordinada con la app | CONSENTIMIENTO_Y_EDAD |
| P-9 | Qué hacer con los perfiles de menores que ya existan | Revisar con la consulta de solo lectura documentada | No se ha consultado ninguna base real | CONSENTIMIENTO_Y_EDAD |
| P-10 | Proveedor de correo, titular de la cuenta y dominio remitente | Resend (recomendado), Brevo, SMTP en Railway Pro | `transport=none`, sin contratar nada | COMPARATIVA_PROVEEDORES_CORREO |
| P-11 | Correo con enlace o con código | Enlace (requiere página o *deep link*) o código | Código (sin plantilla) | RECUPERACION_CONTRASENA |
| P-12 | URL pública de la página de eliminación y de la política de privacidad | — | No existen, no se ha inventado ninguna | SOLICITUD_ELIMINACION_WEB |
| P-13 | Datos enviados a Replicate: base legal, información a las personas, plazo de conservación en Replicate y si se reduce el texto libre (`goal_text`, `health_notes`) | Mantener y declarar; reducir; desactivar la IA | Se mantiene sin cambios (no se tocó la generación) y se declara como compartido | INVENTARIO_PRIVACIDAD_GOOGLE_PLAY |
| P-14 | Plazos de copias de la base, logs de Railway y registros del proveedor de correo | — | **Sin verificar.** Hay que consultarlos en cada consola | PREPARACION_HTTPS_RAILWAY |
| P-15 | Hallazgo S-1: la salida de la IA va a los logs | Cambiar el log en `prod` | Sin cambios (está en el código de generación) | PRUEBAS_SEGURIDAD |
| P-16 | Hallazgos S-2 (el JWT lleva el correo) y S-3 (URL de Neon en el repositorio) | Quitar los dos | Sin cambios | PRUEBAS_SEGURIDAD |
| P-17 | Orden de merge y despliegue | A → C → B → D → E, con una copia de la base antes de V60–V63 | No se ha fusionado ni desplegado nada | REGRESION_FINAL |
| P-18 | Contenido de Seguridad de los datos (*Data safety*) en Play Console | Usar el inventario como base | Play Console no se ha modificado | INVENTARIO_PRIVACIDAD_GOOGLE_PLAY |
