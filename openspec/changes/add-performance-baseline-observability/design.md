# 设计

## 方案

第一版采用“可观测但不阻断”的方式：

- `PerformanceProbe` 负责测量操作耗时。低于阈值输出 debug，高于阈值输出 warning，warning 包含 `operation`、`durationMs`、`thresholdMs` 和 `hint`。
- 核心入口只加薄包装，不改变业务返回值：
  - 字段列表、分组摘要、字段推荐。
  - AI Context field catalog 和 zip package 导出。
  - SQL 检查记录分页。
  - 反向导入 compare。
- `PerformanceBaselineTest` 使用合成数据构造千级字段和千级列 compare，不依赖真实数据库或网络。测试输出本地 metric 行，断言功能结果正确，并使用宽松耗时上限防止明显退化。

## 取舍

- 不把耗时阈值做成配置项。第一版先用服务内常量，避免配置膨胀；后续如果需要部署级调参，再引入 `application.yml`。
- 不把性能测试做成严格 benchmark。单测环境波动大，第一版只提供可重复测量和宽松上限，避免 CI 偶发失败。
- 不覆盖前端 E2E。前端大列表真实体验留给 P6-17，本轮只补 README 中的现状和后端基线。

## 风险与缓解

- 日志噪音：只有超过阈值才 warning，低耗时只 debug。
- 测试波动：合成数据规模控制在千级，阈值保守，并只验证明显退化。
- 误解为真实压测：文档明确这是本地合成基线，不代表生产容量承诺。
