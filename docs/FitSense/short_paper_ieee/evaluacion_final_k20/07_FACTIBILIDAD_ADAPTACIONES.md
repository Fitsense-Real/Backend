# 07 · Factibilidad de las adaptaciones

**Fuente:** `evidencia/final/S_control/factibilidad.csv`. La sonda del arnés se ejecuta antes del primer intento de la IA y no cuenta como llamada.

Para cada sesión señalada para la IA:
- enumera las **modificaciones simples** que permite su intervención:
  - una serie menos;
  - una o dos repeticiones menos;
  - 5 o 10 s menos;
  - 10 % menos de carga;
  - o, si se permite sustituir, cada sustituto del mismo patrón y zona;
- juzga cada una con el **productor y los validadores reales** (V8C, la validación estricta y el límite de dificultad).

Las entradas son las mismas en D, en S y en R (mismo plan inicial y mismos registros). La sonda **solo mide**.

| Usuario | Sesión | Intervención | Probadas | Válidas | Rechazos | ¿Factible? |
|---|---|---|---:|---:|---|---|
| U03 GM | S3 | Fatiga (solo dosis) | 21 | **0** | V24 = 14 (dosis 4D fija), V20 = 7 (series) | **No** |
| U04 GM | S2 | Demasiado difícil | 307 | 259 | 109/109 más fáciles válidos y 150/150 laterales válidos; 27/27 más difíciles rechazados (V8C); dosis: V24/V20 | Sí |
| U04 GM | S3 | Motivación (solo dosis) | 21 | **0** | V24 = 14, V20 = 7 | **No** |
| U08 FU | S3 | Fatiga | 18 | 4 | V9 = 8, V20 = 6 | Sí (repeticiones) |
| U09 FU | S2 | Demasiado difícil | 264 | 197 | 78/78 más fáciles; 53/53 más difíciles rechazados (V8C) | Sí |
| U09 FU | S3 | Motivación | 18 | 4 | V9 = 8, V20 = 6 | Sí |
| U13 LW | S3 | Fatiga | 18 | 6 | V16 = 8, V9 = 4 | Sí (series y segundos) |
| U14 LW | S2 | Demasiado difícil | 67 | 55 | Solo laterales (principiante: máximo dificultad 1); dosis válidas | Sí |
| U14 LW | S3 | Motivación | 18 | 6 | V16 = 8, V9 = 4 | Sí |
| U18 GF | S3 | Fatiga | 24 | 17 | V9 = 7 | Sí |
| U19 GF | S2 | Demasiado difícil | 84 | 77 | Solo laterales (principiante) | Sí |
| U19 GF | S3 | Motivación | 24 | 17 | V9 = 7 | Sí |

**Resumen:**
- Casos que requieren adaptación con IA: **8 usuarios**, con **12 sesiones**.
- **Técnicamente factibles: 10/12 sesiones** y **7/8 usuarios**.
- **No factibles (2): U03 S3 y U04 S3**, ambos GAIN_MUSCLE intermedio en gimnasio, 60 min.
  - La intervención (fatiga o motivación) **solo autoriza cambiar la dosis**.
  - En GAIN_MUSCLE, la dosis 4D/V24 es fija (3 × 10 con 90 s): bajar repeticiones la incumple.
  - Quitar una serie deja la sesión por debajo del piso V20 de 60 min (2.520 s).
  - Es una consecuencia demostrada de las reglas vigentes, que no se cambian. La conservación de esas dos sesiones es CONS-J.
- En los dos usuarios principiantes (U14, U19) no existe ningún sustituto **más fácil**: el catálogo elegible ya es de dificultad 1. La adaptación pertinente ante «demasiado difícil» es entonces una **reducción de dosis** (ACC) o un sustituto lateral (ALT).
- El límite de dificultad rechazó **80/80** sustitutos más difíciles.

**Coste en R:** `FactibilidadPreviaIA` no detecta el caso U03 (solo demuestra imposibilidades de ACORTAR o REPROGRAMAR). En R, U03 gastará hasta 3 predicciones sin ninguna posibilidad de éxito. Se informa como coste; no se cambia la versión congelada.
