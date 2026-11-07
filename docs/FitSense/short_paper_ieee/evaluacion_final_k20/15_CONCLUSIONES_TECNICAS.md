# 15 · Conclusiones técnicas

## Lo que la evidencia congelada permite afirmar (D, D′)

Sobre los 20 escenarios sintéticos y la versión `a31c36b`:
1. **Generación.** FitSense genera 20/20 planes iniciales válidos según las reglas re-verificadas por un oráculo independiente.
2. **Registro y adherencia.** Registra fielmente la ejecución y calcula Aw, Aobs y C sin error (20/20; diferencia 0,00 pp).
3. **Decisión.** Toma la decisión que dicta su política sobre los valores reales en 20/20 casos. En 18/20 coincide además con el oráculo teórico del artículo; la diferencia (U07, U12) se debe a la entrada P40.
4. **Integridad.** Conserva intactas las sesiones no señaladas (46/46).
5. **Seguridad.** No publica ninguna rutina inválida (0/60) ni sube dosis o dificultad.
6. **Publicación.** Publica de forma idempotente, con linaje completo y un texto que describe lo ocurrido.
7. **Continuidad.** Permite continuar la semana siguiente y cerrar un segundo ciclo (20/20).
8. **Reproducibilidad.** La ejecución determinista es reproducible: la huella de D es idéntica a la de D′.

**Objetivo de ingeniería: 20/20 recorridos correctos y verificables, cumplido en D y D′.**

## Lo que aún NO se puede afirmar

- **Que la IA aporta adaptaciones pertinentes.** Sin IA, ninguna de las 12 sesiones que la requieren cambia: la política conserva. Hay **10 modificaciones factibles** que solo la IA puede producir. Si las produce o no, es la pregunta de R/R′, con el criterio preregistrado (`02` §5).
- **Que los rechazos protegen de propuestas reales.** El control S demuestra que el backend rechaza propuestas inválidas *construidas por el laboratorio*, no que rechace las de Replicate.
- **Nada sobre personas reales ni sobre efectos conductuales.**

## Siguiente paso

Autorizar R/R′ con:
- el SHA congelado;
- el límite propuesto: 30 predicciones y USD 2 por corrida;
- el criterio E-1/E-2/E-3.

O pedir cambios **antes** de ejecutar. Cualquier cambio de versión obliga a repetir D/D′.
