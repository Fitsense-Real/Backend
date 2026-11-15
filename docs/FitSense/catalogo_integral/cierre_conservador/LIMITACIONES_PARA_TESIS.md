# Limitaciones para la tesis (versión técnica conservadora del catálogo)

Este documento separa lo que se hizo de lo que no se hizo, para que la tesis lo cite sin exagerar.

## 1. Tres cosas distintas

| | ¿Se hizo? | Qué es exactamente |
|---|---|---|
| **Auditoría técnica** | **Sí** | Revisión asistida por inteligencia artificial, compuesta por:<br>• dos lecturas visuales de los 1.219 GIF activos (R2 y V3) y un coordinador;<br>• comprobación de coherencia entre nombre, instrucciones, equipamiento, patrón, dificultad y banderas de seguridad;<br>• migraciones de corrección trazables (V47–V52.1);<br>• controles automáticos permanentes;<br>• pruebas funcionales y de seguridad de generación, adaptación, progresión y reincorporación |
| **Controles conservadores** | **Sí** | Las incertidumbres materiales que la auditoría no pudo resolver se restringieron. No se resolvieron:<br>• 28 ejercicios excluidos de las planificaciones nuevas (8 INC-4C-03 y 20 de identidad no determinada);<br>• 21 con contención condicionada al perfil (apoyo suspendido, 1113 para principiantes, INC-CAT-C39 e INC-CAT-1374);<br>• 252 con su valor vigente más restrictivo (dificultad o bandera) frente a la propuesta de la IA |
| **Validación profesional** | **No** | Ningún entrenador, fisioterapeuta ni especialista ha validado el catálogo, ni siquiera parcialmente. Las **312 decisiones** que la requieren siguen abiertas, con su ficha preparada (`../cierre/pendientes/FICHAS_PROFESIONALES.xlsx`) y los campos de decisión vacíos |

## 2. Lo que la tesis no puede afirmar

- Que el catálogo, o alguno de sus ejercicios, fue **aprobado por expertos**.
- Que los ejercicios son **seguros para cualquier persona**. FitSense no hace valoración clínica: aplica restricciones declaradas por el usuario y reglas por edad y nivel.
- Que la clasificación A («elegible») equivale a una **validación**. Solo significa que la revisión asistida por IA no encontró una incertidumbre material.
- Que un ejercicio **bloqueado o excluido** quedó revisado o resuelto. Se excluyó precisamente porque no se pudo resolver.
- Que las dificultades son **correctas**. Se conservó el valor más restrictivo de los disponibles, que es un criterio de precaución y no una medida validada.

## 3. Limitaciones concretas que conviene declarar

1. **La revisión visual es de IA.** Los GIF son animaciones cortas, a veces fundidos de pocas poses. La IA puede equivocarse al interpretarlas, y por eso se excluyeron los 27 casos de identidad ambigua.
2. **Los cortes conservadores (edad, nivel, banderas) son criterio de diseño**, no guías clínicas validadas.
3. **La cobertura se reduce.** La contención deja sin plan a **4 de 168** perfiles de la rejilla (pérdida de factibilidad medida, ver `IMPACTO_FACTIBILIDAD.md`) y cambia la composición de **167** planes. En la rejilla de 312 de hipertrofia, los 14 NO_FACTIBLE (775) no cambian.
4. **No hay datos reales.** Todas las mediciones son con perfiles sintéticos y deterministas en una base PostgreSQL vacía. No se ha leído ningún dato de usuarios reales.
5. **La adherencia con sesiones bloqueadas.** Una sesión que el sistema bloquea por seguridad y vence se registra como SKIPPED sin motivo y cuenta como no hecha. La versión candidata guarda la causa (V54), pero **no** cambia el indicador: ver `IMPACTO_PLANES_PUBLICADOS.md` §3 y el procedimiento para la evaluación.
6. **P-03 nivel 2 compara el trabajo total.** Hay un contraejemplo determinista de una colocación que vuelve a la base sin que suba el total. La regla por colocación está preparada, pero apagada: es una decisión pendiente.
7. **El permiso de apoyo suspendido no se puede confirmar todavía.** Flutter no lo pregunta, así que hoy nadie recibe los 15 ejercicios de apoyo suspendido.

## 4. Texto propuesto para el informe académico

Ajustado a lo que demuestran los resultados finales:

> «El catálogo de ejercicios utilizado por FitSense (1.219 ejercicios activos) fue sometido a una auditoría técnica asistida por inteligencia artificial. Esta incluyó la revisión de los recursos visuales de cada ejercicio, la comprobación de la coherencia entre sus atributos (nombre, instrucciones, equipamiento, patrón de movimiento, dificultad y restricciones de seguridad), la corrección trazable de las inconsistencias detectadas y la ejecución de pruebas funcionales y de seguridad de la generación y la adaptación de planes. Para la versión de evaluación (FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1) se adoptó un criterio conservador: se excluyeron de las nuevas recomendaciones los ejercicios cuya identidad no pudo determinarse (28) y se restringieron, según el perfil del usuario, los que presentaban otras incertidumbres materiales (21). En los casos dudosos se conservó la dificultad o la restricción de seguridad más exigente. No se realizó una validación especializada formal por profesionales del ejercicio o de la salud, y 312 decisiones sobre el catálogo permanecen pendientes de esa validación. Esta ausencia constituye una limitación del estudio y deberá abordarse en trabajos posteriores.»
