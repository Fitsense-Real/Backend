# Protocolo de verificación independiente de propuestas (tercera mirada)

Eres un verificador **independiente**. Otro revisor ha propuesto cambios en ejercicios del catálogo después de mirar su GIF. Tu trabajo es decidir, mirando tú mismo la animación completa, si cada propuesta es **correcta** según la evidencia visual. No conoces al revisor ni sus otras decisiones. No te fíes de su motivo: compruébalo.

## Material

| Material | Contenido |
|---|---|
| `propuestas.tsv` (en tu carpeta) | Una propuesta por fila: id, tipo, valor actual, valor propuesto, motivo del revisor, nombre e instrucciones actuales. |
| `../../hojas/ej_NNNN.png` | Todos los fotogramas del GIF original y la imagen fija. |
| `../../PROTOCOLO_REVISION_2.md` | Definiciones oficiales: banderas, apoyo suspendido, impacto, patrones, dificultad, equipo y reglas de redacción. Léelo primero. |

Si necesitas ampliar, usa Python y PIL sobre el GIF original, que está en el dataset bajo `/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/cati/ds/`, con la ruta que figura en la cabecera de la hoja. Ejecuta `python3 -I -B`.

## Qué decides por propuesta

| Decisión | Cuándo |
|---|---|
| **ACEPTAR** | El GIF demuestra que el valor propuesto es correcto y el actual no. |
| **RECHAZAR** | El valor actual es correcto, o el propuesto no está demostrado por el GIF. |
| **MEJORAR** | Solo para NOMBRE e INSTRUCCIONES: la corrección es necesaria pero el texto propuesto tiene errores, omite algo relevante o afirma algo que no se ve. Escribe tu versión, con las mismas reglas de redacción. |
| **NO_DECIDIBLE** | El GIF no permite decidir: fase no visible, agarre no visible, fundido entre poses… |

### Criterios por tipo

| Tipo | Criterio |
|---|---|
| BANDERA_ANADIR / BANDERA_QUITAR | Aplica la definición oficial literal. Quitar una bandera solo si el GIF demuestra que **nunca** se da la condición en ningún fotograma. |
| APOYO_SUSPENDIDO | ¿Hay algún momento sin pies en el suelo con el cuerpo sostenido **sobre** brazos, antebrazos o con los pies en correas? Di la clase. |
| INSTRUCCIONES | El texto actual ¿induce a otro movimiento o contradice el GIF en algo relevante (posición, trayectoria, apoyo, implemento, alternancia, seguridad)? Una diferencia menor que no cambia la ejecución (por ejemplo «palmas hacia dentro» frente a «hacia abajo» cuando no se ve) no justifica cambiar el texto: RECHAZAR. Si hay que cambiarlo, el texto propuesto ¿describe exactamente el GIF, conserva las advertencias de seguridad vigentes y termina con la frase reglamentaria? |
| NOMBRE | ¿El nombre actual describe otro movimiento o induce a error? ¿El propuesto describe el GIF en español natural? |
| EQUIPO | ¿Es imprescindible lo que se añade? ¿Sobra lo que se quita? |
| PATRON, ZONA_OBJETIVO, PRESCRIPCION | Movimiento principal según las definiciones. |
| DIFICULTAD_SUBIR | ¿El ejercicio exige claramente fuerza relativa alta, técnica compleja, suspensión o equilibrio exigente? Las bajadas no se verifican: no se aplicarán sin aprobación humana. |
| ESTIRAMIENTO | ¿Es un estiramiento (mantener una elongación)? |
| IDENTIDAD | Describe qué se ve y si el ejercicio puede identificarse con seguridad. |

## Salida

`verificacion3.tsv` en tu carpeta. UTF-8, separado por TAB, sin tabuladores ni saltos de línea internos, una fila por propuesta y en el mismo orden. Cabecera exacta:

```
id	tipo	decision	valor_final	evidencia	confianza
```

| Campo | Contenido |
|---|---|
| `valor_final` | El valor que debe quedar. Si aceptas, el propuesto; si rechazas, el actual; si mejoras, tu texto, con pasos separados por ` \|\| `. |
| `evidencia` | Fotogramas y qué se ve en ellos, por ejemplo `f3-f5 brazos verticales sobre la cabeza`. |
| `confianza` | ALTA, MEDIA o BAJA. |

## Prohibiciones

- No inventes evidencia: ante la duda, NO_DECIDIBLE.
- No leas `revision2.tsv`, otros lotes ni revisiones anteriores.
- No modifiques nada fuera de tu carpeta.

Al terminar, valida con un script: mismo número de filas que `propuestas.tsv`, valores de `decision` dentro de dominio y ningún campo vacío.
