# Preservación del trabajo de Flutter (fase 8)

**El código Flutter NO está subido a `FitSense-2-0/mobileapp`.** El *push* devolvió **403** dos veces, la segunda en esta orden: la app de GitHub de Claude no tiene acceso a esa organización. **No se ha sobrescrito ninguna rama remota del repositorio móvil.**

## 1. Qué se conserva

| Elemento | Valor |
|---|---|
| Commit | `fdc9439c75a965c68ce523eada5b903dfbbff22d`, rama `feature/mvp-integracion-api` |
| Base | `82105e3` (`main` de `FitSense-2-0/mobileapp`) |
| Cambio | 93 archivos, +5.349 / −547 |

## 2. Verificación de integridad (02/11/2026)

| Comprobación | Resultado |
|---|---|
| `git bundle verify` | Correcto. Requiere `82105e3`; contiene `refs/heads/feature/mvp-integracion-api → fdc9439` |
| Restaurar el *bundle* en un clon nuevo de `mobileapp` | `fdc9439`, 93 archivos |
| Aplicar el *patch* (`git am`) sobre `origin/main` | Mismo árbol que el *bundle*: `5c4d7cec8a597835a5faafa8e012b171035452b3` |
| SHA-256 del *bundle* | `669e80b6f84b9edc3b731c4e5a7be5a5a9e1bab36d7c16368a8e6cb0f4e834aa` |
| SHA-256 del *patch* | `82ba5785291b73d5af0d783d942dec2ba4a7a9218a05cdad31cba8cc4bffdea8` |

## 3. Ubicaciones persistentes (no dependen de `/tmp`)

| Ubicación | Estado |
|---|---|
| **GitHub:** rama huérfana `respaldo/flutter-mvp-integracion-api` de `Fitsense-Real/Backend` (commit `5e4c72c`; sin PR y sin historia compartida con el backend) | Subida. **Descargada de nuevo** desde GitHub; los dos hashes coinciden |
| Archivos enviados al usuario (*bundle*, *patch* y los 5 documentos de `docs/mvp`) | Entregados en la conversación del 02/11/2026 |
| Contenedor de trabajo (`/tmp/…/scratchpad`) | **Temporal:** no contar con él |

## 4. Restaurar la rama

```bash
git clone https://github.com/FitSense-2-0/mobileapp && cd mobileapp
git fetch https://github.com/Fitsense-Real/Backend respaldo/flutter-mvp-integracion-api:respaldo-flutter
git show respaldo-flutter:flutter/mobileapp-mvp-integracion-api.bundle > /tmp/mvp.bundle
git fetch /tmp/mvp.bundle feature/mvp-integracion-api:feature/mvp-integracion-api
git checkout feature/mvp-integracion-api && git log --oneline -1   # fdc9439
git push -u origin feature/mvp-integracion-api   # rama nueva: no sobrescribe nada
```

**Alternativa sin *bundle*:**

```bash
git checkout -b feature/mvp-integracion-api 82105e3
git am mobileapp-mvp-integracion-api.patch
```

## 5. Archivos y pruebas

**Archivos:** la lista completa está en `flutter/ARCHIVOS_FDC9439.txt`, dentro de la rama de respaldo.
- Núcleo: `core/api`, `api_exception`, `dio_client` e interceptor.
- Autenticación, ajustada al contrato real.
- Funcionalidades: `profile`, `plan`, `workout` y `progress`.
- Dashboard con datos reales.
- Pruebas y `docs/mvp/` (5 documentos).

**Pruebas ejecutadas el 02/11/2026:**
- `flutter analyze`: sin incidencias nuevas;
- 49 pruebas unitarias aprobadas;
- 1 prueba de integración **real** contra el backend local, aprobada;
- **la interfaz no se ha ejecutado** en ningún emulador ni dispositivo.

**Contratos del backend que la app todavía no usa** (son aditivos: la rama sigue siendo compatible):
- `motivoCodigo` (#22);
- el 401 del login (#23; la app ya trata el 401);
- la recuperación de contraseña (#25).

**El trabajo de Flutter queda detenido hasta tu autorización.**
