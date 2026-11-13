# Impacto del cierre en la elegibilidad

Compara el catálogo de la base del cierre (V50 con la contención reducida de #55, commit 57075e7) con el catálogo final (V51 + V52 + V53).
- **Rejilla:** la misma de 168 perfiles: 4 objetivos × 3 niveles × gimnasio/casa × 7 combinaciones de restricciones (libre, sin colgarse, sin brazos arriba, sin suelo, sin rodillas, sin impacto, sin todo).
- **Generación:** motor de reglas, sin IA, misma semana y misma semilla (`CatalogoIntegralComparacionIT`).
- **Evidencia:** `cierre/impacto/antes_V50.csv`, `despues_V53.csv` (elegibles y plan por perfil) y `resumen_168.tsv`.

## 1. Desenlace

| Medida | Resultado |
|---|---|
| Perfiles con plan | 168 → **168**. Ninguno gana ni pierde el plan |
| Semanas de la base con algún ejercicio que el catálogo final saca del conjunto elegible de ese perfil | **58** (44 de principiante, 6 de intermedio y 8 de avanzado) |
| Semanas finales con algún ejercicio no elegible | **0**. Se comprobó plan a plan |
| Planes cuyo contenido cambia | 168 |

Los ejercicios que más aparecían en esas 58 semanas eran 510 (9 semanas), 318 y 1205 (7), y 1115, 1111, 1180 y 777 (5). Casi todos son subidas de dificultad 1→2 aceptadas por la V3, que los saca de las semanas de principiante. 777 sale además por V53 en «sin colgarse».

## 2. Conjunto elegible

Media de elegibles por perfil y de ejercicios por semana:

| Objetivo | Elegibles antes | Elegibles después | Ejercicios por semana antes | Ejercicios por semana después |
|---|---|---|---|---|
| LOSE_WEIGHT | 422,0 | 414,4 | 18,74 | 18,79 |
| GAIN_MUSCLE | 410,9 | 403,1 | 23,29 | 23,26 |
| INCREASE_STRENGTH | 410,9 | 403,1 | 16,74 | 16,79 |
| GENERAL_FITNESS | 447,7 | 439,4 | 25,50 | 25,50 |

Cambio del conjunto elegible por restricción (mínimo / mediana / máximo):

| Restricción | Cambio | Lectura |
|---|---|---|
| Libre | −46 / −2 / 0 | Subidas de dificultad y los dos nuevos estiramientos (312 y 1194) |
| Sin colgarse | −30 / −7,5 / 0 | Además, V53 excluye los 16 de apoyo suspendido |
| Sin brazos arriba | −35 / −4,5 / +1 | 14 OH añadidas y 12 retiradas |
| Sin suelo | −25 / +1 / +3 | Los FL retirados (296) devuelven ejercicios |
| Sin rodillas | −43 / −1 / +2 | 5 KN retirados (por ejemplo 253) |
| Sin impacto | −46 / −1,5 / +4 | 675, 952, 262, 267 y 1069 dejan de ser alto impacto |
| Sin todo | −12 / −3,5 / +3 | — |

Cambio por nivel:
- Principiante: −46 / −13 / +1.
- Intermedio: −8 / −2 / +2.
- Avanzado: −12 / 0 / +4.

## 3. Qué sale y qué entra

| Sale del conjunto elegible | Perfiles | Motivo |
|---|---|---|
| 312 y 1194 | 90 cada uno | Pasan a estiramiento explícito y salen del trabajo principal |
| 1205 | 56 | Sube a nivel 2 |
| 1184, 777 y 462 | 44 cada uno | Suben a nivel 2 y, con V53, salen de «sin colgarse» |
| 318, 510, 1111, 508, 1058, 1115, 1262, 1305, 901 y 1180 | 40 cada uno | Suben a nivel 2 |

| Entra | Perfiles | Motivo |
|---|---|---|
| 773 y 455 | 126 cada uno | Dejan de ser estiramientos: el GIF muestra trabajo dinámico |
| 574, 108 y 503 | 24 cada uno | Pierden una OH que el GIF no muestra |
| 296 | 24 | Pierde FL |
| 797 y 481 | 16 cada uno | Pierden OH |
| 675 | 16 | Deja de ser alto impacto (sentadilla sumo con mancuerna) |
| 253 | 12 | Pierde KN |

**1113** no sale: su subida se vetó por la decisión previa de V39 (ver `REVISION_PROFESIONAL.md`).

## 4. Perfiles sensibles

Son los dos perfiles que antes eran NO_FACTIBLE con la contención de 26 ejercicios de #55, ambos LOSE_WEIGHT, principiante, en casa:

| Perfil | Elegibles antes | Elegibles después | Desenlace |
|---|---|---|---|
| Sin suelo | 92 | 93 | PLAN → PLAN |
| Sin todo («sin las tres») | 66 | 67 | PLAN → PLAN |

Los dos siguen teniendo plan. No se relajó ninguna regla ni se liberó ningún ejercicio contenido para conseguirlo.

## 5. Sin V53 (PR A sola)

La rejilla se ejecutó también sobre la PR A sola (V51 + V52). El resultado está en `cierre/impacto/despues_V52_sin_V53.csv`.
- **168 de 168 perfiles con plan.**
- Solo cambian 16 perfiles: los 8 «sin colgarse» y los 8 «sin todo».
- La única diferencia es que siguen elegibles 10 ejercicios de apoyo suspendido: 457, 458, 459, 920, 934, 1317 y 1335 en 8 perfiles cada uno, y 462, 777 y 1184 en 16 perfiles cada uno.
- Los otros 6 marcados por V53 no aparecen en esa diferencia: 1153, 1301, 1303, 1304 y 1306 siguen fuera por la contención INC-CAT-C39, y 1069 tampoco entra en esos perfiles.

La referencia de generación (`GeneracionReferenciaIT`, 72 planes) es idéntica con y sin V53, porque no usa la restricción de colgarse.

---

## Anexo A. Impacto anterior de V47–V50 (PR #56–#59), frente a `generation`

### A.1 Rejilla de perfiles

`CatalogoIntegralComparacionIT` (opt-in) se ejecutó sobre `generation` (antes) y sobre la rama final (después: V47–V50) con la misma rejilla y la misma semilla.
- **Rejilla:** 168 perfiles = 4 objetivos × 3 niveles × gimnasio/casa × 7 combinaciones de restricciones (libre, sin colgarse, sin brazos arriba, sin suelo, sin rodillas, sin impacto, sin todo).
- **Condiciones:** 4 días y 45 minutos.
- **Generación:** motor de reglas, sin IA.
- **Evidencia:** `evidencia/cmp_antes.csv` y `evidencia/cmp_despues.csv`.

| Resultado | Perfiles |
|---|---|
| Desenlace distinto (gana o pierde el plan) | **0** |
| Perfiles NO_FACTIBLE nuevos | **0** |
| Semanas de **antes** con un ejercicio que el catálogo corregido demuestra incompatible con el perfil (bandera o nivel) | **29** |
| Semanas de **después** con alguno | **0** |
| Semana distinta (ejercicios u orden) | 168: cambia el conjunto elegible o, en 8 perfiles con el mismo conjunto, el orden por impacto o por patrón (ver §3) |

Ejemplos de las 29 semanas de antes:
- la flexión pliométrica 460 para un perfil «sin impacto»;
- la dominada gorila 862 o la dominada 1108 para un principiante;
- la flexión con caída 496 para un principiante «sin colgarse».

### Conjunto elegible por objetivo (media por perfil)

| Objetivo | Antes | Después | Desenlaces cambiados |
|---|---|---|---|
| LOSE_WEIGHT | 428,7 | 423,0 | 0 |
| GAIN_MUSCLE | 417,6 | 411,8 | 0 |
| INCREASE_STRENGTH | 417,6 | 411,8 | 0 |
| GENERAL_FITNESS | 453,6 | 448,7 | 0 |

**Volumen:** series totales medias por semana 55,4 → 55,2 y ejercicios por semana 21,07 → 21,05. La dosis no cambia; ninguna migración la toca.

### Qué sale del conjunto elegible y por qué

Ejercicios que salen y número de perfiles afectados:

| Motivo | Ejercicios (perfiles) |
|---|---|
| Pasa a estiramiento por tiempo (V50) | 1254 (126) |
| Ahora exigen el implemento que faltaba (V50) | 1201 banco (80), 496 bancos (72), 1196 banco (48), 1195 estación (48) |
| Alto impacto añadido (V47) | 460 (56) |
| Nivel corregido por V48 | 880, 915, 916 (40); 1108, 874, 464, 868, 862, 1191 (32) |
| Brazos arriba o rodilla (V47) | 1300, 497, 503, 594, 1005 (24); 474, 696 (16); 995 (12); 895, 206 (8); 66 (6); 275 (4) |

Ningún ejercicio entra en un conjunto en el que antes no estuviera: V47–V50 solo restringen o corrigen.

### 2. Perfiles NO_FACTIBLE de la PR #55

Los dos perfiles LOSE_WEIGHT / principiante / casa («sin suelo» y «sin las tres») recuperan un plan **válido** cuando la contención C39 se reduce a los 8 ejercicios que siguen sin resolver. Ver `IMPACTO_PR55.md`; no se relajó V25 ni ninguna bandera.

### 3. Atribución por migración (rejilla de 168 perfiles)

La rejilla se ejecutó también sobre la rama 1 (V47–V48) y la rama 2 (V47–V49) para separar el efecto de cada migración (`evidencia/cmp_r1.csv`, `evidencia/cmp_r2.csv`).

| Paso | Semana idéntica | Mismo conjunto elegible, semana distinta | Conjunto elegible distinto | Desenlace cambiado |
|---|---|---|---|---|
| antes → rama 1 (V47, V48) | 45 | 11 | 112 | 0 |
| rama 1 → rama 2 (V49) | 84 | **84** | **0** | 0 |
| rama 2 → final (V50) | 0 | 28 | 140 | 0 |
| antes → final | 0 | 8 | 160 | 0 |

**V49 no cambia ningún conjunto elegible.** Las 84 semanas que cambian son exactamente los 84 perfiles LOSE_WEIGHT y GENERAL_FITNESS (los que tienen cardio) en los que el 1093 es elegible. Al quitar «correr» del nombre, el impacto por nombre del 1093 pasa de MODERADO a BAJO. Es la única excepción documentada a «reglas por nombre intactas» (`CatalogoIntegralTextosIT.reglasPorNombreIntactas`).

`CapabilityWeekBuilder.rangoImpacto` reordena entonces los huecos de cardio:
- En las 84, las posiciones y el número de huecos de cardio por día son los mismos.
- En 5, el multiconjunto de ejercicios es idéntico y solo cambia el orden.
- En 28, la parte de fuerza es idéntica.
- En el resto, la secuencia pseudoaleatoria determinista (semilla por usuario y semana) se desplaza y cambia también la elección de fuerza.

Los validadores aceptan todas las semanas y ningún desenlace cambia.

En V47–V48 (11 perfiles) y V50 (28 perfiles) los cambios con el mismo conjunto elegible vienen de datos que el motor usa para ordenar o asignar huecos:
- impacto, por la bandera HI de V47;
- patrón y grupo, por V50 (huecos de 4C y V15).

Es el mismo mecanismo determinista. Son cambios esperados por datos corregidos.

### 4. Referencias de generación (72 perfiles)

`GeneracionReferenciaIT`, con 72 perfiles fijos:

| Rama | Semanas que cambian respecto a la anterior | Evidencia |
|---|---|---|
| Rama 1 (V47, V48) | 47 de 72: 16 por un ejercicio afectado directamente y 31 en cascada | `evidencia/DIFERENCIAS_REFERENCIA_V47_V48.md` |
| Rama 2 (V49) | 0 de 72: la referencia no tiene perfiles en los que el orden del 1093 importe | — |
| Rama 3 (V50) | 68 de 72: 25 directas y el resto en cascada | `evidencia/DIFERENCIAS_REFERENCIA_V50.md` |

En las tres ramas, todas las semanas siguen VALIDA.
