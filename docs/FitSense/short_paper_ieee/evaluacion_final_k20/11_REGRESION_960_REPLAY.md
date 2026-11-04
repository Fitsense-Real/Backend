# 11 · Regresión, 960 perfiles y REPLAY

**Código:** `7ee69c2` (`src/main` idéntico al SHA congelado `a31c36b`, que solo añade prueba y documentación).

| Bloque | Resultado |
|---|---|
| Unitarias | **1.185 aprobadas, 0 fallidas, 0 omitidas** |
| Integración (`verify -Pintegration`, configuración por defecto) | **521 ejecutadas: 480 aprobadas, 0 fallidas, 41 omitidas** |
| Laboratorios omitidos | Los 41 son opt-in y **no cuentan como superados**: 960, REPLAY, factibilidad, barridos, R0548, K20… Los relevantes se ejecutaron aparte (abajo) |
| Cambio frente a #39 | `TextoRespaldoSustitucionIT` (P3) pasa a ser **permanente y aprobada**: de 42 omitidas se pasa a 41 |
| 960 perfiles (por defecto y candidata) | `planes.csv` `d109a3da…` y `colocaciones.csv` `d227848b…`: **idénticos a la referencia** |
| REPLAY 100 semanas | Por defecto: solo marcas de tiempo. Candidata: solo N08, idéntica a la del PR #39 (`06`) |
| H1, V8C, V20, V24, 4D, adherencia, decisiones, dolor, respaldo, progresión, reincorporación, linaje, concurrencia, idempotencia | Cubiertos por las pruebas de integración del primer bloque, todas aprobadas. Detalle por clase en `evidencia/regresion/regresion_7ee69c2_por_clase.csv` |

**Identidad no es validez.** Los hashes de los 960 perfiles prueban que la generación inicial **no cambió**, no que todos los perfiles sean válidos.

**Perfil sin plan válido conocido: R0548**, registrado aparte. GAIN_MUSCLE intermedio, casa, 7 días, 75 min: la generación inicial no pasa V20 (2.550 s frente a 3.150 s; faltan 600 s). `GeneracionR0548V20IT` (opt-in) se ejecutó sobre la candidata y **reproduce** el fallo. No afecta a U01–U20. No se cambiaron ni V20 ni el catálogo.

**Cambios funcionales esperados frente a `generation`:**

| Cambio | Efecto |
|---|---|
| Límite de dificultad | N08 en REPLAY: sustituto de dificultad 1 en lugar del de dificultad 2 de la IA |
| Retirada localizada | Sin efecto en U01–U20 (no hay dolor) |
| P3 | Texto del respaldo con sustitución por dolor; no aparece en U01–U20 |

En la configuración por defecto del backend no cambia nada salvo P3.
