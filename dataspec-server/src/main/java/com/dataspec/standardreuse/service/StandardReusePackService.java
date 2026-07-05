package com.dataspec.standardreuse.service;

import com.dataspec.standardreuse.model.StandardReusePackApplicationInfo;
import com.dataspec.standardreuse.model.StandardReusePackApplyReq;
import com.dataspec.standardreuse.model.StandardReusePackApplyResult;
import com.dataspec.standardreuse.model.StandardReusePackCreateReq;
import com.dataspec.standardreuse.model.StandardReusePackDetail;
import com.dataspec.standardreuse.model.StandardReusePackDriftReport;
import com.dataspec.standardreuse.model.StandardReusePackInfo;
import com.dataspec.standardreuse.model.StandardReusePackPlan;

import java.util.List;

/**
 * 标准复用包服务。
 *
 * <p>负责从源项目创建版本化共享包、对目标项目 dry-run、确认应用并输出漂移报告。</p>
 */
public interface StandardReusePackService {

    /** 查询源项目下的复用包列表。 */
    List<StandardReusePackInfo> listPacks(Long projectId);

    /** 查询复用包详情。 */
    StandardReusePackDetail getPack(Long packId);

    /** 从源项目创建版本化标准复用包。 */
    StandardReusePackDetail createPack(StandardReusePackCreateReq req);

    /** 对目标项目进行应用预览，不写入任何标准资产。 */
    StandardReusePackPlan previewApply(StandardReusePackApplyReq req);

    /** 确认应用复用包，只创建缺失资产并记录应用摘要。 */
    StandardReusePackApplyResult applyPack(StandardReusePackApplyReq req);

    /** 查询目标项目复用包应用记录。 */
    List<StandardReusePackApplicationInfo> listApplications(Long projectId);

    /** 计算目标项目相对指定复用包的漂移报告。 */
    StandardReusePackDriftReport driftReport(Long packId, Long targetProjectId);
}
