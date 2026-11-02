# LOSE_WEIGHT: preferencia de cobertura revisada (LW-PREF-0.2)

**Estado:**
- **Desactivada** (`fitsense.generation.lose-weight.preferir-grupo-no-cubierto=false`).
- Versionada como `LW-PREF-0.2`. Con la opción encendida, la traza del plan registra `opciones.preferencia_cobertura_lw = "LW-PREF-0.2"`.
- El cardio no cambia.
- La métrica aeróbica sigue siendo **informativa** (`fitsense.metricas.aerobica.enabled=false`).

## 1. Qué fallaba en la 0.1

Fuente: `implementacion_controlada_c2d_v4c/LOSE_WEIGHT_PREFERENCIA_COBERTURA.csv`.

- La preferencia se aplicaba **siempre**. Cambiaba la composición de 82 planes **sin ninguna brecha**.
- Podía elegir un ejercicio que rompía V15: **2 planes válidos pasaban a inválidos**. El informe anterior hablaba también de 5 empeoramientos: con el recuento corregido son 0 (§3).

## 2. Corrección (`RuleBasedTrainingPlanGenerator.conPreferenciaLW`)

1. La semana se construye **siempre** igual que sin la opción.
2. Se mide la **brecha real**: grupos grandes (pecho, espalda, hombros, pierna) que tienen candidatos resistidos elegibles y ninguna colocación resistida en la semana. **Sin brecha, se devuelve el plan original sin cambios.**
3. Con brecha, se construye la alternativa con la preferencia. **Se acepta solo si**:
   - reduce la brecha;
   - el validador no le encuentra ningún problema nuevo: sus códigos ⊆ los del original, y no tiene más problemas. Esto excluye cualquier V15 nuevo.
4. Si no se cumple, **se conserva el original**.

## 3. Resultados (240 perfiles LOSE_WEIGHT de la matriz)

Fuentes:
- `LOSE_WEIGHT_PREFERENCIA_LW02.csv` (0.2).
- `implementacion_controlada_c2d_v4c/LOSE_WEIGHT_PREFERENCIA_COBERTURA.csv` (0.1).

Ambas con la política histórica.

**Mismo denominador que el informe anterior:** de los 230 planes LOSE_WEIGHT válidos, 148 tienen algún grupo grande a cero y 82 ninguno.

| Métrica | LW-PREF-0.1 | **LW-PREF-0.2** |
|---|---|---|
| Planes sin grupos a cero (82) cuya composición cambia | **82** | **0** |
| De los 148 con grupos a cero: mejoran | 59 (34 sin ningún grupo a cero) | **59 (34)** |
| … quedan igual | 87 | 89 |
| … empeoran | 0 | **0** |
| … pasan a inválidos | **2** (V15) | **0** |
| Inválidos → válidos | 6 | 6 |
| Planes cambiados en total | 200 | **65** (todos reducen la brecha) |
| Minutos de trabajo de cardio | Sin cambios | **Sin cambios** (0 diferencias) |
| Series de fuerza totales | Sin cambios | **Sin cambios** (0 diferencias) |

**Corrección del informe anterior.**
- El informe anterior (`MEJORA_EXPERIMENTAL_LOSE_WEIGHT.md` y `CIERRE_IMPLEMENTACION_CONTROLADA.md`, pregunta 25) daba «48 de 148 mejoran» y «5 empeoran».
- Recalculado ahora con un recuento de grupos corregido («upper legs» se contaba como dos palabras), la 0.1 da **59 mejoras y 0 empeoramientos**.
- Los dos efectos negativos reales de la 0.1 se confirman:
  - **cambia los 82 planes sin grupos a cero**;
  - **2 válidos pasan a inválidos**.

**La 0.2 corrige los dos efectos.**
- Iguala las mejoras de la 0.1.
- Cambia 65 planes en lugar de 200: la 0.1 alteraba otros 135 sin mejorarlos.
- Los 6 inválidos que pasan a válidos tienen menos problemas con la alternativa y ninguno nuevo. **No es un objetivo de la corrección**, se registra. Requieren la misma revisión que el resto.

## 4. Lo que no cambia

- Ni el cardio ni la métrica aeróbica.
- Ni el reparto de fuerza (el constructor reparte la misma fuerza: la preferencia cambia **qué** grupo recibe la colocación, no cuánta fuerza hay).
- Sigue sin resolver: los planes LOSE_WEIGHT con fuerza insuficiente **por diseño** (4A v6: la fuerza es secundaria). Es una decisión profesional y de producto, no un defecto técnico.

## 5. Activación

Requiere aprobación separada. Va a la muestra de revisión profesional: los LW de la muestra se presentan con y sin la preferencia.
