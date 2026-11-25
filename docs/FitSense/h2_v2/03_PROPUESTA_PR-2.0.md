# 03 · Propuesta corregida: prompt de generación PR-2.0

**Identificación:**
- versión de reglas **`PR-2.0`** (`PlanPromptBuilder.VERSION_H2V2`);
- `input_snapshot.prompt_version = PR-2.0`;
- etiqueta de la petición `PR-2.0 <principios>`.

**Sin sobrescribir:**
- PR-1.7 sigue siendo el texto por omisión (`PlanPromptBuilder.VERSION`);
- PR-1.9 sigue en la PR #69, que no se fusiona;
- la evidencia histórica no se toca.

**Activación:** `fitsense.ai.generacion.prompt-version`. Su variable de entorno es `FITSENSE_AI_GENERACION_PROMPT_VERSION`.

| Valor | Efecto |
|---|---|
| sin definir, o `PR-1.7` | **Por omisión.** Prompt e instantánea idénticos a los de `generation` |
| `PR-2.0` | Esta propuesta. Solo la generación inicial: la adaptación no lee la propiedad |
| cualquier otro | Error al arrancar: no se reinterpreta |

**Lo mismo para los cuatro objetivos:** las mismas reglas y la misma comprobación. Lo que cambia por objetivo ya cambiaba antes: la tabla 4D en GAIN_MUSCLE y las capacidades en los otros tres.

**Ningún validador cambia.** V4, V8C, V15, V20, V23, V24, V25, V26 y el resto son los de `generation`.

## 1. Instantánea (`PlanInputSnapshot.of(context, true)`)

| Campo | Contenido | Contradicción |
|---|---|---|
| `constraints.complementos_no_cuentan` | ids de los complementos (cardio y movilidad), que V15 y V26 no cuentan | C1, C2 |
| `constraints.volumen_semanal` | `techo_series_sesion`, `techo_series_semana`, `objetivo_series_semana` (por grupo, de `DosisSemanalPolicy`) y `no_cuentan` (ejercicios de trabajo cuyas series no son directas) | C2, C3 |
| `suggested_split[].max_per_body_part_por_n_trabajo` | La regla de la mitad, n = 4…`max_colocaciones_dia` (sin la propiedad, 10). Solo si el enfoque tiene 3 o más grupos. `max_per_body_part` llega `null` | C1 |
| `suggested_split[].body_parts_con_material` | Los grupos del enfoque con ejercicios de trabajo prescribibles para esta persona: lo que V15 llama disponibles | C7 |
| `suggested_split[].min_body_parts` | `min(2, |body_parts_con_material|)`, que es el mínimo de V15 | C7 |
| `constraints.min_exercises_per_session` | Nunca por encima de `max_colocaciones_dia`; `null` con capacidades (la corrección de #69) | C9 |

**El contrato está en una sola clase de dominio**, `ContratoReparto`. Replica V15 y V26 sin cambiarlos, y `ContratoH2v2IT` comprueba con el validador real que coinciden.

## 2. Texto (sustituciones sobre PR-1.7)

Cada sustitución exige encontrar la frase de PR-1.7. Si alguien cambia PR-1.7, PR-2.0 falla al construirse en vez de quedar a medias.

| Dónde | PR-1.7 | PR-2.0 | Cuándo |
|---|---|---|---|
| Regla 4 | «no puede superar `session_minutes` más 15 %» | `session_minutes` es el máximo; ninguna sesión pasa de `max_session_seconds` | V4 al 100 % (`PLAN-REV-2.0`) |
| Regla 15 | «… no pongas más de `max_per_body_part` … si un grupo llega a su tope, sigue por otro» | Cubre `min_body_parts` de `body_parts_con_material`, sin grupos ajenos. Los complementos no cuentan. Con n ejercicios de trabajo, el tope es el valor de n en la tabla. Si ningún grupo con material admite más, **no añadas más ejercicios**: «una sesión con menos ejercicios es válida, una que pasa un tope no» | Siempre |
| Regla 17 | «comprobar que cada sesión llega a su mínimo … llega con ejercicios o series» | «Esta semana NO hay mínimo de duración … No añadas ejercicios ni series solo para llenar tiempo» | Sin mínimo de V20 |
| **Regla 21 (nueva)** | — | VOLUMEN POR GRUPO MUSCULAR: suma de `planned_sets` por `body_part` (sin complementos ni `no_cuentan`). Máximo N por sesión y M por semana, con los números del nivel. Incluye un ejemplo (3 series por ejercicio caben floor(N/3) veces). `objetivo_series_semana` sustituye a la orientación de los principios y nunca justifica pasar un techo | Si aplica V26 |
| Bloque 4D (GAIN_MUSCLE) | «Única excepción: … `series_max` … si sin esa serie la sesión no llega a su mínimo» | «Sin excepciones: … la serie adicional NO se admite: usa siempre `series`» | Sin mínimo de V20 |
| Capacidades | «Después completa el tiempo con las secundarias» | «Después, si caben, añade secundarias, sin pasar de `max_colocaciones_dia` ni de los techos de la regla 21: no hace falta llenar el tiempo» | `DOSIS_SEMANAL` |
| Regla prioritaria | «Solo después completa el tiempo con las secundarias …» | «Solo después, si caben, añade secundarias … ni de los techos de la regla 21. No hace falta llenar el tiempo» | `DOSIS_SEMANAL` |
| Fuerza | «Solo después, si queda tiempo, completa con HYPERTROPHY» | «Solo después, si caben dentro de los techos de la regla 21, añade HYPERTROPHY» | `DOSIS_SEMANAL` |
| Comprobación de capacidades | «ningún `body_part` pasa del `max_per_body_part`» | «El reparto de cada sesión cumple la COMPROBACIÓN DEL REPARTO» | Con capacidades |
| **Comprobación del reparto (nueva)** | — | Después de los datos, sesión por sesión: n de trabajo y su tope; `min_body_parts` de `body_parts_con_material` sin ajenos; techos de V26; si un grupo llega a su tope y no queda otro, menos ejercicios | Siempre |
| Reintento | «… nunca los elimines sin reponerlos»; «si una sesión no llega al mínimo de minutos, añádele un ejercicio»; «reglas 1 a 20» | Reemplazar dentro de `body_parts_con_material` o, si ninguno admite más, quitar. Para V26, bajar series dentro de la dosis. Sin la frase del mínimo. «Reglas 1 a 21». Pistas de V15 y V26 | Al reintentar |

**Tamaño:** PR-2.0 añade entre **2.035 y 2.843 caracteres** por prompt (+1,1 % a +7,5 %; mediana 2.526) en los 26 casos. La cota de entrada sube unos USD 0,0006 por predicción.

## 3. Lo que no se hace (a propósito)

- No se cambia ningún validador ni ninguna política: ni V8C, V15, V20, V24 o V26, ni `DosisSemanalPolicy` o `PlanningPolicy`.
- No se cambian los principios (`PrescriptionPrinciples`): PR-2.0 sustituye las frases afectadas en el texto final del prompt, solo en la generación inicial.
- No se copia PR-1.9. Se reutiliza su corrección de C1 y C9, que la evaluación real mostró eficaz para V15. A esa base se añade C2–C8.
- No se activa.
