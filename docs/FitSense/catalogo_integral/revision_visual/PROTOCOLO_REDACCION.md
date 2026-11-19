# Protocolo de redacción de correcciones de texto (v1)

Objetivo: para cada ejercicio de tu lote con `nombre_ok = NO` o `instr_ok = NO` en tu `revision.tsv`, redactar el
texto corregido que describe **exactamente el movimiento del GIF**. Estos textos se publicarán en la app (las
instrucciones) y en el contexto de la IA (el nombre), así que deben ser correctos, seguros y sobrios.

## Reglas

1. **Solo lo que el GIF muestra.** Posición inicial, implemento, trayectoria y vuelta. Si no ves algo (agarre, ancho,
   altura exacta), no lo afirmes: usa formulaciones neutras («sujeta las asas», «a la altura que permita…»).
   Vuelve a mirar la hoja o genera fotogramas grandes con zoom.py antes de escribir. Si después de mirar no puedes
   describir el movimiento con seguridad, NO redactes: pon `PENDIENTE` y explica por qué en `notas`.
2. **Nada que contradiga las banderas o el GIF.** No escribas «túmbate en el suelo», «cuélgate», «brazos por encima de
   la cabeza», «apoya la rodilla» si el GIF no lo hace. Si el GIF SÍ lo hace, dilo con claridad.
3. **No cambies el ejercicio** ni la dosis: no menciones series, repeticiones, tiempos ni pesos concretos. Termina con
   «Repite el número de repeticiones indicado.» (SETS_REPS) o «Mantén la duración indicada.» (DURATION) según la
   prescripción de la ficha; para alternos, «Alterna los lados» si el GIF alterna.
4. **Estilo:** español neutro, segunda persona del singular, imperativo («Siéntate», «Sujeta»). 3 a 6 pasos, una
   oración por paso, sin tecnicismos innecesarios. Sin emojis ni marcas comerciales.
5. **Nombre** (`nombre_nuevo`): solo si `nombre_ok = NO`; si no, `=`. Español natural que describa el GIF
   (implemento + posición + acción cuando ayude). No traduzcas literalmente un nombre de origen erróneo.
6. **Instrucciones** (`instrucciones_nuevas`): solo si `instr_ok = NO`; si no, `=`. Pasos separados por ` || `
   (espacio, dos barras verticales, espacio).
7. Si `instr_ok = DUDA` o `nombre_ok = DUDA`, no redactes nada para ese campo (`=`).

Ejemplo (#1093, GIF: de pie, elevación alterna de rodilla con braceo, sin fase de vuelo):
`1093	Elevación alterna de rodillas con braceo	Ponte de pie con los pies separados al ancho de las caderas. || Eleva una rodilla hacia el pecho mientras balanceas el brazo contrario. || Baja el pie al suelo con control y repite con la otra pierna. || Mantén un ritmo constante y el tronco erguido. || Mantén la duración indicada.	-`

## Salida

Escribe `correcciones_texto.tsv` en tu carpeta de lote, UTF-8, TAB, sin tabuladores ni saltos de línea internos, con la
cabecera exacta:

```
id	nombre_nuevo	instrucciones_nuevas	notas
```

Una fila por cada id con `nombre_ok = NO` o `instr_ok = NO` (ni más ni menos). Verifica con un script el recuento y que
ningún texto nuevo contiene las expresiones prohibidas del punto 2 cuando el GIF no lo justifica.
