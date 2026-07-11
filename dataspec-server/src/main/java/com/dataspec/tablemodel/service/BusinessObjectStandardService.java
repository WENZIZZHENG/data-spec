package com.dataspec.tablemodel.service;

import com.dataspec.tablemodel.model.BusinessObjectStandardReq;
import com.dataspec.tablemodel.model.BusinessObjectStandardResp;
import com.dataspec.tablemodel.model.TableRelationSummary;

import java.util.List;

/**
 * 业务对象与表结构标准服务，负责项目隔离、JSON 边界和 relation summary 组装。
 */
public interface BusinessObjectStandardService {

    /** 查询项目下的业务对象标准。 */
    List<BusinessObjectStandardResp> listByProject(Long projectId);

    /** 查询单个业务对象标准。 */
    BusinessObjectStandardResp getById(Long id);

    /** 按项目内对象键查询业务对象标准。 */
    BusinessObjectStandardResp getByObjectKey(Long projectId, String objectKey);

    /** 创建业务对象标准。 */
    BusinessObjectStandardResp create(BusinessObjectStandardReq req);

    /** 更新业务对象标准。 */
    BusinessObjectStandardResp update(Long id, BusinessObjectStandardReq req);

    /** 删除业务对象标准。 */
    void delete(Long id);

    /** 获取项目级业务对象与表模板关系摘要。 */
    TableRelationSummary relationSummary(Long projectId);
}
