# 08 · Resultados de la IA (R y R′)

**PENDIENTE: no ejecutado.** No se ha realizado ninguna llamada pagada a Replicate en esta evaluación: el contador de llamadas de todas las corridas es 0.

**Antes de ejecutar R/R′ se necesita tu autorización para:**

| Elemento | Propuesta |
|---|---|
| SHA congelado | `a31c36bbb0c94eb99c84d8fcb37465797efec459` (`src/main` = `7ee69c2`) |
| Configuración | Límite `preferencia`, retirada `on`, progresión `off`, V8C multiconjunto, PLAN-HIST-1, `MVP-1.8` (`01`) |
| Modelo | `gpt-5-mini` sobre `openai/gpt-5-structured`, esfuerzo `medium`, `prefer-wait=25`, 3 intentos, prompt V1 |
| Predicciones | ≤ 24 previstas por corrida (8 ciclos × 3). **Límite: 30 por corrida** |
| Coste máximo | **USD 2 por corrida** (supuesto de 0,05 por predicción); **R + R′: 60 predicciones / USD 4** |
| G (opcional) | Generación inicial con IA, 20 gemelos, ≤ 60 predicciones / USD 3, como bloque separado |
| Criterio de éxito | `02` §5 (E-1, E-2, E-3), registrado **antes** de ejecutar |
| Resultado de D/D′ | 20/20 PASS; huella idéntica (`05`, `06`) |

**Comando** (exige la autorización expresa en la línea de órdenes):
```
mvnw verify -Pintegration -Dit.test=EvaluacionK20IT -Dfitsense.evaluacion.k20=true \
  -Dfitsense.evaluacion.k20.modo=REPLICATE -Dfitsense.evaluacion.k20.autorizacion-replicate=SI \
  -Dfitsense.evaluacion.k20.credencial=proxy -Dfitsense.evaluacion.k20.max-llamadas=30 \
  -Dfitsense.evaluacion.k20.max-usd=2.0 -Dfitsense.evaluacion.k20.salida=.../evidencia/final/R
```

Con los resultados se completarán aquí, por sesión, las métricas 1–9 del `02` §5, más latencia y coste, el criterio E-1/E-2/E-3 y la comparación EP(R) − EP(D).
