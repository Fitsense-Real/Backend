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
- **Los valores por defecto de `generation` no son la configuración evaluada:**

  | | Por defecto | Evaluado |
  |---|---|---|
  | IA | Apagada | Encendida |
  | Política | PLAN-HIST-1 | PLAN-REV-2.0 |
  | Prompt de adaptación | v1 (FIX-PR-1.3) | V2.1 |
  | Progresión | Apagada | Encendida en el bloque P |

  Esta documentación **no cambia** ningún valor por defecto. Activar la configuración evaluada en un entorno es una decisión aparte, fuera de este encargo.
- **Comentarios desfasados** en `application.properties`, observados pero no modificados porque no se toca código: «NO activar ENFORCE» junto a un valor ENFORCE, y «se entrega en OBSERVE» junto a `ENFORCE_AI_ONLY`.

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
