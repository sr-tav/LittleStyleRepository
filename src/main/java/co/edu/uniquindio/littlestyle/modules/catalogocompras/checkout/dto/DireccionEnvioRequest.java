package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.validation.CodigoPostalCoherente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dirección de entrega en Colombia con celular de contacto.
 *
 * <p>La dirección se captura estructurada con la nomenclatura colombiana
 * («Calle 45 # 12 - 34»): tipo de vía, número de vía, número de cruce y placa.
 * El texto {@code direccion} se calcula siempre en el servidor a partir de esas partes
 * y no se acepta tal cual desde el cliente. Si solo viene {@code direccion} (datos
 * históricos sin estructura) se conserva sin cambios.
 */
@CodigoPostalCoherente
public record DireccionEnvioRequest(
        @NotBlank(message = "El nombre del destinatario es obligatorio")
        @Size(max = 120, message = "El destinatario no puede superar 120 caracteres")
        String destinatario,
        @Size(max = 200, message = "La dirección no puede superar 200 caracteres")
        String direccion,
        @NotBlank(message = "El tipo de vía es obligatorio")
        @Pattern(regexp = "^(Calle|Carrera|Avenida|Avenida Calle|Avenida Carrera|Diagonal|Transversal|Circular|Autopista|Variante|Kilómetro)$",
                message = "El tipo de vía no es válido")
        String tipoVia,
        @NotBlank(message = "El número de la vía es obligatorio")
        @Pattern(regexp = "^\\d{1,3}[A-Za-z]?( ?[Bb][Ii][Ss])?( ?(Norte|Sur|Este|Oeste))?$",
                message = "El número de la vía no es válido (ej. 45, 45A, 45 bis, 45 Sur)")
        String numeroVia,
        @NotBlank(message = "El número de cruce es obligatorio")
        @Pattern(regexp = "^\\d{1,3}[A-Za-z]?( ?[Bb][Ii][Ss])?( ?(Norte|Sur|Este|Oeste))?$",
                message = "El número de cruce no es válido (ej. 12, 12B, 12 bis)")
        String numeroCruce,
        @NotBlank(message = "La placa es obligatoria")
        @Pattern(regexp = "^\\d{1,3}$", message = "La placa debe tener entre 1 y 3 dígitos")
        String placa,
        @Size(max = 120, message = "El complemento no puede superar 120 caracteres")
        String complemento,
        @NotBlank(message = "El código postal es obligatorio")
        @Pattern(regexp = "^\\d{6}$", message = "El código postal debe tener 6 dígitos")
        String codigoPostal,
        @NotBlank(message = "El departamento es obligatorio")
        @Size(max = 80, message = "El departamento no puede superar 80 caracteres")
        String departamento,
        @NotBlank(message = "El municipio es obligatorio")
        @Size(max = 80, message = "El municipio no puede superar 80 caracteres")
        String municipio,
        @NotBlank(message = "El celular de contacto es obligatorio")
        @Pattern(regexp = "^3\\d{9}$", message = "El celular debe tener 10 dígitos e iniciar por 3")
        String telefono
) {

    /** Reconstruye el texto de la dirección cuando llegan las partes estructuradas. */
    public DireccionEnvioRequest {
        if (hayPartes(tipoVia, numeroVia, numeroCruce, placa)) {
            direccion = componer(tipoVia, numeroVia, numeroCruce, placa);
        }
    }

    /** Texto legible: {@code «Calle 45 # 12 - 34»}. */
    public static String componer(String tipoVia, String numeroVia, String numeroCruce, String placa) {
        return tipoVia.trim() + " " + numeroVia.trim() + " # " + numeroCruce.trim() + " - " + placa.trim();
    }

    private static boolean hayPartes(String... partes) {
        for (String parte : partes) {
            if (parte == null || parte.isBlank()) {
                return false;
            }
        }
        return true;
    }
}
