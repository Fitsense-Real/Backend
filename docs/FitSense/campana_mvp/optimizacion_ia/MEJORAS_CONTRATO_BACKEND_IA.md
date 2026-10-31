# Mejoras del contrato backend–IA

**Fecha:** 31/10/2026, fases B y D de la orden de optimización.

**Principio:**
1. El backend calcula las restricciones y las opciones válidas.
2. La IA elige o propone dentro de esos límites.
3. El backend valida la propuesta completa.
4. Solo entonces se publica.

**Qué no cambia:** el modelo, la arquitectura, los validadores (V1–V25 y V8C), los umbrales, el número de reintentos (3) y la fórmula de adherencia.

Cada cambio responde a causas medidas en `AUDITORIA_48_PREDICCIONES_IA.md` (la columna `correccion` de `RECHAZOS_IA.csv`).

## B1 · Dosis

| Cambio | Dónde | Problema que elimina |
|---|---|---|
| En GAIN_MUSCLE, el snapshot ya no ofrece candidatos que no puedan acreditar hipertrofia: los de duración y los que no son ESTRUCTURAL ni RESISTIDO | `PlanInputSnapshot` (la misma condición que el motor de reglas, `puedeDemostrarHipertrofia`) | 11 rechazos V24 (causa 5). V24 los rechazaba siempre |
| `MvpDosePolicy` sigue siendo la única fuente de las dosis | sin cambios | — |
| 4D v3.2 y v3.3 conservan su condición de necesidad, y el prompt incluye la duración exacta para comprobarla | `PlanPromptBuilder` (bloque DURACIÓN CALCULADA) | Las 3×13 innecesarias de U06. `series_max` no significa «pon 3» |

## B2 · Duración

| Cambio | Dónde | Problema que elimina |
|---|---|---|
| El backend fija `expected_duration_minutes` de cada sesión a partir de su contenido, con el mismo estimador de V17. La corrección queda registrada | `PlanDraftNormalizer` en la generación y `AdaptedWeekProducer` en la adaptación | 11 rechazos V17 (causa 4) |
| El prompt de generación da el mínimo de V20 en segundos y los segundos de cada clase de dosis, ya calculados (con la variante `series_max`) | `PlanPromptBuilder.duracionCalculada` | Parte de los 10 rechazos V20 (aritmética) |
| La corrección da, por sesión, su duración calculada y su mínimo, o la exención si está autorizada a acortarse | `SessionFixPromptBuilder.contratoDeLaSesion` | ídem en adaptación |

La regla 17 ya no pide a la IA que calcule los minutos: le dice que el backend los fija y que use la fórmula solo para comprobar que llega al mínimo.

**V17 sigue activo.** Con la duración calculada no puede fallar en una propuesta de la IA, pero se mantiene para el motor de reglas y como defensa.

## B3 · Operaciones por intervención

**Nota común para cualquier corrección:**
- En una corrección no se progresa: no se suben series, repeticiones, segundos, peso ni volumen.
- El descanso no se cambia nunca.
- Esto anula, para las correcciones, los principios 6, 8 y 9 de P-1.3, que pedían progresar. Era la causa de las 10 subidas de dosis rechazadas por V8C (causa 5).

**Línea DOSIS por sesión:**
- **GAIN_MUSCLE:** la dosis 4D no se reduce; solo se puede retirar un relleno. Con esto ACORTAR deja de prometer una reducción de dosis que no existe (U01 S2).
- **Los otros tres objetivos:** como mínimo 2 series, `min_reps` y 20 s, con el descanso igual. Son los límites de `ProgresionDosisPolicy`. Evita los 20 rechazos V24 en que la IA acortó recortando el descanso (causa 2).

**Por tipo de intervención:**

| Intervención | Qué recibe la IA |
|---|---|
| ACORTAR | DOSIS, DURACIÓN y FECHAS YA OCUPADAS. V8C sigue impidiendo combinar operaciones prohibidas |
| SEGURIDAD ANTE DOLOR | La misma nota: no puede subir el volumen. V8C lo rechaza igualmente |
| VARIANTE | SUSTITUTOS ADMITIDOS por ejercicio (como mucho 6): mismo patrón de movimiento, que no esté ya en la sesión ni en los días contiguos (V18), sin el ejercicio que dolió y con dificultad no mayor |
| CONSERVAR | Nada: no se envía a la IA. Cero cambios y cero llamadas, ya desde V42 (`requiereIA`) |

Las sesiones conservadas siguen sin viajar en el prompt: solo sus fechas (`noMandaLasConservadas`).

## B4 · Reprogramación

- **FECHAS LIBRES VÁLIDAS:** los días de `available_days` de la semana que no ocupa ninguna otra sesión.
- **FECHAS YA OCUPADAS:** las del resto de la semana.
- **Antes:** V12 de U07 S2, con dos sesiones el mismo lunes (causa 1).
- **Qué no cambia:** los días del perfil, porque el perfil está congelado durante el estudio.

## D · Reintentos

Siguen siendo 3. A cada motivo de rechazo se le añade una pista que apunta al dato del prompt que lo resuelve (`PistasDeReintento`).

| Motivo | Pista en la corrección | Pista en la generación |
|---|---|---|
| V12 | Usar una de las FECHAS LIBRES VÁLIDAS | Una sola sesión por fecha |
| V18 | Elegir de SUSTITUTOS ADMITIDOS | — (el mensaje ya nombra el grupo y los días) |
| V8C, subida de dosis | Dejar la dosis de antes de la flecha | ídem |
| V8C, otros | Solo SUSTITUTOS ADMITIDOS | — |
| V24 | La línea DOSIS de la sesión, con el descanso igual | La `clase_dosis` exacta |
| V20 | DURACIÓN de la sesión; si no cabe, OMITIRLA | Los segundos de DURACIÓN CALCULADA |
| V17 | — (ya no puede ocurrir) | — |

## Qué no se cambió

No se relajó ninguna validación:
- Las reglas que la IA incumplió con información suficiente (causa 3, 39 problemas: V15, V23, V25 y parte de V24 y V8C) siguen siendo responsabilidad del modelo.
- Esas propuestas se siguen rechazando.

## Pruebas

| Prueba | Qué comprueba |
|---|---|
| `PromptCapacidadesTest.ganarMusculoSinCambios` | B1 (premisa actualizada, con justificación) |
| `PlanDraftNormalizerDuracionTest` | B2 |
| `SessionFixPromptBuilderTest` (25) | B3 y B4, y que las conservadas no viajan |
| `PistasDeReintentoTest` | D, con los motivos literales de la campaña |
