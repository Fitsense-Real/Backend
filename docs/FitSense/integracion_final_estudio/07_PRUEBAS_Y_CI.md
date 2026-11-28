# 07 · Pruebas y CI

Todas las pruebas usan PostgreSQL 16 de **Testcontainers** y usuarios **sintéticos**:
- **IA:** apagada (motor de reglas) o **simulada** con un servidor local;
- **correo:** **simulado** (GreenMail o un servidor HTTP local).

**Ninguna llamada a Replicate real y ninguna credencial de producción.**

**Actualizado el 28/11/2026 con la integración final:** #96, #97 y #98 fusionadas, **sin #99**.

## 1. Pruebas de referencia

| Momento | Base | Ejecutadas | Fallos |
|---|---|---|---|
| Antes de C1–C5 | `generation` `41c498ab` | 53 unitarias + 103 de integración (consentimientos, participación, recuperación, cierre semanal, exportación, roles, errores, adherencia, logs) | 0 |
| Antes de la revisión final | `generation` `41c498ab` | 22 unitarias + 85 de integración (las mismas áreas y la publicación concurrente). El primer intento dio 13 errores **de entorno**: el demonio de Docker se había caído. Se reinició y se repitió | 0 |

## 2. Por PR (con las correcciones de la revisión final)

| PR | Pruebas nuevas o cambiadas | Ejecutadas en local | Fallos | CI de la cabeza |
|---|---|---|---|---|
| #96 C1 (`401ca56`) | `OpcionAExclusivaIT`: 17, de las que 6 se añadieron en la revisión. `ConsentimientoExigidoIT`: códigos, cambio de versión, elegibilidad, solicitud repetida, petición directa con campos falsificados; `sinParticipar` **invertida** a propósito | 15 unitarias + 69 de integración | 0 | verde |
| #97 C2 (`b48d336`) | `MensajeDeRecuperacionTest`, `RecuperacionPorCorreoIT` | 11 + 14 | 0 | verde |
| #98 C3 (`c70b0f1`) | `CierresProlongadosIT`: 10, de las que 4 se añadieron en la revisión, más el defecto `SIN_PLAN_NUEVO` corregido | 24 de integración | 0 | verde |
| #100 C5 | Solo documentación | — | — | verde |
| #99 C4 | **Excluida.** Borrador; no se fusiona | — | — | (verde, no se usa) |

## 3. Regresión combinada antes de los merges (`mvn -B clean verify -Pintegration`)

Rama local `tmp/integracion-final` (`dcc82076`), sin subir: `generation` `41c498ab` + #96 `401ca56` + #97 `b48d336` + #98 `c70b0f1` + #100 `b55dd71`.
- **#99 excluida por completo:** ni V67, ni su tabla, ni su disparador, ni su código. Comprobado en la rama: 0 ficheros V67 y 0 commits de C4.
- **El código de `generation` tras los tres merges (`cca9fc21`) es idéntico al de esta rama.** El único diff son los documentos de #100.

| Fase | Ejecutadas | Aprobadas | Fallos | Errores | Omitidas |
|---|---|---|---|---|---|
| Unitarias | **1.681** | 1.680 | 0 | 0 | 1 |
| Integración | **973** | 901 | 0 | 0 | 72 |
| **Total** | **2.654** | **2.581** | **0** | **0** | **73** |

**BUILD SUCCESS**, 28:41 min.

**Diferencias explicadas:**

| Frente a | Unitarias | Integración | Por qué |
|---|---|---|---|
| Referencia del backend integrado (1.680 + 943, 73 omitidas) | +1 | +30 | **Unitaria:** `MensajeDeRecuperacionTest.sinDatosDeLaCuenta`. **Integración:** `OpcionAExclusivaIT` (17), `ConsentimientoExigidoIT` (+3) y `CierresProlongadosIT` (10) |
| Informe anterior con #99 (1.682 + 966) | −1 | +7 | **Sin #99:** −1 unitaria (`CamposCambiadosTest`) y −4 de integración (`HistorialDeRecalculosIT` ×3 y `MigracionV66aV67IT`). **Revisión final:** +11 de integración (+6 en `OpcionAExclusivaIT`, +1 en `ConsentimientoExigidoIT` y +4 en `CierresProlongadosIT`) |

- **Omitidas:** las mismas 73: laboratorios y corridas con **IA real**, apagados por propiedad del sistema.
- **Retiradas:** ninguna.
- **Invertida a propósito:** `ConsentimientoExigidoIT.sinParticipar`.

### 3.1 Simulación de 12 semanas y prueba inversa (sobre la misma combinación)

| Ejecución | Resultado |
|---|---|
| `Estudio12SemanasIT` (E12-01..E12-18, Replicate y Firebase simulados) | **18 escenarios aprobados**. CSV idéntico, columna a columna, a `cierre_preproduccion/evidencia/estudio_12_semanas.csv` |
| Prueba inversa (ventana de recuperación a 0, cambio temporal deshecho) | **Falla**, con exactamente los fallos de `evidencia/prueba_inversa_sin_recuperacion.txt` |

### 3.2 Áreas críticas (todas en la regresión)

| Área | Suites |
|---|---|
| Participación A | `OpcionAExclusivaIT`, `ParticipacionOpcionAIT`, `ParticipacionOpcionBIT` (la B solo en pruebas) |
| Registro y versiones | `ConsentimientoExigidoIT`, `ConsentimientoYEdadIT`, `DocumentosLegalesTest` |
| Retirada concurrente | `OpcionAExclusivaIT.retiradaSimultaneaEspera` y `dosPublicacionesYRetiradaConcurrentes` |
| Publicación asíncrona | `GeneracionAsincronaResilienciaIT`, `OpcionAExclusivaIT.pendienteNoPublicaTrasRetirarse` |
| O20/V65 | Suites de exclusión por dolor e identificación posterior (sin cambios) |
| V58/V59/V66 | `OperacionesIaV66IT`, `MigracionV65aV66IT`, `ReanudacionConReplicateSimuladoIT`, `DiarioDePrediccionesReplicateTest` |
| Cierre semanal y recuperación de semanas | `CierreSemanalRecuperableIT`, `CicloConcurrenteIT`, `CierresProlongadosIT` |
| Autenticación | `LimitesDeAutenticacionIT`, `ProxyDeConfianza*IT`, `DespliegueHttpsIT` |
| Recuperación de contraseña | `RecuperacionContrasenaIT`, `RecuperacionPorCorreoIT`, `MensajeDeRecuperacionTest` |
| Adherencia | `AdherenciaIT`, `AdherenciaRiesgoIT` |
| Simulación de 12 semanas | `Estudio12SemanasIT` |
| Seguridad de logs | `LogsSinDatosSensiblesIT` |
| Administración y roles | `SeguridadRolesIT`, `OperacionYExportacionIT`, `CierresProlongadosIT.endpoint` |

## 4. Merges y CI

Cada merge se hizo con el SHA de la cabeza revisada (`sha=`) y en este orden. Tras cada uno, pruebas afectadas sobre el commit de merge y CI de `generation`.

| Orden | PR | Commit de merge | Árbol = previsto | Pruebas tras el merge | CI de `generation` |
|---|---|---|---|---|---|
| 1 | #96 | `434a9dc05b3479be88eabba9e23658d2ecb38052` | sí | 15 unitarias + 90 de integración, 0 fallos | verde |
| 2 | #97 | `09a92028cdc756c1eb2f2b815744e75772c186bc` | sí | 11 + 21, 0 fallos | verde |
| 3 | #98 | `cca9fc2162f5a7e00f82e36a64a8e7a4c48ab03d` | sí | 63 de integración (con `Estudio12SemanasIT` y `AdherenciaIT`), 0 fallos | ver informe final |
| 4 | #100 | el que contiene este documento | — | — | ver informe final |

`main` no cambió en ningún momento (`7b8a6d34`). Un documento no puede contener el SHA del merge que lo incorpora: el SHA final de `generation`, su CI y la regresión completa posterior están en el informe de cierre de la orden.

## 5. Lo que estas pruebas NO demuestran

- **IA real:** no se ha usado.
- **Entrega de correo real:** no hay proveedor (`03`).
- **Despliegue**, base persistente y dispositivo real con push.
- **Pruebas físicas con personas** y validación operativa del estudio.
