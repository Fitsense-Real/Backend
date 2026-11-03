# Recomendación final: validación funcional y robustez del prompt V2

## Clasificación

**APTO CON OBSERVACIONES** (provisional): **mantener V2 experimental y apagado.** Esta clasificación no autoriza ninguna activación ni despliegue.

| Criterio | Resultado | Base |
|---|---|---|
| A. Cumplimiento de restricciones | ✔ Ninguna violación nueva: 0 en 4 corridas reales y en 7 ejecuciones sin red, contando las dos semanas | `06`, `07`, `14` |
| B. Coherencia funcional | ⚠ El 40 % de las adaptaciones son discutibles (Q2/Q3), **igual que con C1 (42 %)**. Vienen de la regla (V8C por patrón) y del catálogo, no de V2. C2 tiende a resolver TOO_DIFFICULT bajando la dosis | `03`, `14` |
| C. Continuidad | ✔ Válida. Las sesiones que quedan en el mínimo se conservan en la semana siguiente; las reducciones se acumulan hasta el mínimo y se paran ahí | `06` |
| D. Estabilidad | ⚠ **Solo comprobada sin red**: la descripción de factibilidad es correcta en los 8 perfiles nuevos. **Falta ver la respuesta del modelo real en esos perfiles** (fase con Replicate, pendiente de tu aprobación) | `09`, `11`, `14` §3 |
| E. Eficiencia | ✔ C2: 0 rechazos, la mitad de llamadas y de coste que C1 | `experimento_prompt_v2/14` |
| F. Regresión | ✔ V1 igual que antes: 1.072/0 unitarias, 452/0 (+37 *opt-in*) IT, 960 perfiles y REPLAY idénticos, y CI verde en el SHA correcto | `15` |

**Por qué no es «APTO PARA CONSIDERAR ACTIVACIÓN»:**
- D sigue pendiente con la IA real;
- los problemas funcionales B están abiertos: H1 (V8C por conjunto), H2/H7 (músculo y catálogo) y H3 (variantes sin simplificación).

**Por qué no es «NO APTO»:** no hay ningún defecto, regresión ni violación demostrada atribuible a V2.

## Respuestas al informe final (fase 14)

1. **¿Cuáles fueron las seis sustituciones?**
   - 580 → 313 (C1 r1, U04);
   - 580 → 129 (C2 r2, U04);
   - 775 → 313 (C2 r1, U09);
   - 780 → 119 (C2 r1 y C2 r2, U09);
   - 933 → 318 (C1 r2, U14).

   Con el catálogo real aparecen 2 más en la etapa C del PR #28: 775 → 73 y 780 → 300. Ver `03` §2.
2. **¿Qué cambió?** El patrón se mantuvo en todas. El músculo principal cambió así:
   - pectorales → tríceps (580 y 933);
   - dorsal alto → tríceps o pectorales (775);
   - deltoides → dorsal alto o ancho (780).
3. **¿Cuáles son coherentes?** Ninguna de las 8 es plenamente coherente (Q1). Todas son técnicamente válidas. De las 57 sustituciones reales, 33 son Q1.
4. **¿Cuáles requieren revisión profesional?** Las 8 (Q5), y una muestra de las 21 con dificultad igual.
5. **¿V8C funciona conforme a su diseño?** Sí: patrón por conjunto, sin comparar músculo, distribución, número de añadidos fuera del dolor ni dificultad relativa (`04`).
6. **¿Hay algún defecto demostrable en las sustituciones?**
   - No hay defecto de implementación.
   - Sí hay un **hueco de regla** demostrado (H1): una sesión puede perder un patrón y publicarse. No se observó con Replicate.
   - Hay **problemas de catálogo** (H7): 580, 775 y 780.
7. **¿Cuántas sesiones en el mínimo V20?** Con Replicate real, **7 apariciones** (C1: 2; C2: 5), que son **4 sesiones distintas**: U08 S3, U18 S3, U19 S2 y U19 S3.
8. **¿Qué pasó con una segunda adaptación?**
   - En REVISAR_CARGA e INVESTIGAR con margen 0: el intento de seguir reduciendo se rechaza por V20, la sesión se omite y queda **SIN_EFECTO conservada**.
   - En VARIANTE: se aplicaron sustitutos de la misma dosis (0 s).
   - En todos los casos: 0 subidas y linaje correcto (`06`).
9. **¿Alguna incompatibilidad al combinar?**
   - En duración, dosis o pisos, ninguna: lo inválido se rechaza siempre (V20, V24, V16, V8C).
   - **Sí, en patrón (#12b) y músculo (#12a)**: pasan V8C (`07`).
10. **¿La factibilidad V2 describe bien sus límites?**
    - Sí en duración (incluida la suma de cambios), pisos y 4D.
    - No considera el músculo ni impide la combinación #12b, aunque lo declara («el validador final sigue decidiendo»).
    - Imprime la línea «combinar» aunque nada quepa (H8).
11. **¿Qué perfiles nuevos?** R01–R08: los 4 objetivos × principiante e intermedio (`10`).
12. **¿Qué aportan?** 30, 45, 60 y 75 min; casa (desde solo peso corporal) y gimnasio; márgenes de 0 a 228 s; registro incompleto (Aw ≠ Aobs); desmotivación; dificultad con pocas o muchas variantes; un cambio de causa en la semana 2; dos semanas.
13. **¿Resultados sin Replicate?**
    - R02 es NO_FACTIBLE en la generación (GM, 75 min).
    - Con la IA apagada: RESPALDO o SIN_EFECTO, sin cambios.
    - Con el guion V2: en la semana 1 se aplican 5 de los 6 elegibles (R04 queda SIN_EFECTO con margen 6); en la semana 2, los 7.
    - 0 violaciones (`11`, `06`).
14. **¿Está preparada la nueva comparación?** Sí: perfiles congelados, protocolo y dos opciones de presupuesto dentro de USD 3 y 60 predicciones (`09`).
15. **¿Se autorizó y ejecutó la fase con Replicate?** **No.** Está pendiente de tu aprobación del presupuesto. No se ha hecho ninguna llamada.
16. **¿Adaptaciones aplicadas por condición?** No aplica (no se ejecutó). La referencia del PR #29: C1 6 y 5 de 8, C2 7 y 7 de 8.
17. **¿Rechazos o propuestas sin cambios?** En el PR #29: C1 tuvo 15 rechazos y C2 ninguno. En esta evaluación no hubo llamadas reales.
18. **¿Problemas de calidad funcional?** Sí, los mismos en C1 y C2 (`14`):
    - cambios de músculo;
    - variantes de la misma dificultad;
    - VARIANTE resuelta con menos dosis (más frecuente en C2);
    - sesiones agotadas en el mínimo (más frecuentes en C2).
19. **¿Sesiones modificadas fuera de su alcance?** **0.**
20. **¿Algún plan inválido publicado?** **No.**
21. **¿La regresión histórica sigue pasando?** **Sí** (`15`).
22. **¿El CI de GitHub corrió sobre el SHA correcto?** **Sí:** ejecución 41577492912 sobre `51c145b`, en verde. Corrijo mi informe anterior, que decía lo contrario.
23. **¿Qué necesita decisión funcional?**
    - H1: endurecer V8C (opción A/B + D);
    - H3: TOO_DIFFICULT en ejercicios que ya tienen la dificultad mínima;
    - H4/H5: margen de duración al generar y al adaptar, y si saltarse la IA cuando no hay opciones;
    - H6: GM con 75 min, NO_FACTIBLE;
    - H8: retoques del texto V2 (ordenar los sustitutos por músculo).
24. **¿Qué necesita evaluación profesional?**
    - las 8 sustituciones con cambio de músculo;
    - la clasificación de 580, 775 y 780;
    - si reducir la dosis es una respuesta adecuada a «demasiado difícil»;
    - si conviene llevar sesiones al mínimo.
25. **¿Mantener experimental o activar?** **Mantener V2 experimental y apagado.** Antes de considerar activarlo:
    - **(1)** ejecutar la fase con Replicate sobre R01–R08;
    - **(2)** decidir H1 (V8C);
    - **(3)** revisión profesional de §24.

## Fuera del alcance (respetado)

- V2 no se ha activado.
- No se ha modificado V20, V24, V8C, el catálogo, la dificultad, las dosis ni las políticas.
- No se ha activado C2-D/EF.
- No se ha fusionado PR #17, #28 ni #29.
- Ni Railway, ni Neon, ni migraciones remotas, ni Flutter.
- No se ha modificado el short paper.
- No se han sobrescrito resultados.
- No se ha hecho ninguna llamada a Replicate.
- Nada de esto es una evaluación clínica.
