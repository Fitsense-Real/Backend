# Orden de merge controlado (preparado, NO ejecutado)

**Preparar el merge no es ejecutarlo.** Nada de lo que sigue se ha hecho sobre `generation`. Todo se ensayó en la rama aislada `simulacion/merge-premerge-final` (en `origin`), partiendo del HEAD real de `generation` (`623c4fa`).

## 1. Orden exacto

Cada paso es una fusión `--no-ff` de la cabeza indicada sobre el resultado del paso anterior:

| Paso | PR | Cabeza | Resultado en la simulación |
|---|---|---|---|
| 1 | #62 HALL-PROG-01 | `1da115a` | Sin conflicto |
| 2 | #56 V47 + V48 | `d8ffe4c` | Sin conflicto |
| 3 | #57 V49 | `10b6ad6` | Sin conflicto |
| 4 | #58 V50 | `ea5ebef` | Sin conflicto |
| 5 | #59 controles | `2e7c55c` | Sin conflicto |
| 6 | #55 INC-CAT-C39 | `57075e7` | Sin conflicto |
| 7 | #60 V51 + V52 (+ HALL-PROG-01 equivalente) | `e34e892` | Sin conflicto; el parche equivalente no se duplica |
| 8 | #63 V52.1 | `4a728b4` | Sin conflicto |
| 9 | #65 HALL-GM-01 | `55129f4` | Sin conflicto |
| 10 | #61 V53 (apagada) | `81b5f41` | Sin conflicto |
| 11 | #64 V54 + reparación tolerante (apagada) | `7d7109a` | **Conflicto** en `EsquemaVigenteSobreCatalogoAnterior.SOLO_ESQUEMA` |
| 12 | #66 V53-B + V55 (apagada) | `fce4fdc` | **Conflicto** en el mismo punto |
| 13 | #67 final `cierre/catalogo-premerge-final` | `12c6897` (más los commits de documentos) | Sin conflicto |

**Resultado:** el árbol simulado (`7db872f`) es **idéntico** al de la candidata probada: `tree 475703a028ea316b116ae6fed3fab107d3c024bd` en los dos. Registro: `evidencia/simulacion_merge.txt`.

Los commits de documentos que la rama final añade después de `12c6897` no tocan `src` (`git diff 12c6897 HEAD -- src` vacío), así que no cambian lo probado.

## 2. Conflictos y su resolución

Los dos conflictos son en `src/test/java/main/web/services/fitsense/EsquemaVigenteSobreCatalogoAnterior.java` (lista `SOLO_ESQUEMA`). #64 y #66 añaden cada una una línea al final de la lista.

**Resolución:** conservar **todas** las líneas, en orden de versión:

```
"51:catalogo/esquema_V51_V53_sin_datos.sql",            // comentario de V51 y V53
"54:db/migration/V54__vencimiento_con_bloqueo_de_sistema.sql",
"55:db/migration/V55__perfil_permiso_apoyo_suspendido.sql");
```

El comentario que se conserva es el de V51 **y V53**, que trae #61. **No hay conflictos de código de producción** ni de migraciones Flyway.

## 3. Si `generation` cambia antes del merge

Esta simulación vale para `generation = 623c4fa`. Si `generation` avanza:

1. repetir la simulación sobre el HEAD nuevo;
2. comparar el árbol con el de la candidata;
3. si difiere, repetir la regresión sobre el resultado.

No hay que suponer que la fusión anterior sigue siendo aplicable.

## 4. Después del merge (cuando se autorice)

- **No desplegar** con P-03 por colocación encendida sin una autorización expresa: en producción queda apagada (`FITSENSE_P03_POR_COLOCACION` sin definir).
- Mantener apagadas V53 A, V53-B y #64.
- La PR de la propuesta V57 (#68, `propuesta/traza-omision-sistema`) solo se integra si se aprueba, después de #67.
