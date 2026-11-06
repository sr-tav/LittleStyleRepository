package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.validation;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.DireccionEnvioRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Valida el código postal contra el departamento y el municipio y reporta el error en {@code codigoPostal}. */
public class CodigoPostalCoherenteValidator
        implements ConstraintValidator<CodigoPostalCoherente, DireccionEnvioRequest> {

    @Override
    public boolean isValid(DireccionEnvioRequest direccion, ConstraintValidatorContext context) {
        if (direccion == null || direccion.codigoPostal() == null || direccion.codigoPostal().isBlank()
                || direccion.departamento() == null || direccion.municipio() == null) {
            return true; // los campos obligatorios ya se validan por separado
        }
        var resultado = CatalogoCodigosPostales.validar(
                direccion.departamento(), direccion.municipio(), direccion.codigoPostal().trim());
        if (resultado == CatalogoCodigosPostales.Resultado.VALIDO) {
            return true;
        }
        String mensaje = resultado == CatalogoCodigosPostales.Resultado.NO_CORRESPONDE_AL_DEPARTAMENTO
                ? "El código postal no corresponde al departamento seleccionado"
                : "El código postal no corresponde al municipio seleccionado";
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(mensaje)
                .addPropertyNode("codigoPostal")
                .addConstraintViolation();
        return false;
    }
}
