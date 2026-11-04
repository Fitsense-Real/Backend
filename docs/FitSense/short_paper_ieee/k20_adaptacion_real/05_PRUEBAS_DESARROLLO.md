# 05 · Pruebas de desarrollo (sin Replicate)

**Rama:** `eval/final-ieee-k20-v2`.

**Todo es DESARROLLO:** nada de esto es la evaluación del artículo.

**Evidencia:** `evidencia/desarrollo/`.

## Corridas (arnés `EvaluacionK20IT`, PostgreSQL efímero, oráculo v2)

| Corrida | Cohorte | Modo | Prompt | Resultado del oráculo | Huella normalizada |
|---|---|---|---|---|---|
| DEV1_S_v2 | U01–U20 | IA simulada | V2 | (coherencia V2) | — |
| **DEV2_D_U** | U01–U20 | Determinista | — | **20/20 PASS** | `f9f67973…` = **D congelada** |
| **DEV3_S21_U** | U01–U20 | IA simulada | V2.1 | **20/20 PASS** | `c0eab17e…` = **S congelada** |
| **DEV4_D_H** | H01–H16 | Determinista | — | **15/16 PASS**, **H05 FAIL** (generación) | `6eadc8c6…` |
| **DEV5_S21_H** | H01–H16 | IA simulada | V2.1 | **15/16 PASS**, H05 FAIL | `0b0d44e6…` |
| **V34_D_U** (4D v3.4) | U01–U20 | Determinista | — | **20/20 PASS** | `f9f67973…` = **D congelada** (sin cambios) |
| **V34_D_H** (4D v3.4) | H01–H16 | Determinista | — | **16/16 PASS**, **H05 PASS** | `1ac8992f…` = la del prototipo de laboratorio |
| **V34_S21_U** (4D v3.4) | U01–U20 | IA simulada | V2.1 | **20/20 PASS** | `c0eab17e…` = **S congelada** (sin cambios) |
| **V34_S21_H** (4D v3.4) | H01–H16 | IA simulada | V2.1 | **16/16 PASS**, H05 PASS | `a577eb1a…` (cambia frente a DEV5 porque H05 ya genera) |

**Lectura:**
- En U, las huellas son **idénticas** a las de la evaluación congelada: portar V2/V2.1 no cambia nada fuera del texto del prompt.
- **Con 4D v3.4 (V34_*)**, H05 genera y pasa. U no cambia, porque sus perfiles GM son de 60 min, donde el tope sigue en 7. El fallo anterior de H05 queda registrado en la fila DEV4 y en `A1`.
- **Antes de 4D v3.4:** **H05** (GAIN_MUSCLE avanzado, casa con mancuernas y bandas, 4 días de 75 min) no obtiene un plan inicial válido: V20 falta por 492–600 s en todas las sesiones. Es la familia de R0548 y queda registrado (`04` C-7, `13`). **No se elimina el caso.**

## Coherencia de V2/V2.1 con los validadores reales

`coherencia_v2.csv`: lo que dice el bloque de factibilidad frente al veredicto del productor y los validadores sobre el mismo cambio.

| | U01–U20 | H01–H16 |
|---|---|---|
| Sesión: «hay o no hay solución» | 12/12 | 21/21 |
| Opción de dosis [cabe] = válida, [NO cabe] = rechazada | 63/63 | 139/139 |
| Sustituto «misma dosis» = válido | 101/101 | 231/231 |
| Contradicciones | **0** | **0** |

Hay 47 y 83 sustitutos sin contrastar, porque son de otra zona corporal y la sonda no los prueba.

## Factibilidad en los reservados (sonda, misma corrida)

21 sesiones para la IA (H05 no tiene plan): **16 factibles y 5 imposibles**:
- H01 S3 y H09 S3: GAIN_MUSCLE con fatiga o motivación. 4D/V24.
- H13 S3: GAIN_MUSCLE con motivación. 4D/V24.
- H04 S4: margen V20 de 6 s, y el cambio mínimo retira 9 s.
- H10 S4: margen V20 inferior a cualquier cambio.

## Pruebas unitarias e integración

| Bloque | Resultado |
|---|---|
| `SessionFixPromptV2Test` (V2) | 18/18 |
| `SessionFixPromptV21Test` (V2.1, nueva) | 7/7 |
| `SessionFixPromptBuilderTest` (V1) | 25/25 |
| Oráculo v2 (`test_oraculo_k20.py`) | 23/23, con 13 mutaciones detectadas sobre la evidencia congelada, que solo se lee |
| Regresión completa | Ver `11` |

## Lo que la IA simulada comprueba (y lo que no)

**Comprueba:**
- Los rechazos: contrato 3, V8C 5.
- Que se apliquen las modificaciones válidas.
- El texto publicado (T1).
- La distinción IA/respaldo.
- La integridad de las sesiones no señaladas.
- La continuidad.

**No mide el modelo:** las respuestas las escribe el laboratorio. **No es evidencia de efectividad de la IA.**
