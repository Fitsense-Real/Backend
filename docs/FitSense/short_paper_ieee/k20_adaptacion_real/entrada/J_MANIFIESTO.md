# Cohorte J01–J16 · manifiesto (independiente, NO ejecutada)

**Autorización:** «apruebo la creación de una nueva cohorte independiente J01–J16».
**Fecha de creación:** 04/11/2026.

| Fichero | SHA-256 |
|---|---|
| `J_PERFILES_INDEPENDIENTES.csv` | `2f793c9ffa86861b5eb3bdd4bd7de3223c8c1a32794f6a8bba9bcb8cac9687c7` |
| `../herramientas/generar_casos_independientes_J.py` | `aee0ae9a16ad883695062bd92292160f7d0b36e53bc07ebd3b6133d046255999` |

Reproducción:

```
cd docs/FitSense/short_paper_ieee
python3 -I k20_adaptacion_real/herramientas/generar_casos_independientes_J.py \
  evaluacion_final_k20/entrada/03_PERFILES_U01_U20.csv salida.csv
sha256sum salida.csv
```

## Reglas de uso

1. **No se ejecuta nada sobre J**, ni D, ni D′, ni la IA simulada, ni Replicate, ni la sonda de factibilidad, hasta que esté congelada la versión definitiva: código, prompt, oráculo y criterios.
2. **Sus resultados no se usan para cambiar el sistema.** Las limitaciones que aparezcan se informan como resultados y no se eliminan.
3. **H01–H16 pasan a ser evidencia de desarrollo** (`A1` §7): H05 sirvió para encontrar el defecto de 4D v3.1.

## Regla de construcción

Es determinista y distinta de la de H en el nivel, la ubicación, los días y los minutos. Detalle en el docstring del generador.

| Eje | J | H |
|---|---|---|
| Objetivo | `GOALS[i % 4]` | igual |
| Causa | `CAUSAS[i // 4]`: fatiga, demasiado difícil, desmotivación, difícil + desmotivación | igual |
| Nivel | `(g + k + 1) % 3` | `(g + 2k) % 3` |
| Ubicación | `(2g + k + 1) % 3` | `(g + k) % 3` |
| Días | `(g + k + 1) % 2` (siempre el contrario que H con el mismo g, k) | `(g + k) % 2` |
| Minutos | 36, 47, 59 o 70: puntos medios de los cuartiles de 30–75, rotados `(g + k + 1) % 4` | 30, 45, 60 o 75 |

**Cobertura:**
- cada objetivo tiene un caso de 70 minutos (61–75);
- los 16 casos usan minutos que no aparecen ni en U ni en H.

**Declaración:** la regla se fijó **después** de conocer el hallazgo C-8 (GAIN_MUSCLE de 53 a 59 min no genera con 4D v3.1/v3.4). **No se ajustó para evitarlo:**
- J05 (GAIN_MUSCLE avanzado, casa con peso corporal, 3 días, 59 min) cae en esa franja.
- Si C-8 no se aprueba antes de congelar, J05 es candidato a fallar en la generación inicial. Se informará así.

## Los 16 casos

| Id | Objetivo | Causa | Nivel | Ubicación | Días | Min |
|---|---|---|---|---|---|---|
| J01 | GAIN_MUSCLE | F | INTERMEDIATE | casa, mancuernas y bandas | 4 | 47 |
| J02 | INCREASE_STRENGTH | F | ADVANCED | gimnasio | 3 | 59 |
| J03 | LOSE_WEIGHT | F | BEGINNER | casa, peso corporal | 4 | 70 |
| J04 | GENERAL_FITNESS | F | INTERMEDIATE | casa, mancuernas y bandas | 3 | 36 |
| J05 | GAIN_MUSCLE | D | ADVANCED | casa, peso corporal | 3 | 59 |
| J06 | INCREASE_STRENGTH | D | BEGINNER | casa, mancuernas y bandas | 4 | 70 |
| J07 | LOSE_WEIGHT | D | INTERMEDIATE | gimnasio | 3 | 36 |
| J08 | GENERAL_FITNESS | D | ADVANCED | casa, peso corporal | 4 | 47 |
| J09 | GAIN_MUSCLE | M | BEGINNER | gimnasio | 4 | 70 |
| J10 | INCREASE_STRENGTH | M | INTERMEDIATE | casa, peso corporal | 3 | 36 |
| J11 | LOSE_WEIGHT | M | ADVANCED | casa, mancuernas y bandas | 4 | 47 |
| J12 | GENERAL_FITNESS | M | BEGINNER | gimnasio | 3 | 59 |
| J13 | GAIN_MUSCLE | DM | INTERMEDIATE | casa, mancuernas y bandas | 3 | 36 |
| J14 | INCREASE_STRENGTH | DM | ADVANCED | gimnasio | 4 | 47 |
| J15 | LOSE_WEIGHT | DM | BEGINNER | casa, peso corporal | 3 | 59 |
| J16 | GENERAL_FITNESS | DM | INTERMEDIATE | casa, mancuernas y bandas | 4 | 70 |

**Causas:** F = fatiga · D = demasiado difícil · M = desmotivación · DM = difícil + desmotivación.

**Secuencias:**
- localizada: H/H/O con 3 días o H/H/H/O con 4;
- ampliada: H/O/O o H/O/O/O.

**Datos personales:** los del perfil E1 del mismo objetivo, como en H. Semanas: 2026-11-02 y 2026-11-09.

## Actualización (sin cambiar el CSV ni el SHA)

- **C-8** (GAIN_MUSCLE de 53 a 59 min) está aprobada e integrada, así que J05 ya no es un fallo previsible por esa causa. **J sigue sin ejecutarse.**
- **Criterios de J aprobados** (`07`):
  - A-1: 16/16;
  - B-2: al menos el 80 % de las sesiones factibles, con un límite de Wilson ≥ 63 % y ninguna corrida por debajo del 75 %;
  - 0 incumplimientos de seguridad.
- Cualquier fallo en la evaluación final se informa; no se modifican ni la cohorte ni el sistema para ocultarlo.
