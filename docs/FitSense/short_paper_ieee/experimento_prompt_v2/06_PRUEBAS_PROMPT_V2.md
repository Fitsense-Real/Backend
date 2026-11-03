# Pruebas del prompt V2 (sin Replicate)

**Rama:** `experiment/adaptation-prompt-v2`. **Ninguna prueba llamó a Replicate.**

## 1. Pruebas unitarias nuevas

`SessionFixPromptV2Test`, con 18 pruebas: **18 de 18 aprobadas.**

Las sesiones reproducen las condiciones de la etapa C:
- U03 y U04: GAIN_MUSCLE, 7 × 3×10 con 90 s, 60 min;
- U08 y U09: INCREASE_STRENGTH, 4 × 3×6 con 150 s + 2 × 3×10 con 90 s;
- U18 y U19: GENERAL_FITNESS principiante, 45 min, con cardio y movilidad.

| # | Qué comprueba | Resultado |
|---|---|---|
| 1 | Con el indicador apagado (constructor por defecto o `v1`), el texto es el mismo y no aparece «FACTIBILIDAD». La versión sigue siendo `FIX-PR-1.3` | ✅ |
| 2 | Un valor desconocido (`v3`) impide arrancar. `" V2 "` se normaliza | ✅ |
| 3 | **V2 solo añade:** al quitar sus bloques, su versión y su pista de V20, queda C1 byte a byte, en 6 casos, con y sin reintento | ✅ |
| 4 | La versión viaja en el *snapshot* (`FIX-PR-2.0-EXP`) y en la etiqueta de la petición. V1 queda intacta | ✅ |
| 5 | U08: 2556 / 2520 / 36. «3×9 retira 9 s [cabe]», «2×10 retira 120 s [NO cabe]», «2×6 retira 168 s [NO cabe]» | ✅ |
| 6 | U03 (GM + REVISAR_CARGA): «OPCIONES: ninguna… 4D no admite reducciones» y «OMÍTELA… es la respuesta correcta» | ✅ |
| 7 | U04: S21 sin solución. S20 con sustituto de la misma dosis 4D y sin reducciones de dosis | ✅ |
| 8 | U09: S50 y S51 factibles, con margen de 36 | ✅ |
| 9 | U18: una serie menos de 1054 retira exactamente 150 s = el margen → [cabe] (igualar el mínimo vale) | ✅ |
| 10 | ACORTAR_O_REORGANIZAR: sin máximo, porque V20 no obliga en esa sesión | ✅ |
| 11 | **Coherencia con V20 real:** cada opción [cabe] aplicada a una copia **pasa** V20 en `PlanDraftValidator`, y cada [NO cabe] **la rechaza**. 6 casos, al menos 10 de cada clase | ✅ |
| 12 | Los segundos de cada opción y la duración y el mínimo de cada sesión son exactamente los del estimador y de `minimumSessionSeconds` | ✅ |
| 13 | Propuestas originales:<br>• U08 i1 (2184 s) e U18 i1 (1720 s) se siguen rechazando;<br>• sus subconjuntos (U08: 323 y 487 a 3×8 = 2520 s; U18 sin 1054 = 1930 s) **pasan** V20 | ✅ |
| 14 | V2 nunca ofrece bajar de 2 series, de 6 repeticiones (V9) ni de 20 s, ni toca el descanso | ✅ |
| 15 | Los sustitutos de V2 son exactamente los que V1 imprime en SUSTITUTOS ADMITIDOS | ✅ |
| 16 | Reintento por V20:<br>• V2 cambia la pista («devuelve trabajo que TÚ quitaste»);<br>• V1 conserva la suya («OMÍTELA»);<br>• el mensaje del validador viaja igual | ✅ |
| 17 | Con carga prescrita, REVISAR_CARGA puede bajarla, incluso en GM. Sin carga prescrita no se ofrece | ✅ |
| 18 | Sin sesiones para la IA, V2 no añade nada y se conserva el error de siempre | ✅ |

**Pruebas vigentes del prompt y del *snapshot*:** `SessionFixPromptBuilderTest` (25), `SessionFixInputSnapshotTest` (9), `ReplicateSchemaRoutingTest` (5) y `TipoIntervencionTextoCoherenteTest` (7). **Todas aprobadas, sin cambios.**

## 2. Integración sin red (modo GUION del laboratorio)

**Lo que es real:**
- la base PostgreSQL efímera (Testcontainers) con las 46 migraciones;
- los 20 perfiles congelados del experimento original;
- el ciclo semanal, V8C, el validador estricto y V32.

**Lo que no es real:** solo la respuesta del modelo. La escribe el laboratorio a partir de la factibilidad V2 (`04` §3), y la URL del proveedor apunta a `127.0.0.1:9`, así que no puede salir nada a la red.

| Ejecución | Prompt | Resultado |
|---|---|---|
| `evidencia/GUION_v2` | V2 | **7 de 8 APLICADA y U03 SIN_EFECTO.** 0 llamadas a Replicate.<br>Los intentos que V2 marca como inválidos se rechazaron:<br>• U03 y U04 S21 por **V24** («4D v2 exige 3×10»);<br>• U08 y U09 por **V20** (2388 / 2520).<br>Los que V2 marca como válidos se aceptaron |
| `evidencia/GUION_v1` | V1 | Mismo guion. `decisiones`, `plan_siguiente`, `plan_inicial` y `resultados_20` son **iguales** a los de V2. La única diferencia es una columna de diagnóstico (`rechazos_validador_estricto`), vacía en las 20 filas, que solo tenía la primera ejecución y se retiró del laboratorio antes de la segunda |

**Controles (12 no elegibles):** en las dos ejecuciones, 0 llamadas y el mismo resultado que en el experimento original. Por ejemplo, U07 y U12 quedan en SIN_EFECTO por causa no registrada.

## 3. V1 apagado = el prompt original, byte a byte

Los 8 prompts del intento 1 que genera esta rama con `v1` (`evidencia/GUION_v1/prompts`) son **idénticos byte a byte** a los 8 que se enviaron a Replicate en la etapa C original. Se compararon con `input.prompt` de cada predicción guardada en PR #28:
- U03: 192.986;
- U04: 194.831;
- U08: 189.083;
- U09: 190.805;
- U13: 30.958;
- U14: 32.701;
- U18: 71.764;
- U19: 73.777.

**Consecuencia:** C1 es exactamente la condición de la etapa C.

## 4. Regresión con el indicador apagado

Ver `16_REGRESION_BACKEND.md`. En el momento de escribir esto, la batería unitaria completa ya está aprobada: **1.061 pruebas, 0 fallos y 0 omitidas** (1.043 anteriores + 18 nuevas).
