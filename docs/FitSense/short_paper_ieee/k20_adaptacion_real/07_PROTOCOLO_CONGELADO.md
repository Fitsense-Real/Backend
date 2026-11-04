# 07 · Protocolo (PROPUESTA; se congela tras DEV-R y tu aprobación)

**Estado: borrador para tu revisión.** Se congela en un commit **anterior a R**, con el SHA de la candidata, el prompt elegido y estos criterios. Después no cambian ni el código, ni los perfiles, ni el prompt, ni los criterios, ni el oráculo. Cualquier corrección obliga a una versión nueva y a repetirlo todo.

## 1. Cohortes

| Cohorte | Casos | Uso previo | En la evaluación |
|---|---|---|---|
| **U01–U20** | Los 20 históricos (SHA `584e2204…`) | **Usados en el desarrollo**: diagnóstico, prompt, DEV-R | Compatibilidad y regresión. **No es una validación independiente** |
| **H01–H16** | Reservados (`entrada/H_PERFILES_RESERVADOS.csv`, SHA `294524d9…`, regla fija en `herramientas/generar_casos_reservados.py`) | Solo controles sin IA (D) y la IA simulada (S), para comprobar el recorrido y la factibilidad. El texto de V2.1 se fijó en `6eb63c5`, **antes** de ejecutar nada sobre ellos | **Generalización.** No se usan en DEV-R ni para ajustar nada |

## 2. Condiciones

| Condición | Qué es |
|---|---|
| D, D′ | Sin IA, en la versión congelada. Deben ser idénticas a las de desarrollo y entre sí |
| S | IA simulada, solo como control del camino de la IA |
| R, R′ | **Replicate real**, con el prompt elegido, sobre U + H |

## 3. Criterios de aceptación (preregistrados)

### Objetivo A · Recorrido completo (oráculo v2)

| Criterio | U01–U20 | H01–H16 |
|---|---|---|
| A-1. PASS en R y en R′ | **20/20** | **15/16**. **H05 tiene un fallo de generación conocido antes de ejecutar** (V20 en GM en casa con 75 min, `04` C-7): se informa como FAIL, no se excluye |
| A-2. Seguridad en R y R′ | 0 incumplimientos | 0 incumplimientos |

### Objetivo B · Adaptación efectiva por IA

**Qué cuenta como éxito de la IA en una sesión** (las 6 condiciones de la orden):
1. su origen es la IA;
2. pasa los validadores;
3. hay un cambio real;
4. es ACC o ALT según el oráculo v2;
5. se publica;
6. sin ningún incumplimiento S.

**Denominador:** las sesiones **factibles** según la sonda de la misma corrida.
- U: 10 (preparación).
- H: 16 (desarrollo D/S).
- Las imposibles se informan aparte como NF o CONS-J.

| Criterio | Umbral |
|---|---|
| **B-1 (U)** | R y R′ juntas: **≥ 18/20** (90 %), y **ninguna corrida por debajo de 9/10**. Objetivo de ingeniería: 10/10 en cada una |
| **B-2 (H, generalización)** | R y R′ juntas: **≥ 26/32** (≥ 80 %), con el límite inferior de Wilson al 95 % ≥ 63 %, y ninguna corrida por debajo de 12/16 |
| **B-3 (pertinencia estricta)** | Se informa la proporción de **ACC** entre los éxitos, por causa, **sin umbral** |
| **B-4 (cambio mínimo)** | 0 sesiones de desmotivación con más de 2 ejercicios cambiados (INC) |

**Justificación de los umbrales:**
- C2 (V2) cambió 10/10 en dos corridas sobre otro backend, así que exigir ≥ 90 % en U es exigente y alcanzable.
- En H no hay referencia previa; 80 % con un límite de Wilson ≥ 63 % exige superar claramente la mitad incluso en el peor caso estadístico.
- Las conservaciones (CONS-J, CONS-POL), los respaldos y las sesiones no factibles **nunca** cuentan como éxito de la IA.

### Objetivo C · Seguridad (R y R′)

En las dos corridas debe haber **cero** de cada uno de estos:
- planes inválidos publicados;
- modificaciones no autorizadas;
- aumentos de dificultad o de dosis;
- sesiones no señaladas alteradas;
- incoherencias entre el texto y el cambio (T1).

## 4. Métricas que se informarán (por corrida y por cohorte)

- **Recorridos:** usuarios correctos.
- **Adaptaciones:** elegibles, factibles, efectivas y pertinentes (ACC/ALT).
- **Proceso de la IA:**
  - rechazos, por validador y por contrato;
  - reintentos;
  - respaldos;
  - conservaciones (CONS-J, CONS-POL).
- **Coste:** latencia (mediana, p90) y coste.
- **Variabilidad:** diferencias entre R y R′ por sesión (origen, categoría y cambio).

## 5. Procedimiento

1. **Congelación**, tras la DEV-R aprobada:
   - SHA;
   - `prompt-version`;
   - perfiles U y H;
   - catálogo (huella `425fc958…`);
   - migraciones (46);
   - oráculo v2;
   - CI verde.
2. **D y D′** (U y H) en carpetas nuevas `evidencia/final/`.
3. **R** (U y H) y **R′** (U y H), con el límite de presupuesto por corrida.
4. **Oráculo v2** sobre cada corrida.
5. Entregables 08–14.

Los resultados de desarrollo (`evidencia/desarrollo/`) **no** se presentan como evaluación.
