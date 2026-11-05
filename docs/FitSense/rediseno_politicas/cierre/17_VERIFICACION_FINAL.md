# 17 · Fase 8: regresión integral sin Replicate (código final `cc76bd1`)

**Entorno:**
- PostgreSQL 16 efímero (Testcontainers).
- `llamadas_proveedor=0` y `usd_medido=0.0000` en todas las corridas K20.
- Sin Neon.
- Evidencia: `evidencia/k20_cierre/*.tar.gz` (configuración, tablas, trazas y oráculos de cada corrida), `evidencia/k20_cierre/oraculos_f8.txt` y `evidencia/barridos/{HIST,REV2}_cierre/`.

**Commits del cierre:**

| Commit | Contenido |
|---|---|
| `f95e1f7` | F1/F2 |
| `44c8261` | F6 |
| `e2e8152` | F5 |
| `f3f7863`, `a3e231d`, `9c956a0` | F3/F4/F7 |
| `cc76bd1` | Corrección detectada por M29 |

## 1. Pruebas

| # | Batería | Resultado |
|---|---|---|
| 1 | Unitarias | **1.301 / 0 fallos** (1 omitida) |
| 2 | Integración completa (`verify -Pintegration`, política por defecto HIST) | **588 / 0 fallos** (43 omitidas: laboratorios opcionales), BUILD SUCCESS |
| 3 | Batería específica de progresión REV2 (incluida en 2) | `ProgresionRev2IT`, `PendientesProgresionRev2IT`, `CopiaSinIaRev2IT`, `LinajeIntegridadRev2IT`, `ReparacionLinajeRev2IT`, `RedisenoFatigaGmIT`: sin fallos |
| 4 | Barrido de 3.780 perfiles | **HIST: idéntico byte a byte** a la evidencia (3.641 válidos). **REV2: 3.776 válidos**, 4 V25 (los de fuerza de 1 día y 30 min) |
| 5 | Generación de referencia de los 4 objetivos | `GeneracionReferenciaIT` (en 2): OK |
| 6 | D y D′ en U, J, K, L y M | REV2: **D = D′** en las 5 cohortes |
| 7 | Adaptación con IA simulada | §3 |
| 8 | Negativas de seguridad (en 2) | `ProteccionPlanesPublicadosIT`, V8C/V24, `RedisenoAdaptacionTest` y los intentos inválidos del arnés: OK |
| 9 | Cambio de políticas | `TransicionesPoliticaIT` 6/6, `CatalogoInactivoHistIT`, `PoliticaV4CCicloIT` (en 2) |
| 10 | Días disponibles por nivel | `FrecuenciaPorNivelIT`: **216/216 con HIST y con REV2** |
| 11 | Adherencia sobre sesiones programadas | `AdherenciaSesionesProgramadasTest`; L05 y M30 con 4/4 = Aw 100 % |
| 12 | Regresión de la política histórica | Barrido idéntico y K20 HIST con las huellas congeladas: U `f9f67973…`, J `276d535d…`, K `54a5bda0…`, L `a1379048…` |

**Fuerza sin tiempo** (`FuerzaSinTiempoIT`, 3/3 con HIST y con REV2; incluye la vía de la fachada).

## 2. Barrido REV2 final (frente a `d4556b1`)

| Métrica | `d4556b1` | **Final** |
|---|---|---|
| Válidos | 3.776 | **3.776** |
| Grupos > 24 series/semana · PULL sin tracción · sesiones < 3 ejercicios · insuficientes | 0 · 0 · 0 · 0 | **0 · 0 · 0 · 0** |
| DOSIS_LIMITADA / DOSIS_OBJETIVO | 2.385 / 1.391 | **1.898 / 1.878** (sin el artefacto de brazos) |
| LOSE_WEIGHT sin cadera / sin rodilla | 448 / 184 | **117 / 94** (HIST: 57 / 42) |
| Planes con ejercicio de impacto moderado | 1.007 | **500** (sin los falsos positivos de «cuerda») |

## 3. K20: tres oráculos por corrida

Abreviaturas: **Hist** = oráculo histórico; **REV2** = oráculo REV2; **+DIAS** = oráculo con el contrato de días corregido.

| Corrida | Hist | REV2 | REV2+DIAS | HIST+DIAS | Huella |
|---|---|---|---|---|---|
| HIST D · U / J / K / L | 20 / 15 / 16 / 15 | 20 / 10 / 14 / 8 | — | 20 / 15 / 16 / **16** | f9f67973 / 276d535d / 54a5bda0 / a1379048 |
| HIST D · M | 22 · 8 FAIL | 12 · 18 | 15 · 15 | **30 / 30** | 0e010d07 |
| **REV2 D = D′ · U** | 10 · 10 (V20) | 20 | **20 / 20** | — | a662cb4d |
| **REV2 D = D′ · J** | 7 · 9 (V20) | 16 | **16 / 16** | — | 97228f04 |
| **REV2 D = D′ · K** | 10 · 6 | 16 | **16 / 16** | — | 73f28d97 |
| **REV2 D = D′ · L** | 10 · 6 | 15 · 1 (L05) | **16 / 16** | — | 83bd199a |
| **REV2 D = D′ · M** | 10 · 20 | 21 · 9 | **29 / 30** (M29) | — | 662e19cf |
| **REV2 IA sim. · U / J / K / L** | 10 / 7 / 9 / 9 | 20 / 16 / 16 / 15 | **20 / 16 / 16 / 16** | — | 84dfb6d2 / dafe6034 / 8ad60c9b / dc0ed02d |
| **REV2 IA sim. · M** | 9 · 21 | 21 · 9 | **29 / 30** (M29) | — | e0cc3474 |
| REV2 IA sim. · U con fallos del proveedor | 10 · 10 | 20 | **20 / 20** | — | 472de2bd |
| REV2 IA sim. con prompt V2.1 · M / U | — | — | **29 / 30 · 20 / 20** | — | e0cc3474 / 84dfb6d2 |

**Cambios de clasificación explicados:**

- **Hist frente a REV2.** Los FAIL que el histórico marca en REV2 son el piso de V20 (retirado en P-G1) y el contrato de días (S-MAX).
  - En M el histórico marca 20 FAIL: el piso de V20 y los principiantes con 5–7 días.
  - El oráculo REV2 marca 9 en M: 8 por «N sesiones y el perfil pide days_per_week» (principiantes con más de 4 días, que +DIAS resuelve) y M29.
- **REV2 frente a REV2 + DIAS.** El único cambio es el contrato de días: L05 y los principiantes de M con más de 4 días.
- **M29.** Es el resultado **esperado y fijado antes** en el manifiesto: sin plan con REV2.
  - En la 1.ª ejecución, por la fachada, llegaba el fallo técnico de V25.
  - Tras `cc76bd1`, llega `FUERZA_SIN_TIEMPO`.
  - El oráculo lo cuenta como FAIL («0 planes vigentes»), y así se informa.
- **Ningún resultado histórico se convierte en éxito retrospectivo.** Las evidencias R/R′ y U/J/K/L originales no se tocan; las reevaluaciones van en carpetas `oraculo_<versión>` separadas.

**Independencia:**
- U, J y K son cohortes de **regresión**.
- L y M dejan de ser independientes:
  - L se usó para investigar el contrato de días.
  - M destapó M29 y motivó `cc76bd1`. Su primera ejecución (`b6a4718`, sin la corrección) es la que vale como no vista; la posterior es de regresión.
- Para la evaluación real se propone una cohorte **N** nueva (`18` §4).

## 4. Pertinencia de las adaptaciones (IA simulada, REV2)

- **Sustituciones (U, J, K, L):** ninguna sube dificultad ni impacto, ni cambia de patrón o grupo (comprobación por script sobre las tablas).
- **Sesiones sin candidata válida:** 0.
- **Intento 1 inválido a propósito:** 100 % rechazado; el intento 2 es 100 % publicable.
- **Proveedor caído:** respaldo y continuidad correctos; el cortacircuitos se abre tras 3 fallos.
- **Coherencia prompt/factibilidad/V8C:** 0 discrepancias con el prompt por defecto y con V2.1.
