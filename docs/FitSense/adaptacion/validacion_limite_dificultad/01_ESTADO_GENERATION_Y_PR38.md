# 01 · Estado de `generation` y del PR #38

Fecha de verificación: 2026-11-03/04.

**Entorno:** sin Railway, sin Neon y sin Replicate. Solo PostgreSQL efímero (Testcontainers, `postgres:16-alpine`).

## `generation`

| # | Comprobación | Resultado |
|---|---|---|
| 1 | SHA actual | **`76e15c5`**, sin cambios desde la integración: `git fetch origin generation` → `76e15c57be2213a3dec1ea5b78c65d082886d853` |
| 2 | PR #36 y #37 fusionados | **Sí:**<br>• #36 (H1 limpio) con merge commit `e4344aa`;<br>• #37 (textos) con merge commit `76e15c5`, tras actualizar la rama con `836babb` |
| 3 | CI final | **Verde** en `76e15c5`: runs 41724307111 (push) y 41724319831 |
| 4 | H1 activo por defecto | **Sí:** `fitsense.adaptation.v8c.pattern-rule=${FITSENSE_V8C_PATTERN_RULE:multiconjunto}`. Lo comprueba `ConservacionPatronH1IT.reglaPorDefecto` |
| 5 | Textos integrados | **Sí:**<br>• `DescripcionAdaptacion` (`adjustment_reason`, el resultado);<br>• `MensajeDeIntervencion` (`message_shown`, la intención) |
| 6 | V2 desactivado / no incorporado | **No incorporado:** no existe `FactibilidadAdaptacion` ni propiedad de prompt V2 en `generation` |
| 7 | H12-A/B/C/D | **No incorporadas:** no hay propiedades H12 en `generation`. La igualdad de dificultad sigue permitida |

## PR #38 (`experiment/difficulty-guard`, borrador)

| # | Comprobación | Resultado |
|---|---|---|
| 8 | SHA al empezar | `a2ca0e9`; CI verde, run 41728591246 |
| 9 | SHA final | ver 13 |
| 10 | Evidencia anterior intacta | **Sí.** `docs/FitSense/limite_dificultad/` y su `evidencia/` no se tocan; las evidencias nuevas van solo a esta carpeta. La REPLAY reescribe `docs/FitSense/campana_p123/evidencia/*` al ejecutarse, y se restaura con `git checkout -- docs` tras cada corrida: su diff se guarda aparte en `evidencia/`. |

## Commits de esta validación

Todos van sobre `a2ca0e9` y están separados como pide la fase 10. Con `off`, ninguno cambia el comportamiento.

| Commit | Contenido | Categoría de la orden |
|---|---|---|
| `f9d4fd5` | SUST-SEG-1: etapas con nombre (`Etapa`, `descarte`) y estrategias A/B/C (`Estrategia`); `fitsense.adaptation.difficulty-guard=off\|on\|preferencia` | protección de dificultad + selección determinista |
| `65878c2` | Pruebas de extremo a extremo del 135 sin sustituto (off/on, 2 semanas) | pruebas de ausencia de sustituto |
| `c527ccd` | Laboratorio opt-in: barrido de 19.562 colocaciones A/B/C con descarte por etapa | medición |
| `dea906a` | Pruebas TOO_DIFFICULT 1-15 | protección de dificultad |
| `2385b64` | Pruebas de dolor A-I y continuidad del 135 también con C | pruebas de dolor + continuidad |
| (siguiente) | Laboratorio de los 22 perfiles y entregables | continuidad + documentación |

Cambios de producción respecto de `generation`: 5 ficheros, +215/−26.

| Fichero | Cambio |
|---|---|
| `ConservationValidator` | +112 |
| `SafetySubstitutionPolicy` | +119 |
| `AdaptedWeekProducer` | 4 líneas: pasa la estrategia |
| `PublishedPlanSafetyRepairService` | 2 líneas: pasa la estrategia |
| `application.properties` | +4 |

**Ninguna migración, ningún cambio de catálogo, ni de V20, V24, 4D o adherencia.**

## Configuración

| Propiedad | Por defecto | Valores |
|---|---|---|
| `fitsense.adaptation.difficulty-guard` (env `FITSENSE_DIFFICULTY_GUARD`) | **`off`** (A, el comportamiento de `generation`) | `on` (B), `preferencia` (C); cualquier otro valor **impide arrancar** (`IllegalArgumentException`) |
| `fitsense.adaptation.v8c.pattern-rule` | `multiconjunto` (H1) | `conjunto` (reversión de H1) |
