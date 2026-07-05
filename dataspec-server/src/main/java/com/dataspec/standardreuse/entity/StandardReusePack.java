package com.dataspec.standardreuse.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目标准复用包。
 *
 * <p>保存源项目标准资产的确定性 JSON payload，用于其他项目做轻量初始化和漂移比较。</p>
 */
@Data
@TableName("ds_standard_reuse_pack")
public class StandardReusePack {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 源项目 ID，决定包的归属与访问边界。 */
    private Long projectId;

    /** 源项目名称快照，便于包被应用后解释来源。 */
    private String sourceProjectName;

    /** 项目内稳定包 key，如 shared_core。 */
    private String packKey;

    /** 用户可读包名称。 */
    private String packName;

    /** 用户定义的共享包版本。 */
    private String basePackVersion;

    /** 包说明。 */
    private String description;

    /** payloadJson 的 SHA-256 hash，用于识别共享包内容版本。 */
    private String packageHash;

    /** 字段、枚举、规则和模板的确定性 JSON，不包含数据库 ID 或源库行值。 */
    private String payloadJson;

    /** 包内资产数量摘要 JSON。 */
    private String assetCountsJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
