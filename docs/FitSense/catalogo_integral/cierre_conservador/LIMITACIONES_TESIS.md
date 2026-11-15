# Limitaciones para la tesis (versión técnica conservadora del catálogo)

Este documento separa lo que se hizo de lo que no se hizo, para que la tesis lo cite sin exagerar. Las cifras corresponden a `FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1` (ver `VERSION_CONGELADA.md`).

## 1. Tres cosas distintas

| | ¿Se hizo? | Qué es exactamente |
|---|---|---|
| **Auditoría técnica** | **Sí** | Revisión asistida por inteligencia artificial:<br>• dos lecturas visuales de los 1.219 GIF activos (R2 y V3) y un coordinador;<br>• comprobación de coherencia entre nombre, instrucciones, equipamiento, patrón, dificultad y banderas de seguridad;<br>• migraciones de corrección trazables (V47–V52.1 y V56);<br>• controles automáticos permanentes;<br>• pruebas funcionales y de seguridad de generación, adaptación, progresión y reincorporación |
| **Controles conservadores** | **Sí** | Las incertidumbres materiales que la auditoría no pudo resolver se **restringieron, no se resolvieron**:<br>• **27** ejercicios excluidos de las planificaciones nuevas (8 de INC-4C-03 y 19 de identidad no determinada);<br>• **21** con contención condicionada al perfil (15 de apoyo suspendido, 1113 para principiantes, INC-CAT-C39 e INC-CAT-1374);<br>• **252** con su valor vigente más restrictivo (dificultad o bandera) frente a la propuesta de la IA |
| **Validación profesional** | **No** | Ningún entrenador, fisioterapeuta ni especialista ha validado el catálogo, ni siquiera parcialmente. Las **312 decisiones** que la requieren siguen abiertas, con su ficha preparada (`../cierre/pendientes/FICHAS_PROFESIONALES.xlsx`) y los campos de decisión vacíos |

## 2. Lo que la tesis no puede afirmar

- Que el catálogo, o alguno de sus ejercicios, fue **aprobado o validado por expertos**. Tampoco que está **certificado**.
- Que los ejercicios son **seguros para cualquier persona**, ni que el sistema **garantiza la seguridad**. FitSense no hace valoración clínica: aplica las restricciones que declara el usuario y reglas por edad y nivel.
- Que la clasificación A («elegible») equivale a una **validación**. Solo significa que la revisión asistida por IA no encontró una incertidumbre material.
- Que un ejercicio **excluido o contenido** quedó revisado, resuelto o validado. Se excluyó precisamente porque no se pudo resolver.
- Que las **312 decisiones profesionales** están tomadas o validadas. Están abiertas.
- Que las dificultades son **correctas**. Se conservó el valor más restrictivo de los disponibles: es un criterio de precaución, no una medida validada.
- Que el ejercicio **853** quedó validado. Su identidad se determinó con la animación completa y su texto se corrigió (V56), pero su decisión profesional (P286) sigue abierta.

## 3. Limitaciones concretas que conviene declarar

1. **La revisión visual es de IA.** Los GIF son animaciones cortas, a menudo fundidos de pocas poses, y la IA puede equivocarse al interpretarlas. Por eso se excluyeron los 19 casos de identidad no determinada; 3 más ya estaban retenidos por INC-4C-03.
2. **Los cortes conservadores (edad, nivel, banderas) son criterio de diseño**, no guías clínicas validadas.
3. **Efecto en la cobertura.** En la rejilla de 168 perfiles, la contención **no deja ningún perfil sin plan** (168/168 con plan; los 4 que perdía se recuperaron con V56), pero cambia la composición de 163 planes. En la referencia de 72, cambian los 72 planes y todos siguen siendo válidos. En la rejilla de 312 de hipertrofia siguen los 14 NO_FACTIBLE de casa con solo peso corporal (775 con banco), que no dependen de esta versión.
4. **No hay datos reales.** Todas las mediciones son con perfiles sintéticos y deterministas en una base PostgreSQL vacía. No se ha leído ningún dato de usuarios reales.
5. **Adherencia con omisiones impuestas por el sistema.**
   - Una sesión que el sistema bloquea por seguridad y vence se registra como SKIPPED sin motivo y cuenta como no hecha. V54 guarda la causa, pero **no** cambia el indicador.
   - Un ejercicio marcado por dolor registrado solo se puede omitir, y la omisión cuenta como cualquier otra.
   - Las dos se pueden separar en el análisis (consulta en `VALIDACION_DOLOR_PUBLICADO.md` §5). Si se hace, hay que declararlo.
6. **P-03 nivel 2 compara el trabajo total.** Hay un contraejemplo determinista de una colocación que vuelve a la base sin que suba el total. En los recorridos medidos no se ha observado. La regla por colocación está preparada, pero apagada: es una decisión pendiente (`VALIDACION_P03.md`).
7. **El permiso de apoyo suspendido no se puede confirmar todavía.** Flutter no lo pregunta, así que hoy nadie recibe los 15 ejercicios de apoyo suspendido.
8. **La referencia de evaluación es la conservadora.** Los planes de la versión evaluada se comparan con `generacion_referencia_conservador_v1.txt`, no con la referencia histórica (`REFERENCIA_CATALOGO_CONSERVADOR.md`).

## 4. Texto propuesto para el informe académico

Ajustado a lo que demuestran los resultados finales:

> «El catálogo de ejercicios utilizado por FitSense (1.219 ejercicios activos) fue sometido a una auditoría técnica asistida por inteligencia artificial. Esta incluyó la revisión de los recursos visuales de cada ejercicio, la comprobación de la coherencia entre sus atributos (nombre, instrucciones, equipamiento, patrón de movimiento, dificultad y restricciones de seguridad), la corrección trazable de las inconsistencias detectadas y la ejecución de pruebas funcionales y de seguridad de la generación y la adaptación de planes. Para la versión de evaluación (FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1) se adoptó un criterio conservador: se excluyeron de las nuevas recomendaciones los ejercicios cuya identidad no pudo determinarse o que ya estaban retenidos (27) y se restringieron, según el perfil del usuario, los que presentaban otras incertidumbres materiales (21). En los casos dudosos se conservó la dificultad o la restricción de seguridad más exigente. No se realizó una validación especializada formal por profesionales del ejercicio o de la salud, y 312 decisiones sobre el catálogo permanecen pendientes de esa validación. Esta ausencia constituye una limitación del estudio y deberá abordarse en trabajos posteriores.»
