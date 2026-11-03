# 03 · Regresión y CI de la candidata H1

**Código probado:** `79ec110` (rama `candidate/h1-multiconjunto`), con la **regla por defecto `multiconjunto`**, sin propiedades ni variables de entorno.

**Ejecución local:**
- `mvnw -o -B -ntp verify -Pintegration`;
- después, la comparación de 960 perfiles y la REPLAY de 100 semanas;
- PostgreSQL efímero (Testcontainers, `postgres:16-alpine`), sin Neon, sin Replicate y sin Railway.

**Evidencia:** `evidencia/regresion_candidata_*.txt`, `evidencia/960_replay_candidata_pruebas.txt` y `evidencia/replay_*`.

## 1. Totales: superadas, fallidas y omitidas

**Una prueba omitida no es una prueba superada:** se cuenta aparte.

| Bloque | Ejecutadas | Superadas | Fallidas | Errores | Omitidas | Comparación con #31 (regla `conjunto`) |
|---|---:|---:|---:|---:|---:|---|
| Unitarias (surefire) | 1.105 | 1.104 | **0** | **0** | 1 | Mismas 1.105 |
| Integración (failsafe, perfil `integration`) | 495 | 458 | **0** | **0** | 37 | 489 + **6 nuevas** (`ConservacionPatronH1IT` 3, `ConservacionPatronH1ConjuntoIT` 3) |
| 960 perfiles + REPLAY (*opt-in* activado) | 3 | 2 | **0** | **0** | 1 | Igual |

**Las omitidas son las mismas que en #31.** Todas son *opt-in*, con `@EnabledIfSystemProperty`, y la lista completa está en `evidencia/regresion_candidata_pruebas.txt`:
- **Unitarias (1):** `H1ImpactoHistoricoTest`, el laboratorio de impacto, ya ejecutado en `validacion_h1_h12/04`.
- **Integración (37):** necesitan IA real, URL de Neon o de lectura, o un laboratorio explícito. Ninguna valida la regla H1.
- **960 y REPLAY (1):** `CampanaP123Cohorte02IT` en modo inspección (`fitsense.campana.p123.inspeccion`). La REPLAY en sí se ejecutó y pasó.

## 2. Comprobaciones de seguridad pedidas

| Comprobación | Resultado | Evidencia |
|---|---|---|
| Generación inicial idéntica | **Sí** | 960 perfiles: `planes.csv` `d109a3da…a859` y `colocaciones.csv` `d227848b…265ef`, **iguales** a la referencia de `generation` y de #31; REPLAY `generacion.csv` sin cambios |
| Adherencia y cobertura sin cambios | **Sí** | REPLAY `metricas.csv` (Aw, Aobs, cobertura de registro) sin cambios; `AdherenciaSinPesosTest`, `AdherenciaRiesgoIT` y pruebas de cobertura 4C, todas superadas |
| Decisiones semanales sin cambios | **Sí** | REPLAY `historial.csv` (decisión, estado V32, resultado API) y `v32.csv` sin cambios |
| V20 y V24 intactos | **Sí** | Sin cambios en su código. «V20: el piso es el 70 % exacto» (16), «V34: V24 + V8C» (16), «4D v3.1/v3.2/v4» y «validadores: V23, V24 extendida y V25» superadas |
| Sesiones no señaladas conservadas | **Sí** | `ConservacionPatronH1IT` comprueba que las no señaladas quedan **idénticas** tras publicar la propuesta correcta; pruebas D8 y V8C superadas |
| Sustituciones por dolor intactas | **Sí** | La regla nueva no se evalúa en la rama de dolor. «V8C: dolor: sustituir o retirar, sin más ejercicios ni más volumen» (6), «Respaldo forzado: con dolor y sustituto…» (3), REPLAY `exclusiones_dolor.csv` igual salvo marcas de tiempo |
| Sin aumentos de dosis no autorizados | **Sí** | REPLAY `PASOS.csv` (con columnas `autorizacion` y `validacion_independiente`) sin cambios; `ConservacionPatronH1IT` comprueba que la sesión publicada no sube ninguna dosis; `PlanInputSnapshotDosisTest`, `MvpClaseDosisTest` y C1 superadas |
| Linaje conservado | **Sí** | `LinajeIntegridadIT` superada; REPLAY `planes.csv` (con `source_planned_workout_id`) sin cambios |
| 960 perfiles | **Idénticos** | hashes anteriores |
| REPLAY de 100 semanas | **Pasa** | `git status` tras la ejecución: solo `exclusiones_dolor.csv`, con 2 filas en las que **solo cambian `recorded_at` y `reactivated_at`**, marcas del reloj real (`evidencia/replay_diferencias.diff`). Se descartó con `git checkout`; la evidencia anterior no se sobrescribe |

## 3. La prueba permanente nueva (en CI)

| Prueba | Regla | Resultado |
|---|---|---|
| `ConservacionPatronH1IT.reglaPorDefecto` | por defecto | MULTICONJUNTO |
| `ConservacionPatronH1IT.distribucionIncorrectaRechazada` | por defecto | 3 rechazos con el mensaje de H1; semana conservada (copia exacta); 3 llamadas a la IA simulada |
| `ConservacionPatronH1IT.distribucionCorrectaPublicada` | por defecto | Publicada por la IA; distribución de patrones conservada; sin subir dosis; no señaladas intactas |
| `ConservacionPatronH1ConjuntoIT.reglaExplicita` | `conjunto` | CONJUNTO |
| `ConservacionPatronH1ConjuntoIT` (propuesta incorrecta) | `conjunto` | **Publicada**: la distribución cambia (comportamiento histórico, documentado) |
| `ConservacionPatronH1ConjuntoIT` (propuesta correcta) | `conjunto` | Publicada |

## 4. Regresión de la rama de textos

En `04_TEXTOS_JUSTIFICACION.md` §5, en esa rama.

## 5. CI

`.github/workflows/ci.yml`, trabajo `verify` (`mvn -B -ntp verify -Pintegration`). El resultado del commit final se informa en el PR candidato y en el informe final:
- el commit de código `79ec110`;
- el commit de documentación encima, que no cambia código.
