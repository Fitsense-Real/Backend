# Adenda 4D v3.1 · `GAIN_MUSCLE` a 30, 45 y 60 minutos

**Fecha:** 24/10/2026 · **Autorización:** «corrección puntual y versionada de 4D v3, sin debilitar las validaciones».

4D v2 y la adenda v3 se conservan sin cambios como referencia. `MvpDosePolicy.MAX_COLOC_DIA = 6` sigue declarado. Esta adenda solo añade lo que se lista aquí.

## 1. El problema (comprobado en el código anterior)

`GAIN_MUSCLE` no generaba semana a 30 ni a 60 minutos, en ningún nivel: 24 de 36 perfiles del barrido.

- **30 min:** el motor elegía 5 colocaciones sin mirar el presupuesto de 4D §5. El día costaba 30,5 min y el presupuesto es 25,5 (85 %), así que V24 lo rechazaba.
- **60 min:** con el tope de 6 colocaciones de 4D, el contenido daba 37 min y el mínimo de V20 es 42 (70 %). Con 4D no se pueden subir series, así que ninguna semana era válida.

## 2. Cambios

| | 4D v3 | 4D v3.1 |
|---|---|---|
| Tope de colocaciones | 6 | **6 hasta 59 min; 7 desde 60 min** (`MvpDosePolicy.maxColocaciones`) |
| Presupuesto §5 (85 % de la sesión) | lo exige V24 | igual, y ahora el motor lo **respeta desde la selección**: con sesiones cortas pone menos colocaciones y los slots que no caben un día los cubre otro de la semana |
| Dosis por clase | 3×10 / 2×10, +3 en zona pequeña, 90 s | **sin cambios** |
| V20 (70 %) | exigido | **sin cambios**, no se relaja |
| Qué acredita hipertrofia | cualquier patrón, incluso sin capacidad | **solo** un ejercicio estructural o resistido, de repeticiones y verificado |

**Qué deja de acreditar hipertrofia:**
- movilidad, cardio, equilibrio;
- patrón `OTHER` o sin patrón;
- ejercicios de duración, porque 4D v3 no tiene dosis de hipertrofia para ellos;
- ejercicios en revisión de clasificación.

El motor ya no los elige, y V24 los rechaza en la IA y en el motor por igual. Antes, 902, 1257 y 1070 podían ocupar una colocación de la sesión de hipertrofia.

**Alcance de la regla de evidencia:** va bajo `fitsense.capability.enforcement`. La validación estricta de la adaptación P2-D no la aplica, porque conserva semanas publicadas con 4D v3. El nuevo tope de 7 sí rige en todas partes: solo admite más que antes.

## 3. Resultado

**Barrido de 144 perfiles:** `GAIN_MUSCLE` pasa de 12 a **36 de 36 COMPLETO**, sin un solo fallo en los demás objetivos. Repetido con tres juegos de semillas (432 generaciones) y con comprobación de seguridad y acreditación de cada colocación publicada: **0 fallos**.

**Lo que comprueba `GeneracionPorObjetivoIT` › `Hipertrofia` (30, 45 y 60 min):**
- tope y presupuesto respetados en cada día;
- los cuatro slots presentes en la semana;
- ninguna colocación de duración ni sin evidencia de hipertrofia.

## 4. Lo que no cambia y queda pendiente

- **La fuerza secundaria de `GAIN_MUSCLE`** sigue sin asignarse: el 25 % de 4D v2 §6 aún no tiene denominador.
- **Si alguna combinación no tiene semana válida**, el motor falla con un `NO_FACTIBLE:` y los motivos de cada validación, no con un error genérico. En el barrido no apareció ninguna.

## 5. Corrección del log de DOSIS_4D (plan 60, 26/10/2026)

El plan 60 (`GAIN_MUSCLE`, 60 min, siete colocaciones por sesión) se publicó con un log que decía «0 incumplimientos · máximo 6 colocaciones/día».
- **V24 estaba bien:** exige `MvpDosePolicy.maxColocaciones(minutos)`, que es 7 a 60 minutos, y en ENFORCE rechaza la propuesta.
- **El prompt estaba bien:** `constraints.max_colocaciones_dia` llevaba 7 y el punto 10 de los principios dice que no se pase de ese número.
- **El texto del log era el error:** `describeTabla()` imprimía la referencia congelada de 4D v3 (`MAX_COLOC_DIA` = 6). Ahora `describeTabla(minutos)` imprime el mismo tope que exige V24.

`Plan60HipertrofiaIT` reproduce las 21 colocaciones con el perfil del usuario 27 y el catálogo real:
- a 60 minutos el plan es válido, y con ocho en una sesión V24 lo rechaza;
- a 45 minutos el mismo plan se rechaza en las tres sesiones (máximo 6), y con seis por sesión se acepta.
