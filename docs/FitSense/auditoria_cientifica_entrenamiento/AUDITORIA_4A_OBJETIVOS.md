# Auditoría de 4A: capacidades por objetivo

**Base:** `generation` (`afef98a`), rama `feature/modelo-e-planificacion`.
**Qué se audita:** la matriz 4A v6 (`GoalCapabilityPolicy`, `VERSION = "4A-v6"`; `ADENDA_4A_v6.md`).
**Evidencia:** `REVISION_CIENTIFICA_EXHAUSTIVA.md` y `ESTUDIOS_SELECCIONADOS.csv`. Las 53 fuentes se consultaron solo como **resumen o ficha**: no se leyó ningún texto completo (ver §5).
**Clases:**
- **A:** respaldo directo.
- **B:** compatible y defendible.
- **C:** decisión de ingeniería justificable.
- **D:** evidencia insuficiente.
- **E:** potencialmente problemática.

## 1. Especificaciones recuperadas y no recuperadas

| Documento | Estado |
|---|---|
| 4A v5 | **No disponible en el repositorio.** `ADENDA_4A_v6.md` dice que «se conserva congelado», pero el archivo no está. No se reconstruye su contenido. |
| `ADENDA_4A_v6.md` (23/10/2026) | Disponible. Es la fuente de la matriz vigente. |
| `GoalCapabilityPolicy.java` | Disponible. Implementa la matriz y `Assessment.primariesSatisfied()`. |
| Uso en validación | V25 (`PlanDraftValidator`): al menos una colocación por capacidad principal factible en la semana; en LOSE_WEIGHT, cardio en cada sesión; en INCREASE_STRENGTH, fuerza en los 4 slots. |

## 2. Matriz vigente y veredicto

| Objetivo | Principales | Secundarias | Veredicto | Motivo |
|---|---|---|---|---|
| GAIN_MUSCLE | HYPERTROPHY | STRENGTH | **B** | La hipertrofia responde a un rango amplio de cargas cerca del fallo [S48]. Las cargas altas aportan fuerza sin restar hipertrofia [S46, S47]. |
| INCREASE_STRENGTH | STRENGTH | HYPERTROPHY; POWER condicional | **B** | La fuerza depende de la carga relativa [S46, S48]. El volumen mejora la fuerza y la hipertrofia con rendimientos decrecientes [S02, S47]. POWER queda bloqueada en principiantes, una decisión prudente (C). |
| LOSE_WEIGHT | CARDIO | STRENGTH | **B** (estructura) / **E** (dosis aeróbica resultante, ver 4D) | El aeróbico es la modalidad más eficaz para el peso [S26, S27, S28] y la fuerza preserva la masa magra [S26, S04]. La estructura es correcta; lo que no alcanza es la **dosis** que 4D le asigna. |
| GENERAL_FITNESS | STRENGTH, MUSCULAR_ENDURANCE, CARDIO, MOBILITY | HYPERTROPHY; BALANCE oportunista | **A** | Coincide con la estructura multicomponente del ACSM [S34]. El concurrente es compatible [S24]. El equilibrio «oportunista» es una priorización justificable en adultos sanos (C) [S34, S36]. |

**Reglas de la política**

| Regla | Veredicto | Motivo |
|---|---|---|
| Una secundaria ausente nunca invalida un plan | **C** | Decisión de producto coherente con «la prescripción exacta importa menos que hacerla» [S01, S46]. |
| La principal se cubre en la semana, no en cada sesión | **B** | Las guías dan cifras semanales [S05, S34]. Con el volumen igualado, la distribución apenas importa [S13, S23]. |
| LOSE_WEIGHT: CARDIO única principal (v6 frente a v5) | **B** | Lo respaldan S26–S28. La OMS pide fortalecimiento ≥ 2 d/semana a todos los adultos [S05]. Ese fortalecimiento queda como **secundaria**, y la consecuencia se ve en el laboratorio (§3). |
| POWER solo para no principiantes con requisitos | **C** | No se buscó evidencia específica de potencia en principiantes. Es conservadora. |

## 3. Lo que muestra el laboratorio (960 perfiles, C1 vigente)

Fuente: `MATRIZ_EXPERIMENTOS_REVISADA.csv`.

**Cobertura de grupos grandes.** El indicador mide si algún grupo grande que la semana admite queda por debajo del objetivo escalado del experimento E: 4 series por semana fuera de GAIN_MUSCLE. **Ese objetivo es un parámetro de experimento, no una norma.**

| Objetivo | Planes con ese hallazgo |
|---|---|
| LOSE_WEIGHT | 174/240 |
| GENERAL_FITNESS | 137/240 |
| GAIN_MUSCLE | 197/240 |
| INCREASE_STRENGTH | 55/240 |

- **LOSE_WEIGHT.** Con 3 días y gimnasio, los niveles intermedio y avanzado tienen 0 series semanales en algún grupo grande a 30, 45 y 60 min. Es compatible con 4A v6, que no exige fuerza, pero se aleja de la recomendación de la OMS para todos los adultos [S05].
- **GAIN_MUSCLE.** El dato no significa que la hipertrofia esté mal servida. El objetivo E es 10 series, y con 3 días y gimnasio el mínimo por grupo grande sale entre 4 y 9.

**Ninguna de estas cifras justifica cambiar 4A.** Solo señalan dónde una decisión profesional podría añadir una salvaguarda.

## 4. Conclusión para 4A

- **Se conserva íntegra.** No hay evidencia que contradiga la elección de principales y secundarias de ningún objetivo.
- **Brecha documentada (no es un cambio).** En LOSE_WEIGHT, el fortalecimiento de todos los grupos grandes ≥ 2 d/semana no está garantizado. Queda como decisión en `DECISIONES_QUE_REQUIEREN_APROBACION.md` (D-07).

## 5. Limitaciones

- Ninguna afirmación se ha verificado en métodos y resultados completos: todas proceden de resúmenes.
- La aplicabilidad a adultos de Lima Metropolitana es una extrapolación desde adultos sanos en general. No se hallaron datos peruanos.
- 4A v5 y las especificaciones originales no están disponibles, de modo que el porqué original de la matriz solo se conoce a través de la adenda v6.
