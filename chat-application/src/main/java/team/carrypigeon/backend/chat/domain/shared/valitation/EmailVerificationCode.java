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

/**
 * 邮箱验证码的校验注解
 */
@Documented
@Target({FIELD, METHOD, PARAMETER, RECORD_COMPONENT, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
@NotBlank
@Pattern(regexp = "[0-9]{6}")
@ReportAsSingleViolation
public @interface EmailVerificationCode {
    String message() default "code must be exactly 6 digits";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
