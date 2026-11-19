# 06 · Matriz de problemas, correcciones aplicadas y propuestas que requieren aprobación

**Criterio de la orden:**
- Solo se aplican correcciones **generales**: nada para un ID concreto.
- No se desactivan validadores, no se relaja V24 en la generación, no se añaden tolerancias arbitrarias ni se inventan ejercicios o dosis.
- Todo cambio de una política deportiva aprobada, de un umbral de seguridad o del contrato de un validador se **documenta y se presenta para aprobación antes de aplicarlo**.

**Situación:**
- Esta auditoría aplica **una** corrección de defecto (AUD-G1).
- Añade **un** prototipo de laboratorio apagado por omisión (P-G2), que solo sirve para medir.
- Todo lo demás queda como propuesta.

## 1. Matriz de problemas

Leyenda de la columna «¿Se aplica?»:
- **✔ aplicada:** defecto sin cambio de política.
- **⏸ propuesta:** requiere aprobación.
- **📄 documental:** se corrige el texto, no el comportamiento.
- **👤 profesional:** decisión de un profesional.

| # | Problema | Evidencia | Clase | Impacto medido | Corrección | ¿Se aplica? |
|---|---|---|---|---|---|---|
| G1 | LOSE_WEIGHT de 1 día (35–50 min): sesión FULL_BODY con un solo grupo; V15 la rechaza aunque cabe un ejercicio más | 03 §3; `familias_base/LOSE_WEIGHT_INTERMEDIATE_GIMNASIO_1_45` | Fallo del algoritmo | 15/3.780 perfiles sin plan | **AUD-G1**: `asegurarGrupos` añade un ejercicio de otro grupo si ningún cambio es posible | **✔ aplicada** |
| G2 | J03 y la familia LW principiante en casa (65–75 min): la mitad de V15 se aplica sobre grupos que LW no puede prescribir | 03 §2 | Regla contradictoria | 30 perfiles sin plan (J03 incluido) | **P-G2**: la mitad de V15 solo con 2 o más grupos prescribibles | ⏸ (prototipo apagado; impacto en el documento 07) |
| G3 | GM y IS intermedio o avanzado sin material, 5–7 días: PULL sin material de tracción | 03 §3–4 | Limitación del catálogo + heurística de división | 102 perfiles sin plan; PULL válido a 30–40 min, pero sin tracción real | **P-G3**: sin tracción real elegible, no asignar PULL (fusionar en UPPER_BODY). **P-C1**: incorporar remo invertido con mesa o toalla al catálogo, con requisito de seguridad | ⏸ + 👤 |
| G4 | GF principiante, 1 día, 70–75 min: el tope de 8 + V20 + V25 hace imposible el plan | 03 §3 | Heurística de ingeniería (tope 8) + V20 | 4 perfiles | Ver P-G1 (semántica de `sessionMinutes`) | ⏸ |
| G5 | GF principiante en casa, 75 min: depende de la semilla | 03 §3 (17/30 semillas válidas) | Fallo del algoritmo (no robusto) | 3 perfiles | **D-G3**: si el borrador del motor es inválido, reintentar con una semilla derivada y determinista (k = 1..N) antes de dejar la semana sin plan | ⏸ (*) |
| G6 | `sessionMinutes` sin semántica única; V20 (70 %) es un piso duro sin respaldo científico | 01 §2.a; 02 A | Heurística de producto convertida en restricción dura | Tras AUD-G1, los 139 inválidos restantes fallan todos por V20 (y 3 de ellos dependen de la semilla) | **P-G1**: `sessionMinutes` = máximo disponible; V20 pasa a ser objetivo blando o mínimo de dosis semanal por objetivo | ⏸ + 👤 |
| A1 | GM ante fatiga: no se puede reducir la dosis (4D + V24 + V20) | 04 §1 | Conflicto generación-adaptación | U03 S3 y J01 S4 no factibles | **P-A1**: «dosis de mantenimiento» autorizada por V8C (3→2 series o retirar relleno) durante 1 semana; exención de V20 para REVISAR_CARGA | ⏸ + 👤 |
| A2 | GM ante desmotivación: INVESTIGAR_CAMBIO_MINIMO solo permite cambiar la dosis, que en GM es fija | 04 §1 | Restricción excesiva | U04 S3, J09 S4 y J13 S3 no factibles | **P-A2**: permitir **una** sustitución lateral (mismo patrón, dificultad ≤) en INVESTIGAR_CAMBIO_MINIMO | ⏸ |
| A3 | Fatiga: la reducción mediana (8 %) la limita V20 en la adaptación | 04 §3 | Restricción potencialmente excesiva | 10/10 sesiones con fatiga | Está dentro de P-A1 (exención de V20 en REVISAR_CARGA para todos los objetivos) | ⏸ + 👤 |
| A4 | La dificultad del catálogo no codifica el impacto (J07 R′: cross trainer → saltos) | 04 §3 | Limitación del catálogo | 1 caso observado | **P-C2**: atributo de impacto y regla «más sencillo» = dificultad ≤ y sin más impacto | ⏸ + 👤 |
| A5 | La desmotivación se resuelve con −1 repetición | 04 §3 | Pertinencia baja | 18/18 | Lo cubre P-A2 | ⏸ |
| C3 | Traducción engañosa «Pase de banda» (125, *band pull through*) | 04 §3 | Texto del catálogo | — | Renombrar a «Tirón de banda entre las piernas (bisagra de cadera)» | ⏸ (migración de catálogo) |
| P1 | Diseño e implementación V34 dicen 80 % para la progresión; el código exige 100 % (decisión documentada en la POLÍTICA §1) | 05 §1 | Discrepancia documental | — | Anotar en `DISENO_…` e `IMPLEMENTACION_PROGRESION_V34.md` que la progresión ordinaria exige 100 % | 📄 (se propone; documentos históricos sin tocar en esta rama) |
| P2 | Al apagar la progresión, las copias de GM ya progresadas pasan a NO_FACTIBLE | 05 §8 A (`ProgresionV24V8CTest.apagadoNoHereda`) | Defecto de reversión | Solo con la progresión encendida y después apagada | **P-P1**: heredar el linaje de dosis aunque el interruptor esté apagado (sin nuevos pasos) | ⏸ (cambia V24 en la adaptación) |
| P3 | La reparación de planes publicados no hereda dosis | 05 §8 B | Defecto probable | Solo con la progresión encendida | **P-P2**: emitir autorizaciones heredadas en PPSR | ⏸ (no verificado en ejecución) |
| P4 | Una versión reparada rompe el linaje | 05 §8 C | Defecto deducido | — | Prueba primero (no se ejecutó) | Pendiente |
| P5 | La vuelta atrás bajo el piso de V20 no conserva D-P7 | 05 §15 H7 | Discrepancia | — | Lo cubre P-A1 (exención de V20 en la reducción por fatiga) | ⏸ |
| H1 | Arnés K20: una predicción sin métricas bloquea las siguientes (U13, J08) | `INCIDENCIA_R_U.md`, `INCIDENCIA_Rp_J.md` | Defecto de la infraestructura de evaluación | 2 corridas repetidas | **P-H1**: ver §3 | ⏸ (afecta al protocolo de evaluación) |
| X1 | Volumen GM de hasta 36 series/semana/grupo; sin techo | 02 C | Pendiente de validación profesional | — | **P-V1**: techo semanal por grupo (unas 20 / 24–26) | 👤 |
| X2 | Perfil bloqueado tras el primer plan | 01 §0.8 | Restricción excesiva (producto) | — | Fuera del alcance del núcleo; se informa | ⏸ |
| X3 | Comentarios de `application.properties` contradictorios con los valores por omisión | 01 §0.10 | Documental | — | Corregir los comentarios | 📄 (no aplicado: el fichero es de producción) |

(*) D-G3 no cambia ninguna regla, pero sí el comportamiento observable del generador (otra semilla). Por coherencia con la regla de «no habilitar funcionalidades sin prueba explícita», se deja como propuesta, con impacto acotado: 2 de los 154 perfiles se rescatan con otras semillas.

## 2. Corrección aplicada: AUD-G1

**Qué cambia:** `CapabilityWeekBuilder.asegurarGrupos`.
- Antes, si la sesión tenía un solo grupo de trabajo, intentaba **cambiar** una colocación por otra de otro grupo. Si ninguna se podía quitar (cada una cubre un slot 4C pendiente), la sesión quedaba con V15 incumplida y la semana sin plan.
- Ahora, en ese caso, **añade** un ejercicio de otro grupo del enfoque con la misma capacidad que el trabajo ya colocado, solo si:
  - cabe bajo el tope de colocaciones;
  - no supera el techo (V4);
  - respeta la mitad de V15.

**Por qué no es un cambio de política:**
- No toca ningún validador, dosis, umbral, catálogo ni permiso de intervención.
- Hace lo que V15 ya exigía («al menos 2 grupos») con el margen que la sesión ya tenía.
- Una sesión que ya cumplía V15 no pasa por ahí.

**Verificación:**
- **Barrido de 3.780 perfiles:** 15 pasan de INVALIDO a VALIDO (todos LW de 1 día) y **0** cambian en el sentido contrario. Las duraciones mínima y máxima de los 3.626 válidos de base son idénticas.
- **`GeneracionReferenciaIT`:** 72/72 planes iniciales idénticos a la referencia.
- **`V15CambioDeGrupoIT`:** pasa.
- **Regresión completa:** documento 07.

## 3. Propuestas que requieren aprobación (con justificación)

| Id | Propuesta | Justificación | Riesgo | Cómo se verificaría |
|---|---|---|---|---|
| **P-G1** | `sessionMinutes` = **máximo disponible**. V20 deja de invalidar planes y pasa a objetivo blando: se llena hasta donde permitan el catálogo y los topes. Se sustituye por un mínimo de dosis semanal por objetivo, a fijar por un profesional. V4 pasa a ≤ 100 % de M (o un margen aceptado por el usuario). | Ninguna fuente fija una duración mínima de sesión. OMS/ACSM dosifican por semana (02 A). Es la regla que falla en los 139 perfiles que siguen sin plan tras AUD-G1. | Sesiones más cortas que lo declarado. Hace falta un mínimo semanal para no infradosificar. | Barrido de 3.780 + matriz de 960 + D/D′ sobre U, J y K, con política versionada (PLAN-HIST-2). |
| **P-G2** | La mitad de V15 solo con **2 o más grupos prescribibles** en el enfoque. | Coherencia con el mínimo de V15, que ya cuenta los prescribibles (`prescribibleParaV15`). Evita exigir repartir entre grupos que el sistema no deja prescribir. | **Medido** (documento 07 §1.1): +30 perfiles con plan (J03 incluido), pero **168 planes LW válidos cambian**. La sesión LOWER de J03 queda con 6 ejercicios de fuerza de pierna para llegar a V20: calidad discutible. **Recomendación: no aprobar P-G2 solo; preferir P-G1 (o P-G1 + P-G2).** | Prototipo `fitsense.lab.v15-mitad-prescribibles` (apagado). Validador + constructor + selector. |
| **P-G3** | Sin tracción real elegible (patrón HORIZONTAL_PULL o VERTICAL_PULL con dosis), la división no asigna PULL. Usa UPPER_BODY o PUSH/LEGS + UPPER. | El PULL que se genera hoy sin material no contiene tracción (03 §4). | Cambia la división de 5–7 días sin material. | Barrido y revisión de calidad de las divisiones. |
| **P-A1** | GM con fatiga (REVISAR_CARGA_Y_RECUPERACION): V8C autoriza una **dosis de mantenimiento** (3→2 series, o retirar un relleno) durante 1 semana; V24 la acepta como autorizada; V20 no rige en esa sesión. Con la misma lógica, se exime V20 en REVISAR_CARGA para todos los objetivos. | 02 B: la reducción temporal del volumen conserva la hipertrofia (Bickel 2011, Spiering 2021, Coleman 2024; *deload*, Bell 2023). Es la regla con menos respaldo de todas. | Si la fatiga se repite, mantener una dosis baja. Mitigación: tras 2 semanas con fatiga, mensaje de revisión. | Sonda de factibilidad sobre U03 S3 y J01 S4 + IA simulada + escenarios de 2 semanas. |
| **P-A2** | INVESTIGAR_CAMBIO_MINIMO permite **una** sustitución lateral: mismo patrón, dificultad ≤ y, con P-C2, sin más impacto. Ninguna otra cosa cambia. | En GM la intervención es imposible por construcción (04 §1). Fuera de GM, «−1 repetición» no cambia la experiencia (04 §3). La sonda muestra que hay sustitutos laterales válidos (J13 S2: 82/82). | Más variabilidad. B-4 (≤ 2 cambios) se mantiene. | Sonda sobre U04 S3, J09 S4 y J13 S3 + IA simulada. |
| **P-C1 / P-C2 / C3** | Catálogo: tracción sin material; atributo de impacto; renombrar el 125. | 03 §4; 04 §3. | Datos del catálogo; requieren revisión visual y profesional. | Migración versionada + auditoría del catálogo. |
| **P-P1 / P-P2** | Herencia del linaje de dosis con el interruptor apagado y en la reparación de planes publicados. | 05 §8 A/B. | Solo afecta con la progresión usada; hoy está apagada. | Pruebas nuevas H4/H5 (05 §15). |
| **P-H1** | Arnés K20: una predicción **fallida y sin métricas** se contabiliza al coste de la reserva (0,05 USD), igual que hoy, pero **no bloquea** los reintentos propios del sistema mientras el gasto acumulado + la reserva quepan en el tope. Se bloquea solo cuando, contando la reserva de cada predicción sin métricas, el tope ya no admite otra. | En las incidencias U13 y J08, una predicción atascada y después `failed` (0 tokens según Replicate) bloqueó las llamadas siguientes y obligó a repetir la corrida completa. El control de presupuesto se mantiene: el peor caso sigue acotado por la reserva. | Si el proveedor factura una predicción fallida sin métricas por encima de la reserva, el tope se puede superar en esa diferencia. Mitigación: comprobar `GET /v1/predictions/{id}` al final de la corrida y registrar el coste real. | Prueba unitaria `PresupuestoReplicateK20Test` con un caso «sin métricas, reintento». Gratis. |
| **P-V1** | Techo semanal de series por grupo en GM. | 02 C. | Requiere fijar cifras. | 👤 |

## 4. Qué NO se ha hecho y por qué

- No se ha tocado V20, V24, V15 (salvo el prototipo apagado), 4D, `TipoIntervencion`, el catálogo, el prompt, el oráculo ni los criterios.
- No se ha corregido J03. Toda corrección de J03 cambia el contrato de V15 o el de V20, y queda pendiente de aprobar P-G2 o P-G1.
- No se ha activado la progresión ni la guarda de dificultad fuera de las pruebas que ya lo hacen explícitamente.
- No se ha usado Replicate.
