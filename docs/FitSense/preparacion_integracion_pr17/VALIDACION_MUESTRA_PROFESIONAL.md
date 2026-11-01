# Validación de la muestra profesional completa

**Alcance:** el material que se entregará al evaluador (`revision_profesional/paquete_evaluador/`) y la clave interna (`cierre_final_pr17/CLAVE_INTERNA_MUESTRA.csv`), que no se entrega.

**Método:** `PaqueteEvaluadorTest` (automática) y un recuento independiente con el lector CSV estándar de Python.

## 1. Recuentos

| Elemento | Esperado | Obtenido |
|---|---|---|
| Planes completos (P..) | 34 | **34** |
| Fichas NO_FACTIBLE (D..) | 2 | **2** (D01, D02) |
| Códigos únicos en total | 36 | **36** |
| Filas de la plantilla | 36 | **36**, sin duplicados ni huérfanos |
| Filas de la clave interna | 36 | **36**, sin duplicados ni huérfanos |
| Filas de ejercicio en la muestra | — | **681**: 589 SETS_REPS y 92 DURATION |
| Campos vacíos obligatorios | 0 | **0** |

**Sesiones por plan:**

| Sesiones | Planes |
|---|---|
| 1 | 14 |
| 2 | 2 |
| 4 | 7 |
| 6 | 11 |
| **Total** | **34** |

## 2. Integridad por ejercicio

Cada una de las 681 filas tiene:
- código, objetivo, nivel, días, minutos, equipamiento y restricciones del perfil;
- número de sesión, día relativo, enfoque y duración estimada;
- orden, nombre, descripción, patrón, músculo principal y secundarios;
- tipo de prescripción, series y dosis: repeticiones (SETS_REPS) o segundos (DURATION);
- descanso.

Ninguna dosis vacía ni `null`.

## 3. Ceguera

| Comprobación | Resultado |
|---|---|
| La clave está separada | Sí: no hay ningún archivo con «CLAVE» en el paquete |
| Los CSV del paquete no revelan la política, el estado ni el motivo | Sí: 0 apariciones de `PLAN-`, `C2D`, `C2-D`, `EF-0`, `LW-PREF`, `REP-SUST`, `NOFACT`, `NO_FACTIBLE`, `VALIDO`, `INVALIDO`, `SATURAD`, `POLITICA`, `MOTIVO_SELECCION`, `CASO_EXTREMO`, `CONTROL_SIN`, `EF_ANTES` ni `EF_DESPUES` |
| Las instrucciones no revelan la política | Sí |
| La correspondencia código → política solo está en la clave | Sí: la muestra y la plantilla solo llevan el código |

## 4. Estado de la revisión profesional

- **No iniciada.** La plantilla está vacía (0 respuestas).
- **No se declara completada** ninguna validación profesional.
- **No se ha registrado** ninguna evaluación ficticia.

**Para empezar** hace falta:
1. un evaluador cualificado;
2. que reciba **solo** la carpeta `paquete_evaluador/`;
3. que se registren sus respuestas en la plantilla, sin editar la muestra.
