package com.dataspec.importexport.service;

import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 导入导出服务（MVP：JSON 格式字段导入导出）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportExportService {

    private final FieldService fieldService;
    private final ObjectMapper objectMapper;

    /**
     * 导出项目字段为 JSON
     */
    public String exportFields(Long projectId) {
        try {
            List<Field> fields = fieldService.listByProject(projectId);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(fields);
        } catch (Exception e) {
            throw new RuntimeException("导出失败", e);
        }
    }

    /**
     * 从 JSON 导入字段
     */
    @Transactional
    public int importFields(Long projectId, String json) {
        try {
            Field[] fields = objectMapper.readValue(json, Field[].class);
            int count = 0;
            for (Field field : fields) {
                field.setId(null); // 重置 ID
                field.setProjectId(projectId);
                field.setCreatedAt(null);
                field.setUpdatedAt(null);
                field.setIsDeleted(null);
                try {
                    fieldService.create(field);
                    count++;
                } catch (Exception e) {
                    log.warn("导入字段 {} 失败: {}", field.getName(), e.getMessage());
                }
            }
            return count;
        } catch (Exception e) {
            throw new RuntimeException("导入失败: " + e.getMessage(), e);
        }
    }
}
