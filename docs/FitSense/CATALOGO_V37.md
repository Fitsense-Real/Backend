# Catálogo V37 · Correcciones de seguridad (bloque A) y desactivación de 776

**Fecha:** 29/10/2026 · **Rama:** `catalogo-v37` · **Migración:** `src/main/resources/db/migration/V37__catalog_safety_corrections.sql`

**Estado: CANDIDATA A DESPLIEGUE; NO APLICADA EN NEON.**
- Aprobada por el responsable del proyecto («Aprobación condicionada de V37», 29/10/2026), solo para implementarla y probarla en entornos aislados.
- Aplicarla en Neon y desplegar requieren una autorización aparte.

> **Cuidado con Flyway.** `application.properties` apunta a Neon con `spring.flyway.enabled=true`. Una vez fusionado este cambio, **cualquier arranque del backend contra Neon aplicaría V37**. Para arrancar contra Neon sin aplicarla: `SPRING_FLYWAY_TARGET=36` o `SPRING_FLYWAY_ENABLED=false`. Railway no despliega desde `generation`, según confirmó el responsable.

## 1. Qué cambia

`correcciones_v37.tsv` (bloques A1–A5) es la única fuente: valores antes y después, con la evidencia del GIF. La migración se genera con `docs/FitSense/auditoria_catalogo/v37/generar_v37.py`.

| Bloque | Contenido | Cambios | Ejercicios |
|---|---|---:|---:|
| A1 | Banderas de seguridad ausentes: suelo 52, rodillas 29, brazos arriba 32, suspensión 4, inversión 3 | 120 | 98 |
| A2 | Habilidades avanzadas en nivel 1, pasan a nivel 3 | 10 | 10 |
| A3 | Implemento y requisito de los ejercicios de suspensión o gimnasia | 16 | 8 |
| A4 | 1374: patrón `LOCOMOTION_CARDIO` → `TRUNK_FLEXION`; alto impacto `true` → `false` | 2 | 1 |
| A5 | **776 desactivado** (`is_active` `true` → `false`) | 1 | 1 |
| **Total** | | **149** | **102** |

**La migración comprueba, antes de cambiar nada:**
- el `source_code` y el `name_en` de cada ejercicio;
- que el ejercicio esté activo;
- el valor anterior de cada columna.

Al terminar comprueba el estado final y que la suspensión implique brazos arriba en todo el catálogo. Si algo no coincide, aborta sin cambios.

**No toca:** `exercise_difficulty_approvals` (V27/V28), planes publicados, V15, V20, V23, V24, V25, las políticas de capacidades, la adaptación, la progresión, los prompts ni Java.

## 2. 776: desactivado

- `is_active = false`.
- Además, V37 registra sus características reales: suspensión, brazos arriba e inversión en `true`, nivel 3 y anillas como implemento y requisito.
- Con eso, el registro describe el movimiento del GIF y no la bisagra de pie que evaluó V28.
- V28 queda intacta: el evento `APPROVED` de nivel 1, los revisores y el consenso.
- No se crea ningún evento nuevo de revisión ni de revocación.
- Anexo metodológico: `docs/FitSense/auditoria_catalogo/v37/ANEXO_776_V28.md`. Comprobación: `CatalogoV37IT`.

## 3. Discrepancias con V25

En `docs/FitSense/auditoria_catalogo/v37/DISCREPANCIAS_V25.md`: 45 ejercicios.
- 26 tienen una bandera de seguridad ausente y entran en V37.
- El resto son restricciones de más o casos probables, que quedan para V37.1 o pendientes.
- V25 no se reescribe: cada cambio se justifica con la definición vigente de V10 o V25 y lo que muestra el GIF.

## 4. Resultado de la comparación V36 frente a V37

Detalle completo en `docs/FitSense/auditoria_catalogo/v37/resultados_simulacion.md`. Evidencia en `v37/simulacion/{v36,v37,v37b}/`.

| Prueba | V36 | V37 |
|---|---|---|
| 72 generaciones de referencia | 72 válidas | **72 válidas**: 68 perfiles y 180 sesiones cambian |
| Barrido de 4 a 6 días (72 perfiles) | 72 válidos | **72 válidos**, 72 con cobertura de capacidades COMPLETO |
| Robustez (288 generaciones) | 288 válidas | **288 válidas** |
| Continuidad semanal (8 LOSE_WEIGHT de 5–6 días y 96 perfiles) | correcta | **correcta** |
| 192 perfiles con restricciones: sin plan | 0 | **0** |
| 4C robusta (192) / pool de capacidades ROBUSTO (192) | 192 / 170 | **192 / 170** |
| `LOWER_HIP` en los 96 perfiles sin suelo o sin suspensión: mínimo y media de elegibles | 6 y 20,3 | **5 y 18,4** (ninguno con 0 a 2) |

**Cumplimiento por dimensión.** Planes de los 192 perfiles con al menos un ejercicio que el perfil no puede hacer según la revisión visual:

| Dimensión | V36 | V37 | ¿Corregido por V37? |
|---|---:|---:|---|
| Banderas de seguridad (suelo, rodillas, suspensión, brazos arriba) | 82 | **0** | Sí |
| Dificultad (habilidad de nivel 3 en principiante o intermedio) | 74 | **0** | Sí |
| **Equipamiento real observado en el GIF** | 147 | **93** | **No.** Lo corrige V37.1 (B2), pendiente |
| Cobertura muscular y de capacidades | 192 de 192 con 4C robusta | 192 de 192 | Sin cambio |

**V37 no deja el catálogo completamente corregido.** Corrige la seguridad y la dificultad. En V37, 93 de 192 perfiles siguen recibiendo algún ejercicio cuyo implemento no está registrado como obligatorio: banco, step, paralelas, multipower. Mientras no se aplique V37.1, no puede afirmarse que el equipamiento sea correcto.

**Ejercicios que desaparecen de las referencias y sus sustituciones** (sesiones):
- **776** sale en 43 sesiones; en 39 lo sustituye **142** (peso muerto con banda, `HIP_HINGE`).
- 1098 sale en 9.
- Los demás son reajustes del motor dentro del mismo patrón: 896→898, 1096→1097, 1100→1180, 1103→1102…

La lista completa está en `resultados_simulacion.md`.

**Duración:** el porcentaje estimado de las sesiones del barrido no cambia (media del 74,1 %, frente al 74,2 % de V36). Todas superan V20 con su propia medida.

**Referencias:**
- La oficial pasa a ser la de V37: `docs/FitSense/evidencia/progresion/generacion_referencia.txt`, protegida por `GeneracionReferenciaIT`.
- La de V36 se conserva como evidencia reproducible: `generacion_referencia_v36.txt`, en `GeneracionReferenciaV36IT`, con Flyway hasta la 36.

## 5. Usuario 31 (mismo perfil, sin relajar restricciones)

| Caso | V36 (`Usuario31ReproduccionIT`) | V37 (`Usuario31V37IT`) |
|---|---|---|
| ¿776 y 1320 son elegibles? | sí | **no** |
| Intento 1 de Replicate | V18 y V23 (`LOWER_HIP`, con 13 elegibles) | **igual** (11 elegibles) |
| Intentos 2 y 3 | V15 (4 de 6 y 3 de 5 en upper legs) | **igual** |
| Plan 63 | VÁLIDO, con 776 y 1320 | **rechazado (V1)** |
| Motor de reglas | publica con 776 y 1320 | **publica sin ningún ejercicio que el perfil excluye** |

Los rechazos de Replicate se deben a la selección de la IA, no al catálogo. No se cambian prompts ni reglas.

## 6. Pruebas

| Prueba | Qué protege |
|---|---|
| `CatalogoV37IT` | V37 aplica exactamente su matriz y nada más; V28 intacta; 776 inactivo con sus datos reales; reaplicarla aborta; V37.1 aborta ante una edición previa |
| `Usuario31V37IT` / `Usuario31ReproduccionIT` | Usuario 31 con V37 y con V36 |
| `GeneracionReferenciaIT` / `GeneracionReferenciaV36IT` / `GeneracionReferenciaV35IT` | Referencias de V37 (oficial), V36 y V35 |
| `ImpactoRestriccionesIT`, `GeneracionReferenciaCandidataIT` (opt-in) | Simulación de 192 perfiles con restricciones y referencias candidatas |

## 7. Pendiente para V37.1 (no se descarta)

En `docs/FitSense/auditoria_catalogo/v37/separables/V37_1__…sql`, generada y probada, pero **no aprobada**: 154 ejercicios.

1. **B2, implemento observado y no exigido** (151 cambios). Corrige los 93 perfiles anteriores. Hoy deja sin plan a LOSE_WEIGHT principiante de casa con 4 o 5 días: les falta 1 minuto para el mínimo de V20. **Depende de corregir antes el defecto de duración de V20.** No se usarán ejercicios sin su implemento para cumplir el tiempo mínimo.
2. **B1, grupo y patrón confirmados** (25 cambios: 1103, 142, 382, 684, 685, 730 y 13 patrones). Afecta a V15 y 4C. **142 tiene prioridad:** sustituye a 776 y, por estar clasificado como `back`, se coloca en días de tren superior (§7 bis).
3. **B3, restricciones de más** (32 cambios).
4. **Decisiones funcionales** (`DECISIONES_PENDIENTES.md`):
   - convención de los sentados (74);
   - dificultad v5 (30);
   - ejercicios combinados (14);
   - remo al mentón (6);
   - grupo o patrón ambiguo (38);
   - balón o bosu (5);
   - lanzamientos (4);
   - banderas probables (18).
5. **41 confirmados sin traducción en el esquema** (`no_incluidas_v37.tsv`): banco Scott, espaldera, GHD, rack, textos por redactar.
6. **Revisión humana independiente:** **PENDIENTE**. La revisión visual es asistida por IA y no se atribuye a ninguna persona.

Secuencia acordada después de V37:
1. defecto de duración de V20;
2. V37.1 con el equipamiento corregido;
3. criterios de grupo y patrón;
4. regresión;
5. solo entonces, Replicate.

## 7 bis. Verificación del ejercicio 142 antes de fusionar (29/10/2026)

142 «band straight leg deadlift» sustituye a 776 en 39 sesiones de las referencias. La auditoría confirmó su grupo como incorrecto: `back/spine`, cuando debería ser `upper legs/hamstrings` (bloque B1, pendiente para V37.1).

**Qué muestra el GIF:** peso muerto con piernas rectas, de pie, con la banda bajo los pies.

**Seguridad y equipamiento: correctos.**
- No hay suelo, rodillas, suspensión, brazos arriba, inversión ni impacto.
- `axial_load = true` es una restricción de más, no un riesgo.
- Exige banda, que es exactamente su requisito.

**`LOWER_HIP`: cobertura real.**
- Su patrón `HIP_HINGE` es correcto: es una bisagra de cadera.
- En 32 de los 68 perfiles de referencia que lo usan, es la única cobertura de `LOWER_HIP`, y es legítima.
- V23 cuenta por patrón, no por grupo, así que la cobertura no está falseada.

**V15: sin incumplimientos.**
- Las 73 sesiones de referencia y las 66 del barrido de 4 a 6 días que lo contienen cumplen la regla de la mitad.
- La cumplen tanto con su grupo actual (`back`) como si se reclasificara (`upper legs`).

**Defecto de colocación (pendiente para V37.1).**
- Por estar clasificado como `back`, el motor lo coloca en días UPPER_BODY (42) y PULL (24) del barrido, cuando es un ejercicio de pierna.
- **No es una regresión de V37.** En V36 ya pasaba con 776 (UPPER_BODY 24, PULL 18) y con el propio 142 (23 y 9).
- Las sesiones UPPER/PULL con un ejercicio de patrón de pierna bajan de 87 (V36) a 85 (V37).
- Se corrige con la reclasificación de 142 del bloque B1 de V37.1, junto con la decisión sobre los remos en sentadilla (301, 302), que tienen el mismo efecto.

**Conclusión:** la discrepancia es solo de clasificación muscular. No hay incumplimiento de seguridad ni cobertura estructural falseada. V37 no se amplía ni se modifica V15.

## 8. Anexo · lista final de ejercicios y atributos modificados

| Ej. | Nombre de la fuente | Cambios (antes → después) | Bloque | Evidencia del GIF |
|---:|---|---|---|---|
| 68 | arms apart circular toe touch (male) | requires_overhead false → true | A1 | De pie, brazos arriba; no es de suelo |
| 74 | assisted parallel close grip pull-up | requires_kneeling false → true | A1 | Rodillas en la almohadilla de asistencia |
| 75 | assisted pull-up | requires_kneeling false → true | A1 | Rodillas en la almohadilla de asistencia |
| 103 | band one arm twisting chest press | requires_floor false → true | A1 | Sentado en el suelo con piernas extendidas |
| 119 | band one arm twisting seated row | requires_floor false → true | A1 | Sentado en el suelo con piernas extendidas |
| 141 | band vertical pallof press | requires_overhead false → true | A1 | Lleva la banda por encima de la cabeza |
| 173 | barbell palms down wrist curl over a bench | requires_floor false → true; requires_kneeling false → true | A1 | De rodillas en el suelo detras del banco, antebrazos sobre el banco |
| 174 | barbell palms up wrist curl over a bench | requires_floor false → true; requires_kneeling false → true | A1 | De rodillas en el suelo detras del banco, antebrazos sobre el banco |
| 187 | barbell glute bridge two legs on bench (male) | requires_floor false → true | A1 | Tumbado en el suelo con las piernas sobre el banco |
| 224 | barbell press sit-up | requires_overhead false → true | A1 | Press de la barra por encima de la cabeza |
| 229 | barbell sitted alternate leg raise | requires_floor false → true; requires_overhead false → true | A1 | Sentado en el suelo con la barra arriba |
| 254 | basic toe touch (male) | requires_overhead false → true | A1 | De pie, brazos arriba |
| 269 | barbell upright row v. 2 | requires_overhead false → true | A1 | La barra sube por encima de la cabeza |
| 270 | barbell upright row v. 3 | requires_overhead false → true | A1 | La barra sube por encima de la cabeza |
| 274 | barbell sitted alternate leg raise (female) | requires_floor false → true; requires_overhead false → true | A1 | Sentado en el suelo con la barra arriba |
| 309 | bench hip extension | requires_kneeling false → true | A1 | Una rodilla y las manos en el banco, patada de gluteo |
| 316 | biceps narrow pull-ups | requires_floor false → true | A1 | Tumbado bajo la barra, talones en el suelo (igual que 912) |
| 318 | body-up | requires_floor false → true | A1 | Plancha en el suelo pasando de antebrazos a manos |
| 327 | cable assisted inverse leg curl | requires_kneeling false → true | A1 | Rodillas en la almohadilla (curl nordico asistido) |
| 328 | cable alternate triceps extension | requires_overhead false → true | A1 | Extension de triceps por encima de la cabeza |
| 331 | cable concentration extension (on knee) | requires_floor false → true; requires_kneeling false → true | A1 | Una rodilla en el suelo |
| 351 | cable high row (kneeling) | requires_overhead false → true | A1 | Tira desde polea alta con brazos por encima de la cabeza |
| 353 | cable incline pushdown | requires_overhead false → true | A1 | Tumbado en banco inclinado, tira desde polea por encima de la cabeza |
| 371 | cable lying triceps extension v. 2 | requires_floor false → true | A1 | Tumbado en el suelo |
| 387 | cable one arm straight back high row (kneeling) | requires_overhead false → true | A1 | Polea alta, brazo por encima de la cabeza al inicio |
| 393 | cable reverse-grip straight back seated high row | requires_overhead false → true | A1 | Jalon desde polea alta: manos por encima de la cabeza al inicio |
| 417 | cable seated shoulder internal rotation | requires_floor false → true | A1 | Sentado en el suelo junto a la polea |
| 427 | cable side bend crunch (bosu ball) | requires_floor false → true | A1 | Tumbado sobre el bosu en el suelo |
| 431 | cable standing lift | requires_overhead false → true | A1 | Lleva las manos por encima de la cabeza en diagonal |
| 434 | cable seated high row (v-bar) | requires_overhead false → true | A1 | Polea alta: manos por encima de la cabeza al inicio |
| 456 | chest and front of shoulder stretch | requires_overhead false → true | A1 | Palo o banda por encima de la cabeza |
| 461 | clap push up | difficulty_level 1 → 3 | A2 | Flexion con palmada; ya la excluye el alto impacto para principiantes |
| 466 | chest stretch with exercise ball | requires_floor false → true; requires_kneeling false → true | A1 | De rodillas en el suelo con el brazo sobre el balon |
| 477 | cable twist (up-down) | requires_overhead false → true | A1 | Empieza con las manos por encima de la cabeza |
| 481 | cable straight arm pulldown (with rope) | requires_overhead false → true | A1 | Brazos por encima de la cabeza al inicio (polea alta) |
| 514 | dumbbell bench seated press | requires_overhead false → true | A1 | Press sentado por encima de la cabeza |
| 573 | dumbbell incline one arm lateral raise | requires_overhead false → true | A1 | El brazo llega por encima de la cabeza |
| 605 | dumbbell incline two arm extension | requires_overhead false → true | A1 | Extension de triceps con mancuernas por encima de la cabeza |
| 606 | dumbbell kickback | requires_kneeling false → true | A1 | Rodilla y mano en el banco |
| 607 | dumbbell kickbacks on exercise ball | requires_floor false → true; requires_kneeling false → true | A1 | De rodillas en el suelo, pecho sobre el balon |
| 656 | dumbbell over bench one arm reverse wrist curl | requires_floor false → true; requires_kneeling false → true | A1 | De rodillas en el suelo detras del banco, antebrazo sobre el banco |
| 657 | dumbbell over bench one arm wrist curl | requires_floor false → true; requires_kneeling false → true | A1 | De rodillas en el suelo detras del banco, antebrazo sobre el banco |
| 658 | dumbbell over bench revers wrist curl | requires_floor false → true; requires_kneeling false → true | A1 | De rodillas en el suelo detras del banco, antebrazos sobre el banco |
| 659 | dumbbell over bench wrist curl | requires_floor false → true; requires_kneeling false → true | A1 | De rodillas en el suelo detras del banco, antebrazos sobre el banco |
| 684 | dumbbell over bench neutral wrist curl | requires_floor false → true; requires_kneeling false → true | A1 | De rodillas en el suelo detras del banco, antebrazos sobre el banco |
| 689 | dumbbell preacher curl over exercise ball | requires_floor false → true; requires_kneeling false → true | A1 | De rodillas en el suelo con los brazos sobre el balon |
| 776 | elevator | requires_hanging false → true; requires_inversion false → true; requires_overhead false → true; difficulty_level 1 → 3; equipment body weight → gymnastic rings; requisitos ∅ → 1:gymnastic rings; is_active true → false | A1,A2,A3,A5 | Suspension invertida en anillas, cabeza abajo |
| 782 | dumbbell upright shoulder external rotation | requires_overhead false → true | A1 | La mancuerna sube por encima de la cabeza |
| 804 | exercise ball alternating arm ups | requires_floor false → true | A1 | Prono sobre balon con manos en el suelo |
| 809 | exercise ball hug | requires_floor false → true | A1 | Sentado en el suelo abrazando el balon |
| 810 | exercise ball lat stretch | requires_floor false → true; requires_kneeling false → true | A1 | De rodillas en el suelo con los brazos sobre el balon |
| 812 | exercise ball dip | requires_floor false → true | A1 | Sentado en el suelo con las manos sobre el balon |
| 813 | flag | difficulty_level 1 → 3 | A2 | Bandera humana en un poste |
| 822 | exercise ball lower back stretch (pyramid) | requires_floor false → true; requires_kneeling false → true | A1 | Rodillas y manos en el suelo sobre el balon |
| 825 | exercise ball one leg prone lower body rotation | requires_floor false → true | A1 | Manos en el suelo, piernas sobre el balon |
| 860 | full maltese | requires_floor false → true; difficulty_level 1 → 3 | A1,A2 | Plancha maltesa con manos en el suelo |
| 879 | glute bridge two legs on bench (male) | requires_floor false → true | A1 | Tumbado en el suelo con los pies sobre el banco |
| 880 | glute-ham raise | requires_kneeling false → true | A1 | Rodillas en la almohadilla del GHD |
| 881 | hamstring stretch | requires_floor false → true | A1 | Tumbado boca arriba en el suelo |
| 882 | hands clasped circular toe touch (male) | requires_overhead false → true | A1 | De pie, brazos arriba |
| 884 | hug keens to chest | requires_floor false → true | A1 | Tumbado boca arriba en el suelo |
| 888 | floor fly (with barbell) | requires_kneeling false → true | A1 | Rueda la barra desde de rodillas en el suelo |
| 910 | inverted row | requires_floor false → true | A1 | Tumbado bajo la barra con talones en el suelo (igual que 912, que si la lleva) |
| 911 | inverted row bent knees | requires_floor false → true | A1 | Tumbado bajo la barra, rodillas flexionadas (igual que 912) |
| 913 | inverted row v. 2 | requires_hanging false → true; requires_inversion false → true; requires_overhead false → true; difficulty_level 1 → 3; equipment smith machine → pull-up bar; requisitos 1:smith machine,1:suspension straps → 1:pull-up bar | A1,A2,A3 | Colgado cabeza abajo de la barra con las piernas enganchadas |
| 914 | inverted row with straps | requires_floor false → true | A1 | Remo con correas desde tumbado en el suelo (igual que 912) |
| 915 | inverse leg curl (bench support) | requires_kneeling false → true | A1 | Rodillas en el banco (curl nordico) |
| 916 | inverse leg curl (on pull-up cable machine) | requires_kneeling false → true | A1 | Rodillas sobre la almohadilla (curl nordico) |
| 937 | kipping muscle up | difficulty_level 1 → 3; equipment pull-up bar → gymnastic rings; requisitos 1:pull-up bar → 1:gymnastic rings | A2,A3 | Muscle-up con impulso; el alto impacto ya lo excluye para principiantes |
| 949 | kettlebell seated press | requires_floor false → true | A1 | Sentado en el suelo con piernas abiertas |
| 974 | l-sit on floor | difficulty_level 1 → 3 | A2 | L-sit; un principiante con suelo permitido puede recibirlo |
| 979 | landmine lateral raise | requires_overhead false → true | A1 | La barra sube por encima de la cabeza |
| 1041 | lever unilateral row | requires_kneeling false → true | A1 | Una rodilla en la almohadilla de la maquina |
| 1055 | modified push up to lower arms | requires_kneeling false → true | A1 | De rodillas en el suelo |
| 1058 | modified hindu push-up (male) | requires_inversion false → true | A1 | Posicion de pica, cabeza bajo la cadera con peso en brazos |
| 1068 | muscle up | difficulty_level 1 → 3; equipment pull-up bar → gymnastic rings; requisitos 1:pull-up bar → 1:gymnastic rings | A2,A3 | Muscle-up; el alto impacto ya lo excluye para principiantes |
| 1070 | one arm against wall | requires_overhead false → true | A1 | Brazo apoyado en la pared por encima de la cabeza |
| 1082 | medicine ball chest push multiple response | requires_floor false → true; requires_kneeling false → true | A1 | De rodillas en el suelo, cae sobre las manos |
| 1083 | medicine ball chest push single response | requires_floor false → true; requires_kneeling false → true | A1 | De rodillas en el suelo; cae sobre las manos |
| 1089 | medicine ball supine chest throw | requires_floor false → true | A1 | Tumbado boca arriba en el suelo |
| 1123 | push-up (on stability ball) | requires_floor false → true | A1 | Pies sobre el balon, manos en el suelo (flexion completa) |
| 1124 | prone twist on stability ball | requires_floor false → true | A1 | Manos y pies en el suelo sobre el balon |
| 1125 | pull-in (on stability ball) | requires_floor false → true | A1 | Manos en el suelo, pies en el balon |
| 1136 | seated calf stretch (male) | requires_floor false → true | A1 | Sentado en el suelo con piernas extendidas |
| 1152 | reverse dip | requires_floor false → true | A1 | Tumbado y sentado en el suelo |
| 1161 | reverse hyper extension (on stability ball) | requires_floor false → true | A1 | Manos en el suelo sobre el balon |
| 1185 | side-to-side toe touch (male) | requires_overhead false → true | A1 | De pie, brazos por encima de la cabeza; no es de suelo |
| 1188 | seated lower back stretch | requires_overhead false → true | A1 | Sentado con un brazo extendido por encima de la cabeza |
| 1192 | skin the cat | equipment body weight → gymnastic rings; requisitos ∅ → 1:gymnastic rings | A3 | Exige anillas y no las exige |
| 1195 | self assisted inverse leg curl | requires_kneeling false → true | A1 | Rodillas en la almohadilla, cae hasta las manos |
| 1196 | self assisted inverse leg curl | requires_floor false → true; requires_kneeling false → true | A1 | De rodillas en el suelo, cae hasta apoyar las manos |
| 1197 | self assisted inverse leg curl (on floor) | requires_kneeling false → true | A1 | De rodillas sobre un step |
| 1199 | side hip abduction | requires_floor false → true | A1 | Tumbado de lado en el suelo |
| 1225 | smith hip raise | requires_floor false → true | A1 | Tumbado en el suelo |
| 1237 | smith leg press | requires_floor false → true | A1 | Tumbado en el suelo bajo la barra de la multipower |
| 1294 | swing 360 | requires_hanging false → true; requires_overhead false → true; difficulty_level 1 → 3; equipment body weight → pull-up bar; requisitos ∅ → 1:pull-up bar | A1,A2,A3 | Colgado de la barra balanceandose |
| 1297 | suspended push-up | requires_floor false → true | A1 | Pies en el suelo, posicion de flexion |
| 1298 | straddle maltese | requires_floor false → true; difficulty_level 1 → 3 | A1,A2 | Manos en el suelo, cuerpo suspendido sobre los brazos |
| 1301 | suspended reverse crunch | requires_floor false → true; equipment pull-up bar → suspension straps; requisitos 1:pull-up bar → 1:suspension straps | A1,A3 | Manos en el suelo, pies en las correas |
| 1308 | two toe touch (male) | requires_overhead false → true | A1 | De pie, brazos arriba |
| 1320 | triceps dips floor | requires_floor false → true | A1 | Sentado en el suelo, manos en el suelo detras de la cadera |
| 1374 | wind sprints | requires_hanging false → true; requires_overhead false → true; equipment body weight → pull-up bar; requisitos ∅ → 1:pull-up bar; high_impact true → false; movement_pattern LOCOMOTION_CARDIO → TRUNK_FLEXION | A1,A3,A4 | Colgado de la barra elevando rodillas |
