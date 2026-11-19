# Cierre del PR #18: resumen ejecutivo

## Estado final

| | Estado |
|---|---|
| **PR #18** (Fitsense-Real/Backend#18, `feature/v4c-versionado-aislado`, borrador hacia `generation`) | Trazabilidad corregida (a52e35e), pruebas verdes, generación histórica intacta y CI verde. **Listo para tu aprobación de fusión. No fusionado** |
| **PR #17** (Fitsense-Real/Backend#17, borrador) | Sin modificaciones en esta orden. Paquete profesional completo y sin enviar. Pendiente de la evaluación profesional y del trabajo móvil |

**No se ha hecho nada de lo siguiente:**
- ninguna fusión, ningún despliegue y ninguna migración en Neon o Railway;
- ninguna política activada;
- ninguna llamada a Replicate;
- ninguna evaluación registrada.

## Beneficio real de V4-C (corregido)

- **El caso del 142 % ya se rechazaba** en el pipeline real. El normalizador de duración (B2) reescribe los minutos que declara la IA con los calculados, así que la V4 histórica lo rechaza («dura 45 minutos y el techo es 35»). No es un defecto comprobado del pipeline real.
- **V4-C corrige principalmente la brecha de redondeo.** La histórica admite hasta `(round(M × 1,15) + 0,5) × 60 − 1` segundos:

| M | Admite la histórica | Límite exacto de V4-C |
|---|---|---|
| 30 | 2.129 s (118,3 %) | 2.070 s |
| 45 | 3.149 s | 3.105 s |
| 60 | 4.169 s | 4.140 s |
| 75 | 5.189 s | 5.175 s |

- **Añade defensa en profundidad:** no depende de que todo camino futuro normalice los minutos.
- **Comunica el límite exacto en segundos a la IA** (`max_session_seconds`).

## Entregables (`docs/FitSense/cierre_v4c_pr18/`)

1. `DIAGNOSTICO_TRAZA_V4C.md`
2. `CORRECCION_TRAZABILIDAD.md`
3. `PRUEBAS_TRAZA_V4C.md`
4. `REGRESION_FINAL_PR18.md`
5. `COMPARACION_HISTORICA_PR18.md`
6. `REVISION_DIFF_PR18.md`
7. `PREPARACION_EVALUACION_PROFESIONAL.md`
8. `DECISIONES_APROBACION.md`
9. `CIERRE_PR18.md`

## Informe final (15 preguntas)

**1. ¿La traza refleja exactamente el límite aplicado por V4-C?**
- **Sí.** `parametros.limite_v4_aplicado` usa la misma regla, el mismo tiempo base (minutos del perfil) y la misma condición que el validador, incluido el caso sin bloque `prescription`.
- `sesiones[].dentro_limite_v4_aplicado` coincide con la decisión del validador real en todos los casos probados: 30, 45, 60 y 75 min a ±1 s; ACORTAR; histórica; sin `prescription`.

**2. ¿Se distinguen los minutos configurados de los efectivos?**
- **Sí:** `minutos_perfil` y `minutos_efectivos`.
- `minutos_disponibles` y `limite_v4` conservan su significado (efectivos, informativo), y `limite_v4_minutos_base` lo hace explícito.
- **Ejemplo real** (perfil de 60 con ACORTAR a 30): 60 configurados, 30 efectivos y 4.140 s aplicados; 2.070 s solo informativo.

**3. ¿Se modificó el comportamiento del validador?**
- **No.** El único archivo de código cambiado es `PlanningPolicyTrace.java`.
- El validador, la fórmula, el techo, el tiempo base, los generadores y el contrato con la IA no cambian.
- **Único cambio explícito de valor:** sin `prescription`, `segundos_estimados` y `pct_de_m` pasan de 0 a `null`.

**4. ¿La política histórica conserva sus resultados?**
- **Sí:** 960/960 planes y 19.560/19.560 colocaciones idénticos a `generation`.
- Aceptación histórica verificada contra el validador.

**5. ¿V44 permanece idéntica?**
- **Sí:** SHA-256 `640b0169…e2708400`, igual en el PR #18 y en el PR #17.
- Sin migración nueva.

**6. ¿La rama aislada sigue sin C2-D ni EF?**
- **Sí:** 0 referencias a C2-D, EF, LW-PREF, REP-SUST y NOFACT en el diff.
- Sin V45, sin cambios en `pom.xml` ni en la configuración.

**7. ¿Cuántas pruebas se ejecutaron?** En la ejecución completa de a52e35e:
- **1.010 unitarias** y **433 de integración**, sin fallos;
- además, la batería con PLAN-V4-0.1: **223**;
- y la comparación de 960 perfiles: **1**.

**8. ¿Cuántas se omitieron?**
- **33** en la integración: laboratorios opt-in y pruebas con IA real, las mismas de antes.
- **2** en la batería.
- No se cuentan como aprobadas.

**9. ¿CI terminó correctamente en el commit final?**
- **Sí en 0931ee5**, que contiene el código final (a52e35e) más documentación.
- El commit final, solo de documentación, se comprueba al publicarlo: ver el estado en el PR.

**10. ¿Los 960 perfiles históricos siguen siendo idénticos?**
- **Sí**, línea a línea frente a `generation`.
- Además, son byte a byte iguales a los del commit anterior del PR #18.

**11. ¿El PR #18 está listo para revisión y posible fusión?**
- **Sí**, a falta de tu revisión y aprobación.
- Sigue en borrador hasta que decidas: puedes pasarlo a «listo para revisión» o fusionarlo.

**12. ¿Qué riesgos técnicos permanecen?**
- **(a) Fusión con el PR #17:** al actualizarlo, `PlanningPolicyTrace` debe recibir los campos nuevos. Las pruebas de traza que entran con la fusión lo detectarían.
- **(b) CHECK de V44:** admite versiones C2-D que el código del PR #18 no reconoce. Solo importa si una base hubiera ejecutado el PR #17; no ha ocurrido.
- **(c) Brecha de redondeo:** no afecta hoy a ningún plan del motor de reglas, porque los 960 son iguales con HIST y con V4-C. Su efecto real está en las propuestas de IA.
- **(d) Minutos efectivos:** el decisor actual no fija minutos efectivos. La distinción solo aparece con órdenes con `forcedSessionMinutes`.

**13. ¿Qué necesita autorización antes del despliegue?**
- Fusionar el PR #18.
- Desplegar: Flyway aplicará V44, dos columnas NULL.
- Activar PLAN-V4-0.1 (`policy` más `allow-candidates`), después de un despliegue con PLAN-HIST-1.

**14. ¿El paquete profesional del PR #17 está preparado?**
- **Sí:** 4 archivos, estructura verificada y sin la clave.
- La guía de entrega y de análisis está en `PREPARACION_EVALUACION_PROFESIONAL.md`.
- **Faltan:** elegir a los evaluadores y aprobar los criterios A1–A6 **antes** de enviarlo.

**15. ¿Qué falta antes de activar C2-D + EF?**
- La evaluación profesional, con los criterios aprobados antes de recibirla.
- La decisión sobre los perfiles de peso corporal sin tracción (D01, D02 y G3).
- La lista de equipamiento.
- La actualización del PR #17 tras el PR #18 y su regresión.
- El trabajo en Flutter: duración, NO_FACTIBLE y equipamiento.
- Una aprobación por componente.
