# Informe de cierre · FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1

**Orden:** «Finalizar la versión técnica conservadora del catálogo» (15/11/2026).

**Dictamen: VERSIÓN TÉCNICA CONSERVADORA LISTA PARA REVISIÓN DE MERGE.**

No se ha fusionado ni desplegado nada:

- `generation`, Railway, Neon, Flutter y los datos reales están intactos;
- no se ha gastado nada en Replicate;
- V53, V53-B, #64 y la regla P-03 por colocación siguen apagados.

## Entregables (`docs/FitSense/catalogo_integral/cierre_conservador/`)

| # | Documento | Contenido |
|---|---|---|
| 1 | `INFORME_CIERRE_CONSERVADOR.md` | Este informe |
| 2 | `CATALOGO_ELEGIBLE_FINAL.xlsx` | Activos y elegibles A (más `catalogo_clasificado.tsv`) |
| 3 | `EJERCICIOS_RESTRINGIDOS.xlsx` | B1, B2 y C, con restricción y motivo |
| 4 | `REVISION_853.md` | Evidencia completa, decisión y V56; los 4 perfiles LW |
| 5 | `COMPARACION_72_PLANES.xlsx` | 72 planes, 15 dimensiones, causa de cada diferencia (más `comparacion_72.tsv`) |
| 6 | `REFERENCIA_CATALOGO_CONSERVADOR.md` | Referencia nueva aparte, condiciones de generación, por qué cambian los 72 |
| 7 | `VALIDACION_DOLOR_PUBLICADO.md` | P-DOLOR-PUB, los 10 casos, trazabilidad con OTHER |
| 8 | `VALIDACION_HALL_GM_01.md` | 40 escenarios con las protecciones apagadas y encendidas |
| 9 | `VALIDACION_P03.md` | Contraejemplo, garantía, factibilidad y recomendación |
| 10 | `IMPACTO_FACTIBILIDAD.md` | Matrices 168, 312, 72 y 40, y los casos específicos |
| 11 | `REGRESION_FINAL.md` | Regresión completa (primer candidato fallido y candidato definitivo aprobado) |
| 12 | `VERSION_CONGELADA.md` | Identificación, catálogo, interruptores, huella y reconstrucción |
| 13 | `RIESGOS_RESIDUALES.md` | 12 riesgos conocidos y acotados |
| 14 | `LIMITACIONES_TESIS.md` | Lo que la tesis puede y no puede afirmar; texto propuesto |
| 15 | `ORDEN_INTEGRACION_PR.md` | Pila #55–#66, dependencias y orden propuesto |

Herramientas reproducibles en `herramientas/` y evidencias en `evidencia/`: 853, rejillas de 168 y 312, HALL-GM-01, congelado y regresión.

## Las 20 respuestas

**1. Commit definitivo.**
**`836a1f5aded805ec4c696e2bd411c97d4ee02f8d`** (rama `cierre/catalogo-conservador-v1`). Los commits posteriores solo añaden documentos y evidencias; `git diff 836a1f5 HEAD -- src` está vacío.

El primer candidato, `eccff00`, no pasó la regresión completa (13 fallos y 1 error de pruebas). Se corrigió en un commit nuevo y la regresión se repitió entera.

**2. A/B/C/D.**
Sobre 1.376 ejercicios (1.219 activos):

- **A 919**;
- **B 273** (B1 252, B2 21);
- **C 27** (INC-4C-03 8 + identidad no determinada 19);
- **D 157**.

**3. ¿Se resolvió el 853?**
**Sí, recuperable.** La animación completa (24 fotogramas) muestra una elevación alterna de rodilla y una media sentadilla con los dos pies apoyados, sin ningún fotograma sin apoyo. Coincide con R2, con V3 y con el nombre de la fuente.

- V56 corrige solo el nombre («Media sentadilla con elevación de rodilla alterna») y las instrucciones, que piden expresamente no saltar.
- Exige que los atributos de seguridad sigan siendo los revisados. No relaja nada.
- Sale de la contención (C → A). La decisión profesional P286 sigue abierta.

**4. Los 4 perfiles LW.**
LOSE_WEIGHT en casa (peso corporal, mancuernas y banda), 4 días y 45 min:

- BEGINNER/SIN_SUELO;
- BEGINNER, INTERMEDIATE y ADVANCED con SIN_TODO.

Con el 853 contenido solo quedaban dos cardios (1093 y 1117). El motor de reglas los rechazaba por V5 «1 ejercicio» y V25 «no incluye CARDIO». No había ninguna alternativa en el catálogo. **Con V56, los 4 tienen plan válido.**

**5. Qué cambió en los 72.**
Los 72 planes cambian y los 72 siguen siendo VALIDA:

- mismos días;
- duración ±1–3 min en 15 planes;
- ±1 ejercicio en 7 planes;
- 0 contenidos;
- 0 equipamiento incompatible;
- 0 dosis fuera del patrón de su objetivo y nivel.

Por causa:

- **22 por CONTENCIÓN:** el plan histórico usaba 1113 ×9, 1176 ×5, 1177 ×4, 494 ×4, y 1311, 310 y 535 ×1.
- **50 por SELECCIÓN DETERMINISTA.**

**6. Por qué cambiaron todos.**
El motor de reglas elige con una única secuencia aleatoria con semilla (`nextInt(n)` y `shuffle`). La contención quita candidatos en **todos** los perfiles (el 1311 era elegible para cualquiera). Con una lista más corta, el mismo número aleatorio señala otro ejercicio y desplaza las elecciones siguientes.

**No hubo ningún cambio accidental de política:** con la contención apagada, la generación reproduce línea por línea la referencia histórica (`GeneracionReferenciaIT` pasa sobre el commit definitivo).

**7. ¿Son válidos los 72?**
**Sí:**

- 72/72 VALIDA con el validador real;
- sin contenidos;
- con equipamiento compatible;
- con cobertura completa;
- sin dosis no autorizadas;
- con las 72 diferencias explicadas.

La referencia nueva se guardó **aparte**, con su propia prueba. La histórica no se sobrescribió y su huella está protegida.

**8. Protección por dolor en sesiones publicadas.**
**Verificada en los 10 casos obligatorios**, con pruebas de integración y unitarias:

- el ejercicio con dolor queda marcado con `dolor_registrado` en las sesiones que admiten registros;
- solo se puede registrar como omitido;
- la sesión **no** se bloquea entera, se puede empezar y lleva el aviso en la API;
- las otras causas de seguridad se suman, no se sustituyen;
- las sesiones terminadas no se tocan;
- los reintentos no duplican nada;
- no se añaden sustitutos ni dosis.

En los 40 escenarios: con la protección apagada, 7 participantes registran trabajo en lo que les dolió; encendida, 0.

**OTHER** da una trazabilidad suficiente **por reconstrucción**: mientras la exclusión está activa, toda omisión de ese ejercicio la impone el sistema. La consulta está en el documento. Una causa propia sería un cambio metodológico, así que queda como recomendación.

**9. Dolor con NO_FACTIBLE.**
Cuando la adaptación no encuentra una alternativa segura, no se publica nada, la exclusión sigue activa y la semana vigente queda intacta. En ella, lo que dolió y se repite **queda marcado y solo se puede omitir**. Antes de P-DOLOR-PUB se podía registrar con trabajo; es el defecto que se corrigió.

Comprobado en `halGm01ConContencion` (9717) y en 9703: `trabajoTrasDolor = 0`.

**10. ¿Se evita subir la dosis del ejercicio retirado?**
**Sí.**

- El sustituto no recibe más trabajo que el quitado (HALL-GM-01, V8C).
- Si su capacidad exige más, la semana es NO_FACTIBLE.
- P-03 nivel 2 no publica si sube el trabajo total.
- En los 40 escenarios, 0 colocaciones suben y 0 totales suben en lo publicado.
- La protección por dolor no añade ninguna dosis al plan publicado.

**11. Los 40 de HALL-GM-01.**

| | Publicadas | Aumentos | NO_FACTIBLE | Trabajo registrado en lo que dolió |
|---|---|---|---|---|
| Protecciones apagadas (idéntico a la evidencia corregida de #65, 40/40) | 36 | 0 | 4 (9703, 9716, 9719, 9721) | 7 participantes |
| **Encendidas** | **35** | **0** | **5** (9717, 9728, 9733, 9734, 9740) | **0** |

El cambio de NO_FACTIBLE viene del conjunto elegible distinto, no de ningún cambio de política.

**12. ¿Riesgo demostrado de P-03?**
**Demostrado como posible, no observado.** Hay un contraejemplo determinista (`P03ComparacionPorColocacionIT`): una colocación reducida vuelve a la base sin que suba el total, y la política vigente la acepta.

- **No viola** la garantía aprobada de P-03, que compara el total, sin linaje.
- **Sí** abre una vía de restauración sin autorización.
- En los 40 escenarios ningún participante publica por P-03 nivel 2.

**13. Propuesta para P-03.**
**Activar la regla por colocación**, ya implementada y apagada, como cambio de política aprobado por el propietario.

- Con ella encendida, los 40 escenarios dan el mismo resultado: no cuesta factibilidad en lo medido.
- Cierra la única vía conocida.
- Mientras no se apruebe, se **mantiene** la política vigente. No se ha activado ni modificado.

**14. Perfiles con plan válido.**

- Rejilla de 168: **168/168**.
- Referencia de 72: **72/72**.
- Rejilla de 312 de hipertrofia: **298/312**. Los 14 restantes son NO_FACTIBLE legítimos por V20 (casa con solo peso corporal, 775 con banco), anteriores a esta versión.

**15. Pérdidas de cobertura.**

- **Generación inicial:** ninguna frente a la versión sin contención (168, 312 y 72).
- **Adaptación y reparación:** pérdidas puntuales y conservadoras por haber menos alternativas:
  - HALL-GM-01: 5 NO_FACTIBLE frente a 4;
  - el escenario de reparación con linaje: NO_VALIDA porque V8C no deja subir la dosis del sustituto.
- En ningún caso se publica algo inseguro ni con más trabajo.

**16. ¿Se mantienen V24, V8C y D8?**
**Sí, sin relajar**, igual que V15, V20, V25 y P-03. No se cambió ningún validador ni ninguna política. La adherencia, la progresión y la reincorporación no cambian.

**17. ¿Pasó la regresión completa?**
**Sí, sobre `836a1f5`:**

- 1530 unitarias y 753 de integración;
- 0 fallos y 0 errores;
- 57 opt-in omitidas, de las que las relevantes se ejecutaron aparte sobre el mismo commit;
- en un worktree aislado, sin nada concurrente en esa carpeta;
- con el árbol sin cambios antes y después.

La del primer candidato **no** se dio por aprobada.

**18. ¿Las migraciones se reconstruyen correctamente?**
**Sí.**

- Cada una de las 157 clases de integración arranca un PostgreSQL 16 vacío y aplica Flyway V1…V56 (59 migraciones) con `ddl-auto=validate`.
- La huella del catálogo (`CatalogoCalidadIT`) pasa.
- Dos volcados congelados independientes desde base vacía dan la **misma huella normalizada**. El volcado en bruto solo difiere en dos columnas con la hora de la reconstrucción.

**19. Riesgos restantes.**
Los 12 de `RIESGOS_RESIDUALES.md`. Los principales:

- el catálogo no tiene validación profesional (312 decisiones abiertas);
- la IA puede haberse equivocado en ejercicios clasificados A;
- P-03 compara solo el total (pendiente de decisión);
- las omisiones impuestas por el sistema cuentan en la adherencia;
- hay menos alternativas al sustituir o reparar (NO_FACTIBLE conservadores);
- nadie recibe los ejercicios de apoyo suspendido;
- todo está medido con perfiles sintéticos.

**20. ¿Lista para la revisión final?**
**Sí**, con estas decisiones que corresponden al propietario y que no bloquean la revisión:

- activar o no la regla P-03 por colocación;
- aceptar que una reparación sustituya también los ejercicios contenidos (plan 78: 1303 y 494);
- cómo tratar en la adherencia las omisiones que impone el sistema;
- #64 y #66.

## Verificación de los criterios del dictamen

| Criterio | Cumplido |
|---|---|
| Estado real verificado (HEAD, PR, ausencia de duplicados, interruptores) | Sí (`ORDEN_INTEGRACION_PR.md`) |
| 853 resuelto con evidencia, sin forzar la recuperación | Sí |
| 72: referencia aparte, comparación exhaustiva, cambios explicados, sin ajustar expectativas para obtener verde | Sí |
| Dolor en sesiones publicadas: 10 casos, sin desactivar otras restricciones, sin cambiar la adherencia | Sí |
| P-03 reproducido y documentado, sin activarlo | Sí |
| Matrices clasificadas, sin VIOLACION_SEGURIDAD, ERROR_TECNICO ni REGRESION_INESPERADA | Sí |
| Regresión completa sobre el commit exacto, sin interrupciones | Sí (`836a1f5`) |
| Versión congelada y reconstruible | Sí (huella normalizada REPRODUCIBLE) |
| Limitaciones declaradas, sin afirmar validación profesional ni seguridad garantizada | Sí |

**VERSIÓN TÉCNICA CONSERVADORA LISTA PARA REVISIÓN DE MERGE.**
