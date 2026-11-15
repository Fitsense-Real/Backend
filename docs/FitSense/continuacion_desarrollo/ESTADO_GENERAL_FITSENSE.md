# Estado general de FitSense (15/11/2026)

## Problema

Este documento resume dónde está el sistema tras integrar el catálogo v1.1 y qué impide avanzar hacia la evaluación de la tesis.

## Versión integrada

| Elemento | Estado |
|---|---|
| `generation` | **`0d112785a5df70d686da36c80e814b40ca077cd6`**: catálogo `50849d0` + V57 (`docs/FitSense/integracion/catalogo_v11/`) |
| Catálogo | `FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1.1 — INTEGRADO`: A 919, B 273, C 27, D 157. **1.192 utilizables** (A + B) |
| Migraciones | V1…V57 (60) |
| Política y prompts | PLAN-REV-2.0, generación PR-1.7 y adaptación V2.1 (configuración evaluada) |
| Interruptores | `docs/FitSense/integracion/catalogo_v11/ESTADO_INTERRUPT0RES.md` |
| Despliegue | **Ninguno.** Railway, Neon, `main` y Flutter sin tocar |

## Bloques de esta fase

| Bloque | Estado | Documento | PR |
|---|---|---|---|
| A · H2, contradicción entre el prompt y V15 | Diagnosticado y corregido detrás de un interruptor apagado | `DIAGNOSTICO_H2.md`, `CORRECCION_H2.md` | [#69](https://github.com/Fitsense-Real/Backend/pull/69), borrador sin fusionar |
| B · Tiempo de generación y espera en Flutter | Diagnosticado; diseño asíncrono propuesto y **no implementado** | `DIAGNOSTICO_TIEMPOS_GENERACION.md`, `PROPUESTA_FLUJO_GENERACION.md` | — (pendiente de aprobar el diseño) |
| C · Plan de pruebas con la IA real | Listo; **no ejecutado**, USD 0 gastados | `PLAN_PRUEBAS_IA_REAL.md` | — |
| D · Preparación de la validación de la tesis | Inventario y opciones metodológicas | `PREPARACION_VALIDACION_TESIS.md` | — |
| E · Trazabilidad de las omisiones (V57) | **Integrada** en `0d11278` | `docs/FitSense/integracion/catalogo_v11/INTEGRACION_V57.md` | [#68](https://github.com/Fitsense-Real/Backend/pull/68) |

## Bloqueadores, por orden

1. **Decisión metodológica de la adherencia** (opción A o B) antes de recoger datos.
2. **Espera de la app.** La app corta a los 30 s y la generación tarda de 60 a 350 s. Sin el flujo asíncrono o un cambio en Flutter, el primer plan da error al participante.
3. **H2.** Los reintentos por la regla de la mitad de V15 encarecen la latencia. Hay que decidir la evaluación pareada (≤ USD 5).
4. **Prueba real con Replicate** de la versión integrada (≤ USD 8). Necesita autorización.
5. **Despliegue en un entorno de evaluación.** Confirmar la rama en Railway y hacer copia de Neon antes de aplicar V47–V57. Necesita autorización.
6. **Revisión profesional** de las 312 decisiones abiertas, o declararlas como limitación.
7. **Proceso ético** con participantes. Está fuera del alcance técnico.

## Restricciones respetadas

No se hizo nada de esto:

- desplegar;
- tocar bases reales;
- gastar créditos de Replicate;
- probar con participantes;
- cambiar la fórmula de adherencia;
- relajar V15, V20, V24, V25, V8C, D8 o P-03;
- activar V53 A, V53-B, #64 o P-03 por colocación en producción;
- liberar ejercicios contenidos;
- auditar de nuevo el catálogo;
- fusionar #69.

**Validación profesional:** no se afirma ninguna.

## Pendientes

Las decisiones del propietario de la lista de bloqueadores.
