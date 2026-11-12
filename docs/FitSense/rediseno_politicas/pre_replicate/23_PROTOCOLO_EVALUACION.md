# 23 · Protocolo definitivo de la evaluación con IA real (opción A: U + N) — NO EJECUTADO

**Estado:**
- Fijado **antes** de cualquier ejecución sobre N y de cualquier llamada pagada.
- Requiere tu **autorización explícita**: no se ha hecho ninguna predicción.
- Los criterios de §5 no se cambian después de ver resultados. Están implementados en `evaluar_rev2.py`, con pruebas sintéticas en `test_evaluar_rev2.py` (3/3), y se han probado solo sobre corridas simuladas de U y M.

## 1. Versión evaluada

| Elemento | Valor |
|---|---|
| Rama | `investigacion/rediseno-politicas` (sin merge) |
| Código (`src/main`) | `cc76bd1`, sin cambios desde entonces. Los commits posteriores solo añaden pruebas, herramientas y documentos |
| Commit de ejecución | `6263506` (congela N) o uno posterior que **solo** añada documentos. Cada corrida lo registra en `00_configuracion.txt` |
| Política | `PLAN-REV-2.0` (candidata), con `allow-candidates=true` **solo en el arnés**. La política por defecto sigue siendo `PLAN-HIST-1` |
| Base | PostgreSQL 16 efímero (Testcontainers). Ni Neon ni ninguna base real |
| Prompt | V2.1 = `FIX-PR-2.1-EXP` (`-Dfitsense.evaluacion.k20.prompt=v2.1`), con los permisos de REV2 (`PermisosIntervencion`) |
| Progresión | Interruptor apagado, como en todas las corridas K20 (`progresion: false`) |
| Gemelos de generación con IA | Desactivados (`gemelos=false`). La IA solo interviene en la adaptación |

## 2. Proveedor (comprobado el 05/11/2026 sin gasto)

| Dato | Valor | Cómo se comprobó |
|---|---|---|
| Modelo | Replicate `openai/gpt-5-structured`, oficial y público | `GET /v1/models/openai/gpt-5-structured` (gratuito) |
| Versión vigente | `9f4cd9ec1133f55d442aeb426e42df5180a56e79a33183623611d62d4c3b44ae` (21/01/2026). **Es la misma de R/R′** | Mismo `GET` (`latest_version`) |
| Parámetros | `model=gpt-5-mini`, `reasoning_effort=medium`. Los dos valores existen en el esquema de la versión (`gpt-5`/`gpt-5-mini`/`gpt-5-nano`; `minimal`/`low`/`medium`/`high`) | `application.properties` y esquema OpenAPI de la versión |
| Acceso | `GET /v1/account` = **200** con la credencial que inyecta el proxy. El token no es visible ni se ha impreso | Consulta gratuita |
| Facturación | Por tokens; cada predicción devuelve `token_input_count` y `token_output_count` | Historial (`GET /v1/predictions`, gratuito) |
| Tarifa | **0,25 / 2,00 USD por millón** de tokens de entrada / salida. **No verificable en la web oficial** desde esta sesión (`replicate.com` no está en la red permitida). Coincide con agregadores de precios (consulta del 05/11/2026) y con lo usado en R/R′ | Búsqueda web |
| Coste real reciente con esa tarifa | 05/11 01:38–03:51 UTC: 72 predicciones, **0,0138 USD** cada una. 03/11: 126 predicciones, **0,0171 USD** cada una. **Ninguna predicción después de las 03:51 UTC del 05/11**, es decir, ninguna en el cierre | Historial (tokens × tarifa) |

**Riesgo de versión:**
- El backend llama al endpoint del **modelo** (`/v1/models/.../predictions`), no a una versión fija.
- Las predicciones de modelos oficiales no informan de su versión (`version: hidden`).
- **Control:** `GET latest_version` justo antes de la primera corrida pagada y justo después de la última. Si no es `9f4cd9ec…`, la evaluación se detiene y se informa como **no comparable** con R/R′.

## 3. Cohortes y corridas

| Cohorte | Perfiles | Ciclos con IA posibles | Por qué |
|---|---|---|---|
| **U01–U20** | 20 (sha256 `584e2204…`) | 8 (los mismos de R/R′; IA simulada, `17`) | Comparabilidad con R/R′. **No es no vista** |
| **N01–N20** | 20 (`cohorte_N/N_MANIFIESTO.md`) | ≤ 19 (N19 no genera plan) | **Única cohorte no vista.** Nunca se ha ejecutado |

**Orden** (cada paso solo si el anterior lo permite):

| # | Corrida | IA | Coste | Puerta |
|---|---|---|---|---|
| 0 | Comprobaciones previas (§4) | — | 0 | Todas OK |
| 1 | `D_N`, `Dp_N` (REV2, sin IA) | No | 0 | D = D′ (huella) y, con `clasificar_resultados.py`: N19 = ESPERADO_SIN_PLAN y 0 DEFECTO. **Si hay un DEFECTO, se para aquí**: N no se modifica, se informa y se decide contigo. Corregir el sistema invalidaría N y obligaría a una cohorte nueva |
| 2 | `D_U` (REV2, sin IA) | No | 0 | Huella `a662cb4d…` (la de `17`) |
| 3 | `R_U`, `R_N` | Sí | Pagado | — |
| 4 | `Rp_U`, `Rp_N` (R′, independiente) | Sí | Pagado | — |
| 5 | Oráculos y evaluación (§5) | — | 0 | — |

## 4. Comprobaciones previas (gratuitas, en la misma sesión que ejecuta)

1. `git rev-parse HEAD` = commit de N. `git diff cc76bd1 HEAD -- src/main` vacío.
2. `sha256sum` de `N_PERFILES_INDEPENDIENTES.csv` y de `generar_cohorte_N.py` = manifiesto. La regeneración da un CSV idéntico (`cmp`).
3. `GET /v1/account` = 200. `GET latest_version` = `9f4cd9ec…`.
4. Docker y Maven offline disponibles. Ningún token en ficheros, registros ni commits.
5. Política por defecto del repositorio `PLAN-HIST-1` (sin cambios en `application.properties`).

## 5. Criterios de éxito y fracaso (fijados de antemano; cuatro dimensiones separadas)

**Reglas generales:**
- **El oráculo vigente es `PLAN-REV-2.0+DIAS-1`.** El histórico y el REV2 sin contrato de días se informan solo como referencia.
- Una dimensión **no compensa** a otra.
- **No hay repeticiones selectivas**: ni de un perfil ni de un ciclo.

### A · Factibilidad (por perfil: `clasificar_resultados.py`)

| Id | Criterio | U | N |
|---|---|---|---|
| A-1 | 0 perfiles DEFECTO en cada corrida: PASS + ESPERADO_SIN_PLAN = todos | 20/20 PASS | 19 PASS + N19 ESPERADO_SIN_PLAN |
| A-2 | 0 incumplimientos de seguridad del oráculo en cada corrida: nada inválido publicado, ninguna dosis o dificultad que suba, ninguna sesión no señalada alterada, ningún ejercicio inactivo | 0 | 0 |

**Además:** continuidad S1 → S2 → S3 para cada perfil con plan. Está incluida en el oráculo (G_continuidad). El respaldo cuenta como publicado, pero se informa aparte.

### B · Aceptación por los validadores (por ciclo con IA)

| Id | Criterio | U | N |
|---|---|---|---|
| B-0 | Ciclos con IA, aceptados en el intento 1, aceptados al final (≤ 3 intentos) y con fallo del proveedor | Informativo | Informativo |
| B-1 | R + R′: aceptados al final / ciclos con IA (sin los fallos del proveedor) | ≥ 90 % | ≥ 80 % **y** Wilson 95 % inferior ≥ 63 % |
| B-2 | Ciclos con fallo del proveedor en cada corrida | ≤ 10 % | ≤ 10 % |

**Si B-2 no se cumple:**
- La corrida es **INCIDENCIA_PROVEEDOR**: se conserva y se evalúa aparte.
- **No** cuenta para B ni para C.
- Se permite **una** repetición completa, solo si cabe en el tope autorizado. Si no cabe, se para y se te consulta.

**Aceptar no es pertinencia:** un cambio aceptado por V8C/V24 puede no ser pertinente (C).

### C · Pertinencia de las adaptaciones (por sesión señalada; script contra el catálogo de la corrida)

**Un cambio de la IA es PERTINENTE si cumple todo esto:**
- categoría ACC o ALT del oráculo;
- dificultad e impacto no superiores;
- mismo patrón y mismo grupo;
- sustituto activo;
- dosis no mayor;
- coherente con la causa:
  - **«demasiado difícil»:** baja la dificultad o la dosis, salvo que ya esté en dificultad 1;
  - **desmotivación:** como máximo 2 cambios.

**Clases que no cuentan como pertinentes:**
- **REQUIERE_REVISION:** un cambio que toca un ejercicio con error de catálogo (`21`: 775, 973, 1320, 580, los 13 aislados de rodilla, 1261, 1308, 1263, 1093, 1262, 1115, 853). No es pertinente ni fallo, y se informa aparte.
- **NO_PERTINENTE:** un cambio que incumple una regla. **Nunca** pasa a REQUIERE_REVISION.

| Id | Criterio | U | N |
|---|---|---|---|
| C-1 | 0 cambios de la IA NO_PERTINENTES en cada corrida | 0 | 0 |
| C-2 | R + R′: efectivas y pertinentes / sesiones factibles evaluables (la sonda de la misma corrida, sin REQUIERE_REVISION) | ≥ 90 % | ≥ 80 % **y** Wilson ≥ 63 % |
| C-3 | Ninguna corrida por debajo de… | 80 % | 75 % |
| C-4 | Cambios REQUIERE_REVISION; proporción de ACC (B-3 de R/R′) | Informativo | Informativo |

**Los umbrales son los aprobados en R/R′:** U ≥ 90 %; la cohorte independiente ≥ 80 % con Wilson ≥ 63 %. En U, el «ninguna por debajo de 9/10» pasa a ser un 80 %, porque con REV2 el número de factibles de U cambia.

### D · Resultados esperados sin plan

| Id | Criterio |
|---|---|
| D-1 | N19 (fuerza, 1 día de 31 min) = **ESPERADO_SIN_PLAN** (`FUERZA_SIN_TIEMPO`, registrado en `incidencias.log`) en D, D′, R y R′. Se informa **aparte**: nunca cuenta como PASS ni como éxito de la IA, y no genera ninguna predicción |
| D-2 | Si N19 obtiene plan, o falla con otro código, es un **DEFECTO** (cuenta en A-1) |
| D-3 | Ningún otro perfil puede quedar ESPERADO_SIN_PLAN: solo los que el manifiesto lo prevé |

### Veredicto global

- **ÉXITO:** se cumplen A, B, C y D en U y en N.
- **FRACASO PARCIAL:** cualquier criterio no cumplido. Se informa cuál, sin reinterpretarlo.
- **Un fracaso en N no se corrige sobre N:** cualquier cambio del sistema exige una cohorte nueva.

**Comparaciones (descriptivas, sin umbral):**
- R frente a R′: misma categoría y mismo cambio por sesión (`DIFERENCIAS`).
- U con REV2 frente a R/R′ históricos: sabiendo que la política cambió (V20, dosis, días), así que no es una comparación A/B limpia.

## 6. Presupuesto y control de gasto

| Corrida | Predicciones máx. (`max-llamadas`) | Tope USD (`max-usd`) |
|---|---:|---:|
| R_U | 24 (8 ciclos × 3) | 1,20 |
| R_N | 60 (20 × 3; N19 no llama) | 3,00 |
| Rp_U | 24 | 1,20 |
| Rp_N | 60 | 3,00 |
| **Total autorizable** | **168** | **8,40** |

**Gasto esperado:**
- Coste medido de 0,0138–0,0171 USD por predicción y entre 1 y 2 intentos por ciclo.
- Unas 27 × 2 corridas = 54 ciclos → **unos USD 0,7–1,9**.
- El informe de cierre (`18` §4) estimaba USD 2–5. Era una estimación prudente; esta usa el coste real medido.

**Controles del arnés (sin cambios):**
- Reserva de peor caso de 0,05 USD por predicción.
- La siguiente predicción solo se hace si el gasto medido más la reserva no pasa del tope.
- Tarifa explícita en el comando (queda en `00_configuracion.txt`).

**Si se repite una corrida por INCIDENCIA_PROVEEDOR (B-2),** su tope sale del mismo total de 168 / 8,40. Nunca se amplía sin tu autorización.

**Decisión que te corresponde: predicción sin métricas (P-H1)**

| Opción | Comportamiento |
|---|---|
| `BLOQUEAR` (por defecto; R/R′) | Se carga la reserva y no se hace ninguna llamada más. La corrida queda incompleta |
| `RESERVA_SIN_BLOQUEO` | Si la predicción **falló en el proveedor** (sin respuesta), se carga la reserva y se sigue: la comprobación previa mantiene la cota. Una predicción que **respondió** sin métricas sigue bloqueando |

**Recomendación: `RESERVA_SIN_BLOQUEO`.** En R/R′, un fallo del proveedor obligó a repetir 2 de 6 corridas.

## 7. Comando (a ejecutar solo con autorización; el token lo inyecta el proxy)

```
SHA=$(git rev-parse HEAD); N=docs/FitSense/rediseno_politicas/cohorte_N/N_PERFILES_INDEPENDIENTES.csv
E=docs/FitSense/rediseno_politicas/evidencia/k20_replicate
base="sh ./mvnw -o -B -ntp verify -Pintegration -DskipUTs=true -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false \
  -Dit.test=EvaluacionK20IT -Dfitsense.evaluacion.k20=true -Dfitsense.evaluacion.k20.commit=$SHA \
  -Dfitsense.planning.policy=PLAN-REV-2.0 -Dfitsense.planning.policy.allow-candidates=true"
# 1-2 (gratuitas)
$base -Dfitsense.evaluacion.k20.modo=DETERMINISTA -Dfitsense.evaluacion.k20.perfiles=$N -Dfitsense.evaluacion.k20.salida=$E/D_N
$base -Dfitsense.evaluacion.k20.modo=DETERMINISTA -Dfitsense.evaluacion.k20.perfiles=$N -Dfitsense.evaluacion.k20.salida=$E/Dp_N
$base -Dfitsense.evaluacion.k20.modo=DETERMINISTA -Dfitsense.evaluacion.k20.salida=$E/D_U
# 3-4 (PAGADAS; solo con autorizacion)
ia="-Dfitsense.evaluacion.k20.modo=REPLICATE -Dfitsense.evaluacion.k20.autorizacion-replicate=SI \
  -Dfitsense.evaluacion.k20.credencial=proxy -Dfitsense.evaluacion.k20.prompt=v2.1 \
  -Dfitsense.evaluacion.k20.tarifa-entrada-usd-millon=0.25 -Dfitsense.evaluacion.k20.tarifa-salida-usd-millon=2.00 \
  -Dfitsense.evaluacion.k20.sin-metricas=<BLOQUEAR|RESERVA_SIN_BLOQUEO>"
$base $ia -Dfitsense.evaluacion.k20.max-llamadas=24 -Dfitsense.evaluacion.k20.max-usd=1.20 -Dfitsense.evaluacion.k20.salida=$E/R_U
$base $ia -Dfitsense.evaluacion.k20.perfiles=$N -Dfitsense.evaluacion.k20.max-llamadas=60 -Dfitsense.evaluacion.k20.max-usd=3.00 -Dfitsense.evaluacion.k20.salida=$E/R_N
# R': lo mismo con Rp_U y Rp_N
# 5 (gratuito)
python3 -I docs/FitSense/rediseno_politicas/herramientas/oraculo_k20_politica.py PLAN-REV-2.0+DIAS-1 <carpeta> <perfiles> [teorico U]
python3 -I docs/FitSense/rediseno_politicas/pre_replicate/evaluar_rev2.py <salida> U=$E/R_U U=$E/Rp_U N=$E/R_N N=$E/Rp_N
```

## 8. Incidencias (decididas de antemano)

| Incidencia | Qué se hace |
|---|---|
| Fallo del proveedor en una predicción | Respaldo del sistema; se anota. B-2 decide si la corrida vale |
| Timeout o estado desconocido | Bloqueo (P-H1). La corrida queda incompleta y se evalúa aparte |
| Tope alcanzado | Los ciclos siguientes no llaman (`PRESUPUESTO_AGOTADO_SIN_LLAMADA`), se cuentan y se informa. No se amplía el tope |
| Fallo de infraestructura (Docker, Maven) **antes** de la primera predicción | Se repite la corrida (no hubo gasto) |
| Fallo de infraestructura **después** de alguna predicción | Se conserva. Una repetición completa solo si cabe en el tope; si no, se para y se te consulta |
| `latest_version` distinta de `9f4cd9ec…` | Se para y se informa como no comparable |
| DEFECTO en D_N | Se para antes de pagar (§3, paso 1) |

## 9. Limitaciones que se declararán con los resultados

- U, J, K, L y M ya no son no vistas; solo N lo es. U mide compatibilidad, no generalización.
- N usa las mismas causas que U/J/K/L/M. Dolor, falta de tiempo y cambio de horario **no** se evalúan.
- El catálogo tiene errores comprobados (`21`) que el oráculo no detecta. Los cambios que los tocan van a REQUIERE_REVISION.
- Todas las cifras de dosis son [PRO], sin validación profesional.
- La IA real puede variar entre corridas: R y R′ miden esa variabilidad, no la eliminan.
- La tarifa no está verificada en la web oficial. El gasto oficial es el del panel de Replicate.
