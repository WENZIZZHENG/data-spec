package com.dataspec.standardhealth.model;

import lombok.Data;

/**
 * 创建标准健康快照请求。
 */
@Data
public class StandardHealthSnapshotCreateReq {

    private Long projectId;

    private StandardHealthCoverageInput coverage;
}
