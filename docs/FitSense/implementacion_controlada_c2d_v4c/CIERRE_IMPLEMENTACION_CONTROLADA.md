# Cierre de la implementación controlada (C2-D, V4-C y versionado)

**Rama:** `feature/c2d-v4c-versionado`, desde `generation` (`afef98a`). **No se ha fusionado ni desplegado. Todo está desactivado por defecto.**

## Estado de cada componente

| Componente | Estado técnico | Activación |
|---|---|---|
| Versionado de políticas (`PlanningPolicy`, V44, traza) | **Implementado y probado** (unitarias, IT de migración, ciclo real) | Activo como registro: los planes nuevos guardan `PLAN-HIST-1` y su traza. Requiere desplegar V44 |
| V4-C (`PLAN-V4-0.1`) | **Implementado** en el validador, los dos generadores y los contratos de la IA. Probado con el validador real | Desactivado: la candidata exige permiso explícito |
| C2-D (`PLAN-C2D-0.1`) | **Implementado** en el validador real, con verificación de saturación. Probado en la matriz de 960 perfiles y en el ciclo real con P-02 y P-03 | Desactivado. **Pendiente de revisión profesional, contrato visible en la app y decisión sobre el catálogo** |
| Combinada (`PLAN-C2D-V4-0.1`) | Implementada; idéntica a C2-D con el motor de reglas | Desactivada |
| Contrato de duración estimada | Implementado detrás de `fitsense.api.duracion-estimada.enabled=false` | Flutter pendiente (fuera de esta orden) |
| Métrica aeróbica | Implementada detrás de `fitsense.metricas.aerobica.enabled=false`. Sin minutos ejecutados | Desactivada |
| Preferencia de cobertura de fuerza en LOSE_WEIGHT | Implementada detrás de `...preferir-grupo-no-cubierto=false` | Desactivada. Necesita su propia versión y aprobación |
| Modelo E | **No portado** | — |

## Respuestas (34)

1. **¿Se implementó el versionado real de políticas?** **Sí.**
   - Catálogo cerrado, selector por configuración y versión en el contexto.
   - Migración V44 y traza en generación, adaptación y P-03.
   - Herencia en las versiones siguientes de la misma semana.
   - Una versión desconocida da error controlado.
2. **¿Los planes históricos siguen validándose con sus reglas originales?** **Sí.** `NULL` = `PLAN-HIST-1`, y una reparación o regeneración de la semana hereda la versión. Con la histórica, los 960 perfiles salen idénticos a la matriz anterior.
3. **¿La migración se probó sin afectar bases remotas?** **Sí.** Solo en PostgreSQL efímero:
   - `MigracionV44IT`: histórico V43 → V44, sin cambios en columnas, restricciones ni filas; política `NULL`; lista cerrada; reinicio sin aplicar nada;
   - todas las IT, que migran desde una base vacía.
4. **¿V4-C valida realmente los segundos estimados?** **Sí:** `segundos × 100 ≤ M × 60 × 115`, en aritmética entera, ignorando lo declarado. Comprobado en 30, 45, 60 y 75 min (límite, −1 s y +1 s; declarado correcto, menor y mayor).
5. **¿Se rechaza el caso de IA con un 142 % estimado?** **Sí:** «dura unos 2550 s… techo 2070 s». La histórica lo sigue admitiendo, que es lo esperado en su versión.
6. **¿Se eliminó la divergencia entre generador y validador para V4-C?** **Sí.** El generador, el validador y la IA usan el mismo `PlanningPolicy.excedeTecho` y `limiteV4Segundos` con la misma M. En la matriz, 0 planes superan el límite.
7. **¿C2-D está conectado al validador real?** **Sí**, dentro de `PlanDraftValidator.validate`. No es un bypass: solo actúa si todo es V20 y la saturación queda demostrada.
8. **¿C2-D modifica algún ejercicio o dosis?** **No.** 960 de 960 planes idénticos a la histórica; solo decide la publicación.
9. **¿Cuántos de los 960 perfiles son válidos con C2-D real?** **951.** Son tres menos que el experimento (954): R0040, R0328 y R0616 no están saturados (existe una SUSTITUCIÓN válida). Los 6 de V15 siguen rechazados.
10. **¿Cuántos GAIN_MUSCLE a 75 min reciben plan?** **60 de 60** (al 56,7 %, el mismo plan que con 60 min). En el ciclo real, L2 tiene plan las 10 semanas.
11. **¿Qué criterios exactos definen la saturación?** Ningún movimiento único (ANADIR, SERIE hasta 3, SUSTITUIR por algo más largo, REDISTRIBUIR) con la dosis base aprobada mejora la sesión corta sin que el validador vigente encuentre otro problema. Presupuesto: 4.000 evaluaciones; si se agota o faltan datos, `NO_DEMOSTRADA` (`SATURACION_Y_LIMITES_DE_BUSQUEDA.md`).
12. **¿Se evaluaron sustituciones y redistribuciones?** **Sí.** Ambas son movimientos del verificador; la sustitución es la que excluye a los 3 perfiles de GENERAL_FITNESS. **Limitación:** es saturación local, no optimalidad global.
13. **¿Los cuatro perfiles del 46 % tienen ejercicios pertinentes?** **No demostrado; probablemente no:** el PULL sin material **no contiene ninguna tracción** (`AUDITORIA_CATALOGO_CASOS_EXTREMOS.md`).
14. **¿Hay algún ejercicio posiblemente mal clasificado?** **Sí:**
    - 775: patrón de empuje con zona espalda;
    - 1261, 1308 y 1259: tronco o movilidad clasificados como resistidos;
    - 315: instrucciones con mancuerna y equipo de peso corporal;
    - 320: sin carga, pertinencia como hipertrofia no demostrada.

    **No se ha cambiado nada.**
15. **¿Qué ocurre con R0800 y las 21 series de un grupo?** Es válido y saturado: 138 series por semana, 21 de pierna en una sesión, 10 ejercicios repetidos y **sin tracción como fuerza** (la única dosis de fuerza en PULL es un empuje). No se declara inseguro ni adecuado; va a revisión.
16. **¿Se reprodujo el bloqueo posterior al dolor de L5 y L6?** **Sí**, y es más grave de lo descrito: con la histórica no tienen plan **en ninguna semana**, porque ya la generación inicial falla V20.
17. **¿C2-D permite continuar el ciclo cuando existe un plan válido?** **Sí.**
    - L2, L4, L6 y L8 tienen plan S1–S10.
    - En L6 y L8, P-03 publica tras el dolor (al 40,1 % y al 34,7 %).
    - 0 ejercicios excluidos reaparecen.
18. **¿Qué casos siguen dando NO_FACTIBLE y por qué?** L7 S4, con cualquier política: P-03 nivel 2 detecta que la regeneración **sube el trabajo** (segundos 1.620 → 1.680). Es la regla de no añadir trabajo ante dolor, no V20.
19. **¿D8 y ACORTAR siguen funcionando?** Sus baterías pasan en la regresión completa. En el ciclo de C2-D, ACORTAR se registra y se conserva por RESPALDO (sin IA), y una sola semana con RPE 9 no generó D8. **No se ejercitó** el recorte con IA simulada en C2-D.
20. **¿Se mantienen los límites de progresión?** **Sí:** 0 subidas por encima del 5 % por paso ni del techo del 10 % en las semanas derivadas, y ninguna subida tras dolor, fatiga o baja adherencia. **L7:** en S5 suben las repeticiones (+) y bajan los segundos (−) porque es una **generación nueva** tras una semana sin plan, no una progresión (igual con la histórica).
21. **¿La reincorporación respeta las reglas?** **Sí:** la reactivación explícita devuelve el ejercicio como candidato (las exclusiones activas bajan en una). Ningún ejercicio aún excluido aparece y no hay restauración automática.
22. **¿La API distingue disponibilidad de duración estimada?** **Sí**, detrás de un indicador: `durationInfo` (`availableMinutes`, `estimatedMinutes`, `differenceMinutes`, `policy`, `reason`, `message`) y `planningPolicy`. Desactivado, la respuesta no cambia.
23. **¿La duración puede mostrarse correctamente en Flutter?** **El contrato está listo y especificado** (`CONTRATO_DURACION_ESTIMADA.md`), pero **Flutter no se ha modificado**. Es la condición de activación de C2-D.
24. **¿La métrica aeróbica diferencia trabajo, pausas y ejecución?** Diferencia **trabajo y bloque** (pausas, que no cuentan como minutos aeróbicos), e **intensidad desconocida** (sin equivalentes). Los **minutos ejecutados están en el contrato, pero valen `null`**: no se ha integrado la lectura de ejecución.
25. **¿Qué casos de LOSE_WEIGHT mejoran con la preferencia?** **48 de 148** (34 sin ningún grupo a cero). No los 104 estimados: el constructor reparte la misma fuerza, no la aumenta. Efectos colaterales:
    - cambia la composición de 82 planes sin brecha;
    - 2 válidos pasan a inválidos (V15).
26. **¿Qué cambios podrían alterar planes que antes eran válidos?**
    - C2-D: ninguno (0 de 960).
    - V4-C: ninguno del motor; sí la aceptación de **borradores de IA** que declaran menos de lo que dura su contenido.
    - Preferencia de LOSE_WEIGHT: 82 + 2 planes.
    - Versionado: no altera el contenido, solo añade la traza.
27. **¿Qué falta para completar la revisión profesional?**
    - Un evaluador cualificado, idealmente dos.
    - La muestra ampliada (`MUESTRA_PROFESIONAL_AMPLIADA.csv`: 28 originales + 8 nuevas, A01–A08) con el instrumento de 12 criterios.
    - Confirmar los umbrales de consenso.
    - **No hay ninguna evaluación registrada.**
28. **¿Cuántas pruebas pasaron, fallaron y se omitieron?** Ver `REGRESION_COMPLETA.md`.
29. **¿La campaña histórica sigue idéntica?** **Sí.** REPLAY de 100 semanas sin fallos, 0 llamadas a Replicate y evidencia idéntica salvo `recorded_at` y `reactivated_at` de `exclusiones_dolor.csv` (marcas de tiempo, comprobado línea a línea).
30. **¿Hubo modificaciones en Neon o Railway?** **No.**
31. **¿Hubo llamadas reales a Replicate?** **Cero.**
32. **¿Está todo desactivado por defecto?** **Sí:** `PLAN-HIST-1`, `allow-candidates=false` y los tres indicadores en `false`. No hay un interruptor único.
33. **¿Qué riesgos quedan antes de activar?** Ver `RIESGOS_PENDIENTES.md`. Los principales:
    - sesiones del 46 % y del 34,7 % sin adecuación demostrada;
    - el PULL sin material no tiene tracción;
    - Flutter;
    - el despliegue de V44;
    - el efecto de V4-C sobre la IA;
    - sin textos científicos completos ni revisión profesional.
34. **¿Qué cambios concretos necesitan mi aprobación final?**
    1. Fusionar esta rama y **desplegar V44** (versionado, solo esquema).
    2. Activar **`PLAN-V4-0.1`**, tras una campaña con IA simulada.
    3. Activar **`PLAN-C2D-0.1`** (o la combinada), solo tras la revisión profesional, el contrato visible en Flutter y una decisión sobre el catálogo del PULL sin material (excluir esos perfiles de C2-D o corregir el catálogo).
    4. Activar los indicadores de API (duración y métrica aeróbica).
    5. Decidir sobre los ejercicios dudosos del catálogo (775, 1259, 1261, 1308, 315, 320).
    6. La preferencia de LOSE_WEIGHT (con versión propia y la corrección de V15) o descartarla.
    7. Una mejora del constructor para la SUSTITUCIÓN que recuperaría los 3 perfiles de GENERAL_FITNESS.

**La validación técnica no equivale a validación profesional.** La implementación queda **lista técnicamente** y **no autorizada para activarse**.
