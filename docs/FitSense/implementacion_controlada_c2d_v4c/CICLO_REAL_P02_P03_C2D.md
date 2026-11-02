# Ciclo real: dolor, P-02, P-03 y continuidad con C2-D

**Pruebas:** `CicloRealHistoricaIT`, `CicloRealC2DIT` y `CicloRealC2DV4IT`. Cada una tiene su base efímera y la política fijada **por configuración**:
- `fitsense.planning.policy`;
- `allow-candidates` solo en las candidatas.

**`calculation_configs` no se toca:** cada prueba afirma que el piso sigue al 70 %. Es la política versionada la que decide.

**Recorrido:** perfil → generación → publicación → ejecución → dolor (S3) → exclusiones P-02 → adaptación → P-03 → validación → publicación o rechazo. Datos en `LONGITUDINAL_C2D.csv` (las tres políticas) y `PLANES_CICLO_PLAN-C2D-0.1.csv` (ejercicio por ejercicio).

## 1. Resultados

| Caso | Perfil | PLAN-HIST-1 | PLAN-C2D-0.1 (= PLAN-C2D-V4-0.1) |
|---|---|---|---|
| L1 | GAIN_MUSCLE intermedio, gimnasio, 3 d, 60 min | Plan S1–S10 | Idéntico |
| L2 | GAIN_MUSCLE intermedio, gimnasio, 3 d, **75 min** | **Sin plan S1–S10** | **Plan S1–S10**: 3 sesiones al 56,7 % (3 excepciones C2-D) |
| L4 (antes L5) | INCREASE_STRENGTH intermedio, peso corporal, 6 d, 75 min | **Sin plan S1–S10** | **Plan S1–S10**: 138 series, PULL al 58,5 %. Tras el dolor, la sustitución determinista (respaldo) conserva la semana sin los 8 excluidos |
| **L6** | LOSE_WEIGHT principiante, mancuernas y bandas, 4 d, 75 min | **Sin plan S1–S10** | Plan S1–S3. **S4: P-03 PUBLICADO** (regeneración al 40,1 %, 70 series y 1.620 s de cardio; antes 82 series y 2.700 s). **Continúa S5–S10** |
| L7 | GENERAL_FITNESS avanzado, gimnasio, 4 d, 60 min | S4 sin plan (P-03 nivel 2: «no se añade trabajo ante dolor»); S5–S10 con plan | **Idéntico**: no es un problema de V20 |
| **L8** | GAIN_MUSCLE avanzado, peso corporal, 6 d, 75 min | **Sin plan S1–S10** | Plan al 46 %. **S4: P-03 PUBLICADO al 34,7 %** (100 series; PUSH con solo 4 ejercicios tras excluir 7). Continúa S5–S10 |

**C2-D frente a la combinada.** Entre `PLAN-C2D-0.1` y `PLAN-C2D-V4-0.1` hay **0 diferencias**: V4-C no cambia los planes del motor.

## 2. Respuestas a lo pedido

**¿Se reprodujo el bloqueo de L5 y L6?** Sí. Con la política histórica, L4 (el L5 anterior) y L6 no reciben plan en ninguna semana. Ni siquiera llegan a la semana del dolor: V20 ya rechaza la generación inicial.

**¿C2-D permite continuar el ciclo cuando existe un plan válido?** Sí:
- L2, L4, L6 y L8 tienen plan las 10 semanas;
- en L6 y L8, P-03 publica tras el dolor y el ciclo sigue;
- **las dosis son las base aprobadas**;
- **ningún ejercicio excluido reaparece** (columna `excluidos_en_plan` = 0 en las 180 filas);
- P-03 sigue comprobando que la semana regenerada no sube el trabajo de la semana medida.

**¿Qué sigue siendo NO_FACTIBLE?** L7 en S4, con cualquier política. P-03 rechaza porque la regeneración con dosis base **sube el trabajo** de la semana medida: los segundos por duración pasan de 1.620 a 1.680, aunque las repeticiones bajan de 841 a 818. Es la regla de no añadir trabajo ante dolor, no V20: C2-D no actúa y no debe actuar.

**¿Las sesiones publicadas por C2-D son adecuadas?** **No demostrado.** L8 tras el dolor queda al 34,7 % (unos 26 de 75 minutos, PUSH con 4 ejercicios). Es técnicamente válido y saturado, pero hay que revisarlo profesionalmente (anexo A05). **No se declara adecuado.**

## 3. Progresión, D8, ACORTAR, reincorporación e idempotencia

| Aspecto | Resultado |
|---|---|
| Progresión (encendida) | 0 subidas por encima del 5 % por paso ni del techo del 10 % en las semanas derivadas. L1: 630 → 633 → 636 repeticiones en semanas completas. Sin subidas tras dolor (S3), fatiga (S5) ni baja adherencia (S6) |
| C2-D y la dosis | **No la sube.** Los planes de L2, L4 y L8 conservan series y repeticiones; C2-D solo decide la publicación |
| L7 (S5) | Generación **nueva** tras una semana sin plan: repeticiones 841 → 790 y segundos 1.620 → 1.800. Es una **composición nueva** con las exclusiones de P-02 (base nueva, D-P5), **no un aumento de volumen** por progresión. Ocurre igual con la histórica |
| ACORTAR (S4, falta de tiempo habitual M/2) | Con la IA desactivada se registra `ACORTAR_O_REORGANIZAR` y la sesión se conserva por RESPALDO. El recorte efectivo con IA simulada lo cubren las baterías existentes (D8, C-2), en la regresión completa |
| D8 por fatiga (S5, RPE 9) | Una sola semana con RPE 9 no generó orden de reducción (SIN_ORDEN). D8 lo cubren las baterías existentes |
| Reincorporación (S7) | Reactivación explícita de una exclusión: las exclusiones activas bajan (7 → 6, 8 → 7). El ejercicio vuelve a ser **candidato**, no se impone. Ningún ejercicio aún excluido aparece |
| Idempotencia | Repetir cada ciclo no publica otro plan: `idempotente = true` y como mucho 1 plan activo por semana en las 180 filas |
| Linaje y traza | Cada plan lleva `planning_policy` y `contexto_traza` (GENERACION, ADAPTACION o REGENERACION_P03) |

**No ejercitado aquí:**
- concurrencia real, con dos hilos sobre la misma medición (cubierta por la unicidad de P-03 en V43 y sus IT);
- borradores de IA simulados en el ciclo (V4-C con borradores de IA se prueba con el validador real en `V4CValidadorRealTest`).
