package com.dataspec.aibatch.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.aibatch.entity.AiBatchRun;
import com.dataspec.aibatch.model.AiBatchDeliveryPackage;
import com.dataspec.aibatch.model.AiBatchRunDetail;
import com.dataspec.aibatch.model.AiBatchSqlLintReq;

/**
 * AI 批量任务交付包服务。
 */
public interface AiBatchService {

    AiBatchDeliveryPackage createSqlLintBatch(AiBatchSqlLintReq req);

    IPage<AiBatchRun> listByProject(Long projectId, int current, int size);

    AiBatchRunDetail getDetail(Long id);

    AiBatchDeliveryPackage getPackage(Long id);
}
