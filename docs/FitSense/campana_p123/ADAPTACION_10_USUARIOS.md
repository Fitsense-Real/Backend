# Adaptación en los 10 usuarios

Flujo real del backend, sin atajos:

**Ejecución → Adherencia → Señales → Decisión → Intervención → Factibilidad → Generador simulado → Validación → Publicación.**

**La IA de corrección es un doble con una política fijada antes de ejecutar.** Está en `CampanaP123Cohorte02IT.iaSimulada` y no se cambió tras observar resultados:

| Intervención | Respuesta SIMULADA |
|---|---|
| ACORTAR_O_REORGANIZAR | Retira el último relleno (patrón sin slot 4C). Sin relleno, devuelve la sesión igual |
| REVISAR_CARGA_Y_RECUPERACION y VARIANTE_MAS_SENCILLA_O_CARGA | −2 repeticiones en el primer ejercicio de series y repeticiones (si no baja del mínimo de V16); si no cabe, −1 serie |
| SEGURIDAD_ANTE_DOLOR | N08: sustituye lo que dolió por un elegible del mismo patrón y tipo, con la misma dosis. N06: **respuesta que no cumple el contrato**, para probar P-03 |
| REPROGRAMAR_DIAS, INVESTIGAR_CAMBIO_MINIMO y el resto | La sesión igual |

**Llamadas simuladas** (campaña repetida tras C-2):
- 12 en las 100 semanas principales y 14 en total con la variante.
- Antes de C-2 eran 14 y 16: la diferencia son 2 rechazos redundantes de N03 S4.
- **0 llamadas a Replicate.**
- Nada de esto es una demostración de IA real.

## Decisiones de las 100 semanas

| Estado V32 | Semanas | Resultado en la API |
|---|---:|---|
| SIN_ORDEN (NO_CHANGE / INSUFFICIENT_DATA) | 91 | PROGRESION_DEL_BACKEND 49 · SIN_ADAPTACION 42 |
| APLICADA | 2 | ADAPTACION_APLICADA |
| SIN_EFECTO | 4 | SIN_CAMBIOS_VALIDOS (N03 S4 incluido, tras C-2) |
| RESPALDO | 3 | RESPALDO_PROPUESTA_IA_INVALIDA 1 · REPROGRAMACION_IMPOSIBLE 1 · REGENERACION_SEGURA_SUSTITUCION 1 |
| NO_FACTIBLE | 0 | — |

## Cada adaptación

| Usuario y semana | Qué pasó | Decisión e intervención | Resultado | Por qué |
|---|---|---|---|---|
| N01 S1 | Sesión 1 al 35 % por falta de tiempo, sin M | ADAPT_LOCALIZED · ACORTAR | SIN_EFECTO | La sesión parcial (4 ejercicios, todos de slot 4C) **no tiene relleno**. No se fuerza ninguna retirada |
| N09 S1 | Igual que N01 | ADAPT_LOCALIZED · ACORTAR | SIN_EFECTO | Igual: sin relleno en esa sesión |
| N04 S2 | Dos omisiones por cambio de horario | ADAPT_LOCALIZED · REPROGRAMAR | **REPROGRAMACION_IMPOSIBLE**, 0 llamadas | 6 días disponibles y 6 sesiones: no hay fecha libre. No se inventa un 7.º día ni se duplica |
| N08 S2 | Dolor en el ejercicio 791 (dumbbell supported squat) | ADAPT_LOCALIZED · SEGURIDAD_ANTE_DOLOR | **APLICADA** (1 llamada simulada) | Sustituido por 124 (band one arm single leg split squat), mismo patrón y dosis 3×6. P-02 lo excluye |
| N03 S3 | Dos sesiones parciales por tiempo | ADAPT_LOCALIZED · ACORTAR | **APLICADA** | Retira el relleno 1117 (cardio) de la sesión del lunes: 23 → 15 min, con la exención D8 de V20 |
| N07 S3 | Semana sin registros | INSUFFICIENT_DATA | SIN_ADAPTACION | **Sin datos no es 0 %**: no se adapta, se conserva |
| N10 S3 | Tres omisiones por falta de motivación | ADAPT_LOCALIZED · INVESTIGAR_CAMBIO_MINIMO | SIN_EFECTO | La IA simulada no cambia nada; consta |
| N03 S4 | Dos omisiones por motivación (una es la sesión acortada) | ADAPT_LOCALIZED · INVESTIGAR | **SIN_EFECTO (1 llamada)** | **Tras C-2:** la sesión acortada vuelve idéntica y hereda D8. Antes: RESPALDO tras 3 rechazos de V20, publicando la misma sesión. Ver `CIERRE_C2_D8_V8C.md` |
| N06 S4 | Dolor en el ejercicio 129 (band close-grip push-up) | ADAPT_LOCALIZED · SEGURIDAD_ANTE_DOLOR | RESPALDO → **REGENERACION_SEGURA_SUSTITUCION** (3 llamadas inválidas) | P-03, nivel 1: SUST-SEG-1 sustituye 129 por 78 (assisted triceps dip), misma dosis 3×6, mismo día y minutos |
| N07 S4 | Su única sesión al 35 % por tiempo | ADAPT_EXPANDED · ACORTAR | RESPALDO (3 llamadas) | La IA simulada retiraba el relleno de MOVILIDAD y **V25** lo rechaza (capacidad principal). Se conserva la semana |

### Lo que no adaptó y por qué (no es un fallo)

| Caso | Motivo |
|---|---|
| N02 S3 (fatiga y una sesión parcial) | Adherencia ponderada del 85,57 % (≥ 80 %): NO_CHANGE. La fatiga sí actuó en la progresión (VUELTA_ATRAS) |
| N03 S2 y N05 S2 (dificultad) | Todo completado con un ejercicio a la mitad: adherencia alta, NO_CHANGE. La dificultad consta y bloquea la progresión |
| N04 S5, N10 S2 y N10 S4 | Una falta entre 6 sesiones: adherencia ≥ 80 %, sin orden |
| N02 S5 | Una omisión entre 5 sesiones: NO_CHANGE |

**Ninguna propuesta inválida se publicó:**
- las 6 respuestas simuladas rechazadas (3 de N06, por contrato inválido a propósito, y 3 de N07, por V25) constan en `evidencia/traza_backend.log`;
- lo publicado pasó la validación estricta y la independiente.
