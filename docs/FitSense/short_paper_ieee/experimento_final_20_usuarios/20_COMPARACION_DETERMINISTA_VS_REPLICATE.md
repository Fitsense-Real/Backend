# Comparación determinista frente a Replicate

**Fuentes:**
- A1 (`11_RESULTADOS_20_A1.csv`), sin IA;
- C, intento válido (`17_RESULTADOS_20_REPLICATE.csv`, `15` y `16`), con Replicate real (`gpt-5-mini`).

**Comparación justa:** el plan inicial de U01–U20 en C es **idéntico** al de A1, así que las diferencias en la adaptación se deben solo a la IA.

## 1. Generación inicial (los mismos 20 perfiles)

| | Motor de reglas (A1) | Replicate (gemelos C) |
|---|---|---|
| Planes publicados | 20/20 | 20/20 |
| Origen | `RULE_ENGINE` | `AI` en los 20 (0 respaldos) |
| Intentos | 1 | 1 en 18; 2 en 2 (rechazos de V24 y V15) |
| Validación final | Aprobada | Aprobada (los mismos validadores) |
| Tiempo por plan | < 1 s | Mediana 98 s (63–140 s) |
| Coste | 0 | ~US$ 0,028 por plan |

**No se compara la calidad clínica de los planes:** los dos pasan las mismas comprobaciones técnicas, y la orden prohíbe inferir que uno sea mejor.

## 2. Adaptación (la misma semana medida)

| ID | Decisión | A1 (sin IA) | C (Replicate) | Sesiones cambiadas en C |
|---|---|---|---|---|
| U03 | `ADAPT_LOCALIZED` S3 | Respaldo | Sin efecto (1 llamada, propuesta sin cambios) | — |
| U04 | `ADAPT_EXPANDED` S2, S3 | Respaldo | **Aplicada** (1 llamada) | S2: los 7 ejercicios sustituidos por variantes del mismo patrón y músculo, 6 de ellas de menor dificultad (misma dosis 3×10, r90) |
| U08 | `ADAPT_LOCALIZED` S3 | Respaldo | Sin efecto (2 llamadas: 1 rechazada por V20) | — |
| U09 | `ADAPT_EXPANDED` S2, S3 | Respaldo | **Aplicada** (3 llamadas: 2 rechazadas por V20) | S2: 6 ejercicios sustituidos, del mismo patrón y músculo, 2 de menor dificultad y 4 iguales (misma dosis) |
| U13 | `ADAPT_LOCALIZED` S3 | Respaldo | **Aplicada** (1) | S3: 1 ejercicio de duración 3×90 s → 2×90 s (34 → 32 min) |
| U14 | `ADAPT_EXPANDED` S2, S3 | Respaldo | **Aplicada** (1) | S2 y S3: una serie menos en un ejercicio de duración (34 → 32 min) |
| U18 | `ADAPT_LOCALIZED` S3 | Respaldo | Sin efecto (2: 1 rechazada por V20) | — |
| U19 | `ADAPT_EXPANDED` S2, S3 | Respaldo | Sin efecto (2: 1 rechazada por V20) | — |
| Resto (12) | `NO_CHANGE`, `INSUFFICIENT_DATA` y U07/U12 | Copia o conservación | **Idéntico a A1** (0 llamadas) | — |

**Las métricas y las decisiones son idénticas en A1 y C en los 20 casos.** La IA no interviene en la medición ni en la decisión, solo en la propuesta.

## 3. Qué aporta cada uno

**El motor determinista** mide, decide, protege y garantiza la continuidad:
- sin IA, publica la misma semana;
- en 12 de 20 casos la política ni siquiera consulta a la IA;
- valida todo lo que propone la IA (5 + 2 propuestas rechazadas);
- conserva las sesiones no señaladas.

**Replicate** convierte una decisión de adaptar en un cambio concreto:
- **4 de 8** adaptaciones aplicadas;
- las otras 4 propuestas válidas no cambiaron nada efectivo;
- genera planes iniciales válidos (20/20) a cambio de unos 1,5 minutos y unos 3 centavos por plan.

**Lo que no se concluye:**
- que las sustituciones de la IA sean clínicamente más adecuadas. Son coherentes con el catálogo (mismo patrón y músculo, sin subir la dificultad), pero eso es una comprobación técnica (H10 en `09`);
- ninguna mejora de adherencia en personas.
