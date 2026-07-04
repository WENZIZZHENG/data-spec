## Why

字段质量、覆盖率和 SQL lint 已经能查看，但 CI/AI 自动化还缺少统一的项目级质量门禁。P6-59 第一版要把这些信号组合成可配置、可解释、可机器读取的 pass/fail 结果，避免质量退化只能事后人工发现。

## What Changes

- 新增项目级标准质量门禁配置和评估 API，支持 `minCoverage`、`minAverageFieldScore`、`maxErrorIssues`、`maxNewUnmanagedFields`、`requiredSensitiveMarking` 等阈值。
- 质量门禁评估复用现有字段质量、覆盖率和 SQL lint 结果，不把阈值硬编码进规则实现。
- CLI 新增质量门禁检查命令，输出稳定 JSON，失败时返回非零退出码，方便业务仓库 CI 使用。
- 前端展示当前项目质量门禁状态、失败项和下一步修复建议。
- AI 可读取失败项、阈值、实际值和 `nextActions`，按优先级修复标准或 SQL。

## Capabilities

### New Capabilities

- `standard-quality-gate`: 项目级标准质量门禁配置、评估结果、失败项和修复建议。

### Modified Capabilities

- `dataspec-cli`: 新增质量门禁检查命令和 AI/CI 可读 JSON 结果。
- `frontend-task-entrypoints`: 前端标准维护入口展示质量门禁状态和失败项。

## Impact

- 后端：新增 Flyway 迁移、质量门禁模型/API/service/repository，并复用字段质量、覆盖率和 SQL lint 统计。
- 前端：更新 OpenAPI 类型、API 封装和标准健康/字段质量相关入口，显示 gate 状态与失败项。
- CLI：新增 `quality-gate check` 或等价命令，支持 `--project`、`--format json` 和 CI 退出码。
- 文档与规格：更新 README/TODO/OpenSpec，明确第一版不做企业审批流、不默认阻断个人本地保存、不新增后台调度。

## Verification Evidence

- `mvn test`：397 tests，0 failures，0 errors。
- `pnpm gen:api`：OpenAPI 重新生成 `dataspec-web/src/api/schema.ts` 成功。
- `pnpm test`：101 tests，0 failures。
- `pnpm build`：`vue-tsc --noEmit && vite build` 通过；仅保留既有 Rolldown PURE annotation 与 chunk size 警告。
- `pnpm check:api`：`schema.ts` 已是最新。
- `node --test tools\*.test.mjs`：137 tests，0 failures。
- `openspec validate add-quality-gate-policy --strict`：通过。
- `openspec validate --all`：93 items passed，0 failed。
- `git diff --check`：无空白错误；仅 CRLF 提示。
- 独立 review agent 发现 2 个 P2：evaluate 输入范围校验、CLI 参数错误脱敏；已修复并补测试，复核结论为“两个 P2 已修复，未发现新的阻塞性问题”。
