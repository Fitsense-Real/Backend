# 08 · Congelación de la versión definitiva (V2.1) para D, D′, R y R′

**Autorización:**
- «APRUEBO V2.1 como versión definitiva candidata […] Autorizo congelar la candidata, preparar los comandos, ejecutar las comprobaciones sin IA pagada y verificar la CI».
- **R y R′ NO están autorizados todavía:** se presentan aquí la configuración y el presupuesto para tu autorización.

**Desde este commit no cambian:**
- el código (`src/`, `pom.xml`);
- el prompt;
- el oráculo;
- los perfiles U y J;
- los criterios (`07`).

Cualquier corrección obliga a una versión nueva y a repetir todo, y se informaría como tal. **J01–J16 no se usa para ajustar nada**: un fallo en J se informa como resultado.

## 1. Versión

| Elemento | Valor congelado |
|---|---|
| Rama | `eval/final-ieee-k20-v2` (PR borrador [#41](https://github.com/Fitsense-Real/Backend/pull/41), solo CI; no se fusiona) |
| **SHA de congelación** | El commit que añade este documento. Se anota en `configuracion.json` / `00_configuracion.txt` de cada corrida y en §6 tras el CI |
| **Árbol `src/`** (hash git, independiente del commit) | `08e69206d594fe9ae32a9db29fab618c67915371`, idéntico al de `6b5ccb6` (CI verde: run 41939643726) y al de `fe30096` (CI verde: run 41951213701) |
| `pom.xml` | sha256 `6a3f6895…a94081` |
| `application.properties` | sha256 `ef02a059…8b26b15` |
| Base | `eval/final-ieee-k20` (`generation` `76e15c5` + #38 + #39 + P3) |

## 2. Contenido funcional

| Pieza | Estado |
|---|---|
| **Prompt de adaptación** | **V2.1, `FIX-PR-2.1-EXP`** (`SessionFixPromptBuilder` sha256 `1095e4e9…`), elegida tras DEV-R (`A3`). Se activa con `-Dfitsense.evaluacion.k20.prompt=v2.1`, que el arnés traslada a `fitsense.ai.adaptation.prompt-version`. El valor por omisión del backend sigue en `v1`: la rama no se despliega |
| **4D v3.4 + C-8** | Tope de colocaciones de GAIN_MUSCLE: 6 (30–52 min), 7 (53–60), 8 (61–68) y 9 (69–75) (`MvpDosePolicy` sha256 `c31243c8…`; `ADENDA_4D_v3_4.md`) |
| Límite de dificultad | `preferencia` (`LIMITE_Y_PREFERENCIA`), fijado en el arnés |
| Retirada localizada ante dolor | `on`, fijada en el arnés; solo actúa con el límite |
| H1 (V8C multiconjunto) | por defecto (`MULTICONJUNTO`) |
| Corrección P3 | incluida (base `eval/final-ieee-k20`) |
| Progresión | **desactivada** |
| Política de planificación | `PLAN-HIST-1` (por defecto) |
| V20, V24 y demás validadores | sin cambios, salvo el tope de 4D (v3.4/C-8) |
| Catálogo | 1.219 ejercicios activos, huella `425fc958d26b405a00a233fe9d8a3c3b` |
| Migraciones | 46 (Flyway 44.2), checksum `9981294ded945dc1882bd26f2468c30f` |
| Base de datos | PostgreSQL 16 efímero (Testcontainers), sin Neon |
| Semanas | S1 2026-11-02 (medida) → S2 2026-11-09 (continuidad) → S3 2026-11-16 |

## 3. Modelo y parámetros (R y R′)

| Parámetro | Valor |
|---|---|
| Proveedor | Replicate, `openai/gpt-5-structured` (oficial), versión `9f4cd9ec…` |
| Variante | `gpt-5-mini` |
| Esfuerzo de razonamiento | `medium` |
| Intentos por ciclo | hasta 3 |
| `prefer-wait` | 25 s |
| Credencial | la del proxy de la sesión (`-Dfitsense.evaluacion.k20.credencial=proxy`; `GET /v1/account` = 200) |
| Tarifa para el control de gasto | 0,25 / 2,00 USD por millón de tokens de entrada / salida (`A2` §2) |
| Reserva de peor caso | 0,05 USD por predicción |

## 4. Entradas y herramientas

| Fichero | sha256 |
|---|---|
| `evaluacion_final_k20/entrada/03_PERFILES_U01_U20.csv` | `584e2204ec8eb41ef95bb61b7fe6a6f6c90164a36fe920d1097dcae250ae2307` |
| `evaluacion_final_k20/entrada/04_ORACULO_TEORICO_HISTORICO.csv` | `f3bc863e4bbf0f31a1fc6175a5c3db4e4f0882f0b15c1b03b242aa0b4a177528` |
| `k20_adaptacion_real/entrada/J_PERFILES_INDEPENDIENTES.csv` | `2f793c9ffa86861b5eb3bdd4bd7de3223c8c1a32794f6a8bba9bcb8cac9687c7` |
| `k20_adaptacion_real/herramientas/generar_casos_independientes_J.py` | `aee0ae9a16ad883695062bd92292160f7d0b36e53bc07ebd3b6133d046255999` |
| `k20_adaptacion_real/herramientas/oraculo_k20.py` (oráculo v2) | `d0261691cd53b90888380b87d410009c13aae1156a54a555cfe9dae3de65e237` |
| `k20_adaptacion_real/herramientas/test_oraculo_k20.py` (pruebas del oráculo: OK) | `5f0e1f73b11b9121bfbeceeb09da0ad4cac44c4f7a4a24fe724155a58a933497` |
| `k20_adaptacion_real/herramientas/comparar_dev_r.py` | `773b9984ca8bcebf1d71743d67f2beeeb436bac69683039708779227b32dd468` |
| `src/test/.../orchestation/EvaluacionK20IT.java` (arnés) | `698188620adcafed7b523a3e2290eb615af592ce502fa911cef89a0f06da5641` |

H01–H16 (`294524d9…`) son evidencia de desarrollo y **no** forman parte de la evaluación.

## 5. Criterios (aprobados; `07` §3)

| Cohorte | Recorridos | Adaptación efectiva y pertinente (ACC + ALT, informadas por separado) | Seguridad |
|---|---|---|---|
| **U01–U20** | **20/20 PASS** en R y en R′ | R + R′ ≥ **18/20** sobre las factibles (10 por corrida), **ninguna corrida por debajo de 9/10** | **0** incumplimientos |
| **J01–J16** | **objetivo 16/16 PASS** | R + R′ ≥ **80 %** de las factibles (sonda de la misma corrida), **límite inferior de Wilson al 95 % ≥ 63 %**, **ninguna corrida por debajo del 75 %** | **0** incumplimientos |

**Cómo se informa:**
- **Cumplir un umbral no es alcanzar el 100 %**: se informa la cifra exacta.
- Las sesiones técnicamente no factibles (NF, CONS-J por imposibilidad) y las conservadas por causa no registrada se informan **aparte**, y no cuentan como éxito.

## 6. Procedimiento y comandos

Todas las corridas usan el **mismo SHA de congelación** y las **mismas entradas**. Cada corrida va a una carpeta **nueva** de `k20_adaptacion_real/evidencia/final/`; el arnés se niega a sobrescribir.

**Reglas de ejecución:**
- **Sin repeticiones selectivas.**
- Una corrida que falle por causas técnicas (contenedor, red), antes de que el oráculo la evalúe, se conserva y se repite completa **una sola vez**, con registro.

```
SHA=$(git rev-parse HEAD)            # el de congelación
N=docs/FitSense/short_paper_ieee/k20_adaptacion_real
J=$N/entrada/J_PERFILES_INDEPENDIENTES.csv
base="sh ./mvnw -o -B -ntp verify -Pintegration -DskipUTs=true -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false \
  -Dit.test=EvaluacionK20IT -Dfitsense.evaluacion.k20=true -Dfitsense.evaluacion.k20.commit=$SHA"

# D y D' (sin IA; autorizadas)
$base -Dfitsense.evaluacion.k20.modo=DETERMINISTA -Dfitsense.evaluacion.k20.salida=$N/evidencia/final/D_U
$base -Dfitsense.evaluacion.k20.modo=DETERMINISTA -Dfitsense.evaluacion.k20.perfiles=$J -Dfitsense.evaluacion.k20.salida=$N/evidencia/final/D_J
$base -Dfitsense.evaluacion.k20.modo=DETERMINISTA -Dfitsense.evaluacion.k20.salida=$N/evidencia/final/Dp_U
$base -Dfitsense.evaluacion.k20.modo=DETERMINISTA -Dfitsense.evaluacion.k20.perfiles=$J -Dfitsense.evaluacion.k20.salida=$N/evidencia/final/Dp_J

# R y R' (Replicate real; PENDIENTES DE AUTORIZACION). Para R' se cambia R_ por Rp_
ia="-Dfitsense.evaluacion.k20.modo=REPLICATE -Dfitsense.evaluacion.k20.autorizacion-replicate=SI \
  -Dfitsense.evaluacion.k20.credencial=proxy -Dfitsense.evaluacion.k20.prompt=v2.1 \
  -Dfitsense.evaluacion.k20.tarifa-entrada-usd-millon=0.25 -Dfitsense.evaluacion.k20.tarifa-salida-usd-millon=2.00"
$base $ia -Dfitsense.evaluacion.k20.max-llamadas=24 -Dfitsense.evaluacion.k20.max-usd=1.20 -Dfitsense.evaluacion.k20.salida=$N/evidencia/final/R_U
$base $ia -Dfitsense.evaluacion.k20.perfiles=$J -Dfitsense.evaluacion.k20.max-llamadas=48 -Dfitsense.evaluacion.k20.max-usd=2.40 -Dfitsense.evaluacion.k20.salida=$N/evidencia/final/R_J

# Oraculo v2 sobre cada carpeta
python3 -I $N/herramientas/oraculo_k20.py <carpeta_U> docs/FitSense/short_paper_ieee/evaluacion_final_k20/entrada/03_PERFILES_U01_U20.csv \
  docs/FitSense/short_paper_ieee/evaluacion_final_k20/entrada/04_ORACULO_TEORICO_HISTORICO.csv
python3 -I $N/herramientas/oraculo_k20.py <carpeta_J> $J
```

**Comprobaciones de cada corrida, antes del oráculo:**
- `00_configuracion.txt` registra el SHA de congelación, `prompt_adaptacion: FIX-PR-2.1-EXP` (R y R′), `hash_catalogo 425fc958…`, 46 migraciones y el sha256 de los perfiles (U `584e2204…`, J `2f793c9f…`).
- D = D′ por huella normalizada del oráculo, en U y en J.
- La huella de D sobre U debe ser `f9f67973…`, la de la evaluación congelada.

## 7. Presupuesto de R y R′ (para tu autorización)

| Corrida | Cohorte | Ciclos con IA | Predicciones máx. (3 por ciclo) | Tope USD |
|---|---|---:|---:|---:|
| R | U01–U20 | 8 | 24 | 1,20 |
| R | J01–J16 | ≤ 16 | 48 | 2,40 |
| R′ | U01–U20 | 8 | 24 | 1,20 |
| R′ | J01–J16 | ≤ 16 | 48 | 2,40 |
| **Total** | | | **144** | **7,20** |

**Gasto esperado:**
- **≈ 0,50–1,00 USD.**
- DEV-R costó 0,11 USD por corrida de U, con una predicción por ciclo y entre 0,007 y 0,022 USD por predicción.
- J tiene el doble de ciclos y algunos perfiles de gimnasio, cuyos prompts son de unos 55.000 tokens.

**Control:**
- el gasto real se mide por tokens tras cada predicción;
- la siguiente no se hace si el gasto medido más la reserva pasa del tope de la corrida;
- se bloquea si una predicción supera la reserva o no tiene métricas (`A2` §2).

La cifra oficial es la del panel de Replicate.
