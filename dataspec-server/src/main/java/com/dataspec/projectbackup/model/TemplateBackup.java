package com.dataspec.projectbackup.model;

import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;

import java.util.List;

public record TemplateBackup(
        Template template,
        List<TemplateField> fields
) {
}
