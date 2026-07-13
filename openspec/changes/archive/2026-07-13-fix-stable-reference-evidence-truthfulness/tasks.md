## 1. OpenSpec 与基线

- [x] 1.1 建立 SDD full proposal、design 和 6 份 delta spec，明确兼容、安全与回滚边界
- [x] 1.2 运行 `openspec validate fix-stable-reference-evidence-truthfulness --strict`，确认实现前 artifacts 有效

## 2. 历史字段名与稳定引用

- [x] 2.1 先补 FieldHistoricalAliasService 失败测试，覆盖快照解析、当前值排除、去重、损坏快照和来源证据
- [x] 2.2 实现项目级字段变更日志批量查询与请求级历史别名索引
- [x] 2.3 先补 stable reference 失败测试，再接入历史名精确解析、歧义和 change-log evidence
- [x] 2.4 先补字段搜索/推荐失败测试，再接入低于当前名优先级的历史名评分、matchedAlias 和 ExplainTrace

## 3. Evidence claim 真实性

- [x] 3.1 先补 EvidenceClaimResolver 失败测试，覆盖 VERIFIED、MISSING、CROSS_PROJECT、UNVERIFIABLE 和脱敏边界
- [x] 3.2 实现 allowlist Evidence URI 解析和持久化来源项目归属校验
- [x] 3.3 先补 Evidence Package 失败测试，再为持久化来源输出 additive `source.evidenceRef`，payload-only 来源保持为空
- [x] 3.4 先补 AI output post-check 失败测试，再接入 resolver、稳定诊断码、严重度和 verified evidenceLinks

## 4. 契约与客户端同步

- [x] 4.1 更新 Schema Registry、OpenAPI 字段说明及 CLI/MCP 契约 fixture，保持已有字段兼容
- [x] 4.2 重新生成前端 OpenAPI 类型并运行 post-check 展示与前端契约测试
- [x] 4.3 验证 `review-pr` summary、inline、去重和 fallback 测试与修正后的主规格一致

## 5. 收口、评审与归档

- [x] 5.1 运行后端、前端、tools、OpenSpec all、状态检查、diff 与 secrets 扫描，记录 Verification Evidence
- [x] 5.2 启动独立代码评审子 agent，修复或记录全部 findings，并关闭 agent
- [x] 5.3 同步 TODO/估时/完成归档，归档 OpenSpec change 后重新运行 `openspec validate --all`
- [x] 5.4 精确暂存本任务文件并完成 commit 门禁，准备创建本地 commit，不 push

## Verification Evidence

- 评审：agent `019f5b7c-9dc6-71e1-8284-d513c6604a74` 多次等待后无完整输出，关闭时状态为 `running`；替代 agent `019f5b86-fba8-70d2-abc6-43a46012d942` 完成只读评审并已关闭。
- findings：替代评审未发现 Critical；句尾标点污染 Evidence URI 的 Important 已按 TDD 修复，Repository 项目隔离查询测试缺口的 Minor 已补契约测试；`mvn "-Dtest=AiOutputPostCheckServiceImplTest,StandardChangeLogRepositoryTest" test` 9/9 通过。
- 后端：`mvn test` 681/681 通过；评审修复目标命令 `mvn "-Dtest=AiOutputPostCheckServiceImplTest,StandardChangeLogRepositoryTest" test` 9/9 通过。
- 前端：Docker web 容器内 `pnpm test` 188/188 通过；`pnpm build` 通过，仅保留第三方 pure annotation、chunk size 和 plugin timing warning。
- tools：`node --test tools/*.test.mjs` 437 total，435 pass / 2 个当前平台 symlink skip。
- 契约：`openspec validate fix-stable-reference-evidence-truthfulness --strict` valid；归档前 `openspec validate --all` 136/136 通过；`pnpm check:api --source http://server:8090/api-docs` 确认 `schema.ts` 最新。
- Docker API：重启后的 server 为 healthy；带中文句尾标点的 `dataspec://evidence/sql-check/3。` 返回 `PASS`、`safeToUse=true` 和 canonical evidence link，缺失 `sql-check/999999999。` 返回 `WARN` 与 `MISSING_EVIDENCE_REFERENCE`。
- 通用门禁：`git diff --check` 通过，仅有 Windows LF/CRLF 提示；working diff 与新增文件敏感词扫描命中均为 schema 说明、测试假值或脱敏断言，未发现真实凭据；归档前状态检查唯一 warning 为当前 active change。
- 性能观察：5000 字段推荐基线约 7.1 秒，测试门禁通过；该既有大数据量风险由 `P6-86` 和 `P6-120` 承接，不扩大本次契约修复范围。
- 归档：主规格同步结果为新增 5 个 requirement、修改 1 个 requirement、无删除或重命名；change 已归档到 `openspec/changes/archive/2026-07-13-fix-stable-reference-evidence-truthfulness`。
- 归档后验证：`openspec validate --all` 135/135 通过；`node tools/dataspec-status-check.mjs --format json` 为 `status=pass`、0 issues、0 active changes。
