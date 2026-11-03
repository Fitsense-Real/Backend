# 01 · PR candidato H1: multiconjunto como regla por defecto de V8C

**Rama:** `candidate/h1-multiconjunto`, sobre la de #31 (`experiment/adaptation-functional-safety-h1-h12`).
**Estado:** candidata. **No fusionar ni desplegar** hasta que revises la integración final.

## 1. Qué cambia

| Archivo | Cambio |
|---|---|
| `ConservationValidator.java` | El valor por defecto de la regla de patrón pasa de `CONJUNTO` a **`MULTICONJUNTO`**, también en el constructor sin argumentos, en el valor vacío y en `@Value`. Si se usa la regla histórica, cada contexto lo registra con un `WARN`. |
| `application.properties` | `fitsense.adaptation.v8c.pattern-rule=${FITSENSE_V8C_PATTERN_RULE:multiconjunto}` y un comentario |
| `ConservacionPatronH1Base`, `ConservacionPatronH1IT`, `ConservacionPatronH1ConjuntoIT` | **Nueva prueba de integración permanente** del caso U04, que corre en CI (`*IT`, perfil `integration`) |
| `V8CDiagnosticoSustitucionesTest` | Fija `ReglaPatron.CONJUNTO`: documenta el comportamiento histórico |
| `V8CConservacionPatronH1Test.porDefecto` | Espera MULTICONJUNTO por defecto, y CONJUNTO solo con `"conjunto"` |
| `ExperimentoKora20IT` | Solo el comentario de la propiedad del laboratorio |

**El algoritmo multiconjunto es el de #31, sin cambios:** cada añadido reemplaza a un retirado **distinto** del mismo patrón.

**Sin cambios en:** la rama de dolor, V20, V24, 4D, las fórmulas de adherencia, la generación, el catálogo, las migraciones, el prompt, V2 (sigue `v1`) y H12-A/B/C/D (siguen apagadas, y solo existen en la rama de #33).

## 2. Diferencia funcional

**Única diferencia observable:** fuera del dolor, una VARIANTE que añade **más ejercicios de un patrón de los que retira de ese patrón** se rechaza.

Ejemplo (U04): la IA cambia el empuje A y el tirón B por **dos** empujes. La sesión pierde su tirón.
- **Antes:** se publicaba si el tirón seguía en otra sesión y V15 no lo veía.
- **Ahora:** V8C lo rechaza («añade 2 ejercicio(s) de patrón X y solo retira 1 de ese patrón»), y el reintento o la conservación siguen el flujo normal.

**Impacto medido** (en `validacion_h1_h12` §04, §05 y §12, y repetido aquí, `03`):
- 123 de 123 sustituciones reales siguen siendo válidas;
- 0 sesiones publicadas reales cambian;
- 960 perfiles idénticos;
- REPLAY idéntica salvo marcas de tiempo.

## 3. Cómo volver a la regla anterior (reversión)

**Sin desplegar código nuevo:**
1. Definir la variable de entorno `FITSENSE_V8C_PATTERN_RULE=conjunto` en el servicio, o la propiedad `fitsense.adaptation.v8c.pattern-rule=conjunto`.
2. Reiniciar la aplicación.
3. Comprobar en el arranque el log `V8C: regla de patron HISTORICA activa: CONJUNTO (compatibilidad o reversion de H1).`.

**Con código:** revertir el commit de la candidata. La regla vuelve a ser `conjunto` por defecto, y el multiconjunto sigue disponible con la propiedad.

**No hay datos que migrar:** la regla solo decide si se publica una propuesta. Los planes ya publicados no cambian.

**Prueba de la reversión:** `ConservacionPatronH1ConjuntoIT` arranca con `conjunto` y comprueba que la propuesta incorrecta vuelve a publicarse. Corre en CI.

## 4. Condición de «lista»

| Condición | Estado |
|---|---|
| Unitarias sin fallos | **Sí**: 1.105 ejecutadas, 0 fallos, 0 errores, 1 omitida (*opt-in*, `H1ImpactoHistoricoTest`) |
| Integración sin fallos | **Sí**: 495 ejecutadas (489 + 6 nuevas), 0 fallos, 0 errores, 37 omitidas (*opt-in*, las mismas que antes) |
| 960 perfiles | **Idénticos** (hashes de referencia) |
| REPLAY de 100 semanas | **Pasa**. Solo cambian marcas de tiempo |
| Pruebas históricas con `conjunto` explícito | **Sí**, sin quitar cobertura |
| CI del commit final | ver `03` §5 |
