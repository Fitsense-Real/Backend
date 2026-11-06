# 04 · Evaluación integral de 20 usuarios sintéticos (cohorte O) · Informe final DEFINITIVO

| Elemento | Valor |
|---|---|
| Estado | **Definitivo.** Repetición S_O con el oráculo v1.1 congelado (D-OR-1 y D-OR-2 aprobadas) |
| Versión | **A** (`f1c6c02`): catálogo V46, aviso E2-a `DOSIS_NO_DEMOSTRADA`, P15-C apagado, sin contrafactuales, PLAN-REV-2.0 |
| Arnés | `EvaluacionK20IT` de `cb18381` (solo pruebas; `src/main` idéntico a `f1c6c02`) |
| IA | Simulada (prompt V2.1, sin red). **0 llamadas a Replicate, USD 0** |
| Cohorte | O01–O20 con sus guiones, SHA-256 `9b996825…`. **Sin cambios** desde su congelación (`cb18381`) |
| Oráculo oficial | `oraculo_integral_O_v1_1.py`, SHA-256 `49d160d6…`, congelado en `cc54a3c` **antes** de ejecutar |
| Corridas | Repetición S_O (oficial) y S_O′, del 06/11/2026 a las 02:31 UTC. Evidencia en `repeticion_v1_1/` |
| Reproducibilidad | Huella normalizada `7eb13353…` idéntica en S_O, en S_O′ y en la corrida original |

**Antecedente que se conserva.** La primera evaluación de las mismas salidas, con el oráculo v1.0, dio **139/142 (97,9 %)**. Sus 3 FALLO se debían a dos defectos del **instrumento de evaluación**, no del sistema:
- **D-OR-1:** no admitía la RECUPERACION que permite P-A1 con la progresión apagada.
- **D-OR-2:** buscaba el ejercicio retirado en toda la semana, no en su linaje.

v1.1 corrige solo eso. Las 19 pruebas negativas demuestran que no relaja ninguna validación. El antecedente está íntegro en:
- `antecedente_v1_0/`: informe, texto IV, entregables, paquete y sumas SHA;
- `05_DEFECTOS_Y_CORRECCION.md`;
- `evidencia/`.

**Esta repetición no es una cohorte nueva ni una muestra independiente.**

**Separación de evaluaciones:**
- Estos resultados son de la cohorte O con **IA simulada**.
- La evaluación anterior **U/N con IA real** (`rediseno_politicas/pre_replicate/25_RESULTADOS_EVALUACION_REAL.md`; `src/main` `cc76bd1`, 44 predicciones, USD 0,5622) es otra versión y otra corrida, y se informa por separado.
- **El cumplimiento de la cohorte O es simulado por guion:** no mide ni demuestra el comportamiento de personas reales.

## 1. Resultado global

### Tabla 5b · Totales por aspecto (definitivo, oráculo v1.1)

| Aspecto | APROBADO | FALLO | NO APLICA | REQUIERE REVISIÓN | Aprobación técnica (AP / (AP + FA)) |
|---|---:|---:|---:|---:|---|
| 1. Generación inicial | 6 | 0 | 0 | 14 | 6/6 = 100 % |
| 2. Personalización y restricciones | 20 | 0 | 0 | 0 | 20/20 = 100 % |
| 3. Registro de ejecución | 19 | 0 | 1 | 0 | 19/19 = 100 % |
| 4. Cálculo de adherencia | 19 | 0 | 1 | 0 | 19/19 = 100 % |
| 5. Adaptación con IA simulada | 9 | 0 | 10 | 1 | 9/9 = 100 % |
| 6. Mantenimiento sin adaptar | 19 | 0 | 1 | 0 | 19/19 = 100 % |
| 7. Progresión gradual (y recuperación) | 8 | 0 | 12 | 0 | 8/8 = 100 % |
| 8. Reincorporación condicionada | 4 | 0 | 16 | 0 | 4/4 = 100 % |
| 9. Continuidad multisemana | 19 | 0 | 1 | 0 | 19/19 = 100 % |
| 10. Seguridad e integridad | 19 | 0 | 1 | 0 | 19/19 = 100 % |
| **Total (20 × 10 = 200)** | **142** | **0** | **43** | **15** | **142/142 = 100 %** |

**Aprobación técnica del 100 % en los casos aplicables.** NO APLICA y REQUIERE REVISIÓN se informan aparte y no cuentan como aprobados.

### Evolución del instrumento, sobre la misma ejecución

| | Oráculo v1.0 (antecedente oficial) | Oráculo v1.1 (definitivo) |
|---|---|---|
| APROBADO / FALLO | 139 / 3 | 142 / 0 |
| NO APLICA / REQUIERE REVISIÓN | 43 / 15 | 43 / 15 |
| Aprobación técnica | 97,9 % | 100 % |
| Celdas que cambian | — | Exactamente O03·A7, O14·A7 y O19·A8 |

**Comprobaciones de la corrección:**
- La repetición evaluada con v1.0 reproduce el 139/142.
- La corrida original evaluada con v1.1 da el mismo 142/142.
- El sistema produjo exactamente las mismas salidas: la huella es idéntica.

### NO APLICA (43)

| Origen | Celdas |
|---|---|
| O11 (ESPERADO_SIN_PLAN), aspectos 3–10 | 8 |
| A5, sin orden de adaptar con IA (O01, O02, O07–O10, O13, O15, O16, más O11) | 10 |
| A7, perfiles del bloque G sin ningún paso (contando O11) | 12 |
| A8, ningún ejercicio retirado ni excluido (contando O11) | 16 |

### REQUIERE REVISIÓN (15): observaciones deportivas del catálogo

**No son fallos técnicos ni aprobaciones deportivas.**
- **14 en A1:** el plan inicial contiene 853, 1093 o 1320 (E4/E7: nombre o instrucciones), un curl nórdico (E3: 1196 en O16, 327 en O18) o la regla E2 (dosis de espalda u hombro solo con ejercicios no demostrados).
- **1 en A5:** O12, cuyo sustituto 1093 tiene el nombre y las instrucciones pendientes de corrección.

## 2. Resultados por dimensión (orden §5)

| Dimensión | Resultado |
|---|---|
| **Cumplimiento técnico** | 0 FALLO. La decisión del backend coincide con la recalculada por el oráculo en **103/103 ciclos**, incluidos los horizontes de reincorporación |
| **Coherencia de restricciones** (A2) | 20/20, **en todas las semanas**: ningún ejercicio bloqueado, prohibido por edad, nivel o declaración, ni con equipamiento no declarado. S-MAX aplicado en los 3 perfiles que lo exigen (O07, O08 y O09). Aviso DOSIS_NO_DEMOSTRADA en O02, O05, O08, O13 y O19 |
| **Pertinencia de la intervención** | 15 sesiones con orden de adaptar: **11 ACC** (pertinentes) y **4 CONS-J** (conservación justificada: falta de tiempo sin minutos o causa no registrada). **0 INC** |
| **Calidad deportiva del contenido** | **No validada.** Hay hallazgos de catálogo pendientes en 14 planes iniciales y en 1 sustituto (E2, E3, E4/E7). Ver `entregables/calidad_catalogo.csv`. Las decisiones P-1 a P-14 **no** se han incorporado |

## 3. Tablas finales de los 20 usuarios

Todas están en `entregables/`, con valores individuales de los 20 perfiles:

| Fichero | Contenido |
|---|---|
| `TABLAS_SHORT_PAPER.md` y `EVALUACION_INTEGRAL_O_20_PERFILES.xlsx` | Tablas 1–5 y 5b, más las hojas largas |
| `Tabla1_perfiles.csv` | Objetivo, nivel, equipamiento, días, duración, edad y restricciones |
| `Tabla2_generacion_personalizacion.csv` + `generacion_por_sesion.csv` + `limitaciones.csv` | Generación |
| `Tabla3_ejecucion_adherencia.csv` + `adherencia_semanal.csv` | Sesiones programadas, completas, parciales, omitidas y sin registro; adherencia por semana; señal; decisión esperada y obtenida |
| `Tabla4_adaptacion_mantenimiento_progresion.csv` + `adaptaciones.csv` + `progresion_y_reincorporacion.csv` | Adaptaciones, progresiones y reincorporaciones |
| `Tabla5_diez_aspectos.csv` + `matriz_10_aspectos.csv` | Evaluación de los 10 aspectos |

**Síntesis:**

| Bloque | Resultado |
|---|---|
| Generación (Tabla 2) | 19 planes válidos y 1 ESPERADO_SIN_PLAN (O11). `/plan/limitations` lo explica con `FUERZA_SIN_TIEMPO`: «se necesitan unos 32 minutos por sesión». Sesiones = min(días pedidos, tope del nivel, días disponibles) en 19/19. Recortes S-MAX: O07 5 → 4, O08 7 → 4 y O09 7 → 6. 0 sesiones por encima de los minutos del perfil |
| Ejecución y adherencia (Tabla 3, S1–S4, 19 perfiles) | 256 sesiones programadas: 239 completas, 6 parciales, 10 omitidas y 1 sin registro. Las adherencias recalculadas coinciden con `weekly_user_metrics` y con V32 en todas las semanas |
| Decisiones C1–C4 (Tabla 4) | 63 NO_CHANGE, 11 ADAPT_LOCALIZED y 2 ADAPT_EXPANDED. En V32: 10 APLICADA, 3 SIN_EFECTO (conservación deliberada) y 63 SIN_ORDEN |
| Pasos | 40: 35 PROGRESION, 3 RECUPERACION y 2 REINCORPORACION. Todos con volumen ≤ floor(1,10 × volumen inicial del linaje) y como máximo 1 por ciclo. Ninguna PROGRESION sin dos semanas previas al 100 % |

## 4. Ejemplos ejecutados

Valores de la ejecución. Son idénticos en la corrida original, en la repetición y en S_O′.

| Caso | Qué ocurrió |
|---|---|
| **Generación** (O01: ganar masa, principiante, gimnasio, 3 días × 45 min) | 3 sesiones FULL_BODY de 35, 30 y 30 min, con los 4 slots (empuje, tracción, rodilla, cadera). Lunes: curl femoral tumbado (1009) 3×10, inclinación pélvica a puente (1112) 3×10, press de pecho en máquina (1092) 3×10, remo unilateral con palanca (1041) 3×10, elevación de hombros (590) 2×10 y press de hombro sentado (739) 3×10 |
| **Restricciones** (O13: 67 años, sin arrodillarse, 1205 y 1263 bloqueados) | Ningún ejercicio con impacto, carga axial, inversión, apoyo de rodillas o bloqueado, en ninguna de sus 5 semanas |
| **Fatiga y recuperación** (O03) | C1: adherencia del 75 % con una omisión por fatiga → ADAPT_LOCALIZED con REVISAR_CARGA_Y_RECUPERACION. La propuesta inválida del intento 1 se rechaza. El intento 2 aplica 899 de 3 → 2 series (trabajo 216 → 198 s), con las otras 3 sesiones idénticas. C3: **RECUPERACION** a 3 series, exactamente la dosis de referencia, sin superarla (volumen 72 ≤ techo 79) |
| **Dolor** (O06, C1) | Dolor en 1117 → SEGURIDAD_ANTE_DOLOR. Sustitución 1117 → 53 y recorte de 1054 de 120 → 115 s (trabajo 774 → 759 s). 1117 no vuelve |
| **Dificultad** (O04, C1) | Sustitución 529 → 128: dificultad no mayor, mismo patrón y mismo grupo |
| **Mantenimiento** | O09 omite 1 de 6 sesiones (83,3 % ≥ 80) → NO_CHANGE y copia. O02 omite por falta de tiempo sin declarar minutos → CONSERVAR, sin cambios |
| **Progresión** (O15) | C2: 342 de 2×10 a 2×11 (volumen 170 → 172, techo 187). C3 y C4: un paso cada una en otras sesiones (1063 y 907, de 13 → 14). **No forzada (O16):** C2 conserva y C3 no progresa porque la semana previa no fue del 100 %; el primer paso llega en C4 |
| **Reincorporación** (O19) | C1: ACORTAR retira 853 de una sesión. C3–C8: 6 progresiones, 2 en su linaje (1179: 6 → 7 → 8). C9 (semana 10): 853 vuelve **al final de su sesión** con su dosis inicial exacta (3 × 120 s); volumen 72 → 84, techo 85 |
| **Reincorporación** (O18) | Retirada de 1302 en C1, 18 progresiones y reincorporación en C22 (semana 23) con volumen 182 = techo 182 |
| **Negativo** (O20) | El excluido por dolor (536) no vuelve en 8 semanas con la progresión encendida |

## 5. Corridas complementarias

No se han repetido; se evaluaron con v1.0 y con v1.1.

| Corrida | Resultado |
|---|---|
| Repetición S_O′ | Huella y matriz idénticas a la repetición S_O (142/142) |
| D_O (IA apagada → respaldo determinista) | 138/138 APROBADO, 47 NO APLICA y 15 REQUIERE REVISIÓN. **v1.0 = v1.1** (0 celdas distintas) |
| D_O_HIST (PLAN-HIST-1) | 20/20 generaciones publicadas; v1.0 = v1.1. O11 obtiene plan con HIST (sin comprobación `FUERZA_SIN_TIEMPO`): es una diferencia de política conocida. No cuenta en la Tabla 5 |
| Regresión U/N con el arnés extendido | `a662cb4d`, `11f51d44`, `f9f67973` (HIST congelada), `84dfb6d2` y `da34db35`: iguales a la referencia (`03_VERSION_CONGELADA.md`) |

## 6. Limitaciones

- **Cumplimiento simulado y determinista.** Las respuestas de la IA las escribe el laboratorio: miden el recorrido técnico (contexto, validación, V8C, rechazo y respaldo), no la calidad de un modelo real. Los resultados no se atribuyen a participantes humanos.
- **Calidad deportiva pendiente** de validación profesional (E2, E3, E4/E7), con 15 observaciones abiertas.
- **Reincorporación lenta:** necesitó horizontes de 9 y 22 ciclos.
- **La aprobación técnica** se refiere a reglas del sistema verificadas por un oráculo independiente; no es evidencia clínica ni de eficacia.
- **El oráculo es del mismo laboratorio.** Se corrigió una vez (v1.0 → v1.1), con el antecedente conservado y pruebas negativas.
