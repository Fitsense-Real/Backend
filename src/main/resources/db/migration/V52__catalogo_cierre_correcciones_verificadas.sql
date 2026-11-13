-- =====================================================================
-- FitSense - V52: correcciones de datos del CIERRE DEL CATALOGO (13/11/2026)
--
-- Cada cambio se adjudico con tres miradas sobre la animacion COMPLETA del GIF original: segunda revision visual
-- independiente (20 lotes), verificacion independiente de cada propuesta (tercera mirada) y adjudicacion del
-- coordinador para los cambios de seguridad. Evidencia por cambio: docs/FitSense/catalogo_integral/cierre/
-- (decisiones_v52.tsv, revision2_consolidada.tsv, verificacion3_consolidada.tsv).
--
-- NO baja ninguna dificultad (V27: requiere aprobacion humana). NO cambia el significado de ninguna restriccion del
-- perfil. NO activa ni desactiva ejercicios. Cada ejercicio exige su estado anterior exacto: si no coincide y no esta
-- ya corregido, la migracion FALLA.
-- Cambios: {'axial_load': 14, 'default_prescription': 16, 'difficulty_level': 63, 'equipo': 4, 'high_impact': 5, 'impact_level': 9, 'instructions_es': 329, 'is_stretch': 5, 'movement_pattern': 24, 'name_es': 40, 'requires_floor': 4, 'requires_inversion': 6, 'requires_kneeling': 6, 'requires_overhead': 26, 'zona': 10} en 442 ejercicios.
-- Generada por docs/FitSense/catalogo_integral/cierre/herramientas/gen_v52.py.
-- =====================================================================

-- 53 astride jumps (male): instructions_es: R2 L01 (ALTA): El texto pide bajar en sentadilla, saltar de forma explosiva y aterrizar con los pies a la anchura de los hombros; el GIF muestra un salto pequeño de pies junto / V3 V01 ACEPTAR: f1 de pie pies juntos brazos abajo; f7 pequeño vuelo con pies en punta; f13 pies muy separados y brazos a los lados algo por encima de la horizontal; no hay sen
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies juntos y los brazos a los lados del cuerpo. Da un salto pequeño y aterriza con los pies bastante más separados que la anchura de los hombros mientras subes los brazos hacia los lados hasta la altura de los hombros. Salta de nuevo para juntar los pies y bajar los brazos. Aterriza sobre la parte delantera de los pies con las rodillas ligeramente flexionadas. Mantén la duración indicada.', instruction_steps_es = '["Ponte de pie con los pies juntos y los brazos a los lados del cuerpo.", "Da un salto pequeño y aterriza con los pies bastante más separados que la anchura de los hombros mientras subes los brazos hacia los lados hasta la altura de los hombros.", "Salta de nuevo para juntar los pies y bajar los brazos.", "Aterriza sobre la parte delantera de los pies con las rodillas ligeramente flexionadas.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 53 AND name_en = 'astride jumps (male)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '227261f8f16caffc1f0deab675921c82';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 53 AND name_en = 'astride jumps (male)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '1dbd5a50526a11fa6fcb2b25367667ee') THEN
    RAISE EXCEPTION 'V52: ejercicio 53 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 54 back and forth step: name_es: R2 L02 (MEDIA): No se ve ningún fotograma con ambos pies en el aire, pero el ritmo lateral recuerda a un salto de patinador; si en la práctica hay salto el impacto sería MODERA / V3 V01 ACEPTAR: f1 y f13 apoyo a una pierna con la otra flexionada atrás y la mano bajando hasta el suelo junto al pie; f7 y f19 incorporado en el centro; desplazamiento latera | instructions_es: R2 L02 (MEDIA): El texto describe zancadas hacia delante y hacia atrás; el GIF muestra desplazamientos laterales con la pierna libre cruzada por detrás y una mano bajando hacia / V3 V01 MEJORAR: f1 y f13 apoyo a una pierna, la otra flexionada atrás y hacia fuera (no se ve que cruce por detrás), mano del lado de apoyo hasta el pie y el otro brazo atrás; 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Paso lateral de patinador con toque al suelo', instructions_es = 'Ponte de pie con las rodillas ligeramente flexionadas. Desplázate hacia un lado y apóyate sobre una sola pierna, llevando la otra pierna flexionada hacia atrás. Inclina el tronco hacia delante y baja hacia el pie de apoyo la mano del mismo lado, mientras el otro brazo se eleva hacia atrás. Incorpórate y desplázate hacia el lado contrario para repetir el gesto con la otra pierna y la otra mano. Alterna los lados a ritmo constante. Mantén la duración indicada.', instruction_steps_es = '["Ponte de pie con las rodillas ligeramente flexionadas.", "Desplázate hacia un lado y apóyate sobre una sola pierna, llevando la otra pierna flexionada hacia atrás.", "Inclina el tronco hacia delante y baja hacia el pie de apoyo la mano del mismo lado, mientras el otro brazo se eleva hacia atrás.", "Incorpórate y desplázate hacia el lado contrario para repetir el gesto con la otra pierna y la otra mano.", "Alterna los lados a ritmo constante.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 54 AND name_en = 'back and forth step' AND name_es = 'Paso hacia adelante y atrás' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'dd2de5e43eddbe6163f680dcade8cd87';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 54 AND name_en = 'back and forth step' AND name_es = 'Paso lateral de patinador con toque al suelo' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'bdb1520e2b444f162df9b9b629a38fac') THEN
    RAISE EXCEPTION 'V52: ejercicio 54 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 56 archer push up: instructions_es: R2 L04 (ALTA): El texto dice manos un poco más separadas que los hombros y extender un brazo hacia el lado paralelo al suelo (sugiere despegarlo); el GIF muestra las manos muy / V3 V01 ACEPTAR: f1-f24 ambas manos apoyadas en el suelo muy separadas todo el tiempo; f7 baja hacia la mano derecha de la imagen con el otro brazo estirado; f19 hacia el otro l
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate en posición de flexión con las manos apoyadas en el suelo mucho más separadas que los hombros y el cuerpo recto. Flexiona un codo y baja el pecho hacia esa mano, manteniendo el otro brazo estirado con la mano en el suelo. Empuja para volver al centro con los brazos extendidos. Repite hacia el otro lado manteniendo el core activado. Alterna los lados. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate en posición de flexión con las manos apoyadas en el suelo mucho más separadas que los hombros y el cuerpo recto.", "Flexiona un codo y baja el pecho hacia esa mano, manteniendo el otro brazo estirado con la mano en el suelo.", "Empuja para volver al centro con los brazos extendidos.", "Repite hacia el otro lado manteniendo el core activado.", "Alterna los lados.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 56 AND name_en = 'archer push up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '218e4ccee9752757055ee6dc6e5a95ea';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 56 AND name_en = 'archer push up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '96441472252b56b5c3b025d1ada813d1') THEN
    RAISE EXCEPTION 'V52: ejercicio 56 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 57 3/4 sit-up: instructions_es: R2 L05 (ALTA): El texto dice manos detrás de la cabeza con los codos hacia afuera y subir hasta un ángulo de 45 grados; el GIF muestra los puños junto a la frente con los codo / V3 V01 ACEPTAR: f1 tumbado con rodillas flexionadas, puños junto a la frente y codos hacia delante; f7 tronco casi vertical; el texto actual limita a 45 grados y codos hacia fu
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en el suelo con las rodillas flexionadas y los pies apoyados. Coloca las manos cerradas junto a la frente, con los codos hacia delante. Activa el abdomen y eleva el tronco del suelo hasta quedar casi sentado. Baja el tronco despacio hasta apoyar de nuevo la espalda. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en el suelo con las rodillas flexionadas y los pies apoyados.", "Coloca las manos cerradas junto a la frente, con los codos hacia delante.", "Activa el abdomen y eleva el tronco del suelo hasta quedar casi sentado.", "Baja el tronco despacio hasta apoyar de nuevo la espalda.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 57 AND name_en = '3/4 sit-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '800e583ab286e5d9c2adeb823a56752a';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 57 AND name_en = '3/4 sit-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ef422bc28c4ba5ee1eb533ea549695d2') THEN
    RAISE EXCEPTION 'V52: ejercicio 57 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 60 alternate heel touchers: name_es: R2 L08 (MEDIA): Vista frontal; no se aprecia alternancia ni inclinación lateral hacia los talones. El GIF no representa un heel touch; o se cambia el nombre o se cambia el GIF. / V3 V01 MEJORAR: f1 tumbado con brazos estirados junto al cuerpo hacia los pies; f7 tronco casi vertical, cabeza centrada, sin flexión lateral; f12 vuelta; un solo ciclo simétri | instructions_es: R2 L08 (MEDIA): El texto describe toques de talón alternos a derecha e izquierda con los hombros apenas despegados; el GIF muestra una elevación simétrica del tronco hasta casi / V3 V01 MEJORAR: f1 tumbado con el brazo visible estirado junto al cuerpo y la mano hacia los pies; f7 sentado casi vertical con la mano junto a la cadera: las manos no se desli
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Abdominal completo con brazos junto al cuerpo', instructions_es = 'Túmbate boca arriba en el suelo con las rodillas flexionadas y los pies apoyados. Coloca los brazos estirados junto al cuerpo, con las manos dirigidas hacia los pies. Activa el abdomen y eleva el tronco hasta quedar casi sentado, con los brazos estirados junto al cuerpo. Baja el tronco con control hasta la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en el suelo con las rodillas flexionadas y los pies apoyados.", "Coloca los brazos estirados junto al cuerpo, con las manos dirigidas hacia los pies.", "Activa el abdomen y eleva el tronco hasta quedar casi sentado, con los brazos estirados junto al cuerpo.", "Baja el tronco con control hasta la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 60 AND name_en = 'alternate heel touchers' AND name_es = 'Toques de talón alternos' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '05a3cc12665b44b5d394759cc1afb161';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 60 AND name_en = 'alternate heel touchers' AND name_es = 'Abdominal completo con brazos junto al cuerpo' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '0939dc9142bbfbbe2d19092cddaa0781') THEN
    RAISE EXCEPTION 'V52: ejercicio 60 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 61 arm slingers hanging bent knee legs: difficulty_level: R2 L09 (MEDIA): suspensión del peso corporal en correas; control del balanceo / V3 V01 ACEPTAR: f1 cuerpo suspendido de la barra con los brazos en correas y pies sin apoyo; f7 rodillas elevadas por encima de la cadera en suspensión; soportar el peso corpor
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 61 AND name_en = 'arm slingers hanging bent knee legs' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 61 AND name_en = 'arm slingers hanging bent knee legs' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 61 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 62 arm slingers hanging straight legs: difficulty_level: R2 L10 (ALTA): suspensión; fuerza de core alta con piernas rectas por encima de la horizontal / V3 V01 ACEPTAR: f1 suspendido en correas sin apoyo de pies; f7 piernas rectas elevadas por encima de la horizontal hacia la barra; suspensión y fuerza de core alta
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 62 AND name_en = 'arm slingers hanging straight legs' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 62 AND name_en = 'arm slingers hanging straight legs' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 62 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 63 arms overhead full sit-up (male): instructions_es: R2 L11 (ALTA): Dice rodillas flexionadas y pies apoyados y que el torso queda erguido; el GIF muestra las piernas estiradas en el suelo todo el tiempo y el tronco termina incl / V3 W01 ACEPTAR: f1 tumbado con piernas estiradas y brazos sobre la cabeza apoyados en el suelo; f4 tronco subiendo con piernas rectas; f7 tronco plegado sobre las piernas con b
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en el suelo con las piernas estiradas y los brazos extendidos por encima de la cabeza, apoyados en el suelo. Activa el abdomen y lleva los brazos hacia delante mientras despegas el tronco del suelo. Sube hasta quedar sentado e inclina el tronco sobre las piernas, con los brazos extendidos hacia los pies. Baja con control hasta volver a tumbarte con los brazos por encima de la cabeza. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en el suelo con las piernas estiradas y los brazos extendidos por encima de la cabeza, apoyados en el suelo.", "Activa el abdomen y lleva los brazos hacia delante mientras despegas el tronco del suelo.", "Sube hasta quedar sentado e inclina el tronco sobre las piernas, con los brazos extendidos hacia los pies.", "Baja con control hasta volver a tumbarte con los brazos por encima de la cabeza.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 63 AND name_en = 'arms overhead full sit-up (male)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '8b38c1196268991c8b5d956e5621d1e9';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 63 AND name_en = 'arms overhead full sit-up (male)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '7f0424ed9100820665aba3b533fac13a') THEN
    RAISE EXCEPTION 'V52: ejercicio 63 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 64 archer pull up: difficulty_level: R2 L12 (MEDIA): fuerza relativa alta; suspensión / V3 W01 ACEPTAR: f1 colgado de la barra con brazos extendidos y pies sin apoyo; f7 barbilla sobre la barra con el cuerpo desplazado hacia una mano: dominada con suspensión, perf
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 3
   WHERE exercise_id = 64 AND name_en = 'archer pull up' AND difficulty_level = 2;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 64 AND name_en = 'archer pull up' AND difficulty_level = 3) THEN
    RAISE EXCEPTION 'V52: ejercicio 64 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 65 back lever: instructions_es: R2 L13 (MEDIA): El texto describe colgarse, llevar las rodillas al pecho y elevar las piernas rectas hasta la horizontal; el GIF solo muestra la posición horizontal mantenida,  / V3 W01 ACEPTAR: f1-f12 posición horizontal boca abajo mantenida, brazos rectos detrás del tronco agarrando la barra, piernas juntas y extendidas; solo cambia el sombreado muscu | default_prescription: R2 L13 (MEDIA): Los brazos no están por encima de la cabeza: el hombro está en extensión detrás del tronco, por eso sobra OH. La entrada a la posición (que suele pasar por inve / V3 W01 ACEPTAR: f1-f12 misma posición de plancha trasera sin desplazamiento: isométrico puro
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Sujeta la barra de dominadas con agarre firme y los brazos extendidos. Coloca el cuerpo en horizontal, boca abajo, con los brazos rectos detrás del tronco y las piernas juntas y extendidas. Lleva los omóplatos hacia abajo y aprieta core y glúteos para mantener una línea recta de la cabeza a los pies. Mantén la posición sin dejar caer la cadera. Mantén la duración indicada.', instruction_steps_es = '["Sujeta la barra de dominadas con agarre firme y los brazos extendidos.", "Coloca el cuerpo en horizontal, boca abajo, con los brazos rectos detrás del tronco y las piernas juntas y extendidas.", "Lleva los omóplatos hacia abajo y aprieta core y glúteos para mantener una línea recta de la cabeza a los pies.", "Mantén la posición sin dejar caer la cadera.", "Mantén la duración indicada."]'::jsonb, default_prescription = 'DURATION'
   WHERE exercise_id = 65 AND name_en = 'back lever' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'cf1ad23e0a7add15a4750efcadfe5ebd' AND default_prescription = 'SETS_REPS';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 65 AND name_en = 'back lever' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '13294e5e5b0f750f60c709175dd542af' AND default_prescription = 'DURATION') THEN
    RAISE EXCEPTION 'V52: ejercicio 65 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 67 all fours squad stretch: instructions_es: R2 L15 (ALTA): El texto pide extender la pierna atrás con el pie en flexión y bajar la cadera hacia el suelo; el GIF muestra que una mano sujeta el pie y acerca el talón al gl / V3 W01 MEJORAR: f1 cuatro apoyos; f4-f7 muslo elevado atrás casi horizontal, rodilla flexionada y una mano sujetando el pie junto al glúteo, sin bajar la cadera; la propuesta e
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate a cuatro apoyos en el suelo, con las manos bajo los hombros y las rodillas bajo las caderas. Flexiona una rodilla y eleva esa pierna hacia atrás hasta que el muslo quede casi horizontal. Lleva una mano hacia atrás, sujeta el pie y acerca el talón al glúteo hasta notar el estiramiento en la parte delantera del muslo, apoyándote en la otra mano y la otra rodilla. Mantén la posición y después suelta el pie con control; repite con la otra pierna. Mantén la duración indicada.', instruction_steps_es = '["Colócate a cuatro apoyos en el suelo, con las manos bajo los hombros y las rodillas bajo las caderas.", "Flexiona una rodilla y eleva esa pierna hacia atrás hasta que el muslo quede casi horizontal.", "Lleva una mano hacia atrás, sujeta el pie y acerca el talón al glúteo hasta notar el estiramiento en la parte delantera del muslo, apoyándote en la otra mano y la otra rodilla.", "Mantén la posición y después suelta el pie con control; repite con la otra pierna.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 67 AND name_en = 'all fours squad stretch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ddaf483692de4494452e53c178f4987b';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 67 AND name_en = 'all fours squad stretch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '46b844ca57f361a223ef153c148768bc') THEN
    RAISE EXCEPTION 'V52: ejercicio 67 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 70 assisted motion russian twist: instructions_es: R2 L18 (MEDIA): El texto dice sostener el balón frente al pecho y alternar giros a derecha e izquierda; el GIF muestra recibir el balón lanzado desde delante, girar hacia un so / V3 W01 MEJORAR: f1 balón junto al compañero sentado enfrente; f5 balón llega a las manos; f7 tronco girado con el balón junto a la cadera; f9-f11 lo devuelve; contenido de la p
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en el suelo frente a un compañero, con las rodillas flexionadas y los talones apoyados. Inclina el tronco ligeramente hacia atrás con la espalda recta y el abdomen activado. Recibe con ambas manos el balón medicinal que te lanza tu compañero desde delante. Gira el tronco hacia un lado llevando el balón junto a la cadera y vuelve al frente para devolverlo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en el suelo frente a un compañero, con las rodillas flexionadas y los talones apoyados.", "Inclina el tronco ligeramente hacia atrás con la espalda recta y el abdomen activado.", "Recibe con ambas manos el balón medicinal que te lanza tu compañero desde delante.", "Gira el tronco hacia un lado llevando el balón junto a la cadera y vuelve al frente para devolverlo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 70 AND name_en = 'assisted motion russian twist' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'fe98e684d7c85b243fd3edd95563e8ed';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 70 AND name_en = 'assisted motion russian twist' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a07f2578e4f38ddd2d003a566117e03f') THEN
    RAISE EXCEPTION 'V52: ejercicio 70 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 71 alternate lateral pulldown: name_es: R2 L19 (ALTA): El GIF no alterna brazos: solo trabaja uno, por eso «alterno» no describe lo visto. Se sienta en un banco plano; cualquier asiento sirve, no se añade como requi / V3 Y01 ACEPTAR: f1-f12 solo trabaja el brazo derecho desde arriba (f1 brazo estirado hacia la polea alta) hasta f7 (codo junto al costado, mano a la altura del hombro); el braz | instructions_es: R2 L19 (ALTA): El texto habla de agarraderas con las dos manos, agarre prono, inclinarse hacia atrás y llevar las agarraderas al pecho; el GIF muestra un solo brazo con un asa / V3 Y01 ACEPTAR: f1 un solo asa con la mano derecha y brazo estirado sobre la cabeza; f7 codo junto al costado y mano a la altura del hombro; tronco erguido en todo el GIF; el t
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Jalón unilateral en polea alta sentado', instructions_es = 'Siéntate en un banco bajo una polea alta con la espalda recta y los pies apoyados en el suelo. Sujeta el asa con una mano, con el brazo estirado por encima de la cabeza. Tira del asa hacia abajo llevando el codo junto al costado hasta que la mano quede a la altura del hombro. Vuelve con control estirando el brazo hacia la polea. Completa las repeticiones y cambia de brazo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en un banco bajo una polea alta con la espalda recta y los pies apoyados en el suelo.", "Sujeta el asa con una mano, con el brazo estirado por encima de la cabeza.", "Tira del asa hacia abajo llevando el codo junto al costado hasta que la mano quede a la altura del hombro.", "Vuelve con control estirando el brazo hacia la polea.", "Completa las repeticiones y cambia de brazo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 71 AND name_en = 'alternate lateral pulldown' AND name_es = 'Jalón lateral alterno' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '94e4be54f471d0ba8473d129a7acf282';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 71 AND name_en = 'alternate lateral pulldown' AND name_es = 'Jalón unilateral en polea alta sentado' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b76a2045f06a2b95a55cd88e790ac243') THEN
    RAISE EXCEPTION 'V52: ejercicio 71 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 74 assisted parallel close grip pull-up: instructions_es: R2 L02 (ALTA): El texto dice cuélgate con los pies sin tocar el suelo y no menciona la almohadilla; el GIF muestra las rodillas apoyadas en la almohadilla de asistencia de la  / V3 V01 ACEPTAR: f1-f12 rodillas apoyadas en la almohadilla de asistencia todo el movimiento, manos juntas en las asas; f7 brazos estirados, f1 cabeza a la altura de las asas; e
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ajusta el contrapeso de la máquina de dominadas asistidas. Sujeta las asas superiores con agarre cerrado y apoya las rodillas en la almohadilla de asistencia, con los brazos estirados por encima de la cabeza. Tira del cuerpo hacia arriba flexionando los codos y manteniéndolos cerca del cuerpo. Sube hasta que la cabeza llegue a la altura de las asas. Baja de forma controlada hasta estirar los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ajusta el contrapeso de la máquina de dominadas asistidas.", "Sujeta las asas superiores con agarre cerrado y apoya las rodillas en la almohadilla de asistencia, con los brazos estirados por encima de la cabeza.", "Tira del cuerpo hacia arriba flexionando los codos y manteniéndolos cerca del cuerpo.", "Sube hasta que la cabeza llegue a la altura de las asas.", "Baja de forma controlada hasta estirar los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 74 AND name_en = 'assisted parallel close grip pull-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '6171cb34f688e3402ca1e275e9f1d2f7';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 74 AND name_en = 'assisted parallel close grip pull-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '9fb1bbaab6d42d122f80cb4e3aabe75a') THEN
    RAISE EXCEPTION 'V52: ejercicio 74 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 75 assisted pull-up: instructions_es: R2 L03 (ALTA): El texto dice «Cuélgate con los brazos completamente extendidos y los pies sin tocar el suelo» y omite que en el GIF las rodillas se apoyan en la almohadilla de / V3 V01 ACEPTAR: f1-f12 rodillas en la almohadilla con pies libres detrás; f7 brazos extendidos sobre la cabeza; f1 subida con la cabeza a la altura de los asideros; el texto ac
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ajusta el peso de asistencia de la máquina. Sube a la máquina y apoya las rodillas en la almohadilla, con los pies libres detrás. Agarra los asideros altos con los brazos extendidos por encima de la cabeza. Tira del cuerpo hacia arriba llevando los codos hacia abajo hasta que la barbilla llegue a la altura de los asideros. Baja con control hasta extender los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ajusta el peso de asistencia de la máquina.", "Sube a la máquina y apoya las rodillas en la almohadilla, con los pies libres detrás.", "Agarra los asideros altos con los brazos extendidos por encima de la cabeza.", "Tira del cuerpo hacia arriba llevando los codos hacia abajo hasta que la barbilla llegue a la altura de los asideros.", "Baja con control hasta extender los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 75 AND name_en = 'assisted pull-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'fc1d8d66282db46b1452ccef9b09a690';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 75 AND name_en = 'assisted pull-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'cad5f1a16e09481523cfdd251412ad18') THEN
    RAISE EXCEPTION 'V52: ejercicio 75 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 77 assisted standing pull-up: instructions_es: R2 L05 (ALTA): El texto dice ponerse de pie frente a la máquina; el GIF muestra al usuario de pie sobre la plataforma de asistencia, que acompaña el movimiento. El tipo de aga / V3 V01 ACEPTAR: f1 y f7 de pie sobre la plataforma de asistencia, que sube y baja con el cuerpo; el texto actual dice de pie frente a la máquina sin mencionar la plataforma; ag
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ajusta el peso de asistencia de la máquina. Súbete de pie a la plataforma de asistencia y sujeta las asas superiores con los brazos extendidos por encima de la cabeza. Tira con la espalda y los brazos para subir el cuerpo hasta que la cabeza llegue a la altura de las manos. Baja despacio hasta extender los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ajusta el peso de asistencia de la máquina.", "Súbete de pie a la plataforma de asistencia y sujeta las asas superiores con los brazos extendidos por encima de la cabeza.", "Tira con la espalda y los brazos para subir el cuerpo hasta que la cabeza llegue a la altura de las manos.", "Baja despacio hasta extender los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 77 AND name_en = 'assisted standing pull-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b40e2b40484fc9ec43a56b1f0eefe0f7';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 77 AND name_en = 'assisted standing pull-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '77f839a3c1f81277f07c59afe2ee9206') THEN
    RAISE EXCEPTION 'V52: ejercicio 77 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 96 band single leg calf raise: instructions_es: R2 L10 (ALTA): El texto dice pies separados en el suelo, banda alrededor de los dedos y levantar el talón del suelo; el GIF muestra apoyo a una sola pierna sobre un escalón co / V3 V01 MEJORAR: f1 talón elevado sobre el escalón, f7 talón abajo; apoyo a una pierna con la otra flexionada atrás; la banda sale de una mano hacia abajo y su extremo inferior 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Súbete con un pie a un escalón, con el talón fuera del borde, y flexiona la otra pierna hacia atrás. Sujeta con una mano un extremo de la banda, que queda tensa hacia abajo, y apoya la otra mano en una pared o soporte estable para mantener el equilibrio. Baja el talón con control. Eleva el talón todo lo que puedas y haz una pausa arriba. Al terminar, cambia de pierna. Repite el número de repeticiones indicado.', instruction_steps_es = '["Súbete con un pie a un escalón, con el talón fuera del borde, y flexiona la otra pierna hacia atrás.", "Sujeta con una mano un extremo de la banda, que queda tensa hacia abajo, y apoya la otra mano en una pared o soporte estable para mantener el equilibrio.", "Baja el talón con control.", "Eleva el talón todo lo que puedas y haz una pausa arriba.", "Al terminar, cambia de pierna.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 96 AND name_en = 'band single leg calf raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '2787d2eb5811cc3a3970d540673f8133';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 96 AND name_en = 'band single leg calf raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a920c71b93b5474204999374bc3e80e1') THEN
    RAISE EXCEPTION 'V52: ejercicio 96 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 100 band reverse fly: instructions_es: R2 L14 (ALTA): El texto dice levantar los brazos hacia los lados hasta quedar paralelos al suelo (como una elevación lateral); en el GIF los brazos ya están a la altura de los / V3 W01 MEJORAR: f1 brazos estirados al frente a la altura de los hombros con la banda anclada delante; f7 brazos abiertos en horizontal; el texto actual implica elevar desde ab
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ancla la banda a un punto fijo a la altura del pecho, delante de ti. Ponte de pie con los pies ligeramente separados y sujeta la banda con los brazos estirados al frente, a la altura de los hombros. Abre los brazos estirados hacia los lados, sin bajarlos, hasta que queden en línea con los hombros, y junta los omóplatos. Vuelve despacio con los brazos al frente. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ancla la banda a un punto fijo a la altura del pecho, delante de ti.", "Ponte de pie con los pies ligeramente separados y sujeta la banda con los brazos estirados al frente, a la altura de los hombros.", "Abre los brazos estirados hacia los lados, sin bajarlos, hasta que queden en línea con los hombros, y junta los omóplatos.", "Vuelve despacio con los brazos al frente.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 100 AND name_en = 'band reverse fly' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'c6d7a3e228937a066532ac37f9961cbd';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 100 AND name_en = 'band reverse fly' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd94726676c78d1bdf053ccb3b13d0724') THEN
    RAISE EXCEPTION 'V52: ejercicio 100 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 101 band shoulder press: instructions_es: R2 L15 (MEDIA): El texto pide pies separados a la anchura de los hombros con la banda bajo los pies; el GIF muestra una postura escalonada (un pie adelantado y el otro atrás co / V3 W01 ACEPTAR: f1 postura escalonada con un pie adelantado y el otro atrás, banda bajo el pie delantero y extremos en las manos a la altura de los hombros; f7 brazos extendido
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con un pie adelantado y el otro atrás, y pisa el centro de la banda con un pie. Sujeta un extremo de la banda en cada mano a la altura de los hombros, con los codos flexionados. Empuja las manos hacia arriba hasta extender los brazos por encima de la cabeza. Baja las manos despacio hasta la altura de los hombros. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con un pie adelantado y el otro atrás, y pisa el centro de la banda con un pie.", "Sujeta un extremo de la banda en cada mano a la altura de los hombros, con los codos flexionados.", "Empuja las manos hacia arriba hasta extender los brazos por encima de la cabeza.", "Baja las manos despacio hasta la altura de los hombros.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 101 AND name_en = 'band shoulder press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a8bde0eca71d27541eed58b86ef4c693';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 101 AND name_en = 'band shoulder press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'c878052ddeb0e9cd4cc82e32e72d31e4') THEN
    RAISE EXCEPTION 'V52: ejercicio 101 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 104 band alternating v-up: instructions_es: R2 L18 (MEDIA): El texto dice sujetar con ambas manos una banda anclada; en el GIF la banda va sujeta a los tobillos y las manos quedan libres para tocar el pie elevado / V3 W01 MEJORAR: f1 una pierna elevada con tobillera y banda anclada abajo delante, manos tocando el pie, tronco reclinado; f4 transición; f7 sube la otra pierna; las manos nunc | difficulty_level: R2 L18 (MEDIA): equilibrio sentado en V; fuerza de core / V3 W01 ACEPTAR: f1-f12 tronco reclinado y sostenido en equilibrio sobre los glúteos sin volver al suelo mientras alterna elevaciones de pierna recta contra la banda: coordinaci
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en el suelo con una banda anclada delante, a baja altura, sujeta a los tobillos. Reclina el tronco y mantén el equilibrio sobre los glúteos con el abdomen activado. Eleva una pierna recta contra la resistencia de la banda y lleva las manos a tocar el pie. Baja la pierna con control sin dejar caer el tronco. Alterna los lados. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en el suelo con una banda anclada delante, a baja altura, sujeta a los tobillos.", "Reclina el tronco y mantén el equilibrio sobre los glúteos con el abdomen activado.", "Eleva una pierna recta contra la resistencia de la banda y lleva las manos a tocar el pie.", "Baja la pierna con control sin dejar caer el tronco.", "Alterna los lados.", "Repite el número de repeticiones indicado."]'::jsonb, difficulty_level = 2
   WHERE exercise_id = 104 AND name_en = 'band alternating v-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '615472051c23258a3ba243065474bf7d' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 104 AND name_en = 'band alternating v-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b70e456e211bcc90d2435487cfb45ea7' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 104 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 105 band assisted wheel rollerout: requires_overhead: R2 L19 (MEDIA): En f7 el cuerpo queda casi paralelo al suelo con los brazos alineados por delante de la cabeza (flexión de hombro casi completa), por eso se propone OH, coheren / V3 Y01 ACEPTAR: f6-f8 cuerpo casi paralelo al suelo con los brazos estirados alineados con el tronco por delante de la cabeza (flexión de hombro casi completa); f1 brazos bajo  | instructions_es: R2 L19 (MEDIA): El texto dice sostener las agarraderas de la banda y colocar la banda en el suelo frente a ti; el GIF muestra las manos en las asas de la rueda y la banda engan / V3 Y01 ACEPTAR: f1-f12 manos en las asas de la rueda; la banda sale del eje de la rueda y pasa junto a las rodillas hacia atrás (anclaje fuera de cuadro); f1 de rodillas con br
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = TRUE, instructions_es = 'Engancha la banda a la rueda abdominal y fija el otro extremo a un anclaje bajo situado detrás de ti. Arrodíllate en el suelo y sujeta las asas de la rueda con ambas manos, con los brazos estirados bajo los hombros. Activa el core y rueda hacia delante extendiendo el cuerpo y los brazos mientras la banda te ayuda a controlar el recorrido. Llega hasta donde puedas mantener la espalda sin hundirse y vuelve rodando hacia las rodillas. Repite el número de repeticiones indicado.', instruction_steps_es = '["Engancha la banda a la rueda abdominal y fija el otro extremo a un anclaje bajo situado detrás de ti.", "Arrodíllate en el suelo y sujeta las asas de la rueda con ambas manos, con los brazos estirados bajo los hombros.", "Activa el core y rueda hacia delante extendiendo el cuerpo y los brazos mientras la banda te ayuda a controlar el recorrido.", "Llega hasta donde puedas mantener la espalda sin hundirse y vuelve rodando hacia las rodillas.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 105 AND name_en = 'band assisted wheel rollerout' AND requires_overhead = FALSE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '8c6d0bb74d1446afee8ac077e5915acb';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 105 AND name_en = 'band assisted wheel rollerout' AND requires_overhead = TRUE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3a9f44b7336c9537c7f3a04c7fc4594b') THEN
    RAISE EXCEPTION 'V52: ejercicio 105 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 106 band bicycle crunch: instructions_es: R2 L20 (ALTA): El texto no menciona la banda en ningún paso; el GIF muestra una banda en los tobillos anclada delante, contra la que se extiende la pierna / V3 W01 ACEPTAR: f1-f12 tumbado con la parte alta de la espalda elevada, manos detrás de la cabeza y tobilleras con bandas ancladas abajo delante; f1/f7 pedaleo extendiendo cada
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca una banda alrededor de ambos tobillos y fija el otro extremo a un anclaje bajo delante de ti. Túmbate boca arriba en el suelo con las manos detrás de la cabeza y levanta los pies del suelo. Lleva una rodilla hacia el pecho mientras extiendes la otra pierna contra la resistencia de la banda y gira el tronco para acercar el codo contrario a la rodilla que sube. Cambia de lado con un movimiento de pedaleo controlado, manteniendo el abdomen activado. Alterna los lados. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca una banda alrededor de ambos tobillos y fija el otro extremo a un anclaje bajo delante de ti.", "Túmbate boca arriba en el suelo con las manos detrás de la cabeza y levanta los pies del suelo.", "Lleva una rodilla hacia el pecho mientras extiendes la otra pierna contra la resistencia de la banda y gira el tronco para acercar el codo contrario a la rodilla que sube.", "Cambia de lado con un movimiento de pedaleo controlado, manteniendo el abdomen activado.", "Alterna los lados.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 106 AND name_en = 'band bicycle crunch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'fe7e3b7b01d5af4375aa3719db8fb95a';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 106 AND name_en = 'band bicycle crunch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '41f2bbebc8f788071bf9b280b2fb6b81') THEN
    RAISE EXCEPTION 'V52: ejercicio 106 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 108 band jack knife sit-up: requires_overhead: R2 L02 (ALTA): En ningún fotograma los brazos pasan por encima de la cabeza: en f1 están a los lados y en la subida van al frente. Es un encogimiento con rodillas flexionadas  / V3 V01 ACEPTAR: f1 tumbado con brazos junto al cuerpo; f7 encogimiento en V con rodillas flexionadas y brazos al frente; en ningún fotograma los brazos pasan por encima de la c / coordinador: definicion literal demostrada en todos los fotogramas (R2 + V3)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = FALSE
   WHERE exercise_id = 108 AND name_en = 'band jack knife sit-up' AND requires_overhead = TRUE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 108 AND name_en = 'band jack knife sit-up' AND requires_overhead = FALSE) THEN
    RAISE EXCEPTION 'V52: ejercicio 108 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 111 band push sit-up: name_es: R2 L05 (ALTA): Es un abdominal completo (sit-up), no un crunch; no se ve el punto de anclaje de la banda / V3 V01 ACEPTAR: f1 tumbado con piernas extendidas y brazos hacia el techo con la banda; f7 tronco vertical (abdominal completo), no un crunch; anclaje fuera de cuadro | instructions_es: R2 L05 (ALTA): El texto dice rodillas flexionadas con los pies apoyados y subir hasta 45 grados; el GIF muestra las piernas extendidas en el suelo y el tronco sube hasta la ve / V3 V01 ACEPTAR: f1 tumbado con piernas extendidas y brazos estirados hacia el techo; f7 sentado con tronco vertical y brazos al frente; el texto actual dice rodillas flexionada
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Abdominal completo con empuje de banda', instructions_es = 'Ancla la banda de forma segura detrás de ti. Túmbate boca arriba en el suelo con las piernas extendidas y sujeta la banda con ambas manos, con los brazos estirados hacia el techo. Activa el abdomen y sube el tronco hasta quedar sentado, llevando los brazos estirados hacia delante contra la resistencia de la banda. Baja despacio hasta apoyar de nuevo la espalda. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ancla la banda de forma segura detrás de ti.", "Túmbate boca arriba en el suelo con las piernas extendidas y sujeta la banda con ambas manos, con los brazos estirados hacia el techo.", "Activa el abdomen y sube el tronco hasta quedar sentado, llevando los brazos estirados hacia delante contra la resistencia de la banda.", "Baja despacio hasta apoyar de nuevo la espalda.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 111 AND name_en = 'band push sit-up' AND name_es = 'Crunch con empuje y banda' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a14ee49299134aa403134127e8a8fd61';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 111 AND name_en = 'band push sit-up' AND name_es = 'Abdominal completo con empuje de banda' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'aa7f1df5d45249b8d0807b5928b4dc3c') THEN
    RAISE EXCEPTION 'V52: ejercicio 111 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 112 band seated twist: instructions_es: R2 L06 (MEDIA): El texto dice que los brazos se extienden al frente desde el inicio y que se gira sin doblarlos; en f1 las manos están junto al pecho con los codos flexionados  / V3 V01 MEJORAR: f1 tronco girado hacia la cámara con manos junto al pecho y codos flexionados; f7 tronco de frente hacia los pies con brazos extendidos; en f7 la banda continúa
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en el suelo con las piernas extendidas y la espalda recta, con una banda anclada detrás de ti a la altura del pecho. Gira el tronco hacia un lado y sujeta la banda con ambas manos junto al pecho, con los codos flexionados. Gira el tronco hasta mirar al frente mientras extiendes los brazos hacia delante contra la resistencia de la banda. Vuelve con control a la posición inicial. Completa las repeticiones y cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en el suelo con las piernas extendidas y la espalda recta, con una banda anclada detrás de ti a la altura del pecho.", "Gira el tronco hacia un lado y sujeta la banda con ambas manos junto al pecho, con los codos flexionados.", "Gira el tronco hasta mirar al frente mientras extiendes los brazos hacia delante contra la resistencia de la banda.", "Vuelve con control a la posición inicial.", "Completa las repeticiones y cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 112 AND name_en = 'band seated twist' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '6cfedebf44c9710bbbe143e7d423c705';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 112 AND name_en = 'band seated twist' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '471c1799906b614e1e5e0d110e9e0daa') THEN
    RAISE EXCEPTION 'V52: ejercicio 112 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 113 band assisted pull-up: instructions_es: R2 L07 (ALTA): El texto dice pisa la banda; el GIF muestra la banda pasando por debajo de las rodillas, no de los pies. Además afirma palmas hacia afuera, agarre que no se dis / V3 V01 ACEPTAR: f1 colgado de la barra con brazos extendidos y la banda pasando bajo las rodillas; f7 barbilla a la altura de la barra; la banda nunca pasa por los pies; agarre | difficulty_level: R2 L07 (ALTA): suspensión; fuerza relativa moderada / V3 V01 ACEPTAR: f1 colgado de la barra con pies sin apoyo y rodillas en la banda; f7 subida completa; es suspensión del peso corporal, incompatible con el nivel 1
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Sujeta la banda a la barra de dominadas y comprueba que queda bien anclada. Agarra la barra con las manos algo más separadas que los hombros y coloca las rodillas dentro del lazo de la banda. Cuélgate con los brazos extendidos, el core activado y los hombros abajo. Sube el cuerpo llevando los codos hacia abajo hasta que la barbilla supere la barra. Baja con control hasta extender los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Sujeta la banda a la barra de dominadas y comprueba que queda bien anclada.", "Agarra la barra con las manos algo más separadas que los hombros y coloca las rodillas dentro del lazo de la banda.", "Cuélgate con los brazos extendidos, el core activado y los hombros abajo.", "Sube el cuerpo llevando los codos hacia abajo hasta que la barbilla supere la barra.", "Baja con control hasta extender los brazos.", "Repite el número de repeticiones indicado."]'::jsonb, difficulty_level = 2
   WHERE exercise_id = 113 AND name_en = 'band assisted pull-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '1a20d793f3d0cc7e103b0fb46d481072' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 113 AND name_en = 'band assisted pull-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ebf67410eaa52d38301962ca0c9d2df6' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 113 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 118 band one arm standing low row: instructions_es: R2 L12 (ALTA): El texto ancla la banda a la altura de la cintura y coloca los pies a la anchura de los hombros; el GIF muestra la banda anclada abajo, cerca del suelo, y los p / V3 W01 MEJORAR: f1 postura escalonada, brazo extendido hacia abajo y delante con la banda bajando hacia un anclaje bajo; f7 mano en la cintura con el codo atrás; contenido de l
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ancla la banda a un punto estable cerca del suelo, delante de ti. Ponte de pie con un pie adelantado, flexiona ligeramente las rodillas e inclina un poco el tronco hacia delante con la espalda recta. Sujeta la banda con una mano, con el brazo extendido hacia el anclaje. Tira de la banda hacia la cintura llevando el codo atrás y juntando los omóplatos. Vuelve a extender el brazo con control; al terminar la serie, cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ancla la banda a un punto estable cerca del suelo, delante de ti.", "Ponte de pie con un pie adelantado, flexiona ligeramente las rodillas e inclina un poco el tronco hacia delante con la espalda recta.", "Sujeta la banda con una mano, con el brazo extendido hacia el anclaje.", "Tira de la banda hacia la cintura llevando el codo atrás y juntando los omóplatos.", "Vuelve a extender el brazo con control; al terminar la serie, cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 118 AND name_en = 'band one arm standing low row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '06828b3a3a0c229f8be3dac19c7e00ef';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 118 AND name_en = 'band one arm standing low row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e8e868fc9beb8d9f6fc3dd600c810062') THEN
    RAISE EXCEPTION 'V52: ejercicio 118 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 121 band bent-over hip extension: instructions_es: R2 L15 (MEDIA): El texto pide colocarse de espaldas al anclaje y alternar las piernas; en el GIF la banda va hacia delante (anclaje bajo por delante) y se trabaja siempre la mi / V3 W01 ACEPTAR: f1 de cara al anclaje bajo delantero, tronco inclinado, brazos al frente y tobillera con el pie por delante; f7 la misma pierna estirada atrás casi horizontal c
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Sujeta la banda a un punto de anclaje resistente cerca del suelo y engánchala a un tobillo; comprueba que el anclaje es firme. Colócate de pie de cara al anclaje, inclina un poco el tronco hacia delante y extiende los brazos al frente. Con el peso en la otra pierna, lleva la pierna enganchada estirada hacia atrás, apretando el glúteo. Vuelve despacio hasta que el pie quede por delante del cuerpo. Completa las repeticiones y cambia de pierna. Repite el número de repeticiones indicado.', instruction_steps_es = '["Sujeta la banda a un punto de anclaje resistente cerca del suelo y engánchala a un tobillo; comprueba que el anclaje es firme.", "Colócate de pie de cara al anclaje, inclina un poco el tronco hacia delante y extiende los brazos al frente.", "Con el peso en la otra pierna, lleva la pierna enganchada estirada hacia atrás, apretando el glúteo.", "Vuelve despacio hasta que el pie quede por delante del cuerpo.", "Completa las repeticiones y cambia de pierna.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 121 AND name_en = 'band bent-over hip extension' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '458ed8a151c8de64bfee826ec498cd7d';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 121 AND name_en = 'band bent-over hip extension' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'da99cf6cdbcf33e67f0384ac21b628c9') THEN
    RAISE EXCEPTION 'V52: ejercicio 121 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 129 band close-grip push-up: instructions_es: R2 L03 (MEDIA): El texto coloca la banda alrededor de la parte superior de los brazos por encima de los codos; el GIF muestra la banda pasando por la espalda con los extremos ( / V3 V01 ACEPTAR: f1 brazos extendidos con las manos sobre los extremos de la banda y las asas en el suelo junto a las manos; la banda sube por los brazos hacia la espalda; f7 pe
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Pasa la banda por la parte alta de la espalda y sujeta un extremo en cada mano, apoyando las manos en el suelo bajo los hombros. Estira las piernas y apóyate en las puntas de los pies con el cuerpo en línea recta de la cabeza a los talones. Flexiona los codos y baja el pecho hacia el suelo contra la resistencia de la banda. Empuja con las palmas hasta extender los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Pasa la banda por la parte alta de la espalda y sujeta un extremo en cada mano, apoyando las manos en el suelo bajo los hombros.", "Estira las piernas y apóyate en las puntas de los pies con el cuerpo en línea recta de la cabeza a los talones.", "Flexiona los codos y baja el pecho hacia el suelo contra la resistencia de la banda.", "Empuja con las palmas hasta extender los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 129 AND name_en = 'band close-grip push-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '74171c432c8942ed1e0f52ad230230db';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 129 AND name_en = 'band close-grip push-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '023657081deee1ca4251c01e86b432f8') THEN
    RAISE EXCEPTION 'V52: ejercicio 129 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 133 band wrist curl: instructions_es: R2 L07 (MEDIA): El texto dice sujeta la banda con ambas manos y apoya los antebrazos; el GIF muestra una sola mano con la banda, la otra mano sujetando el antebrazo, y la banda / V3 V01 MEJORAR: f1 mano con el asa baja entre las rodillas, f7 la mano sube por flexión de muñeca; una sola mano en la banda, la otra sujeta el antebrazo; la banda pasa bajo un
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en un cajón o banco con los pies apoyados en el suelo. Pisa la banda con un pie y sujeta el asa con una mano, entre las rodillas. Inclina el tronco hacia delante y sujeta con la otra mano el antebrazo que trabaja para mantenerlo fijo. Flexiona la muñeca hacia arriba contra la banda y bájala con control. Al terminar, cambia de mano. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en un cajón o banco con los pies apoyados en el suelo.", "Pisa la banda con un pie y sujeta el asa con una mano, entre las rodillas.", "Inclina el tronco hacia delante y sujeta con la otra mano el antebrazo que trabaja para mantenerlo fijo.", "Flexiona la muñeca hacia arriba contra la banda y bájala con control.", "Al terminar, cambia de mano.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 133 AND name_en = 'band wrist curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '576bd1bad6edfab46ee922d054ee628d';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 133 AND name_en = 'band wrist curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd0c53a88b3703f3edd0e167c3a1a42b9') THEN
    RAISE EXCEPTION 'V52: ejercicio 133 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 136 band twisting overhead press: instructions_es: R2 L10 (MEDIA): El texto dice banda bajo los pies, dos asas a los hombros y alternar lados; el GIF muestra una sola mano con la banda anclada abajo a un lado y siempre el mismo / V3 V01 MEJORAR: f1 piernas separadas con rodillas flexionadas y mano con la banda junto al hombro, anclaje bajo a un lado; f7 piernas extendidas, brazo estirado en diagonal sob
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ancla la banda abajo a un lado y colócate de pie con los pies bien separados y las rodillas flexionadas. Sujeta el extremo de la banda con la mano más alejada del anclaje, a la altura del hombro. Extiende las piernas y empuja la banda en diagonal por encima de la cabeza mientras giras el tronco, manteniendo el core activo. Vuelve con control a la posición inicial. Al terminar, cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ancla la banda abajo a un lado y colócate de pie con los pies bien separados y las rodillas flexionadas.", "Sujeta el extremo de la banda con la mano más alejada del anclaje, a la altura del hombro.", "Extiende las piernas y empuja la banda en diagonal por encima de la cabeza mientras giras el tronco, manteniendo el core activo.", "Vuelve con control a la posición inicial.", "Al terminar, cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 136 AND name_en = 'band twisting overhead press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '45275b09c0f1f4af7265187cef0229f5';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 136 AND name_en = 'band twisting overhead press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3d3c972bfa1ccc6a2d57cb9e67f1eaf8') THEN
    RAISE EXCEPTION 'V52: ejercicio 136 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 138 band standing crunch: instructions_es: R2 L12 (ALTA): El texto ancla la banda a la altura de la cintura y lleva las manos al pecho; el GIF muestra la banda anclada en alto por detrás y las manos junto a la cabeza / V3 W01 MEJORAR: f1 de pie con la banda bajando desde un anclaje alto por detrás y las manos a la altura de la barbilla delante del cuello; f7 tronco flexionado con las manos en
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ancla la banda a un punto estable alto, por detrás de ti, y ponte de pie de espaldas al anclaje con los pies separados a la anchura de las caderas. Sujeta la banda con ambas manos a la altura de la barbilla, con los codos flexionados y cerca del cuerpo. Activa el abdomen y flexiona el tronco hacia delante y hacia abajo, manteniendo las manos junto a la barbilla. Vuelve despacio a la posición erguida. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ancla la banda a un punto estable alto, por detrás de ti, y ponte de pie de espaldas al anclaje con los pies separados a la anchura de las caderas.", "Sujeta la banda con ambas manos a la altura de la barbilla, con los codos flexionados y cerca del cuerpo.", "Activa el abdomen y flexiona el tronco hacia delante y hacia abajo, manteniendo las manos junto a la barbilla.", "Vuelve despacio a la posición erguida.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 138 AND name_en = 'band standing crunch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3c3996dcafd807247246bf7a7b0c2312';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 138 AND name_en = 'band standing crunch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3e37af95d27848ef30a6c28e9ce161e1') THEN
    RAISE EXCEPTION 'V52: ejercicio 138 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 140 band v-up: difficulty_level: R2 L14 (ALTA): coordinación tronco-piernas; equilibrio sobre glúteos / V3 W01 ACEPTAR: f1 tumbado con brazos sobre la cabeza y tobilleras con banda; f7 V completa con tronco y piernas rectas elevados a la vez y manos hacia los pies, en equilibrio 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 140 AND name_en = 'band v-up' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 140 AND name_en = 'band v-up' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 140 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 145 band squat row: instructions_es: R2 L19 (ALTA): El texto dice tirar de las agarraderas desde la posición de sentadilla y hacer la pausa arriba; el GIF muestra los brazos estirados en la sentadilla y el remo c / V3 Y01 ACEPTAR: f1 y f12 sentadilla con brazos estirados al frente; f4-f7 sube y llega de pie con las asas en la cintura (remo completado arriba); el tirón ocurre durante la su
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Sujeta la banda a un punto de anclaje resistente a la altura de la cintura y ponte de pie frente a él con los pies separados a la anchura de los hombros. Sujeta las asas con los brazos estirados al frente y baja a una sentadilla con la espalda recta y el pecho elevado. Sube a la posición de pie y, a la vez, tira de las asas hacia la cintura juntando los omóplatos. Vuelve a bajar a la sentadilla estirando los brazos de forma controlada. Repite el número de repeticiones indicado.', instruction_steps_es = '["Sujeta la banda a un punto de anclaje resistente a la altura de la cintura y ponte de pie frente a él con los pies separados a la anchura de los hombros.", "Sujeta las asas con los brazos estirados al frente y baja a una sentadilla con la espalda recta y el pecho elevado.", "Sube a la posición de pie y, a la vez, tira de las asas hacia la cintura juntando los omóplatos.", "Vuelve a bajar a la sentadilla estirando los brazos de forma controlada.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 145 AND name_en = 'band squat row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3eb247fd4ec8157acabc45b156bc37a5';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 145 AND name_en = 'band squat row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5f546be2d8aae561c83578be9ce3978d') THEN
    RAISE EXCEPTION 'V52: ejercicio 145 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 147 band stiff leg deadlift: instructions_es: R2 L01 (ALTA): El texto dice colocar la banda alrededor de los tobillos y sujetarla frente a los muslos; el GIF muestra la banda pisada bajo los pies y las asas en las manos a / V3 V01 ACEPTAR: f1 bisagra de cadera con tronco casi horizontal y rodillas casi extendidas, asas en las manos; f7 de pie con las asas a los lados de los muslos; la banda pasa b
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies separados a la anchura de las caderas pisando el centro de la banda y sujeta un asa en cada mano con los brazos extendidos. Mantén la espalda recta, el core activado y las rodillas casi extendidas. Lleva la cadera hacia atrás e inclina el tronco hasta acercarlo a la horizontal, dejando que las manos bajen hacia los pies. Aprieta glúteos e isquiotibiales para volver a la posición de pie. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies separados a la anchura de las caderas pisando el centro de la banda y sujeta un asa en cada mano con los brazos extendidos.", "Mantén la espalda recta, el core activado y las rodillas casi extendidas.", "Lleva la cadera hacia atrás e inclina el tronco hasta acercarlo a la horizontal, dejando que las manos bajen hacia los pies.", "Aprieta glúteos e isquiotibiales para volver a la posición de pie.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 147 AND name_en = 'band stiff leg deadlift' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd7df667afe1382ea8d2fa7df7f04f750';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 147 AND name_en = 'band stiff leg deadlift' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b457a14f7e5ee81ec470d69c43b4a5a2') THEN
    RAISE EXCEPTION 'V52: ejercicio 147 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 160 barbell bench front squat: instructions_es: R2 L14 (ALTA): El texto no menciona el banco ni sentarse en él, y describe agarre pronado con codos altos; el GIF muestra agarre con brazos cruzados y bajada hasta sentarse en / V3 W01 ACEPTAR: f1 de pie delante de un banco con la barra en la parte delantera de los hombros y los brazos cruzados; f7 sentado brevemente en el banco con el tronco erguido; 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca un banco plano detrás de ti y ponte de pie delante de él con los pies separados a la anchura de los hombros. Apoya la barra en la parte delantera de los hombros y sujétala con los brazos cruzados por delante, con los codos altos. Baja flexionando rodillas y cadera, con el pecho erguido, hasta sentarte brevemente en el banco. Empuja con las piernas para volver a ponerte de pie. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca un banco plano detrás de ti y ponte de pie delante de él con los pies separados a la anchura de los hombros.", "Apoya la barra en la parte delantera de los hombros y sujétala con los brazos cruzados por delante, con los codos altos.", "Baja flexionando rodillas y cadera, con el pecho erguido, hasta sentarte brevemente en el banco.", "Empuja con las piernas para volver a ponerte de pie.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 160 AND name_en = 'barbell bench front squat' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '4772960d2b04f9b114c96f52901b8ada';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 160 AND name_en = 'barbell bench front squat' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ba7791e3da407ce12be4509b76fb51d5') THEN
    RAISE EXCEPTION 'V52: ejercicio 160 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 161 barbell bench squat: instructions_es: R2 L15 (ALTA): El texto describe sacar la barra de un soporte a la altura del pecho (no se ve ningún soporte) y no menciona el banco; el GIF muestra bajar hasta sentarse en un / V3 W01 ACEPTAR: f1 de pie con la barra en la parte alta de la espalda y banco detrás; f7 sentado brevemente en el banco; no se ve ningún soporte; conserva rodillas alineadas co
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca un banco plano detrás de ti y apoya la barra sobre la parte alta de la espalda, sujetándola con ambas manos. Ponte de pie con los pies separados a la anchura de los hombros, a una distancia del banco que te permita sentarte. Baja en sentadilla llevando la cadera atrás, con el pecho elevado y las rodillas alineadas con los pies, hasta sentarte brevemente en el banco. Empuja con los pies para volver a ponerte de pie. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca un banco plano detrás de ti y apoya la barra sobre la parte alta de la espalda, sujetándola con ambas manos.", "Ponte de pie con los pies separados a la anchura de los hombros, a una distancia del banco que te permita sentarte.", "Baja en sentadilla llevando la cadera atrás, con el pecho elevado y las rodillas alineadas con los pies, hasta sentarte brevemente en el banco.", "Empuja con los pies para volver a ponerte de pie.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 161 AND name_en = 'barbell bench squat' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '87b94325fb01a2a7e376a7d0b0eef2e4';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 161 AND name_en = 'barbell bench squat' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '8578dc406ca8fcc97de27b2c073bfbbd') THEN
    RAISE EXCEPTION 'V52: ejercicio 161 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 165 barbell front chest squat: zona: R2 L19 (ALTA): El GIF resalta los cuádriceps, no los glúteos. Nivel 3 discutible: es una sentadilla con peso libre de técnica moderada, más cerca del perfil 2. / V3 Y01 ACEPTAR: f1-f12 vista frontal con resaltado rojo en los cuádriceps en todo el recorrido; glúteos no resaltados; sentadilla frontal con barra sobre los hombros (en el cat
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs'), target_muscle = 'quads'
   WHERE exercise_id = 165 AND name_en = 'barbell front chest squat' AND body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs') AND target_muscle = 'glutes';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 165 AND name_en = 'barbell front chest squat' AND body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs') AND target_muscle = 'quads') THEN
    RAISE EXCEPTION 'V52: ejercicio 165 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 166 barbell front squat: zona: R2 L20 (MEDIA): El GIF resalta en rojo los cuádriceps durante todo el movimiento, no los glúteos. La dificultad 3 parece alta: es peso libre con técnica básica-moderada (perfil / V3 W01 ACEPTAR: f1-f12 vista frontal de sentadilla frontal con los cuádriceps resaltados en rojo durante todo el movimiento; los glúteos no se resaltan
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs'), target_muscle = 'quads'
   WHERE exercise_id = 166 AND name_en = 'barbell front squat' AND body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs') AND target_muscle = 'glutes';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 166 AND name_en = 'barbell front squat' AND body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs') AND target_muscle = 'quads') THEN
    RAISE EXCEPTION 'V52: ejercicio 166 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 167 barbell alternate biceps curl: instructions_es: R2 L01 (MEDIA): El texto dice sujetar una barra en cada mano y alternar los brazos; el GIF muestra una única barra sujeta con una mano y la otra mano en la cadera, sin alternan / V3 V01 ACEPTAR: f1 una barra sujeta por el centro con una mano y brazo extendido, la otra mano en la cadera; f7 barra horizontal a la altura del hombro; una sola barra y sin al
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies separados a la anchura de las caderas y sujeta una barra por su centro con una mano, palma hacia delante y el brazo extendido. Apoya la otra mano en la cadera. Mantén el codo pegado al costado y flexiónalo para subir la barra hasta la altura del hombro, manteniéndola horizontal. Baja la barra con control hasta extender el brazo. Completa las repeticiones y cambia de brazo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies separados a la anchura de las caderas y sujeta una barra por su centro con una mano, palma hacia delante y el brazo extendido.", "Apoya la otra mano en la cadera.", "Mantén el codo pegado al costado y flexiónalo para subir la barra hasta la altura del hombro, manteniéndola horizontal.", "Baja la barra con control hasta extender el brazo.", "Completa las repeticiones y cambia de brazo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 167 AND name_en = 'barbell alternate biceps curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'f71d5a1a67e54b6eced43801bae0af5b';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 167 AND name_en = 'barbell alternate biceps curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '11dd702f75a4f985f4c63fcb4ff08ddc') THEN
    RAISE EXCEPTION 'V52: ejercicio 167 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 175 barbell one arm snatch: instructions_es: R2 L09 (ALTA): Dice agarre pronado con las manos un poco más separadas que los hombros, pero el GIF muestra una sola mano agarrando la barra por el centro; además omite la rec / V3 V01 ACEPTAR: f1 barra en el suelo sujeta por el centro con una mano; f7 tirón alto con codo elevado; f13 recepción en sentadilla profunda con brazo extendido arriba; f19 de 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate de pie con los pies algo más separados que los hombros y una barra en el suelo delante de ti. Flexiona la cadera y las rodillas con la espalda recta y sujeta la barra por el centro con una mano, con agarre prono. Extiende la cadera y las rodillas de forma explosiva y sube la barra pegada al cuerpo, con el codo alto, hasta la altura del pecho. Métete bajo la barra en sentadilla profunda, recíbela con el brazo extendido por encima de la cabeza y ponte de pie manteniéndola arriba. Baja la barra con control hasta el suelo y cambia de brazo al terminar la serie. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate de pie con los pies algo más separados que los hombros y una barra en el suelo delante de ti.", "Flexiona la cadera y las rodillas con la espalda recta y sujeta la barra por el centro con una mano, con agarre prono.", "Extiende la cadera y las rodillas de forma explosiva y sube la barra pegada al cuerpo, con el codo alto, hasta la altura del pecho.", "Métete bajo la barra en sentadilla profunda, recíbela con el brazo extendido por encima de la cabeza y ponte de pie manteniéndola arriba.", "Baja la barra con control hasta el suelo y cambia de brazo al terminar la serie.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 175 AND name_en = 'barbell one arm snatch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '644e5e9599d967d92f1c78780bcdaf16';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 175 AND name_en = 'barbell one arm snatch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3a8d2c400f9fe91a1f1280708fd99113') THEN
    RAISE EXCEPTION 'V52: ejercicio 175 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 180 barbell one arm bent over row: instructions_es: R2 L14 (ALTA): El texto no dice que un extremo de la barra se apoya en el suelo (sugiere sostener la barra entera con una mano) y lleva el tirón al pecho; en el GIF la barra p / V3 W01 MEJORAR: f1 extremo sin disco apoyado en el suelo, mano detrás del disco con el brazo estirado y tronco inclinado; f7 mano junto a la cadera con el codo atrás; contenido
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Apoya un extremo de la barra en el suelo y carga solo el otro extremo. Ponte de pie junto a la barra, flexiona ligeramente las rodillas e inclina el tronco desde la cadera con la espalda recta. Sujeta la barra con una mano justo detrás del disco, con el brazo estirado. Tira de la barra hacia la cadera llevando el codo hacia atrás, cerca del cuerpo. Bájala con control hasta estirar el brazo; al terminar la serie, cambia de brazo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Apoya un extremo de la barra en el suelo y carga solo el otro extremo.", "Ponte de pie junto a la barra, flexiona ligeramente las rodillas e inclina el tronco desde la cadera con la espalda recta.", "Sujeta la barra con una mano justo detrás del disco, con el brazo estirado.", "Tira de la barra hacia la cadera llevando el codo hacia atrás, cerca del cuerpo.", "Bájala con control hasta estirar el brazo; al terminar la serie, cambia de brazo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 180 AND name_en = 'barbell one arm bent over row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd5a8b5a6e5e457a20f6720421dda37fb';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 180 AND name_en = 'barbell one arm bent over row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b7066a0c60f0d5937d0f210918df9991') THEN
    RAISE EXCEPTION 'V52: ejercicio 180 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 185 barbell full zercher squat: zona: R2 L19 (ALTA): El GIF resalta los cuádriceps. Nivel 3 discutible (sentadilla con peso libre, perfil 2). La profundidad es similar a la paralela, no se ve una sentadilla comple / V3 Y01 ACEPTAR: f1-f12 resaltado rojo en los cuádriceps (f1 de pie, f7 abajo); barra en el pliegue de los codos; glúteos no resaltados (formato habitual upper legs/quads)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs'), target_muscle = 'quads'
   WHERE exercise_id = 185 AND name_en = 'barbell full zercher squat' AND body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs') AND target_muscle = 'glutes';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 185 AND name_en = 'barbell full zercher squat' AND body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs') AND target_muscle = 'quads') THEN
    RAISE EXCEPTION 'V52: ejercicio 185 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 192 barbell jump squat: name_es: R2 L06 (ALTA): El nombre no menciona la barra y se confunde con la sentadilla con salto sin carga (919). Fase de vuelo visible en f13-f14. / V3 V01 ACEPTAR: f1 barra con discos sobre la espalda; f7 sentadilla; f13 vuelo con barra en la espalda; el nombre actual no menciona la barra, que cambia la carga del ejercicio
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Sentadilla con salto con barra'
   WHERE exercise_id = 192 AND name_en = 'barbell jump squat' AND name_es = 'Sentadilla con salto';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 192 AND name_en = 'barbell jump squat' AND name_es = 'Sentadilla con salto con barra') THEN
    RAISE EXCEPTION 'V52: ejercicio 192 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 197 barbell narrow stance squat: instructions_es: R2 L11 (MEDIA): Dice pies separados a la altura de los hombros; el GIF (y el nombre) muestra una postura estrecha, pies a la anchura de las caderas o menos / V3 W01 MEJORAR: f1 pies a la anchura de las caderas, más estrechos que los hombros, con barra en la espalda; f7 sentadilla con muslos paralelos; los pies no están juntos como d
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies separados a la anchura de las caderas, más cerca que la anchura de los hombros, y apoya la barra sobre la parte superior de la espalda sujetándola con ambas manos. Activa el core y mantén el pecho elevado. Baja flexionando las rodillas y llevando las caderas hacia atrás hasta que los muslos queden paralelos al suelo o algo por debajo, con las rodillas alineadas con los pies. Empuja con los pies para volver a la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies separados a la anchura de las caderas, más cerca que la anchura de los hombros, y apoya la barra sobre la parte superior de la espalda sujetándola con ambas manos.", "Activa el core y mantén el pecho elevado.", "Baja flexionando las rodillas y llevando las caderas hacia atrás hasta que los muslos queden paralelos al suelo o algo por debajo, con las rodillas alineadas con los pies.", "Empuja con los pies para volver a la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 197 AND name_en = 'barbell narrow stance squat' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'f7c9809e0152823519b3bafe16bd6608';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 197 AND name_en = 'barbell narrow stance squat' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '600f6cfe9720b2f529bf973551d59645') THEN
    RAISE EXCEPTION 'V52: ejercicio 197 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 198 barbell one arm side deadlift: instructions_es: R2 L12 (MEDIA): El texto describe una bisagra de cadera (flexiona las caderas, pecho elevado); el GIF muestra una sentadilla profunda con gran flexión de rodillas y el tronco c / V3 W01 MEJORAR: f1 sentadilla profunda con el tronco casi vertical y la barra al costado sujeta por el centro con una mano; f7 de pie con el brazo recto y la otra mano libre; l | zona: R2 L12 (MEDIA): Se resaltan los cuádriceps y el patrón visible es de sentadilla, no de bisagra / V3 W01 ACEPTAR: f1-f12 cuádriceps resaltados; f1 sentadilla profunda con gran flexión de rodillas y tronco casi vertical; f7 de pie | movement_pattern: R2 L12 (MEDIA): Se resaltan los cuádriceps y el patrón visible es de sentadilla, no de bisagra / V3 W01 ACEPTAR: f1 sentadilla profunda con el tronco casi vertical y la barra al costado; f4-f7 extensión de rodillas y caderas hasta quedar de pie: dominante de rodilla, no bi
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca una barra en el suelo a tu lado y ponte de pie junto a su centro, con los pies ligeramente separados. Baja flexionando rodillas y caderas, con la espalda recta, el tronco erguido y el core activado, y sujeta la barra por el centro con una mano. Ponte de pie extendiendo rodillas y caderas, con el brazo recto y sin inclinar el tronco hacia el lado de la barra. Baja con control hasta acercar la barra al suelo; al terminar la serie, cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca una barra en el suelo a tu lado y ponte de pie junto a su centro, con los pies ligeramente separados.", "Baja flexionando rodillas y caderas, con la espalda recta, el tronco erguido y el core activado, y sujeta la barra por el centro con una mano.", "Ponte de pie extendiendo rodillas y caderas, con el brazo recto y sin inclinar el tronco hacia el lado de la barra.", "Baja con control hasta acercar la barra al suelo; al terminar la serie, cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb, body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs'), target_muscle = 'quads', movement_pattern = 'KNEE_DOMINANT'
   WHERE exercise_id = 198 AND name_en = 'barbell one arm side deadlift' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'f8e5f10e1099659db158520940041a3f' AND body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs') AND target_muscle = 'glutes' AND movement_pattern = 'HIP_HINGE';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 198 AND name_en = 'barbell one arm side deadlift' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e0dffb6b63d1803446fd97489c9d7942' AND body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs') AND target_muscle = 'quads' AND movement_pattern = 'KNEE_DOMINANT') THEN
    RAISE EXCEPTION 'V52: ejercicio 198 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 206 barbell lying close-grip triceps extension: instructions_es: R2 L20 (ALTA): El texto dice que la barra empieza por encima del pecho y se baja hacia la frente; el GIF muestra los brazos inclinados hacia la cabeza, con la barra sobre la c / V3 W01 ACEPTAR: f1 tumbado en banco plano con los pies en el suelo, codos flexionados y la barra por detrás y por debajo de la cabeza; f7 brazos extendidos inclinados hacia la 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en un banco plano con los pies apoyados en el suelo. Sujeta la barra con las manos juntas y extiende los brazos con la barra por encima de la cara, inclinados ligeramente hacia la cabeza. Sin mover la parte superior de los brazos, flexiona los codos y baja la barra con control por detrás de la cabeza. Extiende los codos para volver a la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en un banco plano con los pies apoyados en el suelo.", "Sujeta la barra con las manos juntas y extiende los brazos con la barra por encima de la cara, inclinados ligeramente hacia la cabeza.", "Sin mover la parte superior de los brazos, flexiona los codos y baja la barra con control por detrás de la cabeza.", "Extiende los codos para volver a la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 206 AND name_en = 'barbell lying close-grip triceps extension' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd601619a004e4e8f19b82b9eba2d931a';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 206 AND name_en = 'barbell lying close-grip triceps extension' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'aa08e7f05f9b293061add78890ad1352') THEN
    RAISE EXCEPTION 'V52: ejercicio 206 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 211 barbell one arm floor press: instructions_es: R2 L05 (ALTA): El texto dice rodillas flexionadas con los pies planos y la palma hacia arriba; el GIF muestra las piernas extendidas en el suelo y la mano rodeando la barra po / V3 V01 ACEPTAR: f1 tumbado en el suelo con piernas extendidas y codo apoyado, barra sujeta por el centro con una mano; f7 brazo vertical extendido; el texto actual pide rodilla
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en el suelo con las piernas extendidas. Sujeta una barra por el centro con una mano, con el codo apoyado en el suelo junto al cuerpo. Empuja la barra hacia arriba hasta extender el brazo en vertical, manteniéndola equilibrada. Bájala despacio hasta que el codo vuelva a tocar el suelo. Completa las repeticiones y cambia de brazo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en el suelo con las piernas extendidas.", "Sujeta una barra por el centro con una mano, con el codo apoyado en el suelo junto al cuerpo.", "Empuja la barra hacia arriba hasta extender el brazo en vertical, manteniéndola equilibrada.", "Bájala despacio hasta que el codo vuelva a tocar el suelo.", "Completa las repeticiones y cambia de brazo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 211 AND name_en = 'barbell one arm floor press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a8cb3124e3ace172eb73d70c92b61a6e';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 211 AND name_en = 'barbell one arm floor press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ca6df6f479f0f0b806755b4187702780') THEN
    RAISE EXCEPTION 'V52: ejercicio 211 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 212 barbell pin presses: name_es: R2 L06 (MEDIA): «Press de clavija» es una traducción literal de «pin press»; se propone «desde topes». Se ve el rack pero no los topes en uso. El objetivo tríceps es discutible / V3 V01 MEJORAR: f1 brazos extendidos sobre el pecho tumbado en banco dentro de un rack; f7 barra a la altura del pecho; no se ve que la barra apoye en los topes en ningún fotog
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Press de banca en rack con barra'
   WHERE exercise_id = 212 AND name_en = 'barbell pin presses' AND name_es = 'Press de clavija con barra';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 212 AND name_en = 'barbell pin presses' AND name_es = 'Press de banca en rack con barra') THEN
    RAISE EXCEPTION 'V52: ejercicio 212 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 215 barbell seated calf raise: instructions_es: R2 L09 (ALTA): Pide colocar la parte delantera de los pies sobre una plataforma elevada (bloque o escalón) y a la vez los pies planos en el suelo; el GIF muestra los pies dire / V3 V01 ACEPTAR: f1-f12 sentado en un banco con barra sobre los muslos; f1 talones abajo y f7 talones elevados con los pies directamente en el suelo, sin plataforma; el texto ac
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en un banco con los pies apoyados en el suelo, separados a la anchura de las caderas. Apoya una barra sobre los muslos, cerca de las rodillas, y sujétala con firmeza con ambas manos. Con la espalda recta, eleva los talones lo más alto posible apoyándote en la parte delantera de los pies. Haz una pausa breve arriba y baja los talones con control hasta el suelo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en un banco con los pies apoyados en el suelo, separados a la anchura de las caderas.", "Apoya una barra sobre los muslos, cerca de las rodillas, y sujétala con firmeza con ambas manos.", "Con la espalda recta, eleva los talones lo más alto posible apoyándote en la parte delantera de los pies.", "Haz una pausa breve arriba y baja los talones con control hasta el suelo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 215 AND name_en = 'barbell seated calf raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '1bf3e064037d5650e68be4dfc96ae0d9';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 215 AND name_en = 'barbell seated calf raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e7665b0d3464be0010746f401a6d2903') THEN
    RAISE EXCEPTION 'V52: ejercicio 215 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 224 barbell press sit-up: axial_load: R2 L18 (MEDIA): AX propuesta porque la barra se sostiene en press por encima de la cabeza con el tronco erguido (definición oficial), aunque la carga parece ligera. Hace falta  / V3 W01 ACEPTAR: f1 tumbado en banco declinado con la barra sobre el pecho; f7 sentado con el tronco erguido y la barra en press por encima de la cabeza con los brazos extendido
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET axial_load = TRUE
   WHERE exercise_id = 224 AND name_en = 'barbell press sit-up' AND axial_load = FALSE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 224 AND name_en = 'barbell press sit-up' AND axial_load = TRUE) THEN
    RAISE EXCEPTION 'V52: ejercicio 224 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 226 barbell rollerout from bench: requires_floor: R2 L20 (ALTA): Las rodillas apoyan en el banco, no en el suelo, y el cuerpo nunca baja al suelo: según la definición, FL no aplica (KN sí). Pies sobre el banco, sin contacto c / V3 W01 ACEPTAR: f1 rodillas y pies sobre el banco y manos en la barra apoyada en el suelo; f7 cuerpo extendido desde el banco hasta la barra, sin tocar el suelo con el cuerpo;  / coordinador: rodillas y pies sobre el banco, manos en la barra; nunca tumbado, sentado ni a cuatro apoyos en el suelo (R2 + V3 ALTA). KN se mantiene
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_floor = FALSE
   WHERE exercise_id = 226 AND name_en = 'barbell rollerout from bench' AND requires_floor = TRUE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 226 AND name_en = 'barbell rollerout from bench' AND requires_floor = FALSE) THEN
    RAISE EXCEPTION 'V52: ejercicio 226 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 228 barbell side bent v. 2: axial_load: R2 L02 (MEDIA): La barra descansa sobre la espalda y los hombros (criterio AX: barra en la espalda), aunque es ligera. El GIF solo muestra la inclinación hacia un lado; el text / V3 V01 ACEPTAR: f1-f12 barra apoyada sobre los hombros por detrás del cuello; f7 inclinación lateral; barra en la espalda según la definición literal de AX, aunque sin discos
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET axial_load = TRUE
   WHERE exercise_id = 228 AND name_en = 'barbell side bent v. 2' AND axial_load = FALSE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 228 AND name_en = 'barbell side bent v. 2' AND axial_load = TRUE) THEN
    RAISE EXCEPTION 'V52: ejercicio 228 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 229 barbell sitted alternate leg raise: axial_load: R2 L03 (MEDIA): La barra (discos visibles) se mantiene bloqueada por encima de la cabeza durante toda la serie: carga compresiva comparable a la de un press por encima de la ca / V3 V01 ACEPTAR: f1-f12 barra con discos sostenida con brazos extendidos por encima de la cabeza durante toda la serie mientras alterna elevaciones de piernas; carga por encima 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET axial_load = TRUE
   WHERE exercise_id = 229 AND name_en = 'barbell sitted alternate leg raise' AND axial_load = FALSE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 229 AND name_en = 'barbell sitted alternate leg raise' AND axial_load = TRUE) THEN
    RAISE EXCEPTION 'V52: ejercicio 229 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 231 barbell pullover to press: instructions_es: R2 L05 (MEDIA): El texto dice bajar la barra detrás de la cabeza manteniendo los brazos rectos; el GIF muestra los codos claramente flexionados en la fase baja / V3 V01 MEJORAR: f1 brazos verticales sobre el pecho; f7 barra a la altura del pecho con codos flexionados pegados al tronco y apuntando hacia los pies; f2-f6 y f8-f12 son fundi
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en un banco plano con los pies apoyados en el suelo. Sujeta la barra con las manos algo separadas y extiende los brazos en vertical sobre el pecho. Flexiona los codos, manteniéndolos cerca del cuerpo, y baja la barra con control hasta el pecho. Empuja la barra hacia arriba hasta extender de nuevo los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en un banco plano con los pies apoyados en el suelo.", "Sujeta la barra con las manos algo separadas y extiende los brazos en vertical sobre el pecho.", "Flexiona los codos, manteniéndolos cerca del cuerpo, y baja la barra con control hasta el pecho.", "Empuja la barra hacia arriba hasta extender de nuevo los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 231 AND name_en = 'barbell pullover to press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3f7b8f5a803b52d62191b02d06aa1ba5';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 231 AND name_en = 'barbell pullover to press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '6e82b162116cda6950d0ab22fd4051a1') THEN
    RAISE EXCEPTION 'V52: ejercicio 231 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 233 barbell reverse grip incline bench row: instructions_es: R2 L07 (MEDIA): El texto dice siéntate en el banco frente al respaldo y define el agarre invertido como palmas hacia abajo; el GIF muestra el cuerpo tumbado boca abajo sobre el / V3 V01 ACEPTAR: f1 tumbado boca abajo sobre el banco inclinado, pecho en el respaldo, piernas extendidas detrás con puntas de pies en el suelo y brazos colgando con la barra; f
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca un banco inclinado y túmbate boca abajo sobre él, con el pecho apoyado en el respaldo y las puntas de los pies en el suelo. Sujeta la barra con los brazos extendidos hacia el suelo. Mantén la espalda recta y el core activado. Tira de la barra hacia el banco flexionando los codos y juntando los omóplatos. Baja la barra con control hasta extender los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca un banco inclinado y túmbate boca abajo sobre él, con el pecho apoyado en el respaldo y las puntas de los pies en el suelo.", "Sujeta la barra con los brazos extendidos hacia el suelo.", "Mantén la espalda recta y el core activado.", "Tira de la barra hacia el banco flexionando los codos y juntando los omóplatos.", "Baja la barra con control hasta extender los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 233 AND name_en = 'barbell reverse grip incline bench row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3bceea5b53f4a5ae3f1e39ffbed07a0b';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 233 AND name_en = 'barbell reverse grip incline bench row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '519a83c20ac51f376931fd70541de294') THEN
    RAISE EXCEPTION 'V52: ejercicio 233 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 237 barbell rear lunge v. 2: instructions_es: R2 L11 (ALTA): No menciona el step, que en el GIF es imprescindible: se parte de pie sobre él y el pie trasero baja al suelo por detrás (zancada en déficit) / V3 W01 MEJORAR: f1 de pie sobre el step con la barra en la espalda; f7 pie delantero sobre el step y pie trasero en el suelo por detrás apoyado en la punta, rodilla trasera cer
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie sobre un step con los pies separados a la anchura de las caderas y apoya la barra sobre la parte superior de la espalda. Da un paso largo hacia atrás con el pie derecho, bajándolo al suelo por detrás del step y apoyándote en la punta. Flexiona ambas rodillas hasta que el muslo de la pierna adelantada quede paralelo al suelo, con el tronco erguido. Empuja con el talón de la pierna adelantada para volver a subir al step. Alterna los lados. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie sobre un step con los pies separados a la anchura de las caderas y apoya la barra sobre la parte superior de la espalda.", "Da un paso largo hacia atrás con el pie derecho, bajándolo al suelo por detrás del step y apoyándote en la punta.", "Flexiona ambas rodillas hasta que el muslo de la pierna adelantada quede paralelo al suelo, con el tronco erguido.", "Empuja con el talón de la pierna adelantada para volver a subir al step.", "Alterna los lados.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 237 AND name_en = 'barbell rear lunge v. 2' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ecfeecad0758a4e98dcfe5c7fe8a0b9f';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 237 AND name_en = 'barbell rear lunge v. 2' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b8625f0d62c9d43433745e14d0daf43c') THEN
    RAISE EXCEPTION 'V52: ejercicio 237 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 241 barbell side split squat v. 2: instructions_es: R2 L15 (ALTA): El texto pide dar un paso grande hacia el lado y alternar lados; en el GIF los pies no se mueven (postura amplia fija) y solo se flexiona hacia un lado / V3 W01 ACEPTAR: f1 postura amplia con las puntas hacia fuera; f7 flexión de una rodilla con la cadera hacia ese lado y la otra pierna estirada; los pies no se mueven en ningún 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Apoya la barra sobre la parte alta de la espalda y sujétala con ambas manos. Separa los pies bastante más que la anchura de los hombros, con las puntas ligeramente hacia fuera. Sin mover los pies, flexiona una rodilla y lleva la cadera hacia ese lado y atrás, con la otra pierna estirada y el pecho elevado. Empuja con el pie de la pierna flexionada para volver al centro. Completa las repeticiones y cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Apoya la barra sobre la parte alta de la espalda y sujétala con ambas manos.", "Separa los pies bastante más que la anchura de los hombros, con las puntas ligeramente hacia fuera.", "Sin mover los pies, flexiona una rodilla y lleva la cadera hacia ese lado y atrás, con la otra pierna estirada y el pecho elevado.", "Empuja con el pie de la pierna flexionada para volver al centro.", "Completa las repeticiones y cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 241 AND name_en = 'barbell side split squat v. 2' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'add73cf764f1928ec9c388eb50ff0328';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 241 AND name_en = 'barbell side split squat v. 2' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ce8c30d804f75065e81417cd1111dc62') THEN
    RAISE EXCEPTION 'V52: ejercicio 241 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 250 barbell seated close grip behind neck triceps extension: instructions_es: R2 L04 (ALTA): El texto parte con la barra detrás del cuello, luego dice bajarla hacia la parte de atrás de la cabeza (ya está ahí) y extender de vuelta a la posición inicial; / V3 V02 ACEPTAR: f1 barra detrás de la cabeza con codos flexionados hacia arriba; f7 brazos extendidos por encima de la cabeza; sentado en banco con pies en el suelo. El texto a
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en un banco con la espalda recta y los pies apoyados en el suelo. Sujeta la barra con agarre estrecho detrás de la cabeza, con los codos flexionados apuntando hacia arriba. Extiende los codos hasta tener los brazos estirados por encima de la cabeza, sin mover los brazos superiores. Baja la barra con control detrás de la cabeza flexionando los codos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en un banco con la espalda recta y los pies apoyados en el suelo.", "Sujeta la barra con agarre estrecho detrás de la cabeza, con los codos flexionados apuntando hacia arriba.", "Extiende los codos hasta tener los brazos estirados por encima de la cabeza, sin mover los brazos superiores.", "Baja la barra con control detrás de la cabeza flexionando los codos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 250 AND name_en = 'barbell seated close grip behind neck triceps extension' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '4aed6c024b1bfd2262fe1af7ffbdf332';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 250 AND name_en = 'barbell seated close grip behind neck triceps extension' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '2906199a9a28568b1387d7afb478b9b9') THEN
    RAISE EXCEPTION 'V52: ejercicio 250 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 253 bear crawl: requires_kneeling: R2 L07 (ALTA): Ninguna rodilla apoya ni carga peso en el GIF; KN sobra. FL se mantiene por los apoyos de manos en el suelo / V3 V02 ACEPTAR: f1, f7, f13 (ampliados): rodillas siempre por encima del nivel de los pies, despegadas del suelo; apoyos solo en manos y puntas de los pies en los 18 fotogramas / coordinador: definicion literal demostrada en todos los fotogramas (R2 + V3) | instructions_es: R2 L07 (ALTA): El texto empieza apoyado sobre manos y rodillas; el GIF muestra en todo momento las rodillas despegadas del suelo / V3 V02 ACEPTAR: f1-f18 rodillas despegadas del suelo en todo momento, avance a cuatro apoyos sobre manos y puntas de los pies; el texto actual empieza con las rodillas apoyadas
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_kneeling = FALSE, instructions_es = 'Colócate a cuatro apoyos con las manos bajo los hombros y las puntas de los pies en el suelo, con las rodillas despegadas. Mantén la espalda plana y el core activado. Avanza alternando una mano y un pie con la otra mano y el otro pie. Continúa avanzando a ritmo constante sin apoyar las rodillas. Mantén la duración indicada.', instruction_steps_es = '["Colócate a cuatro apoyos con las manos bajo los hombros y las puntas de los pies en el suelo, con las rodillas despegadas.", "Mantén la espalda plana y el core activado.", "Avanza alternando una mano y un pie con la otra mano y el otro pie.", "Continúa avanzando a ritmo constante sin apoyar las rodillas.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 253 AND name_en = 'bear crawl' AND requires_kneeling = TRUE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '0463f88cb4d0e98877bd285a5d187d66';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 253 AND name_en = 'bear crawl' AND requires_kneeling = FALSE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '4a22bc639b3bd5f159b554639b0e8571') THEN
    RAISE EXCEPTION 'V52: ejercicio 253 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 254 basic toe touch (male): instructions_es: R2 L08 (MEDIA): El texto parte de los brazos a los costados y pide espalda recta y rodillas ligeramente flexionadas; el GIF muestra los brazos estirados por encima de la cabeza / V3 V02 ACEPTAR: f7 de pie erguido, pies juntos, brazos estirados por encima de la cabeza; f1/f12 tronco flexionado con piernas estiradas y manos hacia los pies. El texto actual
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie, erguido, con los pies cerca uno del otro. Eleva los brazos estirados por encima de la cabeza. Flexiona el tronco hacia delante desde la cadera, con las piernas estiradas, y lleva las manos hacia las puntas de los pies sin forzar. Vuelve a subir despacio hasta la posición erguida con los brazos por encima de la cabeza. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie, erguido, con los pies cerca uno del otro.", "Eleva los brazos estirados por encima de la cabeza.", "Flexiona el tronco hacia delante desde la cadera, con las piernas estiradas, y lleva las manos hacia las puntas de los pies sin forzar.", "Vuelve a subir despacio hasta la posición erguida con los brazos por encima de la cabeza.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 254 AND name_en = 'basic toe touch (male)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '8070a9537f51452e69123baae0550315';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 254 AND name_en = 'basic toe touch (male)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '4004c634ab221283a44176f173facae4') THEN
    RAISE EXCEPTION 'V52: ejercicio 254 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 256 barbell standing back wrist curl: name_es: R2 L09 (ALTA): «De dorso» es ambiguo y no indica que la barra va detrás del cuerpo. Dificultad 3 no encaja con un curl de muñeca de pie: propuesta 1. La orientación exacta del / V3 V02 ACEPTAR: f1/f7 ampliados: el disco derecho queda oculto tras la cadera, la barra está detrás del cuerpo a la altura de los glúteos con brazos extendidos y sube por flexi | instructions_es: R2 L09 (ALTA): No dice que la barra se sujeta por detrás del cuerpo y describe apoyarla sobre el dorso de las manos con los dedos apuntando hacia el cuerpo, algo que no se ve; / V3 V02 ACEPTAR: f1-f12 barra sujeta por detrás del cuerpo a la altura de los glúteos con brazos extendidos; solo se mueven las muñecas. El texto actual (barra sobre el dorso de
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Curl de muñeca con barra por detrás de la espalda, de pie', instructions_es = 'Ponte de pie erguido con los pies separados a la anchura de los hombros. Sujeta una barra por detrás del cuerpo, a la altura de los glúteos, con los brazos extendidos. Sin mover los brazos, flexiona las muñecas para subir la barra hacia atrás y arriba. Haz una pausa breve y baja la barra con control. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie erguido con los pies separados a la anchura de los hombros.", "Sujeta una barra por detrás del cuerpo, a la altura de los glúteos, con los brazos extendidos.", "Sin mover los brazos, flexiona las muñecas para subir la barra hacia atrás y arriba.", "Haz una pausa breve y baja la barra con control.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 256 AND name_en = 'barbell standing back wrist curl' AND name_es = 'Curl de muñeca de dorso de pie con barra' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e483884d4d544ab69f550f91a2909fce';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 256 AND name_en = 'barbell standing back wrist curl' AND name_es = 'Curl de muñeca con barra por detrás de la espalda, de pie' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '7d5d39bf4ba6126bac0545b1feb47f13') THEN
    RAISE EXCEPTION 'V52: ejercicio 256 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 257 barbell wrist curl: instructions_es: R2 L10 (MEDIA): El texto dice antebrazos apoyados sobre los muslos; el GIF muestra los antebrazos apoyados sobre el tapizado del banco / V3 V02 ACEPTAR: f1/f7 ampliados (vista cenital): antebrazos apoyados sobre el tapizado del banco con las muñecas fuera del borde; antebrazos con la cara anterior hacia arriba (
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate junto al extremo de un banco e inclínate hacia delante. Apoya los antebrazos sobre el banco con las palmas hacia arriba y las muñecas fuera del borde, sujetando la barra. Deja bajar la barra extendiendo las muñecas con control. Flexiona las muñecas para subir la barra sin levantar los antebrazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate junto al extremo de un banco e inclínate hacia delante.", "Apoya los antebrazos sobre el banco con las palmas hacia arriba y las muñecas fuera del borde, sujetando la barra.", "Deja bajar la barra extendiendo las muñecas con control.", "Flexiona las muñecas para subir la barra sin levantar los antebrazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 257 AND name_en = 'barbell wrist curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '23491e63004102cdcc1b68ca21686c80';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 257 AND name_en = 'barbell wrist curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3a3dbe8783cd52b3989ac4dfdccbbac3') THEN
    RAISE EXCEPTION 'V52: ejercicio 257 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 262 barbell skier: high_impact: R2 L15 (MEDIA): No hay salto ni fase de vuelo en ningún fotograma: los pies no se despegan. La bandera HI y el impacto ALTO derivado no corresponden al GIF (impacto BAJO). Movi / V3 W01 ACEPTAR: f1-f12 inclinado con rodillas semiflexionadas llevando la barra con los brazos estirados hacia atrás; ambos pies siempre apoyados en la misma posición; no hay s / coordinador: esquiador con barra: pies fijos y doble apoyo en f1, f4, f7, f10; no es salto, pliometria ni olimpico (R2 + V3 ALTA) | impact_level: R2 L15 (MEDIA): pies=AMBOS_SIEMPRE; Inclinado hacia delante con las rodillas semiflexionadas, sujeta con ambas manos una barra en vertical delante de las piernas y la lleva con / V3 W01 ACEPTAR: f1, f4, f7, f10 ambos pies apoyados en el suelo sin cambio de posición; ningún fotograma con los pies en el aire / coordinador: doble apoyo en todos los fotogramas (V3 ALTA)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET high_impact = FALSE, impact_level = 'BAJO'
   WHERE exercise_id = 262 AND name_en = 'barbell skier' AND high_impact = TRUE AND impact_level = 'ALTO';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 262 AND name_en = 'barbell skier' AND high_impact = FALSE AND impact_level = 'BAJO') THEN
    RAISE EXCEPTION 'V52: ejercicio 262 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 267 barbell thruster: high_impact: R2 L20 (ALTA): No hay salto ni vuelo y no es un levantamiento olímpico (no hay cargada): HI y el impacto ALTO derivado no corresponden; impacto BAJO. AX y OH correctos. / V3 W01 ACEPTAR: f1 de pie con la barra en los hombros; f7 sentadilla profunda; f13 de pie con la barra sobre la cabeza; ambos pies apoyados en f1-f18; thruster sin salto y no e / coordinador: thruster: sentadilla y press sin salto; no es olimpico (R2 + V3 ALTA). AX y OH se mantienen | impact_level: R2 L20 (ALTA): pies=AMBOS_SIEMPRE; De pie con la barra apoyada delante de los hombros → sentadilla profunda → se levanta y empuja la barra por encima de la cabeza hasta extend / V3 W01 ACEPTAR: f1, f7, f10, f13, f16 ambos pies apoyados en el suelo; ningún fotograma con los dos pies en el aire / coordinador: doble apoyo en todos los fotogramas (V3 ALTA)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET high_impact = FALSE, impact_level = 'BAJO'
   WHERE exercise_id = 267 AND name_en = 'barbell thruster' AND high_impact = TRUE AND impact_level = 'ALTO';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 267 AND name_en = 'barbell thruster' AND high_impact = FALSE AND impact_level = 'BAJO') THEN
    RAISE EXCEPTION 'V52: ejercicio 267 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 268 barbell upright row: movement_pattern: R2 L01 (MEDIA): La barra se desplaza en vertical, no horizontal: HORIZONTAL_PULL no encaja; es abducción de hombro con codos flexionados (SHOULDER_RAISE; VERTICAL_PULL sería la / V3 V02 ACEPTAR: f1 barra delante de los muslos; f7 barra a la altura del pecho alto con codos abiertos por encima de las manos: trayectoria vertical, no horizontal. HORIZONTAL_
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET movement_pattern = 'SHOULDER_RAISE'
   WHERE exercise_id = 268 AND name_en = 'barbell upright row' AND movement_pattern = 'HORIZONTAL_PULL';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 268 AND name_en = 'barbell upright row' AND movement_pattern = 'SHOULDER_RAISE') THEN
    RAISE EXCEPTION 'V52: ejercicio 268 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 269 barbell upright row v. 2: requires_overhead: R2 L02 (MEDIA): En el punto alto las manos llegan a la frente y los codos superan los hombros, pero los brazos no quedan por encima de la cabeza; OH no aplica según la definici / V3 V02 ACEPTAR: f7 ampliado (vista posterior alta): brazos con codos a la altura de los hombros y manos a la altura de la cara/cabeza; la elevación de la cámara hace que la bar / coordinador: definicion literal demostrada en todos los fotogramas (R2 + V3) | movement_pattern: R2 L02 (MEDIA): En el punto alto las manos llegan a la frente y los codos superan los hombros, pero los brazos no quedan por encima de la cabeza; OH no aplica según la definici / V3 V02 ACEPTAR: f1 barra en muslos, f7 barra a la altura de la cara con codos a la altura de los hombros: trayectoria vertical, HORIZONTAL_PULL es incorrecto. Nota: 268 y 271 p
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = FALSE, movement_pattern = 'VERTICAL_PULL'
   WHERE exercise_id = 269 AND name_en = 'barbell upright row v. 2' AND requires_overhead = TRUE AND movement_pattern = 'HORIZONTAL_PULL';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 269 AND name_en = 'barbell upright row v. 2' AND requires_overhead = FALSE AND movement_pattern = 'VERTICAL_PULL') THEN
    RAISE EXCEPTION 'V52: ejercicio 269 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 270 barbell upright row v. 3: requires_overhead: R2 L03 (ALTA): En el punto alto las manos quedan a la altura de la barbilla y los codos a la altura de los hombros: los brazos no pasan por encima de la cabeza, OH sobra. La b / V3 V02 ACEPTAR: f1 barra delante de los muslos; f7 barra a la altura de los hombros/barbilla con codos a la altura de los hombros; brazos nunca por encima de la cabeza en los 1 / coordinador: definicion literal demostrada en todos los fotogramas (R2 + V3)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = FALSE
   WHERE exercise_id = 270 AND name_en = 'barbell upright row v. 3' AND requires_overhead = TRUE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 270 AND name_en = 'barbell upright row v. 3' AND requires_overhead = FALSE) THEN
    RAISE EXCEPTION 'V52: ejercicio 270 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 271 barbell wide-grip upright row: movement_pattern: R2 L04 (ALTA): El remo al mentón es una abducción/elevación de hombros en vertical, no un tirón horizontal; nivel 2 más coherente que 3 / V3 V02 ACEPTAR: f1 barra delante de los muslos; f7 barra a la altura del pecho alto con agarre ancho y codos abiertos: trayectoria vertical de elevación de hombros, no tirón ho
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET movement_pattern = 'SHOULDER_RAISE'
   WHERE exercise_id = 271 AND name_en = 'barbell wide-grip upright row' AND movement_pattern = 'HORIZONTAL_PULL';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 271 AND name_en = 'barbell wide-grip upright row' AND movement_pattern = 'SHOULDER_RAISE') THEN
    RAISE EXCEPTION 'V52: ejercicio 271 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 274 barbell sitted alternate leg raise (female): axial_load: R2 L07 (MEDIA): La barra cargada se mantiene por encima de la cabeza en línea con el tronco durante todo el ejercicio: carga compresiva sobre la columna / V3 V02 ACEPTAR: f1-f12 sentada en el suelo con la barra con discos sostenida con los brazos extendidos por encima de la cabeza durante toda la serie mientras alterna elevacione
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET axial_load = TRUE
   WHERE exercise_id = 274 AND name_en = 'barbell sitted alternate leg raise (female)' AND axial_load = FALSE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 274 AND name_en = 'barbell sitted alternate leg raise (female)' AND axial_load = TRUE) THEN
    RAISE EXCEPTION 'V52: ejercicio 274 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 275 barbell standing ab rollerout: requires_overhead: R2 L08 (MEDIA): Se quita OH: en la máxima extensión los brazos quedan perpendiculares al tronco. FL se mantiene por la plancha con apoyo en una barra en el suelo, aunque no hay / V3 V02 ACEPTAR: f1 de pie inclinado con manos sobre la barra apoyada en el suelo, brazos verticales; f7 plancha con brazos verticales bajo los hombros (unos 90° respecto al tro / coordinador: definicion literal demostrada en todos los fotogramas (R2 + V3) | instructions_es: R2 L08 (MEDIA): El texto pide sujetar la barra frente a los muslos y enrollarla hacia el suelo hasta que las manos queden sobre la cabeza; en el GIF la barra está siempre en el / V3 V02 ACEPTAR: f1-f12 la barra con discos está siempre en el suelo delante de los pies; f1 cadera alta, f7 plancha con hombros sobre la barra. El texto actual (sujetar la barr
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = FALSE, instructions_es = 'Ponte de pie con los pies separados a la anchura de la cadera, inclínate hacia delante y apoya las manos sobre una barra con discos colocada en el suelo delante de los pies. Con los brazos estirados y el abdomen activado, haz rodar la barra hacia delante hasta que el cuerpo quede en línea recta, en plancha, con los hombros por encima de la barra. Mantén la espalda recta, sin hundir la zona lumbar. Haz rodar la barra de vuelta hacia los pies elevando la cadera hasta la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies separados a la anchura de la cadera, inclínate hacia delante y apoya las manos sobre una barra con discos colocada en el suelo delante de los pies.", "Con los brazos estirados y el abdomen activado, haz rodar la barra hacia delante hasta que el cuerpo quede en línea recta, en plancha, con los hombros por encima de la barra.", "Mantén la espalda recta, sin hundir la zona lumbar.", "Haz rodar la barra de vuelta hacia los pies elevando la cadera hasta la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 275 AND name_en = 'barbell standing ab rollerout' AND requires_overhead = TRUE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '96d7c36dc72428b3b33a3f53ea6aa4af';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 275 AND name_en = 'barbell standing ab rollerout' AND requires_overhead = FALSE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3f74154e5adfc2ef1a07fbafc5b871a5') THEN
    RAISE EXCEPTION 'V52: ejercicio 275 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 279 barbell squat (on knees): instructions_es: R2 L12 (MEDIA): El texto pide bajar hasta que los muslos queden paralelos al suelo y empujar con los talones; en el GIF el recorrido es corto (los muslos pasan de inclinados a  / V3 W01 ACEPTAR: f1 de rodillas en el suelo con la barra en la espalda y la cadera atrás (muslos inclinados); f7 de rodillas con el tronco erguido y los muslos verticales; el re
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Arrodíllate en el suelo, mejor sobre una superficie acolchada, con las rodillas separadas a la anchura de las caderas. Coloca la barra sobre la parte alta de la espalda, sujetándola con las manos algo más separadas que los hombros. Con el pecho elevado y la espalda recta, lleva la cadera hacia atrás, hacia los talones. Empuja la cadera hacia delante hasta quedar de rodillas con el tronco erguido. Repite el número de repeticiones indicado.', instruction_steps_es = '["Arrodíllate en el suelo, mejor sobre una superficie acolchada, con las rodillas separadas a la anchura de las caderas.", "Coloca la barra sobre la parte alta de la espalda, sujetándola con las manos algo más separadas que los hombros.", "Con el pecho elevado y la espalda recta, lleva la cadera hacia atrás, hacia los talones.", "Empuja la cadera hacia delante hasta quedar de rodillas con el tronco erguido.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 279 AND name_en = 'barbell squat (on knees)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '84f1eb35293ccd7dc599a08c4f8a9756';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 279 AND name_en = 'barbell squat (on knees)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b53e82826fe3f8e79fc5321fbe07c4ba') THEN
    RAISE EXCEPTION 'V52: ejercicio 279 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 280 barbell squat jump step rear lunge: instructions_es: R2 L13 (MEDIA): El texto describe saltar lo más alto posible y aterrizar en el suelo; el GIF muestra un salto sobre un step, aterrizando encima, y la zancada inversa al bajar d / V3 W01 ACEPTAR: f1 de pie frente al step con la barra en la espalda; f7 sentadilla; f10-f13 de pie sobre el step (salto fundido, no hay fotograma de vuelo); f19-f22 baja del st
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie frente a un step con la barra apoyada sobre la parte alta de la espalda y los pies a la anchura de los hombros. Baja a una sentadilla flexionando rodillas y caderas, con la espalda recta. Salta de forma explosiva sobre el step y aterriza suavemente con ambos pies, con las rodillas alineadas con los pies. Baja del step hacia atrás con una pierna y desciende en zancada inversa hasta que la rodilla trasera quede cerca del suelo. Empuja con la pierna delantera para volver de pie. Alterna los lados. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie frente a un step con la barra apoyada sobre la parte alta de la espalda y los pies a la anchura de los hombros.", "Baja a una sentadilla flexionando rodillas y caderas, con la espalda recta.", "Salta de forma explosiva sobre el step y aterriza suavemente con ambos pies, con las rodillas alineadas con los pies.", "Baja del step hacia atrás con una pierna y desciende en zancada inversa hasta que la rodilla trasera quede cerca del suelo.", "Empuja con la pierna delantera para volver de pie. Alterna los lados.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 280 AND name_en = 'barbell squat jump step rear lunge' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e602e512e5bcd275c20c7704b8dabd38';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 280 AND name_en = 'barbell squat jump step rear lunge' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '6d934e17d1a30fe007891e5c07e7c1de') THEN
    RAISE EXCEPTION 'V52: ejercicio 280 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 286 barbell zercher squat: zona: R2 L19 (ALTA): El GIF resalta los cuádriceps. Nivel 3 discutible (perfil 2). / V3 Y01 ACEPTAR: f1-f12 resaltado rojo en los cuádriceps en todas las fases (f7 sentadilla abajo); sentadilla Zercher vista de frente (formato habitual upper legs/quads)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs'), target_muscle = 'quads'
   WHERE exercise_id = 286 AND name_en = 'barbell zercher squat' AND body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs') AND target_muscle = 'glutes';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 286 AND name_en = 'barbell zercher squat' AND body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs') AND target_muscle = 'quads') THEN
    RAISE EXCEPTION 'V52: ejercicio 286 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 292 barbell standing wide-grip curl: instructions_es: R2 L05 (ALTA): El texto dice agarre prono; el GIF muestra agarre supino (palmas hacia delante y hacia arriba al subir) / V3 V02 ACEPTAR: f1/f7 ampliados: bíceps resaltado y antebrazos en supinación al subir la barra a los hombros; el texto actual dice agarre prono, que corresponde a otro ejercici
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies separados a la anchura de las caderas y sujeta una barra con agarre supino, con las manos más separadas que los hombros. Deja la barra colgando delante de los muslos con los brazos extendidos. Sin mover la parte superior de los brazos, flexiona los codos y sube la barra hasta la altura de los hombros. Baja la barra despacio hasta extender los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies separados a la anchura de las caderas y sujeta una barra con agarre supino, con las manos más separadas que los hombros.", "Deja la barra colgando delante de los muslos con los brazos extendidos.", "Sin mover la parte superior de los brazos, flexiona los codos y sube la barra hasta la altura de los hombros.", "Baja la barra despacio hasta extender los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 292 AND name_en = 'barbell standing wide-grip curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '2a9a26653ecf6b03e1f5a5e4aea7d6ae';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 292 AND name_en = 'barbell standing wide-grip curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'afee7c3f85595cddf4a82e9dd7fe0f7b') THEN
    RAISE EXCEPTION 'V52: ejercicio 292 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 293 burpee: instructions_es: R2 L06 (ALTA): El texto pide una flexión de brazos en la plancha; en el GIF los brazos permanecen extendidos en la plancha (f12-f16) y no hay flexión. / V3 V02 ACEPTAR: f12-f17 plancha con brazos extendidos, sin flexión de codos; f24-f26 pies separados del suelo (salto) con brazos por encima de la cabeza. El texto actual añade  | difficulty_level: R2 L06 (ALTA): pliometría; coordinación moderada / V3 V02 ACEPTAR: f6-f20 bajada a cuclillas, paso a plancha y regreso; f24-f26 salto vertical con brazos arriba: coordinación de varias fases y salto repetido, perfil de nivel 2 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies separados a la anchura de los hombros. Baja en cuclillas y apoya las manos en el suelo delante de ti. Lleva los pies hacia atrás hasta quedar en plancha con los brazos extendidos y el cuerpo recto. Recoge los pies hacia las manos y vuelve a la posición de cuclillas. Salta hacia arriba llevando los brazos por encima de la cabeza y aterriza suavemente. Mantén la duración indicada.', instruction_steps_es = '["Ponte de pie con los pies separados a la anchura de los hombros.", "Baja en cuclillas y apoya las manos en el suelo delante de ti.", "Lleva los pies hacia atrás hasta quedar en plancha con los brazos extendidos y el cuerpo recto.", "Recoge los pies hacia las manos y vuelve a la posición de cuclillas.", "Salta hacia arriba llevando los brazos por encima de la cabeza y aterriza suavemente.", "Mantén la duración indicada."]'::jsonb, difficulty_level = 2
   WHERE exercise_id = 293 AND name_en = 'burpee' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '274a14d6ed2a43f68dde210bfe54eca6' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 293 AND name_en = 'burpee' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '859be9cc1bb94c167519c5bee0663f9e' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 293 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 294 bodyweight standing calf raise: instructions_es: R2 L07 (ALTA): El texto indica colocar las manos sobre una pared o superficie estable; el GIF muestra los brazos a los lados sin ningún apoyo / V3 V02 MEJORAR: f1-f12 de pie con los brazos a los lados, sin apoyo de manos, elevando y bajando talones. La corrección procede, pero la propuesta elimina la ayuda de equilibri
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie, erguido, con los pies separados a la anchura de las caderas y los brazos relajados a los lados. Si te cuesta mantener el equilibrio, apoya las manos en una pared o en una superficie estable. Levanta los talones del suelo llevando el peso hacia las puntas de los pies. Haz una pausa breve arriba y baja los talones con control hasta apoyarlos en el suelo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie, erguido, con los pies separados a la anchura de las caderas y los brazos relajados a los lados.", "Si te cuesta mantener el equilibrio, apoya las manos en una pared o en una superficie estable.", "Levanta los talones del suelo llevando el peso hacia las puntas de los pies.", "Haz una pausa breve arriba y baja los talones con control hasta apoyarlos en el suelo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 294 AND name_en = 'bodyweight standing calf raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5d5bc28e26946bdbe52359747e107411';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 294 AND name_en = 'bodyweight standing calf raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'fcfb9e3b884b31a7db6b870448894d48') THEN
    RAISE EXCEPTION 'V52: ejercicio 294 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 296 bodyweight incline side plank: requires_floor: R2 L09 (ALTA): El cuerpo no baja al suelo: solo los pies lo tocan y el apoyo superior está en el banco, por lo que FL no aplica. Se ve un solo lado. / V3 V02 ACEPTAR: f1-f12 posición estática: solo los pies tocan el suelo y la mano se apoya en el banco; el cuerpo nunca está tumbado, sentado ni a cuatro apoyos en el suelo / coordinador: definicion literal demostrada en todos los fotogramas (R2 + V3) | instructions_es: R2 L09 (ALTA): Pide apoyar el antebrazo con el codo bajo el hombro, pero el GIF muestra la mano apoyada en el banco con el brazo extendido / V3 V02 ACEPTAR: f1/f6 ampliados: la mano (puño) se apoya sobre el banco con el brazo casi extendido bajo el hombro; el antebrazo no descansa sobre el banco. El texto actual (an
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_floor = FALSE, instructions_es = 'Colócate de lado junto a un banco y apoya la mano sobre él, con el brazo extendido bajo el hombro. Extiende las piernas, una sobre otra, y apoya el borde de los pies en el suelo. Eleva la cadera hasta que el cuerpo forme una línea recta desde la cabeza hasta los pies. Mantén la posición sin dejar caer la cadera. Mantén la duración indicada.', instruction_steps_es = '["Colócate de lado junto a un banco y apoya la mano sobre él, con el brazo extendido bajo el hombro.", "Extiende las piernas, una sobre otra, y apoya el borde de los pies en el suelo.", "Eleva la cadera hasta que el cuerpo forme una línea recta desde la cabeza hasta los pies.", "Mantén la posición sin dejar caer la cadera.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 296 AND name_en = 'bodyweight incline side plank' AND requires_floor = TRUE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'cf54c6c050932f46b73c1358b5ec83bf';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 296 AND name_en = 'bodyweight incline side plank' AND requires_floor = FALSE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '4a7050ff92c8b8e9e5023eef9b96be0c') THEN
    RAISE EXCEPTION 'V52: ejercicio 296 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 298 bridge - mountain climber (cross body): instructions_es: R2 L11 (ALTA): Termina con «Repite el número de repeticiones deseado» pero la prescripción es DURATION; el resto coincide con el GIF / V3 W02 ACEPTAR: f1-f24 plancha alta con la rodilla cruzando bajo el cuerpo hacia el codo contrario, alternando (f5-f7 un lado, f17-f20 el otro); el contenido actual coincide co
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate en plancha alta con las manos bajo los hombros y el cuerpo en línea recta, apoyado en las puntas de los pies. Activa el core y lleva la rodilla derecha por debajo del cuerpo hacia el codo izquierdo. Devuelve el pie a la posición inicial y lleva la rodilla izquierda hacia el codo derecho. Mantén las caderas niveladas, sin subirlas ni dejarlas caer, y respira de forma constante. Mantén la duración indicada. Alterna los lados.', instruction_steps_es = '["Colócate en plancha alta con las manos bajo los hombros y el cuerpo en línea recta, apoyado en las puntas de los pies.", "Activa el core y lleva la rodilla derecha por debajo del cuerpo hacia el codo izquierdo.", "Devuelve el pie a la posición inicial y lleva la rodilla izquierda hacia el codo derecho.", "Mantén las caderas niveladas, sin subirlas ni dejarlas caer, y respira de forma constante.", "Mantén la duración indicada. Alterna los lados."]'::jsonb
   WHERE exercise_id = 298 AND name_en = 'bridge - mountain climber (cross body)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd849361a5367da3c29ee2b5cfab484c6';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 298 AND name_en = 'bridge - mountain climber (cross body)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'fd7858f8c00d70ef8842506f5bca4a44') THEN
    RAISE EXCEPTION 'V52: ejercicio 298 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 300 bench pull-ups: equipo: R2 L13 (MEDIA): Un pie se mantiene siempre apoyado en el banco (codificado AMBOS_SIEMPRE por falta de clase; el otro pie queda libre): por la definición literal no cuelga con l / V3 W02 ACEPTAR: f1-f12 la barra fija sostiene el agarre y el banco sostiene un pie en todo momento: ambos son imprescindibles a la vez, por lo que deben ir en grupos distintos | difficulty_level: R2 L13 (MEDIA): fuerza relativa moderada; agarre / V3 W02 ACEPTAR: f1-f7 tracción vertical colgado de la barra con solo un pie de apoyo en el banco: fuerza relativa moderada y peso corporal parcialmente suspendido, no asequible
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 300 AND name_en = 'bench pull-ups' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 300 AND name_en = 'bench pull-ups' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 300 no esta en el estado anterior esperado';
  END IF;
END $v52$;
DO $v52$ BEGIN
  IF (SELECT count(*) FROM equipment_types WHERE code IN ('bench', 'pull-up bar')) <> 2 THEN
    RAISE EXCEPTION 'V52: faltan codigos de equipamiento para 300';
  END IF;
  IF (SELECT coalesce(string_agg(r.group_no || ':' || t.code, ',' ORDER BY r.group_no || ':' || t.code), '') FROM exercise_equipment_requirements r JOIN equipment_types t ON t.equipment_id = r.equipment_id WHERE r.exercise_id = 300) = '1:pull-up bar,2:bench' THEN RETURN; END IF;
  IF (SELECT coalesce(string_agg(r.group_no || ':' || t.code, ',' ORDER BY r.group_no || ':' || t.code), '') FROM exercise_equipment_requirements r JOIN equipment_types t ON t.equipment_id = r.equipment_id WHERE r.exercise_id = 300) <> '1:bench,1:pull-up bar' THEN RAISE EXCEPTION 'V52: requisitos de 300 distintos de los esperados'; END IF;
  DELETE FROM exercise_equipment_requirements WHERE exercise_id = 300;
  INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id) VALUES (300, 1, (SELECT equipment_id FROM equipment_types WHERE code = 'pull-up bar'));
  INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id) VALUES (300, 2, (SELECT equipment_id FROM equipment_types WHERE code = 'bench'));
  UPDATE exercises SET equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'pull-up bar') WHERE exercise_id = 300;
END $v52$;
-- 302 bodyweight squatting row (with towel): instructions_es: R2 L15 (MEDIA): El texto indica empezar de pie y bajar a la sentadilla mientras se tira; en el GIF la sentadilla profunda se mantiene todo el tiempo y solo se rema / V3 W02 ACEPTAR: f1 sentadilla profunda con brazos extendidos sujetando la toalla pasada por la espaldera; f7 manos al pecho sin cambiar la altura de la cadera: la sentadilla se
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Pasa una toalla resistente por un barrote de la espaldera a la altura del pecho y sujeta un extremo con cada mano; comprueba que la espaldera está bien fijada a la pared y que la toalla no resbala. Coloca los pies cerca de la base de la espaldera y baja a una sentadilla profunda. Inclina el cuerpo hacia atrás hasta extender los brazos, con la espalda recta. Tira de la toalla hasta llevar las manos al pecho, juntando los omóplatos, sin salir de la sentadilla. Extiende los brazos despacio hasta volver a la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Pasa una toalla resistente por un barrote de la espaldera a la altura del pecho y sujeta un extremo con cada mano; comprueba que la espaldera está bien fijada a la pared y que la toalla no resbala.", "Coloca los pies cerca de la base de la espaldera y baja a una sentadilla profunda.", "Inclina el cuerpo hacia atrás hasta extender los brazos, con la espalda recta.", "Tira de la toalla hasta llevar las manos al pecho, juntando los omóplatos, sin salir de la sentadilla.", "Extiende los brazos despacio hasta volver a la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 302 AND name_en = 'bodyweight squatting row (with towel)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '57345b6f38cba1794faedd0100ea3f3b';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 302 AND name_en = 'bodyweight squatting row (with towel)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e11debf1176d47031c5d1cef247b6c2b') THEN
    RAISE EXCEPTION 'V52: ejercicio 302 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 303 bodyweight standing close-grip one arm row: instructions_es: R2 L16 (MEDIA): El texto describe un remo inclinado con mancuerna; el GIF muestra un remo con el peso corporal agarrado a una espaldera, con el cuerpo inclinado hacia atrás / V3 W02 ACEPTAR: f1 una mano agarrada a un barrote de la espaldera, cuerpo recto inclinado hacia atrás con los pies junto a la base; f7 cuerpo casi vertical junto a la espaldera
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Agárrate a un barrote de la espaldera con una mano, a la altura del pecho, con los pies junto a la base. Deja caer el cuerpo hacia atrás hasta extender el brazo, con el cuerpo recto y los pies fijos. Tira con el brazo para acercar el pecho a la espaldera, manteniendo el codo cerca del cuerpo. Vuelve con control a la posición de brazo extendido. Completa las repeticiones y cambia de brazo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Agárrate a un barrote de la espaldera con una mano, a la altura del pecho, con los pies junto a la base.", "Deja caer el cuerpo hacia atrás hasta extender el brazo, con el cuerpo recto y los pies fijos.", "Tira con el brazo para acercar el pecho a la espaldera, manteniendo el codo cerca del cuerpo.", "Vuelve con control a la posición de brazo extendido.", "Completa las repeticiones y cambia de brazo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 303 AND name_en = 'bodyweight standing close-grip one arm row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a78baad3d3cb2eec653de3d0759c3b9f';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 303 AND name_en = 'bodyweight standing close-grip one arm row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '746c4ec12a316eb5dc39ce2a7dba2b04') THEN
    RAISE EXCEPTION 'V52: ejercicio 303 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 305 bodyweight standing one arm row: instructions_es: R2 L18 (ALTA): El texto describe un remo con mancuerna inclinado hacia delante; el GIF muestra un remo con el peso corporal, agarrado con una mano a una espaldera e inclinado  / V3 W02 ACEPTAR: f1 una sola mano en el barrote, cuerpo recto inclinado hacia atrás y pies junto a la base de la espaldera; f7 pecho junto al barrote con el codo flexionado: el 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie frente a una espaldera con los pies cerca de su base. Agarra un barrote a la altura del pecho con una mano y deja caer el cuerpo hacia atrás hasta extender el brazo, con el cuerpo recto. Tira con el brazo para acercar el pecho al barrote, juntando el omóplato. Vuelve con control a la posición inicial. Completa las repeticiones y cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie frente a una espaldera con los pies cerca de su base.", "Agarra un barrote a la altura del pecho con una mano y deja caer el cuerpo hacia atrás hasta extender el brazo, con el cuerpo recto.", "Tira con el brazo para acercar el pecho al barrote, juntando el omóplato.", "Vuelve con control a la posición inicial.", "Completa las repeticiones y cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 305 AND name_en = 'bodyweight standing one arm row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '9e58cc085be4c7b47fe8cdf15601d760';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 305 AND name_en = 'bodyweight standing one arm row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'efb449cffde06f69271dea7cef16f19e') THEN
    RAISE EXCEPTION 'V52: ejercicio 305 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 306 bodyweight standing one arm row (with towel): instructions_es: R2 L19 (ALTA): El texto dice inclinarse hacia delante desde las caderas y bajar la toalla; el GIF muestra el cuerpo recto inclinado hacia atrás desde los pies, y es el cuerpo  / V3 Y01 ACEPTAR: f1 cuerpo recto inclinado hacia atrás desde los pies junto a la base de la espaldera, brazo estirado sujetando la toalla; f7 cuerpo erguido junto a la espaldera
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Pasa una toalla resistente por un barrote de la espaldera a la altura del pecho y sujeta los dos extremos con una mano. Antes de cargar tu peso, comprueba que la espaldera está bien fijada a la pared y que la toalla no resbala. Coloca los pies cerca de la base de la espaldera y déjate caer hacia atrás con el cuerpo recto y el brazo estirado. Tira de la toalla llevando el codo hacia atrás hasta acercar el pecho a la espaldera y vuelve con control hasta estirar el brazo. Completa las repeticiones y cambia de brazo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Pasa una toalla resistente por un barrote de la espaldera a la altura del pecho y sujeta los dos extremos con una mano. Antes de cargar tu peso, comprueba que la espaldera está bien fijada a la pared y que la toalla no resbala.", "Coloca los pies cerca de la base de la espaldera y déjate caer hacia atrás con el cuerpo recto y el brazo estirado.", "Tira de la toalla llevando el codo hacia atrás hasta acercar el pecho a la espaldera y vuelve con control hasta estirar el brazo.", "Completa las repeticiones y cambia de brazo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 306 AND name_en = 'bodyweight standing one arm row (with towel)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '4fc2d9854795c5b5967888e502892ddc';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 306 AND name_en = 'bodyweight standing one arm row (with towel)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '2bb5756679cd0df8aa4b7874e53349f2') THEN
    RAISE EXCEPTION 'V52: ejercicio 306 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 307 bodyweight standing row: instructions_es: R2 L20 (ALTA): El texto describe tirar de la barra hacia el cuerpo como si se moviera; en el GIF el travesaño de la espaldera está fijo y es el cuerpo, inclinado hacia atrás c / V3 W02 ACEPTAR: f1 ambas manos en un travesaño fijo de la espaldera con el cuerpo recto inclinado hacia atrás y los pies en la base; f7 cuerpo vertical junto a la espaldera: es
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate de pie frente a una espaldera, con los pies cerca de su base. Sujeta un travesaño a la altura del pecho con ambas manos y deja caer el cuerpo hacia atrás, recto, hasta extender los brazos. Con el abdomen activado, tira del cuerpo hacia la espaldera flexionando los codos y juntando los omóplatos. Vuelve con control a la posición inclinada extendiendo los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate de pie frente a una espaldera, con los pies cerca de su base.", "Sujeta un travesaño a la altura del pecho con ambas manos y deja caer el cuerpo hacia atrás, recto, hasta extender los brazos.", "Con el abdomen activado, tira del cuerpo hacia la espaldera flexionando los codos y juntando los omóplatos.", "Vuelve con control a la posición inclinada extendiendo los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 307 AND name_en = 'bodyweight standing row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'f90f901218f4b2bd3da954ae31119930';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 307 AND name_en = 'bodyweight standing row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '1b54c24113f4b2aff141ffecff6e71ba') THEN
    RAISE EXCEPTION 'V52: ejercicio 307 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 312 butterfly yoga pose: instructions_es: R2 L05 (ALTA): El texto pide sentarse erguido y presionar las rodillas hacia el suelo; el GIF muestra una flexión del tronco hacia delante sobre los pies que se repite suaveme / V3 V02 ACEPTAR: f1 sentado con plantas juntas y manos en los pies; f7 tronco flexionado hacia delante con la cabeza hacia los pies; vuelve a erguirse de forma rítmica. El texto | is_stretch: R2 L05 (ALTA): Sentado en el suelo con las plantas de los pies juntas, rodillas abiertas y manos en los pies → inclina el tronco hacia delante bajando la cabeza hacia los pies / V3 V02 ACEPTAR: f1-f12 postura de mariposa sentado mantenida con balanceo suave del tronco hacia los pies: estiramiento de aductores (mantener elongación con balanceos suaves s
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en el suelo, junta las plantas de los pies y deja que las rodillas caigan hacia los lados. Sujeta los pies con las manos y alarga la espalda. Inclina el tronco hacia delante desde la cadera, acercando el pecho a los pies hasta notar el estiramiento en la cara interna de los muslos. Vuelve a erguirte despacio y repite el balanceo de forma suave. Mantén la duración indicada.', instruction_steps_es = '["Siéntate en el suelo, junta las plantas de los pies y deja que las rodillas caigan hacia los lados.", "Sujeta los pies con las manos y alarga la espalda.", "Inclina el tronco hacia delante desde la cadera, acercando el pecho a los pies hasta notar el estiramiento en la cara interna de los muslos.", "Vuelve a erguirte despacio y repite el balanceo de forma suave.", "Mantén la duración indicada."]'::jsonb, is_stretch = TRUE
   WHERE exercise_id = 312 AND name_en = 'butterfly yoga pose' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '968da4be867343a466f017c428b4619a' AND is_stretch = FALSE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 312 AND name_en = 'butterfly yoga pose' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e90e5a1890e30140924756ac6381dcb2' AND is_stretch = TRUE) THEN
    RAISE EXCEPTION 'V52: ejercicio 312 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 313 bench dip (knees bent): instructions_es: R2 L06 (ALTA): El texto pide estirar las piernas frente a ti con los talones en el suelo; el GIF y el nombre muestran las rodillas flexionadas con los pies planos en el suelo. / V3 V02 ACEPTAR: f1/f7 ampliados: rodillas flexionadas unos 90° con los pies planos en el suelo, manos en el borde del banco detrás de la cadera; el texto actual pide piernas es
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en el borde de un banco y apoya las manos en el borde junto a las caderas. Desliza los glúteos fuera del banco, con las rodillas flexionadas y los pies apoyados en el suelo. Flexiona los codos y baja el cuerpo, manteniendo la espalda cerca del banco. Empuja con las manos hasta extender los brazos y volver a la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en el borde de un banco y apoya las manos en el borde junto a las caderas.", "Desliza los glúteos fuera del banco, con las rodillas flexionadas y los pies apoyados en el suelo.", "Flexiona los codos y baja el cuerpo, manteniendo la espalda cerca del banco.", "Empuja con las manos hasta extender los brazos y volver a la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 313 AND name_en = 'bench dip (knees bent)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '842f3ac6c7b9b306c678956c40caf197';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 313 AND name_en = 'bench dip (knees bent)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd8bf835896abe151772ee42286142b95') THEN
    RAISE EXCEPTION 'V52: ejercicio 313 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 316 biceps narrow pull-ups: difficulty_level: R2 L09 (MEDIA): fuerza relativa moderada con el cuerpo casi horizontal / V3 V02 ACEPTAR: f1-f12 remo invertido en barra baja de máquina Smith con cuerpo casi horizontal y solo talones en el suelo: tirar del peso corporal en esa inclinación exige fue
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 316 AND name_en = 'biceps narrow pull-ups' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 316 AND name_en = 'biceps narrow pull-ups' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 316 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 317 biceps pull-up: name_es: R2 L10 (MEDIA): El nombre 'Dominada con agarre' está incompleto. El nombre de la fuente (biceps pull-up) suele indicar agarre supino, pero el GIF no permite confirmarlo. / V3 V02 ACEPTAR: f1 colgado de la barra con brazos extendidos, f7 barbilla a la altura de la barra; vista posterior con bíceps resaltados; agarre no visible. «Dominada con agarr
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Dominada para bíceps'
   WHERE exercise_id = 317 AND name_en = 'biceps pull-up' AND name_es = 'Dominada con agarre';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 317 AND name_en = 'biceps pull-up' AND name_es = 'Dominada para bíceps') THEN
    RAISE EXCEPTION 'V52: ejercicio 317 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 318 body-up: difficulty_level: R2 L11 (MEDIA): fuerza relativa moderada de tríceps / V3 W02 ACEPTAR: f1 plancha sobre antebrazos; f7 plancha alta con brazos extendidos y vuelta a f12: extensión de codos cargando gran parte del peso corporal, fuerza relativa mod
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 318 AND name_en = 'body-up' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 318 AND name_en = 'body-up' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 318 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 320 bodyweight side lying biceps curl: instructions_es: R2 L13 (BAJA): El texto describe tumbarse de lado con la cabeza sobre el brazo y flexionar un codo sin carga; el GIF muestra el tronco elevándose hacia las rodillas con las ma / V3 W02 MEJORAR: Vista cenital: f1 tumbado boca arriba con las rodillas flexionadas y los brazos junto al cuerpo; f7 tronco elevado hacia las rodillas con las manos junto a las 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en el suelo con las rodillas flexionadas. Lleva los brazos a lo largo del cuerpo con las manos hacia las rodillas. Eleva la cabeza y el tronco curvándolo hacia las rodillas, ayudándote de las manos junto a las piernas. Vuelve despacio al suelo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en el suelo con las rodillas flexionadas.", "Lleva los brazos a lo largo del cuerpo con las manos hacia las rodillas.", "Eleva la cabeza y el tronco curvándolo hacia las rodillas, ayudándote de las manos junto a las piernas.", "Vuelve despacio al suelo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 320 AND name_en = 'bodyweight side lying biceps curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '6a80082182651837f8f02cad1bc92dbd';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 320 AND name_en = 'bodyweight side lying biceps curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'f5c17db27b3f876df2ca55acd222b68e') THEN
    RAISE EXCEPTION 'V52: ejercicio 320 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 321 cable alternate shoulder press: axial_load: R2 L14 (MEDIA): La definición de AX incluye cargas en press por encima de la cabeza; en el GIF solo empuja un brazo (la alternancia no se ve) / V3 W02 ACEPTAR: f1 asas a la altura de los hombros; f5-f7 un brazo empuja el cable desde abajo hasta la extensión completa por encima de la cabeza: press por encima de la cabez
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET axial_load = TRUE
   WHERE exercise_id = 321 AND name_en = 'cable alternate shoulder press' AND axial_load = FALSE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 321 AND name_en = 'cable alternate shoulder press' AND axial_load = TRUE) THEN
    RAISE EXCEPTION 'V52: ejercicio 321 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 324 cable cross-over variation: instructions_es: R2 L17 (MEDIA): El texto pide poleas a la altura del pecho, un pie delante del otro, dar un paso adelante y juntar las manos frente al pecho; el GIF muestra los pies paralelos  / V3 W02 ACEPTAR: f1 brazos abiertos a la altura de los hombros con los cables llegando desde arriba; f7 manos juntas delante de la cadera; pies paralelos sin paso en todos los f
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate de pie en el centro de una estación de poleas con los pies paralelos y sujeta un agarre en cada mano, con los cables viniendo desde arriba. Abre los brazos a los lados a la altura de los hombros con una ligera flexión de codos. Lleva las manos hacia abajo y hacia delante describiendo un arco hasta juntarlas delante de la cadera. Vuelve con control a la posición inicial manteniendo el tronco erguido. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate de pie en el centro de una estación de poleas con los pies paralelos y sujeta un agarre en cada mano, con los cables viniendo desde arriba.", "Abre los brazos a los lados a la altura de los hombros con una ligera flexión de codos.", "Lleva las manos hacia abajo y hacia delante describiendo un arco hasta juntarlas delante de la cadera.", "Vuelve con control a la posición inicial manteniendo el tronco erguido.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 324 AND name_en = 'cable cross-over variation' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '8c875eea4b6c378406eadafb03d9b4f4';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 324 AND name_en = 'cable cross-over variation' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e151677a6fdd75a63d779aa52b3db697') THEN
    RAISE EXCEPTION 'V52: ejercicio 324 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 325 cable bar lateral pulldown: name_es: R2 L18 (ALTA): El tirón es frontal hacia el pecho; lateral no corresponde a ningún desplazamiento lateral (probable traducción literal de lat pulldown). Se sienta en un banco  / V3 W02 ACEPTAR: f1 brazos extendidos agarrando la barra de la polea alta, sentado en un banco plano; f7 codos abajo junto al tronco con la barra delante del cuerpo (no detrás d
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Jalón al pecho con barra en polea alta'
   WHERE exercise_id = 325 AND name_en = 'cable bar lateral pulldown' AND name_es = 'Jalón lateral con barra de cable';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 325 AND name_en = 'cable bar lateral pulldown' AND name_es = 'Jalón al pecho con barra en polea alta') THEN
    RAISE EXCEPTION 'V52: ejercicio 325 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 326 cable cross-over lateral pulldown: instructions_es: R2 L19 (ALTA): El texto dice poleas a la altura de los hombros, dar un paso atrás, inclinarse hacia delante y tirar hacia abajo y a través del cuerpo; el GIF muestra poleas al / V3 Y01 ACEPTAR: f1 y f12 vista de espaldas con los brazos cruzados por encima de la cabeza y los cables cruzados desde poleas altas; f7 manos abiertas a la altura de los hombro
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca un asa en cada polea alta y ponte de pie en el centro con los pies separados a la anchura de los hombros. Sujeta cada asa con la mano contraria, de modo que los brazos queden cruzados por encima de la cabeza. Con el tronco erguido, tira de las asas hacia abajo y hacia fuera hasta que las manos queden a la altura de los hombros, juntando los omóplatos. Vuelve con control a la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca un asa en cada polea alta y ponte de pie en el centro con los pies separados a la anchura de los hombros.", "Sujeta cada asa con la mano contraria, de modo que los brazos queden cruzados por encima de la cabeza.", "Con el tronco erguido, tira de las asas hacia abajo y hacia fuera hasta que las manos queden a la altura de los hombros, juntando los omóplatos.", "Vuelve con control a la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 326 AND name_en = 'cable cross-over lateral pulldown' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a728255f7da52a744552f570df85e9fa';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 326 AND name_en = 'cable cross-over lateral pulldown' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ad1c11089ebb058d11fb9e3ad00b5acd') THEN
    RAISE EXCEPTION 'V52: ejercicio 326 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 327 cable assisted inverse leg curl: difficulty_level: R2 L20 (MEDIA): fuerza excéntrica de isquiotibiales alta / V3 W02 ACEPTAR: f1 de rodillas en la almohadilla con el cable sujeto al tronco; f5-f7 desciende hasta quedar casi tendido horizontal controlando con los isquiotibiales y vuelve
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 327 AND name_en = 'cable assisted inverse leg curl' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 327 AND name_en = 'cable assisted inverse leg curl' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 327 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 333 cable forward raise: instructions_es: R2 L05 (ALTA): El texto habla de ambos brazos extendidos frente al cuerpo desde el inicio; el GIF muestra un solo brazo que parte desde el costado del muslo / V3 V02 ACEPTAR: f1 un solo brazo con el agarre junto al muslo, cable hacia abajo; f7 brazo extendido por delante a la altura del hombro; la otra mano no sujeta nada. El texto a
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie junto a una polea baja con los pies separados a la anchura de las caderas. Sujeta el agarre con una mano, con el brazo extendido junto al muslo. Con el codo casi recto, eleva el brazo por delante del cuerpo hasta la altura del hombro. Bájalo despacio hasta la posición inicial. Completa las repeticiones y cambia de brazo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie junto a una polea baja con los pies separados a la anchura de las caderas.", "Sujeta el agarre con una mano, con el brazo extendido junto al muslo.", "Con el codo casi recto, eleva el brazo por delante del cuerpo hasta la altura del hombro.", "Bájalo despacio hasta la posición inicial.", "Completa las repeticiones y cambia de brazo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 333 AND name_en = 'cable forward raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b53962e31c842cc56d73ae0bcc70d9ec';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 333 AND name_en = 'cable forward raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '94a9657cdcb543920410d9a7bba335e8') THEN
    RAISE EXCEPTION 'V52: ejercicio 333 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 340 cable decline press: instructions_es: R2 L12 (ALTA): El texto dice sentarse en el banco y mantener los pies planos en el suelo; el GIF muestra al usuario tumbado en el banco declinado con las piernas enganchadas e / V3 W02 ACEPTAR: f1-f12 tumbado en banco declinado con la cabeza por debajo de la cadera y las piernas enganchadas en los rodillos, sin pies en el suelo; los cables salen hacia 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca un banco declinado entre las poleas bajas. Túmbate boca arriba en el banco con las piernas enganchadas en los rodillos y la cabeza más baja que la cadera. Sujeta un asa en cada mano a la altura del pecho. Empuja las asas hasta extender los brazos y contrae el pecho. Vuelve con control a la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca un banco declinado entre las poleas bajas.", "Túmbate boca arriba en el banco con las piernas enganchadas en los rodillos y la cabeza más baja que la cadera.", "Sujeta un asa en cada mano a la altura del pecho.", "Empuja las asas hasta extender los brazos y contrae el pecho.", "Vuelve con control a la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 340 AND name_en = 'cable decline press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'fbb12fb27edea25eefaf1f4c4809b436';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 340 AND name_en = 'cable decline press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '315baafca73972c6e0eddff0ee16b884') THEN
    RAISE EXCEPTION 'V52: ejercicio 340 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 341 cable incline bench press: instructions_es: R2 L13 (MEDIA): El texto dice usar poleas altas y sentarse frente a la máquina; el GIF muestra los cables llegando desde abajo y desde detrás del banco, con el usuario de espal / V3 W02 ACEPTAR: f1 recostado en banco inclinado con los pies en el suelo y las asas a la altura de los hombros; los cables bajan hacia atrás y por detrás del banco; f7 brazos e
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ajusta un banco inclinado entre las poleas, con los cables a altura baja o media y por detrás del banco. Recuéstate en el banco con los pies apoyados en el suelo y sujeta un asa con cada mano a la altura de los hombros. Empuja las asas hacia arriba hasta extender los brazos por completo. Baja las asas despacio hasta la altura de los hombros. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ajusta un banco inclinado entre las poleas, con los cables a altura baja o media y por detrás del banco.", "Recuéstate en el banco con los pies apoyados en el suelo y sujeta un asa con cada mano a la altura de los hombros.", "Empuja las asas hacia arriba hasta extender los brazos por completo.", "Baja las asas despacio hasta la altura de los hombros.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 341 AND name_en = 'cable incline bench press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '2c7b0370d985cbd66c1250d04eb875f6';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 341 AND name_en = 'cable incline bench press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'f0c0bdc78e12f2ce390a9da88867d5a7') THEN
    RAISE EXCEPTION 'V52: ejercicio 341 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 344 cable low fly: instructions_es: R2 L16 (ALTA): El texto dice cruzar los brazos frente al cuerpo y sentir el estiramiento del pecho arriba; el GIF muestra un arco de abajo hacia arriba en el que las manos se  / V3 W02 ACEPTAR: f1 brazos abajo y algo separados del cuerpo con los cables desde poleas bajas; f7 brazos al frente a la altura del pecho con las manos juntas sin cruzarse: arco
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca un asa en cada polea baja y ponte de pie en el centro con los pies separados a la altura de los hombros. Sujeta las asas con los brazos abajo, un poco separados del cuerpo y con una ligera flexión de codos. Lleva los brazos hacia delante y hacia arriba en un arco amplio hasta juntar las manos frente al pecho. Baja los brazos con control por el mismo recorrido. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca un asa en cada polea baja y ponte de pie en el centro con los pies separados a la altura de los hombros.", "Sujeta las asas con los brazos abajo, un poco separados del cuerpo y con una ligera flexión de codos.", "Lleva los brazos hacia delante y hacia arriba en un arco amplio hasta juntar las manos frente al pecho.", "Baja los brazos con control por el mismo recorrido.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 344 AND name_en = 'cable low fly' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'eec3454ef63782db3d71e616fb5ffc2d';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 344 AND name_en = 'cable low fly' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '6df7b34ce0423675348992b46f84517d') THEN
    RAISE EXCEPTION 'V52: ejercicio 344 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 347 cable judo flip: instructions_es: R2 L19 (MEDIA): El texto dice bajar hasta que las manos queden cerca de las rodillas; en el GIF las manos permanecen junto a la cabeza durante todo el recorrido / V3 Y01 ACEPTAR: f1 de pie con el asa junto al cuello y el cable desde arriba y detrás; f7 tronco casi horizontal con las manos junto a la cabeza; en ningún fotograma las manos  | movement_pattern: R2 L19 (MEDIA): No se aprecia rotación ni flexión lateral del tronco: el movimiento visible es una flexión de tronco y cadera hacia delante. / V3 Y01 ACEPTAR: f1-f12 vista frontal: flexión de tronco y cadera hacia delante (f7 tronco casi horizontal) sin rotación ni inclinación lateral visibles; resaltado en el abdomen
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie de espaldas a una polea alta con los pies separados a la anchura de las caderas. Sujeta el asa con ambas manos junto al cuello, con el cable pasando por encima del hombro. Flexiona la cadera y el tronco hacia delante tirando del cable hacia abajo, con las manos junto a la cabeza. Baja hasta que el tronco quede casi horizontal y vuelve con control a la posición erguida. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie de espaldas a una polea alta con los pies separados a la anchura de las caderas.", "Sujeta el asa con ambas manos junto al cuello, con el cable pasando por encima del hombro.", "Flexiona la cadera y el tronco hacia delante tirando del cable hacia abajo, con las manos junto a la cabeza.", "Baja hasta que el tronco quede casi horizontal y vuelve con control a la posición erguida.", "Repite el número de repeticiones indicado."]'::jsonb, movement_pattern = 'TRUNK_FLEXION'
   WHERE exercise_id = 347 AND name_en = 'cable judo flip' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '1ead48c989f0bb2dd9bcd9e4bb60906d' AND movement_pattern = 'TRUNK_ROTATION_LATERAL_FLEXION';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 347 AND name_en = 'cable judo flip' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '53a2e254ab7ce6a276784c6605e710d0' AND movement_pattern = 'TRUNK_FLEXION') THEN
    RAISE EXCEPTION 'V52: ejercicio 347 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 348 cable kneeling crunch: instructions_es: R2 L20 (ALTA): El texto indica ponerse de espaldas a la máquina y la cuerda detrás de la cabeza; el GIF muestra a la persona de cara a la polea con la cuerda delante de la fre / V3 W02 ACEPTAR: f1 arrodillado en el suelo de cara a la máquina con la cuerda junto a la frente; f7 tronco flexionado hacia el suelo, codos junto a las rodillas y cadera retroc
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Engancha una cuerda a una polea alta y arrodíllate en el suelo de cara a la máquina. Sujeta la cuerda con ambas manos y llévala junto a la frente, con los codos flexionados. Flexiona el tronco hacia el suelo, llevando los codos hacia las rodillas mientras encoges el abdomen. Haz una pausa abajo y vuelve despacio a la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Engancha una cuerda a una polea alta y arrodíllate en el suelo de cara a la máquina.", "Sujeta la cuerda con ambas manos y llévala junto a la frente, con los codos flexionados.", "Flexiona el tronco hacia el suelo, llevando los codos hacia las rodillas mientras encoges el abdomen.", "Haz una pausa abajo y vuelve despacio a la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 348 AND name_en = 'cable kneeling crunch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3da4c4b585d849b306a1d05701ac664e';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 348 AND name_en = 'cable kneeling crunch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '62a4b9760563781bc4b95901802b85e5') THEN
    RAISE EXCEPTION 'V52: ejercicio 348 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 350 cable floor seated wide-grip row: instructions_es: R2 L02 (ALTA): El texto dice que la máquina está detrás de ti y que tires hacia la cintura; el GIF muestra la polea delante, con los pies hacia ella, y la barra llega al pecho / V3 V02 ACEPTAR: f1/f7 ampliados: sentado en el suelo con los pies hacia la columna de polea, que está delante; barra ancha llevada al pecho con codos abiertos a la altura de lo
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en el suelo frente a una polea baja con las piernas extendidas y los pies hacia la máquina. Sujeta una barra ancha con agarre prono y los brazos estirados, con la espalda recta. Tira de la barra hacia el pecho abriendo los codos hacia los lados y juntando los omóplatos. Vuelve despacio a estirar los brazos sin redondear la espalda. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en el suelo frente a una polea baja con las piernas extendidas y los pies hacia la máquina.", "Sujeta una barra ancha con agarre prono y los brazos estirados, con la espalda recta.", "Tira de la barra hacia el pecho abriendo los codos hacia los lados y juntando los omóplatos.", "Vuelve despacio a estirar los brazos sin redondear la espalda.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 350 AND name_en = 'cable floor seated wide-grip row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '1ff661bd28368c557f57fdc285dc69a6';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 350 AND name_en = 'cable floor seated wide-grip row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '9360d740c8065f3f9a20eed6bac86cff') THEN
    RAISE EXCEPTION 'V52: ejercicio 350 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 354 cable lat pulldown full range of motion: name_es: R2 L06 (MEDIA): «Polea al pecho» designa el jalón al pecho sentado; el GIF muestra un tirón de pie desde arriba hasta los muslos. En f1 las manos quedan a la altura de la frent / V3 V02 ACEPTAR: f1 de pie en estación de poleas altas con los brazos cruzados en alto delante de la cabeza; f7 manos junto a los muslos. «Polea al pecho» designa el jalón senta
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Jalón de pie con poleas altas cruzadas'
   WHERE exercise_id = 354 AND name_en = 'cable lat pulldown full range of motion' AND name_es = 'Polea al pecho con rango completo';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 354 AND name_en = 'cable lat pulldown full range of motion' AND name_es = 'Jalón de pie con poleas altas cruzadas') THEN
    RAISE EXCEPTION 'V52: ejercicio 354 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 356 cable lateral pulldown with v-bar: name_es: R2 L08 (MEDIA): 'Lateral' en la fuente parece referirse a 'lat' (dorsal); en español 'jalón lateral' sugiere un tirón de lado. El banco para sentarse aparece. / V3 V02 ACEPTAR: f1 sentado en banco con asa en V y brazos extendidos por encima de la cabeza; f7 asa en la parte alta del pecho. «Jalón lateral» sugiere un tirón de lado que no | instructions_es: R2 L08 (MEDIA): El texto indica agarre prono y ajustar una almohadilla de rodillas; el GIF muestra un asa en V (agarre neutro) y un banco plano sin almohadilla de muslos visibl / V3 V02 ACEPTAR: f1/f7 ampliados: banco plano sin almohadilla de muslos; asa en V (agarre neutro, el prono es imposible con ese accesorio); tirón desde brazos extendidos arriba 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Jalón al pecho en polea alta con asa en V', instructions_es = 'Siéntate en un banco frente a la polea alta con los pies apoyados en el suelo. Sujeta el asa en V con ambas manos y los brazos extendidos por encima de la cabeza. Con la espalda recta y el tronco algo inclinado hacia atrás, tira del asa hacia la parte alta del pecho llevando los codos hacia abajo. Aprieta los músculos de la espalda en la parte baja del movimiento. Vuelve despacio hasta extender los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en un banco frente a la polea alta con los pies apoyados en el suelo.", "Sujeta el asa en V con ambas manos y los brazos extendidos por encima de la cabeza.", "Con la espalda recta y el tronco algo inclinado hacia atrás, tira del asa hacia la parte alta del pecho llevando los codos hacia abajo.", "Aprieta los músculos de la espalda en la parte baja del movimiento.", "Vuelve despacio hasta extender los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 356 AND name_en = 'cable lateral pulldown with v-bar' AND name_es = 'Jalón lateral en cable con barra en V' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'f15b3aec571f05d33f83d801b907f9d7';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 356 AND name_en = 'cable lateral pulldown with v-bar' AND name_es = 'Jalón al pecho en polea alta con asa en V' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '7bd36d0063b3f73728b4d71e3224ee20') THEN
    RAISE EXCEPTION 'V52: ejercicio 356 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 358 cable lying extension pullover (with rope attachment): instructions_es: R2 L10 (ALTA): El texto dice colocar la polea en la posición más alta; en el GIF el cable sale hacia abajo desde detrás de la cabeza, es decir, de una polea baja / V3 V02 ACEPTAR: f1/f7 ampliados: el cable sale hacia abajo y hacia atrás de la cabeza hasta el nivel del suelo (polea baja); f1 brazos verticales sobre el pecho, f7 brazos casi
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Engancha una cuerda a una polea baja y coloca un banco plano delante con la cabeza hacia la máquina. Túmbate boca arriba y sujeta la cuerda con ambas manos con los brazos extendidos sobre el pecho. Manteniendo los brazos casi rectos, baja la cuerda en arco hasta detrás de la cabeza con control. Vuelve a llevar la cuerda sobre el pecho. Repite el número de repeticiones indicado.', instruction_steps_es = '["Engancha una cuerda a una polea baja y coloca un banco plano delante con la cabeza hacia la máquina.", "Túmbate boca arriba y sujeta la cuerda con ambas manos con los brazos extendidos sobre el pecho.", "Manteniendo los brazos casi rectos, baja la cuerda en arco hasta detrás de la cabeza con control.", "Vuelve a llevar la cuerda sobre el pecho.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 358 AND name_en = 'cable lying extension pullover (with rope attachment)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '70b72afb758c63449d47835b43071af3';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 358 AND name_en = 'cable lying extension pullover (with rope attachment)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5647a6cb76e1da5c97c02b7ad3194963') THEN
    RAISE EXCEPTION 'V52: ejercicio 358 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 359 cable one arm bent over row: instructions_es: R2 L11 (ALTA): Dice pies separados a la altura de los hombros; el GIF muestra una postura escalonada, con un pie adelantado y el otro atrasado y la rodilla delantera flexionad / V3 W02 MEJORAR: f1 postura escalonada con la rodilla delantera flexionada, tronco inclinado y brazo extendido hacia la polea baja; f7 mano junto a la cintura con el codo pegado
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie frente a una polea baja con un pie adelantado y el otro atrasado, y flexiona la rodilla delantera. Inclina el tronco hacia delante desde las caderas con la espalda recta y sujeta el asa con una mano, con el brazo extendido. Tira del asa hacia el costado, con el codo cerca del cuerpo, hasta que la mano llegue a la cintura. Extiende el brazo con control hasta la posición inicial. Completa las repeticiones y cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie frente a una polea baja con un pie adelantado y el otro atrasado, y flexiona la rodilla delantera.", "Inclina el tronco hacia delante desde las caderas con la espalda recta y sujeta el asa con una mano, con el brazo extendido.", "Tira del asa hacia el costado, con el codo cerca del cuerpo, hasta que la mano llegue a la cintura.", "Extiende el brazo con control hasta la posición inicial.", "Completa las repeticiones y cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 359 AND name_en = 'cable one arm bent over row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e94dae88546795ed3913791f9a1a5e40';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 359 AND name_en = 'cable one arm bent over row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'f4ac2268c4a5327f0f7dc89ba762b9a1') THEN
    RAISE EXCEPTION 'V52: ejercicio 359 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 360 cable deadlift: instructions_es: R2 L12 (MEDIA): El texto coloca al usuario frente a la máquina y baja el torso hasta quedar paralelo al suelo; el GIF muestra al usuario de pie entre dos poleas, con un asa a c / V3 W02 ACEPTAR: f1 sentadilla profunda entre dos poleas bajas con un asa en cada mano a los lados y tronco poco inclinado; f4-f7 se pone de pie extendiendo rodillas y caderas;  | zona: R2 L12 (MEDIA): Se resaltan los cuádriceps. AX dudosa con poleas (carga en las manos a los costados); se mantiene / V3 W02 ACEPTAR: f1 sentadilla profunda con gran flexión de rodilla y tronco poco inclinado; f1-f7 se resaltan los cuádriceps. Se normaliza el formato propuesto (upper legs · qu | movement_pattern: R2 L12 (MEDIA): Se resaltan los cuádriceps. AX dudosa con poleas (carga en las manos a los costados); se mantiene / V3 W02 ACEPTAR: f1-f12 el movimiento es una sentadilla profunda con las asas a los lados: rodillas muy adelantadas y cadera baja (f1), extensión de rodilla dominante hasta f7; 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie entre las dos poleas bajas, con los pies separados a la anchura de los hombros. Baja flexionando rodillas y caderas con la espalda recta y sujeta un asa con cada mano a los lados del cuerpo. Ponte de pie extendiendo rodillas y caderas, con los brazos rectos y el pecho elevado. Baja con control hasta la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie entre las dos poleas bajas, con los pies separados a la anchura de los hombros.", "Baja flexionando rodillas y caderas con la espalda recta y sujeta un asa con cada mano a los lados del cuerpo.", "Ponte de pie extendiendo rodillas y caderas, con los brazos rectos y el pecho elevado.", "Baja con control hasta la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb, body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs'), target_muscle = 'quads', movement_pattern = 'KNEE_DOMINANT'
   WHERE exercise_id = 360 AND name_en = 'cable deadlift' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '89a97aba7c17880b8966f48620bed050' AND body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs') AND target_muscle = 'glutes' AND movement_pattern = 'HIP_HINGE';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 360 AND name_en = 'cable deadlift' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'bb0a5ec5d0a94f3be1fc19f3caf3f6d9' AND body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs') AND target_muscle = 'quads' AND movement_pattern = 'KNEE_DOMINANT') THEN
    RAISE EXCEPTION 'V52: ejercicio 360 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 361 cable hip adduction: instructions_es: R2 L13 (ALTA): El texto dice ponerse de pie frente a la máquina y apoyar las manos en ella; el GIF muestra al usuario de lado a la polea, con el cable llegando desde el latera / V3 W02 ACEPTAR: f1 de pie de lado a la polea, tobillera en la pierna cercana, pierna separada hacia el lado y brazo de ese lado extendido en el aire sin apoyo; f7 la pierna lle
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Sujeta la tobillera a la polea baja y al tobillo de la pierna más cercana a la máquina. Ponte de pie de lado a la polea, alejado lo suficiente para que el cable quede tenso, y extiende el brazo de ese lado para equilibrarte o apóyate en la máquina. Con la pierna recta, llévala desde el lateral hacia la línea media y por delante de la pierna de apoyo. Vuelve despacio a la posición inicial sin dejar que el peso tire de golpe. Repite el número de repeticiones indicado.', instruction_steps_es = '["Sujeta la tobillera a la polea baja y al tobillo de la pierna más cercana a la máquina.", "Ponte de pie de lado a la polea, alejado lo suficiente para que el cable quede tenso, y extiende el brazo de ese lado para equilibrarte o apóyate en la máquina.", "Con la pierna recta, llévala desde el lateral hacia la línea media y por delante de la pierna de apoyo.", "Vuelve despacio a la posición inicial sin dejar que el peso tire de golpe.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 361 AND name_en = 'cable hip adduction' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b5edf0b3e6d88d56bdec41fe2cd8a38d';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 361 AND name_en = 'cable hip adduction' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'f6ecdf5ce915988a095c54dd5a8a9dd9') THEN
    RAISE EXCEPTION 'V52: ejercicio 361 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 363 cable drag curl: instructions_es: R2 L15 (ALTA): El texto pide mantener quietos los brazos superiores; en el GIF los codos se desplazan hacia atrás y la barra sube pegada al cuerpo (curl de arrastre) / V3 W02 ACEPTAR: f1 barra corta de polea baja con brazos extendidos; f4-f7 la barra sube pegada al cuerpo hasta el pecho mientras los codos se desplazan hacia atrás: curl de arr
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie frente a una polea baja con una barra corta enganchada y sujétala con ambas manos y los brazos extendidos. Sube la barra pegada al cuerpo llevando los codos hacia atrás a medida que flexionas los brazos. Continúa hasta que la barra llegue a la altura del pecho. Baja la barra despacio por el mismo recorrido hasta extender los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie frente a una polea baja con una barra corta enganchada y sujétala con ambas manos y los brazos extendidos.", "Sube la barra pegada al cuerpo llevando los codos hacia atrás a medida que flexionas los brazos.", "Continúa hasta que la barra llegue a la altura del pecho.", "Baja la barra despacio por el mismo recorrido hasta extender los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 363 AND name_en = 'cable drag curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '58062f84ec70ea93ab5fc7b217a580d5';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 363 AND name_en = 'cable drag curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b61b415d117c6895d0719e4e64b0a50c') THEN
    RAISE EXCEPTION 'V52: ejercicio 363 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 364 cable hammer curl (with rope): instructions_es: R2 L16 (ALTA): El texto dice agarre supino y palmas mirándose entre sí a la vez, y subir la cuerda a la altura de los hombros; el GIF muestra agarre neutro (martillo) y la cue / V3 W02 ACEPTAR: f1 cuerda de polea baja sujeta con agarre neutro y brazos extendidos; f7 cuerda a la altura del pecho alto con los codos fijos junto al cuerpo; el texto actual 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca la cuerda en una polea baja y ponte de pie frente a ella con los pies separados a la altura de los hombros. Sujeta los extremos de la cuerda con agarre neutro, palmas enfrentadas, y los brazos extendidos. Flexiona los codos sin mover la parte superior de los brazos y sube la cuerda hacia el pecho. Baja con control hasta extender los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca la cuerda en una polea baja y ponte de pie frente a ella con los pies separados a la altura de los hombros.", "Sujeta los extremos de la cuerda con agarre neutro, palmas enfrentadas, y los brazos extendidos.", "Flexiona los codos sin mover la parte superior de los brazos y sube la cuerda hacia el pecho.", "Baja con control hasta extender los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 364 AND name_en = 'cable hammer curl (with rope)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'de80b48a0e664c07f18cd3cfa032ea6a';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 364 AND name_en = 'cable hammer curl (with rope)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '508be1e5edad61eea8f97640ee38c637') THEN
    RAISE EXCEPTION 'V52: ejercicio 364 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 367 cable kickback: instructions_es: R2 L19 (MEDIA): El texto dice mantener el brazo superior pegado al cuerpo con el codo a 90 grados y extender solo el antebrazo; en el GIF el brazo superior se desplaza desde co / V3 Y01 MEJORAR: f1 postura escalonada frente a polea baja, brazo colgando con la mano delante del muslo; f4-f7 el brazo, casi estirado en todo el recorrido, va hacia atrás hast
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie frente a una polea baja, sujeta el asa con una mano y adelanta un pie para quedar en postura escalonada. Flexiona ligeramente las rodillas e inclina el tronco hacia delante desde la cadera, con la espalda recta. Desde la mano junto al muslo, lleva el brazo casi estirado hacia atrás hasta que quede en línea con el tronco. Vuelve con control a la posición inicial. Completa las repeticiones y cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie frente a una polea baja, sujeta el asa con una mano y adelanta un pie para quedar en postura escalonada.", "Flexiona ligeramente las rodillas e inclina el tronco hacia delante desde la cadera, con la espalda recta.", "Desde la mano junto al muslo, lleva el brazo casi estirado hacia atrás hasta que quede en línea con el tronco.", "Vuelve con control a la posición inicial.", "Completa las repeticiones y cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 367 AND name_en = 'cable kickback' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '9ef5fc66803c649527ee82665bc42408';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 367 AND name_en = 'cable kickback' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e7e84f2486a4ca428f7e3a54923aad42') THEN
    RAISE EXCEPTION 'V52: ejercicio 367 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 368 cable kneeling triceps extension: requires_overhead: R2 L20 (ALTA): Los brazos quedan en flexión completa del hombro (en la línea del cuerpo, más allá de la cabeza) durante todo el ejercicio: falta OH. / V3 W02 ACEPTAR: f1-f12 tronco horizontal con la parte superior de los brazos apoyada en el banco, en flexión completa del hombro en la línea del cuerpo más allá de la cabeza; f | instructions_es: R2 L20 (ALTA): El texto dice ponerse de rodillas frente a la máquina y no menciona el banco; en el GIF la polea queda detrás de la persona, que apoya los brazos en un banco co / V3 W02 ACEPTAR: f1 arrodillado en el suelo de espaldas a la polea alta (el cable sube hacia atrás), banco plano delante con los brazos apoyados y la cuerda detrás de la cabeza;
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = TRUE, instructions_es = 'Engancha una cuerda a una polea alta y arrodíllate en el suelo de espaldas a la máquina, con un banco plano delante. Inclina el tronco hasta la horizontal y apoya la parte superior de los brazos en el banco, con la cuerda detrás de la cabeza. Sin despegar los brazos del banco, extiende los codos hacia delante contrayendo los tríceps. Flexiona los codos despacio para volver a la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Engancha una cuerda a una polea alta y arrodíllate en el suelo de espaldas a la máquina, con un banco plano delante.", "Inclina el tronco hasta la horizontal y apoya la parte superior de los brazos en el banco, con la cuerda detrás de la cabeza.", "Sin despegar los brazos del banco, extiende los codos hacia delante contrayendo los tríceps.", "Flexiona los codos despacio para volver a la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 368 AND name_en = 'cable kneeling triceps extension' AND requires_overhead = FALSE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a5c20c5116996c18ee686d95c388f3e4';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 368 AND name_en = 'cable kneeling triceps extension' AND requires_overhead = TRUE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '94e545ba22a5a482d3be338daa8072a4') THEN
    RAISE EXCEPTION 'V52: ejercicio 368 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 370 cable lying close-grip curl: instructions_es: R2 L02 (ALTA): El texto habla de polea baja, codos pegados a los costados y curl hacia el pecho; el GIF muestra el cable desde una polea alta, los brazos apuntando al techo y  / V3 V02 ACEPTAR: f1/f7 ampliados: cable vertical desde una polea alta; f1 brazos apuntando al techo, f7 codos flexionados y barra junto a la frente con brazos verticales. El tex
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en un banco plano colocado bajo una polea alta, con los pies apoyados en el suelo. Sujeta la barra del cable con agarre cerrado y estira los brazos hacia el techo. Flexiona los codos y lleva la barra hacia la frente sin mover los brazos superiores. Vuelve despacio a estirar los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en un banco plano colocado bajo una polea alta, con los pies apoyados en el suelo.", "Sujeta la barra del cable con agarre cerrado y estira los brazos hacia el techo.", "Flexiona los codos y lleva la barra hacia la frente sin mover los brazos superiores.", "Vuelve despacio a estirar los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 370 AND name_en = 'cable lying close-grip curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '1846ddc6cdebffc6d16ccf6cffef1376';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 370 AND name_en = 'cable lying close-grip curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b0b9b0e2376c20a6b14fd754f4337899') THEN
    RAISE EXCEPTION 'V52: ejercicio 370 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 374 cable one arm lateral raise: instructions_es: R2 L06 (ALTA): El texto pide ponerse frente a la máquina; en el GIF la polea está al lado contrario del brazo que trabaja y el cable cruza por delante del cuerpo. / V3 V02 ACEPTAR: f1 agarre delante de la cadera con el cable cruzando por delante del cuerpo desde abajo del lado contrario; f7 brazo de trabajo a la altura del hombro hacia el 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie de lado a una polea baja, con los pies separados a la anchura de los hombros. Sujeta el agarre con la mano más alejada de la polea, delante de la cadera, de modo que el cable cruce por delante del cuerpo. Eleva el brazo casi recto hacia el lado hasta la altura del hombro. Bájalo despacio a la posición inicial. Completa las repeticiones y cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie de lado a una polea baja, con los pies separados a la anchura de los hombros.", "Sujeta el agarre con la mano más alejada de la polea, delante de la cadera, de modo que el cable cruce por delante del cuerpo.", "Eleva el brazo casi recto hacia el lado hasta la altura del hombro.", "Bájalo despacio a la posición inicial.", "Completa las repeticiones y cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 374 AND name_en = 'cable one arm lateral raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '48d58e9fe6b0a71096b41d8dfee9f0e4';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 374 AND name_en = 'cable one arm lateral raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '042812eab36b8b878a0b844dcf09b250') THEN
    RAISE EXCEPTION 'V52: ejercicio 374 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 380 cable one arm incline press: requires_overhead: R2 L12 (MEDIA): En f7 el brazo queda vertical en banco inclinado (unos 130 grados de flexión respecto al tronco) con la mano por encima de la cabeza; mismo criterio que otros e / V3 W02 ACEPTAR: f1 banco inclinado pronunciado con el asa junto al hombro; f7 brazo vertical con la mano y el antebrazo claramente por encima del nivel de la cabeza (unos 150 g
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = TRUE
   WHERE exercise_id = 380 AND name_en = 'cable one arm incline press' AND requires_overhead = FALSE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 380 AND name_en = 'cable one arm incline press' AND requires_overhead = TRUE) THEN
    RAISE EXCEPTION 'V52: ejercicio 380 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 391 cable pushdown (straight arm) v. 2: instructions_es: R2 L03 (ALTA): El texto indica una barra recta con agarre prono y palmas hacia abajo; el GIF muestra una cuerda con dos extremos sujetos con agarre neutro. También pide rodill / V3 V02 ACEPTAR: f1/f7 ampliados: cuerda con dos extremos (bolas visibles), no barra recta; f1 brazos estirados al frente a la altura de los hombros, f7 manos junto a los muslos
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Engancha una cuerda a una polea alta y ponte de pie frente a ella con los pies separados al ancho de los hombros. Sujeta un extremo de la cuerda con cada mano, con los brazos estirados al frente a la altura de los hombros. Activa el abdomen y, sin doblar los codos, baja los brazos hasta llevar las manos junto a los muslos. Vuelve despacio a la posición inicial manteniendo los brazos rectos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Engancha una cuerda a una polea alta y ponte de pie frente a ella con los pies separados al ancho de los hombros.", "Sujeta un extremo de la cuerda con cada mano, con los brazos estirados al frente a la altura de los hombros.", "Activa el abdomen y, sin doblar los codos, baja los brazos hasta llevar las manos junto a los muslos.", "Vuelve despacio a la posición inicial manteniendo los brazos rectos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 391 AND name_en = 'cable pushdown (straight arm) v. 2' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e3b02f8d206aab6c49aadcea54599607';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 391 AND name_en = 'cable pushdown (straight arm) v. 2' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '41f855a5a76011b52cc087e08d03bdae') THEN
    RAISE EXCEPTION 'V52: ejercicio 391 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 393 cable reverse-grip straight back seated high row: name_es: R2 L05 (ALTA): Remo alto suele entenderse como otro ejercicio; el GIF es un jalón. Hace falta un asiento (banco) / V3 V02 ACEPTAR: f1 sentado en banco con brazos extendidos por encima de la cabeza sujetando la barra; f7 barra en la parte alta del pecho con tronco erguido: es un jalón vertic | instructions_es: R2 L05 (ALTA): El texto describe un remo con el tronco inclinado hacia delante tirando hacia el torso; el GIF muestra un jalón desde los brazos extendidos por encima de la cab / V3 V02 ACEPTAR: f1 brazos extendidos por encima de la cabeza, f7 barra en el pecho alto, tronco erguido. El texto actual describe un remo con inclinación hacia delante y tirón 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Jalón con agarre supino sentado en polea alta', instructions_es = 'Siéntate en un banco frente a una polea alta con los pies apoyados en el suelo. Sujeta la barra con agarre supino, con las manos a la anchura de los hombros y los brazos extendidos por encima de la cabeza. Con el tronco erguido, tira de la barra hacia la parte alta del pecho juntando los omóplatos. Extiende los brazos despacio hasta la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en un banco frente a una polea alta con los pies apoyados en el suelo.", "Sujeta la barra con agarre supino, con las manos a la anchura de los hombros y los brazos extendidos por encima de la cabeza.", "Con el tronco erguido, tira de la barra hacia la parte alta del pecho juntando los omóplatos.", "Extiende los brazos despacio hasta la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 393 AND name_en = 'cable reverse-grip straight back seated high row' AND name_es = 'Remo alto sentado con agarre inverso en polea' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e46f07f7c5fdae52aca0b9be43fcbbad';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 393 AND name_en = 'cable reverse-grip straight back seated high row' AND name_es = 'Jalón con agarre supino sentado en polea alta' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'fd0e45947cf33a71f727ef5661cab8b6') THEN
    RAISE EXCEPTION 'V52: ejercicio 393 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 394 cable rope crossover seated row: instructions_es: R2 L06 (MEDIA): El texto pide inclinarse ligeramente hacia atrás antes de tirar y mezcla agarre prono con palmas enfrentadas; en el GIF el tirón empieza con el tronco inclinado / V3 V02 ACEPTAR: f1/f7 ampliados: cuerda con extremos de bola; f1 tronco inclinado hacia delante con brazos extendidos, f7 tronco erguido con la cuerda en el abdomen y codos atr
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en la máquina de remo con los pies en los apoyos y las rodillas algo flexionadas. Sujeta la cuerda con ambas manos, con los brazos extendidos y el tronco inclinado hacia delante. Tira de la cuerda hacia el abdomen llevando los codos hacia atrás mientras enderezas el tronco y juntas los omóplatos. Vuelve con control a la posición inicial extendiendo los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en la máquina de remo con los pies en los apoyos y las rodillas algo flexionadas.", "Sujeta la cuerda con ambas manos, con los brazos extendidos y el tronco inclinado hacia delante.", "Tira de la cuerda hacia el abdomen llevando los codos hacia atrás mientras enderezas el tronco y juntas los omóplatos.", "Vuelve con control a la posición inicial extendiendo los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 394 AND name_en = 'cable rope crossover seated row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '77a4b57b5a34ea8b4fc9d306c4410ee4';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 394 AND name_en = 'cable rope crossover seated row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'c6a455428290b9728b2cf1bbef0acb93') THEN
    RAISE EXCEPTION 'V52: ejercicio 394 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 399 cable one arm tricep pushdown: instructions_es: R2 L11 (ALTA): Habla de una barra recta sujeta con agarre prono y retroceder un paso, y se contradice («comienza con el brazo completamente extendido» y luego empuja «hasta qu / V3 W02 MEJORAR: f1 asa de una mano con el codo flexionado y la mano a la altura de la barbilla; f7 brazo extendido con la mano junto al muslo. El cambio es necesario (barra rec
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie frente a una polea alta con un asa de una mano y los pies separados a la anchura de las caderas. Sujeta el asa con una mano, con el codo totalmente flexionado, la mano cerca de la barbilla y el brazo cerca del costado. Sin mover la parte superior del brazo, extiende el codo empujando el asa hacia abajo hasta que el brazo quede estirado junto al muslo. Vuelve con control a la posición inicial; mantén la espalda recta y el core activado. Completa las repeticiones y cambia de brazo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie frente a una polea alta con un asa de una mano y los pies separados a la anchura de las caderas.", "Sujeta el asa con una mano, con el codo totalmente flexionado, la mano cerca de la barbilla y el brazo cerca del costado.", "Sin mover la parte superior del brazo, extiende el codo empujando el asa hacia abajo hasta que el brazo quede estirado junto al muslo.", "Vuelve con control a la posición inicial; mantén la espalda recta y el core activado.", "Completa las repeticiones y cambia de brazo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 399 AND name_en = 'cable one arm tricep pushdown' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '83a2f66f027389d431652c100bbdddde';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 399 AND name_en = 'cable one arm tricep pushdown' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '113778dc073edf6358f1db41502d6859') THEN
    RAISE EXCEPTION 'V52: ejercicio 399 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 402 cable overhead triceps extension (rope attachment): instructions_es: R2 L14 (ALTA): El texto pide fijar la cuerda en una polea alta; en el GIF el cable baja desde las manos hasta una polea baja detrás del cuerpo / V3 W02 ACEPTAR: f1 brazos extendidos sobre la cabeza con la cuerda; el cable baja desde las manos por detrás del cuerpo hasta una polea baja; f7 codos flexionados con la cuerda
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Engancha una cuerda a una polea baja y ponte de pie de espaldas a la máquina, con el cable pasando por detrás de tu cuerpo. Sujeta la cuerda con ambas manos y estira los brazos por encima de la cabeza. Con la parte superior de los brazos cerca de la cabeza, flexiona los codos y baja la cuerda por detrás de la cabeza. Extiende los codos para volver a estirar los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Engancha una cuerda a una polea baja y ponte de pie de espaldas a la máquina, con el cable pasando por detrás de tu cuerpo.", "Sujeta la cuerda con ambas manos y estira los brazos por encima de la cabeza.", "Con la parte superior de los brazos cerca de la cabeza, flexiona los codos y baja la cuerda por detrás de la cabeza.", "Extiende los codos para volver a estirar los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 402 AND name_en = 'cable overhead triceps extension (rope attachment)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '44b1d854bad6562af0c00370d04b9d31';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 402 AND name_en = 'cable overhead triceps extension (rope attachment)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'acd165442072479e2d452f2d3e3c9291') THEN
    RAISE EXCEPTION 'V52: ejercicio 402 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 403 cable preacher curl: instructions_es: R2 L15 (ALTA): El texto pide mantener los codos pegados a los costados; en el GIF los brazos están apoyados por delante sobre la almohadilla / V3 W02 ACEPTAR: f1 sentado en el banco Scott con la parte posterior de los brazos apoyada por delante sobre la almohadilla y barra de polea baja; f7 barra junto a los hombros s
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en el banco Scott frente a una polea baja y apoya la parte posterior de los brazos sobre la almohadilla. Sujeta la barra del cable con ambas manos y los brazos casi extendidos. Flexiona los codos y sube la barra hacia los hombros sin despegar los brazos de la almohadilla. Baja la barra despacio hasta extender los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en el banco Scott frente a una polea baja y apoya la parte posterior de los brazos sobre la almohadilla.", "Sujeta la barra del cable con ambas manos y los brazos casi extendidos.", "Flexiona los codos y sube la barra hacia los hombros sin despegar los brazos de la almohadilla.", "Baja la barra despacio hasta extender los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 403 AND name_en = 'cable preacher curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'db0043cd2103fd1d4d39172f5324845e';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 403 AND name_en = 'cable preacher curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'bbc9228095149719d6e9a407370b68d3') THEN
    RAISE EXCEPTION 'V52: ejercicio 403 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 404 cable pulldown bicep curl: movement_pattern: R2 L16 (MEDIA): La parte superior del brazo se mantiene elevada y casi fija; el movimiento principal es la flexión de codo (bíceps resaltado), no un jalón vertical. / V3 W02 ACEPTAR: f1 brazos extendidos por encima con la barra; f7 barra sobre la cabeza con los codos muy flexionados y la parte superior del brazo aún elevada; solo se resalta 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET movement_pattern = 'ELBOW_FLEXION'
   WHERE exercise_id = 404 AND name_en = 'cable pulldown bicep curl' AND movement_pattern = 'VERTICAL_PULL';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 404 AND name_en = 'cable pulldown bicep curl' AND movement_pattern = 'ELBOW_FLEXION') THEN
    RAISE EXCEPTION 'V52: ejercicio 404 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 410 cable reverse one arm curl: instructions_es: R2 L02 (MEDIA): El texto dice agarre supino con la palma hacia abajo, que es contradictorio (supino es palma arriba; un curl invertido es prono); en el GIF la mano que trabaja  / V3 V02 ACEPTAR: f1/f7 ampliados: un brazo con polea baja delante, codo junto al costado, mano sube al hombro; la mano libre queda en la espalda. Orientación de la palma no dist
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie frente a una polea baja con los pies separados a la anchura de los hombros. Sujeta el agarre del cable con una mano, con el brazo extendido junto al muslo, y lleva la otra mano a la espalda. Mantén el codo pegado al costado y flexiónalo hasta llevar la mano a la altura del hombro. Baja despacio hasta extender el brazo. Completa las repeticiones y cambia de brazo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie frente a una polea baja con los pies separados a la anchura de los hombros.", "Sujeta el agarre del cable con una mano, con el brazo extendido junto al muslo, y lleva la otra mano a la espalda.", "Mantén el codo pegado al costado y flexiónalo hasta llevar la mano a la altura del hombro.", "Baja despacio hasta extender el brazo.", "Completa las repeticiones y cambia de brazo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 410 AND name_en = 'cable reverse one arm curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '6ae558160ea99d84f42451302877f761';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 410 AND name_en = 'cable reverse one arm curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '22959853510c57244d3a32dd46129f36') THEN
    RAISE EXCEPTION 'V52: ejercicio 410 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 411 cable reverse preacher curl: instructions_es: R2 L03 (ALTA): El texto dice «Agarra las agarraderas del cable con un agarre supino» (y a la vez palmas hacia abajo); el GIF muestra una barra corta de cable sujeta con agarre / V3 V02 ACEPTAR: f1/f7 ampliados: barra de cable desde polea baja; en f7 los dorsos de las manos quedan arriba (agarre prono) con los brazos sobre el cojín y codos casi extendid
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca el banco Scott frente a una polea baja con una barra corta enganchada. Siéntate y apoya la parte posterior de los brazos sobre el cojín. Sujeta la barra con agarre prono (palmas hacia abajo), separada al ancho de los hombros, con los codos casi extendidos. Sin despegar los brazos del cojín, flexiona los codos y sube la barra hacia la barbilla. Baja la barra despacio hasta casi extender los codos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca el banco Scott frente a una polea baja con una barra corta enganchada.", "Siéntate y apoya la parte posterior de los brazos sobre el cojín.", "Sujeta la barra con agarre prono (palmas hacia abajo), separada al ancho de los hombros, con los codos casi extendidos.", "Sin despegar los brazos del cojín, flexiona los codos y sube la barra hacia la barbilla.", "Baja la barra despacio hasta casi extender los codos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 411 AND name_en = 'cable reverse preacher curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a0cae1928dd54caaf5dc022e83d4f70e';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 411 AND name_en = 'cable reverse preacher curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '8be9d4ee1b9a6c7b25cf4882d2b3206b') THEN
    RAISE EXCEPTION 'V52: ejercicio 411 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 413 cable standing back wrist curl: instructions_es: R2 L05 (MEDIA): El texto dice ponerse frente a la máquina; el cable sale hacia atrás y abajo, así que la polea queda detrás del usuario. La orientación de la palma no se distin / V3 V03 ACEPTAR: f1-f12 vista 3/4 con la cara hacia la derecha; el cable sale del agarre hacia atrás y abajo, por detrás del cuerpo (polea baja detrás); solo se flexiona la muñe
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie de espaldas a una polea baja, con los pies separados a la anchura de las caderas. Sujeta el agarre de la polea con el brazo extendido junto al costado. Sin mover el brazo, flexiona la muñeca para subir el agarre. Bájalo despacio hasta la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie de espaldas a una polea baja, con los pies separados a la anchura de las caderas.", "Sujeta el agarre de la polea con el brazo extendido junto al costado.", "Sin mover el brazo, flexiona la muñeca para subir el agarre.", "Bájalo despacio hasta la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 413 AND name_en = 'cable standing back wrist curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '74a400194f02055f450f58cc2930fc58';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 413 AND name_en = 'cable standing back wrist curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '7f9cf1a2382c4fe339a1069df0410ed9') THEN
    RAISE EXCEPTION 'V52: ejercicio 413 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 414 cable standing calf raise: name_es: R2 L06 (MEDIA): El enganche exacto del cable (cinturón u otro accesorio) no se distingue a esta resolución; no se afirma en la propuesta. Nombre sin tilde en «Elevación». / V3 V03 ACEPTAR: Corrección ortográfica: falta la tilde en Elevación; f1-f12 elevación de talones de pie en máquina de polea, el nombre describe el GIF | instructions_es: R2 L06 (MEDIA): El texto propone una correa de tobillo, que no tiene sentido para una elevación de talones, y habla de levantar los talones del suelo; el GIF muestra al usuario / V3 V03 ACEPTAR: f1-f12 de pie sobre una plataforma de frente a la máquina, manos agarradas al bastidor, cable de la polea baja a la cadera; f1 talones abajo, f5 talones arriba 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Elevación de talones de pie con cable', instructions_es = 'Ponte de pie sobre la plataforma de una máquina de polea baja, de frente a la máquina, con el cable enganchado a la altura de la cadera. Agárrate a la máquina con las manos para mantener el equilibrio. Eleva los talones lo más alto que puedas, quedando sobre la parte delantera de los pies. Baja los talones despacio a la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie sobre la plataforma de una máquina de polea baja, de frente a la máquina, con el cable enganchado a la altura de la cadera.", "Agárrate a la máquina con las manos para mantener el equilibrio.", "Eleva los talones lo más alto que puedas, quedando sobre la parte delantera de los pies.", "Baja los talones despacio a la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 414 AND name_en = 'cable standing calf raise' AND name_es = 'Elevacion de talones de pie con cable' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'f4981b74a10575f5d2d35006d786cfb5';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 414 AND name_en = 'cable standing calf raise' AND name_es = 'Elevación de talones de pie con cable' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'bf023ab9d35c363ccf66680058c127b8') THEN
    RAISE EXCEPTION 'V52: ejercicio 414 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 418 cable shoulder press: instructions_es: R2 L10 (MEDIA): El texto dice ajustar la máquina para que las agarraderas queden a la altura del hombro; en el GIF los cables salen de poleas bajas situadas detrás / V3 V03 ACEPTAR: f1 asas a la altura de los hombros con los cables bajando junto al cuerpo hasta poleas bajas detrás; f7 brazos extendidos sobre la cabeza; el texto actual sitúa
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca las poleas en la posición baja y ponte de pie de espaldas a la máquina, entre los cables, con los pies separados a la anchura de los hombros. Sujeta un asa en cada mano y llévalas a la altura de los hombros con los codos flexionados. Empuja las asas hacia arriba hasta extender los brazos por encima de la cabeza, con el core activo. Baja con control hasta los hombros. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca las poleas en la posición baja y ponte de pie de espaldas a la máquina, entre los cables, con los pies separados a la anchura de los hombros.", "Sujeta un asa en cada mano y llévalas a la altura de los hombros con los codos flexionados.", "Empuja las asas hacia arriba hasta extender los brazos por encima de la cabeza, con el core activo.", "Baja con control hasta los hombros.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 418 AND name_en = 'cable shoulder press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '95dcb8a287ab98dce247471992e046ab';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 418 AND name_en = 'cable shoulder press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '53dda799bf5e73daa95310d67a3a1c0e') THEN
    RAISE EXCEPTION 'V52: ejercicio 418 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 422 cable standing fly: instructions_es: R2 L14 (ALTA): El texto coloca los cables a la altura del pecho, de espaldas a la máquina, y junta las manos delante del pecho; en el GIF las poleas son altas, la persona está / V3 W02 ACEPTAR: f1 brazos abiertos hacia arriba y a los lados con cables desde poleas altas, de pie en el centro; f7 manos juntas delante de la cadera; no está de espaldas a la
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca las agarraderas en las poleas altas y sitúate de pie en el centro, entre ambas. Sujeta una agarradera con cada mano, con los brazos abiertos hacia arriba y hacia los lados y los codos ligeramente flexionados. Lleva los brazos hacia abajo y hacia delante en arco hasta juntar las manos delante de la cadera. Vuelve despacio a la posición inicial con los brazos abiertos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca las agarraderas en las poleas altas y sitúate de pie en el centro, entre ambas.", "Sujeta una agarradera con cada mano, con los brazos abiertos hacia arriba y hacia los lados y los codos ligeramente flexionados.", "Lleva los brazos hacia abajo y hacia delante en arco hasta juntar las manos delante de la cadera.", "Vuelve despacio a la posición inicial con los brazos abiertos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 422 AND name_en = 'cable standing fly' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'f9e72d8b39cf7ecd2983ed8fd8355638';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 422 AND name_en = 'cable standing fly' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ccc53d7cb248dd8e28498309adb5ece2') THEN
    RAISE EXCEPTION 'V52: ejercicio 422 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 425 cable seated twist: instructions_es: R2 L17 (MEDIA): El texto pide repetir del otro lado alternando lados en cada repetición y rodillas ligeramente flexionadas; el GIF muestra solo un lado (con la polea fija a un  / V3 W02 ACEPTAR: f1 sentado en banco de lado a la polea, brazos extendidos hacia ella con el cable horizontal a la altura del pecho, rodillas a 90 grados; f7 tronco girado aleja
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en un banco de lado a una polea situada a la altura del pecho, con los pies apoyados en el suelo. Sujeta el agarre con ambas manos y extiende los brazos al frente. Con el core activado, gira el tronco alejando el agarre de la polea, sin mover la cadera. Vuelve girando con control hacia la polea. Completa las repeticiones y cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en un banco de lado a una polea situada a la altura del pecho, con los pies apoyados en el suelo.", "Sujeta el agarre con ambas manos y extiende los brazos al frente.", "Con el core activado, gira el tronco alejando el agarre de la polea, sin mover la cadera.", "Vuelve girando con control hacia la polea.", "Completa las repeticiones y cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 425 AND name_en = 'cable seated twist' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '65ca5e3c65367492358c46ca2960f216';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 425 AND name_en = 'cable seated twist' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'f49805b769852330ceb91fb2cf96079f') THEN
    RAISE EXCEPTION 'V52: ejercicio 425 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 426 cable side bend: instructions_es: R2 L18 (ALTA): El texto indica repetir del otro lado alternando en cada repetición; el GIF trabaja siempre el mismo lado (agarradera en la misma mano y la otra en la cadera),  / V3 W02 ACEPTAR: f1 de pie de lado a la polea baja, agarradera en la mano cercana y la otra mano en la cadera, inclinado hacia la polea; f7 inclinado hacia el lado contrario ele
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie de lado a una polea baja, con los pies a la anchura de los hombros, y sujeta la agarradera con la mano más cercana. Apoya la otra mano en la cadera y mantén la espalda recta y el abdomen activado. Inclina el tronco hacia la polea bajando la agarradera junto a la pierna. Inclínate hacia el lado contrario elevando la agarradera, sin girar el tronco. Completa las repeticiones y cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie de lado a una polea baja, con los pies a la anchura de los hombros, y sujeta la agarradera con la mano más cercana.", "Apoya la otra mano en la cadera y mantén la espalda recta y el abdomen activado.", "Inclina el tronco hacia la polea bajando la agarradera junto a la pierna.", "Inclínate hacia el lado contrario elevando la agarradera, sin girar el tronco.", "Completa las repeticiones y cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 426 AND name_en = 'cable side bend' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a60c534720e6ee906f6fcaa71f22ea27';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 426 AND name_en = 'cable side bend' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd31e2411ddd25ab75da401767fb4c312') THEN
    RAISE EXCEPTION 'V52: ejercicio 426 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 429 cable standing crunch: instructions_es: R2 L01 (MEDIA): El texto dice ponerse de espaldas a la máquina con el mango detrás de la cabeza; el GIF muestra a la persona de frente a la polea con una barra sujeta delante d / V3 V03 ACEPTAR: f1 de pie de frente a la polea alta con una barra sujeta delante de la frente; f7 tronco flexionado hacia delante con la barra a la altura de la cadera; el text
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca una barra en una polea alta y ponte de pie de frente a la máquina, con los pies separados a la anchura de las caderas y las rodillas ligeramente flexionadas. Sujeta la barra con ambas manos delante de la frente, con los codos flexionados. Activa el abdomen y flexiona el tronco hacia delante llevando la barra hacia abajo hasta la altura de la cadera. Vuelve despacio a la posición erguida controlando el cable. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca una barra en una polea alta y ponte de pie de frente a la máquina, con los pies separados a la anchura de las caderas y las rodillas ligeramente flexionadas.", "Sujeta la barra con ambas manos delante de la frente, con los codos flexionados.", "Activa el abdomen y flexiona el tronco hacia delante llevando la barra hacia abajo hasta la altura de la cadera.", "Vuelve despacio a la posición erguida controlando el cable.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 429 AND name_en = 'cable standing crunch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '95b2c60013ecfd8d20b74321eaad2b2a';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 429 AND name_en = 'cable standing crunch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e4aa9a3546911f40fcd95161651c8dce') THEN
    RAISE EXCEPTION 'V52: ejercicio 429 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 430 cable standing crunch (with rope attachment): instructions_es: R2 L02 (ALTA): El texto coloca la cuerda a la altura del pecho y la pausa en la parte más alta; el GIF muestra la polea en lo alto de la máquina y la flexión termina abajo, co / V3 V03 ACEPTAR: f1 de espaldas a la máquina con la cuerda junto a la cabeza y el cable desde la polea situada en lo alto del bastidor; f7 tronco flexionado con la cabeza hacia 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca una cuerda en una polea alta. Ponte de pie de espaldas a la máquina con los pies separados a la anchura de los hombros y sujeta la cuerda con ambas manos junto a la cabeza. Activa el abdomen y flexiona el tronco hacia delante y abajo, llevando la cabeza hacia las rodillas. Haz una breve pausa abajo y vuelve despacio a la posición erguida. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca una cuerda en una polea alta.", "Ponte de pie de espaldas a la máquina con los pies separados a la anchura de los hombros y sujeta la cuerda con ambas manos junto a la cabeza.", "Activa el abdomen y flexiona el tronco hacia delante y abajo, llevando la cabeza hacia las rodillas.", "Haz una breve pausa abajo y vuelve despacio a la posición erguida.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 430 AND name_en = 'cable standing crunch (with rope attachment)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b0cfdca46f838fac30edbc92fa66d509';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 430 AND name_en = 'cable standing crunch (with rope attachment)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '858735fb77eb03659ad242e66fb6c4e4') THEN
    RAISE EXCEPTION 'V52: ejercicio 430 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 431 cable standing lift: instructions_es: R2 L03 (ALTA): El texto pide colocarse frente a la máquina con el asa a la altura de la cintura y llevarla hacia el hombro opuesto; el GIF parte de lado a una polea baja con l / V3 V03 ACEPTAR: f1 de pie de lado a una polea baja, tronco girado hacia ella y manos a la altura de la cadera con brazos casi rectos; f7 manos por encima de la cabeza en el lad
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie de lado a una polea baja, con los pies separados al ancho de los hombros. Sujeta el asa con ambas manos junto a la cadera más cercana a la polea, con los brazos casi rectos. Activa el abdomen y lleva el asa en diagonal cruzando el cuerpo hasta por encima de la cabeza en el lado contrario, girando el tronco. Vuelve despacio por el mismo recorrido hasta la cadera. Cambia de lado al terminar la serie. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie de lado a una polea baja, con los pies separados al ancho de los hombros.", "Sujeta el asa con ambas manos junto a la cadera más cercana a la polea, con los brazos casi rectos.", "Activa el abdomen y lleva el asa en diagonal cruzando el cuerpo hasta por encima de la cabeza en el lado contrario, girando el tronco.", "Vuelve despacio por el mismo recorrido hasta la cadera.", "Cambia de lado al terminar la serie.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 431 AND name_en = 'cable standing lift' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '4071fd6d1dcf7eade142fd0887cf6391';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 431 AND name_en = 'cable standing lift' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'cb0f8e6645b359b7a1914e55bb5b1a94') THEN
    RAISE EXCEPTION 'V52: ejercicio 431 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 434 cable seated high row (v-bar): instructions_es: R2 L06 (MEDIA): El texto pide inclinarse hacia delante desde las caderas y mezcla agarre prono con palmas enfrentadas; en el GIF el tronco se inclina hacia atrás durante el tir / V3 V03 ACEPTAR: f1 sentado en banco frente a polea alta, brazos extendidos sobre la cabeza con agarre en V, pies en el suelo; f7 agarre en la parte alta del pecho con el tronco
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en un banco frente a una polea alta, con los pies apoyados en el suelo. Sujeta el agarre en V con ambas manos y los brazos extendidos por encima de la cabeza. Tira del agarre hacia la parte alta del pecho, llevando los codos hacia abajo mientras inclinas ligeramente el tronco hacia atrás. Vuelve con control a la posición inicial estirando los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en un banco frente a una polea alta, con los pies apoyados en el suelo.", "Sujeta el agarre en V con ambas manos y los brazos extendidos por encima de la cabeza.", "Tira del agarre hacia la parte alta del pecho, llevando los codos hacia abajo mientras inclinas ligeramente el tronco hacia atrás.", "Vuelve con control a la posición inicial estirando los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 434 AND name_en = 'cable seated high row (v-bar)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5574d8d00cc71505c010c0c3eea6c5e0';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 434 AND name_en = 'cable seated high row (v-bar)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '6b5710c0f8ed7c74b610e0cf7e0a26ea') THEN
    RAISE EXCEPTION 'V52: ejercicio 434 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 438 cable shrug: instructions_es: R2 L10 (ALTA): El texto habla de agarraderas del cable; el GIF muestra una barra recta enganchada a una polea baja / V3 V03 MEJORAR: f1-f12 barra con el cable enganchado en el centro, sujeta delante de los muslos con agarre prono, no agarraderas; f7 hombros elevados; la barra se ve algo curva
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Engancha una barra a una polea baja y ponte de pie frente a la máquina con los pies separados a la anchura de los hombros. Sujeta la barra con agarre prono y deja los brazos extendidos delante de los muslos. Sin flexionar los codos, eleva los hombros hacia las orejas. Mantén un momento arriba y baja los hombros con control. Repite el número de repeticiones indicado.', instruction_steps_es = '["Engancha una barra a una polea baja y ponte de pie frente a la máquina con los pies separados a la anchura de los hombros.", "Sujeta la barra con agarre prono y deja los brazos extendidos delante de los muslos.", "Sin flexionar los codos, eleva los hombros hacia las orejas.", "Mantén un momento arriba y baja los hombros con control.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 438 AND name_en = 'cable shrug' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '23655a284d2ee760167cc9d6ce82339d';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 438 AND name_en = 'cable shrug' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '080a18a64b703f0ab67dfc7757792620') THEN
    RAISE EXCEPTION 'V52: ejercicio 438 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 439 cable squat row (with rope attachment): instructions_es: R2 L11 (ALTA): Describe tirar de la cuerda mientras se está en sentadilla y llevar las manos al pecho; el GIF muestra que el tirón se hace a la vez que se sube de la sentadill / V3 W02 ACEPTAR: f1 sentadilla profunda con los brazos extendidos hacia la polea a media altura; f4-f7 se pone de pie a la vez que tira de la cuerda hasta el abdomen; el tirón n
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Sujeta una cuerda a una polea a media altura y ponte de frente a la máquina con los pies separados a la anchura de los hombros. Agarra la cuerda y baja a una sentadilla profunda con los brazos extendidos, la espalda recta y el pecho elevado. Ponte de pie empujando con las piernas y, al mismo tiempo, tira de la cuerda hacia el abdomen juntando los omóplatos, con los codos cerca del cuerpo. Vuelve a bajar a la sentadilla extendiendo los brazos con control. Repite el número de repeticiones indicado.', instruction_steps_es = '["Sujeta una cuerda a una polea a media altura y ponte de frente a la máquina con los pies separados a la anchura de los hombros.", "Agarra la cuerda y baja a una sentadilla profunda con los brazos extendidos, la espalda recta y el pecho elevado.", "Ponte de pie empujando con las piernas y, al mismo tiempo, tira de la cuerda hacia el abdomen juntando los omóplatos, con los codos cerca del cuerpo.", "Vuelve a bajar a la sentadilla extendiendo los brazos con control.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 439 AND name_en = 'cable squat row (with rope attachment)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd35a264be751c28af004f9f9d2462dbb';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 439 AND name_en = 'cable squat row (with rope attachment)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '9c3648b6a551fec7e6960b0e95ed65d5') THEN
    RAISE EXCEPTION 'V52: ejercicio 439 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 442 cable rope high pulley overhead tricep extension: instructions_es: R2 L14 (ALTA): El texto pide pies a la anchura de los hombros y brazos perpendiculares al suelo; en el GIF hay postura en zancada con el tronco muy inclinado y los brazos casi / V3 W02 ACEPTAR: f1-f12 de espaldas a la polea alta en zancada, tronco muy inclinado; f1 cuerda detrás de la cabeza con los codos flexionados apuntando hacia delante; f7 brazos 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Engancha una cuerda a una polea alta y ponte de espaldas a la máquina, con un pie adelantado y el otro atrás. Inclina el tronco hacia delante y sujeta la cuerda con ambas manos por detrás de la cabeza, con los codos flexionados apuntando hacia delante. Mantén los brazos junto a la cabeza y extiende los codos hasta estirar los brazos hacia delante. Vuelve despacio flexionando los codos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Engancha una cuerda a una polea alta y ponte de espaldas a la máquina, con un pie adelantado y el otro atrás.", "Inclina el tronco hacia delante y sujeta la cuerda con ambas manos por detrás de la cabeza, con los codos flexionados apuntando hacia delante.", "Mantén los brazos junto a la cabeza y extiende los codos hasta estirar los brazos hacia delante.", "Vuelve despacio flexionando los codos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 442 AND name_en = 'cable rope high pulley overhead tricep extension' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '88268cd89041b93a37ab368a05235cc3';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 442 AND name_en = 'cable rope high pulley overhead tricep extension' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'deea0af27b0f3180fb7ccc09be63eac6') THEN
    RAISE EXCEPTION 'V52: ejercicio 442 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 449 cable squatting curl: instructions_es: R2 L01 (ALTA): El texto pide bajar a la sentadilla mientras se hace el curl y mantener las rodillas detrás de las puntas; el GIF mantiene la sentadilla profunda fija todo el t / V3 V03 ACEPTAR: f1 sentadilla profunda fija frente a la polea baja con barra corta y brazos extendidos; f7 misma sentadilla con la barra a la altura del pecho; las piernas no s
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca una barra corta en la polea más baja y ponte de frente a la máquina. Baja a una sentadilla profunda con los pies apoyados y sujeta la barra con agarre supino y los brazos extendidos. Sin levantarte, flexiona los codos y sube la barra hacia el pecho manteniendo los codos junto al cuerpo. Baja la barra con control hasta extender los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca una barra corta en la polea más baja y ponte de frente a la máquina.", "Baja a una sentadilla profunda con los pies apoyados y sujeta la barra con agarre supino y los brazos extendidos.", "Sin levantarte, flexiona los codos y sube la barra hacia el pecho manteniendo los codos junto al cuerpo.", "Baja la barra con control hasta extender los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 449 AND name_en = 'cable squatting curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ab62aaddb01f2a82c36a8b90ef7ab802';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 449 AND name_en = 'cable squatting curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'bd45baa099faa04bb6f2de99571f0c57') THEN
    RAISE EXCEPTION 'V52: ejercicio 449 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 452 cable standing pulldown (with rope): movement_pattern: R2 L04 (MEDIA): El GIF muestra un curl alto de bíceps con cuerda (codo como bisagra, se resaltan bíceps), no un jalón vertical; el nombre Jalón puede confundir (alternativa: Cu / V3 V03 ACEPTAR: f1 brazos extendidos hacia la polea alta con la cuerda a la altura de la cabeza; f7 codos flexionados y cuerda junto a la cara; el movimiento principal es la fl
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET movement_pattern = 'ELBOW_FLEXION'
   WHERE exercise_id = 452 AND name_en = 'cable standing pulldown (with rope)' AND movement_pattern = 'VERTICAL_PULL';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 452 AND name_en = 'cable standing pulldown (with rope)' AND movement_pattern = 'ELBOW_FLEXION') THEN
    RAISE EXCEPTION 'V52: ejercicio 452 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 453 calf push stretch with hands against wall: instructions_es: R2 L05 (ALTA): El texto dice dar un paso atrás con la pierna estirada y flexionar la rodilla delantera; el GIF muestra la pierna estirada delante, con la punta del pie apoyada / V3 V03 ACEPTAR: f1-f12 manos en la pared a la altura de los hombros, pierna delantera estirada con la punta del pie contra la pared y el talón en el suelo, pierna trasera algo 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie frente a una pared y apoya las manos en ella a la altura de los hombros. Coloca la punta de un pie contra la pared con el talón en el suelo y la pierna estirada; deja el otro pie atrás. Lleva la cadera hacia la pared hasta notar el estiramiento en la pantorrilla de la pierna delantera. Mantén la posición con un balanceo suave y cambia de pierna al terminar. Mantén la duración indicada.', instruction_steps_es = '["Ponte de pie frente a una pared y apoya las manos en ella a la altura de los hombros.", "Coloca la punta de un pie contra la pared con el talón en el suelo y la pierna estirada; deja el otro pie atrás.", "Lleva la cadera hacia la pared hasta notar el estiramiento en la pantorrilla de la pierna delantera.", "Mantén la posición con un balanceo suave y cambia de pierna al terminar.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 453 AND name_en = 'calf push stretch with hands against wall' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b3dc43d5259b030d8c0cea59cf589bb4';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 453 AND name_en = 'calf push stretch with hands against wall' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '0038dd10c80ddbbc4528475a0db6e2cb') THEN
    RAISE EXCEPTION 'V52: ejercicio 453 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 455 circles knee stretch: instructions_es: R2 L07 (MEDIA): El texto pide pies separados a la altura de los hombros y equilibrarse sobre las puntas; el GIF muestra los pies juntos y no se aprecia elevación de talones. Ad / V3 V03 ACEPTAR: f1 pies juntos y manos en las caderas; f4-f10 rodillas flexionadas describiendo círculos con los pies apoyados; no se ve elevación de talones; el texto actual p | is_stretch: R2 L07 (MEDIA): De pie con los pies juntos y las manos en las caderas, flexiona ligeramente las rodillas y las mueve en círculos manteniendo los pies en el suelo / V3 V03 ACEPTAR: f1-f12 movimiento circular continuo de rodillas sin mantener ninguna posición de elongación: movilidad dinámica, no estiramiento
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies juntos y las manos en las caderas. Flexiona ligeramente las rodillas manteniéndolas juntas. Describe círculos con las rodillas de forma lenta y controlada. Cambia al sentido contrario a mitad del tiempo. Mantén la duración indicada.', instruction_steps_es = '["Ponte de pie con los pies juntos y las manos en las caderas.", "Flexiona ligeramente las rodillas manteniéndolas juntas.", "Describe círculos con las rodillas de forma lenta y controlada.", "Cambia al sentido contrario a mitad del tiempo.", "Mantén la duración indicada."]'::jsonb, is_stretch = FALSE
   WHERE exercise_id = 455 AND name_en = 'circles knee stretch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '19c3e7d66accb09482275c8aa6afbe6e' AND is_stretch = TRUE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 455 AND name_en = 'circles knee stretch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b90c1c081063cbed988dafad041b992c' AND is_stretch = FALSE) THEN
    RAISE EXCEPTION 'V52: ejercicio 455 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 459 chest dip on straight bar: name_es: R2 L11 (ALTA): El GIF muestra dos barras paralelas, no una barra recta: el nombre (procedente de 'straight bar') no corresponde. No se ve la entrada a la posición / V3 W02 ACEPTAR: f1-f12 dos barras paralelas, no una barra recta: el nombre actual describe otro ejercicio (fondos en barra única); f7 tronco inclinado hacia delante, propio de  | instructions_es: R2 L11 (ALTA): Dice flexionar las rodillas y cruzar los tobillos; en el GIF las piernas cuelgan casi rectas con los pies juntos, y no menciona la inclinación del tronco hacia  / V3 W02 ACEPTAR: f1 brazos extendidos en paralelas con las piernas colgando casi rectas, sin tobillos cruzados; f7 tronco inclinado hacia delante y hombros a la altura de los co
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Fondos de pecho en paralelas', instructions_es = 'Sujétate en las barras paralelas con los brazos extendidos y los pies sin apoyo. Inclina el tronco hacia delante y baja flexionando los codos hasta que los hombros queden a la altura de los codos. Empuja con las manos para volver arriba extendiendo los brazos. Mantén los hombros alejados de las orejas durante todo el movimiento. Repite el número de repeticiones indicado.', instruction_steps_es = '["Sujétate en las barras paralelas con los brazos extendidos y los pies sin apoyo.", "Inclina el tronco hacia delante y baja flexionando los codos hasta que los hombros queden a la altura de los codos.", "Empuja con las manos para volver arriba extendiendo los brazos.", "Mantén los hombros alejados de las orejas durante todo el movimiento.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 459 AND name_en = 'chest dip on straight bar' AND name_es = 'Fondos en barra recta' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5e0b477e5b2c7868b037d1d12823e9c8';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 459 AND name_en = 'chest dip on straight bar' AND name_es = 'Fondos de pecho en paralelas' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3475741bbff4bb7441bd47f6680cb8cb') THEN
    RAISE EXCEPTION 'V52: ejercicio 459 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 460 chest tap push-up (male): difficulty_level: R2 L12 (ALTA): pliometría de tren superior; fuerza relativa / V3 W02 ACEPTAR: f1 plancha alta abajo; f4-f8 el cuerpo sale despedido hacia arriba con las manos despegadas del suelo tocando el pecho y solo los pies apoyados: flexión pliomét
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 460 AND name_en = 'chest tap push-up (male)' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 460 AND name_en = 'chest tap push-up (male)' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 460 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 462 captains chair straight leg raise: difficulty_level: R2 L14 (ALTA): soporte del peso corporal en antebrazos; fuerza de flexores de cadera / V3 W02 ACEPTAR: f1-f12 peso corporal sostenido sobre los antebrazos sin apoyo de pies (soporte en suspensión) y f7 piernas rectas a la horizontal: exige fuerza de flexores de c
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 462 AND name_en = 'captains chair straight leg raise' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 462 AND name_en = 'captains chair straight leg raise' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 462 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 465 chair leg extended stretch: instructions_es: R2 L17 (MEDIA): El texto pide mantener el talón en el suelo e inclinarse hacia delante para estirar los cuádriceps; el GIF muestra la pierna elevada en el aire sin apoyar el ta / V3 W02 ACEPTAR: f1 sentado en el borde del banco con las manos sobre el asiento y los pies apoyados; f5-f9 una pierna extendida casi horizontal en el aire y mantenida, tronco e
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en el borde de un banco o una silla con la espalda recta, los pies apoyados en el suelo y las manos sobre el asiento. Extiende una rodilla y eleva la pierna recta hasta que quede casi horizontal. Mantén la pierna elevada unos instantes sin inclinar el tronco. Bájala con control hasta apoyar el pie. Cambia de pierna. Mantén la duración indicada.', instruction_steps_es = '["Siéntate en el borde de un banco o una silla con la espalda recta, los pies apoyados en el suelo y las manos sobre el asiento.", "Extiende una rodilla y eleva la pierna recta hasta que quede casi horizontal.", "Mantén la pierna elevada unos instantes sin inclinar el tronco.", "Bájala con control hasta apoyar el pie.", "Cambia de pierna.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 465 AND name_en = 'chair leg extended stretch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3f1b70cea33cda23ee8ba8e80a8cb2e2';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 465 AND name_en = 'chair leg extended stretch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'de968a044b8593564d568b70c723ff75') THEN
    RAISE EXCEPTION 'V52: ejercicio 465 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 466 chest stretch with exercise ball: requires_overhead: R2 L18 (MEDIA): OH propuesta: el brazo apoyado en el balón llega a flexión completa del hombro, junto a la cabeza. El GIF oscila entre las dos posiciones en lugar de mantener;  / V3 W03 ACEPTAR: f7 el brazo apoyado en el balón queda extendido por delante, en prolongación del tronco y junto a la cabeza (flexión completa del hombro); f1 ya a la altura de 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = TRUE
   WHERE exercise_id = 466 AND name_en = 'chest stretch with exercise ball' AND requires_overhead = FALSE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 466 AND name_en = 'chest stretch with exercise ball' AND requires_overhead = TRUE) THEN
    RAISE EXCEPTION 'V52: ejercicio 466 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 468 cambered bar lying row: instructions_es: R2 L19 (ALTA): El texto dice piernas rectas con los pies en el suelo y levantar la barra del suelo; en el GIF las piernas descansan sobre el banco y la barra cuelga sin tocar  / V3 Y01 ACEPTAR: f1-f12 piernas estiradas apoyadas sobre el banco con los pies fuera del extremo; f1 brazos colgando estirados con la barra curvada sin tocar el suelo; f7 barra 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca abajo sobre un banco plano alto con el pecho cerca del borde y las piernas estiradas sobre el banco. Sujeta la barra con agarre prono, algo más ancho que los hombros, con los brazos colgando estirados. Tira de la barra hacia el banco llevando los codos hacia arriba y juntando los omóplatos. Baja la barra con control hasta estirar los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca abajo sobre un banco plano alto con el pecho cerca del borde y las piernas estiradas sobre el banco.", "Sujeta la barra con agarre prono, algo más ancho que los hombros, con los brazos colgando estirados.", "Tira de la barra hacia el banco llevando los codos hacia arriba y juntando los omóplatos.", "Baja la barra con control hasta estirar los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 468 AND name_en = 'cambered bar lying row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'f1d5d35abb442e17f722a5e0605b41f4';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 468 AND name_en = 'cambered bar lying row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd80d469d396712c97a7f75dfd1429c96') THEN
    RAISE EXCEPTION 'V52: ejercicio 468 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 476 cable twist: instructions_es: R2 L07 (ALTA): El texto dice ponerse frente a la máquina y alternar lados; el GIF muestra la polea a un costado (de lado a la máquina) y el giro solo hacia un lado / V3 V03 ACEPTAR: f1-f12 vista frontal, cable horizontal que llega desde el costado a la altura del pecho; brazos extendidos al frente; f1 manos hacia la polea y f7 manos giradas
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate de pie de lado a una polea situada a la altura del pecho, con los pies separados a la anchura de los hombros. Sujeta el agarre con ambas manos y los brazos extendidos al frente, con las manos hacia la polea. Activa el core y gira el tronco alejando las manos de la polea sin doblar los brazos. Vuelve con control a la posición inicial resistiendo la tracción del cable. Completa la serie y cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate de pie de lado a una polea situada a la altura del pecho, con los pies separados a la anchura de los hombros.", "Sujeta el agarre con ambas manos y los brazos extendidos al frente, con las manos hacia la polea.", "Activa el core y gira el tronco alejando las manos de la polea sin doblar los brazos.", "Vuelve con control a la posición inicial resistiendo la tracción del cable.", "Completa la serie y cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 476 AND name_en = 'cable twist' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5f9b4c35b9b2e378a4d92177c5132ec7';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 476 AND name_en = 'cable twist' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '87a546c84f55d0cc1f3be22b6b3c7108') THEN
    RAISE EXCEPTION 'V52: ejercicio 476 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 481 cable straight arm pulldown (with rope): requires_overhead: R2 L12 (MEDIA): En ningún fotograma las manos superan la altura de los hombros; OH no procede / V3 W03 ACEPTAR: f1 y f12 (posición más alta) manos a la altura de la barbilla, por debajo de la cabeza; f4-f9 bajan hacia los muslos; en ningún fotograma los brazos pasan por e / coordinador: definicion literal demostrada en todos los fotogramas (R2 + V3)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = FALSE
   WHERE exercise_id = 481 AND name_en = 'cable straight arm pulldown (with rope)' AND requires_overhead = TRUE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 481 AND name_en = 'cable straight arm pulldown (with rope)' AND requires_overhead = FALSE) THEN
    RAISE EXCEPTION 'V52: ejercicio 481 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 483 cable thibaudeau kayak row: movement_pattern: R2 L14 (MEDIA): Con brazos casi rectos desde arriba hacia la cadera es una extensión de hombro, no un remo horizontal / V3 W03 ACEPTAR: f1 y f13 brazos rectos al frente a la altura de los hombros; f7 barra junto a la cadera y f19 junto a la otra cadera con los codos casi extendidos: extensión de
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET movement_pattern = 'SHOULDER_EXTENSION'
   WHERE exercise_id = 483 AND name_en = 'cable thibaudeau kayak row' AND movement_pattern = 'HORIZONTAL_PULL';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 483 AND name_en = 'cable thibaudeau kayak row' AND movement_pattern = 'SHOULDER_EXTENSION') THEN
    RAISE EXCEPTION 'V52: ejercicio 483 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 484 cable twisting pull: instructions_es: R2 L15 (MEDIA): El texto indica polea baja, pies a la anchura de los hombros y rodillas ligeramente flexionadas; el GIF muestra una zancada amplia con el tronco muy inclinado y / V3 W03 MEJORAR: f1 zancada amplia con el tronco inclinado unos 45 grados y el brazo extendido hacia la polea; f7 asa junto al pecho con giro del tronco hacia el lado que tira; 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Engancha un asa a una polea situada más o menos a la altura de la cadera, de modo que el cable quede casi horizontal, y sujétala con una mano. Colócate en zancada amplia, inclina el tronco hacia delante con la espalda recta y el core activado, y deja el brazo libre junto a la pierna adelantada. Desde el brazo extendido hacia la polea, tira del asa hacia el pecho girando el tronco hacia el lado del brazo que tira. Vuelve despacio a extender el brazo manteniendo la tensión del cable. Completa las repeticiones y cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Engancha un asa a una polea situada más o menos a la altura de la cadera, de modo que el cable quede casi horizontal, y sujétala con una mano.", "Colócate en zancada amplia, inclina el tronco hacia delante con la espalda recta y el core activado, y deja el brazo libre junto a la pierna adelantada.", "Desde el brazo extendido hacia la polea, tira del asa hacia el pecho girando el tronco hacia el lado del brazo que tira.", "Vuelve despacio a extender el brazo manteniendo la tensión del cable.", "Completa las repeticiones y cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 484 AND name_en = 'cable twisting pull' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a297114e0d043e7753e752be2ea332ad';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 484 AND name_en = 'cable twisting pull' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '51dfde9b57591b18478d44717f1f2838') THEN
    RAISE EXCEPTION 'V52: ejercicio 484 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 489 cable triceps pushdown (v-bar): instructions_es: R2 L20 (ALTA): El texto pide agarre prono con las manos separadas a la anchura de los hombros; en el GIF las manos van juntas en el centro sobre la barra en V / V3 W03 ACEPTAR: f1 y f7 las dos manos juntas en el centro de la barra en V, codos pegados a los costados; f1 codos flexionados, f7 codos extendidos; el texto actual pide manos 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Engancha una barra en V a una polea alta y ponte de pie frente a la máquina con los pies separados al ancho de los hombros. Sujeta la barra en V con ambas manos juntas en el centro, con los codos pegados a los costados y flexionados. Sin mover la parte superior de los brazos, empuja la barra hacia abajo hasta extender por completo los codos. Vuelve despacio a la posición inicial manteniendo el control. Repite el número de repeticiones indicado.', instruction_steps_es = '["Engancha una barra en V a una polea alta y ponte de pie frente a la máquina con los pies separados al ancho de los hombros.", "Sujeta la barra en V con ambas manos juntas en el centro, con los codos pegados a los costados y flexionados.", "Sin mover la parte superior de los brazos, empuja la barra hacia abajo hasta extender por completo los codos.", "Vuelve despacio a la posición inicial manteniendo el control.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 489 AND name_en = 'cable triceps pushdown (v-bar)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b1e8dd8008b70d14b97a9a934e151c3a';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 489 AND name_en = 'cable triceps pushdown (v-bar)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '93fb5b4da7ab5c816de5c89f9db7fcaf') THEN
    RAISE EXCEPTION 'V52: ejercicio 489 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 492 cable two arm tricep kickback: instructions_es: R2 L03 (ALTA): El texto no menciona la inclinación del tronco: describe estar de pie con rodillas algo flexionadas, cuando en el GIF el tronco está inclinado desde la cadera c / V3 V03 ACEPTAR: f1-f12 de frente a la estación con dos cables desde poleas bajas (las dos pilas suben en f5-f7), tronco inclinado casi paralelo al suelo; f1 codos flexionados c
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie frente a la estación de poleas con un asa en cada mano y los pies separados al ancho de los hombros. Flexiona ligeramente las rodillas e inclina el tronco desde la cadera hasta dejarlo casi paralelo al suelo, con la espalda recta. Pega los codos al tronco, flexionados, con las manos cerca de la cadera. Sin mover los codos, extiéndelos llevando las manos hacia atrás hasta estirar los brazos. Vuelve despacio a la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie frente a la estación de poleas con un asa en cada mano y los pies separados al ancho de los hombros.", "Flexiona ligeramente las rodillas e inclina el tronco desde la cadera hasta dejarlo casi paralelo al suelo, con la espalda recta.", "Pega los codos al tronco, flexionados, con las manos cerca de la cadera.", "Sin mover los codos, extiéndelos llevando las manos hacia atrás hasta estirar los brazos.", "Vuelve despacio a la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 492 AND name_en = 'cable two arm tricep kickback' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '658eec52618b50b3d368663eaadedf5c';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 492 AND name_en = 'cable two arm tricep kickback' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '8ee565ebd1f62cd90b43a424c6652a84') THEN
    RAISE EXCEPTION 'V52: ejercicio 492 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 493 donkey calf raise: instructions_es: R2 L04 (ALTA): El texto describe estar de pie con las manos en una pared o pasamanos; el GIF muestra el tronco flexionado casi en horizontal con las manos apoyadas en un banco / V3 V03 ACEPTAR: f1-f12 puntas de los pies sobre un escalón, tronco flexionado casi horizontal con las manos apoyadas en un banco; f1 talones abajo, f7 talones arriba; el texto 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca las puntas de los pies sobre un escalón o bloque, frente a un banco. Inclina el tronco hacia delante desde la cadera, con la espalda recta, y apoya las manos en el banco. Eleva los talones lo más alto posible quedando sobre las puntas de los pies. Baja los talones con control por debajo del nivel del escalón. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca las puntas de los pies sobre un escalón o bloque, frente a un banco.", "Inclina el tronco hacia delante desde la cadera, con la espalda recta, y apoya las manos en el banco.", "Eleva los talones lo más alto posible quedando sobre las puntas de los pies.", "Baja los talones con control por debajo del nivel del escalón.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 493 AND name_en = 'donkey calf raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'af001ea05e8221f1f0fc1350f9c31a2c';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 493 AND name_en = 'donkey calf raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '399ccb51be15b874a7a0055aba240ccf') THEN
    RAISE EXCEPTION 'V52: ejercicio 493 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 495 decline push-up: difficulty_level: R2 L06 (ALTA): fuerza relativa moderada / V3 V03 ACEPTAR: f1 plancha con los pies sobre el banco y las manos en el suelo, f7 pecho cerca del suelo; flexión completa declinada que exige fuerza relativa moderada (perfil 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 495 AND name_en = 'decline push-up' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 495 AND name_en = 'decline push-up' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 495 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 496 drop push up: difficulty_level: R2 L07 (MEDIA): pliometría de tren superior; fuerza relativa moderada / V3 V03 ACEPTAR: f1-f7 manos en los bancos, f10-f13 caída hasta el suelo, f19 flexión profunda, f22-f24 empuje explosivo de vuelta a los bancos: pliometría de tren superior con 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 496 AND name_en = 'drop push up' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 496 AND name_en = 'drop push up' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 496 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 503 dead bug: requires_overhead: R2 L14 (ALTA): Los brazos apuntan al techo (flexión de hombro de 90 grados) y nunca pasan por encima de la cabeza: sobra OH / V3 W03 ACEPTAR: f1-f12 tumbado boca arriba con los brazos verticales hacia el techo (unos 90 grados de flexión) que no se mueven; nunca pasan por encima de la cabeza / coordinador: definicion literal demostrada en todos los fotogramas (R2 + V3) | instructions_es: R2 L14 (ALTA): El texto pide bajar el brazo derecho junto con la pierna izquierda; en el GIF los brazos permanecen verticales y solo se mueve una pierna / V3 W03 ACEPTAR: f1 caderas y rodillas flexionadas con los brazos hacia el techo; f7 una pierna extendida cerca del suelo y los brazos siguen verticales; ningún brazo baja en ni
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = FALSE, instructions_es = 'Túmbate boca arriba en el suelo con los brazos estirados hacia el techo. Levanta las piernas con las caderas y las rodillas flexionadas a 90 grados. Mantén la zona lumbar pegada al suelo y estira una pierna hacia delante hasta dejarla cerca del suelo, sin mover los brazos. Vuelve a flexionar la pierna hasta la posición inicial. Haz el mismo movimiento con la otra pierna. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en el suelo con los brazos estirados hacia el techo.", "Levanta las piernas con las caderas y las rodillas flexionadas a 90 grados.", "Mantén la zona lumbar pegada al suelo y estira una pierna hacia delante hasta dejarla cerca del suelo, sin mover los brazos.", "Vuelve a flexionar la pierna hasta la posición inicial.", "Haz el mismo movimiento con la otra pierna.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 503 AND name_en = 'dead bug' AND requires_overhead = TRUE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b9f029e6c684af62e221ce76edea1b19';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 503 AND name_en = 'dead bug' AND requires_overhead = FALSE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a656f9c290f7e6df85077ea4b019cb2a') THEN
    RAISE EXCEPTION 'V52: ejercicio 503 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 508 close-grip push-up: instructions_es: R2 L19 (ALTA): El texto dice manos juntas y a la vez debajo de los hombros; en el GIF las manos están separadas a la anchura de los hombros, no juntas / V3 Y01 ACEPTAR: f1 y f7 manos separadas a la anchura de los hombros, bajo ellos, no juntas; flexión completa sobre los pies; el texto actual (manos juntas) induce a una flexión | difficulty_level: R2 L19 (ALTA): fuerza relativa moderada (flexión completa sobre los pies) / V3 Y01 ACEPTAR: f1-f7 flexión completa sobre las puntas de los pies desde el pecho casi en el suelo hasta brazos estirados: fuerza relativa moderada, perfil de nivel 2
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate en plancha alta en el suelo con las manos a la anchura de los hombros, justo debajo de ellos, y el cuerpo en línea recta. Activa el core y baja el pecho hacia el suelo flexionando los codos. Empuja con las palmas hasta estirar los brazos y volver a la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate en plancha alta en el suelo con las manos a la anchura de los hombros, justo debajo de ellos, y el cuerpo en línea recta.", "Activa el core y baja el pecho hacia el suelo flexionando los codos.", "Empuja con las palmas hasta estirar los brazos y volver a la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb, difficulty_level = 2
   WHERE exercise_id = 508 AND name_en = 'close-grip push-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '39bce0e0e3760069309b1273f5dd1ec6' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 508 AND name_en = 'close-grip push-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '2d285f0a6f2eb44e4217605ff1a6a5c0' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 508 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 510 diamond push-up: difficulty_level: R2 L01 (ALTA): fuerza relativa moderada / V3 V03 ACEPTAR: f1 plancha alta sobre las puntas de los pies con las manos juntas bajo el pecho, f7 pecho cerca del suelo; flexión completa con manos juntas que exige fuerza re
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 510 AND name_en = 'diamond push-up' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 510 AND name_en = 'diamond push-up' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 510 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 511 dumbbell alternate side press: axial_load: R2 L02 (MEDIA): Press por encima de la cabeza con mancuerna: cumple el criterio AX (cargas en press por encima de la cabeza). Se aprecia una ligera inclinación lateral del tron / V3 V03 ACEPTAR: f1 mancuernas a la altura de los hombros, f7 una mancuerna empujada por encima de la cabeza con el brazo extendido mientras la otra queda en el hombro: carga en
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET axial_load = TRUE
   WHERE exercise_id = 511 AND name_en = 'dumbbell alternate side press' AND axial_load = FALSE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 511 AND name_en = 'dumbbell alternate side press' AND axial_load = TRUE) THEN
    RAISE EXCEPTION 'V52: ejercicio 511 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 514 dumbbell bench seated press: axial_load: R2 L05 (ALTA): Press de carga por encima de la cabeza: según la definición de AX debería marcarse / V3 V03 ACEPTAR: f1 sentado con mancuernas a la altura de los hombros, f7 ambas mancuernas sobre la cabeza con brazos extendidos: carga en press por encima de la cabeza, criteri
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET axial_load = TRUE
   WHERE exercise_id = 514 AND name_en = 'dumbbell bench seated press' AND axial_load = FALSE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 514 AND name_en = 'dumbbell bench seated press' AND axial_load = TRUE) THEN
    RAISE EXCEPTION 'V52: ejercicio 514 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 515 deep push up: instructions_es: R2 L06 (ALTA): El texto no menciona las mancuernas: en el GIF las manos agarran dos mancuernas apoyadas en el suelo, que permiten bajar el pecho más allá de las manos. / V3 V03 MEJORAR: f1 plancha alta con las manos en dos mancuernas apoyadas en el suelo; f7 pecho entre las mancuernas aproximadamente a la altura de los agarres; no se ve que baj
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca dos mancuernas en el suelo, un poco más separadas que el ancho de los hombros, y sujétalas en posición de plancha alta. Mantén el cuerpo en línea recta de la cabeza a los pies. Flexiona los codos y baja el pecho entre las mancuernas hasta la altura de las manos. Empuja hasta extender los brazos y volver a la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca dos mancuernas en el suelo, un poco más separadas que el ancho de los hombros, y sujétalas en posición de plancha alta.", "Mantén el cuerpo en línea recta de la cabeza a los pies.", "Flexiona los codos y baja el pecho entre las mancuernas hasta la altura de las manos.", "Empuja hasta extender los brazos y volver a la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 515 AND name_en = 'deep push up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'acdb8750518ce92ab1696ca8aad28ffd';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 515 AND name_en = 'deep push up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '9b56b4003bbcf832814bf2b551369194') THEN
    RAISE EXCEPTION 'V52: ejercicio 515 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 520 dumbbell alternate biceps curl: name_es: R2 L11 (ALTA): Errata en el nombre («mancuerdas»). Curl de bíceps de pie: dificultad 2 parece alta (1) / V3 W03 ACEPTAR: f1 y f7 curl de pie alterno con dos mancuernas; «mancuerdas» es una errata
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Curl de bíceps alterno con mancuernas'
   WHERE exercise_id = 520 AND name_en = 'dumbbell alternate biceps curl' AND name_es = 'Curl de bíceps alterno con mancuerdas';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 520 AND name_en = 'dumbbell alternate biceps curl' AND name_es = 'Curl de bíceps alterno con mancuernas') THEN
    RAISE EXCEPTION 'V52: ejercicio 520 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 522 dumbbell alternate hammer preacher curl: instructions_es: R2 L13 (MEDIA): El texto describe levantar ambas mancuernas a la vez; el GIF muestra un curl alterno, un brazo sube mientras el otro baja / V3 W03 ACEPTAR: f1 un brazo arriba junto al hombro y el otro abajo sobre el cojín del banco predicador; f7 al revés: curl alterno con agarre neutro; el texto actual sube las do
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en el banco predicador con una mancuerna en cada mano y apoya la parte posterior de los brazos en el cojín. Sujeta las mancuernas con las palmas mirando hacia el cuerpo y los brazos casi extendidos. Flexiona un codo y lleva la mancuerna hacia el hombro sin despegar el brazo del cojín. Bájala despacio mientras subes la otra mancuerna. Alterna los lados. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en el banco predicador con una mancuerna en cada mano y apoya la parte posterior de los brazos en el cojín.", "Sujeta las mancuernas con las palmas mirando hacia el cuerpo y los brazos casi extendidos.", "Flexiona un codo y lleva la mancuerna hacia el hombro sin despegar el brazo del cojín.", "Bájala despacio mientras subes la otra mancuerna.", "Alterna los lados.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 522 AND name_en = 'dumbbell alternate hammer preacher curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '1a0b10b611a8af16064225a7a3ea6f44';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 522 AND name_en = 'dumbbell alternate hammer preacher curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'dc4fa1f6c3824cceabecdee692603cd0') THEN
    RAISE EXCEPTION 'V52: ejercicio 522 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 527 dumbbell bicep curl lunge with bowling motion: instructions_es: R2 L18 (MEDIA): El texto dice flexionar las mancuernas al avanzar y girar el tronco como lanzando una bola de bolos; el GIF muestra las mancuernas arriba de pie y abajo en la z / V3 W03 ACEPTAR: f1 de pie con las mancuernas a la altura de los hombros y codos flexionados; f7 zancada profunda con los brazos extendidos y las mancuernas junto a la pierna de
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con una mancuerna en cada mano a la altura de los hombros y los codos flexionados. Da un paso adelante y baja a zancada mientras extiendes los brazos y llevas las mancuernas hacia abajo junto a la pierna delantera. Baja hasta que la rodilla trasera quede cerca del suelo sin apoyarla, con el tronco erguido. Empuja con la pierna delantera para volver atrás mientras flexionas los codos y subes las mancuernas a los hombros. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con una mancuerna en cada mano a la altura de los hombros y los codos flexionados.", "Da un paso adelante y baja a zancada mientras extiendes los brazos y llevas las mancuernas hacia abajo junto a la pierna delantera.", "Baja hasta que la rodilla trasera quede cerca del suelo sin apoyarla, con el tronco erguido.", "Empuja con la pierna delantera para volver atrás mientras flexionas los codos y subes las mancuernas a los hombros.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 527 AND name_en = 'dumbbell bicep curl lunge with bowling motion' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '6a76388043dd420d2b303fdc4a663749';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 527 AND name_en = 'dumbbell bicep curl lunge with bowling motion' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'c0db52a2c812f448fe88b438f25faf4d') THEN
    RAISE EXCEPTION 'V52: ejercicio 527 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 532 cycle cross trainer: instructions_es: R2 L03 (ALTA): El texto habla de colocarse en una «bicicleta elíptica»; el GIF muestra una bicicleta de aire con ventilador y manillares móviles, y el texto no menciona el emp / V3 V03 ACEPTAR: f1-f12 bicicleta de aire con ventilador; pedaleo con manillares móviles que avanzan y retroceden de forma alterna (f1 frente a f7); el texto actual habla de bic
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ajusta la altura del asiento de la bicicleta de aire y siéntate. Apoya los pies en los pedales y sujeta los manillares móviles. Pedalea mientras empujas y tiras de los manillares de forma alterna, coordinando brazos y piernas. Mantén el tronco erguido y un ritmo constante. Mantén la duración indicada.', instruction_steps_es = '["Ajusta la altura del asiento de la bicicleta de aire y siéntate.", "Apoya los pies en los pedales y sujeta los manillares móviles.", "Pedalea mientras empujas y tiras de los manillares de forma alterna, coordinando brazos y piernas.", "Mantén el tronco erguido y un ritmo constante.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 532 AND name_en = 'cycle cross trainer' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5462c876d50b7270a96d52ca923f7946';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 532 AND name_en = 'cycle cross trainer' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'dbe51e55d5d12c9f05c43469fdf30448') THEN
    RAISE EXCEPTION 'V52: ejercicio 532 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 533 dumbbell burpee: requires_overhead: R2 L04 (ALTA): Los brazos nunca suben por encima de la cabeza (salto final con mancuernas a los costados): sobra OH / V3 V03 ACEPTAR: f1-f30 mancuernas siempre a los costados o en el suelo: de pie, cuclillas, plancha, cuclillas, de pie con pequeño salto (f25-f30); en ningún fotograma los brazo / coordinador: definicion literal demostrada en todos los fotogramas (R2 + V3)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = FALSE
   WHERE exercise_id = 533 AND name_en = 'dumbbell burpee' AND requires_overhead = TRUE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 533 AND name_en = 'dumbbell burpee' AND requires_overhead = FALSE) THEN
    RAISE EXCEPTION 'V52: ejercicio 533 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 534 dumbbell finger curls: instructions_es: R2 L05 (ALTA): El texto dice sostener una mancuerna en una mano y cambiar de mano al terminar; el GIF muestra una mancuerna en cada mano trabajando a la vez / V3 V03 ACEPTAR: f1-f12 sentado en banco, tronco inclinado, antebrazos sobre los muslos, una mancuerna en cada mano; ambas ruedan y se recogen a la vez (f1 frente a f5); el text
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en un banco con los pies apoyados en el suelo y el tronco ligeramente inclinado hacia delante. Sujeta una mancuerna en cada mano y apoya los antebrazos sobre los muslos, con las palmas hacia arriba. Abre los dedos despacio para que las mancuernas rueden hacia las puntas. Flexiona los dedos para volver a recogerlas en la mano. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en un banco con los pies apoyados en el suelo y el tronco ligeramente inclinado hacia delante.", "Sujeta una mancuerna en cada mano y apoya los antebrazos sobre los muslos, con las palmas hacia arriba.", "Abre los dedos despacio para que las mancuernas rueden hacia las puntas.", "Flexiona los dedos para volver a recogerlas en la mano.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 534 AND name_en = 'dumbbell finger curls' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '21eb9385d2896b7e37b4c23a90982877';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 534 AND name_en = 'dumbbell finger curls' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '8565fa91c89919c450a73c27e25f8eea') THEN
    RAISE EXCEPTION 'V52: ejercicio 534 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 549 dumbbell decline shrug: requires_inversion: R2 L20 (ALTA): La cabeza queda claramente por encima de la cadera (banco inclinado) y no hay peso en brazos o cabeza: INV no aplica. El recorrido de la elevación es corto. / V3 W03 ACEPTAR: f1-f12 pecho apoyado en banco inclinado con la cabeza por encima de la cadera y los pies en el suelo; no hay peso en brazos, hombros ni cabeza / coordinador: definicion literal demostrada en todos los fotogramas (R2 + V3)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_inversion = FALSE
   WHERE exercise_id = 549 AND name_en = 'dumbbell decline shrug' AND requires_inversion = TRUE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 549 AND name_en = 'dumbbell decline shrug' AND requires_inversion = FALSE) THEN
    RAISE EXCEPTION 'V52: ejercicio 549 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 550 dumbbell decline shrug v. 2: requires_inversion: R2 L01 (ALTA): La cabeza está claramente por encima de la cadera (banco inclinado, pecho apoyado): la bandera INV sobra. El nombre de la fuente dice decline pero el GIF muestr / V3 V03 ACEPTAR: f1-f12 pecho apoyado en un banco inclinado con la cabeza claramente más alta que la cadera y los pies en el suelo; nunca hay cabeza por debajo de la cadera: INV / coordinador: definicion literal demostrada en todos los fotogramas (R2 + V3)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_inversion = FALSE
   WHERE exercise_id = 550 AND name_en = 'dumbbell decline shrug v. 2' AND requires_inversion = TRUE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 550 AND name_en = 'dumbbell decline shrug v. 2' AND requires_inversion = FALSE) THEN
    RAISE EXCEPTION 'V52: ejercicio 550 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 551 dumbbell clean: instructions_es: R2 L02 (MEDIA): El texto pide saltar del suelo y recibir las mancuernas de pie; el GIF no muestra ningún salto (pies apoyados en todo momento) y la recepción se hace en sentadi / V3 V03 ACEPTAR: f1 cuclillas con las mancuernas junto a los pies, f7 de pie con las mancuernas tiradas hasta el pecho y los pies apoyados, f13 sentadilla profunda con las mancu
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate en cuclillas con la espalda recta y una mancuerna en cada mano junto a los pies. Extiende la cadera y las rodillas de forma explosiva y tira de las mancuernas hacia arriba pegadas al cuerpo. Métete rápidamente bajo las mancuernas en sentadilla profunda y recíbelas a la altura de los hombros con los codos al frente. Baja las mancuernas de forma controlada hasta el suelo manteniendo la espalda recta. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate en cuclillas con la espalda recta y una mancuerna en cada mano junto a los pies.", "Extiende la cadera y las rodillas de forma explosiva y tira de las mancuernas hacia arriba pegadas al cuerpo.", "Métete rápidamente bajo las mancuernas en sentadilla profunda y recíbelas a la altura de los hombros con los codos al frente.", "Baja las mancuernas de forma controlada hasta el suelo manteniendo la espalda recta.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 551 AND name_en = 'dumbbell clean' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '9277bbee0361b5251ebc0604ecf03e4a';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 551 AND name_en = 'dumbbell clean' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '211935740c456dac94f053ffd820d35d') THEN
    RAISE EXCEPTION 'V52: ejercicio 551 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 559 dumbbell biceps curl v sit on bosu ball: requires_floor: R2 L10 (MEDIA): FL: el BOSU está apoyado en el suelo y la persona se sienta a ras de suelo; entrar y salir equivale a sentarse en el suelo. No se aprecia con claridad si los ta / V3 V03 ACEPTAR: f1-f12 sentado sobre un BOSU apoyado en el suelo, con la cadera a ras de suelo y las piernas hacia delante con los pies a la altura del suelo; entrar y salir ex | instructions_es: R2 L10 (MEDIA): El texto dice pies planos en el suelo y rodillas flexionadas a 90 grados; el GIF muestra las piernas extendidas al frente casi rectas (posición en V) / V3 V03 MEJORAR: f1 y f7 sentado en el BOSU con las piernas hacia delante y las rodillas flexionadas unos 120 grados, no ligeramente; f1 brazos extendidos con mancuernas junto a
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_floor = TRUE, instructions_es = 'Siéntate en el centro de un BOSU con las piernas hacia delante, las rodillas semiflexionadas y el tronco erguido o algo inclinado hacia atrás. Sujeta una mancuerna en cada mano con los brazos extendidos a los costados. Sin mover la parte superior de los brazos, flexiona los codos y sube las mancuernas hacia los hombros. Baja con control hasta extender los brazos, manteniendo el equilibrio con el abdomen activo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en el centro de un BOSU con las piernas hacia delante, las rodillas semiflexionadas y el tronco erguido o algo inclinado hacia atrás.", "Sujeta una mancuerna en cada mano con los brazos extendidos a los costados.", "Sin mover la parte superior de los brazos, flexiona los codos y sube las mancuernas hacia los hombros.", "Baja con control hasta extender los brazos, manteniendo el equilibrio con el abdomen activo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 559 AND name_en = 'dumbbell biceps curl v sit on bosu ball' AND requires_floor = FALSE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'cbe4843411aaf9a097944799c632afc9';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 559 AND name_en = 'dumbbell biceps curl v sit on bosu ball' AND requires_floor = TRUE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '83412fa302e089ecead7ab9ecb7e6618') THEN
    RAISE EXCEPTION 'V52: ejercicio 559 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 563 dumbbell cross body hammer curl: instructions_es: R2 L14 (ALTA): El texto flexiona los dos brazos a la vez; en el GIF los brazos se alternan / V3 W03 ACEPTAR: f1 un brazo cruza por delante del cuerpo hacia el hombro contrario y el otro cuelga; f7 al revés: curl martillo cruzado alterno; el texto actual flexiona ambos 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con una mancuerna en cada mano, los brazos estirados a los lados y las palmas hacia el cuerpo. Mantén los codos cerca del tronco y flexiona un brazo llevando la mancuerna cruzada por delante del cuerpo hacia el hombro contrario. Baja la mancuerna despacio hasta estirar el brazo. Repite con el otro brazo. Alterna los lados. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con una mancuerna en cada mano, los brazos estirados a los lados y las palmas hacia el cuerpo.", "Mantén los codos cerca del tronco y flexiona un brazo llevando la mancuerna cruzada por delante del cuerpo hacia el hombro contrario.", "Baja la mancuerna despacio hasta estirar el brazo.", "Repite con el otro brazo.", "Alterna los lados.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 563 AND name_en = 'dumbbell cross body hammer curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '2ea6535e0c9e100ad81ffec31b5d354c';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 563 AND name_en = 'dumbbell cross body hammer curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'c804bf4c33ad1d25fb7d65fb31cc742c') THEN
    RAISE EXCEPTION 'V52: ejercicio 563 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 564 dumbbell cross body hammer curl v. 2: instructions_es: R2 L15 (ALTA): El texto pide flexionar ambos brazos a la vez; el GIF alterna los brazos / V3 W03 ACEPTAR: f1 brazo derecho cruzado hacia el hombro contrario y el izquierdo abajo; f7 al revés: curl martillo cruzado alterno; el texto actual flexiona ambos a la vez
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con una mancuerna en cada mano, los brazos junto al cuerpo y las palmas mirando hacia los muslos. Flexiona un codo y lleva la mancuerna en diagonal por delante del cuerpo hacia el hombro contrario, con la palma hacia dentro. Baja la mancuerna despacio hasta extender el brazo. Repite con el otro brazo. Alterna los lados. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con una mancuerna en cada mano, los brazos junto al cuerpo y las palmas mirando hacia los muslos.", "Flexiona un codo y lleva la mancuerna en diagonal por delante del cuerpo hacia el hombro contrario, con la palma hacia dentro.", "Baja la mancuerna despacio hasta extender el brazo.", "Repite con el otro brazo. Alterna los lados.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 564 AND name_en = 'dumbbell cross body hammer curl v. 2' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5b3e812c79590cc988fa6a362b47af41';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 564 AND name_en = 'dumbbell cross body hammer curl v. 2' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b11ca5baa3eb53763361ffd126e34c61') THEN
    RAISE EXCEPTION 'V52: ejercicio 564 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 574 dumbbell incline raise: requires_overhead: R2 L05 (MEDIA): Las manos llegan a la altura de la cabeza por la inclinación del banco, pero los brazos no pasan por encima de la cabeza: OH no parece justificada / V3 V03 ACEPTAR: f1 recostado en banco inclinado con los brazos colgando; f7 mancuernas por delante a la altura del pecho y los hombros, por debajo de la cabeza; en ningún fotog / coordinador: definicion literal demostrada en todos los fotogramas (R2 + V3)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = FALSE
   WHERE exercise_id = 574 AND name_en = 'dumbbell incline raise' AND requires_overhead = TRUE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 574 AND name_en = 'dumbbell incline raise' AND requires_overhead = FALSE) THEN
    RAISE EXCEPTION 'V52: ejercicio 574 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 575 dumbbell incline rear lateral raise: instructions_es: R2 L06 (ALTA): El texto pide sentarse en el banco con el pecho contra el respaldo; en el GIF el usuario no está sentado: apoya el pecho en el banco inclinado con las piernas e / V3 V03 ACEPTAR: f1-f12 pecho apoyado en el respaldo inclinado, piernas extendidas detrás y pies en el suelo, no sentado; f1 brazos colgando, f7 brazos abiertos a la altura de l
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Apóyate boca abajo sobre un banco inclinado, con el pecho contra el respaldo y los pies apoyados en el suelo por detrás. Sujeta una mancuerna en cada mano con los brazos colgando hacia el suelo. Con una ligera flexión de codos, eleva los brazos hacia los lados hasta la altura de los hombros. Baja los brazos despacio a la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Apóyate boca abajo sobre un banco inclinado, con el pecho contra el respaldo y los pies apoyados en el suelo por detrás.", "Sujeta una mancuerna en cada mano con los brazos colgando hacia el suelo.", "Con una ligera flexión de codos, eleva los brazos hacia los lados hasta la altura de los hombros.", "Baja los brazos despacio a la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 575 AND name_en = 'dumbbell incline rear lateral raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '41cdb0fb64d30ae86d8c763b354dc7ea';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 575 AND name_en = 'dumbbell incline rear lateral raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ea581fa8b3d28b990b2ab46f8bab6869') THEN
    RAISE EXCEPTION 'V52: ejercicio 575 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 576 dumbbell incline t-raise: instructions_es: R2 L07 (ALTA): El texto dice siéntate en el banco e inclínate hacia adelante; el GIF muestra el cuerpo tumbado boca abajo con el pecho apoyado en el respaldo y los pies en el  / V3 V03 ACEPTAR: f1-f12 pecho apoyado boca abajo en el respaldo inclinado, a horcajadas con los pies en el suelo; f1 brazos colgando, f7 brazos rectos abiertos en T; el texto ac
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ajusta un banco inclinado y túmbate boca abajo sobre él, con el pecho apoyado en el respaldo y los pies en el suelo. Sujeta una mancuerna en cada mano y deja que los brazos cuelguen rectos hacia el suelo. Manteniendo los brazos rectos, elévalos hacia los lados hasta la altura de los hombros formando una T con el cuerpo. Haz una pausa breve arriba y baja los brazos con control. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ajusta un banco inclinado y túmbate boca abajo sobre él, con el pecho apoyado en el respaldo y los pies en el suelo.", "Sujeta una mancuerna en cada mano y deja que los brazos cuelguen rectos hacia el suelo.", "Manteniendo los brazos rectos, elévalos hacia los lados hasta la altura de los hombros formando una T con el cuerpo.", "Haz una pausa breve arriba y baja los brazos con control.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 576 AND name_en = 'dumbbell incline t-raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '28bb2e00b20094e7bbaa8f533ed10aef';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 576 AND name_en = 'dumbbell incline t-raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '77ae36399857562fddbab6ecb7b7cd28') THEN
    RAISE EXCEPTION 'V52: ejercicio 576 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 579 dumbbell lateral to front raise: instructions_es: R2 L10 (ALTA): El texto describe una elevación lateral completa (subir y bajar) seguida de una elevación frontal; en el GIF los brazos suben por los lados, se desplazan al fre / V3 V03 ACEPTAR: f1 brazos a los costados, f7 elevación lateral a la altura de los hombros, f8-f13 los brazos se desplazan al frente sin bajar, f14-f19 bajan por delante; el tex
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con una mancuerna en cada mano a los costados y los codos ligeramente flexionados. Eleva los brazos hacia los lados hasta la altura de los hombros. Sin bajar los brazos, llévalos hacia el frente manteniéndolos a la altura de los hombros. Baja los brazos por delante con control hasta la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con una mancuerna en cada mano a los costados y los codos ligeramente flexionados.", "Eleva los brazos hacia los lados hasta la altura de los hombros.", "Sin bajar los brazos, llévalos hacia el frente manteniéndolos a la altura de los hombros.", "Baja los brazos por delante con control hasta la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 579 AND name_en = 'dumbbell lateral to front raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3aca10ab71e731b0c5bfd01f0e7351eb';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 579 AND name_en = 'dumbbell lateral to front raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd101836371bbd29683cda280273db28e') THEN
    RAISE EXCEPTION 'V52: ejercicio 579 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 580 dumbbell incline breeding: requires_overhead: R2 L11 (MEDIA): Codos casi rectos durante todo el movimiento: es una apertura, no un press. No se ve la versión con los brazos invertidos (no se puede afirmar que alterne). Dif / V3 W03 ACEPTAR: f1 un brazo extendido hacia arriba y atrás, con la mancuerna por encima de la cabeza; f7 ambas mancuernas juntas por encima de la cara | instructions_es: R2 L11 (MEDIA): Describe un press: bajar las mancuernas a los lados del pecho con los codos a 90 grados y empujar; el GIF muestra aperturas con los codos casi rectos en diagona / V3 W03 ACEPTAR: f1 brazos casi rectos en diagonal, uno arriba y atrás por encima de la cabeza y el otro abierto abajo al lado; f7 mancuernas juntas sobre el pecho con los codos | movement_pattern: R2 L11 (MEDIA): Codos casi rectos durante todo el movimiento: es una apertura, no un press. No se ve la versión con los brazos invertidos (no se puede afirmar que alterne). Dif / V3 W03 ACEPTAR: f1-f12 codos casi extendidos en todo el recorrido, arco de apertura y cierre: aducción horizontal (apertura), no empuje
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = TRUE, instructions_es = 'Siéntate en un banco inclinado con la espalda apoyada y los pies en el suelo, con una mancuerna en cada mano y los brazos extendidos sobre el pecho. Con los codos ligeramente flexionados, abre los brazos en diagonal: uno hacia arriba y atrás por encima de la cabeza y el otro hacia abajo y al lado. Cierra ambos brazos describiendo un arco hasta juntar las mancuernas sobre el pecho. Controla la bajada sin forzar el hombro en la apertura. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en un banco inclinado con la espalda apoyada y los pies en el suelo, con una mancuerna en cada mano y los brazos extendidos sobre el pecho.", "Con los codos ligeramente flexionados, abre los brazos en diagonal: uno hacia arriba y atrás por encima de la cabeza y el otro hacia abajo y al lado.", "Cierra ambos brazos describiendo un arco hasta juntar las mancuernas sobre el pecho.", "Controla la bajada sin forzar el hombro en la apertura.", "Repite el número de repeticiones indicado."]'::jsonb, movement_pattern = 'HORIZONTAL_ADDUCTION'
   WHERE exercise_id = 580 AND name_en = 'dumbbell incline breeding' AND requires_overhead = FALSE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '809b3eb1a0b898bfa5f04c99c8179154' AND movement_pattern = 'HORIZONTAL_PUSH';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 580 AND name_en = 'dumbbell incline breeding' AND requires_overhead = TRUE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '9c411c622e8d9feba9d15528bc308fb1' AND movement_pattern = 'HORIZONTAL_ADDUCTION') THEN
    RAISE EXCEPTION 'V52: ejercicio 580 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 581 dumbbell incline fly: requires_overhead: R2 L12 (MEDIA): En f7 los brazos quedan verticales en banco inclinado (unos 130 grados de flexión respecto al tronco) con las mancuernas por encima de la cabeza; mismo criterio / V3 W03 ACEPTAR: f7 en banco inclinado unos 45 grados los brazos quedan verticales con las mancuernas por encima de la altura de la cabeza (unos 130 grados de flexión de hombro)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = TRUE
   WHERE exercise_id = 581 AND name_en = 'dumbbell incline fly' AND requires_overhead = FALSE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 581 AND name_en = 'dumbbell incline fly' AND requires_overhead = TRUE) THEN
    RAISE EXCEPTION 'V52: ejercicio 581 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 592 dumbbell incline row: instructions_es: R2 L03 (ALTA): El texto dice «siéntate en el banco con el pecho apoyado»; en el GIF no está sentado: las piernas están estiradas detrás con los pies en el suelo y el tronco ap / V3 V03 ACEPTAR: f1-f12 pecho apoyado en el respaldo inclinado, piernas estiradas detrás y pies en el suelo, no sentado; f1 brazos extendidos, f7 codos abiertos hacia los lados 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ajusta un banco a una inclinación moderada. Apoya el pecho en el respaldo con las piernas estiradas detrás y los pies firmes en el suelo. Deja colgar una mancuerna en cada mano con los brazos extendidos. Tira de las mancuernas hacia arriba llevando los codos hacia los lados y atrás, juntando los omóplatos. Baja despacio hasta extender los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ajusta un banco a una inclinación moderada.", "Apoya el pecho en el respaldo con las piernas estiradas detrás y los pies firmes en el suelo.", "Deja colgar una mancuerna en cada mano con los brazos extendidos.", "Tira de las mancuernas hacia arriba llevando los codos hacia los lados y atrás, juntando los omóplatos.", "Baja despacio hasta extender los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 592 AND name_en = 'dumbbell incline row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '68c36bfe966ad771c117c8160812b209';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 592 AND name_en = 'dumbbell incline row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '56682907958468d97d007d2f1d81b014') THEN
    RAISE EXCEPTION 'V52: ejercicio 592 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 594 dumbbell incline y-raise: instructions_es: R2 L05 (ALTA): El texto dice sentarse en el banco inclinándose un poco hacia delante y subir los brazos hasta quedar paralelos al suelo; el GIF muestra el pecho apoyado boca a / V3 V03 ACEPTAR: f1-f12 pecho apoyado boca abajo en el respaldo inclinado con los pies en el suelo; f1 brazos colgando, f7 brazos elevados en Y en línea con el tronco; el texto 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ajusta un banco inclinado y túmbate boca abajo con el pecho apoyado en el respaldo y los pies en el suelo. Sujeta una mancuerna en cada mano y deja los brazos colgando con los codos ligeramente flexionados. Eleva los brazos hacia delante y hacia fuera formando una Y hasta que queden en línea con el tronco. Bájalos despacio hasta la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ajusta un banco inclinado y túmbate boca abajo con el pecho apoyado en el respaldo y los pies en el suelo.", "Sujeta una mancuerna en cada mano y deja los brazos colgando con los codos ligeramente flexionados.", "Eleva los brazos hacia delante y hacia fuera formando una Y hasta que queden en línea con el tronco.", "Bájalos despacio hasta la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 594 AND name_en = 'dumbbell incline y-raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5eff0066cde9c64437fcd3f799f6c0ca';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 594 AND name_en = 'dumbbell incline y-raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd57e08ecdc8d84ec6ae09cde66d116ac') THEN
    RAISE EXCEPTION 'V52: ejercicio 594 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 597 dumbbell incline curl: instructions_es: R2 L08 (MEDIA): El texto describe una flexión simultánea de ambas mancuernas con la parte superior de los brazos apoyada en el banco; el GIF muestra un curl alterno con los bra / V3 V04 ACEPTAR: f1 mancuerna izquierda (lado del espectador) a la altura del hombro; f7 brazo izquierdo extendido colgando junto al banco y el derecho flexionado: curl alterno 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ajusta un banco inclinado y siéntate con la espalda apoyada, con una mancuerna en cada mano. Deja que los brazos cuelguen extendidos a los lados del banco. Sin mover la parte superior del brazo, flexiona un codo y sube la mancuerna hacia el hombro. Bájala con control hasta extender el brazo mientras subes la otra. Alterna los lados. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ajusta un banco inclinado y siéntate con la espalda apoyada, con una mancuerna en cada mano.", "Deja que los brazos cuelguen extendidos a los lados del banco.", "Sin mover la parte superior del brazo, flexiona un codo y sube la mancuerna hacia el hombro.", "Bájala con control hasta extender el brazo mientras subes la otra.", "Alterna los lados.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 597 AND name_en = 'dumbbell incline curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a3818c24ff493520c5fb156109a3df0a';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 597 AND name_en = 'dumbbell incline curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '09564304dab9a15062539849ed83199b') THEN
    RAISE EXCEPTION 'V52: ejercicio 597 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 607 dumbbell kickbacks on exercise ball: instructions_es: R2 L18 (MEDIA): El texto dice brazos superiores junto al cuerpo con codos flexionados y extender los codos; el GIF muestra los brazos rectos que pasan de colgar hacia el suelo  / V3 W03 ACEPTAR: f1 de rodillas con el tronco boca abajo sobre el fitball y los brazos rectos colgando; f7 brazos rectos atrás junto al tronco; el codo no se flexiona ni se exti | movement_pattern: R2 L18 (MEDIA): Es una extensión de hombro con brazos rectos, no una patada de tríceps; el GIF resalta deltoides posterior y espalda alta. El objetivo tríceps solo encaja por l / V3 W03 ACEPTAR: f1 a f7 los brazos van rectos desde colgar hasta atrás junto al tronco sin movimiento del codo: extensión de hombro
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Arrodíllate en el suelo y apoya el tronco boca abajo sobre un fitball. Sujeta una mancuerna en cada mano con los brazos rectos colgando hacia el suelo. Lleva los brazos rectos hacia atrás hasta que queden junto al tronco. Vuelve con control a la posición inicial sin doblar los codos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Arrodíllate en el suelo y apoya el tronco boca abajo sobre un fitball.", "Sujeta una mancuerna en cada mano con los brazos rectos colgando hacia el suelo.", "Lleva los brazos rectos hacia atrás hasta que queden junto al tronco.", "Vuelve con control a la posición inicial sin doblar los codos.", "Repite el número de repeticiones indicado."]'::jsonb, movement_pattern = 'SHOULDER_EXTENSION'
   WHERE exercise_id = 607 AND name_en = 'dumbbell kickbacks on exercise ball' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '95c7c56b588462e3e9267429c42cb114' AND movement_pattern = 'ELBOW_EXTENSION';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 607 AND name_en = 'dumbbell kickbacks on exercise ball' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a25fb2511997c67ceba745f4ab0c3d15' AND movement_pattern = 'SHOULDER_EXTENSION') THEN
    RAISE EXCEPTION 'V52: ejercicio 607 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 608 dumbbell kneeling bicep curl exercise ball: instructions_es: R2 L19 (MEDIA): El texto dice arrodillarse en el suelo con el balón delante y apoyar los codos en él; el GIF muestra a la persona arrodillada encima del balón, con el tronco er / V3 Y01 ACEPTAR: f1-f12 arrodillado encima del balón con el tronco erguido y los brazos libres; f1 mancuernas junto a los muslos, f7 a la altura de los hombros; no hay codos apo | difficulty_level: R2 L19 (MEDIA): equilibrio exigente de rodillas sobre superficie inestable / V3 Y01 ACEPTAR: f1-f12 de rodillas sobre un balón de estabilidad con mancuernas y sin apoyo de manos: equilibrio exigente sobre superficie inestable
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate con cuidado de rodillas sobre un balón de estabilidad, cerca de un apoyo fijo, con el tronco erguido. Sujeta una mancuerna en cada mano con los brazos estirados y las palmas hacia delante. Flexiona los codos y sube las mancuernas hacia los hombros sin mover los brazos superiores. Baja las mancuernas con control hasta estirar los brazos manteniendo el equilibrio. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate con cuidado de rodillas sobre un balón de estabilidad, cerca de un apoyo fijo, con el tronco erguido.", "Sujeta una mancuerna en cada mano con los brazos estirados y las palmas hacia delante.", "Flexiona los codos y sube las mancuernas hacia los hombros sin mover los brazos superiores.", "Baja las mancuernas con control hasta estirar los brazos manteniendo el equilibrio.", "Repite el número de repeticiones indicado."]'::jsonb, difficulty_level = 2
   WHERE exercise_id = 608 AND name_en = 'dumbbell kneeling bicep curl exercise ball' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e5a403c59f9d5ea16ecff5ee1663e107' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 608 AND name_en = 'dumbbell kneeling bicep curl exercise ball' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '8caa8494874fc7941839ac4f51f780f6' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 608 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 612 dumbbell lying elbow press: instructions_es: R2 L03 (ALTA): El texto pide bajar las mancuernas hacia los hombros manteniendo quietos los brazos superiores; en el GIF los brazos superiores se mueven (los codos bajan junto / V3 V04 ACEPTAR: f1 mancuernas sobre el pecho con los codos pegados al tronco por debajo del banco; f7 brazos verticales sobre el pecho; el brazo superior se desplaza, contradic | movement_pattern: R2 L03 (ALTA): El movimiento es un press (flexión de hombro con extensión de codo), no una extensión aislada de codo; en el GIF se resalta sobre todo el pectoral, por eso la z / V3 V04 ACEPTAR: f1-f7 el brazo pasa de junto al tronco a vertical con extensión de codo: press cerrado, no extensión aislada de codo; resaltado pectoral
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en un banco plano con los pies apoyados en el suelo y una mancuerna en cada mano, con las palmas enfrentadas. Extiende los brazos en vertical sobre el pecho. Flexiona los codos pegándolos al tronco y baja las mancuernas hasta el pecho. Empuja las mancuernas hacia arriba hasta extender los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en un banco plano con los pies apoyados en el suelo y una mancuerna en cada mano, con las palmas enfrentadas.", "Extiende los brazos en vertical sobre el pecho.", "Flexiona los codos pegándolos al tronco y baja las mancuernas hasta el pecho.", "Empuja las mancuernas hacia arriba hasta extender los brazos.", "Repite el número de repeticiones indicado."]'::jsonb, movement_pattern = 'HORIZONTAL_PUSH'
   WHERE exercise_id = 612 AND name_en = 'dumbbell lying elbow press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'add035ff638af6f1cda42b21780845c1' AND movement_pattern = 'ELBOW_EXTENSION';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 612 AND name_en = 'dumbbell lying elbow press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '6e5fef51352a7eb4fb14d8d60de84b2d' AND movement_pattern = 'HORIZONTAL_PUSH') THEN
    RAISE EXCEPTION 'V52: ejercicio 612 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 630 dumbbell one arm decline chest press: instructions_es: R2 L01 (ALTA): El texto pide colocar los pies planos sobre el suelo; el GIF muestra las piernas enganchadas en los rodillos del banco declinado, sin pies en el suelo / V3 V04 ACEPTAR: f1-f12 banco declinado con la cabeza en el extremo bajo y las piernas enganchadas en los rodillos, sin pies en el suelo; f1 mancuerna junto al pecho, f7 brazo e
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate en un banco declinado con la cabeza en el extremo bajo y engancha las piernas en los rodillos del extremo alto. Sujeta una mancuerna con una mano junto al pecho y mantén la espalda apoyada en el banco. Empuja la mancuerna hacia arriba hasta extender el brazo por completo. Bájala despacio hasta el lateral del pecho. Completa las repeticiones y cambia de brazo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate en un banco declinado con la cabeza en el extremo bajo y engancha las piernas en los rodillos del extremo alto.", "Sujeta una mancuerna con una mano junto al pecho y mantén la espalda apoyada en el banco.", "Empuja la mancuerna hacia arriba hasta extender el brazo por completo.", "Bájala despacio hasta el lateral del pecho.", "Completa las repeticiones y cambia de brazo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 630 AND name_en = 'dumbbell one arm decline chest press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'f74daf4ba4cdaea489e08b5bbed18cc0';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 630 AND name_en = 'dumbbell one arm decline chest press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '795dea683ff0b347d75efb307f3dd274') THEN
    RAISE EXCEPTION 'V52: ejercicio 630 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 635 dumbbell lying rear delt row: instructions_es: R2 L06 (ALTA): El texto pide tumbarse en un banco plano; en el GIF el banco está inclinado y los pies apoyan en el suelo por detrás. / V3 V04 ACEPTAR: f1-f12 pecho apoyado en banco inclinado, pies apoyados en el suelo por detrás; f1 brazos colgando, f7 codos hacia fuera y atrás; el texto pide banco plano
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Apóyate boca abajo sobre un banco inclinado, con los pies apoyados en el suelo por detrás y una mancuerna en cada mano. Deja los brazos extendidos hacia el suelo. Tira de las mancuernas hacia arriba llevando los codos hacia fuera y atrás, juntando los omóplatos. Baja las mancuernas con control hasta extender los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Apóyate boca abajo sobre un banco inclinado, con los pies apoyados en el suelo por detrás y una mancuerna en cada mano.", "Deja los brazos extendidos hacia el suelo.", "Tira de las mancuernas hacia arriba llevando los codos hacia fuera y atrás, juntando los omóplatos.", "Baja las mancuernas con control hasta extender los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 635 AND name_en = 'dumbbell lying rear delt row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b1b8d414081614e7a556ce0421463b99';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 635 AND name_en = 'dumbbell lying rear delt row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b89fc5f86ad5b06c18ff3d031276c6ce') THEN
    RAISE EXCEPTION 'V52: ejercicio 635 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 636 dumbbell one arm bent-over row: requires_kneeling: R2 L07 (ALTA): pies se anota AMBOS_SIEMPRE por el pie que permanece en el suelo. La rodilla carga peso sobre el banco: falta KN. Remo apoyado en banco, dificultad 3 excesiva;  / V3 V04 ACEPTAR: f1-f12 rodilla y espinilla apoyadas sobre el banco cargando peso junto a la mano del mismo lado; el otro pie en el suelo | instructions_es: R2 L07 (ALTA): El texto describe un remo de pie inclinado sin banco; el GIF muestra una rodilla y una mano apoyadas en el banco y solo un pie en el suelo / V3 V04 ACEPTAR: f1-f12 rodilla y mano del mismo lado apoyadas en banco plano, otro pie en el suelo; f1 brazo extendido, f7 mancuerna junto al costado; el texto describe remo de
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_kneeling = TRUE, instructions_es = 'Apoya una rodilla y la mano del mismo lado sobre un banco plano, con el otro pie firme en el suelo. Sujeta una mancuerna con la mano libre y deja el brazo extendido hacia el suelo, con la espalda recta. Tira de la mancuerna hacia el costado manteniendo el codo cerca del cuerpo. Haz una pausa breve arriba y baja con control hasta extender el brazo. Completa la serie y cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Apoya una rodilla y la mano del mismo lado sobre un banco plano, con el otro pie firme en el suelo.", "Sujeta una mancuerna con la mano libre y deja el brazo extendido hacia el suelo, con la espalda recta.", "Tira de la mancuerna hacia el costado manteniendo el codo cerca del cuerpo.", "Haz una pausa breve arriba y baja con control hasta extender el brazo.", "Completa la serie y cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 636 AND name_en = 'dumbbell one arm bent-over row' AND requires_kneeling = FALSE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'c10eb01ebbe5deb9d0450732618edc4b';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 636 AND name_en = 'dumbbell one arm bent-over row' AND requires_kneeling = TRUE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '06acf277eaab8722a0a3f5262422cbcc') THEN
    RAISE EXCEPTION 'V52: ejercicio 636 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 640 dumbbell lying single extension: instructions_es: R2 L11 (ALTA): Dice sujetar la mancuerna con una mano y cambiar de brazo; el GIF muestra una sola mancuerna sujeta con las dos manos y ambos tríceps trabajando a la vez / V3 W03 ACEPTAR: f1 una sola mancuerna sujeta con las dos manos junto a la cabeza; f7 los dos brazos extendidos sobre el pecho sujetando la misma mancuerna
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en un banco plano con los pies apoyados en el suelo. Sujeta una mancuerna con las dos manos y extiende los brazos por encima del pecho. Sin mover la parte superior de los brazos, flexiona los codos y baja la mancuerna con control hasta junto a la cabeza. Extiende los codos para volver a la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en un banco plano con los pies apoyados en el suelo.", "Sujeta una mancuerna con las dos manos y extiende los brazos por encima del pecho.", "Sin mover la parte superior de los brazos, flexiona los codos y baja la mancuerna con control hasta junto a la cabeza.", "Extiende los codos para volver a la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 640 AND name_en = 'dumbbell lying single extension' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '9426214170fb636c32476076a66d0a0b';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 640 AND name_en = 'dumbbell lying single extension' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '41a125d04083296c5883513c7b553240') THEN
    RAISE EXCEPTION 'V52: ejercicio 640 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 650 dumbbell one arm kickback: instructions_es: R2 L01 (ALTA): El texto pide los pies a la anchura de los hombros; el GIF muestra una postura de zancada con un pie muy atrasado y la mano libre apoyada en la rodilla delanter / V3 V04 ACEPTAR: f1-f12 postura de zancada con un pie muy atrasado, tronco inclinado y mano libre sobre la rodilla delantera (f7); f1 codo a 90 grados, f7 brazo extendido atrás
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con un pie adelantado y el otro atrasado, y sujeta una mancuerna con una mano. Inclina el tronco hacia delante con la espalda recta y apoya la mano libre sobre la rodilla delantera. Pega el codo al costado con el brazo flexionado a 90 grados. Extiende el codo llevando la mancuerna hacia atrás hasta estirar el brazo y vuelve despacio. Completa las repeticiones y cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con un pie adelantado y el otro atrasado, y sujeta una mancuerna con una mano.", "Inclina el tronco hacia delante con la espalda recta y apoya la mano libre sobre la rodilla delantera.", "Pega el codo al costado con el brazo flexionado a 90 grados.", "Extiende el codo llevando la mancuerna hacia atrás hasta estirar el brazo y vuelve despacio.", "Completa las repeticiones y cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 650 AND name_en = 'dumbbell one arm kickback' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '25aa6a2e2adb78a9fd8068d7ba0f9769';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 650 AND name_en = 'dumbbell one arm kickback' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a6c9229883c4bae99347f5765d5e79f2') THEN
    RAISE EXCEPTION 'V52: ejercicio 650 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 651 dumbbell one arm prone curl: instructions_es: R2 L02 (MEDIA): El texto indica un banco plano; el GIF muestra un banco inclinado con el pecho más alto que la cadera y la otra mano sujetándose al banco / V3 V04 ACEPTAR: f1-f12 pecho apoyado en banco inclinado (cabeza en el extremo alto), pies en el suelo al pie del banco, mano libre sujeta al banco; f1 brazo colgando, f7 mancue
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca abajo sobre un banco inclinado con el pecho apoyado y los pies en el suelo. Sujeta una mancuerna con una mano y deja el brazo colgando recto hacia el suelo; apoya la otra mano en el banco. Flexiona el codo y sube la mancuerna hacia el hombro sin mover el brazo superior. Baja la mancuerna de forma controlada hasta extender el brazo. Completa las repeticiones y cambia de brazo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca abajo sobre un banco inclinado con el pecho apoyado y los pies en el suelo.", "Sujeta una mancuerna con una mano y deja el brazo colgando recto hacia el suelo; apoya la otra mano en el banco.", "Flexiona el codo y sube la mancuerna hacia el hombro sin mover el brazo superior.", "Baja la mancuerna de forma controlada hasta extender el brazo.", "Completa las repeticiones y cambia de brazo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 651 AND name_en = 'dumbbell one arm prone curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '4d2dfbbc3cd7bc901d2058d659d01b23';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 651 AND name_en = 'dumbbell one arm prone curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e02f0874c8ddcc01bc4ff20c063ab80e') THEN
    RAISE EXCEPTION 'V52: ejercicio 651 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 652 dumbbell one arm prone hammer curl: instructions_es: R2 L03 (ALTA): El texto dice «Túmbate boca abajo en un banco plano»; en el GIF el banco está inclinado, con la cabeza en el extremo alto / V3 V04 ACEPTAR: f1-f12 banco inclinado con la cabeza en el extremo alto, mano libre en el banco; f1 brazo colgando con agarre neutro, f7 mancuerna junto al hombro
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ajusta un banco con una inclinación moderada y túmbate boca abajo sobre el respaldo con una mancuerna en una mano y deja el brazo colgando, con la palma hacia el cuerpo. Apoya la otra mano en el banco para estabilizarte. Sin mover el brazo superior, flexiona el codo y lleva la mancuerna hacia el hombro. Baja despacio hasta extender el brazo. Cambia de brazo al terminar la serie. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ajusta un banco con una inclinación moderada y túmbate boca abajo sobre el respaldo con una mancuerna en una mano y deja el brazo colgando, con la palma hacia el cuerpo.", "Apoya la otra mano en el banco para estabilizarte.", "Sin mover el brazo superior, flexiona el codo y lleva la mancuerna hacia el hombro.", "Baja despacio hasta extender el brazo.", "Cambia de brazo al terminar la serie.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 652 AND name_en = 'dumbbell one arm prone hammer curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'dea7919b094f07dc3f0127e1a6ddc663';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 652 AND name_en = 'dumbbell one arm prone hammer curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '970305d867313c3735cff8020b114c24') THEN
    RAISE EXCEPTION 'V52: ejercicio 652 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 665 dumbbell raise: instructions_es: R2 L16 (ALTA): El texto dice subir los brazos con una ligera flexión de codos; el GIF muestra los codos flexionados unos 90 grados, con los antebrazos colgando, y la parte sup / V3 W03 ACEPTAR: f1 brazos junto al cuerpo; f7 codos flexionados unos 90 grados con la parte superior del brazo horizontal y las mancuernas a la altura del pecho; el texto actua
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies separados a la altura de los hombros y una mancuerna en cada mano a los lados del cuerpo. Mantén la espalda recta y activa el core. Eleva los brazos hacia los lados doblando los codos hasta que la parte superior del brazo quede paralela al suelo, con las mancuernas a la altura del pecho. Baja los brazos con control hasta la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies separados a la altura de los hombros y una mancuerna en cada mano a los lados del cuerpo.", "Mantén la espalda recta y activa el core.", "Eleva los brazos hacia los lados doblando los codos hasta que la parte superior del brazo quede paralela al suelo, con las mancuernas a la altura del pecho.", "Baja los brazos con control hasta la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 665 AND name_en = 'dumbbell raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '9a0b318e3c4b11b5a47996d18b14ae47';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 665 AND name_en = 'dumbbell raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ab2821740b1c1b5fcfaa836bb59c65da') THEN
    RAISE EXCEPTION 'V52: ejercicio 665 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 666 dumbbell rear delt raise: name_es: R2 L17 (ALTA): «Espaldilla» no es un término anatómico adecuado (designa la paletilla de un animal); el GIF trabaja el deltoides posterior. / V3 W03 ACEPTAR: f1 y f7 vista posterior: de pie erguido, codos flexionados que suben hacia fuera y atrás hasta casi la altura de los hombros; «espaldilla» significa omóplato y 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Elevación de deltoides posterior con mancuerna de pie'
   WHERE exercise_id = 666 AND name_en = 'dumbbell rear delt raise' AND name_es = 'Elevación de espaldilla con mancuerna';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 666 AND name_en = 'dumbbell rear delt raise' AND name_es = 'Elevación de deltoides posterior con mancuerna de pie') THEN
    RAISE EXCEPTION 'V52: ejercicio 666 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 669 dumbbell press on exercise ball: instructions_es: R2 L20 (ALTA): El texto dice rodar hasta que la zona lumbar quede apoyada en el balón; el GIF muestra apoyadas la parte alta de la espalda y los hombros, con la cadera elevada / V3 W03 ACEPTAR: f1 y f7 parte alta de la espalda y hombros apoyados en el balón, cadera elevada en línea con las rodillas flexionadas a unos 90 grados; el texto actual apoya la
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate sobre un balón de estabilidad con una mancuerna en cada mano y los pies apoyados en el suelo. Avanza con los pies haciendo rodar el balón hasta que la parte alta de la espalda y los hombros queden apoyados en él, con la cadera elevada y las rodillas flexionadas a unos 90°. Sostén las mancuernas a la altura del pecho, con los codos flexionados. Empuja las mancuernas hacia arriba hasta extender los brazos y bájalas despacio a la posición inicial manteniendo la cadera elevada. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate sobre un balón de estabilidad con una mancuerna en cada mano y los pies apoyados en el suelo.", "Avanza con los pies haciendo rodar el balón hasta que la parte alta de la espalda y los hombros queden apoyados en él, con la cadera elevada y las rodillas flexionadas a unos 90°.", "Sostén las mancuernas a la altura del pecho, con los codos flexionados.", "Empuja las mancuernas hacia arriba hasta extender los brazos y bájalas despacio a la posición inicial manteniendo la cadera elevada.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 669 AND name_en = 'dumbbell press on exercise ball' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ccb377341a39ab63867598a8679ed5f2';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 669 AND name_en = 'dumbbell press on exercise ball' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '6e8d3e5a05b09f0e2718d16d3e775cb2') THEN
    RAISE EXCEPTION 'V52: ejercicio 669 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 671 dumbbell pullover hip extension on exercise ball: instructions_es: R2 L02 (MEDIA): El texto añade una fase de subir y bajar la cadera del suelo; en el GIF la cadera permanece elevada en puente durante todo el movimiento y solo se mueven los br / V3 V04 MEJORAR: f1 brazos verticales sobre el pecho con la cadera baja; f7 mancuerna detrás de la cabeza con la cadera elevada alineada con el tronco: la cadera sube y baja a l
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate sobre un balón de estabilidad con una mancuerna y camina con los pies hacia delante hasta apoyar la parte alta de la espalda y la cabeza en el balón, con los pies en el suelo y la cadera baja. Sujeta la mancuerna con ambas manos y los brazos extendidos sobre el pecho. Baja la mancuerna por detrás de la cabeza con los codos ligeramente flexionados mientras elevas la cadera hasta alinear muslos y tronco. Vuelve a llevar la mancuerna sobre el pecho de forma controlada mientras bajas la cadera. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate sobre un balón de estabilidad con una mancuerna y camina con los pies hacia delante hasta apoyar la parte alta de la espalda y la cabeza en el balón, con los pies en el suelo y la cadera baja.", "Sujeta la mancuerna con ambas manos y los brazos extendidos sobre el pecho.", "Baja la mancuerna por detrás de la cabeza con los codos ligeramente flexionados mientras elevas la cadera hasta alinear muslos y tronco.", "Vuelve a llevar la mancuerna sobre el pecho de forma controlada mientras bajas la cadera.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 671 AND name_en = 'dumbbell pullover hip extension on exercise ball' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e60af924f3071c0dec18ca4d15085def';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 671 AND name_en = 'dumbbell pullover hip extension on exercise ball' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '89a4c9007eadc9fd628cb2d0c31c945c') THEN
    RAISE EXCEPTION 'V52: ejercicio 671 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 675 dumbbell plyo squat: high_impact: R2 L06 (MEDIA): Ningún fotograma muestra los pies en el aire: el impacto observado es BAJO frente al ALTO derivado. Si se mantiene el nombre pliométrico habría que cambiar el G / V3 V04 ACEPTAR: f1-f12 fundido entre de pie y sentadilla profunda con los pies fijos en el suelo en todos los fotogramas; no hay vuelo ni salto / coordinador: el GIF es una sentadilla sumo sin vuelo (R2, V3 V04 y X01); el nombre y las instrucciones pasan a sentadilla sumo en V52, asi que ninguna parte del ejercicio implica salto | name_es: R2 L06 (MEDIA): Ningún fotograma muestra los pies en el aire: el impacto observado es BAJO frente al ALTO derivado. Si se mantiene el nombre pliométrico habría que cambiar el G / V3 V04 ACEPTAR: f1 de pie con pies muy separados y puntas hacia fuera, una mancuerna en vertical sujeta con ambas manos entre las piernas; f7 sentadilla con muslos cerca de la  | instructions_es: R2 L06 (MEDIA): El texto describe un salto explosivo con cambio de pies en el aire y una mancuerna en cada mano; el GIF muestra una sentadilla sumo con una sola mancuerna sujet / V3 V04 ACEPTAR: f1-f12 sentadilla sumo con una sola mancuerna sujeta con ambas manos, sin vuelo ni cambio de pies; el texto actual describe salto con cambio de pies y una mancu | impact_level: R2 L06 (MEDIA): pies=AMBOS_SIEMPRE; De pie con los pies muy separados y una mancuerna sujeta con ambas manos entre las piernas, baja a una sentadilla sumo profunda y vuelve a s / V3 X01 ACEPTAR: f1-f12 fundido entre dos poses (f1 de pie, f7 sentadilla sumo profunda con una mancuerna entre las piernas); en la ampliación los pies no se mueven ni un píxel  / coordinador: pies fijos en f1-f12 (V3 X01) y nombre corregido
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET high_impact = FALSE, name_es = 'Sentadilla sumo con mancuerna', instructions_es = 'Ponte de pie con los pies bien separados y las puntas hacia fuera, sujetando una mancuerna con ambas manos entre las piernas. Baja en sentadilla flexionando rodillas y caderas, con el pecho erguido y las rodillas alineadas con los pies. Baja hasta que los muslos queden cerca de la horizontal. Empuja con los pies para volver a la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies bien separados y las puntas hacia fuera, sujetando una mancuerna con ambas manos entre las piernas.", "Baja en sentadilla flexionando rodillas y caderas, con el pecho erguido y las rodillas alineadas con los pies.", "Baja hasta que los muslos queden cerca de la horizontal.", "Empuja con los pies para volver a la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb, impact_level = 'BAJO'
   WHERE exercise_id = 675 AND name_en = 'dumbbell plyo squat' AND high_impact = TRUE AND name_es = 'Sentadilla pliométrica con mancuerna' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e62d286189c605b4b1d37a2a97c0bd75' AND impact_level = 'ALTO';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 675 AND name_en = 'dumbbell plyo squat' AND high_impact = FALSE AND name_es = 'Sentadilla sumo con mancuerna' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b0a518819043c0b3335d9aa3a08c4b96' AND impact_level = 'BAJO') THEN
    RAISE EXCEPTION 'V52: ejercicio 675 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 681 dumbbell one arm standing hammer curl: instructions_es: R2 L12 (ALTA): El texto habla de una mancuerna en cada mano y de flexionar las pesas; el GIF muestra una sola mancuerna en una mano / V3 W03 ACEPTAR: f1-f12 una sola mancuerna en una mano y la otra mano vacía; f7 sube con agarre neutro hasta el hombro; el texto actual habla de una mancuerna en cada mano
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies separados a la anchura de las caderas y sujeta una mancuerna en una mano, con la palma mirando hacia el cuerpo. Mantén el codo pegado al costado y el brazo quieto. Flexiona el codo y sube la mancuerna hacia el hombro sin girar la muñeca. Baja la mancuerna con control hasta extender el brazo. Repite el número de repeticiones indicado y después cambia de brazo.', instruction_steps_es = '["Ponte de pie con los pies separados a la anchura de las caderas y sujeta una mancuerna en una mano, con la palma mirando hacia el cuerpo.", "Mantén el codo pegado al costado y el brazo quieto.", "Flexiona el codo y sube la mancuerna hacia el hombro sin girar la muñeca.", "Baja la mancuerna con control hasta extender el brazo.", "Repite el número de repeticiones indicado y después cambia de brazo."]'::jsonb
   WHERE exercise_id = 681 AND name_en = 'dumbbell one arm standing hammer curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '94445743b6a1e11e324c4e1a3099de3d';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 681 AND name_en = 'dumbbell one arm standing hammer curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '7c26229cac16c545bc411f7abe296f18') THEN
    RAISE EXCEPTION 'V52: ejercicio 681 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 682 dumbbell one arm triceps extension (on bench): instructions_es: R2 L13 (ALTA): El texto dice apoyar la mano libre sobre el banco y el codo apuntando hacia adelante; el GIF muestra la mano libre en la cadera y el codo apuntando hacia arriba / V3 W03 ACEPTAR: f1 mano libre apoyada en la cadera; f1 codo flexionado apuntando hacia arriba con la mancuerna detrás de la cabeza; f7 brazo extendido sobre la cabeza; el texto
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en un banco con la espalda recta y los pies apoyados en el suelo. Sujeta una mancuerna con una mano y extiende ese brazo por encima de la cabeza; coloca la otra mano en la cadera. Flexiona el codo y baja la mancuerna por detrás de la cabeza, con el codo apuntando hacia arriba y el brazo quieto. Extiende el codo para volver a subir la mancuerna. Completa las repeticiones y cambia de brazo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en un banco con la espalda recta y los pies apoyados en el suelo.", "Sujeta una mancuerna con una mano y extiende ese brazo por encima de la cabeza; coloca la otra mano en la cadera.", "Flexiona el codo y baja la mancuerna por detrás de la cabeza, con el codo apuntando hacia arriba y el brazo quieto.", "Extiende el codo para volver a subir la mancuerna.", "Completa las repeticiones y cambia de brazo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 682 AND name_en = 'dumbbell one arm triceps extension (on bench)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '0b392f85c08dd4af4b4a077772102155';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 682 AND name_en = 'dumbbell one arm triceps extension (on bench)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '2a908b0781f4f7ef8d95144728679ddb') THEN
    RAISE EXCEPTION 'V52: ejercicio 682 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 688 dumbbell preacher curl: instructions_es: R2 L19 (ALTA): El texto dice sostener una mancuerna en cada mano y trabajar con los dos brazos; el GIF muestra un solo brazo con mancuerna y la otra mano apoyada / V3 Y01 ACEPTAR: f1-f12 una sola mancuerna en la mano derecha con el brazo sobre la almohadilla; la mano izquierda descansa sobre el muslo o el borde de la almohadilla; f7 mancu
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en un banco Scott con la parte superior de un brazo apoyada en la almohadilla y el pecho contra ella. Sujeta una mancuerna con la palma hacia arriba y el brazo casi estirado. Sin despegar el brazo de la almohadilla, sube la mancuerna hacia el hombro contrayendo el bíceps. Bájala con control hasta casi estirar el brazo. Completa las repeticiones y cambia de brazo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en un banco Scott con la parte superior de un brazo apoyada en la almohadilla y el pecho contra ella.", "Sujeta una mancuerna con la palma hacia arriba y el brazo casi estirado.", "Sin despegar el brazo de la almohadilla, sube la mancuerna hacia el hombro contrayendo el bíceps.", "Bájala con control hasta casi estirar el brazo.", "Completa las repeticiones y cambia de brazo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 688 AND name_en = 'dumbbell preacher curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'dc76b0bdaee2cf12499e63993b7b5a1f';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 688 AND name_en = 'dumbbell preacher curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3e5d7a20e09341cbc36534cdc0c26c2f') THEN
    RAISE EXCEPTION 'V52: ejercicio 688 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 695 dumbbell seated calf raise: name_es: R2 L06 (ALTA): Nombre sin tilde en «Elevación» y en singular aunque se usan dos mancuernas. El disco bajo los pies aparece en el GIF pero no es imprescindible. / V3 V04 ACEPTAR: f1-f12 sentado con dos mancuernas en vertical sobre las rodillas; el nombre actual carece de tilde y va en singular | instructions_es: R2 L06 (ALTA): El texto habla de una sola mancuerna sobre los muslos y de elevar el peso del cuerpo; el GIF muestra dos mancuernas apoyadas en vertical sobre las rodillas y la / V3 V04 ACEPTAR: f1-f12 dos mancuernas en vertical sobre las rodillas, puntas de los pies sobre un disco, sentado; el texto habla de una sola mancuerna sobre los muslos y de ele
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Elevación de talones sentado con mancuernas', instructions_es = 'Siéntate en un banco con una mancuerna en cada mano, apoyadas en vertical sobre las rodillas. Coloca la parte delantera de los pies sobre una superficie elevada, como un disco. Eleva los talones lo más alto que puedas. Bájalos despacio a la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en un banco con una mancuerna en cada mano, apoyadas en vertical sobre las rodillas.", "Coloca la parte delantera de los pies sobre una superficie elevada, como un disco.", "Eleva los talones lo más alto que puedas.", "Bájalos despacio a la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 695 AND name_en = 'dumbbell seated calf raise' AND name_es = 'Elevacion de talones sentado con mancuerna' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '567de921d41c8d578edc5a80cc65ce44';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 695 AND name_en = 'dumbbell seated calf raise' AND name_es = 'Elevación de talones sentado con mancuernas' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '18715f6cc37093b8417de69c4c627d1c') THEN
    RAISE EXCEPTION 'V52: ejercicio 695 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 699 dumbbell rear lateral raise (support head): instructions_es: R2 L10 (ALTA): El texto describe una elevación inclinada libre sin mencionar el apoyo; el GIF muestra la frente apoyada sobre el extremo de un banco inclinado, que es lo que d / V3 V04 ACEPTAR: f1-f12 de pie detrás del extremo alto de un banco inclinado con la frente apoyada en él; f1 brazos colgando, f7 brazos abiertos a la altura de los hombros; el t
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca un banco en inclinación y ponte de pie detrás de su extremo alto con una mancuerna en cada mano. Inclínate desde las caderas con la espalda recta hasta apoyar la frente en el borde superior del banco, con los brazos colgando. Con los codos ligeramente flexionados, eleva los brazos hacia los lados hasta la altura de los hombros. Baja con control hasta la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca un banco en inclinación y ponte de pie detrás de su extremo alto con una mancuerna en cada mano.", "Inclínate desde las caderas con la espalda recta hasta apoyar la frente en el borde superior del banco, con los brazos colgando.", "Con los codos ligeramente flexionados, eleva los brazos hacia los lados hasta la altura de los hombros.", "Baja con control hasta la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 699 AND name_en = 'dumbbell rear lateral raise (support head)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'cf68d55a1149afd921d6376949da6190';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 699 AND name_en = 'dumbbell rear lateral raise (support head)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5fbb0fa80799d1c243965a1f1eeea0ba') THEN
    RAISE EXCEPTION 'V52: ejercicio 699 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 705 dumbbell seated alternate shoulder: axial_load: R2 L16 (ALTA): Press de mancuernas por encima de la cabeza: según la definición (cargas en press por encima de la cabeza) corresponde AX, igual que en otros press de hombro de / V3 W03 ACEPTAR: f7 y f19 una mancuerna empujada por encima de la cabeza en press sentado: cumple la definición literal de AX (cargas en press por encima de la cabeza) | instructions_es: R2 L16 (ALTA): El texto pide palmas hacia adentro y bajar una mancuerna mientras se sube la otra a la vez; el GIF muestra palmas hacia delante y un brazo cada vez, con ambas m / V3 W03 ACEPTAR: f1 y f13 ambas mancuernas en los hombros con las palmas hacia delante; f7 sube solo la izquierda y f19 solo la derecha, la otra quieta; banco con respaldo visib
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET axial_load = TRUE, instructions_es = 'Siéntate en un banco con respaldo, con la espalda apoyada y los pies en el suelo. Sube una mancuerna en cada mano hasta la altura de los hombros, con las palmas hacia delante. Empuja una mancuerna hacia arriba hasta extender el brazo por encima de la cabeza, sin mover la otra. Bájala con control hasta el hombro y repite con el otro brazo. Alterna los lados. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en un banco con respaldo, con la espalda apoyada y los pies en el suelo.", "Sube una mancuerna en cada mano hasta la altura de los hombros, con las palmas hacia delante.", "Empuja una mancuerna hacia arriba hasta extender el brazo por encima de la cabeza, sin mover la otra.", "Bájala con control hasta el hombro y repite con el otro brazo.", "Alterna los lados.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 705 AND name_en = 'dumbbell seated alternate shoulder' AND axial_load = FALSE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '71d3f8b20fc00bd55f6184889f1d8230';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 705 AND name_en = 'dumbbell seated alternate shoulder' AND axial_load = TRUE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'c0f3ba0eedca4d23bbebe90d38a927a4') THEN
    RAISE EXCEPTION 'V52: ejercicio 705 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 706 dumbbell seated bent arm lateral raise: instructions_es: R2 L17 (MEDIA): El texto pide la espalda recta y los codos flexionados a 90° durante todo el movimiento; el GIF parte con el tronco inclinado hacia delante y las mancuernas jun / V3 W03 ACEPTAR: f1 sentado en el extremo del banco con el tronco inclinado y las mancuernas junto a las piernas; f7 tronco erguido y brazos casi extendidos a la altura de los h
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en el extremo de un banco con los pies apoyados en el suelo y una mancuerna en cada mano. Inclina el tronco hacia delante con la espalda recta y deja las mancuernas junto a los pies. Eleva los brazos hacia los lados con los codos ligeramente flexionados mientras incorporas el tronco, hasta que los brazos queden a la altura de los hombros. Baja con control volviendo a inclinar el tronco. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en el extremo de un banco con los pies apoyados en el suelo y una mancuerna en cada mano.", "Inclina el tronco hacia delante con la espalda recta y deja las mancuernas junto a los pies.", "Eleva los brazos hacia los lados con los codos ligeramente flexionados mientras incorporas el tronco, hasta que los brazos queden a la altura de los hombros.", "Baja con control volviendo a inclinar el tronco.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 706 AND name_en = 'dumbbell seated bent arm lateral raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '9fd09407296f574dda51486a8d7d75f1';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 706 AND name_en = 'dumbbell seated bent arm lateral raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '9c662ebd5ee77044d190c1be0c238b90') THEN
    RAISE EXCEPTION 'V52: ejercicio 706 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 712 dumbbell reverse grip incline bench two arm row: instructions_es: R2 L03 (MEDIA): El texto dice «Siéntate en el banco con el pecho apoyado en el respaldo y los pies planos en el suelo»; en el GIF no está sentado: las piernas están estiradas d / V3 V04 ACEPTAR: f1-f12 no está sentado: pecho sobre el respaldo inclinado, piernas estiradas detrás con las puntas en el suelo; f1 brazos colgando, f7 mancuernas junto al tronc
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ajusta un banco a una inclinación moderada. Apoya el pecho en el respaldo con las piernas estiradas detrás y las puntas de los pies en el suelo. Sujeta una mancuerna en cada mano con agarre supino y deja los brazos colgando. Tira de las mancuernas hacia el tronco llevando los codos hacia atrás y juntando los omóplatos. Baja despacio hasta extender los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ajusta un banco a una inclinación moderada.", "Apoya el pecho en el respaldo con las piernas estiradas detrás y las puntas de los pies en el suelo.", "Sujeta una mancuerna en cada mano con agarre supino y deja los brazos colgando.", "Tira de las mancuernas hacia el tronco llevando los codos hacia atrás y juntando los omóplatos.", "Baja despacio hasta extender los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 712 AND name_en = 'dumbbell reverse grip incline bench two arm row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '4aa1b2f10d1716d687d18ebdee7a7aa1';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 712 AND name_en = 'dumbbell reverse grip incline bench two arm row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5bfafda31b63ec527f04f2d1f05ecc4d') THEN
    RAISE EXCEPTION 'V52: ejercicio 712 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 722 dumbbell seated bent over triceps extension: instructions_es: R2 L13 (ALTA): El texto dice sujetar una mancuerna en cada mano y mantener los codos cerca de la cabeza; el GIF muestra una sola mancuerna y el codo pegado al costado del tron / V3 W03 ACEPTAR: f1 una mancuerna con el brazo pegado al costado y el codo flexionado; f7 codo extendido con la mancuerna atrás; no se ve otra mancuerna en ningún fotograma; el 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en el extremo de un banco con los pies apoyados en el suelo y una mancuerna en una mano. Inclina el tronco hacia delante desde la cadera con la espalda recta. Lleva el codo junto al costado con el brazo paralelo al tronco y el codo flexionado. Extiende el codo llevando la mancuerna hacia atrás hasta estirar el brazo, sin mover el brazo. Vuelve despacio. Completa las repeticiones y cambia de brazo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en el extremo de un banco con los pies apoyados en el suelo y una mancuerna en una mano.", "Inclina el tronco hacia delante desde la cadera con la espalda recta.", "Lleva el codo junto al costado con el brazo paralelo al tronco y el codo flexionado.", "Extiende el codo llevando la mancuerna hacia atrás hasta estirar el brazo, sin mover el brazo.", "Vuelve despacio. Completa las repeticiones y cambia de brazo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 722 AND name_en = 'dumbbell seated bent over triceps extension' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '48f582e585efd2656d4a151f280f363b';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 722 AND name_en = 'dumbbell seated bent over triceps extension' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'abea110699e63fc8b41f132a28cc889e') THEN
    RAISE EXCEPTION 'V52: ejercicio 722 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 730 dumbbell seated neutral wrist curl: instructions_es: R2 L01 (ALTA): El texto dice flexionar las mancuernas hacia los hombros (lo que sugiere un curl de codo); el GIF muestra los antebrazos fijos sobre los muslos y solo movimient / V3 V04 ACEPTAR: f1 mancuernas verticales con agarre neutro y antebrazos sobre los muslos, f7 mancuernas inclinadas hacia delante; solo se mueve la muñeca, el texto actual descr
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en el extremo de un banco con los pies apoyados en el suelo y una mancuerna en cada mano con agarre neutro, pulgares hacia arriba. Apoya los antebrazos sobre los muslos con las muñecas por delante de las rodillas. Sin mover los antebrazos, inclina las muñecas hacia arriba para elevar la parte delantera de las mancuernas. Baja las mancuernas despacio inclinando las muñecas hacia abajo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en el extremo de un banco con los pies apoyados en el suelo y una mancuerna en cada mano con agarre neutro, pulgares hacia arriba.", "Apoya los antebrazos sobre los muslos con las muñecas por delante de las rodillas.", "Sin mover los antebrazos, inclina las muñecas hacia arriba para elevar la parte delantera de las mancuernas.", "Baja las mancuernas despacio inclinando las muñecas hacia abajo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 730 AND name_en = 'dumbbell seated neutral wrist curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '23bd52e399059737c08954957d2f0c10';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 730 AND name_en = 'dumbbell seated neutral wrist curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ecd5ce2f04f4310d0caa2ab390ae2b0f') THEN
    RAISE EXCEPTION 'V52: ejercicio 730 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 732 dumbbell seated one arm kickback: instructions_es: R2 L03 (ALTA): El texto pide la espalda «paralela al suelo»; en el GIF el tronco está inclinado unos 45°. También dice «Extiende el brazo recto hacia atrás» como punto de part / V3 V04 MEJORAR: f1 brazo colgando junto al costado con el codo algo flexionado; f7 brazo estirado hacia atrás y fuera casi horizontal; el brazo superior se mueve mucho (resalta
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en el extremo de un banco con los pies apoyados en el suelo, inclina el tronco hacia delante con la espalda recta y apoya la mano libre en el banco. Sujeta una mancuerna con la otra mano y deja el brazo colgando junto al costado, con el codo ligeramente flexionado. Lleva el brazo hacia atrás y hacia fuera, extendiendo el codo, hasta que quede estirado y casi horizontal. Bájalo despacio hasta la posición inicial. Completa las repeticiones y cambia de brazo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en el extremo de un banco con los pies apoyados en el suelo, inclina el tronco hacia delante con la espalda recta y apoya la mano libre en el banco.", "Sujeta una mancuerna con la otra mano y deja el brazo colgando junto al costado, con el codo ligeramente flexionado.", "Lleva el brazo hacia atrás y hacia fuera, extendiendo el codo, hasta que quede estirado y casi horizontal.", "Bájalo despacio hasta la posición inicial.", "Completa las repeticiones y cambia de brazo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 732 AND name_en = 'dumbbell seated one arm kickback' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a38bfa4b7404bc8a3b587970b3f3b378';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 732 AND name_en = 'dumbbell seated one arm kickback' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '38f5d3fc8572b2bb57a62ef96ec757a4') THEN
    RAISE EXCEPTION 'V52: ejercicio 732 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 734 dumbbell seated one leg calf raise: instructions_es: R2 L05 (MEDIA): El texto pide extender la otra pierna recta hacia delante; en el GIF no se ve ninguna pierna extendida, ambas piernas permanecen flexionadas / V3 V04 MEJORAR: f1-f12 sentado con un pie sobre el borde de un step y una mancuerna sobre el muslo cerca de la rodilla; ninguna pierna extendida al frente; f1 pie plano sobre e
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en un banco y coloca un pie sobre el borde de un step, cerca del banco. Apoya una mancuerna sobre el muslo de esa pierna, cerca de la rodilla, y sujétala con las manos. Eleva el talón lo más alto que puedas. Bájalo despacio hasta la posición inicial. Completa las repeticiones y cambia de pierna. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en un banco y coloca un pie sobre el borde de un step, cerca del banco.", "Apoya una mancuerna sobre el muslo de esa pierna, cerca de la rodilla, y sujétala con las manos.", "Eleva el talón lo más alto que puedas.", "Bájalo despacio hasta la posición inicial.", "Completa las repeticiones y cambia de pierna.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 734 AND name_en = 'dumbbell seated one leg calf raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ff8ade260fe302fc42cf68324ac2f426';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 734 AND name_en = 'dumbbell seated one leg calf raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '4f3874d92d0811f853548dbe37e1d975') THEN
    RAISE EXCEPTION 'V52: ejercicio 734 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 735 dumbbell seated one leg calf raise - hammer grip: name_es: R2 L06 (ALTA): Nombre sin tilde en «Elevación»; «agarre martillo» se entiende mejor como agarre neutro. Sentado no hay exigencia de equilibrio: se propone dificultad 1, como e / V3 V04 ACEPTAR: f1-f12 sentado, una mancuerna en vertical sobre la rodilla y elevación de un solo talón (f7) con el pie sobre un disco; el nombre actual carece de tilde; agarre
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Elevación de talón sentado a una pierna con mancuerna (agarre neutro)'
   WHERE exercise_id = 735 AND name_en = 'dumbbell seated one leg calf raise - hammer grip' AND name_es = 'Elevacion de talones sentado a una pierna con mancuerna (agarre martillo)';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 735 AND name_en = 'dumbbell seated one leg calf raise - hammer grip' AND name_es = 'Elevación de talón sentado a una pierna con mancuerna (agarre neutro)') THEN
    RAISE EXCEPTION 'V52: ejercicio 735 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 737 dumbbell single leg calf raise: instructions_es: R2 L08 (ALTA): El texto pide colocarse en el borde de un escalón con los talones colgando y apoyar la mano libre en una pared; el GIF muestra apoyo en suelo plano, sin escalón / V3 V04 ACEPTAR: f1-f12 vista posterior, de pie sobre una pierna en suelo plano sin escalón ni pared, la otra pierna flexionada atrás, mancuerna en una mano junto al costado; f7
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie sobre una pierna con la otra flexionada hacia atrás y sujeta una mancuerna con una mano junto al costado. Mantén el tronco erguido y el abdomen activado; si necesitas equilibrio, apoya la mano libre en una pared. Eleva el talón lo más alto posible, subiendo sobre la punta del pie. Baja el talón despacio hasta apoyarlo de nuevo en el suelo. Completa las repeticiones y cambia de pierna. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie sobre una pierna con la otra flexionada hacia atrás y sujeta una mancuerna con una mano junto al costado.", "Mantén el tronco erguido y el abdomen activado; si necesitas equilibrio, apoya la mano libre en una pared.", "Eleva el talón lo más alto posible, subiendo sobre la punta del pie.", "Baja el talón despacio hasta apoyarlo de nuevo en el suelo.", "Completa las repeticiones y cambia de pierna.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 737 AND name_en = 'dumbbell single leg calf raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '62744a8117e275acc203e0890624aea1';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 737 AND name_en = 'dumbbell single leg calf raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '4ce563e38c3d773ab4ab78010fac160b') THEN
    RAISE EXCEPTION 'V52: ejercicio 737 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 740 dumbbell seated shoulder press (parallel grip): instructions_es: R2 L11 (ALTA): Se contradice: primero dice palmas hacia adentro y luego, a la altura de los hombros, palmas hacia delante; el GIF mantiene el agarre neutro (palmas enfrentadas / V3 W04 ACEPTAR: f1 y f12 mancuernas a la altura de los hombros con los discos vistos de frente (agarre neutro, palmas enfrentadas) y codos al frente; f7 brazos extendidos sobre
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en un banco con respaldo, con la espalda apoyada y los pies en el suelo. Sujeta una mancuerna en cada mano a la altura de los hombros, con las palmas enfrentadas y los codos hacia delante. Empuja las mancuernas hacia arriba hasta extender los brazos por encima de la cabeza, manteniendo las palmas enfrentadas. Baja con control hasta la altura de los hombros. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en un banco con respaldo, con la espalda apoyada y los pies en el suelo.", "Sujeta una mancuerna en cada mano a la altura de los hombros, con las palmas enfrentadas y los codos hacia delante.", "Empuja las mancuernas hacia arriba hasta extender los brazos por encima de la cabeza, manteniendo las palmas enfrentadas.", "Baja con control hasta la altura de los hombros.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 740 AND name_en = 'dumbbell seated shoulder press (parallel grip)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '59f8b8d4e6a9b267141d3570072a8f7f';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 740 AND name_en = 'dumbbell seated shoulder press (parallel grip)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3541514fae95ea1095c2bebd0429fac0') THEN
    RAISE EXCEPTION 'V52: ejercicio 740 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 742 dumbbell single arm overhead carry: default_prescription: R2 L13 (MEDIA): Porteo por tiempo o distancia: la prescripción debería ser DURATION. Carga sostenida por encima de la cabeza todo el ejercicio: valorar AX (la definición cita c / V3 W04 ACEPTAR: f1-f12 camina de forma continua con la mancuerna sostenida sobre la cabeza con el brazo extendido; es un porteo por tiempo o distancia, sin repeticiones
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET default_prescription = 'DURATION'
   WHERE exercise_id = 742 AND name_en = 'dumbbell single arm overhead carry' AND default_prescription = 'SETS_REPS';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 742 AND name_en = 'dumbbell single arm overhead carry' AND default_prescription = 'DURATION') THEN
    RAISE EXCEPTION 'V52: ejercicio 742 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 745 dumbbell standing around world: instructions_es: R2 L16 (MEDIA): El texto empieza con los brazos en cruz y lleva las mancuernas por delante del cuerpo antes de subirlas; el GIF empieza con los brazos abajo y las sube por los  / V3 W04 MEJORAR: f1 y f18 mancuernas juntas delante de los muslos (no a los lados); f7 brazos en cruz a la altura de los hombros; f13 mancuernas juntas sobre la cabeza. El texto
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies separados y una mancuerna en cada mano delante de los muslos, con los brazos extendidos. Con los brazos rectos, elévalos por los lados hasta la altura de los hombros. Continúa el arco hasta juntar las mancuernas por encima de la cabeza. Baja los brazos con control hasta la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies separados y una mancuerna en cada mano delante de los muslos, con los brazos extendidos.", "Con los brazos rectos, elévalos por los lados hasta la altura de los hombros.", "Continúa el arco hasta juntar las mancuernas por encima de la cabeza.", "Baja los brazos con control hasta la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 745 AND name_en = 'dumbbell standing around world' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'c95ce8773b249b431d2e24d22917de8d';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 745 AND name_en = 'dumbbell standing around world' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd8833df2853ab8602be37eea7468af83') THEN
    RAISE EXCEPTION 'V52: ejercicio 745 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 748 dumbbell side bend: instructions_es: R2 L19 (ALTA): El texto dice inclinar el tronco hacia el lado de la mancuerna dejándola bajar por la pierna; el GIF muestra la inclinación hacia el lado contrario, con la manc / V3 Y01 ACEPTAR: f1 erguido con la mancuerna junto al muslo y la otra mano detrás de la cabeza; f4-f7 el tronco se inclina hacia el lado de la mano detrás de la cabeza y la manc
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con una mancuerna en una mano junto al muslo y la otra mano detrás de la cabeza. Inclina el tronco hacia el lado contrario a la mancuerna, dejando que esta suba por el costado. Vuelve con control a la posición erguida. Mantén las caderas quietas durante el movimiento. Completa las repeticiones y cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con una mancuerna en una mano junto al muslo y la otra mano detrás de la cabeza.", "Inclina el tronco hacia el lado contrario a la mancuerna, dejando que esta suba por el costado.", "Vuelve con control a la posición erguida.", "Mantén las caderas quietas durante el movimiento.", "Completa las repeticiones y cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 748 AND name_en = 'dumbbell side bend' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '360f6e86d174e0cf95a5797689135fc0';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 748 AND name_en = 'dumbbell side bend' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '572ba662c12920d17458976f926e1e2f') THEN
    RAISE EXCEPTION 'V52: ejercicio 748 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 750 dumbbell side plank with rear fly: instructions_es: R2 L01 (ALTA): El texto pide empezar tumbado de lado apoyado sobre el antebrazo; el GIF muestra apoyo sobre la mano con el brazo extendido, partiendo de una plancha alta con g / V3 V04 ACEPTAR: f1 plancha alta con una mano en el suelo y la otra sujetando la mancuerna bajo el pecho; f7 plancha lateral sobre la mano con el brazo extendido y la mancuerna  | default_prescription: R2 L01 (ALTA): Ejercicio dinámico por repeticiones: la prescripción DURATION no encaja (SETS_REPS). Hay rotación de tronco además de la elevación del brazo; el brazo vertical  / V3 V04 ACEPTAR: f1-f12 giro repetido de plancha alta a plancha lateral con elevación de la mancuerna: movimiento dinámico por repeticiones
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate en plancha alta en el suelo con los brazos extendidos, las manos bajo los hombros y una mancuerna en una mano. Activa el core y mantén el cuerpo en línea recta de la cabeza a los pies. Gira el cuerpo hacia el lado de la mancuerna hasta quedar en plancha lateral sobre la otra mano, con los pies uno sobre otro, mientras elevas la mancuerna hasta que el brazo apunte al techo. Baja la mancuerna con control y vuelve a la plancha alta sin dejar caer la cadera. Completa las repeticiones y cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate en plancha alta en el suelo con los brazos extendidos, las manos bajo los hombros y una mancuerna en una mano.", "Activa el core y mantén el cuerpo en línea recta de la cabeza a los pies.", "Gira el cuerpo hacia el lado de la mancuerna hasta quedar en plancha lateral sobre la otra mano, con los pies uno sobre otro, mientras elevas la mancuerna hasta que el brazo apunte al techo.", "Baja la mancuerna con control y vuelve a la plancha alta sin dejar caer la cadera.", "Completa las repeticiones y cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb, default_prescription = 'SETS_REPS'
   WHERE exercise_id = 750 AND name_en = 'dumbbell side plank with rear fly' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '829c8dbddfe019f3bdf6874b5305c2cc' AND default_prescription = 'DURATION';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 750 AND name_en = 'dumbbell side plank with rear fly' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'f93d36c1956efc5f00fe742ce1cc35b2' AND default_prescription = 'SETS_REPS') THEN
    RAISE EXCEPTION 'V52: ejercicio 750 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 752 dumbbell single leg deadlift with stepbox support: axial_load: R2 L03 (ALTA): Una sola mancuerna en la mano no supone carga compresiva relevante: AX sobra. El apoyo del pie trasero reduce la exigencia de equilibrio: nivel 2 (propuesta). / V3 V04 ACEPTAR: f1-f12 una sola mancuerna en una mano durante la bisagra de cadera; sin carga en espalda, hombros ni por encima de la cabeza / coordinador: definicion literal demostrada en todos los fotogramas (R2 + V3) | instructions_es: R2 L03 (ALTA): El texto pide levantar la pierna trasera detrás de ti al bajar; en el GIF la punta del pie trasero permanece apoyada en el step en todo momento, que es lo que d / V3 V04 ACEPTAR: f1 tronco inclinado con la mancuerna junto a la pierna de apoyo, f7 erguido; la punta del pie trasero permanece apoyada en el step en ambos; el texto pide levan
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET axial_load = FALSE, instructions_es = 'Ponte de pie delante de un step con una mancuerna en una mano. Apoya la punta de un pie sobre el step detrás de ti y carga el peso en la pierna delantera. Con la espalda recta, inclina el tronco desde la cadera y baja la mancuerna hacia el suelo junto a la pierna de apoyo, sin despegar el pie del step. Vuelve a erguirte empujando con la cadera. Cambia de lado al terminar la serie. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie delante de un step con una mancuerna en una mano.", "Apoya la punta de un pie sobre el step detrás de ti y carga el peso en la pierna delantera.", "Con la espalda recta, inclina el tronco desde la cadera y baja la mancuerna hacia el suelo junto a la pierna de apoyo, sin despegar el pie del step.", "Vuelve a erguirte empujando con la cadera.", "Cambia de lado al terminar la serie.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 752 AND name_en = 'dumbbell single leg deadlift with stepbox support' AND axial_load = TRUE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'df845c48cade59e97d86f4e20e2ef628';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 752 AND name_en = 'dumbbell single leg deadlift with stepbox support' AND axial_load = FALSE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ba6e8a537d9cfc2cc8c71ba9ac1cabae') THEN
    RAISE EXCEPTION 'V52: ejercicio 752 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 754 dumbbell single leg squat: instructions_es: R2 L05 (ALTA): El texto dice sujetar una mancuerna en cada mano; el GIF muestra una sola mancuerna en una mano y el otro brazo extendido al frente para equilibrarse / V3 V04 ACEPTAR: f1 de pie sobre una pierna con la otra extendida al frente sin apoyo, una sola mancuerna en la mano del lado de apoyo y el otro brazo extendido al frente; f7 se
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie sobre una pierna y sujeta una mancuerna con la mano del mismo lado, con el brazo junto al costado. Extiende la otra pierna al frente sin apoyarla y estira el brazo libre hacia delante para equilibrarte. Flexiona la rodilla de apoyo y baja la cadera como si fueras a sentarte, con el pecho erguido. Empuja con la pierna de apoyo para volver a subir. Completa las repeticiones y cambia de pierna. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie sobre una pierna y sujeta una mancuerna con la mano del mismo lado, con el brazo junto al costado.", "Extiende la otra pierna al frente sin apoyarla y estira el brazo libre hacia delante para equilibrarte.", "Flexiona la rodilla de apoyo y baja la cadera como si fueras a sentarte, con el pecho erguido.", "Empuja con la pierna de apoyo para volver a subir.", "Completa las repeticiones y cambia de pierna.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 754 AND name_en = 'dumbbell single leg squat' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '319010aa13f81cd2dadc5a46757cb316';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 754 AND name_en = 'dumbbell single leg squat' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '607187f19e5f3d9f7d9af42dfb747e63') THEN
    RAISE EXCEPTION 'V52: ejercicio 754 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 756 dumbbell seated preacher curl: instructions_es: R2 L07 (ALTA): El texto dice sujetar una mancuerna con una mano y luego cambiar de brazo; el GIF muestra una mancuerna en cada mano y ambos brazos trabajando a la vez / V3 V04 ACEPTAR: f1-f12 banco Scott con una mancuerna en cada mano y ambos brazos flexionando a la vez (f1 abajo, f7 arriba); el texto indica una sola mano y cambiar de brazo | equipo: R2 L07 (ALTA): El GIF usa un banco Scott, no un banco plano / V3 V04 ACEPTAR: f1-f12 el curl se hace con los brazos sobre la almohadilla inclinada de un banco Scott; un banco plano no permite el movimiento. Comprobar que preacher bench ex
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en un banco Scott con los pies apoyados en el suelo. Sujeta una mancuerna en cada mano con agarre supino y apoya la parte posterior de los brazos en la almohadilla. Sin despegar los brazos de la almohadilla, flexiona los codos y sube las mancuernas hacia los hombros. Haz una pausa breve arriba y baja con control hasta extender los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en un banco Scott con los pies apoyados en el suelo.", "Sujeta una mancuerna en cada mano con agarre supino y apoya la parte posterior de los brazos en la almohadilla.", "Sin despegar los brazos de la almohadilla, flexiona los codos y sube las mancuernas hacia los hombros.", "Haz una pausa breve arriba y baja con control hasta extender los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 756 AND name_en = 'dumbbell seated preacher curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '305df8c7b109219ed7166ea8999f0073';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 756 AND name_en = 'dumbbell seated preacher curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e27c9c83200c74a2a5db2db56408a8e8') THEN
    RAISE EXCEPTION 'V52: ejercicio 756 no esta en el estado anterior esperado';
  END IF;
END $v52$;
DO $v52$ BEGIN
  IF (SELECT count(*) FROM equipment_types WHERE code IN ('dumbbell', 'preacher bench')) <> 2 THEN
    RAISE EXCEPTION 'V52: faltan codigos de equipamiento para 756';
  END IF;
  IF (SELECT coalesce(string_agg(r.group_no || ':' || t.code, ',' ORDER BY r.group_no || ':' || t.code), '') FROM exercise_equipment_requirements r JOIN equipment_types t ON t.equipment_id = r.equipment_id WHERE r.exercise_id = 756) = '1:dumbbell,2:preacher bench' THEN RETURN; END IF;
  IF (SELECT coalesce(string_agg(r.group_no || ':' || t.code, ',' ORDER BY r.group_no || ':' || t.code), '') FROM exercise_equipment_requirements r JOIN equipment_types t ON t.equipment_id = r.equipment_id WHERE r.exercise_id = 756) <> '1:dumbbell,2:bench' THEN RAISE EXCEPTION 'V52: requisitos de 756 distintos de los esperados'; END IF;
  DELETE FROM exercise_equipment_requirements WHERE exercise_id = 756;
  INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id) VALUES (756, 1, (SELECT equipment_id FROM equipment_types WHERE code = 'dumbbell'));
  INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id) VALUES (756, 2, (SELECT equipment_id FROM equipment_types WHERE code = 'preacher bench'));
  UPDATE exercises SET equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'dumbbell') WHERE exercise_id = 756;
END $v52$;
-- 760 dumbbell standing alternate hammer curl and press: instructions_es: R2 L11 (ALTA): Describe una secuencia de curl derecho, curl izquierdo, press derecho y press izquierdo; el GIF muestra con un mismo brazo curl martillo seguido directamente de / V3 W04 MEJORAR: f1 mancuernas a los costados con agarre neutro; f7 curl martillo hasta el hombro con un brazo; f13 press con ese brazo sobre la cabeza; f14-f18 fundido directo 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con una mancuerna en cada mano a los lados del cuerpo y las palmas mirando hacia ti. Con un brazo, flexiona el codo y sube la mancuerna hasta el hombro sin girar la muñeca. Desde el hombro, empuja la mancuerna por encima de la cabeza hasta extender el brazo, con la espalda recta y el core activado. Baja la mancuerna con control hasta el costado. Repite el número de repeticiones indicado. Alterna los lados.', instruction_steps_es = '["Ponte de pie con una mancuerna en cada mano a los lados del cuerpo y las palmas mirando hacia ti.", "Con un brazo, flexiona el codo y sube la mancuerna hasta el hombro sin girar la muñeca.", "Desde el hombro, empuja la mancuerna por encima de la cabeza hasta extender el brazo, con la espalda recta y el core activado.", "Baja la mancuerna con control hasta el costado.", "Repite el número de repeticiones indicado. Alterna los lados."]'::jsonb
   WHERE exercise_id = 760 AND name_en = 'dumbbell standing alternate hammer curl and press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b6ffe913d93a6efc0a797b2316ba6ea3';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 760 AND name_en = 'dumbbell standing alternate hammer curl and press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'cd0511ba558724e81f55fe71d39d1447') THEN
    RAISE EXCEPTION 'V52: ejercicio 760 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 765 dumbbell standing concentration curl: instructions_es: R2 L16 (ALTA): El texto no menciona la inclinación del tronco y da a entender un curl erguido; el GIF muestra el tronco inclinado casi horizontal con el brazo colgando vertica / V3 W04 ACEPTAR: f1-f12 tronco inclinado casi horizontal, pies más separados que los hombros, brazo de trabajo colgando vertical y flexión de codo hasta f7; la otra mano agarra 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies algo más separados que los hombros y una mancuerna en una mano. Inclina el tronco hacia delante desde la cadera, con la espalda recta, y apoya la otra mano en el muslo. Deja colgar el brazo de la mancuerna extendido hacia el suelo. Flexiona el codo y sube la mancuerna hacia el hombro sin mover la parte superior del brazo. Bájala con control. Completa las repeticiones y cambia de brazo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies algo más separados que los hombros y una mancuerna en una mano.", "Inclina el tronco hacia delante desde la cadera, con la espalda recta, y apoya la otra mano en el muslo.", "Deja colgar el brazo de la mancuerna extendido hacia el suelo.", "Flexiona el codo y sube la mancuerna hacia el hombro sin mover la parte superior del brazo.", "Bájala con control. Completa las repeticiones y cambia de brazo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 765 AND name_en = 'dumbbell standing concentration curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'f3fbe53b0904f9518b10fa093e366321';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 765 AND name_en = 'dumbbell standing concentration curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'c6eb0b13a4df7b104664af1f3b4b87ca') THEN
    RAISE EXCEPTION 'V52: ejercicio 765 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 773 dynamic chest stretch (male): instructions_es: R2 L04 (ALTA): El texto sitúa el estiramiento al cruzar los brazos delante y pide mantenerlo 10-30 segundos y repeticiones; el GIF muestra un balanceo continuo en que los braz / V3 V04 ACEPTAR: f1 brazos extendidos al frente a la altura de los hombros sin cruzarse claramente; f7 brazos abiertos a los lados y algo atrás con el pecho resaltado; balanceo  | is_stretch: R2 L04 (ALTA): De pie, balancea los brazos estirados a la altura de los hombros desde delante del pecho hasta abrirlos hacia atrás y vuelve, de forma continua / V3 V04 ACEPTAR: f1-f12 balanceo continuo de amplio recorrido entre brazos al frente y abiertos; no se mantiene una posición de elongación
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con la espalda recta y los pies separados a la altura de los hombros. Extiende los brazos al frente a la altura de los hombros. Abre los brazos hacia los lados y hacia atrás hasta notar el estiramiento en el pecho. Vuelve a llevarlos al frente con control y repite el balanceo de forma continua. Mantén la duración indicada.', instruction_steps_es = '["Ponte de pie con la espalda recta y los pies separados a la altura de los hombros.", "Extiende los brazos al frente a la altura de los hombros.", "Abre los brazos hacia los lados y hacia atrás hasta notar el estiramiento en el pecho.", "Vuelve a llevarlos al frente con control y repite el balanceo de forma continua.", "Mantén la duración indicada."]'::jsonb, is_stretch = FALSE
   WHERE exercise_id = 773 AND name_en = 'dynamic chest stretch (male)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3f6758de729778f398b5cde3035fc4cd' AND is_stretch = TRUE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 773 AND name_en = 'dynamic chest stretch (male)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '6e4c5277c4561277708e31c71ffe677c' AND is_stretch = FALSE) THEN
    RAISE EXCEPTION 'V52: ejercicio 773 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 774 elbow-to-knee: instructions_es: R2 L05 (ALTA): El texto dice empezar con las rodillas flexionadas y los pies apoyados en el suelo y volver a extender las piernas a la posición inicial; el GIF muestra las pie / V3 V04 ACEPTAR: f1-f12 boca arriba con las manos tras la cabeza, hombros despegados y piernas siempre en el aire alternando una flexionada y otra extendida; codo hacia la rodil
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en el suelo y coloca las manos detrás de la cabeza, con los codos abiertos. Despega los hombros y las piernas del suelo. Lleva una rodilla hacia el pecho y gira el tronco para acercarle el codo contrario, mientras extiendes la otra pierna sin apoyarla. Cambia de lado con un movimiento controlado, como si pedalearas. Alterna los lados. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en el suelo y coloca las manos detrás de la cabeza, con los codos abiertos.", "Despega los hombros y las piernas del suelo.", "Lleva una rodilla hacia el pecho y gira el tronco para acercarle el codo contrario, mientras extiendes la otra pierna sin apoyarla.", "Cambia de lado con un movimiento controlado, como si pedalearas.", "Alterna los lados.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 774 AND name_en = 'elbow-to-knee' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '8427c003c1ae8490c78a95dc4f18e0ff';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 774 AND name_en = 'elbow-to-knee' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '9225c49c2ea9c256c63d3f26aebe8c12') THEN
    RAISE EXCEPTION 'V52: ejercicio 774 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 775 elbow lift - reverse push-up: name_es: R2 L06 (MEDIA): Las almohadillas o bancos bajos bajo los codos son imprescindibles en el movimiento del GIF. Se mantiene FL porque las piernas quedan apoyadas en el suelo y hay / V3 V04 ACEPTAR: f1 boca arriba con los codos sobre dos almohadillas altas y la cadera baja, talones en el suelo; f7 tronco y cadera elevados alineados empujando con los codos:  | instructions_es: R2 L06 (MEDIA): El texto describe tumbarse boca abajo en el suelo y empujar con las palmas; el GIF muestra a la persona boca arriba, con los talones en el suelo, empujando con  / V3 V04 MEJORAR: f1 boca arriba entre dos almohadillas con cada codo apoyado en una y la cadera baja, talones en el suelo; f7 cuerpo elevado y alineado; la espalda no apoya en n
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Flexión inversa apoyado en los codos', instructions_es = 'Coloca dos almohadillas o bancos bajos en paralelo y sitúate boca arriba entre ellos, con las piernas extendidas y los talones en el suelo. Flexiona los codos y apoya cada uno sobre una almohadilla, a los lados del tronco. Empuja con los codos hacia abajo para elevar el tronco y la cadera hasta alinear el cuerpo de los hombros a los talones. Baja despacio a la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca dos almohadillas o bancos bajos en paralelo y sitúate boca arriba entre ellos, con las piernas extendidas y los talones en el suelo.", "Flexiona los codos y apoya cada uno sobre una almohadilla, a los lados del tronco.", "Empuja con los codos hacia abajo para elevar el tronco y la cadera hasta alinear el cuerpo de los hombros a los talones.", "Baja despacio a la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 775 AND name_en = 'elbow lift - reverse push-up' AND name_es = 'Elevación de codo y push-up invertido' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '8dcedb658e26cc1fccbfff04186f602e';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 775 AND name_en = 'elbow lift - reverse push-up' AND name_es = 'Flexión inversa apoyado en los codos' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '43f6045ef20859ab94c8f7692fcfd6ff') THEN
    RAISE EXCEPTION 'V52: ejercicio 775 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 777 elbow dips: instructions_es: R2 L07 (MEDIA): El texto describe un fondo normal; el GIF muestra además una fase con los antebrazos apoyados sobre las barras antes de volver a extender los brazos / V3 V04 ACEPTAR: f1 brazos extendidos sin pies apoyados; f7 fondo profundo con los codos flexionados; f13 antebrazos apoyados sobre las barras con el tronco erguido; f14-f18 vue | difficulty_level: R2 L07 (MEDIA): apoyo suspendido sobre brazos; fuerza relativa alta / V3 V04 ACEPTAR: f1-f18 fondo en paralelas con el cuerpo sostenido en apoyo suspendido sobre los brazos: fuerza relativa alta, nivel 3 según la definición (fondos)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Sujétate a unas barras paralelas con los brazos extendidos y los pies sin apoyo. Flexiona los codos y baja el cuerpo de forma controlada hasta apoyar los antebrazos sobre las barras. Empuja con las manos para despegar los antebrazos y extender de nuevo los brazos. Mantén el tronco firme y los hombros alejados de las orejas durante todo el movimiento. Repite el número de repeticiones indicado.', instruction_steps_es = '["Sujétate a unas barras paralelas con los brazos extendidos y los pies sin apoyo.", "Flexiona los codos y baja el cuerpo de forma controlada hasta apoyar los antebrazos sobre las barras.", "Empuja con las manos para despegar los antebrazos y extender de nuevo los brazos.", "Mantén el tronco firme y los hombros alejados de las orejas durante todo el movimiento.", "Repite el número de repeticiones indicado."]'::jsonb, difficulty_level = 2
   WHERE exercise_id = 777 AND name_en = 'elbow dips' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'fb4d1fd84c252417a3e8300957b8c098' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 777 AND name_en = 'elbow dips' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '714701775b66c82ef9fbf861d1187ff9' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 777 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 779 dumbbell standing palms in press: axial_load: R2 L09 (ALTA): Press por encima de la cabeza con mancuernas de pie: la definición de AX incluye las cargas en press por encima de la cabeza, por lo que falta AX. / V3 V05 ACEPTAR: f1 mancuernas a la altura de los hombros; f5-f8 brazos extendidos por encima de la cabeza empujando las mancuernas de pie: press por encima de la cabeza con car
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET axial_load = TRUE
   WHERE exercise_id = 779 AND name_en = 'dumbbell standing palms in press' AND axial_load = FALSE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 779 AND name_en = 'dumbbell standing palms in press' AND axial_load = TRUE) THEN
    RAISE EXCEPTION 'V52: ejercicio 779 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 780 dumbbell upright row: movement_pattern: R2 L10 (ALTA): El remo vertical eleva los brazos en abducción con flexión de codos; no hay tracción horizontal. SHOULDER_RAISE es la clase más próxima (alternativa: VERTICAL_P / V3 V05 ACEPTAR: f1 mancuernas frente a los muslos; f6-f7 codos elevados a la altura de los hombros con las mancuernas junto al pecho: elevación en abducción con flexión de codo
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET movement_pattern = 'SHOULDER_RAISE'
   WHERE exercise_id = 780 AND name_en = 'dumbbell upright row' AND movement_pattern = 'HORIZONTAL_PULL';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 780 AND name_en = 'dumbbell upright row' AND movement_pattern = 'SHOULDER_RAISE') THEN
    RAISE EXCEPTION 'V52: ejercicio 780 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 782 dumbbell upright shoulder external rotation: instructions_es: R2 L12 (ALTA): El texto usa una mancuerna en cada mano y sube los brazos a los lados con los codos ligeramente flexionados; el GIF muestra un solo brazo, con el codo ya a la a / V3 W04 ACEPTAR: f1 un solo brazo con el codo a la altura del hombro flexionado 90 grados y antebrazo hacia abajo, la otra mano en la cadera; f7 antebrazo vertical con la mancue
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con una mancuerna en una mano y la otra mano en la cadera. Separa el brazo hasta que el codo quede a la altura del hombro y flexiona el codo a 90 grados, con el antebrazo apuntando hacia abajo. Sin mover el codo, gira el brazo hacia fuera hasta que el antebrazo quede vertical y la mancuerna por encima de la cabeza. Vuelve despacio a la posición inicial. Repite el número de repeticiones indicado y después cambia de brazo.', instruction_steps_es = '["Ponte de pie con una mancuerna en una mano y la otra mano en la cadera.", "Separa el brazo hasta que el codo quede a la altura del hombro y flexiona el codo a 90 grados, con el antebrazo apuntando hacia abajo.", "Sin mover el codo, gira el brazo hacia fuera hasta que el antebrazo quede vertical y la mancuerna por encima de la cabeza.", "Vuelve despacio a la posición inicial.", "Repite el número de repeticiones indicado y después cambia de brazo."]'::jsonb
   WHERE exercise_id = 782 AND name_en = 'dumbbell upright shoulder external rotation' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '234b8b0c61b13b3201725bf4e150e8dd';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 782 AND name_en = 'dumbbell upright shoulder external rotation' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '774902205350a259337e44199d47a393') THEN
    RAISE EXCEPTION 'V52: ejercicio 782 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 786 dumbbell step-up lunge: name_es: R2 L16 (MEDIA): El nombre actual sugiere que la mancuerna sube. En f7 la rodilla trasera queda muy cerca del banco de detrás; no se aprecia con claridad que apoye y cargue peso / V3 W04 ACEPTAR: f1-f12 entre dos bancos con el pie trasero sobre el banco de detrás; f7 el pie delantero sube al banco de delante con una mancuerna en cada mano. El nombre actu
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Zancada con subida al banco con mancuernas'
   WHERE exercise_id = 786 AND name_en = 'dumbbell step-up lunge' AND name_es = 'Zancada con subida de mancuerna';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 786 AND name_en = 'dumbbell step-up lunge' AND name_es = 'Zancada con subida al banco con mancuernas') THEN
    RAISE EXCEPTION 'V52: ejercicio 786 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 794 dumbbell standing triceps extension: instructions_es: R2 L04 (ALTA): El texto dice sujetar la mancuerna con una mano; el GIF muestra una sola mancuerna sujeta con las dos manos / V3 V05 ACEPTAR: f1 y f7 ampliados: ambos brazos elevados sujetando una sola mancuerna con las dos manos; f7 codos flexionados con la mancuerna detrás de la cabeza y brazos junt
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies separados a la altura de los hombros. Sujeta una mancuerna con las dos manos y extiende los brazos por encima de la cabeza. Flexiona los codos y baja la mancuerna detrás de la cabeza, manteniendo los brazos superiores fijos y cerca de la cabeza. Extiende los codos para volver arriba. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies separados a la altura de los hombros.", "Sujeta una mancuerna con las dos manos y extiende los brazos por encima de la cabeza.", "Flexiona los codos y baja la mancuerna detrás de la cabeza, manteniendo los brazos superiores fijos y cerca de la cabeza.", "Extiende los codos para volver arriba.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 794 AND name_en = 'dumbbell standing triceps extension' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd197a4e385b6241a823dc6092875a1ec';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 794 AND name_en = 'dumbbell standing triceps extension' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '8c65a637bdcca20302476989ae0ef1d0') THEN
    RAISE EXCEPTION 'V52: ejercicio 794 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 796 dumbbell step up single leg balance with bicep curl: name_es: R2 L06 (ALTA): El nombre actual omite la subida al banco, que es el movimiento principal. El GIF usa un banco plano, que no figura entre las alternativas del requisito 2. Solo / V3 V05 ACEPTAR: f1 un pie sobre el banco con la rodilla flexionada; f4-f7 sube al banco sobre una pierna con la otra rodilla elevada y hace curl con una mancuerna en cada mano:
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Subida al banco con equilibrio a una pierna y curl con mancuernas'
   WHERE exercise_id = 796 AND name_en = 'dumbbell step up single leg balance with bicep curl' AND name_es = 'Curl de bíceps con mancuerna en balance de una pierna';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 796 AND name_en = 'dumbbell step up single leg balance with bicep curl' AND name_es = 'Subida al banco con equilibrio a una pierna y curl con mancuernas') THEN
    RAISE EXCEPTION 'V52: ejercicio 796 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 797 dumbbell tate press: requires_overhead: R2 L07 (ALTA): Los brazos quedan en vertical sobre el pecho, nunca más allá de la cabeza: OH sobra / V3 V05 ACEPTAR: f1-f12 tumbado boca arriba en banco plano; los brazos van de verticales sobre el pecho (f1) a codos flexionados con las mancuernas sobre el pecho (f7); nunca pa / coordinador: definicion literal demostrada en todos los fotogramas (R2 + V3)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = FALSE
   WHERE exercise_id = 797 AND name_en = 'dumbbell tate press' AND requires_overhead = TRUE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 797 AND name_en = 'dumbbell tate press' AND requires_overhead = FALSE) THEN
    RAISE EXCEPTION 'V52: ejercicio 797 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 803 dumbbells seated triceps extension: instructions_es: R2 L13 (ALTA): El texto dice sujetar una sola mancuerna con ambas manos; el GIF muestra una mancuerna en cada mano / V3 W04 ACEPTAR: f1-f12 una mancuerna en cada mano; f1 bajadas detrás de la cabeza y f7 brazos extendidos sobre la cabeza; sentado en banco con respaldo. El texto actual dice un
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en un banco con respaldo, con la espalda recta y los pies apoyados en el suelo. Sujeta una mancuerna en cada mano y extiende los brazos por encima de la cabeza. Flexiona los codos y baja las mancuernas por detrás de la cabeza, con los brazos cerca de las orejas. Extiende los codos para volver a subir las mancuernas. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en un banco con respaldo, con la espalda recta y los pies apoyados en el suelo.", "Sujeta una mancuerna en cada mano y extiende los brazos por encima de la cabeza.", "Flexiona los codos y baja las mancuernas por detrás de la cabeza, con los brazos cerca de las orejas.", "Extiende los codos para volver a subir las mancuernas.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 803 AND name_en = 'dumbbells seated triceps extension' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '0c61bbf81941aa18105919b8167759bd';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 803 AND name_en = 'dumbbells seated triceps extension' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd5e2e867ae57ae73bdf18e0b7242e8bb') THEN
    RAISE EXCEPTION 'V52: ejercicio 803 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 804 exercise ball alternating arm ups: instructions_es: R2 L14 (MEDIA): El texto no indica la alternancia de brazos que muestra el GIF y que da nombre al ejercicio / V3 W04 ACEPTAR: f1 la mano cercana apoya en el suelo y el brazo lejano se eleva; f7 apoya la otra mano y se eleva el brazo contrario: los brazos alternan. El texto actual no in
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca abajo sobre el balón de estabilidad con las piernas estiradas y las puntas de los pies apoyadas en el suelo. Apoya una mano en el suelo delante del balón para estabilizarte. Eleva el brazo libre estirado hacia delante y hacia fuera hasta la altura del tronco. Bájalo con control y cambia de brazo. Alterna los lados. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca abajo sobre el balón de estabilidad con las piernas estiradas y las puntas de los pies apoyadas en el suelo.", "Apoya una mano en el suelo delante del balón para estabilizarte.", "Eleva el brazo libre estirado hacia delante y hacia fuera hasta la altura del tronco.", "Bájalo con control y cambia de brazo.", "Alterna los lados.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 804 AND name_en = 'exercise ball alternating arm ups' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3ce8ed6dad30c74752311a2f4356027f';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 804 AND name_en = 'exercise ball alternating arm ups' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '6f1d5f4ee9684f6dcb4f67ede2eeb21f') THEN
    RAISE EXCEPTION 'V52: ejercicio 804 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 805 exercise ball back extension with arms extended: requires_overhead: R2 L15 (ALTA): Los brazos nunca suben por encima de la cabeza: la bandera OH no corresponde / V3 W04 ACEPTAR: f1-f12 brazos extendidos hacia atrás junto a los costados, manos hacia la cadera; en ningún fotograma suben por encima de la cabeza / coordinador: definicion literal demostrada en todos los fotogramas (R2 + V3) | instructions_es: R2 L15 (ALTA): El texto pide estirar los brazos hacia delante alineados con la cabeza; en el GIF los brazos permanecen extendidos hacia atrás, junto al cuerpo, en todos los fo / V3 W04 ACEPTAR: f1 y f7 (y todos los demás) brazos extendidos hacia atrás junto al cuerpo, rodillas en el suelo y abdomen sobre el balón. El texto actual pide brazos al frente 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = FALSE, instructions_es = 'Arrodíllate en el suelo y apoya el abdomen y la cadera sobre el balón de estabilidad. Extiende los brazos hacia atrás, junto a los costados. Eleva el tronco extendiendo la espalda sin despegar las rodillas del suelo. Baja con control hasta apoyar de nuevo el tronco sobre el balón. Repite el número de repeticiones indicado.', instruction_steps_es = '["Arrodíllate en el suelo y apoya el abdomen y la cadera sobre el balón de estabilidad.", "Extiende los brazos hacia atrás, junto a los costados.", "Eleva el tronco extendiendo la espalda sin despegar las rodillas del suelo.", "Baja con control hasta apoyar de nuevo el tronco sobre el balón.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 805 AND name_en = 'exercise ball back extension with arms extended' AND requires_overhead = TRUE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ada1bc9cfceaaa461ca8966dacae87a7';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 805 AND name_en = 'exercise ball back extension with arms extended' AND requires_overhead = FALSE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '0d1c519cd1cdc0490783c5762a88dfaf') THEN
    RAISE EXCEPTION 'V52: ejercicio 805 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 806 exercise ball back extension with hands behind head: requires_overhead: R2 L16 (ALTA): Manos detrás de la cabeza con los codos hacia delante, tumbado boca abajo: los brazos no quedan por encima de la cabeza, así que OH sobra. No se baja al suelo ( / V3 W04 ACEPTAR: f1-f12 manos detrás de la cabeza con los codos hacia delante y abajo, tumbado boca abajo sobre el balón; los brazos nunca quedan por encima de la cabeza / coordinador: definicion literal demostrada en todos los fotogramas (R2 + V3)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = FALSE
   WHERE exercise_id = 806 AND name_en = 'exercise ball back extension with hands behind head' AND requires_overhead = TRUE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 806 AND name_en = 'exercise ball back extension with hands behind head' AND requires_overhead = FALSE) THEN
    RAISE EXCEPTION 'V52: ejercicio 806 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 811 exercise ball hip flexor stretch: requires_kneeling: R2 L01 (MEDIA): La rodilla trasera no llega a apoyarse en el suelo en ningún fotograma (el peso recae en el balón y los pies): KN sobra. El estiramiento es de los flexores de l / V3 V05 ACEPTAR: f1, f4, f7, f10 ampliados: sentado sobre el balón, rodilla trasera claramente por encima del suelo y solo la punta del pie trasero apoyada; ninguna rodilla apoy / coordinador: definicion literal demostrada en todos los fotogramas (R2 + V3)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_kneeling = FALSE
   WHERE exercise_id = 811 AND name_en = 'exercise ball hip flexor stretch' AND requires_kneeling = TRUE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 811 AND name_en = 'exercise ball hip flexor stretch' AND requires_kneeling = FALSE) THEN
    RAISE EXCEPTION 'V52: ejercicio 811 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 812 exercise ball dip: instructions_es: R2 L02 (ALTA): El texto dice sentarse en el balón con los pies en el suelo y las rodillas a 90 grados; el GIF muestra a la persona sentada en el suelo con las piernas extendid / V3 V05 ACEPTAR: f1 sentado no en el balón sino en el suelo delante de él con piernas extendidas y brazos extendidos, cadera elevada y talones apoyados; f7 codos flexionados y c
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en el suelo con las piernas extendidas y un balón de estabilidad justo detrás de ti. Apoya las manos sobre el balón, a ambos lados del cuerpo. Empuja con las manos hasta extender los brazos y despegar la cadera del suelo, con los talones apoyados. Flexiona los codos y baja la cadera hacia el suelo de forma controlada sin soltar el balón. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en el suelo con las piernas extendidas y un balón de estabilidad justo detrás de ti.", "Apoya las manos sobre el balón, a ambos lados del cuerpo.", "Empuja con las manos hasta extender los brazos y despegar la cadera del suelo, con los talones apoyados.", "Flexiona los codos y baja la cadera hacia el suelo de forma controlada sin soltar el balón.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 812 AND name_en = 'exercise ball dip' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '04c7302d8fa45c19da78817f3ccd76e6';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 812 AND name_en = 'exercise ball dip' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'af5b4ebc19abbc3ec88ef189d378bf0f') THEN
    RAISE EXCEPTION 'V52: ejercicio 812 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 813 flag: instructions_es: R2 L03 (ALTA): El texto describe levantar las piernas desde el suelo y bajarlas, y termina con «Repite el número de repeticiones deseado»; el GIF muestra solo la posición hori / V3 V05 MEJORAR: f1-f12 posición de bandera mantenida sin cambios: cuerpo horizontal, una mano arriba y otra abajo en el poste con brazos extendidos; la mano inferior queda por  | default_prescription: R2 L03 (ALTA): Ejercicio isométrico: prescripción DURATION. Solo se ve la fase mantenida; la entrada a la posición no aparece. / V3 V05 ACEPTAR: f1-f12 idénticos salvo el color muscular: isometría mantenida, sin fase dinámica visible
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Sujeta con ambas manos un poste vertical bien fijado, con una mano más alta que la otra y los brazos extendidos. Tira con el brazo de arriba y empuja con el de abajo para sostener el cuerpo en el aire, sin apoyar los pies. Mantén el cuerpo recto y horizontal, con el abdomen y los glúteos activados. Al terminar, baja los pies al suelo con control. Mantén la duración indicada.', instruction_steps_es = '["Sujeta con ambas manos un poste vertical bien fijado, con una mano más alta que la otra y los brazos extendidos.", "Tira con el brazo de arriba y empuja con el de abajo para sostener el cuerpo en el aire, sin apoyar los pies.", "Mantén el cuerpo recto y horizontal, con el abdomen y los glúteos activados.", "Al terminar, baja los pies al suelo con control.", "Mantén la duración indicada."]'::jsonb, default_prescription = 'DURATION'
   WHERE exercise_id = 813 AND name_en = 'flag' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '21d6bc92881eb25c373e1d9682024c41' AND default_prescription = 'SETS_REPS';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 813 AND name_en = 'flag' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'fd2a7c18b6a55355ebda0eb5bca198df' AND default_prescription = 'DURATION') THEN
    RAISE EXCEPTION 'V52: ejercicio 813 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 814 flexion leg sit up (bent knee): requires_overhead: R2 L04 (MEDIA): Brazos por encima de la cabeza (tumbado, sin carga) en f1: falta OH. Por la coordinación y el equilibrio podría ser nivel 2 / V3 V05 ACEPTAR: f1 y f12 ampliados: tumbado en el suelo con los brazos extendidos por detrás de la cabeza a lo largo del suelo; f7 los brazos pasan al frente: brazos por encima | instructions_es: R2 L04 (MEDIA): El texto pide manos detrás de la cabeza con codos hacia fuera y empezar con ambas rodillas flexionadas y pies apoyados; el GIF muestra los brazos extendidos por / V3 V05 MEJORAR: f1 tumbado con una pierna estirada, la otra flexionada con el pie apoyado y brazos extendidos detrás de la cabeza; f7 sentado sobre los glúteos con brazos al fr
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = TRUE, instructions_es = 'Túmbate boca arriba en el suelo con una pierna estirada, la otra flexionada con el pie apoyado y los brazos extendidos por detrás de la cabeza. Activa el abdomen y eleva el tronco hasta quedar sentado sobre los glúteos, llevando los brazos estirados hacia delante. A la vez, flexiona las piernas y lleva una rodilla hacia el pecho con ese pie en el aire. Vuelve con control a la posición tumbada. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en el suelo con una pierna estirada, la otra flexionada con el pie apoyado y los brazos extendidos por detrás de la cabeza.", "Activa el abdomen y eleva el tronco hasta quedar sentado sobre los glúteos, llevando los brazos estirados hacia delante.", "A la vez, flexiona las piernas y lleva una rodilla hacia el pecho con ese pie en el aire.", "Vuelve con control a la posición tumbada.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 814 AND name_en = 'flexion leg sit up (bent knee)' AND requires_overhead = FALSE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '65951f47e8c577e024ff4a1c3655e9c7';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 814 AND name_en = 'flexion leg sit up (bent knee)' AND requires_overhead = TRUE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'db4e2582cff0a8fec4e288ff60940d9e') THEN
    RAISE EXCEPTION 'V52: ejercicio 814 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 815 flexion leg sit up (straight arm): instructions_es: R2 L05 (ALTA): El texto dice piernas extendidas y llevar las piernas hacia el pecho; el GIF muestra una sola pierna estirada que sube mientras la otra permanece flexionada con / V3 V05 ACEPTAR: f1 tumbado con brazos por encima de la cabeza, una rodilla flexionada con el pie apoyado y la otra pierna estirada; f7 sentado con la pierna estirada elevada ca
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en el suelo con los brazos extendidos por encima de la cabeza. Flexiona una rodilla y apoya ese pie en el suelo; mantén la otra pierna estirada. Eleva a la vez el tronco y la pierna estirada, llevando las manos hacia el pie elevado. Baja despacio hasta volver a tumbarte. Completa las repeticiones y cambia de pierna. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en el suelo con los brazos extendidos por encima de la cabeza.", "Flexiona una rodilla y apoya ese pie en el suelo; mantén la otra pierna estirada.", "Eleva a la vez el tronco y la pierna estirada, llevando las manos hacia el pie elevado.", "Baja despacio hasta volver a tumbarte.", "Completa las repeticiones y cambia de pierna.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 815 AND name_en = 'flexion leg sit up (straight arm)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '72a7f284dec721773294b07676300cf8';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 815 AND name_en = 'flexion leg sit up (straight arm)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '05a2d1e63854ff100d13cd04558004c8') THEN
    RAISE EXCEPTION 'V52: ejercicio 815 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 816 exercise ball on the wall calf raise: name_es: R2 L06 (ALTA): Nombre sin tilde en «Elevación». / V3 V05 ACEPTAR: f1-f12 elevación de talones de pie con mancuernas y balón contra la pared: el nombre describe el ejercicio; la corrección es solo la tilde de «Elevación» | instructions_es: R2 L06 (ALTA): El texto coloca el balón en la zona lumbar; en el GIF el balón está a la altura de la parte alta de la espalda. / V3 V05 MEJORAR: f1 y f7 ampliados: se ven la espalda y los glúteos y la cara mira hacia la pared; el balón queda entre la parte alta del pecho y la pared con el cuerpo inclinad
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Elevación de talones con balón de ejercicio en la pared', instructions_es = 'Ponte de pie de cara a una pared, con los pies algo separados de ella, y apoya un balón de ejercicio entre la parte alta del pecho y la pared. Inclina ligeramente el cuerpo recto hacia la pared. Sujeta una mancuerna en cada mano con los brazos extendidos a los lados. Eleva los talones del suelo quedando sobre la parte delantera de los pies. Baja los talones despacio a la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie de cara a una pared, con los pies algo separados de ella, y apoya un balón de ejercicio entre la parte alta del pecho y la pared.", "Inclina ligeramente el cuerpo recto hacia la pared.", "Sujeta una mancuerna en cada mano con los brazos extendidos a los lados.", "Eleva los talones del suelo quedando sobre la parte delantera de los pies.", "Baja los talones despacio a la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 816 AND name_en = 'exercise ball on the wall calf raise' AND name_es = 'Elevacion de talones con balón de ejercicio en la pared' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'c4cf6970538889f4af06d233127dfc83';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 816 AND name_en = 'exercise ball on the wall calf raise' AND name_es = 'Elevación de talones con balón de ejercicio en la pared' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'eddbaec3684c7a67bb49c8e3daa50259') THEN
    RAISE EXCEPTION 'V52: ejercicio 816 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 818 exercise ball on the wall calf raise (tennis ball between knees): instructions_es: R2 L08 (MEDIA): El texto coloca el balón entre la zona lumbar y la pared; en el GIF el balón queda a la altura de los hombros, en la parte alta del tronco / V3 V05 MEJORAR: f1 y f7 ampliados: vista por detrás, espalda y glúteos visibles, cara hacia la pared; balón entre el pecho y hombro y la pared; pelota amarilla entre las rodill
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie de cara a una pared, con los pies algo separados de ella, y apoya un balón de ejercicio entre la parte alta del pecho y la pared, a la altura de los hombros. Sujeta una mancuerna en cada mano con los brazos extendidos a los lados. Coloca una pelota de tenis entre las rodillas y mantenla sujeta durante todo el ejercicio. Eleva los talones del suelo subiendo sobre la punta de los pies. Baja los talones despacio hasta la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie de cara a una pared, con los pies algo separados de ella, y apoya un balón de ejercicio entre la parte alta del pecho y la pared, a la altura de los hombros.", "Sujeta una mancuerna en cada mano con los brazos extendidos a los lados.", "Coloca una pelota de tenis entre las rodillas y mantenla sujeta durante todo el ejercicio.", "Eleva los talones del suelo subiendo sobre la punta de los pies.", "Baja los talones despacio hasta la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 818 AND name_en = 'exercise ball on the wall calf raise (tennis ball between knees)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ab6c0c8c05862e467b262666ef2675ef';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 818 AND name_en = 'exercise ball on the wall calf raise (tennis ball between knees)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e36ed5adbffb70aac2012b4f7844df34') THEN
    RAISE EXCEPTION 'V52: ejercicio 818 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 821 exercise ball pike push up: instructions_es: R2 L11 (MEDIA): Indica flexionar los codos estando en la posición de pica; el GIF muestra la flexión de brazos en plancha y, después, la elevación de la cadera en pica con los  / V3 W04 MEJORAR: f1 plancha con brazos extendidos y puntas de los pies sobre el balón; f7 flexión de brazos en plancha; f13 pica con brazos extendidos y cadera alta; el balón no | movement_pattern: R2 L11 (MEDIA): Es una flexión de brazos en plancha combinada con una pica, no una flexión pica: el empuje es horizontal. El nombre 'pike push-up' induce a error (DUDA). Los pi / V3 W04 ACEPTAR: f7 la flexión de codos ocurre en plancha horizontal; en la pica de f13 los brazos están extendidos y no empujan: el empuje principal es horizontal
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate en plancha con las manos en el suelo bajo los hombros y las puntas de los pies apoyadas sobre un balón de estabilidad. Flexiona los codos y baja el pecho hacia el suelo, manteniendo el cuerpo en línea recta. Empuja hasta extender los brazos. Activa el core y, con las piernas rectas, eleva la cadera hacia el techo hasta formar una V invertida, con los pies sobre el balón. Baja la cadera con control hasta la plancha. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate en plancha con las manos en el suelo bajo los hombros y las puntas de los pies apoyadas sobre un balón de estabilidad.", "Flexiona los codos y baja el pecho hacia el suelo, manteniendo el cuerpo en línea recta.", "Empuja hasta extender los brazos.", "Activa el core y, con las piernas rectas, eleva la cadera hacia el techo hasta formar una V invertida, con los pies sobre el balón.", "Baja la cadera con control hasta la plancha.", "Repite el número de repeticiones indicado."]'::jsonb, movement_pattern = 'HORIZONTAL_PUSH'
   WHERE exercise_id = 821 AND name_en = 'exercise ball pike push up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'c44ad3f46f2b1b09d75e05910e6f3cbd' AND movement_pattern = 'VERTICAL_PUSH';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 821 AND name_en = 'exercise ball pike push up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '553c6f493f98515ec4ddf87e1380ef39' AND movement_pattern = 'HORIZONTAL_PUSH') THEN
    RAISE EXCEPTION 'V52: ejercicio 821 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 822 exercise ball lower back stretch (pyramid): requires_inversion: R2 L12 (MEDIA): Las rodillas no apoyan en ningún fotograma (piernas extendidas sobre las puntas de los pies): sobra KN. La cabeza queda claramente por debajo de la cadera con p / V3 W04 ACEPTAR: f1-f12 cadera en lo alto del balón y cabeza claramente más baja, cerca del suelo, con manos y antebrazos apoyados en el suelo cargando parte del peso; caso lími | requires_kneeling: R2 L12 (MEDIA): Las rodillas no apoyan en ningún fotograma (piernas extendidas sobre las puntas de los pies): sobra KN. La cabeza queda claramente por debajo de la cadera con p / V3 W04 ACEPTAR: f1 y f7 (y el resto, casi sin movimiento) piernas extendidas con las puntas de los pies en el suelo y las rodillas en el aire; ninguna rodilla apoya / coordinador: definicion literal demostrada en todos los fotogramas (R2 + V3)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_inversion = TRUE, requires_kneeling = FALSE
   WHERE exercise_id = 822 AND name_en = 'exercise ball lower back stretch (pyramid)' AND requires_inversion = FALSE AND requires_kneeling = TRUE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 822 AND name_en = 'exercise ball lower back stretch (pyramid)' AND requires_inversion = TRUE AND requires_kneeling = FALSE) THEN
    RAISE EXCEPTION 'V52: ejercicio 822 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 823 exercise ball lying side lat stretch: instructions_es: R2 L13 (MEDIA): El texto dice apoyar la cabeza sobre el balón y colocar el brazo de arriba sobre el balón; el GIF muestra el costado del tronco apoyado en el balón, la cabeza l / V3 W04 MEJORAR: f1 costado sobre el balón, cabeza libre, brazo de abajo rodeando el balón; f7 brazo de arriba extendido sobre la cabeza; una pierna extendida hasta el suelo y l
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate de lado sobre un balón de estabilidad, con el costado del tronco apoyado en él y una pierna extendida con el pie apoyado en el suelo. Rodea el balón con el brazo de abajo para mantener la estabilidad. Extiende el brazo de arriba por encima de la cabeza y deja que el costado se alargue sobre el balón. Respira con calma y siente el estiramiento en el costado de la espalda. Cambia de lado al terminar. Mantén la duración indicada.', instruction_steps_es = '["Colócate de lado sobre un balón de estabilidad, con el costado del tronco apoyado en él y una pierna extendida con el pie apoyado en el suelo.", "Rodea el balón con el brazo de abajo para mantener la estabilidad.", "Extiende el brazo de arriba por encima de la cabeza y deja que el costado se alargue sobre el balón.", "Respira con calma y siente el estiramiento en el costado de la espalda.", "Cambia de lado al terminar.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 823 AND name_en = 'exercise ball lying side lat stretch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b162c93e2af995a3f4a6a67277d3a55b';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 823 AND name_en = 'exercise ball lying side lat stretch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '8d245b5bbaf07455749131c1e51fb8aa') THEN
    RAISE EXCEPTION 'V52: ejercicio 823 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 825 exercise ball one leg prone lower body rotation: name_es: R2 L15 (MEDIA): Lo que gira es la cadera y la pierna, no el tronco. Los pies no tocan el suelo: un pie apoya en el balón (no encaja exactamente en PIES_SUSPENDIDOS, pero es un  / V3 W04 ACEPTAR: f1 plancha con manos en el suelo y un pie sobre el balón; f7 la rodilla libre cruza por debajo del cuerpo hacia el lado contrario: gira la cadera y la pierna, n
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Rotación de cadera en plancha con un pie sobre balón'
   WHERE exercise_id = 825 AND name_en = 'exercise ball one leg prone lower body rotation' AND name_es = 'Rotación de tronco con una pierna en balón';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 825 AND name_en = 'exercise ball one leg prone lower body rotation' AND name_es = 'Rotación de cadera en plancha con un pie sobre balón') THEN
    RAISE EXCEPTION 'V52: ejercicio 825 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 826 exercise ball one legged diagonal kick hamstring curl: instructions_es: R2 L16 (MEDIA): El texto parte con la cadera ya elevada, dice mantener extendida la pierna de apoyo y alternar las piernas; el GIF parte con la cadera abajo, la pierna que qued / V3 W04 ACEPTAR: f1 boca arriba con talones en el balón y cadera abajo; f7 cadera elevada, una rodilla flexionada con el talón en el balón y la otra pierna extendida hacia arrib
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en el suelo con los brazos a los lados y los talones apoyados sobre el balón de estabilidad. Eleva la cadera y, a la vez, flexiona una rodilla manteniendo el talón sobre el balón mientras subes la otra pierna extendida hacia arriba. Mantén el cuerpo estable un instante en la posición alta. Baja la cadera y la pierna con control hasta la posición inicial. Haz el mismo número de repeticiones con la otra pierna. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en el suelo con los brazos a los lados y los talones apoyados sobre el balón de estabilidad.", "Eleva la cadera y, a la vez, flexiona una rodilla manteniendo el talón sobre el balón mientras subes la otra pierna extendida hacia arriba.", "Mantén el cuerpo estable un instante en la posición alta.", "Baja la cadera y la pierna con control hasta la posición inicial.", "Haz el mismo número de repeticiones con la otra pierna.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 826 AND name_en = 'exercise ball one legged diagonal kick hamstring curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3889bbbd7972c341102489c5bc1f8333';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 826 AND name_en = 'exercise ball one legged diagonal kick hamstring curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'aa25cd85492b94aca204720768465c67') THEN
    RAISE EXCEPTION 'V52: ejercicio 826 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 830 ez barbell anti gravity press: axial_load: R2 L20 (MEDIA): Con el pecho apoyado en el banco y la barra empujada hacia delante no hay carga compresiva relevante sobre la columna: AX no corresponde. Dificultad 3 parece al / V3 W04 ACEPTAR: f1 y f7 pecho apoyado en el banco inclinado; la barra se empuja hacia delante hasta brazos casi horizontales: la carga queda en palanca delante del cuerpo, sin  / coordinador: definicion literal demostrada en todos los fotogramas (R2 + V3)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET axial_load = FALSE
   WHERE exercise_id = 830 AND name_en = 'ez barbell anti gravity press' AND axial_load = TRUE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 830 AND name_en = 'ez barbell anti gravity press' AND axial_load = FALSE) THEN
    RAISE EXCEPTION 'V52: ejercicio 830 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 831 ez bar lying bent arms pullover: instructions_es: R2 L01 (ALTA): El texto pide mantener solo una ligera flexión de codos; el GIF muestra un pullover con los codos muy flexionados (brazos doblados, como indica el nombre) / V3 V05 MEJORAR: f1 barra detrás de la cabeza por debajo del banco con los codos muy flexionados; f7 barra sobre el pecho con los brazos casi verticales y aparentemente más exte
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en un banco plano con la cabeza cerca del extremo y los pies apoyados en el suelo. Sujeta la barra EZ con ambas manos por encima del pecho, con los brazos verticales. Flexiona los codos y lleva la barra en arco por detrás de la cabeza hasta que quede por debajo del nivel del banco. Tira de la barra en arco hasta volver a situarla por encima del pecho. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en un banco plano con la cabeza cerca del extremo y los pies apoyados en el suelo.", "Sujeta la barra EZ con ambas manos por encima del pecho, con los brazos verticales.", "Flexiona los codos y lleva la barra en arco por detrás de la cabeza hasta que quede por debajo del nivel del banco.", "Tira de la barra en arco hasta volver a situarla por encima del pecho.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 831 AND name_en = 'ez bar lying bent arms pullover' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '2ed03fedeb2cd9af7ef5fb6c6e3fd64b';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 831 AND name_en = 'ez bar lying bent arms pullover' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '49b1294b1713571cb68ea2eab3fef409') THEN
    RAISE EXCEPTION 'V52: ejercicio 831 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 838 ez barbell close-grip curl: instructions_es: R2 L08 (ALTA): El texto indica manos separadas a la anchura de los hombros; el GIF y el nombre muestran un agarre estrecho, con las manos más juntas que los hombros / V3 V05 ACEPTAR: f1 y f7 ampliados: manos juntas en la zona central de la barra EZ, claramente más cerca que la anchura de los hombros; f7 barra a la altura de los hombros con c
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies separados a la anchura de la cadera y sujeta la barra EZ con las palmas hacia arriba y las manos juntas, más cerca que la anchura de los hombros. Mantén los codos cerca del tronco y la parte superior de los brazos quieta. Sube la barra hacia los hombros flexionando los codos. Baja la barra despacio hasta extender los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies separados a la anchura de la cadera y sujeta la barra EZ con las palmas hacia arriba y las manos juntas, más cerca que la anchura de los hombros.", "Mantén los codos cerca del tronco y la parte superior de los brazos quieta.", "Sube la barra hacia los hombros flexionando los codos.", "Baja la barra despacio hasta extender los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 838 AND name_en = 'ez barbell close-grip curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '53efa7edf2e1ff9856ebdbeec0bfca0a';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 838 AND name_en = 'ez barbell close-grip curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '1ee3f579296a219a19a0e53318461a9c') THEN
    RAISE EXCEPTION 'V52: ejercicio 838 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 843 ez barbell jm bench press: instructions_es: R2 L13 (MEDIA): El texto dice apoyar los pies en el suelo y bajar la barra al pecho con los codos pegados; el GIF muestra los pies sobre el banco y la barra bajando hacia el cu / V3 W04 ACEPTAR: f1 y f7 rodillas flexionadas y pies sobre el banco; f7 la barra baja hacia el cuello y la parte alta del pecho con los codos al frente. El texto actual dice pie
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en un banco plano con las rodillas flexionadas y los pies apoyados sobre el banco. Sujeta la barra EZ con agarre prono y extiende los brazos por encima del pecho. Flexiona los codos, apuntándolos hacia delante, y baja la barra con control hacia la parte alta del pecho y el cuello. Empuja la barra hasta extender de nuevo los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en un banco plano con las rodillas flexionadas y los pies apoyados sobre el banco.", "Sujeta la barra EZ con agarre prono y extiende los brazos por encima del pecho.", "Flexiona los codos, apuntándolos hacia delante, y baja la barra con control hacia la parte alta del pecho y el cuello.", "Empuja la barra hasta extender de nuevo los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 843 AND name_en = 'ez barbell jm bench press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '771e6dcc35bfa40960d351248173122f';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 843 AND name_en = 'ez barbell jm bench press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '6fe818c7a93c31ce6e35095e3051ef8c') THEN
    RAISE EXCEPTION 'V52: ejercicio 843 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 846 ez barbell seated curls: instructions_es: R2 L16 (ALTA): El texto dice apoyar la parte superior de los brazos en los muslos; en el GIF la parte superior de los brazos queda vertical junto al tronco, sin apoyarse en lo / V3 W04 ACEPTAR: f1 sentado en el extremo del banco con brazos extendidos y barra en el regazo; f7 barra al pecho con la parte superior de los brazos vertical junto al tronco, s
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en el extremo de un banco con la espalda recta y los pies apoyados en el suelo. Sujeta la barra EZ con agarre supino, con las manos separadas a la altura de los hombros y los brazos extendidos junto al cuerpo. Flexiona los codos y sube la barra hacia el pecho sin mover la parte superior de los brazos. Bájala con control hasta extender los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en el extremo de un banco con la espalda recta y los pies apoyados en el suelo.", "Sujeta la barra EZ con agarre supino, con las manos separadas a la altura de los hombros y los brazos extendidos junto al cuerpo.", "Flexiona los codos y sube la barra hacia el pecho sin mover la parte superior de los brazos.", "Bájala con control hasta extender los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 846 AND name_en = 'ez barbell seated curls' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'dbc0e0677cc8932da5269be8ef8d22cf';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 846 AND name_en = 'ez barbell seated curls' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '2b3fe83e1999920978e8bdfa7592f198') THEN
    RAISE EXCEPTION 'V52: ejercicio 846 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 854 high knee against wall: instructions_es: norma de texto del cierre: la frase final pedia repeticiones («Repite durante la duración o el número de repeticiones deseado.») y la prescripcion vigente es DURATION, que la R2 (animacion completa) no propuso cambiar; solo se sustituye la frase final
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie frente a una pared con los pies separados a la altura de las caderas. Coloca las manos en la pared para mayor apoyo. Activa el core y levanta la rodilla derecha hacia el pecho, mientras mantienes el pie izquierdo en el suelo. Cambia rápidamente de pierna, llevando la rodilla izquierda hacia el pecho y bajando el pie derecho de nuevo. Continúa alternando las piernas con un movimiento de carrera, llevando las rodillas lo más alto posible. Mantén un ritmo rápido y la parte superior del cuerpo estable durante todo el ejercicio. Mantén la duración indicada.', instruction_steps_es = '["Ponte de pie frente a una pared con los pies separados a la altura de las caderas.", "Coloca las manos en la pared para mayor apoyo.", "Activa el core y levanta la rodilla derecha hacia el pecho, mientras mantienes el pie izquierdo en el suelo.", "Cambia rápidamente de pierna, llevando la rodilla izquierda hacia el pecho y bajando el pie derecho de nuevo.", "Continúa alternando las piernas con un movimiento de carrera, llevando las rodillas lo más alto posible.", "Mantén un ritmo rápido y la parte superior del cuerpo estable durante todo el ejercicio.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 854 AND name_en = 'high knee against wall' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a83442945b525b4498fd7502107435ff';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 854 AND name_en = 'high knee against wall' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e3c40cfce1442e6f7dbb1755a11cb4f7') THEN
    RAISE EXCEPTION 'V52: ejercicio 854 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 855 full planche push-up: instructions_es: R2 L05 (ALTA): El texto empieza en flexión con los pies en el suelo y los levanta al bajar; el GIF muestra los pies sin apoyo durante todo el ejercicio / V3 V05 ACEPTAR: f1-f12 los pies nunca apoyan; f1 brazos extendidos, f7 codos flexionados con el pecho cerca del suelo; el texto actual empieza con los pies en el suelo y los le
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate con las manos en el suelo y los brazos extendidos, inclina el tronco hacia delante y sostén el cuerpo horizontal sin apoyar los pies. Mantén el cuerpo recto desde la cabeza hasta los talones y el abdomen firme. Flexiona los codos y baja el cuerpo hasta que el pecho quede cerca del suelo. Empuja con las manos hasta extender los brazos sin dejar caer las piernas. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate con las manos en el suelo y los brazos extendidos, inclina el tronco hacia delante y sostén el cuerpo horizontal sin apoyar los pies.", "Mantén el cuerpo recto desde la cabeza hasta los talones y el abdomen firme.", "Flexiona los codos y baja el cuerpo hasta que el pecho quede cerca del suelo.", "Empuja con las manos hasta extender los brazos sin dejar caer las piernas.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 855 AND name_en = 'full planche push-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a059695215a1fa9ae112c184773436f6';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 855 AND name_en = 'full planche push-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ae81076219788e53ace6277986c34d01') THEN
    RAISE EXCEPTION 'V52: ejercicio 855 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 857 frog planche: requires_inversion: R2 L07 (MEDIA): La cabeza queda claramente por debajo de la cadera con el peso en los brazos: falta INV. El GIF es una posición mantenida, por eso DURATION. Apoyo suspendido so / V3 V05 ACEPTAR: f1-f12 manos en el suelo con codos flexionados, rodillas sobre los brazos, cadera arriba y cabeza junto al suelo por debajo de la cadera, pies en el aire: peso  | instructions_es: R2 L07 (MEDIA): El texto describe partir de una flexión, bajar y recoger las rodillas y volver a la flexión en repeticiones; el GIF solo muestra el mantenimiento estático del e / V3 V05 MEJORAR: f1-f12 equilibrio estático sobre las manos con las rodillas sobre los brazos y los pies en el aire; la propuesta es correcta salvo que fija la anchura de las ma | default_prescription: R2 L07 (MEDIA): La cabeza queda claramente por debajo de la cadera con el peso en los brazos: falta INV. El GIF es una posición mantenida, por eso DURATION. Apoyo suspendido so / V3 V05 ACEPTAR: f1-f12 la postura no cambia, solo el color muscular: posición mantenida
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_inversion = TRUE, instructions_es = 'Ponte en cuclillas y apoya las manos en el suelo delante de los pies. Flexiona los codos y coloca las rodillas sobre la parte posterior de los brazos. Lleva el peso hacia delante sobre las manos hasta despegar los pies del suelo, con la cabeza más baja que la cadera. Mantén el equilibrio con el core activado y, al terminar, vuelve a apoyar los pies con control. Mantén la duración indicada.', instruction_steps_es = '["Ponte en cuclillas y apoya las manos en el suelo delante de los pies.", "Flexiona los codos y coloca las rodillas sobre la parte posterior de los brazos.", "Lleva el peso hacia delante sobre las manos hasta despegar los pies del suelo, con la cabeza más baja que la cadera.", "Mantén el equilibrio con el core activado y, al terminar, vuelve a apoyar los pies con control.", "Mantén la duración indicada."]'::jsonb, default_prescription = 'DURATION'
   WHERE exercise_id = 857 AND name_en = 'frog planche' AND requires_inversion = FALSE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '1c588232e39379780eda6afe207085ff' AND default_prescription = 'SETS_REPS';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 857 AND name_en = 'frog planche' AND requires_inversion = TRUE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '084f86c93a0bf04daa5c92660c45cad9' AND default_prescription = 'DURATION') THEN
    RAISE EXCEPTION 'V52: ejercicio 857 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 858 front lever: instructions_es: R2 L08 (MEDIA): El texto describe una progresión desde colgado con las rodillas al pecho hasta la palanca y pide repeticiones; el GIF muestra solo el mantenimiento estático de  / V3 V05 ACEPTAR: f1-f12 palanca frontal estática: brazos extendidos hacia los agarres por encima, cuerpo horizontal y piernas extendidas sin apoyo; no hay progresión con rodilla | default_prescription: R2 L08 (MEDIA): Prescripción propuesta: DURATION (isometría). El apoyo es una estructura con dos agarres, no se ve una barra de dominadas convencional. OH se mantiene porque la / V3 V05 ACEPTAR: f1-f12 la postura no cambia, solo el color muscular: isometría mantenida
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Sujétate con ambas manos a los agarres situados por encima de ti, con los brazos extendidos y los pies sin apoyo. Activa el core y lleva los omóplatos hacia abajo y atrás. Eleva el cuerpo hasta dejarlo horizontal, en línea recta desde los hombros hasta los pies, con las piernas extendidas. Mantén la posición sin arquear la zona lumbar. Baja las piernas de forma controlada para salir. Mantén la duración indicada.', instruction_steps_es = '["Sujétate con ambas manos a los agarres situados por encima de ti, con los brazos extendidos y los pies sin apoyo.", "Activa el core y lleva los omóplatos hacia abajo y atrás.", "Eleva el cuerpo hasta dejarlo horizontal, en línea recta desde los hombros hasta los pies, con las piernas extendidas.", "Mantén la posición sin arquear la zona lumbar.", "Baja las piernas de forma controlada para salir.", "Mantén la duración indicada."]'::jsonb, default_prescription = 'DURATION'
   WHERE exercise_id = 858 AND name_en = 'front lever' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3048ba64661fb70ea50af53dfa14a6d3' AND default_prescription = 'SETS_REPS';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 858 AND name_en = 'front lever' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd91e4fe7319e9a665f56b09e17240b8d' AND default_prescription = 'DURATION') THEN
    RAISE EXCEPTION 'V52: ejercicio 858 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 859 front plank with twist: instructions_es: R2 L09 (ALTA): Dice empezar en plancha alta con las manos debajo de los hombros, pero el GIF muestra una plancha sobre los antebrazos; el giro también se hace sobre el antebra / V3 V05 ACEPTAR: f1 plancha sobre los antebrazos con codos bajo los hombros; f7 giro a plancha lateral sobre un antebrazo con el otro brazo vertical hacia el techo; el texto act | default_prescription: R2 L09 (ALTA): Es un movimiento dinámico por repeticiones (giro y vuelta), por lo que SETS_REPS encaja mejor que DURATION; el propio texto habla de repeticiones. El brazo haci / V3 V05 ACEPTAR: f1-f7-f12 fundido de ida y vuelta entre plancha sobre antebrazos y plancha lateral con brazo al techo: movimiento dinámico repetido
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate en plancha sobre los antebrazos, con los codos bajo los hombros y el cuerpo en línea recta de la cabeza a los pies. Activa el core y los glúteos para mantener la cadera estable. Gira el cuerpo hacia un lado quedando apoyado sobre un antebrazo y el borde de los pies, y extiende el otro brazo hacia el techo. Vuelve con control a la plancha sobre los antebrazos y repite hacia el otro lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate en plancha sobre los antebrazos, con los codos bajo los hombros y el cuerpo en línea recta de la cabeza a los pies.", "Activa el core y los glúteos para mantener la cadera estable.", "Gira el cuerpo hacia un lado quedando apoyado sobre un antebrazo y el borde de los pies, y extiende el otro brazo hacia el techo.", "Vuelve con control a la plancha sobre los antebrazos y repite hacia el otro lado.", "Repite el número de repeticiones indicado."]'::jsonb, default_prescription = 'SETS_REPS'
   WHERE exercise_id = 859 AND name_en = 'front plank with twist' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '9d113b48a9a4c19a5c8f7470b317bce8' AND default_prescription = 'DURATION';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 859 AND name_en = 'front plank with twist' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '49e99f69b54819f3b9ae171a11bf82b7' AND default_prescription = 'SETS_REPS') THEN
    RAISE EXCEPTION 'V52: ejercicio 859 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 860 full maltese: instructions_es: R2 L10 (MEDIA): El texto describe repeticiones de despegar y volver a apoyar los pies; el GIF solo muestra la posición mantenida de forma estática / V3 V05 ACEPTAR: f1-f12 posición mantenida sin despegar ni apoyar los pies; la propuesta describe la postura vista y acaba con la frase de DURATION | default_prescription: R2 L10 (MEDIA): Prescripción: es un sostenimiento isométrico, propongo DURATION. Apoyo suspendido sobre brazos extendidos con las manos en el suelo. Implica sobre todo hombros  / V3 V05 ACEPTAR: f1-f12 la postura no cambia, solo el color muscular: isometría mantenida
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate boca abajo con las manos apoyadas en el suelo bajo el tronco, cerca de la cintura, y los brazos extendidos. Inclina el cuerpo hacia delante sobre las manos hasta despegar los pies del suelo. Mantén el cuerpo recto y horizontal, muy cerca del suelo, con el core y los glúteos activos. Vuelve a apoyar los pies con control al terminar. Mantén la duración indicada.', instruction_steps_es = '["Colócate boca abajo con las manos apoyadas en el suelo bajo el tronco, cerca de la cintura, y los brazos extendidos.", "Inclina el cuerpo hacia delante sobre las manos hasta despegar los pies del suelo.", "Mantén el cuerpo recto y horizontal, muy cerca del suelo, con el core y los glúteos activos.", "Vuelve a apoyar los pies con control al terminar.", "Mantén la duración indicada."]'::jsonb, default_prescription = 'DURATION'
   WHERE exercise_id = 860 AND name_en = 'full maltese' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '0194d1a77fa1760f6d85b89c338ea84c' AND default_prescription = 'SETS_REPS';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 860 AND name_en = 'full maltese' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b064ede18f1095d98eb831c07bf1d60c' AND default_prescription = 'DURATION') THEN
    RAISE EXCEPTION 'V52: ejercicio 860 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 861 full planche: instructions_es: R2 L11 (MEDIA): Describe una entrada desde flexión y termina con repeticiones; el GIF solo muestra la posición mantenida y la prescripción adecuada es por tiempo / V3 W04 ACEPTAR: f1-f12 solo se ve la posición mantenida (la entrada no se ve en ninguno de los dos textos); el texto debe terminar con la frase de duración coherente con la pre | default_prescription: R2 L11 (MEDIA): Prescripción: es una posición isométrica, debería ser DURATION. El resaltado incluye hombros, serrato y abdomen; el motor principal es el hombro. La entrada a l / V3 W04 ACEPTAR: f1-f12 posición isométrica idéntica en todos los fotogramas: se sostiene por tiempo
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate boca abajo con las manos en el suelo a la anchura de los hombros y los brazos extendidos. Activa el core e inclina el peso hacia delante sobre las manos hasta despegar los pies del suelo. Mantén el cuerpo recto y paralelo al suelo, sostenido solo con los brazos extendidos. Apoya los pies con control para terminar. Mantén la duración indicada.', instruction_steps_es = '["Colócate boca abajo con las manos en el suelo a la anchura de los hombros y los brazos extendidos.", "Activa el core e inclina el peso hacia delante sobre las manos hasta despegar los pies del suelo.", "Mantén el cuerpo recto y paralelo al suelo, sostenido solo con los brazos extendidos.", "Apoya los pies con control para terminar.", "Mantén la duración indicada."]'::jsonb, default_prescription = 'DURATION'
   WHERE exercise_id = 861 AND name_en = 'full planche' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3b67c547c02905aa4aa13acfe36e9a2c' AND default_prescription = 'SETS_REPS';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 861 AND name_en = 'full planche' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '71c6b3ef82d31a2d6f3368490d1ac34b' AND default_prescription = 'DURATION') THEN
    RAISE EXCEPTION 'V52: ejercicio 861 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 862 gorilla chin: instructions_es: R2 L12 (ALTA): El texto empieza de pie y describe solo una dominada; el GIF muestra que durante la subida las rodillas se llevan al pecho, que es la parte característica del e / V3 W04 ACEPTAR: f1 colgado con brazos extendidos y rodillas flexionadas; f7 barbilla a la barra con las rodillas llevadas al pecho. El texto actual empieza de pie y describe so
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Cuélgate de una barra de dominadas con los brazos extendidos y las rodillas flexionadas. Activa el abdomen y tira del cuerpo hacia arriba hasta acercar la barbilla a la barra. A la vez, lleva las rodillas hacia el pecho. Baja con control hasta extender los brazos, dejando que las piernas vuelvan a colgar. Repite el número de repeticiones indicado.', instruction_steps_es = '["Cuélgate de una barra de dominadas con los brazos extendidos y las rodillas flexionadas.", "Activa el abdomen y tira del cuerpo hacia arriba hasta acercar la barbilla a la barra.", "A la vez, lleva las rodillas hacia el pecho.", "Baja con control hasta extender los brazos, dejando que las piernas vuelvan a colgar.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 862 AND name_en = 'gorilla chin' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '474a349811d4c3c04b3d5c60a059e356';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 862 AND name_en = 'gorilla chin' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b96482ec3de998990c89b9e1ee169157') THEN
    RAISE EXCEPTION 'V52: ejercicio 862 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 863 groin crunch: instructions_es: R2 L13 (ALTA): El texto dice colocar las manos junto a la cabeza; el GIF muestra los brazos cruzados sobre el pecho / V3 W04 ACEPTAR: f1 y f7 brazos cruzados sobre el pecho, manos hacia los hombros contrarios, con las plantas juntas y rodillas abiertas. El texto actual dice manos junto a la ca
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en el suelo con las plantas de los pies juntas y las rodillas abiertas hacia los lados. Cruza los brazos sobre el pecho. Eleva la cabeza y los hombros contrayendo el abdomen. Baja con control hasta la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en el suelo con las plantas de los pies juntas y las rodillas abiertas hacia los lados.", "Cruza los brazos sobre el pecho.", "Eleva la cabeza y los hombros contrayendo el abdomen.", "Baja con control hasta la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 863 AND name_en = 'groin crunch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b88c6dcd9e97a30150ecb9ca19c865f0';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 863 AND name_en = 'groin crunch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'fb21a610015a796e8f426d2419caeabf') THEN
    RAISE EXCEPTION 'V52: ejercicio 863 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 864 half sit-up (male): name_es: R2 L14 (ALTA): El GIF no muestra un abdominal medio sino un recorrido completo. Con piernas estiradas y sin sujetar los pies podría ser 2 / V3 W04 ACEPTAR: f1 tumbado con piernas estiradas; f5-f8 tronco vertical sentado: recorrido completo, no medio abdominal | instructions_es: R2 L14 (ALTA): El texto pide rodillas flexionadas, manos detrás de la cabeza y subir a 45 grados; en el GIF las piernas están estiradas, los brazos a los lados y el tronco lle / V3 W04 MEJORAR: f1 brazos a los lados con las manos junto a los muslos; f7 sentado con tronco vertical y manos en el suelo junto a la cadera; piernas estiradas en el suelo. Las
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Abdominal completo con piernas estiradas', instructions_es = 'Túmbate boca arriba en el suelo con las piernas estiradas y los brazos extendidos a los lados del cuerpo. Activa el abdomen y eleva el tronco con control, sin despegar las piernas del suelo, hasta quedar sentado con el tronco vertical. Baja con control hasta volver a tumbarte. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en el suelo con las piernas estiradas y los brazos extendidos a los lados del cuerpo.", "Activa el abdomen y eleva el tronco con control, sin despegar las piernas del suelo, hasta quedar sentado con el tronco vertical.", "Baja con control hasta volver a tumbarte.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 864 AND name_en = 'half sit-up (male)' AND name_es = 'Abdominal medio' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '800e583ab286e5d9c2adeb823a56752a';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 864 AND name_en = 'half sit-up (male)' AND name_es = 'Abdominal completo con piernas estiradas' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '652d0598aa33c551bb09696c533df45e') THEN
    RAISE EXCEPTION 'V52: ejercicio 864 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 865 hanging leg hip raise: difficulty_level: R2 L15 (ALTA): suspensión del peso corporal; agarre / V3 W04 ACEPTAR: f1-f12 colgado de la barra soportando el peso corporal en suspensión durante todo el ejercicio; el nivel 1 excluye la suspensión
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 865 AND name_en = 'hanging leg hip raise' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 865 AND name_en = 'hanging leg hip raise' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 865 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 866 hanging leg raise: name_es: R2 L16 (ALTA): La dificultad 1 exige no soportar el peso corporal en suspensión; aquí se cuelga de la barra durante todo el ejercicio, propuesta 2. El agarre prono no se disti / V3 W04 ACEPTAR: f1 colgado con piernas extendidas; f7 rodillas flexionadas con muslos horizontales y espinillas verticales: es una elevación de rodillas | instructions_es: R2 L16 (ALTA): El texto pide subir las piernas rectas hasta la horizontal; el GIF muestra las rodillas flexionadas (elevación de rodillas) / V3 W04 ACEPTAR: f7 rodillas flexionadas con muslos paralelos al suelo; el texto actual pide piernas rectas | difficulty_level: R2 L16 (ALTA): suspensión por agarre / V3 W04 ACEPTAR: f1-f12 suspensión del peso corporal colgado de la barra todo el tiempo; el nivel 1 excluye la suspensión
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Elevación de rodillas colgado', instructions_es = 'Cuélgate de una barra con los brazos completamente extendidos. Activa el core y eleva las rodillas flexionadas hasta que los muslos queden paralelos al suelo. Haz una pausa breve en la parte alta. Baja las piernas con control hasta colgar de nuevo, sin balancearte. Repite el número de repeticiones indicado.', instruction_steps_es = '["Cuélgate de una barra con los brazos completamente extendidos.", "Activa el core y eleva las rodillas flexionadas hasta que los muslos queden paralelos al suelo.", "Haz una pausa breve en la parte alta.", "Baja las piernas con control hasta colgar de nuevo, sin balancearte.", "Repite el número de repeticiones indicado."]'::jsonb, difficulty_level = 2
   WHERE exercise_id = 866 AND name_en = 'hanging leg raise' AND name_es = 'Elevación de piernas colgado' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '09eb203afa6041588a97120bd9029c8b' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 866 AND name_en = 'hanging leg raise' AND name_es = 'Elevación de rodillas colgado' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '755cb84c1a1bf250a348a44362ad8626' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 866 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 867 hanging oblique knee raise: difficulty_level: R2 L17 (ALTA): suspensión; fuerza de agarre / V3 W04 ACEPTAR: f1-f24 suspensión del peso corporal colgado de la barra con elevaciones oblicuas alternas; el nivel 1 excluye la suspensión
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 867 AND name_en = 'hanging oblique knee raise' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 867 AND name_en = 'hanging oblique knee raise' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 867 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 868 hanging pike: instructions_es: R2 L18 (ALTA): El texto dice elevar las piernas rectas hasta formar una V con las piernas paralelas al suelo; el GIF sube mucho más, con las piernas apuntando hacia arriba y l / V3 W04 MEJORAR: f1 colgado con rodillas flexionadas; f5-f7 piernas extendidas verticales con los pies por encima de la barra. El texto actual se queda en piernas paralelas al s
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Cuélgate de una barra de dominadas con los brazos extendidos y las rodillas ligeramente flexionadas. Activa el core y eleva las piernas hacia la barra, extendiéndolas durante la subida. Continúa hasta que las piernas queden verticales y los pies sobrepasen la barra. Baja con control hasta volver a colgar, sin balancearte. Repite el número de repeticiones indicado.', instruction_steps_es = '["Cuélgate de una barra de dominadas con los brazos extendidos y las rodillas ligeramente flexionadas.", "Activa el core y eleva las piernas hacia la barra, extendiéndolas durante la subida.", "Continúa hasta que las piernas queden verticales y los pies sobrepasen la barra.", "Baja con control hasta volver a colgar, sin balancearte.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 868 AND name_en = 'hanging pike' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '402a2db98213391ab41569d3afa33463';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 868 AND name_en = 'hanging pike' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5b367109a50f66766123078ee94ad35e') THEN
    RAISE EXCEPTION 'V52: ejercicio 868 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 869 hanging straight leg hip raise: instructions_es: R2 L19 (ALTA): El texto dice subir las piernas hasta que queden paralelas al suelo; el GIF muestra las piernas subiendo mucho más, hasta llevar los pies junto a la barra con l / V3 Y01 ACEPTAR: f1 colgado con brazos estirados y pies sin apoyo; f6-f8 piernas estiradas con los pies junto a la barra y la cadera enrollada, mucho más allá de la horizontal | difficulty_level: R2 L19 (ALTA): suspensión de agarre; fuerza relativa alta de core / V3 Y01 ACEPTAR: f1-f12 colgado de la barra sin apoyo de pies; f7 pies a la barra con piernas rectas: suspensión y fuerza relativa alta de core
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Cuélgate de una barra de dominadas con los brazos completamente estirados y los pies sin apoyo. Activa el core y, con las piernas estiradas, elévalas por delante del cuerpo enrollando la cadera hasta acercar los pies a la barra. Baja las piernas despacio y sin balanceo hasta la posición colgada. Repite el número de repeticiones indicado.', instruction_steps_es = '["Cuélgate de una barra de dominadas con los brazos completamente estirados y los pies sin apoyo.", "Activa el core y, con las piernas estiradas, elévalas por delante del cuerpo enrollando la cadera hasta acercar los pies a la barra.", "Baja las piernas despacio y sin balanceo hasta la posición colgada.", "Repite el número de repeticiones indicado."]'::jsonb, difficulty_level = 2
   WHERE exercise_id = 869 AND name_en = 'hanging straight leg hip raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b80ecb247d5eb3943ae10c140ceba9e2' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 869 AND name_en = 'hanging straight leg hip raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'be94ede2ef5da5093681534dc45d498f' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 869 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 870 hanging straight leg raise: difficulty_level: R2 L20 (ALTA): suspensión con agarre; fuerza de core y flexores de cadera / V3 W04 ACEPTAR: f1-f12 colgado de la barra en suspensión, elevando las piernas rectas hasta la horizontal (f7); el nivel 1 excluye la suspensión
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 870 AND name_en = 'hanging straight leg raise' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 870 AND name_en = 'hanging straight leg raise' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 870 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 871 hanging straight twisting leg hip raise: instructions_es: R2 L01 (MEDIA): El texto pide colgar con los brazos completamente extendidos; en el GIF los codos permanecen flexionados con la cabeza cerca de la barra. Además la alternancia  / V3 V05 MEJORAR: f1 y f7 ampliados: colgado con los codos claramente flexionados y la cabeza justo bajo la barra (no ligeramente); f1 piernas elevadas hacia un lado y f7 hacia e | difficulty_level: R2 L01 (MEDIA): suspensión exigente; fuerza de core alta con piernas rectas / V3 V05 ACEPTAR: f1-f12 todo el peso colgado de las manos con los codos flexionados y las piernas rectas elevadas y giradas: suspensión exigente, incompatible con el nivel 1
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Cuélgate de una barra con los codos flexionados y la cabeza cerca de la barra, sin apoyar los pies. Activa el core y eleva las piernas rectas hacia delante. Gira la cadera para llevar las piernas elevadas hacia un lado. Llévalas con control hacia el otro lado sin balancearte. Alterna los lados. Repite el número de repeticiones indicado.', instruction_steps_es = '["Cuélgate de una barra con los codos flexionados y la cabeza cerca de la barra, sin apoyar los pies.", "Activa el core y eleva las piernas rectas hacia delante.", "Gira la cadera para llevar las piernas elevadas hacia un lado.", "Llévalas con control hacia el otro lado sin balancearte.", "Alterna los lados.", "Repite el número de repeticiones indicado."]'::jsonb, difficulty_level = 2
   WHERE exercise_id = 871 AND name_en = 'hanging straight twisting leg hip raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e32ca6170519d90959ba0b026df50d9d' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 871 AND name_en = 'hanging straight twisting leg hip raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '2a9944e1da06b2ea93f6be5f6211c854' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 871 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 873 front lever reps: instructions_es: R2 L03 (ALTA): El texto pide subir las piernas hasta la horizontal y mantenerla 10-20 segundos (dosis concreta); el GIF muestra el cuerpo ya en horizontal que sube y baja haci / V3 V05 ACEPTAR: f1 cuerpo horizontal con brazos extendidos; f7 mismo cuerpo horizontal elevado hacia los agarres con codos flexionados; no hay elevación de piernas ni mantenimi
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Cuélgate de una barra alta con agarre prono y los brazos extendidos. Activa el abdomen y los glúteos y lleva el cuerpo recto a la horizontal, en posición de palanca frontal. Manteniendo el cuerpo horizontal, tira de él hacia la barra flexionando los codos. Baja con control hasta extender los brazos sin perder la horizontal. Repite el número de repeticiones indicado.', instruction_steps_es = '["Cuélgate de una barra alta con agarre prono y los brazos extendidos.", "Activa el abdomen y los glúteos y lleva el cuerpo recto a la horizontal, en posición de palanca frontal.", "Manteniendo el cuerpo horizontal, tira de él hacia la barra flexionando los codos.", "Baja con control hasta extender los brazos sin perder la horizontal.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 873 AND name_en = 'front lever reps' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '4568418b5769225bfb624c983391326a';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 873 AND name_en = 'front lever reps' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'da5a7d73ceb043d3fbb0f8df1f8e924d') THEN
    RAISE EXCEPTION 'V52: ejercicio 873 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 876 forward jump: requires_overhead: R2 L06 (ALTA): Durante el vuelo los brazos quedan extendidos por encima de la cabeza (f5-f8): falta OH. Es un salto horizontal con parada, no un cardio rítmico continuo: encaj / V3 V05 ACEPTAR: f1 cuclillas con brazos atrás; f5-f8 cuerpo extendido en vuelo con los brazos extendidos verticalmente por encima de la cabeza
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = TRUE
   WHERE exercise_id = 876 AND name_en = 'forward jump' AND requires_overhead = FALSE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 876 AND name_en = 'forward jump' AND requires_overhead = TRUE) THEN
    RAISE EXCEPTION 'V52: ejercicio 876 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 880 glute-ham raise: instructions_es: R2 L10 (ALTA): El texto dice colocarse boca abajo y elevar el tronco hacia el techo; el GIF empieza arrodillado en vertical, baja el cuerpo recto hacia delante y vuelve a subi / V3 V05 ACEPTAR: f1 arrodillado erguido sobre la almohadilla con los tobillos bajo los rodillos y brazos cruzados al pecho; f7 cuerpo recto inclinado hacia delante cerca de la h
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca las rodillas sobre la almohadilla del banco GHD y sujeta los tobillos bajo los rodillos. Ponte erguido de rodillas con el cuerpo en línea recta de las rodillas a la cabeza y los brazos cruzados sobre el pecho. Inclínate hacia delante con el cuerpo recto, frenando con los isquiotibiales, hasta acercarte a la horizontal. Contrae los isquiotibiales y los glúteos para volver a la posición erguida. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca las rodillas sobre la almohadilla del banco GHD y sujeta los tobillos bajo los rodillos.", "Ponte erguido de rodillas con el cuerpo en línea recta de las rodillas a la cabeza y los brazos cruzados sobre el pecho.", "Inclínate hacia delante con el cuerpo recto, frenando con los isquiotibiales, hasta acercarte a la horizontal.", "Contrae los isquiotibiales y los glúteos para volver a la posición erguida.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 880 AND name_en = 'glute-ham raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '591383c9181c5df7f534e15b15129193';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 880 AND name_en = 'glute-ham raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'eee084832e3475efda76298dbffa1198') THEN
    RAISE EXCEPTION 'V52: ejercicio 880 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 882 hands clasped circular toe touch (male): instructions_es: R2 L12 (MEDIA): El texto coloca las manos entrelazadas frente al pecho y pide inclinarse con la espalda recta; el GIF empieza con los brazos extendidos por encima de la cabeza  / V3 W04 MEJORAR: f1 de pie con brazos sobre la cabeza y manos entrelazadas; f7-f19 tronco flexionado y redondeado, la cabeza se desplaza en círculo; f22-f25 sube con los brazos 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies algo separados y los brazos extendidos por encima de la cabeza, con las manos entrelazadas. Flexiona el tronco hacia delante y hacia abajo, redondeando la espalda, y lleva las manos hacia los pies sin forzar. Desde abajo, gira el tronco describiendo un círculo y sube con los brazos extendidos al frente hasta quedar erguido. Completa el círculo moviendo los brazos alrededor de la cabeza hasta extenderlos de nuevo por encima de ella, despacio y sin rebotes. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies algo separados y los brazos extendidos por encima de la cabeza, con las manos entrelazadas.", "Flexiona el tronco hacia delante y hacia abajo, redondeando la espalda, y lleva las manos hacia los pies sin forzar.", "Desde abajo, gira el tronco describiendo un círculo y sube con los brazos extendidos al frente hasta quedar erguido.", "Completa el círculo moviendo los brazos alrededor de la cabeza hasta extenderlos de nuevo por encima de ella, despacio y sin rebotes.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 882 AND name_en = 'hands clasped circular toe touch (male)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '76256293d6fe0df598c499d4a8cbf303';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 882 AND name_en = 'hands clasped circular toe touch (male)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b57ae32866c65177e2b9d687cbd61fe8') THEN
    RAISE EXCEPTION 'V52: ejercicio 882 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 883 hands reversed clasped circular toe touch (male): instructions_es: R2 L13 (MEDIA): El texto dice empezar con los brazos en cruz y entrelazar las manos detrás de las piernas; el GIF empieza de pie con los brazos sobre la cabeza y las manos junt / V3 W04 ACEPTAR: f1 brazos sobre la cabeza con manos juntas; f7-f19 tronco flexionado con las manos hacia los pies; f22-f25 sube en arco con los brazos extendidos al frente; f34
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies a la anchura de las caderas y los brazos extendidos por encima de la cabeza, con las manos juntas. Inclínate hacia delante desde la cadera y lleva las manos hacia los pies con las rodillas casi rectas. Sube el tronco describiendo un arco con los brazos extendidos al frente hasta volver a tener los brazos sobre la cabeza. Completa el círculo moviendo los brazos alrededor de la cabeza, despacio y sin rebotes. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies a la anchura de las caderas y los brazos extendidos por encima de la cabeza, con las manos juntas.", "Inclínate hacia delante desde la cadera y lleva las manos hacia los pies con las rodillas casi rectas.", "Sube el tronco describiendo un arco con los brazos extendidos al frente hasta volver a tener los brazos sobre la cabeza.", "Completa el círculo moviendo los brazos alrededor de la cabeza, despacio y sin rebotes.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 883 AND name_en = 'hands reversed clasped circular toe touch (male)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '77b50851ea9d8d09bdd006b967ac1312';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 883 AND name_en = 'hands reversed clasped circular toe touch (male)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '889dfe724b86c240ff6d369c6fd1d453') THEN
    RAISE EXCEPTION 'V52: ejercicio 883 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 884 hug keens to chest: instructions_es: R2 L14 (ALTA): El texto parte de piernas estiradas; en el GIF se parte de rodillas flexionadas con los pies apoyados / V3 W05 ACEPTAR: f1 tumbado boca arriba con rodillas flexionadas, pies apoyados y manos sobre las rodillas (no piernas estiradas); f7 rodillas abrazadas contra el pecho con la c
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en el suelo con las rodillas flexionadas, los pies apoyados y las manos sobre las rodillas. Lleva las dos rodillas hacia el pecho y abrázalas con los brazos, levantando ligeramente la cabeza. Mantén un momento la posición. Suelta las piernas y vuelve con control a la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en el suelo con las rodillas flexionadas, los pies apoyados y las manos sobre las rodillas.", "Lleva las dos rodillas hacia el pecho y abrázalas con los brazos, levantando ligeramente la cabeza.", "Mantén un momento la posición.", "Suelta las piernas y vuelve con control a la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 884 AND name_en = 'hug keens to chest' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '7df739bc16b22b11b1f5008f60ce00c6';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 884 AND name_en = 'hug keens to chest' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '50b72b8fd116ab39480c0f35789e1443') THEN
    RAISE EXCEPTION 'V52: ejercicio 884 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 885 handstand: instructions_es: R2 L15 (MEDIA): El texto pide lanzar las piernas hacia la pared pero empieza buscando un espacio abierto; el GIF muestra una parada de manos libre sin pared, entrando con un pa / V3 W05 MEJORAR: El texto actual lanza las piernas hacia la pared; f13 pino libre sin pared. La propuesta acierta en la entrada (f1 de pie con brazos arriba y una pierna adelant | default_prescription: R2 L15 (MEDIA): La parada de manos se mantiene: prescripción DURATION. Peso sobre los brazos extendidos con los pies en el aire (anotado como BRAZOS_EXTENDIDOS). FL: solo apoya / V3 W05 ACEPTAR: f13 posición de pino sostenida tras la entrada (f10-f13) antes del fundido de salida; es una posición que se mantiene, como indica el propio texto (mantén el pi
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Busca un espacio despejado y ponte de pie con los brazos extendidos por encima de la cabeza. Da un paso largo adelante e inclínate hasta apoyar las manos en el suelo, separadas a la anchura de los hombros. Impúlsate con la pierna apoyada y lleva la otra pierna hacia arriba hasta quedar en vertical sobre las manos, sin pared, con los brazos extendidos, las piernas juntas y el abdomen activado para mantener el equilibrio. Para bajar, lleva las piernas de vuelta al suelo de una en una y con control, y vuelve a ponerte de pie. Mantén la duración indicada.', instruction_steps_es = '["Busca un espacio despejado y ponte de pie con los brazos extendidos por encima de la cabeza.", "Da un paso largo adelante e inclínate hasta apoyar las manos en el suelo, separadas a la anchura de los hombros.", "Impúlsate con la pierna apoyada y lleva la otra pierna hacia arriba hasta quedar en vertical sobre las manos, sin pared, con los brazos extendidos, las piernas juntas y el abdomen activado para mantener el equilibrio.", "Para bajar, lleva las piernas de vuelta al suelo de una en una y con control, y vuelve a ponerte de pie.", "Mantén la duración indicada."]'::jsonb, default_prescription = 'DURATION'
   WHERE exercise_id = 885 AND name_en = 'handstand' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '1449b45201003b5eb28f95a6a59581fd' AND default_prescription = 'SETS_REPS';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 885 AND name_en = 'handstand' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '429dfccb6b9667d218c5bf4fd63e820b' AND default_prescription = 'DURATION') THEN
    RAISE EXCEPTION 'V52: ejercicio 885 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 887 hyght dumbbell fly: name_es: R2 L17 (MEDIA): El nombre actual («ancho») no es comprensible; la fuente parece indicar «high». Las manos llegan a la altura de la cara, no por encima de la cabeza. / V3 W05 ACEPTAR: f1 sentado en un banco con inclinación alta, brazos abiertos con mancuernas; f7 mancuernas juntas delante de la cara. «Aperturas con mancuerna ancho» no es comp | instructions_es: R2 L17 (MEDIA): El texto pide tumbarse boca arriba en un banco; el GIF muestra un banco con inclinación alta, casi sentado, y las mancuernas se juntan delante de la cara / V3 W05 ACEPTAR: f1 respaldo muy inclinado, casi sentado, pies en el suelo y brazos abiertos con codos algo flexionados; f7 mancuernas juntas delante de la cara. El texto actual
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Aperturas con mancuernas en banco inclinado alto', instructions_es = 'Ajusta un banco con una inclinación alta y siéntate con la espalda apoyada en el respaldo y los pies en el suelo. Sujeta una mancuerna en cada mano con los brazos abiertos a los lados y los codos ligeramente flexionados. Junta las mancuernas delante de la cara describiendo un amplio arco, sin cambiar la flexión de los codos. Vuelve con control a la posición inicial hasta notar el estiramiento en el pecho. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ajusta un banco con una inclinación alta y siéntate con la espalda apoyada en el respaldo y los pies en el suelo.", "Sujeta una mancuerna en cada mano con los brazos abiertos a los lados y los codos ligeramente flexionados.", "Junta las mancuernas delante de la cara describiendo un amplio arco, sin cambiar la flexión de los codos.", "Vuelve con control a la posición inicial hasta notar el estiramiento en el pecho.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 887 AND name_en = 'hyght dumbbell fly' AND name_es = 'Aperturas con mancuerna ancho' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'abd0ce2c4a44c2c42a9d0cda5d888727';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 887 AND name_en = 'hyght dumbbell fly' AND name_es = 'Aperturas con mancuernas en banco inclinado alto' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '43e95730bcba5fe06b041ced5da96e5a') THEN
    RAISE EXCEPTION 'V52: ejercicio 887 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 888 floor fly (with barbell): requires_kneeling: R2 L18 (ALTA): Las rodillas no apoyan en ningún fotograma: la plancha es sobre las puntas de los pies, KN sobra. Se usan dos barras, una por mano; el requisito no indica la ca / V3 W05 ACEPTAR: f1 plancha alta sobre las puntas de los pies con piernas rectas; f7 plancha baja con piernas rectas y rodillas despegadas del suelo; f2-f6 y f8-f12 son fundidos / coordinador: definicion literal demostrada en todos los fotogramas (R2 + V3)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_kneeling = FALSE
   WHERE exercise_id = 888 AND name_en = 'floor fly (with barbell)' AND requires_kneeling = TRUE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 888 AND name_en = 'floor fly (with barbell)' AND requires_kneeling = FALSE) THEN
    RAISE EXCEPTION 'V52: ejercicio 888 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 889 frankenstein squat: instructions_es: R2 L19 (ALTA): El texto dice sostener la barra frente al cuerpo con los brazos rectos, lo que sugiere sujetarla con las manos; en el GIF la barra descansa sobre la parte delan / V3 Y01 ACEPTAR: f1 y f7 barra apoyada sobre la parte delantera de los hombros y brazos estirados al frente con las manos abiertas, sin agarrarla; f7 sentadilla hasta muslos par | zona: R2 L19 (ALTA): El GIF resalta los cuádriceps. / V3 Y01 ACEPTAR: f1-f12 resaltado rojo en los cuádriceps en todo el recorrido (formato habitual upper legs/quads)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies separados a la anchura de los hombros y apoya la barra sobre la parte delantera de los hombros. Estira los brazos al frente, por debajo de la barra, sin agarrarla, y mantén el pecho elevado. Baja flexionando las rodillas y la cadera hasta que los muslos queden paralelos al suelo, o tan abajo como puedas con comodidad. Empuja con los talones para volver a la posición inicial sin dejar que la barra ruede. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies separados a la anchura de los hombros y apoya la barra sobre la parte delantera de los hombros.", "Estira los brazos al frente, por debajo de la barra, sin agarrarla, y mantén el pecho elevado.", "Baja flexionando las rodillas y la cadera hasta que los muslos queden paralelos al suelo, o tan abajo como puedas con comodidad.", "Empuja con los talones para volver a la posición inicial sin dejar que la barra ruede.", "Repite el número de repeticiones indicado."]'::jsonb, body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs'), target_muscle = 'quads'
   WHERE exercise_id = 889 AND name_en = 'frankenstein squat' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '8e6a2fe02e0a1738c76f2d389ae0aebe' AND body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs') AND target_muscle = 'glutes';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 889 AND name_en = 'frankenstein squat' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '577ce1678977f39bd9183c3243d91cfd' AND body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs') AND target_muscle = 'quads') THEN
    RAISE EXCEPTION 'V52: ejercicio 889 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 893 jack burpee: instructions_es: R2 L20 (MEDIA): El texto incluye una flexión de brazos en la plancha que el GIF no muestra; en el GIF las piernas se abren y se cierran en la plancha antes de recogerlas, cosa  / V3 W05 MEJORAR: No hay flexión de brazos (el texto actual la incluye). f1 cuclillas con manos en el suelo; f7 plancha con brazos extendidos y piernas ya abiertas; f13 plancha c | difficulty_level: R2 L20 (MEDIA): coordinación; salto; bajar y subir del suelo / V3 W05 ACEPTAR: f1-f30 ciclo de varias fases: cuclillas, paso a plancha con piernas abiertas (f7), cierre (f13), vuelta a cuclillas (f19) y salto con brazos arriba (f25). Exige
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies separados a la anchura de los hombros, baja a cuclillas y apoya las manos en el suelo delante de ti. Lleva los pies hacia atrás con las piernas abiertas hasta quedar en plancha con los brazos extendidos. Junta las piernas manteniendo el cuerpo recto en la plancha. Recoge los pies hacia las manos para volver a cuclillas. Salta hacia arriba de forma explosiva extendiendo los brazos por encima de la cabeza y aterriza suavemente con las rodillas flexionadas. Continúa el ciclo sin pausa. Mantén la duración indicada.', instruction_steps_es = '["Ponte de pie con los pies separados a la anchura de los hombros, baja a cuclillas y apoya las manos en el suelo delante de ti.", "Lleva los pies hacia atrás con las piernas abiertas hasta quedar en plancha con los brazos extendidos.", "Junta las piernas manteniendo el cuerpo recto en la plancha.", "Recoge los pies hacia las manos para volver a cuclillas.", "Salta hacia arriba de forma explosiva extendiendo los brazos por encima de la cabeza y aterriza suavemente con las rodillas flexionadas.", "Continúa el ciclo sin pausa.", "Mantén la duración indicada."]'::jsonb, difficulty_level = 2
   WHERE exercise_id = 893 AND name_en = 'jack burpee' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '821b73fbf2f5fb42b3ba7a9a456e916e' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 893 AND name_en = 'jack burpee' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '9791d997205d2164923087ff9f75daee' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 893 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 894 jack jump (male): requires_overhead: R2 L01 (ALTA): Los brazos van al frente hacia los pies, a la altura de los hombros, y nunca pasan por encima de la cabeza: OH sobra. Salto alto con apertura de piernas: se pro / V3 V05 ACEPTAR: f1 de pie con brazos a los lados; f7 cuclillas con manos cerca del suelo; f13 vuelo con piernas abiertas y brazos al frente a la altura de los hombros; en ningú / coordinador: definicion literal demostrada en todos los fotogramas (R2 + V3) | difficulty_level: R2 L01 (ALTA): pliometría exigente; coordinación / V3 V05 ACEPTAR: f7-f13 salto explosivo desde cuclillas hasta piernas abiertas casi horizontales en el aire: pliometría exigente con coordinación
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = FALSE, difficulty_level = 3
   WHERE exercise_id = 894 AND name_en = 'jack jump (male)' AND requires_overhead = TRUE AND difficulty_level = 2;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 894 AND name_en = 'jack jump (male)' AND requires_overhead = FALSE AND difficulty_level = 3) THEN
    RAISE EXCEPTION 'V52: ejercicio 894 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 895 incline push up depth jump: difficulty_level: R2 L02 (MEDIA): pliometría exigente de tren superior; fuerza relativa alta / V3 V05 ACEPTAR: f1-f24 pies sobre un banco y manos alternando entre el suelo entre los steps y los steps, con caída y empuje explosivo: pliometría de tren superior con pies ele
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 895 AND name_en = 'incline push up depth jump' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 895 AND name_en = 'incline push up depth jump' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 895 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 896 incline push-up: requires_floor: R2 L03 (MEDIA): El step es muy bajo (unos centímetros): la posición es prácticamente la de una flexión en el suelo, por eso se propone FL igual que en las flexiones en el suelo / V3 V05 ACEPTAR: f1-f12 manos sobre un step de pocos centímetros y pies en el suelo; f1 pecho a ras del step: la posición obliga a bajar prácticamente al suelo como en una flexi
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_floor = TRUE
   WHERE exercise_id = 896 AND name_en = 'incline push-up' AND requires_floor = FALSE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 896 AND name_en = 'incline push-up' AND requires_floor = TRUE) THEN
    RAISE EXCEPTION 'V52: ejercicio 896 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 900 isometric chest squeeze: instructions_es: norma de texto del cierre: la frase final pedia repeticiones («Repite el número de repeticiones deseado.») y la prescripcion vigente es DURATION, que la R2 (animacion completa) no propuso cambiar; solo se sustituye la frase final
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies separados a la altura de los hombros y las rodillas ligeramente flexionadas. Extiende los brazos rectos frente a ti, paralelos al suelo, con las palmas una frente a la otra. Aprieta los músculos del pecho con la mayor fuerza posible, manteniendo los brazos rectos. Mantén esta posición durante unos segundos, concentrándote en contraer los músculos del pecho. Suelta la contracción y relaja los músculos del pecho. Mantén la duración indicada.', instruction_steps_es = '["Ponte de pie con los pies separados a la altura de los hombros y las rodillas ligeramente flexionadas.", "Extiende los brazos rectos frente a ti, paralelos al suelo, con las palmas una frente a la otra.", "Aprieta los músculos del pecho con la mayor fuerza posible, manteniendo los brazos rectos.", "Mantén esta posición durante unos segundos, concentrándote en contraer los músculos del pecho.", "Suelta la contracción y relaja los músculos del pecho.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 900 AND name_en = 'isometric chest squeeze' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'c78dce9edcfadc7108ba6bbecebe8616';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 900 AND name_en = 'isometric chest squeeze' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '40d0e13721f411cd4a6d51e1ad053d2a') THEN
    RAISE EXCEPTION 'V52: ejercicio 900 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 901 isometric wipers: movement_pattern: R2 L08 (MEDIA): El movimiento principal es un empuje horizontal con desplazamiento lateral, no una rotación ni flexión lateral del tronco. Se propone dificultad 2 por la carga  / V3 V05 ACEPTAR: f1 cuerpo desplazado sobre un brazo flexionado; f7 desplazado hacia el otro; tronco rígido sin rotación ni flexión lateral: el trabajo es un empuje de brazos en | difficulty_level: R2 L08 (MEDIA): fuerza relativa moderada de empuje / V3 V05 ACEPTAR: f1-f12 vaivén lateral en plancha baja cargando el peso sobre un brazo flexionado: fuerza relativa moderada de empuje, por encima del nivel 1
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET movement_pattern = 'HORIZONTAL_PUSH', difficulty_level = 2
   WHERE exercise_id = 901 AND name_en = 'isometric wipers' AND movement_pattern = 'TRUNK_ROTATION_LATERAL_FLEXION' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 901 AND name_en = 'isometric wipers' AND movement_pattern = 'HORIZONTAL_PUSH' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 901 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 902 inchworm: requires_inversion: R2 L09 (BAJA): En la pica (f7-f13) la cabeza queda claramente por debajo de la cadera con el peso en brazos y pies, y los brazos quedan alineados con el tronco por encima de l / V3 V06 ACEPTAR: f7 y f13 pica con manos y pies en el suelo: la cabeza queda claramente por debajo de la cadera (en f13 unos 23 px de 150 de altura corporal) con el peso reparti
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_inversion = TRUE
   WHERE exercise_id = 902 AND name_en = 'inchworm' AND requires_inversion = FALSE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 902 AND name_en = 'inchworm' AND requires_inversion = TRUE) THEN
    RAISE EXCEPTION 'V52: ejercicio 902 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 903 inchworm v. 2: movement_pattern: R2 L10 (MEDIA): No es cardio rítmico continuo: es un ejercicio dinámico de movilidad y control (alternativa CORE_STABILITY). La cadera queda por encima de la cabeza en la V inv / V3 V06 ACEPTAR: f1 plancha alta; f7 y f13 pica con cadera alta; f9-f18 fundidos con manos y pies avanzando: desplazamiento lento a cuatro apoyos con elongación posterior, no ca
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET movement_pattern = 'MOBILITY'
   WHERE exercise_id = 903 AND name_en = 'inchworm v. 2' AND movement_pattern = 'LOCOMOTION_CARDIO';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 903 AND name_en = 'inchworm v. 2' AND movement_pattern = 'MOBILITY') THEN
    RAISE EXCEPTION 'V52: ejercicio 903 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 904 incline leg hip raise (leg straight): requires_overhead: R2 L11 (MEDIA): OH por los brazos por encima de la cabeza agarrando el banco (codos flexionados; discutible). Elevar las piernas rectas hasta despegar la cadera exige más fuerz / V3 W05 ACEPTAR: f1-f12 tumbado en banco inclinado con las manos agarrando el banco por detrás de la cabeza y los codos apuntando hacia arriba junto a la cabeza (hombros en flex | instructions_es: R2 L11 (MEDIA): Dice colocar las manos a los lados del banco; en el GIF las manos agarran el banco por detrás de la cabeza. Tampoco menciona que la cadera se despega del banco  / V3 W05 ACEPTAR: f1 tumbado boca arriba con la cabeza en el extremo alto del banco y las manos agarrando el banco detrás de la cabeza (no a los lados); f7 piernas rectas casi ve
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = TRUE, instructions_es = 'Túmbate boca arriba en un banco inclinado con la cabeza en el extremo alto y agarra el banco por detrás de la cabeza con ambas manos. Mantén las piernas estiradas y juntas. Activa el abdomen y eleva las piernas rectas hasta la vertical, despegando ligeramente la cadera del banco. Baja las piernas con control sin dejar que la zona lumbar se arquee. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en un banco inclinado con la cabeza en el extremo alto y agarra el banco por detrás de la cabeza con ambas manos.", "Mantén las piernas estiradas y juntas.", "Activa el abdomen y eleva las piernas rectas hasta la vertical, despegando ligeramente la cadera del banco.", "Baja las piernas con control sin dejar que la zona lumbar se arquee.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 904 AND name_en = 'incline leg hip raise (leg straight)' AND requires_overhead = FALSE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '4d099bb76e1339816c4a2fb046c5460f';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 904 AND name_en = 'incline leg hip raise (leg straight)' AND requires_overhead = TRUE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '2abce0e29b9e70a0c32a0586cdf5b98b') THEN
    RAISE EXCEPTION 'V52: ejercicio 904 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 905 incline twisting sit-up: requires_inversion: R2 L12 (MEDIA): Es una incorporación completa (sit-up), no un crunch. INV con el mismo criterio que los demás ejercicios en banco declinado del lote, aunque el declive es moder / V3 W05 ACEPTAR: f1 y f12 tumbado boca arriba en banco declinado (unos 14 grados medidos sobre el banco) con la cabeza visiblemente por debajo de la cadera y el peso en la espal | name_es: R2 L12 (MEDIA): Es una incorporación completa (sit-up), no un crunch. INV con el mismo criterio que los demás ejercicios en banco declinado del lote, aunque el declive es moder / V3 W05 ACEPTAR: f1 tumbado en banco con la cabeza más baja que la cadera (declinado, no inclinado); f7 tronco casi vertical con giro: incorporación completa, no crunch. El prop | instructions_es: R2 L12 (MEDIA): El texto pide un banco inclinado a 45 grados; el GIF muestra un banco con un declive moderado (cabeza más baja que la cadera) y una incorporación completa con g / V3 W05 ACEPTAR: f1 banco declinado con piernas enganchadas en los rodillos y manos detrás de la cabeza; f7 casi sentado con el tronco girado hacia un lado; f8-f12 vuelta a tumb
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_inversion = TRUE, name_es = 'Abdominal con giro en banco declinado', instructions_es = 'Túmbate boca arriba en un banco declinado con las piernas enganchadas en los rodillos y la cabeza más baja que la cadera. Coloca las manos detrás de la cabeza. Activa el abdomen y eleva el tronco hasta quedar casi sentado, girándolo para llevar un codo hacia la rodilla contraria. Baja despacio hasta apoyar de nuevo la espalda en el banco. Repite el número de repeticiones indicado y después cambia el lado del giro.', instruction_steps_es = '["Túmbate boca arriba en un banco declinado con las piernas enganchadas en los rodillos y la cabeza más baja que la cadera.", "Coloca las manos detrás de la cabeza.", "Activa el abdomen y eleva el tronco hasta quedar casi sentado, girándolo para llevar un codo hacia la rodilla contraria.", "Baja despacio hasta apoyar de nuevo la espalda en el banco.", "Repite el número de repeticiones indicado y después cambia el lado del giro."]'::jsonb
   WHERE exercise_id = 905 AND name_en = 'incline twisting sit-up' AND requires_inversion = FALSE AND name_es = 'Crunch con giro en inclinada' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '8de994136a2d0dd3185c5f7cf93ffb8d';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 905 AND name_en = 'incline twisting sit-up' AND requires_inversion = TRUE AND name_es = 'Abdominal con giro en banco declinado' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e52aa9d368441f2e2b489465e7f4b81e') THEN
    RAISE EXCEPTION 'V52: ejercicio 905 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 906 jackknife sit-up: difficulty_level: R2 L13 (MEDIA): coordinación; fuerza de core / V3 W05 ACEPTAR: f1 tumbado con brazos por encima de la cabeza; f7 posición en V equilibrada sobre los glúteos con piernas rectas y manos hacia los pies. Exige fuerza de core, c
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 906 AND name_en = 'jackknife sit-up' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 906 AND name_en = 'jackknife sit-up' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 906 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 907 janda sit-up: instructions_es: R2 L14 (ALTA): El texto pide manos detrás de la cabeza y subir a 45 grados; en el GIF los brazos están cruzados sobre el pecho y el tronco llega casi a la vertical / V3 W05 ACEPTAR: f1 tumbado con rodillas flexionadas, pies apoyados y brazos cruzados sobre el pecho (no manos detrás de la cabeza); f7 tronco casi vertical (no 45 grados): camb
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en el suelo con las rodillas flexionadas y los pies apoyados. Cruza los brazos sobre el pecho con las manos hacia los hombros. Activa el abdomen y eleva el tronco hasta quedar casi vertical. Baja con control hasta volver a tumbarte. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en el suelo con las rodillas flexionadas y los pies apoyados.", "Cruza los brazos sobre el pecho con las manos hacia los hombros.", "Activa el abdomen y eleva el tronco hasta quedar casi vertical.", "Baja con control hasta volver a tumbarte.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 907 AND name_en = 'janda sit-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '800e583ab286e5d9c2adeb823a56752a';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 907 AND name_en = 'janda sit-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e819d314de899f5850eaf8e7e86b7339') THEN
    RAISE EXCEPTION 'V52: ejercicio 907 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 909 hyperextension (on bench): instructions_es: R2 L16 (MEDIA): El texto habla de un banco de hiperextensiones con los pies asegurados; el GIF muestra un banco plano con las piernas tumbadas sobre él y sin sujeción visible / V3 W05 ACEPTAR: f1 boca abajo sobre un banco plano con la cadera en el extremo, piernas tumbadas sobre el banco, tronco colgando y manos detrás de la cabeza; f7 tronco elevado 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca abajo sobre un banco plano con la cadera en el extremo y el tronco por fuera del banco. Apoya las piernas a lo largo del banco; si lo necesitas, pide a alguien que te sujete los tobillos. Coloca las manos detrás de la cabeza y deja bajar el tronco hacia el suelo con la espalda recta. Eleva el tronco hasta que quede alineado con las piernas y vuelve a bajar con control. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca abajo sobre un banco plano con la cadera en el extremo y el tronco por fuera del banco.", "Apoya las piernas a lo largo del banco; si lo necesitas, pide a alguien que te sujete los tobillos.", "Coloca las manos detrás de la cabeza y deja bajar el tronco hacia el suelo con la espalda recta.", "Eleva el tronco hasta que quede alineado con las piernas y vuelve a bajar con control.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 909 AND name_en = 'hyperextension (on bench)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a7fbf39f3f01d27c5bf4fa13587eaa37';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 909 AND name_en = 'hyperextension (on bench)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e5f12f8cc2f42d237a0f550b0c4753bf') THEN
    RAISE EXCEPTION 'V52: ejercicio 909 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 910 inverted row: difficulty_level: R2 L17 (ALTA): fuerza relativa moderada / V3 W05 ACEPTAR: f1-f12 remo invertido con barra baja en un rack: cuerpo casi horizontal apoyado solo en los talones, tirando del pecho hasta la barra en f7. Con el cuerpo casi 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 910 AND name_en = 'inverted row' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 910 AND name_en = 'inverted row' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 910 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 912 inverted row on bench: difficulty_level: R2 L19 (ALTA): fuerza relativa moderada con pies elevados / V3 Y01 ACEPTAR: f1-f12 cuerpo recto bajo la barra Smith con los talones sobre un banco, tirando del pecho hasta la barra (f7): fuerza relativa moderada con gran parte del peso 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 912 AND name_en = 'inverted row on bench' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 912 AND name_en = 'inverted row on bench' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 912 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 913 inverted row v. 2: instructions_es: R2 L20 (MEDIA): El texto describe un remo inclinado de pie con los pies en el suelo en una máquina Smith o un entrenador de suspensión; el GIF muestra a la persona colgada boca / V3 W05 ACEPTAR: f1-f12 colgado boca abajo de una barra de dominadas, sin apoyo de pies, con las piernas hacia arriba junto a la barra; f1 brazos extendidos; f7 codos flexionado
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Sujeta una barra de dominadas y colócate colgado boca abajo, con la cabeza hacia el suelo y las piernas extendidas hacia arriba junto a la barra. Con los brazos extendidos a lo largo del tronco, activa el abdomen y mantén el cuerpo firme. Tira del cuerpo hacia la barra flexionando los codos y juntando los omóplatos. Baja con control hasta extender de nuevo los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Sujeta una barra de dominadas y colócate colgado boca abajo, con la cabeza hacia el suelo y las piernas extendidas hacia arriba junto a la barra.", "Con los brazos extendidos a lo largo del tronco, activa el abdomen y mantén el cuerpo firme.", "Tira del cuerpo hacia la barra flexionando los codos y juntando los omóplatos.", "Baja con control hasta extender de nuevo los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 913 AND name_en = 'inverted row v. 2' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '1f55181a48a921547a32c82d5b7ff316';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 913 AND name_en = 'inverted row v. 2' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a1b52e4e22bd58b73b962b17e297a938') THEN
    RAISE EXCEPTION 'V52: ejercicio 913 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 914 inverted row with straps: difficulty_level: R2 L01 (MEDIA): fuerza relativa moderada / V3 V06 ACEPTAR: f1 cuerpo casi horizontal bajo las anillas con solo los talones en el suelo; f7 pecho a las anillas: casi todo el peso corporal suspendido de las correas, exige
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 914 AND name_en = 'inverted row with straps' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 914 AND name_en = 'inverted row with straps' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 914 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 917 iron cross stretch: instructions_es: R2 L04 (MEDIA): El texto pide elevar las dos piernas rectas hacia el techo y bajarlas juntas hacia un lado; el GIF muestra una sola pierna que se flexiona y se cruza sobre la o / V3 V06 ACEPTAR: f1 boca arriba con brazos en cruz y piernas estiradas; f5-f8 una sola pierna con la rodilla flexionada cruza sobre la otra hacia el lado contrario con giro de c
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en el suelo con las piernas estiradas y los brazos extendidos en cruz. Eleva una pierna flexionando la rodilla y crúzala sobre la otra hacia el lado contrario, girando la cadera. Mantén los hombros y los brazos apoyados en el suelo mientras notas el estiramiento. Vuelve con control a la posición inicial y cambia de lado al terminar. Mantén la duración indicada.', instruction_steps_es = '["Túmbate boca arriba en el suelo con las piernas estiradas y los brazos extendidos en cruz.", "Eleva una pierna flexionando la rodilla y crúzala sobre la otra hacia el lado contrario, girando la cadera.", "Mantén los hombros y los brazos apoyados en el suelo mientras notas el estiramiento.", "Vuelve con control a la posición inicial y cambia de lado al terminar.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 917 AND name_en = 'iron cross stretch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'aff3cee0684e25e0be13b08abc76ed22';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 917 AND name_en = 'iron cross stretch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'c17bff03e5d24c6a1b4aede83833de7a') THEN
    RAISE EXCEPTION 'V52: ejercicio 917 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 918 jump squat: instructions_es: R2 L05 (ALTA): El texto dice llevar los brazos hacia delante en el aire; el GIF muestra los brazos bajando a los costados durante el salto y las manos juntas delante del pecho / V3 V06 ACEPTAR: f1 y f12 sentadilla con las manos juntas delante del pecho; f6-f7 extensión completa con los brazos a los costados, sin llevarlos al frente como dice el texto a
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies separados a la anchura de los hombros. Baja en sentadilla llevando las caderas hacia atrás, con las manos juntas delante del pecho. Salta de forma explosiva extendiendo caderas, rodillas y tobillos, y deja los brazos a los costados. Aterriza suavemente con las rodillas flexionadas y alineadas con los pies, y enlaza con la siguiente sentadilla. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies separados a la anchura de los hombros.", "Baja en sentadilla llevando las caderas hacia atrás, con las manos juntas delante del pecho.", "Salta de forma explosiva extendiendo caderas, rodillas y tobillos, y deja los brazos a los costados.", "Aterriza suavemente con las rodillas flexionadas y alineadas con los pies, y enlaza con la siguiente sentadilla.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 918 AND name_en = 'jump squat' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '475d2fac27e05feaf4addb13405f27a9';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 918 AND name_en = 'jump squat' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '9e08df96687c74991e93b73287f3dbb7') THEN
    RAISE EXCEPTION 'V52: ejercicio 918 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 920 impossible dips: instructions_es: R2 L07 (ALTA): El texto pide flexionar las rodillas y cruzar los tobillos y bajar hasta brazos paralelos al suelo; el GIF muestra las piernas rectas y la bajada hasta apoyar l / V3 V06 ACEPTAR: f1 piernas rectas y adelantadas, sin rodillas flexionadas ni tobillos cruzados; f7 codos flexionados con los antebrazos apoyados sobre las barras, más abajo que
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Sujétate a unas barras paralelas con los brazos extendidos y los pies sin apoyo, con las piernas rectas y algo adelantadas. Flexiona los codos y baja el cuerpo de forma controlada hasta apoyar los antebrazos sobre las barras. Empuja con las manos para despegar los antebrazos y extender de nuevo los brazos. Mantén el tronco firme y los hombros alejados de las orejas durante todo el movimiento. Repite el número de repeticiones indicado.', instruction_steps_es = '["Sujétate a unas barras paralelas con los brazos extendidos y los pies sin apoyo, con las piernas rectas y algo adelantadas.", "Flexiona los codos y baja el cuerpo de forma controlada hasta apoyar los antebrazos sobre las barras.", "Empuja con las manos para despegar los antebrazos y extender de nuevo los brazos.", "Mantén el tronco firme y los hombros alejados de las orejas durante todo el movimiento.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 920 AND name_en = 'impossible dips' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '4005013578f419ab830745b1583ffd70';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 920 AND name_en = 'impossible dips' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b7875ec6ba16c5706ab07939cf7a5aa1') THEN
    RAISE EXCEPTION 'V52: ejercicio 920 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 921 incline close-grip push-up: instructions_es: R2 L08 (ALTA): El texto pide manos algo más separadas que los hombros sobre un banco o escalón; el GIF muestra un agarre cerrado (manos juntas) sobre la barra de una máquina S / V3 V06 ACEPTAR: f1-f12 manos juntas, más cerca que la anchura de los hombros, sobre la barra de una máquina Smith a la altura de la cadera; f7 pecho hacia la barra con cuerpo r
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca la barra de la máquina Smith a la altura de la cadera y asegúrate de que está bien fijada. Apoya las manos juntas sobre la barra, más cerca que la anchura de los hombros, y lleva los pies atrás hasta que el cuerpo forme una línea recta. Baja el pecho hacia la barra flexionando los codos y manteniéndolos cerca de los costados. Empuja hasta extender los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca la barra de la máquina Smith a la altura de la cadera y asegúrate de que está bien fijada.", "Apoya las manos juntas sobre la barra, más cerca que la anchura de los hombros, y lleva los pies atrás hasta que el cuerpo forme una línea recta.", "Baja el pecho hacia la barra flexionando los codos y manteniéndolos cerca de los costados.", "Empuja hasta extender los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 921 AND name_en = 'incline close-grip push-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '029e4043d52f2f5ea1e0f5e395083764';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 921 AND name_en = 'incline close-grip push-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '704df0e84239915f8bd256ce5fa6b774') THEN
    RAISE EXCEPTION 'V52: ejercicio 921 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 922 kettlebell alternating hang clean: instructions_es: R2 L09 (BAJA): Describe inclinarse desde la cadera, extensión explosiva de cadera y encogimiento de hombros, pero el GIF muestra el cuerpo erguido con las piernas rectas y sol / V3 V06 MEJORAR: f1 de pie erguido con piernas rectas y dos pesas rusas colgando, sin la bisagra de cadera que pide el texto actual; f4-f7 fundido hasta el rack de un brazo. El 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie erguido con una pesa rusa en cada mano, colgando delante de los muslos con los brazos extendidos y la espalda recta. Lleva una pesa rusa hasta el hombro y recíbela en posición de rack, apoyada en la parte externa del antebrazo y con el codo cerca del cuerpo. Haz una pausa breve y baja la pesa rusa con control hasta la posición inicial. Repite con el otro brazo. Alterna los lados. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie erguido con una pesa rusa en cada mano, colgando delante de los muslos con los brazos extendidos y la espalda recta.", "Lleva una pesa rusa hasta el hombro y recíbela en posición de rack, apoyada en la parte externa del antebrazo y con el codo cerca del cuerpo.", "Haz una pausa breve y baja la pesa rusa con control hasta la posición inicial.", "Repite con el otro brazo. Alterna los lados.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 922 AND name_en = 'kettlebell alternating hang clean' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd0ad1e8088cddf4ed41d72c0cf298275';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 922 AND name_en = 'kettlebell alternating hang clean' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ec2f8e18988aaf205e189a2e6d6eaac0') THEN
    RAISE EXCEPTION 'V52: ejercicio 922 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 923 kettlebell alternating press: axial_load: R2 L10 (ALTA): Press por encima de la cabeza de pie con dos pesas rusas: la definición de AX incluye cargas en press por encima de la cabeza (coherente con 945, que la lleva). / V3 V06 ACEPTAR: f1 dos pesas rusas en los hombros; f5-f8 un brazo extiende la pesa por encima de la cabeza de pie: press por encima de la cabeza con carga, incluido en la defin
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET axial_load = TRUE
   WHERE exercise_id = 923 AND name_en = 'kettlebell alternating press' AND axial_load = FALSE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 923 AND name_en = 'kettlebell alternating press' AND axial_load = TRUE) THEN
    RAISE EXCEPTION 'V52: ejercicio 923 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 924 kettlebell arnold press: instructions_es: R2 L11 (ALTA): Dice sostener una pesa rusa en cada mano y empujar ambas; el GIF muestra una sola pesa rusa en una mano mientras el otro brazo cuelga / V3 W05 ACEPTAR: f1 una sola pesa rusa en una mano delante de la cara con el codo flexionado y el otro brazo colgando; f7 brazo extendido por encima de la cabeza. El texto actua
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies separados a la anchura de los hombros y sujeta una pesa rusa con una mano delante del hombro, con el codo flexionado y la palma hacia ti. Activa el core y empuja la pesa rusa por encima de la cabeza, girando el brazo hasta que la palma mire al frente con el brazo extendido. Baja con control deshaciendo el giro hasta la posición inicial. Mantén el tronco erguido sin arquear la zona lumbar. Repite el número de repeticiones indicado. Alterna los lados.', instruction_steps_es = '["Ponte de pie con los pies separados a la anchura de los hombros y sujeta una pesa rusa con una mano delante del hombro, con el codo flexionado y la palma hacia ti.", "Activa el core y empuja la pesa rusa por encima de la cabeza, girando el brazo hasta que la palma mire al frente con el brazo extendido.", "Baja con control deshaciendo el giro hasta la posición inicial.", "Mantén el tronco erguido sin arquear la zona lumbar.", "Repite el número de repeticiones indicado. Alterna los lados."]'::jsonb
   WHERE exercise_id = 924 AND name_en = 'kettlebell arnold press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5ab59aa846f60f7b2e8d88a4cf987329';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 924 AND name_en = 'kettlebell arnold press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '46a84de4a2a3f99e0ecdbd0b13833170') THEN
    RAISE EXCEPTION 'V52: ejercicio 924 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 925 kettlebell alternating press on floor: instructions_es: R2 L12 (ALTA): El texto pide rodillas dobladas con los pies apoyados y bajar una pesa mientras se sube la otra a la vez; el GIF muestra las piernas extendidas en el suelo y un / V3 W05 ACEPTAR: f1 tumbado con piernas extendidas (no rodillas dobladas) y dos pesas rusas arriba; f7 una pesa bajada junto al hombro mientras la otra sigue arriba; no se bajan
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en el suelo con las piernas extendidas. Sujeta una pesa rusa en cada mano con los brazos extendidos hacia el techo. Baja una pesa rusa flexionando el codo hasta acercarla al hombro, manteniendo la otra arriba. Empuja de nuevo hasta extender el brazo. Repite el número de repeticiones indicado con cada brazo.', instruction_steps_es = '["Túmbate boca arriba en el suelo con las piernas extendidas.", "Sujeta una pesa rusa en cada mano con los brazos extendidos hacia el techo.", "Baja una pesa rusa flexionando el codo hasta acercarla al hombro, manteniendo la otra arriba.", "Empuja de nuevo hasta extender el brazo.", "Repite el número de repeticiones indicado con cada brazo."]'::jsonb
   WHERE exercise_id = 925 AND name_en = 'kettlebell alternating press on floor' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '058e14b5b67aa5b008f37aae6a6c9bdf';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 925 AND name_en = 'kettlebell alternating press on floor' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '7cf266274d1c4429592566bdfb2091cb') THEN
    RAISE EXCEPTION 'V52: ejercicio 925 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 927 kettlebell bent press: axial_load: R2 L14 (MEDIA): Carga sostenida por encima de la cabeza con la columna inclinada: según la definición de AX (cargas en press por encima de la cabeza) falta AX / V3 W05 ACEPTAR: f4-f8 pesa rusa sostenida con el brazo extendido por encima de la cabeza mientras el tronco se inclina; f7 brazo vertical con la pesa en lo alto: carga en press | instructions_es: R2 L14 (MEDIA): El texto empuja primero la kettlebell por encima de la cabeza, flexiona la rodilla y toca el suelo con la mano; en el GIF la kettlebell parte del hombro, el bra / V3 W05 ACEPTAR: f1 pesa rusa a la altura del hombro, pies más separados que los hombros; f4-f7 el tronco se inclina al lado contrario mientras el brazo de la pesa queda vertica
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET axial_load = TRUE, instructions_es = 'Ponte de pie con los pies más separados que los hombros y sujeta una kettlebell a la altura del hombro con una mano. Inclina el tronco hacia el lado contrario a la kettlebell, deslizando la mano libre por la pierna hacia la rodilla, mientras el brazo de la kettlebell queda estirado por encima de la cabeza. Mantén la mirada en la kettlebell y el brazo vertical. Vuelve a erguir el tronco con control hasta llevar la kettlebell de nuevo al hombro. Repite el número de repeticiones indicado y cambia de lado.', instruction_steps_es = '["Ponte de pie con los pies más separados que los hombros y sujeta una kettlebell a la altura del hombro con una mano.", "Inclina el tronco hacia el lado contrario a la kettlebell, deslizando la mano libre por la pierna hacia la rodilla, mientras el brazo de la kettlebell queda estirado por encima de la cabeza.", "Mantén la mirada en la kettlebell y el brazo vertical.", "Vuelve a erguir el tronco con control hasta llevar la kettlebell de nuevo al hombro.", "Repite el número de repeticiones indicado y cambia de lado."]'::jsonb
   WHERE exercise_id = 927 AND name_en = 'kettlebell bent press' AND axial_load = FALSE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5eff02f6a0497e00c55858e252f25adf';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 927 AND name_en = 'kettlebell bent press' AND axial_load = TRUE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5061055ba4d0afd86578667d062f1e8c') THEN
    RAISE EXCEPTION 'V52: ejercicio 927 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 928 kettlebell alternating renegade row: instructions_es: R2 L15 (MEDIA): El texto pide plancha alta con los pies separados a la anchura de las caderas y alternar brazos; en el GIF una pierna está flexionada por delante con el pie apo / V3 W05 ACEPTAR: f1 manos sobre dos pesas rusas, una pierna estirada atrás y la otra adelantada con la rodilla flexionada y el pie apoyado (no plancha con pies a la anchura de l
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca dos kettlebells en el suelo y apoya una mano sobre el asa de cada una, con los brazos extendidos. Estira una pierna hacia atrás y apoya el pie de la otra pierna por delante, con la rodilla flexionada. Tira de una kettlebell hacia la cadera, con el codo cerca del cuerpo, manteniendo la otra mano apoyada. Baja la kettlebell al suelo con control. Completa las repeticiones y cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca dos kettlebells en el suelo y apoya una mano sobre el asa de cada una, con los brazos extendidos.", "Estira una pierna hacia atrás y apoya el pie de la otra pierna por delante, con la rodilla flexionada.", "Tira de una kettlebell hacia la cadera, con el codo cerca del cuerpo, manteniendo la otra mano apoyada.", "Baja la kettlebell al suelo con control.", "Completa las repeticiones y cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 928 AND name_en = 'kettlebell alternating renegade row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '337eedb0ec64ac854219d01554111d59';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 928 AND name_en = 'kettlebell alternating renegade row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '64a8280bb7879b20debf161bb37f8298') THEN
    RAISE EXCEPTION 'V52: ejercicio 928 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 930 kettlebell bottoms up clean from the hang position: instructions_es: R2 L17 (MEDIA): El texto pide inclinarse desde la cadera, dejar la pesa entre las piernas y extender la cadera de forma explosiva; el GIF muestra al ejecutante erguido todo el  / V3 W05 ACEPTAR: f1 erguido con la pesa colgando al costado, brazo extendido; f7 codo flexionado, antebrazo vertical y pesa con la base hacia arriba cerca de la cabeza; nunca ha
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie erguido con los pies separados a la anchura de las caderas y una pesa rusa colgando a un lado con el brazo extendido. Flexiona el codo y lleva la pesa hacia arriba girando la muñeca para que la base quede hacia arriba. Detén la pesa con el antebrazo vertical y la mano cerca de la cabeza, apretando el agarre para mantenerla equilibrada. Bájala con control hasta extender el brazo. Completa las repeticiones y cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie erguido con los pies separados a la anchura de las caderas y una pesa rusa colgando a un lado con el brazo extendido.", "Flexiona el codo y lleva la pesa hacia arriba girando la muñeca para que la base quede hacia arriba.", "Detén la pesa con el antebrazo vertical y la mano cerca de la cabeza, apretando el agarre para mantenerla equilibrada.", "Bájala con control hasta extender el brazo.", "Completa las repeticiones y cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 930 AND name_en = 'kettlebell bottoms up clean from the hang position' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '04132ce4e4dd25ed111893782e166c04';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 930 AND name_en = 'kettlebell bottoms up clean from the hang position' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b724abf90356ec3eec4de0a4c32ffc0c') THEN
    RAISE EXCEPTION 'V52: ejercicio 930 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 935 knee touch crunch: instructions_es: R2 L20 (ALTA): El texto pide manos detrás de la cabeza con los codos hacia fuera y alternar la mano derecha hacia la rodilla izquierda y viceversa; el GIF muestra ambas manos  / V3 W05 ACEPTAR: f1 tumbado con rodillas flexionadas y ambas manos sobre los muslos; f7 cabeza y hombros elevados con las manos en las rodillas; sin manos detrás de la cabeza, s
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en el suelo con las rodillas flexionadas y los pies apoyados. Apoya ambas manos sobre los muslos. Activa el abdomen y eleva la cabeza y los hombros del suelo deslizando las manos hasta tocar las rodillas. Baja despacio a la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en el suelo con las rodillas flexionadas y los pies apoyados.", "Apoya ambas manos sobre los muslos.", "Activa el abdomen y eleva la cabeza y los hombros del suelo deslizando las manos hasta tocar las rodillas.", "Baja despacio a la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 935 AND name_en = 'knee touch crunch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '4ded45283c094d560f35443bbd3324a8';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 935 AND name_en = 'knee touch crunch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'fc878a4fa7b0374aac2ebafc22649fa7') THEN
    RAISE EXCEPTION 'V52: ejercicio 935 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 937 kipping muscle up: instructions_es: R2 L02 (BAJA): El texto habla de una barra de dominadas y de pasar a posición de fondos sobre la barra; el GIF muestra anillas y solo la fase de balanceo y tirón, sin transici / V3 V06 MEJORAR: f1 colgado de anillas (no barra) con brazos extendidos y rodillas flexionadas; f4-f8 la cadera y las rodillas van hacia delante y el tronco se inclina atrás has
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Cuélgate de unas anillas con los brazos extendidos por encima de la cabeza y las rodillas flexionadas, sin apoyar los pies. Activa el core y balancea el cuerpo llevando la cadera y las rodillas hacia delante mientras inclinas el tronco hacia atrás, hasta quedar casi horizontal bajo las anillas. Mantén los brazos casi extendidos y el agarre firme durante el balanceo. Vuelve con control a la posición colgada vertical. Repite el número de repeticiones indicado.', instruction_steps_es = '["Cuélgate de unas anillas con los brazos extendidos por encima de la cabeza y las rodillas flexionadas, sin apoyar los pies.", "Activa el core y balancea el cuerpo llevando la cadera y las rodillas hacia delante mientras inclinas el tronco hacia atrás, hasta quedar casi horizontal bajo las anillas.", "Mantén los brazos casi extendidos y el agarre firme durante el balanceo.", "Vuelve con control a la posición colgada vertical.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 937 AND name_en = 'kipping muscle up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '9332c6443a87a6b60d96002a9f45a161';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 937 AND name_en = 'kipping muscle up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd90109f27c88d4dabb2b5bce7adff633') THEN
    RAISE EXCEPTION 'V52: ejercicio 937 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 938 kneeling lat stretch: instructions_es: R2 L03 (MEDIA): El texto pide deslizar ambas manos y llevar la cadera hacia los talones; en el GIF solo un brazo se estira hacia delante, la otra mano queda apoyada bajo el tro / V3 V06 ACEPTAR: f1 a cuatro apoyos en el suelo; f7 un solo brazo estirado hacia delante sobre el suelo, la otra mano apoyada bajo el tronco, pecho más bajo y cadera sobre las r
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate a cuatro apoyos en el suelo con las manos bajo los hombros y las rodillas bajo la cadera. Desliza un brazo estirado hacia delante por el suelo y deja que el pecho baje, con la otra mano apoyada bajo el tronco. Mantén la cadera sobre las rodillas y nota el estiramiento en el costado de la espalda. Vuelve despacio a cuatro apoyos. Alterna los lados. Mantén la duración indicada.', instruction_steps_es = '["Colócate a cuatro apoyos en el suelo con las manos bajo los hombros y las rodillas bajo la cadera.", "Desliza un brazo estirado hacia delante por el suelo y deja que el pecho baje, con la otra mano apoyada bajo el tronco.", "Mantén la cadera sobre las rodillas y nota el estiramiento en el costado de la espalda.", "Vuelve despacio a cuatro apoyos.", "Alterna los lados.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 938 AND name_en = 'kneeling lat stretch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd2c7e68bc71f31b7d670e8c80742031f';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 938 AND name_en = 'kneeling lat stretch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'db385befc3f0c60b71020b4752a912e4') THEN
    RAISE EXCEPTION 'V52: ejercicio 938 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 939 kick out sit: zona: R2 L04 (MEDIA): El trabajo visible es isométrico de cuádriceps más extensión de rodilla (el GIF resalta cuádriceps), no isquiotibiales; pies alternan, uno siempre en el suelo / V3 V06 ACEPTAR: f1-f18 sentadilla isométrica contra la pared con muslos paralelos; se resaltan los cuádriceps y una pierna se extiende al frente de forma alterna; no hay trabaj
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs'), target_muscle = 'quads'
   WHERE exercise_id = 939 AND name_en = 'kick out sit' AND body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs') AND target_muscle = 'hamstrings';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 939 AND name_en = 'kick out sit' AND body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs') AND target_muscle = 'quads') THEN
    RAISE EXCEPTION 'V52: ejercicio 939 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 940 kettlebell double jerk: instructions_es: R2 L05 (MEDIA): El texto describe una recepción en sentadilla parcial con las rodillas ligeramente flexionadas; el GIF muestra la recepción en zancada profunda con una pierna a / V3 V06 ACEPTAR: f1 de pie con dos pesas rusas en los hombros; f5-f7 brazos extendidos arriba con una pierna atrás en zancada profunda, sin apoyar la rodilla; el texto actual de
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con una pesa rusa en cada mano apoyada a la altura de los hombros. Flexiona ligeramente las rodillas y extiéndelas con fuerza para impulsar las pesas hacia arriba. Mientras empujas las pesas por encima de la cabeza, lleva una pierna atrás y recíbelas en zancada con los brazos extendidos. Junta los pies para ponerte de pie con las pesas arriba y bájalas con control a los hombros. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con una pesa rusa en cada mano apoyada a la altura de los hombros.", "Flexiona ligeramente las rodillas y extiéndelas con fuerza para impulsar las pesas hacia arriba.", "Mientras empujas las pesas por encima de la cabeza, lleva una pierna atrás y recíbelas en zancada con los brazos extendidos.", "Junta los pies para ponerte de pie con las pesas arriba y bájalas con control a los hombros.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 940 AND name_en = 'kettlebell double jerk' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e00db593693b6287222d19c830ac1e16';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 940 AND name_en = 'kettlebell double jerk' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5d78f67466589a2847f443f0bda023ed') THEN
    RAISE EXCEPTION 'V52: ejercicio 940 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 948 kettlebell pirate supper legs: movement_pattern: R2 L13 (ALTA): Es una elevación frontal con el brazo recto, no un press: SHOULDER_RAISE. Dificultad 2 parece suficiente (solo propuesta). Un solo brazo. / V3 W05 ACEPTAR: f1 pesa rusa colgando delante con el brazo extendido; f7 brazo recto por encima de la cabeza con la pesa arriba; el codo está extendido en ambas poses: elevació
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET movement_pattern = 'SHOULDER_RAISE'
   WHERE exercise_id = 948 AND name_en = 'kettlebell pirate supper legs' AND movement_pattern = 'VERTICAL_PUSH';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 948 AND name_en = 'kettlebell pirate supper legs' AND movement_pattern = 'SHOULDER_RAISE') THEN
    RAISE EXCEPTION 'V52: ejercicio 948 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 952 kettlebell thruster: high_impact: R2 L17 (ALTA): Los pies no despegan en ningún fotograma: no hay salto ni levantamiento olímpico, HI no procede e impacto BAJO. El equipo «kettlebell» sirve, aunque se usan dos / V3 W05 ACEPTAR: f1 de pie con dos pesas a los hombros; f7 sentadilla profunda; f13 de pie con las pesas arriba; el punto más bajo de los pies es el mismo en los 18 fotogramas:  / coordinador: thruster: no es salto, pliometria ni olimpico; pies con el mismo punto mas bajo en los 18 fotogramas (V3 ALTA) | instructions_es: R2 L17 (ALTA): El texto describe una sola pesa rusa sujeta con ambas manos frente al pecho; el GIF muestra dos pesas rusas, una en cada mano a la altura de los hombros / V3 W05 ACEPTAR: f1 una pesa rusa en cada mano a la altura de los hombros (no una sola pesa con ambas manos); f7 sentadilla profunda; f13 ambas pesas por encima de la cabeza con | impact_level: R2 L17 (ALTA): pies=AMBOS_SIEMPRE; De pie con una pesa rusa en cada mano a la altura de los hombros → sentadilla profunda → se pone de pie y empuja ambas pesas por encima de l / V3 W05 ACEPTAR: f1-f18 ambos pies apoyados siempre, mismo punto más bajo de los pies en todos los fotogramas; ningún fotograma con los dos pies en el aire: BAJO / coordinador: pies siempre apoyados (V3 ALTA)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET high_impact = FALSE, instructions_es = 'Ponte de pie con los pies separados a la anchura de los hombros y una pesa rusa en cada mano a la altura de los hombros. Baja a una sentadilla flexionando las rodillas y llevando la cadera atrás, con el pecho elevado y la espalda recta. Sube con fuerza y, al ponerte de pie, empuja ambas pesas por encima de la cabeza hasta extender los codos. Baja las pesas a los hombros con control antes de la siguiente sentadilla. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies separados a la anchura de los hombros y una pesa rusa en cada mano a la altura de los hombros.", "Baja a una sentadilla flexionando las rodillas y llevando la cadera atrás, con el pecho elevado y la espalda recta.", "Sube con fuerza y, al ponerte de pie, empuja ambas pesas por encima de la cabeza hasta extender los codos.", "Baja las pesas a los hombros con control antes de la siguiente sentadilla.", "Repite el número de repeticiones indicado."]'::jsonb, impact_level = 'BAJO'
   WHERE exercise_id = 952 AND name_en = 'kettlebell thruster' AND high_impact = TRUE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'daa71932d7b2c857c8d8853acb531366' AND impact_level = 'ALTO';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 952 AND name_en = 'kettlebell thruster' AND high_impact = FALSE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '84ae703056aa25306e0430676ce48f25' AND impact_level = 'BAJO') THEN
    RAISE EXCEPTION 'V52: ejercicio 952 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 955 kettlebell extended range one arm press on floor: instructions_es: R2 L20 (MEDIA): El texto indica ambas rodillas dobladas con los pies planos; el GIF muestra una pierna flexionada y la otra extendida en el suelo, con el brazo libre abierto a  / V3 W05 ACEPTAR: f1 tumbado boca arriba con la pierna del lado de la pesa flexionada y el pie apoyado, la otra extendida, y el brazo libre abierto en el suelo; pesa junto al pec
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en el suelo con la pierna del lado de la pesa flexionada y la otra extendida, y abre el brazo libre apoyado en el suelo. Sujeta una pesa rusa con una mano junto al pecho. Empuja la pesa rusa hacia el techo hasta extender el brazo, con la muñeca recta. Bájala despacio a la posición inicial manteniendo el control. Completa las repeticiones y cambia de brazo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en el suelo con la pierna del lado de la pesa flexionada y la otra extendida, y abre el brazo libre apoyado en el suelo.", "Sujeta una pesa rusa con una mano junto al pecho.", "Empuja la pesa rusa hacia el techo hasta extender el brazo, con la muñeca recta.", "Bájala despacio a la posición inicial manteniendo el control.", "Completa las repeticiones y cambia de brazo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 955 AND name_en = 'kettlebell extended range one arm press on floor' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '6d0f8c2efbc4c55095cc49f44153b218';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 955 AND name_en = 'kettlebell extended range one arm press on floor' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '96854846adccb477ea2d3103445cfc0c') THEN
    RAISE EXCEPTION 'V52: ejercicio 955 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 956 kettlebell one arm floor press: instructions_es: R2 L01 (ALTA): El texto pide rodillas flexionadas y pies planos en el suelo; el GIF muestra las piernas extendidas sobre el suelo / V3 V06 ACEPTAR: f1-f12 boca arriba en el suelo con las piernas extendidas, no con las rodillas flexionadas; f1 codo y brazo apoyados en el suelo con la pesa junto al hombro; f7
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en el suelo con las piernas extendidas. Sujeta una kettlebell con una mano junto al hombro, con el codo apoyado en el suelo. Empuja la kettlebell hacia el techo hasta extender por completo el brazo. Bájala despacio hasta que la parte superior del brazo vuelva a tocar el suelo. Completa las repeticiones y cambia de brazo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en el suelo con las piernas extendidas.", "Sujeta una kettlebell con una mano junto al hombro, con el codo apoyado en el suelo.", "Empuja la kettlebell hacia el techo hasta extender por completo el brazo.", "Bájala despacio hasta que la parte superior del brazo vuelva a tocar el suelo.", "Completa las repeticiones y cambia de brazo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 956 AND name_en = 'kettlebell one arm floor press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '6fd9bb59bc7995a40ee532eb925f5b7c';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 956 AND name_en = 'kettlebell one arm floor press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ca8b157a3de42cee2a6c471d2105a97c') THEN
    RAISE EXCEPTION 'V52: ejercicio 956 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 958 kettlebell double windmill: instructions_es: R2 L03 (ALTA): El texto describe una sola pesa rusa en la mano derecha; el GIF (y el nombre «doble») muestra dos: una por encima de la cabeza y otra colgando en la mano contra / V3 V06 ACEPTAR: f1 dos pesas rusas, una arriba con el brazo extendido y otra colgando en la otra mano; f7 inclinación lateral con la pesa baja hacia el pie y el brazo alto exte
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies algo más separados que el ancho de los hombros y una pesa rusa en cada mano. Extiende un brazo con su pesa por encima de la cabeza y deja la otra colgando junto al muslo. Con la mirada en la pesa de arriba y el brazo alto extendido, inclina el tronco hacia el lado de la pesa baja y llévala hacia el pie. Vuelve a erguirte con control, sin doblar el brazo de arriba. Cambia de lado al terminar la serie. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies algo más separados que el ancho de los hombros y una pesa rusa en cada mano.", "Extiende un brazo con su pesa por encima de la cabeza y deja la otra colgando junto al muslo.", "Con la mirada en la pesa de arriba y el brazo alto extendido, inclina el tronco hacia el lado de la pesa baja y llévala hacia el pie.", "Vuelve a erguirte con control, sin doblar el brazo de arriba.", "Cambia de lado al terminar la serie.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 958 AND name_en = 'kettlebell double windmill' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a26463dea9137cf42a6c5d6f932ae44e';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 958 AND name_en = 'kettlebell double windmill' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '77c4bd4064ceaf795bafe42463072b62') THEN
    RAISE EXCEPTION 'V52: ejercicio 958 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 961 kettlebell one arm row: instructions_es: R2 L06 (ALTA): El texto pide los pies separados a la anchura de los hombros; el GIF muestra una postura escalonada con un pie atrás y la mano libre apoyada en el muslo delante / V3 V06 ACEPTAR: f1-f12 postura escalonada con un pie atrás, tronco inclinado y la mano libre apoyada en el muslo delantero; f7 la pesa rusa llega a la cadera; el texto actual p
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con un pie adelantado y el otro atrás e inclina el tronco hacia delante desde la cadera, con la espalda recta. Apoya la mano libre sobre el muslo de la pierna adelantada y sujeta la pesa rusa con la otra mano, con el brazo extendido. Tira de la pesa rusa hacia la cadera, manteniendo el codo cerca del cuerpo y juntando los omóplatos. Baja la pesa rusa con control hasta extender el brazo. Completa las repeticiones y cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con un pie adelantado y el otro atrás e inclina el tronco hacia delante desde la cadera, con la espalda recta.", "Apoya la mano libre sobre el muslo de la pierna adelantada y sujeta la pesa rusa con la otra mano, con el brazo extendido.", "Tira de la pesa rusa hacia la cadera, manteniendo el codo cerca del cuerpo y juntando los omóplatos.", "Baja la pesa rusa con control hasta extender el brazo.", "Completa las repeticiones y cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 961 AND name_en = 'kettlebell one arm row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e71f9de1cb4a6449da7fe78bec596cbb';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 961 AND name_en = 'kettlebell one arm row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '0bec2d52d5304cda762962916f621f46') THEN
    RAISE EXCEPTION 'V52: ejercicio 961 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 964 kettlebell front squat: instructions_es: R2 L09 (ALTA): Indica sujetar una sola pesa rusa con ambas manos frente al pecho, pero el GIF muestra dos pesas rusas, una en cada mano, apoyadas en los hombros en posición de / V3 V06 ACEPTAR: f1 dos pesas rusas, una en cada mano, en rack sobre los hombros; f7 sentadilla profunda; el texto actual pide una sola pesa sujeta con las dos manos frente al p
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies separados a la anchura de los hombros y las puntas ligeramente hacia fuera. Sujeta una pesa rusa en cada mano a la altura de los hombros, con los codos delante del cuerpo. Activa el core y, con el pecho elevado, baja la cadera hacia abajo y atrás flexionando las rodillas hasta una sentadilla profunda o hasta donde puedas con control. Empuja con los talones para volver a ponerte de pie. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies separados a la anchura de los hombros y las puntas ligeramente hacia fuera.", "Sujeta una pesa rusa en cada mano a la altura de los hombros, con los codos delante del cuerpo.", "Activa el core y, con el pecho elevado, baja la cadera hacia abajo y atrás flexionando las rodillas hasta una sentadilla profunda o hasta donde puedas con control.", "Empuja con los talones para volver a ponerte de pie.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 964 AND name_en = 'kettlebell front squat' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'f35ed58c2eca8efd50ba3591f9aea161';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 964 AND name_en = 'kettlebell front squat' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'f0a7c719440d1f1675ce139b78d6bdd2') THEN
    RAISE EXCEPTION 'V52: ejercicio 964 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 967 kettlebell lunge pass through: instructions_es: R2 L12 (MEDIA): El texto sujeta la pesa rusa frente al pecho con ambas manos; el GIF la muestra en una mano junto al costado y solo se cambia de mano bajo el muslo; tampoco se  / V3 W05 ACEPTAR: f1 de pie con la pesa rusa en una mano junto al costado (no frente al pecho con ambas manos); f7 zancada adelante con la rodilla trasera cerca del suelo y la pe
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies separados a la anchura de las caderas y una pesa rusa en una mano, con el brazo junto al costado. Da un paso largo y baja a una zancada hasta que la rodilla trasera quede cerca del suelo, con la rodilla delantera alineada con el pie. En la posición baja, pasa la pesa rusa por debajo del muslo delantero a la otra mano. Empuja con el pie delantero para volver a ponerte de pie. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies separados a la anchura de las caderas y una pesa rusa en una mano, con el brazo junto al costado.", "Da un paso largo y baja a una zancada hasta que la rodilla trasera quede cerca del suelo, con la rodilla delantera alineada con el pie.", "En la posición baja, pasa la pesa rusa por debajo del muslo delantero a la otra mano.", "Empuja con el pie delantero para volver a ponerte de pie.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 967 AND name_en = 'kettlebell lunge pass through' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '1e2604197efa54fd084cc38efe222e83';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 967 AND name_en = 'kettlebell lunge pass through' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '7959dea1c438d79b5d1b7fb15e69383b') THEN
    RAISE EXCEPTION 'V52: ejercicio 967 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 971 kettlebell double alternating hang clean: instructions_es: R2 L16 (MEDIA): El texto pide inclinarse desde la cadera, una extensión explosiva de cadera y rodillas con encogimiento y atrapar ambas pesas rusas; el GIF muestra el tronco er / V3 W05 ACEPTAR: f1 tronco erguido, pies algo más separados que los hombros y una pesa en cada mano con brazos extendidos; f7 una sola pesa sube a rack con el codo cerca del cue
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies algo más separados que los hombros y una pesa rusa en cada mano, con los brazos extendidos. Mantén el tronco erguido y el core activado. Lleva una pesa rusa hasta el hombro flexionando el codo y recíbela en posición de rack, con el codo cerca del cuerpo. Bájala con control hasta extender el brazo. Repite con el otro brazo. Alterna los lados. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies algo más separados que los hombros y una pesa rusa en cada mano, con los brazos extendidos.", "Mantén el tronco erguido y el core activado.", "Lleva una pesa rusa hasta el hombro flexionando el codo y recíbela en posición de rack, con el codo cerca del cuerpo.", "Bájala con control hasta extender el brazo.", "Repite con el otro brazo. Alterna los lados.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 971 AND name_en = 'kettlebell double alternating hang clean' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'c9fcb8e0b651d7c2940faa19eee0915e';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 971 AND name_en = 'kettlebell double alternating hang clean' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a18c1bf693baf594fbceb3c084fd8b34') THEN
    RAISE EXCEPTION 'V52: ejercicio 971 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 972 kneeling jump squat: instructions_es: R2 L17 (MEDIA): El texto pide extender por completo tobillos, rodillas y caderas en el salto; el GIF aterriza directamente en sentadilla profunda sin llegar a extender, y luego / V3 W05 ACEPTAR: f1 de rodillas con las puntas de los pies apoyadas y la barra en la espalda; f7 sentadilla profunda con pies planos, sin llegar a extender caderas y rodillas co
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Arrodíllate en el suelo con las rodillas separadas a la anchura de las caderas, las puntas de los pies apoyadas y la barra sobre la parte alta de la espalda. Mantén el tronco erguido y el core activado. Impulsa la cadera de forma explosiva para pasar de las rodillas a los pies y aterriza con suavidad en sentadilla profunda. Vuelve a apoyar las rodillas en el suelo con control. Repite el número de repeticiones indicado.', instruction_steps_es = '["Arrodíllate en el suelo con las rodillas separadas a la anchura de las caderas, las puntas de los pies apoyadas y la barra sobre la parte alta de la espalda.", "Mantén el tronco erguido y el core activado.", "Impulsa la cadera de forma explosiva para pasar de las rodillas a los pies y aterriza con suavidad en sentadilla profunda.", "Vuelve a apoyar las rodillas en el suelo con control.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 972 AND name_en = 'kneeling jump squat' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '03c081210eb7a2d10eade614c3b31155';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 972 AND name_en = 'kneeling jump squat' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '16ec6c14f2272fbcff1df298416fb7ac') THEN
    RAISE EXCEPTION 'V52: ejercicio 972 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 974 l-sit on floor: instructions_es: norma de texto del cierre: la frase final pedia repeticiones («Repite el número de repeticiones deseado.») y la prescripcion vigente es DURATION, que la R2 (animacion completa) no propuso cambiar; solo se sustituye la frase final
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en el suelo con las piernas extendidas frente a ti. Coloca las manos en el suelo junto a las caderas, con los dedos apuntando hacia adelante. Activa el core y levanta las piernas del suelo, manteniéndolas rectas. Intenta llevar las piernas paralelas al suelo, formando una ''L'' con el cuerpo. Mantén esta posición durante el mayor tiempo posible. Baja lentamente las piernas de vuelta a la posición inicial. Mantén la duración indicada.', instruction_steps_es = '["Siéntate en el suelo con las piernas extendidas frente a ti.", "Coloca las manos en el suelo junto a las caderas, con los dedos apuntando hacia adelante.", "Activa el core y levanta las piernas del suelo, manteniéndolas rectas.", "Intenta llevar las piernas paralelas al suelo, formando una ''L'' con el cuerpo.", "Mantén esta posición durante el mayor tiempo posible.", "Baja lentamente las piernas de vuelta a la posición inicial.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 974 AND name_en = 'l-sit on floor' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ace7920cbd9be5fac9b12b574523b849';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 974 AND name_en = 'l-sit on floor' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '2b97668e7811c5e9406d3c952f01bc6f') THEN
    RAISE EXCEPTION 'V52: ejercicio 974 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 978 leg up hamstring stretch: instructions_es: R2 L03 (ALTA): El texto incluye una dosis concreta («Mantén el estiramiento durante 20-30 segundos»), que no debe aparecer; el resto coincide con el GIF, que muestra además un / V3 V06 ACEPTAR: f1 boca arriba con la cabeza en el suelo y las manos detrás del muslo; f5-f8 la rodilla se extiende hasta casi la vertical y vuelve a flexionarse en vaivén; el 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en el suelo con las piernas extendidas. Flexiona una rodilla y sujeta con ambas manos la parte posterior del muslo. Estira la rodilla llevando la pierna hacia la vertical hasta notar el estiramiento detrás del muslo, sin despegar la cabeza del suelo. Puedes flexionar y extender la rodilla suavemente. Alterna los lados. Mantén la duración indicada.', instruction_steps_es = '["Túmbate boca arriba en el suelo con las piernas extendidas.", "Flexiona una rodilla y sujeta con ambas manos la parte posterior del muslo.", "Estira la rodilla llevando la pierna hacia la vertical hasta notar el estiramiento detrás del muslo, sin despegar la cabeza del suelo.", "Puedes flexionar y extender la rodilla suavemente.", "Alterna los lados.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 978 AND name_en = 'leg up hamstring stretch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '99940562b0ab713ded9ad746c27facad';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 978 AND name_en = 'leg up hamstring stretch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b8bfa25b7e88157c2cc4a32706930e57') THEN
    RAISE EXCEPTION 'V52: ejercicio 978 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 980 landmine 180: instructions_es: R2 L05 (ALTA): El texto dice sujetar la barra frente al pecho y llevarla hacia el hombro contrario; el GIF muestra los brazos extendidos y la barra pasando de una cadera a la  / V3 V06 ACEPTAR: f1 brazos extendidos con el extremo cargado de la barra junto a una cadera; f7 tronco y cadera girados con la barra junto a la otra cadera, siempre a la altura 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca un extremo de la barra en el anclaje de landmine y ponte de pie frente al otro extremo, con los pies separados a la anchura de los hombros. Sujeta el extremo libre con ambas manos y los brazos extendidos, junto a una cadera. Gira el tronco y la cadera para llevar la barra en arco hasta la otra cadera, manteniendo los brazos extendidos. Vuelve con control al lado inicial. Alterna los lados. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca un extremo de la barra en el anclaje de landmine y ponte de pie frente al otro extremo, con los pies separados a la anchura de los hombros.", "Sujeta el extremo libre con ambas manos y los brazos extendidos, junto a una cadera.", "Gira el tronco y la cadera para llevar la barra en arco hasta la otra cadera, manteniendo los brazos extendidos.", "Vuelve con control al lado inicial.", "Alterna los lados.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 980 AND name_en = 'landmine 180' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e0ddd01f06f4ef8d96282cac113acec1';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 980 AND name_en = 'landmine 180' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5d3950fe239304fc60890897e833857a') THEN
    RAISE EXCEPTION 'V52: ejercicio 980 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 981 lever bent over row: instructions_es: R2 L06 (MEDIA): El texto pide un agarre prono más ancho que los hombros, como en un remo con barra; en el GIF las dos manos sujetan juntas el extremo libre de una barra apoyada / V3 V06 MEJORAR: f1 y f7 las dos manos juntas justo detrás de los discos de una barra apoyada en el suelo por el otro extremo, no un agarre ancho; ampliación de f1 y f7: la barr
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca una barra con un extremo apoyado en el suelo y discos en el otro extremo. Colócate sobre la barra, con la barra entre las piernas y el extremo cargado delante de ti; flexiona ligeramente las rodillas e inclina el tronco hacia delante desde la cadera, con la espalda recta. Sujeta la barra con las dos manos juntas, justo por detrás de los discos, con los brazos extendidos. Tira de la barra hacia el tronco llevando los codos hacia atrás y juntando los omóplatos. Baja la barra con control hasta extender los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca una barra con un extremo apoyado en el suelo y discos en el otro extremo.", "Colócate sobre la barra, con la barra entre las piernas y el extremo cargado delante de ti; flexiona ligeramente las rodillas e inclina el tronco hacia delante desde la cadera, con la espalda recta.", "Sujeta la barra con las dos manos juntas, justo por detrás de los discos, con los brazos extendidos.", "Tira de la barra hacia el tronco llevando los codos hacia atrás y juntando los omóplatos.", "Baja la barra con control hasta extender los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 981 AND name_en = 'lever bent over row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'df750a3d47ed9a43660412e428191eb0';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 981 AND name_en = 'lever bent over row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '6f42ac1948944a882786db8dbf46733a') THEN
    RAISE EXCEPTION 'V52: ejercicio 981 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 982 lever one arm bent over row: instructions_es: R2 L07 (ALTA): El texto describe un remo con la barra sujeta con agarre prono y ambas manos frente al cuerpo; el GIF muestra una sola mano sujetando el extremo cargado de una  / V3 V06 MEJORAR: f1 una mano sujeta la barra justo detrás del disco y el otro extremo está en el suelo; f7 tirón hacia el costado; ampliación de f1 y f7: la barra pasa entre las
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Apoya un extremo de la barra en el suelo, contra una esquina o un anclaje que impida que se deslice, y carga el otro extremo con un disco. Colócate sobre la barra, con la barra entre las piernas y el extremo cargado delante de ti; flexiona ligeramente las rodillas e inclínate hacia delante desde la cadera con la espalda recta. Sujeta la barra con una mano justo por detrás del disco, con el brazo extendido. Tira de la barra hacia el costado manteniendo el codo cerca del cuerpo y bájala con control hasta extender el brazo. Completa la serie y cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Apoya un extremo de la barra en el suelo, contra una esquina o un anclaje que impida que se deslice, y carga el otro extremo con un disco.", "Colócate sobre la barra, con la barra entre las piernas y el extremo cargado delante de ti; flexiona ligeramente las rodillas e inclínate hacia delante desde la cadera con la espalda recta.", "Sujeta la barra con una mano justo por detrás del disco, con el brazo extendido.", "Tira de la barra hacia el costado manteniendo el codo cerca del cuerpo y bájala con control hasta extender el brazo.", "Completa la serie y cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 982 AND name_en = 'lever one arm bent over row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '056321a8c5a7a0abbf2958de1a0371fa';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 982 AND name_en = 'lever one arm bent over row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '69ea089cdbe87ca69f35bce58b55458d') THEN
    RAISE EXCEPTION 'V52: ejercicio 982 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 984 lever calf press: instructions_es: R2 L09 (ALTA): Dice ajustar el asiento para alinear los hombros con la almohadilla de la palanca y empujar la almohadilla hacia abajo, pero el GIF muestra a la persona sentada / V3 V06 ACEPTAR: f1-f12 sentado y recostado en el respaldo con las manos en los asideros laterales; las puntas de los pies empujan una plataforma hacia delante con las piernas c
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en la máquina con la espalda apoyada en el respaldo y sujeta las asas laterales. Coloca la parte delantera de los pies en la plataforma, con las piernas casi extendidas y los talones fuera del borde. Empuja la plataforma extendiendo los tobillos, sin bloquear las rodillas. Vuelve con control dejando que los talones se acerquen al cuerpo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en la máquina con la espalda apoyada en el respaldo y sujeta las asas laterales.", "Coloca la parte delantera de los pies en la plataforma, con las piernas casi extendidas y los talones fuera del borde.", "Empuja la plataforma extendiendo los tobillos, sin bloquear las rodillas.", "Vuelve con control dejando que los talones se acerquen al cuerpo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 984 AND name_en = 'lever calf press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '38a4f6a30f673cf730856c37e560455b';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 984 AND name_en = 'lever calf press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'db670a5a9879bba17c4cd4d03ae30738') THEN
    RAISE EXCEPTION 'V52: ejercicio 984 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 985 lever donkey calf raise: instructions_es: R2 L10 (ALTA): El texto sugiere colocarse de frente a la máquina sujetando unas asas, sin la flexión del tronco; el GIF muestra el tronco horizontal con la almohadilla sobre l / V3 V06 ACEPTAR: f1-f12 tronco horizontal con la almohadilla sobre la zona lumbar o la cadera y los antebrazos apoyados delante; puntas de los pies en la plataforma; f7 talones 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ajusta la almohadilla de la máquina a la altura de tu cadera. Apoya las puntas de los pies en la plataforma con los talones fuera del borde, inclina el tronco hacia delante hasta la horizontal con la almohadilla sobre la cadera y apoya los antebrazos delante. Con las rodillas casi extendidas, eleva los talones todo lo que puedas. Baja los talones con control por debajo de la plataforma. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ajusta la almohadilla de la máquina a la altura de tu cadera.", "Apoya las puntas de los pies en la plataforma con los talones fuera del borde, inclina el tronco hacia delante hasta la horizontal con la almohadilla sobre la cadera y apoya los antebrazos delante.", "Con las rodillas casi extendidas, eleva los talones todo lo que puedas.", "Baja los talones con control por debajo de la plataforma.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 985 AND name_en = 'lever donkey calf raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'fb7a37b06896cf683cb7725621b0f0a9';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 985 AND name_en = 'lever donkey calf raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e19a9d77e4b239d20f6c9eb444e8e6ca') THEN
    RAISE EXCEPTION 'V52: ejercicio 985 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 986 lever lateral raise: instructions_es: R2 L11 (ALTA): Dice mantener los brazos rectos; el GIF muestra los codos flexionados unos 90 grados durante todo el recorrido, con los brazos apoyados en las almohadillas / V3 W06 ACEPTAR: f1 codos flexionados unos 90 grados con antebrazos al frente y brazos pegados al tronco; f7 codos elevados a la altura de los hombros con los codos aún flexiona
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ajusta el asiento para que los hombros queden a la altura del eje de la máquina y siéntate con el tronco erguido. Apoya los brazos en las almohadillas con los codos flexionados y sujeta las asas. Eleva los codos hacia los lados hasta que los brazos queden paralelos al suelo. Baja con control hasta la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ajusta el asiento para que los hombros queden a la altura del eje de la máquina y siéntate con el tronco erguido.", "Apoya los brazos en las almohadillas con los codos flexionados y sujeta las asas.", "Eleva los codos hacia los lados hasta que los brazos queden paralelos al suelo.", "Baja con control hasta la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 986 AND name_en = 'lever lateral raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd966bdeb4cde6ed23b1b4fbf98d97e65';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 986 AND name_en = 'lever lateral raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '9454d4cad15484594e64ea5a448aab0d') THEN
    RAISE EXCEPTION 'V52: ejercicio 986 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 993 lever kneeling twist: instructions_es: R2 L18 (MEDIA): El texto dice girar el torso con la parte inferior fija; en el GIF el tronco permanece fijo sujeto a las asas y lo que gira es la cadera con las piernas sobre l / V3 W06 ACEPTAR: f1-f12 los hombros y el tronco no cambian de orientación con las manos fijas en las asas a la altura del pecho; lo que rota es la almohadilla de rodillas con ca
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate en la máquina con las rodillas sobre la almohadilla giratoria y el tronco erguido. Sujeta las asas delante del pecho para fijar la parte superior del cuerpo. Con el abdomen activado, gira la cadera y las piernas hacia un lado haciendo rotar la plataforma, sin mover los hombros. Vuelve al centro con control y gira hacia el lado contrario. Alterna los lados. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate en la máquina con las rodillas sobre la almohadilla giratoria y el tronco erguido.", "Sujeta las asas delante del pecho para fijar la parte superior del cuerpo.", "Con el abdomen activado, gira la cadera y las piernas hacia un lado haciendo rotar la plataforma, sin mover los hombros.", "Vuelve al centro con control y gira hacia el lado contrario.", "Alterna los lados.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 993 AND name_en = 'lever kneeling twist' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '7990dceb151705f32cba65f02c42835d';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 993 AND name_en = 'lever kneeling twist' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5447f59e72147b4b1ed43b9d0846e256') THEN
    RAISE EXCEPTION 'V52: ejercicio 993 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 997 lever bent-over row with v-bar: instructions_es: R2 L02 (MEDIA): El texto habla de ajustar el asiento y colocarse frente a una máquina; el GIF muestra una barra libre anclada al suelo (landmine), sin asiento, con la persona i / V3 V06 ACEPTAR: f1-f12 barra libre con un extremo en una base de landmine en el suelo y discos en el otro; persona inclinada sobre la barra con rodillas flexionadas; f7 tirón h | difficulty_level: R2 L02 (MEDIA): mantener la bisagra con carga / V3 V06 ACEPTAR: f1-f12 remo con peso libre manteniendo la bisagra de cadera y la espalda recta con carga (f1 brazos extendidos, f7 tirón): exige técnica y coordinación moderada
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca un extremo de la barra en un anclaje en el suelo y carga discos en el otro extremo. Colócate sobre la barra con las rodillas flexionadas y el tronco inclinado hacia delante con la espalda recta. Sujeta la barra junto a los discos con ambas manos y los brazos estirados. Tira de la barra hacia el abdomen flexionando los codos y juntando los omóplatos. Baja de forma controlada hasta estirar los brazos sin redondear la espalda. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca un extremo de la barra en un anclaje en el suelo y carga discos en el otro extremo.", "Colócate sobre la barra con las rodillas flexionadas y el tronco inclinado hacia delante con la espalda recta.", "Sujeta la barra junto a los discos con ambas manos y los brazos estirados.", "Tira de la barra hacia el abdomen flexionando los codos y juntando los omóplatos.", "Baja de forma controlada hasta estirar los brazos sin redondear la espalda.", "Repite el número de repeticiones indicado."]'::jsonb, difficulty_level = 2
   WHERE exercise_id = 997 AND name_en = 'lever bent-over row with v-bar' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5c07af19e473607e5891ca9b260a90d4' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 997 AND name_en = 'lever bent-over row with v-bar' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '9a7c6b41c15baeb844ee4cd5cf5d5d40' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 997 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1001 lever high row: requires_overhead: R2 L06 (MEDIA): En f1 las manos quedan por encima del nivel de la cabeza con los brazos elevados hacia delante: caso límite, se propone añadir OH por prudencia. / V3 V06 ACEPTAR: f1 brazos extendidos hacia arriba y al frente con las manos por encima de lo alto de la cabeza (unos 8 px) y flexión de hombro de unos 120 grados; f7 manos junt
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = TRUE
   WHERE exercise_id = 1001 AND name_en = 'lever high row' AND requires_overhead = FALSE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1001 AND name_en = 'lever high row' AND requires_overhead = TRUE) THEN
    RAISE EXCEPTION 'V52: ejercicio 1001 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1006 lever horizontal one leg press: instructions_es: R2 L11 (ALTA): Dice colocar los pies separados a la altura de los hombros sobre la placa; en el GIF solo un pie va sobre la plataforma y el otro descansa en el apoyo inferior / V3 W06 ACEPTAR: f1 y f7 un solo pie en la plataforma y el otro apoyado en el soporte inferior de la máquina, manos en las asas laterales del asiento; f1 rodilla flexionada, f7 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ajusta el asiento de la prensa para que la rodilla quede flexionada unos 90 grados con el pie en la plataforma. Siéntate con la espalda apoyada en el respaldo, coloca un pie en el centro de la plataforma y deja el otro en el apoyo inferior. Sujeta las asas laterales y empuja extendiendo la pierna sin bloquear la rodilla. Flexiona la rodilla con control hasta la posición inicial, manteniendo la rodilla alineada con el pie. Repite el número de repeticiones indicado. Alterna los lados.', instruction_steps_es = '["Ajusta el asiento de la prensa para que la rodilla quede flexionada unos 90 grados con el pie en la plataforma.", "Siéntate con la espalda apoyada en el respaldo, coloca un pie en el centro de la plataforma y deja el otro en el apoyo inferior.", "Sujeta las asas laterales y empuja extendiendo la pierna sin bloquear la rodilla.", "Flexiona la rodilla con control hasta la posición inicial, manteniendo la rodilla alineada con el pie.", "Repite el número de repeticiones indicado. Alterna los lados."]'::jsonb
   WHERE exercise_id = 1006 AND name_en = 'lever horizontal one leg press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '8f006a2a348e3dec04f09bd8d104682b';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1006 AND name_en = 'lever horizontal one leg press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '88afab6e16cc35bba0240714a4d65f31') THEN
    RAISE EXCEPTION 'V52: ejercicio 1006 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1007 lever kneeling leg curl: instructions_es: R2 L12 (ALTA): El texto apoya las dos rodillas y flexiona las dos piernas; el GIF muestra un ejercicio unilateral: una rodilla apoyada en la almohadilla y la otra pierna traba / V3 W06 ACEPTAR: f1 rodilla de la pierna libre apoyada en su almohadilla y pierna que trabaja extendida con el tobillo bajo el rodillo; f7 esa rodilla flexionada con el rodillo 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ajusta la máquina y selecciona el peso. Arrodíllate en la máquina apoyando una rodilla en su almohadilla y coloca el tobillo de la otra pierna bajo el rodillo. Apoya los antebrazos en el soporte delantero y sujeta las asas. Sin mover el tronco, flexiona la rodilla de la pierna que trabaja llevando el talón hacia el glúteo. Baja con control hasta extender la pierna. Repite el número de repeticiones indicado y después cambia de pierna.', instruction_steps_es = '["Ajusta la máquina y selecciona el peso.", "Arrodíllate en la máquina apoyando una rodilla en su almohadilla y coloca el tobillo de la otra pierna bajo el rodillo.", "Apoya los antebrazos en el soporte delantero y sujeta las asas.", "Sin mover el tronco, flexiona la rodilla de la pierna que trabaja llevando el talón hacia el glúteo.", "Baja con control hasta extender la pierna.", "Repite el número de repeticiones indicado y después cambia de pierna."]'::jsonb
   WHERE exercise_id = 1007 AND name_en = 'lever kneeling leg curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'f8bc478e4f473eea216089f125f01759';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1007 AND name_en = 'lever kneeling leg curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'bb8bbf9ce53c25fa95832d53932f521b') THEN
    RAISE EXCEPTION 'V52: ejercicio 1007 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1010 lever lying two-one leg curl: instructions_es: R2 L15 (MEDIA): El texto describe solo la flexión con ambas piernas; el GIF muestra fases con ambas piernas y fases con una sola pierna sosteniendo el rodillo (variante dos pie / V3 W06 MEJORAR: f1-f12 repetición con las dos piernas (f7 ambas rodillas flexionadas, f8-f12 bajan juntas); f13-f24 repetición con una sola pierna (f19 una rodilla flexionada c
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca abajo en la máquina con el rodillo apoyado justo encima de los tobillos y sujeta las asas. Flexiona las dos rodillas llevando el rodillo hacia los glúteos sin despegar la cadera de la almohadilla y bájalo despacio hasta estirar las piernas. En la siguiente repetición deja una pierna estirada sobre la almohadilla y flexiona solo la otra rodilla, llevando el rodillo hacia el glúteo. Baja el rodillo con control hasta estirar la pierna. Alterna repeticiones con las dos piernas y con una sola. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca abajo en la máquina con el rodillo apoyado justo encima de los tobillos y sujeta las asas.", "Flexiona las dos rodillas llevando el rodillo hacia los glúteos sin despegar la cadera de la almohadilla y bájalo despacio hasta estirar las piernas.", "En la siguiente repetición deja una pierna estirada sobre la almohadilla y flexiona solo la otra rodilla, llevando el rodillo hacia el glúteo.", "Baja el rodillo con control hasta estirar la pierna.", "Alterna repeticiones con las dos piernas y con una sola.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1010 AND name_en = 'lever lying two-one leg curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '81f738aa8ebd93f08dd59371e879626d';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1010 AND name_en = 'lever lying two-one leg curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ab2995318b25c7a87b30cf27a16038b1') THEN
    RAISE EXCEPTION 'V52: ejercicio 1010 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1011 lever bicep curl: instructions_es: R2 L16 (ALTA): El texto dice apoyar la espalda en la almohadilla y mantener los codos cerca de los costados; el GIF muestra la parte superior de los brazos apoyada en almohadi / V3 W06 ACEPTAR: f1 parte superior de los brazos sobre almohadillas horizontales a la altura de los hombros, separada del tronco, brazos extendidos; f7 codos flexionados con las
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ajusta la altura del asiento para que los brazos queden apoyados en las almohadillas a la altura de los hombros. Siéntate y apoya la parte superior de los brazos en las almohadillas, con los brazos extendidos y las asas sujetas con agarre supino. Flexiona los codos y lleva las asas hacia la cabeza sin despegar los brazos de las almohadillas. Extiende los codos con control hasta la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ajusta la altura del asiento para que los brazos queden apoyados en las almohadillas a la altura de los hombros.", "Siéntate y apoya la parte superior de los brazos en las almohadillas, con los brazos extendidos y las asas sujetas con agarre supino.", "Flexiona los codos y lleva las asas hacia la cabeza sin despegar los brazos de las almohadillas.", "Extiende los codos con control hasta la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1011 AND name_en = 'lever bicep curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd52a3c2ab941024e4127c1ce776bb300';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1011 AND name_en = 'lever bicep curl' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'bb4f0424f499e9146295c34902dd6ba8') THEN
    RAISE EXCEPTION 'V52: ejercicio 1011 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1016 lever seated calf raise: instructions_es: R2 L20 (ALTA): El texto pide ajustar el asiento para que las rodillas queden ligeramente flexionadas y los pies planos sobre la placa; en el GIF las rodillas están flexionadas / V3 W06 ACEPTAR: f1 y f7 rodillas flexionadas unos 90 grados bajo la almohadilla apoyada sobre los muslos, manos en las asas de la almohadilla, parte delantera de los pies en la
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en la máquina y coloca la parte delantera de los pies sobre la plataforma, con los talones fuera del borde. Ajusta la almohadilla para que quede firme sobre los muslos, con las rodillas flexionadas a unos 90°, y sujeta las asas. Empuja con la parte delantera de los pies para elevar los talones todo lo posible. Haz una pausa breve arriba y baja los talones despacio. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en la máquina y coloca la parte delantera de los pies sobre la plataforma, con los talones fuera del borde.", "Ajusta la almohadilla para que quede firme sobre los muslos, con las rodillas flexionadas a unos 90°, y sujeta las asas.", "Empuja con la parte delantera de los pies para elevar los talones todo lo posible.", "Haz una pausa breve arriba y baja los talones despacio.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1016 AND name_en = 'lever seated calf raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'dbc1283aa9ee3a5e212a4fc6261ef7f2';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1016 AND name_en = 'lever seated calf raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ba37a73ea98e9016436673a107d534bc') THEN
    RAISE EXCEPTION 'V52: ejercicio 1016 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1017 lever seated squat calf raise on leg press machine: name_es: R2 L01 (MEDIA): Los músculos resaltados son cuádriceps y glúteos, no gemelos. Si la fuente pretendía una elevación de talones, el GIF no la muestra; revisar el emparejamiento G / V3 V06 ACEPTAR: f1 rodillas flexionadas con los pies en la plataforma y f7 rodillas extendidas en una prensa horizontal sentado; la diferencia de píxeles en la zona de los pies | instructions_es: R2 L01 (MEDIA): El texto describe una elevación de talones con las rodillas extendidas; el GIF muestra una prensa de piernas (flexo-extensión de rodillas) sin movimiento visibl / V3 V06 ACEPTAR: f1 y f7 solo flexión y extensión de rodillas, sentado con la espalda en el respaldo; no se ve elevación de talones con las rodillas extendidas como dice el text | zona: R2 L01 (MEDIA): Los músculos resaltados son cuádriceps y glúteos, no gemelos. Si la fuente pretendía una elevación de talones, el GIF no la muestra; revisar el emparejamiento G / V3 V06 ACEPTAR: el movimiento visible es extensión de rodilla con los cuádriceps resaltados (f1, f7); también se resaltan los gemelos (región posterior de la pierna en f1 y f7) | movement_pattern: R2 L01 (MEDIA): Los músculos resaltados son cuádriceps y glúteos, no gemelos. Si la fuente pretendía una elevación de talones, el GIF no la muestra; revisar el emparejamiento G / V3 V06 ACEPTAR: f1 a f7 flexión y extensión de rodillas con los pies fijos en la plataforma; no se ve flexión plantar en ningún fotograma
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Prensa de piernas sentado en máquina', instructions_es = 'Ajusta el asiento de la prensa para que las rodillas queden flexionadas con los pies sobre la plataforma. Siéntate con la espalda apoyada en el respaldo y los pies separados a la anchura de los hombros. Si la máquina tiene palancas de seguridad, suéltalas antes de empezar y vuelve a colocarlas al terminar. Empuja con los pies para extender las rodillas sin llegar a bloquearlas. Vuelve despacio a la posición inicial flexionando las rodillas. Mantén la espalda apoyada durante todo el movimiento. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ajusta el asiento de la prensa para que las rodillas queden flexionadas con los pies sobre la plataforma.", "Siéntate con la espalda apoyada en el respaldo y los pies separados a la anchura de los hombros.", "Si la máquina tiene palancas de seguridad, suéltalas antes de empezar y vuelve a colocarlas al terminar.", "Empuja con los pies para extender las rodillas sin llegar a bloquearlas.", "Vuelve despacio a la posición inicial flexionando las rodillas.", "Mantén la espalda apoyada durante todo el movimiento.", "Repite el número de repeticiones indicado."]'::jsonb, body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs'), target_muscle = 'quads', movement_pattern = 'KNEE_DOMINANT'
   WHERE exercise_id = 1017 AND name_en = 'lever seated squat calf raise on leg press machine' AND name_es = 'Elevacion de talones sentado sobre prensa de piernas' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5fda6054d85f96f227d285b5af81daa0' AND body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'lower legs') AND target_muscle = 'calves' AND movement_pattern = 'ANKLE_PLANTAR_FLEXION';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1017 AND name_en = 'lever seated squat calf raise on leg press machine' AND name_es = 'Prensa de piernas sentado en máquina' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '6a9d915a95fe784f442d13927112e731' AND body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs') AND target_muscle = 'quads' AND movement_pattern = 'KNEE_DOMINANT') THEN
    RAISE EXCEPTION 'V52: ejercicio 1017 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1022 lever shoulder press: difficulty_level: R2 L06 (MEDIA): coordinación moderada; peso libre / V3 V06 ACEPTAR: f1 sentadilla profunda con el extremo de la barra de landmine en el pecho; f5-f7 se pone de pie y empuja la barra arriba y al frente hasta extender los brazos: 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 1022 AND name_en = 'lever shoulder press' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1022 AND name_en = 'lever shoulder press' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1022 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1026 lever standing chest press: instructions_es: R2 L10 (MEDIA): El texto habla de ajustar el asiento y empujar las asas hacia delante; en el GIF no hay asiento: la persona está de pie, las asas quedan fijas y es el cuerpo co / V3 V06 ACEPTAR: f1 de pie sobre la plataforma con la espalda en un respaldo móvil y las manos en asas fijas a la altura del pecho; f7 brazos extendidos con el cuerpo y el respa
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie sobre la plataforma de la máquina, de cara a las asas, con la espalda apoyada en el respaldo. Sujeta las asas a la altura del pecho con los codos flexionados. Empuja extendiendo los brazos, de modo que el cuerpo y el respaldo se desplacen hacia atrás, sin bloquear los codos. Vuelve con control a la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie sobre la plataforma de la máquina, de cara a las asas, con la espalda apoyada en el respaldo.", "Sujeta las asas a la altura del pecho con los codos flexionados.", "Empuja extendiendo los brazos, de modo que el cuerpo y el respaldo se desplacen hacia atrás, sin bloquear los codos.", "Vuelve con control a la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1026 AND name_en = 'lever standing chest press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '2a332873a4d469924507440aee07cce3';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1026 AND name_en = 'lever standing chest press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5db17ce0a428bb2e7c62442179cdc75b') THEN
    RAISE EXCEPTION 'V52: ejercicio 1026 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1027 lever seated crunch: instructions_es: R2 L11 (ALTA): Describe inclinarse hacia atrás hasta 45 grados antes de hacer el crunch; el GIF parte del tronco erguido y solo muestra la flexión hacia delante y la vuelta. T / V3 W06 ACEPTAR: f1 tronco erguido contra el respaldo, manos en las asas a la altura de hombros y cabeza, piernas tras los rodillos; f7 tronco flexionado hacia delante con el pe
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ajusta la máquina, siéntate con la espalda apoyada y coloca los tobillos bajo los rodillos. Sujeta las asas a la altura de los hombros. Activa el abdomen y flexiona el tronco hacia delante llevando el pecho hacia las rodillas. Vuelve con control a la posición erguida. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ajusta la máquina, siéntate con la espalda apoyada y coloca los tobillos bajo los rodillos.", "Sujeta las asas a la altura de los hombros.", "Activa el abdomen y flexiona el tronco hacia delante llevando el pecho hacia las rodillas.", "Vuelve con control a la posición erguida.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1027 AND name_en = 'lever seated crunch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '0e9e9875d384884ed0086a5deb0c8146';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1027 AND name_en = 'lever seated crunch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '29d491d0f199378213f7dacd585b1573') THEN
    RAISE EXCEPTION 'V52: ejercicio 1027 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1031 lever one arm lateral high row: requires_overhead: R2 L15 (MEDIA): En la posición inicial la mano queda por encima de la cabeza: falta OH. La trayectoria es diagonal de arriba abajo, más cercana a un jalón que a un remo horizon / V3 W06 ACEPTAR: f1 y f12 mano por encima de la cabeza con el brazo extendido hacia arriba y delante sujetando el asa; f7 asa junto al pecho. Cumple la definición de OH (brazos 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = TRUE
   WHERE exercise_id = 1031 AND name_en = 'lever one arm lateral high row' AND requires_overhead = FALSE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1031 AND name_en = 'lever one arm lateral high row' AND requires_overhead = TRUE) THEN
    RAISE EXCEPTION 'V52: ejercicio 1031 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1032 lever one arm lateral wide pulldown: instructions_es: R2 L16 (MEDIA): El texto pide llevar el asa a través del cuerpo hacia la cadera opuesta; el GIF muestra un tirón vertical hacia el hombro del mismo lado / V3 W06 ACEPTAR: f1 brazo extendido por encima de la cabeza sujetando el asa; f7 asa a la altura del hombro del mismo lado con el codo junto al costado; en ningún fotograma el a
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ajusta el asiento y siéntate en la máquina con el tronco contra la almohadilla y los pies en el suelo. Sujeta el asa con una mano y el brazo extendido por encima de la cabeza. Tira del asa hacia abajo hasta la altura del hombro, llevando el codo hacia el costado y apretando la espalda. Devuelve el asa con control hasta extender el brazo. Completa las repeticiones y cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ajusta el asiento y siéntate en la máquina con el tronco contra la almohadilla y los pies en el suelo.", "Sujeta el asa con una mano y el brazo extendido por encima de la cabeza.", "Tira del asa hacia abajo hasta la altura del hombro, llevando el codo hacia el costado y apretando la espalda.", "Devuelve el asa con control hasta extender el brazo.", "Completa las repeticiones y cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1032 AND name_en = 'lever one arm lateral wide pulldown' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '4403812b961b6706c3621c800580cc68';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1032 AND name_en = 'lever one arm lateral wide pulldown' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '00cffa8de6c7d886e3e00e6a135aea3e') THEN
    RAISE EXCEPTION 'V52: ejercicio 1032 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1034 lever reverse grip lateral pulldown: name_es: R2 L18 (MEDIA): El tirón es vertical hacia los hombros; lateral no describe nada visible (probable traducción literal de lat pulldown). El tipo de agarre no se distingue. / V3 W06 ACEPTAR: f1 brazos por encima de la cabeza; f7 asas a la altura de los hombros: tirón vertical bilateral; lateral no describe nada visible y puede sugerir un tirón hacia
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Jalón con agarre inverso en máquina de palanca'
   WHERE exercise_id = 1034 AND name_en = 'lever reverse grip lateral pulldown' AND name_es = 'Jalón lateral con agarre inverso en palanca';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1034 AND name_en = 'lever reverse grip lateral pulldown' AND name_es = 'Jalón con agarre inverso en máquina de palanca') THEN
    RAISE EXCEPTION 'V52: ejercicio 1034 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1036 lever reverse t-bar row: instructions_es: R2 L20 (ALTA): El texto dice «Siéntate en la máquina» y ajustar la altura del asiento; en el GIF no hay asiento: la persona está de pie sobre las plataformas para los pies con / V3 W06 ACEPTAR: f1-f12 no hay asiento: de pie sobre las plataformas inclinadas para los pies con rodillas algo flexionadas y el pecho en la almohadilla; f1 brazos extendidos, f
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ajusta la posición de la almohadilla para el pecho y colócate de pie sobre las plataformas para los pies de la máquina. Apoya el pecho en la almohadilla, con las rodillas algo flexionadas, y sujeta las asas con los brazos extendidos. Con la espalda recta, tira de las asas hacia el pecho juntando los omóplatos. Haz una pausa breve arriba y extiende los brazos despacio hasta la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ajusta la posición de la almohadilla para el pecho y colócate de pie sobre las plataformas para los pies de la máquina.", "Apoya el pecho en la almohadilla, con las rodillas algo flexionadas, y sujeta las asas con los brazos extendidos.", "Con la espalda recta, tira de las asas hacia el pecho juntando los omóplatos.", "Haz una pausa breve arriba y extiende los brazos despacio hasta la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1036 AND name_en = 'lever reverse t-bar row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '48dc7573ddd86786eb650eec51b3c54d';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1036 AND name_en = 'lever reverse t-bar row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a7a03f225201a596a3294bb7de2999fd') THEN
    RAISE EXCEPTION 'V52: ejercicio 1036 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1041 lever unilateral row: instructions_es: R2 L05 (ALTA): El texto habla de ajustar un asiento y tirar de las asas con ambas manos; el GIF muestra una rodilla apoyada en la almohadilla, el tronco inclinado, una mano en / V3 V06 ACEPTAR: f1-f12 una espinilla y rodilla apoyadas en la almohadilla, el otro pie en el suelo, tronco inclinado y una mano en el soporte delantero; f7 la otra mano tira de
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Apoya una rodilla sobre la almohadilla de la máquina y deja el otro pie en el suelo. Inclina el tronco hacia delante con la espalda recta y apoya una mano en el soporte delantero. Sujeta el agarre con la otra mano y el brazo extendido. Tira del agarre hacia la cadera llevando el codo atrás y bájalo despacio hasta extender el brazo. Completa las repeticiones y cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Apoya una rodilla sobre la almohadilla de la máquina y deja el otro pie en el suelo.", "Inclina el tronco hacia delante con la espalda recta y apoya una mano en el soporte delantero.", "Sujeta el agarre con la otra mano y el brazo extendido.", "Tira del agarre hacia la cadera llevando el codo atrás y bájalo despacio hasta extender el brazo.", "Completa las repeticiones y cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1041 AND name_en = 'lever unilateral row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '1572f05ba1efd415e1e5b29d27fcebd8';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1041 AND name_en = 'lever unilateral row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '64bf7846894b9aa71f33b0f19d7abd06') THEN
    RAISE EXCEPTION 'V52: ejercicio 1041 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1043 lever seated good morning: axial_load: R2 L07 (MEDIA): La resistencia llega por un cable desde la columna de peso hasta las manos; no hay almohadilla ni carga sobre la espalda, por lo que AX sobra / V3 V07 ACEPTAR: f1-f12 la carga llega por un cable desde la polea baja a las manos; brazos extendidos al frente, sin barra ni almohadilla sobre espalda u hombros / coordinador: definicion literal demostrada en todos los fotogramas (R2 + V3) | name_es: R2 L07 (MEDIA): La resistencia llega por un cable desde la columna de peso hasta las manos; no hay almohadilla ni carga sobre la espalda, por lo que AX sobra / V3 V07 ACEPTAR: f1 y f7 ampliados: columna de peso con polea baja y agarre de cable (f7 cable visible hasta las manos), asiento bajo sin respaldo; no hay palanca; el nombre pro | instructions_es: R2 L07 (MEDIA): El texto habla de una máquina con respaldo y apoyapiés y de sujetar las asas o los lados del asiento; el GIF muestra un asiento bajo sin respaldo y un agarre de / V3 V07 MEJORAR: f1 tronco inclinado con las manos junto a la polea; f7 tronco vertical con los brazos aún extendidos al frente (el agarre no se lleva al cuerpo con los brazos); | equipo: R2 L07 (MEDIA): La resistencia llega por un cable desde la columna de peso hasta las manos; no hay almohadilla ni carga sobre la espalda, por lo que AX sobra / V3 V07 ACEPTAR: f1-f12 columna con polea baja y cable hasta el agarre; no aparece máquina de palanca
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET axial_load = FALSE, name_es = 'Buenos días sentado en polea baja', instructions_es = 'Siéntate en el asiento bajo frente a la polea baja, con los pies apoyados en la plataforma y las rodillas flexionadas. Sujeta el agarre del cable con ambas manos y, con los brazos extendidos, inclina el tronco hacia delante desde la cadera. Manteniendo los brazos extendidos y la espalda recta, extiende la cadera hasta llevar el tronco a la vertical. Vuelve con control a la inclinación inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en el asiento bajo frente a la polea baja, con los pies apoyados en la plataforma y las rodillas flexionadas.", "Sujeta el agarre del cable con ambas manos y, con los brazos extendidos, inclina el tronco hacia delante desde la cadera.", "Manteniendo los brazos extendidos y la espalda recta, extiende la cadera hasta llevar el tronco a la vertical.", "Vuelve con control a la inclinación inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1043 AND name_en = 'lever seated good morning' AND axial_load = TRUE AND name_es = 'Buenos días sentado en palanca' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '2ed10b2b7cbc53c19651fe1f58e7b0da';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1043 AND name_en = 'lever seated good morning' AND axial_load = FALSE AND name_es = 'Buenos días sentado en polea baja' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ff423e46d0f181c76c100483d1f688ba') THEN
    RAISE EXCEPTION 'V52: ejercicio 1043 no esta en el estado anterior esperado';
  END IF;
END $v52$;
DO $v52$ BEGIN
  IF (SELECT count(*) FROM equipment_types WHERE code IN ('cable')) <> 1 THEN
    RAISE EXCEPTION 'V52: faltan codigos de equipamiento para 1043';
  END IF;
  IF (SELECT coalesce(string_agg(r.group_no || ':' || t.code, ',' ORDER BY r.group_no || ':' || t.code), '') FROM exercise_equipment_requirements r JOIN equipment_types t ON t.equipment_id = r.equipment_id WHERE r.exercise_id = 1043) = '1:cable' THEN RETURN; END IF;
  IF (SELECT coalesce(string_agg(r.group_no || ':' || t.code, ',' ORDER BY r.group_no || ':' || t.code), '') FROM exercise_equipment_requirements r JOIN equipment_types t ON t.equipment_id = r.equipment_id WHERE r.exercise_id = 1043) <> '1:leverage machine' THEN RAISE EXCEPTION 'V52: requisitos de 1043 distintos de los esperados'; END IF;
  DELETE FROM exercise_equipment_requirements WHERE exercise_id = 1043;
  INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id) VALUES (1043, 1, (SELECT equipment_id FROM equipment_types WHERE code = 'cable'));
  UPDATE exercises SET equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'cable') WHERE exercise_id = 1043;
END $v52$;
-- 1051 lever seated dip: instructions_es: R2 L15 (ALTA): El texto dice bajar el cuerpo flexionando los codos y volver a empujarse hacia arriba; en el GIF el cuerpo permanece sentado y lo que se mueve son las asas / V3 W06 ACEPTAR: f1-f12 el cuerpo permanece sentado con la espalda en el respaldo y los pies en el suelo; f1 codos flexionados con asas a los lados, f7 brazos extendidos hacia a
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ajusta el asiento para que los pies queden planos en el suelo y siéntate con la espalda apoyada. Sujeta las asas a los lados del cuerpo con los codos flexionados. Empuja las asas hacia abajo hasta extender los brazos, sin despegar la espalda del respaldo. Deja subir las asas despacio hasta volver a flexionar los codos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ajusta el asiento para que los pies queden planos en el suelo y siéntate con la espalda apoyada.", "Sujeta las asas a los lados del cuerpo con los codos flexionados.", "Empuja las asas hacia abajo hasta extender los brazos, sin despegar la espalda del respaldo.", "Deja subir las asas despacio hasta volver a flexionar los codos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1051 AND name_en = 'lever seated dip' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3bef5992e5940eb5281b473316841eeb';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1051 AND name_en = 'lever seated dip' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '1c066dac8d64ffc4277dd947615ae708') THEN
    RAISE EXCEPTION 'V52: ejercicio 1051 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1052 lever triceps extension: instructions_es: R2 L16 (ALTA): El texto dice apoyar la espalda en la almohadilla y empezar con los brazos extendidos; el GIF muestra la parte superior de los brazos apoyada en una almohadilla / V3 W06 ACEPTAR: f1 parte superior de los brazos sobre la almohadilla inclinada delante del tronco y codos flexionados con asas junto a la cara; f7 codos extendidos hacia delant
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ajusta la altura del asiento para que la parte superior de los brazos quede apoyada en la almohadilla delantera. Siéntate, apoya los brazos en la almohadilla y sujeta las asas con los codos flexionados, junto a la cara. Extiende los codos empujando las asas hacia delante sin despegar los brazos de la almohadilla. Flexiona los codos con control hasta la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ajusta la altura del asiento para que la parte superior de los brazos quede apoyada en la almohadilla delantera.", "Siéntate, apoya los brazos en la almohadilla y sujeta las asas con los codos flexionados, junto a la cara.", "Extiende los codos empujando las asas hacia delante sin despegar los brazos de la almohadilla.", "Flexiona los codos con control hasta la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1052 AND name_en = 'lever triceps extension' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd45931d65ff7615a4dd712c3de8c4c75';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1052 AND name_en = 'lever triceps extension' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '97d77dfc38e9fa58a791f33b439260f4') THEN
    RAISE EXCEPTION 'V52: ejercicio 1052 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1053 neck side stretch: instructions_es: R2 L17 (ALTA): El texto incluye dosis concretas (15-30 segundos, 2 a 4 series por lado) que chocan con la prescripción por duración del catálogo; no menciona las manos detrás  / V3 W06 ACEPTAR: f1-f12 de pie con las manos detrás de la espalda; f1 cabeza inclinada hacia un hombro y f7 hacia el otro (alterna). El texto actual omite las manos y contiene d
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie, erguido, con los hombros relajados y las manos entrelazadas detrás de la espalda. Inclina la cabeza hacia un lado llevando la oreja hacia el hombro, sin elevar el hombro contrario. Mantén el estiramiento con una respiración tranquila. Vuelve al centro con suavidad y repite hacia el otro lado. Alterna los lados. Mantén la duración indicada.', instruction_steps_es = '["Ponte de pie, erguido, con los hombros relajados y las manos entrelazadas detrás de la espalda.", "Inclina la cabeza hacia un lado llevando la oreja hacia el hombro, sin elevar el hombro contrario.", "Mantén el estiramiento con una respiración tranquila.", "Vuelve al centro con suavidad y repite hacia el otro lado.", "Alterna los lados.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 1053 AND name_en = 'neck side stretch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e0fa907b71001148d6cf745a6f09a3a5';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1053 AND name_en = 'neck side stretch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '313c3871f8252e1a38e6f0d9cf22c4b9') THEN
    RAISE EXCEPTION 'V52: ejercicio 1053 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1054 mountain climber: instructions_es: norma de texto del cierre: la frase final pedia repeticiones («Repite el número de repeticiones deseado.») y la prescripcion vigente es DURATION, que la R2 (animacion completa) no propuso cambiar; solo se sustituye la frase final
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Comienza en una posición de plancha alta con las manos justo debajo de los hombros y el cuerpo en línea recta. Activa el core y lleva la rodilla derecha hacia el pecho, luego cambia rápidamente y lleva la rodilla izquierda hacia el pecho. Continúa alternando las piernas con un movimiento de carrera, manteniendo las caderas bajas y el core activado. Mantén un ritmo constante y respira de forma regular durante todo el ejercicio. Mantén la duración indicada.', instruction_steps_es = '["Comienza en una posición de plancha alta con las manos justo debajo de los hombros y el cuerpo en línea recta.", "Activa el core y lleva la rodilla derecha hacia el pecho, luego cambia rápidamente y lleva la rodilla izquierda hacia el pecho.", "Continúa alternando las piernas con un movimiento de carrera, manteniendo las caderas bajas y el core activado.", "Mantén un ritmo constante y respira de forma regular durante todo el ejercicio.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 1054 AND name_en = 'mountain climber' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'c5f11e4ceb02256fa8d99107e1f3f08b';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1054 AND name_en = 'mountain climber' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '54da0dd0397d5f1fb041c5741007d85a') THEN
    RAISE EXCEPTION 'V52: ejercicio 1054 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1056 one leg donkey calf raise: instructions_es: R2 L20 (ALTA): El texto dice apoyar las manos en una pared o una barra, sin mencionar la inclinación del tronco; el GIF muestra el tronco casi horizontal con las manos en un b / V3 W06 ACEPTAR: f1 y f7 tronco inclinado hacia delante con las manos apoyadas en un banco plano y brazos extendidos, una rodilla flexionada con el pie atrás en el aire; f7 taló
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate de pie frente a un banco plano, inclina el tronco hacia delante y apoya las manos sobre él con los brazos extendidos. Levanta una pierna del suelo flexionando la rodilla hacia atrás. Eleva el talón de la pierna de apoyo lo más alto posible. Haz una pausa arriba y baja el talón despacio. Completa las repeticiones y cambia de pierna. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate de pie frente a un banco plano, inclina el tronco hacia delante y apoya las manos sobre él con los brazos extendidos.", "Levanta una pierna del suelo flexionando la rodilla hacia atrás.", "Eleva el talón de la pierna de apoyo lo más alto posible.", "Haz una pausa arriba y baja el talón despacio.", "Completa las repeticiones y cambia de pierna.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1056 AND name_en = 'one leg donkey calf raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '7cf0506876e88e97e497eb52c9e649a3';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1056 AND name_en = 'one leg donkey calf raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '1a62b2aa1e90871ec5a54e911d3b509b') THEN
    RAISE EXCEPTION 'V52: ejercicio 1056 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1058 modified hindu push-up (male): instructions_es: R2 L02 (MEDIA): El texto mezcla las fases: dice que la V invertida se forma mientras bajas el pecho y termina empujando la cadera hacia abajo; el GIF muestra tres posiciones su / V3 V07 ACEPTAR: f1 flexión baja con el pecho cerca del suelo; f7 V invertida con la cabeza entre los brazos; f13 brazos extendidos, cadera baja y pecho elevado; f18 vuelve a f1 | difficulty_level: R2 L02 (MEDIA): movilidad de hombros y columna; fuerza de empuje / V3 V07 ACEPTAR: f1-f18 flexión completa desde el suelo encadenada con V invertida (f7) y vuelta: fuerza relativa moderada y coordinación, por encima del perfil 1 (nivel 2)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate en posición de flexión con las manos algo más separadas que los hombros y el pecho cerca del suelo. Empuja con los brazos y lleva la cadera hacia arriba y atrás hasta formar una V invertida, con la cabeza entre los brazos. Desde la V, baja la cadera y lleva el pecho hacia delante hasta quedar con los brazos extendidos y el pecho elevado. Flexiona los codos y vuelve a la posición de flexión con el pecho cerca del suelo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate en posición de flexión con las manos algo más separadas que los hombros y el pecho cerca del suelo.", "Empuja con los brazos y lleva la cadera hacia arriba y atrás hasta formar una V invertida, con la cabeza entre los brazos.", "Desde la V, baja la cadera y lleva el pecho hacia delante hasta quedar con los brazos extendidos y el pecho elevado.", "Flexiona los codos y vuelve a la posición de flexión con el pecho cerca del suelo.", "Repite el número de repeticiones indicado."]'::jsonb, difficulty_level = 2
   WHERE exercise_id = 1058 AND name_en = 'modified hindu push-up (male)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '13cbcb99a0552e06dc5ec4f7121d3bcb' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1058 AND name_en = 'modified hindu push-up (male)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '2f7c6dba3c07c959882baea070cca4c2' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1058 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1059 lunge with twist: instructions_es: R2 L03 (MEDIA): El texto pide llevar el codo izquierdo hacia la rodilla derecha; en el GIF los brazos permanecen junto al cuerpo con las manos a la altura de la cadera y solo s / V3 V07 ACEPTAR: f1 de pie; f7 zancada con rodilla trasera cerca del suelo, giro moderado del tronco hacia la pierna delantera y brazos junto al cuerpo (manos a la altura de la 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies separados al ancho de las caderas y los brazos relajados. Da un paso adelante y baja a una zancada hasta que la rodilla trasera quede cerca del suelo, sin apoyarla. En la posición baja, gira el tronco hacia el lado de la pierna delantera manteniendo la rodilla alineada con el pie. Deshaz el giro y empuja con el pie delantero para volver a la posición inicial. Alterna los lados. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies separados al ancho de las caderas y los brazos relajados.", "Da un paso adelante y baja a una zancada hasta que la rodilla trasera quede cerca del suelo, sin apoyarla.", "En la posición baja, gira el tronco hacia el lado de la pierna delantera manteniendo la rodilla alineada con el pie.", "Deshaz el giro y empuja con el pie delantero para volver a la posición inicial.", "Alterna los lados.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1059 AND name_en = 'lunge with twist' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'f4ac00759b95577df9ede21226a3bb38';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1059 AND name_en = 'lunge with twist' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '58af98649b8895fadc72ad4abe1d037e') THEN
    RAISE EXCEPTION 'V52: ejercicio 1059 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1060 lying elbow to knee: instructions_es: R2 L04 (ALTA): El texto pide ambos pies apoyados y extender y elevar la pierna contraria en cada repetición alternando lados; el GIF muestra una pierna cruzada con el tobillo  / V3 V07 ACEPTAR: f1 boca arriba con un pie en el suelo y el tobillo contrario cruzado sobre esa rodilla, manos tras la cabeza; f7 tronco elevado y girado hacia la rodilla cruzad
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en el suelo con las rodillas flexionadas y un pie apoyado. Cruza el tobillo de la otra pierna sobre la rodilla de la pierna apoyada y coloca las manos detrás de la cabeza. Eleva el tronco girándolo para llevar el codo contrario hacia la rodilla cruzada. Baja con control a la posición inicial y cambia de lado al terminar. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en el suelo con las rodillas flexionadas y un pie apoyado.", "Cruza el tobillo de la otra pierna sobre la rodilla de la pierna apoyada y coloca las manos detrás de la cabeza.", "Eleva el tronco girándolo para llevar el codo contrario hacia la rodilla cruzada.", "Baja con control a la posición inicial y cambia de lado al terminar.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1060 AND name_en = 'lying elbow to knee' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '6cf0161721996fdef293cb267acb95c4';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1060 AND name_en = 'lying elbow to knee' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '7d42d4f298f1020c77ab098909f2a0a3') THEN
    RAISE EXCEPTION 'V52: ejercicio 1060 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1064 oblique crunch v. 2: instructions_es: R2 L08 (ALTA): El texto pide ambas manos detrás de la cabeza; el GIF muestra una mano detrás de la cabeza y el otro brazo extendido en el suelo / V3 V07 ACEPTAR: f1 y f7 ampliados: una mano detrás de la cabeza y el otro brazo extendido en el suelo, separado del cuerpo; talones sobre el banco con rodillas flexionadas; el 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en el suelo con las piernas apoyadas sobre un banco y las rodillas flexionadas. Coloca una mano detrás de la cabeza y apoya el otro brazo extendido en el suelo, separado del cuerpo. Eleva los hombros del suelo girando el tronco para llevar el codo de la mano que está detrás de la cabeza hacia la rodilla contraria. Baja con control a la posición inicial. Completa las repeticiones y cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en el suelo con las piernas apoyadas sobre un banco y las rodillas flexionadas.", "Coloca una mano detrás de la cabeza y apoya el otro brazo extendido en el suelo, separado del cuerpo.", "Eleva los hombros del suelo girando el tronco para llevar el codo de la mano que está detrás de la cabeza hacia la rodilla contraria.", "Baja con control a la posición inicial.", "Completa las repeticiones y cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1064 AND name_en = 'oblique crunch v. 2' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'eb2c23de59542b73a778e98da329b6f0';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1064 AND name_en = 'oblique crunch v. 2' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'c2efa8f2a4def76a010bc7272445a9d4') THEN
    RAISE EXCEPTION 'V52: ejercicio 1064 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1069 muscle-up (on vertical bar): high_impact: R2 L13 (MEDIA): No hay salto, aterrizaje ni contacto con el suelo: el muscle-up no encaja en HI (saltos, pliometría, olímpicos) e implica impacto BAJO, aunque es explosivo. En  / V3 W06 ACEPTAR: f1-f18 sin salto ni aterrizaje: f1 colgado, f7 tirón con rodillas recogidas, f13 apoyo sobre la barra, f14-f18 vuelve a colgar; los pies nunca tocan el suelo. M / coordinador: muscle-up claro sin salto ni aterrizaje (f1 colgado, f7 tiron, f13 apoyo): no es salto, pliometria ni olimpico; HG y dificultad 3 siguen excluyendolo de quien no debe colgarse | impact_level: R2 L13 (MEDIA): pies=SIN_APOYO_DE_PIES; Colgado de una barra con brazos extendidos → tirón recogiendo las rodillas → pasa el tronco por encima de la barra hasta el apoyo con br / V3 W06 ACEPTAR: f1-f18 los pies nunca tocan el suelo ni hay aterrizaje: colgado (f1), tirón (f7), apoyo sobre la barra (f13) y bajada; las piernas no cargan el peso corporal; n / coordinador: pies nunca en el suelo ni aterrizaje (R2 + V3 ALTA)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET high_impact = FALSE, impact_level = 'BAJO'
   WHERE exercise_id = 1069 AND name_en = 'muscle-up (on vertical bar)' AND high_impact = TRUE AND impact_level = 'ALTO';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1069 AND name_en = 'muscle-up (on vertical bar)' AND high_impact = FALSE AND impact_level = 'BAJO') THEN
    RAISE EXCEPTION 'V52: ejercicio 1069 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1071 one arm chin-up: instructions_es: R2 L15 (MEDIA): El texto dice que la otra mano sujeta la muñeca del brazo que tira; en el GIF la otra mano agarra un poste vertical lateral / V3 W06 ACEPTAR: f1 y f7 una mano en la barra y la otra agarrando un poste vertical lateral a la altura de la cabeza, rodillas flexionadas y pies sin apoyo; f7 cabeza a la altur
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate bajo una barra de dominadas que tenga un poste vertical a un lado. Agarra la barra con una mano y el poste con la otra mano, a la altura de la cabeza. Cuélgate con el brazo de la barra extendido y los pies sin apoyo. Tira del cuerpo hacia arriba con el brazo de la barra, usando la otra mano solo como ayuda, hasta que la cabeza llegue a la altura de la barra. Baja despacio hasta extender el brazo. Completa las repeticiones y cambia de brazo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate bajo una barra de dominadas que tenga un poste vertical a un lado.", "Agarra la barra con una mano y el poste con la otra mano, a la altura de la cabeza.", "Cuélgate con el brazo de la barra extendido y los pies sin apoyo.", "Tira del cuerpo hacia arriba con el brazo de la barra, usando la otra mano solo como ayuda, hasta que la cabeza llegue a la altura de la barra.", "Baja despacio hasta extender el brazo. Completa las repeticiones y cambia de brazo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1071 AND name_en = 'one arm chin-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e9950a25d291cdf7eed696533bac65ec';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1071 AND name_en = 'one arm chin-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b4bd79b12f6b1ebcbb0da64d4b54a571') THEN
    RAISE EXCEPTION 'V52: ejercicio 1071 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1079 narrow push-up on exercise ball: difficulty_level: R2 L03 (ALTA): fuerza relativa moderada; estabilidad con pies sobre balón / V3 V07 ACEPTAR: f1-f12 flexión completa con manos juntas en el suelo y pies sobre el balón de estabilidad (f7 pecho cerca del suelo): fuerza relativa moderada y equilibrio, niv
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 1079 AND name_en = 'narrow push-up on exercise ball' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1079 AND name_en = 'narrow push-up on exercise ball' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1079 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1082 medicine ball chest push multiple response: name_es: R2 L06 (ALTA): «repuestas» es una errata de «respuestas» y «Push» es un anglicismo. La caída controlada sobre las manos exige algo más que un nivel principiante: se propone di / V3 V07 ACEPTAR: f1 de rodillas con el balón al pecho; f7 balón fuera de las manos al final del empuje (lanzamiento); f13 caída sobre las manos; f18 vuelve de rodillas. «repuest | difficulty_level: R2 L06 (ALTA): potencia de tren superior; recepción del peso corporal en las manos / V3 V07 ACEPTAR: f7-f13 lanzamiento explosivo y caída controlada del peso corporal sobre las manos desde rodillas: exige técnica básica y potencia, nivel 2 (coherente con la ver
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Lanzamiento de pecho con balón medicinal de rodillas (respuesta múltiple)', difficulty_level = 2
   WHERE exercise_id = 1082 AND name_en = 'medicine ball chest push multiple response' AND name_es = 'Push de pecho con balón medicinal, múltiples repuestas' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1082 AND name_en = 'medicine ball chest push multiple response' AND name_es = 'Lanzamiento de pecho con balón medicinal de rodillas (respuesta múltiple)' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1082 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1083 medicine ball chest push single response: impact_level: R2 L07 (MEDIA): pies=NO_APLICA; De rodillas en el suelo con un balón medicinal al pecho, lo lanza al frente, cae hacia delante recibiéndose sobre las manos, hace una flexión co / V3 X01 ACEPTAR: f1 de rodillas con balón al pecho; f2-f7 lanzamiento explosivo del balón al frente; f8-f13 el tronco cae hacia delante y se recibe sobre las manos en flexión co
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET impact_level = 'ALTO'
   WHERE exercise_id = 1083 AND name_en = 'medicine ball chest push single response' AND impact_level = 'BAJO';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1083 AND name_en = 'medicine ball chest push single response' AND impact_level = 'ALTO') THEN
    RAISE EXCEPTION 'V52: ejercicio 1083 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1084 medicine ball chest push with run release: name_es: R2 L08 (MEDIA): No se ve ninguna carrera ni fase de vuelo: ambos pies permanecen apoyados. Se propone quitar HI e impacto BAJO; si el ejercicio real incluye la carrera tras sol / V3 V07 ACEPTAR: f1-f18 no aparece ninguna carrera: pies fijos; f7 balón al pecho, f10-f13 manos abiertas sin balón tras el empuje. «y carrera» induce a error | instructions_es: R2 L08 (MEDIA): El texto describe un empuje sin soltar el balón y termina con Mantén la duración indicada; el GIF muestra que el balón se suelta al extender los brazos (manos a / V3 V07 ACEPTAR: f1 tronco inclinado y balón a la altura de las rodillas; f7 balón al pecho; f10-f13 brazos extendidos y manos abiertas sin balón (se suelta); el texto actual no | default_prescription: R2 L08 (MEDIA): No se ve ninguna carrera ni fase de vuelo: ambos pies permanecen apoyados. Se propone quitar HI e impacto BAJO; si el ejercicio real incluye la carrera tras sol / V3 V07 ACEPTAR: f1-f18 un único lanzamiento con vuelta a la posición inicial: gesto discreto por repeticiones, no actividad continua
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Lanzamiento de pecho con balón medicinal', instructions_es = 'Ponte de pie con los pies separados, el tronco inclinado y el balón medicinal sujeto con ambas manos a la altura de las rodillas. Incorpórate llevando el balón al pecho. Empuja el balón hacia delante con fuerza, extendiendo los brazos por completo y soltándolo al final del empuje. Recupera el balón y vuelve a la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies separados, el tronco inclinado y el balón medicinal sujeto con ambas manos a la altura de las rodillas.", "Incorpórate llevando el balón al pecho.", "Empuja el balón hacia delante con fuerza, extendiendo los brazos por completo y soltándolo al final del empuje.", "Recupera el balón y vuelve a la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb, default_prescription = 'SETS_REPS'
   WHERE exercise_id = 1084 AND name_en = 'medicine ball chest push with run release' AND name_es = 'Press de pecho con balón medicinal y carrera' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5edd15ab50c7278bb6592a90cd1f8cbe' AND default_prescription = 'DURATION';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1084 AND name_en = 'medicine ball chest push with run release' AND name_es = 'Lanzamiento de pecho con balón medicinal' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '7bae4dcb5f4b0d26f96e59b2716bc0e6' AND default_prescription = 'SETS_REPS') THEN
    RAISE EXCEPTION 'V52: ejercicio 1084 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1086 medicine ball catch and overhead throw: movement_pattern: R2 L10 (BAJA): No se ve ninguna sentadilla: el tren inferior hace una bisagra de cadera. El lanzamiento y la recepción no aparecen; si el ejercicio real incluye lanzar, la ins / V3 V07 ACEPTAR: f1 rodillas casi extendidas y tronco flexionado desde la cadera; en ningún fotograma hay sentadilla. El tren inferior hace bisagra; KNEE_DOMINANT no se apoya
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET movement_pattern = 'HIP_HINGE'
   WHERE exercise_id = 1086 AND name_en = 'medicine ball catch and overhead throw' AND movement_pattern = 'KNEE_DOMINANT';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1086 AND name_en = 'medicine ball catch and overhead throw' AND movement_pattern = 'HIP_HINGE') THEN
    RAISE EXCEPTION 'V52: ejercicio 1086 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1088 medicine ball close grip push up: instructions_es: R2 L12 (ALTA): El texto separa las manos a la anchura de los hombros; el GIF muestra las dos manos juntas sobre el balón / V3 W06 ACEPTAR: f1-f12 las dos manos juntas sobre un único balón medicinal, pies en el suelo y cuerpo recto; f7 pecho cerca del balón con codos flexionados. El texto actual sep | difficulty_level: R2 L12 (ALTA): equilibrio sobre superficie inestable / V3 W06 ACEPTAR: f1-f12 todo el peso del tren superior sobre un balón con las manos juntas (base estrecha e inestable) y flexión completa (f7): exige fuerza relativa moderada y 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate en plancha alta con las dos manos juntas sobre un balón medicinal y los pies apoyados en el suelo. Mantén el cuerpo en línea recta de la cabeza a los pies. Baja el pecho hacia el balón flexionando los codos, cerca de los costados. Empuja hasta extender los brazos sin perder el equilibrio sobre el balón. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate en plancha alta con las dos manos juntas sobre un balón medicinal y los pies apoyados en el suelo.", "Mantén el cuerpo en línea recta de la cabeza a los pies.", "Baja el pecho hacia el balón flexionando los codos, cerca de los costados.", "Empuja hasta extender los brazos sin perder el equilibrio sobre el balón.", "Repite el número de repeticiones indicado."]'::jsonb, difficulty_level = 2
   WHERE exercise_id = 1088 AND name_en = 'medicine ball close grip push up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '0368c150b8cefb628740ae5e0c6241bc' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1088 AND name_en = 'medicine ball close grip push up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '6e1f93cbe5c1e3edc8d568f4f3c2e965' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1088 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1095 push and pull bodyweight: movement_pattern: R2 L17 (MEDIA): No hay empuje: es una apertura y cierre horizontal de brazos sin carga, propia de calentamiento o movilidad, por lo que HORIZONTAL_PUSH no encaja. El nombre «em / V3 W06 ACEPTAR: f1 brazos extendidos al frente a la altura de los hombros; f7 brazos abiertos en cruz a la misma altura; sin carga ni empuje en ningún fotograma: apertura y cie
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET movement_pattern = 'MOBILITY'
   WHERE exercise_id = 1095 AND name_en = 'push and pull bodyweight' AND movement_pattern = 'HORIZONTAL_PUSH';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1095 AND name_en = 'push and pull bodyweight' AND movement_pattern = 'MOBILITY') THEN
    RAISE EXCEPTION 'V52: ejercicio 1095 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1102 posterior step to overhead reach: instructions_es: R2 L04 (MEDIA): El texto pide flexionar la rodilla y bajar a una posición de zancada; en el GIF la rodilla delantera apenas se flexiona y el cuerpo no baja: es un paso atrás co / V3 V07 ACEPTAR: f1 de pie; f7 pierna atrasada en punta, rodillas casi extendidas, sin bajar a zancada, brazos estirados por encima de la cabeza; f12 vuelve a pies juntos
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies separados a la anchura de las caderas y los brazos a los lados. Da un paso atrás con una pierna apoyando la punta del pie, con las rodillas casi estiradas. A la vez, eleva los brazos estirados por encima de la cabeza y estira el cuerpo hacia arriba. Vuelve a juntar los pies bajando los brazos. Alterna los lados. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies separados a la anchura de las caderas y los brazos a los lados.", "Da un paso atrás con una pierna apoyando la punta del pie, con las rodillas casi estiradas.", "A la vez, eleva los brazos estirados por encima de la cabeza y estira el cuerpo hacia arriba.", "Vuelve a juntar los pies bajando los brazos.", "Alterna los lados.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1102 AND name_en = 'posterior step to overhead reach' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '7db6c474ea73bff07b24bbed1b15aef7';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1102 AND name_en = 'posterior step to overhead reach' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '93fb92db65fcce220cfbadc7ff17e057') THEN
    RAISE EXCEPTION 'V52: ejercicio 1102 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1104 power point plank: instructions_es: R2 L06 (MEDIA): El texto pide elevar a la vez un brazo y la pierna contraria; en el GIF el brazo (f7, f19) y la pierna (f13) se elevan por separado, con los otros tres apoyos e / V3 V07 ACEPTAR: f7 se eleva un brazo con piernas apoyadas; f13 se eleva una pierna con ambas manos apoyadas; f19 el otro brazo; f1/f24 la otra pierna; nunca brazo y pierna a la
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate en plancha alta con las manos en el suelo bajo los hombros y el cuerpo en línea recta. Eleva un brazo extendido al frente y vuelve a apoyarlo. Eleva una pierna extendida hacia atrás y vuelve a apoyarla. Mantén la cadera estable y el abdomen activo durante todo el ejercicio. Alterna los lados. Mantén la duración indicada.', instruction_steps_es = '["Colócate en plancha alta con las manos en el suelo bajo los hombros y el cuerpo en línea recta.", "Eleva un brazo extendido al frente y vuelve a apoyarlo.", "Eleva una pierna extendida hacia atrás y vuelve a apoyarla.", "Mantén la cadera estable y el abdomen activo durante todo el ejercicio.", "Alterna los lados.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 1104 AND name_en = 'power point plank' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '314546dac1259e6ac9d758186d67b76c';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1104 AND name_en = 'power point plank' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '9c64a2b9e5313c0e62370512f84896b1') THEN
    RAISE EXCEPTION 'V52: ejercicio 1104 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1111 outside leg kick push-up: instructions_es: R2 L13 (MEDIA): El texto describe lanzar la pierna hacia el lado manteniendo la flexión; el GIF muestra que, al subir, se levanta una mano, el tronco gira y la pierna pasa por  / V3 W06 MEJORAR: f1 posición baja de flexión con codos flexionados; f7 apoyo en una mano y un pie, la otra mano despegada, tronco girado y una pierna pasada por debajo del cuerp | difficulty_level: R2 L13 (MEDIA): coordinación; apoyo en un brazo / V3 W06 ACEPTAR: f7 apoyo en una sola mano y un pie con el tronco girado y una pierna pasada por debajo del cuerpo, tras una flexión (f1): exige coordinación y fuerza relativa m
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate en posición de flexión con las manos algo más separadas que los hombros y el cuerpo en línea recta. Baja el pecho hacia el suelo flexionando los codos. Al empujar hacia arriba, despega una mano del suelo, gira el tronco hacia ese lado y pasa la pierna contraria por debajo del cuerpo, con la rodilla flexionada, hasta quedar apoyado en una mano y un pie. Deshaz el giro, apoya de nuevo la mano y vuelve a la posición de flexión. Alterna los lados. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate en posición de flexión con las manos algo más separadas que los hombros y el cuerpo en línea recta.", "Baja el pecho hacia el suelo flexionando los codos.", "Al empujar hacia arriba, despega una mano del suelo, gira el tronco hacia ese lado y pasa la pierna contraria por debajo del cuerpo, con la rodilla flexionada, hasta quedar apoyado en una mano y un pie.", "Deshaz el giro, apoya de nuevo la mano y vuelve a la posición de flexión.", "Alterna los lados.", "Repite el número de repeticiones indicado."]'::jsonb, difficulty_level = 2
   WHERE exercise_id = 1111 AND name_en = 'outside leg kick push-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '32c5ed6499d7da06c978a8e01004ee6d' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1111 AND name_en = 'outside leg kick push-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a04dc7ff086130bda4a8e0db5aa593ca' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1111 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1115 push-up inside leg kick: instructions_es: R2 L17 (MEDIA): El texto indica levantar la pierna recta al empujar hacia arriba; en el GIF la pierna se despega mientras baja, está elevada en la posición baja con la rodilla  / V3 W06 ACEPTAR: f1 plancha alta con brazos extendidos y pies juntos; f7 posición baja con una pierna despegada y elevada hacia el lado con la rodilla flexionada; f12 vuelve arr | difficulty_level: R2 L17 (MEDIA): fuerza relativa moderada; apoyo sobre una pierna / V3 W06 ACEPTAR: f7 flexión en posición baja con apoyo en las dos manos y un solo pie: fuerza relativa moderada y estabilidad del tronco con tres apoyos; no es asequible para un
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate en posición de flexión con las manos algo más separadas que los hombros y el cuerpo recto. Baja el pecho hacia el suelo flexionando los codos y, a la vez, despega una pierna y llévala hacia el lado. Mantén el core activado y la cadera estable en la posición baja. Empuja el suelo para subir mientras devuelves el pie a su sitio. Alterna los lados. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate en posición de flexión con las manos algo más separadas que los hombros y el cuerpo recto.", "Baja el pecho hacia el suelo flexionando los codos y, a la vez, despega una pierna y llévala hacia el lado.", "Mantén el core activado y la cadera estable en la posición baja.", "Empuja el suelo para subir mientras devuelves el pie a su sitio.", "Alterna los lados.", "Repite el número de repeticiones indicado."]'::jsonb, difficulty_level = 2
   WHERE exercise_id = 1115 AND name_en = 'push-up inside leg kick' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '04067f0794dedc72d1716cb88dd777ca' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1115 AND name_en = 'push-up inside leg kick' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '2bbd8c8c20d9ab8fe07c96f354417044' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1115 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1117 quick feet v. 2: instructions_es: R2 L18 (MEDIA): El texto dice brazos a los costados y no menciona la postura; el GIF muestra el tronco inclinado hacia delante, rodillas flexionadas y las manos juntas delante  / V3 W06 ACEPTAR: f1-f12 tronco inclinado hacia delante, rodillas flexionadas, pies más separados que los hombros y manos juntas delante del cuerpo; f1 y f7 cada pie apoyado alte | impact_level: R2 L18 (MEDIA): pies=ALTERNOS_SIN_VUELO; De pie con las rodillas flexionadas, el tronco inclinado hacia delante y las manos juntas delante del cuerpo; levanta y apoya los pies  / V3 W06 ACEPTAR: Solo dos poses clave de 1 s (f1 pie derecho apoyado y el izquierdo elevado; f7 al revés) en fundido; ningún fotograma con los dos pies en el aire, pero es un ej
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies algo más separados que los hombros, las rodillas flexionadas y el tronco inclinado hacia delante. Junta las manos delante del cuerpo. Levanta y apoya los pies alternativamente con pasos cortos y rápidos, como si corrieras en el sitio. Mantén el peso sobre la parte delantera de los pies y los movimientos ligeros. Mantén la duración indicada.', instruction_steps_es = '["Ponte de pie con los pies algo más separados que los hombros, las rodillas flexionadas y el tronco inclinado hacia delante.", "Junta las manos delante del cuerpo.", "Levanta y apoya los pies alternativamente con pasos cortos y rápidos, como si corrieras en el sitio.", "Mantén el peso sobre la parte delantera de los pies y los movimientos ligeros.", "Mantén la duración indicada."]'::jsonb, impact_level = 'MODERADO'
   WHERE exercise_id = 1117 AND name_en = 'quick feet v. 2' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3f4b143687c7119b382d996963d79c2b' AND impact_level = 'BAJO';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1117 AND name_en = 'quick feet v. 2' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5ca45f1513ee886f34a25f1ed375e11a' AND impact_level = 'MODERADO') THEN
    RAISE EXCEPTION 'V52: ejercicio 1117 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1120 push-up on lower arms: instructions_es: R2 L01 (ALTA): El texto pide empezar con los antebrazos en el suelo y bajar el pecho desde ahí; el GIF empieza en plancha alta sobre las manos y baja hasta apoyar los antebraz / V3 V07 ACEPTAR: f1 plancha alta sobre las manos; f7 antebrazos apoyados en el suelo con codos junto al cuerpo; f12 vuelve a plancha alta. El texto actual empieza en antebrazos
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate en plancha alta en el suelo con las manos bajo los hombros y el cuerpo en línea recta. Flexiona los codos y baja el cuerpo hasta apoyar los antebrazos en el suelo, con los codos cerca del cuerpo. Empuja con las manos para extender los brazos y volver a la plancha alta. Mantén el core activado sin hundir la cadera. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate en plancha alta en el suelo con las manos bajo los hombros y el cuerpo en línea recta.", "Flexiona los codos y baja el cuerpo hasta apoyar los antebrazos en el suelo, con los codos cerca del cuerpo.", "Empuja con las manos para extender los brazos y volver a la plancha alta.", "Mantén el core activado sin hundir la cadera.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1120 AND name_en = 'push-up on lower arms' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '77daff22051923ca0c709efe7ce8f978';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1120 AND name_en = 'push-up on lower arms' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ff7397dfde5cb5b4b26f962fcbde5b5c') THEN
    RAISE EXCEPTION 'V52: ejercicio 1120 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1121 push-up close-grip off dumbbell: instructions_es: R2 L02 (ALTA): El texto dice sostener una mancuerna en cada mano apoyadas en el suelo; el GIF muestra ambas manos juntas sobre una sola mancuerna colocada de pie / V3 V07 ACEPTAR: f1-f12 una sola mancuerna de pie sobre un disco y las dos manos juntas sobre el disco superior; f7 pecho hacia la mancuerna; no hay dos mancuernas
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca una mancuerna de pie en el suelo, sobre uno de sus discos. Colócate en plancha con los pies en el suelo y las dos manos juntas sobre el disco superior de la mancuerna. Baja el pecho hacia la mancuerna flexionando los codos y manteniéndolos cerca del cuerpo. Empuja hasta extender los brazos sin dejar caer la cadera. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca una mancuerna de pie en el suelo, sobre uno de sus discos.", "Colócate en plancha con los pies en el suelo y las dos manos juntas sobre el disco superior de la mancuerna.", "Baja el pecho hacia la mancuerna flexionando los codos y manteniéndolos cerca del cuerpo.", "Empuja hasta extender los brazos sin dejar caer la cadera.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1121 AND name_en = 'push-up close-grip off dumbbell' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '7eee9f8d7d1edac4668ffcfaa2d79b8a';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1121 AND name_en = 'push-up close-grip off dumbbell' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5f51fc180f61a2e0c104ccceec7118da') THEN
    RAISE EXCEPTION 'V52: ejercicio 1121 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1122 push-up (on stability ball): difficulty_level: R2 L03 (ALTA): estabilidad de hombro sobre apoyo inestable / V3 V07 ACEPTAR: f1-f12 manos sobre balón de estabilidad, pies en el suelo, flexión hasta f7: apoyo inestable, no es postura estable de nivel 1 (nivel 2)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 1122 AND name_en = 'push-up (on stability ball)' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1122 AND name_en = 'push-up (on stability ball)' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1122 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1123 push-up (on stability ball): difficulty_level: R2 L04 (MEDIA): equilibrio sobre balón; fuerza relativa moderada / V3 V07 ACEPTAR: f1-f12 pies sobre el balón y manos en el suelo, flexión completa (f7 pecho cerca del suelo): fuerza relativa moderada y equilibrio, nivel 2
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 1123 AND name_en = 'push-up (on stability ball)' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1123 AND name_en = 'push-up (on stability ball)' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1123 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1125 pull-in (on stability ball): difficulty_level: R2 L06 (ALTA): estabilidad sobre superficie inestable / V3 V07 ACEPTAR: f1 plancha con pies sobre el balón; f7 rodillas al pecho rodando el balón; f12 vuelve: equilibrio sobre superficie inestable, nivel 2
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 1125 AND name_en = 'pull-in (on stability ball)' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1125 AND name_en = 'pull-in (on stability ball)' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1125 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1126 push-up medicine ball: instructions_es: R2 L07 (ALTA): El texto coloca ambas manos sobre el balón; el GIF muestra una sola mano sobre el balón y la otra en el suelo / V3 V07 ACEPTAR: f1-f12 una mano sobre el balón medicinal y la otra en el suelo, ambas más abiertas que los hombros; f7 pecho cerca del suelo. El texto actual pone ambas manos s | difficulty_level: R2 L07 (ALTA): apoyo inestable asimétrico / V3 V07 ACEPTAR: f1-f12 flexión completa con una mano sobre balón medicinal: apoyo inestable y asimétrico con fuerza relativa moderada, nivel 2
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate en plancha alta con una mano sobre un balón medicinal y la otra en el suelo, algo más separadas que los hombros. Activa el core y mantén el cuerpo en línea recta. Flexiona los codos y baja el pecho hacia el suelo. Empuja con las palmas hasta extender los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate en plancha alta con una mano sobre un balón medicinal y la otra en el suelo, algo más separadas que los hombros.", "Activa el core y mantén el cuerpo en línea recta.", "Flexiona los codos y baja el pecho hacia el suelo.", "Empuja con las palmas hasta extender los brazos.", "Repite el número de repeticiones indicado."]'::jsonb, difficulty_level = 2
   WHERE exercise_id = 1126 AND name_en = 'push-up medicine ball' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '4120e45a8f15780f62750cc830acc0a7' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1126 AND name_en = 'push-up medicine ball' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '589dfc375b2002ccc3aeb94f6cca5be7' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1126 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1133 run: instructions_es: R2 L09 (MEDIA): Dice levantar las rodillas hacia el pecho, pero el GIF muestra una carrera en la que el talón de la pierna libre sube hacia atrás, sin elevación de rodillas / V3 V07 ACEPTAR: f1-f12 carrera con talón de la pierna libre hacia atrás, apoyo en la parte delantera del pie, codos flexionados y braceo; ninguna elevación de rodillas al pecho
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie erguido con los codos flexionados junto al cuerpo. Trota en el sitio alternando las piernas: apoya la parte delantera de un pie mientras el talón de la otra pierna sube hacia atrás. Acompaña con un braceo coordinado y mantén el tronco erguido y relajado. Aterriza con suavidad y mantén un ritmo constante, respirando de forma regular. Mantén la duración indicada.', instruction_steps_es = '["Ponte de pie erguido con los codos flexionados junto al cuerpo.", "Trota en el sitio alternando las piernas: apoya la parte delantera de un pie mientras el talón de la otra pierna sube hacia atrás.", "Acompaña con un braceo coordinado y mantén el tronco erguido y relajado.", "Aterriza con suavidad y mantén un ritmo constante, respirando de forma regular.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 1133 AND name_en = 'run' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ba89c7134adb2a3c834a6b654e38d24e';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1133 AND name_en = 'run' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '56c0b595c11d3b510653774b4423601a') THEN
    RAISE EXCEPTION 'V52: ejercicio 1133 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1134 run (equipment): name_es: R2 L10 (MEDIA): Ningún fotograma muestra los dos pies en el aire (el GIF funde dos poses de apoyo), pero el ritmo, la postura y el braceo son de carrera: impacto MODERADO con d / V3 V07 ACEPTAR: f1-f12 carrera sobre una cinta motorizada; «con equipamiento» no dice cuál y el propuesto describe el GIF | instructions_es: R2 L10 (MEDIA): El texto pide trotar en el sitio llevando las rodillas hacia el pecho; el GIF muestra una carrera sobre cinta con braceo y el talón llevado hacia atrás, sin ele / V3 V07 ACEPTAR: f1-f12 carrera sobre cinta con braceo, codos flexionados y talón hacia atrás; no hay trote en el sitio ni rodillas al pecho. Las indicaciones de arranque y para
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Correr en cinta', instructions_es = 'Colócate sobre la cinta de correr y arranca a una velocidad baja. Empieza a correr con pasos cortos, apoyando suavemente los pies bajo la cadera. Mantén el tronco erguido, el core activo y un braceo relajado con los codos flexionados. Ajusta la velocidad a un ritmo de carrera cómodo y reduce la velocidad de forma gradual antes de bajar. Mantén la duración indicada.', instruction_steps_es = '["Colócate sobre la cinta de correr y arranca a una velocidad baja.", "Empieza a correr con pasos cortos, apoyando suavemente los pies bajo la cadera.", "Mantén el tronco erguido, el core activo y un braceo relajado con los codos flexionados.", "Ajusta la velocidad a un ritmo de carrera cómodo y reduce la velocidad de forma gradual antes de bajar.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 1134 AND name_en = 'run (equipment)' AND name_es = 'Correr con equipamiento' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ba89c7134adb2a3c834a6b654e38d24e';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1134 AND name_en = 'run (equipment)' AND name_es = 'Correr en cinta' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '0c20f55d5181146fb3ad28353586190c') THEN
    RAISE EXCEPTION 'V52: ejercicio 1134 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1138 scapula push-up: instructions_es: R2 L14 (ALTA): El texto pide bajar el pecho con los codos cerca del cuerpo (como una flexión); en el GIF los codos permanecen estirados y solo se mueven los omóplatos / V3 W06 ACEPTAR: f1 plancha alta con brazos extendidos; f7 pecho algo más bajo con los omóplatos juntos y los codos igual de extendidos; en ningún fotograma se flexionan los cod
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate en plancha alta en el suelo con las manos bajo los hombros y el cuerpo en línea recta. Con los brazos estirados, deja que el pecho baje un poco juntando los omóplatos. Empuja el suelo para separar los omóplatos y volver a la posición inicial, sin flexionar los codos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate en plancha alta en el suelo con las manos bajo los hombros y el cuerpo en línea recta.", "Con los brazos estirados, deja que el pecho baje un poco juntando los omóplatos.", "Empuja el suelo para separar los omóplatos y volver a la posición inicial, sin flexionar los codos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1138 AND name_en = 'scapula push-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '7ff2a1723ef870a0b91a859ada64623c';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1138 AND name_en = 'scapula push-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '9662a97f8675933ca9a3097e57d43b6f') THEN
    RAISE EXCEPTION 'V52: ejercicio 1138 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1140 reverse plank with leg lift: instructions_es: R2 L16 (ALTA): El texto indica piernas extendidas en plancha inversa; el GIF muestra una mesa invertida con las rodillas flexionadas y los pies apoyados, desde la que se eleva / V3 W06 ACEPTAR: f1 mesa invertida con rodillas flexionadas, pies apoyados, manos detrás y cadera elevada; f7 una pierna extendida hacia arriba, f13 de vuelta, f19 la otra piern | default_prescription: R2 L16 (ALTA): Nombre: el GIF muestra mesa invertida con rodillas flexionadas, no plancha inversa con piernas rectas (posible nombre: Mesa invertida con elevación de pierna).  / V3 W06 ACEPTAR: f1-f24 elevaciones alternas repetidas de una pierna (f7 una, f19 la otra) desde la mesa invertida: movimiento dinámico por repeticiones, no sostener una posició
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en el suelo con las rodillas flexionadas, los pies apoyados y las manos en el suelo detrás de ti. Empuja con manos y pies y eleva la cadera hasta que el tronco y los muslos queden alineados. Activa el core y eleva una pierna extendida hacia arriba. Bájala con control sin dejar caer la cadera. Repite con la otra pierna. Alterna los lados. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en el suelo con las rodillas flexionadas, los pies apoyados y las manos en el suelo detrás de ti.", "Empuja con manos y pies y eleva la cadera hasta que el tronco y los muslos queden alineados.", "Activa el core y eleva una pierna extendida hacia arriba.", "Bájala con control sin dejar caer la cadera.", "Repite con la otra pierna. Alterna los lados.", "Repite el número de repeticiones indicado."]'::jsonb, default_prescription = 'SETS_REPS'
   WHERE exercise_id = 1140 AND name_en = 'reverse plank with leg lift' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '54646ad079d3d400639e2ff38c435f85' AND default_prescription = 'DURATION';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1140 AND name_en = 'reverse plank with leg lift' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '041dfaab78022428b68eb342e09df28e' AND default_prescription = 'SETS_REPS') THEN
    RAISE EXCEPTION 'V52: ejercicio 1140 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1141 russian twist: instructions_es: R2 L17 (ALTA): El texto pide levantar los pies del suelo y equilibrarse sobre los isquiones; en el GIF los talones permanecen apoyados en el suelo durante todo el movimiento / V3 W06 ACEPTAR: f1, f7, f13 y f19 talones apoyados en el suelo durante todo el giro, manos juntas delante del pecho; el texto actual pide levantar los pies y equilibrarse sobre
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en el suelo con las rodillas flexionadas y los talones apoyados. Inclina el tronco hacia atrás manteniendo la espalda recta y el core activado. Junta las manos frente al pecho. Gira el tronco hacia un lado llevando las manos hacia ese costado y después gira hacia el otro lado. Alterna los lados. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en el suelo con las rodillas flexionadas y los talones apoyados.", "Inclina el tronco hacia atrás manteniendo la espalda recta y el core activado.", "Junta las manos frente al pecho.", "Gira el tronco hacia un lado llevando las manos hacia ese costado y después gira hacia el otro lado.", "Alterna los lados.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1141 AND name_en = 'russian twist' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'f65767f33602c81c1cc66fefece2008c';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1141 AND name_en = 'russian twist' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '85941c10a0a5f278d1add9ee83a8ea55') THEN
    RAISE EXCEPTION 'V52: ejercicio 1141 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1142 seated leg raise: instructions_es: R2 L18 (ALTA): El texto dice elevar las piernas rectas hasta paralelas al suelo desde los pies apoyados; el GIF muestra un encogimiento: las rodillas se flexionan hacia el pec / V3 W06 ACEPTAR: f1 sentado en el extremo de un banco plano con manos junto a la cadera, tronco algo inclinado atrás y piernas extendidas por delante con los pies por encima del
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en el borde de un banco plano y apoya las manos a los lados, junto a la cadera. Inclina el tronco ligeramente hacia atrás y extiende las piernas por delante, con los pies sin apoyar. Flexiona las rodillas y llévalas hacia el pecho mientras acercas el tronco. Vuelve a extender las piernas con control sin apoyar los pies. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en el borde de un banco plano y apoya las manos a los lados, junto a la cadera.", "Inclina el tronco ligeramente hacia atrás y extiende las piernas por delante, con los pies sin apoyar.", "Flexiona las rodillas y llévalas hacia el pecho mientras acercas el tronco.", "Vuelve a extender las piernas con control sin apoyar los pies.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1142 AND name_en = 'seated leg raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b161a5df6f44f85e07652c2cb51709ad';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1142 AND name_en = 'seated leg raise' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b2f235f4f48994e8280ced1fcdf250c7') THEN
    RAISE EXCEPTION 'V52: ejercicio 1142 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1146 scapula dips: movement_pattern: R2 L02 (MEDIA): El movimiento es de elevación y depresión escapular con los codos rectos, no un empuje horizontal; la clase más cercana es SCAPULAR_ELEVATION (u OTHER). La cade / V3 V07 ACEPTAR: f1-f12 sentado en el borde del banco con las manos apoyadas y los codos rectos; solo suben y bajan los hombros (elevación y depresión escapular); no hay empuje 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET movement_pattern = 'SCAPULAR_ELEVATION'
   WHERE exercise_id = 1146 AND name_en = 'scapula dips' AND movement_pattern = 'HORIZONTAL_PUSH';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1146 AND name_en = 'scapula dips' AND movement_pattern = 'SCAPULAR_ELEVATION') THEN
    RAISE EXCEPTION 'V52: ejercicio 1146 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1147 scapular pull-up: difficulty_level: R2 L03 (ALTA): suspensión del peso corporal / V3 V07 ACEPTAR: f1-f12 colgado de la barra con las rodillas flexionadas y los pies sin apoyo: soporta el peso corporal en suspensión, fuera del perfil 1 (nivel 2)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 1147 AND name_en = 'scapular pull-up' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1147 AND name_en = 'scapular pull-up' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1147 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1151 seated glute stretch: instructions_es: R2 L07 (ALTA): El texto parte de piernas extendidas y pide presionar la rodilla con la mano, además da una dosis concreta (30 segundos a 1 minuto); el GIF muestra la pierna de / V3 V07 ACEPTAR: f1 sentado con la pierna de apoyo flexionada y el pie en el suelo, tobillo contrario cruzado sobre esa rodilla y mano detrás; f7 tronco inclinado hacia la piern
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en el suelo con una rodilla flexionada y el pie apoyado. Cruza el tobillo de la otra pierna sobre esa rodilla. Apoya una mano en el suelo detrás de ti y mantén la espalda recta. Inclina el tronco hacia delante acercando el pecho a la pierna cruzada hasta notar el estiramiento en el glúteo. Vuelve a erguirte y cambia de lado al terminar. Mantén la duración indicada.', instruction_steps_es = '["Siéntate en el suelo con una rodilla flexionada y el pie apoyado.", "Cruza el tobillo de la otra pierna sobre esa rodilla.", "Apoya una mano en el suelo detrás de ti y mantén la espalda recta.", "Inclina el tronco hacia delante acercando el pecho a la pierna cruzada hasta notar el estiramiento en el glúteo.", "Vuelve a erguirte y cambia de lado al terminar.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 1151 AND name_en = 'seated glute stretch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '90535e4e82d1499e4ef7d59e02c0dbb4';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1151 AND name_en = 'seated glute stretch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '431b6f4cd0133171ab5ddd5d98ed7a2a') THEN
    RAISE EXCEPTION 'V52: ejercicio 1151 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1158 resistance band leg extension: movement_pattern: R2 L14 (ALTA): Se hace sentado en un banco y el texto lo pide. Extensión de rodilla: la acción principal es de rodilla, como el curl femoral (1009) clasificado KNEE_DOMINANT / V3 W07 ACEPTAR: f1 rodilla flexionada → f6-f7 rodilla extendida contra la banda, cadera inmóvil: extensión de rodilla aislada
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET movement_pattern = 'KNEE_DOMINANT'
   WHERE exercise_id = 1158 AND name_en = 'resistance band leg extension' AND movement_pattern = 'OTHER';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1158 AND name_en = 'resistance band leg extension' AND movement_pattern = 'KNEE_DOMINANT') THEN
    RAISE EXCEPTION 'V52: ejercicio 1158 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1175 short stride run: instructions_es: R2 L01 (MEDIA): El texto pide trotar levantando las rodillas alto; el GIF muestra zancadas cortas con las rodillas poco elevadas. Además termina con duración o distancia en lug / V3 V07 ACEPTAR: f1-f24 pasos cortos y rápidos con rodillas poco elevadas, apoyo en la parte delantera del pie y braceo alterno con codos flexionados; no hay rodillas altas; el 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies separados a la anchura de las caderas y los codos flexionados. Empieza a correr con pasos cortos y rápidos, apoyando la parte delantera del pie y sin elevar mucho las rodillas. Acompaña el ritmo con un braceo alterno. Mantén el tronco erguido y una respiración regular. Mantén la duración indicada.', instruction_steps_es = '["Ponte de pie con los pies separados a la anchura de las caderas y los codos flexionados.", "Empieza a correr con pasos cortos y rápidos, apoyando la parte delantera del pie y sin elevar mucho las rodillas.", "Acompaña el ritmo con un braceo alterno.", "Mantén el tronco erguido y una respiración regular.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 1175 AND name_en = 'short stride run' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '2e9be7d428b051adc690bd920b9b059e';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1175 AND name_en = 'short stride run' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e46ff80251451c317971c9620c94b01a') THEN
    RAISE EXCEPTION 'V52: ejercicio 1175 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1176 skater hops: instructions_es: norma de texto del cierre: la frase final pedia repeticiones («Repite el número de repeticiones deseado.») y la prescripcion vigente es DURATION, que la R2 (animacion completa) no propuso cambiar; solo se sustituye la frase final
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies separados a la altura de los hombros. Dobla ligeramente las rodillas y salta hacia la derecha, aterrizando sobre el pie derecho. Al aterrizar, balancea la pierna izquierda detrás de la pierna derecha y toca el suelo con los dedos del pie izquierdo. Salta inmediatamente hacia la izquierda, aterrizando sobre el pie izquierdo. Al aterrizar, balancea la pierna derecha detrás de la pierna izquierda y toca el suelo con los dedos del pie derecho. Continúa alternando los lados, saltando y tocando el suelo con cada pierna. Mantén la duración indicada.', instruction_steps_es = '["Ponte de pie con los pies separados a la altura de los hombros.", "Dobla ligeramente las rodillas y salta hacia la derecha, aterrizando sobre el pie derecho.", "Al aterrizar, balancea la pierna izquierda detrás de la pierna derecha y toca el suelo con los dedos del pie izquierdo.", "Salta inmediatamente hacia la izquierda, aterrizando sobre el pie izquierdo.", "Al aterrizar, balancea la pierna derecha detrás de la pierna izquierda y toca el suelo con los dedos del pie derecho.", "Continúa alternando los lados, saltando y tocando el suelo con cada pierna.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 1176 AND name_en = 'skater hops' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b77bf73883311c5effaa096fd4b36a5b';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1176 AND name_en = 'skater hops' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '1201673edb71bf57cb814c9b43dfd268') THEN
    RAISE EXCEPTION 'V52: ejercicio 1176 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1178 side wrist pull stretch: instructions_es: R2 L04 (MEDIA): El texto pide extender el brazo hacia el lado; en el GIF el brazo se extiende al frente, por delante del cuerpo, sujetado por la otra mano / V3 V08 ACEPTAR: f5-f9 el brazo se mantiene extendido al frente y cruzado por delante del pecho a la altura del hombro, sujeto por la otra mano; f1 y f12 mismo agarre más bajo; 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies separados a la altura de los hombros. Extiende un brazo al frente a la altura del hombro. Sujeta esa mano con la otra y tira suavemente de ella hacia ti hasta notar el estiramiento en el antebrazo. Mantén la posición sin dolor y cambia de lado al terminar. Mantén la duración indicada.', instruction_steps_es = '["Ponte de pie con los pies separados a la altura de los hombros.", "Extiende un brazo al frente a la altura del hombro.", "Sujeta esa mano con la otra y tira suavemente de ella hacia ti hasta notar el estiramiento en el antebrazo.", "Mantén la posición sin dolor y cambia de lado al terminar.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 1178 AND name_en = 'side wrist pull stretch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '8a63a8e2ea9cffeb9b104192570fc7eb';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1178 AND name_en = 'side wrist pull stretch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ebb1bfa527619aae81f706e7eb644aff') THEN
    RAISE EXCEPTION 'V52: ejercicio 1178 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1180 single arm push-up: instructions_es: R2 L06 (ALTA): El texto describe una flexión a dos manos en la que se levanta un brazo hacia el lado durante la bajada y se alterna; el GIF muestra una flexión completa sobre  / V3 V08 ACEPTAR: f1 abajo y f7 arriba sobre una sola mano; la otra mano queda en la espalda en todos los fotogramas; pies separados; no hay apoyo a dos manos ni alternancia | difficulty_level: R2 L06 (ALTA): fuerza relativa alta / V3 V08 ACEPTAR: f1-f12 flexión completa sobre un solo brazo, fuerza relativa alta (perfil de nivel 3)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate en plancha con una sola mano apoyada en el suelo bajo el pecho y los pies bien separados. Mantén el cuerpo en línea recta y el abdomen activo, sin apoyar la otra mano. Baja el pecho hacia el suelo flexionando el codo. Empuja con la mano hasta extender el brazo. Completa las repeticiones y cambia de brazo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate en plancha con una sola mano apoyada en el suelo bajo el pecho y los pies bien separados.", "Mantén el cuerpo en línea recta y el abdomen activo, sin apoyar la otra mano.", "Baja el pecho hacia el suelo flexionando el codo.", "Empuja con la mano hasta extender el brazo.", "Completa las repeticiones y cambia de brazo.", "Repite el número de repeticiones indicado."]'::jsonb, difficulty_level = 3
   WHERE exercise_id = 1180 AND name_en = 'single arm push-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '79ac9c0d2fb4d1886210bd12ca55f029' AND difficulty_level = 2;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1180 AND name_en = 'single arm push-up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ae89669a43702bc1510c1148648134b7' AND difficulty_level = 3) THEN
    RAISE EXCEPTION 'V52: ejercicio 1180 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1183 side bridge v. 2: instructions_es: R2 L09 (ALTA): Pide apoyar el antebrazo con el codo a 90 grados y mantener la posición por tiempo, pero el GIF muestra el apoyo sobre la mano con el brazo extendido y elevacio / V3 V08 ACEPTAR: f1-f12 apoyo sobre la mano con el brazo extendido, la otra mano en la cadera; f1 cadera baja y f7 cadera alta, subidas y bajadas repetidas, no mantenimiento sob
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate de lado en el suelo con las piernas extendidas, una sobre otra. Apoya la mano de abajo en el suelo, bajo el hombro, con el brazo extendido, y coloca la otra mano en la cadera. Activa el core y eleva la cadera hasta formar una línea recta desde la cabeza hasta los pies. Baja la cadera con control hacia el suelo y vuelve a subirla. Repite del otro lado al terminar. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate de lado en el suelo con las piernas extendidas, una sobre otra.", "Apoya la mano de abajo en el suelo, bajo el hombro, con el brazo extendido, y coloca la otra mano en la cadera.", "Activa el core y eleva la cadera hasta formar una línea recta desde la cabeza hasta los pies.", "Baja la cadera con control hacia el suelo y vuelve a subirla.", "Repite del otro lado al terminar.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1183 AND name_en = 'side bridge v. 2' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '32db7b1f2ac2604895971769996e1e52';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1183 AND name_en = 'side bridge v. 2' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'dd91dd5f9288766d98b7b9674e9f3a12') THEN
    RAISE EXCEPTION 'V52: ejercicio 1183 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1184 side hip (on parallel bars): instructions_es: R2 L10 (MEDIA): El texto dice ponerse entre paralelas, sostenerse con los brazos y elevar las piernas rectas hacia el lado; el GIF muestra una silla de capitán con apoyo en los / V3 V08 MEJORAR: f1 rodillas elevadas al centro; f7 rodillas giradas hacia un lado; f12 vuelta al centro; solo se ve un lado, así que no hay alternancia y la propuesta no termin | equipo: R2 L10 (MEDIA): El aparato es el mismo tipo de estación que en 1303 (silla de capitán), no paralelas. No hay abducción de cadera: es un giro de cadera con trabajo de oblicuos.  / V3 V08 ACEPTAR: f1-f12 estación vertical con respaldo, almohadillas para antebrazos y asas (silla de capitán); no son paralelas ni hay apoyo sobre manos | movement_pattern: R2 L10 (MEDIA): El aparato es el mismo tipo de estación que en 1303 (silla de capitán), no paralelas. No hay abducción de cadera: es un giro de cadera con trabajo de oblicuos.  / V3 V08 ACEPTAR: f1-f7 las dos rodillas juntas giran hacia un lado rotando la cadera; no hay separación de piernas (abducción) | difficulty_level: R2 L10 (MEDIA): apoyo suspendido sobre antebrazos / V3 V08 ACEPTAR: f1-f12 el peso corporal se sostiene en apoyo suspendido sobre antebrazos, lo que excluye el perfil de nivel 1
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate en la silla de capitán con los antebrazos en las almohadillas, las manos en las asas y la espalda contra el respaldo. Deja los pies sin apoyo y eleva los muslos con las rodillas flexionadas. Gira la cadera llevando las rodillas hacia un lado sin balancear el tronco. Vuelve al centro con control. Completa la serie y cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate en la silla de capitán con los antebrazos en las almohadillas, las manos en las asas y la espalda contra el respaldo.", "Deja los pies sin apoyo y eleva los muslos con las rodillas flexionadas.", "Gira la cadera llevando las rodillas hacia un lado sin balancear el tronco.", "Vuelve al centro con control.", "Completa la serie y cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb, movement_pattern = 'TRUNK_ROTATION_LATERAL_FLEXION', difficulty_level = 2
   WHERE exercise_id = 1184 AND name_en = 'side hip (on parallel bars)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '2e2dba39c346ef4df7bac6d4e1c77cc5' AND movement_pattern = 'HIP_ABDUCTION_ADDUCTION' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1184 AND name_en = 'side hip (on parallel bars)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ae02bf48fad06c91b301de2acd484ded' AND movement_pattern = 'TRUNK_ROTATION_LATERAL_FLEXION' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1184 no esta en el estado anterior esperado';
  END IF;
END $v52$;
DO $v52$ BEGIN
  IF (SELECT count(*) FROM equipment_types WHERE code IN ('captains chair')) <> 1 THEN
    RAISE EXCEPTION 'V52: faltan codigos de equipamiento para 1184';
  END IF;
  IF (SELECT coalesce(string_agg(r.group_no || ':' || t.code, ',' ORDER BY r.group_no || ':' || t.code), '') FROM exercise_equipment_requirements r JOIN equipment_types t ON t.equipment_id = r.equipment_id WHERE r.exercise_id = 1184) = '1:captains chair' THEN RETURN; END IF;
  IF (SELECT coalesce(string_agg(r.group_no || ':' || t.code, ',' ORDER BY r.group_no || ':' || t.code), '') FROM exercise_equipment_requirements r JOIN equipment_types t ON t.equipment_id = r.equipment_id WHERE r.exercise_id = 1184) <> '1:parallel bars' THEN RAISE EXCEPTION 'V52: requisitos de 1184 distintos de los esperados'; END IF;
  DELETE FROM exercise_equipment_requirements WHERE exercise_id = 1184;
  INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id) VALUES (1184, 1, (SELECT equipment_id FROM equipment_types WHERE code = 'captains chair'));
  UPDATE exercises SET equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'captains chair') WHERE exercise_id = 1184;
END $v52$;
-- 1185 side-to-side toe touch (male): instructions_es: R2 L11 (MEDIA): Describe brazos en cruz y flexión lateral a cada lado con la otra mano extendida; el GIF muestra brazos por encima de la cabeza, flexión del tronco hacia delant / V3 W07 ACEPTAR: f1 y f24 de pie con los brazos verticales sobre la cabeza; f5-f13 tronco flexionado al frente con las manos hacia los pies; f16-f19 el tronco gira y las manos p
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies separados a la anchura de las caderas y los brazos extendidos por encima de la cabeza. Flexiona el tronco hacia delante desde la cadera y lleva las manos hacia los pies. Sin incorporarte, desplaza las manos hacia un pie y después hacia el otro, girando ligeramente el tronco. Vuelve a erguirte con control hasta la posición inicial con los brazos arriba. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies separados a la anchura de las caderas y los brazos extendidos por encima de la cabeza.", "Flexiona el tronco hacia delante desde la cadera y lleva las manos hacia los pies.", "Sin incorporarte, desplaza las manos hacia un pie y después hacia el otro, girando ligeramente el tronco.", "Vuelve a erguirte con control hasta la posición inicial con los brazos arriba.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1185 AND name_en = 'side-to-side toe touch (male)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'c76f2bbdb247f5d1e726e03787334c73';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1185 AND name_en = 'side-to-side toe touch (male)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd51bb3574e09a28c68cab46bc7f0fc8c') THEN
    RAISE EXCEPTION 'V52: ejercicio 1185 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1186 sit-up v. 2: instructions_es: R2 L12 (ALTA): El texto limita la subida a un ángulo de 45 grados; el GIF muestra la incorporación hasta el tronco casi vertical / V3 W07 ACEPTAR: f1 tumbado con rodillas flexionadas y manos tras la cabeza; f6-f7 tronco incorporado casi vertical, muy por encima de 45°; f12 vuelve al suelo
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en el suelo con las rodillas flexionadas y los pies apoyados. Coloca las manos detrás de la cabeza con los codos hacia fuera, sin tirar del cuello. Activa el abdomen y eleva el tronco hasta quedar sentado con la espalda casi vertical. Baja despacio hasta apoyar de nuevo la espalda. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en el suelo con las rodillas flexionadas y los pies apoyados.", "Coloca las manos detrás de la cabeza con los codos hacia fuera, sin tirar del cuello.", "Activa el abdomen y eleva el tronco hasta quedar sentado con la espalda casi vertical.", "Baja despacio hasta apoyar de nuevo la espalda.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1186 AND name_en = 'sit-up v. 2' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '800e583ab286e5d9c2adeb823a56752a';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1186 AND name_en = 'sit-up v. 2' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'efd4a1d8e2b7676b24b16f2e80ecdad9') THEN
    RAISE EXCEPTION 'V52: ejercicio 1186 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1190 side lying floor stretch: instructions_es: R2 L16 (MEDIA): El texto pide agarrarse a una pared o un mueble y levantar la pierna de abajo, e incluye una dosis de 20-30 segundos; en el GIF no hay ningún objeto, la pierna  / V3 W07 ACEPTAR: f1 de lado en el suelo, brazo de abajo extendido sobre la cabeza; f4-f7 el brazo de arriba pasa sobre la cabeza y la pierna de arriba se flexiona y cruza por de
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate de lado en el suelo con las piernas rectas y el brazo de abajo extendido por encima de la cabeza. Lleva el brazo de arriba por encima de la cabeza hasta juntarlo con el otro. Flexiona la pierna de arriba y crúzala por delante de la de abajo hasta notar el estiramiento en el costado. Mantén la posición respirando con calma y vuelve despacio. Cambia de lado. Mantén la duración indicada.', instruction_steps_es = '["Túmbate de lado en el suelo con las piernas rectas y el brazo de abajo extendido por encima de la cabeza.", "Lleva el brazo de arriba por encima de la cabeza hasta juntarlo con el otro.", "Flexiona la pierna de arriba y crúzala por delante de la de abajo hasta notar el estiramiento en el costado.", "Mantén la posición respirando con calma y vuelve despacio.", "Cambia de lado.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 1190 AND name_en = 'side lying floor stretch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '375e9985de0aafa54cde7f962c4d46f8';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1190 AND name_en = 'side lying floor stretch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '433c9d3adeb2fc196dc775235cadfe41') THEN
    RAISE EXCEPTION 'V52: ejercicio 1190 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1191 side-to-side chin: instructions_es: R2 L17 (MEDIA): El texto empieza pidiendo estar de pie con las rodillas flexionadas y describe una dominada recta con la barbilla sobre la barra; el GIF muestra una dominada qu / V3 W07 ACEPTAR: f1 colgado con brazos extendidos (vista de espalda, sin fase de pie); f5-f8 sube desplazando el cuerpo y la cabeza hacia la mano derecha; f12 vuelve abajo. Solo
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Sujeta la barra de dominadas con las manos algo más separadas que los hombros y cuélgate con los brazos extendidos. Tira del cuerpo hacia arriba desplazándolo hacia una de las manos hasta acercar la cabeza a esa mano por encima de la barra. Baja con control hasta extender los brazos. En la siguiente repetición, sube hacia la otra mano. Alterna los lados. Repite el número de repeticiones indicado.', instruction_steps_es = '["Sujeta la barra de dominadas con las manos algo más separadas que los hombros y cuélgate con los brazos extendidos.", "Tira del cuerpo hacia arriba desplazándolo hacia una de las manos hasta acercar la cabeza a esa mano por encima de la barra.", "Baja con control hasta extender los brazos.", "En la siguiente repetición, sube hacia la otra mano.", "Alterna los lados.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1191 AND name_en = 'side-to-side chin' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5cb9ad4d784ae3cf6042b07237b1574c';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1191 AND name_en = 'side-to-side chin' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '0be3ac1a629603862e4a56e54a07f93c') THEN
    RAISE EXCEPTION 'V52: ejercicio 1191 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1192 skin the cat: instructions_es: R2 L18 (BAJA): El texto describe el giro completo desde una barra, pasando las piernas entre los brazos y volviendo; el GIF muestra anillas y solo la posición final de colgado / V3 W07 ACEPTAR: f1-f12 colgado de dos anillas en un marco, cuerpo por detrás de los brazos con hombros en extensión, tronco casi horizontal boca abajo y piernas colgando sin ap | default_prescription: R2 L18 (BAJA): El GIF no muestra el giro de skin the cat completo, solo el colgado por detrás de los brazos casi estático: encaja mejor como estiramiento mantenido (DURATION). / V3 W07 ACEPTAR: f1-f12 misma posición de colgado por detrás de los brazos con oscilación mínima (cambia sobre todo el sombreado muscular): mantenimiento por tiempo | is_stretch: R2 L18 (BAJA): Colgado de dos anillas con los pies sin apoyo, en posición de colgado invertido por detrás de los brazos (hombros en extensión, tronco casi horizontal boca abaj / V3 W07 ACEPTAR: f1-f12 posición mantenida de hombros en extensión máxima colgado de anillas (colgado alemán), sin repeticiones visibles: estiramiento mantenido
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Cuelga de dos anillas con los pies sin apoyo. Coloca el cuerpo por detrás de los brazos, con los hombros en extensión, el tronco casi horizontal boca abajo y las piernas colgando hacia el suelo. Mantén la posición con los brazos extendidos y el abdomen activado, sin forzar el hombro. Sal de la posición con control. Mantén la duración indicada.', instruction_steps_es = '["Cuelga de dos anillas con los pies sin apoyo.", "Coloca el cuerpo por detrás de los brazos, con los hombros en extensión, el tronco casi horizontal boca abajo y las piernas colgando hacia el suelo.", "Mantén la posición con los brazos extendidos y el abdomen activado, sin forzar el hombro.", "Sal de la posición con control.", "Mantén la duración indicada."]'::jsonb, default_prescription = 'DURATION', is_stretch = TRUE
   WHERE exercise_id = 1192 AND name_en = 'skin the cat' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3e2cacf40574f23e35e0b96ad435ce49' AND default_prescription = 'SETS_REPS' AND is_stretch = FALSE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1192 AND name_en = 'skin the cat' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'cd0cc52dbd7b5873ec2d1269a87586fc' AND default_prescription = 'DURATION' AND is_stretch = TRUE) THEN
    RAISE EXCEPTION 'V52: ejercicio 1192 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1194 seated wide angle pose sequence: instructions_es: R2 L20 (MEDIA): El texto pide apoyar las manos en el suelo detrás del cuerpo e inclinarse hacia delante, y termina con repeticiones; en el GIF las manos empiezan sobre las rodi / V3 W07 ACEPTAR: f1 sentado con piernas muy abiertas y manos sobre las rodillas, sin apoyo detrás; f5-f8 tronco inclinado en diagonal hacia la pierna derecha con las manos hacia | is_stretch: R2 L20 (MEDIA): Sentado en el suelo con las piernas extendidas y muy abiertas, tronco erguido y manos sobre las rodillas → inclina el tronco en diagonal hacia una de las pierna / V3 W07 ACEPTAR: f5-f8 inclinación mantenida del tronco sobre la pierna extendida en piernas abiertas; patrón MOBILITY y DURATION: estiramiento
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en el suelo con las piernas extendidas y abiertas en un ángulo amplio, con la espalda recta y las manos sobre las rodillas. Con la espalda larga, inclina el tronco desde la cadera hacia una de las piernas, deslizando las manos hacia el pie. Detente al notar el estiramiento en la parte posterior de las piernas y respira con calma. Vuelve despacio a la posición erguida. Alterna los lados. Mantén la duración indicada.', instruction_steps_es = '["Siéntate en el suelo con las piernas extendidas y abiertas en un ángulo amplio, con la espalda recta y las manos sobre las rodillas.", "Con la espalda larga, inclina el tronco desde la cadera hacia una de las piernas, deslizando las manos hacia el pie.", "Detente al notar el estiramiento en la parte posterior de las piernas y respira con calma.", "Vuelve despacio a la posición erguida.", "Alterna los lados.", "Mantén la duración indicada."]'::jsonb, is_stretch = TRUE
   WHERE exercise_id = 1194 AND name_en = 'seated wide angle pose sequence' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '50c37ddbf05d9f917baa9d3939aecafd' AND is_stretch = FALSE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1194 AND name_en = 'seated wide angle pose sequence' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '451051651d4b4270dca97ba5961dfad7' AND is_stretch = TRUE) THEN
    RAISE EXCEPTION 'V52: ejercicio 1194 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1195 self assisted inverse leg curl: difficulty_level: R2 L01 (ALTA): fuerza excéntrica alta de isquiotibiales / V3 V08 ACEPTAR: f1 de rodillas con tobillos sujetos; f7 cuerpo casi en el suelo frenando con isquiotibiales (curl nórdico); exige fuerza excéntrica alta, no asequible para prin
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 1195 AND name_en = 'self assisted inverse leg curl' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1195 AND name_en = 'self assisted inverse leg curl' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1195 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1197 self assisted inverse leg curl (on floor): difficulty_level: R2 L03 (ALTA): fuerza excéntrica alta de isquiotibiales / V3 V08 ACEPTAR: f1 de rodillas sobre step con correa; f7 bajada completa hasta apoyar manos (curl nórdico); fuerza excéntrica alta de isquiotibiales
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 1197 AND name_en = 'self assisted inverse leg curl (on floor)' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1197 AND name_en = 'self assisted inverse leg curl (on floor)' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1197 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1201 side plank hip adduction: instructions_es: R2 L07 (ALTA): El texto apoya el antebrazo con el codo bajo el hombro y pide mantener una duración; el GIF muestra el apoyo sobre la mano con el brazo extendido y repeticiones / V3 V08 ACEPTAR: f1-f12 apoyo en la mano con brazo extendido, pie de arriba en el banco; f1 pierna de abajo bajo el banco y cadera baja; f7 cadera alta y pierna de abajo junto a | default_prescription: R2 L07 (ALTA): pies SIN_APOYO_DE_PIES en el sentido de que ningún pie apoya en el suelo: el pie de arriba descansa en el banco. El GIF muestra repeticiones dinámicas, por eso  / V3 V08 ACEPTAR: f1 a f7 y vuelta: subida y bajada repetida de cadera y pierna, movimiento dinámico
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate de lado con la mano de abajo en el suelo bajo el hombro y el brazo extendido, y apoya el pie de arriba sobre un banco. Deja la pierna de abajo por debajo del banco, con la cadera baja. Eleva la cadera y lleva la pierna de abajo hasta el banco junto a la de arriba, manteniendo el cuerpo alineado. Baja la cadera y la pierna con control sin apoyarte del todo. Completa la serie y cambia de lado. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate de lado con la mano de abajo en el suelo bajo el hombro y el brazo extendido, y apoya el pie de arriba sobre un banco.", "Deja la pierna de abajo por debajo del banco, con la cadera baja.", "Eleva la cadera y lleva la pierna de abajo hasta el banco junto a la de arriba, manteniendo el cuerpo alineado.", "Baja la cadera y la pierna con control sin apoyarte del todo.", "Completa la serie y cambia de lado.", "Repite el número de repeticiones indicado."]'::jsonb, default_prescription = 'SETS_REPS'
   WHERE exercise_id = 1201 AND name_en = 'side plank hip adduction' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'c3d12679421432d79a7336092d0c4a73' AND default_prescription = 'DURATION';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1201 AND name_en = 'side plank hip adduction' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'f993dcb59bb856c77ea4e6416e94a854' AND default_prescription = 'SETS_REPS') THEN
    RAISE EXCEPTION 'V52: ejercicio 1201 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1204 single leg squat (pistol) male: name_es: R2 L09 (ALTA): «Masculina» procede de la etiqueta de la ilustración de la fuente y no aporta nada al nombre. / V3 V08 ACEPTAR: f1-f12 sentadilla a una pierna con la otra extendida al frente (pistol); masculina solo procede de la ilustración y puede sugerir que es exclusivo de hombres. C
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Sentadilla a una pierna (pistol)'
   WHERE exercise_id = 1204 AND name_en = 'single leg squat (pistol) male' AND name_es = 'Sentadilla a una pierna (pistol) masculina';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1204 AND name_en = 'single leg squat (pistol) male' AND name_es = 'Sentadilla a una pierna (pistol)') THEN
    RAISE EXCEPTION 'V52: ejercicio 1204 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1205 sissy squat: instructions_es: R2 L10 (ALTA): El texto dice mantener el torso erguido y empujar con los talones; el GIF muestra el tronco inclinado hacia atrás en línea con los muslos y los talones elevados / V3 V08 ACEPTAR: f1 de pie sujeto con una mano al respaldo de un banco; f7 talones elevados, rodillas adelantadas y tronco inclinado atrás en línea con los muslos; el texto actu | difficulty_level: R2 L10 (ALTA): fuerza alta de cuádriceps; equilibrio / V3 V08 ACEPTAR: f7 sentadilla sissy profunda con muslos casi horizontales y talones elevados; exige fuerza relativa de cuádriceps y control, no es de principiante
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie junto a un soporte estable y sujétalo con una mano para mantener el equilibrio. Eleva los talones y lleva las rodillas hacia delante mientras inclinas el tronco hacia atrás, manteniendo una línea recta de las rodillas a la cabeza. Baja hasta donde puedas con control, sin dejar caer la cadera. Vuelve a la posición inicial extendiendo las rodillas. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie junto a un soporte estable y sujétalo con una mano para mantener el equilibrio.", "Eleva los talones y lleva las rodillas hacia delante mientras inclinas el tronco hacia atrás, manteniendo una línea recta de las rodillas a la cabeza.", "Baja hasta donde puedas con control, sin dejar caer la cadera.", "Vuelve a la posición inicial extendiendo las rodillas.", "Repite el número de repeticiones indicado."]'::jsonb, difficulty_level = 2
   WHERE exercise_id = 1205 AND name_en = 'sissy squat' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'c4288a9412a7b050338240354fed1025' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1205 AND name_en = 'sissy squat' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ae968b73541887545b26af219947be6a' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1205 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1229 smith one arm row: instructions_es: R2 L09 (MEDIA): Pide ajustar la barra a la altura de la cintura, pero en el GIF la barra parte de la altura de las rodillas con el brazo extendido y sube hacia la cadera / V3 V08 ACEPTAR: f1 barra a la altura de las rodillas con el brazo extendido; f7 barra junto a la cadera con el codo atrás; la barra no parte de la cintura
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca la barra de la máquina Smith a la altura de las rodillas y ponte de pie frente a ella, con los pies separados a la anchura de los hombros. Flexiona ligeramente las rodillas e inclínate hacia delante desde la cadera, con la espalda recta. Sujeta la barra con una mano, con agarre prono y el brazo extendido. Tira de la barra hacia la cadera llevando el codo atrás y pegado al cuerpo, y bájala con control hasta extender el brazo. Cambia de brazo al terminar la serie. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca la barra de la máquina Smith a la altura de las rodillas y ponte de pie frente a ella, con los pies separados a la anchura de los hombros.", "Flexiona ligeramente las rodillas e inclínate hacia delante desde la cadera, con la espalda recta.", "Sujeta la barra con una mano, con agarre prono y el brazo extendido.", "Tira de la barra hacia la cadera llevando el codo atrás y pegado al cuerpo, y bájala con control hasta extender el brazo.", "Cambia de brazo al terminar la serie.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1229 AND name_en = 'smith one arm row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'c0043505b9809faa5cfa13cab6cb13b9';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1229 AND name_en = 'smith one arm row' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '4a1a6ff228b183031c77947b080f0d84') THEN
    RAISE EXCEPTION 'V52: ejercicio 1229 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1241 smith machine decline close grip bench press: instructions_es: R2 L01 (ALTA): El texto pide los pies firmemente apoyados en el suelo; el GIF muestra las piernas enganchadas en el extremo alto del banco declinado / V3 V08 ACEPTAR: f1-f12 banco declinado con las rodillas flexionadas enganchadas en el extremo alto; los pies nunca apoyan en el suelo; f1 barra en el pecho y f7 brazos extendid
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ajusta el banco de la máquina Smith en posición declinada. Túmbate con la cabeza en el extremo bajo y engancha las piernas en el apoyo del extremo alto. Agarra la barra con un agarre cerrado, a la anchura de los hombros o algo menos, y desbloquéala. Baja la barra despacio hasta el pecho con los codos cerca del cuerpo. Empuja la barra hasta extender los brazos por completo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ajusta el banco de la máquina Smith en posición declinada.", "Túmbate con la cabeza en el extremo bajo y engancha las piernas en el apoyo del extremo alto.", "Agarra la barra con un agarre cerrado, a la anchura de los hombros o algo menos, y desbloquéala.", "Baja la barra despacio hasta el pecho con los codos cerca del cuerpo.", "Empuja la barra hasta extender los brazos por completo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1241 AND name_en = 'smith machine decline close grip bench press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a2c517b5e265aa79378dd351b0970e1a';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1241 AND name_en = 'smith machine decline close grip bench press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '668b64777bff1485583a425788bc720b') THEN
    RAISE EXCEPTION 'V52: ejercicio 1241 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1242 smith machine incline tricep extension: requires_overhead: R2 L02 (MEDIA): Los codos quedan por encima de la cabeza en todo el recorrido y las manos en la parte alta: falta OH / V3 V08 ACEPTAR: f1 codos apuntando arriba junto a la cabeza con la barra detrás; f7 brazos extendidos por encima de la cabeza en banco inclinado | instructions_es: R2 L02 (MEDIA): El texto dice sostener la barra sobre el pecho y bajarla hacia la frente; el GIF muestra la barra bajando por detrás de la cabeza, con los codos hacia arriba, y / V3 V08 ACEPTAR: f1 barra detrás de la cabeza con codos hacia arriba; f7 brazos extendidos por encima de la cabeza; nunca se sostiene sobre el pecho ni baja a la frente
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = TRUE, instructions_es = 'Coloca un banco inclinado dentro de la máquina Smith y siéntate con la espalda apoyada y los pies en el suelo. Sujeta la barra con agarre prono y extiende los brazos por encima de la cabeza. Flexiona los codos y baja la barra por detrás de la cabeza, manteniendo los codos apuntando hacia arriba. Extiende los codos hasta volver a estirar los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca un banco inclinado dentro de la máquina Smith y siéntate con la espalda apoyada y los pies en el suelo.", "Sujeta la barra con agarre prono y extiende los brazos por encima de la cabeza.", "Flexiona los codos y baja la barra por detrás de la cabeza, manteniendo los codos apuntando hacia arriba.", "Extiende los codos hasta volver a estirar los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1242 AND name_en = 'smith machine incline tricep extension' AND requires_overhead = FALSE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'cbbf2d671619e7feeace1afa5b5a6c26';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1242 AND name_en = 'smith machine incline tricep extension' AND requires_overhead = TRUE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd581bc0c50a1a644afb5e15164cb61cf') THEN
    RAISE EXCEPTION 'V52: ejercicio 1242 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1254 standing calves: instructions_es: R2 L04 (MEDIA): El texto pide llevar los dos pies atrás separados al ancho de las caderas; en el GIF solo una pierna va atrás (postura escalonada) y se estira el gemelo de esa  / V3 V08 ACEPTAR: f1 pies juntos cerca de la pared; f7 una pierna atrás con la rodilla estirada y el talón apoyado, rodilla delantera flexionada; el gemelo resaltado es el de la 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate de pie frente a una pared y apoya las manos en ella a la altura de los hombros. Lleva una pierna atrás con la rodilla estirada y el talón apoyado en el suelo. Inclina el cuerpo hacia la pared hasta notar el estiramiento en el gemelo de la pierna de atrás. Vuelve despacio a la posición inicial y cambia de pierna al terminar. Mantén la duración indicada.', instruction_steps_es = '["Colócate de pie frente a una pared y apoya las manos en ella a la altura de los hombros.", "Lleva una pierna atrás con la rodilla estirada y el talón apoyado en el suelo.", "Inclina el cuerpo hacia la pared hasta notar el estiramiento en el gemelo de la pierna de atrás.", "Vuelve despacio a la posición inicial y cambia de pierna al terminar.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 1254 AND name_en = 'standing calves' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'bf334c1b582779e2ad3886a951e31dda';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1254 AND name_en = 'standing calves' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a8955f93f44081cae91ba217ce019d4d') THEN
    RAISE EXCEPTION 'V52: ejercicio 1254 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1260 standing lateral stretch: requires_overhead: R2 L10 (ALTA): Brazo por encima de la cabeza durante todo el GIF: falta OH. / V3 V08 ACEPTAR: f1-f12 un brazo elevado con el codo flexionado y la mano detrás de la cabeza; en f7 el codo queda por encima de la cabeza | instructions_es: R2 L10 (ALTA): El texto dice extender los brazos hacia los lados paralelos al suelo e incluye una dosis concreta (15-30 segundos) y repeticiones; el GIF muestra un brazo por e / V3 V08 MEJORAR: f1 brazo elevado con la mano tras la cabeza y el otro abajo; f7 inclinación hacia el lado del brazo bajo; piernas estiradas en todos los fotogramas (no se ven r
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = TRUE, instructions_es = 'Ponte de pie con los pies ligeramente separados y las piernas estiradas. Lleva un brazo por encima de la cabeza con el codo flexionado y la mano detrás de la cabeza; deja el otro brazo a lo largo del cuerpo. Inclina el tronco hacia el lado del brazo bajo hasta notar el estiramiento en el costado del brazo elevado, sin girar el tronco. Vuelve al centro con control y cambia de lado al terminar. Mantén la duración indicada.', instruction_steps_es = '["Ponte de pie con los pies ligeramente separados y las piernas estiradas.", "Lleva un brazo por encima de la cabeza con el codo flexionado y la mano detrás de la cabeza; deja el otro brazo a lo largo del cuerpo.", "Inclina el tronco hacia el lado del brazo bajo hasta notar el estiramiento en el costado del brazo elevado, sin girar el tronco.", "Vuelve al centro con control y cambia de lado al terminar.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 1260 AND name_en = 'standing lateral stretch' AND requires_overhead = FALSE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd4b6cccde3640c701c1be9133cd5fcf1';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1260 AND name_en = 'standing lateral stretch' AND requires_overhead = TRUE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e522028071b02d6a51bd00a6a9a3503d') THEN
    RAISE EXCEPTION 'V52: ejercicio 1260 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1262 spider crawl push up: difficulty_level: R2 L12 (MEDIA): coordinación; apoyo en tres puntos / V3 W07 ACEPTAR: f1 plancha alta en el suelo; f4-f7 lleva una rodilla al codo quedando sobre tres apoyos y vuelve: peso corporal sobre brazos con coordinación, por encima del pe
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 1262 AND name_en = 'spider crawl push up' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1262 AND name_en = 'spider crawl push up' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1262 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1273 squat jerk: instructions_es: R2 L01 (MEDIA): El texto coloca la barra sobre la parte alta de la espalda y la devuelve a la espalda; el GIF muestra la barra en rack frontal, apoyada delante de los hombros,  / V3 V08 ACEPTAR: f1 barra en rack frontal sobre las clavículas con los codos delante; f7 flexión de rodillas; f13 barra subiendo; f19 recepción en sentadilla profunda con brazos
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca la barra sobre la parte delantera de los hombros, con las manos algo más separadas que los hombros y los codos por delante. Flexiona ligeramente las rodillas y extiende las piernas de forma explosiva para impulsar la barra hacia arriba. Mientras la barra sube, baja rápidamente a una sentadilla profunda y recíbela con los brazos extendidos por encima de la cabeza. Ponte de pie manteniendo la barra arriba con los brazos bloqueados. Baja la barra con control hasta la parte delantera de los hombros. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca la barra sobre la parte delantera de los hombros, con las manos algo más separadas que los hombros y los codos por delante.", "Flexiona ligeramente las rodillas y extiende las piernas de forma explosiva para impulsar la barra hacia arriba.", "Mientras la barra sube, baja rápidamente a una sentadilla profunda y recíbela con los brazos extendidos por encima de la cabeza.", "Ponte de pie manteniendo la barra arriba con los brazos bloqueados.", "Baja la barra con control hasta la parte delantera de los hombros.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1273 AND name_en = 'squat jerk' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '25c4f99e9c29be8dbecb3210c9c9ab01';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1273 AND name_en = 'squat jerk' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'e8a3e5119396f76e73a44674424ca5a0') THEN
    RAISE EXCEPTION 'V52: ejercicio 1273 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1279 smith seated shoulder press: instructions_es: R2 L07 (ALTA): El texto habla de asas y agarraderas que se levantan de los soportes; el GIF muestra una barra de máquina Smith / V3 V08 ACEPTAR: f1 barra de Smith a la altura del mentón con agarre algo más ancho que los hombros, sentado en banco con respaldo; f7 brazos extendidos por encima de la cabeza;
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca un banco con respaldo dentro de la máquina Smith y ajusta la barra a la altura de los hombros. Siéntate con la espalda apoyada en el respaldo y los pies planos en el suelo. Sujeta la barra con agarre prono algo más ancho que los hombros, desbloquéala y colócala a la altura de los hombros. Empuja la barra hacia arriba hasta extender los brazos por encima de la cabeza. Baja la barra con control hasta la altura de los hombros. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca un banco con respaldo dentro de la máquina Smith y ajusta la barra a la altura de los hombros.", "Siéntate con la espalda apoyada en el respaldo y los pies planos en el suelo.", "Sujeta la barra con agarre prono algo más ancho que los hombros, desbloquéala y colócala a la altura de los hombros.", "Empuja la barra hacia arriba hasta extender los brazos por encima de la cabeza.", "Baja la barra con control hasta la altura de los hombros.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1279 AND name_en = 'smith seated shoulder press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'f2f8a8bfb598817e7cbf71c5db48cbcb';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1279 AND name_en = 'smith seated shoulder press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '2aaf0ff211e65eadbf44fdd392bcc745') THEN
    RAISE EXCEPTION 'V52: ejercicio 1279 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1281 smith standing behind head military press: instructions_es: R2 L09 (ALTA): Habla de ajustar la altura del asiento y de levantar la barra del soporte y dar un paso atrás, pero el ejercicio es de pie y en la Smith la barra va guiada y se / V3 V08 ACEPTAR: f1 de pie con la barra de Smith detrás de la cabeza sobre los trapecios; f7 brazos extendidos arriba; no hay asiento ni paso atrás; se conservan core activado y
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Coloca la barra de la máquina Smith a la altura de los hombros y ponte de pie bajo ella, con los pies separados a la anchura de los hombros y las rodillas ligeramente flexionadas. Sujeta la barra con agarre prono, algo más abierto que los hombros, con la barra por detrás de la cabeza a la altura de los trapecios, y desengánchala de los ganchos. Con el core activado y el pecho elevado, empuja la barra hacia arriba hasta extender los brazos por encima de la cabeza. Baja la barra con control por detrás de la cabeza hasta la altura de los trapecios. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca la barra de la máquina Smith a la altura de los hombros y ponte de pie bajo ella, con los pies separados a la anchura de los hombros y las rodillas ligeramente flexionadas.", "Sujeta la barra con agarre prono, algo más abierto que los hombros, con la barra por detrás de la cabeza a la altura de los trapecios, y desengánchala de los ganchos.", "Con el core activado y el pecho elevado, empuja la barra hacia arriba hasta extender los brazos por encima de la cabeza.", "Baja la barra con control por detrás de la cabeza hasta la altura de los trapecios.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1281 AND name_en = 'smith standing behind head military press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3ea18d08eebca44c841771ec3042f254';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1281 AND name_en = 'smith standing behind head military press' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '80d1071a3113783f8045aab4335fb95e') THEN
    RAISE EXCEPTION 'V52: ejercicio 1281 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1293 star jump (male): instructions_es: norma de texto del cierre: la frase final pedia repeticiones («Repite el número de repeticiones deseado.») y la prescripcion vigente es DURATION, que la R2 (animacion completa) no propuso cambiar; solo se sustituye la frase final
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies separados a la altura de los hombros y los brazos a los costados. Flexiona ligeramente las rodillas y salta hacia arriba de forma explosiva. Mientras saltas, separa las piernas y extiende los brazos hacia los lados, formando una figura de estrella con tu cuerpo. Aterriza suavemente sobre la punta de los pies con las rodillas ligeramente flexionadas. Mantén la duración indicada.', instruction_steps_es = '["Ponte de pie con los pies separados a la altura de los hombros y los brazos a los costados.", "Flexiona ligeramente las rodillas y salta hacia arriba de forma explosiva.", "Mientras saltas, separa las piernas y extiende los brazos hacia los lados, formando una figura de estrella con tu cuerpo.", "Aterriza suavemente sobre la punta de los pies con las rodillas ligeramente flexionadas.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 1293 AND name_en = 'star jump (male)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '1d17d753f6ea859b177485f878ee2eb3';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1293 AND name_en = 'star jump (male)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5f871a5657f489a73ae0c4bb8212e298') THEN
    RAISE EXCEPTION 'V52: ejercicio 1293 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1295 walking high knees lunge: impact_level: R2 L02 (ALTA): pies=ALTERNOS_SIN_VUELO; De pie, eleva una rodilla a la altura de la cadera y da un paso adelante bajando a una zancada con la rodilla trasera cerca del suelo;  / V3 X01 ACEPTAR: f1 y f13 apoyo sobre una pierna con la otra rodilla elevada a la altura de la cadera; f7 y f19 zancada con ambos pies en el suelo y rodilla trasera cerca del su / coordinador: zancada caminando con rodilla alta: siempre un pie en el suelo (R2 y V3 ALTA)
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET impact_level = 'BAJO'
   WHERE exercise_id = 1295 AND name_en = 'walking high knees lunge' AND impact_level = 'MODERADO';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1295 AND name_en = 'walking high knees lunge' AND impact_level = 'BAJO') THEN
    RAISE EXCEPTION 'V52: ejercicio 1295 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1297 suspended push-up: difficulty_level: R2 L04 (MEDIA): estabilidad en apoyo inestable / V3 V08 ACEPTAR: f1-f12 flexión completa con las manos en asas de suspensión cerca del suelo y el cuerpo casi horizontal: apoyo inestable con peso corporal casi total, por encim
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 1297 AND name_en = 'suspended push-up' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1297 AND name_en = 'suspended push-up' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1297 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1298 straddle maltese: instructions_es: R2 L05 (BAJA): El texto describe sentarse con las piernas abiertas, despegar el cuerpo y bajar por repeticiones; el GIF muestra una posición mantenida boca abajo, sin fase sen / V3 V08 ACEPTAR: f1-f12 posición mantenida boca abajo con brazos extendidos, manos a la altura de la cadera y piernas abiertas sin apoyo; no hay fase sentada ni subidas y bajada | default_prescription: R2 L05 (BAJA): Es una postura estática: la prescripción debería ser DURATION. No se distingue con seguridad si los muslos o los pies tocan el suelo / V3 V08 ACEPTAR: f1-f12 la pose no cambia, solo el resaltado muscular: sostenimiento isométrico
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate boca abajo con las manos apoyadas en el suelo a la altura de la cadera y los brazos extendidos. Abre las piernas y mantenlas sin apoyar en el suelo. Inclina el tronco hacia delante y empuja el suelo para sostener el cuerpo horizontal, con el abdomen y los hombros firmes. Mantén la posición sin dejar caer la cadera y apóyate al terminar. Mantén la duración indicada.', instruction_steps_es = '["Colócate boca abajo con las manos apoyadas en el suelo a la altura de la cadera y los brazos extendidos.", "Abre las piernas y mantenlas sin apoyar en el suelo.", "Inclina el tronco hacia delante y empuja el suelo para sostener el cuerpo horizontal, con el abdomen y los hombros firmes.", "Mantén la posición sin dejar caer la cadera y apóyate al terminar.", "Mantén la duración indicada."]'::jsonb, default_prescription = 'DURATION'
   WHERE exercise_id = 1298 AND name_en = 'straddle maltese' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '76f78c96931a326f95abddf3ccfb611f' AND default_prescription = 'SETS_REPS';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1298 AND name_en = 'straddle maltese' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5fa9e9965293b7684aa625b44e172684' AND default_prescription = 'DURATION') THEN
    RAISE EXCEPTION 'V52: ejercicio 1298 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1299 straddle planche: instructions_es: R2 L06 (ALTA): El texto describe una flexión de brazos con los pies en el suelo y repeticiones; el GIF muestra una plancha de gimnasia estática, con los pies en el aire y el c / V3 V08 ACEPTAR: f1-f12 plancha de gimnasia estática con los hombros por delante de las manos y los pies en el aire; no hay flexión de codos ni pies en el suelo como dice el tex | default_prescription: R2 L06 (ALTA): Posición isométrica sin movimiento en ningún fotograma: la prescripción debería ser DURATION. El cuerpo se sostiene sobre los brazos extendidos sin apoyo de pie / V3 V08 ACEPTAR: f1-f12 sin movimiento, solo cambia el resaltado muscular: sostenimiento isométrico
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Apoya las manos en el suelo a la anchura de los hombros, con los brazos extendidos. Inclina los hombros por delante de las manos y separa bien las piernas. Eleva los pies del suelo hasta que el cuerpo quede horizontal, sostenido solo por las manos. Mantén el abdomen y los glúteos activos y los brazos estirados. Mantén la duración indicada.', instruction_steps_es = '["Apoya las manos en el suelo a la anchura de los hombros, con los brazos extendidos.", "Inclina los hombros por delante de las manos y separa bien las piernas.", "Eleva los pies del suelo hasta que el cuerpo quede horizontal, sostenido solo por las manos.", "Mantén el abdomen y los glúteos activos y los brazos estirados.", "Mantén la duración indicada."]'::jsonb, default_prescription = 'DURATION'
   WHERE exercise_id = 1299 AND name_en = 'straddle planche' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'd4a0a89846dda37367549af14254d23d' AND default_prescription = 'SETS_REPS';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1299 AND name_en = 'straddle planche' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'a8521513054bda0a3e89d08fe99791fd' AND default_prescription = 'DURATION') THEN
    RAISE EXCEPTION 'V52: ejercicio 1299 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1301 suspended reverse crunch: difficulty_level: R2 L08 (ALTA): estabilidad de core con pies suspendidos / V3 V08 ACEPTAR: f1-f12 plancha con los pies suspendidos en correas y recogida de rodillas: exige estabilidad de core en apoyo inestable, por encima del perfil de nivel 1
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 1301 AND name_en = 'suspended reverse crunch' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1301 AND name_en = 'suspended reverse crunch' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1301 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1302 tuck crunch: instructions_es: R2 L09 (ALTA): Dice empezar con los pies apoyados en el suelo y las manos detrás de la cabeza, llevar las rodillas al pecho y luego extender las piernas; el GIF muestra las pi / V3 V08 ACEPTAR: f1-f12 piernas elevadas con caderas y rodillas flexionadas e inmóviles; manos cruzadas sobre el pecho; f7 cabeza y hombros elevados; los pies nunca apoyan ni la
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Túmbate boca arriba en el suelo y eleva las piernas con las caderas y las rodillas flexionadas, sin apoyar los pies. Coloca las manos sobre el pecho. Contrae el abdomen y eleva la cabeza y los hombros hacia las rodillas. Baja con control sin mover las piernas. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en el suelo y eleva las piernas con las caderas y las rodillas flexionadas, sin apoyar los pies.", "Coloca las manos sobre el pecho.", "Contrae el abdomen y eleva la cabeza y los hombros hacia las rodillas.", "Baja con control sin mover las piernas.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1302 AND name_en = 'tuck crunch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '5d32ca5eefda6967192992e54d7a654b';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1302 AND name_en = 'tuck crunch' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b7956623d4b085875c6e63b869728b51') THEN
    RAISE EXCEPTION 'V52: ejercicio 1302 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1303 twisted leg raise: difficulty_level: R2 L10 (MEDIA): apoyo suspendido sobre antebrazos; core fuerte con piernas rectas / V3 V09 ACEPTAR: f1-f18 todo el peso corporal sostenido sobre los antebrazos sin apoyo de pies (no cumple el perfil 1: sin soportar el peso en suspensión); f7 piernas rectas ele
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 1303 AND name_en = 'twisted leg raise' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1303 AND name_en = 'twisted leg raise' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1303 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1304 twisted leg raise (female): difficulty_level: R2 L11 (MEDIA): apoyo suspendido sobre antebrazos; fuerza abdominal / V3 W07 ACEPTAR: f4-f16 cuerpo sostenido sobre los antebrazos sin apoyo de pies mientras eleva las piernas rectas hasta la horizontal y las baja girando: el perfil 1 excluye sop
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 1304 AND name_en = 'twisted leg raise (female)' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1304 AND name_en = 'twisted leg raise (female)' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1304 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1305 v-sit on floor: instructions_es: R2 L12 (ALTA): El texto pide inclinarse hacia atrás, levantar el tronco del suelo y extender los brazos hacia las piernas; el GIF muestra el tronco erguido todo el tiempo con  / V3 W07 MEJORAR: f1 sentado erguido, manos en el suelo junto a la cadera con brazos estirados, piernas en el suelo; f6-f8 piernas rectas casi verticales pegadas al tronco; f12 p | difficulty_level: R2 L12 (ALTA): fuerza de flexores de cadera y abdomen en compresión / V3 W07 ACEPTAR: f6-f8 eleva las piernas rectas hasta casi la vertical sentado erguido con solo las manos de apoyo: compresión que exige fuerza de flexores de cadera y abdomen m
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en el suelo con las piernas extendidas al frente, el tronco erguido y las manos apoyadas en el suelo junto a la cadera, con los brazos estirados. Activa el abdomen y eleva las piernas juntas y rectas hasta casi la vertical, cerca del pecho, con el tronco lo más erguido posible. Baja las piernas con control hasta apoyarlas de nuevo en el suelo. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en el suelo con las piernas extendidas al frente, el tronco erguido y las manos apoyadas en el suelo junto a la cadera, con los brazos estirados.", "Activa el abdomen y eleva las piernas juntas y rectas hasta casi la vertical, cerca del pecho, con el tronco lo más erguido posible.", "Baja las piernas con control hasta apoyarlas de nuevo en el suelo.", "Repite el número de repeticiones indicado."]'::jsonb, difficulty_level = 2
   WHERE exercise_id = 1305 AND name_en = 'v-sit on floor' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '1fcb9bdf0305e753db161db9ac6ded70' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1305 AND name_en = 'v-sit on floor' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b5bd8ed20d272f59c862ded57a9d182f' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1305 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1306 vertical leg raise (on parallel bars): name_es: R2 L13 (ALTA): El GIF muestra una silla romana con respaldo, no paralelas, y las rodillas suben flexionadas (no elevación de piernas rectas). Apoyo suspendido en antebrazos du / V3 W07 ACEPTAR: f1-f12 estación de silla romana o de capitán con respaldo y almohadillas, no paralelas; f5-f8 las rodillas suben flexionadas por encima de la cadera, no piernas | difficulty_level: R2 L13 (ALTA): soporte del peso corporal sobre antebrazos / V3 W07 ACEPTAR: f1-f12 todo el peso corporal sobre los antebrazos sin apoyo de pies: el perfil 1 excluye soportar el peso en suspensión; nivel 2
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Elevación de rodillas en silla romana', difficulty_level = 2
   WHERE exercise_id = 1306 AND name_en = 'vertical leg raise (on parallel bars)' AND name_es = 'Elevación de piernas vertical en paralelas' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1306 AND name_en = 'vertical leg raise (on parallel bars)' AND name_es = 'Elevación de rodillas en silla romana' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1306 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1307 suspended row: difficulty_level: R2 L14 (MEDIA): fuerza relativa moderada / V3 W07 ACEPTAR: f1 cuerpo casi horizontal colgado de anillas con solo los talones en el suelo; f7 pecho a las anillas: remo invertido con gran parte del peso corporal, fuerza r
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 1307 AND name_en = 'suspended row' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1307 AND name_en = 'suspended row' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1307 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1308 two toe touch (male): instructions_es: R2 L15 (MEDIA): El texto parte con los brazos extendidos hacia los lados y pide a la vez rodillas ligeramente flexionadas y piernas rectas; el GIF parte con los brazos por enci / V3 W07 ACEPTAR: f1 y f25-f30 de pie con los brazos verticales sobre la cabeza; f7-f19 tronco flexionado con las piernas estiradas y las manos hacia los pies, con pequeñas oscil
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies juntos o ligeramente separados y extiende los brazos por encima de la cabeza. Flexiona el tronco hacia delante de forma controlada, con las piernas estiradas, hasta llevar las manos hacia la punta de los pies. Mantén la posición un momento sin rebotes bruscos. Vuelve a erguirte despacio hasta llevar de nuevo los brazos por encima de la cabeza. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies juntos o ligeramente separados y extiende los brazos por encima de la cabeza.", "Flexiona el tronco hacia delante de forma controlada, con las piernas estiradas, hasta llevar las manos hacia la punta de los pies.", "Mantén la posición un momento sin rebotes bruscos.", "Vuelve a erguirte despacio hasta llevar de nuevo los brazos por encima de la cabeza.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1308 AND name_en = 'two toe touch (male)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b13b8c592dc595cf87907360093fe81e';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1308 AND name_en = 'two toe touch (male)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'dcef5ee9e9506c35aca2d0b2a4aa27bb') THEN
    RAISE EXCEPTION 'V52: ejercicio 1308 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1311 straight leg outer hip abductor: instructions_es: R2 L18 (BAJA): El texto dice separar una pierna estirada hacia el lado; en el GIF la pierna no se separa: se cruza por detrás de la otra con el tronco inclinado / V3 W07 ACEPTAR: f1 de pie con pies juntos; f5-f8 una pierna se cruza por detrás de la de apoyo con la rodilla algo flexionada y solo la punta del pie en el suelo, el cuerpo se 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los pies juntos y los brazos relajados a los lados. Cruza una pierna por detrás de la otra apoyando solo la punta del pie. Desplaza la cadera hacia un lado e inclina ligeramente el tronco hacia el otro hasta notar tensión en la parte externa de la cadera. Vuelve a la posición inicial con control. Completa las repeticiones y cambia de pierna. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los pies juntos y los brazos relajados a los lados.", "Cruza una pierna por detrás de la otra apoyando solo la punta del pie.", "Desplaza la cadera hacia un lado e inclina ligeramente el tronco hacia el otro hasta notar tensión en la parte externa de la cadera.", "Vuelve a la posición inicial con control.", "Completa las repeticiones y cambia de pierna.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1311 AND name_en = 'straight leg outer hip abductor' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'f9db33efc4d7d3c083ed142baa4c7ed7';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1311 AND name_en = 'straight leg outer hip abductor' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '7f7ed683c0bceace09350fecda31aa69') THEN
    RAISE EXCEPTION 'V52: ejercicio 1311 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1312 suspended split squat: difficulty_level: R2 L19 (ALTA): equilibrio a una pierna con el pie trasero en apoyo inestable / V3 Y01 ACEPTAR: f1-f12 pie trasero en la correa de suspensión y apoyo en una sola pierna sin manos; f7 bajada profunda: equilibrio a una pierna con apoyo inestable, perfil de n
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 1312 AND name_en = 'suspended split squat' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1312 AND name_en = 'suspended split squat' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1312 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1314 twist hip lift: requires_overhead: R2 L01 (MEDIA): El brazo extendido pasa por encima de la cabeza (falta OH). La alternancia de lados no se ve en el GIF / V3 V09 ACEPTAR: f1 brazos cruzados sobre el pecho; f4-f8 un brazo se extiende y queda estirado más allá de la cabeza en la dirección del eje del cuerpo (f7 puño por delante de  | instructions_es: R2 L01 (MEDIA): El texto pide las manos a los lados y girar la parte inferior del cuerpo llevando las rodillas hacia el suelo; el GIF muestra los brazos cruzados sobre el pecho / V3 V09 MEJORAR: El texto actual (manos a los lados, girar la parte inferior llevando las rodillas al suelo, alternar) contradice el GIF: f1 brazos cruzados sobre el pecho; f4-f
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_overhead = TRUE, instructions_es = 'Túmbate boca arriba en el suelo con las rodillas flexionadas, los pies apoyados y los brazos cruzados sobre el pecho. Aprieta los glúteos y eleva la cadera del suelo. Mientras subes, gira el tronco y estira un brazo en diagonal por encima de la cabeza; el otro brazo sigue cruzado sobre el pecho. Baja la cadera y vuelve a cruzar el brazo sobre el pecho. Repite el número de repeticiones indicado.', instruction_steps_es = '["Túmbate boca arriba en el suelo con las rodillas flexionadas, los pies apoyados y los brazos cruzados sobre el pecho.", "Aprieta los glúteos y eleva la cadera del suelo.", "Mientras subes, gira el tronco y estira un brazo en diagonal por encima de la cabeza; el otro brazo sigue cruzado sobre el pecho.", "Baja la cadera y vuelve a cruzar el brazo sobre el pecho.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1314 AND name_en = 'twist hip lift' AND requires_overhead = FALSE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3683ebc2f045e2c10a95f60b2f0c16ba';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1314 AND name_en = 'twist hip lift' AND requires_overhead = TRUE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'f39014607a6f785f23401a7b246c3f4a') THEN
    RAISE EXCEPTION 'V52: ejercicio 1314 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1318 triceps dip (bench leg): instructions_es: R2 L04 (MEDIA): El texto pide caminar con los pies hacia delante (en el suelo); en el GIF los pies están elevados sobre un segundo banco / V3 V09 ACEPTAR: f1-f12 manos en el borde del banco trasero junto a la cadera, piernas estiradas con los talones sobre un segundo banco delante; f7 codos flexionados y cadera po | difficulty_level: R2 L04 (MEDIA): fuerza relativa moderada con pies elevados / V3 V09 ACEPTAR: f1-f12 pies elevados sobre un segundo banco con las piernas rectas y la cadera en el aire entre ambos bancos; f7 descenso profundo con gran parte del peso corpo
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Siéntate en el borde de un banco y agarra el borde con las manos junto a la cadera, con los dedos hacia delante. Apoya los talones en un segundo banco situado delante, con las piernas estiradas, y desliza la cadera fuera del banco. Flexiona los codos y baja la cadera manteniendo la espalda cerca del banco. Empuja con las palmas hasta estirar los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Siéntate en el borde de un banco y agarra el borde con las manos junto a la cadera, con los dedos hacia delante.", "Apoya los talones en un segundo banco situado delante, con las piernas estiradas, y desliza la cadera fuera del banco.", "Flexiona los codos y baja la cadera manteniendo la espalda cerca del banco.", "Empuja con las palmas hasta estirar los brazos.", "Repite el número de repeticiones indicado."]'::jsonb, difficulty_level = 2
   WHERE exercise_id = 1318 AND name_en = 'triceps dip (bench leg)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '49b5daa65ecbd283914fca6cd6a99de4' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1318 AND name_en = 'triceps dip (bench leg)' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b1bd8bafd6f9fe64e8f4a5c27e4f5da7' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1318 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1321 triceps press: name_es: R2 L06 (MEDIA): «Prensa» se asocia a la prensa de piernas; el movimiento es una extensión de codos con el peso corporal, tipo rompecráneos. En f7 los codos quedan a la altura d / V3 V09 ACEPTAR: f1 cuerpo recto inclinado sobre las puntas de los pies con las manos en la barra de una máquina Smith y los brazos extendidos; f4-f7 los codos se flexionan y la | instructions_es: R2 L06 (MEDIA): El texto describe ponerse de pie con los brazos al frente y bajar el cuerpo hacia el suelo, sin barra; el GIF muestra el cuerpo inclinado con las manos en la ba / V3 V09 ACEPTAR: El texto actual describe estar de pie y bajar el cuerpo al suelo sin barra; f1 cuerpo inclinado recto con apoyo en las puntas y manos en la barra Smith a la alt | movement_pattern: R2 L06 (MEDIA): «Prensa» se asocia a la prensa de piernas; el movimiento es una extensión de codos con el peso corporal, tipo rompecráneos. En f7 los codos quedan a la altura d / V3 V09 ACEPTAR: f1-f7 el movimiento principal es la flexión y extensión de codos con los brazos orientados hacia la barra y apenas desplazamiento del hombro; el cuerpo se manti
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Extensión de tríceps con peso corporal en barra Smith', instructions_es = 'Coloca la barra de una máquina Smith a media altura y sujétala con las manos a la anchura de los hombros. Retrocede con los pies hasta que el cuerpo quede inclinado en línea recta, apoyado en las puntas de los pies, con los brazos extendidos. Flexiona los codos y lleva la cabeza hacia la barra, manteniendo el cuerpo recto. Extiende los codos para volver a la posición inicial. Repite el número de repeticiones indicado.', instruction_steps_es = '["Coloca la barra de una máquina Smith a media altura y sujétala con las manos a la anchura de los hombros.", "Retrocede con los pies hasta que el cuerpo quede inclinado en línea recta, apoyado en las puntas de los pies, con los brazos extendidos.", "Flexiona los codos y lleva la cabeza hacia la barra, manteniendo el cuerpo recto.", "Extiende los codos para volver a la posición inicial.", "Repite el número de repeticiones indicado."]'::jsonb, movement_pattern = 'ELBOW_EXTENSION'
   WHERE exercise_id = 1321 AND name_en = 'triceps press' AND name_es = 'Prensa de tríceps' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'c609c5a44682392ebdeab2cbed6337e2' AND movement_pattern = 'HORIZONTAL_PUSH';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1321 AND name_en = 'triceps press' AND name_es = 'Extensión de tríceps con peso corporal en barra Smith' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '78ef7605f3ee286243c5987b234fb519' AND movement_pattern = 'ELBOW_EXTENSION') THEN
    RAISE EXCEPTION 'V52: ejercicio 1321 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1322 triceps stretch: name_es: R2 L07 (ALTA): El brazo cruzado por delante del pecho estira la parte posterior del hombro (deltoides resaltado), no el tríceps / V3 V09 ACEPTAR: f1 de pie con los brazos a los lados; f4-f9 un brazo extendido con el codo recto cruzado por delante del pecho a la altura del hombro y sujetado por el otro; en | zona: R2 L07 (ALTA): El brazo cruzado por delante del pecho estira la parte posterior del hombro (deltoides resaltado), no el tríceps / V3 V09 ACEPTAR: f6-f8 posición de máxima elongación con el brazo recto cruzado por delante del pecho y resaltado rojo en el deltoides; el resaltado del tríceps solo aparece en 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Estiramiento de hombro con brazo cruzado', body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'shoulders'), target_muscle = 'delts'
   WHERE exercise_id = 1322 AND name_en = 'triceps stretch' AND name_es = 'Estiramiento de tríceps' AND body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper arms') AND target_muscle = 'triceps';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1322 AND name_en = 'triceps stretch' AND name_es = 'Estiramiento de hombro con brazo cruzado' AND body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'shoulders') AND target_muscle = 'delts') THEN
    RAISE EXCEPTION 'V52: ejercicio 1322 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1323 twin handle parallel grip lat pulldown: name_es: R2 L08 (MEDIA): El nombre mezcla inglés (lat pulldown) y la expresión dos mecanismos es poco clara. / V3 V09 MEJORAR: f1 sentado en un banco plano entre dos torres, cada mano sujeta un asa de una polea alta distinta (cable izquierdo a la polea izquierda, derecho a la derecha) c
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Jalón en dos poleas altas con agarre paralelo'
   WHERE exercise_id = 1323 AND name_en = 'twin handle parallel grip lat pulldown' AND name_es = 'Lat pulldown con agarre paralelo de dos mecanismos';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1323 AND name_en = 'twin handle parallel grip lat pulldown' AND name_es = 'Jalón en dos poleas altas con agarre paralelo') THEN
    RAISE EXCEPTION 'V52: ejercicio 1323 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1324 stationary bike walk: name_es: R2 L09 (ALTA): «Bicicleta estática caminando» es una traducción literal de bike walk que no se entiende; el GIF muestra un pedaleo sentado. Pedalear sentado en bicicleta estát / V3 V09 MEJORAR: f1-f12 sentado en el sillín de una bicicleta estática con las manos en el manillar, pedaleo continuo alterno; no hay marcha. «Caminando» es una traducción liter
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Pedaleo sentado en bicicleta estática'
   WHERE exercise_id = 1324 AND name_en = 'stationary bike walk' AND name_es = 'Bicicleta estática caminando';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1324 AND name_en = 'stationary bike walk' AND name_es = 'Pedaleo sentado en bicicleta estática') THEN
    RAISE EXCEPTION 'V52: ejercicio 1324 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1333 wheel run: difficulty_level: R2 L11 (MEDIA): pliometría; coordinación / V3 W07 ACEPTAR: f6-f8 y f24-f26 gran zancada con los dos pies en el aire unos 48 px por encima del apoyo de f1/f13: pliometría con coordinación de brazos, impropia del perfil 1 | impact_level: R2 L11 (MEDIA): pies=FASE_DE_VUELO; Carrera con zancadas muy amplias y saltadas: impulso con una pierna llevando la rodilla contraria arriba → gran zancada en el aire con los d / V3 W07 ACEPTAR: Ambos pies en el aire en f6-f8 y f24-f26 (punto más bajo de la figura en y=121-122 frente a y=167-170 en los fotogramas de apoyo f1-f5, f10-f22, f28-f36): salto
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2, impact_level = 'ALTO'
   WHERE exercise_id = 1333 AND name_en = 'wheel run' AND difficulty_level = 1 AND impact_level = 'MODERADO';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1333 AND name_en = 'wheel run' AND difficulty_level = 2 AND impact_level = 'ALTO') THEN
    RAISE EXCEPTION 'V52: ejercicio 1333 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1334 wide hand push up: instructions_es: R2 L12 (ALTA): El texto pide mantener los codos cerca de los costados; el GIF muestra los codos abiertos hacia fuera, propio del agarre ancho / V3 W07 ACEPTAR: f1 plancha alta con las manos bastante más separadas que los hombros; f6-f7 pecho cerca del suelo con los codos abiertos hacia los lados, no pegados al cuerpo; 
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Colócate en plancha alta en el suelo con las manos bastante más separadas que la anchura de los hombros. Mantén el cuerpo en línea recta de la cabeza a los pies. Baja el pecho hacia el suelo flexionando los codos, que se abren hacia los lados. Empuja con las palmas hasta extender los brazos. Repite el número de repeticiones indicado.', instruction_steps_es = '["Colócate en plancha alta en el suelo con las manos bastante más separadas que la anchura de los hombros.", "Mantén el cuerpo en línea recta de la cabeza a los pies.", "Baja el pecho hacia el suelo flexionando los codos, que se abren hacia los lados.", "Empuja con las palmas hasta extender los brazos.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1334 AND name_en = 'wide hand push up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '88fc50273d4e19c61118464a375982ee';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1334 AND name_en = 'wide hand push up' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'fefa076d0cd74f540e7ddcd8b250de4e') THEN
    RAISE EXCEPTION 'V52: ejercicio 1334 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1373 wrist circles: instructions_es: R2 L17 (ALTA): El texto pide extender los brazos rectos al frente y cerrar las manos en puño; el GIF muestra los codos flexionados a 90° junto al cuerpo y las manos abiertas / V3 W07 ACEPTAR: f1-f12 de pie con los codos junto al cuerpo flexionados a unos 90°, antebrazos al frente y manos abiertas describiendo círculos; los brazos nunca están rectos a
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET instructions_es = 'Ponte de pie con los brazos junto al cuerpo y los codos flexionados a 90°, con los antebrazos hacia delante. Relaja las manos y mantén los antebrazos quietos. Gira las muñecas describiendo círculos amplios y controlados. Repite el número de repeticiones indicado.', instruction_steps_es = '["Ponte de pie con los brazos junto al cuerpo y los codos flexionados a 90°, con los antebrazos hacia delante.", "Relaja las manos y mantén los antebrazos quietos.", "Gira las muñecas describiendo círculos amplios y controlados.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 1373 AND name_en = 'wrist circles' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'ed83110fc8408195e8a34c8937a9ca82';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1373 AND name_en = 'wrist circles' AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '6908b790d48ede869a2755e5b3405d07') THEN
    RAISE EXCEPTION 'V52: ejercicio 1373 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1374 wind sprints: difficulty_level: R2 L18 (ALTA): suspensión; fuerza de agarre / V3 W07 ACEPTAR: f1-f12 colgado de la barra con los pies sin apoyo mientras alterna elevaciones de rodilla: suspensión, excluida del perfil 1; nivel 2
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET difficulty_level = 2
   WHERE exercise_id = 1374 AND name_en = 'wind sprints' AND difficulty_level = 1;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1374 AND name_en = 'wind sprints' AND difficulty_level = 2) THEN
    RAISE EXCEPTION 'V52: ejercicio 1374 no esta en el estado anterior esperado';
  END IF;
END $v52$;
-- 1375 world greatest stretch: requires_kneeling: R2 L19 (ALTA): Sobra KN: la rodilla trasera no apoya en ningún fotograma. FL se mantiene por las manos en el suelo en zancada baja, aunque no hay que tumbarse ni sentarse. El  / V3 Y01 ACEPTAR: f1-f18 la rodilla trasera nunca toca el suelo: f1 y f18 rodilla trasera flexionada pero elevada sobre la punta del pie; f7-f13 pierna trasera estirada; solo apo / coordinador: definicion literal demostrada en todos los fotogramas (R2 + V3) | instructions_es: R2 L19 (ALTA): El texto dice bajar la rodilla trasera al suelo y girar el tronco llevando el brazo hacia el techo; en el GIF la rodilla trasera nunca toca el suelo, no hay gir / V3 Y01 MEJORAR: f1 zancada con la rodilla trasera flexionada y elevada (no estirada como dice la propuesta); f4-f7 tronco inclinado con las manos hacia el suelo junto al pie de
DO $v52$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_kneeling = FALSE, instructions_es = 'Colócate en una zancada larga con un pie adelantado y la pierna de atrás apoyada en la punta del pie, sin apoyar la rodilla trasera en el suelo. Inclina el tronco hacia delante y lleva las manos al suelo junto al pie adelantado. Eleva la cadera y llévala hacia atrás estirando la rodilla delantera y la pierna de atrás hasta notar el estiramiento en la parte posterior del muslo. Vuelve a flexionar las rodillas y regresa a la zancada con el tronco erguido. Mantén la duración indicada.', instruction_steps_es = '["Colócate en una zancada larga con un pie adelantado y la pierna de atrás apoyada en la punta del pie, sin apoyar la rodilla trasera en el suelo.", "Inclina el tronco hacia delante y lleva las manos al suelo junto al pie adelantado.", "Eleva la cadera y llévala hacia atrás estirando la rodilla delantera y la pierna de atrás hasta notar el estiramiento en la parte posterior del muslo.", "Vuelve a flexionar las rodillas y regresa a la zancada con el tronco erguido.", "Mantén la duración indicada."]'::jsonb
   WHERE exercise_id = 1375 AND name_en = 'world greatest stretch' AND requires_kneeling = TRUE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = '3abaf0112adbd7ede58016e39887d42b';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1375 AND name_en = 'world greatest stretch' AND requires_kneeling = FALSE AND md5(regexp_replace(instructions_es, '[\t\r\n]+', ' ', 'g')) = 'b75ad0b45026857b46eaacc2ecaab86e') THEN
    RAISE EXCEPTION 'V52: ejercicio 1375 no esta en el estado anterior esperado';
  END IF;
END $v52$;
