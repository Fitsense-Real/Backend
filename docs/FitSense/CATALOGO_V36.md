# V36 · Corrección de nueve ejercicios del catálogo según sus GIF

**Fecha:** 29/10/2026 · **Rama:** `catalogo-v36` → `generation` (PR #4) · **Migración:** `V36__catalog_review_gif_corrections.sql`.

**Estado:** técnicamente lista para aprobar su integración, después del PR #3. La revisión humana doble e independiente sigue pendiente como requisito metodológico (§3).

**No se ha hecho:** no se ha ejecutado en Neon ni desplegado, no se ha llamado a Replicate y nada se ha fusionado.

**No cambia:** `src/main/java`, V15, V20, V22, V24, V25, V35, adaptación, progresión, D8, adherencia, seguridad, roles ni prompts. La corrección de V15 en la copia semanal está en el PR #3, no aquí.

## 1. Resumen

| Criterio de cierre | Resultado |
|---|---|
| El catálogo representa correctamente los ejercicios | Los 9 están corregidos y cotejados fotograma a fotograma con su GIF (§3) |
| Las rutinas nuevas se generan | 72/72 referencias VALIDA y COMPLETO. Barrido de 4–6 días: 72/72 con el PR #3. Robustez: 0/288 sin plan con el PR #3 |
| Las rutinas nuevas continúan a la semana siguiente | **96/96** en la combinación con el PR #3 corregido. 72/72 en la base: los 5–6 días de GENERAL_FITNESS y LOSE_WEIGHT no se generan sin el PR #3 |
| V28 íntegra y bien interpretada | V28 intacta. 872 ya no cuenta para LOWER_HIP (de 4 a 3 aprobados). Anexo de rectificación (§5) |

## 2. Migración V36

| Id | Columna | V35 | V36 |
|---:|---|---|---|
| 1267 | `is_active` | true | **false** (no se borra) |
| 1296 | `difficulty_level` | 1 | **3** |
| 1296 | `high_impact` | false | false (sin cambio: no hay fase de vuelo) |
| 1296 | instrucciones (es) | Elevación alterna de brazo y pierna | **Flexión de palanca larga** |
| 1333 | `equipment_id` / requisito | ab wheel / `1:ab wheel` | **body weight / ninguno** |
| 1333 | `high_impact` · `requires_floor` · `requires_overhead` | false · true · true | **true · false · false** |
| 1333 | `name_es` · instrucciones (es) | Rodar con rueda · rueda abdominal | **Carrera con zancadas amplias** · carrera |
| 872 | `movement_pattern` | HIP_EXTENSION | **TRUNK_FLEXION** (`waist/abs` y nivel 1 sin cambio) |
| 872 | `name_es` · instrucciones (es) | Levantamiento de cadera… · puente de glúteo | **Encogimiento invertido** · encogimiento |
| 1059 | `body_part` / `target` / `synergist` | waist / abs / quadriceps | **upper legs / quads / abs** |
| 527 | ídem | upper arms / biceps / forearms | **upper legs / quads / biceps** |
| 609 | ídem | upper arms / biceps / quadriceps | **upper legs / quads / biceps** |
| 725 | ídem | upper arms / biceps / shoulders | **shoulders / delts / biceps** |
| 760 | ídem | upper arms / biceps / forearms | **shoulders / delts / biceps** |

«Instrucciones (es)» son `instructions_es` e `instruction_steps_es`. `instructions_en`, el texto de la fuente, no se toca: la app muestra el español.

**Controles de la migración:**
- **Precondición estricta:** valores anteriores exactos, huella MD5 de las instrucciones de 872, 1296 y 1333, y requisito único de 1333.
- **Poscondición:** estado final, y que `instructions_es` sea los pasos unidos, sin restos del texto anterior.
- **Probada en PostgreSQL aislado:**
  - las otras 1367 filas, los requisitos y el historial V27/V28 dan huellas idénticas antes y después;
  - aplicarla dos veces, o con instrucciones alteradas, aborta sin aplicar nada.

## 3. GIF, dictamen y revisión humana

Expediente en `evidencia/catalogo_v36/revision_humana/`:
- `Dictamen_…` (sin modificar) y `APROBACION_DICTAMEN_V36.md`: origen IA, aprobación del responsable y condiciones;
- `VERIFICACION_GIF_V36.md` y `cotejo_gif_v36.py`: cotejo reproducible;
- `ANEXO_872_V28.md`;
- `REVISION_HUMANA_V36.md`, `material_ciego_v36.md` y `formulario_revisor_v36.csv`.

**Lo que muestran los GIF:**
- **1333:** la figura entera está en el aire en los fotogramas 5–8 y 22–26, así que el alto impacto queda confirmado.
- **872:** encogimiento invertido; la cadera no se extiende.
- **1296: la evidencia corrigió la hipótesis inicial.** El dictamen suponía una flexión pliométrica, pero manos y pies no se despegan del suelo en ninguno de los 12 fotogramas (y=107–109). Es una flexión de **palanca larga**. Se detuvo la corrección, se presentó el hallazgo y el responsable aprobó nivel 3 sin alto impacto.
- **Los otros seis** coinciden con el dictamen.

**Revisión humana:**
- El dictamen técnico (IA) está aprobado por el responsable.
- Las **dos revisiones humanas independientes siguen pendientes**: no hay formularios, ni consenso, ni revisores inventados.
- Es un pendiente metodológico. No bloquea las pruebas técnicas y no activa el filtro global de aprobación humana.

## 4. Instrucciones finales (`instruction_steps_es`)

**1296 · Push-up de Superman (palanca larga)**
1. Colócate boca abajo en posición de plancha, apoyado en las puntas de los pies, con los brazos totalmente extendidos por delante de la cabeza y las manos apoyadas en el suelo.
2. Activa el abdomen y los glúteos para mantener el cuerpo en línea recta desde la cabeza hasta los talones.
3. Baja el pecho hacia el suelo de forma controlada, sin despegar las manos ni los pies.
4. Empuja con los brazos y los hombros para volver a elevar el tronco a la posición inicial, sin arquear la zona lumbar.
5. Repite el número de repeticiones indicado. Es un ejercicio de palanca larga y muy exigente: mantén siempre el apoyo de manos y pies, sin saltos.

**1333 · Carrera con zancadas amplias**
1. Ponte de pie con el tronco ligeramente inclinado hacia delante y los brazos flexionados a unos 90 grados.
2. Impúlsate con una pierna y avanza con una zancada larga, llevando la rodilla contraria hacia delante y arriba.
3. En cada zancada los dos pies se despegan del suelo un instante: aterriza suave sobre la parte delantera del pie, con la rodilla ligeramente flexionada.
4. Coordina los brazos con las piernas: adelanta el brazo contrario a la pierna que avanza.
5. Encadena zancadas de forma continua durante el tiempo indicado, con el tronco firme y la respiración regular, en una superficie plana y con espacio libre delante.

**872 · Encogimiento invertido**
1. Túmbate boca arriba con las rodillas flexionadas, los pies apoyados en el suelo y los brazos a los lados, con las palmas hacia abajo.
2. Activa el abdomen y despega los pies del suelo, llevando las rodillas hacia el pecho sin estirar las piernas.
3. Al final del recorrido, enrolla la pelvis y despega ligeramente la cadera del suelo con la fuerza del abdomen, sin impulsarte con las piernas.
4. Baja la cadera y después los pies de forma controlada hasta volver a apoyarlos en el suelo.
5. Mantén la cabeza y los hombros apoyados durante todo el ejercicio y repite el número de repeticiones indicado.

## 5. 872 frente a V28

Ver `revision_humana/ANEXO_872_V28.md`.

- **V28 intacta:** migración, evento `APPROVED` de nivel 1, revisores (Juan y Daniel) y resultados.
- **La aprobación de V28 no valida la nueva identidad:** se revisó sobre un texto de puente de glúteo.
- **LOWER_HIP pasa de 4 a 3 aprobados** (122, 776, 878).
- **No se transfiere:** el filtro global de aprobación sigue apagado.
- **Nivel operativo:** 872 sigue en nivel 1.
- **Sin eventos nuevos.**
- **Pendiente:** la revisión humana de 872 como encogimiento invertido.

`CatalogoV36IT` comprueba los conteos, la integridad de V28 y que el filtro siga apagado.

## 6. Banco del ejercicio 725 · aplazado

El GIF muestra un banco con respaldo y el modelo podría exigirlo. Pero 704, 723, 726, 739 y 740, sentados en banco en su GIF, tampoco lo exigen.

**Decisión del responsable:** aplazar la revisión general. Queda como inconsistencia documentada, sin migración en esta fase.

## 7. Las 72 referencias: V35 frente a V36

**Archivos:**
- **Oficial desde V36:** `evidencia/progresion/generacion_referencia.txt` (`GeneracionReferenciaIT`), recapturada con los nueve ejercicios.
- **V35 como evidencia:** `generacion_referencia_v35.txt` (`GeneracionReferenciaV35IT`, Flyway hasta V35). Su reproducción demuestra que el código de generación no cambió.

| Medida | V35 | V36 |
|---|---:|---:|
| Perfiles VALIDA · cobertura COMPLETO | 72 · 72 | **72 · 72** |
| Sesiones con otro día o enfoque | — | **0** |
| Sesiones con otros ejercicios o dosis | — | 213 de 216 (72 perfiles) |
| · directas (contienen alguno de los nueve) | — | 77 |
| · indirectas (selección determinista) | — | 136 |
| Otros minutos estimados | — | 26 sesiones (±4 min) |
| Colocaciones que la clasificación correcta no admite | 6 (1296 por encima del nivel: 3 en principiantes y 3 en intermedios) | **0** |
| Por debajo del 70 % exacto (V20, sin cambiar) | 17 | 18 |

**Patrones:**
- HIP_EXTENSION pasa de 43 a 29, porque 872 deja de contar como cadera;
- HIP_HINGE pasa de 88 a 102;
- las dosis salen de las mismas familias y no aparece ninguna dosis nueva.

**Por qué cambian sesiones sin ninguno de los nueve.** El motor siembra con `userId × 1 000 003 + díaDeLaSemana` y sortea sobre listas ordenadas de candidatos. 1267 era elegible para los 72 perfiles, y 1296 sale de las listas de principiantes e intermedios. Con listas distintas, la misma semilla elige otros índices. No se modificó el algoritmo.

Ninguna diferencia nueva incumple una política: los 72 son VALIDA con el validador de producción, con cobertura completa y 0 colocaciones inadmisibles.

Tablas completas en `evidencia/catalogo_v36/resultados_comparacion.md`; detalle por sesión en `referencias_sesiones_cambiadas.tsv`.

## 8. Barrido, robustez y continuidad

| Medida | Base sin V36 | Base + V36 | PR #3 sin V36 | PR #3 + V36 |
|---|---:|---:|---:|---:|
| Barrido 4–6 días: VALIDA | 60/72 | 60/72 | 72/72 | **72/72** |
| Sesiones = días pedidos | 44/60 | 44/60 | 56/72 | 56/72 |
| Robustez 288: sin plan | 52 | 52 (los mismos) | 0 | **0** |
| 1267 colocado | 44 | 0 | 51 | **0** |
| Combinados en PULL | 21 | 0 | 31 | **0** |
| Dificultad por encima del nivel (1296) | 6 | 0 | 7 | **0** |
| Cobertura | COMPLETO 60 | COMPLETO 60 | COMPLETO 72 | **COMPLETO 72** |

«PR #3» es el PR #3 corregido (fase 3). Su generación es idéntica a la anterior.

**Continuidad semanal de planes nuevos.** Se prueba con el ciclo real: la mitad de los perfiles cumple todas sus sesiones y la otra mitad no registra nada.
- **`CatalogoV36IT`, PR #3 + V36:** **96/96** publican la semana 2 (SIN_ORDEN, CONSERVED), sin 1267 ni combinados en PULL y sin llamar a la IA.
- **`CatalogoV36IT`, base + V36:** 72/72; los otros 24 no se generan sin el PR #3.
- **`ContinuidadSemanalIT` del PR #3, combinada con V36:** 96/96. Los 8 LOSE_WEIGHT de 5–6 días tienen las semanas 1 y 2 válidas para el validador de producción y con cobertura COMPLETA.

El defecto de copia de esos 8 lo corrigió el PR #3 (fase 3, `CORRECCION_RESPALDO_5_6_DIAS.md` §5).

## 9. Pruebas

| Prueba | Catálogo | Qué demuestra |
|---|---|---|
| `CatalogoV36IT` (7) | V35 → V36 con Flyway | <br>1. Matriz de los nueve e instrucciones. <br>2. Huellas intactas del resto, de V27/V28 y de los planes publicados. <br>3. LOWER_HIP de 4 a 3 aprobados y filtro apagado. <br>4. Elegibilidad: 1267 nunca; 1296 solo para avanzados (también con «sin impacto» declarado y en la edad sin impacto) y respeta «sin brazos arriba»; 1333 sin rueda y nunca para principiantes; 725 y 760 con brazos arriba. <br>5. Planes nuevos: 1296 solo en avanzados, sin 1267 ni combinados en PULL. <br>6. Continuidad. <br>7. Evidencia V35→V36 de planes sintéticos |
| `GeneracionReferenciaIT` / `GeneracionReferenciaV35IT` | V36 / V35 | Referencia oficial / histórica |
| `DuracionForzadaIT`, `HerenciaD8IT`, `ProgresionIT` | V36 | Comportamiento vigente. En D8 y progresión, la premisa del viernes acortado se busca entre participantes; no cambia ninguna aserción |
| `DuracionForzadaPlan60IT`, `Plan60HipertrofiaIT` | V35 | Históricas: el plan 60 tal como se publicó |

**Planes sintéticos de V35.** No hay compatibilidad retroactiva, por decisión del responsable. Los planes 60 y 61 y la prueba 7 quedan como evidencia del cambio. `consultas/exposicion_v36.sql` es un diagnóstico opcional.

## 10. Regresión y CI

| Ejecución | Unitarias | Integración | Resultado |
|---|---|---|---|
| `catalogo-v36` (PR #4) | ver CI | ver CI | ver CI del PR #4 |
| PR #3 corregido, local | 836/836 | 264, 0 fallos, 10 omitidas | **BUILD SUCCESS**. CI del PR #3 en verde (5145531) |
| **Combinación PR #3 corregido + V36**, local | 836/836 | 272, 0 fallos, 10 omitidas | **BUILD SUCCESS** |

Las omitidas son opcionales: Replicate real, Neon y evaluaciones manuales.

## 11. Pendientes

| Pendiente | Efecto en el MVP |
|---|---|
| Dos revisiones humanas independientes | Ninguno técnico; afecta a la trazabilidad académica. El expediente está listo |
| Banco de los ejercicios sentados (aplazado) | Ninguno inmediato |
| Aprobación de la fusión del PR #3 y después del PR #4 | Hasta entonces, la combinación solo se ha validado en local |
