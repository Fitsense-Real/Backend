# 03 · Evidencia real con `openai/gpt-5-mini` (Replicate)

**Condiciones comunes:**
- **Código:** rama `feature/replicate-gpt-5-mini`; cada corrida registra su commit en `00_configuracion.txt`.
- **Camino:** el backend real (Spring Boot, PostgreSQL 16 efímero), recorrido por el arnés `EvaluacionK20IT` en modo integral, con generación con IA (`generacion-ia=SI`). Es el mismo protocolo que GRA.
- **Cohorte O** sin modificar: `O_PERFILES.csv`, sha256 `9b996825…`, comprobado por el arnés.
- **Configuración:** política `PLAN-REV-2.0`, adaptación V2.1.
- **Credencial:** la inyecta el proxy de red y no está en el repositorio ni en los registros.

`00_configuracion.txt` de cada corrida registra el modelo **cargado**. Por ejemplo:

```
modelo_ia: gpt-5-mini formato=direct endpoint=https://api.replicate.com/v1/models/openai/gpt-5-mini/predictions reasoning_effort=low verbosity=low max_completion_tokens=16000
```

## A. Generación real (corrida `G5M_SMOKE_O02`)

O02 es LOSE_WEIGHT, BEGINNER, en casa, 30 min y 2 días.

| Paso | Evidencia |
|---|---|
| Predicción creada | `w7fq48e6nsrzw0d216kawxb9cm` (intento 1), respuesta en 19,9 s: `REPLICATE_TIMING … resultado=SUCCEEDED` |
| Respuesta interpretada | JSON del contrato, 3.697 caracteres (`REPLICATE_CONSUMO … caracteres_salida=3697`) |
| Validadores | **Rechazada por V24**: «el ejercicio 1096 del 2026-11-03 no lleva la dosis de ninguna capacidad de LOSE_WEIGHT. Usa exactamente una de: [STRENGTH 2-3x8, descanso 120 s]» y dos más |
| Reintento con retroalimentación | `yt7hcy8m65rzw0d216ktdpvpew` (intento 2): **válida** |
| Publicación | `GENERACION_TIEMPOS user=1 semana=1 origen=IA motivo_respaldo=- intentos_ia=2 ia_ms=[20250, 15690] … total_ms=36003`. El plan se publica con `generation_source=AI` y `model_name=gpt-5-mini`, **sin respaldo** |
| Oráculo v1.4 | O02: A1 APROBADO («propuesta de la IA válida (Replicate)»), A2 y A10 APROBADO |

**Lo que muestra:**
- El modelo **no** se aceptó solo porque su salida fuera JSON. La primera respuesta era JSON válido y la rechazó V24.
- Publicó la segunda, después de pasar V1–V27.

En las corridas completas (`04`):
- **32 perfiles-corrida (13 con `low`, 18 con `medium` y 1 en la prueba de humo)** generaron con la IA y publicaron planes válidos;
- **0 propuestas inválidas** llegaron al usuario;
- **todo respaldo** quedó registrado como `RULE_ENGINE`.

## B. Adaptación real

Son **15 adaptaciones con llamada real** a la IA. Cada una pasó por V2.1 → `SessionFixFromJsonAssembler` → V8C y los demás controles.

| Corrida | Perfil / ciclo | Predicción | Intervención pedida | Resultado |
|---|---|---|---|---|
| G5M_G | O03/c1 | `gcnfqgdqn5rzw0d216rajxrb68` | REVISAR_CARGA_Y_RECUPERACION | APLICADA, AI |
| G5M_G | O04/c1 | `35h5gq0rmhrzr0d216s8shw0sm` | VARIANTE_MAS_SENCILLA_O_CARGA | APLICADA, AI (ver `05` H-2) |
| G5M_G | O05/c1 | `a71ty9w9b5rzy0d216ssz8w9kr` | INVESTIGAR_CAMBIO_MINIMO ×2 y CAUSA_NO_REGISTRADA (ADAPT_EXPANDED) | APLICADA: 2 sesiones aplicadas y 1 sin efecto |
| G5M_G | O06/c1 | `ad8esnvth9rzr0d216tac131h0` | SEGURIDAD_ANTE_DOLOR | APLICADA, AI |
| G5M_G_COMPLEMENTO | O12/c1 | `e4kge9rjjdrzy0d2172sabzmzr` | SEGURIDAD_ANTE_DOLOR | APLICADA, AI |
| G5M_G_COMPLEMENTO | O14/c2 | `4m9xzpdzthrzt0d2173bc5jc8g` | REVISAR_CARGA_Y_RECUPERACION | APLICADA, AI |
| G5M_P | O17/c1 | `kyh045w56nrzr0d216y8mh75br` | REVISAR_CARGA_Y_RECUPERACION | APLICADA, AI |
| G5M_P | O18/c1 | `rfdjfbm5nhrzw0d216ysn104a0` | ACORTAR_O_REORGANIZAR | APLICADA, AI |
| G5M_P | O19/c1 | `snjjgeac85rzy0d216z8y1xn1g` | ACORTAR_O_REORGANIZAR | APLICADA, AI |
| G5M_P | O20/c1 | `r6mc1ntb4xrzr0d216ztagzth0` | SEGURIDAD_ANTE_DOLOR | APLICADA, AI |
| G5M_MEDIUM_G | O03/c1 | `ds035x1wvxrzt0d217grp818sc` | REVISAR_CARGA_Y_RECUPERACION | APLICADA, AI |
| G5M_MEDIUM_P | O17/c1 | `cz8vmrcqjnrzr0d217wsa44wzr` | REVISAR_CARGA_Y_RECUPERACION | APLICADA, AI |
| G5M_MEDIUM_P | O18/c1 | `rapspt23nsrzt0d217xtkbtg1c` | ACORTAR_O_REORGANIZAR | **SIN_EFECTO**, se conserva la sesión |
| G5M_MEDIUM_P | O19/c1 | `5kgp26kpznrzy0d217y8jz1388` | ACORTAR_O_REORGANIZAR | APLICADA, AI |
| G5M_MEDIUM_P | O20/c1 | `47534zs0c5rzy0d217zbk63g80` | SEGURIDAD_ANTE_DOLOR | APLICADA, AI |

**Ejemplo, adaptación con `low`, O17, ciclo 1:**
- **Petición:** el backend ordena `ADAPT_LOCALIZED` con `REVISAR_CARGA_Y_RECUPERACION` sobre la sesión 38.
- **Respuesta:** gpt-5-mini propone las correcciones en 12,0 s.
- **Validación:** V8C las acepta.
- **Resultado:** se publica el plan siguiente con `applied_sessions={38}` y las demás sesiones conservadas (`plan_adaptation_attempts`, intento con `ai_calls=1`).

**Secuencia de políticas sin cambios** (adaptación → cumplimiento sostenido → progresión gradual → reincorporación → mantenimiento):
- **Progresión:** las semanas con cumplimiento sostenido del bloque P producen pasos `PROGRESION` deterministas de +1 repetición, dentro del techo. Por ejemplo, O20: 13 → 14 → 15 repeticiones con `techo=129`.
- **Reincorporación:** el oráculo la evalúa en A8. En O20, «excluidos por dolor [537] no vuelven» es el comportamiento esperado de la regla de seguridad.
- **Restauraciones:** no se produjo ninguna automática ni ningún aumento sin autorización. A7 y A8 no tienen FALLO en ninguna corrida.

## C. Llamadas fallidas

En todas las corridas reales:
- `REPLICATE_TIMING`: **0 POST_ERROR, 0 TIMEOUT, 0 NOT_SUCCEEDED, 0 INVALID_RESPONSE y 0 POLL_ERROR**;
- **0 respuestas** que no se pudieran interpretar como JSON del contrato.

Los únicos «fallos del proveedor» registrados son los bloqueos **del presupuesto del laboratorio** (`PRESUPUESTO_AGOTADO … no se llama a Replicate`, ver `05` I-1/I-2). Esos perfiles pasaron al motor de reglas y nunca se contaron como éxitos de la IA.
