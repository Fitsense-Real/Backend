# Auditoría integral de la lógica del backend

**Fecha:** 30/10/2026. **Rama:** `auditoria-integral`. **PR:** [#9](https://github.com/Fitsense-Real/Backend/pull/9), hacia `generation`.

**Alcance:**
- Revisión técnica con pruebas automáticas y datos sintéticos (Testcontainers, nunca Neon).
- La revisión visual del catálogo está **asistida por IA**. **No hay validación humana.**
- **No se hizo ninguna llamada de pago a Replicate.**

Documentos relacionados:
- `CORRECCIONES_IMPLEMENTADAS.md`: qué se corrigió y con qué prueba.
- `ADAPTACION_POR_CAUSAS.md`: matriz de motivos.
- `VALIDACION_MULTISEMANA.md`: escenarios humanos.
- `INFORME_CIERRE_AUDITORIA.md`: CI, simulaciones, migraciones y decisiones pendientes.

## 1. Estado por bloque (criterio §22 de la orden)

| # | Bloque | Estado | Evidencia |
|---|---|---|---|
| 1 | Catálogo y coherencia grupo/slot | **CORREGIDO Y PROBADO** (262 y 14 más, V41). Quedan **PENDIENTES DE DECISIÓN** 54, 1174, 1295, 862 y 1086, más C1, C2, 1113 y 1106 | §2 |
| 2 | Generación inicial | **VERIFICADO**. El orden de los ejercicios en GAIN_MUSCLE queda **PENDIENTE DE DECISIÓN** | §3 |
| 3 | Adherencia | **VERIFICADO** | §4 |
| 4 | Adaptación por causas y subcausas | **CORREGIDO Y PROBADO** (A01, V42) | `ADAPTACION_POR_CAUSAS.md` |
| 5 | Casos con varios motivos | **CORREGIDO Y PROBADO** (dolor con adherencia buena) | `ADAPTACION_POR_CAUSAS.md` §5 |
| 6 | Progresión | **VERIFICADO**, con la progresión apagada por defecto | §5 |
| 7 | Reincorporación | **VERIFICADO** | §5 |
| 8 | Trazabilidad | **VERIFICADO**. El resultado por sesión adaptada se implementó como consulta de solo lectura | §6 |
| 9 | Validación multisemana | **VERIFICADO** en 16 de 18 escenarios. El cambio de perfil queda **PENDIENTE DE DECISIÓN**. Replicate 31/32/33: **NO REPRODUCIBLE** sin los registros originales | `VALIDACION_MULTISEMANA.md` |

## 2. Catálogo

### 2.1 262 y los 17 cruces registrados (V41)

`V41__catalog_c4_c6_fase2.sql` aplica 25 cambios de columna en 15 ejercicios. Sigue el patrón de V40: comprobación del estado previo, comprobación del final y aborto sin cambios ante cualquier diferencia.

| Ej. | Antes | Después | Uso en refs V40 → V41 |
|---|---|---|---|
| 262 *barbell skier* | `HIP_HINGE` | `SHOULDER_EXTENSION` | **11 → 0**. Era el único que acreditaba LOWER_HIP sin bisagra |
| 267, 952 | `shoulders`/`delts` | `upper legs`/`quads` | 267: 0 → 1 |
| 796 | `upper arms`/`biceps` | `upper legs`/`quads` | 0 → 0 |
| 301, 302 | `KNEE_DOMINANT` | `HORIZONTAL_PULL` | 0 → 0. Evaluados por separado, sin heredar la decisión de 439 |
| 671 | `HIP_EXTENSION` | `SHOULDER_EXTENSION` | 0 → 0 |
| 824 | `back`/`spine` | `upper legs`/`glutes` | 0 → 0 |
| 922, 930, 971 | `HIP_HINGE` | `ELBOW_FLEXION` | 0 → 0 |
| 674 | `VERTICAL_PUSH` | `HIP_HINGE` | 2 → 2. Familia de arrancadas revisada |
| 175, 942, 947 | `shoulders`/`delts`/`VERTICAL_PUSH` | `upper legs`/`glutes`/`HIP_HINGE` | 175: 0 → 1. Hallados al revisar la familia de 674 (§5 de la orden: errores en cadena) |

- **Excepción documentada:** 162 se mantiene (familia de cargadas).
- **PENDIENTES DE DECISIÓN:**
  - 54, 1174 y 1295: hay que decidir si son cardio. Cambiarlos altera la clase (CARDIO frente a RESISTIDO) y la composición de LOSE_WEIGHT.
  - 862: su dificultad 1 parece incorrecta. Sin el instrumento de dificultad no se toca.
  - 1086: el GIF no permite decidir el movimiento predominante.

**Pruebas:**
- `CatalogoV41IT`: atributos; clase y slot; ningún cruce de slot de pierna en planes generados.
- `CoherenciaGrupoSlotIT`: avisa de los cruces abiertos y falla solo ante uno nuevo.
- `CatalogoV40IT` queda fijado en `spring.flyway.target=40`.

**Referencias:**
- V40 queda congelada en `generacion_referencia_v40.txt` y la reproduce `GeneracionReferenciaV40IT`.
- La oficial pasa a V41: cambian **20 de 72** perfiles (IS 6, GF 6, GM 5, LW 3), todos de nivel avanzado (11 de gimnasio, 9 de casa), y los 72 siguen válidos.
- La referencia oficial es idéntica byte a byte a la de la simulación exploratoria V41.

### 2.2 Otros pendientes del catálogo (F1b)

Medido en la referencia oficial V41. No se modificó el catálogo.

| Caso | Uso | Estado |
|---|---|---|
| **C1:** 74 ejercicios sentados en banco o cajón (¿exige banco?) | **48 apariciones en perfiles de casa sin banco**: 115 ×18, 116 ×12, 143 ×9, 114 ×7 y 513 ×2. Además, 20 en gimnasio | **PENDIENTE DE DECISIÓN**. 114, 115, 116 y 143 son jalones con banda y la principal fuente de UPPER_PULL en casa. Exigir banco dejaría el tirón casi sin opciones en perfiles sin banco (el mismo dilema que D3) |
| **C2:** balón o bosu apoyado en el suelo (69, 559, 805, 806, 807) | 0 en las referencias | **PENDIENTE DE DECISIÓN**, sin impacto actual |
| **1113** *pike-to-cobra push-up* (nivel 1) | 11 apariciones, **4 de ellas en perfiles principiantes** (LW 60 casa, GM 30 y 60 casa, GF 60 casa) | **PENDIENTE DE DECISIÓN**. El nivel solo se cambia con el instrumento de dificultad (contrato V27) |
| **849** *ez barbell spider curl* | 0. Exige banco Scott, la opción más restrictiva | **VERIFICADO** técnicamente. Falta la confirmación humana del banco |
| **1106** *push-up to side plank* (nivel 2 **provisional**) | 2 (LW intermedio 60 casa, GF intermedio 45 casa) | **PENDIENTE DE DECISIÓN**. No hay evento en `exercise_difficulty_approvals` |

## 3. Generación inicial (cuatro objetivos)

| Punto (§7 de la orden) | Resultado |
|---|---|
| Selección, equilibrio, patrones y capacidades | 72/72 referencias válidas con V15, V18, V20, V23, V24 y V25. Barrido de 72: capacidades COMPLETO en 72/72 |
| Sesiones FULL_BODY, LOWER, PUSH y PULL; días y tiempos reducidos | Barrido de 4 a 6 días y robustez 288/288 (ver la simulación final en el informe de cierre) |
| Restricciones de equipamiento | 192 escenarios de restricciones: 0 con un implemento ausente y 0 violaciones |
| Dificultad, dosis y duración (V20 exacto) | Sin problemas automáticos. 1113 y 1106 en §2.2 |
| E1/E2 | `GeneracionReferenciaV37IT` y `V39IT` reproducen sus referencias congeladas: E1/E2 solo actúa en sesiones que incumplen V15 |
| Coherencia entre generación, copia y adaptación | `CopiaSinIaIT`, `LinajeIntegridadIT` y la validación multisemana (sin violaciones de linaje) |
| **Orden de los ejercicios** | `CapabilityWeekBuilder.ordenar` ordena por capacidad (fuerza, hipertrofia, resistencia, equilibrio, cardio, movilidad), no «multiarticular primero». En las referencias, **20 de 54 sesiones de GAIN_MUSCLE** y 9 de 54 de GENERAL_FITNESS ponen un ejercicio estructural de slot detrás de uno resistido no estructural. **No hay política aprobada que lo exija**, así que **no se cambió**: PENDIENTE DE DECISIÓN |

## 4. Adherencia

- `SessionOutcomeIdentifier`: una sesión es problemática si su cumplimiento es menor del 80 %. Si un ejercicio tuvo dolor, el motivo pasa a PAIN.
- **Decisión vigente respetada:** una sesión completada con dolor no se señala como incumplida (`completadaConDolorNoSeSenala`).
- El dolor se trata en el decisor (`CORRECCIONES_IMPLEMENTADAS.md`, corrección 3), no cambiando la adherencia.
- Las sesiones parciales por falta de tiempo siguen acortando (D4).
- Sin registro se obtiene INSUFFICIENT_DATA y no se adapta; validado en las secuencias «sin registro».

## 5. Progresión y reincorporación

- La progresión sigue **apagada por defecto**. No se reintrodujo la restauración automática (§3 del diseño v0.3, sustituido).
- **Aritmética verificada:** paso del 5 % (`progression_increase_pct`) y techo de 1,10 × base. Un escenario que no progresa por el 5 % se explicó por aritmética antes de tratarlo como fallo (precedente de `INFORME_V40.md` §3).
- **Pruebas en CI**, todas con la progresión encendida solo en la prueba:

  | Prueba | Cubre |
  |---|---|
  | `ProgresionIT` | progresión durante varias semanas |
  | `ProgresionCienPorCienIT` | 100 % dos semanas |
  | `PendientesProgresionIT` | techo, serie no recuperable |
  | `CopiaSinIaIT` | reincorporación y mantenimiento |
  | `HerenciaD8IT` | herencia de reducciones |

- Estas pruebas asumían ADAPT-1 (una omisión por falta de tiempo siempre acorta). Ahora declaran la subcausa de forma explícita con `saltoPorFaltaDeTiempo`; sus aserciones no cambian.

## 6. Trazabilidad

- V32 (`plan_adaptation_attempts`) registra:
  - las intervenciones por sesión, ahora con CONSERVAR;
  - la versión de la regla (`adaptation_rule_version`: ADAPT-1 en las filas existentes, ADAPT-2 en las nuevas).
- **CONSERVAR** no llama a la IA y queda como SIN_EFECTO, con 0 intentos.
- **Resultado por sesión adaptada:** `consultas/resultado_sesion_adaptada.sql` (solo lectura) clasifica cada sesión adaptada publicada como:
  - COMPLETADA;
  - PARCIAL_VALIDA;
  - PARCIAL_NO_VALIDA;
  - OMITIDA, con `misma_causa`;
  - SIN_INFORMACION.

  Lo prueba `AdaptacionPorCausasIT.resultadoPorSesionAdaptada`. **No es una prueba causal.**

## 7. Riesgos pendientes

1. **Cambio de perfil:** si la persona cambia sus días o su equipamiento, la semana copiada no se adapta y la siguiente queda sin publicar (V3/V1, NO_FACTIBLE). Es el pendiente de más prioridad. Ver `VALIDACION_MULTISEMANA.md` §4.
2. **C1:** los jalones con banda sentados se dan a perfiles sin banco.
3. **1113 en principiantes:** dificultad sin evaluar con el instrumento.
4. **Cotas inviables:** con MENOS_TIEMPO_HABITUAL, si los minutos declarados no permiten una sesión válida (V20), el resultado es RESPALDO (sesión original), no SIN_EFECTO. Es correcto por seguridad, pero el indicador lo cuenta como respaldo.
5. **A01 con IA real:** `A01AdaptacionRealIT` (de pago, solo con `-Dfitsense.replicate.real=true`) omite sin subcausa. Con ADAPT-2 esa sesión se conserva sin llamar a la IA. No se ejecutó.
