# Decisiones pendientes

Cada decisión está clasificada como en `IMPLEMENTACIONES_PENDIENTES_MVP.md`: **[B]** bloqueante para el MVP, **[U]** antes de las pruebas con usuarios, **[E]** experimental, **[P]** posterior. Junto a cada una va la **recomendación**, que **no está aplicada**.

| # | Decisión | Clase | Opciones | Recomendación |
|---|---|---|---|---|
| D1 | ~~Fusionar #22, #23, #24, #25 y #26 en `generation`~~ **Hecho** (orden de integración final; etiqueta `backend-mvp-1.0`) | — | Todos / por partes | Todos en una tanda (#22 antes que #25) y etiqueta `backend-mvp-1.0`. Así se congela el contrato para Flutter |
| D2 | Aprobar A1–A6 (formulario en `ESTADO_PR17_Y_EVALUACION.md` §2) | [E] | Tal cual / con cambios | Tal cual: es tu redacción, ya registrada con la huella del paquete |
| D3 | Elegir 2 evaluadores, acordar el plazo y autorizar el envío del paquete ciego | [E] | — | Solo `revision_profesional/paquete_evaluador/` |
| D4 | Ampliar a 12 planes cortos de C2-D | [E] | Sí / no | Solo si C2-D va a usarse con participantes |
| D5 | Fusionar el PR #17 (con los indicadores apagados) | [E] | Antes / después de la evaluación | **Después**: el MVP no lo necesita |
| D6 | Proveedor de correo para la recuperación | [U] | Ninguno / proveedor transaccional | Decidirlo antes de las pruebas con usuarios. Mientras tanto, la recuperación la hace el equipo a mano |
| D7 | Recuperación del planificador semanal si se pierde un tick | [U] | Aprobar / no | Aprobar: es un cambio interno que reutiliza la idempotencia existente; no cambia ninguna regla |
| D8 | Comprobación de salud | [U] | Actuator solo con `health` / otra ruta | Actuator con `management.endpoints.web.exposure.include=health` |
| D9 | Revocación de sesiones (retirada, cambio de contraseña, cierre de sesión) | [U] | Marca «válido desde» / no | Implementarla antes de las pruebas con usuarios |
| D10 | Valor por defecto de Neon en `application.properties` | [U] | Mantener / pasarlo a variables | Pasarlo a variables de Railway después de verlas en el panel |
| D11 | `FITSENSE_PROGRESSION_ENABLED` en el entorno de pruebas | [U] | Encendida / apagada | Encendida, si se confirma que es la progresión aprobada |
| D12 | IA real (`FITSENSE_AI_ENABLED`) en el entorno de pruebas | [U] | Apagada (motor de reglas y respaldo) / encendida | Apagada hasta que autorices el consumo de Replicate |
| D13 | Medios: alojamiento y licencia (© Gym visual) | [U]/[P] | Sin imágenes / alojamiento propio / otra fuente | Sin imágenes en el MVP hasta aclarar la licencia |
| D14 | Registro con un correo existente: mensaje explícito (permite descubrir cuentas) o genérico | [P] | — | Mantenerlo (usabilidad); revisarlo si se publica la app |
| D15 | Cambio de zona horaria después del registro | [P] | Endpoint con efecto desde la semana siguiente / no | Posterior al MVP |
| D16 | Despliegue en Railway | [U] | — | Confirmar antes la rama en el panel y seguir `MIGRACIONES_Y_DESPLIEGUE.md` §2 |
| D17 | Reanudar Flutter | — | — | Después de D1: la app ya es compatible; solo tiene que aprovechar `motivoCodigo`, el 401 del login y la recuperación |
