# Protocolo de confirmación de las mejoras de IA (CONF-IA)

**Orden:** «ORDEN DEFINITIVA — FITSENSE: CORRECCIÓN DE V8C Y CONFIRMACIÓN DE MEJORAS DE IA» (10/11/2026).
**Estado:** se congela **antes de la primera llamada pagada**. Nada de lo que sigue cambia después de ver resultados. Si algo tuviera que cambiar, sería una enmienda fechada y razonada, aplicada antes del bloque al que afecta.
**Experimentos anteriores** (MEJ, MEJ ronda 2): no se modifican ni se reutilizan como corridas nuevas.

## 1. Preguntas

1. **Generación.**
   - ¿pr-1.8c mejora la generación inicial frente a PR-1.7 sin empeorar dosis, estructura ni seguridad?
   - Si no, ¿lo consigue pr-1.8a?
2. **Adaptación.** Con V8C corregido en ambas condiciones, ¿v2.2a mantiene o mejora el comportamiento de V2.1?
3. **Flujo conjunto.** ¿La combinación candidata funciona cuando el plan inicial lo genera Replicate y después se adapta con v2.2a bajo V8C corregido?

## 2. Elementos congelados

| Elemento | Valor |
|---|---|
| Código ejecutado | `ab04e67`: `1aac853` + corrección de V8C (`5bea428`) + pruebas de fecha (`ab04e67`, solo código de prueba). Variantes apagadas por defecto |
| Prompts de generación | `PlanPromptBuilder.java` SHA-256 `33a648cd…dbca6`, idéntico al de `1aac853`. Condiciones: `pr-1.7` (control), `pr-1.8c`, `pr-1.8a` |
| Prompts de adaptación | `SessionFixPromptBuilder.java` SHA-256 `1f8256e9…d3ea`, idéntico al de `1aac853`. Condiciones: `v2.1` (control), `v2.2a` |
| Perfiles | `O_PERFILES.csv` SHA-256 `9b996825a6c146f156b7bd8d585a024cb1ccc44adf2054b11f1badb5de2b2106` (O01–O20) |
| Catálogo y filtros | Migraciones `src/main/resources` sin cambios desde `1aac853` (catálogo filtrado de la ronda 2) |
| Política | PLAN-REV-2.0 (`allow-candidates=true`) |
| Validadores | Los reales del backend: V1–V27 en generación, V8C corregido + V1–V27 en adaptación. Timeout del proveedor de 150 s, sin cambios |
| Versión del modelo | `045f81020dac69eadec8cb98d0d4205e198774becd0c6a29445771264297836e` (10/11/2026). Se registra antes y después de cada bloque |
| Ejecutor | `correr_CONF_IA.sh` (huella en `SHA256SUMS_CONGELACION.txt`) |
| Instrumentos | `analisis_mej_r2.py` (SHA-256 `c328532b…f09c`) y oráculo v1.4, sin cambios. El informe se genera con `entregables_conf_ia.py`, que solo lee y aplica el §6 |

## 3. Diseño

### 3.1 Generación (fase 2)
- **Perfiles:** los 19 perfiles factibles (O01–O20 sin O11). O11 es la inviabilidad esperada del flujo controlado: se ejecuta sin llamada y nunca se cuenta como fallo de Replicate.
- **Rondas:** cada ronda ejecuta los 19 perfiles con PR-1.7 y con pr-1.8c, con el mismo catálogo, contexto y versión del modelo.
  - **C1** en orden PR-1.7 → pr-1.8c.
  - **C2** en orden inverso.
  - **C3** en orden directo.
  - Así se reparte entre las dos condiciones cualquier deriva del proveedor a lo largo del día.
- **pr-1.8a (alternativa):** se ejecuta solo si, tras C1–C3, pr-1.8c no cumple los criterios del §6.1 y queda presupuesto de generación.
  - Hasta 3 rondas A1–A3, cada una pareada por perfil con la ronda de control del mismo número (C1–C3).
  - Misma versión del modelo, y el modelo no guarda estado entre llamadas.
  - La limitación (control y variante en momentos distintos) se informa.
- Se registra, sin repetir nada selectivamente:
  - las propuestas originales;
  - la validación de cada intento, con rechazos y códigos;
  - los reintentos;
  - el plan publicado (IA o respaldo);
  - la traza de dosis y tiempo.

### 3.2 Adaptación (fase 3)
- **Plan previo:** determinista. Mismos planes, mismas situaciones y misma causa en las dos condiciones.
- **Perfiles con llamada a la IA según el guion congelado:**
  - **fase G:** O03, O04, O05, O06 y O12 en el ciclo 1, y O14 en el ciclo 2 (la sesión adicional del guion);
  - **fase P** (con progresión): O17, O18, O19 y O20.
  - Son 10 predicciones y 12 sesiones con IA (O05 adapta tres sesiones en una predicción), las mismas de la ronda 2.
- **Rondas D1–D4**, alternando el orden de V2.1 y v2.2a. Si queda presupuesto de adaptación, una D5.
- **V8C corregido activo en las dos condiciones:** las diferencias no pueden atribuirse al validador.

### 3.3 Flujo conjunto (fase 4)
- **Una ronda (I1)** sobre los mismos perfiles y guiones:
  1. generación con la candidata;
  2. validación;
  3. publicación experimental;
  4. adherencia simulada del guion;
  5. adaptación con v2.2a;
  6. validación con V8C corregido.
- **Candidata:** pr-1.8c. Pasa a ser pr-1.8a si pr-1.8c no cumple el §6.1 y pr-1.8a sí; PR-1.7 si ninguna lo cumple.
- **Alcance:** las cuatro semanas medidas y los ciclos del guion (G y P). No es el horizonte completo de 26 semanas, y se informa así.

### 3.4 Compleción (regla I-MEJ-1, sin cambios)

| Situación | Tratamiento |
|---|---|
| Perfil no ejecutado (bloque detenido) | Se completa en otra carpeta |
| Perfil cortado por timeout o cancelado sin salida | Un reintento en otra carpeta |
| Respuesta que el proveedor terminó y el backend no pudo leer (502 o sondeo agotado) | Se descarga y se valida gratis con el validador real, en solo lectura. **No se repite** |
| Salida ya caducada | Se trata como fallo del proveedor |

- Un vigilante (`vigilante_conf.sh`) descarga cada predicción en cuanto aparece en los registros.
- Nunca se repite una respuesta válida.

## 4. Presupuesto (incremental, separado de MEJ ronda 2)

**Autorizado:** USD 10 nuevos.
- El remanente de MEJ ronda 2 (USD 2,0949) **no se usa ni se mezcla**.
- El gasto se suma solo de las carpetas `CONF_*`, en `/home/user/conf_runs`.

| Bloque | Tope acumulado (USD) | Asignado |
|---|---|---|
| Generación (fase `GEN`) | 5,50 | 5,50 |
| Adaptación (fase `ADA`) | 7,50 | 2,00 |
| Flujo conjunto (fase `INT`) | 9,00 | 1,50 |
| Reserva de incidencias (fase `RES`) | 10,00 | 1,00 |

- Antes de cada bloque, el ejecutor pasa el remanente al arnés como tope duro (`max-llamadas`/`max-usd`).
- Cada predicción se reserva a USD 0,05 si no trae métricas, y todo se detiene ante un timeout.
- No es obligatorio gastar los USD 10. La corrección de V8C y las pruebas locales no consumen presupuesto.
- **Tarifa:** 0,25 / 2,00 USD por millón de tokens de entrada / salida.

## 5. Medidas

**Generación (por perfil, ronda y condición):**
- primera propuesta válida;
- propuestas, rechazadas y reintentos;
- respaldo y su causa (proveedor o rechazos agotados);
- hallazgos V15, V26, V24, V23 y otros;
- DOSIS_LIMITADA / DOSIS_OBJETIVO;
- grupos bajo objetivo, mejorables o inevitables frente al plan determinista;
- series por grupo;
- % de M (aprovechamiento del tiempo);
- clase de cada sesión (insuficiente, corta o completa);
- latencia y coste.

**Casos de vigilancia:**
- **O10:** tiempo, sin mínimos nuevos.
- **O12:** rechazos repetidos V23.
- **O18:** que la dosis fija de GAIN_MUSCLE no suba (V24).
- **GENERAL_FITNESS:** V23 y V25.

**Adaptación (por sesión, ronda y condición):**
- estado V32 (aplicada, sin efecto o rechazada/respaldo);
- pertinencia ACC/ALT/CONS-J/INC, con INC corregido por identidad;
- SIN_EFECTO evitables;
- cambios no autorizados por identidad (retiradas y descansos), además del veredicto de V8C;
- trabajo antes y después, dificultad máxima retirada y añadida;
- conservación de las no señaladas.

**Casos prioritarios:**
- **O18:** la retirada de relleno.
- **O12:** el dolor, con las observaciones deportivas aparte.
- **O04:** sustituciones por identidad.

**Flujo conjunto:**
- linaje de los planes;
- V24;
- estructuras por objetivo;
- adaptaciones autorizadas;
- conservación;
- descansos;
- retiradas;
- compatibilidad con progresión y reincorporación dentro del alcance ejecutado.

## 6. Análisis y criterios (no se relajan después)

**Unidad de análisis: el perfil.**
- Los mismos perfiles se repiten en cada ronda, y las repeticiones no se tratan como usuarios independientes.
- **Prueba principal de generación:**
  - para cada perfil se calcula d = (rondas válidas a la primera con la variante) − (rondas válidas con el control), sobre C1–C3;
  - se aplica Wilcoxon de rangos con signo a las 19 diferencias, bilateral, α = 0,05.
- **Se informa además:**
  - McNemar exacta por ronda;
  - recuentos con numeradores y denominadores;
  - IC 95 % de Wilson por condición;
  - variabilidad entre rondas.
- **Análisis secundario:** se junta con la confirmatoria de la ronda 2 (R2 + R3, 5 rondas). Se informa aparte y no decide.

### 6.1 Generación: una variante es recomendable solo si cumple TODO

1. **Prueba principal:** validez a la primera con p < 0,05 a favor de la variante. Si p ≥ 0,05 pero todo lo demás se cumple, el resultado es «favorable no concluyente» y se recomienda mantener PR-1.7 o confirmar más.
2. **V24:** hallazgos de la variante ≤ los del control en C1–C3. Si son más, se informa expresamente y se evalúa pr-1.8a.
3. **Estructura:** hallazgos V2, V13, V18, V23 y V25 de la variante ≤ los del control, y respaldos por rechazos agotados ≤ los del control.
4. **Publicación:** ningún plan publicado con violaciones.
5. **Dosis y tiempo:**
   - el % de M y los grupos bajo objetivo mejorables no empeoran de forma significativa (Wilcoxon por perfil, p < 0,05 en contra);
   - no se fuerzan dosis mayores.
6. **Coste y latencia:** se informan. Un timeout del proveedor es una incidencia, no un fallo del prompt.

Con dos variantes, la segunda (pr-1.8a) solo se evalúa si falla la primera. El orden queda fijado aquí.

### 6.2 Adaptación: v2.2a es recomendable si

1. tiene **0 cambios no autorizados** (por identidad y en V8C);
2. no tiene peores adaptaciones que V2.1: no menos aplicadas, no más INC por identidad y no más SIN_EFECTO evitables, en el total de D1–D4;
3. no comete errores ante dolor o fatiga;
4. se informa su ventaja medida: aplicadas y SIN_EFECTO evitables por sesión, con McNemar sobre los pares sesión × ronda y Wilcoxon por sesión.

Si no hay diferencia significativa, se dice. Se explica aparte si su coherencia (la retirada de relleno en O18) justifica adoptarla sin una mejora estadística.

### 6.3 Flujo conjunto
- Es descriptivo: lo que se ejecutó y si se cumplió cada comprobación del §5.
- No tiene prueba estadística y no se presenta como una prueba integral completa.

## 7. Garantías

- Sin funcionalidades, reglas ni validadores nuevos.
- Sin cambios en V24, V25, V15/V26, estructuras, catálogo, progresión ni reincorporación.
- Variantes apagadas por defecto.
- Sin merge, despliegue ni PR de adopción.
- Sin Neon ni mobileapp.
- La credencial la inyecta el proxy. Antes de archivar, la evidencia se revisa en busca de `r8_` y `Bearer`.
