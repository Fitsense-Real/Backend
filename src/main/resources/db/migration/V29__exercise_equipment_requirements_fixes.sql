-- =====================================================================
-- FitSense MVP 1.0 - V29 (CANDIDATA, NO APLICADA): el requisito de banco
-- que falta en ejercicios cuya instruccion lo exige sin alternativa
--
-- GENERADA AUTOMATICAMENTE por generar_v29.py el 2026-10-12.
-- No editar a mano: regenerar desde el script y volver a copiar.
--
-- Requiere V22 (codigos de equipamiento), V23 (tabla de requisitos) y
-- V24 (requisitos revisados). NO modifica nada de la V23 ni de la V24.
--
-- QUE HACE
--   80 ejercicios reciben UN requisito nuevo de banco (equipment_code
--      'bench'), en un grupo NUEVO, de modo que se suma con AND a los
--      requisitos que ya tenian. Ninguna fila existente se borra ni se
--      modifica: la migracion es solo INSERT.
--   17 de ellos ocupan uno de los cuatro slots obligatorios de 4C.
--   4 de ellos aparecieron en el plan 3 generado el 12/10/2026 con un
--      perfil HOME que NO declaraba banco.
--
-- POR QUE
-- La V23 sembro grupo 1 = exercises.equipment_id para todo lo que no era
-- 'body weight'. Para estos ejercicios eso dejo un unico requisito de
-- mancuerna (o ninguno), y el banco -que la propia instruccion exige, sea
-- para montarlo en angulo, para tumbarse encima o porque es un aparato con
-- nombre propio- no quedo exigido en ninguna parte. La V24 solo reviso 214
-- ejercicios y ninguno de estos estaba entre ellos.
--
-- CRITERIO (fijado antes de clasificar, aplicado a la instruccion completa)
--   ENTRA  si hay que montar el banco en angulo, tumbarse sobre el, o es
--          un aparato con nombre propio (preacher, hyperextension).
--   NO ENTRA si la instruccion ofrece alternativa universal ('bench or
--          chair', 'bench or step', 'mat or bench'): exigir el banco
--          crearia un falso NEGATIVO.
--   NO ENTRA si solo dice 'sit on a bench': una silla serviria. Son 40
--          ejercicios y es una decision de producto, no un hallazgo.
--
-- COMO LO HACE
-- Igual que la V21 y la V24: identifica cada ejercicio por source_code y
-- comprueba ademas name_en. Si algo no coincide, aborta sin tocar nada.
-- El grupo nuevo se calcula como MAX(group_no) + 1 del propio ejercicio,
-- asi que la semantica AND se respeta cualquiera que sea el estado previo.
--
-- LO QUE ESTA MIGRACION **NO** ARREGLA
-- El modelo de datos tiene un unico codigo de banco. No distingue plano,
-- inclinable, declinable, preacher ni hyperextension, y de los 80 de aqui
-- 30 exigen inclinacion, 11 declinacion, 12 son aparatos con nombre propio
-- y 27 valen con un banco plano. Quien declare un banco plano seguira
-- recibiendolos todos. Es una aproximacion conservadora: corrige el falso
-- positivo grande (no tener banco) y deja uno pequeno (tener el banco
-- equivocado). Los puntos de anclaje y las sillas tampoco son
-- representables; ver revision-V29-20261012.md, seccion 5.
--
-- NO editar ni reformatear este archivo despues de aplicarlo
-- (checksum de Flyway, incidente de V18 y V19).
-- =====================================================================

-- ---------------------------------------------------------------------
-- Requisito: V23 aplicada (tabla exercise_equipment_requirements).
-- ---------------------------------------------------------------------
DO $$
BEGIN
    IF to_regclass('exercise_equipment_requirements') IS NULL THEN
        RAISE EXCEPTION 'V29: falta la tabla exercise_equipment_requirements. Se aplico V23?';
END IF;
END $$;

-- ---------------------------------------------------------------------
-- Requisito: el codigo 'bench' tiene que existir.
-- ---------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM equipment_types WHERE code = 'bench') THEN
        RAISE EXCEPTION 'V29: falta el codigo de equipamiento bench. Se aplico V22?';
END IF;
END $$;

-- ---------------------------------------------------------------------
-- Los ejercicios y el requisito que se les anade.
--
-- La columna de comentario trae el id observado el 12/10/2026 y la frase
-- literal de instructions_en que justifica el requisito. El id es
-- informativo: lo que identifica es source_code + name_en.
-- ---------------------------------------------------------------------
CREATE TEMP TABLE v29_req (
    source_code    VARCHAR(20)  PRIMARY KEY,
    name_en        VARCHAR(255) NOT NULL,
    equipment_code VARCHAR(40)  NOT NULL
);

INSERT INTO v29_req (source_code, name_en, equipment_code) VALUES
                                                               ('0277', 'decline crunch', 'bench'),  -- id 504
                                                               -- "Lie on a decline bench with your feet secured and your knees bent at a 90-degree angle."
                                                               ('0282', 'decline sit-up', 'bench'),  -- id 505
                                                               -- "Lie on a decline bench with your feet secured and your knees bent."
                                                               ('0288', 'dumbbell around pullover', 'bench'),  -- id 516
                                                               -- "Lie flat on a bench with your head at one end and your feet firmly on the ground."
                                                               ('1646', 'dumbbell alternate hammer preacher curl', 'bench'),  -- id 522
                                                               -- "Sit on a preacher bench with a dumbbell in each hand, palms facing your torso and arms fully extended."
                                                               ('1647', 'dumbbell alternate preacher curl', 'bench'),  -- id 523
                                                               -- "Sit on a preacher bench with a dumbbell in each hand, palms facing up."
                                                               ('0302', 'dumbbell decline fly', 'bench'),  -- id 541
                                                               -- "Lie on a decline bench with your feet secured and your head lower than your hips."
                                                               ('0303', 'dumbbell decline hammer press', 'bench'),  -- id 542  [UPPER_PUSH]
                                                               -- "Lie on a decline bench with your feet secured and your head lower than your hips."
                                                               ('1276', 'dumbbell decline one arm fly', 'bench'),  -- id 543
                                                               -- "Lie down on a decline bench with a dumbbell in one hand, resting it on your thigh."
                                                               ('0307', 'dumbbell decline twist fly', 'bench'),  -- id 544
                                                               -- "Lie down on a decline bench with your head lower than your hips."
                                                               ('0308', 'dumbbell fly', 'bench'),  -- id 545
                                                               -- "Lie flat on a bench with a dumbbell in each hand, palms facing each other."
                                                               ('3545', 'dumbbell incline alternate press', 'bench'),  -- id 547  [UPPER_PUSH]
                                                               -- "Sit on an incline bench with a dumbbell in each hand, resting on your thighs."
                                                               ('0305', 'dumbbell decline shrug', 'bench'),  -- id 549
                                                               -- "Set up a decline bench at a 45-degree angle."
                                                               ('0304', 'dumbbell decline shrug v. 2', 'bench'),  -- id 550
                                                               -- "Set up a decline bench at a 45-degree angle."
                                                               ('1617', 'dumbbell decline one arm hammer press', 'bench'),  -- id 565  [UPPER_PUSH]
                                                               -- "Lie on a decline bench with a dumbbell in one hand, resting on your chest."
                                                               ('0306', 'dumbbell decline triceps extension', 'bench'),  -- id 566
                                                               -- "Lie on a decline bench with your head lower than your feet and hold a dumbbell in each hand, palms facing each other."
                                                               ('0323', 'dumbbell incline one arm lateral raise', 'bench'),  -- id 573
                                                               -- "Sit on an incline bench with a dumbbell in one hand, resting it on your thigh."
                                                               ('0325', 'dumbbell incline raise', 'bench'),  -- id 574  [UPPER_PUSH]
                                                               -- "Sit on an incline bench with a dumbbell in each hand, resting on your thighs."
                                                               ('0326', 'dumbbell incline rear lateral raise', 'bench'),  -- id 575  [EN EL PLAN 3]
                                                               -- "Set up an incline bench at a 45-degree angle."
                                                               ('3542', 'dumbbell incline t-raise', 'bench'),  -- id 576  [EN EL PLAN 3]
                                                               -- "Set an incline bench to a 45-degree angle and sit on it with a dumbbell in each hand, palms facing inwards."
                                                               ('0316', 'dumbbell incline breeding', 'bench'),  -- id 580  [UPPER_PUSH]
                                                               -- "Set up an incline bench at a 45-degree angle."
                                                               ('0319', 'dumbbell incline fly', 'bench'),  -- id 581  [EN EL PLAN 3]
                                                               -- "Set an incline bench to a 45-degree angle."
                                                               ('0321', 'dumbbell incline hammer press', 'bench'),  -- id 583  [UPPER_PUSH]
                                                               -- "Set an incline bench to a 45-degree angle and sit on it with a dumbbell in each hand, resting on your thighs."
                                                               ('1279', 'dumbbell incline one arm fly', 'bench'),  -- id 584
                                                               -- "Adjust the incline bench to a 30-45 degree angle."
                                                               ('1281', 'dumbbell incline one arm press', 'bench'),  -- id 586  [UPPER_PUSH]
                                                               -- "Sit on an incline bench with a dumbbell in one hand, resting on your thigh."
                                                               ('0324', 'dumbbell incline palm-in press', 'bench'),  -- id 588  [UPPER_PUSH]
                                                               -- "Set up an incline bench at a 45-degree angle."
                                                               ('0328', 'dumbbell incline shoulder raise', 'bench'),  -- id 590
                                                               -- "Set an incline bench to a 45-degree angle and sit on it with a dumbbell in each hand, palms facing down."
                                                               ('0331', 'dumbbell incline twisted flyes', 'bench'),  -- id 591
                                                               -- "Set an incline bench to a 45-degree angle and sit on it with a dumbbell in each hand, palms facing each other."
                                                               ('0327', 'dumbbell incline row', 'bench'),  -- id 592  [UPPER_PULL, EN EL PLAN 3]
                                                               -- "Set up an incline bench at a 45-degree angle."
                                                               ('0329', 'dumbbell incline shrug', 'bench'),  -- id 593
                                                               -- "Set an incline bench to a 45-degree angle and sit on it with a dumbbell in each hand."
                                                               ('3541', 'dumbbell incline y-raise', 'bench'),  -- id 594
                                                               -- "Set an incline bench to a 45-degree angle and sit on it with a dumbbell in each hand, palms facing inwards."
                                                               ('0315', 'dumbbell incline biceps curl', 'bench'),  -- id 596
                                                               -- "Sit on an incline bench with a dumbbell in each hand, palms facing forward, and arms fully extended."
                                                               ('0318', 'dumbbell incline curl', 'bench'),  -- id 597
                                                               -- "Set an incline bench to a 45-degree angle and sit on it with a dumbbell in each hand, palms facing forward."
                                                               ('0317', 'dumbbell incline curl v. 2', 'bench'),  -- id 598
                                                               -- "Sit on an incline bench with a dumbbell in each hand, palms facing forward and arms fully extended."
                                                               ('0320', 'dumbbell incline hammer curl', 'bench'),  -- id 599
                                                               -- "Sit on an incline bench with a dumbbell in each hand, palms facing your torso and arms fully extended."
                                                               ('0322', 'dumbbell incline inner biceps curl', 'bench'),  -- id 601
                                                               -- "Sit on an incline bench with a dumbbell in each hand, palms facing each other."
                                                               ('1619', 'dumbbell incline one arm hammer press', 'bench'),  -- id 602  [UPPER_PUSH]
                                                               -- "Sit on an incline bench with a dumbbell in one hand, resting on your thigh."
                                                               ('0330', 'dumbbell incline triceps extension', 'bench'),  -- id 604
                                                               -- "Sit on an incline bench with a dumbbell in each hand, palms facing inwards."
                                                               ('1733', 'dumbbell incline two arm extension', 'bench'),  -- id 605
                                                               -- "Sit on an incline bench with a dumbbell in each hand, resting on your thighs."
                                                               ('0337', 'dumbbell lying extension (across face)', 'bench'),  -- id 610
                                                               -- "Lie flat on a bench with your feet flat on the ground and your head at the end of the bench."
                                                               ('1729', 'dumbbell lying alternate extension', 'bench'),  -- id 611
                                                               -- "Lie flat on a bench with a dumbbell in each hand, palms facing each other."
                                                               ('0338', 'dumbbell lying elbow press', 'bench'),  -- id 612
                                                               -- "Lie flat on a bench with a dumbbell in each hand, palms facing each other and arms extended straight up over your chest."
                                                               ('0347', 'dumbbell lying pronation', 'bench'),  -- id 613
                                                               -- "Lie flat on a bench with your chest facing down and your arms extended straight down, holding a dumbbell in each hand."
                                                               ('0349', 'dumbbell lying supination', 'bench'),  -- id 615
                                                               -- "Lie flat on a bench with your feet flat on the ground."
                                                               ('0863', 'dumbbell lying external shoulder rotation', 'bench'),  -- id 617
                                                               -- "Lie on your side on a flat bench with your upper arm against your side and your elbow bent 90 degrees."
                                                               ('0341', 'dumbbell lying one arm deltoid rear', 'bench'),  -- id 619
                                                               -- "Lie face down on a flat bench with a dumbbell in one hand, palm facing inwards."
                                                               ('0345', 'dumbbell lying one arm rear lateral raise', 'bench'),  -- id 620
                                                               -- "Lie face down on a flat bench with a dumbbell in one hand, hanging towards the floor."
                                                               ('0348', 'dumbbell lying rear lateral raise', 'bench'),  -- id 621
                                                               -- "Lie face down on a flat bench with a dumbbell in each hand, palms facing each other."
                                                               ('0340', 'dumbbell lying hammer press', 'bench'),  -- id 624  [UPPER_PUSH]
                                                               -- "Lie flat on a bench with a dumbbell in each hand, palms facing each other and arms extended straight up."
                                                               ('0343', 'dumbbell lying one arm press', 'bench'),  -- id 625  [UPPER_PUSH]
                                                               -- "Lie flat on a bench with a dumbbell in one hand and your feet flat on the ground."
                                                               ('0342', 'dumbbell lying one arm press v. 2', 'bench'),  -- id 626  [UPPER_PUSH]
                                                               -- "Lie flat on a bench with your back supported and feet flat on the ground."
                                                               ('1287', 'dumbbell one arm decline chest press', 'bench'),  -- id 630  [UPPER_PUSH]
                                                               -- "Lie on a decline bench with a dumbbell in one hand, resting on your chest."
                                                               ('1289', 'dumbbell one arm incline chest press', 'bench'),  -- id 632  [UPPER_PUSH]
                                                               -- "Adjust the incline bench to a 45-degree angle."
                                                               ('1328', 'dumbbell lying rear delt row', 'bench'),  -- id 635  [UPPER_PULL]
                                                               -- "Lie face down on a flat bench with a dumbbell in each hand, palms facing inwards."
                                                               ('0344', 'dumbbell lying one arm pronated triceps extension', 'bench'),  -- id 638
                                                               -- "Lie flat on a bench with your back and head supported, and your feet flat on the ground."
                                                               ('0346', 'dumbbell lying one arm supinated triceps extension', 'bench'),  -- id 639
                                                               -- "Lie flat on a bench with your back and head supported, and your feet flat on the ground."
                                                               ('1735', 'dumbbell lying single extension', 'bench'),  -- id 640
                                                               -- "Lie flat on a bench with a dumbbell in one hand and your arm fully extended above your chest."
                                                               ('1661', 'dumbbell lying supine biceps curl', 'bench'),  -- id 641
                                                               -- "Lie flat on a bench with your back and head supported, and your feet flat on the ground."
                                                               ('0350', 'dumbbell lying supine curl', 'bench'),  -- id 642
                                                               -- "Lie flat on a bench with a dumbbell in each hand, palms facing up and arms fully extended."
                                                               ('0351', 'dumbbell lying triceps extension', 'bench'),  -- id 643
                                                               -- "Lie flat on a bench with a dumbbell in each hand, palms facing each other."
                                                               ('1662', 'dumbbell lying wide curl', 'bench'),  -- id 644
                                                               -- "Lie flat on a bench with a dumbbell in each hand, palms facing up and arms fully extended."
                                                               ('1663', 'dumbbell one arm hammer preacher curl', 'bench'),  -- id 648
                                                               -- "Sit on a preacher bench with a dumbbell in one hand and your upper arm resting on the pad."
                                                               ('1665', 'dumbbell one arm prone curl', 'bench'),  -- id 651
                                                               -- "Lie face down on a flat bench with a dumbbell in one hand, palm facing down."
                                                               ('1666', 'dumbbell one arm prone hammer curl', 'bench'),  -- id 652
                                                               -- "Lie face down on a flat bench with a dumbbell in one hand, palm facing your body and arm fully extended."
                                                               ('0375', 'dumbbell pullover', 'bench'),  -- id 670
                                                               -- "Lie flat on a bench with your head at one end and your feet on the floor."
                                                               ('1414', 'dumbbell one arm reverse preacher curl', 'bench'),  -- id 676
                                                               -- "Sit on a preacher bench with your chest against the pad and your arm extended over the edge of the bench, holding a dumbbell with an underhand grip."
                                                               ('1672', 'dumbbell one arm zottman preacher curl', 'bench'),  -- id 683
                                                               -- "Sit on a preacher curl bench and hold a dumbbell in one hand with an underhand grip."
                                                               ('0372', 'dumbbell preacher curl', 'bench'),  -- id 688
                                                               -- "Sit on a preacher curl bench with your upper arms resting on the pad and your chest against it."
                                                               ('0374', 'dumbbell prone incline curl', 'bench'),  -- id 691
                                                               -- "Adjust the bench to a 45-degree incline."
                                                               ('1674', 'dumbbell prone incline hammer curl', 'bench'),  -- id 692
                                                               -- "Adjust the bench to a 45-degree incline."
                                                               ('0384', 'dumbbell reverse preacher curl', 'bench'),  -- id 717
                                                               -- "Sit on a preacher bench with your upper arms resting on the pad and your chest against the support."
                                                               ('0402', 'dumbbell seated preacher curl', 'bench'),  -- id 756
                                                               -- "Sit on a preacher curl bench with your feet flat on the floor."
                                                               ('0433', 'dumbbell straight arm pullover', 'bench'),  -- id 784
                                                               -- "Lie flat on a bench with your head at one end and your feet planted firmly on the ground."
                                                               ('0428', 'dumbbell standing preacher curl', 'bench'),  -- id 792
                                                               -- "Rest the back of your upper arms against the preacher bench or an incline bench, with your elbows slightly bent."
                                                               ('2293', 'dumbbell standing zottman preacher curl', 'bench'),  -- id 795
                                                               -- "Place your upper arms against the preacher bench and keep your elbows slightly bent."
                                                               ('2294', 'dumbbell zottman preacher curl', 'bench'),  -- id 802
                                                               -- "Sit on a preacher bench with a dumbbell in each hand, palms facing up and elbows resting on the pad."
                                                               ('3234', 'hyght dumbbell fly', 'bench'),  -- id 887
                                                               -- "Lie flat on a bench with a dumbbell in each hand, palms facing each other."
                                                               ('3011', 'incline scapula push up', 'bench'),  -- id 899  [UPPER_PUSH]
                                                               -- "Set up an incline bench at a 45-degree angle."
                                                               ('0491', 'incline leg hip raise (leg straight)', 'bench'),  -- id 904
                                                               -- "Lie on an incline bench with your back flat against the bench and your legs extended straight out in front of you."
                                                               ('0495', 'incline twisting sit-up', 'bench'),  -- id 905
                                                               -- "Set up an incline bench at a 45-degree angle."
                                                               ('0489', 'hyperextension', 'bench');  -- id 908
-- "Adjust the hyperextension bench so that your upper thighs are resting on the pad and your feet are secured."

-- ---------------------------------------------------------------------
-- Guarda 1: los 80 pares (source_code, name_en) tienen que resolver a un
-- ejercicio existente. Si no, aborta.
-- ---------------------------------------------------------------------
DO $$
DECLARE
faltan TEXT;
BEGIN
SELECT string_agg(t.source_code || ' (' || t.name_en || ')', ', ') INTO faltan
FROM v29_req t
         LEFT JOIN exercises e ON e.source_code = t.source_code AND e.name_en = t.name_en
WHERE e.exercise_id IS NULL;
IF faltan IS NOT NULL THEN
        RAISE EXCEPTION 'V29: no coinciden %', faltan;
END IF;
END $$;

-- ---------------------------------------------------------------------
-- Guarda 2: ninguno de los 80 puede tener ya un grupo que admita banco.
-- Si lo tiene, la correccion ya esta hecha y esta migracion sobra.
-- ---------------------------------------------------------------------
DO $$
DECLARE
yatienen TEXT;
BEGIN
SELECT string_agg(DISTINCT t.source_code, ', ') INTO yatienen
FROM v29_req t
         JOIN exercises e ON e.source_code = t.source_code AND e.name_en = t.name_en
         JOIN exercise_equipment_requirements r ON r.exercise_id = e.exercise_id
         JOIN equipment_types eq ON eq.equipment_id = r.equipment_id
WHERE eq.code = t.equipment_code;
IF yatienen IS NOT NULL THEN
        RAISE EXCEPTION 'V29: ya exigen banco (%). Nada que hacer.', yatienen;
END IF;
END $$;

-- ---------------------------------------------------------------------
-- Guarda 3: cuenta y huella del estado previo de la tabla, para probar
-- despues que no se ha tocado ni una fila existente.
-- ---------------------------------------------------------------------
CREATE TEMP TABLE v29_antes AS
SELECT exercise_id, group_no, equipment_id
FROM exercise_equipment_requirements;

-- ---------------------------------------------------------------------
-- INC-V29-01 (17/10/2026). CORRECCION ANADIDA A LA VERSION APROBADA.
--
-- Seis de los 80 no tienen HOY ninguna fila de requisitos y su implemento
-- principal es 'body weight'. El trigger diferido tg_exercises_principal
-- exime de requisitos SOLO a quien no tiene NINGUNA fila; en cuanto el
-- INSERT de abajo les crea el grupo del banco pasan a tener filas sin su
-- principal dentro, y el COMMIT falla:
--
--   ERROR: Ejercicio 504: su implemento principal (body weight) no esta
--          entre sus requisitos
--
-- Correccion minima: a esos seis se les anade PRIMERO un grupo con su
-- propio implemento principal. No se toca el INSERT del banco, que sigue
-- siendo literalmente el de la version aprobada, ni se relaja el trigger.
-- Al escribirse antes, el principal queda como grupo 1 y el banco como
-- grupo 2; los grupos se combinan con AND, asi que el ejercicio exige su
-- principal Y el banco.
--
-- Va DESPUES de la guarda 3 a proposito: estas filas deben contarse como
-- nuevas en la comprobacion 1.
-- ---------------------------------------------------------------------
CREATE TEMP TABLE v29_sin_requisitos AS
SELECT e.exercise_id, e.equipment_id
FROM v29_req t
         JOIN exercises e ON e.source_code = t.source_code AND e.name_en = t.name_en
WHERE NOT EXISTS (SELECT 1 FROM exercise_equipment_requirements r
                  WHERE r.exercise_id = e.exercise_id);

INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT exercise_id, 1, equipment_id FROM v29_sin_requisitos;

-- ---------------------------------------------------------------------
-- El cambio. Un INSERT y nada mas. El grupo nuevo es MAX(group_no) + 1
-- del propio ejercicio: grupos distintos se combinan con AND, asi que el
-- requisito viejo se conserva y el banco se suma.
-- ---------------------------------------------------------------------
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT e.exercise_id,
       COALESCE((SELECT MAX(r.group_no)
                 FROM exercise_equipment_requirements r
                 WHERE r.exercise_id = e.exercise_id), 0) + 1,
       eq.equipment_id
FROM v29_req t
         JOIN exercises e ON e.source_code = t.source_code AND e.name_en = t.name_en
         JOIN equipment_types eq ON eq.code = t.equipment_code;

-- ---------------------------------------------------------------------
-- Comprobacion 1: se han escrito exactamente 80 filas nuevas.
-- ---------------------------------------------------------------------
DO $$
DECLARE
nuevas INTEGER;
    principales INTEGER;
BEGIN
SELECT COUNT(*) INTO principales FROM v29_sin_requisitos;
IF principales <> 6 THEN
        RAISE EXCEPTION 'V29: se esperaban 6 ejercicios sin requisitos previos y hay % (INC-V29-01)', principales;
END IF;
SELECT COUNT(*) INTO nuevas FROM (
                                     SELECT exercise_id, group_no, equipment_id FROM exercise_equipment_requirements
                                     EXCEPT ALL
                                     SELECT exercise_id, group_no, equipment_id FROM v29_antes) d;
IF nuevas <> 86 THEN
        RAISE EXCEPTION 'V29: se esperaban 86 filas nuevas (80 de banco + 6 de implemento principal) y hay %', nuevas;
END IF;
END $$;

-- ---------------------------------------------------------------------
-- Comprobacion 2: ninguna fila previa ha desaparecido ni ha cambiado.
-- ---------------------------------------------------------------------
DO $$
DECLARE
perdidas INTEGER;
BEGIN
SELECT COUNT(*) INTO perdidas FROM (
                                       SELECT exercise_id, group_no, equipment_id FROM v29_antes
                                       EXCEPT ALL
                                       SELECT exercise_id, group_no, equipment_id FROM exercise_equipment_requirements) d;
IF perdidas <> 0 THEN
        RAISE EXCEPTION 'V29: han desaparecido % filas previas', perdidas;
END IF;
END $$;

-- ---------------------------------------------------------------------
-- Comprobacion 3: cada uno de los 80 exige ahora banco, y sigue
-- exigiendo lo que exigia antes (el grupo viejo intacto).
-- ---------------------------------------------------------------------
DO $$
DECLARE
sinbanco TEXT;
BEGIN
SELECT string_agg(t.source_code, ', ') INTO sinbanco
FROM v29_req t
         JOIN exercises e ON e.source_code = t.source_code AND e.name_en = t.name_en
WHERE NOT EXISTS (
    SELECT 1 FROM exercise_equipment_requirements r
                      JOIN equipment_types eq ON eq.equipment_id = r.equipment_id
    WHERE r.exercise_id = e.exercise_id AND eq.code = t.equipment_code);
IF sinbanco IS NOT NULL THEN
        RAISE EXCEPTION 'V29: siguen sin exigir banco (%)', sinbanco;
END IF;
END $$;

-- ---------------------------------------------------------------------
-- Comprobacion 5 (INC-V29-01): ninguno de los 80 queda sin su implemento
-- principal entre sus requisitos. Es la invariante que impone
-- fn_principal_en_requisitos. Se afirma aqui para que la migracion falle
-- con un mensaje que nombre a todos los afectados, en vez de abortar en el
-- COMMIT con el primero que encuentre el trigger.
-- ---------------------------------------------------------------------
DO $$
DECLARE
rotos TEXT;
BEGIN
SELECT string_agg(e.exercise_id || ' (' || eq.code || ')', ', ' ORDER BY e.exercise_id) INTO rotos
FROM v29_req t
         JOIN exercises e ON e.source_code = t.source_code AND e.name_en = t.name_en
         JOIN equipment_types eq ON eq.equipment_id = e.equipment_id
WHERE NOT EXISTS (SELECT 1 FROM exercise_equipment_requirements r
                  WHERE r.exercise_id = e.exercise_id AND r.equipment_id = e.equipment_id);
IF rotos IS NOT NULL THEN
        RAISE EXCEPTION 'V29: quedan sin su implemento principal entre sus requisitos: %', rotos;
END IF;
END $$;

DROP TABLE v29_req;
DROP TABLE v29_antes;
DROP TABLE v29_sin_requisitos;
