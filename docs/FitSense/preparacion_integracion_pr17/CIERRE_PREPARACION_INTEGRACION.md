# Cierre de la preparación de integración

## Condición de cierre

| Resultado | Estado |
|---|---|
| **1.** Rama aislada de versionado + V4-C con PR borrador y pruebas completas | **Cumplido:** `feature/v4c-versionado-aislado`, PR borrador Fitsense-Real/Backend#18 hacia `generation`, CI verde, **sin fusionar** |
| **2.** PR #17 en borrador, con matrices verificadas, evidencias corregidas, especificación Flutter y material profesional listo | **Cumplido:** PR Fitsense-Real/Backend#17 sigue en borrador; 6.720 comparaciones perfil a perfil sin diferencias; fichas corregidas; paquete del evaluador separado |

## Entregables (`docs/FitSense/preparacion_integracion_pr17/`)

| # | Archivo |
|---|---|
| 1 | `CORRECCION_FICHAS_PROFESIONALES.md` |
| 2 | `VALIDACION_MUESTRA_PROFESIONAL.md` |
| 3 | `COMPARACION_960_PERFILES.md` |
| 4 | `DIFERENCIAS_MATRICES.csv` |
| 5 | `SEPARACION_V4C.md` |
| 6 | `DEPENDENCIAS_VERSIONADO_V44.md` |
| 7 | `PRUEBAS_V4C_AISLADO.md` |
| 8 | `ESPECIFICACION_FLUTTER.md` |
| 9 | `CONTRATOS_API_REVISADOS.md` |
| 10 | `REGRESION_PREINTEGRACION.md` |
| 11 | `DECISIONES_PARA_APROBACION.md` |
| 12 | `CIERRE_PREPARACION_INTEGRACION.md` |

**Además:** `revision_profesional/paquete_evaluador/` (4 archivos).

**Evidencias anteriores:** **no se ha sobrescrito ninguna**, salvo la corrección autorizada de `cierre_final_pr17/FICHAS_NO_FACTIBLE.csv`.

## Las 20 preguntas

**1. ¿Se corrigió `FICHAS_NO_FACTIBLE.csv`?**
- Sí. D01 y D02 tenían 11 columnas porque un campo con coma iba sin comillas.
- Ahora tiene 10/10/10 columnas con escapado RFC 4180.
- El generador del laboratorio está corregido.
- `PaqueteEvaluadorTest` falla con el archivo anterior y pasa con el nuevo.

**2. ¿Las 36 fichas son coherentes y ciegas?**
- Sí: 34 planes y 2 fichas, 36 códigos únicos, iguales en la muestra, la plantilla y la clave, sin duplicados ni huérfanos.
- 681 filas de ejercicio completas.
- El paquete del evaluador no contiene la clave ni ningún término revelador.
- La plantilla está vacía: no hay evaluaciones registradas.

**3. ¿Se compararon los 960 perfiles individualmente?**
- Sí, plan a plan, en 13 campos:
  - `generation` frente al PR #17 (HIST);
  - 661c4aa frente al PR #17 (5 políticas);
  - `generation` frente a la rama aislada (HIST).
- En total, 6.720 combinaciones.
- Con un control positivo, que confirma que el comparador detecta diferencias.

**4. ¿Hay diferencias sin explicar?**
- **No.** Hay 0 diferencias.
- Los volcados de 661c4aa y del PR #17 son idénticos byte a byte.

**5. ¿Se creó la rama V4-C?**
- Sí: `feature/v4c-versionado-aislado`, desde `generation` (afef98a).
- Commits 6aec7e0 y d1de25c, publicados.

**6. ¿V44 es idéntica?**
- Sí, byte a byte (SHA-256 `640b0169…e2708400`).
- No existe una segunda V44. V45 sigue solo en el PR #17.

**7. ¿Qué se excluyó de la rama V4-C?**
- C2-D;
- EF-0.1;
- V45;
- LW-PREF-0.2;
- REP-SUST-0.1;
- NOFACT-0.1;
- la API de duración y la métrica aeróbica;
- los laboratorios del PR #17.

**8. ¿La rama aislada pasa sus pruebas?**
- **Sí:**
  - 995 unitarias;
  - integración: 462 contabilizadas, 429 ejecutadas, 33 omitidas;
  - batería PLAN-V4-0.1: 223/225 con 2 omitidas;
  - CI de GitHub verde.
- **0 fallos.**

**9. ¿Se abrió el PR borrador?**
- Sí: Fitsense-Real/Backend#18, en borrador, hacia `generation`.
- **No fusionado.**

**10. ¿Cómo se evita duplicar V44?**
- V44 es el mismo archivo con el mismo contenido en las dos ramas.
- Tras fusionar el PR #18, el PR #17 se actualiza con *merge* de `generation`.
- Simulado:
  - V44 entra sin conflicto y una sola vez;
  - 11 conflictos, que se resuelven conservando la versión del PR #17;
  - `src/main` queda idéntico al PR #17.
- Se detectó y corrigió un método que git habría duplicado en silencio.
- Prohibido crear V44b o renumerar V45.

**11. ¿Es posible el motivo neutral de duración?**
- Sí, y está hecho en el PR #17:
  - `DURACION_POR_CONTENIDO`, sin consumidores;
  - el mensaje exacto de la orden, con tildes;
  - prueba unitaria de texto y de palabras prohibidas;
  - prueba HTTP con el indicador apagado y encendido.

**12. ¿La especificación de Flutter cubre NO_FACTIBLE y la duración?**
- Sí:
  - casos A–E con modelos Dart, JSON, estados de la interfaz y 12 pruebas;
  - sin plan parcial, sin compras ni superficies supuestas;
  - el punto de anclaje diferenciado.
- Flutter **no** se ha modificado.

**13. ¿Se conciliaron las listas de equipamiento?**
- **Propuesto, no aplicado.** Hoy `/limitations` tiene 3 códigos y NOFACT-0.1 tiene 6.
- La propuesta es una lista única por niveles, según la elegibilidad real medida en los 48 perfiles:
  - nivel 1: banda y mancuernas;
  - nivel 2: barra, espaldera y correas;
  - nivel 3, diferenciado: punto de anclaje.
- Calculada con el filtro de seguridad de cada perfil.
- Requiere aprobación, porque `/limitations` ya está en `generation`.

**14. ¿C2-D + EF están apagados?**
- Sí:
  - política por defecto PLAN-HIST-1;
  - `allow-candidates=false`;
  - ninguna configuración de producción tocada.
- PLAN-C2D-0.1 sin EF no se recomienda para usuarios reales.

**15. ¿Siguen identificados los 48 perfiles?**
- Sí.
- Con PLAN-C2D-EF-0.1 los 48 son inválidos por VEF (48/48), con el equipamiento original.

**16. ¿LW-PREF-0.2 y REP-SUST-0.1 están apagados?**
- Sí, con sus indicadores a `false`.
- No existen en la rama aislada.

**17. ¿Las regresiones históricas están verdes?**
- **Sí:**
  - 960/960 perfiles iguales;
  - ciclo real y campaña REPLAY idénticos;
  - baterías HIST 223/225;
  - integración completa sin fallos en las dos ramas.

**18. ¿Pruebas ejecutadas y omitidas por rama?**

| Rama | Unitarias | Integración contabilizadas / ejecutadas / omitidas | Batería |
|---|---|---|---|
| **PR #17** (361d270) | 1.013 | 490 / 436 / 54 | 3 × (225 contabilizadas: 223 aprobadas, 2 omitidas); REPLAY 2/1/1 |
| **Rama aislada** | 995 (d1de25c) | 462 / 429 / 33 (6aec7e0), más 2 de integración en d1de25c | 225 contabilizadas: 223 aprobadas, 2 omitidas |
| **`generation`** | 939 | Integración **no ejecutada** en esta orden | — |

**19. ¿Qué cambios necesitan evaluación profesional?**
- Si se aceptan sesiones más cortas que M por saturación: C2-D (e5, G4, pregunta de duración).
- La clasificación funcional de EF: e2, e3, G1 y G2.
- Si un único ejercicio de tracción basta: punto de anclaje, G3.
- La distribución de fuerza en LOSE_WEIGHT: e7, G4, LW-PREF-0.2.
- Cambiar fuerza por cardio: G5, REP-SUST-0.1.
- Si es pertinente no generar la semana: D01 y D02.

**V4-C no necesita evaluación profesional:** solo endurece un límite que ya existía.

**20. ¿Qué autorizaciones faltan para fusionar, desplegar o activar cada componente?**

| Componente | Fusionar | Desplegar | Activar |
|---|---|---|---|
| PR #18 (versionado + V4-C) | Aprobación de la revisión humana | Aprobación expresa (Flyway aplica V44 al arrancar) | PLAN-V4-0.1: aprobación expresa, después de desplegar con HIST |
| PR #17 | Revisión profesional, más las decisiones C3–C5 y P1–P3, más la actualización tras el PR #18 | Aprobación expresa (V45) | Cada componente por separado: C2-D, EF, LW-PREF-0.2, REP-SUST-0.1 (no recomendado), NOFACT-0.1 y duración/aeróbica (con Flutter) |
| Lista de equipamiento unificada | Aprobación de un cambio de contrato | — | — |
| Flutter | Autorización para modificarlo | — | — |

## Lo que no se ha hecho (no autorizado)

- Fusionar o desplegar.
- Migraciones en Neon o Railway.
- Activar cualquier política o indicador.
- Cambios en 4A, 4C, 4D, V24, V8C, la progresión, la reincorporación, el catálogo o Flutter.
- Llamadas a Replicate.
- Registrar evaluaciones.
