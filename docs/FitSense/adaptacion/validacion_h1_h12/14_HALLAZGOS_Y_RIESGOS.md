# 14 · Hallazgos y riesgos

**Clases de hallazgo:**
- **D:** defecto demostrado.
- **G:** hueco de regla.
- **F:** cuestión funcional o de política.
- **C:** catálogo.
- **P:** prompt.
- **T:** trazabilidad o datos.
- **L:** laboratorio.

**Ninguna corrección está activa por defecto.**

| # | Hallazgo | Clase | Evidencia | Riesgo | Estado y propuesta |
|---|---|---|---|---|---|
| K1 | **H1:** V8C compara el patrón por conjunto. Una sesión puede perder un patrón (U04 S2) | G | `02` | Medio: no se observó con IA real | **Resuelto en candidata:** MULTICONJUNTO, apagada; 123 de 123 sustituciones reales siguen válidas y 0 regresiones (`04`, `05`). **Autorizar** |
| K2 | Con MULTICONJUNTO, «añadir 2 de un patrón retirando 1» (fuera del dolor) deja de pasar | F | `03` §5; prueba 4 | Bajo: 0 casos reales | Cambio declarado. Es trabajo añadido en una sesión «demasiado difícil» |
| K3 | **H12:** en principiantes, el conjunto elegible solo tiene **dificultad 1**, así que una «variante más sencilla» no existe en el catálogo. Las 59 sustituciones de principiantes son laterales | F/C | `06` §5 | Medio | Causa principal de H12. Decisión de política: H12-A |
| K4 | El prompt (V1 y V2) ofrece sustitutos de dificultad **≤** (no <), ordenados por id y hasta 6. V2 los llama «opciones válidas» y pide «omite solo si ninguna sirve» | P | `06` §6 | Medio | Prototipo H12-A: el prompt solo ofrece < y dice «OMÍTELA» si no queda nada (apagado) |
| K5 | La **justificación** que ve la persona afirma «variantes más sencillas» / «menos exigentes» cuando la dificultad no cambia (1 → 1) | P/F | `06` §3 | Medio: comunica una simplificación que el sistema no puede demostrar | Con H12-A desaparece el caso. Sin H12-A, revisar el texto de la justificación (decisión de producto) |
| K6 | **Rotación** de sustituciones con IA real (18 en la semana 2) y **circular** en escenarios (R01: A → B → A). Con IA real, R05 C2 **reintroduce en la semana 2 los ejercicios de la semana 1 que la persona dijo que eran difíciles** (1054 y 253) | F | `06` §4; `08` | Medio | H12-A la impide en lo lateral. H12-D necesitaría historial |
| K7 | V8C no recibe historial de sustituciones de VARIANTE; `conservation_report.sustituciones` solo registra las de seguridad | T | `06` §6.1 | Bajo hoy | Necesario si se quiere H12-D o una auditoría longitudinal. Requiere decisión (sin migración, en JSON) |
| K8 | **H12-C** (límite por sesión) no distingue lateral de simplificación: con N = 2 bloquea bajadas legítimas (E09, E10; U04 cambia 7 de 7 y 5–6 bajan) | F | `07` §1; `08` | Alto si se activa | **No recomendado** |
| K9 | **H12-A** sube SIN_EFECTO de forma acotada: en el histórico, 8 sesiones laterales sin salida (GM principiante 4, LW principiante en margen 0 3, FU principiante 1). En escenarios, 11 → 26 filas | F | `07` §2; `08` §3 | Medio: menos adaptaciones aplicadas | **Es el comportamiento buscado** («conservar es legítimo»), pero requiere evaluación profesional (§19 de `15`) |
| K10 | Una sustitución lateral **puede** ser más fácil en la práctica (máquina asistida, apoyo de rodillas), y el catálogo no lo mide. H12-A la rechazaría | F/C | `07` H12-B | Bajo-medio | H12-B, con una tabla curada por un profesional. No se infiere del nombre |
| K11 | **H2:** ninguna alternativa compara el músculo. 23 de 123 sustituciones lo cambian; 11 de ellas vienen de 580, 775, 780 y 862 | C/F | `10` §3 | Medio | Revisar primero el catálogo (`10`). No exigir equivalencia absoluta todavía |
| K12 | **862** (*gorilla chin*): suspensión con dificultad 1 y músculo «abs» | C | `10` | Medio: dominada colgada en planes de principiante | Revisión profesional (ya PENDIENTE_CRITERIO en `v37`) |
| K13 | En los escenarios con H12-A, una sesión que se adapta cada semana **por dosis** puede llegar al mínimo V20 en la semana 3 (E11: 3 → 1 → SIN_EFECTO) | F | `08` (E11) | Bajo: comportamiento ya conocido (H5 del PR #30) | Sin cambio; documentado |
| K14 | El laboratorio pasa a admitir 3 semanas y el guion H12. Los valores por defecto reproducen lo anterior: `GUION_COMBINADOS` con 2 semanas da resultados idénticos (`02`) | L | `08` | — | — |
| K15 | Las IT no comprueban de forma específica la regla H1 de extremo a extremo; solo el laboratorio *opt-in* lo hace | L | `05` | Bajo | Si se autoriza H1, añadir una IT del caso U04 que corra en CI |
| K16 | Con H12-A, si el añadido no está en el catálogo elegible, el mensaje de H12-A repite el ejercicio que V8C ya rechazaba («no consta su patrón») | L | `12` §3 | Ninguno: el resultado es el mismo | Si se aprueba H12-A, omitir en su mensaje los añadidos sin ficha |
| K17 | Con H12-A, la IT de control `AdaptacionRegresionIT.sinBloqueoLaVarianteSePublica` falla porque espera una VARIANTE lateral (919 → 124, dificultad 2 → 2) | F | `12` §3 | Ninguno con la regla apagada | Cambio esperado. Si se aprueba H12-A, adaptar el control a un sustituto de menor dificultad |

**Defectos demostrados del backend (clase D): 0.** H1 era un hueco de la regla, no un error de implementación. H12 es una combinación de catálogo, política, prompt y falta de límite.

**Riesgo general:** cualquier alternativa activada cambia el comportamiento funcional de TOO_DIFFICULT. **Ninguna** se activa sin tu aprobación, y una adaptación técnicamente válida **no** es una adaptación clínicamente adecuada.
