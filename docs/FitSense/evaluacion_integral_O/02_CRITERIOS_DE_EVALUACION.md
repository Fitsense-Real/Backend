# 02 · Criterios de evaluación (fijados ANTES de ejecutar la cohorte O)

## Herramientas

**Oráculo:** `herramientas/oraculo_integral_O.py`.
- Es independiente: Python, y solo lee las tablas exportadas y los ficheros de la corrida.
- Reutiliza únicamente funciones puras del oráculo histórico K20 (redondeos, dosis, segundos estimados, comparación de sesiones, pertinencia).
- Las reglas del backend que replica están documentadas en el código fuente y citadas abajo. El oráculo **no llama a ninguna función del backend**.

**Desarrollo:** se probó solo con la cohorte desechable Z (Z01–Z06), nunca con O. Los ajustes hechos durante el ensayo:
- el bloque de progresión se lee de la configuración real de la corrida;
- se añadió la pertinencia de ACORTAR;
- se replica `FaltaDeTiempoPolicy`.

**Congelación:** el commit que congela el oráculo y este documento se cita en `03_VERSION_CONGELADA.md`. **No se modifican después de ver los resultados de O.**

## Estados

| Estado | Cuándo |
|---|---|
| **APROBADO** | Todas las comprobaciones técnicas del aspecto se cumplen |
| **FALLO** | Al menos una comprobación técnica no se cumple. Se conserva la evidencia, se diagnostica y no se corrige durante la corrida |
| **NO APLICA** | El aspecto no se ejercita en ese perfil. Ejemplos: progresión en el bloque G, o adaptación sin orden de adaptar |
| **REQUIERE REVISIÓN** | Técnicamente correcto, pero el contenido incluye ejercicios con validación deportiva pendiente (E2, E3, E4/E7), o la predicción de ESPERADO_SIN_PLAN no se cumplió |

**Cómputo:** la aprobación técnica es APROBADO / (APROBADO + FALLO). NO APLICA y REQUIERE REVISIÓN **no cuentan como aprobados** y se informan aparte.

## Los diez aspectos

| # | Aspecto | Comprobaciones (todas recalculadas por el oráculo) |
|---|---|---|
| 1 | Generación inicial | Un plan vigente en S1. Validación REV2: V1, V5, V6, V7, V17, V21, días disponibles, sin repetir día, dentro de la semana; **V4M** (segundos estimados ≤ minutos × 60); **QINS** (≥ 3 ejercicios y ≥ 6 series). **DIAS-1:** sesiones = min(días pedidos, tope del nivel 4/6, días disponibles). O11: ningún plan y `/plan/limitations` lo explica |
| 2 | Personalización y restricciones | `/plan/limitations` responde HTTP 200. S_MAX solo si días pedidos > tope. **En todas las semanas:** ningún ejercicio bloqueado, todo el equipamiento declarado y ninguna bandera prohibida, ni por edad (≥ 60 impacto, ≥ 65 carga axial e inversión, ≥ 70 suelo), ni por principiante (impacto), ni por lo declarado. DOSIS_NO_DEMOSTRADA (E2-a) presente si y solo si la regla se cumple en el plan, tras la generación y al final |
| 3 | Registro de ejecución | Cada paso del guion queda registrado exactamente: H 100 %; P40/P60 con round(total × f); HF/HD con el último ejercicio a total//2 y su causa; PT con la regla del 35 % y LACK_OF_TIME; O:motivo como SKIPPED con su motivo y sin sesión que cuente; N sin registro |
| 4 | Cálculo de adherencia | Cumplimiento por sesión: media de ejercicios con tope en 100. Adherencia observada, cobertura y ponderada **iguales** (± 0,01) a `weekly_user_metrics` y a la fila V32 de cada ciclo |
| 5 | Adaptación con IA simulada | (Si un ciclo ordena una intervención que requiere IA.) Decisión, sesiones señaladas e intervenciones iguales a las del oráculo (`AdaptationOrderDecider` + `FaltaDeTiempoPolicy`). El intento 1, inválido a propósito, existe y no es publicable. En la sesión señalada: sin más trabajo, sin añadidos, sustitutos del mismo patrón y grupo y con dificultad ≤, y el ejercicio con dolor desaparece. Las sesiones no señaladas siguen idénticas (A6). La pertinencia (ACC/ALT/CONS-J/INC) se informa aparte |
| 6 | Mantenimiento cuando no procede adaptar | En los ciclos NO_CHANGE / INSUFFICIENT_DATA, en las sesiones no señaladas y en las intervenciones que no autorizan cambios (CONSERVAR, CAUSA_NO_REGISTRADA), la semana siguiente es una copia exacta. La única diferencia admitida es el paso de progresión registrado en el informe V32. La decisión coincide con el oráculo |
| 7 | Progresión gradual | (Solo bloque P.) Como máximo un paso por ciclo y solo con NO_CHANGE. **PROGRESION:** S y O al 100 %, sin fatiga ni dolor, O→S fue una copia, el cambio es +1 repetición o +1 serie en un solo ejercicio (dosis exacta + 2 como máximo) y el volumen queda ≤ floor(1,10 × volumen inicial del linaje). **RECUPERACION:** nunca por encima de la referencia. Si se cumplen las condiciones de dos semanas sin ningún paso ni bloqueo registrado en todo el horizonte: FALLO |
| 8 | Reincorporación condicionada | Un retirado por ACORTAR solo vuelve con un paso REINCORPORACION, después de al menos una PROGRESION posterior a la retirada, al final de la sesión y con su dosis inicial exacta. Si vuelve sin ese paso hay restauración automática: FALLO. Un excluido por dolor nunca vuelve. **Positivo:** reincorporación observada y válida. **Negativo:** retirados sin reincorporar con la progresión apagada, o excluidos por dolor que no vuelven. Ambos son APROBADO y se distinguen en las notas. Pendiente en el horizonte con progresión: NO APLICA |
| 9 | Continuidad multisemana | Hay un plan vigente por semana medida y otro para la siguiente. Una fila V32 por ciclo. Linaje completo y biyectivo (source_planned_workout_id), con cada sesión derivada 7 días después. Al menos 4 semanas medidas. `GET /plan/<final>` HTTP 200 |
| 10 | Seguridad e integridad | Repetir el ciclo 1 no crea filas V32 ni planes (idempotencia). Ningún volumen supera 1,10 × el inicial del linaje. Ninguna dificultad supera el tope. El intento 1 inválido nunca es publicable. El ejercicio excluido por dolor no vuelve. El bloque de la corrida coincide con el del perfil |

## REQUIERE REVISIÓN (calidad del contenido; no es un fallo técnico)

| Aspecto | Cuándo |
|---|---|
| A1 | El plan de S1 contiene un curl nórdico (E3), 853/580/1093/1320 (E4/E7, solo lo que se muestra) o la regla E2-a (espalda u hombro solo de 775/973/1261/1308/1259/1146) |
| A5 | Un sustituto introducido es uno de esos ejercicios |
| A7 | Un paso se aplica sobre un curl nórdico |

**Por qué 1205 y 1076 no se marcan:** el protocolo solo los señala con P15-C activo, y en la versión A P15-C está apagado. Su presencia se informa en la tabla de calidad del catálogo.

**Las cuatro dimensiones que el informe distingue:**
- cumplimiento técnico (A1, A3–A10);
- coherencia de restricciones (A2);
- pertinencia de la intervención (ACC/ALT/CONS-J/INC por sesión adaptada);
- calidad deportiva del contenido (hallazgos de catálogo). **Nunca se declara «validada».**

## Corridas

**Todas sin Replicate.** Versión A con política PLAN-REV-2.0.

| Corrida | Modo | Bloques | Uso |
|---|---|---|---|
| **S_O** | IA simulada (prompt V2.1) | G (progresión apagada) + P (encendida) | **Evaluación principal (Tablas 1–5)** |
| S_O′ | Igual que S_O | G + P | Reproducibilidad: huella normalizada idéntica |
| D_O | Determinista (IA apagada → respaldo) | G + P | Núcleo determinista, como referencia |
| D_O_HIST | Determinista, PLAN-HIST-1 | G + P | Conservación de HIST con la cohorte nueva (solo generación y ciclos; no cuenta en la Tabla 5) |
| REG_U / REG_N | K20 sin modo integral | — | Regresión del arnés extendido: huellas `a662cb4d`, `11f51d44`, `f9f67973`, `84dfb6d2` y `da34db35` |
