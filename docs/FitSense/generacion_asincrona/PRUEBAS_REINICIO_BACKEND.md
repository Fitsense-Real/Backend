# Pruebas de reinicio del backend (V59)

El reinicio se simula como lo ve la base tras la muerte del proceso:

- el lease vence (`lease_expires_at` en el pasado), sin apagado ordenado; o bien
- el lease lo tiene otro proceso (`lease_owner` distinto), como cuando una instancia nueva ya reclamó la operación.

Después, el barrido (`recoverStale`) la reencola y otro reclamo la ejecuta.

## Pipeline real con un Replicate simulado (`ReanudacionConReplicateSimuladoIT`)

El falso cuenta los POST y devuelve una salida que los validadores rechazan. Así el pipeline hace sus 3 intentos y
publica el respaldo determinista validado. Para «morir antes de publicar», el lease se le quita a la operación justo
antes de `persist`: el worker ya no es su propietario y no publica.

| Prueba | Secuencia | POST | Plan | Ejecuciones | Libro |
|---|---|---|---|---|---|
| `caminoNormal` | reclamar y ejecutar | 3 | 1 | 1 | — |
| `reanudacionNoPagaOtraVez` | ejecutar (3 POST), morir antes de publicar, reiniciar, ejecutar | **3** | 1 | 2 | iniciadas 3, completadas 3, reutilizadas 0 |
| `dosReiniciosSiguenSiendoTres` | morir dos veces y luego completar | **3** | 1 | 3 | — |
| `reintentoManualAdopta` | la primera operación se cierra sin plan; la nueva la adopta | **3** | 1 | — | segunda: iniciadas 0, reutilizadas 3 |
| `aceptadaSinIdNoSeReenvia` | Replicate acepta y corta sin responder, se publica el respaldo, se reinicia y se reanuda | **1** | 1 | 2 | no confirmadas 1, ningún id guardado |

Extracto del log de la ejecución local:

```
GENERACION_TIEMPOS user=2 semana=1 origen=RESPALDO motivo_respaldo=RECHAZOS intentos_ia=3   (x3 ejecuciones)
GENERACION_ASINCRONA_PREDICCIONES ... iniciadas=3 reutilizadas=0 completadas=3 ...          (las 3 ejecuciones)
REPLICATE_REUTILIZADA   x12   (3 por reanudación)
REPLICATE_ADOPTADA      x3    (reintento manual)
REPLICATE_NO_CONFIRMADA x1, REPLICATE_REPETIDA x1, motivo_respaldo=PROVEEDOR intentos_ia=1   (E17)
```

## Estados de la operación (`GeneracionAsincronaResilienciaIT`)

- **`reinicioRecuperaYCompleta`.** Al reclamar se fija el lease. Tras vencer, pasa a `PENDING` sin propietario. Se
  vuelve a reclamar con este proceso como propietario y termina en `COMPLETED` con 1 plan.
- **`interrupcionesRepetidasTerminan`.** Tres interrupciones dejan `FAILED / EJECUCION_INTERRUMPIDA`,
  reintentable, con `executions = 3`. Nunca queda colgada.
- **`planYaPublicadoAntesDeCerrar`.** El proceso publicó y murió antes de cerrar. La reanudación encuentra el plan
  (`PlanAlreadyExistsException`) y cierra `COMPLETED` con ese plan.
- **`apagadoSoloLasPropias`.** Un SIGTERM devuelve a la cola solo las operaciones con su `lease_owner`, no las de
  otra instancia.
- **`elLimiteNoSeReiniciaAlReencolar`.** Una reanudación no amplía el tiempo máximo.

## Contra una base PostgreSQL vacía en local (`GeneracionAsincronaIT`, opt-in)

```
docker run -d --name pg_v59 -e POSTGRES_PASSWORD=x -e POSTGRES_DB=fitsense_v59 -p 5433:5432 postgres:16-alpine
./mvnw -Pintegration verify -Dit.test=GeneracionAsincronaIT \
  -Dfitsense.pruebas.url=jdbc:postgresql://localhost:5433/fitsense_v59 \
  -Dfitsense.pruebas.usuario=postgres -Dfitsense.pruebas.clave=x
Tests run: 15, Failures: 0, Errors: 0
```

Cambios en la prueba de #71, todos por la nueva semántica y no para ocultar un fallo:

1. **`envejecer`** también vence el lease. Desde V59 solo un lease vencido indica un proceso muerto, que es justo el
   defecto D2 corregido.
2. **La prueba de interrupción** necesita 3 ejecuciones, porque `max-executions` pasa de 2 a 3.
3. **El tiempo máximo** se simula con 31 min desde `first_started_at`, porque pasa de 600 s a 1.800 s.
4. **La limpieza entre pruebas** suelta el lease, que `ck_pgo_lease` exige al salir de `PROCESSING`.

## Lo que no se simula

- **Un SIGKILL real de la JVM en mitad de un POST.** El efecto en la base es el mismo: una fila en `SOLICITANDO`,
  que cubre el caso E17 unitario.
- **Un reinicio real del contenedor en Railway.** Ver `COMPATIBILIDAD_FLUTTER_KAPI.md` y `RIESGOS_RESIDUALES.md`.
