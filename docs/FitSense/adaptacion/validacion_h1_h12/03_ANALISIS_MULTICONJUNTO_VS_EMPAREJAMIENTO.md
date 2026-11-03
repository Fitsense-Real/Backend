# 03 · Multiconjunto frente a emparejamiento (fases 2.2 y 2.3)

## 1. Las dos alternativas

| | A · Multiconjunto | B · Emparejamiento |
|---|---|---|
| Regla | Para cada patrón *p*: nº de añadidos de *p* ≤ nº de retirados de *p* | Cada añadido se asocia a un retirado **distinto** del mismo patrón |
| Implementación | Contar por patrón (dos mapas) | Buscar un emparejamiento en un grafo bipartito (caminos de aumento) |
| Mensaje de error | «añade *n* de *p* [ids] y solo retira *m* de *p* [ids]» | «el añadido *x* no tiene retirado libre del mismo patrón» |

## 2. Con el patrón como único criterio, A y B son la misma regla

**Grafo:**
- un nodo por cada añadido y por cada retirado;
- una arista entre el añadido *a* y el retirado *r* si son del mismo patrón.

El grafo es la unión disjunta, patrón por patrón, de bipartitos **completos** (todos los añadidos de *p* son compatibles con todos los retirados de *p*).

**Por el teorema de Hall:** hay un emparejamiento que cubre a todos los añadidos ⇔ en cada componente, nº de añadidos ≤ nº de retirados.

Esa es exactamente la regla A, así que **A acepta y rechaza exactamente lo mismo que B**.

**Además:** en V8C, `Diferencias.anadidos` y `quitados` salen de las claves de un mapa (cada `exercise_id` aparece una sola vez por sesión). Por eso un mismo retirado no puede contarse dos veces, y A ya impide «usar repetidamente el mismo retirado para justificar varias incorporaciones», como pide B.

**Comprobado con el código real:**
- en las 32 pruebas de `V8CConservacionPatronH1Test`;
- en las 90 sesiones publicadas de `04` (ninguna diferencia entre «contar» y «emparejar», porque con un solo atributo no puede haberla).

## 3. Cuándo dejan de ser equivalentes

Si la compatibilidad de una pareja exige **más** que el patrón, el grafo deja de ser bipartito completo dentro de cada patrón, y contar ya no basta. Por ejemplo:
- dificultad del añadido < la del retirado (H12-A);
- o el mismo músculo principal (H2).

**Ejemplo:**
- retirados: *r1* (HPUSH, dif. 2) y *r2* (HPUSH, dif. 1);
- añadidos: *a1* (HPUSH, dif. 1) y *a2* (HPUSH, dif. 1).

El multiconjunto acepta (2 ≤ 2). Con «menor dificultad», *a1* y *a2* solo pueden emparejarse con *r1*, así que no hay emparejamiento completo y se rechaza.

**Consecuencia:**
- para **H1**, basta el multiconjunto;
- **si se adopta H12-A** (sustitución con reducción de dificultad), su regla necesita **emparejamiento**. Así se ha prototipado en la rama de H12 (`07`, `08`).

## 4. Elección para H1: multiconjunto (A)

| Criterio | A | B |
|---|---|---|
| Resultado con el patrón | Igual que B (§2) | Igual que A |
| Código | ~25 líneas, sin algoritmo nuevo | Algoritmo de emparejamiento (~40–60 líneas) |
| Integración con V8C | Encaja con el estilo actual (conjuntos y mapas por patrón) | Introduce una estructura nueva para un resultado idéntico |
| Mensaje para la IA en el reintento | Dice cuántos sobran de qué patrón y cuáles son | Dice qué añadido queda sin pareja; depende del orden de búsqueda |
| Extensible | No a criterios por pareja | Sí (H12-A, músculo) |

**Recomendación:** A para H1 (corrección mínima). B solo si se aprueba una regla por pareja (H12-A), y entonces dentro de esa regla.

## 5. Excepciones legítimas (fase 2.3): qué toca y qué no toca la regla

La regla nueva solo se evalúa en la **rama no dolorosa de `revisarSustitucion`**, y solo si hay añadidos. Esa rama solo la alcanza `VARIANTE_MAS_SENCILLA_O_CARGA`, la única intervención que permite sustituir sin dolor.

| Operación | ¿Pasa por la regla? | Comportamiento con MULTICONJUNTO | Evidencia |
|---|---|---|---|
| Adaptación **localizada** y **ampliada** | Solo en sus sesiones VARIANTE | Igual, salvo el desequilibrio de patrones | `04`: 59 sesiones reales (localizadas y ampliadas), 0 cambios |
| VARIANTE con sustituciones del mismo patrón (1 por 1) | Sí | Aceptadas | Prueba 2 (8 pares reales); `04`: 123 de 123 |
| VARIANTE que retira 2 y añade 1 del mismo patrón | Sí | Aceptada (la regla no obliga a reponer) | Prueba 8 |
| VARIANTE que **solo retira** | No: sin añadidos se sale antes | Aceptada, igual que antes | Prueba 7 |
| **Reducciones de dosis** sin sustitución (VARIANTE, FATIGA, INVESTIGAR) | No | Sin cambios | Pruebas 6 y 9 |
| FATIGA o INVESTIGAR que intentan sustituir | No: las rechaza el permiso de la intervención, como siempre | Sin cambios (un único mensaje) | Prueba 10 |
| **Dolor** (`SEGURIDAD_ANTE_DOLOR`) | No: rama propia, que sale antes | Sin cambios: otro patrón permitido, sin añadir trabajo | Pruebas 11 y 12; 22 pruebas de `SustitucionSeguridadTest` y 6 de `DolorSinMasTrabajoV8CTest` en el barrido de `05` |
| Retirada de un relleno (`ACORTAR_O_REORGANIZAR`) | No: rama de retirada | Sin cambios | `ConservationValidatorQuitarRellenoTest` (10) en el barrido |
| Progresión, reincorporación y sustituciones de seguridad declaradas | No (otras ramas de V8C) | Sin cambios | `ProgresionV24V8CTest`, `HerenciaReduccionV20Test` y las IT de `05` |
| Sesiones **no señaladas** | No: se comparan enteras como antes | No pueden cambiar, con ninguna regla | Prueba 13 |

**La regla no impone igualdad absoluta:**
- solo limita los añadidos **por patrón** al número de retirados de ese patrón;
- retirar sin reponer sigue siendo posible donde ya lo era.

**Cambio de comportamiento declarado:** fuera del dolor, **añadir dos ejercicios de un patrón retirando uno solo** pasaba con CONJUNTO y se rechaza con MULTICONJUNTO (prueba 4; la prueba 3 de `V8CDiagnosticoSustitucionesTest` documenta el comportamiento anterior).
- Ninguna de las 59 sesiones reales lo hizo (`04`).
- Es trabajo añadido en una sesión que el usuario encontró difícil.
