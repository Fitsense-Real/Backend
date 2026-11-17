# Conclusión de la validación con Replicate real

Fecha: 17/11/2026. Rama `validacion/replicate-real` (sin fusionar). Base `65e1b7e`; código ejecutado `d1ac33f`.

## 1. Veredicto

**La generación asíncrona con IA real NO puede darse por validada todavía.** El motivo es externo: el modelo
`openai/gpt-5-structured` en Replicate devolvió E002 en todas las peticiones de la cuenta, incluida una sonda trivial,
y siguió así 31 minutos después.

Lo que sí queda demostrado:

1. **Un defecto real, encontrado y corregido antes de gastar** (HR-1). Un apagado ordenado con una generación en
   curso cancelaba la predicción pagada y publicaba el respaldo. Corrección mínima con su prueba determinista (R21),
   que falla antes y pasa después. Regresión: 51/51 IT asíncronas y 1576 unitarias, 0 fallos.
2. **Reinicio (SIGTERM y kill -9) y vencimiento con reintento, de extremo a extremo** con el backend real y un
   Replicate simulado:
   - la predicción en curso se reanuda por su id, sin POST nuevo;
   - el resultado tardío se adopta;
   - nunca hay planes duplicados;
   - el libro cuadra con los POST.
3. **Con el proveedor real caído, el backend degrada como está diseñado.** El libro cuadra 1 a 1 con Replicate, no
   gasta más intentos, el participante recibe un plan válido del motor de reglas en ~3 s y el cortacircuitos cuenta
   los fallos.

## 2. Tabla de evaluación

| Escenario | Resultado | Predicciones nuevas | Tiempo | Coste real | Observaciones |
|---|---|---|---|---|---|
| Ensayo sin coste | Conforme tras HR-1 | 0 reales (25 simuladas) | — | USD 0 | HR-1 corregido (`d1ac33f`). HR-2 era un defecto del simulador. |
| G1 | **No superado**: respaldo por fallo del proveedor | 2 (G1 + 1 repetición) | 4,1 s y 2,9 s | estimado USD 0,00 (0 tokens) | E002 idéntico en las dos. El libro coincide con Replicate. |
| Diagnóstico | Fallo del proveedor confirmado | 2 sondas mínimas | 0,3 s cada una | estimado USD 0,00 | El fallo no depende de la entrada y persiste 31 min. |
| G2 | **No ejecutado** con IA real | 0 | — | USD 0 | Sin predicción en curso no hay nada que reanudar. Validado sin coste. |
| G3 | **No ejecutado** con IA real | 0 | — | USD 0 | Igual. Validado sin coste. |
| **Total** | Bloqueado por el proveedor | **4** (2 del backend + 2 sondas) | — | **estimado USD 0,00** | Cargo real por confirmar en el panel de Replicate |

## 3. Cifras

| Medida | Valor |
|---|---|
| Predicciones facturables enviadas | 4, todas `failed` y con 0 tokens |
| Predicciones de operaciones del backend | 2 |
| Operaciones internas (backend) | 2, ambas COMPLETED con respaldo |
| Reutilizadas | 0 en real (1 en el ensayo) |
| Duplicadas | 0 |
| NO_CONFIRMADA | 0 |
| Reintentos de IA | 0 (política: ante un fallo del proveedor no se reintenta) |
| Rechazos de validadores | 0 (no hubo propuestas de la IA) |
| Planes publicados | 2, `RULE_ENGINE`, base de pruebas |
| Operaciones fallidas / recuperadas en real | 0 / 0 |
| Coste estimado | USD 0,00 (tokens × tarifa) |
| Presupuesto no usado | USD 10,00 de USD 10 (sin cargos esperados; por confirmar en el panel) |

## 4. Para reanudar la campaña

1. Revisar en el panel de Replicate:
   - el estado del modelo `openai/gpt-5-structured`;
   - la facturación de la cuenta (saldo, límite y método de pago).

   Con los ids de `CONCILIACION_REPLICATE.md`, soporte puede identificar el fallo.
2. Cuando una predicción mínima vuelva a terminar con éxito, repetir G1 → G2 → G3 con este mismo protocolo congelado
   (`PROTOCOLO_CONGELADO.md`), sin cambiar el modelo. Coste previsto: < USD 0,30.
3. Decidir la integración de `d1ac33f` en `generation`, que es necesaria antes de desplegar.

No se ha fusionado nada ni se ha desplegado nada. No se usaron datos reales y el catálogo y los validadores no se
han tocado.
