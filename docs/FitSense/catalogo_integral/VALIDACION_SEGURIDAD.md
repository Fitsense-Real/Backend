# Validación de seguridad del cierre del catálogo

## 1. Regla de decisión aplicada

| Cambio | Exigencia |
|---|---|
| Añadir una bandera, subir el impacto, marcar apoyo suspendido | Segunda revisión (R2) y verificación independiente (V3) que ACEPTA. El coordinador puede vetar |
| Quitar una bandera, bajar el impacto | R2, V3 ACEPTAR **y** adjudicación expresa del coordinador con motivo (`adjudicacion/coordinador.tsv`) |
| Bajar una dificultad | **Nunca** (V27: exige aprobación humana). Hay 248 propuestas listadas para la revisión profesional |
| Cambiar el significado de un permiso del usuario | No se aplica en silencio: va en una PR propia, identificada como **decisión de significado** (V53) |

Para quitar una bandera, la V3 solo acepta si el GIF demuestra que la condición no se da **en ningún fotograma**. Si la fase no se ve, la decisión es NO_DECIDIBLE y la bandera se mantiene.

## 2. Banderas modificadas en V52

| Bandera | Añadidas | Quitadas |
|---|---|---|
| HI (alto impacto) | — | 5: 262, 267, 675, 952, 1069 |
| FL (suelo) | 2: 559, 896 | 2: 226, 296 |
| AX (carga axial) | 11: 224, 228, 229, 274, 321, 511, 514, 705, 779, 923, 927 | 3: 752, 830, 1043 |
| KN (rodilla apoyada) | 1: 636 | 5: 253, 811, 822, 888, 1375 |
| HG (colgarse) | — | — |
| OH (brazos sobre la cabeza) | 14: 105, 368, 380, 466, 580, 581, 814, 876, 904, 1001, 1031, 1242, 1260, 1314 | 12: 108, 269, 270, 275, 481, 503, 533, 574, 797, 805, 806, 894 |
| INV (invertido) | 4: 822, 857, 902, 905 | 2: 549 y 550, ambos en banco **inclinado** con la cabeza por encima de la cadera |

Motivos de las retiradas de HI:
- **262** (esquiador): pies apoyados en todos los fotogramas.
- **267 y 952** (thrusters): sentadilla y press, sin salto y sin ser olímpicos.
- **675:** el GIF es una sentadilla sumo. El nombre «pliométrica» y las instrucciones de salto se corrigen en V52, así que ninguna parte del ejercicio implica salto.
- **1069** (muscle-up en barra): sin salto ni aterrizaje. La bandera HG y la dificultad 3 siguen excluyéndolo de quien no debe colgarse.

Impacto explícito: 262, 267, 675, 952 y 1069 pasan de ALTO a BAJO; 1295 de MODERADO a BAJO. Al alza: 1083 BAJO→ALTO (pliometría del tren superior), 1117 BAJO→MODERADO (pies rápidos en el sitio) y 1333 MODERADO→ALTO (zancadas saltadas con vuelo medido).

## 3. Retiradas propuestas que NO se aplicaron

| Id | Propuesta | Motivo |
|---|---|---|
| 171, 377, 840, 841 | Quitar INV (banco declinado) | **DEC-INV-DECLINADO.** El catálogo trata el banco declinado de forma desigual: 540, 566, 1224 y 1241 conservan INV y la V3 lo confirmó. Unificar el criterio cambia el significado de «sin inversión» |
| 922, 930, 971 | Quitar HI (clean con pesa rusa) | Derivados olímpicos por nombre y fuente: ALTO por definición. El GIF es un fundido y la identidad es dudosa |
| 937, 1068 | Quitar HI (muscle-up en anillas) | Identidad AMBIGUA: el GIF no muestra el muscle-up completo |
| 1084, 1085, 1087 | Quitar HI (lanzamientos con balón medicinal) | **DEC-HI-LANZAMIENTOS.** No hay criterio fijado sobre si el lanzamiento balístico es pliometría |
| 1134 | Quitar HI (carrera en cinta) | **DEC-HI-CARRERA.** 1133 y 1175 (carrera) conservan HI; sin un criterio común no se relaja uno solo |
| 1177 | Quitar HI | Verificadores en desacuerdo, y el fundido entre poses no puede mostrar un vuelo |
| 913 | Quitar OH | Contrato vigente `ck_exercises_hanging_is_overhead` |
| 854 | Quitar HI (rodillas altas contra la pared) | R2 y V3 lo aceptan, pero la **decisión D2** (`DIAGNOSTICO_V37_1.md`) lo mantiene fuera «hasta una segunda revisión humana». La revisión del cierre es de IA: se respeta D2 |
| 65, 496, 608, 647, 711 | Quitar OH, KN o FL | La V3 no demuestra la condición en todos los fotogramas |
| 618, 300, 886, 823, 1293, 1224, 540, 566, 142 | Quitar OH, HG, FL, INV o AX | V3 RECHAZAR |

## 4. 853 y 1093 (impacto ligado al nombre)

- **853** «Marcha rápida en el sitio». Mi revisión, R2 y V3 V05 coinciden:
  - no hay ningún fotograma sin apoyo;
  - en f6–f8 y f18–f20 los dos pies están en el suelo en semiflexión, un doble apoyo que la carrera no tiene.

  Impacto **BAJO**, sin cambio. Su identidad sigue AMBIGUA (marcha con semiflexión o «half knee bends») y pasa a la revisión profesional.
- **1093:** R2 propuso MODERADO por ritmo de carrera. La V3 W06 lo **rechaza**: ningún fotograma con los dos pies en el aire, siempre un pie plano o de puntillas y ritmo pausado. Queda **BAJO**, coherente con lo documentado en V49.
- **Dependencia del nombre eliminada (V51):** ImpactoPolicy y la exclusión de estiramientos usan `impact_level` e `is_stretch`. Renombrar un ejercicio ya no cambia su impacto (`AtributosExplicitosV51Test`, `CatalogoAtributosExplicitosIT`).

## 5. Apoyo suspendido: 13 + 5 casos y V53

La revisión R2/V3 del catálogo completo encontró 32 casos y la adjudicación los reparte así:

| Clase | Ids | Tratamiento |
|---|---|---|
| Sobre un aparato: brazos extendidos | 457, 458, 459, 777, 920, 934, 1069, 1153, 1317, 1335 | **V53** |
| Sobre un aparato: antebrazos | 462, 1184, 1303, 1304, 1306 | **V53** |
| Pies en correas | 1301 | **V53** |
| Manos en el suelo (planchas, maltés, pino, L-sit) | 855, 857, 860, 861, 885, 974, 1298, 1299 | Documentado, **no se marca**: no hay aparato del que caer ni agarre |
| Máquina asistida | 72, 73, 74, 75, 76, 77, 78, 995 | Documentado, **no se marca**: la máquina reduce la carga |
| Rechazado por V3 | 821 (pies sobre balón) | — |

**V53 es una decisión de significado pendiente de tu aprobación:** «no colgarse» (`allow_hanging_exercises = false`) pasa a excluir también los 16 ejercicios marcados. Va en una PR propia que puede rechazarse sin afectar a V51 ni a V52. Mientras no se apruebe, la contención INC-CAT-C39 sigue protegiendo 1153, 1301, 1303, 1304 y 1306.

## 6. Invariantes comprobados tras V52 y V53

- Ningún activo con HI tiene impacto BAJO (`ck_exercises_high_impact_not_bajo`).
- Todo HG tiene OH.
- Todo activo tiene impacto y estiramiento explícitos.
- Las banderas declaradas coinciden con el texto (`CatalogoCalidadIT.textoContraBandera`, verde).
- La huella cambia exactamente en los 442 ejercicios decididos.

## 7. Lo que la IA no puede validar

Ninguna de estas decisiones es una validación profesional. Las tomaron revisores y verificadores de IA sobre el GIF completo y un coordinador de IA. La lista de decisiones que exigen a un profesional está en `REVISION_PROFESIONAL.md`, y su falta es un **bloqueo real del cierre**.
