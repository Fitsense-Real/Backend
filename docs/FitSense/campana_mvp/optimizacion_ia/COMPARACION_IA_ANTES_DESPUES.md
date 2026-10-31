# Comparación con IA real: antes y después

**Fecha:** 31/10/2026, fase F de la orden de optimización.
**Casos:** solo los problemáticos de la orden: U01 S2 (acortar), U02 S3 y S5 (variante e investigar), U04 S5 (factibilidad) y U07 S2 (reprogramar).
**Qué no se repitió:**
- Las 64 semanas.
- U06: su generación no se ve afectada. El motor de reglas no cambia y las pruebas deterministas de 4D v3.2/v3.3 pasan.

## 1. Método

**Simulador:** `CampanaMvp8UsuariosIT` en **modo foco**, con `-Dfitsense.campana.foco=U01:2,U02:3,U02:5,U04:5,U07:2`.

- **Fuera de los casos no se llama a Replicate.** Cada intento repite, sin coste, la salida que dio el modelo en la campaña original en ese mismo intento (`../evidencia/llamadas.jsonl`). Esa salida pasa por el backend **actual**.
- **Si la campaña original no llegó a ese intento**, se rechaza sin llamada.
- **Recreación del estado:** con las mismas respuestas y la misma adherencia del guion, sin registrar nada a mano. Cada usuario se detiene tras su último caso.
- **Las respuestas antiguas se conservan como referencia.** No se modificó ninguna evidencia histórica.

**Comprobación de la recreación** (`evidencia_foco/0_repeticion_sin_llamadas`, con `max-llamadas=0`):
- Los cuatro usuarios llegan a cada caso con el mismo origen de plan, la misma decisión y la misma adherencia oficial que en la campaña original.
- Todos los planes iniciales salen del motor de reglas (U01, U02, U04) o de la IA (U07), como entonces.

**Dos limitaciones de la repetición, que no afectan a los casos:**
- **Los identificadores cambian entre ejecuciones**, porque usuarios y sesiones se crean en paralelo.
  - Una respuesta antigua de adaptación que nombra una sesión deja de coincidir, y se rechaza por «sesión no señalada».
  - Antes de los casos solo se repitió una adaptación, U02 S2, que en la campaña original fue SIN_EFECTO (sin cambios). El estado publicado es el mismo.
  - Por la misma razón, la semilla del motor de reglas cambia y U04 recibe 1095 donde antes tenía 1262. La invalidez independiente de U04 S3–S5 (V24/V25) ya estaba en la campaña original.
  - Es el paso de PROGRESION de S2, con una dosis autorizada que la validación independiente del simulador no conoce.
  - Es una de las 29 INVALIDA explicadas en `../SIMULACION_MVP_8_USUARIOS_8_SEMANAS.md`.
- **La fila de V32 de U02 S2 cuenta los intentos repetidos** como llamadas: «ai_calls V32 = 3, llamadas contadas = 0». Es un artefacto del simulador; no hubo llamadas.

## 2. Ejecuciones con coste

| Ejecución | Código | Llamadas reales | Latencia | Coste estimado |
|---|---|---:|---:|---:|
| `1_foco_cinco_casos` | candidato (`df68478`) | 3 | 93 s | US$ 0,027 |
| `2_foco_U02_codigo_final` | candidato + corrección de duración (ver §4) | 2 | 67 s | US$ 0,016 |
| **Total de la fase F** | | **5** | 160 s | **US$ 0,044** |

**Presupuesto:** como mucho 20 llamadas y US$ 5. Se usaron 5. El total global queda en 53 de 80.

**Cálculo del coste:** tokens × tarifa del simulador (US$ 0,25 por millón de entrada y US$ 2,00 por millón de salida). Las predicciones están completas en `evidencia_foco/*/predicciones/`.

## 3. Resultados por caso (código final)

| Caso | Antes | Después |
|---|---|---|
| **U01 S2**, ACORTAR (GAIN_MUSCLE, 22 min declarados) | 2 llamadas, 70 s y US$ 0,029. La 1.ª, V8C por combinar retirar y cambiar dosis; la 2.ª, sin cambios. → SIN_EFECTO | **0 llamadas.** Factibilidad previa: la cota es 28 min, mayor que 22 → RESPALDO. Misma semana publicada |
| **U02 S3**, VARIANTE | 3 llamadas, 184 s y US$ 0,039. Sustituciones de otro patrón (V8C, 3 veces) y V17 → RESPALDO | **1 llamada, aceptada al primer intento** → APLICADA. 1196 → 144 (variante con banda, mismo patrón), misma dosis 2×8/120 y cardio intacto |
| **U02 S5**, INVESTIGAR | 3 llamadas, 140 s y US$ 0,029. V17 y V20; subidas de dosis (V8C); sustitución no autorizada → RESPALDO | **1 llamada, aceptada al primer intento** → APLICADA. Cardio de 90 a 80 s por serie, mismos ejercicios y descansos. Reducción autorizada (≥ 20 s) y V20 cumplido |
| **U04 S5**, ACORTAR (20 min declarados) | 3 llamadas, 99 s y US$ 0,032. V9, V17 dos veces y V24 → RESPALDO | **0 llamadas.** Cota de 21 min, mayor que 20 → RESPALDO. Misma semana |
| **U07 S2**, REPROGRAMAR | 2 llamadas, 63 s y US$ 0,023. V12 (dos sesiones el lunes) y después sin cambios → SIN_EFECTO | **0 llamadas.** 6 días disponibles y 6 sesiones, sin fecha libre → RESPALDO. Misma semana |

## 4. Defecto encontrado y corregido durante la comparación

**Síntoma:** en la 1.ª ejecución, el primer intento de U02 S3 fue rechazado, entre otros motivos, por **V17**: la IA repitió los 23 min de antes para un contenido de 16.

**Causa:** la duración calculada (B2) solo se aplicaba si la IA cambiaba los minutos. Era la condición heredada de «solo minutos = SIN_EFECTO».

**Corrección (`AdaptedWeekProducer.minutosSegunContenido`):**
- Si el contenido cambia, la duración la calcula siempre el backend.
- Si el contenido no cambia, se conservan los minutos medidos, como antes.

**Prueba:** `AdaptedWeekProducerTest.minutosRepetidosConContenidoNuevo`.

**Repetición:** con el código final se repitió solo U02 (2 llamadas). Los dos casos salieron al primer intento.

**Resultado de la 1.ª ejecución, como referencia:**
- U02 S3, APLICADA en el 2.º intento. El 1.º falló por V17, V20 (faltaban 312 s) y V24 (dosis de cardio).
- U02 S5, APLICADA en el 1.er intento.
- Los otros tres casos, 0 llamadas.

## 5. Comparación agregada de los cinco casos

| | Antes | 1.ª ejecución | Código final |
|---|---:|---:|---:|
| Llamadas | 13 | 3 | **2** |
| Aceptadas al primer intento (de las que llamaron) | 0 / 5 | 1 / 2 | **2 / 2** |
| Reintentos | 8 | 1 | **0** |
| Rechazos por reglas (problemas individuales, `RECHAZOS_IA.csv`) | 47 | 3 | **0** |
| Adaptaciones aplicadas | 0 | 2 | **2** |
| Respaldos o SIN_EFECTO | 5 | 3 | 3 (las tres imposibles, sin llamada) |
| Llamadas evitadas por factibilidad | — | 7 | **7** |
| Latencia de IA | 556 s | 93 s | **67 s** |
| Coste | US$ 0,152 | US$ 0,027 | **US$ 0,016** |
| Validación independiente de las semanas publicadas en los casos | VALIDA | VALIDA | **VALIDA** |

## 6. Seguridad

- **Ninguna adaptación aplicada sube la dosis**, cambia el descanso, sustituye por otro patrón ni añade trabajo.
- **Las dos pasaron** el backend completo (V8C, V1–V25) y la revalidación independiente del simulador.
- **Ninguna validación se relajó:**
  - las propuestas se aceptan porque cumplen las mismas reglas que antes;
  - los tres casos imposibles siguen publicando la semana conservada.
- **La progresión siguió encendida solo dentro del simulador**, como en la campaña. Fuera de las pruebas aisladas sigue apagada.

## 7. Límites de la comparación

- **Es una muestra pequeña:** 2 casos con llamada, con un modelo no determinista. Muestra que los rechazos evitables desaparecen, pero no basta para estimar una tasa.
- **Las propuestas que la IA incumple teniendo la información** (causa 3 de la auditoría, 39 problemas) no se probaron aquí más allá de estos casos.
