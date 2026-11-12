# Informe final del experimento: 20 usuarios sintéticos × 3 sesiones

**Backend:** `generation` `0974fb3` (`backend-mvp-1.0`), **sin cambios en producción**.
**Laboratorio:** `lab/experimento-kora-20usuarios`.
**El artículo no se ha modificado.**

## Resumen

### Etapa A (determinista)
A0, A1 y su repetición son **idénticas**.
- 20/20 planes iniciales válidos.
- Aw, Aobs y C exactos frente al recálculo independiente en 20/20.
- 18/20 decisiones iguales al oráculo del artículo. U07 y U12 difieren porque el P40 real es 39,26 % y 38,33 %.
- Sin IA, las 8 decisiones de adaptar terminan en respaldo (la misma semana).

### Etapa B
**Ningún defecto del backend demostrado**, así que no hay correcciones de producción. Los hallazgos están en `09`.

### Etapa C (Replicate real, `gpt-5-mini`)
- **Generación inicial con IA:** 20/20 planes válidos con origen `AI`. 18 al primer intento; 2 rechazos de los validadores (V24 y V15) corregidos al segundo.
- **Adaptación:** de 20 casos, solo **8 requieren IA**. Resultado: **4 adaptaciones aplicadas**, 4 sin efecto y 0 respaldos. Los validadores rechazaron 5 de 13 propuestas, todas por V20 (duración mínima).
- **Seguridad:** ninguna sesión no señalada cambió, 0 violaciones y 0 rutinas inválidas publicadas.
- **Igualdad con A:** las métricas y decisiones son idénticas a las de A1.
- **Consumo:** 35 predicciones en el intento válido (47 en el día) y **unos US$ 0,99 en total**, una estimación por tokens; la cifra oficial está en Replicate.
- **Intentos fallidos, conservados:** uno por un error del laboratorio (0 predicciones) y otro por el corte del proxy a los 30 s.

## Tabla de los 20 casos (resultado V32: original, corregido y con Replicate)

| ID | Obj. | Patrón | Aw/Aobs/C | Decisión | A0 (original) | A1 (corregido) | C (Replicate) |
|---|---|---|---|---|---|---|---|
| U01 | GM | E1 H/H/H | 100.00/100.00/100.00 | NO_CHANGE | SIN_ORDEN | SIN_ORDEN | SIN_ORDEN |
| U02 | GM | E2 H/H/P40 | 80.00/80.00/100.00 | NO_CHANGE | SIN_ORDEN | SIN_ORDEN | SIN_ORDEN |
| U03 | GM | E3 H/H/O | 66.67/66.67/100.00 | ADAPT_LOCALIZED | RESPALDO | RESPALDO | SIN_EFECTO (1 ll.) |
| U04 | GM | E4 H/O/O | 33.33/33.33/100.00 | ADAPT_EXPANDED | RESPALDO | RESPALDO | APLICADA (1 ll.) · cambió S2 |
| U05 | GM | E5 H/N/N | 33.33/100.00/33.33 | INSUFFICIENT_DATA | SIN_ORDEN | SIN_ORDEN | SIN_ORDEN |
| U06 | FU | E1 H/H/H | 100.00/100.00/100.00 | NO_CHANGE | SIN_ORDEN | SIN_ORDEN | SIN_ORDEN |
| U07 | FU | E2 H/H/P40 | 79.75/79.75/100.00 | ADAPT_LOCALIZED | SIN_EFECTO | SIN_EFECTO | SIN_EFECTO |
| U08 | FU | E3 H/H/O | 66.67/66.67/100.00 | ADAPT_LOCALIZED | RESPALDO | RESPALDO | SIN_EFECTO (2 ll.) |
| U09 | FU | E4 H/O/O | 33.33/33.33/100.00 | ADAPT_EXPANDED | RESPALDO | RESPALDO | APLICADA (3 ll.) · cambió S2 |
| U10 | FU | E5 H/N/N | 33.33/100.00/33.33 | INSUFFICIENT_DATA | SIN_ORDEN | SIN_ORDEN | SIN_ORDEN |
| U11 | PP | E1 H/H/H | 100.00/100.00/100.00 | NO_CHANGE | SIN_ORDEN | SIN_ORDEN | SIN_ORDEN |
| U12 | PP | E2 H/H/P40 | 79.44/79.44/100.00 | ADAPT_LOCALIZED | SIN_EFECTO | SIN_EFECTO | SIN_EFECTO |
| U13 | PP | E3 H/H/O | 66.67/66.67/100.00 | ADAPT_LOCALIZED | RESPALDO | RESPALDO | APLICADA (1 ll.) · cambió S3 |
| U14 | PP | E4 H/O/O | 33.33/33.33/100.00 | ADAPT_EXPANDED | RESPALDO | RESPALDO | APLICADA (1 ll.) · cambió S2|S3 |
| U15 | PP | E5 H/N/N | 33.33/100.00/33.33 | INSUFFICIENT_DATA | SIN_ORDEN | SIN_ORDEN | SIN_ORDEN |
| U16 | AG | E1 H/H/H | 100.00/100.00/100.00 | NO_CHANGE | SIN_ORDEN | SIN_ORDEN | SIN_ORDEN |
| U17 | AG | E2 H/H/P40 | 80.00/80.00/100.00 | NO_CHANGE | SIN_ORDEN | SIN_ORDEN | SIN_ORDEN |
| U18 | AG | E3 H/H/O | 66.67/66.67/100.00 | ADAPT_LOCALIZED | RESPALDO | RESPALDO | SIN_EFECTO (2 ll.) |
| U19 | AG | E4 H/O/O | 33.33/33.33/100.00 | ADAPT_EXPANDED | RESPALDO | RESPALDO | SIN_EFECTO (2 ll.) |
| U20 | AG | E5 H/N/N | 33.33/100.00/33.33 | INSUFFICIENT_DATA | SIN_ORDEN | SIN_ORDEN | SIN_ORDEN |

«ll.» = llamadas reales a Replicate. Los valores Aw/Aobs/C son iguales en A0, A1 y C.

## Respuestas (orden §17)

1. **¿Se ejecutaron los veinte usuarios?** Sí, en A0, A1, la repetición de A1 y C (más los 20 gemelos de la generación con IA).
2. **¿Qué configuración se utilizó?**
   - `0974fb3`, 46 migraciones, 1.219 ejercicios activos;
   - `PLAN-HIST-1`, `allow-candidates=false`, progresión apagada;
   - PostgreSQL 16 efímero, reloj controlado, semillas `userId × 1.000.003 + 20.759`;
   - en C: `gpt-5-mini` sobre `openai/gpt-5-structured`, credencial inyectada por el proxy y `prefer-wait-seconds=25` (anexo 2).
3. **¿Cuántos planes iniciales se generaron correctamente?** 20/20 con el motor de reglas y 20/20 con Replicate.
4. **¿Qué perfiles fueron rechazados?** Ninguno de forma definitiva. Con IA hubo 2 propuestas rechazadas (U05G por V24 y U14G por V15) que se corrigieron al segundo intento.
5. **¿Coincide la fórmula del artículo con el backend?** La dosis, la sesión, Aw, Aobs, C y el umbral del 50 % coinciden. **La decisión usa Aobs, no Aw** (D1), sin efecto en estos 20 casos.
6. **¿Cómo contabiliza las sesiones parciales?** Por ejercicio, ejecutado / prescrito con un tope del 100 %. La sesión es la media sobre los ejercicios prescritos y cuenta con su porcentaje real.
7. **¿Se alcanzó exactamente P40?** En U02 y U17 sí. En U07 y U12, no (39,26 y 38,33) con la regla uniforme congelada; con un reparto desigual sí se habría podido (P1).
8. **¿Coinciden Aw, Aobs y C?** Con el recálculo independiente, 20/20. Con el oráculo teórico, C 20/20 y Aw/Aobs 18/20.
9. **¿Se distingue la omisión declarada de la ausencia de registro?** Sí: la omisión cuenta como registro al 0 %; la ausencia no cuenta.
10. **¿Cuántas decisiones coincidieron con el oráculo?** 18/20, en A y en C.
11. **¿Qué casos no coincidieron?** U07 y U12 (`ADAPT_LOCALIZED` en lugar de «sin cambio»).
12. **¿Diferencias metodológicas o defectos reales?** Hallazgos H1–H12, todos de laboratorio, entorno, metodología u observación. **Defectos reales del backend: 0.**
13. **¿Cuántos defectos se corrigieron?** Del backend, 0. Del laboratorio, 1 (H8).
14. **¿Qué archivos de producción cambiaron?** Ninguno.
15. **¿Cómo cambiaron los resultados tras las correcciones?** No cambiaron: A1 = A0.
16. **¿Se ejecutó Replicate real en los veinte escenarios?** Sí. Los 20 pasaron por el ciclo con la IA activa; la política solo llamó a la IA en los 8 que la requieren, y además se generaron 20 planes con IA.
17. **¿Cuántas llamadas reales se realizaron?** 35 en el intento válido (22 de generación y 13 de adaptación). 47 en el día, contando las 12 del intento 2.
18. **¿Cuántas propuestas de la IA fueron válidas?** 28 de 35: 20 de 22 en la generación y 8 de 13 en la adaptación.
19. **¿Cuántas fueron rechazadas?** 7: 2 en la generación (V24, V15) y 5 en la adaptación (V20).
20. **¿Cuántos reintentos y respaldos se usaron?** 7 reintentos (2 + 5) y **0 respaldos** en C. En A hubo 8 respaldos sin IA.
21. **¿Cuántas adaptaciones se aplicaron realmente?** 4 (U04, U09, U13 y U14), con 5 sesiones cambiadas.
22. **¿Cuántas sesiones no afectadas permanecieron intactas?** 46 de 46, en A y en C.
23. **¿Se publicó alguna rutina inválida?** No. 0 violaciones en A y en C.
24. **¿Diferencias entre el motor determinista y Replicate?**
    - Las mismas métricas y decisiones.
    - **Adaptación:** C aplica cambios en 4 de 8 casos, donde A publica la misma semana.
    - **Generación:** C tarda unos 98 s por plan y A menos de 1 s (`20`).
25. **¿Cuál fue el consumo del proveedor?** Unos US$ 0,76 en el intento válido y unos US$ 0,99 en el día (estimación por tokens: 1,78 M de entrada y 0,27 M de salida). La cifra oficial está en el panel de Replicate.
26. **¿La ejecución determinista fue reproducible?** Sí, idéntica en tres bases. Además, el plan inicial de C es idéntico al de A1.
27. **¿La regresión histórica sigue pasando?** Sí:
    - 1.043 unitarias y 489 de integración, con 0 fallos y 37 omitidas *opt-in*;
    - 960 perfiles idénticos;
    - REPLAY idéntica.
    - Ninguna llamada a Replicate en la regresión.
28. **¿Qué partes del artículo hay que corregir?** D1–D13 (`23`). Las principales:
    - la decisión usa Aobs;
    - el alcance de la adaptación localizada y de la ampliada;
    - «decidir adaptar» no es lo mismo que «adaptación aplicada» (4/8 con IA, 0/8 sin IA);
    - el P40 real;
    - la duración frente a los minutos disponibles;
    - los tiempos y rechazos de la IA.

    Antes hay que cotejar con el .docx, que no estaba disponible.

## Condición de aceptación

| Condición | Estado |
|---|---|
| Resultados observados y trazables de U01–U20 | ✅ A0, A1 y C |
| Resultados anteriores a las correcciones conservados | ✅ A0 (`33d8d06`), intentos 1 y 2 de C |
| Diferencias explicadas | ✅ |
| Defectos corregidos o identificados | ✅ 0 en el backend; 1 en el laboratorio, corregido; propuestas P1–P4 pendientes de tu decisión |
| Experimento determinista repetido | ✅ |
| Etapa con Replicate real dentro del límite | ✅ 47 de 90 predicciones y ~US$ 0,99 de US$ 10 |
| Propuestas, rechazos, reintentos y respaldos registrados | ✅ (`14`–`18`) |
| Sin cambios funcionales no autorizados | ✅ |
| Regresión histórica sin diferencias | ✅ |
| Sin despliegue, migraciones remotas ni Flutter | ✅ |

**La evaluación queda terminada.**
