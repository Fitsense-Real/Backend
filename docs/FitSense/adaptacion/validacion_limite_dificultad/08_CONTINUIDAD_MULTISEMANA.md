# 08 · Continuidad multisemana

Condiciones de ejecución:
- PostgreSQL efímero y servicios reales (ciclo semanal, P-02, V8C, validación estricta, P-03);
- IA apagada o simulada; **sin Replicate**.

## 1. Los 22 perfiles de los 24 casos del 135, de extremo a extremo

`SinSustitutoDolor24CasosIT` es un laboratorio opt-in que recibe la estrategia por `-Dfitsense.adaptation.difficulty-guard`. Se ejecutó tres veces sobre el mismo código, una por estrategia.

Para reproducir **los mismos planes** del barrido se usaron:
- la misma semana (2026-11-02);
- el mismo `user_id` que cada perfil tuvo en el barrido. La semilla del motor de reglas es `userId × 1.000.003 + semana`, así que hay que rellenar ids hasta llegar a ese valor.

En cada perfil, el flujo de la prueba es:
1. Plan real.
2. Todas las sesiones hechas, y en la del 135 el 135 con `PAIN_OR_DISCOMFORT` y 0 hecho.
3. Ciclo semanal.
4. Semana siguiente ejecutada entera, y otro ciclo.
5. Si no hubo plan en la semana nueva, otro ciclo igualmente.

Las invariantes se comprueban en cada perfil; si alguna falla, la prueba falla. Las tres corridas pasaron: **1/1** cada una.

Evidencia: `evidencia/flujo_135_22_perfiles_{off,on,preferencia}.csv`.

| Desenlace | A (off) | B (on) | C (preferencia) |
|---|---:|---:|---:|
| Perfiles reproducidos (llevan el 135 ese día) | 21 (23 colocaciones) | 21 | 21 |
| Plan inicial no válido en el ciclo real (R0548, V20: 2.550 s de 3.150 s), **independiente del límite** | 1 | 1 | 1 |
| RESPALDO: 135 → **780 (dif 2)**, intermedios | 10 | 0 | 0 |
| RESPALDO: 135 → **663 (dif 3)**, avanzados | 11 | 0 | 0 |
| NO_FACTIBLE `SIN_SUSTITUTO_SEGURO` → **P-03 nivel 2 publica** la semana regenerada sin el 135 | 0 | **14** | **14** |
| NO_FACTIBLE → P-03 nivel 2 **no puede publicar** (`TRABAJO_SUPERIOR_AL_PERMITIDO`) → **semana sin plan** | 0 | **7** | **7** |
| El 135 reaparece en alguna semana publicada | 0 | 0 | 0 |
| Plan a la semana siguiente | 21 | 21 | 21 |

La semana siguiente:
- los 14 perfiles con plan regenerado siguen con `NO_CHANGE/SIN_ORDEN` y el plan sin el 135;
- los 7 que se quedaron sin plan reciben uno nuevo generado por el ciclo, sin intento V32 porque no había plan que medir, y también sin el 135.

Los 7 sin plan en la semana del dolor son:

| Perfil | Objetivo | Nivel | Días | Minutos |
|---|---|---|---:|---:|
| R0521 | GF | intermedio | 6 | 30 |
| R0524 | GF | intermedio | 6 | 75 |
| R0527 | GF | avanzado | 6 | 60 |
| R0545 | GM | intermedio | 7 | 30 |
| R0570 | GF | intermedio | 7 | 45 |
| R0573 | GF | avanzado | 7 | 30 |
| R0574 | GF | avanzado | 7 | 45 |

Seis de los siete son GENERAL_FITNESS.

En los siete, la semana regenerada por reglas, que parte de las dosis base, **supera el trabajo de la semana medida**. P-03 la rechaza para no añadir trabajo en plena semana de dolor (auditoría del PR #16).

**B y C dan exactamente el mismo resultado** en los 22 perfiles: la preferencia de C no crea candidatos.

> Este es el **coste restrictivo medido** del límite. Una exposición simulada (dolor en el 135 de una sesión PUSH, en casa con mancuernas y bandas) termina, en 7 de 21 perfiles, en **una semana sin plan** en lugar de en un sustituto de dificultad 2 o 3. No se publica nada inseguro y la semana siguiente se recupera, pero es una pérdida funcional que el responsable debe aceptar o descartar (14, R-3).

## 2. Pruebas de continuidad que corren en CI (no opt-in)

| Prueba | Semanas | Invariantes |
|---|---:|---|
| `SinSustitutoDolorLimiteApagadoIT`, `…EncendidoIT`, `…PreferenciaIT` | 2 | Con el 135 excluido no reaparece; la exclusión sigue ACTIVA; ≤ 1 plan activo; el ciclo termina. En la configuración común (R0487) el resultado es: A: 780; B y C: regeneración P-03. |
| `RespaldoConDolorIT` (respaldo forzado: la IA falla siempre) | 2 | **B:** solo cambia la posición que dolió; las demás sesiones y posiciones son idénticas; la dosis no sube; V32 recoge la sustitución; la semana siguiente conserva el sustituto y no reincorpora.<br>**C (1373 sin sustituto):** no se publica nada inválido; la exclusión sigue intacta. |
| `ValidacionMultisemanaIT`, `CicloConcurrenteIT`, `LinajeIntegridadIT`, `ProgresionIT`, `HerenciaD8IT`, `AdaptacionCompletaIT` y el resto | varias | En la regresión completa con off, on y preferencia (12): **0 fallos** de comportamiento. |
| REPLAY P123, 100 semanas | 10 por usuario | 09: solo cambia N08 y sus semanas posteriores; el resto es byte a byte igual. |

## 3. Comprobaciones pedidas (fase 5.2)

| Comprobación | Resultado |
|---|---|
| Ninguna reintroducción accidental | 0 en todas las corridas (aserciones en cada semana) |
| Ningún incremento de dificultad no autorizado | con B y C: 0. Con A: 21/21 suben (es lo que corrige el límite) |
| Ninguna subida de dosis no autorizada | `dosisNoSube`; RESPALDO con la dosis de la posición (N08: 3x6, 150 s → 3x6, 150 s); P-03 rechaza si sube el trabajo |
| Conservación del linaje | RESPALDO: linaje V31 completo (N08: 100←62, 138←100…). P-03 nivel 2: sin linaje V31, traza en `safe_regeneration_attempts` (03, pregunta 12) |
| Sesiones no señaladas sin modificar | `assertNoSenaladasIntactas`; `RespaldoConDolorIT.B` |
| Resultados reproducibles | Laboratorio de 22 perfiles: B y C idénticos. REPLAY con on y con preferencia: idénticos entre sí y con la corrida del 03/11 |
| Sin ciclos de reintento infinitos | 1 intento V32 por semana; como mucho 3 llamadas a la IA; 1 intento P-03 |
| Manejo sin sustituto | NO_FACTIBLE con motivo, luego P-03 publica o deja NO_FACTIBLE con motivo; la API informa `REGENERACION_SEGURA_REGLAS` o `NO_FACTIBLE_POR_SEGURIDAD` |
