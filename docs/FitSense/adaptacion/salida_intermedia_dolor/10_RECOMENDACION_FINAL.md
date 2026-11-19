# 10 · Recomendación final

## Resultado en una frase

La retirada localizada es **segura y no debilita ninguna regla**, pero su alcance es **pequeño**. Recupera 2 de las 7 semanas sin plan y convierte 1 de las 14 regeneraciones en una semana conservada. Las otras 5 semanas sin plan las bloquea **V20**, que no se toca.

## Respuestas

| Pregunta | Respuesta | Evidencia |
|---|---|---|
| **¿Cuántas de las siete semanas sin plan se recuperan?** | **2: R0521 y R0573**, ambos GF de 30 min. Se publica la semana medida sin el 135, con linaje 6/6, 0 dosis cambiadas y menos repeticiones (−18). | 05, `RetiradaLocalizada*IT` |
| **¿Cuántas siguen siendo NO_FACTIBLE y por qué?** | **5.** Hay dos causas:<br>• **V20 (4: R0524, R0527, R0570, R0574):** la sesión sin el 135 queda por debajo del 70 % de los minutos del perfil; faltan 309, 219, 291 y 21 s.<br>• **Sesión no señalada (1: R0545):** el 135 está también en una sesión **no señalada** por dolor, que esta vía no modifica; además, las dos sesiones incumplirían V20.<br>En las 5, P-03 tampoco puede publicar (`TRABAJO_SUPERIOR_AL_PERMITIDO`) y la semana sigue sin plan. La siguiente se recupera. | 01, 03, 05 |
| **¿Qué ocurre con las otras 14 semanas regeneradas?** | **1 (R0561, FU de 30 min)** pasa a retirada: se conserva la semana conocida sin el 135, con linaje 6/6 en lugar de 0/6. **13** siguen regeneradas por P-03: la retirada incumple V20 en 12 y en R0536 el 135 está también en una sesión no señalada. | 05 |
| **¿La retirada conserva las restricciones funcionales?** | **Sí.** Se publica solo si pasa V8C (condiciones del 02) y la validación estricta completa, sin ninguna excepción ni cambio en V20, V24, 4D o V23. El trabajo no sube, no se añade nada y ninguna otra dosis cambia. Si algo falla, el NO_FACTIBLE y P-03 de siempre. | 02, 03, 04 |
| **¿Se mantiene el linaje correctamente?** | **Sí:** 6/6 sesiones con `source_planned_workout_id` en las retiradas, frente a 0/6 en P-03. V32 registra la retirada como `RESPALDO`/`CONSERVED`, con la nota `RETIRADA_SEGURA` en `correcciones_del_backend`, según la semántica documentada del mapper. La sesión figura en `no_effect_sessions` porque en RESPALDO ese campo significa «sin efecto de la IA», igual que en una sustitución SUST-SEG-1. | 02 §Trazabilidad |
| **¿Se puede incorporar esta salida sin cambiar V20, V24 o 4D?** | **Sí.** No los toca ni los exime. Tampoco cambian migraciones, el JSON de V32 ni la API. | 02, 08 |
| **¿Conviene integrar la retirada localizada junto con C o mantener el flujo actual?** | **Si se activa C, conviene activar también la retirada (configuración D)**, por cuatro razones:<br>1. Solo actúa cuando no hay sustituto compatible.<br>2. Nunca publica lo que los validadores vigentes rechazan.<br>3. Donde actúa, conserva más del entrenamiento conocido que P-03, o evita una semana sin plan.<br>4. No cambia la REPLAY ni la generación.<br>Mientras no se activen ni C ni la retirada, el flujo actual es seguro. **Recomiendo no activar nada todavía**, hasta que decidas los puntos de abajo. | 05, 08 |

## Decisiones que te corresponden antes de activar

1. **Activar C + retirada (D) o mantener `off`.** Ninguna se activa sin tu aprobación.
2. **Retirada de una colocación estructural.** El 135 es estructural (UPPER_PULL) en los 21 perfiles. La implementación solo lo permite con **dolor registrado** y si el **slot sigue cubierto** en la semana. ¿Apruebas esa regla?
3. **La retirada no depende del límite.** También resuelve casos que ya son NO_FACTIBLE con A. Ejemplo: el **1373** (círculos de muñeca, relleno) de `RespaldoConDolorIT.C`, que con la retirada se publica sin el 1373 en vez de quedar sin plan. ¿Se permite la retirada con el límite apagado, o debe exigir C? Hoy son interruptores independientes.
4. **Contratos (no implementados):**
   - **P1:** valor de API `RETIRADA_SEGURA`; hoy la API devuelve `RESPALDO_SIN_IA`.
   - **P2:** clave `retiradas` en el informe V32.
   - **P3:** corregir el texto publicado de la **sustitución** SUST-SEG-1 en RESPALDO, que según el código dice «no aplicamos cambios». Es un hallazgo previo.
5. **R0545 y R0536: sesiones no señaladas.** Retirar el 135 también de una sesión no señalada **no** recuperaría estos perfiles, porque V20 lo impide igualmente. Por eso la restricción conservadora no cuesta nada en los datos medidos.
6. **Las 5 semanas restantes sin plan** solo se resolverían cambiando V20 o el catálogo, y ninguna de las dos cosas está autorizada. Queda como pregunta profesional, R-4 de #38: falta una alternativa de dificultad 1 al 135 en casa.

## Estado

| Elemento | Estado |
|---|---|
| PR #39 (`experiment/pain-localized-removal`) | **Borrador**, apilado sobre #38; `pain-localized-removal=off` por defecto; CI en el 09 |
| PR #38 | Sin cambios en su rama; sigue en borrador |
| Restricciones respetadas | Nada fusionado, activado ni desplegado. Ni Railway, ni Neon, ni Replicate. Sin cambios en V20, V24, 4D, adherencia, catálogo, Flutter ni en el paper |
