# 08 · Regresión, 960 perfiles y REPLAY

**Código evaluado:** `b8c283d`, con todo el código de producción y las pruebas de la retirada. Después solo hay documentación.

**Ejecución:** en la configuración por defecto (`off/off`) y en dos encendidas, cada una con:
- `mvnw verify -Pintegration` completo;
- la comparación de 960 perfiles;
- la REPLAY de 100 semanas.

Las configuraciones se pasan con `-Dfitsense.adaptation.difficulty-guard` y `-Dfitsense.adaptation.pain-localized-removal`.

Evidencia (`evidencia/`):

| Fichero | Contenido |
|---|---|
| `regresion_*_pruebas.txt` | recuento por clase |
| `regresion_*_fallos.txt` | fallos con su mensaje |
| `c960_replay_*_pruebas.txt` | 960 perfiles y REPLAY |
| `replay_*.diff` | diferencias de la REPLAY |
| `regresion_resumen.txt` | hashes |
| `reg_si.sh` | script de la corrida |

## Resultados

Una omitida no cuenta como aprobada.

| Bloque | off / off (por defecto) | preferencia / on (D) | on / on (D con B) |
|---|---|---|---|
| Unitarias | **1.185 aprobadas**, 0 fallidas, 0 omitidas | 1.185 / 0 / 0 | 1.185 / 0 / 0 |
| Integración | **478 aprobadas**, 0 fallidas, 40 omitidas (de 518) | 475 / **3** / 40 | 475 / **3** / 40 |
| 960 perfiles | `planes.csv` `d109a3da…`, `colocaciones.csv` `d227848b…` (los de referencia) | **idénticos** | **idénticos** |
| REPLAY 100 semanas | solo marcas de tiempo de `exclusiones_dolor.csv` | solo N08, **idéntica** a la REPLAY de C sin retirada | idéntica a la de D |

Las 40 omitidas son laboratorios opt-in: las 38 de antes, más `RetiradaLocalizadaFactibilidadIT` y el barrido A/B/C. Varios se ejecutaron aparte: 960, REPLAY, 22 perfiles y factibilidad.

## Las 3 fallidas con la retirada encendida

Las tres son **esperadas y explicadas**. **No se ha modificado ninguna prueba.**

| Prueba | Por qué falla con `-D…=on` | ¿Defecto? |
|---|---|---|
| `ConservacionPatronH1IT.reglaPorDefecto` | Comprueba que el límite viene **apagado por defecto**, y la corrida lo enciende por `-D` | No: comprueba la configuración por defecto (ya documentado en #38) |
| `RetiradaLocalizadaPorDefectoIT.apagadaPorDefecto` | Comprueba que la retirada viene **apagada por defecto**, y la corrida la enciende por `-D` | No: misma razón |
| `RespaldoConDolorIT.conDolorSinSustitutoNoPublicaNadaInvalido` | Es el caso del **1373** («círculos de muñeca», MOBILITY, de **relleno**), sin sustituto en ninguna estrategia. Con la retirada se **publica** la semana medida sin el 1373 (sesión 105, 37 → 34 min; V32 RESPALDO/CONSERVED con `RETIRADA_SEGURA`). La prueba fija, para la semana de referencia, el desenlace **histórico**: NO_FACTIBLE sin publicar | **No: es el cambio funcional que se busca.** Las aserciones de seguridad de la misma prueba se cumplen: la exclusión del 1373 sigue activa y el 1373 no está en lo publicado (`contiene(publicado, 1373)` falso). Solo falla la última línea, que fija el desenlace |

Ese tercer caso muestra algo que la orden no preveía: la retirada **no depende del límite de dificultad**. También resuelve casos que ya eran NO_FACTIBLE con A (el 1373). Ver 10.

Con `off/off`, **0 fallos**: el comportamiento es el de #38.

## Cobertura de la lista de la fase 9

| Ámbito | Resultado en las tres configuraciones |
|---|---|
| Unitarias e integración | ✔ (salvo las 3 explicadas) |
| Generación inicial (960) | ✔ idéntica |
| REPLAY 100 semanas | ✔ solo N08 cuando el límite está encendido; la retirada no cambia nada: N08 tiene sustituto |
| H1, V8C, V20, V24, 4D | ✔ (`ConservacionPatronH1*`, `V8C*`, `V20ComposicionCasosIT`, `StrictValidationAdapterTest`, `Reincorporacion4DTest`…) |
| Dolor P-02 | ✔ (`PoliticasP123IT`, `MigracionV43IT`, `SinSustitutoDolor*IT`) |
| Respaldo SUST-SEG-1 | ✔ (`RespaldoConDolorIT.A/.B`, `RespaldoCincoSeisDiasIT`…) |
| Regeneración P-03 | ✔ (`RetiradaLocalizada*IT` R0487/R0524, `SinSustitutoDolor*IT`) |
| Progresión y reincorporación | ✔ (`ProgresionIT`, `ProgresionReservaIT`, `ReincorporacionTrasProgresionTest`) |
| Linaje | ✔ (`LinajeIntegridadIT`, `HerenciaD8IT`) |
| Concurrencia e idempotencia | ✔ (`CicloConcurrenteIT`, `PoliticasP123IT`, idempotencia de R0521) |

## N08

Con D, N08 obtiene el mismo resultado que con C:
- la IA propone 124 (dif 2), que se rechaza 3 veces;
- SUST-SEG-1 coloca el **1003** (dif 1);
- RESPALDO/CONSERVED;
- 8 semanas posteriores con el 1003.

**La retirada no altera una sustitución segura que ya funciona**: solo se intenta cuando no hay sustituto.
