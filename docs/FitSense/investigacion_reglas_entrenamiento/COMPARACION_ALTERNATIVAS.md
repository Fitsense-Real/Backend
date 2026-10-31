# Comparación de alternativas

## Datos usados

**Datos:** `MATRIZ_EXPERIMENTOS.csv`. Son 954 perfiles por 6 variantes (5.724 filas). De ellos, 912 están dentro de P-01 (30–75 min):
- 4 objetivos, 3 niveles y 3 equipos;
- 1/2/3/4/6/7 días;
- 30/45/60/75 min;
- 48 perfiles con restricciones de seguridad.

**Sensibilidad (solo laboratorio):** 90 y 120 min, más los CASO A pendientes de GENERAL_FITNESS a 90 min.

**Cómo se produjeron:**
- Las variantes de piso (A, D) usan el código de producción con la configuración de la base efímera.
- B, C y E son post-procesos de laboratorio con dosis aprobadas, validados con el validador vigente.
- Los planes que solo incumplen las reglas en estudio se marcan **EXPERIMENTAL**: no son válidos con la política vigente.

## 1. Resultados (912 perfiles dentro de P-01)

| | A · vigente | B · tope dinámico | C · sin tope fijo | D · V20 replanteada (piso 50 % / sin piso) | E · combinado |
|---|---|---|---|---|---|
| Qué cambia | Nada | Por encima de 6/7/8 solo si falta V20; ≤ 10 series por grupo y sesión y ≤ 20 por semana | Como B, sin el límite por sesión | Solo el piso de V20 | Sin V20 (tiempo como techo); mínimo de volumen semanal (GAIN_MUSCLE 10, resto 4 series en los grupos grandes); límites de B |
| Perfiles con plan | **830** | 863 (33 experimentales) | 863 | 902 / **912** | 896 (722 experimentales) |
| GAIN_MUSCLE a 75 min | **0/57** | 30/57 | 30/57 | 53/57 / 57/57 | **57/57** |
| % de M (media / mínimo) | 78 / 70 | 78 / 70 | 78 / 70 | 65 / 50 · 57 / 25 | 76 / 25 |
| Sesiones con > 11 series de un grupo | 14 % (hasta 21) | 14 % | 14 % | 7 % · 4 % | **4 %** |
| GAIN_MUSCLE con ≥ 10 series/sem en los 4 grupos grandes | 25/163 | 40/193 | 40/193 | 25/224 · **0/228** | **98/225** |
| LOSE_WEIGHT: fuerza por semana | 0–51 series | ídem | ídem | **0** sin piso | 16–24 |
| Cardio por semana (LOSE_WEIGHT) | 27–36 min | ídem | ídem | ídem | ídem (no se modeló el cardio) |
| Determinismo | 288/288 repeticiones idénticas | determinista | determinista | ídem A | determinista |
| Tiempo de cálculo | ~7 ms por semana | +< 1 ms | +< 1 ms | ~4 ms | +< 1 ms |
| Llamadas a la IA | 0 | 0 | 0 | 0 | 0 |

## 2. Valoración

### A · Mantener los máximos y V20 al 70 %

- **Ventajas:** sin cambios, probada y trazable.
- **Desventajas:**
  - GAIN_MUSCLE a 66–75 min nunca tiene plan, aunque P-01 lo admite (contradicción del producto);
  - 14 % de planes válidos con volumen redundante en una sesión;
  - la dosis semanal no está garantizada (solo 25 de 163 planes de GAIN_MUSCLE con ≥ 10 series en los cuatro grupos grandes);
  - el cardio de LOSE_WEIGHT queda muy lejos de la dosis semanal de la OMS.
- **Riesgo:** bajo técnico, alto funcional.
- **Complejidad:** ninguna.

### B · Tope dinámico

- **Ventajas:** rescata la mitad de GAIN_MUSCLE a 75 min y conserva la arquitectura.
- **Desventajas:** sigue llenando tiempo, no dosis, y hereda el relleno de A. El límite por grupo y sesión no corrige lo que ya trae el borrador vigente.
- **Complejidad:** baja. Un cálculo del tope por día con los límites de volumen.

### C · Sin máximo fijo universal

- **Ventajas:** igual que B en esta matriz.
- **Riesgos:** sin límite por sesión, el algoritmo puede concentrar volumen. En los experimentos no apareció porque el resto de reglas (V15, enfoque, tiempo) lo contuvo, **pero no hay garantía estructural**.
- **Conclusión:** quitar el tope sin sustituirlo por límites de volumen no es defendible.

### D · Revisar V20 sin tocar los máximos

- **Piso 50 %:** resuelve casi todo GAIN_MUSCLE a 75 min (53 de 57) y reduce el relleno a la mitad. Sigue siendo una cifra sin base científica.
- **Sin piso:** factible en todos los perfiles, pero **infradosifica**:
  - ningún plan de GAIN_MUSCLE con ≥ 10 series en los grupos grandes;
  - LOSE_WEIGHT sin fuerza;
  - sesiones del 25–57 % de M.
- **Lección:** el tiempo es un sustituto pobre de la dosis. Quitar el piso sin sustituto empeora la prescripción.

### E · Modelo combinado

- **Ventajas:**
  - el tiempo es un techo, como dice la evidencia (dosis mínima, sesiones cortas eficaces);
  - el contenido se exige por **volumen semanal por grupo** (la variable con dosis-respuesta);
  - se controla el volumen por sesión;
  - mejores resultados en las métricas de calidad: 4 % de redundancia y 98 de 225 planes de GAIN_MUSCLE con ≥ 10 series en los cuatro grupos grandes;
  - GAIN_MUSCLE a 75 min resuelto.
- **Desventajas:**
  - con 1–2 días, el mínimo semanal fijo de 10 series obliga a sesiones de 13–21 colocaciones (113–115 % de M): **el objetivo semanal tiene que escalar con los días** y hace falta un techo por sesión;
  - exige reescribir el núcleo del motor y de V20/V24;
  - el cardio sigue sin modelarse.
- **Complejidad:** alta (motor, validadores, prompt de la IA, adaptación, progresión y pruebas).

## 3. Matriz de decisión

Escala 1–5; 5 es lo mejor.

| Criterio | A | B | C | D (sin piso) | E |
|---|:-:|:-:|:-:|:-:|:-:|
| Fundamento científico | 2 | 2 | 2 | 2 | **4** |
| Seguridad (volumen, fatiga, recuperación) | 3 | 3 | 2 | 4 | **4** |
| Factibilidad | 3 | 3 | 3 | 5 | **5** |
| Personalización | 2 | 2 | 2 | 2 | **4** |
| Adherencia potencial (adecuación al tiempo) | 2 | 2 | 2 | 4 | **4** |
| Complejidad (5 = baja) | **5** | 4 | 4 | 4 | 2 |
| Trazabilidad | 4 | 3 | 2 | 4 | 4 |
| Riesgo de regresión (5 = bajo) | **5** | 4 | 3 | 3 | 2 |
| Rendimiento (cálculo, IA) | 5 | 5 | 5 | 5 | 5 |

No se ha favorecido la alternativa que más planes produce: D sin piso es la más factible y una de las peores en calidad de dosis.
