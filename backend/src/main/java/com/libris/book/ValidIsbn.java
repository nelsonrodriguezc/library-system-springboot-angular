package com.libris.book;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Rejects malformed ISBNs at the edge, check digit included. */
@Documented
@Constraint(validatedBy = ValidIsbn.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidIsbn {

    String message() default "El ISBN no es válido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidIsbn, String> {

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            // Blank is left to @NotBlank so the caller gets one clear message per problem.
            return value == null || value.isBlank() || Isbn.isValid(value);
        }
    }
}
