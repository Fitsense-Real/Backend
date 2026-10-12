-- =====================================================================
-- FitSense MVP 1.0 - V22: codigos de equipamiento que faltaban (14)
--
-- POR QUE
-- El catalogo tiene 87 ejercicios activos que necesitan un implemento que
-- equipment_types no sabe nombrar: un banco, una barra fija, unas correas de
-- suspension, unas paralelas, unas anillas. Todos estaban registrados como
-- 'body weight', asi que cualquier participante los recibia: "bench dip",
-- "inverted row with straps", "captains chair straight leg raise" o
-- "muscle up" le llegaban a alguien que solo tiene una esterilla.
--
-- Sin estos codigos las dos unicas salidas eran malas: dejarlos activos y
-- aceptar que le lleguen a quien no puede hacerlos, o desactivarlos y perder
-- 87 ejercicios, incluidos los de quien SI tiene un banco en casa.
--
-- Con estos codigos, ningun ejercicio tiene que desactivarse: basta con
-- registrar lo que necesita y que el participante diga si lo tiene.
--
-- Los que necesitan DOS implementos a la vez (120 en el catalogo, 105
-- activos: 31 mancuerna + balon, 23 banco + mancuerna, 20 barra + banco...) no
-- caben en un unico equipment_id. Para eso esta V23, la tabla N:M
-- exercise_equipment_requirements, y V24 la rellena desde la revision.
--
-- SOBRE requires_gym
-- Ya no filtra nada. El conjunto elegible se calcula solo con el equipamiento
-- que el participante declara tener; training_location se guarda y entra en el
-- prompt, pero no decide que ejercicios existen para el. Aqui requires_gym
-- sobrevive como metadato del onboarding: que opciones conviene enseñar
-- primero. Por eso lo que es razonable tener en casa (banco, barra fija,
-- correas, BOSU, tabla, rueda, step, cajon, bici, caminadora) entra como
-- false, y 'parallel bars', 'gymnastic rings', 'landmine' y 'captains chair'
-- como true.
--
-- NO editar ni reformatear este archivo despues de aplicarlo
-- (checksum de Flyway, incidente de V18 y V19).
-- =====================================================================

INSERT INTO equipment_types (code, name_es, requires_gym, display_order) VALUES
                                                                             ('bench',             'Banco',                  FALSE, 18),
                                                                             ('pull-up bar',       'Barra fija',             FALSE, 19),
                                                                             ('suspension straps', 'Correas de suspension',  FALSE, 20),
                                                                             ('parallel bars',     'Barras paralelas',       TRUE,  21),
                                                                             ('gymnastic rings',   'Anillas',                TRUE,  22),
                                                                             -- Un BOSU no es un balon de estabilidad: quien tiene fitball no tiene
                                                                             -- BOSU. V9 los habia mandado a 'other'; los dos activos que lo usan
                                                                             -- ("cable side bend crunch (bosu ball)", "dumbbell biceps curl v sit on
                                                                             -- bosu ball") estaban registrados como cable y mancuerna y le llegaban a
                                                                             -- cualquiera con polea o mancuernas.
                                                                             ('bosu ball',         'BOSU',                   FALSE, 23),
                                                                             -- Implementos que el catalogo activo usa y que estaban escondidos en
                                                                             -- 'body weight', 'leverage machine' o 'barbell'. "balance board" estaba
                                                                             -- como peso corporal y le llegaba a cualquiera; "walking on incline
                                                                             -- treadmill" y "stationary bike walk" como maquina de palanca; "landmine
                                                                             -- 180" solo como barra.
                                                                             ('balance board',     'Tabla de equilibrio',    FALSE, 24),
                                                                             ('ab wheel',          'Rueda abdominal',        FALSE, 25),
                                                                             ('step platform',     'Step / plataforma',      FALSE, 26),
                                                                             ('plyo box',          'Cajon pliometrico',      FALSE, 27),
                                                                             ('stationary bike',   'Bicicleta estatica',     FALSE, 28),
                                                                             ('treadmill',         'Caminadora',             FALSE, 29),
                                                                             ('landmine',          'Landmine',               TRUE,  30),
                                                                             ('captains chair',    'Estacion de elevacion de piernas', TRUE, 31)
    ON CONFLICT (code) DO NOTHING;


-- Comprobacion dura: sin estos catorce codigos, V24 no puede registrar los
-- requisitos y fallaria a mitad dejando el catalogo a medio corregir.
DO $$
DECLARE
encontrados INTEGER;
BEGIN
SELECT COUNT(*) INTO encontrados
FROM equipment_types
WHERE code IN ('bench', 'pull-up bar', 'suspension straps',
               'parallel bars', 'gymnastic rings', 'bosu ball',
               'balance board', 'ab wheel', 'step platform', 'plyo box',
               'stationary bike', 'treadmill', 'landmine', 'captains chair');

IF encontrados <> 14 THEN
        RAISE EXCEPTION 'V22: se esperaban 14 codigos nuevos y hay %', encontrados;
END IF;

    RAISE NOTICE 'V22: 14 codigos de equipamiento anadidos';
END $$;
