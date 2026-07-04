package com.dataspec.aitaskrun.service;

import com.dataspec.aitaskrun.entity.AiTaskRun;
import com.dataspec.aitaskrun.model.AiTaskResumeInfo;
import com.dataspec.aitaskrun.model.AiTaskRunDetail;
import com.dataspec.aitaskrun.model.AiTaskRunFinishCommand;
import com.dataspec.aitaskrun.model.AiTaskRunListItem;
import com.dataspec.aitaskrun.model.AiTaskRunStartCommand;
import com.dataspec.common.result.PageResult;

import java.util.List;

/**
 * AI 任务运行状态服务。
 */
public interface AiTaskRunService {

    AiTaskRun start(AiTaskRunStartCommand command);

    AiTaskRun succeed(AiTaskRun run, AiTaskRunFinishCommand command);

    AiTaskRun partialFail(AiTaskRun run, AiTaskRunFinishCommand command);

    AiTaskRun fail(AiTaskRun run, AiTaskRunFinishCommand command);

    PageResult<AiTaskRunListItem> list(Long projectId, String status, String taskType, int current, int size);

    List<AiTaskRunListItem> recentFailures(Long projectId, Integer limit);

    AiTaskRunDetail detail(Long projectId, Long id);

    AiTaskResumeInfo resumeInfo(AiTaskRun run);
}
