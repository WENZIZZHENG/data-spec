package com.dataspec.fieldconflict.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FieldConflictField {
    private Long fieldId;
    private String name;
    private String displayName;
    private String dataType;
    private Long codeSetId;
    private Boolean sensitive;
    private String status;
    private List<String> aliases = new ArrayList<>();
}
