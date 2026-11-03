# Protocolo de la cohorte complementaria R01–R08

**Estado:**
- **Preparado y congelado.**
- **La fase con Replicate se ejecutó** después de tu aprobación de la opción A (anexos 1 y 2). Las secciones 1–5 se conservan tal como se congelaron.
- Las fases sin coste (perfiles, ejecución determinista y GUION, continuidad de dos semanas) **ya se ejecutaron** con estos mismos perfiles (`11`).

## 1. Perfiles (congelados antes de ejecutar)

`10_PERFILES_SINTETICOS_R01_R08.csv`, con *commit* `7af7c0f`, anterior a cualquier ejecución sobre ellos. Usa el mismo formato que U01–U20, más `secuencia_semana2`, `motivo_omision_*_semana2` y `caso_que_cubre`.

| ID | Objetivo | Nivel | Lugar, minutos y días | Equipamiento | Semana 1 | Semana 2 | Qué aporta |
|---|---|---|---|---|---|---|---|
| R01 | GM | Principiante | Casa, 45 min, L-X-V | peso corporal, mancuerna, banda | H/O/O (dificultad, fatiga) | Igual | GM en casa: variantes limitadas; fatiga en GM (F3 esperado) |
| R02 | GM | Intermedio | Gimnasio, 75 min, M-J-S | gimnasio completo + barra de dominadas | H/H/O (fatiga) | Igual | Duración larga en GM |
| R03 | FU | Principiante | Casa, 30 min, L-X-V | peso corporal, banda | H/O/O (dificultad, desmotivación) | Igual | Mínimo bajo y equipo muy limitado |
| R04 | FU | Intermedio | Gimnasio, 45 min, M-J-S | gimnasio completo | H/H/O (fatiga) | H/O/O (dificultad, fatiga) | Margen pequeño; causa distinta en la semana 2 |
| R05 | PP | Principiante | Gimnasio, 60 min, L-X-V | gimnasio completo | H/O/O (dificultad, desmotivación) | Igual | Muchas variantes |
| R06 | PP | Intermedio | Casa, 30 min, M-J-S | solo peso corporal | H/P40/N (registro incompleto) | H/H/O (fatiga) | C = 66,7, Aw ≠ Aobs, causa no registrada; IA solo en la semana 2 |
| R07 | AG | Principiante | Casa, 60 min, L-X-V | peso corporal, mancuerna, kettlebell, fitball | H/H/O (desmotivación) | Igual | Desmotivación con más margen |
| R08 | AG | Intermedio | Gimnasio, 45 min, M-J-S | gimnasio completo | N/O/H (sin registro + dificultad) | Igual | Aw 33,3 frente a Aobs 50; dificultad con variantes |

**Sin restricciones de seguridad declaradas.** Política PLAN-HIST-1, progresión desactivada y semillas `userId × 1.000.003 + epochDay`.

## 2. Resultados sin Replicate (ya obtenidos)

Ver `11` y `06`. Resumen:
- R02 es **NO_FACTIBLE** en la generación inicial (H6).
- R06 no llama a la IA en la semana 1 (causa no registrada).
- **Operaciones elegibles para IA:**
  - semana 1: R01, R03, R04, R05, R07 y R08 (**6**);
  - semana 2: R01, R03, R04, R05, R06, R07 y R08 (**7**), según lo que salga de la semana 1.

## 3. Comparación C1 frente a C2 propuesta

**Iguales en las dos condiciones** (como en `experimento_prompt_v2/07`):
- modelo `gpt-5-mini`, esfuerzo `medium`;
- salida `FIX-1.0`;
- 3 intentos y `prefer-wait` 25;
- mismos validadores, política, catálogo, perfiles, semanas y registros;
- base efímera nueva por corrida;
- `gemelos=false`, `semanas=2`.

**Única diferencia:** `prompt=v1` o `prompt=v2`.

**Opciones de diseño** (las dos caben en el límite de la orden: USD 3 y 60 predicciones):

| | Opción A (recomendada) | Opción B |
|---|---|---|
| Corridas | C1 r1 → C2 r1 | C1 r1 → C2 r1 → C2 r2 → C1 r2 |
| Semanas | 2 (las dos con IA) | 1 |
| Operaciones por corrida | 6 + 7 = 13 | 6 |
| Peor caso (3 intentos) | 39 por corrida | 18 por corrida |
| Límite por corrida | 30 predicciones, USD 1,50 (supuesto 0,05/predicción) | 15 predicciones, USD 0,75 |
| Total máximo | **60 predicciones, USD 3,00** | **60 predicciones, USD 3,00** |
| Previsto (con los datos de `experimento_prompt_v2`) | C2 ≈ 13 y C1 ≈ 20–26 predicciones; unos US$ 0,8–1,2 | C2 ≈ 6 y C1 ≈ 10–12 por corrida; unos US$ 0,8–1,1 |
| Aporta | **Continuidad con IA real** en perfiles nuevos | **Replicación** (2 corridas por condición) |
| Riesgo | Una sola corrida por condición. Si C1 llega al límite de 30, sus últimos casos quedan sin llamada (se registra) | Sin segunda semana con IA |

**Coste por predicción:** en la cohorte R los prompts de gimnasio miden ~227.000 caracteres (unos 65.000 *tokens*), así que costarán unos US$ 0,02–0,03. El supuesto de 0,05 es conservador.

## 4. Métricas y clasificación (fijadas antes)

**Se registra:**
- adaptaciones elegibles y aplicadas;
- sesiones modificadas;
- rechazos y su motivo;
- reintentos;
- SIN_EFECTO, RESPALDO y NO_FACTIBLE;
- publicaciones inválidas y cambios fuera de alcance;
- latencia y coste.

**Además se aplica a cada adaptación:**
- la auditoría de sustituciones (`02`/`03`, mismo script);
- la seguridad de las dos semanas (`evidencia/seguridad.py`).

**No se fija ninguna tasa mínima de éxito para C2.** Las violaciones de seguridad deben ser 0. Los cambios Q2/Q3 se informan, no se ocultan.

## 5. Normas

- Verificar antes: autenticación (`GET /v1/account` = 200), modelo, configuración y base local.
- Sin repeticiones selectivas: una corrida que falle por causas técnicas se conserva y se repite una sola vez, registrada.
- El prompt no se cambia entre corridas.
- Toda predicción iniciada cuenta. Al llegar al límite, el laboratorio deja de llamar.

**Comando** (se ejecuta solo con tu aprobación; `<v>` es `v1` o `v2`, y `<N>` es 30 en la opción A o 15 en la B):

```
mvnw -o -Pintegration verify -Dtest=none -Dit.test=ExperimentoKora20IT \
  -Dfitsense.experimento.kora20=true -Dfitsense.experimento.kora20.modo=REPLICATE \
  -Dfitsense.experimento.kora20.credencial=proxy -Dfitsense.experimento.kora20.prefer-wait=25 \
  -Dfitsense.experimento.kora20.prompt=<v> -Dfitsense.experimento.kora20.gemelos=false \
  -Dfitsense.experimento.kora20.perfiles=docs/.../10_PERFILES_SINTETICOS_R01_R08.csv \
  -Dfitsense.experimento.kora20.semanas=2 -Dfitsense.experimento.kora20.max-llamadas=<N> \
  -Dfitsense.experimento.kora20.max-usd=<1.50|0.75> -Dfitsense.experimento.kora20.coste-por-llamada=0.05 \
  -Dfitsense.experimento.kora20.salida=docs/.../evidencia/<corrida>
```

---

## Anexo 1 (03/11/2026): aprobación y congelación de la ejecución

**El responsable aprobó la opción A** («A»), antes de cualquier llamada.

**Se ejecutarán, en este orden, sin repeticiones selectivas:**
1. **RC1_r1:** `prompt=v1`, 2 semanas, `max-llamadas=30`, `max-usd=1.50`, `coste-por-llamada=0.05`;
2. **RC2_r1:** `prompt=v2`, con los mismos parámetros.

**Límites:**
- total: 60 predicciones y USD 3,00 estimados;
- el código evaluado es el de este *commit* (sin cambios en `src/main` respecto a `51c145b`);
- carpetas: `evidencia/RC1_r1` y `evidencia/RC2_r1`.

## Anexo 2 (03/11/2026): ejecución

**Se ejecutó lo aprobado, en el orden fijado, sin repeticiones y sin cambiar el prompt.**

| | RC1_r1 (C1, `v1`) | RC2_r1 (C2, `v2`) |
|---|---:|---:|
| Verificación previa | `GET /v1/account` = 200 (proxy), base efímera nueva, catálogo V44_2 (`00_configuracion.txt`) | Ídem |
| *Commit* ejecutado | `52f8bb4` | `597dbaa` (solo añade la evidencia de RC1; `src/main` sin cambios) |
| Predicciones | 22 de 30 | 13 de 30 |
| Estado de las predicciones | 22 `succeeded` | 13 `succeeded` |
| *Tokens* de entrada / salida | 741.080 / 72.097 | 410.482 / 36.149 |
| Coste (a 0,25 y 2,00 US$ por millón) | **US$ 0,3295** | **US$ 0,1749** |
| Coste por predicción | US$ 0,015 | US$ 0,013 |
| Latencia media por predicción (cliente) | 32,7 s | 28,3 s |
| Límite alcanzado | No | No |
| Fallos técnicos o repeticiones | 0 | 0 |
| Incidencias | 1: R02 NO_FACTIBLE en la generación (ya prevista, H6) | Ídem |

**Total de la fase:** 35 predicciones y **US$ 0,50**, frente a un máximo aprobado de 60 y USD 3,00.

**El supuesto de 0,05 por predicción era conservador**: el coste real fue de unos 0,014 por predicción (cada predicción de la cohorte R usó entre 8.437 y 68.186 *tokens* de entrada).

**Resultados:** `12`, `13`, `14` §4 y `16`. **Evidencia:** `evidencia/RC1_r1`, `evidencia/RC2_r1` (cada predicción en JSON, en `predicciones_replicate/`).
