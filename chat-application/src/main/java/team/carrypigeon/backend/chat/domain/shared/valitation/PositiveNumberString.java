package team.carrypigeon.backend.chat.domain.shared.valitation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Target({FIELD, METHOD, PARAMETER, RECORD_COMPONENT, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
@NotBlank
@Pattern(regexp = "[1-9][0-9]*")
@ReportAsSingleViolation
public @interface PositiveNumberString {
    String message() default "must be a positive integer";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
