# Auditoría de V4: redondeos y duración declarada

**V4 no se ha modificado.** La versión corregida existe solo como candidata experimental (`TechoV4Versionado`), sin cablear ni activar.

## 1. Origen del problema

V4 dice: «ninguna sesión excede M + 15 %». Hoy hay **tres** cálculos distintos.

| Dónde | Código | Fórmula | Qué compara |
|---|---|---|---|
| Perfil (lo que se expone) | `UserProfile.maxSessionMinutesAllowed` | `floor(M × 1,15)` | — |
| Validador V4 | `PlanDraftValidator.validateSessions` + `PlanningProfile.maxSessionMinutes` | `Math.round(M × 1,15)` | Minutos **declarados** por el borrador |
| Generador (techo) | `CapabilityWeekBuilder` / `RuleBasedTrainingPlanGenerator` | `PlanningProfile.maxSessionMinutes` (round) | Minutos estimados |
| Estimador | `SessionDurationEstimator.estimateMinutes` | `max(1, round(s / 60))` | — |

**Hallazgo nuevo de esta orden:** V4 compara los minutos **declarados**, y V17 admite un desvío del 20 % entre lo declarado y lo estimado.
- La prueba `V4DuracionDeclaradaTest` lo demuestra con el validador real: un perfil de 30 min, 7 ejercicios de 3×10/90 s, 2.550 s estimados (42,5 min, **142 % de M**), declarado como 35 min.
- **Pasa V4** (35 ≤ 35) y **pasa V17** (desvío del 18,6 % ≤ 20 %).
- El motor de reglas declara siempre lo que estima. Esto solo puede ocurrir con un borrador de IA, pero el validador no lo impide.

## 2. Casos de borde

Límite exacto de la tolerancia: L = M × 60 × 1,15 = 69 × M segundos.

| M | L exacto | Máximo admitido HISTÓRICO (round, declarado) | TRUNCADO (V4-B) | SEGUNDOS (V4-C) |
|---:|---:|---:|---:|---:|
| 30 | 2.070 s (34,5 min) | 2.129 s (35,48 min, 118,3 %) | 2.069 s (34,48 min) | **2.070 s** |
| 45 | 3.105 s (51,75 min) | 3.149 s (52,48 min, 116,6 %) | 3.089 s (51,48 min, 114,4 %) | **3.105 s** |
| 60 | 4.140 s (69 min) | 4.169 s (69,48 min, 115,8 %) | 4.169 s (115,8 %) | **4.140 s** |
| 75 | 5.175 s (86,25 min) | 5.189 s (86,48 min, 115,3 %) | 5.189 s (115,3 %) | **5.175 s** |

Comprobado en `PoliticasCandidatasTest.bordesV4` y `maximosAdmitidos`: límite, un segundo por debajo y un segundo por encima, con las tres versiones.

### V4-B frente a V4-C

**No aceptan las mismas sesiones:**
- **A 30 y 45 min, V4-B es más estricta que la tolerancia real.** Rechaza 2.070 s a 30 min (exactamente el 115 %) y todo lo que va de 3.090 a 3.105 s a 45 min, aunque está dentro del 15 %.
- **A 60 y 75 min, V4-B es más permisiva.** Acepta hasta 4.169 s (115,8 %) y 5.189 s (115,3 %).
- Esto ocurre porque V4-B redondea **dos veces**: trunca el techo y redondea los segundos al minuto. **V4-C** aplica exactamente el 15 %, en aritmética entera (`segundos × 100 ≤ M × 60 × 115`), sin redondeos.

## 3. Alternativas

| Versión | Qué hace | A favor | En contra |
|---|---|---|---|
| **V4-A**: histórico | Se conserva tal cual, solo para planes ya publicados | No reinterpreta nada | Mantiene la incoherencia y la brecha de lo declarado |
| **V4-B**: truncamiento | `floor` uniforme sobre minutos declarados | Coincide con lo que el perfil expone hoy | Sigue dependiendo de lo declarado y del redondeo al minuto. Error de hasta ±1 min |
| **V4-C**: segundos estrictos | Segundos **estimados** frente a M × 60 × 1,15 | Exacta, igual que V20, que ya compara segundos. Cierra la brecha de lo declarado. Generador y validador usan el mismo número | El perfil debería mostrar 34,5 min a 30, o seguir mostrando 34 como valor orientativo |

**Recomendación (pendiente de aprobación): V4-C**, como versión nueva y sin efecto retroactivo.
- La razón **no** es aceptar más planes: acepta menos que el histórico a 30, 45, 60 y 75.
- La razón es que es la única que aplica la tolerancia declarada y que **mide el contenido**, no la declaración.
- Para que sea coherente, el generador debe usar el mismo límite en segundos (hoy usa `maxSessionMinutes` en minutos). Así no producirá planes que el validador rechace.

## 4. Consecuencias medidas

| Conjunto | Sesiones | Cambian con V4-B | Cambian con V4-C |
|---|---:|---:|---:|
| Campaña P-1/2/3 de 100 semanas (`campana_p123/evidencia/planes.csv`), recalculada con el estimador | 456 | **0** | **0** |
| Laboratorio: 960 perfiles × 6 caminos (`MATRIZ_C2_VERIFICADA.csv`) | ver `REGRESIONES_V4.md` | | |

En la campaña, lo declarado coincide con lo estimado en las 456 sesiones, porque son del motor de reglas.

## 5. Compatibilidad histórica

- Un plan sin versión registrada se valida como **HISTÓRICO** (`VersionPoliticaPlanificacion.deUnPlan(null) = HISTORICA`). Nada se reinterpreta.
- La versión `PLAN-V4-0.1` (V4-C) queda definida pero **no activable** (`activable() = false`).
- La migración propuesta para registrar la versión en el plan está en `VERSIONADO_POLITICAS_PROPUESTA.md`. Es un SQL de documentación: **no está** en `db/migration` y no se aplica a Neon ni a Railway.
