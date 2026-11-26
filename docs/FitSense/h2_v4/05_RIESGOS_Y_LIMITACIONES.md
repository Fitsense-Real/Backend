# 05 · Riesgos y limitaciones

1. **Latencia, la causa sigue sin demostrar.** El tamaño de la entrada explica menos de 1 s de los +6,3 s de PR-2.1 (`01` §2). Que el resto sea razonamiento oculto es la hipótesis más probable, pero Replicate no da tokens. PR-2.2 quita cruces de listas y la tabla, y acorta el texto entre 1.902 y 2.950 caracteres frente a PR-2.1. **Que eso baje `predict_time` solo lo puede decir la evaluación real.**
2. **Variabilidad del modelo y del día.** Con 26 parejas, K2 se decide por la mediana de cada brazo, y entre días varía mucho: +15,4 s pareados con PR-2.0 pasaron K2, y +6,3 s con PR-2.1 no. Un día con la carga de Replicate desigual entre brazos puede decidir el resultado. La opción C reduce ese riesgo sin cambiar el criterio.
3. **Sobreajuste.** Las correcciones de G17, G21 y G24 **no usan ningún ejemplo de esos perfiles**:
   - la regla de la mitad es la general (`n / 2` desde 4, la de `ContratoReparto`);
   - la marca `cuenta` sale de las mismas reglas del backend para todos.

   Aun así, se diseñaron mirando estos tres casos. La evaluación con los 26 perfiles, y con réplicas en la opción C, es la que comprueba que no empeora el resto.
4. **Validez que puede bajar.** Al quitar la comprobación de cinco puntos, el modelo podría revisar menos y volver a fallar la cobertura o la mitad. La comprobación sigue, pero en un párrafo que remite a las reglas. Si así fuera, P2 y K1 lo detectarían.
5. **Seguridad.** O20 no se toca:
   - lo excluido por dolor, las equivalencias y las precauciones no llegan a `available_exercises`, igual que antes;
   - está probado con Replicate simulado (`H2v4PipelineSimuladoIT`);
   - los 26 perfiles congelados no tienen dolor, así que la evaluación real no lo ejercita.
6. **El lector literal no es el validador.** `AplicadorJerarquiaPR21` (ahora lee PR-2.1 y PR-2.2) solo muestra que **existe** una semana válida siguiendo las instrucciones; quien decide sigue siendo `PlanDraftValidator`. Sus salidas corregidas **no son generaciones de la IA** y no se presentan como tales.
7. **Dependencia del texto de PR-1.7.** PR-2.2 se construye sustituyendo frases de PR-1.7, igual que PR-2.0 y PR-2.1. Si PR-1.7 cambia, fallan de forma ruidosa y las pruebas lo detectan.
8. **H2 no se declara resuelta.** Las condiciones de `README` §Aceptación dicen que PR-2.2 está **preparada para evaluar**, no que funcione.
