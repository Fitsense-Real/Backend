# Estado de las PR #55–#66 y de los cambios sin PR propia

Comprobado el 15/11/2026 con GitHub (las PR abiertas y el CI del HEAD) y con git sobre `origin`. `generation` está en `623c4fa`, sin cambios desde la versión v1. **Ninguna PR está fusionada.**

## 1. Inventario

| PR | Rama (head) | Base real | Commit | Funcionalidad | Depende de | Migraciones | Interruptores | CI | En la candidata | Aprobación adicional | Orden |
|---|---|---|---|---|---|---|---|---|---|---|---|
| #62 | `fix/sustituto-dosis-capacidad` | `generation` | `1da115a` | HALL-PROG-01: el sustituto SUST-SEG-1 recibe una dosis de capacidad válida | — | — | — | ✅ verify | Sí | No | 1 |
| #56 | `catalogo/integral-1-seguridad` | `generation` | `d8ffe4c` | V47 banderas de seguridad que exige el GIF; V48 dificultad conservadora | — | V47, V48 | — | ✅ | Sí | No | 2 |
| #57 | `catalogo/integral-2-textos` | #56 | `10b6ad6` | V49 nombres e instrucciones | #56 | V49 | — | ✅ | Sí | No | 3 |
| #58 | `catalogo/integral-3-clasificacion` | #57 | `ea5ebef` | V50 clasificación y equipamiento | #57 | V50 | — | ✅ | Sí | No | 4 |
| #59 | `audit/correccion-integral-catalogo` | #58 | `2e7c55c` | Controles permanentes del catálogo (huella, coherencia) | #58 | — | — | ✅ | Sí | No | 5 |
| #55 | `fix/contencion-seguridad-catalogo` | #59 | `57075e7` | INC-CAT-C39 reducida a 8 | #59 | — | — | ✅ | Sí | No | 6 |
| #60 | `cierre/v51-v52-correcciones` | #55 | `e34e892` | V51 impacto y estiramiento explícitos; V52 correcciones verificadas; HALL-PROG-01 (**mismo parche que #62**) | #55 | V51, V52 | — | ✅ | Sí | No | 7 |
| #63 | `cierre/pendientes-seguridad` | #60 | `4a728b4` | V52.1 pendientes técnicos; 1294 retenido; comparación por colocación (pruebas); verificación | #60 | V52.1 | — | ✅ | Sí | No | 8 |
| #65 | `fix/sustituto-gm-sin-mas-trabajo` | #63 | `55129f4` | HALL-GM-01: el sustituto de un ejercicio a mantenimiento no recibe la base | #63 | — | — | ✅ | Sí | No | 9 |
| #61 | `cierre/v53-apoyo-suspendido` | #60 (contiene ya V52.1 de #63) | `81b5f41` | V53 dato `requires_suspended_support`; opción A apagada | #60, #63 | V53 (V52.1 heredada) | `fitsense.seguridad.apoyo-suspendido-en-no-colgarse=false` | ✅ | Sí, **apagada** | Para **activar** A | 10 |
| #64 | `propuesta/reparacion-dificultad-publicada` | #63 | `7d7109a` | Reparación del plan 78 tolerando la dificultad subida tras publicar; V54 constancia del bloqueo al vencer | #63 | V54 | `fitsense.reparacion.tolerar-dificultad-de-publicacion=false` | ✅ | Sí, **apagada** (V54 sí se aplica: solo constancia) | Para **activar** la tolerancia | 11 |
| #66 | `propuesta/v53b-permiso-apoyo-suspendido` | #61 | `fce4fdc` | V53-B permiso propio de apoyo suspendido (opción B); V55 dos columnas en `user_profiles` | #61 | V55 | `fitsense.seguridad.permiso-apoyo-suspendido=false` | ✅ | Sí, **apagada** | Para **activar** B (y Flutter) | 12 |
| **#67** (final, borrador) | `cierre/catalogo-premerge-final` | `generation`; se fusiona después de las 12 | `12c6897` + documentos | Cambios propios de la candidata: P-DOLOR-PUB, INC-CAT-CONS-v1, V56 (853), referencia de 72 conservadora, pruebas, **P-03 por colocación** (encendida en pruebas, apagada en producción) | las 12 | V56 | `fitsense.catalogo.contencion-conservadora=true`, `fitsense.seguridad.dolor-en-planes-publicados=true`, `fitsense.regeneracion.p03-por-colocacion=false` en producción y true en pruebas | Regresión local completa en verde (`REGRESION_INTEGRADA_FINAL.md`); CI de #67 | — | **La autorización de merge** | 13 |
| **#68** (propuesta, borrador) | `propuesta/traza-omision-sistema` | `cierre/catalogo-premerge-final` (#67) | `ce775b8` | V57 constancia de la omisión impuesta | la final | V57 | — | Pruebas parciales | **No** | Sí (cambio de esquema) | Opcional, después |

## 2. Comprobaciones

**Parches equivalentes entre #60 y #62 (HALL-PROG-01):**

- `b65452c` (#60) y `b0267d0` (#62) tienen el **mismo** `git patch-id --stable` (`1acddf012c52…`).
- En la simulación, #62 se fusiona primero y #60 después sin conflicto.
- `SafetySubstitutionPolicy` contiene **una sola** definición de `dosisHeredada` y de `trabajo`, y hay un solo `SustitutoDosisCapacidadTest`.
- **No hay duplicado.**

**Migraciones V54, V55 y V56:**

- Cada una existe una vez (`V54__vencimiento_con_bloqueo_de_sistema.sql`, `V55__perfil_permiso_apoyo_suspendido.sql` y `V56__catalogo_853_texto_segun_gif.sql`).
- La numeración de Flyway queda continua tras V52.1/V53: 54, 55 y 56.

**`SOLO_ESQUEMA`:**

- #64 (V54) y #66 (V55) añaden cada una su línea en el mismo sitio. La simulación da conflicto en las dos fusiones y se resuelve **conservando las dos líneas** junto a la de V51/V53.
- El resultado es idéntico al de la candidata, así que no se pierde ninguna línea (ver `ORDEN_MERGE_CONTROLADO.md`).
- V56 cambia datos del catálogo, así que **no** va en `SOLO_ESQUEMA`.

**CI:** las 12 PR tienen el check `verify` en ✅ success sobre su HEAD:

| PR | Fecha del check |
|---|---|
| #56, #57, #58, #59 | 13/11 |
| #55 | 13/11 22:37 |
| #60 | 14/11 02:57 |
| #62 | 14/11 01:39 |
| #61 | 14/11 17:26 |
| #64 | 14/11 17:23 |
| #65 | 14/11 21:56 |
| #66 | 14/11 22:01 |
| #63 | 14/11 22:57 |

**Cambios de la candidata sin PR propia:** los 39 commits de `fce4fdc..12c6897` (P-DOLOR-PUB, INC-CAT-CONS-v1, V56, P-03 por colocación, pruebas y documentos). Se agrupan en la PR borrador final #67, que no duplica ninguna corrección de las otras PR: al fusionarse la pila, su diff contiene solo esos commits.
