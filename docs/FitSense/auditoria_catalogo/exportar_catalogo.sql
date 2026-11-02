-- Exporta el catalogo para auditar_catalogo.py. SOLO LECTURA.
--   psql "$URL" -v ON_ERROR_STOP=1 -At -F $'\t' -f exportar_catalogo.sql > catalogo.tsv
-- Una fila por ejercicio (activos e inactivos). Los saltos de linea del texto se
-- sustituyen por espacios para que cada ejercicio ocupe una linea.
BEGIN TRANSACTION READ ONLY;
SELECT e.exercise_id, e.source_code, e.is_active, e.name_en,
       coalesce(e.name_es, ''), bp.code, e.target_muscle, coalesce(e.synergist_muscle, ''),
       coalesce(array_to_string(e.secondary_muscles, ','), ''), eq.code,
       coalesce((SELECT string_agg(r.group_no || ':' || q.code, ',' ORDER BY r.group_no, q.code)
                 FROM exercise_equipment_requirements r
                 JOIN equipment_types q ON q.equipment_id = r.equipment_id
                 WHERE r.exercise_id = e.exercise_id), ''),
       e.difficulty_level, e.default_prescription, coalesce(e.movement_pattern, ''),
       coalesce(e.is_isolation::text, ''), e.high_impact, e.requires_floor, e.axial_load,
       e.requires_kneeling, e.requires_hanging, e.requires_overhead, e.requires_inversion,
       coalesce(e.gif_path, ''),
       regexp_replace(coalesce(e.instructions_en, ''), '[\t\r\n]+', ' ', 'g'),
       regexp_replace(coalesce(e.instructions_es, ''), '[\t\r\n]+', ' ', 'g')
  FROM exercises e
  JOIN body_parts bp ON bp.body_part_id = e.body_part_id
  JOIN equipment_types eq ON eq.equipment_id = e.equipment_id
 ORDER BY e.exercise_id;
ROLLBACK;
