## 1. OpenSpec 与范围确认

- [x] 1.1 读取 proposal/design/spec delta、P6-185/P6-176 候选详情和现有 synthetic examples、Schema Registry、CLI/MCP contract fixture 实现入口。
- [x] 1.2 运行 `openspec validate add-standard-test-data-compat-suite --strict`，确认 artifacts 可实施。

## 2. 后端标准测试数据包

- [x] 2.1 先补失败测试，覆盖字段级 valid/invalid/boundary case、枚举/格式/敏感字段、fallback source summary、deterministic specHash 和 secret redaction。
- [x] 2.2 新增带 Javadoc/Schema 字段说明的 test data package request/result/case/seed/mock/coverage/safety/diagnostic 模型。
- [x] 2.3 实现只读 `StandardTestDataPackageService`，复用字段、枚举、格式约束、语义和表模板 metadata，生成 bounded deterministic JSON/CSV/SQL seed/mock 草稿。
- [x] 2.4 新增只读 API endpoint，保持统一 `R`/DataSpecError 风格和项目归属校验，不写入数据库或业务文件。

## 3. 消费端兼容套件与本地 checker

- [x] 3.1 先补失败测试，覆盖 required adapter、stable field、breaking rule、additive-compatible、unsafe fixture redaction 和 exit code。
- [x] 3.2 新增 `consumer-compatibility-suite` fixture 与 checker，校验 Schema Registry、AI Context、CLI JSON、MCP descriptor/resource/tool、CLI/MCP fixtures 和 test data package golden payload。
- [x] 3.3 将 `consumer-compat check` 接入 CLI/tools 统一验证入口，输出 stable JSON、text summary、adapterResults、diagnostics 和 nextActions。

## 4. CLI、MCP 与契约 fixture

- [x] 4.1 先补 CLI/MCP 测试，覆盖 `test-data generate` JSON/text、server failure、argument bounds、DataSpecError redaction 和 `consumer-compat check` 本地执行。
- [x] 4.2 实现 CLI `test-data generate` 和 `consumer-compat check` 命令，复用现有 config/server/token/format/error 处理。
- [x] 4.3 实现 MCP `generate_test_data_package` 与 `check_consumer_compatibility` 工具或等价 resource，补 input schema description、structuredContent 和 read-only safety metadata。
- [x] 4.4 更新 `tools/fixtures/cli-mcp-contracts.json` 与 contract checker，覆盖新增 CLI/MCP 入口、示例输出和安全 metadata。

## 5. Schema Registry、前端最小 wiring 与文档

- [x] 5.1 扩展 Schema Registry，登记 test data package、compatibility suite、adapter result、breaking rule schema，并补 description 测试。
- [x] 5.2 按最小需要更新前端 API 类型 / smoke 测试或确认本轮无独立 UI 变更，避免未使用页面膨胀。
- [x] 5.3 同步 TODO/候选池/完成归档、README 或 AI contract 文档中受本变更直接影响的入口。

## 6. 验证、评审与收口

- [x] 6.1 运行受影响后端目标测试、tools tests、前端测试/build、OpenSpec strict/all、OpenAPI drift、`git diff --check` 和 secrets scan。
- [x] 6.2 启动独立子 agent 做 API/CLI/MCP/AI 契约、安全、逻辑和测试评审，修复或记录全部 findings，并关闭 agent。
- [x] 6.3 在本文件记录 Verification Evidence，包含关键命令、结果、评审 agent id、关闭状态、未覆盖风险和 active change 状态。
- [x] 6.4 按 Git 门禁精确 stage 本次变更并创建本地 commit，不自动 push、不自动 archive OpenSpec。

## Verification Evidence

- `node --test tools/dataspec-consumer-compat-check.test.mjs`：11 tests，11 pass；新增回归先 RED 后 GREEN，覆盖删除 `testDataCases.value` 与 `safety.readOnly` 时应返回 `STABLE_FIELD_MISSING`。
- `node tools/dataspec-consumer-compat-check.mjs --format json`：`ok=true`，`status=COMPATIBLE`，`diagnostics=0`；输出已覆盖 `diagnostics`、`testDataCases.value/reason/sourceRefs/requiresBusinessReview` 和 `safety.readOnly/writesProject/writesBusinessRepo/containsRealBusinessRows/externalNetworkUsed/externalLlmUsed`。
- `node tools/dataspec-cli-mcp-contract-check.mjs --format json`：`ok=true`，`diagnostics=0`。
- `node --test tools/dataspec-cli.test.mjs tools/dataspec-mcp.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs tools/dataspec-consumer-compat-check.test.mjs`：283 tests，281 pass，2 skipped。
- `D:\develop\maven\apache-maven-3.9.14\bin\mvn.cmd "-Dtest=SchemaRegistryServiceImplTest,SchemaRegistryControllerTest,StandardTestDataPackageServiceImplTest,StandardTestDataPackageControllerTest" test`：22 tests，0 failures，0 errors。
- `node --test tools/*.test.mjs`：432 tests，430 pass，2 skipped。
- `D:\develop\maven\apache-maven-3.9.14\bin\mvn.cmd test`（`dataspec-server`）：674 tests，0 failures，0 errors。
- `pnpm test`（`dataspec-web`）：185 tests，185 pass。
- `pnpm build`（`dataspec-web`）：通过；保留既有 Rolldown `INVALID_ANNOTATION` 与 chunk size warning。
- `npx.cmd openspec validate add-standard-test-data-compat-suite --strict`：valid。
- `npx.cmd openspec validate --all`：130 passed，0 failed。
- `node tools/dataspec-status-check.mjs --format json`：`status=warn`；仅 active OpenSpec change warning，符合本项目暂不自动 archive 约定。
- `node tools/dataspec-verify-advisor.mjs --changed --format json`：输出本次 touched files 的验证建议，已覆盖后端、前端、contract、CLI、docs、OpenSpec 和 diff check。
- `pnpm check:api`（`dataspec-web`）：未通过；`ECONNREFUSED localhost:8090/api-docs`，当前后端未启动。未为了 drift check 启动会连接本地数据库/Flyway 的后端，记录为环境未覆盖风险。
- `git diff --check`：通过；仅 Git LF/CRLF warning。
- secrets 扫描：评审与本地扫描均只命中文档安全规则、脱敏说明、占位符或负例测试；未发现真实 token/password/Authorization/JDBC/DSN/private key 泄漏。commit 前仍会对 staged diff 复扫。
- 独立评审子 agent `019f53e6-5f4f-7793-80d6-33f8d5543d0a`：`REQUEST_CHANGES`，已关闭；发现的 registry schema、nested drift 和文档字段说明问题均已修复。
- 独立复评子 agent `019f53f3-646e-73e2-aa15-1108335e75e9`：`REQUEST_CHANGES`，已关闭；发现的 `adapterResults` object array、nested schema path 和 DTO/schema 残余漂移均已修复。
- 独立复评子 agent `019f5404-5dbc-7c81-b99d-1fe9d87f4197`：`REQUEST_CHANGES`，已关闭；发现的 test data package stableFields 保护缺口已补齐并加回归测试。
- 独立复评子 agent `019f540e-4dbb-7da1-bf2e-431e05e2a88c`：`APPROVE`，已关闭；确认 docs、OpenSpec、fixture、checker 和 Schema Registry 口径一致，未发现真实 secret。
- Git 收口：本轮精确 stage 并创建本地 commit；不 push，不 archive OpenSpec，change 按项目约定暂保留 active。
