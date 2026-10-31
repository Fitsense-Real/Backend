# Cierre: políticas P-01, P-02 y P-03 y campaña de 10 usuarios nuevos

**Fecha:** 31/10/2026.
**Rama:** `politicas-p123`, desde `generation` en `7af6ad8`.
**Entorno:** PostgreSQL efímero (Testcontainers). La URL efectiva se comprobó: `jdbc:postgresql://localhost:…/test`.
**Sin:** Neon, Railway, Flutter, `main`, despliegue ni llamadas a Replicate.

## Resumen ejecutivo

**Políticas:**
- **P-01, P-02 y P-03 implementadas**, con una migración nueva (V43, solo tablas nuevas).
- **P-04 no se implementó**, como se decidió.

**Campaña `FITSENSE_STRESS_P123_COHORT_02`:** 10 usuarios **nuevos** × 10 semanas.
- 10 de 10 planes iniciales válidos.
- **100 de 100 semanas con plan.**
- 100/100 dentro de la frecuencia por nivel.
- **0 INVALIDA** en la validación independiente.
- 0 publicaciones de un ejercicio excluido.

**Dolor:**
- N06: P-03 lo resolvió en el mismo ciclo (sustitución determinista).
- N08: la adaptación (IA simulada) sustituyó; la exclusión persistió hasta la reactivación explícita, y reactivar no reincorporó.

**Progresión:** 48 pasos y 1 vuelta atrás por fatiga.

**Reincorporación:**
- **0 en N01 y N09.** Su sesión parcial no tenía ningún relleno que retirar.
- Una **variante declarada a posteriori** (N09B) completó retirada → progresión → reincorporación (S11) → ejecución (S11), con la IA **simulada**.

**Errores:**
- 1 defecto del instrumento de validación (C-1), corregido.
- 1 defecto de P-03 (C-3), corregido antes de la campaña.
- 1 conflicto en V8C pendiente de decisión (C-2).

**Llamadas reales a Replicate: 0.**

## Respuestas

**1. ¿P-01 quedó implementada?**
Sí:
- en el alta y en la edición previa al primer plan (`UserProfile`);
- en la planificación, para los perfiles históricos (`PERFIL_FUERA_DE_P01`, sin llamar a la IA).

**2. ¿Los cuatro objetivos respetan sus mínimos?**
Sí: 15 en GAIN_MUSCLE e INCREASE_STRENGTH; 30 en LOSE_WEIGHT y GENERAL_FITNESS; 180 de máximo. Hay 27 casos límite probados (14/15/16, 29/30/31 y 180/181).

Que un perfil se admita no garantiza un plan: INCREASE_STRENGTH intermedio de 15 min en gimnasio no tiene plan, porque V4 rechaza 18 min con un techo de 17.

**3. ¿El perfil sigue siendo inmutable?**
Sí:
- no hay endpoint ni mecanismo para cambiar los minutos ni M;
- las exclusiones por dolor viven en otra tabla (`user_safety_exclusions`);
- `blocked_exercise_ids` no se toca.

**4. ¿P-02 conserva las exclusiones por dolor entre semanas?**
Sí. Están persistidas en la base y no caducan:
- N06: 7 semanas publicadas sin 129;
- `PoliticasP123IT`: 5 semanas;
- `ProgresionIT`: 6 semanas;
- sobreviven a un reinicio del contexto.

**5. ¿Se mantienen durante regeneraciones y nuevas generaciones?**
Sí. `prepare()` las resta del conjunto elegible en todos los caminos:
- generación nueva;
- regeneración de P-03;
- adaptación;
- progresión;
- reincorporación (reserva DESCARTADA).

**6. ¿La reactivación requiere una acción explícita y segura?**
Sí:
- `POST /users/me/pain-exclusions/{id}/reactivation` autenticado, con `confirmNoPain: true`;
- un control de seguridad que la confirmación no puede saltarse;
- evidencia guardada.

Se probaron 401, 404 (otro usuario), 422 (sin confirmar o con una restricción incompatible) y 200.

**7. ¿P-03 regenera de manera segura en el mismo ciclo?**
Sí, en dos niveles:
- **nivel 1:** sustitución determinista SUST-SEG-1, sin subir el trabajo;
- **nivel 2:** regeneración solo con reglas, validada, idempotente y atómica.

En la campaña lo resolvió el nivel 1 (N06). Las pruebas cubren el nivel 2, la doble ejecución y la interrupción.

**8. ¿Qué sucede cuando no existe alternativa?**
- Queda NO_FACTIBLE, con el motivo en `safe_regeneration_attempts` y en `/users/me/plan/adaptations` (`P-03: sin alternativa segura válida: …`).
- No se publica nada.
- El dolor sigue recordado para el ciclo siguiente.

**9. ¿Los diez perfiles son realmente nuevos?**
Sí: cohorte, usuarios, perfiles, planes, ejecuciones y decisiones nuevos, en una base vacía.
- Solo se conservó la distribución de días pedida (2, 5, 7, 6, 1, 4, 1 y 3, más 2 y 6).
- No se reutilizó nada de campañas anteriores.

**10. ¿Cuántos consiguieron un plan inicial válido?**
10 de 10, todos con el motor de reglas, y validados de forma independiente.

**11. ¿Cuántas de las 100 semanas tuvieron plan?**
100.

**12. ¿Se respetaron las frecuencias máximas?**
Sí, en 100 de 100: N03 tiene 7 días y recibe 4 sesiones; nunca 7; sin fechas repetidas ni días no disponibles.

**13. ¿Cuántas adaptaciones se aplicaron?**
Dos con la propuesta de la IA simulada (N08 S2 por dolor; N03 S3 ACORTAR), más una resuelta por P-03 sin IA (N06 S4).

**14. ¿Cuántas fueron rechazadas?**
- **9 respuestas simuladas rechazadas** por los validadores en 3 semanas:
  - N03 S4: V20 sin la exención D8 (C-2);
  - N06 S4: contrato inválido, a propósito;
  - N07 S4: V25 al quitar la movilidad.
- Ninguna se publicó.
- Además: 1 REPROGRAMACION_IMPOSIBLE (N04 S2, sin llamada) y 3 sin efecto.

**15. ¿Cuántas progresiones se ejecutaron?**
- **48** en las 100 semanas.
- 2 en la extensión de N09.
- 15 en la variante (N01B 8, N09B 7).

Todas ≤ 5 % de la sesión y ≤ 1,10 × base, con un paso por ciclo.

**16. ¿Hubo retrocesos por fatiga?**
Sí, 1: N02 llevó el ejercicio 318 de 3×11 a 3×10 tras declarar fatiga en la sesión del paso.

**17. ¿Qué ejercicios fueron retirados?**
- Por ACORTAR:
  - 1117 en N03 (S3);
  - 416 en N09B (variante, S1).
- Por dolor (sustituidos, no retirados para volver):
  - 129 en N06;
  - 791 en N08.

**18. ¿Cuáles pudieron reincorporarse?**
- **416 (N09B, variante a posteriori).**
- 1117 (N03) no: N03 no tiene progresiones, porque el menor paso supera el 5 %.
- N01 y N09 no tuvieron ninguna retirada.

**19. ¿En qué semana regresaron?**
416, en S11 (publicada en el ciclo de S10).

**20. ¿Fueron entrenados posteriormente?**
Sí: 416 se ejecutó en S11.

**21. ¿Se respetó siempre la memoria del dolor?**
Sí: 0 publicaciones de un ejercicio excluido en 139 semanas publicadas (principales, extensión y variante), y en todas las pruebas.

**22. ¿Hubo violaciones de dosis, seguridad o linaje?**
No:
- 0 INVALIDA y 0 NO_CONCLUYENTE;
- 0 dosis sin autorización;
- la consulta de linaje V31/V32 no encuentra violaciones (también con P-03).

**23. ¿Qué errores técnicos se corrigieron?**
- **C-1:** instrumento de validación independiente: el linaje D8 se cortaba en una señalada sin efecto.
- **C-3:** el nivel 1 de P-03 podía subir el trabajo de una sesión con dolor.
- **C-4:** dobles de IA de pruebas incompatibles con P-02.
- **C-5:** el detector del arnés.

Detalle en `ERRORES_Y_CORRECCIONES.md`.

**24. ¿Qué limitaciones permanecen?**
- **C-2** (decisión): en el camino de la IA, una sesión acortada que se señala por otra causa y vuelve igual pierde la exención D8. Cuesta hasta 3 llamadas. Corregirlo exige tocar V8C.
- **Mínimo de P-01 en INCREASE_STRENGTH:** con 15 min no hay plan para el intermedio en gimnasio (V4). ¿Subir el mínimo a 20?
- **Progresión imposible** en sesiones muy cortas o solo de duración (N03 y N10): el menor paso supera el 5 %. Ya documentada; no se fuerza.
- **ACORTAR sin relleno:** en sesiones de 4 ejercicios, todos de slot 4C (N01 y N09 a 30 min), ACORTAR no tiene nada que retirar, así que esos perfiles nunca pasan por la retirada y la reincorporación.
- **Composición dependiente del `user_id`:** la semilla del motor de reglas depende de él. Un mismo perfil con otro identificador recibe otra composición (N01 frente a N01B).
- **Nivel 2 de P-03 sin linaje:** la semana regenerada vuelve a las dosis base, sin linaje hacia la semana medida (igual que la regeneración que antes ocurría una semana después).
- **Validación no humana:** nada de esto es una validación con usuarios reales ni con IA real.

**25. ¿Cuántas llamadas reales nuevas se hicieron a Replicate?**
**Cero.**
- `fitsense.ai.enabled=false`, y el espía de la generación con IA registró 0 invocaciones.
- Las 16 respuestas de la IA de corrección son **SIMULADAS** (14 en las 100 semanas).
- El total histórico sigue en 78 de 80.

**26. ¿Qué pruebas con IA real convendría hacer si se aprueba presupuesto?** (no ejecutadas)

| Prueba | Llamadas estimadas |
|---|---:|
| Generación inicial de los 10 perfiles con IA real, para comparar con el motor de reglas | 10–30 (1 a 3 intentos) |
| Las 14 adaptaciones de esta campaña con IA real, en modo REPLAY sobre los mismos datos | 14–42 |
| P-03 con IA real: dolor sin sustituto obvio (¿la IA encuentra alternativa antes del nivel 1?) | 3–6 |
| Reincorporación N09B con retirada propuesta por la IA real | 1–3 |
| **Total** | **≈ 28–81 llamadas** |

Coste estimado: **US$ 0,8–3,5**, a US$ 0,02–0,05 por llamada (campañas anteriores). Requiere autorizar el presupuesto. El límite histórico de 80 no alcanza (quedan 2): habría que fijar un límite nuevo.

## Integración

- **PR a `generation`** con la implementación, las regresiones, el arnés (opt-in) y esta evidencia.
- **CI completa** (`mvn verify -Pintegration`).
- **No se fusiona automáticamente:** incluye la migración V43 y cambios de contrato (dos resultados nuevos en la API y dos endpoints nuevos). Queda para revisión.
- **V43:**
  - probada desde una base vacía y sobre esquemas con historial (catálogos fijados en V36–V41 más V42 y V43);
  - **no aplicada a Neon ni a Railway**.
- **Progresión:** sigue apagada fuera de las pruebas (`fitsense.adaptation.progression.enabled=false` por defecto).

## Entregables

| Archivo | Contenido |
|---|---|
| `POLITICAS_P01_P02_P03_IMPLEMENTACION.md` | Reglas, clases, persistencia, migración, contrato y pruebas |
| `PERFILES_10_USUARIOS_NUEVOS.csv` | Los 10 perfiles más el negativo de P-01 |
| `GUION_10_USUARIOS.csv` | Evento programado, registro real y comportamiento esperado por semana |
| `HISTORIAL_100_SEMANAS.csv` | Las 100 semanas principales |
| `EXTENSION_REINCORPORACION.csv` | Extensión de N01/N09 (S11–S16) y la variante N01B/N09B, separadas de las 100 |
| `GENERACION_10_USUARIOS.md` | Generación inicial |
| `ADAPTACION_10_USUARIOS.md` | Decisiones, intervenciones y resultados |
| `PROGRESION_REINCORPORACION_10_USUARIOS.md` | Pasos, volúmenes, reservas, retiradas y reincorporación |
| `VALIDACION_DOLOR_PERSISTENTE.md` | P-02 en la campaña y en las pruebas |
| `VALIDACION_NO_FACTIBLE_SEGURO.md` | P-03 en la campaña y en las pruebas |
| `AUDITORIA_INVARIANTES.md` | Validación independiente e invariantes |
| `ERRORES_Y_CORRECCIONES.md` | Causas raíz, correcciones y regresiones |
| `CIERRE_POLITICAS_Y_CAMPANA.md` | Este documento |
| `evidencia/` | Volcados en crudo de la base aislada (V32, planes, métricas, exclusiones, regeneraciones y perfiles), `PASOS.csv`, `traza_backend.log` y `eventos.log`. Sin credenciales: los usuarios son sintéticos, con correos `@pruebas.invalid` |
