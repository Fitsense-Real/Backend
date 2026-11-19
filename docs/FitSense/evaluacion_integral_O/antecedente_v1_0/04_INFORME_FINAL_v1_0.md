# 04 · Evaluación integral de 20 usuarios sintéticos (cohorte O) · Informe final

| Elemento | Valor |
|---|---|
| Versión | **A** (`f1c6c02`): V46 + aviso E2-a, P15-C apagado, sin contrafactuales, PLAN-REV-2.0 |
| Commit ejecutado | `cb18381` (arnés extendido; `src/main` idéntico a `f1c6c02`) |
| IA | Simulada (prompt V2.1, sin red) |
| Coste | **0 llamadas a Replicate, USD 0** |
| Corrida principal | **S_O**: bloque G (O01–O14, progresión apagada) + bloque P (O15–O20, progresión encendida) |
| Reproducibilidad | **S_O′** da una huella normalizada idéntica (`7eb13353…`) y una matriz idéntica |
| Oráculo | Independiente, v1.0, congelado antes de ejecutar (`628be48e…`) |

**Separación de evaluaciones:**
- Estos resultados son de la cohorte **O** con IA **simulada**.
- No se mezclan con la evaluación anterior **U/N con IA real** (informe 25, 44 predicciones, USD 0,5622), que fue otra versión y otra corrida.

**El cumplimiento es SIMULADO por guion.** No mide ni demuestra un cambio de comportamiento de personas reales.

## 1. Resultado global

### Tabla 5b · Resultado oficial (oráculo v1.0, corrida S_O)

| Aspecto | APROBADO | FALLO | NO APLICA | REQUIERE REVISIÓN | Aprobación técnica (AP / (AP + FA)) |
|---|---|---|---|---|---|
| 1. Generación inicial | 6 | 0 | 0 | 14 | 6/6 = 100 % |
| 2. Personalización y restricciones | 20 | 0 | 0 | 0 | 20/20 = 100 % |
| 3. Registro de ejecución | 19 | 0 | 1 | 0 | 19/19 = 100 % |
| 4. Cálculo de adherencia | 19 | 0 | 1 | 0 | 19/19 = 100 % |
| 5. Adaptación con IA simulada | 9 | 0 | 10 | 1 | 9/9 = 100 % |
| 6. Mantenimiento sin adaptar | 19 | 0 | 1 | 0 | 19/19 = 100 % |
| 7. Progresión gradual | 6 | **2** | 12 | 0 | 6/8 = 75 % |
| 8. Reincorporación condicionada | 3 | **1** | 16 | 0 | 3/4 = 75 % |
| 9. Continuidad multisemana | 19 | 0 | 1 | 0 | 19/19 = 100 % |
| 10. Seguridad e integridad | 19 | 0 | 1 | 0 | 19/19 = 100 % |
| **Total (20 × 10 = 200)** | **139** | **3** | **43** | **15** | **139/142 = 97,9 %** |

**Los 3 FALLO (O03·A7, O14·A7, O19·A8) son defectos del oráculo, no del sistema** (`05_DEFECTOS_Y_CORRECCION.md`):
- O03 y O14: el oráculo 1.0 contaba como «paso con la progresión apagada» la RECUPERACION que la regla aprobada P-A1 admite en REV2.
- O19: el oráculo 1.0 buscaba el ejercicio retirado en toda la semana, no en su linaje.

**El resultado oficial se conserva.** La recalificación diagnóstica con la corrección propuesta (v1.1), sobre las mismas salidas, da **142/142 = 100 %** y cambia exactamente esas 3 celdas. **No es oficial** hasta que apruebes la corrección y se repita la evaluación completa.

### NO APLICA (43)

Se informan aparte; no cuentan como aprobados.

| Origen | Celdas |
|---|---|
| O11 (ESPERADO_SIN_PLAN): aspectos 3–10 | 8 |
| A5, sin orden de adaptar con IA (O01, O02, O07–O10, O13, O15, O16 y O11) | 10 |
| A7, progresión apagada en el bloque G y sin pasos | 12, contando O11 |
| A8, ningún ejercicio retirado ni excluido | 16, contando O11 |

### REQUIERE REVISIÓN (15)

Contenido con validación deportiva pendiente. **No es un fallo técnico, ni una aprobación deportiva.**
- 14 en A1: el plan inicial contiene 853/1093/1320 (E4/E7), un curl nórdico (E3: O16 → 1196; O18 → 327) o la regla E2.
- 1 en A5: O12, cuyo sustituto 1093 tiene nombre e instrucciones pendientes de corrección.

## 2. Resultados por dimensión (lo que pide la orden §5)

| Dimensión | Resultado |
|---|---|
| **Cumplimiento técnico** | 0 FALLO del sistema. Las 3 celdas FALLO oficiales se deben al oráculo (§1). La decisión del backend coincide con la recalculada por el oráculo en **103/103 ciclos**, incluidos los del horizonte de reincorporación |
| **Coherencia de restricciones** (A2) | 20/20. Ningún ejercicio bloqueado, prohibido por edad, nivel o declaración, ni con equipamiento no declarado, **en ninguna semana**. S_MAX en los 3 perfiles que lo exigen (O07, O08, O09). DOSIS_NO_DEMOSTRADA donde la regla se cumple (O02, O05, O08, O13, O19) |
| **Pertinencia de la intervención** | 15 sesiones con orden de adaptar: **11 ACC** (pertinentes) y **4 CONS-J** (conservación justificada: falta de tiempo sin minutos o causa no registrada). **0 INC** |
| **Calidad deportiva del contenido** | **No validada.** Hay hallazgos de catálogo pendientes en 14 planes iniciales (E2/E3/E4). Ver `entregables/calidad_catalogo.csv` y las decisiones P-1 a P-14 |

## 3. Tablas 1–4

Las tablas completas, con valores reales por perfil, están en `entregables/TABLAS_SHORT_PAPER.md` y en el libro `entregables/EVALUACION_INTEGRAL_O_20_PERFILES.xlsx`.

**Generación (Tabla 2):**
- 19 planes válidos y 1 ESPERADO_SIN_PLAN (O11). `/plan/limitations` explica O11 con `FUERZA_SIN_TIEMPO`: «se necesitan unos 32 minutos por sesión».
- Sesiones programadas = min(días pedidos, tope del nivel, días disponibles) en 19/19 casos.
- Recortes S-MAX: O07 5 → 4, O08 7 → 4, O09 7 → 6.
- Ninguna sesión supera los minutos del perfil (V4M: 0 incumplimientos).

**Ejecución y adherencia (Tabla 3, S1–S4, 19 perfiles):**
- 256 sesiones programadas: 239 completas, 6 parciales, 10 omitidas y 1 sin registro.
- La adherencia observada, la cobertura y la ponderada coinciden con `weekly_user_metrics` y con V32 en todas las semanas.

**Decisiones C1–C4 (Tabla 4):**
- 63 NO_CHANGE, 11 ADAPT_LOCALIZED y 2 ADAPT_EXPANDED.
- En V32: 10 APLICADA, 3 SIN_EFECTO (conservación deliberada) y 63 SIN_ORDEN.

**Progresión:**
- 40 pasos: 35 PROGRESION, 3 RECUPERACION y 2 REINCORPORACION.
- Todos con volumen ≤ floor(1,10 × volumen inicial del linaje). Como máximo un paso por ciclo.
- Ningún paso sin dos semanas previas al 100 %.

## 4. Ejemplos ejecutados

Todos los valores proceden de la corrida S_O.

**Generación (O01: ganar masa, principiante, gimnasio, 3 días × 45 min):**
- Plan de 3 sesiones FULL_BODY de 35, 30 y 30 min (máximo 45). Cubre los 4 slots (empuje, tracción, rodilla, cadera).
- Sesión del lunes: curl femoral tumbado (1009) 3×10, inclinación pélvica a puente (1112) 3×10, press de pecho en máquina (1092) 3×10, remo unilateral con palanca (1041) 3×10, elevación de hombros (590) 2×10 y press de hombro sentado (739) 3×10.

**Restricciones (O13: 67 años, sin arrodillarse, 1205 y 1263 bloqueados):**
- Ningún ejercicio con impacto, carga axial, inversión, rodillas o bloqueado en ninguna de sus 5 semanas.

**Adaptación localizada por fatiga (O03, C1):**
- Adherencia del 75 % con una omisión por fatiga, lo que da ADAPT_LOCALIZED con REVISAR_CARGA_Y_RECUPERACION.
- La IA simulada propuso primero una subida, que fue rechazada (intento 1). El intento 2, aplicado, fue 899: 3 → 2 series, con un trabajo de 216 → 198 s.
- Las otras 3 sesiones quedaron idénticas. En C3 la dosis vuelve a su referencia (RECUPERACION, 2 → 3 series) sin superarla.

**Dolor (O06, C1):**
- Dolor marcado en el último ejercicio (1117), lo que da SEGURIDAD_ANTE_DOLOR.
- Sustitución 1117 → 53, más un recorte de 1054 de 120 → 115 s. El trabajo baja de 774 a 759 s.
- 1117 no vuelve en ninguna semana posterior.

**Dificultad (O04, C1):** sustitución 529 → 128 por un ejercicio de dificultad no mayor, del mismo patrón y del mismo grupo (VARIANTE_MAS_SENCILLA_O_CARGA).

**Mantenimiento:**
- O09 omite 1 de 6 sesiones (83,3 % ≥ 80): NO_CHANGE y la semana se copia.
- O02 omite por falta de tiempo sin declarar minutos: CONSERVAR (FaltaDeTiempoPolicy), sin cambios.

**Progresión (O15, bloque P):**
- C1: sin paso (solo una semana).
- C2: 342 pasa de 2×10 a 2×11; volumen 170 → 172, techo 187.
- C3 y C4: un paso cada uno en otras sesiones (1063 y 907: 13 → 14 repeticiones).
- **Progresión no forzada (O16):** C2 conserva (omisión por tiempo) y C3 no progresa porque la semana previa no fue del 100 %. El primer paso llega en C4.

**Reincorporación (O19):**
- C1: ACORTAR retira 853 de una sesión.
- C3–C8: 6 progresiones, 2 de ellas en ese mismo linaje (1179: 6 → 7 → 8 repeticiones).
- C9 (semana 10): 853 vuelve al final de la sesión con su dosis inicial exacta (3 × 120 s); volumen 72 → 84, techo 85.
- **O18:** retirada de 1302 en C1, 18 progresiones y reincorporación en C22 (semana 23), con volumen 182 = techo 182.
- **Negativo (O20):** el ejercicio excluido por dolor (536) no vuelve en 8 semanas con la progresión encendida.

## 5. Corridas complementarias

| Corrida | Resultado |
|---|---|
| S_O′ (repetición) | Huella `7eb13353…` y matriz **idénticas** a S_O |
| D_O (IA apagada → respaldo determinista) | 138/138 APROBADO, 0 FALLO, 47 NO APLICA, 15 REQUIERE REVISIÓN. Huella `d46b3f50…` |
| D_O_HIST (PLAN-HIST-1) | 20/20 generaciones publicadas y ciclos continuados. O11 sí obtiene plan con HIST, porque la política histórica no tiene la comprobación `FUERZA_SIN_TIEMPO`. Es una diferencia de política conocida, no un fallo. No cuenta en la Tabla 5 |
| Regresión U/N con el arnés extendido | `a662cb4d`, `11f51d44`, `f9f67973` (HIST congelada), `84dfb6d2` y `da34db35`: **todas iguales a la referencia** |

## 6. Limitaciones

- Cumplimiento simulado y deterministas. Las respuestas de la IA las escribe el laboratorio: miden el recorrido técnico (contexto, validación, V8C, rechazo y respaldo), **no** la calidad de un modelo real.
- La calidad deportiva del catálogo sigue pendiente de validación profesional (E2, E3, E4/E7).
- La reincorporación necesitó horizontes largos (9 y 22 ciclos), como ya mostraba la batería permanente.
- La «aprobación técnica» se refiere a reglas del sistema verificadas por un oráculo independiente; no es evidencia clínica.
