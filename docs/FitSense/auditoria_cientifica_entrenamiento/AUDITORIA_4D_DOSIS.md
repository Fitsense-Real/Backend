# Auditoría de 4D: dosis, descansos, topes y tiempo

**Qué se audita:**
- las dosis de `MvpDosePolicy` (GAIN_MUSCLE) y `CapabilityPrescriptionPolicy` (los otros tres objetivos);
- los topes de V24;
- el presupuesto del 85 %;
- el estimador de duración (V14, V17);
- V4 y V20.

**Especificaciones:**
- «4D v2 §2/§5» y «4D v4»: **no disponibles**.
- Disponibles: `ADENDA_4D_v3_1.md`, `ADENDA_4D_v3_2.md`, `ADENDA_4D_v3_3.md` y el código.

## 1. Dosis por capacidad

| Código | Dosis vigente | Clase | Comentario |
|---|---|---|---|
| GM1 | Hipertrofia estructural 3×10, descanso 90 s | **B** | Dentro del rango de cargas con hipertrofia similar [S48]. 90 s es suficiente [S07]. 2–3 series por ejercicio son comparables a 4–6 [S50]. Su eficacia depende de la proximidad al fallo, que no se controla [S10, S11]. |
| GM2 | Zona pequeña 3×13 | **B** | Dentro de 9–15 RM [S48]. |
| GM3 | Relleno 2×10 / 2×13; hasta 3 series «solo para V20» | **C** | Compatible con la dosis-respuesta del volumen [S02, S15], pero el **motivo es el reloj**, no la fisiología. |
| IS1 | Fuerza en principiante 2×8, descanso 120 s | **B** | [S08, S17, S46] |
| IS2 | Fuerza intermedio/avanzado: nominal 3×5, descanso 150 s. **Se prescribe 3×6** porque el mínimo de repeticiones es 6 | **B** | 150 s superan los «> 2 min» para entrenados [S08]. El ACSM 2026 no encuentra efecto del descanso [S01]. Queda por debajo de los 3–5 min del ACSM 2009 con cargas pesadas [S06]. Sin control de carga o RIR **no es «alta intensidad»** [S39]. |
| GF2 | Resistencia muscular 2×15 / 3×18, descanso 45 s | **A** | ACSM 2009: más de 15 repeticiones, menos de 90 s [S06]. |
| GF3 | Isométricos 3×40 s, descanso 45 s | **C** | Los isométricos son eficaces [S52]; los 40 s son una decisión de ingeniería. |
| GF4 | Movilidad 2×30 s, pausa 15 s | **A** | 60 s por ejercicio, ≥ 2 d/semana [S34, S35]. |
| LW1 | Cardio 3×90 s (principiante) / 3×120 s, descanso 60 s | **B** como formato | Formato de intervalos válido [S29–S33]. La intensidad no se mide. |
| LW2 | **Dosis aeróbica total resultante** | **E** | Ver §2. |
| T9 | Principiantes ≤ 2 series (salvo GAIN_MUSCLE) | **B** | [S17, S46, S49] |
| T11 | Repeticiones 6–30 | **B** | [S48, S06]. Tiene una incoherencia interna con IS2. |

## 2. La única regla E: dosis aeróbica en LOSE_WEIGHT

En el laboratorio C1 (`MATRIZ_EXPERIMENTOS_REVISADA.csv`, 230 planes válidos de LOSE_WEIGHT):
- cardio de **9 a 120 min/semana**, con una media de 42;
- **ningún plan llega a 150 min/semana**;
- solo 12 llegan a 75 min.

Con 3 días y gimnasio, el cardio sale en 27 min/semana (principiante) y 36 min/semana (resto).

La referencia es:
- 150–300 min/semana moderados o 75–150 vigorosos [S05];
- unos 150 min/semana, con dosis-respuesta para el peso [S04];
- el HIIT de bajo volumen no mejora la composición corporal [S32].

**No es un riesgo de seguridad.** Es un riesgo de **eficacia** del objetivo principal.

Alternativas, a decidir:
1. **Solo medir y avisar.** Contabilizar los minutos aeróbicos equivalentes de la semana (vigoroso ×2) y mostrar si no se llega a 150. No cambia ningún plan.
2. **Contar la actividad registrada fuera de la app** («todo cuenta» [S05]). Requiere datos que hoy no existen.
3. **Alargar los bloques**, por ejemplo hacia 15–30 min por sesión. **Cambia la dosis 4D** y está **no autorizado** sin aprobación. Además consume tiempo de fuerza y puede reabrir V20 y V24.

Recomendación: la alternativa 1, como versión de política nueva y desactivada. La 3 solo tras revisión profesional.

## 3. Topes, presupuesto y tiempo

| Código | Regla | Clase | Comentario |
|---|---|---|---|
| GM5 | V24 GAIN_MUSCLE: 6 (< 60 min) / 7 (≥ 60 min) | **C** | Ninguna fuente fija el número de ejercicios. El 7 es un parche de V20 (adenda v3.1). |
| IS6 / LW6 | V24 capacidades: 8 | **C** | Es la constante técnica del motor convertida en regla. |
| GM6 | Presupuesto del 85 % | **C** | Sin evidencia sobre la variable. |
| T1 | V20: piso del 70 % de M | **C** | Nació en V12 «SIN CALIBRAR». No hay evidencia de una duración mínima [S19, S17]. |
| T2 | V4: techo M × 1,15 | **C** | Tolerancia de producto. **Incoherencia:** el perfil usa `floor` y el validador `Math.round` (30 → 34 frente a 35; 45 → 51 frente a 52). **No se modifica** (prohibido). |
| T3 | Calentamiento de 5 min | **B** | [S42, S19] |
| T4 | Transición de 60 s | **C** | Logística. |
| T5 | Descanso máximo de 180 s | **B** | [S07, S06, S08] |
| GM7 | 3 s por repetición | **B** | [S40] |

**Conflicto V20 × V24 × dosis fijas.** Con 7 colocaciones de 3×13/90, GAIN_MUSCLE llega como mucho a unos 2.739 s. El piso de 75 min es 3.150 s. En C1, GAIN_MUSCLE a 75 min tiene **0/60 planes**. No es un problema de las dosis, que son B, sino de que **un piso de tiempo sin base (C) choca con un tope sin base (C)**.

## 4. Dosis que se pueden conservar

**Todas.** No hay ninguna dosis 4D clasificada D o E por sí misma. La E es la **suma semanal** de cardio que resulta de aplicar LW1 en sesiones cortas.

Cambios opcionales, todos con aprobación:
- 180 s en básicos pesados de intermedio y avanzado (IS2/T5);
- nombrar la dosis de fuerza como «3×5 nominal, prescrita 3×6» o resolver la excepción (T10/T11);
- texto de la nota: «peso que deje 1–3 repeticiones en reserva» en vez de «alta intensidad».

## 5. Revisión de descansos

| Descanso | ¿Revisar? |
|---|---|
| 90 s en hipertrofia | No |
| 120 s en fuerza de principiante | No |
| 150 s en fuerza de intermedio y avanzado | **Revisión opcional**: permitir 180 s en básicos pesados [S06, S08]. No es obligatorio, porque el ACSM 2026 no encuentra efecto del descanso [S01]. |
| 45 s en resistencia muscular | No [S06] |
| 60 s entre bloques de cardio | No |
| 15 s en movilidad | No (C) |

Ningún descanso es E.
