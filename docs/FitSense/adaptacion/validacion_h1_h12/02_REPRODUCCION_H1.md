# 02 · Reproducción de H1 (fase 2.1)

## 1. El caso

**U04** (GAIN_MUSCLE, intermedio, gimnasio, 60 min), sesión **S2** del plan 10 (*planned workout* 29; la orden la llama «S20»), señalada como **VARIANTE_MAS_SENCILLA_O_CARGA** (`TOO_DIFFICULT`).
- **Origen:** escenario #12b del laboratorio `GUION_COMBINADOS` (PR #30, `07_PRUEBAS_CAMBIOS_COMBINADOS.md`).
- Es una respuesta **controlada**, no una propuesta de Replicate.

| Posición | Antes | Patrón | Después | Patrón |
|---:|---|---|---|---|
| 1 | 380 *cable one arm incline press* (dif. 2) | HORIZONTAL_PUSH | **56** *archer push up* (dif. 2) | HORIZONTAL_PUSH |
| 2 | 389 *cable pulldown* (dif. 2) | **VERTICAL_PULL** | **72** *assisted chest dip (kneeling)* (dif. 1) | **HORIZONTAL_PUSH** |
| 3–7 | 360, 779, 1236, 580, 392 | HINGE, VPUSH, KNEE, HPUSH, VPULL | sin cambios | — |

**Distribución de patrones de la sesión:**
- antes: 2 HORIZONTAL_PUSH y **2 VERTICAL_PULL**;
- después: **3 HORIZONTAL_PUSH** y **1 VERTICAL_PULL**.

## 2. Por qué V8C lo aceptaba

`ConservationValidator.revisarSustitucion`, rama no dolorosa:
1. Reúne los patrones de los retirados en un **conjunto**: {HORIZONTAL_PUSH, VERTICAL_PULL}.
2. Para cada añadido, comprueba que su patrón **pertenezca** a ese conjunto. 56 y 72 son HORIZONTAL_PUSH, que pertenece.

Nada compara **cuántos** ejercicios hay de cada patrón. El retirado 380 «justifica» a la vez a 56 y a 72, y el 389 (VERTICAL_PULL) se pierde sin reemplazo de su patrón.

- La cobertura semanal 4C (V25) tampoco lo detiene: hay tracción en otras sesiones de la semana.
- Los demás validadores miran dosis, duración y equipamiento, no la distribución.

**No es un defecto de implementación:** V8C hace lo que su diseño dice (pertenencia al conjunto). Es un **hueco de la regla**.

## 3. Reproducción conservada (comportamiento anterior como evidencia)

**A. Prueba unitaria con el V8C real y datos reales del catálogo** (`V8CConservacionPatronH1Test`):

| Prueba | Regla | Resultado |
|---|---|---|
| `1a u04AntesSeAcepta` | CONJUNTO (histórica) | **aceptada** y contada como APLICADA |
| `1b u04DespuesSeRechaza` | MULTICONJUNTO (candidata) | **rechazada**: «anade 2 ejercicio(s) de patron HORIZONTAL_PUSH [56, 72] y solo retira 1 de ese patron [380]» |
| `1c u04CorregidoSeAcepta` | MULTICONJUNTO | 389 → 74 (VERTICAL_PULL): **aceptada** |

`V8CDiagnosticoSustitucionesTest` (PR #30) **se conserva sin cambios**. Sus pruebas 2 y 3 documentan el hueco con la regla histórica, que sigue siendo la regla por defecto.

**B. De extremo a extremo** (servicios reales, PostgreSQL efímero, guion `COMBINADOS`, `prompt=v2`, 2 semanas, mismo *commit* `d8c577c`, sin red):

| | `evidencia/H1_COMBINADOS_conjunto` | `evidencia/H1_COMBINADOS_multiconjunto` |
|---|---|---|
| `v8c_regla_patron` (`00_configuracion.txt`) | CONJUNTO | MULTICONJUNTO |
| U04, intento 1 (56 + 72) | **aceptado y publicado** | **rechazado por V8C** (mensaje de §3.A en `ejecucion_maven.log`) |
| U04, intento 2 | — | el guion propone 56 + **71** (*alternate lateral pulldown*, VERTICAL_PULL): **aceptado** |
| U04 S2 publicada | [56, **72**, 360, 779, 1236, 580, 392]: 3 HPUSH y 1 VPULL | [56, **71**, 360, 779, 1236, 580, 392]: 2 HPUSH y 2 VPULL |
| Llamadas de IA de U04 en la semana 1 | 1 | 2 |
| Resto de los 20 usuarios (generación, plan inicial, adherencia, decisiones, semanas 1 y 2) | — | **idéntico byte a byte** |

**Única diferencia entre las dos ejecuciones: U04.**
- `plan_siguiente.csv`, `resultados_20.csv` y `decisiones.csv`: solo cambia su fila.
- `plan_inicial.csv`: solo cambia su fila «siguiente».
- `plan_siguiente_semana2.csv`: solo cambia la continuación de esa misma sesión.
- `adherencia.csv`, `generacion.csv` y `semana2.csv`: idénticos.
