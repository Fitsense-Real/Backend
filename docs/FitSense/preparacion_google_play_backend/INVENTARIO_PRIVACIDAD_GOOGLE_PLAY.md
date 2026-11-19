# Inventario de privacidad para Google Play (backend)

Esta es la información técnica para rellenar la sección **Seguridad de los datos** (*Data safety*) de Google Play Console y para redactar la política de privacidad.

**No es la declaración en sí.** Las categorías de Google y su interpretación legal las decide el equipo responsable. No se ha modificado Play Console.

Fuente: la auditoría del esquema real ([INVENTARIO_DATOS_PERSONALES](INVENTARIO_DATOS_PERSONALES.md)) y el código que envía datos fuera.

## 1. Datos recogidos

| Dato (categoría orientativa de Play) | Para qué | ¿Obligatorio? | Dónde |
|---|---|---|---|
| Correo electrónico (Información personal) | Cuenta, inicio de sesión, recuperación, eliminación web | Sí | `users` |
| Nombre y apellidos (Información personal) | Identificación en la cuenta | Nombre sí, apellidos no | `users` |
| Contraseña (solo el hash BCrypt) | Autenticación | Sí | `users` |
| Fecha de nacimiento (Información personal) | Edad mínima y edad para la prescripción | Sí, para recibir rutina | `user_profiles` |
| Sexo biológico, altura, peso actual y objetivo (Salud y forma física) | Prescripción del entrenamiento | Sexo no; altura y peso sí | `user_profiles` |
| Nivel, objetivo, días, minutos, lugar, equipamiento, limitaciones de movimiento (Salud y forma física) | Prescripción | Sí | `user_profiles` |
| Objetivo y notas de salud en texto libre (Salud y forma física) | Contexto para la prescripción | Notas: opcionales | `user_profiles` |
| Dolor o molestia comunicados y ejercicios retirados (Salud) | Seguridad: no volver a prescribir lo que dolió | Opcional (lo genera la persona al omitir) | `user_safety_exclusions`, `planned_workouts` |
| Sesiones realizadas: series, repeticiones, tiempos, esfuerzo percibido (RPE), satisfacción (Salud y forma física / actividad en la app) | Adaptar la siguiente semana; medir la adherencia | Opcional (al entrenar) | `workout_sessions`, `workout_session_exercises` |
| Adherencia y riesgo de abandono calculados (inferencias) | Adaptación; análisis del estudio | Se calculan | `weekly_user_metrics` |
| Zona horaria | Calendario semanal | Sí (por omisión America/Lima) | `users` |
| Consentimientos y versiones | Prueba de lo que se aceptó | Sí, si se participa | `users`, `user_consents` |
| Código de participante P-xxx | Seudónimo para el análisis | Si se participa | `users` |
| Dirección IP | Límite de intentos (en memoria, 15 min) y logs técnicos de Railway | Automático | Memoria del proceso; logs de la plataforma |

**No se recogen:**
- ubicación;
- contactos;
- fotos;
- identificadores publicitarios;
- información financiera.

Esta lista se limita al backend. **Los SDK que incluya la app Flutter (analítica, notificaciones) se declaran aparte** y no están en este inventario.

## 2. Datos que salen del backend

| Destinatario | Qué recibe | Cuándo | ¿Anónimo? |
|---|---|---|---|
| **Replicate** (modelo `openai/gpt-5-structured`), y a través de él el proveedor del modelo | Edad, sexo, altura, peso, peso objetivo, nivel, objetivo, **texto libre del objetivo**, **notas de salud**, equipamiento, disponibilidad y ejecución de la semana anterior (series, repeticiones, RPE, motivos de omisión, incluido el dolor) | En cada generación o adaptación con IA (si la IA está activada) | **No.** No lleva nombre, correo ni código, pero el texto libre puede identificar a la persona y contiene datos de salud. Se declara como **compartido con un tercero** que lo procesa. No hay que darlo por anónimo |
| Proveedor de correo (Resend, Brevo o SMTP, cuando se configure) | Correo electrónico y un enlace o código de un solo uso | Recuperación de contraseña y eliminación web | No |
| Proveedor de PostgreSQL (Neon por omisión) | Toda la base | Siempre (alojamiento) | No. Es un encargado del tratamiento |
| Railway | Tráfico, logs técnicos e IP | Siempre (alojamiento) | No |

**Cifrado en tránsito:**
- app y web con el backend: HTTPS (PR E);
- backend con Replicate y con el proveedor de correo: HTTPS;
- backend con PostgreSQL: `sslmode=require`.

## 3. Conservación y eliminación

| Dónde | Qué se elimina cuando la persona elimina su cuenta | Qué no |
|---|---|---|
| PostgreSQL | **Todo** (alternativa A): las 13 tablas con datos de la persona y sus dependientes | Una fila mínima en `account_deletions` (id aleatorio, origen, fechas y recuentos), sin datos personales |
| Logs | Nunca han tenido correo ni contraseña. Los de eliminación solo llevan ids | Los ids internos en logs ya emitidos, hasta que caduquen según el plan de Railway (**plazo sin verificar**) |
| Copias de la base | — | Lo copiado antes de la eliminación, hasta que caduque la copia (**plazo sin verificar**) |
| Replicate | — | Las predicciones ya enviadas, según la política de Replicate (**sin verificar**) |
| Proveedor de correo | — | Sus registros de envío, según su política |

**Mecanismos para pedir la eliminación** (los dos que exige Google Play):
- Dentro de la app: `DELETE /api/v1/users/me` (PR A).
- Desde la web, sin la app: el backend de la PR B. **Falta la página y su URL pública**, que se declara en Play Console.

**Retención propuesta.** No se fija ningún plazo nuevo.
- Mientras la cuenta exista, sus datos se conservan.
- Al eliminarla, se borran de inmediato.
- La retirada del estudio sin eliminar la cuenta conserva los datos, como hasta ahora. **Esto está pendiente de decisión.**

## 4. Lo que no se puede afirmar todavía

- **Que los datos de la IA son anónimos.** No lo son (ver el apartado 2).
- **Plazos de las copias, de los logs y de Replicate.** No se han verificado.
- **Cumplimiento de una norma concreta** (por ejemplo, la ley peruana 29733 o el RGPD) y **aprobación ética.** No se afirman; corresponden al equipo responsable.
- **La URL de la página de eliminación y la de la política de privacidad.** No existen aún.
