# 02 · V1 frente a V2 (y V2.1)

## Evidencia histórica (no reutilizable como resultado de la nueva versión)

| Corrida | Prompt | Backend | Aplicadas / 8 | Sesiones factibles cambiadas / 10 | Rechazos | Llamadas | Coste |
|---|---|---|---:|---:|---:|---:|---:|
| Etapa C (PR #28) | V1 | `generation` anterior a H1 | 4 | — | 5 (todos V20) | 13 | ≈ US$ 0,76 la etapa entera |
| C1 r1 / r2 | V1 | `0974fb3` (sin H1, sin límite ni retirada) | 6 / 5 | 7 / 5 | 7 / 8 | 15 / 15 | 0,22 / 0,23 |
| C2 r1 / r2 | **V2** | Ídem | **7 / 7** (el techo) | **10 / 10** | **0 / 0** | **8 / 8** | **0,12 / 0,12** |

**Qué explica la mejora de V2** (`experimento_prompt_v2/17` y `01` aquí):
1. **El margen de V20 y el coste exacto de cada cambio**, calculados con el mismo estimador que V20. Eliminan el «retirar de más», que causó todos los rechazos de V1.
2. **Una pista de reintento de V20 que se puede cumplir**: deshacer o recuperar lo retirado, en lugar de «añadir un ejercicio».
3. **«F: omítela»** cuando no hay solución (U03, U04 S3): 1 llamada en lugar de varias.

**Lo que V2 no resolvía:**
- **Pertinencia del cambio mínimo:** en C2 r2, U19 S3 cambió 8 ejercicios.
- **Sustituciones laterales o de otro músculo** en «demasiado difícil» (P4 del PR #28).
- La evidencia es exploratoria (n = 8 × 2) y de **otro backend**.

## Compatibilidad con la candidata actual (H1 + #38 + #39 + P3)

| Comprobación | Resultado |
|---|---|
| Portado | Un solo commit (`109c3a3`) aplicado sobre `eval/final-ieee-k20`. Un conflicto trivial en `application.properties`; ninguno en el código |
| Pruebas propias | V2 18/18, V1 25/25, `FactibilidadPreviaIA` 7/7 |
| **¿La información de V2 es verdadera frente a los validadores de la candidata?** | **Sí, en todo lo contrastado.** Por la sonda (`coherencia_v2.csv`):<br>• U01–U20: 12/12 veredictos de sesión, 63/63 opciones y 101/101 sustitutos con la misma dosis;<br>• reservados H01–H16: 21/21, 139/139 y 231/231.<br>**0 contradicciones**, incluidos el límite de dificultad y H1 |
| Sustitutos | V2 solo lista sustitutos de dificultad no mayor, que es lo que exige el límite de dificultad (#38) |
| Retirada (#39) | Solo actúa ante dolor, que no tiene ninguna de estas sesiones. Sin interacción |
| Por defecto | `prompt-version=v1`: el texto es idéntico byte a byte (prueba). El backend no cambia con V1 |

## V2.1 (candidata posterior; solo texto)

Añade a V2, solo en el prompt y sin ninguna regla nueva:
- un bloque general de **PERTINENCIA**;
- **G. CAMBIO MÍNIMO:** una opción [cabe] sobre un solo ejercicio. Se retira la invitación a combinar cambios en esa sesión;
- **G. MÁS FÁCIL:**
  - la dificultad registrada y el músculo objetivo de cada ejercicio;
  - sus sustitutos **de menor dificultad del mismo músculo** que admiten la misma dosis;
  - si no hay ninguno, la indicación de **bajar la dosis**, porque un lateral no lo hace más fácil;
- **G. CANSANCIO:** bajar trabajo sin sustituir.

**Pruebas:** 7 nuevas.
- Sin sus añadidos, V2.1 contiene V2 íntegro en los 6 escenarios.
- Los datos que muestra salen del catálogo elegible.
- V1 y V2 no reciben nada de V2.1.

**Es una versión nueva:** no se le atribuyen los resultados de V2 (C2). Su efecto sobre el modelo **solo se puede medir con Replicate** (`06`, `07`).

## Recomendación

**V2.1** para la evaluación definitiva, si el diagnóstico de desarrollo con Replicate lo confirma frente a V2:
- la información es la misma que la de V2, ya validada;
- añade la pertinencia que exige la orden.

Si V2.1 empeorase la tasa de cambio frente a V2 en desarrollo, se elige V2. La decisión se toma **antes** de congelar y se documenta.
