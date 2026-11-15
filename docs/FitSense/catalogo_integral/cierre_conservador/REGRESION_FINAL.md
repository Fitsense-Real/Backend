# Regresión final de la versión técnica conservadora

## 1. Resultado

| | Commit `eccff00` (primer candidato) | Commit `836a1f5` (**candidato definitivo**) |
|---|---|---|
| Unitarias (surefire) | 1530 · 0 fallos · 0 errores · 1 omitida | **1530 · 0 fallos · 0 errores · 1 omitida** |
| Integración (failsafe, PostgreSQL 16 en Testcontainers) | 752 · **13 fallos · 1 error** · 57 omitidas | **753 · 0 fallos · 0 errores · 57 omitidas** |
| Resultado de Maven | BUILD FAILURE → **no aprobado** | **BUILD SUCCESS** |
| Duración | 30 min (02:18–02:49 UTC) | 31 min (02:56–03:27 UTC) |
| Árbol de trabajo antes y después | Sin cambios | Sin cambios (`git status --porcelain` vacío antes y después) |

Las 57 omitidas son pruebas opt-in (`@EnabledIfSystemProperty`): campañas con Replicate, exploraciones, diagnósticos, el volcado congelado y la rejilla de 168. Las relevantes para esta orden se ejecutaron aparte sobre el mismo commit (§4).

Evidencia en `evidencia/regresion/`:

- `resumen_regresion_eccff00.txt` y `resumen_regresion_836a1f5.txt`: líneas de resumen de Maven y estado.
- `surefire_por_clase.tsv` y `failsafe_por_clase.tsv`: un informe por clase. Las clases `@Nested` publican además informes propios con su nombre visible, así que la suma de estas tablas no coincide con el total de Maven. **El total que cuenta es el de Maven.**

## 2. Condiciones de ejecución

- **Commit exacto:** `836a1f5aded805ec4c696e2bd411c97d4ee02f8d`, en un worktree aislado (`wt_cons_reg`, HEAD desacoplado) en el que no se compiló ni ejecutó nada más durante la regresión.
- **Orden:** `sh mvnw -o verify -Pintegration`, con `target` borrado antes. Unitarias e integración en la misma ejecución.
- **Recursos comprobados antes:**
  - disco: 15 GB libres de 252 GB;
  - memoria: 15 GB, 14 disponibles;
  - Docker 29.9.2 en marcha.
- **Registro completo conservado** fuera del repositorio, en el directorio temporal de la sesión (20 MB); en el repositorio queda el resumen.
- **Migraciones desde cero:** cada clase de integración arranca un PostgreSQL 16 **vacío** y aplica Flyway V1…V56 completo, con Hibernate `ddl-auto=validate`. Las 157 clases de integración reconstruyen el esquema y el catálogo desde cero.
- **Huella del catálogo:** `CatalogoCalidadIT` compara las 1.376 filas con `huella_catalogo.tsv` (recapturada en el mismo commit que V56) y pasa.

## 3. Los 14 fallos del primer candidato y cómo se resolvieron

Ninguno era una violación de seguridad ni un fallo del código de producción. Todos eran pruebas escritas antes de INC-CAT-CONS-v1 o de P-DOLOR-PUB que la regresión completa anterior no había llegado a ejecutar (se interrumpió).

| Prueba | Causa | Resolución (commit `836a1f5`) |
|---|---|---|
| `CatalogoElegibleIT` (3) | Su oráculo SQL del conjunto elegible no conocía la contención | El oráculo incorpora INC-CAT-CONS-v1. El apoyo suspendido se comprueba con el **dato** `requires_suspended_support` (V53), no con la lista del código, así que verifica también que coinciden |
| `Usuario31ReproduccionIT` | Reproduce el catálogo **V36** (Flyway hasta la 36) | Sin la contención, como las referencias V35–V40 |
| `RetiradaLocalizadaLimiteIT` y `RetiradaLocalizadaPreferenciaIT` | Reproducen el catálogo **V50** | Ídem |
| `HerenciaD8IT.dolorConRegeneracionSegura` (9004) | Premisa («P-03 publica para 9004») construida con el conjunto elegible anterior | Se ejecuta con la contención apagada; el mecanismo no depende del catálogo |
| `ReparacionLinajeIT` y `ReparacionLinajeRev2IT` | Premisa del sustituto. Con la contención, el sustituto del 1374 es el 104 (relleno de zona pequeña, 2x13), V8C no deja subir los 2x10 del quitado y la reparación queda **NO_VALIDA**, sin publicar | Contención apagada para el mecanismo P-P2/H6. El desenlace con la contención (conservador) queda en `IMPACTO_FACTIBILIDAD.md` y en `RIESGOS_RESIDUALES.md` R12 |
| `PermisoApoyoSuspendidoIT` (2) | Mide V53-B frente a «hoy» sin la contención | V53-B se mide sin la contención. **Se añade** `conContencionConservadora`: con la contención encendida y sin el permiso confirmado, ninguno de los 15; confirmado, los 10 |
| `PoliticasP123IT.restriccionesMultiplesYDolorAnterior` (error) | El arnés registraba trabajo en un ejercicio marcado por dolor (`dolor_registrado`), que P-DOLOR-PUB rechaza correctamente | El arnés registra como omitido lo marcado, como la persona con el aviso de la app |
| `Plan78ContencionReducidaIT` (2) | La versión reparada sustituye el 1303 **y también el 494** (identidad no determinada) | **Cambio de comportamiento aceptado y explicado:** una reparación crea una versión nueva, que es una planificación nueva y no puede publicar un contenido (V1). La prueba exige exactamente esas dos sustituciones, sin más dosis y sin ningún sustituto contenido. La versión publicada no se toca |

En `eccff00` ya se habían ajustado, antes de la regresión, otras pruebas por la misma razón (ver el mensaje de su commit):

- `ContencionTextoBanderaIT` (1303 y 1306);
- `CatalogoIntegralTextosIT` (V56);
- las referencias V35–V40 (catálogos anteriores);
- 9703/9710 en `RegeneracionPorColocacionRev2IT`.

**Ninguna prueba se desactivó, se borró ni se marcó como omitida.** Cuando una prueba se ejecuta con la contención apagada, es porque su premisa depende del conjunto elegible anterior. Cuando se cambió una expectativa, fue por un comportamiento que exige la política aprobada, y queda explicado en el código y aquí.

## 4. Pruebas opt-in ejecutadas aparte sobre el mismo commit `836a1f5`

| Prueba | Resultado | Evidencia |
|---|---|---|
| Rejilla de 168, contención encendida y apagada (`CatalogoIntegralComparacionIT`) | Ver `IMPACTO_FACTIBILIDAD.md` | `evidencia/rejilla_168/` |
| Volcado congelado (`CatalogoCongeladoIT`) | Ver `VERSION_CONGELADA.md` | `evidencia/congelado/` |
| 40 escenarios HALL-GM-01, protecciones encendidas y apagadas | Ver `VALIDACION_HALL_GM_01.md` | `evidencia/hall_gm_01/` |
| Rejilla de 312 de hipertrofia (`Hipertrofia4Dv34IT`, de la regresión) | 298 VALIDO, 14 FALLO (V20, los 14 de peso corporal del 775), 0 violaciones | `evidencia/rejilla_312/` |

## 5. Dictamen de la regresión

**La regresión completa pasó sobre el commit candidato definitivo `836a1f5`:** sin fallos, sin errores, sin ejecuciones interrumpidas y con el árbol sin cambios. La del primer candidato (`eccff00`) **no se considera aprobada**: sus fallos se corrigieron en un commit nuevo y la regresión se repitió entera.
