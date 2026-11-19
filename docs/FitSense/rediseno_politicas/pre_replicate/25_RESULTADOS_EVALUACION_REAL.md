# 25 · Resultados de la evaluación con IA real (REV2, opción A: U + N, R y R′)

**Autorización:** la tuya del 05/11/2026, bajo el protocolo `23`:
- opción A, con R y R′ independientes;
- máximo de 168 predicciones y USD 8,40;
- P-H1 = `RESERVA_SIN_BLOQUEO` solo para fallos confirmados;
- catálogo actual.

**Solo evaluación experimental controlada:** no autoriza REV2 con usuarios reales.

**Ejecución:**

| Elemento | Valor |
|---|---|
| Commit | `adc795c` (`src/main` = `cc76bd1`) |
| Base | PostgreSQL efímero (Testcontainers) |
| Modelo | Replicate `openai/gpt-5-structured`, `gpt-5-mini`, razonamiento `medium` |
| Prompt | V2.1 `FIX-PR-2.1-EXP` |
| Política | `PLAN-REV-2.0` (`allow-candidates` solo en el arnés) |

**No se tocó durante el experimento:** V24, HIST, REV2, el catálogo, el prompt ni los oráculos.

**Evidencia:** `evidencia/k20_replicate_rev2/`:
- 7 corridas en `.tar.gz`, con su `SHA256SUMS`;
- `versiones.txt`;
- `evaluacion/`: criterios, métricas, sesiones, reproducibilidad, exposición y clasificaciones.

## 0. Desviación del protocolo, declarada antes de ejecutar

**Arnés (`adc795c`, solo pruebas):** tu autorización excluye los timeouts del P-H1. El arnés, sin embargo, trataba como no bloqueante un timeout cuya predicción terminaba cancelada.
- Se corrigió **antes** de cualquier corrida: un timeout con cancelación pedida por el arnés **bloquea** y carga la reserva.
- `ControlPresupuestoK20Test` 7/7.
- No cambia el sistema, la generación ni la adaptación, así que N sigue válida.
- **No llegó a activarse:** 0 fallos y 0 timeouts.

## 1. Comprobaciones y puertas (gratuitas)

| Paso | Resultado |
|---|---|
| `src/main` = `cc76bd1` · N regenerada idéntica (sha `8cbf3b85…`) · política por defecto `PLAN-HIST-1` · cuenta 200 · Docker | OK |
| Versión antes de cada corrida pagada y después de la última | `9f4cd9ec…` las 5 veces (21:06, 21:12, 21:21, 21:27 y 21:37 UTC). **Sin discrepancias** |
| `D_N` = `Dp_N` | Huella `11f51d44…`, idéntica. Clasificación: **19 PASS, 1 ESPERADO_SIN_PLAN (N19), 0 DEFECTO** → puerta superada |
| `D_U` | Huella `a662cb4d…` = la esperada (`17`) |

## 2. Gasto (dentro del límite global)

| Corrida | Predicciones | USD medido | Bloqueo |
|---|---:|---:|---|
| R_U | 8 | 0,1158 | ninguno |
| R_N | 14 | 0,1622 | ninguno |
| Rp_U | 8 | 0,1201 | ninguno |
| Rp_N | 14 | 0,1642 | ninguno |
| **Total** | **44 de 168** | **0,5622 de 8,40** | — |

**Detalle del gasto:**
- Tokens: 1.381.956 de entrada y 108.376 de salida.
- Las 44 predicciones tienen métricas (`MEDIDO`).
- Fallos del proveedor, reservas cargadas, timeouts y repeticiones: **0**.

**Conciliación con el proveedor** (historial, GET gratuito):
- 44 predicciones `succeeded` con `gpt-5-mini` entre las 21:07:28 y las 21:36:37 UTC;
- los **44 identificadores coinciden** con los del arnés;
- los mismos tokens, que a la tarifa dan USD 0,5622.

**La cifra oficial es la del panel de Replicate.**

## 3. Resultados por dimensión (criterios preregistrados de `23` §5, sin cambios)

### A · Factibilidad (oráculo `PLAN-REV-2.0+DIAS-1` + `clasificar_resultados.py`)

| Criterio | R_U | Rp_U | R_N | Rp_N |
|---|---|---|---|---|
| A-1: 0 DEFECTO | 20 PASS ✅ | 20 PASS ✅ | 19 PASS + N19 ESPERADO_SIN_PLAN ✅ | 19 PASS + N19 ESPERADO_SIN_PLAN ✅ |
| A-2: 0 incumplimientos de seguridad | 0 ✅ | 0 ✅ | 0 ✅ | 0 ✅ |

**Además:**
- continuidad S1 → S2 → S3 en todos los perfiles con plan;
- 0 ejercicios inactivos;
- 0 sesiones no señaladas alteradas;
- 0 aumentos de dosis o dificultad.

**Referencia** (no es el oráculo vigente):

| Oráculo | U | N |
|---|---|---|
| Histórico | 10/20 | 7/20 |
| REV2 sin contrato de días | 20/20 | 13/20 |

Las diferencias son el piso de V20 retirado y el contrato de días S-MAX, igual que en D (`17` §3).

### B · Aceptación por los validadores (por ciclo con IA)

| | R_U | Rp_U | R_N | Rp_N |
|---|---|---|---|---|
| Ciclos con IA | 8 | 8 | 14 | 14 |
| Aceptados en el intento 1 | 8 | 8 | 13 | 14 |
| Aceptados al final | 8 | 8 | 13 | 14 |
| Propuestas **rechazadas** por los validadores | 0 | 0 | 0 | 0 |
| Fallos del proveedor (B-2 ≤ 10 %) | 0 ✅ | 0 ✅ | 0 ✅ | 0 ✅ |

| Criterio | Valor | Umbral | Veredicto |
|---|---|---|---|
| B-1 U (R + R′) | **16/16 = 100 %** | ≥ 90 % | **CUMPLE** |
| B-1 N (R + R′) | **27/28 = 96,4 %**, Wilson inferior **82,3 %** | ≥ 80 % y ≥ 63 % | **CUMPLE** |

**El ciclo no aceptado (N09, R_N):**
- La IA respondió **`SIN_EFECTO`**: propuso no cambiar nada en una sesión factible de «demasiado difícil».
- **No fue un rechazo:** los validadores la admitieron como conservación (CONS-POL).
- Cuenta como no aceptado, porque no se publicó ningún cambio de la IA.
- En R′, el mismo ciclo sí se adaptó (ALT).

### C · Pertinencia de las adaptaciones

**Lo que hizo la IA:**

| Corrida | Cambios con categoría ACC | ALT | INC | Sustituciones | Reducciones de dosis | Sesiones «difícil» con variante más sencilla |
|---|---:|---:|---:|---:|---:|---:|
| R_U | 12 | 0 | 0 | 7 | 23 | 3 |
| Rp_U | 12 | 0 | 0 | 7 | 24 | 3 |
| R_N | 13 | 0 | 0 | 7 | 32 | 2 |
| Rp_N | 13 | 1 | 0 | 11 | 35 | 2 |

**Además:**
- **Desmotivación:** exactamente 1 cambio en cada una de las 16 sesiones (B-4 de R/R′: 0 sesiones con más de 2).
- **Ningún cambio NO_PERTINENTE** en ninguna corrida. Ninguno subió dificultad, impacto ni dosis, ni cambió de patrón o grupo, ni usó un sustituto inactivo.

| Criterio | Valor | Umbral | Veredicto |
|---|---|---|---|
| C-1: 0 NO_PERTINENTE por corrida | 0 / 0 / 0 / 0 | 0 | **CUMPLE** |
| C-2 U: efectivas y pertinentes / factibles evaluables | **17/17 = 100 %** | ≥ 90 % | **CUMPLE** |
| C-2 N | **14/15 = 93,3 %**, Wilson **70,2 %** | ≥ 80 % y ≥ 63 % | **CUMPLE** |
| C-3: ninguna corrida por debajo del mínimo | U 8/8 y 9/9 (≥ 80 %); N 6/7 = 85,7 % y 8/8 (≥ 75 %) | | **CUMPLE** |

**REQUIERE_REVISION (C-4), aparte y sin contar como pertinentes.** Son cambios de la IA, todos ACC según el oráculo, que tocan ejercicios con error de catálogo (`21`):

| Cohorte | REQUIERE_REVISION / factibles | Proporción |
|---|---:|---:|
| U (R + R′) | 7 / 24 | **29,2 %** |
| N (R + R′) | 13 / 28 | **46,4 %** |
| **Total** | **20 / 52** | **38,5 %** |

**Por ejercicio tocado** (una sesión puede tocar varios):
- 853 (cardio «media sentadilla», revisión visual): 12;
- 775: 4;
- 973: 4;
- 1262: 3;
- 1009 (curl femoral aislado): 3;
- 1115: 2.

**Sensibilidad (solo informativa; el criterio preregistrado no cambia):**

| Si los REQUIERE_REVISION fueran… | C-2 U | C-2 N |
|---|---|---|
| …todos pertinentes | 24/24 = 100 % | 27/28 = 96,4 % |
| …todos **no** pertinentes (peor caso) | 17/24 = **70,8 %** (< 90 %) | 14/28 = **50,0 %** (< 80 %) |

**Lectura:** el resultado de pertinencia depende de cómo se resuelva el catálogo. En el peor caso, C-2 no se cumpliría en ninguna de las dos cohortes. **La pertinencia de algo más de un tercio de las adaptaciones factibles no está demostrada.**

### D · Resultados esperados sin plan

| Criterio | Resultado |
|---|---|
| D-1: N19 (fuerza, 1 día de 31 min) = ESPERADO_SIN_PLAN en D, D′, R y R′, con `FUERZA_SIN_TIEMPO` registrado y 0 predicciones | **Sí, en las 4 corridas**. Se informa aparte; no cuenta como PASS ni como éxito de la IA |
| D-2: ningún plan ni código distinto para N19 | Cumple |
| D-3: ningún otro ESPERADO_SIN_PLAN | Cumple. N20 (32 min) sí obtiene plan, como preveía el manifiesto |

## 4. Reproducibilidad R frente a R′ (descriptiva, sin umbral)

| Cohorte | Sesiones señaladas | Misma decisión | Misma categoría | Mismo cambio exacto |
|---|---:|---:|---:|---:|
| U | 15 | 15/15 | 15/15 | **6/15** |
| N | 16 | 15/16 | 15/16 | **7/16** |

**Lectura:**
- La **decisión** (intervención, origen y si hay cambio) y la **categoría** se reproducen en 30 de 31 sesiones.
- La única diferencia es **N09 S2**: SIN_EFECTO en R frente a ALT en R′.
- El **cambio concreto** (qué ejercicio o qué dosis) solo coincide en 13 de 31: la IA llega a soluciones válidas distintas en cada corrida.
- Por tanto, R y R′ no son réplicas exactas; son dos muestras de una respuesta variable que en ambas cumple los criterios.

**Gasto y ciclos:** prácticamente iguales (8/8 y 14/14 predicciones; 0,116 frente a 0,120 y 0,162 frente a 0,164 USD).

## 5. Limitaciones del catálogo: exposición de los planes publicados

Descriptivo y sin umbral: cuenta los planes publicados con ejercicios dudosos, **los haya tocado o no la IA**.

| Corrida | Planes | Con 775 | Con 973 | Con 1320 | Con 580 | Con aislado de rodilla | Con alguno de `21` sin 853 | Con alguno, incluido 853 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| R_U / Rp_U | 60 | 18 (30 %) | 0 | 0 | 3 (5 %) | 18 (30 %) | 40 (66,7 %) / 42 (70,0 %) | 43 (71,7 %) / 45 (75,0 %) |
| R_N / Rp_N | 57 | 18 (31,6 %) | 12 (21,1 %) | 9 (15,8 %) | 0 | 24 (42,1 %) | 48 (84,2 %) | 51 (89,5 %) |

**La aceptación técnica de estos planes** (validadores y oráculo) **no es una validación deportiva**:
- en N, el 84 % de los planes publicados contiene al menos un ejercicio con error comprobado o pendiente de revisión;
- el oráculo y V24/V25 no lo detectan.

## 6. Veredicto

**Según los criterios preregistrados (`23` §5), sin cambiar ningún umbral: ÉXITO.**
- Se cumplen A, B, C y D en U y en N.
- No hay ningún fracaso parcial que informar.

**Lo que este resultado NO demuestra (resultados negativos y límites):**
1. **Pertinencia deportiva no demostrada en el 38,5 %** de las adaptaciones factibles, las REQUIERE_REVISION. En el peor caso, C-2 fallaría en U (70,8 %) y en N (50,0 %).
2. **Los planes contienen ejercicios mal clasificados** en el 67–84 % de los casos, y nada en la evaluación lo penaliza.
3. **El cambio concreto no se reproduce** entre R y R′ (13 de 31 iguales). Hubo una decisión distinta: N09, sin cambio frente a ALT.
4. **Un ciclo factible (N09, R) quedó sin adaptar** porque la IA propuso no cambiar nada.
5. **U no es no vista:** solo N lo es. N tiene 20 perfiles, 14 ciclos con IA por corrida y 15 sesiones evaluables en total. El intervalo de Wilson de C-2 en N tiene un límite inferior de 70,2 %, muy por encima del 63 %, pero con una muestra pequeña.
6. **Causas no evaluadas:** dolor, falta de tiempo y cambio de horario.
7. **Cifras de dosis [PRO]** sin validación profesional. La tarifa no está verificada en la web oficial.

**Comparación con R/R′ históricos en U** (descriptiva; cambió la política, así que no es un A/B limpio):

| | Ciclos con IA | Efectivas y pertinentes | Propuestas rechazadas |
|---|---|---|---|
| R/R′ históricos (HIST) | 8 por corrida | 10/10 por corrida | 0 |
| REV2 | 8 por corrida | 12 de 12 sesiones factibles adaptadas por corrida (8–9 pertinentes y 3–4 REQUIERE_REVISION) | 0 |

**Sin cambios en el sistema:** como N no reveló ningún defecto, no hay nada que corregir.
