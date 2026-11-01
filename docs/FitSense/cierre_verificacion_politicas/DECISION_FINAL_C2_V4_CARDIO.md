# Decisión final: C2, V4 y cardio (pendiente de aprobación)

**No se ha activado, fusionado ni desplegado nada.** Las piezas candidatas existen en `planning/domain/services/experimental/` (desactivadas; una prueba de aislamiento impide cablearlas) y se miden en el laboratorio opt-in `VerificacionPoliticasIT`.

## 1. V20 / C2

| Opción | Válidos (960) | GAIN_MUSCLE 75 | Planes alterados respecto de C1 | Sin plan | Evidencia de calidad |
|---|---:|---:|---:|---|---|
| C1 (V20 al 70 %) | 875 | 0/60 | — | 79 por V20 y 6 por V15 | Planes válidos sin cambios |
| C2-A (regenera hasta el 50 %) | 950 | 56/60 | 35 | 4 GM peso corporal y 6 por V15 | **Empobrece la semana**: regenera todas las sesiones con menos contenido |
| C2-B (hasta el 55 %) | 950 | 56/60 | 35 | Igual que C2-A | Igual que C2-A |
| C2-C (GM 55 %, resto 65 %) | **942** | 56/60 | 31 | 8 discrepantes, 4 GM peso corporal y 6 por V15 | Igual que C2-A, con menos recuperados |
| C2-E (otra composición sin bajar el piso) | 875 | 0/60 | 0 | Igual que C1 | Las 79 semanas ya estaban **saturadas**: no hay composición alternativa dentro de las reglas |
| **C2-D (mejor esfuerzo saturado)** | **954** | **60/60** | **0** | 6 por V15 | **El plan es el mismo que construye el motor al 70 %.** Solo deja de bloquear la sesión saturada |

### Recomendación: C2-D

1. No altera ningún plan: 960 de 960 idénticos a C1.
2. Recupera los 79 casos rechazados solo por V20.
3. Conserva más contenido que cualquier variante regenerada.
4. **No inventa un porcentaje:** el mínimo lo fija la saturación de la estructura (4A, 4C, 4D, V18, V24), no una cifra elegida para que pasen más planes.

### Condiciones estrictas (ya implementadas en la política candidata `PisoV20Acotado`)

- Actúa solo si **V20 es la única regla incumplida**. Con cualquier otro código no actúa.
- No actúa con una orden de volumen activa: D8 y ACORTAR ya eximen.
- No crea excepciones para V4, V8C, V15, V18, V23, V24, V25, P-02 ni P-03.
- No añade trabajo. Con dolor, P-03 nivel 2 sigue aplicando.

### Parámetros que requieren aprobación

| # | Parámetro | Propuesta | Alternativa |
|---|---|---|---|
| P1 | ¿Suelo absoluto adicional bajo C2-D? | **No**, a la espera de la revisión profesional | Si el profesional considera insuficientes las sesiones del 46 % (4 casos de GM con peso corporal y 6–7 días), fijar un suelo del 50 %: el resultado sería 950 válidos y esos 4 seguirían sin plan |
| P2 | Definición de «saturada» | Ninguna adición de un ejercicio con su dosis base aprobada ni ninguna serie más (hasta 3) es válida para el validador vigente | Añadir sustituciones (el constructor ya las prueba) |
| P3 | Mostrar la duración estimada | Obligatorio con C2-D (`CALIDAD_PLANES_RECUPERADOS.md` §5) | — |
| P4 | Versión | `PLAN-C2-0.1` en el plan nuevo; los planes existentes siguen como históricos | — |

### Si se conserva V20 al 70 %

- 79 perfiles válidos por P-01 siguen **sin plan**. Entre ellos, todos los GAIN_MUSCLE a 75 min y varias combinaciones de casa con 6–7 días.
- **Hallazgo del longitudinal:** tras una semana con dolor, P-02 excluye 7–8 ejercicios. En catálogos pequeños (peso corporal o mancuernas), la regeneración llega al 40–42 % de M, V20 la rechaza **todas las semanas** y la persona queda sin plan desde la semana 4 hasta la 10 (L5 y L6). Pasa con cualquier piso fijo del 55–70 %.
  - C2-D lo resolvería si la semana está saturada.
  - **No se ha podido comprobar en el ciclo real**, porque C2-D no está cableado.

## 2. V4

**Recomendación: V4-C (segundos estrictos) en una versión nueva (`PLAN-V4-0.1`)**, con el mismo límite en el generador.

| Motivo | Detalle |
|---|---|
| Exactitud | Aplica exactamente el 15 %. V4-B es más estricta a 30 y 45 min y más permisiva a 60 y 75 |
| Brecha de lo declarado | Hoy un borrador puede declarar 35 min para un contenido del 142 % de M y pasar V4 y V17 (`V4DuracionDeclaradaTest`) |
| Coherencia | V20 ya compara segundos sin redondear |
| Impacto | 0 de 456 sesiones históricas de la campaña; 0 de 875 planes válidos del laboratorio (V4-B rechazaría 6) |

**No retroactiva:** los planes sin versión se validan como HISTÓRICO.

## 3. Cardio de LOSE_WEIGHT

| Decisión | Recomendación |
|---|---|
| Aumentar el cardio programado | **No por ahora** (requiere datos de actividad y revisión profesional) |
| Métrica aeróbica informativa (programado, ejecutado, externo, intensidad) | **Sí**, como campo opcional del resumen semanal (`PROPUESTA_METRICAS_AEROBICAS.md`). Sin equivalencias si no hay intensidad |
| Aviso | «Tu plan incluye X minutos… lo que haces fuera de FitSense también cuenta». Sin juicio y sin promesa |
| Fuerza de LOSE_WEIGHT | Proponer al generador preferir el grupo grande no cubierto al colocar fuerza secundaria. Lo cerraría en 104 de 148 planes sin tocar 4A, 4C ni 4D. **Cambia la composición: requiere aprobación** |

## 4. Orden de aprobación sugerido

1. **Revisión profesional** con la muestra ciega: imprescindible antes de C2-D.
2. Versionado de políticas: migración en base efímera y luego en los entornos, con orden explícita.
3. V4-C.
4. C2-D, con la duración estimada visible.
5. Métrica aeróbica informativa.
6. Preferencia de grupo en la fuerza de LOSE_WEIGHT.
7. Abrir la red a los textos completos para cerrar la revisión científica.
