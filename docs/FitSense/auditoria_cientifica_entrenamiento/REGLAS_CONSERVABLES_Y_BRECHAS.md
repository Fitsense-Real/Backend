# Reglas conservables y brechas

Fuente: `TRAZABILIDAD_REGLAS_EVIDENCIA.csv` (42 reglas).

**Clases**

| A | B | C | D | E |
|---:|---:|---:|---:|---:|
| 4 | 19 | 15 | 3 | 1 |

**Regla de decisión**
- A y B: conservar.
- C: conservar y documentar.
- D: análisis de sensibilidad, sin cambio automático.
- E: alternativas con pruebas.

## 1. Se conservan sin cambios (A/B, 23 reglas)

- **A (4):**
  - GF1 multicomponente;
  - GF2 resistencia muscular;
  - GF4 movilidad;
  - T6 progresión de carga hasta +10 %.
- **B (19):**
  - **GAIN_MUSCLE:** GM1 3×10/90; GM2 3×13; GM4 fuerza secundaria; GM7 3 s por repetición.
  - **INCREASE_STRENGTH:** IS1 2×8/120; IS2 3×5/150; IS4 hipertrofia secundaria; IS5 4 patrones con fuerza.
  - **LOSE_WEIGHT:** LW1 formato de bloques; LW3 cardio en cada sesión; LW4 fuerza secundaria.
  - **GENERAL_FITNESS:** GF6 4 patrones.
  - **Transversales:** T3 calentamiento de 5 min; T5 descanso máximo de 180 s; T7 D-P3; T9 principiantes ≤ 2 series; T11 repeticiones 6–30; T12 sesiones por semana; T13 P-01 30–75.

## 2. Se conservan y se documentan como decisiones de ingeniería (C, 15 reglas)

- **GAIN_MUSCLE:** GM3 rellenos; GM5 topes 6/7; GM6 presupuesto del 85 %.
- **INCREASE_STRENGTH:** IS3 mínimos por zona; IS6 tope de 8.
- **LOSE_WEIGHT:** LW5 sin 4 patrones; LW6 tope de 8.
- **GENERAL_FITNESS:** GF3 isométricos de 40 s; GF5 equilibrio oportunista.
- **Transversales:**
  - T1 V20 70 %;
  - T2 V4 ×1,15;
  - T4 transición de 60 s;
  - T8 V18 días consecutivos;
  - T10 mínimos de validación;
  - T14 banda del 60–125 %.

Ninguna de estas reglas debe presentarse como requisito científico. Se conservan porque **funcionan, son prudentes y su cambio no está justificado por evidencia**. La única excepción operativa es T1 a 66–75 min en GAIN_MUSCLE.

## 3. Evidencia insuficiente (D, 3 reglas): sensibilidad, sin cambio

| ID | Regla | Análisis de sensibilidad hecho | Resultado |
|---|---|---|---|
| IS7 | Control de intensidad con RPE de sesión | No se puede simular: no hay RIR en los datos | Requiere un dato nuevo (decisión D-05) |
| T15 | Volumen semanal por grupo (no existe) | Medido en C1/C2/C3 con el validador candidato E4 | C1: el 57 % de los planes tiene algún grupo grande por debajo del objetivo escalado de E, que **no es una norma** |
| T16 | Techo por grupo y sesión (no existe) | Medido con E2 (10 series) | C1: el 14 % de los planes lo supera. Solo hay un preprint: no se convierte en regla |

## 4. Potencialmente problemática (E, 1 regla): alternativas con pruebas

**LW2, dosis aeróbica total de LOSE_WEIGHT.**
- En C1 ningún plan llega a 150 min/semana; la media es 42.
- Alternativas: ver `AUDITORIA_4D_DOSIS.md` §2.
- Recomendación: medir y avisar como versión de política nueva; cambiar la dosis solo con revisión profesional (D-06).

## 5. Brechas que no son reglas

1. **No hay contabilidad de volumen por grupo y semana.** Ya existe como componente experimental (`VolumenSemanal`), fuera de la ruta de producción.
2. **No hay contabilidad de minutos aeróbicos equivalentes.**
3. **No se registra el RIR o RPE por serie.**
4. **Incoherencias internas:**
   - V4 `floor` frente a `round`;
   - 3×5 nominal frente al mínimo de 6 repeticiones.
5. **Especificaciones originales no disponibles:** 4A v5, 4C §5, 4D v2/v4 y §19–20. Su justificación no se puede auditar.
6. **Ningún texto completo leído** y **ninguna revisión profesional humana**.

## 6. Cuánto se conserva

- **Se conserva el 100 % de 4A y 4C y el 100 % de las dosis 4D.**
- De las 42 reglas, 41 se conservan tal cual. La E (LW2) es una propiedad emergente de la semana, no una dosis, y se puede tratar sin cambiar ninguna regla vigente (alternativa 1).
- El único cambio de comportamiento con valor operativo demostrado es el **piso de V20 acotado por la estructura (C2)**. Es una evolución de una regla C y requiere aprobación.
