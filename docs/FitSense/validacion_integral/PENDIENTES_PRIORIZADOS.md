# Pendientes priorizados

Fecha: 16/11/2026. Ninguno es un defecto del código de producción encontrado en esta orden. Son integraciones
pendientes, decisiones por confirmar, funciones que faltan y riesgos de pruebas o de despliegue. Nada de esto se ha
implementado: la orden prohíbe introducir funciones sin identificar antes su necesidad.

Los identificadores PD-xx no tienen relación con las políticas P-01, P-02 y P-03.

## Prioridad alta: antes de integrar o de abrir el estudio

### PD-02. Integrar #73, #71 y #72, en ese orden (integración)

Sin #73, el CI de cualquier rama está rojo. Sin #72, #71 tiene los defectos conocidos (600 s, latido, predicciones
repetidas).

Orden propuesto:

1. #73 → `generation`.
2. Actualizar #71 con `generation`.
3. #71 → `generation`, sin desplegar.
4. Actualizar #72.
5. #72 → `generation`.
6. Desplegar solo con las tres dentro.

`CORRECCION_CI.md` muestra la combinación #72 + #73 en verde.

**Decide:** el usuario. Aquí no se fusiona nada.

### PD-01. Pruebas con fecha relativa (pruebas)

38 clases de integración calculan la semana con `TrainingWeek.containing(LocalDate.now())`. Hoy pasan, pero son el
mismo patrón que rompió el CI el 16/11. Las que dependen de la semilla son las de mayor riesgo:

- `Plan55CardioIT`, `Plan58FuerzaIT`, `Plan60HipertrofiaIT`;
- `V20ComposicionCasosIT`, `ComposicionE1E2IT`;
- `CatalogoV39IT`, `CatalogoV40IT`, `CatalogoV41IT`;
- `ProgresionCienPorCienIT`, `PendientesProgresionIT`.

**Propuesta:** migrarlas a `RelojPruebas.SEMANA_REFERENCIA`, como ya se hizo con 32 clases. Cada migración debe
mantener sus aserciones. Va en una PR propia y solo toca pruebas.

**Decide:** el usuario (el alcance de la PR).

### PD-03. Recuperación de contraseña sin envío (función incompleta)

`fitsense.password-reset.delivery=none`: el token se genera y se valida, pero no se envía ningún correo. Sin
proveedor, un participante que olvide la contraseña depende del equipo.

**Propuesta:** elegir el proveedor y configurarlo en el despliegue. El código ya admite los notificadores
(`PasswordResetNotifiersTest`).

**Decide:** el usuario o el equipo de despliegue.

### PD-07. Un slot vacío no da 422 (decidido: se conserva OBSERVE de forma provisional)

`fitsense.coverage.pregeneration=OBSERVE`: un perfil que vacía un slot obligatorio recibe un plan parcial, explicado en
`/plan/limitations`, en vez de un NO_FACTIBLE.

**Decisión del 16/11/2026:**

- Se conserva OBSERVE de forma provisional.
- No se cambia a `ENFORCE_*` (422) durante la integración, ni de forma automática.

**Pendiente antes de usarlo con participantes reales:** una evaluación separada de los casos de cobertura parcial.
Debe cubrir:

- qué perfiles la producen;
- qué slots quedan vacíos;
- cómo lo explica `/plan/limitations`;
- qué implica para el objetivo y para la adherencia.

### PD-16. Carrera en la reparación de seguridad concurrente (defecto menor, preexistente)

`ProteccionPlanesPublicadosIT.reparacionesSimultaneas` falló 1 vez en la regresión de #72 actualizada y pasó en las
otras 16 ejecuciones. Si dos reparaciones se cruzan entre sus fases 1 y 2, la perdedora informa NO_VALIDA en vez de
CAMBIO_CONCURRENTE. No publica nada y no hay riesgo, y el servicio no tiene llamadores en producción. Detalle y
corrección propuesta en `FALLO_INTERMITENTE_REPARACION_CONCURRENTE.md`.

**Decide:** el usuario (continuar o corregir antes).

## Prioridad media

### PD-04. Reparación NO_VALIDA: la sesión sigue bloqueada (decidido: se mantiene la conducta conservadora)

**El caso.** El único sustituto seguro del ejercicio inseguro necesita más dosis que el ejercicio quitado. Por eso la
reparación de seguridad queda NO_VALIDA y no publica nada. Así lo exigen V24 (dosis exacta de capacidad) y V8C (no
subir el trabajo). El barrido de `DIAGNOSTICO_REPARACION_LINAJE.md` lo da en 9 de 16 semanas para ese escenario.

**Decisión del 16/11/2026:**

- Se mantiene la conducta conservadora.
- No se fuerza una reparación inválida.
- No se relajan V24 ni V8C.
- La sesión afectada sigue bloqueada hasta un ciclo posterior.

**Consecuencias para el usuario:**

- El plan muestra la sesión con `safetyBlocked = true` y su aviso (`safetyBlockMessage`).
- `POST /workouts/{id}/start` la rechaza (`WorkoutNotAvailableException.unsafe`).
- El resto de la semana se puede entrenar con normalidad.
- La semana siguiente la genera el ciclo semanal con las restricciones vigentes, así que el ejercicio inseguro no vuelve.

**Consecuencias para la adherencia (sin cambiar la fórmula oficial):**

- La sesión bloqueada sigue siendo un entrenamiento **previsto** (cuenta en m).
- Si no se hace, su A_s es 0, igual que una sesión omitida.
- Con 3 sesiones previstas y las otras 2 cumplidas, la semana da 66,67 % y no 100 %.
- La causa no queda en `skip_reason` (lo elige la persona). V57 (`system_block_cause`) deja constancia de la causa de
  seguridad cuando un ejercicio marcado se registra como omitido. Para el análisis, una semana así se identifica por
  sus sesiones `safetyBlocked`.
- Es un efecto conocido y acotado, que debe tenerse en cuenta al interpretar la adherencia de esas semanas.
- No se corrige cambiando la fórmula.

**Pendiente:** ninguno técnico. La revisión profesional puede reconsiderar la capacidad del sustituto (104) sin tocar
los validadores.

### PD-05. Panel: vista por participante (falta para el panel)

Hay que ver los planes y versiones, las sesiones, las adaptaciones con sus causas, las exclusiones por dolor y las
sesiones `safetyBlocked`. Los datos existen y hoy se consultan por SQL.

**Propuesta:** endpoints de solo lectura bajo `/admin/participants/{id}/...`, solo cuando se defina el panel.

**Decide:** el usuario.

### PD-06. Panel: exportación de datos del estudio (falta para el panel)

Se necesita para el análisis de la tesis: CSV por semana y participante, con la adherencia oficial, las decisiones y
las causas.

**Decide:** el usuario.

### PD-08. Interruptor de la IA en memoria (despliegue)

`AirCircuitBreaker` es local a cada instancia: con varias réplicas, cada una tiene el suyo. Es aceptable con **una**
instancia. No se prepara Railway todavía.

**Decide:** el equipo de despliegue.

### PD-10. Panel: seguimiento de las generaciones asíncronas (falta para el panel)

`plan_generation_operations` y `plan_generation_predictions` (#71/#72) no tienen ninguna vista de administración.

**Decide:** el usuario.

## Prioridad baja

### PD-09. Flutter (cliente)

- **PR #2.** Llama a `/users/me/plan/generation`, así que solo debe desplegarse después de #71 + #72.
- **`runCycle()`.** Es código muerto: en producción da 403. Conviene quitarlo o dejarlo detrás de una opción de
  desarrollo.

**Decide:** el equipo móvil.

### PD-12. Roles por SQL (operación)

Cambiar un rol exige acceso a la base (`cambiar_rol.sql`). Es suficiente para un estudio con pocos administradores.

**Decide:** el usuario.

## Registradas para fases posteriores (no se desarrollan en esta orden)

### PD-13. Evaluación de H2 (validación)

La PR #69 sigue sin fusionar y H2 sigue apagado. Evaluarlo exige el arnés pareado A/B ya preparado y un presupuesto.

**Decide:** el usuario.

### PD-14. Preparación de Railway (despliegue)

Incluye:

- las variables (`JWT_SECRET`, `FITSENSE_*`);
- una sola instancia mientras el interruptor de la IA esté en memoria (PD-08);
- las migraciones V58 y V59 sobre la base real;
- `FITSENSE_MANUAL_CYCLE_ENABLED=false`.

**Decide:** el usuario.

### PD-15. Mobile en un Android físico (cliente)

Hay que probar la PR #2 de MobileApp contra el backend integrado: la generación asíncrona, la recuperación tras cerrar
la app y la red móvil.

**Decide:** el equipo móvil.

## Antes de usar la IA real

### PD-11. Prueba con Replicate real (validación)

La resiliencia está probada con Replicate simulado (#72). Falta la ronda real con presupuesto
(`docs/FitSense/generacion_asincrona/DECISION_PREPARACION_IA_REAL.md`). H2 y P-03 siguen sin activar.

**Decide:** el usuario (autorizar el gasto).

## Resumen por tipo

| Tipo | Pendientes |
|---|---|
| Integración | PD-02 |
| Pruebas | PD-01 |
| Funciones que faltan | PD-03, PD-05, PD-06, PD-10 |
| Decisiones (tomadas el 16/11/2026; PD-07 con evaluación pendiente) | PD-04, PD-07 |
| Despliegue y operación | PD-08, PD-12 |
| Cliente | PD-09 |
| Validación con IA real | PD-11, PD-13 |
| Despliegue y cliente posteriores | PD-14, PD-15 |
