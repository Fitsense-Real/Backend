# 07 · Cohorte independiente L01–L16: resultados

**Condiciones de ejecución:**
- **Publicada antes de ejecutarla:** commit `8ed112a`. El CSV tiene SHA-256 `bc0baebf…`, el mismo que registra `00_configuracion.txt` de cada corrida.
- **Código:** cerrado en `30f1647`. **Nada se ajustó con L.**
- **Modos:** gratuitos. Sin Replicate (`llamadas_proveedor=0`, `usd_medido=0`).
- **Evidencia:** `evidencia/k20/{REV2_D_L,REV2_Dp_L,HIST_D_L,REV2_S_L}.tar.gz`.

## 1. Resultado global

| Corrida | Oráculo histórico | Oráculo REV2 | Huella |
|---|---|---|---|
| REV2 · D | 10 PASS · 6 FAIL (V20) | **15 PASS · 1 FAIL (L05)** | `6ed4296d…` |
| REV2 · D′ | 10 · 6 | **15 · 1 (L05)** | `6ed4296d…` (**idéntica a D**) |
| HIST · D (referencia) | 15 PASS · 1 FAIL (L05) | 8 PASS · 8 FAIL | `a1379048…` |
| REV2 · IA simulada | 9 · 7 (V20) | **15 · 1 (L05)** | `69672f33…` |

## 2. El fallo: L05

**Perfil:** GAIN_MUSCLE principiante, casa con peso corporal, **5 días** de 31 min.

**Qué falla:** el backend publica **4** sesiones, y el oráculo comprueba que haya tantas como `days_per_week` (5). Ocurre **igual con las dos políticas**.

**Causa:**
- Es una regla de producto **previa**, presente desde el primer commit: `PlanGenerationContext.MAX_SESSIONS_BEGINNER = 4`, el tope de sesiones de fuerza para principiantes (P3, división semanal).
- Ninguna cohorte anterior tenía un principiante con 5 días, así que es la primera vez que se observa en K20.

**Cómo se trata:**
- **No se cambia el oráculo ni el sistema.** Queda como resultado.
- Pendiente de decidir: ¿el oráculo debe conocer ese tope (es una regla documentada) o el producto debe decírselo al usuario? Ver L14 en `08`.

## 3. Lo que muestra L de la política nueva

| Aspecto | HIST | REV2 |
|---|---|---|
| 5 días con PULL (L04, L10, L15) | L15 (LW avanzado, casa con mancuernas): **PULL sin tracción** y 5 sesiones de **2 ejercicios** (QINS) | Las tres PULL **con tracción**; ninguna sesión por debajo de 3 ejercicios |
| Duración máxima | L04 y L10 (31 min): sesiones de 2.013–2.055 s y 1.896 s, por encima de los 1.860 s de M (V4M; HIST lo admite por su tolerancia del 15 %) | Todas ≤ M |
| Volumen | L02, L08 y L14 (principiantes): hasta 22 series/semana y 10 por sesión. L09: 12 por sesión | Dentro del techo |
| Adaptaciones con IA simulada | — | 19 sesiones APLICADAS. 13/13 intentos inválidos rechazados. **11 sustituciones, 0 incumplen P-A2** (comprobación independiente). Ninguna sesión no factible en la sonda |
| Fatiga en GM (L01) | — | `MANTENIMIENTO`: estructurales 3 → 2 series |

## 4. Observaciones que se informan, sin cambios

- **Adherencia 4/5 = 80 % (L04, L10):** decisión `NO_CHANGE`. Con el umbral de 80 % se conserva la semana. Es la regla vigente y el oráculo coincide.
- **L14** (INCREASE_STRENGTH principiante, casa, 4 días de 64 min): sesiones de unos **19 min** (30 % de M). Es «corta razonable» según la dosis, la misma limitación L6. Pendiente de revisión profesional.
- **L16 (DM con 2 días):** queda con una sola omisión `TOO_DIFFICULT`, como dice el manifiesto.

**Conclusión:**
- En una cohorte no vista, con 2 y 5 días y minutos nuevos, REV2 es **reproducible** (D = D′).
- No publica nada inválido según el oráculo más estricto.
- No produce PULL falsos ni volumen por encima del techo.
- Adapta todas las sesiones señaladas.
- El único FAIL viene de una regla previa, común a las dos políticas.
