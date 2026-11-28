# 05 · Incidencias, riesgos y limitaciones

## 1. Incidencias durante las pruebas reales

| # | Incidencia | Causa | Efecto | Tratamiento |
|---|---|---|---|---|
| I-1 | La primera corrida G (`G5M_G_BLOQUEADA_RESERVA`) se bloqueó en la 5.ª llamada | **Error de configuración del arnés, mío.** La reserva por predicción (0,08 USD) era menor que la cota de los prompts grandes (O03: 192.454 caracteres, cota 0,0803) | El control de presupuesto paró todas las llamadas siguientes. Esos perfiles se generaron con el motor de reglas, registrado como `RESPALDO/PROVEEDOR`: el control funcionó como debía | Corrida invalidada para la comparación y conservada como evidencia. Se lanzó P y se detuvo a mano tras 1 llamada (`G5M_P_ABORTADA`), anotada a mano en el libro de gasto |
| I-2 | La segunda corrida G (`G5M_G`) se bloqueó en O09 | La reserva subió a 0,10, pero el prompt de O09 tiene 296.281 caracteres (cota 0,1062) | O09 (intento 2) a O14 pasaron al motor de reglas por `PROVEEDOR` o por el cortacircuitos | Para la comparación solo se usan O01–O08 de esa corrida. O09–O14 se repitieron en `G5M_G_COMPLEMENTO`, con reserva 0,13 (el prompt máximo observado es de unos 300.000 caracteres) |
| I-3 | Replicate no informa tokens para `gpt-5-mini` | Del proveedor (`01` §1) | No se puede medir el coste real por predicción desde la API | Se registra la cota superior (`coste_cota_usd`) y una estimación mínima. El gasto real solo se ve en el panel de facturación de Replicate |

**Ninguna incidencia la causó el backend ni el modelo**:
- 0 errores del proveedor en las llamadas hechas;
- 0 respuestas que no fueran JSON interpretable como contrato;
- 0 tiempos agotados.

## 2. Hallazgos técnicos

### H-1 · Calidad de la propuesta inicial con `reasoning_effort=low`

Con la configuración inicial que fijaba la orden, solo **5 de 19** propuestas fueron válidas a la primera, frente a 12–13/19 con `gpt-5-structured` y `medium` (`04`).

- **Causa principal: V24.** El modelo recorta dosis, por ejemplo 2×10 donde 4D v2 exige 3×10.
- **Segunda causa: V15.** Reparto de grupos, con 4–5 de 6 ejercicios en el mismo grupo.

Los validadores rechazaron **todas** las propuestas defectuosas y ninguna llegó al usuario. El precio es que 6 de 19 planes salieron del motor de reglas (`RULE_ENGINE`), con su origen registrado.

Por eso, con los resultados medidos, se probó `reasoning_effort=medium` (`04` §3), como permite la orden.

### H-2 · Ejercicio DURATION sin series en una adaptación (O04, oráculo A5)

**Qué pasó:**
- La adaptación de O04 (ciclo 1, `VARIANTE_MAS_SENCILLA_O_CARGA`) publicó la sesión 52 con dos ejercicios DURATION (1133 y 1309) con `planned_sets = NULL`. Antes tenían 3 y 2 series.
- El trabajo de la sesión **bajó** de 696 s a 344 s. V8C la aceptó (`APLICADA`).

**Por qué el oráculo marca FALLO:**
- El oráculo congelado (v1.4, sin modificar) cuenta como «aumento» cualquier cambio de un campo que no sea una reducción con los dos valores presentes.
- Por eso marca **FALLO** en A5 («más trabajo»).

**Por qué el plan sí cumple el contrato:**
- V4 (`ck_pwe_*`) admite DURATION sin series: `planned_sets IS NULL OR planned_sets >= 2`, con su rama DURATION.
- `SessionFixJsonSchema` declara `planned_sets` *nullable*.

**No es nuevo de `gpt-5-mini`:**
- La línea base con `gpt-5-structured` (`GRA_R2_G`) ya publicó 4 filas DURATION sin series.
- En `G5M_G` son 8 filas: la misma sesión adaptada, copiada a las semanas siguientes.
- En `G5M_G_COMPLEMENTO` hay 4 más.

**Decisión pendiente de revisión, no tomada en esta PR:**
- La orden prohíbe modificar V8C y los validadores. Tampoco se modifica el oráculo.
- Queda por decidir si DURATION sin series debe seguir admitido.

**Riesgo para Mobile:** que muestre bien un DURATION sin series. El motor de reglas nunca lo genera, pero la IA sí, con los dos modelos.

## 3. Riesgos

| Riesgo | Mitigación actual |
|---|---|
| Sin salida estructurada del proveedor, la forma del JSON depende de la instrucción | Se valida entera: ensamblador y V1–V27 / V8C. Medido: 0 respuestas con forma inválida en todas las llamadas reales |
| Respuesta cortada o vacía si el razonamiento agota `max_completion_tokens` (16.000) | Se rechaza como intento fallido. Medido: 0 casos, con un máximo de 4.634 caracteres de salida visible |
| Coste real desconocido por predicción | Cota superior por llamada (`REPLICATE_CONSUMO`). Revisar el panel de facturación de Replicate |
| Más respaldo por reglas que con `gpt-5-structured` | El respaldo es determinista y está validado, y su origen se registra. Ver la recomendación de `04` |
| El endpoint estructurado podría volver a funcionar y alguien configurarlo sin querer | Solo se usa por configuración explícita, con WARN al arrancar. No hay conmutación automática |

## 4. Limitaciones de esta evaluación

- **Una sola repetición por configuración**, frente a dos de GRA. La variabilidad entre corridas del modelo es alta: GRA midió un Jaccard de ejercicios con mediana 0,36. Las diferencias de pocos perfiles no son concluyentes.
- **`medium` se probó en un subconjunto** (los 5 perfiles G que acabaron en respaldo con `low` y el bloque P completo), no en los 20. El presupuesto, contado por cota, no alcanzaba para los 20 con margen.
- **El gasto real** no se puede verificar desde la sesión (`I-3`).
- **No se desplegó nada.** No se probó con la app Mobile ni en Railway.
