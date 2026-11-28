# 11 · Riesgos y decisiones pendientes

## 1. Riesgos

| # | Riesgo | Probabilidad / impacto | Mitigación |
|---|---|---|---|
| R1 | Con H1, una VARIANTE que antes se publicaba (2 añadidos de un patrón por 1 retirado) pasa a reintento o conservación | Baja: 0 de 123 reales. Impacto bajo | Es el cambio buscado. Reversión con `FITSENSE_V8C_PATTERN_RULE=conjunto`, sin código |
| R2 | **Sustituciones de mayor dificultad registrada ante TOO_DIFFICULT**: V8C no las impide, y SUST-SEG-1 tampoco mira la dificultad | Posible; 0 observadas en las 123 reales | El PR B ya no las llama «del mismo nivel» y lo dice. Prohibirlas es una decisión funcional pendiente (D3) |
| R3 | El texto de resultado depende de la dificultad del catálogo; si está mal (por ejemplo 862 o 780), el texto hereda el error | Media en casos concretos | Dice «registrada»; la revisión del catálogo (#32) lo corrige en origen |
| R4 | Cambian dos textos que la app ya muestra (`adjustmentReason`, `messageShown`) | Seguro (es el objetivo) | Solo presentación; la app no los interpreta. Coordinarlo con la integración móvil (`10` §E) |
| R5 | `MensajeDeIntervencion` distingue los dos INSUFFICIENT_DATA por el texto del motivo técnico («ninguna sesion quedo senalada») | Baja: si alguien cambia ese texto, cae en el mensaje de registro insuficiente, que también es correcto | Prueba `MensajeDeIntervencionTest.sinSenalada`, que falla si cambia el texto |
| R6 | El prompt sigue diciendo que `rationale` y `change_reason` se muestran (P-T1) | Ya existe hoy | Propuesta en `07`; requiere una medición con IA real |
| R7 | Fusionar fuera de orden (B antes que A, o #31/#34/#35 además de A/B) | Media | Orden en `12`. Cerrar #31, #34 y #35 cuando A y B estén fusionados |
| R8 | `AdjustmentDecisionTable.message()` promete resultados («bajamos la exigencia»…) | Nula hoy: la tabla no está en el flujo | Si vuelve al flujo, aplicarle el mismo criterio de intención |

## 2. Decisiones pendientes (tuyas)

1. **D1:** autorizar la fusión del PR A (#36) en `generation`.
2. **D2:** autorizar la fusión del PR B (después de A; cambiar antes su base a `generation`).
3. **D3:** ¿prohibir ante TOO_DIFFICULT las sustituciones de **mayor** dificultad registrada? Por ejemplo, una regla en V8C «añadido ≤ retirado» en VARIANTE. Hoy solo se informan. Es un cambio funcional: necesita la prueba de impacto (123 sustituciones, 960 perfiles y REPLAY).
4. **D4:** P-T1, el texto del prompt V1/V2. Requiere una corrida medida con Replicate (no autorizada aquí).
5. **D5:** H12-A, cuando estén las respuestas profesionales (formulario de 27 filas).
6. **D6:** el catálogo de 580/775/780/862, tras la revisión profesional.
7. **D7:** ¿cerrar #31, #34 y #35 cuando se fusionen A y B? Siguen abiertos como evidencia. #28–#30, #32 y #33 siguen como experimentos y documentación.

## 3. Lo que no se ha hecho (autorizaciones de la orden)

**No se ha:**
- fusionado ningún PR;
- desplegado ni tocado Railway o Neon;
- ejecutado migraciones remotas;
- activado V2, H12-A/B/C/D ni políticas del PR #17;
- tocado V20, V24, 4D, las fórmulas de adherencia, el catálogo ni los prompts V1/V2;
- llamado a Replicate;
- trabajado en Flutter;
- tocado el short paper: su CSV del catálogo solo se leyó, en una copia temporal fuera de la rama;
- reescrito ninguna evidencia ni ningún mensaje histórico.
