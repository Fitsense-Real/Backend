# Validación de P-03 por colocación

Todas las pruebas se ejecutan con la configuración de la candidata (`fitsense.regeneracion.p03-por-colocacion=true` en las pruebas) sobre el commit `12c6897`. Resultado de la regresión completa: ver `REGRESION_INTEGRADA_FINAL.md`.

## 1. Las 10 pruebas obligatorias

| # | Caso | Prueba | Resultado |
|---|---|---|---|
| 1 | Contraejemplo de restauración indebida | `P03ComparacionPorColocacionIT#contraejemplo` (9850): la medida tiene una estructural reducida a 2 series; la regenerada la devuelve a 3 el mismo día y quita otra colocación; el total no sube | Con la política de solo total, **la acepta** (se reproduce el defecto) |
| 2 | La misma prueba con la protección | ídem, regla encendida | **La rechaza**: «P-03 nivel 2 (por colocación, MANTENIDA): el ejercicio … sube de 2x10 a 3x10» |
| 3 | Reducción legítima | `ComparacionDosisPorColocacionTest#reduccionLegitima` | Ningún aumento |
| 4 | Progresión autorizada | `#progresionAutorizada` (medida 3x12 progresada, regenerada 3x10 base), más `ProgresionIT` y `ProgresionRev2IT` (regresión, regla encendida) y `Reincorporacion4DTest` y `ReincorporacionTrasProgresionTest` | Ningún aumento; las progresiones siguen funcionando |
| 5 | Ejercicio repetido en distintos días | `#repetidoEnDistintosDias` y `P03ComparacionPorColocacionIT#cambioDeDia` (9851: la reducida reaparece otro día a 3 series) | Emparejado por día cuando corresponde; si cambia de día, se compara con la **menor** dosis medida y **se rechaza** (SIN_CORRESPONDENCIA) |
| 6 | Sustitución por dolor | `#sustitucionPorDolor` | El sustituto en el puesto del retirado se compara con la dosis del retirado. Con la misma dosis pasa; con más, aumento (SUSTITUCION). Sin exclusión, es NUEVA |
| 7 | Adaptación localizada | `#adaptacionLocalizada`, más `RetiradaLocalizada*IT` en la regresión | Ningún aumento; publican igual que en v1 |
| 8 | Adaptación expandida | `#adaptacionExpandida`, más las pruebas de adaptación de la regresión | Ningún aumento |
| 9 | Regeneración NO_FACTIBLE | `#regeneracionNoFactible`, más `RegeneracionPorColocacionRev2IT#halGm01ConContencion` (9717) y `#sustitutoDeMantenimientoSinMasTrabajo` (9703) | Con un aumento, P-03 no publica. Los NO_FACTIBLE siguen igual |
| 10 | Semana sin cambios | `#sinCambios` | Todo MANTENIDA, ningún aumento |

Además, `#segundosYVolumen`: subir segundos o series × repeticiones también es un aumento, aunque bajen las repeticiones.

## 2. Pruebas de integración sensibles con la regla encendida

Ejecutadas antes de la regresión, sobre el código de `12c6897`:

- `P03ComparacionPorColocacionIT` (2);
- `RegeneracionPorColocacionRev2IT` (6);
- `HerenciaD8IT` (6);
- `ReparacionLinajeIT`;
- `RetiradaLocalizada*IT` (5 clases);
- `PoliticasP123IT` (39);
- `RespaldoConDolorIT` (3).

**Todas pasan.**

**En las 9 regeneraciones de P-03 nivel 2 que pasaron la comprobación del total, la comparación por colocación no encontró ningún aumento.** Su clasificación, según el registro `P03_POR_COLOCACION`:

| Colocaciones regeneradas | Cuántas |
|---|---|
| MANTENIDA | 0–6 |
| SUSTITUCION | 1–6 |
| SIN_CORRESPONDENCIA | 0–5 |
| NUEVA | 8–40 |

La regeneración parte de cero con dosis base, así que la mayoría de sus colocaciones son nuevas.

## 3. Los 40 escenarios HALL-GM-01

Con la regla encendida, el resultado es **idéntico, columna por columna**, al de v1 con la regla apagada:

- **Escenarios que cambian: 0.**
- 35 publicadas, sin aumentos ni por colocación ni en total.
- 5 NO_FACTIBLE: 9717, 9728, 9733, 9734 y 9740 (V24 y P-03 «sube el trabajo»).
- Trabajo registrado en lo que dolió: 0.

Evidencia: `evidencia/hall_gm_01/hall_gm_01_40_v11_p03_por_colocacion.csv`. Se repite sobre el commit exacto en la regresión (`REGRESION_INTEGRADA_FINAL.md`).

En ninguno de los 40, P-03 nivel 2 llega a la comparación por colocación: la comprobación del total ya decide.

## 4. Conclusión

| Criterio | Resultado |
|---|---|
| Aumentos indebidos | 0 |
| Restauraciones sin autorización | El contraejemplo y el cambio de día se bloquean; ninguna en los escenarios |
| Progresiones válidas | Siguen funcionando |
| Pérdida de factibilidad | **Ninguna** medida |
| Otros validadores modificados | Ninguno |

**P-03 por colocación queda aprobada para la candidata.**
