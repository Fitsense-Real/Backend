# Correcciones aplicadas

**Al backend: ninguna.**

La etapa A no demostró ningún defecto de las categorías B, C, D ni E (`09_HALLAZGOS_Y_CAUSAS_RAIZ.md`). Las dos diferencias con el oráculo (U07 y U12) se deben a la entrada, ya que la regla P40 congelada (cada ejercicio al 40 %, redondeado) no da el 40 % exacto con dosis enteras, y a una regla vigente: el umbral estricto del 80 %. La orden prohíbe cambiar umbrales, fórmulas o prescripciones para que coincidan.

**Por tanto:**
- no hay PR de corrección;
- no se ha modificado ningún archivo de `src/main`;
- el código evaluado sigue siendo `generation` `0974fb3` (`backend-mvp-1.0`).

## Cambios del laboratorio (no del backend)

| Versión | Commit | Cambio | Efecto en las entradas |
|---|---|---|---|
| v1 | `911a0d3` | Laboratorio original (A0) | — |
| v2 | `2c0cc8b` | Añade la captura del resultado de la API de adaptaciones (`AdaptationOutcomeQueryService.history`, la misma fuente que `GET /users/me/plan/adaptations`) | Ninguno: solo columnas `api_*` en `decisiones.csv` y `resultados_20.csv` |

## Propuestas que quedan pendientes de tu decisión (no aplicadas)

| # | Propuesta | Por qué no se aplica |
|---|---|---|
| P1 | Definir cómo representar «P40» en una nueva ejecución:<br>• **(a)** el porcentaje real con la regla uniforme (actual: 39,26 y 38,33);<br>• **(b)** un reparto desigual que dé 40,00 en la sesión (existe para U07 y U12);<br>• **(c)** perfiles cuya dosis por 0,4 sea entera.<br>Cualquier cambio exige una nueva ejecución registrada aparte, sin sustituir A0 ni A1 | Es una decisión metodológica del experimento, no del backend |
| P2 | Rótulo propio en la API para `INSUFFICIENT_DATA` (hoy `SIN_ADAPTACION`) y un texto que explique la falta de datos | Es un cambio de contrato, posterior al MVP |
| P3 | Valorar `fitsense.ai.prefer-wait-seconds` de 25–30 s por defecto (hoy 60), para no perder respuestas pagadas detrás de intermediarios que cortan a los 30 s (H9) | Es un cambio de configuración de producción. En el laboratorio solo se aplicó por propiedad (anexo 2) |
| P4 | Revisar el nombre en español del ejercicio 580 («Curl inclinado con mancuerna», clasificado como empuje horizontal de pectorales) y valorar exigir en VARIANTE_MAS_SENCILLA que cada sustituto tenga una dificultad ≤ que la del ejercicio al que sustituye (H10) | Tocar el catálogo y las reglas de validación está prohibido sin tu aprobación |

## Cambios del laboratorio en la etapa C (no del backend)

| Commit | Cambio |
|---|---|
| `46e2366` | Modo de credencial inyectada por el proxy (anexo 1) |
| `c63bad2` | Corrección del espía tras el error del intento 1 (`ClassCastException`): ahora el laboratorio hace todo su trabajo antes de contar y llamar |
| `d289dce` | Opciones `prefer-wait` y `max-llamadas` (anexo 2) |

**En la etapa C tampoco se corrigió ningún defecto del backend: no se demostró ninguno.**
