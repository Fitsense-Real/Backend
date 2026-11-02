# Preparación de la evaluación profesional

**Estado:**
- Paquete **preparado y no enviado**.
- Ninguna respuesta recibida.
- Los criterios A1–A6 están pendientes de tu aprobación (`CRITERIOS_EVALUACION_A1_A6.md`).

## 1. Contenido del paquete (`docs/FitSense/revision_profesional/paquete_evaluador/`)

| Archivo | Contenido | Comprobación |
|---|---|---|
| `MUESTRA_PROFESIONAL_CIEGA.csv` | 34 planes con códigos entre P02 y P98; **681 filas**, una por ejercicio | 34 códigos distintos, todos presentes en la plantilla |
| `FICHAS_NO_FACTIBLE.csv` | 2 fichas, D01 y D02 | Perfil, sesión requerida, ejercicios disponibles y decisión del sistema |
| `PLANTILLA_RESPUESTAS_EVALUADOR.csv` | 36 filas (34 planes + D01 + D02) | **Vacía**: 0 filas con datos |
| `INSTRUCCIONES_EVALUADOR.md` | Escala 1–5/NS (c1–c12), pregunta de duración, e1–e7, fichas y G1–G5 | Ver §2 |

## 2. Comprobaciones de la orden

| Comprobación | Resultado |
|---|---|
| Estructura | Correcta: 4 archivos, con códigos coherentes entre la muestra, las fichas y la plantilla |
| Sin clave interna en el paquete | **Sí.** No aparecen nombres de política (`PLAN-HIST`, `C2-D`, `EF`, `V4-C`), ni «clave», ni «policy», ni identificadores de perfil (`R0…`), ni columnas de versión, origen o modelo. Las claves están **fuera** del paquete: `cierre_final_pr17/CLAVE_INTERNA_MUESTRA.csv` e `implementacion_controlada_c2d_v4c/CLAVE_MUESTRA_AMPLIADA_NO_ENTREGAR_AL_EVALUADOR.csv` |
| Instrucciones comprensibles | Sí: escala definida, una pregunta guía por criterio, notas sobre la ausencia de cargas y RIR, y forma de registro. **Dos mejoras de redacción posibles** (no aplicadas; el paquete no se modifica sin tu aprobación): <br>• decir «34 planes con códigos entre P02 y P98» en lugar de «P01–P99»; <br>• aclarar que «duración estimada» es una estimación del sistema |
| Criterios registrables antes de las respuestas | Sí: A1–A6 se pueden fijar con fecha y con la huella de §3 |
| La ficha del 1373, separada | **Sí:** `cierre_backend_pre_mvp/FICHA_TECNICA_1373.md`. No está en el paquete ni se menciona en él (0 apariciones de «1373» en los 4 archivos) |
| Datos personales | Ninguno: perfiles ficticios |

## 3. Huella del paquete (SHA-256, 02/11/2026)

| Archivo | SHA-256 |
|---|---|
| `FICHAS_NO_FACTIBLE.csv` | `0b31e9e9bbbe4e10f5dd83bf91a659008f08b6ef26842a329aafe1f32d3040cf` |
| `INSTRUCCIONES_EVALUADOR.md` | `fe01d3986e042a4d70dbac5f91958b7104fc2e6b0f301616e6f287c78d7f46e8` |
| `MUESTRA_PROFESIONAL_CIEGA.csv` | `754b72b9c0d4eab4ab55cf06baaecda37c1a58f427cbffa0b78f9889b995ccc8` |
| `PLANTILLA_RESPUESTAS_EVALUADOR.csv` | `0145930b53c68e65ec0a1199a2790f451a4fd353b847636132c6defa025c98c4` |

Si el paquete cambia antes del envío (por ejemplo, con las dos mejoras de redacción), se recalcula y se anota aquí.

## 4. Pasos pendientes (todos requieren tu decisión)

1. Aprobar A1–A6 y las reglas de procedimiento.
2. Elegir a **dos** profesionales independientes y acordar el plazo.
3. Decidir sobre las dos mejoras de redacción.
4. **Autorizar el envío**: mientras no se autorice, no se envía nada.
5. Al recibir las plantillas:
   - guardarlas sin cambios;
   - abrir la clave;
   - calcular A1–A6;
   - levantar el acta de la reunión de consenso.

**No se ha inventado ningún resultado y no se declara validado C2-D ni EF.**
