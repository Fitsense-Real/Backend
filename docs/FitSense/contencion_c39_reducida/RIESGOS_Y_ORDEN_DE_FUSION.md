# Riesgos de seguridad abiertos, orden de fusión y recomendaciones previas a producción

No se ha fusionado ni desplegado nada. Este documento es la propuesta para decidir la integración.

## 1. Riesgos de seguridad todavía abiertos

| # | Riesgo | Alcance | Mitigación actual | Qué falta |
|---|---|---|---|---|
| **R1** | **853 «Marcha rápida en el sitio»: impacto (HI) en duda y sin marcar** | 83 de 168 perfiles; cardio de pie de los perfiles LOSE_WEIGHT recuperados | Ninguna: se sirve como bajo impacto | Revisión del GIF. Si lleva HI, sale también para todo principiante (política por nivel) y hay que volver a comprobar la factibilidad de los perfiles LOSE_WEIGHT de casa. |
| **R2** | **Apoyo suspendido no contenido.** Quien declara «sin colgarse» puede recibir fondos y ejercicios de apoyo sobre brazos o antebrazos que no están ni contenidos ni marcados con `requires_hanging` | 13 ejercicios: 777 (8 perfiles), 1184, 1317, 457, 458, 459, 462, 920, 934, 1335 y los asistidos de rodillas 72, 73 y 78 | Solo los 5 de C39 (texto contradictorio). Los demás no tienen contradicción de texto | Decisión profesional sobre el apoyo suspendido (`catalogo_integral/PROPUESTA_APOYO_SUSPENDIDO.md`). Mientras tanto, mantener contenidos los 5 y decidir si se amplía la contención temporal a estos 13. La orden pide exactamente 8; no se amplía sin tu decisión. |
| **R3** | **68 banderas en duda sin marcar** | Destacan las de inversión (299, 224, 226, 822, 868), carga axial (514, 779, 278) y brazos sobre la cabeza (368, 617; 400 y 448 ya contenidos) | Ninguna para quien declara la restricción | Revisión profesional (lista C-2 de `REVISION_HUMANA_PRIORIZADA.md`). |
| **R4** | **La reparación de planes publicados puede quedar bloqueada por V48 o V50** | Toda semana publicada que necesite reparación por seguridad y contenga un ejercicio que V48 subió de nivel o al que V50 añadió un implemento | La sesión insegura sigue bloqueada: nadie la ejecuta | Decidir cómo se repara: tolerar en la revalidación los ejercicios ya publicados que solo dejaron de ser elegibles por dificultad o equipo, o regenerar. Hoy el plan 78 da NO_VALIDA por el 916. |
| **R5** | **Adherencia.** Una sesión bloqueada por seguridad vence como SKIPPED sin motivo | Toda sesión bloqueada que no se repara: semana empezada, NO_VALIDA, sesión en curso | Ninguna | Decidir un motivo de omisión «por seguridad» o excluir esas sesiones del denominador. Cambia la adherencia y requiere tu decisión: las órdenes previas prohíben tocar sus fórmulas. |
| **R6** | **H6: IN_PROGRESS pegado** e inicio de sesiones de fechas futuras | Sesiones abandonadas | La tarea de sesiones obsoletas libera tarde | Guardas de estado (diseño en la auditoría del caso 39, §I.4). |
| **R7** | **59 PENDIENTE_VISUAL** | 7 de suspensión o inversión (1068, 1191, 937, 1184, 65, 1294, 913) y 7 de impacto | Conservan texto y banderas anteriores; casi todos llevan HG, OH, INV o HI | Otra fuente visual o revisión profesional. |
| **R8** | **Impacto deducido del nombre** (1093) | Cualquier renombrado futuro | La huella de `CatalogoCalidadIT` detecta el cambio de clase | Confirmar el 1093 con el GIF; a medio plazo, guardar el impacto en una columna revisada. |
| **R9** | **Textos de V49 redactados por IA** | 346 ejercicios con instrucciones nuevas; 57 en P1 | Protocolo estricto, advertencias conservadas y probadas, sin contradicciones texto/bandera | Revisión humana de A-P1 antes de producción. |

## 2. Orden de fusión propuesto

| Paso | PR | Rama → base actual | Contenido |
|---|---|---|---|
| 1 | [#56](https://github.com/Fitsense-Real/Backend/pull/56) | `catalogo/integral-1-seguridad` → `generation` | V47 y V48 |
| 2 | [#57](https://github.com/Fitsense-Real/Backend/pull/57) | `catalogo/integral-2-textos` → `catalogo/integral-1-seguridad` | V49 |
| 3 | [#58](https://github.com/Fitsense-Real/Backend/pull/58) | `catalogo/integral-3-clasificacion` → `catalogo/integral-2-textos` | V50 |
| 4 | [#59](https://github.com/Fitsense-Real/Backend/pull/59) | `audit/correccion-integral-catalogo` → `catalogo/integral-3-clasificacion` | Controles y documentación |
| 5 | [#55](https://github.com/Fitsense-Real/Backend/pull/55) | `fix/contencion-seguridad-catalogo` → `audit/correccion-integral-catalogo` | Contención reducida a 8 |

**Por qué este orden.**
- Flyway aplica V47 → V48 → V49 → V50 por número.
- La PR #55 reducida **necesita** V49: sin ella, 18 ejercicios conservarían un texto contradictorio y quedarían sin contener.
- Fusionar la #55 antes que la pila sería inseguro. Fusionar la pila sin la #55 deja los 8 sin contención, como ocurre hoy en `generation`.
- Lo más seguro es fusionar la #55 justo después de la #59, sin desplegar entre medias.

**Reajuste de las ramas base** (las PR están apiladas):
1. **Usar «Create a merge commit»** en las cinco, no «Squash» ni «Rebase».
   - Con squash, la PR siguiente mostraría otra vez los commits ya fusionados y habría que integrar `generation` en su rama.
   - Las ramas se construyeron con merges, sin reescribir historia.
2. Tras fusionar la #56, **cambiar la base de la #57 a `generation`** antes de fusionarla. GitHub lo hace solo si se borra la rama `catalogo/integral-1-seguridad` al fusionar y está activo el borrado automático de ramas. Repetir con la #58 tras la #57, la #59 tras la #58 y la **#55 tras la #59**.
3. **Después de cada fusión, esperar el CI de `generation`**, que corre en cada push a `generation`, antes de fusionar la siguiente. Cambiar la base de una PR no relanza su CI (`pull_request` sin el tipo `edited`). El CI de `generation` es la comprobación de lo integrado.
4. Antes de cada fusión, comprobar que ninguna otra PR ha añadido una migración V47 o posterior a `generation`. V45 sigue reservada por la PR #17.

## 3. Recomendaciones antes de desplegar en producción

Imprescindibles:
1. **Revisión humana de los P1 de la lista A** (57), empezando por 853, 1093, 143, 116, 295 y los liberados con más exposición: 1152, 1095, 1156 y 1181.
2. **Decidir R1 (853)** y, si lleva HI, rehacer la comprobación de factibilidad de los perfiles LOSE_WEIGHT de casa.
3. **Medir la exposición en producción con las consultas de solo lectura:**
   - sesiones abiertas que la contención reducida bloquearía (`PLANES_PUBLICADOS_C39.md` §2);
   - sesiones que bloquearían las banderas nuevas de V47 (`catalogo_integral/PLANES_PUBLICADOS.md`);
   - semanas publicadas que contienen ejercicios de V48 (R4).

   Con esos números se decide la fecha y si hace falta reparar antes.
4. **Desplegar al cambio de semana**, con el menor número posible de semanas en curso, para reducir R4 y R5. Avisar a los participantes afectados por la consulta del punto 3.
5. **Decidir R5 (adherencia)** antes de la primera adaptación posterior al despliegue. Si no, una sesión que el participante no podía hacer contará como no hecha.

Muy recomendables:

6. **Decisión profesional sobre el apoyo suspendido (R2).** Resuelve los 5 contenidos por colgarse y los 13 no contenidos.
7. **Revisión de los 7 PENDIENTE_VISUAL de suspensión o inversión y de los 7 de impacto (R7).**
8. Guardas de H6 (R6).

No necesario antes del despliegue: listas A-P2/P3, C-3 (banderas de más, que solo afectan a la factibilidad) y el resto de B.
