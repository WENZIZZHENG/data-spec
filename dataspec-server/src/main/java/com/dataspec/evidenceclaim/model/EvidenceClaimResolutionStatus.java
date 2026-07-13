package com.dataspec.evidenceclaim.model;

/**
 * Evidence claim 的项目级确定性解析状态。
 */
public enum EvidenceClaimResolutionStatus {
    /** 持久化来源存在且属于当前项目。 */
    VERIFIED,
    /** URI 格式与来源类型有效，但记录不存在。 */
    MISSING,
    /** 记录存在但属于其他项目；不得返回目标项目元数据。 */
    CROSS_PROJECT,
    /** URI、来源类型或项目归属无法由本地持久化数据验证。 */
    UNVERIFIABLE
}
