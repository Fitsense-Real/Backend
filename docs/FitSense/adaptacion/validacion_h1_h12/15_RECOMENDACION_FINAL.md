# 15 · Recomendación final y decisiones (fases 11 y 12)

## Resumen

| Fase | Estado | Clasificación |
|---|---|---|
| **H1** | **Técnicamente satisfactoria.** Detecta la pérdida de patrón, acepta las 123 sustituciones legítimas, no toca la generación, no tiene regresiones y queda aislada, revisable y apagada | **Recomiendo autorizar su activación** (`multiconjunto`) |
| **H12** | **Definida.** Se entiende la causa, se distinguen el lateral, la reducción observable y la simplificación funcional, hay 4 alternativas comparadas y 2 probadas, se conoce el impacto en los cuatro objetivos y hay una política concreta propuesta. Todo queda apagado | **Recomiendo H12-A**, condicionada a la evaluación profesional de §19. No la activo |

**No se ha activado nada:**
- V2 sigue apagado.
- No se ha fusionado ningún PR.
- No se ha tocado V20, V24, dosis, catálogo, políticas, Railway, Neon, Flutter ni el short paper.
- Sin Replicate.

## Respuestas (fase 11)

1. **¿Por qué V8C aceptaba perder un patrón?**
   - Comprobaba que el patrón de cada añadido **perteneciera al conjunto** de patrones retirados, sin contar cuántos de cada patrón.
   - En U04 el retirado 380 (HORIZONTAL_PUSH) «justificaba» a la vez a 56 y a 72, y el 389 (VERTICAL_PULL) quedaba sin reemplazo de su patrón.
   - Es un hueco de la regla, no de implementación (`02` §2).
2. **¿Corrección mínima recomendable?** **Multiconjunto:** por patrón, añadidos ≤ retirados. Son unas 25 líneas en `revisarSustitucion`, con la propiedad `fitsense.adaptation.v8c.pattern-rule` (`03` §4, `05` §1).
3. **¿El multiconjunto es suficiente?**
   - **Sí, para H1.** Con el patrón como único criterio equivale exactamente al emparejamiento (teorema de Hall), y los ids son únicos por sesión, así que un retirado no puede justificar dos añadidos (`03` §2).
   - **No basta** si se añaden criterios por pareja (dificultad <, músculo). Para eso H12-A usa emparejamiento (`03` §3; prueba 5 de `V8CPoliticaVarianteH12Test`).
4. **¿Cuántas de las 123 sustituciones seguirían siendo válidas?** **123 de 123**, con el V8C real: GM 56/56, LW 32/32, FU 26/26 y AG 9/9. Rechazadas, 0; con excepción, 0 (`04`).
5. **¿Se afectan las sustituciones por dolor?**
   - **No:** la rama de dolor sale antes de la regla.
   - Sigue admitiendo otro patrón y sigue prohibiendo añadir trabajo (pruebas 11 y 12).
   - `SustitucionSeguridadTest` y `DolorSinMasTrabajoV8CTest` pasan con MULTICONJUNTO por defecto, y las IT de dolor pasan con la regla activa (`05` §2.2–2.3).
6. **¿Se comprobó U04 antes y después?** **Sí:**
   - en unitarias, con datos reales del catálogo: antes aceptada, después rechazada, y corregida aceptada;
   - de extremo a extremo: antes publicada con 3 HPUSH y 1 VPULL; después, rechazo y reintento con un VERTICAL_PULL (`02` §3).
7. **¿Alguna regresión?** **No.**
   - **Con todo apagado:** 1.119 unitarias y 489 IT, 0 fallos (37 *opt-in*); 960 perfiles idénticos; REPLAY igual (`12` §1).
   - **Con H1 activa:** 0 fallos en las IT completas; 960 y REPLAY idénticos (`12` §2).
   - **Con H1 + H12-A activas (prototipo):** 1 fallo **esperado**, una IT de control que espera una VARIANTE lateral (`12` §3, K17).
   - **Único cambio de comportamiento:** «añadir 2 de un patrón retirando 1» fuera del dolor, declarado; 0 casos reales.
8. **¿Por qué R01 y R05 cambiaban tantos ejercicios?** Por la combinación de (`06` §6):
   1. principiantes con `max_difficulty_level = 1`: todo su elegible tiene dificultad 1, así que no hay variante «más sencilla» en el catálogo;
   2. dosis cerrada (4D en GM para R01; margen 0 en R05);
   3. VARIANTE permite sustituir por el mismo patrón sin exigir menor dificultad;
   4. el prompt ofrece sustitutos de dificultad ≤;
   5. nada limita ni el número ni la repetición.
9. **¿Las sustituciones de la misma dificultad producían alguna simplificación observable?**
   - **No observable con los datos registrados.** Dificultad 1 → 1 y la misma dosis en las 59 sustituciones de R01 y R05.
   - Algunas (máquinas asistidas, apoyo de rodillas) **podrían** ser más fáciles en la práctica, pero el catálogo no lo mide. Es una **sustitución válida**, no una reducción observable ni una simplificación demostrada (`07` §0).
   - Aun así, la justificación de la IA decía «más sencillas» (`06` §3).
10. **¿Existían alternativas de dificultad inferior?**
    - **Para principiantes, ninguna** (0 de 146 ejercicios).
    - Para intermedios, 52 de 93 ejercicios las tenían, y la IA bajó la dificultad en 41 de 64 sustituciones (`06` §5).
11. **¿Se podía reducir la dosis?**
    - En **22 de 38** sesiones TOO_DIFFICULT cabía alguna reducción.
    - **No** en GM (4D: 9 sesiones) ni en las de margen 0 (4) o margen pequeño (`06` §5).
    - **R01:** no (4D). **R05:** no (margen 0).
12. **¿Cuántos cambios fueron solo laterales?** **82 de 123** sustituciones, y **59 de 59** en principiantes (`06` §5).
13. **¿Qué pasó en la segunda semana?**
    - Con IA real, **18 sustituciones** volvieron a cambiar ejercicios recién introducidos (R01 en C1: 3 y en C2: 6; R03 en C1: 1; R05 en C2: 8).
    - R05 C2 **reintrodujo dos ejercicios de la sesión que la persona había declarado difícil** (1054 y 253).
    - En escenarios controlados, R01 rota A → B → A en tres semanas (`06` §4; `08`).
14. **¿Política recomendada cuando no existe una variante más sencilla?** **H12-A (reducción demostrable):**
    1. variante de **menor** dificultad del mismo patrón, si existe;
    2. si no, **reducción de dosis** donde ya está permitida (sin tocar 4D, V16 ni V20);
    3. si tampoco, **conservar la sesión** (SIN_EFECTO), con un mensaje honesto.

    Nunca una sustitución lateral presentada como «más sencilla». Los laterales con posible valor real quedan para H12-B, con datos curados por un profesional (`07`).
15. **¿Conviene limitar las sustituciones por sesión?**
    - **No con un número fijo (H12-C).** Las sesiones con muchas sustituciones son tanto las laterales (R01, R05) como las de simplificación legítima (U04: 7 de 7, con 5–6 bajadas).
    - Con N = 2, H12-C bloquea E09 y E10, que eran bajadas reales (`07`, `08`).
    - H12-A resuelve el problema sin un número.
16. **¿Conviene impedir rotaciones repetidas?**
    - **Sí, el objetivo es correcto**, pero **H12-A ya impide la rotación lateral** (cada sustitución debe bajar y la dificultad tiene suelo).
    - H12-D solo añadiría valor con historial de sustituciones, que hoy no llega a V8C (`06` §6.1).
    - **No implementarla todavía.**
17. **¿Impacto de cada alternativa en SIN_EFECTO?**

    | Alternativa | Escenarios (41 filas sesión × semana) | Histórico (38 sesiones TOO_DIFFICULT) |
    |---|---|---|
    | Histórica | 11 | 5 |
    | H1 | 12 | 5 |
    | H12-A | **26** | ~13 (+8 laterales sin salida) |
    | H12-C (2) | 24 | — (no estimable sin modelo) |

    El aumento de H12-A se concentra en GM principiante, LW principiante en margen 0 y FU principiante (`09`).
18. **¿Impacto en los cuatro objetivos?** Ver `11`.
    - **GM:** el principiante queda SIN_EFECTO sin alternativa, y el intermedio conserva sus bajadas.
    - **FU:** se mantienen las bajadas; el principiante pasa a dosis.
    - **LW:** el principiante con margen 0 queda SIN_EFECTO; con margen, dosis.
    - **AG:** casi sin cambio.
19. **¿Qué decisiones requieren intervención profesional?**
    - **(a)** ¿Es aceptable **conservar** la sesión (o solo bajar dosis) cuando la persona dice «demasiado difícil» y no hay variante menor?
    - **(b)** ¿Qué laterales del mismo nivel son de verdad más sencillos? (H12-B: una tabla curada).
    - **(c)** ¿Qué cambios de músculo dentro de un patrón son aceptables? (H2).
    - **(d)** Las fichas de 580, 775, 780 y 862 (`10`).
    - **(e)** La ventana de no repetición si se quisiera H12-D.
20. **¿Qué problemas del catálogo siguen pendientes?**
    - **580:** traducción «Curl…» para un press inclinado.
    - **775:** patrón de empuje para «espalda alta».
    - **780:** remo al mentón como tracción horizontal.
    - **862:** dominada colgada con músculo «abs» y dificultad 1.
    - Además: la ausencia de cualquier atributo de demanda o asistencia que permita graduar dentro de la dificultad 1 (`10`, `07` H12-B).
21. **¿Se conservó V24?** **Sí:** ninguna alternativa autoriza una dosis prohibida. En los escenarios, V24 rechazó la reducción en GM con las cuatro configuraciones (E12), y 0 dosis subidas (`08`).
22. **¿Se conservaron V20 y la adherencia?**
    - **Sí:** V20 rechazó lo que no cabía en las cuatro configuraciones, y hay 0 sesiones publicadas bajo el mínimo.
    - Aw, Aobs y la cobertura **no dependen** de V8C (se calculan antes). `adherencia.csv` y las métricas de las semanas 2–3 son idénticos en las cuatro corridas de escenarios.
    - Todas las IT de adherencia, V20, RESPALDO y SIN_EFECTO pasan con la configuración por defecto y con H1 (`11` §4, `12`).
23. **¿La generación inicial sigue idéntica?**
    - **Sí.** 960 perfiles **idénticos byte a byte** con las reglas apagadas, con H1 y con H1 + H12-A (`planes.csv d109a3da…`, `colocaciones.csv d227848b…`, los mismos que en `generation`).
    - En los escenarios, `generacion.csv` y el plan inicial son idénticos en las 4 configuraciones (`12`, `11` §4).
24. **¿La REPLAY y los 960 perfiles siguen pasando?** **Sí**, en las tres configuraciones. La REPLAY de 100 semanas solo difiere en 2 marcas de tiempo de `exclusiones_dolor.csv` (`12`).
25. **¿Qué PR quedaron creados y con qué CI?** Tres, en borrador y sin fusionar (`13`):
    - [#31](https://github.com/Fitsense-Real/Backend/pull/31) H1, CI verde en `77ca7dc`;
    - [#32](https://github.com/Fitsense-Real/Backend/pull/32) catálogo, verde en `4d43ee1`;
    - [#33](https://github.com/Fitsense-Real/Backend/pull/33) H12, verde en `a38b312`. El *push* final solo añade documentos.
26. **¿Qué cambios concretos recomiendo autorizar?**
    1. **Activar H1** (`fitsense.adaptation.v8c.pattern-rule=multiconjunto`), con una IT del caso U04 en CI (K15).
    2. **Llevar H12-A a evaluación profesional** (§19 a) y, si se aprueba, activar `fitsense.adaptation.variante.policy=reduccion-demostrable` y validarlo en una última corrida con Replicate (el prompt con sustitutos < ya está preparado).
    3. **Revisar el texto de la justificación** para que no afirme «más sencillo» sin una reducción observable (K5).
    4. **Revisar las 4 fichas de catálogo** con un profesional.
27. **¿Qué no recomiendo implementar todavía?**
    - **H12-C** (límite numérico): bloquea simplificaciones legítimas.
    - **H12-D** (anti-rotación): necesita historial nuevo y es redundante con H12-A.
    - **Una regla de músculo absoluta en V8C (H2):** depende de un catálogo con clasificaciones dudosas.
    - **Inferir «más sencillo» del nombre o del equipamiento (H12-B sin datos).**
    - **Cualquier cambio de catálogo sin revisión.**

**Una adaptación técnicamente válida no es una adaptación clínicamente adecuada.** Ninguna de estas pruebas sustituye la evaluación profesional.
