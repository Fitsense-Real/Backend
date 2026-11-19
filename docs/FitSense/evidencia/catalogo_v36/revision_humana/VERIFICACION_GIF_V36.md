# V36 · Cotejo de los nueve GIF originales

**Fecha:** 28–29/10/2026.

**Fuente:** `exercises.gif_path` de cada ejercicio, en el dataset de origen (`hasaneyldrm/exercises-dataset`, © Gym visual), a 180 × 180 px.

**Método:**
- Lectura de todos los fotogramas, ampliados ×3.
- En 1296 y 1333, además, una medida reproducible del contacto con el suelo (`cotejo_gif_v36.py`): la fila más baja con píxeles de la figura en cada fotograma.

**Autoría:** el cotejo lo hizo el asistente de desarrollo (IA) sobre los archivos originales. No es una revisión humana independiente.

Los fotogramas no se versionan (licencia de la fuente). Se reproducen con el `gif_path` y el script.

| Id | GIF | Fotogramas | Lo que muestra | ¿Coincide con el dictamen? |
|---:|---|---:|---|---|
| 1267 | `videos/3291-zd4P4B2.gif` | 12 | Parte del pino, baja a apoyo de manos con piernas abiertas (straddle) y vuelve al pino. Inversión y control gimnástico avanzado | **Sí.** Desactivar |
| 1296 | `videos/0803-4GqRrAk.gif` | 12 | Plancha con los brazos totalmente extendidos al frente, por delante de la cabeza: el tronco sube y baja. **Manos y pies no se despegan del suelo en ningún fotograma**: el punto más bajo de los pies queda en y=108–109 y el de las manos en y=107–108 en los 12. No hay fase de vuelo | **No.** El dictamen suponía una fase pliométrica que el GIF no tiene. La corrección se detuvo, se presentó el hallazgo y el responsable aprobó la clasificación que corresponde al GIF: **nivel 3 sin alto impacto** (aplicado en V36) |
| 1333 | `videos/3637-km2Ljzj.gif` | 36 | Carrera con zancadas amplias y braceo. En los fotogramas 5–8 y 22–26 la figura entera está en el aire: el punto más bajo sube de y≈167–170 a y=121–122. Hay fase de vuelo | **Sí.** Peso corporal, sin rueda, sin suelo ni brazos arriba, y **alto impacto confirmado** |
| 872 | `videos/0484-196HJGw.gif` | 12 | Tumbado boca arriba, rodillas flexionadas: despega los pies y lleva las rodillas al pecho, enrolla la pelvis y la despega del suelo (fotogramas 5–7), y vuelve. Se resalta el abdomen; la cadera no se extiende | **Sí.** Encogimiento invertido, `TRUNK_FLEXION` |
| 1059 | `videos/1688-K9VL0Jq.gif` | 12 | De pie a zancada adelante, con la rodilla de atrás cerca del suelo y giro del tronco hacia la pierna adelantada | **Sí.** `upper legs/quads`, `KNEE_DOMINANT` |
| 527 | `videos/1651-1VpF8db.gif` | 12 | Curl con mancuernas de pie y después zancada en la que baja las mancuernas junto al pie adelantado («lanzamiento») | **Sí.** `upper legs/quads`, `KNEE_DOMINANT` |
| 609 | `videos/1658-Mz6lLcW.gif` | 12 | Zancada con curl de bíceps en la posición baja | **Sí.** `upper legs/quads`, `KNEE_DOMINANT` |
| 725 | `videos/3547-OeL23VY.gif` | 18 | **Sentado en un banco con respaldo:** curl, giro de muñecas y press por encima de la cabeza | **Sí** en la clasificación (`shoulders/delts`, `VERTICAL_PUSH`, brazos arriba). Sobre el banco, ver abajo |
| 760 | `videos/3560-LeaZOIz.gif` | 18 | De pie: curl martillo alterno y press a una mano por encima de la cabeza | **Sí.** `shoulders/delts`, `VERTICAL_PUSH`, sin banco |

## 1296 · la evidencia corrigió la hipótesis inicial

El dictamen suponía una flexión pliométrica, con nivel 3 y alto impacto condicionados a una fase de vuelo. El GIF no la tiene. Es una flexión de **palanca larga**: brazos extendidos por delante de la cabeza y apoyo continuo de manos y pies.

Se detuvo la corrección y se presentó el hallazgo. **Decisión del responsable (29/10/2026), aplicada en V36:**
- `difficulty_level = 3`: la palanca larga es de alta exigencia;
- `high_impact = false`: no hay vuelo ni aterrizaje;
- se mantienen `HORIZONTAL_PUSH`, `chest/pectorals`, suelo y brazos arriba;
- instrucciones nuevas que describen el GIF, sin la elevación alterna de brazo y pierna del texto anterior ni ningún salto.

## 725 · banco

El GIF exige asiento y el modelo actual permitiría exigir el banco: un grupo 2 en `exercise_equipment_requirements` (AND con la mancuerna), como 514 y 720.

**No se aplica, por coherencia.** Los ejercicios sentados con mancuerna del catálogo **704, 723, 726, 739 y 740** también se ejecutan sentados en un banco en su GIF y **ninguno** lo exige. La convención de V24 añadió el banco solo cuando el nombre lo dice («bench»: 514, 720) y trata «sentado» como «basta un asiento».

Exigirlo solo en 725 haría que un perfil de casa sin banco perdiera 725 pero conservara 739, con la misma postura.

**Decisión del responsable (29/10/2026): aplazada.** No se modifican los requisitos de banco de 725 ni de los demás ejercicios sentados. La inconsistencia queda documentada para una futura corrección del catálogo, sin migración adicional en esta fase.
