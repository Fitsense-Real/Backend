# Repetición con el oráculo v1.1 · 00 · Congelación previa a la ejecución

**Orden:** «Cierre definitivo de la evaluación integral O01–O20». Aprueba las correcciones D-OR-1 y D-OR-2.

**Qué es esta repetición:**
- Es la misma cohorte O01–O20, con los mismos guiones y la misma versión, reevaluada con el instrumento corregido.
- **No es una cohorte nueva.**
- El resultado v1.0 (139/142) se conserva como antecedente en `../04_INFORME_FINAL.md` (versión del commit `db6f6f5`), `../05_DEFECTOS_Y_CORRECCION.md` y `../evidencia/`. Ninguno de esos ficheros se ha modificado.

## Elementos congelados (antes de ejecutar)

| Elemento | Valor |
|---|---|
| Código de producción | Versión A `f1c6c02`. `git diff f1c6c02 HEAD -- src/main` está vacío |
| Arnés ejecutado | `EvaluacionK20IT` del commit `cb18381`, el mismo de S_O. Se ejecuta en un árbol desacoplado de ese commit |
| Cohorte y guiones | `cohorte/O_PERFILES.csv`, SHA-256 `9b996825a6c146f156b7bd8d585a024cb1ccc44adf2054b11f1badb5de2b2106`. El arnés aborta si no coincide |
| Catálogo | V46 (Flyway 46) |
| P15-C | Apagado |
| Política | PLAN-REV-2.0. Progresión solo en el bloque P (O15–O20) |
| IA | Simulada, prompt V2.1, sin red. **Sin Replicate y sin gasto** |
| **Oráculo oficial de esta repetición** | `herramientas/oraculo_integral_O_v1_1.py`, SHA-256 `49d160d64f100b1521be6030cfd4b991320bbe31e572c8223d3f9e8de6bcc07f`. **Byte a byte igual** al presentado en `db6f6f5` como propuesta; no se ha retocado tras la aprobación |
| Oráculo v1.0 (antecedente, intacto) | `herramientas/oraculo_integral_O.py`, SHA-256 `628be48ed069868e5faffeb35371cad58db4362fe536f81c03935346837b0e4b` |
| Pruebas negativas | `herramientas/pruebas_negativas_oraculo_v1_1.py`, SHA-256 `3c5c3d7f2445a442a04bfe4891bdad9dc3922cafa7342319fa9a3452b4807571` |

## Pruebas negativas del oráculo v1.1

Resultado: `pruebas/pruebas_negativas_v1_1.csv`. **19 de 19 pasan.**

**Método:**
- Se toman copias de las salidas reales de S_O y se introduce en ellas un defecto concreto.
- Se exige que v1.1 dé FALLO en el aspecto esperado, con el motivo esperado.
- Se exige también que v1.0 lo rechace igual (la corrección no relaja esa validación).
- Se exige que ninguna otra celda de otros perfiles cambie.
- Las carpetas originales nunca se tocan.

| Prueba | Corrección vigilada | Defecto introducido | v1.1 | v1.0 |
|---|---|---|---|---|
| N1 | D-OR-1 | O03: la RECUPERACION de 899 sube a 4 series (referencia 3) | FALLO A7 «RECUPERACION por encima de la referencia» | FALLO |
| N2 | D-OR-1 | O14: la RECUPERACION de 253 sube a 4 series (referencia 3) | FALLO A7 (mismo motivo) | FALLO |
| N3 | D-OR-1 | O03: el paso se registra como PROGRESION con la progresión apagada | FALLO A7 «paso de progresion con la progresion apagada» | FALLO |
| N4 | D-OR-1 | O01 (sin pasos): se inyecta una PROGRESION de +1 repetición en C2 | FALLO A7 (mismo motivo) | FALLO |
| N5 | D-OR-1 | O03: misma RECUPERACION, pero con política declarada PLAN-HIST-1 (P-A1 solo ampara REV2) | FALLO A7. O14 también pasa a FALLO: es el único efecto colateral esperado | FALLO |
| N6 | control | O15 (bloque P): PROGRESION en C1, sin dos semanas medidas | FALLO A7 «PROGRESION sin dos semanas medidas» | FALLO |
| N7 | control | O15: la PROGRESION de 342 pasa a +2 repeticiones | FALLO A7 «no es +1 repeticion o +1 serie» | FALLO |
| N8 | D-OR-2 | O19: sin PROGRESION previa en el linaje antes de la reincorporación de C9 | FALLO A8 «sin un paso de PROGRESION posterior a la retirada» | FALLO |
| N9 | D-OR-2 | O19: 853 reaparece **en su linaje** en S3, sin paso de REINCORPORACION | FALLO A8 «vuelve sin paso de REINCORPORACION» | FALLO |
| N10 | D-OR-2 | O19: el reincorporado vuelve con 4 series en lugar de su dosis inicial (3) | FALLO A8 «no con su dosis inicial» | FALLO |
| N11 | D-OR-2 | O19: el reincorporado se coloca primero, no al final | FALLO A8 «no va al final de la sesion» | FALLO |
| N12 | D-OR-2 | O18: el retirado 1302 aparece en una sesión de **otro** linaje que no lo tenía | FALLO A6 «cambio sin orden ni paso registrado». La búsqueda por linaje no abre un hueco: cualquier cambio fuera del linaje sigue detectado | FALLO |

**Controles sin mutar (7):** O03·A7, O14·A7, O15·A7, O19·A8, O18·A6 y O18·A8 dan APROBADO. O01·A7 da NO APLICA.

**Transparencia:** en el primer intento de las pruebas, 2 expectativas estaban mal escritas en la propia prueba. Se corrigieron en la prueba, **no en el oráculo**, y antes de congelar:
- el control de O01·A7 esperaba APROBADO, pero O01 no tiene pasos y lo correcto es NO APLICA;
- N5 no admitía que O14, del mismo bloque y con la misma política, cambiara también.

## Las correcciones no modifican ningún otro criterio

Resultado: `pruebas/no_regresion_criterios_v1_0_vs_v1_1.csv`. Se compararon v1.0 y v1.1 sobre las cuatro corridas existentes:

| Corrida existente | Celdas distintas v1.0 → v1.1 | semanas / adaptaciones / pasos / huella |
|---|---|---|
| O_S (S_O) | Exactamente 3: O03·A7, O14·A7 y O19·A8, de FALLO a APROBADO | Idénticos byte a byte |
| O_Sp (S_O′) | Las mismas 3 | Idénticos |
| O_D (IA apagada) | **0** | Idénticos |
| O_HIST | **0** (su FALLO de O11 bajo HIST, una diferencia de política conocida, se mantiene) | Idénticos |

## Protocolo de la repetición

1. Commit de esta congelación (oráculo v1.1, pruebas y este documento) **antes** de ejecutar.
2. Ejecutar S_O (bloques G y P) y S_O′ (repetición) con `cb18381` desacoplado, los mismos parámetros que la corrida original y salida en una carpeta nueva.
3. Evaluar con v1.1 (oficial de la repetición) y también con v1.0, solo para comparar.
4. Comparar con la corrida original: salidas, huella y matriz.
5. Archivar en `repeticion_v1_1/evidencia/`, con su configuración y SHA-256, y hacer un commit propio.

Los criterios de aceptación no cambian, y los umbrales tampoco.
