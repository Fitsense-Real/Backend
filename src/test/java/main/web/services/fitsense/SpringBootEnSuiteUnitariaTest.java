package main.web.services.fitsense;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guardia del aislamiento de {@code mvn test}.
 * <p>
 * Una prueba que levanta Spring arranca Flyway contra la base configurada, y la
 * de application.properties es Neon de PRODUCCION. Por eso toda prueba con
 * contexto de Spring tiene que llamarse {@code *IT}: surefire las excluye y
 * solo corren con {@code mvn verify -Pintegration}, donde cada una declara su
 * propia base.
 * <p>
 * Maven ya bloquea la URL en surefire; esta prueba cubre el otro lado: que nadie
 * anada un {@code @SpringBootTest} con nombre de prueba unitaria y lo vea fallar
 * en silencio por conexion en vez de saber por que no debe estar ahi.
 */
class SpringBootEnSuiteUnitariaTest {

    @Test
    void ningunaPruebaConContextoDeSpringCorreEnLaSuiteUnitaria() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(
                    org.springframework.beans.factory.annotation.AnnotatedBeanDefinition definicion) {
                return true;
            }
        };
        scanner.addIncludeFilter(new AnnotationTypeFilter(SpringBootTest.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(ContextConfiguration.class));

        var conContexto = scanner.findCandidateComponents("main.web.services.fitsense").stream()
                .map(definicion -> definicion.getBeanClassName())
                .toList();

        // Si el escaneo no viera nada, la guardia pasaria sin proteger nada.
        assertTrue(conContexto.contains(FitsenseApplicationIT.class.getName()),
                "El escaneo no detecta las pruebas con contexto: " + conContexto);

        var infractoras = conContexto.stream()
                .filter(nombre -> nombre != null && !nombre.endsWith("IT"))
                .sorted()
                .toList();

        assertTrue(infractoras.isEmpty(),
                "Pruebas con contexto de Spring fuera de *IT (correrian en mvn test "
                        + "contra la base de application.properties): " + infractoras);
    }
}
