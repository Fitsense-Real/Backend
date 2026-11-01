# Regresiones de V4: duración y compatibilidad histórica

## 1. Pruebas de borde (unitarias, `PoliticasCandidatasTest`)

| Prueba | Casos | Resultado |
|---|---:|---|
| `bordesV4`: 30, 45, 60 y 75 min en el límite exacto, ±1 s y en el límite de redondeo de cada versión | 22 | Pasan |
| `maximosAdmitidos`: máximo en segundos que acepta cada versión para cada M | 4 | Pasan |
| `v4CoincideConProduccion`: HISTÓRICO = `Math.round(M × 1,15)` y TRUNCADO = `floor(M × 1,15)` para M de 30 a 75; mismo redondeo de minutos que el estimador | 1 | Pasa |
| `V4DuracionDeclaradaTest` (validador **real**): un borrador de IA con el 142 % de M y una duración declarada de 35 min pasa V4 y V17 vigentes; SEGUNDOS lo rechaza | 1 | Pasa (documenta la brecha) |

**Correspondencia entre perfil, generador y validador**

| Pieza | Valor |
|---|---|
| Perfil (`maxSessionMinutesAllowed`) | `floor` |
| Validador | `round` sobre lo declarado |
| Generador | `round` sobre lo estimado |
| Motor de reglas | Declarado = estimado en el 100 % de los planes del laboratorio (columna `declarado_coincide`) |

## 2. Efecto en el laboratorio (960 perfiles)

| Camino | Planes válidos | Rechazados por V4-B (truncado) | Rechazados por V4-C (segundos) |
|---|---:|---:|---:|
| C1 | 875 | **6** | 0 |
| C2-A | 950 | 6 | 0 |
| C2-C | 942 | 6 | 0 |
| C2-D | 954 | 6 | 0 |

Los 6 son GENERAL_FITNESS a 30 min (R0045, R0137, R0525, R0857, R0861 y R0954), con una sesión de **exactamente 2.070 s = 115,0 %**.
- V4-B los rechazaría aunque están justo en la tolerancia, porque redondea 34,5 a 35 > 34.
- V4-C los acepta.
- Es la diferencia práctica entre truncar minutos y comparar segundos.

## 3. Compatibilidad histórica

| Conjunto histórico | Sesiones | Cambian con V4-B | Cambian con V4-C |
|---|---:|---:|---:|
| Campaña P-1/2/3 (100 semanas, `campana_p123/evidencia/planes.csv`), segundos recalculados con la fórmula del estimador | 456 | 0 | 0 |

- Lo declarado coincide con lo estimado en las 456 sesiones.
- **Planes históricos que podrían verse afectados** si se reinterpretaran con otra versión:
  - con V4-B, las sesiones del motor de reglas en el límite exacto (como los 6 del laboratorio);
  - con V4-C, los borradores de IA que declaren menos de lo que estiman, dentro del 20 % de V17.
- **Por eso la versión se registra en el plan y los planes sin versión se validan como HISTÓRICO** (`VersionPoliticaPlanificacion.deUnPlan(null)`). No se reinterpreta nada.

## 4. Estado

- La implementación corregida existe como `TechoV4Versionado.SEGUNDOS` y la versión `PLAN-V4-0.1`.
- **No está activada ni cableada.** El validador vigente es el mismo de `generation`.
