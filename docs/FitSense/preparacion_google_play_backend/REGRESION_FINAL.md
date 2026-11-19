# Regresión final (integración local de las cinco PR)

Fecha: 19/11/2026.

**Árbol probado:** la rama local `integracion/google-play-local` (`63308cb`), que **no se ha subido**, combina:

| PR | Rama | Cabeza probada | Base de la PR |
|---|---|---|---|
| A — eliminación de cuenta (Fitsense-Real/Backend#77) | `cuentas/eliminar-cuenta` | `cd07435` | `generation` (`a1ccf53`) |
| C — recuperación por correo (Fitsense-Real/Backend#78) | `cuentas/recuperacion-correo` | `028f54b` | A |
| B — eliminación web (Fitsense-Real/Backend#79) | `cuentas/eliminacion-web` | `57b1ae7` | C |
| D — edad y consentimiento (Fitsense-Real/Backend#80) | `cuentas/edad-consentimiento` | `7118a12` | B |
| E — HTTPS y Railway (Fitsense-Real/Backend#81) | `despliegue/https-railway` | `8cd84ff` (código) | `generation` |

**Comando:** el mismo del CI, sin conexión a internet.

```
mvn -B -ntp -o verify -Pintegration
```

PostgreSQL 16 efímero (Testcontainers), IA desactivada, sin Neon, sin Replicate y sin proveedor de correo.

## Resultado

| Fase | Ejecutadas | Fallos | Errores | Omitidas | Referencia `generation` (a1ccf53) |
|---|---|---|---|---|---|
| Unitarias (surefire) | 1.584 | 0 | 0 | 1 | 1.576 |
| Integración (failsafe) | 850 | 0 | 0 | 72 | 821 |
| **Total** | | | | | **BUILD SUCCESS**, 27 min 17 s |

**Omitidas.** Las 72 omitidas son las IT opcionales de siempre (campañas con Replicate real, lectura de Neon, catálogo congelado y P2-D), que solo se activan con sus propiedades. No se ha añadido ninguna.

**Diferencias con la referencia:**
- 8 unitarias nuevas: `TransportesDeCorreoTest` (6) y `MensajeDeRecuperacionTest` (2).
- 29 IT nuevas:

  | Prueba | IT |
  |---|---|
  | `EliminacionDeCuentaIT` | 7 |
  | `RecuperacionPorCorreoIT` | 4 |
  | `EliminacionWebIT` | 5 |
  | `ConsentimientoYEdadIT` | 5 |
  | `ConsentimientoExigidoIT` | 1 |
  | `MigracionCuentasV60aV63IT` | 1 |
  | `DespliegueHttpsIT` | 6 |

**Fuera del alcance:** no se ha modificado ninguna prueba de generación, adaptación, progresión ni catálogo. Tampoco se han tocado los validadores, V24, V8C, H2 ni P-03.

## Incidencia encontrada y corregida durante la regresión

**Lo que falló.** La primera ejecución combinada dio errores en 13 IT con el catálogo fijado (`spring.flyway.target` entre V35 y V50), entre ellas `DuracionForzadaIT`, `GeneracionReferenciaV35…V40IT` y `CatalogoV37IT`:

```
Schema validation: missing column [token_version] in table [users]
```

**La causa.** Esas pruebas aplican, sobre un catálogo antiguo, solo las migraciones de esquema que figuran en `EsquemaVigenteSobreCatalogoAnterior`. V61 añade `users.token_version` a la entidad `User` y no estaba en esa lista. La PR A no se vio afectada.

**La corrección.** V60 y V61 se añadieron a la lista en C, V62 en B y V63 en D. La cadena C → B → D se actualizó con merges, sin reescribir el historial.

**La comprobación.** Primero se pasaron `DuracionForzadaIT`, `GeneracionReferenciaV36IT` y `MigracionPropuesta1374IT`. Después se repitió la regresión completa, con el resultado de arriba.

## Orden de merge propuesto (no ejecutado)

1. **A** en `generation`. GitHub redirige entonces la base de C a `generation`.
2. **C**, después **B**, después **D**, cada una con su CI en verde tras la redirección.
3. **E**, que no depende de las anteriores. Sus documentos dan por hechas las cuatro.
4. Antes de desplegar, hacer una copia o una rama de la base de datos. V60–V63 solo añaden objetos, pero no tienen vuelta atrás en Flyway.

**No se ha fusionado ni desplegado nada.**
