# Calidad de los planes recuperados por C2

**Fuentes:** `MATRIZ_C2_VERIFICADA.csv` (una fila por perfil y camino), `PLANES_RECUPERADOS_DETALLE.csv` (ejercicio por ejercicio de los 79 recuperados, con sus caminos C1, C2-A, C2-C, C2-E, C2-D y la referencia) y `REFERENCIA_MENOR_TIEMPO.csv`.

**Advertencia.** Que un plan sea válido no significa que sea bueno. Este documento mide contenido. **No sustituye la revisión profesional, que está pendiente** (`INSTRUMENTO_REVISION_PROFESIONAL.md`). Los indicadores del modelo E (objetivo semanal y techos) solo se usan como **comparación**, no como reglas de rechazo.

## 1. Qué se compara

| Variante | Qué es |
|---|---|
| C1 rechazado | El mejor intento del motor vigente con piso del 70 %. Solo lo rechaza V20 |
| C2-A / C2-B / C2-C (**regenerado**) | El motor vuelve a generar con un piso menor |
| C2-D (**mejor esfuerzo**) | El mismo plan de C1, aceptado porque la sesión corta está **saturada**: no admite otro ejercicio ni otra serie dentro de las reglas vigentes |
| Referencia | El mismo perfil con la mayor duración menor que M cuyo plan vigente es válido. Solo es una referencia experimental |

## 2. Hallazgo principal: regenerar con un piso menor empobrece la semana

El motor rellena **hasta el piso**. Si se le da un piso menor, deja de rellenar **todas** las sesiones, no solo la que no llegaba. C2-D conserva el plan tal como el motor lo construyó al 70 %.

| Objetivo (recuperados) | Variante | Planes | Series/semana (media) | % de M, sesión más corta | % de M, sesión más larga | Mín. series de un grupo grande | Máx. series de un grupo en una sesión |
|---|---|---:|---:|---:|---:|---:|---:|
| GAIN_MUSCLE (68) | C2-A | 64 | 70 | 57,1 | 57,9 | 6,4 | 18 (media 9,6) |
| | C2-C | 64 | 70 | 57,1 | 57,9 | 6,4 | 18 (media 9,6) |
| | **C2-D** | **68** | **75** | 56,8 | **59,5** | 6,3 | 18 (media 10,4) |
| INCREASE_STRENGTH (4) | C2-A | 4 | 120 | 55,9 | 60,0 | 4,0 | 18 |
| | C2-C | 0 | — | — | — | — | — |
| | **C2-D** | **4** | **138** | 58,5 | **71,5** | 6,0 | 21 |
| LOSE_WEIGHT (4) | C2-A | 4 | 44 | 60,1 | 62,1 | 2,0 | 9 |
| | C2-C | 0 | — | — | — | — | — |
| | **C2-D** | **4** | **50** | 60,1 | **71,7** | 3,0 | 9 |
| GENERAL_FITNESS (3) | C2-A = C2-C = C2-D | 3 | 17 | 65,7 | 65,7 | 2,0 | 6 |

**Ejemplos**
- **R0800** (INCREASE_STRENGTH intermedio, peso corporal, 6 días, 75 min). El plan vigente tiene 4 sesiones del 70–71,5 % y 2 sesiones PULL del 58,5 %.
  - C2-A regenera al 55 %: **todas** las sesiones bajan (máximo 60 %) y la semana pierde 18 series (138 → 120).
  - C2-D publica el plan vigente intacto.
- **R0436** (LOSE_WEIGHT principiante, mancuernas y bandas, 4 días, 75 min). Las sesiones de tren superior están al 71,7 % y las de tren inferior al 60,1 %.
  - C2-A al 60 % recorta también las de tren superior (62,1 %) y pierde 6 series de fuerza (52 → 46).
  - El cardio programado apenas cambia.

**Conclusión:**
- Las variantes **regeneradas** (C2-A, B y C) son peores que C2-D en todos los indicadores de contenido.
- Además alteran 35 planes respecto de C1 (C2-A: 925 idénticos de 960).
- **C2-D no altera ningún plan (960 de 960 idénticos a C1)** y solo deja de bloquear la sesión saturada.

## 3. Contenido por objetivo (C2-D)

### GAIN_MUSCLE (68 recuperados; 60 a 75 min)

**Estructura**
- 7 colocaciones por sesión (el tope de V24 a ≥ 60 min).
- Estructurales 3×10/90 s; zonas pequeñas 3×13.
- Los 4 slots cubiertos en la mayoría (42 de 68).

**Duración real a 75 min:** 2.550 s = **42,5 min (56,7 % de M)** en gimnasio y en casa con 1–3 días.
- **Es el mismo plan que el perfil generaría con 60 min** (referencia: 2.550 s, mismas series).
- Configurar 75 en lugar de 60 no añade contenido: el tope de 7 colocaciones y la dosis fija lo impiden.

**Volumen:** 21–126 series por semana según los días; mínimo de un grupo grande 6,3 de media. Dentro de los rangos de inferencia de la auditoría (6–20 por grupo y semana) salvo con 6–7 días.

**Peso corporal con 6–7 días a 75 min (R0788, R0792, R0836, R0840)**
- Las sesiones PULL quedan al **46 %** (34,5 min).
- El catálogo de tracción sin material es pequeño. El PULL usa ejercicios de «espalda» como «Inclinación pélvica», «Curl de espalda baja» o «Toque de dos dedos», cuya pertinencia como trabajo de tracción **debe valorar el profesional**.
- **Son los únicos que ninguna variante regenerada recupera.** Solo C2-D los acepta.

### INCREASE_STRENGTH (4 recuperados: R0800, R0804, R0848, R0852)

- Fuerza 3×6/150 en los principales e hipertrofia 3×10/90 como secundaria.
- **Volumen elevado:** 138 series/semana, 42 de pierna y **21 de un grupo en una sesión** (LEGS).
  - **El plan vigente válido ya tiene este patrón:** 120 planes de C1 tienen más de 10 series de un grupo en una sesión, 68 de ellos de INCREASE_STRENGTH.
  - No lo introduce C2.
  - Es un indicador para el profesional, no una regla, porque el techo por sesión solo procede de un preprint [S03].
- Patrones: falta UPPER_PULL **como fuerza** en peso corporal (los slots cubiertos son LOWER_HIP, LOWER_KNEE y UPPER_PUSH). Es un efecto del catálogo, no de C2.

### LOSE_WEIGHT (4 recuperados: R0436, R0532, R0724, R0820; principiante, casa, 75 min)

- 3 bloques de cardio de 90 s por sesión y fuerza 3×8/120.
- Las sesiones de tren inferior se quedan en 3 ejercicios de fuerza más 3 de cardio, al 60,1 %.
- Cardio programado: **unos 71 min de bloque por semana**.
- Tren inferior y superior cubiertos.

### GENERAL_FITNESS (3 recuperados: 1 día, 75 min)

- 8 colocaciones (tope) al 65–67 %.
- Las cuatro capacidades principales cubiertas.

## 4. Concentración y redundancia

| Indicador (recuperados, C2-D) | Valor |
|---|---|
| Máximo de series de un grupo en una sesión | 18 en GAIN_MUSCLE; 21 en INCREASE_STRENGTH; 9 en LOSE_WEIGHT; 6 en GENERAL_FITNESS |
| Mismo ejercicio repetido en la semana (colocaciones extra) | 3,3 en GAIN_MUSCLE; 12 en INCREASE_STRENGTH (6 días, peso corporal); 7,5 en LOSE_WEIGHT; 0 en GENERAL_FITNESS |
| V18 (días seguidos) | Se cumple en todos: lo valida el validador vigente |
| Restricciones | Se respetan: los planes con restricciones o dolor se generan con su catálogo reducido |

## 5. Duración visible

**Ejemplo:** un usuario configura 75 min y FitSense planifica 42,5 min. Es válido, pero **no se debe mostrar «75 min» como duración del entrenamiento**.

**Propuesta de contrato** (sin implementar; no cambia el perfil congelado ni Flutter):

```json
"duracion": {
  "minutos_disponibles": 75,
  "minutos_estimados": 43,
  "diferencia_minutos": 32,
  "motivo": "ESTRUCTURA_COMPLETA",
  "explicacion": "Tu rutina ya incluye todos los ejercicios y series recomendados para tu objetivo. Puedes terminar antes o anadir calentamiento y movilidad."
}
```

- `motivo` es un código cerrado: `ESTRUCTURA_COMPLETA` (C2-D), `REDUCCION_AUTORIZADA` (D8/ACORTAR) o `SIN_DIFERENCIA`.
- `minutos_estimados` es el del estimador, el mismo que valida V20.
- **En el móvil:** «Duración estimada: 43 min (tienes 75)», y la explicación debajo. Nunca «75 min» en la tarjeta del entrenamiento.
