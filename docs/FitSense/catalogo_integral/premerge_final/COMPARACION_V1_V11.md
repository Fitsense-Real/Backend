# v1 frente a v1.1

| | `FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1` | `FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1.1` |
|---|---|---|
| Commit de código | `836a1f5` | **`12c6897`** |
| Rama | `cierre/catalogo-conservador-v1`, intacta como referencia | `cierre/catalogo-premerge-final` |
| Diferencia de código | — | `git diff 836a1f5 12c6897 -- src` = 6 archivos. Ver la lista debajo |
| Migraciones | V1…V56 | **Las mismas**: V1…V56, sin migraciones nuevas |
| Catálogo (A/B/C/D) | 919 / 273 / 27 / 157 | **Idéntico**: ningún dato del catálogo cambia |
| Huella del catálogo (`huella_catalogo.tsv`) | `76ad282c…` | **Idéntica**: el archivo no cambia |
| P-03 nivel 2 | Solo el trabajo total; la regla por colocación existía, apagada | Comparación por colocación (`ComparacionDosisPorColocacion`): **encendida en la candidata y sus pruebas**, apagada en producción |
| Otros interruptores | Contención y P-DOLOR-PUB encendidas; V53 A, V53-B y #64 apagadas | **Iguales** |
| Generación inicial (168, 72, 312) | 168/168, 72/72, 298/312 | Ver `REGRESION_INTEGRADA_FINAL.md`. La generación no pasa por P-03, así que se espera idéntica |
| HALL-GM-01 (40) | 35 publicadas + 5 NO_FACTIBLE, 0 aumentos | **Idéntico** (0 escenarios cambian) |
| Planes publicados, dolor y plan 78 | P-DOLOR-PUB; el plan 78 sustituye 1303 y 494 | **Iguales**; el criterio del plan 78 queda aprobado por el propietario |
| Omisiones impuestas | Limitación documentada | Igual. Se añade la propuesta V57 en una PR aparte, no integrada |

**Archivos de código que cambian de v1 a v1.1:**

| Archivo | Cambio |
|---|---|
| `src/main/.../domain/services/ComparacionDosisPorColocacion.java` | Nuevo |
| `src/main/.../commandservices/SafeRegenerationService.java` | `subidaPorColocacion` usa el comparador; comentario del interruptor |
| `src/main/resources/application.properties` | `fitsense.regeneracion.p03-por-colocacion=${FITSENSE_P03_POR_COLOCACION:false}` |
| `src/test/resources/config/application.properties` | Nuevo; `p03-por-colocacion=true` para las pruebas |
| `src/test/.../ComparacionDosisPorColocacionTest.java` | Nuevo; los 10 casos y uno más |
| `src/test/.../P03ComparacionPorColocacionIT.java` | Comprueba la configuración, restaura el valor y añade `cambioDeDia` |

**Por qué hay un identificador nuevo:** P-03 por colocación es un cambio de comportamiento de la regeneración ante dolor. Aunque en los escenarios medidos no cambia ningún resultado, la versión anterior ya no describe el comportamiento probado. v1 se conserva como referencia válida anterior.
