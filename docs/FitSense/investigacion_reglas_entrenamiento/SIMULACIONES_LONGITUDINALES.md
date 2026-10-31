# Simulaciones longitudinales (6 semanas, ciclo semanal real)

**Datos:** `SIMULACIONES_LONGITUDINALES.csv`.
**Prueba:** `LaboratorioReglasIT#longitudinal`.

**Entorno:**
- PostgreSQL efímero, con el `WeeklyCycleService` real (P-02, P-03, V8C, D8, adaptación).
- IA apagada (doble), 0 llamadas a Replicate.
- Progresión **apagada** (configuración por defecto).

## Guion (igual para todos)

| Semana | Qué hace la persona sintética |
|---|---|
| S1 | Completa todo |
| S2 | Omite la primera sesión por **dolor** (P-02) |
| S3 | Omite dos sesiones por **motivación** (baja adherencia) |
| S4 | Completa con **RPE 9** (fatiga) |
| S5–S6 | Completa todo |

Se ejecuta dos veces:
- con el piso vigente de V20 (70 %);
- con el piso a 0 (alternativa D), cambiado **solo** en la base efímera.

## Resultados

| Caso | Perfil | Piso 70 %: semanas con plan | % de M | Series/sem del grupo grande menos cubierto | Piso 0 %: semanas con plan | % de M | Series/sem del grupo grande menos cubierto |
|---|---|:-:|:-:|:-:|:-:|:-:|:-:|
| T1 | GAIN_MUSCLE intermedio, 60 min | 6/6 | 72 | 9 | 6/6 | 48 | 6 |
| **T2** | **GAIN_MUSCLE intermedio, 75 min** | **0/6** | — | — | 6/6 | 38 | 5 |
| T3 | INCREASE_STRENGTH intermedio, 75 min | 6/6 | 72 | 18 | 6/6 | 43 | 6 |
| T4 | LOSE_WEIGHT intermedio, 75 min | 6/6 | 76 | 9 | 6/6 | 29 | **0** |
| T5 | GENERAL_FITNESS intermedio, 75 min | 6/6 | 72 | 9 | 6/6 | 45 | **0** |
| T6 | GENERAL_FITNESS principiante, 45 min, casa | 6/6 | 74 | 6 → 4 (tras el dolor) | 6/6 | 56 | **0** |

En ambos casos:
- P-02 queda activo: tras la S2, de 2 a 8 exclusiones vigentes por dolor de sesión.
- Las semanas S3–S4 se resuelven por RESPALDO o CONSERVED.
- No hubo ninguna publicación insegura.
- P-03 no llegó a actuar: no hubo NO_FACTIBLE por dolor.

## Lectura

1. **El perfil congelado de GAIN_MUSCLE a 75 min queda bloqueado.** Con la regla vigente, una persona con ese perfil no recibe ningún plan en 6 semanas. Por eso P-01 en 30–75 y V20 + 4D son hoy **incompatibles para GAIN_MUSCLE a 66–75 min**.
2. **Sin piso, el ciclo funciona, pero la semana se queda en la composición mínima del motor.** En LOSE_WEIGHT y GENERAL_FITNESS el grupo grande menos cubierto recibe **0 series** por semana. La adaptación y la conservación mantienen esa semana: no la mejoran, porque no hay ningún criterio de dosis que las empuje.
3. **Lo que no cambia con el piso:** la memoria del dolor, las sustituciones seguras y V8C se comportan igual. El piso solo afecta a la cantidad de contenido.
4. **Progresión y reincorporación** no se ejercitaron: la progresión está apagada por defecto y no hubo retiradas. Antes de implementar una política nueva habrá que repetir estas simulaciones con la progresión encendida (ver `RIESGOS_Y_REGRESIONES.md`).

## Límites

- Son usuarios **sintéticos** con un guion fijo. **No se infiere adherencia humana real** ni eficacia o seguridad clínica.
- Esto mide **factibilidad técnica y comportamiento del sistema**, no resultados físicos.
