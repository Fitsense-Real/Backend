# Proceso de revisión del catálogo (permanente)

Las pruebas automáticas (`CatalogoCalidadIT`) detectan regresiones, pero no sustituyen la revisión semántica y visual. Este proceso es obligatorio para cualquier migración que toque `exercises` o `exercise_equipment_requirements`.

## 1. Antes de escribir la migración

1. Generar las hojas de fotogramas de los ejercicios afectados:
   `python3 -I -B herramientas/hojas.py <catalogo.tsv> <clon de hasaneyldrm/exercises-dataset> <salida> 5 <ids…>`.
   Las hojas no se versionan, porque son derivados de GIF con copyright.
2. Revisar cada GIF con `revision_visual/PROTOCOLO_REVISION_VISUAL.md` y anotar el veredicto en un TSV con el mismo formato que `revision_LNN.tsv`.
3. Si se cambia un texto, redactarlo con `revision_visual/PROTOCOLO_REDACCION.md`: solo lo que muestra el GIF y nada que contradiga las banderas.

## 2. Reglas de la migración

- Cada fila exige el estado anterior exacto (nombre de la fuente y valor previo) y nunca borra filas ni ids.
- Una bandera de seguridad solo se **relaja** con revisión profesional registrada. Se puede **endurecer** con evidencia visual.
- La dificultad se baja solo con aprobación profesional (V27). Una subida preventiva se documenta como pendiente de confirmación.
- Un nombre nuevo no debe cambiar el resultado de las reglas por nombre (`ImpactoPolicy`, «estiramiento») sin justificarlo.

## 3. Al hacer el commit

- `CatalogoCalidadIT` falla si cambia la huella del catálogo. Hay que regenerar `src/test/resources/catalogo/huella_catalogo.tsv` (`-Dfitsense.catalogo.huella.capturar=true`) **en el mismo commit**: su diff es la traza revisable.
- Si la guarda texto/bandera señala un falso positivo, se añade a `excepciones_texto_bandera.tsv` con su justificación. La prueba exige que cada excepción siga haciendo falta.
- Hay que regenerar las referencias de generación afectadas (`GeneracionReferenciaIT`, etc.) y explicar cada diferencia.

## 4. Revisión profesional (clasificaciones ambiguas)

Las decisiones que esta auditoría deja PENDIENTE_PROFESIONAL están listadas en el registro maestro (filtro «Estado final»). Para cada una, el profesional:
1. abre el GIF (`gif_path`) y la ficha;
2. decide y firma en una migración posterior (dos revisores para APPROVED de dificultad, según V27);
3. actualiza el registro maestro (columna «Validación profesional») y la huella.
