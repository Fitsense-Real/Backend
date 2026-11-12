package main.web.services.fitsense.planning.infrastructure.validation;

import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La brecha que V4-C cierra en el pipeline real. Allí la duración declarada es la CALCULADA (el normalizador B2 la
 * reescribe con {@code estimateMinutes}, redondeo al minuto), y la V4 histórica la compara con {@code round(M x 1,15)}.
 * Admite así sesiones de hasta {@code (round(M x 1,15) + 0,5) x 60 - 1} segundos estimados: por encima del 115 %.
 * V4-C compara los segundos y rechaza desde {@code M x 69 + 1}.
 */
@DisplayName("V4: brecha de redondeo de la historica frente a V4-C, con la duracion declarada = calculada")
class BrechaRedondeoV4Test {

    @ParameterizedTest(name = "{0} min, {1} s estimados: historica rechaza={2}, V4-C rechaza={3}")
    @CsvSource({
            // limite V4-C exacto: pasan las dos
            "30,2070,false,false", "45,3105,false,false", "60,4140,false,false", "75,5175,false,false",
            // un segundo por encima del 115 %: V4-C rechaza; la historica (35, 52, 69, 86 min) todavia no
            "30,2071,false,true", "45,3106,false,true", "60,4141,false,true", "75,5176,false,true",
            // el ultimo segundo que la historica admite (redondeo al minuto)
            "30,2129,false,true", "45,3149,false,true", "60,4169,false,true", "75,5189,false,true",
            // el primero que la historica rechaza
            "30,2130,true,true", "45,3150,true,true", "60,4170,true,true", "75,5190,true,true"})
    void brecha(int m, long segundos, boolean historica, boolean v4c) {
        int declarados = (int) Math.max(1, Math.round(segundos / 60.0)); // lo que escribe el normalizador
        int techo = (int) Math.round(m * 1.15);
        assertEquals(historica, PlanningPolicy.HISTORICA.excedeTecho(segundos, declarados, m),
                "historica: " + declarados + " min frente a " + techo);
        assertEquals(v4c, PlanningPolicy.V4C.excedeTecho(segundos, declarados, m));
    }
}
