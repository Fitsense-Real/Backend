# Informe final del experimento: 20 usuarios sintéticos × 3 sesiones

**Backend:** `generation` `0974fb3` (`backend-mvp-1.0`).
**Rama del laboratorio:** `lab/experimento-kora-20usuarios`.
**El artículo no se ha modificado.**

## Resumen

**Etapa A (determinista):** ejecutada (A0), repetida tras la revisión (A1) y repetida otra vez desde una base vacía. Las tres son **idénticas**.
- 20/20 planes iniciales válidos.
- Métricas exactas frente al recálculo independiente en 20/20.
- 18/20 decisiones coinciden con el oráculo del artículo. Las 2 que no (U07, U12) son el efecto del P40 real (39,26 % y 38,33 %) sobre el umbral estricto del 80 %.

**Etapa B:** **ningún defecto del backend** demostrado, así que no hay correcciones de producción. Hay 7 hallazgos metodológicos o de laboratorio (`09`).

**Etapa C (Replicate real):** **no ejecutada.** La sesión no tiene `REPLICATE_API_TOKEN`.
- 0 llamadas y US$ 0.
- El laboratorio está preparado, con límite de 90 llamadas / USD 10 y guarda de base local, y está comprobado que se bloquea sin token.
- **No hay ningún resultado de la IA inventado.**

## Tabla de los 20 casos

| ID | Obj. | Patrón | A0: Aw/Aobs/C · decisión · resultado | A1 (corregido) | C (Replicate) |
|---|---|---|---|---|---|
| U01 | GM | E1 H/H/H | 100.00/100.00/100.00 · NO_CHANGE · SIN_ORDEN | = A0 · API SIN_ADAPTACION | No ejecutado (sin token) |
| U02 | GM | E2 H/H/P40 | 80.00/80.00/100.00 · NO_CHANGE · SIN_ORDEN | = A0 · API SIN_ADAPTACION | No ejecutado (sin token) |
| U03 | GM | E3 H/H/O | 66.67/66.67/100.00 · ADAPT_LOCALIZED · RESPALDO | = A0 · API RESPALDO_SIN_IA | No ejecutado (sin token) |
| U04 | GM | E4 H/O/O | 33.33/33.33/100.00 · ADAPT_EXPANDED · RESPALDO | = A0 · API RESPALDO_SIN_IA | No ejecutado (sin token) |
| U05 | GM | E5 H/N/N | 33.33/100.00/33.33 · INSUFFICIENT_DATA · SIN_ORDEN | = A0 · API SIN_ADAPTACION | No ejecutado (sin token) |
| U06 | FU | E1 H/H/H | 100.00/100.00/100.00 · NO_CHANGE · SIN_ORDEN | = A0 · API SIN_ADAPTACION | No ejecutado (sin token) |
| U07 | FU | E2 H/H/P40 | 79.75/79.75/100.00 · ADAPT_LOCALIZED · SIN_EFECTO | = A0 · API CONSERVACION_DELIBERADA | No ejecutado (sin token) |
| U08 | FU | E3 H/H/O | 66.67/66.67/100.00 · ADAPT_LOCALIZED · RESPALDO | = A0 · API RESPALDO_SIN_IA | No ejecutado (sin token) |
| U09 | FU | E4 H/O/O | 33.33/33.33/100.00 · ADAPT_EXPANDED · RESPALDO | = A0 · API RESPALDO_SIN_IA | No ejecutado (sin token) |
| U10 | FU | E5 H/N/N | 33.33/100.00/33.33 · INSUFFICIENT_DATA · SIN_ORDEN | = A0 · API SIN_ADAPTACION | No ejecutado (sin token) |
| U11 | PP | E1 H/H/H | 100.00/100.00/100.00 · NO_CHANGE · SIN_ORDEN | = A0 · API SIN_ADAPTACION | No ejecutado (sin token) |
| U12 | PP | E2 H/H/P40 | 79.44/79.44/100.00 · ADAPT_LOCALIZED · SIN_EFECTO | = A0 · API CONSERVACION_DELIBERADA | No ejecutado (sin token) |
| U13 | PP | E3 H/H/O | 66.67/66.67/100.00 · ADAPT_LOCALIZED · RESPALDO | = A0 · API RESPALDO_SIN_IA | No ejecutado (sin token) |
| U14 | PP | E4 H/O/O | 33.33/33.33/100.00 · ADAPT_EXPANDED · RESPALDO | = A0 · API RESPALDO_SIN_IA | No ejecutado (sin token) |
| U15 | PP | E5 H/N/N | 33.33/100.00/33.33 · INSUFFICIENT_DATA · SIN_ORDEN | = A0 · API SIN_ADAPTACION | No ejecutado (sin token) |
| U16 | AG | E1 H/H/H | 100.00/100.00/100.00 · NO_CHANGE · SIN_ORDEN | = A0 · API SIN_ADAPTACION | No ejecutado (sin token) |
| U17 | AG | E2 H/H/P40 | 80.00/80.00/100.00 · NO_CHANGE · SIN_ORDEN | = A0 · API SIN_ADAPTACION | No ejecutado (sin token) |
| U18 | AG | E3 H/H/O | 66.67/66.67/100.00 · ADAPT_LOCALIZED · RESPALDO | = A0 · API RESPALDO_SIN_IA | No ejecutado (sin token) |
| U19 | AG | E4 H/O/O | 33.33/33.33/100.00 · ADAPT_EXPANDED · RESPALDO | = A0 · API RESPALDO_SIN_IA | No ejecutado (sin token) |
| U20 | AG | E5 H/N/N | 33.33/100.00/33.33 · INSUFFICIENT_DATA · SIN_ORDEN | = A0 · API SIN_ADAPTACION | No ejecutado (sin token) |

Los valores son Aw/Aobs/C en %. «= A0» significa idéntico a A0.

## Respuestas (orden §17)

1. **¿Se ejecutaron los veinte usuarios?** Sí, en la etapa determinista: tres veces (A0, A1 y la repetición), todas completas. En la etapa C, no.
2. **¿Qué configuración se utilizó?**
   - `0974fb3`, con 46 migraciones (hasta V44_2) y 1.219 ejercicios activos;
   - `PLAN-HIST-1`, `allow-candidates=false`, progresión apagada e IA apagada;
   - PostgreSQL 16 efímero y reloj controlado;
   - perfiles congelados (`02`, `03`), semilla `userId × 1.000.003 + 20.759`.
3. **¿Cuántos planes iniciales se generaron correctamente?** 20/20, con el motor de reglas, en 1 intento y con 0 problemas de validación.
4. **¿Qué perfiles fueron rechazados?** Ninguno.
5. **¿Coincide la fórmula del artículo con el backend?** La dosis, la sesión, Aw, Aobs, C y el umbral del 50 % coinciden. **La decisión usa Aobs, no Aw** (D1), lo que no afecta a estos 20 casos. Hay además reglas que el artículo no menciona (D6).
6. **¿Cómo contabiliza las sesiones parciales?**
   - Cada ejercicio vale 100 × ejecutado / prescrito, con 2 decimales y un tope del 100 %.
   - La sesión es la media sobre los ejercicios prescritos.
   - Con ≥ 30 % la sesión queda PARTIAL y cuenta su porcentaje real en Aw y en Aobs.
7. **¿Se alcanzó exactamente P40?** En U02 y U17 sí (40,00). En U07 (39,26) y U12 (38,33) no: la regla del protocolo (cada ejercicio al 40 % redondeado) no da el 40 % con 18 o 16 repeticiones. Un reparto desigual sí lo habría dado, pero no se cambió la entrada después de ver el resultado (P1).
8. **¿Coinciden Aw, Aobs y C?**
   - Con el recálculo independiente: 20/20, con error 0,00.
   - Con el oráculo teórico: C 20/20; Aw y Aobs 18/20 (U07 −0,25 pp y U12 −0,56 pp, por P40).
9. **¿Se distingue la omisión declarada de la ausencia de registro?** Sí:
   - la omisión con motivo cuenta como registro al 0 % (E3, E4);
   - la ausencia no cuenta (E5: C = 33,33 y Aobs = 100).
10. **¿Cuántas decisiones coincidieron con el oráculo?** 18/20.
11. **¿Qué casos no coincidieron?** U07 y U12: el backend decidió `ADAPT_LOCALIZED` y el oráculo esperaba «sin cambio». El resultado fue `SIN_EFECTO`, una conservación deliberada porque no hay causa registrada.
12. **¿Diferencias metodológicas o defectos reales?**
    - Metodológicas o de laboratorio: H1–H7 (`09`).
    - Defectos reales: **0**.
13. **¿Cuántos defectos se corrigieron?** 0, porque no había ninguno demostrado.
14. **¿Qué archivos de producción cambiaron?** Ninguno.
15. **¿Cómo cambiaron los resultados tras las correcciones?** No cambiaron: A1 es idéntica a A0 (`12`).
16. **¿Se ejecutó Replicate real en los veinte escenarios?** **No**, por falta de token. Bloqueo documentado (`13`, `evidencia/C_bloqueo`).
17. **¿Cuántas llamadas reales se realizaron?** 0.
18. **¿Cuántas propuestas de la IA fueron válidas?** No aplica, porque no hubo propuestas.
19. **¿Cuántas fueron rechazadas?** No aplica.
20. **¿Cuántos reintentos y respaldos se usaron?**
    - Con Replicate: no aplica.
    - En la etapa A hubo **8 respaldos sin IA** (E3 y E4) y 0 reintentos. No son fallos de la IA.
21. **¿Cuántas adaptaciones se aplicaron realmente?** 0. Sin IA, las 8 decisiones de adaptar terminaron en respaldo, y las 2 de U07/U12 en conservación deliberada.
22. **¿Cuántas sesiones no afectadas permanecieron intactas?** 46 de 46. Las 14 señaladas tampoco cambiaron.
23. **¿Se publicó alguna rutina inválida?** No. 20 planes iniciales y 20 siguientes validados, con 0 violaciones de linaje.
24. **¿Diferencias entre el motor determinista y Replicate?** No se pueden medir sin la etapa C. Lo que sí se sabe es que solo 8 de los 20 casos requieren IA en la adaptación (`20`).
25. **¿Cuál fue el consumo del proveedor?** US$ 0 y 0 solicitudes.
26. **¿La ejecución determinista fue reproducible?** Sí, de forma idéntica en tres bases distintas (`12`, `22`).
27. **¿La regresión histórica sigue pasando?** Sí:
    - 1.043 unitarias y 489 de integración, con 0 fallos y 37 omitidas *opt-in*;
    - 960 perfiles idénticos;
    - REPLAY idéntica (`22`).
28. **¿Qué partes del artículo hay que corregir?** `23_DISCREPANCIAS_CON_EL_SHORT_PAPER.md`:
    - la decisión usa Aobs y no Aw (D1);
    - el alcance de la adaptación localizada y de la ampliada (D2, D3);
    - **decidir adaptar no es lo mismo que adaptar**: sin IA, respaldo (D4);
    - el P40 real (D5);
    - las reglas adicionales (D6);
    - la duración frente a los minutos disponibles (D7).

    Antes hay que cotejar con el .docx, que no estaba disponible.

## Condición de aceptación

| Condición | Estado |
|---|---|
| Resultados observados y trazables de U01–U20 | ✅ Etapa A |
| Resultados anteriores a cualquier corrección conservados | ✅ A0 en su propio commit (`33d8d06`) y nunca sobrescrito |
| Diferencias explicadas | ✅ |
| Defectos corregidos o pendientes | ✅ 0 defectos; 2 propuestas pendientes de tu decisión (P1, P2) |
| Experimento determinista repetido | ✅ A1 y su repetición |
| Etapa con Replicate real | ⛔ **Bloqueada y documentada** (sin token). No hay resultados inventados |
| Propuestas, rechazos, reintentos y respaldos registrados | Parcial: respaldos de la etapa A registrados; los de la IA quedan pendientes de la etapa C |
| Sin cambios funcionales no autorizados | ✅ Ningún cambio en `src/main` |
| Regresión histórica sin diferencias | ✅ |
| Sin despliegue, migraciones remotas ni Flutter | ✅ |

**La evaluación queda terminada en las etapas A y B. La etapa C está pendiente únicamente del token.**
