-- =====================================================================
-- FitSense MVP 1.0 - V26: patron de movimiento y aislamiento (Step 3A)
--
-- GENERADA AUTOMATICAMENTE por generar_V26.py desde patrones-catalogo-final.csv.
-- No editar a mano: regenerar desde el CSV y volver a copiar.
--
-- Requiere V25 (comprueba un efecto exclusivo suyo, ver el bloque siguiente).
-- Definiciones: reglas-patrones-v2.md (22/09/2026), 24 clases cerradas.
-- Procedencia de cada etiqueta: criterio-clasificacion-final-step3A.md e
-- informe-verificacion-371.md.
--
-- QUE HACE
--   Esquema
--     exercises.movement_pattern VARCHAR(50)  CHECK sobre las 24 clases
--     exercises.is_isolation     BOOLEAN
--     CHECK: todo ejercicio ACTIVO tiene patron y is_isolation
--     (los inactivos pueden quedar en NULL: no se clasificaron)
--   Datos: 1221 ejercicios activos, 428 con is_isolation = TRUE
--     origen de las etiquetas: HUMANO_RONDA_D3.1 441, HUMANO_RONDA_D3.2 407, DETECTOR_D3.2_VERIFICADO 367, HUMANO_READJUDICADO 6
--
-- Identifica cada ejercicio por source_code y comprueba name_en, como V21,
-- V24 y V25. Si algo no coincide, aborta sin tocar nada. Reaplicarla no cambia
-- nada. NO editar ni reformatear despues de aplicarla (checksum de Flyway).
-- =====================================================================

-- Requisito: V25 aplicada. Se comprueba un efecto exclusivo de V25 (la columna
-- requires_inversion) y, si existe el historial de Flyway, que V25 conste.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                    WHERE table_name = 'exercises' AND column_name = 'requires_inversion') THEN
        RAISE EXCEPTION 'V26 requiere V25: exercises.requires_inversion no existe';
END IF;
    IF to_regclass('flyway_schema_history') IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '25' AND success) THEN
            RAISE EXCEPTION 'V26 requiere V25 aplicada con exito segun flyway_schema_history';
END IF;
END IF;
END $$;

-- 1. ESQUEMA ----------------------------------------------------------
ALTER TABLE exercises ADD COLUMN IF NOT EXISTS movement_pattern VARCHAR(50);
ALTER TABLE exercises ADD COLUMN IF NOT EXISTS is_isolation     BOOLEAN;

COMMENT ON COLUMN exercises.movement_pattern IS
    'Patron principal de movimiento, una sola clase por ejercicio. 24 clases cerradas en reglas-patrones-v2.md. NULL solo en ejercicios inactivos.';
COMMENT ON COLUMN exercises.is_isolation IS
    'TRUE si el ejercicio mueve una sola articulacion. En tronco, movilidad, equilibrio y OTHER no aplica y se guarda FALSE (reglas-patrones-v2.md, seccion 8).';

-- 2. DATOS ------------------------------------------------------------
CREATE TEMP TABLE v26_patrones (
    source_code      VARCHAR(20) PRIMARY KEY,
    name_en          VARCHAR(255) NOT NULL,
    movement_pattern VARCHAR(50)  NOT NULL,
    is_isolation     BOOLEAN      NOT NULL
) ON COMMIT DROP;

INSERT INTO v26_patrones VALUES
                             ('3220', 'astride jumps (male)', 'LOCOMOTION_CARDIO', FALSE),   -- id 53
                             ('3672', 'back and forth step', 'KNEE_DOMINANT', FALSE),   -- id 54
                             ('1368', 'ankle circles', 'MOBILITY', FALSE),   -- id 55
                             ('3294', 'archer push up', 'HORIZONTAL_PUSH', FALSE),   -- id 56
                             ('0001', '3/4 sit-up', 'TRUNK_FLEXION', FALSE),   -- id 57
                             ('0002', '45° side bend', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 58
                             ('0003', 'air bike', 'LOCOMOTION_CARDIO', FALSE),   -- id 59
                             ('0006', 'alternate heel touchers', 'TRUNK_FLEXION', FALSE),   -- id 60
                             ('2355', 'arm slingers hanging bent knee legs', 'TRUNK_FLEXION', FALSE),   -- id 61
                             ('2333', 'arm slingers hanging straight legs', 'TRUNK_FLEXION', FALSE),   -- id 62
                             ('3204', 'arms overhead full sit-up (male)', 'TRUNK_FLEXION', FALSE),   -- id 63
                             ('3293', 'archer pull up', 'VERTICAL_PULL', FALSE),   -- id 64
                             ('3297', 'back lever', 'CORE_STABILITY', FALSE),   -- id 65
                             ('1405', 'back pec stretch', 'MOBILITY', FALSE),   -- id 66
                             ('1512', 'all fours squad stretch', 'MOBILITY', FALSE),   -- id 67
                             ('3214', 'arms apart circular toe touch (male)', 'TRUNK_FLEXION', FALSE),   -- id 68
                             ('1314', 'back extension on exercise ball', 'TRUNK_EXTENSION', FALSE),   -- id 69
                             ('0014', 'assisted motion russian twist', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 70
                             ('0007', 'alternate lateral pulldown', 'VERTICAL_PULL', FALSE),   -- id 71
                             ('0009', 'assisted chest dip (kneeling)', 'HORIZONTAL_PUSH', FALSE),   -- id 72
                             ('2364', 'assisted wide-grip chest dip (kneeling)', 'HORIZONTAL_PUSH', FALSE),   -- id 73
                             ('0015', 'assisted parallel close grip pull-up', 'VERTICAL_PULL', FALSE),   -- id 74
                             ('0017', 'assisted pull-up', 'VERTICAL_PULL', FALSE),   -- id 75
                             ('1431', 'assisted standing chin-up', 'VERTICAL_PULL', FALSE),   -- id 76
                             ('1432', 'assisted standing pull-up', 'VERTICAL_PULL', FALSE),   -- id 77
                             ('0019', 'assisted triceps dip (kneeling)', 'HORIZONTAL_PUSH', FALSE),   -- id 78
                             ('1473', 'backward jump', 'LOCOMOTION_CARDIO', FALSE),   -- id 93
                             ('0020', 'balance board', 'BALANCE_CONTROL', FALSE),   -- id 94
                             ('0994', 'band reverse wrist curl', 'WRIST_FOREARM', TRUE),   -- id 95
                             ('0999', 'band single leg calf raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 96
                             ('1000', 'band single leg reverse calf raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 97
                             ('0977', 'band front lateral raise', 'SHOULDER_RAISE', TRUE),   -- id 98
                             ('0978', 'band front raise', 'SHOULDER_RAISE', TRUE),   -- id 99
                             ('0993', 'band reverse fly', 'SHOULDER_RAISE', TRUE),   -- id 100
                             ('0997', 'band shoulder press', 'VERTICAL_PUSH', FALSE),   -- id 101
                             ('1254', 'band bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 102
                             ('0989', 'band one arm twisting chest press', 'HORIZONTAL_PUSH', FALSE),   -- id 103
                             ('0969', 'band alternating v-up', 'TRUNK_FLEXION', FALSE),   -- id 104
                             ('0971', 'band assisted wheel rollerout', 'CORE_STABILITY', FALSE),   -- id 105
                             ('0972', 'band bicycle crunch', 'TRUNK_FLEXION', FALSE),   -- id 106
                             ('0979', 'band horizontal pallof press', 'CORE_STABILITY', FALSE),   -- id 107
                             ('0981', 'band jack knife sit-up', 'TRUNK_FLEXION', FALSE),   -- id 108
                             ('0985', 'band kneeling twisting crunch', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 109
                             ('1002', 'band lying straight leg raise', 'TRUNK_FLEXION', FALSE),   -- id 110
                             ('0992', 'band push sit-up', 'TRUNK_FLEXION', FALSE),   -- id 111
                             ('1011', 'band seated twist', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 112
                             ('0970', 'band assisted pull-up', 'VERTICAL_PULL', FALSE),   -- id 113
                             ('0974', 'band close-grip pulldown', 'VERTICAL_PULL', FALSE),   -- id 114
                             ('3117', 'band fixed back close grip pulldown', 'VERTICAL_PULL', FALSE),   -- id 115
                             ('3116', 'band fixed back underhand pulldown', 'VERTICAL_PULL', FALSE),   -- id 116
                             ('0983', 'band kneeling one arm pulldown', 'VERTICAL_PULL', FALSE),   -- id 117
                             ('0988', 'band one arm standing low row', 'HORIZONTAL_PULL', FALSE),   -- id 118
                             ('0990', 'band one arm twisting seated row', 'HORIZONTAL_PULL', FALSE),   -- id 119
                             ('1018', 'band shrug', 'SCAPULAR_ELEVATION', TRUE),   -- id 120
                             ('0980', 'band bent-over hip extension', 'HIP_EXTENSION', FALSE),   -- id 121
                             ('1408', 'band hip lift', 'HIP_EXTENSION', FALSE),   -- id 122
                             ('0984', 'band lying hip internal rotation', 'OTHER', FALSE),   -- id 123
                             ('0987', 'band one arm single leg split squat', 'KNEE_DOMINANT', FALSE),   -- id 124
                             ('0991', 'band pull through', 'HIP_HINGE', FALSE),   -- id 125
                             ('0996', 'band seated hip internal rotation', 'OTHER', FALSE),   -- id 126
                             ('1001', 'band single leg split squat', 'KNEE_DOMINANT', FALSE),   -- id 127
                             ('0968', 'band alternating biceps curl', 'ELBOW_FLEXION', TRUE),   -- id 128
                             ('0975', 'band close-grip push-up', 'HORIZONTAL_PUSH', FALSE),   -- id 129
                             ('0976', 'band concentration curl', 'ELBOW_FLEXION', TRUE),   -- id 130
                             ('0986', 'band one arm overhead biceps curl', 'ELBOW_FLEXION', TRUE),   -- id 131
                             ('0998', 'band side triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 132
                             ('1016', 'band wrist curl', 'WRIST_FOREARM', TRUE),   -- id 133
                             ('1369', 'band two legs calf raise - (band under both legs) v. 2', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 134
                             ('1022', 'band standing rear delt row', 'HORIZONTAL_PULL', FALSE),   -- id 135
                             ('1012', 'band twisting overhead press', 'VERTICAL_PUSH', FALSE),   -- id 136
                             ('1017', 'band y-raise', 'SHOULDER_RAISE', TRUE),   -- id 137
                             ('1005', 'band standing crunch', 'TRUNK_FLEXION', FALSE),   -- id 138
                             ('1007', 'band standing twisting crunch', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 139
                             ('1014', 'band v-up', 'TRUNK_FLEXION', FALSE),   -- id 140
                             ('1015', 'band vertical pallof press', 'CORE_STABILITY', FALSE),   -- id 141
                             ('1010', 'band straight leg deadlift', 'HIP_HINGE', FALSE),   -- id 142
                             ('1013', 'band underhand pulldown', 'VERTICAL_PULL', FALSE),   -- id 143
                             ('1004', 'band squat', 'KNEE_DOMINANT', FALSE),   -- id 144
                             ('1003', 'band squat row', 'KNEE_DOMINANT', FALSE),   -- id 145
                             ('1008', 'band step-up', 'KNEE_DOMINANT', FALSE),   -- id 146
                             ('1009', 'band stiff leg deadlift', 'HIP_HINGE', FALSE),   -- id 147
                             ('1023', 'band straight back stiff leg deadlift', 'HIP_HINGE', FALSE),   -- id 148
                             ('1370', 'barbell floor calf raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 149
                             ('0041', 'barbell front raise', 'SHOULDER_RAISE', TRUE),   -- id 150
                             ('0025', 'barbell bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 151
                             ('0033', 'barbell decline bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 152
                             ('1255', 'barbell decline pullover', 'SHOULDER_EXTENSION', TRUE),   -- id 153
                             ('0036', 'barbell decline wide-grip press', 'HORIZONTAL_PUSH', FALSE),   -- id 154
                             ('0040', 'barbell front raise and pullover', 'SHOULDER_EXTENSION', TRUE),   -- id 155
                             ('1316', 'barbell bent arm pullover', 'SHOULDER_EXTENSION', TRUE),   -- id 156
                             ('0027', 'barbell bent over row', 'HORIZONTAL_PULL', FALSE),   -- id 157
                             ('0034', 'barbell decline bent arm pullover', 'SHOULDER_EXTENSION', TRUE),   -- id 158
                             ('0037', 'barbell decline wide-grip pullover', 'SHOULDER_EXTENSION', TRUE),   -- id 159
                             ('0024', 'barbell bench front squat', 'KNEE_DOMINANT', FALSE),   -- id 160
                             ('0026', 'barbell bench squat', 'KNEE_DOMINANT', FALSE),   -- id 161
                             ('0028', 'barbell clean and press', 'VERTICAL_PUSH', FALSE),   -- id 162
                             ('0029', 'barbell clean-grip front squat', 'KNEE_DOMINANT', FALSE),   -- id 163
                             ('0032', 'barbell deadlift', 'HIP_HINGE', FALSE),   -- id 164
                             ('0039', 'barbell front chest squat', 'KNEE_DOMINANT', FALSE),   -- id 165
                             ('0042', 'barbell front squat', 'KNEE_DOMINANT', FALSE),   -- id 166
                             ('0023', 'barbell alternate biceps curl', 'ELBOW_FLEXION', TRUE),   -- id 167
                             ('2407', 'barbell biceps curl (with arm blaster)', 'ELBOW_FLEXION', TRUE),   -- id 168
                             ('0030', 'barbell close-grip bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 169
                             ('0031', 'barbell curl', 'ELBOW_FLEXION', TRUE),   -- id 170
                             ('0035', 'barbell decline close grip to skull press', 'ELBOW_EXTENSION', TRUE),   -- id 171
                             ('0038', 'barbell drag curl', 'ELBOW_FLEXION', TRUE),   -- id 172
                             ('1411', 'barbell palms down wrist curl over a bench', 'WRIST_FOREARM', TRUE),   -- id 173
                             ('1412', 'barbell palms up wrist curl over a bench', 'WRIST_FOREARM', TRUE),   -- id 174
                             ('0067', 'barbell one arm snatch', 'VERTICAL_PUSH', FALSE),   -- id 175
                             ('0045', 'barbell guillotine bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 176
                             ('0047', 'barbell incline bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 177
                             ('0050', 'barbell incline shoulder raise', 'SHOULDER_RAISE', TRUE),   -- id 178
                             ('0049', 'barbell incline row', 'HORIZONTAL_PULL', FALSE),   -- id 179
                             ('0064', 'barbell one arm bent over row', 'HORIZONTAL_PULL', FALSE),   -- id 180
                             ('3017', 'barbell pendlay row', 'HORIZONTAL_PULL', FALSE),   -- id 181
                             ('0043', 'barbell full squat', 'KNEE_DOMINANT', FALSE),   -- id 182
                             ('1461', 'barbell full squat (back pov)', 'KNEE_DOMINANT', FALSE),   -- id 183
                             ('1462', 'barbell full squat (side pov)', 'KNEE_DOMINANT', FALSE),   -- id 184
                             ('1545', 'barbell full zercher squat', 'KNEE_DOMINANT', FALSE),   -- id 185
                             ('1409', 'barbell glute bridge', 'HIP_EXTENSION', FALSE),   -- id 186
                             ('3562', 'barbell glute bridge two legs on bench (male)', 'HIP_EXTENSION', FALSE),   -- id 187
                             ('0044', 'barbell good morning', 'HIP_HINGE', FALSE),   -- id 188
                             ('0046', 'barbell hack squat', 'KNEE_DOMINANT', FALSE),   -- id 189
                             ('1436', 'barbell high bar squat', 'KNEE_DOMINANT', FALSE),   -- id 190
                             ('0051', 'barbell jefferson squat', 'KNEE_DOMINANT', FALSE),   -- id 191
                             ('0053', 'barbell jump squat', 'KNEE_DOMINANT', FALSE),   -- id 192
                             ('1410', 'barbell lateral lunge', 'KNEE_DOMINANT', FALSE),   -- id 193
                             ('1435', 'barbell low bar squat', 'KNEE_DOMINANT', FALSE),   -- id 194
                             ('0054', 'barbell lunge', 'KNEE_DOMINANT', FALSE),   -- id 195
                             ('0058', 'barbell lying lifting (on hip)', 'HIP_EXTENSION', FALSE),   -- id 196
                             ('0063', 'barbell narrow stance squat', 'KNEE_DOMINANT', FALSE),   -- id 197
                             ('0066', 'barbell one arm side deadlift', 'HIP_HINGE', FALSE),   -- id 198
                             ('0068', 'barbell one leg squat', 'KNEE_DOMINANT', FALSE),   -- id 199
                             ('0069', 'barbell overhead squat', 'KNEE_DOMINANT', FALSE),   -- id 200
                             ('1719', 'barbell incline close grip bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 201
                             ('0048', 'barbell incline reverse-grip press', 'HORIZONTAL_PUSH', FALSE),   -- id 202
                             ('0052', 'barbell jm bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 203
                             ('1720', 'barbell lying back of the head tricep extension', 'ELBOW_EXTENSION', TRUE),   -- id 204
                             ('0055', 'barbell lying close-grip press', 'HORIZONTAL_PUSH', FALSE),   -- id 205
                             ('0056', 'barbell lying close-grip triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 206
                             ('0057', 'barbell lying extension', 'ELBOW_EXTENSION', TRUE),   -- id 207
                             ('0059', 'barbell lying preacher curl', 'ELBOW_FLEXION', TRUE),   -- id 208
                             ('0061', 'barbell lying triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 209
                             ('0060', 'barbell lying triceps extension skull crusher', 'ELBOW_EXTENSION', TRUE),   -- id 210
                             ('0065', 'barbell one arm floor press', 'HORIZONTAL_PUSH', FALSE),   -- id 211
                             ('1751', 'barbell pin presses', 'HORIZONTAL_PUSH', FALSE),   -- id 212
                             ('0079', 'barbell revers wrist curl v. 2', 'WRIST_FOREARM', TRUE),   -- id 213
                             ('0082', 'barbell reverse wrist curl', 'WRIST_FOREARM', TRUE),   -- id 214
                             ('0088', 'barbell seated calf raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 215
                             ('1371', 'barbell seated calf raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 216
                             ('0075', 'barbell rear delt raise', 'SHOULDER_RAISE', TRUE),   -- id 217
                             ('0076', 'barbell rear delt row', 'HORIZONTAL_PULL', FALSE),   -- id 218
                             ('0086', 'barbell seated behind head military press', 'VERTICAL_PUSH', FALSE),   -- id 219
                             ('0087', 'barbell seated bradford rocky press', 'VERTICAL_PUSH', FALSE),   -- id 220
                             ('0091', 'barbell seated overhead press', 'VERTICAL_PUSH', FALSE),   -- id 221
                             ('1256', 'barbell reverse grip decline bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 222
                             ('1257', 'barbell reverse grip incline bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 223
                             ('0071', 'barbell press sit-up', 'TRUNK_FLEXION', FALSE),   -- id 224
                             ('0084', 'barbell rollerout', 'CORE_STABILITY', FALSE),   -- id 225
                             ('0083', 'barbell rollerout from bench', 'CORE_STABILITY', FALSE),   -- id 226
                             ('0094', 'barbell seated twist', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 227
                             ('0096', 'barbell side bent v. 2', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 228
                             ('2799', 'barbell sitted alternate leg raise', 'TRUNK_FLEXION', FALSE),   -- id 229
                             ('0073', 'barbell pullover', 'SHOULDER_EXTENSION', TRUE),   -- id 230
                             ('0022', 'barbell pullover to press', 'VERTICAL_PUSH', FALSE),   -- id 231
                             ('0118', 'barbell reverse grip bent over row', 'HORIZONTAL_PULL', FALSE),   -- id 232
                             ('1317', 'barbell reverse grip incline bench row', 'HORIZONTAL_PULL', FALSE),   -- id 233
                             ('0095', 'barbell shrug', 'SCAPULAR_ELEVATION', TRUE),   -- id 234
                             ('0074', 'barbell rack pull', 'HIP_HINGE', FALSE),   -- id 235
                             ('0078', 'barbell rear lunge', 'KNEE_DOMINANT', FALSE),   -- id 236
                             ('0077', 'barbell rear lunge v. 2', 'KNEE_DOMINANT', FALSE),   -- id 237
                             ('0085', 'barbell romanian deadlift', 'HIP_HINGE', FALSE),   -- id 238
                             ('0090', 'barbell seated good morning', 'HIP_HINGE', FALSE),   -- id 239
                             ('0098', 'barbell side split squat', 'KNEE_DOMINANT', FALSE),   -- id 240
                             ('0097', 'barbell side split squat v. 2', 'KNEE_DOMINANT', FALSE),   -- id 241
                             ('1756', 'barbell single leg deadlift', 'HIP_HINGE', FALSE),   -- id 242
                             ('0099', 'barbell single leg split squat', 'KNEE_DOMINANT', FALSE),   -- id 243
                             ('0070', 'barbell preacher curl', 'ELBOW_FLEXION', TRUE),   -- id 244
                             ('0072', 'barbell prone incline curl', 'ELBOW_FLEXION', TRUE),   -- id 245
                             ('2187', 'barbell reverse close-grip bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 246
                             ('0080', 'barbell reverse curl', 'ELBOW_FLEXION', TRUE),   -- id 247
                             ('1721', 'barbell reverse grip skullcrusher', 'ELBOW_EXTENSION', TRUE),   -- id 248
                             ('0081', 'barbell reverse preacher curl', 'ELBOW_FLEXION', TRUE),   -- id 249
                             ('1718', 'barbell seated close grip behind neck triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 250
                             ('0089', 'barbell seated close-grip concentration curl', 'ELBOW_FLEXION', TRUE),   -- id 251
                             ('0092', 'barbell seated overhead triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 252
                             ('3360', 'bear crawl', 'LOCOMOTION_CARDIO', FALSE),   -- id 253
                             ('3212', 'basic toe touch (male)', 'TRUNK_FLEXION', FALSE),   -- id 254
                             ('0104', 'barbell standing back wrist curl', 'WRIST_FOREARM', TRUE),   -- id 256
                             ('0126', 'barbell wrist curl', 'WRIST_FOREARM', TRUE),   -- id 257
                             ('0125', 'barbell wrist curl v. 2', 'WRIST_FOREARM', TRUE),   -- id 258
                             ('1372', 'barbell standing calf raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 259
                             ('0108', 'barbell standing leg calf raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 260
                             ('0111', 'barbell standing rocking leg calf raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 261
                             ('0100', 'barbell skier', 'HIP_HINGE', FALSE),   -- id 262
                             ('0105', 'barbell standing bradford press', 'VERTICAL_PUSH', FALSE),   -- id 263
                             ('1456', 'barbell standing close grip military press', 'VERTICAL_PUSH', FALSE),   -- id 264
                             ('0107', 'barbell standing front raise over head', 'SHOULDER_RAISE', TRUE),   -- id 265
                             ('1457', 'barbell standing wide military press', 'VERTICAL_PUSH', FALSE),   -- id 266
                             ('3305', 'barbell thruster', 'KNEE_DOMINANT', FALSE),   -- id 267
                             ('0120', 'barbell upright row', 'HORIZONTAL_PULL', FALSE),   -- id 268
                             ('0119', 'barbell upright row v. 2', 'HORIZONTAL_PULL', FALSE),   -- id 269
                             ('0121', 'barbell upright row v. 3', 'HORIZONTAL_PULL', FALSE),   -- id 270
                             ('0123', 'barbell wide-grip upright row', 'HORIZONTAL_PULL', FALSE),   -- id 271
                             ('0122', 'barbell wide bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 272
                             ('1258', 'barbell wide reverse grip bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 273
                             ('2800', 'barbell sitted alternate leg raise (female)', 'TRUNK_FLEXION', FALSE),   -- id 274
                             ('0103', 'barbell standing ab rollerout', 'CORE_STABILITY', FALSE),   -- id 275
                             ('0112', 'barbell standing twist', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 276
                             ('0101', 'barbell speed squat', 'KNEE_DOMINANT', FALSE),   -- id 277
                             ('2810', 'barbell split squat v. 2', 'KNEE_DOMINANT', FALSE),   -- id 278
                             ('0102', 'barbell squat (on knees)', 'KNEE_DOMINANT', FALSE),   -- id 279
                             ('2798', 'barbell squat jump step rear lunge', 'KNEE_DOMINANT', FALSE),   -- id 280
                             ('0114', 'barbell step-up', 'KNEE_DOMINANT', FALSE),   -- id 281
                             ('0115', 'barbell stiff leg good morning', 'HIP_HINGE', FALSE),   -- id 282
                             ('0116', 'barbell straight leg deadlift', 'HIP_HINGE', FALSE),   -- id 283
                             ('0117', 'barbell sumo deadlift', 'HIP_HINGE', FALSE),   -- id 284
                             ('0124', 'barbell wide squat', 'KNEE_DOMINANT', FALSE),   -- id 285
                             ('0127', 'barbell zercher squat', 'KNEE_DOMINANT', FALSE),   -- id 286
                             ('0106', 'barbell standing close grip curl', 'ELBOW_FLEXION', TRUE),   -- id 287
                             ('2414', 'barbell standing concentration curl', 'ELBOW_FLEXION', TRUE),   -- id 288
                             ('0109', 'barbell standing overhead triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 289
                             ('0110', 'barbell standing reverse grip curl', 'ELBOW_FLEXION', TRUE),   -- id 290
                             ('1629', 'barbell standing wide grip biceps curl', 'ELBOW_FLEXION', TRUE),   -- id 291
                             ('0113', 'barbell standing wide-grip curl', 'ELBOW_FLEXION', TRUE),   -- id 292
                             ('1160', 'burpee', 'LOCOMOTION_CARDIO', FALSE),   -- id 293
                             ('1373', 'bodyweight standing calf raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 294
                             ('1374', 'box jump down with one leg stabilization', 'BALANCE_CONTROL', FALSE),   -- id 295
                             ('3544', 'bodyweight incline side plank', 'CORE_STABILITY', FALSE),   -- id 296
                             ('0138', 'bottoms-up', 'TRUNK_FLEXION', FALSE),   -- id 297
                             ('2466', 'bridge - mountain climber (cross body)', 'LOCOMOTION_CARDIO', FALSE),   -- id 298
                             ('0870', 'butt-ups', 'TRUNK_FLEXION', FALSE),   -- id 299
                             ('3019', 'bench pull-ups', 'HORIZONTAL_PULL', FALSE),   -- id 300
                             ('3168', 'bodyweight squatting row', 'KNEE_DOMINANT', FALSE),   -- id 301
                             ('3167', 'bodyweight squatting row (with towel)', 'KNEE_DOMINANT', FALSE),   -- id 302
                             ('3156', 'bodyweight standing close-grip one arm row', 'HORIZONTAL_PULL', FALSE),   -- id 303
                             ('3158', 'bodyweight standing close-grip row', 'HORIZONTAL_PULL', FALSE),   -- id 304
                             ('3162', 'bodyweight standing one arm row', 'HORIZONTAL_PULL', FALSE),   -- id 305
                             ('3161', 'bodyweight standing one arm row (with towel)', 'HORIZONTAL_PULL', FALSE),   -- id 306
                             ('3166', 'bodyweight standing row', 'HORIZONTAL_PULL', FALSE),   -- id 307
                             ('3165', 'bodyweight standing row (with towel)', 'HORIZONTAL_PULL', FALSE),   -- id 308
                             ('0130', 'bench hip extension', 'HIP_EXTENSION', FALSE),   -- id 309
                             ('3639', 'bent knee lying twist (male)', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 310
                             ('3543', 'bodyweight drop jump squat', 'KNEE_DOMINANT', FALSE),   -- id 311
                             ('1494', 'butterfly yoga pose', 'MOBILITY', FALSE),   -- id 312
                             ('0129', 'bench dip (knees bent)', 'HORIZONTAL_PUSH', FALSE),   -- id 313
                             ('1399', 'bench dip on floor', 'HORIZONTAL_PUSH', FALSE),   -- id 314
                             ('1770', 'biceps leg concentration curl', 'ELBOW_FLEXION', TRUE),   -- id 315
                             ('0139', 'biceps narrow pull-ups', 'VERTICAL_PULL', FALSE),   -- id 316
                             ('0140', 'biceps pull-up', 'VERTICAL_PULL', FALSE),   -- id 317
                             ('0137', 'body-up', 'HORIZONTAL_PUSH', FALSE),   -- id 318
                             ('1771', 'bodyweight kneeling triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 319
                             ('1769', 'bodyweight side lying biceps curl', 'ELBOW_FLEXION', TRUE),   -- id 320
                             ('0148', 'cable alternate shoulder press', 'VERTICAL_PUSH', FALSE),   -- id 321
                             ('0154', 'cable cross-over revers fly', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 322
                             ('0151', 'cable bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 323
                             ('0155', 'cable cross-over variation', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 324
                             ('0150', 'cable bar lateral pulldown', 'VERTICAL_PULL', FALSE),   -- id 325
                             ('0153', 'cable cross-over lateral pulldown', 'VERTICAL_PULL', FALSE),   -- id 326
                             ('3235', 'cable assisted inverse leg curl', 'KNEE_DOMINANT', TRUE),   -- id 327
                             ('0149', 'cable alternate triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 328
                             ('1630', 'cable close grip curl', 'ELBOW_FLEXION', TRUE),   -- id 329
                             ('1631', 'cable concentration curl', 'ELBOW_FLEXION', TRUE),   -- id 330
                             ('0152', 'cable concentration extension (on knee)', 'ELBOW_EXTENSION', TRUE),   -- id 331
                             ('0161', 'cable forward raise', 'SHOULDER_RAISE', TRUE),   -- id 333
                             ('0162', 'cable front raise', 'SHOULDER_RAISE', TRUE),   -- id 334
                             ('0164', 'cable front shoulder raise', 'SHOULDER_RAISE', TRUE),   -- id 335
                             ('3697', 'cable kneeling rear delt row (with rope) (male)', 'HORIZONTAL_PULL', FALSE),   -- id 336
                             ('0178', 'cable lateral raise', 'SHOULDER_RAISE', TRUE),   -- id 337
                             ('0158', 'cable decline fly', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 338
                             ('1260', 'cable decline one arm press', 'HORIZONTAL_PUSH', FALSE),   -- id 339
                             ('1261', 'cable decline press', 'HORIZONTAL_PUSH', FALSE),   -- id 340
                             ('0169', 'cable incline bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 341
                             ('0171', 'cable incline fly', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 342
                             ('0170', 'cable incline fly (on stability ball)', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 343
                             ('0179', 'cable low fly', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 344
                             ('0185', 'cable lying fly', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 345
                             ('0188', 'cable middle fly', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 346
                             ('0174', 'cable judo flip', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 347
                             ('0175', 'cable kneeling crunch', 'TRUNK_FLEXION', FALSE),   -- id 348
                             ('0159', 'cable decline seated wide-grip row', 'HORIZONTAL_PULL', FALSE),   -- id 349
                             ('0160', 'cable floor seated wide-grip row', 'HORIZONTAL_PULL', FALSE),   -- id 350
                             ('0167', 'cable high row (kneeling)', 'HORIZONTAL_PULL', FALSE),   -- id 351
                             ('1318', 'cable incline bench row', 'HORIZONTAL_PULL', FALSE),   -- id 352
                             ('0172', 'cable incline pushdown', 'ELBOW_EXTENSION', TRUE),   -- id 353
                             ('2330', 'cable lat pulldown full range of motion', 'VERTICAL_PULL', FALSE),   -- id 354
                             ('0177', 'cable lateral pulldown (with rope attachment)', 'VERTICAL_PULL', FALSE),   -- id 355
                             ('2616', 'cable lateral pulldown with v-bar', 'VERTICAL_PULL', FALSE),   -- id 356
                             ('0180', 'cable low seated row', 'HORIZONTAL_PULL', FALSE),   -- id 357
                             ('0184', 'cable lying extension pullover (with rope attachment)', 'SHOULDER_EXTENSION', TRUE),   -- id 358
                             ('0189', 'cable one arm bent over row', 'HORIZONTAL_PULL', FALSE),   -- id 359
                             ('0157', 'cable deadlift', 'HIP_HINGE', FALSE),   -- id 360
                             ('0168', 'cable hip adduction', 'HIP_ABDUCTION_ADDUCTION', TRUE),   -- id 361
                             ('0868', 'cable curl', 'ELBOW_FLEXION', TRUE),   -- id 362
                             ('1632', 'cable drag curl', 'ELBOW_FLEXION', TRUE),   -- id 363
                             ('0165', 'cable hammer curl (with rope)', 'ELBOW_FLEXION', TRUE),   -- id 364
                             ('1722', 'cable high pulley overhead tricep extension', 'ELBOW_EXTENSION', TRUE),   -- id 365
                             ('0173', 'cable incline triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 366
                             ('0860', 'cable kickback', 'ELBOW_EXTENSION', TRUE),   -- id 367
                             ('0176', 'cable kneeling triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 368
                             ('1634', 'cable lying bicep curl', 'ELBOW_FLEXION', TRUE),   -- id 369
                             ('0182', 'cable lying close-grip curl', 'ELBOW_FLEXION', TRUE),   -- id 370
                             ('0186', 'cable lying triceps extension v. 2', 'ELBOW_EXTENSION', TRUE),   -- id 371
                             ('0190', 'cable one arm curl', 'ELBOW_FLEXION', TRUE),   -- id 372
                             ('0210', 'cable reverse wrist curl', 'WRIST_FOREARM', TRUE),   -- id 373
                             ('0192', 'cable one arm lateral raise', 'SHOULDER_RAISE', TRUE),   -- id 374
                             ('0202', 'cable rear delt row (stirrups)', 'HORIZONTAL_PULL', FALSE),   -- id 375
                             ('0203', 'cable rear delt row (with rope)', 'HORIZONTAL_PULL', FALSE),   -- id 376
                             ('1262', 'cable one arm decline chest fly', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 377
                             ('1263', 'cable one arm fly on exercise ball', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 378
                             ('1264', 'cable one arm incline fly on exercise ball', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 379
                             ('1265', 'cable one arm incline press', 'HORIZONTAL_PUSH', FALSE),   -- id 380
                             ('1266', 'cable one arm incline press on exercise ball', 'HORIZONTAL_PUSH', FALSE),   -- id 381
                             ('0191', 'cable one arm lateral bent-over', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 382
                             ('1267', 'cable one arm press on exercise ball', 'HORIZONTAL_PUSH', FALSE),   -- id 383
                             ('1268', 'cable press on exercise ball', 'HORIZONTAL_PUSH', FALSE),   -- id 384
                             ('0873', 'cable reverse crunch', 'TRUNK_FLEXION', FALSE),   -- id 385
                             ('3563', 'cable one arm pulldown', 'VERTICAL_PULL', FALSE),   -- id 386
                             ('0193', 'cable one arm straight back high row (kneeling)', 'HORIZONTAL_PULL', FALSE),   -- id 387
                             ('1319', 'cable palm rotational row', 'HORIZONTAL_PULL', FALSE),   -- id 388
                             ('0198', 'cable pulldown', 'VERTICAL_PULL', FALSE),   -- id 389
                             ('0197', 'cable pulldown (pro lat bar)', 'VERTICAL_PULL', FALSE),   -- id 390
                             ('0199', 'cable pushdown (straight arm) v. 2', 'ELBOW_EXTENSION', TRUE),   -- id 391
                             ('0205', 'cable rear pulldown', 'VERTICAL_PULL', FALSE),   -- id 392
                             ('0208', 'cable reverse-grip straight back seated high row', 'HORIZONTAL_PULL', FALSE),   -- id 393
                             ('1320', 'cable rope crossover seated row', 'HORIZONTAL_PULL', FALSE),   -- id 394
                             ('1321', 'cable rope elevated seated row', 'HORIZONTAL_PULL', FALSE),   -- id 395
                             ('0196', 'cable pull through (with rope)', 'HIP_HINGE', FALSE),   -- id 396
                             ('1633', 'cable one arm preacher curl', 'ELBOW_FLEXION', TRUE),   -- id 397
                             ('1635', 'cable one arm reverse preacher curl', 'ELBOW_FLEXION', TRUE),   -- id 398
                             ('1723', 'cable one arm tricep pushdown', 'ELBOW_EXTENSION', TRUE),   -- id 399
                             ('1636', 'cable overhead curl', 'ELBOW_FLEXION', TRUE),   -- id 400
                             ('1637', 'cable overhead curl on exercise ball', 'ELBOW_FLEXION', TRUE),   -- id 401
                             ('0194', 'cable overhead triceps extension (rope attachment)', 'ELBOW_EXTENSION', TRUE),   -- id 402
                             ('0195', 'cable preacher curl', 'ELBOW_FLEXION', TRUE),   -- id 403
                             ('1638', 'cable pulldown bicep curl', 'VERTICAL_PULL', FALSE),   -- id 404
                             ('0201', 'cable pushdown', 'ELBOW_EXTENSION', TRUE),   -- id 405
                             ('0200', 'cable pushdown (with rope attachment)', 'ELBOW_EXTENSION', TRUE),   -- id 406
                             ('0204', 'cable rear drive', 'SHOULDER_EXTENSION', TRUE),   -- id 407
                             ('0206', 'cable reverse curl', 'ELBOW_FLEXION', TRUE),   -- id 408
                             ('2406', 'cable reverse grip triceps pushdown (sz-bar) (with arm blaster)', 'ELBOW_EXTENSION', TRUE),   -- id 409
                             ('1413', 'cable reverse one arm curl', 'ELBOW_FLEXION', TRUE),   -- id 410
                             ('0209', 'cable reverse preacher curl', 'ELBOW_FLEXION', TRUE),   -- id 411
                             ('0207', 'cable reverse-grip pushdown', 'ELBOW_EXTENSION', TRUE),   -- id 412
                             ('0224', 'cable standing back wrist curl', 'WRIST_FOREARM', TRUE),   -- id 413
                             ('1375', 'cable standing calf raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 414
                             ('1376', 'cable standing one leg calf raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 415
                             ('0215', 'cable seated rear lateral raise', 'SHOULDER_RAISE', TRUE),   -- id 416
                             ('0216', 'cable seated shoulder internal rotation', 'OTHER', FALSE),   -- id 417
                             ('0219', 'cable shoulder press', 'VERTICAL_PUSH', FALSE),   -- id 418
                             ('0225', 'cable standing cross-over high reverse fly', 'SHOULDER_RAISE', TRUE),   -- id 419
                             ('0233', 'cable standing rear delt row (with rope)', 'HORIZONTAL_PULL', FALSE),   -- id 420
                             ('2144', 'cable seated chest press', 'HORIZONTAL_PUSH', FALSE),   -- id 421
                             ('0227', 'cable standing fly', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 422
                             ('0211', 'cable russian twists (on stability ball)', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 423
                             ('0212', 'cable seated crunch', 'TRUNK_FLEXION', FALSE),   -- id 424
                             ('2399', 'cable seated twist', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 425
                             ('0222', 'cable side bend', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 426
                             ('0221', 'cable side bend crunch (bosu ball)', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 427
                             ('0223', 'cable side crunch', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 428
                             ('0226', 'cable standing crunch', 'TRUNK_FLEXION', FALSE),   -- id 429
                             ('0874', 'cable standing crunch (with rope attachment)', 'TRUNK_FLEXION', FALSE),   -- id 430
                             ('0230', 'cable standing lift', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 431
                             ('1322', 'cable rope extension incline bench row', 'HORIZONTAL_PULL', FALSE),   -- id 432
                             ('1323', 'cable rope seated row', 'HORIZONTAL_PULL', FALSE),   -- id 433
                             ('0213', 'cable seated high row (v-bar)', 'HORIZONTAL_PULL', FALSE),   -- id 434
                             ('0214', 'cable seated one arm alternate row', 'HORIZONTAL_PULL', FALSE),   -- id 435
                             ('0861', 'cable seated row', 'HORIZONTAL_PULL', FALSE),   -- id 436
                             ('0218', 'cable seated wide-grip row', 'HORIZONTAL_PULL', FALSE),   -- id 437
                             ('0220', 'cable shrug', 'SCAPULAR_ELEVATION', TRUE),   -- id 438
                             ('1717', 'cable squat row (with rope attachment)', 'KNEE_DOMINANT', FALSE),   -- id 439
                             ('0228', 'cable standing hip extension', 'HIP_EXTENSION', FALSE),   -- id 440
                             ('1639', 'cable rope hammer preacher curl', 'ELBOW_FLEXION', TRUE),   -- id 441
                             ('1724', 'cable rope high pulley overhead tricep extension', 'ELBOW_EXTENSION', TRUE),   -- id 442
                             ('1725', 'cable rope incline tricep extension', 'ELBOW_EXTENSION', TRUE),   -- id 443
                             ('1726', 'cable rope lying on floor tricep extension', 'ELBOW_EXTENSION', TRUE),   -- id 444
                             ('1640', 'cable rope one arm hammer preacher curl', 'ELBOW_FLEXION', TRUE),   -- id 445
                             ('1641', 'cable seated curl', 'ELBOW_FLEXION', TRUE),   -- id 446
                             ('1642', 'cable seated one arm concentration curl', 'ELBOW_FLEXION', TRUE),   -- id 447
                             ('1643', 'cable seated overhead curl', 'ELBOW_FLEXION', TRUE),   -- id 448
                             ('1644', 'cable squatting curl', 'KNEE_DOMINANT', FALSE),   -- id 449
                             ('0229', 'cable standing inner curl', 'ELBOW_FLEXION', TRUE),   -- id 450
                             ('0231', 'cable standing one arm triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 451
                             ('0232', 'cable standing pulldown (with rope)', 'VERTICAL_PULL', FALSE),   -- id 452
                             ('1407', 'calf push stretch with hands against wall', 'MOBILITY', FALSE),   -- id 453
                             ('1377', 'calf stretch with hands against wall', 'MOBILITY', FALSE),   -- id 454
                             ('0257', 'circles knee stretch', 'MOBILITY', FALSE),   -- id 455
                             ('1271', 'chest and front of shoulder stretch', 'MOBILITY', FALSE),   -- id 456
                             ('0251', 'chest dip', 'HORIZONTAL_PUSH', FALSE),   -- id 457
                             ('1430', 'chest dip (on dip-pull-up cage)', 'HORIZONTAL_PUSH', FALSE),   -- id 458
                             ('2462', 'chest dip on straight bar', 'HORIZONTAL_PUSH', FALSE),   -- id 459
                             ('3216', 'chest tap push-up (male)', 'HORIZONTAL_PUSH', FALSE),   -- id 460
                             ('1273', 'clap push up', 'HORIZONTAL_PUSH', FALSE),   -- id 461
                             ('2963', 'captains chair straight leg raise', 'TRUNK_FLEXION', FALSE),   -- id 462
                             ('1326', 'chin-up', 'VERTICAL_PULL', FALSE),   -- id 463
                             ('0253', 'chin-ups (narrow parallel grip)', 'VERTICAL_PULL', FALSE),   -- id 464
                             ('1548', 'chair leg extended stretch', 'MOBILITY', FALSE),   -- id 465
                             ('1272', 'chest stretch with exercise ball', 'MOBILITY', FALSE),   -- id 466
                             ('0248', 'cambered bar lying row', 'HORIZONTAL_PULL', FALSE),   -- id 468
                             ('0247', 'cable wrist curl', 'WRIST_FOREARM', TRUE),   -- id 469
                             ('0235', 'cable standing shoulder external rotation', 'OTHER', FALSE),   -- id 470
                             ('0240', 'cable supine reverse fly', 'SHOULDER_RAISE', TRUE),   -- id 471
                             ('0246', 'cable upright row', 'HORIZONTAL_PULL', FALSE),   -- id 472
                             ('1269', 'cable standing up straight crossovers', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 473
                             ('1270', 'cable upper chest crossovers', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 474
                             ('0242', 'cable tuck reverse crunch', 'TRUNK_FLEXION', FALSE),   -- id 475
                             ('0243', 'cable twist', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 476
                             ('0862', 'cable twist (up-down)', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 477
                             ('0234', 'cable standing row (v-bar)', 'HORIZONTAL_PULL', FALSE),   -- id 478
                             ('0236', 'cable standing twist row (v-bar)', 'HORIZONTAL_PULL', FALSE),   -- id 479
                             ('0238', 'cable straight arm pulldown', 'SHOULDER_EXTENSION', TRUE),   -- id 480
                             ('0237', 'cable straight arm pulldown (with rope)', 'SHOULDER_EXTENSION', TRUE),   -- id 481
                             ('0239', 'cable straight back seated row', 'HORIZONTAL_PULL', FALSE),   -- id 482
                             ('2464', 'cable thibaudeau kayak row', 'HORIZONTAL_PULL', FALSE),   -- id 483
                             ('0244', 'cable twisting pull', 'HORIZONTAL_PULL', FALSE),   -- id 484
                             ('0245', 'cable underhand pulldown', 'VERTICAL_PULL', FALSE),   -- id 485
                             ('1324', 'cable upper row', 'HORIZONTAL_PULL', FALSE),   -- id 486
                             ('1325', 'cable wide grip rear pulldown behind neck', 'VERTICAL_PULL', FALSE),   -- id 487
                             ('1727', 'cable standing reverse grip one arm overhead tricep extension', 'ELBOW_EXTENSION', TRUE),   -- id 488
                             ('0241', 'cable triceps pushdown (v-bar)', 'ELBOW_EXTENSION', TRUE),   -- id 489
                             ('2405', 'cable triceps pushdown (v-bar) (with arm blaster)', 'ELBOW_EXTENSION', TRUE),   -- id 490
                             ('1645', 'cable two arm curl on incline bench', 'ELBOW_FLEXION', TRUE),   -- id 491
                             ('1728', 'cable two arm tricep kickback', 'ELBOW_EXTENSION', TRUE),   -- id 492
                             ('0284', 'donkey calf raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 493
                             ('0258', 'clock push-up', 'HORIZONTAL_PUSH', FALSE),   -- id 494
                             ('0279', 'decline push-up', 'HORIZONTAL_PUSH', FALSE),   -- id 495
                             ('1275', 'drop push up', 'HORIZONTAL_PUSH', FALSE),   -- id 496
                             ('0260', 'cocoons', 'TRUNK_FLEXION', FALSE),   -- id 497
                             ('1468', 'crab twist toe touch', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 498
                             ('0262', 'cross body crunch', 'TRUNK_FLEXION', FALSE),   -- id 499
                             ('0267', 'crunch (hands overhead)', 'TRUNK_FLEXION', FALSE),   -- id 500
                             ('0274', 'crunch floor', 'TRUNK_FLEXION', FALSE),   -- id 501
                             ('3016', 'curl-up', 'ELBOW_FLEXION', TRUE),   -- id 502
                             ('0276', 'dead bug', 'CORE_STABILITY', FALSE),   -- id 503
                             ('0277', 'decline crunch', 'TRUNK_FLEXION', FALSE),   -- id 504
                             ('0282', 'decline sit-up', 'TRUNK_FLEXION', FALSE),   -- id 505
                             ('1327', 'close grip chin-up', 'VERTICAL_PULL', FALSE),   -- id 506
                             ('3769', 'curtsey squat', 'KNEE_DOMINANT', FALSE),   -- id 507
                             ('0259', 'close-grip push-up', 'HORIZONTAL_PUSH', FALSE),   -- id 508
                             ('2398', 'close-grip push-up (on knees)', 'HORIZONTAL_PUSH', FALSE),   -- id 509
                             ('0283', 'diamond push-up', 'HORIZONTAL_PUSH', FALSE),   -- id 510
                             ('0286', 'dumbbell alternate side press', 'VERTICAL_PUSH', FALSE),   -- id 511
                             ('2137', 'dumbbell arnold press', 'VERTICAL_PUSH', FALSE),   -- id 512
                             ('0287', 'dumbbell arnold press v. 2', 'VERTICAL_PUSH', FALSE),   -- id 513
                             ('0290', 'dumbbell bench seated press', 'VERTICAL_PUSH', FALSE),   -- id 514
                             ('1274', 'deep push up', 'HORIZONTAL_PUSH', FALSE),   -- id 515
                             ('0288', 'dumbbell around pullover', 'SHOULDER_EXTENSION', TRUE),   -- id 516
                             ('0289', 'dumbbell bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 517
                             ('0293', 'dumbbell bent over row', 'HORIZONTAL_PULL', FALSE),   -- id 518
                             ('0291', 'dumbbell bench squat', 'KNEE_DOMINANT', FALSE),   -- id 519
                             ('0285', 'dumbbell alternate biceps curl', 'ELBOW_FLEXION', TRUE),   -- id 520
                             ('2403', 'dumbbell alternate biceps curl (with arm blaster)', 'ELBOW_FLEXION', TRUE),   -- id 521
                             ('1646', 'dumbbell alternate hammer preacher curl', 'ELBOW_FLEXION', TRUE),   -- id 522
                             ('1647', 'dumbbell alternate preacher curl', 'ELBOW_FLEXION', TRUE),   -- id 523
                             ('1648', 'dumbbell alternate seated hammer curl', 'ELBOW_FLEXION', TRUE),   -- id 524
                             ('1649', 'dumbbell alternating bicep curl with leg raised on exercise ball', 'ELBOW_FLEXION', TRUE),   -- id 525
                             ('1650', 'dumbbell alternating seated bicep curl on exercise ball', 'ELBOW_FLEXION', TRUE),   -- id 526
                             ('1651', 'dumbbell bicep curl lunge with bowling motion', 'KNEE_DOMINANT', FALSE),   -- id 527
                             ('1652', 'dumbbell bicep curl on exercise ball with leg raised', 'TRUNK_FLEXION', FALSE),   -- id 528
                             ('1653', 'dumbbell bicep curl with stork stance', 'ELBOW_FLEXION', TRUE),   -- id 529
                             ('0271', 'crunch (on stability ball)', 'TRUNK_FLEXION', FALSE),   -- id 530
                             ('0272', 'crunch (on stability ball, arms straight)', 'TRUNK_FLEXION', FALSE),   -- id 531
                             ('2331', 'cycle cross trainer', 'LOCOMOTION_CARDIO', FALSE),   -- id 532
                             ('1201', 'dumbbell burpee', 'LOCOMOTION_CARDIO', FALSE),   -- id 533
                             ('1437', 'dumbbell finger curls', 'WRIST_FOREARM', TRUE),   -- id 534
                             ('0299', 'dumbbell cuban press', 'VERTICAL_PUSH', FALSE),   -- id 535
                             ('2136', 'dumbbell cuban press v. 2', 'VERTICAL_PUSH', FALSE),   -- id 536
                             ('0310', 'dumbbell front raise', 'SHOULDER_RAISE', TRUE),   -- id 537
                             ('0309', 'dumbbell front raise v. 2', 'SHOULDER_RAISE', TRUE),   -- id 538
                             ('0311', 'dumbbell full can lateral raise', 'SHOULDER_RAISE', TRUE),   -- id 539
                             ('0301', 'dumbbell decline bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 540
                             ('0302', 'dumbbell decline fly', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 541
                             ('0303', 'dumbbell decline hammer press', 'HORIZONTAL_PUSH', FALSE),   -- id 542
                             ('1276', 'dumbbell decline one arm fly', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 543
                             ('0307', 'dumbbell decline twist fly', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 544
                             ('0308', 'dumbbell fly', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 545
                             ('1277', 'dumbbell fly on exercise ball', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 546
                             ('3545', 'dumbbell incline alternate press', 'HORIZONTAL_PUSH', FALSE),   -- id 547
                             ('0314', 'dumbbell incline bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 548
                             ('0305', 'dumbbell decline shrug', 'SCAPULAR_ELEVATION', TRUE),   -- id 549
                             ('0304', 'dumbbell decline shrug v. 2', 'SCAPULAR_ELEVATION', TRUE),   -- id 550
                             ('0295', 'dumbbell clean', 'HIP_HINGE', FALSE),   -- id 551
                             ('3635', 'dumbbell contralateral forward lunge', 'KNEE_DOMINANT', FALSE),   -- id 552
                             ('0300', 'dumbbell deadlift', 'HIP_HINGE', FALSE),   -- id 553
                             ('1760', 'dumbbell goblet squat', 'KNEE_DOMINANT', FALSE),   -- id 554
                             ('0294', 'dumbbell biceps curl', 'ELBOW_FLEXION', TRUE),   -- id 555
                             ('2401', 'dumbbell biceps curl (with arm blaster)', 'ELBOW_FLEXION', TRUE),   -- id 556
                             ('1654', 'dumbbell biceps curl reverse', 'ELBOW_FLEXION', TRUE),   -- id 557
                             ('1655', 'dumbbell biceps curl squat', 'KNEE_DOMINANT', FALSE),   -- id 558
                             ('1656', 'dumbbell biceps curl v sit on bosu ball', 'ELBOW_FLEXION', TRUE),   -- id 559
                             ('1731', 'dumbbell close grip press', 'HORIZONTAL_PUSH', FALSE),   -- id 560
                             ('0296', 'dumbbell close-grip press', 'HORIZONTAL_PUSH', FALSE),   -- id 561
                             ('0297', 'dumbbell concentration curl', 'ELBOW_FLEXION', TRUE),   -- id 562
                             ('0298', 'dumbbell cross body hammer curl', 'ELBOW_FLEXION', TRUE),   -- id 563
                             ('1657', 'dumbbell cross body hammer curl v. 2', 'ELBOW_FLEXION', TRUE),   -- id 564
                             ('1617', 'dumbbell decline one arm hammer press', 'HORIZONTAL_PUSH', FALSE),   -- id 565
                             ('0306', 'dumbbell decline triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 566
                             ('1732', 'dumbbell forward lunge triceps extension', 'KNEE_DOMINANT', FALSE),   -- id 567
                             ('0313', 'dumbbell hammer curl', 'ELBOW_FLEXION', TRUE),   -- id 568
                             ('1659', 'dumbbell hammer curl on exercise ball', 'ELBOW_FLEXION', TRUE),   -- id 569
                             ('0312', 'dumbbell hammer curl v. 2', 'ELBOW_FLEXION', TRUE),   -- id 570
                             ('2402', 'dumbbell hammer curls (with arm blaster)', 'ELBOW_FLEXION', TRUE),   -- id 571
                             ('1664', 'dumbbell high curl', 'ELBOW_FLEXION', TRUE),   -- id 572
                             ('0323', 'dumbbell incline one arm lateral raise', 'SHOULDER_RAISE', TRUE),   -- id 573
                             ('0325', 'dumbbell incline raise', 'VERTICAL_PUSH', FALSE),   -- id 574
                             ('0326', 'dumbbell incline rear lateral raise', 'SHOULDER_RAISE', TRUE),   -- id 575
                             ('3542', 'dumbbell incline t-raise', 'SHOULDER_RAISE', TRUE),   -- id 576
                             ('0332', 'dumbbell iron cross', 'SHOULDER_RAISE', TRUE),   -- id 577
                             ('0334', 'dumbbell lateral raise', 'SHOULDER_RAISE', TRUE),   -- id 578
                             ('0335', 'dumbbell lateral to front raise', 'SHOULDER_RAISE', TRUE),   -- id 579
                             ('0316', 'dumbbell incline breeding', 'HORIZONTAL_PUSH', FALSE),   -- id 580
                             ('0319', 'dumbbell incline fly', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 581
                             ('1278', 'dumbbell incline fly on exercise ball', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 582
                             ('0321', 'dumbbell incline hammer press', 'HORIZONTAL_PUSH', FALSE),   -- id 583
                             ('1279', 'dumbbell incline one arm fly', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 584
                             ('1280', 'dumbbell incline one arm fly on exercise ball', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 585
                             ('1281', 'dumbbell incline one arm press', 'HORIZONTAL_PUSH', FALSE),   -- id 586
                             ('1282', 'dumbbell incline one arm press on exercise ball', 'HORIZONTAL_PUSH', FALSE),   -- id 587
                             ('0324', 'dumbbell incline palm-in press', 'HORIZONTAL_PUSH', FALSE),   -- id 588
                             ('1283', 'dumbbell incline press on exercise ball', 'HORIZONTAL_PUSH', FALSE),   -- id 589
                             ('0328', 'dumbbell incline shoulder raise', 'SHOULDER_RAISE', TRUE),   -- id 590
                             ('0331', 'dumbbell incline twisted flyes', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 591
                             ('0327', 'dumbbell incline row', 'HORIZONTAL_PULL', FALSE),   -- id 592
                             ('0329', 'dumbbell incline shrug', 'SCAPULAR_ELEVATION', TRUE),   -- id 593
                             ('3541', 'dumbbell incline y-raise', 'SHOULDER_RAISE', TRUE),   -- id 594
                             ('0336', 'dumbbell lunge', 'KNEE_DOMINANT', FALSE),   -- id 595
                             ('0315', 'dumbbell incline biceps curl', 'ELBOW_FLEXION', TRUE),   -- id 596
                             ('0318', 'dumbbell incline curl', 'ELBOW_FLEXION', TRUE),   -- id 597
                             ('0317', 'dumbbell incline curl v. 2', 'ELBOW_FLEXION', TRUE),   -- id 598
                             ('0320', 'dumbbell incline hammer curl', 'ELBOW_FLEXION', TRUE),   -- id 599
                             ('1618', 'dumbbell incline hammer press on exercise ball', 'HORIZONTAL_PUSH', FALSE),   -- id 600
                             ('0322', 'dumbbell incline inner biceps curl', 'ELBOW_FLEXION', TRUE),   -- id 601
                             ('1619', 'dumbbell incline one arm hammer press', 'HORIZONTAL_PUSH', FALSE),   -- id 602
                             ('1620', 'dumbbell incline one arm hammer press on exercise ball', 'HORIZONTAL_PUSH', FALSE),   -- id 603
                             ('0330', 'dumbbell incline triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 604
                             ('1733', 'dumbbell incline two arm extension', 'ELBOW_EXTENSION', TRUE),   -- id 605
                             ('0333', 'dumbbell kickback', 'ELBOW_EXTENSION', TRUE),   -- id 606
                             ('1734', 'dumbbell kickbacks on exercise ball', 'ELBOW_EXTENSION', TRUE),   -- id 607
                             ('1660', 'dumbbell kneeling bicep curl exercise ball', 'ELBOW_FLEXION', TRUE),   -- id 608
                             ('1658', 'dumbbell lunge with bicep curl', 'KNEE_DOMINANT', FALSE),   -- id 609
                             ('0337', 'dumbbell lying extension (across face)', 'ELBOW_EXTENSION', TRUE),   -- id 610
                             ('1729', 'dumbbell lying alternate extension', 'ELBOW_EXTENSION', TRUE),   -- id 611
                             ('0338', 'dumbbell lying elbow press', 'ELBOW_EXTENSION', TRUE),   -- id 612
                             ('0347', 'dumbbell lying pronation', 'WRIST_FOREARM', TRUE),   -- id 613
                             ('2705', 'dumbbell lying pronation on floor', 'WRIST_FOREARM', TRUE),   -- id 614
                             ('0349', 'dumbbell lying supination', 'WRIST_FOREARM', TRUE),   -- id 615
                             ('2706', 'dumbbell lying supination on floor', 'WRIST_FOREARM', TRUE),   -- id 616
                             ('0863', 'dumbbell lying external shoulder rotation', 'OTHER', FALSE),   -- id 617
                             ('2470', 'dumbbell lying on floor rear delt raise', 'SHOULDER_RAISE', TRUE),   -- id 618
                             ('0341', 'dumbbell lying one arm deltoid rear', 'SHOULDER_RAISE', TRUE),   -- id 619
                             ('0345', 'dumbbell lying one arm rear lateral raise', 'SHOULDER_RAISE', TRUE),   -- id 620
                             ('0348', 'dumbbell lying rear lateral raise', 'SHOULDER_RAISE', TRUE),   -- id 621
                             ('0355', 'dumbbell one arm lateral raise', 'SHOULDER_RAISE', TRUE),   -- id 622
                             ('0356', 'dumbbell one arm lateral raise with support', 'SHOULDER_RAISE', TRUE),   -- id 623
                             ('0340', 'dumbbell lying hammer press', 'HORIZONTAL_PUSH', FALSE),   -- id 624
                             ('0343', 'dumbbell lying one arm press', 'HORIZONTAL_PUSH', FALSE),   -- id 625
                             ('0342', 'dumbbell lying one arm press v. 2', 'HORIZONTAL_PUSH', FALSE),   -- id 626
                             ('1284', 'dumbbell lying pullover on exercise ball', 'SHOULDER_EXTENSION', TRUE),   -- id 627
                             ('1285', 'dumbbell one arm bench fly', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 628
                             ('1286', 'dumbbell one arm chest fly on exercise ball', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 629
                             ('1287', 'dumbbell one arm decline chest press', 'HORIZONTAL_PUSH', FALSE),   -- id 630
                             ('1288', 'dumbbell one arm fly on exercise ball', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 631
                             ('1289', 'dumbbell one arm incline chest press', 'HORIZONTAL_PUSH', FALSE),   -- id 632
                             ('1290', 'dumbbell one arm press on exercise ball', 'VERTICAL_PUSH', FALSE),   -- id 633
                             ('1291', 'dumbbell one arm pullover on exercise ball', 'SHOULDER_EXTENSION', TRUE),   -- id 634
                             ('1328', 'dumbbell lying rear delt row', 'HORIZONTAL_PULL', FALSE),   -- id 635
                             ('0292', 'dumbbell one arm bent-over row', 'HORIZONTAL_PULL', FALSE),   -- id 636
                             ('0339', 'dumbbell lying femoral', 'KNEE_DOMINANT', TRUE),   -- id 637
                             ('0344', 'dumbbell lying one arm pronated triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 638
                             ('0346', 'dumbbell lying one arm supinated triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 639
                             ('1735', 'dumbbell lying single extension', 'ELBOW_EXTENSION', TRUE),   -- id 640
                             ('1661', 'dumbbell lying supine biceps curl', 'ELBOW_FLEXION', TRUE),   -- id 641
                             ('0350', 'dumbbell lying supine curl', 'ELBOW_FLEXION', TRUE),   -- id 642
                             ('0351', 'dumbbell lying triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 643
                             ('1662', 'dumbbell lying wide curl', 'ELBOW_FLEXION', TRUE),   -- id 644
                             ('0352', 'dumbbell neutral grip bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 645
                             ('0353', 'dumbbell one arm concentration curl (on stability ball)', 'ELBOW_FLEXION', TRUE),   -- id 646
                             ('1736', 'dumbbell one arm french press on exercise ball', 'ELBOW_EXTENSION', TRUE),   -- id 647
                             ('1663', 'dumbbell one arm hammer preacher curl', 'ELBOW_FLEXION', TRUE),   -- id 648
                             ('1621', 'dumbbell one arm hammer press on exercise ball', 'VERTICAL_PUSH', FALSE),   -- id 649
                             ('0354', 'dumbbell one arm kickback', 'ELBOW_EXTENSION', TRUE),   -- id 650
                             ('1665', 'dumbbell one arm prone curl', 'ELBOW_FLEXION', TRUE),   -- id 651
                             ('1666', 'dumbbell one arm prone hammer curl', 'ELBOW_FLEXION', TRUE),   -- id 652
                             ('0358', 'dumbbell one arm reverse wrist curl', 'WRIST_FOREARM', TRUE),   -- id 653
                             ('1415', 'dumbbell one arm seated neutral wrist curl', 'WRIST_FOREARM', TRUE),   -- id 654
                             ('0364', 'dumbbell one arm wrist curl', 'WRIST_FOREARM', TRUE),   -- id 655
                             ('1441', 'dumbbell over bench one arm reverse wrist curl', 'WRIST_FOREARM', TRUE),   -- id 656
                             ('0367', 'dumbbell over bench one arm wrist curl', 'WRIST_FOREARM', TRUE),   -- id 657
                             ('0368', 'dumbbell over bench revers wrist curl', 'WRIST_FOREARM', TRUE),   -- id 658
                             ('0369', 'dumbbell over bench wrist curl', 'WRIST_FOREARM', TRUE),   -- id 659
                             ('0359', 'dumbbell one arm reverse fly (with support)', 'SHOULDER_RAISE', TRUE),   -- id 660
                             ('0361', 'dumbbell one arm shoulder press', 'VERTICAL_PUSH', FALSE),   -- id 661
                             ('0360', 'dumbbell one arm shoulder press v. 2', 'VERTICAL_PUSH', FALSE),   -- id 662
                             ('0363', 'dumbbell one arm upright row', 'HORIZONTAL_PULL', FALSE),   -- id 663
                             ('1700', 'dumbbell push press', 'VERTICAL_PUSH', FALSE),   -- id 664
                             ('0376', 'dumbbell raise', 'SHOULDER_RAISE', TRUE),   -- id 665
                             ('2292', 'dumbbell rear delt raise', 'SHOULDER_RAISE', TRUE),   -- id 666
                             ('1622', 'dumbbell one arm reverse grip press', 'VERTICAL_PUSH', FALSE),   -- id 667
                             ('1292', 'dumbbell one leg fly on exercise ball', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 668
                             ('1293', 'dumbbell press on exercise ball', 'HORIZONTAL_PUSH', FALSE),   -- id 669
                             ('0375', 'dumbbell pullover', 'SHOULDER_EXTENSION', TRUE),   -- id 670
                             ('1294', 'dumbbell pullover hip extension on exercise ball', 'HIP_EXTENSION', FALSE),   -- id 671
                             ('1295', 'dumbbell pullover on exercise ball', 'SHOULDER_EXTENSION', TRUE),   -- id 672
                             ('1329', 'dumbbell palm rotational bent over row', 'HORIZONTAL_PULL', FALSE),   -- id 673
                             ('3888', 'dumbbell one arm snatch', 'VERTICAL_PUSH', FALSE),   -- id 674
                             ('0371', 'dumbbell plyo squat', 'KNEE_DOMINANT', FALSE),   -- id 675
                             ('1414', 'dumbbell one arm reverse preacher curl', 'ELBOW_FLEXION', TRUE),   -- id 676
                             ('1667', 'dumbbell one arm reverse spider curl', 'ELBOW_FLEXION', TRUE),   -- id 677
                             ('1668', 'dumbbell one arm seated bicep curl on exercise ball', 'ELBOW_FLEXION', TRUE),   -- id 678
                             ('1669', 'dumbbell one arm seated hammer curl', 'ELBOW_FLEXION', TRUE),   -- id 679
                             ('1670', 'dumbbell one arm standing curl', 'ELBOW_FLEXION', TRUE),   -- id 680
                             ('1671', 'dumbbell one arm standing hammer curl', 'ELBOW_FLEXION', TRUE),   -- id 681
                             ('0362', 'dumbbell one arm triceps extension (on bench)', 'ELBOW_EXTENSION', TRUE),   -- id 682
                             ('1672', 'dumbbell one arm zottman preacher curl', 'ELBOW_FLEXION', TRUE),   -- id 683
                             ('0365', 'dumbbell over bench neutral wrist curl', 'WRIST_FOREARM', TRUE),   -- id 684
                             ('0366', 'dumbbell over bench one arm neutral wrist curl', 'WRIST_FOREARM', TRUE),   -- id 685
                             ('1623', 'dumbbell palms in incline bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 686
                             ('0370', 'dumbbell peacher hammer curl', 'ELBOW_FLEXION', TRUE),   -- id 687
                             ('0372', 'dumbbell preacher curl', 'ELBOW_FLEXION', TRUE),   -- id 688
                             ('1673', 'dumbbell preacher curl over exercise ball', 'ELBOW_FLEXION', TRUE),   -- id 689
                             ('0373', 'dumbbell pronate-grip triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 690
                             ('0374', 'dumbbell prone incline curl', 'ELBOW_FLEXION', TRUE),   -- id 691
                             ('1674', 'dumbbell prone incline hammer curl', 'ELBOW_FLEXION', TRUE),   -- id 692
                             ('0385', 'dumbbell reverse wrist curl', 'WRIST_FOREARM', TRUE),   -- id 693
                             ('0399', 'dumbbell seated one arm rotate', 'WRIST_FOREARM', TRUE),   -- id 694
                             ('1379', 'dumbbell seated calf raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 695
                             ('0377', 'dumbbell rear delt row_shoulder', 'HORIZONTAL_PULL', FALSE),   -- id 696
                             ('0378', 'dumbbell rear fly', 'SHOULDER_RAISE', TRUE),   -- id 697
                             ('0380', 'dumbbell rear lateral raise', 'SHOULDER_RAISE', TRUE),   -- id 698
                             ('0379', 'dumbbell rear lateral raise (support head)', 'SHOULDER_RAISE', TRUE),   -- id 699
                             ('0383', 'dumbbell reverse fly', 'SHOULDER_RAISE', TRUE),   -- id 700
                             ('0386', 'dumbbell rotation reverse fly', 'SHOULDER_RAISE', TRUE),   -- id 701
                             ('2397', 'dumbbell scott press', 'VERTICAL_PUSH', FALSE),   -- id 702
                             ('0387', 'dumbbell seated alternate front raise', 'SHOULDER_RAISE', TRUE),   -- id 703
                             ('0388', 'dumbbell seated alternate press', 'VERTICAL_PUSH', FALSE),   -- id 704
                             ('3546', 'dumbbell seated alternate shoulder', 'VERTICAL_PUSH', FALSE),   -- id 705
                             ('2317', 'dumbbell seated bent arm lateral raise', 'SHOULDER_RAISE', TRUE),   -- id 706
                             ('0392', 'dumbbell seated front raise', 'SHOULDER_RAISE', TRUE),   -- id 707
                             ('0396', 'dumbbell seated lateral raise', 'SHOULDER_RAISE', TRUE),   -- id 708
                             ('0395', 'dumbbell seated lateral raise v. 2', 'SHOULDER_RAISE', TRUE),   -- id 709
                             ('1624', 'dumbbell reverse bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 710
                             ('1330', 'dumbbell reverse grip incline bench one arm row', 'HORIZONTAL_PULL', FALSE),   -- id 711
                             ('1331', 'dumbbell reverse grip incline bench two arm row', 'HORIZONTAL_PULL', FALSE),   -- id 712
                             ('2327', 'dumbbell reverse grip row (female)', 'HORIZONTAL_PULL', FALSE),   -- id 713
                             ('0381', 'dumbbell rear lunge', 'KNEE_DOMINANT', FALSE),   -- id 714
                             ('1459', 'dumbbell romanian deadlift', 'HIP_HINGE', FALSE),   -- id 715
                             ('0382', 'dumbbell revers grip biceps curl', 'ELBOW_FLEXION', TRUE),   -- id 716
                             ('0384', 'dumbbell reverse preacher curl', 'ELBOW_FLEXION', TRUE),   -- id 717
                             ('1675', 'dumbbell reverse spider curl', 'ELBOW_FLEXION', TRUE),   -- id 718
                             ('1676', 'dumbbell seated alternate hammer curl on exercise ball', 'ELBOW_FLEXION', TRUE),   -- id 719
                             ('0389', 'dumbbell seated bench extension', 'ELBOW_EXTENSION', TRUE),   -- id 720
                             ('1730', 'dumbbell seated bent over alternate kickback', 'ELBOW_EXTENSION', TRUE),   -- id 721
                             ('1737', 'dumbbell seated bent over triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 722
                             ('1677', 'dumbbell seated bicep curl', 'ELBOW_FLEXION', TRUE),   -- id 723
                             ('0390', 'dumbbell seated biceps curl (on stability ball)', 'ELBOW_FLEXION', TRUE),   -- id 724
                             ('3547', 'dumbbell seated biceps curl to shoulder press', 'VERTICAL_PUSH', FALSE),   -- id 725
                             ('0391', 'dumbbell seated curl', 'ELBOW_FLEXION', TRUE),   -- id 726
                             ('1678', 'dumbbell seated hammer curl', 'ELBOW_FLEXION', TRUE),   -- id 727
                             ('0393', 'dumbbell seated inner biceps curl', 'ELBOW_FLEXION', TRUE),   -- id 728
                             ('0394', 'dumbbell seated kickback', 'ELBOW_EXTENSION', TRUE),   -- id 729
                             ('0397', 'dumbbell seated neutral wrist curl', 'WRIST_FOREARM', TRUE),   -- id 730
                             ('1679', 'dumbbell seated one arm bicep curl on exercise ball with leg raised', 'ELBOW_FLEXION', TRUE),   -- id 731
                             ('0398', 'dumbbell seated one arm kickback', 'ELBOW_EXTENSION', TRUE),   -- id 732
                             ('0401', 'dumbbell seated palms up wrist curl', 'WRIST_FOREARM', TRUE),   -- id 733
                             ('0400', 'dumbbell seated one leg calf raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 734
                             ('1380', 'dumbbell seated one leg calf raise - hammer grip', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 735
                             ('1381', 'dumbbell seated one leg calf raise - palm up', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 736
                             ('0409', 'dumbbell single leg calf raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 737
                             ('0417', 'dumbbell standing calf raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 738
                             ('0405', 'dumbbell seated shoulder press', 'VERTICAL_PUSH', FALSE),   -- id 739
                             ('0404', 'dumbbell seated shoulder press (parallel grip)', 'VERTICAL_PUSH', FALSE),   -- id 740
                             ('0408', 'dumbbell side lying one hand raise', 'SHOULDER_RAISE', TRUE),   -- id 741
                             ('3548', 'dumbbell single arm overhead carry', 'OTHER', FALSE),   -- id 742
                             ('0414', 'dumbbell standing alternate overhead press', 'VERTICAL_PUSH', FALSE),   -- id 743
                             ('0415', 'dumbbell standing alternate raise', 'SHOULDER_RAISE', TRUE),   -- id 744
                             ('2143', 'dumbbell standing around world', 'SHOULDER_RAISE', TRUE),   -- id 745
                             ('0419', 'dumbbell standing front raise above head', 'SHOULDER_RAISE', TRUE),   -- id 746
                             ('0424', 'dumbbell standing one arm palm in press', 'VERTICAL_PUSH', FALSE),   -- id 747
                             ('0407', 'dumbbell side bend', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 748
                             ('0406', 'dumbbell shrug', 'SCAPULAR_ELEVATION', TRUE),   -- id 749
                             ('3664', 'dumbbell side plank with rear fly', 'SHOULDER_RAISE', TRUE),   -- id 750
                             ('1757', 'dumbbell single leg deadlift', 'HIP_HINGE', FALSE),   -- id 751
                             ('2805', 'dumbbell single leg deadlift with stepbox support', 'HIP_HINGE', FALSE),   -- id 752
                             ('0410', 'dumbbell single leg split squat', 'KNEE_DOMINANT', FALSE),   -- id 753
                             ('0411', 'dumbbell single leg squat', 'KNEE_DOMINANT', FALSE),   -- id 754
                             ('0413', 'dumbbell squat', 'KNEE_DOMINANT', FALSE),   -- id 755
                             ('0402', 'dumbbell seated preacher curl', 'ELBOW_FLEXION', TRUE),   -- id 756
                             ('0403', 'dumbbell seated revers grip concentration curl', 'ELBOW_FLEXION', TRUE),   -- id 757
                             ('1738', 'dumbbell seated reverse grip one arm overhead tricep extension', 'ELBOW_EXTENSION', TRUE),   -- id 758
                             ('2188', 'dumbbell seated triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 759
                             ('3560', 'dumbbell standing alternate hammer curl and press', 'VERTICAL_PUSH', FALSE),   -- id 760
                             ('1739', 'dumbbell standing alternating tricep kickback', 'ELBOW_EXTENSION', TRUE),   -- id 761
                             ('1740', 'dumbbell standing bent over one arm triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 762
                             ('1741', 'dumbbell standing bent over two arm triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 763
                             ('0416', 'dumbbell standing biceps curl', 'ELBOW_FLEXION', TRUE),   -- id 764
                             ('0418', 'dumbbell standing concentration curl', 'ELBOW_FLEXION', TRUE),   -- id 765
                             ('2321', 'dumbbell standing inner biceps curl v. 2', 'ELBOW_FLEXION', TRUE),   -- id 766
                             ('0420', 'dumbbell standing kickback', 'ELBOW_EXTENSION', TRUE),   -- id 767
                             ('0421', 'dumbbell standing one arm concentration curl', 'ELBOW_FLEXION', TRUE),   -- id 768
                             ('0422', 'dumbbell standing one arm curl (over incline bench)', 'ELBOW_FLEXION', TRUE),   -- id 769
                             ('1680', 'dumbbell standing one arm curl over incline bench', 'ELBOW_FLEXION', TRUE),   -- id 770
                             ('0423', 'dumbbell standing one arm extension', 'ELBOW_EXTENSION', TRUE),   -- id 771
                             ('0425', 'dumbbell standing one arm reverse curl', 'ELBOW_FLEXION', TRUE),   -- id 772
                             ('1167', 'dynamic chest stretch (male)', 'MOBILITY', FALSE),   -- id 773
                             ('0443', 'elbow-to-knee', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 774
                             ('1772', 'elbow lift - reverse push-up', 'HORIZONTAL_PUSH', FALSE),   -- id 775
                             ('3292', 'elevator', 'HIP_HINGE', FALSE),   -- id 776
                             ('3287', 'elbow dips', 'HORIZONTAL_PUSH', FALSE),   -- id 777
                             ('0426', 'dumbbell standing overhead press', 'VERTICAL_PUSH', FALSE),   -- id 778
                             ('0427', 'dumbbell standing palms in press', 'VERTICAL_PUSH', FALSE),   -- id 779
                             ('0437', 'dumbbell upright row', 'HORIZONTAL_PULL', FALSE),   -- id 780
                             ('1765', 'dumbbell upright row (back pov)', 'HORIZONTAL_PULL', FALSE),   -- id 781
                             ('0864', 'dumbbell upright shoulder external rotation', 'OTHER', FALSE),   -- id 782
                             ('0438', 'dumbbell w-press', 'VERTICAL_PUSH', FALSE),   -- id 783
                             ('0433', 'dumbbell straight arm pullover', 'SHOULDER_EXTENSION', TRUE),   -- id 784
                             ('0431', 'dumbbell step-up', 'KNEE_DOMINANT', FALSE),   -- id 785
                             ('2796', 'dumbbell step-up lunge', 'KNEE_DOMINANT', FALSE),   -- id 786
                             ('2812', 'dumbbell step-up split squat', 'KNEE_DOMINANT', FALSE),   -- id 787
                             ('0432', 'dumbbell stiff leg deadlift', 'HIP_HINGE', FALSE),   -- id 788
                             ('0434', 'dumbbell straight leg deadlift', 'HIP_HINGE', FALSE),   -- id 789
                             ('2808', 'dumbbell sumo pull through', 'HIP_HINGE', FALSE),   -- id 790
                             ('2803', 'dumbbell supported squat', 'KNEE_DOMINANT', FALSE),   -- id 791
                             ('0428', 'dumbbell standing preacher curl', 'ELBOW_FLEXION', TRUE),   -- id 792
                             ('0429', 'dumbbell standing reverse curl', 'ELBOW_FLEXION', TRUE),   -- id 793
                             ('0430', 'dumbbell standing triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 794
                             ('2293', 'dumbbell standing zottman preacher curl', 'ELBOW_FLEXION', TRUE),   -- id 795
                             ('1684', 'dumbbell step up single leg balance with bicep curl', 'KNEE_DOMINANT', FALSE),   -- id 796
                             ('0436', 'dumbbell tate press', 'ELBOW_EXTENSION', TRUE),   -- id 797
                             ('1742', 'dumbbell tricep kickback with stork stance', 'ELBOW_EXTENSION', TRUE),   -- id 798
                             ('1743', 'dumbbell twisting bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 799
                             ('5201', 'dumbbell waiter biceps curl', 'ELBOW_FLEXION', TRUE),   -- id 800
                             ('0439', 'dumbbell zottman curl', 'ELBOW_FLEXION', TRUE),   -- id 801
                             ('2294', 'dumbbell zottman preacher curl', 'ELBOW_FLEXION', TRUE),   -- id 802
                             ('2189', 'dumbbells seated triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 803
                             ('1332', 'exercise ball alternating arm ups', 'SHOULDER_RAISE', TRUE),   -- id 804
                             ('1333', 'exercise ball back extension with arms extended', 'TRUNK_EXTENSION', FALSE),   -- id 805
                             ('1334', 'exercise ball back extension with hands behind head', 'TRUNK_EXTENSION', FALSE),   -- id 806
                             ('1335', 'exercise ball back extension with knees off ground', 'TRUNK_EXTENSION', FALSE),   -- id 807
                             ('1336', 'exercise ball back extension with rotation', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 808
                             ('1338', 'exercise ball hug', 'MOBILITY', FALSE),   -- id 809
                             ('1339', 'exercise ball lat stretch', 'MOBILITY', FALSE),   -- id 810
                             ('1559', 'exercise ball hip flexor stretch', 'MOBILITY', FALSE),   -- id 811
                             ('1744', 'exercise ball dip', 'HORIZONTAL_PUSH', FALSE),   -- id 812
                             ('3303', 'flag', 'CORE_STABILITY', FALSE),   -- id 813
                             ('0456', 'flexion leg sit up (bent knee)', 'TRUNK_FLEXION', FALSE),   -- id 814
                             ('0457', 'flexion leg sit up (straight arm)', 'TRUNK_FLEXION', FALSE),   -- id 815
                             ('1382', 'exercise ball on the wall calf raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 816
                             ('3241', 'exercise ball on the wall calf raise (tennis ball between ankles)', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 817
                             ('3240', 'exercise ball on the wall calf raise (tennis ball between knees)', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 818
                             ('2133', 'farmers walk', 'OTHER', FALSE),   -- id 819
                             ('1746', 'exercise ball supine triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 820
                             ('1296', 'exercise ball pike push up', 'VERTICAL_PUSH', FALSE),   -- id 821
                             ('1341', 'exercise ball lower back stretch (pyramid)', 'MOBILITY', FALSE),   -- id 822
                             ('1342', 'exercise ball lying side lat stretch', 'MOBILITY', FALSE),   -- id 823
                             ('1343', 'exercise ball prone leg raise', 'HIP_EXTENSION', FALSE),   -- id 824
                             ('1416', 'exercise ball one leg prone lower body rotation', 'OTHER', FALSE),   -- id 825
                             ('1417', 'exercise ball one legged diagonal kick hamstring curl', 'KNEE_DOMINANT', FALSE),   -- id 826
                             ('1560', 'exercise ball seated hamstring stretch', 'MOBILITY', FALSE),   -- id 827
                             ('1745', 'exercise ball seated triceps stretch', 'MOBILITY', FALSE),   -- id 828
                             ('0455', 'finger curls', 'WRIST_FOREARM', TRUE),   -- id 829
                             ('0445', 'ez barbell anti gravity press', 'VERTICAL_PUSH', FALSE),   -- id 830
                             ('3010', 'ez bar lying bent arms pullover', 'SHOULDER_EXTENSION', TRUE),   -- id 831
                             ('1344', 'ez bar reverse grip bent over row', 'HORIZONTAL_PULL', FALSE),   -- id 832
                             ('1747', 'ez bar french press on exercise ball', 'ELBOW_EXTENSION', TRUE),   -- id 833
                             ('1748', 'ez bar lying close grip triceps extension behind head', 'ELBOW_EXTENSION', TRUE),   -- id 834
                             ('1682', 'ez bar seated close grip concentration curl', 'ELBOW_FLEXION', TRUE),   -- id 835
                             ('1749', 'ez bar standing french press', 'ELBOW_EXTENSION', TRUE),   -- id 836
                             ('1627', 'ez barbell close grip preacher curl', 'ELBOW_FLEXION', TRUE),   -- id 837
                             ('0446', 'ez barbell close-grip curl', 'ELBOW_FLEXION', TRUE),   -- id 838
                             ('0447', 'ez barbell curl', 'ELBOW_FLEXION', TRUE),   -- id 839
                             ('0448', 'ez barbell decline close grip face press', 'ELBOW_EXTENSION', TRUE),   -- id 840
                             ('2186', 'ez barbell decline triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 841
                             ('0449', 'ez barbell incline triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 842
                             ('0450', 'ez barbell jm bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 843
                             ('0451', 'ez barbell reverse grip curl', 'ELBOW_FLEXION', TRUE),   -- id 844
                             ('0452', 'ez barbell reverse grip preacher curl', 'ELBOW_FLEXION', TRUE),   -- id 845
                             ('1458', 'ez barbell seated curls', 'ELBOW_FLEXION', TRUE),   -- id 846
                             ('0453', 'ez barbell seated triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 847
                             ('0454', 'ez barbell spider curl', 'ELBOW_FLEXION', TRUE),   -- id 848
                             ('1628', 'ez barbell spider curl', 'ELBOW_FLEXION', TRUE),   -- id 849
                             ('2404', 'ez-bar biceps curl (with arm blaster)', 'ELBOW_FLEXION', TRUE),   -- id 850
                             ('2432', 'ez-bar close-grip bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 851
                             ('2741', 'ez-barbell standing wide grip biceps curl', 'ELBOW_FLEXION', TRUE),   -- id 852
                             ('3221', 'half knee bends (male)', 'KNEE_DOMINANT', FALSE),   -- id 853
                             ('3636', 'high knee against wall', 'LOCOMOTION_CARDIO', FALSE),   -- id 854
                             ('3327', 'full planche push-up', 'HORIZONTAL_PUSH', FALSE),   -- id 855
                             ('2429', 'frog crunch', 'TRUNK_FLEXION', FALSE),   -- id 856
                             ('3301', 'frog planche', 'CORE_STABILITY', FALSE),   -- id 857
                             ('3296', 'front lever', 'CORE_STABILITY', FALSE),   -- id 858
                             ('0464', 'front plank with twist', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 859
                             ('3315', 'full maltese', 'CORE_STABILITY', FALSE),   -- id 860
                             ('3299', 'full planche', 'CORE_STABILITY', FALSE),   -- id 861
                             ('0467', 'gorilla chin', 'VERTICAL_PULL', FALSE),   -- id 862
                             ('0469', 'groin crunch', 'TRUNK_FLEXION', FALSE),   -- id 863
                             ('3202', 'half sit-up (male)', 'TRUNK_FLEXION', FALSE),   -- id 864
                             ('1764', 'hanging leg hip raise', 'TRUNK_FLEXION', FALSE),   -- id 865
                             ('0472', 'hanging leg raise', 'TRUNK_FLEXION', FALSE),   -- id 866
                             ('1761', 'hanging oblique knee raise', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 867
                             ('0473', 'hanging pike', 'TRUNK_FLEXION', FALSE),   -- id 868
                             ('0474', 'hanging straight leg hip raise', 'TRUNK_FLEXION', FALSE),   -- id 869
                             ('0475', 'hanging straight leg raise', 'TRUNK_FLEXION', FALSE),   -- id 870
                             ('0476', 'hanging straight twisting leg hip raise', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 871
                             ('0484', 'hip raise (bent knee)', 'HIP_EXTENSION', FALSE),   -- id 872
                             ('3295', 'front lever reps', 'CORE_STABILITY', FALSE),   -- id 873
                             ('0466', 'gironda sternum chin', 'VERTICAL_PULL', FALSE),   -- id 874
                             ('0459', 'flutter kicks', 'TRUNK_FLEXION', FALSE),   -- id 875
                             ('1472', 'forward jump', 'LOCOMOTION_CARDIO', FALSE),   -- id 876
                             ('3470', 'forward lunge (male)', 'KNEE_DOMINANT', FALSE),   -- id 877
                             ('3561', 'glute bridge march', 'HIP_EXTENSION', FALSE),   -- id 878
                             ('3523', 'glute bridge two legs on bench (male)', 'HIP_EXTENSION', FALSE),   -- id 879
                             ('3193', 'glute-ham raise', 'KNEE_DOMINANT', FALSE),   -- id 880
                             ('1511', 'hamstring stretch', 'MOBILITY', FALSE),   -- id 881
                             ('3218', 'hands clasped circular toe touch (male)', 'TRUNK_FLEXION', FALSE),   -- id 882
                             ('3215', 'hands reversed clasped circular toe touch (male)', 'TRUNK_FLEXION', FALSE),   -- id 883
                             ('1418', 'hug keens to chest', 'KNEE_DOMINANT', FALSE),   -- id 884
                             ('3302', 'handstand', 'BALANCE_CONTROL', FALSE),   -- id 885
                             ('0471', 'handstand push-up', 'VERTICAL_PUSH', FALSE),   -- id 886
                             ('3234', 'hyght dumbbell fly', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 887
                             ('0458', 'floor fly (with barbell)', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 888
                             ('3194', 'frankenstein squat', 'KNEE_DOMINANT', FALSE),   -- id 889
                             ('0501', 'jack burpee', 'LOCOMOTION_CARDIO', FALSE),   -- id 893
                             ('3224', 'jack jump (male)', 'LOCOMOTION_CARDIO', FALSE),   -- id 894
                             ('0492', 'incline push up depth jump', 'HORIZONTAL_PUSH', FALSE),   -- id 895
                             ('0493', 'incline push-up', 'HORIZONTAL_PUSH', FALSE),   -- id 896
                             ('3785', 'incline push-up (on box)', 'HORIZONTAL_PUSH', FALSE),   -- id 897
                             ('0494', 'incline reverse grip push-up', 'HORIZONTAL_PUSH', FALSE),   -- id 898
                             ('3011', 'incline scapula push up', 'HORIZONTAL_PUSH', FALSE),   -- id 899
                             ('1297', 'isometric chest squeeze', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 900
                             ('0500', 'isometric wipers', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 901
                             ('1471', 'inchworm', 'LOCOMOTION_CARDIO', FALSE),   -- id 902
                             ('3698', 'inchworm v. 2', 'LOCOMOTION_CARDIO', FALSE),   -- id 903
                             ('0491', 'incline leg hip raise (leg straight)', 'TRUNK_FLEXION', FALSE),   -- id 904
                             ('0495', 'incline twisting sit-up', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 905
                             ('0507', 'jackknife sit-up', 'TRUNK_FLEXION', FALSE),   -- id 906
                             ('0508', 'janda sit-up', 'TRUNK_FLEXION', FALSE),   -- id 907
                             ('0489', 'hyperextension', 'TRUNK_EXTENSION', FALSE),   -- id 908
                             ('0488', 'hyperextension (on bench)', 'TRUNK_EXTENSION', FALSE),   -- id 909
                             ('0499', 'inverted row', 'HORIZONTAL_PULL', FALSE),   -- id 910
                             ('2300', 'inverted row bent knees', 'HORIZONTAL_PULL', FALSE),   -- id 911
                             ('2298', 'inverted row on bench', 'HORIZONTAL_PULL', FALSE),   -- id 912
                             ('0497', 'inverted row v. 2', 'HORIZONTAL_PULL', FALSE),   -- id 913
                             ('0498', 'inverted row with straps', 'HORIZONTAL_PULL', FALSE),   -- id 914
                             ('0496', 'inverse leg curl (bench support)', 'KNEE_DOMINANT', TRUE),   -- id 915
                             ('2400', 'inverse leg curl (on pull-up cable machine)', 'KNEE_DOMINANT', TRUE),   -- id 916
                             ('1419', 'iron cross stretch', 'MOBILITY', FALSE),   -- id 917
                             ('0514', 'jump squat', 'KNEE_DOMINANT', FALSE),   -- id 918
                             ('0513', 'jump squat v. 2', 'KNEE_DOMINANT', FALSE),   -- id 919
                             ('3289', 'impossible dips', 'HORIZONTAL_PUSH', FALSE),   -- id 920
                             ('0490', 'incline close-grip push-up', 'HORIZONTAL_PUSH', FALSE),   -- id 921
                             ('0518', 'kettlebell alternating hang clean', 'HIP_HINGE', FALSE),   -- id 922
                             ('0520', 'kettlebell alternating press', 'VERTICAL_PUSH', FALSE),   -- id 923
                             ('0523', 'kettlebell arnold press', 'VERTICAL_PUSH', FALSE),   -- id 924
                             ('0519', 'kettlebell alternating press on floor', 'HORIZONTAL_PUSH', FALSE),   -- id 925
                             ('0517', 'kettlebell advanced windmill', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 926
                             ('0524', 'kettlebell bent press', 'VERTICAL_PUSH', FALSE),   -- id 927
                             ('0521', 'kettlebell alternating renegade row', 'HORIZONTAL_PULL', FALSE),   -- id 928
                             ('0522', 'kettlebell alternating row', 'HORIZONTAL_PULL', FALSE),   -- id 929
                             ('0525', 'kettlebell bottoms up clean from the hang position', 'HIP_HINGE', FALSE),   -- id 930
                             ('3211', 'kneeling push-up (male)', 'HORIZONTAL_PUSH', FALSE),   -- id 933
                             ('3288', 'korean dips', 'HORIZONTAL_PUSH', FALSE),   -- id 934
                             ('3640', 'knee touch crunch', 'TRUNK_FLEXION', FALSE),   -- id 935
                             ('3239', 'kneeling plank tap shoulder (male)', 'CORE_STABILITY', FALSE),   -- id 936
                             ('0558', 'kipping muscle up', 'VERTICAL_PULL', FALSE),   -- id 937
                             ('1346', 'kneeling lat stretch', 'MOBILITY', FALSE),   -- id 938
                             ('0555', 'kick out sit', 'KNEE_DOMINANT', FALSE),   -- id 939
                             ('0527', 'kettlebell double jerk', 'VERTICAL_PUSH', FALSE),   -- id 940
                             ('0528', 'kettlebell double push press', 'VERTICAL_PUSH', FALSE),   -- id 941
                             ('0529', 'kettlebell double snatch', 'VERTICAL_PUSH', FALSE),   -- id 942
                             ('0537', 'kettlebell one arm clean and jerk', 'VERTICAL_PUSH', FALSE),   -- id 943
                             ('0538', 'kettlebell one arm jerk', 'VERTICAL_PUSH', FALSE),   -- id 944
                             ('0539', 'kettlebell one arm military press to the side', 'VERTICAL_PUSH', FALSE),   -- id 945
                             ('0540', 'kettlebell one arm push press', 'VERTICAL_PUSH', FALSE),   -- id 946
                             ('0542', 'kettlebell one arm snatch', 'VERTICAL_PUSH', FALSE),   -- id 947
                             ('0543', 'kettlebell pirate supper legs', 'VERTICAL_PUSH', FALSE),   -- id 948
                             ('0546', 'kettlebell seated press', 'VERTICAL_PUSH', FALSE),   -- id 949
                             ('1438', 'kettlebell seated two arm military press', 'VERTICAL_PUSH', FALSE),   -- id 950
                             ('0547', 'kettlebell seesaw press', 'VERTICAL_PUSH', FALSE),   -- id 951
                             ('0550', 'kettlebell thruster', 'KNEE_DOMINANT', FALSE),   -- id 952
                             ('0552', 'kettlebell two arm clean', 'HIP_HINGE', FALSE),   -- id 953
                             ('0553', 'kettlebell two arm military press', 'VERTICAL_PUSH', FALSE),   -- id 954
                             ('0531', 'kettlebell extended range one arm press on floor', 'HORIZONTAL_PUSH', FALSE),   -- id 955
                             ('1298', 'kettlebell one arm floor press', 'HORIZONTAL_PUSH', FALSE),   -- id 956
                             ('0545', 'kettlebell plyo push-up', 'HORIZONTAL_PUSH', FALSE),   -- id 957
                             ('0530', 'kettlebell double windmill', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 958
                             ('0532', 'kettlebell figure 8', 'HIP_HINGE', FALSE),   -- id 959
                             ('0554', 'kettlebell windmill', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 960
                             ('0541', 'kettlebell one arm row', 'HORIZONTAL_PULL', FALSE),   -- id 961
                             ('0548', 'kettlebell sumo high pull', 'KNEE_DOMINANT', FALSE),   -- id 962
                             ('1345', 'kettlebell two arm row', 'HORIZONTAL_PULL', FALSE),   -- id 963
                             ('0533', 'kettlebell front squat', 'KNEE_DOMINANT', FALSE),   -- id 964
                             ('0534', 'kettlebell goblet squat', 'KNEE_DOMINANT', FALSE),   -- id 965
                             ('0535', 'kettlebell hang clean', 'HIP_HINGE', FALSE),   -- id 966
                             ('0536', 'kettlebell lunge pass through', 'KNEE_DOMINANT', FALSE),   -- id 967
                             ('0544', 'kettlebell pistol squat', 'KNEE_DOMINANT', FALSE),   -- id 968
                             ('0549', 'kettlebell swing', 'HIP_HINGE', FALSE),   -- id 969
                             ('0551', 'kettlebell turkish get up (squat style)', 'OTHER', FALSE),   -- id 970
                             ('0526', 'kettlebell double alternating hang clean', 'HIP_HINGE', FALSE),   -- id 971
                             ('1420', 'kneeling jump squat', 'KNEE_DOMINANT', FALSE),   -- id 972
                             ('2271', 'left hook. boxing', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 973
                             ('3419', 'l-sit on floor', 'CORE_STABILITY', FALSE),   -- id 974
                             ('3300', 'lean planche', 'CORE_STABILITY', FALSE),   -- id 975
                             ('0570', 'leg pull in flat bench', 'TRUNK_FLEXION', FALSE),   -- id 976
                             ('3418', 'l-pull-up', 'VERTICAL_PULL', FALSE),   -- id 977
                             ('1576', 'leg up hamstring stretch', 'MOBILITY', FALSE),   -- id 978
                             ('3237', 'landmine lateral raise', 'SHOULDER_RAISE', TRUE),   -- id 979
                             ('0562', 'landmine 180', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 980
                             ('0574', 'lever bent over row', 'HORIZONTAL_PULL', FALSE),   -- id 981
                             ('0589', 'lever one arm bent over row', 'HORIZONTAL_PULL', FALSE),   -- id 982
                             ('2288', 'lever gripper hands', 'WRIST_FOREARM', TRUE),   -- id 983
                             ('2289', 'lever calf press', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 984
                             ('1253', 'lever donkey calf raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 985
                             ('0584', 'lever lateral raise', 'SHOULDER_RAISE', TRUE),   -- id 986
                             ('0587', 'lever military press', 'VERTICAL_PUSH', FALSE),   -- id 987
                             ('0577', 'lever chest press', 'HORIZONTAL_PUSH', FALSE),   -- id 988
                             ('0576', 'lever chest press', 'HORIZONTAL_PUSH', FALSE),   -- id 989
                             ('1300', 'lever decline chest press', 'HORIZONTAL_PUSH', FALSE),   -- id 990
                             ('1299', 'lever incline chest press', 'HORIZONTAL_PUSH', FALSE),   -- id 991
                             ('1479', 'lever incline chest press v. 2', 'HORIZONTAL_PUSH', FALSE),   -- id 992
                             ('0583', 'lever kneeling twist', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 993
                             ('0571', 'lever alternating narrow grip seated row', 'HORIZONTAL_PULL', FALSE),   -- id 994
                             ('0572', 'lever assisted chin-up', 'VERTICAL_PULL', FALSE),   -- id 995
                             ('0573', 'lever back extension', 'TRUNK_EXTENSION', FALSE),   -- id 996
                             ('3200', 'lever bent-over row with v-bar', 'HORIZONTAL_PULL', FALSE),   -- id 997
                             ('0579', 'lever front pulldown', 'VERTICAL_PULL', FALSE),   -- id 998
                             ('0580', 'lever gripless shrug', 'SCAPULAR_ELEVATION', TRUE),   -- id 999
                             ('1439', 'lever gripless shrug v. 2', 'SCAPULAR_ELEVATION', TRUE),   -- id 1000
                             ('0581', 'lever high row', 'HORIZONTAL_PULL', FALSE),   -- id 1001
                             ('0588', 'lever narrow grip seated row', 'HORIZONTAL_PULL', FALSE),   -- id 1002
                             ('2287', 'lever alternate leg press', 'KNEE_DOMINANT', FALSE),   -- id 1003
                             ('0578', 'lever deadlift', 'HIP_HINGE', FALSE),   -- id 1004
                             ('2286', 'lever hip extension v. 2', 'HIP_EXTENSION', FALSE),   -- id 1005
                             ('2611', 'lever horizontal one leg press', 'KNEE_DOMINANT', FALSE),   -- id 1006
                             ('0582', 'lever kneeling leg curl', 'KNEE_DOMINANT', TRUE),   -- id 1007
                             ('0585', 'lever leg extension', 'KNEE_DOMINANT', TRUE),   -- id 1008
                             ('0586', 'lever lying leg curl', 'KNEE_DOMINANT', TRUE),   -- id 1009
                             ('3195', 'lever lying two-one leg curl', 'KNEE_DOMINANT', TRUE),   -- id 1010
                             ('0575', 'lever bicep curl', 'ELBOW_FLEXION', TRUE),   -- id 1011
                             ('1615', 'lever hammer grip preacher curl', 'ELBOW_FLEXION', TRUE),   -- id 1012
                             ('2315', 'lever rotary calf', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 1014
                             ('2335', 'lever seated calf press', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 1015
                             ('0594', 'lever seated calf raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 1016
                             ('1385', 'lever seated squat calf raise on leg press machine', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 1017
                             ('0605', 'lever standing calf raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 1018
                             ('0590', 'lever one arm shoulder press', 'VERTICAL_PUSH', FALSE),   -- id 1019
                             ('0602', 'lever seated reverse fly', 'SHOULDER_RAISE', TRUE),   -- id 1020
                             ('0601', 'lever seated reverse fly (parallel grip)', 'SHOULDER_RAISE', TRUE),   -- id 1021
                             ('0603', 'lever shoulder press', 'VERTICAL_PUSH', FALSE),   -- id 1022
                             ('0869', 'lever shoulder press v. 2', 'VERTICAL_PUSH', FALSE),   -- id 1023
                             ('2318', 'lever shoulder press v. 3', 'VERTICAL_PUSH', FALSE),   -- id 1024
                             ('0596', 'lever seated fly', 'HORIZONTAL_ADDUCTION', TRUE),   -- id 1025
                             ('3758', 'lever standing chest press', 'HORIZONTAL_PUSH', FALSE),   -- id 1026
                             ('1452', 'lever seated crunch', 'TRUNK_FLEXION', FALSE),   -- id 1027
                             ('0595', 'lever seated crunch (chest pad)', 'TRUNK_FLEXION', FALSE),   -- id 1028
                             ('3760', 'lever seated crunch v. 2', 'TRUNK_FLEXION', FALSE),   -- id 1029
                             ('0600', 'lever seated leg raise crunch', 'TRUNK_FLEXION', FALSE),   -- id 1030
                             ('1356', 'lever one arm lateral high row', 'HORIZONTAL_PULL', FALSE),   -- id 1031
                             ('1347', 'lever one arm lateral wide pulldown', 'VERTICAL_PULL', FALSE),   -- id 1032
                             ('2285', 'lever pullover', 'SHOULDER_EXTENSION', TRUE),   -- id 1033
                             ('2736', 'lever reverse grip lateral pulldown', 'VERTICAL_PULL', FALSE),   -- id 1034
                             ('1348', 'lever reverse grip vertical row', 'HORIZONTAL_PULL', FALSE),   -- id 1035
                             ('1349', 'lever reverse t-bar row', 'HORIZONTAL_PULL', FALSE),   -- id 1036
                             ('1350', 'lever seated row', 'HORIZONTAL_PULL', FALSE),   -- id 1037
                             ('0604', 'lever shrug', 'SCAPULAR_ELEVATION', TRUE),   -- id 1038
                             ('0606', 'lever t bar row', 'HORIZONTAL_PULL', FALSE),   -- id 1039
                             ('1351', 'lever t-bar reverse grip row', 'HORIZONTAL_PULL', FALSE),   -- id 1040
                             ('1313', 'lever unilateral row', 'HORIZONTAL_PULL', FALSE),   -- id 1041
                             ('0593', 'lever reverse hyperextension', 'HIP_EXTENSION', FALSE),   -- id 1042
                             ('3759', 'lever seated good morning', 'HIP_HINGE', FALSE),   -- id 1043
                             ('0597', 'lever seated hip abduction', 'HIP_ABDUCTION_ADDUCTION', TRUE),   -- id 1044
                             ('0598', 'lever seated hip adduction', 'HIP_ABDUCTION_ADDUCTION', TRUE),   -- id 1045
                             ('0599', 'lever seated leg curl', 'KNEE_DOMINANT', TRUE),   -- id 1046
                             ('0591', 'lever overhand triceps dip', 'HORIZONTAL_PUSH', FALSE),   -- id 1047
                             ('0592', 'lever preacher curl', 'ELBOW_FLEXION', TRUE),   -- id 1048
                             ('1614', 'lever preacher curl v. 2', 'ELBOW_FLEXION', TRUE),   -- id 1049
                             ('1616', 'lever reverse grip preacher curl', 'ELBOW_FLEXION', TRUE),   -- id 1050
                             ('1451', 'lever seated dip', 'HORIZONTAL_PUSH', FALSE),   -- id 1051
                             ('0607', 'lever triceps extension', 'ELBOW_EXTENSION', TRUE),   -- id 1052
                             ('1403', 'neck side stretch', 'MOBILITY', FALSE),   -- id 1053
                             ('0630', 'mountain climber', 'LOCOMOTION_CARDIO', FALSE),   -- id 1054
                             ('1421', 'modified push up to lower arms', 'HORIZONTAL_PUSH', FALSE),   -- id 1055
                             ('1386', 'one leg donkey calf raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 1056
                             ('1387', 'one leg floor calf raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 1057
                             ('3217', 'modified hindu push-up (male)', 'HORIZONTAL_PUSH', FALSE),   -- id 1058
                             ('1688', 'lunge with twist', 'KNEE_DOMINANT', FALSE),   -- id 1059
                             ('2312', 'lying elbow to knee', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 1060
                             ('0620', 'lying leg raise flat bench', 'TRUNK_FLEXION', FALSE),   -- id 1061
                             ('0865', 'lying leg-hip raise', 'TRUNK_FLEXION', FALSE),   -- id 1062
                             ('0634', 'negative crunch', 'TRUNK_FLEXION', FALSE),   -- id 1063
                             ('1495', 'oblique crunch v. 2', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 1064
                             ('0635', 'oblique crunches floor', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 1065
                             ('1352', 'lower back curl', 'TRUNK_EXTENSION', FALSE),   -- id 1066
                             ('0627', 'mixed grip chin-up', 'VERTICAL_PULL', FALSE),   -- id 1067
                             ('0631', 'muscle up', 'VERTICAL_PULL', FALSE),   -- id 1068
                             ('1401', 'muscle-up (on vertical bar)', 'VERTICAL_PULL', FALSE),   -- id 1069
                             ('1355', 'one arm against wall', 'OTHER', FALSE),   -- id 1070
                             ('0638', 'one arm chin-up', 'VERTICAL_PULL', FALSE),   -- id 1071
                             ('1773', 'one arm towel row', 'HORIZONTAL_PULL', FALSE),   -- id 1072
                             ('3013', 'low glute bridge on floor', 'HIP_EXTENSION', FALSE),   -- id 1073
                             ('3582', 'lunge with jump', 'KNEE_DOMINANT', FALSE),   -- id 1074
                             ('0613', 'lying (side) quads stretch', 'MOBILITY', FALSE),   -- id 1075
                             ('0624', 'march sit (wall)', 'KNEE_DOMINANT', FALSE),   -- id 1076
                             ('0628', 'monster walk', 'HIP_ABDUCTION_ADDUCTION', TRUE),   -- id 1077
                             ('0639', 'one arm dip', 'HORIZONTAL_PUSH', FALSE),   -- id 1078
                             ('2328', 'narrow push-up on exercise ball', 'HORIZONTAL_PUSH', FALSE),   -- id 1079
                             ('1302', 'medicine ball chest pass', 'HORIZONTAL_PUSH', FALSE),   -- id 1080
                             ('1303', 'medicine ball chest push from 3 point stance', 'HORIZONTAL_PUSH', FALSE),   -- id 1081
                             ('1304', 'medicine ball chest push multiple response', 'HORIZONTAL_PUSH', FALSE),   -- id 1082
                             ('1305', 'medicine ball chest push single response', 'HORIZONTAL_PUSH', FALSE),   -- id 1083
                             ('1312', 'medicine ball chest push with run release', 'HORIZONTAL_PUSH', FALSE),   -- id 1084
                             ('0640', 'one arm slam (with medicine ball)', 'SHOULDER_EXTENSION', TRUE),   -- id 1085
                             ('1353', 'medicine ball catch and overhead throw', 'KNEE_DOMINANT', FALSE),   -- id 1086
                             ('1354', 'medicine ball overhead slam', 'SHOULDER_EXTENSION', TRUE),   -- id 1087
                             ('1701', 'medicine ball close grip push up', 'HORIZONTAL_PUSH', FALSE),   -- id 1088
                             ('1750', 'medicine ball supine chest throw', 'HORIZONTAL_PUSH', FALSE),   -- id 1089
                             ('1301', 'machine inner chest press', 'HORIZONTAL_PUSH', FALSE),   -- id 1092
                             ('3638', 'push to run', 'LOCOMOTION_CARDIO', FALSE),   -- id 1093
                             ('1306', 'plyo push up', 'HORIZONTAL_PUSH', FALSE),   -- id 1094
                             ('1689', 'push and pull bodyweight', 'HORIZONTAL_PUSH', FALSE),   -- id 1095
                             ('0662', 'push-up', 'HORIZONTAL_PUSH', FALSE),   -- id 1096
                             ('0659', 'push-up (wall)', 'HORIZONTAL_PUSH', FALSE),   -- id 1097
                             ('0658', 'push-up (wall) v. 2', 'HORIZONTAL_PUSH', FALSE),   -- id 1098
                             ('3145', 'push-up plus', 'HORIZONTAL_PUSH', FALSE),   -- id 1099
                             ('0666', 'raise single arm push-up', 'HORIZONTAL_PUSH', FALSE),   -- id 1100
                             ('3147', 'pelvic tilt', 'TRUNK_FLEXION', FALSE),   -- id 1101
                             ('1687', 'posterior step to overhead reach', 'KNEE_DOMINANT', FALSE),   -- id 1102
                             ('3119', 'potty squat', 'KNEE_DOMINANT', FALSE),   -- id 1103
                             ('3665', 'power point plank', 'CORE_STABILITY', FALSE),   -- id 1104
                             ('3203', 'prisoner half sit-up (male)', 'TRUNK_FLEXION', FALSE),   -- id 1105
                             ('0664', 'push-up to side plank', 'HORIZONTAL_PUSH', FALSE),   -- id 1106
                             ('3201', 'quarter sit-up', 'TRUNK_FLEXION', FALSE),   -- id 1107
                             ('0651', 'pull up (neutral grip)', 'VERTICAL_PULL', FALSE),   -- id 1108
                             ('0652', 'pull-up', 'VERTICAL_PULL', FALSE),   -- id 1109
                             ('1476', 'one leg squat', 'KNEE_DOMINANT', FALSE),   -- id 1110
                             ('0642', 'outside leg kick push-up', 'HORIZONTAL_PUSH', FALSE),   -- id 1111
                             ('1422', 'pelvic tilt into bridge', 'HIP_EXTENSION', FALSE),   -- id 1112
                             ('3662', 'pike-to-cobra push-up', 'HORIZONTAL_PUSH', FALSE),   -- id 1113
                             ('3132', 'potty squat with support', 'KNEE_DOMINANT', FALSE),   -- id 1114
                             ('0661', 'push-up inside leg kick', 'HORIZONTAL_PUSH', FALSE),   -- id 1115
                             ('3552', 'quick feet v. 2', 'LOCOMOTION_CARDIO', FALSE),   -- id 1117
                             ('0668', 'rear decline bridge', 'HIP_EXTENSION', FALSE),   -- id 1118
                             ('0643', 'overhead triceps stretch', 'MOBILITY', FALSE),   -- id 1119
                             ('1467', 'push-up on lower arms', 'HORIZONTAL_PUSH', FALSE),   -- id 1120
                             ('0660', 'push-up close-grip off dumbbell', 'HORIZONTAL_PUSH', FALSE),   -- id 1121
                             ('0655', 'push-up (on stability ball)', 'HORIZONTAL_PUSH', FALSE),   -- id 1122
                             ('0656', 'push-up (on stability ball)', 'HORIZONTAL_PUSH', FALSE),   -- id 1123
                             ('1707', 'prone twist on stability ball', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 1124
                             ('0650', 'pull-in (on stability ball)', 'TRUNK_FLEXION', FALSE),   -- id 1125
                             ('0663', 'push-up medicine ball', 'HORIZONTAL_PUSH', FALSE),   -- id 1126
                             ('0648', 'power clean', 'HIP_HINGE', FALSE),   -- id 1129
                             ('0685', 'run', 'LOCOMOTION_CARDIO', FALSE),   -- id 1133
                             ('0684', 'run (equipment)', 'LOCOMOTION_CARDIO', FALSE),   -- id 1134
                             ('3219', 'scissor jumps (male)', 'LOCOMOTION_CARDIO', FALSE),   -- id 1135
                             ('1390', 'seated calf stretch (male)', 'MOBILITY', FALSE),   -- id 1136
                             ('0669', 'rear deltoid stretch', 'MOBILITY', FALSE),   -- id 1137
                             ('3021', 'scapula push-up', 'HORIZONTAL_PUSH', FALSE),   -- id 1138
                             ('0872', 'reverse crunch', 'TRUNK_FLEXION', FALSE),   -- id 1139
                             ('3663', 'reverse plank with leg lift', 'CORE_STABILITY', FALSE),   -- id 1140
                             ('0687', 'russian twist', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 1141
                             ('0689', 'seated leg raise', 'TRUNK_FLEXION', FALSE),   -- id 1142
                             ('0670', 'rear pull-up', 'VERTICAL_PULL', FALSE),   -- id 1143
                             ('0674', 'reverse grip pull-up', 'VERTICAL_PULL', FALSE),   -- id 1144
                             ('0678', 'rocky pull-up pulldown', 'VERTICAL_PULL', FALSE),   -- id 1145
                             ('3012', 'scapula dips', 'HORIZONTAL_PUSH', FALSE),   -- id 1146
                             ('0688', 'scapular pull-up', 'VERTICAL_PULL', FALSE),   -- id 1147
                             ('1423', 'reverse hyper on flat bench', 'HIP_EXTENSION', FALSE),   -- id 1148
                             ('2571', 'rocking frog stretch', 'MOBILITY', FALSE),   -- id 1149
                             ('1585', 'runners stretch', 'MOBILITY', FALSE),   -- id 1150
                             ('1424', 'seated glute stretch', 'MOBILITY', FALSE),   -- id 1151
                             ('0672', 'reverse dip', 'HORIZONTAL_PUSH', FALSE),   -- id 1152
                             ('0677', 'ring dips', 'HORIZONTAL_PUSH', FALSE),   -- id 1153
                             ('3122', 'resistance band seated shoulder press', 'VERTICAL_PUSH', FALSE),   -- id 1154
                             ('3124', 'resistance band seated chest press', 'HORIZONTAL_PUSH', FALSE),   -- id 1155
                             ('3144', 'resistance band seated straight back row', 'HORIZONTAL_PULL', FALSE),   -- id 1156
                             ('3236', 'resistance band hip thrusts on knees (female)', 'HIP_EXTENSION', FALSE),   -- id 1157
                             ('3007', 'resistance band leg extension', 'OTHER', FALSE),   -- id 1158
                             ('3006', 'resistance band seated hip abduction', 'HIP_ABDUCTION_ADDUCTION', TRUE),   -- id 1159
                             ('3123', 'resistance band seated biceps curl', 'ELBOW_FLEXION', TRUE),   -- id 1160
                             ('0675', 'reverse hyper extension (on stability ball)', 'HIP_EXTENSION', FALSE),   -- id 1161
                             ('0673', 'reverse grip machine lat pulldown', 'VERTICAL_PULL', FALSE),   -- id 1164
                             ('0716', 'side push neck stretch', 'MOBILITY', FALSE),   -- id 1173
                             ('3222', 'semi squat jump (male)', 'KNEE_DOMINANT', FALSE),   -- id 1174
                             ('3656', 'short stride run', 'LOCOMOTION_CARDIO', FALSE),   -- id 1175
                             ('3361', 'skater hops', 'LOCOMOTION_CARDIO', FALSE),   -- id 1176
                             ('3671', 'ski step', 'LOCOMOTION_CARDIO', FALSE),   -- id 1177
                             ('0721', 'side wrist pull stretch', 'MOBILITY', FALSE),   -- id 1178
                             ('0699', 'shoulder tap push-up', 'HORIZONTAL_PUSH', FALSE),   -- id 1179
                             ('0725', 'single arm push-up', 'HORIZONTAL_PUSH', FALSE),   -- id 1180
                             ('0691', 'seated side crunch (wall)', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 1181
                             ('3699', 'shoulder tap', 'CORE_STABILITY', FALSE),   -- id 1182
                             ('0705', 'side bridge v. 2', 'CORE_STABILITY', FALSE),   -- id 1183
                             ('0709', 'side hip (on parallel bars)', 'HIP_ABDUCTION_ADDUCTION', TRUE),   -- id 1184
                             ('3213', 'side-to-side toe touch (male)', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 1185
                             ('0735', 'sit-up v. 2', 'TRUNK_FLEXION', FALSE),   -- id 1186
                             ('3679', 'sit-up with arms on chest', 'TRUNK_FLEXION', FALSE),   -- id 1187
                             ('0690', 'seated lower back stretch', 'MOBILITY', FALSE),   -- id 1188
                             ('1763', 'shoulder grip pull-up', 'VERTICAL_PULL', FALSE),   -- id 1189
                             ('1358', 'side lying floor stretch', 'MOBILITY', FALSE),   -- id 1190
                             ('0720', 'side-to-side chin', 'VERTICAL_PULL', FALSE),   -- id 1191
                             ('3304', 'skin the cat', 'MOBILITY', FALSE),   -- id 1192
                             ('2567', 'seated piriformis stretch', 'MOBILITY', FALSE),   -- id 1193
                             ('1587', 'seated wide angle pose sequence', 'MOBILITY', FALSE),   -- id 1194
                             ('0697', 'self assisted inverse leg curl', 'KNEE_DOMINANT', TRUE),   -- id 1195
                             ('1766', 'self assisted inverse leg curl', 'KNEE_DOMINANT', TRUE),   -- id 1196
                             ('0696', 'self assisted inverse leg curl (on floor)', 'KNEE_DOMINANT', TRUE),   -- id 1197
                             ('1774', 'side bridge hip abduction', 'HIP_ABDUCTION_ADDUCTION', TRUE),   -- id 1198
                             ('0710', 'side hip abduction', 'HIP_ABDUCTION_ADDUCTION', TRUE),   -- id 1199
                             ('3667', 'side lying hip adduction (male)', 'HIP_ABDUCTION_ADDUCTION', TRUE),   -- id 1200
                             ('1775', 'side plank hip adduction', 'HIP_ABDUCTION_ADDUCTION', TRUE),   -- id 1201
                             ('3645', 'single leg bridge with outstretched leg', 'HIP_EXTENSION', FALSE),   -- id 1202
                             ('1759', 'single leg squat (pistol) male', 'KNEE_DOMINANT', FALSE),   -- id 1204
                             ('1489', 'sissy squat', 'KNEE_DOMINANT', FALSE),   -- id 1205
                             ('0717', 'side push-up', 'HORIZONTAL_PUSH', FALSE),   -- id 1206
                             ('0727', 'single leg calf raise (on a dumbbell)', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 1207
                             ('1393', 'smith one leg floor calf raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 1213
                             ('0763', 'smith reverse calf raises', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 1214
                             ('1394', 'smith reverse calf raises', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 1215
                             ('0747', 'smith behind neck press', 'VERTICAL_PUSH', FALSE),   -- id 1216
                             ('0762', 'smith rear delt row', 'HORIZONTAL_PULL', FALSE),   -- id 1217
                             ('0748', 'smith bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 1218
                             ('0753', 'smith decline bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 1219
                             ('0754', 'smith decline reverse-grip press', 'HORIZONTAL_PUSH', FALSE),   -- id 1220
                             ('0757', 'smith incline bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 1221
                             ('0758', 'smith incline reverse-grip press', 'HORIZONTAL_PUSH', FALSE),   -- id 1222
                             ('0759', 'smith incline shoulder raises', 'HORIZONTAL_PULL', FALSE),   -- id 1223
                             ('1626', 'smith machine reverse decline close grip bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 1224
                             ('0756', 'smith hip raise', 'HIP_EXTENSION', FALSE),   -- id 1225
                             ('0746', 'smith back shrug', 'SCAPULAR_ELEVATION', TRUE),   -- id 1226
                             ('1359', 'smith bent over row', 'HORIZONTAL_PULL', FALSE),   -- id 1227
                             ('0761', 'smith narrow row', 'HORIZONTAL_PULL', FALSE),   -- id 1228
                             ('1360', 'smith one arm row', 'HORIZONTAL_PULL', FALSE),   -- id 1229
                             ('1361', 'smith reverse grip bent over row', 'HORIZONTAL_PULL', FALSE),   -- id 1230
                             ('0749', 'smith bent knee good morning', 'HIP_HINGE', FALSE),   -- id 1231
                             ('0750', 'smith chair squat', 'KNEE_DOMINANT', FALSE),   -- id 1232
                             ('0752', 'smith deadlift', 'HIP_HINGE', FALSE),   -- id 1233
                             ('1433', 'smith front squat (clean grip)', 'KNEE_DOMINANT', FALSE),   -- id 1234
                             ('3281', 'smith full squat', 'KNEE_DOMINANT', FALSE),   -- id 1235
                             ('0755', 'smith hack squat', 'KNEE_DOMINANT', FALSE),   -- id 1236
                             ('0760', 'smith leg press', 'KNEE_DOMINANT', FALSE),   -- id 1237
                             ('1434', 'smith low bar squat', 'KNEE_DOMINANT', FALSE),   -- id 1238
                             ('0751', 'smith close-grip bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 1239
                             ('1683', 'smith machine bicep curl', 'ELBOW_FLEXION', TRUE),   -- id 1240
                             ('1625', 'smith machine decline close grip bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 1241
                             ('1752', 'smith machine incline tricep extension', 'ELBOW_EXTENSION', TRUE),   -- id 1242
                             ('1490', 'standing calf raise (on a staircase)', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 1253
                             ('1397', 'standing calves', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 1254
                             ('1398', 'standing calves calf stretch', 'MOBILITY', FALSE),   -- id 1255
                             ('2329', 'spine twist', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 1256
                             ('1362', 'sphinx', 'MOBILITY', FALSE),   -- id 1257
                             ('1363', 'spine stretch', 'MOBILITY', FALSE),   -- id 1258
                             ('3669', 'standing archer', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 1259
                             ('0794', 'standing lateral stretch', 'MOBILITY', FALSE),   -- id 1260
                             ('1364', 'standing pelvic tilt', 'TRUNK_FLEXION', FALSE),   -- id 1261
                             ('0778', 'spider crawl push up', 'HORIZONTAL_PUSH', FALSE),   -- id 1262
                             ('2368', 'split squats', 'KNEE_DOMINANT', FALSE),   -- id 1263
                             ('1685', 'squat to overhead reach', 'KNEE_DOMINANT', FALSE),   -- id 1264
                             ('1686', 'squat to overhead reach with twist', 'KNEE_DOMINANT', FALSE),   -- id 1265
                             ('0795', 'standing single leg curl', 'KNEE_DOMINANT', TRUE),   -- id 1266
                             ('3291', 'stalder press', 'KNEE_DOMINANT', FALSE),   -- id 1267
                             ('0777', 'spell caster', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 1268
                             ('2297', 'stability ball crunch (full range hands behind head)', 'TRUNK_FLEXION', FALSE),   -- id 1269
                             ('0788', 'standing behind neck press', 'VERTICAL_PUSH', FALSE),   -- id 1271
                             ('0776', 'snatch pull', 'HIP_HINGE', FALSE),   -- id 1272
                             ('0786', 'squat jerk', 'KNEE_DOMINANT', FALSE),   -- id 1273
                             ('1426', 'smith seated wrist curl', 'WRIST_FOREARM', TRUE),   -- id 1274
                             ('0771', 'smith standing back wrist curl', 'WRIST_FOREARM', TRUE),   -- id 1275
                             ('1395', 'smith seated one leg calf raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 1276
                             ('0773', 'smith standing leg calf raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 1277
                             ('1396', 'smith toe raise', 'ANKLE_PLANTAR_FLEXION', TRUE),   -- id 1278
                             ('0765', 'smith seated shoulder press', 'VERTICAL_PUSH', FALSE),   -- id 1279
                             ('0766', 'smith shoulder press', 'VERTICAL_PUSH', FALSE),   -- id 1280
                             ('0772', 'smith standing behind head military press', 'VERTICAL_PUSH', FALSE),   -- id 1281
                             ('0774', 'smith standing military press', 'VERTICAL_PUSH', FALSE),   -- id 1282
                             ('0775', 'smith upright row', 'HORIZONTAL_PULL', FALSE),   -- id 1283
                             ('0764', 'smith reverse-grip press', 'HORIZONTAL_PUSH', FALSE),   -- id 1284
                             ('1308', 'smith wide grip bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 1285
                             ('1309', 'smith wide grip decline bench press', 'HORIZONTAL_PUSH', FALSE),   -- id 1286
                             ('0767', 'smith shrug', 'SCAPULAR_ELEVATION', TRUE),   -- id 1287
                             ('0768', 'smith single leg split squat', 'KNEE_DOMINANT', FALSE),   -- id 1288
                             ('0769', 'smith sprint lunge', 'KNEE_DOMINANT', FALSE),   -- id 1289
                             ('0770', 'smith squat', 'KNEE_DOMINANT', FALSE),   -- id 1290
                             ('3142', 'smith sumo squat', 'KNEE_DOMINANT', FALSE),   -- id 1291
                             ('3223', 'star jump (male)', 'LOCOMOTION_CARDIO', FALSE),   -- id 1293
                             ('3318', 'swing 360', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 1294
                             ('3655', 'walking high knees lunge', 'KNEE_DOMINANT', FALSE),   -- id 1295
                             ('0803', 'superman push-up', 'HORIZONTAL_PUSH', FALSE),   -- id 1296
                             ('0806', 'suspended push-up', 'HORIZONTAL_PUSH', FALSE),   -- id 1297
                             ('3314', 'straddle maltese', 'CORE_STABILITY', FALSE),   -- id 1298
                             ('3298', 'straddle planche', 'CORE_STABILITY', FALSE),   -- id 1299
                             ('0805', 'suspended abdominal fallout', 'CORE_STABILITY', FALSE),   -- id 1300
                             ('0807', 'suspended reverse crunch', 'TRUNK_FLEXION', FALSE),   -- id 1301
                             ('0871', 'tuck crunch', 'TRUNK_FLEXION', FALSE),   -- id 1302
                             ('2802', 'twisted leg raise', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 1303
                             ('2801', 'twisted leg raise (female)', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 1304
                             ('3420', 'v-sit on floor', 'TRUNK_FLEXION', FALSE),   -- id 1305
                             ('0826', 'vertical leg raise (on parallel bars)', 'TRUNK_FLEXION', FALSE),   -- id 1306
                             ('0808', 'suspended row', 'HORIZONTAL_PULL', FALSE),   -- id 1307
                             ('3231', 'two toe touch (male)', 'TRUNK_FLEXION', FALSE),   -- id 1308
                             ('1365', 'upper back stretch', 'MOBILITY', FALSE),   -- id 1309
                             ('1366', 'upward facing dog', 'MOBILITY', FALSE),   -- id 1310
                             ('1427', 'straight leg outer hip abductor', 'HIP_ABDUCTION_ADDUCTION', TRUE),   -- id 1311
                             ('0809', 'suspended split squat', 'KNEE_DOMINANT', FALSE),   -- id 1312
                             ('3433', 'swimmer kicks v. 2 (male)', 'CORE_STABILITY', FALSE),   -- id 1313
                             ('1466', 'twist hip lift', 'TRUNK_ROTATION_LATERAL_FLEXION', FALSE),   -- id 1314
                             ('1460', 'walking lunge', 'KNEE_DOMINANT', FALSE),   -- id 1315
                             ('0814', 'triceps dip', 'HORIZONTAL_PUSH', FALSE),   -- id 1317
                             ('0812', 'triceps dip (bench leg)', 'HORIZONTAL_PUSH', FALSE),   -- id 1318
                             ('0815', 'triceps dips floor', 'HORIZONTAL_PUSH', FALSE),   -- id 1320
                             ('0816', 'triceps press', 'HORIZONTAL_PUSH', FALSE),   -- id 1321
                             ('0817', 'triceps stretch', 'MOBILITY', FALSE),   -- id 1322
                             ('0818', 'twin handle parallel grip lat pulldown', 'VERTICAL_PULL', FALSE),   -- id 1323
                             ('0798', 'stationary bike walk', 'LOCOMOTION_CARDIO', FALSE),   -- id 1324
                             ('3666', 'walking on incline treadmill', 'LOCOMOTION_CARDIO', FALSE),   -- id 1325
                             ('3637', 'wheel run', 'LOCOMOTION_CARDIO', FALSE),   -- id 1333
                             ('1311', 'wide hand push up', 'HORIZONTAL_PUSH', FALSE),   -- id 1334
                             ('2363', 'wide-grip chest dip on high parallel bars', 'HORIZONTAL_PUSH', FALSE),   -- id 1335
                             ('1429', 'wide grip pull-up', 'VERTICAL_PULL', FALSE),   -- id 1336
                             ('1367', 'wide grip rear pull-up', 'VERTICAL_PULL', FALSE),   -- id 1337
                             ('0847', 'weighted seated bicep curl (on stability ball)', 'ELBOW_FLEXION', TRUE),   -- id 1338
                             ('1428', 'wrist circles', 'MOBILITY', FALSE),   -- id 1373
                             ('0858', 'wind sprints', 'LOCOMOTION_CARDIO', FALSE),   -- id 1374
                             ('1604', 'world greatest stretch', 'MOBILITY', FALSE);   -- id 1375

-- Los 1221 ejercicios tienen que existir con el mismo nombre.
DO $$
DECLARE faltan TEXT;
BEGIN
SELECT string_agg(t.source_code || ' ' || t.name_en, ', ') INTO faltan
FROM v26_patrones t
         LEFT JOIN exercises e ON e.source_code = t.source_code AND e.name_en = t.name_en
WHERE e.exercise_id IS NULL;
IF faltan IS NOT NULL THEN
        RAISE EXCEPTION 'V26: ejercicios que no existen o cambiaron de nombre: %', faltan;
END IF;
END $$;

UPDATE exercises e SET
                       movement_pattern = t.movement_pattern,
                       is_isolation     = t.is_isolation
    FROM v26_patrones t
WHERE e.source_code = t.source_code AND e.name_en = t.name_en;

-- 3. RESTRICCIONES ----------------------------------------------------
ALTER TABLE exercises DROP CONSTRAINT IF EXISTS ck_exercises_movement_pattern;
ALTER TABLE exercises ADD CONSTRAINT ck_exercises_movement_pattern
    CHECK (movement_pattern IS NULL OR movement_pattern IN (
                                                            'KNEE_DOMINANT',
                                                            'HIP_HINGE',
                                                            'HIP_EXTENSION',
                                                            'HIP_ABDUCTION_ADDUCTION',
                                                            'ANKLE_PLANTAR_FLEXION',
                                                            'HORIZONTAL_PUSH',
                                                            'VERTICAL_PUSH',
                                                            'HORIZONTAL_PULL',
                                                            'VERTICAL_PULL',
                                                            'SHOULDER_RAISE',
                                                            'HORIZONTAL_ADDUCTION',
                                                            'SHOULDER_EXTENSION',
                                                            'SCAPULAR_ELEVATION',
                                                            'ELBOW_FLEXION',
                                                            'ELBOW_EXTENSION',
                                                            'WRIST_FOREARM',
                                                            'TRUNK_FLEXION',
                                                            'TRUNK_EXTENSION',
                                                            'TRUNK_ROTATION_LATERAL_FLEXION',
                                                            'CORE_STABILITY',
                                                            'LOCOMOTION_CARDIO',
                                                            'MOBILITY',
                                                            'BALANCE_CONTROL',
                                                            'OTHER'
        ));

-- Un ejercicio activo tiene que estar clasificado; uno inactivo puede no estarlo.
ALTER TABLE exercises DROP CONSTRAINT IF EXISTS ck_exercises_activo_clasificado;
ALTER TABLE exercises ADD CONSTRAINT ck_exercises_activo_clasificado
    CHECK (NOT is_active OR (movement_pattern IS NOT NULL AND is_isolation IS NOT NULL));

-- 4. COMPROBACION -----------------------------------------------------
DO $$
DECLARE n_clasificados INT; n_clases INT; n_aislados INT;
BEGIN
SELECT count(*) FILTER (WHERE movement_pattern IS NOT NULL),
        count(DISTINCT movement_pattern),
       count(*) FILTER (WHERE is_isolation)
INTO n_clasificados, n_clases, n_aislados
FROM exercises WHERE is_active;
IF n_clasificados <> 1221 OR n_clases <> 24 OR n_aislados <> 428 THEN
        RAISE EXCEPTION 'V26: recuentos inesperados (clasificados %, clases %, aislados %)',
              n_clasificados, n_clases, n_aislados;
END IF;
    RAISE NOTICE 'V26: % activos clasificados en % clases, % aislados',
          n_clasificados, n_clases, n_aislados;
END $$;
