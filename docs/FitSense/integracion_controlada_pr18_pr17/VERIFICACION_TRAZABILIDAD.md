# Verificación de la trazabilidad tras la integración

**Archivo:** `PlanningPolicyTrace.java` del PR #17 actualizado (1e42519).

## 1. Cómo se integró

1. Se partió de la versión del PR #17 (6d6e522).
2. Se le aplicaron, una a una, las 5 modificaciones de a52e35e (PR #18).

**Resultado:** la diferencia entre el archivo integrado y el de `generation` es **exactamente** la parte propia del PR #17. No hay ninguna diferencia en los campos de V4-C:

```
+ (Javadoc) ... si se publico por la EXCEPCION C2-D ...
+ params.put("c2d", politica.c2d());
+ if (politica.elegibilidadFuncional()) params.put("elegibilidad_funcional", ElegibilidadFuncional.VERSION);
+ int excepciones = 0;
+ boolean c2d = bajo && !exenta && politica.c2d();
+ if (c2d) excepciones++;
+ x.put("excepcion_c2d", c2d);
+ t.put("excepciones_c2d", excepciones);
```

## 2. Campos de la traza integrada

| Campo | Origen | Presente |
|---|---|---|
| `politica`, `version_calculo`, `origen`, `contexto` | V44 | Sí |
| `parametros.v4`, `piso_v20_pct`, `piso_v20_segundos`, `tolerancia_v4_pct`, `limite_v4` | V44 | Sí, con el significado de siempre |
| `minutos_disponibles` | V44 | Sí (efectivos) |
| **`minutos_perfil`**, **`minutos_efectivos`** | PR #18 (a52e35e) | **Sí** |
| **`parametros.limite_v4_aplicado`** `{regla, minutos_base, segundos \| minutos_declarados}` | PR #18 | **Sí** |
| **`parametros.limite_v4_minutos_base`** | PR #18 | **Sí** |
| **`sesiones[].dentro_limite_v4_aplicado`** | PR #18 | **Sí** |
| `segundos_estimados` y `pct_de_m` en `null` sin `prescription` | PR #18 | **Sí** |
| `parametros.c2d` | PR #17 (C2-D) | Sí |
| `sesiones[].excepcion_c2d` y `excepciones_c2d` | PR #17 (C2-D) | Sí |
| `parametros.elegibilidad_funcional` (solo con EF) | PR #17 (EF-0.1) | Sí |

## 3. La traza corresponde a las decisiones reales

| Decisión | Fuente de la traza | Comprobación |
|---|---|---|
| Límite V4 aplicado | Misma condición y tiempo base que `PlanDraftValidator.validateSessions`. La regla V4 del validador del PR #17 es **idéntica** a la de `generation` | `TrazaV4CTest` (15, contra el validador real), `PoliticaV4CCicloIT.acortarTrazaDiferenciada` y `TrazaHistoricaIT`, ejecutadas en el PR #17 integrado |
| Excepción C2-D | `bajo && !exenta && c2d`, sin cambios. Con `segundos_estimados = null` (sin `prescription`), `bajo` es `false`, igual que antes (piso 0) | `PoliticasP123IT` con C2-D (la traza registra la excepción en la fecha) |
| EF | `elegibilidad_funcional` solo con versiones EF | Ciclos y baterías EF |

**Consumidores de la traza en el PR #17:**
- `DuracionYAerobicaAssembler` ya trata la ausencia de `segundos_estimados`.
- Sus cambios (`DURACION_POR_CONTENIDO` y `excepcion_c2d`) siguen igual.
- **`availableMinutes` sigue tomando `minutos_disponibles`**, los minutos efectivos: es el tiempo que la persona tiene esa semana.
