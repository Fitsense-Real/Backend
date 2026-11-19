# Referencia de 72 planes de la versión conservadora

## 1. Identificación

| | Histórica (se conserva) | Conservadora (nueva) |
|---|---|---|
| Versión | Oficial hasta la versión conservadora (catálogo hasta V55, **sin** INC-CAT-CONS-v1) | `FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1` (catálogo hasta **V56**, INC-CAT-CONS-v1 **encendida**) |
| Archivo | `docs/FitSense/evidencia/progresion/generacion_referencia.txt` | `docs/FitSense/evidencia/progresion/generacion_referencia_conservador_v1.txt` |
| SHA-256 | `07ce3ad3927d06c3a7cd346761d1500004683a016d5f04c6a9d5eac2256204c0` | `0b52f59ecf7a5469404c02ca0edc94494b61fb29e11d2cada191dab1f2c8e6ce` |
| Prueba que la protege | `GeneracionReferenciaIT` con `fitsense.catalogo.contencion-conservadora=false`; su huella está además en `ReferenciasHistoricasInmutablesTest` | `GeneracionReferenciaConservadoraIT` (contención encendida) |
| Captura | Última vez en `3f92fa4` (V52.1). No se ha sobrescrito | 15/11/2026, sobre el commit candidato (`-Dfitsense.generacion.conservador.capturar=true`) |
| Resultado | 72/72 VALIDA | 72/72 VALIDA |

La referencia histórica **no se ha sobrescrito ni borrado**. Con la contención apagada, el catálogo hasta V56 la reproduce línea por línea (`GeneracionReferenciaIT` pasa). Eso demuestra dos cosas:

1. V56 solo cambia textos.
2. Todos los cambios de la referencia nueva los causa la contención. No hay ningún cambio accidental de política.

## 2. Condiciones de generación (idénticas en las dos)

- **Perfiles (72):** 4 objetivos (LOSE_WEIGHT, GAIN_MUSCLE, INCREASE_STRENGTH, GENERAL_FITNESS) × 3 niveles × 30/45/60 minutos × gimnasio/casa.
- **Participante sintético:** mujer nacida el 15/01/1992, 165 cm y 70 kg, 3 días (lunes, miércoles y viernes), sin restricciones declaradas (las cinco permitidas) y sin limitaciones de salud.
- **Equipamiento:**
  - Gimnasio: peso corporal, mancuernas, barra, polea, máquina guiada, banco, banda, bicicleta estática, cinta, kettlebell, barra EZ y multipower.
  - Casa: peso corporal, mancuernas y banda.
- **Semilla:** la semana fija `RelojPruebas.SEMANA_REFERENCIA = 2026-11-02`. La semilla del motor de reglas depende de la semana y del usuario. Los usuarios se crean en el mismo orden (n = 1…72) sobre una base PostgreSQL 16 vacía.
- **Política:** `PLAN-HIST-1`, prompt de adaptación v1, progresión apagada, límite de dificultad `off`, retirada localizada por dolor `off`, IA apagada (motor de reglas, sin coste).
- **Interruptores:** los de la candidata. V53 A, V53-B y #64 están apagados. P-03 por colocación está apagado.

## 3. Qué cambió (detalle en `COMPARACION_72_PLANES.xlsx`)

- **Planes que cambian:** 72 de 72. Los 72 siguen siendo VALIDA.
- **Estructura:** mismos días en los 72.
  - Duración estimada distinta en 15 planes (±1–3 min en alguna sesión).
  - Número de ejercicios distinto en 7 (±1).
  - El total de series cambia en 11: baja en 7 (como mucho −3, en GAIN_MUSCLE de 45 min) y sube en 4 (+1). Todo dentro de la duración que valida V4/V4-C. El constructor llena el tiempo de la sesión con la duración estimada de cada ejercicio elegido, así que otra elección deja sitio para un puesto más o uno menos.
- **Ejercicios contenidos en la referencia conservadora: 0.**
- **Equipamiento incompatible:** 0. Ningún plan de casa usa equipamiento fuera de peso corporal, mancuernas y banda.
- **Dosis:** ninguna fuera del patrón que la referencia histórica usa para el mismo objetivo y nivel. En 18 planes un mismo ejercicio aparece con otra dosis: la dosis la fija el **puesto** que ocupa en la sesión, no el ejercicio. Por ejemplo, el 136 lleva 2x10r/90 como último ejercicio del lunes y 2x8r/120 como estructural del miércoles.

### Clasificación de las diferencias

| Causa | Planes | Qué significa |
|---|---|---|
| **CONTENCION** | 22 | El plan histórico usaba un ejercicio que ahora está contenido. Al faltar, el motor elige otro y la secuencia aleatoria se desplaza. Contenidos usados en la histórica: **1113** ×9 (solo principiantes), **1176** ×5, **1177** ×4, **494** ×4, **1311**, **310** y **535** ×1 |
| **SELECCION_DETERMINISTA** | 50 | El plan histórico no usaba ningún contenido. Cambia porque cambia el conjunto de candidatos |
| Equipamiento | 0 | El equipamiento no cambió |
| Clasificación corregida | 0 | V56 solo corrige el texto del 853, no su clasificación |
| **INESPERADA** | **0** | Ningún plan no válido, ningún contenido, ninguna dosis fuera de patrón, ningún equipamiento incompatible |

## 4. Por qué cambian los 72, incluidos los 50 que no usaban ningún contenido

El motor de reglas elige con **una sola secuencia aleatoria con semilla**:

- `ExerciseSelector.pickOneFrom`: `random.nextInt(fresh.size())`.
- `CapabilityWeekBuilder`: `Collections.shuffle(opciones, random)` y `random.nextInt(opciones.size())`.

Si una lista de candidatos tiene un elemento menos, el mismo número aleatorio señala otro ejercicio y todas las elecciones siguientes de la semana se desplazan.

La contención quita candidatos **en todos los perfiles**. Por ejemplo, el **1311** (abductor de cadera de pie con pierna recta, peso corporal, dificultad 1) es elegible para cualquiera sin la contención. Así, ningún conjunto elegible de los 72 perfiles es igual al de antes, aunque el plan histórico no usara ningún contenido.

**Comprobación de que no hay otra causa:**

- Con la contención apagada, la generación es idéntica a la histórica: `GeneracionReferenciaIT` pasa sobre el commit candidato.
- En la rejilla de 168, el conjunto elegible cambia en los 168 perfiles (el 1311 en todos) y el plan cambia en 163.

**Primera pasada de la comprobación de dosis:** marcó 2 planes (GENERAL_FITNESS principiante e intermedio, 30 min, casa) con la dosis 3x40s/45, porque no aparecía en el plan histórico **del mismo perfil**. Se investigó:

- Es la dosis por tiempo de los ejercicios 900, 901 y 1077, la misma que les da la referencia histórica en otros perfiles del mismo objetivo y nivel.
- La comprobación se cambió a «patrón de dosis por objetivo y nivel», porque la dosis depende de la clase del ejercicio, el objetivo y el nivel, no del perfil concreto.
- No se cambió ninguna expectativa para obtener verde: los 2 planes ya eran VALIDA para el validador real (V24).

## 5. Validez de los 72

| Criterio | Resultado |
|---|---|
| Planes válidos (validador real, V1–V25) | 72/72 VALIDA |
| Ningún ejercicio contenido (INC-4C-03, INC-CAT-C39, INC-CAT-1374, INC-CAT-CONS-v1) | 0 contenidos de INC-CAT-CONS-v1 (comprobación independiente). Las demás contenciones las aplica el filtro del conjunto elegible y las cubre su propia prueba |
| Equipamiento compatible | Sí (72/72) |
| Cobertura | 72/72 con plan (ningún SIN_PLAN) |
| Dosis no autorizadas | 0 (V24 en el validador y comprobación independiente del patrón) |
| Cambios explicados | 72/72 (22 CONTENCION + 50 SELECCION_DETERMINISTA) |

La referencia **no se actualizó para obtener pruebas verdes**: se guardó aparte, con su propia prueba, y la histórica sigue protegida.

## 6. Reproducir

```
# referencia conservadora (contencion encendida, el valor por defecto)
sh mvnw -o verify -Pintegration -Dtest=NONE -Dit.test=GeneracionReferenciaConservadoraIT \
  -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false
# referencia historica (la misma generacion con la contencion apagada)
sh mvnw -o verify -Pintegration -Dtest=NONE -Dit.test=GeneracionReferenciaIT ...
# comparacion plan a plan
python3 -I -B docs/FitSense/catalogo_integral/cierre_conservador/herramientas/comparar_72.py . docs/FitSense/catalogo_integral/cierre_conservador
```
