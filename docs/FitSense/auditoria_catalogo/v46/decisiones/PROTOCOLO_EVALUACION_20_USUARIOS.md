# Protocolo de evaluación integral: 20 usuarios sintéticos (preparado; **NO ejecutado**)

**Estado:** propuesta para aprobar.
- No se ha ejecutado nada de este protocolo.
- No hay llamadas a Replicate ni consumo de presupuesto.
- Los modos previstos son **DETERMINISTA** y **SIMULADA** (IA escrita por el laboratorio, sin red).
- Una fase con IA real necesitaría una autorización nueva, con su propio tope (ver `../F_PROPUESTA_EVALUACION.md`).

## 0. Principios (fijados antes de ejecutar)

1. **La versión se congela antes de generar la cohorte:** commit, `flyway_version`, checksum de migraciones, interruptores (`fitsense.experimental.*`), política y versión del prompt.
   - Cualquier cambio posterior invalida la corrida.
   - Las corridas no se repiten de forma selectiva.
2. **Cohorte nueva O01–O20.** Se genera y congela antes de ejecutar: CSV, manifiesto y SHA-256.
   - U y N **solo como regresión**: comparación de huellas con V46. No cuentan como evidencia independiente.
3. **Resultado por caso y aspecto.** Uno de:
   - **APROBADO**;
   - **FALLO**;
   - **NO APLICA**: el aspecto no se ejercita en ese caso; por ejemplo, la reincorporación sin una retirada previa;
   - **REQUIERE REVISIÓN**: técnicamente válido, pero depende de una decisión deportiva pendiente (E1–E7).
   - Los NO APLICA y los REQUIERE REVISIÓN se informan **por separado** y **no** cuentan como aprobados.
4. **Objetivo:** 100 % APROBADO en los casos aplicables.
   - Un FALLO no se corrige durante la corrida.
   - Se registra, se diagnostica después y solo una corrida nueva y completa puede cerrarlo.
5. **Prohibido para «hacer pasar» un caso:**
   - relajar V20, V24, V25, V8C o los controles de seguridad;
   - cambiar los oráculos tras ver resultados;
   - introducir restauración automática;
   - tocar los planes publicados.
6. **Doble verificación.** El arnés (validadores del backend) y el **oráculo independiente en Python**, que lee las tablas exportadas sin usar funciones del backend.
   - Un caso solo es APROBADO si ambos coinciden.
   - Una discrepancia se trata como FALLO hasta explicarla.

## 1. Cohorte O01–O20 (diseño)

| Eje | Cobertura mínima |
|---|---|
| Objetivo | 5 por objetivo (GAIN_MUSCLE, LOSE_WEIGHT, INCREASE_STRENGTH, GENERAL_FITNESS) |
| Nivel | ≥ 6 principiantes, ≥ 6 intermedios, ≥ 6 avanzados |
| Equipamiento | ≥ 6 casa sin material, ≥ 5 casa con mancuernas o bandas, ≥ 6 gimnasio; ≥ 2 con anclaje o silla declarados si E5/E6 se adopta |
| Frecuencia y duración | De 1 a 7 días; 30 a 75 minutos, con extremos (1 día × 30 min; 7 días × 75 min); ≥ 2 que el tope del nivel recorte (S-MAX) |
| Restricciones | ≥ 4 con restricción declarada (rodilla, hombro, impacto) y ≥ 2 con edad que active filtros |
| Dirigidos a problemas del catálogo | ≥ 3 principiantes cuyo plan V46 tenía curl nórdico; ≥ 3 casa sin material con hombro solo de 973; ≥ 2 LOSE_WEIGHT de 1–2 días (rodilla solo con aislados en V46) |

## 2. Guion por usuario

Tres semanas como mínimo, más una cuarta si hay reincorporación:
- **S1:** generación y ejecución con adherencia de guion;
- **S2:** señal de adaptación, decisión, adaptación, validación y publicación;
- **S3:** progresión o mantenimiento;
- **S4:** reincorporación condicional.

**Adherencia** asignada antes de ejecutar:
- 8 completos;
- 5 parciales (60–80 %);
- 3 con sesiones saltadas por falta de tiempo;
- 2 con dolor localizado;
- 2 con desmotivación o «demasiado difícil».

## 3. Los 10 aspectos y sus criterios (APROBADO si se cumplen todos)

| # | Aspecto | Criterio técnico | Herramienta | NO APLICA cuando |
|---|---|---|---|---|
| 1 | Generación | Plan VALIDO o NO_FACTIBLE explicado. V20, V24 y V25 cumplidos. Sesiones = min(días, tope del nivel). Duración estimada ≤ minutos del perfil. Dosis en la traza (REV2). Ningún ejercicio con dificultad > tope del nivel | Arnés K20 (generación) + `oraculo_k20_politica.py` | — |
| 2 | Personalización y restricciones | Solo ejercicios elegibles por equipamiento (V23, incluidos los requisitos E5/E6 si se adoptan), restricciones y edad. Sin alto impacto si está restringido. `/plan/limitations` coherente: S_MAX, DOSIS_LIMITADA, **DOSIS_NO_DEMOSTRADA (E2-a)**, slots vacíos | Arnés + oráculo + llamada a `/plan/limitations` | — |
| 3 | Ejecución de sesiones | Registro de series y repeticiones, estados de sesión y adherencia calculada según el contrato (completar las sesiones programadas = 100 %) | Arnés (ejecución) + oráculo de adherencia | — |
| 4 | Adherencia | Métrica semanal igual en el backend y en el oráculo. La falta de tiempo y el dolor quedan registrados con su motivo | Oráculo | Ninguna sesión registrada (no debe ocurrir) |
| 5 | Adaptación con IA | Solo sesiones señaladas. Nunca sube la dosis. Sustitutos del mismo patrón y con dificultad ≤. V8C cumplido. Si V8C rechaza la propuesta, se reintenta y luego se recurre al respaldo | Modo SIMULADA (rechazo forzado en el intento 1) + oráculo | Usuario sin señal de adaptación |
| 6 | Mantenimiento cuando no procede adaptar | Sin cambios en el plan; motivo trazado; ninguna publicación espuria | Arnés + oráculo | Hubo adaptación |
| 7 | Progresión gradual | Solo tras el criterio (100 % durante dos semanas, por sesión). Incrementos ≤ los de la política. Nunca después de dolor | IT de progresión REV2 + arnés S3 | Sin el criterio cumplido |
| 8 | Reincorporación condicional | Solo si desaparece la causa de la retirada. Linaje correcto. **Sin restauración automática** | IT de reincorporación + arnés S4 | Sin retirada previa |
| 9 | Continuidad multisemana | La semana siguiente se genera a partir de la publicada. Sin huecos ni duplicados de versión. Linaje íntegro | Arnés (ciclos) + oráculo de linaje | — |
| 10 | Seguridad e integridad | Ningún plan publicado modificado a posteriori. Ningún ejercicio prohibido por la restricción. Sin regresiones en HIST. Evidencia con SHA-256 y salida en una carpeta nueva | Oráculo + `SHA256SUMS` + comparación HIST | — |

**REQUIERE REVISIÓN**, de forma automática, en cualquier caso cuyo plan o cuya adaptación contenga:
- un curl nórdico (E3) mientras su dificultad no esté aprobada;
- 775, 973, 1261, 1308, 1259 o 1146 como única fuente de dosis de un grupo (E2);
- 853, 580, 1093 o 1320 con nombre o instrucciones sin corregir (E4/E7; solo afecta a lo que se muestra);
- 1205 o 1076 como cobertura de rodilla si P15-C está activo y su dificultad no está validada.

## 4. Corridas previstas (sin coste)

| Corrida | Modo | Política | Cohorte | Uso |
|---|---|---|---|---|
| D_O / D_O′ | DETERMINISTA, dos veces | Versión recomendada | O01–O20 | Evaluación y reproducibilidad (huellas idénticas) |
| S_O | SIMULADA, prompt V2.1 | Versión recomendada | O01–O20 | Camino de la IA, rechazo y respaldo |
| D_O_HIST | DETERMINISTA | PLAN-HIST-1 | O01–O20 | Conservación de HIST |
| REG_U, REG_N | DETERMINISTA y SIMULADA | V46 y versión recomendada | U, N | **Solo regresión**: diferencias explicadas, una por una |
| T443 | DETERMINISTA | Catálogo original (`flyway.target=44.3`) | O01–O20 | Línea base del catálogo sin corregir |

## 5. Informe de resultados (plantilla)

**Tabla 20 × 10** con APROBADO, FALLO, NO APLICA o REQUIERE REVISIÓN, más los totales:
- % APROBADO sobre los aplicables;
- número de NO APLICA;
- número de REQUIERE REVISIÓN, con el motivo E-x.

**Cada FALLO** con:
- su evidencia (archivo y línea);
- su causa;
- si se reproduce en V46.

**Se adjuntan** las diferencias U/N frente a V46 y el resultado del oráculo independiente.

## 6. Requisitos previos a la ejecución (no cumplidos todavía)

- [ ] Decisión sobre la versión técnica (ver `00_INFORME_DECISIONES.md` §6).
- [ ] Aprobación de este protocolo y de la cohorte O antes de generarla.
- [ ] Extender el arnés para la semana S4 (reincorporación) y el aspecto 2 (`/plan/limitations`).
  - Hoy esas partes las cubren IT separadas.
  - El cambio sería solo de pruebas.
