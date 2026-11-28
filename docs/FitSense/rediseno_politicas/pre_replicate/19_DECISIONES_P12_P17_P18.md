# 19 · Decisiones P12, P17 y P18 (aprobadas el 05/11/2026)

**Estado:** documentadas. `src/main` **no cambia** con estas decisiones: el código que ya estaba en `cc76bd1` cumple lo aprobado. Solo se añade una prueba (`c2fbab3`).

## P17 · `allow-candidates=false` con usuarios REV2 en curso

**Decisión:** se mantiene la política del linaje para los usuarios existentes. `allow-candidates=false` debe impedir **nuevas** asignaciones de REV2, sin migrar ni bloquear automáticamente a los usuarios REV2. El mecanismo de apagado de emergencia se trata aparte (`20_APAGADO_DE_EMERGENCIA.md`).

**Cómo lo cumple el código (sin cambios):**

| Pieza | Comportamiento |
|---|---|
| `PlanningPolicySelector(politica, allowCandidates)` | Rechaza arrancar (`IllegalStateException`) si la política configurada es candidata y `allow-candidates=false`. Con esa configuración no se puede asignar REV2 a nadie nuevo |
| `WeeklyTrainingPlanCommandServiceImpl.prepare` | La semana nueva usa la política del plan anterior del usuario (`politicaDelLinaje`). La configuración solo decide para usuarios **sin** planes |
| Migración automática | No existe. Un usuario no cambia de política sin una migración explícita (P16, sin implementar) |

**Prueba nueva:** `TransicionesPoliticaIT.p17CandidatasDesactivadas` (`c2fbab3`):
- un usuario con REV2 genera su 1.ª semana;
- se reconfigura el selector a `PLAN-HIST-1` con `allow-candidates=false`;
- sus 3 semanas siguientes salen con `PLAN-REV-2.0` (salida `TRANSICION P17 [PLAN-REV-2.0, PLAN-REV-2.0, PLAN-REV-2.0]`), sin quedarse sin plan y sin violaciones;
- un usuario **nuevo** recibe `PLAN-HIST-1`;
- `new PlanningPolicySelector("PLAN-REV-2.0", false)` lanza `IllegalStateException`.

**Resultado:** `TransicionesPoliticaIT` **7/7**, BUILD SUCCESS. El resto de la clase no cambia.

**Consecuencia que debe conocerse:** apagar las candidatas **no** detiene REV2 para quien ya la tiene. Si alguna vez hiciera falta detenerla, hoy no hay un mecanismo propio (ver `20`).

## P18 · Planes anteriores a V44

**Decisión:** aprobado. Los usuarios con planes HIST, incluso anteriores a V44 (sin `planning_policy`), siguen en HIST hasta una migración explícita. Se conservan las pruebas que lo demuestran.

| Prueba | Qué demuestra |
|---|---|
| `PoliticaV4CCicloIT.planesHistoricosIntactos` | Un plan sin política (anterior a V44) no se reescribe (`planning_policy` y su traza siguen nulos). La semana siguiente se registra `PLAN-HIST-1` (`PlanningPolicy.deRegistro(null)` = histórica), aunque la configuración diga otra cosa |
| `TransicionesPoliticaIT.historicaARev2` | Con REV2 configurada, el usuario HIST en curso sigue en HIST y su semana sale; el usuario nuevo recibe REV2 |
| `TransicionesPoliticaIT.mantenimientoACambioHistorica` | REV2 en mantenimiento → cambio de configuración: el usuario sigue su linaje; no hay paso automático |

**Estado de la expectativa cambiada en `9c956a0`:** queda **aprobada** y se mantiene. Ya no está pendiente de confirmación.

## P12 · Fuerza en 1 día de 30 minutos

**Decisión:** se mantiene la alternativa C. No se cambian V25, V24 ni la prescripción de fuerza. La inviabilidad prevista debe informarse claramente.

| Elemento | Estado |
|---|---|
| V25 (incluida su regla `M < 30 ? 3 : 4`) | Sin cambio. Su incoherencia con el V4 de REV2 sigue documentada en `11` y **no** se corrige |
| V24 y la dosis de fuerza | Sin cambio (ver la auditoría `22` §3) |
| Antes de generar | `GET /users/me/plan/limitations` devuelve `TIME / FUERZA_SIN_TIEMPO`, con el mínimo calculado (32 min) y las alternativas (más minutos o más días) |
| Al generar (cualquier vía, también la fachada desde `cc76bd1`) | HTTP 422 con `code=FUERZA_SIN_TIEMPO` y el mensaje con el mínimo y las alternativas. No se publica un plan recortado |
| HIST | No cambia: con su tolerancia del 15 % sí genera |
| Pruebas | `FuerzaSinTiempoIT` 3/3 con HIST y con REV2 |

**Pendiente fuera del backend (P19):** la app móvil aún no muestra el aviso previo ni el 422. Sin ese cambio, «informar claramente» se cumple en la API pero no en la pantalla. El repositorio `mobileapp` no está en esta sesión.

**En la evaluación:** los perfiles con este caso (M29, N19) se clasifican como **ESPERADO_SIN_PLAN**, separados de los PASS y de los defectos (`23` §4).
