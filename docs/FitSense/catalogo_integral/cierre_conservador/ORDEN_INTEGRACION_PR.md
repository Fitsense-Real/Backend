# Orden de integración de la versión técnica conservadora

**No se ha fusionado nada.** La versión candidata vive en la rama `cierre/catalogo-conservador-v1`.

## 1. Composición verificada (HEAD, base y dependencias)

Estado de GitHub del 15/11/2026, comprobado de nuevo en la fase 1 de la orden de finalización: las 12 PR siguen abiertas, sin fusionar y con el mismo HEAD. Todas son ancestros del commit candidato `eccff00` (`git merge-base --is-ancestor`).

| PR | Rama (head) | Base | Contenido | En la candidata |
|---|---|---|---|---|
| #56 | `catalogo/integral-1-seguridad` (`d8ffe4c`) | `generation` | V47 banderas · V48 dificultad conservadora | Sí |
| #57 | `catalogo/integral-2-textos` (`10b6ad6`) | #56 | V49 nombres e instrucciones | Sí |
| #58 | `catalogo/integral-3-clasificacion` (`ea5ebef`) | #57 | V50 clasificación y equipamiento | Sí |
| #59 | `audit/correccion-integral-catalogo` (`2e7c55c`) | #58 | Controles permanentes del catálogo | Sí |
| #55 | `fix/contencion-seguridad-catalogo` (`57075e7`) | #59 | INC-CAT-C39 reducida a 8 | Sí |
| #60 | `cierre/v51-v52-correcciones` (`e34e892`) | #55 | V51 + V52 + HALL-PROG-01 | Sí |
| #63 | `cierre/pendientes-seguridad` (`4a728b4`) | #60 | V52.1, 1294 retenido, comparación por colocación, verificación | Sí |
| #65 | `fix/sustituto-gm-sin-mas-trabajo` (`55129f4`) | #63 | **HALL-GM-01** | Sí |
| #64 | `propuesta/reparacion-dificultad-publicada` (`7d7109a`) | #63 | Plan 78 (interruptor **apagado**) + V54 (solo constancia) | Sí, apagado |
| #61 | `cierre/v53-apoyo-suspendido` (`81b5f41`) | #60 | V53 (dato; comportamiento **apagado**) | Sí, apagado |
| #66 | `propuesta/v53b-permiso-apoyo-suspendido` (`fce4fdc`) | #61 | V53-B + V55 (interruptor **apagado**) | Sí, apagado |
| #62 | `fix/sustituto-dosis-capacidad` (`1da115a`) | `generation` | HALL-PROG-01 | Sí |

**Sin duplicados.**
- HALL-PROG-01 llega dos veces, por #60 (`b65452c`) y por #62 (`b0267d0`), pero las dos tienen el mismo `patch-id` (`1acddf012c52`). El código se aplica una sola vez.
- Las demás PR se apilan sin solaparse.

## 2. Commits propios de la candidata (encima de la pila)

| Commit | Qué |
|---|---|
| `798635f` | **P-DOLOR-PUB**: el ejercicio con dolor registrado queda marcado en las sesiones publicadas abiertas, y la API ya no muestra la sesión como bloqueada; ver `ee780a5` |
| `2e590d3` | **INC-CAT-CONS-v1**: contención conservadora para planificaciones nuevas |
| `8cf81ef` | **P-03-COLOCACION**: regla por colocación en P-03 nivel 2, **apagada**, más el contraejemplo |
| `ee780a5` | API del plan alineada con P-DOLOR-PUB; volcado congelado (`CatalogoCongeladoIT`) |
| `c6aeced`, `b018d9f` | Clasificación A/B/C/D, orden de integración y limitaciones (solo documentos) |
| `3493194` | **V56**: nombre e instrucciones del 853 según el GIF completo; sale de INC-CAT-CONS-v1; huella recapturada |
| `eccff00` | **Commit candidato.** Pruebas: referencia de 72 conservadora aparte (la histórica intacta), los 10 casos de dolor publicado, HALL-GM-01 con contención |
| siguientes | Solo documentos y evidencias del cierre (resultados de la regresión, congelación). No tocan código ni pruebas: ver `VERSION_CONGELADA.md` |

## 3. Orden propuesto (todo sujeto a aprobación)

1. **#62** → `generation`. HALL-PROG-01; es independiente.
2. **#56 → #57 → #58 → #59 → #55**, en ese orden, cada uno sobre el anterior.
3. **#60**, y después **#63**.
4. **#65** (HALL-GM-01). Corrige un defecto real y no depende de ninguna decisión pendiente.
5. **Los commits propios de la candidata**: P-DOLOR-PUB, INC-CAT-CONS-v1, V56, P-03-COLOCACION apagada, pruebas y documentos. Pueden ir como una PR propia sobre #65 (rama `cierre/catalogo-conservador-v1`).
6. **#61** (V53, apagado).
7. **#64 y #66**, solo si se aprueban. Conflicto conocido: las dos añaden una línea a `SOLO_ESQUEMA` (V54 y V55). Se resuelve conservando las dos líneas, como en la candidata.

Si **no** se aprueban #64 y #66, la candidata puede construirse sin ellas. En ese caso hay que repetir la regresión, porque V54 y V55 desaparecerían del esquema congelado. Eso sería **otra versión**, no `FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1`.
