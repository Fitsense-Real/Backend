# Arquitectura candidata del modelo E (experimental, desactivada)

## 1. Lo que existe en `feature/modelo-e-planificacion`

| Componente | Paquete | Qué hace | Estado |
|---|---|---|---|
| `ParametrosModeloE` | `planning/domain/services/experimental` | Parámetros versionados (`E-0.1-EXPERIMENTAL-SIN-APROBAR`): objetivo semanal por objetivo, escalado `min(objetivo, 4 × días)`, techo blando de 10 series por grupo y sesión, techo de 20 por grupo y semana, techo de tiempo = M | `habilitado = false`; **valores sin aprobar** |
| `VolumenSemanal` | ídem | Cuenta series directas por `body_part` y sesión, por semana, y minutos de cardio | Dominio puro |
| `ValidadorModeloE` | ídem | Validador candidato E1–E4: tiempo > M, sesión > techo, semana > techo, grupo grande entrenable bajo el objetivo escalado | **No lo llama `PlanDraftValidator`**; solo lo usa el laboratorio |
| `ConstructorSemanaModeloE` | `planning/infrastructure/generation/experimental` | Ejecuta el motor vigente con el piso de V20 a 0 (copia en memoria del contexto) y añade, con la dosis **base** aprobada, lo que falte hasta el objetivo escalado, respetando V18, V22, enfoque y techos | No es un bean; se niega a construir con parámetros deshabilitados salvo en modo laboratorio |
| `ModeloEExperimentalTest` | test | Parámetros, volumen, validador y **prueba de aislamiento**: ninguna clase de producción fuera de `experimental` referencia el modelo E | En la batería normal |
| `LaboratorioModeloEIT` | test (opt-in) | Matriz de los tres caminos y simulación longitudinal con progresión | `-Dfitsense.laboratorio.e=true` |

**La ruta histórica no cambia:** generación, adaptación, P-03, progresión y reincorporación no ven estas clases (lo comprueba la prueba de aislamiento). Con E desactivado, la batería completa y la campaña de 100 semanas deben salir iguales (ver `SIMULACIONES_Y_REGRESIONES.md`).

## 2. Lo que haría falta para activarlo (no hecho; requiere aprobación)

1. **Versión de política.** Hoy `calculation_configs` versiona los parámetros (`MVP-1.x`), pero no la *forma* de las reglas. Haría falta:
   - una clave `planning_policy` (p. ej. `VIGENTE_4D_V4` / `MODELO_E_V1`) en una versión nueva de `calculation_configs`;
   - que generación y validación la lean;
   - que cada plan guarde la versión con la que se generó y se validó (ya existe `calculation_version` en las métricas; habría que extenderlo a `weekly_training_plans` con una migración).
2. **Validación.** Haría falta:
   - un `PlanDraftValidator` que, según la versión, aplique V20 (vigente) o E1–E4;
   - que V24 (tope) se mantenga en la versión vigente y se sustituya en E por E2/E3 más el techo de tiempo;
   - **nunca una autorización genérica para saltarse V8C o V24.**
3. **Generación.** Habría que conectar el constructor en `RuleBasedTrainingPlanGenerator` solo para la versión E y adaptar el prompt de la IA, que hoy recibe `min_session_minutes` y los topes de 4D, para que reciba los objetivos de volumen. Sin esto, la IA y el validador divergirían.
4. **Adaptación y progresión.** Haría falta:
   - que V8C compare con la semana medida igual que hoy;
   - que las reducciones (D8, ACORTAR, dolor) eximan de E4, como hoy D8 exime de V20;
   - que la progresión no sea empujada por E4. Hoy la progresión es D-P3 (+2 repeticiones, hasta 3 series), con un límite del 5 % por paso, un techo acumulado de 1,10 × V_base (D-P5, `ProgresionResolver.TECHO`) y la banda del 60–125 % de la semana 1 en la tabla de decisión: **una cuota de volumen no puede forzar una subida con dolor o fatiga**.
5. **Compatibilidad histórica.** Los planes antiguos se revalidan con su versión; nada se migra retroactivamente.

## 3. Coste estimado

| Ámbito | Cambio |
|---|---|
| Base de datos | 1 migración: versión de política en el plan; fila nueva en `calculation_configs` |
| Validador | Rama por versión; E1–E4; exención por D8 |
| Motor | Conexión del constructor E; ajuste del repaso semanal |
| IA | Contrato GEN-IN y prompt: objetivos de volumen por grupo; pruebas sin coste |
| Pruebas | Las 458 IT y la campaña deben pasar con la versión vigente; una batería espejo con E |
