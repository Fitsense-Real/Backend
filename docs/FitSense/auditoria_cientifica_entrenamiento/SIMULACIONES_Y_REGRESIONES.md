# Simulaciones y regresiones

**Entorno**
- Rama `feature/modelo-e-planificacion`.
- PostgreSQL 16 efímero (Testcontainers); el JDBC se comprueba como localhost en cada IT.
- Flyway sobre la base efímera.
- **Sin Neon, Railway ni Replicate.**

## 1. Regresión completa (`mvn -o verify -Pintegration`)

| Batería | Referencia histórica | Esta rama | Diferencia |
|---|---|---|---|
| Unitarias (surefire) | 939, 0 fallos | **944, 0 fallos, 0 errores** | +5: `ModeloEExperimentalTest` |
| Integración (failsafe) | 458, 32 omitidas, 0 fallos | **462, 36 omitidas, 0 fallos, 0 errores** | +4 pruebas opt-in, todas omitidas: `LaboratorioModeloEIT` ×2 y `LaboratorioReglasIT` ×2 (este último viene de la rama de investigación y no está en `generation`) |
| IT ejecutadas | 426 | **426** | **Idénticas en número** |

**Resultado:** `EXIT 0`.

**Prueba de aislamiento.** Ninguna clase de `src/main` fuera de `/experimental/` referencia el modelo E. La ruta de generación, validación, adaptación, P-02, P-03, progresión y reincorporación es la misma de `generation`.

## 2. Campaña de 100 semanas (REPLAY)

`CampanaP123Cohorte02IT` con `-Dfitsense.campana.p123=true`:
- **0 llamadas a Replicate**: la IA de corrección es un `@MockitoBean` y la generación inicial usa el motor de reglas;
- 1 prueba ejecutada y 1 omitida (la variante no activada), sin fallos;
- `EXIT 0`.

Evidencia regenerada en `docs/FitSense/campana_p123/evidencia/`:
- **solo cambió `exclusiones_dolor.csv`, y solo en las marcas de tiempo** `recorded_at` y `reactivated_at`;
- los ids, estados, ejercicios y motivos son idénticos;
- el cambio se revirtió.

Con E desactivado, la campaña es **idéntica** a la histórica.

## 3. Matriz revisada (960 perfiles × 3 caminos)

- **Detalle:** `COMPARACION_TRES_CAMINOS.md` y `MATRIZ_EXPERIMENTOS_REVISADA.csv`.
- **Cobertura:**
  - 4 objetivos, 3 niveles y 3 equipos;
  - días {1, 2, 3, 4, 6, 7};
  - minutos {30, 45, 60, 75};
  - 48 perfiles con restricciones y 48 con dolor P-02, donde se retiran en memoria los 6 ejercicios más usados.
- **Hueco de cobertura:** la combinación de 5 días no está en BASE; ya la cubren `CorreccionRespaldo5y6DiasIT` y `FactibilidadDuracionIT`.

| | C1 | C2 | C3 |
|---|---:|---:|---:|
| Válidos | 875 | 950 | 781 (+178 experimentales) |
| V4 estricto excedido | 0 | 0 | 0 |

## 4. Longitudinal con progresión (8 semanas)

Fuente: `SIMULACIONES_LONGITUDINALES_REVISADAS.csv`, con `fitsense.adaptation.progression.enabled=true`.

**Guion**

| S1–S2 | S3 | S4 | S5 | S6–S8 |
|---|---|---|---|---|
| Completas | Dolor | LACK_OF_TIME en 2 sesiones | RPE 9 | Completas |

| Caso | Perfil | Resultado |
|---|---|---|
| P1 | GAIN_MUSCLE intermedio, 60 min | Plan en las 8 semanas; 63 series/semana. Repeticiones semanales 630 → 633 (tras S2) → 630 (tras el dolor, por SEGURIDAD_ANTE_DOLOR) → 630 (CONSERVAR tras falta de tiempo y fatiga) → 633 → 636 |
| **P2** | GAIN_MUSCLE intermedio, **75 min, piso del 70 %** | **Sin plan en ninguna semana.** Es la limitación conocida de V20 × V24 |
| P2m | Igual que P2 con **piso del 55 %** (C2) | Plan en las 8 semanas, con la misma trayectoria que P1 |
| P3 | INCREASE_STRENGTH principiante, 45 min | P-03 PUBLICADO desde S4; progresión 432 → 436 |
| P4 | LOSE_WEIGHT intermedio, 45 min | P-03 PUBLICADO; 36 series/semana; progresión 108 → 114 |
| P5 | GENERAL_FITNESS avanzado, 60 min | En S4, P-03 da **NO_FACTIBLE** y no hay plan; en S5 se regenera con el motor (86 series) |

**Efectos en adaptación y progresión**
- **Nunca se fuerza una subida con dolor ni con fatiga**: S4 y S6 no suben respecto de su semana previa.
- La progresión es pequeña: unas 3 repeticiones por semana completa, por D-P3 y el paso del 5 %.
- LACK_OF_TIME dio CONSERVAR, no ACORTAR, porque la falta de tiempo fue parcial. **El escenario ACORTAR no quedó cubierto** en este guion; lo cubre la batería existente (D8/C-2).
- **La reincorporación no se ejercitó** en el longitudinal; la cubre su batería (R-1 a R-5).

## 5. Riesgos de regresión que quedan

1. Si se activa C2, los planes de 79 perfiles pasan de «sin plan» a «plan más corto que lo declarado». Requiere D-03.
2. Si se activa E:
   - cambia el volumen de casi todos los perfiles;
   - la IA, que recibe `min_session_minutes` y los topes 4D, divergiría del validador.
3. Las semillas dependen del orden de `user_id`. Añadir casos a las IT existentes desplaza los resultados; ya ocurrió con `PoliticasP123IT`.
4. Los laboratorios son opt-in, de modo que la CI no los ejecuta. Sus CSV son instantáneas.

## 6. Evaluación humana

**No se hizo ninguna evaluación humana.** No hubo evaluadores ni profesionales del ejercicio, y no se afirma lo contrario.
