# Informe final del cierre del catálogo de ejercicios

**Fecha:** 13/11/2026.

**Alcance:** el catálogo completo (1.376 ejercicios: 1.219 activos y 157 inactivos), sobre la base de las PR #56–#59 y #55 sin fusionar (commit 57075e7).

**Ramas:**
- `cierre/v51-v52-correcciones` (PR A: V51 + V52);
- `cierre/catalogo-final` (PR B: V53, decisión de significado, sobre la PR A).

**Lo que no se ha hecho:** nada se ha fusionado ni desplegado. No se han tocado datos de usuarios, no se ha gastado en Replicate, no se ha modificado Flutter y no se han relajado V15, V24, V8C, P-03 ni la progresión.

## 1. Qué se hizo

1. **Línea base inmutable y procedencia por atributo.** `ProcedenciaCatalogoIT` aplica V1–V50 una a una sobre una base efímera y registra qué migración fijó cada valor (`cierre/linea_base/`).
2. **Segunda revisión visual independiente de los 1.219 activos con la animación completa.**
   - R2: 20 revisores de IA, uno por lote.
   - V3: 18 verificadores de IA, uno por cada una de las 717 propuestas.
   - Adjudicación del coordinador (IA) con reglas escritas.

   Ver `REVISION_VISUAL_COMPLETA.md`.
3. **V51.** `impact_level` e `is_stretch` pasan a ser atributos explícitos y el nombre deja de decidir. No cambia el comportamiento (`CatalogoAtributosExplicitosIT`).
4. **V52.** 561 correcciones en 442 ejercicios, con protección del valor anterior exacto:
   - 555 vienen de las propuestas verificadas;
   - 6 aplican la norma de texto a contradicciones texto/prescripción que encontró la matriz de coherencia.

   Ver `CORRECCIONES_DEFINITIVAS.md`.
5. **V53.** `requires_suspended_support` en 16 ejercicios. Es una **decisión de significado** del permiso «no colgarse» y queda en una PR propia para tu aprobación.
6. **Controles permanentes.**
   - Huella ampliada: impacto, estiramiento, apoyo y firma md5 de nombre y textos.
   - Paridad con V51 y oráculo de elegibilidad con atributos explícitos.
   - CHECK de V51.
   - Matriz de coherencia de 13 reglas (`MATRIZ_COHERENCIA_FINAL.xlsx`).
7. **Impacto en el sistema:**
   - rejilla de 168 perfiles;
   - referencia de generación (72 planes);
   - hipertrofia de 30 a 75 minutos;
   - adaptación, progresión y reincorporación;
   - plan 78 y consulta de planes publicados.
8. **Regresión completa en las dos ramas y segunda auditoría desde una base nueva** (`REGRESION_FINAL.md`).
9. **Registro maestro de 1.376 filas** (`REGISTRO_MAESTRO_CATALOGO.xlsx`), contrastado con la huella real: 0 discrepancias en textos y dificultad.

## 2. Respuestas a las 30 preguntas

**1. ¿Se revisaron profundamente los 1.219 ejercicios activos?**
Sí, por IA:
- los 1.219, con la animación completa, en la segunda revisión R2 (20 lotes);
- cada una de las 717 propuestas, por un verificador V3 independiente.

No es una revisión humana (ver la 3).

**2. ¿Cuántos se verificaron visualmente con evidencia suficiente?**
- **1.192** tienen la identidad del movimiento confirmada (CLARA) con fotogramas citados. Por confianza de la R2: ALTA 830, MEDIA 371 y BAJA 18.
- **27** tienen la identidad AMBIGUA (el GIF no basta) y están en la lista profesional.

**3. ¿Cuántos recibieron revisión humana?**
- En el cierre, **ninguno**.
- En todo el historial, solo 4 aprobaciones de dificultad (V28: 122, 776, 872 y 878, firmadas por Juan y Daniel el 10/10/2026). 776 está inactivo.

**4. ¿Cuántos recibieron validación profesional?**
**Ninguno.** No hubo acceso a ningún profesional y no se ha inventado ninguna firma. Es un bloqueo del cierre (`REVISION_PROFESIONAL.md`).

**5. ¿Cuántos ejercicios tenían errores reales?**
- **442 activos** con contradicciones demostradas entre el GIF (o la prescripción) y el dato: 561 cambios.
- Además, **16** con apoyo suspendido que el permiso actual no cubre (V53).
- Otros errores probables no se corrigen porque exigen una decisión, entre ellos:
  - 775 (equipo);
  - 1113 (dificultad, por la decisión previa de V39);
  - 854 (HI, por D2);
  - 913 (OH, por un contrato);
  - las familias DEC-INV-DECLINADO, DEC-HI-LANZAMIENTOS y DEC-HI-CARRERA;
  - 1083;
  - 7 hallazgos colaterales (ver la 9).

**6. ¿Cuántos fueron corregidos?**
- **442** en V52.
- Los 16 de V53 están corregidos en el dato, pero pendientes de que apruebes su significado.
- Ninguna dificultad se ha bajado (V27).

**7. ¿Cuántos quedaron excluidos o inactivos por falta de evidencia?**
- En el cierre, **ninguno**: no se desactivó ningún ejercicio para reducir pendientes.
- Los 157 inactivos lo eran ya: 52 de la semilla V8, 99 nunca activados en V11 y 6 desactivados en revisiones anteriores. Su inventario está en `cierre/inactivos/`.

**8. ¿Cuántos permanecen contenidos por seguridad?**
**16**, sin cambios de código (`CONTENCIONES_DEFINITIVAS.md`):
- INC-CAT-C39: 8;
- INC-4C-03: 7, de los que 5 tienen propuesta de liberación no aplicada;
- INC-CAT-1374: 1, redundante.

Además, 854 conserva HI por D2.

**9. ¿Queda algún ejercicio elegible con una contradicción material?**
**Sí.** No se ocultan. Los conocidos son:

| Caso | Ejercicios | Contradicción |
|---|---|---|
| Impacto sin HI | 1083 | Impacto ALTO sin HI: sigue elegible con «sin impacto» (regla R03 de la matriz) |
| Hallazgos colaterales sin R2 ni V3 | 231 | Nombre, patrón y zona frente a un press de banca cerrado |
| | 620 | Identidad |
| | 1323 | Texto que menciona unos rodillos que no aparecen |
| | 367 | Clasificación dudosa |
| | 1182 | Equipo de más (conservador) |
| | 1017 | Músculos secundarios |
| Identidad AMBIGUA | 27 | El GIF no basta para confirmar el ejercicio |
| Dificultad sin aprobación humana (V27) | 248 | La R2 propone bajarla. El efecto es conservador |

Todos figuran en la lista profesional o como hallazgos abiertos.

**10. ¿Se resolvieron los 320 pendientes profesionales?**
- **181 resueltos con evidencia del GIF:** 109 corregidos y 72 verificados sin cambios.
- **139** siguen necesitando a un profesional, sobre todo por dificultad e identidad.

**11. ¿Se resolvieron los 59 pendientes visuales?**
**39 sí** (31 corregidos y 8 verificados). 20 pasan a la lista profesional.

**12. ¿Se resolvieron los 11 pendientes técnicos?**
**10 sí** (2 corregidos y 8 verificados). 1 pasa a la lista profesional.

**13. ¿Se revisaron nuevamente los 813 anteriores?**
Sí, los 813 (552 verificados y 261 corregidos en la auditoría integral):
- **211** recibieron correcciones nuevas;
- **486** se confirman sin cambios;
- **116** pasan a la lista profesional.

**14. ¿Todos los nombres corresponden al movimiento real?**
No todos:
- 40 nombres corregidos en V52;
- 27 identidades AMBIGUAS sin decidir;
- dos nombres que no corresponden (231, y 620 por identidad).

El resto (1.192 con identidad CLARA) sí corresponde según la R2.

**15. ¿Todas las instrucciones corresponden al movimiento real?**
No todas:
- 329 textos reescritos;
- 6 frases finales corregidas por la norma de texto, ya incluidas entre los 329;
- 4 textos que el GIF no permite verificar (494, 535, 1086 y 1294);
- 2 hallazgos abiertos (1323, y 1303, que está contenido).

**16. ¿Todos los equipos son coherentes?**
Casi todos:
- 4 corregidos (300, 756, 1043 y 1184);
- la corrección de 775 se aplaza (DEC-FACTIBILIDAD-775);
- 1182 probablemente exige un equipo que sobra;
- 11 accesorios esperan un criterio (`PENDIENTES_Y_LIMITACIONES.md`).

El principal está siempre entre los requisitos (regla R05: 0 incumplimientos).

**17. ¿Todas las dificultades están justificadas?**
**No.** `difficulty_level` nunca fue aprobado por personas (V27):
- 63 subidas conservadoras aplicadas con evidencia;
- 248 bajadas propuestas sin aplicar;
- la subida de 1113, vetada por la decisión previa de V39.

**18. ¿Todas las clasificaciones y capacidades son coherentes?**
- La matriz da 0 incumplimientos en las reglas estructurales R01, R02 y R04–R10.
- Las capacidades no son un atributo del catálogo: se derivan en ejecución del patrón, la zona y la prescripción.
- Quedan 1083 (R03) y las clasificaciones dudosas de 231 y 367.

**19. ¿Las restricciones de seguridad están correctamente aplicadas?**
En el código, sí:
- el oráculo independiente (`CatalogoElegibleIT`) coincide con el motor en todas las restricciones;
- los CHECK de V51 impiden HI con impacto BAJO.

En el dato hay tres excepciones:
- 1083, la única contradicción R03;
- el permiso «no colgarse» no cubre el apoyo suspendido mientras no apruebes V53;
- los 5 retenidos de INC-4C-03 siguen fuera sin necesitarlo.

**20. ¿Se resolvieron los 13 ejercicios adicionales de apoyo suspendido?**
El dato, sí. La revisión del catálogo completo encontró **32** casos, más que los 13 + 5 de partida (`VALIDACION_SEGURIDAD.md` §5):

| Tratamiento | Ejercicios |
|---|---|
| Marcados en V53 | **16**: los 5 de C39 (1153, 1301, 1303, 1304 y 1306) y 11 más (457, 458, 459, 462, 777, 920, 934, 1069, 1184, 1317 y 1335), contrastados por nombre |
| Documentados sin marcar, con las manos en el suelo | 8 |
| Documentados sin marcar, en máquina asistida | 8 |
| Rechazado por la V3 | 1 (821) |

Que el permiso «no colgarse» cubra el apoyo suspendido **espera tu aprobación**.

**21. ¿Se resolvió definitivamente la clasificación del 853?**
- El **impacto**, sí: BAJO, con evidencia por fotograma (siempre un pie en el suelo), igual que 1093.
- Su **identidad** queda AMBIGUA y está en la lista profesional.
- Su impacto ya no depende del nombre (V51).

**22. ¿Qué sucedió con los dos perfiles previamente NO_FACTIBLE?**
Los dos (LOSE_WEIGHT, principiante, en casa, «sin suelo» y «sin las tres») **siguen teniendo plan**:
- sin suelo: 92 → 93 elegibles;
- sin las tres: 66 → 67 elegibles.

No se ha relajado ninguna regla (`IMPACTO_ELEGIBILIDAD.md` §4).

**23. ¿Qué sucedió con el plan 78?**
- La semana sin empezar bloquea solo el jueves, por el 1303 (`INC-CAT-C39+apoyo_suspendido`).
- La reparación sigue sin publicar: además del 916 (V48), **el 910 sube a nivel 2 en V52** y la revalidación lo rechaza.
- No se resuelve relajando la dificultad. Las opciones están en `IMPACTO_PLANES_PUBLICADOS.md` §2.
- Con los dos sustituidos (1046 y 433), la reparación publica una versión 2 que solo cambia el 1303.

**24. ¿Existen otros planes publicados potencialmente afectados?**
Sí, potencialmente:
- cualquier sesión futura con uno de los 32 ejercicios que ganan bandera o de los 16 de V53, si el perfil restringe esa bandera;
- y las reparaciones de semanas que contengan alguno de los 63 que suben de nivel.

La consulta de solo lectura está en `IMPACTO_PLANES_PUBLICADOS.md` §3. **No se ha ejecutado** contra producción.

**25. ¿Qué comportamiento cambió respecto de la línea base?**
- **Elegibilidad:** cambia en los 168 perfiles (mediana −2, principiantes −13), aunque ninguno pierde el plan. Desaparecen las 58 semanas de la base que contenían un ejercicio que el catálogo final saca para ese perfil.
- **Generación:** 70 de 72 planes de referencia cambian, y los 72 siguen VALIDA.
- **Plan 78:** el 910 se suma como límite de la reparación.
- **Contención:** una nueva causa `apoyo_suspendido` (solo con V53).
- **Textos:** los que ve el usuario cambian en 329 ejercicios.

El código del motor no cambia, salvo que V51 sustituye la regla por nombre por el atributo explícito con el mismo valor.

**26. ¿Se mantuvieron V24 y V8C?**
Sí. Ningún validador se ha tocado. Sus pruebas pasan en la regresión: PENDIENTE_REGRESION_V24.

**27. ¿Pasaron las pruebas completas?**
PENDIENTE_REGRESION_TOTAL

**28. ¿Todas las migraciones pueden reproducirse desde cero?**
Sí:
- cada prueba de integración crea una base nueva y aplica V1–V53;
- `ProcedenciaCatalogoIT` las aplica una a una (`cierre/auditoria_final/`): PENDIENTE_AUDITORIA;
- V52 falla ante un estado previo inesperado y es idempotente frente a una base ya corregida.

**29. ¿Qué PR deben fusionarse y en qué orden?**
**#56 → #57 → #58 → #59 → #55 → PR A (V51 + V52) → PR B (V53)**. Cada una tiene como base la anterior y Flyway no admite fusionarlas fuera de orden. Dos condiciones:
- #56 arrastra HALL-PROG-01 (ver la 30);
- PR B solo si apruebas el significado de V53.

**30. ¿El catálogo está realmente listo para merge sin pendientes que comprometan su coherencia o seguridad?**
**No.** Quedan:
- la validación profesional inexistente (313 decisiones);
- seis decisiones de significado o de criterio tuyas;
- dos decisiones previas que exigen revisión humana (854 y 1113);
- la contradicción visible de 1083;
- 7 hallazgos colaterales abiertos;
- el plan 78;
- HALL-PROG-01: con el catálogo de #56, un caso de progresión con dolor deja una semana sin plan por un defecto latente de la adaptación.

## 3. Bloqueos

1. **Validación profesional:** ninguna. Hay 313 decisiones en `cierre/REVISION_PROFESIONAL_LISTA.tsv`, entre ellas 248 de dificultad (V27) y 27 identidades.
2. **Decisiones de significado o de criterio que debes aprobar:**
   - V53: el apoyo suspendido entra en «no colgarse»;
   - DEC-INV-DECLINADO: 171, 377, 840 y 841;
   - DEC-HI-LANZAMIENTOS: 1083–1087, incluida la contradicción visible de 1083;
   - DEC-HI-CARRERA: 1133, 1134 y 1175;
   - DEC-FACTIBILIDAD-775;
   - la liberación de 5 retenidos de INC-4C-03.
3. **Decisiones previas que exigen revisión humana:** D2 de V37.1 (854) y el dictamen de V39 (1113). Se respetan.
4. **Plan 78:** la reparación no publica mientras la semana contenga ejercicios que subieron de nivel (916 y 910).
5. **Hallazgos colaterales abiertos:** 231, 620, 1323, 367, 1182 y 1017. 1303 está contenido y 1084 quedó resuelto.
6. **HALL-PROG-01**, desde #56:
   - el sustituto de un ejercicio progresado hereda la dosis progresada y V24 lo rechaza, un defecto latente que ya existe en `generation`;
   - con el catálogo de #56, la regeneración segura ya no encuentra una semana sin más trabajo y esa semana queda NO_FACTIBLE.

   `ProgresionIT` y `ProgresionRev2IT` p02 fallan desde #56. No se corrige aquí porque es un cambio de adaptación, fuera del catálogo.

## 4. Veredicto

CATÁLOGO NO APTO PARA MERGE
