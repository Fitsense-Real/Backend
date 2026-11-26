# 03 · Pruebas sin coste y comparación preliminar

Ninguna prueba llama a Replicate. Las salidas reales que se usan son las 51 **ya pagadas** de la evaluación de H2-v2; se leen y no se modifican.

## 1. Pruebas nuevas

| Prueba | Qué comprueba | Resultado |
|---|---|---|
| `H2v3RegresionesIT` (1): **G02, G03, G15, G18, G19** | **Reproduce** el rechazo: la salida real de PR-2.0, con el mismo perfil, semana y contexto, da exactamente los problemas registrados.<br>**Causa** en el texto de PR-2.0: «acércate a ella si cabe» y «baja series» o «también puedes bajar series» en el reintento.<br>**PR-2.1** no contiene «acercate», `objetivo_series_semana`, «baja series» ni «bajar series». Contiene las reglas 21 y 22 y la comprobación final, y `prioridad_series_semana` es exactamente `DosisSemanalPolicy` sin los ceros.<br>**Reintento** de PR-2.1 con esos mismos motivos: sin «bajar series»; con «NUNCA cambies la dosis» y las pistas de V24, V25, V15 o V23 que tocan.<br>**Coherencia:** aplicar PR-2.1 al pie de la letra deja la propuesta **sin ningún rechazo**.<br>**G03 con PR-1.7** (válida) cumple ya PR-2.1: 0 cambios | **5/5** |
| `H2v3RegresionesIT` (2): **los 26 perfiles** | **Cobertura:** los 4 objetivos, con 3, 4, 5 y 6 días, en casa y en gimnasio, con y sin restricciones.<br>**Texto de PR-2.1:** sin las frases conflictivas y con los cinco puntos de la jerarquía.<br>**Prioridad:** igual a la política.<br>**Contrato sin cambios:** el de V15 (`suggested_split`), los techos de V26, `no_cuentan`, `dosis_por_clase` y `capacidades` son **idénticos** a PR-2.0, que `ContratoH2v2IT` comprueba exactos: una unidad de más da el rechazo.<br>**Motor de reglas:** su rutina válida ya cumple la jerarquía, sin ningún cambio.<br>**Lector literal sobre las 51 salidas reales:** al aplicar PR-2.1, todas quedan válidas | Motor de reglas: **26/26 válidas y 26/26 sin cambios**.<br>Salidas reales: **51/51 válidas** tras PR-2.1, partiendo de 16 rechazadas |
| `H2v3PipelineSimuladoIT` (6): **pipeline real con PR-2.1 encendida**, Replicate simulado | **Respaldo:** salida inválida → 3 intentos → `RULE_ENGINE` con `model_name` nulo; la instantánea queda con PR-2.1 (5 y 6 días).<br>**Válida:** se publica como `AI` / `gpt-5-mini` en 1 predicción (5 y 6 días).<br>**V26 de más:** rechazo; el reintento dice «quita una colocación […] o llévala» y «NUNCA cambies la dosis»; acaba en respaldo bien registrado.<br>**O20:** el ejercicio con dolor no se ofrece y la propuesta que lo trae no se publica.<br>**Dosis recortada** (el patrón de G02: 3×10 pasa a 2×10): V24 la rechaza; el reintento pide copiar la dosis y cambiar colocaciones, nunca bajar series; acaba en respaldo.<br>**Sin distribución factible** (GENERAL_FITNESS intermedio, 30 min, 1 día): 0 predicciones, `RULE_ENGINE` | **6/6** |
| `ContratoRepartoTest` (versión) | `PR-2.1` se admite y `esH2v3` lo reconoce. `PR-2.2`, `pr-2.0` o `PR-1.9` hacen fallar | 3/3 |
| `H2v2PipelineCompletoIT`, modo `SIMULADO`, con `version-b=PR-2.1` | **Etapa 2 simulada** (G25, G26, G05 y G06):<br>• la IA publica en 1 predicción con los dos prompts;<br>• el respaldo llega tras 3;<br>• cada brazo envía su versión;<br>• lo publicado se revalida como válido, sin ejercicios fuera de `available_exercises`;<br>• hay una reserva por predicción (`evidencia/etapa2_simulado_PR21/`) | Correcto |

**El lector literal** (`AplicadorJerarquiaPR21`, solo en pruebas) aplica la regla 22 usando únicamente lo que la instantánea de PR-2.1 da al modelo:
1. copia la dosis exacta;
2. quita colocaciones que pasan un techo;
3. reparte con grupos de `body_parts_con_material` (respetando las reglas 18 y 20) o quita;
4. cubre el slot que falta cambiando un ejercicio del mismo grupo;
5. quita secundarias si se pasa de duración.

No es un generador ni predice al modelo. Comprueba que **seguir las instrucciones produce semanas que el validador acepta**, es decir, que ninguna obliga a romper otra en estos perfiles.

**Qué cambia en las cinco regresiones** (`evidencia/lector_literal_PR21.csv` y `evidencia/tras_PR21/`):

| Caso | Rechazo de PR-2.0 | Qué cambia al aplicar PR-2.1 | Después |
|---|---|---|---|
| G02 | V24 ×16, V25 | 16 dosis restauradas (2×10 → 3×10; 2×12 → 2×13 o 2×10) | Válida |
| G03 | V24 ×24, V25 | 24 dosis restauradas (3×8 → 3×10; 60 s → 90 s) | Válida |
| G15 | V15 ×2 | En las dos inferiores, una colocación de piernas pasa a `waist` (864) | Válida |
| G18 | V15 | En el inferior del 29/11, piernas → `waist` (864) | Válida |
| G19 | V23 | En la sesión del 25/11, un ejercicio de piernas → uno de LOWER_HIP (125) | Válida |

## 2. Pruebas que no cambian

| Prueba | Resultado |
|---|---|
| `ContratoH2v2IT` (PR-2.0, 26 perfiles) | 26/26 |
| `H2v2PipelineSimuladoIT` (PR-2.0) | 4/4 |
| Modo `PROMPTS`, `version-b=PR-2.0` | **PR-1.7 y PR-2.0 idénticos** a lo congelado en los 52 casos (caracteres y cota) |

## 3. Comparación preliminar sin coste

| | PR-1.7 | PR-2.0 | PR-2.1 |
|---|---|---|---|
| Comunica la mitad de V15 y los techos de V26 | No | Sí | Sí |
| La orientación por grupo, como cifra que alcanzar | «10 series» en los principios | Sí («acércate») | **No**: prioridad para elegir grupo |
| Ceros que parecen prohibiciones | — | Sí | **No** |
| El reintento invita a bajar series | «o las series» | Sí | **No** |
| Jerarquía explícita (dosis, techos, reparto, cobertura, duración) | No | No | **Sí** |
| Salidas reales que quedan válidas al seguir el texto al pie de la letra | — | — | **51/51** |
| Caracteres de entrada (mediana B) | — | ≈103.200 | ≈105.400 |

**Límite de esta comparación.** Demuestra que el texto ya no contiene las contradicciones conocidas y que sus instrucciones son satisfacibles. **No demuestra** que gpt-5-mini las siga: eso solo lo mide la evaluación real (`04`). **H2 no se declara resuelta.**

## 4. Regresión y CI

- `mvn verify -Pintegration` completo, local:
  - unitarias: **1.635, 0 fallos**;
  - integración: **927, 0 fallos**, 74 omitidas (las opcionales);
  - BUILD SUCCESS.
- CI de GitHub: ver la PR.
