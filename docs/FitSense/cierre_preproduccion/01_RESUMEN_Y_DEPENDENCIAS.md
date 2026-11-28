# 01 · Resumen y dependencias del cierre de preproducción del backend

- **Orden:** implementación del cierre de preproducción (27/11/2026).
- **Base:** `generation` en `77b9df3d6bd26f6196514c084d40bd952db77adc`. No ha avanzado desde la auditoría.
- **Auditoría de referencia:** rama `audit/preproduccion-backend`, `docs/FitSense/preproduccion/01–06`.
- **Restricciones:** **ninguna PR fusionada, ningún despliegue, cero llamadas a Replicate, ningún dato real tocado.**

## 1. Verificación inicial (§2 de la orden)

| # | Verificación | Resultado |
|---|---|---|
| 1 | SHA de `generation` | `77b9df3`: el mismo que se auditó; no hubo que comparar diferencias |
| 2 | PR abiertas | #89 (ampliación V59, V66), #39, #33, #32, #30, #29, #28 y #17 (experimentos marcados NO FUSIONAR) y #1 |
| 3 | Ampliación V59 | Sigue en `feature/v59-recovery-all-ai-flows`, ahora como **PR #89** (6 commits, CI en verde, sin fusionar) |
| 4 | V66 | **Reservada por #89** (`V66__operaciones_ia_sincrona_y_ciclo_semanal.sql`). Ninguna PR de esta orden crea migraciones |
| 5 | Auditoría | Revisada. Se corrigió un error suyo: **P2-06 ya estaba cubierto** por `ContratoErroresApiIT.recursosAjenos` |
| 6 | *Prompt* de generación | **PR-1.7** (`PlanPromptBuilder.VERSION`): sin cambios |
| 7 | Validadores y reglas de seguridad | Sin cambios desde `77b9df3`; ninguna PR los toca |
| 8 | Contratos de Mobile | `CONTRATOS_FINALES_PARA_FLUTTER.md`, `CONTRATO_PUSH_MOBILE.md` y los endpoints actuales. Los cambios propuestos son compatibles hacia atrás con la configuración por omisión |
| 9 | Cambios concurrentes | #89 toca `WeeklyCycleService`, `GeneracionInicialService`, `PlanGenerationOperationService`, `DetectorEventosPush`, `application.properties` y `pom.xml`. Las PR de esta orden se diseñaron para no chocar: B3 no toca `WeeklyCycleService.ejecutar`, y B5 solo añade un *setter* opcional en `PlanGenerationOperationService` |

## 2. PR creadas (ninguna fusionada)

| Bloque | PR | Rama | Base | Prioridad | Contenido |
|---|---|---|---|---|---|
| B1 | [#90](https://github.com/Fitsense-Real/Backend/pull/90) | `preproduccion/b1-seguridad-logs-limites` | `generation` | P0 | Logs sin datos sensibles (S-1), límites de login, registro y consentimientos, sin Neon por omisión, guardas fijas en `prod` |
| B2 | [#91](https://github.com/Fitsense-Real/Backend/pull/91) | `preproduccion/b2-consentimientos-participacion` | `generation` | P0 | Consentimiento informado versionado, documentos legales publicados, exigencia probada, participación A/B |
| B3 | [#92](https://github.com/Fitsense-Real/Backend/pull/92) | `preproduccion/b3-cierre-semanal-recuperable` | `generation` | P0 | Cierre semanal recuperable y control optimista al publicar |
| B4 | ver `10` | `preproduccion/b4-operacion-correo-respaldos` | `generation` | P0 | Correo, copias y restauración verificables, lista de Railway, y estos documentos 01–10 |
| B5 | [#93](https://github.com/Fitsense-Real/Backend/pull/93) | `preproduccion/b5-operacion-trazabilidad` | `generation` | P1 | Indicadores y alertas, exportación seudonimizada, trazabilidad, límite asíncrono |
| B6 | [#94](https://github.com/Fitsense-Real/Backend/pull/94) | `preproduccion/b6-simulacion-12-semanas` | **B3** (apilada) | P1 | Simulación de 12 semanas E12-01..18 |

Los SHA de cada PR están en `10_ESTADO_FINAL_Y_BLOQUEANTES.md` §1.

## 3. Dependencias

```
#89 (V59/V66, otra orden) ──► B3 (#92) ──► B6 (#94, apilada sobre B3)
B1 (#90), B2 (#91), B5 (#93), B4: independientes entre sí y de B3.
```

- **#89 antes de B3.** Son complementarias y la combinación se probó en local: 37/37 pruebas.
- **B6 sobre B3.** Usa el planificador recuperable y el control de versión.
- **Sin conflictos.** Las cinco PR de código se fusionaron en una rama local temporal (sin subir) **sin conflictos** y se ejecutó la regresión completa (`09`). En la primera prueba hubo un conflicto trivial en `application.properties` entre B1 y B3, que se resolvió en B3 llevando su bloque al final del fichero.
- **Decisiones externas que bloquean la activación:**
  - documentos aprobados (P-6);
  - ~~participación A/B (P0-04)~~: resuelta el 28/11/2026, opción A (`integracion_final_estudio/01`);
  - protocolo de datos D-1..D-7 (P0-07);
  - proveedor de correo (P-10);
  - base y retención de preproducción (P-14);
  - autorización de despliegue.

## 4. Mapa de documentos

| Documento | Contenido |
|---|---|
| `02_SEGURIDAD_Y_LOGS.md` | B1 |
| `03_CONSENTIMIENTOS_Y_CONTRATOS.md` | B2 y el contrato de consentimientos para Mobile |
| `04_CIERRE_SEMANAL_Y_V59.md` | B3, coordinación con #89 |
| `05_CORREO_Y_RECUPERACION.md` | B4.1 |
| `06_OPERACION_RAILWAY_Y_RESPALDOS.md` | B4.2 y B4.3, más `railway_preproduccion.env.ejemplo` |
| `07_TRAZABILIDAD_Y_MONITOREO.md` | B5 |
| `08_SIMULACION_12_SEMANAS.md` | B6 |
| `09_RESULTADOS_PRUEBAS.md` | Regresión y CI |
| `10_ESTADO_FINAL_Y_BLOQUEANTES.md` | Matriz final, bloqueantes y riesgos |

Los documentos 02, 03, 04, 07 y 08 van en la PR de su bloque. Los documentos 01, 05, 06, 09 y 10 y la evidencia van en B4.
