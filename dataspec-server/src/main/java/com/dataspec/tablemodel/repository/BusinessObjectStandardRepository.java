package com.dataspec.tablemodel.repository;

import com.dataspec.tablemodel.entity.BusinessObjectStandard;

import java.util.List;
import java.util.Optional;

/**
 * 业务对象标准持久化端口，封装项目内对象键、实体名和模板关联查询。
 */
public interface BusinessObjectStandardRepository {

    /** 查询项目下未删除的业务对象标准。 */
    List<BusinessObjectStandard> findByProjectId(Long projectId);

    /** 根据 ID 查询业务对象标准。 */
    Optional<BusinessObjectStandard> findById(Long id);

    /** 根据项目内对象键查询业务对象标准。 */
    Optional<BusinessObjectStandard> findByObjectKey(Long projectId, String objectKey);

    /** 根据关联模板查询业务对象标准。 */
    List<BusinessObjectStandard> findByTemplateId(Long templateId);

    /** 检查项目内对象键是否重复。 */
    boolean existsByObjectKey(Long projectId, String objectKey, Long excludeId);

    /** 检查项目内实体名是否重复。 */
    boolean existsByEntityName(Long projectId, String entityName, Long excludeId);

    /** 新增业务对象标准。 */
    int insert(BusinessObjectStandard standard);

    /** 更新业务对象标准。 */
    int update(BusinessObjectStandard standard);

    /** 逻辑删除业务对象标准。 */
    int deleteById(Long id);
}
