package com.dataspec.reverseimport.model;

import lombok.Data;

/**
 * 当前扫描页的轻量汇总，只统计表级 metadata，不采样业务数据行。
 */
@Data
public class DatabaseMetadataScanSummary {

    /** 当前页返回的表数量。 */
    private int pageTableCount;

    /** 用户已选择的表数量，用于前端批次选择提示。 */
    private int selectedTableCount;

    /** 当前连接可见表数量估算。 */
    private int estimatedTableCount;
}
