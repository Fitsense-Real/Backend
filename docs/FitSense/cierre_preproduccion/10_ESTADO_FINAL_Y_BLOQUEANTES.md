# 10 · Estado final y bloqueantes

**El backend NO está listo para producción.** Abrir PR y pasar las pruebas no basta:
- nada se ha fusionado ni desplegado;
- la activación depende de decisiones del estudio y de servicios externos que no se han verificado.

Lo que sí hay: **las correcciones críticas implementadas en PR revisables**, los contratos preparados, el cierre semanal recuperable y una simulación de 12 semanas en verde.

## 1. PR, ramas y SHA (27/11/2026)

| PR | Rama | SHA de la cabeza | Base |
|---|---|---|---|
| [#90](https://github.com/Fitsense-Real/Backend/pull/90) B1 | `preproduccion/b1-seguridad-logs-limites` | `afe2282` | `generation` |
| [#91](https://github.com/Fitsense-Real/Backend/pull/91) B2 | `preproduccion/b2-consentimientos-participacion` | `13b17cd` | `generation` |
| [#92](https://github.com/Fitsense-Real/Backend/pull/92) B3 | `preproduccion/b3-cierre-semanal-recuperable` | `240ccca` | `generation` |
| B4 (este documento) | `preproduccion/b4-operacion-correo-respaldos` | ver la PR | `generation` |
| [#93](https://github.com/Fitsense-Real/Backend/pull/93) B5 | `preproduccion/b5-operacion-trazabilidad` | `cb3a51b` | `generation` |
| [#94](https://github.com/Fitsense-Real/Backend/pull/94) B6 | `preproduccion/b6-simulacion-12-semanas` | `aff80cf` | `preproduccion/b3-cierre-semanal-recuperable` |
| #89 (otra orden) | `feature/v59-recovery-all-ai-flows` | `b490a4c` | `generation` |

## 2. Matriz por ID de la auditoría

Estados: **RESUELTO** · **PREPARADO SIN ACTIVAR** · **BLOQUEADO POR DECISIÓN** · **PENDIENTE** · **NO APLICA**.
"RESUELTO" quiere decir resuelto **en la PR, sin fusionar**.

| ID | Tema | Estado | Dónde | Qué falta |
|---|---|---|---|---|
| P0-01 | Logs sin datos sensibles (S-1) | **RESUELTO** | B1 #90 | Fusionar |
| P0-02 | Límites de login y registro | **RESUELTO** | B1 #90 | Fusionar; verificar el proxy de Railway |
| P0-03 | Términos, privacidad y consentimiento informado | **PREPARADO SIN ACTIVAR** | B2 #91 | Documentos aprobados (P-6), Mobile y activar la exigencia |
| P0-04 | Participación y servicio | **BLOQUEADO POR DECISIÓN** | B2 #91 (A por omisión, B preparada) | Decisión formal del equipo y del comité |
| P0-05 | Cierre semanal recuperable | **RESUELTO** | B3 #92, complementa #89 | Fusionar #89 y después B3 |
| P0-06 | Copias y restauración | **PREPARADO SIN ACTIVAR** | B4 (script ensayado con datos sintéticos) | Base y retención (P-14); ensayo contra la base de preproducción |
| P0-07 | Protocolo de datos del estudio | **BLOQUEADO POR DECISIÓN** | B2 (especificación D-1..D-7) | Decisiones del estudio y del comité |
| P0-08 | *Staging* y Railway | **PREPARADO SIN ACTIVAR** | B4 (lista y plantilla) | Autorización de despliegue y verificación en el panel |
| P0-09 | Recuperación por correo operativa | **BLOQUEADO POR DECISIÓN** | B4 (configuración y procedimiento) | Proveedor, titular y dominio (P-10); prueba con buzón sintético |
| P1-01 | Monitorización y alertas | **RESUELTO** | B5 #93 | Configurar la regla de alertas y el *uptime* (operación) |
| P1-02 | Control optimista al publicar | **RESUELTO** | B3 #92 | Fusionar |
| P1-03 | *Logout*, renovación y claim `email` en el JWT | **PENDIENTE** | — | No estaba en esta orden; propuesta en la auditoría (`06` P1-03) |
| P1-04 | Sin Neon por omisión | **RESUELTO** | B1 #90 | Rotar el usuario de Neon (operación) |
| P1-05 | Panel: acceso mínimo y exportación | **RESUELTO** (parcial) | B5 #93 | Auditoría persistente y rol de solo lectura: propuestos, con migración |
| P1-06 | Guardas de seguridad fijas | **RESUELTO** | B1 #90 | Fusionar |
| P1-07 | Gasto de IA | **RESUELTO** (por usuario) / **PENDIENTE** (tope global) | B5 #93 | Decisión de presupuesto y tope global |
| P1-08 | Ampliación V59 | **PENDIENTE** (otra orden) | #89 | Revisión y fusión de #89 |
| P1-09 | Trazabilidad temporal | **RESUELTO** (parcial: `calculated_at`) | B5 #93 | Historial de métricas y sesiones: propuesto, con migración tras V66 |
| P1-10 | Verificación de correo | **PENDIENTE** | — | Necesita migración; fuera de esta orden |
| P1-11 | Arnés de 12 semanas | **RESUELTO** | B6 #94 | Fusionar tras B3 |
| P1-12 | Cambio de contraseña autenticado | **PENDIENTE** | — | No estaba en esta orden |
| P1-13 | Push en un dispositivo real | **BLOQUEADO POR DECISIÓN** | — | Proyecto de Firebase de pruebas y credenciales |
| P2-01 | `run-for-me` con la zona del servidor | **PENDIENTE** | — | Apagado en `prod`; bajo impacto |
| P2-02 | Motivo del respaldo en la base | **PENDIENTE** (propuesto) | B5 `07` §4 | Migración tras V66 |
| P2-03 | Límites en memoria | **NO APLICA** (aceptado) | — | Una réplica a propósito |
| P2-04 | V45 reservada y `ck_pgo_lease NOT VALID` | **PENDIENTE** | — | `VALIDATE CONSTRAINT` en preproducción; renumerar cualquier V45 |
| P2-05 | Igualar tiempos en la recuperación | **PENDIENTE** | — | Bajo impacto |
| P2-06 | IDOR en `skip`, `report` y `finish` | **NO APLICA** | — | Ya cubierto por `ContratoErroresApiIT.recursosAjenos` (error de la auditoría) |
| P2-07 | Cambio de zona horaria | **PENDIENTE** | — | Posterior al estudio |

## 3. Orden recomendado de fusión (cuando se autorice)

1. **B1 (#90)**: seguridad. Sin dependencias.
2. **#89**: ampliación V59 y V66. **Copia de la base antes de aplicar V66.**
3. **B3 (#92)**, después **B6 (#94)**. B6 se rebasa sobre `generation` una vez que B3 esté dentro.
4. **B5 (#93)**.
5. **B2 (#91)**. Se puede fusionar con la exigencia apagada y sin cambiar nada para la app actual. La activación es aparte.
6. **B4**: documentación y script.

La combinación B1+B2+B3+B5+B6 se fusionó y probó en local (`09`).

## 4. Respuestas de la entrega final (§13 de la orden)

| # | Pregunta | Respuesta |
|---|---|---|
| 1–2 | PR, ramas y SHA | §1 |
| 3 | P0 resueltos | **P0-01, P0-02 y P0-05**, en PR sin fusionar |
| 4 | P0 bloqueados y motivo | **P0-03** (no hay documentos aprobados), **P0-04** (decisión A/B), **P0-07** (protocolo de datos), **P0-09** (proveedor de correo), **P0-06** y **P0-08** (preparados; falta la base y la autorización de despliegue) |
| 5 | P1 implementados | P1-01, P1-02, P1-04, P1-06, P1-11; P1-05 y P1-09 en parte; P1-07 el límite por usuario |
| 6 | Cambios de contrato para Mobile | 429 en login, registro, consentimientos y generación; `GET /api/v1/legal-documents`; `acceptedStudyConsentVersion`; `approvedDocuments`; 409 `PLAN_CAMBIADO_DURANTE_LA_GENERACION`; recuperación con código (`05` §4). Con la configuración por omisión, **nada se rompe** |
| 7 | Decisiones pendientes del estudio | P-6 documentos, P0-04 A/B, D-1..D-7 (retirada, eliminación y evidencias, datos a Replicate, `health_notes`, cribado de aptitud, criterios de exclusión, acceso seudonimizado), P-10 correo, P-14 retención, tope de gasto de IA |
| 8 | Resultado de las pruebas | `09` |
| 9 | Estado del CI | `09` §3 |
| 10 | V59 y V66 | #89 abierta con CI en verde, sin fusionar. V66 es suya. B3 la complementa y es compatible (37/37) |
| 11 | Cierre semanal | Recuperable si se pierde el tick (B3); los ciclos interrumpidos se relanzan (#89); probado durante 12 semanas (B6) |
| 12 | Recuperación por correo | El código existe y está probado con SMTP simulado; **no operativa** hasta tener proveedor (P-10) |
| 13 | Seguridad y logs | S-1 corregido; límites de login y registro; guardas fijas en `prod`; sin Neon por omisión (B1) |
| 14 | Simulación de 12 semanas | En verde (B6), con prueba inversa. **Sin IA real** |
| 15 | Riesgos que quedan para desplegar | §5 |
| 16 | Llamadas pagadas | **Cero.** Todas las pruebas con IA usan un Replicate **local**. El *token* de las pruebas es ficticio (`token-ficticio-de-prueba`) y en este entorno no hay `REPLICATE_API_TOKEN` |

## 5. Riesgos que quedan para desplegar

1. **Nada fusionado.** Cada PR necesita revisión humana. La combinación probada en local no sustituye al CI de la combinación real.
2. **Captación de participantes bloqueada** hasta tener los documentos aprobados y la decisión A/B. Activar la exigencia sin Mobile coordinado impide inscribir a nadie.
3. **Sin correo**, quien olvide la contraseña no puede entrar y no hay cambio de contraseña autenticado (P1-12).
4. **Copias:** hasta que no se ensaye una restauración real, no hay garantía de recuperación.
5. **Proxy de Railway sin verificar.** Si Tomcat no confía en él, el límite por IP afectaría a todos a la vez.
6. **IA real no reevaluada.** En la simulación todos los planes son del motor de reglas. Activar la IA en el estudio requiere autorización de gasto, el tope global y vigilar las alertas.
7. **Reinicio real a mitad de un ciclo:** lo cubre #89. Sin #89, B3 recupera la semana pero puede pagar otra predicción.
8. **Tokens de 7 días sin *logout*** (P1-03) y **sin verificación de correo** (P1-10).
9. **Límites en memoria y una sola réplica.** Cada despliegue los reinicia.
