# Migración de Replicate: `openai/gpt-5-structured` → `openai/gpt-5-mini` directo (G5M)

**Orden:** «ORDEN DE IMPLEMENTACIÓN — BACKEND FITSENSE / KORA», 23/11/2026.
**Autorización:** implementar y probar. **NO MERGE**: no se fusiona, no se cambian las políticas de entrenamiento y no se despliega hasta revisar los resultados.

**Rama:** `feature/replicate-gpt-5-mini`, creada desde `generation` en `e46dbe18`. PR independiente hacia `generation`.

## Resultado en una línea

`openai/gpt-5-mini` directo **genera y adapta rutinas reales que pasan los validadores**, sin regresiones.
- Con el esfuerzo inicial de la orden (`low`), la generación inicial queda por debajo de la línea base.
- Con `medium`, aplicado por resultados medidos, recupera la calidad de `gpt-5-structured`:
  - 11/19 propuestas válidas a la primera;
  - 18/19 planes desde la IA;
  - oráculo al 99,3 %.

## Índice

| Entregable de la orden | Documento |
|---|---|
| 1. Diagnóstico de E002 (hechos e hipótesis) | `01_DIAGNOSTICO_E002.md` |
| 2. Resumen de cambios | `02_CAMBIOS.md` |
| 3. Evidencia de generación real | `03_EVIDENCIA_REAL.md` §A |
| 4. Evidencia de adaptación real | `03_EVIDENCIA_REAL.md` §B |
| 5. Resultados de los perfiles sintéticos | `04_PERFILES_Y_COMPARACION.md` §2 |
| 6. Comparación de resultados, tiempos y costes | `04_PERFILES_Y_COMPARACION.md` §2–§4 |
| 7. Pruebas automatizadas y CI | `06_PRUEBAS_Y_CI.md` |
| 8. Incidencias, riesgos y limitaciones | `05_INCIDENCIAS_RIESGOS_LIMITACIONES.md` |
| 9. PR y commit final | Descripción de la PR |
| Configuración efectiva, compatibilidad con O20 y V65 (orden «MERGE O20 E INTEGRACIÓN GPT-5 MINI») | `07_CONFIGURACION_EFECTIVA_Y_O20.md` |

**Evidencia** (`evidencia/`):
- `corridas/*.tar.gz`: salida completa del arnés y registro de cada corrida real, con sha256 en `SHA256SUMS.txt`;
- `oraculo/`: resultados del oráculo v1.4;
- `sondas_e002/`: las tres sondas del diagnóstico;
- `correr_G5M.sh` y `analizar_G5M.py`.

## Presupuesto (máximo USD 8)

| Concepto | Predicciones | USD |
|---|---:|---:|
| Sondas de diagnóstico (A, que falló con E002 sin tokens, B y C, triviales) | 3 | ≈ 0,00 |
| Corridas del arnés, cargadas por **cota superior** (gpt-5-mini no informa tokens) | 102 | **6,5362** |
| **Total cargado** | 105 | **≤ 6,54** |
| Mínimo estimado sin razonamiento (`04` §3) | | 1,18 |

- El control de presupuesto se detuvo **antes** del tope en cada corrida.
- Las dos paradas por reserva (`05` I-1, I-2) fueron del control de presupuesto, no del proveedor.
