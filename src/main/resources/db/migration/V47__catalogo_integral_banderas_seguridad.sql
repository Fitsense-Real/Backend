-- V47 - Catalogo integral (auditoria visual 2026-11): banderas de seguridad que el GIF exige y faltaban.
-- SOLO pone a TRUE banderas de seguridad. Nunca relaja ninguna (las banderas de mas quedan para revision profesional).
-- Evidencia: revision visual del GIF original (8 fotogramas + imagen) por el revisor del lote y verificacion propia
-- independiente de cada caso. Detalle: docs/FitSense/catalogo_integral/. Cada fila exige el estado anterior exacto
-- (nombre de la fuente y bandera a FALSE): si el catalogo no es el esperado, la fila no cambia nada.
-- Efecto: el ejercicio deja de ser elegible para quien excluye esa condicion (perfil declarado o politica de edad).

-- 66 back pec stretch: de pie, inclinado, manos en el respaldo de un banco y brazos extendidos en prolongacion del tronco (flexion completa del hombro).
UPDATE exercises SET requires_overhead = TRUE
 WHERE exercise_id = 66 AND name_en = 'back pec stretch' AND requires_overhead = FALSE;
-- 206 barbell lying close-grip triceps extension: tumbado en banco, la barra baja por detras de la cabeza con los brazos en flexion de hombro mas alla de la vertical (como el 204, que ya tiene OH).
UPDATE exercises SET requires_overhead = TRUE
 WHERE exercise_id = 206 AND name_en = 'barbell lying close-grip triceps extension' AND requires_overhead = FALSE;
-- 474 cable upper chest crossovers: cruce de poleas de abajo arriba que termina con las manos juntas por encima de la cabeza.
UPDATE exercises SET requires_overhead = TRUE
 WHERE exercise_id = 474 AND name_en = 'cable upper chest crossovers' AND requires_overhead = FALSE;
-- 497 cocoons: tumbado en el suelo, empieza con los brazos extendidos por encima de la cabeza.
UPDATE exercises SET requires_overhead = TRUE
 WHERE exercise_id = 497 AND name_en = 'cocoons' AND requires_overhead = FALSE;
-- 503 dead bug: dead bug: tumbado, el brazo se extiende hacia atras por encima de la cabeza alternando.
UPDATE exercises SET requires_overhead = TRUE
 WHERE exercise_id = 503 AND name_en = 'dead bug' AND requires_overhead = FALSE;
-- 594 dumbbell incline y-raise: elevacion en Y en banco inclinado: los brazos terminan en prolongacion del tronco, por encima de la cabeza.
UPDATE exercises SET requires_overhead = TRUE
 WHERE exercise_id = 594 AND name_en = 'dumbbell incline y-raise' AND requires_overhead = FALSE;
-- 1300 suspended abdominal fallout: fallout en suspension: el cuerpo cae hacia delante con los brazos extendidos por encima de la cabeza.
UPDATE exercises SET requires_overhead = TRUE
 WHERE exercise_id = 1300 AND name_en = 'suspended abdominal fallout' AND requires_overhead = FALSE;
-- 149 barbell floor calf raise: elevacion de talones de pie con la barra cargada sobre la espalda.
UPDATE exercises SET axial_load = TRUE
 WHERE exercise_id = 149 AND name_en = 'barbell floor calf raise' AND axial_load = FALSE;
-- 220 barbell seated bradford rocky press: press Bradford: barra cargada por encima de la cabeza, alternando delante y detras de la nuca (219 y 221 ya tienen AX).
UPDATE exercises SET axial_load = TRUE
 WHERE exercise_id = 220 AND name_en = 'barbell seated bradford rocky press' AND axial_load = FALSE;
-- 227 barbell seated twist: giro sentado con barra cargada sobre los hombros detras de la nuca.
UPDATE exercises SET axial_load = TRUE
 WHERE exercise_id = 227 AND name_en = 'barbell seated twist' AND axial_load = FALSE;
-- 276 barbell standing twist: giro de pie con barra cargada sobre los hombros detras de la nuca.
UPDATE exercises SET axial_load = TRUE
 WHERE exercise_id = 276 AND name_en = 'barbell standing twist' AND axial_load = FALSE;
-- 819 farmers walk: paseo del granjero: mancuernas pesadas sostenidas de pie, caminando.
UPDATE exercises SET axial_load = TRUE
 WHERE exercise_id = 819 AND name_en = 'farmers walk' AND axial_load = FALSE;
-- 889 frankenstein squat: sentadilla Frankenstein: barra cargada apoyada en la parte delantera de los hombros.
UPDATE exercises SET axial_load = TRUE
 WHERE exercise_id = 889 AND name_en = 'frankenstein squat' AND axial_load = FALSE;
-- 999 lever gripless shrug: encogimiento de pie con la carga sobre los hombros (almohadillas de la maquina).
UPDATE exercises SET axial_load = TRUE
 WHERE exercise_id = 999 AND name_en = 'lever gripless shrug' AND axial_load = FALSE;
-- 1000 lever gripless shrug v. 2: encogimiento de pie con la carga sobre los hombros (almohadillas de la maquina).
UPDATE exercises SET axial_load = TRUE
 WHERE exercise_id = 1000 AND name_en = 'lever gripless shrug v. 2' AND axial_load = FALSE;
-- 1213 smith one leg floor calf raise: elevacion de talon en maquina Smith con la barra cargada sobre los hombros.
UPDATE exercises SET axial_load = TRUE
 WHERE exercise_id = 1213 AND name_en = 'smith one leg floor calf raise' AND axial_load = FALSE;
-- 1214 smith reverse calf raises: elevacion de talones en maquina Smith con la barra cargada sobre los hombros.
UPDATE exercises SET axial_load = TRUE
 WHERE exercise_id = 1214 AND name_en = 'smith reverse calf raises' AND axial_load = FALSE;
-- 275 barbell standing ab rollerout: rollout de pie con barra: termina en plancha baja con las manos a ras de suelo.
UPDATE exercises SET requires_floor = TRUE
 WHERE exercise_id = 275 AND name_en = 'barbell standing ab rollerout' AND requires_floor = FALSE;
-- 685 dumbbell over bench one arm neutral wrist curl: arrodillado en el suelo detras del banco (su gemelo 684 ya tiene FL y KN).
UPDATE exercises SET requires_floor = TRUE, requires_kneeling = TRUE
 WHERE exercise_id = 685 AND name_en = 'dumbbell over bench one arm neutral wrist curl' AND requires_floor = FALSE AND requires_kneeling = FALSE;
-- 805 exercise ball back extension with arms extended: arrodillado en el suelo con el torso sobre el balon.
UPDATE exercises SET requires_floor = TRUE, requires_kneeling = TRUE
 WHERE exercise_id = 805 AND name_en = 'exercise ball back extension with arms extended' AND requires_floor = FALSE AND requires_kneeling = FALSE;
-- 895 incline push up depth jump: las manos caen de los steps al suelo en cada repeticion.
UPDATE exercises SET requires_floor = TRUE
 WHERE exercise_id = 895 AND name_en = 'incline push up depth jump' AND requires_floor = FALSE;
-- 460 chest tap push-up (male): flexion explosiva: las dos manos despegan del suelo.
UPDATE exercises SET high_impact = TRUE
 WHERE exercise_id = 460 AND name_en = 'chest tap push-up (male)' AND high_impact = FALSE;
-- 496 drop push up: flexion con caida desde dos bancos y empuje explosivo de vuelta.
UPDATE exercises SET high_impact = TRUE
 WHERE exercise_id = 496 AND name_en = 'drop push up' AND high_impact = FALSE;
-- 696 dumbbell rear delt row_shoulder: remo a una mano con rodilla y mano apoyadas en el banco.
UPDATE exercises SET requires_kneeling = TRUE
 WHERE exercise_id = 696 AND name_en = 'dumbbell rear delt row_shoulder' AND requires_kneeling = FALSE;
-- 995 lever assisted chin-up: dominada asistida arrodillado sobre la almohadilla de asistencia.
UPDATE exercises SET requires_kneeling = TRUE
 WHERE exercise_id = 995 AND name_en = 'lever assisted chin-up' AND requires_kneeling = FALSE;
-- 1005 lever hip extension v. 2: una rodilla apoyada y cargando peso en la almohadilla baja de la maquina.
UPDATE exercises SET requires_kneeling = TRUE
 WHERE exercise_id = 1005 AND name_en = 'lever hip extension v. 2' AND requires_kneeling = FALSE;
