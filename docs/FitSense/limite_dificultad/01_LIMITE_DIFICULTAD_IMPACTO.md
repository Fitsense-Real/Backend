# Límite de dificultad: diagnóstico, corrección candidata e impacto medido

**Estado:** candidato **APAGADO** (`fitsense.adaptation.difficulty-guard=off`). **Pendiente de tu revisión.**
- **No se ha cambiado la política vigente:** sin activar, el comportamiento es idéntico al de `generation`.
- **Rama:** `experiment/difficulty-guard`, sobre `generation` (`76e15c5`, que incluye H1 y los textos).
- **Entorno:** sin Replicate, sin Neon y sin Railway. H12-A y V2 siguen apagados.

## 1. El problema (hoy, en `generation`)

Ante «demasiado difícil» (`VARIANTE_MAS_SENCILLA_O_CARGA`) y ante dolor, el sistema puede publicar un sustituto con **más** dificultad registrada que el original:

| Vía | Qué la limita hoy | ¿Puede subir la dificultad? |
|---|---|---|
| IA, VARIANTE | El prompt pide sustitutos de dificultad ≤; V8C exige el mismo patrón y, con H1, la misma distribución | **Sí**, si la IA propone otro ejercicio elegible: ningún validador compara la dificultad |
| IA, dolor | V8C: sustituir o retirar, sin más ejercicios ni más volumen | **Sí** |
| **SUST-SEG-1** (sustitución determinista por seguridad, también en RESPALDO) | Elegibilidad, slot, tipo, zona, brazo y días contiguos; ordena por zona, patrón, músculo e **id** | **Sí, y con frecuencia** (§3) |

El único tope es la elegibilidad: el nivel máximo de la persona. Por eso a los principiantes no les afecta (todo su catálogo tiene dificultad 1).

## 2. La corrección candidata (apagada)

| Punto | Con `difficulty-guard=on` |
|---|---|
| V8C, VARIANTE | Cada añadido se empareja con un retirado **distinto** del **mismo patrón** con dificultad registrada **≥**. Ordenando ambos lados de mayor a menor, el emparejamiento existe si y solo si ningún añadido supera al retirado de su posición. Si no existe, se rechaza con un mensaje explícito y el flujo sigue con reintento o conservación |
| V8C, dolor | Lo mismo **sin exigir el patrón**. La ficha del ejercicio que dolió, que ya no es elegible, sale de las exclusiones de seguridad |
| SUST-SEG-1 | Solo candidatos con dificultad **≤** la del original. Si no hay ninguno, se informa como cualquier «sin sustituto» de hoy: no se publica una sustitución que no cumpla |
| Resto | Sin cambios: otras intervenciones, dosis, V20, V24, 4D, generación y prompt |

**Archivos tocados:**
- `ConservationValidator`: bandera, propiedad y `revisarDificultad`;
- `SafetySubstitutionPolicy`: sobrecargas con el límite;
- `AdaptedWeekProducer` y `PublishedPlanSafetyRepairService`: pasan la bandera del validador;
- `application.properties`.

**No hay migraciones.**

**Reversión:** está apagado por defecto. Si se activa, se vuelve atrás con `FITSENSE_DIFFICULTY_GUARD=off`.

## 3. Impacto medido

### 3.1 SUST-SEG-1 en los planes reales de los 960 perfiles

`ImpactoLimiteDificultadIT`, *opt-in*: para **cada** ejercicio colocado (19.562) se simuló que dolía y se preguntó a SUST-SEG-1, con los elegibles reales del perfil, el enfoque y los días contiguos.

| Sin límite (hoy) → con límite | Colocaciones |
|---|---:|
| igual → igual | 13.438 |
| menor → menor | 3.264 |
| **mayor → igual** | **2.443** |
| **mayor → menor** | **2** |
| **mayor → sin sustituto** | **24** |
| sin sustituto → sin sustituto (ya hoy) | 391 |

**Hoy SUST-SEG-1 elige un sustituto más difícil en 2.469 de 19.562 colocaciones (12,6 %) y en 573 de 960 perfiles.**

| Nivel | Colocaciones | Hoy sube | Con límite, sin sustituto |
|---|---:|---:|---:|
| Principiante | 6.366 | 0 (0 %) | 0 |
| Intermedio | 6.596 | 1.302 (19,7 %) | 13 (0,2 %) |
| Avanzado | 6.600 | 1.167 (17,7 %) | 11 (0,2 %) |

**Por equipamiento:** sube del 16 % al 21 % en intermedios y avanzados, sea en casa o en gimnasio.

**Por objetivo:**

| Objetivo | Hoy sube |
|---|---:|
| GM | 15,5 % |
| FU | 13,9 % |
| GF | 11,1 % |
| LW | 9,7 % |

**Ejemplos frecuentes de subida** (el desempate por id favorece al 56):

| Original (dificultad) | Sustituto (dificultad) | Veces |
|---|---|---:|
| 1098 *flexión en pared* (1) | **56 *flexión arquero*** (2) | 64 |
| 775 *elevación de codo y push-up invertido* (1) | 56 *flexión arquero* (2) | 166 |
| 1156 *remo sentado con banda* (1) | 118 *remo bajo a una mano con banda* (2) | 76 |

**El coste del límite:** 24 colocaciones (22 perfiles) se quedan sin sustituto. **Todas son el 135** (*remo de deltoides traseros con banda*, dificultad 1, HORIZONTAL_PULL), en casa con mancuernas y bandas, nivel intermedio o avanzado. Hoy se sustituye por uno de dificultad 2. Con el límite, «sin sustituto» pasa de 391 a 415 casos (+6 %) y se trata como hoy: sin publicar una sustitución que no cumple.

**Exposición en VARIANTE** (lo que podría hacer la IA): en intermedios, 3.703 de 6.596 colocaciones tienen al menos una alternativa elegible del mismo patrón **más difícil**; en avanzados, 4.735 de 6.600. Solo el prompt la frena.

### 3.2 Las 123 sustituciones históricas reales (VARIANTE)

Revalidadas con el V8C real con el límite encendido (`evidencia/historico_123_con_limite.csv`): **0 de 90 sesiones cambian**. Las 59 sesiones reales con sus 123 sustituciones siguen válidas, porque ninguna subía la dificultad.

### 3.3 REPLAY de 100 semanas con el límite encendido

**Cambia exactamente un caso** (`evidencia/replay_con_limite.diff`):

| | Hoy | Con límite |
|---|---|---|
| N08, semana 2 (dolor) | La IA (simulada en la campaña P123) sustituye **791** *sentadilla con apoyo* (dif. 1) por **124** *sentadilla dividida a una pierna con banda* (dif. **2**) → APLICADA, AI | Se rechaza en los 3 intentos → RESPALDO: SUST-SEG-1 coloca **1003** *prensa alterna de piernas* (dif. 1, mismo patrón) → CONSERVED |

Las semanas siguientes de ese participante heredan el 1003 en lugar del 124. El resto de la REPLAY es idéntico: decisiones, adherencia y linaje. **Es un ejemplo real de lo que el límite evita:** ante dolor, publicar un ejercicio más difícil que el que dolió.

### 3.4 Regresión

| Bloque | Límite apagado (por defecto) | Límite encendido |
|---|---|---|
| Unitarias | 1.127 ejecutadas, **0 fallos**, 0 omitidas | 1.127, **0 fallos**, 0 omitidas |
| Integración | 496 ejecutadas, **0 fallos**, 37 omitidas (*opt-in*) | 496, **0 fallos**, 37 omitidas |
| 960 perfiles | **Idénticos** (`d109a3da…`, `d227848b…`) | No cambian (el límite no actúa en la generación) |
| REPLAY | Igual (solo marcas de tiempo) | **1 semana distinta** (§3.3) |

Las pruebas nuevas son `LimiteDificultadTest` (9) y `LimiteDificultadConfiguracionIT` (1); además, `ConservacionPatronH1IT` comprueba que el límite viene apagado por defecto.

**Con el límite encendido no falla ninguna prueba convencional:** ninguna dependía de una sustitución más difícil.

## 4. Decisión pendiente (tuya)

1. **¿Activar el límite?** Medido:
   - evita 2.469 subidas de SUST-SEG-1 y la de N08;
   - a cambio, 24 colocaciones (el 135 en casa) se quedan sin sustituto.
2. **Si se activa, ¿qué hacer con el 135?** Opciones:
   - **(a)** aceptarlo: hoy ya hay 391 casos sin sustituto con el mismo tratamiento;
   - **(b)** permitir **+1** de dificultad solo cuando no haya alternativa de igual o menor (más código, a medir);
   - **(c)** revisar el catálogo de remos con banda.
3. **¿Desempate por dificultad ascendente en SUST-SEG-1, además o en lugar del límite?** Hoy desempata por id, y por eso sale tanto el 56. Ordenar por dificultad dentro del mismo patrón resolvería la mayoría sin dejar a nadie sin sustituto. Es un cambio de la regla SUST-SEG-1; no lo he implementado.
4. **Revisión profesional:** que una dificultad menor en el catálogo no implica que el ejercicio sea más fácil en la práctica ya está en el instrumento H12-A. Sigue sin respuesta y no he inferido nada.
