# 06 · Pruebas TOO_DIFFICULT (VARIANTE_MAS_SENCILLA_O_CARGA), escenarios 1-15

**Commit:** `dea906a`. Las pruebas no tocan ninguna prueba de referencia.

| Clase | Tipo | Resultado |
|---|---|---|
| `LimiteDificultadTooDifficultTest` | unitaria; V8C real, H1 multiconjunto (la regla por defecto) y las tres estrategias | **20/20**, 0 omitidas |
| `LimiteDificultadTooDifficultIT` | de extremo a extremo; flujo real de adaptación, V8C, validación estricta (V1-V25, 4D/V24, V20), PostgreSQL efímero, IA **simulada**; límite `on` solo en esta prueba | **5/5**, 0 omitidas |
| `LimiteDificultadTest` (ya existía en #38) | unitaria | 12/12 |

Logs: `evidencia/too_difficult_it.log`.

## 1. Matriz de escenarios

Lectura de las columnas:
- **A** = off; **B** = on; **C** = preferencia.
- «✔» = la propuesta se acepta; «✘» = V8C la rechaza.
- En los rechazos, la prueba exige además dos cosas:
  - que el **único** motivo sea el límite (sin él, la propuesta se acepta);
  - que el mensaje nombre al sustituto que sube y a ningún otro.

| # | Escenario | Retirados (dif) → añadidos (dif) | A | B | C | Prueba |
|---|---|---|:-:|:-:|:-:|---|
| 1 | 3 → 2 | 70301 (3) → 56 (2) | ✔ | ✔ | ✔ | `escenario[01]` |
| 2 | 2 → 1 | 380 (2) → 72 (1) | ✔ | ✔ | ✔ | `escenario[02]` |
| 3 | 2 → 2 | 380 (2) → 56 (2): **la igualdad sigue permitida (no H12-A)** | ✔ | ✔ | ✔ | `escenario[03]` |
| 4 | 1 → 1 | 933 (1) → 73 (1) | ✔ | ✔ | ✔ | `escenario[04]` |
| 5 | 1 → 2 | 580 (1) → 56 (2) | ✔ | ✘ | ✘ | `escenario[05]` |
| 6 | 2 → 3 | 380 (2) → 70301 (3) | ✔ | ✘ | ✘ | `escenario[06]` |
| 7 | varias sustituciones en una sesión | {70301 (3), 933 (1)} → {56 (2), 73 (1)} | ✔ | ✔ | ✔ | `escenario[07]`; también `07b`: {380 (2), 933 (1)} → {72 (1), 56 (2)}, el emparejamiento no depende del orden de la lista |
| 8 | dos retirados de patrones distintos | {HP 380 (2), HPULL 300 (1)} → {HP 72 (1), HPULL 780 (2)}. **El máximo y el promedio coinciden**, pero en HORIZONTAL_PULL sube 1 → 2 | ✔ | ✘ | ✘ | `escenario[08]`: el mensaje nombra 780, no 72 |
| 9 | dos sustitutos del mismo patrón | {70301 (3), 933 (1)} → {56 (2), 380 (2)}. **El promedio coincide** (2 = 2), pero no hay emparejamiento válido | ✔ | ✘ | ✘ | `escenario[09]` |
| 10 | una válida y otra más difícil | {380 (2), 933 (1)} → {72 (1), 70301 (3)} | ✔ | ✘ | ✘ | `escenario[10]`: solo se nombra 70301 |
| 11 | no existe sustituto ≤ | La IA propone 1114 (1) → **124 (2)**, el primer más difícil del mismo patrón y zona | se aceptaría (deducido de V8C con A en el escenario 5; esta IT solo corre con `on`) | **3 rechazos → CONSERVED** | igual que B | `LimiteDificultadTooDifficultIT.masDificilRechazadoYConservado` |
| 12 | se permite reducir la dosis | 380 3x10 → 3x10, la sesión pasa a 2x10 sin sustituir; y 12b: sustituto igual + menos series | ✔ | ✔ | ✔ | `reduccionDeDosis`, `sustitucionYReduccion` |
| 13 | V20 impide reducir la dosis | Todos los ejercicios a 1 serie y 30 s: la sesión dura 690 s y el mínimo es 1.890 s (70 % de 45 min) | ✘ por V20/V9/V24 | igual | igual | `v20Bloquea`: el motivo contiene V20 y **no** el límite |
| 14 | 4D impide reducir la dosis | Estructural 3x10 → 3x3: «4D v2 exige 3x10, descanso 90 s (HYPERTROPHY)» | ✘ por V24/4D, V16, V9 | igual | igual | `cuatroDBloquea`: el motivo contiene 4D y **no** el límite |
| 15 | la sesión conserva sus ejercicios | Escenarios 11, 13 y 14: tras 3 intentos, `generation_source = CONSERVED` y **copia exacta** de la semana medida (`assertCopiaExacta`) | — | ✔ | ✔ | IT |

Ids reales del catálogo V44_2, con dos excepciones:
- 70301 y 70302 son fichas sintéticas de dificultad 3, solo para la prueba unitaria;
- la IT usa los ids y el catálogo reales de la base efímera.

## 2. Validación conjunta con H1 (fase 4.2)

La comparación de dificultad se hace por **emparejamiento** dentro de cada patrón (`ConservationValidator.revisarDificultad`):
- se ordenan retirados y añadidos de mayor a menor;
- cada añadido no puede superar al retirado de su posición.

Por el argumento de intercambio, ese emparejamiento existe si y solo si existe **alguno** válido. Los escenarios 8 y 9 demuestran que **no** se compara el máximo ni el promedio.

Lo que la prueba deja comprobado en cada propuesta:

| Aspecto | Comprobación |
|---|---|
| Patrón original y nuevo; cantidad por patrón | H1 multiconjunto sigue actuando: la prueba `h1SigueActuando` (380 HP + 389 VPULL → 72 + 56 HP) se **rechaza por H1, no por el límite**, con las tres estrategias. |
| Dificultad original y nueva | Matriz §1. |
| Dosis, descansos, duración | 12, 12b, 13 y 14: el límite no cambia el veredicto de las demás reglas (en 12b, mismos problemas con off y on). |
| Elegibilidad | En la IT, los sustitutos salen de `available_exercises` real del participante. |
| Sesiones señaladas y no señaladas | `assertNoSenaladasIntactas` en la publicación con límite (`menorOIgualPublicado`). |
| V8C, V20, V24 | Motivos capturados en los tres intentos (`feedbacks(3)`). |
| Estado V32 | `generation_source` y `ai_calls` de `plan_adaptation_attempts`. |

## 3. Observación de trazabilidad

Previa al límite, va al 14 como R-5. Cuando los tres intentos se rechazan y la semana se **conserva**, se registra así:
- `plan_adaptation_attempts.rejection_reason` queda **NULL**, porque la semana no se rechazó: se conservó;
- `conservation_report` es el de la copia conservada.

Los motivos concretos de cada rechazo de la IA solo viajan en la retroalimentación del reintento y en el log. No quedan en la base.

Esto ya ocurría con el límite apagado (cualquier rechazo de V8C, V20 o V24). El límite no lo empeora, pero añade un motivo más que no se persiste.
