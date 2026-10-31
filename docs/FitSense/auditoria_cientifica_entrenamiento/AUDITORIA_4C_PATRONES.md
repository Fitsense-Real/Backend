# Auditoría de 4C: patrones obligatorios y estructura de la semana

**Qué se audita:**
- la cobertura semanal de 4 slots (V23, `SlotCoveragePolicy`, `validateWeeklySlotCoverage`);
- la fuerza en los 4 slots (V25 en INCREASE_STRENGTH);
- la cobertura del enfoque (V15);
- la recuperación entre días consecutivos (V18);
- el número de sesiones.

**Especificación original:** «Step 4C §5». **No está en el repositorio**, así que no se reconstruye su contenido. Las reglas se recuperan del código y de las pruebas.

## 1. Reglas recuperadas

| Regla | Valor | Alcance | Implementación | Clase | Motivo |
|---|---|---|---|---|---|
| V23: 4 patrones en la semana | Empuje, tracción, dominante de rodilla y de cadera | Todos salvo LOSE_WEIGHT (4A v6) | `SlotCoveragePolicy`, `SlotCoverageGate` | **B** | Operacionaliza «todos los grandes grupos ≥ 2 d/semana» [S05, S34] y la preferencia por multiarticulares [S19, S22]. La **taxonomía** no tiene evidencia directa [S53, SR]. |
| V25-IS: fuerza en los 4 slots | 1 colocación de fuerza por slot y semana | INCREASE_STRENGTH | `PlanDraftValidator` V25 | **B** | En la fuerza influyen la práctica del patrón y la frecuencia [S14] y la carga relativa [S46]. |
| V15: grupos por enfoque y regla de la mitad | ≥ min(2, disponibles) grupos; ningún grupo > ½ si hay ≥ 3 grupos y ≥ 4 ejercicios | Todos | `validateFocusCoverage` | **C** | Regla de variedad sin fuente. Es compatible con la variación sistemática [S20]. |
| V18: sin repetir ejercicio ni grupo grande en días consecutivos | Complementos permitidos | Todos | Generadores y validador V18 | **C** | No se localizó una regla primaria de 48 h. Con el volumen igualado, la distribución apenas importa [S13, S23]. Es conservadora y compatible con 2–3 d/semana por grupo [S05, S06]. |
| Sesiones por semana | 1–6; máximo 4 en principiantes | Todos | `PlanGenerationContext.MAX_SESSIONS_*` | **B** | 2–3 d/semana en principiantes [S06]. El reparto dividido y el de cuerpo completo dan resultados similares [S23]. |
| Enfoques (FULL_BODY, PUSH/PULL/LEGS, etc.) | Según días | Todos | `WorkoutFocus`, divisores | **B** | Divididos y cuerpo completo son equivalentes con volumen igualado [S23]. |

## 2. Distribuciones semanales razonables

Son **inferencias**, no normas.

| Días | Estructura vigente | Valoración |
|---|---|---|
| 1 | Cuerpo completo | B: sesiones únicas con dosis mínima eficaz [S17, S19]. No permite ≥ 2 d/semana por grupo [S05]. Es un límite del usuario, no de la regla. |
| 2–3 | Cuerpo completo o división superior/inferior | B: cumple ≥ 2 d/semana por grupo [S05, S34]. |
| 4 | Superior/inferior | B. |
| 5–6 | PUSH/PULL/LEGS y variantes | B: equivalente con volumen igualado [S23]. V18 impide repetir grupo en días seguidos. |
| 7 | La admisión permite 7 días, pero el plan tiene 6 sesiones como máximo | C. |

## 3. Riesgos y brechas

1. **Los 4 patrones no garantizan el core, los hombros ni los gemelos** [S53, SR]. Se documenta y no se propone exigirlos: añadir obligaciones reduce la holgura y empeora V20.
2. **LOSE_WEIGHT está fuera de V23.** Ver `AUDITORIA_4A_OBJETIVOS.md` §3.
3. **V18 es más restrictiva que la evidencia.** Es una C conservadora, y relajarla no está justificado sin un motivo de factibilidad. El laboratorio no detecta que V18 cause rechazos.
4. **V15** produjo 6 de los 85 rechazos de C1, todos en LOSE_WEIGHT con restricciones o dolor. Es un efecto del catálogo reducido, no de la regla.

## 4. Conclusión

**4C se conserva íntegra.**
- Las reglas que tienen fundamento indirecto (V23 y V25) son **B**.
- Las de variedad y recuperación (V15 y V18) son **C**: se conservan y se documentan como decisiones de ingeniería.
- **No se cambia ningún patrón obligatorio.**
