# Tablas para el short paper (valores de la corrida S_O)

## Tabla 1. Características de los 20 perfiles sintéticos

| ID | Objetivo | Nivel | Equipamiento | Días solicitados | Días programados | Duración (min) | Edad | Restricciones |
|---|---|---|---|---|---|---|---|---|
| O01 | Ganar masa muscular | Principiante | gimnasio | 3 | 3 | 45 | 28 | ninguna |
| O02 | Perder peso | Principiante | casa sin material | 2 | 2 | 30 | 35 | ninguna |
| O03 | Aumentar fuerza | Intermedio | gimnasio | 4 | 4 | 60 | 41 | sin alto impacto |
| O04 | Condición general | Avanzado | casa con mancuernas y bandas | 5 | 5 | 50 | 30 | ninguna |
| O05 | Ganar masa muscular | Intermedio | casa sin material | 4 | 4 | 40 | 26 | ninguna |
| O06 | Perder peso | Intermedio | gimnasio | 3 | 3 | 35 | 52 | ninguna |
| O07 | Aumentar fuerza | Principiante | casa con mancuernas y bandas | 5 | 4 | 50 | 33 | ninguna |
| O08 | Condición general | Principiante | casa sin material | 7 | 4 | 30 | 45 | sin alto impacto |
| O09 | Condición general | Avanzado | gimnasio | 7 | 6 | 75 | 28 | ninguna |
| O10 | Perder peso | Avanzado | casa sin material | 1 | 1 | 30 | 38 | ninguna |
| O11 | Aumentar fuerza | Avanzado | casa con mancuernas y bandas | 1 | 0 | 30 | 31 | ninguna |
| O12 | Condición general | Intermedio | gimnasio | 2 | 2 | 55 | 62 | sin por encima cabeza; sin colgarse |
| O13 | Ganar masa muscular | Principiante | casa sin material | 3 | 3 | 60 | 67 | sin rodillas; bloqueados 1205/1263 |
| O14 | Perder peso | Principiante | gimnasio | 4 | 4 | 40 | 23 | ninguna |
| O15 | Ganar masa muscular | Intermedio | gimnasio | 3 | 3 | 45 | 34 | ninguna |
| O16 | Aumentar fuerza | Avanzado | gimnasio | 4 | 4 | 60 | 39 | ninguna |
| O17 | Condición general | Principiante | casa con mancuernas y bandas | 3 | 3 | 45 | 27 | ninguna |
| O18 | Ganar masa muscular | Avanzado | gimnasio | 3 | 3 | 45 | 31 | ninguna |
| O19 | Perder peso | Intermedio | casa sin material | 3 | 3 | 50 | 44 | sin rodillas |
| O20 | Aumentar fuerza | Intermedio | casa con mancuernas y bandas | 3 | 3 | 45 | 35 | ninguna |

## Tabla 2. Generación y personalización

| ID | Resultado | Sesiones (esperadas) | Duración declarada por sesión (min) | Máx. del perfil (min) | Slots 4C cubiertos | Estado de dosis | Incumplimientos de validación (oráculo) | Limitaciones (/plan/limitations) |
|---|---|---|---|---|---|---|---|---|
| O01 | Plan válido | 3 (3) | 35/30/30 | 45 | 4/4 (HIP+KNEE+PULL+PUSH) | DOSIS_OBJETIVO | 0 | ninguna |
| O02 | Plan válido | 2 (2) | 27/27 | 30 | 1/4 (PUSH) | DOSIS_LIMITADA | 0 | DOSIS_LIMITADA, DOSIS_NO_DEMOSTRADA, UPPER_PULL |
| O03 | Plan válido | 4 (4) | 35/25/32/25 | 60 | 4/4 (HIP+KNEE+PULL+PUSH) | DOSIS_OBJETIVO | 0 | ninguna |
| O04 | Plan válido | 5 (5) | 43/40/43/43/40 | 50 | 4/4 (HIP+KNEE+PULL+PUSH) | DOSIS_OBJETIVO | 0 | ninguna |
| O05 | Plan válido | 4 (4) | 33/30/31/30 | 40 | 3/4 (HIP+KNEE+PUSH) | DOSIS_LIMITADA | 0 | DOSIS_LIMITADA, DOSIS_NO_DEMOSTRADA, UPPER_PULL |
| O06 | Plan válido | 3 (3) | 29/29/29 | 35 | 1/4 (PUSH) | DOSIS_LIMITADA | 0 | DOSIS_LIMITADA |
| O07 | Plan válido | 4 (4) | 23/19/19/19 | 50 | 4/4 (HIP+KNEE+PULL+PUSH) | DOSIS_OBJETIVO | 0 | S_MAX |
| O08 | Plan válido | 4 (4) | 28/25/27/24 | 30 | 3/4 (HIP+KNEE+PUSH) | DOSIS_LIMITADA | 0 | DOSIS_LIMITADA, DOSIS_NO_DEMOSTRADA, S_MAX, UPPER_PULL |
| O09 | Plan válido | 6 (6) | 43/40/46/40/40/40 | 75 | 4/4 (HIP+KNEE+PULL+PUSH) | DOSIS_OBJETIVO | 0 | S_MAX |
| O10 | Plan válido | 1 (1) | 29 | 30 | 1/4 (PUSH) | DOSIS_LIMITADA | 0 | DOSIS_LIMITADA, UPPER_PULL |
| O11 | ESPERADO_SIN_PLAN | 0 (1) | - | 30 | - | - | - | FUERZA_SIN_TIEMPO |
| O12 | Plan válido | 2 (2) | 48/52 | 55 | 4/4 (HIP+KNEE+PULL+PUSH) | DOSIS_LIMITADA | 0 | DOSIS_LIMITADA |
| O13 | Plan válido | 3 (3) | 37/26/24 | 60 | 3/4 (HIP+KNEE+PUSH) | DOSIS_OBJETIVO | 0 | DOSIS_NO_DEMOSTRADA, UPPER_PULL |
| O14 | Plan válido | 4 (4) | 34/30/34/30 | 40 | 4/4 (HIP+KNEE+PULL+PUSH) | DOSIS_OBJETIVO | 0 | ninguna |
| O15 | Plan válido | 3 (3) | 35/33/33 | 45 | 4/4 (HIP+KNEE+PULL+PUSH) | DOSIS_LIMITADA | 0 | DOSIS_LIMITADA |
| O16 | Plan válido | 4 (4) | 37/25/32/25 | 60 | 4/4 (HIP+KNEE+PULL+PUSH) | DOSIS_OBJETIVO | 0 | ninguna |
| O17 | Plan válido | 3 (3) | 40/42/42 | 45 | 4/4 (HIP+KNEE+PULL+PUSH) | DOSIS_OBJETIVO | 0 | ninguna |
| O18 | Plan válido | 3 (3) | 37/35/33 | 45 | 4/4 (HIP+KNEE+PULL+PUSH) | DOSIS_LIMITADA | 0 | DOSIS_LIMITADA |
| O19 | Plan válido | 3 (3) | 43/43/43 | 50 | 3/4 (HIP+KNEE+PUSH) | DOSIS_LIMITADA | 0 | DOSIS_LIMITADA, DOSIS_NO_DEMOSTRADA, UPPER_PULL |
| O20 | Plan válido | 3 (3) | 37/32/32 | 45 | 4/4 (HIP+KNEE+PULL+PUSH) | DOSIS_OBJETIVO | 0 | ninguna |

## Tabla 3. Ejecución y adherencia simulada (S1–S4)

| ID | Sesiones programadas (S1-S4) | Completas | Parciales | Omitidas | Sin registro | Adherencia observada S1/S2/S3/S4 (%) | Señal detectada | Decisión esperada (oráculo) C1-C4 | Decisión obtenida (backend) C1-C4 | Coinciden |
|---|---|---|---|---|---|---|---|---|---|---|
| O01 | 12 | 12 | 0 | 0 | 0 | 100.00 / 100.00 / 100.00 / 100.00 | ninguna | NO_CHANGE / NO_CHANGE / NO_CHANGE / NO_CHANGE | NO_CHANGE / NO_CHANGE / NO_CHANGE / NO_CHANGE | sí |
| O02 | 8 | 7 | 0 | 1 | 0 | 50.00 / 100.00 / 100.00 / 100.00 | S1: LACK_OF_TIME | ADAPT_LOCALIZED / NO_CHANGE / NO_CHANGE / NO_CHANGE | ADAPT_LOCALIZED / NO_CHANGE / NO_CHANGE / NO_CHANGE | sí |
| O03 | 16 | 15 | 0 | 1 | 0 | 75.00 / 100.00 / 100.00 / 100.00 | S1: FATIGUE | ADAPT_LOCALIZED / NO_CHANGE / NO_CHANGE / NO_CHANGE | ADAPT_LOCALIZED / NO_CHANGE / NO_CHANGE / NO_CHANGE | sí |
| O04 | 20 | 18 | 1 | 1 | 0 | 71.94 / 100.00 / 100.00 / 100.00 | S1: TOO_DIFFICULT | ADAPT_LOCALIZED / NO_CHANGE / NO_CHANGE / NO_CHANGE | ADAPT_LOCALIZED / NO_CHANGE / NO_CHANGE / NO_CHANGE | sí |
| O05 | 16 | 13 | 1 | 2 | 0 | 34.74 / 100.00 / 100.00 / 100.00 | S1: LACK_OF_MOTIVATION | ADAPT_EXPANDED / NO_CHANGE / NO_CHANGE / NO_CHANGE | ADAPT_EXPANDED / NO_CHANGE / NO_CHANGE / NO_CHANGE | sí |
| O06 | 12 | 12 | 0 | 0 | 0 | 94.44 / 100.00 / 100.00 / 100.00 | S1: PAIN_OR_DISCOMFORT | ADAPT_LOCALIZED / NO_CHANGE / NO_CHANGE / NO_CHANGE | ADAPT_LOCALIZED / NO_CHANGE / NO_CHANGE / NO_CHANGE | sí |
| O07 | 16 | 16 | 0 | 0 | 0 | 100.00 / 100.00 / 100.00 / 100.00 | ninguna | NO_CHANGE / NO_CHANGE / NO_CHANGE / NO_CHANGE | NO_CHANGE / NO_CHANGE / NO_CHANGE / NO_CHANGE | sí |
| O08 | 16 | 15 | 0 | 0 | 1 | 100.00 / 100.00 / 100.00 / 100.00 | ninguna | NO_CHANGE / NO_CHANGE / NO_CHANGE / NO_CHANGE | NO_CHANGE / NO_CHANGE / NO_CHANGE / NO_CHANGE | sí |
| O09 | 24 | 23 | 0 | 1 | 0 | 83.33 / 100.00 / 100.00 / 100.00 | S1: SCHEDULE_CHANGE | NO_CHANGE / NO_CHANGE / NO_CHANGE / NO_CHANGE | NO_CHANGE / NO_CHANGE / NO_CHANGE / NO_CHANGE | sí |
| O10 | 4 | 3 | 1 | 0 | 0 | 39.63 / 100.00 / 100.00 / 100.00 | ninguna | ADAPT_EXPANDED / NO_CHANGE / NO_CHANGE / NO_CHANGE | ADAPT_EXPANDED / NO_CHANGE / NO_CHANGE / NO_CHANGE | sí |
| O12 | 8 | 7 | 0 | 1 | 0 | 50.00 / 100.00 / 100.00 / 100.00 | S1: PAIN_OR_DISCOMFORT | ADAPT_LOCALIZED / NO_CHANGE / NO_CHANGE / NO_CHANGE | ADAPT_LOCALIZED / NO_CHANGE / NO_CHANGE / NO_CHANGE | sí |
| O13 | 12 | 11 | 1 | 0 | 0 | 86.77 / 100.00 / 100.00 / 100.00 | ninguna | NO_CHANGE / NO_CHANGE / NO_CHANGE / NO_CHANGE | NO_CHANGE / NO_CHANGE / NO_CHANGE / NO_CHANGE | sí |
| O14 | 16 | 15 | 0 | 1 | 0 | 97.92 / 75.00 / 100.00 / 100.00 | S1: FATIGUE / S2: FATIGUE | NO_CHANGE / ADAPT_LOCALIZED / NO_CHANGE / NO_CHANGE | NO_CHANGE / ADAPT_LOCALIZED / NO_CHANGE / NO_CHANGE | sí |
| O15 | 12 | 12 | 0 | 0 | 0 | 100.00 / 100.00 / 100.00 / 100.00 | ninguna | NO_CHANGE / NO_CHANGE / NO_CHANGE / NO_CHANGE | NO_CHANGE / NO_CHANGE / NO_CHANGE / NO_CHANGE | sí |
| O16 | 16 | 15 | 0 | 1 | 0 | 100.00 / 75.00 / 100.00 / 100.00 | S2: LACK_OF_TIME | NO_CHANGE / ADAPT_LOCALIZED / NO_CHANGE / NO_CHANGE | NO_CHANGE / ADAPT_LOCALIZED / NO_CHANGE / NO_CHANGE | sí |
| O17 | 12 | 11 | 0 | 1 | 0 | 66.67 / 100.00 / 100.00 / 100.00 | S1: FATIGUE | ADAPT_LOCALIZED / NO_CHANGE / NO_CHANGE / NO_CHANGE | ADAPT_LOCALIZED / NO_CHANGE / NO_CHANGE / NO_CHANGE | sí |
| O18 | 12 | 11 | 1 | 0 | 0 | 78.33 / 100.00 / 100.00 / 100.00 | S1: LACK_OF_TIME | ADAPT_LOCALIZED / NO_CHANGE / NO_CHANGE / NO_CHANGE | ADAPT_LOCALIZED / NO_CHANGE / NO_CHANGE / NO_CHANGE | sí |
| O19 | 12 | 11 | 1 | 0 | 0 | 78.52 / 100.00 / 100.00 / 100.00 | S1: LACK_OF_TIME | ADAPT_LOCALIZED / NO_CHANGE / NO_CHANGE / NO_CHANGE | ADAPT_LOCALIZED / NO_CHANGE / NO_CHANGE / NO_CHANGE | sí |
| O20 | 12 | 12 | 0 | 0 | 0 | 95.83 / 100.00 / 100.00 / 100.00 | S1: PAIN_OR_DISCOMFORT | ADAPT_LOCALIZED / NO_CHANGE / NO_CHANGE / NO_CHANGE | ADAPT_LOCALIZED / NO_CHANGE / NO_CHANGE / NO_CHANGE | sí |

## Tabla 4. Adaptación, mantenimiento y progresión

| ID | Decisiones C1-C4 | Modificaciones realizadas | Restricciones respetadas (A2) | Pasos de progresión | Reincorporación | Semanas medidas |
|---|---|---|---|---|---|---|
| O01 | NO_CHANGE·- / NO_CHANGE·- / NO_CHANGE·- / NO_CHANGE·- | ninguna | sí | 0 | - | 4 |
| O02 | ADAPT_LOCALIZED·CONSERVAR / NO_CHANGE·- / NO_CHANGE·- / NO_CHANGE·- | C1 CONSERVAR (SIN_EFECTO, CONS-J): sin cambios | sí | 0 | - | 4 |
| O03 | ADAPT_LOCALIZED·REVISAR_CARGA_Y_RECUPERACION / NO_CHANGE·- / NO_CHANGE·- / NO_CHANGE·- | C1 REVISAR_CARGA_Y_RECUPERACION (APLICADA, ACC): reducción 899.sets 3->2 | sí | RECUPERACION×1 | - | 4 |
| O04 | ADAPT_LOCALIZED·VARIANTE_MAS_SENCILLA_O_CARGA / NO_CHANGE·- / NO_CHANGE·- / NO_CHANGE·- | C1 VARIANTE_MAS_SENCILLA_O_CARGA (APLICADA, ACC): sustitución 529->128 | sí | 0 | - | 4 |
| O05 | ADAPT_EXPANDED·CAUSA_NO_REGISTRADA/INVESTIGAR_CAMBIO_MINIMO / NO_CHANGE·- / NO_CHANGE·- / NO_CHANGE·- | C1 INVESTIGAR_CAMBIO_MINIMO (APLICADA, ACC): sustitución 318->508 / C1 INVESTIGAR_CAMBIO_MINIMO (APLICADA, ACC): sustitución 1076->507 / C1 CAUSA_NO_REGISTRADA (APLICADA, CONS-J): sin cambios | sí | 0 | - | 4 |
| O06 | ADAPT_LOCALIZED·SEGURIDAD_ANTE_DOLOR / NO_CHANGE·- / NO_CHANGE·- / NO_CHANGE·- | C1 SEGURIDAD_ANTE_DOLOR (APLICADA, ACC): sustitución 1117->53, reducción 1054.dur 120->115 | sí | 0 | - | 4 |
| O07 | NO_CHANGE·- / NO_CHANGE·- / NO_CHANGE·- / NO_CHANGE·- | ninguna | sí | 0 | - | 4 |
| O08 | NO_CHANGE·- / NO_CHANGE·- / NO_CHANGE·- / NO_CHANGE·- | ninguna | sí | 0 | - | 4 |
| O09 | NO_CHANGE·- / NO_CHANGE·- / NO_CHANGE·- / NO_CHANGE·- | ninguna | sí | 0 | - | 4 |
| O10 | ADAPT_EXPANDED·CAUSA_NO_REGISTRADA / NO_CHANGE·- / NO_CHANGE·- / NO_CHANGE·- | C1 CAUSA_NO_REGISTRADA (SIN_EFECTO, CONS-J): sin cambios | sí | 0 | - | 4 |
| O12 | ADAPT_LOCALIZED·SEGURIDAD_ANTE_DOLOR / NO_CHANGE·- / NO_CHANGE·- / NO_CHANGE·- | C1 SEGURIDAD_ANTE_DOLOR (APLICADA, ACC): sustitución 575->98;591->342;395->119;1233->239;1117->253;532->1054;853->1093;455->453 | sí | 0 | - | 4 |
| O13 | NO_CHANGE·- / NO_CHANGE·- / NO_CHANGE·- / NO_CHANGE·- | ninguna | sí | 0 | - | 4 |
| O14 | NO_CHANGE·- / ADAPT_LOCALIZED·REVISAR_CARGA_Y_RECUPERACION / NO_CHANGE·- / NO_CHANGE·- | C2 REVISAR_CARGA_Y_RECUPERACION (APLICADA, ACC): reducción 253.sets 3->2 | sí | RECUPERACION×1 | - | 4 |
| O15 | NO_CHANGE·- / NO_CHANGE·- / NO_CHANGE·- / NO_CHANGE·- | ninguna | sí | PROGRESION×3 | - | 4 |
| O16 | NO_CHANGE·- / ADAPT_LOCALIZED·CONSERVAR / NO_CHANGE·- / NO_CHANGE·- | C2 CONSERVAR (SIN_EFECTO, CONS-J): sin cambios | sí | PROGRESION×1 | - | 4 |
| O17 | ADAPT_LOCALIZED·REVISAR_CARGA_Y_RECUPERACION / NO_CHANGE·- / NO_CHANGE·- / NO_CHANGE·- | C1 REVISAR_CARGA_Y_RECUPERACION (APLICADA, ACC): reducción 853.sets 3->2 | sí | PROGRESION×1, RECUPERACION×1 | - | 4 |
| O18 | ADAPT_LOCALIZED·ACORTAR_O_REORGANIZAR / NO_CHANGE·- / NO_CHANGE·- / NO_CHANGE·- | C1 ACORTAR_O_REORGANIZAR (APLICADA, ACC): retirado 1302 | sí | PROGRESION×18, REINCORPORACION×1 | ej. 1302 en C22 (retirado en C1) | 22 |
| O19 | ADAPT_LOCALIZED·ACORTAR_O_REORGANIZAR / NO_CHANGE·- / NO_CHANGE·- / NO_CHANGE·- | C1 ACORTAR_O_REORGANIZAR (APLICADA, ACC): retirado 853 | sí | PROGRESION×6, REINCORPORACION×1 | ej. 853 en C9 (retirado en C1) | 9 |
| O20 | ADAPT_LOCALIZED·SEGURIDAD_ANTE_DOLOR / NO_CHANGE·- / NO_CHANGE·- / NO_CHANGE·- | C1 SEGURIDAD_ANTE_DOLOR (APLICADA, ACC): sustitución 536->511 | sí | PROGRESION×6 | - | 8 |

## Tabla 5. Evaluación integral de los diez aspectos

| ID | 1. Generación inicial | 2. Personalización y restricciones | 3. Registro de ejecución | 4. Cálculo de adherencia | 5. Adaptación con IA simulada | 6. Mantenimiento sin adaptar | 7. Progresión gradual | 8. Reincorporación condicionada | 9. Continuidad multisemana | 10. Seguridad e integridad |
|---|---|---|---|---|---|---|---|---|---|---|
| O01 | APROBADO | APROBADO | APROBADO | APROBADO | NO APLICA | APROBADO | NO APLICA | NO APLICA | APROBADO | APROBADO |
| O02 | REQUIERE REVISION | APROBADO | APROBADO | APROBADO | NO APLICA | APROBADO | NO APLICA | NO APLICA | APROBADO | APROBADO |
| O03 | APROBADO | APROBADO | APROBADO | APROBADO | APROBADO | APROBADO | FALLO | NO APLICA | APROBADO | APROBADO |
| O04 | REQUIERE REVISION | APROBADO | APROBADO | APROBADO | APROBADO | APROBADO | NO APLICA | NO APLICA | APROBADO | APROBADO |
| O05 | REQUIERE REVISION | APROBADO | APROBADO | APROBADO | APROBADO | APROBADO | NO APLICA | NO APLICA | APROBADO | APROBADO |
| O06 | REQUIERE REVISION | APROBADO | APROBADO | APROBADO | APROBADO | APROBADO | NO APLICA | APROBADO | APROBADO | APROBADO |
| O07 | REQUIERE REVISION | APROBADO | APROBADO | APROBADO | NO APLICA | APROBADO | NO APLICA | NO APLICA | APROBADO | APROBADO |
| O08 | REQUIERE REVISION | APROBADO | APROBADO | APROBADO | NO APLICA | APROBADO | NO APLICA | NO APLICA | APROBADO | APROBADO |
| O09 | REQUIERE REVISION | APROBADO | APROBADO | APROBADO | NO APLICA | APROBADO | NO APLICA | NO APLICA | APROBADO | APROBADO |
| O10 | APROBADO | APROBADO | APROBADO | APROBADO | NO APLICA | APROBADO | NO APLICA | NO APLICA | APROBADO | APROBADO |
| O11 | APROBADO | APROBADO | NO APLICA | NO APLICA | NO APLICA | NO APLICA | NO APLICA | NO APLICA | NO APLICA | NO APLICA |
| O12 | REQUIERE REVISION | APROBADO | APROBADO | APROBADO | REQUIERE REVISION | APROBADO | NO APLICA | NO APLICA | APROBADO | APROBADO |
| O13 | REQUIERE REVISION | APROBADO | APROBADO | APROBADO | NO APLICA | APROBADO | NO APLICA | NO APLICA | APROBADO | APROBADO |
| O14 | REQUIERE REVISION | APROBADO | APROBADO | APROBADO | APROBADO | APROBADO | FALLO | NO APLICA | APROBADO | APROBADO |
| O15 | APROBADO | APROBADO | APROBADO | APROBADO | NO APLICA | APROBADO | APROBADO | NO APLICA | APROBADO | APROBADO |
| O16 | REQUIERE REVISION | APROBADO | APROBADO | APROBADO | NO APLICA | APROBADO | APROBADO | NO APLICA | APROBADO | APROBADO |
| O17 | REQUIERE REVISION | APROBADO | APROBADO | APROBADO | APROBADO | APROBADO | APROBADO | NO APLICA | APROBADO | APROBADO |
| O18 | REQUIERE REVISION | APROBADO | APROBADO | APROBADO | APROBADO | APROBADO | APROBADO | APROBADO | APROBADO | APROBADO |
| O19 | REQUIERE REVISION | APROBADO | APROBADO | APROBADO | APROBADO | APROBADO | APROBADO | FALLO | APROBADO | APROBADO |
| O20 | APROBADO | APROBADO | APROBADO | APROBADO | APROBADO | APROBADO | APROBADO | APROBADO | APROBADO | APROBADO |

## Tabla 5b. Totales por aspecto

| Aspecto | APROBADO | FALLO | NO APLICA | REQUIERE REVISIÓN | Aplicables (AP+FA) | Aprobación técnica |
|---|---|---|---|---|---|---|
| 1. Generación inicial | 6 | 0 | 0 | 14 | 6 | 100.0 % |
| 2. Personalización y restricciones | 20 | 0 | 0 | 0 | 20 | 100.0 % |
| 3. Registro de ejecución | 19 | 0 | 1 | 0 | 19 | 100.0 % |
| 4. Cálculo de adherencia | 19 | 0 | 1 | 0 | 19 | 100.0 % |
| 5. Adaptación con IA simulada | 9 | 0 | 10 | 1 | 9 | 100.0 % |
| 6. Mantenimiento sin adaptar | 19 | 0 | 1 | 0 | 19 | 100.0 % |
| 7. Progresión gradual | 6 | 2 | 12 | 0 | 8 | 75.0 % |
| 8. Reincorporación condicionada | 3 | 1 | 16 | 0 | 4 | 75.0 % |
| 9. Continuidad multisemana | 19 | 0 | 1 | 0 | 19 | 100.0 % |
| 10. Seguridad e integridad | 19 | 0 | 1 | 0 | 19 | 100.0 % |
| TOTAL (20 × 10 = 200) | 139 | 3 | 43 | 15 | 142 | 97.9 % |

