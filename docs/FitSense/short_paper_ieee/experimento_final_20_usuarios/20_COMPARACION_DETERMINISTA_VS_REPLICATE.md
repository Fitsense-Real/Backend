# Comparación determinista frente a Replicate

**La comparación no se puede hacer todavía: la etapa C no se ejecutó** (no hay `REPLICATE_API_TOKEN` en la sesión). Este documento recoge lo que la etapa A demuestra por sí sola y lo que queda pendiente de medir.

## Lo que aporta el motor determinista (demostrado en A0/A1)

- **Generación inicial:** 20/20 planes válidos para 4 objetivos, 2 ubicaciones y 2 niveles, en 1 intento y con 0 problemas de validación.
- **Medición:** Aw, Aobs y C exactos respecto al recálculo independiente en los 20 casos (error 0,00), y diferencia entre omisión declarada y ausencia de registro.
- **Decisión:** las franjas, la cobertura mínima y el alcance (localizada sobre la peor sesión; ampliada sobre todas las problemáticas). La decisión coincide con el oráculo del artículo en 18/20; las otras 2 difieren por el porcentaje real de P40.
- **Protección:**
  - nunca se publica un plan sin validar (V8C y la revisión estricta en todas las copias);
  - ninguna sesión no señalada cambia;
  - 0 violaciones;
  - con datos insuficientes se conserva el plan;
  - una causa no registrada no se adapta «a ciegas».
- **Continuidad sin IA:** en los 8 casos que piden adaptar, el sistema publica la misma semana (`RESPALDO`) en lugar de dejar al usuario sin plan.

## Lo que aportaría Replicate (pendiente de medir)

- **Cuántos casos requieren realmente la IA:** 8 de 20 en la adaptación (E3 y E4). En E1, E2, E5 y en las causas no registradas la política **no** llama a la IA.
- **Pendiente de medir:**
  - propuestas aceptadas y rechazadas;
  - reintentos y respaldos por fallo de la IA;
  - qué cambia en S2/S3 y si las sesiones no señaladas siguen intactas;
  - tiempos y coste;
  - la generación inicial con IA en los 20 gemelos.
- **Restricciones que protegerían los validadores:**
  - V8C: conservación de las sesiones no señaladas y sin subir trabajo;
  - la revisión estricta V1–V25;
  - la factibilidad previa;
  - el límite de 3 intentos con retroalimentación.

## Lo que no se puede concluir

- Que un plan sea clínicamente mejor porque lo propuso un LLM o porque pasó las validaciones técnicas.
- Ninguna mejora de adherencia en personas: son usuarios sintéticos con patrones fijados de antemano.
