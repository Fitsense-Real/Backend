# Validación multisemana (escenarios humanos)

**Fecha:** 30/10/2026. **Prueba principal:** `orchestation/ValidacionMultisemanaIT`.

**Cómo se ejecuta:**
- Servicios reales: generación, registro, adherencia, decisión, V8C, V1–V25, linaje V31–V33 y publicación.
- PostgreSQL temporal (Testcontainers).
- La IA está **simulada** y respeta los permisos de cada intervención:
  - SEGURIDAD_ANTE_DOLOR sustituye por un ejercicio elegible del mismo patrón y grupo;
  - en GAIN_MUSCLE no se reduce el volumen (V24).
- **Progresión apagada**, como en producción.
- **No es una validación con personas reales.**

**Comprobaciones en cada semana y en cada secuencia:**
- el plan se publica;
- el linaje no tiene violaciones;
- ninguna sesión no señalada cambia;
- el ejercicio que dolió no vuelve;
- ninguna sesión se recorta sin una causa que lo autorice.

## 1. Escenarios (§15 de la orden) y dónde se cubren

| # | Escenario | Prueba | Estado |
|---|---|---|---|
| 1 | Cumple todo | Multisemana «1 cumple todo» | VERIFICADO: NO_CHANGE ×3 |
| 2 | Falta una vez por un imprevisto | «2 imprevisto una vez» | VERIFICADO: CONSERVAR (SIN_EFECTO) y después NO_CHANGE |
| 3 | Falta recurrente de tiempo | «3 falta de tiempo recurrente con minutos» | VERIFICADO: ACORTAR con techo. APLICADA en LW y GM, SIN_EFECTO en IS, RESPALDO en GF (techo inviable) |
| 4 | Ningún minuto ese día | «4 sin ningún minuto ese día» | VERIFICADO: CONSERVAR |
| 5 | Cambia sus días disponibles | `PerfilCongeladoIT`; `cambiaSuPerfil` (opt-in) reproduce el caso si se cambia por SQL | **CORREGIDO Y PROBADO** (§4): el perfil no cambia durante el estudio |
| 6 | Fatiga | «6 fatiga» | VERIFICADO: REVISAR_CARGA, sin subir dosis |
| 7 | Dolor | «7 dolor» | VERIFICADO: SEGURIDAD_ANTE_DOLOR APLICADA; el ejercicio no vuelve |
| 8 | Rutina demasiado difícil | «8 demasiado difícil» | VERIFICADO: VARIANTE_MAS_SENCILLA |
| 9 | Desmotivado | «9 desmotivado» | VERIFICADO: INVESTIGAR_CAMBIO_MINIMO |
| 10 | Varios motivos | «10 varios motivos»; `AdaptacionPorCausasIT.dolorYFaltaDeTiempoEnLaMismaSemana` | **CORREGIDO Y PROBADO** (corrección 3) |
| 11 | Alterna semanas completas e incompletas | «11 alterna y 13 vuelve a omitir la adaptada» | VERIFICADO: s2 adapta, s3 NO_CHANGE, s4 vuelve a adaptar, sin recortes acumulados |
| 12 | Completa una sesión adaptada | `AdaptacionPorCausasIT.resultadoPorSesionAdaptada`; s3 de las secuencias 3, 7 y 10 | VERIFICADO: la consulta la clasifica COMPLETADA; en las secuencias, la semana siguiente es NO_CHANGE |
| 13 | Vuelve a omitir una sesión adaptada | `resultadoPorSesionAdaptada` (fatiga otra vez); «11 … 13» (s4) | VERIFICADO: la consulta da OMITIDA con `misma_causa`, y en la secuencia se adapta de nuevo sin recortes acumulados |
| 14 | Progresa varias semanas | `ProgresionIT`, `ProgresionCienPorCienIT` | VERIFICADO (progresión encendida solo en la prueba) |
| 15 | Llega al techo de progresión | `PendientesProgresionIT` | VERIFICADO: techo de 1,10 × base y paso del 5 % |
| 16 | Reincorpora un ejercicio | `CopiaSinIaIT.reincorporacionYMantenimiento` | VERIFICADO |
| 17 | Equipamiento limitado | Las 36 referencias de casa e `ImpactoRestriccionesIT` (192 escenarios) | VERIFICADO: 0 implementos ausentes. Ver C1 en `AUDITORIA_INTEGRAL_LOGICA.md` §2.2 |
| 18 | Adaptación no factible | `menosTiempoHabitualInviable` (RESPALDO); `CorreccionesReplicate*IT` C5 (NO_FACTIBLE trazable) | VERIFICADO |
| — | Sin registro | «sin registro» | VERIFICADO: INSUFFICIENT_DATA, sin adaptación |

## 2. Resultados por objetivo

12 secuencias × 4 objetivos = **48 de 48 pasan**. Decisión y estado V32 de la semana 2:

| Secuencia | LOSE_WEIGHT | GAIN_MUSCLE | INCREASE_STRENGTH | GENERAL_FITNESS |
|---|---|---|---|---|
| 1 cumple todo | NO_CHANGE | NO_CHANGE | NO_CHANGE | NO_CHANGE |
| 2 imprevisto | CONSERVAR / SIN_EFECTO | ídem | ídem | ídem |
| 3 tiempo con minutos | ACORTAR / APLICADA | ACORTAR / APLICADA | ACORTAR / SIN_EFECTO | ACORTAR / RESPALDO |
| 4 sin minutos | CONSERVAR / SIN_EFECTO | ídem | ídem | ídem |
| 6 fatiga | REVISAR / SIN_EFECTO | REVISAR / SIN_EFECTO | REVISAR / SIN_EFECTO | REVISAR / APLICADA |
| 7 dolor | SEGURIDAD / APLICADA | ídem | ídem | ídem |
| 8 difícil | VARIANTE / SIN_EFECTO | ídem | ídem | ídem |
| 9 desmotivado | INVESTIGAR / SIN_EFECTO | ídem | ídem | INVESTIGAR / APLICADA |
| 10 varios motivos | SEGURIDAD / APLICADA | ídem | ídem | ídem |
| 11 alterna (s2, s4) | REVISAR / SIN_EFECTO ×2 | ídem | ídem | REVISAR / APLICADA ×2 |
| horario | REPROGRAMAR / SIN_EFECTO | ídem | ídem | ídem |
| sin registro | INSUFFICIENT_DATA | ídem | ídem | ídem |

**Interpretación de los resultados:**
- Los SIN_EFECTO de VARIANTE, INVESTIGAR y REPROGRAMAR vienen de la IA simulada: no propone cambios.
- Los SIN_EFECTO de CONSERVAR son el resultado esperado (D2).
- **Ninguno es un fallo:** la semana se publica, conservada.
- La **adherencia** de cada semana la calcula el backend real; la prueba solo comprueba la decisión que resulta (NO_CHANGE, localizada o INSUFFICIENT_DATA). Los umbrales del 80 % y el 30 % no cambiaron.

Trazas completas: `evidencia/auditoria_integral/multisemana_trazas.txt`.

## 3. Fallos encontrados y correcciones

| Fallo | Estado |
|---|---|
| Un ejercicio dejado por dolor no se resolvía con adherencia buena, ni cuando la peor sesión era otra | **CORREGIDO Y PROBADO** (`a2271ab`) |
| La falta de tiempo siempre acortaba | **CORREGIDO Y PROBADO** (`a335e48`) |
| Artefactos de la IA simulada: quitar el ejercicio que dolió dejaba la sesión bajo el piso V20, y en GAIN_MUSCLE reducía el volumen (V24) | Era un defecto **del simulador**, no del backend: los validadores rechazaron correctamente. Se ajustó el simulador para que sustituya |

## 4. Cambio de perfil: decidido y aplicado

**Decisión del responsable (30/10/2026):** durante el estudio, la persona no puede modificar su perfil. Lo que declaró al registrarse se mantiene.

**Cómo se aplica en el backend:**
- Desde el primer plan, `PUT /api/v1/users/me/profile` responde **422** y no guarda nada.
- Antes del primer plan, el perfil se puede corregir.
- Lo prueba `PerfilCongeladoIT`.
- Así la semana copiada siempre cumple los días y el equipamiento del perfil. El caso de abajo ya no puede ocurrir por la API; `cambiaSuPerfil` lo reproduce solo cambiando la base directamente.

**Análisis previo** (se conserva como antecedente):

**Reproducción:** `-Dfitsense.pendientes=true` ejecuta `cambiaSuPerfil`. Evidencia en `evidencia/auditoria_integral/pendiente_cambio_de_perfil.txt`.

**Qué pasa:**
- La persona cambia sus días disponibles o su equipamiento a mitad del estudio.
- La semana siguiente se **copia** de la anterior (NO_CHANGE).
- Las fechas o los implementos copiados ya no cumplen el perfil: V3 o V1.
- La semana queda **NO_FACTIBLE, sin plan publicado**.

**Opciones** (requieren aprobación porque cambian el comportamiento publicado):
1. Regenerar la semana desde cero cuando el perfil cambie. Es lo más simple, pero rompe el linaje de la copia y la herencia de reducciones.
2. Remapear los días y sustituir solo lo que el equipamiento nuevo no permite, con una excepción explícita en V8C. Conserva el linaje, pero exige más código.
3. Mantener el comportamiento actual y bloquear el cambio de perfil durante el estudio desde el cliente.

**Recomendación técnica:** la opción 1, porque solo cambia la generación y no toca V8C. No está implementada.

## 5. Comparación con campañas previas y Replicate

- **Campaña real con Replicate** (`PRUEBAS_REPLICATE_REAL.md`): no se repitió, porque supondría llamadas de pago. Los cinco defectos que encontró (C1–C5) se reproducen con las respuestas guardadas en `CorreccionesReplicate{Apagado,Encendido}IT`, que siguen en la suite.
- **Diferencia esperada respecto a esa campaña:** los ciclos que acortaban una omisión `LACK_OF_TIME` sin subcausa ahora son CONSERVAR (ADAPT-2).
- **Usuarios 31, 32 y 33:** sus registros originales no están en el repositorio. **NO REPRODUCIBLE.** Se comprobaron las defensas documentadas, todas en pruebas de CI:
  - timeout y respuesta tardía → RESPALDO;
  - dosis no autorizadas → C1;
  - duración mínima → V20 exacto;
  - minutos frente a `min_session_seconds` → C4.

  Que la IA real respete el techo M y CONSERVAR **queda sin evaluar con la IA real**.

## 6. Cómo reproducirlo

```
mvn verify -Pintegration -Dit.test=ValidacionMultisemanaIT
mvn verify -Pintegration -Dit.test=ValidacionMultisemanaIT -Dfitsense.pendientes=true   # cambio de perfil
mvn verify -Pintegration -Dit.test=AdaptacionPorCausasIT
```
