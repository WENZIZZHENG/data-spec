package com.dataspec.aitaskrun.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.aitaskrun.entity.AiTaskRun;
import com.dataspec.aitaskrun.model.AiTaskPartialArtifact;
import com.dataspec.aitaskrun.model.AiTaskResumeInfo;
import com.dataspec.aitaskrun.model.AiTaskRunDetail;
import com.dataspec.aitaskrun.model.AiTaskRunFinishCommand;
import com.dataspec.aitaskrun.model.AiTaskRunListItem;
import com.dataspec.aitaskrun.model.AiTaskRunStartCommand;
import com.dataspec.aitaskrun.model.AiTaskStepStatus;
import com.dataspec.aitaskrun.repository.AiTaskRunRepository;
import com.dataspec.aitaskrun.service.AiTaskRunService;
import com.dataspec.common.exception.BizException;
import com.dataspec.common.result.PageResult;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.dataspec.security.context.ProjectAccessGuard;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 任务运行状态服务实现。只记录诊断与恢复建议，不调度后台任务。
 */
@Service
@RequiredArgsConstructor
public class AiTaskRunServiceImpl implements AiTaskRunService {

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCEEDED = "SUCCEEDED";
    public static final String STATUS_PARTIAL_FAILED = "PARTIAL_FAILED";
    public static final String STATUS_FAILED = "FAILED";

    private static final TypeReference<List<AiTaskStepStatus>> STEP_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<AiTaskPartialArtifact>> ARTIFACT_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final AiTaskRunRepository aiTaskRunRepository;
    private final ObjectMapper objectMapper;

    @Override
    public AiTaskRun start(AiTaskRunStartCommand command) {
        if (command == null || command.projectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        if (isBlank(command.taskType())) {
            throw new BizException("AI 任务类型不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(command.projectId());
        AiTaskRun run = new AiTaskRun();
        run.setProjectId(command.projectId());
        run.setTaskType(safeText(command.taskType()));
        run.setSourceType(safeText(command.sourceType()));
        run.setStatus(STATUS_RUNNING);
        run.setInputHash(safeText(command.inputHash()));
        run.setIdempotencyKey(safeText(command.idempotencyKey()));
        run.setStepStatusJson(writeJson(safeSteps(command.stepStatus())));
        run.setRetryable(false);
        run.setPartialArtifactsJson(writeJson(safeArtifacts(command.partialArtifacts())));
        run.setMetadataJson(writeJson(sanitizeValue(safeMap(command.metadata()))));
        run.setOperatorName(DataSpecSecurityContext.currentOperator());
        run.setStartedAt(LocalDateTime.now());
        run.setExpiresAt(command.expiresAt());
        aiTaskRunRepository.insert(run);
        return run;
    }

    @Override
    public AiTaskRun succeed(AiTaskRun run, AiTaskRunFinishCommand command) {
        return finish(run, STATUS_SUCCEEDED, command);
    }

    @Override
    public AiTaskRun partialFail(AiTaskRun run, AiTaskRunFinishCommand command) {
        return finish(run, STATUS_PARTIAL_FAILED, command);
    }

    @Override
    public AiTaskRun fail(AiTaskRun run, AiTaskRunFinishCommand command) {
        return finish(run, STATUS_FAILED, command);
    }

    @Override
    public PageResult<AiTaskRunListItem> list(Long projectId, String status, String taskType, int current, int size) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);
        IPage<AiTaskRun> page = aiTaskRunRepository.findByProjectId(
                projectId,
                safeFilter(status),
                safeFilter(taskType),
                Math.max(1, current),
                Math.max(1, Math.min(size, 100)));
        PageResult<AiTaskRunListItem> result = new PageResult<>();
        result.setRecords(page.getRecords().stream().map(AiTaskRunListItem::from).toList());
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setPages(page.getPages());
        return result;
    }

    @Override
    public List<AiTaskRunListItem> recentFailures(Long projectId, Integer limit) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);
        return aiTaskRunRepository.findRecentFailures(projectId, limit == null ? 10 : limit)
                .stream()
                .map(AiTaskRunListItem::from)
                .toList();
    }

    @Override
    public AiTaskRunDetail detail(Long projectId, Long id) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        if (id == null) {
            throw new BizException("AI 任务运行 ID 不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);
        AiTaskRun run = aiTaskRunRepository.findById(id)
                .orElseThrow(() -> new BizException("AI 任务运行不存在: " + id));
        if (!projectId.equals(run.getProjectId())) {
            throw new BizException("AI 任务运行不属于当前项目");
        }
        return AiTaskRunDetail.from(
                run,
                readJsonList(run.getStepStatusJson(), STEP_LIST_TYPE),
                readJsonList(run.getPartialArtifactsJson(), ARTIFACT_LIST_TYPE),
                readJsonMap(run.getMetadataJson())
        );
    }

    @Override
    public AiTaskResumeInfo resumeInfo(AiTaskRun run) {
        if (run == null) {
            return null;
        }
        return new AiTaskResumeInfo(
                run.getId(),
                run.getStatus(),
                run.getRetryable(),
                run.getFailedStep(),
                run.getResumeCommand(),
                run.getNextAction()
        );
    }

    private AiTaskRun finish(AiTaskRun run, String status, AiTaskRunFinishCommand command) {
        if (run == null || run.getId() == null) {
            throw new BizException("AI 任务运行不能为空");
        }
        AiTaskRun persisted = aiTaskRunRepository.findById(run.getId()).orElse(run);
        persisted.setStatus(status);
        if (command != null) {
            persisted.setSourceId(command.sourceId());
            persisted.setRetryable(Boolean.TRUE.equals(command.retryable()));
            persisted.setFailedStep(safeText(command.failedStep()));
            persisted.setResumeCommand(safeText(command.resumeCommand()));
            persisted.setNextAction(safeText(command.nextAction()));
            persisted.setStepStatusJson(writeJson(safeSteps(command.stepStatus())));
            persisted.setPartialArtifactsJson(writeJson(safeArtifacts(command.partialArtifacts())));
            persisted.setMetadataJson(writeJson(sanitizeValue(safeMap(command.metadata()))));
            persisted.setExpiresAt(command.expiresAt());
        }
        persisted.setFinishedAt(LocalDateTime.now());
        aiTaskRunRepository.update(persisted);
        return persisted;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BizException("AI 任务运行 JSON 序列化失败: " + e.getMessage());
        }
    }

    private <T> List<T> readJsonList(String json, TypeReference<List<T>> type) {
        if (isBlank(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private Map<String, Object> readJsonMap(String json) {
        if (isBlank(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private Object sanitizeValue(Object value) {
        return SensitiveDataSanitizer.sanitizeValue(value);
    }

    private String safeText(String value) {
        return SensitiveDataSanitizer.redactText(value);
    }

    private <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }

    private List<AiTaskStepStatus> safeSteps(List<AiTaskStepStatus> steps) {
        return safeList(steps).stream()
                .map(step -> new AiTaskStepStatus(
                        safeText(step.step()),
                        safeText(step.status()),
                        safeText(step.message()),
                        safeText(step.artifactRef())))
                .toList();
    }

    private List<AiTaskPartialArtifact> safeArtifacts(List<AiTaskPartialArtifact> artifacts) {
        return safeList(artifacts).stream()
                .map(artifact -> new AiTaskPartialArtifact(
                        safeText(artifact.type()),
                        safeText(artifact.name()),
                        safeText(artifact.ref()),
                        safeText(artifact.summary())))
                .toList();
    }

    private Map<String, Object> safeMap(Map<String, Object> value) {
        return value == null ? Map.of() : new LinkedHashMap<>(value);
    }

    private String safeFilter(String value) {
        return isBlank(value) ? null : safeText(value.trim());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
