package com.dataspec.rulebaseline.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目当前规则基线元数据。
 *
 * <p>实际生效规则仍存放在 ds_rule_config；本表只记录最近一次基线应用/导入来源，
 * 供前端和 AI Context 解释规则来源。</p>
 */
@Data
@TableName("ds_rule_baseline")
public class RuleBaseline {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String baselineKey;

    private String baselineName;

    private String baselineVersion;

    private String source;

    private LocalDateTime appliedAt;

    private String rulesJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
