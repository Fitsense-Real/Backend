# Decisiones pendientes

**Ninguna se ha aplicado.** Cada una necesita tu aprobación expresa.

## 1. Respaldo y dolor

| # | Decisión | Opciones | Recomendación |
|---|---|---|---|
| **D1-a** | Fusionar el PR de D1: la aserción corregida, la prueba explícita y el laboratorio 1373 | Fusionar / pedir cambios | **Fusionar** tras tu revisión. Solo pruebas; CI y regresión en `CIERRE_FINAL.md` |
| **D2-a** | Implementar P-03 localizado como nivel intermedio, entre el nivel 1 y el nivel 2, detrás de un indicador apagado | Sí / no / más estudio | **Sí, pero después de D3-b** (la regla de trabajo). Ver `DISENO_P03_LOCALIZADO.md`: resuelve 35 de 40 frente a 30 de 40, y 36 de 40 con los dos en cadena, con mucha más continuidad |
| **D2-b** | Mantener P-03 nivel 2 como último recurso | Mantener / restringir / desactivar | **Mantener** |
| **D3-a** | El dato de 1373 «Círculos de muñeca» (`SETS_REPS`, el único complemento de movilidad por repeticiones) | Confirmar que está bien / cambiar a DURATION | **Confirmarlo con una persona del equipo**. Si se cambia, es un cambio de catálogo (migración) que necesita autorización |
| **D3-b** | La regla «ante dolor no se añade trabajo» compara repeticiones y segundos por separado. Pasar de repeticiones a duración siempre sube los segundos | Mantener / equivalencia acotada para rellenos de movilidad / permitir retirar un complemento con dolor sin sustituto | **Mantener por ahora**. Decidir antes de D2-a |
| **D3-c** | Mientras no se decida: con dolor en 1373 y sin alternativa, NO_FACTIBLE y nada publicado | Mantener | **Mantener** (es lo que pediste) |

## 2. Integración

| # | Decisión | Estado |
|---|---|---|
| I-1 | Incorporar el PR de D1 al PR #17 cuando se fusione | Pendiente de D1-a |
| I-2 | Fusionar el PR #17 | **No autorizado.** Pendiente de la evaluación profesional |
| I-3 | Railway: confirmar en su panel que no despliega desde `generation` | Sin acceso desde aquí |
| I-4 | Despliegue de `generation` | Aplicaría V44 en Neon al arrancar. Decisión aparte de activar políticas |

## 3. Evaluación profesional

Aprobar A1–A6 y las reglas de procedimiento (`DECISIONES_EVALUACION_PROFESIONAL.md`), elegir a los dos profesionales y autorizar el envío de `paquete_evaluador/`.

## 4. Flutter

| # | Decisión |
|---|---|
| F-1 | Autorizar el trabajo en Flutter y dar acceso a su repositorio |
| F-2 | Qué contrato se enciende primero: duración estimada o NOFACT-0.1. Ambos están en el PR #17 y apagados |
| F-3 | Diseñar el contrato de NO_FACTIBLE **de adaptación** (código de motivo, sesión afectada y estado de publicación). Hoy solo hay texto. Ver `ESTADO_CONTRATOS_FLUTTER.md` §3 |
| F-4 | Aprobar la lista de equipamiento unificada |

## 5. Menores (sin urgencia)

- **Texto del motivo V32:** cuando el respaldo con dolor no encuentra sustituto, el motivo de V8C dice «no se señaló para adaptarla» aunque la sesión sí estaba señalada. Afecta solo al texto; corregirlo toca `src/main`.
- **Conjunto «estrés» de `CampanaMvp8UsuariosIT`:** su perfil de 15 minutos ya no es válido con P-01. Hay que decidir por qué perfil se sustituye si se quiere volver a ejecutar.
