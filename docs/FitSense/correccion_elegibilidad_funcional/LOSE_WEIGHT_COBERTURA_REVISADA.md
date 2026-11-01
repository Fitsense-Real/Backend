# LOSE_WEIGHT: preferencia de cobertura revisada (LW-PREF-0.2)

**Estado:**
- **Desactivada** (`fitsense.generation.lose-weight.preferir-grupo-no-cubierto=false`).
- Versionada como `LW-PREF-0.2`. Con la opción encendida, la traza del plan registra `opciones.preferencia_cobertura_lw = "LW-PREF-0.2"`.
- El cardio no cambia.
- La métrica aeróbica sigue siendo **informativa** (`fitsense.metricas.aerobica.enabled=false`).

## 1. Qué fallaba en la 0.1

Fuente: `implementacion_controlada_c2d_v4c/LOSE_WEIGHT_PREFERENCIA_COBERTURA.csv`.

- La preferencia se aplicaba **siempre**. Cambiaba la composición de 82 planes **sin ninguna brecha**.
- Podía elegir un ejercicio que rompía V15: **2 planes válidos pasaban a inválidos**, y 5 empeoraban.

## 2. Corrección (`RuleBasedTrainingPlanGenerator.conPreferenciaLW`)

1. La semana se construye **siempre** igual que sin la opción.
2. Se mide la **brecha real**: grupos grandes (pecho, espalda, hombros, pierna) que tienen candidatos resistidos elegibles y ninguna colocación resistida en la semana. **Sin brecha, se devuelve el plan original sin cambios.**
3. Con brecha, se construye la alternativa con la preferencia. **Se acepta solo si**:
   - reduce la brecha;
   - el validador no le encuentra ningún problema nuevo: sus códigos ⊆ los del original, y no tiene más problemas. Esto excluye cualquier V15 nuevo.
4. Si no se cumple, **se conserva el original**.

## 3. Resultados (240 perfiles LOSE_WEIGHT de la matriz)

Fuente: `LOSE_WEIGHT_PREFERENCIA_LW02.csv`, política histórica.

| Métrica | LW-PREF-0.1 | **LW-PREF-0.2** |
|---|---|---|
| Planes sin brecha (84) cuya composición cambia | 82 | **0** |
| Válidos → inválidos | 2 (V15) | **0** |
| Planes que empeoran (más grupos a cero) | 5 | **0** |
| Planes con menos grupos a cero | 48 | **48** (34 sin ningún grupo a cero) |
| Inválidos → válidos | 6 | 6 |
| Planes con brecha sin alternativa aceptable (se conserva el original) | — | **91** de 156 |
| Planes cambiados (todos con brecha) | — | 65 |
| Minutos de trabajo de cardio | Sin cambios | **Sin cambios** (0 diferencias) |
| Series de fuerza totales | Sin cambios | **Sin cambios** (0 diferencias) |

**Notas:**
- Los 65 planes cambiados reducen la **brecha** (grupos con candidatos y sin colocación).
- 48 de ellos reducen además los «grupos a cero» con la métrica del laboratorio anterior, que cuenta los grupos entrenables del enfoque sin series.
- Los 17 restantes reducen la brecha sin cambiar esa métrica. Las dos métricas no son idénticas.
- Los 6 inválidos que pasan a válidos tienen menos problemas con la alternativa y ninguno nuevo. **No es un objetivo de la corrección**, se registra. Requieren la misma revisión que el resto.

## 4. Lo que no cambia

- Ni el cardio ni la métrica aeróbica.
- Ni el reparto de fuerza (el constructor reparte la misma fuerza: la preferencia cambia **qué** grupo recibe la colocación, no cuánta fuerza hay).
- Sigue sin resolver: los planes LOSE_WEIGHT con fuerza insuficiente **por diseño** (4A v6: la fuerza es secundaria). Es una decisión profesional y de producto, no un defecto técnico.

## 5. Activación

Requiere aprobación separada. Va a la muestra de revisión profesional: los LW de la muestra se presentan con y sin la preferencia.
