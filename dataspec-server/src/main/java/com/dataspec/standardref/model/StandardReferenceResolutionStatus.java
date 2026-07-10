package com.dataspec.standardref.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 标准引用解析状态。
 */
@Schema(description = "标准引用解析状态；调用方不得在 AMBIGUOUS、UNKNOWN 或 CROSS_PROJECT 时猜测 canonical 对象。")
public enum StandardReferenceResolutionStatus {
    /** 引用唯一命中当前可用标准对象。 */
    CURRENT,
    /** 引用命中废弃、停用、被替代或需要迁移确认的标准对象。 */
    STALE,
    /** 引用在当前项目内命中多个对象，系统不会猜测 canonicalRef。 */
    AMBIGUOUS,
    /** 引用在当前项目内无法解析。 */
    UNKNOWN,
    /** stableRef 指向其他项目，系统不会暴露目标项目对象元数据。 */
    CROSS_PROJECT
}
