# MEJ · Mejora de la generación y la adaptación con IA · protocolo congelado

**Este documento se congela antes de la primera llamada pagada.** Nada de lo que aquí figura se cambia después de ver resultados.

**Rama:** `experimento/mejora-ia`, sobre `generation` tras la PR #42. El código de producción solo cambia en tres archivos de prompt y factibilidad, con interruptores **apagados por defecto**.

## 1. Encargo

Mejorar la generación y la adaptación de planes **sin añadir funciones, reglas ni validadores**. Solo cambia cómo se usa lo que ya existe: el texto y los datos de los prompts y su coherencia con los validadores del backend.

Objetivos elegidos por el responsable:
1. menos rechazos en la generación;
2. más dosis en los planes;
3. menos adaptaciones SIN_EFECTO;
4. mejor calidad de las adaptaciones.

## 2. Diagnóstico (gratuito, con las evidencias de GRA-R1 y GRA-R2)

### Rechazos en generación

13 de 38 primeras propuestas rechazadas (19 perfiles factibles × 2 corridas). Los hallazgos, en etapa NORMALIZADO:

| Código | Hallazgos |
|---|---:|
| V26 | 23 |
| V15 | 18 |
| V4 | 3 |
| V23 | 2 |
| V24 | 1 |
| V18 | 1 |

Casi todos están en sesiones LOWER_BODY que concentran *upper legs*.

| Causa | Evidencia |
|---|---|
| **V26:** los techos de series directas por grupo (6/12 en principiante; 10/20 en intermedio; 10/24 en avanzado; `DosisSemanalPolicy`) **no aparecen en el prompt** PR-1.7 | El texto de reglas 1–20 y el JSON no mencionan series directas ni techos. El modelo no puede respetar un límite que no conoce |
| **V15:** `max_per_body_part` viaja en `null` en todas las sesiones con PLAN-REV-2.0 | `PlanInputSnapshot.suggestedSession` solo lo calcula si hay `min_exercises_per_session`, y con PLAN-REV-2.0 siempre es `null`. El validador sí aplica la regla de la mitad. El prompt dice «no pongas más de max_per_body_part» con un valor nulo |

### Dosis baja

16 de 20 planes con DOSIS_LIMITADA en cada corrida de GRA. Los grupos que quedan por debajo:

| Grupo | GRA-R1 | GRA-R2 |
|---|---:|---:|
| Hombros | 14 | 14 |
| Espalda | 10 | 12 |
| Aeróbico | 10 | 10 |
| Pecho | 8 | 6 |

| Causa | Evidencia |
|---|---|
| El objetivo semanal con el que `CalidadDosis` decide DOSIS_LIMITADA (`objetivoSeriesSemana`, `objetivoAerobicoMinutosSemana`) **no llega al prompt** | Ningún campo del prompt lo contiene |

### SIN_EFECTO evitable (O18, GRA-R2)

La causa no es una mala respuesta del modelo, sino un **defecto de coherencia del prompt V2.1**:
- **El permiso existe.** ACORTAR_O_REORGANIZAR permite retirar UNA colocación de relleno (`TipoIntervencion.permiteQuitarEjercicio`) y V8C lo acepta.
- **La factibilidad lo ignora.** `FactibilidadAdaptacion` nunca consultaba ese permiso. En GAIN_MUSCLE, donde la dosis 4D no se puede reducir, el bloque decía «B: No sustituyas ni retires ejercicios», «D. OPCIONES: ninguna» y «F: OMÍTELA, es la respuesta correcta».
- **El modelo hizo lo que se le indicaba.** La sonda real confirmó que retirar el relleno era válido.

### Patrón nuevo ante dolor (O12, GRA-R2)

Fue válido, pero quedó pendiente de revisión profesional. El prompt no indica qué hacer ante un dolor sin localizar.

## 3. Variantes (sin reglas nuevas; apagadas por defecto)

| Variante | Propiedad | Qué añade |
|---|---|---|
| **PR-1.8** (generación) | `fitsense.ai.generation.prompt-version=pr-1.8` | Un bloque «COHERENCIA CON LOS VALIDADORES» antes de los datos: (a) la regla de la mitad de V15, válida aunque `max_per_body_part` sea `null`; (b) los techos de V26 del nivel; (c) la dosis orientativa de `CalidadDosis` por grupo grande con material y admitido por la división, más los minutos aeróbicos. Son los mismos números y las mismas fuentes que usa el backend. El resto del prompt es idéntico a PR-1.7 |
| **FIX-PR-2.2** (adaptación) | `fitsense.ai.adaptation.prompt-version=v2.2` | V2.1 más: (a) la factibilidad de ACORTAR lista los rellenos retirables con su efecto en duración (B, D, E y F coherentes con V8C); (b) dos indicaciones de pertinencia: ante dolor, sustituir primero por el mismo patrón y grupo; en ACORTAR, retirar un relleno es un cambio válido |

**Verificación gratuita:**
- **Pruebas:**
  - `MejoraIaPromptsTest` (4);
  - huellas que garantizan PR-1.7 y V2.1 idénticos byte a byte (`PromptHistoricoHuellaTest`, `ContratoV4CIATest`);
  - 69 de 69 en las pruebas de prompts.
- **Volcado real:** los prompts PR-1.7 de los 20 perfiles, generados en solo lectura con el contexto del backend, son idénticos a los que recibió GRA (comprobado en O02, O03, O07 y O13). PR-1.8 es PR-1.7 más el bloque.
- **Simulación V2.2:** los prompts de adaptación coinciden con los de R_O, salvo el texto de V2.2. Las 4 retiradas que ofrece V2.2 en O18 y O19 son válidas para los validadores reales (sonda ampliada a todos los rellenos: 2 de 2 y 2 de 2).

## 4. Medición pagada

| Corrida | Configuración | Comparación | Llamadas esperadas |
|---|---|---|---:|
| **MEJ-A1**, **MEJ-A2** | Generación determinista + adaptación **V2.2** con Replicate (bloques G y P) | **Pareada** con R_O y R′_O (generación determinista + V2.1, mismo código de producción, mismos ciclos y prompts idénticos salvo V2.2) | ≈ 10 cada una |
| **MEJ-G1**, **MEJ-G2** | Generación con IA **PR-1.8** + adaptación V2.2 (bloques G y P), como GRA | Con GRA-R1 y GRA-R2 (PR-1.7): mismos perfiles, mismo pipeline y la misma política | ≈ 30–45 cada una |

Mismo modelo y parámetros que GRA: `gpt-5-mini`, `medium`, `json_schema` y política PLAN-REV-2.0.

**Presupuesto:** bloque nuevo autorizado de **120 predicciones y USD 3,00**, aparte de las 168 anteriores.
- Control antes de cada predicción: llamadas + 1 ≤ tope y USD + 0,05 ≤ tope.
- Orden: MEJ-A1 → MEJ-A2 → MEJ-G1 (tope propio: la mitad del remanente) → MEJ-G2 (el resto).
- Sin repeticiones selectivas. Lo que no quepa no se ejecuta y se informa.

## 5. Métricas (numerador / denominador)

**Generación (19 perfiles factibles por corrida; O11 aparte):**
- **Principal:** primeras propuestas válidas, frente a 12/19 y 13/19 en GRA.
- **Secundarias:**
  - propuestas rechazadas y códigos (V15 y V26 sobre todo);
  - planes publicados desde la IA y planes de respaldo;
  - llamadas por perfil;
  - planes con DOSIS_LIMITADA tras la generación;
  - grupos por debajo del objetivo;
  - coste.
- **Medida pareada por perfil:** primera propuesta válida o no, con McNemar exacta.

**Adaptación (los 10 ciclos de cada corrida, pareados con R_O/R′_O):**
- adaptaciones publicadas desde la IA, rechazos, SIN_EFECTO (evitables según la sonda) y respaldos;
- pertinencia ACC/ALT/CONS-J/INC con el oráculo v1.4, con las mismas definiciones;
- cambios no autorizados (oráculo de `contar_baseline.py`).

## 6. Reglas de decisión (fijadas antes de ver resultados)

- **Adoptar PR-1.8 como recomendación** si se cumplen las tres condiciones:
  - las primeras propuestas válidas suben en las dos corridas, o en total sin bajar en ninguna;
  - no aparece ningún plan publicado por respaldo atribuible a la variante;
  - los planes con DOSIS_LIMITADA no aumentan.
- **Adoptar V2.2 como recomendación** si se cumplen las tres condiciones:
  - los SIN_EFECTO evitables no aumentan y, en los ciclos ACORTAR, el modelo usa la opción válida en vez de omitir;
  - no aumentan los rechazos ni los INC;
  - los cambios no autorizados siguen en 0.
- **Lo que no adoptaría:** con n = 19 y n = 10, una diferencia pequeña o mixta no basta para adoptar nada. Se informa como no concluyente y no se cambia el valor por defecto.
- **Alcance de la PR:** ningún interruptor se activa por defecto en esta rama. La adopción se propone en una PR aparte, con la decisión del responsable.

## 7. Enmienda E-1 (antes de las corridas de control; motivo: versión del modelo, no los resultados)

**El hecho.** Las corridas MEJ pasan por el cliente Java del backend, que llama al endpoint del modelo sin poder fijar versión. El registro `versiones.txt` muestra:

| Corridas | Versión |
|---|---|
| R_O, R′_O, GRA-R1, GRA-R2 | `9f4cd9ec…` |
| MEJ | `045f8102…` (vigente desde el 06/11 a las 18:46 UTC) |

Comparar MEJ con GRA o con R_O mezclaría el efecto del prompt con el cambio de versión del proveedor.

**La corrección.** Se sustituye la réplica **MEJ-G2** por dos **controles con la versión vigente** y los prompts anteriores, con la misma configuración:

| Control | Configuración | Compara con | Llamadas |
|---|---|---|---:|
| **MEJ-A0** | Generación determinista + **V2.1** | MEJ-A1 y MEJ-A2: pareada, misma versión | ≈ 10 |
| **MEJ-C** | Generación con IA **PR-1.7** + adaptación **V2.1** | MEJ-G1: misma versión, mismos perfiles | ≈ 35–40 |

**Lo que no cambia:** el presupuesto (120 / USD 3,00), las métricas y las reglas de decisión. Las comparaciones con GRA y R_O se siguen informando, señaladas como «distinta versión del modelo».

**Lo que ya se había visto al decidir E-1:** el resultado agregado de MEJ-A1/A2 (12 de 12 adaptaciones aplicadas en ambas) y que MEJ-G1 estaba en curso. La enmienda no excluye ni repite ninguna corrida.

## 8. Incidencia I-MEJ-1 (proveedor) en MEJ-G1, bloque G

**Qué pasó:**
- O01 y O02 respondieron (79 s y 51 s).
- La generación de **O03** superó el límite de sondeo de 150 s (176 s en total; prompt de 193 801 caracteres). La predicción se canceló (`CANCELED`).
- La regla de control del arnés («tiempo agotado con una predicción en curso: no se hacen más llamadas») bloqueó el resto del bloque. De O04 a O14, los perfiles no llamaron al modelo y recibieron el plan de respaldo (`PRESUPUESTO_AGOTADO_SIN_LLAMADA`), que **no cuenta como resultado de la IA**.
- El bloque P se completó (11 llamadas). O16 respondió en 167 s y O18/ciclo 1 tuvo un error del proveedor.

**Decisión:**
- **Perfiles nunca ejecutados (no es una repetición):** se ejecutan, en una corrida del bloque G limitada a ellos (**MEJ-G1C**: O04–O10 y O12–O14).
- **O03:** recibe **un** reintento por fallo del proveedor, la misma regla de la línea base, en una corrida aparte y al final (**MEJ-G1R**), para que un nuevo timeout no bloquee a otros perfiles.
- **Lo que no cambia:** el límite de 150 s (el de producción). Ningún perfil con respuesta se repite.
- **Cómputo de G1:** O01, O02 y O15–O20 de MEJ-G1, más G1C y G1R.
- **Corridas de control:** se aplica lo mismo si sufren la misma incidencia.
