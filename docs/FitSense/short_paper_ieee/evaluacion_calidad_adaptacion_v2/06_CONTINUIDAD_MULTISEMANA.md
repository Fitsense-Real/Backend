# Continuidad multisemana y sesiones en el límite V20

**Pruebas deterministas, sin Replicate.** Base PostgreSQL efímera, servicios reales y laboratorio con `semanas=2`. Después del ciclo S1 → S2, se ejecuta el plan publicado para S2 con el mismo patrón (o con `secuencia_semana2` si el perfil la define) y se cierra el ciclo S2 → S3.

**Ejecuciones:**
- `evidencia/GUION_COMBINADOS_2S`: U01–U20, semana 1 con los guiones de combinaciones y semana 2 con el guion V2;
- `evidencia/R_GUION_2S`: R01–R08 con el guion V2;
- `evidencia/R_DETERMINISTA_2S`: R01–R08 con la IA apagada.

## 1. Sesiones exactamente en el mínimo V20

**Detalle:** `05_SESIONES_EN_LIMITE_V20.csv`. Cálculo con la réplica exacta del estimador, verificada antes contra los mensajes del backend.

| Origen | Apariciones | Sesiones distintas |
|---|---:|---|
| C1 (r1 + r2) | 2 | U08 S3 (las dos veces) |
| C2 (r1 + r2) | **5** (r1: U18 S3, U19 S2, U19 S3; r2: U08 S3, U18 S3) | 4: U08 S3, U18 S3, U19 S2 y U19 S3 |
| **Total con Replicate real** | **7** | **4** |

**La cifra del informe anterior (5 en C2) es correcta.** Distinguiendo sesiones, son 4 distintas en C2.

**Detalle de las 4:**

| Sesión | Objetivo | Minutos | Duración antes → después | Mínimo | Margen inicial → restante | Intervención | Cambio |
|---|---|---:|---|---:|---|---|---|
| U08 S3 | FU | 60 | 2556 → 2520 | 2520 | 36 → 0 | REVISAR_CARGA (fatiga) | 323 y 487: 3×10 → 3×8 |
| U18 S3 | AG | 45 | 2040 → 1890 | 1890 | 150 → 0 | REVISAR_CARGA (fatiga) | 1054: 3 series de 90 s → 2 |
| U19 S2 | AG | 45 | 2040 → 1890 | 1890 | 150 → 0 | VARIANTE (dificultad) | 853: 3 → 2 series |
| U19 S3 | AG | 45 | 2040 → 1890 | 1890 | 150 → 0 | INVESTIGAR (desmotivación) | 1054: 3 → 2 series |

**¿Redujo la demanda?**
- Sí: menos series o repeticiones, con la misma dosis en el resto. Responde al motivo en REVISAR_CARGA (fatiga) e INVESTIGAR.
- En U19 S2 (dificultad), quitar una serie de cardio reduce la carga, pero no simplifica la técnica (Q5).

**¿Gastó todo el margen?**
- Sí: el margen restante es 0.
- **Que sea mejor o peor gastarlo entero no se puede deducir de las reglas.** Con margen 0, la siguiente reducción de dosis ya no cabe (§2).

## 2. Segunda solicitud de reducción (continuidad)

En `GUION_COMBINADOS_2S`, la semana 1 llevó U08, U18 y U19 exactamente al mínimo (escenario #8), igual que las sesiones reales. En la semana 2 se repite el mismo motivo.

| Caso | Margen en S2 | Intento 1 | Intento 2 | V32 semana 2 | Plan S3 | Linaje |
|---|---:|---|---|---|---|---:|
| U08 S3 (REVISAR_CARGA) | 0 | Opción «NO cabe» (2×6 en 1106) → **rechazada por V20** (2352 / 2520) | V2: «sin solución», se omite | **SIN_EFECTO** | Conservado (CONSERVED), sin cambios | 0 |
| U18 S3 (REVISAR_CARGA) | 0 | «NO cabe» (2×9 en 101) → **rechazada por V20** | Se omite | **SIN_EFECTO** | Conservado | 0 |
| U19 S2 (VARIANTE) | 0 | «NO cabe» → **rechazada por V20** | Sustitutos de la **misma dosis** (135 → 119 y 1111 → 129): retiran 0 s | **APLICADA** | Publicado (AI), solo S2 cambia | 0 |
| U19 S3 (INVESTIGAR) | 0 | «NO cabe» → rechazada | Se omite | SIN_EFECTO (en el mismo intento) | Conservado | 0 |

**Respuestas a la fase 4.3:**

| Pregunta | Resultado |
|---|---|
| ¿Encuentra otra alternativa válida? | Solo donde la intervención permite sustituir con la misma dosis (VARIANTE, U19 S2). En REVISAR_CARGA e INVESTIGAR con margen 0, no hay ninguna, y V2 lo dice («OMÍTELA») |
| ¿Conserva la sesión? | Sí: SIN_EFECTO y plan conservado |
| ¿Rechaza las propuestas inválidas? | Sí: los 3 intentos de seguir reduciendo se rechazaron por V20 |
| ¿Usa el respaldo correspondiente? | No hizo falta: hubo respuesta válida (omitir). Con la IA apagada, la misma situación da RESPALDO (`R_DETERMINISTA_2S`) |
| ¿Mantiene el linaje? | Sí: 0 violaciones en las dos semanas |
| ¿Evita incrementos no autorizados? | Sí: 0 dosis subidas y 0 cambios de descanso en las dos semanas (`evidencia/seguridad.py`) |

**Lectura:**
- Una sesión en el mínimo es un estado **estable y válido**: el backend la conserva.
- Pero **agota la capacidad de adaptación por dosis** de esa sesión para siguientes fatigas o desmotivaciones.
- Si eso es aceptable es una **decisión de política** (por ejemplo, reservar margen o permitir otra intervención). No se cambia V20.

## 3. Otros recorridos de dos semanas

`R_GUION_2S` y `GUION_COMBINADOS_2S`:
- **Reducciones acumuladas:**
  - U09: 3×10 → 3×9 en la semana 1, y → 3×8 en la semana 2;
  - R03: 2×10 → 2×9 → 2×8;
  - R07: margen 30 → 6 → 0.

  V8C compara con la semana medida, así que **cada semana se puede bajar un poco más mientras quepa**. Después de dos semanas, U09 y R07 llegan al mínimo.
- **R05 (PP principiante, 60 min) se genera ya con margen 0** (2520 = mínimo). Desde la primera semana:
  - INVESTIGAR no tiene ninguna opción de dosis, y V2 lo omite;
  - VARIANTE solo puede sustituir con la misma dosis.

  **Las sesiones sin solución por dosis no son exclusivas de GAIN_MUSCLE:** aparecen siempre que el plan inicial queda en el mínimo.
- **R04 (FU, 45 min) tiene margen 6.** Sus 4 ejercicios están en 3×6, que ya es el mínimo de repeticiones, y la única reducción posible (una serie menos) retira 168 s: REVISAR_CARGA queda SIN_EFECTO.
  - V2 lo describe bien (opciones «NO cabe» y «OMÍTELA»).
  - Aun así, imprime la línea genérica «Puedes combinar cambios…», que no aporta nada cuando ninguna opción cabe. Es una imperfección menor del texto, anotada en `08`.
- **Seguridad en las dos semanas** (7 ejecuciones, 4 de ellas con segunda semana): **0** cambios en sesiones no señaladas, **0** subidas de dosis, **0** cambios de descanso, **0** sesiones por debajo del mínimo y **0** violaciones de linaje.
