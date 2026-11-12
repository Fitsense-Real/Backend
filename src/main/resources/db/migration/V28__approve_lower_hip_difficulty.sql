-- =====================================================================
-- FitSense MVP 1.0 - V28: primeras aprobaciones humanas de dificultad
--                         slot LOWER_HIP, perfil HOME de prueba
--
-- ESTADO: CANDIDATA A DESPLIEGUE; NO APLICADA NI VERIFICADA CONTRA LA BD REAL.
--
-- QUE HACE
--   Inserta CUATRO eventos APPROVED de nivel 1 en
--   exercise_difficulty_approvals, creada por la V27.
--   NO toca exercises.difficulty_level. NO cambia ninguna consulta Java.
--
-- DE DONDE SALEN ESTAS CUATRO FILAS
--   Revision independiente de Juan y Daniel, el 10/10/2026, sobre SIETE
--   ejercicios del grupo A, seleccionados entre QUINCE candidatos historicos
--   de LOWER_HIP. Los quince eran evidencia ya gastada en rondas anteriores;
--   el grupo A son los siete cuyo implemento principal es peso corporal o
--   banda. El material entregado NO llevaba nivel propuesto por el
--   clasificador, ni la regla que lo decidio, ni la dificultad heredada.
--   Material: revision-LOWER_HIP.txt
--   SHA-256: 816efc4834e3ebc9723b9b645f3ca3b1520717f40835f69e095501442ecf3ed0
--
--   Criterio aplicado: criterio-dificultad-v5.md
--   SHA-256: 0e7caac01260e63ad54fa3f24c165e74673430082d58c860344949c8ff8a59ec
--
--   El criterio humano v5 es utilizable aunque el CLASIFICADOR v5 no superara
--   su validacion (kappa 0,585, las cuatro condiciones fallidas). Son dos
--   artefactos distintos: el documento define la escala, el programa intenta
--   implementarla.
--
--   Expediente de evidencia: expediente-LOWER_HIP-20261010.md
--   SHA-256: 49237ceb6c1a21190bf56fdeb702d13032a8bb3b3a5faa40742d3e94c9a5a205
--
-- QUE NO SE CARGA, Y POR QUE
--   125  band pull through      necesita un anclaje seguro que la V23 no modela
--   1073 low glute bridge       duplicado funcional del 872 (92,8 % de
--                               coincidencia en instrucciones); no se elimina
--                               del catalogo, solo no cuenta como alternativa
--   1157 band hip thrusts       la instruccion del catalogo describe mal la
--                               ejecucion; material del Step 3C
--
-- LIMITACIONES, con sus numeros
--   Cubre el perfil HOME de prueba {peso corporal, mancuerna, banda}, NO todos
--   los perfiles HOME. De los cuatro aprobados, solo UNO (776) no exige trabajo
--   en el suelo: un perfil sin suelo se queda con 1 candidato frente al minimo
--   de 3 y NO es factible. Sin banda quedarian 3, el minimo exacto.
--
--   ESTA MIGRACION NO AUTORIZA CAMBIAR LA SELECCION EN JAVA. Eso exige >= 3
--   aprobados en LOS CUATRO slots obligatorios; LOWER_KNEE, UPPER_PUSH y
--   UPPER_PULL siguen con cero.
--
-- Una vez ejecutada con Flyway, NO modificar ni reformatear este archivo.
-- =====================================================================

-- PRECONDICIONES
DO $$
DECLARE
faltan    TEXT;
    inactivo  TEXT;
    distintos TEXT;
    previos   INT;
BEGIN
    IF to_regclass('public.exercise_difficulty_approvals') IS NULL THEN
        RAISE EXCEPTION 'V28: no existe exercise_difficulty_approvals; falta la V27';
END IF;

    -- Los cuatro ids tienen que existir y estar activos.
SELECT string_agg(x::TEXT, ', ' ORDER BY x) INTO faltan
FROM unnest(ARRAY[122, 776, 872, 878]) AS x
WHERE NOT EXISTS (SELECT 1 FROM public.exercises e WHERE e.exercise_id = x);
IF faltan IS NOT NULL THEN
        RAISE EXCEPTION 'V28: estos exercise_id no existen: %', faltan;
END IF;

SELECT string_agg(e.exercise_id::TEXT, ', ' ORDER BY e.exercise_id) INTO inactivo
FROM public.exercises e
WHERE e.exercise_id IN (122, 776, 872, 878) AND e.is_active IS DISTINCT FROM TRUE;
IF inactivo IS NOT NULL THEN
        RAISE EXCEPTION 'V28: estos exercise_id no estan activos: %', inactivo;
END IF;


    -- IDENTIDAD: los ids tienen que seguir siendo LOS EJERCICIOS REVISADOS.
    -- Un id es un numero; lo que se aprobo es un movimiento concreto. Si el
    -- catalogo cambio de source_code, de nombre o de patron bajo ese id, esta
    -- aprobacion ya no le corresponde y la migracion no debe entrar.
    -- Valores tomados de catalogo-v25.csv (d247c837...) y
    -- patrones-catalogo-final.csv (1813e987...), las fuentes con las que se
    -- preparo el material de revision.
SELECT string_agg(
               esp.id || ': ' || string_agg_inner, '; ' ORDER BY esp.id)
INTO distintos
FROM (VALUES
          (122::BIGINT, '1408', 'band hip lift',         'HIP_EXTENSION'),
          (776,         '3292', 'elevator',              'HIP_HINGE'),
          (872,         '0484', 'hip raise (bent knee)', 'HIP_EXTENSION'),
          (878,         '3561', 'glute bridge march',    'HIP_EXTENSION')
     ) AS esp(id, source_code, name_en, movement_pattern)
         JOIN public.exercises e ON e.exercise_id = esp.id
         CROSS JOIN LATERAL (
    SELECT string_agg(d, ', ') AS string_agg_inner
    FROM (
             SELECT 'source_code ' || coalesce(e.source_code,'NULL') ||
                    ' != ' || esp.source_code AS d
                 WHERE e.source_code IS DISTINCT FROM esp.source_code
             UNION ALL
             SELECT 'name_en ' || coalesce(e.name_en,'NULL') ||
                 ' != ' || esp.name_en
             WHERE e.name_en IS DISTINCT FROM esp.name_en
             UNION ALL
             SELECT 'movement_pattern ' || coalesce(e.movement_pattern,'NULL') ||
                 ' != ' || esp.movement_pattern
             WHERE e.movement_pattern IS DISTINCT FROM esp.movement_pattern
         ) AS difs
        ) AS agg
WHERE agg.string_agg_inner IS NOT NULL;

IF distintos IS NOT NULL THEN
        RAISE EXCEPTION 'V28: la identidad de los ejercicios no coincide con lo revisado -> %', distintos;
END IF;

    -- Append-only: si ya hubiera eventos para estos ids, esta migracion no es
    -- la primera carga y hay que revisar antes de anadir nada.
SELECT count(*) INTO previos
FROM public.exercise_difficulty_approvals
WHERE exercise_id IN (122, 776, 872, 878);
IF previos > 0 THEN
        RAISE EXCEPTION 'V28: ya existen % eventos para estos ids; revisar antes de cargar', previos;
END IF;
END $$;

-- Las cuatro aprobaciones. Nivel 1 en las cuatro, dos revisores distintos,
-- huella del criterio aplicado y referencia al expediente.
INSERT INTO public.exercise_difficulty_approvals
(exercise_id, decision, approved_difficulty_level,
 criterion_version, criterion_sha256,
 reviewer_1, reviewer_2, evidence_reference, notes)
VALUES
    (122, 'APPROVED', 1,
     'criterio-dificultad-v5.md',
     '0e7caac01260e63ad54fa3f24c165e74673430082d58c860344949c8ff8a59ec',
     'Juan', 'Daniel',
     'expediente-LOWER_HIP-20261010.md sha256=49237ceb6c1a21190bf56fdeb702d13032a8bb3b3a5faa40742d3e94c9a5a205',
     'band hip lift. Revision independiente 10/10/2026: Juan 1, Daniel 1. Sin motivo escrito por fila en el momento de la valoracion.'),

    (776, 'APPROVED', 1,
     'criterio-dificultad-v5.md',
     '0e7caac01260e63ad54fa3f24c165e74673430082d58c860344949c8ff8a59ec',
     'Juan', 'Daniel',
     'expediente-LOWER_HIP-20261010.md sha256=49237ceb6c1a21190bf56fdeb702d13032a8bb3b3a5faa40742d3e94c9a5a205',
     'elevator. Revision independiente 10/10/2026: Juan 1, Daniel 1. Unico de los cuatro que NO exige trabajo en el suelo.'),

    (872, 'APPROVED', 1,
     'criterio-dificultad-v5.md',
     '0e7caac01260e63ad54fa3f24c165e74673430082d58c860344949c8ff8a59ec',
     'Juan', 'Daniel',
     'expediente-LOWER_HIP-20261010.md sha256=49237ceb6c1a21190bf56fdeb702d13032a8bb3b3a5faa40742d3e94c9a5a205',
     'hip raise (bent knee). DECISION PREVIA: ya figuraba con nivel 1 en manifiesto-decisiones-congeladas.csv, de criterio-dificultad-v5.md seccion 4.2. La revision del 10/10 coincide; es comprobacion de consistencia, no evidencia nueva.'),

    (878, 'APPROVED', 1,
     'criterio-dificultad-v5.md',
     '0e7caac01260e63ad54fa3f24c165e74673430082d58c860344949c8ff8a59ec',
     'Juan', 'Daniel',
     'expediente-LOWER_HIP-20261010.md sha256=49237ceb6c1a21190bf56fdeb702d13032a8bb3b3a5faa40742d3e94c9a5a205',
     'glute bridge march. Revision independiente 10/10/2026: Juan 1, Daniel 1. Variante con alternancia de apoyo.');

-- POSCONDICIONES: exactamente cuatro eventos nuevos, cuatro vigentes, y
-- exercises.difficulty_level sin tocar por esta migracion.
DO $$
DECLARE
n_eventos INT;
    n_vista   INT;
BEGIN
SELECT count(*) INTO n_eventos
FROM public.exercise_difficulty_approvals
WHERE exercise_id IN (122, 776, 872, 878) AND decision = 'APPROVED';
IF n_eventos <> 4 THEN
        RAISE EXCEPTION 'V28: se esperaban 4 eventos APPROVED y hay %', n_eventos;
END IF;

SELECT count(*) INTO n_vista
FROM public.v_exercises_manually_approved
WHERE exercise_id IN (122, 776, 872, 878);
IF n_vista <> 4 THEN
        RAISE EXCEPTION 'V28: se esperaban 4 filas vigentes en la vista y hay %', n_vista;
END IF;
END $$;

-- Diagnostico post-deploy (ejecutar manualmente, NO forma parte de V28):
-- SELECT exercise_id, name_en, approved_difficulty_level, criterion_version
--   FROM public.v_exercises_manually_approved ORDER BY exercise_id;   -- 4 filas
-- SELECT count(*) FROM public.exercise_difficulty_approvals;          -- 4
