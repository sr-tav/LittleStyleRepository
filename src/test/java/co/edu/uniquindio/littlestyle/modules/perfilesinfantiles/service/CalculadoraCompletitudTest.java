package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.service;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.AlergiaTextil;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Contextura;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Holgura;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.MedicionCrecimiento;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.PerfilInfantil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraCompletitudTest {

    private final CalculadoraCompletitud calculadora = new CalculadoraCompletitud();

    @Test
    void consideraElPerfilCompletoCuandoTieneDatosYMedicion() {
        PerfilInfantil perfil = PerfilInfantil.builder()
                .nombre("Sofía")
                .fechaNacimiento(LocalDate.now().minusYears(6))
                .alergias(Set.of(AlergiaTextil.LANA))
                .build();
        perfil.setContextura(Contextura.MEDIA);
        perfil.setHolgura(Holgura.REGULAR);
        perfil.setColoresPreferidos(Set.of(ColorPreferido.AZUL));
        perfil.setEstampadosPreferidos(Set.of());
        MedicionCrecimiento medicion = MedicionCrecimiento.builder()
                .estaturaCm(new BigDecimal("110"))
                .pesoKg(new BigDecimal("22"))
                .build();

        assertThat(calculadora.calcular(perfil, medicion)).isEqualTo(100);
        assertThat(calculadora.camposPendientes(perfil, medicion)).isEmpty();
    }

    @Test
    void marcaComoDiligenciadaLaDeclaracionExplicitaDeNoAlergias() {
        PerfilInfantil perfil = PerfilInfantil.builder()
                .nombre("Sofía")
                .fechaNacimiento(LocalDate.now().minusYears(6))
                .build();
        perfil.setContextura(Contextura.MEDIA);
        perfil.setHolgura(Holgura.REGULAR);
        perfil.setSinAlergias(true);

        assertThat(calculadora.calcular(perfil, null)).isEqualTo(63);
        assertThat(calculadora.camposPendientes(perfil, null))
                .containsExactly("Estatura", "Peso", "Agrega al menos un color o estampado preferido");
    }

    @Test
    void consideraLaPreferenciaLibreComoPreferenciaDiligenciada() {
        PerfilInfantil perfil = PerfilInfantil.builder()
                .nombre("Sofía")
                .fechaNacimiento(LocalDate.now().minusYears(6))
                .otroColor("Turquesa")
                .build();
        perfil.setContextura(Contextura.MEDIA);
        perfil.setHolgura(Holgura.REGULAR);
        perfil.setSinAlergias(true);

        assertThat(calculadora.calcular(perfil, null)).isEqualTo(75);
    }
}
