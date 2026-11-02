# Corrección de la aserción de respaldo en `ValidacionMultisemanaIT` (D1)

**PR independiente hacia `generation`:** rama `fix/asercion-respaldo-dolor` (ver `CIERRE_FINAL.md`). Pendiente de tu revisión, **sin fusionar**.

**Alcance:**
- Solo `src/test`; `AdaptedWeekProducer`, `SafetySubstitutionPolicy` y el validador no se tocan.
- **Ninguna aserción de seguridad eliminada.**

## 1. Qué había y por qué estaba mal

**Antes,** para `{}`, `{CONSERVAR}`, `SIN_EFECTO` y `RESPALDO`:
```java
for (int i = 0; i < antes.size(); i++)
    assertEquals(antes.get(i).getValue(), despues.get(i).getValue(), ...);   // igualdad absoluta
assertEquals(minutosAntes, estructura(plan, 0), ...);
...
assertTrue(publicado.isPresent(), "semana sin publicar");                    // publicar siempre
```

**Por qué fallaba:**
- Con `RESPALDO {SEGURIDAD_ANTE_DOLOR}`, P-03 nivel 1 sustituye por SUST-SEG-1 el ejercicio que dolió, como está aprobado. La igualdad absoluta fallaba, y además contradecía la aserción «lo que dolió no vuelve» de la misma prueba.
- Exigir `publicado.isPresent()` obligaba a publicar a cualquier coste, incluso cuando no hay una semana segura (caso C).

## 2. Qué hay ahora

Una utilidad de pruebas, `InvariantesRespaldo`, compartida con la prueba nueva:

| Caso | Qué se comprueba |
|---|---|
| **A**: sin orden, `{CONSERVAR}`, `SIN_EFECTO` o RESPALDO sin dolor | La semana es la medida: mismas sesiones; misma fecha +7, enfoque y minutos; mismos ejercicios en el mismo orden con la misma dosis. **Única excepción admitida:** una sustitución SUST-SEG-1 de un ejercicio con exclusión P-02 **activa**, **registrada en V32** con la regla SUST-SEG-1. La carga que V11 retira no se compara (las pruebas no comparan cargas). Ningún otro cambio, ni por semilla ni por variedad |
| **B**: RESPALDO con dolor | Lo mismo que A, y además: cada cambio está en la posición de un ejercicio **excluido**; el sustituto tiene el **mismo tipo** y la **misma clase de slot**; no sube series, repeticiones ni segundos; la sesión no sube su trabajo total; origen `CONSERVED`; nota `REGENERACION_SEGURA` en V32; «lo que dolió no vuelve» (la aserción de siempre) |
| **C**: con dolor y sin semana segura | Si no se publica: estado V32 `NO_FACTIBLE` con motivo; **ningún plan activo** para la semana; las exclusiones por dolor **siguen activas**; la secuencia se detiene ahí. **Sin dolor**, no publicar sigue siendo un fallo |

**Qué no se ha tocado:**
- las demás comprobaciones: intervenciones esperadas, «nada se recorta sin causa», «lo que dolió no vuelve» y el linaje sin violaciones;
- el resto de escenarios.

## 3. Resultados (PostgreSQL efímero, IA simulada, 0 llamadas a Replicate)

| Semana de referencia | Antes (aserción vieja) | **Ahora** |
|---|---|---|
| 2026-10-26 | 2 fallos («7 dolor» y «10 varios motivos», RESPALDO con dolor) | **57 contabilizadas, 55 ejecutadas, 0 fallos**, 2 omitidas (*opt-in*) |
| 2026-11-09 | 1 fallo («7 dolor») | **57 / 55, 0 fallos**, 2 omitidas |
| 2026-11-02 (la fijada) | 0 fallos | **57 / 55, 0 fallos**, 2 omitidas |

Las tres semanas se ejecutaron con `-Dfitsense.pruebas.semana-referencia`. Ahora la prueba es **robusta a la semilla**: comprueba lo que la política exige, no la composición concreta de una semana.

**Lo que no se relajó:** con la aserción nueva, cualquier cambio no autorizado sigue fallando:
- un ejercicio no excluido sustituido;
- un cambio sin traza en V32;
- un sustituto de otro tipo o slot;
- una dosis o un trabajo mayores;
- otros minutos;
- un plan publicado con algo que dolió.
