# Pruebas de H2 sin IA (fase 2)

## Problema

Antes de gastar, hay que demostrar sin Replicate que:

- la corrección dice la regla correcta;
- apagada, no cambia nada;
- los validadores siguen rechazando lo inválido.

## Pruebas

Ejecutadas en local sobre `fix/h2-contrato-prompt-v15`, con el cambio de esta orden incluido. PostgreSQL 16 efímero.

| Prueba | Qué comprueba | Resultado |
|---|---|---|
| `ContratoV15PromptTest#apagadoIdentico` | Apagado: instantánea y prompt **idénticos byte a byte** a PR-1.7 en los 4 objetivos, sin campos nuevos | ✅ |
| `#encendidoSinContradicciones` | Encendido: tabla = `RepartoV15` para cada n; complementos = V15; mínimo ≤ `max_colocaciones_dia` | ✅ |
| `#sinMinimoConCapacidades` | Encendido: no se comunica el mínimo mal calculado | ✅ |
| `#textoEncendido` | El prompt ya no dice la frase histórica y sí la de V15 | ✅ |
| `#falloRapido` | Si cambia el texto histórico, la variante falla | ✅ |
| **`#adaptacionNoLeeElInterruptor`** (nueva) | La adaptación no depende del interruptor; la proyección V2.1 es la histórica en los 4 objetivos | ✅ |
| `RepartoV15Test` (2) | Topes y tabla | ✅ |
| `ContratoPromptV15IT#sondaV15` | Validador real: la función predice exactamente a V15 (n = 2…6, k = 1…n) | ✅ |
| **`ContratoPromptV15IT#iaSimuladaCuatroObjetivos`** (nueva) | Ver abajo | ✅ |
| `ConfiguracionEvaluadaTest`, `PromptHistoricoHuellaTest`, `PromptCapacidadesTest`, `PromptRev2Test` | La configuración evaluada y los prompts históricos no cambian | ✅ |
| **Suite unitaria completa** | 1548 pruebas: las 1540 de `generation` más 8 de H2 | ✅ 0 fallos, 1 omitida |
| **`EvaluacionPareadaH2IT`, modo ENSAYO** (respuestas **simuladas**) | La mecánica del arnés. No es un resultado de la IA (`DISENO_EVALUACION_PAREADA.md`) | ✅ |

## IA simulada en los cuatro objetivos (positiva y negativa, con el validador real)

**Perfiles:** 16, es decir, 4 objetivos × principiante/intermedio × gimnasio/casa.

**Método:**

1. Se usa la semana del motor de reglas sobre el contexto real.
2. Una **IA simulada obediente** compone cada sesión donde aplica la regla de la mitad (enfoque de 3 grupos o más, 4 ejercicios de trabajo o más).
3. Concentra en el grupo con más material exactamente lo que le permite cada contrato.
4. **Decide el validador real.**

| Contrato obedecido | Sesiones | Rechazos V15 por la mitad |
|---|---|---|
| PR-1.7: `max_per_body_part`; `null` = sin tope comunicado | 51 | **51** |
| **PR-1.9:** `max_per_body_part_por_n_trabajo[n]` | 51 | **0** |
| PR-1.9 + 1, uno más de lo permitido (control negativo) | 51 | **51** |

Lectura de la tabla:

- **Positiva:** obedecer el contrato nuevo nunca provoca un rechazo V15.
- **Negativa:** V15 sigue rechazando cualquier sesión que se pase del tope.
- Con la política vigente, PR-1.7 no comunica ningún tope en esas sesiones, así que una IA que solo siguiera la instrucción podría concentrar todo en un grupo. Es la forma actual de H2 (`REVISION_H2.md`).

**Lo que esta prueba no demuestra:** que la IA real obedezca el contrato. Eso solo lo mide la evaluación pareada.

## Comportamiento de adaptación

La adaptación no lee el interruptor, así que no cambia en ninguna configuración. Lo comprueban:

- la prueba unitaria `adaptacionNoLeeElInterruptor`;
- la suite completa, que incluye todas las pruebas de adaptación con el interruptor apagado.

## Resultado

La corrección supera todas las pruebas automatizadas sin coste.

## Pendientes

El CI de #69 en la cabeza nueva.
