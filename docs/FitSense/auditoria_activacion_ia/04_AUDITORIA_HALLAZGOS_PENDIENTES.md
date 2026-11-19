# 04 · Hallazgos pendientes de la auditoría de reglas

Fuente: `docs/FitSense/mejora_ia/ronda2/AUDITORIA_REGLAS_FITSENSE.md` (rama `experimento/mejora-ia`, §4–§7).

**Estado:** ninguno se ha corregido en esta tarea. No hay una causa demostrada que los relacione con O12, V24 o H-G9.

| Id | Descripción original | Condición | Riesgo | ¿Afecta a la configuración activada? | ¿Tiene pruebas? | ¿Trabajo posterior? |
|---|---|---|---|---|---|---|
| H-A3 | Tope M residual: el texto de V2.2 ofrece retirar un relleno al acortar sin comprobar que la sesión resultante respete la M declarada | Prompt de adaptación V2.2/v2.4 | Bajo (calidad) | **No:** el prompt activo es V2.1. Con V2.1 queda el efecto ya medido: SIN_EFECTO evitable en ACORTAR (O18), que conserva la sesión sin riesgo | Medido en MEJ R2 y CONF-IA; sin prueba unitaria propia | Solo si se adopta v2.2a |
| H-G4 | V27 (tracción real en PULL) no se comunica al modelo en ninguna versión del prompt | REV2 activa V27 | Bajo (coste) | **Sí:** con PLAN-REV-2.0 una sesión PULL de la IA sin tracción real se rechaza y se reintenta. No se publica nada inválido | V27 está cubierta por las pruebas REV2 del validador; no hay prueba del prompt | Cambiar el prompt (prohibido aquí). Propuesta separada |
| H-G5 | En GENERAL_FITNESS ningún texto del prompt explica los slots (V23) | GENERAL_FITNESS con la IA | Bajo/medio (coste y calidad) | **Sí:** es la causa principal de los rechazos V23 en GENERAL_FITNESS (5/15 válidos a la primera con PR-1.7 en CONF-IA). El backend lo controla | V23 cubierta; prompt sin prueba | Propuesta de prompt separada |
| H-G8 | Fuera de GAIN_MUSCLE, V24 acepta hasta 3 series en principiantes; su justificación (llegar al mínimo de V20) ya no existe con REV2, que no rechaza por V20 | Principiantes fuera de GAIN_MUSCLE con REV2 | Bajo (carga) | **Sí:** con REV2 un principiante puede recibir 3 series donde la base es 2. Está dentro del rango del ACSM (1–3) que cita la política | `ProgresionV24V8CTest`, pruebas de capacidad | Decisión de política (`SERIES_MAX_AMPLIACION`) |
| H-G14 | V17 es redundante: el normalizador sustituye la duración declarada por la estimada antes de validar | Siempre | Ninguno | No cambia nada | Cubierta | No |

## Otros pendientes detectados en esta auditoría

| Id | Descripción | Riesgo | Impacto | Origen | ¿Trabajo posterior? |
|---|---|---|---|---|---|
| H-G9 | GENERAL_FITNESS intermedio o avanzado con 1×30–35 min: ninguna propuesta de la IA puede ser válida; se gastan hasta 3 llamadas antes del motor de reglas | Ninguno de seguridad | Coste (unos USD 0,07 por generación inicial de esos perfiles) y latencia | `03` | Comprobación previa o mínimo P-01 (decisión de política) |
| P-SC | La progresión no registra los ciclos en que no encuentra candidatos: O06 y O10 cumplieron dos semanas al 100 % sin paso ni bloqueo registrado | Ninguno de seguridad (no se añade trabajo) | Trazabilidad: el oráculo A7 no puede distinguir «sin candidatos» de «no evaluado» | `02` | Registrar `PROGRESION_SIN_CANDIDATOS` (cambio de código, sin cambiar la política) |
| API-SCV | La API muestra `SIN_CAMBIOS_VALIDOS` (sesión `SIN_EFECTO`) cuando la propuesta de la IA no tiene efecto, aunque el backend sí sustituyó ejercicios por dolor | Ninguno de seguridad: el plan publicado excluye lo que dolió | Presentación: el usuario o el evaluador pueden entender que no se hizo nada | `01` §5 | Ajustar la etiqueta del resultado cuando hay sustituciones del backend |

Clasificación conjunta, de mayor a menor prioridad:
1. **Coste:** H-G9, H-G5, H-G4.
2. **Trazabilidad y presentación:** P-SC, API-SCV.
3. **Calidad:** H-A3 (solo si se adopta v2.2a).
4. **Política:** H-G8.
5. **Sin riesgo:** H-G14.

Ninguno de estos hallazgos impide activar la configuración de forma segura. H-G4 y H-G5 explican parte del coste en reintentos de la generación inicial.
