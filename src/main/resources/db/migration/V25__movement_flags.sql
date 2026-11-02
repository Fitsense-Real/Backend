-- =====================================================================
-- FitSense MVP 1.0 - V25: banderas funcionales de movimiento
--
-- GENERADA AUTOMATICAMENTE por generar_V25.py el 2026-09-22
-- a partir de FitSense-banderas-revision-FINAL.xlsx.
-- No editar a mano: regenerar desde la hoja y volver a copiar.
--
-- Requiere V24. Se comprueba un efecto exclusivo de V24 (la tabla de requisitos
-- la crea V23, asi que su existencia no basta): ver el bloque siguiente.
-- Definiciones: reglas-banderas.md, version 2 (axial_load solo compresiva;
-- la posicion invertida pasa a requires_inversion).
--
-- QUE HACE
--   Esquema
--     exercises.requires_kneeling / requires_hanging / requires_overhead / requires_inversion (false por defecto)
--     user_profiles.allow_* (5), true por defecto: ningun perfil pierde ejercicios
--     restriccion: requires_hanging implica requires_overhead
--   Datos
--      47 ejercicios con requires_hanging
--     239 ejercicios con requires_overhead
--      32 ejercicios con requires_kneeling
--      33 ejercicios con requires_inversion
--   high_impact           8 pasan a true,    5 pasan a false
--   requires_floor       55 pasan a true,  100 pasan a false
--   axial_load           95 pasan a true,   35 pasan a false
--   Muestra: 360 filas para las metricas; 300 fuera de Revision entran con el valor humano (en las demas manda Revision)
--     colgarse: sensibilidad 92.0% (cota inferior 95 %: 76.2%)
--     sobre la cabeza: sensibilidad 92.0% (cota inferior 95 %: 87.2%)
--     rodillas: sensibilidad 100.0% (cota inferior 95 %: 71.5%)
--     inversion: sensibilidad 100.0% (cota inferior 95 %: 89.8%)
--     impacto: sensibilidad 100.0% (cota inferior 95 %: 88.9%)
--     suelo: sensibilidad 100.0% (cota inferior 95 %: 96.3%)
--     axial: sensibilidad 56.5% (cota inferior 95 %: 52.1%)
--
-- Identifica cada ejercicio por source_code y comprueba name_en, como V21 y
-- V24. Si algo no coincide, aborta sin tocar nada. Reaplicarla no cambia nada.
-- NO editar ni reformatear este archivo despues de aplicarlo (checksum de Flyway).
-- =====================================================================

-- Requisito: V24 aplicada. La tabla de requisitos la crea V23, asi que su
-- existencia NO demuestra V24: se comprueba un efecto que solo produce V24
-- ("dumbbell bench press" con dos grupos, mancuerna Y banco) y, si existe el
-- historial de Flyway, que la version 24 figure como aplicada.
DO $$
DECLARE grupos INT; v24_en_flyway BOOLEAN;
BEGIN
    IF to_regclass('exercise_equipment_requirements') IS NULL THEN
        RAISE EXCEPTION 'V25: falta exercise_equipment_requirements. ¿Se aplicaron V23 y V24?';
END IF;
SELECT count(DISTINCT r.group_no) INTO grupos
FROM exercise_equipment_requirements r JOIN exercises e ON e.exercise_id = r.exercise_id
WHERE e.name_en = 'dumbbell bench press' AND e.is_active;
IF grupos <> 2 THEN
        RAISE EXCEPTION 'V25: V24 no esta aplicada (dumbbell bench press tiene % grupos de requisitos, deberian ser 2)', grupos;
END IF;
    -- EXECUTE: una referencia directa a una tabla que no existe falla al
    -- planificar, aunque la condicion no llegue a evaluarse.
    IF to_regclass('flyway_schema_history') IS NOT NULL THEN
        EXECUTE 'SELECT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = ''24'' AND success)'
            INTO v24_en_flyway;
        IF NOT v24_en_flyway THEN
            RAISE EXCEPTION 'V25: la version 24 no figura como aplicada en flyway_schema_history';
END IF;
END IF;
END $$;

-- 1. ESQUEMA ----------------------------------------------------------
ALTER TABLE exercises
    ADD COLUMN IF NOT EXISTS requires_kneeling BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS requires_hanging  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS requires_overhead BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS requires_inversion BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN exercises.requires_kneeling IS
    'Una rodilla apoya y carga peso (suelo, banco o almohadilla). reglas-banderas.md v2.';
COMMENT ON COLUMN exercises.requires_hanging IS
    'El cuerpo cuelga del agarre, pies sin apoyo. Incluye asistidas. reglas-banderas.md v2.';
COMMENT ON COLUMN exercises.requires_overhead IS
    'Brazos por encima de la cabeza en alguna fase, empujando, tirando o sin carga. reglas-banderas.md v2.';
COMMENT ON COLUMN exercises.requires_inversion IS
    'Cabeza claramente por debajo de la cadera con el peso en brazos, hombros o cabeza. Solo politica de edad (65+). reglas-banderas.md v2.';
COMMENT ON COLUMN exercises.axial_load IS
    'Carga compresiva relevante sobre columna o cuello. Desde V25 NO incluye la posicion invertida (requires_inversion). reglas-banderas.md v2.';

ALTER TABLE user_profiles
    ADD COLUMN IF NOT EXISTS allow_high_impact        BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS allow_floor_exercises    BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS allow_kneeling_exercises BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS allow_hanging_exercises  BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS allow_overhead_exercises BOOLEAN NOT NULL DEFAULT TRUE;

COMMENT ON COLUMN user_profiles.allow_kneeling_exercises IS
    'false = excluir requires_kneeling. Restriccion declarada por el perfil; no tiene restriccion automatica por edad.';
COMMENT ON COLUMN user_profiles.allow_hanging_exercises IS
    'false = excluir requires_hanging. Restriccion declarada por el perfil; no tiene restriccion automatica por edad.';
COMMENT ON COLUMN user_profiles.allow_overhead_exercises IS
    'false = excluir requires_overhead. Restriccion declarada por el perfil; no tiene restriccion automatica por edad.';
COMMENT ON COLUMN user_profiles.allow_high_impact IS
    'false = excluir high_impact. Se suma a la politica automatica (60+ y principiantes); nunca la relaja.';
COMMENT ON COLUMN user_profiles.allow_floor_exercises IS
    'false = excluir requires_floor. Se suma a la politica automatica (70+); nunca la relaja.';

-- 2. DATOS -----------------------------------------------------------
CREATE TEMP TABLE v25_banderas (
    source_code       VARCHAR(20)  PRIMARY KEY,
    name_en           VARCHAR(255) NOT NULL,
    requires_hanging  BOOLEAN NOT NULL,
    requires_overhead BOOLEAN NOT NULL,
    requires_kneeling BOOLEAN NOT NULL,
    requires_inversion BOOLEAN NOT NULL,
    high_impact       BOOLEAN NOT NULL,
    requires_floor    BOOLEAN NOT NULL,
    axial_load        BOOLEAN NOT NULL
) ON COMMIT DROP;

INSERT INTO v25_banderas VALUES
                             ('1368', 'ankle circles', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 55
                             ('0003', 'air bike', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 59
                             ('0006', 'alternate heel touchers', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 60
                             ('2355', 'arm slingers hanging bent knee legs', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 61
                             ('2333', 'arm slingers hanging straight legs', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 62
                             ('3204', 'arms overhead full sit-up (male)', FALSE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 63
                             ('3293', 'archer pull up', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 64
                             ('3297', 'back lever', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 65
                             ('1512', 'all fours squad stretch', FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE),   -- id 67
                             ('0014', 'assisted motion russian twist', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 70
                             ('0007', 'alternate lateral pulldown', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 71
                             ('0009', 'assisted chest dip (kneeling)', FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE),   -- id 72
                             ('2364', 'assisted wide-grip chest dip (kneeling)', FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE),   -- id 73
                             ('0015', 'assisted parallel close grip pull-up', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 74
                             ('0017', 'assisted pull-up', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 75
                             ('1431', 'assisted standing chin-up', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 76
                             ('1432', 'assisted standing pull-up', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 77
                             ('0019', 'assisted triceps dip (kneeling)', FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE),   -- id 78
                             ('0997', 'band shoulder press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 101
                             ('0969', 'band alternating v-up', FALSE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 104
                             ('0971', 'band assisted wheel rollerout', FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE),   -- id 105
                             ('0981', 'band jack knife sit-up', FALSE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 108
                             ('0985', 'band kneeling twisting crunch', FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE),   -- id 109
                             ('1011', 'band seated twist', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 112
                             ('0970', 'band assisted pull-up', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 113
                             ('0974', 'band close-grip pulldown', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 114
                             ('3117', 'band fixed back close grip pulldown', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 115
                             ('3116', 'band fixed back underhand pulldown', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 116
                             ('0983', 'band kneeling one arm pulldown', FALSE, TRUE, TRUE, FALSE, FALSE, TRUE, FALSE),   -- id 117
                             ('1408', 'band hip lift', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 122
                             ('0986', 'band one arm overhead biceps curl', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 131
                             ('1012', 'band twisting overhead press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 136
                             ('1017', 'band y-raise', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 137
                             ('1005', 'band standing crunch', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 138
                             ('1007', 'band standing twisting crunch', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 139
                             ('1014', 'band v-up', FALSE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 140
                             ('1013', 'band underhand pulldown', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 143
                             ('1370', 'barbell floor calf raise', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 149
                             ('0033', 'barbell decline bench press', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 152
                             ('1255', 'barbell decline pullover', FALSE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 153
                             ('0036', 'barbell decline wide-grip press', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 154
                             ('0040', 'barbell front raise and pullover', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 155
                             ('1316', 'barbell bent arm pullover', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 156
                             ('0034', 'barbell decline bent arm pullover', FALSE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 158
                             ('0037', 'barbell decline wide-grip pullover', FALSE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 159
                             ('0024', 'barbell bench front squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 160
                             ('0026', 'barbell bench squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 161
                             ('0028', 'barbell clean and press', FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, TRUE),   -- id 162
                             ('0029', 'barbell clean-grip front squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 163
                             ('0039', 'barbell front chest squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 165
                             ('0042', 'barbell front squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 166
                             ('0035', 'barbell decline close grip to skull press', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 171
                             ('0067', 'barbell one arm snatch', FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, TRUE),   -- id 175
                             ('0050', 'barbell incline shoulder raise', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 178
                             ('0043', 'barbell full squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 182
                             ('1461', 'barbell full squat (back pov)', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 183
                             ('1462', 'barbell full squat (side pov)', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 184
                             ('3562', 'barbell glute bridge two legs on bench (male)', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 187
                             ('0046', 'barbell hack squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 189
                             ('1436', 'barbell high bar squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 190
                             ('0053', 'barbell jump squat', FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE),   -- id 192
                             ('1410', 'barbell lateral lunge', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 193
                             ('1435', 'barbell low bar squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 194
                             ('0054', 'barbell lunge', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 195
                             ('0058', 'barbell lying lifting (on hip)', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 196
                             ('0063', 'barbell narrow stance squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 197
                             ('0068', 'barbell one leg squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 199
                             ('0069', 'barbell overhead squat', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 200
                             ('1720', 'barbell lying back of the head tricep extension', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 204
                             ('0055', 'barbell lying close-grip press', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 205
                             ('0056', 'barbell lying close-grip triceps extension', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 206
                             ('0057', 'barbell lying extension', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 207
                             ('0059', 'barbell lying preacher curl', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 208
                             ('0061', 'barbell lying triceps extension', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 209
                             ('0060', 'barbell lying triceps extension skull crusher', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 210
                             ('0086', 'barbell seated behind head military press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 219
                             ('0087', 'barbell seated bradford rocky press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 220
                             ('0091', 'barbell seated overhead press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 221
                             ('1256', 'barbell reverse grip decline bench press', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 222
                             ('0084', 'barbell rollerout', FALSE, TRUE, TRUE, FALSE, FALSE, TRUE, FALSE),   -- id 225
                             ('0083', 'barbell rollerout from bench', FALSE, TRUE, TRUE, FALSE, FALSE, TRUE, FALSE),   -- id 226
                             ('0073', 'barbell pullover', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 230
                             ('0022', 'barbell pullover to press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 231
                             ('0095', 'barbell shrug', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 234
                             ('0074', 'barbell rack pull', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 235
                             ('0078', 'barbell rear lunge', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 236
                             ('0077', 'barbell rear lunge v. 2', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 237
                             ('0098', 'barbell side split squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 240
                             ('0097', 'barbell side split squat v. 2', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 241
                             ('0099', 'barbell single leg split squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 243
                             ('0072', 'barbell prone incline curl', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 245
                             ('1718', 'barbell seated close grip behind neck triceps extension', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 250
                             ('0092', 'barbell seated overhead triceps extension', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 252
                             ('3360', 'bear crawl', FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE),   -- id 253
                             ('1372', 'barbell standing calf raise', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 259
                             ('0108', 'barbell standing leg calf raise', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 260
                             ('0111', 'barbell standing rocking leg calf raise', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 261
                             ('0100', 'barbell skier', FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE),   -- id 262
                             ('0105', 'barbell standing bradford press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 263
                             ('1456', 'barbell standing close grip military press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 264
                             ('0107', 'barbell standing front raise over head', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 265
                             ('1457', 'barbell standing wide military press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 266
                             ('3305', 'barbell thruster', FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, TRUE),   -- id 267
                             ('0103', 'barbell standing ab rollerout', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 275
                             ('0101', 'barbell speed squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 277
                             ('2810', 'barbell split squat v. 2', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 278
                             ('0102', 'barbell squat (on knees)', FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, TRUE),   -- id 279
                             ('2798', 'barbell squat jump step rear lunge', FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE),   -- id 280
                             ('0114', 'barbell step-up', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 281
                             ('0124', 'barbell wide squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 285
                             ('0109', 'barbell standing overhead triceps extension', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 289
                             ('1160', 'burpee', FALSE, TRUE, FALSE, FALSE, TRUE, TRUE, FALSE),   -- id 293
                             ('0138', 'bottoms-up', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 297
                             ('0870', 'butt-ups', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 299
                             ('3019', 'bench pull-ups', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 300
                             ('1494', 'butterfly yoga pose', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 312
                             ('1399', 'bench dip on floor', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 314
                             ('0139', 'biceps narrow pull-ups', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 316
                             ('0140', 'biceps pull-up', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 317
                             ('1771', 'bodyweight kneeling triceps extension', FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE),   -- id 319
                             ('0148', 'cable alternate shoulder press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 321
                             ('0150', 'cable bar lateral pulldown', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 325
                             ('0153', 'cable cross-over lateral pulldown', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 326
                             ('3697', 'cable kneeling rear delt row (with rope) (male)', FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE),   -- id 336
                             ('1261', 'cable decline press', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 340
                             ('0185', 'cable lying fly', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 345
                             ('0175', 'cable kneeling crunch', FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE),   -- id 348
                             ('0167', 'cable high row (kneeling)', FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE),   -- id 351
                             ('2330', 'cable lat pulldown full range of motion', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 354
                             ('0177', 'cable lateral pulldown (with rope attachment)', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 355
                             ('2616', 'cable lateral pulldown with v-bar', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 356
                             ('0184', 'cable lying extension pullover (with rope attachment)', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 358
                             ('1722', 'cable high pulley overhead tricep extension', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 365
                             ('0173', 'cable incline triceps extension', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 366
                             ('0176', 'cable kneeling triceps extension', FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE),   -- id 368
                             ('1634', 'cable lying bicep curl', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 369
                             ('0182', 'cable lying close-grip curl', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 370
                             ('0186', 'cable lying triceps extension v. 2', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 371
                             ('1262', 'cable one arm decline chest fly', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 377
                             ('3563', 'cable one arm pulldown', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 386
                             ('0193', 'cable one arm straight back high row (kneeling)', FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE),   -- id 387
                             ('0198', 'cable pulldown', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 389
                             ('0197', 'cable pulldown (pro lat bar)', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 390
                             ('0205', 'cable rear pulldown', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 392
                             ('1637', 'cable overhead curl on exercise ball', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 401
                             ('0194', 'cable overhead triceps extension (rope attachment)', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 402
                             ('1638', 'cable pulldown bicep curl', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 404
                             ('0219', 'cable shoulder press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 418
                             ('0212', 'cable seated crunch', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 424
                             ('0221', 'cable side bend crunch (bosu ball)', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 427
                             ('0223', 'cable side crunch', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 428
                             ('0226', 'cable standing crunch', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 429
                             ('0874', 'cable standing crunch (with rope attachment)', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 430
                             ('1724', 'cable rope high pulley overhead tricep extension', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 442
                             ('1725', 'cable rope incline tricep extension', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 443
                             ('0232', 'cable standing pulldown (with rope)', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 452
                             ('1407', 'calf push stretch with hands against wall', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 453
                             ('1326', 'chin-up', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 463
                             ('0253', 'chin-ups (narrow parallel grip)', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 464
                             ('0248', 'cambered bar lying row', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 468
                             ('0240', 'cable supine reverse fly', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 471
                             ('0245', 'cable underhand pulldown', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 485
                             ('1325', 'cable wide grip rear pulldown behind neck', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 487
                             ('1727', 'cable standing reverse grip one arm overhead tricep extension', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 488
                             ('0279', 'decline push-up', FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, FALSE),   -- id 495
                             ('1275', 'drop push up', FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE),   -- id 496
                             ('0260', 'cocoons', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 497
                             ('1468', 'crab twist toe touch', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 498
                             ('0267', 'crunch (hands overhead)', FALSE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 500
                             ('3016', 'curl-up', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 502
                             ('0277', 'decline crunch', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 504
                             ('0282', 'decline sit-up', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 505
                             ('1327', 'close grip chin-up', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 506
                             ('2398', 'close-grip push-up (on knees)', FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE),   -- id 509
                             ('0286', 'dumbbell alternate side press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 511
                             ('2137', 'dumbbell arnold press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 512
                             ('0287', 'dumbbell arnold press v. 2', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 513
                             ('0288', 'dumbbell around pullover', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 516
                             ('0271', 'crunch (on stability ball)', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 530
                             ('0272', 'crunch (on stability ball, arms straight)', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 531
                             ('1201', 'dumbbell burpee', FALSE, TRUE, FALSE, FALSE, TRUE, TRUE, FALSE),   -- id 533
                             ('0299', 'dumbbell cuban press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 535
                             ('2136', 'dumbbell cuban press v. 2', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 536
                             ('0301', 'dumbbell decline bench press', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 540
                             ('0302', 'dumbbell decline fly', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 541
                             ('0303', 'dumbbell decline hammer press', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 542
                             ('1276', 'dumbbell decline one arm fly', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 543
                             ('0307', 'dumbbell decline twist fly', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 544
                             ('0305', 'dumbbell decline shrug', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 549
                             ('0304', 'dumbbell decline shrug v. 2', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 550
                             ('1617', 'dumbbell decline one arm hammer press', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 565
                             ('0306', 'dumbbell decline triceps extension', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 566
                             ('1732', 'dumbbell forward lunge triceps extension', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 567
                             ('0325', 'dumbbell incline raise', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 574
                             ('0328', 'dumbbell incline shoulder raise', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 590
                             ('0330', 'dumbbell incline triceps extension', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 604
                             ('1660', 'dumbbell kneeling bicep curl exercise ball', FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE),   -- id 608
                             ('1658', 'dumbbell lunge with bicep curl', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 609
                             ('0337', 'dumbbell lying extension (across face)', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 610
                             ('1729', 'dumbbell lying alternate extension', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 611
                             ('0338', 'dumbbell lying elbow press', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 612
                             ('0347', 'dumbbell lying pronation', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 613
                             ('2705', 'dumbbell lying pronation on floor', FALSE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 614
                             ('0349', 'dumbbell lying supination', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 615
                             ('2470', 'dumbbell lying on floor rear delt raise', FALSE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 618
                             ('0341', 'dumbbell lying one arm deltoid rear', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 619
                             ('0345', 'dumbbell lying one arm rear lateral raise', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 620
                             ('0348', 'dumbbell lying rear lateral raise', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 621
                             ('0340', 'dumbbell lying hammer press', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 624
                             ('0343', 'dumbbell lying one arm press', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 625
                             ('0342', 'dumbbell lying one arm press v. 2', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 626
                             ('1284', 'dumbbell lying pullover on exercise ball', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 627
                             ('1287', 'dumbbell one arm decline chest press', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 630
                             ('1290', 'dumbbell one arm press on exercise ball', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 633
                             ('1291', 'dumbbell one arm pullover on exercise ball', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 634
                             ('1328', 'dumbbell lying rear delt row', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 635
                             ('0344', 'dumbbell lying one arm pronated triceps extension', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 638
                             ('0346', 'dumbbell lying one arm supinated triceps extension', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 639
                             ('1735', 'dumbbell lying single extension', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 640
                             ('1661', 'dumbbell lying supine biceps curl', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 641
                             ('0350', 'dumbbell lying supine curl', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 642
                             ('0351', 'dumbbell lying triceps extension', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 643
                             ('1662', 'dumbbell lying wide curl', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 644
                             ('1736', 'dumbbell one arm french press on exercise ball', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 647
                             ('1621', 'dumbbell one arm hammer press on exercise ball', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 649
                             ('1665', 'dumbbell one arm prone curl', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 651
                             ('1666', 'dumbbell one arm prone hammer curl', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 652
                             ('0361', 'dumbbell one arm shoulder press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 661
                             ('0360', 'dumbbell one arm shoulder press v. 2', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 662
                             ('1700', 'dumbbell push press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 664
                             ('1622', 'dumbbell one arm reverse grip press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 667
                             ('0375', 'dumbbell pullover', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 670
                             ('1294', 'dumbbell pullover hip extension on exercise ball', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 671
                             ('1295', 'dumbbell pullover on exercise ball', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 672
                             ('3888', 'dumbbell one arm snatch', FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, TRUE),   -- id 674
                             ('0362', 'dumbbell one arm triceps extension (on bench)', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 682
                             ('0373', 'dumbbell pronate-grip triceps extension', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 690
                             ('0374', 'dumbbell prone incline curl', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 691
                             ('1674', 'dumbbell prone incline hammer curl', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 692
                             ('2397', 'dumbbell scott press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 702
                             ('0388', 'dumbbell seated alternate press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 704
                             ('3546', 'dumbbell seated alternate shoulder', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 705
                             ('1330', 'dumbbell reverse grip incline bench one arm row', FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE),   -- id 711
                             ('0381', 'dumbbell rear lunge', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 714
                             ('0389', 'dumbbell seated bench extension', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 720
                             ('3547', 'dumbbell seated biceps curl to shoulder press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 725
                             ('0405', 'dumbbell seated shoulder press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 739
                             ('0404', 'dumbbell seated shoulder press (parallel grip)', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 740
                             ('3548', 'dumbbell single arm overhead carry', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 742
                             ('0414', 'dumbbell standing alternate overhead press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 743
                             ('2143', 'dumbbell standing around world', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 745
                             ('0419', 'dumbbell standing front raise above head', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 746
                             ('0424', 'dumbbell standing one arm palm in press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 747
                             ('0413', 'dumbbell squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 755
                             ('1738', 'dumbbell seated reverse grip one arm overhead tricep extension', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 758
                             ('2188', 'dumbbell seated triceps extension', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 759
                             ('3560', 'dumbbell standing alternate hammer curl and press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 760
                             ('0423', 'dumbbell standing one arm extension', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 771
                             ('0443', 'elbow-to-knee', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 774
                             ('0426', 'dumbbell standing overhead press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 778
                             ('0427', 'dumbbell standing palms in press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 779
                             ('0438', 'dumbbell w-press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 783
                             ('0433', 'dumbbell straight arm pullover', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 784
                             ('2803', 'dumbbell supported squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 791
                             ('0430', 'dumbbell standing triceps extension', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 794
                             ('0436', 'dumbbell tate press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 797
                             ('2189', 'dumbbells seated triceps extension', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 803
                             ('1333', 'exercise ball back extension with arms extended', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 805
                             ('1334', 'exercise ball back extension with hands behind head', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 806
                             ('1339', 'exercise ball lat stretch', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 810
                             ('1559', 'exercise ball hip flexor stretch', FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE),   -- id 811
                             ('3303', 'flag', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 813
                             ('0456', 'flexion leg sit up (bent knee)', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 814
                             ('0457', 'flexion leg sit up (straight arm)', FALSE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 815
                             ('1746', 'exercise ball supine triceps extension', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 820
                             ('1296', 'exercise ball pike push up', FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE),   -- id 821
                             ('1342', 'exercise ball lying side lat stretch', FALSE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 823
                             ('1416', 'exercise ball one leg prone lower body rotation', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 825
                             ('1417', 'exercise ball one legged diagonal kick hamstring curl', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 826
                             ('1745', 'exercise ball seated triceps stretch', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 828
                             ('0445', 'ez barbell anti gravity press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 830
                             ('3010', 'ez bar lying bent arms pullover', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 831
                             ('1747', 'ez bar french press on exercise ball', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 833
                             ('1748', 'ez bar lying close grip triceps extension behind head', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 834
                             ('1749', 'ez bar standing french press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 836
                             ('0448', 'ez barbell decline close grip face press', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 840
                             ('2186', 'ez barbell decline triceps extension', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 841
                             ('0449', 'ez barbell incline triceps extension', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 842
                             ('0453', 'ez barbell seated triceps extension', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 847
                             ('3636', 'high knee against wall', FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE),   -- id 854
                             ('3301', 'frog planche', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 857
                             ('3296', 'front lever', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 858
                             ('3299', 'full planche', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 861
                             ('0467', 'gorilla chin', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 862
                             ('1764', 'hanging leg hip raise', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 865
                             ('0472', 'hanging leg raise', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 866
                             ('1761', 'hanging oblique knee raise', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 867
                             ('0473', 'hanging pike', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 868
                             ('0474', 'hanging straight leg hip raise', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 869
                             ('0475', 'hanging straight leg raise', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 870
                             ('0476', 'hanging straight twisting leg hip raise', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 871
                             ('0484', 'hip raise (bent knee)', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 872
                             ('3295', 'front lever reps', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 873
                             ('0466', 'gironda sternum chin', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 874
                             ('3523', 'glute bridge two legs on bench (male)', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 879
                             ('3215', 'hands reversed clasped circular toe touch (male)', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 883
                             ('3302', 'handstand', FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE),   -- id 885
                             ('0471', 'handstand push-up', FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE),   -- id 886
                             ('0501', 'jack burpee', FALSE, TRUE, FALSE, FALSE, TRUE, TRUE, FALSE),   -- id 893
                             ('3224', 'jack jump (male)', FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE),   -- id 894
                             ('0492', 'incline push up depth jump', FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE),   -- id 895
                             ('0493', 'incline push-up', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 896
                             ('3785', 'incline push-up (on box)', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 897
                             ('0494', 'incline reverse grip push-up', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 898
                             ('3011', 'incline scapula push up', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 899
                             ('0500', 'isometric wipers', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 901
                             ('1471', 'inchworm', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 902
                             ('3698', 'inchworm v. 2', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 903
                             ('0495', 'incline twisting sit-up', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 905
                             ('0507', 'jackknife sit-up', FALSE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 906
                             ('0489', 'hyperextension', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 908
                             ('0488', 'hyperextension (on bench)', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 909
                             ('0499', 'inverted row', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 910
                             ('2300', 'inverted row bent knees', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 911
                             ('2298', 'inverted row on bench', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 912
                             ('0497', 'inverted row v. 2', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 913
                             ('0498', 'inverted row with straps', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 914
                             ('1419', 'iron cross stretch', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 917
                             ('0490', 'incline close-grip push-up', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 921
                             ('0520', 'kettlebell alternating press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 923
                             ('0523', 'kettlebell arnold press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 924
                             ('0517', 'kettlebell advanced windmill', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 926
                             ('0524', 'kettlebell bent press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 927
                             ('0521', 'kettlebell alternating renegade row', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 928
                             ('3211', 'kneeling push-up (male)', FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE),   -- id 933
                             ('3239', 'kneeling plank tap shoulder (male)', FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE),   -- id 936
                             ('0558', 'kipping muscle up', TRUE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE),   -- id 937
                             ('1346', 'kneeling lat stretch', FALSE, TRUE, TRUE, FALSE, FALSE, TRUE, FALSE),   -- id 938
                             ('0527', 'kettlebell double jerk', FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, TRUE),   -- id 940
                             ('0528', 'kettlebell double push press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 941
                             ('0529', 'kettlebell double snatch', FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, TRUE),   -- id 942
                             ('0537', 'kettlebell one arm clean and jerk', FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, TRUE),   -- id 943
                             ('0538', 'kettlebell one arm jerk', FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, TRUE),   -- id 944
                             ('0539', 'kettlebell one arm military press to the side', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 945
                             ('0540', 'kettlebell one arm push press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 946
                             ('0542', 'kettlebell one arm snatch', FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, TRUE),   -- id 947
                             ('0543', 'kettlebell pirate supper legs', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 948
                             ('0546', 'kettlebell seated press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 949
                             ('1438', 'kettlebell seated two arm military press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 950
                             ('0547', 'kettlebell seesaw press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 951
                             ('0550', 'kettlebell thruster', FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, TRUE),   -- id 952
                             ('0553', 'kettlebell two arm military press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 954
                             ('0530', 'kettlebell double windmill', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 958
                             ('0554', 'kettlebell windmill', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 960
                             ('0548', 'kettlebell sumo high pull', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 962
                             ('0533', 'kettlebell front squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 964
                             ('0551', 'kettlebell turkish get up (squat style)', FALSE, TRUE, TRUE, FALSE, FALSE, TRUE, TRUE),   -- id 970
                             ('1420', 'kneeling jump squat', FALSE, FALSE, TRUE, FALSE, TRUE, TRUE, TRUE),   -- id 972
                             ('3300', 'lean planche', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 975
                             ('3418', 'l-pull-up', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 977
                             ('1576', 'leg up hamstring stretch', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 978
                             ('0587', 'lever military press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 987
                             ('0583', 'lever kneeling twist', FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE),   -- id 993
                             ('0572', 'lever assisted chin-up', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 995
                             ('0579', 'lever front pulldown', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 998
                             ('0582', 'lever kneeling leg curl', FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE),   -- id 1007
                             ('0586', 'lever lying leg curl', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1009
                             ('3195', 'lever lying two-one leg curl', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1010
                             ('0605', 'lever standing calf raise', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 1018
                             ('0590', 'lever one arm shoulder press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 1019
                             ('0603', 'lever shoulder press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 1022
                             ('0869', 'lever shoulder press v. 2', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 1023
                             ('2318', 'lever shoulder press v. 3', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 1024
                             ('1452', 'lever seated crunch', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1027
                             ('0595', 'lever seated crunch (chest pad)', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1028
                             ('3760', 'lever seated crunch v. 2', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1029
                             ('0600', 'lever seated leg raise crunch', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1030
                             ('1347', 'lever one arm lateral wide pulldown', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1032
                             ('2285', 'lever pullover', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1033
                             ('2736', 'lever reverse grip lateral pulldown', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1034
                             ('0593', 'lever reverse hyperextension', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1042
                             ('1403', 'neck side stretch', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1053
                             ('1387', 'one leg floor calf raise', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1057
                             ('0620', 'lying leg raise flat bench', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1061
                             ('1352', 'lower back curl', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 1066
                             ('0627', 'mixed grip chin-up', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1067
                             ('0631', 'muscle up', TRUE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE),   -- id 1068
                             ('1401', 'muscle-up (on vertical bar)', TRUE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE),   -- id 1069
                             ('0638', 'one arm chin-up', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1071
                             ('1303', 'medicine ball chest push from 3 point stance', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1081
                             ('1304', 'medicine ball chest push multiple response', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1082
                             ('1305', 'medicine ball chest push single response', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1083
                             ('1312', 'medicine ball chest push with run release', FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE),   -- id 1084
                             ('0640', 'one arm slam (with medicine ball)', FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE),   -- id 1085
                             ('1353', 'medicine ball catch and overhead throw', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1086
                             ('1354', 'medicine ball overhead slam', FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE),   -- id 1087
                             ('1750', 'medicine ball supine chest throw', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1089
                             ('0659', 'push-up (wall)', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1097
                             ('0658', 'push-up (wall) v. 2', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1098
                             ('3147', 'pelvic tilt', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 1101
                             ('1687', 'posterior step to overhead reach', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1102
                             ('0651', 'pull up (neutral grip)', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1108
                             ('0652', 'pull-up', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1109
                             ('3662', 'pike-to-cobra push-up', FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, FALSE),   -- id 1113
                             ('0643', 'overhead triceps stretch', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1119
                             ('0655', 'push-up (on stability ball)', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1122
                             ('0656', 'push-up (on stability ball)', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1123
                             ('1707', 'prone twist on stability ball', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1124
                             ('0685', 'run', FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE),   -- id 1133
                             ('0684', 'run (equipment)', FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE),   -- id 1134
                             ('0687', 'russian twist', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 1141
                             ('0670', 'rear pull-up', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1143
                             ('0674', 'reverse grip pull-up', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1144
                             ('0678', 'rocky pull-up pulldown', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1145
                             ('0688', 'scapular pull-up', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1147
                             ('2571', 'rocking frog stretch', FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE),   -- id 1149
                             ('1424', 'seated glute stretch', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 1151
                             ('3122', 'resistance band seated shoulder press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1154
                             ('3144', 'resistance band seated straight back row', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 1156
                             ('3236', 'resistance band hip thrusts on knees (female)', FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE),   -- id 1157
                             ('0673', 'reverse grip machine lat pulldown', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1164
                             ('0716', 'side push neck stretch', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1173
                             ('3656', 'short stride run', FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE),   -- id 1175
                             ('3671', 'ski step', FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE),   -- id 1177
                             ('3699', 'shoulder tap', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 1182
                             ('1763', 'shoulder grip pull-up', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1189
                             ('1358', 'side lying floor stretch', FALSE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 1190
                             ('0720', 'side-to-side chin', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1191
                             ('3304', 'skin the cat', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1192
                             ('2567', 'seated piriformis stretch', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 1193
                             ('1587', 'seated wide angle pose sequence', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 1194
                             ('0697', 'self assisted inverse leg curl', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 1195
                             ('1489', 'sissy squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1205
                             ('1393', 'smith one leg floor calf raise', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1213
                             ('0747', 'smith behind neck press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 1216
                             ('0753', 'smith decline bench press', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 1219
                             ('0754', 'smith decline reverse-grip press', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 1220
                             ('1626', 'smith machine reverse decline close grip bench press', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 1224
                             ('0750', 'smith chair squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 1232
                             ('1433', 'smith front squat (clean grip)', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 1234
                             ('3281', 'smith full squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 1235
                             ('0755', 'smith hack squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 1236
                             ('1434', 'smith low bar squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 1238
                             ('1625', 'smith machine decline close grip bench press', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 1241
                             ('2329', 'spine twist', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 1256
                             ('1363', 'spine stretch', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 1258
                             ('1685', 'squat to overhead reach', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1264
                             ('1686', 'squat to overhead reach with twist', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1265
                             ('3291', 'stalder press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1267
                             ('2297', 'stability ball crunch (full range hands behind head)', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1269
                             ('0788', 'standing behind neck press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 1271
                             ('0786', 'squat jerk', FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, TRUE),   -- id 1273
                             ('0773', 'smith standing leg calf raise', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 1277
                             ('1396', 'smith toe raise', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 1278
                             ('0765', 'smith seated shoulder press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 1279
                             ('0766', 'smith shoulder press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 1280
                             ('0772', 'smith standing behind head military press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 1281
                             ('0774', 'smith standing military press', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 1282
                             ('1309', 'smith wide grip decline bench press', FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE),   -- id 1286
                             ('0768', 'smith single leg split squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 1288
                             ('0769', 'smith sprint lunge', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 1289
                             ('0770', 'smith squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 1290
                             ('3142', 'smith sumo squat', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE),   -- id 1291
                             ('3223', 'star jump (male)', FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE),   -- id 1293
                             ('0803', 'superman push-up', FALSE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 1296
                             ('0806', 'suspended push-up', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1297
                             ('3314', 'straddle maltese', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1298
                             ('3298', 'straddle planche', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 1299
                             ('0807', 'suspended reverse crunch', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1301
                             ('2802', 'twisted leg raise', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 1303
                             ('2801', 'twisted leg raise (female)', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 1304
                             ('1365', 'upper back stretch', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1309
                             ('1427', 'straight leg outer hip abductor', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 1311
                             ('3433', 'swimmer kicks v. 2 (male)', FALSE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 1313
                             ('1466', 'twist hip lift', FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 1314
                             ('0815', 'triceps dips floor', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1320
                             ('0817', 'triceps stretch', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1322
                             ('0818', 'twin handle parallel grip lat pulldown', FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1323
                             ('3637', 'wheel run', FALSE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE),   -- id 1333
                             ('1429', 'wide grip pull-up', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1336
                             ('1367', 'wide grip rear pull-up', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE),   -- id 1337
                             ('1604', 'world greatest stretch', FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE);   -- id 1375

DO $$
DECLARE faltan TEXT;
BEGIN
SELECT string_agg(t.source_code || ' (' || t.name_en || ')', ', ') INTO faltan
FROM v25_banderas t
         LEFT JOIN exercises e ON e.source_code = t.source_code AND e.name_en = t.name_en
WHERE e.exercise_id IS NULL;
IF faltan IS NOT NULL THEN
        RAISE EXCEPTION 'V25: ejercicios que no existen o cambiaron de nombre: %', faltan;
END IF;
END $$;

UPDATE exercises e SET
                       requires_hanging  = t.requires_hanging,
                       requires_overhead = t.requires_overhead,
                       requires_kneeling = t.requires_kneeling,
                       requires_inversion = t.requires_inversion,
                       high_impact       = t.high_impact,
                       requires_floor    = t.requires_floor,
                       axial_load        = t.axial_load
    FROM v25_banderas t
WHERE e.source_code = t.source_code AND e.name_en = t.name_en;

ALTER TABLE exercises DROP CONSTRAINT IF EXISTS ck_exercises_hanging_is_overhead;
ALTER TABLE exercises ADD CONSTRAINT ck_exercises_hanging_is_overhead
    CHECK (NOT requires_hanging OR requires_overhead);

-- 3. COMPROBACION -----------------------------------------------------
DO $$
DECLARE n_h INT; n_o INT; n_k INT; n_i INT;
BEGIN
SELECT count(*) FILTER (WHERE requires_hanging), count(*) FILTER (WHERE requires_overhead),
        count(*) FILTER (WHERE requires_kneeling), count(*) FILTER (WHERE requires_inversion)
INTO n_h, n_o, n_k, n_i FROM exercises WHERE is_active;
IF n_h <> 47 OR n_o <> 239 OR n_k <> 32 OR n_i <> 33 THEN
        RAISE EXCEPTION 'V25: recuentos inesperados (colgarse %, sobre la cabeza %, rodillas %, inversion %)', n_h, n_o, n_k, n_i;
END IF;
    RAISE NOTICE 'V25: % activos requieren colgarse, % brazos sobre la cabeza, % rodillas, % inversion', n_h, n_o, n_k, n_i;
END $$;
