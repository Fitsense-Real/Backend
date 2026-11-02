# Mejora experimental de LOSE_WEIGHT: preferir un grupo grande no cubierto (desactivada)

**Implementación.** `CapabilityWeekBuilder`, en la colocación de la fuerza secundaria de LOSE_WEIGHT.
- Antes de lo habitual (slot pendiente, estructural…), prueba un ejercicio de un grupo grande (pecho, espalda, hombros o pierna) que la semana aún no trabaja.
- Usa **el mismo selector y el mismo filtro**: V15, V18, V22 y los topes de grupos pequeños se aplican igual.
- No crea obligaciones nuevas en 4A/4C.
- No añade tiempo ni dosis: sigue rellenando hasta el mismo piso y con la misma dosis.

**Indicador:** `fitsense.generation.lose-weight.preferir-grupo-no-cubierto` (por defecto `false`). El laboratorio lo activa solo en su instancia y lo restablece al terminar.

## 1. Resultados (`LOSE_WEIGHT_PREFERENCIA_COBERTURA.csv`, 240 perfiles)

| Indicador | Valor |
|---|---:|
| Válidos con la histórica | 230 |
| …con algún grupo grande sin series | 148 |
| **Mejoran** (menos grupos a cero, siguen válidos) | **48** (34 quedan sin ningún grupo a cero) |
| Sin cambio en grupos a cero | 95 |
| Empeoran (pasan a inválidos o más grupos a cero) | 5 (2 válidos pasan a **inválidos por V15**) |
| Inválidos con la histórica que pasan a válidos | 6 |
| Planes válidos **sin** grupos a cero cuya composición cambia | **82** |
| Minutos de cardio | **Sin cambio** en todos (delta 0) |
| Series totales de fuerza | **Sin cambio** en todos (delta 0): se reparten distinto |

**Frente a lo previsto.** El laboratorio anterior estimó 104 de 148 con una alternativa válida añadiendo un ejercicio. Conectada al constructor completo, la preferencia **mejora 48**:
- el constructor no añade un ejercicio más: **reparte** la misma fuerza secundaria entre grupos;
- y el tiempo disponible no cambia.

## 2. Valoración

- La cobertura aeróbica obligatoria no empeora y la dosis de fuerza tampoco.
- **Cambia la composición de 82 planes que ya eran válidos y sin brecha**, y deja 2 válidos como inválidos (V15).
- **No se activa:** requiere aprobación separada y, antes, una versión de política propia (hoy es un indicador de generación y no queda en la traza).
- Si se aprueba, conviene:
  1. limitarla a las semanas con algún grupo grande sin cubrir;
  2. corregir el caso V15 (no elegir el grupo si rompe la regla de la mitad en el día).
