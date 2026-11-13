# Impacto en elegibilidad y generación (antes / después)

## 1. Rejilla de perfiles

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

## 2. Perfiles NO_FACTIBLE de la PR #55

Los dos perfiles LOSE_WEIGHT / principiante / casa («sin suelo» y «sin las tres») recuperan un plan **válido** cuando la contención C39 se reduce a los 8 ejercicios que siguen sin resolver. Ver `IMPACTO_PR55.md`; no se relajó V25 ni ninguna bandera.

## 3. Atribución por migración (rejilla de 168 perfiles)

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

## 4. Referencias de generación (72 perfiles)

`GeneracionReferenciaIT`, con 72 perfiles fijos:

| Rama | Semanas que cambian respecto a la anterior | Evidencia |
|---|---|---|
| Rama 1 (V47, V48) | 47 de 72: 16 por un ejercicio afectado directamente y 31 en cascada | `evidencia/DIFERENCIAS_REFERENCIA_V47_V48.md` |
| Rama 2 (V49) | 0 de 72: la referencia no tiene perfiles en los que el orden del 1093 importe | — |
| Rama 3 (V50) | 68 de 72: 25 directas y el resto en cascada | `evidencia/DIFERENCIAS_REFERENCIA_V50.md` |

En las tres ramas, todas las semanas siguen VALIDA.
