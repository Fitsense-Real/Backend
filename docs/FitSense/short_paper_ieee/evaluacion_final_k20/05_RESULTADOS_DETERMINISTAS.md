# 05 · Resultados deterministas (D, evaluación congelada)

**Condición:** D, con la IA desactivada.

**SHA congelado:** `a31c36bbb0c94eb99c84d8fcb37465797efec459`.

**Evidencia:** `evidencia/final/D/` (tablas, resultados y log de Maven) y `evidencia/final/D/oraculo/`.

## Resultado 1: recorrido completo correcto

**20/20 PASS** según el oráculo independiente. D′ da el mismo resultado (`06`).

| Fase | Resultado | Detalle |
|---|---|---|
| A · Generación inicial | **20/20** planes válidos | • Perfil almacenado igual al congelado.<br>• 3 sesiones L-X-V.<br>• Equipamiento, nivel, V5, V7, V21, V20, V4 y V17 según el oráculo.<br>• 0 rechazos del validador en la generación |
| B · Registro | **20/20** | Lo almacenado coincide con lo registrado: H = dosis total; P40 = round(0,4 × total); 12 omisiones con su motivo; 8 «sin registro» que no cuentan como omisión |
| C · Adherencia | **20/20** | Aw, Aobs y C iguales al recálculo, con diferencia 0,00 |
| D · Decisión | **20/20** frente a la política sobre los valores reales | 18/20 frente al oráculo teórico histórico (U07 y U12, `09`) |
| E · Adaptación | **20/20** | **46/46** sesiones no señaladas idénticas; ninguna dosis aumentada; ningún sustituto; 0 INC |
| F · Publicación | **20/20** | Un solo plan vigente, linaje 3/3, idempotencia (el ciclo repetido devuelve el mismo plan y 0 filas V32 nuevas) y texto coherente |
| G · Continuidad | **20/20** | API HTTP 200 (plan por id y actual) con 3 sesiones; 60/60 sesiones de S2 registradas; métricas de S2 = 100; decisión NO_CHANGE; S3 válido y copia exacta |

## Decisiones (política vigente, valores reales)

| Decisión | Usuarios |
|---|---|
| NO_CHANGE | 6: E1 × 4, U02, U17 (P40 = 40 % exacto) |
| INSUFFICIENT_DATA | 4: E5 × 4 (C = 33,33 %) |
| ADAPT_LOCALIZED | 6: E3 × 4 (S3, fatiga), U07 y U12 (S3 parcial, causa no registrada) |
| ADAPT_EXPANDED | 4: E4 × 4 (S2 demasiado difícil, S3 desmotivación) |

## Desenlace de la publicación (V32)

| Estado | Usuarios | Sesiones señaladas | Por qué |
|---|---|---|---|
| SIN_ORDEN (copia) | 10 | — | NO_CHANGE o INSUFFICIENT_DATA |
| RESPALDO (`RESPALDO_SIN_IA`) | 8 (E3, E4) | 12 → RESPALDO-COPIA, CONS-J | La política, **sin IA**, conserva la semana: sin dolor no hay sustitución determinista |
| SIN_EFECTO (`CONSERVACION_DELIBERADA`) | 2 (U07, U12) | 2 → CONS-J | CAUSA_NO_REGISTRADA no autoriza cambios ni llama a la IA |

**En D no hay ninguna adaptación efectiva**: 0 de 14 sesiones señaladas cambian. Es lo correcto según la política (sin IA y sin dolor, se conserva), y por eso la aportación de la IA se mide contra este 0 (`02` §5).

## Control: IA simulada (`evidencia/final/S_control`)

**No es un resultado de IA.** Comprueba que el camino de la IA de la versión congelada valida, rechaza, publica y continúa correctamente:
- **20/20 PASS.**
- Las 8 propuestas inválidas a propósito del intento 1 se **rechazaron**: 3 por el contrato (sesión no señalada) y 5 por V8C (4 subidas de dosis y 1 sustitución no autorizada).
- **10** sesiones factibles se publicaron como adaptación de origen IA y pertinente (ACC).
- Las 2 sesiones imposibles (U03 S3, U04 S3) se conservaron (CONS-J).
- Ninguna sesión no señalada cambió y no hubo ningún incumplimiento de seguridad.
