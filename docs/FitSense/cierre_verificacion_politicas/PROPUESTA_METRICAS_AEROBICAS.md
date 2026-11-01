# Propuesta de métricas aeróbicas (informativas)

**Estado:** diseño y prototipo experimental (`MetricaAerobica`, sin cablear), con pruebas unitarias. No es una validación, no cambia ningún plan y no cambia la dosis de cardio.

## 1. Qué se mide y qué no

| Magnitud | Fuente | Hoy disponible | Uso |
|---|---|---|---|
| Minutos de **trabajo** programados | Plan: series × segundos de los ejercicios de clase CARDIO | Sí | Informativo |
| Minutos de **bloque** programados | Lo anterior + pausas entre series de cardio | Sí | Informativo (es lo que ocupa la sesión) |
| Minutos **ejecutados** | `session_exercises` (segundos registrados) | Sí, si la persona registra la sesión | Informativo |
| Minutos **externos** (caminar, transporte activo, deporte, trabajo) | Solo si se registran | **No hay dato** | Futuro: requiere un campo nuevo |
| **Intensidad** | RPE o talk test por bloque | **No hay dato.** El RPE de sesión no basta | Sin intensidad, **desconocida** |
| Minutos **equivalentes** (moderado ×1, vigoroso ×2) | Solo con intensidad conocida | No | **Nunca se suponen**: con intensidad desconocida el valor es nulo |

**Reglas de diseño:**
1. **No suponer intensidad.** Los bloques de 90–120 s se describen como «intervalos», no como «vigorosos».
2. **Separar lo programado de lo ejecutado.** Un plan con 36 min programados y 12 ejecutados es otra situación.
3. **No juzgar la actividad total.** FitSense no conoce la actividad externa, de modo que no puede afirmar que alguien no cumple la recomendación de la OMS. La OMS cuenta toda la actividad («todo cuenta»), y la equivalencia vigoroso = 2 × moderado es una inferencia de sus rangos (ver `REVISION_CIENTIFICA_ACTUALIZADA.md` §6).

## 2. Aviso informativo

Texto implementado en `MetricaAerobica.aviso()`, con pruebas que impiden «incumpl…», «no cumples», «perder…» y «garantiza»:

> «Tu plan incluye unos X minutos de actividad aeróbica esta semana. La actividad que realizas fuera de FitSense (caminar, ir en bicicleta, hacer deporte) también cuenta.»

**Cuándo mostrarlo (propuesta):** en LOSE_WEIGHT y GENERAL_FITNESS, en el resumen semanal.
- No se muestra como alerta roja ni bloquea nada.
- No promete pérdida de peso.

**Variante con registro (futura):** «Esta semana registraste X minutos en FitSense y Y fuera». Solo se mostraría con datos reales.

## 3. Contrato de API propuesto (sin implementar)

Campo opcional en el resumen semanal del plan:

```json
"actividad_aerobica": {
  "minutos_trabajo_programados": 27.0,
  "minutos_bloque_programados": 33.0,
  "minutos_ejecutados": null,
  "minutos_externos": null,
  "intensidad": "DESCONOCIDA",
  "minutos_equivalentes": null,
  "aviso": "Tu plan incluye unos 33 minutos de actividad aerobica esta semana. ..."
}
```

- Retrocompatible: los clientes que no lo lean no cambian.
- Flutter no se toca en esta orden.

## 4. Datos que faltan para evaluar la actividad aeróbica total

1. La actividad fuera de la app: minutos y tipo.
2. La intensidad por bloque (RPE de 6–20 o 0–10, o talk test: «puedo hablar / me cuesta / no puedo»).
3. La duración real de cada bloque ejecutado. Hoy se registran los segundos por ejercicio, no por bloque.
4. Opcional: pasos o frecuencia cardiaca de un dispositivo. Requiere consentimiento y está fuera del MVP.

## 5. Por qué no se aumenta todavía el cardio programado

- La E de LW2 vale **si el cardio de la app es la única actividad aeróbica**. Sin datos externos no sabemos si lo es.
- Alargar los bloques **cambia la dosis 4D**, quita tiempo de fuerza y puede reabrir V20 y V24. Eso exige revisión profesional y aprobación.
- **El paso proporcionado es medir primero** (programado y ejecutado) y decidir con datos reales.
