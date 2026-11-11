# 01 · Inventario de campañas y evidencias

**Criterio:** una campaña solo se da por completa si su evidencia lo demuestra (corridas, registros de predicciones y huellas), no porque exista su carpeta. Los ZIP **no se duplican aquí**: se citan por ruta, rama y commit.

## 1. Ramas con evidencia

Commits comprobados en `origin` el 11/11/2026.

| Rama | Commit | Contenido |
|---|---|---|
| `generation` | `543932a` | Código vigente; GRA (`docs/FitSense/evaluacion_integral_O/generacion_ia_real/`); BASE (`docs/FitSense/evaluacion_integral_O/baseline_sin_reglas/`); V8C (código y pruebas) |
| `experimento/mejora-ia` | `1d5c28f` | MEJ (`docs/FitSense/mejora_ia/`), MEJ ronda 2 (`docs/FitSense/mejora_ia/ronda2/`) y CONF-IA (`docs/FitSense/mejora_ia/confirmacion/`). **No está fusionada en `generation`** |
| `experimento/confirmacion-pr18a-final` | `ffca1f4` | Confirmación de pr-1.8a (`docs/FitSense/mejora_ia/confirmacion_pr18a/`). **No está fusionada** |

## 2. Resumen de campañas

| Campaña | Fecha (UTC) | Objetivo | Rama / commit ejecutado | Versión del modelo | Prompts | Perfiles | Predicciones | Coste USD | Estado |
|---|---|---|---|---|---|---|---|---|---|
| **GRA-R1 / GRA-R2** | 06/11/2026, 05:13–06:44 | Generación inicial + adaptación con IA real, flujo completo | Producción `f1c6c02`; arnés `c8ff1bf`; congelación `b60e5ea`; entregables `3883054` (`generation`) | `9f4cd9ec…` | PR-1.7 + V2.1 | O01–O20 + O11-F35 | **76** (R1 40, R2 36) | **1,5368** | **Completa** (2 corridas) |
| **BASE-R1 / BASE-R2** | 07/11/2026 (congelación `ba2f16e` 21:24; resultados `e0a6441` 22:09) | Mismo modelo **sin reglas**; contar infracciones | Arnés de solo lectura `6a507f8` (`generation`) | `9f4cd9ec…` solicitada; el proveedor la devuelve como `hidden` (**no verificable**) | Plantillas BASE de generación y adaptación | O01–O20 | **60** (30 + 30) | **0,9594** | **Completa**. La carpeta `parcial_BASE_R1/` era provisional, con las mismas cifras |
| **MEJ** | 07/11 23:49 – 08/11 01:22 | Mejorar solo los prompts: rechazos, dosis, SIN_EFECTO, pertinencia | `e851de7`, `c7b4b9d`, `ca4ef1f` (`experimento/mejora-ia`) | `045f8102…` | PR-1.7 frente a PR-1.8; V2.1 frente a V2.2 | 19 factibles + O11; 12 sesiones de adaptación | **102** | **2,00** (1,9999) | **Completa**, con una corrida por condición (G2 sustituida por controles, E-1) |
| **MEJ ronda 2** | 09/11/2026, 06:03–22:08 | Variantes pr-1.8a/b/c, v2.2a y v2.4; auditoría de reglas | `1aac853` (congelación `a57abd2`; cierre `0878f85`) | `045f8102…` | 4 de generación + 3 de adaptación | 19 factibles + O11 | **362** (R1 135; R2+R3 227) | **7,9051** | **Completa**. G17 R2 reconstruida sin métricas de dosis ni tiempo (I-R2-9) |
| **CONF-IA** | 10/11/2026 | Confirmar pr-1.8c y pr-1.8a, v2.2a y el flujo conjunto con V8C corregido | `ab04e67` (congelación `957a08d`) | `045f8102…` | PR-1.7, pr-1.8c, pr-1.8a; V2.1, v2.2a | 19 factibles; 12 sesiones de adaptación | **336** | **7,0950** | **Completa** en C1–C3, A1, D1–D5 e I1. pr-1.8a solo tuvo **una ronda** (A1) por presupuesto |
| **pr-1.8a final** | 10/11/2026 (congelación `5f48dd5`) | Confirmar pr-1.8a con la versión actual del modelo | `experimento/confirmacion-pr18a-final` (código `f95b218`) | `81cb6179…` | PR-1.7 frente a pr-1.8a | 18 en A2 (O09 excluido) | **99** | **2,6115** | **A2 completa** (decide). **A3 incompleta**: excluida de la decisión |

**Total de las seis campañas:** 1035 predicciones y **USD 22,1077**, con MEJ en USD 1,9999 medidos (USD 22,1078 si se toma el redondeo de 2,00).

## 3. Detalle por campaña

### GRA-R1 y GRA-R2
- **Condiciones:**
  - bases efímeras limpias;
  - ninguna respuesta reutilizada;
  - adherencia simulada por guion congelado;
  - PLAN-REV-2.0, con progresión activada en el bloque P.
- **Indicadores:** válidos a la primera, rechazos y motivos, respaldos, aplicadas y SIN_EFECTO, pertinencia, oráculo v1.4 (10 aspectos), progresión y reproducibilidad.
- **Resultados clave:**
  - generación: 13/20 y 14/20 válidos a la primera (12/19 y 13/19 sin O11-F35), 20/20 publicados desde la IA y 0 respaldos;
  - adaptación: 10/10 y 9/10 publicadas;
  - oráculo: 150/150 y 148/148 aspectos aplicables aprobados.
- **Incidencias:** ningún defecto del sistema (`entregables/INCIDENCIAS_Y_CORRECCIONES.md`).
- **Evidencia:**
  - `generacion_ia_real/entregables/` (informe, tablas, 7 CSV, xlsx);
  - `entregables/GRA_EVIDENCIA_COMPLETA.zip`;
  - `evidencia/versiones.txt`.
- **Integridad:**
  - `SHA256SUMS.txt`: 65 entradas; 62 OK en el repositorio y 3 `*.log` excluidos por `.gitignore`, comprobados contra su copia del ZIP, así que **65/65**;
  - `SHA256SUMS_CONGELACION.txt` 5/5 OK.

### BASE-R1 y BASE-R2
- **Condiciones:**
  - se retiraron dosis, división, límites, reglas 1–20, normalizador, respaldo y reintentos;
  - en adaptación, sin permisos ni V8C;
  - se mantuvieron el perfil, el catálogo filtrado y el esquema;
  - validación solo a posteriori, sin publicar nada.
- **Indicadores:** planes con infracciones, infracciones en crudo y normalizadas, cambios no autorizados (C1–C12).
- **Resultados clave:**
  - 19/19 planes con infracciones en cada corrida, frente a 7/19 y 6/19 en GRA;
  - adaptación: 10/10 ciclos con cambios no autorizados, frente a 0/10.
- **Incidencias:**
  - Cloudflare 1010, corregido antes de pagar;
  - corte de red en O16, recuperado por id;
  - I-1, el contador de DIVISION, corregido.
- **Evidencia:** `baseline_sin_reglas/` (protocolo, resumen, tablas, prompts, CSV) y `EVIDENCIA_BASELINE_SIN_REGLAS.zip` (197 archivos).
- **Integridad:** `SHA256SUMS.txt` **24/24 OK**; el ZIP se abre sin errores.

### MEJ
- **Condiciones:** una corrida por condición; comparación pareada; tope de 120 predicciones y USD 3.
- **Resultados clave:**
  - PR-1.8: 15/19 frente a 12/19 (p = 0,45), con 13 V24 en O18;
  - V2.2: 12/12, igual que V2.1.
- **Incidencias:**
  - E-1: cambio de versión del modelo respecto a GRA;
  - I-MEJ-1: timeout en G1 O03, estado desconocido en C O18 c1, O08 `failed`; todo completado con G1C, G1R y CC.
- **Evidencia:** `mejora_ia/PROTOCOLO_MEJ.md`, `RESULTADOS_MEJ.md`, `metricas_mej.json` y `evidencia/MEJ_corridas_y_oraculos.tar.gz` (908 entradas).
- **Integridad:** `mejora_ia/SHA256SUMS.txt` **5/5 OK**; el tar se lee sin errores.

### MEJ ronda 2
- **Condiciones:**
  - exploratoria R1 y confirmatoria R2 (orden inverso) + R3 (orden directo);
  - α = 0,025 para dos variantes;
  - criterios fijados antes de pagar.
- **Resultados clave:**
  - confirmatoria: 21/38, 27/38 y 28/38 (p = 0,11 y 0,039), sin ninguna candidata;
  - adaptación: 35/36, 36/36 y 36/36;
  - la auditoría encuentra H-A1 y H-A2.
- **Incidencias:** I-R2-0 a I-R2-13 y la enmienda E-R2-1 (`ronda2/INCIDENCIAS_MEJ_R2.md`).
- **Evidencia:** `ronda2/` (recomendación, auditoría, protocolo, CSV) y `EVIDENCIA_MEJ_RONDA_2.zip` (2978 archivos, 18 MB).
- **Integridad:**
  - **no existe `SHA256SUMS.txt`** para esta ronda;
  - las huellas del ejecutor (`correr_MEJ_R2.sh` 71d46f86…, `correr_MEJ_R2_compleccion.sh` 18bc96f5…) y del análisis (`analisis_mej_r2.py` c328532b…) coinciden con las del protocolo y las incidencias;
  - el ZIP se abre sin errores.
  - Es una **integridad parcial**: no hay manifiesto completo de huellas.

### CONF-IA
- **Condiciones:**
  - V8C corregido;
  - protocolo con huellas congelado antes de pagar;
  - regla de compleción I-MEJ-1: los timeouts se repiten en bloques nuevos y las huérfanas se recuperan sin repetir la llamada.
- **Resultados clave:**
  - pr-1.8c NO CUMPLE (34/57 frente a 41/57, p = 0,29, V24 2 frente a 0);
  - pr-1.8a FAVORABLE NO CONCLUYENTE (12/19 frente a 16/19, p = 0,28);
  - v2.2a NO PEOR, SIN VENTAJA DEMOSTRADA (58/60 frente a 60/60, p = 0,50);
  - I1: 148/148.
- **Incidencias:** I-C-1 a I-C-12, entre ellas 5 timeouts, 3 errores de E/S en el POST recuperados y 1 `failed` (`confirmacion/INCIDENCIAS_CONF_IA.md`).
- **Evidencia:** `confirmacion/` (recomendación, resultados, CSV, xlsx) y `EVIDENCIA_CONFIRMATORIA.zip` (2353 archivos, 15 MB).
- **Integridad:**
  - `SHA256SUMS_CONGELACION.txt` tiene 12 entradas: 8 archivos OK, 3 entradas `ab04e67:<ruta>` (prompts y V8C) OK con `git show | sha256sum`, y 1 registro informativo (la versión `045f8102…`, que no es un archivo);
  - el ZIP se abre sin errores.

### Confirmación final de pr-1.8a
- **Condiciones:**
  - traslado exacto de pr-1.8a, apagado por defecto;
  - regresión previa (1334/0/1 unitarias, 593/0/44 integración) y prompts byte a byte idénticos;
  - presupuesto: el resto del bloque de USD 10.
- **Resultados clave:**
  - A2: 13/18 frente a 13/18 (p = 1,0); fallan los criterios 2, 3 y 6;
  - **decisión B**.
- **Incidencias:**
  - I-P-2: error en el ejecutor (`pagado()` sobrescribía la variable del prompt). Los bloques M2 fallaron sin llamar al proveedor (0 llamadas, USD 0) y se corrigió;
  - I-P-4: O09, 3 timeouts de pr-1.8a;
  - en A3: 2 timeouts, 2 `failed`, 1 error de E/S recuperado y saldo agotado.
- **Evidencia:** `confirmacion_pr18a/` (recomendación, análisis V4/V24, resultados por objetivo, CSV, xlsx) y `EVIDENCIA_PR18A.zip` (1090 archivos, 7,6 MB).
- **Integridad:**
  - `SHA256SUMS_CONGELACION_PR18A.txt` tiene 16 líneas: 11 archivos, de los que 10 dan OK y 1 difiere (`correr_PR18A.sh`, modificado por la corrección I-P-2 y documentado). El resto son una cabecera y 4 registros informativos: huellas de los prompts concatenados, versión `81cb6179…` y commit `f95b218`;
  - el ZIP se abre sin errores.

## 4. Evidencia fuera del repositorio

Las carpetas de trabajo locales de CONF-IA y pr-1.8a (`conf_runs`, `p18a_runs`) eran temporales del entorno. Su contenido relevante está dentro de los ZIP citados.

## 5. Estado de integridad

| Campaña | Manifiesto | Resultado |
|---|---|---|
| GRA | `SHA256SUMS.txt` (65) + congelación (5) | **Íntegra**: 65/65 (3 contra la copia del ZIP) y 5/5 |
| BASE | `SHA256SUMS.txt` (24) | **Íntegra**: 24/24 |
| MEJ | `SHA256SUMS.txt` (5) | **Íntegra**: 5/5 |
| MEJ ronda 2 | — | **Parcial**: sin manifiesto; ejecutores y análisis verificados a mano |
| CONF-IA | Congelación (12) | **Íntegra**: 11/11 verificables y 1 registro informativo |
| pr-1.8a | Congelación (16 líneas) | **Íntegra**: 10/11 archivos, con una diferencia esperada y documentada (`correr_PR18A.sh`, I-P-2) |
