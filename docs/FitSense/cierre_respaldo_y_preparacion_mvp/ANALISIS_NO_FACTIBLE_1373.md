# Análisis del caso NO_FACTIBLE del ejercicio 1373 (D3)

**Decisión que se mantiene:** si no existe una semana segura y válida, FitSense **no publica** una rutina incorrecta ni levanta restricciones para producirla.

**Cómo se ha estudiado:**
- **Laboratorio:** `AnalisisSustitucion1373IT` (*opt-in*, `-Dfitsense.analisis.1373=true`, en el PR de D1). Usa un PostgreSQL efímero propio, la IA simulada que siempre falla y 0 llamadas a Replicate.
- **Qué reproduce:** el caso C de `RespaldoConDolorIT` (GENERAL_FITNESS, intermedio, gimnasio, 3 días, 45 min, semana 2026-11-02).
- **Qué mide:** con el **contexto real** de la semana nueva (el mismo `prepare` que usa el ciclo), aplica uno a uno los filtros de `SafetySubstitutionPolicy.elegir`.
- **Código ejecutado:** `generation` 8f112d5 más ese laboratorio.
- **Evidencia:**
  - `evidencia/analisis_1373.md`;
  - `evidencia/candidatos_1373.csv`, con los candidatos que fallan un único filtro.

## 1. El ejercicio

| Atributo | Valor |
|---|---|
| Id | **1373** |
| Nombre | «Círculos de muñeca» |
| Zona (`body_part`) | lower arms |
| Músculo objetivo | forearms |
| Equipamiento | body weight |
| Dificultad | 1 |
| Tipo de prescripción | **SETS_REPS** (`is_duration_based = false`) |
| Patrón de movimiento | MOBILITY |
| Clase de slot (4C) | relleno (MOBILITY no es un slot obligatorio) |
| Para GENERAL_FITNESS | **Complemento** (`CatalogEligibilityPolicy.esComplemento`: solo entra por la capacidad de movilidad de 4A v6) |

## 2. Los dos casos reproducidos

**C1 y C2 tienen la misma sesión:** el viernes FULL_BODY, con dolor en el 1373.

| Caso | Usuario | ¿SUST-SEG-1 encuentra sustituto? | Desenlace del ciclo |
|---|---|---|---|
| C1 | 2 | **No** | NO_FACTIBLE en V32 → **P-03 nivel 2 regenera la semana con reglas y la publica**: repeticiones 416 → 416 y segundos 1.200 → 1.200, dentro del trabajo medido |
| C2 | 32 | **No** | NO_FACTIBLE → P-03 nivel 2 **rechazado**: repeticiones 416 → 396, pero segundos 1.200 → **1.260**. **No se publica nada** |

**Motivo V32 en los dos:** «el ejercicio 1373 … dejó de ser compatible por seguridad (dolor_registrado) y no hay sustituto elegible con la regla SUST-SEG-1: mismo slot (relleno), tipo SETS_REPS y zona que admita el enfoque FULL_BODY. Requiere intervención; no se publica una sustitución insegura».

## 3. Por qué no hay sustituto: el embudo de SUST-SEG-1

### Sobre los elegibles del perfil (731), con el contexto real

| Filtro (en el orden de `elegir`) | Quedan |
|---|---|
| F1 distinto del original | 731 |
| F2 misma clase de slot (relleno) | 418 |
| F3 mismo tipo de prescripción (SETS_REPS) | 351 |
| **F4 misma condición de complemento (sí)** | **0** |
| F5 zona que admite FULL_BODY o la misma | 0 |
| F6 brazo admitido (V22) | 0 |
| F7 no en revisión de clasificación | 0 |
| F8 no en la sesión ni en un día contiguo | 0 |

### Sobre el catálogo activo entero, sin filtro de perfil (1.219)

El mismo resultado: **0** tras F4.

### Candidatos que fallan **un único** filtro

| Filtro que falla | Cuántos (elegibles) | Qué son |
|---|---|---|
| **F3, tipo de prescripción** | **21** (27 en el catálogo) | Los **estiramientos de movilidad complementarios**, todos de **DURATION**. Entre ellos, **1178 «Estiramiento de muñeca lateral»**: misma zona (lower arms) y mismo músculo (forearms), dificultad 1, peso corporal. También 66, 67, 456, 773, 881, 917, 1150, 1375… |
| F4, complemento | 197 | Rellenos de series y repeticiones que **no** son complemento (core, abducción de cadera, toques de dedos…). Entran por la vía estructural o de relleno normal, no por la capacidad de movilidad |

**Conclusión del embudo:**
- **1373 es el único complemento de movilidad que se prescribe por repeticiones** en todo el catálogo activo.
- Los otros 27 complementos de movilidad son de duración.
- SUST-SEG-1 exige el **mismo tipo** (V7) y la **misma condición de complemento**, así que no puede elegir ninguno.

## 4. Clasificación

| Opción de la orden | ¿Aplica? |
|---|---|
| No hay un sustituto compatible en el catálogo | **No exactamente:** hay 27 complementos de movilidad de la misma clase, uno de ellos de la misma zona y músculo (1178) |
| **Existen candidatos, pero incumplen restricciones** | **Sí:** todos incumplen la restricción «mismo tipo de prescripción» (F3) |
| **La política es demasiado limitada y requiere una decisión funcional** | **Sí, como consecuencia:** SUST-SEG-1 no admite cambiar de SETS_REPS a DURATION. Con este catálogo, un complemento de movilidad por repeticiones nunca tiene sustituto |
| Hay una inconsistencia concreta en los datos | **No demostrada.** Unos círculos de muñeca se prescriben razonablemente por repeticiones, así que no se puede afirmar que `is_duration_based = false` sea un error. Es, eso sí, el **único** ejercicio de movilidad complementaria así marcado. Conviene que una persona del equipo lo confirme |

**Nada se ha corregido.** No se ha tocado el catálogo, ni la dificultad, ni el patrón, ni la política.

## 5. Opciones (para decidir; ninguna implementada)

| Opción | Qué cambiaría | Riesgo |
|---|---|---|
| **a. Mantener (actual)** | Nada. Con dolor en 1373 la semana queda NO_FACTIBLE salvo que P-03 nivel 2 encuentre una semana dentro del trabajo medido | La persona puede quedarse sin semana (caso C2) |
| b. Revisar el dato de 1373 | Si el equipo decide que los círculos de muñeca son de duración, tendrían sustituto (1178) | Cambio de catálogo: necesita autorización y migración |
| c. Permitir en SUST-SEG-1 el cambio SETS_REPS → DURATION **solo entre complementos de movilidad** | Habría sustituto, con una dosis que no suba el trabajo; se necesita una regla de equivalencia | Cambio funcional de política; además, hoy la regla de trabajo compara segundos y repeticiones por separado, y pasar de repeticiones a duración **sube los segundos** (es lo que hizo fallar C2) |
| d. Permitir **retirar** un complemento de movilidad con dolor si no hay sustituto | La sesión quedaría con un ejercicio menos (relleno, no slot obligatorio) | Cambio funcional; hay que comprobar V20 (minutos mínimos) y la cobertura de la capacidad de movilidad |

**Recomendación:** a) mientras se decide; después, b) como primer paso, porque es el más acotado si el dato resulta discutible. Ver `DECISIONES_PENDIENTES.md`.

## 6. Contrato para Flutter (documentado, sin implementar)

Hoy, con NOFACT-0.1 **apagado**, la app ve:
- en `GET /api/v1/users/me/plan/adaptations`: el `AdaptationOutcome` con `resultado = NO_FACTIBLE_POR_SEGURIDAD`, `estado = NO_FACTIBLE` y `motivos` (el texto de V32);
- en `GET /api/v1/users/me/plan/current`: **no hay plan activo** para la semana nueva.

**Lo que la app necesitaría para explicarlo** (contrato propuesto; ver `ESTADO_CONTRATOS_FLUTTER.md`):

| Campo | Contenido | Hoy |
|---|---|---|
| `estadoPublicacion` | `NO_PUBLICADO` | Implícito (sin plan activo) |
| `motivoCodigo` | `SIN_SUSTITUTO_SEGURO` | No existe: solo hay texto |
| `motivoTexto` | «No hemos encontrado un ejercicio seguro que sustituya a "Círculos de muñeca", que te causó molestias. No publicamos una rutina que no podamos garantizar.» | El texto de V32 es técnico |
| `sesionAfectada` | Fecha y enfoque | En `motivos`, como texto |
| `restriccion` | El ejercicio excluido y su exclusión (`/pain-exclusions`) | Sí, por separado |
| `alternativas` | Solo si están **validadas** (p. ej. reactivar la exclusión si ya no hay dolor: `POST /pain-exclusions/{id}/reactivation`) | El endpoint existe |

NOFACT-0.1 (`fitsense.api.no-factible-estructurado.enabled`, en el PR #17) ya da un 422 con `diagnosis` estructurado para la generación inicial. **Para la adaptación semanal haría falta extenderlo:** no se ha hecho y sigue apagado.
