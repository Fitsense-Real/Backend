# 01 · Versión candidata de la evaluación K20

## Qué se evalúa

**Rama:** `eval/final-ieee-k20`, creada desde `generation` `76e15c5`. **No se fusiona en `generation`.**

PR [#40](https://github.com/Fitsense-Real/Backend/pull/40): borrador abierto solo para obtener la CI.

| Commit | Contenido | ¿Toca `src/main`? |
|---|---|---|
| `1488dc1` | Merge de #39 (`experiment/pain-localized-removal`, que contiene #38) | Sí: límite de dificultad y retirada localizada, apagados por defecto |
| `7ee69c2` | Corrección P3 y su prueba permanente | Sí: una línea en `DescripcionAdaptacion` |
| `6098584` y siguientes | Arnés `EvaluacionK20IT`, oráculo, documentación y evidencia | **No** (prueba y documentación) |

El **SHA congelado** se fija en `02` §8, en el commit que contiene este documento, el protocolo y el oráculo.

**Diferencias con `generation` en `src/main`** (8 ficheros, +573 / −33):
- `ConservationValidator`, `SafetySubstitutionPolicy` y `PublishedPlanSafetyRepairService`: el límite de dificultad (#38).
- `AdaptedWeekProducer`, `RetiradaSeguridad` y `AdaptationWiringConfiguration`: la retirada localizada (#39).
- `DescripcionAdaptacion`: los textos (#39 y P3).
- `application.properties`: los dos interruptores, en `off`.

**Sin migraciones nuevas:** 46, la última `44.2`, con checksum Flyway `9981294d…`. **Catálogo sin cambios:** 1.219 ejercicios activos, huella `425fc958…`.

## Estado de cada pieza

| Pieza | Estado real | En la candidata |
|---|---|---|
| `generation` | `76e15c5`, con #36 (H1) y #37 (textos) | Base |
| #38 · límite de dificultad | Borrador; CI verde en su rama | **Sí**, estrategia C (`preferencia`) |
| #39 · retirada localizada | Borrador; CI verde (`ab8c971`, `a6af02d`, `2b9bd95`) | **Sí**, `on`, y solo actúa con el límite |
| P3 · texto del respaldo con sustitución | Defecto confirmado (`TextoRespaldoSustitucionIT`) | **Sí, corregido**. Alcance: solo las sesiones con dolor (`SEGURIDAD_ANTE_DOLOR`) que el respaldo cambió sin la IA. Ninguna otra intervención exige revisión de seguridad |
| H1 · V8C multiconjunto | Integrado (#36) | Sí, por defecto |
| Textos verificables | Integrado (#37) | Sí |
| Prompt V1 | Único en la rama (`SessionFixPromptBuilder`, blob `91fc686d`) | **Sí** |
| Prompt V2 | Solo en `experiment/adaptation-prompt-v2` | **No** |
| H12 | Pendiente de decisión funcional | **No** |
| Progresión y reincorporación | Código presente; progresión apagada | Progresión `false`. La reincorporación 4D sigue activa, como en `generation` |
| V8C, V20, V24, 4D | Sin cambios | Sin cambios |

## Configuración efectiva

La fija el arnés en `@SpringBootTest(properties)`, que tiene prioridad sobre `-D`. El arnés comprueba al arrancar que el límite y la retirada están activos.

| Propiedad | Valor |
|---|---|
| `fitsense.adaptation.difficulty-guard` | `preferencia` (`LIMITE_Y_PREFERENCIA`) |
| `fitsense.adaptation.pain-localized-removal` | `on` (activa con el límite) |
| `fitsense.adaptation.progression.enabled` | `false` |
| `fitsense.adaptation.v8c.pattern-rule` | `multiconjunto` (por defecto) |
| Política | `PLAN-HIST-1` (por defecto) |
| Configuración de cálculo | `MVP-1.8` (activa en V7):<br>• umbrales 80 / 50;<br>• cobertura mínima del 50 %;<br>• piso V20 del 70 %;<br>• tolerancia V17 del 20 % |
| IA en D y D′ | `fitsense.ai.enabled=false` |
| IA en R y R′ (pendiente) | `gpt-5-mini` sobre `openai/gpt-5-structured`, esfuerzo `medium`, `prefer-wait=25`, 3 intentos, prompt V1 |

Fuera del arnés (CI y uso normal), los interruptores siguen en `off`: el comportamiento es el de `generation` más P3.

## Restricciones conocidas

1. **Generación inicial.** R0548 (GM intermedio, casa, 7 días, 75 min) no genera un plan válido por V20 (faltan 600 s). No afecta a U01–U20, pero existe; ver `13`.
2. **Retirada localizada.** Recupera pocas semanas. Con el límite, 5 de los 21 perfiles reproducidos del PR #39 siguen sin plan por V20. No afecta a U01–U20: ninguno tiene dolor.
3. **GAIN_MUSCLE.** La dosis 4D/V24 es fija y el piso V20 impide quitar series en 60 minutos. Ante fatiga o motivación, no admite ninguna modificación simple (U03 S3 y U04 S3). Lo mide la sonda (`07`).
4. **`FactibilidadPreviaIA`.** Solo demuestra imposibilidades de ACORTAR y REPROGRAMAR. En U03, R llamaría a la IA aunque no hubiera ninguna modificación posible: es coste, no un error.
5. **Sin ejecución en Railway ni en Neon.** Todo corre en PostgreSQL efímero (Testcontainers).
