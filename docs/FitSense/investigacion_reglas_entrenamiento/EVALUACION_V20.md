# Evaluación de V20 (mínimo del 70 % de la duración configurada)

## 1. Qué es y de dónde viene

**V20.** Cada sesión debe durar, según el estimador, al menos el 70 % de `session_minutes`, medido en segundos (`PlanDraftValidator.minimumSessionSeconds`).

Exenciones actuales:
- con una orden de reducción de volumen activa;
- con D8 (sesiones acortadas autorizadas, más la herencia de C-2).

**Origen (V12, `calculation_configs` MVP-1.1).** El comentario de la migración dice textualmente: «una sesión declarada de 45 minutos salió con 25». La regla nació para que la duración **declarada** fuera coherente con el contenido. La misma migración la marca **«SIN CALIBRAR»** y la deja configurable («se ajustan tras el piloto insertando una fila nueva»).

**Fuente científica.** Ninguna. No hay estudio que trate el «70 % del tiempo disponible» (`REVISION_CIENTIFICA_REGLAS.md`, §3 f).

**Respuesta a la pregunta de la orden.** El 70 % es una **decisión de diseño** para coherencia del producto, no una exigencia fundamentada en evidencia de entrenamiento. Por tanto se puede replantear.

## 2. Qué hace en la práctica

La matriz de experimentos tiene 912 perfiles dentro de P-01 (30–75 min); datos en `MATRIZ_EXPERIMENTOS.csv`.

| Efecto | Evidencia en el backend |
|---|---|
| **Es la única regla que obliga a poner contenido.** Sin ella, el motor arma su composición base mínima | `D_SIN_PISO`: sesiones al 25–57 % de M. GAIN_MUSCLE a 75 min: ~38 % (≈ 29 min). LOSE_WEIGHT a 75 min: 25–29 %, **0 series de fuerza**, 27–36 min de cardio por semana. Ningún plan de GAIN_MUSCLE con ≥ 10 series por semana en los cuatro grupos grandes |
| **Choca con el tope de V24.** Con dosis fijas y 6/7/8 colocaciones, la sesión más larga posible no llega al 70 % | GAIN_MUSCLE a 66–75 min: 0 de 57 perfiles con plan (`A_VIGENTE`); 6 semanas sin plan en la simulación T2 |
| **Obliga a rellenar.** Cuando el enfoque es estrecho (PUSH/PULL/LEGS con 6–7 días), llega al 70 % repitiendo el mismo grupo | 119 de 830 planes válidos (14 %) tienen una sesión con más de 11 series de un mismo grupo, hasta 21 (INCREASE_STRENGTH, 75 min, 6 días, pierna). Por encima de ~11 series fraccionales por sesión no se detecta beneficio adicional (preprint, [4]) |
| **Generó parches en el motor** que solo existen para satisfacerla | Series ampliadas hasta 3 «solo para V20», el tope de 7 en GAIN_MUSCLE (adenda 4D v3.1), la sustitución por duración (CASO A del PR #16), el relleno 3×13 |

## 3. Alternativas evaluadas (laboratorio)

En todos los casos cambia **solo** el piso, en la base efímera. Los topes de V24 y las dosis no cambian.

| Piso | Perfiles con plan (de 912) | GAIN_MUSCLE 75 | % de M (medio / mínimo) | Sesiones con > 11 series de un grupo | GAIN_MUSCLE con ≥ 10 series/sem en los grupos grandes |
|---|---:|---:|---|---:|---:|
| 70 % (vigente) | 830 | 0/57 | 78 / 70 | 14 % | 25 de 163 |
| 50 % | 902 | 53/57 | 65 / 50 | 7 % | 25 de 224 |
| 0 % (solo techo) | 912 | 57/57 | 57 / 25 | 4 % | 0 de 228 |
| 0 % + mínimo de volumen semanal (E) | 896 | 57/57 | 76 / 25 | 4 % | 98 de 225 |

**Lectura.**
- Bajar el porcentaje no resuelve el problema de fondo. El tiempo es un mal sustituto de la dosis.
  - Con el 70 % hay relleno redundante.
  - Con el 0 % hay infradosificación.
- Lo que la evidencia mide es **volumen por grupo y semana**: ≥ 10 series para hipertrofia (ACSM 2026; Schoenfeld 2017), un mínimo de ~4 (Iversen 2021) y minutos aeróbicos semanales (OMS 2020).

## 4. Conclusiones sobre V20

1. Como regla de **coherencia**, que lo declarado sea igual a lo calculado, hay que conservarla. Ya lo hace V17/V4 con la duración declarada frente a la estimada.
2. Como **piso de contenido**, el 70 % no tiene base y produce efectos indeseados (relleno o imposibilidad).
3. **Propuesta de sustitución** (para decidir):
   - el tiempo configurado pasa a ser un **techo** (V4 se mantiene);
   - el mínimo pasa a ser de **contenido**: cuota semanal de series por grupo según objetivo y nivel, y minutos aeróbicos semanales en LOSE_WEIGHT y GENERAL_FITNESS;
   - un mínimo absoluto por sesión (p. ej. ≥ 2 ejercicios, que ya existe) evita sesiones vacías.
4. **No se debe eliminar V20 sin su sustituto.** La fila `D_SIN_PISO` muestra que el motor, sin piso, prescribe por debajo de las dosis mínimas de la evidencia.
