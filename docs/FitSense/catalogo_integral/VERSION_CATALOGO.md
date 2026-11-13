# Versión auditada del catálogo

| Campo | Valor |
|---|---|
| Identificador | **FS-CAT-2026.11-INTEGRAL-R1** |
| Base | `generation` @ `623c4fa` (Flyway V1–V46) |
| Migraciones de la auditoría | V47 (banderas), V48 (dificultad conservadora), V49 (textos), V50 (clasificación y equipamiento) |
| Estado de Flyway esperado | última versión aplicada `50` (ver `linea_base/flyway_final.tsv`) |
| Huella de clasificación, equipamiento y banderas (`src/test/resources/catalogo/huella_catalogo.tsv`) | SHA-256 `304dd658be7318b2b2dc7e1397155d13d4db3984b73fb07bfd2036030fd44062` |
| Exportación completa tras V50 (`linea_base/catalogo_final_V50.tsv`) | SHA-256 `83cdefeaefbf00fc87cec9aad785247c968c23159fc5f416fff661c8f9957217` |
| Línea base antes de la auditoría (`linea_base/catalogo_linea_base_V46.tsv`) | SHA-256 `c913da0c11bba42771defa135ae19c69a5b210006aa17aa4095a286c0d1b7f5b` |
| Commits | los de las ramas `catalogo/integral-1-seguridad` → `catalogo/integral-2-textos` → `catalogo/integral-3-clasificacion` → `audit/correccion-integral-catalogo` (ver las PR) |

## Cómo se reproduce

1. Aplicar las migraciones hasta V50 sobre una base vacía. Cualquier IT de Testcontainers lo hace, y `CatalogoLineaBaseIT` exporta el catálogo con `-Dfitsense.catalogo.exportar=true`.
2. Comprobar que `CatalogoCalidadIT` pasa: la huella coincide byte a byte.
3. Comparar la exportación con `linea_base/catalogo_final_V50.tsv`.

## Qué significa «auditada» en esta versión

Significa que los 1.219 ejercicios activos tienen revisión automatizada y revisión visual del GIF original, hecha por revisores de IA, más verificación propia de todos los cambios de seguridad y de clasificación.

**No significa validación profesional:** ningún ejercicio la tiene todavía. Tampoco significa que el catálogo esté saneado del todo: quedan pendientes 59 visuales, 320 profesionales y 11 técnicos, y 16 ejercicios siguen contenidos (ver `PENDIENTES_Y_LIMITACIONES.md`).
