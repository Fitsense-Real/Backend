# Casos extremos del 46 % (R0788, R0792, R0836, R0840)

**Perfiles:** GAIN_MUSCLE, intermedio o avanzado, **solo peso corporal**, 6 o 7 días, 75 minutos. División PUSH/PULL/LEGS.

**Datos:**
- `CASOS_EXTREMOS_DETALLE.csv`: cada colocación con su patrón, clase, compatibilidad EF y ancla.
- `CASOS_EXTREMOS_ELEGIBLES.csv`: elegibles por enfoque.
- `CASOS_EXTREMOS_ALTERNATIVA.csv`
- `MATRIZ_COMPARACION_FUNCIONAL.csv`

Generador y validador reales, PostgreSQL efímero, sin IA. **El catálogo no se modificó.**

## 1. Lo que publicaba C2-D (catálogo original)

Las cuatro dan el mismo plan (R0788 como muestra):

| Sesión | Segundos | % de M | Contenido |
|---|---|---|---|
| PUSH (×2) | 2.430 | 54,0 | 6 empujes horizontales + **973** (rotación de tronco con zona hombros) |
| **PULL (×2)** | **2.070** | **46,0** | **1259** rotación de tronco · **315** curl con mancuerna (equipo «peso corporal») · **775** empuje con zona espalda · 320 curl sin carga · **1308** y **1261** flexión de tronco · 1066 extensión lumbar |
| LEGS (×2) | 2.577 | 57,3 | 5–6 de rodilla o cadera + gemelo |

**Síntesis:**
- 116 series por semana; C2-D: SATURADA.
- **En el PULL no hay ninguna tracción.** De sus 7 colocaciones, solo 320 y 1066 son de la familia PULL, y **ninguna es ancla**.

## 2. Elegibles reales del PULL (peso corporal)

| Perfil | En zona (back, bíceps) | Compatibles con PULL | **Tracciones (ancla)** | Patrones compatibles |
|---|---|---|---|---|
| R0788 y R0836 (intermedio) | 19 | 5 | **0** | ELBOW_FLEXION 1 · TRUNK_EXTENSION 1 · MOBILITY 2 · OTHER 1 |
| R0792 y R0840 (avanzado) | 20 | 5 | **0** | Igual |

Las tracciones con peso corporal del catálogo (303–308 «remo de pie», 1072 «remo con toalla») **exigen material** (barras de pared o un anclaje, V39). No son elegibles sin ese material declarado.

## 3. Con la corrección funcional (`PLAN-C2D-EF-0.1`)

| Sesión | Segundos | % de M | Contenido |
|---|---|---|---|
| PUSH | 1.560 | 34,7 | 4 empujes horizontales (sin 973) |
| PULL | 780 | 17,3 | 1066 + 320 |
| LEGS | 2.577 | 57,3 | Sin cambio de función |

**Resultado: INVÁLIDO, por VEF y V20.**
- **VEF:** «el entrenamiento PULL … no tiene ningún ejercicio de tracción (HORIZONTAL_PULL o VERTICAL_PULL) y el catálogo elegible con el equipamiento declarado no tiene ninguno (**NO_FACTIBLE por catálogo o equipamiento insuficiente**…)». Dos sesiones.
- **C2-D no actúa:** no todo es V20.

El PUSH baja de 7 a 4 colocaciones:
- 973 (tronco) deja de contar.
- La zona `shoulders` no tiene ningún otro candidato compatible con peso corporal.
- Los topes de zona por sesión del selector (V15: ningún grupo por encima de la mitad; brazo con tope propio) limitan pecho y brazo.

**No es un fallo nuevo:** sin 973, el PUSH sin material solo tiene pecho y tríceps.

## 4. Preguntas de la orden

| Pregunta | Respuesta |
|---|---|
| ¿Existe una alternativa funcional? | **No** con el equipamiento declarado. 0 tracciones elegibles |
| ¿Es factible con menos ejercicios? | **No.** Retirar las 12 colocaciones no computables del plan de C2-D deja un plan inválido: V17, VEF y V20 (`CASOS_EXTREMOS_ALTERNATIVA.csv`). Menos ejercicios no crea una tracción |
| ¿Es imposible el objetivo? | No el objetivo, sino **esta división** (PULL dedicado) **con este equipamiento**. Sin material, una semana de hipertrofia de 6–7 días con PULL no tiene contenido de tracción |
| ¿Registrar falta de equipamiento? | **Sí.** VEF lo registra en el rechazo («catálogo o equipamiento insuficiente»). La decisión de producto (proponer declarar material, cambiar de división, avisar) queda fuera de esta orden y requiere aprobación |
| ¿Es adecuado por estar saturado? | **No.** C2-D lo consideraba saturado porque nada **admisible** mejoraba la sesión. Lo admisible no era pertinente. Con EF, la saturación ya no se evalúa: hay un problema que no es de duración |

## 5. Estado

| Política | R0788 | R0792 | R0836 | R0840 |
|---|---|---|---|---|
| PLAN-HIST-1 | INVÁLIDO (V20) | INVÁLIDO (V20) | INVÁLIDO (V20) | INVÁLIDO (V20) |
| PLAN-C2D-0.1 | VÁLIDO (C2-D) | VÁLIDO (C2-D) | VÁLIDO (C2-D) | VÁLIDO (C2-D) |
| **PLAN-C2D-EF-0.1** | **NO_FACTIBLE (VEF)** | **NO_FACTIBLE (VEF)** | **NO_FACTIBLE (VEF)** | **NO_FACTIBLE (VEF)** |

**Deben quedar NO_FACTIBLE** mientras el catálogo no tenga una tracción sin material validada o el usuario no declare material. Ningún plan se ha «aprobado» ni se recomienda publicarlo.
