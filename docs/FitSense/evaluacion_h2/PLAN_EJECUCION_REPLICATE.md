# Plan de ejecución con Replicate real (fases 5 y 7)

**No ejecutado.** Cada paso pagado necesita tu autorización expresa.

## 0. Requisitos previos (sin coste)

| Requisito | Estado |
|---|---|
| Rama | `fix/h2-contrato-prompt-v15` (#69), con `generation` `2578719` integrado. Contiene el arnés y el tope |
| Pruebas sin IA | En verde (`PRUEBAS_H2_SIN_IA.md`) |
| Ensayo del arnés | Hecho (`DISENO_EVALUACION_PAREADA.md`) |
| **Acceso de red a `api.replicate.com`** | **Falta.** En esta sesión el host no resuelve. Hay que añadirlo a la red permitida del entorno |
| **Token de Replicate** | **Falta.** Se configura como secreto del entorno (cabecera `Authorization: Bearer`). Nunca en el repositorio ni en la conversación |
| Base de datos | PostgreSQL efímero (Testcontainers). **Nunca Neon** |
| Límite de gasto en la cuenta de Replicate | Recomendado como segunda barrera |

## 1. Bloque G · generación pareada A/B

```bash
sh mvnw -o verify -Pintegration -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false \
  -Dfailsafe.failIfNoSpecifiedTests=false -Dit.test=EvaluacionPareadaH2IT \
  -Dfitsense.h2.pareada.modo=REAL -Dfitsense.h2.pareada.autorizado=SI \
  -Dfitsense.h2.pareada.tope-usd=7.20 -Dfitsense.h2.pareada.max-predicciones=144
```

**Duración prevista:** unos 75 s por generación × 48, es decir, **1–1,5 h**.

**Salida** en `target/h2-pareada/real/`:

- `casos.csv`, `predicciones.csv` y `salidas/`;
- `metricas_replicate.csv`, `tiempos.log` y `resumen.md`.

**Piloto opcional:** 2 parejas antes de todo el bloque (`-Dfitsense.h2.pareada.casos=G01,G06`, máximo 12 predicciones, unos USD 0,25).

## 2. Bloque AD · adaptación real (configuración A)

Se calcula `T = 8,00 − USD medidos en G`.

```bash
# modo adaptacion: 4 perfiles, 9 semanas
sh mvnw -o verify -Pintegration -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false \
  -Dfailsafe.failIfNoSpecifiedTests=false -Dit.test=CampanaReplicateRealIT -Dfitsense.replicate.real=true \
  -Dfitsense.replicate.perfiles=GAIN_MUSCLE/INTERMEDIATE/45/GYM,LOSE_WEIGHT/BEGINNER/30/CASA,INCREASE_STRENGTH/INTERMEDIATE/45/GYM,GENERAL_FITNESS/BEGINNER/30/CASA \
  -Dfitsense.replicate.tope-usd=$T -Dfitsense.replicate.max-predicciones=84
# modo progresion: 2 perfiles, 10 semanas (tope = T − medido en el paso anterior)
sh mvnw ... -Dfitsense.replicate.progresion=true \
  -Dfitsense.replicate.perfiles=GAIN_MUSCLE/INTERMEDIATE/60/GYM,INCREASE_STRENGTH/INTERMEDIATE/60/GYM \
  -Dfitsense.replicate.max-predicciones=18 -Dfitsense.replicate.tope-usd=$T2
```

**Salida** en `target/replicate-real/{adaptacion,progresion}/`:

- llamadas, ciclos, violaciones, métricas de Replicate e informe.

Cada ciclo deja su desenlace explícito: APLICADA, SIN_EFECTO, **RESPALDO**, RECHAZADA o NO_FACTIBLE. **Nunca se sustituye una respuesta inválida por el motor de reglas sin dejar constancia.**

## 3. Análisis

1. **Oráculo independiente:** el validador del arnés sobre lo publicado y la revisión de V8C, P-03 y D8 en las adaptaciones.
2. **Comparación A/B** pareada por caso (`CRITERIOS_ACEPTACION_H2.md`), con los casos en que A sale mejor visibles.
3. **Entregables con evidencia real** en `docs/FitSense/evaluacion_h2/real/`:
   - respuestas del proveedor y resultados de los validadores;
   - tiempos, costes, comparación A/B y conclusiones;
   - SHA, comandos y versión del modelo.

## 4. Bloque S · flujo asíncrono (#71), fase 7: dependencia

**Estado de #71** (rama `claude/async-first-plan-generation`, `e14a28e`, de otra sesión):

- abierta y sin fusionar;
- usa V58;
- no está en `generation` ni en #69.

**No se prueba aquí**: no hay un entorno aislado de evaluación con #71 + #69 y acceso a Replicate.

**Instrumental preparado:**

| Escenario | Qué mide | Predicciones máx. |
|---|---|---|
| S1 · normal | `POST /users/me/plan/generation` → 202; consultas → COMPLETED. Mide el tiempo hasta crear la operación, la espera del proveedor (REPLICATE_TIMING), la validación y la publicación | 3 |
| S2 · la app pierde la conexión | Sin consultas durante la generación; después, `GET …/latest` | 3 |
| S3 · reinicio del backend en PROCESSING | Parada y arranque a mitad. Mide el reencolado, el estado final y si se pagan predicciones **duplicadas** | 6 |
| S4 · doble petición simultánea | Una sola operación y una sola generación | 3 |

**Hallazgos de revisión sin coste sobre #71**, para quien la revise (no he comentado en esa PR):

1. **`max-processing-seconds=600` es menor que el peor caso del pipeline con IA real.**
   - El peor caso son 3 intentos de hasta unos 291 s (unos 331 s con la cancelación): **unos 873–993 s**.
   - Si se supera, la operación pasa a `FAILED TIEMPO_EXCEDIDO` y **se descarta un resultado ya pagado**; un reintento del participante paga otra generación.
   - Con lo medido en GRA (máximo de 350 s, p95 de 264 s) no ocurriría casi nunca, pero el peor caso existe.
   - **Antes de desplegarlo** conviene elegir entre:
     - subir `max-processing-seconds` por encima de ~1.000 s;
     - activar `fitsense.ai.generate.total-budget-seconds`, que hoy vale 0, para acotar el pipeline por debajo de ese tiempo.
2. **Reencolado tras un reinicio** (`stale-seconds=180`, latido cada 20 s):
   - Una operación huérfana se vuelve a ejecutar una vez.
   - Si el proceso murió con una predicción en curso, esa predicción sigue en Replicate hasta que termina; no se cancela.
   - **Puede haber una predicción pagada y no aprovechada.** S3 lo mediría.

## Pendientes

- Tu autorización y el tope (`PRESUPUESTO_REPLICATE.md`).
- Habilitar `api.replicate.com` y el token en el entorno.
- Para el bloque S: un entorno de evaluación con #71 y #69, que no existe todavía.
