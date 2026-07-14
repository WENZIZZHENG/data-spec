package com.dataspec.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link CodePointSize} 的 Bean Validation 实现。
 */
public final class CodePointSizeValidator implements ConstraintValidator<CodePointSize, CharSequence> {

    private int min;
    private int max;

    /** 读取注解声明的长度边界。 */
    @Override
    public void initialize(CodePointSize annotation) {
        min = annotation.min();
        max = annotation.max();
        if (min < 0 || max < min) {
            throw new IllegalArgumentException("CodePointSize 需要满足 0 <= min <= max");
        }
    }

    /** null 交由 @NotNull/@NotBlank 处理，其余值按 code point 计数。 */
    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        int count = Character.codePointCount(value, 0, value.length());
        return count >= min && count <= max;
    }
}
