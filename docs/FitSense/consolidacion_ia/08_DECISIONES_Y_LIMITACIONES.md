# 08 · Decisiones y limitaciones

## 1. Decisiones tomadas

Estas decisiones ya estaban tomadas y aquí solo se registran. Esta documentación no las cambia.

| Componente | Decisión | Base de evidencia | Documento |
|---|---|---|---|
| Generación inicial | **Mantener PR-1.7** | Ninguna variante cumplió los criterios en una ronda confirmatoria. pr-1.8a: 13/18 frente a 13/18 con la versión actual del modelo (decisión B) | `05` §5–§7 |
| Adaptación | **Mantener V2.1** | v2.2a no es peor, pero su ventaja es un solo caso (O18), con p = 0,50 en CONF-IA y p = 1 en MEJ R2 | `05` §4, §7; `03` |
| PR-1.8 y variantes (pr-1.8a, pr-1.8b, pr-1.8c) | **No adoptadas** | PR-1.8: V24 en O18. pr-1.8b: V24 tras R1. pr-1.8c: NO CUMPLE (V24, V4). pr-1.8a: decisión B | `05` |
| v2.2a y v2.4 | **No adoptadas** | Sin ventaja demostrada; v2.4 no aporta nada sobre v2.2a | `05` §3–§4 |
| V8C (H-A1 y H-A2) | **Corregidos y fusionados** (PR #44, `543932a`) | 25 pruebas nuevas, 9 fallos sin la corrección; regresión 1331/0/1 y 593/0/44; CI verde | `06` |
| Pruebas dependientes de la fecha | **Corregidas y fusionadas** (PR #43, `d8c925f`) | Solo código de prueba; CI verde | `06` §5 |
| V24 | **Sin cambios** | Rechazó todas las dosis incorrectas; ningún plan se publicó con V24 | `02`, `05` |
| Progresión y reincorporación | **Políticas existentes, sin cambios** | GRA: 26/30 progresiones, 3/3 recuperaciones y 1/0 reincorporaciones, sin violar el techo ni el paso por ciclo | `07` |
| Configuración por defecto de `generation` | **Configuración evaluada activada** (PR #45, 11/11/2026): IA, PR-1.7, V2.1, PLAN-REV-2.0, progresión, límite de dificultad `preferencia` y retirada localizada `on` | Coincide con la de las campañas con IA real, salvo la progresión, que pasa del bloque P a todos los perfiles (§3) | `00` §5, `07` §1 |

### Lo que sigue abierto y no se activa
- **pr-1.8a** existe solo en `experimento/confirmacion-pr18a-final`, apagada por defecto.
- **pr-1.8b, pr-1.8c, v2.2a y v2.4** existen solo en `experimento/mejora-ia`.
- Ninguna de las dos ramas está fusionada en `generation`.

## 2. Tres tipos de afirmación

| Tipo | Ejemplos | Qué sostiene |
|---|---|---|
| **Resultado técnico** (medido y reproducible con la evidencia) | Ningún plan ni adaptación publicados con infracciones de V1–V27/V8C. BASE: 19/19 planes con infracciones sin reglas. 0 cambios no autorizados con reglas. Regresión 1331/0/1 y 593/0/44 | El backend impide publicar propuestas que incumplen las políticas de FitSense |
| **Decisión experimental** (comparación con criterios congelados) | Mantener PR-1.7 y V2.1; no adoptar las variantes | Ninguna variante mostró una mejora estadística con los criterios fijados antes de pagar |
| **Observación pendiente de validación profesional** | Pertinencia ACC/ALT/CONS-J/INC; planes «requiere revisión» por el catálogo; DOSIS_LIMITADA en 16/19; O12 retirando toda la sesión ante dolor; el 5 %, el techo de 1,10× y el 100 % de la progresión | **No** sostiene calidad deportiva: la juzga un oráculo del laboratorio, no un profesional |

## 3. Limitaciones

### De diseño experimental
- **Perfiles sintéticos** (O01–O20) y **adherencia simulada** por guion. **No se puede afirmar que FitSense mejore la adherencia de usuarios reales.**
- **Muestras pequeñas:**
  - 18–19 perfiles por ronda de generación;
  - 12 sesiones de adaptación por ronda.
  - Los resultados neutros (p > 0,05) significan «no demostrado», no «igual».
- **Pocas rondas confirmatorias:**
  - pr-1.8a tuvo una ronda con cada versión del modelo (A1 con `045f8102…` y A2 con `81cb6179…`);
  - A3 quedó incompleta.
- **Oráculo del mismo laboratorio** (v1.4), aunque se congeló antes de pagar y tiene pruebas negativas.

### Del proveedor
- **La versión del modelo cambió dos veces** (`9f4cd9ec…` → `045f8102…` → `81cb6179…`), y el backend no puede fijarla. Las comparaciones se hacen dentro de una misma versión.
  - GRA y las campañas de mejora no son comparables entre sí de forma directa.
- **BASE** solicitó `9f4cd9ec…`, pero el proveedor la devuelve como `hidden`: **no verificable**.
- **Variabilidad del modelo:** 0/20 planes idénticos entre corridas (Jaccard 0,36).
- **Incidencias de infraestructura:** timeouts, 502, `failed` y errores de E/S en el POST. Se gestionaron sin repetir respuestas válidas (regla I-MEJ-1).

### De configuración
- **Desde la PR #45 (11/11/2026), los valores por defecto de `generation` son la configuración evaluada** (tabla en `00` §5). Cada valor conserva su variable de entorno para revertirlo.
- **Diferencias que quedan respecto a lo evaluado con IA real:**
  - **progresión:** se evaluó solo en el bloque P (O15–O20); ahora está encendida para todos. Arrastra la V24 por capacidades a todas las adaptaciones (acoplamiento ya existente). Con IA simulada, en el bloque G, una adaptación de dolor (O12) pasa de APLICADA a SIN_EFECTO (`07` §1). Es más restrictivo y nunca añade trabajo, pero no se ha medido con IA real;
  - **espera del POST:** el arnés usaba `Prefer: wait=25`; la aplicación mantiene 60 s (`FITSENSE_AI_PREFER_WAIT_SECONDS`). Es un parámetro de transporte; el límite de 150 s es el mismo.
- **Requisitos para que la IA funcione en un entorno:**
  - `REPLICATE_API_TOKEN` definido en ese entorno; sin él, la IA no es utilizable y genera el motor de reglas;
  - la migración V44_3 aplicada, porque la restricción de `weekly_training_plans` admite PLAN-REV-2.0 desde V44_3. Flyway la aplica al arrancar.
- **Configuración desplegada no verificada:** según la documentación del repositorio, Railway no despliega desde `generation`, y las variables de los entornos desplegados no se han comprobado. **No se afirma que la IA funcione en ningún entorno.**
- **Pruebas que conservan la configuración histórica:** 37 pruebas de integración escritas sobre los valores por defecto anteriores los fijan ahora de forma explícita, sin cambiar sus aserciones. Siguen siendo necesarias: los planes con linaje PLAN-HIST-1 conservan esa política.
- **Comentarios desfasados** en `application.properties`, observados y no modificados: «NO activar ENFORCE» junto a un valor ENFORCE, y «se entrega en OBSERVE» junto a `ENFORCE_AI_ONLY`.
- **Política ante perfiles sin plan factible** (PLAN-REV-2.0):
  - INCREASE_STRENGTH sin tiempo para los 4 patrones: FUERZA_SIN_TIEMPO antes de llamar a la IA, sin plan y con mensaje al usuario (O11);
  - si ninguna propuesta de la IA es válida, el motor de reglas; con REV2, además, alternativas deterministas acotadas (D-G3);
  - si todo falla, la semana queda sin plan (`PlanGenerationFailedException`, adherencia NULL);
  - H-G9 (GENERAL_FITNESS intermedio o avanzado con 1 × 30–35 min): la IA nunca da un plan válido, se gastan hasta 3 llamadas y publica el motor de reglas. No es un riesgo de seguridad.

### De evidencia
- **MEJ ronda 2 no tiene manifiesto `SHA256SUMS.txt`.** Solo se verificaron a mano los ejecutores y el análisis.
- **GRA:** 3 registros `*.log` están fuera del repositorio por `.gitignore`. Se verificaron con su copia del ZIP.
- **G17 R2 de MEJ ronda 2** se reconstruyó sin métricas de dosis ni tiempo (I-R2-9).

## 4. Pendientes (no forman parte de este encargo)

1. **Revisión profesional de la calidad deportiva:**
   - planes con observaciones de catálogo;
   - adaptaciones ALT (O12);
   - dosis conservadoras (DOSIS_LIMITADA);
   - parámetros de progresión.
2. **Hallazgos de auditoría no tratados** (`ronda2/AUDITORIA_REGLAS_FITSENSE.md` §7): H-A3, H-G4, H-G5, H-G8, H-G9 (alta: GENERAL_FITNESS intermedio/avanzado con 1 × 30–35 min no tiene solución), H-G14 y otros.
3. **Si en el futuro se reconsidera pr-1.8a:**
   - antes de medir, resolver la regresión V4 en LOSE_WEIGHT y los timeouts;
   - medir en varias rondas con la misma versión del modelo.
4. **Si se reconsidera v2.2a:** presentarla como mejora de coherencia en un caso concreto (O18), no como mejora estadística.
5. **Evaluación con usuarios reales:** necesaria para cualquier afirmación sobre adherencia.
