# 04 · Análisis de SUST-SEG-1: estrategias A, B y C

**Código:** `SafetySubstitutionPolicy` (commit `f9d4fd5`).

Las restricciones se convierten en etapas con nombre (`Etapa`) y la selección en una `Estrategia`. Con `off`, el comportamiento es **idéntico** al de `generation`: el barrido de 19.562 colocaciones con A y B reproduce el anterior con **0 diferencias** (`evidencia/barrido_19562_analisis.txt`).

## 1. Obligaciones y preferencias (fase 3.2, puntos 1-4)

### Obligatorias

Un candidato se descarta por la **primera** etapa que no cumple. El orden de comprobación es este:

| # | Etapa | Qué exige | A | B | C |
|---|---|---|:-:|:-:|:-:|
| 1 | `ES_EL_MISMO` | no es el ejercicio que se sustituye | ✔ | ✔ | ✔ |
| 2 | `MISMO_SLOT` | mismo slot estructural (MandatorySlot) o, si es relleno, relleno | ✔ | ✔ | ✔ |
| 3 | `MISMA_PRESCRIPCION` | mismo tipo de prescripción (SETS_REPS / DURATION) | ✔ | ✔ | ✔ |
| 4 | `MISMO_COMPLEMENTO` | misma clase de complemento según el objetivo | ✔ | ✔ | ✔ |
| 5 | `ZONA_DEL_ENFOQUE` | zona corporal admitida por el enfoque de la sesión (PUSH, PULL, LOWER, FULL_BODY…) | ✔ | ✔ | ✔ |
| 6 | `BRAZO_DEL_ENFOQUE` | V22: en PUSH no bíceps; en PULL no tríceps | ✔ | ✔ | ✔ |
| 7 | `NO_EN_REVISION` | no está en revisión de catálogo | ✔ | ✔ | ✔ |
| 8 | `NO_EN_LA_SESION_NI_CONTIGUO` | no está ya en la sesión ni en los días contiguos | ✔ | ✔ | ✔ |
| 9 | `DIFICULTAD_NO_MAYOR` | dificultad registrada ≤ la del original | — | ✔ | ✔ |

Hay dos obligaciones que no son etapas de SUST-SEG-1 porque se cumplen antes:
- **dolor y elegibilidad:** el conjunto elegible ya no contiene lo excluido por dolor, por equipamiento o por restricción declarada;
- **dosis:** se recalcula con la clase de dosis de la posición, sin subir (`dosis`; prueba `dosisNoSube`).

### Preferencias (orden), solo entre los candidatos que pasan todas las etapas

1. misma zona;
2. mismo patrón de movimiento;
3. mismo músculo objetivo;
4. **solo C:** dificultad más cercana a la original sin superarla (`original − candidato`: 0 antes que 1, y 1 antes que 2);
5. **id** ascendente: el desempate final, siempre determinista.

El patrón **no** es obligatorio en SUST-SEG-1: es la segunda preferencia. En VARIANTE sí es obligatorio, pero en V8C, no aquí.

## 2. Efectos medidos (fase 3.2, puntos 5-10)

Datos: 19.562 colocaciones de los 960 perfiles, simulando que cada ejercicio colocado dolió. Detalle en `05_COMPARACION_ESTRATEGIAS.csv` y `10_IMPACTO_960_PERFILES.csv`.

| | A (hoy) | B (límite) | C (límite + preferencia) |
|---|---:|---:|---:|
| con sustituto | 19.171 | 19.147 | 19.147 |
| igual dificultad | 13.438 | 15.881 | **18.758** |
| menor dificultad | 3.264 | 3.266 | 389 |
| **mayor dificultad** | **2.469** | **0** | **0** |
| sin sustituto | 391 | 415 | 415 |
| sin sustituto **nuevos** frente a A | — | 24 | 24 |
| misma zona | 18.027 | 17.979 | 17.979 |
| mismo patrón | 18.293 | 18.197 | 18.197 |
| mismo músculo | 17.487 | 17.398 | 17.398 |

5. **¿Priorizar la dificultad altera el músculo objetivo?**
   - **Con C, no.** Su preferencia va detrás de zona, patrón y músculo, y los recuentos de misma zona, patrón y músculo de B y C son **idénticos**. La prueba `LimiteDificultadTest.preferenciaDetrasDeLaZona` lo confirma.
   - **Lo que sí lo altera es el filtro (B o C) frente a A.** De las 2.445 colocaciones que cambian de sustituto A → B:
     - 65 pierden el mismo músculo;
     - 72 pierden el mismo patrón;
     - 24 pierden la misma zona;
     - 0 ganan patrón.

     El motivo es que el mejor candidato funcional de A era el más difícil.
6. **¿Altera el patrón?** Igual que el punto 5: C no lo altera respecto de B; el filtro sí, en 72 colocaciones de 19.147 (0,38 %).
7. **¿Puede elegir una alternativa funcionalmente menos apropiada?**
   - Sí: esas 72, 65 y 24 colocaciones (las que pierden patrón, músculo o zona) son el coste funcional del límite.
   - Siguen pasando **todas** las etapas obligatorias: mismo slot, prescripción, zona del enfoque y V22.
   - **Menor dificultad registrada no equivale a mayor seguridad clínica**; la dificultad del catálogo es un atributo descriptivo (ver 14, R-1).
8. **¿El orden actual favorece determinados IDs?**
   - **Sí.** Con zona, patrón y músculo empatados, decide el id, y el id no tiene significado funcional.
   - Ejemplo de los avanzados con el 135: A elige **663** (dif 3) antes que **780** (dif 2) solo porque 663 < 780. Ambos son de shoulders, HORIZONTAL_PULL y delts.
   - Con B pasa algo parecido: 2.877 colocaciones eligen un sustituto de **menor** dificultad aunque existía uno de **igual** dificultad, también por id.
9. **¿El orden nuevo reduce sustituciones innecesariamente difíciles?**
   - Las elimina el **filtro**: 2.469 → 0.
   - El **orden** de C reduce además las bajadas innecesarias: los sustitutos de menor dificultad pasan de 3.266 a 389, y 2.877 colocaciones pasan de menor a igual.
   - Es decir, C acerca la elección a «la misma exigencia, otra variante», que es lo que pide «no subir» sin imponer H12-A (bajar siempre).
10. **¿Hay algún caso donde elegir menor dificultad sea incompatible con otra restricción?**
    - Por construcción, no: la preferencia solo ordena candidatos que ya cumplen todas las obligaciones.
    - El único conflicto real es el **filtro** frente a la existencia de candidatos: los 24 casos del 135 (§3).

## 3. Los 24 casos: clasificación (fase 3.3)

Los 24 son el 135 («Remo de deltoides traseros con banda, de pie»: shoulders, HORIZONTAL_PULL, delts, dificultad 1, slot UPPER_PULL), siempre en sesiones **PUSH** de perfiles intermedios y avanzados de casa con mancuernas y bandas, de 6 y 7 días. Detalle por colocación en `02_AUDITORIA_24_CASOS_SIN_SUSTITUTO.csv`.

Candidatos del slot que superan `MISMO_SLOT`, iguales en los 24 casos:

| Candidatos | Dificultad | Zona | Descarte con límite |
|---|---:|---|---|
| 114, 115, 116, 117, 143 (VERTICAL_PULL), 119, 1156 (HORIZONTAL_PULL) | **1** | back | `ZONA_DEL_ENFOQUE`: una sesión PUSH no admite back |
| 118, 518, 673, 713 | 2 | back | `ZONA_DEL_ENFOQUE` |
| 780, 781 | 2 | shoulders | `DIFICULTAD_NO_MAYOR`, o `NO_EN_LA_SESION_NI_CONTIGUO` en 3 colocaciones |
| 663 | 3 | shoulders | `DIFICULTAD_NO_MAYOR` |

Las cuatro categorías que pide la orden:

| Categoría | Casos | Por qué |
|---|---:|---|
| Corregibles cambiando el orden | **0** | No hay ningún candidato de dificultad ≤ 1 que supere las etapas obligatorias: el orden no crea candidatos. |
| Corregibles aplicando el filtro | **0** | El filtro es justamente lo que deja el conjunto vacío. |
| Sin ningún sustituto compatible con el límite | **24** | Todos. |
| Requieren revisar catálogo o política | **24, como pregunta, no como cambio** | Ver la nota bajo la tabla. |

Sobre la última fila: en el catálogo V44_2 hay 17 ejercicios de shoulders y HORIZONTAL_PULL.
- Los de dificultad 1 son solo dos: el **135** (band) y el **336** (cable).
- Con body weight, dumbbell o band, el único de dificultad 1 es el propio 135.
- Por eso en gimnasio, con cable, no aparece ningún caso nuevo sin sustituto, y en casa sí.

Si eso es correcto o es un hueco del catálogo lo decide una persona profesional (14, R-4). **No se modifica el catálogo ni la dificultad del 135.**

En los 24, el desenlace con el límite **no** es una sustitución insegura: es la salida segura del 03 (NO_FACTIBLE y luego regeneración P-03 nivel 2). El laboratorio de los 22 perfiles (08) lo comprueba de extremo a extremo.

## 4. Recomendación de estrategia

**C (límite + preferencia), si se decide activar el límite.**

Garantiza lo mismo que B:
- 0 subidas;
- las mismas obligaciones;
- la misma zona, patrón y músculo.

Y además elige la variante de **igual** dificultad cuando existe, en lugar de bajar por un desempate de id: 18.758 iguales frente a 15.881.

No cambia ningún descarte ni ningún caso sin sustituto respecto de B, así que su riesgo adicional sobre B es solo de orden.

B es aceptable si se prefiere el cambio mínimo. A deja 2.469 exposiciones simuladas a subidas.
