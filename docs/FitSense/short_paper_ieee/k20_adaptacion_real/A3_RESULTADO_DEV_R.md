# A3 · Resultado de DEV-R: V2 frente a V2.1 con Replicate real (U01–U20)

**Naturaleza: DESARROLLO.** DEV-R sirve para elegir el prompt antes de congelar. **No es la evaluación del artículo**, y U01–U20 no son una cohorte independiente.

**Autorización:** «Confirmo mi autorización para ejecutar DEV-R con Replicate real […] 48 predicciones, USD 2,40».

**Ejecución:**
- Una corrida por versión, sin repeticiones y sin descartar nada.
- Las dos terminaron sin fallos técnicos y se conservan íntegras.

| | DEV-R-V2 | DEV-R-V2.1 |
|---|---|---|
| Carpeta | `evidencia/desarrollo/DEV_R_v2` | `evidencia/desarrollo/DEV_R_v2.1` |
| Commit | `49d9a40` | `49d9a40` |
| Prompt | `FIX-PR-2.0-EXP` | `FIX-PR-2.1-EXP` |
| Modelo | `gpt-5-mini` sobre `openai/gpt-5-structured`, esfuerzo `medium`, hasta 3 intentos | igual |
| Huella del oráculo v2 | `19e911b1…` | `79f32cd2…` |

Tabla completa: `evidencia/desarrollo/DEV_R_comparacion/COMPARACION_DEV_R.csv`, generada por `herramientas/comparar_dev_r.py`.

## 1. Resultado

| Dimensión | V2 | V2.1 |
|---|---:|---:|
| **Recorridos correctos (oráculo v2)** | **20/20** | **20/20** |
| **Incumplimientos de seguridad** (planes inválidos publicados, sesiones no señaladas alteradas, aumentos de dosis o dificultad, incoherencias de texto) | **0** | **0** |
| Sesiones señaladas | 14 | 14 |
| … factibles según la sonda | 10 | 10 |
| … no factibles (U03 S3 y U04 S3: CONS-J; U07 S3 y U12 S3: sin causa) | 4 | 4 |
| **Adaptaciones efectivas y pertinentes de la IA** (origen IA, cambio real, publicadas, ACC o ALT) | **9 / 10** | **10 / 10** |
| … **ACC** | **5** | **10** |
| … **ALT** | **4** | **0** |
| **INC** (no pertinentes) | **1** (U19 S3) | **0** |
| **Dificultad** ante «demasiado difícil» (4 sesiones) | 2 con variante menos difícil y 2 con menos dosis; **0 sustitutos más difíciles** | 2 con variante menos difícil y 2 con menos dosis; **0 sustitutos más difíciles** |
| **Músculo objetivo** en las sustituciones | **6 de 9** lo conservan | **4 de 4** lo conservan |
| **Cambio mínimo ante desmotivación** (3 sesiones con cambio) | 2 ejercicios, 2 ejercicios y **8** (INC) | **1, 1 y 1** |
| **Propuestas rechazadas** (validadores o contrato) | **0 de 8** | **0 de 8** |
| Reintentos | 0: todos los ciclos se resolvieron al primer intento | 0 |
| Respaldos deterministas en sesiones factibles | 0 | 0 |
| Predicciones | 8 | 8 |
| Tokens (entrada / salida) | 291.118 / 21.485 | 293.095 / 19.790 |
| **Coste real medido** | **USD 0,1157** | **USD 0,1129** |
| Coste por predicción (mín. – máx.) | 0,0074 – 0,0224 | 0,0080 – 0,0207 |
| Latencia (mediana / p90) | 34,9 s / 44,0 s | 26,5 s / 35,3 s |

**Gasto total de DEV-R:**
- **USD 0,2286** en 16 predicciones (de 48 autorizadas).
- Ninguna predicción se acercó a la reserva de 0,05 USD y no hubo ningún bloqueo de presupuesto.
- Es el gasto calculado con la tarifa por token; la cifra oficial es la del panel de Replicate.

## 2. Adaptaciones por usuario (sesiones con IA)

Detalle con nombres en `evidencia/desarrollo/DEV_R_comparacion/DEV_R_ADAPTACIONES_DETALLE.csv`; ambas versiones por sesión en `DEV_R_SESIONES.csv`.

| Usuario | Causa | V2 | V2.1 |
|---|---|---|---|
| **U04 S2** | demasiado difícil | **ALT**. **Siete sustituciones**, todas de menor o igual dificultad. **Dos cambian el músculo objetivo**: peso muerto con cable (360, glúteos) → peso muerto con piernas rectas con banda (142, **isquiotibiales**), y 580 (pectorales) → 78 (**tríceps**) | **ACC**. Press inclinado en polea (380) → fondos de pecho asistidos (72), pectorales. Hack squat Smith (1236) → sentadilla con banda (144), glúteos. Las dos de dificultad 2 → 1 |
| **U08 S3** | fatiga | **ACC**. Press de banca con cable y jalón trasero: 10 → 9 repeticiones | **ACC**. Igual |
| **U09 S2** | demasiado difícil | **ALT**. Remo vertical con mancuerna (780, deltoides) → remo sentado a una mano (119, **espalda alta**): cambia el músculo. Además, menos repeticiones | **ACC**. Press declinado (540) → fondos de pecho asistidos (72), pectorales. Remo vertical (780) → remo de deltoides traseros con banda (135), **deltoides**. Más reducción de repeticiones |
| **U09 S3** | desmotivación | **ALT**. Reduce **2** ejercicios | **ACC**. Reduce **1**: press de agarre invertido, 10 → 9 |
| **U13 S3** | fatiga | **ACC**. Pies rápidos y media sentadilla: 90 → 80 s | **ACC**. Pies rápidos: 3 → 2 series |
| **U14 S2** | demasiado difícil | **ACC**. Media sentadilla y pies rápidos: 90 → 80 s | **ACC**. Igual |
| **U14 S3** | desmotivación | **ALT**. Reduce **2** ejercicios | **ACC**. Reduce **1**: empujar para correr, 90 → 80 s |
| **U18 S3** | fatiga | **ACC**. Escalador: 3 → 2 series | **ACC**. Reduce 3 ejercicios: press de hombros con banda, 10 → 9; escalador, 90 → 80 s; estiramiento lumbar sentado, 30 → 20 s |
| **U19 S2** | demasiado difícil | **ACC**. Media sentadilla: 3 → 2 series | **ACC**. Igual |
| **U19 S3** | desmotivación | **INC**. Reduce **8** ejercicios (una repetición o un segundo menos en cada uno): no es un cambio mínimo | **ACC**. Reduce **1**: escalador, 90 → 80 s |

Sin IA, en las dos versiones:
- **U03 S3 y U04 S3:** CONS-J. GAIN_MUSCLE con dosis fija, imposibilidad demostrada en `01`. **No cuentan como éxito.**
- **U07 S3 y U12 S3:** sin causa registrada; se conservan sin llamar a la IA.

**Para la revisión profesional** (no afectan a la clasificación):
- En **U18 S3** (fatiga), V2.1 acorta también un **estiramiento** de 30 a 20 s. La regla de fatiga lo admite, porque es menos trabajo, pero un profesional podría preferir conservar la movilidad.
- En **U13 S3**, V2.1 elimina una serie en lugar de acortar la duración. Los dos son una reducción de trabajo.

## 3. Lectura

1. **Recorridos y seguridad: empate perfecto.** Las dos versiones tienen 20/20 recorridos y 0 incumplimientos. Ninguna propuesta se rechazó: el contrato y los datos de factibilidad de V2 bastan para que el modelo proponga cambios válidos al primer intento.
2. **Efectividad.** V2.1 publica 10 adaptaciones efectivas y pertinentes de 10 posibles; V2 publica 9. La diferencia es U19 S3, que en V2 es INC: redujo 8 ejercicios ante desmotivación.
3. **Pertinencia.** Aquí está la diferencia principal. V2.1 tiene **10 ACC y 0 ALT**; V2, **5 ACC y 4 ALT**. Las ALT de V2 se deben a dos cosas:
   - sustituciones que cambian el músculo objetivo: 3 de sus 9 sustituciones, 2 en U04 S2 y 1 en U09 S2;
   - cambios de 2 ejercicios ante desmotivación (U09 S3 y U14 S3).

   V2.1 sustituye menos (4 frente a 9; V2 hizo 7 sustituciones en una sola sesión, U04 S2) y siempre con el mismo músculo.
4. **Dificultad:** igual en las dos. Las sustituciones bajan de dificultad 2 a 1, y ningún sustituto es más difícil.
5. **Coste y latencia:** prácticamente iguales en coste (0,113 frente a 0,116 USD). V2.1 es algo más rápida (mediana de 26,5 s frente a 34,9 s), con 8 predicciones cada una. Una sola corrida no basta para afirmar una diferencia de latencia.

**Límites de esta comparación:**
- Es **una sola corrida por versión, sobre 10 sesiones factibles**, con una cohorte que se usó en el desarrollo. La diferencia (10 frente a 9 efectivas; 10 frente a 5 ACC) es clara en esta muestra, pero no permite estimar su variabilidad. Eso lo medirán R y R′.
- La pertinencia es la regla operativa del oráculo v2, no un juicio profesional.

## 4. Recomendación

Aplico la regla de decisión fijada antes de ejecutar (`A2` §4):

1. **Seguridad:** las dos tienen 0 incumplimientos, así que ninguna queda descartada.
2. **Adaptaciones correctas y pertinentes:** V2.1 tiene 10 (10 ACC) y V2 tiene 9 (5 ACC y 4 ALT), más 1 INC. **Gana V2.1**, y no por cambiar más: hace **menos** sustituciones (4 frente a 9) y cambios más pequeños.
3. **Cambio mínimo, dificultad y músculo:** V2.1 es mejor o igual en las tres.
4. Coste y latencia no hacen falta para decidir.

**Recomendación: V2.1 (`fitsense.ai.adaptation.prompt-version=v2.1`) como versión definitiva** para congelar y ejecutar R y R′ sobre U01–U20 y J01–J16. La decisión final es tuya.

**Antes de congelar no cambio nada más.** Lo que quedaría fijado:
- SHA de la candidata: `49d9a40`, o el commit de congelación que solo añada esta documentación;
- prompt V2.1;
- oráculo v2;
- criterios del `07`;
- cohortes U y J.
