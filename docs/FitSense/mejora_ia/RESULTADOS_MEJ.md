# MEJ · Resultados de la mejora de generación y adaptación con IA

**Encargo:** mejorar la generación y la adaptación sin añadir funciones, reglas ni validadores.

**Protocolo:** `PROTOCOLO_MEJ.md`, congelado antes de pagar, con la enmienda E-1 y la incidencia I-MEJ-1.

**Rama:** `experimento/mejora-ia`. Las dos variantes están **apagadas por defecto**: PR-1.7 y V2.1 siguen siendo idénticos byte a byte.

## 1. Resumen

| Objetivo | Qué se cambió | Resultado con la misma versión del modelo | Recomendación |
|---|---|---|---|
| Menos rechazos en generación | PR-1.8: el prompt da los números de V15 (la mitad) y de V26 (techos por grupo), que el backend ya exigía y PR-1.7 no daba | Rechazos de 10 a **3**. Hallazgos V15+V26 de 20 a **5**. Primeras propuestas válidas de 12/19 a **15/19** (pareado: 5 frente a 2, p = 0,45) | **Adoptar las líneas de V15 y V26** |
| Más dosis | PR-1.8: dosis orientativa de `CalidadDosis` en el prompt | DOSIS_LIMITADA de 17/19 a 15/19; los grupos por debajo, prácticamente iguales. Efecto secundario: 1 rechazo V24 en GAIN_MUSCLE (el modelo subió series fijadas por 4D) | **No adoptar** esa línea tal como está; reformularla antes de medirla otra vez |
| Menos SIN_EFECTO | V2.2: la factibilidad de ACORTAR ofrece retirar un relleno (antes decía «omítela») | Con la versión actual, V2.1 y V2.2 dan lo mismo: 12 de 12 aplicadas en A0, A1 y A2. El SIN_EFECTO de O18 de las corridas antiguas no se repite ni con V2.1 | **Adoptar como corrección de coherencia**, sin efecto medible |
| Calidad de las adaptaciones | V2.2: ante dolor, primero el mismo patrón y grupo | O12 sigue en ALT (patrón nuevo) en todas las condiciones | **No adoptar** esa línea: no cambia nada. La pregunta sigue pendiente de revisión profesional |

**Ninguna variante empeoró la seguridad.** No hubo ningún plan ni adaptación publicados con violaciones, ningún cambio no autorizado y ningún INC.

## 2. Ejecución y presupuesto

**Gasto:** bloque autorizado de 120 predicciones y USD 3,00; usadas **102 y USD 2,00**. Sobran 18.

| Corrida | Qué es | Llamadas | USD |
|---|---|---:|---:|
| MEJ-A1, MEJ-A2 | Generación determinista + **V2.2** | 10 + 10 | 0,275 |
| MEJ-A0 | Generación determinista + **V2.1** (control, enmienda E-1) | 10 | 0,136 |
| MEJ-G1 (G + P + G1C + G1R) | Generación con **PR-1.8** + adaptación V2.2 | 33 | 0,750 |
| MEJ-C (+ CC) | Generación con **PR-1.7** + adaptación V2.1 (control, E-1) | 39 | 0,839 |

**Versión del modelo:** todas las corridas MEJ usaron la versión vigente, `045f8102…`. GRA, R_O y R′_O usaron `9f4cd9ec…`. Por eso las comparaciones válidas son **MEJ frente a MEJ**; GRA y R_O se muestran solo como referencia.

**Incidencias del proveedor (I-MEJ-1)**, tratadas igual en las dos condiciones:
- **G1:** O03 superó el límite de 150 s y el arnés detuvo el bloque. Se completaron los perfiles no ejecutados y O03 tuvo un reintento.
- **G1, O08:** la predicción falló («failed») y el backend aplicó el respaldo. Cuenta como no válida a la primera.
- **C:** O18/ciclo 1 devolvió un error con estado desconocido. Se completaron O19 y O20.

Ninguna respuesta válida se repitió.

## 3. Generación (19 perfiles factibles; O11 aparte, bloqueado antes de llamar en todas)

| | GRA-R1 (PR-1.7, versión anterior) | GRA-R2 (PR-1.7, versión anterior) | **MEJ-C (PR-1.7, vigente)** | **MEJ-G1 (PR-1.8, vigente)** |
|---|---|---|---|---|
| Primeras propuestas válidas | 12/19 | 13/19 | **12/19** | **15/19** (15/18 sin el fallo del proveedor) |
| Propuestas totales | 29 | 25 | 29 | 22 |
| Propuestas rechazadas | 10 | 6 | **10** | **3** |
| Hallazgos V15 / V26 / V23 / V24 | 11 / 12 / 2 / 1 | 7 / 11 / 0 / 0 | 12 / 8 / 3 / 0 | 2 / 3 / 1 / **13** |
| Planes publicados desde la IA / respaldo | 19 / 0 | 19 / 0 | 19 / 0 | 18 / 1 (proveedor) |
| DOSIS_LIMITADA tras la generación | 16/19 | 16/19 | 17/19 | 15/19 |
| Llamadas / USD | 39 / 0,82 | 35 / 0,68 | 39 / 0,84 | 33 / 0,75 |

**Comparación pareada por perfil** (MEJ-G1 frente a MEJ-C, primera propuesta):

| Válida en… | Perfiles |
|---|---:|
| Solo PR-1.8 | 5 (O03, O05, O12, O16, O17) |
| Solo PR-1.7 | 2 (O04, O18) |
| Las dos | 10 |
| Ninguna | 2 |

McNemar exacta: p = 0,45. Es una mejora consistente en los códigos que PR-1.8 aborda, pero **no concluyente** con 19 perfiles y una sola corrida.

**Hallazgos:**
- **V15 y V26:** era la incoherencia diagnosticada, y es donde se concentra la mejora (20 hallazgos a 5).
- **Efecto secundario en O18 (GAIN_MUSCLE):** con PR-1.8, el modelo puso 4×8 y 4×10 en ejercicios cuya dosis 4D es fija (3×10). Son los 13 hallazgos V24 de una sola propuesta, y se resolvió en el reintento. La línea de dosis orientativa empuja a subir series donde la dosis no es libre. En GAIN_MUSCLE, el objetivo semanal se alcanza con más colocaciones, no con más series.
- **Dosis:** los grupos por debajo del objetivo apenas cambian (hombros 13→14, espalda 11→10, pecho 7→7, aeróbico 10→10). Exponer el objetivo no basta; la limitación real parece estar en los días, el tiempo y las colocaciones de cada perfil.

## 4. Adaptación

### Pareada: generación determinista, mismos 10 ciclos

| Ciclo | R_O (V2.1, ant.) | R′_O (V2.1, ant.) | **A0 (V2.1, vig.)** | **A1 (V2.2, vig.)** | **A2 (V2.2, vig.)** |
|---|---|---|---|---|---|
| O03, O04, O05, O06, O14, O17, O19, O20 | APLIC/ACC | APLIC/ACC | APLIC/ACC | APLIC/ACC | APLIC/ACC |
| O12 (dolor) | RESPALDO | APLIC/ALT | APLIC/ALT | APLIC/ALT | APLIC/ALT |
| O18 (ACORTAR, GAIN_MUSCLE) | **SIN_EFECTO** | APLIC/ACC | APLIC/ACC | APLIC/ACC | APLIC/ACC |
| **Total (12 sesiones)** | 10 apl., 1 resp., 1 SE | 12 apl. | **12 apl.** | **12 apl.** | **12 apl.** |

En O05 se señalan tres sesiones (dos ACC y una CONS-J), igual en todas las condiciones.

**Lectura:**
- **V2.2 no cambia el resultado con la versión actual del modelo.**
- **O18:** también con V2.1, el modelo retira el relleno 1302 aunque la factibilidad le diga que omita la sesión.
- **De dónde viene la diferencia frente a R_O:** la versión del modelo (o el azar), no el prompt.
- **Qué aporta V2.2:** elimina la contradicción del prompt, que con la versión anterior sí produjo un SIN_EFECTO evitable (O18 en GRA-R2 y en R_O).
- **Las retiradas que ofrece V2.2 son válidas:** las 4 de O18 y O19 lo son para los validadores reales.

### Sobre planes generados por la IA

MEJ-G1 (V2.2) y MEJ-C (V2.1) dan lo mismo: 11 aplicadas y 1 respaldo, el error del proveedor en O18/ciclo 1, que ocurrió en ambas. Pertinencia: ACC 9, CONS-J 2, ALT 1.

## 5. Oráculo v1.4: celdas FALLO investigadas

| Corrida | Celda | Causa | Clase |
|---|---|---|---|
| MEJ-C y MEJ-G1 | O10, A1 | Sesión del sábado con 2 ejercicios y 6 series (QINS) en un plan válido. Ocurre igual con PR-1.7 y PR-1.8 | Comportamiento de la versión vigente del modelo; **no atribuible a la variante**. Es una observación de calidad, no una violación del backend |
| MEJ-G1 (G1C) | O04, A5 | El oráculo empareja por posición y lee como «sustituto de otro patrón» un reordenamiento. Los cambios reales son 520→128 y 555→315, del mismo patrón (ELBOW_FLEXION), más bajadas de series. V8C lo aceptó | **Artefacto del instrumento**, la limitación conocida del emparejamiento posicional |

## 6. Decisión (reglas del §6 del protocolo)

**PR-1.8:**
- **Las reglas se cumplen formalmente:** las primeras válidas suben, ningún respaldo es atribuible a la variante y DOSIS_LIMITADA no aumenta.
- **Pero hay una sola corrida y p = 0,45.**
- **Recomendación:** activar las dos líneas de **V15 y V26**, que son las que muestran el efecto, y **retirar o reformular la línea de dosis** antes de adoptarla. Por ejemplo: «si la dosis viene fijada por la clase (GAIN_MUSCLE), acércate con más colocaciones, nunca con más series».

**V2.2:**
- Se cumplen las reglas (0 SIN_EFECTO evitables, sin más rechazos ni INC, 0 cambios no autorizados), **sin efecto medible** con la versión actual.
- **Recomendación:** adoptar la corrección de factibilidad de ACORTAR como **corrección de coherencia**. La línea ante dolor no cambia nada y se puede quitar.

**Valor por defecto:** ninguno se cambia en esta rama. Activar algo queda para la decisión del responsable, en una PR aparte.

## 7. Limitaciones

- **Muestra:** 19 perfiles y 10 ciclos; una corrida por condición con la versión vigente (la réplica G2 se sustituyó por el control, E-1).
- **Proveedor:** la versión no se puede fijar desde el backend y cambió durante el proyecto, y la latencia de la versión nueva provoca algún timeout con el límite de 150 s.
- **Calidad deportiva:** ni la dosis ni la pertinencia se han validado profesionalmente.
