# Adenda 4D v3.4 · `GAIN_MUSCLE` de 61 a 75 minutos

**Fecha:** 04/11/2026
**Autorización:** «Apruebo la opción A como corrección candidata para 4D v3.4 […] sin modificar V20, V24, las dosis, los descansos ni el catálogo».
**Rama:** `eval/final-ieee-k20-v2` (candidata). No se fusiona en `generation` ni se despliega.

4D v2, v3, v3.1, v3.2 y v3.3 se conservan como referencia. Esta adenda solo cambia el **tope de colocaciones por encima de 60 minutos**.

## 1. El problema (demostrado)

Diagnóstico completo: `docs/FitSense/short_paper_ieee/k20_adaptacion_real/A1_DIAGNOSTICO_H05_R0548.md`.

**4D v3.1 fijó el tope en 7 colocaciones desde 60 minutos**, con la dosis estructural de 3×10 y 90 s. Lo derivó del piso de V20 a 60 minutos, que eran los minutos de entonces (30, 45 y 60). Después, P-01 admitió cualquier valor de 30 a 75 y 4D no se extendió.

| | Valor |
|---|---|
| Contenido máximo con 7 colocaciones | 300 + 7 × (90 + 180) + 6 × 60 = **2.550 s** |
| Piso de V20 a 61 min | ⌈61 × 60 × 0,70⌉ = 2.562 s |
| Piso de V20 a 75 min | 3.150 s |

Ninguna sesión de `GAIN_MUSCLE` de 61 a 75 minutos podía cumplir V20, con cualquier equipamiento:
- R0548 y H05 fallaban;
- en la matriz de 960 perfiles, 0 de 60 perfiles de 75 minutos eran válidos.

## 2. Cambio

Por encima de 60 minutos, el tope es el **mínimo número de colocaciones estructurales** (3×10, 90 s) cuyo contenido llega al piso de V20. Con el estimador de MVP-1.8:

```
contenido(n) = 300 + n · (3·10·3 + 2·90) + (n − 1) · 60 = 240 + 330 · n   (segundos)
piso(M)      = ⌈M · 60 · 70 / 100⌉
tope(M)      = 6                                si M < 60
             = 7                                si M = 60      (4D v3.1, sin cambios)
             = max(7, ⌈(piso(M) − 240) / 330⌉)  si M > 60
```

| Minutos | Piso V20 (s) | Tope | Contenido con el tope (s) | Contenido con uno menos (s) |
|---|---|---|---|---|
| 30–59 | — | 6 | — | — |
| 60 | 2.520 | **7** | 2.550 | 2.220 |
| 61 | 2.562 | **8** | 2.880 | 2.550 |
| 68 | 2.856 | **8** | 2.880 | 2.550 |
| 69 | 2.898 | **9** | 3.210 | 2.880 |
| 70 | 2.940 | **9** | 3.210 | 2.880 |
| 75 | 3.150 | **9** | 3.210 | 2.880 |

Es decir: **6** por debajo de 60 minutos, **7** con 60, **8** de 61 a 68 y **9** de 69 a 75.

> **Corrección registrada.** El primer borrador (A1 y el mensaje del commit del prototipo `720d058`) decía «8 de 61 a 69 y 9 de 70 a 75». Es un error de texto: a 69 minutos el piso es 2.898 s y 8 colocaciones dan 2.880 s. La fórmula del prototipo y la integrada son la misma y dan 9. Los resultados del prototipo no cambian, porque la matriz de 960 solo usa 30, 45, 60 y 75 minutos. Lo detectó `MvpDosePolicyTopeTest`.

### Por qué 9 cuando hay un límite general de 8

- `RuleBasedTrainingPlanGenerator.MAX_EXERCISES_PER_SESSION` (8) y `CapabilityPrescriptionPolicy.MAX_COLOC_DIA` (8, V24 extendida) son los topes de los objetivos **sin dosis fija**: `LOSE_WEIGHT`, `INCREASE_STRENGTH` y `GENERAL_FITNESS`.
- En `GAIN_MUSCLE` el tope **siempre lo ha fijado 4D**: 6 en v3 y 6/7 en v3.1.
  - V24 de `GAIN_MUSCLE` compara con `MvpDosePolicy.maxColocaciones`.
  - El motor de reglas toma ese mismo valor cuando la dosis es fija.
- La v3.4 solo deja que ese tope pase de 8 entre 69 y 75 minutos. Los demás objetivos siguen con 8.

### Lo que NO cambia

- **V20** (70 %), **V4** (+15 %) y **V24**: siguen exigiendo el tope de 4D, ahora coherente con V20.
- **Dosis y descansos:** 3×10 estructural, 3×13 en zona pequeña, 2×10/2×13 de relleno, 90 s, y la serie adicional de v3.2/v3.3 con su condición.
- **Presupuesto del 85 %** (4D v2 §5): 9 × 270 s = 2.430 s ≤ 3.825 s a 75 min.
- **Catálogo**, V25, V13, V15 y V23.
- **Objetivos distintos de `GAIN_MUSCLE`.**

### Dónde está

| Fichero | Cambio |
|---|---|
| `MvpDosePolicy.maxColocaciones` | la fórmula de arriba |
| `RuleBasedTrainingPlanGenerator` | en GM usa el tope de 4D, sin recortarlo a 8 |
| `PlanDraftValidator` (V24) | solo el texto: nombra «4D v3.4» por encima de 60 min y «4D v3.1» hasta 60, como antes |

El prompt de la IA (`constraints.max_colocaciones_dia`), V24 y el log `DOSIS_4D` leen la misma función, así que no hace falta tocarlos.

## 3. Pruebas

| Prueba | Qué comprueba |
|---|---|
| `MvpDosePolicyTopeTest` (unitaria) | Valor para **cada minuto de 30 a 75**, con 60, 61, 68, 69, 70 y 75 explícitos. Con el estimador real: el tope llega al piso de V20 y uno menos no llega. Presupuesto del 85 % y techo de V4. Dosis sin cambios |
| `Hipertrofia4Dv34IT` (integración) | Rejilla de 312 perfiles `GAIN_MUSCLE`: cada minuto de 30 a 75 × 3 equipamientos × 3 y 4 días, más principiante y avanzado en los bordes. Comprueba, recalculado, tope, dosis, V20, V4 y V13. Registra patrones, grupos, series semanales por grupo y grupos en días consecutivos |
| `GeneracionR0548V20IT` (integración) | **Antes** afirmaba el fallo; ahora exige el plan válido. El registro histórico del fallo se conserva en su javadoc |

Los resultados están en `k20_adaptacion_real/11_SEGURIDAD_Y_REGRESION.md` y en `k20_adaptacion_real/evidencia/v34/`.

## 4. Hallazgos de la rejilla de 30 a 75 minutos (no corregidos)

### 4.1 El mismo defecto entre 53 y 59 minutos (C-8, pendiente de tu decisión)

`Hipertrofia4Dv34IT` fijó un criterio antes de ejecutarse: gimnasio y casa con material generan en todos los minutos. **No se cumple.**
- Fallan los **42 perfiles intermedios de 53 a 59 minutos**: 14 por equipamiento, con 3 y con 4 días.
- Todos fallan por V20. El tope de 6 de 4D v3.1 da 2.220 s y el piso va de 2.226 s (53 min) a 2.478 s (59 min).

Es el mismo defecto de cobertura: 4D v3.1 solo derivó el 6 para 30 y 45 minutos. **No forma parte de lo aprobado**, así que no se corrige aquí:
- la prueba afirma el fallo, igual que antes `GeneracionR0548V20IT`;
- la propuesta es C-8 (`04_CAMBIOS_CANDIDATOS.md`).

Ni la matriz de 960 (30/45/60/75) ni U, ni H, tienen minutos de 53 a 59, y por eso no se veía.

### 4.2 Volumen semanal por grupo con 61–75 minutos (observación)

Las colocaciones añadidas cumplen V15: ningún grupo pasa de la mitad de una sesión (cuota máxima 0,50), y ningún grupo se trabaja en días consecutivos. Pero suben las series semanales de los grupos que las reciben:

| Grupo | Series semanales con 60 min y 3 días | Con 61–75 min y 3 días |
|---|---|---|
| Pecho | hasta 27 | hasta 36 |
| Piernas (muslo) | hasta 27 | hasta 36 |

Ninguna regla vigente limita el volumen semanal por grupo, así que **no es un incumplimiento**. Queda como limitación para la revisión profesional y no se cambia nada para corregirlo.

## 5. Limitaciones que no corrige

Siguen en los 960 perfiles, sin tocar sus datos (`k20_adaptacion_real/evidencia/v34/PERFILES_SIN_PLAN_VALIDO.csv`):
- V15 con `LOSE_WEIGHT` de 1 día y 45 min;
- principiantes de 75 min en `GENERAL_FITNESS` y `LOSE_WEIGHT`;
- peso corporal con 6–7 días.
