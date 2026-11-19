# Regresión final (integración local de las cinco PR)

Fecha: 19/11/2026.

**Árbol probado (tras la revisión de las cinco PR):** la rama local `integracion/google-play-local` (`1f08b5f`), que **no se ha subido**, combina:

| PR | Rama | Cabeza probada | Base de la PR |
|---|---|---|---|
| A — eliminación de cuenta (Fitsense-Real/Backend#77) | `cuentas/eliminar-cuenta` | `7258fa8` | `generation` (`a1ccf53`) |
| C — recuperación por correo (Fitsense-Real/Backend#78) | `cuentas/recuperacion-correo` | `144121c` | A |
| B — eliminación web (Fitsense-Real/Backend#79) | `cuentas/eliminacion-web` | `8e450db` | C |
| D — edad y consentimiento (Fitsense-Real/Backend#80) | `cuentas/edad-consentimiento` | `0eb86ff` | B |
| E — HTTPS y Railway (Fitsense-Real/Backend#81) | `despliegue/https-railway` | `1f75b43` (código sin cambios desde `8cd84ff`) | `generation` |

**Comando:** el mismo del CI, sin conexión a internet.

```
mvn -B -ntp -o verify -Pintegration
```

PostgreSQL 16 efímero (Testcontainers), IA desactivada, sin Neon, sin Replicate y sin proveedor de correo.

## Resultado

| Fase | Ejecutadas | Fallos | Errores | Omitidas | Referencia `generation` (a1ccf53) |
|---|---|---|---|---|---|
| Unitarias (surefire) | 1.586 | 0 | 0 | 1 | 1.576 |
| Integración (failsafe) | 851 | 0 | 0 | 72 | 821 |
| **Total** | | | | | **BUILD SUCCESS**, 25 min 29 s |

**Omitidas.** Las 72 omitidas son las IT opcionales de siempre (campañas con Replicate real, lectura de Neon, catálogo congelado y P2-D), que solo se activan con sus propiedades. No se ha añadido ninguna.

**Diferencias con la referencia:**
- 10 unitarias nuevas: `TransportesDeCorreoTest` (6), `MensajeDeRecuperacionTest` (2) y `LimiteDeReautenticacionTest` (2).
- 30 IT nuevas:

  | Prueba | IT |
  |---|---|
  | `EliminacionDeCuentaIT` | 7 |
  | `RecuperacionPorCorreoIT` | 4 |
  | `EliminacionWebIT` | 6 |
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

## Revisión de las cinco PR (19/11/2026)

Una revisión independiente del código encontró los siguientes defectos. **Ninguno era de severidad alta.**

| PR | Hallazgo | Severidad | Corrección | Prueba |
|---|---|---|---|---|
| A | El límite de reautenticación se podía saltar con peticiones en paralelo: todas se comprobaban antes de anotar el primer fallo | Media | El intento se reserva de forma atómica antes de comprobar la contraseña | `LimiteDeReautenticacionTest` (50 hilos, solo 5 pasan) |
| A/C | Al guardar `User`, JPA reescribía la fila entera. Un inicio de sesión en curso podía devolver `status=ACTIVE` durante una eliminación, o restaurar el hash y `token_version` tras un cambio de contraseña | Baja | `@DynamicUpdate` en `User`: solo se escriben las columnas que cambian | Regresión completa |
| B | Si fallaba el registro de la eliminación (503), el token ya estaba gastado, aunque la respuesta pedía reintentar | Media | Ante ese fallo se restauran los tokens del intento | `EliminacionWebIT.registroFallidoDevuelveElEnlace` |
| C | SMTP sin tiempos máximos: un servidor que no responde bloqueaba el hilo de envío | Baja | `spring.mail.properties.mail.smtp.*timeout=10000` | — |
| C | `LimitePorIp` podía crecer sin límite si se rotaban muchas IP | Baja | Por encima de 50.000 claves, se vacía tras purgar | — |
| C/B | **Dependencia de despliegue.** Sin `server.forward-headers-strategy` (PR E), el límite por IP vería la IP del proxy y sería global | Media | No se cambia código: **C y B no se despliegan sin E** (ver el orden de merge) | `DespliegueHttpsIT.ipYEsquemaDelProxy` |
| D | Ningún defecto. Dos aceptaciones simultáneas de la misma versión: la segunda recibe 409 en vez de una respuesta idempotente | — | Sin cambios | — |
| E | Ningún defecto. Queda un riesgo por verificar en el primer despliegue: que el proxy de Railway esté en los rangos de confianza | — | Documentado en PREPARACION_HTTPS_RAILWAY §6 | — |

**Logs.** Por instrucción expresa, ninguna corrección toca los logs existentes de generación, adaptación ni de las respuestas de la IA. S-1 sigue pendiente y bloquea la producción (PRUEBAS_SEGURIDAD).

## Orden de merge propuesto (no ejecutado)

1. **A** en `generation`. GitHub redirige entonces la base de C a `generation`.
2. **C**, después **B**, después **D**, cada una con su CI en verde tras la redirección.
3. **E**, que no depende de las anteriores. Sus documentos dan por hechas las cuatro.
   - **No se despliega ninguna combinación con C o B sin E.** Detrás del proxy, los límites por IP necesitan `forward-headers-strategy`.
4. Antes de desplegar, hacer una copia o una rama de la base de datos. V60–V63 solo añaden objetos, pero no tienen vuelta atrás en Flyway.

**No se ha fusionado ni desplegado nada.**
