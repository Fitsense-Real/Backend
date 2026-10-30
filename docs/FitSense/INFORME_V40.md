# V40 · Cierre de C4/C6 (primera fase)

**Fecha:** 30/10/2026. **Rama:** `c4-c6`. **PR:** [#8](https://github.com/Fitsense-Real/Backend/pull/8), hacia `generation`.

**Base:** decisión del responsable sobre `DIAGNOSTICO_C4_C6.md`. La evidencia visual es una revisión asistida por IA. **No hay doble revisión humana.**

## 1. Correcciones aplicadas (`V40__catalog_c4_c6.sql`)

| Ej. | Cambio | Efecto en el motor |
|---|---|---|
| 1225 smith hip raise | `HIP_EXTENSION` → `TRUNK_FLEXION` | Deja de acreditar LOWER_HIP. Pasa de ESTRUCTURAL a RESISTIDO |
| 1223 smith incline shoulder raises | `HORIZONTAL_PULL` → `SHOULDER_RAISE` | Deja de acreditar UPPER_PULL. Pasa de ESTRUCTURAL a RESISTIDO |
| 439 cable squat row | `back`/`lats` → `upper legs`/`quads`. Sigue en `KNEE_DOMINANT` | Sigue en LOWER_KNEE, ahora con su grupo coherente |
| 959 kettlebell figure 8 | `HIP_HINGE` → `TRUNK_ROTATION_LATERAL_FLEXION` | Deja de acreditar LOWER_HIP. Pasa a RESISTIDO |
| 962 kettlebell sumo high pull | `back`/`traps` → `upper legs`/`glutes`. Sigue en `KNEE_DOMINANT` | `HIP_HINGE` queda documentado como pendiente |
| 927 kettlebell bent press | `VERTICAL_PUSH` → `TRUNK_ROTATION_LATERAL_FLEXION` | Deja de acreditar UPPER_PUSH. Pasa a RESISTIDO |
| 953 kettlebell two arm clean | `shoulders`/`delts` → `upper legs`/`glutes`. Sigue en `HIP_HINGE` | Sigue en LOWER_HIP, ahora con su grupo coherente |

**Comprobación previa de 927.** Se revisaron 16 fotogramas de la fase de subida:
- la kettlebell sube en vertical mientras el tronco se inclina de lado por debajo;
- el brazo llega arriba porque el cuerpo se aparta, no por un press activo;
- en todos los fotogramas se resaltan oblicuos y abdomen, nunca el deltoides.

El movimiento predomina en el tronco. No se dejó como excepción.

**Sin cambios:**
- dificultad, equipamiento y dosis;
- `exercise_difficulty_approvals`;
- V38 y V39;
- 674 y 162.

## 2. Estado de V40

- La numeración de Flyway era V39, así que la migración es la V40.
- Comprueba el estado previo y el final, y aborta sin cambios si encuentra algo inesperado.
- **No se ha aplicado en ningún entorno persistente.**

## 3. Pruebas dirigidas

| Prueba | Resultado |
|---|---|
| `CatalogoV40IT` (3): atributos; clase y slot; en 8 perfiles generados, ningún ejercicio de tronco acredita un slot de fuerza | 3/3 |
| `CoherenciaGrupoSlotIT`: advierte de los 18 cruces registrados que siguen abiertos; falla solo ante uno nuevo. Resueltos respecto a V39: 1225, 439, 927, 953, 959 y 962 | Pasa |
| `GeneracionReferenciaV39IT`: el motor reproduce la referencia V39 congelada | Pasa |
| `GeneracionReferenciaIT`, con la referencia V40 regenerada | Pasa |
| `ReferenciasHistoricasInmutablesTest`, con V39 añadida | Pasa |
| `CatalogoV39IT`, `ComposicionE1E2IT`, `V15CambioDeGrupoIT`, `V20ComposicionCasosIT`, `GeneracionPorObjetivoIT`, `LimitacionesPlanIT`, `CatalogoElegibleIT` | Pasan |
| Adaptación y progresión: `AdaptacionRegresionIT`, `AdaptacionCompletaIT`, `ProgresionIT`, `ProgresionCienPorCienIT`, `PendientesProgresionIT` | Pasan, con un ajuste explicado abajo |

**`PendientesProgresionIT.serieNoRecuperableYProgresionDeOtros` falló con V40.** La causa está diagnosticada con trazas, que después se retiraron:

1. La prueba exige dos cosas a la vez:
   - devolver la serie que quitó la IA supera el 10 % de la sesión;
   - +1 repetición de **otro** ejercicio cabe en el 5 % (`progression_increase_pct`).
2. Con dos ejercicios 3×6, como en LOSE_WEIGHT intermedio de 45 min, es imposible: 6/54 > 10 % obliga a que 3/54 = 5,6 %, que supera el 5 %.
3. Con V39 se cumplía solo porque el viernes llevaba 1225, el abdominal mal clasificado como fuerza de cadera, a 3×10.

La regla actúa correctamente y no se ha tocado. Se cambió únicamente el perfil del escenario a GENERAL_FITNESS intermedio en casa de 30 min, que cumple la premisa (16,8 % y 2,8 %). Las aserciones son las mismas.

## 4. CI

Suite completa (`mvn verify -Pintegration`) en GitHub Actions sobre el último commit del PR #8. No se duplicó en local. El resultado y el commit verificado constan en el PR.

## 5. Simulaciones finales (una vez, sobre `4f8754d`)

| | Resultado |
|---|---|
| 72 referencias | 72/72, idénticas a la oficial |
| Barrido de 72 | 72/72 válidos; capacidades COMPLETO en 72/72 |
| Robustez | 288/288 |
| Continuidad (96) | Sin fallos |
| Restricciones (192) | 192/192 publicados; 0 con implemento ausente; 0 violaciones |

Barrido, robustez y restricciones son **idénticos byte a byte** a la simulación exploratoria «combinada + 953». Evidencia: `evidencia/v40/final/`.

## 6. Diferencias de referencias

`evidencia/v40/DIFERENCIAS_REFERENCIAS_V40.md`:
- **Cambian 22 de 72 perfiles**, todos de gimnasio intermedio o avanzado. El número coincide con la simulación.
- **Ejercicios de tronco que acreditan un slot de fuerza: de 52 a 0.**
- Slots de pierna acreditados por un ejercicio que no es de pierna: de 70 a 11, todos de 262.
- Entran puentes de glúteo, remos reales y sentadillas.
- Las capacidades obligatorias se mantienen y las comprobaciones de equipamiento, dificultad, impacto y duración dan 0 problemas.

## 7. PR y fusión

Ver el PR #8: el commit verificado y el de fusión constan allí.
- Fusionar no despliega nada: el CI no tiene secretos y Railway no despliega desde `generation`.
- **Advertencia:** arrancar el backend a mano contra Neon aplicaría V38, V39 y V40. Para evitarlo: `SPRING_FLYWAY_TARGET=37`.

## 8. Pendientes reales de C4/C6

C4/C6 **no está cerrado**.

- **262 *barbell skier*** (`shoulders`/`HIP_HINGE`):
  - entra 11 veces en las referencias V40, como fuerza de cadera, en el FULL_BODY de avanzados de gimnasio;
  - en el GIF el tronco está quieto y la barra se balancea con los hombros: **no hay bisagra**;
  - **sí está acreditando LOWER_HIP de forma indebida**;
  - no se ha modificado. Es el primer candidato de la siguiente fase (probable `SHOULDER_EXTENSION`).
- **Los otros 17 cruces registrados:** 54, 162, 267, 301, 302, 671, 674, 796, 824, 862, 922, 930, 952, 971, 1086, 1174 y 1295.
- **674 y 162:** se decidirán con sus familias (arrancadas y cargadas).
- **301 y 302:** se evaluarán de forma independiente; la decisión de 439 no se les aplica.
