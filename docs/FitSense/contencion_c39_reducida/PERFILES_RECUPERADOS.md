# Perfiles recuperados y comparación de las tres variantes

## 1. Rejilla de 120 perfiles

`ContencionC39ComparacionIT` (opt-in): 4 objetivos × 3 niveles × gimnasio/casa × 5 restricciones (libre, sin colgarse, sin brazos arriba, sin suelo, sin las tres). Motor de reglas, semana fija 2026-11-16 y mismos usuarios en el mismo orden, así que la semilla coincide.

Se ejecutó sobre el mismo catálogo V47–V50 con tres variantes de la contención (evidencia en `evidencia/c39_*.csv`, comparadas con `herramientas/comparar_variantes.py`):

| Variante | Commit | PLAN | NO_FACTIBLE | Semanas con un ejercicio pendiente de decisión que el perfil restringe |
|---|---|---|---|---|
| Pila #56–#59 sin contención | `2e7c55c` | 120 | 0 | **4**: 1306 a «sin colgarse»; 1303, 1306, 1304 y 448 a «sin las tres» |
| PR #55 original (26) | `aaf5997` | 118 | **2** | 0 |
| **Reducida (8)** | esta rama | **120** | **0** | **0** |

**Reducida frente a la pila sin contención:**
- 68 perfiles quedan idénticos.
- Los otros 52 solo pierden alguno de los 8 contenidos, y solo cuando el perfil declara su restricción: 131, 448, 400, 1303, 1304, 1306, 1301 y 1153.
- Ningún ejercicio entra y ningún desenlace cambia.
- De esos 52, 15 conservan la misma semana; en el resto, la semilla determinista se desplaza.

**Reducida frente a la original:**
- 87 perfiles recuperan ejercicios liberados (1311, 1156, 1095, 1181 y 55 en 48 perfiles cada uno; 1152, 316, 875, 1093, 1062 y 1063 en 24…).
- 33 quedan idénticos.

**La contención sigue siendo necesaria.** Sin ella, la pila corregida sirve a 4 perfiles de la rejilla un ejercicio de apoyo suspendido o de curl alto que han restringido. Con los 8, ninguno.

## 2. Los dos perfiles LOSE_WEIGHT / principiante / casa

Con la contención de 26, los dos quedaban **NO_FACTIBLE**: V25 exige cardio en cada sesión de LOSE_WEIGHT y el cardio de pie disponible estaba contenido por su texto antiguo. Con la reducida, los dos publican semana, **idéntica a la de la pila sin contención**: los 8 no afectan a este equipo ni a estas restricciones.

Comprobado también por la prueba permanente `ContencionTextoBanderaIT.r_perfilesRecuperados`. La semana está publicada, así que pasó el validador obligatorio V1–V25. No contiene ningún contenido ni ninguna bandera restringida, y cada sesión lleva cardio (`LOCOMOTION_CARDIO`).

### «Sin suelo» (equipo de casa: peso corporal, mancuerna, banda)

| Día | Ejercicios (dosis) |
|---|---|
| Lun | 1098 flexión en pared 2×8 · 115 jalón con banda 2×8 · 704 press alterno sentado con mancuerna 2×8 · **1093** 3×90 s · **1117** 3×90 s · **853** 3×90 s |
| Jue | 144 sentadilla con banda 2×8 · **1093** 3×90 s · **1117** 3×90 s |
| Vie | 1098 flexión en pared 2×8 · 1156 remo sentado con banda 2×8 · 101 press de hombros con banda 2×8 · **853** 3×90 s |
| Dom | 148 peso muerto con banda 2×8 · **1093** 3×90 s · **853** 3×90 s · **1117** 3×90 s |

Ningún ejercicio tiene bandera de suelo ni de impacto. 115, 704 y 101 llevan brazos sobre la cabeza, permitido para este perfil.

### «Sin las tres» (sin suelo, sin colgarse, sin brazos arriba)

| Día | Ejercicios (dosis) |
|---|---|
| Lun | 1098 flexión en pared 2×8 · 1156 remo sentado con banda 2×8 · 135 remo de deltoides con banda 2×8 · **853** · **1093** · **1117** (3×90 s) |
| Jue | 121 extensión de cadera con banda 2×8 · **1117** · **1093** (3×90 s) |
| Vie | 1155 press de pecho sentado con banda 2×8 · 1156 2×8 · 135 2×8 · **853** 3×90 s |
| Dom | 877 zancada 2×8 · **853** · **1093** · **1117** (3×90 s) |

Ningún ejercicio tiene bandera de suelo, colgarse, brazos arriba ni impacto.

### Dependencias que conviene revisar

Las dos semanas descansan en tres cardios de pie: 1093, 1117 y 853.
- **1093** y **1156** son ejercicios liberados de la contención: V49 reescribió su texto según el GIF.
- **853** tiene el impacto en duda (R1). Si llevara HI, saldría para todo principiante y estos perfiles tendrían que reevaluarse.

Por eso los tres encabezan la lista de revisión humana (`REVISION_HUMANA_PRIORIZADA.md`). La recuperación no relaja V25 ni ninguna bandera: se debe a que el texto ya no contradice la bandera, no a que se necesiten para la factibilidad.
