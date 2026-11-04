# 14 · Riesgos y decisiones pendientes

En esta validación **no se ha encontrado ningún problema crítico de seguridad**. Ningún camino reintroduce un ejercicio excluido por dolor (03 §3). Por eso no se prepara ninguna corrección de seguridad adicional.

Lo que sigue son riesgos conocidos y decisiones que corresponden al responsable o a una persona profesional.

| # | Riesgo / hallazgo | Origen | Gravedad | Qué se hizo | Decisión pendiente |
|---|---|---|---|---|---|
| R-1 | **«Dificultad registrada» no es seguridad clínica.** El límite compara `difficulty_level` del catálogo, que es descriptivo: un 1 no es necesariamente más seguro que un 2 para un dolor concreto. | Diseño | Media | Se documenta; las pruebas no lo presentan como garantía clínica. | Ninguna para activar; sí para comunicarlo (texto, paper). |
| R-2 | **El dolor no registrado no se excluye.** La cadena depende de que P-02 tenga el evento: un ejercicio con `PAIN_OR_DISCOMFORT`, o una sesión omitida por dolor. | Existente, no lo introduce el límite | Media | Sin cambio. | Producto / Flutter: cómo se pide registrar el dolor. |
| R-3 | **Los 24 casos del 135 pasan de sustitución a regeneración** (P-03 nivel 2). La semana regenerada parte de las dosis base y **no lleva linaje V31**. La trazabilidad queda en `safe_regeneration_attempts`. Si una semana regenerada supera el trabajo de la medida, queda **NO_FACTIBLE**. | Efecto del límite (B/C) | Baja-media | Comprobado de extremo a extremo (08: los 22 perfiles con A, B y C). | Aceptar que, en casa con mancuernas y bandas, el dolor en el 135 de una sesión PUSH lleve a regenerar la semana en vez de a un sustituto más difícil. |
| R-4 | **Hueco de catálogo:** fuera del gimnasio, el 135 es el único shoulders/HORIZONTAL_PULL de dificultad 1 (el otro, el 336, es de cable). | Catálogo | Baja | **No se modifica** el catálogo ni la dificultad del 135. | Profesional: ¿falta una alternativa de dificultad 1, o la clasificación del 135, 780 y 781 es correcta? |
| R-5 | **Rechazos de IA no persistidos al conservar.** Si los 3 intentos se rechazan y la semana se conserva, `rejection_reason` queda NULL y el motivo de cada rechazo no queda en la base, solo en el log y en la retroalimentación. | Existente | Baja (trazabilidad) | Documentado (06 §3). | ¿Persistir los motivos de los intentos? Sería un cambio separado de este PR. |
| R-6 | **Coste funcional del filtro:** 72 de 19.147 colocaciones simuladas (0,38 %) pierden el mismo patrón; 65 pierden músculo y 24 zona. Todas siguen cumpliendo las etapas obligatorias. | Efecto del límite | Baja | Medido (04 §2). C no lo empeora. | Aceptarlo como precio de «no subir». |
| R-7 | **V32 de N08 con el límite:** la sesión 62 figura en `no_effect_sessions` y en `conservadas`, aunque lleva la sustitución SUST-SEG-1 791 → 1003. La sustitución consta en `conservation_report.sustituciones` y en `correcciones_del_backend`. | Existente: así registra el RESPALDO | Baja | Documentado (09). | Ninguna. |
| R-8 | **Exposición ≠ incidentes.** Las 2.469 subidas del barrido son exposiciones **simuladas**: cada ejercicio colocado se trata como si hubiera dolido. En los datos registrados que se revisaron hay **1** sola sustitución efectiva que sube: N08 (dolor, propuesta por la IA y publicada), en la REPLAY de 100 semanas con respuestas registradas.
- En la REPLAY, N08 es lo único que cambia con el límite.
- Las 123 sustituciones históricas de 59 sesiones (VARIANTE y otras causas) no tienen ninguna subida con emparejamiento por patrón. | Interpretación | — | Se separan exposición, propuesta, rechazo, efectiva, publicada y respaldo (12 §4). | — |

## Decisiones que **no** se toman aquí

Ninguna de ellas está hecha:
- activar el límite por defecto;
- fusionar #38;
- permitir +1 como excepción;
- cambiar políticas de dolor;
- tocar V20, V24 o 4D;
- tocar el catálogo;
- activar H12-A, B, C o D, o V2.

## Casos que requieren criterio profesional (pregunta 28)

1. El 135 y sus alternativas (R-4): ¿es razonable que 780 y 781 («remo vertical con mancuerna», dificultad 2) sustituyan a un remo de deltoides traseros con banda (dificultad 1) en una sesión PUSH de alguien con dolor?
2. Si para el dolor la «dificultad registrada» es el criterio adecuado (R-1), o si debería pesar más el patrón o el músculo afectados.
3. Si la regeneración de la semana completa (R-3) es aceptable frente a la alternativa no implementada de «retirar solo ese ejercicio y conservar el resto». La orden prohíbe introducir esa alternativa sin demostrar antes que hace falta.
