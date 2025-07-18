package com.ling.lingkb.entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Critical code hint
 *
 * @author shipotian
 * @version 1.0.0
 * @since 2025/6/25
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
public @interface CodeHint {
    String value() default "";
}
