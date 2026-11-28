# Evidencia de regresión (V46 como referencia)

**Commits de esta orden** (rama `investigacion/catalogo-v46`):

| Commit | Contenido |
|---|---|
| `254f02a` | P15-C experimental (desactivado por defecto) |
| `f1c6c02` | Aviso E2-a y contrafactuales |
| Posteriores | Solo documentos, evidencia y SQL de medición |

## 1. Con P15-C apagado (por defecto) y el aviso E2-a, todo es idéntico a V46

| Prueba | Resultado | Referencia V46 |
|---|---|---|
| Barrido REV2, 3.780 perfiles (`254f02a`) | **Idéntico byte a byte** (`planes.csv` y `barrido_generacion.csv`). SHA-256 de `planes.csv`: `4b2994ca…` en ambos | `reg46/barrido_PLAN-REV-2.0` |
| Barrido HIST, 3.780 perfiles (`254f02a`) | **Idéntico byte a byte** | `reg46/barrido_PLAN-HIST-1` |
| K20 REV2 D, U (`f1c6c02`) | `a662cb4d`, 20 PASS | `a662cb4d` |
| K20 REV2 D, N | `11f51d44`, 19 PASS + N19 | `11f51d44` |
| K20 HIST D, U | `f9f67973`, 20 PASS (**huella congelada**) | `f9f67973` |
| K20 REV2 IA simulada V2.1, U | `84dfb6d2`; protocolo 23: 14/14 CUMPLE, 0 cambios no pertinentes | `84dfb6d2` |
| K20 REV2 IA simulada V2.1, N | `da34db35`; ídem | `da34db35` |
| Unitarias completas | **1.306 / 0 fallos** (1 omitida): las 1.301 de V46 + 2 de `CoberturaDePatronTest` + 3 de `PlanLimitationsDosisNoDemostradaTest` | 1.301 / 0 |
| Integración completa (`verify -Pintegration`, HIST por defecto) | **593 / 0 fallos / 0 errores** (44 omitidas: laboratorios opcionales) | 593 / 0 (44) |

**Qué cubre esta batería:**
- **generación inicial:** 4 objetivos × 3 niveles × 3 equipamientos × 1–7 días × 15 duraciones;
- **dosis y patrones:** V20, V24, V25 y dosis en la traza;
- **adaptación localizada y V8C:** K20 con IA simulada, más las IT de adaptación;
- **progresión y reincorporación:** `ProgresionRev2IT`, `PendientesProgresionRev2IT`, `ReparacionLinajeRev2IT` y el resto de la integración;
- **linaje e integridad de planes históricos:** IT de linaje y huella HIST congelada;
- **conservación de HIST:** barrido byte a byte y huella `f9f67973`.

**Sin cambios en:** V20, V24, V25, V8C, los oráculos, el prompt, las políticas históricas y la restauración automática (sigue sin existir).

## 2. Con cada variante

Sobre el código de esta orden: P15-C a través del interruptor; el resto, con SQL en la base efímera.

| Comprobación | Resultado |
|---|---|
| Planes válidos perdidos | **0** en todas las variantes y en ambas políticas, salvo **CF5a/HIST: 24** (V20; no se relaja) |
| V8C y criterios del protocolo 23 con IA simulada (U y N) | 14/14 CUMPLE y 0 cambios no pertinentes en las 6 configuraciones medidas |
| HIST D U (oráculo) | 20 PASS en todas |
| Principiantes con dificultad > 1 o alto impacto | 0 en todas |
| Planes publicados | No se tocan: las variantes solo existen en bases efímeras de prueba |

## 3. Reproducir

**Barrido de una variante:**
```
sh ./mvnw -o -B -ntp verify -Pintegration -DskipUTs=true -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false \
  -Dit.test=BarridoGeneracionInicialIT -Dfitsense.barrido.generacion=true -Dfitsense.barrido.semillas=0 \
  -Dfitsense.barrido.dg3=true -Dfitsense.planning.policy.allow-candidates=true -Dfitsense.planning.policy=PLAN-REV-2.0 \
  [-Dfitsense.experimental.p15c.aislados-sin-cobertura=true] \
  [-Dfitsense.barrido.sql-previo=$PWD/docs/FitSense/auditoria_catalogo/v46/contrafactuales/<VARIANTE>.sql] \
  -Dfitsense.barrido.salida=<carpeta nueva>
```

**Comparación:**
```
python3 -I docs/FitSense/auditoria_catalogo/v46/decisiones/comparar_decisiones.py <catalogo.tsv> <salida> \
  <V46_REV2> <V46_HIST> NOMBRE=<dir_REV2>,<dir_HIST> ...
```
El catálogo TSV se regenera con `ExportarCatalogoIT`; no se versiona porque contiene enlaces a GIF con copyright.
