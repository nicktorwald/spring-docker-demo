package org.nicktorwald.platform.dice.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.ReportAsSingleViolation;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * Restricts a number of attempts per request from 1 up to 5.
 */
@Min(1)
@Max(5)
@Documented
@ReportAsSingleViolation
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
public @interface ValidRollTimes {

    String message() default "{org.nicktorwald.platform.dice.api.ValidRollTimes}";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}
