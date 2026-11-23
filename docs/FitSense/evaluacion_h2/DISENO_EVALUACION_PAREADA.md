# Diseño de la evaluación pareada A/B (fases 3 y 4)

## Problema

Hay que medir con la IA real si PR-1.9 (H2 encendido) mejora la generación frente a PR-1.7 sin empeorar nada más. Hay que comparar propuestas reales de la IA y no solo el resultado final.

## Brazos

| | A · control | B · tratamiento |
|---|---|---|
| Prompt | PR-1.7 (`contrato-v15=false`, la configuración evaluada) | PR-1.9 (`contrato-v15=true`) |
| Todo lo demás | **Idéntico**: catálogo `FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1.1`, política PLAN-REV-2.0, validadores V1–V27 y V8C de `generation` + #69, proveedor `openai/gpt-5-structured` en Replicate, modelo `gpt-5-mini`, `reasoning_effort=medium`, hasta 3 intentos, mismos timeouts | Idéntico |

## Muestra

**24 casos pareados (48 generaciones)**, definidos en `EvaluacionPareadaH2IT.casos()` y en `MATRIZ_GENERACION_REAL.xlsx`. Para cada uno de los 4 objetivos hay 6 perfiles:

| Plantilla | Nivel | Lugar | Días | Minutos | Restricciones |
|---|---|---|---|---|---|
| 1 | Principiante | Gimnasio | 3 (L-X-V) | 45 | No |
| 2 | Principiante | Casa | 4 | 60 | No |
| 3 | Intermedio | Gimnasio | 4 | 60 | No |
| 4 | Intermedio | Casa | 3 | 45 | No |
| 5 | Intermedio | Gimnasio | 5 | 60 | **Sí**: sin impacto alto, suelo, rodillas, colgarse ni brazos arriba |
| 6 | Principiante | Gimnasio | 4 (L-J-V-D) | 60 | No (réplica del caso 39 en la política vigente) |

**Emparejamiento:**

- Cada caso crea dos participantes sintéticos idénticos, uno por brazo.
- El orden se alterna (impar A→B, par B→A) para repartir cualquier deriva del proveedor.
- Los casos corren uno detrás de otro, sin concurrencia.

**Cobertura comprobada sin coste:**

- Los 24 llegan a la IA; ninguno se descarta por la comprobación previa de factibilidad (ensayo).
- Todos tienen sesiones donde aplica la regla de la mitad.

## Camino evaluado

Es el de producción, `GeneracionInicialService.generar`:

1. bloqueo por persona (R-5) y límite;
2. `prepare`;
3. `generate` (IA → validación → hasta 3 intentos → respaldo);
4. `persist`.

**Los espías no cambian nada.** Solo copian lo que entra y sale de Replicate. Fuera de ENSAYO no sustituyen ninguna respuesta.

## Dos etapas, siempre por separado

| Etapa | Qué se registra | Fichero |
|---|---|---|
| **Propuesta de la IA** (cada predicción) | Caso, brazo, usuario, intento, `prompt_version`, tiempo, error, salida cruda y motivos de rechazo de ese intento | `predicciones.csv`, `salidas/`, log de rechazos |
| **Resultado del sistema** | Publicado por la IA, **respaldo** del motor de reglas, NO_FACTIBLE, error o corte; `prompt_version` de la instantánea persistida; **validador independiente** sobre lo publicado, leído de la base | `casos.csv` |
| **Proveedor** | `prediction_id`, estado, tokens, `predict_time` y USD (GET sin coste) | `metricas_replicate.csv`, `tiempos.log` |

**Un respaldo nunca cuenta como propuesta válida de la IA.** Se marca como tal en `origen=RULE_ENGINE`.

## Métricas (fase 4)

Son las 12 de la orden, con fuente y análisis en `MATRIZ_GENERACION_REAL.xlsx`, hoja «Métricas»:

1. Porcentaje de propuestas válidas al primer intento.
2. Rechazos V15, en total y por la regla de la mitad.
3. Rechazos por otros validadores, por código.
4. Reintentos.
5. Predicciones consumidas.
6. Tiempo por predicción.
7. Tiempo total hasta un plan válido.
8. Planes publicados (IA o respaldo).
9. NO_FACTIBLE.
10. Errores técnicos.
11. Costo.
12. Violaciones de seguridad en lo publicado.

**Métrica primaria:** casos con al menos un rechazo V15 de la regla de la mitad. Se compara A frente a B con la prueba de McNemar exacta, porque los datos son pareados.

**Advertencia de potencia.** En las campañas reales anteriores, la regla de la mitad apareció en 5 de 112 rechazos de generación, y en 2 fue la única causa. Con 24 parejas es probable que A produzca muy pocos casos de ese rechazo. Si A tiene menos de 3, la métrica primaria será **inconclusa** por construcción (`CRITERIOS_ACEPTACION_H2.md`).

## Adaptación

H2 no toca la adaptación (`PRUEBAS_H2_SIN_IA.md`), así que **no hay brazo B en la adaptación**. La adaptación con IA real se evalúa una sola vez, con la configuración A:

- arnés `CampanaReplicateRealIT`, ahora con tope duro;
- escenarios de `MATRIZ_ADAPTACION_REAL.xlsx`: 4 perfiles en modo adaptación (9 semanas) y 2 en modo progresión (10 semanas);
- cubre cumplimiento completo, parcial, sesiones omitidas, baja adherencia, fatiga, dolor (con regeneración P-03 y NO_FACTIBLE si se produce) y progresión autorizada.

## Arnés y ensayo sin coste

`EvaluacionPareadaH2IT` tiene tres modos:

- **Sin propiedad:** se omite (CI).
- **`ENSAYO`:** proveedor sustituido por una respuesta **SIMULADA**, etiquetada en todos los ficheros.
- **`REAL`:** exige `autorizado=SI` y `tope-usd`.

Ensayo hecho, sin Replicate:

- 48 casos, 144 predicciones simuladas;
- los 48 terminan en el respaldo, marcado y válido;
- `prompt_version` correcto por brazo;
- **tope verificado**: con un máximo de 10, no empieza una pareja que no quepa entera.

**No son resultados de la IA.**

## Pendientes

La autorización de la fase pagada (`PRESUPUESTO_REPLICATE.md` y `PLAN_EJECUCION_REPLICATE.md`).
