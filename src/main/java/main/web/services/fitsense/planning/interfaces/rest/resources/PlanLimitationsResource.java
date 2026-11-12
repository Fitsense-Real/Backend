package main.web.services.fitsense.planning.interfaces.rest.resources;

import java.time.LocalDate;
import java.util.List;

/**
 * Lo que el plan NO puede cubrir con lo que el participante declaro (decision I del
 * 30/10/2026). La app lo muestra tal cual: los textos ya estan pensados para el usuario.
 *
 * @param limitations vacia si el plan puede cubrir todo lo que exige su objetivo.
 */
public record PlanLimitationsResource(LocalDate weekStartDate, List<Limitation> limitations) {

    /**
     * @param type                   {@code SLOT} (un patron obligatorio sin ejercicios) o
     *                               {@code CAPABILITY} (una capacidad principal del objetivo).
     * @param code                   el slot ({@code UPPER_PULL}...) o la capacidad ({@code CARDIO}...).
     * @param optionalEquipmentCodes codigos de {@code equipment_types} que la cubririan si el
     *                               participante YA los tiene. Nunca es una compra obligatoria.
     */
    public record Limitation(String type, String code, String title, String message,
                             List<String> optionalEquipmentCodes) {}
}
