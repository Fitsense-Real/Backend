# Auditoría y corrección integral del catálogo de ejercicios

Versión auditada: **FS-CAT-2026.11-INTEGRAL-R1** (`VERSION_CATALOGO.md`). Base: `generation` @ `623c4fa`. Sin merge, sin despliegue, sin acceso a Railway ni a Neon y sin llamadas a Replicate.

## 1. Qué se hizo

1. **Línea base congelada** del catálogo tras V1–V46 (`INVENTARIO_INICIAL.md`, `linea_base/`).
2. **Revisión de los 1.219 activos contra su GIF original**:
   - Material: los 1.324 GIF y las imágenes de la fuente, clonados del dataset.
   - Hojas: 8 fotogramas más la imagen fija por ejercicio.
   - Organización: 10 lotes de unos 122 ejercicios, cada uno con un revisor de IA y el mismo protocolo (`revision_visual/PROTOCOLO_REVISION_VISUAL.md`).
   - Resultado: un veredicto por ejercicio sobre nombre, instrucciones, equipamiento, clasificación, dificultad, banderas y apoyo suspendido (`revision_visual/revision_LNN.tsv`).
3. **Verificación propia** de todos los cambios de seguridad y de clasificación, de los 26 ejercicios de la PR #55 y de una muestra de textos.
4. **Correcciones en cuatro migraciones**, todas con guardas por estado anterior:
   - V47: banderas que faltaban.
   - V48: dificultad conservadora.
   - V49: nombres e instrucciones.
   - V50: clasificación y equipamiento.
5. **Controles permanentes** (`CatalogoCalidadIT`) y **proceso de revisión** (`PROCESO_REVISION.md`).
6. **Impacto** en elegibilidad, generación, PR #55 y planes publicados, y **regresión completa**.

## 2. Respuestas

**1. ¿Cuántos ejercicios activos tiene FitSense?** **1.219** de 1.376 filas: los 1.324 de la fuente más 52 MVP inactivos. Hay 157 inactivos.

**2. ¿Cuántos se revisaron automáticamente?** Los **1.219**: integridad, valores, huella, guarda texto/bandera, duplicados y comparación con la línea base.

**3. ¿Cuántos se revisaron visualmente?** Los **1.219**, fotograma a fotograma del GIF original, por revisores de IA. No hay revisión humana ni profesional. El coordinador verificó él mismo unos 80 GIF: las 26 banderas de V47, las 12 clasificaciones propuestas, los 26 de la PR #55 y los textos prioritarios.

**4. ¿Cuántos tienen problemas confirmados?** **514** ejercicios activos tienen al menos una discrepancia confirmada (NO) en algún atributo:
- 372 instrucciones;
- 84 nombres (82 corregidos);
- 79 equipamientos (17 imprescindibles);
- 12 clasificaciones;
- 53 dificultades;
- 26 banderas que faltaban.

Las cifras se solapan entre ejercicios. Además hay **73 de gravedad ALTA**.

**5. ¿Cuántos fueron corregidos?** **429** ejercicios tienen al menos una corrección aplicada y probada. 261 quedan como CORREGIDO y el resto sigue pendiente por otro aspecto.

| Migración | Alcance |
|---|---|
| V47 | 26 ejercicios, 28 banderas |
| V48 | 12 ejercicios |
| V49 | 81 nombres y 372 instrucciones |
| V50 | 9 clasificaciones y 17 equipamientos |

**6. ¿Cuántos siguen pendientes?** **390**: 59 visuales, 320 profesionales y 11 técnicos.

**7. ¿Cuántos requieren revisión profesional?** **320** (desglose en `PENDIENTES_Y_LIMITACIONES.md`), más la confirmación de los 16 contenidos.

**8. ¿Cuántos permanecen contenidos por seguridad?** **16**:
- 7 de INC-4C-03;
- 1 de INC-CAT-1374;
- 8 de INC-CAT-C39, que solo están contenidos si se fusiona la PR #55: 1153, 1301, 1303, 1304, 1306, 131, 400 y 448.

**9. ¿Cuántos ejercicios de la PR #55 pueden recuperarse?** **18 de 26 por completo**. En los 26 el GIF y la bandera coinciden y el error era el texto, que V49 corrige. 1301, 1303 y 1304 recuperan además parte de su contención. Quedan **8** (`IMPACTO_PR55.md`).

**10. ¿Se recuperan los dos perfiles NO_FACTIBLE?** **Sí**, siempre que la contención C39 se reduzca a los 8 restantes. Sobre el catálogo corregido, LOSE_WEIGHT / principiante / casa, tanto «sin suelo» como «sin las tres», publican un plan válido: cumplen V25 con el cardio de pie 1093, 853 y 1117. No contienen nada prohibido y no se relaja V25. Con la PR #55 tal cual siguen en NO_FACTIBLE.

**11. ¿Qué cambios hubo en la elegibilidad?** En la rejilla de 168 perfiles:
- **0 desenlaces cambiados** y 0 NO_FACTIBLE nuevos.
- Semanas con un ejercicio incompatible según el catálogo corregido: **29 → 0**.
- Conjunto elegible medio: −5 a −6 ejercicios por perfil; ninguno entra donde antes no estaba.
- Dosis y volumen sin cambio: 55,4 → 55,2 series por semana.
- Atribución en la rejilla:
  - V47–V48 cambian el conjunto elegible de 112 perfiles.
  - V49 no cambia ninguno. Solo reordena el cardio de los 84 perfiles en los que el 1093 es elegible: su impacto por nombre pasa de MODERADO a BAJO.
  - V50 cambia 140.
- Referencias de generación (72 perfiles): V47–V48 cambian 47, V49 ninguna y V50 68, todas VALIDA (`IMPACTO_ELEGIBILIDAD.md`).

**12. ¿Qué impacto hay sobre los planes publicados?**
- V47 actúa mediante el bloqueo de seguridad existente (P2D/SUST-SEG-1): sesiones no iniciadas.
- V49 cambia el texto que se muestra, porque se lee por `exercise_id`.
- V48 y V50 solo afectan a selecciones futuras.
- No se modificó ningún plan real.

La matriz está en `PLANES_PUBLICADOS.md`, con una consulta de exposición de solo lectura.

**13. ¿Qué migraciones se prepararon?**
- `V47__catalogo_integral_banderas_seguridad.sql`
- `V48__catalogo_integral_dificultad_conservadora.sql`
- `V49__catalogo_integral_textos.sql`
- `V50__catalogo_integral_clasificacion_equipamiento.sql`

Las cuatro solo se aplican si el estado anterior es el esperado. No borran filas ni ids. V45 queda reservada por la PR #17, y V47–V50 no coinciden con ninguna rama abierta. **Orden de fusión obligatorio: 1 → 2 → 3 → 4.**

**14. ¿Qué controles automáticos se implementaron?** `CatalogoCalidadIT`, que corre en CI:
1. integridad y valores obligatorios;
2. valores válidos y equipamiento coherente con el disparador de V23;
3. GIF, imagen y atribución;
4. **huella** de clasificación, equipo, banderas y firma de las reglas por nombre: cualquier cambio exige actualizar `huella_catalogo.tsv` en el mismo commit;
5. **guarda texto/bandera**, con excepciones justificadas: detecta 14 incoherencias en la línea base y 0 tras las correcciones;
6. sin nombres duplicados nuevos.

Además, cada migración tiene su IT (`CatalogoIntegral{Seguridad,Textos,Clasificacion}IT`). Hay dos herramientas opt-in, `CatalogoLineaBaseIT` y `CatalogoIntegralComparacionIT`, y los scripts de revisión visual (`herramientas/`).

**15. Resultados completos de regresión:** pila completa con 1.417 unitarias y 698 de integración, **0 fallos** (§3).

**16. ¿Qué PR y commits quedaron preparados?** Cuatro PR apiladas, sin fusionar; la lista está en la descripción de cada PR:

| PR | Rama | Base | Contenido |
|---|---|---|---|
| 1 | `catalogo/integral-1-seguridad` | `generation` | V47, V48 y referencia regenerada |
| 2 | `catalogo/integral-2-textos` | PR 1 | V49 |
| 3 | `catalogo/integral-3-clasificacion` | PR 2 | V50 y expectativas |
| 4 | `audit/correccion-integral-catalogo` | PR 3 | Controles, documentación y Excel |

**17. ¿Qué hacer con la PR #55?** No fusionarla tal cual. Después de fusionar 1 → 4, actualizarla para que contenga solo los 8 restantes, con sus pruebas adaptadas, o resolverla según la decisión profesional sobre apoyo suspendido (`IMPACTO_PR55.md`, `PROPUESTA_APOYO_SUSPENDIDO.md`).

**18. ¿Qué impide declarar la versión completamente auditada?**
- 59 ejercicios sin verificación visual posible.
- 320 decisiones profesionales.
- 11 inconsistencias técnicas.
- 16 contenidos.
- Ninguna validación profesional registrada.

**El catálogo no está completamente saneado.**

## 3. Regresión

Se ejecutó `mvnw clean verify -Pintegration` con PostgreSQL 16 en Testcontainers y sin Replicate. Cada rama se compiló desde limpio.

| Árbol | Unitarias | Integración | Resultado |
|---|---|---|---|
| Rama 1 (V47, V48), primera pasada | 1.417, 0 fallos | 659, 1 fallo (`GeneracionReferenciaIT`) | Referencia regenerada y documentada |
| Rama 2 (V49), primera pasada | 1.417, 0 fallos | 662, 2 fallos (referencia, `CatalogoV39IT`) | V49 corregida; la prueba no se tocó |
| Rama 3 (V50), primera pasada | 1.417, 0 fallos | 690, 5 fallos | Expectativas justificadas en §4 |
| **Pila completa (PR 4, `8a3bd18`), final** | **1.417, 0 fallos (1 omitida)** | **698, 0 fallos (48 opt-in omitidas)** | **BUILD SUCCESS (26 min 31 s)** |

Criterio de la orden: **0 violaciones nuevas** de restricciones y políticas. No se modificó ningún validador, prompt, política, dosis, umbral de adaptación, progresión ni reincorporación. El CI de GitHub corre en cada PR.

## 4. Cambios de expectativa en pruebas existentes (justificados)

| Prueba | Rama | Motivo |
|---|---|---|
| `GeneracionReferenciaIT` | 1 y 3 | Planes de referencia regenerados. 47 y 68 semanas cambian, todas VALIDA y explicadas una a una (`evidencia/DIFERENCIAS_REFERENCIA_*.md`). |
| `CatalogoV39IT` | 2 | **No se cambió la prueba: se corrigió V49**, que había perdido el paso de seguridad «comprueba la espaldera o el anclaje» de los remos con toalla (308, 1072). |
| `RetiradaLocalizadaLimiteIT` / `PreferenciaIT` (R0524) | 3 | Con V50, P-03 ya publica una semana válida sin el 135 (antes NO_FACTIBLE). Se exige que el 135 no vuelva y que haya un solo plan activo. El camino NO_FACTIBLE sigue cubierto por otras pruebas. |
| `V15CambioDeGrupoIT` | 3 | Con V50 el cambio de grupo no se activaba en 8 semillas. Se recorren 24 y se activa con todas sus propiedades. |
| `CatalogoIntegralTextosIT` | 3 | V50 cambia el último paso del 1254 al pasarlo a estiramiento por tiempo. |

## 5. Documentos

| Documento | Contenido |
|---|---|
| `INVENTARIO_INICIAL.md` | Recuentos y línea base. |
| `REGISTRO_MAESTRO_CATALOGO.xlsx` (+ `registro_maestro.tsv`) | Una fila por ejercicio, con estado y evidencia. |
| `CORRECCIONES_POR_EJERCICIO.md` (+ `correcciones_texto_detalle.tsv`) | Cada cambio aplicado, con su antes y después. |
| `IMPACTO_ELEGIBILIDAD.md`, `IMPACTO_PR55.md`, `PLANES_PUBLICADOS.md` | Impacto sobre elegibilidad, la PR #55 y los planes publicados. |
| `PROPUESTA_APOYO_SUSPENDIDO.md` | Propuesta para decisión profesional. |
| `PENDIENTES_Y_LIMITACIONES.md` | Lo que queda abierto y los límites de la revisión. |
| `PROCESO_REVISION.md` | Proceso permanente para futuras migraciones. |
| `VERSION_CATALOGO.md` | Identificación de la versión auditada. |
| `revision_visual/` | Protocolos, veredictos por lote y textos redactados. |
| `herramientas/` | Scripts reproducibles. |
| `evidencia/` | Comparaciones y diferencias de referencia. |
