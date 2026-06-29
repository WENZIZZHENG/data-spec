package com.dataspec.businessglossary.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目级业务术语表条目。
 */
@Data
@TableName("ds_business_glossary")
public class BusinessGlossary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    /** 主术语，例如“会员”“订单费用”。 */
    private String term;

    /** 同义词，逗号分隔。 */
    private String synonyms;

    /** 英文词根，逗号分隔。 */
    private String rootTerms;

    /** 拼音、历史缩写或英文缩写，逗号分隔。 */
    private String abbreviations;

    /** 禁用或不推荐术语，逗号分隔。 */
    private String disabledTerms;

    /** 推荐 canonical 标准字段。 */
    private Long canonicalFieldId;

    /** 适用范围类型：GLOBAL/CATEGORY/DOMAIN/TAG。 */
    private String scopeType;

    /** 适用范围值。 */
    private String scopeValue;

    /** 示例字段名，逗号分隔。 */
    private String exampleFields;

    private String description;

    /** enabled/disabled。 */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
