# Concurrencia e idempotencia con C2-D activo

## 1. Ciclo concurrente (`CicloConcurrenteIT`, 9 casos) con PLAN-C2D-0.1 y con PLAN-C2D-EF-0.1

| Caso | Comprueba | C2-D | C2-D-EF |
|---|---|---|---|
| Misma persona y semana a la vez, IA lenta | La segunda ejecución no hace nada (EN_CURSO): 1 llamada, 1 V32, 1 semana | Pasa | Pasa |
| Cinco carreras simultáneas sin IA | Siempre 1 V32 y 1 semana; ninguna versión reemplazada | Pasa | Pasa |
| Dos instancias de la aplicación sobre la misma base | La segunda ve EN_CURSO y después devuelve el mismo plan | Pasa | Pasa |
| Fallo durante el ciclo | El bloqueo se libera; un reintento procesa la semana una sola vez | Pasa | Pasa |
| Se cae la conexión del bloqueo | El ciclo termina sin excepción y el bloqueo queda libre | Pasa | Pasa |
| Se pierde el bloqueo mientras se espera a la IA, o al empezar | Una decisión, una evidencia, una semana | Pasa | Pasa |
| Fallo tardío de la ejecución que perdió el bloqueo | No deja evidencia si otra ya publicó | Pasa | Pasa |
| Personas distintas a la vez | No se bloquean entre sí | Pasa | Pasa |

## 2. Ciclo real de 10 semanas (`CicloReal*IT`), con un ciclo repetido cada semana

| Política | Planes activos por semana (máximo) | Repetición idéntica | Política del plan = configurada | Excluidos por dolor en plan |
|---|---|---|---|---|
| PLAN-HIST-1 | 1 | Sí (60/60) | 19/19 planes | 0 |
| PLAN-C2D-0.1 | 1 | Sí (60/60) | 59/59 | 0 |
| PLAN-C2D-EF-0.1 | 1 | Sí (60/60) | 39/39 | 0 |
| PLAN-C2D-V4-EF-0.1 | 1 | Sí (60/60) | 39/39 | 0 |

## 3. Requisitos de la orden

| Requisito | Evidencia |
|---|---|
| Un solo plan activo por semana | Máximo 1 en los cuatro ciclos y en las carreras de `CicloConcurrenteIT` |
| Sin duplicados | 1 semana y 1 V32 por persona y semana con 5 carreras |
| Sin pérdida de linaje | `LinajeIntegridadIT` pasa con las dos políticas. `nextVersionOf` copia la política y la traza |
| Sin autorizaciones duplicadas | Una decisión y una evidencia por semana tras perder el bloqueo |
| Sin cambios de versión incoherentes | Cada plan guarda la política configurada; una versión siguiente de la semana hereda la de su linaje. Una desconocida da error (`PoliticaPlanificacionTest`) y la base la rechaza (V45) |
| Sin progresiones extra en los reintentos | La repetición del ciclo devuelve el mismo plan (idempotente). «Ciclo semanal repetido … ya se publicó, no se adapta otra vez» |

**Limitación:**
- La concurrencia se prueba dentro de un proceso, con dos contextos de aplicación sobre la misma base efímera.
- No se ha probado con varios nodos reales ni con la latencia de Neon (prohibido).
