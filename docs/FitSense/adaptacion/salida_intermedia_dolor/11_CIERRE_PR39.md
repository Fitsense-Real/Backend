# 11 · Cierre del PR #39

Este documento aplica las decisiones del responsable sobre los resultados del 01–10. **No se inicia ningún experimento nuevo para recuperar las cinco semanas restantes sin plan.** V20, V24, 4D y el catálogo no se modifican.

## Decisiones y cómo quedan aplicadas

| # | Decisión | Estado en la rama |
|---|---|---|
| 1 | #38 y #39 siguen en borrador, sin fusionar ni activar | Sin cambios. Los dos interruptores siguen en `off` por defecto |
| 2 | Se acepta experimentalmente retirar una colocación **estructural**, solo ante dolor registrado, con la cobertura semanal preservada y la validación estricta completa | Es la regla ya implementada en V8C (`RetiradaSeguridad`, 02 §Condiciones). No hay código nuevo |
| 3 | La retirada localizada **no** puede actuar sin el límite de dificultad | `1c1f1a1`:<br>• `AdaptedWeekProducer` solo activa la retirada si `difficulty-guard` es `on` o `preferencia`;<br>• con el límite en `off`, `pain-localized-removal=on` se ignora y se escribe un aviso al arrancar;<br>• `RetiradaLocalizadaSinLimiteIT` comprueba que el valor se lee pero no actúa (R0521 recibe el 780, como en A) |
| 4 | P1 (valor de API `RETIRADA_SEGURA`) y P2 (clave `retiradas` en V32) **se posponen** | No implementados. La API sigue devolviendo `RESPALDO_SIN_IA`, y V32 registra la retirada con la nota `RETIRADA_SEGURA` en `correcciones_del_backend` |
| 5 | Preparar una prueba de P3 | `f95c02f`: `TextoRespaldoSustitucionIT`, opt-in (ver abajo). **Falla hoy**: el defecto queda confirmado |
| 6 | Documentar las tres pruebas que fallan y cómo verificar los dos comportamientos sin ocultar diferencias | `3b72de2`: `verificar_configuraciones.sh` y `RetiradaLocalizada1373IT` (ver abajo) |
| 7 | Registrar aparte el caso R0548 | `ab8c971`: `GeneracionR0548V20IT`, opt-in (ver abajo) |

## P3 · El texto de un respaldo con sustitución

**Prueba:**
```
sh ./mvnw -o -B -ntp verify -Pintegration -Dit.test=TextoRespaldoSustitucionIT -Dfitsense.verificacion.p3=true
```

**Escenario:**
- configuración por defecto (límite `off`);
- R0521 registra dolor en el 135;
- la IA falla;
- SUST-SEG-1 coloca el 780.

**Resultado de hoy:** **falla**. El texto publicado es:

> Reportaste molestias. En una de las sesiones no aplicamos cambios esta semana: se mantiene como estaba.

Es falso: la sesión **sí** cambió, porque el 135 se sustituyó por el 780.

La prueba exige:
- que el texto no diga «no aplicamos cambios»;
- que diga «Por seguridad, cambiamos o retiramos 1 ejercicio».

**Causa:** en `DescripcionAdaptacion.clasificar` (línea 78), el cambio de seguridad solo se reconoce si hay una nota `RETIRADA_SEGURA`:

```java
boolean cambioDeSeguridad = retiradaSegura && derivada.isPresent() && ...
```

Una sustitución SUST-SEG-1 no lleva esa nota y acaba clasificada como «sin cambios».

**Corrección candidata (una línea, NO aplicada):** quitar `retiradaSegura &&`. Así, cualquier sesión de respaldo que difiere de su origen se describe como un cambio de seguridad.

Se probó en local y después se **revirtió**:
- `TextoRespaldoSustitucionIT` pasa;
- `DescripcionAdaptacion*Test` (11), `AdaptacionCompletaIT` (22), `RespaldoConDolorIT` (3) y `ConservacionPatronH1IT` (4) siguen pasando.

Hay que aprobarla para aplicarla: cambia el texto que ven los usuarios con la configuración por defecto.

**Por qué la prueba es opt-in:** fallaría en CI hasta que se apruebe la corrección. Cuando se apruebe, debe dejar de ser opt-in, sin cambiar sus aserciones.

## Las tres pruebas que fallan con la configuración experimental

Con `difficulty-guard=preferencia` y `pain-localized-removal=on` pasados por `-D`, fallan **exactamente** tres pruebas. Las tres fijan el comportamiento **por defecto** o el **histórico**. Ninguna se ha modificado ni desactivado.

| Prueba | Qué fija | Por qué falla con la configuración experimental | Contrapartida positiva |
|---|---|---|---|
| `ConservacionPatronH1IT.reglaPorDefecto` | El límite viene **apagado** por defecto | `-D` lo enciende | `LimiteDificultad*IT` (su propia configuración) |
| `RetiradaLocalizadaPorDefectoIT.apagadaPorDefecto` | La retirada viene **apagada** por defecto | `-D` la enciende | `RetiradaLocalizadaLimiteIT`, `RetiradaLocalizadaPreferenciaIT` |
| `RespaldoConDolorIT.conDolorSinSustitutoNoPublicaNadaInvalido` | El caso del **1373** (relleno de movilidad, sin sustituto): NO_FACTIBLE sin publicar | Con la retirada, la semana **se publica** sin el 1373. Las aserciones de seguridad de la misma prueba se cumplen; solo falla la que fija el desenlace histórico | **`RetiradaLocalizada1373IT`** (nueva, permanente) |

**`RetiradaLocalizada1373IT`** fija su configuración: límite `preferencia`, retirada `on` e IA que siempre falla. Así se ejecuta en CI y comprueba, sobre el mismo ejercicio, el comportamiento experimental:
- la semana se publica en RESPALDO, con la nota `RETIRADA_SEGURA`;
- el 1373 no aparece y su exclusión sigue activa;
- las demás sesiones quedan idénticas;
- en la sesión retirada, la duración baja de 37 a 34 min.

**Cómo verificar los dos comportamientos:**

```
sh docs/FitSense/adaptacion/salida_intermedia_dolor/verificar_configuraciones.sh
```

El script ejecuta dos corridas y lee los informes XML de surefire y failsafe, borrados antes de cada una:

| Corrida | Configuración | Resultado exigido |
|---|---|---|
| 1 | Por defecto | **0 fallos** |
| 2 | Experimental (`-D`) | Fallan **exactamente** las tres pruebas de la tabla |

En la corrida 2:
- un fallo más es una **regresión**;
- un fallo menos significa que el cambio funcional esperado **no ocurrió**.

Los dos casos hacen fallar el script, así que no se ocultan diferencias en ninguna dirección.

**Resultado del script sobre `ab8c971`:** §Verificación, abajo.

## R0548 · La generación inicial incumple V20

Es un hallazgo **de generación**, independiente de la retirada y de la adaptación. Se registra por separado y no se corrige aquí (V20 y el catálogo no se tocan).

| | |
|---|---|
| Perfil | GAIN_MUSCLE, intermedio, casa (peso corporal, mancuernas y bandas), 7 días, 75 min |
| Síntoma | `PlanGenerationFailedException`, V20: la sesión suma 2.550 s y el mínimo es 3.150 s (70 % de 75 min). Faltan 600 s, en 6 sesiones |
| ¿Depende de la semilla? | **No.** Falla igual con el usuario 547 en la semana 2026-11-02 y con el usuario 12 en otra semana |
| Efecto en el laboratorio de 21 perfiles | R0548 no tiene plan inicial válido. Se excluyó de la comparación A/B/C/D, porque no hay semana que adaptar, y se informó aparte (05, 07) |
| Prueba | `GeneracionR0548V20IT`, opt-in: `-Dit.test=GeneracionR0548V20IT -Dfitsense.registro.r0548=true`. Afirma el fallo actual, como registro y no como especificación. Si una corrección futura lo resuelve, la prueba fallará y habrá que convertirla en prueba de éxito |
| Pregunta abierta | ¿Composición del generador o falta de ejercicios de casa para 75 min y 7 días? Hay que decidirlo fuera del PR #39, sin cambiar V20 |

## Verificación

**CI (`verify`, configuración por defecto) sobre `ab8c971`:** ✔, run 41776942778.

**Script sobre `ab8c971`** (`evidencia/verificacion_ab8c971_v1.txt`):

| Corrida | Unitarias | Integración | Fallidas |
|---|---|---|---|
| 1 · por defecto | 1.185 / 0 / 0 | 521 ejecutadas: 479 aprobadas, 0 fallidas, 42 omitidas | **ninguna** |
| 2 · experimental | 1.185 / 0 / 0 | 521 ejecutadas: 476 aprobadas, **3** fallidas, 42 omitidas | **las 3 esperadas** |

Las omitidas son los laboratorios opt-in: las 40 de antes, más P3 y R0548.

La primera versión del script dio aun así «DIFERENCIA», y el defecto estaba en **el propio script**, no en las pruebas:
- JUnit 5 escribe el `@DisplayName` en el atributo `classname` de los XML;
- por eso los nombres no coincidían con la lista esperada.

El analizador ahora toma la clase del nombre del fichero (`TEST-<clase>.xml`). Aplicado a los mismos informes de la corrida 2, devuelve exactamente las tres esperadas. La repetición completa del script corregido figura en `evidencia/verificacion_v2.txt`.

## Estado

| Elemento | Estado |
|---|---|
| PR #39 | **Borrador**, apilado sobre #38, interruptor `off` |
| PR #38 | Sin cambios; borrador |
| P3 | Defecto confirmado y corrección de una línea preparada. **Pendiente de aprobación** |
| P1, P2 | Pospuestos |
| R0548 | Registrado; sin corrección |
| Restricciones | Nada fusionado, activado ni desplegado. Ni Replicate, ni Railway, ni Neon. Sin cambios en V20, V24, 4D, adherencia, catálogo, Flutter ni en el artículo. Sin migraciones |
