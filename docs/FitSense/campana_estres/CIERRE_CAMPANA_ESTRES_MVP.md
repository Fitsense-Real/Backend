# Cierre de la campaña de estrés del MVP

**Fecha:** 31/10/2026.
**Rama:** `campana-estres`, desde `generation` (`22fb709`).
**Entorno:** PostgreSQL efímero comprobado; sin Neon, Railway, Flutter ni `main`; sin despliegue y sin migraciones.

## Resumen ejecutivo

- **Generación:** 7 de 8 perfiles tienen plan: 6 por IA real y 1 por el motor de reglas. **U07** (GENERAL_FITNESS de 15 min) es **inviable** con las políticas actuales.
- **Semanas con plan válido:** 69 de 80.
- **Frecuencia:** 76 de 76 planes respetan el tope por nivel; nunca 7 sesiones.
- **Validación independiente:** 0 violaciones.
- **Adaptaciones con IA real:** 2 aplicadas. 1 imposibilidad demostrada sin llamada.
- **Progresión:** 32 pasos y 1 vuelta atrás.
- **Retiradas y reincorporaciones:** 1 retirada real; 0 reincorporaciones dentro de las 10 semanas.
- **Defectos:** se encontraron y corrigieron 2 con regresión: una sesión por encima del techo que se publicaba y un riesgo de seguridad tras un NO_FACTIBLE.
- **Coste:** 20 llamadas reales, ≈ US$ 0,54. El total global queda en 78 de 80.

## Las 22 preguntas

**1. ¿Los ocho usuarios pudieron generar sus planes?**
Siete sí. U07 no: ni la IA (3 intentos: V15, V23 y V4) ni el motor de reglas (V15 y V4, porque la composición mínima dura 21 min y el techo es 17) dan un plan válido en 15 min.

**2. ¿Cuántos los generó realmente Replicate?**
Seis: U01, U02, U04, U05, U06 y U08. Tres de ellos al primer intento (U04, U05 y U08).

**3. ¿Cuántos necesitaron el motor de respaldo?**
Uno, U03, tras 3 propuestas rechazadas (V15, V4 y V24). U06 usó el motor en la regeneración de S7.

**4. ¿Se respetó el máximo de cuatro sesiones para principiantes?**
Sí. U03 tiene 7 días disponibles y tuvo 4 sesiones en todos sus planes; U05 tuvo 1. `PerfilesLimiteIT.principianteSieteDias` lo confirma.

**5. ¿Y el de seis para intermedios y avanzados?**
Sí. U04 tiene 6 días y tuvo 6 sesiones. Con 7 días disponibles hay 6 (`intermedioSieteDias`).

**6. ¿Hubo algún intento de asignar siete sesiones?**
No. V2 exige exactamente `effectiveDaysPerWeek()` sesiones (≤ 4 o ≤ 6) en todo borrador, y ninguno de los 76 planes tuvo 7.

**7. ¿Cuántas de las 80 semanas tuvieron un plan válido?**
69. Faltan las 10 de U07 y S6 de U06, la semana después del NO_FACTIBLE por dolor; la semana siguiente se regeneró.

**8. ¿Qué combinaciones fueron inviables?**

| Combinación | Resultado |
|---|---|
| GENERAL_FITNESS principiante, 15 min, casa, peso corporal (U07) | Inviable |
| LOSE_WEIGHT, 15 min | Inviable, en la comprobación determinista |
| GAIN_MUSCLE e INCREASE_STRENGTH, 15 min | Sí tienen plan |

**9. ¿Cuántas adaptaciones reales se aplicaron?**
Dos:
- **U01 S2:** ACORTAR retira el relleno 416.
- **U08 S2:** dolor, con sustitución segura en el 2.º intento.

**10. ¿Cuáles fueron rechazadas y por qué?**
- **U08 S2:** el 1.er intento fue rechazado por el backend y se aplicó el 2.º.
- **U04 S2:** no hubo llamada; REPROGRAMACION_IMPOSIBLE porque no había fecha libre (6 días, 6 sesiones).
- **Sin presupuesto, sin llamada:** U03 S3/S4, U05 S2 y U06 S5. Se conservó la semana; U06 quedó en NO_FACTIBLE_POR_SEGURIDAD.

**11. ¿Cuántas progresiones ocurrieron?**
32 pasos (U01 7, U06 6, U02 5, U04 5, U08 5 y U05 4) más 1 vuelta atrás (U02, tras la fatiga).

U03 no tuvo ninguno. En S1–S4 no completó las semanas, y desde S5 sí las completó, pero en LOSE_WEIGHT principiante de 30 min el menor paso supera el 5 % de la sesión. Es la limitación ya documentada en `PendientesProgresionIT` (1(a)), y no se fuerza.

**12. ¿Se respetaron todos los límites de volumen?**
Sí. Cada paso fue ≤ 5 % de la sesión y ≤ 1,10 × base, con un paso por ciclo como máximo.

**13. ¿Cuántos ejercicios se retiraron?**
Uno con IA real (416, U01). En U08 el ejercicio que dolió se sustituyó (no es una retirada reincorporable).

**14. ¿Cuántos se reincorporaron?**
Ninguno. La ruta necesita unos 10 ciclos después de la retirada (prueba focalizada anterior) y la retirada fue en S2.

**15. ¿Cuántos se reincorporaron y entrenaron dentro de las diez semanas?**
Ninguno. No se adelantó ningún paso.

**16. ¿Se respetó siempre M?**
Sí. No hubo omisiones con M en este guion: U01 acortó desde una sesión parcial, sin M. La regla de M (sin reincorporar por encima de la M vigente) sigue cubierta por `CopiaSinIaIT.conMNoSeReincorpora`.

**17. ¿Hubo violaciones reales de seguridad, dosis o linaje?**
En lo publicado, no.
- Se encontró un **riesgo de seguridad latente** (E2): tras un NO_FACTIBLE por dolor, la regeneración podía volver a prescribir lo que dolió. Se corrigió y se comprobó en vivo con U06 (151 excluido).
- Se encontró una **violación de duración que se publicaba** (E1): sesiones de 19–21 min con techo 17. Se corrigió.

**18. ¿Qué errores técnicos se corrigieron?**
- **E1:** el motor de reglas declaraba `min(techo, estimado)` y ocultaba las sesiones por encima del techo.
- **E2:** la regeneración tras un NO_FACTIBLE por dolor no excluía lo que dolió.
- **Además, en el simulador:** el presupuesto por fases, seguir sin IA, regenerar tras una semana sin plan y la medición por fases.

**19. ¿Qué problemas requerirían nuevas decisiones funcionales?**

| Asunto | Conflicto | Alternativas |
|---|---|---|
| **Perfiles de 15 min** en GENERAL_FITNESS y LOSE_WEIGHT | El perfil admite 15 min (15–180), pero la composición que exigen V15, V25 y V20 no cabe en 17,25 min | **(a)** Subir el mínimo del perfil para esos objetivos (por ejemplo, 20–25 min); **(b)** relajar V15 en sesiones únicas cortas; **(c)** mantener el rechazo y que la app explique por qué no hay plan |
| **Memoria del dolor** | Hoy solo se excluye en la regeneración inmediata tras el NO_FACTIBLE | Decidir si el ejercicio que dolió se excluye N semanas o hasta que la persona lo reactive |
| **Un NO_FACTIBLE deja una semana sin plan** (U06 S6) | La persona se queda sin plan esa semana | **(a)** Publicar la semana sin la sesión del dolor; **(b)** regenerar en el mismo ciclo |
| **Presupuesto de IA** | 78 de 80 llamadas | Autorizar más si se quieren las 4 adaptaciones pendientes (≤ 12 llamadas) |

**20. ¿Cuánto tardó y costó cada generación?**

| | Por llamada | Por usuario |
|---|---|---|
| Duración | 46–171 s (mediana 87 s) | 60–462 s, según los reintentos |
| Coste | US$ 0,02–0,05 | — |

- **Total de la generación:** US$ 0,473.
- **El backend:** menos de 1 s por generación (`RENDIMIENTO_GENERACION_IA.md`).

**21. ¿Qué mejoras de rendimiento son posibles sin cambiar políticas?**
- Evitar reintentos (lo que más ahorra).
- Pedir menos texto de salida: la latencia correlaciona 0,96 con los tokens de salida.
- Un `prefer-wait` mayor.
- Un prompt de candidatos más compacto.

Cambiar el modelo o el esfuerzo de razonamiento no está autorizado.

**22. ¿Qué falta para integrar y usar el backend desde Flutter?**
1. Leer `adaptation.resultado` y `GET /users/me/plan/adaptations` en lugar de `adjustmentApplied`.
2. Mostrar una semana sin plan (NO_FACTIBLE o perfil inviable) con su motivo.
3. Tratar el 422 del perfil congelado.
4. Ofrecer el contexto de falta de tiempo al omitir: IMPREVISTO, SIN_TIEMPO_ESE_DIA o MENOS_TIEMPO_HABITUAL con sus minutos.
5. Decidir qué hacer con los perfiles inviables, por ejemplo validar los minutos mínimos por objetivo en el alta.
6. Aplicar las migraciones V38–V42 en la base del estudio. Esa es una decisión de despliegue, no de esta campaña.

## Pendientes

- Las decisiones de la pregunta 19.
- Más presupuesto para las adaptaciones no ejecutadas, si se quieren con IA real.
- Una campaña más larga, de unas 14 semanas, si se quiere observar una reincorporación dentro del horizonte.
