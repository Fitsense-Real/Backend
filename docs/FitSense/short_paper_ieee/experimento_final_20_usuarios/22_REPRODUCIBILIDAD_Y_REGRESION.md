# Reproducibilidad y regresión

## 1. Reproducibilidad del experimento determinista

**A0, A1 y la repetición de A1 son idénticas byte a byte**, cada una en una base vacía distinta. El detalle está en `12_COMPARACION_A0_A1.md`.
- Mismos `userId` (1–20) y semillas (`userId × 1.000.003 + 20.759`).
- Mismos planes iniciales y siguientes, ejercicio a ejercicio.
- Mismas métricas, decisiones y estados V32.

## 2. Código evaluado

| Elemento | Valor |
|---|---|
| Backend | `generation` `0974fb3` (`backend-mvp-1.0`), **sin ningún cambio en `src/main`** |
| Rama del laboratorio | `lab/experimento-kora-20usuarios`. Solo añade `ExperimentoKora20IT` (*opt-in*) y documentación |
| Commits | Protocolo `d3c78d7` → laboratorio v1 `911a0d3` → A0 `33d8d06` → laboratorio v2 `2c0cc8b` → A1 `132253c` → documentos |

## 3. Regresión (con Replicate apagado; ninguna llamada al proveedor)

Ejecutada en la rama del laboratorio, con PostgreSQL efímero:

| Bloque | Resultado |
|---|---|
| Unitarias | **1.043**, con 0 fallos y 0 omitidas |
| Integración completa: generación, V1–V25, V8C, V24, adherencia, adaptación, RESPALDO, P-02/P-03, progresión, reincorporación, concurrencia, idempotencia, seguridad y contratos de la API | **489 ejecutadas**, 0 fallos y 37 omitidas. Son las 36 *opt-in* de siempre más `ExperimentoKora20IT`, que también es *opt-in*.<br>**No se cuentan como aprobadas** |
| 960 perfiles × `PLAN-HIST-1` | **Idéntica byte a byte** a `generation` 7c19703 y 0974fb3 (`planes.csv` 761d67cf…, `colocaciones.csv` fa9fa3ed…) |
| REPLAY de 100 semanas (cohorte 02) | **Idéntica.** Solo cambian 2 líneas de `exclusiones_dolor.csv`, iguales al normalizar las fechas, y se descartaron sin commit |
| Bloqueo de la etapa C sin token | El modo REPLICATE falla en la guarda previa («BLOQUEADO…») con **0 llamadas** y sin crear evidencia (`evidencia/C_bloqueo/intento_sin_token.log`) |

**Conclusión:** sin cambios en producción, el comportamiento histórico sigue igual. La REPLAY y los 960 perfiles detectan regresiones, pero **no sustituyen** a la cohorte experimental de 20 usuarios.

## 4. Etapa C: comparabilidad con A

- **Plan inicial:** en la ejecución válida de C, el de U01–U20 es **idéntico byte a byte** al de A1 (400 filas).
- **Medición y decisión:** también idénticas a A1 (`ejecucion.csv`, `adherencia.csv`, decisiones, sesiones señaladas e intervenciones).
- **Por tanto,** las diferencias en la adaptación se deben solo a las propuestas de Replicate.

**La salida del modelo no es determinista.** La etapa C no se repitió para buscar mejores respuestas. Los intentos 1 y 2 se conservan como fallidos (laboratorio y proxy), y solo el 3 se usa como resultado.
