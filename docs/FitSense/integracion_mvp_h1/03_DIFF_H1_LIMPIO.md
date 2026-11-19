# 03 · Diff del PR A (H1 limpio) contra `generation`

**Origen:** `generation` `0974fb3`.
**Destino:** `integration/h1-multiconjunto-mvp`. El commit de código es `983e216`; los documentos van en un commit aparte encima.

| Archivo | Cambio | Líneas |
|---|---|---:|
| `src/main/java/.../ConservationValidator.java` | `ReglaPatron` (CONJUNTO/MULTICONJUNTO), tres constructores (por defecto MULTICONJUNTO; `@Value` con `multiconjunto`), aviso en el log si la regla es CONJUNTO, `reglaPatron()` y `revisarMulticonjunto` | +83 |
| `src/main/resources/application.properties` | `fitsense.adaptation.v8c.pattern-rule=${FITSENSE_V8C_PATTERN_RULE:multiconjunto}` y 3 líneas de comentario | +4 |
| `src/test/.../p2d/ConservacionPatronH1Base.java` | Arnés de la IT U04 (servicios reales, PostgreSQL efímero, IA simulada) | +265 |
| `src/test/.../p2d/ConservacionPatronH1IT.java` | La regla por defecto: rechaza la distribución incorrecta y publica la correcta | +74 |
| `src/test/.../p2d/ConservacionPatronH1ConjuntoIT.java` | `conjunto` explícito: reversión probada | +60 |
| `src/test/.../internal/V8CConservacionPatronH1Test.java` | 32 unitarias, cada caso con las dos reglas | +285 |
| `src/test/.../internal/V8CDiagnosticoSustitucionesTest.java` | 11 unitarias del comportamiento histórico, con `CONJUNTO` explícito | +152 |

**Total:** 7 archivos, +923 líneas y 0 borradas. El diff exacto de `src/main` está en `evidencia/diff_A_src_main.diff`.

## Frente a #34

| Archivo | Diferencia con #34 |
|---|---|
| `ConservationValidator.java` | Solo dos comentarios: «(H1, experimental)» pasa a «(H1)», y «H1 (experimental):» a «H1:» |
| Las 5 pruebas | **Idénticas** byte a byte |
| `application.properties` | Sin la línea de V2 (`fitsense.ai.adaptation.prompt-version=v1`), que en #34 venía de #29 |

## El comportamiento

Fuera del dolor, en una sustitución de VARIANTE:
- **Antes:** cada añadido tenía que ser de **algún** patrón retirado.
- **Ahora:** además, por patrón, **los añadidos no pueden superar a los retirados de ese patrón**.

| Retira | Añade | `conjunto` | `multiconjunto` |
|---|---|---|---|
| un empuje y una tracción | dos empujes | pasa | **rechazada** («añade 2 ejercicio(s) de patrón … y solo retira 1 de ese patrón») |
| un empuje y una tracción | un empuje y una tracción | pasa | pasa (y luego los demás validadores) |

**No se evalúa en:**
- la rama de dolor;
- las retiradas sin añadidos;
- los cambios solo de dosis;
- la generación inicial.

## Lo que el diff **no** tiene

- **Migraciones:** 0.
- **Prompt:** sin cambios.
- **V20, V24, 4D y adherencia:** sin cambios.
- **Catálogo:** sin cambios.
- **V2:** ni su propiedad ni `FactibilidadAdaptacion`.
- **H12:** nada.
- **Laboratorios:** nada.
- **Flutter y short paper:** sin cambios.
