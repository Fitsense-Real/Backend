# Inventario inicial del catálogo (línea base congelada)

Línea base: catálogo resultante de aplicar **todas las migraciones vigentes de `generation`** (V1–V46; `623c4fa`) sobre PostgreSQL 16 efímero, exportado con `CatalogoLineaBaseIT` (opt-in, reproducible en cualquier commit).

- Archivo: `linea_base/catalogo_linea_base_V46.tsv` (SHA-256 `c913da0c11bba42771defa135ae19c69a5b210006aa17aa4095a286c0d1b7f5b`).
- Historial de Flyway aplicado: `linea_base/flyway_linea_base.tsv`.
- Catálogo tras las correcciones (V47–V50): `linea_base/catalogo_final_V50.tsv` (SHA-256 `83cdefeaefbf00fc87cec9aad785247c968c23159fc5f416fff661c8f9957217`).

La referencia no se modifica: cualquier comparación antes/después se hace contra ese archivo.

## Recuentos

| Concepto | Ejercicios |
|---|---|
| Total de filas en `exercises` | 1376 |
| Catálogo original de la fuente (hasaneyldrm/exercises-dataset) | 1.324 |
| Siembra mínima MVP (V8), retirada | 52 |
| **Activos** | **1219** |
| Inactivos (excluidos de la elegibilidad) | 157 (52 MVP + 105 de la fuente) |
| Retenidos para todos (INC-4C-03) | 7 (813, 858, 861, 873, 885, 1192, 1298) |
| Contenidos de forma condicionada (INC-CAT-1374) | 1 (1374) |
| Contenidos preventivamente (INC-CAT-C39, PR #55, sin fusionar) | 26 (solo con la PR #55) |
| Activos sin GIF o sin imagen en la fuente | 0 (los 1.219 GIF e imágenes existen en el dataset) |
| Activos sin `name_es`, `instructions_es`, pasos, patrón o `is_isolation` | 0 |
| Nombres en español repetidos entre activos | 24 nombres (48 ejercicios) |
| GIF idénticos (bytes) entre activos | 0 |
| Activos que alguna migración ≥ V10 cita por id (heurística) | 314 |

Banderas de seguridad entre los activos de la línea base: alto impacto 56, suelo 224, carga axial 142, rodillas 61, colgarse 47, sobre la cabeza 259, inversión 35.

Migraciones que más activos tocan (heurística por ids citados; ver `linea_base/migraciones_por_id.json`): V38 (154), V37 (101), V39 (43), V41 (15), V36 (8), V40 (7), V28 (3), V46 (2).

## Distinción catálogo original / vigente

- **Original:** los 1.324 registros de la fuente (V9). Todos entran inactivos, con `name_es` nulo y dificultad 2 de relleno.
- **Vigente:** el resultado de V10–V46: activación, traducciones, banderas (V10, V25, V36–V38), patrón (V26), equipamiento N:M (V23, V29, V38, V39), dificultad (V21, V27) y correcciones de catálogo (V36–V41, V46).
- Las instrucciones `instructions_en` son las de la fuente. `instructions_es` es su traducción. Los textos de la fuente **no** se revisaron contra el GIF hasta esta auditoría.
