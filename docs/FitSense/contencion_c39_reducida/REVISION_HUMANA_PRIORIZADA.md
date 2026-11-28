# Lista priorizada de revisión humana (antes de recomendar la fusión de #56–#59)

Fuente: `revision_humana_priorizada.tsv`, generada por `herramientas/lista_revision_humana.py` a partir de tres ficheros:
- el registro maestro del catálogo integral;
- el detalle de textos de V49;
- la exposición en la rejilla final de 168 perfiles (en cuántos perfiles aparece el ejercicio en la semana generada).

Es reproducible y de solo lectura.

## Prioridad

Cada ejercicio suma puntos:

| Criterio | Puntos |
|---|---|
| Texto reescrito por V49 | +1 |
| Nombre cambiado | +1 |
| El texto nuevo describe una acción de riesgo (salto, colgarse, suspensión, inversión, sobre la cabeza, peso muerto, anillas, impulso) | +2 |
| Banderas de seguridad marcadas | +2 |
| Banderas en duda | +2 |
| Apoyo suspendido | +3 |
| Gravedad ALTA | +2 |
| Gravedad MEDIA | +1 |
| Exposición en ≥ 10 de 168 perfiles | +2 |
| Exposición en 1–9 perfiles | +1 |

Niveles: **P1** ≥ 7 puntos, **P2** 4–6, **P3** < 4.

| Lista | P1 | P2 | P3 | Total |
|---|---|---|---|---|
| A · instrucciones reescritas por V49 | 57 | 173 | 116 | 346 |
| B · PENDIENTE_VISUAL | 6 | 18 | 35 | 59 |
| C · banderas pendientes de decisión | 15 | 62 | 31 | 108 |

Las listas no se solapan: cada ejercicio aparece una sola vez. B tiene preferencia sobre C, y C sobre A. Si un ejercicio de B o C también tiene texto de V49, se indica con «(también en la lista A)». En C solo entra lo que sigue abierto. Una duda sobre una bandera **ya marcada** está resuelta del lado conservador y no entra.

## Revisar primero (los diez de mayor impacto)

1. **853 «Marcha rápida en el sitio»**
   - Bandera de impacto (HI) en duda y sin marcar.
   - Aparece en **83 de 168 perfiles**.
   - V49 le cambió el nombre: antes era «Media sentadilla».
   - Es uno de los cardios de pie con los que los dos perfiles LOSE_WEIGHT recuperan su semana.
   - Si el GIF muestra fase de vuelo, debe llevar HI. Entonces saldría para los perfiles «sin impacto» y, por la política por nivel (`SafetyProfile`: un principiante no recibe alto impacto), para **todo principiante**, incluidos los dos perfiles recuperados.
   - En ese caso esos perfiles dependerían del 1093 y del 1117 y habría que volver a comprobar su factibilidad. No se ha simulado: es una decisión pendiente, no un hecho.
2. **1093 «Elevación de rodilla con patada frontal alterna»**
   - Aparece en 83 de 168 perfiles.
   - El nombre nuevo cambia su clase de impacto por nombre (§4).
   - Confirmar con el GIF que no hay carrera ni salto.
3. **1152 «Press a cobra desde el suelo»**
   - Liberado de la contención.
   - Gravedad ALTA y 11 perfiles.
   - Confirmar el texto nuevo contra el GIF. Ya está marcado como de suelo.
4. **143 «Jalón con agarre supino»** y **116 «Jalón sentado con banda anclada en alto»**: brazos sobre la cabeza marcado; 16 y 17 perfiles; textos nuevos.
5. **295 «Salto desde banco con aterrizaje a una pierna»**: impacto marcado; 10 perfiles; nombre y texto nuevos.
6. **1095, 1156 y 1181**: liberados de la contención, en 20, 24 y 13 perfiles. Confirmar que el texto nuevo (de pie, asiento con respaldo, sentadilla contra la pared) es el del GIF.
7. **148, 147 y 142** (peso muerto con banda): carga axial marcada «de más», no relajada, en 14, 14 y 11 perfiles. Su efecto es de factibilidad, no de seguridad.
8. **1120 «Flexión en antebrazos»**: PENDIENTE_VISUAL; 15 perfiles.
9. **494 «Flexión en reloj»**: impacto en duda, sin marcar; 9 perfiles.
10. **777 «Fondos entre codos»**: apoyo suspendido; 8 perfiles.

## A · Instrucciones de V49 de mayor riesgo (P1: 57)

Primeros de la lista, por prioridad y exposición:

| Id | Nombre final | Banderas | Exposición | Motivo |
|---|---|---|---|---|
| 295 | Salto desde banco con aterrizaje a una pierna | HI | 10 | nombre y texto nuevos; acción de riesgo |
| 143 | Jalón con agarre supino | OH | 16 | texto nuevo; acción de riesgo |
| 116 | Jalón sentado con banda anclada en alto | OH | 17 | nombre y texto nuevos |
| 1152 | Press a cobra desde el suelo | FL | 11 | liberado de C39; gravedad ALTA |
| 574 | Elevación inclinada con mancuerna | OH (duda OH) | 3 | acción de riesgo |
| 62, 61 | Elevación de piernas / rodillas colgado en correas abdominales | HG OH | 2, 0 | nombre nuevo |
| 702, 783, 536, 220, 949 | Press de Scott, press en W, prensa cubana, press Bradford, press con kettlebell | AX OH | 0–5 | acción de riesgo; carga axial |
| 497, 906 | Cocoons, abdominal en navaja | FL OH | 4, 3 | acción de riesgo |
| 894, 895 | Saltos y flexión pliométrica con caída | HI | 0 | nombre nuevo |
| 912, 316 | Remos invertidos (pies en banco; barra baja, liberado de C39) | FL | 1, 0 | nombre nuevo |
| 916 | Curl de piernas invertido en polea | KN | 1 | gravedad ALTA; V48 lo subió a nivel 3; está en el plan 78 |
| 1272 | Tracción snatch (liberado de C39) | AX HI | 1 | gravedad ALTA |
| 1004 | Peso muerto con palanca | AX | 5 | acción de riesgo |

Qué revisar: comparar las instrucciones nuevas con el GIF y confirmar que no falta ninguna advertencia. El detalle de antes y después está en `catalogo_integral/correcciones_texto_detalle.tsv`.

## B · PENDIENTE_VISUAL (59), empezando por suspensión, inversión e impacto

| Grupo | Ids (orden de prioridad) |
|---|---|
| Suspensión e inversión (7) | **1068** muscle up · **1191** dominada de lado a lado · **937** muscle up con kipping (2 perfiles) · **1184** cadera lateral en paralelas (apoyo suspendido, 4) · **65** plancha trasera (inversión en duda) · **1294** balanceo 360 (impacto e inversión en duda) · **913** remo invertido v2 (inversión marcada) |
| Impacto (7) | **1084** press con balón medicinal y carrera · **494** flexión en reloj (9) · **675** sentadilla pliométrica · **930** y **971** clean con kettlebell desde hang · **54** paso adelante y atrás · **940** jerk doble con kettlebell |
| Otros (45) | Encabezan 1214 y 1215 (elevaciones de talones invertidas en Smith, carga axial), 1019, 1188, 580, 1033, 1299, 535, 231 y 1120 (15 perfiles). Lista completa en el TSV. |

La inversión no tiene grupo propio porque todos sus casos están en el grupo de suspensión (65, 1294 y 913).

Mientras no se revisen, conservan su texto y sus banderas anteriores. Ningún ejercicio de B se liberó de una contención. El 1322, liberado de C39 por brazos arriba, figura como PENDIENTE_VISUAL solo por su nombre («de tríceps»): el GIF muestra el brazo cruzado ante el pecho, a la altura del hombro.

## C · Banderas pendientes de decisión profesional (108)

| Grupo | N | Primeros por prioridad |
|---|---|---|
| Apoyo suspendido sin clase en el esquema | 23 | 1069, **1301**, 995, **1153**, **1304**, **1303**, 777 (8 perfiles), 78, 76, 72, 75, **1306**, 73, 74, 1317, 77, 459, 458, 920, 934, 457, 462, 1335 |
| Bandera en duda **sin marcar** (riesgo sin mitigar) | 68 | **853** (HI, 83 perfiles), 299 (INV), 224 (INV), 514 (AX), 278 (AX/KN), 779 (AX), 368 (OH), 226 (INV), 243 (KN), **400** y **448** (OH), 617 (OH), 822 (INV), 825 (KN), 868 (INV) |
| Bandera «de más», no relajada | 17 | 148, 147 y 142 (AX, 11–14 perfiles), 550 y 549 (INV), 711, 618, 262, 296, 496, 533, 108, 797, 830, 888, 609, 952 |

- En negrita, los ejercicios que siguen contenidos por INC-CAT-C39 (1153, 1301, 1303, 1304, 1306, 400 y 448) y el 853.
- La decisión sobre el apoyo suspendido resuelve de golpe los cinco contenidos por colgarse (`catalogo_integral/PROPUESTA_APOYO_SUSPENDIDO.md`).
- Los otros 18 casos de apoyo suspendido no están contenidos: hoy los recibe quien declara «sin colgarse». El riesgo queda abierto (`RIESGOS_Y_ORDEN_DE_FUSION.md`, R2).

## 4. Dependencia entre el nombre del 1093 y su clase de impacto

**Qué ocurre:**
- `ImpactoPolicy` clasifica el impacto por expresiones regulares sobre el **nombre**.
- V49 cambió el nombre del 1093 de «Empujar para correr» a «Elevación de rodilla con patada frontal alterna». Al desaparecer «correr», su impacto pasa de MODERADO a BAJO.

**Efecto medido:**
- Ningún conjunto elegible cambia.
- Se reordena el cardio de los 84 perfiles de la rejilla con cardio en los que el 1093 es elegible, por el orden de impacto y la semilla determinista.
- 0 desenlaces cambiados (`catalogo_integral/IMPACTO_ELEGIBILIDAD.md` §3).

**Riesgo:** una traducción o un cambio de nombre puede cambiar en silencio una propiedad de seguridad y de prescripción. Es la única excepción documentada: `CatalogoIntegralTextosIT.reglasPorNombreIntactas` la exceptúa de forma explícita.

**Control actual:** `CatalogoCalidadIT` guarda en la huella la firma de las reglas por nombre (M/A/E por ejercicio). Cualquier renombrado que cambie una clase rompe la prueba y obliga a actualizar la huella en el mismo commit.

**Revisión humana pedida:**
- Confirmar con el GIF que el 1093 no tiene fase de carrera ni salto. Si la tiene, su impacto debería ser al menos MODERADO y la clase BAJA sería un error introducido por el nombre.
- Lo mismo para el 853 (punto 1).

**Propuesta (no implementada):** guardar el impacto en una columna del catálogo, revisada como las banderas, en lugar de deducirlo del nombre.

## Qué es seguro incorporar con la evidencia actual

| Cambio | Valoración | Por qué |
|---|---|---|
| V47 (28 banderas en 26 ejercicios) | **Seguro de incorporar** | Solo endurece. Cada bandera se verificó contra el GIF. |
| V48 (12 subidas de dificultad) | **Seguro de incorporar**, con el riesgo R4 | Solo restringe. Puede impedir reparar semanas publicadas que contengan esos ejercicios (plan 78: 916). |
| V50 (9 clasificaciones y 17 equipamientos) | **Seguro de incorporar** | Los equipamientos solo restringen. Las clasificaciones se verificaron con el GIF y cambian huecos sin cambiar desenlaces. |
| V49 (81 nombres y 372 instrucciones) | **Incorporable, con revisión humana de la lista A-P1 antes de producción** | Es lo que ve el participante. Redactado por revisores de IA con un protocolo estricto; advertencias conservadas y probadas. |
| Contención reducida a 8 (PR #55) | **Incorporar** | Mantiene contenido todo movimiento dudoso de suspensión o de brazos sobre la cabeza. Lo liberado tiene una justificación comprobada id por id. |
| Ejercicios de las listas B y C-1/C-2 | **Requieren revisión o contención adicional** | Ver `RIESGOS_Y_ORDEN_DE_FUSION.md`. |
