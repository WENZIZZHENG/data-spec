package com.dataspec.aibatch.model;

import com.dataspec.aibatch.entity.AiBatchRun;

/**
 * 批量任务详情：运行记录元数据 + 反序列化后的交付包。
 */
public record AiBatchRunDetail(
        AiBatchRun run,
        AiBatchDeliveryPackage deliveryPackage
) {
}
