# Prompt de adaptación: C1 (vigente) frente a C2 (V2 experimental)

**Versiones:**
- **C1** = `FIX-PR-1.3`, el prompt vigente, con `fitsense.ai.adaptation.prompt-version=v1` (valor por defecto);
- **C2** = `FIX-PR-2.0-EXP`, con `=v2`.

**Mismo código y misma configuración del modelo:** `gpt-5-mini`, esfuerzo `medium`, salida estructurada `FIX-1.0`, 3 intentos y `prefer-wait` 25. Solo cambia el texto del prompt.

**C2 añade un cálculo previo** (la factibilidad de `04`). Por eso **es una intervención combinada**: prompt mejorado + factibilidad precalculada. No se puede atribuir el efecto a la redacción por separado.

## 1. Análisis del prompt vigente (C1)

| Aspecto | Qué trae C1 | Diagnóstico |
|---|---|---|
| Clase de tarea | «Eres un entrenador que CORRIGE sesiones concretas… No diseñas una semana nueva» | Correcta |
| Datos | Perfil, restricciones y catálogo filtrado (`available_exercises`: 645 ejercicios en gimnasio, 212 en casa AG y 76 en casa PP) con patrón, músculo, dificultad, `min_reps` y clase de dosis | Completos. El catálogo es más del 90 % del texto en GM y FU |
| Orden | Reglas 1–16 → principios P-1.3 → nota «no se progresa» → SESIONES SEÑALADAS → datos → (reintento) | Lógico. El reintento va al final |
| Límites | Por sesión: «ahora dura X s; no puede bajar de Y s» | **Solo el total y el mínimo.** No dice cuánto se puede retirar ni cuánto retira cada cambio (`02`) |
| Catálogo y sustitutos | En VARIANTE, la lista SUSTITUTOS ADMITIDOS (mismo patrón, elegibles, hasta 6) | Bien. Pero no dice cuáles admiten la misma dosis, y un sustituto con otra dosis cambia la duración (U09 i2: −432 s) |
| Dosis | «Bajar significa menos series (mín. 2), menos repeticiones (≥ `min_reps`) o menos segundos (mín. 20). El descanso no cambia y nada sube». En GM: «la dosis 4D no se reduce ni se sube» | Correcto, pero **no cuantificado** |
| Motivos | «QUÉ PUEDES CAMBIAR AQUÍ», sacado de `TipoIntervencion.instruccion()`, la misma fuente que V8C | Correcto: 0 rechazos por permisos en la etapa C |
| Conservación | Reglas 1–2 («omítela si no la mejoras») y «no se mandan las conservadas» | Correcto, pero empuja a omitir. El esquema JSON repite «si no encuentras una corrección mejor… omítela» |
| Formato | Esquema `FIX-1.0` estricto | Sin errores de formato en la etapa C |
| Retroalimentación | La propuesta anterior, los motivos literales del validador y la pista «V20: mira DURACIÓN; si no puedes llegar al mínimo con lo que la intervención permite, OMÍTELA» | **Problema principal.** El texto de V20 («agrega un ejercicio o una serie») no se puede cumplir en una sesión que solo puede bajar. La pista lleva a omitir (U08, U18 y U19) |
| Camino sin solución | Implícito («omítela») | No distingue «no hay solución» (U03 y U04 S21) de «hay solución, pero no la encontraste» |

## 2. Qué cambia en C2 (mínimo; solo añade)

| Bloque V2 | Contenido (valores reales del backend) |
|---|---|
| Global «CÓMO USAR FACTIBILIDAD» | 6 líneas: qué son las partes A–F y que un cambio permitido que cabe es mejor que omitir |
| Por sesión, **A. Objetivo** | REVISAR_CARGA: bajar algo la carga por fatiga sin bajar del mínimo. VARIANTE: más fácil de ejecutar. INVESTIGAR: un cambio pequeño de dosis |
| **B. Alcance** | Solo esta sesión, en su fecha; sustituir solo si la intervención lo permite |
| **C. Límites** | «Dura X s; mínimo Y s; puedes retirar como MÁXIMO M s» (igualar el mínimo vale) |
| **D. Opciones** | Cada cambio unitario con los segundos que retira y [cabe] o [NO cabe]; los sustitutos que admiten la misma dosis; las cargas que se pueden bajar |
| **E. Prohibido** | Subir por encima de lo prescrito, cambiar descansos, añadir; «volver hacia la dosis prescrita, sin pasarla, NO es subir» |
| **F. Sin solución** | Si el backend no encuentra ningún cambio permitido y efectivo: «OMÍTELA: es la respuesta correcta». Si lo hay: «omite solo si ninguna opción sirve al objetivo» |
| Reintento por V20 | Sustituye la pista de V20 por: «quitaste más de lo que cabe; devuelve trabajo que TÚ quitaste… hasta que quepa en el MÁXIMO (C)». **El mensaje del validador viaja igual** |

**No cambia:**
- reglas 1–16, principios, catálogo, esquema y modelo;
- validadores (V8C, V1–V25 y V20/V24), dosis, umbrales y políticas.

**Comprobado:**
- sin sus bloques, el texto C2 es C1 byte a byte (prueba 3 de `06`);
- los prompts C1 que genera esta rama son **idénticos byte a byte** a los 8 que se enviaron en la etapa C original (`06` §3).

## 3. Tamaño en los 8 casos (intento 1)

**Fuente:** `evidencia/GUION_v1/prompts` y `evidencia/GUION_v2/prompts`. Los C1 coinciden con los de la etapa C.

| Caso | C1 (caracteres) | C2 (caracteres) | Diferencia | % | *Tokens* de entrada C1 (etapa C) |
|---|---:|---:|---:|---:|---:|
| U03 | 192.986 | 194.403 | +1.417 | +0,7 % | 54.721 |
| U04 | 194.831 | 197.595 | +2.764 | +1,4 % | 55.347 |
| U08 | 189.083 | 191.089 | +2.006 | +1,1 % | 55.873 |
| U09 | 190.805 | 194.672 | +3.867 | +2,0 % | 56.458 |
| U13 | 30.958 | 33.074 | +2.116 | +6,8 % | 8.950 |
| U14 | 32.701 | 36.766 | +4.065 | +12,4 % | 9.536 |
| U18 | 71.764 | 73.879 | +2.115 | +2,9 % | 21.270 |
| U19 | 73.777 | 77.858 | +4.081 | +5,5 % | 21.952 |

**Composición (U08, C1):**
- reglas y principios: unos 9.000 caracteres (4,8 %);
- sesiones señaladas: unos 1.300 (0,7 %);
- datos de entrada: unos 178.000 (94 %), casi todo el catálogo.

**No se recorta nada para ahorrar *tokens*** (lo pide la orden).
- El aumento de C2 es de unos 400–1.200 *tokens* por llamada.
- Con el precio público de GPT-5 mini, unos US$ 0,0001–0,0003 de entrada por llamada.

## 4. Riesgos conocidos de C2 (declarados antes de ejecutar)

1. **Puede inducir cambios mínimos** (1 repetición menos) que cumplen la regla pero tienen poca magnitud. Se medirán como «aplicadas», y su magnitud se informará aparte (`13`).
2. **Puede cambiar la tasa de omisión en las sesiones F3:** se espera que omita U03 S15 y U04 S21.
3. **Al ser una intervención combinada**, una mejora no separa el efecto del cálculo del efecto de la redacción.
4. **La salida del modelo no es determinista.** Por eso se hacen dos corridas por condición, y aun así el resultado es exploratorio.
