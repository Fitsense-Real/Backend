# Conclusión de la validación con Replicate real

Actualizada el 18/11/2026, tras la reanudación. Base: `generation` = `a1ccf53` (merge de Fitsense-Real/Backend#76).

## 1. Veredicto

**La generación asíncrona con IA real sigue sin validarse.** El motivo es externo y persiste: el modelo
`openai/gpt-5-structured` devuelve E002 (`1cah9wlWR9`, «No sid») en todas las peticiones de la cuenta. Se ha
comprobado:

- desde el 17/11 a las 16:47 UTC;
- de nuevo el 18/11 a las 01:23 UTC, con la única sonda autorizada para la reanudación;
- y otra vez el 18/11 a las 03:58 UTC, **después** de que se diera por resuelta la incidencia del 17/11. Esa sonda quedó 114 s en cola y luego falló con el mismo E002.

**Comprobado el 18/11:** el mismo error se reproduce en el Playground web de Replicate con la sesión del titular. Queda probado que no lo causa FitSense, ni su token ni su proxy. El fallo está en Replicate, para esta cuenta y este modelo. Su causa concreta (la cuenta o el modelo) **no está comprobada**. Puede ser la cuenta, el proveedor (los agregadores registran una incidencia de
Replicate abierta desde el 17/11) o ambas cosas. La entrada de FitSense está descartada.

**Lo que sí queda cerrado:**

1. **HR-1 corregido e integrado.**
   - #76 está fusionada (`a1ccf53`).
   - El CI de `generation` está verde (runs 375 y 376).
   - Regresión: 1.576 unitarias y 821 de integración, 0 fallos.
2. **El ensayo sin coste se repitió sobre `a1ccf53` y es conforme:**
   - SIGTERM con dos predicciones en curso: 0 cancelaciones, ambas reanudadas por el mismo id, sin respaldo
     publicado durante el apagado;
   - vencimiento con adopción del resultado tardío;
   - 9 POST para 9 ids, 1 plan por usuario.
3. **Degradación ante un proveedor caído**, ya demostrada el 17/11: el libro cuadra 1 a 1 y el participante recibe un
   plan válido del motor de reglas.

## 2. Tabla de evaluación

| Escenario | Estado | Predicciones nuevas | Coste | Resultado |
|---|---|---|---|---|
| Ensayo sin coste (`a1ccf53`) | PASS | 0 reales (9 simuladas) | USD 0 | SIGTERM, reanudación, vencimiento y adopción conformes |
| Sonda mínima (18/11) | **FAIL** | 1 | estimado USD 0,00 (0 tokens) | E002, misma referencia. Campaña detenida. |
| G1 | No ejecutado | 0 | USD 0 | Bloqueado por la sonda |
| G2 | No ejecutado | 0 | USD 0 | Bloqueado por la sonda |
| G3 | No ejecutado | 0 | USD 0 | Bloqueado por la sonda |
| Sonda tras la incidencia resuelta (18/11 03:56) | **FAIL** | 1 | estimado USD 0,00 (0 tokens) | E002 tras 114 s en cola. Campaña detenida. |
| Sonda a petición del usuario (18/11 14:00) | **FAIL** | 1 | estimado USD 0,00 (0 tokens) | E002. Campaña detenida. |
| Sonda a petición del usuario (18/11 14:40) | **FAIL** | 1 | estimado USD 0,00 (0 tokens) | E002. |
| **Total del 18/11** | | **4** | **estimado USD 0,00** | Cargo real pendiente de confirmar en el panel |
| Sonda a petición del usuario (19/11 03:28) | **FAIL** | 1 | estimado USD 0,00 (0 tokens) | E002. |
| *Acumulado de la campaña (17–19/11)* | | *9 (2 del backend + 7 sondas)* | *estimado USD 0,00; real pendiente* | *Las 9 `failed`, 0 tokens* |

**Presupuesto:**
- tope operativo de la reanudación: USD 1,50 o 12 predicciones. Usado: 5 predicciones (sondas), sin coste estimado;
- techo absoluto: USD 10. Disponible: prácticamente todo, pendiente del cargo real.

## 3. Para continuar

1. **Panel de Replicate**, con el navegador del titular:
   - Billing: saldo, límite de gasto y método de pago;
   - el cargo de las 5 predicciones;
   - el estado de la incidencia en el sitio de estado de Cloudflare / Replicate.
2. **Solicitud de soporte:** `SOLICITUD_SOPORTE_REPLICATE.md`, actualizada con los 5 ids y la pregunta sobre la
   incidencia del 17/11.
3. **Reanudación:** cuando el panel o soporte confirmen la solución, seguir `PROPUESTA_REANUDACION.md` sobre
   `a1ccf53`:
   - primero 1 sonda;
   - si pasa, G1 → G2 → G3;
   - coste previsto: 3–7 predicciones y menos de USD 0,30.
4. **H2 y adaptación con IA real:** no conviene empezarlas mientras el proveedor falle; usan el mismo modelo.

No se ha desplegado nada ni se han usado datos reales. No se han cambiado el modelo, sus parámetros, los validadores,
el catálogo ni las políticas. H2 y P-03 siguen apagados.
