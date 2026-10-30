# V38 · Corrección del catálogo: implementación candidata y resultados

**Fecha:** 30/10/2026. **Rama:** `catalogo-v37-1`. **Sin fusionar.**

**Orden aplicada:** «Decisiones D1–D7 y orden de implementación V37.1». Continúa `DIAGNOSTICO_V37_1.md`.

**Qué no se toca:**
- Neon, Railway, Replicate, `main` y producción;
- las políticas, los validadores (V15, V18, V20, V23, V24, V25), las dosis, la adaptación ni la progresión (que sigue apagada);
- las referencias oficiales e históricas.

**Material:**
- Migración candidata: `auditoria_catalogo/v38/candidata/V38__catalog_functional_equipment.sql`. Está **fuera** del directorio oficial de Flyway y se genera con `generar_v38.py` desde `cambios_v38.tsv`.
- Matriz: `auditoria_catalogo/v38/matriz_v38.tsv`.
- Evidencia: `evidencia/v38/`.
- Comparativa: `evidencia/v38/comparativa_v38.md`.

## 1. Resumen

| | V37 (oficial) | V37 + V37.1 | **V38 candidata** (V37.1 + V38) |
|---|---|---|---|
| Escenarios (de 192) con un ejercicio cuyo implemento, **según su GIF**, no está declarado | **126** | **105** | **0** |
| 72 referencias válidas | 72 | 72 | 70 |
| Barrido de 72 (4–6 días) con plan | 72 | 72 | 66 |
| Robustez de 288 | 288 | 288 | 264 |
| Continuidad de 96 | 96 | 96 | 88 |
| 192 restricciones publicados | 192 | 192 | 176 |
| Sesiones bajo el 70 % exacto | 0 | 0 | 0 |

(Todas las columnas con el código definitivo de D1.)

**Lectura:**
- V38 es la primera versión del catálogo en la que **ningún plan pide un implemento que el usuario no tiene**.
- Al quitar los atributos incorrectos que «hacían pasar» a V15, aparecen **24 perfiles sin plan**, todos por V15 (composición).
  - Con la etapa C sola (equipamiento) solo se pierden 2 de los 192 escenarios.
  - Sin el cambio de 1102 (etapa B) se pierden 4, todos de LOSE_WEIGHT.
- **No se ha restaurado ningún dato para recuperarlos.**
- Un ajuste de composición del motor, probado y **no aplicado** (E1/E2, §5), los recupera todos: 72/72, 288/288, 96/96 y 192/192.
- **Necesita tu autorización** (decisión E).

## 2. Qué contiene la V38 candidata (54 cambios en 43 ejercicios)

Mismo formato que V37:
- comprueba el estado previo de cada columna, y también `source_code` y `name_en`, y aborta si no coincide;
- comprueba el estado final;
- borra sus objetos temporales;
- no toca `exercise_difficulty_approvals`, las revisiones humanas, los planes ni la adaptación.

Se aplica en Flyway después de V37.1 (separada). No existe ninguna V38 en `src/main/resources/db/migration`.

| Etapa | Decisión | Cambios |
|---|---|---|
| A · cardio | D1 | 1117: `SETS_REPS` → `DURATION`, `upper legs`/`quads` → `cardio`/`cardiovascular system` |
| B · clasificación | D4 | 1102: `KNEE_DOMINANT` → `MOBILITY` (deja de cubrir LOWER_KNEE) |
| | D7 | 1106: `waist`/`abs` → `chest`/`pectorals`, `DURATION` → `SETS_REPS`, dificultad 1 → **2 provisional** |
| | D5 | 1111, 1113, 1115, 1262: `upper legs`/`glutes` → `chest`/`pectorals`. Patrón `HORIZONTAL_PUSH` y dificultad 1 **sin cambios** |
| | D6 | 558, 567: `upper arms` → `upper legs`, `target` → `quads` (precedente de V36 en 527 y 609) |
| C · equipamiento | D3 | 6 implementos nuevos y requisitos reales en 34 ejercicios (§4) |

**Código (separado de los datos): D1.**
- `GoalFeasibilityPolicy.enRevision` saca a 853 y 1117 de «en revisión» **solo cuando el catálogo ya tiene la clasificación revisada**: cardio continuo por tiempo.
- Con V35, V36 o V37 siguen en revisión. Verificado: la referencia de V37 queda **idéntica byte a byte** con este código.
- `EnRevisionD1Test` cubre los 6 casos.

### Evidencia por decisión

**D1 · 853 y 1117**
- GIF 3221 (853): marcha en el sitio con rodillas altas, sin fase de vuelo.
- GIF 3552 (1117): apoyos rápidos alternos sin vuelo; las instrucciones dicen «durante el tiempo».
- **Dosis:** la vigente de CARDIO para principiante, 3×90 s con 60 s de descanso. No se ha creado ninguna dosis. `CatalogoV38IT` comprueba que `CapabilityPrescriptionPolicy.dosis(CARDIO)` la devuelve, que ninguno tiene alto impacto ni suelo, y que 298 y 854 siguen fuera (D2).
- **V24 y V25:** las referencias, el barrido, la robustez y la continuidad validan con V24 y V25 activas: 0 rechazos de V24 o V25 en todas las variantes.
- **No se desbloquean para completar tiempo:** entran como cardio principal, igual que 253, 1054 y 1093.

**D4 · 1102**
- GIF 1687: paso atrás de pie con los brazos estirados por encima de la cabeza, sin flexión de rodilla significativa.
- **Grupo:** se mantiene `waist`. El GIF resalta abdomen y dorsal, y no hay evidencia para otro grupo.
- **Patrón:** `MOBILITY`. Capacidad: MOBILITY. Dosis vigente: 2×10 con 30 s.
- **Slot:** ninguno.
- **Efecto aislado** (etapa B sola): no deja ningún escenario sin plan.
- **Efecto con el equipamiento real** (B + C): GENERAL_FITNESS principiante de 4 a 6 días se queda sin plan (V15, §5).
- **Uso:** 128 apariciones en las referencias con V37.1, y 2 con V38 (como movilidad).

**D5 · flexiones** (revisadas una a una)

| Ej. | GIF | Grupo / patrón / slot | Dificultad |
|---|---|---|---|
| 1111 | flexión con patada lateral | pecho / `HORIZONTAL_PUSH` / UPPER_PUSH | se mantiene 1, **sin evaluar con el instrumento** |
| 1113 | de pica a flexión y a cobra | pecho / `HORIZONTAL_PUSH` / UPPER_PUSH | ídem |
| 1115 | flexión con patada interior | pecho / `HORIZONTAL_PUSH` / UPPER_PUSH | ídem |
| 1262 | flexión con rodilla al codo | pecho / `HORIZONTAL_PUSH` / UPPER_PUSH | ídem |

No se ha asumido que tengan la misma dificultad ni que equivalgan a 1106. Las restricciones (suelo) no cambian.

**D6 · 558 y 567: por qué el tren inferior domina**
- **558:** sentadilla completa con las mancuernas colgando y curl al subir. La flexión de rodilla y cadera es el movimiento principal; el curl se hace con las piernas extendidas y es breve. El GIF resalta cuádriceps y glúteo durante la sentadilla.
- **567:** zancada adelante completa con extensión de tríceps por encima de la cabeza. La zancada mueve el cuerpo y el GIF resalta cuádriceps y glúteo.
- **El patrón ya era `KNEE_DOMINANT`**: solo el grupo lo contradecía, y es el mismo caso que V36 corrigió en 527 y 609.
- **Efecto:** pasan de 33–42 apariciones en días de brazo a 1–4, en días de pierna.

**D7 · 1106**
- Pecho, por repeticiones, dificultad 2 **provisional**.
- No es un consenso validado: la dificultad definitiva debe salir del instrumento de FitSense y de su evaluación humana.
- Con nivel 2 ya no llega a los principiantes.

**142** (V37.1, sin cambios en V38):
- 0 colocaciones en UPPER_BODY o PULL;
- sigue cubriendo LOWER_HIP;
- nunca es el único candidato (mínimo 5);
- 7–8 usos en días de pierna.

## 3. Comparación por cambio y acumulada

Fuente: `evidencia/v38/comparativa_v38.md`. Base: V37 + V37.1.

| Variante | Refs válidas | Barrido | Robustez | 192 | Sin plan / regla |
|---|---|---|---|---|---|
| V37.1 (motor sin D1) | 72 | 70 | 280 | 192 | LW principiante 4d y 5d casa (V20) |
| A (cardio) | 72 | **72** | **288** | 192 | — |
| B (clasificación) | 72 | 70 | 280 | 192 | los 2 de V20 |
| C (equipamiento) | 72 | 70 | 280 | 190 | los 2 de V20; LW int./av. casa sin brazos arriba (V15) |
| A+B | 72 | 72 | 288 | 192 | — |
| B+C | 70 | 64 | 256 | 180 | GF principiante 4–6d (V15), LW 4d y 5d (V20) |
| A+C | 72 | 72 | 288 | 188 | LW int./av. casa: sin brazos arriba y «todas» (V15) |
| **A+B+C (V38)** | **70** | **66** | **264** | **176** | **GF principiante 4–6d y LW int./av. 3d casa (V15)** |
| A+B+C sin 1102 | 72 | 72 | 288 | 188 | 4 escenarios LW int./av. casa (V15) |
| A+B+C + prototipo E1/E2 | 72 | 72 | 288 | **192** | — (continuidad 96/96) |

- **Diferencias de referencias:** las 72 cambian frente a V37.1 en V38. Cambian los ejercicios y la composición; las dosis y las reglas son las mismas. Las referencias **no se han actualizado**.
- **Sesiones bajo el 70 % exacto:** 0 en todas las variantes.

## 4. Equipamiento real (etapa C)

**Implementos nuevos** (filas en `equipment_types`; `requires_gym` excluye de casa):

| Código | Nombre | ¿Casa? | Ejercicios que lo exigen (GIF) | Uso con V37.1 (refs/barrido/192) |
|---|---|---|---|---|
| `wall bars` | Espaldera | sí | 301–308, 813 | 46 / 70 / 105 escenarios con alguno |
| `anchor point` | Punto de anclaje | sí | 1072 | 2 / 6 / incluido arriba |
| `ghd bench` | Banco GHD | no | 880 | 4 / 4 |
| `hyperextension bench` | Banco romano | no | 58 | 0 / 1 |
| `squat rack` | Rack | no | 235 | 0 / 1 |
| `preacher bench` | Banco Scott | no | 20 ejercicios de curl de predicador, con evidencia uno por uno en `no_incluidas_v37.tsv` | 0 / 0 |
| `step platform` (ya existía) | Step | sí | **1253**, que pasa a exigirlo | 0 / 15 |

**No se desactiva ningún ejercicio.** El banco Scott se representa como requisito: queda disponible para quien lo declare.

**Contrato del perfil, backend y Flutter** (investigado):
- **Backend:** `user_profiles.equipment_codes` es un `VARCHAR[]` sin FK. `UserProfile.normalizeEquipment` no valida contra `equipment_types`. `GET /api/v1/catalog/equipment-types` publica la lista. **Los códigos nuevos funcionan sin tocar Java.**
- **Flutter** (`fitsense-2-0/mobileapp`): **no hay todavía pantalla de perfil ni de equipamiento**. Ningún usuario de la app puede declarar hoy ni `step platform` ni los nuevos.
  - Recomendación: que esa pantalla lea `GET /catalog/equipment-types`, para no fijar códigos en la app.
  - Mientras no exista, los ejercicios que exigen estos implementos **no llegan a nadie que use la app**.
- **Efecto para el usuario:** quien no declare espaldera deja de recibir los remos 301–308. Es correcto, porque no puede hacerlos.

**Impacto sobre V23 y capacidades** (`CatalogoV38IT`, `solo_peso_corporal.tsv`):

| Perfil | Tirón superior elegible (nivel ≤ 1 / ≤ 2) | Resultado de la generación (3 días) |
|---|---|---|
| Casa (peso corporal, mancuerna y banda) | 11 → **8** / 21 → **14** (todos con banda) | se genera |
| Casa + espaldera | 11 → 11 / 21 → 20 | se genera |
| **Solo peso corporal** | 3 → **0** / 7 → **0** | **10 de 12 publican sin tirón superior.** V23 no lo exige cuando no hay ejercicios: lo declara no factible. **LOSE_WEIGHT intermedio y avanzado: sin plan (V15)** |

- **Sin tirón superior, de forma expresa:** los 12 perfiles solo con peso corporal (4 objetivos × 3 niveles).
- **No se ha inventado ninguna alternativa ni se ha cambiado V23.** Hoy, un perfil solo con peso corporal obtiene un plan de empuje, piernas y core, sin tirón.
- Para darle tirón haría falta declarar un implemento: banda, espaldera o anclaje.

## 5. Por qué aparecen perfiles sin plan y qué los resolvería (decisión E)

**Dos limitaciones de composición del motor** que los atributos incorrectos tapaban:
1. **GENERAL_FITNESS principiante, día de pierna.**
   - El motor abre la sesión con 3 ejercicios principales. Con 3, el selector permite que los tres sean del mismo grupo.
   - Los slots LOWER_KNEE y LOWER_HIP solo tienen ejercicios `upper legs`, y es correcto que así sea.
   - Antes, 1102 (`waist`, fuerza de rodilla, **incorrecto**) y los remos con espaldera ofrecían otra opción.
   - Al añadir gemelo y abdomen, la sesión queda en 3 de 5 en `upper legs`.
2. **LOSE_WEIGHT intermedio y avanzado, 3 días en casa (FULL_BODY).** Los 2 ejercicios de fuerza estructural salen ambos de `upper legs`, porque la corrección de grupos excluye FULL_BODY a propósito.

**Prototipo E1/E2** (`evidencia/v38/prototipo_E1_E2_composicion.diff`, **no aplicado**). Amplía `asegurarGrupos`, que ya existía:
- **E1:** también actúa en FULL_BODY;
- **E2:** también corrige un grupo que pase de la mitad, cambiando una colocación de ese grupo por otra de otro grupo del enfoque, con el mismo patrón de comprobaciones (techo, mitad, piso de V20 y V18).

**Resultado:** 72/72, 288/288, 96/96 y 192/192, con y sin 1102. No cambia reglas, dosis ni capacidades: solo el orden de composición.

## 6. Pruebas

| Prueba | Resultado |
|---|---|
| `CatalogoV38IT`, solo con la candidata: cardio (1), 1102, flexiones y combinados (2-3), equipamiento declarado y no declarado (4-5), solo peso corporal con UPPER_PULL a 0 y sesiones al 70 % (6-8) | 4/4 |
| `EnRevisionD1Test` | 6/6 |
| `ImpactoRestriccionesIT`: nueva columna `equipo_ausente_segun_gif`, independiente del catálogo cargado | V37 126 · V37.1 105 · V38 **0** |
| Simulación: 72 referencias, barrido de 72, 288 de robustez, 96 de continuidad y 192 restricciones (V37, V37.1, V38 y cada etapa) | §1 y §3 |
| Suite completa (`mvn verify -Pintegration`) con el catálogo oficial y con la V38 candidata | §6.1 |

### 6.1 Suite completa

_(se completa al terminar)_

## 7. Riesgos residuales y revisión humana

- La revisión visual sigue siendo asistida por IA. La **doble revisión humana** de las 43 filas de V38 está pendiente.
- **Dificultad:** 1106 está en 2 de forma provisional, y la de las flexiones 1111, 1113, 1115 y 1262 no se ha evaluado con el instrumento.
- Siguen abiertos C1 (convención de los sentados, 74 ejercicios) y C2.
- **298 y 854:** pendientes de una revisión adicional (D2).
- **301 y 302:** el patrón sigue dudoso (C6). Con la espaldera exigida ya no llegan a casa sin declararla.
- **App Flutter:** sin pantalla de equipamiento, los implementos nuevos y `step platform` no pueden declararse desde la app.

## 8. Decisiones que necesitan tu autorización

| # | Decisión | Recomendación |
|---|---|---|
| **E** | Ajuste de composición E1/E2 en `CapabilityWeekBuilder.asegurarGrupos` (no cambia reglas ni dosis) | **Sí.** Sin él, V38 deja 24 perfiles sin plan por limitaciones del motor que los datos incorrectos ocultaban |
| F | Integrar V37.1 + V38 en el directorio oficial, renumeradas como V38 y V39 o en una sola V38 | Tras E y tu revisión de las diferencias |
| G | Nuevas referencias versionadas para V38: las 72 cambian | Generarlas cuando apruebes el conjunto; las oficiales e históricas no se tocan |
| H | Pantalla de equipamiento en Flutter, que lea `/catalog/equipment-types` | Necesaria para que los implementos nuevos sean declarables |
| I | Perfiles solo con peso corporal: aceptar que no tengan tirón superior, o pedir un implemento mínimo como la banda en el onboarding | Aceptarlo y avisarlo en la app |

**Replicate:** sin cambios. No se ha hecho ninguna llamada. `min_session_seconds` sigue pendiente. Las respuestas de los usuarios 32 y 33 se incorporarán cuando estén en el repositorio.
