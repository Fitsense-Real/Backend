# 04 · Cambios candidatos

| Id | Cambio | Dónde | Estado | Afecta a reglas | Por qué |
|---|---|---|---|---|---|
| **C-1** | Prompt V2: factibilidad por sesión y pista de reintento de V20 | `SessionFixPromptBuilder`, `FactibilidadAdaptacion` | **Implementado** en `eval/final-ieee-k20-v2` tras `prompt-version=v2` (por defecto `v1`) | No | K1, K2 (`01`) |
| **C-2** | Prompt V2.1: pertinencia por causa (cambio mínimo, más fácil del mismo músculo, cansancio) | Ídem | **Implementado** tras `prompt-version=v2.1` | No | K3, K4, K5 |
| **C-3** | Oráculo v2: una sustitución que cambia el músculo objetivo es como mucho **ALT**, nunca ACC | `k20_adaptacion_real/herramientas/oraculo_k20.py` (el oráculo congelado no cambia) | **Implementado**, con 1 prueba nueva (23 en total) | No (criterio de evaluación) | K5 |
| **C-4** | Opcional: hacer **regla** de V8C que INVESTIGAR_CAMBIO_MINIMO cambie como máximo 1 ejercicio | `ConservationValidator` | **No implementado; requiere tu aprobación** | **Sí**: regla nueva | Hoy lo garantiza el prompt y lo mide el oráculo (más de 2 cambios = INC). Como regla, una propuesta así se rechazaría y se reintentaría |
| **C-5** | Opcional: permitir alguna adaptación en GM con fatiga o motivación:<br>(a) 4D admite una reducción;<br>(b) eximir de V20 a la sesión con fatiga;<br>(c) permitir sustituir con fatiga | 4D/V24, V20, `TipoIntervencion` | **No implementado; no recomendado** | **Sí**: debilita dosis o duración | Recuperaría U03 S3, U04 S3 y H01/H09/H13 S3, pero cambia la prescripción por motivos de tasa |
| **C-6** | Opcional: no llamar a la IA cuando la factibilidad V2 **demuestra** que ninguna sesión tiene solución (GM con dosis fija, sin sustitución y sin carga) | `FactibilidadPreviaIA` | **No implementado** | No (ahorro) | U03 gasta 1 llamada con V2, que la omite bien. El ahorro es ≈ 1 predicción por caso; no merece el riesgo de tocar el backend antes de medir |
| **C-7** | Generación inicial de GAIN_MUSCLE de 61 a 75 min (R0548, H05 y 60/60 perfiles de 75 min en la matriz): el tope de 7 colocaciones de 4D v3.1 no llega al piso de V20 | `MvpDosePolicy`, motor de reglas, texto de V24 | **Aprobado e integrado como 4D v3.4** (`ADENDA_4D_v3_4.md`): 7 a 60 min, 8 de 61 a 68, 9 de 69 a 75 | No: V20, V24, la dosis, los descansos y el catálogo sin cambios | El prototipo de laboratorio, ahora en la candidata (`11` §4D v3.4) |
| **C-8** | GAIN_MUSCLE de **53 a 59 min** no generaba: con el tope de 6 de 4D v3.1 el contenido es 2.220 s y el piso de V20 va de 2.226 a 2.478 s (42/42 intermedios de la rejilla) | `MvpDosePolicy.maxColocaciones` | **Aprobado e integrado** (misma derivación de 4D v3.4): 7 de 53 a 59; 30–52 y 60 sin cambios | No: V20, V24, la dosis, los descansos y el catálogo sin cambios | Rejilla 312/312, 960 y REPLAY idénticos, U01–U20 idénticos (`11` §C-8) |

**Nada de C-4 a C-6 se implementa sin tu decisión.** C-7 y C-8 están aprobados e integrados. C-1 a C-3 no cambian ninguna regla ni validador:
- sin indicador, el backend es la evaluación congelada (pruebas);
- con V2 o V2.1, solo cambia el **texto** que recibe el modelo.
