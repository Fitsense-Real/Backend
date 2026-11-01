# Simulaciones y regresiones del cierre

**Entorno común**
- Rama `feature/modelo-e-planificacion`.
- PostgreSQL 16 efímero (Testcontainers). Cada IT comprueba que el JDBC es `jdbc:postgresql://localhost:`. Flyway corre sobre la base efímera.
- **Sin Neon, sin Railway, sin despliegues y sin Replicate real:** la IA de corrección es un `@MockitoBean` y `fitsense.ai.enabled=false`.

## 1. Regresión completa (`mvn -o verify -Pintegration`)

| Batería | Referencia | Ahora | Detalle |
|---|---|---|---|
| Unitarias | 944, 0 fallos | **975, 0 fallos, 0 errores, 0 omitidas** | +30 `PoliticasCandidatasTest` (V4 de borde, C2, versionado, métrica aerobica) y +1 `V4DuracionDeclaradaTest` |
| Integración | 462, 36 omitidas, 0 fallos | **466, 40 omitidas, 0 fallos, 0 errores** | +4 omitidas: `VerificacionPoliticasIT`, que es opt-in |
| IT **ejecutadas** | 426 | **426** | Idénticas en número: las nuevas son opt-in |

`EXIT 0`.

**Nota de trazabilidad**
- El método 5 del laboratorio (`diagnosticoDiscrepantes`) se añadió después de compilar esa regresión. En la próxima ejecución completa contará como una omitida más: 467/41.
- Se ejecutó en su propia corrida (abajo), junto con los otros cuatro, con 5/5 sin fallos.
- **Las omitidas no se cuentan como ejecutadas.**

**Aislamiento de producción**
- `ModeloEExperimentalTest.aislamiento`: ninguna clase de `src/main` fuera de `/experimental/` referencia `ModeloE` ni `.experimental.`. Cubre también `PisoV20Acotado`, `TechoV4Versionado`, `VersionPoliticaPlanificacion` y `MetricaAerobica`.
- El único cambio en una clase existente es hacer `public` el método `ConstructorSemanaModeloE.dosisBase`, que es experimental.
- **La ruta de producción no cambió.** Generación, validación, adaptación, P-02, P-03, progresión y reincorporación son las de `generation`.

## 2. Campaña de 100 semanas (REPLAY determinista)

`CampanaP123Cohorte02IT` con `-Dfitsense.campana.p123=true`.

| Ejecución | Resultado | Evidencia |
|---|---|---|
| 1 (tras la regresión) | 1 ejecutada y 1 omitida (variante no activada); 0 fallos | Solo cambió `exclusiones_dolor.csv` |
| 2 (repetida para comparar bien) | Igual | **Las 2 líneas cambiadas son idénticas salvo las marcas de tiempo** (`recorded_at`, `reactivated_at`). Revertido |

**0 llamadas a Replicate.** Con las políticas nuevas desactivadas, la generación y los ciclos históricos son **idénticos**.

## 3. Laboratorios opt-in (aparte)

| Prueba | Resultado |
|---|---|
| `VerificacionPoliticasIT` (5 métodos), con `-Dfitsense.verificacion.politicas=true` | 5/5, 0 fallos (110 s) |
| `matrizC2` | 960 perfiles × 6 caminos (`MATRIZ_C2_VERIFICADA.csv`). Reproduce exactamente la matriz anterior |
| `detalleYMuestra` | Detalle de los 79 recuperados y de la referencia de menor tiempo. Muestra ciega de 28 planes válidos |
| `fuerzaYCardioLoseWeight` | 230 planes válidos de LOSE_WEIGHT |
| `longitudinal` | 7 casos × 10 semanas |
| `diagnosticoDiscrepantes` | 11 perfiles, una fila por sesión corta |

**Escenarios cubiertos por la matriz**
- Los 8 discrepantes de C2-C.
- GAIN_MUSCLE a 75 min (60 perfiles).
- LOSE_WEIGHT con pocos días (1–2) y con restricciones.
- GENERAL_FITNESS con sus capacidades obligatorias.
- INCREASE_STRENGTH 3×6.
- Dolor (48 perfiles con P-02 simulado).

Los perfiles congelados no cambian: el laboratorio no modifica perfiles ni M.

## 4. Longitudinal de 10 semanas (`LONGITUDINAL_VERIFICACION.csv`)

**Guion**

| S1–S2 | S3 | S4 | S5 | S6 | S7–S10 |
|---|---|---|---|---|---|
| Completas | Dolor (P-02) | ACORTAR (falta de tiempo habitual, M/2) | RPE 9 | Baja adherencia (1 sesión) | Completas |

Progresión encendida. El piso de la configuración de la base efímera se fija por caso, como aproximación de C2 (en producción C2 solo actuaría si V20 fuera la única regla incumplida).

| Caso | Perfil | Piso | Resultado |
|---|---|---:|---|
| L1 | GAIN_MUSCLE intermedio, gimnasio, 3 d, 60 min | 70 | Plan las 10 semanas; repeticiones 630 → 636 |
| L2 | GAIN_MUSCLE intermedio, gimnasio, 3 d, **75 min** | 70 | **Sin plan ninguna semana** |
| L3 | Igual que L2 | **55** | Plan las 10 semanas, idéntico a L1 |
| L4 | INCREASE_STRENGTH intermedio, peso corporal, 6 d, 75 min | 70 | Sin plan ninguna semana |
| L5 | Igual que L4 | 55 | S1–S3 con plan; **desde S4, sin plan** (P-03 NO_FACTIBLE semana a semana) |
| L6 | LOSE_WEIGHT principiante, mancuernas y bandas, 4 d, 75 min | 60 | S1–S3 con plan; **desde S4, sin plan** |
| L7 | GENERAL_FITNESS avanzado, gimnasio, 4 d, 60 min | 70 | S4 sin plan (P-03 nivel 2: no añade trabajo ante dolor); S5 regenerado; S5–S10 con plan |

**Invariantes de progresión** (comprobados sobre repeticiones + segundos semanales):
- **0 violaciones** del 5 % por paso y del techo acumulado del 10 % en los 7 casos.
- **Nunca sube tras dolor, fatiga o baja adherencia:** las semanas siguientes a S3, S5 y S6 conservan o reducen el trabajo total.
  - **Matiz en L7:** la semana regenerada en S5, tras una S4 sin plan, sube las repeticiones (739 → 818) y baja los segundos de trabajo por duración (1.920 → 1.680). El total baja de 2.659 a 2.498.
  - Es la generación ordinaria tras una semana sin plan, con las exclusiones de P-02 activas, no una progresión. El patrón ya aparecía en el laboratorio anterior (P5).
  - Se señala para revisión porque el trabajo se mide en dos unidades distintas.
- **Un piso menor no autoriza a subir la dosis:** L3, al 55 %, sigue la misma trayectoria que L1, al 70 %.

**Hallazgos**
1. **L5 y L6 (catálogos pequeños con dolor).**
   - P-02 excluye 7–8 ejercicios. La regeneración segura llega a 1.896 s (42 %) y a 1.806 s (40 %) de M, y V20 (55 % y 60 %) la rechaza (`REGENERACION_SEGURA_NO_FACTIBLE` en la traza).
   - La persona queda **sin plan de S4 a S10**.
   - No lo provoca C2: ocurre con cualquier piso fijo. Es la interacción de P-02 con V20 en catálogos pequeños.
   - C2-D (saturación) podría resolverlo, pero **no se ha comprobado en el ciclo**, porque no está cableado.
2. **ACORTAR con la IA desactivada.** Las semanas tras «falta de tiempo habitual» salen como RESPALDO y la sesión se conserva. El recorte lo cubren las baterías existentes con la IA simulada (D8, C-2). Aquí no se ejercitó.
3. **Reincorporación:** no se ejercitó en este guion. La cubren sus baterías (R-1 a R-5) dentro de la regresión.

## 5. Riesgos de regresión que quedan

- **C2-D:** sesiones del 46–68 % de M. Hay que mostrar la duración estimada (cambio de contrato y de Flutter, no hecho).
- **V4-C:** cambia la aceptación de borradores de IA con duración declarada inferior a la estimada. Se compensa con el versionado.
- **Semillas:** dependen del orden de `user_id`. Añadir perfiles a la matriz desplaza los resultados; los nuevos se añaden siempre al final.
- **Laboratorios:** son opt-in y la CI no los ejecuta. Sus CSV son instantáneas reproducibles.
