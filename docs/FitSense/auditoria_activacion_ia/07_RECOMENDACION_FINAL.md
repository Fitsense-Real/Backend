# 07 · Recomendación final

**Pregunta:** ¿está el backend de `generation`, con la configuración activada por la PR #45, preparado para un entorno de prueba con Replicate real?

## 1. Respuesta

**Preparado técnicamente, con limitaciones, y condicionado a dos cosas:**
- **la fusión de la PR de la corrección O12**, porque sin ella hay un defecto de seguridad en la adaptación ante dolor (§2);
- **la configuración operativa** del entorno (`06` §7).

**Pendiente de validación profesional** en la calidad deportiva. **No preparado para usuarios reales.**

| Clasificación | Componente | Motivo |
|---|---|---|
| **Preparado técnicamente** | Generación inicial (PR-1.7, PLAN-REV-2.0, V1–V27); V24 sin cambios; progresión y reincorporación; comportamiento sin token | Regresión completa en verde (`05`); los cuatro objetivos los generan `ConfiguracionEvaluadaIT` y las pruebas de la cohorte; ninguna semana publicada con infracciones en la cohorte con la progresión apagada ni encendida |
| **Preparado tras fusionar la corrección** | Adaptación ante dolor con IA (V2.1 + SUST-SEG-1 + V8C) | Defecto demostrado y corregido: la IA podía subir series a través de un sustituto de seguridad (`01`) |
| **Con limitaciones** | Adaptaciones con progresión encendida; H-G9; trazabilidad de la progresión | Más SIN_EFECTO o RESPALDO por la V24 por capacidades (más restrictivo, nunca más trabajo); 3 llamadas perdidas en H-G9 (`03`); la ausencia de candidatos de progresión no se registra (`02`) |
| **Pendiente de configuración operativa** | Base de datos, token, límite de gasto | La URL de Neon fijada en el código (riesgo de migrar la base del proyecto); no hay tope de gasto en la aplicación (`06`) |
| **Pendiente de validación profesional** | Pertinencia de las sustituciones ante dolor (8/8 ejercicios de una sesión sustituidos ante un dolor sin localizar); 5 %, techo 1,10× y umbral del 100 %; dosis de principiante (H-G8) | Los validadores verifican políticas, no calidad deportiva |
| **Bloqueado** | Uso con usuarios reales | No forma parte de esta auditoría; requiere validación profesional y evaluación con usuarios |

## 2. O12: conclusión de seguridad

- **El cambio APLICADA → SIN_EFECTO/RESPALDO no es un problema de seguridad.**
  - Encender la progresión activa la V24 por capacidades en la validación de las adaptaciones. Las propuestas de la IA que reducen series quedan fuera de la dosis exacta y se rechazan.
  - En la cohorte con IA simulada, y en la reproducción, el resultado es SIN_EFECTO o RESPALDO.
  - En **todos** los casos, el backend excluye los ejercicios dolorosos (P-02) y los sustituye con SUST-SEG-1, y ninguno reaparece en las semanas siguientes.
  - Nunca se añade trabajo.
- **Defecto independiente, encontrado durante el diagnóstico y corregido:**
  - Con la progresión **apagada**, una respuesta de la IA que **sube** una serie en el ejercicio doloroso se publicaba como APLICADA: 3×18 → sustituto a 4×18.
  - V8C no autoriza dosis para un ejercicio nuevo, y el sustituto heredaba la dosis modificada por la IA.
  - La corrección rechaza la propuesta con V8C («sube la dosis a través del sustituto») sin relajar ni añadir validadores: aplica la regla existente («la IA no sube dosis») al sustituto.
  - **Si la corrección no se fusiona, se recomienda no desplegar con la IA activa.** La alternativa provisional sería `FITSENSE_AI_ENABLED=false`.

## 3. Interacción progresión–V24

- **Las autorizaciones de progresión no se pueden usar en la adaptación con IA.** V8C rechaza cualquier subida de la IA; el backend es el único que progresa; las pruebas están listadas en `02`.
- **Generación inicial:** la V24 es la misma con la progresión encendida o apagada. `PlanDraftValidator` no depende de la progresión.

## 4. Lo que **no** se ha verificado

- El comportamiento con Replicate real de esta configuración exacta, en particular la progresión en el bloque G y la corrección O12. Esta auditoría no hizo llamadas pagadas: **USD 0**.
- Las variables de ningún entorno de Railway.
- La calidad deportiva, que requiere un profesional.

## 5. Próximos pasos recomendados

1. Revisar y fusionar la PR técnica de O12.
2. Configurar el entorno de prueba según `06` §7, con una base separada, un token propio y un límite de gasto.
3. Primera ronda acotada con usuarios de prueba. Vigilar SIN_EFECTO/RESPALDO en las adaptaciones y los respaldos de generación (H-G9).
4. Decidir por separado las propuestas que exigen cambiar la política o los prompts: la comprobación previa de factibilidad o el mínimo P-01 para H-G9; H-G4 y H-G5 en el prompt; H-G8; el registro de «sin candidatos» en la progresión; y la etiqueta SIN_CAMBIOS_VALIDOS cuando el backend sustituye.
