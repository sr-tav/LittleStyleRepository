package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.service;

import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

@Component
public class ValidadorMedidas {

    public void validar(LocalDate fechaNacimiento, LocalDate fechaMedicion,
                        BigDecimal estaturaCm, BigDecimal pesoKg) {
        LocalDate hoy = LocalDate.now();
        if (fechaMedicion.isAfter(hoy)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "La fecha de medición no puede ser futura", "fechaMedicion");
        }
        if (fechaMedicion.isBefore(fechaNacimiento)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "La medición no puede ser anterior a la fecha de nacimiento", "fechaMedicion");
        }
        validarRango(estaturaCm, new BigDecimal("40"), new BigDecimal("200"),
                "La estatura debe estar entre 40 y 200 cm", "estaturaCm");
        validarRango(pesoKg, new BigDecimal("2"), new BigDecimal("120"),
                "El peso debe estar entre 2 y 120 kg", "pesoKg");

        int edad = Period.between(fechaNacimiento, fechaMedicion).getYears();
        RangoMedidas rango = rangoParaEdad(edad);
        validarRango(estaturaCm, rango.estaturaMinima(), rango.estaturaMaxima(),
                "La estatura no es coherente con la edad del perfil", "estaturaCm");
        validarRango(pesoKg, rango.pesoMinimo(), rango.pesoMaximo(),
                "El peso no es coherente con la edad del perfil", "pesoKg");
    }

    private static void validarRango(BigDecimal valor, BigDecimal minimo, BigDecimal maximo,
                                     String mensaje, String campo) {
        if (valor == null || valor.compareTo(minimo) < 0 || valor.compareTo(maximo) > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, mensaje, campo);
        }
    }

    private static RangoMedidas rangoParaEdad(int edad) {
        if (edad < 2) {
            return new RangoMedidas("40", "100", "2", "20");
        }
        if (edad <= 5) {
            return new RangoMedidas("60", "130", "5", "40");
        }
        if (edad <= 9) {
            return new RangoMedidas("80", "160", "10", "80");
        }
        if (edad <= 13) {
            return new RangoMedidas("100", "190", "15", "120");
        }
        return new RangoMedidas("120", "200", "25", "120");
    }

    private record RangoMedidas(BigDecimal estaturaMinima, BigDecimal estaturaMaxima,
                                BigDecimal pesoMinimo, BigDecimal pesoMaximo) {
        private RangoMedidas(String estaturaMinima, String estaturaMaxima,
                             String pesoMinimo, String pesoMaximo) {
            this(new BigDecimal(estaturaMinima), new BigDecimal(estaturaMaxima),
                    new BigDecimal(pesoMinimo), new BigDecimal(pesoMaximo));
        }
    }
}
