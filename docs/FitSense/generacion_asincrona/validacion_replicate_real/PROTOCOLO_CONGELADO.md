# Protocolo congelado: validación de la generación asíncrona con Replicate real

Fecha de congelación: 17/11/2026, antes de la primera llamada pagada. Nada de esta tabla cambia entre G1, G2 y G3.

## 1. Código

| Elemento | Valor |
|---|---|
| Reanudación del 18/11 | `generation` = **`a1ccf53`** (merge de Fitsense-Real/Backend#76, con HR-1; CI verde en los runs 375 y 376). El resto del protocolo, sin cambios. |
| Base | `generation` = `65e1b7e` (V58, V59, latido y lease, fencing, recuperación tras reinicio, publicación única, PD-16, CI verde: ejecuciones 372 y 373) |
| Código ejecutado | rama `validacion/replicate-real` = `d1ac33f` (`65e1b7e` + **una corrección**) encontrada en el ensayo sin coste (`ENSAYO_SIN_COSTO.md` §3, hallazgo HR-1) |
| Justificación del cambio | La orden manda corregir antes de gastar. Sin la corrección, G2 (reinicio controlado) cancelaría en Replicate la predicción pagada y publicaría el respaldo del motor de reglas. |
| Alcance de la corrección | `ReplicateTrainingPlanGenerator.seguir` (no cancela una predicción en curso si el hilo se interrumpe por el apagado), `PlanGenerationOperationService.execute` (un proceso que se apaga no envía ni publica), `GeneracionInterrumpidaException` (nueva). No toca validadores, catálogo, prompts, progresión, reincorporación ni adherencia. |
| Artefacto | `fitsense-backend-0.0.1-SNAPSHOT.jar` construido con `mvnw clean package` sobre ese árbol |

## 2. Modelo y proveedor

| Elemento | Valor |
|---|---|
| Endpoint | `POST https://api.replicate.com/v1/models/openai/gpt-5-structured/predictions` (sin `Prefer: wait` en el camino asíncrono) |
| Variante | `gpt-5-mini` (`fitsense.ai.model`, valor por defecto), razonamiento `medium` |
| Versión servida | `openai/gpt-5-structured` con `latest_version` = `81cb6179c0b9…` (creada el 10/11/2026), según `GET /v1/models/openai/gpt-5-structured`. Replicate devuelve `version = hidden` en las predicciones de modelos oficiales. La variante real se lee en `metrics.model_variant` de cada predicción. |
| Tarifa | USD 0,25 por millón de tokens de entrada y USD 2,00 por millón de salida. `replicate.com` no está en la red permitida, así que no se pudo leer la página oficial. La tarifa coincide con los agregadores consultados el 17/11/2026, incluido el listado de Replicate de uno de ellos, y con la usada en campañas anteriores. |
| Coste de referencia | Última predicción de generación de la cuenta (12/11/2026): 33.985 tokens de entrada y 11.421 de salida, unos USD 0,031. Campañas anteriores: USD 0,014–0,023 por predicción. |

## 3. Prompt y parámetros de la aplicación

| Elemento | Valor |
|---|---|
| Prompt de generación | `PR-1.7` + principios `P-1.3` (`PlanPromptBuilder.VERSION`, `PrescriptionPrinciples.VERSION`) |
| Prompt de adaptación | V2.1 (no interviene: solo se genera el primer plan) |
| Política de planificación | `PLAN-REV-2.0` (configuración vigente) |
| Intentos de IA por generación | 3 (`PlanGenerationPipeline.AI_ATTEMPTS`), después el respaldo del motor de reglas |
| Sondeo | cada 1.000 ms, límite 150 s por predicción (~331 s con cancelación) |
| Comprobación previa de factibilidad | activa |
| Límite por persona | 5 generaciones por hora (en memoria) |
| Generación asíncrona | latido 20 s, lease 90 s, tiempo máximo de la operación 1.800 s, 3 ejecuciones, reutilización 24 h, barrido cada 30 s |
| Interruptores que siguen apagados | H2 (#69 sin fusionar), `fitsense.regeneracion.p03-por-colocacion=false`, políticas experimentales no aprobadas |
| Sin cambios | V15, V24, V8C y el resto de validadores; catálogo; progresión; reincorporación; fórmula de adherencia |

## 4. Entorno

- Una sola instancia del backend (`java -jar`, puerto 8091) en el contenedor de la sesión.
- PostgreSQL 16 de pruebas en Docker (`pg_vr`, base `fitsense_vr`), vacía y migrada por Flyway. La URL de datos se pasa por línea de comandos: nunca Neon ni Railway.
- Usuarios sintéticos `vr-*@pruebas.invalid` creados por la API. No hay datos reales.
- Credencial: el proxy del entorno inyecta la cabecera `Authorization` para `api.replicate.com`. La aplicación recibe un token ficticio y la credencial real nunca está en el proceso ni en los documentos.
- Cuenta: `GET /v1/account` devuelve la cuenta personal (tipo `user`) del responsable del proyecto, la autorizada. El nombre no se transcribe.

## 5. Perfil de prueba (igual en G1, G2 y G3)

GAIN_MUSCLE, INTERMEDIATE, gimnasio, 3 días (lunes, miércoles y viernes), 45 min. Equipo: mancuernas, barra, polea,
máquina, banco y banda. Sin ejercicios bloqueados y sin restricciones. Es el perfil de `CicloCompletoHttpIT`. Cada
escenario usa un usuario nuevo, para que la reutilización de G3 solo pueda venir de su propio libro.

## 6. Escenarios y criterios de aceptación

| Escenario | Procedimiento | Aceptado si |
|---|---|---|
| G1 · normal | `POST /plan/generation` y consulta hasta el estado terminal | COMPLETED con plan `generation_source = AI`. Un respaldo puro no cuenta. Predicciones del libro == predicciones nuevas en Replicate. `GET /plan/current` devuelve el plan. |
| G2 · reinicio | Se espera a que el libro tenga la fila ENVIADA **con su id**. Se apaga con SIGTERM (apagado ordenado) y se arranca otra vez la misma versión. | 0 POST nuevos para la predicción en curso, sin cancelación, 1 plan, operación no perdida, estados coherentes. |
| G3 · vencimiento y reintento | Con la predicción en curso, se fuerza el vencimiento en la base de pruebas (`first_started_at` 1.801 s atrás) y se deja que el barrido actúe. Después se reintenta por la API. | La operación queda FAILED/TIEMPO_EXCEDIDO reintentable y no publica (fencing). La salida tardía queda en el libro. El reintento la **adopta** sin POST nuevo para ese intento. No hay plan duplicado. |

El vencimiento no se fuerza bajando `max-processing-seconds`: `PoliticaTiemposGeneracion` no arranca con menos del
peor caso (~1.113 s), y bajarlo cambiaría las garantías.

## 7. Control del consumo

| Control | Valor |
|---|---|
| Presupuesto autorizado | USD 10 (techo de seguridad, no objetivo) |
| Tope operativo propio | **12 predicciones o USD 1,50 estimados**, lo que llegue antes |
| Peor caso por predicción (reserva) | USD 0,10 (40.000 tokens de entrada + 45.000 de salida) |
| Peor caso por escenario | 3 predicciones = USD 0,30. No se inicia un escenario si el gasto estimado acumulado + 0,30 supera el tope operativo. |
| Contador | `PREDICCIONES_Y_COSTOS.csv`: una fila por predicción, con el id de Replicate, sus tokens y su coste calculado |
| Punto de partida | Última predicción de la cuenta antes de la campaña: 12/11/2026 20:34 UTC. Toda predicción posterior a la campaña tiene que estar en el libro. |
| Conciliación | Tras cada escenario: `GET /v1/predictions` (las nuevas) frente a `plan_generation_predictions` |
| Facturación | No se consulta ni se modifica: la API pública de Replicate no expone gasto, límites ni recargas. **No se ha verificado** que exista un límite de gasto en la cuenta. Por eso hay un tope propio. |
| Reintentos | Solo los de la aplicación (≤ 3 por generación) y un reintento manual en G3. Ninguna repetición de escenarios superados. |

## 8. Condiciones de parada

Se dejan de enviar predicciones ante:
- una duplicación inesperada;
- una predicción sin identificar;
- la pérdida de la traza de gasto;
- el riesgo de un plan inválido;
- el tope operativo superado;
- una operación fuera de control;
- un gasto cercano al máximo;
- un resultado inesperado que haya que investigar.
