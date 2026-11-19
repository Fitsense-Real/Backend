# 20 · Necesidad de un mecanismo de apagado de emergencia (P17; solo análisis, nada implementado)

**Origen:** decisión P17 (`19`). `allow-candidates=false` impide asignar REV2 a usuarios nuevos, pero los usuarios REV2 en curso siguen con REV2. Es lo correcto en condiciones normales: evita dejarlos sin plan. Pero significa que **hoy no hay forma de detener REV2 para quien ya la tiene** si apareciera un problema grave.

**Este documento no propone activarlo ni implementarlo.** Recoge qué existe, qué falta y qué requisitos debería cumplir, para que decidas si se diseña.

## 1. Qué existe hoy

| Qué se quiere detener | Mecanismo actual | Efecto | Probado |
|---|---|---|---|
| La IA (cualquier política) | `fitsense.ai.enabled=false` | La adaptación sigue sin IA: reglas deterministas y, si no hay cambio factible, el respaldo (copia validada) | Sí: todas las corridas D/D′ son exactamente ese modo (U, J, K, L y M; `17` §3) |
| La IA, ante fallos del proveedor | Cortacircuitos | Se abre tras 3 fallos; respaldo y continuidad | Sí: `REV2_S_U_FALLO` (5 semanas de respaldo, `17` §4) |
| REV2 para usuarios **nuevos** | `allow-candidates=false` | Los nuevos reciben HIST | Sí: `p17CandidatasDesactivadas` |
| REV2 para usuarios **en curso** | **Ninguno** | — | — |
| Un usuario concreto | **Ninguno** (P16, migración explícita, sin implementar) | — | — |

## 2. Lo que no debe hacerse

- **Cambiar la configuración y esperar que los usuarios pasen a HIST.** No ocurre (linaje). Y antes de `9c956a0`, cuando sí ocurría, dejaba usuarios sin plan (HIST → REV2 por V4; REV2 → HIST por V20).
- **Bloquear la generación de REV2 sin alternativa.** El usuario se quedaría sin semana.
- **Reescribir planes ya publicados.** Va contra `ProteccionPlanesPublicadosIT` y contra la trazabilidad.

## 3. Requisitos de un mecanismo futuro (propuesta para tu decisión)

1. **Explícito y auditado.** Lo activa una persona y queda registrado: quién, cuándo, por qué y a qué usuarios afecta. Nunca se activa solo (ni por un error ni por un umbral).
2. **Sin dejar a nadie sin plan.** Para un usuario REV2 afectado, la semana siguiente debe publicarse por una vía segura que ya existe. Hay dos candidatas:
   - **(a) Congelar:** copia validada del último plan REV2 (el respaldo actual), sin progresión. Mantiene la política del linaje.
   - **(b) Migrar a HIST:** migración explícita (P16), que **solo** es segura si se resuelve antes lo que hacía fallar el paso REV2 → HIST (el piso del 70 % de V20 con sesiones cortas, `TransicionesPoliticaIT.rev2AHistorica`).
3. **Sin reescribir el pasado.** Los planes publicados conservan su política y su traza.
4. **Reversible** y con el mismo control de linaje al volver.
5. **Probado antes de existir en producción:** IT del ciclo real con usuarios REV2 en progresión, mantenimiento, respaldo y retirada por dolor; y comprobación de que HIST no cambia (barrido byte a byte).

**Recomendación:** si se diseña, empezar por (a), porque reutiliza el respaldo ya validado y no cambia de política. (b) depende de P16 y de una decisión deportiva.

## 4. Relación con la evaluación con IA real

No bloquea la evaluación propuesta: se ejecuta en una base efímera (Testcontainers) y sin usuarios reales. Sí es un **requisito previo a cualquier despliegue** de REV2 con usuarios reales, junto con P19 (app móvil).
