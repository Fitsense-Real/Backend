# U06 y 4D v3.2 · cierre del único perfil sin plan

**Fecha:** 31/10/2026. **Rama:** `gm-60-series`, desde `generation` (`3aa48ec`). **Decisión aplicada:** opción B, aprobada el 31/10/2026 (`../ADENDA_4D_v3_2.md`).

**Resultado:**
- **U06 tiene plan y completa las 8 semanas con IA real.**
- La opción B resuelve **16 de las 24** combinaciones que fallaban.
- Las 8 restantes (GAIN_MUSCLE intermedio y avanzado en casa) necesitan otra decisión. No se forzaron.

## 1. Implementación (mínima)

| Pieza | Cambio |
|---|---|
| `MvpDosePolicy` | Solo `RELLENO_ZONA_PEQUENA` (`lower legs`, `waist`) tiene `seriesMax = 3`. Las otras tres clases, igual |
| Motor de reglas (ruta de GAIN_MUSCLE) | 1) composición de siempre; 2) repaso de V20 de siempre, que ahora no deja una sesión con menos de 2 grupos del enfoque (V15); 3) solo si el día sigue por debajo del mínimo, la serie adicional de una en una y dentro del presupuesto de 4D §5 |
| V24 | Acepta 3×13 en esa clase solo si con 2×13 la sesión baja del mínimo de V20. Si sobra: «Usa 2x13» |
| Prompt | `dosis_por_clase.series_max` y una línea en la regla 10, con la misma condición |
| Sin cambios | V15, V20, V23, V25, los otros tres objetivos, la progresión (desactivada en `application.properties`) y la reincorporación (devuelve la dosis base, 2×13) |

**Error de implementación encontrado y corregido:**
- En la primera versión, la serie adicional iba antes del repaso.
- Eso cambió planes de GAIN_MUSCLE de 45 min que el repaso ya resolvía: fallaban las 6 referencias.
- Con el orden correcto (primero la composición existente), las referencias vuelven a ser idénticas.

## 2. Las 24 combinaciones (`RespaldoSesentaMinutosIT`, sin IA)

| Combinaciones de GAIN_MUSCLE con 5–6 días, 60 min | Antes | Ahora |
|---|---:|---:|
| Principiante (gimnasio y casa), intermedio y avanzado de gimnasio: 4 patrones de días cada uno | 0/16 | **16/16** |
| Intermedio y avanzado en casa | 0/8 | 0/8 (pendiente) |

**Qué se comprueba en las 16:**
- Plan válido (V1–V25), con el mismo objetivo, capacidades y equipamiento.
- Toda colocación por encima de su dosis es relleno de zona pequeña a 3×13.
- Cada serie adicional es **necesaria**: con 2×13 la sesión baja de 2520 s.

**Las 8 pendientes:**
- **Dónde fallan:** el día PULL en casa, con 6 ejercicios de espalda a 3×10 y un curl de bíceps de relleno **estándar** a 2×10 (2430 s de 2520).
- **Por qué no las cubre la opción B:** no hay ejercicios estructurales de `upper arms` con el equipamiento de casa, y el bíceps no es zona pequeña.
- **Qué haría falta:** otra decisión; por ejemplo, la misma serie adicional en el relleno estándar del día PULL (2×10 → 3×10), con la misma condición de necesidad.
- **Reproducción:** `-Dfitsense.pendientes=true`.

## 3. Lo que no cambió

| Comprobación | Resultado |
|---|---|
| Referencias V35, V36, V37, V39, V40 (congeladas) y V41 (oficial), con GAIN_MUSCLE de 30, 45 y 60 min a 3 días | **Idénticas** |
| Los otros tres objetivos a 60 min con 5–6 días | 72/72, sin cambios |
| Simulaciones largas (barrido 4–6 días, robustez 288, restricciones 192, continuidad) | **Idénticas byte a byte** a `evidencia/v41/final/` |
| Regresión dirigida (27 clases: referencias, objetivos, adaptación, progresión, reincorporación, herencia, Replicate guardado) | 179/179 |
| Unitarias | Todas |

## 4. U06 durante 8 semanas con IA real

**Ejecución:** `CampanaMvp8UsuariosIT` solo con U06 (`-Dfitsense.campana.usuarios=U06`), con el mismo guion y el mismo calendario (S1 = 2/11/2026). Los otros siete usuarios **no se repitieron**.

**Presupuesto:** 35 llamadas como máximo (80 autorizadas − 45 de la campaña).

### Generación inicial (Replicate real)

| Intento | Latencia | Resultado |
|---|---:|---|
| 1 | 122 s | Rechazado: **tres 3×13 que sobraban** (V24 nueva: «con 2×13 dura 2592 s y el mínimo es 2520 s; usa 2×13»), además de ejercicios de duración sin dosis de hipertrofia |
| 2 | 99 s | Rechazado: V20 (tres sesiones, a 60–90 s del mínimo) y V24 (descansos de 165–170 s) |
| 3 | 127 s | **Aceptado (AI)**. LEGS lleva 1014 y 149 a 3×13; LOWER_BODY lleva 1014, 149 y 1105 a 3×13, y 815 a 2×13. Son las justas para el mínimo |

### Semana a semana

| S | Sesiones | C/P/O/SR | Adherencia (orden · backend) | Decisión → estado | IA | Progresión |
|---|---:|---|---|---|---:|---|
| 1 | 5 (PUSH, PULL, LEGS, UPPER, LOWER) | 5/0/0/0 | 100 · 100 | NO_CHANGE | 0 | — |
| 2 | 5 | 5/0/0/0 | 100 · 100 | NO_CHANGE | 0 | 517: 3×10 → 3×11 (+1,43 %) |
| 3 | 5 | 5/0/0/0 | 100 · 100 | NO_CHANGE | 0 | 354: 3×10 → 3×11 (+1,43 %) |
| 4 | 5 | 5/0/0/0 | 100 · 100 | NO_CHANGE | 0 | **1014: 3×13 → 3×14** (+1,32 %), sobre una dosis ampliada |
| 5 | 5 | 4/0/1/0 | 80 · 80 | NO_CHANGE | 0 | — (no se cumplen las dos semanas) |
| 6 | 5 | 5/0/0/0 | 100 · 100 | NO_CHANGE | 0 | — |
| 7 | 5 | 5/0/0/0 | 100 · 100 | NO_CHANGE | 0 | 517: 3×10 → 3×11 (+1,43 %) |
| 8 | 5 | 5/0/0/0 | 100 · 100 | NO_CHANGE | 0 | 815: 2×13 → 2×14 (+0,86 %) |

**Resultados:**
- Las 8 semanas se publicaron.
- Todos los pasos de progresión quedaron ≤ 5 % y bajo el techo de 1,10 × base.
- 0 violaciones de linaje y 0 de las comprobaciones independientes.
- Las marcas «INVALIDA» de la validación independiente son las dosis de progresión autorizadas (D-P3), como en la campaña principal.

**S5:**
- La persona omite el LEGS por menos tiempo habitual (31 min declarados).
- Con 4 de 5 sesiones completas, la adherencia observada es **80 %**, el umbral bueno, así que la política vigente **no adapta** y no llama a la IA.
- La semana siguiente es la misma copia y la persona la cumple.
- Por eso el guion «cumple el plan adaptado» de S6 no tuvo adaptación que cumplir. Es la política vigente, no un fallo. Pasa igual con cualquier perfil de 5 o más sesiones y un solo día perdido.

### Replicate

| | |
|---|---|
| Llamadas reales | **3** (solo la generación); sin errores |
| Tokens | 235 186 de entrada y 37 318 de salida |
| Cómputo | 340,7 s; espera total de 5,8 min |
| Coste estimado | **≈ US$ 0,13** (la cifra oficial es la del panel de Replicate) |
| Acumulado de la orden | 45 + 3 = **48 de 80** llamadas, ≈ US$ 1,00 |

## 5. Adaptación, progresión, techo, recuperación y reincorporación con las dosis nuevas

**U06 en `ValidacionMultisemanaIT`** (ciclo real, IA simulada con los permisos de cada intervención): 7 secuencias, 7/7.
- **Qué incluye:** el perfil exacto de U06; el motivo cae en el domingo LOWER_BODY, que lleva las 3×13, y el sábado queda sin registro para que haya orden de adaptar.

| Secuencia | Resultado |
|---|---|
| Menos tiempo habitual | ACORTAR **APLICADA**, sin superar los minutos declarados |
| Dolor | SEGURIDAD **APLICADA**; el ejercicio no vuelve |
| Fatiga | REVISAR, SIN_EFECTO |
| Dificultad | VARIANTE, SIN_EFECTO |
| Desmotivación | INVESTIGAR, SIN_EFECTO |
| Imprevisto | CONSERVAR, sin IA |
| Cumple todo | NO_CHANGE |

En todas, cada semana se publica, las sesiones no señaladas no cambian y nada se recorta sin causa.

**Progresión y techo:** en la ejecución real de U06, 5 pasos ≤ 5 % y bajo el techo; uno sobre una colocación 3×13 (1014 → 3×14). La regresión de `ProgresionIT`, `PendientesProgresionIT`, `ProgresionCienPorCienIT` y `HerenciaD8IT` está en verde.

**Recuperación y reincorporación:**
- `CopiaSinIaIT` y `PendientesProgresionIT` siguen en verde.
- La reincorporación de un relleno de zona pequeña devuelve su dosis **base** (2×13), no la ampliada, porque la política sigue tomando la dosis de la clase.
- **No se ejercitó específicamente** una recuperación ni una reincorporación sobre una colocación 3×13: en U06 no hubo una reducción ni un ACORTAR seguidos de progresión en las 8 semanas.

## 6. Estado de los 8 usuarios

| | Antes | Ahora |
|---|---|---|
| Usuarios con plan inicial | 7/8 | **8/8** |
| Semanas con plan válido | 56/64 | **64/64**: las 56 de la campaña, sin cambios, y las 8 nuevas de U06 |

**Por qué no se repitieron los otros siete:** sus perfiles no cambian con 4D v3.2. Seis no son GAIN_MUSCLE o no tienen 60 min con 5–6 días, y U01 es GAIN_MUSCLE de 45 min con 2 días. Las referencias, los barridos y las simulaciones son idénticos.

**Pendiente de decisión:** GAIN_MUSCLE intermedio y avanzado en casa con 5–6 días a 60 min (§2). Ninguno de los 8 usuarios lo es.

## 7. Evidencia

- `u06_4d_v32/`: CSV de las 8 semanas, las 3 predicciones originales (sin secretos), `eventos.log` y `resumen.json`.
- **Pruebas:**

  | Prueba | Qué cubre |
  |---|---|
  | `RespaldoSesentaMinutosIT` | 72 + 16 + U06 en CI; 8 opt-in |
  | `Adenda4Dv32IT` | V24 |
  | `ValidacionMultisemanaIT.secuenciaU06` | 7 secuencias con el perfil de U06 |
