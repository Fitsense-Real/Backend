# Corrección de V8C · hallazgos H-A1 y H-A2

**Fecha:** 10/11/2026 · **Rama:** `experimento/mejora-ia` · **Commits:** `5bea428` (corrección y pruebas) y `ab04e67` (pruebas dependientes de la fecha, aparte).

**Qué es V8C:** la validación de conservación de las adaptaciones (`ConservationValidator`). Comprueba que en una sesión señalada la IA solo cambió lo que permite su tipo de intervención (`TipoIntervencion`), y que las no señaladas se conservan.

**Alcance:**
- Se corrigen solo los dos huecos que encontró la auditoría de MEJ ronda 2 (`AUDITORIA_REGLAS_FITSENSE.md`, §H-A1 y §H-A2).
- Lo que se hace es aplicar en V8C lo que la política ya dice:
  - no se añade ninguna regla ni ningún validador;
  - no cambian los permisos de `TipoIntervencion`;
  - no cambia ningún valor de descanso de las capacidades o clases de dosis.
- **La generación inicial no pasa por V8C**: V24 y V1–V27 de generación no cambian.

---

## 1. H-A1 · Retiradas no autorizadas

### Causa
- `revisarSustitucion` (para las intervenciones con permiso de sustituir) terminaba con:

  ```java
  if (d.anadidos().isEmpty()) return;   // solo se retiraron: nada que emparejar
  ```

- Si la propuesta solo retiraba ejercicios, V8C volvía **sin comprobar nada**. Con más retiradas que añadidos, el emparejamiento multiconjunto solo miraba que cada añadido tuviera un retirado de su patrón, no lo contrario.
- **Resultado:** `VARIANTE_MAS_SENCILLA_O_CARGA` aceptaba propuestas que retiraban uno o varios ejercicios, estructurales incluidos.

### Qué dice la política (sin cambios)

| Intervención | Retirar ejercicios | Fuente |
|---|---|---|
| `VARIANTE_MAS_SENCILLA_O_CARGA` | **No.** «Buscar una variante más sencilla del mismo patrón de movimiento, o ajustar la carga» | `TipoIntervencion` |
| `ACORTAR_O_REORGANIZAR` | **Una** colocación de relleno, con el resto intacto | `TipoIntervencion.permiteQuitarEjercicio()` |
| `SEGURIDAD_ANTE_DOLOR` | **Sí**: «retirarlo sin reemplazo también vale» | `TipoIntervencion` |
| Las demás (fatiga, motivación, reprogramar, causa no registrada) | No | `TipoIntervencion` |

### Corrección (`ConservationValidator.revisarSustitucion`)
- Fuera de `SEGURIDAD_ANTE_DOLOR`, **cada ejercicio retirado necesita su reemplazo**.
- Si los retirados superan a los añadidos, la propuesta se rechaza con un motivo explícito.
- Los ejercicios que dolieron no cuentan como retirada sin reemplazo: la política de dolor nunca obliga a mantenerlos.
- `ACORTAR_O_REORGANIZAR` sigue por su rama propia, sin cambios (una de relleno, sin tocar las demás dosis).
- Las sustituciones del mismo patrón, el multiconjunto, el límite de dificultad y el impacto siguen igual.

---

## 2. H-A2 · Cambios de descanso

### Causa
- En una sesión señalada, el cambio de dosis se juzga en `revisarDosisDeLaIa` (subidas) y en `revisarSenalada` (permiso de dosis).
- Un cambio **solo de `rest_seconds`** no es una subida y entra en el permiso de dosis. Por eso se aceptaba en cualquier intervención con `permiteCambiarDosis` (acortar, variante, dolor, fatiga, motivación).
- Lo mismo pasaba con una bajada válida de series que llevaba además otro descanso.
- Antes, el descanso solo se miraba para no emitir la reducción para V24.

### Qué dice la política (sin cambios)
- La instrucción de corrección (`SessionFixPromptBuilder`) dice: «El descanso (rest_seconds) de un ejercicio que se queda no se cambia nunca: ni para acortar ni para alargar».
- V24 define «bajar la dosis» como menos series, repeticiones o segundos, **nunca el descanso**.
- El oráculo independiente K20 ya lo comprueba: «no hay menos descanso».

### Corrección (`ConservationValidator.revisarDosisDeLaIa`)
- Si un ejercicio que **se queda** en la sesión señalada vuelve con otro descanso, la propuesta se rechaza.
- **Los sustitutos no se comparan:** un ejercicio nuevo no tiene descanso previo y lleva la prescripción propia de su clase, que juzgan V16, V17 y V24.
- Si la semana medida no registra el descanso (`null`), no hay nada que comparar.

---

## 3. Pruebas

### Nuevas: `V8CRetiradasYDescansoTest` (25 pruebas)

La sesión medida tiene tres ejercicios a 3×10 con 90 s de descanso: A (HORIZONTAL_PUSH, estructural), B (KNEE_DOMINANT, estructural) y C (CORE, relleno).

| Grupo | Pruebas | Qué demuestran |
|---|---|---|
| **H-A1 negativas (7)** | VARIANTE retira un estructural · retira varios · sustituye A→X y retira C · retira el relleno · FATIGA retira · ACORTAR retira un estructural · ACORTAR retira dos | Todas rechazadas |
| **H-A1 positivas (8)** | VARIANTE sustituye por el mismo patrón (una y dos) · sustituye y baja una dosis · patrón distinto sigue rechazado · ACORTAR retira una de relleno · DOLOR retira sin reemplazo · DOLOR sustituye uno por uno · VARIANTE sin cambios = sin efecto | Lo autorizado sigue funcionando |
| **H-A2 negativas (5)** | Solo descanso en ACORTAR, FATIGA, VARIANTE y MOTIVACIÓN · bajada válida (3×10 → 2×10) con el descanso cambiado | Todas rechazadas |
| **H-A2 positivas (5)** | Bajada de series o repeticiones con el mismo descanso · sustituto con su propio descanso (VARIANTE y DOLOR) · una sesión no señalada sigue sin poder cambiar el descanso | Sin cambios de comportamiento |

**El defecto queda demostrado:**
- Sobre el validador **sin corregir**, las mismas 25 pruebas dan **9 fallos**: las 4 negativas de H-A1 con VARIANTE y las 5 negativas de H-A2.
- Las demás negativas (FATIGA y ACORTAR) ya se rechazaban.
- Con la corrección pasan las 25. El registro está en la evidencia: `sin_correccion.log`.

### Pruebas existentes que fijaban el comportamiento defectuoso (se invierten)

| Prueba | Antes | Ahora | Motivo |
|---|---|---|---|
| `V8CConservacionPatronH1Test` 7 «VARIANTE que retira sin añadir» | aceptada («la regla no prohíbe retiradas») | rechazada | H-A1 |
| `V8CConservacionPatronH1Test` 8 «VARIANTE retira 2 de un patrón y añade 1» | aceptada | rechazada | H-A1: una retirada sin reemplazo |
| `AdaptedWeekProducerTest` C4 «menos descanso con permiso de dosis» | APLICADA | rechazada | H-A2 |

Notas sobre estas pruebas:
- Las tres describían el comportamiento del código, no una decisión de política.
  - La prueba 7 dice expresamente que la regla H1 «no prohíbe retiradas».
  - La C4 (27/10/2026) servía para comprobar que un cambio de dosis real cuenta como APLICADA. Ese caso lo cubren sus vecinas «menos series» y «menos repeticiones».
- Ninguna política se ha modificado para que las pruebas pasen.

---

## 4. Lo que no cambia

| Ámbito | Estado |
|---|---|
| V24, V25 y los límites de V15/V26 | Sin cambios. La generación inicial no usa V8C |
| Estructuras de los cuatro objetivos, catálogo y filtros | Sin cambios |
| Progresión y reincorporación | Sin cambios. El paso de progresión lo hace el backend y no pasa por `revisarDosisDeLaIa` |
| Sustitución por seguridad (P-02), herencia D8 y tope declarado | Sin cambios |
| `TipoIntervencion` (permisos e instrucciones) | Sin cambios |
| Prompts | Sin cambios. La instrucción de corrección ya dice que el descanso no se cambia nunca, y la de VARIANTE pide una variante del mismo patrón o ajustar la carga |

## 5. Regresión

Ver `PRUEBAS_Y_REGRESIONES.md`.
