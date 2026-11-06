# Tablas de la evaluación con Replicate REAL (R_O y R′_O)

**Origen:**
- Respuestas reales del modelo; adherencia simulada por guion.
- Valores obtenidos de `analisis/` y de los oráculos.
- Las tablas 1–5 de cada corrida están en `R_O/TABLAS_SHORT_PAPER.md` y `Rp_O/TABLAS_SHORT_PAPER.md`.

## Tabla 6. Aceptación de las propuestas de Replicate

| Corrida | Ciclos con orden de IA | Predicciones | Tokens entrada / salida | USD medido | Latencia mediana / máx. (s) | Aceptadas en el intento 1 | Aceptadas tras reintento | Intentos rechazados por los validadores | SIN_EFECTO propuesto por la IA | Respaldo determinista | Errores del proveedor | Adaptación de la IA publicada |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| R_O | 10 | 10 | 281.633 / 22.784 | 0,166 | 47,8 / 63,1 | 8 | 0 | 0 | 1 (O18; evitable: sí) | 1 (tras error del proveedor: 1) | 1 | 8/10 |
| R′_O | 10 | 10 | 334.826 / 26.838 | 0,1374 | 38,2 / 59,9 | 10 | 0 | 0 | 0 | 0 | 0 | 10/10 |

## Tabla 7. Pertinencia de las sesiones adaptadas (oráculo v1.3)

| Corrida | Sesiones con orden de adaptar | ACC (pertinente) | ALT (alternativa válida) | CONS-J (conservación justificada por la política) | INC (inadecuada) |
|---|---|---|---|---|---|
| R_O | 12 | 10 | 0 | 2 | 0 |
| R′_O | 12 | 10 | 1 | 1 | 0 |

## Tabla 8. Reproducibilidad R_O frente a R′_O

| Comparación R_O frente a R′_O (ciclos con orden de IA) | Misma decisión e intervención | Misma clasificación del resultado | Misma pertinencia | Mismo cambio exacto |
|---|---|---|---|---|
| 10 | 10/10 | 8/10 | 8/10 | 1/10 |

## Tabla 9. Diez aspectos: IA simulada frente a Replicate real

| Evaluación | APROBADO | FALLO | NO APLICA | REQUIERE REVISIÓN | Aprobación técnica |
|---|---|---|---|---|---|
| IA simulada · repetición S_O · oráculo v1.1 (9ae4c49) | 142 | 0 | 43 | 15 | 142/142 = 100.0 % |
| Replicate real · R_O · oráculo v1.2 (congelado) | 140 | 0 | 45 | 15 | 140/140 = 100.0 % |
| Replicate real · R_O · oráculo v1.3 | 140 | 0 | 45 | 15 | 140/140 = 100.0 % |
| Replicate real · R′_O · oráculo v1.2 (congelado) | 140 | 1 | 45 | 14 | 140/141 = 99.3 % |
| Replicate real · R′_O · oráculo v1.3 | 141 | 0 | 45 | 14 | 141/141 = 100.0 % |

## Tabla 10. Estados por aspecto (AP/FA/NA/RR)

| Aspecto | IA simulada AP/FA/NA/RR | R_O AP/FA/NA/RR | R′_O AP/FA/NA/RR |
|---|---|---|---|
| A1_generacion | 6/0/0/14 | 6/0/0/14 | 6/0/0/14 |
| A2_personalizacion_restricciones | 20/0/0/0 | 20/0/0/0 | 20/0/0/0 |
| A3_registro_ejecucion | 19/0/1/0 | 19/0/1/0 | 19/0/1/0 |
| A4_adherencia | 19/0/1/0 | 19/0/1/0 | 19/0/1/0 |
| A5_adaptacion_ia | 9/0/10/1 | 9/0/10/1 | 10/0/10/0 |
| A6_mantenimiento | 19/0/1/0 | 19/0/1/0 | 19/0/1/0 |
| A7_progresion | 8/0/12/0 | 8/0/12/0 | 7/0/13/0 |
| A8_reincorporacion | 4/0/16/0 | 2/0/18/0 | 3/0/17/0 |
| A9_continuidad | 19/0/1/0 | 19/0/1/0 | 19/0/1/0 |
| A10_seguridad_integridad | 19/0/1/0 | 19/0/1/0 | 19/0/1/0 |

