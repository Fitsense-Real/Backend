# 02 · Plan de integración selectiva

## 1. Ramas limpias

| PR | Rama | Base | Contenido |
|---|---|---|---|
| **A** | `integration/h1-multiconjunto-mvp` | **`generation` (`0974fb3`)** | H1: validador, propiedad, pruebas y documentación (`01`–`04`, `08`) |
| **B** | `integration/adjustment-messages-mvp` | **PR A** mientras ambos estén en revisión | Textos de resultado y de intención, sus pruebas y documentación (`05`–`07`, `09`–`12`) |

**Se integraron archivos uno a uno; no se fusionó ninguna rama anterior:**
- **`ConservationValidator.java`:** se tomó la versión de #34, ya que la base de #31 es idéntica a `generation`. Se corrigieron solo dos comentarios que todavía decían «experimental».
- **`application.properties`:** se añadieron solo las 4 líneas de H1. La línea de V2 (`prompt-version`) **no** se incluyó.
- **Pruebas:** las tres de la IT U04 y las dos unitarias de V8C, tal como están en #34.
- **No se incorporaron:**
  - `FactibilidadAdaptacion`, `SessionFixPromptBuilder` V2 ni la propiedad de V2;
  - los cambios de H12 en V8C y en el prompt;
  - `ExperimentoKora20IT` y los demás laboratorios;
  - `H1ImpactoHistoricoTest`;
  - la evidencia de #28–#33.

## 2. Cómo terminan integrados

1. **Revisar y fusionar el PR A en `generation`.** Su diff contra `generation` es exactamente H1 (`03`).
2. **Cambiar la base del PR B a `generation`.** Desde ese momento su diff muestra solo los textos. Si se prefiere, el PR B se puede rebasar sobre `generation` (es una rama creada para esto y no tiene más usuarios).
3. **Revisar y fusionar el PR B.**
4. **No fusionar #28–#35.** Quedan como evidencia, o se cierran tras fusionar A y B:
   - #31 y #34 quedan sustituidos por A;
   - #35 queda sustituido por B;
   - #28–#30, #32 y #33 siguen como experimentos y documentación.

## 3. Lo que no cambia en ninguna de las dos

- **Configuración:** V2 sigue `v1` (no se toca su propiedad, que ni siquiera existe en `generation`), H12 no existe en estas ramas, y las políticas del PR #17 no se tocan.
- **Reglas:** no hay migraciones, ni cambios en V20, V24, 4D, adherencia, catálogo o prompt.
- **Mensajes históricos:** no se reescriben. Los cambios de texto solo afectan a lo que se escriba a partir de la fusión.

## 4. Reversión

| PR | Cómo revertir |
|---|---|
| A | `FITSENSE_V8C_PATTERN_RULE=conjunto` y reiniciar, sin código. Cada contexto registra `V8C: regla de patron HISTORICA activa: CONJUNTO (compatibilidad o reversion de H1).` |
| A (código) | Revertir el commit `983e216`: vuelve `ConservationValidator` de `generation`, sin la propiedad |
| B | Revertir su commit de código. No hay datos que migrar; los textos ya guardados se quedan como están |
