# 24 · Estado consolidado y riesgos pendientes antes de Replicate

**Fecha:** 05/11/2026.
**Rama:** `investigacion/rediseno-politicas`.
**Código (`src/main`):** `cc76bd1`, sin cambios.

**No hecho, por tus restricciones:**
- ningún merge, despliegue ni Replicate (0 predicciones);
- Neon no se ha tocado;
- el short paper y el PR #41 no se han tocado;
- la evidencia R/R′ y la de U/J/K/L/M no se han modificado.

## 1. Estado consolidado

| Área | Estado | Evidencia |
|---|---|---|
| Política por defecto | `PLAN-HIST-1`, idéntica: barrido byte a byte y huellas K20 congeladas, también en el código final | `22` §3 |
| V24 de generación inicial | Intacta: `StrictValidationAdapter` sin cambios desde `d4556b1`; pruebas de V24 en la regresión; HIST idéntica | `22` §3 |
| REV2 (candidata) | Puerta técnica satisfecha (`18` §3). Solo se activa con `allow-candidates=true` | `17`, `18` |
| P17 | **Decidido:** linaje para los usuarios en curso; `allow-candidates=false` solo bloquea las asignaciones nuevas. Prueba nueva, 7/7 | `19` |
| Apagado de emergencia | **No existe** para usuarios REV2 en curso. Necesidad y requisitos documentados, nada implementado | `20` |
| P18 | **Aprobado:** HIST, incluso antes de V44, hasta una migración explícita. Pruebas conservadas | `19` |
| P12 | **Decidido:** alternativa C. V25, V24 y la prescripción de fuerza sin cambios; `FUERZA_SIN_TIEMPO` en la API | `19` |
| Catálogo | Clasificado (comprobado / visual / profesional) y medido en la selección real. **Nada modificado** | `21` |
| Coherencia entre informe, F8, oráculos y evidencia | Recalculada desde los `.tar.gz`: 26 corridas, **0 discrepancias**. Una precisión de redacción (`22` §2) | `22` |
| M29 | Clasificado aparte con un script nuevo, sin tocar el oráculo ni los resultados. **1.ª ejecución: DEFECTO** (llegaba el fallo técnico de V25); **tras `cc76bd1`: ESPERADO_SIN_PLAN** | `22` §4 |
| Proveedor | Versión `9f4cd9ec…` vigente (= R/R′), `gpt-5-mini` y `medium` válidos, cuenta accesible, coste real medido 0,014–0,017 USD por predicción. Tarifa sin verificar en la web oficial | `23` §2 |
| Cohorte N | Generada **después** del último cambio de código y publicada con manifiesto y SHA-256 **antes** de cualquier ejecución. Nunca ejecutada | `cohorte_N/N_MANIFIESTO.md` |
| Criterios | Fijados de antemano en cuatro dimensiones (factibilidad, aceptación, pertinencia, sin plan esperado) e implementados en un script con pruebas | `23` §5 |

## 2. Riesgos pendientes

| # | Riesgo | Gravedad para la evaluación | Gravedad para usuarios reales | Mitigación actual |
|---|---|---|---|---|
| R1 | **775 y 973**: dosis de espalda y de hombro de cientos de planes REV2 salen de ejercicios mal clasificados (615 y 917 planes) | Media: el oráculo no lo detecta y una sustitución entre ejercicios mal clasificados podría parecer pertinente | **Alta** | Los cambios que los tocan van a REQUIERE_REVISION (`23` C). Corregirlos requiere revisión visual y profesional y tu aprobación |
| R2 | **Aislados de rodilla** cubren el patrón principal (324 planes REV2; 194 de IS/GF con fuerza) | Media: V24/V25 y el oráculo los aceptan | Media | REQUIERE_REVISION. Decisión P15 pendiente |
| R3 | **Sin apagado de emergencia** para usuarios REV2 en curso | Ninguna: base efímera | **Alta** antes de desplegar | `20`: requisitos. `fitsense.ai.enabled=false` sí apaga la IA |
| R4 | **App móvil (P19)**: no muestra FUERZA_SIN_TIEMPO, FREQUENCY ni DOSE, y envía `daysPerWeek` = días marcados | Ninguna (la evaluación es de backend) | **Alta**: la inviabilidad no se «informa claramente» en pantalla | Contrato en `10` §4. El repositorio no está en la sesión |
| R5 | **Versión del modelo no fijada** en la llamada (endpoint del modelo; versión oculta en las predicciones) | Media: un cambio de versión rompería la comparación con R/R′ | Media | `latest_version` antes y después (`23` §2 y §8) |
| R6 | **Tarifa** sin verificar en la web oficial | Baja: el gasto se mide por tokens y el tope usa una reserva de 0,05 USD | — | Tope de 8,40 USD; la cifra oficial es la del panel |
| R7 | **Variabilidad de la IA real**: R y R′ pueden diferir | Media | Media | Dos corridas independientes, sin repeticiones selectivas |
| R8 | **N limitada a las causas ya evaluadas**: dolor, falta de tiempo y cambio de horario no se miden | Media (alcance) | Media | Declarado como limitación (`23` §9) |
| R9 | **U no es no vista** (y J, K, L y M tampoco) | Media (generalización) | — | Solo N cuenta como no vista |
| R10 | **Cifras de dosis [PRO]** sin validación profesional (P2, P3, P10, P13) | Media | Media | Declarado |
| R11 | **P-H1** (predicción sin métricas): con `BLOQUEAR`, un fallo del proveedor deja la corrida incompleta, como en R/R′ | Media: puede obligar a repetir | — | Te pido decidir `RESERVA_SIN_BLOQUEO` (recomendado) o `BLOQUEAR` |
| R12 | **chooseDates (P11)** no separa al máximo los días | Baja | Baja | Alternancia de enfoques garantizada (216/216) |

## 3. Qué necesito de ti para continuar

1. **Autorización explícita** de la evaluación con IA real según `23`:
   - opción A: U + N;
   - R y R′;
   - como máximo **168 predicciones y USD 8,40**;
   - gasto esperado de unos USD 0,7–1,9.
2. **P-H1:** `RESERVA_SIN_BLOQUEO` (recomendado) o `BLOQUEAR`.
3. Si quieres medir con el catálogo corregido (775, 973, aislados), hay que decidirlo **antes**:
   - corregir el catálogo cambia la selección;
   - y eso invalidaría N, que habría que regenerar.

   **Recomendación:** evaluar ahora con el catálogo actual, marcando REQUIERE_REVISION, y corregir el catálogo como trabajo posterior.

**Me detengo aquí.** Ninguna llamada pagada hasta tu autorización.
