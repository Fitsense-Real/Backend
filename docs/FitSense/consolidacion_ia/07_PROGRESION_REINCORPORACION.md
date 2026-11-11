# 07 · Progresión y reincorporación

**Estado:**
- Se mantienen las políticas existentes, **sin cambios**.
- Ninguna campaña de este periodo modificó la progresión ni la reincorporación, y esta documentación tampoco lo hace.

Referencias:
- **Código:** `generation` `543932a`, `src/main/java/main/web/services/fitsense/planning/application/internal/commandservices/ProgresionResolver.java` (PR).
- **Diseño y política:**
  - `docs/FitSense/DISENO_PROGRESION_Y_REINCORPORACION.md`;
  - `docs/FitSense/POLITICA_ADAPTACION_Y_PROGRESION.md`;
  - `docs/FitSense/rediseno_politicas/05_ADAPTACION_Y_PROGRESION.md`.

## 1. Activación

- **Por defecto, la progresión está apagada:** `fitsense.adaptation.progression.enabled=${FITSENSE_PROGRESSION_ENABLED:false}`. `POLITICA_ADAPTACION_Y_PROGRESION.md` §5 dice «apagada en todos los entornos».
- **Excepción:** con PLAN-REV-2.0, en GAIN_MUSCLE con fatiga (P-A1) se permiten pasos de RECUPERACION aunque la progresión esté apagada.
- **En las evaluaciones:**
  - se activó solo en el bloque P de la cohorte O (O15–O20), según `evaluacion_integral_O/03_VERSION_CONGELADA.md`;
  - las pruebas de integración (`ProgresionIT`, `ProgresionRev2IT`, `ProgresionCienPorCienIT`, `PendientesProgresionIT`, `ReincorporacionIaRealIT`) la activan en sus propiedades.

## 2. Reglas vigentes

| Regla | Cómo está implementada | Referencia |
|---|---|---|
| **Progresión condicionada a cumplimiento sostenido** | Dos semanas consecutivas al **100 % real**: adherencia y cobertura de registro al 100 %, con todas las sesiones y ejercicios completados. La recuperación y la reincorporación usan el umbral del 80 % | PR: `UMBRAL_PROGRESION = 100.0` (l. 122) y `dosSemanasPerfectas` (l. 333). El diseño v0.2.1 decía 80 %; la nota del 05/11 lo corrige al 100 % |
| **Incrementos graduales y limitados** | Primero +1 repetición; +1 serie solo cuando las repeticiones están en su máximo. Cada paso ≤ 5 % del volumen (`progression_increase_pct = 5.0`); recuperación ≤ 10 % | `V7__config_data.sql` l. 25; PR |
| **Techo acumulado 1,10×** | El volumen no supera 1,10 × el volumen base de la raíz del linaje. Con un ejercicio retirado pendiente, el techo para progresar es 1,10 × base − volumen del retirado (se le reserva el sitio) | PR: `TECHO = 1.10` (l. 74) |
| **Un paso por ciclo** | El resolvedor devuelve candidatos ordenados; el servicio los revalida (V8C, V1–V27) de uno en uno y **publica a lo sumo uno**. Solo en ciclos NO_CHANGE, nunca en el mismo ciclo que una orden de adaptar | PR l. 29–30; `AdaptedWeekCommandService`; `AdaptedWeekProducer` |
| **Reincorporación solo tras una retirada previa y si se cumplen las condiciones** | Solo vuelve un relleno retirado por ACORTAR, con su dosis inicial exacta. Debe seguir elegible, no haber dolido ni estar excluido, caber en V4 y en la M vigente, y caber en el techo. Si no cabe por tiempo, la reserva pasa a EN_ESPERA_DE_TIEMPO | PR l. 236–270 |
| **Progresión antes de reincorporación** | Prioridad de los pasos: vuelta atrás 0, recuperación 1, **progresión 2, reincorporación 3** (4 si la progresión está en espera de tiempo). Además, la reincorporación exige **al menos una progresión válida después del acortamiento** (`pasosTrasRetirada(linaje) == 0` → no hay reincorporación) | PR l. 178–184 y 262–265 |
| **Sin restauración automática** | El ejercicio retirado no se restaura solo. Vuelve únicamente como paso de REINCORPORACION, con las condiciones anteriores | `DISENO_PROGRESION_Y_REINCORPORACION.md` (tabla de cabecera y nota del 05/11); prueba `Reincorporacion4DTest.noRestauraLaHistorica` |

## 3. Relación con la generación y la adaptación

| Componente | Quién decide | ¿Puede subir dosis? | Validación |
|---|---|---|---|
| Generación inicial | IA (PR-1.7) con respaldo determinista | Propone la dosis exacta de su clase o capacidad | V1–V27; sin V8C |
| Adaptación | IA (V2.1), solo en las sesiones señaladas | **No** | V8C + V1–V27 |
| **Progresión, recuperación y reincorporación** | **Solo el backend, de forma determinista** (0 llamadas a la IA) | Sí, en pasos pequeños y con techo | V8C + V1–V27 |

**Secuencia típica** (perfiles O18 y O19 de la cohorte):
1. Falta de tiempo → ACORTAR: la IA retira un relleno.
2. Cumplimiento sostenido → PROGRESION (1 paso por ciclo, ≤ 5 %, techo 1,10×).
3. Tras al menos una progresión → REINCORPORACION del relleno retirado, si cabe.

**Consecuencia:**
- Si la IA **no retira** el relleno (SIN_EFECTO en O18, GRA-R2) o reduce dosis en lugar de retirar (O19), **no hay nada que reincorporar**.
- La reincorporación nunca se fuerza.

## 4. Resultados observados (backend determinista, sin IA)

**GRA-R1 y GRA-R2** (06/11/2026). Fuente: INF, tabla 5.

| | GRA-R1 | GRA-R2 |
|---|---|---|
| Pasos de PROGRESION | 26 | 30 |
| Pasos de RECUPERACION | 3: O03, O14, O17 | 3: O03, O14, O17 |
| Pasos de REINCORPORACION | **1**: O18, ejercicio 502 retirado en C1 y reincorporado en C14 | **0** (la IA no retiró en O18; en O19 redujo dosis) |
| Techo 1,10, ≤ 1 paso por ciclo, ninguna PROGRESION sin dos semanas al 100 % | Sí | Sí |
| Oráculo A7 / A8 (AP/FA/NA) | 8/0/12 · 3/0/17 | 8/0/12 · 2/0/18 |

- **Por perfil:** O15 3 progresiones, O16 1, O17 1, O18 11 en R1 y 15 en R2, O19 4, O20 6.
- La diferencia en O18 (11 frente a 15) se debe a que en R1 parte del margen se reservó para la reincorporación.
- **CONF-IA, flujo conjunto I1** (PR-1.7 + v2.2a + V8C corregido): A7 8/0/12 y A8 3/0/17 (AP/FA/NA), sin fallos (`confirmacion/RESULTADOS_VALIDACION_INTEGRAL.md`).

## 5. Qué no se ha demostrado

- **Que estos incrementos sean los adecuados para personas reales:** el 5 %, el techo de 1,10× y el umbral del 100 % son decisiones de diseño, **pendientes de validación profesional**.
- **El efecto a largo plazo:** las cohortes son sintéticas y los horizontes cortos (4 semanas en la mayoría de perfiles; 8–26 en O18–O20).
