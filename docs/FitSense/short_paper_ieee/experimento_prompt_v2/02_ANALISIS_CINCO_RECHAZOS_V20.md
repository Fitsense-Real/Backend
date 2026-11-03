# Análisis de los cinco rechazos por V20

**Qué se mide:**
- cuánto trabajo pretendía retirar la IA en cada sesión rechazada;
- cuánto podía retirar realmente sin bajar del mínimo.

**Método:**
- la duración de cada cambio se calcula con la réplica exacta de `SessionDurationEstimator` (ver `01`);
- se comprobó contra los 7 valores que dio el backend (coinciden todos).

**«Margen»:** duración actual − mínimo V20, es decir, lo máximo que se puede retirar.

## Tabla por sesión

| Propuesta | Sesión (intervención) | Margen | Retirado por la IA | Exceso | Desglose de lo que retira cada cambio | Mayor subconjunto de **sus propios cambios** que cabía |
|---|---|---:|---:|---:|---|---|
| U08 i1 | S45 (REVISAR_CARGA) | 36 | 372 | 336 | • 1106: 3×6 → 2×6 = 168<br>• 994: 3×6 → 2×6 = 168<br>• 323: 3×10 → 3×8 = 18<br>• 487: 3×10 → 3×8 = 18 | 323 y 487 de 3×10 a 3×8 = **36 s → 2520 s, igual al mínimo, válido** |
| U09 i1 | S50 (VARIANTE) | 36 | 960 | 924 | • 775 y 1315: series −1 = 168 + 168<br>• 380 y 1039: 3×10 → 2×6 = 144 + 144<br>• sustituir 540 y 780 por 72 y 300 = 336 | Ninguno (cada cambio pasa de 36) |
| U09 i1 | S51 (INVESTIGAR) | 36 | 912 | 876 | • 73, 1228, 1196 y 705: series −1 = 168 cada uno<br>• 1284 y 117: series −1 = 120 cada uno | Ninguno |
| U09 i2 | S50 (VARIANTE) | 36 | 432 | 396 | 6 sustituciones con la dosis de catálogo de cada sustituto = 432 | Ninguno |
| U09 i2 | S51 (INVESTIGAR) | 36 | 336 | 300 | 73 y 1196: series −1 = 168 + 168 | Ninguno |
| U18 i1 | S105 (REVISAR_CARGA) | 150 | 320 | 170 | • 6 ejercicios con 2 o 3 repeticiones menos = 12 × 3 + 18 × 3<br>• 1054: 3×90 → 2×60 s = 210<br>• 1188: 2×30 → 2×20 s = 20 | **Todo menos 1054 = 110 s → 1930 s, válido** |
| U19 i1 | S110 (VARIANTE) | 150 | 200 | 50 | • 4 reducciones de repeticiones = 12 + 18 + 18 + 18<br>• 853: 3×90 → 3×60 s = 90<br>• 1260: −10 s = 20<br>• 2 sustituciones = 24 | **Todo menos 853 = 110 s, válido** |
| U19 i1 | S111 (INVESTIGAR) | 150 | 200 | 50 | • 6 reducciones de repeticiones = 36 + 54<br>• 1054: 3×90 → 3×60 s = 90<br>• 1322: −10 s = 20 | **Todo menos 1054 = 110 s, válido** |

**Cuentas:**
- 5 propuestas rechazadas: U08 i1, U09 i1, U09 i2, U18 i1 y U19 i1;
- 8 sesiones-propuesta rechazadas por duración, de las que salen los 7 mensajes V20 del registro;
- U09 i1 y U09 i2 traen 2 cada una, y U19 i1 también.

**Nota:** en el registro, U09 y U19 tienen dos mensajes por intento, uno por fecha (en U09: 11/11 = S50 y 13/11 = S51). Aquí se desglosa por sesión.

## U09: comprobación pedida

| Dato | Valor | Fuente |
|---|---|---|
| Duración de S50 y S51 | **2556 s** (43 min) | Prompt («ahora dura unos 2556 s») y la réplica del estimador |
| Mínimo | **2520 s** (70 % de 60 min) | Prompt y mensaje V20 |
| Margen | **36 s** | 2556 − 2520 |
| Menor cambio de dosis posible en S51 | 9 s (una repetición menos en las 3 series de 1284 o de 117, que tienen `min_reps` 6) | Catálogo del prompt |
| Cambios de dosis que caben en S51 | Hasta 4 repeticiones menos en total entre 1284 y 117 (4 × 9 = 36 s). Los 3×6 no pueden bajar de repeticiones (`min_reps` 6) y una serie menos retira 168 s | Ídem |

## Lo que dicen los datos

1. **La IA quiere retirar entre 1,3 y 27 veces lo que cabe.** El exceso no es marginal: en FU, el cambio que más usó (una serie menos) cuesta por sí solo entre 3 y 5 veces el margen (120–168 s frente a 36 s).
2. **En 3 de las 5 propuestas (U08 i1, U18 i1 y U19 i1), deshacer uno o dos de sus propios cambios habría dado una propuesta válida y efectiva.**
   - El reintento no lo consiguió, porque el mensaje decía «agrega un ejercicio o una serie» y la pista decía «OMÍTELA».
   - El modelo interpretó «volver hacia la dosis prescrita» como «subir», que tiene prohibido (`rationale` de U08 i2 y U19 i2).
3. **En U09 (márgenes de 36 s) solo cabían reducciones de 9–36 s**, y la IA nunca propuso cambios de ese tamaño. El prompt no le dice cuánto retira una repetición ni una serie.
4. **El validador funcionó como debía:** las 8 sesiones-propuesta habrían dejado la sesión por debajo del 70 %. **No hay ningún rechazo injustificado.**

## Respuesta a P1 (para estos casos)

**¿La IA propone cambios inválidos porque no entiende las restricciones?**

**En parte sí.**
- Entiende los **permisos**: 0 rechazos por V8C, V24 o V6.
- Pero no tiene cómo **cuantificar** la restricción de duración: el prompt solo trae el total y el mínimo.
- Y el texto del reintento le da una instrucción que, en su intervención, **no puede cumplir literalmente**.

Es un problema de información y de redacción del contexto, no de las reglas. **Las reglas no se cambian:** V20 rechazó correctamente.
