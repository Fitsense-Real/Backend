# 04 · Riesgos y limitaciones

**No se afirma que se eliminen todos los duplicados ni todas las pérdidas.** Esto es lo que queda, y por qué.

1. **La causa del corte de G08 no está demostrada.** Se sabe *qué* llegó: cabeceras 2xx sin `Content-Type` y una conexión reiniciada a mitad del cuerpo. No se sabe *quién* lo provocó, Replicate, el proxy del entorno de la evaluación o la red. Comprobarlo exigiría repetir POST pagados. La corrección no depende de saberlo: trata cualquier lectura fallida como incierta.

2. **La recuperación puede no encontrar la predicción.** En ese caso queda `PREDICCION_NO_CONFIRMADA` y se publica el respaldo `RULE_ENGINE`.
   - **La predicción puede seguir ejecutándose y facturándose:** sin id no se puede cancelar. Se evita el duplicado, no el coste de la primera.
   - Cuándo no se encuentra:
     - si en la ventana hay más de 20 predicciones de la API del mismo modelo, por ejemplo con mucho tráfico de la cuenta, o la nuestra queda fuera de la primera página del listado;
     - si hay **dos** con la entrada idéntica, por ejemplo el mismo usuario reenviando enseguida la misma semana: queda ambigua y no se elige ninguna;
     - si una consulta falla durante la búsqueda: no se puede descartar nada, así que queda incierta;
     - si el reloj del servidor difiere en más de 2 minutos del de Replicate;
     - si Replicate tarda más de dos rondas, de aproximadamente 1 s, en mostrarla en el listado.

3. **Reanudar después de una hora.** Replicate borra la entrada de las predicciones de la API a la hora (`data_removed`), así que no se puede comparar. Un intento `SOLICITANDO` reanudado más tarde queda `NO_CONFIRMADA`, como antes.

4. **Los caminos sin libro no sobreviven a un reinicio.** El endpoint síncrono, el ciclo semanal y la adaptación no anotan la predicción en V59.
   - Si el proceso muere con un POST en curso, no queda rastro, y una nueva ejecución pediría otra.
   - Cubrirlo exige **extender el libro a esos caminos**: ahora está ligado a `plan_generation_operations`. Es un cambio de arquitectura, y quizá de esquema.
   - **No se hace en esta PR.** Se propone por separado (`05` §4).

5. **Reintento silencioso del POST por el JDK.** `HttpURLConnection` reenvía un POST que no está en modo *streaming*. El cliente actual de Spring (7.0.9) sí usa *streaming*, y la prueba `t05CerradaSinRespuesta` lo vigila. Si se cambiara la fábrica HTTP, por ejemplo a una con el cuerpo en memoria, esa prueba lo detectaría.

6. **Coste de la búsqueda.** Solo en envíos inciertos: hasta 2 listados y 20 consultas por ronda, todos `GET` gratuitos. Cada consulta devuelve la entrada completa, unos 100 KB con el catálogo, y alarga unos segundos el camino incierto. Todo está acotado por los límites de lectura del sondeo.

7. **Sondeo más tolerante.** Hasta 3 fallos seguidos antes de dejar de esperar, dentro del mismo límite total. Una caída real de Replicate tarda unas consultas más en llevar al respaldo, sin pasar el límite.

8. **Adoptar la predicción de otra ejecución de la misma operación.** Solo ocurre en una carrera con el libro: las dos siguen **la misma** predicción, y el libro registra un solo final. No se crea otra.

9. **Lo que no se prueba aquí.** Replicate real: no hay gasto. Mobile, Railway y Firebase no se tocan.
