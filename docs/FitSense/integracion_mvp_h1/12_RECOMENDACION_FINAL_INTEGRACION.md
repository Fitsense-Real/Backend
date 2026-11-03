# 12 · Recomendación final de integración y respuestas a la fase 11

## Recomendación

**Integrar, con tu autorización y en este orden:**
1. **PR A (#36)** en `generation`: H1 limpio, multiconjunto por defecto y reversión a `conjunto`.
2. **PR B (#37)**: primero cambiar su base a `generation`, luego revisar y fusionar. Son los textos verificables.
3. Cerrar #31, #34 y #35, que quedan sustituidos por A y B. Mantener #28–#30, #32 y #33 abiertos como evidencia y experimentos, o cerrarlos sin fusionar.

**No fusionar** la cadena #28–#35. **No activar** V2, H12 ni las políticas del #17.

## Respuestas (fase 11)

| # | Pregunta | Respuesta |
|---|---|---|
| 1 | SHA actual de `generation` | **`0974fb3eafc39e8b7397b4dbfe7c4405e4847440`**, igual que la referencia histórica (`01`) |
| 2 | Cambios exactos de H1 trasladados | `ConservationValidator` (de #34, con 2 comentarios corregidos), 4 líneas de `application.properties` y 5 archivos de prueba idénticos a #34 (`03`) |
| 3 | ¿Se arrastró algún componente experimental? | **No**: ni V2 (ni su propiedad), ni H12, ni laboratorios, ni evidencia de #28–#33 (`01` §4, `03`) |
| 4 | ¿H1 funciona sin el prompt V2? | **Sí**. La rama limpia no tiene V2 y pasan las 43 unitarias, las 6 IT y la regresión completa (`04`) |
| 5 | ¿Las 123 sustituciones siguen siendo válidas? | **Sí**. Revalidadas en la rama limpia: 59 de 59 sesiones reales y salida idéntica byte a byte a la de #31 (`04` §3) |
| 6 | ¿U04 queda protegido? | **Sí**. La propuesta incorrecta se rechaza, la correcta se publica y la reversión está probada, todo en CI (`04` §1) |
| 7 | ¿Dolor intacto? | **Sí**. La regla no se evalúa en la rama de dolor y pasan sus pruebas (6 + 3 + 8); REPLAY `exclusiones_dolor.csv` igual (`04` §3, `08`) |
| 8 | ¿960 perfiles iguales? | **Sí**, en A y en B (`08`, `06`) |
| 9 | ¿REPLAY equivalente? | **Sí**. Solo cambian 2 marcas de tiempo del reloj real (`08`) |
| 10 | ¿Se modificó alguna regla de duración o dosis? | **No**. V20, V24, 4D y adherencia están sin cambios (`03`, `06` §2) |
| 11 | ¿Qué problema había en los textos de dificultad? | El clasificador del #35 llamaba «del mismo nivel» a una sustitución que **sube** la dificultad, omitía la sustitución si faltaba la ficha y no contaba la retirada sobrante. Además, el informe anterior atribuyó a la persona un texto («bajamos la exigencia») que hoy no se muestra: lo que se muestra es el motivo técnico de la orden (`05`) |
| 12 | ¿Puede publicarse una sustitución de mayor dificultad? | **Sí**, en VARIANTE y ante dolor, si el sustituto es elegible: V8C no compara la dificultad y SUST-SEG-1 no la mira. No se ha observado en las 123 reales (`05` §2, `SustitucionDificultadMayorTest`) |
| 13 | ¿Qué texto recibe entonces el usuario? | «Cambiamos 1 ejercicio por otro registrado como de mayor dificultad.» y, ante TOO_DIFFICULT, «Si te sigue resultando difícil, indícalo de nuevo al registrar la sesión.» (`05` §4) |
| 14 | ¿Y si la adaptación no se aplica? | `adjustment_reason`: «En una de las sesiones no aplicamos cambios esta semana: se mantiene como estaba.» (o «no encontramos un cambio compatible…» con factibilidad previa). `message_shown` seguía diciendo «Revisaremos…», porque se escribió al decidir |
| 15 | ¿Los textos de decisión prometen resultados no ejecutados? | **Hoy no prometen una reducción, pero sí afirman «se corrige»** (motivo técnico) aunque luego no se corrija nada. Con el PR B, `message_shown` expresa intención. Los textos que sí prometían (`AdjustmentDecisionTable`) están fuera del flujo (`07`) |
| 16 | ¿Solución para P-T1? | Propuesta: decir a la IA que `change_reason`/`rationale` son traza, no se muestran, y que no llame «más sencillo» a lo que no baja la dificultad registrada. **No aplicada**: requiere medirla con IA real (`07`) |
| 17 | ¿Qué se hizo con P-T2 y P-T3? | **Implementado y probado** en el PR B: `MensajeDeIntervencion` (intención), con el `reason` técnico intacto. Tres momentos (decisión, procesamiento, resultado) en tres campos sin contradicción (`07`) |
| 18 | ¿Los textos afectan las decisiones? | **No**. Decisión, sesiones aplicadas, plan, dosis, adherencia y entrada de la IA no cambian; la REPLAY es idéntica (`06` §2) |
| 19 | ¿H12 desactivado? | **Sí**: no existe en `generation` ni en A/B |
| 20 | ¿V2 desactivado? | **Sí**: no existe en `generation` ni en A/B |
| 21 | ¿Qué pruebas fueron omitidas? | 36 IT *opt-in* (IA real, Neon o lectura, laboratorios), las mismas en A y en B, y 1 de la REPLAY (modo inspección). Ninguna valida H1 ni los textos. Listas en `evidencia/regresion_*_pruebas.txt` |
| 22 | ¿El CI pasó en el commit final? | Ver `09`. PR A: verde en `db11d98`. PR B: lo que indique `09` y su PR |
| 23 | ¿Qué PR están listos para revisión? | **#36 (A)** y **#37 (B)**, en borrador |
| 24 | Orden de integración | A → (cambiar la base de B a `generation`) → B. Después, cerrar #31, #34 y #35 |
| 25 | ¿Qué está listo para el MVP? | Generación, adherencia, decisión semanal, validación, publicación con linaje, conservación, seguridad, respaldo, progresión y reincorporación; con A y B, también H1 y los textos (`10`) |
| 26 | ¿Qué requiere revisión profesional? | H12-A (formulario de 27 respuestas, sin responder), los principiantes en dificultad mínima, las fichas de 580/775/780/862 y si deben prohibirse las sustituciones de mayor dificultad (`10` §D) |
| 27 | ¿Qué falta para la integración con Flutter? | Fusionar A y B, fijar qué campo muestra la app en cada pantalla (`messageShown` = decisión; `adjustmentReason` y `adaptation` = resultado) y una prueba de extremo a extremo con backend local y datos sintéticos (`10` §E) |
