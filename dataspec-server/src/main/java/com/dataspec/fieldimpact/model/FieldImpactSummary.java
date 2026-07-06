package com.dataspec.fieldimpact.model;

import lombok.Data;

/**
 * 字段影响报告汇总。
 */
@Data
public class FieldImpactSummary {

    private int totalImpactCount;
    private int templateImpactCount;
    private int importSourceImpactCount;
    private int sqlCheckImpactCount;
    private int snapshotImpactCount;
    private int codeSetImpactCount;
    /** 业务仓库字段引用索引命中的影响项数量；只表示摘要，不包含本地业务代码内容。 */
    private int codeReferenceImpactCount;
    private int warningCount;
}
