-- CONTRAFACTUAL (solo base efimera; NO es una correccion): 775 y 973 dejan de acreditar dosis resistida.
-- MOBILITY = clase MOVILIDAD: siguen siendo elegibles como movilidad, pero sus series ya no cuentan como dosis de
-- espalda ni de hombro, y 775 deja de ser candidato de empuje horizontal.
UPDATE exercises SET movement_pattern = 'MOBILITY' WHERE exercise_id IN (775, 973);
