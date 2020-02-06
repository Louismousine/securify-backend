package com.dto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;
import org.springframework.stereotype.Component;
@Constraint(validatedBy = PasswordConstraintValidator.class)
@Target({ ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface ValidPassword {
    String message() default "Invalid password";
 
    Class<?>[] groups() default {};
 
    Class<? extends Payload>[] payload() default {};
 
}
