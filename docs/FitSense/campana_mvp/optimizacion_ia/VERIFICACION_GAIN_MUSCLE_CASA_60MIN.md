# Verificación · GAIN_MUSCLE en casa, 5–6 días, 60 min

**Fecha:** 31/10/2026, fase E de la orden. **Configuraciones:** GAIN_MUSCLE intermedio y avanzado en casa (peso corporal, mancuerna y banda), 60 min, con cuatro patrones de días: `[1-5]`, `[1,3,4,6,7]`, `[1-6]` y `[1,2,3,5,6,7]`. Son 8 en total.

## 1. Verificación previa, con la política 4D v3.2

| Paso | Resultado | Prueba |
|---|---|---|
| 1 · Reproducir las 8 con el motor actual | **0/8.** El día PULL dura 2430 s y el mínimo de V20 es 2520 s | `gainMuscleCasaSesentaMinutos` de la versión anterior; borrador en el log |
| 2 · ¿Existe una composición válida con las dosis actuales? | **No** (ver abajo) | `casaPullNecesitaLaSerieDelBiceps` |
| 3 · Simular una serie más solo en el bíceps de relleno | 6 × 3×10 + bíceps a **3×10** ≥ 2520 s | ídem |
| 4 · Duración, volumen, capacidades, equipamiento, grupos y validadores | Plan válido en V1–V25: dos grupos (V15), bíceps (V22), equipamiento de casa y HYPERTROPHY en todas las colocaciones | barrido de 24 |
| 5 · ¿Hay una alternativa de menor impacto? | **No** (ver abajo) | — |

**Por qué no existe una composición válida con las dosis actuales:**
- **No hay tracción estructural de brazo en casa:** entre los candidatos elegibles de `upper arms` admitidos en PULL, ninguno tiene slot. El curl (716, ELBOW_FLEXION) es RESISTIDO y va como relleno estándar a 2×10.
- **El día PULL necesita dos grupos (V15):** espalda y bíceps.
- **El techo de duración:** con el tope de 7 colocaciones de 4D v3.1 y la dosis fija, lo máximo es 6 de espalda a 3×10 más el bíceps a 2×10, que da **2430 s**.
- **El resultado:** no hay forma de llegar a 2520 s.

**Alternativas descartadas:**

| Alternativa | Por qué no |
|---|---|
| 3×13 | El bíceps no es zona pequeña, así que no le corresponde |
| Un segundo bíceps a 2×10 en vez de un estructural | Acorta la sesión |
| Un octavo ejercicio | El tope de 4D v3.1 es 7 |
| Bajar V20 o V15 | No autorizado |

La extensión mínima es **una serie más en un solo ejercicio de la sesión**, con unos 120 s adicionales (de 2430 a unos 2550 s).

## 2. Implementación (4D v3.3, `../../ADENDA_4D_v3_3.md`)

- **Qué cambia:** solo el relleno estándar de `upper arms` en el día PULL de GAIN_MUSCLE, y solo cuando hace falta para V20.
- **Qué no cambia:** la clase RELLENO_ESTANDAR, los otros días y los otros objetivos.
- **Coherencia:** la política, el motor, V24 y el prompt usan la misma condición.

## 3. Pruebas de aceptación

| Criterio | Resultado |
|---|---|
| Las 8 configuraciones antes imposibles | **8/8** con plan válido |
| Las 16 corregidas por 4D v3.2 | 16/16 |
| Cada serie de más es del tipo autorizado y necesaria (con una serie menos la sesión baja del mínimo) | Comprobado en las 24 (`seriesAdicionales`) |
| Los otros tres objetivos a 60 min | 72/72 |
| GAIN_MUSCLE de 30 y 45 min, y las referencias históricas | Ver la regresión en `CIERRE_OPTIMIZACION_IA_MVP.md` |
| Progresión y reincorporación | Ver la regresión en `CIERRE_OPTIMIZACION_IA_MVP.md` |

**No se relajaron V15 ni V20.**
