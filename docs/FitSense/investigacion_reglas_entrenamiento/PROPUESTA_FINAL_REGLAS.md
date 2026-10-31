# Propuesta final: reglas de ejercicios, duración y volumen (para tu decisión)

**No se ha implementado nada en producción.** Esta propuesta se apoya en:
- `REVISION_CIENTIFICA_REGLAS.md`: evidencia. Verificada por resúmenes y fichas bibliográficas; no se pudo leer el texto completo de ningún artículo por restricciones de red.
- `MATRIZ_EXPERIMENTOS.csv` y `SIMULACIONES_LONGITUDINALES.csv`: experimentos del backend.

## Conclusión en una frase

FitSense controla hoy la dosis con dos números sin base científica:
- un **tope de ejercicios** (6/7/8);
- un **piso de tiempo** (70 %).

Entre los dos producen tres efectos:
- semanas imposibles (GAIN_MUSCLE a 66–75 min);
- relleno redundante (hasta 21 series de un grupo en una sesión);
- ningún control de la variable que la evidencia sí mide: las **series por grupo muscular y semana**.

**El modelo más defendible es el combinado (E), con dos correcciones que los experimentos han mostrado necesarias:**
- un objetivo semanal que escale con los días disponibles;
- un techo por sesión.

## Respuestas

**1. ¿Debe seguir existiendo un máximo fijo de ejercicios?**
- **No como control de dosis ni de seguridad.** Ninguna guía lo establece; el «8–10» histórico era un mínimo.
- **Sí como protección práctica por sesión**, pero derivada de lo que cabe y de la dosis, no universal. Los experimentos con 1–2 días muestran que sin ningún techo por sesión aparecen sesiones de 13–21 ejercicios.

**2. Si existe, ¿de cuánto?**
- La evidencia no da un número. Propuesta como **decisión de ingeniería, documentada como tal**:
  - techo blando de **~10 series por grupo muscular y sesión** (Remmert 2025, preprint: ~11 series fraccionales);
  - y el **techo de tiempo de V4** (M × 1,15).
- Con eso, el número de ejercicios queda acotado por construcción. Si se quiere un tope de UX explícito, que sea configurable y no forme parte de la validación de dosis.

**3. Si no hay máximo fijo, ¿qué lo reemplaza?**
- Series por grupo y **semana**:
  - mínimo y máximo por objetivo y nivel, escalado con los días disponibles;
  - p. ej. GAIN_MUSCLE ≥ 10 cuando la frecuencia lo permite y ≥ 4 como mínimo absoluto.
- Series por grupo y **sesión**: techo blando de ~10.
- **Minutos aeróbicos semanales** en LOSE_WEIGHT y GENERAL_FITNESS, frente a OMS 150–300 moderado o 75–150 vigoroso. Hoy dan 27–40 min.
- Se conservan:
  - V4 (techo de tiempo);
  - recuperación (V18, días pegados);
  - patrones (V23) y capacidades (V25);
  - P-02 y P-03;
  - los límites de progresión (carga +2–10 %).

**4. ¿Qué parte de 4D conservar?**
- **Conservar:**
  - las dosis por capacidad (3×10/90 s, 3×13 en zona pequeña, resistencia 3×18/45 s), coherentes con ACSM 2009 y Singer 2024;
  - no alargar descansos para rellenar (descanso máximo 180 s);
  - la evidencia de hipertrofia por colocación.
- **Revisar:**
  - el descanso de fuerza (150 s) para intermedios y avanzados (ACSM 2009: 3–5 min);
  - el tope 6/7;
  - el presupuesto del 85 %, sin origen auditable.

**5. ¿Tiene sentido V20 con un mínimo del 70 %?**
- **No como regla de contenido.** Nació para la coherencia de lo declarado (V12, «sin calibrar»). Hoy impide planes, como GAIN_MUSCLE a 75 min, y obliga a rellenar.
- **Pero no debe eliminarse sin sustituto:** sin piso, el motor infradosifica (LOSE_WEIGHT y GENERAL_FITNESS con 0 series en el grupo grande menos cubierto).
- **Propuesta:** sustituirla por el mínimo de contenido del punto 3, y mantener la coherencia entre la duración declarada y la calculada.

**6. ¿Qué relación debe haber entre la duración configurada y el contenido real?**
- La duración configurada es **disponibilidad (un techo)**, no una obligación.
- La sesión debe contener **la dosis que toca esa semana**. Si cabe en menos tiempo, se declara lo que dura de verdad, como ya hace el estimador.
- Si **no cabe** en el techo, el sistema debe decirlo: por ejemplo, «con 1 día y 30 min no se alcanza el volumen de hipertrofia; se prescribe el mínimo eficaz». No debe forzar sesiones de 115 %.

**7. ¿Qué modelo es más defendible científicamente?** E, el combinado:
- tiempo como techo;
- dosis semanal por grupo, escalada con la frecuencia;
- techo por sesión y grupo;
- dosis por capacidad de 4D;
- cardio semanal.

**8. ¿Qué mejora la personalización sin comprometer la seguridad?**
- Objetivos semanales por nivel: principiantes en la parte baja (≥ 4–10), avanzados en 12–20 (Baz-Valle 2022).
- Escalar por días disponibles.
- Descanso de fuerza según el nivel.
- Minutos aeróbicos según el objetivo.
- Mantener P-02, P-03, V18 y el techo de V4.

**9. ¿Qué pruebas hacen falta antes de implementar?** Ver `RIESGOS_Y_REGRESIONES.md`:
- versionado de la regla;
- matriz en el validador real;
- conservación de los 830 planes válidos, o una lista justificada de los que cambian;
- campaña de 100 semanas;
- longitudinal con progresión y reincorporación;
- contrato de la IA;
- revisión por un profesional.

**10. Riesgos e incertidumbres que permanecen.**
- La evidencia se verificó por resúmenes, no por textos completos.
- El techo por sesión procede de un **preprint**.
- Las 48 h de recuperación por grupo no aparecen en las guías primarias: son una regla de producto.
- Los usuarios sintéticos no dicen nada sobre adherencia real.
- El estimador (3 s por repetición, 60 s de transición, 5 min de calentamiento) está **sin calibrar**: cualquier regla basada en tiempo hereda ese error.
- E exige cambios grandes en el motor, el validador y la IA.

## Decisiones que necesito de ti

1. **¿Sustituir el piso de V20 por un mínimo de contenido** (series por semana y minutos aeróbicos)? ¿O conservarlo temporalmente con otro porcentaje? El 50 % resuelve casi todo GAIN_MUSCLE a 75 min (53 de 57), pero sigue sin base.
2. **¿Sustituir el tope fijo 6/7/8** por un techo de series por grupo y sesión, más el techo de tiempo de V4? ¿O mantenerlo como límite de UX configurable?
3. **¿Añadir objetivos de volumen semanal** por objetivo y nivel, escalados por días? Y valores iniciales a validar por un profesional:
   - GAIN_MUSCLE 10–20;
   - INCREASE_STRENGTH ≥ 4–6 de fuerza por patrón;
   - LOSE_WEIGHT y GENERAL_FITNESS ≥ 4 y minutos aeróbicos.
4. **Mientras tanto, para GAIN_MUSCLE a 66–75 min** (perfiles congelados sin plan), elegir una opción provisional:
   - bajar el piso solo para ese caso;
   - limitar P-01 de GAIN_MUSCLE a 65;
   - esperar a la política nueva.

Cada opción es una política nueva y requiere tu aprobación.
