# Auditoría de las 48 predicciones reales de Replicate

**Fecha:** 31/10/2026. **Coste de la auditoría:** sin llamadas nuevas.

**Fuentes:**
- `../evidencia/` (45 predicciones de la campaña) y `../u06_4d_v32/` (3 de U06).
- Cada predicción guarda el prompt y la salida originales.
- Los rechazos de cada intento vienen de `eventos.log`, con su texto completo.

**Ficheros de esta carpeta:**
- `predicciones_48.json`: identificador, usuario, operación, intento, latencia, tokens, coste y rechazos de cada predicción.
- `RECHAZOS_IA.csv`: un problema por fila, con su regla, causa, si era evitable y qué cambio lo evita.

## 1. Resumen

| | Generación | Adaptación | Total |
|---|---:|---:|---:|
| Predicciones | 20 | 28 | 48 |
| Pasaron la validación | 5 | 10 (6 aplicadas, 4 sin efecto) | 15 |
| Rechazadas por reglas | 14 | 18 | 32 |
| Fallo del proveedor (timeout) | 1 | 0 | 1 |

- **Problemas individuales en los rechazos:** 115. Una propuesta suele tener varios.
- **Aceptadas al primer intento:** 4 de 22 operaciones (U01 S6 y las generaciones iniciales de U05, U07 y U08).
- **Respaldos:** 3 generaciones fueron al motor de reglas y 3 adaptaciones quedaron con la sesión original.
- **Coste:** ≈ US$ 1,01 en total, con 2989 s de espera.

## 2. Causa real de los 115 problemas

| Causa | Problemas | Reglas | Quién lo origina |
|---|---:|---|---|
| 1 · Restricción ausente | 3 | V18 en corrección, V12 | **Backend:** la corrección no enviaba el día contiguo ni las fechas ocupadas |
| 2 · Restricción ambigua | 20 | V24 en adaptaciones | **Backend:** «puedes bajar la dosis» no decía que el descanso no cambia ni los rangos admitidos. La IA acortó recortando descansos (120 → 60 s) |
| 3 · Clara, incumplida por la IA | 39 | V15, V24, V25, V23, V8C (patrón, sustituir donde no se puede), DIVISION | **Modelo** (en parte evitable con candidatos prefiltrados) |
| 4 · Error numérico o de duración | 24 | V17 (11), V20 (10), serie adicional 3×13 (3) | **Mixto:** la IA debía calcular minutos y segundos con una fórmula |
| 5 · Contexto contradictorio | 23 | V8C subidas (10) y volumen ante dolor (2), V24 GM de duración (11) | **Backend:** los principios 6, 8 y 9 piden progresar, pero en la corrección V8C lo prohíbe. En GAIN_MUSCLE se ofrecían candidatos de duración que V24 nunca admite, y el texto de ACORTAR permitía bajar una dosis que en GAIN_MUSCLE no se puede reducir |
| 6 · Interpretación del backend | 0 | — | Ningún rechazo resultó ser un error del validador |
| 7 · Operación imposible | 5 | U01 S2 y U04 S5 | **Datos:** con el tiempo declarado, ninguna operación permitida cabía |
| 8 · Proveedor | 1 | timeout de U06 | Proveedor |
| 9 · Defecto de implementación | 0 | — | — |

**Evitables:** 62 sí, 29 en parte y 24 no.

**Por predicción**, de las 33 rechazadas:
- 8 eran evitables del todo;
- 18 tenían algún problema evitable;
- 7 eran errores puros del modelo o del proveedor.

## 3. Hallazgos por regla

**V8C, subidas de dosis (10 en 4 ciclos: U02 S5, U04 S7, U05 S3, U08 S5).**
- La IA no lo hacía por capricho: el prompt de corrección incluye los principios P-1.3 completos.
- Esos principios dicen «COMPLETED: puedes mantenerlo o progresarlo» (9), «sube repeticiones» (6) y «sugiere más peso» (8).
- La corrección prohíbe todo eso. **Causa 5.**

**V24 en adaptaciones de objetivos por capacidades (20).** En ACORTAR, REVISAR e INVESTIGAR, la IA bajó la dosis cambiando el **descanso**:
- U04 S5: 2×8/120 → 2×8/60;
- U08 S7: seis ejercicios a 60 s;
- U02 S2: cardio con descanso de 180 s.

V24 admite reducciones solo en series, repeticiones o segundos, nunca en el descanso (`ProgresionDosisPolicy`), y eso no figuraba en el texto. **Causa 2.**

**V24 en GAIN_MUSCLE.** Tres causas distintas:
- **Candidatos de duración (11):** el snapshot de generación ofrecía candidatos de duración y «sin capacidad» con `clase_dosis = null`, y la regla 10 decía que «siguen rigiéndose por el punto 7». V24 los rechaza siempre en GAIN_MUSCLE, y el motor de reglas ya los excluye (`puedeDemostrarHipertrofia`). **Causa 5.**
- **ACORTAR en U01 S2:** el texto permitía «bajar la dosis», pero en GAIN_MUSCLE no hay reducciones autorizadas (`ProgresionDosisPolicy`: «4D sigue igual»). **Causa 5.**
- **U06, primer intento:** puso tres 3×13 de más. La condición de 4D v3.2 depende de la duración exacta en segundos, que la IA no tenía. **Causa 4.**

**V17 (11).**
- La IA debía declarar los minutos con una fórmula y falló por un 20–42 %, sobre todo al acortar.
- El backend ya sabe calcularlos (el estimador de V17).
- **Causa 4, evitable del todo:** basta con que el backend fije la duración declarada a partir del contenido.

**V20 (10).**
- Faltaban entre 6 y 378 s.
- La IA solo tenía la fórmula en minutos y `min_session_minutes`.
- **Causa 4, evitable en parte:** con segundos por clase y el mínimo en segundos.

**V12 (U07 S2).** REPROGRAMAR no recibía las fechas ocupadas por el resto de la semana, y la IA puso dos sesiones el lunes. **Causa 1.**

**V18 en corrección (U02 S3).** La corrección no envía el resto de la semana, así que la IA no podía saber qué había el día contiguo. **Causa 1.**

**V8C, patrón de sustitución (U02 S3, 5).**
- La regla 9 era clara.
- La IA sustituyó cardio y rodilla por cadera.
- **Causa 3**, evitable en parte con una lista de sustitutos admitidos por ejercicio.

**Imposibles (U01 S2 y U04 S5).**
- **U01:** 22 min declarados frente a una sesión de 37 de GAIN_MUSCLE. En GAIN_MUSCLE solo se puede retirar un relleno, que ahorra unos 4 min, y retirar cualquiera de los tres de la sesión rompe V15 (3 de 5 en `upper legs`).
- **U04:** 16 min para una sesión de 24 de GENERAL_FITNESS principiante. Hay más detalle en `FACTIBILIDAD_PREVIA_IA.md`.
- En los dos casos, la llamada no podía producir una adaptación válida. **Causa 7.**

**Proveedor (U06).** Un timeout de 172 s en el 2.º intento, después de 156 s en el 1.º. **Causa 8.**

## 4. Los tres intentos de U06 (4D v3.2)

| Intento | Problemas | Causa |
|---|---|---|
| 1 | Tres 3×13 innecesarias (con 2×13 duraba 2592 s y el mínimo es 2520 s); 750, 819 y 900 de duración, sin hipertrofia | 4 y 5 |
| 2 | V20 en tres sesiones (faltaban 60–90 s); descansos de 165–170 s (V24); 1105 a 2×10 en vez de 2×13 | 4 y 3 |
| 3 | Aceptado | — |

**Conclusión:**
- Con los segundos por clase y sin candidatos de duración, el 1.er intento habría tenido un único error de cálculo como mucho.
- Las 3×13 innecesarias muestran que `series_max` sin la duración exacta incita a usarlas. La regla y la validación decían lo mismo, pero la IA no podía comprobar la condición.

## 5. Errores del modelo y errores del backend

- **Modelo (causa 3):** 39 problemas.
  - V15: repartos con más de la mitad en un grupo.
  - V24: dosis que no corresponden a la capacidad.
  - V25: capacidad principal omitida.
  - V23: patrones obligatorios sin cubrir.
  - V8C: sustitución de otro patrón.
  - Se mantienen como responsabilidad de la IA. El validador funciona y la propuesta se rechaza.
- **Backend o contexto (causas 1, 2 y 5):** 46 problemas, que se corrigen en el contrato (`MEJORAS_CONTRATO_BACKEND_IA.md`).
- **Mixtos (causa 4):** 24 problemas. Se eliminan los de V17 y se reducen los de V20 entregando las duraciones calculadas.
- **Imposibles (causa 7):** 5 problemas. La comprobación previa evita la llamada (`FACTIBILIDAD_PREVIA_IA.md`).
