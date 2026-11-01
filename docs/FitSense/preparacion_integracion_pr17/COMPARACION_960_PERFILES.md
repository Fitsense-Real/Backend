# Comparación de las matrices de 960 perfiles, perfil a perfil

**Objetivo:** demostrar que la configuración histórica no tiene ningún cambio funcional y que las políticas candidatas no han cambiado desde la referencia anterior. Hasta ahora solo se habían comparado los recuentos.

## 1. Método

**Laboratorio:** `ComparacionPerfilesIT` (opt-in, `-Dfitsense.comparacion.perfiles=true`, PostgreSQL efímero, IA apagada, 0 llamadas a Replicate).

| Aspecto | Detalle |
|---|---|
| Perfiles | Los mismos 960 de las matrices anteriores |
| Usuarios | Mismo orden de creación (mismos `user_id`) |
| Semillas | Las mismas |
| Dolor | El grupo DOLOR_P02 excluye los 6 ejercicios más usados, como siempre |

**Para cada perfil y política** el laboratorio genera la semana con el generador de reglas, la valida con el validador real y vuelca dos archivos:

| Archivo | Contenido |
|---|---|
| `planes.csv` | Válido o inválido, reglas que rechazan (motivo), estado y motivo de C2-D, ejercicios excluidos por dolor, número de sesiones y segundos totales |
| `colocaciones.csv` | Una fila por ejercicio: fecha, enfoque, segundos estimados de la sesión, orden, ejercicio, patrón, clase de capacidad, tipo, series, repeticiones, segundos y descanso |

**Versiones del código ejecutadas** (tres *worktrees* aislados, cada uno con su base efímera):

| Versión | Commit | Políticas |
|---|---|---|
| `generation` (estable) | afef98a | PLAN-HIST-1 (única que existe) |
| Referencia anterior del PR #17 | 661c4aa | PLAN-HIST-1, PLAN-C2D-0.1, PLAN-C2D-V4-0.1, PLAN-C2D-EF-0.1, PLAN-C2D-V4-EF-0.1 |
| PR #17 actual | 254a18e | Las mismas 5 |
| Rama aislada V4-C | `feature/v4c-versionado-aislado` | PLAN-HIST-1 y PLAN-V4-0.1 |

**Campos comparados**, 13 por perfil y política:
1. resultado (válido/inválido);
2. reglas (motivo del rechazo);
3. estado C2-D;
4. motivo C2-D;
5. excluidos por dolor;
6. número de sesiones;
7. segundos totales;
8. composición semanal (fecha → enfoque);
9. duración estimada por sesión;
10. ejercicios (fecha, orden, id);
11. patrón y capacidad de cada ejercicio;
12. series, repeticiones y segundos;
13. descanso.

El dato funcional persistido del ciclo real (planes publicados) se compara aparte (§4).

## 2. Resultados

| Comparación | Combinaciones perfil × política | Con alguna diferencia |
|---|---|---|
| `generation` frente al PR #17 actual (PLAN-HIST-1) | 960 | **0** |
| `generation` frente a 661c4aa (PLAN-HIST-1) | 960 | **0** |
| 661c4aa frente al PR #17 actual (5 políticas) | 4.800 | **0** |
| `generation` frente a la rama aislada (PLAN-HIST-1) | 960 | **0** |

**Además, byte a byte:**
- `planes.csv` y `colocaciones.csv` de 661c4aa y de 254a18e son **idénticos** (mismo SHA-256).
- Las 960 líneas de PLAN-HIST-1 del PR #17 son idénticas, línea a línea, al volcado de `generation`.

**Recuentos** (iguales en todas las versiones donde la política existe):

| Política | Válidos | Inválidos |
|---|---|---|
| PLAN-HIST-1 | 875 | 85 |
| PLAN-C2D-0.1 | 951 | 9 |
| PLAN-C2D-V4-0.1 | 951 | 9 |
| PLAN-C2D-EF-0.1 | 902 | 58 |
| PLAN-C2D-V4-EF-0.1 | 902 | 58 |
| PLAN-V4-0.1 (rama aislada) | 875 | 85 |

**Sin diferencias nuevas, no hay nada que clasificar:**
- 0 diferencias nuevas;
- 0 regresiones;
- 0 diferencias sin explicar.

Por eso `DIFERENCIAS_MATRICES.csv` tiene sus 6.720 filas (960 + 4.800 + 960) en `SIN_CAMBIO`. Las columnas de valor anterior, valor actual, causa y clasificación están preparadas para cuando aparezca una diferencia.

## 3. Control positivo (el comparador detecta diferencias)

Para descartar una comparación vacía, se comparó dentro del PR #17 PLAN-HIST-1 frente a PLAN-C2D-0.1:

| Resultado | Detalle |
|---|---|
| Perfiles distintos | **79** |
| Cambian de inválido a válido | 76 |
| Cambian solo el estado y el motivo de C2-D | 3 |

**Lo que cambia es solo la publicación:**
- 0 diferencias en ejercicios, dosis, descansos, composición o duración;
- es decir, C2-D decide si se publica, pero no modifica el contenido.

**Lo que no cambia con V4-C:**
- PLAN-C2D-0.1 frente a PLAN-C2D-V4-0.1: 0 perfiles distintos;
- PLAN-HIST-1 frente a PLAN-V4-0.1 en la rama aislada: 0 perfiles distintos.

V4-C no cambia ningún plan del generador de reglas, porque ya caben en el 115 %. Su efecto está en las propuestas de la IA que caen entre el 115 % y el redondeo al minuto de la histórica (`SEPARACION_V4C.md` §1 y `PRUEBAS_V4C_AISLADO.md`).

## 4. Datos funcionales persistidos (ciclo real)

**Planes publicados en el ciclo real de 10 semanas** (`PLANES_CICLO_*.csv`, una fila por ejercicio publicado), comparando la orden anterior (`correccion_elegibilidad_funcional/`) con la actual (`cierre_final_pr17/`):

| Política | Filas | Diferencias |
|---|---|---|
| PLAN-HIST-1 | 542 | **0** |
| PLAN-C2D-0.1 | 1.969 | **0** |
| PLAN-C2D-EF-0.1 | 1.049 | **0** |
| PLAN-C2D-V4-EF-0.1 | 1.049 | **0** |

**El código no ha cambiado desde esos volcados:**
- desde 583b723 (cuando se volcaron) hasta 254a18e no cambia ningún archivo de `src/main`;
- los únicos cambios de esta orden en `src/main` son el renombrado `DURACION_POR_CONTENIDO`, que es de presentación y está apagado.

## 5. Reproducción

```
# en cada worktree (generation, 661c4aa, PR #17 y rama aislada)
mvn -o verify -Pintegration -Dit.test=ComparacionPerfilesIT -Dtest=NoSuchTest \
  -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false \
  -Dfitsense.comparacion.perfiles=true -Dfitsense.comparacion.salida=<directorio>
```

**Notas:**
- En `generation` el laboratorio se ejecutó con su variante de PLAN-HIST-1, sin `conPolitica` (esa rama no tiene versionado). En la rama aislada, con PLAN-HIST-1 y PLAN-V4-0.1.
- Los volcados completos (9,9 MB por versión) no se versionan.

**Huellas SHA-256:**

| Archivo | SHA-256 |
|---|---|
| PR #17 y 661c4aa · `planes.csv` | `c582fce21cac5bd731226871015ac222096f6637e6babc244ec349bd69574ee2` |
| PR #17 y 661c4aa · `colocaciones.csv` | `ee9d21118e65679dd68f2886c40e2c35f7206da5407a47792e6651bb2ccaa3d9` |
| `generation` · `planes.csv` | `761d67cfba8f01b66e571ea6ea25890b12d0043fb718ad4acd4f60043e348a8d` |
| `generation` · `colocaciones.csv` | `fa9fa3edc200ae123a69427cbe35f0e03bc5e0c0a8e63ecbcf1a04a1685619d4` |
