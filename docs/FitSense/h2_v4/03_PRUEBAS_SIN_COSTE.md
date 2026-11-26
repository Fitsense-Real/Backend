# 03 · Pruebas sin coste

Ninguna prueba llama a Replicate. Se usan las **103 salidas reales ya pagadas**, que no se modifican:
- 51 de H2-v2 (PR-1.7 y PR-2.0, 25/11);
- 52 de H2-v3 (PR-1.7 y PR-2.1, 26/11).

Las salidas «corregidas» por el lector de pruebas **no son generaciones de la IA**: solo muestran que las instrucciones se pueden cumplir a la vez.

## 1. Pruebas nuevas

### 1.1 `H2v4RegresionesIT`

**Test 1: los ocho casos de las regresiones.**
- **Casos:** G17, G21 y G24 (salidas de PR-2.1); G02, G03, G15, G18 y G19 (salidas de PR-2.0).
- **Reproduce** el rechazo real, con los mismos problemas registrados.
- **El texto de PR-2.2:**
  - no contiene nada de esto: «acércate», `objetivo_series_semana`, «baja series», «bajar series», `complementos_no_cuentan`, `max_per_body_part_por_n_trabajo` ni `volumen_semanal.no_cuentan`;
  - sí contiene: «Solo cuentan los ejercicios de trabajo», `"cuenta": "complemento"`, `"regla_mitad": true`, «redondeando hacia abajo», «tenga o no prioridad», las reglas 21 y 22 con sus cinco puntos y la comprobación final.
- **El reintento de PR-2.2,** con los mismos motivos:
  - no dice «bajar series»;
  - dice «NUNCA cambies la dosis», «reglas 1 a 22» y las pistas que tocan (V15, V24, V25, V23).
- **Siguiendo PR-2.2 al pie de la letra,** la propuesta queda **sin ningún rechazo**.
- **En G21 y G24,** la sesión LOWER_BODY nombra `upper legs`, `lower legs` y `waist` en `body_parts_con_material`, con `min_body_parts = 2` y `regla_mitad`.
- **Resultado: 8/8.**

**Test 2: los 26 perfiles** (4 objetivos; 3, 4, 5 y 6 días; casa y gimnasio; con y sin restricciones).
- **Tamaño:** PR-2.2 es **más corta que PR-2.1 en los 26**.
- **Mismo contrato que PR-2.1:**
  - las marcas `complemento` y `sin_series` son exactamente las dos listas;
  - los ejercicios ofrecidos son los mismos, en el mismo orden;
  - `regla_mitad` aparece donde había tabla, y la tabla era `n / 2`;
  - las sesiones son iguales salvo eso;
  - son iguales los techos, la prioridad, las dosis, las capacidades, `slots_exigibles`, `max_session_seconds`, las colocaciones, los días, el descanso y `safety_notes`.
- **Motor de reglas:** su rutina válida no necesita ningún cambio.
- **Lector literal sobre las 103 salidas reales:** todas quedan válidas.
- **Resultado:**
  - texto, contrato y tamaño correctos en **26/26**;
  - motor de reglas: **26/26 válidas y 26/26 sin cambios**;
  - salidas reales: **103/103 válidas** tras aplicar PR-2.2, partiendo de 27 rechazadas (9 + 7 de H2-v2; 8 + 3 de H2-v3).

### 1.2 `H2v4PipelineSimuladoIT` (6): pipeline real con PR-2.2 encendida y Replicate simulado

| Caso | Resultado |
|---|---|
| Salida inválida | 3 intentos y respaldo `RULE_ENGINE` con `model_name` nulo; la instantanea queda con PR-2.2 (5 y 6 días) |
| Salida válida | Se publica como `AI` / `gpt-5-mini` en 1 predicción (5 y 6 días) |
| V26 de más | El reintento dice «quita una colocación […] o llévala […]; no cambies ninguna dosis» y acaba en respaldo |
| O20 | El ejercicio con dolor no se ofrece y la propuesta que lo trae no se publica |
| Dosis recortada (el patrón de G02) | V24 la rechaza y el reintento pide copiar la dosis |
| Sin distribución factible | 0 predicciones y `RULE_ENGINE` |

En todas las peticiones del brazo:
- **contiene:** PR-2.2, `"regla_mitad":true` y «Solo cuentan los ejercicios de trabajo»;
- **no contiene:** listas de ids, tabla, «acércate» ni «bajar series».

**Resultado: 6/6.**

### 1.3 Otras pruebas

| Prueba | Resultado |
|---|---|
| `ContratoRepartoTest` (versión): `PR-2.2` se admite; `esH2v4` es exclusivo; `PR-2.3` hace fallar | 3/3 |
| `H2v2PipelineCompletoIT` en modo `SIMULADO`, con `version-b=PR-2.2` (G25, G26, G21, G24) | La IA publica en 1 predicción con los dos prompts; el respaldo llega tras 3; lo publicado se revalida válido y sin ejercicios fuera de `available_exercises` (`evidencia/etapa2_simulado_PR22/`) |
| Modo `PROMPTS` (26 perfiles × PR-1.7, PR-2.0, PR-2.1 y PR-2.2) | **PR-1.7, PR-2.0 y PR-2.1 idénticas a sus históricos.** PR-2.2 con las cifras de `02` §2.3 |

## 2. Casos de la orden (§8)

| Caso | Qué se comprueba | Resultado |
|---|---|---|
| **G02, G03** | Dosis autorizadas, sin V24/V25 | Las salidas de PR-2.0 vuelven a dar V24/V25. El texto de PR-2.2 manda la dosis primero («copiada tal cual, nunca recortada»), sin cifras que alcanzar. Siguiéndolo, quedan válidas |
| **G15, G18** | Correcciones de reparto | Válidas siguiendo PR-2.2: se cambia una colocación de piernas por `waist` |
| **G19** | Cobertura LOWER_HIP (V23) | Válida siguiendo PR-2.2 |
| **G17** | La mitad | La regla dice «la mitad, redondeando hacia abajo», sin tabla. Siguiéndola, PUSH y LOWER_BODY quedan válidas |
| **G21, G24** | Mínimo de grupos en LOWER_BODY | La regla dice que cardio y movilidad no cubren grupo, y cada ejercicio trae su marca. Siguiéndola, quedan válidas |
| **26 perfiles** | Coherencia para los cuatro objetivos, niveles y días | Ver la tabla de `H2v4RegresionesIT` |
| Reintentos y respaldo | — | `H2v4PipelineSimuladoIT` |
| O20 | — | `H2v4PipelineSimuladoIT` y la suite de O20 |

Detalle de cada salida (rechazos antes y después, y cambios): `evidencia/lector_literal_PR22.csv` y `evidencia/tras_PR22/`.

## 3. Regresión completa

`mvn verify -Pintegration` completo, local:
- unitarias: **1.636, 0 fallos**;
- integración: **935, 0 fallos**, 74 omitidas;
- BUILD SUCCESS.

**Contratos de versiones anteriores**, en verde:
- `ContratoH2v2IT` (PR-2.0, 26/26);
- `H2v2PipelineSimuladoIT` (4/4);
- `H2v3RegresionesIT` (2/2);
- `H2v3PipelineSimuladoIT` (6/6).

**Resto de la suite**, incluida en la ejecución: generación inicial, reintentos, motor de reglas, O20, progresión, reincorporación, adaptación y configuración por defecto.

**Validadores y políticas:** el diff de `src/main` frente a PR-2.1 solo toca `PlanPromptBuilder`, `PlanInputSnapshot` y `application.properties` (`evidencia/diff_produccion_PR21_a_PR22.diff`).
