# Criterios de aceptación de H2 (fase 8)

Se fijan **antes** de ver ningún dato real y no se cambian después.

## Problema

Hay que decidir con reglas previas si PR-1.9 se activa o se conserva PR-1.7. No hay que declarar una mejora que la evidencia no demuestre.

## Unidad de análisis

La pareja: el mismo perfil con A y con B. Se compara cada pareja y, después, la suma.

## Criterios

| # | Criterio | Regla | Tipo |
|---|---|---|---|
| C1 | **Reduce la contradicción prompt–V15** | Casos con al menos un rechazo V15 por la regla de la mitad: **B < A** | Primario |
| C2 | No aumenta las violaciones de seguridad | Planes publicados que el validador independiente rechaza: **0 en B** (y 0 en A) | Bloqueante |
| C3 | No deteriora la factibilidad | NO_FACTIBLE + respaldos en B ≤ en A + 1 | Bloqueante |
| C4 | No aumenta los reintentos sin beneficio | Predicciones totales en B ≤ en A × 1,10 | Bloqueante |
| C5 | Validez al primer intento | B ≥ A − 1 caso | Bloqueante |
| C6 | Sin regresiones en otros validadores | Ninguna categoría (V20, V23, V24, V25, V26, V27, V8C, contrato) sube más de 1 caso en B | Bloqueante |
| C7 | Coherencia y latencia | Mediana del tiempo total en B ≤ A × 1,10; los planes de B cubren su enfoque igual que los de A (V15 y cobertura) | Bloqueante |
| C8 | Conserva las políticas aprobadas | Ningún cambio en V15, V24, V8C, D8, P-03 ni el catálogo, con el árbol de código fijado por SHA | Bloqueante |

## Decisión

| Resultado | Condición | Recomendación |
|---|---|---|
| **Favorable** | C1 se cumple con A ≥ 3 casos con rechazo V15 de la mitad, y C2–C8 se cumplen | Activar PR-1.9 en el entorno de evaluación y actualizar la configuración evaluada (`ConfiguracionEvaluadaTest`) con una decisión aparte |
| **Inconcluso** | **A < 3 casos** con rechazo V15 de la mitad (no hay potencia), o C1 empata, con C2–C8 cumplidos | **Conservar PR-1.7.** Opcionalmente, repetir solo los casos de 4 o 5 días con más sesiones donde aplica la regla |
| **Desfavorable** | Falla cualquier criterio bloqueante | Conservar PR-1.7 y analizar la causa caso por caso |

**Transparencia:**

- se informan las 24 parejas, incluidos los casos en que A sale mejor que B;
- la prueba de McNemar exacta sobre C1 se informa como dato, no como único criterio, porque n es pequeño;
- no se declara que H2 mejora el sistema si C1 no se cumple con potencia suficiente.

## Expectativa honesta

- En las campañas reales anteriores, la regla de la mitad apareció en 5 de 112 rechazos de generación, y en solo 2 fue la única causa.
- Con 24 parejas, **lo más probable es un resultado inconcluso en C1**.
- La evaluación seguirá siendo útil para C2–C7: comprobar que PR-1.9 **no empeora** nada con la IA real.
- Pero **no bastará para afirmar una mejora**.
