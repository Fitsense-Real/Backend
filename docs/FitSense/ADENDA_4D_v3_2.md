# Adenda 4D v3.2 · serie adicional del relleno de zona pequeña (`GAIN_MUSCLE`)

**Decisión:** del responsable, 31/10/2026, opción B de `campana_mvp/HALLAZGOS_Y_CORRECCIONES_MVP.md` (H1).

**Alcance:**
- Solo `GAIN_MUSCLE`.
- No cambia V15, V20, V23, V24 (salvo esta regla), V25 ni ningún umbral.
- Los otros tres objetivos no se tocan.

## 1. El problema

**Síntoma:** GAIN_MUSCLE con 5 o 6 días de 60 min no tenía plan en ninguna de sus 24 combinaciones (campaña MVP, usuario U06).

**Causa:** con la dosis fija de 4D v3 y el tope de 7 colocaciones de 4D v3.1, ninguna sesión LEGS, PULL o LOWER_BODY que cumpla V15 llegaba a los 2520 s de V20.

**Por qué el motor no lo resolvía:** su «repaso de V20» cambiaba el relleno por estructurales para ganar tiempo, y así dejaba los días LEGS solo con `upper legs`, lo que rompía V15.

## 2. La regla

| Clase de dosis | 4D v3.1 | 4D v3.2 |
|---|---|---|
| ESTRUCTURAL_ESTANDAR | 3×10 / 90 s | igual |
| ESTRUCTURAL_ZONA_PEQUENA | 3×13 / 90 s | igual |
| RELLENO_ESTANDAR | 2×10 / 90 s | igual |
| **RELLENO_ZONA_PEQUENA** (`lower legs`, `waist`) | 2×13 / 90 s | **2×13; 3×13 solo si sin esa serie la sesión no llega al mínimo de V20** |

- **Política** (`MvpDosePolicy.SERIES_MAX_RELLENO_ZONA_PEQUENA = 3`): solo esa clase tiene `seriesMax` mayor que `series`.
- **Motor de reglas**, ruta de GAIN_MUSCLE:
  1. Primero, la composición de siempre (selección y relleno hasta el tope).
  2. Si la sesión sigue por debajo del mínimo, sube a 3×13 los rellenos de zona pequeña **de uno en uno**, solo mientras falte y dentro del presupuesto de 4D §5.
  3. Después, el repaso de siempre, que ya no hace un cambio que deje la sesión con menos de 2 grupos del enfoque (V15).
- **V24** acepta 3×13 en esa clase solo si, con 2×13, la sesión baja del mínimo de V20. Si sobra, la rechaza: *«la serie adicional de 4D v3.2 solo se admite si sin ella la sesión no llega al mínimo de V20»*.
  - No se comprueba donde V20 no aplica: sesiones autorizadas a acortarse (V8C) o sin mínimo.
  - El presupuesto de §5 cobra las series reales.
- **Prompt:** `constraints.dosis_por_clase` lleva `series_max`, y la regla 10 explica la excepción con las mismas condiciones.
- **Progresión y reincorporación:** sin cambios. La progresión ya admitía hasta 3 series en el relleno (`SERIES_MAX_AMPLIACION`). La reincorporación devuelve un relleno con su dosis base, 2×13, que es la conservadora.

## 3. Resultado (`RespaldoSesentaMinutosIT`, motor de reglas, sin IA)

| Grupo | Antes | Después |
|---|---:|---:|
| GAIN_MUSCLE, 5–6 días, 60 min | 0/24 | **16/24** |
| LOSE_WEIGHT, INCREASE_STRENGTH y GENERAL_FITNESS, 5–6 días, 60 min | 72/72 | 72/72 |

En las 16 combinaciones resueltas, la prueba independiente comprueba dos cosas:
- toda colocación con más series que su dosis es relleno de zona pequeña a 3×13;
- esa serie es necesaria: con 2×13 la sesión baja del mínimo.

**Quedan 8 sin resolver, y no se fuerzan:** GAIN_MUSCLE intermedio y avanzado **en casa**.
- **El día PULL:** 6 ejercicios de espalda a 3×10 y un curl de bíceps de relleno estándar a 2×10, que dan 2430 s de 2520.
- **Por qué no hay salida:** en casa no hay ejercicios estructurales de `upper arms`, y `upper arms` no es zona pequeña, así que la opción B no lo cubre.
- **Qué haría falta:** cerrarlo exigiría otra decisión. Por ejemplo, extender la serie adicional al relleno estándar del día PULL (2×10 → 3×10), con la misma condición de necesidad.
- **Reproducción:** con `-Dfitsense.pendientes=true`.
