# Revisión de las familias de patrones de EF-0.1

**Las familias son una regla técnica explícita**, no una validación profesional. Esta revisión explica qué hace la lógica real y cuánto pesa cada decisión en los planes.

**Fuente de datos:** `FAMILIAS_EF_USO.csv` (laboratorio `familiasEf`): colocaciones de los planes válidos de los 960 perfiles por enfoque, patrón, clase, rol y compatibilidad, con `PLAN-C2D-0.1` y con `PLAN-C2D-EF-0.1`.

## 1. Lógica real (código)

- **`ElegibilidadFuncional.compatible(enfoque, c)`:**
  - un ejercicio **no resistido** se acepta siempre y no se juzga por patrón (cardio, movilidad, equilibrio y SIN_CAPACIDAD, es decir, patrón `null` u `OTHER`);
  - un resistido excluido globalmente (tronco fuera de waist, 315) se rechaza;
  - un resistido con patrón `null` se acepta; si no, se mira la familia del enfoque.
- **`esAncla(enfoque, c)`:** el patrón debe ser del ancla, el ejercicio **resistido** y no excluido. **Un patrón `null` u `OTHER` nunca es ancla.**
- **VEF** no juzga los **complementos**, ni exige ancla a una sesión sin trabajo resistido propio.
- **Capacidades (V25):** `claseDe` da SIN_CAPACIDAD a `null` y `OTHER`, y `demostrablesPor(SIN_CAPACIDAD)` está vacío.
  - **Un ejercicio sin patrón nunca acredita una capacidad obligatoria** ni cuenta como ancla.
  - Puede aparecer como complemento o relleno compatible, nunca como prueba de cobertura.
- **En la matriz, ningún ejercicio con patrón `null` u `OTHER` aparece en un plan válido**, con ninguna de las dos políticas. El PULL sin material tiene 1 candidato `OTHER` elegible y nunca se coloca.

## 2. Casos pedidos

| Caso | Regla EF-0.1 | Uso (colocaciones C2-D → C2-D-EF) | Riesgo | Pregunta para el profesional |
|---|---|---|---|---|
| **TRUNK_EXTENSION en PULL** | Admitido en la familia; **no es ancla** | 75 → 8 (con EF, el PULL se llena de tracciones) | Bajo: no acredita tracción | ¿La extensión lumbar es contenido pertinente de una sesión de tracción? |
| **ELBOW_FLEXION en PULL** | Admitido; no es ancla | 223 → 136 compatibles. Los 97 no compatibles con C2-D son 315 (excluido) | Medio: 320, sin carga, sigue admitido | ¿Un curl sin carga cuenta como trabajo del PULL? |
| **SHOULDER_RAISE en PUSH** | Admitido; no es ancla | 20 → 45 | Bajo | ¿Las elevaciones de hombro pertenecen a PUSH? |
| **Tronco en LOWER_BODY** | TRUNK_FLEXION, TRUNK_ROTATION, CORE_STABILITY y TRUNK_EXTENSION admitidos si la zona es waist | TRUNK_FLEXION 446 → 436, ROTATION 146 → 149, CORE 54 → 55. Los de zona distinta de waist: 20 → 0 | **Medio:** el tronco ocupa una parte grande de LOWER_BODY y acredita HYPERTROPHY como RESISTIDO | ¿Es correcto que el core cuente como trabajo resistido del tren inferior? ¿Cuánto debería pesar? |
| **Sin patrón (`null` u `OTHER`)** | No se juzga; no acredita capacidad ni ancla | 0 colocaciones | Ninguno hoy | — |
| **Habilidades excluidas de más** (65 *back lever*, 873 *front lever*, 808 extensión con giro en balón) | Excluidas como resistidas (tronco con zona back) | **0 perfiles donde sean elegibles** (exigen barra o balón y nivel) | **Sin efecto hoy.** Lo tendría con perfiles avanzados con barra | ¿Son trabajo de espalda (PULL)? Si lo son, añadirlos a una lista de excepciones, con revisión |
| Empuje (775) en FULL_BODY y UPPER | Cuenta como empuje (su patrón) | 422 colocaciones con EF | **Medio:** si su patrón real fuera SHOULDER_EXTENSION, acreditaría UPPER_PUSH sin serlo | La misma revisión que pide el catálogo (P1) |

## 3. Lo que EF-0.1 impide (medido)

| Combinación incompatible | Colocaciones con C2-D | Con C2-D-EF |
|---|---|---|
| HORIZONTAL_PUSH en PULL | 165 | **0** |
| HORIZONTAL_PULL en PUSH | 175 | **0** |
| TRUNK_FLEXION y ROTATION (zona ≠ waist) en PULL | 213 | **0** |
| TRUNK_ROTATION en PUSH | 93 | **0** |
| Tronco (zona ≠ waist) en UPPER_BODY | 110 | **0** |
| Tronco (zona ≠ waist) en LEGS y LOWER_BODY | 32 | **0** |

**No se alteran globalmente** 4A, 4C, 4D, V23 ni V25. EF solo existe en las versiones `*-EF-0.1`.

## 4. Propuesta (pendiente de revisión profesional)

1. **Mantener EF-0.1 tal cual** para la decisión de activación. Cambiar una familia es crear una **EF-0.2**, con versión propia (V46).
2. Preguntas al profesional: las de §2, ya incluidas en `INSTRUMENTO_PROFESIONAL_FINAL.md` (bloque E).
3. **Excepciones de habilidades** (65, 873, 808): solo si el profesional confirma que son trabajo de espalda. Hoy no tienen efecto.
