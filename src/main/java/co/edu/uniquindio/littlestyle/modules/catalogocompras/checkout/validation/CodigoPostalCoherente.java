package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** El código postal debe corresponder al departamento y al municipio de la dirección. */
@Documented
@Constraint(validatedBy = CodigoPostalCoherenteValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CodigoPostalCoherente {
    String message() default "El código postal no corresponde a la ubicación";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
