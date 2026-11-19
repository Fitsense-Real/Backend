# Prompts de la línea base sin reglas (congelados)

**Fuente:** `herramientas/baseline_replicate.py`.
- Las entradas exactas enviadas, una por llamada, están en `protocolo/PROMPTS_Y_CONTEXTO_BASE.tar.gz`.
- Sus huellas SHA-256 están en `protocolo/HUELLAS_PROMPTS.csv`: 60 prompts, de 26 498 a 228 745 caracteres; el tamaño depende del catálogo elegible del perfil.
- Huellas de las plantillas (SHA-256, 16 primeros caracteres): generación `1c10c72aaffc9550`, adaptación `547fdc03f80b3c19`, esquema `e1392664d3bde15c`.

**Qué no contienen:**
- reglas de FitSense (V1–V27, V24, V25, V8C);
- dosis, límites de series, repeticiones, volumen o descanso;
- patrones obligatorios, división sugerida ni número de ejercicios;
- `min_reps`, `slot` ni `clase_dosis`;
- ejemplos, retroalimentación ni política de reintentos.

**Qué contienen:**
- **Contrato mínimo de la tarea:** elegir de la lista e indicar `exercise_id`. Sin él, las respuestas no se podrían cruzar con el catálogo.
- **Formato de salida:** el esquema JSON del backend con descripciones neutras.

## 1. Plantilla de generación inicial

```
Eres un entrenador personal. Diseña el plan de entrenamiento de la semana del {ini} al {fin} para la persona descrita abajo.
Elige los ejercicios de la lista EJERCICIOS DISPONIBLES (el catalogo de la aplicacion para esta persona) e indica su exercise_id.
Para cada entrenamiento indica la fecha, el enfoque, un nombre, la duracion prevista en minutos y los ejercicios. Para cada ejercicio indica el orden, el tipo de prescripcion (series y repeticiones, o duracion), las series, las repeticiones o los segundos, la carga si procede y el descanso en segundos.
Devuelve UNICAMENTE el objeto JSON del esquema.

PERSONA
{perfil}

EJERCICIOS DISPONIBLES ({n})
{catalogo}
```

## 2. Plantilla de adaptación

```
Eres un entrenador personal. Abajo tienes a una persona, el plan que tenia programado la semana del {ini_k} al {fin_k} y lo que ocurrio en cada sesion.
Propon su plan de entrenamiento para la semana siguiente, del {ini_s} al {fin_s}, ajustandolo como consideres oportuno.
Elige los ejercicios de la lista EJERCICIOS DISPONIBLES (el catalogo de la aplicacion para esta persona) e indica su exercise_id.
Usa el mismo formato: para cada entrenamiento la fecha, el enfoque, un nombre, la duracion prevista y los ejercicios (orden, tipo de prescripcion, series, repeticiones o segundos, carga si procede y descanso).
Devuelve UNICAMENTE el objeto JSON del esquema.

PERSONA
{perfil}

PLAN DE LA SEMANA DEL {ini_k} AL {fin_k}
{plan}

LO QUE OCURRIO
{ocurrio}

EJERCICIOS DISPONIBLES ({n})
{catalogo}
```

En la adaptación:
- `{plan}` lista cada sesión de la semana medida (fecha, día, enfoque, minutos) y sus ejercicios (`[exercise_id N] nombre: series x repeticiones o segundos, descanso`).
- `{ocurrio}` dice, por sesión, si la completó, la hizo en parte (porcentaje) o no la hizo (motivo), con las marcas por ejercicio.
- No se indica qué sesiones señaló el backend ni qué intervención correspondía.

## 3. Esquema de salida (`json_schema`)

```json
{
 "format": {
  "type": "json_schema",
  "name": "weekly_training_plan",
  "schema": {
   "type": "object",
   "additionalProperties": false,
   "properties": {
    "schema_version": {
     "type": "string"
    },
    "plan_name": {
     "type": "string"
    },
    "total_volume": {
     "type": "integer"
    },
    "rationale": {
     "type": "string"
    },
    "workouts": {
     "type": "array",
     "description": "Los entrenamientos de la semana.",
     "items": {
      "type": "object",
      "additionalProperties": false,
      "properties": {
       "scheduled_date": {
        "type": "string"
       },
       "focus_code": {
        "type": "string",
        "enum": [
         "FULL_BODY",
         "UPPER_BODY",
         "LOWER_BODY",
         "PUSH",
         "PULL",
         "LEGS",
         "CORE"
        ]
       },
       "workout_name": {
        "type": "string"
       },
       "expected_duration_minutes": {
        "type": "integer"
       },
       "exercises": {
        "type": "array",
        "description": "Los ejercicios de esa sesion.",
        "items": {
         "type": "object",
         "additionalProperties": false,
         "properties": {
          "exercise_id": {
           "type": "integer"
          },
          "exercise_order": {
           "type": "integer"
          },
          "prescription_type": {
           "type": "string",
           "enum": [
            "SETS_REPS",
            "DURATION"
           ]
          },
          "planned_sets": {
           "type": [
            "integer",
            "null"
           ]
          },
          "planned_reps": {
           "type": [
            "integer",
            "null"
           ]
          },
          "planned_duration_seconds": {
           "type": [
            "integer",
            "null"
           ]
          },
          "target_load_kg": {
           "type": [
            "number",
            "null"
           ]
          },
          "rest_seconds": {
           "type": [
            "integer",
            "null"
           ]
          },
          "notes": {
           "type": [
            "string",
            "null"
           ]
          }
         },
         "required": [
          "exercise_id",
          "exercise_order",
          "prescription_type",
          "planned_sets",
          "planned_reps",
          "planned_duration_seconds",
          "target_load_kg",
          "rest_seconds",
          "notes"
         ]
        }
       }
      },
      "required": [
       "scheduled_date",
       "focus_code",
       "workout_name",
       "expected_duration_minutes",
       "exercises"
      ]
     }
    }
   },
   "required": [
    "schema_version",
    "plan_name",
    "total_volume",
    "rationale",
    "workouts"
   ]
  }
 }
}
```

## 4. Ejemplo real: O13, generación (catálogo abreviado aquí, completo en el envío)

```
Eres un entrenador personal. Diseña el plan de entrenamiento de la semana del 2026-11-02 al 2026-11-08 para la persona descrita abajo.
Elige los ejercicios de la lista EJERCICIOS DISPONIBLES (el catalogo de la aplicacion para esta persona) e indica su exercise_id.
Para cada entrenamiento indica la fecha, el enfoque, un nombre, la duracion prevista en minutos y los ejercicios. Para cada ejercicio indica el orden, el tipo de prescripcion (series y repeticiones, o duracion), las series, las repeticiones o los segundos, la carga si procede y el descanso en segundos.
Devuelve UNICAMENTE el objeto JSON del esquema.

PERSONA
… (118 líneas más, una por ejercicio elegible)
```

## 5. Ejemplo real: O12, ciclo 1, adaptación (catálogo abreviado aquí, completo en el envío)

```
Eres un entrenador personal. Abajo tienes a una persona, el plan que tenia programado la semana del 2026-11-02 al 2026-11-08 y lo que ocurrio en cada sesion.
Propon su plan de entrenamiento para la semana siguiente, del 2026-11-09 al 2026-11-15, ajustandolo como consideres oportuno.
Elige los ejercicios de la lista EJERCICIOS DISPONIBLES (el catalogo de la aplicacion para esta persona) e indica su exercise_id.
Usa el mismo formato: para cada entrenamiento la fecha, el enfoque, un nombre, la duracion prevista y los ejercicios (orden, tipo de prescripcion, series, repeticiones o segundos, carga si procede y descanso).
Devuelve UNICAMENTE el objeto JSON del esquema.

PERSONA
… (602 líneas más, una por ejercicio elegible)
```
