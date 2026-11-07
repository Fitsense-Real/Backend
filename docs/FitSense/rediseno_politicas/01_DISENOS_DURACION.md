# 01 · P-G1: comparación de dos diseños de duración y decisión

**Orden:** P-G1 está aprobada como dirección de diseño. `sessionMinutes` es el máximo disponible; V20 deja de rechazar solo por no llegar al 70 %; V4 respeta el máximo; no se inventa volumen para llenar la sesión.

**Medición:** barrido de generación inicial sobre 3.780 perfiles (4 objetivos × 3 niveles × 3 equipamientos × 1–7 días × 15 duraciones), con el motor de reglas real y el validador de producción.
- Testcontainers, sin IA y sin Replicate.
- Evidencia en `evidencia/barridos/{HIST,A,B,A_v1,B_v1}`: los resultados están en `barrido_generacion.csv`, todos los planes ejercicio a ejercicio en `planes.csv.gz` y la comparación en `comparacion_HIST_A_B/`.
- Herramienta: `herramientas/comparar_barridos.py`.

## 1. Los dos diseños

Comparten tres cosas:
- V20 ya no rechaza por el 70 %.
- V4 se limita a ≤ 100 % de M, sin la tolerancia del 15 %.
- V26 es un validador nuevo: techo de series directas por grupo, en la semana y en cada sesión (`DosisSemanalPolicy`). Las cifras y la evidencia están en `02_EVIDENCIA_DOSIS.md`; todas son **pendientes de validación profesional**.

| | **A · Dosis semanal** (`PLAN-DUR-A-0.1`) | **B · Llenado acotado** (`PLAN-DUR-B-0.1`) |
|---|---|---|
| Qué decide el contenido | La cobertura de patrones (slots 4C) y una **dosis objetivo por semana**: series por grupo grande y minutos aeróbicos, repartidos entre las sesiones que entrenan cada grupo. | El generador sigue intentando llegar al **70 % de M** como objetivo blando, con el techo de volumen como filtro. |
| Qué hace el tiempo | Solo limita por arriba (V4). | Es el objetivo de llenado (blando) y el límite (V4). |
| Si sobra tiempo | La sesión acaba antes: no se inventa volumen. | Se sigue llenando hasta el 70 % o hasta el techo. |

## 2. Resultados (versiones corregidas; la v1 de cada uno tenía un defecto que se corrigió en los dos)

| Métrica | Histórica (PLAN-HIST-1) | **A** | B |
|---|---|---|---|
| Perfiles con plan | 3.641 | **3.776** | 3.073 |
| Sin plan | 139 (V20) | **4** (V25, ver §4) | 707 (V26 en 684) |
| Planes con algún grupo > 20 series/semana | 953 | **4** (avanzados; techo 24) | 85 |
| Planes con algún grupo > 24 series/semana | 446 (máximo 48) | **0** | 0 |
| Planes con alguna sesión de menos de 3 ejercicios | 42 | **0** | 42 |
| Planes válidos idénticos a los de la histórica | — | 137 de 3.637 | 1.835 de 3.035 |

**Por objetivo y nivel** (medianas; t = % del tiempo disponible usado; ej = ejercicios de la sesión más corta; s = series máx. por grupo y semana, mediana/máximo; ae = minutos de trabajo aeróbico por semana):

| Objetivo · nivel | Histórica | A | B |
|---|---|---|---|
| GAIN_MUSCLE principiante | t72 % ej7 s18/**36** | t58 % ej5 s12/12 | t77 % ej5 s12/12 |
| GAIN_MUSCLE intermedio | t73 % ej7 s18/**48** | t65 % ej5 s15/20 | t74 % ej6 s15/20 |
| GAIN_MUSCLE avanzado | t73 % ej7 s18/**48** | t66 % ej5 s15/21 | t74 % ej6 s15/24 |
| INCREASE_STRENGTH principiante | t72 % ej8 s18/**36** | **t37 %** ej4 s8/8 | t72 % ej6 s12/12 |
| INCREASE_STRENGTH intermedio | t74 % ej6 s18/42 | t56 % ej4 s12/18 | t74 % ej5,5 s15/20 |
| LOSE_WEIGHT principiante | t74 % ej7 s12/24 ae36 | t70 % ej4 s4/6 ae45 | t74 % ej6 s9/12 ae45 |
| LOSE_WEIGHT intermedio | t73 % ej5 s12/30 ae48 | **t92 %** ej6 s6/12 ae72 | t73 % ej5 s12/18 ae48 |
| GENERAL_FITNESS principiante | t72 % ej8 s14/22 ae18 | t61 % ej6 s6/10 ae31 | t72 % ej8 s11/12 ae18 |
| GENERAL_FITNESS intermedio | t77 % ej7 s15/30 ae30 | t84 % ej6 s9/15 ae42 | t77 % ej7 s12/20 ae24 |

**Lectura:**
- **La histórica genera volumen excesivo.** Principiantes con hasta 36 series semanales por grupo y avanzados con hasta 48. Lo produce el piso del 70 %, no una decisión de dosis.
- **B no funciona tal como está diseñado.** Llenar hacia el 70 % choca con el techo de volumen en cada fase del generador (subida de series, sustituciones por duración, repaso semanal). Resultado: 707 sin plan. Haría falta un parche por fase, y la causa (llenar minutos) seguiría ahí.
- **A cumple la dirección aprobada.**
  - El contenido sale de patrones y dosis; nunca supera el techo; ninguna sesión baja de 3 ejercicios.
  - Genera para 3.776 de 3.780 perfiles.
  - Usa menos tiempo cuando la dosis no lo necesita: por ejemplo, INCREASE_STRENGTH principiante usa un 37 % (unos 20 min con 4 ejercicios de fuerza de 2-3 series).
  - Usa casi todo el tiempo cuando la dosis sí lo pide: LOSE_WEIGHT intermedio llega al 92 %, por el aeróbico.

## 3. Decisión

**Se elige A** como diseño de duración de la versión integrada `PLAN-REV-2.0`.

**Justificación:**
- Es el único de los dos que no fabrica volumen para gastar minutos (requisito de la orden).
- Cumple el techo de volumen sin rechazos.
- Deja sin plan solo 4 perfiles, con una causa concreta.

B se conserva solo como versión de comparación (`PLAN-DUR-B-0.1`, candidata).

**Riesgos de A** (pendientes de validación profesional):
1. Sesiones cortas en INCREASE_STRENGTH principiante (~20 min). La evidencia (Iversen 2021) considera razonable una sesión de 3–5 multiarticulares con 6–12 series efectivas; FitSense las clasifica (`CalidadDosis`) y no las rechaza.
2. LOSE_WEIGHT intermedio y avanzado con mucho aeróbico: objetivo de 150 min/semana [SP]. Los minutos de trabajo aeróbico medianos (72/semana) siguen por debajo de 150, por el catálogo y por V18. El plan lo declara como `DOSIS_LIMITADA` en la traza.
3. Las cifras de dosis y techo son [HP][PRO].

## 4. Los 4 perfiles sin plan con A

Perfiles: INCREASE_STRENGTH intermedio y avanzado, gimnasio y casa con mancuernas, **1 día de 30 min**.

- V25 exige los 4 patrones de fuerza en la semana. Con la dosis de fuerza (3×6 con 150 s), 4 ejercicios suman unos 1.896 s, y el máximo es 1.800 s.
- Con la tolerancia histórica del 15 % sí cabían.
- Ninguna variante de semilla ni reparación local lo resuelve (D-G3).

**Clasificación:** limitación justificada. Con un único día de 30 minutos no caben los 4 patrones de fuerza con la dosis de fuerza.

**Alternativas, que necesitan decisión:**
- (a) V25 admite 3 de 4 patrones cuando el tiempo no da para más, con aviso;
- (b) una tolerancia de V4 que el usuario acepte de forma explícita;
- (c) en 1 día de 30 min, la dosis de fuerza con 2 series.

No se aplica ninguna sin aprobación.

## 5. P-G2 (V15), pospuesta: ¿sigue haciendo falta?

- Con A, **ningún perfil del barrido falla por V15**; con la histórica fallaban 15, ya corregidos con AUD-G1.
- J03 genera con A sin tocar V15. Su sesión LOWER tiene 1 ejercicio de pierna (2 series) y 2 de cardio, unos 23 min: corta, pero sin las 6 piernas de relleno.
- **Conclusión:** con el rediseño de duración, la regla de la mitad de V15 deja de producir contradicciones en la rejilla, y **no se propone modificarla**. P-G2 queda descartada mientras no aparezca un caso nuevo.
