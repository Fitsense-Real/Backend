# GRA · Incidencias y correcciones

**Regla aplicada:** primero investigar y después decidir.

- **No se encontró ningún defecto del sistema**, así que no se ha corregido nada en el backend, el prompt, los validadores, los perfiles, los criterios ni el oráculo.
- **No se repitió ninguna llamada ni ninguna corrida.**
- Toda la evidencia original está en `../evidencia/`.

## 1. Lo encontrado antes de pagar (preparación)

| N.º | Hallazgo | Clase | Decisión |
|---|---|---|---|
| P-1 | La generación inicial con IA **ya existía** en el backend (`PlanGenerationPipeline`), pero el arnés la desactivaba en el modo integral | Arnés | Habilitarla con un interruptor de pruebas, `generacion-ia=SI`. No hace falta implementar nada en producción |
| P-2 | El pipeline llama a `requestOutput(context, feedback, attempt)`, de **3 argumentos**, y el arnés solo interceptaba la variante de 2 | **Defecto del arnés** (demostrado leyendo el código) | Corregido **antes** de pagar: ahora se interceptan ambas. Sin esto, las llamadas de generación habrían escapado al control del presupuesto y a la traza |
| P-3 | El oráculo no distinguía si el plan S1 venía de la IA o del respaldo | Instrumento (falta de cobertura) | **v1.4** = v1.3 + R-2, con 6 pruebas propias. Idéntico a v1.3 en todas las corridas anteriores |

## 2. Resultados desfavorables en las corridas pagadas

| N.º | Corrida | Caso | Qué ocurrió | Clase | Decisión |
|---|---|---|---|---|---|
| G-1 | GRA-R1 | 10 propuestas de generación rechazadas en 7 perfiles (O03, O06, O07, O12, O13, O16 y O17), por V15 (6), V26 (6), V23 (2), V24 (1) y V4 (1) | El backend las rechazó con el motivo exacto y reenvió la propuesta con esos motivos. **Las 7 se resolvieron en el reintento** (2.º o 3.er intento) | **Respuesta insuficiente de la IA, rechazada correctamente** | Ninguna: es el comportamiento diseñado (política 19.4). Ningún plan se publicó por respaldo |
| G-2 | GRA-R2 | 6 propuestas de generación rechazadas en 6 perfiles (O03, O07, O13, O14, O16 y O20), por V15 (4), V26 (5), V13 (1) y V18 (1) | Igual: todas resueltas en el 2.º intento | Igual | Ninguna |
| A-1 | GRA-R2 | **O18, C1 (ACORTAR, GAIN_MUSCLE):** SIN_EFECTO propuesto por el modelo («no hay ninguna corrección válida que pueda hacer») | La sonda de factibilidad encontró **una** modificación válida: retirar el relleno 1141. El prompt identifica el relleno (`clase_dosis: RELLENO_ZONA_PEQUENA`) y autoriza retirarlo | **Respuesta insuficiente de la IA** (contexto correcto, justificación falsa del modelo). Es la misma conducta que en R_O | Ninguna en el sistema. Se registra como **SIN_EFECTO evitable**, no como adaptación lograda (Tabla 4: 9/10) |
| A-2 | GRA-R2 | **O12, C1 (dolor de sesión):** la IA sustituyó los 6 ejercicios por 5, entre ellos un **patrón nuevo** (HORIZONTAL_PULL), con menos trabajo (786 → 372 s) | Cumple la regla aprobada de la rama de dolor: no hay más añadidos que retirados, el volumen y la dificultad no suben, y no queda ningún ejercicio de la sesión con dolor (v1.3, D-OR-3) | **Comportamiento válido del modelo**, con pertinencia ALT | **Observación deportiva separada:** introducir patrones nuevos ante un dolor sin localizar queda para revisión profesional (REQUIERE REVISIÓN del criterio, no de la técnica) |
| A-3 | GRA-R2 | **Sin reincorporación** en O18 (por A-1) ni en O19 (la IA redujo dosis en lugar de retirar, como en R_O y R′_O) | Sin retirada no hay nada que reincorporar | Consecuencia de A-1 y del comportamiento válido de la IA | **NO APLICA**, no FALLO. No se fuerza ninguna retirada |
| D-1 | Ambas | **Plan inicial con dosis por debajo de la orientativa** más a menudo con la IA: DOSIS_LIMITADA en 16 de 19 planes (R_O determinista: 9 de 19) | Los planes de la IA son válidos (A1 y A2 aprobados) pero más conservadores en volumen semanal. El backend lo avisa con `/plan/limitations` | **Comportamiento válido pero diferente; pertinencia deportiva** | Se informa. No se fuerza más volumen (no hay regla que lo exija) |

## 3. Incidencias anteriores vigiladas (orden §11)

| Caso | GRA-R1 | GRA-R2 |
|---|---|---|
| **O12, dolor de sesión** | La IA retiró los 6 ejercicios y puso otros sin patrones nuevos (ACC). No queda ninguno incompatible | 6 → 5, con un patrón nuevo (ALT, A-2). Técnicamente válido; observación deportiva separada |
| **O18, SIN_EFECTO evitable** | No hubo: la IA retiró el relleno 502 y se **reincorporó en C14**, al final, con la dosis inicial y tras progresión | Sí (A-1): registrado como evitable |
| **O03, recuperación** | Se redujeron 3 ejercicios, 3 → 2 series. **RECUPERACION en C3** dentro del 10 % | Igual: RECUPERACION en C3. **No hubo bloqueo:** el límite del 10 % no se alcanzó. La política no se tocó |
| **Catálogo** | A1 REQUIERE REVISIÓN en 6 planes iniciales | En 7 | 

Sobre el catálogo: menos que con el generador determinista (14). Los hallazgos (E2, E3, E4/E7) siguen bajo revisión profesional.

## 4. Correcciones aplicadas

**Ninguna al sistema.** Las dos únicas modificaciones se hicieron antes de pagar y son del instrumento:
- P-2, en el arnés;
- P-3, el oráculo v1.4.

Las dos tienen pruebas y quedaron congeladas en `b60e5ea` antes de la primera llamada pagada.
