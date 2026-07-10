package com.dataspec.aioutputcheck.service;

import com.dataspec.aioutputcheck.model.AiOutputPostCheckRequest;
import com.dataspec.aioutputcheck.model.AiOutputPostCheckResult;

/**
 * AI 输出后置校验服务。
 *
 * <p>服务只读检查 AI 生成文本中的标准引用、规则、快照和证据声明，不保存 raw content，
 * 不修改标准、AI job、业务文件或数据库。</p>
 */
public interface AiOutputPostCheckService {

    /**
     * 执行确定性后置校验。
     *
     * @param request 待校验 AI 输出和项目边界。
     * @return PASS/WARN/FAIL 结果、引用解析详情、脱敏问题和下一步动作。
     */
    AiOutputPostCheckResult check(AiOutputPostCheckRequest request);
}
