# 13 · Hallazgos y limitaciones

## Defectos del backend encontrados en la preparación

**Ninguno nuevo.**

En las corridas P1–P6 y en las definitivas, el oráculo no encontró ningún incumplimiento en el backend. P3, el único defecto conocido de la versión anterior, se corrigió **antes** de congelar, con autorización.

## Defectos y ajustes del laboratorio (prueba y documentación, sin efecto en `src/main`)

| # | Qué | Efecto | Estado |
|---|---|---|---|
| L1 | El arnés llamaba al controlador directamente y obtenía `LazyInitializationException` al recuperar el plan. Por HTTP no ocurre: Open Session in View lo evita | La comprobación de recuperación era inválida | **Corregido antes de congelar**: el arnés usa la API HTTP con token. En las corridas definitivas, 20/20 respuestas 200 |
| L2 | Las propuestas rechazadas por el **contrato** (sesión no señalada) no se registraban: no llegan al productor | La métrica de rechazos quedaba incompleta | Corregido antes de congelar |
| L3 | El reparto de las propuestas inválidas por `hashCode` no cubría las tres clases | Faltaba cobertura | Corregido antes de congelar: rotación por orden de usuario. El sustituto más difícil ante «demasiado difícil» no se pudo construir en el camino de la IA para principiantes: no hay nada más difícil elegible. Lo cubre la sonda (80/80 rechazados) |
| L4 | Las columnas `ciclo2_api_*` de `resultados_20.csv` repiten el resultado del ciclo 1: el arnés toma la primera coincidencia de semana | Solo un informe; el oráculo no las usa | **No corregido** (posterior a la congelación). El valor correcto está en `api_adaptaciones` (por ejemplo `2026-11-09:RESPALDO_SIN_IA\|2026-11-16:SIN_ADAPTACION`). Se corregiría en una versión nueva |
| L5 | La primera evidencia de preparación (P1, P2) incluía 25 MB de instantáneas JSON | Tamaño del repositorio | Se redujo en `99a02da`. El commit `6098584` sigue en el historial: borrarlo exige reescritura, que no está autorizada |

## Hallazgos funcionales (consecuencia de reglas vigentes, que no se cambian)

**H1. GAIN_MUSCLE con fatiga o desmotivación: imposible de adaptar** (U03 S3, U04 S3).
- La intervención solo permite cambiar la dosis.
- 4D/V24 la fija.
- V20 impide quitar series en 60 min.
- La IA no puede aportar nada en esos casos.
- `FactibilidadPreviaIA` no lo detecta: en R, U03 gastará hasta 3 predicciones.

**H2. U07 y U12.**
- P40 con dosis enteras da 39,26 % y 38,33 %.
- La política decide ADAPT_LOCALIZED con causa no registrada y conserva.
- Es correcto con los valores reales; difiere del oráculo teórico histórico. No se tocaron las entradas.

**H3. «Demasiado difícil» en principiantes** (U14, U19).
- No existe ningún sustituto más fácil: todo es de dificultad 1.
- Lo pertinente es bajar la dosis.

**H4. Sin IA no hay adaptaciones.**
- Sin dolor no hay ningún mecanismo determinista que cambie el plan.
- En D, las 12 sesiones para la IA se conservan (RESPALDO).
- Por eso toda adaptación efectiva de E3/E4 depende de la IA.

**H5. R0548**, registrado aparte (`11`). La generación inicial no pasa V20 en GM intermedio, casa, 7 días y 75 min.

## Limitaciones del estudio

- **Usuarios y alcance:**
  - 20 usuarios **sintéticos**, deterministas, con 3 días por semana;
  - 2 perfiles por objetivo;
  - una semana medida y una de continuidad;
  - no son personas reales y no permiten afirmar efectos conductuales ni clínicos.
- **Casos no cubiertos:** los 20 escenarios no contienen dolor, falta de tiempo ni cambio de horario. El límite de dificultad y la retirada se ejercitan solo indirectamente (sonda, control S, REPLAY). Los casos I01–I16 quedaron fuera de esta fase.
- **Resultados de IA:** no hay ninguno todavía (R/R′ pendientes). El control S no mide la IA.
- **Alcance del oráculo:** no re-verifica V16, V24/4D, V9 ni la cobertura 4C (`03`).
- **Alcance de la sonda:** solo enumera modificaciones **simples** (un cambio). Una conclusión de «no factible» se demuestra para esas clases y para cualquier combinación que incluya una reducción ya imposible. No cubre combinaciones de sustitución y dosis en sesiones de solo dosis, que tampoco están autorizadas.
