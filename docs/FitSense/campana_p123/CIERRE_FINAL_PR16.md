# Cierre final del PR #16: factibilidad de 30–120 minutos y revisión final de V43

Rama `politicas-p123`. Base: `12555f6`, CI verde. **No se fusiona.**

Documentos:
- `INVESTIGACION_FACTIBILIDAD_30_120.md`: método, resultados, perfil congelado.
- `MATRIZ_FACTIBILIDAD_30_120.csv`: 1.236 combinaciones.
- `SEMILLAS_30_120.csv`: robustez de la semilla.
- `CORRECCIONES_GENERADOR_REGLAS.md`: cuatro defectos corregidos y opciones para los pendientes.
- `REVISION_FINAL_V43.md`.

## Qué se hizo

| Fase | Resultado |
|---|---|
| A · Verificación | Rama, commit y CI de `12555f6` verificados. Motor de reglas: `CapabilityWeekBuilder` y `RuleBasedTrainingPlanGenerator`. V20: `PlanDraftValidator.minimumSessionSeconds`. V4: `UserProfile.maxSessionMinutesAllowed`. 4D: `MvpDosePolicy`. Dosis por capacidad: `CapabilityPrescriptionPolicy` |
| B · Políticas | P-01 30–120, V4 15 %, V20 70 %, D8 y el perfil congelado, sin tocar |
| C · Reproducción | Matriz de 1.236 combinaciones con IDs y semillas reproducibles (`FactibilidadDuracionIT`) |
| D · Causa raíz | Cota superior independiente, cota con obligaciones semanales (búsqueda exhaustiva) y dos verificadores de existencia, constructivo y por búsqueda local, con el validador de producción como juez |
| E · Correcciones | 4 defectos del generador corregidos, con regresión que fallaba antes (`GeneradorReglasCasoAIT`, 10 casos) |
| F · Perfil congelado | Analizado; diseño propuesto, no implementado |
| G · V43 | Revisión directa del SQL; 2 pruebas de concurrencia nuevas |
| H · Pruebas | Batería completa verde; campaña re-ejecutada en REPLAY: planes idénticos |

## Respuestas

**1. ¿Por qué fallaban los entrenamientos de 90–120 minutos?**
- **Casi siempre, porque son imposibles con las reglas aprobadas.**
  - V20 pide el 70 % de los minutos.
  - Con el tope de colocaciones de V24 (7 en GAIN_MUSCLE, 8 en el resto) y las dosis aprobadas más largas, la sesión más larga posible es de 46 min en GAIN_MUSCLE, 53–59 en INCREASE_STRENGTH y 61–76 en LOSE_WEIGHT y GENERAL_FITNESS.
  - El piso de 120 min (5.040 s) no se alcanza con ningún objetivo.
- **En algunos casos, porque el motor no encontraba planes que existían:** LOSE_WEIGHT a 105 y GENERAL_FITNESS a 75–90.

**2. ¿Cuántas combinaciones se evaluaron?** **1.236**:
- base y expansión: 1.080;
- 7 días: 12;
- restricciones: 72;
- bloqueo de cardio: 72.

**3. ¿Cuántas produjeron planes válidos?** **639** con el motor corregido; **521** con el motor original.

**4. ¿Cuántas fallaron por V20?** Con el motor corregido, 597: 585 solo por V20 y 12 por V15 y V20. Con el original, 709.

**5. ¿Cuántas por 4D?** **0 rechazos directos** de V24/4D: el motor respeta su tope, sus dosis y su presupuesto. Pero 4D es la causa de que V20 no se alcance en GAIN_MUSCLE: 204 combinaciones CASO B por el tope de 7 colocaciones con 3x10/90 o 3x13/90 (como mucho 65 min de sesión con piso).

**6. ¿Cuántas por otros validadores?**
- V15: 12 con el motor corregido, siempre junto con V20 (30 con el original: 6 solas y 24 con V20).
- Ninguno más.

**7. ¿Existían planes factibles que el motor no construía?** **Sí, 126.**
- Ahora el motor construye 118.
- Quedan 8 (GENERAL_FITNESS, 4–6 días), cada uno con su semana testigo en la matriz.

**8. ¿Qué defectos se encontraron?** Los cuatro tienen detalle en `CORRECCIONES_GENERADOR_REGLAS.md`:
- **D-1.** El día lleno nunca cambiaba lo corto por lo largo.
- **D-2.** Cada día solo veía lo ya construido.
- **D-3.** La fuerza de los cuatro slots quedaba concentrada en un día.
- **D-4.** El recorte de 4D quitaba el segundo grupo del día LEGS.

**9. ¿Cuáles se corrigieron?** Los cuatro, con regresión.
- Los 521 planes válidos anteriores salen idénticos.
- La campaña de 100 semanas sale idéntica.

**10. ¿Qué combinaciones siguen siendo inviables?**
- **565 CASO B**, imposibles con las reglas vigentes: 527 por cálculo directo y 38 por las obligaciones de la semana. Por ejemplo:
  - GAIN_MUSCLE ≥ 75 min;
  - INCREASE_STRENGTH ≥ 90 min (principiante ≥ 77);
  - LOSE_WEIGHT principiante ≥ 90 y el resto a 120;
  - GENERAL_FITNESS intermedio/avanzado a 105 y 120.
- **13 CASO C**, por las restricciones de seguridad o el bloqueo del cardio, cuando con gimnasio libre sí cabrían.

**11. ¿Qué casos siguen sin demostración concluyente?** **11 NO_DEMOSTRADO**, listados en `INVESTIGACION_FACTIBILIDAD_30_120.md` §3.7:
- GENERAL_FITNESS a 90 min con 2 días;
- LOSE_WEIGHT principiante a 75 con 4 días en casa;
- GENERAL_FITNESS principiante con 1 día en casa;
- GAIN_MUSCLE a 60 e INCREASE_STRENGTH a 75 con 6 días y peso corporal.

**12. ¿Qué implica para un usuario con perfil congelado?**
- El perfil se congela con **el primer plan guardado**. Si la generación inicial falla (IA y respaldo), no se guarda ningún plan y el perfil **sigue editable**. Por ejemplo, INCREASE_STRENGTH intermedio, 120 min, 3 días, gimnasio: 422 y nada congelado.
- Riesgos reales:
  - un mensaje técnico (V20), sin el máximo viable;
  - con la IA habilitada, hasta **3 llamadas pagadas** desperdiciadas por intento, porque no hay una comprobación previa de la cota en la generación inicial.
- Un perfil congelado solo se queda sin plan si una semana posterior falla. La semilla queda descartada: 8/8 o 0/8 en todos los perfiles probados. Las vías posibles son P-02, cuando P-03 queda NO_FACTIBLE de forma segura, o un CASO A o NO_DEMOSTRADO.
- Diseño propuesto, sin implementar: `FactibilidadPerfilPolicy`, que evita la IA si el perfil es inviable y devuelve el máximo viable.

**13. ¿V4, V20, 4D y P-01 quedaron intactos?** **Sí.** No hay cambios en `PlanDraftValidator`, `MvpDosePolicy`, `CapabilityPrescriptionPolicy`, `GoalType`, `UserProfile` ni `DuracionPorObjetivoPolicy`.

**14. ¿Se mantuvieron P-02, P-03 y C-2?** **Sí.** Sin cambios en `PainMemoryService`, `SafeRegenerationService`, `ConservationValidator` ni V43. Sus pruebas pasan.

**15. ¿Qué encontró la revisión directa de V43?**
- El SQL coincide columna a columna con las entidades (lo valida `ddl-auto=validate` en cada arranque); los CHECK coinciden con los enums.
- La idempotencia está garantizada por `ON CONFLICT` y por `ux_sra_medicion`.
- Las FK son RESTRICT y no hay ningún camino de borrado en el código.
- Hay concurrencia probada, con dos pruebas nuevas: dolor simultáneo, y dos usuarios y dos regeneraciones de la misma semana.
- No se encontró ningún defecto. V43 no está en `generation` (V42) ni en `main` (V24): no se ha aplicado en ningún entorno.

**16. ¿Puede aplicarse V43 sobre V42 sin alterar el historial?** **Sí.** `MigracionV43IT`:
- V42 con datos, después V43: ni tablas, ni columnas, ni restricciones, ni filas anteriores cambian.
- El reinicio aplica 0 migraciones y `validate` pasa.

**17. ¿Las pruebas y el CI quedaron en verde?** Sí en local: `mvn verify -Pintegration` BUILD SUCCESS. El estado del CI está en el PR.

**18. ¿Cuántas pruebas pasaron y cuántas se omitieron?** Unitarias: 935 pasaron, 0 omitidas. Integración: 458 pasaron y 32 omitidas (opt-in). Antes eran 432 + 30: hay 26 de integración nuevas (10 de `GeneradorReglasCasoAIT`, 2 de concurrencia de V43 y otras de la batería actual), y `FactibilidadDuracionIT` queda omitida por ser opt-in. El método de recuento es el de Maven; no es comparable con la suma de .txt de 816.

**19. ¿Cuántas llamadas reales a Replicate se hicieron?** **0.** Todas las ejecuciones usan `fitsense.ai.enabled=false` y el log no contiene ninguna petición a `api.replicate.com`.

**20. ¿El PR está técnicamente listo para fusionarse, o necesita una decisión?**
- **Técnicamente sí:**
  - las correcciones no cambian políticas ni planes válidos anteriores;
  - V43 está lista;
  - todo está en verde.
- **Necesita tus decisiones funcionales:**
  - **(i)** Qué hacer con los máximos imposibles de P-01. Opciones:
    - mantener 120 y explicar el NO_FACTIBLE;
    - un máximo por objetivo y nivel (GAIN_MUSCLE 65, INCREASE_STRENGTH 76/84, LOSE_WEIGHT y GENERAL_FITNESS 87/108; GENERAL_FITNESS intermedio < 105);
    - revisar V20 o el tope de V24 para sesiones largas, que sería una política nueva.
  - **(ii)** Si se implementa la comprobación previa de factibilidad del perfil: ahorra llamadas pagadas y da mensajes útiles.
  - **(iii)** Qué hacer con los 8 CASO A pendientes: un constructor de semana en otro PR, o aceptarlos.
- **No se ha fusionado.**

## Pruebas

| Batería | Ejecutadas | Fallos | Omitidas |
|---|---:|---:|---:|
| Unitarias (`mvn test`) | 935 | 0 | 0 |
| Integración (`mvn verify -Pintegration`) | 458 | 0 | 32 |

Las pruebas opt-in quedan omitidas: campañas, IA real e investigaciones como `FactibilidadDuracionIT`.

Investigaciones ejecutadas aparte, todas en verde:
- `FactibilidadDuracionIT#matriz`: 1.236 combinaciones.
- `FactibilidadDuracionIT#semillas`: 72 perfiles × 8 semillas.
- `CampanaP123Cohorte02IT`, en REPLAY determinista con IA simulada: los planes salen idénticos y solo cambian marcas de tiempo.
