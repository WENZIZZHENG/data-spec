package com.dataspec.syntheticexample.service;

import com.dataspec.syntheticexample.model.SyntheticStandardExamplePackage;

/**
 * 合成标准样例生成服务，只读组合项目标准字段、模板和内置业务场景骨架。
 */
public interface SyntheticStandardExampleService {

    /**
     * 生成指定项目和场景的合成标准样例包。
     *
     * @param projectId DataSpec 项目 ID，不能为空。
     * @param scenario 支持 user、order、payment、audit。
     * @param maxCases 单类样例的最大数量；为空时使用默认值。
     * @return 稳定、脱敏、只读的合成样例包。
     */
    SyntheticStandardExamplePackage generate(Long projectId, String scenario, Integer maxCases);
}
