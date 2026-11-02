# Cierre de la estabilización (orden final)

**La regla central permanece sin cambios:**

> «FitSense adapta únicamente lo necesario. Si una sesión falla, se corrige esa sesión; si corresponde progresar, se aumenta la dosis autorizada conservando los ejercicios; si no existe motivo de cambio, se mantiene la continuidad. Un respaldo no permite saltarse estas decisiones.»

## Entregables (`docs/FitSense/estabilizacion_ci_y_respaldo/`)

1. `VERIFICACION_Y_FUSION_PR19.md`
2. `ACTUALIZACION_PR17.md`
3. `REGRESION_PR17_ACTUALIZADO.md`
4. `COMPARACION_960_PERFILES.md`
5. `LABORATORIOS_OPCIONALES_FECHA.md`
6. `REPLAY_100_SEMANAS.md`
7. `DIAGNOSTICO_RESPALDO_MULTISEMANA.md`
8. `VERIFICACION_POLITICAS_ADAPTACION.md`
9. `DECISIONES_PENDIENTES.md`
10. `CIERRE_ESTABILIZACION.md`

**Evidencia** en `evidencia/diagnostico_respaldo/`:
- `ciclos.csv`;
- `sesiones.csv`;
- `resumen.md`;
- las salidas de `ValidacionMultisemanaIT` con las tres semanas.

Se conservan todas las evidencias anteriores.

---

## A. Integración técnica

| | Resultado |
|---|---|
| **PR #19** | **Fusionado** en `generation` con *merge commit*, tras comprobar CI verde en su cabeza 33bac15 y que no había despliegue. Ver `VERIFICACION_Y_FUSION_PR19.md` |
| **Commit de `generation`** | **dd61837730370d4510fe21f0bc9a14bf62c1a767**. Árbol idéntico a 33bac15; `src/main` sin cambios |
| **CI de `generation`** | **success** (#101 por `push` y #102 del PR #1). Es el primer verde desde el 2-nov |
| **PR #17 actualizado** | *Merge* de `generation` sin conflictos (bbf4e16), más dos commits solo de pruebas: 7b6f666 (`CicloRealPoliticaBase`) y 97befb6 (`LinajeIntegridadIT` y `PoliticasP123IT`). `src/main` sin cambios. Sigue **en borrador**, con `PLAN-HIST-1` por defecto y los indicadores apagados. Ver `ACTUALIZACION_PR17.md` |
| **Regresión del PR #17** | 1.046 unitarias; integración 492/438/54 (contabilizadas/ejecutadas/omitidas); 6/6 ciclos reales; 960 perfiles idénticos byte a byte; 4 baterías (HIST, C2-D, C2-D-EF, C2-D-V4-EF) con 223/225 cada una sobre 97befb6, 0 fallos. Ver `REGRESION_PR17_ACTUALIZADO.md` |
| **CI del PR #17** | **success** en 7b6f666 (#103), 97befb6 (#105) y bd023df (#107). El commit final de entregables se comprueba al subirlo |

**Despliegues y migraciones remotas:** ninguno iniciado por esta sesión. El único workflow es `CI` (Testcontainers, sin secretos). No hay acceso a Neon ni a Railway.

## B. Laboratorios

| | Resultado |
|---|---|
| **Pruebas corregidas** | `CampanaP123Cohorte02IT`, `CampanaMvp8UsuariosIT` y `ReincorporacionIaRealIT` (reloj de pruebas). Además, `LinajeIntegridadIT` y `PoliticasP123IT`, encontradas durante la regresión |
| **REPLAY de 100 semanas** | **Ejecutada.** Evidencia idéntica a la histórica salvo marcas de tiempo (`REPLAY_100_SEMANAS.md`) |
| **Resultados** | Mvp8 en ensayo seco 1/1. Reincorporación en modo seco 1/1, idéntica byte a byte a su evidencia. Con una semana ya vencida, 0 errores de vencimiento |
| **Limitaciones** | Ni `ReincorporacionIaRealIT` ni Mvp8 se han ejecutado con IA real (Replicate no autorizado). El conjunto `estres` de Mvp8 falla por P-01 (perfil de 15 min); no depende de la fecha y no se ha cambiado |
| **PR independiente** | **Fitsense-Real/Backend#20**, en borrador y sin fusionar. CI **success** en bc05ec9 (#104) y d4717b9 (#106) |

## C. Respaldo

**Definición real según el código:**
- `RESPALDO` es una adaptación con orden de adaptar y **sin propuesta de la IA usable**: IA apagada, cortacircuitos, factibilidad previa imposible o 3 propuestas rechazadas.
- La semana sale de `AdaptedWeekProducer.respaldo` como **copia** de la semana medida, revalidada entera (V8C y V1-V25), con origen `CONSERVED`.
- **Con dolor** cubierto por la memoria P-02, la copia **sustituye solo el ejercicio que dolió** por la regla determinista SUST-SEG-1 (P-03 nivel 1): mismo día, enfoque, minutos, slot y tipo, sin subir la dosis ni el trabajo, y registrado en V32.
- **No es una regeneración con reglas.**

**Causa de las diferencias entre semillas:**
- La semilla solo cambia el **plan inicial** de la prueba. Según cuál sea el último ejercicio del miércoles (el que el escenario marca con dolor), la **IA simulada de la prueba** propone algo válido (APLICADA) o algo que V7 rechaza: copia el tipo de prescripción del original, y entonces la semana va a RESPALDO.
- En RESPALDO, la sustitución SUST-SEG-1 es determinista.
- Reproducido con la prueba original: 26-oct, 2 fallos; 9-nov, 1 fallo; 2-nov, 0 fallos. Siempre el mismo cambio: el último ejercicio de la sesión del dolor (455→453, 1178→66) con la misma dosis.

**Comparación con las reglas de adaptación:** 72 recorridos, 175 ciclos y 525 sesiones, con **0 incumplimientos**:
- las no señaladas son idénticas;
- lo que dolió no vuelve;
- no hay variedad ni reincorporación automática;
- no se publica nada inválido.

Ver `VERIFICACION_POLITICAS_ADAPTACION.md`.

**¿Hay un defecto funcional?** **No.** Es el **Resultado 2**:
- la política permite una diferencia concreta (la sustitución del ejercicio que dolió);
- la aserción «RESPALDO ⇒ semana idéntica» es más estricta que la política y contradice la propia aserción de la prueba «lo que dolió no vuelve».

**Recomendación concreta (sin aplicar):**
- sustituir esa igualdad, **solo** para `RESPALDO` con `SEGURIDAD_ANTE_DOLOR`, por las invariantes del §4 del diagnóstico (D-1);
- actualizar el comentario de `AdaptedWeekProducer.produce`, que es solo documentación pero está en `src/main`.

**Observación independiente:** P-03 nivel 2 (regeneración con reglas tras un NO_FACTIBLE por dolor) no es localizada. Requiere decisión (D-2).

## D. Próximos pasos

**Decisiones que debes aprobar** (`DECISIONES_PENDIENTES.md`):
- **D-1:** la nueva aserción de respaldo con dolor.
- **D-2:** P-03 nivel 2 frente a las reglas 5.1 y 5.5.
- **D-3:** la semana sin publicar cuando SUST-SEG-1 no tiene sustituto.
- **D-4:** si se fusiona el PR #20, para que los laboratorios y las dos pruebas corregidas lleguen a `generation`.
- **D-5:** Railway, la evaluación profesional, Flutter y el despliegue.

**Preparación para la evaluación profesional de C2-D + EF:**
- **Técnicamente, lista:** el PR #17 está actualizado, con pruebas y CI verdes, comportamiento histórico idéntico y los 48 perfiles identificados por EF.
- Falta aprobar A1–A6 y elegir a los evaluadores. Se envía **solo** `revision_profesional/paquete_evaluador/`.
- El PR #17 no debe fusionarse ni activarse antes.

**Contratos para Flutter:**
- **Ya implementados en el PR #17, detrás de indicadores apagados:** duración estimada (`fitsense.api.duracion-estimada.enabled`) y NO_FACTIBLE estructurado (`fitsense.api.no-factible-estructurado.enabled`).
- **La especificación** (casos A–E) está en `preparacion_integracion_pr17/ESPECIFICACION_FLUTTER.md`.
- **Faltan:**
  - autorizar el trabajo en Flutter;
  - dar acceso a su repositorio, que no está en esta sesión;
  - decidir qué contrato se enciende primero;
  - aprobar la lista de equipamiento unificada.
- Para la app, el resultado `REGENERACION_SEGURA_SUSTITUCION` de la API distingue el respaldo con sustitución de una copia (`RESPALDO_PROPUESTA_IA_INVALIDA` y `RESPALDO_SIN_IA`).

## Lo que no se ha hecho

- Fusionar el PR #17 ni el PR #20.
- Desplegar ni aplicar migraciones remotas.
- Activar V4-C, C2-D, EF ni ninguna política experimental.
- Cambiar la lógica de adaptación, las reglas de dosis, progresión o reincorporación, ni el catálogo.
- Permitir regeneración libre, cambiar ejercicios por variedad o restaurar excluidos.
- Tocar Flutter ni consumir Replicate.
- Eliminar o relajar la aserción de respaldo.

## Validación final (fase 7)

| # | Condición | Estado |
|---|---|---|
| 1 | PR #19 fusionado correctamente | **Sí** (dd61837) |
| 2 | CI de `generation` verde | **Sí** (#101) |
| 3 | PR #17 actualizado desde `generation` | **Sí** (bbf4e16, *merge*) |
| 4 | CI del último commit del PR #17 verde | **Sí** (97befb6 #105, bd023df #107; los commits posteriores solo tocan documentación) |
| 5 | Política histórica intacta | **Sí** |
| 6 | 960 perfiles sin cambios inesperados | **Sí** (byte a byte) |
| 7 | Laboratorios corregidos o con limitaciones documentadas | **Sí** |
| 8 | Respaldo explicado y clasificado | **Sí** (Resultado 2) |
| 9 | 0 cambios funcionales no autorizados | **Sí**: `src/main` sin cambios en los PR #17 y #20; el PR #19 fusionado solo tenía pruebas |
