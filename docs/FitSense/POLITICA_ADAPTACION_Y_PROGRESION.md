# Política de adaptación y progresión · cierre (28/10/2026)

Decisiones de la orden «Cierre de adaptación y ajuste de progresión». Este documento describe **lo que hace el backend**; las pruebas que lo demuestran están en §6.

## 1. Cuándo se adapta y cuándo se progresa

| Adherencia semanal observada | Adaptación (IA) | Progresión (backend) |
|---|---|---|
| < 50 % | Se evalúa una adaptación **ampliada** (todas las sesiones señaladas) | No |
| 50 % a < 80 % | Se evalúa una adaptación **localizada** (la peor sesión señalada) | No |
| 80 % a < 100 % | Mantenimiento (copia) | No |
| 100 % una semana | Mantenimiento | No |
| 100 % **real** dos semanas consecutivas | No hace falta adaptar | Se **evalúa** la progresión |

- **La adaptación sigue con su 80 %** (`good_threshold_pct` y `moderate_threshold_pct` de la configuración, sin cambios). Además, solo se adapta si hay registro suficiente (`EvidenceRule`) y alguna sesión señalada. Tener menos del 80 % no garantiza una llamada a Replicate.
- **La progresión ordinaria tiene su propio umbral, del 100 %** (`ProgresionResolver.UMBRAL_PROGRESION`, en el código y no en la configuración, para no cambiar el de la adaptación). «100 % real» significa, en **las dos** semanas:
  - adherencia y cobertura de registro al 100 %;
  - todas las sesiones registradas y completadas al 100 %;
  - todos sus ejercicios completados al 100 %, ninguno omitido ni parcial.

  El porcentaje solo no basta: se guarda con dos decimales, y la adherencia se calcula sobre lo registrado (una sesión sin registrar daba «100,00 %»). Una semana sin sesiones no cuenta.
- **El 100 % habilita la evaluación, no garantiza un paso.** Se mantienen los límites: 5 % por paso, techo de 1,10 × base, V24, V25, V8C y la identidad del paso. Sin un incremento válido, la rutina se conserva.
- **Frecuencia (opción A, por sesión):** cada sesión necesita que la transición anterior de **su** linaje sea una copia sin orden. Una sesión que acaba de progresar espera dos semanas suyas posteriores al aumento; otra sesión puede progresar entretanto. No hay un bloqueo global por participante.
- **Recuperación y reincorporación** mantienen sus condiciones (80 %, dos semanas, sin fatiga ni dolor). La reincorporación exige una `PROGRESION` previa tras el último acortamiento, así que indirectamente tardará más (efecto aceptado). Si las semanas no son perfectas y aún cabía una progresión, la sesión **se mantiene**: no se salta a reincorporar porque la progresión esté bloqueada.
- **Toda la progresión es determinista:** `ai_calls = 0` en V32.

## 2. Los cinco motivos del flujo móvil

| Motivo mostrado | Código (`SkipReason`) | Intervención (`TipoIntervencion`) |
|---|---|---|
| No tuve tiempo | `LACK_OF_TIME` | `ACORTAR_O_REORGANIZAR` |
| Me sentí muy cansado | `FATIGUE` | `REVISAR_CARGA_Y_RECUPERACION` |
| Me pareció demasiado difícil | `TOO_DIFFICULT` | `VARIANTE_MAS_SENCILLA_O_CARGA` |
| Sentí dolor o molestias | `PAIN_OR_DISCOMFORT` | `SEGURIDAD_ANTE_DOLOR` |
| No tuve motivación | `LACK_OF_MOTIVATION` | `INVESTIGAR_CAMBIO_MINIMO` |

- **Iguales para los cuatro objetivos.** Lo que cambia por objetivo son las restricciones (p. ej. en `GAIN_MUSCLE`, V24/4D no admite reducir las dosis obligatorias).
- **Compatibilidad.** El backend sigue aceptando `SCHEDULE_CHANGE` (→ `REPROGRAMAR_DIAS`) y `OTHER` (→ `CAUSA_NO_REGISTRADA`, que conserva): los usan registros existentes, `ck_wse_skip_reason` y `ck_wum_skip_reason`. Se retiran **solo del selector de Flutter**; no se borra ningún valor ni ningún registro.
- **Registrar un motivo no llama a la IA.** La pantalla de motivo y la decisión semanal son procesos distintos: el motivo se guarda; la decisión semanal decide si adaptar (§1).
- **Cumplimiento completo con una observación** (p. ej. una sesión completada por encima del umbral con un ejercicio con molestias): no hay incumplimiento que adaptar, así que no hay IA. La seguridad va por su camino de siempre: V8C no vuelve a publicar un ejercicio que dolió, y `SafetySubstitutionPolicy` lo sustituye por uno seguro o la semana no sale.

### 2.1 Dolor entre varios motivos (corregido)

El motivo de una sesión parcial era la **causa dominante**, es decir, el motivo más repetido entre sus ejercicios. Un dolor frente a dos fatigas daba `FATIGUE`, y un empate era arbitrario. La sesión se adaptaba entonces con una intervención que no permite sustituir el ejercicio que dolió.

**Ahora, si algún ejercicio se dejó por dolor, la sesión se trata con `SEGURIDAD_ANTE_DOLOR`** (`WorkoutSession.algunEjercicioConDolor` → `SessionOutcomeIdentifier`). La causa dominante que guardan las métricas semanales (`weekly_user_metrics.dominant_skip_reason`) **no cambia**; no se toca ningún registro.

## 3. Dolor sin alternativa válida: `NO_FACTIBLE` (sin cambios)

1. Se identifica la sesión y sus restricciones.
2. Se piden alternativas a Replicate (3 intentos).
3. Se validan ejercicios, dosis, volumen (no más que antes), tiempo y seguridad.
4. Solo se publica una propuesta completamente válida.
5. Si ninguna lo es, el respaldo **no** vuelve a publicar la sesión con el ejercicio que dolió: `NO_FACTIBLE`, con el motivo de los rechazos en V32 (`rejection_reason`, `conservation_report`).

No se publica una sesión «retirando solo lo que dolió» ni una semana parcial para evitar `NO_FACTIBLE`; quedan como mejora futura con evaluación propia.

**Para Flutter:** `NO_FACTIBLE` por dolor **no** es un error de conexión. El mensaje debe decir:
- que se registró la dificultad por dolor;
- que FitSense no encontró una alternativa que superara las validaciones;
- que la sesión requiere revisión;
- que no debe asumirse que repetir el ejercicio sea seguro.

El motivo está en V32.

## 4. `RESPALDO` en V32 (sin cambios)

Cuando la publicación pasa por el respaldo, el intento es `RESPALDO`, aunque la sesión final sea idéntica a la anterior. La evidencia ya separa las dos dimensiones:
- **procedimiento:** `status = RESPALDO`, `generation_source = CONSERVED`;
- **efecto:** la señalada en `no_effect_sessions`, `applied_sessions` vacío (V33), así que no cuenta como adaptación.

D8 conserva su exención si la sesión es idéntica y el linaje continuo (`ReduccionAutorizadaResolver`). No se requiere ningún cambio en V32 ni en V31–V34.

## 5. Activación y reversión de la progresión

- **Interruptor:** `fitsense.adaptation.progression.enabled` (por omisión `false`). Encendido, también activa la V24 por capacidades en la adaptación (`StrictValidationAdapter`).
- **Activar:** variable de entorno `FITSENSE_PROGRESSION_ENABLED=true` y reiniciar. No requiere migración: V34 ya está aplicada.
- **Revertir:** `FITSENSE_PROGRESSION_ENABLED=false` (o quitarla) y reiniciar. Los planes ya publicados no cambian. Las semanas siguientes vuelven a ser copias sin pasos (la evidencia V34 de los pasos dados se conserva).
- Hoy está **apagada** en todos los entornos. Activarla requiere aprobación explícita.

## 6. Pruebas

| Requisito | Prueba |
|---|---|
| 100 % dos semanas → evalúa y progresa, `ai_calls = 0` | `ProgresionCienPorCienIT.dosSemanasPerfectasProgresa` |
| 80–99,99 % → mantenimiento sin progresión; después hacen falta dos semanas nuevas al 100 % | `ProgresionCienPorCienIT.semanaParcialBloqueaDosSemanas` (97,22 %; con la regla anterior progresaba) |
| 100 % de lo registrado con una sesión sin registro → no progresa | `ProgresionCienPorCienIT.registroIncompletoNoProgresa` (con la regla anterior marcaba 100,00 % y progresaba) |
| < 80 % → adaptación, sin progresión en ese ciclo | `ProgresionCienPorCienIT.bajoOchentaAdaptaSinProgresar` |
| Una sesión que progresó espera dos semanas suyas | `ProgresionCienPorCienIT.dosSemanasPerfectasProgresa` (tercera semana) |
| Reejecutar el ciclo no duplica la progresión | `ProgresionCienPorCienIT.reejecucionNoDuplica` |
| 5 %, 10 %, techo 110 %, V24 incompatible → se mantiene | `ProgresionIT`, `ProgresionV24V8CTest`, `PendientesProgresionIT` (sin cambios, pasan) |
| Fatiga o dolor bloquean el paso | `ProgresionResolver.cumple` (sin cambios); `ProgresionIT` |
| Dolor entre varios motivos → seguridad | `SessionOutcomeIdentifierTest` (3 casos nuevos) |
| Dolor con alternativa → `APLICADA`; sin ella → `NO_FACTIBLE` con motivo; sin más volumen | `DolorSinMasTrabajoV8CTest`, `CorreccionesReplicate*IT.c5`, `HerenciaD8IT.dolorNoAprovechaD8` |
| Una semana sin sesiones no cuenta | `dosSemanasPerfectas` rechaza una semana vacía, en las dos semanas |

## 7. Progresión antes y después (80 % → 100 %)

| Escenario (ciclo completo, `ProgresionCienPorCienIT`) | Regla anterior (80 %) | Regla nueva (100 % real) |
|---|---|---|
| Dos semanas completas | Progresa | Progresa |
| Una semana al 97,22 % (un ejercicio con una serie menos) | **Progresa** | Mantiene; después hacen falta dos semanas nuevas al 100 % |
| Una sesión sin registrar (la adherencia sale «100,00 %» sobre lo registrado) | **Progresa** | Mantiene |
| < 80 % (sesión omitida) | Adapta, sin paso | Adapta, sin paso |
| La semana siguiente a un paso, en la misma sesión | No progresa (ya era por sesión) | No progresa |

**Simulación de 10 semanas (`EvaluacionLimpiaProgresionIT`, 72 perfiles): informe idéntico al anterior** (312 progresiones, 14 recuperaciones, 0 reincorporaciones, 720/720 publicadas). Es lo esperado: su guion solo tiene semanas completas al 100 % o con una sesión omitida (66,7 %), y la regla nueva solo cambia el resultado entre el 80 % y el 99,99 % o con registros incompletos. La semana con omisión adapta y la siguiente no puede progresar en ninguna de las dos reglas, porque la transición anterior no es una copia. La diferencia queda demostrada en la tabla de arriba, con el ciclo completo.

## 8. Neon: verificación del bloqueo — **verificada el 27/10/2026 (23:06, hora local)**

Desde el entorno de desarrollo **no se puede**: falta `DB_PASSWORD`, y el entorno no alcanza Neon en absoluto (solo permite HTTPS saliente; los nombres `*.neon.tech` ni siquiera se resuelven).

Configuración comprobada en el código:
- `spring.datasource.url` usa el **pooler** (`…-pooler.c-6.us-east-2.aws.neon.tech`), que es lo correcto para el pool de Hikari.
- `CicloSemanalBloqueo` deriva el endpoint **directo** quitando `-pooler.`, salvo que se configure `fitsense.cycle.lock.url`. Abre su propia conexión con `DriverManager`, fuera del pool, con `ApplicationName=fitsense-ciclo-semanal-bloqueo`.

Para verificarlo contra Neon sin escribir datos, desde una máquina con acceso:

```
DB_PASSWORD=… mvn -o verify -Pintegration -Dtest=NoUnit -Dsurefire.failIfNoSpecifiedTests=false \
    -Dit.test=NeonBloqueoDirectoIT -Dfitsense.neon.verificar=true
```

`NeonBloqueoDirectoIT` no levanta Spring (no corre Flyway), abre las conexiones de comprobación en solo lectura y usa la clave de un participante inexistente. Comprueba:
1. que el bloqueo va al endpoint sin `-pooler` y conecta;
2. que el bloqueo se ve tomado desde otra conexión;
3. que una segunda ejecución del mismo participante recibe `EN_CURSO`;
4. que sigue tomado tras varias transacciones;
5. que se libera al terminar;
6. que la conexión directa es una sesión estable (mismo `pg_backend_pid`).

Solo imprime los nombres de host, nunca la contraseña. La pérdida de conexión durante el ciclo la cubre la guarda de publicación (`CicloConcurrenteIT`, 9/9).

**Resultado** (`NeonBloqueoDirectoIT`, ejecutado por el responsable del proyecto desde su equipo con acceso a Neon; `BUILD SUCCESS`, 1/1):

```
NEON host configurado: ep-dark-brook-b4emftmn-pooler.c-6.us-east-2.aws.neon.tech
NEON host del bloqueo: ep-dark-brook-b4emftmn.c-6.us-east-2.aws.neon.tech
CICLO_BLOQUEO usuario 4294967295: otro ciclo ya esta en curso; no se ejecuta este.
NEON durante el bloqueo: tomados=1 segunda=EN_CURSO sigue=1 · resultado=EJECUTADO
NEON backend estable en la conexion directa: true
```

- El bloqueo va al endpoint **directo**: el host sin `-pooler`, derivado del configurado.
- Desde otra conexión se ve **tomado**.
- Una **segunda ejecución** del mismo participante recibe `EN_CURSO` y no se ejecuta.
- **Sigue tomado** tras varias transacciones.
- Se **libera** al terminar.
- La conexión directa es una **sesión estable**: el mismo `pg_backend_pid`, así que no pasa por el pool transaccional.
- No se escribieron datos.

