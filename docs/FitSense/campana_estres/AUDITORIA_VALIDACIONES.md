# Auditoría de validaciones e invariantes

**Fuentes:** `evidencia/real/` (`ADHERENCIA_SEMANAL.csv`, `eventos.log`, `FRECUENCIA_SEMANAL.csv`) y la consulta `docs/FitSense/consultas/linaje_v31_v32.sql`.

## 1. Validación independiente de las semanas publicadas

`ValidacionIndependiente` reconstruye cada autorización desde V31/V32 en crudo y valida con ella.

| Resultado | Semanas |
|---|---:|
| VALIDA (solo dosis base) | 32 |
| VALIDA_CON_AUTORIZACIONES (paso, heredada, reducción o fecha reducible) | 37 |
| INVALIDA | **0** |
| NO_CONCLUYENTE | **0** |
| Sin plan (U07 ×10 y U06 S6) | 11 |

Los identificadores del instrumento son BASE, PASO, REDUCCION, HEREDADA, REINCORPORACION, SIN_AUTORIZACION y AMBIGUO. La serie adicional de 4D cuenta como BASE porque V24 la condiciona.

**Que sigue detectando violaciones:** `ProgresionIT.validacionIndependienteDistingue` pone +1 repetición a mano y obtiene INVALIDA.

## 2. Rechazos de los validadores sobre las propuestas de la IA

Las propuestas rechazadas nunca se publicaron.

| Regla | Intentos rechazados con esa regla | Dónde |
|---|---:|---|
| V15 (grupos del enfoque y mitad) | 5 | U03 ×2, U06, U07 ×2 |
| V20 (mínimo de duración) | 3 | U02, U06 ×2 |
| V4 (techo del perfil) | 2 | U03 (38 min con techo 35), U07 (21 con techo 17) |
| V23 (patrones 4C) | 3 | U07 ×3 |
| V24 (dosis 4D o de capacidad) | 3 | U01 (presupuesto 4D §5), U02 (3×13 innecesarias), U03 |

## 3. Invariantes

| Invariante | Resultado |
|---|---|
| V8C: la IA solo toca lo señalado, sin subir dosis | 2 adaptaciones aplicadas, sin violación |
| V1–V25 en lo publicado | 0 violaciones (validador del backend y validación independiente) |
| Linaje V31/V32 y un paso por ciclo | La consulta de linaje se ejecuta en las IT de la regresión; la campaña no registró violaciones |
| Frecuencia por nivel, sin 7 y sin fechas repetidas | 76/76 planes OK |
| Volumen ≤ 1,10 × base y paso ≤ 5 % | 32 pasos dentro de los límites |
| Sin restauración automática | Ningún ejercicio retirado volvió sin un paso de reincorporación |
| El retirado por dolor no se reincorpora | U08: el que dolió se sustituyó y no volvió. U06: el regenerado excluye 151 |
| Nunca se publica una sesión insegura | U06 S5: NO_FACTIBLE sin publicar. La regeneración posterior excluye lo que dolió (corrección de esta campaña) |
| Contabilidad V32 = LIVE + REPLAY + SIN_SALIDA + SIN_PRESUPUESTO | Sin discrepancias |
| Sin datos ≠ 0 % | INSUFFICIENT_DATA en las pruebas deterministas |
| Resultado de la API | ADAPTACION_APLICADA ×2, REPROGRAMACION_IMPOSIBLE ×1, NO_FACTIBLE_POR_SEGURIDAD ×1, PROGRESION_DEL_BACKEND ×33, SIN_ADAPTACION ×29 y RESPALDO_PROPUESTA_IA_INVALIDA ×3. Estos 3 son **SIN_PRESUPUESTO** del simulador, ver la campaña §3 |
