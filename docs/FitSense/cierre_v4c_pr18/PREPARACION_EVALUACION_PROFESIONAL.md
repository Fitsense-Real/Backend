# Preparación de la evaluación profesional (material del PR #17)

**Alcance:** guía **interna**, para el equipo, sobre cómo entregar el paquete y cómo analizar las respuestas.

**No se entrega al evaluador.** Este documento cita la clave interna y no debe llegar a sus manos.

**Estado:**
- **ninguna evaluación recibida ni registrada**;
- C2-D + EF **no** está validado profesionalmente.

## 1. El paquete está completo (comprobado)

Rama `feature/c2d-v4c-versionado`, carpeta `docs/FitSense/revision_profesional/paquete_evaluador/`:

| Archivo | Contenido | Comprobación |
|---|---|---|
| `MUESTRA_PROFESIONAL_CIEGA.csv` | 34 planes, 681 filas de ejercicio | 23 columnas en todas las filas |
| `FICHAS_NO_FACTIBLE.csv` | D01 y D02 | 10 columnas en todas las filas |
| `PLANTILLA_RESPUESTAS_EVALUADOR.csv` | 36 filas, una por código | 24 columnas; **vacía** salvo el código |
| `INSTRUCCIONES_EVALUADOR.md` | Escala, criterios c1–c12, pregunta de duración, e1–e7, fichas, G1–G5 y registro | Sin nombres de política ni umbrales internos |

**Ceguera:**
- **la clave no está en el paquete**: `CLAVE_INTERNA_MUESTRA.csv` sigue en `cierre_final_pr17/`;
- `PaqueteEvaluadorTest` comprueba la estructura, los 36 códigos y que no aparece ningún término revelador.

## 2. Cómo entregarlo

1. Elegir **uno o dos** profesionales del ejercicio (titulación en ciencias del deporte o equivalente, con experiencia en prescripción de fuerza). Que no hayan participado en el diseño.
2. Enviar **solo** los 4 archivos de `paquete_evaluador/`. Nada de `cierre_final_pr17/`.
3. Cada evaluador trabaja **por separado** y sin comunicarse con el otro hasta entregar.
4. Cada uno devuelve su propia copia de la plantilla. **No se edita la muestra.**
5. Antes de abrir la clave, guardar las plantillas recibidas tal cual, con su fecha.

## 3. Qué evalúa el profesional

| Bloque | Qué es |
|---|---|
| **Cada plan** (P..) | 11 criterios de 1 a 5 o NS (estructura, series y repeticiones, descansos, selección, distribución, recuperación, volumen, intensidad, seguridad, duración y nivel), más c12 de observaciones |
| Pregunta de duración | «¿La sesión es suficientemente completa aunque dure menos que el tiempo configurado?» |
| Preguntas específicas | e1–e7, según el caso |
| **Fichas D01–D02** | ¿Es pertinente no generar la semana? ¿Qué sesión propondría? ¿Qué material mínimo? |
| **Globales** G1–G5 | Una vez, al final |

**Cómo registrar:**
- una fila por código;
- números del 1 al 5 o `NS`;
- `Sí`, `No`, `Con cambios` o `NS` en las preguntas;
- texto libre en `observaciones`;
- código del evaluador y fecha en cada fila.

## 4. Casos críticos (para el análisis interno; el evaluador no lo sabe)

Clasificados por el motivo de selección de la clave interna (36 códigos):

| Grupo | Códigos | Por qué es crítico | Qué preguntas pesan |
|---|---|---|---|
| PULL sin tracción con peso corporal (publicado por C2-D sin EF) | 5 | Es el defecto que EF corrige | e2, e3 y G1–G3 |
| Fichas NO_FACTIBLE | 2 (D01, D02) | Pertinencia de no generar | Las 3 preguntas de la ficha |
| C2-D: sesión corta por saturación | 4 | Si se acepta publicar bajo el 70 % de M | Pregunta de duración, e5 y c10 |
| EF antes y después | 4 + 4 | Clasificación funcional de patrones | e1, e2 y G1–G2 |
| LOSE_WEIGHT con brecha de fuerza | 3 + 3 + 3 | Grupos grandes sin fuerza (LW-PREF-0.2) | e7 y G4 |
| Ciclo tras dolor (P-03 / P-02) | 2 | Sesión razonable sin los ejercicios excluidos | e6 |
| Control sin anomalía | 4 | Referencia: deberían puntuar bien | c1–c11 |
| Otros del ciclo real | 2 | Contexto longitudinal | c1–c11 |

**Un caso es crítico para la seguridad** si algún evaluador puntúa **c9 (Seguridad) ≤ 2**. Se revisa uno a uno, sea cual sea el resto de la puntuación.

## 5. Cómo se analizarán las discrepancias

- **Por criterio:** diferencia entre evaluadores.
  - ≤ 1 punto: acuerdo;
  - > 1 punto: discrepancia.
- **Se informa:**
  - el porcentaje de acuerdo exacto;
  - el porcentaje de acuerdo en ±1;
  - las discrepancias por grupo crítico.
- **Discrepancias:** se resuelven en una reunión con los dos evaluadores, registrando el motivo. Nunca promediando sin más ni eligiendo la respuesta favorable.
- **Con un solo evaluador:** se informa como opinión única, sin cifras de acuerdo, y se señala como limitación.
- **NS** no cuenta como puntuación.

## 6. Criterios de aceptación (hay que acordarlos **antes** de recibir las evaluaciones)

El instrumento interno (`cierre_final_pr17/INSTRUMENTO_PROFESIONAL_FINAL.md` §7) los **propone**. **No están aprobados.**

| # | Criterio propuesto | Decisión pendiente |
|---|---|---|
| A1 | Plan aceptable: media ≥ 3 en c1–c11, ningún c9 ≤ 2, «Sí» o «Con cambios» en la pregunta de duración y ningún «No» en e2 ni e3 | ¿Se aprueba tal cual? |
| A2 | C2-D + EF aceptable: ≥ 80 % de los planes con sesión corta publicada por C2-D aceptables y ninguno con c9 ≤ 2 | ¿80 %? Con solo 4 planes, un rechazo deja un 75 %: ¿basta un rechazo para no aceptar? |
| A3 | Acuerdo entre evaluadores: ±1 por criterio | ¿Se exige un porcentaje mínimo de acuerdo? |
| A4 | Fichas D01/D02: ¿qué respuesta mantiene NO_FACTIBLE? | Por ejemplo: «Sí, es pertinente» de todos los evaluadores |
| A5 | G3 (un único ejercicio de tracción): ¿qué respuesta permite mantener el punto de anclaje en la lista de equipamiento? | — |
| A6 | ¿Cuántos evaluadores hacen falta (1 o 2) y qué se hace si no hay consenso? | — |

**Reglas:**
- Dejar estas decisiones **por escrito y fechadas antes de enviar el paquete**.
- **No** ajustar los umbrales después de ver los resultados.

## 7. Lo que depende del resultado

| Componente | Preguntas que lo deciden |
|---|---|
| C2-D | Duración, e5, c10 y G4 |
| EF-0.1 | e1–e3, G1 y G2 |
| Punto de anclaje en la lista de equipamiento | G3 |
| LW-PREF-0.2 | e7 y G4 |
| REP-SUST-0.1 | G5 |
| NO_FACTIBLE de peso corporal | D01 y D02 |

**V4-C (PR #18) no depende de la evaluación profesional.**

## 8. Flutter (referencia, sin cambios)

`preparacion_integracion_pr17/ESPECIFICACION_FLUTTER.md` sigue siendo la referencia. El siguiente trabajo móvil deberá contemplar:
- la duración estimada y el tiempo disponible;
- las sesiones acortadas;
- el diagnóstico NO_FACTIBLE;
- la revisión del equipamiento;
- el manejo de los campos opcionales de la API.

**No se ha modificado Flutter.**
