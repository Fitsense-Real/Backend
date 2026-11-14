# Informe final: cierre de pendientes, seguridad y validación final del catálogo (orden de 14/11/2026)

Fuentes:
- `CIERRE_PENDIENTES.md` (hallazgos, seguridad, 775, dificultad e identidad).
- `INTEGRACION_SIN_DUPLICAR.md`.
- `REGRESION_CIERRE_PENDIENTES.md`.
- `cierre/V53_DEFINITIVO.md`.
- `cierre/pendientes/PLAN78_REPARACION.md` y `SESIONES_BLOQUEADAS.md`.
- `cierre/REGISTRO_DECISIONES_PENDIENTES.tsv` y `cierre/pendientes/FICHAS_PROFESIONALES.xlsx`.

## Las 20 respuestas

**1. ¿Se resolvieron los 6 hallazgos técnicos?** Sí, los seis tienen un resultado con evidencia del GIF completo:
- cinco «Corregido y validado técnicamente» (V52.1);
- uno «Correcto, con evidencia que descarta el hallazgo».

Ninguno queda abierto.

**2. Cada uno:**

| Id | Resultado |
|---|---|
| 231 | Corregido. Press de banca con agarre cerrado: tríceps, HORIZONTAL_PUSH. OH se conserva; quitarlo es decisión profesional |
| 367 | Corregido. Extensión de hombro con brazo estirado en polea baja, SHOULDER_EXTENSION |
| 1303 (instrucciones) | Correcto. «Silla romana» designa la silla del capitán; no hay cambio |
| 1323 | Corregido. Sin la frase de los «rodillos»; ya no queda elegible con instrucciones contradictorias |
| 1182 | Corregido. Peso corporal, sin cajón |
| 1017 | Corregido. Gemelos como músculos secundarios |

**3. ¿Cuántas de las 43 se cerraron?**
- **Ninguna con validación profesional.**
- **42 contenidas preventivamente**, entre ellas 868, al que V52.1 añade INV, y 1294, que pasa a retenido.
- **1 abierta y sin contención:** A-V53, el significado de «no colgarse».

**4. ¿Cuántas necesitan un profesional?** Las **43**.

**5. Ejercicios elegibles con incertidumbre material:**
- Los **10 de apoyo suspendido** (457, 458, 459, 462, 777, 920, 934, 1184, 1317 y 1335) siguen elegibles para quien declara «no colgarse».
- **1113**: dificultad 1 por V39, cuando R2 y V3 ven nivel 2. Es elegible para principiantes en 120 de 168 perfiles.
- Las 248 dificultades que R2 propone bajar **no** son incertidumbre de seguridad: el vigente es el nivel más alto.

**6. Protecciones temporales que siguen:**
- **INC-4C-03:** 813, 858, 861, 873, 885, 1192, **1294** y 1298.
- **INC-CAT-C39:** 131, 400 y 448 para OH; 1153, 1301, 1303, 1304 y 1306 para «no colgarse».
- **INC-CAT-1374.**
- **Banderas conservadoras pendientes de profesional:** HI en 854 (D2), 1083–1085, 1087, 1133, 1134, 1175 y 1177; INV en 171, 377, 840, 841 y 868; OH en 231.
- **Bloqueo de sesiones publicadas** por causa de seguridad.

**7. Decisión propuesta para V53:** la **opción B**, un permiso propio de «apoyo suspendido» que no cambia el significado de «no colgarse».
- Cubre solo la situación 2: el peso corporal sostenido sobre barras o aparatos, sin apoyo de pies (los 15).
- No se crean atributos para pies apoyados, pies suspendidos ni correas.
- 1301 queda como pregunta profesional, y contenido.
- **No se ha activado nada.**

**8. Permisos guardados:**
- No se reinterpretan en silencio.
- El permiso nuevo se inicializaría con el valor de «no colgarse», que es lo conservador, se marcaría como pendiente de confirmar y se pediría la confirmación en la app.
- Antes, el propietario mide el alcance con `cierre/v53/impacto_permisos_v53_solo_lectura.sql`. No se ha leído ningún dato real.

**9. ¿Tienen alternativas válidas los 14 perfiles del 775?** **No, sin cambiar de equipo.**
- El catálogo no tiene ninguna resistencia de espalda solo con peso corporal; el 775 necesita banco.
- Los remos invertidos piden barra, Smith, correas o anillas.
- **NO_FACTIBLE legítimo por V20.** Ni V20 ni V25 se han tocado.

**10. ¿HALL-PROG-01 sigue corregido tras integrar la pila?** **Sí**: las 9 pruebas HALL-PROG-01 están en verde en la integración.

**11. ¿V24 y V8C conservan todas sus garantías?** **Sí.**
- Todas las pruebas de V24 y V8C están en verde en la integración, y no se ha relajado nada.
- El único rechazo de V24 en la regresión (`conBarraSeConserva`) es V24 funcionando: una dosis heredada incorrecta.

**12. ¿Puede repararse correctamente el plan 78?**
- **Sí, con la propuesta** de #64 (interruptor encendido): solo se sustituye el 1303, se conservan el 916 y el 910 con constancia en el snapshot, sin más dosis y con la versión 1 intacta.
- Con el contrato aprobado vigente (interruptor apagado), **no**.
- La propuesta cambia ese contrato, así que necesita aprobación.

**13. Cambios necesarios en sesiones bloqueadas y adherencia:**
- **S1, preparada (V54):** guardar la causa del bloqueo al vencer, sin tocar la adherencia.
- **S2, para decidir:** reparar antes de que venza.
- **S3, metodológicas:** S3a, decir a la adaptación que la sesión no se ofreció; S3b, excluirla del denominador.
- Hoy, una sesión bloqueada cuenta como 0 en la adherencia principal, baja la cobertura y puede provocar adaptaciones equivocadas.
- No se ha cambiado la fórmula ni los umbrales.

**14. Decisiones profesionales sin resolver:** **312** de 323 filas, ninguna con validación profesional. Son 306 de profesional y 6 de propietario y profesional. Por bloques:
- 249 de dificultad;
- 36 de seguridad (35 contenidas y A-V53 sin contención);
- 23 de identidad;
- 4 de texto.

**15. Ejercicios corregidos en esta orden:**
- 231, 367, 620, 1017, 1182 y 1323 (V52.1).
- 868: se le añade INV (contención).
- 1294: retenido.

**16. Migraciones y PR preparadas:**

| PR | Contenido |
|---|---|
| #63 | `V52_1__catalogo_cierre_pendientes_tecnicos.sql`, retención de 1294, expediente, registro con estado final, fichas, documentos |
| #64 (propuesta, no fusionar sin aprobación) | Interruptor del plan 78 y `V54__vencimiento_con_bloqueo_de_sistema.sql` |
| #61 (actualizada) | Incluye #63, huella recapturada, `V53_DEFINITIVO.md`, registro maestro y matriz con V52.1. «VERIFICADO» pasa a «REVISADO_IA_SIN_HALLAZGO» |

**17. Resultados finales de la regresión:**

| Rama o medida | Resultado |
|---|---|
| Integración | 1.468 unitarias en verde. 727 de integración con **2 fallos** (`HerenciaD8IT.dolorNoAprovechaD8` y `AdaptacionRegresionIT.conBarraSeConserva`), los dos causados por V52.1 y **sin resolver** |
| #62 | Verde (1.431 + 617) |
| Rejilla de 168 | 168 de 168, con V53 apagado y encendido |
| Hipertrofia (312) | 298 válidos + 14 NO_FACTIBLE esperados |
| Referencia | 72 válidos |
| Procedencia V1–V53 | Correcta |
| CI de #61 | Rojo por los mismos 2 fallos |
| Propuesta #64 | 1.468 unitarias en verde; 723 de integración con solo los 2 fallos heredados y 0 errores (los 48 errores de la primera pasada se corrigieron registrando V54 como migración de solo esquema) |

**18. Orden de integración, sin duplicar #62:**
1. #62 → `generation`.
2. #56 → #57 → #58 → #59 → #55.
3. #63 → base de #60, y después #60.
4. #61.
5. #64, solo si se aprueba.

`b65452c` (#60) y `b0267d0` (#62) tienen el mismo `patch-id`: el código no se duplica, y el árbol integrado es igual al de #61.

**19. Riesgos antes de producción:**
- **(a)** Dos pruebas de integración en rojo, pendientes de decisión.
- **(b)** Ninguna decisión profesional validada (312).
- **(c)** Los 10 de apoyo suspendido elegibles para «no colgarse».
- **(d)** 1113 por debajo del nivel que ven R2 y V3.
- **(e)** Plan 78 y sesiones bloqueadas: no se pueden reparar con el contrato vigente, y vencen como SKIPPED sin causa.
- **(f)** Impacto de V53 y de los planes publicados sin medir en datos reales.
- **(g)** 14 perfiles de hipertrofia en casa sin plan (775).
- **(h)** Retenciones y contenciones temporales que reducen la variedad.

**20. ¿Cumple el catálogo todos los criterios para aprobar la fusión?** **No.**

## Veredicto

# CATÁLOGO NO APTO PARA MERGE

**Qué falta, exactamente:**
1. **CI en verde.** Decidir sobre `HerenciaD8IT.dolorNoAprovechaD8` y `AdaptacionRegresionIT.conBarraSeConserva`. La propuesta es que comprueben la garantía real (sin excluidos que reaparezcan, sin más trabajo; relleno de zona pequeña). La alternativa es otra decisión del propietario. No se han tocado sin autorización.
2. **Validación profesional** de las 43 decisiones de seguridad, en especial A-V53 y 1113, y de las 312 filas pendientes (`FICHAS_PROFESIONALES.xlsx`), con nombre, función y fecha.
3. **Decisión del propietario sobre V53** (opción A o B, permisos guardados), después de medir el alcance con el script de solo lectura.
4. **Aprobación o rechazo de la propuesta #64**: el contrato de reparación del plan 78 y V54. Decidir también S2, S3a y S3b.
5. **Aceptar la consecuencia del 775** (14 perfiles NO_FACTIBLE), o decidir un cambio de equipo o de catálogo.
