# 06 · Limitaciones y pendientes

## Estado

**H2 no está resuelta.** PR-2.0 está **implementada, probada sin coste y desactivada**. Hace falta la evaluación con gpt-5-mini real (`05`) y una autorización para activarla.

## Limitaciones

1. **Sin evidencia de que el modelo obedezca.** Las pruebas demuestran que PR-2.0 dice lo que el validador comprueba, que sus cuotas son factibles y exactas, y que el pipeline reacciona bien. No demuestran que gpt-5-mini cometa menos errores de V15 y V26: eso solo lo dice la evaluación real.
2. **Latencia.** PR-2.0 añade unos 2.500 caracteres de entrada (+1–7,5 %). PR-1.9 añadió 480 y aun así la salida fue más larga y unos 14–19 s más lenta. La comprobación final de PR-2.0 podría alargar el razonamiento. Hay que medirlo (criterio K2).
3. **Potencia estadística.** Con 26 parejas, la etapa 1 solo distingue diferencias grandes (`05` §4).
4. **La factibilidad se comprobó con 26 perfiles.** Son los de la evaluación de H2. Otras combinaciones de equipamiento y restricciones pueden dejar sesiones sin material suficiente. En ese caso PR-2.0 pide expresamente poner menos ejercicios, que V15 y V26 admiten.
5. **`objetivo_series_semana` se lista para todos los grupos con candidatos.** Es solo orientación: el validador no la comprueba.
6. **Fuera del alcance de H2.** Los rechazos V23 (slots), V24 (dosis) y V1 (ejercicio inexistente) no cambian con PR-2.0.

## Pendientes

| # | Pendiente | Quién decide |
|---|---|---|
| 1 | Autorizar un presupuesto nuevo para la evaluación (opción A: USD 7,50; B: 3,20; C: 1,45) | Responsable del estudio |
| 2 | Construir el arnés de la etapa 2 (pipeline completo pareado) y ensayarlo con Replicate simulado antes de gastar | Tras autorizar la etapa 2 |
| 3 | Con resultados favorables: activar `FITSENSE_AI_GENERACION_PROMPT_VERSION=PR-2.0` en el entorno, con autorización expresa | Responsable del estudio |
| 4 | La PR #69 (PR-1.9) puede cerrarse sin fusionar: PR-2.0 incorpora su corrección eficaz (C1, C9). No se ha tocado | Responsable del estudio |
| 5 | Revisar `FITSENSE_AI_BASE_URL` en Railway antes de desplegar (no tengo acceso) | Quien opere Railway |
