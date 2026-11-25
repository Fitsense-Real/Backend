# H2-v2 · contrato prompt–V15–V26 (PR-2.0)

**Orden:** «ORDEN DE TRABAJO — VALIDACIÓN GPT-5 MINI Y CORRECCIÓN DEFINITIVA H2».
**Autorización:** implementar, probar y crear PR. **NO MERGE.**

**Rama:** `feature/h2-v2-contrato-v15-v26`, desde `generation` en `98276a93`, que ya incluye #82, #83 y #84.
- La PR #69 no se fusiona.
- Su evidencia no se modifica.

## Estado en una línea

**PR-2.0 está implementada, probada sin coste y desactivada por omisión** (`fitsense.ai.generacion.prompt-version=PR-1.7`). **H2 no se declara resuelta** hasta una evaluación con gpt-5-mini real, que exige un presupuesto nuevo (`05`).

## Índice

| Entregable | Documento |
|---|---|
| 1–2. `generation` con GPT-5 Mini, O20 y V65; configuración efectiva de Replicate | `01_VERIFICACION_INTEGRACION.md` |
| 3. Diagnóstico de las contradicciones de H2 (C1–C9) | `02_DIAGNOSTICO_CONTRADICCIONES.md` |
| 4. Propuesta corregida del prompt (PR-2.0) | `03_PROPUESTA_PR-2.0.md` |
| 5. Resultados de las pruebas sin coste | `04_PRUEBAS_SIN_COSTE.md` |
| 6. Diseño y presupuesto de la evaluación con IA real | `05_DISENO_EVALUACION_Y_PRESUPUESTO.md` |
| 7. Regresión y CI | §Regresión, abajo |
| 9. Limitaciones y pendientes | `06_LIMITACIONES_Y_PENDIENTES.md` |

**Evidencia** (`evidencia/`), toda sin coste:
- `prompts_y_cotas_26_casos_PR17_PR20.csv`: el modo `PROMPTS`;
- `tamano_prompts_26_perfiles.csv`;
- `revalidacion_26_salidas_reales.csv`: el modo `VALIDAR` sobre las 26 salidas ya pagadas de la evaluación anterior.

## Resultado clave

- **Diagnóstico.** En producción (`PLAN-REV-2.0`), PR-1.7 no comunica ni la regla de la mitad de V15 (`max_per_body_part` llega nulo) ni los techos de V26. Además conserva frases que contradicen a la política:
  - llenar el tiempo;
  - V4 con +15 %;
  - la serie adicional de 4D;
  - reponer siempre en el reintento.

  Todos los rechazos V15 y V26 de la evaluación real caen ahí.
- **PR-2.0.** Comunica exactamente lo que verifican V15 y V26, sin cambiar ningún validador.
  - **Factible:** la rutina del motor de reglas cumple todas las cuotas en 26 de 26 perfiles.
  - **Exacta:** una unidad más que una cuota da justo el rechazo V15 (25 casos) o V26 (26 casos).
  - **PR-1.7 queda intacto:** su prompt tiene el mismo tamaño que en la evaluación anterior en los 26 casos.

## Regresión

REGRESION_PENDIENTE
