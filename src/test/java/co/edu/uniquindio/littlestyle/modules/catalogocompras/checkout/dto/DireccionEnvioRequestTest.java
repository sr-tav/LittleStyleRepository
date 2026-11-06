package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DireccionEnvioRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private DireccionEnvioRequest request(String tipoVia, String numeroVia, String numeroCruce, String placa) {
        return new DireccionEnvioRequest("Ana Ruiz", "texto libre ignorado", tipoVia, numeroVia, numeroCruce,
                placa, "Apto 302", "630001", "Quindío", "Armenia", "3001234567");
    }

    @Test
    void componeLaDireccionDesdeLasPartes() {
        var r = request("Calle", "45A", "12", "34");

        assertThat(r.direccion()).isEqualTo("Calle 45A # 12 - 34");
        assertThat(validator.validate(r)).isEmpty();
    }

    @Test
    void ignoraElTextoLibreCuandoHayPartesEstructuradas() {
        assertThat(request("Carrera", "13", "7", "8").direccion()).isEqualTo("Carrera 13 # 7 - 8");
    }

    @Test
    void aceptaBisYOrientacion() {
        assertThat(validator.validate(request("Avenida Calle", "45 bis", "12B", "34"))).isEmpty();
        assertThat(validator.validate(request("Carrera", "10 Sur", "5", "20"))).isEmpty();
    }

    @Test
    void rechazaTipoDeViaDesconocido() {
        var violaciones = validator.validate(request("Callejón", "45", "12", "34"));

        assertThat(violaciones).anyMatch(v -> v.getPropertyPath().toString().equals("tipoVia"));
    }

    @Test
    void rechazaNumerosConFormatoInvalido() {
        assertThat(validator.validate(request("Calle", "abc", "12", "34")))
                .anyMatch(v -> v.getPropertyPath().toString().equals("numeroVia"));
        assertThat(validator.validate(request("Calle", "45", "1234", "34")))
                .anyMatch(v -> v.getPropertyPath().toString().equals("numeroCruce"));
        assertThat(validator.validate(request("Calle", "45", "12", "3a")))
                .anyMatch(v -> v.getPropertyPath().toString().equals("placa"));
    }

    @Test
    void exigeCadaParteEstructurada() {
        var violaciones = validator.validate(request(null, "", " ", null));

        assertThat(violaciones).extracting(v -> v.getPropertyPath().toString())
                .contains("tipoVia", "numeroVia", "numeroCruce", "placa");
    }

    @Test
    void conservaLaDireccionHistoricaSiNoHayPartes() {
        var r = new DireccionEnvioRequest("Ana Ruiz", "Calle 1 # 2-3", null, null, null, null,
                null, "630001", "Quindío", "Armenia", "3001234567");

        assertThat(r.direccion()).isEqualTo("Calle 1 # 2-3");
    }

    @Test
    void rechazaCodigoPostalQueNoCorrespondeAlMunicipio() {
        var r = new DireccionEnvioRequest("Ana Ruiz", null, "Calle", "45", "12", "34",
                null, "631001", "Quindío", "Armenia", "3001234567");

        var violaciones = validator.validate(r);

        assertThat(violaciones).anyMatch(v -> v.getPropertyPath().toString().equals("codigoPostal")
                && v.getMessage().contains("municipio"));
    }

    @Test
    void rechazaCodigoPostalQueNoCorrespondeAlDepartamento() {
        var r = new DireccionEnvioRequest("Ana Ruiz", null, "Calle", "45", "12", "34",
                null, "050001", "Quindío", "Armenia", "3001234567");

        assertThat(validator.validate(r)).anyMatch(v -> v.getPropertyPath().toString().equals("codigoPostal")
                && v.getMessage().contains("departamento"));
    }

    @Test
    void aceptaUnCodigoExistenteDelMunicipio() {
        var r = new DireccionEnvioRequest("Ana Ruiz", null, "Calle", "45", "12", "34",
                null, "630008", "Quindío", "Armenia", "3001234567");

        assertThat(validator.validate(r)).isEmpty();
    }
}
