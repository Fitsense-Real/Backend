# Pruebas de cambios combinados

**Ejecución:** `evidencia/GUION_COMBINADOS_2S`.
- Laboratorio con la base PostgreSQL efímera, los servicios reales y la cadena completa: V8C + validador estricto V1–V25 + V32.
- La «respuesta del modelo» la escribe el guion `COMBINADOS` (`ExperimentoKora20IT.combinados`). **Sin red ni Replicate.**
- Cada caso elegible recibe, por intentos, primero lo que debe rechazarse y después lo que debe pasar.

**Ejecución fallida (conservada):** `GUION_COMBINADOS_2S_fallo_laboratorio`. Un defecto **del laboratorio** (un ternario `Short`/`short` desempaquetaba un `null` en ejercicios DURATION) cortó U13, U14, U18 y U19. Se corrigió el guion y se repitió. **No es un defecto del backend.**

## Resultados (los 12 escenarios de la orden)

| # | Escenario | Caso e intento | Propuesta | Resultado real | ¿Lo esperado? |
|---|---|---|---|---|---|
| 1 | Dos reducciones de repeticiones | U08 i3 | 323 y 487: 3×10 → 3×8 | **Aceptado**, 2520 s = mínimo | Sí |
| 2 | Series + repeticiones | U08 i1 | 1106: 3×6 → 2×6, y 323: 3×10 → 3×9 | **Rechazado V20** (2379 / 2520) | Sí |
| 3 | Dos reducciones de duración | U13 i1 | 1117 y 853: 3 series de 90 s → 80 s | **Aceptado** (−60 s) | Sí |
| 4 | Sustitución + reducción de dosis | U09 i2, S50 | 540 → 56, 775 → 72 y 1039: 3×10 → 3×9 | **Aceptado** | Sí |
| 5 | Varias sustituciones | U09 i2 (2) y U04 S2 de la semana 2 (2) | Sustitutos de la misma dosis | **Aceptado** | Sí |
| 6 | Cambios en dos sesiones señaladas | U09 i2 (S50 y S51), U14 i1 (S80 y S81) | Sustituciones y reducciones | **Aceptado**, las dos sesiones APLICADAS | Sí |
| 7 | Una válida y una inválida | U08 i2 | 323: 3×9 (válida) + 487: 3×11 (sube) | **Rechazado V8C** («sube la dosis… 3×10 → 3×11») | Sí: se rechaza la propuesta entera |
| 8 | Combinación justo en el mínimo | U08 i3, U18 i2 y U19 i2 (S110 y S111) | Varias | **Aceptado** (2520 y 1890 = mínimo) | Sí: la igualdad vale |
| 9 | Bajo el mínimo con cambios que **caben por separado** | U19 i1 | Serie −1 de cardio (150 = margen) + −10 s de movilidad (20), en cada sesión | **Rechazado V20** (1870 / 1890) | Sí |
| 10 | V20 bien, V24/V16 mal | U03 i1 (GM), U18 i1 | GM: 3×10 → 3×9; AG: 101 a 2×7 (`min_reps` 8) | **Rechazado V24** («4D exige 3×10») y **V16** («su min_reps es 8») | Sí |
| 11 | V20 bien, V8C mal | U09 i1 | 540 (HORIZONTAL_PUSH) → 55 (MOBILITY) | **Rechazado V8C** («mismo patrón») **y V16** | Sí |
| 12a | Cambia el músculo objetivo | U14 i1, S80 | 933 (pectorales) → 129 (tríceps), mismo patrón y dosis | **Aceptado** | Era lo esperado según `04` (V8C no compara el músculo) |
| 12b | Cambia la distribución de patrones | U04 i1, S20 (GM) | 380 (HP) → 56 (HP) y **389 (VERTICAL_PULL) → 72 (HORIZONTAL_PUSH)** | **Aceptado y publicado** | **Hueco demostrado** (`04` §3) |

**Resultado de la semana 1:** 7 de 8 APLICADAS y U03 SIN_EFECTO (V24 y luego omitida).

**Seguridad en las dos semanas:**
- 0 cambios en sesiones no señaladas;
- 0 subidas de dosis;
- 0 cambios de descanso;
- 0 sesiones bajo el mínimo;
- 0 violaciones de linaje.

## ¿La factibilidad de V2 sigue siendo correcta al combinar cambios?

- **La duración, sí.** El estimador es aditivo por ejercicio. Todas las combinaciones con suma ≤ margen pasaron V20 y todas con suma > margen fueron rechazadas (#1, #2, #8 y #9).
  - El #9 muestra por qué V2 dice «la SUMA de lo retirado no puede pasar del margen»: dos cambios que caben por separado no caben juntos.
- **Los pisos** (V9, V16) **y 4D (V24), sí.** V2 no ofrece ninguna opción por debajo, y lo que el guion forzó por debajo se rechazó (#10).
- **El patrón, parcialmente.**
  - V2 ofrece sustitutos del mismo patrón para **cada** ejercicio. Si se usan uno a uno, la distribución se conserva.
  - Nada en V2 ni en V8C impide la combinación #12b: usar el sustituto de un ejercicio para reemplazar a otro de distinto patrón.
- **El músculo:** V2 no lo considera (#12a). Ver `04` §4.
- **Varias sesiones:** cada sesión se valida con su propio margen. Los cambios en una no afectan a la otra (#6).

## Clasificación de las incompatibilidades (fase 5.3)

| Problema | Pertenece a | Acción |
|---|---|---|
| #12b: distribución de patrones | **Validador** (V8C por conjunto). La política funcional lo permite por diseño | Propuesta (`04` §3, opción A/B), **no aplicada**: cambiar V8C requiere tu autorización |
| #12a: músculo | **Política funcional** (V8C no exige el músculo) y explicación del modelo (V2 no lo menciona) | Propuesta (`08`), no aplicada |
| Ninguna otra | — | — |

**No se ha encontrado ningún defecto técnico del backend ni del cálculo de factibilidad que exija una corrección.**
