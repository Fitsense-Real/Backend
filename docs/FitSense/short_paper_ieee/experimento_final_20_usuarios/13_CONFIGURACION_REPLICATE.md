# Configuración de Replicate (etapa C)

## Estado: **bloqueada, sin ninguna llamada**

| Comprobación previa (orden §10.3) | Resultado |
|---|---|
| Token de Replicate válido | **No disponible.** La sesión de trabajo no tiene la variable `REPLICATE_API_TOKEN`; no se buscó en otros sitios ni se pidió por chat |
| Modelo configurado | `openai/gpt-5-structured` (`https://api.replicate.com/v1/models/openai/gpt-5-structured/predictions`), modelo `gpt-5-mini`, esfuerzo `medium` (`application.properties`, prefijo `fitsense.ai`) |
| Versión del modelo | La fija Replicate en cada predicción (se registra en la traza al llamar). **No consultable sin token** |
| Coste aplicable | **No consultable sin token.** La campaña MVP anterior observó unos US$ 0,87 en 45 llamadas (unos US$ 0,02 por llamada), estimado por tokens; la cifra oficial está en el panel de Replicate |
| Límite de consumo | **USD 10 o 90 solicitudes**, lo que llegue antes. El laboratorio cuenta cada predicción **antes** de hacerla y supone un máximo de US$ 0,10 por llamada (90 × 0,10 = 9). Al llegar al límite no llama: lanza `AiProviderUnavailableException("PRESUPUESTO_AGOTADO…")` y lo registra |
| Intentos por operación | 3, que es la política implementada (`AI_ATTEMPTS = 3` en la generación inicial y en la adaptación) |
| Reintentos del laboratorio | Ninguno: el laboratorio no reintenta nada |
| Disyuntor | 3 fallos seguidos del proveedor lo abren durante 10 minutos (política del backend) |
| Protección de la base | Antes de la primera llamada, el laboratorio exige que la URL sea `jdbc:postgresql://localhost` o `127.0.0.1` y que el contexto use esa misma base (Testcontainers). **El perfil `local-dev` y su guardián no se han tocado**: la etapa C no usa ese perfil |
| Token en la evidencia | Nunca. La traza solo guarda huellas (SHA-256 truncado) del contexto, la salida del modelo y los tiempos |

## Diseño previsto (`ExperimentoKora20IT`, modo `REPLICATE`)

**1. Adaptación con la misma entrada que la etapa A** (orden §11.3)
- U01–U20 se crean primero en una base vacía, con la generación por IA desactivada solo durante su plan inicial. Tienen así los mismos `userId`, semillas y planes iniciales que A0/A1, lo que se comprobará por la huella de `plan_inicial.csv`.
- Después se registran los mismos patrones y el ciclo semanal llama a la IA **solo cuando la política lo pide**, es decir, solo en E3 y E4.
- Máximo: 8 usuarios × 3 = 24 llamadas.

**2. Generación inicial con IA** (orden §11.1)
- 20 gemelos (`U01G`–`U20G`) con los mismos perfiles, pero con otros `userId`.
- Hasta 3 intentos con IA por usuario y después el motor de reglas.
- Máximo: 60 llamadas.

**Total máximo: 84 llamadas**, por debajo del límite de 90.

## Cómo desbloquearla

1. En la configuración del entorno de Claude Code, añade `REPLICATE_API_TOKEN` como secreto o variable de entorno. Lo recoge **una sesión nueva**.
2. Autoriza la ejecución con el límite indicado. Ya está autorizada en la orden, pero la sesión nueva debe confirmar el token.
3. Ejecutar:
```
mvn -o verify -Pintegration -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false \
  -Dfailsafe.failIfNoSpecifiedTests=false -Dit.test=ExperimentoKora20IT \
  -Dfitsense.experimento.kora20=true -Dfitsense.experimento.kora20.modo=REPLICATE \
  -Dfitsense.experimento.kora20.salida=docs/FitSense/short_paper_ieee/experimento_final_20_usuarios/evidencia/C \
  -Dfitsense.experimento.kora20.commit=$(git rev-parse HEAD)
```

Sin token, este modo **falla antes de llamar** («BLOQUEADO: sin REPLICATE_API_TOKEN…»). Está comprobado y registrado en `evidencia/C_bloqueo`.
