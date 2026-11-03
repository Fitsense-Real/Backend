# Comparación funcional C1 frente a C2

**Fuentes:**
- `14_MATRIZ_CALIDAD_POR_ADAPTACION.csv`: las 32 sesiones adaptadas con Replicate real en C1 r1, C1 r2, C2 r1 y C2 r2 (PR #29), con las 12 preguntas de la fase 10;
- `02`/`03`: sustituciones;
- `06`/`07`: continuidad y combinaciones, sin red.

**La fase complementaria con Replicate sobre R01–R08 no se ha ejecutado:** espera la aprobación del presupuesto (`09`). Por eso `12` y `13` no existen todavía, y esta comparación se apoya en las corridas del PR #29.

**Tres niveles, separados (fase 10):**
- **Validez técnica:** se cumplen las reglas implementadas.
- **Coherencia funcional:** la modificación es compatible con el propósito de la intervención, según el catálogo y las reglas documentadas.
- **Adecuación profesional:** **no evaluada**. Ninguna prueba de software la sustituye.

## 1. Resumen

| | C1 (r1 + r2) | C2 (r1 + r2) |
|---|---:|---:|
| Sesiones adaptadas | 12 | 20 |
| **Técnicamente válidas** | **12 (100 %)** | **20 (100 %)** |
| Coherentes según el catálogo y la regla | 7 (58 %) | 12 (60 %) |
| Funcionalmente discutibles (Q2/Q3) | 5 (42 %) | 8 (40 %) |
| …con cambio de músculo principal (sesiones; no excluyentes) | 2 | 3 (4 sustituciones) |
| …VARIANTE con alguna sustitución de dificultad igual (sesiones) | 4 | 4 |
| …VARIANTE resuelta solo bajando la dosis (sesiones) | 1 | **3** |
| Sesiones con margen 0 tras la adaptación | 2 | 5 |
| Fuera de alcance, dosis subida, descanso cambiado, bajo el mínimo, linaje | 0 | 0 |

**Lectura:**
- **C2 aplica más** (20 sesiones frente a 12) **sin peor proporción de cambios discutibles** (40 % frente a 42 %).
- **Los problemas funcionales son los mismos en las dos condiciones**, porque vienen de la regla (V8C por patrón) y del catálogo (`03`), no del prompt.
- **Dos diferencias de estilo de C2:**
  - **(a) Ante TOO_DIFFICULT, C2 baja la dosis** (una serie menos de cardio, −10 s) en lugar de sustituir: U14 S2 en r1 y U19 S2 en r1 y r2. En C1 r1 también ocurre, una vez (U19 S2). El objetivo VARIANTE de V2 dice «variantes más sencillas **y/o algo menos de dosis**», y la intervención («o ajustar la carga») lo permite. Pero **no simplifica la técnica**: es Q5.
  - **(b) C2 lleva más sesiones exactamente al mínimo V20**: 5 frente a 2 (`06`).

## 2. Las 12 preguntas, en conjunto

| # | Pregunta | C1 | C2 |
|---|---|---|---|
| 1 | ¿Aplicado? | 12 de 12 (APLICADA y publicado) | 20 de 20 |
| 2 | ¿Responde al motivo? | Fatiga e INVESTIGAR: sí (reducen dosis). VARIANTE: 5 de 6 parcial (dificultad igual o solo dosis) | Fatiga e INVESTIGAR: sí. VARIANTE: 7 de 8 parcial |
| 3 | ¿Patrón conservado? | Sí, en todas | Sí, en todas |
| 4 | ¿Músculo conservado? | No en 2 sustituciones (580 → 313 y 933 → 318) | No en 4 (580 → 129, 775 → 313 y 780 → 119 ×2) |
| 5 | ¿Dificultad? | Nunca sube | Nunca sube |
| 6 | ¿Dosis respetada? | Sí: nada sube, descanso igual | Sí |
| 7 | ¿Equipamiento? | Dentro del catálogo filtrado del perfil | Ídem |
| 8 | ¿Duración válida? | Sí, ≥ mínimo | Sí, ≥ mínimo |
| 9 | ¿Resto del entrenamiento conservado? | Sí | Sí |
| 10 | ¿Combinación válida? | Sí (las combinaciones inválidas se rechazaron antes) | Sí (0 rechazos) |
| 11 | ¿Continuidad? | Válida. 2 sesiones en margen 0: en la siguiente semana se conservan (`06`) | Válida. 5 sesiones en margen 0: ídem |
| 12 | ¿Revisión profesional? | Sí: 5 sesiones (y muestra del resto) | Sí: 8 sesiones (y muestra del resto) |

## 3. Estabilidad en escenarios nuevos (sin Replicate)

Con la cohorte R01–R08 y el guion V2 (`R_GUION_2S`):
- La **descripción de V2** fue correcta en todos los perfiles nuevos:
  - márgenes de 0, 6, 30, 60, 102, 151, 201 y 228 s;
  - minutos 30–75;
  - casa o gimnasio.

  Lo que V2 llama válido se aceptó; lo que llama «NO cabe» se rechazó por V20; y donde dice «sin solución» la sesión se conservó.
- Esto prueba que **la factibilidad es correcta** en perfiles nuevos. **No prueba** cómo responderá el modelo real a esos perfiles: eso es la fase con Replicate (`09`), pendiente de autorización.
