# Validación de P-03 por colocación

Todas las pruebas se ejecutan con la configuración de la candidata (`fitsense.regeneracion.p03-por-colocacion=true` en las pruebas) sobre el commit `12c6897`. Resultado de la regresión completa: ver `REGRESION_INTEGRADA_FINAL.md`.

## 1. Las 10 pruebas obligatorias

| # | Caso | Prueba | Resultado |
|---|---|---|---|
| 1 | Contraejemplo de restauración indebida | `P03ComparacionPorColocacionIT#contraejemplo` (9850): la medida tiene una estructural reducida a 2 series; la regenerada la devuelve a 3 el mismo día y quita otra colocación; el total no sube | Con la política de solo total, **la acepta** (se reproduce el defecto) |
| 2 | La misma prueba con la protección | ídem, regla encendida | **La rechaza**: «P-03 nivel 2 (por colocación, MANTENIDA): el ejercicio … sube de 2x10 a 3x10» |
| 3 | Reducción legítima | `ComparacionDosisPorColocacionTest#reduccionLegitima` | Ningún aumento |
| 4 | Progresión autorizada | `#progresionAutorizada` (medida 3x12 progresada, regenerada 3x10 base), más `ProgresionIT` y `ProgresionRev2IT` (regresión, regla encendida) y `Reincorporacion4DTest` y `ReincorporacionTrasProgresionTest` | Las progresiones siguen funcionando (24 + 24 pruebas pasan). **Un escenario cambia:** dolor **después** de una progresión que llega a P-03 nivel 2 (§3) |
| 5 | Ejercicio repetido en distintos días | `#repetidoEnDistintosDias` y `P03ComparacionPorColocacionIT#cambioDeDia` (9851: la reducida reaparece otro día a 3 series) | Emparejado por día cuando corresponde; si cambia de día, se compara con la **menor** dosis medida y **se rechaza** (SIN_CORRESPONDENCIA) |
| 6 | Sustitución por dolor | `#sustitucionPorDolor` | El sustituto en el puesto del retirado se compara con la dosis del retirado. Con la misma dosis pasa; con más, aumento (SUSTITUCION). Sin exclusión, es NUEVA |
| 7 | Adaptación localizada | `#adaptacionLocalizada`, más `RetiradaLocalizada*IT` en la regresión | Ningún aumento; publican igual que en v1 |
| 8 | Adaptación expandida | `#adaptacionExpandida`, más las pruebas de adaptación de la regresión | Ningún aumento |
| 9 | Regeneración NO_FACTIBLE | `#regeneracionNoFactible`, más `RegeneracionPorColocacionRev2IT#halGm01ConContencion` (9717) y `#sustitutoDeMantenimientoSinMasTrabajo` (9703) | Con un aumento, P-03 no publica. Los NO_FACTIBLE siguen igual |
| 10 | Semana sin cambios | `#sinCambios` | Todo MANTENIDA, ningún aumento |

Además, `#segundosYVolumen`: subir segundos o series × repeticiones también es un aumento, aunque bajen las repeticiones.

## 2. Regresión completa con la regla encendida (`12c6897`)

La regresión completa (`REGRESION_INTEGRADA_FINAL.md`) se ejecutó con la regla encendida en todas las pruebas:

- 754 pruebas de integración y 1540 unitarias;
- **0 fallos.**

El registro `P03_POR_COLOCACION` muestra **13 evaluaciones** por colocación, es decir, regeneraciones de nivel 2 en las que la comprobación del total no encontró subida. La evidencia está en `evidencia/p03/regeneraciones_v1_vs_v11.txt`.

| Evaluaciones | Origen | Aumentos | Resultado |
|---|---|---|---|
| 2 | `P03ComparacionPorColocacionIT` (contraejemplo 9850 y `cambioDeDia` 9851) | 1 cada una | **Bloqueadas, como se espera** |
| 9 | `CampanaP123Cohorte02IT`, `PoliticasP123IT` (4) y `LinajeIntegridadIT` (2), entre otras | 0 | Igual que en v1 |
| 2 | Usuario 17: `ProgresionIT#p03Nivel2TrasProgresion` y su copia en `ProgresionRev2IT` | 2 | **Cambia: PUBLICADO en v1 → NO_FACTIBLE en v1.1** |

**Clasificación de las colocaciones regeneradas** (las 11 evaluaciones de regeneraciones reales):

| Tipo | Cuántas por evaluación |
|---|---|
| MANTENIDA | 0–6 |
| SUSTITUCION | 1–6 |
| SIN_CORRESPONDENCIA | 0–5 |
| NUEVA | 8–40 |

La regeneración parte de cero con dosis base, así que la mayoría de sus colocaciones son nuevas.

## 3. El escenario que cambia (usuario 17)

**Escenario.** Es una prueba de auditoría de `ProgresionIT`, que admite los dos desenlaces y comprueba la seguridad en ambos:

1. una semana con una progresión autorizada;
2. un dolor sin identificar en la sesión del paso, que excluye sus 7 ejercicios (122, 295, 773, 775, 1054, 1098 y 1263);
3. el nivel 1 se fuerza a no tener sustituto;
4. P-03 nivel 2 regenera la semana con el motor de reglas.

| | v1 (`836a1f5`, solo total) | v1.1 (`12c6897`, por colocación) |
|---|---|---|
| Resultado | **PUBLICADO** (plan 76) | **NO_FACTIBLE** |
| Trabajo total | Repeticiones 503 → 476, segundos 1620 → 1560 (baja) | La comprobación del total pasa |
| Dosis de los ejercicios presentes en las dos semanas | Sin cambios | — |
| Excluidos publicados | Ninguno | Nada publicado; la exclusión P-02 se conserva |
| Motivo | — | «P-03 nivel 2 (por colocación, SUSTITUCION): el ejercicio 253 del lunes sube de 2x12 (ejercicio 295) a 3x120s [series 2→3 …]» |

**Por qué se bloquea.** En la semana regenerada, el lunes, en el puesto del 295 (retirado por dolor y medido a 2x12), aparece el 253 a 3 series de 120 s, su dosis base. Para la regla, eso es una sustitución con **más series** que la colocación que reemplaza, así que la bloquea.

Las dimensiones de segundos del mensaje («0→120», «0→360») vienen de que el 295 se mide en repeticiones y el 253 en segundos. **La subida que cuenta es la de series (2 → 3).**

**Cómo se clasifica:**

- **No es un defecto de seguridad:** no se publica nada, se conserva la exclusión y P-DOLOR-PUB sigue actuando.
- **No es un fallo de prueba:** la prueba admite los dos desenlaces y pasa.
- **Es una pérdida de factibilidad, explicada.** Una colocación que no se puede comparar de forma fiable (otro ejercicio, otra modalidad, emparejado por su posición) recibe el comportamiento conservador documentado (`DECISION_P03.md` §3), como pide la orden.
- **Precio:**
  - las regeneraciones de nivel 2 publicadas en la regresión bajan de **10 ejecuciones (7 escenarios)** a **8 (6)**;
  - ninguna NO_FACTIBLE pasa a publicarse.

**Ajuste posible, no aplicado:** emparejar una SUSTITUCION solo cuando la modalidad o el patrón coinciden, y tratar las demás como NUEVA (solo el total). Publicaría este caso, pero **relaja** la protección justo donde la comparación es menos fiable, en contra de la orden. Queda como decisión del propietario antes de activarla en producción.

## 4. Los 40 escenarios HALL-GM-01

Con la regla encendida, el resultado es **idéntico, columna por columna**, al de v1 con la regla apagada:

- **Escenarios que cambian: 0.**
- 35 publicadas, sin aumentos ni por colocación ni en total.
- 5 NO_FACTIBLE: 9717, 9728, 9733, 9734 y 9740 (V24 y P-03 «sube el trabajo»).
- Trabajo registrado en lo que dolió: 0.

Evidencia: `evidencia/hall_gm_01/hall_gm_01_40_v11_p03_por_colocacion.csv`. Se repite sobre el commit exacto en la regresión (`REGRESION_INTEGRADA_FINAL.md`).

En ninguno de los 40, P-03 nivel 2 llega a la comparación por colocación: la comprobación del total ya decide.

## 5. Conclusión

| Criterio | Resultado |
|---|---|
| Aumentos indebidos publicados | 0 |
| Restauraciones sin autorización | El contraejemplo y el cambio de día se bloquean; ninguna se publica |
| Progresiones válidas | Siguen funcionando (`ProgresionIT`, `ProgresionRev2IT`, `ProgresionCienPorCienIT`, reincorporación) |
| Generación inicial (168, 312, 72) | Sin cambios (no pasa por P-03) |
| HALL-GM-01 (40) | Sin cambios |
| Pérdida de factibilidad | **1 escenario** de P-03 nivel 2 (usuario 17), de PUBLICADO a NO_FACTIBLE, explicado (§3) |
| Otros validadores modificados | Ninguno |

**P-03 por colocación queda validada para la candidata**, con ese cambio de resultado documentado. No es una regresión de seguridad: es el precio conservador de no poder comparar una colocación de forma fiable.

En producción sigue apagada. Aceptar ese precio forma parte de la autorización para activarla.
