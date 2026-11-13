-- SOLO PARA PRUEBAS CON CATALOGO FIJADO (EsquemaVigenteSobreCatalogoAnterior). Columnas de V51 y V53 SIN DATOS:
-- impact_level e is_stretch quedan NULL, asi que ImpactoPolicy y CatalogEligibilityPolicy usan la regla por nombre de
-- entonces (comportamiento identico al catalogo fijado), y requires_suspended_support queda FALSE (como antes de V53).
-- No se aplican V51 ni V53 reales: rellenan valores del catalogo V50 y cambiarian la evidencia historica.
ALTER TABLE exercises ADD COLUMN IF NOT EXISTS impact_level VARCHAR(10);
ALTER TABLE exercises ADD COLUMN IF NOT EXISTS is_stretch BOOLEAN;
ALTER TABLE exercises ADD COLUMN IF NOT EXISTS requires_suspended_support BOOLEAN NOT NULL DEFAULT FALSE;
