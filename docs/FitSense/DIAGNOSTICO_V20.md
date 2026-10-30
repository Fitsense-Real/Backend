# V20 · Diagnóstico: ¿redondeo o composición? (solo diagnóstico)

**Fecha:** 30/10/2026 · **Rama:** `v20-duracion`, desde `generation` `e14da35` (V37 integrada).

**Alcance:** solo diagnóstico.
- No se modifica V20, V15, V23, las políticas de capacidades, Replicate ni ningún código de producción.
- No se aplica V37.1 en ninguna base real. Aquí se carga solo en bases Testcontainers, mediante `spring.flyway.locations`.

**Pregunta:** con V37.1, LOSE_WEIGHT principiante en casa, con 4 y 5 días, se queda sin plan por «unos 1 minutos». ¿Lo causa el redondeo del validador o la composición del motor de respaldo?

**Prueba:** `DiagnosticoV20ComposicionIT` (opt-in: `-Dfitsense.v20.diagnostico.dir=…`).
- Perfiles: LOSE_WEIGHT principiante, de 3 a 6 días, 45 minutos, en casa y en gimnasio.
- Para cada sesión del motor de reglas registra:
  - los segundos reales del estimador, antes de redondear;
  - el piso entero de V20 (31 minutos) frente al 70 % exacto (1.890 s);
  - el repertorio libre del enfoque.
- Evidencia: `docs/FitSense/evidencia/v20/{v37,v37b}/`.

## 1. Resultado

| Catálogo | Sesiones que fallan V20 | Segundos | % real | Clasificación |
|---|---:|---:|---:|---|
| V37 (vigente) | 0 | — | — | — |
| V37 + V37.1 | **5**: tren inferior en casa, 3, 4 y 5 días | 1.824 | **67,6 %** | **COMPOSICIÓN** |

**Las sesiones que fallan no llegan ni al 70 % exacto** (1.890 s). Están por debajo incluso del umbral permisivo actual (30:30). El redondeo no las rechaza «por un minuto mal contado»: les faltan de verdad 66 segundos de trabajo.

**El defecto de redondeo existe, pero va en el sentido contrario.** Es permisivo: piso truncado a 31 y duración redondeada al minuto. Hoy hace pasar sesiones por debajo del 70 %:
- con V37 vigente, **10 sesiones** de LOSE_WEIGHT principiante pasan V20 solo por redondeo (1.830–1.836 s, entre el 67,8 % y el 68 %);
- con V37.1, 10 más.

## 2. Por qué el motor se queda corto (composición)

Sesión de tren inferior, casa, 4 días, con V37.1:
- 1073 (upper legs, `HIP_EXTENSION`), 3×8;
- 1102 (waist, `KNEE_DOMINANT`), 3×10;
- 507 (upper legs, `KNEE_DOMINANT`), 3×8;
- 1054 (cardio), 3×90 s.

Son 4 colocaciones y 1.824 s. **Quedan 117 ejercicios de trabajo elegibles del enfoque sin usar:** 30 estructurales de upper legs, 50 resistidos de waist, 12 resistidos de upper legs y 5 de lower legs.

Cadena que lo bloquea, en `CapabilityWeekBuilder` y `ExerciseSelector`:
1. **LOSE_WEIGHT solo rellena con STRENGTH estructural.** La principal es CARDIO y la secundaria es STRENGTH, que exige ejercicio estructural (filtro `ESTRUCTURAL`). Ninguna capacidad de LOSE_WEIGHT puede prescribir los 55 ejercicios resistidos de waist o lower legs disponibles.
2. **Los estructurales del tren inferior son casi todos de upper legs:** 30 de 31.
3. **La regla de la mitad de V15**, que el selector respeta (`halfCap`: sesión de 4, con 3 o más grupos en el enfoque, tope 2), impide un tercer upper legs.
4. **Las series ya están en el máximo de principiante** (3), así que `ampliarSeries` no puede subir más.
5. **No queda otro cardio complementario utilizable ese día:** V18 impide repetir los del día anterior. Es probable; falta confirmarlo con una traza.

**Por qué aparece con V37.1 y no con V37:**
- V37.1 reclasifica **1103** de waist a upper legs (bloque B1).
- V37.1 retira, en casa, los ejercicios que exigen banco o step.
- En V37 la sesión tenía 1103 contado como «waist», lo que dejaba sitio a un tercer ejercicio de pierna sin superar la mitad.
- Es decir: **un error de clasificación de V36/V37 tapaba el defecto de composición.**

## 3. Conclusiones

1. **La causa es la composición, no el redondeo.** Corregir solo el redondeo (exigir el 70 % exacto) **no** arreglaría estos perfiles y dejaría sin plan otras 10 sesiones que hoy pasan por redondeo.
2. **El defecto de composición es específico de LOSE_WEIGHT.** Hay repertorio seguro y con el implemento disponible (55 resistidos de waist y lower legs), pero la política de capacidades no ofrece una dosis con que prescribirlo como relleno.
3. **El redondeo permisivo es un segundo defecto, independiente.** Conviene corregirlo **después** de la composición; si no, aumentan los perfiles sin plan.

## 4. Opciones (sin aplicar; decisión del responsable)

| Opción | Qué toca | Efecto esperado | Riesgo |
|---|---|---|---|
| A. Relleno de LOSE_WEIGHT con trabajo resistido de grupos pequeños (waist, lower legs) cuando el estructural choca con V15 | Política de capacidades de LOSE_WEIGHT (dosis para RESISTIDO): **requiere autorización** | Completa la sesión con ejercicios seguros y con implemento | Cambia la composición de LOSE_WEIGHT; hay que repetir referencias y simulación |
| B. Un segundo complemento de cardio (la principal de LOSE_WEIGHT) en el día corto | `CapabilityWeekBuilder`: no cambia la política | Coherente con el objetivo principal | Depende de que haya cardio no usado el día anterior (V18); en casa el repertorio de cardio es pequeño |
| C. Solo corregir el redondeo (70 % exacto) | `PlanDraftValidator` (V20) | **Empeora:** más sesiones sin plan | No resuelve la causa |
| D. Corregir el redondeo **después** de A o B | V20 | Exige de verdad el 70 % | Hay que medir el impacto en el barrido y la robustez |

**No se propone:**
- bajar el porcentaje mínimo;
- usar ejercicios sin el implemento;
- relajar V15;
- inflar descansos.

## 5. Siguiente paso propuesto

1. Confirmar el punto 5 de §2 (cardio disponible y bloqueado por V18) con una traza del constructor.
2. Prototipar B, y A si se autoriza, en esta rama. Medir con el barrido de 4 a 6 días, la robustez, la continuidad y los 192 perfiles con restricciones, con V37 y con V37 + V37.1.
3. Después, corregir el redondeo de V20 y volver a medir.
