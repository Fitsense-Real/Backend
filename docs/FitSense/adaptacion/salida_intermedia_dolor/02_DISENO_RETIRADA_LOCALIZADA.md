# 02 · Diseño de la retirada localizada

**PR:** #39 (`experiment/pain-localized-removal`, borrador), apilado sobre #38.

La dependencia es técnica: sin el límite de dificultad no hay casos «sin sustituto compatible» del 135, y la retirada usa la estrategia de SUST-SEG-1 de #38.

**Interruptor:** `fitsense.adaptation.pain-localized-removal` (env `FITSENSE_PAIN_LOCALIZED_REMOVAL`).

| Valor | Efecto |
|---|---|
| **`off` (por defecto)** | El comportamiento de #38, sin ningún cambio. |
| `on` | Se activa la retirada localizada. |
| Cualquier otro | La aplicación no arranca. |

**No cambian:**
- migraciones;
- el JSON de `conservation_report` (no hay claves nuevas);
- la API (no hay valores nuevos de `resultado`);
- V20, V24, 4D;
- adherencia;
- catálogo;
- generación inicial.

## Flujo

```
P-02 registra el dolor (exclusión ACTIVA, DOLOR_IDENTIFICADO)
  └─ adaptación ordenada (SEGURIDAD_ANTE_DOLOR)
      ├─ IA (si está activa) — sin cambios
      └─ RESPALDO con dolor cubierto por P-02 (AdaptedWeekProducer.respaldo)
          ├─ continuidad + SUST-SEG-1 ............................ publica si hay sustituto (como siempre)
          └─ NO_FACTIBLE con SIN_SUSTITUTO_SEGURO
              ├─ interruptor off ................................ NO_FACTIBLE (como siempre)
              └─ interruptor on → retirar()                       [NUEVO]
                  1. la misma copia reparada (SUST-SEG-1 sustituye lo que puede)
                  2. retira SOLO lo excluido por dolor registrado que siguió sin sustituto,
                     y SOLO de sesiones señaladas por dolor; minutos = estimador
                  3. V8C de continuidad con las RetiradaSeguridad declaradas
                  4. validación estricta entera (V1–V25, V20, V24/4D, V23…)
                  5. el trabajo de las sesiones del dolor no sube
                  ├─ todo pasa → PUBLICABLE (RESPALDO / CONSERVED) + nota RETIRADA_SEGURA
                  └─ algo falla → el NO_FACTIBLE original + «RETIRADA_LOCALIZADA no aplicable: <motivo>»
  └─ la fachada: si no se publicó → P-03 nivel 2 (SafeRegenerationService) SIN CAMBIOS
      └─ si P-03 tampoco puede → NO_FACTIBLE, sin plan inválido
```

## Qué autoriza V8C (`neutralizarRetiradas`)

Una `RetiradaSeguridad` solo deja de contar como diferencia si se cumplen todas estas condiciones:

1. El retirado estaba en esa sesión de la semana medida.
2. Su causa verificada (`ExclusionesDeSeguridad`) contiene **`dolor_registrado`**. Un bloqueo, el equipamiento o una restricción declarada **no** autorizan a retirar sin sustituto.
3. El motivo declarado coincide con la causa verificada.
4. Ya no es elegible y no está en la sesión nueva.
5. La sesión **no añade nada, no quita nada más y no cambia ninguna otra dosis**, salvo las sustituciones SUST-SEG-1 ya admitidas.
6. Los minutos no suben.
7. Si era una colocación **estructural**, algún otro ejercicio de la semana nueva cubre su slot.
8. No se combina con un paso de progresión en la misma semana.
9. Una sesión con retirada pierde las exenciones D8 de V20: solo una copia idéntica las conserva.

Además, la retirada no se intenta en tres situaciones:
- **sesión no señalada:** el excluido aparece también en una sesión **no señalada** por dolor; esa sesión no se toca por esta vía;
- **otra causa:** queda sin sustituto algo cuya causa no es el dolor;
- **sesión vacía:** la sesión quedaría sin ejercicios.

En esos casos se pasa directamente a NO_FACTIBLE y P-03.

## Trazabilidad (fase 5)

| Dónde | Qué queda | Por qué así |
|---|---|---|
| V31 (linaje) | Cada sesión nueva conserva `source_planned_workout_id` (6/6 en los tres casos publicados) | Es una copia; no se regenera |
| V32 `status` / `generation_source` | `RESPALDO` / `CONSERVED` | V33 impone `APLICADA ⇔ applied_sessions no vacío`, y `APLICADA` es solo para correcciones de la IA. La retirada **no** es una corrección de la IA |
| V32 `no_effect_sessions` | La sesión señalada figura aquí | Semántica **documentada** del mapper: en RESPALDO, «sin efecto» son las señaladas que **la IA** no aplicó (`AdaptationEvidenceMapper`, «una sola fórmula vale para APLICADA, SIN_EFECTO y RESPALDO»). Es la misma con la que se registra hoy una sustitución SUST-SEG-1 (N08) |
| `conservation_report.correcciones_del_backend` | `RETIRADA_SEGURA: sesiones [61]: sin sustituto SUST-SEG-1 compatible, se retira [135] (dolor registrado) y se conserva el resto; minutos [61:34->27]` | Lista ya existente, sin contrato nuevo |
| `conservation_report.conservadas` | Incluye la sesión | Es una copia con una excepción autorizada, como la sustitución |
| Exclusiones por dolor | El 135 sigue `ACTIVA` | No se toca P-02 |
| Historial de intentos | 1 intento V32; ninguno en `safe_regeneration_attempts` (P-03 no hizo falta) | Si la retirada no se aplica, el motivo queda en `rejection_reason` del NO_FACTIBLE |
| API `GET /users/me/plan/adaptations` | `resultado = RESPALDO_SIN_IA`, o `RESPALDO_PROPUESTA_IA_INVALIDA` si hubo IA | Son valores existentes y no inventan nada, pero **no dicen «retirada»** (ver propuesta) |
| Texto publicado (`adjustment_reason`) | «Reportaste molestias. Por seguridad, cambiamos o retiramos 1 ejercicio. Si la molestia continúa, consulta a un profesional de la salud.» | `DescripcionAdaptacion`: con la nota `RETIRADA_SEGURA`, la sesión que cambió se describe como cambio de seguridad y **no** como «no aplicamos cambios». Sin la nota, el texto es el de siempre |

### Propuestas de contrato

No se han implementado; la orden pide presentarlas primero.

| # | Propuesta | Efecto | Necesidad |
|---|---|---|---|
| P1 | Valor de API `resultado = RETIRADA_SEGURA` cuando la nota esté presente | Flutter tendría que conocerlo (hoy mostraría el genérico de respaldo) | Media: `RESPALDO_SIN_IA` es cierto, pero poco informativo |
| P2 | Clave estructurada `retiradas` en `conservation_report` (como `sustituciones`) | JSON V32 con una clave nueva; sin migración (es JSONB), pero es un contrato persistido nuevo | Baja: la nota textual basta para auditar |
| P3 | Corregir el texto publicado también para la **sustitución** SUST-SEG-1 en RESPALDO | Según el código de `DescripcionAdaptacion.clasificar` (en RESPALDO, `aplicadas` está vacío), la sesión cuenta como NO_APLICADA y el texto dice «En una de las sesiones no aplicamos cambios», aunque se sustituyó el ejercicio (como en N08 con el límite). Es lectura de código; no se ha verificado con una prueba en esta orden | **Hallazgo previo**, no de esta orden; lo cambiaría también con la configuración por defecto, así que no se toca sin aprobación |
