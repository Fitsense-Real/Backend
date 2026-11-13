# Correcciones definitivas del cierre (V51, V52, V53)

El detalle por ejercicio, con valor anterior, valor nuevo y fundamento de R2 y V3, está en `cierre/adjudicacion/decisiones_v52.tsv` (556 filas). La traza completa de cada propuesta está en `adjudicacion_traza.tsv`, y el SQL generado en `V52__catalogo_cierre_correcciones_verificadas.sql`.

## 1. V51: el nombre deja de decidir

| Antes | Después |
|---|---|
| ImpactoPolicy derivaba el impacto del nombre en español, con expresiones como «correr», «skipping» o «salto», más `high_impact` | `exercises.impact_level` explícito (BAJO, MODERADO o ALTO). La regla por nombre solo se usa si el dato falta, y el `CHECK` lo impide en los activos |
| CatalogEligibilityPolicy excluía del trabajo principal todo nombre con «estiramiento» | `exercises.is_stretch` explícito |

V51 rellena exactamente lo que la regla derivaba sobre V50, así que **no cambia el comportamiento**. Las correcciones van en V52.

## 2. V52: 556 cambios en 437 ejercicios

| Atributo | Cambios |
|---|---|
| Instrucciones | 323 |
| Dificultad (todas al alza: 61 de 1→2 y 3 de 2→3) | 64 |
| Nombre | 40 |
| OH (14 añadidas, 12 quitadas) | 26 |
| Patrón | 24 |
| Prescripción (con la frase final del texto coherente) | 16 |
| AX (11 añadidas, 3 quitadas) | 14 |
| Zona y músculo objetivo | 10 |
| Impacto explícito (6 bajadas y 3 subidas) | 9 |
| HI (todas retiradas) | 5 |
| INV (4 añadidas, 2 quitadas en banco inclinado) | 6 |
| KN (1 añadida, 5 quitadas) | 6 |
| Equipamiento: 300, 756, 1043, 1184 | 4 |
| Estiramiento explícito: 312, 1192 y 1194 a TRUE; 455 y 773 a FALSE | 5 |
| FL (2 añadidas, 2 quitadas) | 4 |

Normas de texto:
- Cada paso termina en punto.
- La frase reglamentaria va como último paso propio y coincide con la prescripción final: «Repite el número de repeticiones indicado.» o «Mantén la duración indicada.».
- Se conservan las advertencias de seguridad vigentes.
- Nada que el GIF no muestre.

Muestra: 675 «Sentadilla pliométrica con mancuerna» pasa a «Sentadilla sumo con mancuerna», con nuevas instrucciones y sin HI. 1017 «Elevación de talones sobre prensa» pasa a «Prensa de piernas sentado en máquina», con patrón KNEE_DOMINANT y zona upper legs/quads.

## 3. V53: apoyo suspendido (decisión de significado)

- `requires_suspended_support` = TRUE en 16 ejercicios: 457, 458, 459, 462, 777, 920, 934, 1069, 1153, 1184, 1301, 1303, 1304, 1306, 1317 y 1335.
- En el motor:
  - `ExerciseRepository.findEligible` excluye `requires_suspended_support` cuando `excludeHanging`.
  - `SafetyConflictPolicy` añade la causa `apoyo_suspendido` en los planes publicados.

## 4. Lo que no se corrigió y por qué

| Grupo | Motivo |
|---|---|
| 248 bajadas de dificultad | V27: no se baja sin aprobación humana |
| 27 identidades ambiguas, 7 no decidibles | El GIF no permite decidir |
| 35 retiradas de bandera (incluida 854 por la decisión D2) | V3 rechaza o no demuestra la condición, criterio sin fijar (DEC-INV-DECLINADO, DEC-HI-LANZAMIENTOS, DEC-HI-CARRERA), identidad dudosa o contrato vigente (913) |
| 25 equipamientos | Criterio V50: no se exige banco a lo que solo se hace sentado; «towel» no existe como código; **775** (codos sobre apoyos elevados → banco) es correcto pero deja sin plan a GM intermedio en casa con peso corporal a 74–75 min: DEC-FACTIBILIDAD-775, pendiente de tu decisión |
| Hallazgos colaterales (231, 620, 1084, 1182, 1303, 1323, 367, 1017) | Sin R2 ni V3: siguiente ronda |
| Dependencia de las contenciones | Sin cambios de código (`CONTENCIONES_DEFINITIVAS.md`) |
