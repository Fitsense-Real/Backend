# Hallazgos y correcciones · campaña MVP de 8 usuarios × 8 semanas

**Fecha:** 31/10/2026. **Rama:** `campana-8-usuarios`.

Estado de cada hallazgo:
- **CORREGIDO Y PROBADO**;
- **VERIFICADO**: comportamiento correcto demostrado;
- **PENDIENTE DE DECISIÓN**;
- **NO REPRODUCIBLE**.

**Resumen:** no se encontró ningún defecto de código del backend. El único usuario sin plan se debe a un conflicto entre políticas aprobadas, que no se puede corregir sin autorización.

## H1 · GAIN_MUSCLE de 5–6 días a 60 min no tiene plan — PENDIENTE DE DECISIÓN

**Reproducción:**
- U06 quedó sin plan inicial (IA: 1 propuesta rechazada y un timeout; reglas: V15/V20).
- `RespaldoSesentaMinutosIT` (sin IA, con `-Dfitsense.pendientes=true`) lo reproduce en **las 24** combinaciones de GAIN_MUSCLE con 5 y 6 días de 60 min.

**Causa raíz:** las políticas son incompatibles entre sí (`SIMULACION_MVP_8_USUARIOS_8_SEMANAS.md` §5):
- 4D v3.1 fija la dosis, sin ampliación, y un tope de 7 colocaciones.
- Con eso, un día LEGS o PULL que cumpla V15 dura como mucho 2448 s, y un LOWER_BODY con la mitad de V15, 2142 s.
- V20 exige 2520 s.
- No es un error del motor de reglas ni de la IA. Con 45 min el problema no aparece (barridos anteriores).

**Opciones**, todas cambian una política aprobada y requieren autorización:

| Opción | Cambio | Efecto |
|---|---|---|
| A | 8 colocaciones a partir de 60 min (ampliar la adenda 4D v3.1) | Una más para GAIN_MUSCLE |
| B | Permitir 3 series en el resistido de zona pequeña de GAIN_MUSCLE | 2×13 pasaría a 3×13 |
| C | Aplicar a GAIN_MUSCLE la tolerancia de V20 que ya tienen otros casos | — |
| D | Limitar GAIN_MUSCLE a 4 sesiones con 60 min | Lo hace el divisor de días, no la composición |

**Recomendación técnica:** A, que sigue el precedente de la adenda 4D v3.1 (6 → 7 por el mismo motivo). Antes de decidir habría que verificar con el mismo barrido que 8 colocaciones bastan para LOWER_BODY (4 × 364 + 4 × 262 = 2504 s: **todavía 16 s corto**). B sí cierra los dos casos.

**Pruebas añadidas:**
- `RespaldoSesentaMinutosIT.barridoSesentaMinutos`: regresión en CI de los 72 casos de 60 min de los otros tres objetivos, todos en verde. Antes no había cobertura a 60 min con 5–6 días.
- `usuarioU06` y `gainMuscleSesentaMinutos`: reproducción, opt-in.

## H2 · La IA intenta subir dosis — VERIFICADO (la defensa funciona)

10 subidas en 4 ciclos (U02 S5, U04 S7, U05 S3 y U08 S5). Otros casos:
- dos veces se subió el volumen ante dolor;
- se cambió la carga junto con la dosis.

V8C las rechazó todas. Ninguna llegó a publicarse: la comprobación independiente de subidas da 0 violaciones.

## H3 · Fallos frecuentes de la IA — VERIFICADO (los validadores los frenan)

En los 24 intentos fallidos:

| Regla | Qué falló | Intentos |
|---|---|---:|
| V24 | dosis sin capacidad | 12 |
| V17 | minutos declarados distintos del contenido | 9 |
| V20 / V15 | duración y grupos | 5 cada uno |

**Resultados:**
- 3 de 8 generaciones iniciales agotaron los 3 intentos y se publicó el plan de reglas, válido.
- 3 de las 13 adaptaciones con IA terminaron en RESPALDO, con la sesión original publicada.

**No se corrigió nada:**
- son decisiones de la IA, no defectos del backend;
- el reintento con la lista de problemas ya existe.

**Mejora posible**, sin decidir: dar a la IA en el prompt el cálculo de minutos que usa V17. Es el rechazo más repetido después de V24.

## H4 · Acortar con poco tiempo no siempre es posible — VERIFICADO / observación

| Caso | Resultado | Qué pasó |
|---|---|---|
| U01 S2 (M = 70 % de la sesión) | SIN_EFECTO | La IA no propuso una versión válida más corta |
| U04 S5 (sesión única, M ≈ 16 min) | RESPALDO | Con esos minutos no cabe una sesión válida (V9, V17) |
| U08 S7 | APLICADA | Sin superar los minutos declarados |

En los tres casos la persona recibe una sesión que respeta las reglas; en los dos primeros, más larga que su tiempo declarado. ADAPT-2 lo prevé: el techo M nunca se supera en una sesión acortada.

## H5 · Adherencia: fórmula de la orden y fórmula oficial — VERIFICADO

- Se calcularon las dos en las 56 semanas con plan.
- Difieren en 8, por sesiones parciales que no quedan al 50 % y por sesiones completadas con un ejercicio dejado por dolor.
- Se mantiene la oficial (`ADHERENCIA.md`).
- Sin registro (U04 S4) → INSUFFICIENT_DATA, distinto de una omisión.

## H6 · Cambios de perfil del guion — VERIFICADO

Los 4 intentos (U03 S5 y S6, U05 S5, U07 S6) recibieron un **422** por el contrato de la app y el perfil quedó intacto. Es la regla del estudio aprobada el 30/10/2026 (`PerfilCongeladoIT`).

## H7 · Validación independiente sin exenciones — VERIFICADO (no es defecto)

**Qué marcó:** 29 semanas, en V24, V25 y una vez V20, por tres causas autorizadas:
- dosis de progresión autorizada (+1 repetición, D-P3);
- reducciones autorizadas por REVISAR e INVESTIGAR, con herencia D8;
- una sesión acortada heredada (U08 S8).

**Revisión:** caso por caso. Ninguna publicación sale de lo que autorizan las políticas.

**Observación** (U04 S8): una reducción por desmotivación deja la única dosis de cardio de un perfil de 1 sesión semanal por debajo de la referencia.

## H8 · Reincorporación no alcanzada en la campaña — VERIFICADO en prueba focalizada

- No hubo un relleno retirado por ACORTAR seguido de un paso de progresión dentro de las 8 semanas.
- `CopiaSinIaIT.reincorporacionYMantenimiento` lo demuestra con el backend real: 1324 vuelve con su dosis inicial exacta, bajo el techo, después de la progresión y en un ciclo propio.
- Con IA real queda **sin validar**.

## H9 · Etiqueta del simulador — CORREGIDO

- **Fallo:** `CampanaMvp8UsuariosIT` escribía «ver semanas» en el plan inicial de un usuario sin plan.
- **Corrección:** ahora escribe SIN_PLAN. Se corrigió también la fila de U06 en los CSV de esta ejecución.
- **Alcance:** no afecta a ningún dato del backend.

## H10 · Usuarios 31, 32 y 33 (orden anterior) — NO REPRODUCIBLE

Sus registros originales siguen sin estar en el repositorio. Esta campaña no los sustituye.

## Pruebas ejecutadas

| Prueba | Resultado |
|---|---|
| `CampanaMvp8UsuariosIT`, ensayo seco | Simulador verificado |
| `CampanaMvp8UsuariosIT`, IA real, una sola vez | 45 llamadas, ≈ US$ 0,87; 0 violaciones de linaje; 0 violaciones de las comprobaciones independientes |
| `RespaldoSesentaMinutosIT` | 72/72 en CI; 24/24 de GAIN_MUSCLE fallan en la reproducción opt-in |
| `CopiaSinIaIT` y `PendientesProgresionIT` | 11/11 |
| Suite completa | GitHub Actions, en el PR |
