# Desbloqueo del cierre del catálogo: informe (orden de 14/11/2026)

**Autorizado:** investigar y preparar las correcciones técnicas.
**No autorizado:** merge, despliegue ni cambios en las políticas deportivas.
**Lo que no se ha hecho:**
- nada se ha fusionado ni desplegado;
- no se ha gastado en Replicate;
- no se han tocado datos reales;
- no se han relajado V15, V24, V8C ni P-03;
- no se ha bajado el impacto de 1083;
- V53 no se ha activado;
- no se ha inventado ninguna firma.

**El catálogo NO está cerrado:** siguen abiertas decisiones de seguridad que requieren un profesional (§6).

## 1. HALL-PROG-01: causa exacta y evidencia

Detalle completo en `HALL_PROG_01.md`.
- **Causa.** `SafetySubstitutionPolicy.dosis` copiaba al sustituto de seguridad (SUST-SEG-1) la dosis del ejercicio quitado. Si esa dosis venía de una progresión (3x18 → 3x19), el sustituto heredaba una dosis sin autorización propia y V24 rechazaba la semana. La regeneración segura con dosis base podía subir el trabajo (1.140 → 1.260 s). P-03 la rechazaba y la semana quedaba NO_FACTIBLE.
- **No era del catálogo.** Reproducido por semilla en condiciones idénticas, con p02 sola:
  - `generation` falla en 4 de 5 semillas;
  - la base 57075e7, anterior a V51 y V52, falla en 5 de 5.

  El catálogo solo cambiaba qué semillas lo disparan. Esto corrige lo que se dijo antes: que «en la CI aparece con el cierre». En la CI, el `userId` de la prueba caía en una semilla u otra.

## 2. Corrección y pruebas

| Cambio | Dónde | Efecto |
|---|---|---|
| Dosis de capacidad válida para el sustituto, nunca con más trabajo que el quitado; si no cabe ninguna, la semana se rechaza | `SafetySubstitutionPolicy` (PR #62 y #60) | p02 pasa en las 5 semillas, en `generation`, en la base y en la PR A |
| Refuerzo de V8C: una subida de la IA sobre un ejercicio que luego se sustituye se rechaza | `AdaptedWeekProducer.subidasDeLaIaEnQuitados` (PR #62 y #60) | Cierra el efecto colateral que encontró la primera regresión (O12 `subeSeries`). Más estricto |

**Pruebas nuevas:**
- `SustitutoDosisCapacidadTest` (9 casos: positivo, negativo, regresión, nunca más trabajo y controles);
- `SubidaIaEnQuitadoTest` (5 casos).

**Pruebas existentes que cubren cada flujo:**
- progresión: `ProgresionIT` y `ProgresionRev2IT`;
- adaptación: O12, `RetiradaLocalizada*IT`, `SinSustitutoDolor*IT` y las unitarias V8C;
- reincorporación: `Reincorporacion4DTest` y `ReincorporacionTrasProgresionTest`;
- planes publicados: `Plan78ContencionReducidaIT`;
- generación: no pasa por SUST-SEG-1.

**No se ha modificado ninguna prueba para volverla verde.** El único cambio en una prueba es el texto de `Hipertrofia4Dv34IT`: decía 15 perfiles y la lista exigida tiene 14. La expectativa no cambia.

## 3. Estado de 1083

| Medida | Estado |
|---|---|
| **Contención temporal (código)** | `ExerciseRepository.findEligible` excluye, para quien no admite impacto, también `impact_level = 'ALTO'`, aunque falte HI. Oráculo de `CatalogoElegibleIT` actualizado |
| **Corrección definitiva (dato)** | HI añadido en V52 por decisión conservadora del coordinador (`APLICAR_CONSERVADOR`). Se apoya en la evidencia de la V3 (X01) y en que 1084, 1085 y 1087, de la misma familia, ya llevan HI |
| **Control permanente** | `CHECK (NOT is_active OR impact_level IS DISTINCT FROM 'ALTO' OR high_impact)` en V52: ningún activo puede volver a quedar ALTO sin HI |
| **Lo que no se hizo** | No se bajó su impacto para recuperar elegibilidad |
| **Pendiente** | La pregunta de clase DEC-HI-LANZAMIENTOS (1083–1087: ¿el lanzamiento balístico es pliometría?) requiere un profesional (A-1083) |

## 4. Estado de V53 y 775

### 775
- **Equipo real:** banco, con los codos sobre dos apoyos elevados, según R2, V3 y el coordinador.
- V52 lo corrige (`bench; 1:bench`) y ya no figura como solo peso corporal.
- **Consecuencia medida.** **14 perfiles** GAIN_MUSCLE en casa solo con peso corporal quedan sin plan por V20:
  - intermedio, 3 y 4 días, a 58, 59, 60, 74 y 75 min;
  - principiante y avanzado, 3 días, a 60 y 75 min.

  `Hipertrofia4Dv34IT` exige exactamente esa lista y ese motivo. No existe semana válida con el equipo que declaran.
- **Referencia de 72 planes.** Cambian 30, todos de casa, y los 72 siguen VALIDA.

### V53
- **Criterio conservador propuesto:** «no colgarse» incluye el apoyo suspendido sobre un aparato sin soporte de pies.
- **Revisión de los 16 candidatos:** 15 cumplen. 1301 no cumple (manos en el suelo y pies en correas), sale de V53 y sigue contenido por INC-CAT-C39.
- **Interruptor:** `fitsense.seguridad.apoyo-suspendido-en-no-colgarse`, **apagado por defecto**. V53 solo migra el dato; apagado, el permiso significa lo mismo que antes. Lo prueba `ApoyoSuspendidoInterruptorIT`: dato, apagado, encendido, causa del validador y script de impacto de solo lectura.
- **Impacto sobre permisos guardados:** `cierre/v53/impacto_permisos_v53_solo_lectura.sql`, que no se ha ejecutado contra datos reales.
- **Para aprobar:** el significado, la lista de 15 y qué hacer con los perfiles que ya tienen el permiso guardado (`cierre/V53_APOYO_SUSPENDIDO.md`).

## 5. Regresión completa

[PENDIENTE: se completa con los resultados de la regresión final de las tres ramas]

## 6. Decisiones que siguen exigiendo revisión profesional

Registro único: `cierre/REGISTRO_DECISIONES_PENDIENTES.tsv` (`REGISTRO_DECISIONES.md`). Tiene 323 filas: las 313 anteriores, 4 de esta orden y 6 hallazgos colaterales.

| Quién | Filas | Contenido |
|---|---|---|
| Profesional | 309 | 248 dificultades (V27), 27 identidades, contenciones INC-4C-03 e INC-CAT-C39, banderas HI, INV y AX dudosas, D2 (854), V39 (1113) y textos no verificables |
| Propietario y profesional | 6 | Significado de «no colgarse» (V53) |
| Técnico (otra ronda R2 + V3) | 6 | 231, 367, 1303 (texto), 1323, 1182 y 1017 |
| Resuelto técnicamente | 2 | 775 y la identidad de 675 |
| Contenido | 1 | 1083 |

- **Prioridad 1 (seguridad):** 43 filas, todas abiertas.
- **Valor vigente no conservador:** 868 (sin INV), 1294 (texto erróneo, no elegible en la rejilla), 1113 (nivel 1) y 1323 (texto).

## 7. Riesgos para planes publicados (incluido el plan 78)

Detalle en `IMPACTO_PLANES_PUBLICADOS.md`.
- **1083 con HI:** para quien no admite impacto, una sesión publicada con 1083 queda bloqueada (`alto_impacto`). Es lo correcto.
- **775 con banco:** no bloquea sesiones publicadas, porque el equipo no es causa de seguridad. La reparación y la regeneración ya no lo usarán sin banco.
- **V53:** sin efecto mientras siga apagado.
- **Corrección de HALL-PROG-01:** solo afecta a reparaciones nuevas. Lo ya publicado no cambia.
- **Plan 78:**
  - solo el jueves queda bloqueado (1303, `INC-CAT-C39`);
  - la reparación sigue sin publicar por el 916 (V48) y el 910 (V52), que subieron de nivel después de publicarse;
  - el mecanismo de reparación, aislado, publica sin subir dosis;
  - si la semana ya empezó, la sesión vence como SKIPPED sin motivo.

  No se resuelve relajando la dificultad: queda para decisión.
- **Antes de desplegar:** ejecutar la consulta de solo lectura de `IMPACTO_PLANES_PUBLICADOS.md` §3.

## 8. Estado de las PR

[PENDIENTE: se completa con la regresión final y la CI]
