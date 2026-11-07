# Repetición con el oráculo v1.1 · 01 · Resultado y verificación

**Qué es:** la misma cohorte O01–O20, con los mismos guiones, **reevaluada** tras corregir el instrumento de evaluación. **No es una cohorte nueva ni una muestra independiente.**

**Congelación previa:** `00_CONGELACION_ORACULO_V1_1.md`, commit `cc54a3c`, hecho antes de ejecutar.

## Ejecución

| Elemento | Valor |
|---|---|
| Corridas | S_O (bloques G y P) y S_O′ (repetición) |
| Hora | 06/11/2026, 02:31 UTC (`fecha_ejecucion_utc` en `evidencia/00_configuracion_*.txt`) |
| Arnés | `EvaluacionK20IT` del commit `cb18381`, el mismo de la corrida original |
| Código de producción | `src/main` = `f1c6c02` (versión A) |
| Parámetros | Idénticos a los de la corrida original: `00_configuracion.txt` solo difiere en la fecha. Script: `evidencia/corridas/correr_O_v11.sh` |
| Política y progresión | PLAN-REV-2.0; progresión solo en el bloque P |
| IA y catálogo | IA simulada V2.1; Flyway 46 |
| Construcción | 4 × BUILD SUCCESS. **0 llamadas a Replicate, USD 0** |
| Incidencias del arnés | 1 por corrida G: O11 `FUERZA_SIN_TIEMPO`, el ESPERADO_SIN_PLAN previsto |
| Oráculo oficial | v1.1, SHA `49d160d6…` |
| Oráculo v1.0 | Ejecutado sobre las mismas salidas, **solo para comparar** |

## Resultado definitivo (oráculo v1.1, repetición S_O)

| APROBADO | FALLO | NO APLICA | REQUIERE REVISIÓN | Aprobación técnica en casos aplicables |
|---:|---:|---:|---:|---|
| **142** | **0** | **43** | **15** | **142/142 = 100 %** |

- Las 15 REQUIERE REVISIÓN (observaciones deportivas del catálogo) y los 43 NO APLICA se mantienen exactamente.
- No cuentan como aprobados.

## Comparación con la evaluación original

Ficheros: `comparacion/totales_por_evaluacion.csv`, `comparacion/comparacion_matrices.csv` y `comparacion/comparacion_salidas_oraculo.csv`.

| Evaluación | AP | FA | NA | RR | Aprobación | Huella normalizada |
|---|---:|---:|---:|---:|---|---|
| Original S_O · v1.0 (**antecedente oficial**) | 139 | 3 | 43 | 15 | 97,9 % | `7eb13353…` |
| Original S_O · v1.1 (diagnóstico) | 142 | 0 | 43 | 15 | 100 % | `7eb13353…` |
| Repetición S_O · v1.0 (solo comparación) | 139 | 3 | 43 | 15 | 97,9 % | `7eb13353…` |
| Repetición S_O′ · v1.0 (solo comparación) | 139 | 3 | 43 | 15 | 97,9 % | `7eb13353…` |
| **Repetición S_O · v1.1 (DEFINITIVA)** | **142** | **0** | **43** | **15** | **100 %** | `7eb13353…` |
| Repetición S_O′ · v1.1 (reproducibilidad) | 142 | 0 | 43 | 15 | 100 % | `7eb13353…` |

**Celdas que cambian entre pares de evaluaciones:**

| Pares comparados | Celdas distintas |
|---|---|
| Original v1.0 → repetición v1.1 | **Exactamente 3:** O03·A7, O14·A7 y O19·A8 (FALLO → APROBADO) |
| Original v1.0 → repetición v1.0 | 0 |
| Original v1.1 → repetición v1.1 | 0 |
| Repetición S_O → S_O′ (v1.1) | 0 |

**Ficheros derivados idénticos byte a byte:** semanas, adaptaciones, pasos de progresión y huella. Se comparó la original frente a la repetición, y S_O frente a S_O′.

## Por qué desaparecen los tres fallos: el oráculo aplica la regla, no la relaja

| Celda | v1.0 | v1.1 | Qué comprueba v1.1 en la repetición |
|---|---|---|---|
| O03·A7 | FALLO: «paso de progresión con la progresión apagada» | APROBADO | El único paso es una **RECUPERACION** de 899 en C3 (2 → 3 series = su dosis de referencia; volumen 66 → 72, techo 79). Nota del oráculo: «RECUPERACION con la progresión apagada (P-A1, REV2)». Se evalúa con sus reglas: no supera la referencia ni el techo, y no cambia la lista |
| O14·A7 | FALLO, mismo motivo | APROBADO | **RECUPERACION** de 253 en C4 (2 → 3 series; volumen 79 → 82, techo 90), con las mismas reglas |
| O19·A8 | FALLO: «853 vuelve sin REINCORPORACION», «no va al final» | APROBADO | 853 se busca en su linaje. Vuelve en C9 tras 2 PROGRESION en ese linaje (1179 en C4 y C7), al final de la sesión, con su dosis inicial exacta y volumen 84 ≤ 85 |

**Las validaciones siguen vigentes.** Las 19 pruebas negativas (`pruebas/pruebas_negativas_v1_1.csv`) demuestran que v1.1 sigue rechazando:
- una recuperación por encima de la referencia;
- una progresión con la progresión apagada o sin dos semanas;
- una reincorporación sin progresión previa, fuera del final o con otra dosis;
- la reaparición del retirado en su linaje y su aparición en otro linaje.

## Verificación por área (repetición S_O, v1.1; S_O′ idéntica)

**0 motivos de fallo en los 10 aspectos**, en las dos corridas.

| Área (orden §3) | Resultado |
|---|---|
| Generación y personalización | 19 planes válidos y O11 ESPERADO_SIN_PLAN (`FUERZA_SIN_TIEMPO`). A2: 20/20. Sesiones = min(días pedidos, tope del nivel, días disponibles); S-MAX en O07, O08 y O09; 0 sesiones por encima de los minutos del perfil |
| Registro y cálculo de adherencia | A3 19/19 y A4 19/19 (O11 NO APLICA). 256 sesiones S1–S4: 239 completas, 6 parciales, 10 omitidas y 1 sin registro. Observada, cobertura y ponderada iguales a `weekly_user_metrics` y a V32 |
| Adaptación y mantenimiento | La decisión del oráculo coincide con la del backend en **103/103 ciclos**. En C1–C4: 63 NO_CHANGE, 11 ADAPT_LOCALIZED y 2 ADAPT_EXPANDED. 15 sesiones adaptadas: 11 ACC y 4 CONS-J, 0 INC. A5 9 AP + 1 RR; A6 19/19 |
| Progresión y recuperación | 35 PROGRESION y 3 RECUPERACION, todas ≤ techo y como máximo 1 por ciclo. Ninguna PROGRESION sin dos semanas al 100 %. A7: 8/8 (6 del bloque P y 2 RECUPERACION P-A1 del bloque G) |
| Reincorporación | 2 REINCORPORACION: O19 en C9 y O18 en C22. O20: el excluido por dolor no vuelve. A8: 4/4 |
| Seguridad | A10 19/19: sin ejercicios prohibidos, techo respetado, intento 1 inválido de la IA nunca publicable, idempotencia del ciclo 1 |
| Continuidad multisemana | A9 19/19: plan vigente cada semana y `GET /plan/{final}` 200 |
| Reproducibilidad S_O/S_O′ | Huella `7eb13353…` idéntica en S_O, S_O′ y la corrida original. Matriz idéntica |

**Sin cambios en:** el backend, los perfiles, las reglas de negocio, los criterios de aceptación, los umbrales, V24, V25, V8C y HIST.
