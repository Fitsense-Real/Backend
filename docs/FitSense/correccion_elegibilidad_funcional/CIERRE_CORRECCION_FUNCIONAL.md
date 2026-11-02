# Cierre de la corrección de elegibilidad funcional

**Rama:** `feature/c2d-v4c-versionado` (PR borrador `Fitsense-Real/Backend#17`, sin fusionar).

**No se ha fusionado, desplegado ni activado nada:**
- La política por defecto sigue siendo `PLAN-HIST-1`, con `allow-candidates=false`.
- EF-0.1 solo existe en tres versiones candidatas nuevas.
- `LW-PREF-0.2`, el contrato de duración y la métrica aeróbica siguen apagados.

## Entregables (`docs/FitSense/correccion_elegibilidad_funcional/`)

**Los 17 pedidos:**
1. `DIAGNOSTICO_SELECTOR_EJERCICIOS.md`
2. `AUDITORIA_V23_V25.md`
3. `MATRIZ_CATALOGO_INCONSISTENCIAS.csv`
4. `PROPUESTA_CORRECCION_CATALOGO.md`
5. `ELEGIBILIDAD_FUNCIONAL_EXPERIMENTAL.md`
6. `CASOS_EXTREMOS_46_PORCIENTO.md`
7. `AUDITORIA_R0800.md`
8. `MATRIZ_COMPARACION_FUNCIONAL.csv`
9. `INTERACCION_C2D_CATALOGO.md`
10. `LONGITUDINAL_C2D_CORREGIDO.csv`
11. `PRUEBAS_IA_D8_ACORTAR_C2D.md`
12. `PRUEBAS_CONCURRENCIA_C2D.md`
13. `REVISION_CONTRATO_DURACION.md`
14. `LOSE_WEIGHT_COBERTURA_REVISADA.md`
15. `REGRESION_FINAL.md`
16. `RIESGOS_RESTANTES.md`
17. Este documento.

**Anexos:**
- `ESCANEO_CATALOGO_ACTIVO.csv`
- `CASOS_EXTREMOS_DETALLE.csv`, `CASOS_EXTREMOS_ELEGIBLES.csv` y `CASOS_EXTREMOS_ALTERNATIVA.csv`
- `AUDITORIA_R0800_DETALLE.csv`
- `LOSE_WEIGHT_PREFERENCIA_LW02.csv`
- `LONGITUDINAL_PLAN-*.csv` y `PLANES_CICLO_PLAN-*.csv`
- `MUESTRA_REVISION_PROFESIONAL_EF.csv`

Los documentos y CSV de `implementacion_controlada_c2d_v4c/` se conservan sin cambios.

## Respuestas (27)

1. **¿Por qué se usaban movimientos incorrectos en PULL?**
   - El selector decide la pertenencia a la sesión por la **zona**, no por el **patrón**.
   - Cualquier patrón que no sea cardio, movilidad ni equilibrio cuenta como resistido.
   - El catálogo tiene un empuje con zona espalda (775) y patrones de tronco o movilidad con zona espalda (1259, 1261, 1308).
   - Con peso corporal, el PULL **no tiene ninguna tracción elegible**, así que se llenaba con eso.
2. **¿El defecto está en el catálogo, en el selector, en V23 o en V25?**
   - **Es una combinación.**
   - La causa raíz es el selector y el validador: no hay compatibilidad patrón-enfoque.
   - El catálogo la agrava.
   - V23 y V25 la toleran, porque solo exigen lo factible y por semana; V23 ni siquiera bloquea al motor de reglas en producción.
   - Ver `DIAGNOSTICO_SELECTOR_EJERCICIOS.md` §5.
3. **¿Qué ejercicios son inconsistentes?**
   - De los 7 señalados:
     - **Inconsistentes:** 775, 1259 y 315.
     - **Correcto:** 1066.
     - **Requieren revisión profesional:** 1261, 1308 y 320.
   - El escaneo sistemático de los 1.219 activos marca 52 filas: 18 inconsistentes, 17 correctas, 12 a revisión profesional y 5 con información insuficiente (`MATRIZ_CATALOGO_INCONSISTENCIAS.csv`).
4. **¿Qué modificaciones del catálogo se proponen?**
   - Para 775, revisar el movimiento real: no es una tracción.
   - Reclasificar 1259, 1261, 1308, 68, 254, 882, 883 y 310 como movilidad, tras revisión.
   - Pasar a zona `waist` 875, 901 y 973.
   - Para 315, equipo mancuerna o instrucciones sin material.
   - Añadir requisitos de equipo a 318, 1152, 1196, 1298 y 1077.
   - Corregir las instrucciones de 303 y 305.
   - Decidir si se presupone una silla (465, 1114, 1136, 1188, 1320).
   - **No se ha cambiado nada en la base** (`PROPUESTA_CORRECCION_CATALOGO.md`).
5. **¿Cuáles requieren revisión profesional?**
   - 1261, 1308, 320, 775 (movimiento real), 939, 65, 873, 808, 1294, 1313, 1314, 231 y 1146.
   - Además, las familias de patrones y el ancla de EF-0.1.
6. **¿El backend detecta ahora la falsa cobertura de tracción?**
   - **Sí, con una versión EF:** VEF rechaza cualquier colocación de patrón incompatible y cualquier PULL con trabajo resistido sin tracción.
   - Con la histórica, no (por diseño: no cambia).
7. **¿Qué ocurre cuando no hay ejercicios de tracción elegibles?**
   - El plan es **NO_FACTIBLE** con el motivo «catálogo o equipamiento insuficiente; no se sustituye por otro movimiento».
   - No se rellena con otra cosa, no se añaden ejercicios y no se supone material.
8. **¿Qué ocurre con los cuatro perfiles del 46 %?**
   - **NO_FACTIBLE** con `PLAN-C2D-EF-0.1`: dos PULL sin tracción y 0 tracciones elegibles.
   - Ni con menos ejercicios hay un plan válido (`CASOS_EXTREMOS_46_PORCIENTO.md`).
9. **¿Qué ocurre con R0800?**
   - **NO_FACTIBLE** por la misma causa: su «fuerza» de PULL era un empuje.
   - El volumen de pierna (21 series por sesión) no cambia y va a revisión. No se impone ningún tope (`AUDITORIA_R0800.md`).
10. **¿Cuántos planes siguen siendo válidos con la corrección?**
    - **902** de 960 con `PLAN-C2D-EF-0.1` (y con `PLAN-C2D-V4-EF-0.1`).
    - **842** con `PLAN-EF-0.1`.
    - Frente a 951 con C2-D y 875 con la histórica.
11. **¿Cuántos dejaron de ser válidos con razón?**
    - **49** frente a C2-D: 48 NO_FACTIBLE por VEF (peso corporal, 6–7 días, PUSH/PULL/LEGS) y R0808, rechazado por V20 porque C2-D no lo exime (existe una sustitución).
    - **33** frente a la histórica.
12. **¿Qué alternativas funcionalmente válidas se encontraron?**
    - Ninguna tracción con peso corporal **sin material**.
    - El catálogo tiene tracciones **con** material (barras de pared o anclaje, V39): un usuario que lo tenga puede declararlo.
    - En 282 planes, EF sustituyó colocaciones no computables por otras compatibles, y siguen siendo válidos.
13. **¿C2-D sigue aplicándose solo cuando corresponde?**
    - **Sí:** solo cuando todo es V20.
    - Con EF: 60 excepciones (antes 76) y 4 NO_SATURADA.
    - Ningún plan funcionalmente inválido se publica por C2-D.
14. **¿La saturación considera ahora la elegibilidad real?**
    - **Sí:** el verificador usa el validador con VEF y parte de un borrador sin colocaciones no computables.
    - Las 16 saturaciones de contenido sin tracción pasan a NO_FACTIBLE.
15. **¿L6 y L8 continúan tras el dolor sin romper restricciones?**
    - **L6, sí:** P-03 publica al 40,1 % y no reaparece ningún excluido.
    - **L8, no:** con EF no tiene plan **en ninguna semana** (PULL sin tracción con peso corporal). Es un NO_FACTIBLE legítimo.
    - Con C2-D sin EF, L8 continuaba con contenido no pertinente.
16. **¿Qué casos deben seguir NO_FACTIBLE?**
    - R0788, R0792, R0836, R0840 y R0800.
    - Los 48 de peso corporal en PUSH/PULL/LEGS.
    - L4 y L8 con EF.
    - L7 S4 (P-03 subiría el trabajo tras el dolor).
    - Los 6 de V15.
17. **¿D8, ACORTAR y P-03 funcionan con C2-D activo e IA simulada?**
    - **Sí.** 225 pruebas con `PLAN-C2D-0.1` y otras 225 con `PLAN-C2D-EF-0.1`: 222 pasan y 2 se omiten.
    - **1 falla, y es esperada:** la aserción histórica del piso de V20 en GAIN_MUSCLE a 75 min, que es justo lo que C2-D cambia (`PRUEBAS_IA_D8_ACORTAR_C2D.md`).
18. **¿La concurrencia y la idempotencia son correctas?**
    - **Sí:** `CicloConcurrenteIT` pasa 9 de 9 con las dos políticas.
    - Los cuatro ciclos tienen como máximo 1 plan activo por semana, repetición idéntica, política del plan igual a la configurada y 0 excluidos (`PRUEBAS_CONCURRENCIA_C2D.md`).
19. **¿V4-C sigue funcionando sin alterar planes históricos?**
    - **Sí:** 902 de 902 válidos de C2-D-EF y V4 dentro de `segundos × 100 ≤ M × 60 × 115`.
    - `V4CValidadorRealTest` pasa 19 de 19.
    - La histórica es idéntica: 960 de 960.
20. **¿La duración visible evita afirmaciones de suficiencia no demostradas?**
    - **Sí:** el mensaje es ahora «La duración se calculó según los ejercicios disponibles y las restricciones de tu plan».
    - La prueba prohíbe «todos», «complet», «admite», «suficiente» y otras.
    - Flutter no se ha tocado.
21. **¿Se corrigieron los efectos negativos de la preferencia LW?**
    - **Sí:** 0 planes sin brecha cambiados (antes 82) y 0 válidos que pasan a inválidos (antes 2).
    - Mantiene las mismas 59 mejoras (34 completas).
    - El cardio y las series de fuerza no cambian.
    - Se corrigió además el recuento del informe anterior.
22. **¿Qué evidencia requiere revisión profesional?**
    - La muestra de 36 planes (`MUESTRA_REVISION_PROFESIONAL_EF.csv`): casos extremos, R0800, L6 y L8, LW sin fuerza y con la preferencia, 10 regenerados por EF, 6 saturados con EF y 5 de control.
    - El instrumento de 12 criterios.
    - Las familias de EF.
    - Los 12 ejercicios a revisión.
    - **No hay ninguna evaluación registrada:** no hay evaluadores.
23. **¿Cuántas pruebas pasaron y cuántas se omitieron?**
    - Unitarias: **992 pasan, 0 fallos, 0 omitidas.**
    - Integración: **473 contabilizadas, 428 ejecutadas sin fallos, 45 omitidas** (opt-in).
    - Laboratorios opt-in: 8 de 8.
    - Baterías con la política candidata: 2 × 225 (222 pasan, 1 diferencia esperada, 2 omitidas).
24. **¿La campaña histórica sigue idéntica?**
    - **Sí:** REPLAY sin fallos (1 ejecutada y 1 omitida), con la evidencia idéntica salvo las marcas de tiempo de `exclusiones_dolor.csv`.
    - Las matrices HIST, C2D y C2D-V4 son idénticas perfil a perfil a las anteriores.
25. **¿Se tocaron Neon, Railway o Replicate?**
    - **No.** Solo PostgreSQL efímero y 0 llamadas a Replicate.
26. **¿Qué cambios requieren tu aprobación final?**
    1. Fusionar el PR y desplegar **V44 y V45** (solo esquema).
    2. Activar una versión con EF (`PLAN-C2D-EF-0.1` o `PLAN-C2D-V4-EF-0.1`) en lugar de `PLAN-C2D-0.1`, **tras la revisión profesional** y el contrato visible en Flutter.
    3. Decidir qué hacer con los perfiles NO_FACTIBLE con peso corporal: declarar material, cambiar de división o validar una tracción sin material.
    4. Las correcciones del catálogo (requisitos de equipo, reclasificaciones, 315, 303 y 305).
    5. Activar `LW-PREF-0.2`.
    6. Parametrizar por versión la prueba histórica del piso de V20.
    7. Los indicadores de API (duración y métrica aeróbica).
27. **¿El PR #17 está técnicamente listo para revisión final?**
    - **Sí, para revisión técnica final:** todo está versionado, desactivado por defecto, probado y documentado, y la histórica es idéntica.
    - **No implica activación.**
    - La activación sigue bloqueada por la revisión profesional (R13), Flutter (R12) y la decisión sobre el peso corporal (R1).

**La validación técnica no equivale a validación profesional.** Ningún plan se ha aprobado como adecuado.
