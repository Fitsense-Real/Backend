# 14 · Fase 6: adaptación con IA simulada (REV2)

**Condiciones de las corridas:**
- **Commit:** `44c8261`. Las corridas posteriores con el código final están en `17_VERIFICACION_FINAL.md`.
- **Modo:** IA simulada (Mockito, sin red, `llamadas_proveedor=0`, `usd_medido=0`).
- **Oráculos:** histórico, REV2 y REV2 + DIAS-1.
- **Evidencia:** `evidencia/k20_cierre/`.

## 1. Defecto encontrado y corregido: el prompt ofrecía lo que V8C rechaza

La sonda de coherencia del arnés (`coherencia_v2.csv`) contrasta lo que el bloque V2/V2.1 dice a la IA con lo que el productor real acepta.

| Corrida | Antes (`30f1647`) | Después (`44c8261`) |
|---|---|---|
| U | **7** sustitutos listados y rechazados | **0** |
| J | **38** | **0** |
| K | **45** | **0** |
| L | **30** | **0** |
| HIST (control) | 0 | 0 |

**Causa:**
- En `VARIANTE_MAS_SENCILLA_O_CARGA`, la factibilidad previa y el prompt listaban sustitutos que **suben el impacto**: 36 de los 45 de K.
- V8C los rechaza con REV2 (P-C2).
- El filtro estricto solo se había añadido para la desmotivación (P-A2).

**Corrección:** `PermisosIntervencion.admiteComoSustituto` es ahora la **única regla**, y la usan el prompt, la factibilidad y V8C:
- P-A2: equivalente estricto;
- P-C2: en VARIANTE, sin subir impacto;
- con la histórica, como antes.

**Pruebas:**
- `AdmiteComoSustitutoTest`;
- `PromptHistoricoHuellaTest` (prompt histórico **idéntico byte a byte**);
- `PromptRev2Test` y `SessionFixPrompt*Test`: 50/50.

**Efecto en lo publicado:** ninguno. Las huellas de U, J, K y L con IA simulada son **idénticas** antes y después (`5825140e…`, `ef996d97…`, `7666ed3d…`, `69672f33…`): la IA simulada ya elegía opciones válidas, y ahora el prompt no ofrece las inválidas.

## 2. Resultados (REV2, IA simulada)

| Cohorte | Histórico | REV2 | **REV2 + DIAS-1** | Sesiones sin candidata válida | Intento 1 (inválido a propósito) | Intento 2 |
|---|---|---|---|---|---|---|
| U | 10 · 10 (solo V20) | 20 | **20 PASS** | 0 / 12 | 8/8 rechazados | 8/8 publicables |
| J | 7 · 9 (solo V20) | 16 | **16 PASS** | 0 / 22 | 16/16 rechazados | 16/16 |
| K | 9 · 7 (solo V20) | 16 | **16 PASS** | 0 / 22 | 16/16 rechazados | 16/16 |
| L | 9 · 7 (V20) | 15 · 1 (L05) | **16 PASS** | 0 / 19 | 13/13 rechazados | 13/13 |

**Intento 1 inválido a propósito**, rotando tres clases (53 en total):
- subir una serie (dosis no autorizada);
- tocar una sesión no señalada;
- un sustituto más difícil.

**Ninguno se publicó.** El impacto superior lo cubre `RedisenoAdaptacionTest.varianteNoSubeImpacto`, y la sonda no validó **ningún** sustituto que suba el impacto.

## 3. Los nueve escenarios antes bloqueados (U03, U04, J01, J09, J13, K01, K09, K10, K13)

Siguen resolviéndose igual que en `06_VERIFICACION.md` §3.1:
- mantenimiento P-A1 en U03, J01 y K01;
- una sustitución P-A2 en los otros seis.

Con HIST, los 9 siguen no factibles (V20 y V24), y así se conserva como control.

**Pertinencia (no basta con que sea publicable):**

| Comprobación | Resultado |
|---|---|
| Sustituciones de U, J, K y L | **48**: 37 de U/J/K + 11 de L |
| Mismo patrón y mismo grupo | Todas (verificación independiente sobre las tablas exportadas) |
| Dificultad e impacto | Ninguna sube |
| Fatiga en GAIN_MUSCLE | Solo mantenimiento (−1 serie en estructurales de 3 series) |
| Desmotivación | Como máximo una sustitución, sin cambio de dosis |
| Sustitución frente a «−1 rep» | La sustitución es factible en 20/20 sesiones de desmotivación; «−1 rep» en 6/20. Ninguno de los 512 sustitutos más difíciles se valida |

## 4. Fallos del proveedor simulado (nuevo en el arnés)

**Corrida:** `-Dfitsense.evaluacion.k20.simulada.fallos=U03,U04,U08,U09,U13`. Se registra en `00_configuracion.txt`; si la propiedad no se pasa, ninguna corrida anterior cambia.

**Fallo:** la IA simulada lanza `AiProviderUnavailableException`, como un proveedor caído.

| Comprobación | Resultado |
|---|---|
| Publicación | Los 5 usuarios: **RESPALDO** (copia segura, `RESPALDO-COPIA`). Ninguno queda sin semana |
| Cortacircuitos | 3 fallos reales; después se abre y los otros 2 reciben el respaldo **sin llamar** al proveedor. Es el comportamiento diseñado |
| Continuidad (semana 3) | OK en los 5 |
| Oráculo REV2 + DIAS-1 | **20/20 PASS** (huella `c3fe7e1d…`) |

## 5. Coherencia prompt · factibilidad · V8C · oráculo

| Regla | Prompt | Factibilidad previa | V8C | Oráculo |
|---|---|---|---|---|
| P-A2: una sustitución estricta | Sí | Sí | Sí | Comprobado por script (patrón, grupo, dificultad, impacto) |
| P-C2: VARIANTE sin subir impacto | **Sí (desde `44c8261`)** | **Sí (desde `44c8261`)** | Sí | Comprobado por script |
| P-A1: mantenimiento en GAIN_MUSCLE ante fatiga | Sí (solo 3×10 → 2×10) | Sí | Sí (`MANTENIMIENTO`) | V24 lo valida; el oráculo comprueba que no sube dosis |
| La IA nunca sube la dosis | Sí | Sí | Sí | Sí |
| Sesiones no señaladas intactas | Sí | — | Sí | Sí |

**Estado: COMPROBADO**, con la corrección `44c8261`. Queda una limitación conocida: el oráculo no valida por sí mismo la dosis de mantenimiento; se apoya en el registro de V24 y V8C.
