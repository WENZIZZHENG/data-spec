package com.dataspec.projectbackup.model;

import com.dataspec.changelog.entity.StandardChangeLog;
import com.dataspec.domain.entity.Domain;
import com.dataspec.field.entity.Field;
import com.dataspec.reverseimport.entity.FieldSource;
import com.dataspec.reverseimport.entity.ReverseImportBatch;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rulebaseline.model.RuleBaselinePackage;
import com.dataspec.standard.entity.StandardSnapshot;

import java.util.List;

public record ProjectBackupAssets(
        List<Domain> domains,
        List<Field> fields,
        List<EnumDictBackup> enumDicts,
        List<RuleConfig> rules,
        RuleBaselinePackage ruleBaseline,
        List<TemplateBackup> templates,
        List<StandardSnapshot> snapshots,
        List<ReverseImportBatch> reverseImportBatches,
        List<FieldSource> fieldSources,
        List<StandardChangeLog> changeLogs
) {
}
