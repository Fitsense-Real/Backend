# Respaldo del trabajo Flutter (no es código del backend)

**Rama huérfana de preservación.** No se fusiona y no tiene PR. Guarda el commit `fdc9439` de `FitSense-2-0/mobileapp`, rama `feature/mvp-integracion-api`. Ese commit **no está subido** al repositorio móvil: el *push* devolvió 403 porque la app de GitHub de Claude no tiene acceso a esa organización.

| Archivo | SHA-256 |
|---|---|
| `flutter/mobileapp-mvp-integracion-api.bundle` | `669e80b6f84b9edc3b731c4e5a7be5a5a9e1bab36d7c16368a8e6cb0f4e834aa` |
| `flutter/mobileapp-mvp-integracion-api.patch` | `82ba5785291b73d5af0d783d942dec2ba4a7a9218a05cdad31cba8cc4bffdea8` |

**Verificado el 02/11/2026:**
- el *bundle* contiene `refs/heads/feature/mvp-integracion-api` → `fdc9439c75a965c68ce523eada5b903dfbbff22d` y requiere `82105e3` (`main`);
- el *patch* aplicado sobre `main` da **el mismo árbol** (`5c4d7cec…`).

## Restaurar

Dentro de una copia de `FitSense-2-0/mobileapp` que tenga `main` en `82105e3`:

```bash
git fetch <ruta>/mobileapp-mvp-integracion-api.bundle feature/mvp-integracion-api:feature/mvp-integracion-api
git checkout feature/mvp-integracion-api
git log --oneline -1   # fdc9439
git push -u origin feature/mvp-integracion-api   # rama nueva: no sobrescribe nada
```

**Alternativa con el *patch*:**

```bash
git checkout -b feature/mvp-integracion-api 82105e3
git am mobileapp-mvp-integracion-api.patch
```

**Archivos y pruebas:** `flutter/ARCHIVOS_FDC9439.txt` y `docs/mvp/PRUEBAS_INTEGRACION_MVP.md`, dentro del propio *patch*:
- 49 pruebas unitarias aprobadas;
- 1 prueba de integración real contra el backend local, aprobada;
- `flutter analyze` sin incidencias nuevas.
