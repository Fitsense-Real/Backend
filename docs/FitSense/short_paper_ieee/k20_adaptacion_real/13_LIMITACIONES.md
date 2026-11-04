# 13 · Limitaciones (conocidas antes de ejecutar R/R′)

**Este documento se completará con lo que aparezca en R/R′.**

1. **U01–U20 no son independientes.** Se usaron para diagnosticar y diseñar V2/V2.1, y serán la cohorte de DEV-R. Su resultado demuestra compatibilidad y regresión, no generalización.
2. **H01–H16 son la cohorte de generalización.** También son sintéticos y salen de una regla fija.
   - Solo cubren fatiga, demasiado difícil y desmotivación, con 3–4 días.
   - No cubren dolor, falta de tiempo ni cambio de horario.
3. **Imposibilidades legítimas** (no cuentan contra la IA, pero limitan la tasa alcanzable):
   - en U, 2 de 12 sesiones (GAIN_MUSCLE con fatiga o motivación);
   - en H, 5 de 21 (GAIN_MUSCLE y márgenes V20 menores que el cambio mínimo).
4. **H05: fallo de generación conocido**, de la familia de R0548 (GM en casa con 75 min). Dará FAIL en A-1.
5. **La sonda solo enumera modificaciones simples.** Una imposibilidad está demostrada para U03/U04 S3 (`01`). En el resto, «factible» quiere decir que hay al menos una modificación simple válida.
6. **El oráculo** no re-verifica V16, V24/4D, V9 ni la cobertura 4C. La pertinencia es una regla operativa, no un juicio profesional.
7. **Variabilidad del modelo:** con 2 corridas por cohorte, la variabilidad solo queda descrita, no estimada con precisión.
8. **La evidencia previa de V2 es de otro backend.** No se le atribuye a esta versión.
