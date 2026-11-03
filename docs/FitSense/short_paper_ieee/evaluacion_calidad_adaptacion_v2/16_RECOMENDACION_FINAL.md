# Recomendación final: validación funcional y robustez del prompt V2

## Clasificación

**APTO CON OBSERVACIONES:** **mantener V2 experimental y apagado.** Esta clasificación no autoriza ninguna activación ni despliegue.

La provisional se escribió antes de la fase con Replicate; tras esa fase **se mantiene**:
- el criterio D, que estaba pendiente, ahora está comprobado con el modelo real;
- el criterio B empeora en la cohorte R, **en las dos condiciones por igual**.

| Criterio | Resultado | Base |
|---|---|---|
| A. Cumplimiento de restricciones | ✔ Ninguna violación: 0 en las 6 corridas reales (4 del PR #29 y RC1_r1/RC2_r1, dos semanas) y en las 7 ejecuciones sin red | `06`, `07`, `14`, `seguridad.py` |
| B. Coherencia funcional | ⚠ Las adaptaciones discutibles (Q2/Q3) son muchas **en las dos condiciones**: PR #29 42 % C1 y 40 % C2; cohorte R 78 % C1 y 69 % C2. Vienen de la regla (V8C por patrón), del catálogo y de la falta de variantes más sencillas (60 de 66 sustituciones parten de dificultad 1), **no de V2**. C2 resuelve a veces TOO_DIFFICULT bajando la dosis (H15) | `03`, `14` §1 y §4, H12–H15 |
| C. Continuidad | ✔ Válida, con una observación. Las sesiones en el mínimo se conservan y las reducciones se acumulan hasta el mínimo y se paran ahí. **Observación (H12):** sin variante más sencilla, las dos condiciones vuelven a sustituir cada semana la sesión casi entera por ejercicios de la misma dificultad | `06`, `14` §4 |
| D. Estabilidad | ✔ **Comprobada con el modelo real en 8 perfiles nuevos:** C2 aplicó las 11 operaciones factibles (C1: 8) y omitió solo las 2 sin solución. Fue 1 predicción por operación, sin ningún rechazo | `12`, `13`, `14` §4.1 |
| E. Eficiencia | ✔ C2 frente a C1 en la cohorte R: 13 frente a 22 predicciones, 0 frente a 9 rechazos, US$ 0,17 frente a US$ 0,33. Es coherente con el PR #29 | `09` anexo 2, `14` §4.1 |
| F. Regresión | ✔ V1 sigue igual: 1.072/0 unitarias, 452/0 (+37 *opt-in*) IT, 960 perfiles y REPLAY idénticos, y CI verde en el SHA correcto | `15` |

**Por qué no es «APTO PARA CONSIDERAR ACTIVACIÓN»:**
- **Los problemas funcionales del criterio B siguen abiertos** y aparecen con cualquier prompt:
  - H1: V8C comprueba por conjunto;
  - H2, H7 y H13: músculo y catálogo;
  - H3 y H12: variantes laterales sin simplificación, repetidas cada semana.

  Activar V2 haría que estas adaptaciones se **apliquen más a menudo**, porque V2 falla menos. **Que se apliquen más adaptaciones no las hace mejores.**
- **Hay una sola corrida por condición en la cohorte R:** son indicios, no estimaciones.
- **No hay evaluación profesional** (§24).

**Por qué no es «NO APTO»:** no hay ningún defecto, regresión ni violación demostrados que se puedan atribuir a V2. En fiabilidad y en coste, V2 es claramente mejor que V1.

## Respuestas al informe final (fase 14)

1. **¿Cuáles fueron las seis sustituciones?**
   - 580 → 313 (C1 r1, U04);
   - 580 → 129 (C2 r2, U04);
   - 775 → 313 (C2 r1, U09);
   - 780 → 119 (C2 r1 y C2 r2, U09);
   - 933 → 318 (C1 r2, U14).

   Con el catálogo real aparecen 2 más en la etapa C del PR #28: 775 → 73 y 780 → 300 (`03` §2). En la cohorte R hay 66 sustituciones más (`auditoria_sustituciones_RC.csv`).
2. **¿Qué cambió?** El patrón se mantuvo en todas, también en las 66 de la cohorte R. El músculo principal cambió así:
   - pectorales → tríceps (580 y 933);
   - dorsal alto → tríceps o pectorales (775);
   - deltoides → dorsal alto o ancho (780).

   En la cohorte R hubo 15 cambios de músculo más (9 en C1 y 6 en C2). Cuatro vienen del 862, un ejercicio de abdominales clasificado como VERTICAL_PULL (H13).
3. **¿Cuáles son coherentes?**
   - De las 8: **ninguna** es plenamente coherente (Q1), pero todas son técnicamente válidas.
   - De las 57 sustituciones del PR #28/#29, 33 son Q1.
   - De las 66 de la cohorte R, solo **5 son Q1** (bajan la dificultad sin cambiar el músculo).
4. **¿Cuáles requieren revisión profesional?**
   - las 8 (Q5) y una muestra de las 21 con la misma dificultad;
   - en la cohorte R, las 16 sesiones discutibles de la matriz, sobre todo las sustituciones laterales de la sesión completa (H12).
5. **¿V8C funciona conforme a su diseño?** Sí. Compara el patrón por conjunto, y no compara músculo, distribución, número de añadidos fuera del dolor ni dificultad relativa (`04`).
6. **¿Hay algún defecto demostrable en las sustituciones?**
   - **No hay defecto de implementación.**
   - Hay un **hueco de regla** demostrado (H1): una sesión puede perder un patrón y publicarse. No se observó con Replicate, ni en el PR #29 ni en la cohorte R.
   - Hay **problemas de catálogo**: 580, 775 y 780 (H7), y 862 (H13).
7. **¿Cuántas sesiones en el mínimo V20?**
   - **PR #29:** 7 apariciones (C1: 2; C2: 5), que son **4 sesiones distintas**: U08 S3, U18 S3, U19 S2 y U19 S3.
   - **Cohorte R, R05 S2:** se generó en el mínimo (margen 0) y se adaptó solo con sustitutos de la misma dosis (C1 en la semana 1; C2 en las semanas 1 y 2).
   - **Cohorte R, R06 S3:** C2 la llevó al mínimo (1320 → 1260 s).
8. **¿Qué pasó con una segunda adaptación?**
   - **Sin margen:** el intento de seguir reduciendo se rechaza por V20 (C1) o no se hace (C2), y la sesión queda **SIN_EFECTO, conservada**.
   - **En VARIANTE:** se vuelven a aplicar sustitutos de la misma dosis. En la cohorte R se sustituyen otra vez los ejercicios introducidos la semana anterior (R01 C2: 6 de 6; R05 C2: 8 de 8; H12).
   - **En todos los casos:** 0 subidas y linaje correcto (`06`, `seguridad.py`).
9. **¿Alguna incompatibilidad al combinar?**
   - En duración, dosis o pisos, ninguna: lo inválido se rechaza siempre (V20, V24, V16, V8C).
   - **En patrón (#12b) y músculo (#12a), sí:** pasan V8C (`07`).
10. **¿La factibilidad V2 describe bien sus límites?**
    - **Sí** en duración (incluida la suma de cambios), pisos y 4D, y también en los 8 perfiles nuevos con el modelo real: el modelo omitió exactamente las 2 operaciones sin opción (R04, margen 6 s; R07, margen 3 s).
    - **No** considera el músculo ni la dificultad relativa de los sustitutos, ni impide la combinación #12b. Lo declara («el validador final sigue decidiendo»).
    - Imprime la línea «combinar» aunque nada quepa (H8).
11. **¿Qué perfiles nuevos?** R01–R08: los 4 objetivos, cada uno con un principiante y un intermedio (`10`).
12. **¿Qué aportan?**
    - 30, 45, 60 y 75 min;
    - casa (desde solo peso corporal) y gimnasio;
    - márgenes de 0 a 228 s;
    - registro incompleto (Aw ≠ Aobs);
    - desmotivación;
    - dificultad con pocas o muchas variantes;
    - un cambio de causa en la semana 2;
    - dos semanas.
13. **¿Resultados sin Replicate?**
    - R02 es NO_FACTIBLE en la generación (GM, 75 min).
    - Con la IA apagada: RESPALDO o SIN_EFECTO, sin cambios.
    - Con el guion V2: en la semana 1 se aplican 5 de los 6 elegibles (R04 queda SIN_EFECTO con margen 6); en la semana 2, los 7.
    - 0 violaciones (`11`, `06`).
14. **¿Está preparada la nueva comparación?** Sí: perfiles congelados (`7af7c0f`), protocolo y presupuesto aprobado antes de cualquier llamada (`09`, anexo 1).
15. **¿Se autorizó y ejecutó la fase con Replicate?** **Sí.** Aprobaste la opción A y se ejecutó tal como se congeló (`09`, anexo 2):
    - RC1_r1 (`v1`) y después RC2_r1 (`v2`);
    - 2 semanas;
    - 35 predicciones en total (22 y 13), todas `succeeded`;
    - **US$ 0,50** de un máximo de USD 3,00;
    - sin repeticiones ni fallos técnicos.
16. **¿Adaptaciones aplicadas por condición?**

    | | Semana 1 | Semana 2 | Sobre las 11 factibles |
    |---|---:|---:|---:|
    | C1 (RC1_r1) | 5 de 6 | **3 de 7** | 8 |
    | C2 (RC2_r1) | 5 de 6 | **6 de 7** | **11** |

    Referencia del PR #29: C1 6 y 5 de 8; C2 7 y 7 de 8.
17. **¿Rechazos o propuestas sin cambios?**
    - **C1:**
      - 9 rechazos, todos V20;
      - 8 de 13 operaciones necesitaron más de un intento;
      - 5 SIN_EFECTO: las 2 sin opción y **3 factibles perdidas** (R05, R06 y R08 en la semana 2), donde el segundo intento se rindió (H14).
    - **C2:**
      - 0 rechazos;
      - 2 SIN_EFECTO, que son las 2 operaciones sin opción, en las que el modelo explicó por qué no había corrección válida.
    - **PR #29:** C1 15 rechazos y C2 ninguno.
18. **¿Problemas de calidad funcional?** Sí, **los mismos en C1 y C2** (`14` §4.2):
    - **sustitución lateral de la sesión casi completa** cuando no hay variante más sencilla ni dosis que bajar, repetida cada semana (H12);
    - **cambios de músculo:** 9 en C1 y 6 en C2, cuatro de ellos por el 862;
    - **variantes de la misma dificultad:** 26 de 29 sustituciones en C1 y 35 de 37 en C2;
    - **VARIANTE resuelta solo con menos dosis:** 3 sesiones en C2 y 0 en C1 (H15);
    - **una sesión de fatiga llevada al mínimo exacto** (C2, R06).
19. **¿Sesiones modificadas fuera de su alcance?** **0**, también en la cohorte R.
20. **¿Algún plan inválido publicado?** **No.**
21. **¿La regresión histórica sigue pasando?** **Sí** (`15`). La fase con Replicate no cambió `src/main`.
22. **¿El CI de GitHub corrió sobre el SHA correcto?** **Sí.** Ejecución 41577492912 sobre `51c145b`, en verde. Corrijo mi informe anterior, que decía lo contrario. El PR de esta evaluación también tiene CI en cada *push* (`15`).
23. **¿Qué necesita decisión funcional?**
    - **H1:** endurecer V8C (opción A/B + D).
    - **H3/H12:** qué hacer con TOO_DIFFICULT cuando no hay variante de menor dificultad. Las opciones:
      - conservar la sesión;
      - limitar el número de sustituciones;
      - en V2, listar solo los sustitutos de menor dificultad (es un cambio del prompt).
    - **H4/H5:** el margen de duración al generar y al adaptar, y si saltarse la IA cuando no hay ninguna opción.
    - **H6:** GM con 75 min es NO_FACTIBLE.
    - **H8:** retoques del texto V2, por ejemplo ordenar los sustitutos por músculo.
24. **¿Qué necesita evaluación profesional?**
    - los cambios de músculo: las 8 del PR #28/#29 y las 15 de la cohorte R;
    - la clasificación de 580, 775, 780 y 862;
    - si reducir la dosis es una respuesta adecuada a «demasiado difícil»;
    - si es aceptable cambiar la sesión entera por otra de la misma dificultad, cada semana;
    - si conviene llevar sesiones al mínimo.
25. **¿Mantener experimental o activar?** **Mantener V2 experimental y apagado.**
    - **(1) La fase con Replicate ya está hecha.** Confirma que V2 es más fiable y barato que V1 en perfiles nuevos, y que no es más agresivo.
    - **Antes de considerar activarlo:**
      - **(2)** decidir H1 (V8C) y H3/H12 (TOO_DIFFICULT sin variante más sencilla);
      - **(3)** la revisión profesional de §24;
      - **(4)** si se cambia el texto de V2 (H8/H12), repetir una comparación con el mismo protocolo.
    - **Optimizar V2 para que aplique más cambios no es un objetivo:** sus ventajas medidas son 0 rechazos, operaciones factibles no perdidas y menor coste.

## Fuera del alcance (respetado)

- V2 no se ha activado; la configuración por defecto sigue en `v1`.
- No se ha modificado V20, V24, V8C, el catálogo, la dificultad, las dosis ni las políticas.
- No se ha activado C2-D/EF.
- No se ha fusionado PR #17, #28 ni #29.
- Ni Railway, ni Neon, ni migraciones remotas, ni Flutter.
- No se ha modificado el short paper.
- No se han sobrescrito resultados:
  - `14_MATRIZ` y los informes solo **añaden** la fase nueva;
  - las corridas se conservan tal como salieron.
- **Replicate solo se llamó después de tu aprobación, dentro del límite:** 35 predicciones y US$ 0,50.
- Nada de esto es una evaluación clínica.
