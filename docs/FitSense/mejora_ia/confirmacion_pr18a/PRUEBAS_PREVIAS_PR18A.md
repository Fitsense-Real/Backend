# Pruebas previas · confirmación de pr-1.8a (gratuitas, antes de cualquier llamada pagada)

**Rama:** `experimento/confirmacion-pr18a-final`, código en `f95b218`. Es `generation` `543932a` más pr-1.8a apagada, el modo `solo-generacion` del arnés y `PromptPr18aTest`.

| Ejecución | Resultado |
|---|---|
| Unitarias completas | **1334 pruebas, 0 fallos, 1 omitida** (DEV-R, opcional con Replicate). Son las 1331 de `generation` más las 3 de `PromptPr18aTest` |
| Integración completa (`verify -Pintegration`) | **593 pruebas, 0 fallos, 0 errores, 44 omitidas** (opcionales) |
| `PromptPr18aTest` | 3/3: PR-1.7 idéntico por defecto en los 4 objetivos; pr-1.8a = PR-1.7 + las líneas V15/V26 antes de los datos; pr-1.8, pr-1.8b, pr-1.8c y pr-9 se rechazan |
| Identidad de prompts, 20 perfiles, en modo solo lectura | PR-1.7 de la rama = PR-1.7 de `ab04e67` = valor por defecto de la rama: **idénticos byte a byte**. pr-1.8a de la rama = pr-1.8a de `ab04e67`: **idénticos byte a byte** |
| Contra los prompts enviados de verdad a Replicate | pr-1.8a: 19/19 iguales a los primeros intentos de A1. PR-1.7: 19/19 iguales a los de C2 |
| Referencia determinista (`DET_GEN`) | Idéntica a la de CONF-IA: la generación por reglas no cambia |

La integración cubre:
- V1–V27 en la generación inicial de los cuatro objetivos, con V24 y V25 (referencias y rejilla);
- V8C corregido, con adaptación localizada y expandida;
- PLAN-REV-2.0, progresión, recuperación, reincorporación y conservación de sesiones;
- las pruebas de fecha.

Sin fallos.
