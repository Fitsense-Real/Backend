# Fallo intermitente: `ProteccionPlanesPublicadosIT.reparacionesSimultaneas`

Fecha: 16/11/2026. Encontrado durante la integración controlada de #73, #71 y #72. **Detuvo la integración antes del
merge de #71**, como manda la orden ante un fallo inesperado.

## 1. Dónde apareció

| Ejecución | Árbol | Resultado |
|---|---|---|
| Regresión local de #72 actualizada (`a74cbf0` = #72 + #71 actualizada + `generation` con #73) | 1576 unitarias, 809 de integración | **1 fallo**: esta prueba |
| Regresión local de #72 + la corrección de #73, esta mañana (árbol equivalente) | 1576 / 809 | 0 fallos |
| Regresión local de #71 actualizada (`3e83b6f`) | 1555 / 774 | 0 fallos |
| CI de `generation` tras #73 (`41934d6`) y CI de #71 (`3e83b6f`) | — | verde |
| La prueba sola, 8 veces en `generation` y 8 veces en #72 | — | **16/16 pasan** |

```
reparacionesSimultaneas: [NO_VALIDA, REPARADA] ==> expected: <true> but was: <false>
REPARACION_SEGURIDAD user=6 estado=REPARADA nuevo=7 sustituciones=1
REPARACION_SEGURIDAD user=6 estado=NO_VALIDA nuevo=null sustituciones=0 problemas=[
  SEGURIDAD: siguen en la semana [1374] ...,
  V8C: se iba a conservar la semana y falta la semana medida de la que deriva ...,
  V1: el ejercicio 1374 no esta en available_exercises.]
```

Lo que comprueba la prueba se cumplió:

- se publica una sola reparación;
- hay 2 versiones y 1 activa.

Falló solo la clasificación del perdedor: se espera REPARADA, CAMBIO_CONCURRENTE o NADA_QUE_REPARAR, y dio NO_VALIDA.

## 2. Causa

`PublishedPlanSafetyRepairService.reparar` trabaja por fases, cada una en su transacción:

1. lee la versión activa (v1, con el 1374);
2. prepara el contexto y lee la semana publicada, que hace de semana de referencia;
3. sustituye y valida;
4. publica solo si la versión activa sigue siendo la leída en la fase 1. Si no lo es, da CAMBIO_CONCURRENTE.

Si la reparación ganadora publica la v2 **entre la fase 1 y la fase 2** de la perdedora, ocurre lo siguiente:

1. La perdedora lee la v2 como semana de referencia, y la v2 ya no tiene el 1374.
2. `SafetySubstitutionPolicy` no encuentra qué sustituir (`sustituciones=0`).
3. La validación de la fase 3 falla, porque la semana sigue con el 1374.
4. El servicio devuelve NO_VALIDA antes de llegar a la fase 4, que habría devuelto CAMBIO_CONCURRENTE.

La ventana es estrecha. Solo se abre si la perdedora se retrasa entre sus fases 1 y 2, por ejemplo con la CPU cargada
durante la suite completa.

## 3. Alcance

- **No es de #71 ni de #72.** El servicio y la prueba son idénticos en `generation` y en #72: el diff es vacío.
  Ninguna de las dos PR toca la reparación de seguridad.
- **No hay impacto en producción.** `PlanningContextFacade.repairPublishedPlanForSafety` es el único punto de
  entrada y está documentado como «NADIE LA LLAMA TODAVIA: no hay endpoint ni tarea».
- **No hay riesgo de seguridad ni de datos.** NO_VALIDA no publica nada. `ux_plan_active` y
  `uq_plan_user_week_version` siguen impidiendo dos versiones activas; la prueba lo comprueba y se cumple.
- **Lo que sí es incorrecto** es el estado que se registra: NO_VALIDA («requiere intervención») cuando el motivo real
  es un cambio concurrente.

## 4. Corrección (aplicada en #74, fusionada el 17/11/2026 como `f8ffb61`)

Es mínima y está en el servicio de reparación. No toca validadores, catálogo, políticas ni adherencia.

1. En la fase 2, comprobar que la semana publicada leída es la misma versión que la de la fase 1. También vale volver a
   leer la versión activa justo antes de devolver NO_VALIDA o SIN_SUSTITUTO.
2. Si ha cambiado, devolver CAMBIO_CONCURRENTE.
3. Con una prueba determinista que fuerce la ventana, por ejemplo publicando la v2 entre las fases con un gancho de
   prueba.

Iría en una PR pequeña e independiente, porque es de alcance distinto al de esta orden.

**Decide:** el usuario. Las opciones son:

- **(a)** continuar con la integración de #71 y #72 dejando esto registrado (PD-16), ya que es preexistente y está en
  código sin llamadores;
- **(b)** corregirlo antes en una PR aparte.
