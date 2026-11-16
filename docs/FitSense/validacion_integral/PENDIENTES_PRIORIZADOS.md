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

### PD-07. Un slot vacío no da 422 (decisión por confirmar)

`fitsense.coverage.pregeneration=OBSERVE`: un perfil que vacía un slot obligatorio recibe un plan parcial, explicado en
`/plan/limitations`, en vez de un NO_FACTIBLE. Es la configuración entregada a propósito.

Hay que confirmarla para el estudio. Las opciones son:

- dejarla en OBSERVE;
- pasar a `ENFORCE_*`, que da 422 y no genera.

No se cambia sin decisión expresa.

**Decide:** el estudio.

## Prioridad media

### PD-04. Sesión `safetyBlocked` tras una reparación NO_VALIDA (limitación conocida)

Si el único sustituto seguro del 1374 necesita más dosis que el ejercicio quitado, la semana se queda como estaba. La
sesión afectada no se puede iniciar hasta el ciclo siguiente: V24 y V8C, sin subir trabajo. El barrido de
`DIAGNOSTICO_REPARACION_LINAJE.md` lo da en 9 de 16 semanas para ese escenario concreto.

**Propuesta:** decisión profesional. Las opciones son:

- aceptarlo (la decisión vigente);
- revisar la capacidad del 104 para que admita 2x10.

No se cambian ni las políticas ni el catálogo sin decisión.

**Decide:** la revisión profesional.

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
| Decisiones por confirmar | PD-04, PD-07 |
| Despliegue y operación | PD-08, PD-12 |
| Cliente | PD-09 |
| Validación con IA real | PD-11 |
