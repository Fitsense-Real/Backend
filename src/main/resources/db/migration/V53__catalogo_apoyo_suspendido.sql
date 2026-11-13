-- =====================================================================
-- FitSense - V53: APOYO SUSPENDIDO como atributo explicito del catalogo (cierre del catalogo)
--
-- DECISION DE SIGNIFICADO PENDIENTE DE APROBACION (ver docs/FitSense/catalogo_integral/cierre/VALIDACION_SEGURIDAD.md).
-- Hasta ahora «no colgarse» (user_profiles.allow_hanging_exercises = false) excluia solo requires_hanging: el cuerpo
-- cuelga del agarre. Con esta migracion excluye tambien el APOYO SUSPENDIDO: el cuerpo, sin los pies en el suelo, se
-- sostiene SOBRE brazos extendidos (fondos en paralelas o anillas), antebrazos (silla romana o del capitan) o con los
-- pies en correas o anillas. Carga el hombro con todo el peso corporal, como colgarse o mas, y la caida es posible.
-- El apoyo parcial en maquina (rodillas o pies en la plataforma de asistencia) NO se marca: no hay suspension.
--
-- Sustituye, con un dato comprobable, la contencion por lista de ids de INC-CAT-C39 para colgarse.
-- =====================================================================

ALTER TABLE exercises ADD COLUMN requires_suspended_support BOOLEAN NOT NULL DEFAULT FALSE;
COMMENT ON COLUMN exercises.requires_suspended_support IS
    'El cuerpo, sin los pies en el suelo, se sostiene sobre brazos extendidos, antebrazos o con los pies en correas. '
    'Lo excluye la restriccion «no colgarse» (decision de significado del cierre del catalogo, V53).';

-- Ejercicios con apoyo suspendido SOBRE UN APARATO (16), adjudicados con tres miradas al GIF completo (segunda revision,
-- verificacion independiente y coordinador). Las posiciones con las manos en el SUELO (planchas, maltes, pino, L-sit:
-- 855, 857, 860, 861, 885, 974, 1298, 1299) se documentan pero NO se marcan: no hay aparato del que caer ni agarre, y ampliar a ellas
-- «no colgarse» estiraria aun mas el significado del permiso. Todas son de dificultad 3.
-- Detalle y evidencia: docs/FitSense/catalogo_integral/cierre/adjudicacion/apoyo_suspendido_v53.tsv
CREATE TEMP TABLE v53_apoyo (exercise_id BIGINT PRIMARY KEY, name_en TEXT NOT NULL, clase TEXT NOT NULL) ON COMMIT DROP;
INSERT INTO v53_apoyo (exercise_id, name_en, clase) VALUES
    (457, 'chest dip', 'BRAZOS_EXTENDIDOS'),
    (458, 'chest dip (on dip-pull-up cage)', 'BRAZOS_EXTENDIDOS'),
    (459, 'chest dip on straight bar', 'BRAZOS_EXTENDIDOS'),
    (462, 'captains chair straight leg raise', 'ANTEBRAZOS'),
    (777, 'elbow dips', 'BRAZOS_EXTENDIDOS'),
    (920, 'impossible dips', 'BRAZOS_EXTENDIDOS'),
    (934, 'korean dips', 'BRAZOS_EXTENDIDOS'),
    (1069, 'muscle-up (on vertical bar)', 'BRAZOS_EXTENDIDOS'),
    (1153, 'ring dips', 'BRAZOS_EXTENDIDOS'),
    (1184, 'side hip (on parallel bars)', 'ANTEBRAZOS'),
    (1301, 'suspended reverse crunch', 'PIES_SUSPENDIDOS'),
    (1303, 'twisted leg raise', 'ANTEBRAZOS'),
    (1304, 'twisted leg raise (female)', 'ANTEBRAZOS'),
    (1306, 'vertical leg raise (on parallel bars)', 'ANTEBRAZOS'),
    (1317, 'triceps dip', 'BRAZOS_EXTENDIDOS'),
    (1335, 'wide-grip chest dip on high parallel bars', 'BRAZOS_EXTENDIDOS');

DO $v53$ DECLARE n INT; BEGIN
    SELECT count(*) INTO n FROM v53_apoyo a JOIN exercises e ON e.exercise_id = a.exercise_id AND e.name_en = a.name_en;
    IF n <> (SELECT count(*) FROM v53_apoyo) THEN
        RAISE EXCEPTION 'V53: % de % ejercicios no coinciden con su nombre de la fuente', (SELECT count(*) FROM v53_apoyo) - n, (SELECT count(*) FROM v53_apoyo);
    END IF;
END $v53$;

UPDATE exercises e SET requires_suspended_support = TRUE FROM v53_apoyo a WHERE a.exercise_id = e.exercise_id;
