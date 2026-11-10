# Recomendación final de adopción · CONF-IA

**Fecha:** 10/11/2026 · **Rama:** `experimento/mejora-ia` · **Código evaluado:** `ab04e67`.
**Protocolo:** `PROTOCOLO_CONFIRMACION_IA.md`, congelado con huellas en `957a08d` **antes** de la primera llamada pagada.
**Restricciones respetadas:**
- no se activa nada, no se hace merge, no se despliega y no se abre PR de adopción;
- no se tocan V24, V25, V15/V26, las estructuras de los objetivos, el catálogo, la progresión, la reincorporación, Neon ni la app móvil;
- no hay funcionalidades, políticas ni validadores nuevos.

---

## Resumen en una frase

**Mantener PR-1.7 para la generación y V2.1 para la adaptación, con el V8C corregido.**
- pr-1.8a es la mejor candidata para una confirmación futura; pr-1.8c no.
- v2.2a no empeora nada y corrige un caso concreto (quitar el relleno en O18), pero su ventaja no está demostrada con estadística.

---

## 1. Respuestas en lenguaje sencillo

| Pregunta | Respuesta |
|---|---|
| **¿Cuál es la mejor versión de generación?** | Para usar hoy, **PR-1.7**, la vigente: ninguna variante cumple todos los criterios fijados de antemano. La más prometedora es **pr-1.8a**: más planes válidos a la primera (16/19 frente a 12/19 en la ronda pareada), sin V24 y sin empeorar la dosis. Con una sola ronda, la diferencia no es estadísticamente segura (p = 0,28) |
| **¿Qué mejoró?** | Con pr-1.8c y pr-1.8a la IA **acierta más a menudo a la primera** y **los rechazos bajan mucho**: de 35 a 20 con pr-1.8c en 3 rondas, y de 11 a 3 con pr-1.8a. Los errores de reparto por grupos (V15) y de techo semanal (V26), los más frecuentes con PR-1.7, casi desaparecen. Lo que más mejora es fuerza (de 3/12 a 9/12 con pr-1.8c y a 4/4 con pr-1.8a) y condición física general |
| **¿Qué no mejoró?** | (1) La mejora **no llega a ser significativa** (pr-1.8c p = 0,29 con 3 rondas; pr-1.8a p = 0,28 con 1). (2) **pr-1.8c generó 2 hallazgos V24** en ganancia muscular (O13 y O18), por 0 de PR-1.7. El backend los rechazó y nunca se publicaron, pero son justo lo que el criterio 2 prohíbe. (3) Con pr-1.8c y pr-1.8a aparecen **rechazos por duración (V4) en pérdida de peso**: 14 con pr-1.8c y 3 con pr-1.8a, por 0 de PR-1.7. Con pr-1.8c, pérdida de peso baja de 14/15 a 10/15 válidos a la primera |
| **¿Y la dosis?** | **Ninguna variante empeora la dosis ni el tiempo.** % de M medio (sin los respaldos por fallo del proveedor): PR-1.7 63,9; pr-1.8c 62,7; pr-1.8a 61,2 (PR-1.7 en C1: 64,7). Wilcoxon por perfil sin empeoramiento significativo (pr-1.8c p = 0,98; pr-1.8a p = 0,37). Grupos por debajo del objetivo que se podrían mejorar: 83 con PR-1.7 frente a 74 con pr-1.8c en 3 rondas, y 26 frente a 25 con pr-1.8a. Ninguna fuerza dosis mayores. La mayoría de los planes siguen en DOSIS_LIMITADA con cualquier prompt: es un límite del perfil y del catálogo, no del prompt |
| **¿Y los cuatro objetivos?** | Ver la tabla del §3. En resumen: **fuerza** mejora mucho con las dos variantes; **condición general** mejora; **ganancia muscular** queda igual, pero con pr-1.8c aparecen los 2 V24; **pérdida de peso** empeora con pr-1.8c por V4 |
| **¿Quedaron corregidos H-A1 y H-A2?** | **Sí.** 25 pruebas nuevas: 9 fallaban con el validador sin corregir y las 25 pasan con la corrección. Unitarias 1341/0 fallos; integración 593/0 fallos. En las 132 sesiones adaptadas con IA real no pasó ningún cambio no autorizado. Ver `CORRECCION_V8C_HA1_HA2.md` y `PRUEBAS_Y_REGRESIONES.md` |
| **¿Cuál es la mejor versión de adaptación?** | **V2.1 y v2.2a quedan prácticamente empatadas.** v2.2a aplicó 60/60 sesiones y V2.1 58/60. Las 2 de V2.1 son el mismo caso, O18 (quitar una de relleno al acortar), que quedó sin efecto en D1 y D2. v2.2a lo resolvió en las 5 rondas. McNemar: 2 a 0, p = 0,50. No hay ventaja estadística |
| **¿Hubo cambios no autorizados?** | **Ninguno**, con ninguna de las dos versiones ni en el flujo conjunto, por identidad y en V8C. El oráculo marcó una sesión de O12 con v2.2a como «sustituto más difícil». Es un artefacto de su emparejamiento por posición. Por identidad, que es la medida del protocolo, la sesión retira dos ejercicios de nivel 2 y añade solo uno, del mismo patrón (peso muerto 1233 → 715), y el trabajo baja de 1686 a 740. No es un error ante dolor |
| **¿Funcionan juntas las mejoras?** | **Sí, dentro del alcance probado** (`RESULTADOS_VALIDACION_INTEGRAL.md`). Con generación con IA (PR-1.7), adaptación con v2.2a y V8C corregido: 0 fallos en los 10 aspectos del oráculo, 12/12 adaptaciones aplicadas, 0 cambios no autorizados, y progresión y reincorporación aprobadas. Es una sola ronda, sin pr-1.8a dentro |
| **¿Qué variante recomiendo?** | Generación: **PR-1.7** (mantener). Adaptación: **V2.1** (mantener); v2.2a es aceptable si se valora su coherencia (ver §4). V8C: **adoptar la corrección** H-A1/H-A2 |
| **¿Cuánto costó?** | **USD 7,10 de los 10 autorizados**: generación 4,93, adaptación 1,38 y flujo conjunto 0,78, en 336 predicciones. Separado de MEJ ronda 2. Sobran USD 2,90, que no se gastan |

---

## 2. Generación: resultados frente a los criterios fijados

### pr-1.8c frente a PR-1.7 (C1–C3, 19 perfiles, 57 pares perfil × ronda)

| Criterio (§6.1) | PR-1.7 | pr-1.8c | ¿Cumple? |
|---|---|---|---|
| 1. Válido a la primera: Wilcoxon por perfil, p < 0,05 | 34/57 (IC 95 % 47–71 %) | 41/57 (IC 95 % 59–82 %) | **No** (p = 0,29; 8 perfiles mejor, 4 peor, 7 igual) |
| 2. V24 de la variante ≤ control | 0 | **2** (O13 y O18 en C3, ganancia muscular) | **No** |
| 3a. Estructurales (V2, V13, V18, V23, V25) ≤ control | 7 | 6 | Sí |
| 3b. Respaldos por rechazos agotados ≤ control | 0 | 0 | Sí |
| 4. Ningún plan publicado con violaciones | 0 | 0 | Sí |
| 5. Dosis y tiempo sin empeoramiento significativo | — | p = 0,98 (% de M); p = 0,35 (grupos mejorables) | Sí |
| **Veredicto** | | | **NO CUMPLE** |

Por ronda (McNemar exacta):
- C1 y C2: 6 a 3 (p = 0,51);
- C3: 5 a 4 (p = 1,00).

### pr-1.8a frente a PR-1.7 (A1 pareada con C1)

| Criterio (§6.1) | PR-1.7 C1 | pr-1.8a A1 | ¿Cumple? |
|---|---|---|---|
| 1. Válido a la primera, p < 0,05 | 12/19 (IC 41–81 %) | 16/19 (IC 62–94 %) | **No** (p = 0,28; 7 mejor, 3 peor, 9 igual) |
| 2. V24 ≤ control | 0 | 0 | Sí |
| 3a. Estructurales ≤ control | 1 | 0 | Sí |
| 3b. Respaldos por rechazos ≤ control | 0 | 0 | Sí |
| 4. Ningún plan publicado con violaciones | 0 | 0 | Sí |
| 5. Dosis y tiempo | — | p = 0,37 (% de M); p = 0,40 (grupos mejorables) | Sí |
| **Veredicto** | | | **FAVORABLE NO CONCLUYENTE** |

- Solo se pudo hacer **una ronda** de pr-1.8a: el resto del presupuesto de generación no alcanzaba para otra completa, y no se corren rondas parciales (I-C-11).
- El protocolo dice qué hacer en este caso: «se recomienda mantener PR-1.7 o confirmar más».

### Análisis secundario (con la confirmatoria de la ronda 2; informativo, no decide)

| Variante | Rondas | Válidos a la primera (v / c) | Wilcoxon por perfil | V24 (v / c) |
|---|---|---|---|---|
| pr-1.8c | C1–C3 + R2–R3 (5) | 69/95 frente a 55/95 | p = 0,078 | 5 frente a 2 |
| pr-1.8a | C1 + R2–R3 (3) | 43/57 frente a 33/57 | p = 0,103 | 1 frente a 2 |

- Con todo junto, pr-1.8a sigue mejorando la validez sin aumentar V24.
- pr-1.8c, en cambio, acumula más V24 que el control.

## 3. Los cuatro objetivos (válidos a la primera; V24; DOSIS_LIMITADA entre los planes medibles; % de M)

| Objetivo | PR-1.7 (3 rondas) | pr-1.8c (3 rondas) | pr-1.8a (1 ronda) |
|---|---|---|---|
| Ganancia muscular | 12/15 · V24 0 · 13/15 · 56 % | 13/15 · **V24 2** · 11/14 · 58 % | 5/5 · V24 0 · 4/5 · 60 % |
| Condición física general | 5/15 · 0 · 15/15 · 62 % | 9/15 · 0 · 15/15 · 60 % | 3/5 · 0 · 5/5 · 60 % |
| Fuerza | 3/12 · 0 · 3/11 · 63 % | **9/12** · 0 · 4/12 · 61 % | **4/4** · 0 · 0/4 · 56 % |
| Pérdida de peso | **14/15** · 0 · 15/15 · 83 % | **10/15** (14 rechazos V4) · 0 · 14/14 · 84 % | 4/5 (3 rechazos V4) · 0 · 5/5 · 81 % |

- Las estructuras de los cuatro objetivos, V24, V25 y los límites de V15/V26 **no se tocaron**.
- La generación inicial no pasa por V8C, y las referencias de generación de la regresión siguen idénticas.

## 4. Adaptación: v2.2a frente a V2.1 (D1–D5, con el V8C corregido en las dos)

| Criterio (§6.2) | V2.1 | v2.2a | ¿Cumple? |
|---|---|---|---|
| 1. Cambios no autorizados (identidad y V8C) | 0 | 0 | Sí |
| 2. Aplicadas / INC por identidad / SIN_EFECTO evitables | 58/60 · 0 · 2 | 60/60 · 0 · 0 | Sí, no es peor |
| 3. Errores ante dolor o fatiga | 0 | 0 (O12 aclarado en §1) | Sí |
| 4. Ventaja medida | — | McNemar 2 a 0, p = 0,50; Wilcoxon por sesión n = 1 | **Sin ventaja demostrada** |
| **Veredicto** | | | **NO PEOR, SIN VENTAJA DEMOSTRADA** |

**Casos prioritarios:**
- **O18 (quitar relleno al acortar):**
  - V2.1 quedó SIN_EFECTO en 2 de 5 rondas (en la ronda 2 también en 1 de 3);
  - v2.2a retiró una de relleno en 5 de 5 (y en 3 de 3 en la ronda 2);
  - es la única diferencia real entre las dos.
- **O12 (dolor):** las dos versiones aplicaron siempre.
  - Casi siempre retiran ejercicios y bajan el trabajo: ALT en 4 de 5 rondas con cada versión, ACC en D5 con V2.1.
  - En D1, v2.2a sustituyó los 8 uno por uno (el caso aclarado en §1).
  - Nunca subió el trabajo.
- **O04 (identidad, no posición):** aplicada y pertinente con las dos versiones en las 5 rondas. Siempre una variante del mismo patrón o una bajada de dosis.

**¿Se justifica adoptar v2.2a por coherencia?** Es defendible, aunque no está demostrado con estadística:
- **A favor:**
  - corrige el único defecto que se repite en V2.1 (O18), en las dos campañas;
  - no empeora nada medible;
  - no generó ningún cambio no autorizado;
  - funcionó dentro del flujo conjunto.
- **En contra:**
  - la diferencia es un solo caso (n = 1 sesión con discordancia);
  - en D3, O19, su primer intento fue rechazado por V8C, aunque el reintento sí se aplicó.

**Recomendación:**
- Mantener V2.1 por defecto.
- Si se quiere adoptar v2.2a, presentarla como una mejora de coherencia en un caso concreto (acortar quitando una de relleno), **no** como una mejora estadística.

## 5. Qué se recomienda adoptar (sin activar nada)

| Pieza | Recomendación | Motivo |
|---|---|---|
| **V8C corregido (H-A1, H-A2)** | **Adoptar** | Aplica la política que ya existía; demostrado con pruebas; sin regresiones; sin efectos negativos en corridas reales |
| **Generación** | **Mantener PR-1.7** | Ninguna variante cumple todos los criterios congelados |
| pr-1.8c | **No adoptar** | Más V24 que el control (2 en esta campaña, 5 en total con la ronda 2) y rechazos por duración en pérdida de peso |
| pr-1.8a | **Candidata para confirmar** con 2 rondas más pareadas (unos USD 1,0, ver costes) | Todos los criterios menos la significación; sin V24 en esta campaña |
| **Adaptación** | **Mantener V2.1**; v2.2a es opcional, por coherencia | Empate estadístico; v2.2a corrige O18 |

**Combinación utilizable hoy:** PR-1.7 + V2.1 (o v2.2a) + V8C corregido.
- Es la única combinación con todas sus partes dentro de los criterios fijados.
- El flujo conjunto con v2.2a funcionó sin fallos.

## 6. Limitaciones

- **19 perfiles sintéticos** de la cohorte O; no son usuarios reales. Las repeticiones de un perfil no son independientes, por eso la unidad de análisis es el perfil.
- **pr-1.8a: una ronda** (19 pares).
- **Flujo conjunto: una ronda.** Abarca las semanas medidas, no las 26.
- **Proveedor:** 5 predicciones superaron el tiempo de espera de 150 s, 3 tuvieron error de E/S (recuperadas y validadas sin coste) y 1 falló en el proveedor. Se trataron con la regla I-MEJ-1, sin repetir nada de forma selectiva (`INCIDENCIAS_CONF_IA.md`). Las latencias son iguales con todos los prompts (mediana unos 85 s).
- Los planes de respaldo por fallo del proveedor (2) se quitan de las medidas de dosis y tiempo en las dos condiciones. Es la misma regla que el instrumento ya aplicaba a las respuestas recuperadas (`depurar_respaldos_proveedor.py`).
- Los criterios no se relajaron tras ver los resultados. El veredicto lo calcula el script congelado (`entregables_conf_ia.py`, SHA-256 `49665e6a…`).

## 7. Evidencia

`EVIDENCIA_CONFIRMATORIA.zip` contiene:
- todas las corridas `CONF_*` (prompts, respuestas originales, trazas, validaciones);
- las respuestas recuperadas y su validación;
- los oráculos;
- las listas de la cuenta;
- las métricas y el registro de presupuesto y versiones.

No contiene credenciales: se ha revisado que no aparecen `r8_` ni `Bearer`.

Resto de entregables en esta carpeta:
- `RESULTADOS_GENERACION_CONFIRMATORIA.csv`;
- `RESULTADOS_ADAPTACION_CONFIRMATORIA.csv`;
- `COMPARACION_FINAL_PROMPTS.xlsx`;
- `PREDICCIONES_Y_COSTOS_CONFIRMATORIA.csv`;
- `decisiones_conf_ia.json`.
