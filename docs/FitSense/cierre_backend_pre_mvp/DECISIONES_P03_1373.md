# Decisiones: P-03 localizado, ejercicio 1373 y NO_FACTIBLE

**Fecha de la orden:** 02/11/2026.

**Alcance:** solo documentación. **No se ha cambiado** nada de lo siguiente:
- código, generador o V8C;
- migraciones o catálogo;
- equivalencias o reglas de dosis o dificultad.

## 1. P-03 localizado: **pospuesto**

| | Estado |
|---|---|
| Diseño | Se conserva en `cierre_respaldo_y_preparacion_mvp/DISENO_P03_LOCALIZADO.md` |
| Prototipo de medición | Se conserva en `AnalisisSustitucion1373IT` (*opt-in*), con evidencia en `evidencia/prototipo_localizado.md` y `.csv` |
| Comparación | `COMPARACION_P03_ACTUAL_VS_LOCALIZADA.md` |
| Implementación | **No**. No se ha creado `SafeLocalizedRegenerationService` |
| P-03 nivel 2 | **Sin cambios**: sigue siendo el comportamiento de producción |

**Lo medido (40 casos con regeneración, 10 por objetivo; IA simulada):**

| | Nivel 2 actual | Localizada (prototipo) |
|---|---|---|
| Sesiones cambiadas | 120/120 (toda la semana) | Solo el día afectado |
| Publicable (V1–V25, sin excluidos, sin más trabajo) | 30/40 | 35/40 |
| Las dos en cadena (localizada y, si falla, nivel 2) | — | 36/40 |
| Caso C2 (de repeticiones a duración) | Rechazada (segundos 1.200 → 1.260) | **También rechazada** (360 → 420 en la sesión) |

**Limitaciones conocidas de la variante localizada:**
- necesita una excepción nueva en V8C («sesión reconstruida por seguridad»);
- necesita un resultado nuevo en la API (`REGENERACION_SEGURA_SESION`);
- necesita un linaje mixto;
- necesita una clave de idempotencia;
- la sesión se arma sin conocer las demás, con riesgo de repeticiones entre días y de incumplir V18 o los slots semanales. El prototipo solo lo detecta.

**Pruebas propuestas si se retoma:**
- los 40 casos del prototipo como regresión;
- C1 y C2 como casos fijos;
- dolor en varios días;
- dosis ya reducidas (D8);
- idempotencia y concurrencia del ciclo;
- invariantes A, B y C de `InvariantesRespaldo` sobre el día reconstruido;
- el contrato de la API con el resultado nuevo.

## 2. Ejercicio 1373 «Círculos de muñeca»: **sin cambios**

- Se mantiene **SETS_REPS**.
- No se crea ninguna equivalencia entre repeticiones y segundos.
- SUST-SEG-1 y el catálogo quedan como están.
- Si duele y no hay sustituto, el resultado sigue siendo **NO_FACTIBLE**, salvo que P-03 nivel 2 encuentre una semana válida dentro del trabajo medido.

**Ficha técnica para la revisión profesional posterior:** `FICHA_TECNICA_1373.md`. Contiene los atributos, el embudo de SUST-SEG-1, los candidatos descartados y el motivo del rechazo. Está **fuera** del paquete ciego y no se envía con él.

## 3. NO_FACTIBLE: confirmación de la decisión funcional

**Principio:** «FitSense adapta únicamente lo necesario, respeta las restricciones del usuario y nunca publica una rutina inválida para evitar un resultado NO_FACTIBLE.»

| Situación | Comportamiento vigente (`generation` 7c19703) | Prueba que lo fija |
|---|---|---|
| Dolor y sustituto seguro | RESPALDO con **una** sustitución SUST-SEG-1 trazada | `RespaldoConDolorIT`, caso B |
| Dolor sin sustituto (1373) | P-03 nivel 2. Si tampoco hay semana válida: **NO_FACTIBLE**, sin plan activo y con la exclusión activa | `RespaldoConDolorIT`, caso C; `InvariantesRespaldo.sinPublicar` |
| Sin dolor | Nunca NO_FACTIBLE por respaldo: se conserva la semana | `ValidacionMultisemanaIT`; `RespaldoConDolorIT`, caso A |
| Reincorporación | Solo con `POST /pain-exclusions/{id}/reactivation` y `confirmNoPain=true` | P-02 |

**Para la app:**
- El estado se ve en `GET /plan/adaptations` (`NO_FACTIBLE_POR_SEGURIDAD`) y en la ausencia de plan en `GET /plan/current`.
- Flutter muestra un mensaje no alarmista con el detalle técnico plegado.
- La propuesta de contrato estructurado es **solo diseño**: `MobileApp/docs/mvp/MODELOS_Y_CONTRATOS.md` §3. No hay cambios en el backend.
