# Copia histórica: no usar con db/migration

Desde el 30/10/2026 (decisión F):
- `v37/separables/V37_1__…` es la **V38 oficial** (`src/main/resources/db/migration/V38__catalog_classification_equipment.sql`);
- `v38/candidata/V38__…` es la **V39 oficial** (`src/main/resources/db/migration/V39__catalog_functional_equipment.sql`).

El contenido es el mismo salvo la cabecera, los mensajes y los nombres de los objetos temporales (`v38_*`, `v39_*`).

**No añadir este directorio a `spring.flyway.locations`** junto con `db/migration`: los cambios se aplicarían dos veces y las migraciones abortarían por el estado previo. Se conserva como evidencia de lo que se simuló antes de la aprobación y lo usan `CatalogoV37IT` y los scripts de simulación históricos.
