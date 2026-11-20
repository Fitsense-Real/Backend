# Discrepancias con el short paper

**Advertencia:** `Kora_Short_Paper_IEEE.docx` no estaba disponible en el entorno de trabajo. Las definiciones del artículo se toman de la orden (§1.2). Antes de redactar, hay que cotejar estas filas con las secciones II.C–II.F reales. **No se ha modificado el artículo.**

| # | Lo que dice el artículo (según la orden) | Lo que hace el backend (`0974fb3`) | Evidencia | Sugerencia para el texto |
|---|---|---|---|---|
| D1 | Las franjas de decisión (≥ 80, 50–80, < 50) se aplican a **Aw** | Se aplican a **Aobs**. Aw solo se guarda y se informa | `AdaptationOrderDecider`; 01 §2.7 | Decir que la decisión usa la adherencia **observada**, condicionada a una cobertura ≥ 50 %, y que Aw es la métrica oficial que se informa. En los 20 casos no cambia ningún resultado |
| D2 | Adaptación localizada (50–80) | Señala **solo la peor** sesión con registro (o todas las de dolor) | U03, U08, U13, U18 → S3; U07, U12 → S3 | Precisar «sobre la sesión de menor cumplimiento» |
| D3 | Adaptación ampliada (< 50) | Señala **todas** las sesiones con registro por debajo del 80 %. **No** reestructura la semana entera | U04, U09, U14, U19 → S2 y S3 | Evitar «replanificación completa»; decir «sobre todas las sesiones con bajo cumplimiento» |
| D4 | La adaptación modifica el plan | **Solo con IA.** Sin IA, la decisión termina en `RESPALDO` (se republica la misma semana) y no se aplica ningún cambio. Excepción: las sustituciones de seguridad ante dolor | 8 respaldos con 0 llamadas | Separar en el texto «decisión de adaptar» y «adaptación aplicada»; no contar los respaldos como adaptaciones |
| D5 | P40 = el 40 % de la dosis | Se registran dosis enteras por ejercicio. El porcentaje real depende de la prescripción: 40,00 en U02/U17; 39,26 y 38,33 en U07/U12 con la regla uniforme | H1 | Informar el porcentaje realmente registrado y el efecto del umbral estricto (79,75 no llega a 80) |
| D6 | (No lo menciona) | Reglas adicionales:<br>• el dolor tiene prioridad y fuerza una adaptación de seguridad aunque Aobs ≥ 80 o falten datos;<br>• una causa no registrada no se adapta (`SIN_EFECTO`);<br>• una sesión sin registro nunca se señala | 01 §2.8 y §3 | Mencionarlas si el artículo describe la política completa |
| D7 | (Según cómo se describa) Los planes ocupan el tiempo disponible | Las sesiones duran el 71–76 % de los minutos declarados (V20 ≥ 70 %, V4 ≤ 115 %) | H4 | No afirmar que el plan ocupa todo el tiempo disponible |
| D8 | Adherencia por ejercicio = ejecutada / prescrita | Coincide, con un **tope del 100 %** y la media de la sesión sobre los ejercicios **prescritos** (sin registro = 0) | 01 §2.1–2.2 | Mencionar el tope del 100 % |
| D9 | «Adherencia ponderada» | Cada sesión pesa 1 (el nombre `weighted_adherence_pct` es histórico) | H6 | Usar «promedio por sesión» |
| D10 | Datos insuficientes por debajo del 50 % | Coincide (C ≥ 50 % es suficiente) | U05, U10, U15, U20 | — |
| D11 | (Según cómo se describa) La IA adapta el plan | Con Replicate real, **4 de 8** adaptaciones elegibles se aplicaron. Las otras 4 propuestas superaron la validación pero no cambiaron nada efectivo. Hubo 5 propuestas rechazadas, todas por V20, con hasta 3 intentos | `16`, `18` | Informar la tasa real (4/8) y que los validadores rechazaron 5 de 13 propuestas |
| D12 | (Según cómo se describa) La IA genera el plan inicial | Con IA, 20/20 planes válidos; 2 necesitaron un segundo intento (rechazos de V24 y V15). Tardan unos 98 s por plan (mediana) | `15`, `19` | Dar el tiempo y el número de intentos; sin la IA, el motor de reglas genera en menos de 1 s |
| D13 | — | Solo **8 de 20** casos requieren IA en la adaptación. Las métricas y las decisiones no dependen de ella (idénticas con y sin IA) | `20` | Separar claramente el núcleo determinista (medir y decidir) del componente generativo (proponer) |

**Coinciden sin matices:**
- Aobs y la cobertura;
- el umbral del 80 %;
- la diferencia entre omisión declarada (cuenta como registro al 0 %) y ausencia de registro (no cuenta);
- el uso de validadores antes de publicar.
