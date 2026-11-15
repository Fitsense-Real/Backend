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

## 3. Por qué cambian semanas con el mismo conjunto elegible

En 8 perfiles el conjunto elegible es idéntico y la semana cambia. Todos contienen:
- el 1093, que pierde «correr» en el nombre (V49), con lo que su impacto por nombre pasa de MODERADO a BAJO;
- algún ejercicio reclasificado por V50.

Tanto `CapabilityWeekBuilder.rangoImpacto` (orden por impacto) como la asignación por patrón (huecos de 4C, V15) usan esos datos, así que la secuencia pseudoaleatoria determinista se desplaza. Es un cambio esperado por datos corregidos: los validadores aceptan las 168 semanas.

## 4. Referencias de generación (72 perfiles)

Ver `evidencia/DIFERENCIAS_REFERENCIA_V47_V48.md` (rama 1) y sus equivalentes en las ramas 2 y 3: 68 de 72 semanas cambian con V47/V48 y todas siguen VALIDA.
