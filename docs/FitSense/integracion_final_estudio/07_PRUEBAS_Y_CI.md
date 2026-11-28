# 07 · Pruebas y CI

Todas las pruebas usan PostgreSQL 16 de **Testcontainers** y usuarios **sintéticos**. La IA está apagada (motor de reglas) o **simulada** (servidor local), y el correo también es **simulado** (GreenMail o un servidor HTTP local). **Ninguna llamada a Replicate real y ninguna credencial de producción.**

## 1. Pruebas de referencia antes de cambiar nada (§2.10)

Sobre `generation` en `41c498ab`, las suites que esta orden iba a tocar:
- consentimientos (`ConsentimientoExigidoIT`, `ConsentimientoYEdadIT`, `DocumentosLegalesTest`);
- participación (`ParticipacionOpcionA/BIT`);
- recuperación (`RecuperacionContrasenaIT`, `RecuperacionPorCorreoIT` y las unitarias de correo);
- cierre semanal (`CierreSemanalRecuperableIT`, `CicloConcurrenteIT`, `CicloCompletoHttpIT`);
- exportación, roles, contrato de errores, adherencia y logs.

**53 unitarias y 103 de integración ejecutadas, 0 fallos.**

## 2. Por PR

| PR | Pruebas nuevas o cambiadas | Ejecutadas en local | Resultado | CI de la cabeza |
|---|---|---|---|---|
| [#96](https://github.com/Fitsense-Real/Backend/pull/96) C1 | `OpcionAExclusivaIT` (11, nueva); `ConsentimientoExigidoIT` (códigos, cambio de versión, elegibilidad por consentimiento válido, solicitud repetida; `sinParticipar` **invertida** a propósito) | 30 unitarias + 101 IT; después, 19 IT tras quitar el id interno del mensaje | 0 fallos. Dos pruebas nuevas fallaron al principio porque omitían `acceptsStudyParticipation`: el campo es obligatorio, y se documentó como contrato | `035e961`: **verde** |
| [#97](https://github.com/Fitsense-Real/Backend/pull/97) C2 | `MensajeDeRecuperacionTest` (actualizada y ampliada); `RecuperacionPorCorreoIT` (asunto y extracción del código) | 11 unitarias + 14 IT | 0 fallos | `b48d336`: **verde** |
| [#98](https://github.com/Fitsense-Real/Backend/pull/98) C3 | `CierresProlongadosIT` (6, nueva) | 45 IT | 0 fallos | `5e0c789`: **verde** |
| [#99](https://github.com/Fitsense-Real/Backend/pull/99) C4 (borrador) | `HistorialDeRecalculosIT` (3), `MigracionV66aV67IT` (1), `CamposCambiadosTest` (1) | 1 unitaria + 38 IT; después, 52 IT | 0 fallos tras dos correcciones (§3) | `4eabd62`: ver §4 |

## 3. Regresión completa combinada (`mvn -B clean verify -Pintegration`)

Rama local temporal `tmp/integracion-estudio` (`d24d893e`), sin subir: `generation` `41c498ab` + C1 `035e961` + C2 `b48d336` + C3 `5e0c789` + C4 `4eabd62`. Las cuatro se fusionan **sin conflictos**.

| Fase | Ejecutadas | Aprobadas | Fallos | Errores | Omitidas |
|---|---|---|---|---|---|
| Unitarias (surefire) | **1.682** | 1.681 | 0 | 0 | 1 |
| Integración (failsafe) | **966** | 894 | 0 | 0 | 72 |
| **Total** | **2.648** | **2.575** | **0** | **0** | **73** |

**BUILD SUCCESS**, 30:20 min.

**Frente a la referencia posterior al cierre** (1.680 unitarias, 943 de integración, 73 omitidas): +2 unitarias (`CamposCambiadosTest` y `MensajeDeRecuperacionTest.sinDatosDeLaCuenta`) y +23 de integración (11 + 2 + 6 + 3 + 1). Las mismas 73 omitidas: son laboratorios, barridos y corridas de evidencia con **IA real**, apagados salvo que se active su propiedad del sistema, y no se activó ninguna.

**Una primera regresión combinada falló** (C4 con su primera cabeza `da7df42`): 2 errores en `RetiradaLocalizadaLimiteIT` y `RetiradaLocalizadaPreferenciaIT`. Esas pruebas fijan un catálogo anterior (`spring.flyway.target=50`) y aplican después las migraciones de solo esquema de `EsquemaVigenteSobreCatalogoAnterior`. Hubo dos problemas:
1. V67 no estaba en esa lista;
2. el ayudante cortaba los scripts en `;`, lo que rompe la función plpgsql de V67 (`$$`).

Se corrigió en C4 (`4eabd62`): V67 entra en la lista y un script con `$$` se ejecuta entero. Las 21 clases que usan el ayudante pasan (49 pruebas). Después se repitió **la regresión completa**, que es la de la tabla.

### 3.1 Regresiones que pedía la orden

| Área | Suites (todas en la regresión completa) |
|---|---|
| Autenticación | `LimitesDeAutenticacionIT`, `SeguridadRolesIT`, `ProxyDeConfianza*IT`, `ContratoErroresApiIT` |
| Consentimientos | `ConsentimientoExigidoIT`, `ConsentimientoYEdadIT`, `DocumentosLegalesTest` |
| Participación exclusiva | `OpcionAExclusivaIT`, `ParticipacionOpcionAIT`, `ParticipacionOpcionBIT` (la B solo en pruebas) |
| Generación inicial | `GeneracionAsincronaResilienciaIT`, `CicloCompletoHttpIT` y las suites de V58 |
| Adaptación | `PublishAdaptedTest`, `PublicacionConcurrenteIT`, adaptación y regeneración segura |
| O20/V65 | Suites de exclusiones por dolor e identificación posterior, sin cambios |
| V58/V59/V66 | `OperacionesIaV66IT`, `MigracionV65aV66IT`, `ReanudacionConReplicateSimuladoIT`, `DiarioDePrediccionesReplicateTest` |
| Cierre semanal | `CierreSemanalRecuperableIT`, `CicloConcurrenteIT`, `CierresProlongadosIT` |
| Recuperación de contraseña | `RecuperacionContrasenaIT`, `RecuperacionPorCorreoIT`, `MensajeDeRecuperacionTest` |
| Seguridad de logs | `LogsSinDatosSensiblesIT` |
| Simulación de 12 semanas | `Estudio12SemanasIT` (18 escenarios) |
| Persistencia | `MigracionV66aV67IT`, `EliminacionDeCuentaIT`, `HistorialDeRecalculosIT` |
| Concurrencia | `OpcionAExclusivaIT.retiradaSimultaneaEspera` (bloqueo `FOR SHARE`), `CicloConcurrenteIT`, `PublicacionConcurrenteIT` |
| Contratos con Mobile | `ContratoErroresApiIT`, `ContratoPushHttpIT` y los ejemplos HTTP de `02` §4, obtenidos ejecutando el backend |

## 4. CI

El CI (`.github/workflows/ci.yml`, job `verify`) se ejecuta en cada PR sobre la combinación con `generation`.

| PR | Cabeza | CI |
|---|---|---|
| #96 C1 | `035e961` | verde. La cabeza anterior `ec716f9` quedó cancelada por la nueva subida, como está configurado |
| #97 C2 | `b48d336` | verde |
| #98 C3 | `5e0c789` | verde |
| #99 C4 | `4eabd62` | en curso al escribir esto; la cabeza anterior `da7df42` falló por el error de §3 |
| #100 C5 | la de este documento | solo documentación |

El estado final del CI se da en el informe de cierre de la orden.

## 5. Ensayos operativos, sin pruebas de Maven

- **Copia, restauración y V66 sobre una copia sintética, con fallo y recuperación:** `05` §2 y `evidencia/ensayo_v66_copia_sintetica.txt`.
- **Ejemplos HTTP de Mobile** obtenidos ejecutando el backend de C1 con documentos sintéticos (`02` §4). Las clases temporales de captura no se subieron.

## 6. Lo que estas pruebas NO demuestran

- **IA real:** no se ha usado. Toda generación es del motor de reglas o de un Replicate simulado.
- **Entrega de correo real:** no hay proveedor (`03` §6).
- **Un despliegue en Railway**, una base persistente ni un dispositivo real con push.
- **Pruebas físicas con personas** y validación operativa del estudio.
