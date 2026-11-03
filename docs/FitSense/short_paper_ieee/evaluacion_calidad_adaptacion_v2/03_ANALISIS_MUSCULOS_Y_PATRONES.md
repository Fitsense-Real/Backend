# Análisis de músculos y patrones en las sustituciones

**Fuentes:**
- `02_AUDITORIA_SUSTITUCIONES.csv`: las 57 sustituciones de la etapa C (PR #28) y de C1 r1, C2 r1, C2 r2 y C1 r2 (PR #29);
- el catálogo real, con las 46 migraciones aplicadas a una base local efímera: `evidencia/catalogo_exercises_v44_2.csv` (1.219 ejercicios activos, como en el laboratorio);
- `evidencia/auditoria_sustituciones.py`.

**La revisión es del desarrollador sobre los atributos del catálogo. No es una evaluación clínica ni profesional.**

## 1. Método

**Emparejamiento:**
- V8C no empareja ejercicios: compara conjuntos (`04`).
- Para auditar, cada ejercicio retirado se empareja con el que ocupa **su misma posición** en la sesión devuelta (la IA devuelve la prescripción completa y en orden).
- Para no depender de esa interpretación, se calcula además el **mínimo de cambios de músculo con el mejor emparejamiento posible** dentro de cada patrón.

**Resultado:** el mínimo coincide con el conteo por posición en **todas** las sesiones. **Los cambios de músculo no son un efecto del emparejamiento**: vienen del conjunto de sustitutos elegido.

**Atributos comparados:**
- patrón (`movement_pattern`);
- músculo principal (`target_muscle`);
- sinergista;
- secundarios (`secondary_muscles`);
- zona;
- equipamiento;
- aislamiento;
- dificultad;
- dosis;
- duración de la sesión.

## 2. Las seis observaciones del informe anterior (C1/C2) y dos más de la etapa C

Todas son VARIANTE_MAS_SENCILLA_O_CARGA (motivo TOO_DIFFICULT), con la misma dosis y el mismo patrón. Todas fueron aceptadas por V8C y por V1–V25 y publicadas.

| # | Corrida | Caso | Original → sustituto | Patrón | Músculo principal (sinergista; secundarios) | Equipo | Dificultad | Clasificación |
|---|---|---|---|---|---|---|---|---|
| 1 | C1 r1 | U04 S2 (GM) | **580** «Curl inclinado con mancuerna» / *dumbbell incline breeding* → **313** *bench dip (knees bent)* | HORIZONTAL_PUSH = | pectorales (hombros; hombros, tríceps) → **tríceps** (pecho; pecho, hombros) | mancuerna → banco | 1 → 1 | **Q3 + Q2**, Q5 |
| 2 | C2 r2 | U04 S2 (GM) | **580** → **129** *band close-grip push-up* | = | pectorales → **tríceps** (pecho; pecho, hombros) | mancuerna → banda | 1 → 1 | **Q3 + Q2**, Q5 |
| 3 | C2 r1 | U09 S2 (FU) | **775** *elbow lift – reverse push-up* → **313** | HORIZONTAL_PUSH = | **dorsal alto** (tríceps; tríceps, hombros) → **tríceps** | peso corporal → banco | 1 → 1 | **Q3**, Q5 |
| 4 | C2 r1 | U09 S2 (FU) | **780** *dumbbell upright row* → **119** *band one arm twisting seated row* | HORIZONTAL_PULL = | **deltoides** (trapecio; trapecio, bíceps) → **dorsal alto** (bíceps; bíceps, hombros) | mancuerna → banda | 2 → 1 | **Q3**, Q5 |
| 5 | C2 r2 | U09 S2 (FU) | 780 → 119 | = | ídem | ídem | 2 → 1 | **Q3**, Q5 |
| 6 | C1 r2 | U14 S2 (PP) | **933** *kneeling push-up* → **318** *body-up* | HORIZONTAL_PUSH = | pectorales (tríceps; tríceps, hombros) → **tríceps** (pecho; pecho, hombros) | peso corporal = | 1 → 1 | **Q2**, Q5 |
| 7 | C (PR #28) | U09 S2 (FU) | 775 → **73** *chest dip assisted (knees)* | = | dorsal alto → **pectorales** (tríceps; tríceps, hombros) | peso corporal → máquina | 1 → 1 | **Q3**, Q5 |
| 8 | C (PR #28) | U09 S2 (FU) | 780 → **300** *bench pull-ups* | = | deltoides → **dorsal ancho** (bíceps; bíceps, antebrazos) | mancuerna → banco | 2 → 1 | **Q3**, Q5 |

**Clases:**
- **Q1:** coherente.
- **Q2:** válida, pero funcionalmente discutible.
- **Q3:** clasificación o traducción discutible del catálogo.
- **Q4:** posible defecto de validación o de selección.
- **Q5:** requiere valoración profesional.

### Justificación por ejercicio implicado

- **580** («Curl inclinado con mancuerna»):
  - El nombre en inglés del catálogo (*dumbbell incline breeding*) y su músculo principal (pectorales) describen un **aperturas inclinadas** (horizontal adduction), no un curl. **El nombre español es incorrecto**; ya estaba anotado en H10 y P4 del PR #28.
  - Además, una apertura es un movimiento de aducción horizontal, casi de aislamiento, que el catálogo clasifica como HORIZONTAL_PUSH. Sustituirla por un fondo de tríceps (313) o una flexión cerrada (129) conserva el patrón según el catálogo, pero **cambia el énfasis de pectoral a tríceps**. El pectoral se mantiene como sinergista.
  - Por eso es Q3 (nombre y clasificación) y Q2 (énfasis distinto). **No se corrige el catálogo por el nombre** (lo prohíbe la orden).
- **775** (*elbow lift – reverse push-up*):
  - Su músculo principal es el dorsal alto, pero está clasificado como HORIZONTAL_PUSH.
  - Un «push-up invertido» que eleva el tronco apoyando los codos trabaja sobre todo la musculatura posterior del hombro y la espalda, así que **la clasificación de empuje es discutible**.
  - Con esa clasificación, cualquier empuje horizontal (fondos, *dips*) pasa por «mismo patrón», aunque el énfasis sea opuesto. Es Q3.
- **780** (*dumbbell upright row*, remo al mentón):
  - Clasificado como HORIZONTAL_PULL, con deltoides como principal.
  - El remo al mentón se suele describir como tirón vertical con énfasis en deltoides y trapecio, así que **la clasificación es discutible**.
  - Sustituirlo por un remo horizontal (119) o una dominada en banco (300) cambia el énfasis a la espalda. Es Q3.
  - La dificultad baja de 2 a 1, lo que sí es coherente con VARIANTE.
- **933 → 318:**
  - Las dos están bien clasificadas como empuje horizontal. El sustituto enfatiza el tríceps, con el pecho como sinergista, y tiene la misma dificultad (1).
  - Es una **variante válida pero de otro énfasis**, y no es «más sencilla» según el único atributo disponible. Es Q2.

**Corrección del PR #28 (sin modificarlo):** H10 afirmaba que las 13 sustituciones de la etapa C conservaban patrón **y músculo**. **No es exacto.** Con el catálogo real, 2 de las 13 (775 → 73 y 780 → 300) cambian el músculo principal, y es inevitable con el conjunto elegido (mínimo = 2). Las 13 sí conservan el patrón y no suben la dificultad.

## 3. El resto de las sustituciones

Las 57 son de VARIANTE_MAS_SENCILLA_O_CARGA.

| Clase | Etapa C | C1 (r1 + r2) | C2 (r1 + r2) | Qué significa |
|---|---:|---:|---:|---|
| Q1 | 7 | 13 | 13 | Mismo patrón y músculo, dificultad menor (2 → 1) |
| Q2 por dificultad igual | 4 | 8 | 4 | Mismo patrón y músculo, pero **misma dificultad**. Contando también las Q2/Q3 con cambio de músculo, hay 21 sustituciones con dificultad igual. En 19 de esas 21 la original ya tenía la dificultad mínima (1): **no existe variante más sencilla según ese atributo**. Las otras 2 son 2 → 2 |
| Q2 por cambio de músculo | 0 | 1 | 0 | #6 |
| Q3 | 2 | 1 | 4 | #1–#5, #7 y #8 |
| Q4 | 0 | 0 | 0 | Ninguna sustitución real incumple lo que V8C dice comprobar (`04`) |
| **Total** | **13** | **23** | **21** | |

**Lectura:**
- **Ninguna sustitución real cambia de patrón ni sube la dificultad.** Las 57 son técnicamente válidas.
- **8 de 57 (14 %) cambian el músculo principal** (7 pares distintos: 780 → 119 aparece dos veces). Siete de las 8 afectan a los ejercicios 580, 775 y 780, de clasificación discutible.
- **21 de 57 (37 %) no son más sencillas según el catálogo**, porque la original ya tenía la dificultad mínima. Para TOO_DIFFICULT en esos ejercicios, la única simplificación disponible con las reglas vigentes es bajar la dosis, que en GAIN_MUSCLE no está permitida (4D).
- **No hay diferencia de calidad atribuible a V2.** C1 y C2 producen el mismo tipo de sustituciones porque ambos reciben la misma lista de SUSTITUTOS ADMITIDOS. V2 añade «misma dosis», pero no filtra por músculo (`04`, `08`).

## 4. Equipamiento

Todos los sustitutos están en `available_exercises`, ya filtrado por el equipamiento y la ubicación del perfil.

Hay cambios de implemento dentro de lo disponible (mancuerna → banda o banco). No se valora si son adecuados (Q5).

## 5. Revisión profesional (Q5)

Requieren valoración de un profesional del ejercicio:
- las 8 observaciones de §2;
- y, como muestra, las Q2 por dificultad igual.

Este documento **no** sustituye esa valoración.
