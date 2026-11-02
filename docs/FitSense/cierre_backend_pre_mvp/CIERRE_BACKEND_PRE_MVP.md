# Cierre del backend pre-MVP

**Fecha:** 02/11/2026.

**Orden:** «FitSense — orden de cierre del backend y avance hacia el MVP funcional».

## Documentos

| Documento | Contenido |
|---|---|
| `FUSION_PR21.md` | Las 9 comprobaciones, las invariantes y la fusión del PR #21 (7c19703) |
| `ACTUALIZACION_PR17.md` | Merge de `generation` en el PR #17 (51a311f), sin conflictos y sin cambios en `src/main` |
| `REGRESION_BACKEND_FINAL.md` | Resultados aprobados, fallidos y omitidos por separado; comparación histórica |
| `DECISIONES_P03_1373.md` | P-03 localizado pospuesto, 1373 sin cambios, NO_FACTIBLE confirmado |
| `FICHA_TECNICA_1373.md` | Ficha del 1373 para la revisión posterior, **fuera** del paquete ciego |
| `CRITERIOS_EVALUACION_A1_A6.md` | A1–A6 con la redacción de la orden, **pendientes de tu aprobación** |
| `PREPARACION_EVALUADORES.md` | Comprobación del paquete y su huella SHA-256 (**no enviado**) |
| `ESTADO_BACKEND_MVP.md` | Qué ofrece el backend al MVP, verificado desde la app |

**Documentos de Flutter**, en el repositorio móvil (`docs/mvp/`):
- `INVENTARIO_ESTADO_FLUTTER.md`;
- `MAPEO_PANTALLAS_API.md`;
- `MODELOS_Y_CONTRATOS.md`;
- `PRUEBAS_INTEGRACION_MVP.md`;
- `PENDIENTES_MOVIL.md`.

## Respuestas (fase 9)

### Backend

1. **¿Se fusionó el PR #21?** Sí, tras las 9 comprobaciones, con el CI verde en e033025 y sin despliegue.
2. **¿Qué commit quedó en `generation`?** **7c19703**.
3. **¿CI?** `generation` 7c19703 verde (ejecuciones 116 y 117). PR #17 verde en 51a311f (118) y en 7baeb66.
4. **¿Se actualizó el PR #17?** Sí, por **merge** (51a311f), sin conflictos. Sigue **en borrador y sin fusionar**.
5. **¿Se conservaron las políticas y los contratos?** Sí:
   - V44 y V45, C2-D, EF-0.1, LW-PREF-0.2, REP-SUST-0.1 y NOFACT-0.1 intactos;
   - `src/main` sin cambios;
   - `PLAN-HIST-1` por defecto, `allow-candidates=false` y los indicadores apagados.
6. **¿Pruebas aprobadas y omitidas?**
   - unitarias: 1.046/1.046;
   - integración: 441 aprobadas, 0 fallidas, **57 omitidas** (*opt-in* o de IA real);
   - 4 baterías: 223 aprobadas y 2 omitidas cada una;
   - ciclo real: 6/6;
   - 960 perfiles: 1/1;
   - campaña REPLAY: 1 aprobada y 1 omitida (LIVE).

   **Las omitidas no se cuentan como aprobadas.**
7. **¿Resultado histórico idéntico?** **Sí.**
   - 960 perfiles × 5 políticas, byte a byte: `c582fce2…` / `ee9d2111…`; válidos 875/951/951/902/902; 48 perfiles EF.
   - REPLAY idéntica, salvo dos marcas de tiempo, que se descartaron.
8. **¿Comportamiento de P-03?** Nivel 1 (SUST-SEG-1) y nivel 2 (regeneración de la semana con reglas) **sin cambios**:
   - las invariantes A, B y C se cumplen;
   - nunca se publica una rutina inválida;
   - con dolor y sin solución segura, el resultado es **NO_FACTIBLE**, sin plan y con la exclusión activa.
9. **¿Bloqueos?**
   - En el backend, **ninguno**.
   - Pendiente de decisión: un entorno de pruebas accesible desde el teléfono (requiere tu autorización de despliegue).

### Funcional

10. **¿P-03 localizado es solo una propuesta?** **Sí.** Diseño, prototipo de medición y comparación; sin implementación.
11. **¿Se mantienen las reglas del 1373?** **Sí.** SETS_REPS, SUST-SEG-1, equivalencias y catálogo sin cambios. Su ficha está separada del paquete ciego.
12. **¿NO_FACTIBLE funciona como se planeó?** **Sí.** Lo fijan `RespaldoConDolorIT` (caso C) e `InvariantesRespaldo.sinPublicar`. La app lo muestra con un mensaje no alarmista y el detalle técnico plegado.
13. **¿A1–A6 listos?** **Preparados para tu aprobación** con la redacción de la orden. El paquete está comprobado: sin clave y con su huella. **No se ha enviado.**
14. **¿Qué evaluación falta?** Toda la profesional:
    - aprobar A1–A6;
    - elegir a 2 profesionales;
    - autorizar el envío;
    - recibir las respuestas;
    - celebrar el consenso.

    **Ninguna respuesta registrada; C2-D y EF no están validados.**

### Flutter

15. **¿Acceso al repositorio?**
    - Lectura: **sí** (`FitSense-2-0/mobileapp`).
    - **Escritura: no.** El *push* devolvió 403 porque la app de GitHub de Claude no tiene acceso a esa organización.
    - El commit fdc9439 de la rama `feature/mvp-integracion-api` está guardado como *bundle* y como *patch* para que lo subas tú.
16. **¿Pantallas implementadas?** Splash, login, registro y recuperación (ya existían), dashboard (maqueta) y, nuevas:
    - perfil;
    - plan semanal;
    - detalle de sesión con omisión;
    - entrenamiento en curso;
    - progreso (adherencia, cambios y dolor).
17. **¿Conectadas?** **Todas**, a los contratos estables de `generation`. Se corrigieron el login, el registro y `users/me`, que no coincidían con el backend, y se quitaron del dashboard los datos inventados.
18. **¿Probadas con el backend real?** **La capa de datos sí:** 1 prueba de integración real contra el backend local recorre la primera ruta del MVP. **La interfaz no:** no hay emulador ni dispositivo en el entorno.
19. **¿Qué solo se probó con simulaciones?**
    - el flujo de autenticación unitario;
    - la reincorporación por dolor;
    - los mensajes de adaptación con datos reales de cierre semanal (`ADAPTACION_APLICADA`, `REGENERACION_SEGURA_*`, `NO_FACTIBLE`).
20. **¿Qué contratos faltan?**
    - recuperación de contraseña;
    - motivo estructurado de NO_FACTIBLE en la adaptación (solo diseño);
    - URL pública de los medios;
    - envío de la zona horaria (la app no la detecta todavía);
    - duración estimada por contenido (PR #17, apagada; no se usa).
21. **¿Qué bloquea la primera ruta del MVP?**
    1. El acceso de escritura al repositorio móvil, o que subas tú el *bundle*.
    2. Ejecutar la interfaz en un emulador o teléfono.
    3. Un backend accesible desde el teléfono (en local, IP de la LAN; si no, un despliegue de pruebas que **requiere tu autorización**).

## Condición final

| Condición | Estado |
|---|---|
| PR #21 integrado | Sí (7c19703) |
| PR #17 actualizado, en borrador y verde | Sí |
| Sin cambios de producción | Sí: sin despliegue, sin migraciones en Neon, sin Railway, sin Replicate |
| P-03 localizado pospuesto | Sí |
| NO_FACTIBLE conservado | Sí |
| A1–A6 preparados, sin resultados inventados y sin paquete enviado | Sí |
| MVP de extremo a extremo | Capa de datos probada contra el backend real; interfaz y *push* pendientes |
| Despliegue, activación de políticas o publicación del MVP | **Ninguno** |
