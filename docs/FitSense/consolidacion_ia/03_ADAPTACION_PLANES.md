# 03 · Adaptación de planes con IA

**Estado:**
- Se mantiene el prompt de adaptación **V2.1** (`FIX-PR-2.1-EXP`), con el **V8C corregido** (`06_CORRECCIONES_V8C.md`).
- V2.2, v2.2a y v2.4 no se adoptaron (`05_EXPERIMENTOS_MEJORA_PROMPTS.md`).

**Aviso importante:**
- Que una adaptación sea **técnicamente aceptada** (respeta los permisos, V8C y V1–V27) **no es una validación profesional de su calidad deportiva**.
- La «pertinencia» (ACC/ALT/CONS-J/INC) la juzga un oráculo propio del laboratorio, no un entrenador.

Referencias de código: `generation` `543932a`.

## 1. Configuración

| Elemento | Histórica (hasta `543932a`) | Campañas con IA real | Incorporada a `generation` (PR #45) |
|---|---|---|---|
| Prompt de adaptación | v1 = `FIX-PR-1.3` | **V2.1** (control); las variantes solo en `experimento/mejora-ia` | **V2.1 = `FIX-PR-2.1-EXP`** (`FITSENSE_AI_ADAPTATION_PROMPT_VERSION`) |
| IA | Apagada | Encendida desde el arnés | Encendida por defecto (`FITSENSE_AI_ENABLED`) |
| Límite de dificultad | `off` | `preferencia` | `preferencia` (`FITSENSE_DIFFICULTY_GUARD`) |
| Retirada localizada ante dolor | `off` | `on` | `on` (`FITSENSE_PAIN_LOCALIZED_REMOVAL`) |
| V24 por capacidades en la adaptación | Apagada | Solo en el bloque P (va con la progresión) | **Encendida** (va con la progresión, ver `07`) |
| V8C | `multiconjunto` | GRA, MEJ y MEJ R2: anterior a la corrección; CONF-IA: corregido | `multiconjunto`, con H-A1/H-A2 corregidos |

La configuración desplegada no se ha verificado (ver `00` §5).

## 2. Flujo

```
Cierre de la semana → adherencia observada y cobertura de registro
  → AdaptationOrderDecider:
      dolor (sin adaptación por adherencia) → solo la sesión del dolor, SEGURIDAD_ANTE_DOLOR
      cobertura < 50 %                      → INSUFFICIENT_DATA
      adherencia ≥ 80 %                      → NO_CHANGE (posible paso de progresión, ver 07)
      50–80 %                                → ADAPT_LOCALIZED (peor sesión señalada)
      < 50 %                                 → ADAPT_EXPANDED (todas las señaladas)
  → motivo registrado → TipoIntervencion (qué se puede cambiar)
      CAUSA_NO_REGISTRADA / CONSERVAR → sin IA, sesión conservada
      falta de tiempo y la sesión ya cabe → CONSERVAR (FaltaDeTiempoPolicy)
  → factibilidad previa: si es imposible → respaldo sin llamar
  → Replicate (prompt V2.1) → propuesta de las sesiones señaladas
  → V8C (solo cambió lo señalado y lo permitido) + V1–V27
      ├─ válida y con cambios → APLICADA
      ├─ la IA no cambia nada → SIN_EFECTO (la sesión se conserva)
      └─ inválida → reintento con los motivos (hasta 3) → si se agotan, no se publica cambio
```

**Sin respaldo del motor de reglas en la adaptación:** si la IA no da una corrección válida, la sesión no se cambia.

### Permisos por intervención (`TipoIntervencion`)

| Intervención | Escenario típico | Qué permite |
|---|---|---|
| REVISAR_CARGA_Y_RECUPERACION | Fatiga | Bajar dosis o carga, sin sustituir |
| VARIANTE_MAS_SENCILLA_O_CARGA | «Demasiado difícil» | Variante más sencilla **del mismo patrón** o bajar carga. **No retirar** (H-A1) |
| SEGURIDAD_ANTE_DOLOR | Dolor | Sustituir o **retirar sin reemplazo** el ejercicio que dolió; exige revisión de seguridad |
| ACORTAR_O_REORGANIZAR | Falta de tiempo | Bajar dosis o retirar **una** colocación de relleno; única intervención exenta de V20 |
| INVESTIGAR_CAMBIO_MINIMO | Desmotivación, motivo poco concreto | Un cambio pequeño (una opción sobre un ejercicio) |
| CAUSA_NO_REGISTRADA / CONSERVAR | Sin motivo registrado | Nada; no se llama a la IA |
| REPROGRAMAR_DIAS | Cambio de horario | Solo mover el día |

**En todas:**
- la IA no puede subir dosis;
- el descanso de un ejercicio que se queda no cambia (H-A2);
- las sesiones no señaladas se conservan.

**Estados registrados (V32):** APLICADA, SIN_EFECTO, RECHAZADA, RESPALDO, NO_FACTIBLE, SIN_ORDEN. Ante dolor sin alternativa segura: NO_FACTIBLE y regeneración segura (P-03).

## 3. Escenarios de la cohorte O (O01–O20)

| Escenario | Perfiles | Intervención | ¿Llama a la IA? |
|---|---|---|---|
| Fatiga | O03, O14, O17 | REVISAR_CARGA_Y_RECUPERACION | Sí |
| Dificultad | O04 | VARIANTE_MAS_SENCILLA_O_CARGA | Sí |
| Dolor | O06, O12, O20 | SEGURIDAD_ANTE_DOLOR | Sí |
| Falta de tiempo | O18, O19 (sesión parcial por tiempo) | ACORTAR_O_REORGANIZAR | Sí |
| Falta de tiempo con una sesión que ya cabe | O02 | CONSERVAR | No (conservación) |
| Desmotivación | O05 (sesiones 71 y 72) | INVESTIGAR_CAMBIO_MINIMO | Sí |
| Causa no registrada | O05 (sesión 74, parcial sin motivo), O10 | CAUSA_NO_REGISTRADA | No; se conserva |

Fuente: `docs/FitSense/evaluacion_integral_O/01_COHORTE_O_MANIFIESTO.md` y la tabla 4 de INF.

## 4. Resultados con IA real

### 4.1 GRA-R1 y GRA-R2 (06/11/2026, `9f4cd9ec…`, PR-1.7 + V2.1)

Fuente: INF = `evaluacion_integral_O/generacion_ia_real/entregables/INFORME_INTEGRAL_GENERACION_ADAPTACION_IA_REAL.md`, tabla 4 y tabla 4 por ciclo.

| | GRA-R1 | GRA-R2 |
|---|---:|---:|
| Ciclos con orden de IA | 10 | 10 |
| Sesiones adaptadas evaluadas | 12 | 12 |
| Aceptadas al primer intento | 10 | 9 |
| Rechazadas por los validadores | 0 | 0 |
| **SIN_EFECTO** propuesto por la IA | 0 | **1** (O18, ACORTAR; evitable) |
| **Publicadas desde la IA** | **10/10** | **9/10** |
| **Cambios no autorizados** | **0** | **0** |
| Pertinencia ACC / ALT / CONS-J / INC (12 sesiones) | 11 / 0 / 1 / 0 | 9 / 1 / 2 / 0 |
| Latencia mediana (máx.) | 31,9 s (44,3) | 28,7 s (47,7) |
| Coste | USD 0,1367 | USD 0,1312 |

**Qué hizo la IA en cada sesión** (recuento sobre la tabla 4 de INF; una sesión puede combinar tipos):

| Tipo de cambio | GRA-R1 | GRA-R2 |
|---|---|---|
| Sustituciones (mismo patrón) | 3 sesiones: O04 (2 sustituciones), O05-71, O05-72 | 3 sesiones: O04 (4), O05-71, O05-72 |
| Retiradas | 4: O06 (755), O12 (6 ejercicios, dolor), O18 (502, relleno), O20 (709) | 3: O06 (755), O12 (6), O20 (1156) |
| Reducciones de dosis | 5: O04, O03, O14, O17, O19 | 6: O04, O06, O03, O14, O17, O19 |
| Conservación justificada (CONS-J) | 1: O05-74 | 2: O05-74, O18 (SIN_EFECTO) |

**Cumplen todas las adaptaciones publicadas:**
- sin más trabajo ni más dificultad;
- sustitutos del mismo patrón y grupo, salvo en dolor;
- el ejercicio que dolió no vuelve (O06, O20);
- las sesiones no señaladas quedan idénticas;
- linaje correcto: se adapta el plan generado en la misma corrida.

**Observaciones de calidad:**
- **O12 en R2 (ALT):** ante un dolor de hombro sin localizar, la IA retiró los 6 ejercicios de la sesión. Es seguro, pero mucho más drástico de lo necesario.
- **O18 en R2:** SIN_EFECTO evitable, con una justificación incorrecta («no se puede quitar»). Es el caso que motivó V2.2 y v2.2a.

**Otros resultados:**
- **Reproducibilidad:** misma intervención 10/10, misma clasificación 9/10, mismo cambio exacto 0/10.
- **Mantenimiento** (O02, O10, O16): conservaciones sin IA, idénticas en las dos corridas.

### 4.2 Campañas posteriores (versión `045f8102…`)

| Campaña | Condición | Sesiones aplicadas | SIN_EFECTO evitables | Cambios no autorizados | INC por identidad |
|---|---|---|---|---|---|
| MEJ (A0, A1, A2) | V2.1 · V2.2 · V2.2 | 12/12 · 12/12 · 12/12 | 0 | 0 | 0 |
| MEJ ronda 2 (R1–R3) | V2.1 · v2.2a · v2.4 | **35/36** · 36/36 · 36/36 | 1 (O18) · 0 · 0 | 0 en 108 sesiones | 0 |
| CONF-IA (D1–D5, V8C corregido) | V2.1 · v2.2a | **58/60** · 60/60 | 2 (O18 en D1 y D2) · 0 | 0 | 0 |
| CONF-IA flujo conjunto I1 | v2.2a | 12/12 | 0 | 0 | 0 |

**El único fallo repetido de V2.1** es O18: quitar un relleno al acortar.
- En GRA-R2, MEJ R2 y CONF-IA terminó en SIN_EFECTO 4 veces en total.
- **No hay riesgo:** SIN_EFECTO conserva la sesión sin cambios.

**V8C corregido en las corridas reales:** 132 sesiones de CONF-IA sin ningún cambio no autorizado, y una sola propuesta rechazada (D3, O19, v2.2a) cuyo reintento se aplicó.

## 5. Cómo se relaciona con la generación y la progresión

- La adaptación parte **del plan que publicó la generación** (IA o respaldo) y solo toca las sesiones señaladas.
- **No regenera la semana ni sube dosis.**
- La subida de dosis solo la hace el backend con los pasos de progresión (`07_PROGRESION_REINCORPORACION.md`), en ciclos NO_CHANGE y nunca en el mismo ciclo que una adaptación.

## 6. Límites

- La pertinencia la juzga un oráculo del laboratorio. **Falta la revisión de un profesional del deporte.**
- La muestra es pequeña: 10–12 sesiones con IA por corrida. Los porcentajes son descriptivos.
- La adherencia es simulada por guion. **No se puede afirmar que estas adaptaciones mejoren la adherencia de personas reales.**
