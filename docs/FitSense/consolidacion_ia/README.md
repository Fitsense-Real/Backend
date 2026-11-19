# Consolidación de los experimentos de IA de FitSense

Documentación técnica interna de los experimentos de **generación inicial** y **adaptación de planes** con IA, entre el 06/11/2026 y el 10/11/2026.

| Elemento | Valor |
|---|---|
| Código de referencia | `generation` `543932a` (PR #43 y #44 fusionadas) para los experimentos. La configuración evaluada se activó por defecto con la PR #45 (merge `13b75f2`, 11/11/2026; ver `00` §5) |
| Rama de esta documentación | `docs/consolidacion-ia`, creada desde `generation`. Solo documentación |
| Uso | Interno. No es material para el short paper, el full paper ni la tesis |
| Qué no se hizo | Llamadas a Replicate; gasto; experimentos nuevos; cambios de validadores, prompts, reglas, catálogo, políticas o resultados históricos; cambios en Neon o en mobileapp; despliegue. El único cambio de código asociado es la configuración por defecto (PR #45) |

## Documentos

| Archivo | Contenido |
|---|---|
| [`00_RESUMEN_EJECUTIVO.md`](00_RESUMEN_EJECUTIVO.md) | Qué se probó, conclusiones, decisiones y lo que los resultados no dicen |
| [`01_INVENTARIO_EVIDENCIAS.md`](01_INVENTARIO_EVIDENCIAS.md) | Las seis campañas: fecha, objetivo, rama y commit, versión del modelo, prompts, perfiles, predicciones, coste, incidencias, evidencia e integridad |
| [`02_GENERACION_INICIAL.md`](02_GENERACION_INICIAL.md) | Flujo perfil → catálogo → contexto → Replicate → V1–V27 → plan o respaldo; los cuatro objetivos; propuestas originales frente a planes aceptados |
| [`03_ADAPTACION_PLANES.md`](03_ADAPTACION_PLANES.md) | Flujo de adaptación, escenarios, permisos, aplicadas, SIN_EFECTO, sustituciones, reducciones y pertinencia |
| [`04_LINEA_BASE_SIN_REGLAS.md`](04_LINEA_BASE_SIN_REGLAS.md) | BASE-R1/R2: qué se quitó y qué se mantuvo; crudo frente a normalizado; comparación con GRA |
| [`05_EXPERIMENTOS_MEJORA_PROMPTS.md`](05_EXPERIMENTOS_MEJORA_PROMPTS.md) | Cronología de prompts (PR-1.7, PR-1.8, pr-1.8a/b/c; V2.1, V2.2, v2.2a, v2.4) por versión del modelo, y por qué se mantienen PR-1.7 y V2.1 |
| [`06_CORRECCIONES_V8C.md`](06_CORRECCIONES_V8C.md) | H-A1 y H-A2, pruebas de fecha (PR #43), regresión y CI |
| [`07_PROGRESION_REINCORPORACION.md`](07_PROGRESION_REINCORPORACION.md) | Reglas vigentes y su relación con la generación y la adaptación |
| [`08_DECISIONES_Y_LIMITACIONES.md`](08_DECISIONES_Y_LIMITACIONES.md) | Decisiones, tipos de afirmación, limitaciones y pendientes |
| [`09_TRAZABILIDAD.md`](09_TRAZABILIDAD.md) | Cronología de commits, matriz cifra → fuente y comprobaciones de integridad |

## Cómo leerla

- **Orden recomendado:** primero `00`, después `01`, y luego el tema que interese.
- **Figuras:** son exactas y llevan su denominador. Los planes válidos a la primera se cuentan sobre los **19 perfiles factibles** (O01–O20 sin O11), salvo que se diga otra cosa.
- **Tres tipos de afirmación** (`08` §2):
  - resultados técnicos;
  - decisiones experimentales;
  - observaciones pendientes de validación profesional.
- **Evidencia pesada:** los ZIP y tar no se copian aquí. Se citan por rama, commit y ruta (`01` §1).
