# Resultados de las variantes (barridos de 3.780 perfiles × HIST y REV2; K20 con IA simulada)

**Referencia: V46.** Mismo catálogo y mismos planes que el cierre; ver `REGRESION.md`.

**Qué son las variantes:**
- Las variantes **contrafactuales (CF)** son SQL aplicados solo a la base efímera de la prueba (`-Dfitsense.barrido.sql-previo`).
- **P15C** es el interruptor experimental `fitsense.experimental.p15c.aislados-sin-cobertura=true`.
- **Ninguna variante es una migración. Ningún plan publicado se toca.**

**Barrido:** 4 objetivos × 3 niveles × 3 equipamientos × 1–7 días × 15 duraciones. Es la misma rejilla que en los cierres.

**Evidencia:** `../evidencia/decisiones/`
- `barridos/<variante>_<política>/`: `barrido_generacion.csv` y `planes.csv.gz`;
- `comparacion/`: `RESUMEN.csv`, `PERFILES_SIN_ISQUIOS.csv` y `PERDIDOS.csv`;
- `k20/`;
- `SHA256SUMS`.

**Herramientas:** `comparar_decisiones.py`; las definiciones están en su cabecera, fijadas antes de mirar los resultados.

## 0. Advertencia de lectura: «planes distintos» no es «efecto deportivo»

La consulta del conjunto elegible ordena por `body_part, difficulty_level, id` (`ExerciseRepository.java:87`). El generador elige por posición con una semilla fija por usuario y semana.

**Consecuencia:** cambiar la dificultad o los requisitos de **un** ejercicio desplaza las posiciones y vuelve a sortear otras elecciones del mismo perfil, aunque no tengan relación con él. Por eso CF3a (solo excluye a principiantes) cambia también unos 1.000 planes de intermedios y otros tantos de avanzados.

**Así se separa el efecto real del ruido de sorteo:**
- se comparan métricas agregadas, no planes uno a uno;
- se desglosa por nivel (§3.2): en CF3a, intermedios y avanzados varían solo +5 / +15 en «sin isquios» en REV2, mientras que los principiantes varían +187;
- este artefacto ya existía; no lo introduce ninguna variante.

## 1. Tabla general

**Definiciones de las columnas:**
- **Sin isquios:** el plan no tiene ningún ejercicio resistido con objetivo «hamstrings» ni con patrón HIP_HINGE. Los puentes (HIP_EXTENSION) no cuentan aquí; aparecen en la columna «Sin patrón de cadera».
- **Nórdico sustituido sin isquios:** perfiles que tenían curl nórdico en V46 y en la variante no tienen ni nórdico ni otro ejercicio de isquios. Incluye ruido de sorteo en los niveles no afectados.
- **Aviso E2-a:** planes en los que `/plan/limitations` mostraría DOSIS_NO_DEMOSTRADA.

### PLAN-REV-2.0

| Variante | Válidos | Perdidos | DOSIS_LIMITADA | Seg. medios/sesión | Con nórdico | Principiante con nórdico | Sin isquios | Nórdico sustituido sin isquios | Sin patrón de cadera | Rodilla solo aislados | Sin rodilla | Casa con ejercicio de asiento | Aviso E2-a |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **V46** | 3776 | 0 | 1898 | 2253.2 | 650 | 289 | 1572 | 0 | 123 | 324 | 94 | 1416 | 1308 |
| P15C | 3776 | 0 | 1898 | 2253.2 | 194 | 83 | 1953 | 248 | 118 | 0 | 99 | 1421 | 1308 |
| CF2B_P15B (orden anterior) | 3776 | 0 | 1899 | 2252.6 | 113 | 59 | 2014 | 296 | 118 | 0 | 99 | 1431 | 1308 |
| CF3a nórdico dif. 2 | 3776 | 0 | 1901 | 2252.9 | 368 | **0** | 1779 | 327 | 116 | 196 | 101 | 1405 | 1308 |
| CF3b nórdico dif. 3 | 3776 | 0 | 1901 | 2252.9 | 173 | **0** | 1849 | 318 | 124 | 162 | 93 | 1411 | 1308 |
| CF5a asiento (peor caso) | 3776 | 0 | 1900 | 2252.8 | 693 | 324 | 1647 | 215 | 120 | 351 | 97 | **0** | 1301 |
| CF5b anclaje nórdico | 3776 | 0 | 1900 | 2252.9 | 196 | 101 | 1866 | 309 | 122 | 185 | 95 | 1414 | 1308 |
| P15C + CF6 (sissy dif. 2) | 3776 | 0 | 1900 | 2253.0 | 191 | 77 | 1936 | 288 | 121 | 0 | 96 | 1425 | 1308 |
| P15C + CF3a + CF6 | 3776 | 0 | 1902 | 2252.8 | 123 | **0** | 1975 | 344 | 121 | **0** | 96 | 1442 | 1308 |
| CF3a + CF5b + CF6 | 3776 | 0 | 1903 | 2252.8 | 83 | **0** | 1888 | 325 | 125 | 163 | 92 | 1445 | 1308 |
| P15C + CF3a + CF5b + CF6 | 3776 | 0 | 1903 | 2252.7 | 25 | **0** | 2019 | 345 | 121 | **0** | 96 | 1450 | 1308 |

### PLAN-HIST-1 (política por defecto)

| Variante | Válidos | Perdidos | DOSIS_LIMITADA | Seg. medios/sesión | Con nórdico | Principiante con nórdico | Sin isquios | Nórdico sustituido sin isquios | Sin patrón de cadera | Rodilla solo aislados | Sin rodilla | Casa con ejercicio de asiento | Aviso E2-a |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **V46** | 3641 | 0 | 2443 | 2481.2 | 1040 | 468 | 1023 | 0 | 57 | 181 | 42 | 1524 | 1081 |
| P15C | 3641 | 0 | 2443 | 2481.2 | 144 | 56 | 1446 | 353 | 57 | 0 | 42 | 1540 | 1081 |
| CF2B_P15B | 3641 | 0 | 2443 | 2481.1 | 82 | 33 | 1479 | 380 | 57 | 0 | 42 | 1543 | 1080 |
| CF3a nórdico dif. 2 | 3641 | 0 | 2441 | 2481.1 | 570 | **0** | 1190 | 332 | 57 | 133 | 42 | 1515 | 1076 |
| CF3b nórdico dif. 3 | 3641 | 0 | 2441 | 2481.1 | 238 | **0** | 1310 | 359 | 57 | 87 | 42 | 1513 | 1077 |
| CF5a asiento (peor caso) | **3617** | **24** | 2437 | 2480.7 | 1087 | 509 | 1095 | 222 | 57 | 204 | 42 | **0** | 1058 |
| CF5b anclaje nórdico | 3641 | 0 | 2441 | 2481.3 | 319 | 165 | 1357 | 369 | 57 | 109 | 42 | 1525 | 1080 |
| P15C + CF6 | 3641 | 0 | 2442 | 2481.2 | 145 | 57 | 1430 | 343 | 57 | 0 | 42 | 1562 | 1076 |
| P15C + CF3a + CF6 | 3641 | 0 | 2439 | 2481.2 | 102 | **0** | 1468 | 395 | 57 | **0** | 42 | 1568 | 1076 |
| CF3a + CF5b + CF6 | 3641 | 0 | 2441 | 2481.2 | 161 | **0** | 1348 | 353 | 57 | 78 | 42 | 1559 | 1080 |
| P15C + CF3a + CF5b + CF6 | 3641 | 0 | 2441 | 2481.2 | 28 | **0** | 1495 | 396 | 57 | **0** | 42 | 1567 | 1080 |

**Por objetivo:**
- Los válidos por objetivo coinciden con V46 en todas las variantes: REV2 945 / 945 / 941 / 945; HIST 873 / 915 / 915 / 938.
- **Única excepción: CF5a en HIST** (§4).
- Los códigos de invalidez son los de V46: REV2 «V25: 4»; HIST «V20: 139». En CF5a/HIST, «V20: 163».
- **No se ha relajado ninguna regla** (V20, V24, V25, V8C) en ninguna variante.

## 2. P15-C (cobertura de patrón separada de la dosis): resultados propios, no los de CF2-B

**Qué cumple:**
- **0 planes perdidos** en las dos políticas y en los 4 objetivos.
- **Rodilla cubierta solo por aislados: 324 → 0 (REV2) y 181 → 0 (HIST).**
- **Dosis idéntica a V46:**
  - DOSIS_LIMITADA 1.898 (REV2) y 2.443 (HIST);
  - grupos bajo objetivo 4.357 y 5.107;
  - segundos por sesión iguales.
  - Es exactamente lo que el diseño C promete: el aislado conserva su dosis.

**En qué difiere de CF2-B** (la orden pedía no asumirlo):

| | P15-C | CF2-B |
|---|---|---|
| DOSIS_LIMITADA (REV2) | 1.898 | 1.899 |
| Grupos bajo objetivo (REV2) | 4.357 | 4.365 |
| Curls nórdicos que conserva (REV2) | 194 | 113 |
| Perfiles que pierden sus isquios (REV2) | 248 | 296 |

**Coste:**
- Plans without hamstring work rise **1.572 → 1.953 (REV2) and 1.023 → 1.446 (HIST)**.
- Los curls femorales aislados (1195, 1196, 1266, 1007, 1009, 1046…) dejan de elegirse para cubrir la rodilla. El hueco lo ocupan multiarticulares de cuádriceps: sissy +99, sentadilla con alcance +65, sentadilla marcha en pared +62, zancada caminando +52.
- El patrón de cadera (puentes y bisagras) no cambia: 118 frente a 123.
- **Esto es una decisión de diseño:** P15-C no distingue la flexión de rodilla (isquios) de la extensión de rodilla (cuádriceps) dentro de KNEE_DOMINANT. Requiere criterio profesional (ver `APROBACIONES_PENDIENTES.md`).

**1205 (sissy) y 1076 (sentadilla marcha en pared), revisados antes de activar** (asistido por IA; GIF en `E4_E7_MATRIZ_REVISION_HUMANA.md`, nota al pie):

| Ejercicio | Qué muestra el GIF | Contradicción con las instrucciones | Propuesta |
|---|---|---|---|
| **1205** | Agarre a un respaldo; flexión de rodilla muy profunda con los talones elevados y el tronco inclinado atrás en línea con los muslos | Las instrucciones dicen «tronco erguido» y «empuja con los talones»: **contradicen el GIF** (OBJ) | Dificultad 2. Es el sustituto más usado con P15-C (153 planes de principiante en REV2) |
| **1076** | Sentada isométrica en pared con elevaciones alternas de rodilla | — | Se mantiene en 1 (sin impacto; isométrico). **Pendiente profesional** |

**P15C + CF6** (sissy fuera de principiantes):
- 0 planes perdidos.
- Rodilla solo aislados: 0.
- Sin rodilla: 96, frente a 94 en V46 y 99 en P15C.
- Sissy en principiantes: 153 → 0, reemplazado sobre todo por 1076.
- **Excluir 1205 de principiantes no le cuesta cobertura a P15-C.**

**K20 con IA simulada** (U y N, REV2): todos los criterios del protocolo 23 se cumplen.

| Variante | Huella U (oráculo) | Huella N (oráculo) | HIST D U | Criterios del protocolo 23 | Cambios no pertinentes |
|---|---|---|---|---|---|
| V46 (OFF) | `84dfb6d2` 20 PASS | `da34db35` 19 PASS + N19 | `f9f67973` 20 PASS | 14 / 14 CUMPLE | 0 |
| P15C | `e4f8fdb4` 20 PASS | `ea0b5226` 19 + N19 | `c56e8ac6` 20 PASS | 14 / 14 | 0 |
| CF3a | `2c9fcf7f` 20 PASS | `22688606` 19 + N19 | `f9b7e18c` 20 PASS | 14 / 14 | 0 |
| CF3a + CF5b + CF6 | `f56a9337` 20 PASS | `137e57df` 19 + N19 | `148eb47b` 20 PASS | 14 / 14 | 0 |
| P15C + CF3a + CF6 | `a0cb7533` 20 PASS | `80e809f7` 19 + N19 | `591d0513` 20 PASS | 14 / 14 | 0 |
| P15C + CF3a + CF5b + CF6 | `509dcbca` 20 PASS | `dd6aeb72` 19 + N19 | `f43872f0` 20 PASS | 14 / 14 | 0 |

**N19** es el perfil sin plan vigente en S1 (ESPERADO_SIN_PLAN). Es idéntico en V46 y en todas las variantes.

## 3. Curl nórdico (E3): impacto de restringirlo a principiantes

### 3.1 Factibilidad, dosis y duración

| Medida | Resultado |
|---|---|
| Planes válidos | **Sin cambios** en CF3a, CF3b y CF5b, en las dos políticas |
| Dosis | DOSIS_LIMITADA +2/+3 en REV2 y −2 en HIST |
| Duración media | ±0,5 s por sesión |
| Seguridad | No aparece ningún ejercicio de alto impacto ni de dificultad > 1 para principiantes |

### 3.2 Cobertura de isquios por nivel

Columnas: sin isquios / con nórdico / con sissy.

| REV2 | Principiante | Intermedio | Avanzado |
|---|---|---|---|
| V46 | 604 / 289 / 92 | 519 / 198 / 82 | 449 / 163 / 70 |
| CF3a (dif. 2) | **791** / 0 / 158 | 524 / 210 / 85 | 464 / 158 / 73 |
| CF3b (dif. 3) | 791 / 0 / 158 | **594** / 0 / 80 | 464 / 173 / 73 |
| CF5b (anclaje) | 740 / 101 / 154 | 576 / 53 / 80 | 550 / 42 / 83 |
| CF3a + CF5b + CF6 | 750 / 0 / 0 | 582 / 59 / 82 | 556 / 24 / 68 |
| P15C + CF3a + CF6 | 793 / 0 / 0 | 613 / 67 / 90 | 569 / 56 / 72 |

| HIST | Principiante | Intermedio | Avanzado |
|---|---|---|---|
| V46 | 380 / 468 / 206 | 333 / 317 / 114 | 310 / 255 / 93 |
| CF3a (dif. 2) | **554** / 0 / 213 | 324 / 306 / 142 | 312 / 264 / 116 |
| CF3b (dif. 3) | 554 / 0 / 213 | **446** / 0 / 145 | 310 / 238 / 117 |
| CF5b (anclaje) | 525 / 165 / 215 | 442 / 94 / 146 | 390 / 60 / 109 |

**Lectura:**
- Excluir el nórdico de principiantes deja **unos 190 planes de principiante más sin ningún ejercicio de isquios**: +187 en REV2 y +174 en HIST.
- El patrón de cadera no cambia (puentes: «sin patrón de cadera» 116–125 frente a 123). Esos planes conservan puentes, donde los isquios son sinergistas.
- CF3b añade el mismo efecto en intermedios: +75 en REV2 y +113 en HIST. Es una razón más para no proponer la dificultad 3.

### 3.3 Perfiles sin alternativa fiable

Principiantes cuyo plan V46 tenía nórdico y, con CF3a, no tienen ningún ejercicio de isquios:

| | Total | Casa sin material | Casa con mancuernas o bandas | Gimnasio |
|---|---|---|---|---|
| REV2 | 159 | **79** | 33 | 47 |
| HIST | 196 | **127** | 33 | 36 |

Están repartidos por los 4 objetivos y por los 7 días; ver `../evidencia/decisiones/comparacion/PERFILES_SIN_ISQUIOS.csv`.

**Causa en el catálogo.** Alternativas de isquios con dificultad 1, sin contar los nórdicos:

| Equipamiento | Alternativas |
|---|---|
| Casa sin material | **solo 939 «Abdominal patada»** (*kick out sit*). Sus instrucciones exigen silla y describen extender las rodillas, lo que contradice el objetivo «hamstrings». **No es una alternativa fiable** |
| Casa con mancuernas o bandas | 5 (pesos muertos con banda) |
| Gimnasio | 10 |

**Conclusión:** en casa sin material, para un principiante, **el catálogo no tiene un ejercicio fiable de isquios.** Restringir el nórdico, que además exige un anclaje que esa persona no ha declarado, cambia «isquios con un ejercicio inadecuado» por «sin isquios dirigidos, solo puentes». Cerrar el hueco requiere contenido nuevo con validación, como en E2-c. Ejemplos: peso muerto a una pierna sin carga, curl deslizante con toalla, buenos días sin carga.

## 4. Equipamiento (E5/E6)

| Variante | REV2 | HIST |
|---|---|---|
| **CF5a: asiento exigido, nadie declara silla (peor caso)** | **0 planes perdidos.** Los 1.416 planes de casa con ejercicio de asiento pasan a 0. Entran ejercicios de banda de pie: press de hombros con banda +133, remo de deltoides posterior +127… | **−24 planes válidos** (V20: 139 → 163). Todos son INCREASE_STRENGTH intermedio o avanzado, en casa sin material, de 5–7 días |
| **CF5b: anclaje para 1195/1196** | 0 planes perdidos. Nórdico en casa sin material: 252 → 0. Principiantes con nórdico: 289 → 101 (quedan 327, 915, 916 y 1197, que ya exigían polea, banco o *step*) | 0 planes perdidos. 389 → 0 |

**Lectura de CF5a:**
- Con silla, la situación es la de V46.
- Sin silla, REV2 se mantiene, pero **HIST pierde 24 planes en casa sin material**.
- Por la orden, **no se relaja V20**. Si se adopta el requisito de asiento, esos 24 perfiles serían NO_FACTIBLE en HIST, explicado en `/plan/limitations`.
- **El efecto real depende de cuántos usuarios declaren silla:** está entre V46 y CF5a.

## 5. Aviso E2-a (exposición)

- **REV2:** 1.308 planes (34,6 %): 1.015 por espalda y 917 por hombro.
- **HIST:** 1.081 planes (29,7 %): 868 y 537.
- Las variantes E3/P15 apenas cambian estas cifras (±5); CF5a baja a 1.301 y 1.058.
- **Es una limitación frecuente.** El aviso la hace visible sin cambiar planes.
