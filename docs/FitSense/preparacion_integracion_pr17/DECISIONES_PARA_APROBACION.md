# Decisiones para aprobación

Nada de lo que sigue está aplicado en producción. Cada punto necesita una **aprobación expresa**.

## 1. Integración

| # | Decisión | Recomendación | Consecuencia si se aprueba | Riesgo si no |
|---|---|---|---|---|
| I1 | Revisar y fusionar el PR borrador de la rama aislada (versionado + V4-C) en `generation` | **Sí, tras revisión humana** | `generation` gana V44 (dos columnas NULL) y la política seleccionable, con **PLAN-HIST-1 por defecto**. Sin cambio funcional mientras no se configure PLAN-V4-0.1 (960/960 idénticos) | La brecha de redondeo de V4 sigue sin poder cerrarse, y el PR #17 sigue arrastrando el versionado |
| I2 | Después de I1, fusionar `generation` en el PR #17 (*merge*, no *rebase*) y repetir la regresión | Sí | El diff del PR #17 se reduce a C2-D, EF, V45 y las opciones. V44 queda una sola vez (simulado: `DEPENDENCIAS_VERSIONADO_V44.md`) | — |
| I3 | Fusionar el PR #17 | **No todavía** | — | Requiere la revisión profesional y las decisiones C, E y P |

## 2. Activaciones (cada una por separado)

| # | Componente | Estado | Recomendación |
|---|---|---|---|
| A1 | **PLAN-V4-0.1** en producción (`fitsense.planning.policy=PLAN-V4-0.1` y `allow-candidates=true`) | Apagado | Candidata a activar **después de** I1 y de un despliegue con PLAN-HIST-1, para confirmar que V44 se aplica sin cambios. Efecto esperado: solo rechaza lo que cae entre el 115 % y el redondeo |
| A2 | **C2-D** (PLAN-C2D-*) | Apagado | **No**, hasta la revisión profesional (e5, G4) |
| A3 | **PLAN-C2D-0.1 sin EF** para usuarios reales | Apagado | **Nunca**: publica PULL sin tracción en los 48 perfiles de peso corporal |
| A4 | **PLAN-C2D-EF-0.1 / PLAN-C2D-V4-EF-0.1** | Apagado | No, hasta la revisión profesional (e2, e3, G1–G3) |
| A5 | **LW-PREF-0.2** | Apagado | No, hasta la revisión profesional (e7, G4) |
| A6 | **REP-SUST-0.1** | Apagado | No: recupera 1 plan y no se recomienda |
| A7 | **NOFACT-0.1** (`no-factible-estructurado`) | Apagado | Después de A4 y con Flutter preparado (caso C) |
| A8 | **Duración estimada** (`duracion-estimada`) y **métrica aeróbica** | Apagados | Con Flutter preparado (casos A y B) |

## 3. Contratos y textos

| # | Decisión | Estado |
|---|---|---|
| C1 | Renombrar `ESTRUCTURA_COMPLETA` → `DURACION_POR_CONTENIDO` | **Hecho en el PR #17**, sin consumidores. Se aprueba al aprobar el PR |
| C2 | Mensajes de duración con tildes y la redacción de la orden | **Hecho en el PR #17** |
| C3 | Lista única de equipamiento por niveles (`ESPECIFICACION_FLUTTER.md` §5) en `/limitations` y NOFACT-0.1 | **Propuesta**: cambia `/limitations`, que ya está en `generation` |
| C4 | Calcular las alternativas de equipamiento con el filtro de seguridad de cada perfil (sin suponer que se pueda colgar) | **Propuesta**; requiere implementación y pruebas con perfiles restringidos |
| C5 | Traza: añadir el límite **aplicado** por V4-C (minutos del perfil) junto al mostrado (minutos efectivos) | **Propuesta**, menor. Hoy la traza muestra el límite con los minutos efectivos (`CONTRATOS_API_REVISADOS.md` §2) |

## 4. Revisión profesional

| # | Decisión |
|---|---|
| P1 | Designar al evaluador o evaluadores y entregarles **solo** `revision_profesional/paquete_evaluador/` |
| P2 | Cuántos evaluadores y qué umbral de consenso, definidos **antes** de abrir la clave (el instrumento interno los propone; no se han aplicado) |
| P3 | Qué cambios dependen del resultado: ver `CIERRE_PREPARACION_INTEGRACION.md`, pregunta 19 |

## 5. Lo que no se ha tocado (sigue sin autorización)

- 4A, 4C y las dosis 4D;
- V24 y V8C;
- las reglas de progresión y reincorporación;
- el catálogo (ningún registro modificado ni ejercicio añadido);
- Flutter;
- Neon y Railway (ninguna migración aplicada);
- Replicate (0 llamadas);
- ningún despliegue ni ninguna fusión.
