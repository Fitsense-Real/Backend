# Protocolo comparativo C1 frente a C2 (congelado antes de cualquier llamada)

**Se congela** con el *commit* que añade este documento, **antes de la primera predicción** del experimento. Nada de lo que sigue se cambia después de ver resultados. Cualquier desviación se anota como anexo fechado, sin reescribir este texto.

## 1. Pregunta e hipótesis (exploratorias)

**Pregunta:** ¿el contexto de adaptación con factibilidad explícita (C2) aumenta las adaptaciones aplicadas sin perder seguridad, frente al prompt vigente (C1)?

**Hipótesis:**
- **H1.** C2 aplica más adaptaciones de las 8 elegibles que C1.
- **H2.** C2 tiene menos rechazos por V20.
- **H3.** C2 omite directamente las sesiones sin solución (U03 S15 y U04 S21).
- **H4.** C2 mantiene en 0 todas las métricas de seguridad.

**Carácter:** n = 8 casos elegibles por corrida, con 2 corridas por condición. **La evidencia es exploratoria.** No se hacen pruebas de significación, ni se atribuyen efectos conductuales o clínicos.

## 2. Condiciones

| | C1 | C2 |
|---|---|---|
| Prompt | `FIX-PR-1.3`: el vigente, idéntico byte a byte al de la etapa C (`06` §3) | `FIX-PR-2.0-EXP` (`05`) |
| Indicador | `fitsense.ai.adaptation.prompt-version=v1` | `=v2` |
| Naturaleza | Referencia | **Intervención combinada:** prompt mejorado + factibilidad precalculada (`04`) |
| Código | El mismo *commit* para las 4 corridas | ← |
| Modelo | `gpt-5-mini` vía Replicate (`openai/gpt-5-structured`), esfuerzo `medium`, salida estructurada `FIX-1.0` | ← |
| Intentos | Máximo 3 por operación (`AI_ATTEMPTS`), sin reintentos extra del laboratorio | ← |
| Transporte | `prefer-wait-seconds=25` (anexo 2 de PR #28), `timeout-seconds=150` | ← |
| Validadores | V8C + V1–V25 sin cambios | ← |
| Política | PLAN-HIST-1, progresión desactivada (como en la etapa C) | ← |

## 3. Población y entradas

**Perfiles:** U01–U20, los 20 perfiles congelados de `experimento_final_20_usuarios/03_PERFILES_U01_U20.csv`, leídos del mismo archivo, sin copiarlo.
- 8 elegibles: U03, U04, U08, U09, U13, U14, U18 y U19.
- 12 controles no elegibles, que no deben llamar a la IA.

**Mismas entradas en cada corrida:**
- semana medida 02/11/2026 y siguiente 09/11/2026;
- mismos patrones de ejecución;
- mismas semillas.

**Plan inicial:** el del motor de reglas, con la generación con IA desactivada. Ya se comprobó que es idéntico al de la etapa C.

**Sin generación inicial con IA:** no hay gemelos (`gemelos=false`).

**Base de datos:** PostgreSQL efímera nueva en cada corrida (Testcontainers, `localhost`). El laboratorio aborta si la URL no es local. **Ni Neon ni Railway.**

## 4. Corridas, en este orden

| # | Corrida | Carpeta |
|---|---|---|
| 1 | C1 r1 | `evidencia/C1_r1` |
| 2 | C2 r1 | `evidencia/C2_r1` |
| 3 | C2 r2 | `evidencia/C2_r2` |
| 4 | C1 r2 | `evidencia/C1_r2` |

**Normas:**
- **No se repite ninguna corrida por su resultado.**
- **No se cambia el prompt ni el código** entre corridas.
- **Fallo técnico:** una corrida interrumpida por un fallo técnico ajeno al resultado (caída del proxy o del laboratorio) se conserva tal cual en `evidencia/<corrida>_fallo_tecnico`. Se puede repetir **una sola vez**, registrada, si el presupuesto lo permite. Si no se puede, se informa con las corridas disponibles.

## 5. Presupuesto y guardas

**Total del experimento:** como máximo **USD 5 estimados y 120 predicciones**, lo que llegue antes.

**Por corrida:** `max-llamadas=30`, `coste-por-llamada=0,04` (supuesto conservador) y `max-usd=1,20`.
- 4 × 30 = 120 predicciones.
- 4 × 1,20 = 4,80 USD.

**Coste observado en la etapa C:** máximo US$ 0,025 por llamada (57.671 *tokens* de entrada y 5.092 de salida). C2 añade unos 1.200 *tokens* como máximo.

**Necesidad prevista:** 8 casos × 3 intentos = 24 predicciones por corrida, como mucho.

**Cómo se cuenta:**
- toda predicción iniciada cuenta, aunque su respuesta se pierda;
- al llegar al límite, el laboratorio deja de llamar y lo registra (`PRESUPUESTO_AGOTADO_SIN_LLAMADA`);
- **el presupuesto no se amplía.**

**El coste real** se calcula después con las métricas de cada predicción (`token_input_count` y `token_output_count`) y el precio público de GPT-5 mini: US$ 0,25 por millón de *tokens* de entrada y US$ 2,00 por millón de salida.

**Verificaciones antes de cada corrida** (si alguna falla, no se ejecuta):
1. **Autenticación:** `GET /v1/account` = 200 (gratuito; el proxy inyecta la credencial). La credencial nunca se ve, se imprime ni se guarda.
2. **Proveedor, modelo y configuración:** `00_configuracion.txt` debe decir `gpt-5-mini`, la versión de prompt correcta, `prefer_wait` 25 y sin gemelos.
3. **Base local efímera:** guarda del laboratorio.
4. **Carpeta de salida vacía:** el laboratorio no sobrescribe.

## 6. Trazas por corrida

| Archivo | Contenido |
|---|---|
| `00_configuracion.txt` | *Commit*, versión de prompt, modelo, límites, base y migraciones |
| `trazas_ia.jsonl` | Por llamada: etiqueta, intento, versión y tamaño del prompt, huella de entrada, respuesta, resultado y tiempo |
| `prompts/` | Prompt exacto de cada intento |
| `ejecucion_maven.log` | `REPLICATE_TIMING` (id de predicción, tiempos) y el texto literal de cada rechazo del validador |
| `predicciones_replicate/` | Por predicción, descargada después (gratis): entrada, salida, estado y métricas (*tokens* y `predict_time`) |
| `decisiones.csv` | V32: estado, sesiones aplicadas, sin efecto y conservadas, llamadas |
| `plan_siguiente.csv` | Cambio efectivo sesión a sesión |
| `resultados_20.csv` | Resumen por usuario |

## 7. Métricas (definidas antes de ver datos)

**Primaria:** adaptaciones aplicadas (V32 = APLICADA) / 8, por corrida.

**Secundarias:**
- sesiones señaladas con cambio efectivo (de 14);
- propuestas rechazadas y su motivo;
- SIN_EFECTO;
- llamadas por caso;
- omisión directa de las 2 sesiones F3 (U03 S15 y U04 S21);
- latencia;
- *tokens* y coste.

**Seguridad (todas deben ser 0):**
1. planes inválidos publicados (todo plan publicado ha pasado los validadores; se comprueba además que ninguna sesión publicada baje del mínimo V20 ni suba la dosis);
2. cambios en sesiones no señaladas;
3. dosis no autorizada (cualquier subida respecto a lo prescrito);
4. reincorporaciones no permitidas;
5. ejercicios excluidos;
6. violaciones de traza o linaje V31/V32.

**Controles:** los 12 no elegibles con 0 llamadas y el mismo resultado que en la etapa C.

## 8. Clasificación del resultado (fijada antes)

Sean a1 y a2 las aplicadas en C1 r1 y C1 r2, y b1 y b2 las de C2 r1 y C2 r2.

| Resultado | Condición |
|---|---|
| **Desfavorable** | Alguna violación de seguridad en C2, o media(b) < media(a) |
| **Favorable** | 0 violaciones, min(b) > max(a) y media(b) − media(a) ≥ 2 |
| **Parcial** | 0 violaciones, media(b) − media(a) ≥ 1, sin llegar a Favorable |
| **No concluyente** | 0 violaciones y diferencia de medias < 1 |

**Máximo teórico:** 7 de 8 por corrida, porque U03 no tiene ninguna modificación factible (`03`).

## 9. Lo que no se hace

- No se modifica V20, V24, V8C ni ningún validador.
- No se reduce el mínimo de duración.
- No se cambian las políticas, el catálogo, las dosis ni la dificultad.
- No se activa C2-D/EF.
- No se fusiona PR #17, PR #28 ni el PR nuevo.
- Ni Railway ni Neon.
- No se toca Flutter.
- No se modifica el short paper ni el experimento original.
- No se repiten resultados de forma selectiva.
- No se amplía el presupuesto.

---

## Anexo 1 (03/11/2026, posterior a las corridas): corrección de un conteo, no del método

**§7** dice «sesiones señaladas con cambio efectivo (de 14)». Es un error de conteo al redactar. Los 8 casos elegibles tienen **12** sesiones señaladas: U03 1, U04 2, U08 1, U09 2, U13 1, U14 2, U18 1 y U19 2. De ellas, **10 son factibles**, porque U03 S15 y U04 S21 son F3 (`03`).

Los resultados se informan sobre esas 12 y 10, en `15` y `17`. **Ninguna métrica, umbral ni regla de clasificación cambia.**

## Anexo 2 (03/11/2026): ejecución

- Las 4 corridas se ejecutaron en el orden congelado (C1 r1, C2 r1, C2 r2, C1 r2), una vez cada una.
- No hubo fallos técnicos ni repeticiones.
- No se alcanzó ninguna guarda de presupuesto: 46 predicciones y US$ 0,69 en total.
- **Herramienta:** el script de descarga de predicciones se corrigió antes de la primera llamada para enviar un nombre de cliente genérico, porque Replicate devolvía 403 al de `urllib`. No afecta al código evaluado ni al protocolo.
