# Hallazgos y riesgos

**Clasificación:**
- **D:** defecto demostrado.
- **G:** hueco de una regla, demostrado pero no observado con la IA real.
- **F:** cuestión funcional o de política.
- **C:** catálogo.
- **T:** texto del prompt V2.
- **L:** laboratorio.
- **E:** error en un informe anterior.

**Ninguna corrección se ha aplicado a código funcional, reglas ni catálogo.**

| # | Hallazgo | Clase | Evidencia | Riesgo | Propuesta (pendiente de tu decisión) |
|---|---|---|---|---|---|
| H1 | V8C comprueba el patrón **por conjunto**: una sesión puede perder un patrón (VERTICAL_PULL → HORIZONTAL_PUSH) y se publica | **G** | `04` §3; `07` #12b; prueba de diagnóstico 2 | Medio: no ocurrió en las 57 sustituciones reales, pero nada lo impide | Opción A/B (multiconjunto o pareja) + D (dificultad ≤ por pareja). Ninguna rechazaría una sustitución real |
| H2 | V8C no compara el **músculo principal**: 8 de 57 sustituciones reales lo cambian (6 en C1/C2 y 2 en la etapa C; 7 pares distintos) | **F** | `02`, `03` | Medio: énfasis distinto dentro del mismo patrón (pectoral → tríceps, deltoides → espalda) | Antes de cualquier regla, revisar la clasificación de 580, 775 y 780 (C1) y llevar los 8 casos a un profesional. Una regla de músculo absoluta bloquearía variantes (`04` opción C) |
| H3 | 21 de 57 sustituciones de VARIANTE tienen la **misma dificultad**. En 19 la original ya tenía la mínima (1): no hay variante «más sencilla» según el catálogo | **F** | `03` §3 | Bajo-medio: la adaptación a TOO_DIFFICULT puede ser un cambio lateral | Decisión de política: con dificultad mínima, ¿preferir una dosis menor (donde se permita) o conservar? En GM, 4D no permite bajar la dosis |
| H4 | **Sesiones sin solución por dosis**, no solo en GM: planes generados en el mínimo (R05: margen 0) o con un margen menor que la menor reducción (R04: 6 s frente a 168 s) | **F** | `06` §3; `R_GUION_2S` | Bajo: el backend conserva (SIN_EFECTO) y V2 lo dice | Política de duración (reservar margen al generar) o saltarse la llamada a la IA cuando la factibilidad no deja ninguna opción (ahorro) |
| H5 | Las adaptaciones que llegan al **mínimo V20** (4 sesiones distintas con IA real) agotan la reducción por dosis para la semana siguiente. Las reducciones son **acumulables** semana a semana hasta el mínimo | **F** | `05`, `06` | Bajo: el estado es válido y estable; segundo intento → V20 rechaza y se conserva | Decisión de política (no se toca V20) |
| H6 | GAIN_MUSCLE intermedio, gimnasio, **75 min** (R02): **NO_FACTIBLE** en la generación inicial (2577 s frente a un mínimo de 3150 s) | **F** (generación) | `11`; `R_*_2S/generacion.csv` | Medio para ese perfil: no hay plan. **No es de V2** | Revisar la cota de colocaciones de 4D para sesiones largas (la factibilidad 30–75 min de P-01). Ver los informes de factibilidad anteriores |
| H7 | El nombre español del 580 («Curl…») no corresponde a su ficha. La clasificación de 775 (*reverse push-up*, dorsal alto, PUSH) y de 780 (remo al mentón, deltoides, PULL horizontal) es discutible | **C** | `03` §2 | Las sustituciones «del mismo patrón» de estos ejercicios cambian de énfasis | Revisión del catálogo con un profesional (P4 de PR #28). **No se corrige por el nombre** |
| H8 | V2 lista sustitutos sin considerar el músculo, e imprime «Puedes combinar cambios…» aunque ninguna opción quepa | **T** | `04` §4; R04 en `06` | Bajo | Si se activa V2 (o en una V2.1), ordenar o filtrar los sustitutos por mismo músculo y omitir la línea de combinar cuando no hay ninguna opción [cabe]. **Requiere tu autorización** (es un cambio del prompt) |
| H9 | El laboratorio cortó 4 casos por un NullPointerException propio (ternario `Short`/`short`) | **L** | `GUION_COMBINADOS_2S_fallo_laboratorio` | Ninguno en el backend | Corregido en el laboratorio; ejecución conservada |
| H10 | **Informe anterior:** dije que el PR #29 no tendría CI. **Sí lo tuvo**, y verde en `51c145b` | **E** | `01` | — | Corregido aquí |
| H11 | **PR #28, H10:** «las 13 sustituciones conservan el músculo» no es exacto: 2 de 13 lo cambian | **E** | `03` §2 | — | Documentado aquí; PR #28 sin modificar |

**Defectos demostrados del backend (clase D): 0.**
