# 09 · CI y commits

**PR #39** (`experiment/pain-localized-removal` → `experiment/difficulty-guard`), **borrador**.

| Commit | Contenido |
|---|---|
| `d4a05c9` | Laboratorio opt-in de factibilidad (sin cambiar producción) |
| `39ebe41` | V8C: `RetiradaSeguridad` y su autorización en la continuidad + 13 pruebas unitarias |
| `fbfa121` | Productor: la retirada en el respaldo con dolor, tras `fitsense.adaptation.pain-localized-removal` (off) |
| `e8b86c7` | Texto publicado: la retirada como cambio de seguridad (solo con la nota `RETIRADA_SEGURA`) |
| `e6f294c` | Pruebas permanentes con PostgreSQL (B, C, sin límite, por defecto) |
| `b8c283d` | Laboratorio de 22 perfiles: columnas de retirada, trabajo, dosis, linaje y excluidos (**SHA de la regresión local, 08**) |
| `86ce1ed` | Entregables 01–07 y evidencia — CI `verify` ✔, run 41765276890 |
| (siguientes) | Entregables 08–10 y evidencia de regresión: solo documentación |

**CI del commit final:** el job `verify` (`mvnw verify -Pintegration`, configuración por defecto) se comprueba sobre el último SHA del PR. El resultado se da en el informe final de la sesión, porque este documento forma parte de ese commit.

**Configuración**

| Propiedad | Env | Por defecto | Valores |
|---|---|---|---|
| `fitsense.adaptation.pain-localized-removal` | `FITSENSE_PAIN_LOCALIZED_REMOVAL` | **`off`** | `on`; otro valor → la aplicación no arranca |
| `fitsense.adaptation.difficulty-guard` (de #38) | `FITSENSE_DIFFICULTY_GUARD` | **`off`** | `on`, `preferencia` |

**Reversión:** `off` y reiniciar. Sin migraciones.
