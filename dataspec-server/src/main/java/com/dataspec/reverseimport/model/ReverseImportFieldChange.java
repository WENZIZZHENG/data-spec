package com.dataspec.reverseimport.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字段属性差异。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReverseImportFieldChange {

    private String property;
    private String currentValue;
    private String standardValue;
}
