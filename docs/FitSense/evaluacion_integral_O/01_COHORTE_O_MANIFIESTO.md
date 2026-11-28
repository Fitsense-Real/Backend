# 01 · Cohorte O01–O20: manifiesto (congelado ANTES de ejecutar)

**Generación:** `cohorte/generar_cohorte_O.py`. Es determinista: no usa azar ni consulta el backend.

**Ficheros congelados:**

| Fichero | SHA-256 |
|---|---|
| `cohorte/O_PERFILES.csv` | `9b996825a6c146f156b7bd8d585a024cb1ccc44adf2054b11f1badb5de2b2106` |
| `cohorte/O_CRITERIOS_ESPERADOS.csv` | `e636cdbb89f9115d5f9a454799d2eb06f617e2f7cc57ac8bee4f046407877e62` |
| `cohorte/generar_cohorte_O.py` | `2d39ebd9ab63b85a447e8453ea26fb5854a8289caf1cd3ab724fcff90f9c3279` |

**Comprobación en la ejecución:** el arnés verifica el SHA-256 del CSV de perfiles con `-Dfitsense.evaluacion.k20.perfiles-sha256` y aborta si no coincide.

**Nota de transparencia.** La primera versión del CSV (SHA `fba6f2b5…`) describía como «ACORTAR» la intención de O02 y O16, cuyas omisiones son por falta de tiempo y sin minutos declarados.
- El ensayo con la cohorte desechable Z mostró que la regla documentada (`FaltaDeTiempoPolicy`) las conserva.
- Se corrigió solo el texto de la intención **antes de ejecutar O**.
- Ni los guiones ni los perfiles cambiaron.

## Cobertura (comprobada por el propio generador)

| Eje | Cobertura |
|---|---|
| Objetivo | 5 GAIN_MUSCLE, 5 LOSE_WEIGHT, 5 INCREASE_STRENGTH, 5 GENERAL_FITNESS |
| Nivel | 7 principiantes, 7 intermedios, 6 avanzados |
| Equipamiento | 9 gimnasio, 6 casa sin material, 5 casa con mancuernas y bandas |
| Frecuencia y duración | De 1 a 7 días pedidos y de 30 a 75 min. Extremos: 1 × 30 (O10, O11) y 7 × 75 (O09). Tres recortes S-MAX: O07 (5 → 4), O08 (7 → 4) y O09 (7 → 6) |
| Restricciones | Impacto (O03, O08); hombro: por encima de la cabeza y colgarse (O12); rodilla: arrodillarse, más 1205/1263 bloqueados (O13); rodilla (O19) |
| Edad | 62 años (O12: sin alto impacto) y 67 años (O13: sin impacto, carga axial ni inversión) |
| Dirigidos | Tres principiantes en casa sin material (O02, O08, O13). LOSE_WEIGHT de 1–2 días (O02, O10). ESPERADO_SIN_PLAN (O11) |

## Perfiles y guion

**Pasos del guion:**
- `H`: 100 %.
- `P40` / `P60`: 40 % / 60 % de cada ejercicio, sin causa.
- `O:<motivo>`: omisión declarada con ese motivo.
- `HF` / `HD`: sesión completa salvo el último ejercicio, que se deja al 50 % con fatiga o dolor marcados.
- `PT`: parcial al 35 % por falta de tiempo, sin minutos declarados.
- `N`: sin registro.

**Bloques:**
- **G:** progresión apagada.
- **P:** progresión encendida. La orden pide activarla solo en los escenarios que la necesitan.

| ID | Objetivo | Nivel | Equipamiento | Días pedidos → esperados | Min | Nacimiento | Restricciones | Bloque | Escenario | S1 | S2 | S3–S4 | Horizonte |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| O01 | GAIN_MUSCLE | BEGINNER | gimnasio | 3 → 3 | 45 | 1998-03-14 | — | G | CUMPLIMIENTO_TOTAL | H/H/H | H/H/H | H | 4 |
| O02 | LOSE_WEIGHT | BEGINNER | casa sin material | 2 → 2 | 30 | 1991-06-20 | — | G | OMISION_POR_TIEMPO | O:LACK_OF_TIME/H | H/H | H | 4 |
| O03 | INCREASE_STRENGTH | INTERMEDIATE | gimnasio | 4 → 4 | 60 | 1985-02-11 | sin alto impacto | G | FATIGA_OMISION | H/H/O:FATIGUE/H | H×4 | H | 4 |
| O04 | GENERAL_FITNESS | ADVANCED | casa (mancuernas/bandas) | 5 → 5 | 50 | 1996-09-02 | — | G | DIFICULTAD | H/O:TOO_DIFFICULT/H/P60/H | H×5 | H | 4 |
| O05 | GAIN_MUSCLE | INTERMEDIATE | casa sin material | 4 → 4 | 40 | 2000-01-25 | — | G | DESMOTIVACION | O:LACK_OF_MOTIVATION ×2/H/P40 | H×4 | H | 4 |
| O06 | LOSE_WEIGHT | INTERMEDIATE | gimnasio | 3 → 3 | 35 | 1974-04-18 | — | G | DOLOR_EN_EJERCICIO | HD/H/H | H×3 | H | 4 |
| O07 | INCREASE_STRENGTH | BEGINNER | casa (mancuernas/bandas) | 5 → 4 | 50 | 1993-07-30 | — | G | SMAX_CUMPLIMIENTO | H×4 | H×4 | H | 4 |
| O08 | GENERAL_FITNESS | BEGINNER | casa sin material | 7 → 4 | 30 | 1981-10-05 | sin alto impacto | G | SMAX_SIN_REGISTRO | H/N/H/H | H×4 | H | 4 |
| O09 | GENERAL_FITNESS | ADVANCED | gimnasio | 7 → 6 | 75 | 1997-12-08 | — | G | OMISION_TOLERADA | H/H/H/O:SCHEDULE_CHANGE/H/H | H×6 | H | 4 |
| O10 | LOSE_WEIGHT | ADVANCED | casa sin material | 1 → 1 | 30 | 1988-05-16 | — | G | PARCIAL_SIN_CAUSA | P40 | H | H | 4 |
| O11 | INCREASE_STRENGTH | ADVANCED | casa (mancuernas/bandas) | 1 → 1 | 30 | 1995-08-22 | — | G | **ESPERADO_SIN_PLAN** | — | — | — | — |
| O12 | GENERAL_FITNESS | INTERMEDIATE | gimnasio | 2 → 2 | 55 | 1964-03-10 | hombro (sin overhead ni colgarse) + 62 años | G | DOLOR_OMISION_MAYOR | H/O:PAIN_OR_DISCOMFORT | H/H | H | 4 |
| O13 | GAIN_MUSCLE | BEGINNER | casa sin material | 3 → 3 | 60 | 1959-02-17 | rodilla (sin arrodillarse; 1205 y 1263 bloqueados) + 67 años | G | PARCIAL_TOLERADO | H/P60/H | H×3 | H | 4 |
| O14 | LOSE_WEIGHT | BEGINNER | gimnasio | 4 → 4 | 40 | 2002-11-27 | — | G | FATIGA_TARDIA | H/H/HF/H | O:FATIGUE/H/H/H | H | 4 |
| O15 | GAIN_MUSCLE | INTERMEDIATE | gimnasio | 3 → 3 | 45 | 1992-04-03 | — | P | PROGRESION_ORDINARIA | H×3 | H×3 | H | 4 |
| O16 | INCREASE_STRENGTH | ADVANCED | gimnasio | 4 → 4 | 60 | 1987-01-19 | — | P | PROGRESION_NO_FORZADA | H×4 | H/H/O:LACK_OF_TIME/H | H | 4 |
| O17 | GENERAL_FITNESS | BEGINNER | casa (mancuernas/bandas) | 3 → 3 | 45 | 1999-06-11 | — | P | FATIGA_Y_RECUPERACION | H/H/O:FATIGUE | H×3 | H | 4 |
| O18 | GAIN_MUSCLE | ADVANCED | gimnasio | 3 → 3 | 45 | 1995-10-28 | — | P | REINCORPORACION | H/H/PT | H×3 | H | 26 |
| O19 | LOSE_WEIGHT | INTERMEDIATE | casa sin material | 3 → 3 | 50 | 1982-09-14 | rodilla (sin arrodillarse) | P | REINCORPORACION | H/PT/H | H×3 | H | 26 |
| O20 | INCREASE_STRENGTH | INTERMEDIATE | casa (mancuernas/bandas) | 3 → 3 | 45 | 1990-12-01 | — | P | DOLOR_Y_PROGRESION | H/H/HD | H×3 | H | 8 |

**Horizonte.**
- Todos los perfiles tienen 4 semanas medidas (S1–S4) y 4 ciclos, que publican hasta S5.
- O18, O19 y O20 continúan con semanas al 100 % hasta su horizonte, porque la reincorporación exige una progresión posterior a la retirada.
- En la batería permanente (`ProgresionRev2IT`) la reincorporación llegó en la semana 22.
- La corrida se detiene al producirse la reincorporación.

**Intención esperada de cada perfil:** columna `esperado` de `O_PERFILES.csv` y `O_CRITERIOS_ESPERADOS.csv`.

**Viabilidad prevista:** sale del barrido V46 REV2, que es evidencia previa a la cohorte. Todas las combinaciones dan VALIDO salvo O11, que da INVALIDO por V25 (precedente: N19, `FUERZA_SIN_TIEMPO`).

**El cumplimiento es SIMULADO por guion.** No es el comportamiento de personas reales y no permite afirmar ninguna mejora de adherencia.
