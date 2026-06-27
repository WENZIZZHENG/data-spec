package com.dataspec.fieldimpact.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 编辑字段关键属性前的非阻断提示。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldEditWarning {

    private String attribute;
    private FieldImpactSeverity severity;
    private String message;
}
