# Informe del merge controlado del catálogo v1.1

## Problema

Había que integrar en `generation` `FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1.1`, formado por:

- la pila #55–#66;
- la PR final #67, con la candidata probada `12c6897`.

La integración tenía que cumplir cuatro condiciones:

- no duplicar la corrección de HALL-PROG-01, que traen #60 y #62;
- resolver el conflicto conocido de `SOLO_ESQUEMA` (V54/V55);
- no cambiar nada respecto de lo probado;
- no desplegar nada.

## Comprobaciones previas (fase 1)

| Comprobación | Resultado |
|---|---|
| HEAD de `generation` | `623c4fa`, el mismo de la simulación. No se movió, así que no hizo falta reconstruirla |
| HEAD de #55–#68 | Las doce cabezas coinciden con las registradas en `ESTADO_PR_55_66.md` y están contenidas en la candidata |
| CI de #67 y #68 | Verde |
| Comentarios abiertos | Solo los históricos de #60, #63 y #64 sobre fallos ya resueltos. Ninguno pide cambios |
| Duplicación #60 / #62 | #62 aporta el parche de HALL-PROG-01 y #60 lo trae como antecesor. Git aplica una sola vez el mismo commit, porque está en la historia de las dos |
| Interruptores | Los que figuran en la versión congelada v1.1. Ver `ESTADO_INTERRUPT0RES.md` |
| Despliegue automático | No consta. Evidencia más abajo |

### La diferencia 1540 / 1539 y la de las omitidas

No falta ninguna ejecución. La diferencia viene del recuento de los informes, no de Maven.

| Fuente | Unitarias | Integración |
|---|---|---|
| **Resumen de Maven** (registro de la regresión de `12c6897`) | **1540**, 1 omitida | **754**, 57 omitidas |
| Suma de las líneas `Tests run … -- in` del registro | 1540, 1 omitida (177 líneas) | 754, 57 omitidas (192 líneas) |
| Suma del atributo `tests` de los XML de `target/*-reports` | 1539, 1 omitida | 749, 54 omitidas |

**Unitarias, 1540 frente a 1539.**

- `TEST-…RedisenoAdaptacionTest.xml` declara `tests="17"`, pero contiene **18** `<testcase>`.
- La clase tiene tres `@Nested` y dos de ellos tienen un método con el mismo nombre (`historicaRechaza` en P-A1 y en P-A2). El generador del informe agrupa los anidados en un solo fichero y el atributo descuenta el repetido.
- Contando los `<testcase>` salen 1540, igual que Maven.

**Integración, 754/57 frente a 749/54.**

- Tras la regresión se ejecutaron a propósito tres pruebas opt-in sobre el mismo `target`:
  - `CatalogoIntegralComparacionIT`;
  - `CatalogoCongeladoIT`;
  - `RegeneracionPorColocacionRev2IT`, con la propiedad que habilita su caso opt-in.
- Esas ejecuciones reescribieron sus XML (hora de modificación 04:50–04:52, posterior al final de la regresión a las 04:48).
- En la regresión, `RegeneracionPorColocacionRev2IT` contaba 6 pruebas con 1 omitida; su XML reescrito cuenta 1 prueba con 0 omitidas. Eso explica 5 pruebas y 1 omitida.
- Las otras dos contaban 1 prueba omitida cada una; sus XML reescritos cuentan 1 ejecutada. Eso explica 2 omitidas.
- Total: 754 − 5 = 749 y 57 − 3 = 54.

**Las omitidas** son siempre las mismas:

- 1 unitaria;
- 57 de integración opt-in: IA real, Neon, rejillas largas, volcados congelados y mediciones V53-B. Se activan con propiedades explícitas.
- No se cuentan como aprobadas.

**Conclusión:** no revela ninguna ejecución incompleta. Se continuó.

### No hay despliegue automático

| Evidencia | Qué muestra |
|---|---|
| `.github/workflows/` | Un solo workflow, `ci.yml`. Solo ejecuta `mvn -B -ntp verify -Pintegration` en PR y en push a `main` y `generation`. `permissions: contents: read`, sin secretos y sin ningún paso de despliegue |
| Ejecuciones de Actions tras el merge | Solo «CI» (336 y 337 para `50849d0`). Ninguna de despliegue |
| Repositorio | No hay `railway.json`, `nixpacks.toml`, `Procfile` ni `Dockerfile`. Solo `docker-compose.local.yml`, para uso local |
| Railway | «Railway no despliega desde `generation`», según confirmó el responsable (`CATALOGO_V37.md`, `INFORME_V40.md`, `INTEGRACION_FINAL_Y_CONGELAMIENTO.md`). `main` sigue en `7b8a6d3` (21/09/2026), sin tocar |
| Secretos y configuración de producción | Sin cambios. Ninguna PR integrada modifica secretos ni variables de entorno de despliegue. Los valores por defecto de los interruptores son los aprobados |

**Límite de lo verificado:** desde esta sesión no se puede ver el panel de Railway. La garantía de que no despliega desde `generation` es la confirmación documentada del responsable.

## Estrategia (fase 3)

Antes de la primera fusión, la simulación se reconstruyó sobre el `generation` real (`623c4fa`) con el mismo orden. Se registró el árbol esperado después de cada paso (`arboles_esperados.txt`).

| # | PR | Cabeza | Commit en `generation` | Árbol tras el merge | ¿Igual que la simulación? |
|---|---|---|---|---|---|
| 1 | #62 HALL-PROG-01 | `1da115a` | `17a747a` | `02e8047` | Igual |
| 2 | #56 V47 y V48 | `d8ffe4c` | `5ad814c` | `e948799` | Igual |
| 3 | #57 V49 | `10b6ad6` | `f3075d8` | `d43ff87` | Igual |
| 4 | #58 V50 | `ea5ebef` | `41e61bc` | `db0f189` | Igual |
| 5 | #59 controles permanentes | `2e7c55c` | `9936a84` | `919430c` | Igual |
| 6 | #55 contención INC-CAT-C39 | `57075e7` | `29ed676` | `1667ff1` | Igual |
| 7 | #60 V51 y V52 | `e34e892` | `dc4f0e1` | `6537618` | Igual |
| 8 | #63 V52.1 | `4a728b4` | `ce7c892` | `a68addd` | Igual |
| 9 | #65 HALL-GM-01 | `55129f4` | `893cc2f` | `9ee031b` | Igual |
| 10 | #61 V53 (opción A apagada) | `81b5f41` | `a824420` | `8e81495` | Igual |
| 11 | #67 candidata final, que contiene #64 y #66 | `79604b0` | **`50849d0`** | `96dc37e` | Igual |

### Estrategia equivalente para #64 y #66

La orden permitía una estrategia equivalente si conservaba exactamente el resultado aprobado.

- **Qué se hizo:** #64 y #66 no se fusionaron por separado. Sus cabezas (`7d7109a` y `fce4fdc`) ya están dentro de la candidata `12c6897` / #67, con la resolución de `SOLO_ESQUEMA` probada: las líneas V51/V53, V54 y V55 se conservan.
- **Resultado:** al fusionar #67, GitHub marcó #64 y #66 como fusionadas (14:26:30 UTC), porque sus cabezas ya están en `generation`.
- **Qué se evitó:** escribir de nuevo, a mano, la resolución del conflicto de V54/V55.

### Merge del #67

- GitHub rechazó la fusión del #67 con el error 405 «merge conflicts». La pila deja cuatro bases de merge cruzadas (`4a728b4`, `55129f4`, `81b5f41`, `1da115a`), y GitHub no fusiona en ese caso.
- En local, `git merge` resolvía sin conflictos.
- Se fusionó `generation` (`a824420`) en la rama de #67 con un merge commit (`79604b0`), sin reescribir historia.
- El árbol de `79604b0` es **idéntico** al de la candidata probada `af7e633`, así que ese merge no cambia ningún archivo.
- Con el CI de `79604b0` en verde (14:25:55 UTC), se fusionó #67 con `merge_method=merge` y `expectedHeadSha`.

### Identidad final

| Comprobación | Resultado |
|---|---|
| `git diff 12c6897 50849d0 -- src pom.xml .github` | Vacío |
| Árbol de `src` | `5599aacf…` en los dos commits |
| Árbol completo de `50849d0` | `96dc37e…`, el esperado por la simulación |

### CI intermedio

Las once fusiones se hicieron seguidas, entre las 13:57 y las 14:26 UTC. El CI de cada push a `generation` cancela el anterior (`concurrency: cancel-in-progress`), así que no hubo un CI verde por cada paso intermedio. La garantía de cada paso es otra:

- su árbol coincide con el de la simulación;
- el árbol final es el de la candidata, que tiene la regresión completa en verde.

El CI terminó en verde en `a824420` (paso 10, run 333). En `50849d0` corre la regresión posterior al merge (`REGRESION_POST_MERGE.md`).

## Resultado

- `generation` = **`50849d0f4b5c979cd980bdaceea1dfd7dd550ebc`**.
- Contiene las 13 PR del catálogo, sin duplicados y sin reescribir historia.
- Ninguna fusión forzada. Nada desplegado.
- Después, tras la regresión posterior al merge en verde, se fusionó #68 (V57): `generation` = **`0d112785a5df70d686da36c80e814b40ca077cd6`** (`INTEGRACION_V57.md`).

## Pendientes

Ver `REGRESION_POST_MERGE.md`, `INTEGRACION_V57.md` y `RIESGOS_RESIDUALES.md`.
