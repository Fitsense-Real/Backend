# Cierre de la integración controlada

## Resultado

| | Estado |
|---|---|
| **PR #18** | **Fusionado** en `generation` con *merge commit* (**bfd5756**), con autorización expresa. **Sin despliegue y sin activar V4-C**: `PLAN-HIST-1` por defecto y `allow-candidates=false` |
| **PR #17** | **Actualizado** desde `generation` con *merge* (1e42519; documentación en commits posteriores). Pruebas verdes, CI verde. **Sigue en borrador, sin fusionar** |
| Siguiente etapa | Evaluación profesional de C2-D + EF (tras aprobar A1–A6) e implementación móvil de los contratos pendientes |

**No se ha hecho nada de lo siguiente:**
- fusionar el PR #17;
- desplegar;
- aplicar migraciones en Neon o Railway;
- activar políticas o indicadores;
- cambiar 4A, 4C, 4D, V24, V8C, la progresión, la reincorporación o el catálogo;
- tocar Flutter;
- llamar a Replicate;
- enviar el paquete profesional.

## Entregables (`docs/FitSense/integracion_controlada_pr18_pr17/`)

1. `VERIFICACION_PREVIA_PR18.md`
2. `RESULTADO_FUSION_PR18.md`
3. `ACTUALIZACION_PR17.md`
4. `VERIFICACION_TRAZABILIDAD.md`
5. `REGRESION_PR17_ACTUALIZADO.md`
6. `COMPARACION_HISTORICA.md`
7. `DECISIONES_PROFESIONALES_PENDIENTES.md`
8. `CIERRE_INTEGRACION.md`

## Informe final (15 preguntas)

**1. ¿Se fusionó el PR #18 correctamente?**
- **Sí.** Primero se quitó el estado de borrador; después se fusionó con *merge commit*, comprobando la cabeza fa7402b.
- El árbol de `generation` es idéntico al del PR y no hay cambios inesperados.

**2. ¿Cuál es el commit final de `generation`?**
- **bfd57563d0e594e190b5e0b6068f6332558b43e8**.
- Su CI (ejecución #94, `push`) terminó en **success**.

**3. ¿Se confirmó que no hubo despliegue ni migración remota?**
- **Por parte de esta sesión, ninguno:** solo existe y solo se ejecutó el workflow `CI` (ejecuciones #94 a #96), con Testcontainers y sin secretos. El repositorio no tiene configuración de despliegue.
- **Railway:** no hay acceso a su panel desde aquí. Que no despliegue desde `generation` se apoya en la confirmación documentada del responsable y en la ausencia de estados de despliegue en GitHub.
- **Recomendación:** comprobar en el panel de Railway que no hubo ningún despliegue a las 22:56 UTC y que la rama observada no es `generation`.

**4. ¿V44 se conserva intacta?**
- **Sí:** un único archivo, SHA-256 `640b0169…e2708400`, en `generation` y en el PR #17.
- V45 sigue después, solo en el PR #17.

**5. ¿La política histórica sigue siendo la predeterminada?**
- **Sí:** `PLAN-HIST-1` y `allow-candidates=false`, en `generation` y en el PR #17.

**6. ¿Se actualizó el PR #17?**
- **Sí,** con *merge* y 13 conflictos resueltos:
  - 11 con la versión del PR #17, porque el PR #18 no los cambió tras el port;
  - `PoliticaV4CCicloIT` con la de `generation`;
  - `PlanningPolicyTrace` integrada a mano.
- Frente al PR #17 anterior, `src/` cambia solo en 4 archivos.

**7. ¿Se conservaron los nuevos campos de trazabilidad?**
- **Sí:** `minutos_perfil`, `minutos_efectivos`, `limite_v4_aplicado`, `limite_v4_minutos_base` y `dentro_limite_v4_aplicado`.
- Junto a ellos siguen `c2d`, `excepcion_c2d`, `excepciones_c2d` y `elegibilidad_funcional`.
- Lo verifican `TrazaV4CTest` y `TrazaHistoricaIT`.

**8. ¿El PR #17 compila y pasa todas las pruebas?**
- **Sí,** con 0 fallos:
  - **1.028 unitarias**;
  - **438 de integración**;
  - **4 baterías** con 223/225 cada una;
  - los 960 perfiles y REPLAY.
- CI verde (#96).

**9. ¿Los 960 perfiles históricos siguen idénticos?**
- **Sí:**
  - con HIST, iguales a `generation` línea a línea;
  - en las 5 políticas, iguales byte a byte al PR #17 anterior.

**10. ¿La campaña REPLAY conserva sus resultados?**
- **Sí:** idéntica salvo marcas de tiempo, como siempre.

**11. ¿Cuántas pruebas se ejecutaron y cuántas se omitieron?**
- **PR #17:**
  - 1.028 unitarias, todas ejecutadas;
  - integración: 492 contabilizadas, 438 ejecutadas y **54 omitidas**;
  - baterías: 4 × 225 contabilizadas, 223 ejecutadas y **2 omitidas** en cada una;
  - campaña: 2 contabilizadas, 1 ejecutada y **1 omitida**.
- **Las omitidas son las de siempre** (opt-in o IA real) y no se cuentan como aprobadas.

**12. ¿Qué problemas permanecen pendientes?**
- **(a) Railway:** confirmar en su panel la rama observada (ver la pregunta 3).
- **(b) CHECK de V44 en `generation`:** admite versiones `PLAN-C2D-*` que `generation` no reconoce. Solo importa si una base ejecuta el PR #17 y luego vuelve a `generation`.
- **(c) Docker:** el entorno de esta sesión se detuvo dos veces; no afecta al código.
- **(d) El PR #1**, abierto desde la rama `generation`, recibió la fusión; su CI (#95) está verde. No se ha tocado.
- **(e) Pendientes funcionales del PR #17:** la revisión profesional, la lista de equipamiento y Flutter.

**13. ¿Qué criterios profesionales debemos aprobar?**
- **A1** (plan aceptable);
- **A2** (umbral de C2-D + EF: con 4 planes de sesión corta, «≥ 80 %» equivale a «4 de 4». Se proponen las opciones a, b y c);
- **A3** (acuerdo);
- **A4** (D01/D02);
- **A5** (punto de anclaje);
- **A6** (número de evaluadores).

Todo está en `DECISIONES_PROFESIONALES_PENDIENTES.md`.

**14. ¿Qué falta para comenzar la integración con Flutter?**
- Tu autorización para modificar Flutter.
- Acceso a su repositorio, que no está en esta sesión.
- Decidir qué contratos se encienden primero:
  - duración estimada (`fitsense.api.duracion-estimada.enabled`);
  - NOFACT-0.1 (`fitsense.api.no-factible-estructurado.enabled`).
- Aprobar la lista de equipamiento unificada.
- La especificación de referencia es `preparacion_integracion_pr17/ESPECIFICACION_FLUTTER.md` (casos A–E).

**15. ¿Está el PR #17 preparado para la siguiente etapa?**
- **Sí:** actualizado, probado, con CI verde y en borrador.
- Queda a la espera de la evaluación profesional (tras aprobar A1–A6) y del trabajo móvil. No debe fusionarse ni activarse antes.

## Próximos pasos (cada uno requiere tu aprobación)

1. Confirmar en Railway la rama de despliegue.
2. Aprobar A1–A6 y elegir a los evaluadores. Enviar **solo** `revision_profesional/paquete_evaluador/`.
3. Autorizar el trabajo en Flutter.
4. Decidir el despliegue de `generation` (aplicaría V44 en Neon al arrancar) y, por separado, la activación de PLAN-V4-0.1.
