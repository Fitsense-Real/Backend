# 04 · Continuidad del cierre semanal

**PR:** [#98](https://github.com/Fitsense-Real/Backend/pull/98) (Backend-C3), **fusionada** (`cca9fc21`, cabeza `c70b0f1`). Sin migraciones. Lo que ya hacían #89 (V66) y #92 (B3) **no se reescribe**.

## 1. La ventana de 24 horas: lo que hay de verdad

La auditoría decía que la ventana estaba «fijada en el planificador». **No es exacto:**
- `fitsense.weekly-cycle.catch-up-hours` (`FITSENSE_WEEKLY_CYCLE_CATCH_UP_HOURS`) vale 24 por omisión y admite de 0 a 144;
- solo el arnés de 12 semanas (`Estudio12SemanasIT`) la pasa como constante. Por eso la variable de entorno no llega a esa prueba (`cierre_preproduccion/11_INTEGRACION_FINAL_Y_MERGES.md` §6).

Lo que ocurre según el retraso respecto al lunes 03:00 locales:

| Momento | Quién actúa | Qué pasa |
|---|---|---|
| Lunes 03:00 | Planificador (B3) | Cierre de toda la zona: medir la semana anterior, decidir y publicar la nueva |
| De 03:00 a +24 h | Planificador (B3) | Cada hora, solo los **pendientes** (sin medición o sin plan nuevo), como mucho 3 intentos por persona y semana |
| Un ciclo que empezó y se interrumpió (proceso caído) | Recuperación de V66 (#89) | Se relanza adoptando su operación y su libro de predicciones: no se paga otra vez la IA ni se publica dos veces |
| **Más de 24 h** | Nadie, **a propósito** | No se cierra ni se publica tarde |

### 1.1 Por qué no se recupera sola una semana pasadas 24 h

Es una decisión de **semántica temporal del estudio**, no técnica:
1. **Medir tarde puede no dar la misma medición que a su hora.** Hasta que se cierra, el plan de la semana sigue `ACTIVE`. Lo que haya pasado con sus sesiones durante la caída depende de cuándo vuelva a correr la tarea de vencimiento de las 00:30 y de las sesiones que quedaran abiertas. **No se ha comprobado caso por caso**, y por eso no se automatiza.
2. **Publicar tarde aparenta disponibilidad.** Un plan de la semana nueva publicado el miércoles tiene como `week_start_date` el lunes, aunque la persona no lo tuvo hasta el miércoles. La fila guarda `created_at`, pero un análisis por semana lo trataría como un plan de semana completa.
3. **Generar semanas ya pasadas sería inventar evidencia.** Nunca se hace.

Por eso C3 **no** añade ninguna recuperación tardía automática. Añade que esas semanas no queden en silencio.

## 2. Lo que añade C3

| Requisito de la orden | Implementación |
|---|---|
| Identificar las semanas pendientes | `IndicadoresOperativosService.cierresAtrasados()`: todas las semanas de las **últimas 13** (`fitsense.weekly-cycle.overdue-lookback-weeks`) en que un participante activo tuvo plan, no tiene medición y ya pasó su cierre. Antes solo se contaba la semana anterior, y una caída de dos semanas desaparecía del indicador al lunes siguiente |
| Distinguir lo que se recupera solo | `EN_VENTANA` (dentro de `catch-up-hours`, lo hará el planificador) o `FUERA_DE_VENTANA` (necesita una decisión) |
| Qué falta (revisión final) | `reason`: `SIN_MEDICION` (la semana no se midió) o **`SIN_PLAN_NUEVO`** (se midió, pero el cierre no publicó el plan de la semana siguiente). Más `measured` y `newWeekWithoutPlan`. Una semana sin plan no se espera medir: no aparece; aparece la anterior como `SIN_PLAN_NUEVO` |
| Notificar al equipo responsable | `ALERTA_OPERATIVA tipo=CIERRE_SEMANAL_FUERA_DE_VENTANA valor=<n>` en **ERROR**, solo con el recuento y la ruta de consulta; medidor `fitsense.operaciones.cierres_fuera_de_ventana`; campo `cierresFueraDeVentana` en `GET /api/v1/admin/operations`. La regla de Railway sobre `ALERTA_OPERATIVA` (`cierre_preproduccion/07_TRAZABILIDAD_Y_MONITOREO.md`) la recoge sin cambios |
| Saber de quién y qué semana | `GET /api/v1/admin/operations/weekly-closures/overdue` (ADMIN): `participantCode` (P-xxx), zona, semana medida, horas de retraso y estado. **Sin correos.** Queda auditado como consulta de datos de participantes (`ADMIN_AUDIT … weekly-closures-overdue`) |
| Evitar cierres duplicados, dos planes activos y sobrescribir evidencia | Todo es de **solo lectura**: no cierra, no mide, no decide ni publica. La idempotencia de `WeeklyCycleService.ejecutar` (un cierre, una medición y una publicación por semana medida, con bloqueo por participante) no cambia |
| No generar retrospectivamente | Probado: el planificador real a las 25, 30, 48 y 72 h del cierre no crea medición ni plan y deja un solo plan activo |

Ejemplo de respuesta (sintética):

```json
{
  "calculatedAt": "2026-11-28T10:00:00Z",
  "catchUpHours": 24,
  "items": [
    { "participantCode": "P-014", "timezone": "America/Lima", "measuredWeekStart": "2026-11-09",
      "hoursSinceDue": 223, "status": "FUERA_DE_VENTANA", "newWeekWithoutPlan": true,
      "measured": false, "reason": "SIN_MEDICION" },
    { "participantCode": "P-014", "timezone": "America/Lima", "measuredWeekStart": "2026-11-16",
      "hoursSinceDue": 55, "status": "FUERA_DE_VENTANA", "newWeekWithoutPlan": true,
      "measured": true, "reason": "SIN_PLAN_NUEVO" }
  ]
}
```

## 3. Procedimiento ante una interrupción de más de 24 horas (propuesta verificable)

**No es automático.** Lo ejecuta la persona responsable de operación, con el visto bueno de la responsable del estudio para el paso 4.

1. **Detectar.** Llega la alerta `CIERRE_SEMANAL_FUERA_DE_VENTANA`, o se consulta `GET /admin/operations/weekly-closures/overdue`.
2. **Acotar.** Anotar en el acta el intervalo de la caída: el último `Cierre semanal en …` del log y el primer arranque después. Anotar también las semanas y los códigos P-xxx afectados, copiados de la respuesta.
3. **Restablecer el servicio.** Comprobar el arranque, `GET /actuator/health` y que las alertas `OPERACIONES_HUERFANAS` y `CICLO_SEMANAL_PENDIENTE` se vacían. Los ciclos que estuvieran dentro de la ventana se recuperan solos.
4. **Decidir, por cada semana `FUERA_DE_VENTANA`** (decisión del estudio, `08` B-2). Las opciones:
   - **a) Registrar la semana como no medida** (adherencia nula por caída del servicio) y continuar con el cierre normal del lunes siguiente. No escribe nada en la base: es la opción que **no cambia la semántica temporal** y la que se recomienda mientras no haya otra decisión.
   - **b) Cerrar y medir la semana tarde**, sabiendo que la medición puede no coincidir con la que se habría hecho a su hora (§1.1). Hoy no existe una acción para hacerlo y no se implementa sin la decisión.
   - **c) Publicar tarde el plan de la semana en curso**, declarando la fecha real de publicación. Tampoco existe todavía.
5. **Verificar.** Repetir la consulta del paso 1. Con la opción a), las semanas siguen apareciendo hasta que salen de las 13 semanas de búsqueda: el acta es la constancia.
6. **Acta.** Incluir fecha, intervalo, semanas, códigos, decisión y quién la tomó, y archivarla con la documentación del estudio.

## 3.1 Conservar el registro de incidencias más allá de 13 semanas

La consulta solo mira las últimas 13 semanas (`overdue-lookback-weeks`, ampliable hasta 60). Para que la incidencia no se pierda cuando deja de aparecer:
1. **Acta** (paso 6 de §3) en la documentación del estudio, con los códigos P-xxx, las semanas y la decisión.
2. **Copia de la respuesta** de `GET /admin/operations/weekly-closures/overdue`, guardada como JSON junto al acta el día de la detección. Es seudonimizada y la consulta queda auditada.
3. **El dato no desaparece de la base.** La semana sigue sin medición o sin plan siguiente, así que cualquier análisis posterior la ve. La lista de 13 semanas es una ayuda operativa, no el registro oficial.
4. Las líneas `ALERTA_OPERATIVA` del log dependen de la retención de Railway: no son un registro duradero.

## 4. Pruebas

`CierresProlongadosIT` (C3):

| Prueba | Qué demuestra |
|---|---|
| `dosSemanasSinCerrar` | Una caída de dos semanas deja las **dos** semanas identificadas; la antigua, `FUERA_DE_VENTANA` (más de 7×24 h) |
| `soloLoQueFaltaDeVerdad` | No aparecen las semanas medidas, las cuentas retiradas ni las que no participan |
| `soloLectura` | Repetir la detección y la revisión de alertas no crea ninguna medición, plan, operación ni intervención |
| `alerta` | `CIERRE_SEMANAL_FUERA_DE_VENTANA` en ERROR, sin código P-xxx ni correo en el log |
| `endpoint` | ADMIN 200 (códigos, ningún «@»), consulta auditada; USER 403 |
| `fueraDeVentanaNoSeRecuperaSolo` | Planificador real a las 25, 30, 48 y 72 h: ni medición ni plan tardío, un solo plan activo |
| `medidaSinPlanNuevo` (revisión final) | Semana medida sin plan siguiente: `SIN_PLAN_NUEVO`, `FUERA_DE_VENTANA` y alerta. La semana sin plan no aparece («falta de datos de una semana») |
| `sobreviveAUnReinicio` | Una instancia nueva del servicio ve lo mismo: la detección sale de la base, no de la memoria |
| `reanudacionDentroDeVentana` | Al reanudar dentro de la ventana, el planificador recupera la semana y deja de aparecer |
| `concurrentes` | Seis detecciones y seis revisiones de alertas en paralelo: mismo resultado, ninguna escritura |

Resultado tras la revisión, con `OperacionYExportacionIT` y `CierreSemanalRecuperableIT`: **24 de integración, 0 fallos**. Después del merge, también con `SeguridadRolesIT`, `Estudio12SemanasIT` y `AdherenciaIT`: **63, 0 fallos**.

## 5. Estado

| Punto | Clasificación |
|---|---|
| Recuperación automática en 24 h y relanzamiento de interrumpidos | **RESUELTO** (#89 y #92, ya en `generation`) |
| Detección, alerta y consulta de cierres de más de 24 h | **RESUELTO** (integrado: #98) |
| Qué hacer con una semana fuera de ventana (a, b o c) | **BLOQUEADO POR APROBACIÓN** (protocolo; `08` B-2) |
| Acción manual de cierre o publicación tardía | **PENDIENTE** (solo si se aprueban b o c; afecta a la medición) |
