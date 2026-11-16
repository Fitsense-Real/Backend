# Diagnóstico: `ReparacionLinajeIT` y `ReparacionLinajeRev2IT`

Fecha: 16/11/2026 (lunes). Ramas revisadas:

| Rama | Cabeza |
|---|---|
| `generation` | `2578719` |
| #71 | `12a01c6` |
| #72 | `8658089` |

## 1. Síntoma

Desde el 16/11/2026 fallan las dos pruebas, en local y en GitHub Actions, en las tres ramas y también en
`generation` sin #71 ni #72. El 15/11 pasaban: CI de #71 y de #72 en verde, y regresión local 795/0.

```
ReparacionLinajeIT.repararYHeredar:103 -> repararYHeredarSinContencion:138
P-P2: la semana con la dosis progresada se puede reparar:
  Resultado[estado=NO_VALIDA, ... conflictos={44=INC-CAT-1374}, sustituciones=[],
  problemas=[V24: el ejercicio 104 del 2026-12-09 lleva 2x10 con 90 s de descanso y 4D v2 exige 2x13 ...]]
  expected <REPARADA> but was <NO_VALIDA>
```

`ReparacionLinajeRev2IT` hereda la misma prueba con `PLAN-REV-2.0`.

## 2. Qué hace la prueba

1. Genera la semana S1 y luego la ejecuta entera. El ciclo publica S1+1.
2. Ejecuta S1+1 entera. El ciclo publica S1+2 con un **paso de progresión**.
3. Convierte en el 1374 (inseguro sin barra) un relleno de **otra** sesión de S1+2: el primero por fecha y orden.
4. **P-P2.** Pide la reparación de seguridad de S1+2. Espera `REPARADA` con la dosis progresada intacta.
5. **H6.** Ejecuta la versión reparada y el ciclo publica S1+3. Espera que herede la dosis progresada.

`S1` venía de `TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1)`:

| Día de la ejecución | S1 |
|---|---|
| Domingo 15/11 | 16/11 |
| Lunes 16/11 | 23/11 |

## 3. Por qué la fecha cambia el resultado

El motor de reglas siembra con `userId × 1.000.003 + weekStartDate.toEpochDay()` (estable por usuario y semana, a
propósito). Al cambiar S1 cambia la composición de las semanas y, con ella, **qué relleno** convierte la prueba en el
1374.

La reparación sustituye el 1374 (TRUNK_FLEXION) por su sustituto seguro, el **104** (SUST-SEG-1). Es un relleno de
zona pequeña cuya dosis 4D v2 en HYPERTROPHY es **2x13/90**. El resultado depende de la dosis del relleno quitado:

- **Relleno de 2x13/90:** el 104 hereda 2x13. Cumple V24 y V8C. La reparación se publica y H6 se cumple.
- **Relleno de 2x10/90:** el 104 no puede bajar a 2x10 porque V24 exige su dosis exacta, ni subir a 2x13 porque V8C
  no deja aumentar el trabajo al sustituir. No hay ninguna semana válida: **NO_VALIDA, sin publicar nada**.

### Barrido (prueba de investigación temporal, no incorporada)

Mismo escenario y perfil, con un usuario por semana:

| S1 | Relleno quitado (id, dosis) | Resultado |
|---|---|---|
| 2026-11-16 | 1268, 2x13/90 | REPARADA, H6 sí |
| 2026-11-23 | 480, 2x10/90 | NO_VALIDA (V24 104 2x10) |
| 2026-11-30 | 784, 2x10/90 | NO_VALIDA |
| 2026-12-07 | 422, 2x10/90 | NO_VALIDA |
| 2026-12-14 | 416, 2x10/90 | NO_VALIDA |
| 2026-12-21 | 344, 2x10/90 | NO_VALIDA |
| 2026-12-28 (año nuevo en S1+1) | 907, 2x13/90 | REPARADA, H6 sí |
| 2027-01-04 | 545, 2x10/90 | NO_VALIDA |
| 2027-01-11 | 391, 2x10/90 | NO_VALIDA |
| 2027-01-18 | 1142, 2x13/90 | REPARADA, H6 sí |
| 2027-01-25 | 996, 2x10/90 | NO_VALIDA |
| 2027-02-01 | 497, 2x13/90 | REPARADA, H6 sí |
| 2027-03-01 | 139, 2x13/90 | REPARADA, H6 sí |
| 2027-03-29 | 1030, 2x13/90 | REPARADA, H6 sí |
| 2027-06-07 | 346, 2x10/90 | NO_VALIDA |
| 2027-12-27 | 864, 2x13/90 | REPARADA, H6 sí |

En total, 7 reparadas y 9 NO_VALIDA, y la diferencia es siempre la dosis del relleno quitado. En los 9 casos
NO_VALIDA:

- el número de planes no cambia (3 → 3);
- la semana publicada y su paso de progresión se conservan.

**Contraprueba.** Se repitió el barrido eligiendo siempre un relleno de 2x13/90: **16/16 REPARADA**, con la dosis
progresada intacta y H6 cumplido. Con la contención del catálogo **encendida**, que es la configuración real, también
salió REPARADA con H6 en las 5 semanas probadas.

Las demás variables no influyen: cambio de domingo a lunes, de mes y de año, zona horaria (la de la JVM; el usuario no
tiene ninguna configurada), las fechas de los registros y la recuperación de semanas anteriores. Su lógica ya la
cubre `TrainingWeekTest`: cada día de la semana, domingo y lunes, cambio de mes y de año, años que empiezan en lunes y
bisiestos.

## 4. Clasificación

**Caso A: prueba dependiente de la fecha.** La lógica es correcta.

- La ruta REPARADA (P-P2 y H6) funciona en todas las semanas donde existe un sustituto con la misma dosis.
- La ruta NO_VALIDA es el comportamiento conservador **ya aprobado y documentado** (`IMPACTO_FACTIBILIDAD.md` del
  cierre conservador del catálogo, R12): *«NO_VALIDA: el sustituto del 1374 (104, relleno de zona pequeña, 2x13) no
  puede recibir más que los 2x10 del quitado (V8C). No se publica nada.»*

La prueba tenía además una premisa equivocada. Apagaba la contención del catálogo para cambiar el sustituto, pero el
sustituto es el 104 con y sin contención. Lo que decidía el resultado era la semilla.

No hay ningún defecto funcional que corregir: no se cambia código de producción, ni validadores (V24, V8C), ni
SUST-SEG-1, ni progresión ni reincorporación.

**Limitación conocida, no nueva.** Cuando el único sustituto seguro necesita más dosis que el ejercicio quitado, la
semana se queda con el ejercicio inseguro. La sesión afectada queda bloqueada para iniciarla (`safetyBlocked`, con su
aviso) hasta el siguiente ciclo. Es la decisión conservadora aprobada: no subir el trabajo. Queda registrada como pendiente en
`docs/FitSense/validacion_integral/PENDIENTES_PRIORIZADOS.md` (rama `validacion/backend-integral`).

Corrección aplicada y resultados: `CORRECCION_CI.md`. Políticas protegidas: `REGRESION_PROGRESION.md`.
