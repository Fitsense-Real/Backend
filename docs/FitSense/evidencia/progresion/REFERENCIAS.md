# Referencias de la generación inicial (72 perfiles)

Cada archivo guarda los planes iniciales de los 72 perfiles de referencia:
- 4 objetivos × 3 niveles × 30/45/60 min × gimnasio o casa;
- con sus ejercicios, dosis, estructura y resultado de la validación.

Cada archivo es la salida de **un catálogo con un motor**.

**Decisión del 30/10/2026:**
- Las referencias históricas **no se sobrescriben**.
- Las pruebas de regresión comparan cada combinación de catálogo y motor con **su** referencia.

## Archivos

| Archivo | Catálogo | Motor | Estado | Prueba que lo usa | SHA-256 |
|---|---|---|---|---|---|
| `generacion_referencia_v35.txt` | V35 | anterior a V20 exacto. Creada en `1f7d929`, conservada en `657ef09`, reproducida por última vez en `e14da35` | **histórica, intacta** | `ReferenciasHistoricasInmutablesTest` (solo la huella) | `a32cbe7e…e9b3` |
| `generacion_referencia_v36.txt` | V36 | anterior a V20 exacto. Creada en `158439d`, reproducida por última vez en `e14da35` | **histórica, intacta** | `ReferenciasHistoricasInmutablesTest` | `6f204c07…94b` |
| `generacion_referencia_v37_motor_e14da35.txt` | V37 | anterior a V20 exacto (`e14da35`, CI verde de `generation`) | **histórica, intacta**. Copia byte a byte de la oficial hasta V20 exacto | `ReferenciasHistoricasInmutablesTest` | `e4676975…8d23` |
| `generacion_referencia_v35_motor_v20.txt` | V35 | vigente, con V20 exacto | regresión del motor actual sobre V35 | `GeneracionReferenciaV35IT` | `c104c75f…1f73` |
| `generacion_referencia_v36_motor_v20.txt` | V36 | vigente, con V20 exacto | regresión del motor actual sobre V36 | `GeneracionReferenciaV36IT` | `2ccd9e24…cd12` |
| `generacion_referencia.txt` | V37 | vigente, con V20 exacto | **oficial**. Regenerada con 13 cambios aprobados | `GeneracionReferenciaIT` | `00951b53…59e5` |

Las tres nuevas (`*_motor_v20.txt` y la oficial) son **salidas del motor actual**, no las históricas. Las tres históricas guardan lo que producía el motor anterior y no se ejecutan contra el motor actual: solo se comprueba que no cambian.

## Qué cambió por el motor y qué por el catálogo

**Por el motor** (mismo catálogo, motor anterior frente al motor con V20 exacto):

| Catálogo | Perfiles que cambian | LOSE_WEIGHT | GAIN_MUSCLE | INCREASE_STRENGTH | GENERAL_FITNESS |
|---|---|---|---|---|---|
| V35 | 11 | 2 | 2 | 0 | 7 |
| V36 | 11 | 2 | 2 | 0 | 7 |
| V37 | 13 | 2 | 4 | 0 | 7 |

Siempre son perfiles de 45 o 60 minutos con alguna sesión que llegaba al piso solo por el redondeo. Los 72 siguen válidos en los tres catálogos. Perfiles concretos:
- **V35:** LW principiante 45 gimnasio y casa; GM intermedio y avanzado 45 casa; GF principiante 45 gimnasio y casa; GF principiante 60 casa; GF intermedio y avanzado 60 gimnasio y casa.
- **V36:** LW principiante 45 gimnasio y casa; GM principiante y avanzado 45 gimnasio; GF principiante 45 gimnasio y casa; GF principiante 60 gimnasio; GF intermedio y avanzado 60 gimnasio y casa.
- **V37:** los de `IMPLEMENTACION_V20.md` §2.4.

Los perfiles exactos dependen del catálogo, porque cada catálogo deja sesiones distintas cerca del piso.

**Por el catálogo** (mismo motor, catálogo siguiente):

| Paso | Motor anterior | Motor con V20 exacto |
|---|---|---|
| V35 → V36 | 72 perfiles (18 por objetivo) | 72 (18 por objetivo) |
| V36 → V37 | 68 (LW 14, GM 18, IS 18, GF 18) | 68 (LW 14, GM 18, IS 18, GF 18) |

El efecto del catálogo tiene el mismo tamaño con los dos motores. Las explicaciones por catálogo están en `CATALOGO_V36.md` y `CATALOGO_V37.md`.

## Regenerar (solo con aprobación)

| Referencia | Propiedad |
|---|---|
| oficial | `-Dfitsense.generacion.capturar=true` |
| V36 con el motor vigente | `-Dfitsense.generacion.v36.capturar=true` |
| V35 con el motor vigente | `-Dfitsense.generacion.capturar.v35=true` |

Las históricas no se regeneran nunca. Si un cambio futuro del motor modifica la generación:
1. La referencia vigente se copia antes como `*_motor_<commit>.txt`.
2. Su huella se añade a `ReferenciasHistoricasInmutablesTest`.
