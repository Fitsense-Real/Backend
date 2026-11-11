# 01 · Diagnóstico de O12 (adaptación ante dolor)

**Base:** `generation` `471296d`, con la configuración evaluada activa (PR-1.7, V2.1, PLAN-REV-2.0, IA, progresión, límite de dificultad `preferencia`, retirada localizada `on`, V8C con H-A1/H-A2).

**Reproducción:** `O12DolorProgresionApagadaIT` y `O12DolorProgresionEncendidaIT` (base común `O12DolorBase`). Sin red; la IA está simulada.

## 1. Escenario

- **Perfil O12 de la cohorte O:** GENERAL_FITNESS, intermedia, gimnasio, 55 min, martes y jueves, 62 años, sin colgarse ni ejercicios por encima de la cabeza.
- **Semana 1:** martes completado; el jueves se omite por dolor (`PAIN_OR_DISCOMFORT`, sin ejercicio identificado). El backend ordena `ADAPT_LOCALIZED` con `SEGURIDAD_ANTE_DOLOR` sobre el jueves.
- **Respuesta de la IA:** fija e idéntica en las dos condiciones (progresión apagada y encendida). Se probaron tres variantes:

| Respuesta | Qué propone |
|---|---|
| `MENOS_SERIES` | El jueves igual, con una serie menos en el primer ejercicio de 3 series. Es lo que propuso la IA simulada del arnés con la progresión apagada («MENOS_SERIES 591 series 3->2») |
| `VACIA` | Ninguna sesión. Es lo que propuso con la progresión encendida («SIN_CANDIDATA_VALIDA») |
| `SUBE_SERIES` | El jueves igual, con una serie más en el primer ejercicio de 3 series (prohibido) |

## 2. Resultados

**Antes de la corrección** (`generation` `471296d`):

| Respuesta | Progresión apagada | Progresión encendida |
|---|---|---|
| `MENOS_SERIES` | APLICADA (1 llamada) | 3 rechazos V24 → RESPALDO |
| `VACIA` | SIN_EFECTO | SIN_EFECTO |
| `SUBE_SERIES` | **APLICADA: se publica una serie más** | 3 rechazos V24 → RESPALDO |

**Después de la corrección** (rama `fix/validated-ai-safety-review`):

| Respuesta | Progresión apagada | Progresión encendida |
|---|---|---|
| `MENOS_SERIES` | APLICADA | RESPALDO |
| `VACIA` | SIN_EFECTO | SIN_EFECTO |
| `SUBE_SERIES` | **RESPALDO** (rechazo V8C «sube la dosis a través del sustituto») | RESPALDO |

**En todas las variantes y condiciones:**
- P-02 excluye los 8 ejercicios del jueves (dolor sin identificar → toda la sesión);
- el jueves publicado sustituye los 8 por SUST-SEG-1;
- **ningún ejercicio excluido reaparece** en las semanas 2 a 5, tampoco con los 2 pasos de progresión que hubo con la progresión encendida.

## 3. Causa exacta del cambio APLICADA → SIN_EFECTO / RESPALDO

1. **La IA no sustituye los ejercicios con dolor:** les baja una serie, es decir, mantiene un ejercicio que dolió.
2. **El backend los repara.** `AdaptedWeekProducer.produce` aplica `SafetySubstitutionPolicy.reparar` antes de V8C:
   - los ejercicios heredados de la semana medida que están excluidos por P-02 se sustituyen por SUST-SEG-1;
   - el ejercicio que la IA modificó también es heredado (estaba en la semana medida), así que también se sustituye.
3. **La dosis del sustituto viene de la propuesta de la IA.** `SafetySubstitutionPolicy.dosis` le da «series y descanso del original», y el original es la colocación ya modificada. Ejemplo: el 336 (3x10) baja a 2x10 y su sustituto, el 135, sale con 2x10.
4. **V8C no autoriza la dosis de un sustituto.** `ConservationValidator.revisarDosisDeLaIa` solo autoriza reducciones de ejercicios que se quedan: «un ejercicio nuevo (sustituto) no recibe nada: dosis exacta».
5. **Con la progresión encendida entra V24 por capacidades en la adaptación** (`StrictValidationAdapter`: `capacidades = capacidadesFlag || progresion`), y rechaza la dosis 2x10 del sustituto:
   > V24: el ejercicio 135 … no lleva la dosis de ninguna capacidad de GENERAL_FITNESS. Usa exactamente una de: STRENGTH 3x6 / HYPERTROPHY 3x10 / MUSCULAR_ENDURANCE 3x18.
6. **Desenlace con la progresión encendida:**
   - con la respuesta fija, tras 3 rechazos, RESPALDO por P-03: sustitución determinista con la dosis medida;
   - la IA simulada del arnés, que filtra sus candidatas con la misma validación, no encuentra ninguna válida («V24=22» en `factibilidad.csv`) y responde vacío → SIN_EFECTO, con la misma reparación del backend.

**Con la progresión apagada,** nadie comprueba la dosis 2x10 del sustituto (4D solo actúa en GAIN_MUSCLE) y la propuesta se publica.

## 4. Defecto encontrado y corregido

**Defecto:** con `SUBE_SERIES` y la progresión apagada, la IA subió el 991 de 3x18 a 4x18. El backend lo sustituyó por el 72, que heredó 4x18 («dosis_nueva 4x18»), y la semana se publicó como **APLICADA, con una serie más en plena semana de dolor**.

**Reglas existentes que incumple:**
- «La IA nunca sube dosis, con o sin interruptores» (`ConservationValidator.revisarDosisDeLaIa`);
- «ante dolor no se añade trabajo» (P-03, `masTrabajoQueLaMedida`).

**Por qué escapaba:**
- V8C compara las dosis por identificador de ejercicio, y el sustituto es nuevo;
- SUST-SEG-1 calcula el «nunca mayor» respecto a la dosis propuesta por la IA, no respecto a la semana medida.

Con la progresión encendida la frenaba V24 solo de forma indirecta: una subida que coincidiera con una dosis exacta de capacidad (por ejemplo, 2x10 → 3x10 de HYPERTROPHY) también habría pasado.

**Corrección mínima** (`AdaptedWeekProducer.subidasPorSustitucion`, solo en el camino de corrección de la IA):
- para cada sustitución SUST-SEG-1, la dosis publicada del sustituto no puede superar en series, repeticiones ni segundos la que SUST-SEG-1 le daría partiendo de la dosis **medida** del ejercicio quitado;
- si la supera, la propuesta se rechaza con un problema V8C y el código SEMANA_NO_VALIDA, y sigue el flujo de reintentos y respaldo de siempre;
- las bajadas siguen valiendo;
- no cambia V24, V8C, SUST-SEG-1, los prompts, la generación inicial ni ninguna política.

**Pruebas:**
- **Antes del cambio:** `subeSeries` falla en las dos clases. Con la progresión apagada, «una subida de dosis de la IA no se publica nunca» (estado APLICADA); con la progresión encendida falta el rechazo V8C, solo hay V24.
- **Después del cambio:** pasan las 6. Las variantes `MENOS_SERIES` y `VACIA` demuestran que lo autorizado sigue igual.

## 5. ¿Qué pasa con el ejercicio que dolió?

| Pregunta | Respuesta comprobada |
|---|---|
| ¿Permanece en el plan almacenado? | No: el jueves publicado lleva los 8 sustitutos |
| ¿Sigue siendo ejecutable? | No: no está en ninguna sesión publicada |
| ¿Queda excluido? | Sí: P-02 lo deja `ACTIVA` en `user_safety_exclusions` |
| ¿Aparece la semana siguiente? | No, en las semanas 2 a 5 (`excluidos_que_reaparecen_por_semana` vacío) |
| ¿Puede volver por progresión? | No: la progresión exige ejercicios elegibles no excluidos (`ProgresionResolver.cumple`) y no hubo reaparición tras 2 pasos |
| ¿Puede reincorporarse solo? | No: la reincorporación descarta lo excluido o lo que dolió (`descartada`) |
| ¿Se activa una regeneración segura? | Sí, en el camino de respaldo: «REGENERACION_SEGURA_SUSTITUCION» |
| ¿El estado es coherente para el usuario? | El texto publicado sí: «Reportaste molestias. Por seguridad, cambiamos o retiramos 8 ejercicios». Con la respuesta vacía, la API marca `SIN_CAMBIOS_VALIDOS` / sesión `SIN_EFECTO` aunque el backend sí sustituyó: la etiqueta se refiere a la propuesta de la IA. Es una incoherencia menor de presentación, no de seguridad (ver `07`) |

## 6. Conclusión

- **Seguridad:** el ejercicio que dolió no vuelve en ninguna variante ni condición.
- **Defecto corregido:** la vía por la que la IA podía subir la dosis de un sustituto en una semana de dolor.
- **El cambio de O12 no es un defecto:** es la V24 por capacidades rechazando una dosis de sustituto que la propia regla de V8C no autoriza. Que una reducción de la IA sobre un ejercicio con dolor no sobreviva a su sustitución es una consecuencia de diseño: la IA no debería mantener el ejercicio que dolió. Si se quiere que un sustituto conserve una bajada, es una decisión de política (ver `07`).
