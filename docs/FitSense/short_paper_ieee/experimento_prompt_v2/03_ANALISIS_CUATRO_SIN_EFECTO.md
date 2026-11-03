# Análisis de los cuatro SIN_EFECTO

**Clases (de la orden):**
- **F1:** existía una alternativa válida que la IA no encontró;
- **F2:** existían alternativas, pero ninguna cumplía el objetivo de la intervención;
- **F3:** no se identificó ninguna modificación factible;
- **F4:** la evidencia no basta.

**Criterio:** una alternativa cuenta como «válida» solo si cumple **todas** las reglas vigentes, sin tocar ninguna. Esto incluye:
- los permisos de la intervención (V8C);
- la dosis (V9, V16 y V24, y 4D en GAIN_MUSCLE);
- la duración (V20, con igualdad admitida).

Además, la alternativa debe producir un **cambio efectivo**: una dosis menor, una sustitución o una carga menor. Las notas, el orden o los minutos declarados no cuentan (`ConservationReport.huboAdaptacionEfectiva`).

**Grado de comprobación:**
- **Analítica:** con la réplica exacta del estimador y las reglas leídas en el código. Ver `01` y `02`.
- **Indirecta:** en la ejecución original, las propuestas rechazadas **solo** recibieron mensajes V20. V8C y el validador estricto acumulan todos los problemas antes de rechazar, así que el resto de sus cambios cumplía las demás reglas, y un subconjunto de esos cambios también las cumple.
- **Directa:** con los validadores reales sobre copias, en las pruebas de `06` (casos U03, U08, U18 y U19).

## Clasificación

| Caso | Sesión: intervención | Margen | Alternativas válidas y efectivas | Qué hizo la IA | Clase |
|---|---|---:|---|---|---|
| **U03** | S15: REVISAR_CARGA (GAIN_MUSCLE) | 30 | **Ninguna.**<br>• No puede sustituir ni retirar (intervención).<br>• No puede bajar la dosis: 4D no admite reducciones (`ProgresionDosisPolicy.admite4D` → `false` con REDUCCION), y V24 exige la dosis exacta.<br>• No puede bajar la carga: no había carga prescrita.<br>• No puede cambiar el descanso ni el día | Devolvió la sesión idéntica con notas | **F3** |
| **U08** | S45: REVISAR_CARGA (INCREASE_STRENGTH) | 36 | **Sí.** Repeticiones menos en 323 y 487 (3×10, `min_reps` 6, 9 s por repetición y serie):<br>• 3×10 → 3×9 en uno o en los dos (9–18 s);<br>• 3×10 → 3×8 en los dos (36 s → 2520 s, igual al mínimo).<br>Las otras opciones (una serie menos en los 3×6, 168 s) no caben | Su intento 1 **incluía** 323 y 487 → 3×8, junto con dos «series −1» que no cabían. Tras el V20, omitió la sesión | **F1** (alternativa pequeña: unas 12 de 132 repeticiones de la sesión) |
| **U18** | S105: REVISAR_CARGA (GENERAL_FITNESS) | 150 | **Sí, varias:**<br>• una serie menos de cardio en 1054 (3×90 s → 2×90 s = 150 s, igual que lo aplicado en U13);<br>• repeticiones menos en 6 ejercicios (6 s por repetición);<br>• −10 s en 1188;<br>• o su propia propuesta sin el cambio de 1054 (110 s) | Retiró 320 s; tras el V20, omitió | **F1** |
| **U19** | S110: VARIANTE (GENERAL_FITNESS) | 150 | **Sí:**<br>• solo las sustituciones del mismo patrón (como en U04 y U09);<br>• o su propuesta sin el cambio de 853 (110 s) | Retiró 200 s; tras el V20, omitió | **F1** |
| | S111: INVESTIGAR (GENERAL_FITNESS) | 150 | **Sí:** su propuesta sin el cambio de 1054 (110 s), u otras reducciones de hasta 150 s | Retiró 200 s; tras el V20, omitió | **F1** |

**SIN_EFECTO por sesión dentro de casos APLICADA** (no cuentan en los 4, pero la orden pide no ocultarlos):

| Caso | Sesión | Clase | Por qué |
|---|---|---|---|
| U04 | S21: INVESTIGAR (GAIN_MUSCLE) | **F3** | INVESTIGAR solo permite cambiar la dosis, y en GAIN_MUSCLE la dosis 4D no admite reducciones |
| U09 | S51: INVESTIGAR (INCREASE_STRENGTH) | **F1** | Repeticiones menos en 1284 y 117 hasta 36 s.<br>La IA solo probó «series −1» (120–168 s) y en el intento 3 la devolvió sin cambios |

## Respuesta a P2

**¿Hay sesiones imposibles de modificar con utilidad?**

**Sí, dos:** U03 S15 y U04 S21.
- La combinación «GAIN_MUSCLE + intervención que solo permite cambiar la dosis» (REVISAR_CARGA o INVESTIGAR) **no admite ningún cambio efectivo** con las reglas vigentes, porque 4D no admite reducciones y no había carga prescrita.
- No es un fallo de la IA: ninguna respuesta podía producir un cambio válido.
- **No se propone cambiar 4D:** está prohibido. Se documenta como límite del diseño.

**Hay además sesiones factibles pero con muy poco margen** (FU y GM con 30–36 s). Ahí la única modificación posible es de 1 a 4 repeticiones menos en uno o dos ejercicios. Su utilidad práctica no se evalúa aquí: no se atribuyen efectos clínicos ni conductuales.

## Resumen

| Clase | Casos (de los 4 SIN_EFECTO) | Sesiones (de las 7 sin cambio efectivo en total) |
|---|---|---|
| F1 | 3: U08, U18 y U19 | 5: U08 S45, U18 S105, U19 S110, U19 S111 y U09 S51 |
| F2 | 0 | 0 |
| F3 | 1: U03 | 2: U03 S15 y U04 S21 |
| F4 | 0 | 0 |

**Consecuencias para C2:**
- Una mejora del contexto solo puede, como mucho, convertir F1 en APLICADA. Las F3 tienen que seguir sin efecto, y C2 debe decirlo explícitamente («no hay ningún cambio factible: omite»), en lugar de esperar otra cosa.
- **Máximo teórico** de adaptaciones aplicadas por corrida: **7 de 8**, porque U03 es F3. Con 7 casos, U04 aparece como aplicado aunque su S21 sea F3.
