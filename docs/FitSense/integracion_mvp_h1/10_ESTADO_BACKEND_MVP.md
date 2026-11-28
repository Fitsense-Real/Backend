# 10 · Estado del backend para el MVP

Fotografía a 03/11/2026, con `generation` en `0974fb3` y los PR A (#36) y B encima, en borrador.

## A. Funcionando y verificado (en `generation`)

| Capacidad | Evidencia |
|---|---|
| Generación de planes para los cuatro objetivos (PLAN-HIST-1, 4D, cobertura 4C, V20 exacto) | 960 perfiles, hashes de referencia; `GeneracionReferencia*IT`; validación multisemana |
| Adherencia (Aw, Aobs) y cobertura de registro | `AdherenciaSinPesosTest`, `AdherenciaRiesgoIT`; REPLAY `metricas.csv` |
| Decisión semanal (NO_CHANGE / ADAPT_LOCALIZED / ADAPT_EXPANDED / INSUFFICIENT_DATA; dolor separado de la adherencia) | `AdaptationOrderDecider`; REPLAY `historial.csv` |
| Validación antes de publicar (V1–V25, V8C, V20, V24) | Suites de validadores; 0 violaciones en las campañas |
| Publicación con linaje V31/V32 e idempotencia | `LinajeIntegridadIT`, reintento del ciclo, `CicloConcurrenteIT` |
| Conservación de sesiones no señaladas | `AdaptacionCompletaIT`, V8C |
| Seguridad: dolor, exclusiones, SUST-SEG-1, sin subir dosis | Pruebas de dolor de V8C y respaldo forzado; REPLAY `exclusiones_dolor.csv` y `PASOS.csv` |
| RESPALDO, SIN_EFECTO, NO_FACTIBLE estructurado | Pruebas de respaldo y de NO_FACTIBLE |
| Progresión y reincorporación | `ProgresionIT`, `Reincorporacion4DTest` |

## B. Listo para integración (PR en borrador, CI)

| PR | Contenido | Estado |
|---|---|---|
| **#36 (A)** | H1: V8C multiconjunto por defecto, reversión a `conjunto` | Regresión local completa sin fallos; CI en `09` |
| **B** | Textos verificables: resultado en `adjustment_reason` e intención en `message_shown` | Regresión local en `06`; CI en `09` |

## C. Experimental (apagado; no forma parte del MVP)

- **Prompt V2** con factibilidad previa (#29): `fitsense.ai.adaptation.prompt-version`. No existe en `generation`.
- **H12-A/B/C/D** (#33): no existe en `generation` ni en A/B.
- **Políticas candidatas del PR #17**: sin activar.
- **Laboratorios** (#28–#35): *opt-in*, solo como evidencia.

## D. Pendiente de revisión profesional

- **Calidad funcional de las sustituciones ante TOO_DIFFICULT:** el instrumento H12-A y su formulario de 27 respuestas (#33). **Sin responder:** no se ha inferido ninguna respuesta.
- **Principiantes en dificultad mínima:** todo el catálogo elegible tiene dificultad 1 (R01, R05, E12).
- **Fichas de 580, 775, 780 y 862** (#32): nombre, patrón, músculo y dificultad.
- **Sustituciones de mayor dificultad registrada:** hoy ningún validador las impide (`05` §2). ¿Deben prohibirse ante TOO_DIFFICULT?

## E. Pendiente de integración móvil (sin tocar Flutter en esta orden)

| Contrato que usa la app | Estado en el backend | Lo que falta |
|---|---|---|
| Generación (`POST` de generación del plan, `GET current`) | Estable | Verificar contra la app con datos sintéticos |
| Registro de ejecución (sesiones, motivos, RPE) | Estable | Ídem |
| Adherencia (métricas semanales) | Estable | Decidir qué cifras se muestran (Aw, Aobs, cobertura) |
| Plan adaptado (`WeeklyTrainingPlanResource.adaptation`: APLICADA, SIN_EFECTO o CONSERVADA por sesión) | Estable | Que la app muestre el código por sesión |
| **Mensajes de resultado** (`adjustmentReason`) | **Cambia con el PR B**: de texto técnico a resultado en lenguaje natural | La app debe mostrarlo como resultado de la semana |
| **Mensaje de decisión** (`GET /api/v1/users/me/interventions` → `messageShown`) | **Cambia con el PR B**: de texto técnico a intención | La app debe mostrarlo como «lo que vamos a revisar», no como resultado |
| Errores (`ApiErrorResource` 401/400/404) | Estable | — |

**Para empezar la integración final con Flutter faltan:**
1. Fusionar A y B (tras tu autorización).
2. Fijar en la app qué campo se muestra en cada pantalla (decisión frente a resultado).
3. Una prueba de extremo a extremo con un backend local, datos sintéticos y la IA desactivada o simulada.
