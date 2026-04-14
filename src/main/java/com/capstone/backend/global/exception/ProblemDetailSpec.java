package com.capstone.backend.global.exception;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.http.HttpStatus;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProblemDetailSpec {

    HttpStatus status();

    String code();

    String title() default "";

    String type() default "about:blank";
}
