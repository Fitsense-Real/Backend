# Validación de la reincorporación con las dosis 4D v3.2 y v3.3

**Fecha:** 31/10/2026, §7 de la orden. Las pruebas son deterministas, sin Replicate.

**Las dos excepciones aprobadas:**
- **4D v3.2:** el relleno de zona pequeña (`lower legs`, `waist`) va a 2×13, y a 3×13 **solo** si sin esa serie la sesión no llega al mínimo de V20.
- **4D v3.3:** el bíceps de relleno estándar en las sesiones PULL va a 2×10, y a 3×10 con la misma condición.

## Resultados

| Comprobación | Resultado | Prueba |
|---|---|---|
| Una ampliación autorizada no es una dosis base nueva: el relleno de zona pequeña retirado con 3×13 vuelve con **2×13 / 90 s** | ✔ | `Reincorporacion4DTest.zonaPequenaVuelveConLaBase` |
| El bíceps de relleno retirado con 3×10 vuelve con **2×10 / 90 s** | ✔ | `Reincorporacion4DTest.bicepsVuelveConLaBase` |
| No se restaura una dosis histórica: retirado con 2×15 (progresado), vuelve con 2×13 | ✔ | `Reincorporacion4DTest.noRestauraLaHistorica` |
| La progresión desde la ampliada respeta los límites: 3×14 y 3×15 sí; 3×16 (más de +2 repeticiones) y 4×13 (más de 3 series) no | ✔ | `Reincorporacion4DTest.progresionDesdeLaAmpliada` |
| El bíceps ampliado progresa como relleno: hasta 3×12, nunca 4 series | ✔ | `Reincorporacion4DTest.progresionDelBicepsAmpliado` |
| V24 acepta la serie adicional solo si hace falta, y rechaza la que sobra | ✔ | `Adenda4Dv32IT`, `RespaldoSesentaMinutosIT` (`seriesAdicionales` en las 24 combinaciones y `casaPullNecesitaLaSerieDelBiceps`) |
| El historial distingue la ampliación de la reincorporación | ✔ | La validación independiente las clasifica por separado (ver abajo) |
| Reincorporación real con dosis 4D, en el escenario con IA real | ✔ | 480 vuelve con 2×10 / 90 s (RELLENO_ESTANDAR) |

## Cómo lo garantiza el código

**`ProgresionResolver.exacta`:**
- Para GAIN_MUSCLE toma `MvpDosePolicy.dosisPara(slot, grupo).series()`, es decir, la dosis de la clase.
- **Nunca** toma `seriesMax` ni la dosis que tenía el ejercicio al retirarse.
- La serie adicional depende de la duración de la sesión (`seriesMaxEn` + V24): no la decide la reincorporación. Si al volver la sesión no llega al mínimo, la regla vigente de 4D v3.2/v3.3 la decide en su propia revalidación.

**`ProgresionResolver.rango`:**
- Reconoce como punto de partida una dosis ampliada autorizada, mediante `admite4D` con el origen HEREDADA.
- Limita la progresión a +2 repeticiones y a `SERIES_MAX_AMPLIACION` = 3 series en el relleno.

**La validación independiente del simulador** (`ValidacionIndependiente`):
- La serie adicional de 4D es **BASE**: la juzga V24 con su condición de necesidad.
- Un paso registrado en V32 es **PASO**, y su copia es **HEREDADA**.
- La vuelta es **REINCORPORACION**: se valida como dosis base.
- Una dosis que ninguna regla explica queda **SIN_AUTORIZACION**, y la semana es INVALIDA (`ProgresionIT.validacionIndependienteDistingue`).

## Sin cambios

- Las políticas 4D v3.2 y v3.3.
- V24 y V8C.
- Los límites de volumen y progresión.

Para las pruebas solo se pasaron a visibilidad de paquete `exacta` y `rango`, sin cambiar su lógica.
