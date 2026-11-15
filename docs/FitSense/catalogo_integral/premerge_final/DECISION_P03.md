# Decisión P-03 por colocación

## 1. Decisión del propietario (orden premerge final, fase 2.1)

Se **autoriza** incorporar y **habilitar** la protección P-03 por colocación en la candidata final, **solo para pruebas y preparación de la integración**. **No autoriza** activarla en producción.

**Motivo (defecto potencial demostrado):** P-03 nivel 2 comparaba solo el trabajo **total**. Podía aceptar una semana regenerada en la que una colocación reducida por adaptación (3×10 → 2×10) vuelve a 3×10 sin autorización, mientras otra colocación baja y el total no sube.

**Principio que se mantiene:** una reducción aplicada por adaptación no se restaura sin autorización, aunque el trabajo total no cambie.

## 2. Cómo se materializa

| Elemento | Valor |
|---|---|
| Código | `ComparacionDosisPorColocacion` (dominio, sin dependencias), usado por `SafeRegenerationService.subidaPorColocacion` cuando el total no sube |
| Interruptor | `fitsense.regeneracion.p03-por-colocacion` |
| Producción | **false**: `application.properties` → `${FITSENSE_P03_POR_COLOCACION:false}`. Para activarla hace falta una autorización expresa y la variable de entorno |
| Candidata y todas sus pruebas | **true**: `src/test/resources/config/application.properties`. Spring Boot lo carga además del `application.properties` principal, con prioridad sobre él. No se empaqueta en el jar |
| Commit | `12c6897` (rama `cierre/catalogo-premerge-final`) |

## 3. Reglas de comparación

| Tipo | Cuándo | Se compara con |
|---|---|---|
| MANTENIDA | Mismo ejercicio y mismo día de la semana (por orden de aparición si se repite) | La colocación medida correspondiente |
| SUSTITUCION | Mismo día y misma posición que un ejercicio medido **retirado** (exclusión activa) que ya no aparece | La dosis del retirado |
| SIN_CORRESPONDENCIA (regla conservadora) | El ejercicio estaba en la semana medida, pero otro día o más veces | La **menor** dosis medida de ese ejercicio en la semana, **nunca el máximo semanal** |
| NUEVA | El ejercicio no estaba en la semana medida | Con nada por colocación: su dosis es la base de su capacidad (V24) y cuenta en el total |
| RETIRADA | Colocación medida sin pareja | Solo baja trabajo |

**Dimensiones que no pueden subir:** series, repeticiones, segundos, series × repeticiones y series × segundos.

**La comprobación del trabajo total se conserva**, y se evalúa antes.

## 4. Lo que no cambia

- **La generación inicial no cambia.** P-03 solo actúa en la regeneración ante dolor NO_FACTIBLE.
- **Las progresiones autorizadas siguen igual.** P-03 regenera con dosis base, que nunca superan una dosis progresada. La progresión (V31) sigue su propio camino, que la regla no toca.
- **V24, V8C, D8, V15, V20 y V25 no cambian.** No se tocó ningún validador.

## 5. Estado

**Aprobada para la candidata.** Las pruebas obligatorias y los 40 escenarios pasan, y no hay ninguna regresión (`VALIDACION_P03_POR_COLOCACION.md`).

**Activarla en producción requiere una autorización posterior** y la variable `FITSENSE_P03_POR_COLOCACION=true`.
