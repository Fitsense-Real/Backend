# Revisión visual completa (cierre del catálogo)

## 1. Alcance

Entraron los **1.219 activos**, sin excepción. Ya no se distingue entre VERIFICADO, CORREGIDO y PENDIENTE: los 552 verificados y los 261 corregidos de la auditoría integral se revisaron otra vez desde cero. Además se inventariaron los **157 inactivos**.

| Paso | Quién | Qué | Resultado |
|---|---|---|---|
| Línea base | — | Catálogo V50 congelado, con sha256 en `cierre/linea_base/SHA256SUMS`, y procedencia de cada atributo por migración (V1–V50) | 1.376 filas |
| Hojas | Script | Todos los fotogramas del GIF original más la imagen fija, por ejercicio (`hojas_completas.py`). Normalmente 12 fotogramas; algunos tienen 18, 24 o 36 | 1.219 hojas |
| R2: segunda revisión | 20 revisores independientes (IA), lotes intercalados L01–L20 | Cada atributo final contra la animación completa, con el protocolo `cierre/PROTOCOLO_REVISION_2.md`. Sin acceso a revisiones anteriores | 1.219 filas de 33 columnas |
| V3: verificación | 18 verificadores independientes (IA): lotes V01–V09, W01–W07, X01 y Y01 | Cada propuesta de R2 contra el GIF (`cierre/PROTOCOLO_VERIFICACION_3.md`). Decisiones: ACEPTAR, RECHAZAR, MEJORAR o NO_DECIDIBLE | 717 propuestas |
| Adjudicación | Coordinador (IA) | Reglas de `VALIDACION_SEGURIDAD.md` §1. Las retiradas de seguridad se deciden una a una, y el coordinador revisó personalmente los GIF de 503, 853 y 1093 | 561 cambios en 442 ejercicios; 16 en V53 |

## 2. Resultados de la R2

- Identidad: 1.192 CLARA y 27 AMBIGUA.
- Confianza: 830 ALTA, 371 MEDIA y 18 BAJA.

Propuestas, 965 en total:

| Tipo | Propuestas | Tipo | Propuestas |
|---|---|---|---|
| INSTRUCCIONES | 340 | IDENTIDAD | 27 |
| DIFICULTAD_BAJAR | 248 | PATRON | 25 |
| DIFICULTAD_SUBIR | 71 | IMPACTO | 20 |
| BANDERA_QUITAR | 60 | PRESCRIPCION | 18 |
| NOMBRE | 42 | ZONA_OBJETIVO | 12 |
| BANDERA_ANADIR | 35 | ESTIRAMIENTO | 5 |
| APOYO_SUSPENDIDO | 33 | | |
| EQUIPO | 29 | | |

De las 323 correcciones de instrucciones aplicadas, 286 recaen en textos que V49 no había tocado. De esos 286, la auditoría anterior había marcado como VERIFICADO 173, como PENDIENTE_PROFESIONAL 70, como PENDIENTE_VISUAL 33, como CORREGIDO 5, como CONTENIDO 4 y como PENDIENTE_TECNICO 1. **La primera auditoría dio por buenos textos que no describían el GIF.** Lo corrige V52.

## 3. Resultados de la V3

Sobre 717 propuestas, todas menos DIFICULTAD_BAJAR, que nunca se aplica:

| Decisión | Propuestas |
|---|---|
| ACEPTAR | 604 |
| MEJORAR (texto reescrito por el verificador) | 62 |
| RECHAZAR | 44 |
| NO_DECIDIBLE | 7 |

## 4. Resultado de la adjudicación

| Resultado | Filas |
|---|---|
| APLICADO en V52 | 555 de propuestas, más 6 por la norma de texto (`FRASE_FINAL`) |
| V53 (apoyo suspendido sobre un aparato) | 24 propuestas → 16 ejercicios marcados; 8 de suelo documentados |
| DOCUMENTADO (máquina asistida) | 8 |
| NO_APLICADO (V3 rechaza, coordinador veta, criterio V50, contrato vigente o decisión previa) | 97 |
| PROFESIONAL (bloqueo, ver `REVISION_PROFESIONAL.md`) | 281 |

## 5. Hallazgos que no formaban parte de ninguna propuesta

Los verificadores anotaron contradicciones que la R2 no había propuesto corregir. **No se aplicó ninguna por esta vía**, porque no tienen R2 ni V3. Siguen elegibles y son contradicciones materiales abiertas, salvo 1084, que ya resolvió V52 por otra propuesta, y 1303, que está contenido. Quedan para la siguiente ronda de revisión:

| Id | Hallazgo |
|---|---|
| 231 | El GIF es un press de banca cerrado, no un press desde pull-over; el patrón sería HORIZONTAL_PUSH |
| 620 | El GIF es una extensión de hombro con el brazo recto, no una elevación lateral |
| 1084 | El nombre decía «y carrera» y el GIF no muestra ninguna carrera. **Resuelto por V52**: la R2 propuso el nombre por su cuenta y V52 aplica «Lanzamiento de pecho con balón medicinal» |
| 1182 | El equipo «plyo box» probablemente sobra: los pies están en el suelo |
| 1303 | Las instrucciones dicen «silla romana» y el aparato es una silla de capitán |
| 1323 | Las instrucciones mencionan unos rodillos que no aparecen en el GIF |
| 367 | Se resalta el deltoides posterior; la zona y el patrón podrían ser otros |
| 1017 | Se resaltan también los gemelos; podrían añadirse como secundarios |

## 6. Inactivos (157)

Inventario completo en `cierre/inactivos/INVENTARIO_INACTIVOS.tsv`. La clase y el motivo se calculan desde la procedencia, sin depender de los comentarios.

| Clase | Ejercicios | Motivo |
|---|---|---|
| SEMILLA_V8 | 52 | V11 §3: semilla mínima retirada. Se desactivan y no se borran por la FK de los planes históricos |
| NUNCA_ACTIVADO_V11 | 99 | V11 §2: equipamiento fuera de la lista (weighted 36, other 21, assisted 15, sled machine 15, rope 10, olympic barbell 2) |
| DESACTIVADO_REVISION | 6 | V24: 1116, 1203, 1316 y 1319, implemento sin código. V36: 1267, Stalder press. V37: 776, elevator |

No se reactiva ninguno: reactivar exige revisarlo como a un activo. La nueva restricción de V51 obliga además a clasificarlo antes (impacto y estiramiento explícitos).
