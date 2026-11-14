-- SOLO PARA PRUEBAS CON CATALOGO FIJADO (EsquemaVigenteSobreCatalogoAnterior). Columnas de V51 SIN DATOS:
-- impact_level e is_stretch quedan NULL, asi que ImpactoPolicy y CatalogEligibilityPolicy usan la regla por nombre de
-- entonces (comportamiento identico al catalogo fijado). No se aplica V51 real: rellena valores del catalogo V50 y
-- cambiaria la evidencia historica.
ALTER TABLE exercises ADD COLUMN IF NOT EXISTS impact_level VARCHAR(10);
ALTER TABLE exercises ADD COLUMN IF NOT EXISTS is_stretch BOOLEAN;
