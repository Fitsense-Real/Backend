# Impacto del cierre en elegibilidad, generación, adaptación, progresión y reincorporación

## 1. Rejilla de 168 perfiles: V50 frente al catálogo final

La rejilla cubre 4 objetivos × 3 niveles × gimnasio/casa × 7 combinaciones de restricciones, con el motor de reglas y sin IA, la misma semana y la misma semilla (`CatalogoIntegralComparacionIT`).

| Medida | Resultado |
|---|---|
| Perfiles con plan, antes y después | **168 de 168 → 168 de 168**. Ningún perfil pierde el plan |
| Cambio del conjunto elegible: mínimo / mediana / máximo | −46 / −2 / +4 |
| Principiantes | −46 / −13 / +1. Las 63 subidas de dificultad (1→2) sacan ejercicios que no eran de principiante |
| Intermedios | −8 / −2 / +2 |
| Avanzados | −12 / 0 / +4 |
| Planes cuyo contenido cambia | 168. Cambia la selección, no el desenlace |

Ejercicios que más salen del conjunto elegible:
- 312 y 1194: pasan a estiramiento (`is_stretch`) y salen del trabajo principal.
- 1205, 318, 510, 1111, 508, 1058, 1115, 1262, 1305, 901 y 1180: suben a nivel 2.
- 1113 **no** sube: la subida que proponía la revisión se vetó por la decisión previa de V39 («1113 conserva los valores de V39») y pasa a la lista profesional.
- 1184, 462 y 777: suben a nivel 2 y, con V53, quedan fuera de «sin colgarse».

Ejercicios que más entran:
- 455 y 773: dejan de ser estiramientos.
- 574, 108, 503, 797 y 481: pierden una OH que no tenían.
- 296: pierde FL.
- 675: deja de ser alto impacto.

Evidencia en `cierre/impacto/` (`antes_V50.csv`, `despues_V53.csv` y `resumen_168.tsv`).

## 2. Hipertrofia de 30 a 75 minutos (`Hipertrofia4Dv34IT`)

**Hallazgo.** Con la corrección de equipo de **775** (codos sobre dos apoyos elevados, es decir, banco), GAIN_MUSCLE intermedio en casa con solo peso corporal, 3 días a 74 y 75 minutos, deja de tener plan:
- Con V50, el plan llevaba 775 en las tres sesiones y llegaba a 3.210 s.
- Sin 775, el techo baja a 3.090 s, por debajo del piso V20 (3.108 s a 74 min).

**Decisión.** No se relaja V20 y no se aplica en silencio. La corrección de 775 **sale de V52** y queda como **DEC-FACTIBILIDAD-775**, con sus textos corregidos. Con esa decisión, la rejilla queda como con V50 (`REGRESION_FINAL.md`).

## 3. Adaptación, progresión y reincorporación

- **Código del motor:** sin cambios, salvo que V51 sustituye la regla por nombre por el atributo explícito con el mismo valor (`CatalogoAtributosExplicitosIT`).
- **Pruebas de adaptación ante dolor, retirada localizada y linaje** (`RetiradaLocalizada*IT`, `TextoRespaldoSustitucionIT` y `LinajeIntegridadIT`): sus escenarios estaban construidos con perfiles y planes elegidos sobre el catálogo V50. Con V52 el motor elige otros ejercicios y la **premisa** del escenario deja de darse (por ejemplo, «el plan lleva el 135»); el mecanismo no falla. Se fijan al catálogo V50, igual que las demás pruebas históricas, y siguen probando el mecanismo.
- **Progresión y reincorporación.** `ProgresionIT.p02ProgresionSinElExcluido` y su gemela `ProgresionRev2IT` fallan, pero **no por el cierre**. Lo comprobé en cuatro estados:

  | Estado | Resultado de p02 |
  |---|---|
  | `generation` (623c4fa) | Pasa |
  | Cabeza de #56 (V47 y V48) | **Falla** |
  | Base del cierre, #55 sobre #56–#59 (57075e7) | **Falla** |
  | Cierre (V51–V53) | **Falla**, igual que la base, y también con el catálogo fijado en V50 |

  **Mecanismo:**
  1. La progresión sube el ejercicio marcado a 3x19.
  2. El usuario lo señala con dolor.
  3. La semana conservada lo sustituye, y **el sustituto hereda la dosis progresada 3x19**. V24 lo rechaza: no es la dosis de ninguna capacidad. Esto ya ocurre en `generation` (ejercicio 72).
  4. En `generation`, la regeneración segura lo resuelve en el mismo ciclo.
  5. Con el catálogo de #56, la semana regenerada con dosis base sube los segundos (1.140 → 1.260). P-03 nivel 2 la rechaza («ante dolor no se añade trabajo»), y esa semana queda sin plan (NO_FACTIBLE registrado en V32).

  **Lo que no se hace:**
  - No se relaja P-03 ni V24.
  - No se cambia la prueba.
  - No se toca el motor de adaptación: corregir la herencia de dosis del sustituto es un cambio de adaptación, fuera del catálogo.

  Queda como hallazgo **HALL-PROG-01** y bloqueo para fusionar #56 (`REGRESION_FINAL.md`).
- **Generación de referencia** (`GeneracionReferenciaIT`): la referencia cambia porque cambia el catálogo. Ejemplos: 1117 sube a impacto MODERADO y 253 pierde KN. Se regenera: 70 de 72 planes cambian y los 72 siguen VALIDA. Es idéntica con y sin V53. El diff está en `REGRESION_FINAL.md` §2, para tu aprobación.

## 4. Planes publicados

Ver `IMPACTO_PLANES_PUBLICADOS.md`. En el plan 78, el 910 se suma al 916 como límite de la reparación.
