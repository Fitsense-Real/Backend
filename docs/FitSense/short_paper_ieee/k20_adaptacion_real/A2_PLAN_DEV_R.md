# A2 · Plan de DEV-R: diagnóstico con Replicate real, V2 frente a V2.1 en U01–U20

**Estado: PREPARADO, NO EJECUTADO.**
- «Todavía no autorizo realizar llamadas pagadas.»
- DEV-R es **desarrollo**, no evaluación: su resultado sirve para elegir el prompt antes de congelar y **no** se presenta como resultado científico.
- **No se ejecuta nada sobre J01–J16.**

## 1. Versión sobre la que se ejecutaría

| | |
|---|---|
| Rama | `eval/final-ieee-k20-v2`, la candidata con 4D v3.4 integrada |
| SHA | El HEAD con CI verde en [#41](https://github.com/Fitsense-Real/Backend/pull/41) en el momento de autorizar. Se anota en `configuracion.json` de cada corrida |
| Cohorte | U01–U20 (perfiles históricos, SHA `584e2204…`). Sus perfiles GM son de 60 min, así que 4D v3.4 no cambia su recorrido: la huella D es idéntica (`11` §4D v3.4) |
| Modelo | `gpt-5-mini` sobre `openai/gpt-5-structured`, esfuerzo `medium`, hasta 3 intentos por ciclo, `prefer-wait=25` |
| Prompts | Corrida DEV-R-V2 con `-Dfitsense.evaluacion.k20.prompt=v2` y corrida DEV-R-V2.1 con `-Dfitsense.evaluacion.k20.prompt=v2.1`. Mismo SHA, misma semilla, mismo orden de usuarios |

## 2. Coste (confirmado sobre el mecanismo; la tarifa, sin verificar en línea)

| | Por corrida | DEV-R (2 corridas) |
|---|---:|---:|
| Ciclos de adaptación con IA en U | 8 | 16 |
| Predicciones máximas (3 intentos por ciclo) | **24** | **48** |
| Tope duro en USD (24 × 0,05) | **1,20** | **2,40** |
| Gasto esperado (≈ 1 intento por ciclo con V2, a 0,013–0,027 por predicción) | ≈ 0,10–0,25 | ≈ 0,20–0,50 |

**Qué está confirmado y qué no:**
- **Confirmado en el código** (`EvaluacionK20IT.proveedor`):
  - cada predicción se cuenta **antes** de hacerse;
  - si `n > max-llamadas` o `n × 0,05 > max-usd`, no se llama y se registra `PRESUPUESTO_AGOTADO_SIN_LLAMADA`;
  - sin `autorizacion-replicate=SI` la prueba se detiene antes de cualquier llamada;
  - antes de la primera predicción se comprueba la credencial (`GET /v1/account` = 200), que es gratuita.
- **Supuesto, no verificado hoy:**
  - el coste máximo de 0,05 USD por predicción;
  - la tarifa pública de GPT-5 mini que usaron las campañas anteriores (0,25 / 2,00 USD por millón de tokens), con un coste observado de 0,013 a 0,027 USD por predicción.
  - Desde esta sesión `replicate.com` no está permitido en la red y no hay credencial, así que no he podido consultarla.
- **La cifra oficial es la del panel de Replicate** después de ejecutar. El arnés no registra tokens; registra la latencia de cada predicción (`ms`) y el tamaño del prompt.

## 3. Comandos (no ejecutar sin autorización expresa)

```
SHA=$(git rev-parse HEAD)
for P in v2 v2.1; do
  sh ./mvnw -o -B -ntp verify -Pintegration -DskipUTs=true -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false \
    -Dit.test=EvaluacionK20IT -Dfitsense.evaluacion.k20=true -Dfitsense.evaluacion.k20.modo=REPLICATE \
    -Dfitsense.evaluacion.k20.autorizacion-replicate=SI \
    -Dfitsense.evaluacion.k20.prompt=$P \
    -Dfitsense.evaluacion.k20.max-llamadas=24 -Dfitsense.evaluacion.k20.max-usd=1.20 \
    -Dfitsense.evaluacion.k20.salida=docs/FitSense/short_paper_ieee/k20_adaptacion_real/evidencia/desarrollo/DEV_R_$P \
    -Dfitsense.evaluacion.k20.commit=$SHA
  python3 -I docs/FitSense/short_paper_ieee/k20_adaptacion_real/herramientas/oraculo_k20.py \
    docs/FitSense/short_paper_ieee/k20_adaptacion_real/evidencia/desarrollo/DEV_R_$P \
    docs/FitSense/short_paper_ieee/evaluacion_final_k20/entrada/03_PERFILES_U01_U20.csv \
    docs/FitSense/short_paper_ieee/evaluacion_final_k20/entrada/04_ORACULO_TEORICO_HISTORICO.csv
done
```

**Requisitos del entorno:**
- `REPLICATE_API_TOKEN` en el entorno (no se imprime ni se guarda);
- `replicate.com` / `api.replicate.com` permitidos en la red de la sesión.

**Reglas de ejecución:**
- Las corridas se ejecutan una vez cada una.
- Una corrida que falle por causas técnicas se conserva y se repite completa **una sola vez**, con registro.

## 4. Qué se evaluará (por sesión señalada y por corrida, V2 frente a V2.1)

| Dimensión | Fuente | Medida |
|---|---|---|
| **Pertinencia a la causa** | Oráculo v2 | ACC / ALT / INC / CONS-J / CONS-POL por causa (fatiga, demasiado difícil, desmotivación, dolor). ACC y ALT se informan **por separado**: una alternativa lateral no se presenta como una simplificación demostrada |
| **Cambios reales de la IA** | `tablas/`, origen de la sesión | Sesiones con origen IA, validadas, con cambio real y publicadas, sobre las **factibles** de la sonda (10 en U). U03 S3 y U04 S3 son CONS-J y no cuentan como éxito |
| **Respeto de restricciones** | Oráculo (S1–S7) | 0 aumentos de dosis o dificultad, 0 sesiones no señaladas alteradas, 0 planes inválidos publicados |
| **Rechazos y reintentos** | `trazas_ia`, `intentos` | Rechazos por validador y por contrato; intentos por ciclo; respaldos |
| **Sustituciones del mismo músculo** | Oráculo v2 (regla del músculo objetivo) | Proporción de sustituciones con el mismo músculo objetivo; las que lo cambian son como mucho ALT |
| **Dificultad** | Catálogo y oráculo | Diferencia de dificultad de cada sustituto. Ante «demasiado difícil», ninguno más difícil |
| **Cambio mínimo ante desmotivación** | Oráculo (B-4) | Ejercicios cambiados por sesión; más de 2 = INC |
| **Coste y latencia** | `trazas_ia` (`ms`) y panel de Replicate | Predicciones usadas, latencia mediana y p90, USD del panel |

**Regla de decisión** (propuesta; queda fijada antes de ejecutar):
1. Se elige V2.1 solo si **no empeora** en seguridad (0 en ambas) ni en cambios reales factibles.
2. Además debe **mejorar** la pertinencia (más ACC, menos INC) o el cambio mínimo.
3. Si empatan, se queda **V2**, que es el cambio menor.
4. No se elige V2.1 automáticamente: la elección final es tuya.

## 5. Lo que sigue después de DEV-R (también sin autorizar)

1. Congelar SHA, prompt, oráculo v2, criterios y cohortes U y J (`07`).
2. D y D′ sobre U y J.
3. R y R′ con Replicate sobre U01–U20 + J01–J16: 72 + 72 = 144 predicciones, USD 7,20 de tope.
