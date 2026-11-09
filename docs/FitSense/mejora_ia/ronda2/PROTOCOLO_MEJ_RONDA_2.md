# MEJ ronda 2 · Protocolo (se congela antes de la primera llamada pagada)

**Objetivo:** mejorar la generación inicial y la adaptación con IA cambiando solo lo que recibe el modelo. No se tocan reglas, validadores, políticas, catálogo ni estructuras.

**Rama:** `experimento/mejora-ia`. Todas las variantes están **apagadas por defecto**: PR-1.7, FIX-PR-1.3, V2.1 y V2.2 siguen idénticos.

**Base:** la auditoría gratuita (`AUDITORIA_REGLAS_FITSENSE.md`) no encontró defectos que invaliden la medición.

## 1. Condiciones

| Bloque | Control | Variante | Qué cambia en el prompt (nada más) |
|---|---|---|---|
| G1 | PR-1.7 | **pr-1.8a** | Las líneas de V15 (la mitad, con el número) y V26 (techos de sesión y semana) de PR-1.8. Sin la dosis orientativa |
| G2 | pr-1.8a | **pr-1.8b** | pr-1.8a + la dosis orientativa de `CalidadDosis` diciendo que la dosis de cada ejercicio no se toca para alcanzarla (GM: `dosis_por_clase`, V24; resto: una dosis de `capacidades.dosis`). Se alcanza con selección y reparto, o se queda por debajo |
| G3 | pr-1.8a | **pr-1.8c** | pr-1.8a + la estructura del objetivo ya presente en `constraints` (principales con su exigencia real, secundarias, slots), el coste en segundos de cada dosis con el estimador de V4/V17 y el tiempo útil de la sesión |
| A1 | V2.1 | **v2.2a** | V2.1 + solo la corrección de ACORTAR: la factibilidad ofrece retirar un relleno, marcado [cabe] solo si respeta el tope M. Sin la línea de dolor de V2.2 |
| A2 | V2.1 / v2.2a | **v2.4** | v2.2a + un «resumen del caso» por sesión: problema, sesión, opciones existentes, lo modificable y el resultado buscado. No amplía ningún permiso |

**O10** entra como caso diagnóstico en G1–G3, junto con los otros 18 perfiles factibles. No se optimiza solo para O10.

**O11** se reporta aparte: el backend lo bloquea antes de llamar al modelo y no consume predicciones.

## 2. Cohorte, configuración y versión

- **Perfiles:** O01–O20 (`O_PERFILES.csv`, SHA-256 `9b996825…`); 19 factibles más O11. Mismo catálogo (base efímera con Flyway V46 + V44_3), misma política `PLAN-REV-2.0`, mismo arnés (`EvaluacionK20IT`, modo integral) y mismos contextos.
- **Generación (G):** `solo-generacion=true`. Se mide solo el plan inicial y no se pagan adaptaciones.
- **Adaptación (A):** generación determinista, así que todas las variantes reciben los mismos planes y las mismas situaciones. Ciclos con IA: O03, O04, O05, O06, O12, O14, O17, O18, O19, O20, más los que surjan en el resto de la cohorte con el guion congelado.
- **Versión del modelo:** se registra antes y después de cada bloque en `versiones.txt`; el backend no la puede fijar. Si cambia a mitad, los bloques afectados se separan y cada comparación se hace solo dentro de la misma versión. No se toca producción para fijarla.
- **Controles contemporáneos:** cada variante se mide en la misma sesión que su control y con la misma versión.

## 3. Presupuesto (independiente de GRA, BASE y MEJ)

**Precio comprobado:** el coste medido en MEJ con la versión vigente fue:
- generación ≈ USD 0,024 por llamada (MEJ-C: 39 llamadas y USD 0,839, incluidas unas 12 de adaptación);
- adaptación ≈ USD 0,0137 por llamada (MEJ-A0/A1/A2: 10 llamadas ≈ USD 0,137 cada bloque).

Tarifa del registro: 0,25 USD por millón de tokens de entrada y 2,00 por millón de salida. El coste se mide con las métricas de cada predicción.

| Bloque | Predicciones esperadas | Peor caso (3 intentos) | USD esperado |
|---|---:|---:|---:|
| Una corrida G (19 perfiles) | 22–29 | 57 | 0,55–0,70 (1,40 en el peor caso) |
| Una corrida A (10 sesiones) | 10–12 | 30 | 0,14–0,17 |

| Fase | Contenido | Tope acumulado |
|---|---|---:|
| **Exploratoria** | A21, A22A, A24, G17, G18A, G18B, G18C (una corrida de cada una, en este orden) | **USD 4,00** |
| **Confirmatoria** | Segunda corrida, en orden inverso, del control y de las variantes que no muestren una regresión clara. Con el remanente, una tercera del control y de la mejor candidata de cada lado | **USD 9,00** |
| **Reserva** | Incidencias del proveedor: completar perfiles no ejecutados (regla I-MEJ-1) | **USD 10,00** |

**Control del gasto:**
- Antes de cada bloque, `correr_MEJ_R2.sh` suma el gasto real de las carpetas `MEJR2_*` y pasa el remanente de la fase al arnés como tope duro (`max-llamadas` / `max-usd`).
- El arnés detiene todo ante un timeout o una predicción sin métricas; esta se reserva a USD 0,05.
- Nunca se superan USD 10.
- **No se repite ninguna respuesta válida.** Solo se completan los perfiles que no llegaron a ejecutarse por una incidencia del proveedor, igual en todas las condiciones.

## 4. Medidas

**Generación** (por perfil y en total; instrumento: `generacion_ia.csv`, `limitaciones.jsonl`, tablas de la base y prompts):
- primeras propuestas válidas, propuestas totales, rechazadas, hallazgos por código (V15, V26, V24, V23 y resto) y reintentos;
- planes publicados desde la IA y respaldos deterministas;
- latencia y coste;
- `DOSIS_OBJETIVO` / `DOSIS_LIMITADA`, series por grupo grande frente a su objetivo, minutos aeróbicos;
- sesiones usadas, segundos estimados de cada sesión frente a M×60, colocaciones por sesión, sesiones con menos de 3 ejercicios de trabajo (criterio de `CalidadDosis`);
- **DOSIS_LIMITADA inevitable o mejorable**, con datos existentes y sin umbrales nuevos: es *mejorable* si el generador determinista, con el mismo perfil, catálogo y política, alcanza `DOSIS_OBJETIVO` o más series en ese grupo; es *inevitable* si el determinista tampoco llega. La referencia determinista sale de una corrida gratuita `DETERMINISTA` con `solo-generacion`.

**Adaptación** (oráculo v1.4, `factibilidad.csv`, tablas):
- aplicadas, rechazos de V8C y del validador, respaldos;
- SIN_EFECTO evitable (la sonda encontró alguna modificación válida);
- pertinencia ACC/ALT/CONS-J/INC y respuesta a la causa;
- trabajo antes y después, dificultad y conservación de las no señaladas;
- **cambios no autorizados**, con dos vigilancias independientes de V8C (hallazgos H-A1 y H-A2):
  - retirada sin autorización: retirados > añadidos, por identidad, en una intervención distinta de ACORTAR y SEGURIDAD_ANTE_DOLOR, o más de una retirada en ACORTAR;
  - cambio de `rest_seconds` en un ejercicio que se queda.

  Las dos se calculan desde las tablas, no desde el emparejamiento posicional del oráculo (artefacto de O04).

## 5. Análisis y reglas de decisión

- **Comparación pareada por perfil:**
  - McNemar exacta para la validez a la primera;
  - Wilcoxon de rangos con signo para las medidas continuas (series por grupo, aprovechamiento del tiempo).
  - Con dos corridas, se informa cada una y la combinada (pares perfil × corrida). Se dan intervalos o rangos, no solo el valor puntual.
- **Una variante es candidata solo si cumple todo esto:**
  1. mejora sus propios indicadores (G1: rechazos y hallazgos V15/V26; G2: dosis por grupo o DOSIS_LIMITADA mejorable; G3: aprovechamiento del tiempo y sesiones de menos de 3 ejercicios, incluido O10; A1: SIN_EFECTO evitables y coherencia; A2: pertinencia y respuesta a la causa);
  2. sin más hallazgos V24 que su control;
  3. sin estructuras rotas (V2, V13, V18, V23, V25) ni más respaldos atribuibles a la variante;
  4. sin peores adaptaciones (más INC, menos aplicadas o más SIN_EFECTO evitables);
  5. sin más cambios no autorizados;
  6. ningún plan ni adaptación publicados con violaciones.
- **Si la mejora no alcanza significación**, se etiqueta «exploratoria / no concluyente». Una regresión clara en la fase exploratoria (más V24, cambios no autorizados o estructuras rotas) excluye la variante de la confirmatoria.
- **Calidad, no solo validez:** cada plan y adaptación se revisa también por su contenido. Las observaciones de calidad deportiva no son defectos técnicos y no sustituyen la revisión profesional.

## 6. Garantías

- **Antes de pagar:** regresión completa (unitarias e integración) sobre el commit que se ejecuta; pruebas de las variantes; prompts volcados gratis (`PROMPTS`) y revisados.
- **Producción:**
  - variantes apagadas por defecto;
  - prompts históricos intactos;
  - sin cambios en Neon ni en mobileapp;
  - sin merge, sin despliegue y sin PR de adopción sin aprobación;
  - los resultados de GRA, BASE y MEJ no se tocan.
- **Defectos:** un defecto de una regla que aparezca durante la ronda se documenta aparte (`INCIDENCIAS_MEJ_R2.md`) y nunca se oculta con un ajuste de prompt.
- **Credencial:** se inyecta por el proxy. Antes de archivar, la evidencia se revisa en busca de `r8_` y `Bearer`.

## 7. Congelación (09/11/2026, antes de la primera llamada pagada)

- **Código ejecutado:** commit `1aac853`. Las variantes pr-1.8a/b/c, v2.2a y v2.4 están apagadas por defecto.
- **Regresión sobre `1aac853`:** 1316 unitarias sin fallos y 593 de integración con 3 fallos, los mismos dependientes de la fecha que en `42d9dc1` (I-R2-1).
- **Ejecutor:** `correr_MEJ_R2.sh`, SHA-256 `71d46f869b4a00c6490cf9a0336600d2bfa122daad13e8ab98bd670fdb75988c`.
- **Instrumento:** `analisis_mej_r2.py`, SHA-256 `8e80067baaea32bc5f069f285809f7a6c80b1af822e461a07fb9066005e5f50d`, y oráculo v1.4 sin cambios.
- **Prompts volcados gratis:** `PROMPTS_*`, de las 5 variantes de generación con los 20 perfiles. Adaptación simulada (`SIM_ADA_*`) de v2.1, v2.2a y v2.4, sin llamadas. Revisados: O10 recibe `CARDIO_1=480 s`, `STRENGTH_1=354 s` y 1500 s de tiempo útil; en GM, pr-1.8b no ofrece la serie extra (REV2 sin piso).
- **Gasto antes de empezar:** 0 predicciones, USD 0.
