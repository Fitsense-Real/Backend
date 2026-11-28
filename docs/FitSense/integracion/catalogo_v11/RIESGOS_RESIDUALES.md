# Riesgos residuales tras la integración (catálogo v1.1 en `generation`)

Parten de `../../catalogo_integral/premerge_final/RIESGOS_RESIDUALES.md`. Aquí se indica qué cambió con el merge.

## Problema

Al integrar v1.1 hay que revisar qué riesgos se cierran y cuáles siguen abiertos.

## Cerrados por la integración

| Riesgo | Estado |
|---|---|
| **R13** · `generation` puede avanzar antes del merge | **Cerrado.** La simulación se reconstruyó sobre `623c4fa` justo antes del primer merge. Los 11 árboles coinciden (`INFORME_MERGE_CATALOGO.md`) |
| **R5** · las omisiones impuestas no dejan su causa | **Reducido con V57**, integrada en `0d11278`: la causa queda persistida por ejercicio y por sesión. El indicador de adherencia no cambia: su tratamiento sigue siendo una **decisión metodológica pendiente** (`INTEGRACION_V57.md`) |

## Vigentes

| # | Riesgo | Mitigación | Qué lo cerraría |
|---|---|---|---|
| R1 | Catálogo **sin validación profesional**: **312 decisiones abiertas** | Contención de las incertidumbres y valores más restrictivos | Revisión profesional |
| R2 | Error de interpretación de la IA en ejercicios A | Dos lecturas independientes, controles permanentes y huella | Revisión profesional |
| R3' | P-03 por colocación **apagada en producción** | Valor por defecto seguro respecto de v1, que compara solo el total | Autorización expresa al desplegar |
| R4 | El 853 podría hacerse con rebote | El texto prohíbe saltar; P286 está abierta | Revisión profesional |
| R7 | Nadie recibe los 15 ejercicios de apoyo suspendido | V53-B apagada y contención mantenida | Flujo en Flutter y decisión de activar B |
| R8 | 14 perfiles de casa con solo peso corporal se quedan sin plan | NO_FACTIBLE legítimo por V20 | Ampliar el catálogo de peso corporal. **No se hace:** tras el cierre el catálogo no se modifica |
| R10 | Interruptores mal configurados al desplegar | Valores por defecto conservadores; V53 A y B a la vez impiden arrancar | Revisar las variables (`ESTADO_INTERRUPT0RES.md`) |
| R11 | Todo se ha medido con perfiles sintéticos | Escenarios deterministas y validador real | Evaluación con participantes |
| R12 | La contención reduce las alternativas al sustituir o reparar | No se publica nada | Ampliar las alternativas seguras |
| R14 | P-03 por colocación declara NO_FACTIBLE algunas regeneraciones seguras (usuario 17: el 295 de 2×12 → el 253 de 3×120 s) | Apagada en producción | Decidirlo al activarla |

## Nuevos tras la integración

| # | Riesgo | Mitigación | Qué lo cerraría |
|---|---|---|---|
| R15 | **Despliegue desde `generation`.** Al arrancar contra Neon se aplicarían las migraciones pendientes, de V47 a V57 (60 en total) | Railway no despliega desde `generation` (confirmación documentada); `main` no se ha tocado | Antes de desplegar: confirmar la rama en el panel, hacer copia de Neon y seguir `MIGRACIONES_Y_DESPLIEGUE.md` |
| R16 | H2: la IA recibe una cuota por grupo distinta de la que verifica V15. Provoca reintentos y latencia | Corrección apagada en #69 | Evaluación pareada pagada y decisión |
| R17 | La app espera 30 s y la generación tarda de 60 a 350 s | R-5 (409/429) evita los duplicados | Flujo asíncrono (`PROPUESTA_FLUJO_GENERACION.md`) |
| R18 | El catálogo queda congelado y solo puede cambiar con una versión nueva | Huella y `CatalogoCalidadIT` detectan cualquier cambio | Versión v1.2, si se decide |

## Resultado

No se conoce ningún defecto técnico de seguridad sin corregir. Los riesgos que quedan vigentes son limitaciones de medición, de validación profesional, de cobertura o de operación, y están documentados.
