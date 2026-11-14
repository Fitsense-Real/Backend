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
- 1184, 462 y 777: suben a nivel 2 y, con V53 encendido, quedarían fuera de «sin colgarse».

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

**Decisión (orden de desbloqueo, 14/11/2026).** «Si necesita banco, no debe permanecer clasificado como solo peso corporal para conservar la factibilidad.» V52 aplica ahora la corrección de 775 (`bench; 1:bench`). No se relaja V20.

**Efecto medido** con el catálogo final de la PR A (`Hipertrofia4Dv34IT`): **14 perfiles** GAIN_MUSCLE en casa solo con peso corporal quedan sin plan, todos por V20 (piso de la sesión):
- intermedio, 3 y 4 días, a 58, 59, 60, 74 y 75 minutos (10);
- principiante y avanzado, 3 días, a 60 y 75 minutos (4).

La prueba fija la lista exacta, de modo que un perfil más o uno menos la hace fallar. Es una pérdida de factibilidad **legítima**: no existe una semana válida con el equipo que esos perfiles declaran. Queda documentada para el propietario y la retira un perfil que declare banco.

## 3. Adaptación, progresión y reincorporación

- **Código del motor:** sin cambios, salvo que V51 sustituye la regla por nombre por el atributo explícito con el mismo valor (`CatalogoAtributosExplicitosIT`).
- **Pruebas de adaptación ante dolor, retirada localizada y linaje** (`RetiradaLocalizada*IT`, `TextoRespaldoSustitucionIT` y `LinajeIntegridadIT`): sus escenarios estaban construidos con perfiles y planes elegidos sobre el catálogo V50. Con V52 el motor elige otros ejercicios y la **premisa** del escenario deja de darse (por ejemplo, «el plan lleva el 135»); el mecanismo no falla. Se fijan al catálogo V50, igual que las demás pruebas históricas, y siguen probando el mecanismo.
- **Progresión y reincorporación: HALL-PROG-01 (corregido).** `ProgresionIT.p02ProgresionSinElExcluido` y su gemela `ProgresionRev2IT` fallaban porque el sustituto de seguridad heredaba una dosis progresada (3x19) que V24 rechaza. La reproducción por semilla demuestra que el defecto **ya existía en el motor antes del cierre**: falla en 5 de 5 semillas en la base 57075e7 y en 4 de 5 en `generation`. El catálogo solo cambia qué semillas lo disparan. Corrección mínima en `SafetySubstitutionPolicy`: el sustituto recibe una dosis de capacidad válida para él, nunca con más trabajo. V24, V8C, P-03 y la prueba no se tocan. Pasan las 5 semillas en las tres ramas. Detalle, trazas y pruebas en `HALL_PROG_01.md` (PR #62, y el mismo commit en #60).
- **Generación de referencia** (`GeneracionReferenciaIT`): la referencia cambia porque cambia el catálogo. Ejemplos: 1117 sube a impacto MODERADO y 253 pierde KN. Se regenera: 70 de 72 planes cambian y los 72 siguen VALIDA. Tras la orden de desbloqueo se regeneró otra vez por 775: cambian 30 planes, todos de casa, y los 72 siguen VALIDA (`REGRESION_FINAL.md` §3). Es idéntica con V53 apagado.

## 4. Planes publicados

Ver `IMPACTO_PLANES_PUBLICADOS.md`. En el plan 78, el 910 se suma al 916 como límite de la reparación.
