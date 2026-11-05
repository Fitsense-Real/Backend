# 09 · Comparativo final, conclusiones y limitaciones

**Versiones comparadas:**
- **Anterior:** `89a1e4f`, la congelada del PR #41, con R/R′ en `2dc4abe`.
- **Corregida:** `7f2da39`, rama `investigacion/k20-post-rrp-auditoria`. Contiene AUD-G1 y el prototipo P-G2 apagado.

**Alcance de la versión corregida:**
- **Sin Replicate.** La fase 9 no se ejecutó, porque requiere un presupuesto nuevo autorizado y porque la puerta de la fase 8 deja decisiones pendientes (documento 07 §5).
- Todo lo de la columna «corregida» procede de las ejecuciones gratuitas del documento 07.

Clasificación: **COMPROBADO / CORREGIDO / NO CUMPLE / PENDIENTE DE VALIDACIÓN / NO EVALUADO**.

## 1. Tabla comparativa

| Aspecto | Versión anterior | Versión corregida (medida) | Clasificación |
|---|---|---|---|
| U01–U20 | 20/20 (D, D′, R, R′) | **20/20** en D y D′ (huella `f9f67973…`, idéntica) y 20/20 en IA simulada | **COMPROBADO** (sin IA real: NO EVALUADO con Replicate) |
| J01–J16 | 15/16 | **15/16** en D, D′ e IA simulada (huella `276d535d…`, idéntica) | **NO CUMPLE** el objetivo 16/16 |
| J03 | Sin plan (V20) | **Sigue sin plan.** Causa demostrada: la mitad de V15 sobre grupos no prescribibles, más V20. Con P-G2 (laboratorio) hay plan, pero de calidad discutible y con efectos colaterales (168 planes de LW cambian). | **NO CUMPLE / PENDIENTE DE VALIDACIÓN** (decisión P-G1/P-G2) |
| Sesiones no factibles por políticas | 5 (U03 S3, U04 S3, J01 S4, J09 S4, J13 S3) | **Las mismas 5**, reproducidas por la sonda con IA simulada. **La nueva cohorte K añade 4** (K01, K09 y K13 de GM; **K10 de INCREASE_STRENGTH**). Causa: matriz de permisos de `TipoIntervencion` + 4D/V24 + V20 + V9. | **NO CUMPLE**: conflicto de política demostrado; corrección pendiente de aprobar (P-A1, P-A2) |
| Adaptaciones factibles | 56/56 en dos corridas (R + R′, U y J) | **Sin IA real: NO EVALUADO.** Con IA simulada, todas las factibles de U, J y K se publican sin incumplimientos (U 10/10, J 18/18 y K 18/18 factibles publicadas; ACC/ALT de la IA simulada: U 10/0, J 17/1, K 15/3). | **COMPROBADO con un doble** / NO EVALUADO con Replicate |
| Seguridad | 0 incumplimientos | 0 incumplimientos del oráculo v2 en D, D′ e IA simulada sobre U, J y K | **COMPROBADO** (con el oráculo v2; sin validación profesional) |
| Regresión de los cuatro objetivos | Conforme al informe anterior | Unitarias 1.218 y de integración 525: **0 fallos** (43 opt-in omitidas). `GeneracionReferenciaIT` 72/72 idénticos. | **COMPROBADO** |
| Generación inicial (barrido de 3.780 perfiles) | 154 sin plan (no medido antes de esta auditoría) | **139 sin plan.** +15 por AUD-G1 y 0 empeoran. | **CORREGIDO** (15) / **NO CUMPLE** (139, pendiente de decisión) |
| Progresión y reincorporación | Apagada; sin evidencia en K20 | Sigue apagada. Las IT de progresión que ya existen pasan en la regresión (`ProgresionIT` 24/24, 100 % 5/5, pendientes 6/6, copia sin IA 12/12, linaje 1/1). Hay discrepancias y riesgos documentados (H1–H15), sin corregir. | **PENDIENTE DE VALIDACIÓN** (y NO EVALUADO en K20) |
| Nueva cohorte independiente | No aplica | K01–K16, publicada antes de ejecutarla (`28422fe`): **16/16** en D, D′ e IA simulada; 4 sesiones no factibles por política | **COMPROBADO** sin IA real; NO EVALUADO con Replicate |
| Incidencias del proveedor | 2 predicciones fallidas sin métricas, que bloquearon el resto de su corrida | Causa analizada. Propuesta P-H1: una predicción **fallida** sin métricas carga la reserva sin bloquear, manteniendo la cota de presupuesto. Prueba unitaria `PresupuestoReplicateK20Test.sinMetricas` (commit `6f8b1b9`). | **PENDIENTE DE VALIDACIÓN** (aprobación del protocolo; NO EVALUADO con Replicate) |

**Denominadores y exclusiones:**
- Los «20/20», «15/16» y «16/16» son recorridos completos del oráculo v2 sobre los perfiles de cada cohorte.
- Las sesiones no factibles **no** se excluyen del análisis: se cuentan aparte.
- Nada de esto mide la adherencia real de personas.

## 2. Respuestas a las nueve preguntas de la fase 10

**1. ¿Qué problemas eran defectos de implementación?**
- **G1:** `asegurarGrupos` solo sabía cambiar ejercicios, no añadirlos. Afectaba a LW de 1 día. **Corregido.**
- **G5:** la generación es sensible a la semilla en GF principiante en casa a 75 min (3 perfiles). No corregido.
- **H1** del arnés: el bloqueo por una predicción fallida sin métricas. Es infraestructura de evaluación, no producto.
- **P2 y P3**, en la progresión: la herencia de dosis al apagar el interruptor y en la reparación de planes publicados. Defectos probables; uno lo demuestra una prueba existente.
- Discrepancias documentales: progresión al 100 % frente a 80 %, y los comentarios de `application.properties`.

**2. ¿Qué problemas eran restricciones excesivas?**
- **V20** como piso duro (70 % de `sessionMinutes`). No tiene respaldo científico y es la regla que bloquea los 139 perfiles sin plan.
- **No poder reducir la dosis ante fatiga en GM**, la regla menos respaldada por la evidencia.
- **INVESTIGAR_CAMBIO_MINIMO** sin ninguna sustitución, que hace imposibles las intervenciones en GM y también en IS (K10).
- La **mitad de V15** sobre grupos no prescribibles.
- El **perfil bloqueado** tras el primer plan.

**3. ¿Qué reglas resultaron coherentes y deben mantenerse?**
- V1 (elegibilidad), V6 (dificultad), V11 (carga) y V8C (no tocar lo no señalado, no subir dosis).
- La seguridad ante el dolor: memoria P-02, sustitución segura y respaldo `NO_FACTIBLE`.
- El límite de dificultad en «más sencillo», la conservación sin causa registrada (U07, U12) y el mínimo de V15 sobre grupos prescribibles.
- El descanso de 90 s en GM y los pasos de progresión pequeños, con techo y un paso por ciclo.
- La separación entre la V24 de la generación inicial y las autorizaciones de adaptación y progresión.

**4. ¿Qué reglas se modificaron y por qué?**
- **Ninguna regla.**
- Solo se corrigió el algoritmo de construcción (AUD-G1), para que cumpla V15 cuando ya había margen.
- P-G2 existe solo como prototipo apagado de laboratorio.

**5. ¿Qué limitaciones técnicas permanecen?**
- 139 perfiles admitidos sin plan inicial (V20).
- 9 sesiones (5 en U y J, 4 en K) que la política hace no adaptables.
- PULL sin tracción en casa sin material.
- La dificultad del catálogo no codifica el impacto.
- La progresión, sin evaluación integrada en K20.
- La variabilidad del modelo: 13/35 pares R/R′ con un cambio distinto, aunque de la misma categoría.

**6. ¿Qué decisiones necesitan validación profesional?**
- Mínimos de dosis semanal por objetivo, si V20 deja de ser un piso (P-G1).
- Tamaño y duración de la reducción por fatiga en GM (P-A1).
- Techo de series por grupo en GM (36/semana hoy).
- Cardio de impacto para principiantes en casa.
- Ejercicios de tracción sin material.
- Derivación ante dolor persistente.
- Dosis con peso corporal (rango de repeticiones y RIR).

**7. ¿Qué resultados sostienen el funcionamiento del núcleo?**
- En U, J y K, con D, D′ e IA simulada: generación determinista y reproducible (huellas idénticas a la versión congelada).
- 0 incumplimientos del oráculo.
- Las sesiones factibles se adaptan y publican.
- Lo no factible se detecta y se publica la copia, sin publicar planes inválidos.
- Regresión sin fallos: 1.218 unitarias + 525 de integración.
- El barrido de 3.780 perfiles es reproducible y explica cada fallo.

**8. ¿Qué resultados NO permiten afirmar una mejora real de adherencia?**
- **Ninguno de los de esta auditoría.** La adherencia es simulada (patrones H, O y P40 del guion); no hay usuarios reales ni seguimiento longitudinal.
- La pertinencia de las adaptaciones es técnica (reglas y oráculo), **no profesional**.

**9. ¿Hay evidencia suficiente para usar la nueva versión en el short paper?**
- **Para la generación y la adaptación sin IA real, sí**, como una corrección menor (AUD-G1): no cambia ningún resultado de U ni de J.
- **Para sustituir los resultados R/R′ del PR #41, no**: la versión corregida no se ha evaluado con Replicate.
- **No hay evidencia para afirmar que J03 o las 5 sesiones no factibles estén resueltas.**
- Si el paper se actualiza, debe declarar como limitaciones:
  - J03;
  - las sesiones no factibles por política;
  - los 139 perfiles sin plan del barrido;
  - la ausencia de validación profesional.

## 3. Conclusión: ¿forman las restricciones de FitSense un sistema coherente y funcional?

**Parcialmente.**
- **Lo que funciona:**
  - El núcleo de seguridad es coherente y está comprobado: elegibilidad, dificultad, dolor, V8C y no subir dosis.
  - El sistema nunca publica un plan inválido y explica sus fallos.
- **Lo que no funciona:** el **sistema de duración y dosis** es incoherente en su diseño.
  - V20 convierte una preferencia de tiempo en una restricción dura.
  - Ese piso obliga a derivar los topes y el volumen de GM (hasta 36 series por semana).
  - En la adaptación, V20 choca con las intervenciones de fatiga y desmotivación.
  - Y en LW choca con V15.
- **Consecuencia:** la mayoría de los fallos (139 perfiles sin plan, 9 sesiones no adaptables, J03) **no son defectos del código ni del modelo de IA, sino de esa política**.

Corregirlos exige decisiones tuyas, con apoyo profesional:
- **P-G1** (`sessionMinutes` como máximo y dosis mínima semanal);
- **P-A1** (dosis de mantenimiento ante fatiga);
- **P-A2** (una sustitución lateral ante desmotivación).

Hasta entonces, la versión corregida es **tan segura como la anterior y algo más funcional**: 15 perfiles más con plan y 0 regresiones. Pero **no cumple** el objetivo de generar y adaptar en todos los casos admitidos.
