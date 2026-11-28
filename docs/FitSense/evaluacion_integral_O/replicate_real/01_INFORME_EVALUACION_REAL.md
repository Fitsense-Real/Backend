# Evaluación integral O01–O20 con Replicate REAL · 01 · Informe

| Elemento | Valor |
|---|---|
| Versión evaluada | A `f1c6c02` (`src/main` sin cambios): V46, PLAN-REV-2.0, P15-C apagado, aviso E2-a |
| Cohorte | O01–O20 congelada (`9b996825…`) y escenario complementario O11-F35 (`1720227e…`) |
| Arnés | `cf3ac39`, congelación commiteada **antes** de la primera llamada pagada (`00_CONGELACION_Y_COMPROBACIONES.md`) |
| Modelo | Replicate `openai/gpt-5-structured`, `gpt-5-mini`, razonamiento `medium`, prompt V2.1. Versión `9f4cd9ec…` **antes de cada corrida y después de la última**, sin cambios (`evidencia/versiones.txt`) |
| Corridas | **R_O** (principal) y **R′_O** (reproducibilidad), cada una con bloque G (O01–O14), bloque P (O15–O20) y O11-F35. El 06/11/2026, de 03:40 a 03:58 UTC |
| Coste | **20 predicciones, USD 0,3033** (límite: 168 y USD 8,40). Incluye la reserva de USD 0,05 cargada por la predicción fallida de O12 |
| Respuestas | **Todas del modelo real.** Ninguna simulada ni precargada (la sonda de factibilidad solo mide) |
| Adherencia | **Simulada por guion.** Los resultados no se atribuyen a participantes humanos |
| Oráculo | v1.2 congelado (resultado original) y v1.3 (corrige el defecto del instrumento D-OR-3; ver `02_INCIDENCIAS.md`) |

## 1. Resultado global

### Diez aspectos (Tabla 9)

| Evaluación | APROBADO | FALLO | NO APLICA | REQUIERE REVISIÓN | Aprobación técnica |
|---|---:|---:|---:|---:|---|
| IA simulada · repetición S_O · v1.1 (antecedente, `9ae4c49`) | 142 | 0 | 43 | 15 | 142/142 = 100 % |
| **Replicate real · R_O · v1.2 (congelado)** | **140** | **0** | 45 | 15 | **140/140 = 100 %** |
| Replicate real · R_O · v1.3 | 140 | 0 | 45 | 15 | 140/140 = 100 % |
| **Replicate real · R′_O · v1.2 (congelado)** | **140** | **1** | 45 | 14 | **140/141 = 99,3 %** |
| Replicate real · R′_O · v1.3 | 141 | 0 | 45 | 14 | 141/141 = 100 % |

**Sobre el FALLO de R′_O** (O12·A5, dolor de sesión):
- Es un defecto del **oráculo**, no del sistema: juzgaba ante dolor con una regla más estricta que la aprobada en el backend.
- Se conserva el resultado v1.2. La corrección v1.3 cambia solo esa celda y lo demuestran 23 pruebas.

**Sobre los NO APLICA** (45 frente a 43 con la IA simulada): el modelo real no retiró ningún relleno en O19 en ninguna corrida, ni en O18 en R_O. Sin retirada no hay reincorporación que evaluar (A8).

**Las 14–15 REQUIERE REVISIÓN** son observaciones del catálogo (E2, E3, E4/E7), separadas de las aprobaciones técnicas.

### Estados por aspecto (Tabla 10, v1.3; AP/FA/NA/RR)

| Aspecto | IA simulada | R_O | R′_O |
|---|---|---|---|
| A1 Generación | 6/0/0/14 | 6/0/0/14 | 6/0/0/14 |
| A2 Personalización y restricciones | 20/0/0/0 | 20/0/0/0 | 20/0/0/0 |
| A3 Registro | 19/0/1/0 | 19/0/1/0 | 19/0/1/0 |
| A4 Adherencia | 19/0/1/0 | 19/0/1/0 | 19/0/1/0 |
| A5 Adaptación con IA | 9/0/10/1 | 9/0/10/1 | 10/0/10/0 |
| A6 Mantenimiento | 19/0/1/0 | 19/0/1/0 | 19/0/1/0 |
| A7 Progresión y recuperación | 8/0/12/0 | 8/0/12/0 | 7/0/13/0 |
| A8 Reincorporación | 4/0/16/0 | 2/0/18/0 | 3/0/17/0 |
| A9 Continuidad | 19/0/1/0 | 19/0/1/0 | 19/0/1/0 |
| A10 Seguridad e integridad | 19/0/1/0 | 19/0/1/0 | 19/0/1/0 |

## 2. Generación, O11 y O11-F35

**Cohorte original:**
- **19 planes válidos y 1 inviabilidad esperada**, en las dos corridas.
- O11 informa `FUERZA_SIN_TIEMPO`: «con 1 sesión de 30 minutos no caben los 4 patrones de fuerza […] (se necesitan unos 32 minutos por sesión)».
- No es un plan generado ni un fallo.

**Escenario complementario O11-F35** (O11 con 35 min; se informa aparte y no sustituye a O11):
- Plan válido de 1 sesión FULL_BODY de 32 min, con los 4 patrones: 1195 3×6, 125 3×6, 1100 3×6 y 119 3×6.
- Aviso DOSIS_LIMITADA (espalda, pecho y hombros por debajo de la dosis orientativa).
- 4 semanas al 100 %, sin adaptaciones y sin llamadas a la IA.
- Oráculo v1.3: todo APROBADO o NO APLICA, salvo A1 REQUIERE REVISIÓN, porque 1195 es un curl nórdico asistido (catálogo E3).
- **No se relajó V24, V25 ni ninguna validación:** el plan sale del mismo código, con 5 minutos más de perfil.

## 3. Ejecución, adherencia y decisiones

Guiones idénticos a la evaluación simulada.

| Medida | R_O | R′_O |
|---|---|---|
| Sesiones S1–S4 (19 perfiles) | 256: 239 completas, 6 parciales, 10 omitidas, 1 sin registro | Igual |
| Adherencias recalculadas por el oráculo frente a las guardadas | Iguales en todas las semanas | Iguales en todas las semanas |
| **Decisión del backend = decisión recalculada** | **124/124 ciclos** (incluidos horizontes) | **120/120** |
| Decisiones C1–C4 | 63 NO_CHANGE, 11 ADAPT_LOCALIZED, 2 ADAPT_EXPANDED | Igual |
| V32 C1–C4 | 63 SIN_ORDEN, 8 APLICADA, 4 SIN_EFECTO, 1 RESPALDO | 63 SIN_ORDEN, 10 APLICADA, 3 SIN_EFECTO |

## 4. Adaptación con Replicate (Tablas 6 y 7)

| | R_O | R′_O |
|---|---|---|
| Ciclos con orden de IA | 10 | 10 |
| Predicciones · tokens (entrada / salida) | 10 · 281.633 / 22.784 | 10 · 334.826 / 26.838 |
| USD · latencia mediana (máx.) | 0,166 · 47,8 s (63,1) | 0,137 · 38,2 s (59,9) |
| **Aceptadas en el intento 1** | **8** | **10** |
| Aceptadas tras reintento | 0 | 0 |
| **Propuestas rechazadas por los validadores** | **0** | **0** |
| SIN_EFECTO propuesto por la IA | 1 (O18, **evitable**: había una retirada válida) | 0 |
| Respaldo determinista | 1 (O12, **tras error del proveedor**) | 0 |
| **Adaptación de la IA publicada** | **8/10** | **10/10** |
| Pertinencia de las sesiones con orden de IA (v1.3) | 10 ACC, 2 CONS-J (O05 sin causa registrada; O18 SIN_EFECTO evitable), **0 INC** | 10 ACC, 1 ALT (O12), 1 CONS-J (O05), **0 INC** |

**Lo que cumplen todas las adaptaciones publicadas** (comprobado por el oráculo sobre el plan publicado):
- La propuesta corresponde a la intervención ordenada.
- Sin más trabajo ni dosis mayores.
- Sustitutos del mismo patrón y grupo (fuera del dolor) y nunca más difíciles.
- El ejercicio con dolor desaparece y no vuelve (O06: 1117; O20: 536).
- Las sesiones no señaladas quedan idénticas (A6).
- Techo de volumen y prohibidos respetados (A10).
- **El backend nunca publicó una propuesta que no hubiera juzgado publicable** (R-1).
- **Mantenimiento:** las conservaciones sin IA (O02 y O16 por falta de tiempo sin minutos; O10 por causa no registrada) se mantienen idénticas.

## 5. Progresión, recuperación y reincorporación

| | R_O | R′_O |
|---|---|---|
| PROGRESION | 32 | 32 |
| RECUPERACION | 3: O03 (704), O14 (253), O17 (100) | 2: O14 (253), O17 (100) |
| REINCORPORACION | 0 | 1: **O18**, ejercicio 1302 retirado en C1, reincorporado en C22 |
| Todos los pasos ≤ techo de volumen; ≤ 1 por ciclo; ninguna PROGRESION sin dos semanas al 100 % | Sí | Sí |

**Diferencias explicadas** (`02_INCIDENCIAS.md`):
- **Sin reincorporación en O19** (ninguna corrida) **ni en O18** (R_O): el modelo no retiró relleno. En O19 redujo dosis; en O18-R_O devolvió SIN_EFECTO.
- **O03 en R′_O sin recuperación:** el modelo redujo los 4 ejercicios. Con 8 series, el paso mínimo (+1 serie = 12,5 %) supera `recovery_step_pct` (10 %). Es una regla aprobada que puede bloquear la recuperación; hay una propuesta pendiente de tu decisión.

## 6. Comparación con la IA simulada (Tabla `analisis/comparacion_simulada_vs_real.csv`)

| | IA simulada (evaluación cerrada `9ae4c49`) | Replicate real (R_O / R′_O) |
|---|---|---|
| Respuesta | La escribe el laboratorio: intento 1 inválido a propósito (siempre rechazado) e intento 2 con la mejor candidata válida de la sonda | Del modelo, sin guía del laboratorio |
| Llamadas por ciclo con IA | 2 (por diseño) | 1 (ningún reintento necesario) |
| Rechazos | 10 por corrida (intencionados) | **0** |
| Estilo de las adaptaciones | Un cambio mínimo: 1 reducción o 1 sustitución | **Más amplio y más conservador:** más reducciones y retiradas. Trabajo de la sesión adaptada: O04 −33 % (simulada: 0 %), O06 −62 % (−2 %), O14 −25 % (−12 %), O17 −21/−26 % (−7,5 %), O19 −33/−31 % (−41 %) |
| Ante dolor | Sustituir por un ejercicio del mismo patrón | Retirar el ejercicio con dolor (O06, O20). En O12 (R′_O), rehacer la sesión con menos volumen |
| ACORTAR (falta de tiempo) | Retirar el relleno | O19: bajar dosis (las dos corridas). O18: SIN_EFECTO (R_O) o retirar 1302 (R′_O) |
| Diez aspectos | 142/142 | 140/140 y 140/141 (v1.2) → 141/141 (v1.3) |

**Ninguna diferencia** es un fallo del sistema. Las respuestas reales, distintas de las simuladas, se evaluaron con los mismos criterios.

## 7. Reproducibilidad R_O frente a R′_O (Tabla 8)

| Coincidencia en los 10 ciclos con orden de IA | Resultado |
|---|---|
| Misma decisión e intervención | **10/10** (las decide el backend) |
| Misma clasificación del resultado | 8/10. Difieren O12 (error del proveedor frente a aceptada) y O18 (SIN_EFECTO frente a aceptada) |
| Misma pertinencia | 8/10 |
| Mismo cambio exacto | **1/10** (solo O20: retirar 536) |

**Lectura:**
- El núcleo determinista se reproduce exactamente.
- El modelo llega a soluciones válidas distintas en cada corrida.
- Las dos corridas cumplen los mismos criterios técnicos.

## 8. Evidencia individual por usuario

| Qué | Dónde |
|---|---|
| Por perfil, semana y corrida | `entregables/R_O/` y `entregables/Rp_O/`: Tablas 1–5, `adherencia_semanal.csv`, `adaptaciones.csv`, `progresion_y_reincorporacion.csv`, `limitaciones.csv`, `matriz_10_aspectos.csv`, `calidad_catalogo.csv`, `generacion_por_sesion.csv` |
| Libro único | `entregables/EVALUACION_INTEGRAL_O_REPLICATE_REAL.xlsx` (R_O, R′_O y las hojas de Replicate) |
| Replicate | `analisis/predicciones_replicate.csv` (id, tokens, latencia, coste), `analisis/ciclos_ia.csv` (intentos, validación, clasificación y pertinencia por ciclo), `analisis/llamadas_ia_trazas.csv` |
| Corridas completas | `evidencia/corridas/*.tar.gz`: tablas exportadas, prompts enviados, respuestas en `trazas_ia.jsonl`, `intentos_ia.csv` y `factibilidad.csv` |
| Salidas del oráculo | `evidencia/oraculo/` (v1.2 y v1.3) |
| Credenciales | Ninguna en la evidencia (comprobado): el proxy inyectó la credencial y nunca fue visible |

## 9. Limitaciones

- **Adherencia simulada por guion;** no es comportamiento humano. La IA sí es real.
- **Pocas adaptaciones:** 10 ciclos con IA por corrida. Las proporciones son descriptivas, no estimaciones con intervalo.
- **Variabilidad del modelo:** 1/10 cambios idénticos entre corridas, 1 SIN_EFECTO evitable con justificación incorrecta y 1 error del proveedor.
- **Calidad deportiva no validada:** 14–15 observaciones del catálogo y la observación I-6 (rediseño ante un dolor sin localizar).
- **El oráculo es del mismo laboratorio.** Se corrigió dos veces antes y después de las corridas (v1.2, por la IA real; v1.3, por el dolor), con resultados originales conservados y pruebas negativas.
- **La versión del modelo no se puede fijar** sin cambiar el backend (usa el endpoint del modelo); se registró y no cambió.
