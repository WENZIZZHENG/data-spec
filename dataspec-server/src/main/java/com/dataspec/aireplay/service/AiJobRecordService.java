package com.dataspec.aireplay.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.aireplay.entity.AiJobRecord;
import com.dataspec.aireplay.model.AiJobRecordCreateReq;
import com.dataspec.aireplay.model.AiJobRecordDetail;

/**
 * AI 生成与修复决策回放记录服务。
 */
public interface AiJobRecordService {

    AiJobRecord create(AiJobRecordCreateReq req);

    IPage<AiJobRecord> listByProject(Long projectId, String jobType, int current, int size);

    AiJobRecordDetail getDetail(Long id);
}
