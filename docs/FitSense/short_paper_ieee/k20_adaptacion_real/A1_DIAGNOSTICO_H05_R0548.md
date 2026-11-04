# A1 · Diagnóstico de H05 y R0548 (generación inicial de GAIN_MUSCLE por encima de 60 min)

**Alcance de la autorización (C-7):** investigar y proponer, **sin alterar V20 ni el catálogo y sin integrar en silencio**.

El prototipo vive en una rama de laboratorio aparte, **`lab/k20-gm-tope-derivado`** (`720d058`, evidencia en `7605e30`). **No está en la candidata** `eval/final-ieee-k20-v2`.

## 1. Síntoma

| Caso | Perfil | Resultado |
|---|---|---|
| R0548 | GM intermedio, casa (peso corporal, mancuernas y bandas), 7 días, 75 min | `PlanGenerationFailedException`: V20, 2.550 s frente a 3.150 s |
| H05 (reservado) | GM avanzado, casa (peso corporal, mancuernas y bandas), 4 días, 75 min | Igual: V20, 2.550–2.658 s frente a 3.150 s en todas las sesiones |

## 2. Causa raíz (demostrada)

**No es el equipamiento ni el catálogo. Es una incoherencia entre dos reglas vigentes:**
- **4D v3.1** (`MvpDosePolicy.maxColocaciones`): tope de **7 colocaciones** desde 60 min, sin ningún escalón por encima.
- **Dosis de 4D:** fija en 3×10 con 90 s.

El contenido máximo de una sesión es:

300 s de calentamiento + 7 × (3 × 10 × 3 s + 2 × 90 s) + 6 × 60 s de transición = **2.550 s**

Con la serie adicional de 4D v3.2/v3.3 llega a unos 2.604 s.

**V20** exige ⌈M × 60 × 0,70⌉, que es **2.562 s con 61 min** y **3.150 s con 75 min**. Por tanto, **ninguna sesión de GAIN_MUSCLE de 61 a 75 min puede cumplir V20, sea cual sea el equipamiento.**

**Cómo se llegó aquí:**
- La adenda `ADENDA_4D_v3_1.md` subió el tope de 6 a 7 colocaciones **solo para 30, 45 y 60 min**, que eran los minutos de entonces.
- Después, P-01 admitió **cualquier valor de 30 a 75** para todos los objetivos.
- 4D no se extendió. Es un **defecto de cobertura entre versiones**, no un caso raro.

**Comprobación en la matriz de 960 perfiles** (candidata actual):
- **GAIN_MUSCLE de 75 min: 0 de 60 perfiles válidos**: gimnasio 0/24, casa con material 0/18, peso corporal 0/18.
- La duración por sesión es la misma que con 60 min: 2.540–2.604 s.
- Ya figuraban en el hash de referencia de los 960 perfiles, que prueba identidad y no validez.

## 3. Otros fallos de generación de la matriz (otra causa; fuera de C-7)

De 85 perfiles inválidos de 960, 60 son esta causa. **Los otros 25 son de catálogo o composición en perfiles extremos:**
- V15 con LOSE_WEIGHT de 1 día y 45 min: 6.
- Principiantes de 75 min en GENERAL_FITNESS (3) y LOSE_WEIGHT (4).
- Peso corporal con 6–7 días: GM a 45/60 min (8) e INCREASE_STRENGTH a 75 min (4).

Ninguno de ellos está en U01–U20 ni en H01–H16. Quedan registrados como limitación y no se tocan.

## 4. Alternativas

| Opción | Qué cambia | ¿Toca V20 o el catálogo? | ¿Debilita algo? | Valoración |
|---|---|---|---|---|
| **A. 4D v3.4: tope de colocaciones derivado del piso de V20** por encima de 60 min: el mínimo número de colocaciones de 3×10/90 s que alcanza el 70 %. Da 7 con 60 min (sin cambios), **8 con 61–68** y **9 con 69–75** | `MvpDosePolicy.maxColocaciones` y el tope del motor de reglas para GM | No | No: la dosis, el descanso, el presupuesto del 85 %, V20, V24 y V25 se mantienen. V24 sigue exigiendo el tope, ahora coherente con V20 | **Recomendada.** Continúa exactamente la lógica de 4D v3.1, que derivó el 7 del mismo cálculo |
| B. Serie adicional (4×10) también en colocaciones estructurales cuando falta tiempo | Tabla de dosis de 4D y el repaso del motor | No | Cambia la dosis de hipertrofia | Más compleja y más invasiva en la prescripción |
| C. Limitar a GAIN_MUSCLE a 60 min en el perfil (P-01) | Validación del perfil | No | Rechaza una entrada que hoy se admite; H05 y R0548 no podrían crearse | No resuelve la generación; cambia el producto |
| D. Generar 60 min para un perfil de 75 | Minutos efectivos | **Sí**: el piso de V20 se calcularía sobre 60 | **Sí** | **Descartada** |
| E. Rellenar con descanso | Descanso | No | V20 lo prohíbe expresamente («no descanso») | **Descartada** |

## 5. Prototipo de la opción A (laboratorio, medido)

`lab/k20-gm-tope-derivado`, con 2 ficheros de `src/main`:
- `MvpDosePolicy.maxColocaciones` deriva el tope por encima de 60 min.
- El motor de reglas deja que, en GAIN_MUSCLE, el tope de 4D supere su límite genérico de 8.

| Comprobación | Resultado |
|---|---|
| Unitarias | 1.210/1.210 |
| Integración de hipertrofia, V20, respaldo de 60 min y duración forzada (`Plan60HipertrofiaIT`, `RespaldoSesentaMinutosIT`, `GeneracionPorObjetivoIT`, `DuracionForzadaIT`, `V20*IT`) | 34/34 |
| Regresión completa | 1.210/0/0 unitarias · 480/0/42 integración (§8) |
| **Matriz de 960** (las dos políticas, 1.920 filas) | **Solo cambian las 120 filas de GAIN_MUSCLE de 75 min**: 112 pasan de inválidas a válidas y 8 siguen inválidas (los 4 perfiles de peso corporal con 6–7 días, en las dos políticas, la misma familia de §3).<br>**Ninguna otra fila cambia**, ni en `planes.csv` ni en `colocaciones.csv`.<br>PLAN-HIST-1: de **875 a 931** válidos. Duración por sesión de GM a 75 min: 3.210–3.219 s (piso 3.150; techo V4 86 min) |
| REPLAY de 100 semanas | **Idéntica** a la de la candidata |
| R0548 | **Genera un plan válido**. `GeneracionR0548V20IT`, que afirmaba el fallo, falla ahora por la razón correcta |
| H01–H16 sin IA (oráculo v2) | **16/16 PASS**: H05 pasa |
| U01–U20 sin IA | Huella **idéntica** (`f9f67973…`): sus perfiles GM son de 60 min y el tope no cambia |

**Evidencia:** `evidencia/laboratorio_gm/` en la rama de laboratorio:
- `960/comparacion_960_tope_derivado.csv` (las 120 filas);
- `960/resumen_prototipo.txt`;
- `replay_prototipo.diff`;
- `LAB1_D_H`;
- `LAB2_D_U`;
- `r0548_prototipo_maven.txt`.

## 6. Recomendación

1. **Adoptar la opción A como «4D v3.4»** en la candidata, con:
   - una adenda `ADENDA_4D_v3_4.md` que recoja el cálculo y la procedencia, como la v3.1;
   - una prueba unitaria del tope por minutos (60 → 7; 61 y 68 → 8; 69 y 75 → 9);
   - una prueba de integración de generación GM de 75 min en gimnasio y en casa.
2. **Convertir `GeneracionR0548V20IT`** en una prueba de éxito y **conservar el registro del fallo anterior**: su historia y este documento.
3. **No tocar** V20, V24, V25, la dosis, el descanso ni el catálogo.
4. Los 25 + 4 perfiles de §3 quedan como **limitación conocida**.

## 7. Exposición de los casos reservados

El fallo se **detectó** al ejecutar H05 sin IA. La causa es general (60/60 en la matriz, y R0548 ya registrado). La corrección no usa nada propio de H05: depende solo de los minutos y de reglas existentes. Aun así, según tu criterio, **H01–H16 quedan expuestos**: se usaron para descubrir un defecto que se corrige.

**Propuesta:**
- **Mantener H01–H16** como cohorte de **validación de desarrollo**, informada como tal, con el fallo de H05 antes de la corrección registrado.
- **Generar una cohorte nueva J01–J16 no ejecutada** para la evaluación independiente:
  - misma estructura de causas;
  - rotación distinta, decidida antes de verla;
  - minutos que incluyan 61–75 en todos los objetivos.

  **No se ejecutará nada sobre J** (ni D ni la IA simulada) hasta la evaluación congelada.

## 8. Regresión completa del prototipo

`mvnw verify -Pintegration` sobre `720d058`, con la configuración por defecto:

| Bloque | Resultado |
|---|---|
| Unitarias | **1.210 aprobadas, 0 fallidas, 0 omitidas** |
| Integración | **522 ejecutadas: 480 aprobadas, 0 fallidas, 42 omitidas** (opt-in; las omitidas no cuentan como superadas) |

Son los mismos totales que en la candidata sin el prototipo. Ninguna prueba existente exigía el tope de 7 por encima de 60 min, así que ninguna se ha modificado.
