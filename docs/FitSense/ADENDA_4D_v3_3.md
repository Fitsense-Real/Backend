# Adenda 4D v3.3 · serie adicional del bíceps de relleno en PULL (`GAIN_MUSCLE`)

**Autorización:** orden del 31/10/2026, fase E. La extensión estaba autorizada solo si las pruebas demostraban que era necesaria y segura, y así fue: `campana_mvp/optimizacion_ia/VERIFICACION_GAIN_MUSCLE_CASA_60MIN.md`.

**Relación con 4D v3.2:** la complementa sin modificarla (`ADENDA_4D_v3_2.md`).

## Regla

| Colocación | 4D v3.2 | 4D v3.3 |
|---|---|---|
| RELLENO_ESTANDAR de `upper arms` (bíceps, V22) **en una sesión PULL** | 2×10 / 90 s | **2×10; 3×10 solo si sin esa serie la sesión no llega al mínimo de V20** |
| RELLENO_ESTANDAR en cualquier otro enfoque o grupo | 2×10 / 90 s | igual |
| Las demás clases | — | igual que 4D v3.2 |

- **Política:** `MvpDosePolicy.SERIES_MAX_RELLENO_BICEPS_PULL = 3` y `MvpDosePolicy.seriesMaxEn(dosis, enfoque, grupo)`.
  - No es una dosis de la clase: depende del enfoque y del grupo.
  - La tabla `dosis_por_clase` no cambia; el RELLENO_ESTANDAR sigue con `series_max = 2`.
- **Motor:** el mismo orden que 4D v3.2: composición → repaso de V20 (con la guarda de V15) → serie adicional de una en una, solo mientras falte y dentro del presupuesto de 4D §5.
  - La función que lo hace (`ampliarRelleno`) consulta `seriesMaxEn`.
- **V24:** la misma condición de necesidad: con 2×10 la sesión debe bajar del mínimo. Si no, la rechaza.
- **Prompt:** la regla 10 añade la excepción con las mismas condiciones.
- **Sin cambios:** V15, V20, los otros tres objetivos, la progresión (sigue desactivada fuera de las pruebas aisladas) y la reincorporación (devuelve la dosis base).

## Resultado (`RespaldoSesentaMinutosIT`, sin IA)

| GAIN_MUSCLE con 5–6 días, 60 min | 4D v3.1 | 4D v3.2 | 4D v3.3 |
|---|---:|---:|---:|
| Las 24 combinaciones | 0 | 16 | **24** |
