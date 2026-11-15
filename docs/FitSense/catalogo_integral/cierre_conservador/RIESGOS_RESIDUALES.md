# Riesgos residuales de la versión técnica conservadora

Son riesgos que quedan **conocidos, acotados y documentados**. Ninguno se ocultó cambiando una prueba ni relajando una restricción.

| # | Riesgo | Probabilidad / evidencia | Mitigación vigente | Qué lo cerraría |
|---|---|---|---|---|
| R1 | **Catálogo sin validación profesional** (312 decisiones abiertas) | Cierta: no hay ninguna validación | Contención de las incertidumbres materiales (27 excluidos y 21 condicionados) y valor más restrictivo en 252 | Revisión profesional con las fichas preparadas (`../cierre/pendientes/FICHAS_PROFESIONALES.xlsx`) |
| R2 | **Error de interpretación de la IA** en un ejercicio clasificado A | Posible: GIF cortos, revisión de IA con confianza MEDIA en parte del catálogo | Dos lecturas independientes (R2 y V3), coordinador, controles permanentes y huella del catálogo | La misma revisión profesional |
| R3 | **P-03 nivel 2 compara solo el total**: una regeneración podría devolver a la base una colocación reducida | Demostrado como posible (`P03ComparacionPorColocacionIT`), **no observado** en los 40 escenarios ni en `HerenciaD8IT` | El contraejemplo queda fijado por la prueba; la regla por colocación está preparada y apagada | Aprobar `fitsense.regeneracion.p03-por-colocacion=true` (`VALIDACION_P03.md`) |
| R4 | **853 ejecutado con rebote o salto** | Baja: el GIF no muestra vuelo y el texto lo prohíbe expresamente | Impacto BAJO solo para el movimiento que se indica (siempre un pie en el suelo); V56 falla si cambian los atributos | Decisión profesional P286 |
| R5 | **Omisiones impuestas por el sistema cuentan en la adherencia** (sesión bloqueada que vence, ejercicio con dolor) | Cierta cuando ocurre | V54 deja constancia en las sesiones bloqueadas; las omisiones por dolor se reconstruyen sin ambigüedad (`VALIDACION_DOLOR_PUBLICADO.md` §5) | Decisión metodológica: excluirlas en el análisis o una causa específica (`SkipReason`), que exigiría aprobación |
| R6 | **Planes ya publicados antes de la contención** siguen mostrando ejercicios ahora contenidos | Por diseño: la contención solo afecta a las planificaciones nuevas | Esos ejercicios no tienen una incompatibilidad de seguridad del perfil; si la tuvieran, `PublishedPlanSafetyService` los marcaría. Si el plan se **repara** (versión nueva), la reparación sustituye también los contenidos (plan 78: 1303 inseguro y 494 contenido), sin más dosis | Regenerar los planes vigentes al desplegar, si se decide |
| R12 | **La contención reduce las alternativas al sustituir o reparar.** Un sustituto con dosis de capacidad mayor que la del ejercicio quitado no puede subirla (V8C), así que la semana no se publica | Observado en el escenario de `ReparacionLinajeIT` con la contención: reparación NO_VALIDA (sustituto 104, 2x13 frente a 2x10). En los 40 escenarios HALL-GM-01: 5 NO_FACTIBLE (antes 4) | Desenlace conservador: no se publica nada y la semana vigente queda intacta | Ampliar las alternativas seguras del catálogo, o una decisión profesional sobre los contenidos |
| R7 | **Nadie recibe los 15 ejercicios de apoyo suspendido** | Cierta: Flutter no pregunta el permiso (V53-B apagado) | Menos variedad para quien sí podría hacerlos; no es un riesgo de seguridad | Aprobar #66 y la pregunta en la app |
| R8 | **14 perfiles de casa con solo peso corporal sin plan** en la rejilla de hipertrofia (775 exige banco) | Cierta y anterior a esta versión | NO_FACTIBLE legítimo, con su motivo (V20) | Ampliar el catálogo de peso corporal o una decisión sobre el 775 |
| R9 | **Cambio de los 72 planes de referencia** respecto a la referencia histórica | Cierta y explicada (22 por contención, 50 por selección determinista) | Referencia nueva aparte, con su prueba; la histórica, intacta y reproducible con la contención apagada | — (es el comportamiento esperado) |
| R10 | **Interruptores mal configurados al desplegar** (contención o dolor apagados por error) | Baja | Los dos están encendidos por defecto; apagarlos exige una propiedad explícita | Revisar la configuración del entorno en el despliegue (fuera del alcance: no se toca Railway ni Neon) |
| R11 | **Sin datos reales**: todo medido con perfiles sintéticos | Cierta | Escenarios deterministas, oráculos independientes y validador real | Evaluación con participantes (fuera de esta orden) |

**Fuera de esta lista no queda ningún defecto técnico conocido.** Los defectos encontrados durante esta orden se corrigieron:

- P-DOLOR-PUB, en sesiones publicadas;
- V56, para el 853;
- las 14 pruebas que no contemplaban la contención ni la protección por dolor, que salieron en la regresión de `eccff00` (ver `REGRESION_FINAL.md`).
