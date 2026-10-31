# Referencias V39 → V40: diferencias revisadas

**Qué cambia:**
- Solo el catálogo: 7 ejercicios de C4/C6, en `V40__catalog_c4_c6.sql`. El motor es el mismo.
- Cambian **22 de 72 perfiles**, todos de gimnasio intermedio o avanzado, en los cuatro objetivos. Ningún perfil de casa ni de principiante cambia.
- La referencia nueva es **idéntica byte a byte** a la de la simulación exploratoria «combinada + 953» (`evidencia/c4_c6/resultados/combinada953/`).

## Comprobaciones

| Comprobación | V39 | V40 |
|---|---|---|
| Referencias válidas, con todos los validadores (V15, V18, V20, V23, V24 y V25, duración incluida) | 72/72 | 72/72 |
| Problemas automáticos sobre todos los ejercicios (`revision_automatica_v39_v40.md`): equipamiento de casa, dificultad del nivel, impacto en principiante, movilidad sin dosis de fuerza, GAIN_MUSCLE solo resistido o estructural, LOSE_WEIGHT con cardio en cada sesión | 0 | 0 |
| **Ejercicios de tronco (`waist`) que acreditan un slot de fuerza** | **52** | **0** |
| Slots de pierna acreditados por un ejercicio cuyo grupo no es de pierna | 70 (1225 ×31, 439 ×17, 959 ×15, 962 ×6, 952 ×1) | **11, todos de 262** |
| Capacidades del objetivo cubiertas en el barrido | 72/72 COMPLETO | 72/72 COMPLETO (simulación final) |

## Qué entra en lugar de los corregidos

- **Cadera:** puentes y extensiones de cadera reales. Por ejemplo 878 *glute bridge march*, 309 *bench hip extension*, 879 y 239 *good morning*.
- **Días de tirón:** remos reales. Por ejemplo 518, 1037, 1041, 478, 911 y 1002.
- **Rodilla:** sentadillas y zancadas. Por ejemplo 595, 791 y 1205.

## Vigilado: 262 (barbell skier)

| | |
|---|---|
| Uso | Entra **11 veces**, siempre en el FULL_BODY de los lunes de avanzados de gimnasio, como fuerza de cadera (LOWER_HIP) |
| Registro | `shoulders`/`HIP_HINGE` |
| GIF (revisión asistida por IA) | El tronco se queda inclinado y **quieto** mientras la barra se balancea con los hombros: es una extensión de hombro, con el deltoide posterior resaltado. **No hay bisagra de cadera** |
| Conclusión | **Sí está acreditando LOWER_HIP de forma indebida.** No se ha modificado, como pide la decisión. Es el primer candidato de la siguiente fase (probable `shoulders`/`SHOULDER_EXTENSION`) |
