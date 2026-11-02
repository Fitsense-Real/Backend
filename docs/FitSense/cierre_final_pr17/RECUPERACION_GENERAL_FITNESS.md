# Recuperación de R0040, R0328 y R0616 (GENERAL_FITNESS)

**Perfiles:** GENERAL_FITNESS, principiante, 1 día, 75 minutos.

| Perfil | Material |
|---|---|
| R0040 | Gimnasio |
| R0328 | Mancuernas y bandas |
| R0616 | Peso corporal |

**Situación:**
- La única sesión dura 2.928–3.006 s (65,1–66,8 % de M). El piso de V20 es 3.150 s.
- C2-D no la acepta: el verificador encuentra una **SUSTITUCIÓN** que alarga la sesión (NO_SATURADA).

**Datos:**
- `REPARACION_SUSTITUCION_DETALLE.csv`: las sustituciones que propone el verificador, paso a paso.
- `REPARACION_SUSTITUCION_IMPACTO.csv`: el efecto de la reparación en los 960 perfiles.

## 1. Qué sustitución encuentra el verificador

| Perfil | Paso | Original (patrón, clase, dosis, s) | Sustituto (patrón, clase, dosis, s) | Sesión antes → después |
|---|---|---|---|---|
| R0040 | 1 | 1046 Curl de piernas sentado (KNEE_DOMINANT, ESTRUCTURAL, 3×8, 120 s, 612 s) | 253 Gateo de oso (LOCOMOTION_CARDIO, **CARDIO**, 3×90 s, 690 s) | 65,1 % → 66,8 % |
| | 2 | 122 Elevación de cadera con banda (HIP_EXTENSION, ESTRUCTURAL) | 853 Media sentadilla (LOCOMOTION_CARDIO, **CARDIO**) | → 68,5 % |
| | 3 | 1146 Fondos de escápula (HORIZONTAL_PUSH, ESTRUCTURAL) | 1054 Escalador (LOCOMOTION_CARDIO, **CARDIO**) | → 70,3 % |
| R0328 | 1–3 | 1103 Sentadilla de baño, 122 Elevación de cadera, 775 Elevación de codo (ESTRUCTURAL) | 253, 853, 1054 (**CARDIO**) | 65,1 % → 70,3 % |
| R0616 | 1–2 | 1263 Sentadilla búlgara (KNEE_DOMINANT), 1112 Puente (HIP_EXTENSION) (ESTRUCTURAL) | 1054 Escalador, 1093 Empujar para correr (**CARDIO**) | 66,8 % → 70,3 % |

**Todas cumplen:**
- material elegible (peso corporal);
- compatibilidad EF (`ef_compatible=true`: el cardio no se juzga por patrón);
- V4;
- V24 (el sustituto lleva su dosis base 4D).

## 2. ¿Es válida funcionalmente?

**No.**
- **Ninguna sustitución conserva la función:** todas cambian un ejercicio **de fuerza** (rodilla, cadera o empuje) por **cardio**, solo porque la dosis base del cardio (3×90 s) dura 78 s más que la de fuerza (3×8 con 120 s).
- **La sesión llega al 70 % perdiendo la fuerza que la justifica.**
- **Con el validador completo, la «reparación» queda rechazada:**
  - R0040 y R0328 terminan inválidos por **V25** (omiten una capacidad principal).
  - R0616 pasa, pero con dos patrones de pierna menos.
- El primer prototipo de la reparación usaba un validador **sin** V25 y la aceptaba. **Era un error del prototipo**, corregido antes de esta medición.

## 3. Mejora experimental implementada: REP-SUST-0.1

**`RuleBasedTrainingPlanGenerator.repararPorSustitucion`**, detrás de `fitsense.generation.reparacion-sustitucion-v20=false`. Si está encendida, se traza como `opciones.reparacion_sustitucion_v20`.

**Cuándo actúa:**
- solo si la **única** infracción es V20;
- y el verificador de C2-D demuestra una **SUSTITUCIÓN**.

**Qué hace:**
- La aplica con la dosis base 4D, como mucho 6 pasos, de forma determinista (candidatos por id).
- **Valida con el validador completo** (V23 cuando es satisfacible, V24 y **V25**).
- **Solo acepta sustituciones que conservan la clase** del ejercicio: estructural por estructural, resistido por resistido, cardio por cardio, movilidad por movilidad.
- **No añade** ejercicios ni series. **No toca C2-D:** si la sesión sigue corta, C2-D decide con su criterio de siempre.

**Resultado en los 960 perfiles:**

| Política | Válidos sin reparación → con reparación | Planes válidos alterados | Inválidos → válidos |
|---|---|---|---|
| PLAN-HIST-1 | 875 → 875 | **0** | 0 |
| PLAN-C2D-0.1 | 951 → 951 | **0** | 0 |
| PLAN-C2D-EF-0.1 | 902 → **903** | **0** | **1: R0808** |

- **R0040, R0328 y R0616 no se recuperan:** no existe ninguna sustitución que conserve la clase y alargue la sesión.
  - En GENERAL_FITNESS principiante todas las dosis estructurales duran lo mismo (612 s).
  - Siguen inválidos por V20 y **C2-D sigue sin eximirlos**, porque existe un movimiento que mejora la duración.
- **R0808** (GENERAL_FITNESS, principiante, peso corporal, 6 días, 75 min, solo con EF):
  - se sustituye 1178 (estiramiento de muñeca lateral, MOVILIDAD, 2×30 s) por 1373 (círculos de muñeca, MOVILIDAD, 2×10);
  - la sesión pasa de 67,9 % a 68,3 %;
  - el verificador declara SATURADA y C2-D la publica.
  - **Es legítimo según las reglas, pero marginal:** gana 15 s. Va a la revisión profesional.

## 4. Recomendación

- **No activar REP-SUST-0.1.** No recupera los tres casos y su único efecto es un caso marginal.
- Mantenerla desactivada como herramienta de laboratorio.
- **Hallazgo sobre C2-D:** el verificador cuenta como «mejora» una sustitución que cambia la clase del ejercicio (fuerza por cardio). Eso impide la excepción en estos tres casos.
  - Corregirlo (que el verificador solo considere sustituciones de la misma clase) **haría a C2-D más permisivo**: los tres pasarían a SATURADA y se publicarían al 65–67 %.
  - Es una decisión que requiere aprobación (`DECISIONES_PENDIENTES.md`). **No se ha cambiado:** la orden prohíbe debilitar C2-D para aceptar el plan corto.
