# Estado final del backend

Fecha: 17/11/2026. Integración controlada de #73, #71 y #72 terminada.

- No se ha desplegado nada ni se ha usado Replicate.
- No se han tocado datos reales.
- H2 (#69) y P-03 siguen apagados.

## 1. PR

| PR | Cabeza final | CI | Conflictos | Estado |
|---|---|---|---|---|
| #73: `ReparacionLinajeIT` determinista | `8313919` | verde | No | **Fusionada** (`41934d6`) |
| #71: generación asíncrona (V58) | `3e83b6f` (con `generation` + #73) | verde | No | **Fusionada** (`d2f784a`) |
| #72: generación asíncrona resiliente (V59) | `c608462` (con `generation` + #71) | verde | No | **Fusionada** (`b9c51ef`) |
| #74: PD-16, reparación concurrente entre fases | `b9f8299` | verde | No | **Fusionada** (`f8ffb61`) |
| #75: auditoría integral (`validacion/backend-integral`) | ver la PR | ver la PR | No | Se fusiona al cerrar la etapa |

`generation` = `b9c51ef`. Su CI está en verde tras cada merge: `41934d6`, `d2f784a` y `b9c51ef`, tanto en push como en PR.

## 2. Pruebas ejecutadas sobre `generation` (`b9c51ef`)

| Ejecución | Resultado |
|---|---|
| Regresión completa local (`mvnw verify -Pintegration`) | **1576 unitarias y 809 de integración, 0 fallos.** Omitidas: 1 unitaria y 72 de integración, todas opt-in |
| `GeneracionAsincronaIT` (opt-in, PostgreSQL efímero) | 15/15 |
| `GeneracionAsincronaResilienciaIT` (R01–R20) | 23/23 |
| `ReanudacionConReplicateSimuladoIT` | 11/11 |
| `MigracionV58aV59IT` | 1/1 |
| `GeneracionInicialConcurrenteIT` / `CicloConcurrenteIT` | 3/3 y 9/9 |
| `ProteccionPlanesPublicadosIT` (incluye `reparacionesSimultaneas`) | 12/12 |
| `ReparacionLinajeIT` / Rev2 | 3/3 y 3/3 |
| `ProgresionIT` / `PendientesProgresionIT` / `PoliticasP123IT` | 24/24, 6/6 y 39/39 |
| `ConfiguracionEvaluadaIT` (configuración aprobada) | 3/3 |
| `CicloCompletoHttpIT`, en la rama de auditoría actualizada con `generation` | 9/9 |
| CI `verify` de `generation` | verde |

Durante la integración se ejecutaron además:

- la regresión de #71 actualizada: 1555 unitarias y 774 de integración, 0 fallos;
- la de #72 actualizada: 1576 y 809, con 1 fallo de PD-16, preexistente y ajeno.

## 3. Garantías comprobadas

**Generación asíncrona (V58 + V59):**

- Una generación larga no se declara abandonada: el tiempo máximo es de 1.800 s desde la primera ejecución, con 3 ejecuciones como mucho.
- El latido va en un hilo propio y no lo bloquea el cierre semanal.
- No hay dos publicaciones: lease con propietario y fencing.
- Un worker sin lease no puede publicar.
- Una predicción identificada se recupera tras un reinicio.
- Una `NO_CONFIRMADA` no se reenvía.
- Hay una sola operación activa por usuario.

Todo está cubierto por R01–R20, `ReanudacionConReplicateSimuladoIT` y `GeneracionAsincronaIT`.

**Configuración.** Respecto a la base anterior (`2578719`), `application.properties` solo añade `fitsense.plan-generation.*`. Siguen:

- `fitsense.regeneracion.p03-por-colocacion=false`;
- las banderas experimentales sin cambios;
- H2 sin integrar.

**Código de dominio.** La integración solo añade V58 y V59. No cambian:

- los validadores ni los servicios de dominio de planificación;
- el catálogo ni la adherencia;
- la progresión ni la reincorporación.

## 4. Estado por módulo

| Módulo | Estado |
|---|---|
| A. Usuarios y perfil | Completo. Recuperación de contraseña **parcial**: no hay proveedor de correo (PD-03). |
| B. Generación inicial | Completa: síncrona y **asíncrona resiliente**, para los 4 objetivos |
| C. Ejecución | Completo |
| D. Adherencia | Completo, con la fórmula oficial intacta y verificada al centésimo |
| E. Adaptación | Completo, sin H2 ni P-03 |
| F. Progresión y reincorporación | Completo |
| G. Trazabilidad y analítica | Completo para el estudio. Panel **parcial**: solo lectura agregada (PD-05, PD-06, PD-10). |

## 5. Pendientes para la etapa siguiente

Detalle en `PENDIENTES_PRIORIZADOS.md`.

1. PD-16 está resuelto: #74 está fusionada, con su prueba determinista.
2. La auditoría integral (#75) se fusiona al cerrar la etapa.
3. **Antes de la IA real:**
   - la ronda con Replicate real y presupuesto (PD-11);
   - desplegar con una sola instancia mientras el interruptor de la IA esté en memoria (PD-08).
4. **Antes de participantes reales:**
   - el proveedor de correo (PD-03);
   - la evaluación de la cobertura parcial con OBSERVE (PD-07, que se conserva de forma provisional);
   - lo que se decida sobre el panel por participante y la exportación (PD-05, PD-06).
5. **Despliegue y cliente:**
   - Railway (PD-14);
   - Mobile en un Android físico (PD-15);
   - orden: primero el backend, después Flutter (PD-09).
6. **Calidad de las pruebas.** Pasar a semana fija las 38 IT con fecha relativa (PD-01).
7. **Evaluación de H2 (PD-13).**

PD-04 está decidido: se mantiene la conducta conservadora en la reparación NO_VALIDA.
