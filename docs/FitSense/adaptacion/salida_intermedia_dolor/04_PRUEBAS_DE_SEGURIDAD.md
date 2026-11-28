# 04 · Pruebas de seguridad

Pruebas **permanentes**: corren en CI y no son opt-in. Ninguna prueba histórica se ha modificado.

| Clase | Tipo | Resultado |
|---|---|---|
| `RetiradaLocalizadaV8CTest` | unitaria, V8C real | **13/13** |
| `RetiradaLocalizadaLimiteIT` (límite `on` + retirada `on`) | PostgreSQL efímero, servicios reales, IA apagada, perfiles congelados | **4/4** |
| `RetiradaLocalizadaPreferenciaIT` (límite `preferencia` + retirada `on`) | ídem | **4/4** |
| `RetiradaLocalizadaSinLimiteIT` (límite `off` + retirada `on`) | ídem | **1/1** |
| `RetiradaLocalizadaPorDefectoIT` (sin fijar nada) | ídem | **1/1** |

## Comprobaciones pedidas (fase 7)

| Comprobación | Dónde | Resultado |
|---|---|---|
| El ejercicio doloroso nunca reaparece automáticamente | `assertRetiradaPublicada` / `assertRetiradaRechazada`: el 135 no está en ningún plan, ni el de la semana nueva ni el de la siguiente (`assertSemanaSiguiente`); V8C `siguePrescrito` | ✔ |
| No se introduce otro ejercicio excluido | V8C `ademasAnade`: nada añadido; el elegible P-02 no contiene excluidos; V1 | ✔ |
| No aumentan las dosis | contenido idéntico, ejercicio a ejercicio (series, repeticiones, segundos, descanso), salvo el 135; V8C `ademasDosis` | ✔ |
| No se incrementa el trabajo semanal | `trabajo(nuevo) ≤ trabajo(medido)`; `masTrabajoQueLaMedida` en el productor | ✔ |
| No se modifican sesiones no señaladas | todas las demás sesiones idénticas; V8C `noSenaladaIntacta`; R0545 (135 también en una no señalada): no se retira | ✔ |
| No se publica una sesión que incumpla V20 | R0487 y R0524: retirada rechazada por V20 | ✔ |
| No se rompe V24 ni 4D | validación estricta completa antes de publicar (la misma de siempre) | ✔ |
| No se elimina una colocación estructural sin autorización | solo con dolor registrado (`otraCausa`) y con el slot cubierto en la semana (`slotSinCobertura`) | ✔ |
| No se alteran las reglas de adherencia | ningún cambio de código en adherencia; REPLAY `metricas.csv` (08) | ✔ |
| La publicación es idempotente | R0521: volver a correr el ciclo de esa semana mantiene el mismo plan y un solo activo | ✔ |
| No se crean dos planes activos | `activos(u, semana) == 1` en la nueva y en la siguiente | ✔ |
| Continuidad multisemana correcta | `assertSemanaSiguiente`; laboratorio de 22 perfiles (06) | ✔ |
| Si falla la retirada, P-03 sigue funcionando | R0487: retirada rechazada por V20 → P-03 `PUBLICADO`, API `REGENERACION_SEGURA_REGLAS` | ✔ |
| Si P-03 también falla: NO_FACTIBLE sin plan inválido | R0524: P-03 `NO_FACTIBLE`, sin plan, API `NO_FACTIBLE_POR_SEGURIDAD` | ✔ |
| Solo para dolor | V8C `otraCausa` (brazos_arriba); el productor solo entra desde el respaldo con dolor cubierto por P-02 | ✔ |
| Con B y con C | `…LimiteIT` y `…PreferenciaIT`: mismos 4 escenarios, mismos resultados | ✔ |
| Si hay sustituto, no se retira | `RetiradaLocalizadaSinLimiteIT`: con A, SUST-SEG-1 pone el 780 y no hay nota de retirada | ✔ |
| Apagada por defecto | `RetiradaLocalizadaPorDefectoIT`; parser `off`/`on`/error en `configuracion` | ✔ |
| N08 no cambia | REPLAY con D (08): N08 tiene sustituto (1003), así que la retirada no actúa | ver 08 |
