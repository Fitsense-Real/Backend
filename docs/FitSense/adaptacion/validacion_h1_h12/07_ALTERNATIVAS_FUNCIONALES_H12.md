# 07 · Alternativas funcionales para H12 (fases 4 y 5.1)

## 0. Tres conceptos (fase 3.2)

| Concepto | Definición operativa en FitSense | ¿Lo verifica el software? |
|---|---|---|
| **A. Sustitución válida** | Un ejercicio se reemplaza por otro y el plan pasa V1–V25, V8C y V32 | **Sí** (validadores) |
| **B. Reducción técnicamente observable** | La modificación baja al menos una dimensión **registrada y autorizada**: `difficulty_level` del catálogo (dentro del mismo patrón), la **dosis** (series, repeticiones o segundos, dentro de 4D, V16 y V20) o la **carga** prescrita (kg). **No cuentan:** cambiar el equipamiento, el nombre, el orden o el músculo | **Sí**, por comparación de catálogo y dosis |
| **C. Simplificación funcional** | La modificación facilita de verdad la ejecución: técnica, estabilidad, coordinación, rango, demanda de agarre o biomecánica | **No.** El catálogo no tiene esas dimensiones; requiere evaluación profesional |

**Consecuencias:**
- Una sustitución 1 → 1 **puede** ser más sencilla en la práctica (por ejemplo, una máquina asistida), pero **el catálogo no permite demostrarlo**. Se clasifica «lateral», **no incorrecta** y **no mejora demostrada**.
- Una 2 → 1 es una reducción observable **del dato del catálogo**. Que facilite la ejecución sigue sin estar validado profesionalmente.

## 1. Las cuatro alternativas

### H12-A · Priorizar reducciones demostrables (**prototipo implementado, apagado**)

**Regla** (`fitsense.adaptation.variante.policy=reduccion-demostrable`). En una sesión VARIANTE (TOO_DIFFICULT):
1. Cada ejercicio añadido debe **emparejarse** con un retirado **distinto** del mismo patrón y de **mayor** dificultad. La sustitución lateral se rechaza.
2. La reducción de dosis que ya estaba permitida **sigue permitida** (sin tocar 4D, V16 ni V20).
3. Si no hay ni variante de menor dificultad ni dosis que quepa, **conservar** la sesión (SIN_EFECTO) es la respuesta.

**Código:**
- V8C: `revisarReduccionDemostrable`, con un emparejamiento por caminos de aumento.
- Prompt: los «SUSTITUTOS ADMITIDOS» de V1 y los de la factibilidad V2 solo ofrecen los de dificultad **<** (no ≤), y V2 dice «OMÍTELA» cuando no queda nada. Con la política histórica, el prompt es **idéntico byte a byte** (`PromptVarianteH12Test` 1).

### H12-B · Variantes del mismo nivel con justificación (**no implementable con los datos actuales**)

**Qué haría falta para considerar «más sencilla» una variante del mismo nivel:** un atributo que lo respalde. Por ejemplo:
- asistencia mecánica, base de apoyo, estabilidad, rango, carga relativa al peso corporal o complejidad técnica;
- o una relación de simplificación (A → B) **aprobada por un profesional**.

**Lo que hay en el catálogo:**
- nombre, zona, equipamiento, músculo objetivo, sinérgico, secundarios, patrón, aislamiento, dificultad (1–3), tipo de prescripción e instrucciones de texto;
- ninguna de esas columnas mide la demanda.

Inferirla del **nombre** («assisted», «kneeling», «wall») o del equipamiento sería una interpretación del desarrollador, que la orden excluye.

**Estado:** **pendiente de evaluación profesional.** Si se aprueba, la vía mínima es una tabla curada de pares «más sencillo que», consultada por V8C y por el prompt. Su forma la decidiría quien la cure.

### H12-C · Limitar cambios masivos (**prototipo implementado, apagado**)

**Regla** (`fitsense.adaptation.variante.max-sustituciones=N`, 0 = sin límite): en VARIANTE, como máximo *N* ejercicios añadidos por sesión. No toca el dolor.

**Análisis previo, sin fijar *N*:** distribución histórica de sustituciones por sesión (25 sesiones):

| Sustituciones por sesión | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Sesiones | 2 | 2 | 2 | 5 | 2 | 5 | 5 | 2 |

| *N* | Sesiones históricas que rechazaría | …de ellas, con **todas** las sustituciones a menor dificultad |
|---:|---:|---:|
| 1 | 23 | 1 |
| 2 | 21 | 0 |
| 3 | 19 | 0 |
| 4 | 14 | 0 |
| 5 | 12 | 0 |
| 6 | 7 | 0 |
| 7 | 2 | 0 |

**Hallazgo:** las sesiones con muchas sustituciones **no son solo las laterales**. U04 (GM intermedio) cambia 7 de 7 en las 5 corridas, y **5 o 6 de los 7 bajan la dificultad** en cada una. Por eso un límite numérico frena por igual el cambio masivo lateral y la simplificación legítima de una sesión entera (`08`: E09 y E10 pasan a SIN_EFECTO con *N* = 2). **No hay un *N* que separe los dos casos.**

### H12-D · Evitar rotaciones repetidas (**no implementado: requiere datos que V8C no recibe**)

**Regla propuesta:** si la sesión señalada por TOO_DIFFICULT contiene ejercicios que **entraron por una sustitución lateral** en la adaptación anterior, no volver a sustituirlos lateralmente. Tampoco reintroducir los retirados por dificultad en las últimas *k* semanas.

**Datos:** V8C solo recibe la semana medida; no sabe qué entró por sustitución (`06` §6.1). Haría falta:
- (a) pasar al contexto el plan anterior y la correspondencia de sesiones; o
- (b) registrar las sustituciones de VARIANTE en `conservation_report` (como las de seguridad).

(a) es cambio de código; (b) es cambio de esquema (JSON) sin migración. **Ninguno se ha hecho** («no implementar bloqueo permanente ni migraciones sin autorización»).

**Observación:** con H12-A activa, la rotación **lateral** no puede ocurrir, porque cada sustitución tiene que bajar la dificultad y la dificultad tiene un suelo (1). D solo añadiría valor frente a A en la vuelta atrás de una sustitución a menor dificultad, y no se observó.

## 2. Comparación obligatoria (fase 4.1)

| | H12-A Reducción demostrable | H12-B Mismo nivel justificado | H12-C Límite por sesión | H12-D Sin rotación |
|---|---|---|---|---|
| **Beneficio esperado** | Toda sustitución por TOO_DIFFICULT baja un dato registrado; desaparece el cambio lateral masivo y su rotación | Admitiría variantes del mismo nivel que de verdad son más fáciles | Evita reescribir la sesión entera | Evita volver a lo que ya resultó difícil y la sucesión de laterales |
| **Restricciones necesarias** | V8C por pareja (patrón + dificultad <); prompt con sustitutos < | Atributo o tabla curada por un profesional | Un *N* fijo | Historial de sustituciones en el contexto o en el registro |
| **GAIN_MUSCLE** | Principiante: sin dif. menor y sin dosis (4D) → **SIN_EFECTO** (4 de 4 sesiones históricas). Intermedio: las bajadas 2 → 1 siguen (29 de 35) | Depende de los datos | Intermedio: frena simplificaciones de toda la sesión (U04) | Igual que hoy hasta tener historial |
| **INCREASE_STRENGTH** | Intermedio: 11 bajadas siguen; principiante: dosis si cabe (R03) | Íd. | Frena 5 de 11 sesiones con *N* = 2 | Íd. |
| **LOSE_WEIGHT** | Principiante en margen 0 → SIN_EFECTO (R05); con margen, dosis | Íd. | Frena las de 6–8 ejercicios | Íd. |
| **GENERAL_FITNESS** | Sin efecto relevante: ya se resolvía con dosis (15 cambios) o con bajadas | Íd. | Poco impacto | Íd. |
| **Compatibilidad con V8C** | Se añade en la misma rama (VARIANTE, no dolor), después del patrón y de H1. Requiere emparejamiento (`03` §3) | Nueva consulta | Trivial | Requiere contexto nuevo |
| **V20** | No la toca. La dosis sigue validada por V20; no añade duración | — | No la toca | No la toca |
| **V24 y política 4D** | No la toca; no autoriza ninguna reducción prohibida (GM sigue sin bajar dosis) | — | No la toca | No la toca |
| **Sin alternativas** | Conserva la sesión (SIN_EFECTO): **comportamiento buscado** | Conserva | Hace lo de siempre (lateral hasta *N*) | Conserva lo rotado |
| **Riesgo de más SIN_EFECTO** | **Sí, acotado.** Histórico: 8 de 33 sesiones cambiadas pasarían a SIN_EFECTO (todas laterales: 4 GM principiante, 3 LW principiante, 1 FU principiante). Escenarios: 11 → 26 sesiones SIN_EFECTO | Bajo | **Sí, y sobre casos legítimos** (E09, E10) | Bajo-medio |
| **Riesgo de limitar sustituciones legítimas** | Bajo: rechaza laterales (no demostrables). Una lateral funcionalmente mejor (máquina asistida) se perdería hasta que B la respalde | — | **Alto:** no distingue el lateral del simplificado | Bajo |
| **Cambios de código** | Hechos, apagados: V8C (≈60 líneas), prompt y factibilidad (filtro <) | Datos y código, pendientes | Hechos, apagados (≈8 líneas) | Contexto o registro, pendiente |
| **Evaluación profesional** | Sí: ¿es aceptable conservar (o solo bajar dosis) cuando no hay variante más fácil? | **Imprescindible** | Sí, para fijar *N* | Sí, para la ventana *k* |

**No se elige la alternativa que más adaptaciones produce:**
- la que más produce es la histórica, con laterales sin límite;
- H12-A es la que **menos** produce, y produce solo las justificables por un dato.

## 3. Prototipos (fase 5.1)

| Prototipo | Propiedad | Por defecto | Dónde |
|---|---|---|---|
| H12-A | `fitsense.adaptation.variante.policy=reduccion-demostrable` | `historica` | `ConservationValidator`, `SessionFixPromptBuilder`, `FactibilidadAdaptacion` |
| H12-C | `fitsense.adaptation.variante.max-sustituciones=N` | `0` (sin límite) | `ConservationValidator` |

- Al activar cualquiera, la aplicación lo registra en cada contexto: `V8C: politica de VARIANTE EXPERIMENTAL activa…`.
- **El comportamiento predeterminado de TOO_DIFFICULT no cambia.**
- Ningún permiso de intervención, dosis, V20, V24, catálogo ni política aprobada se ha modificado.
- Pruebas: `V8CPoliticaVarianteH12Test` (10), `PromptVarianteH12Test` (4) y escenarios de `08`.
