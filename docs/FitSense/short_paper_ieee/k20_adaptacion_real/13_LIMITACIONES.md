# 13 · Limitaciones (conocidas antes de ejecutar R/R′)

**Este documento se completará con lo que aparezca en R/R′.**

1. **U01–U20 no son independientes.** Se usaron para diagnosticar y diseñar V2/V2.1, y serán la cohorte de DEV-R. Su resultado demuestra compatibilidad y regresión, no generalización.
2. **H01–H16 están EXPUESTOS:** H05 sirvió para descubrir el defecto de 4D v3.1 (`A1` §7). Son **evidencia de desarrollo**.
   - La evaluación independiente usa **J01–J16** (`entrada/J_MANIFIESTO.md`), generada con otra regla y no ejecutada.
   - La regla de J se fijó después de conocer C-8 y no se ajustó para evitarlo.
   - Ambas cohortes son sintéticas y salen de una regla fija.
   - Solo cubren fatiga, demasiado difícil y desmotivación, con 3–4 días. No cubren dolor, falta de tiempo ni cambio de horario.
3. **Imposibilidades legítimas** (no cuentan contra la IA, pero limitan la tasa alcanzable):
   - en U, 2 de 12 sesiones (GAIN_MUSCLE con fatiga o motivación);
   - en H, 5 de 21 (GAIN_MUSCLE y márgenes V20 menores que el cambio mínimo).
4. **Generación inicial de GAIN_MUSCLE por minutos:**
   - **61–75 min:** corregido por 4D v3.4 (C-7, aprobado). H05 y R0548 generan.
   - **53–59 min:** corregido por C-8 (aprobado). La rejilla `Hipertrofia4Dv34IT` pasa de 42 fallos (registro histórico) a 312/312.
   - **Volumen semanal elevado (limitación):** con 61–75 min y 3 días, pecho y muslo llegan a 36 series semanales, frente a 27 con 60 min. No incumple ninguna regla vigente (V15 y V13 se cumplen). **Su adecuación profesional no está demostrada** y no se afirma; queda para la revisión profesional (`ADENDA_4D_v3_4.md` §4.2).
   - **J03 (cohorte independiente) no genera plan inicial** en D y D′ (LOSE_WEIGHT principiante, peso corporal, 4 días, 70 min: V20, faltan 234 s). Es la familia de los principiantes de 75 min, ya registrada antes de ejecutar J. Limita J a un máximo de 15/16 recorridos y se informa como resultado (`08` §8).
   - **Otros perfiles extremos de la matriz de 960 siguen sin plan válido** por otras causas: V15 con 1 día, principiantes de 75 min y peso corporal con 6–7 días. Están registrados sin alterar en `evidencia/v34/PERFILES_SIN_PLAN_VALIDO.csv`.
5. **Dependencia del proveedor (R y R′):**
   - 2 de 58 predicciones fallaron en Replicate (`failed` y `canceled`, sin tokens).
   - El arnés de evaluación bloqueó entonces las llamadas siguientes, también el reintento del sistema. Las dos corridas se repitieron una vez según el protocolo y se informan (`09` §6).
   - El sistema publicó respaldos seguros, con 0 incumplimientos.
6. **La sonda solo enumera modificaciones simples.** Una imposibilidad está demostrada para U03/U04 S3 (`01`). En el resto, «factible» quiere decir que hay al menos una modificación simple válida.
6. **El oráculo** no re-verifica V16, V24/4D, V9 ni la cobertura 4C. La pertinencia es una regla operativa, no un juicio profesional.
7. **Variabilidad del modelo:** con 2 corridas por cohorte, la variabilidad solo queda descrita, no estimada con precisión.
8. **La evidencia previa de V2 es de otro backend.** No se le atribuye a esta versión.
