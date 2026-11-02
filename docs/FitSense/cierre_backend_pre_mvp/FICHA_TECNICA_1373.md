# Ficha técnica: ejercicio 1373 «Círculos de muñeca»

**Uso:** revisión profesional **posterior**, separada del paquete ciego. **No forma parte de `revision_profesional/paquete_evaluador/`** y no se envía con él.

**Origen de los datos:** el laboratorio `AnalisisSustitucion1373IT` (*opt-in*), con evidencia en `cierre_respaldo_y_preparacion_mvp/evidencia/analisis_1373.md` y `candidatos_1373.csv`.

## 1. Atributos

| Atributo | Valor |
|---|---|
| Id | 1373 |
| Nombre | Círculos de muñeca |
| Zona (`body_part`) | lower arms |
| Músculo objetivo | forearms |
| Equipamiento | body weight |
| Dificultad | 1 |
| Tipo de prescripción | **SETS_REPS** |
| Patrón | MOBILITY |
| Clase de slot | relleno |
| Complemento | **true** (complemento de movilidad para GENERAL_FITNESS) |

**Singularidad:** es el **único** complemento de movilidad con prescripción por repeticiones. Los otros 27 complementos de movilidad son por tiempo (DURATION); entre ellos está 1178 «Estiramiento de muñeca lateral», de la misma zona.

## 2. Por qué no hay sustituto (regla SUST-SEG-1)

**Qué exige la regla:** que el sustituto tenga el mismo slot, el mismo tipo de prescripción y la misma condición de complemento, y que su zona admita el enfoque de la sesión. Además, que no esté en la sesión ni en un día contiguo y que cumpla V22 y la revisión.

**Embudo (caso C1, perfil real, 731 ejercicios elegibles):**

| Filtro | Quedan |
|---|---:|
| F1. Distinto del original | 731 |
| F2. Misma clase de slot (relleno) | 418 |
| F3. Mismo tipo (SETS_REPS) | 351 |
| **F4. Misma condición de complemento (true)** | **0** |

Con el catálogo activo entero, sin filtro de perfil (1.219 ejercicios), el resultado es el mismo: **0 tras F4**.

## 3. Candidatos descartados más cercanos

**Fallan un solo filtro.** Con el perfil real fallan F3 21 candidatos y F4 197; en el catálogo entero, 27 y 332.

| Candidato | Falla | Por qué no se usa |
|---|---|---|
| 1178 Estiramiento de muñeca lateral (lower arms, DURATION, MOBILITY, complemento) | F3, tipo | Usarlo exigiría **una equivalencia entre repeticiones y segundos, que no está autorizada** |
| Otros estiramientos y movilidad por tiempo (p. ej. 67, 881, 917, 1375) | F3, tipo | Igual que 1178, y además son de otra zona |
| Ejercicios de muñeca y antebrazo por repeticiones: 95, 133 (banda); 534, 613–616, 653–659 (mancuerna); 173, 174, 213, 214, 256–258 (barra); 373, 413, 469 (polea)… | F4, complemento | Son trabajo **resistido** de antebrazo, no complementos de movilidad. Muchos requieren material que el perfil no tiene |
| Movilidad de cadera y tronco por repeticiones, no complemento (p. ej. 68, 254, 884) | F4, complemento | Otra función y otra zona |

## 4. Motivo del rechazo (texto de V32, caso C1)

> SEGURIDAD: el ejercicio 1373 de la sesión (2026-11-13) dejó de ser compatible por seguridad (dolor_registrado) y no hay sustituto elegible con la regla SUST-SEG-1: mismo slot (relleno), tipo SETS_REPS y zona que admita el enfoque FULL_BODY. Requiere intervención; no se publica una sustitución insegura.

## 5. Decisión vigente y preguntas para la revisión

**Decisión vigente:**
- se mantienen SETS_REPS, las equivalencias, SUST-SEG-1 y el catálogo **sin cambios**;
- si el 1373 duele y no hay sustituto, el resultado sigue siendo **NO_FACTIBLE** (salvo que P-03 nivel 2 encuentre una semana válida).

**Preguntas para el profesional** (no se han enviado):
1. ¿Es apropiado prescribir los círculos de muñeca por repeticiones, o deberían ir por tiempo, como los demás complementos de movilidad?
2. Si alguien siente molestias en este ejercicio, ¿es aceptable sustituirlo por 1178 «Estiramiento de muñeca lateral» (por tiempo), o es preferible retirarlo sin sustituto?
3. ¿Debe un complemento de movilidad con molestias impedir que se publique la semana, o basta con retirarlo?
