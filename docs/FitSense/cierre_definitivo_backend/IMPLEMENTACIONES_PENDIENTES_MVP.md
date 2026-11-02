# Implementaciones y pendientes del backend

**Clasificación:**
- **[B]** bloqueante para el MVP;
- **[U]** necesario antes de las pruebas con usuarios;
- **[E]** experimental, pendiente de evaluación;
- **[P]** mejora posterior al MVP.

## 1. Implementado en esta etapa (PR abiertos, pendientes de tu revisión; **ninguno fusionado**)

| PR | Qué resuelve | Clase |
|---|---|---|
| Fitsense-Real/Backend#23 | Login fallido 401 (antes 500); 400 homogéneos; caducidad del JWT configurable | **[B]** |
| Fitsense-Real/Backend#24 | `/plan/current` según el día local del usuario (con el servidor en UTC daba 404 el domingo por la noche en Lima) | **[B]** para un despliegue en UTC; en local, con el PC en hora de Lima, no se manifiesta |
| Fitsense-Real/Backend#26 | Perfil `local-dev` y guardián contra Neon/Railway; prueba de humo con curl | **[B]** para un entorno local reproducible y seguro |
| Fitsense-Real/Backend#22 | NO_FACTIBLE estructurado y compatible (V44_1) | [U]. La app ya funciona con el mensaje genérico; el código permite explicarlo bien |
| Fitsense-Real/Backend#25 | Recuperación de contraseña segura (V44_2) | [U]. **No operativa sin proveedor de correo** |

**Para cerrar los [B] no queda trabajo de código:** solo tu revisión y la fusión de #23, #24 y #26. #22 y #25 se recomiendan en la misma tanda, porque sus migraciones 44.1 y 44.2 deben aplicarse juntas.

## 2. Pendientes

| # | Pendiente | Clase | Requiere |
|---|---|---|---|
| 1 | Fusionar #22–#26 y etiquetar la versión congelada (`backend-mvp-1.0`) | **[B]** | Tu aprobación |
| 2 | Recuperación del planificador semanal si se pierde el tick de las 03:00 (reinicio o despliegue) | [U] | Aprobar el diseño (ver `MATRIZ_CONTRATOS_API.md` fila 10) |
| 3 | Comprobación de salud para Railway (Actuator solo con `health`, u otra ruta) | [U] | Decisión |
| 4 | Revocación de sesiones: retirada del estudio, cambio de contraseña, cierre de sesión | [U] | Decisión de diseño |
| 5 | Límite de peticiones por IP (login, registro, recuperación) | [U] | Pasarela o Railway |
| 6 | Proveedor de correo para la recuperación de contraseña | [U] | Autorización y proveedor (credenciales en Railway) |
| 7 | Quitar Neon como valor por defecto de `application.properties` (pasarlo a variables de Railway) | [U] | Ver la configuración actual de Railway |
| 8 | Decidir `FITSENSE_PROGRESSION_ENABLED` y `FITSENSE_AI_ENABLED` para las pruebas con usuarios | [U] | Decisión (la progresión está aprobada, pero apagada por defecto; la IA real necesita autorización y presupuesto) |
| 9 | Entorno de pruebas desplegado (Railway y una rama de Neon) | [U] | Autorización expresa; procedimiento en `MIGRACIONES_Y_DESPLIEGUE.md` |
| 10 | Medios de los ejercicios: alojamiento, licencia y `gifUrl`/`imageUrl` | [U] si se quieren mostrar imágenes; si no, [P] | Licencia y alojamiento |
| 11 | Evaluación profesional A1–A6 (aprobación, envío y resultados) | [E] | Tu aprobación |
| 12 | C2-D, EF-0.1, V4-C con C2-D, LW-PREF-0.2, REP-SUST-0.1, NOFACT-0.1, duración por contenido y métrica aeróbica (PR #17) | [E] | La evaluación y tu aprobación por componente |
| 13 | Ampliación a 12 planes cortos de C2-D (criterio ya fijado) | [E] | Solo si C2-D se va a usar con participantes |
| 14 | Cambio de zona horaria después del registro; rechazar desfases fijos | [P] | Regla de negocio |
| 15 | Quitar el correo del JWT; unificar el formato de errores de los filtros; filtrar la propiedad antes del bloqueo | [P] | — |
| 16 | Envío asíncrono de la recuperación (tiempos de respuesta iguales con o sin cuenta) | [P], o [U] si se activa el correo | — |
| 17 | P-03 localizado | **Pospuesto** | No implementar (decisión vigente) |

**Las reglas de entrenamiento quedan congeladas.** No hay ningún pendiente que cambie la generación, V1–V25, V8C, V24, P-01, P-02, P-03, el 1373, el catálogo, las dosis ni la dificultad.
