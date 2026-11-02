# Informe de cierre · auditoría integral de la lógica del backend

**Fecha:** 30/10/2026. **Rama:** `auditoria-integral`. **PR:** [#9](https://github.com/Fitsense-Real/Backend/pull/9), hacia `generation`.

**Documentos de la auditoría:**

| Doc. | Archivo | Contenido |
|---|---|---|
| A | `AUDITORIA_INTEGRAL_LOGICA.md` | Estado por bloque |
| B | `CORRECCIONES_IMPLEMENTADAS.md` | Qué se corrigió y con qué prueba |
| C | `ADAPTACION_POR_CAUSAS.md` | Matriz de motivos e intervenciones |
| D | `VALIDACION_MULTISEMANA.md` | Escenarios humanos |

## 1. Estado del PR

| Commit | Contenido |
|---|---|
| `e60cdee` | V41 (catálogo C4/C6 fase 2) y referencia V40 congelada |
| `a335e48` | Adaptación por causas: D1, D2, D6 y D7, y V42 |
| `a2271ab` | Dolor con adherencia buena |
| `68a1f15` | Resultado por sesión adaptada (consulta) |
| `8dff41a` | Validación multisemana |
| `194f270` | Texto de CONSERVAR (fallo del CI, ver §2) |
| el commit de este informe | Documentos A–E y `REFERENCIAS.md` |

## 2. CI

**Fallo en los tres primeros CI:**
- Los commits `a335e48`, `a2271ab` y `68a1f15` fallaron en las pruebas unitarias, en `TipoIntervencionTextoCoherenteTest` (4 de 7).
- **Causa:** el texto de la intervención nueva CONSERVAR no enunciaba lo que no permite (sustituir, retirar, la dosis, el día).
- **Por qué importa:** ese texto también acaba en los mensajes de rechazo de V8C.
- **Corrección:** se cambió el texto y la prueba quedó igual (`194f270`). La suite unitaria pasa en local (864 pruebas).
- **Efecto secundario:** como fallaba en las unitarias, el CI de esos commits no llegó a las de integración.

**Fallo en el CI de `d41347f` (37 errores en las pruebas de integración):**
- Las otras pruebas de integración pasaron, incluidas las de adaptación y la multisemana.
- **Causa:** V42 es la primera migración desde V35 que añade columnas que las entidades mapean. Con `ddl-auto=validate`, las 15 clases fijadas a un catálogo anterior con `spring.flyway.target` no cargaban el contexto.
- **Corrección (solo en pruebas):** `EsquemaVigenteSobreCatalogoAnterior` aplica, después del target, las migraciones de solo esquema posteriores (V42, que no toca el catálogo). Es idempotente.
  - Las aserciones no cambian.
  - La aplicación sigue validando el esquema completo.
  - Las 15 clases (37 pruebas) pasan en local.
- Una migración futura que cambie datos del catálogo no debe añadirse a esa lista.

**Resultado sobre el último commit:** ver §8.

## 3. Simulaciones finales (una vez, sobre el candidato `194f270`)

| | Resultado |
|---|---|
| 72 referencias | 72/72 válidas. **Idéntica byte a byte** a la oficial V41 |
| Barrido de 72 (4–6 días) | 72/72 VALIDA; capacidades COMPLETO en 72/72 |
| Robustez | 288/288 VALIDA |
| Restricciones (192) | 192/192 publicados; 0 violaciones; 0 con un implemento ausente |
| Continuidad (96) | ver §8 |

- Barrido, robustez y restricciones son **idénticos byte a byte** a la evidencia de V41 (`evidencia/v41/final/`).
- Era lo esperado: V42 y los cambios de adaptación no tocan la generación.

## 4. Cambios en las referencias

- **V40 congelada:** `generacion_referencia_v40.txt`, reproducida por `GeneracionReferenciaV40IT` y con su huella en `ReferenciasHistoricasInmutablesTest`.
- **Oficial V41:** cambian 20 de 72 perfiles, todos de nivel avanzado; los 72 siguen válidos.
- **262:** de 11 apariciones a 0.
- **Ninguna referencia histórica se ha modificado.**
- Detalle en `evidencia/progresion/REFERENCIAS.md`.

## 5. Migraciones

| Migración | Contenido | Aplicada en un entorno persistente |
|---|---|---|
| V41 `catalog_c4_c6_fase2` | 25 cambios de columna en 15 ejercicios, con comprobación previa y final | **No** |
| V42 `adaptation_lack_of_time_context` | 2 columnas opcionales en `planned_workouts`, CONSERVAR en V32 y `adaptation_rule_version` | **No** |

- Ninguna migración ya aplicada se editó.
- **Fusionar no despliega nada:** el CI no tiene secretos y Railway no despliega desde `generation`.
- **Advertencia:** arrancar el backend a mano contra Neon aplicaría V38–V42. Para evitarlo: `SPRING_FLYWAY_TARGET=37`.

## 6. Riesgos residuales

1. **Cambio de perfil** (días o equipamiento): la semana siguiente queda sin plan (NO_FACTIBLE). Es el más importante.
2. **C1:** 48 apariciones de jalones con banda sentados en perfiles de casa sin banco.
3. **1113** (nivel 1) en 4 perfiles principiantes, con la dificultad sin evaluar.
4. **El cliente Flutter aún no pregunta la subcausa:** hasta que lo haga, toda falta de tiempo se conserva (D1). No se tocó Flutter.
5. **Sin validación con IA real:** el comportamiento de ADAPT-2 (techo M, CONSERVAR) no se ha probado con Replicate. No hubo llamadas de pago.
6. **No hay validación humana.** La revisión visual del catálogo es asistida por IA.

## 7. Decisiones funcionales que requieren aprobación

| # | Decisión | Opciones | Recomendación técnica |
|---|---|---|---|
| 1 | Cambio de perfil a mitad del estudio | regenerar / remapear con excepción V8C / bloquear en el cliente | Regenerar |
| 2 | Orden de los ejercicios en GAIN_MUSCLE (20 de 54 sesiones con un estructural detrás de un resistido) | regla «multiarticular primero» / mantener | Regla nueva, solo de orden; no cambia la selección |
| 3 | C1: ¿sentarse en un banco o cajón exige banco? | exigirlo / no | Decidir junto con D3 (tirón en casa) |
| 4 | C2: ¿balón apoyado en el suelo cuenta como suelo? | sí / no | Sin uso actual; baja prioridad |
| 5 | 54, 1174 y 1295: ¿son cardio? | cardio / resistido | Decidir con el criterio de cardio de V38 |
| 6 | Dificultad de 862, 1113 y 1106 | instrumento de dificultad (V27) | Priorizar 1113 por su uso en principiantes |
| 7 | 1086 | revisión humana del GIF | — |
| 8 | ¿Cuenta aparte un RESPALDO por un techo M inviable? | sí / no | Sí, como subestado en los indicadores |

## 8. Resultado final

| Comprobación | Resultado |
|---|---|
| CI (`mvn verify -Pintegration`, GitHub Actions) | **Verde** sobre `95baa02`: compilación, 864 pruebas unitarias e integración completa. Este informe solo añade documentación; el CI de su commit consta en el PR |
| Simulaciones finales | Todas pasan (§3). La continuidad: 96 perfiles y LW de 5–6 días, 0 fallos |
| Restricciones de la orden | Sin despliegue, sin Neon, sin `main`, sin Replicate de pago, sin Flutter. Progresión apagada. Ninguna referencia histórica cambia. Ningún validador relajado |
| Fusión en `generation` | Se cumplen las condiciones de la orden. Las decisiones pendientes (§7) no tienen código en este PR |

**Estado por bloque (§22 de la orden):**

| Bloque | Estado |
|---|---|
| Catálogo | CORREGIDO Y PROBADO; con pendientes de decisión |
| Generación | VERIFICADO; con un pendiente de decisión (el orden en GAIN_MUSCLE) |
| Adherencia | VERIFICADO |
| Adaptación por causas | CORREGIDO Y PROBADO |
| Varios motivos | CORREGIDO Y PROBADO |
| Progresión | VERIFICADO |
| Reincorporación | VERIFICADO |
| Trazabilidad | VERIFICADO |
| Multisemana | VERIFICADO; con un pendiente de decisión (el cambio de perfil) y NO REPRODUCIBLE (usuarios 31, 32 y 33, sin sus registros) |

## 9. Actualización posterior: decisión 1 resuelta

**Decisión del responsable (30/10/2026):** durante el estudio, la persona no puede modificar su perfil.

**Implementación:**
- Desde el primer plan, `PUT /api/v1/users/me/profile` responde 422 (`UserProfileLockedException`) y no guarda nada.
- Antes del primer plan, el perfil se puede corregir.
- Lo prueba `PerfilCongeladoIT`.

**Efecto:** el riesgo residual 1 y la decisión 1 quedan cerrados.

**Lo que tiene que hacer la app Flutter:** no ofrecer la edición del perfil después del primer plan, y mostrar el mensaje del 422 si llega.
