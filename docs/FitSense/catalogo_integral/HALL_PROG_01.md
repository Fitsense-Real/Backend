# HALL-PROG-01 · Causa exacta, reproducción y corrección

**Estado: corregido y verificado. Sin merge ni despliegue.**
- PR independiente: #62 (`fix/sustituto-dosis-capacidad`, sobre `generation`).
- El mismo commit está dentro de la PR A (#60) para que su CI pueda ponerse en verde.

## 1. Síntoma

Fallaban `ProgresionIT.p02ProgresionSinElExcluido` y su gemela `ProgresionRev2IT.p02ProgresionSinElExcluido` con `NoSuchElementException` en la línea 524: la semana 4 quedaba **sin plan**. La CI de #60 y #61 estaba en rojo solo por estas dos pruebas.

## 2. Reproducción en condiciones idénticas

Mismas condiciones en todas las filas:
- p02 ejecutada sola;
- PostgreSQL 16 nuevo con Flyway;
- motor de reglas sin IA;
- la misma configuración.

Solo cambian la rama y la **semana de referencia**, que fija la semilla del motor de reglas (`userId × 1.000.003 + inicio de semana`) con `-Dfitsense.pruebas.semana-referencia=AAAA-MM-DD`.

| Rama (catálogo) | 19-oct | 26-oct | 02-nov | 09-nov | 16-nov |
|---|---|---|---|---|---|
| `generation` 623c4fa (V1–V50), sin corrección | FALLA | FALLA | FALLA | pasa | FALLA |
| Base del cierre 57075e7 (V1–V50 + #56–#59), sin corrección | FALLA | FALLA | FALLA | FALLA | FALLA |
| PR A (V51 + V52) **con corrección** | pasa | pasa | pasa | pasa | pasa |
| Base 57075e7 **con corrección** | pasa | pasa | pasa | pasa | pasa |
| `generation` **con corrección** | pasa | pasa | pasa | pasa (clase completa) | pasa |

**Conclusión de la reproducción:**
- El defecto **ya existía antes del cierre del catálogo**. En la base, sin V51 ni V52, falla en las 5 semillas, y en `generation` falla en 4 de 5.
- No es del catálogo: el catálogo solo cambia **qué semillas lo disparan**.
- Corrige una afirmación anterior. En la CI, la base pasaba (16:10 y 22:16 UTC) porque el usuario de p02 caía en una semilla que no lo dispara. Su id depende del orden de las pruebas en el contexto compartido. Con el catálogo nuevo, el mismo id cayó en una semilla que sí lo dispara.

## 3. Causa exacta

Traza de una semana que falla:
1. La progresión sube el ejercicio marcado de 3x18/45 (resistencia muscular) a **3x19/45**. Esa subida está autorizada para **esa colocación**.
2. El usuario lo señala con dolor. La semana conservada lo quita y SUST-SEG-1 (`SafetySubstitutionPolicy`) pone un sustituto seguro.
3. **Defecto.** `SafetySubstitutionPolicy.dosis` copiaba al sustituto las series, repeticiones y descanso del quitado. El sustituto heredaba **3x19**, una dosis progresada sin autorización propia.
4. V24 (`CapabilityPrescriptionPolicy`) rechaza la semana: el sustituto «no lleva la dosis de ninguna capacidad». **V24 tiene razón.**
5. Se intenta la regeneración segura (P-03 nivel 2):
   - con el catálogo V50 y algunas semillas, la semana regenerada pasa;
   - con otras semillas, y con el catálogo nuevo, la regenerada con dosis base **sube el trabajo** (1.140 → 1.260 s). P-03 la rechaza, porque ante dolor no se añade trabajo, y la semana queda NO_FACTIBLE (V32).
6. No se publica nada inválido: la semana 4 queda sin plan y la prueba falla al buscarlo.

Los validadores hicieron su trabajo. El fallo estaba en el paso 3: el sustituto no debe heredar automáticamente una dosis progresada que no es suya.

## 4. Corrección (mínima)

Archivo: `SafetySubstitutionPolicy.dosis`. Solo afecta a los objetivos que se prescriben por capacidades (`CapabilityPrescriptionPolicy.gobiernaGeneracion`) cuando hay perfil.
1. Si la dosis heredada ya es una dosis de capacidad válida para el sustituto (`acreditada`), se conserva. La reparación localizada no cambia cuando ya era válida.
2. Si no, se toma la dosis de una **capacidad admisible para el sustituto** (`admisibles` y `dosis`, la misma fuente que usa V24):
   - con las series del original, si caben;
   - **nunca con más trabajo** (series × repeticiones o × segundos) que el quitado;
   - prefiriendo el mismo descanso (la misma capacidad) y, entre las válidas, la de más trabajo.
3. Si ninguna dosis de capacidad cabe sin añadir trabajo, no se inventa ninguna: queda la heredada. Los validadores rechazan la semana en vez de publicarla.

**No cambian:**
- V24, V8C ni P-03;
- la prueba p02;
- GAIN_MUSCLE (dosis 4D v3);
- los contextos sin perfil;
- la generación inicial, que no pasa por SUST-SEG-1.

**Rutas que usan esta dosis:**
- la semana conservada de adaptación (`AdaptedWeekProducer`, incluida la comprobación V8C de «la IA no sube dosis a través del sustituto», que ahora compara con una dosis igual o menor: es más estricta, no más laxa);
- la reparación de planes publicados (`PublishedPlanSafetyRepairService`, plan 78).

## 4 bis. Efecto colateral encontrado en la regresión y cerrado: V8C ya no veía una subida de la IA

La primera regresión completa con la corrección descubrió 2 fallos nuevos: `O12DolorProgresionApagadaIT.subeSeries` y `O12DolorProgresionEncendidaIT.subeSeries` («una subida de dosis de la IA no se publica nunca»).
- **Escenario.** La IA sube 1222 de 3x18 a 4x18. 1222 está excluido por dolor y SUST-SEG-1 lo sustituye por 56.
- **Antes.** El sustituto heredaba 4x18 y V8C («la IA no sube dosis a través del sustituto») rechazaba la propuesta.
- **Con la corrección.** El sustituto recibe 3x18, una dosis de capacidad y la misma que la medida. **Al usuario no le llegaba ninguna subida**, pero la propuesta de la IA, que sí subía la dosis, se publicaba como APLICADA.

**Decisión.** No se toca la prueba: su intención («una propuesta de la IA que sube dosis no se publica») es la correcta. Se **refuerza V8C**:
- `AdaptedWeekProducer.subidasDeLaIaEnQuitados` compara la dosis que la IA dio a cada ejercicio que luego se sustituye con la que tenía antes de la IA;
- si la sube (series, repeticiones o segundos), la propuesta se rechaza y se pasa al respaldo, como antes.

Es más estricto, no más laxo. Pruebas:
- `SubidaIaEnQuitadoTest` (5 casos: series, repeticiones, segundos y 2 controles);
- O12 vuelve a dar RESPALDO con la progresión apagada y encendida;
- p02 sigue pasando.

## 5. Pruebas

**Nuevas (`SustitutoDosisCapacidadTest`, 9 casos):**

| Tipo | Caso |
|---|---|
| Positivo | La dosis progresada 3x19/45 no pasa al sustituto: recibe 3x18/45, acreditada por V24 |
| Regresión | Una dosis que ya es de capacidad (3x10/90) se conserva |
| Ante dolor nunca más trabajo | GENERAL_FITNESS, LOSE_WEIGHT e INCREASE_STRENGTH × 6 dosis: el trabajo del sustituto ≤ el del quitado |
| Negativo | Si ninguna dosis de capacidad cabe (1x5/150), no se inventa ninguna y la heredada no se acredita: la semana se rechaza, no se publica |
| Control | GAIN_MUSCLE sin cambios |
| Control | Sin perfil, el comportamiento anterior no cambia |
| Control | El cardio de duración conserva su dosis |

**De integración, que cubren cada flujo:**

| Flujo | Pruebas |
|---|---|
| Progresión | `ProgresionIT` y `ProgresionRev2IT` (p02 y las demás), en las 5 semillas |
| Adaptación | `O12DolorProgresion{Apagada,Encendida}IT`, `SubidaIaEnQuitadoTest`, `SubidaPorSustitucionTest`, las 6 `RetiradaLocalizada*IT`, las 4 `SinSustitutoDolor*IT` y las unitarias de V8C (`DolorSinMasTrabajoV8CTest`, `V8CDiagnosticoSustitucionesTest`…) |
| Reincorporación | `Reincorporacion4DTest` y `ReincorporacionTrasProgresionTest` (unitarias). `ReincorporacionIaRealIT` es opcional: necesita Replicate y no se ha ejecutado |
| Planes publicados | `Plan78ContencionReducidaIT` |
| Generación | `GeneracionReferenciaIT`: el cambio no la toca. Sus diferencias se deben a 775 (`REGRESION_FINAL.md`) |

El resultado de la regresión completa está en `REGRESION_FINAL.md`.
