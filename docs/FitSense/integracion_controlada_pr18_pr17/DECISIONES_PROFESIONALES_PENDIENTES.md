# Decisiones profesionales pendientes (criterios A1–A6)

**Estado:**
- **Ninguna evaluación enviada, recibida ni registrada.**
- C2-D + EF **no** está validado profesionalmente.
- **El paquete no se enviará hasta que apruebes estos criterios.**

## 1. Paquete disponible (comprobado tras la integración)

`docs/FitSense/revision_profesional/paquete_evaluador/`:

| Archivo | Contenido |
|---|---|
| `MUESTRA_PROFESIONAL_CIEGA.csv` | **34 planes completos** (681 filas, 23 columnas) |
| `FICHAS_NO_FACTIBLE.csv` | **D01 y D02** (10 columnas) |
| `PLANTILLA_RESPUESTAS_EVALUADOR.csv` | 36 códigos, **vacía** |
| `INSTRUCCIONES_EVALUADOR.md` | Escala y preguntas |

**Sin la clave.** La integración no cambió ninguno de estos archivos.

## 2. Criterios para tu decisión

### A1. ¿Cuándo es aceptable un plan?

**Propuesta:**
- media ≥ 3 en c1–c11;
- ningún c9 (Seguridad) ≤ 2;
- «Sí» o «Con cambios» en la pregunta de duración;
- ningún «No» en e2 (función de la sesión) ni en e3 (tracción real).

**A decidir:** si se aprueba tal cual o si, por ejemplo, se exige una media ≥ 3,5 en los planes de nivel principiante.

### A2. ¿Cuándo es aceptable C2-D + EF? (el umbral del 80 %)

**Propuesta del instrumento:** ≥ 80 % de los planes con sesión corta publicada por C2-D son aceptables (A1), y ninguno tiene c9 ≤ 2.

**El problema: la muestra solo tiene 4 planes de ese tipo.**

| Planes aceptables de 4 | % | ¿Pasa con «≥ 80 %»? |
|---|---|---|
| 4 | 100 % | Sí |
| 3 | 75 % | **No** |
| 2 o menos | ≤ 50 % | No |

En la práctica, «≥ 80 %» con 4 planes significa **«los 4»**. Un único plan no aceptable bloquea la decisión. Con una muestra tan pequeña, el porcentaje no tiene precisión: con 4 de 4, el intervalo de confianza del 95 % de la proporción real va aproximadamente del 40 % al 100 %.

**Opciones:**

| Opción | Regla | Ventaja | Inconveniente |
|---|---|---|---|
| **a)** | 4 de 4 aceptables y ningún c9 ≤ 2 (decirlo así, sin porcentaje) | Clara y conservadora | Muy sensible a un solo caso |
| **b)** | Ampliar los planes de sesión corta publicados por C2-D a 10 o más (por ejemplo, con la misma regla de selección sobre la matriz de 960) y mantener ≥ 80 % | El porcentaje tiene sentido | Más trabajo del evaluador y regenerar el paquete (nuevos códigos y clave) |
| **c)** | Decisión cualitativa: los 4 se revisan uno a uno, y basta un rechazo con motivo de seguridad o de función para no activar C2-D | Aprovecha el criterio profesional | Menos reproducible |

**Recomendación:**
- **a)** si se quiere decidir ya con el paquete actual;
- **b)** si C2-D va a activarse para usuarios reales a medio plazo.

En cualquier caso, **ningún c9 ≤ 2** debe bloquear.

### A3. Acuerdo entre evaluadores

**Propuesta:** acuerdo si la diferencia es ≤ 1 punto por criterio; se informa el porcentaje de acuerdo exacto y en ±1.

**A decidir:**
- si se exige un mínimo, por ejemplo ≥ 70 % de acuerdo en ±1, para dar por válida la evaluación;
- qué se hace si no se alcanza: un tercer evaluador o una reunión de consenso.

### A4. Fichas D01 y D02 (NO_FACTIBLE con peso corporal)

**A decidir:** qué respuesta mantiene «no generar la semana». **Propuesta:** «Sí, es pertinente» de todos los evaluadores.

**Si alguno dice «No»:**
- su sesión alternativa con los ejercicios disponibles se revisa aparte;
- no se cambia nada automáticamente.

### A5. G3: ¿basta un único ejercicio de tracción (punto de anclaje)?

**A decidir:** si una respuesta «No» de cualquier evaluador basta para retirar `anchor point` de la lista de equipamiento sugerida, o para dejarlo solo como último nivel con advertencia.

### A6. Número de evaluadores y desempate

**A decidir:**
- uno o dos profesionales. Se recomiendan **dos**: con uno solo no se mide el acuerdo;
- el procedimiento si no hay consenso.

## 3. Reglas que conviene aprobar junto con A1–A6

- Los criterios se aprueban **por escrito y con fecha, antes de enviar el paquete**, y no se cambian después de ver los resultados.
- La clave se abre **solo después** de recibir y guardar todas las plantillas.
- Lo que depende del resultado:

| Componente | Preguntas o criterios |
|---|---|
| C2-D | A2 y la pregunta de duración |
| EF | e1–e3, G1 y G2 |
| Lista de equipamiento | G3 y A5 |
| LW-PREF-0.2 | e7 y G4 |
| REP-SUST-0.1 | G5 |
| NO_FACTIBLE | A4 |
