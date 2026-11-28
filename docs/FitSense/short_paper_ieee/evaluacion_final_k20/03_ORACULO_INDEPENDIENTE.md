# 03 · Oráculo independiente

**Código:** `herramientas/oraculo_k20.py`, en Python 3 con la biblioteca estándar.

**Uso:**
```
python3 -I herramientas/oraculo_k20.py <corrida> entrada/03_PERFILES_U01_U20.csv entrada/04_ORACULO_TEORICO_HISTORICO.csv
```

**Pruebas:**
```
python3 -I herramientas/test_oraculo_k20.py -v
```

## Independencia

- Lee solo las tablas exportadas (`tablas/*.csv`), el perfil congelado y los ficheros de la corrida. No importa ni llama a código del backend.
- Las reglas se reimplementan a partir de su **definición documentada** (`experimento_final_20_usuarios/01`, migraciones y configuración `MVP-1.8`). Los umbrales se leen de la configuración activa exportada, no del código.
- Si el backend y el oráculo discrepan, se investiga; **nunca** se cambia uno para que coincida con el otro.

## Qué calcula

| Concepto | Regla del oráculo |
|---|---|
| Adherencia por ejercicio | min(100, 100 × ejecutada / prescrita), con 2 decimales HALF_UP. Prescrita = series × repeticiones o series × segundos. Sin fila = 0 |
| Por sesión | Media de sus ejercicios prescritos, tope 100 |
| Aw / Aobs / C | Σ de todas / n · Σ de las registradas / n registradas · 100 × registradas / n. Registrada = sesión que cuenta u omisión con motivo |
| Decisión | 1. Dolor → localizada.<br>2. C < 50 o Aobs nulo → INSUFFICIENT_DATA.<br>3. Aobs ≥ 80 → NO_CHANGE.<br>4. Sin sesión registrada < 80 → INSUFFICIENT_DATA.<br>5. Aobs < 50 → EXPANDED (todas las registradas < 80).<br>6. Si no, LOCALIZED (la peor).<br>Intervención = motivo de omisión; parcial sin motivo = CAUSA_NO_REGISTRADA |
| Duración estimada | 300 s de calentamiento + Σ[trabajo + (series − 1) × descanso] + (n − 1) × 60 s, con 3 s por repetición |
| V20 | ⌈minutos × 60 × 70 / 100⌉ s; exentas las sesiones con contexto de falta de tiempo |
| V4 (PLAN-HIST-1) | Minutos declarados ≤ round(minutos × 1,15), con el redondeo de Java |
| V17 | \|declarados − estimados\| / estimados ≤ 20 % |
| Equipamiento | Grupos distintos = AND; mismo grupo = OR; sin grupos = peso corporal |
| Integridad | Sesiones no señaladas idénticas: ejercicios, orden, dosis, carga, descanso, fecha + 7 y minutos |
| Seguridad | • No sube ninguna dosis (series, repeticiones, segundos, carga), no hay menos descanso, no se añade nada y el trabajo de la sesión no aumenta.<br>• Ningún sustituto es más difícil (dificultad del catálogo) ni con más dosis.<br>• Los excluidos ACTIVE no reaparecen |
| Origen | Según V32 (`status`, `generation_source`, `applied_sessions`, nota `RETIRADA_SEGURA`, `REGENERACION_SEGURA`) **y** el cambio observado. Una incoherencia (aplicada sin cambio, cambio sin origen) es un FAIL |
| Pertinencia | Tabla del protocolo: ACC / ALT / CONS-J / CONS-POL / INC por causa. CONS-J exige la justificación del `02` §4 |
| Texto (T1) | El texto anuncia un cambio ⇔ alguna sesión señalada cambió; dice «no aplicamos cambios» ⇔ alguna quedó igual; no atribuye a la IA lo que no hizo |
| Continuidad | API 200; S2 registrado al 100 %; métricas y decisión de S2; S3 válido; copia exacta con NO_CHANGE; sin reincorporación indebida |
| Huella normalizada | SHA-256 de una representación canónica sin ids sustitutos ni marcas de tiempo. Los ids se reemplazan por (usuario, semana, posición). Incluye planes, ejecución, métricas, V32 y textos |

## Qué NO re-verifica (confía en la validación estricta del backend)

- V16: rango de repeticiones por objetivo.
- V24/4D: la dosis del objetivo.
- V9: las series mínimas.
- La cobertura de slots 4C/V15.
- La lógica interna de V8C, salvo sus efectos observables: integridad, dosis y dificultad.

Ninguna de estas reglas puede **aumentar** el riesgo sin que lo detecte alguna comprobación de seguridad del oráculo.

## Pruebas propias (22, todas superadas)

- **9 de funciones puras:**
  - redondeos;
  - piso V20 (1.890 s con 45 min; 3.150 s con 75);
  - duración;
  - tope del 100 %;
  - grupos de equipamiento;
  - decisión E1/E3/E4/E5;
  - U07 con sus valores reales;
  - prioridad del dolor;
  - comparación y pertinencia.
- **13 de mutación sobre una corrida real:** sin alterar, todo debe pasar. Cada una de estas alteraciones debe dar FAIL en su fase:
  - una métrica;
  - un registro;
  - una decisión;
  - una sesión no señalada;
  - un sustituto más difícil;
  - una dosis aumentada;
  - un plan duplicado;
  - un texto incoherente;
  - V20 en la continuidad;
  - un excluido que reaparece;
  - el equipamiento;
  - la idempotencia.

## Teórico frente a política

El oráculo **teórico histórico** (`entrada/04_ORACULO_TEORICO_HISTORICO.csv`) suponía que P40 da el 40 % exacto. Se conserva y se compara aparte (`decision.coincide_teorico`).

**El juicio PASS/FAIL usa la política sobre los valores reales.** U07 (79,75) y U12 (79,44) quedan por debajo de 80, así que lo correcto es ADAPT_LOCALIZED sobre S3, con causa no registrada y conservación sin IA. La diferencia frente al teórico se documenta en `09` y `13`.
