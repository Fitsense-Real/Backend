-- =====================================================================
-- FitSense MVP 1.0 - V24: correcciones de la revision del catalogo
--
-- GENERADA AUTOMATICAMENTE por generar_V24.py el 2026-09-21
-- a partir de revision-cerrada.xlsx.
-- No editar a mano: regenerar desde la hoja y volver a copiar.
--
-- Requiere V22 (codigos de equipamiento) y V23 (tabla de requisitos).
--
-- QUE HACE
--    214 ejercicios con requisitos reescritos (lo que decide quien puede hacerlo)
--     87 cambios de implemento principal (solo lo que se muestra)
--      0 cambios de body_part          (la zona que el nombre indica)
--      8 cambios de default_prescription (tiempo o movilidad prescritos por repeticiones)
--      1 cambios de name_es            (nombres rotos o sin traducir)
--      4 desactivaciones               (variante ambigua respecto a la cantidad de equipamiento, o retirados por vosotros)
--      0 cambios de difficulty_level   (reglas aceptadas por su concordancia con la referencia humana; en la muestra manda la referencia)
--    314 cambios en total, sobre 224 ejercicios distintos
--        (un ejercicio puede cambiar en varias categorias a la vez)
--
-- COMO LO HACE
-- Igual que V21: identifica cada ejercicio por source_code y comprueba
-- ademas name_en. Si algo no coincide, aborta sin tocar nada. Reaplicarla
-- no cambia nada.
--
-- REQUISITOS Y PRINCIPAL SON COSAS DISTINTAS
-- Los requisitos (exercise_equipment_requirements) deciden quien puede
-- hacer el ejercicio: "dumbbell bench press" exige mancuerna Y banco, y
-- quien solo tiene una de las dos no lo recibe. El implemento principal
-- (exercises.equipment_id) es solo lo que se muestra en la app y en el
-- prompt; ya no filtra nada.
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
        RAISE EXCEPTION 'V24: falta la tabla exercise_equipment_requirements. ¿Se aplico V23?';
END IF;
END $$;

-- ---------------------------------------------------------------------
-- Requisito: los codigos de equipamiento que esta migracion usa tienen que
-- existir. Los catorce que anade V22 incluidos.
-- ---------------------------------------------------------------------
DO $$
DECLARE
faltan TEXT;
BEGIN
SELECT string_agg(c, ', ') INTO faltan
FROM unnest(ARRAY['ab wheel', 'assisted', 'balance board', 'band', 'barbell', 'bench', 'bosu ball', 'cable', 'captains chair', 'dumbbell', 'ez barbell', 'gymnastic rings', 'landmine', 'medicine ball', 'other', 'parallel bars', 'plyo box', 'pull-up bar', 'rope', 'smith machine', 'stability ball', 'stationary bike', 'step platform', 'suspension straps', 'treadmill', 'weighted']) AS c
WHERE NOT EXISTS (SELECT 1 FROM equipment_types eq WHERE eq.code = c);

IF faltan IS NOT NULL THEN
        RAISE EXCEPTION 'V24: faltan codigos de equipamiento (%). ¿Se aplico V22?', faltan;
END IF;
END $$;

-- ------------------------------------------------------------------
-- Requisitos de equipamiento: 214 ejercicios
-- Grupos distintos = AND, mismo grupo = OR. Sin filas = peso corporal.
-- ------------------------------------------------------------------
CREATE TEMP TABLE v24_req_ejercicio (
    source_code VARCHAR(20)  PRIMARY KEY,
    name_en     VARCHAR(255) NOT NULL
);

INSERT INTO v24_req_ejercicio (source_code, name_en) VALUES
                                                         ('MVP-0051', 'Hanging knee raise'),   -- id 15: pull-up bar
                                                         ('MVP-0015', 'Pull-up'),   -- id 18: pull-up bar
                                                         ('MVP-0025', 'Bench dip'),   -- id 24: bench
                                                         ('MVP-0005', 'Dumbbell bench press'),   -- id 29: dumbbell + bench
                                                         ('MVP-0039', 'Dumbbell step-up'),   -- id 35: dumbbell + bench | plyo box | step platform
                                                         ('MVP-0007', 'Barbell bench press'),   -- id 45: barbell + bench
                                                         ('2355', 'arm slingers hanging bent knee legs'),   -- id 61: pull-up bar
                                                         ('2333', 'arm slingers hanging straight legs'),   -- id 62: pull-up bar
                                                         ('3293', 'archer pull up'),   -- id 64: pull-up bar
                                                         ('3297', 'back lever'),   -- id 65: pull-up bar
                                                         ('1716', 'assisted seated pectoralis major stretch with stability ball'),   -- id 80: stability ball
                                                         ('0011', 'assisted hanging knee raise'),   -- id 81: assisted + pull-up bar
                                                         ('0010', 'assisted hanging knee raise with throw down'),   -- id 82: assisted + pull-up bar
                                                         ('0020', 'balance board'),   -- id 94: balance board
                                                         ('1254', 'band bench press'),   -- id 102: band + bench
                                                         ('0971', 'band assisted wheel rollerout'),   -- id 105: band + ab wheel
                                                         ('0970', 'band assisted pull-up'),   -- id 113: band + pull-up bar
                                                         ('1008', 'band step-up'),   -- id 146: band + plyo box | step platform
                                                         ('0025', 'barbell bench press'),   -- id 151: barbell + bench
                                                         ('0033', 'barbell decline bench press'),   -- id 152: barbell + bench
                                                         ('0030', 'barbell close-grip bench press'),   -- id 169: barbell + bench
                                                         ('1411', 'barbell palms down wrist curl over a bench'),   -- id 173: barbell + bench
                                                         ('1412', 'barbell palms up wrist curl over a bench'),   -- id 174: barbell + bench
                                                         ('0045', 'barbell guillotine bench press'),   -- id 176: barbell + bench
                                                         ('0047', 'barbell incline bench press'),   -- id 177: barbell + bench
                                                         ('3562', 'barbell glute bridge two legs on bench (male)'),   -- id 187: barbell + bench
                                                         ('1719', 'barbell incline close grip bench press'),   -- id 201: barbell + bench
                                                         ('0052', 'barbell jm bench press'),   -- id 203: barbell + bench
                                                         ('1256', 'barbell reverse grip decline bench press'),   -- id 222: barbell + bench
                                                         ('1257', 'barbell reverse grip incline bench press'),   -- id 223: barbell + bench
                                                         ('0083', 'barbell rollerout from bench'),   -- id 226: barbell + bench
                                                         ('1317', 'barbell reverse grip incline bench row'),   -- id 233: barbell + bench
                                                         ('2187', 'barbell reverse close-grip bench press'),   -- id 246: barbell + bench
                                                         ('0122', 'barbell wide bench press'),   -- id 272: barbell + bench
                                                         ('1258', 'barbell wide reverse grip bench press'),   -- id 273: barbell + bench
                                                         ('0114', 'barbell step-up'),   -- id 281: barbell + bench | plyo box | step platform
                                                         ('1374', 'box jump down with one leg stabilization'),   -- id 295: plyo box
                                                         ('3019', 'bench pull-ups'),   -- id 300: bench | pull-up bar
                                                         ('0130', 'bench hip extension'),   -- id 309: bench
                                                         ('0129', 'bench dip (knees bent)'),   -- id 313: bench
                                                         ('0139', 'biceps narrow pull-ups'),   -- id 316: pull-up bar
                                                         ('0140', 'biceps pull-up'),   -- id 317: pull-up bar
                                                         ('0169', 'cable incline bench press'),   -- id 341: cable + bench
                                                         ('0170', 'cable incline fly (on stability ball)'),   -- id 343: cable + stability ball
                                                         ('1318', 'cable incline bench row'),   -- id 352: cable + bench
                                                         ('1263', 'cable one arm fly on exercise ball'),   -- id 378: cable + stability ball
                                                         ('1264', 'cable one arm incline fly on exercise ball'),   -- id 379: cable + stability ball
                                                         ('1266', 'cable one arm incline press on exercise ball'),   -- id 381: cable + stability ball
                                                         ('1267', 'cable one arm press on exercise ball'),   -- id 383: cable + stability ball
                                                         ('1268', 'cable press on exercise ball'),   -- id 384: cable + stability ball
                                                         ('1637', 'cable overhead curl on exercise ball'),   -- id 401: cable + stability ball
                                                         ('0211', 'cable russian twists (on stability ball)'),   -- id 423: cable + stability ball
                                                         ('1322', 'cable rope extension incline bench row'),   -- id 432: cable + bench
                                                         ('0251', 'chest dip'),   -- id 457: parallel bars
                                                         ('1430', 'chest dip (on dip-pull-up cage)'),   -- id 458: parallel bars
                                                         ('2462', 'chest dip on straight bar'),   -- id 459: parallel bars
                                                         ('2963', 'captains chair straight leg raise'),   -- id 462: captains chair
                                                         ('1326', 'chin-up'),   -- id 463: pull-up bar
                                                         ('0253', 'chin-ups (narrow parallel grip)'),   -- id 464: pull-up bar
                                                         ('1645', 'cable two arm curl on incline bench'),   -- id 491: cable + bench
                                                         ('1327', 'close grip chin-up'),   -- id 506: pull-up bar
                                                         ('0290', 'dumbbell bench seated press'),   -- id 514: dumbbell + bench
                                                         ('0289', 'dumbbell bench press'),   -- id 517: dumbbell + bench
                                                         ('0291', 'dumbbell bench squat'),   -- id 519: dumbbell + bench
                                                         ('1649', 'dumbbell alternating bicep curl with leg raised on exercise ball'),   -- id 525: dumbbell + stability ball
                                                         ('1650', 'dumbbell alternating seated bicep curl on exercise ball'),   -- id 526: dumbbell + stability ball
                                                         ('1652', 'dumbbell bicep curl on exercise ball with leg raised'),   -- id 528: dumbbell + stability ball
                                                         ('0301', 'dumbbell decline bench press'),   -- id 540: dumbbell + bench
                                                         ('1277', 'dumbbell fly on exercise ball'),   -- id 546: dumbbell + stability ball
                                                         ('0314', 'dumbbell incline bench press'),   -- id 548: dumbbell + bench
                                                         ('1656', 'dumbbell biceps curl v sit on bosu ball'),   -- id 559: dumbbell + bosu ball
                                                         ('1659', 'dumbbell hammer curl on exercise ball'),   -- id 569: dumbbell + stability ball
                                                         ('1278', 'dumbbell incline fly on exercise ball'),   -- id 582: dumbbell + stability ball
                                                         ('1280', 'dumbbell incline one arm fly on exercise ball'),   -- id 585: dumbbell + stability ball
                                                         ('1282', 'dumbbell incline one arm press on exercise ball'),   -- id 587: dumbbell + stability ball
                                                         ('1283', 'dumbbell incline press on exercise ball'),   -- id 589: dumbbell + stability ball
                                                         ('1618', 'dumbbell incline hammer press on exercise ball'),   -- id 600: dumbbell + stability ball
                                                         ('1620', 'dumbbell incline one arm hammer press on exercise ball'),   -- id 603: dumbbell + stability ball
                                                         ('1734', 'dumbbell kickbacks on exercise ball'),   -- id 607: dumbbell + stability ball
                                                         ('1660', 'dumbbell kneeling bicep curl exercise ball'),   -- id 608: dumbbell + stability ball
                                                         ('1284', 'dumbbell lying pullover on exercise ball'),   -- id 627: dumbbell + stability ball
                                                         ('1285', 'dumbbell one arm bench fly'),   -- id 628: dumbbell + bench
                                                         ('1286', 'dumbbell one arm chest fly on exercise ball'),   -- id 629: dumbbell + stability ball
                                                         ('1288', 'dumbbell one arm fly on exercise ball'),   -- id 631: dumbbell + stability ball
                                                         ('1290', 'dumbbell one arm press on exercise ball'),   -- id 633: dumbbell + stability ball
                                                         ('1291', 'dumbbell one arm pullover on exercise ball'),   -- id 634: dumbbell + stability ball
                                                         ('0352', 'dumbbell neutral grip bench press'),   -- id 645: dumbbell + bench
                                                         ('0353', 'dumbbell one arm concentration curl (on stability ball)'),   -- id 646: dumbbell + stability ball
                                                         ('1736', 'dumbbell one arm french press on exercise ball'),   -- id 647: dumbbell + stability ball
                                                         ('1621', 'dumbbell one arm hammer press on exercise ball'),   -- id 649: dumbbell + stability ball
                                                         ('1441', 'dumbbell over bench one arm reverse wrist curl'),   -- id 656: dumbbell + bench
                                                         ('0367', 'dumbbell over bench one arm wrist curl'),   -- id 657: dumbbell + bench
                                                         ('0368', 'dumbbell over bench revers wrist curl'),   -- id 658: dumbbell + bench
                                                         ('0369', 'dumbbell over bench wrist curl'),   -- id 659: dumbbell + bench
                                                         ('1292', 'dumbbell one leg fly on exercise ball'),   -- id 668: dumbbell + stability ball
                                                         ('1293', 'dumbbell press on exercise ball'),   -- id 669: dumbbell + stability ball
                                                         ('1294', 'dumbbell pullover hip extension on exercise ball'),   -- id 671: dumbbell + stability ball
                                                         ('1295', 'dumbbell pullover on exercise ball'),   -- id 672: dumbbell + stability ball
                                                         ('1668', 'dumbbell one arm seated bicep curl on exercise ball'),   -- id 678: dumbbell + stability ball
                                                         ('0362', 'dumbbell one arm triceps extension (on bench)'),   -- id 682: dumbbell + bench
                                                         ('0365', 'dumbbell over bench neutral wrist curl'),   -- id 684: dumbbell + bench
                                                         ('0366', 'dumbbell over bench one arm neutral wrist curl'),   -- id 685: dumbbell + bench
                                                         ('1623', 'dumbbell palms in incline bench press'),   -- id 686: dumbbell + bench
                                                         ('1673', 'dumbbell preacher curl over exercise ball'),   -- id 689: dumbbell + stability ball
                                                         ('1624', 'dumbbell reverse bench press'),   -- id 710: dumbbell + bench
                                                         ('1330', 'dumbbell reverse grip incline bench one arm row'),   -- id 711: dumbbell + bench
                                                         ('1331', 'dumbbell reverse grip incline bench two arm row'),   -- id 712: dumbbell + bench
                                                         ('1676', 'dumbbell seated alternate hammer curl on exercise ball'),   -- id 719: dumbbell + stability ball
                                                         ('0389', 'dumbbell seated bench extension'),   -- id 720: dumbbell + bench
                                                         ('0390', 'dumbbell seated biceps curl (on stability ball)'),   -- id 724: dumbbell + stability ball
                                                         ('1679', 'dumbbell seated one arm bicep curl on exercise ball with leg raised'),   -- id 731: dumbbell + stability ball
                                                         ('2805', 'dumbbell single leg deadlift with stepbox support'),   -- id 752: dumbbell + bench | plyo box | step platform
                                                         ('0422', 'dumbbell standing one arm curl (over incline bench)'),   -- id 769: dumbbell + bench
                                                         ('1680', 'dumbbell standing one arm curl over incline bench'),   -- id 770: dumbbell + bench
                                                         ('0431', 'dumbbell step-up'),   -- id 785: dumbbell + bench | plyo box | step platform
                                                         ('2796', 'dumbbell step-up lunge'),   -- id 786: dumbbell + plyo box | step platform
                                                         ('2812', 'dumbbell step-up split squat'),   -- id 787: dumbbell + bench | plyo box | step platform
                                                         ('1684', 'dumbbell step up single leg balance with bicep curl'),   -- id 796: dumbbell + plyo box | step platform
                                                         ('1743', 'dumbbell twisting bench press'),   -- id 799: dumbbell + bench
                                                         ('1382', 'exercise ball on the wall calf raise'),   -- id 816: stability ball
                                                         ('3241', 'exercise ball on the wall calf raise (tennis ball between ankles)'),   -- id 817: stability ball
                                                         ('3240', 'exercise ball on the wall calf raise (tennis ball between knees)'),   -- id 818: stability ball
                                                         ('1746', 'exercise ball supine triceps extension'),   -- id 820: stability ball
                                                         ('1747', 'ez bar french press on exercise ball'),   -- id 833: ez barbell + stability ball
                                                         ('0450', 'ez barbell jm bench press'),   -- id 843: ez barbell + bench
                                                         ('2432', 'ez-bar close-grip bench press'),   -- id 851: ez barbell + bench
                                                         ('3296', 'front lever'),   -- id 858: pull-up bar
                                                         ('0467', 'gorilla chin'),   -- id 862: pull-up bar
                                                         ('1764', 'hanging leg hip raise'),   -- id 865: pull-up bar
                                                         ('0472', 'hanging leg raise'),   -- id 866: pull-up bar
                                                         ('1761', 'hanging oblique knee raise'),   -- id 867: pull-up bar
                                                         ('0473', 'hanging pike'),   -- id 868: pull-up bar
                                                         ('0474', 'hanging straight leg hip raise'),   -- id 869: pull-up bar
                                                         ('0475', 'hanging straight leg raise'),   -- id 870: pull-up bar
                                                         ('0476', 'hanging straight twisting leg hip raise'),   -- id 871: pull-up bar
                                                         ('3295', 'front lever reps'),   -- id 873: pull-up bar
                                                         ('0466', 'gironda sternum chin'),   -- id 874: pull-up bar
                                                         ('3523', 'glute bridge two legs on bench (male)'),   -- id 879: bench
                                                         ('3785', 'incline push-up (on box)'),   -- id 897: plyo box | step platform
                                                         ('0488', 'hyperextension (on bench)'),   -- id 909: bench
                                                         ('0499', 'inverted row'),   -- id 910: pull-up bar | smith machine | suspension straps
                                                         ('2300', 'inverted row bent knees'),   -- id 911: pull-up bar | smith machine
                                                         ('2298', 'inverted row on bench'),   -- id 912: bench
                                                         ('0497', 'inverted row v. 2'),   -- id 913: smith machine | suspension straps
                                                         ('0498', 'inverted row with straps'),   -- id 914: suspension straps
                                                         ('0496', 'inverse leg curl (bench support)'),   -- id 915: bench
                                                         ('2400', 'inverse leg curl (on pull-up cable machine)'),   -- id 916: cable
                                                         ('3289', 'impossible dips'),   -- id 920: parallel bars
                                                         ('3288', 'korean dips'),   -- id 934: parallel bars
                                                         ('0558', 'kipping muscle up'),   -- id 937: pull-up bar
                                                         ('0570', 'leg pull in flat bench'),   -- id 976: bench
                                                         ('3418', 'l-pull-up'),   -- id 977: pull-up bar
                                                         ('3237', 'landmine lateral raise'),   -- id 979: barbell + landmine
                                                         ('0562', 'landmine 180'),   -- id 980: barbell + landmine
                                                         ('0620', 'lying leg raise flat bench'),   -- id 1061: bench
                                                         ('0627', 'mixed grip chin-up'),   -- id 1067: pull-up bar
                                                         ('0631', 'muscle up'),   -- id 1068: pull-up bar
                                                         ('1401', 'muscle-up (on vertical bar)'),   -- id 1069: pull-up bar
                                                         ('0638', 'one arm chin-up'),   -- id 1071: pull-up bar
                                                         ('0651', 'pull up (neutral grip)'),   -- id 1108: pull-up bar
                                                         ('0652', 'pull-up'),   -- id 1109: pull-up bar
                                                         ('1307', 'push up on bosu ball'),   -- id 1131: other + bosu ball
                                                         ('0653', 'push-up (bosu ball)'),   -- id 1132: other + bosu ball
                                                         ('0670', 'rear pull-up'),   -- id 1143: pull-up bar
                                                         ('0674', 'reverse grip pull-up'),   -- id 1144: pull-up bar
                                                         ('0678', 'rocky pull-up pulldown'),   -- id 1145: pull-up bar
                                                         ('0688', 'scapular pull-up'),   -- id 1147: pull-up bar
                                                         ('1423', 'reverse hyper on flat bench'),   -- id 1148: bench
                                                         ('0672', 'reverse dip'),   -- id 1152: parallel bars
                                                         ('0677', 'ring dips'),   -- id 1153: gymnastic rings
                                                         ('0709', 'side hip (on parallel bars)'),   -- id 1184: parallel bars
                                                         ('1763', 'shoulder grip pull-up'),   -- id 1189: pull-up bar
                                                         ('0720', 'side-to-side chin'),   -- id 1191: pull-up bar
                                                         ('0748', 'smith bench press'),   -- id 1218: smith machine + bench
                                                         ('0753', 'smith decline bench press'),   -- id 1219: smith machine + bench
                                                         ('0757', 'smith incline bench press'),   -- id 1221: smith machine + bench
                                                         ('1626', 'smith machine reverse decline close grip bench press'),   -- id 1224: smith machine + bench
                                                         ('0751', 'smith close-grip bench press'),   -- id 1239: smith machine + bench
                                                         ('1625', 'smith machine decline close grip bench press'),   -- id 1241: smith machine + bench
                                                         ('1599', 'standing hamstring and calf stretch with strap'),   -- id 1270: rope + suspension straps
                                                         ('1308', 'smith wide grip bench press'),   -- id 1285: smith machine + bench
                                                         ('1309', 'smith wide grip decline bench press'),   -- id 1286: smith machine + bench
                                                         ('1705', 'squat on bosu ball'),   -- id 1292: other + bosu ball
                                                         ('0806', 'suspended push-up'),   -- id 1297: suspension straps
                                                         ('0805', 'suspended abdominal fallout'),   -- id 1300: suspension straps
                                                         ('0807', 'suspended reverse crunch'),   -- id 1301: pull-up bar
                                                         ('0826', 'vertical leg raise (on parallel bars)'),   -- id 1306: parallel bars
                                                         ('0808', 'suspended row'),   -- id 1307: suspension straps
                                                         ('0809', 'suspended split squat'),   -- id 1312: suspension straps
                                                         ('1753', 'three bench dip'),   -- id 1316: bench
                                                         ('0812', 'triceps dip (bench leg)'),   -- id 1318: bench
                                                         ('0813', 'triceps dip (between benches)'),   -- id 1319: bench
                                                         ('0798', 'stationary bike walk'),   -- id 1324: stationary bike
                                                         ('3666', 'walking on incline treadmill'),   -- id 1325: treadmill
                                                         ('0830', 'weighted bench dip'),   -- id 1326: weighted + bench
                                                         ('2138', 'stationary bike run v. 3'),   -- id 1327: stationary bike
                                                         ('0796', 'standing wheel rollerout'),   -- id 1330: other + ab wheel
                                                         ('3637', 'wheel run'),   -- id 1333: ab wheel
                                                         ('2363', 'wide-grip chest dip on high parallel bars'),   -- id 1335: parallel bars
                                                         ('1429', 'wide grip pull-up'),   -- id 1336: pull-up bar
                                                         ('1367', 'wide grip rear pull-up'),   -- id 1337: pull-up bar
                                                         ('0847', 'weighted seated bicep curl (on stability ball)'),   -- id 1338: medicine ball + stability ball
                                                         ('0866', 'weighted hanging leg-hip raise'),   -- id 1350: weighted + pull-up bar
                                                         ('0840', 'weighted overhead crunch (on stability ball)'),   -- id 1351: weighted + stability ball
                                                         ('0849', 'weighted seated twist (on stability ball)'),   -- id 1355: weighted + stability ball
                                                         ('0850', 'weighted side bend (on stability ball)'),   -- id 1356: weighted + stability ball
                                                         ('0835', 'weighted hyperextension (on stability ball)'),   -- id 1358: weighted + stability ball
                                                         ('3286', 'weighted muscle up'),   -- id 1359: weighted + pull-up bar
                                                         ('3312', 'weighted muscle up (on bar)'),   -- id 1360: weighted + pull-up bar
                                                         ('3290', 'weighted one hand pull up'),   -- id 1361: weighted + pull-up bar
                                                         ('0841', 'weighted pull-up'),   -- id 1362: weighted + pull-up bar
                                                         ('1754', 'weighted three bench dips'),   -- id 1369: weighted + bench
                                                         ('1767', 'weighted triceps dip on high parallel bars'),   -- id 1371: weighted + parallel bars
                                                         ('0857', 'wheel rollerout');   -- id 1372: other + ab wheel

CREATE TEMP TABLE v24_req (
    source_code    VARCHAR(20) NOT NULL,
    group_no       SMALLINT    NOT NULL,
    equipment_code VARCHAR(40) NOT NULL
);

INSERT INTO v24_req (source_code, group_no, equipment_code) VALUES
                                                                ('MVP-0051', 1, 'pull-up bar'),
                                                                ('MVP-0015', 1, 'pull-up bar'),
                                                                ('MVP-0025', 1, 'bench'),
                                                                ('MVP-0005', 1, 'dumbbell'),
                                                                ('MVP-0005', 2, 'bench'),
                                                                ('MVP-0039', 1, 'dumbbell'),
                                                                ('MVP-0039', 2, 'bench'),
                                                                ('MVP-0039', 2, 'plyo box'),
                                                                ('MVP-0039', 2, 'step platform'),
                                                                ('MVP-0007', 1, 'barbell'),
                                                                ('MVP-0007', 2, 'bench'),
                                                                ('2355', 1, 'pull-up bar'),
                                                                ('2333', 1, 'pull-up bar'),
                                                                ('3293', 1, 'pull-up bar'),
                                                                ('3297', 1, 'pull-up bar'),
                                                                ('1716', 1, 'stability ball'),
                                                                ('0011', 1, 'assisted'),
                                                                ('0011', 2, 'pull-up bar'),
                                                                ('0010', 1, 'assisted'),
                                                                ('0010', 2, 'pull-up bar'),
                                                                ('0020', 1, 'balance board'),
                                                                ('1254', 1, 'band'),
                                                                ('1254', 2, 'bench'),
                                                                ('0971', 1, 'band'),
                                                                ('0971', 2, 'ab wheel'),
                                                                ('0970', 1, 'band'),
                                                                ('0970', 2, 'pull-up bar'),
                                                                ('1008', 1, 'band'),
                                                                ('1008', 2, 'plyo box'),
                                                                ('1008', 2, 'step platform'),
                                                                ('0025', 1, 'barbell'),
                                                                ('0025', 2, 'bench'),
                                                                ('0033', 1, 'barbell'),
                                                                ('0033', 2, 'bench'),
                                                                ('0030', 1, 'barbell'),
                                                                ('0030', 2, 'bench'),
                                                                ('1411', 1, 'barbell'),
                                                                ('1411', 2, 'bench'),
                                                                ('1412', 1, 'barbell'),
                                                                ('1412', 2, 'bench'),
                                                                ('0045', 1, 'barbell'),
                                                                ('0045', 2, 'bench'),
                                                                ('0047', 1, 'barbell'),
                                                                ('0047', 2, 'bench'),
                                                                ('3562', 1, 'barbell'),
                                                                ('3562', 2, 'bench'),
                                                                ('1719', 1, 'barbell'),
                                                                ('1719', 2, 'bench'),
                                                                ('0052', 1, 'barbell'),
                                                                ('0052', 2, 'bench'),
                                                                ('1256', 1, 'barbell'),
                                                                ('1256', 2, 'bench'),
                                                                ('1257', 1, 'barbell'),
                                                                ('1257', 2, 'bench'),
                                                                ('0083', 1, 'barbell'),
                                                                ('0083', 2, 'bench'),
                                                                ('1317', 1, 'barbell'),
                                                                ('1317', 2, 'bench'),
                                                                ('2187', 1, 'barbell'),
                                                                ('2187', 2, 'bench'),
                                                                ('0122', 1, 'barbell'),
                                                                ('0122', 2, 'bench'),
                                                                ('1258', 1, 'barbell'),
                                                                ('1258', 2, 'bench'),
                                                                ('0114', 1, 'barbell'),
                                                                ('0114', 2, 'bench'),
                                                                ('0114', 2, 'plyo box'),
                                                                ('0114', 2, 'step platform'),
                                                                ('1374', 1, 'plyo box'),
                                                                ('3019', 1, 'bench'),
                                                                ('3019', 1, 'pull-up bar'),
                                                                ('0130', 1, 'bench'),
                                                                ('0129', 1, 'bench'),
                                                                ('0139', 1, 'pull-up bar'),
                                                                ('0140', 1, 'pull-up bar'),
                                                                ('0169', 1, 'cable'),
                                                                ('0169', 2, 'bench'),
                                                                ('0170', 1, 'cable'),
                                                                ('0170', 2, 'stability ball'),
                                                                ('1318', 1, 'cable'),
                                                                ('1318', 2, 'bench'),
                                                                ('1263', 1, 'cable'),
                                                                ('1263', 2, 'stability ball'),
                                                                ('1264', 1, 'cable'),
                                                                ('1264', 2, 'stability ball'),
                                                                ('1266', 1, 'cable'),
                                                                ('1266', 2, 'stability ball'),
                                                                ('1267', 1, 'cable'),
                                                                ('1267', 2, 'stability ball'),
                                                                ('1268', 1, 'cable'),
                                                                ('1268', 2, 'stability ball'),
                                                                ('1637', 1, 'cable'),
                                                                ('1637', 2, 'stability ball'),
                                                                ('0211', 1, 'cable'),
                                                                ('0211', 2, 'stability ball'),
                                                                ('1322', 1, 'cable'),
                                                                ('1322', 2, 'bench'),
                                                                ('0251', 1, 'parallel bars'),
                                                                ('1430', 1, 'parallel bars'),
                                                                ('2462', 1, 'parallel bars'),
                                                                ('2963', 1, 'captains chair'),
                                                                ('1326', 1, 'pull-up bar'),
                                                                ('0253', 1, 'pull-up bar'),
                                                                ('1645', 1, 'cable'),
                                                                ('1645', 2, 'bench'),
                                                                ('1327', 1, 'pull-up bar'),
                                                                ('0290', 1, 'dumbbell'),
                                                                ('0290', 2, 'bench'),
                                                                ('0289', 1, 'dumbbell'),
                                                                ('0289', 2, 'bench'),
                                                                ('0291', 1, 'dumbbell'),
                                                                ('0291', 2, 'bench'),
                                                                ('1649', 1, 'dumbbell'),
                                                                ('1649', 2, 'stability ball'),
                                                                ('1650', 1, 'dumbbell'),
                                                                ('1650', 2, 'stability ball'),
                                                                ('1652', 1, 'dumbbell'),
                                                                ('1652', 2, 'stability ball'),
                                                                ('0301', 1, 'dumbbell'),
                                                                ('0301', 2, 'bench'),
                                                                ('1277', 1, 'dumbbell'),
                                                                ('1277', 2, 'stability ball'),
                                                                ('0314', 1, 'dumbbell'),
                                                                ('0314', 2, 'bench'),
                                                                ('1656', 1, 'dumbbell'),
                                                                ('1656', 2, 'bosu ball'),
                                                                ('1659', 1, 'dumbbell'),
                                                                ('1659', 2, 'stability ball'),
                                                                ('1278', 1, 'dumbbell'),
                                                                ('1278', 2, 'stability ball'),
                                                                ('1280', 1, 'dumbbell'),
                                                                ('1280', 2, 'stability ball'),
                                                                ('1282', 1, 'dumbbell'),
                                                                ('1282', 2, 'stability ball'),
                                                                ('1283', 1, 'dumbbell'),
                                                                ('1283', 2, 'stability ball'),
                                                                ('1618', 1, 'dumbbell'),
                                                                ('1618', 2, 'stability ball'),
                                                                ('1620', 1, 'dumbbell'),
                                                                ('1620', 2, 'stability ball'),
                                                                ('1734', 1, 'dumbbell'),
                                                                ('1734', 2, 'stability ball'),
                                                                ('1660', 1, 'dumbbell'),
                                                                ('1660', 2, 'stability ball'),
                                                                ('1284', 1, 'dumbbell'),
                                                                ('1284', 2, 'stability ball'),
                                                                ('1285', 1, 'dumbbell'),
                                                                ('1285', 2, 'bench'),
                                                                ('1286', 1, 'dumbbell'),
                                                                ('1286', 2, 'stability ball'),
                                                                ('1288', 1, 'dumbbell'),
                                                                ('1288', 2, 'stability ball'),
                                                                ('1290', 1, 'dumbbell'),
                                                                ('1290', 2, 'stability ball'),
                                                                ('1291', 1, 'dumbbell'),
                                                                ('1291', 2, 'stability ball'),
                                                                ('0352', 1, 'dumbbell'),
                                                                ('0352', 2, 'bench'),
                                                                ('0353', 1, 'dumbbell'),
                                                                ('0353', 2, 'stability ball'),
                                                                ('1736', 1, 'dumbbell'),
                                                                ('1736', 2, 'stability ball'),
                                                                ('1621', 1, 'dumbbell'),
                                                                ('1621', 2, 'stability ball'),
                                                                ('1441', 1, 'dumbbell'),
                                                                ('1441', 2, 'bench'),
                                                                ('0367', 1, 'dumbbell'),
                                                                ('0367', 2, 'bench'),
                                                                ('0368', 1, 'dumbbell'),
                                                                ('0368', 2, 'bench'),
                                                                ('0369', 1, 'dumbbell'),
                                                                ('0369', 2, 'bench'),
                                                                ('1292', 1, 'dumbbell'),
                                                                ('1292', 2, 'stability ball'),
                                                                ('1293', 1, 'dumbbell'),
                                                                ('1293', 2, 'stability ball'),
                                                                ('1294', 1, 'dumbbell'),
                                                                ('1294', 2, 'stability ball'),
                                                                ('1295', 1, 'dumbbell'),
                                                                ('1295', 2, 'stability ball'),
                                                                ('1668', 1, 'dumbbell'),
                                                                ('1668', 2, 'stability ball'),
                                                                ('0362', 1, 'dumbbell'),
                                                                ('0362', 2, 'bench'),
                                                                ('0365', 1, 'dumbbell'),
                                                                ('0365', 2, 'bench'),
                                                                ('0366', 1, 'dumbbell'),
                                                                ('0366', 2, 'bench'),
                                                                ('1623', 1, 'dumbbell'),
                                                                ('1623', 2, 'bench'),
                                                                ('1673', 1, 'dumbbell'),
                                                                ('1673', 2, 'stability ball'),
                                                                ('1624', 1, 'dumbbell'),
                                                                ('1624', 2, 'bench'),
                                                                ('1330', 1, 'dumbbell'),
                                                                ('1330', 2, 'bench'),
                                                                ('1331', 1, 'dumbbell'),
                                                                ('1331', 2, 'bench'),
                                                                ('1676', 1, 'dumbbell'),
                                                                ('1676', 2, 'stability ball'),
                                                                ('0389', 1, 'dumbbell'),
                                                                ('0389', 2, 'bench'),
                                                                ('0390', 1, 'dumbbell'),
                                                                ('0390', 2, 'stability ball'),
                                                                ('1679', 1, 'dumbbell'),
                                                                ('1679', 2, 'stability ball'),
                                                                ('2805', 1, 'dumbbell'),
                                                                ('2805', 2, 'bench'),
                                                                ('2805', 2, 'plyo box'),
                                                                ('2805', 2, 'step platform'),
                                                                ('0422', 1, 'dumbbell'),
                                                                ('0422', 2, 'bench'),
                                                                ('1680', 1, 'dumbbell'),
                                                                ('1680', 2, 'bench'),
                                                                ('0431', 1, 'dumbbell'),
                                                                ('0431', 2, 'bench'),
                                                                ('0431', 2, 'plyo box'),
                                                                ('0431', 2, 'step platform'),
                                                                ('2796', 1, 'dumbbell'),
                                                                ('2796', 2, 'plyo box'),
                                                                ('2796', 2, 'step platform'),
                                                                ('2812', 1, 'dumbbell'),
                                                                ('2812', 2, 'bench'),
                                                                ('2812', 2, 'plyo box'),
                                                                ('2812', 2, 'step platform'),
                                                                ('1684', 1, 'dumbbell'),
                                                                ('1684', 2, 'plyo box'),
                                                                ('1684', 2, 'step platform'),
                                                                ('1743', 1, 'dumbbell'),
                                                                ('1743', 2, 'bench'),
                                                                ('1382', 1, 'stability ball'),
                                                                ('3241', 1, 'stability ball'),
                                                                ('3240', 1, 'stability ball'),
                                                                ('1746', 1, 'stability ball'),
                                                                ('1747', 1, 'ez barbell'),
                                                                ('1747', 2, 'stability ball'),
                                                                ('0450', 1, 'ez barbell'),
                                                                ('0450', 2, 'bench'),
                                                                ('2432', 1, 'ez barbell'),
                                                                ('2432', 2, 'bench'),
                                                                ('3296', 1, 'pull-up bar'),
                                                                ('0467', 1, 'pull-up bar'),
                                                                ('1764', 1, 'pull-up bar'),
                                                                ('0472', 1, 'pull-up bar'),
                                                                ('1761', 1, 'pull-up bar'),
                                                                ('0473', 1, 'pull-up bar'),
                                                                ('0474', 1, 'pull-up bar'),
                                                                ('0475', 1, 'pull-up bar'),
                                                                ('0476', 1, 'pull-up bar'),
                                                                ('3295', 1, 'pull-up bar'),
                                                                ('0466', 1, 'pull-up bar'),
                                                                ('3523', 1, 'bench'),
                                                                ('3785', 1, 'plyo box'),
                                                                ('3785', 1, 'step platform'),
                                                                ('0488', 1, 'bench'),
                                                                ('0499', 1, 'pull-up bar'),
                                                                ('0499', 1, 'smith machine'),
                                                                ('0499', 1, 'suspension straps'),
                                                                ('2300', 1, 'pull-up bar'),
                                                                ('2300', 1, 'smith machine'),
                                                                ('2298', 1, 'bench'),
                                                                ('0497', 1, 'smith machine'),
                                                                ('0497', 1, 'suspension straps'),
                                                                ('0498', 1, 'suspension straps'),
                                                                ('0496', 1, 'bench'),
                                                                ('2400', 1, 'cable'),
                                                                ('3289', 1, 'parallel bars'),
                                                                ('3288', 1, 'parallel bars'),
                                                                ('0558', 1, 'pull-up bar'),
                                                                ('0570', 1, 'bench'),
                                                                ('3418', 1, 'pull-up bar'),
                                                                ('3237', 1, 'barbell'),
                                                                ('3237', 2, 'landmine'),
                                                                ('0562', 1, 'barbell'),
                                                                ('0562', 2, 'landmine'),
                                                                ('0620', 1, 'bench'),
                                                                ('0627', 1, 'pull-up bar'),
                                                                ('0631', 1, 'pull-up bar'),
                                                                ('1401', 1, 'pull-up bar'),
                                                                ('0638', 1, 'pull-up bar'),
                                                                ('0651', 1, 'pull-up bar'),
                                                                ('0652', 1, 'pull-up bar'),
                                                                ('1307', 1, 'other'),
                                                                ('1307', 2, 'bosu ball'),
                                                                ('0653', 1, 'other'),
                                                                ('0653', 2, 'bosu ball'),
                                                                ('0670', 1, 'pull-up bar'),
                                                                ('0674', 1, 'pull-up bar'),
                                                                ('0678', 1, 'pull-up bar'),
                                                                ('0688', 1, 'pull-up bar'),
                                                                ('1423', 1, 'bench'),
                                                                ('0672', 1, 'parallel bars'),
                                                                ('0677', 1, 'gymnastic rings'),
                                                                ('0709', 1, 'parallel bars'),
                                                                ('1763', 1, 'pull-up bar'),
                                                                ('0720', 1, 'pull-up bar'),
                                                                ('0748', 1, 'smith machine'),
                                                                ('0748', 2, 'bench'),
                                                                ('0753', 1, 'smith machine'),
                                                                ('0753', 2, 'bench'),
                                                                ('0757', 1, 'smith machine'),
                                                                ('0757', 2, 'bench'),
                                                                ('1626', 1, 'smith machine'),
                                                                ('1626', 2, 'bench'),
                                                                ('0751', 1, 'smith machine'),
                                                                ('0751', 2, 'bench'),
                                                                ('1625', 1, 'smith machine'),
                                                                ('1625', 2, 'bench'),
                                                                ('1599', 1, 'rope'),
                                                                ('1599', 2, 'suspension straps'),
                                                                ('1308', 1, 'smith machine'),
                                                                ('1308', 2, 'bench'),
                                                                ('1309', 1, 'smith machine'),
                                                                ('1309', 2, 'bench'),
                                                                ('1705', 1, 'other'),
                                                                ('1705', 2, 'bosu ball'),
                                                                ('0806', 1, 'suspension straps'),
                                                                ('0805', 1, 'suspension straps'),
                                                                ('0807', 1, 'pull-up bar'),
                                                                ('0826', 1, 'parallel bars'),
                                                                ('0808', 1, 'suspension straps'),
                                                                ('0809', 1, 'suspension straps'),
                                                                ('1753', 1, 'bench'),
                                                                ('0812', 1, 'bench'),
                                                                ('0813', 1, 'bench'),
                                                                ('0798', 1, 'stationary bike'),
                                                                ('3666', 1, 'treadmill'),
                                                                ('0830', 1, 'weighted'),
                                                                ('0830', 2, 'bench'),
                                                                ('2138', 1, 'stationary bike'),
                                                                ('0796', 1, 'other'),
                                                                ('0796', 2, 'ab wheel'),
                                                                ('3637', 1, 'ab wheel'),
                                                                ('2363', 1, 'parallel bars'),
                                                                ('1429', 1, 'pull-up bar'),
                                                                ('1367', 1, 'pull-up bar'),
                                                                ('0847', 1, 'medicine ball'),
                                                                ('0847', 2, 'stability ball'),
                                                                ('0866', 1, 'weighted'),
                                                                ('0866', 2, 'pull-up bar'),
                                                                ('0840', 1, 'weighted'),
                                                                ('0840', 2, 'stability ball'),
                                                                ('0849', 1, 'weighted'),
                                                                ('0849', 2, 'stability ball'),
                                                                ('0850', 1, 'weighted'),
                                                                ('0850', 2, 'stability ball'),
                                                                ('0835', 1, 'weighted'),
                                                                ('0835', 2, 'stability ball'),
                                                                ('3286', 1, 'weighted'),
                                                                ('3286', 2, 'pull-up bar'),
                                                                ('3312', 1, 'weighted'),
                                                                ('3312', 2, 'pull-up bar'),
                                                                ('3290', 1, 'weighted'),
                                                                ('3290', 2, 'pull-up bar'),
                                                                ('0841', 1, 'weighted'),
                                                                ('0841', 2, 'pull-up bar'),
                                                                ('1754', 1, 'weighted'),
                                                                ('1754', 2, 'bench'),
                                                                ('1767', 1, 'weighted'),
                                                                ('1767', 2, 'parallel bars'),
                                                                ('0857', 1, 'other'),
                                                                ('0857', 2, 'ab wheel');

DO $$
DECLARE
faltan TEXT;
BEGIN
SELECT string_agg(t.source_code || ' (' || t.name_en || ')', ', ') INTO faltan
FROM v24_req_ejercicio t
         LEFT JOIN exercises e ON e.source_code = t.source_code AND e.name_en = t.name_en
WHERE e.exercise_id IS NULL;
IF faltan IS NOT NULL THEN
        RAISE EXCEPTION 'V24/requisitos: no coinciden %', faltan;
END IF;
END $$;

DELETE FROM exercise_equipment_requirements r
    USING exercises e, v24_req_ejercicio t
WHERE r.exercise_id = e.exercise_id
  AND e.source_code = t.source_code
  AND e.name_en = t.name_en;

INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT e.exercise_id, v.group_no, eq.equipment_id
FROM v24_req v
         JOIN v24_req_ejercicio t ON t.source_code = v.source_code
         JOIN exercises e ON e.source_code = t.source_code AND e.name_en = t.name_en
         JOIN equipment_types eq ON eq.code = v.equipment_code;

DO $$
DECLARE
esperadas INTEGER;
    escritas  INTEGER;
BEGIN
SELECT COUNT(*) INTO esperadas FROM v24_req;
SELECT COUNT(*) INTO escritas
FROM exercise_equipment_requirements r
         JOIN exercises e ON e.exercise_id = r.exercise_id
         JOIN v24_req_ejercicio t ON t.source_code = e.source_code;
IF escritas <> esperadas THEN
        RAISE EXCEPTION 'V24/requisitos: se esperaban % filas y hay %', esperadas, escritas;
END IF;
END $$;

DROP TABLE v24_req;
DROP TABLE v24_req_ejercicio;

-- ------------------------------------------------------------------
-- Implemento principal (el que se muestra): 87 ejercicios
-- ------------------------------------------------------------------
CREATE TEMP TABLE v24_equipment (
    source_code VARCHAR(20)  PRIMARY KEY,
    name_en     VARCHAR(255) NOT NULL,
    valor       VARCHAR(40)   NOT NULL
);

INSERT INTO v24_equipment (source_code, name_en, valor) VALUES
                                                            ('MVP-0051', 'Hanging knee raise', 'pull-up bar'),   -- id 15
                                                            ('MVP-0015', 'Pull-up', 'pull-up bar'),   -- id 18
                                                            ('MVP-0025', 'Bench dip', 'bench'),   -- id 24
                                                            ('2355', 'arm slingers hanging bent knee legs', 'pull-up bar'),   -- id 61
                                                            ('2333', 'arm slingers hanging straight legs', 'pull-up bar'),   -- id 62
                                                            ('3293', 'archer pull up', 'pull-up bar'),   -- id 64
                                                            ('3297', 'back lever', 'pull-up bar'),   -- id 65
                                                            ('1716', 'assisted seated pectoralis major stretch with stability ball', 'stability ball'),   -- id 80
                                                            ('0020', 'balance board', 'balance board'),   -- id 94
                                                            ('1374', 'box jump down with one leg stabilization', 'plyo box'),   -- id 295
                                                            ('3019', 'bench pull-ups', 'bench'),   -- id 300
                                                            ('0130', 'bench hip extension', 'bench'),   -- id 309
                                                            ('0129', 'bench dip (knees bent)', 'bench'),   -- id 313
                                                            ('0139', 'biceps narrow pull-ups', 'pull-up bar'),   -- id 316
                                                            ('0140', 'biceps pull-up', 'pull-up bar'),   -- id 317
                                                            ('0251', 'chest dip', 'parallel bars'),   -- id 457
                                                            ('1430', 'chest dip (on dip-pull-up cage)', 'parallel bars'),   -- id 458
                                                            ('2462', 'chest dip on straight bar', 'parallel bars'),   -- id 459
                                                            ('2963', 'captains chair straight leg raise', 'captains chair'),   -- id 462
                                                            ('1326', 'chin-up', 'pull-up bar'),   -- id 463
                                                            ('0253', 'chin-ups (narrow parallel grip)', 'pull-up bar'),   -- id 464
                                                            ('1327', 'close grip chin-up', 'pull-up bar'),   -- id 506
                                                            ('1382', 'exercise ball on the wall calf raise', 'stability ball'),   -- id 816
                                                            ('3241', 'exercise ball on the wall calf raise (tennis ball between ankles)', 'stability ball'),   -- id 817
                                                            ('3240', 'exercise ball on the wall calf raise (tennis ball between knees)', 'stability ball'),   -- id 818
                                                            ('1746', 'exercise ball supine triceps extension', 'stability ball'),   -- id 820
                                                            ('3296', 'front lever', 'pull-up bar'),   -- id 858
                                                            ('0467', 'gorilla chin', 'pull-up bar'),   -- id 862
                                                            ('1764', 'hanging leg hip raise', 'pull-up bar'),   -- id 865
                                                            ('0472', 'hanging leg raise', 'pull-up bar'),   -- id 866
                                                            ('1761', 'hanging oblique knee raise', 'pull-up bar'),   -- id 867
                                                            ('0473', 'hanging pike', 'pull-up bar'),   -- id 868
                                                            ('0474', 'hanging straight leg hip raise', 'pull-up bar'),   -- id 869
                                                            ('0475', 'hanging straight leg raise', 'pull-up bar'),   -- id 870
                                                            ('0476', 'hanging straight twisting leg hip raise', 'pull-up bar'),   -- id 871
                                                            ('3295', 'front lever reps', 'pull-up bar'),   -- id 873
                                                            ('0466', 'gironda sternum chin', 'pull-up bar'),   -- id 874
                                                            ('3523', 'glute bridge two legs on bench (male)', 'bench'),   -- id 879
                                                            ('3785', 'incline push-up (on box)', 'plyo box'),   -- id 897
                                                            ('0488', 'hyperextension (on bench)', 'bench'),   -- id 909
                                                            ('0499', 'inverted row', 'pull-up bar'),   -- id 910
                                                            ('2300', 'inverted row bent knees', 'pull-up bar'),   -- id 911
                                                            ('2298', 'inverted row on bench', 'bench'),   -- id 912
                                                            ('0497', 'inverted row v. 2', 'smith machine'),   -- id 913
                                                            ('0498', 'inverted row with straps', 'suspension straps'),   -- id 914
                                                            ('0496', 'inverse leg curl (bench support)', 'bench'),   -- id 915
                                                            ('2400', 'inverse leg curl (on pull-up cable machine)', 'cable'),   -- id 916
                                                            ('3289', 'impossible dips', 'parallel bars'),   -- id 920
                                                            ('3288', 'korean dips', 'parallel bars'),   -- id 934
                                                            ('0558', 'kipping muscle up', 'pull-up bar'),   -- id 937
                                                            ('0570', 'leg pull in flat bench', 'bench'),   -- id 976
                                                            ('3418', 'l-pull-up', 'pull-up bar'),   -- id 977
                                                            ('3237', 'landmine lateral raise', 'landmine'),   -- id 979
                                                            ('0562', 'landmine 180', 'landmine'),   -- id 980
                                                            ('0620', 'lying leg raise flat bench', 'bench'),   -- id 1061
                                                            ('0627', 'mixed grip chin-up', 'pull-up bar'),   -- id 1067
                                                            ('0631', 'muscle up', 'pull-up bar'),   -- id 1068
                                                            ('1401', 'muscle-up (on vertical bar)', 'pull-up bar'),   -- id 1069
                                                            ('0638', 'one arm chin-up', 'pull-up bar'),   -- id 1071
                                                            ('0651', 'pull up (neutral grip)', 'pull-up bar'),   -- id 1108
                                                            ('0652', 'pull-up', 'pull-up bar'),   -- id 1109
                                                            ('0670', 'rear pull-up', 'pull-up bar'),   -- id 1143
                                                            ('0674', 'reverse grip pull-up', 'pull-up bar'),   -- id 1144
                                                            ('0678', 'rocky pull-up pulldown', 'pull-up bar'),   -- id 1145
                                                            ('0688', 'scapular pull-up', 'pull-up bar'),   -- id 1147
                                                            ('1423', 'reverse hyper on flat bench', 'bench'),   -- id 1148
                                                            ('0672', 'reverse dip', 'parallel bars'),   -- id 1152
                                                            ('0677', 'ring dips', 'gymnastic rings'),   -- id 1153
                                                            ('0709', 'side hip (on parallel bars)', 'parallel bars'),   -- id 1184
                                                            ('1763', 'shoulder grip pull-up', 'pull-up bar'),   -- id 1189
                                                            ('0720', 'side-to-side chin', 'pull-up bar'),   -- id 1191
                                                            ('0806', 'suspended push-up', 'suspension straps'),   -- id 1297
                                                            ('0805', 'suspended abdominal fallout', 'suspension straps'),   -- id 1300
                                                            ('0807', 'suspended reverse crunch', 'pull-up bar'),   -- id 1301
                                                            ('0826', 'vertical leg raise (on parallel bars)', 'parallel bars'),   -- id 1306
                                                            ('0808', 'suspended row', 'suspension straps'),   -- id 1307
                                                            ('0809', 'suspended split squat', 'suspension straps'),   -- id 1312
                                                            ('1753', 'three bench dip', 'bench'),   -- id 1316
                                                            ('0812', 'triceps dip (bench leg)', 'bench'),   -- id 1318
                                                            ('0813', 'triceps dip (between benches)', 'bench'),   -- id 1319
                                                            ('0798', 'stationary bike walk', 'stationary bike'),   -- id 1324
                                                            ('3666', 'walking on incline treadmill', 'treadmill'),   -- id 1325
                                                            ('2138', 'stationary bike run v. 3', 'stationary bike'),   -- id 1327
                                                            ('3637', 'wheel run', 'ab wheel'),   -- id 1333
                                                            ('2363', 'wide-grip chest dip on high parallel bars', 'parallel bars'),   -- id 1335
                                                            ('1429', 'wide grip pull-up', 'pull-up bar'),   -- id 1336
                                                            ('1367', 'wide grip rear pull-up', 'pull-up bar');   -- id 1337

DO $$
DECLARE
encontrados INTEGER;
    faltan      TEXT;
BEGIN
SELECT COUNT(*) INTO encontrados
FROM exercises e
         JOIN v24_equipment v ON v.source_code = e.source_code AND v.name_en = e.name_en;

IF encontrados <> (SELECT COUNT(*) FROM v24_equipment) THEN
SELECT string_agg(v.source_code || ' (' || v.name_en || ')', ', ') INTO faltan
FROM v24_equipment v
         LEFT JOIN exercises e
                   ON e.source_code = v.source_code AND e.name_en = v.name_en
WHERE e.exercise_id IS NULL;
RAISE EXCEPTION 'V24/equipment: no coinciden %', faltan;
END IF;
END $$;

UPDATE exercises e
SET equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = v.valor)
    FROM v24_equipment v
WHERE v.source_code = e.source_code
  AND v.name_en = e.name_en;

DROP TABLE v24_equipment;


-- ------------------------------------------------------------------
-- Tipo de prescripcion: 8 ejercicios
-- ------------------------------------------------------------------
CREATE TEMP TABLE v24_default_prescription (
    source_code VARCHAR(20)  PRIMARY KEY,
    name_en     VARCHAR(255) NOT NULL,
    valor       VARCHAR(25)   NOT NULL
);

INSERT INTO v24_default_prescription (source_code, name_en, valor) VALUES
                                                                       ('0020', 'balance board', 'DURATION'),   -- id 94
                                                                       ('1494', 'butterfly yoga pose', 'DURATION'),   -- id 312
                                                                       ('3419', 'l-sit on floor', 'DURATION'),   -- id 974
                                                                       ('1582', 'reclining big toe pose with rope', 'DURATION'),   -- id 1163
                                                                       ('3222', 'semi squat jump (male)', 'SETS_REPS'),   -- id 1174
                                                                       ('1587', 'seated wide angle pose sequence', 'DURATION'),   -- id 1194
                                                                       ('3655', 'walking high knees lunge', 'SETS_REPS'),   -- id 1295
                                                                       ('1460', 'walking lunge', 'SETS_REPS');   -- id 1315

DO $$
DECLARE
encontrados INTEGER;
    faltan      TEXT;
BEGIN
SELECT COUNT(*) INTO encontrados
FROM exercises e
         JOIN v24_default_prescription v ON v.source_code = e.source_code AND v.name_en = e.name_en;

IF encontrados <> (SELECT COUNT(*) FROM v24_default_prescription) THEN
SELECT string_agg(v.source_code || ' (' || v.name_en || ')', ', ') INTO faltan
FROM v24_default_prescription v
         LEFT JOIN exercises e
                   ON e.source_code = v.source_code AND e.name_en = v.name_en
WHERE e.exercise_id IS NULL;
RAISE EXCEPTION 'V24/default_prescription: no coinciden %', faltan;
END IF;
END $$;

UPDATE exercises e
SET default_prescription = v.valor
    FROM v24_default_prescription v
WHERE v.source_code = e.source_code
  AND v.name_en = e.name_en;

DROP TABLE v24_default_prescription;

-- ------------------------------------------------------------------
-- Nombres rotos o sin traducir: 1 ejercicios
-- ------------------------------------------------------------------
CREATE TEMP TABLE v24_name_es (
    source_code VARCHAR(20)  PRIMARY KEY,
    name_en     VARCHAR(255) NOT NULL,
    valor       VARCHAR(255)   NOT NULL
);

INSERT INTO v24_name_es (source_code, name_en, valor) VALUES
    ('0778', 'spider crawl push up', 'Flexión con desplazamiento de araña');   -- id 1262

DO $$
DECLARE
encontrados INTEGER;
    faltan      TEXT;
BEGIN
SELECT COUNT(*) INTO encontrados
FROM exercises e
         JOIN v24_name_es v ON v.source_code = e.source_code AND v.name_en = e.name_en;

IF encontrados <> (SELECT COUNT(*) FROM v24_name_es) THEN
SELECT string_agg(v.source_code || ' (' || v.name_en || ')', ', ') INTO faltan
FROM v24_name_es v
         LEFT JOIN exercises e
                   ON e.source_code = v.source_code AND e.name_en = v.name_en
WHERE e.exercise_id IS NULL;
RAISE EXCEPTION 'V24/name_es: no coinciden %', faltan;
END IF;
END $$;

UPDATE exercises e
SET name_es = v.valor
    FROM v24_name_es v
WHERE v.source_code = e.source_code
  AND v.name_en = e.name_en;

DROP TABLE v24_name_es;


-- ------------------------------------------------------------------
-- Ejercicios que exigen un implemento sin codigo: 4 ejercicios
-- ------------------------------------------------------------------
CREATE TEMP TABLE v24_is_active (
    source_code VARCHAR(20)  PRIMARY KEY,
    name_en     VARCHAR(255) NOT NULL,
    valor       BOOLEAN   NOT NULL
);

INSERT INTO v24_is_active (source_code, name_en, valor) VALUES
                                                            ('3533', 'quads', FALSE),   -- id 1116
                                                            ('0730', 'single leg platform slide', FALSE),   -- id 1203
                                                            ('1753', 'three bench dip', FALSE),   -- id 1316
                                                            ('0813', 'triceps dip (between benches)', FALSE);   -- id 1319

DO $$
DECLARE
encontrados INTEGER;
    faltan      TEXT;
BEGIN
SELECT COUNT(*) INTO encontrados
FROM exercises e
         JOIN v24_is_active v ON v.source_code = e.source_code AND v.name_en = e.name_en;

IF encontrados <> (SELECT COUNT(*) FROM v24_is_active) THEN
SELECT string_agg(v.source_code || ' (' || v.name_en || ')', ', ') INTO faltan
FROM v24_is_active v
         LEFT JOIN exercises e
                   ON e.source_code = v.source_code AND e.name_en = v.name_en
WHERE e.exercise_id IS NULL;
RAISE EXCEPTION 'V24/is_active: no coinciden %', faltan;
END IF;
END $$;

UPDATE exercises e
SET is_active = v.valor
    FROM v24_is_active v
WHERE v.source_code = e.source_code
  AND v.name_en = e.name_en;

DROP TABLE v24_is_active;

-- ---------------------------------------------------------------------
-- Comprobacion final: ninguna zona puede quedarse sin ejercicios para
-- ninguno de los tres niveles declarados, o los planes saldrian cortos.
--
-- El techo por nivel es el de FitnessLevel: BEGINNER 1, INTERMEDIATE 2,
-- ADVANCED 3. El estudio admite los tres, asi que se cuentan los tres: las
-- pruebas de los planes 29-39 se hicieron con un perfil principiante, pero
-- eso era el banco de pruebas.
--
-- El umbral son 10 candidatos por zona, que es lo que pide una cuota por
-- sesion del redisenyo de la seccion 4.3. Avisa, no aborta: puede ser
-- aceptable en zonas que no entran en todas las sesiones.
-- ---------------------------------------------------------------------
DO $$
DECLARE
fila   RECORD;
    techo  SMALLINT;
    nivel  TEXT;
BEGIN
    FOREACH techo IN ARRAY ARRAY[1, 2, 3]::SMALLINT[] LOOP
        nivel := CASE techo WHEN 1 THEN 'BEGINNER'
                            WHEN 2 THEN 'INTERMEDIATE'
                            ELSE 'ADVANCED' END;
FOR fila IN
SELECT bp.code AS zona, COUNT(*) AS n
FROM exercises e
         JOIN body_parts bp ON bp.body_part_id = e.body_part_id
WHERE e.is_active
  AND e.difficulty_level <= techo
  AND e.high_impact = FALSE
  -- La misma regla que el filtro real: ningun grupo sin cubrir.
  AND NOT EXISTS (
    SELECT 1 FROM exercise_equipment_requirements r
                      JOIN equipment_types eq ON eq.equipment_id = r.equipment_id
    WHERE r.exercise_id = e.exercise_id
    GROUP BY r.group_no
    HAVING bool_and(eq.code NOT IN ('body weight', 'dumbbell', 'band')))
GROUP BY bp.code
ORDER BY bp.code
    LOOP
            IF fila.n < 10 THEN
                RAISE WARNING 'V24: solo % candidatos en % para %',
                    fila.n, fila.zona, nivel;
ELSE
                RAISE NOTICE 'V24: % candidatos en % para %',
                    fila.n, fila.zona, nivel;
END IF;
END LOOP;
END LOOP;
END $$;
