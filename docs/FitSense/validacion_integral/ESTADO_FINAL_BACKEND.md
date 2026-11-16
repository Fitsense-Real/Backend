# Estado final del backend

Fecha: 16/11/2026. No se ha hecho ningún merge ni ningún despliegue. No se ha usado Replicate y no se han tocado datos
reales.

## 1. Ramas y PR

| Rama | Cabeza | Contenido | PR |
|---|---|---|---|
| `generation` | `2578719` | Base: catálogo v1.1 y V57 | — |
| `fix/reparacion-linaje-determinista` | cabeza de #73 | La corrección de `ReparacionLinajeIT` (solo la prueba) y `correccion_ci_linaje/` (diagnóstico, corrección del CI, regresión de progresión) | [#73](https://github.com/Fitsense-Real/Backend/pull/73), borrador, base `generation`. CI `verify` en verde. |
| `validacion/backend-integral` | cabeza de la rama | #73, `CicloCompletoHttpIT` y esta carpeta | Sin PR: se abre si se quiere conservar la prueba integral |
| `claude/async-first-plan-generation` | `12a01c6` | V58 | #71, sin cambios en esta orden |
| `fix/async-generacion-resiliente` | `8658089` | V59 | #72, sin cambios en esta orden. Su CI se pone verde con #73 (`CORRECCION_CI.md`). |

## 2. Entregables

| Documento | Ubicación |
|---|---|
| `DIAGNOSTICO_REPARACION_LINAJE.md` | `docs/FitSense/correccion_ci_linaje/` (#73) |
| `CORRECCION_CI.md` | `docs/FitSense/correccion_ci_linaje/` (#73) |
| `REGRESION_PROGRESION.md` | `docs/FitSense/correccion_ci_linaje/` (#73) |
| `AUDITORIA_FUNCIONAL_BACKEND.md` | esta carpeta |
| `MATRIZ_MODULOS_BACKEND.xlsx` | esta carpeta (hojas Matriz, Pendientes y Resultados) |
| `PRUEBAS_CICLO_COMPLETO.md` | esta carpeta |
| `SEGURIDAD_CONSISTENCIA.md` | esta carpeta |
| `CONTRATOS_MOBILE_WEB.md` | esta carpeta |
| `PENDIENTES_PRIORIZADOS.md` | esta carpeta |
| `ESTADO_FINAL_BACKEND.md` | esta carpeta |

## 3. Resultados de las pruebas

| Ejecución | Unitarias | Integración | Fallos |
|---|---|---|---|
| `generation` + #73, local | 1540 (1 omitida) | 759 (57 omitidas, opt-in) | 0 |
| PR #73, GitHub Actions | — | — | 0 (verde) |
| `CicloCompletoHttpIT` | — | 9 | 0 |
| `ReparacionLinajeIT` con 3 semanas de referencia más | — | 3 × 3 | 0 |
| #72 + #73, local (rama temporal, no subida) | 1576 (1 omitida) | 809 (72 omitidas: las 57 de antes y las 15 de `GeneracionAsincronaIT`, opt-in) | 0 |

## 4. Estado por módulo

| Módulo | Estado |
|---|---|
| A. Usuarios y perfil | Completo. Recuperación de contraseña **parcial**: sin proveedor de correo. |
| B. Generación inicial | Completo en el camino síncrono, para los 4 objetivos. El asíncrono está implementado y probado en #71/#72, **sin fusionar**. |
| C. Ejecución | Completo |
| D. Adherencia | Completo, con la fórmula oficial intacta y verificada al centésimo |
| E. Adaptación | Completo (sin H2 ni P-03 activados) |
| F. Progresión y reincorporación | Completo. El linaje es determinista desde #73. |
| G. Trazabilidad y analítica | Completo para el estudio. Panel **parcial**: solo lectura agregada; falta la vista por participante y la exportación. |

## 5. Qué falta para considerar el backend funcionalmente completo

1. **Integrar** #73, #71 y #72, en ese orden y sin desplegar en medio (PD-02).
2. **Proveedor de correo** para la recuperación de contraseña (PD-03).
3. **Confirmar** dos decisiones:
   - qué pasa cuando un slot queda vacío en la generación (PD-07);
   - qué pasa con la sesión bloqueada tras una reparación NO_VALIDA (PD-04).
4. **Endpoints del panel** de administración que el estudio necesite: vista por participante, exportación y
   seguimiento de las generaciones (PD-05, PD-06, PD-10).
5. **Ronda con Replicate real** antes de usar la IA con participantes (PD-11).

Recomendado, aunque no es funcional: estabilizar las 38 pruebas con fecha relativa (PD-01).

## 6. Siguiente paso

Revisar los resultados y decidir:

- la fusión de #73;
- el orden de integración de #71 y #72;
- si se abre una PR para `validacion/backend-integral`;
- las decisiones PD-04 y PD-07.
