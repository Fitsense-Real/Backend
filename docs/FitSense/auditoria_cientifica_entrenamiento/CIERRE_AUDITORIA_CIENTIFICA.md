# Cierre de la auditoría científica y del modelo E

**Rama:** `feature/modelo-e-planificacion`, a partir de `generation` (`afef98a`).
**No se ha fusionado, activado ni desplegado nada.**

## 0. Alcance y límites

- **Evidencia:** 53 fuentes, 51 leídas como resumen y 2 solo como referencia.
  - **No se leyó ningún texto completo**: los hosts de PMC, PubMed, doi.org, LWW, Springer y OMS están bloqueados por la política de red.
  - Ninguna conclusión se presenta como verificada en los métodos y resultados.
  - 20 DOI no aparecieron en las fichas vistas: están marcados NO en `ESTUDIOS_SELECCIONADOS.csv`.
- **Especificaciones 4A v5, 4C §5, 4D v2/v4 y §19–20:** no están en el repositorio y no se reconstruyen. Se auditan el código, las migraciones, las adendas y las pruebas.
- **Evaluación humana:** no hubo ninguna.
- **Pruebas:** regresión 944 unitarias y 462 IT (36 omitidas), 0 fallos. Campaña REPLAY idéntica, con 0 llamadas a Replicate.

## 1. Entregables (18)

| # | Archivo |
|---:|---|
| 1 | `PROTOCOLO_REVISION_CIENTIFICA.md` |
| 2 | `ESTUDIOS_SELECCIONADOS.csv` |
| 3 | `REVISION_CIENTIFICA_EXHAUSTIVA.md` |
| 4 | `AUDITORIA_4A_OBJETIVOS.md` |
| 5 | `AUDITORIA_4C_PATRONES.md` |
| 6 | `AUDITORIA_4D_DOSIS.md` |
| 7 | `AUDITORIA_POR_OBJETIVO.md` |
| 8 | `TRAZABILIDAD_REGLAS_EVIDENCIA.csv` |
| 9 | `REGLAS_CONSERVABLES_Y_BRECHAS.md` |
| 10 | `REFERENCIAS_APA7.md` |
| 11 | `AUDITORIA_LABORATORIO_E.md` |
| 12 | `COMPARACION_TRES_CAMINOS.md` |
| 13 | `MATRIZ_EXPERIMENTOS_REVISADA.csv` (y `SIMULACIONES_LONGITUDINALES_REVISADAS.csv`) |
| 14 | `PROPUESTA_POLITICA_FUNDAMENTADA.md` |
| 15 | `ARQUITECTURA_E_CANDIDATA.md` |
| 16 | `SIMULACIONES_Y_REGRESIONES.md` |
| 17 | `DECISIONES_QUE_REQUIEREN_APROBACION.md` |
| 18 | `CIERRE_AUDITORIA_CIENTIFICA.md` |

## 2. Respuestas a las 25 preguntas

### 1. ¿Es defendible científicamente la estructura de GAIN_MUSCLE?

**Sí.**
- La hipertrofia es la principal y la fuerza la secundaria (B).
- Las dosis 3×10/90 y 3×13 son B.
- Los rellenos, los topes 6/7 y el 85 % son C.
- Su único defecto es operativo: no hay plan a 66–75 min.

### 2. ¿Y la de INCREASE_STRENGTH?

**Sí.**
- 2×8/120 y 3×5/150 son B.
- Los 4 patrones con fuerza son B.
- El control de intensidad (último peso y RPE de sesión) es **D**.
- El 3×5 se prescribe como 3×6 por el mínimo de repeticiones. Es una incoherencia de ingeniería, no un problema científico.

### 3. ¿Y la de LOSE_WEIGHT?

**La estructura sí, la dosis aeróbica total no.**
- Cardio como principal, fuerza como secundaria y bloques de 3×90–120 s son B.
- La dosis aeróbica total es **E**: en el laboratorio va de 9 a 120 min/semana, con media de 42, y **ningún plan llega a 150**, frente a la referencia de 150–300 min [S04, S05].

### 4. ¿Y la de GENERAL_FITNESS?

**Sí.**
- El esquema multicomponente, la resistencia muscular y la movilidad son A.
- Los isométricos de 40 s y el equilibrio oportunista son C.

### 5. ¿Qué capacidades y patrones tienen respaldo suficiente?

- **Respaldo directo (A):**
  - el esquema multicomponente [S34];
  - la resistencia muscular de más de 15 repeticiones con menos de 90 s [S06];
  - la movilidad de 60 s por ejercicio [S34, S35].
- **Respaldo compatible (B):** la hipertrofia en un rango amplio de cargas cerca del fallo [S48], la fuerza con carga relativa [S46] y el aeróbico para el peso [S26–S28].
- **Los 4 patrones** tienen respaldo **indirecto** (B): cubrir los grupos grandes ≥ 2 d/semana y priorizar los multiarticulares. La taxonomía en sí no tiene respaldo.

### 6. ¿Qué distribuciones semanales son razonables?

- **2–3 días** en cuerpo completo o superior/inferior; **4** en superior/inferior; **5–6** en PUSH/PULL/LEGS (B). Con el volumen igualado, la división y el cuerpo completo dan resultados similares [S23].
- **1 día** no cumple ≥ 2 d/semana por grupo, pero es una elección del usuario.
- **V18** (sin días consecutivos para el mismo grupo grande) es una C conservadora.

### 7. ¿Qué dosis se pueden conservar?

**Todas las dosis 4D.** Ninguna dosis individual es D ni E.

### 8. ¿Qué descansos deben revisarse?

- **Ninguno obligatoriamente.**
- Opcional: permitir 180 s en los básicos pesados de intermedio y avanzado, que hoy tienen 150 s [S06, S08]. Es la decisión D-04.

### 9. ¿Qué valores son decisiones de ingeniería y no científicas?

- **Duración y tiempo:** 70 % (V20), ×1,15 (V4), 85 %, transición de 60 s.
- **Topes:** 6/7/8 colocaciones.
- **Recuperación y validación:** V18, mínimos (2/2/6/20 s), mínimos por zona.
- **Adaptación:** banda del 60–125 %.
- **Dosis:** isométricos de 40 s y pausa de movilidad de 15 s.
- **Modelo E:** todos sus parámetros.

### 10. ¿Hay evidencia que contradiga alguna regla importante?

**Solo la dosis aeróbica semanal de LOSE_WEIGHT** [S04, S05, S32].

### 11. ¿Qué reglas tienen riesgos reales?

- **Eficacia:** LW2.
- **Operativo:** V20 × V24 en GAIN_MUSCLE a 66–75 min, sin plan.
- **Calidad de la dosis:** sin RIR, la intensidad de la fuerza no se controla (IS7).
- **Coherencia:** V4 `floor` frente a `round` (+1 min a 30 y 45).

**Ninguna regla supone un riesgo de seguridad demostrado.**

### 12. ¿Qué cambios son estrictamente necesarios?

**Ninguno por seguridad.** Por eficacia y factibilidad:
1. medir los minutos aeróbicos y avisar en LOSE_WEIGHT (D-06a);
2. una solución para GAIN_MUSCLE a 66–75 min (C2, D-02).

Los dos están **pendientes de aprobación**.

### 13. ¿Qué cambios serían mejoras opcionales?

- 180 s en básicos pesados.
- Registrar el RIR de la última serie.
- Resolver el 3×5 frente a 3×6.
- Cambiar el texto «alta intensidad».
- Fortalecer todos los grupos grandes en LOSE_WEIGHT.
- Garantizar una dosis mínima de equilibrio.
- Métricas de volumen semanal por grupo.
- Unificar V4.

### 14. ¿Cuánto del diseño actual puede conservarse?

- **41 de las 42 reglas** auditadas (A, B, C y D sin cambio automático).
- **El 100 % de 4A, de 4C y de las dosis 4D.**
- C2 deja idénticos **925 de 960** planes.

### 15. ¿Se pueden resolver V20 y V24 con cambios mínimos?

**Sí.** C2 solo baja el piso de V20 cuando V20 es la única regla incumplida, sin tocar V24.
- Planes válidos: 875 → 950.
- GAIN_MUSCLE a 75 min: 0 → 56/60, con un piso del 55 %.

### 16. ¿Es realmente necesario el modelo E completo?

**No.**
- Con el V24 vigente, E es válido en menos perfiles que C1: 781 frente a 875.
- Exigiría sustituir V20 **y** V24, migrar, versionar, adaptar la IA y montar una batería espejo.
- Se queda como laboratorio desactivado.

### 17. ¿Qué parámetros de volumen semanal son defendibles?

Son **rangos de inferencia, no normas**:

| Objetivo | Series por grupo y semana |
|---|---|
| GAIN_MUSCLE | 6–10 (principiante), 10–16 (intermedio), 12–20 (avanzado) |
| INCREASE_STRENGTH | 4–6 series pesadas por patrón (principiante); 6–10 (resto) |
| LOSE_WEIGHT y GENERAL_FITNESS | 4–10 por grupo, ≥ 2 d/semana |

El techo por sesión procede solo de un preprint [S03]: como mucho justifica un aviso.

### 18. ¿Qué controles de intensidad se pueden aplicar con los datos actuales?

- La progresión de carga hasta +10 % (V11) y el no subir carga y volumen a la vez.
- El RPE de sesión (≥ 8, alto) en la tabla de decisión.
- Los límites de repeticiones.

**No hay RIR por serie ni intensidad del cardio**, que exigen datos nuevos (D-05).

### 19. ¿Qué pasa con GAIN_MUSCLE a 75 min?

- **C1:** 0/60. Es infactible: con 7 colocaciones el tope ronda 2.739 s y el piso es 3.150 s.
- **C2:** 56/60, con un piso del 55 %.
- **C3:** 60/60 generados, pero 22 incumplen el tope o el presupuesto de V24.
- **Longitudinal:** P2 (piso del 70 %) no tiene plan en ninguna de las 8 semanas; P2m (55 %) tiene plan en todas.

### 20. ¿Se corrigieron los errores del laboratorio?

**Sí** (`AUDITORIA_LABORATORIO_E.md`).
1. El exceso del 115 % venía de V4 `floor`/`round`, del redondeo de los minutos declarados y de que E rellenaba hasta el techo.
2. E ahora planifica hasta M. Se añadieron las columnas de V4 estricto y del máximo por sesión: **0 excesos en todos los caminos**.
3. La cifra 713 frente a 722 era un error aritmético del informe.
4. **No se acepta 896 como prueba**: la cifra reconciliada es 781 válidos más 178 experimentales.

### 21. ¿Qué efectos aparecen en adaptación y progresión?

- La progresión es pequeña (~3 repeticiones por semana completa).
- **Nunca sube con dolor ni con fatiga.**
- P-03 sale PUBLICADO en P3 y P4, y NO_FACTIBLE en P5 (S4), que se regenera en S5.
- LACK_OF_TIME parcial dio CONSERVAR.
- **No se ejercitaron** ACORTAR ni la reincorporación en el longitudinal; están cubiertos por la batería existente.

### 22. ¿Qué riesgos de regresión quedan?

- Con C2, sesiones más cortas que lo declarado en 79 perfiles.
- Con E, divergencia entre la IA y el validador, y cambio de volumen generalizado.
- Las semillas dependen del orden de `user_id`.
- Los laboratorios opt-in no se ejecutan en la CI.

### 23. ¿Cuáles son las limitaciones científicas?

- Solo resúmenes; ningún texto completo.
- Cifras del ACSM 2026 y de Pelland 2026 obtenidas de fuentes secundarias.
- Un preprint como única fuente del techo por sesión.
- Ninguna fuente primaria para la regla de 48 h.
- Ningún dato de población peruana.
- Usuarios sintéticos: se mide factibilidad, no eficacia ni adherencia.
- Series directas sin conteo fraccional.

### 24. ¿Qué debería revisar un profesional del ejercicio?

1. La dosis aeróbica de LOSE_WEIGHT y cómo alargarla.
2. El suelo aceptable de duración por objetivo, si se aprueba C2.
3. 180 s en básicos pesados.
4. El RIR por serie y cómo registrarlo.
5. El fortalecimiento ≥ 2 d/semana en LOSE_WEIGHT.
6. La dosis de equilibrio en GENERAL_FITNESS.
7. La regla de días consecutivos.
8. Los rangos de volumen semanal de §17 antes de usarlos como avisos.

### 25. ¿Qué decisión recomienda el equipo y por qué?

**Conservar 4A, 4C y 4D íntegros. Preparar C2 y la métrica de minutos aeróbicos como nueva versión de política, desactivada y versionada, para aprobación. Mantener E como laboratorio desactivado.**

**Por qué:**
- La evidencia no contradice ninguna estructura ni dosis, salvo la suma aeróbica de LOSE_WEIGHT.
- C2 resuelve el único fallo operativo con el menor cambio: 925/960 planes idénticos, sin tocar V24, V4 ni P-01.
- E no es necesario y, con las reglas vigentes, es menos factible.

**Requisitos antes de activar nada:**
- revisión profesional;
- acceso a los textos completos (D-12);
- la aprobación final del responsable.
