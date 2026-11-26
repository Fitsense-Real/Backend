# 02 · PR-2.2: optimización y corrección de V15

## 1. Activación

`fitsense.ai.generacion.prompt-version=PR-2.2`. Desactivada. Por omisión sigue `PR-1.7`.

`PR-1.7`, `PR-2.0` y `PR-2.1` **no cambian**: la prueba lo confirma en los 26 perfiles, carácter a carácter y en cota, frente a sus históricos. La adaptación tiene su propia propiedad (`fitsense.ai.adaptation.prompt-version`) y no lee esta.

## 2. Qué cambia sobre PR-2.1

**Se conserva** la jerarquía funcional (dosis, techos, reparto, cobertura, duración) y todo el contrato de PR-2.1:
- los techos de V26 y `prioridad_series_semana`;
- `body_parts_con_material` y `min_body_parts`;
- las dosis (`dosis_por_clase` o `capacidades`), `slots_exigibles`, `max_session_seconds` y el resto de `constraints`;
- los mismos ejercicios ofrecidos, en el mismo orden;
- el reintento sin «bajar series» y su forma de reponer.

### 2.1 Instantánea: menos cruces

| Dato | PR-2.1 | PR-2.2 |
|---|---|---|
| Lo que V15 y V26 no cuentan (cardio y movilidad) | `constraints.complementos_no_cuentan`: lista de 38–49 ids que cruzar con el catálogo | **`"cuenta": "complemento"`** en el propio ejercicio |
| Lo que V26 no cuenta como series directas | `volumen_semanal.no_cuentan`: otra lista de ids | **`"cuenta": "sin_series"`** en el propio ejercicio |
| La mitad de V15 | `max_per_body_part_por_n_trabajo`: una tabla por sesión | **`"regla_mitad": true`** en la sesión y la regla en el texto: con 4 o más de trabajo, como mucho la mitad redondeando hacia abajo |

**Es el mismo contrato.** La prueba compara en los 26 perfiles que:
- las marcas son exactamente las listas;
- `regla_mitad` aparece justo donde había tabla;
- cada valor de la tabla era `n / 2`, que es la regla exacta de `ContratoReparto.topeMitad`;
- todo lo demás de la instantánea es idéntico.

### 2.2 Texto: reglas 15, 21 y 22 y comprobación sin repeticiones

| Pieza | PR-2.1 | PR-2.2 |
|---|---|---|
| Regla 15 (V15) | Tabla, recuento con la lista y el mínimo de grupos en frases separadas | Una pieza: «Solo cuentan los ejercicios de trabajo: los que traen `"cuenta": "complemento"` (cardio y movilidad) no cubren ningún grupo […]. Si la sesión trae `"regla_mitad": true` y tiene 4 o más ejercicios de trabajo, ningún body_part pasa de la mitad, redondeando hacia abajo (4 o 5: 2; 6 o 7: 3; 8: 4). Si un grupo llega a su tope, sigue con otro de `body_parts_con_material`, tenga o no prioridad» |
| Regla 21 (V26) | Techos, ejemplo, prioridad y principios (~1.000 caracteres) | Techos, «se cumple eligiendo cuántas colocaciones, nunca cambiando una dosis», la prioridad solo para elegir grupo y no prohíbe ninguno, y que los principios no se aplican |
| Regla 22 (jerarquía) | 5 puntos de 2–3 líneas cada uno | **Los mismos 5**, en una línea cada uno: 1) dosis exacta (V24), nunca recortada; 2) series por grupo: quitar o mover colocaciones; 3) reparto: cambiar la que sobra por otra de un grupo con material; 4) cobertura (V23, V25): no quitar la última de un slot ni de una capacidad; 5) duración: quitar una secundaria. Luego, «no inventes dosis, grupos ni ejercicios» |
| Comprobación final | 5 puntos numerados (~900 caracteres) | **Un párrafo** que remite a las reglas 15, 21 y 22 en lugar de repetirlas |
| Pistas del reintento | V15 con la tabla, V26, V24, V25, V23 | Las mismas, más cortas; la de V15 dice «cuenta solo los ejercicios de trabajo (sin `"cuenta": "complemento"`)» |

**Restricciones que siguen comunicadas** (orden §4):
- dosis exactas o rangos autorizados (22.1);
- límites de series (21);
- distribución muscular (15);
- cobertura de patrones y capacidades (22.4);
- equipamiento: `available_exercises` ya filtrado, y la regla 1;
- duración (regla 4 y 22.5);
- seguridad: O20 y precauciones, ya filtradas en `available_exercises`, y `safety_notes`;
- corrección ante rechazos (reintento).

### 2.3 Tamaño

`evidencia/tamanos_prompt_26_perfiles_PR17_PR20_PR21_PR22.csv`, caracteres de prompt e instrucción de sistema del brazo B:

| Versión | Mediana | Frente a PR-1.7 |
|---|---:|---:|
| PR-1.7 | 100.969 | — |
| PR-2.0 | 103.179 | +2.210 |
| PR-2.1 | 105.617 | +4.648 |
| **PR-2.2** | **102.876** | **+1.907** |

- **PR-2.2 frente a PR-2.1:** **entre 1.902 y 2.950 caracteres menos en cada uno de los 26 perfiles**. Recorta más de la mitad de lo que PR-2.1 añadía sobre PR-1.7.
- **PR-2.2 frente a PR-2.0:** más corta.
- **Cota de la etapa 1:** USD 3,1154 (PR-2.1: 3,1327).

**Un tamaño menor no demuestra un tiempo menor** (`01` §2): el tamaño de la entrada explica poco de la latencia. Lo que PR-2.2 busca reducir es el **cálculo**: cruces de listas y consultas de tabla. Solo la evaluación real puede decir si baja `predict_time`.

## 3. Archivos de producción

Diff respecto de PR-2.1: `evidencia/diff_produccion_PR21_a_PR22.diff`.

| Archivo | Cambio |
|---|---|
| `PlanPromptBuilder` | `VERSION_H2V4 = "PR-2.2"`, `esH2v4`, `h2v4()`; la clase `H2v4` con la regla 15, las reglas 21 y 22, la comprobación y las pistas. `aplicar` y `comprobacionReparto` reciben la variante (2, 3 o 4); los métodos anteriores siguen y delegan |
| `PlanInputSnapshot` | `AvailableExercise.cuenta`, `SuggestedSession.regla_mitad` y `VolumenSemanal.no_cuentan` como opcional. Todos `NON_NULL` y solo con PR-2.2: las otras versiones serializan exactamente lo de antes |
| `application.properties` | Comentario de los valores admitidos; por omisión sigue `PR-1.7` |

**Sin cambios:**
- **validadores:** V8C, V15, V20, V23, V24, V25 y V26;
- **políticas:** la de generación y la de adaptación, la progresión y la reincorporación;
- **seguridad:** O20 y sus equivalencias;
- **resto:** V65, Firebase, el transporte de gpt-5-mini, el catálogo, los perfiles y las salidas históricas.
