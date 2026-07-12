package com.dataspec.testdata.service;

import com.dataspec.testdata.model.StandardTestDataPackage;
import com.dataspec.testdata.model.StandardTestDataPackageReq;

/**
 * 标准测试数据包服务。
 *
 * <p>服务只读取项目标准元数据并生成合成 mock/seed/case 草稿，不写入 DataSpec 项目、业务仓库或源数据库。</p>
 */
public interface StandardTestDataPackageService {

    /**
     * 生成标准驱动测试数据包。
     *
     * @param req 生成请求，必须包含 projectId。
     * @return 安全、确定性的测试数据包。
     */
    StandardTestDataPackage generate(StandardTestDataPackageReq req);
}
