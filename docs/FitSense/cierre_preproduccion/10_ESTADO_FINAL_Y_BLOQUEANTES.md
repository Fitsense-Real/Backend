# 10 · Estado final y bloqueantes

**Actualizado el 28/11/2026, tras la revisión final y los merges condicionados (`11`).**

**El backend NO está listo para producción ni para recibir participantes.**
- #89–#95 están fusionadas en `generation`, tras revisarlas y corregir tres defectos (`11` §2).
- `main` no ha cambiado y no se ha desplegado nada en Railway.
- La activación sigue dependiendo de decisiones del estudio y de servicios externos que no se han verificado.

Lo que sí hay en `generation`:
- las correcciones críticas;
- la identidad de predicciones revisada (H-1);
- los contratos preparados para Mobile, sin activar;
- el cierre semanal recuperable;
- una simulación de 12 semanas en verde, con prueba inversa.

## 1. PR y merges (27–28/11/2026)

| Orden | PR | Rama | Cabeza fusionada | Commit de merge |
|---|---|---|---|---|
| 1 | [#90](https://github.com/Fitsense-Real/Backend/pull/90) B1 | `preproduccion/b1-seguridad-logs-limites` | `afe2282` | `25bcf27e` |
| 2 | [#89](https://github.com/Fitsense-Real/Backend/pull/89) V59/V66 | `feature/v59-recovery-all-ai-flows` | `bf49947` (H-1) | `069a6031` |
| 3 | [#92](https://github.com/Fitsense-Real/Backend/pull/92) B3 | `preproduccion/b3-cierre-semanal-recuperable` | `240ccca` | `51e8d7ed` |
| 4 | [#94](https://github.com/Fitsense-Real/Backend/pull/94) B6 | `preproduccion/b6-simulacion-12-semanas` (base cambiada a `generation`) | `c770444` | `57146228` |
| 5 | [#93](https://github.com/Fitsense-Real/Backend/pull/93) B5 | `preproduccion/b5-operacion-trazabilidad` | `92b32bf` (H-2) | `a5a7a0cd` |
| 6 | [#91](https://github.com/Fitsense-Real/Backend/pull/91) B2 | `preproduccion/b2-consentimientos-participacion` | `13b17cd` | `eb13db92` |
| 7 | [#95](https://github.com/Fitsense-Real/Backend/pull/95) B4 | `preproduccion/b4-operacion-correo-respaldos` | la que contiene este documento (H-3) | informe final de la orden |

Los SHA completos, las pruebas tras cada merge y el CI están en `11` §5.

## 2. Matriz por ID de la auditoría

Estados: **RESUELTO** · **PREPARADO SIN ACTIVAR** · **BLOQUEADO POR DECISIÓN** · **PENDIENTE** · **NO APLICA**.
"RESUELTO" quiere decir **fusionado en `generation`**, no desplegado.

| ID | Tema | Estado | Dónde | Qué falta |
|---|---|---|---|---|
| P0-01 | Logs sin datos sensibles (S-1) | **RESUELTO** | B1 #90 | — |
| P0-02 | Límites de login y registro | **RESUELTO** | B1 #90 | Verificar el *proxy* de Railway |
| P0-03 | Términos, privacidad y consentimiento informado | **PREPARADO SIN ACTIVAR** | B2 #91 | Documentos aprobados (P-6), Mobile y activar la exigencia |
| P0-04 | Participación y servicio | ~~BLOQUEADO POR DECISIÓN~~ → **RESUELTO: opción A (28/11/2026)** | B2 #91; C1 (exclusividad comprobada en todas las rutas, B bloqueada en `prod`) | Aprobación del protocolo y del consentimiento informado (`integracion_final_estudio/08`) |
| P0-05 | Cierre semanal recuperable | **RESUELTO** | B3 #92 y #89 (V66) | — |
| P0-06 | Copias y restauración | **PREPARADO SIN ACTIVAR** | B4 (script ensayado con datos sintéticos) | Base y retención (P-14); ensayo contra la base de preproducción |
| P0-07 | Protocolo de datos del estudio | **BLOQUEADO POR DECISIÓN** | B2 (especificación D-1..D-7) | Decisiones del estudio y del comité |
| P0-08 | *Staging* y Railway | **PREPARADO SIN ACTIVAR** | B4 (lista y plantilla) | Autorización de despliegue y verificación en el panel |
| P0-09 | Recuperación por correo operativa | **BLOQUEADO POR DECISIÓN** | B4 (configuración y procedimiento) | Proveedor, titular y dominio (P-10); prueba con buzón sintético |
| P1-01 | Monitorización y alertas | **RESUELTO** | B5 #93 | Configurar la regla de alertas y el *uptime* (operación) |
| P1-02 | Control optimista al publicar | **RESUELTO** | B3 #92 | — |
| P1-03 | *Logout*, renovación y claim `email` en el JWT | **PENDIENTE** | — | No estaba en esta orden; propuesta en la auditoría (`06` P1-03) |
| P1-04 | Sin Neon por omisión | **RESUELTO** | B1 #90 | Rotar el usuario de Neon (operación) |
| P1-05 | Panel: acceso mínimo y exportación | **RESUELTO** (parcial) | B5 #93 | Auditoría persistente y rol de solo lectura: propuestos, con migración |
| P1-06 | Guardas de seguridad fijas | **RESUELTO** | B1 #90 | — |
| P1-07 | Gasto de IA | **RESUELTO** (por usuario) / **PENDIENTE** (tope global) | B5 #93 | Decisión de presupuesto y tope global |
| P1-08 | Ampliación V59 | **RESUELTO** | #89, con la corrección de identidad H-1 | V66 en preproducción, con copia previa |
| P1-09 | Trazabilidad temporal | **PENDIENTE** (propuesto) | B5 #93 (`07` §4) | `calculated_at` conserva la fecha del primer cálculo (H-2). `recalculated_at` y el historial de métricas y sesiones necesitan una migración tras V66 |
| P1-10 | Verificación de correo | **PENDIENTE** | — | Necesita migración; fuera de esta orden |
| P1-11 | Arnés de 12 semanas | **RESUELTO** | B6 #94 | — |
| P1-12 | Cambio de contraseña autenticado | **PENDIENTE** | — | No estaba en esta orden |
| P1-13 | Push en un dispositivo real | **BLOQUEADO POR DECISIÓN** | — | Proyecto de Firebase de pruebas y credenciales |
| P2-01 | `run-for-me` con la zona del servidor | **PENDIENTE** | — | Apagado en `prod`; bajo impacto |
| P2-02 | Motivo del respaldo en la base | **PENDIENTE** (propuesto) | B5 `07` §4 | Migración tras V66 |
| P2-03 | Límites en memoria | **NO APLICA** (aceptado) | — | Una réplica a propósito |
| P2-04 | V45 reservada y `ck_pgo_lease NOT VALID` | **PENDIENTE** | — | `VALIDATE CONSTRAINT` en preproducción; renumerar cualquier V45 |
| P2-05 | Igualar tiempos en la recuperación | **PENDIENTE** | — | Bajo impacto |
| P2-06 | IDOR en `skip`, `report` y `finish` | **NO APLICA** | — | Ya cubierto por `ContratoErroresApiIT.recursosAjenos` (error de la auditoría) |
| P2-07 | Cambio de zona horaria | **PENDIENTE** | — | Posterior al estudio |

## 3. Orden de fusión aplicado

Es el de la orden y no el de los números de PR: #90 → #89 → #92 → #94 → #93 → #91 → #95. Antes se fusionaron las siete en una rama local temporal y pasó la regresión completa: 1.680 + 943, 0 fallos (`11` §4). Después de cada merge se repitieron sus pruebas y se esperó el CI (`11` §5).

## 4. Respuestas de la entrega final (§13 de la orden)

| # | Pregunta | Respuesta |
|---|---|---|
| 1–2 | PR, ramas y SHA | §1 |
| 3 | P0 resueltos | **P0-01, P0-02 y P0-05**, fusionados en `generation` |
| 4 | P0 bloqueados y motivo | **P0-03** (no hay documentos aprobados), **P0-04** (decisión A/B; **resuelta el 28/11/2026: opción A**), **P0-07** (protocolo de datos), **P0-09** (proveedor de correo), **P0-06** y **P0-08** (preparados; falta la base y la autorización de despliegue) |
| 5 | P1 implementados | P1-01, P1-02, P1-04, P1-06, P1-11; P1-05 y P1-09 en parte; P1-07 el límite por usuario |
| 6 | Cambios de contrato para Mobile | 429 en login, registro, consentimientos y generación; `GET /api/v1/legal-documents`; `acceptedStudyConsentVersion`; `approvedDocuments`; 409 `PLAN_CAMBIADO_DURANTE_LA_GENERACION`; recuperación con código (`05` §4). Con la configuración por omisión, **nada se rompe** |
| 7 | Decisiones pendientes del estudio | P-6 documentos, ~~P0-04 A/B~~ (resuelta: A), D-1..D-7 (retirada, eliminación y evidencias, datos a Replicate, `health_notes`, cribado de aptitud, criterios de exclusión, acceso seudonimizado), P-10 correo, P-14 retención, tope de gasto de IA |
| 8 | Resultado de las pruebas | `09`; con #89 y las siete combinadas, `11` §4 y §5 |
| 9 | Estado del CI | En verde en cada merge (`11` §5) |
| 10 | V59 y V66 | #89 fusionada con la corrección H-1. V66 está en `generation` y **no se ha aplicado a ninguna base real**. B3 la complementa (37/37 tras el merge) |
| 11 | Cierre semanal | Recuperable si se pierde el tick (B3); los ciclos interrumpidos se relanzan (#89); probado durante 12 semanas (B6) |
| 12 | Recuperación por correo | El código existe y está probado con SMTP simulado; **no operativa** hasta tener proveedor (P-10) |
| 13 | Seguridad y logs | S-1 corregido; límites de login y registro; guardas fijas en `prod`; sin Neon por omisión (B1) |
| 14 | Simulación de 12 semanas | En verde (B6), con prueba inversa. **Sin IA real** |
| 15 | Riesgos que quedan para desplegar | §5 |
| 16 | Llamadas pagadas | **Cero**, también en esta orden. Todas las pruebas con IA usan un Replicate **local**. El *token* de las pruebas es ficticio (`token-ficticio-de-prueba`) y en este entorno no hay `REPLICATE_API_TOKEN` |

## 5. Riesgos que quedan para desplegar

1. **Fusionado no es desplegado.** Railway no se ha tocado. V66 se aplicará en el primer despliegue. Antes: copia verificada (`06` §1) y aplicarla primero en preproducción.
2. **Captación de participantes bloqueada** hasta tener los documentos aprobados y la decisión A/B. Activar la exigencia sin Mobile coordinado impide inscribir a nadie.
3. **Sin correo**, quien olvide la contraseña no puede entrar y no hay cambio de contraseña autenticado (P1-12).
4. **Copias:** hasta que no se ensaye una restauración real, no hay garantía de recuperación.
5. **Proxy de Railway sin verificar.** Si Tomcat no confía en él, el límite por IP afectaría a todos a la vez.
6. **IA real no reevaluada.** En la simulación todos los planes son del motor de reglas. Tras H-1, un reintento manual paga su propia predicción. Activar la IA en el estudio requiere autorización de gasto, el tope global y vigilar las alertas.
7. **Tokens de 7 días sin *logout*** (P1-03) y **sin verificación de correo** (P1-10).
8. **Límites en memoria y una sola réplica.** Cada despliegue los reinicia.
9. **El piloto no está listo** por haber terminado la integración. Faltan las decisiones de §2.
