# 02 · V8C antes y después de la candidata H1

## 1. La regla

| | Antes (`generation` y PR #28–#30) | Candidata (`candidate/h1-multiconjunto`) |
|---|---|---|
| Valor por defecto de `fitsense.adaptation.v8c.pattern-rule` | no existía (siempre por conjunto) | **`multiconjunto`** |
| Sustitución en VARIANTE (TOO_DIFFICULT) | Cada añadido tiene un patrón **que está entre** los retirados | Además: por patrón, **añadidos ≤ retirados de ese patrón** (cada añadido reemplaza a un retirado **distinto** del mismo patrón) |
| Dolor (`SEGURIDAD_ANTE_DOLOR`) | Rama propia: otro patrón permitido; no añadir más de lo retirado; el volumen no sube | **Igual** (la regla nueva no se evalúa en esa rama) |
| Retirar sin añadir (VARIANTE, ACORTAR) | Permitido donde ya lo estaba | **Igual** |
| Cambios de dosis sin sustitución | Juzgados por permisos, V20 y V24 | **Igual** |
| Sesiones no señaladas | No pueden cambiar | **Igual** |
| Generación inicial | V8C no interviene | **Igual** |
| Constructor sin argumentos (pruebas) | CONJUNTO | **MULTICONJUNTO** |
| Volver a la regla anterior | — | `FITSENSE_V8C_PATTERN_RULE=conjunto`. Cada contexto lo registra: `V8C: regla de patron HISTORICA activa: CONJUNTO (compatibilidad o reversion de H1).` |

**Archivos de `src/main` que cambian frente a la rama de #31:**
- `ConservationValidator.java`: valor por defecto, constructor y aviso de la regla histórica;
- `application.properties`: valor por defecto y comentario.

El algoritmo multiconjunto es el aprobado (PR #31) y **no se ha modificado**.

## 2. El mismo caso con las dos reglas (prueba de integración permanente, en CI)

`ConservacionPatronH1IT` (regla por defecto) y `ConservacionPatronH1ConjuntoIT` (`conjunto` explícito):
- flujo real de adaptación, PostgreSQL efímero y validadores reales;
- **solo la IA simulada**.

**El caso:** un GAIN_MUSCLE intermedio de gimnasio omite una sesión por TOO_DIFFICULT. La sesión tiene dos estructurales A y B de patrones distintos y la misma dosis 4D, y el patrón de B también aparece en otra sesión de la semana, como el tirón vertical de U04.

| Propuesta de la «IA» | `multiconjunto` (por defecto) | `conjunto` (histórica) |
|---|---|---|
| **A → a1 y B → a2**, ambos del patrón de A: la sesión pierde B | **Rechazada** en los 3 intentos con el mensaje de H1; la semana se **conserva** (CONSERVED, copia exacta) | **Publicada** (AI): la distribución de patrones cambia |
| **A → a1 y B → b1**, cada uno de su patrón | **Publicada** (AI); distribución conservada; misma dosis; no señaladas intactas | **Publicada** |
| Regla vigente en el contexto | MULTICONJUNTO | CONJUNTO |

En la ejecución local, el rechazo fue exactamente: «V8C: la sesion 1 anade 2 ejercicio(s) de patron HIP_EXTENSION [121, 122] y solo retira 1 de ese patron [309]…».
- El participante es generado, así que los ids pueden variar.
- La prueba busca la sesión adecuada y comprueba el resultado, no los ids.

### 2.1 Lo que se aprendió al escribir la prueba

En los dos primeros intentos de la prueba, la propuesta incorrecta la rechazaban **otras** reglas incluso con `conjunto`:
- **V23 (cobertura 4C semanal):** el patrón perdido era el único de su clase en la semana. La cobertura ya lo protegía.
- **V15 (reparto por grupo):** al poner dos ejercicios de la misma zona, un grupo superaba la mitad de la sesión.

**H1 cubre justo el caso que esas reglas no ven:** el patrón perdido sigue presente en otra sesión y el reparto por grupo se mantiene, como en U04. La prueba fija ese caso explícitamente, para que no dependa de que V23 o V15 lo atrapen por casualidad.

## 3. Impacto (ya medido en `validacion_h1_h12`, sin cambios)

| Medida | Resultado |
|---|---|
| Sustituciones históricas reales que siguen siendo válidas | **123 / 123** (GM 56, LW 32, FU 26, AG 9) |
| Sesiones publicadas cuya validación cambia | 0 reales; 1 de laboratorio (U04 #12b) |
| Cambio de comportamiento declarado | «Añadir 2 de un patrón retirando 1» fuera del dolor ya no pasa (0 casos reales) |

## 4. Pruebas que documentan el comportamiento histórico

**Se conservan y se ejecutan con `conjunto` explícito:**
- `V8CDiagnosticoSustitucionesTest` (11 pruebas): ahora fija `ReglaPatron.CONJUNTO`. Sus pruebas 2 y 3 siguen mostrando el hueco anterior;
- `ConservacionPatronH1ConjuntoIT` (3 pruebas, en CI);
- `V8CConservacionPatronH1Test` (32): cada caso con las dos reglas;
- `H1ImpactoHistoricoTest` (*opt-in*): las dos reglas.

**Ninguna cobertura se ha eliminado.**
