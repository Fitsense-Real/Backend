# Auditoría del nivel 2 de P-03

**Pregunta:** el nivel 2 regenera la semana con el motor de reglas, con dosis base y sin el linaje de la semana medida. ¿Puede eso aumentar el trabajo de forma no autorizada después de una reducción previa?

Primero se reprodujo y se midió.

## 1. Método

En `ProgresionIT`, con la progresión encendida solo en la base aislada:
- la IA simulada reduce por fatiga (−2 repeticiones o −1 serie, D-P11);
- el nivel 1 se fuerza a no encontrar sustituto (resolutor de exclusiones vacío para ese usuario), para llegar al nivel 2;
- el dolor se declara al omitir una sesión, así que P-02 excluye sus ejercicios.

**Medidas:** repeticiones totales (series × repeticiones) y segundos totales (series × segundos), por separado, de la semana medida frente a la regenerada.

## 2. Resultados antes de la corrección

| Escenario | Semana medida | Regenerada | ¿Sube? |
|---|---|---|---|
| 1 · LOSE_WEIGHT, reducción por fatiga y después dolor | 102 rep / 2160 s | **108 rep** / 2160 s | **Sí: deshace la reducción** |
| 1 · GENERAL_FITNESS, reducción por fatiga y después dolor | 390 / 1260 | 372 / 1260 | No |
| 1 · INCREASE_STRENGTH (sin reducción válida posible) y después dolor | 216 / 0 | 216 / 0 | No |
| 2 · GENERAL_FITNESS, progresión (paso autorizado) y después dolor | 399 / 1260 | 372 / 1260 | No (vuelve a la base; sin paso nuevo) |
| D8 · ACORTAR (viernes acortado) y después dolor (`HerenciaD8IT`) | 450 / 0 | **486** / 0 | **Sí: devuelve el trabajo retirado** |

**Defecto confirmado.**
- **Dimensión que aumenta:** las repeticiones totales de la semana.
- **Causa raíz:** el nivel 2 genera desde cero con las dosis base de 4D y no conserva las reducciones autorizadas (fatiga) ni las retiradas de ACORTAR, porque la semana regenerada no tiene linaje.

## 3. Corrección

- **Alternativa determinista evaluada:** no existe con las reglas vigentes. Conservar las dosis reducidas en otra composición exigiría inventar autorizaciones sin linaje, que es una política nueva y no está aprobada.
- **Lo que se hizo** (`SafeRegenerationService.masTrabajoQueLaMedida`): aplica las reglas ya aprobadas («ante dolor no se añade trabajo» y «P-03 no puede superar volumen»). Si la semana regenerada sube las repeticiones **o** los segundos totales de la semana medida, el nivel 2 **no publica**:
  - registra NO_FACTIBLE con el motivo en `safe_regeneration_attempts`;
  - la API muestra `NO_FACTIBLE_POR_SEGURIDAD` y `P-03: sin alternativa segura válida: …`;
  - la exclusión P-02 se conserva.
- **Lo que no se introdujo:** ninguna política nueva de reducción ni de progresión.

## 4. Resultados después de la corrección

| Escenario | Resultado |
|---|---|
| LOSE_WEIGHT con reducción | **NO_FACTIBLE** («102 → 108 repeticiones … ante dolor no se añade trabajo»); nada publicado; exclusión activa |
| GENERAL_FITNESS con reducción | Publicado (372 ≤ 390) |
| INCREASE_STRENGTH | Publicado (216 = 216) |
| Progresión | Publicado (372 ≤ 399); **sin paso nuevo** (ningún V32 para el plan); idempotente |
| D8 (ACORTAR y dolor) | **NO_FACTIBLE** (450 → 486) |

## 5. Comprobaciones de los cuatro escenarios de la orden

| Escenario | Prueba | Comprobado |
|---|---|---|
| 1 · Dosis previamente reducida | `ProgresionIT.p03Nivel2TrasReduccion` (3 objetivos) | Sin aumento publicado; NO_FACTIBLE cuando subiría |
| 2 · Dosis progresada | `ProgresionIT.p03Nivel2TrasProgresion` | Sin progresión nueva, sin el excluido, sin recuperar dosis, trazable e idempotente |
| 3 · Varias sesiones y dolor localizado | `PoliticasP123IT.sinSustitutoRegeneraYEsIdempotente`, `interrupcionYReintento`, `restriccionesMultiplesYDolorAnterior` | Sesiones ejecutadas intactas, sin duplicados, V1–V25 y V8C |
| 4 · Regeneración imposible | `PoliticasP123IT.regeneracionInviable`, LOSE_WEIGHT y D8 de arriba | NO_FACTIBLE con motivo, exclusión conservada, nada publicado, sin sesiones ni fechas inventadas |

## 6. Linaje y trazabilidad del nivel 2 (comprobado en las pruebas)

- **V32:** el intento original NO_FACTIBLE consta; el plan regenerado **no** tiene V32 propio, así que no se presenta como una adaptación con continuidad.
- **Sin linaje falso:** ninguna sesión del plan regenerado lleva `source_planned_workout_id`.
- **Traza:** `safe_regeneration_attempts` con `source = RULES`, `outcome`, el plan, los excluidos y el motivo; el plan regenerado tiene origen `RULE_ENGINE`.
- **Dosis:** las dosis base de 4D, revalidadas con la validación completa (V1–V25, V24 incluida) antes de publicar.
- **Ejercicios prohibidos:** ninguno.
- **Idempotencia:** el ciclo repetido devuelve el mismo plan y hay una sola fila P-03.

## 7. Efecto en la campaña

Ninguno. En la campaña p123 el nivel 2 no llegó a actuar (`regeneraciones.csv` vacío): el único dolor que no resolvió la IA (N06) lo resolvió el nivel 1.
