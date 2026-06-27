package com.dataspec.changelog.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.changelog.entity.StandardChangeLog;
import com.dataspec.changelog.repository.StandardChangeLogRepository;
import com.dataspec.changelog.service.StandardChangeLogService;
import com.dataspec.common.exception.BizException;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 标准变更记录服务实现。
 */
@Service
@RequiredArgsConstructor
public class StandardChangeLogServiceImpl implements StandardChangeLogService {

    private final StandardChangeLogRepository standardChangeLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    public String snapshot(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BizException("标准变更快照序列化失败: " + e.getMessage());
        }
    }

    @Override
    public void recordChange(Long projectId,
                             String targetType,
                             Long targetId,
                             String action,
                             String beforeJson,
                             String afterJson) {
        StandardChangeLog log = new StandardChangeLog();
        log.setProjectId(projectId);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setAction(action);
        log.setBeforeJson(beforeJson);
        log.setAfterJson(afterJson);
        log.setOperatorName(DataSpecSecurityContext.currentOperator());
        log.setChangedAt(LocalDateTime.now());
        standardChangeLogRepository.insert(log);
    }

    @Override
    public StandardChangeLog getById(Long id) {
        if (id == null) {
            throw new BizException("变更日志ID不能为空");
        }
        return standardChangeLogRepository.findById(id)
                .orElseThrow(() -> new BizException("变更日志不存在: " + id));
    }

    @Override
    public IPage<StandardChangeLog> page(Long projectId, String targetType, Long targetId, int current, int size) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        return standardChangeLogRepository.page(projectId, targetType, targetId, current, size);
    }
}
