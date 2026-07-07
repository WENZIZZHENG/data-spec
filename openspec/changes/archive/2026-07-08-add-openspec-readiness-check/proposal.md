## Why

TODO 到 OpenSpec 草稿生成后，真正开工前还需要判断 proposal、design、spec 和 tasks 是否已经足够明确。`openspec validate` 能检查格式，却不会判断影响范围、验收标准、边界、验证命令和人工确认问题是否足够实施；AI 很容易在信息不足时边做边猜。

## What Changes

- 新增本地只读脚本 `tools/dataspec-openspec-readiness.mjs`，输入 active change id，扫描该 change 的 OpenSpec artifacts。
- 输出稳定 JSON 与可读 text 报告，包含 `readinessScore`、`readinessLevel`、`missingFacts`、`affectedSpecs`、`validationPlan`、`reviewBoundary`、`riskFlags`、`humanQuestions`、`checks` 和 `nextActions`。
- 为缺验收标准、缺边界、缺影响规格、缺验证命令、模板占位、缺 tasks 或缺 spec delta 等情况给出确定性诊断。
- 补充 Node 测试和 README/TODO 说明，把第一版作为开工前提示和 AI 自检入口。

## Capabilities

### New Capabilities
- `openspec-readiness-check`: 对 repo-local OpenSpec change 做本地准备度评分和缺口检查。

### Modified Capabilities
- 无。

## Impact

- 工具范围：新增 `tools/dataspec-openspec-readiness.mjs` 与对应测试。
- 文档范围：更新 `README.md` 的开发验证说明和 `TODO.md` 的 P6-177 状态。
- 规格范围：新增 `openspec-readiness-check` delta spec。
- 安全边界：只读取仓库内 OpenSpec 文档，不联网、不读取业务数据、不执行、提交、归档或修改 change。
