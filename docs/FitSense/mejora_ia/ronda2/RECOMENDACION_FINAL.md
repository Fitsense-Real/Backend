# MEJ ronda 2 · Recomendación final

**Fecha:** 09/11/2026 · **Rama:** `experimento/mejora-ia` · **Código ejecutado:** `1aac853` (variantes apagadas por defecto) · **Protocolo:** `PROTOCOLO_MEJ_RONDA_2.md` (congelado antes de la primera llamada; enmienda E-R2-1 antes de la confirmatoria) · **Modelo:** la misma versión del proveedor (`045f8102…`) en todos los bloques, de la primera a la última llamada. No hay comparaciones afectadas por un cambio de versión.

**Nada está activado.** Ninguna variante se ha encendido en el backend. No hay merge, despliegue ni PR de adopción. La decisión de incorporar una versión es del responsable, después de leer este informe.

---

## 1. Respuesta corta

| Pregunta | Respuesta |
|---|---|
| ¿Qué prompt genera las mejores rutinas? | **pr-1.8c**, con una reserva. En la fase confirmatoria tiene más planes válidos a la primera (28/38 frente a 21/38 del control), menos rechazos (12 frente a 23) y menos hallazgos de reparto (V15 9 frente a 20; V26 5 frente a 14). No dejó ningún plan en respaldo ni tuvo ningún timeout real. **Reserva:** tuvo 3 hallazgos V24 frente a 2 del control. Todos los rechazó V24 y ninguno se publicó, pero el protocolo pide «sin más V24 que el control», y por eso no cumple todos los criterios. pr-1.8a queda muy cerca (27/38 y 1 V24), con un respaldo atribuible (O12 en R3). |
| ¿Cuántos planes fueron válidos al primer intento? | **Confirmatoria (R2+R3, 38 planes por versión):** PR-1.7 21 (55,3 %), pr-1.8a 27 (71,1 %), pr-1.8c 28 (73,7 %). **Con R1 (57 por versión):** 31 (54,4 %), 41 (71,9 %) y 43 (75,4 %). pr-1.8b (solo en R1): 14/19. |
| ¿Qué versión aprovechó mejor la dosis? | **Ninguna de forma demostrable.** DOSIS_LIMITADA en el 69–89 % de los planes medibles de todas las versiones. Aprovechamiento del tiempo del 61–64 % de M en todas. Wilcoxon pareado, sin diferencias (p = 0,21–0,89). |
| ¿Qué versión distribuyó mejor los ejercicios? | **pr-1.8a y pr-1.8c, por igual.** Reducen a un tercio los hallazgos de reparto en las propuestas: V15 20 → 7 / 9 y V26 14 → 7 / 5. En los planes publicados no hay diferencia, porque todos pasan V15/V26. pr-1.8c además casi elimina V23 en GENERAL_FITNESS (6 → 1). |
| ¿Qué pasó con O10? | **El problema de MEJ no se repitió en ninguna de las 10 corridas, tampoco con el control.** O10 siempre salió válido a la primera, con 3 ejercicios (nunca 2), una sesión COMPLETA y el 89–96 % del tiempo. La mejora no puede atribuirse a pr-1.8c. El techo aeróbico (6–12 de 150 min) es estructural: una sesión de 30 min por semana. |
| ¿Qué prompt hizo las mejores adaptaciones? | **Empate técnico entre v2.2a y v2.4** (36/36 aplicadas, 0 SIN_EFECTO evitables, 0 INC por identidad, 0 cambios no autorizados). V2.1 queda a un caso (35/36: 1 SIN_EFECTO evitable en O18, R2). La diferencia **no es concluyente** (McNemar 1:0, p = 1). |
| ¿Qué pasó con O12 y O18? | **O12 (dolor):** las tres versiones aplican SEGURIDAD_ANTE_DOLOR en las 9 corridas. Retiran los 8 ejercicios de la sesión con dolor y ponen 5–8 de dificultad igual o menor. El trabajo baja de 1686 a 384–648 s. Pertinencia ALT (aceptable). La política del dolor sigue pendiente de revisión profesional. **O18 (acortar):** retiran el relleno 1302 (dificultad 1) y el trabajo baja de 498 a 420 s. Así fue en 8 de 9 corridas; la excepción es V2.1 en R2, que conservó la sesión (SIN_EFECTO evitable). |
| ¿Aparecieron cambios no autorizados? | **No.** 0 en 108 sesiones adaptadas, vigiladas por identidad independientemente de V8C: ninguna retirada sin autorización (H-A1) y ningún cambio de descanso (H-A2). **Los dos huecos de V8C siguen en el código** (§6). |
| ¿Qué variantes empeoraron? | pr-1.8b (3 V24 en R1, excluida antes de la confirmatoria). pr-1.8c: +1 V24 respecto al control y latencia mediana más alta (76–91 s). pr-1.8a: 1 respaldo por tres rechazos V23 (O12 R3) y una caída a 12/19 en R3. V2.1: 1 SIN_EFECTO evitable. v2.4: 1 timeout del proveedor (R3), completado por la regla. |
| ¿Qué mejoras son concluyentes y cuáles exploratorias? | **Ninguna mejora es concluyente con el criterio confirmatorio estricto**: dos variantes, α = 0,025 por Bonferroni. La mejora de validez a la primera de pr-1.8c y pr-1.8a es **consistente** (las 6 comparaciones por ronda favorecen a la variante) y **probable**, pero queda como **exploratoria / no concluyente** (§3.2). Lo demás (dosis, tiempo, adaptación) no muestra diferencia. |
| ¿Cuánto costó? | **362 predicciones, USD 7,9051** medidos: exploratoria 135 / USD 3,0632 y confirmatoria 227 / USD 4,8419. Por debajo de los topes (USD 4, 9 y 10). Sin usar: USD 2,09. Incluye la reserva de USD 0,05 de las 11 predicciones sin métricas, cuyo coste real fue igual o menor. |

---

## 2. Qué se hizo

- **Exploratoria (R1, USD 3,06):** una corrida de cada versión sobre los mismos 19 perfiles factibles (O01–O20 sin O11) y el mismo catálogo:
  - generación: PR-1.7 (control), pr-1.8a, pr-1.8b y pr-1.8c;
  - adaptación: V2.1 (control), v2.2a y v2.4.
- **Decisión exploratoria:** pr-1.8b quedó excluida (más V24 que el control). Las demás pasaron a la confirmatoria (§8 del protocolo).
- **Confirmatoria (R2 en orden inverso y R3 en orden directo, USD 4,84):** control y variantes otra vez sobre los mismos perfiles y la misma versión del modelo. **Los prompts no se combinaron ni se modificaron después de ver los resultados.**
- **Compleción (regla I-MEJ-1, incidencias I-R2-3 a I-R2-12):**
  - Un perfil cortado por un timeout o un fallo del proveedor tiene un reintento en otra carpeta.
  - Los perfiles no ejecutados se completan.
  - Una respuesta que el proveedor terminó y que el backend no pudo leer (502 o sondeo agotado) se recupera y se valida gratis con el validador real, **sin repetir la llamada**.
  - No se repitió ninguna respuesta válida ni ninguna variante terminada.
  - El timeout de producción (150 s) no se cambió.
- **Validación y vigilancia:** la validez la decide el validador real del backend (V1–V27 en generación; V8C + V1–V27 en adaptación). La adaptación se mide también con el oráculo v1.4 y con una vigilancia por identidad, independiente de V8C (H-A1/H-A2).

---

## 3. Generación

### 3.1 Resultados por corrida (19 perfiles en cada una)

| Versión | Ronda | Válidos 1.º | Propuestas | Rechazadas | V15 | V26 | V24 | V23 | Respaldo | DOSIS_LIMITADA | % de M (media) | Latencia mediana | USD |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| PR-1.7 | R1 | 10 | 30 | 12 | 13 | 8 | 0 | 4 | 1 (502) | 11/16 | 63,1 | 89 s | 0,93 |
| PR-1.7 | R2 | 10 | 31 | 12 | 8 | 4 | 1 | 3 | 0 | n/d (I-R2-9) | n/d | 65 s | 0,67 |
| PR-1.7 | R3 | 11 | 30 | 11 | 12 | 10 | 1 | 3 | 0 | 16/19 | 61,3 | 89 s | 0,81 |
| pr-1.8a | R1 | 14 | 23 | 4 | 2 | 0 | 0 | 1 | 1 (proveedor) | 14/19 | 63,3 | 75 s | 0,55 |
| pr-1.8a | R2 | 15 | 22 | 4 | 2 | 2 | 0 | 2 | 0 | 16/18 | 63,4 | 67 s | 0,55 |
| pr-1.8a | R3 | 12 | 29 | 11 | 5 | 5 | 1 | 4 | **1 (O12, 3×V23)** | 17/19 | 63,4 | 82 s | 0,70 |
| pr-1.8c | R1 | 15 | 23 | 4 | 5 | 2 | 0 | 0 | 0 | 14/19 | 61,9 | 76 s | 0,56 |
| pr-1.8c | R2 | 14 | 26 | 7 | 4 | 3 | 2 | 1 | 0 | 16/19 | 61,6 | 88 s | 0,65 |
| pr-1.8c | R3 | 14 | 24 | 5 | 5 | 2 | 1 | 0 | 0 | 14/18 | 64,4 | 91 s | 0,61 |
| pr-1.8b | R1 | 14 | 25 | 6 | 4 | 3 | **3** | 2 | 0 | 14/19 | 63,7 | 69 s | 0,62 |

Notas de la tabla:
- V15, V26, V24 y V23 cuentan hallazgos en las propuestas rechazadas. **Ningún plan publicado tiene violaciones**: lo que no pasa V1–V27 no se publica, y si se agotan los intentos se publica el plan determinista.
- Los respaldos de PR-1.7 R1 y pr-1.8a R1 se deben a fallos del proveedor (502 / `failed`), no al prompt.
- Las columnas USD son el gasto total de cada corrida, incluidas sus carpetas de compleción, según la hoja `Costes`.

**Variabilidad entre rondas** (válidos a la primera):
- PR-1.7: 10, 10, 11 (rango 10–11).
- pr-1.8a: 14, 15, 12 (12–15).
- pr-1.8c: 15, 14, 14 (14–15).

pr-1.8c es la más estable.

**Por objetivo** (confirmatoria, válidos a la primera):

| Objetivo | PR-1.7 | pr-1.8a | pr-1.8c |
|---|---|---|---|
| GAIN_MUSCLE | 7/10 | 9/10 | 8/10 |
| GENERAL_FITNESS | 4/10 | 5/10 | **7/10** |
| INCREASE_STRENGTH | 2/8 | **6/8** | 4/8 |
| LOSE_WEIGHT | 8/10 | 7/10 | **9/10** |

- Las cuatro estructuras por objetivo se mantienen: ningún plan publicado rompe V2, V13, V18, V23 ni V25.
- El único hallazgo estructural es V23 (los cuatro patrones de la semana) en GENERAL_FITNESS: 6 con PR-1.7, 6 con pr-1.8a y 1 con pr-1.8c.

### 3.2 Comparación pareada con el control (mismo perfil y misma ronda)

| Variante | Ronda | Solo la variante válida | Solo el control válido | McNemar exacta | Rechazadas (Wilcoxon) |
|---|---|---|---|---|---|
| pr-1.8a | R1 | 5 | 1 | p = 0,22 | p = 0,04 |
| pr-1.8a | R2 | 5 | 0 | p = 0,06 | p = 0,04 |
| pr-1.8a | R3 | 3 | 2 | p = 1,00 | p = 1,00 |
| pr-1.8c | R1 | 6 | 1 | p = 0,13 | p = 0,04 |
| pr-1.8c | R2 | 5 | 1 | p = 0,22 | p = 0,29 |
| pr-1.8c | R3 | 3 | 0 | p = 0,25 | p = 0,06 |
| **pr-1.8a** | **Confirmatoria (38 pares)** | **8** | **2** | **p = 0,11** | — |
| **pr-1.8c** | **Confirmatoria (38 pares)** | **8** | **1** | **p = 0,039** | — |
| pr-1.8a | Todas (57 pares) | 13 | 3 | p = 0,021 | — |
| pr-1.8c | Todas (57 pares) | 14 | 2 | p = 0,004 | — |

**Por perfil**, sumando las rondas, para no tratar como independientes las repeticiones de un mismo perfil:

| Variante | Fase | Perfiles mejor / peor / igual | Wilcoxon |
|---|---|---|---|
| pr-1.8a | Confirmatoria | 5 / 1 / 13 | p = 0,12 |
| pr-1.8a | Todas las rondas | 7 / 1 / 11 | p = 0,042 |
| pr-1.8c | Confirmatoria | 7 / 1 / 11 | p = 0,059 |
| pr-1.8c | Todas las rondas | 9 / 0 / 10 | p = 0,009 |

**Intervalos (Wilson, 95 %), confirmatoria:**
- PR-1.7: 55,3 % [39,7–69,9]
- pr-1.8a: 71,1 % [55,2–83,0]
- pr-1.8c: 73,7 % [58,0–85,0]

Los intervalos se solapan.

**Lectura:**
- Las 6 comparaciones por ronda van a favor de las variantes: la mejora es consistente.
- Con el criterio confirmatorio estricto (solo R2+R3; dos variantes, α = 0,025), **ninguna variante demuestra la mejora**:
  - pr-1.8c: p = 0,039 en McNemar y 0,059 por perfil;
  - pr-1.8a: p = 0,11 y 0,12.
- Con las tres rondas, pr-1.8c sí pasa el umbral (p = 0,004 y 0,009). Pero R1 sirvió para elegir las variantes, así que ese agregado está sesgado a favor y no puede usarse como prueba confirmatoria.
- **Conclusión:** la mejora de validez a la primera es **probable pero exploratoria / no concluyente**.

### 3.3 Criterios del protocolo (§5) para declarar candidata

| Criterio | pr-1.8a | pr-1.8c |
|---|---|---|
| 1. Mejora sus indicadores (G1 rechazos y V15/V26; G2 dosis; G3 tiempo) | G1 sí; G2 y G3 sin diferencia | G1 sí; G2 y G3 sin diferencia |
| 2. Sin más V24 que el control (confirmatoria: control 2) | **Sí (1)** | **No (3)** |
| 3. Sin estructuras rotas ni más respaldos atribuibles | **No: 1 respaldo atribuible (O12 R3, 3×V23)**; control 0 | **Sí (0)** |
| 4–5. (adaptación) | n/a | n/a |
| 6. Ningún plan publicado con violaciones | Sí | Sí |
| **¿Candidata según el protocolo?** | **No (criterio 3, por un caso)** | **No (criterio 2, por un caso)** |

### 3.4 V24

V24 nunca se relajó: cada hallazgo se rechazó y el plan publicado lo corrigió en el reintento.

| Versión | Hallazgos V24 | Casos |
|---|---|---|
| Control PR-1.7 | 2 | R2 O05, R3 O08 |
| pr-1.8a | 1 | R3 O16 |
| pr-1.8c | 3 | R2 O05 y O16, R3 O07 |
| pr-1.8b | 3 | En una sola ronda (R1 O12 y O16) |

V24 aparece también con el control, así que no es exclusivo de las variantes.

---

## 4. Adaptación

Estas son las 12 sesiones con IA por corrida: el mismo plan determinista y las mismas situaciones (dolor, tiempo, dificultad, causa no registrada) en todas las versiones.

| Versión | Ronda | Aplicadas | SIN_EFECTO | SIN_EFECTO evitables | ACC / ALT / CONS-J | INC (oráculo) | INC por identidad | No autorizados (identidad) | USD |
|---|---|---|---|---|---|---|---|---|---|
| V2.1 | R1 | 12 | 0 | 0 | 10 / 1 / 1 | 0 | 0 | 0 | 0,14 |
| V2.1 | R2 | 11 | 1 | **1 (O18)** | 9 / 1 / 2 | 0 | 0 | 0 | 0,13 |
| V2.1 | R3 | 12 | 0 | 0 | 10 / 1 / 1 | 0 | 0 | 0 | 0,13 |
| v2.2a | R1 | 12 | 0 | 0 | 10 / 1 / 1 | 0 | 0 | 0 | 0,14 |
| v2.2a | R2 | 12 | 0 | 0 | 10 / 1 / 1 | 0 | 0 | 0 | 0,14 |
| v2.2a | R3 | 12 | 0 | 0 | 10 / 1 / 1 | 0 | 0 | 0 | 0,14 |
| v2.4 | R1 | 12 | 0 | 0 | 10 / 0 / 1 | 1 (artefacto) | 0 | 0 | 0,14 |
| v2.4 | R2 | 12 | 0 | 0 | 10 / 1 / 1 | 0 | 0 | 0 | 0,14 |
| v2.4 | R3 | 12 | 0 | 0 | 10 / 1 / 1 | 0 | 0 | 0 | 0,19 |

Notas de la tabla:
- La columna USD de v2.4 R3 incluye la compleción de I-R2-12.
- **Total de las tres rondas:** V2.1 35/36 aplicadas (1 SIN_EFECTO evitable); v2.2a 36/36; v2.4 36/36.
- **No autorizados:** 0 en las 108 sesiones adaptadas.
- **CONS-J** es la conservación justificada de O05 (CAUSA_NO_REGISTRADA), que es la respuesta correcta.
- **El INC del oráculo** en v2.4 R1 (O12) es el artefacto del emparejamiento posicional. Por identidad, la dificultad máxima no sube (2 → 2) y el trabajo baja (1686 → 564 s).

**Sustituciones, reducciones y conservación** (detalle por sesión en `RESULTADOS_ADAPTACION_MEJ_R2.csv`):
- **Dolor (O12):** sustitución completa de la sesión con dolor. Dificultad máxima de los añadidos menor o igual que la de los retirados en las 9 corridas.
- **Tiempo (O18):** una sola retirada de relleno, permitida en ACORTAR_O_REORGANIZAR.
- **Dificultad:** sustitución por variantes más sencillas sin retiradas. La vigilancia H-A1 no encontró retiradas en ninguna corrida.
- **Descansos:** sin cambios en los ejercicios que se quedan (vigilancia H-A2).
- **Ejercicios no señalados:** se conservan.

**Lectura:** las tres versiones se comportan igual en 35 de 36 pares; la única diferencia es un caso (O18 R2 con V2.1). Que una versión cambie más ejercicios o pase todos los validadores no la hace mejor (orden §4). Con estos datos **no se demuestra que v2.2a o v2.4 mejoren a V2.1**.

---

## 5. Recomendación

1. **Generación:**
   - **Recomiendo pr-1.8c como candidata para una decisión de adopción, sin activarla.** Es la versión con más evidencia: la mayor validez a la primera, la más estable, la que menos rechaza, menos V23 y ningún respaldo atribuible.
   - Antes de adoptarla hay que aceptar dos cosas:
     - tiene un V24 más que el control (siempre rechazado);
     - la mejora no es concluyente con el criterio confirmatorio estricto.
   - **pr-1.8a es la alternativa:** menos V24 y menor latencia, pero un respaldo atribuible.
   - Ninguna cumple los seis criterios del protocolo al pie de la letra; cada una falla por un caso.
   - Si se quiere una prueba concluyente antes de adoptar, haría falta otra confirmatoria del control y de pr-1.8c, como decisión aparte y con presupuesto nuevo. No la he lanzado porque el protocolo terminaba en R3.
2. **Adaptación:**
   - **No hay una mejora demostrada que justifique cambiar V2.1.**
   - Si se busca coherencia en O18 (retirar un relleno al acortar), **v2.2a** es la opción: 36/36 aplicadas, no peor en nada y sin cambios no autorizados. Pero la diferencia (un caso) es exploratoria.
   - v2.4 da lo mismo y no añade ventaja.
3. **Descartadas:**
   - **pr-1.8b:** regresión V24 en la exploratoria (3 hallazgos frente a 0 del control).
   - **v2.4:** sin ventaja sobre v2.2a.
4. **Antes de fiarse de V8C** (independiente del prompt elegido), hay que corregir sus dos huecos (§6). En esta ronda no se han tocado, por orden expresa. Hoy, un cambio no autorizado de esos dos tipos se publicaría.
5. **Ninguna variante se activa.** Todas siguen apagadas por defecto. El paso a producción requiere tu aprobación, un PR de adopción separado y una regresión completa.

---

## 6. Defectos de V8C (sin modificar en esta ronda)

| Id | Defecto | Efecto | En esta ronda |
|---|---|---|---|
| **H-A1** | `ConservationValidator:1247` (`if (d.anadidos().isEmpty()) return;`): en VARIANTE_MAS_SENCILLA, V8C acepta propuestas que solo retiran ejercicios, aunque sean varios o estructurales. La política solo autoriza retirar en ACORTAR_O_REORGANIZAR (una retirada) y en SEGURIDAD_ANTE_DOLOR | Una retirada no autorizada se publicaría. Ninguna prueba lo cubre | 0 casos en 108 sesiones (vigilancia por identidad) |
| **H-A2** | Con permiso de dosis, V8C acepta un cambio solo del descanso (`rest_seconds`); la política dice que el descanso no se cambia | Un cambio de descanso se publicaría | 0 casos |

Recomendación: corregir los dos con sus pruebas en un PR propio antes de activar cualquier versión de adaptación. No se ha hecho aquí porque la orden lo prohíbe.

---

## 7. Regresión

| Suite | Resultado | Fecha |
|---|---|---|
| Unitarias, en `1aac853` | **1316 pruebas, 0 fallos**, 1 omitida | 09/11/2026, tras la ronda |
| Integración, en `1aac853` | **593 pruebas, 3 fallos** | Congelación |

Detalle de la regresión:
- **Integración, los 3 fallos:**
  - `CatalogoV36IT.semanaSiguiente`
  - `ProgresionIT.p02ProgresionSinElExcluido`
  - `ProgresionRev2IT.p02ProgresionSinElExcluido`

  Fijan su semana con `LocalDate.now()` y fallan igual sin las variantes (I-R2-1). **La integración no está toda en verde.** Las pruebas no se han modificado.
- Desde `1aac853` solo ha cambiado documentación y herramientas de análisis: el código del backend es el mismo.
- Se mantienen intactos:
  - V1–V27, V24, V25 y V8C;
  - PLAN-REV-2.0 y las 4 estructuras;
  - la progresión, la recuperación y la reincorporación;
  - el catálogo y los prompts históricos.

---

## 8. Coste

| Fase | Predicciones | USD medidos | Tope |
|---|---|---|---|
| Exploratoria (R1) | 135 | 3,0632 | 4,00 |
| Confirmatoria (R2 + R3 y compleciones) | 227 | 4,8419 | 9,00 acumulado |
| **Total** | **362** | **7,9051** | 10,00 |

**Por condición (todas las rondas):**

| Condición | Predicciones | USD |
|---|---|---|
| PR-1.7 | 97 | 2,40 |
| pr-1.8a | 75 | 1,79 |
| pr-1.8c | 74 | 1,82 |
| pr-1.8b | 25 | 0,62 |
| V2.1 | 30 | 0,40 |
| v2.2a | 30 | 0,41 |
| v2.4 | 31 | 0,46 |

El control es la generación más cara porque necesita más reintentos.

**Cómo se calcula el coste:**
- Tarifa: 0,25 / 2,00 USD por millón de tokens de entrada / salida.
- 11 predicciones sin métricas (8 timeouts, 2 sin métricas y 1 fallida) se cargan con la reserva de USD 0,05 cada una. En el caso de O18 R3 de pr-1.8c el coste real fue USD 0,0268.
- Cada predicción figura en `PREDICCIONES_Y_COSTOS.csv`, con su versión de modelo.

---

## 9. Entregables

| Archivo | Contenido |
|---|---|
| `RESULTADOS_GENERACION_MEJ_R2.csv` | 209 filas: condición, ronda y perfil (validez, rechazos, códigos, dosis, sesiones, tiempo, latencia, coste) |
| `RESULTADOS_ADAPTACION_MEJ_R2.csv` | 108 sesiones: intervención, estado, pertinencia, quitados / añadidos / se quedan por identidad, dosis, dificultad, trabajo y vigilancia H-A1/H-A2 |
| `COMPARACION_PROMPTS_MEJ_R2.xlsx` | Resúmenes, pareado por ronda y agregados (todas y confirmatoria), por perfil, adaptación y costes |
| `PREDICCIONES_Y_COSTOS.csv` | Las 362 predicciones con tokens, tiempo, coste, estado y versión del modelo |
| `EVIDENCIA_MEJ_RONDA_2.zip` | Corridas, oráculo, respuestas recuperadas, registros, prompts volcados, ejecutores y análisis |
| `INCIDENCIAS_MEJ_R2.md` | I-R2-0 a I-R2-13 |

Fase 1 (sin cambios): `AUDITORIA_REGLAS_FITSENSE.md`, `MATRIZ_REGLAS_Y_VALIDADORES.xlsx`, `DIAGNOSTICO_GENERACION_ADAPTACION.md`, `PROTOCOLO_MEJ_RONDA_2.md` y `correr_MEJ_R2.sh`.
