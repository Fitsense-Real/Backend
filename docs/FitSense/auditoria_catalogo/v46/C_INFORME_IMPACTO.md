# C · Informe de impacto: catálogo original, V46 y variantes contrafactuales

## 1. Versiones comparadas

| Versión | Qué es | Dónde está |
|---|---|---|
| **Original** | Catálogo de la evaluación real (`flyway 44.3`, 47 migraciones, `hash_catalogo 425fc958…`) | Reproducible en la rama nueva con `-Dspring.flyway.target=44.3`: mismo checksum de migraciones (`666694b7…`) y misma huella K20 (`a662cb4d…`) |
| **V46** | Original + traducciones objetivas de 973 y 1263 (solo `name_es`) | `V46__catalogo_traducciones_objetivas.sql`; rama aislada, sin desplegar |
| **CF1** | 775 y 973 sin dosis resistida | `contrafactuales/CF1_775_973_sin_dosis.sql`; **solo en la base efímera de la medición: no es una corrección** |
| **CF1b** | CF1 + 1261, 1308, 1259 y 1146 sin dosis (todos los «tronco que cuenta como grupo») | Igual |
| **CF2** (P15-B) | Los 13 aislados de rodilla sin cobertura del patrón ni dosis de fuerza | Igual |

**Rejilla:** barrido de 3.780 perfiles (4 objetivos × 3 niveles × 3 equipamientos × 1–7 días × 15 valores de minutos), con REV2 y con HIST.

**Línea base de la medición:** idéntica byte a byte a la evidencia del cierre (`REV2_cierre` y `HIST_cierre`).

## 2. V46 (lo implementado): sin efecto en la selección, demostrado

| Comprobación | Resultado |
|---|---|
| K20 REV2 D sobre U / N | `a662cb4d` / `11f51d44`, **iguales** al original |
| K20 HIST D sobre U | `f9f67973`, **la huella congelada** |
| K20 REV2 con IA simulada V2.1 sobre U / N | `84dfb6d2` / `da34db35`, **iguales** al original |
| Prompts de la IA | **Hashes idénticos**: 28/28 en N y 16/16 en U, incluidos N12 y N14, donde la IA adapta sesiones con 973 |
| `hash_catalogo` (id, activo, dificultad, patrón) | Igual (`425fc958…`) |
| Barridos HIST y REV2 | §6 (`D`) |

**Lo que sí cambia:** el nombre que ve la persona en 973 (976 / 560 planes) y en 1263 (299 / 477).

## 3. Variantes contrafactuales: generación inicial

**Fuente:** `evidencia/C_IMPACTO_VARIANTES.csv`.

| Métrica | Base REV2 | CF1 | CF1b | CF2 (P15-B) | Base HIST | CF1 | CF1b | CF2 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Planes válidos | 3.776 | 3.776 | 3.776 | 3.776 | 3.641 | **3.533** | **3.496** | 3.641 |
| Válidos que dejan de serlo | — | 0 | 0 | 0 | — | **110** | **146** | 0 |
| Fallos V20 | — | — | — | — | 139 | 247 | 284 | 139 |
| DOSIS_LIMITADA | 1.898 | **2.340** | **2.352** | 1.899 | 2.443 | 2.543 | 2.511 | 2.443 |
| DOSIS_OBJETIVO | 1.878 | 1.436 | 1.424 | 1.877 | 1.198 | 990 | 985 | 1.198 |
| Sin dosis de hombro: GM / IS / GF | 29 / 16 / 57 | **332 / 327 / 353** | 332 / 327 / 350 | 29 / 16 / 57 | 145 / 61 / 190 | 216 / 265 / 359 | 204 / 241 / 356 | 147 / 61 / 190 |
| Sin dosis de espalda (LW) | 60 | 347 | 347 | 60 | 172 | 385 | 385 | 172 |
| Rodilla cubierta solo por aislados (total) | 324 | 319 | 318 | **0** | 181 | 182 | 180 | **0** |
| Planes sin patrón de rodilla (LW) | 94 | 87 | 87 | 99 | 42 | 42 | 42 | 42 |
| Planes con algún aislado de rodilla | 984 | 984 | 1.015 | **176** | 1.445 | 1.398 | 1.371 | **145** |
| Perfiles con un plan distinto | — | 3.484 | 3.484 | 3.625 | — | 3.409 | 3.411 | 3.686 |

**Lectura:**

1. **775 y 973 hacen de «espalda» y «hombro» sin serlo.**
   - En REV2, quitarles la dosis no hace perder ningún plan, pero DOSIS_LIMITADA sube en 442.
   - Aparecen **unos 300 planes sin hombro por objetivo**.
2. **El problema migra:**
   - Sin 775, la espalda pasa a 1261 (+302), 1308 (+291) y 1259 (+225), tan dudosos como él.
   - Sin todos ellos (CF1b), pasa a 1066 «lower back curl» (+659), una extensión de tronco.
   - **Corregir 775 solo no resuelve la espalda.**
3. **El hombro no tiene sustituto:**
   - En casa sin material, el catálogo no tiene ningún ejercicio de hombro con dosis aparte del 973.
   - Los planes de casa sin material sin hombro pasan de 346 a 1.260 (CF1b).
   - **Es un hueco del catálogo, no un error de clasificación.**
4. **HIST depende de 775 y 973 para el piso de duración V20.**
   - Quitarles la dosis rompe 110–146 planes HIST.
   - **No se puede aplicar a HIST** sin otra decisión.
5. **P15-B no cuesta factibilidad** (0 planes perdidos en las dos políticas) y elimina los 324 / 181 planes con la rodilla cubierta solo por aislados.
   - Los sustitutos son multiarticulares de rodilla, sin alto impacto ni dificultad ≥ 3 para principiantes.
   - Pero el más usado, el **sissy squat**, tiene dificultad 1 y debe revisarse.
   - En LOSE_WEIGHT aparecen 5 planes REV2 más sin patrón de rodilla.

## 4. Variantes contrafactuales: adaptación (IA simulada V2.1 en U y N; regresión, no evaluación independiente)

**Fuentes:** `evidencia/k20_ia_simulada_variantes/`, evaluadas con los criterios del protocolo 23.

| Variante | U: A-1 / A-2 / C-1 | U: C-2 | N: A-1 / A-2 / C-1 | N: C-2 | REQUIERE_REVISION (U / N) |
|---|---|---|---|---|---|
| Original | 20 PASS / 0 / 0 | 10/10 | 19 + N19 / 0 / 0 | 11/11 | 2 / 3 |
| CF1b | 20 PASS / 0 / 0 | 11/11 | 19 + N19 / 0 / 0 | 11/11 | 1 / 3 |
| CF2 (P15-B) | 20 PASS / 0 / 0 | 10/10 | 19 + N19 / 0 / 0 | 12/12 | 2 / 2 |

**Lectura:**
- Las variantes cambian los planes (huellas distintas: U `65c66d27` y `87ba7051`; N `57c7b4c9` y `21dbedfc`).
- **No introducen fallos de seguridad ni cambios no pertinentes** en las adaptaciones.
- El evaluador sigue marcando la lista original de `21` como REQUIERE_REVISION. En las variantes, algunos de esos ejercicios ya no tienen el problema medido, así que la cifra es conservadora.

## 5. Relectura de los 20 REQUIERE_REVISION de la evaluación real (descriptiva)

**No cambia el resultado ni los denominadores del informe 25.**

| Tipo de problema del ejercicio tocado | Sesiones | ¿Afecta a la selección? |
|---|---:|---|
| Solo 853 (nombre e instrucciones; el patrón cardio coincide con el GIF) | **10** | No: la duda es qué movimiento se muestra, no cómo cuenta |
| 1262 / 1115 (dificultad 1 dudosa) | 3 | Sí, si cambia la dificultad |
| 775 / 973 (dosis por tronco) | 4 | Sí (CF1) |
| 1009 (aislado como patrón, P15) | 3 | Sí (CF2) |

**Lectura:**
- La mitad de las adaptaciones «en revisión» lo están por un problema que **no afecta a la selección**.
- Las otras 10 dependen de decisiones deportivas pendientes (`E`).

## 6. Planes que siguen teniendo limitaciones (con el catálogo actual o con V46)

| Limitación | Planes REV2 / HIST |
|---|---|
| Curl nórdico con dificultad 1, anclaje no declarado e instrucciones de otro movimiento | 650 / 1.040 (289 / 468 de principiante) |
| La dosis de espalda la aporta solo 775 | 615 / 640 |
| La dosis de hombro la aporta solo 973 | 917 / 537 |
| Rodilla cubierta solo por aislados | 324 / 181 |
| Instrucciones de 1320 que piden silla (casa sin material) | 194 REV2 |
| Ejercicios «sentado» sin asiento modelado | 78 ejercicios usados |
