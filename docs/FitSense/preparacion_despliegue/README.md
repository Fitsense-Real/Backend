# Preparación segura del despliegue del backend

**Estado:**
- Solo diagnóstico y propuestas. **Nada se ha desplegado** ni se ha modificado en Railway ni en Neon.
- No se han ejecutado migraciones remotas. **USD 0** en Replicate.
- Base: `generation` `3374213f3ab67271dcba383000c707657c556403`, con las PR #47 y #48 fusionadas.

| Archivo | Contenido |
|---|---|
| `01_RIESGO_NEON_Y_PROPUESTA.md` | URL de Neon fijada en `application.properties` con Flyway activo: diagnóstico y propuesta (no implementada) para fallar de forma controlada sin base explícita |
| `02_VARIABLES_ENTORNO_PRUEBAS.md` | Variables necesarias para un entorno de pruebas en Railway, sin valores secretos |
| `03_REPLICATE_FALLOS_Y_COSTE.md` | Comportamiento ante fallos de Replicate, hallazgos R-1 a R-5 y recomendación de límite de gasto y alertas |

## 1. Verificación final de `generation` (11/11/2026)

**Fusiones:**

| PR | Merge |
|---|---|
| #47: corrección O12 | `5cbea9eb72728bb912ff3c13caa38de2cc200e27` |
| #48: auditoría | `3374213f3ab67271dcba383000c707657c556403` |

**Pruebas y CI:**

| Comprobación | Resultado |
|---|---|
| Regresión local completa (`mvn verify -Pintegration`, sin token) | **1345/0/1 unitarias; 603/0/44 integración**. Coincide con la referencia (1338 + 7 de `SubidaPorSustitucionTest`; 603) |
| CI de `generation` sobre `3374213` (run 233) | **Verde** |
| CI del merge de la #47 (run 229) | Cancelado por `cancel-in-progress` al llegar el push de la #48. No es un fallo |

**Configuración efectiva**, comprobada con `ConfiguracionEvaluadaIT` (contexto real de Spring) y `ConfiguracionEvaluadaTest` (valores por defecto e interruptores):
- IA activa;
- PR-1.7;
- FIX-PR-2.1-EXP;
- PLAN-REV-2.0;
- progresión activa;
- límite de dificultad `preferencia`;
- retirada localizada `on`;
- V8C `multiconjunto`.

**V24 en generación inicial, con los cuatro objetivos:**
- `ProgresionV24V8CTest` («la generación, intacta»);
- las pruebas de coherencia de V23/V24/V25 con el generador;
- `FactibilidadPorObjetivoIT`;
- la semana COMPLETA en los cuatro objetivos;
- la validación multisemana (57 escenarios).

**O06, O12 y O20** con IA simulada y la progresión encendida: **0 llamadas, 0 incidencias**. Estados de adaptación y pasos de progresión idénticos a los de la auditoría.
- O20: 6 pasos de progresión; el excluido por dolor no vuelve.
- O12: SIN_EFECTO con los 8 ejercicios dolorosos excluidos y sustituidos; 2 pasos de progresión sin reaparición.
- O06: APLICADA; el excluido no vuelve.
- **El oráculo v1.4 marca:**
  - A10 en O06 y O12: artefacto de protocolo (bloque G ejecutado con la progresión encendida);
  - A7 en O06: progresión sin candidatos sin registrar (P-SC);
  - A6 en O12: el 336, excluido por dolor, también se sustituye en la sesión no señalada, porque P-02 lo excluye de toda la semana. El oráculo no modela esa propagación; la conducta es la segura.

## 2. Lo que falta para desplegar con seguridad

| # | Pendiente | Tipo | Responsable |
|---|---|---|---|
| 1 | Confirmar en el panel de Railway qué servicios existen, qué rama vigilan y si el despliegue automático está activo | Operativo | Responsable del proyecto |
| 2 | Decidir y aplicar la corrección de la URL por defecto (`01` §2, opción A). Antes, añadir `SPRING_DATASOURCE_URL` y `SPRING_DATASOURCE_USERNAME` a los servicios existentes | Código + operativo | Orden posterior |
| 3 | Base de datos de pruebas separada (otra base o rama de Neon), con sus credenciales solo en Railway | Operativo | Responsable |
| 4 | `JWT_SECRET` propio del entorno | Operativo | Responsable |
| 5 | Token de Replicate exclusivo, con límite de gasto y alertas (`03` §4) | Operativo | Responsable |
| 6 | Revisar el primer arranque: host de Flyway, versión 46 y política PLAN-REV-2.0 en el primer plan | Verificación | Tras desplegar |
| 7 | Decidir R-1 (sobre ilegible sin respaldo) y R-2 (cancelar predicciones con timeout) antes de usar la IA con muchos usuarios | Código | Orden posterior |
| 8 | Hallazgos de coste y trazabilidad: H-G9, H-G4, H-G5, P-SC, API-SCV (`auditoria_activacion_ia/04`) | Política, prompt o código | Orden posterior |
| 9 | Validación profesional de la calidad deportiva y evaluación con usuarios reales | Externo | — |
