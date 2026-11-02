# Prueba explícita del respaldo con dolor (D1)

**Prueba nueva:** `RespaldoConDolorIT` (en el PR de D1). Corre en CI, no es *opt-in*.

## 1. Cómo se fuerza el respaldo sin depender de la semilla

- **La IA simulada contesta, pero su propuesta se rechaza siempre:** `interpretar` lanza `InvalidPlanDraftException("PRUEBA: propuesta rechazada de forma controlada…")`. Se agotan los **3 intentos** (`ai_calls = 3`) y el ciclo entra en `AdaptedWeekProducer.respaldo`, que es el camino real de producción.
- **Es determinista:**
  - una base PostgreSQL efímera propia;
  - un orden de pruebas fijo (`@Order`);
  - un perfil fijo (GENERAL_FITNESS, intermedio, gimnasio, 3 días, 45 min);
  - la semana `RelojPruebas.SEMANA_REFERENCIA`.

  Mismos `user_id`, mismas semillas y mismos planes en cada ejecución.
- **Reloj controlado:** se abre la ventana de ejecución y el barrido horario de vencidos se sustituye por un doble.

## 2. Los tres casos

| # | Escenario | Lo que se comprueba |
|---|---|---|
| **B** | Plan original conocido. El miércoles se hace todo salvo el **último ejercicio**, dejado por dolor. La IA falla de forma controlada | 1) plan original conocido; 2) dolor registrado; 3) exclusión P-02 **activa**; 4) 3 intentos fallidos; 5) estado `RESPALDO`, origen `CONSERVED`; 6) **una sola** posición cambia (la del ejercicio que dolió, en el miércoles), por **SUST-SEG-1**; 7) lunes y viernes **idénticos**; 8) mismo tipo y clase de slot, sin subir series, repeticiones, segundos ni trabajo de la sesión; 9) V32 con la sustitución (`regla = SUST-SEG-1`) y la nota `REGENERACION_SEGURA`; la API devuelve `REGENERACION_SEGURA_SUSTITUCION`; 10) el plan se publica solo porque pasa V8C y V1–V25. **La semana siguiente** (todo hecho, sin motivo): idéntica, conserva el sustituto y **no reincorpora** lo que dolió; la exclusión sigue activa |
| **A** | Lunes y miércoles hechos; viernes omitido por **fatiga** (`REVISAR_CARGA_Y_RECUPERACION`). La IA falla | `RESPALDO`, origen `CONSERVED`, **copia exacta** de la semana medida, sin sustituciones ni nota P-03 |
| **C** | El primer participante determinista cuyo plan lleva el **1373** (sin sustituto SUST-SEG-1). Dolor en el 1373 y la IA falla | **No se publica nada:** NO_FACTIBLE con motivo («no hay sustituto elegible con la regla SUST-SEG-1»), ningún plan activo para la semana y la exclusión **sigue activa**. No se levanta ninguna restricción para publicar |

**Con otra semana** (`-Dfitsense.pruebas.semana-referencia`), las invariantes se comprueban igual sobre el camino que tome cada caso. El camino esperado solo se fija para la semana de referencia.

## 3. Resultados (PostgreSQL efímero, 0 llamadas a Replicate)

| Semana | Resultado | B: ejercicio que dolió → sustituto | C |
|---|---|---|---|
| **2026-11-02** (referencia) | **3/3** | 456 → **773** (estiramientos de pecho, DURATION 2×30 s/15) | Usuario 32: NO_FACTIBLE, nada publicado |
| 2026-10-26 | **3/3** | 1102 → 57 | Usuario 18: NO_FACTIBLE, nada publicado |
| 2026-11-09 | **3/3** | 453 → 454 | Usuario 26: NO_FACTIBLE, nada publicado |

**Determinismo:** la semana de referencia se ejecutó dos veces (la prueba aislada y la cadena de tres semanas) con el mismo resultado: 456 → 773 y el usuario 32.

**Con otras semillas** cambian los ejercicios (como se espera) y se mantienen las invariantes:
- B se resuelve siempre con **una** sustitución SUST-SEG-1 y las otras sesiones intactas;
- C nunca publica.

## 4. Laboratorio relacionado

`AnalisisSustitucion1373IT` (*opt-in*) usa el mismo arreglo para explicar el caso C y medir la alternativa localizada de P-03. Ver `ANALISIS_NO_FACTIBLE_1373.md` y `COMPARACION_P03_ACTUAL_VS_LOCALIZADA.md`.
