# Revisión de V39 (43 ejercicios)

**Estado:** el responsable del proyecto aprobó la **revisión técnica** de los 43 ejercicios el 30/10/2026.
- Fuente: `FitSense_V39_Dictamen_Tecnico_Aprobado_2026-10-30.xlsx` (en este directorio).
- Es una **revisión técnica aceptada por el responsable del proyecto**, partiendo de una revisión visual asistida por IA.
- **No** es una doble revisión humana independiente ni un consenso de dos revisores.
- No se registran nombres ni veredictos de revisores. En `revision_humana_v39.tsv`, las columnas `revisor_*`, `veredicto_*` y `consenso` siguen vacías.

| Decisión | Ejercicios | Estado |
|---|---|---|
| APROBAR | 35 | Dictamen aceptado, sin cambios respecto a V39 |
| CORREGIR | 6: 302, 306, 308, 1072, 1106, 813 | Aplicados en `V39__catalog_functional_equipment.sql` |
| REVISAR GIF | 2: 1113, 849 | Pendientes, sin cambios nuevos |

`revision_humana_v39.tsv` tiene dos columnas nuevas: `dictamen_responsable` y `estado_tras_dictamen`.

## Ajustes aplicados

| Ej. | Qué dice el GIF | Qué se hizo |
|---|---|---|
| 302, 306, 308 | Toalla pasada por un barrote de la espaldera | Se mantiene la espaldera como requisito. El primer paso de las instrucciones ahora dice cómo poner la toalla y que hay que comprobar que la espaldera está fijada y la toalla no resbala. No se crea un código «toalla»: cualquier toalla resistente sirve y exigirla no añade seguridad. Lo obligatorio es el soporte. Sin espaldera declarada no son elegibles, tampoco con solo peso corporal |
| 1072 | Toalla en el borde de una puerta | Se mantiene el punto de anclaje como requisito, y el implemento se llama ahora «Punto de anclaje firme (poste fijo o anclaje de puerta)». El primer paso de las instrucciones excluye el picaporte y la puerta sin anclaje, y pide comprobar el anclaje antes de cargar el peso |
| 813 | **Poste vertical sobre una base**, no espaldera | Nuevo implemento `vertical pole`, «Poste vertical fijo (barra de bandera)», de gimnasio. Requisito: `1:body weight,2:vertical pole`. No se equipara con la espaldera. Además 813 sigue **retenido** en el backend (`ExternalCatalogService`): no llega a ningún plan |
| 1106 | Flexión dinámica con giro a plancha lateral | Sin cambios de datos: pecho, repeticiones (SETS_REPS) y nivel 2 **PROVISIONAL**. No hay ningún evento en `exercise_difficulty_approvals` |

## Pendientes de verificación visual (no bloquean los otros 41)

| Ej. | Qué falta | Tratamiento conservador |
|---|---|---|
| 1113 pike-to-cobra push-up | Confirmar el movimiento predominante (pecho u hombros), la dificultad y la dosis | Quedan los valores de V39 (pecho, nivel 1). Los fotogramas muestran perro boca abajo → flexión → cobra. Ningún atributo de seguridad (suelo, inversión) cambia |
| 849 ez barbell spider curl | Confirmar el banco exacto | Se exige banco Scott, que es lo que muestran los fotogramas del GIF 1628 (brazos sobre el respaldo del Scott, de pie). Es la opción más restrictiva: nadie sin ese banco lo recibe. Falta la confirmación humana |

## Dificultad

La dificultad de 1106 (2) sigue siendo provisional:
- debe resolverse con el instrumento de dificultad de FitSense;
- cuando haya evaluación, se registra con el contrato de V27, en una migración nueva.

## Si una revisión posterior cambia algo

- Si V39 ya se ha aplicado en algún entorno persistente, el cambio va en una **V40**. V39 no se edita.
- Hay que repetir las pruebas afectadas: `CatalogoV39IT`, las simulaciones y el CI.
