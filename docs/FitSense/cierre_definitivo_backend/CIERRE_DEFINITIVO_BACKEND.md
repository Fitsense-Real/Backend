# Cierre definitivo del backend

**Fecha:** 2026-11-02 · **Base:** `generation` 7c19703 · **PR de esta etapa:** Fitsense-Real/Backend#22, #23, #24, #25 y #26 · **PR #17:** borrador, sin fusionar.

> **Actualización tras la integración final (2026-11-02).** Los PR #22 a #26 están **fusionados** en `generation`, por ese orden; el último es `114eb89`. La regresión sobre el código integrado está en `INTEGRACION_FINAL_Y_CONGELAMIENTO.md`. La versión congelada es la etiqueta `backend-mvp-1.0`. Las respuestas de abajo se escribieron antes de fusionar; donde dicen «cuando se fusionen», esa condición ya se cumple.

**Qué hay en este documento:** las respuestas de la orden y los estados A/B/C. Cada respuesta remite al documento que tiene el detalle; aquí no se repite.

## Estados

| Estado | Significado | Situación |
|---|---|---|
| **A · Técnicamente listo** | El código hace lo que dice el contrato, con pruebas y regresión | **Sí.** #22–#26 fusionados; regresión sobre el árbol final de `generation` en verde (`INTEGRACION_FINAL_Y_CONGELAMIENTO.md` §5). La política histórica no cambia: 960 perfiles y REPLAY idénticos |
| **B · Listo para integrar con Flutter** | Contratos congelados, entorno local reproducible, guía y ejemplos | **Sí.** Contratos integrados en `generation`, etiqueta `backend-mvp-1.0`, perfil local seguro y documentación alineada con el código: `CONTRATOS_FINALES_PARA_FLUTTER.md` y `GUIA_EJECUCION_LOCAL.md`. No significa que Flutter se haya ejecutado |
| **C · Habilitado para personas reales** | Desplegado, con datos reales y decisiones de operación tomadas | **No.** Sigue pendiente de seguridad operativa, evaluación profesional y autorizaciones (abajo, e `INTEGRACION_FINAL_Y_CONGELAMIENTO.md` §7) |

**Bloquean C** (decisiones tuyas o de terceros, no trabajo de código pendiente de esta orden):
1. autorizar el despliegue y confirmar la rama en el panel de Railway;
2. la base de destino y su copia de seguridad;
3. la comprobación de salud;
4. la recuperación del planificador si se pierde el tick;
5. la revocación de sesiones;
6. el límite de peticiones por IP;
7. el proveedor de correo, o la recuperación manual por el equipo;
8. la progresión encendida o apagada;
9. la IA real (apagada salvo autorización);
10. los medios y su licencia;
11. quitar Neon como valor por defecto.

Ver `DECISIONES_PENDIENTES.md` D6–D13 y D16.

## Las 20 respuestas

**1. ¿Qué partes están completamente terminadas?**

Con `PLAN-HIST-1`, todo el flujo del MVP:
- registro e inicio de sesión;
- perfil (P-01, congelado tras el primer plan);
- generación con el motor de reglas;
- plan actual (por día local con #24);
- ejecución, omisión y adherencia;
- dolor (P-02 con reactivación, P-03);
- adaptación localizada con respaldo;
- progresión, implementada y apagada por defecto;
- reincorporación;
- NO_FACTIBLE estructurado (#22);
- errores coherentes (#23);
- perfil local seguro (#26).

La recuperación de contraseña (#25) está terminada **como código**, pero **no es operativa** sin proveedor de correo.

Detalle en `MATRIZ_CONTRATOS_API.md` e `IMPLEMENTACIONES_PENDIENTES_MVP.md` §1.

**2. ¿Qué contratos faltaban?**

Antes de esta etapa faltaban:
- NO_FACTIBLE con código (no solo texto);
- login fallido con 401;
- 400 coherentes;
- `/plan/current` en la zona del usuario;
- recuperación de contraseña;
- un perfil local seguro.

Están **todos implementados en PR**. Quedan **sin contrato**:
- los medios de los ejercicios: no hay archivos ni licencia; no se ha inventado ninguna URL;
- una comprobación de salud.

Los dos dependen de decisiones.

**3. ¿Qué se implementó y probó?**

| PR | Contenido | Pruebas | CI |
|---|---|---|---|
| #22 | NO_FACTIBLE estructurado | 9 de contrato + caso real C + V44_1 | ✅ |
| #23 | Errores y JWT | 6 IT | ✅ |
| #24 | Zona horaria | 3 IT con horario de verano | ✅ |
| #25 | Recuperación de contraseña | 5 IT + V44_2 | ✅ |
| #26 | Perfil local | 4 unitarias + humo 20/20 | ✅ |

Además, la regresión combinada de B y C (`REGRESION_CIERRE_BACKEND.md`).

**4. ¿Qué endpoints necesitan una decisión?**
- Medios: licencia y alojamiento. Los campos `gifUrl`/`imageUrl` son solo una propuesta (`MATRIZ_CONTRATOS_API.md`); no existen en el código.
- Recuperación de contraseña: proveedor de correo.
- Salud: Actuator u otra ruta.
- Cierre de sesión o revocación: diseño.
- Cambio de zona horaria tras el registro: posterior al MVP.

Ver `DECISIONES_PENDIENTES.md`.

**5. ¿La configuración local evita Neon?** **Sí, con el perfil `local-dev` (#26):**
- PostgreSQL local por defecto;
- IA apagada;
- `LocalProfileGuard` impide arrancar si la URL no es local, antes de abrir ninguna conexión: 0 líneas de Hikari o Flyway contra Neon o Railway en la prueba.

**Sin perfil**, `application.properties` sigue apuntando a Neon por defecto (sin contraseña, así que no conecta). Quitar ese valor por defecto es la decisión D10.

**6. ¿Qué riesgos de seguridad quedan abiertos?** Ninguno crítico demostrado tras #23. Abiertos y clasificados en `SEGURIDAD_Y_CONFIGURACION.md`:
- sin revocación de JWT (retirada, cambio de contraseña, cierre de sesión) [U];
- sin límite por IP [U];
- correo dentro del JWT [P];
- el registro revela si un correo existe [P];
- tres formatos de error en los filtros [P];
- desfases fijos aceptados como zona horaria [P];
- Neon como valor por defecto [U];
- sin comprobación de salud [U].

**7. ¿Las migraciones están verificadas?** **Sí, en bases aisladas:**
- V44, V44_1 y V44_2 (y V45 en el PR #17) sobre un esquema histórico con datos, sin alterar columnas, restricciones ni filas anteriores;
- reinicio sin cambios;
- `validate` de Hibernate correcto;
- *jar* en base limpia: 46 y 47 migraciones.

**Nada aplicado en Neon ni en Railway.** Ver `MIGRACIONES_Y_DESPLIEGUE.md`, incluido el conflicto de `SOLO_ESQUEMA` entre #22 y #25.

**8. ¿La política histórica sigue intacta?** **Sí:**
- `PLAN-HIST-1` y `allow-candidates=false` por defecto;
- ninguna política experimental activa;
- ningún cambio en la generación, el catálogo, el 1373, las dosis ni la dificultad.

**9. ¿Los 960 perfiles coinciden?** **Sí, byte a byte:**
- B frente a `generation` (761d67cf… / fa9fa3ed…);
- C frente a la referencia del PR #17 (c582fce2… / ee9d2111…; válidos 875 / 951 / 951 / 902 / 902; 48 perfiles EF con VEF).

**10. ¿REPLAY coincide?** **Sí**, en B y en C. Solo cambian las marcas de tiempo de `exclusiones_dolor.csv`, que se descartaron.

**11. ¿Cuál fue el último CI?**
- `generation` 7c19703: ejecuciones 116 y 117, verdes.
- #22 5740124, #23 a251798, #24 2114704, #25 3f31a4b y #26 65cf12f: verdes.
- PR #17 3a27f0b: verde (`verify`, 2026-11-02 17:42 UTC).

Las combinaciones B y C son locales; no tienen CI.

**12. ¿Qué pruebas se omitieron y por qué?** 36 en B y 57 en C, **todas opt-in**. Ninguna se omite por fallar.
- IA real (5): no autorizada.
- Neon de solo lectura (4): prohibido.
- P-2D (7): **ejecutadas aparte, 7/7 verdes** contra PostgreSQL efímero.
- Laboratorios (15 + 15 del PR #17): las comparaciones de 960, la campaña y el ciclo real se ejecutaron aparte.
- 2 que reproducen por SQL un pendiente ya resuelto (perfil congelado).

**No cuentan como aprobadas.** Tabla completa en `REGRESION_CIERRE_BACKEND.md` §4.

**13. ¿Qué necesita el PR #17?**
- La evaluación profesional: aprobar A1–A6, elegir evaluadores y autorizar el envío.
- Al actualizarse desde `generation`, dos ajustes ya probados en local: `MigracionV45IT` y `SOLO_ESQUEMA` (`ESTADO_PR17_Y_EVALUACION.md` §4 bis).

Recomendación: **no fusionarlo antes de la evaluación.**

**14. ¿Están aprobados A1–A6?** **No.**
- El formulario está preparado (`ESTADO_PR17_Y_EVALUACION.md` §2), pendiente de tu firma.
- La ampliación a 12 planes está preparada (`AMPLIACION_C2D_PROPUESTA_INTERNA.csv`).
- No se ha enviado nada.

**15. ¿Puede funcionar el backend con `PLAN-HIST-1`?** **Sí.** Ningún flujo del MVP depende del PR #17.

**16. ¿Qué falta para un despliegue de prueba seguro?**
1. Tu autorización y la rama confirmada en el panel de Railway.
2. Las variables de Railway (`MIGRACIONES_Y_DESPLIEGUE.md` §2).
3. Una rama de Neon separada, con una consulta previa de solo lectura de `flyway_schema_history`.
4. Una copia restaurable.
5. Las decisiones D8, D11 y D12.
6. Las verificaciones posteriores y el plan de recuperación (mismo documento).

**17. ¿Flutter está preservado?** **Sí:**
- bundle y parche verificados por hash, con el mismo árbol (5c4d7cec…);
- copia persistente en la rama `respaldo/flutter-mvp-integracion-api` (5e4c72c) de este repositorio;
- **no se ha subido al repositorio móvil**: el *push* dio 403 dos veces, y no se ha sobrescrito nada.

Restauración en `PRESERVACION_TRABAJO_FLUTTER.md`. **No se ha hecho trabajo móvil.**

**18. ¿Qué es realmente bloqueante?**
- Para A y B: nada. **Hecho:** #22–#26 fusionados y versión etiquetada.
- Para C: las decisiones listadas arriba.

**19. ¿Qué puede esperar a después del MVP?**
- Correo fuera del JWT.
- Formato de error único.
- Registro sin revelar cuentas.
- Cambio de zona horaria.
- Envío asíncrono de la recuperación.
- Medios, si se acepta el MVP sin imágenes.
- Las políticas del PR #17 (tras la evaluación).
- P-03 localizado, que sigue pospuesto.

**20. ¿Podemos congelar los contratos y empezar Flutter sin tocar más el backend?** **Sí: #22–#26 ya están fusionados y congelados en `backend-mvp-1.0`.**
- Esos cambios son aditivos y compatibles (`AdaptationOutcome` mantiene los 9 campos y el formato de error es el de siempre).
- Lo que queda pendiente para C no cambia ningún contrato que Flutter use: la salud, la revocación y el correo son de operación o aditivos.

Única salvedad: si decides mostrar medios, habrá que añadir `gifUrl`/`imageUrl`. Hoy no existen; serían campos **aditivos** y anulables, que no rompen el contrato. Flutter debe tratarlos como opcionales.

**Flutter sigue en pausa hasta tu siguiente autorización.**
