# Cierre final del PR #17 (informe ejecutivo)

**Rama:** `feature/c2d-v4c-versionado`. **PR `Fitsense-Real/Backend#17`: borrador, sin fusionar.**

**Nada activado:**
- `PLAN-HIST-1` y `allow-candidates=false` por defecto.
- Los indicadores (`duracion-estimada`, `metricas.aerobica`, `no-factible-estructurado`, `lose-weight.preferir-grupo-no-cubierto`, `reparacion-sustitucion-v20`) siguen en `false`.
- Sin conexión a Neon ni a Railway, 0 llamadas a Replicate y Flutter sin tocar.

## 1. Fase 0 (estado inicial verificado)

| Elemento | Estado |
|---|---|
| `generation` | `afef98a` |
| Rama (al inicio) | `661c4aa`, sincronizada con origin |
| Árbol de trabajo | Limpio |
| PR #17 | Borrador |
| Migraciones | V44 y V45 solo en la rama, sin aplicar en remoto |
| Modelo E | No portado (solo se menciona en `PlanningPolicy`: «no tiene versión») |
| Evidencias anteriores | Conservadas (`implementacion_controlada_c2d_v4c/` y `correccion_elegibilidad_funcional/`) |

## 2. Resultados clave

| Tema | Resultado |
|---|---|
| **48 perfiles sin tracción** | Con **cualquiera** de 6 materiales declarados (banda, mancuernas, barra de dominadas, correas de suspensión, espaldera, punto de anclaje), **48 de 48** tienen un plan válido con tracción real. Con otra división (4 días UPPER/LOWER, 3 FULL_BODY) son «válidos» pero **sin ninguna tracción**: se descartan porque ocultan la carencia. **Sin material, los 48 siguen NO_FACTIBLE (falta de equipamiento)** |
| **Respuesta del backend** | NOFACT-0.1: un 422 con `diagnosis` estructurado y un mensaje sin términos técnicos, detrás de un indicador apagado. Probado por HTTP encendido y apagado |
| **R0040, R0328, R0616** | Las sustituciones que encuentra el verificador **cambian fuerza por cardio** para llenar minutos: **no son válidas funcionalmente**. REP-SUST-0.1 (solo de la misma clase, validador completo) **no los recupera**; solo recupera R0808 (+15 s). Se recomienda no activarla. C2-D sin cambios |
| **Pruebas con políticas candidatas** | La aserción del piso, parametrizada por política. **3 × 225: 223 aprobadas, 0 fallos y 2 omitidas** con HIST, C2-D y C2-D-EF |
| **EF × LW-PREF-0.2** | **0 regresiones** en 4 combinaciones. 0 cambios sin brecha, 0 V15 nuevas y 0 incompatibilidades. El cardio y la dosis no cambian. 42 corregidos con EF (34 sin EF). La traza identifica la preferencia |
| **Familias de EF** | Lógica revisada. Los patrones nulos u `OTHER` nunca acreditan capacidad ni ancla, y no aparecen en ningún plan. Posibles exclusiones excesivas (65, 873, 808) **sin efecto hoy**. Preguntas para el profesional |
| **Catálogo** | 52 marcados, priorizados por uso real. 5 P1 (775, 318, 1152, 1196, 1077). Nada modificado |
| **Muestra profesional** | **Realmente ciega:** 34 planes completos (descripción, patrón, músculos, series, repeticiones, descanso y duración) y 2 fichas NO_FACTIBLE. Códigos aleatorios. 0 menciones de política o estado. Clave separada. Instrumento de 12 criterios más el bloque E. **Ninguna evaluación registrada** |
| **Ciclos L1–L8** | HIST, C2-D, C2-D-EF y C2-D-V4-EF **idénticos** a la orden anterior, fila a fila. L6 continúa (P-03, 40,1 %). L4 y L8 NO_FACTIBLE con EF. L7 S4 NO_FACTIBLE (no sube el trabajo tras el dolor). Volumen: paso máximo +0,5 % y acumulado +1,0 %. 0 excluidos en planes. Idempotente, ≤ 1 activo por semana y versión coherente |
| **Migraciones** | Plan de staging con comprobaciones previas y posteriores, recuperación y riesgos de Flyway automático. **Sin ejecución remota** |
| **Regresión completa y campaña** | **995 unitarias y 483 de integración (430 ejecutadas, 53 omitidas), 0 fallos.** Campaña REPLAY idéntica (`REGRESION_CIERRE_PR17.md`) |

## 3. Respuestas (25)

1. **¿Cuántos de los 48 tienen una alternativa real con material adicional?**
   **48 de 48** con cualquiera de los 6 materiales probados.
   - La alternativa con anclaje es la más débil: un solo ejercicio y sesiones del 55 % de M de mediana.
   - Solo vale si la persona **ya** tiene el material.
2. **¿Cuántos podrían usar otra división sin perder cobertura funcional?**
   **0.** Con UPPER/LOWER o FULL_BODY pasan el validador, pero sin ninguna tracción.
3. **¿Cuántos deben seguir NO_FACTIBLE?**
   **Los 48 con el material declarado** (solo peso corporal). Más L4 y L8, que son dos de ellos en el ciclo real.
4. **¿Qué requisitos de equipamiento faltan?**
   Uno de estos (basta uno): `band`, `dumbbell`, `pull-up bar`, `suspension straps`, `wall bars` o `anchor point`.
   - Barra y anclaje tienen además requisitos de seguridad: colgarse permitido, y un anclaje firme declarado.
5. **¿Los tres casos de GENERAL_FITNESS se corrigen con sustituciones válidas?**
   **No.** La única sustitución que alarga la sesión es cambiar fuerza por cardio. Con el validador completo, R0040 y R0328 acaban rechazados por V25.
6. **¿Las pruebas con C2-D activo quedaron completamente verdes?**
   **Sí:** 0 fallos en las 3 políticas. Las 2 omitidas son las mismas de siempre.
   - Hubo un intento con 12 errores: era un fallo de la prueba nueva, corregido.
7. **¿EF sigue bloqueando la falsa cobertura de tracción?**
   **Sí:** 0 PULL sin tracción con trabajo resistido y 0 empujes en PULL, en la matriz, en los ciclos y en `PoliticasP123IT`.
8. **¿Hay exclusiones excesivas en EF?**
   **Hay tres candidatas** (65, 873 y 808), pero **no son elegibles en ningún perfil** de la matriz: sin efecto hoy. Van al profesional.
9. **¿LW-PREF-0.2 mantiene sus mejoras combinada con EF?**
   **Sí, y las amplía:** 42 corregidos y 31 mejorados parcialmente, con 0 regresiones.
10. **¿Cambia algún plan que ya estaba bien cubierto?**
    - **No con LW:** 0 cambios en planes sin brecha.
    - **No con REP-SUST:** 0 planes válidos alterados.
    - **No con EF:** solo cambian los planes con colocaciones no computables.
11. **¿Aparecen nuevas infracciones de V15?**
    **0**, en todas las combinaciones de EF × LW.
12. **¿La muestra profesional es ya realmente ciega y con ejercicios completos?**
    **Sí:**
    - 681 filas de ejercicio con descripción completa;
    - códigos aleatorios y antes/después no correlativos;
    - 0 menciones de política, estado o motivo;
    - la clave va en un archivo aparte.
13. **¿Qué registros del catálogo necesitan decisiones?**
    - **P1:** 775, 318, 1152, 1196 y 1077.
    - **P2:** la silla doméstica (1114, 1320) y las reclasificaciones que EF ya neutraliza (973, 315, 1259, 1308, 1261 y otros).
    - **P3:** 320, 939, 1146, 231, 1313 y 1314.
    - **P4:** 303, 305, 1298 y las habilidades.
14. **¿L6 conserva su continuidad sin incumplir restricciones?**
    **Sí:** P-03 al 40,1 % desde S4, 0 excluidos en el plan e igual con LW-PREF-0.2.
15. **¿L4 y L8 siguen rechazados correctamente?**
    **Sí:** NO_FACTIBLE las 10 semanas con EF, por VEF (PULL sin tracción, 0 tracciones elegibles).
16. **¿V4-C sigue validando los segundos estimados?**
    **Sí:** `V4CValidadorRealTest`, y 288 de 288 alternativas dentro del 115 %.
17. **¿Se conservan la progresión y la reincorporación?**
    **Sí:**
    - volumen semanal: paso máximo +0,5 % y acumulado +1,0 %;
    - las subidas por ejercicio están dentro de lo que admite la progresión (+1 repetición);
    - 0 restauraciones automáticas; la reactivación (S7) es explícita.
18. **¿La campaña histórica sigue siendo idéntica?**
    **Sí:** REPLAY de 100 semanas sin fallos (1 ejecutada y 1 omitida), con la evidencia idéntica salvo las marcas de tiempo de `exclusiones_dolor.csv`.
19. **¿Cuántas pruebas se ejecutaron y cuántas se omitieron?**
    - Unitarias: **995 aprobadas, 0 fallos, 0 omitidas.**
    - Integración: **483 contabilizadas, 430 ejecutadas sin fallos, 53 omitidas** (laboratorios opt-in o IA real; ninguna omisión nueva salvo los 8 laboratorios de esta orden).
    - Laboratorios opt-in: 14 sin fallos (9 de `CierrePR17LabIT` en dos ejecuciones, contando dos veces `base`, y 5 ciclos).
    - Baterías candidatas: 3 × 225 (223 aprobadas, 0 fallos y 2 omitidas en cada una).
20. **¿Qué componentes están listos técnicamente?**
    - Versionado y V44/V45.
    - V4-C.
    - C2-D y C2-D + EF.
    - NOFACT-0.1.
    - Contrato de duración.
    - LW-PREF-0.2, como opción.
21. **¿Qué sigue bloqueado por la revisión profesional?**
    - Activar C2-D + EF.
    - Las familias de EF.
    - LW-PREF-0.2.
    - Las correcciones de catálogo P1–P3.
    - Suficiencia de la alternativa con anclaje y de las sesiones cortas por saturación.
22. **¿Qué cambios necesitaría Flutter?**
    - Mostrar `durationInfo.message` y los dos datos de duración.
    - Tratar el 422 con `diagnosis` (mensaje y acción «Revisar mi equipamiento» con los códigos sugeridos).
    - No mostrar planes parciales.
    - No cambiar días ni división sin confirmación (`CONTRATO_NO_FACTIBLE_Y_DURACION.md`).
23. **¿Qué pasaría al aplicar V44 y V45?**
    - Flyway las aplica al arrancar: dos columnas nullable y una lista cerrada de versiones.
    - Los planes antiguos quedan como `PLAN-HIST-1` y no se reescribe nada.
    - La vuelta atrás del código es segura.
    - Bloqueo breve de `weekly_training_plans` al validar la restricción (`PLAN_MIGRACIONES_V44_V45.md`).
24. **¿Qué partes del PR podrían separarse para integrar V4-C antes que C2-D y EF?**
    - **Separable:** versionado (`PlanningPolicy`, selector, contexto y traza) + V44 + V4-C (validador, `excedeTecho` en los generadores y contrato de la IA `max_session_seconds`) + sus pruebas (`PoliticaPlanificacionTest`, `V4CValidadorRealTest`, `ContratoV4CIATest`, `MigracionV44IT`).
    - **Se quedarían fuera:** `SaturacionContenidoV20` y la rama de C2-D del validador, `ElegibilidadFuncional`, VEF, V45 y las versiones EF, LW-PREF, REP-SUST, NOFACT, los contratos de duración y aerobio, y los laboratorios.
    - **Coste:** el registro `PlanningPolicy` tiene los campos `c2d` y `elegibilidadFuncional`. Se conservarían a `false` (o se quitarían) en una rama nueva creada con *cherry-pick* de los commits de versionado y V4-C.
25. **¿Qué aprobaciones debes dar antes de fusionar o desplegar?**
    `DECISIONES_PENDIENTES.md`:
    - A1 (fusionar) y A2 (desplegar V44/V45);
    - B (qué política activar y cuándo, nunca C2-D sin EF);
    - C1 y C2 (qué ofrecer a los 48 de peso corporal y la lista de material);
    - D1–D3 (LW-PREF-0.2, REP-SUST-0.1 y si se toca el verificador de C2-D);
    - E1 (código de la duración);
    - F (catálogo);
    - G (evaluadores y umbrales).

**La validación técnica no equivale a validación profesional.** El PR está **técnicamente preparado para la revisión final**; su activación sigue bloqueada por la revisión profesional, Flutter y las decisiones de producto.
