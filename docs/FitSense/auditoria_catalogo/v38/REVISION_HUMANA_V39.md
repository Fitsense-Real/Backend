# Doble revisión humana de V39 (43 ejercicios)

**Estado: PENDIENTE.** Este paquete prepara la revisión; **no la sustituye**.

Los veredictos de `cambios_v38.tsv` vienen de una revisión visual asistida por IA. La doble revisión humana tiene que hacerla el equipo, sin inventar revisores ni veredictos.

## Qué revisar

`revision_humana_v39.tsv`: una fila por ejercicio, ordenada por prioridad.

| Prioridad | Qué | Ejercicios |
|---|---|---|
| **1** | Cambios de movimiento, de dificultad y de equipamiento que se usan en planes de casa | 1102 (patrón), 1106 (dificultad 2 provisional), espaldera 301–308, 1072, 880, 1253 |
| 2 | Grupo y prescripción | 1117, flexiones 1111, 1113, 1115 y 1262, combinados 558 y 567 |
| 3 | Requisitos de implementos de gimnasio | banco Scott (20), 58, 235, 813 |

**Para cada fila:**
1. Abrir el GIF (`gif`) o generar su hoja de fotogramas:
   ```
   python3 docs/FitSense/auditoria_catalogo/revision_visual/hoja_fotogramas.py <id>
   ```
2. Rellenar `revisor_1` y `revisor_2` (nombre), y `veredicto_1` y `veredicto_2`: `CONFIRMA`, `CORRIGE` (explicar en el comentario) o `DUDA`.
3. En `consenso`, anotar `CONFIRMADO`, `CORREGIR` o `SIN_CONSENSO`.

**Dificultad.** La de 1106 (2) es provisional y debe resolverse con el instrumento de dificultad de FitSense y su procedimiento:
- Hoy no se registra ningún evento en `exercise_difficulty_approvals`.
- Cuando haya evaluación, se registra con el contrato de V27, en una migración nueva.

**Si la revisión cambia alguna decisión:**
1. Editar `cambios_v38.tsv`.
2. Regenerar V39 con `generar_v38.py` (o una V40 si V39 ya se hubiera aplicado en algún entorno).
3. Repetir las pruebas afectadas (`CatalogoV39IT`, simulaciones y suite).

## Relación con la integración

La integración técnica no requiere la revisión humana para ser **reversible y trazable**:
- cada fila tiene su estado previo comprobado;
- la evidencia está en este directorio.

Pero los datos de V39 deben considerarse **revisados por IA, pendientes de confirmación humana** hasta que este formulario esté completo. Ver `INFORME_FINAL_V39.md` §8.
