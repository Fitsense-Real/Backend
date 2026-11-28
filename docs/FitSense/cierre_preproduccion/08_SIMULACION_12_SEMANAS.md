# 08 · Simulación de 12 semanas (Backend-B6)

- **Rama:** `preproduccion/b6-simulacion-12-semanas`. Está **apilada sobre B3** (`preproduccion/b3-cierre-semanal-recuperable`) porque usa el planificador recuperable y el control de versión de B3.
- **Prueba:** `src/test/java/main/web/services/fitsense/orchestation/Estudio12SemanasIT.java`. **Se ejecuta en la suite por omisión** (`mvn verify -Pintegration`): ~90 s en local.

> **Nada de esta prueba es IA real.** Replicate es un servidor HTTP **local** usado con el cliente de producción. Sus salidas son JSON válido que el contrato rechaza, así que **todos los planes salen del motor de reglas** (`RULE_ENGINE`) o se conservan (`CONSERVED`). Firebase y el correo son sustitutos en memoria. **Cero llamadas pagadas.**

## 1. Cómo funciona

| Elemento | Implementación |
|---|---|
| Base de datos | PostgreSQL 16 efímero (Testcontainers). Ningún dato real; nunca el usuario demo 48 |
| Reloj | **El planificador real** (`WeeklyCycleScheduler`, B3), construido en cada tick con el reloj fijado en el lunes a las 03:00 de cada zona (Tokio, Madrid y Lima, en el orden en que llegan). No se llama a `runFor` con fechas |
| Ejecución de sesiones | Servicios reales: iniciar, registrar ejercicios, terminar u omitir. Los vencimientos los controla la prueba (`RelojPruebas.abrirVentanaDeEjecucion`) |
| Replicate | Servidor local. Modo `RECHAZABLE` (salida que el contrato rechaza → reintentos y respaldo), `SIN_ID` (HTTP 500 sin id) y `CORTE` (cierra la conexión) |
| Firebase | `TransportePush` simulado (`@Primary`). Se ejecutan el detector y el emisor reales cada semana |
| Correo | `PasswordResetNotifier` en memoria (el código de recuperación) |
| Cohorte | 12 participantes sintéticos registrados por la API real, con participación explícita |

## 2. Escenarios y resultado

**Resultado: 1/1 en verde (97 s).** Cada fila tiene su comprobación en la prueba; un fallo nombra el escenario.

| ID | Escenario | Cómo se reproduce | Qué se comprueba | Resultado |
|---|---|---|---|---|
| E12-01 | Adherencia alta 12 semanas | U01 hace todo | Un plan por semana; una medición por semana | OK |
| E12-02 | Adherencia baja y recuperación | U02: S1–S4 omite, S5–S12 hace todo | `ADAPT_EXPANDED` y después `NO_CHANGE` (CSV) | OK |
| E12-03 | Parciales y omitidas | U03 alterna parcial y omitida | Mediciones y adaptaciones cada semana | OK |
| E12-04 | Semana sin actividad | U04: S6 y S7 sin registro | Se cierra igual (`INSUFFICIENT_DATA`), sin perder semanas; vuelve en S8 | OK |
| E12-05 | Dolor y exclusiones O20 | U05 deja por dolor un ejercicio en S3 | Queda excluido y **ni él ni sus equivalentes vuelven** en ningún plan posterior | OK |
| E12-06 | Progresión y reincorporación | U01 y U06 (fatiga en S5) | Al menos una progresión (`progressed_sessions`) | OK |
| E12-07 | Adaptaciones sucesivas | U03 | ≥ 3 semanas con `ADAPT_*` | OK |
| E12-08 | Semana sin cambios | U01 | ≥ 1 `NO_CHANGE` | OK |
| E12-09 | Reinicio antes del cierre | S4: en Lima el tick de las 03:00 no llega; vuelve a las 05:00 | Premisa: la semana queda pendiente. Después: cerrada y con plan | OK |
| E12-10 | Interrupción durante la adaptación | S7: la adaptación de U09 falla una vez (caída simulada **con una excepción**) | Premisa: no hay plan de S8. El tick de las 04:00 lo recupera | OK |
| E12-11 | Predicción sin identificador confirmado | U12 se da de alta en S9; primer plan por la ruta asíncrona (libro V59); Replicate responde 500 sin id | **1 solo POST** (no se reenvía), fila `NO_CONFIRMADA`, plan publicado por el respaldo | OK |
| E12-12 | Dos cierres concurrentes | S10: dos planificadores a la vez en cada zona | Un plan activo por persona y semana; ninguna versión duplicada | OK |
| E12-13 | Cambio de versión durante la generación | S11, U10: se prepara una versión, otro proceso publica otra y luego se persiste la primera | `PlanCambioDuranteLaGeneracionException`; una sola versión activa | OK |
| E12-14 | Pérdida temporal de conexión | U11 se da de alta en S5 con Replicate cortando la conexión | Primer plan publicado por el respaldo; nada se reenvía | OK |
| E12-15 | Varias zonas horarias | Cohorte en Tokio, Madrid y Lima | Cada zona se cierra a su hora local; ninguna sin cierres | OK |
| E12-16 | Retirada a mitad del estudio | U07 se retira en S6 | Sin planes desde S7 (opción A por omisión); sus 5 semanas anteriores se conservan | OK |
| E12-17 | Recuperación de cuenta | U08 en S8: solicita, recibe el código en el buzón simulado y confirma | El JWT anterior da 401; entra con la contraseña nueva; sigue el ciclo | OK |
| E12-18 | Exportación y reconstrucción | Al final, consulta de solo lectura | Linaje (toda v2+ apunta a una versión anterior de la misma semana y persona); CSV por persona y semana | OK |

**Invariantes semanales (todas las semanas, todos los participantes salvo los casos explicados):**
- un plan activo y solo uno;
- una medición por semana;
- **ninguna predicción atribuida a otra persona** (`plan_generation_predictions.user_id = operación.user_id`);
- ningún ejercicio inactivo publicado;
- dolor (E12-05);
- **push**: un aviso por plan semanal publicado, sin envíos duplicados.

**Evidencia** (en `docs/FitSense/cierre_preproduccion/evidencia/`):
- `estudio_12_semanas.csv`: la reconstrucción de la ejecución en verde. Por persona y semana: origen, versiones, adherencia, decisión y estado. Las únicas semanas sin plan son de U07 desde S7 (retirada) y de U11 y U12 antes de su alta.
- `prueba_inversa_sin_recuperacion.txt`: **prueba inversa.** Con `catch-up-hours=0`, es decir, sin la recuperación de B3, el arnés **falla**: todos los participantes de Lima pierden S4 y U09 pierde además S8. El arnés detecta lo que dice detectar. El cambio fue temporal y se deshizo antes del commit.

## 3. Lo que esta simulación **no** demuestra

- **El comportamiento de la IA real.** No hay ningún plan `AI`. La calidad de los planes de gpt-5-mini y su latencia están en la evidencia histórica (`03` de la auditoría).
- **Un reinicio real del proceso** (JVM muerta) durante la adaptación. E12-10 lo simula con una excepción. El reinicio real, con *lease* y libro en el ciclo, es lo que cubre **V66 en #89** (`OperacionesIaV66IT` V59-03b, con un segundo contexto de Spring).
- **La entrega real** de push y de correo.
- **E12-14 no cubre la pérdida de conexión con la base de datos.** Ese caso lo cubre `CicloConcurrenteIT`: la conexión del bloqueo se pierde a mitad del ciclo.
- **Los relojes de vencimiento del código.** Siguen usando `now()` del sistema, y la prueba los controla ampliando `expires_at`. Un `Clock` global sería una mejora (P2).

## 4. Cómo ejecutarla

```bash
mvn -B verify -Pintegration -Dtest=NoExiste -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=Estudio12SemanasIT -Dfailsafe.failIfNoSpecifiedTests=false
```

La tabla queda en `target/estudio_12_semanas/semanas.csv` y en el log (`E12_RESUMEN`).
