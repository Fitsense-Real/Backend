# Decisiones para la evaluación profesional (A1–A6)

**Estado:**
- **Ningún paquete enviado y ninguna evaluación recibida ni registrada.** No se ha inventado ningún resultado.
- El paquete se conserva sin cambios en `docs/FitSense/revision_profesional/paquete_evaluador/`:

| Archivo | Contenido |
|---|---|
| `MUESTRA_PROFESIONAL_CIEGA.csv` | 34 planes completos |
| `FICHAS_NO_FACTIBLE.csv` | D01 y D02 |
| `PLANTILLA_RESPUESTAS_EVALUADOR.csv` | 36 códigos, vacía |
| `INSTRUCCIONES_EVALUADOR.md` | Escala y preguntas |

La clave **no** está en el paquete.

**Las propuestas completas** (con sus alternativas) siguen en `integracion_controlada_pr18_pr17/DECISIONES_PROFESIONALES_PENDIENTES.md`. Aquí van las **recomendaciones finales** para que puedas aprobarlas de una vez.

## Recomendaciones finales

| Criterio | Recomendación para aprobar | Motivo |
|---|---|---|
| **A1.** Plan aceptable | Media ≥ 3 en c1–c11; **ningún c9 (seguridad) ≤ 2**; duración «Sí» o «Con cambios»; ningún «No» en e2 (función) ni e3 (tracción real) | Pone la seguridad y la función por delante del promedio |
| **A2.** C2-D + EF aceptable | **Opción a:** «los 4 planes de sesión corta publicados por C2-D aceptables y ningún c9 ≤ 2», sin porcentaje. Si C2-D va a llegar a usuarios reales, ampliar a ≥ 10 planes (opción b) **antes** de activarlo | Con 4 planes, «≥ 80 %» ya equivale a «4 de 4». Decirlo así evita una precisión que la muestra no tiene |
| **A3.** Acuerdo | Mínimo ≥ 70 % de acuerdo en ±1 punto por criterio; si no se alcanza, una **reunión de consenso** (antes que un tercer evaluador) | Mantiene el número de evaluadores y resuelve las discrepancias con argumentos |
| **A4.** D01/D02 (NO_FACTIBLE con peso corporal) | Se mantiene «no generar la semana» si **todos** responden «Sí, es pertinente». Un «No» abre una revisión aparte de la sesión alternativa, sin cambios automáticos | Coherente con D3: no publicar una rutina incorrecta |
| **A5.** Punto de anclaje como única tracción | Un «No» de cualquier evaluador basta para dejar `anchor point` **solo como último nivel con advertencia** (no para retirarlo) | Conserva una vía para quien solo dispone de eso |
| **A6.** Evaluadores | **Dos** profesionales (con uno no se mide el acuerdo). Desempate: reunión de consenso; si persiste, prevalece la respuesta **más conservadora en seguridad** | Reproducible y alineado con la prioridad de proteger al usuario |

## Reglas de procedimiento (aprobar junto con A1–A6)

1. Los criterios se aprueban **por escrito y con fecha**, antes de enviar el paquete, y no se cambian después de ver resultados.
2. Se envía **solo** `paquete_evaluador/`. La clave se abre después de recibir y guardar todas las plantillas.
3. Los resultados se registran tal como llegan, sin interpretarlos ni completarlos.

## Lo que decide cada resultado

| Componente | Criterios | Si no se supera |
|---|---|---|
| C2-D | A2 y la pregunta de duración | Sigue apagado |
| EF | e1–e3, G1 y G2 | Sigue apagado |
| Lista de equipamiento | G3 y A5 | Se revisa la lista |
| LW-PREF-0.2 | e7 y G4 | Sigue apagado |
| REP-SUST-0.1 | G5 | Sigue apagado |
| NO_FACTIBLE | A4 | Revisión aparte |

## Qué falta para empezar

- [ ] Aprobar A1–A6 y las reglas de procedimiento.
- [ ] Elegir a los dos profesionales y acordar el plazo.
- [ ] Autorizar el envío de `paquete_evaluador/`.
