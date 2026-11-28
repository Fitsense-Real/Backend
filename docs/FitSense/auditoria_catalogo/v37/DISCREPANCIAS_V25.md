# Discrepancias con V25 (banderas de movimiento)

**Fecha:** 29/10/2026 · **Estado:** documental. No modifica V25.

V25 (`V25__movement_flags.sql`) fijó las banderas a partir de la hoja revisada `FitSense-banderas-revision-FINAL.xlsx`, con las definiciones de `reglas-banderas.md` v2. Según su cabecera:
- la hoja tenía una muestra de 360 filas con valor humano, que sirvió para las métricas;
- el resto se completó con el valor de la columna «Revisión».

No se reescriben esos registros.
La auditoría visual compara el GIF original con las definiciones **vigentes**:
- V10 `requires_floor`: «exige bajar al suelo y levantarse»;
- V25: «una rodilla apoya y carga peso»; «el cuerpo cuelga del agarre, pies sin apoyo»; «brazos por encima de la cabeza en alguna fase»; «cabeza claramente por debajo de la cadera con el peso en brazos, hombros o cabeza».

Cuando el GIF contradice el valor que dejó V25, la corrección se propone en V37 y se justifica abajo.
La propuesta no afirma que la revisión de V25 estuviera mal hecha: en muchos casos el texto de la fuente (nombre e instrucciones) describe otra cosa que el GIF.

| Ej. | Nombre de la fuente | Atributo | V25 (vigente) | Según el GIF | Veredicto | Gravedad | Bloque V37 | Justificación (GIF frente a la definición vigente) |
|---:|---|---|---|---|---|---|---|---|
| 55 | ankle circles | requires_floor | true | false | CONFIRMADO | BAJA | B3 | De pie (restriccion de mas) |
| 74 | assisted parallel close grip pull-up | requires_kneeling | false | true | CONFIRMADO | ALTA | A1 | Rodillas en la almohadilla de asistencia |
| 75 | assisted pull-up | requires_kneeling | false | true | CONFIRMADO | ALTA | A1 | Rodillas en la almohadilla de asistencia |
| 131 | band one arm overhead biceps curl | requires_overhead | true | false | CONFIRMADO | BAJA | B3 | Brazo a la altura del hombro (restriccion de mas) |
| 149 | barbell floor calf raise | axial_load | false | true | PROBABLE | MEDIA | — (no entra: probable) | Barra sobre la espalda, de pie (no es de suelo) |
| 187 | barbell glute bridge two legs on bench (male) | requires_floor | false | true | CONFIRMADO | ALTA | A1 | Tumbado en el suelo con las piernas sobre el banco |
| 253 | bear crawl | requires_kneeling | true | false | PROBABLE | BAJA | — (no entra: probable) | Gateo con rodillas en el aire (restriccion de mas) |
| 316 | biceps narrow pull-ups | requires_hanging / requires_overhead | true / true | false / false | CONFIRMADO | BAJA | A1,B1,B3 | No cuelga (restriccion de mas) |
| 316 | biceps narrow pull-ups | requires_floor | false | true | CONFIRMADO | ALTA | A1,B1,B3 | Tumbado bajo la barra, talones en el suelo (igual que 912) |
| 351 | cable high row (kneeling) | requires_overhead | false | true | CONFIRMADO | ALTA | A1 | Tira desde polea alta con brazos por encima de la cabeza |
| 371 | cable lying triceps extension v. 2 | requires_floor | false | true | CONFIRMADO | ALTA | A1 | Tumbado en el suelo |
| 387 | cable one arm straight back high row (kneeling) | requires_overhead | false | true | CONFIRMADO | ALTA | A1 | Polea alta, brazo por encima de la cabeza al inicio |
| 427 | cable side bend crunch (bosu ball) | requires_floor | false | true | CONFIRMADO | ALTA | A1,B2 | Tumbado sobre el bosu en el suelo |
| 574 | dumbbell incline raise | requires_overhead | true | false | PROBABLE | BAJA | — (no entra: probable) | No pasa de la altura del hombro (restriccion de mas) |
| 608 | dumbbell kneeling bicep curl exercise ball | requires_floor | true | false | PROBABLE | BAJA | — (no entra: probable) | Sobre el balon, no en el suelo |
| 633 | dumbbell one arm press on exercise ball | requires_overhead | true | false | CONFIRMADO | BAJA | B1,B3 | Press tumbado (restriccion de mas) |
| 649 | dumbbell one arm hammer press on exercise ball | requires_overhead | true | false | CONFIRMADO | BAJA | B1,B3 | Press tumbado (restriccion de mas) |
| 667 | dumbbell one arm reverse grip press | requires_overhead | true | false | CONFIRMADO | BAJA | B1,B2,B3 | Press tumbado, no por encima de la cabeza (restriccion de mas) |
| 805 | exercise ball back extension with arms extended | requires_kneeling | false | true | PROBABLE | ALTA | — (no entra: probable) | Rodillas apoyadas en el suelo |
| 810 | exercise ball lat stretch | requires_floor / requires_kneeling | false / false | true / true | CONFIRMADO | ALTA | A1 | De rodillas en el suelo con los brazos sobre el balon |
| 825 | exercise ball one leg prone lower body rotation | requires_floor | false | true | CONFIRMADO | ALTA | A1 | Manos en el suelo, piernas sobre el balon |
| 879 | glute bridge two legs on bench (male) | requires_floor | false | true | CONFIRMADO | ALTA | A1 | Tumbado en el suelo con los pies sobre el banco |
| 895 | incline push up depth jump | requires_floor | false | true | PROBABLE | ALTA | B2 | Baja las manos al suelo entre los apoyos |
| 910 | inverted row | requires_floor | false | true | CONFIRMADO | ALTA | A1 | Tumbado bajo la barra con talones en el suelo (igual que 912, que si la lleva) |
| 911 | inverted row bent knees | requires_floor | false | true | CONFIRMADO | ALTA | A1 | Tumbado bajo la barra, rodillas flexionadas (igual que 912) |
| 913 | inverted row v. 2 | requires_hanging / requires_inversion / requires_overhead | false / false / false | true / true / true | CONFIRMADO | ALTA | A1,A2,A3 | Colgado cabeza abajo de la barra con las piernas enganchadas |
| 914 | inverted row with straps | requires_floor | false | true | CONFIRMADO | ALTA | A1 | Remo con correas desde tumbado en el suelo (igual que 912) |
| 949 | kettlebell seated press | requires_floor | false | true | CONFIRMADO | ALTA | A1 | Sentado en el suelo con piernas abiertas |
| 1082 | medicine ball chest push multiple response | requires_floor / requires_kneeling | false / false | true / true | CONFIRMADO | ALTA | A1 | De rodillas en el suelo, cae sobre las manos |
| 1083 | medicine ball chest push single response | requires_floor / requires_kneeling | false / false | true / true | CONFIRMADO | ALTA | A1 | De rodillas en el suelo; cae sobre las manos |
| 1089 | medicine ball supine chest throw | requires_floor | false | true | CONFIRMADO | ALTA | A1 | Tumbado boca arriba en el suelo |
| 1123 | push-up (on stability ball) | requires_floor | false | true | CONFIRMADO | ALTA | A1 | Pies sobre el balon, manos en el suelo (flexion completa) |
| 1124 | prone twist on stability ball | requires_floor | false | true | CONFIRMADO | ALTA | A1 | Manos y pies en el suelo sobre el balon |
| 1156 | resistance band seated straight back row | requires_floor | true | false | CONFIRMADO | BAJA | B3 | Sentado en un asiento inclinado, no en el suelo |
| 1193 | seated piriformis stretch | requires_floor | true | false | CONFIRMADO | BAJA | B3 | Sentado en un taburete (restriccion de mas) |
| 1195 | self assisted inverse leg curl | requires_kneeling | false | true | CONFIRMADO | ALTA | A1 | Rodillas en la almohadilla, cae hasta las manos |
| 1278 | smith toe raise | axial_load | true | false | CONFIRMADO | BAJA | B3 | Sentado, la carga va sobre los muslos |
| 1297 | suspended push-up | requires_floor | false | true | CONFIRMADO | ALTA | A1 | Pies en el suelo, posicion de flexion |
| 1298 | straddle maltese | requires_floor | false | true | CONFIRMADO | ALTA | A1,A2,B3 | Manos en el suelo, cuerpo suspendido sobre los brazos |
| 1298 | straddle maltese | requires_hanging / requires_overhead | true / true | false / false | CONFIRMADO | BAJA | A1,A2,B3 | No cuelga |
| 1301 | suspended reverse crunch | requires_floor | false | true | CONFIRMADO | ALTA | A1,A3,B3 | Manos en el suelo, pies en las correas |
| 1301 | suspended reverse crunch | requires_hanging / requires_overhead | true / true | false / false | CONFIRMADO | BAJA | A1,A3,B3 | No cuelga (restriccion de mas) |
| 1303 | twisted leg raise | requires_floor | true | false | CONFIRMADO | BAJA | B2,B3 | De pie en la estacion (restriccion de mas) |
| 1304 | twisted leg raise (female) | requires_floor | true | false | CONFIRMADO | BAJA | B2,B3 | De pie en la estacion (restriccion de mas) |
| 1309 | upper back stretch | requires_overhead | true | false | CONFIRMADO | BAJA | B3 | Brazos a la altura del hombro (restriccion de mas) |
| 1311 | straight leg outer hip abductor | requires_floor | true | false | CONFIRMADO | BAJA | B3 | De pie (restriccion de mas) |
| 1320 | triceps dips floor | requires_floor | false | true | CONFIRMADO | ALTA | A1 | Sentado en el suelo, manos en el suelo detras de la cadera |
| 1322 | triceps stretch | requires_overhead | true | false | CONFIRMADO | BAJA | B3 | Brazo cruzado ante el pecho (restriccion de mas) |

**Resumen:** 45 ejercicios con algún valor de V25 contradicho por el GIF.
- 26 tienen una bandera de seguridad que falta (gravedad alta, bloque A de V37);
- el resto son restricciones de más (gravedad baja, V37.1) o casos probables que no entran en V37.
