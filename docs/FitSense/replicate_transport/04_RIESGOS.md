# 04 · Riesgos y limitaciones

**No se afirma que se eliminen todos los duplicados ni todas las pérdidas.** Esto es lo que queda, y por qué.

1. **La causa del corte de G08 no está demostrada.**
   - Se sabe *qué* llegó: 2xx sin `Content-Type` y un reinicio de la conexión a mitad del cuerpo.
   - No se sabe *quién* lo provocó: Replicate, el proxy del entorno de la evaluación o la red. Comprobarlo exigiría POST pagados.
   - Tampoco se sabe **cuántos bytes** llegaron en G08 ni, por tanto, si el id estaba en ellos.
   - **Con esta corrección, G08 se recupera solo si su id llegó completo antes del corte.** Si no, queda incierta: no se duplica, pero se pierde.

2. **Sin el id de la propia respuesta no se recupera nada.** Es una decisión de seguridad: el contenido no prueba la identidad (`02` §1).
   - **Consecuencia:** en un envío incierto sin id (5xx, cierre sin respuesta, límite de lectura antes del cuerpo, `SOLICITANDO` al reanudar), la predicción **puede seguir ejecutándose y facturándose**, y el usuario recibe `RULE_ENGINE`.
   - **Se evita el duplicado, no el coste de esa primera predicción.** Sin id no se puede cancelar.

3. **Datos borrados.** Replicate marca `data_removed` y vacía la entrada. Con esos datos no se puede confirmar la identidad: queda incierta.
   - Solo afecta a la confirmación inmediata de una respuesta cortada, de segundos.
   - El seguimiento de una `ENVIADA` del libro no depende de la entrada.

4. **Una sola consulta de confirmación.** Si ese GET falla, queda incierta, aunque el id fuera bueno. Es conservador a propósito; el sondeo posterior sí tolera fallos.

5. **Los caminos sin libro no sobreviven a un reinicio.** Son el endpoint síncrono, el ciclo semanal y la adaptación.
   - Si el proceso muere con un POST en curso, no queda rastro, y una ejecución nueva pide otra predicción.
   - Cubrirlo exige **extender V59** a esos caminos: un cambio de arquitectura, porque la tabla está ligada a `plan_generation_operations`.
   - **No se hace en esta PR** (`05` §4).
   - En una misma ejecución, esos caminos **no reenvían**: un fallo del proveedor va al respaldo.

6. **Reintento silencioso del POST por el JDK.** `HttpURLConnection` reenvía un POST que no esté en modo *streaming*. El cliente actual de Spring 7.0.9 sí usa *streaming*. Las pruebas `t05CerradaSinRespuesta` y `s09PostSinRespuesta` lo detectarían si se cambiara la fábrica HTTP.

7. **Cuerpo de la respuesta en memoria.** Se lee entero (`exchange`), igual que antes con `body(String)`. Incluye el prompt repetido, unos 100 KB.

8. **Sondeo más tolerante.** Hasta 3 fallos seguidos antes de dejar de esperar, dentro del mismo límite total. Una caída real tarda unas consultas más en llevar al respaldo.

9. **Peticiones idénticas del mismo usuario.** La reutilización V59 entre operaciones (`reutilizable`: mismo usuario, misma huella, menos de 24 h) **no cambia**. Se basa en los registros propios de la base, no en el contenido de Replicate.

10. **Lo que no se prueba aquí.** Replicate real: no hay gasto. Tampoco el despliegue en Railway, Mobile ni Firebase.

**Alternativa evaluada y no adoptada: `Cancel-After`.** Es una cabecera documentada que cancela la predicción a los N segundos. Acotaría el coste de una predicción incierta. Cambia el comportamiento de todas las predicciones, así que queda como recomendación (`05` §4).
