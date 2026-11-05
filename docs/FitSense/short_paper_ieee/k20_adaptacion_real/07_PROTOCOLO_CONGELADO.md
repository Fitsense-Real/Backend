# 07 · Protocolo (PROPUESTA; se congela tras DEV-R y tu aprobación)

**Estado: borrador para tu revisión.** Se congela en un commit **anterior a R**, con el SHA de la candidata, el prompt elegido y estos criterios. Después no cambian ni el código, ni los perfiles, ni el prompt, ni los criterios, ni el oráculo. Cualquier corrección obliga a una versión nueva y a repetirlo todo.

## 1. Cohortes

| Cohorte | Casos | Uso previo | En la evaluación |
|---|---|---|---|
| **U01–U20** | Los 20 históricos (SHA `584e2204…`) | **Usados en el desarrollo**: diagnóstico, prompt, DEV-R | Compatibilidad y regresión. **No es una validación independiente** |
| **H01–H16** | `entrada/H_PERFILES_RESERVADOS.csv`, SHA `294524d9…` | Ejecutados en D y S. H05 sirvió para encontrar el defecto de 4D v3.1 (`A1` §7) | **Evidencia de desarrollo.** Ya no son una cohorte independiente; se informan como tal |
| **J01–J16** | `entrada/J_PERFILES_INDEPENDIENTES.csv`, SHA `2f793c9f…`, regla fija en `herramientas/generar_casos_independientes_J.py` (`entrada/J_MANIFIESTO.md`) | **Ninguno**: no se ejecuta nada sobre J hasta congelar | **Evaluación independiente.** Sus limitaciones se informan como resultado |

## 2. Condiciones

| Condición | Qué es |
|---|---|
| D, D′ | Sin IA, en la versión congelada. Deben ser idénticas a las de desarrollo y entre sí |
| S | IA simulada, solo como control del camino de la IA |
| R, R′ | **Replicate real**, con el prompt elegido, sobre U + J |

## 3. Criterios de aceptación (preregistrados)

### Objetivo A · Recorrido completo (oráculo v2)

| Criterio | U01–U20 | J01–J16 (**aprobados**) |
|---|---|---|
| A-1. PASS en R y en R′ | **20/20** | **16/16**. Un fallo, de generación o de recorrido, se informa como FAIL y no se excluye. **No se modifican ni J ni el sistema para ocultarlo.** J05 (GM, 59 min) cae en la franja de C-8, que ya está integrada; J no se ha ejecutado |
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
- J: las que dé la sonda en la corrida congelada (J no se ha ejecutado).
- Las imposibles se informan aparte como NF o CONS-J.

| Criterio | Umbral |
|---|---|
| **B-1 (U)** | R y R′ juntas: **≥ 18/20** (90 %), y **ninguna corrida por debajo de 9/10**. Objetivo de ingeniería: 10/10 en cada una |
| **B-2 (J, evaluación independiente; aprobado)** | Adaptaciones **efectivas y pertinentes** (ACC o ALT, que se informan por separado) sobre las sesiones factibles de J en cada corrida. R y R′ juntas **≥ 80 %**, con el límite inferior de Wilson al 95 % ≥ 63 %, y ninguna corrida por debajo del 75 % de sus factibles. Las sesiones no factibles (NF, CONS-J) se registran aparte |
| **B-3 (pertinencia estricta)** | Se informa la proporción de **ACC** entre los éxitos, por causa, **sin umbral** |
| **B-4 (cambio mínimo)** | 0 sesiones de desmotivación con más de 2 ejercicios cambiados (INC) |

**Justificación de los umbrales:**
- C2 (V2) cambió 10/10 en dos corridas sobre otro backend, así que exigir ≥ 90 % en U es exigente y alcanzable.
- En J no hay referencia previa: 80 % con un límite de Wilson ≥ 63 % exige superar claramente la mitad incluso en el peor caso estadístico. El denominador es el de la sonda de la misma corrida, porque J no se ha ejecutado.
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
   - perfiles U y J (SHA de cada CSV);
   - catálogo (huella `425fc958…`);
   - migraciones (46);
   - oráculo v2;
   - CI verde.
2. **D y D′** (U y J) en carpetas nuevas `evidencia/final/`. Es la primera ejecución sobre J.
3. **R** (U y J) y **R′** (U y J), con el límite de presupuesto por corrida.
4. **Oráculo v2** sobre cada corrida.
5. Entregables 08–14.

Los resultados de desarrollo (`evidencia/desarrollo/`) **no** se presentan como evaluación.
