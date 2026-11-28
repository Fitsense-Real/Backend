# E5 / E6 · Diseño de equipamiento: sin material, silla o banco, anclaje y especializado

**Estado:** diseño y medición.
- No se ha cambiado ninguna migración, ni la política, ni `mobileapp`.
- **No se asume que todos los usuarios tengan una silla o un banco.**

## 1. Qué modela hoy el catálogo (V23)

- **Requisitos por ejercicio** (`exercise_equipment_requirements`): grupos distintos se combinan con AND; implementos del mismo grupo, con OR. Un ejercicio **sin filas no exige nada** y siempre es elegible.
- **El perfil declara una lista de implementos.** La app ofrece `body weight` y los tipos de `equipment_types`, que ya incluyen `bench`, `step platform`, `anchor point`, `wall bars` y otros (V22, V38, V39).
- **No existe «silla».** Un ejercicio que se hace sentado en una silla o en un banco y no declara `bench` se trata como de peso corporal puro.

**Consecuencia medida** (escaneo S1 de V46):
- 75 ejercicios usados en algún plan tienen instrucciones que exigen sentarse o apoyarse en un banco o silla, pero no declaran ese requisito.
- 1320 no está entre ellos: su GIF es en el suelo.
- Los curls nórdicos 1195 y 1196 no declaran el anclaje que muestra el GIF.

## 2. Diseño propuesto: cuatro categorías

| Categoría | Qué es | Cómo se modela (sin tocar el contrato V23) | Qué se pregunta al usuario |
|---|---|---|---|
| **Sin material** | Solo el cuerpo y el suelo | Ejercicio sin filas de requisitos | Nada (siempre disponible) |
| **Silla o banco** | Superficie estable para sentarse o apoyarse, de altura normal | Tipo nuevo `chair` («Silla estable»), `requires_gym = false`. Grupo de requisito **`bench` O `chair`** en los ejercicios que lo exigen | «¿Tienes una silla estable o un banco?» (sí/no) **Por defecto: no** |
| **Anclaje** | Un punto fijo para sujetar los tobillos o una banda | Tipos ya existentes: `anchor point` (V39) y `wall bars`. Grupo de requisito en los ejercicios que lo exigen (nórdicos 1195 y 1196) | «¿Tienes un punto de anclaje firme (espaldera, anclaje de puerta, mueble pesado)?» **Por defecto: no** |
| **Especializado** | Máquinas, poleas, barras, bancos específicos | Lo actual (`requires_gym`, tipos de V22/V38/V39) | Lo actual |

**Reglas del diseño:**
- **Nunca** se presupone una silla, un banco o un anclaje: por defecto, «no».
- Un ejercicio cuyo GIF se hace en el suelo (como 1320) **no** debe exigir silla, aunque sus instrucciones la mencionen: se corrigen las instrucciones (E7).
- La lista de los 75 ejercicios procede de un escaneo de texto de las instrucciones. **Hay que confirmar cada uno con su GIF antes de añadirle un requisito.** Hoy solo están revisados visualmente 1320 y los nórdicos.
- **Pendiente profesional:** si un mueble doméstico es un anclaje seguro para el curl nórdico. Si no lo es, el nórdico sería solo de gimnasio (`requires_gym`).

## 3. Medición (barridos de 3.780 perfiles × HIST y REV2)

Las cifras están en `RESULTADOS_VARIANTES.md` §4.

| Variante | SQL | Qué simula |
|---|---|---|
| CF5a | `contrafactuales/CF5a_asiento_requerido.sql` | Los 75 ejercicios exigen `bench` O `chair`, y **nadie** declara `chair`. Es el **peor caso**: ningún usuario de casa tiene silla. El gimnasio tiene banco |
| CF5b | `contrafactuales/CF5b_nordico_anclaje.sql` | 1195 exige `anchor point` O `wall bars`; 1196 exige `bench` O `anchor point`. Ningún perfil del barrido los declara |
| V46 | — | Es el **mejor caso para la silla**: todos la tienen, porque hoy no se exige |

El efecto real de la silla queda **entre V46 y CF5a**, según qué fracción de usuarios declare tenerla.

## 4. Qué haría falta para adoptarlo (no hecho)

1. Revisión visual de los 75 ejercicios, para confirmar cuáles exigen de verdad silla o banco.
2. Migración candidata: tipo `chair` y requisitos, solo para los confirmados.
3. **`mobileapp`**: dos preguntas nuevas en el perfil (silla o banco; anclaje). **No se ha tocado** (orden).
4. Barridos, K20 y regresión. Además, una prueba de que un perfil sin silla nunca recibe un ejercicio que la exige, y de que uno con silla sí puede recibirlo.
