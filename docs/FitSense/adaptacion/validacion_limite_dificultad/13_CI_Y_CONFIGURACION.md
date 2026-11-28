# 13 · CI y configuración

## PR #38 (`experiment/difficulty-guard`)

- Estado: **borrador**, sin fusionar.
- Base: `generation` (`76e15c5`, sin cambios durante la validación).

| Commit | Contenido | CI (`verify`) |
|---|---|---|
| `a2ca0e9` | estado al empezar | ✔ run 41728591246 |
| `f9d4fd5` | SUST-SEG-1: etapas y estrategias A/B/C (apagado) | — (subido junto con los siguientes) |
| `65878c2` | pruebas de extremo a extremo del 135 sin sustituto | — |
| `c527ccd` | laboratorio del barrido A/B/C | — |
| `dea906a` | pruebas TOO_DIFFICULT 1-15 | — |
| `2385b64` | pruebas de dolor A-I y continuidad con C (**SHA de la regresión local, 12**) | — |
| `41cfd4e`, `a12c318` | laboratorio de 22 perfiles (1.ª versión) y borradores | ✔ run 41744908922 (`a12c318`) |
| `91613f2`, `f4dd02d` | laboratorio de 22 perfiles (semana y semilla del barrido), entregables y evidencia | ver abajo |

**CI sobre el SHA final:** se completa en el último commit de este documento (ver el informe final de la sesión). El job es `verify`: `mvnw verify -Pintegration` con la configuración por defecto (`off`).

## Configuración

| Propiedad | Variable de entorno | Por defecto | Efecto |
|---|---|---|---|
| `fitsense.adaptation.difficulty-guard` | `FITSENSE_DIFFICULTY_GUARD` | **`off`** | A: comportamiento de `generation` |
| | | `on` | B: V8C rechaza sustitutos más difíciles (VARIANTE por patrón, dolor entre todos) y SUST-SEG-1 filtra `DIFICULTAD_NO_MAYOR` |
| | | `preferencia` | C: lo mismo que B y, entre los admitidos, igual dificultad antes que menor (detrás de zona, patrón y músculo) |
| | | otro valor | la aplicación **no arranca** (`IllegalArgumentException`), para evitar un valor mal escrito que deje el límite apagado sin avisar |
| `fitsense.adaptation.v8c.pattern-rule` | `FITSENSE_V8C_PATTERN_RULE` | `multiconjunto` | H1, sin cambios |

**Activar (cuando se autorice):** `FITSENSE_DIFFICULTY_GUARD=preferencia` en el entorno y reinicio. No requiere migración.

**Revertir:** `FITSENSE_DIFFICULTY_GUARD=off`, o quitar la variable, y reinicio. Con `off` el comportamiento es el de `generation`. Lo verifican:
- los hashes de los 960 perfiles;
- la REPLAY;
- el barrido, con 0 diferencias.

Lo que el límite publicó mientras estuvo activo (sustitutos de menor o igual dificultad, semanas regeneradas) queda como historial normal: no hay datos que deshacer.

**Nada desplegado:** ni Railway, ni Neon, ni migraciones remotas, ni Replicate.
