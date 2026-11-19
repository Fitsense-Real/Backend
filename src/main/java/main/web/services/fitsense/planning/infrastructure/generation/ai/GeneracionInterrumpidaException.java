package main.web.services.fitsense.planning.infrastructure.generation.ai;

/**
 * V59: el hilo que seguia una prediccion de la generacion asincrona se interrumpio porque la aplicacion se esta
 * apagando (SIGTERM, despliegue). La prediccion sigue viva en Replicate y su id ya esta en el libro (ENVIADA): no se
 * cancela ni se da por terminada, para que la instancia siguiente la reanude por su id sin pagar otra.
 * <p>
 * No es un fallo del proveedor (no cuenta para el cortacircuitos). Quien la recibe no debe publicar nada: la
 * operacion vuelve a la cola ({@code PlanGenerationOperationService#requeueOnShutdown}).
 */
public class GeneracionInterrumpidaException extends RuntimeException {

    public GeneracionInterrumpidaException(String message) {
        super(message);
    }
}
