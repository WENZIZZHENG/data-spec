## 1. OpenSpec 与范围确认

- [x] 1.1 校验 proposal、design 和 spec delta 与 P6-93 范围一致：只读契约候选预览、后端 API、CLI/fixture、测试与文档；不写正式标准、不自动入箱、不调用外部 LLM、不访问外部 URL、不自动 archive。
- [x] 1.2 运行 `openspec validate add-contract-candidate-import --strict`，修复 artifacts 格式或契约问题。

## 2. 后端契约候选预览

- [x] 2.1 先补失败测试：OpenAPI、JSON Schema、Protobuf 文本/descriptor 风格输入可抽取候选，`contractHash` 确定性，现有字段去重，复杂 schema diagnostics，安全 metadata 和敏感值脱敏。
- [x] 2.2 实现 `contractimport` model/service/controller，新增 `POST /api/contract-import/preview`，读取项目标准字段并生成只读 preview package、candidate fields、inbox payload、diagnostics 和 nextActions。
- [x] 2.3 补充 controller/MockMvc 测试，覆盖路由、sourceKind 参数、缺失/超限输入、服务错误脱敏和只读安全边界。
- [x] 2.4 补充后端 contract fixture/golden 断言，确保 preview stable fields、candidate stable fields 和 redaction 防漂移。

## 3. CLI 与契约 fixture

- [x] 3.1 先补失败测试：`contract-import preview --format json` 调用后端并保留稳定字段，`--format text` 输出摘要，非法参数、缺失文件和服务错误返回 exit code 2 且脱敏。
- [x] 3.2 实现 CLI `contract-import preview`、help 文本、参数校验、文件读取、JSON/text 输出和请求错误诊断。
- [x] 3.3 更新 CLI/MCP contract fixture 和 fixture 校验测试，覆盖 `contract-import preview` 输出 shape、安全 metadata、unsafe 示例拒绝和 recommended next actions。

## 4. 文档与 TODO

- [x] 4.1 更新 README，记录契约候选预览 API/CLI、支持 sourceKind、输出字段、只读边界、脱敏策略和通过 `inboxPayload` 复用候选审核流程的说明。
- [x] 4.2 更新 TODO，将 P6-93 标记为已完成第一版，并记录已完成能力、限制和验收证据。

## 5. 验证、评审与提交

- [x] 5.1 运行相关验证：后端定点 Maven 测试、CLI 定点测试、CLI/MCP fixture 测试、`node --test tools/*.test.mjs`、OpenSpec strict/all、`git diff --check`。
- [x] 5.2 在本文件追加 `Verification Evidence`，记录命令结果、敏感词扫描、子 agent 信息、结构化自审和未覆盖风险。
- [x] 5.3 启动独立子 agent 做强制只读代码评审，处理或记录 findings，并关闭子 agent。
- [x] 5.4 按项目 Git 规则核对 staged diff、敏感词扫描并创建本地 commit；不主动 push，不自动 archive OpenSpec。

## Verification Evidence

- 验证时间：2026-07-07。
- RED 证据：新增回归测试后，`mvn "-Dtest=ContractCandidateImportServiceImplTest" test` 在 `dataspec-server` 失败，覆盖 `sourcePath` 敏感属性名未脱敏、500 字符后契约变化未改变 hash、OpenAPI array response 未抽候选；`node --test tools/dataspec-cli-mcp-contract-check.test.mjs` 失败，覆盖 `contract-import-preview` fixture 缺少完整 candidate stable fields。
- 评审修复：按子 agent findings 修复派生 `sourcePath` / `sourceRef` 脱敏，`contractHash` 改用完整脱敏 `sourcePath` 和 `contractContent`，OpenAPI/JSON Schema array `items` 递归抽取，CLI/MCP fixture 补齐 `candidateKey/displayName/required/enumValues/exampleValues/schemaVersion/confidence`，并为 contract import API model 补字段级 `@Schema` 描述。
- 后端定点验证：在 `dataspec-server` 运行 `mvn "-Dtest=ContractCandidateImportServiceImplTest,ContractCandidateImportControllerTest,ContractCandidateImportContractFixtureTest" test`，结果 BUILD SUCCESS，14 tests / 0 failures / 0 errors / 0 skipped；Maven 输出本地 `javax.annotation-api` 传递 POM warning，但不影响本次测试结果。曾误在仓库根目录运行同命令，因根目录无 `pom.xml` 失败，已按正确模块目录重跑通过。
- CLI 定点验证：`node --test tools/dataspec-cli.test.mjs`，结果 140 tests，138 pass，2 skipped，0 fail。
- CLI/MCP fixture 定点验证：`node --test tools/dataspec-cli-mcp-contract-check.test.mjs`，结果 21 pass，0 fail。
- tools 全量验证：`node --test tools/*.test.mjs`，结果 242 tests，240 pass，2 skipped，0 fail；跳过项为当前 Windows 权限下无法创建 symlink 的既有平台条件测试。
- OpenSpec 验证：`openspec validate add-contract-candidate-import --strict` 通过；`openspec validate --all` 通过，114 items passed，0 failed。
- diff 检查：`git diff --check` 退出 0，仅提示 README/TODO/tools 文件下次 Git touch 时 LF 会转 CRLF，无 whitespace error。
- 敏感词扫描：`git diff -- . | Select-String -Pattern '(?i)(password|passwd|token|secret|authorization|api_key|apikey|jdbc:|dsn)'` 和针对新建未跟踪目录的 `rg -n -i "password|passwd|token|secret|authorization|api_key|apikey|jdbc:|dsn" ...` 已执行；命中项均为脱敏说明、CLI 参数名、`sanitizeSecretText`/`redactSecrets` 调用、fixture 安全 metadata、schema description 中的安全术语，或 `raw-secret`/`raw-token`/`fixture-secret` 等假测试值，并有断言验证不会出现在输出中；未发现真实凭据。
- 结构化自审：需求覆盖 API、CLI、CLI/MCP fixture、README/TODO 和 OpenSpec；只读边界通过 `safety.readOnly=true`、`writesProject=false`、无写入 service 调用和测试断言覆盖；复杂 schema 组合与 `$ref` 走 diagnostics / `REVIEW_REQUIRED`；现有字段命中走 `MERGE_EXISTING`；输出、错误、`sourcePath`、`sourceRef`、evidence 和文本摘要均走脱敏路径；实现未新增数据库表、迁移、外部网络或外部 LLM 调用。
- 子 agent 评审：`019f396f-af2e-7160-98bc-25a8aff0fb7c`（Maxwell），用途为 API/CLI/MCP/AI 可观察契约强制只读代码评审；发现 2 个 Critical、2 个 Important、1 个 Minor，均已修复并补回归测试或 schema 描述；已调用 `close_agent` 关闭，关闭结果返回 completed 状态。
- 未覆盖风险：未运行完整后端 `mvn test` 和前端 `pnpm test`/`pnpm build`；本次变更未触及前端，后端风险用 contractimport 定点 service/controller/fixture 测试覆盖，tools 风险用全量 `node --test tools/*.test.mjs` 覆盖。

## Archive Verification Evidence

- 2026-07-07：执行 `openspec archive add-contract-candidate-import --yes`，同步 `ai-contract-fixtures`、`cli-mcp-contract-fixtures`、`contract-candidate-import`、`dataspec-cli`、`standard-candidate-inbox` 主规格，并归档到 `openspec/changes/archive/2026-07-07-add-contract-candidate-import/`。
- 2026-07-07：补齐新建主规格 `openspec/specs/contract-candidate-import/spec.md` 的 Purpose，确认默认占位文本扫描无命中。
- 2026-07-07：`openspec validate --all` 通过，118 passed、0 failed。
- 2026-07-07：`node --test tools/dataspec-status-check.test.mjs tools/dataspec-verify-advisor.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs` 通过，44 pass、0 fail。
- 2026-07-07：`node tools/dataspec-status-check.mjs --format json` 返回 `status=warn`，active change warning 从 8 降至 2；第三条 next action 为 `当前问题编码：OPENSPEC_ACTIVE_CHANGE_PRESENT(count=2,severity=warning)`。
- 2026-07-07：`git diff --check` 退出码 0，仅输出 Windows LF/CRLF 提示。
- 2026-07-07：独立只读复评子 agent `019f3ac9-9655-7961-9767-c75277266b0a`（Noether）复评 staged archive diff，结论 Ready，无 Critical / Important / Minor findings；已调用 `close_agent` 关闭。
