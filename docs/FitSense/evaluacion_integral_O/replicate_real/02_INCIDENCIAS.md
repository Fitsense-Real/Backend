# Evaluación integral O con Replicate REAL · 02 · Incidencias, causa demostrada y decisión

**Regla aplicada (orden §8):** primero investigar, después decidir.

- Ninguna incidencia ha llevado a modificar el backend, los perfiles, los guiones, los criterios ni los umbrales.
- **No se ha repetido ninguna corrida pagada.**
- Todas las salidas originales se conservan en `evidencia/`.

## Resumen

| N.º | Corrida | Perfil · ciclo | Qué se observó | Clase | Decisión |
|---|---|---|---|---|---|
| I-1 | R_O | O12 · C1 (dolor de sesión) | La predicción falló en Replicate: `failed` en 1,2 s, sin tokens | **E. Proveedor** | Ninguna. El backend aplicó el **respaldo determinista** (RESPALDO) y se cargó la reserva de USD 0,05, según la regla. No es un defecto del backend |
| I-2 | R_O | O18 · C1 (ACORTAR) | El modelo propuso **SIN_EFECTO** existiendo una modificación válida (retirar el relleno 1302) | **C. Comportamiento del modelo** | Ninguna en el sistema. Se informa como «SIN_EFECTO evitable» y no se cuenta como adaptación pertinente. Consecuencia: en R_O no hay retirada y, por tanto, no hay reincorporación en O18 |
| I-3 | R′_O | O12 · C1 (dolor de sesión) | **FALLO A5** con el oráculo v1.2: «más trabajo: añadidos […]» | **B. Instrumento** (D-OR-3) | Se conserva el resultado v1.2 (140/141). Oráculo v1.3, corregido solo en la rama de dolor, con pruebas negativas: 141/141 |
| I-4 | R_O | O19 · C1 (ACORTAR) | El modelo redujo series en lugar de retirar un relleno | **C** | Ninguna. Es válido y pertinente (LOSE_WEIGHT admite bajar dosis). Consecuencia: no hay reincorporación en O19 en R_O ni en R′_O |
| I-5 | R′_O | O03 · C3–C4 | Ninguna RECUPERACION tras la reducción por fatiga (en R_O sí, en C3) | **Regla de negocio**, no defecto | Ninguna; propuesta para tu decisión (abajo). El backend aplica `recovery_step_pct` = 10 % tal como está |
| I-6 | R′_O | O12 · C1 | La IA rehízo la sesión con patrones nuevos ante un dolor de localización desconocida | **D/observación deportiva** | REQUIERE REVISIÓN profesional del criterio, sin tocar nada. La regla técnica (aprobada) se cumple |

## I-1 · O12, R_O: fallo del proveedor (E)

**Evidencia:**
- Traza: `evidencia/corridas/R_O_G.tar.gz` → `trazas_ia.jsonl`, `predicciones_metricas.csv`.
- Predicción `tdverjg2anrmy0d1nvtrrpexw0`: `status: failed`, `predict_time` 1,24 s, sin métricas de tokens.
- Registro del proveedor: `httpx.ReadTimeout` → `TypeError` → `JSONDecodeError`. Es un error interno de Replicate al llamar al modelo.

**Comportamiento del sistema:**
- `AiProviderUnavailableException` → respaldo determinista (`status = RESPALDO`).
- Sustituye los 8 ejercicios, ya que el dolor era de sesión, por otros del mismo patrón, sin más trabajo.
- El oráculo da A5 REQUIERE REVISIÓN por el sustituto 1093 (catálogo E4/E7), no FALLO.

**Coste:** la regla `RESERVA_SIN_BLOQUEO` carga USD 0,05 sin bloquear la corrida. No hubo reintento: con el proveedor caído, el backend pasa directamente al respaldo.

## I-2 · O18, R_O: SIN_EFECTO propuesto por el modelo con alternativa válida (C)

**Lo que le dijo el contexto al modelo:**
- «OBJETIVO: Acortar o reorganizar la sesión. Puedes bajar la dosis y retirar UNA colocación de relleno».
- «DOSIS: en GAIN_MUSCLE la dosis de 4D NO se reduce […]. Para acortar solo puedes retirar una colocación de relleno».

**Lo que respondió el modelo:** `{"rationale": "No modifiques la sesión 53: las reglas de GAIN_MUSCLE y la factibilidad impiden retirar o reducir dosis sin violar las restricciones.", "sessions": []}`.

**Lo que había de verdad:**
- La sonda de factibilidad, que solo mide, encontró **una** modificación válida: retirar el relleno 1302.
- Bajar repeticiones o series: 0 de 18 válidas.
- **La justificación del modelo es incorrecta**, porque retirar sí era posible.

**Por qué no es un defecto del sistema:**
- El prompt autorizaba exactamente esa retirada.
- El backend acepta SIN_EFECTO como conservación, que es un resultado permitido.
- **No se cambia el prompt ni el sistema** (orden §8-C).

**Cómo se informa:**
- El oráculo lo clasifica como CONS-J (conservación aceptada por la política).
- En el informe se cuenta como **adaptación no publicada (8/10)** y como «SIN_EFECTO evitable».
- En R′_O, el mismo ciclo **sí** retiró 1302, y O18 se reincorporó en C22.

## I-3 · O12, R′_O: FALLO del oráculo v1.2 (B, D-OR-3)

**Lo que pasó:**
- O12 omitió la sesión 182 entera por dolor, sin indicar el ejercicio.
- El prompt decía «dolor y no consta en qué ejercicio. No conserves ninguno de los de arriba», y la intervención admite sustituir o retirar.
- El modelo cambió los 8 ejercicios por 6 nuevos. El trabajo bajó de 1686 a 432 s y la dificultad no superó la de lo retirado (máx. 2).

**Por qué el oráculo v1.2 dio FALLO** (instrumento, demostrado):
- (a) Empareja por posición. Si la sesión cambia de longitud, cuenta todos los sustitutos como «añadidos» y los da por «más trabajo», aunque el trabajo baje un 74 %.
- (b) Exigía el mismo patrón y grupo también ante dolor.
- La regla **aprobada** del backend (`ConservationValidator`, rama de dolor, corrección 5 del 27/10/2026) dice expresamente que ante dolor **no** se exige el patrón. Exige en su lugar:
  - no más añadidos que retirados;
  - volumen que no sube;
  - dificultad no superior.

**Corrección v1.3 (D-OR-3)** — solo para SEGURIDAD_ANTE_DOLOR:
- Aplica esa regla.
- Con **dolor de sesión**, exige además que no quede ningún ejercicio de la sesión medida. v1.2 tampoco lo comprobaba (lo descubrió la prueba D4).
- Pertinencia: INC si hay más trabajo o más dificultad; ALT si entran patrones nuevos; ACC en otro caso.

**Pruebas** (`pruebas/pruebas_oraculo_v1_3.csv`, todas pasan):
- Las 19 negativas de v1.1, y la identidad y R-1 de v1.2.
- Control: O12 de R′_O sin mutar → APROBADO con v1.3, FALLO con v1.2.
- D1, más añadidos que retirados → FALLO.
- D2, trabajo que sube → FALLO.
- D3, añadido más difícil → FALLO.
- D4, un ejercicio de la sesión con dolor sigue → FALLO.
- D5, fuera del dolor: sustituto de otro patrón → FALLO (nada cambia fuera del dolor).

**Identidad** (`pruebas/identidad_v1_2_vs_v1_3.csv`): en S_O, S_O′, D_O y HIST, en la repetición S/S′ y en R_O, v1.3 = v1.2 (matriz, adaptaciones, semanas, pasos y huella). En R′_O cambia **exactamente** una celda: O12·A5, de FALLO a APROBADO (pertinencia INC → ALT).

| SHA-256 | Valor |
|---|---|
| v1.3 | Ver `SHA256SUMS.txt` |
| v1.2 (congelado en `cf3ac39`, sin cambios) | `07cb481f…` |

## I-4 · O19: ACORTAR por reducción de dosis (C)

- En las dos corridas, el modelo bajó series de 4–5 ejercicios en lugar de retirar un relleno.
- Trabajo: 882 → 588 s (R_O) y 882 → 606 s (R′_O).
- Está permitido para LOSE_WEIGHT («Puedes bajar la dosis y retirar UNA colocación de relleno») y es pertinente (ACC).
- **Consecuencia:** al no haber retirada, no hay reincorporación que observar en O19. A8 queda en NO APLICA, no en FALLO.
- Con la IA simulada, en cambio, se retiraba 853 y se reincorporaba en C9.

## I-5 · O03, R′_O: recuperación bloqueada por el tamaño del paso (regla de negocio)

| | R_O | R′_O |
|---|---|---|
| Reducción del modelo | 2 ejercicios a 2 series: la sesión queda en 10 series | Los 4 ejercicios a 2 series: la sesión queda en 8 series |
| Recuperación de una serie | +1 serie = **10,0 %** ≤ `recovery_step_pct` (10 %) → RECUPERACION en C3 | +1 serie = **12,5 %** > 10 % → `ProgresionResolver.haciaReferencia` no encuentra paso |
| Repeticiones | — | Siguen en 6 (no se redujeron) |

**Resultado:**
- La dosis reducida **no puede recuperarse nunca** mientras la sesión tenga 8 series.
- El oráculo no exige la recuperación (A7 NO APLICA), así que no es un FALLO. **Es una limitación de la regla aprobada**, no un error de implementación.

**Propuesta, no implementada (cambia una regla de negocio; orden §9):**
- Permitir como paso mínimo de recuperación **una serie** aunque supere el 10 %, solo cuando sea el único paso posible hacia la referencia y sin superarla.
- Requiere tu aprobación y, en su caso, una evaluación nueva.

## I-6 · O12, R′_O: observación deportiva

- Ante un dolor de localización desconocida, el modelo sustituyó una sesión de deltoides posterior, aducción, tracción horizontal, bisagra, 3 cardios y movilidad.
- La nueva tiene tracción, **empuje horizontal**, bisagra, **sentadilla con mancuerna**, plancha y cardio, con menos volumen.
- Técnicamente cumple la regla aprobada.
- **Desde el punto de vista deportivo** conviene que un profesional valore si, ante un dolor sin localizar, es prudente introducir patrones que no estaban, como la sentadilla. Se anota como REQUIERE REVISIÓN del criterio y no se toca nada.

## Sin incidencias en

- Generación.
- Restricciones (A2 20/20 en las dos corridas).
- Registro y adherencia.
- Coincidencia de decisiones: 124/124 en R_O y 120/120 en R′_O.
- Techo de volumen.
- Seguridad (A10 19/19).
- Idempotencia.
- Continuidad.
- O11: `FUERZA_SIN_TIEMPO` en las dos corridas.
- O11-F35: plan válido en las dos corridas.
