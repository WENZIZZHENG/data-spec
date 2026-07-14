package com.dataspec.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 按 Unicode code point 而非 UTF-16 code unit 校验字符序列长度。
 *
 * <p>该口径与 JSON Schema、JavaScript 展开字符串和 DataSpec 脱敏限长保持一致，
 * supplementary 字符只计为一个字符。</p>
 */
@Documented
@Constraint(validatedBy = CodePointSizeValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER,
        ElementType.ANNOTATION_TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CodePointSize {

    /** 校验失败消息。 */
    String message() default "长度超出允许的 Unicode code point 范围";

    /** 最小 Unicode code point 数。 */
    int min() default 0;

    /** 最大 Unicode code point 数。 */
    int max() default Integer.MAX_VALUE;

    /** Bean Validation 分组。 */
    Class<?>[] groups() default {};

    /** Bean Validation payload。 */
    Class<? extends Payload>[] payload() default {};
}
