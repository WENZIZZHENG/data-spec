package com.dataspec.lint.model;

/**
 * fixedSql 自动修复风险等级，数值越高越需要人工确认。
 */
public enum FixRiskLevel {
    LOW(1),
    MEDIUM(2),
    HIGH(3);

    private final int rank;

    FixRiskLevel(int rank) {
        this.rank = rank;
    }

    public boolean allowedBy(FixRiskLevel maxRiskLevel) {
        FixRiskLevel max = maxRiskLevel == null ? MEDIUM : maxRiskLevel;
        return rank <= max.rank;
    }
}
