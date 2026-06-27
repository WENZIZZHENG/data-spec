package com.dataspec.aireplay.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.aireplay.entity.AiJobRecord;
import com.dataspec.aireplay.model.AiJobRecordCreateReq;
import com.dataspec.aireplay.model.AiJobRecordDetail;
import com.dataspec.aireplay.repository.AiJobRecordRepository;
import com.dataspec.aireplay.service.AiJobRecordService;
import com.dataspec.common.exception.BizException;
import com.dataspec.security.context.ProjectAccessGuard;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 作业回放记录服务实现。
 */
@Service
@RequiredArgsConstructor
public class AiJobRecordServiceImpl implements AiJobRecordService {

    private static final int REPLAY_SCHEMA_VERSION = 1;

    private final AiJobRecordRepository aiJobRecordRepository;
    private final ObjectMapper objectMapper;

    @Override
    public AiJobRecord create(AiJobRecordCreateReq req) {
        if (req == null || req.projectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        if (isBlank(req.jobType())) {
            throw new BizException("AI 作业类型不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(req.projectId());
        AiJobRecord record = new AiJobRecord();
        record.setProjectId(req.projectId());
        record.setJobType(req.jobType().trim());
        record.setTitle(trimToNull(req.title()));
        record.setInputSummary(trimToNull(req.inputSummary()));
        record.setPromptVersion(trimToNull(req.promptVersion()));
        record.setStatus(isBlank(req.status()) ? "SUCCESS" : req.status().trim());
        record.setInputPayloadJson(writeJson(req.inputPayload()));
        record.setOutputPayloadJson(writeJson(req.outputPayload()));
        record.setStandardSnapshotId(req.standardSnapshotId());
        record.setStandardSnapshotVersion(trimToNull(req.standardSnapshotVersion()));
        record.setStandardSnapshotHash(trimToNull(req.standardSnapshotHash()));
        record.setSqlCheckRecordId(req.sqlCheckRecordId());
        aiJobRecordRepository.insert(record);
        return record;
    }

    @Override
    public IPage<AiJobRecord> listByProject(Long projectId, String jobType, int current, int size) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);
        return aiJobRecordRepository.findByProjectId(projectId, jobType, current, size);
    }

    @Override
    public AiJobRecordDetail getDetail(Long id) {
        if (id == null) {
            throw new BizException("AI 作业记录 ID 不能为空");
        }
        AiJobRecord record = aiJobRecordRepository.findById(id)
                .orElseThrow(() -> new BizException("AI 作业记录不存在: " + id));
        ProjectAccessGuard.requireProjectAccess(record.getProjectId());
        Object inputPayload = readJson(record.getInputPayloadJson());
        Object outputPayload = readJson(record.getOutputPayloadJson());
        return new AiJobRecordDetail(
                record,
                inputPayload,
                outputPayload,
                replayPayload(record, inputPayload, outputPayload),
                replayCommand(record)
        );
    }

    private Map<String, Object> replayPayload(AiJobRecord record, Object inputPayload, Object outputPayload) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schemaVersion", REPLAY_SCHEMA_VERSION);
        payload.put("kind", "dataspec-ai-replay");
        payload.put("jobId", record.getId());
        payload.put("projectId", record.getProjectId());
        payload.put("jobType", record.getJobType());
        payload.put("promptVersion", record.getPromptVersion());
        payload.put("status", record.getStatus());
        if (record.getSqlCheckRecordId() != null) {
            payload.put("sqlCheckRecordId", record.getSqlCheckRecordId());
        }
        payload.put("standard", standardPayload(record));
        payload.put("input", inputPayload);
        payload.put("output", outputPayload);
        return payload;
    }

    private Map<String, Object> standardPayload(AiJobRecord record) {
        Map<String, Object> standard = new LinkedHashMap<>();
        if (record.getStandardSnapshotId() != null) {
            standard.put("snapshotId", record.getStandardSnapshotId());
        }
        if (record.getStandardSnapshotVersion() != null) {
            standard.put("specVersion", record.getStandardSnapshotVersion());
        }
        if (record.getStandardSnapshotHash() != null) {
            standard.put("specHash", record.getStandardSnapshotHash());
        }
        standard.put("versioned", record.getStandardSnapshotId() != null);
        return standard;
    }

    private String replayCommand(AiJobRecord record) {
        return "curl -s \"http://localhost:8090/api/ai-jobs/" + record.getId() + "\"";
    }

    private String writeJson(Object value) {
        if (value == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BizException("AI 作业记录 JSON 序列化失败: " + e.getMessage());
        }
    }

    private Object readJson(String json) {
        if (isBlank(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception ignored) {
            return json;
        }
    }

    private String trimToNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
