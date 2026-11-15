# Limitaciones para la tesis (v1.1)

El texto completo de las limitaciones y el párrafo propuesto para el informe académico están en `../cierre_conservador/LIMITACIONES_TESIS.md`, y **siguen vigentes sin cambios** para v1.1, porque el catálogo es idéntico. Aquí solo lo que v1.1 añade o precisa.

## 1. Cierre técnico frente a validación clínica

| Lo que es v1.1 | Lo que **no** es |
|---|---|
| El cierre **técnico** del catálogo: correcciones trazables, contenciones conservadoras y pruebas automáticas que pasan sobre un commit concreto y reconstruible | Una **validación clínica** o profesional del catálogo |
| Una versión **lista para que el propietario autorice su integración** en el backend | Una **autorización de uso con personas reales**, que requiere su propio proceso (consentimiento, revisión ética, supervisión) |
| Un sistema que aplica restricciones declaradas y reglas de seguridad | Una garantía de seguridad o una certificación |

Las **312 decisiones** que requieren revisión profesional siguen **abiertas**. No se presentan como aprobadas ni se resolvieron con nuevas rondas de IA.

## 2. Limitaciones que v1.1 precisa

1. **P-03 por colocación.**
   - En la candidata y sus pruebas está encendida. En producción está apagada hasta una autorización.
   - Si la evaluación se hace con el sistema desplegado, hay que declarar el valor de `FITSENSE_P03_POR_COLOCACION` que se usó.
   - En los 40 escenarios HALL-GM-01 no cambia ningún resultado. En la regresión completa cambia uno: una regeneración tras una progresión pasa de publicarse a NO_FACTIBLE. Es conservador y explicado.
2. **Adherencia y omisiones impuestas por el sistema.**
   - El indicador oficial no cambia y cuenta como no hechas las omisiones que impone FitSense.
   - La causa no se guarda en la fila de la omisión: la de dolor se puede reconstruir, pero las demás no.
   - Una evaluación de adherencia con datos reales debe:
     - **(a)** declarar esta limitación y, si separa las omisiones por dolor, el supuesto de configuración; o
     - **(b)** integrar antes la propuesta V57, que guarda la causa sin cambiar ningún cálculo (`TRAZABILIDAD_OMISIONES_SISTEMA.md`).
3. **La capacidad de generar rutinas** (validada técnicamente) **no equivale a** la **validez de la medición de adherencia** (sujeta a la limitación anterior).
4. **Funcionalidades opcionales apagadas:** V53 A, V53-B y #64. Los resultados de la tesis no deben atribuírselas.

## 3. Frase de configuración sugerida para el método

> «La versión del catálogo evaluada fue FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1.1 (commit 12c6897), con la contención conservadora y la protección ante dolor en planes publicados activas, y las funcionalidades opcionales de apoyo suspendido y de reparación tolerante desactivadas. [Indicar si la comparación por colocación de P-03 estuvo activa en el despliegue.]»
