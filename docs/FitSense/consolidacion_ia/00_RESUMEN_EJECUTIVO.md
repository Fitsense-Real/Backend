# 00 · Resumen ejecutivo

**Alcance:** documentación técnica interna de los experimentos de generación inicial y adaptación de planes con IA en FitSense, entre el 06/11/2026 y el 10/11/2026.
- Se basa solo en la evidencia original.
- No incluye ejecuciones nuevas, llamadas a Replicate ni cambios de código.
- Estado del código de referencia: `generation` `543932a`.

## 1. Qué se probó

| Campaña | Pregunta | Predicciones | USD |
|---|---|---:|---:|
| GRA-R1/R2 | ¿Funciona el flujo completo con IA real (generación + adaptación)? | 76 | 1,5368 |
| BASE-R1/R2 | ¿Qué propone el mismo modelo **sin** las reglas de FitSense? | 60 | 0,9594 |
| MEJ | ¿Mejoran PR-1.8 y V2.2 cambiando solo el prompt? | 102 | 2,00 |
| MEJ ronda 2 | ¿Mejoran pr-1.8a/b/c, v2.2a y v2.4? Auditoría de reglas | 362 | 7,9051 |
| CONF-IA | Confirmar pr-1.8c, pr-1.8a y v2.2a, con V8C corregido | 336 | 7,0950 |
| pr-1.8a final | Confirmar pr-1.8a con la versión actual del modelo | 99 | 2,6115 |
| **Total** | | **1035** | **22,11** |

Las seis campañas tienen evidencia suficiente para sus conclusiones, con dos matices:
- **pr-1.8a:** la ronda A3 quedó incompleta y no entra en la decisión;
- **CONF-IA:** pr-1.8a tuvo una sola ronda (A1).

Detalle en `01_INVENTARIO_EVIDENCIAS.md`.

## 2. Conclusiones principales

1. **Las reglas de FitSense son las que hacen seguro el uso de la IA.**
   - Sin reglas (BASE), el mismo modelo propone **19/19** planes con infracciones (637 y 659 infracciones normalizadas) y cambia lo no autorizado en **10/10** ciclos de adaptación.
   - Con reglas (GRA), **12/19 y 13/19** planes son válidos a la primera, el resto se corrige en el reintento, y **ningún plan ni adaptación publicados tiene infracciones**.
2. **La IA propone y el backend decide.**
   - En GRA se rechazaron 10 y 6 propuestas de generación (sobre todo V15 y V26), todas corregidas en el reintento.
   - 20/20 planes se publicaron desde la IA, con 0 respaldos.
   - Las adaptaciones publicadas (10/10 y 9/10) no tuvieron ningún cambio no autorizado.
3. **Ninguna variante de prompt demostró una mejora que justifique cambiar los vigentes.**
   - pr-1.8c no cumplió los criterios: V24 en ganancia muscular y 14 rechazos V4 en pérdida de peso.
   - pr-1.8a, favorable con una versión del modelo (16/19 frente a 12/19, p = 0,28), quedó en **13/18 frente a 13/18** con la versión actual. Además trajo regresiones (V4 en pérdida de peso, V24, V23) y más timeouts.
   - v2.2a solo resolvió un caso (O18), con p = 0,50.
   - **Se mantienen PR-1.7 y V2.1.**
4. **Se corrigieron dos huecos del validador de adaptación (V8C)**, detectados por auditoría y sin incidentes reales: H-A1, retiradas no autorizadas, y H-A2, cambios solo del descanso.
   - PR #44, fusionada.
   - Regresión: 1331/0/1 unitarias y 593/0/44 integración; CI verde.
5. **Progresión y reincorporación siguen sin cambios.**
   - Funcionaron dentro de sus límites: techo 1,10×, un paso por ciclo y dos semanas al 100 %.
   - La reincorporación solo ocurre si antes hubo una retirada y al menos una progresión.

## 3. Decisiones registradas

| Componente | Decisión |
|---|---|
| Generación inicial | Mantener **PR-1.7** |
| Adaptación | Mantener **V2.1** |
| PR-1.8 y variantes | No adoptadas |
| v2.2a y v2.4 | No adoptadas |
| V8C H-A1/H-A2 | Corregidos y fusionados (PR #44) |
| V24 | Sin cambios |
| Progresión y reincorporación | Políticas existentes |

## 4. Lo que estos resultados **no** dicen

- **No demuestran calidad deportiva profesional.** Los validadores verifican las políticas de FitSense, y la pertinencia la juzga un oráculo del laboratorio. Hace falta la revisión de un profesional.
- **No demuestran que FitSense mejore la adherencia de usuarios reales.** Los 20 perfiles y su adherencia son sintéticos.
- **No son comparables entre versiones del modelo.** El proveedor cambió de versión dos veces (`9f4cd9ec…`, `045f8102…`, `81cb6179…`), así que cada comparación se hizo dentro de una misma versión.

## 5. Aviso de configuración

Los valores por defecto de `generation` **no** son la configuración evaluada:

| | Por defecto | Evaluado |
|---|---|---|
| IA | Apagada | Encendida |
| Política | PLAN-HIST-1 | PLAN-REV-2.0 |
| Prompt de adaptación | v1 | V2.1 |
| Progresión | Apagada | Encendida en el bloque P |

Esta documentación no cambia nada; ver `08_DECISIONES_Y_LIMITACIONES.md` §3.
