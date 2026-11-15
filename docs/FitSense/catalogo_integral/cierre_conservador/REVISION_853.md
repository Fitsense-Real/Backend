# Revisión del ejercicio 853 (fase 2 de la orden de finalización)

**Decisión: 853 RECUPERABLE con corrección de texto (V56).** La animación completa permite determinar el movimiento. El nombre y las instrucciones vigentes (V49) no lo describían. V56 corrige solo esos dos campos; no cambia ningún atributo de seguridad. El 853 sale de la contención `INC-CAT-CONS-v1` (identidad) y los 4 perfiles LOSE_WEIGHT que dejaba sin plan vuelven a tenerlo.

Esto **no es una validación profesional**. La decisión P286 (bloque IDENTIDAD) sigue abierta en el registro de decisiones pendientes, con su ficha.

## 1. Evidencia revisada

| # | Elemento | Valor | Fuente |
|---|---|---|---|
| 1 | GIF completo | `videos/3221-ia6kIIl.gif`, 24 fotogramas (f0–f23), revisados todos | dataset del catálogo; hojas en `evidencia/853/` |
| 2 | Nombre original (fuente) | «half knee bends (male)» | `name_en`, sin cambios |
| 3 | Nombre en español anterior a V49 | «Media sentadilla» | condición `WHERE` de V49 (`V49__catalogo_integral_textos.sql`, bloque 853) |
| 4 | Nombre vigente antes de V56 | «Marcha rápida en el sitio» (V49) | `linea_base/catalogo_pre_cierre_V50.tsv` |
| 5 | Instrucciones originales (fuente, inglés) | Sentadilla genérica: «Bend your knees and lower your body down as if you were sitting back into a chair…». No describen el GIF | `linea_base/catalogo_pre_cierre_V50.tsv` |
| 6 | Instrucciones vigentes antes de V56 | «…Marcha en el sitio a ritmo vivo…». No mencionan la semiflexión con los dos pies apoyados | V49 |
| 7 | Clasificación cardio | Zona `cardio`, patrón `LOCOMOTION_CARDIO`, prescripción `DURATION` (D1/V38). **Sin cambios** | V38, huella |
| 8 | Impacto | `impact_level = BAJO` explícito (V51), `high_impact = false`. **Sin cambios** | V51, huella |
| 9 | Restricción de suelo y demás banderas | Ninguna: sin suelo, rodillas, colgarse, brazos arriba, inversión, carga axial ni apoyo suspendido. **Sin cambios** | huella |
| 10 | Equipamiento | Peso corporal (`body weight`), sin requisitos. **Sin cambios** | huella |
| 11 | R2 (segunda revisión) | Identidad AMBIGUA, confianza MEDIA. Movimiento observado: «alterna la elevación de una rodilla hasta la altura de la cadera, con el pie de apoyo en puntillas […] con una semiflexión de ambas rodillas con los dos pies en el suelo entre cada elevación». Pies `ALTERNOS_SIN_VUELO`, impacto BAJO. «No se ve ningún fotograma sin apoyo» | `adjudicacion/revision2_consolidada.tsv` (L03) |
| 12 | V3 (verificación independiente) | ACEPTAR AMBIGUA, confianza MEDIA. «f1 y f13 una rodilla elevada […] f6-f8 y f18-f20 ambos pies en el suelo con semiflexión de rodillas; la punta del pie de apoyo toca el suelo en todos los fotogramas y no hay fase de vuelo visible» | `adjudicacion/verificacion3_consolidada.tsv` (V05) |
| 13 | Por qué se excluyó | R2 marcó la identidad como AMBIGUA y el texto vigente no describía el GIF. Las dudas que dejaron R2/V3 eran: «marcha con rodillas altas» o «semiflexión alterna» (nombre de la fuente); R2 añadía que, «si se interpreta como trote», el impacto sería MODERADO | `ContencionConservadora` (versión anterior), P286 |

## 2. Lectura de la animación completa

Hojas: `evidencia/853/hoja_853.png` (los 24 fotogramas) y `evidencia/853/clave_853.png` (f0, f3, …, f21).

- **f0 y f12:** de pie, una rodilla elevada hasta la cadera. La pierna cambia de un fotograma al otro. El pie de apoyo está en el suelo, sobre el antepié, y los brazos acompañan.
- **f6 y f18:** semisentadilla con **los dos pies apoyados** en el suelo y las rodillas flexionadas.
- **f3, f9, f15 y f21:** fundidos entre las poses anteriores (la animación interpola cuatro poses clave).
- **Ningún fotograma sin apoyo.** En todos hay al menos un pie en el suelo.

El ciclo es: rodilla A arriba → semisentadilla con doble apoyo → rodilla B arriba → semisentadilla con doble apoyo.

**La fase de doble apoyo con flexión de rodillas en cada ciclo descarta un trote.** Un trote o una carrera no tienen fase de doble apoyo, y sí una de vuelo. Por eso la duda de R2 sobre el impacto no se confirma.

La ambigüedad estaba solo en el **nombre**: R2 y V3 coinciden en lo que se ve y difieren en cómo llamarlo. Las dos lecturas que barajaban («marcha con rodillas altas» y «semiflexión alterna») son de bajo impacto y sin vuelo. El GIF contiene las dos cosas: la elevación de rodilla **y** la media sentadilla. El nombre de la fuente («half knee bends») y el nombre en español anterior a V49 («Media sentadilla») corresponden a la parte de semiflexión.

## 3. Corrección (V56)

`V56__catalogo_853_texto_segun_gif.sql`:

- **Nombre:** «Media sentadilla con elevación de rodilla alterna».
- **Instrucciones (6 pasos):** describen la elevación alterna de rodilla con el pie de apoyo firme, la media sentadilla con los dos pies apoyados y el tronco erguido, y piden expresamente: «**Mantén siempre al menos un pie en el suelo: no saltes.**»

Salvaguardas de la migración:

- Exige el estado anterior exacto (nombre y texto de V49). Si no coincide y no está ya corregido, **falla**.
- Comprueba después que los atributos de seguridad son exactamente los revisados: activo, `LOCOMOTION_CARDIO`, `DURATION`, dificultad 1, peso corporal, ninguna bandera (incluidas carga axial y apoyo suspendido) e impacto BAJO. Si no lo son, **falla**.
- **No cambia** zona, patrón, prescripción, dificultad, equipamiento, banderas ni `is_active`. No relaja ninguna restricción. Las reglas por nombre no cambian de resultado (`CatalogoIntegralTextosIT`, `AtributosExplicitosV51Test`), y el impacto explícito manda sobre el nombre (V51).
- Huella del catálogo recapturada **en el mismo commit**: cambia solo la columna de hash de textos de la fila 853 (`8d47cacf1192 → 3544a0d48a1d`).

Código: `ContencionConservadora.IDENTIDAD_NO_DETERMINADA` pasa de 20 a **19** ids (sale 853). Pruebas: `ExternalCatalogServiceContencionConservadoraTest` (853 ya no se contiene) y `CatalogoIntegralTextosIT` (V56 reescribe el texto de V49; el texto final lo fija la huella).

## 4. Los 4 perfiles LOSE_WEIGHT de la rejilla de 168

Todos con objetivo LOSE_WEIGHT, en casa, equipamiento de peso corporal, mancuernas y banda, 4 días (lunes, miércoles, viernes y sábado) y 45 minutos por sesión.

| Perfil | Nivel | Restricciones | Elegibles sin 853 → con 853 | Cardio elegible sin 853 | Validador que rechazaba | Con V56 |
|---|---|---|---|---|---|---|
| LOSE_WEIGHT/BEGINNER/HOME/SIN_SUELO | Principiante | sin suelo | 91 → 92 | 1093, 1117 | Motor de reglas: V5 «1 ejercicio» + V25 «no incluye CARDIO» (`PlanGenerationFailedException`) | **PLAN** (853 en 3 de 4 sesiones) |
| LOSE_WEIGHT/BEGINNER/HOME/SIN_TODO | Principiante | sin impacto, suelo, rodillas, colgarse ni brazos arriba | 65 → 66 | 1093, 1117 | ídem | **PLAN** |
| LOSE_WEIGHT/INTERMEDIATE/HOME/SIN_TODO | Intermedio | ídem | 132 → 133 | 1093, 1117 | ídem | **PLAN** |
| LOSE_WEIGHT/ADVANCED/HOME/SIN_TODO | Avanzado | ídem | 148 → 149 | 1093, 1117 | ídem | **PLAN** |

**Capacidad no cubierta sin el 853:** cardio de pie, sin suelo y de bajo impacto en casa. Con solo dos cardios (1093 y 1117), el motor de reglas no puede poner cardio en las 4 sesiones con la variedad que exige (V5, V25).

**Alternativas:** no hay ninguna otra en el catálogo vigente. Ningún otro cardio de casa sin suelo y de bajo impacto es elegible para estos perfiles. Los demás cardios de casa exigen suelo o son de impacto, y no se relajan V20, V24, V25 ni las restricciones de impacto.

**Resultado con V56:** la rejilla de 168 con la contención encendida da **168 PLAN** (antes 164 + 4 sin plan). La diferencia de elegibles con la contención apagada queda en un solo ejercicio en estos 4 perfiles, el 1311, que sigue contenido.

## 5. Lo que queda abierto

- **P286 sigue ABIERTA** para la revisión profesional: confirmar el ejercicio y su dosis.
- **Riesgo residual:** alguien puede ejecutarlo con rebote o saltando. El texto lo prohíbe expresamente y el impacto registrado (BAJO) corresponde al movimiento que se indica, sin vuelo. Ver `RIESGOS_RESIDUALES.md`.
- La clasificación del 853 pasa de C (excluido temporalmente) a **A** (elegible). El motivo queda explícito en `catalogo_clasificado.tsv`, y A **no** significa validado.
