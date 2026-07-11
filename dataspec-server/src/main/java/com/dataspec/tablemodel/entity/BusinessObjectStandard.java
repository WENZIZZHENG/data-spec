package com.dataspec.tablemodel.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务对象标准持久化实体，保存业务实体、表模板依赖和关系提示的结构化 JSON。
 */
@Data
@TableName("ds_business_object_standard")
public class BusinessObjectStandard {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目 ID，所有读写都必须校验项目访问权限。 */
    private Long projectId;

    /** 项目内唯一业务对象键，用于 CLI/MCP/AI Context 稳定引用。 */
    private String objectKey;

    /** 人可读业务实体名称，例如订单、用户、支付记录。 */
    private String entityName;

    /** 推荐表名模式或前缀提示，仅用于建表 guidance。 */
    private String tablePattern;

    /** 可选关联表模板 ID，必须属于同一项目。 */
    private Long templateId;

    /** 必选字段引用数组 JSON，不得包含真实业务数据行或凭据。 */
    private String requiredFieldsJson;

    /** 可选字段引用数组 JSON，不得包含真实业务数据行或凭据。 */
    private String optionalFieldsJson;

    /** 业务对象关系数组 JSON，只表达结构提示，不保存 raw SQL。 */
    private String relationsJson;

    /** 外键提示数组 JSON，用于 DDL preview 和 AI guidance。 */
    private String foreignKeyHintsJson;

    /** 审计字段提示 JSON。 */
    private String auditFieldsJson;

    /** 常见误用或反模式数组 JSON。 */
    private String commonPitfallsJson;

    /** AI 建表或 SQL 生成时的补充说明，必须经过敏感信息检查。 */
    private String aiUsageNotes;

    /** 是否默认导出到 AI Context。 */
    private Boolean contextExport;

    /** 业务对象状态，ENABLED 时默认可被消费。 */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
