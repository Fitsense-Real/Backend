# 00 · Corrección integral de políticas tras la auditoría R/R′: resumen, matriz de decisiones y compuerta

**Datos de la entrega:**
- **Rama:** `investigacion/rediseno-politicas`.
- **Código cerrado:** `30f1647`.
- **Cohorte L:** publicada en `8ed112a` antes de ejecutarla.
- **Estado:** sin merge, sin despliegue, sin Replicate y sin Neon. El short paper y el PR #41 no se tocan.
- **Política por defecto:** `PLAN-HIST-1`, intacta (barrido y K20 idénticos byte a byte). La nueva, `PLAN-REV-2.0`, es **candidata**: solo se activa con `fitsense.planning.policy=PLAN-REV-2.0` **y** `allow-candidates=true`.

## Documentos

| # | Documento | Contenido |
|---|---|---|
| 01 | `01_DISENOS_DURACION.md` | P-G1: dos diseños (A dosis semanal, B llenado acotado) medidos en 3.780 perfiles; decisión A; P-G2 descartada |
| 02 | `02_EVIDENCIA_DOSIS.md` | Evidencia por tema, con etiquetas [SP] [ER] [HP] [PRO] [SEG] |
| 03 | `03_ORACULO_VERSIONADO.md` | Oráculo K20 por política: quita solo V20 y añade V4M, V26O, V27O y QINS |
| 04 | `04_CALIDAD_PLANES.md` | Calidad HIST frente a REV2 por objetivo y nivel; J03 ejercicio a ejercicio |
| 05 | `05_ADAPTACION_Y_PROGRESION.md` | P-A1, P-A2, P-P1, P-P2, H6, H7, H14 y reincorporación |
| 06 | `06_VERIFICACION.md` | Pruebas, reproducibilidad, K20 U/J/K, los 9 escenarios, P-H1 y regresión |
| 07 | `07_COHORTE_L.md` | Cohorte independiente L: resultados sin ajustar nada |
| 08 | `08_LIMITACIONES_Y_PROPUESTAS.md` | 13 limitaciones clasificadas y 10 propuestas pendientes |

## 1. Matriz de decisiones

| Punto | Orden | Qué se hizo | Evidencia | Estado |
|---|---|---|---|---|
| P-G1 duración | Aprobada con condiciones | M = máximo; V20 sin el 70 %; V4 ≤ 100 % de M, sin tolerancia; contenido por patrones y dosis semanal (diseño A, elegido frente a B tras medir los dos) | 3.776/3.780 con plan (HIST 3.641); 0 grupos > 24 series; 0 sesiones < 3 ejercicios | Hecho; cifras [PRO] |
| Volumen (caso de 36 series) | Sin techo arbitrario, con límites explícitos | V26: techo por nivel (12/20/24 por semana; 6/10 por sesión) y objetivo por objetivo y nivel. En semana derivada no obliga a recortar lo heredado, pero impide subir | HIST: hasta 48 series/semana; REV2: máximo 21 | Hecho; [PRO] |
| P-G2 (V15) | Pospuesta | No se activa. Con A ningún perfil falla por V15, y J03 genera sin seis piernas | `01` §5; `04` §5 | No hace falta |
| P-G3 PULL real | Sin PULL falsos | V27 + división superior/inferior sin tracción elegible + `asegurarTraccion` | 271 → 0 planes con PULL sin tracción | Hecho |
| P-C2 impacto | «Más fácil» nunca sube impacto | `ImpactoPolicy` (BAJO/MODERADO/ALTO/DESCONOCIDO); V8C rechaza la VARIANTE que sube impacto; complementos de menor impacto | 37/37 sustituciones sin subir impacto; alto impacto en LW/GF de ~1.200 a ~100–230 | Hecho; escala [PRO] |
| D-G3 | Comparar búsqueda acotada con semillas | Semillas derivadas k = 1..4 frente a reparación local: las semillas rescataron 23 en B y la reparación 0 | `01`; barrido con columna `dg3` | Semillas |
| P-A1 fatiga GM | Diseño controlado | Dosis de mantenimiento 3×10 → 2×10, solo en estructurales, solo con fatiga, solo REV2. Recuperación gradual sin restauración automática; avisos a las 2 y 3 semanas; dolor ≠ fatiga | U03, J01 y K01 antes no factibles ahora aplicados; `RedisenoFatigaGmIT` 2/2 | Hecho; tamaño [PRO] |
| P-A2 desmotivación | Con restricciones | Como máximo 1 sustitución; mismo patrón, slot, grupo y músculo; dificultad e impacto no superiores; sin cambio de dosis; solo sesiones señaladas | Sustitución factible en 20/20 sesiones frente a «−1 rep» en 6/20; 0 de 512 sustitutos más difíciles aceptados | Hecho |
| P-H1 arnés | Aprobada solo en pruebas | Reserva por fallida sin métricas; bloqueo ante timeout o estado desconocido; cancelación | `ControlPresupuestoK20Test` 7/7 | Hecho. **Replicate NO autorizado** |
| P-P1, P-P2, H6, H7 | Corregir | Arreglos de linaje y autorización en todas las políticas | `ReparacionLinajeIT` reproduce el fallo con el arreglo revertido | Hecho |
| H14 | Analizar | V24 revalida con el objetivo vigente | Prueba unitaria | Sin código |
| Reincorporación (varios retirados) | Analizar | Documentado (D-P8, «uno solo») | `05` §5.6 | Propuesta P5 |
| 100 % frente a 80 % | Documentar | Notas fechadas en los dos documentos de progresión | — | Hecho |
| Progresión | Apagada por defecto | Sigue apagada. Probada encendida (ProgresionIT 24/24 con REV2) y apagada (RedisenoFatigaGmIT) | `06` §4 | Hecho |
| Oráculo | No cambiar criterios para ocultar | Histórico intacto y versionado más estricto; se publican los dos | `03` | Hecho |
| Independencia | K ya se usó; cohorte L nueva | L fijada y publicada antes; no se ajustó nada con L | `07` | Hecho |

## 2. Antes y después (resumen)

| Métrica | HIST | REV2 |
|---|---|---|
| Barrido: perfiles con plan | 3.641 / 3.780 | **3.776 / 3.780** |
| Planes con un grupo > 24 series/semana | 446 (máximo 48) | **0** |
| Planes con PULL sin tracción | 271 | **0** |
| Planes con sesión de < 3 ejercicios | 42 | **0** |
| J03 | Sin plan | Plan válido, sin relleno |
| K20 IA simulada: escenarios no factibles en U, J y K | 9 | **0** |
| K20 oráculo versionado (D, U/J/K) | 20 · 10 de 16 · 14 de 16 | **20 · 16 · 16** |
| Reproducibilidad (D = D′; HIST idéntico) | — | Sí |

## 3. Compuerta antes de Replicate

| Condición | Estado | Evidencia |
|---|---|---|
| Sin regresiones críticas | ✅ | Regresión completa: 1.263 unitarias y 528 IT, 0 fallos. HIST idéntica |
| Ningún plan publicado inválido | ✅ | Oráculo versionado: U/J/K/L en D y en IA simulada; 79/79 intentos inválidos rechazados |
| J03 y equivalentes resueltos cuando es razonable | ✅ | J03 válido; 139 rescatados; 4 sin plan justificados (L1, pendiente de decisión) |
| Reducciones por fatiga funcionales y trazables | ✅ | `MANTENIMIENTO` en V8C/V24 y en el informe V32; avisos de persistencia |
| Sustituciones por desmotivación funcionales y pertinentes | ✅ | 37/37 cumplen P-A2 (comprobación independiente) |
| Sin PULL falsos | ✅ | 0 en 3.776 planes |
| Sin aumentos de volumen injustificados | ✅ | Techo V26; IA nunca sube dosis; semana derivada no sube |
| Seguridad y linaje preservados | ✅ | P-P2, H6, H7 corregidos; V1/V6/V10/V11 sin cambio; nada inactivo publicado |
| Progresión probada encendida y apagada | ✅ | `06` §4 (4 premisas de HIST no aplican a REV2, documentadas) |
| Reproducible | ✅ | Huellas D = D′; barridos idénticos |
| Limitaciones restantes clasificadas | ✅ | `08` (L1–L13, P1–P10) |

**Resultado de la compuerta:** **cumplida para pasar a la decisión sobre Replicate**, con las limitaciones de `08` declaradas.
- **No se ha consumido Replicate.**
- **No se ha autorizado** ninguna predicción: ni las 240 ni los USD 12,00.
- Cualquier campaña con IA real necesita una autorización nueva y explícita.

## 4. Lo que necesita una decisión del usuario o de un profesional

1. **L1:** IS 1 día de 30 min. Elegir una de las alternativas (a), (b) o (c), o dejarla como limitación.
2. **Cifras [PRO]:** dosis, techos, dosis de mantenimiento, aeróbico y escala de impacto (P2, P8, P10).
3. **Ritmo de recuperación tras fatiga** (P3) y reincorporación de varios retirados (P5).
4. **Si se adopta REV2 por defecto:** escribir variantes REV2 de las 4 IT de progresión cuyas premisas son de HIST (L13).
5. **Si se autoriza Replicate:** con qué política (HIST congelada o REV2 candidata), qué cohortes y qué presupuesto.
