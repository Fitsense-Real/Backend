# Recomendación final

## Resultado

**Clasificación: PARCIAL**, con la regla congelada antes de ejecutar.

| | Aplicadas / 8 | Sesiones factibles cambiadas / 10 | Rechazos | Llamadas | Coste |
|---|---:|---:|---:|---:|---:|
| C1 (vigente) | 6 y 5 | 7 y 5 | 7 y 8 | 15 y 15 | US$ 0,22 y 0,23 |
| C2 (V2) | **7 y 7** (el techo) | **10 y 10** | **0 y 0** | **8 y 8** | **US$ 0,12 y 0,12** |

**Seguridad:** 0 violaciones en las 4 corridas.

**Gasto total del experimento:** 46 predicciones y US$ 0,69, frente a un tope de 120 y USD 5.

## Respuestas a las dos preguntas

- **P1. ¿La IA propone cambios inválidos porque no entiende las restricciones?**
  - **Sí, en buena parte por falta de información cuantitativa y por un mensaje de reintento que no se puede cumplir.** Entiende los permisos: 0 rechazos por permisos en la etapa C.
  - Cuando se le dan el margen, el coste en segundos de cada cambio y una pista de reintento coherente, **los rechazos pasan de 7–8 por corrida a 0**, con los mismos validadores.
- **P2. ¿Hay sesiones imposibles de modificar con utilidad?**
  - **Sí, dos:** U03 S15 y U04 S21, por la combinación de GAIN_MUSCLE con una intervención que solo permite cambiar la dosis. 4D no admite reducciones y no hay carga prescrita.
  - Las dos condiciones las omiten. **No se arregla con el prompt:** es un límite del diseño. Cambiar 4D o la intervención es una decisión tuya, fuera de esta orden.

## Recomendaciones (ninguna aplicada; todas pendientes de tu revisión)

1. **Mantener V2 detrás de su indicador, apagado por defecto, y no fusionar todavía.**
   - Las razones para adoptarlo son sólidas: el techo alcanzado en las 2 corridas, 0 rechazos, la mitad de coste y 0 violaciones.
   - Pero la evidencia es exploratoria (n = 8 × 2) y el resultado formal es Parcial.
   - Antes de activarlo, valorar una tercera corrida por condición, u otra semana del ciclo, siguiendo el mismo protocolo.
2. **Si se decide activarlo, hacerlo solo cambiando la configuración** (`fitsense.ai.adaptation.prompt-version=v2`). El código ya está y V1 queda intacto. Basta con revertir el valor para volver atrás.
3. **No tocar los validadores.** El experimento muestra que el problema estaba en el contexto, no en las reglas: V20 y V8C rechazaron lo que debían en las dos condiciones.
4. **Revisar aparte (fuera de esta orden):**
   - **(a)** las 2 sesiones F3: si REVISAR_CARGA o INVESTIGAR en GAIN_MUSCLE deben permitir algo, o si el sistema debe saltarse la llamada a la IA cuando la factibilidad dice que no hay solución (ahorraría 1 llamada por caso);
   - **(b)** P4 de PR #28, sobre el músculo objetivo en las sustituciones (6 casos en estas corridas);
   - **(c)** las sesiones que quedan exactamente en el mínimo de V20.
5. **Short paper: no se ha modificado.** Si se usa este resultado, debe presentarse como exploratorio. No se atribuyen mejoras de adherencia, conductuales ni clínicas; solo técnicas: aplicación, rechazos y coste.

## Lo que no se ha hecho (según la orden)

- No se ha modificado ningún validador (V20, V24, V8C), ni el mínimo de duración, las políticas, el catálogo, las dosis ni la dificultad.
- No se ha activado C2-D/EF.
- No se ha fusionado PR #17, PR #28 ni el PR nuevo.
- Ni Railway ni Neon.
- No se ha tocado Flutter, el short paper ni el experimento original.
- No se han repetido corridas de forma selectiva.
- No se ha ampliado el presupuesto.
