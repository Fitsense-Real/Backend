# Informe final: verificación de seguridad, adaptación y cierre del catálogo (orden de 14/11/2026)

**No se ha hecho ningún merge ni despliegue.** Tampoco se ha modificado `generation`, tocado datos reales, gastado en Replicate, cambiado Flutter ni activado V53, V53-B o la propuesta #64. No se ha relajado V15, V20, V24, V8C, D8 ni P-03, y no se ha inventado ninguna validación profesional.

## Fuentes

| Documento | Contenido |
|---|---|
| `../../HALL_GM_01.md` (en #65) | Defecto de producción encontrado, corrección, medición y pruebas |
| `VERIFICACION_FASES_2_6.md` | Garantías ante dolor, decisiones de seguridad, 775, plan 78, V54 y fichas |
| `CLASIFICACION_SEGURIDAD.tsv` | Las 59 filas clasificadas (script `../herramientas/clasificar_seguridad.py`) |
| `../v53b/PROPUESTA_V53B.md` (en #66) | Opción B de V53 |
| `../../REGRESIONES_V52_1.md` | Orden anterior; sigue vigente |

## Ramas y PR (todas en borrador, ninguna fusionada)

| PR | Rama | Base | Contenido | CI |
|---|---|---|---|---|
| #63 | `cierre/pendientes-seguridad` | — | + `9dbbb65`: comparación por colocación (solo pruebas). + `e96ca13`: verificación final (docs) | Verde en `9dbbb65` |
| **#65** (nueva) | `fix/sustituto-gm-sin-mas-trabajo` | #63 | **HALL-GM-01**: corrección de producción (+20 líneas), 4 unitarias, 1 de integración, docs y evidencia | Verde en `55129f4` |
| **#66** (nueva) | `propuesta/v53b-permiso-apoyo-suspendido` | #61 | V53-B: permiso propio, V55, interruptor apagado | Verde en `fce4fdc` |
| #64 | `propuesta/reparacion-dificultad-publicada` | — | Plan 78 + V54 (sin cambios en esta orden) | Verde en la orden anterior |
| #61 | `cierre/v53-apoyo-suspendido` | — | Sin cambios en esta orden | Verde |

## Las 20 respuestas

**1. ¿Se compara ahora la dosis por colocación?** Sí. `ComparadorDosisPorColocacion` sustituye al máximo semanal en `HerenciaD8IT` y se usa en `RegeneracionPorColocacionRev2IT`.
- La correspondencia es explícita:
  - mismo día y ejercicio;
  - puesto del retirado (sustitución);
  - sin correspondencia, se compara con la menor dosis medida;
  - nueva.
- Compara series, repeticiones, segundos y volumen. El trabajo total se compara aparte.
- 12 pruebas unitarias, entre ellas el ejemplo de la orden (lunes 3x10 → 3x12 con el máximo semanal bajando de 15 a 13).
- También cubre: el ejercicio repetido, el cambio de día, el mismo ejercicio en dos sesiones, la sustitución por dolor, y los casos de mantener, reducir e intentar aumentar.

**2. ¿El problema estaba solo en la prueba o era un defecto real?** En los dos sitios:
- La prueba usaba el máximo semanal; ya está reforzada.
- Al recorrer el caso más exigente (PLAN-REV-2.0, mantenimiento por fatiga autorizado, copia y después dolor) apareció un **defecto real de producción, HALL-GM-01**. En GAIN_MUSCLE, el sustituto de seguridad recibía la dosis base (3x10) sobre una colocación a mantenimiento (2x10). Así se publicaba más trabajo ante dolor y se deshacía una reducción autorizada.
- Afectaba a 4 de 40 participantes deterministas (9703: 442 → 452).

**3. ¿Cómo se ha tratado?**
- Aislado: es una sola regla de `SafetySubstitutionPolicy.dosis`.
- Documentado: `HALL_GM_01.md`.
- Corrección independiente en #65, sin fusionar. La política no se ha modificado en silencio.
- Con la corrección, el sustituto nunca supera el trabajo del quitado. V24 rechaza esa dosis, porque el sustituto no tiene autorización propia, y el desenlace es **NO_FACTIBLE**. **Solo cambian los 4 casos del defecto**; los otros 36 quedan idénticos.
- Las pruebas nuevas fallan sin la corrección.
- No se relaja V24, V8C, D8 ni P-03.

**4. ¿Se mantienen las garantías ante dolor?** Sí. Cada garantía tiene su caso positivo, su caso negativo y la prueba que lo exige (`VERIFICACION_FASES_2_6.md`, fase 2):
- lo que dolió no reaparece;
- no se añade trabajo, ni en total ni por colocación;
- la IA no publica nada inválido;
- la regeneración solo publica semanas `VALIDA` con dosis base;
- no hay restauración, progresión ni reincorporación indebidas;
- se respetan las restricciones del usuario;
- si no hay alternativa segura, NO_FACTIBLE.

**5. ¿Puede la IA publicar una adaptación inválida?** No.
- En 9004 y 9703 su intento queda NO_FACTIBLE.
- En 9703, V24 rechaza tres veces la propuesta y V32 lo registra.
- Solo publica el motor determinista, y solo una semana válida.

**6. ¿Qué queda sin cubrir?** Un **riesgo residual documentado**: P-03 nivel 2 compara el trabajo total y no por colocación.
- No se ha observado ninguna restauración por esa vía.
- Endurecerlo es un cambio de política de P-03. Queda como propuesta para que decida el propietario.

**7. Las decisiones de seguridad abiertas, por categoría** (59 filas: las 43 de prioridad 1 y 16 casos del expediente):

| Categoría | Filas |
|---|---|
| Restricción conservadora ya aplicada | 41 |
| Del propietario (A-V53, 1153, 1301, 1303, 1304, 1306) | 6 |
| Error técnico ya corregido (1323) | 1 |
| Elegible con incertidumbre material | 11 |

Todas menos A-V53 necesitan además a un profesional.

**8. ¿Qué ejercicios elegibles tienen incertidumbre material?**
- Los **10 de apoyo suspendido** (457, 458, 459, 462, 777, 920, 934, 1184, 1317 y 1335), elegibles para «no colgarse».
- **1113**: dificultad 1 vigente, cuando R2 y V3 ven nivel 2. Es elegible para principiantes.
- No se han contenido. Contener los 10 equivale a activar V53, y la orden lo prohíbe.
- No se ha cambiado la dificultad del 1113 sin decisión profesional.

**9. Prioridades de la orden:**
- 1083–1087, 854 (D2) y 868: contenidos por bandera.
- 1294 y el resto de INC-4C-03: retenidos. 813, 858, 861, 873 y 885 cumplen el criterio de salida, pero **no se liberan**.
- INC-CAT-C39: vigente.
- 1323: corregido.
- 1113 y los 10: abiertos (respuesta 8).
- **No se ha liberado nada.**

**10. ¿Está preparada la opción B de V53?** Sí, en #66, con el interruptor `fitsense.seguridad.permiso-apoyo-suspendido=false`.
- Es excluyente con la opción A.
- V55 solo añade esquema: dos columnas `NULL`, sin backfill.
- Sin API ni Flutter.
- **Usuarios nuevos y permisos guardados:** sin responder = heredan «no colgarse», que es lo conservador. No se reinterpreta nada en silencio.
- **Confirmación explícita:** `confirmarApoyoSuspendido`.
- **Generación y adaptación:** las decide el conjunto elegible. V24 y V8C sin cambios.
- **Planes publicados:** causa `apoyo_suspendido`.

**11. ¿Qué mide V53-B (rejilla de 168, datos sintéticos)?**

| Modo | Con plan | Perfiles con elegibles distintos |
|---|---|---|
| Apagado | 168/168 | 0 (idéntico a hoy) |
| A, o B sin responder | 168/168 | 16; salen los 10 |
| B confirmado que sí | 168/168 | 0 |
| B confirmado que no | 168/168 | 56 |

El alcance en los perfiles guardados lo mide el propietario con `v53b/impacto_permisos_v53b_solo_lectura.sql`, que es de solo lectura.

**12. ¿Tiene el 775 alguna alternativa segura?** **No**, sin cambiar de equipo.
- Consulta independiente: ningún ejercicio de tracción sin equipo en los 1.219 activos.
- V20 rechaza correctamente: 14 NO_FACTIBLE en `Hipertrofia4Dv34IT`.
- La duda sobre la clasificación del 775 ya está registrada y no crea una factibilidad falsa.
- V20 y V25 sin tocar.
- **Explicación al usuario:** se propone un texto. El aviso actual («Tu plan trabaja empuje, piernas y core») no es exacto para estos perfiles, que no reciben plan.

**13. ¿Cumple el plan 78 (#64) cada requisito?** Sí, contra el diff y `Plan78ContencionReducidaIT`:
- solo ejercicios ya publicados, sin sustitutos;
- misma seguridad, solo se levanta el tope de dificultad;
- sin más dosis;
- semana empezada intacta;
- una sola sustitución;
- la excepción se limita a V6/V1 de los tolerados;
- comportamiento comprobado con el interruptor apagado (NO_VALIDA) y encendido (REPARADA).

**No activado.** Hay una observación menor: un comentario dice «misma sesión» donde el código usa «misma semana», sin efecto en el resultado.

**14. ¿Cumple V54 sus requisitos?** Sí:
- la causa es real (la misma fuente que bloquea) y trazable (`system_block_cause`);
- `skip_reason` sigue NULL, así que no se presenta como omisión declarada;
- sin cambios en la fórmula de adherencia, el historial ni los umbrales.

La propuesta metodológica (S3a/S3b) sigue separada y sin aplicar.

**15. ¿Están completas las fichas profesionales?** Sí:
- cubren 312 de 312 decisiones pendientes y 43 de 43 de seguridad;
- todos los campos de evidencia están completos;
- los de decisión (decisión, valor, responsable, credenciales, fecha, observaciones) están presentes y **vacíos**.

El revisor necesita el conjunto de datos de medios (GIF).

**16. ¿Cuántas validaciones profesionales hay?** **Ninguna.** Ninguna de las 312 decisiones pendientes, ni ninguna de las 43 de seguridad, tiene validación profesional. Una revisión hecha por la IA no cuenta como tal.

**17. ¿Qué da la regresión completa?**

| Rama | Unitarias | Integración |
|---|---|---|
| #65 (`55129f4`) | 1.484 / 0 fallos | 730 / 0 fallos / 0 errores / 53 omitidas (opt-in) |
| Integración (`39d6cbe` = #62 + #61 + #63 + #65) | 1.484 / 0 fallos | **735 / 0 fallos / 0 errores** / 53 omitidas |
| Integración + #64 + #66 (`239c542`, todo apagado) | 1.516 / 0 fallos | En curso al escribir este commit (se actualiza al terminar) |
| #66 (agente, en local) | 1.499 / 0 fallos | 79 seleccionadas / 0 fallos. Además CI en verde |

Incluye todas las pruebas que pide la orden:
- dolor y P-03: `HerenciaD8IT`, `RegeneracionPorColocacionRev2IT`, `AdaptacionRegresionIT`;
- progresión: `ProgresionIT`, `ProgresionRev2IT`;
- O12, V24/V8C y HALL-PROG-01;
- retirada localizada, reincorporación y planes publicados (`Plan78ContencionReducidaIT` en la rama combinada);
- 72 referencias (`GeneracionReferenciaIT`);
- 312 de hipertrofia (298 válidos y 14 NO_FACTIBLE por V20);
- controles del catálogo.

**Rejilla de 168:** 168 de 168 con plan, con V53 apagado y encendido. Los dos CSV son **idénticos byte a byte** a la medición anterior: la corrección no cambia la generación inicial.

**18. ¿Se aplican las migraciones desde una base nueva?** Sí. Cada contexto de prueba de integración arranca un PostgreSQL 16 vacío y aplica las migraciones; en la rama combinada, V1–V55 con V52_1.
- `V43 · migración sobre un esquema histórico (V42) y reinicio` y `V44 · versión de política sobre un esquema histórico (V43) y reinicio` pasan.
- V54 y V55 están registradas en `SOLO_ESQUEMA` para las pruebas que fijan un catálogo anterior.

**19. ¿En qué orden integrar, y qué conflictos hay?**
1. #62 → `generation`.
2. #56 → #57 → #58 → #59 → #55.
3. #63 (con #60).
4. **#65**, si se aprueba.
5. #61.
6. #64 y #66, solo si se aprueban.

Conflicto conocido: #64 y #66 tocan la misma lista `SOLO_ESQUEMA` (V54 y V55). Se resuelve conservando las dos líneas; está hecho así en la rama combinada.

**20. ¿Cumple el conjunto los criterios para aprobar la fusión?** **No.** Ver el veredicto.

## Veredicto

# NO APTO PARA MERGE

**Qué falta, exactamente:**
1. **Decisión sobre HALL-GM-01 (#65).** Es un defecto real de producción. Sin la corrección, la versión por defecto puede publicar más trabajo ante dolor. Hay que aprobarla y fusionarla antes que el resto, o decidir otra cosa.
2. **Validación profesional** de las 43 decisiones de seguridad, con nombre, función y fecha en `FICHAS_PROFESIONALES.xlsx`. Prioridad:
   - A-V53 y los 10 de apoyo suspendido;
   - 1113;
   - la liberación de 813, 858, 861, 873 y 885.

   Después, las 312 decisiones restantes.
3. **Decisión del propietario sobre V53:** opción A o B (#66) y tratamiento de los permisos guardados, después de medir el alcance con el script de solo lectura.
4. **Aprobación o rechazo de #64** (plan 78 y V54) y de las propuestas metodológicas S2, S3a y S3b.
5. **Decisión sobre el riesgo residual de P-03 nivel 2**: comparar también por colocación o aceptar el total.
6. **Aceptar la consecuencia del 775** (14 perfiles NO_FACTIBLE) y decidir el texto con que se explica al usuario.

**El catálogo no se declara cerrado:** siguen sin aprobación profesional decisiones de seguridad indispensables.
