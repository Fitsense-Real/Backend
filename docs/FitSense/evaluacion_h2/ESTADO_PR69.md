# Estado de la PR #69 (H2)

| | |
|---|---|
| PR | [Fitsense-Real/Backend#69](https://github.com/Fitsense-Real/Backend/pull/69), **borrador**, sin fusionar |
| Rama | `fix/h2-contrato-prompt-v15` |
| Base | `generation` (`2578719`, catálogo v1.1 integrado, V57 y documentación), integrada por merge (`ff61681`) sin reescribir historia |
| Interruptor | `fitsense.ai.generacion.contrato-v15` (`FITSENSE_AI_GENERACION_CONTRATO_V15`), **false** por defecto |
| Merge | **No autorizado** (orden 3). No se fusiona ni se despliega |

## Commits de esta orden

| Commit | Contenido |
|---|---|
| `ff61681` | Merge de `generation`. Único conflicto: `application.properties`, donde se conservan las dos propiedades apagadas (P-03 por colocación y `contrato-v15`) |
| `756770b` | La adaptación deja de leer el interruptor. `adaptacionNoLeeElInterruptor` y la IA simulada en los cuatro objetivos (`iaSimuladaCuatroObjetivos`) |
| `406c2f3` | Arnés `EvaluacionPareadaH2IT` (opt-in) y tope duro opcional en `CampanaReplicateRealIT` |
| (este) | Documentación `docs/FitSense/evaluacion_h2/` y matrices |

## Qué cambia en producción con el interruptor apagado

**Nada.** El prompt y la instantánea son idénticos byte a byte a PR-1.7 (`ContratoV15PromptTest#apagadoIdentico`, `ConfiguracionEvaluadaTest`). Los arneses nuevos son código de prueba y se omiten sin sus propiedades.

## Pruebas

En local:

- **1548 unitarias**, 0 fallos;
- `ContratoPromptV15IT` (2/2);
- pruebas de prompt y de configuración;
- ensayo del arnés.

El CI de GitHub ejecuta la suite completa en cada cabeza (`ESTADO_PR69.md` se actualiza con el resultado).

## ¿Lista para revisión?

**Sí, como revisión técnica.** El código está completo, probado y apagado.

**No para activar ni fusionar como decisión de producto.** Activar PR-1.9 cambia la configuración evaluada y requiere:

1. tu autorización de la fase pagada (`PRESUPUESTO_REPLICATE.md`);
2. la evaluación pareada con un resultado **favorable** según `CRITERIOS_ACEPTACION_H2.md`;
3. una decisión aparte para actualizar `ConfiguracionEvaluadaTest`.

Sigue como **borrador** hasta entonces.
