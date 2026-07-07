## 1. OpenSpec 与范围确认

- [x] 1.1 确认 proposal、design 和 spec delta 与 P6-88 范围一致，第一版坚持 CLI 本地只读扫描、不自动修改业务代码、不新增持久化索引表。
- [x] 1.2 运行 `openspec validate add-code-field-reference-index --strict`，修复 artifacts 格式或契约问题。

## 2. CLI 引用索引核心

- [x] 2.1 为 `index-refs` 补失败优先测试：defaultPaths 缺失、显式路径扫描、多字段/alias、SQL/DDL 高置信、model/config 中置信、敏感片段脱敏、跳过生成目录。
- [x] 2.2 实现本地扫描工具：解析 field/alias、解析 explicit paths 或 defaultPaths、递归扫描允许文件、跳过生成目录、输出相对路径和行列。
- [x] 2.3 实现 referenceKind、confidence、possibleReference、renameRisk、suggestedAction、diagnostics 和 nextActions 聚合。
- [x] 2.4 接入 `dataspec-cli.mjs` 的 `index-refs` 命令、help 文本、JSON/text 输出和退出码。

## 3. 契约与字段影响展示

- [x] 3.1 更新 CLI/MCP contract fixture 和 fixture 校验测试，覆盖 `index-refs` 的输出结构、安全元数据、示例和 nextActions。
- [x] 3.2 扩展后端字段影响类型和 summary 模型，支持 `CODE_REFERENCE` / `codeReferenceImpactCount` 的兼容输出。
- [x] 3.3 更新前端类型、字段影响展示工具和字段影响弹窗，显示业务代码引用计数与标签。
- [x] 3.4 补充后端/前端定点测试，覆盖字段影响新增类型不会破坏既有影响报告。

## 4. 文档、验证与评审

- [x] 4.1 更新 README/TODO，记录 `index-refs` 用法、安全边界、P6-88 完成状态和剩余限制。
- [x] 4.2 运行相关验证：OpenSpec strict、tools `index-refs` 定点测试、CLI/MCP fixture 检查、后端定点测试、前端定点测试、必要时全量 tools/前端/后端验证。
- [x] 4.3 在本文件追加 `Verification Evidence`，记录命令、结果、敏感词扫描和未覆盖风险。
- [x] 4.4 启动独立子 agent 做强制代码评审，处理或记录 findings，关闭子 agent。
- [x] 4.5 按项目 Git 规则核对 staged diff、敏感词扫描并创建本地 commit；不主动 push，不自动 archive OpenSpec。

## Verification Evidence

- OpenSpec：`openspec validate add-code-field-reference-index --strict` 通过；`openspec validate --all` 通过，110 passed / 0 failed。
- CLI/tools：`node --test tools/dataspec-cli.test.mjs --test-name-pattern "index-refs"` 通过，119 passed / 1 skipped；修复评审发现后新增覆盖多字段 `field=alias`、歧义 alias 拒绝、越界路径拒绝、缺失路径退出 2、text diagnostics；`node --test tools/dataspec-cli-mcp-contract-check.test.mjs --test-name-pattern "index-refs|fixtures validate"` 通过，15 passed；`node tools/dataspec-cli-mcp-contract-check.mjs --format json` 返回 `ok=true`；`node --test tools/*.test.mjs` 通过，215 passed / 1 skipped。
- 后端：`mvn "-Dtest=FieldImpactServiceImplTest,FieldImpactControllerTest" test` 通过，5 tests / 0 failures / 0 errors；`mvn test` 通过，484 tests / 0 failures / 0 errors。Maven 仍有本机既有 `javax.annotation-api` 父 POM parse warning，不影响 BUILD SUCCESS。
- 前端：`pnpm exec node --test tests/fieldImpactDisplay.test.ts tests/frontendSmoke.test.ts` 通过，32 passed；`pnpm test` 通过，150 passed；`pnpm build` 通过。build 输出存在既有 Rolldown `@vueuse/core` pure annotation warning 和 chunk size warning，不阻断构建。
- 状态与 diff：`node tools/dataspec-status-check.mjs --format json` 返回 `status=warn`，仅报告 active OpenSpec change 存在，包括本次保持 active 的 `add-code-field-reference-index` 和既有 active changes；无 error。`git diff --check` 通过，仅 Windows LF/CRLF 提示。
- 独立代码评审：子 agent `019f38d3-b599-7243-b9b3-f7ad0e7307fe` 用途为 P6-88 强制只读代码评审。首轮发现两个 Important：扫描路径可越界、顶层缺失路径仍返回 0；另有 text diagnostics、fixture outputShape、TODO `ignorePatterns` wording 等 Minor。已修复并补测试；复核结论为无新的 Critical / Important，可进入最终验证 / commit。该 agent 已关闭。
- 提交门禁：写入本 Evidence 后，按项目 Git 规则复跑 `git status --short`、`git diff --check`、`git diff --cached --check`、`git diff --cached --stat`、staged 文件名敏感词扫描和 staged diff 敏感词扫描。敏感词扫描只允许命中安全边界说明、脱敏规则、fixture/test 中的占位或脱敏示例，不得包含真实 password、token、secret、Authorization、JDBC URL、DSN 或连接串。
- 未覆盖风险：第一版是确定性文本扫描，不做完整 AST / language server，不自动修改业务代码，不新增持久化引用索引表，不让后端读取任意业务仓库路径；低置信命中仍需人工确认。

## Archive Verification Evidence

- 2026-07-07：执行 `openspec archive add-code-field-reference-index --yes`，同步 `code-field-reference-index`、`dataspec-cli`、`field-impact-analysis` 主规格，并归档到 `openspec/changes/archive/2026-07-07-add-code-field-reference-index/`。
- 2026-07-07：补齐新建主规格 `openspec/specs/code-field-reference-index/spec.md` 的 Purpose，确认默认占位文本扫描无命中。
- 2026-07-07：`openspec validate --all` 通过，118 passed、0 failed。
- 2026-07-07：`node --test tools/dataspec-status-check.test.mjs tools/dataspec-verify-advisor.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs` 通过，44 pass、0 fail。
- 2026-07-07：`node tools/dataspec-status-check.mjs --format json` 返回 `status=warn`，active change warning 从 8 降至 2；第三条 next action 为 `当前问题编码：OPENSPEC_ACTIVE_CHANGE_PRESENT(count=2,severity=warning)`。
- 2026-07-07：`git diff --check` 退出码 0，仅输出 Windows LF/CRLF 提示。
- 2026-07-07：独立只读复评子 agent `019f3ac9-9655-7961-9767-c75277266b0a`（Noether）复评 staged archive diff，结论 Ready，无 Critical / Important / Minor findings；已调用 `close_agent` 关闭。
