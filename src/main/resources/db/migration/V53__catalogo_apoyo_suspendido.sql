-- =====================================================================
-- FitSense - V53: APOYO SUSPENDIDO como atributo explicito del catalogo (cierre del catalogo)
--
-- SOLO DATO. El COMPORTAMIENTO esta APAGADO hasta la aprobacion del propietario (orden de desbloqueo, 14/11/2026:
-- "No actives el cambio hasta nuestra aprobacion del comportamiento final"). Esta migracion anade y rellena la columna;
-- el significado del permiso «no colgarse» (user_profiles.allow_hanging_exercises = false) NO cambia mientras la
-- propiedad fitsense.seguridad.apoyo-suspendido-en-no-colgarse sea false (valor por defecto).
--
-- Criterio conservador PROPUESTO (pendiente de aprobacion): «no colgarse» excluiria tambien el APOYO SUSPENDIDO SOBRE UN
-- APARATO SIN SOPORTE DE PIES: el cuerpo, sin los pies en el suelo ni en una plataforma, se sostiene sobre brazos
-- extendidos (fondos en paralelas, barra o anillas; muscle-up) o sobre los antebrazos (silla del capitan, paralelas).
-- Carga el hombro con todo el peso corporal, como colgarse o mas, y la caida desde el aparato es posible.
-- NO se marca: el apoyo parcial en maquina (rodillas o pies en la plataforma de asistencia), ni las posiciones con las
-- manos en el SUELO (planchas, maltes, pino, L-sit, y 1301 con los pies en correas y las manos en el suelo), donde no
-- hay aparato del que caer. Detalle: docs/FitSense/catalogo_integral/cierre/V53_APOYO_SUSPENDIDO.md
-- =====================================================================

ALTER TABLE exercises ADD COLUMN requires_suspended_support BOOLEAN NOT NULL DEFAULT FALSE;
COMMENT ON COLUMN exercises.requires_suspended_support IS
    'El cuerpo, sin los pies en el suelo ni en una plataforma, se sostiene sobre un aparato con los brazos extendidos o los antebrazos. '
    'Propuesta pendiente de aprobacion: la restriccion «no colgarse» lo excluiria solo con '
    'fitsense.seguridad.apoyo-suspendido-en-no-colgarse=true (apagado por defecto).';

-- Ejercicios con apoyo suspendido SOBRE UN APARATO SIN SOPORTE DE PIES (15), adjudicados con tres miradas al GIF completo
-- (segunda revision, verificacion independiente y coordinador) y revisados de nuevo contra el criterio conservador.
-- De los 16 candidatos de la primera version, 1301 (suspended reverse crunch) NO cumple el criterio: las manos estan en
-- el suelo y los pies en correas; sigue contenido por INC-CAT-C39. Las posiciones con las manos en el SUELO (855, 857,
-- 860, 861, 885, 974, 1298, 1299) se documentan pero NO se marcan.
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
