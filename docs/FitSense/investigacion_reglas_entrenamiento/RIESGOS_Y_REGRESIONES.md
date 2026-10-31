# Riesgos y regresiones por cambio

| Cambio | Qué podría romperse | Cómo se detectaría | Severidad |
|---|---|---|---|
| **Quitar el piso de V20 sin sustituto** | Infradosificación: sesiones del 25–57 % de M; LOSE_WEIGHT y GENERAL_FITNESS sin fuerza en el grupo grande menos cubierto; GAIN_MUSCLE < 10 series/semana | Matriz `D_SIN_PISO`; longitudinal T4–T6 | **Alta** (calidad de la prescripción) |
| Bajar V20 al 50 % | Sigue sin base científica; reduce el relleno a la mitad, pero no garantiza la dosis | Matriz `D_PISO_50` | Media |
| **Quitar el tope de V24 sin límite por sesión** | Concentración de volumen por grupo en un día; sesiones muy largas con 1–2 días | Matriz C y E: 13–21 colocaciones con 1–2 días | **Alta** (fatiga, redundancia) |
| Tope dinámico (B) | Hereda el relleno de V20; se cumple el tiempo, no la dosis | Matriz B | Media |
| Modelo combinado (E) | Reescritura del motor, del validador, del prompt de la IA y de V8C (las semanas derivadas se comparan con la medida) | Toda la batería (939 unitarias + 458 de integración), campaña de 100 semanas, nueva matriz | **Alta** técnica |
| Cambiar V20 o V24 en el validador | Las semanas **históricas** se validaron con las reglas antiguas. Revalidarlas con las nuevas daría resultados distintos | `calculation_configs` versionadas; revalidación histórica | Media: hay que versionar la regla, no editarla |
| Cambiar las dosis de 4D | La comparación del estudio (IA frente a reglas) cambia de base a mitad del estudio | Campañas, métricas por `calculation_version` | Alta (validez del estudio) |
| Controles de volumen semanal | P-02 y P-03 excluyen ejercicios y pueden dejar un grupo sin cuota → aumentan los NO_FACTIBLE | Matriz con exclusiones; `PoliticasP123IT` | Media |
| Progresión con controles nuevos | D-P3 (+2 repeticiones, hasta 3 series) puede chocar con techos de volumen por sesión o semana | `ProgresionIT`, campaña con progresión encendida | Media |
| Reincorporación | El retorno escalonado podría quedar por debajo de la cuota semanal mínima → hace falta una exención explícita, como D8 | `ReincorporacionIT` | Media |
| Perfiles congelados | Un perfil congelado de GAIN_MUSCLE a 66–75 min no tiene plan hoy. Cualquier política nueva debe resolverlo **sin modificar el perfil** | Longitudinal T2 | **Alta** (usuarios bloqueados) |

## Pruebas necesarias antes de implementar una política nueva

1. Versión nueva de `calculation_configs` (o de la política) y revalidación de semanas históricas con **su** versión.
2. Matriz completa (`LaboratorioReglasIT`) con la regla nueva **en el validador real**, no en post-proceso.
3. Que los 830 planes válidos actuales sigan siendo válidos, o la lista justificada de los que cambian.
4. Campaña de 100 semanas en REPLAY y comparación con la evidencia actual.
5. Longitudinal con progresión encendida, dolor, ACORTAR y reincorporación.
6. Pruebas de la IA (prompt y contrato) sin coste: la IA debe recibir las mismas reglas que valida el backend.
7. Revisión por un profesional del ejercicio de una muestra de planes. **No se declara validación humana sin evaluadores humanos.**
