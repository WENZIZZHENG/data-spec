## 1. OpenSpec 与测试先行

- [x] 1.1 运行 `openspec validate add-db-metadata-scan-jobs --strict`，确认 proposal、design 和 delta specs 可校验。
- [x] 1.2 先补后端失败测试：scan job 字段、恢复 cursor、取消、限速 pageSize、failureSummary、evidence 脱敏和旧字段兼容。
- [x] 1.3 先补前端失败测试：反向导入采集作业进度、继续/取消动作、失败摘要脱敏和 partial result 选择边界。
- [x] 1.4 记录 TDD 红灯命令和失败点。

## 2. 后端采集作业契约

- [x] 2.1 扩展 `DatabaseMetadataScanReq`、`DatabaseMetadataScanResult` 和必要 model，补充 Javadoc/字段说明：`scanJobId`、`status`、`resumeCursor`、`cancelToken`、`rateLimit`、`retryPolicy`、`sourcePressureHint`、`partialResult`、`failureSummary`、`evidence`。
- [x] 2.2 扩展 `DatabaseReverseImportServiceImpl.scan()`，兼容现有 `scanId/cursor/pageSize/cancel`，并输出新 scan job 字段。
- [x] 2.3 实现 pageSize 服务端上限、source pressure hint、retry policy、取消语义和无凭据 resume/evidence。
- [x] 2.4 对单表 metadata 失败输出 bounded failure summary，成功表进入 partial result；不得写源数据库或标准字段库。
- [x] 2.5 更新 OpenAPI schema 生成或项目约定的前端类型同步文件。

## 3. 前端采集作业视图

- [x] 3.1 更新 `dataspec-web` 类型和 reverse import API wrapper 使用新增 scan job 字段。
- [x] 3.2 在反向导入页展示 scan job status、进度、page size、source pressure hint、failure summary、partial result 和 evidence 摘要。
- [x] 3.3 实现继续和取消动作，发送 `scanJobId/resumeCursor/cancelToken`，并保留用户已选表。
- [x] 3.4 确保预览/比对只使用 successful partial tables，不静默导入失败、取消或未扫描表。
- [x] 3.5 如覆盖率页复用采集作业结果，展示 partial coverage 边界和 next actions。

## 4. 验证、评审与收口

- [x] 4.1 运行后端验证：`mvn test` 或受影响测试类。
- [x] 4.2 运行前端验证：`pnpm test`，必要时 `pnpm build`。
- [x] 4.3 运行 OpenSpec 验证：`openspec validate add-db-metadata-scan-jobs --strict`。
- [x] 4.4 运行状态和通用检查：`node tools/dataspec-status-check.mjs --format json`、`git diff --check`。
- [x] 4.5 启动独立子 agent 做只读代码评审，记录 agent id、用途、结论和关闭状态；修复 Critical/Important findings 或说明技术理由。
- [x] 4.6 补充 `Verification Evidence`，记录关键命令、结果、评审证据和未覆盖风险。
- [x] 4.7 完成后按 OpenSpec 归档流程归档 change，运行 `openspec validate --all`，更新 `TODO.md` 中 P6-180 状态与验证证据。
- [x] 4.8 满足门禁后按项目 Git 规则创建本地 commit，不主动 push。

## Verification Evidence

- OpenSpec artifacts：`openspec validate add-db-metadata-scan-jobs --strict`，结果 `Change 'add-db-metadata-scan-jobs' is valid`。
- TDD 红灯（后端）：`mvn -Dtest=DatabaseReverseImportServiceTest test`（目录 `dataspec-server`），失败点为新增 scan job 模型 `DatabaseMetadataScanEvidence`、`DatabaseMetadataScanFailureSummary`、`DatabaseMetadataScanRateLimit` 尚不存在。
- TDD 红灯（前端 scan job 展示）：`pnpm test tests/databaseMetadataScan.test.ts`（目录 `dataspec-web`），失败点为 `databaseMetadataScan.ts` 尚未导出 `buildScanEvidenceSummary` 等新增采集作业展示工具。
- TDD 红灯（partial coverage 闭环）：`node --test tests/frontendSmoke.test.ts tests/scanPartialCoverage.test.ts`（目录 `dataspec-web`），失败点为 `ReverseImport.vue` 尚未保存 `scanPartialCoverage` payload，`FieldCoverage.vue` 尚未调用 `reportScanPartialCoverage`。
- TDD 红灯（评审修复）：`node --test tests/frontendSmoke.test.ts tests/databaseMetadataScan.test.ts`（目录 `dataspec-web`），失败点为 partial coverage 重试路径未走 `/coverage/scan-partial`、partial 边界缺少未扫描提示、`Authorization=` 未被前端二次脱敏。
- 后端目标验证：`mvn "-Dtest=FieldCoverageServiceImplTest,FieldCoverageControllerTest,DatabaseReverseImportServiceTest" test`（目录 `dataspec-server`），39 tests / 39 pass。
- 后端全量验证：`mvn test`（目录 `dataspec-server`），559 tests / 559 pass；保留本地 Maven 仓库 `javax.annotation-api` POM warning、ByteBuddy 动态 agent warning 和既有性能 baseline 日志。
- 前端目标验证：`node --test tests/frontendSmoke.test.ts tests/scanPartialCoverage.test.ts`，32 tests / 32 pass；`node --test tests/frontendSmoke.test.ts tests/databaseMetadataScan.test.ts`，39 tests / 39 pass。
- 前端全量验证：`pnpm test`（目录 `dataspec-web`），164 tests / 164 pass。
- 前端构建验证：`pnpm build`（目录 `dataspec-web`）通过；保留既有 `@vueuse/core` pure annotation、chunk size 和 plugin timing warnings。
- 状态检查：`node tools/dataspec-status-check.mjs --format json`，结果 `status=warn`，唯一 warning 为 active change `add-db-metadata-scan-jobs` 尚未归档。
- 通用检查：`git diff --check` 退出码 0；仅 Git 提示 LF/CRLF 转换。
- OpenSpec 归档：`openspec archive add-db-metadata-scan-jobs --yes`，同步主规格 `db-metadata-scan-jobs`、`db-metadata-scan-plan`、`db-reverse-import-frontend`、`field-coverage-report`，归档到 `openspec/changes/archive/2026-07-09-add-db-metadata-scan-jobs`。
- OpenSpec 全量验证：`openspec validate --all`，122 passed / 0 failed。
- 归档后状态检查：`node tools/dataspec-status-check.mjs --format json` 初次发现新主规格 Purpose 占位，已替换为稳定中文能力目的说明并复跑通过。
- 独立评审：agent `019f4667-53c9-7fd0-bc79-ee55c4d0689b` 用途为首轮复评，发现 coverage partial 前端未消费 API、tasks/evidence 未同步、`ScanPartialCoverageReq` 手写可选字段等问题；已修复阻塞项并关闭 agent。
- 独立评审：agent `019f4674-704e-7b00-8033-1c7f3fec490e` 用途为收口前只读代码评审；首轮发现 partial coverage 重试路径、未扫描边界提示、tasks/evidence 同步和 `Authorization=` 脱敏问题，复核后仅剩 tasks/evidence 和 minor 文案；均已处理并关闭 agent。
- 未覆盖风险：第一版不做跨服务重启持久作业、不做后台定时同步、不扫描业务数据行、不自动写标准字段库；REFRESH 仍可能在同一页内重复读取一次 metadata dump，未越过 pageSize 上限，作为后续低风险优化记录。
