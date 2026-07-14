package com.dataspec.field.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.field.entity.Field;
import com.dataspec.field.model.FieldBulkUpdatePreview;
import com.dataspec.field.model.FieldBulkUpdateReq;
import com.dataspec.field.model.FieldBulkUpdateResult;
import com.dataspec.field.model.FieldChangeUndoResult;
import com.dataspec.field.model.FieldGroupSummary;
import com.dataspec.field.model.FieldGroupingBatchUpdateReq;
import com.dataspec.field.model.FieldGroupingBatchUpdateResult;
import com.dataspec.field.model.FieldSearchReq;
import com.dataspec.field.model.FieldSearchResult;
import com.dataspec.field.model.FieldSuggestion;

import java.util.List;

/** 标准字段服务接口。 */
public interface FieldService {
    IPage<Field> page(Long projectId, int current, int size);
    List<Field> listByProject(Long projectId);
    Field getById(Long id);
    /** 直接创建标准字段；项目内存在同名 active 候选时要求先处理候选。 */
    Field create(Field field);

    /**
     * 将指定 active 候选采纳为标准字段。
     *
     * @param field             由候选元数据构造的标准字段
     * @param sourceCandidateId 当前正在采纳、可从同名候选冲突检查中排除的候选 ID
     */
    Field createFromCandidate(Field field, Long sourceCandidateId);
    Field update(Long id, Field field);
    void delete(Long id);
    FieldSearchResult search(FieldSearchReq req);
    List<FieldSuggestion> suggest(Long projectId, String query, int limit);
    FieldGroupSummary groupSummary(Long projectId);
    FieldGroupingBatchUpdateResult batchUpdateGrouping(FieldGroupingBatchUpdateReq req);
    FieldBulkUpdatePreview previewBulkUpdate(FieldBulkUpdateReq req);
    FieldBulkUpdateResult bulkUpdateFields(FieldBulkUpdateReq req);
    FieldChangeUndoResult undoFieldChange(Long fieldId, Long logId);
}
