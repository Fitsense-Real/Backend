# Comparación de los tres caminos

Fuente: `MATRIZ_EXPERIMENTOS_REVISADA.csv` (`LaboratorioModeloEIT.matrizRevisada`).
- 960 perfiles dentro de P-01: 864 BASE, 48 con restricciones y 48 con dolor P-02.
- PostgreSQL 16 efímero (Testcontainers) y motor de reglas, sin IA ni Replicate.

**Ningún camino se declaró ganador antes de comparar.**

## 1. Definición

| Camino | Qué cambia | Qué se mantiene |
|---|---|---|
| **C1 Conservar** | Nada | Todo |
| **C2 Evolución mínima** | El piso de V20 se acota a lo que la estructura vigente puede entregar. Solo cuando **V20 es la única regla incumplida**, se baja el piso en pasos de 5 (70 → 65 → … → 50) hasta que la semana sea válida | V4, V24, 4A, 4C, 4D y dosis. Los planes válidos con el 70 % salen **idénticos** |
| **C3 Modelo E** | Sin piso de V20. Se planifica hasta M (no M × 1,15). Se rellena por volumen semanal por grupo (objetivo escalado por días), con techos de 10 series por grupo y sesión y 20 por semana | V4, V18, V22, enfoque, dosis base. **Necesita un cambio de V24** en 178 casos |

## 2. Resultados

| Indicador | C1 | C2 | C3 |
|---|---:|---:|---:|
| Válidos con su política | 875 | **950** | 781 |
| Experimentales (solo incumplen el tope o el presupuesto de V24) | — | — | 178 |
| Inválidos | 85 (V20 79, V15 6) | 10 (V15 6, V20 4) | 1 (V15) |
| GAIN_MUSCLE a 75 min | 0/60 | 56/60 | 60/60 (de ellos, solo una parte sin tocar V24) |
| V4 estricto excedido | 0 | 0 | 0 |
| Ejercicios por sesión (máximo) | 8 | 8 | 11 |
| % de M (media) | 78 | 76 | 67 |
| Planes con > 10 series de un grupo en una sesión | 14 % | 15 % | 4 % |
| Planes con algún grupo grande por debajo del objetivo escalado de E | 57 % | 58 % | 34 % |
| Idénticos a C1 | — | 925/960 | — |
| Tiempo medio de cálculo | 5,5 ms | 6,2 ms | 3,3 ms |

**Piso usado por C2 en los 950 válidos:**

| Piso | 70 % | 65 % | 60 % | 55 % | 50 % |
|---|---:|---:|---:|---:|---:|
| Planes | 881 | 7 | 4 | 64 | 4 |

El 55 % domina en GAIN_MUSCLE a 75 min, donde 7 × (3×13/90) llega a unos 2.739 s de 4.500.

## 3. Lectura

1. **C2 resuelve el problema operativo con el menor cambio.**
   - Pasa de 875 a 950 planes válidos y de 0 a 56/60 en GAIN_MUSCLE a 75 min.
   - No altera ni un plan que ya era válido.
   - No toca V24, 4A, 4C ni 4D.
   - Sí reduce el piso de V20 por debajo del 70 % en 79 perfiles, y la sesión puede durar el **55 % de lo declarado**. Es justamente el síntoma que V20 quería evitar (V12: «45 que sale de 25»).
   - Mitigación: informar la duración real estimada al usuario y limitar el piso inferior por objetivo, decisión D-02.
2. **C3 es el más coherente con la evidencia de volumen**, pero:
   - con el tope vigente de V24 solo es válido en 781/960, **menos que C1**;
   - para ser útil necesita sustituir V20 **y** V24 en la versión E. Eso está **no autorizado** en la ruta histórica y supone el coste de `ARQUITECTURA_E_CANDIDATA.md` §3;
   - recorta el volumen de algunos perfiles: INCREASE_STRENGTH principiante pasa de 30–72 a 24 series/semana, porque ya no rellena hasta el 70 %. Es más fiel a la evidencia [S17], pero es un **cambio de dosis** respecto de lo aprobado;
   - sus objetivos (10 y 4 series, escalado 4 × días, techo de 10) **no son normas**: son parámetros experimentales sin aprobar.
3. **C1 es defendible** salvo en GAIN_MUSCLE a 66–75 min (y unos pocos casos de 45–60 min), donde no da plan.

## 4. Criterios de elección

| Criterio | C1 | C2 | C3 |
|---|---|---|---|
| Respeta las restricciones de la orden (V4, V24 histórico, 4A/4C/4D) | Sí | Sí | No en la ruta histórica |
| Factibilidad | 91 % | 99 % | 81 % (+ 19 % si se cambia V24) |
| Regresión | Ninguna | 925/960 idénticos; el resto, planes nuevos donde no había | Planes distintos en casi todos los perfiles |
| Coherencia con la evidencia de volumen | Media | Media | Alta, con parámetros sin aprobar |
| Coste | 0 | Bajo: parametrizar un piso por estructura y una versión de política | Alto: migración, validador por versión, prompt de IA, batería espejo |
| Riesgo | Usuarios sin plan | Sesiones más cortas que lo declarado | Divergencia IA–validador y cambio de dosis |

## 5. Conclusión

- **Recomendación: C2 como siguiente versión de política**, desactivada hasta aprobación, más las mejoras informativas sin cambio de plan (minutos aeróbicos y volumen por grupo como métricas).
- **E se mantiene como laboratorio experimental**, sin activar.
- **El modelo E completo no es necesario** para resolver V20 y V24.
