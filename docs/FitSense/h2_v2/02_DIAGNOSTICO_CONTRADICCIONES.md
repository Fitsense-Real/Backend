# 02 · Diagnóstico: contradicciones entre el prompt de generación y los validadores

**Fuentes:**
- el código de `generation` (`98276a93`): `PlanPromptBuilder`, `PlanInputSnapshot`, `PrescriptionPrinciples`, `PlanDraftValidator`, `DosisSemanalPolicy` y `PlanningPolicy`;
- la PR #69 (PR-1.9);
- la evaluación real de H2 con gpt-5-mini (`docs/FitSense/cierre_gpt5_mini_h2/01_H2_PR17_VS_PR19.md` y su evidencia: 26 salidas reales con sus rechazos).

## 1. Restricciones efectivas del backend (política por omisión `PLAN-REV-2.0`)

| Regla | Qué comprueba de verdad |
|---|---|
| V4 | Ninguna sesión pasa de `session_minutes × 60` segundos estimados (`v4Maximo`: tolerancia **0 %**, no 15 %) |
| V15 | Cada sesión cubre al menos `min(2, grupos del enfoque con material prescribible, ejercicios de trabajo)` grupos.<br>Sin grupos ajenos, salvo FULL_BODY.<br>**Regla de la mitad:** con 3 o más grupos en el enfoque y 4 o más ejercicios de trabajo, ningún grupo pasa de `n / 2` (división entera).<br>Los complementos (cardio y movilidad) no cuentan |
| V20 | **No rechaza** con `PLAN-REV-2.0` (`Duracion.DOSIS_SEMANAL`): no hay mínimo de duración |
| V24 | Dosis exacta por clase (GAIN_MUSCLE, 4D) o por capacidad (resto), y máximo de colocaciones por día.<br>**Sin mínimo de duración, la serie adicional de 4D (`series_max`) se rechaza siempre** |
| V26 | Series **directas** por `body_part`: ejercicios ESTRUCTURAL o RESISTIDO que no son complemento.<br>Máximo **6** por sesión en BEGINNER y **10** en el resto.<br>Máximo **12, 20 o 24** por semana (BEGINNER, INTERMEDIATE, ADVANCED) |
| V23 / V25 | Slots obligatorios de la semana y capacidades principales factibles: ya los comunica PR-1.7 |

## 2. Contradicciones encontradas

En cada caso, «qué dice PR-1.7» frente a «qué exige el backend», con la evidencia.

| # | Contradicción | PR-1.7 (vigente) | Backend | Evidencia real (gpt-5-mini) | PR-1.9 (#69) | PR-2.0 |
|---|---|---|---|---|---|---|
| **C1** | La regla de la mitad de V15 **no llega a la IA en producción** | La regla 15 dice «no pongas más de `max_per_body_part`».<br>Con `PLAN-REV-2.0` no hay mínimo de minutos, así que `min_exercises_per_session` es `null` y **`max_per_body_part` es `null` en todas las sesiones**. La regla remite a un campo vacío | Mitad de los ejercicios de trabajo con 3+ grupos y 4+ ejercicios | G14-A «3 de 4 a upper legs» ×2; G18-A «4 de 6», «4 de 5»; G08-B «4 de 4» ×2. Todo en LOWER_BODY | Tabla por `n`: **corregido** | Tabla por `n` (se conserva) |
| **C2** | **V26 no se comunica** | Ni el techo por sesión ni el semanal | 6/10 por sesión y 12/20/24 por semana | G18-A y G08-B «8 series de upper legs, techo 6» y «16 en la semana»; G05-B «11, techo 10»; G14-B V26 ×3 | **No** (por eso V26 subió de 1 a 3 casos) | **Regla 21** con los techos del nivel, más la comprobación final |
| **C3** | La orientación de volumen de GAIN_MUSCLE no coincide con la política | «unas 10 series por grupo muscular a la semana, si la sesión lo permite» | Objetivo 6 (BEGINNER) o 10 (INTERMEDIATE), techo 12/20. «Si la sesión lo permite» invita a concentrar | Concentración en upper legs (C2) | No | `objetivo_series_semana` por grupo, que **sustituye** a la de los principios y nunca pasa del techo |
| **C4** | Instrucciones de **llenar tiempo** que `PLAN-REV-2.0` ya no pide | Varias:<br>• regla 17: «comprobar que cada sesión llega a su mínimo»;<br>• capacidades: «completa el tiempo con las secundarias»;<br>• fuerza: «si queda tiempo, completa con HYPERTROPHY» | Sin mínimo (V20 no rechaza). El tiempo solo limita por arriba (V4) | Empujan a sumar colocaciones y series hasta V26 | No | Sin mínimo: «no añadas ejercicios ni series solo para llenar tiempo», respetando los techos |
| **C5** | **Dos límites distintos** para la duración | Regla 4: «no puede superar `session_minutes` más 15 %».<br>El bloque DURACIÓN dice el límite en segundos, al 100 % | V4 al **100 %** | — | No | Regla 4: `session_minutes` es el máximo (`max_session_seconds`) |
| **C6** | La **serie adicional** de 4D (GAIN_MUSCLE) | «puedes darle `series_max` … si sin esa serie la sesión no llega a su duración mínima» | Sin mínimo, **V24 la rechaza siempre** | — | No | «Sin excepciones … usa siempre `series`» |
| **C7** | `min_body_parts` puede pedir **grupos sin material** | `min(2, grupos del enfoque)` | `min(2, grupos con material prescribible, n)` | G05-B: LEGS «solo trabaja upper legs» | No | `body_parts_con_material` (lo que V15 llama disponibles) y `min_body_parts` calculado con ellos |
| **C8** | El **reintento** pide algo que V15 y V26 pueden impedir | «Si sobran de un grupo … REEMPLÁZALOS por otros del enfoque, **nunca los elimines sin reponerlos**».<br>«Si una sesión no llega al mínimo de minutos, añádele un ejercicio».<br>Ninguna pista para V15 ni V26 | Si ningún otro grupo con material admite más, hay que quitar | Rechazos que se repiten en los reintentos (V15 y V26) | No | Reemplazar dentro de `body_parts_con_material` o quitar; pistas de V15 y V26; sin la frase del mínimo |
| **C9** | `min_exercises_per_session` por encima del tope de V24 | Calculado con `target_sets × target_reps`. Con capacidades no son las dosis de V24 | Tope `max_colocaciones_dia` | Solo con políticas con mínimo de duración (`PLAN-HIST-1`) | Corregido | Se conserva la corrección de #69: nunca por encima del tope y `null` con capacidades |

**Comprobado y sin contradicción:**
- V20 frente a V26 con `PLAN-HIST-1`: allí V26 no aplica (`volumenSemanal=false`).
- V24: el máximo de colocaciones ya se comunica.
- V23 y V25: slots y capacidades ya están en el prompt.
- Ninguna contradicción impide un plan válido con el catálogo. La rutina del motor de reglas cumple todas las cuotas en los 26 perfiles (`04` §2).

## 3. Por qué PR-1.9 no bastaba

PR-1.9 corrigió C1 (y C9), pero dejó C2–C8:
- **V26:** el modelo seguía cargando `upper legs` hasta pasar el techo, que no conocía. V26 pasó de 1 a 3 casos.
- **C4:** las frases de llenar tiempo seguían empujando a añadir.
- **Latencia:** las propuestas de B fueron más largas (mediana de 6.541 caracteres frente a 5.818) y unos 14–19 s más lentas.

PR-2.0 parte del mismo principio («decir lo que el validador comprueba»), pero lo aplica a **todas** las reglas que se cruzan con el reparto. Además dice explícitamente cuándo **poner menos** es la respuesta correcta.
