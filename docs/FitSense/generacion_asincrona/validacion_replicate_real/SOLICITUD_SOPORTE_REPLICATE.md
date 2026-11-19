# Solicitud de soporte a Replicate (borrador)

Este borrador lo envía el titular de la cuenta desde el panel de Replicate (Help / Support) o a support@replicate.com.
Antes de enviarlo, completa `<usuario de la cuenta>` y no añadas tokens ni claves.

---

**Subject:** All predictions on `openai/gpt-5-structured` fail instantly with E002 (1cah9wlWR9), "No sid"

Hello,

Since 2026-11-17 16:47 UTC (and still on 2026-11-19 09:33 UTC, after the 2026-11-17 incident was reported as resolved), every prediction I create on the official model `openai/gpt-5-structured` fails within
about 0.3 s. The error is always the same, the logs read `No sid`, and no tokens are counted. The same account and
model worked normally on 2026-11-12. The last successful prediction was `jhxmsrkj61rmr0d1t5trg6znj4`
(gpt-5-mini, 33,985 input tokens and 11,421 output tokens).

Account: `<usuario de la cuenta>` (personal account, API access with a valid token; `GET /v1/account` works).

Error returned in all cases:

```
status: failed
error:  An error occurred while processing your request (E002) (1cah9wlWR9)
logs:   No sid
metrics: predict_time ≈ 0.22–0.28 s, no token counts
```

Affected predictions (all created via the API, `POST /v1/models/openai/gpt-5-structured/predictions`, HTTP 201):

| Prediction ID | Created (UTC) | Input |
|---|---|---|
| `7dxtdb57znrmy0d1x9kvp0dppc` | 2026-11-17 16:47:04 | model `gpt-5-mini`, reasoning_effort `medium`, prompt ≈ 177k characters, `json_schema` in the `{"format": {"type": "json_schema", ...}}` form |
| `1chmxnn085rmy0d1x9mbgxdhrg` | 2026-11-17 16:48:07 | same as above |
| `cc8tx5wdsxrmr0d1x9mrkr9kqg` | 2026-11-17 16:49:08 | minimal test: model `gpt-5-mini`, reasoning_effort `minimal`, one-line prompt, one-property `json_schema` |
| `ydpzjayh7xrmr0d1xa2rtkz8gr` | 2026-11-17 17:20:00 | same minimal test, 31 minutes later |
| `e23edtt1h9rmr0d1xh09hsd94c` | 2026-11-18 01:23:16 | same minimal test, about 8.5 hours later: still failing |
| `6cb86gw095rmy0d1xk69hkra7g` | 2026-11-18 03:56:27 | same minimal test after the status page reported the 2026-11-17 incident as resolved: queued ~114 s (starting/processing), then failed with the same error |
| `y27xr1snzdrmt0d1xvtsmsqepw` | 2026-11-18 14:00:09 | same minimal test, about 10 hours later: same error |
| `eta2a81ek5rmr0d1xwd98ngd48` | 2026-11-18 14:40:32 | same minimal test: same error |
| `11y682362nrmw0d1y7csf1q86w` | 2026-11-19 03:28:38 | same minimal test: same error |
| `afrn5j7ab9rmy0d1ya0rperk4g` | 2026-11-19 06:32:42 | same minimal test: same error |
| `k2hevnyq3nrmw0d1yck8sd8gj4` | 2026-11-19 09:32:51 | same minimal test: queued ~47 s, then the same error |

Because a trivial one-line request fails with the same reference code, I don't believe the input is the cause.

**The same error also happens in your web Playground** with my logged-in account (model `gpt-5-mini`, the default example prompt): "Prediction failed. An error occurred while processing your request (E002) (1cah9wlWR9)", generated in 0.1 s, logs `No sid`. So it is not caused by our client, token or input.

Could you please tell us:

1. What does E002 / reference `1cah9wlWR9` / "No sid" mean for this model?
2. Is there an issue on our account (billing, credit, spend limit, payment method, or access to OpenAI-backed official
   models) that blocks these predictions?
3. Is the incident posted on the status page on 2026-11-17 ("Errors and stalled predictions on some models") related, and is there an ongoing incident with the OpenAI integration for `openai/gpt-5-structured` (or the `gpt-5-mini`
   variant)?
4. Were these eleven failed predictions billed? We expect no charge, since no tokens were processed.

Thank you.

---

## Lista de comprobación antes de enviarlo

- [ ] En el panel: Account → Billing. Revisar el saldo o crédito, si se alcanzó el límite de gasto, el estado del
      método de pago y los avisos de la cuenta.
- [ ] Abrir la página web de una de las predicciones (`https://replicate.com/p/<id>`) para ver el detalle que muestra
      el panel.
- [ ] Revisar `status.replicate.com` por si hay incidentes del 17/11 en adelante.
- [ ] Si el panel muestra un problema de facturación, resolverlo allí. En ese caso puede que no haga falta escribir a
      soporte.
- [ ] Confirmar que el mensaje no contiene ningún token, clave ni dato personal más allá del usuario de la cuenta.
