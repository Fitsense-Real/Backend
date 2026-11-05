# D · Evidencia de pruebas (gratuitas; sin Replicate)

## 1. Commits de la rama aislada `investigacion/catalogo-v46` (desde `ae5fdd4`)

| Commit | Contenido | ¿Toca `src/main`? |
|---|---|---|
| `d733af2` | `ExportarCatalogoIT` (exportación, solo lectura); `sql-previo` en el barrido; escaneo; variantes contrafactuales | No (solo pruebas y documentos) |
| `ad6c900` | **V46** (traducciones de 973 y 1263) + `CatalogoV46IT` | Solo la migración V46 (`name_es`) |
| `b7324e0` | `sql-previo` en el arnés K20 (vacío por defecto) | No |
| Resto | Documentos y evidencia | No |

**Ningún cambio en el código Java de producción.** Sin cambios en V24, V8C, HIST, REV2, el prompt, los oráculos ni la política de adaptación.

## 2. Pruebas ejecutadas y resultados

| # | Prueba | Versión | Resultado |
|---|---|---|---|
| 1 | `CatalogoV46IT` (nombres; la migración solo cambia `name_es`; sin reglas por nombre) | V46 | **3/3** |
| 2 | Unitarias completas | V46 (`ad6c900`) | **1.301 / 0 fallos** (1 omitida), BUILD SUCCESS |
| 3 | Integración completa (`verify -Pintegration`, política por defecto HIST) | V46 | INTEGRACION_RESULTADO |
| 4 | Barrido HIST (3.780 perfiles) | V46 | **Idéntico byte a byte** a `HIST_cierre` (planes y resultados) |
| 5 | Barrido REV2 | V46 | **Idéntico byte a byte** a `REV2_cierre` |
| 6 | K20 REV2 D sobre U / N | V46 | `a662cb4d` / `11f51d44`: **iguales** al original |
| 7 | K20 HIST D sobre U | V46 | `f9f67973` (**huella congelada**) |
| 8 | K20 REV2 con IA simulada V2.1 sobre U / N | V46 | `84dfb6d2` / `da34db35`: **iguales**. Prompts idénticos (hashes 16/16 y 28/28) |
| 9 | Catálogo original reproducible: K20 D sobre U / N con `spring.flyway.target=44.3` | `ad6c900` | 47 migraciones, checksum `666694b7…` (= original), huellas `a662cb4d` / `11f51d44` |
| 10 | Línea base de los barridos contrafactuales (sin `sql-previo`) | `d733af2` | Idéntica byte a byte a `REV2_cierre` y `HIST_cierre` |
| 11 | Barridos contrafactuales CF1, CF1b y CF2 × REV2/HIST | `d733af2` | `C` §3 |
| 12 | K20 con IA simulada × {original, CF1b, CF2} × {U, N} | `b7324e0` | `C` §4: 0 incumplimientos y 0 cambios no pertinentes en todas |
| 13 | Revisión visual asistida (24 ejercicios, 8 fotogramas cada uno) | — | `A` |

**Qué cubre la batería frente a lo que pediste en la orden** (§8), a través de la integración completa y los barridos:

| Área | Cobertura |
|---|---|
| Generación inicial | 4 objetivos × 3 niveles × 3 equipamientos × 1–7 días × 15 duraciones (barridos); S-MAX (`FrecuenciaPorNivelIT`); sesiones cortas; dosis y duración (V20, V24, V25) |
| Adaptación | Fatiga, «demasiado difícil», desmotivación, adaptación localizada, sustituciones equivalentes, sesiones no señaladas y V8C (K20 con IA simulada y las IT de adaptación de la batería de integración) |
| Progresión y reincorporación | `ProgresionRev2IT`, `PendientesProgresionRev2IT`, `ReparacionLinajeRev2IT` y otras de la integración; sin restauración automática |
| Compatibilidad | HIST byte a byte; huellas congeladas; catálogo original reproducible |

## 3. Huellas y artefactos (SHA-256 en cada carpeta)

| Carpeta (`evidencia/`) | Contenido |
|---|---|
| `barridos_contrafactuales/` | 6 barridos (CF1, CF1b, CF2 × REV2/HIST): `planes.csv.gz`, `barrido_generacion.csv`, `sql_previo.sql`, `SHA256SUMS` |
| `k20_ia_simulada_variantes/` | 6 corridas K20 (`.tar.gz`) + evaluación con los criterios del protocolo 23 |
| `regresion_v46/` | 7 corridas K20 (`.tar.gz`) y huellas de los barridos V46 |
| `C_IMPACTO_VARIANTES.csv` | Tabla de impacto |
| `../escaneo_catalogo.csv` | Detecciones sistemáticas |

**El catálogo exportado y las hojas de fotogramas no se versionan:** derivan de GIF con copyright. Se regeneran con `ExportarCatalogoIT` y `revision_visual/hoja_fotogramas.py`.

## 4. Diferencias causadas por la nueva versión del catálogo

**V46: ninguna en la selección.**
- Mismos planes, mismas huellas y mismos prompts.
- Solo cambia `name_es` en la base (lo que se muestra).
- Cambian `flyway_version` (46) y el número de migraciones (48) en `00_configuracion.txt`.

**Las variantes contrafactuales sí cambian la selección (`C`):** son mediciones, no correcciones, y no están en ninguna migración.
