## 1. 规划与失败基线

- [x] 1.1 校验 proposal、design 与 delta specs 一致，并同步 TODO 中的 active change 与 P6-189 状态
- [x] 1.2 先补 `NameLexicalTokenizer` 失败测试，覆盖分隔符、camelCase、连续 acronym、字母/数字边界、单位分类、数量与长度上限
- [x] 1.3 先补 `QueryNormalizationService` 失败测试，覆盖中文最长匹配、精确缩写展开、同 canonical 折叠、跨 canonical 歧义、disabled term、未解析 token、单次 glossary 读取和敏感文本脱敏

## 2. 确定性命名解析核心

- [x] 2.1 新增带职责与字段语义说明的 tokenizer、normalization service、token/evidence/status/kind 模型，并用最小实现通过词法与归一化单测
- [x] 2.2 让 `BusinessGlossaryService` 复用共享 tokenizer 和项目启用词典，实现从左到右最长匹配、来源优先级、精确 abbreviation 与保守歧义语义
- [x] 2.3 限制 query、token、candidate ids 和 reason 输出规模，并通过 `SensitiveDataSanitizer` 保证 token evidence、诊断与日志不泄露 secret-like 原文

## 3. 字段搜索与推荐

- [x] 3.1 先补字段搜索/推荐失败测试，覆盖 `HTTPStatus2Code`、`会员手机号`、`ord_amt`、歧义/disabled fallback、直接字段名或别名优先和每请求只归一化一次
- [x] 3.2 在 `FieldServiceImpl` 权限检查之后、评分之前接入共享 normalization，复用同一结果完成搜索、推荐、fallback 和稳定 Explain Trace
- [x] 3.3 向 `FieldSearchSummary` 与 `FieldSuggestion` additive 地增加可选 `queryTokens`，补全 Javadoc、字段级 OpenAPI description 和旧构造调用兼容

## 4. Standard Query 与前端消费

- [x] 4.1 先补 Standard Query 失败测试，证明 FIELD text 与 legacy 搜索共享 token 顺序、glossary 状态和 canonical evidence，且 `explain=false` 保持兼容
- [x] 4.2 向 `StandardQueryNormalized` additive 地增加可选 `queryTokens`，沿用 FieldService 结果而不引入第二套解析器
- [x] 4.3 更新前端类型、字段搜索与标准问答测试和展示逻辑，消费后端 token evidence，并删除自行用 glossary substring 提升 canonical 置信度的判断

## 5. 契约与回归门禁

- [x] 5.1 同步 Schema Registry、OpenAPI 生成类型与 CLI/MCP contract fixtures，补齐 `QueryTokenEvidence` 各字段的业务语义、可空性、枚举和脱敏说明
- [x] 5.2 增加 deterministic golden fixtures 和推荐质量回归样例，接入项目既有后端、前端和 tools 验证入口，不新增第二套 harness
- [x] 5.3 运行受影响后端单测、前端单测/build、tools 契约测试、OpenSpec change strict 校验和 `git diff --check`，修复所有失败

## 6. 收口、评审与归档

- [x] 6.1 结构化自查需求覆盖、权限边界、评分优先级、错误处理、公共注释、字段说明、性能、脱敏、兼容性和无关改动
- [x] 6.2 启动独立代码评审子 agent，记录 agent id/用途，修复或说明全部 findings，并关闭 agent 释放线程位
- [x] 6.3 运行后端、前端、tools、OpenSpec all 和通用检查的接近全量验证，在 tasks.md 记录新鲜 `Verification Evidence`
- [x] 6.4 同步 TODO 与完成归档，归档 `add-deterministic-name-tokenization`，复验主规格并按安全 Git 门禁创建本地 commit；不 push

## Verification Evidence

- 日期：2026-07-14。
- 后端定向验证：在 `dataspec-server` 运行 `mvn "-Dtest=SensitiveDataSanitizerTest,NameLexicalTokenizerTest,QueryNormalizationServiceImplTest,BusinessGlossaryServiceImplTest,FieldServiceImplTest" test`，113 tests，0 failures / 0 errors；新增 supplementary code point、evidence 保留顺序、长 term/abbreviation 精确来源和 secret-safe 搜索评分用例均通过。
- 后端全量验证：在 `dataspec-server` 运行 `mvn test`，725 tests，0 failures / 0 errors / 0 skipped。Maven 保留本机缓存中 `jvnet-parent-3.pom` 的既有解析 warning，但命令 exit code 为 0，未影响编译和测试。
- CLI / MCP / tools：运行 `node --test tools/*.test.mjs`，438 tests，436 passed / 2 skipped / 0 failed；2 个 skipped 均为 Windows 当前权限不支持 symlink 的既有平台条件。
- 前端：Docker web 容器内运行 `pnpm test`，191 passed / 0 failed；运行 `pnpm build` 成功，保留依赖 pure annotation、chunk size 和 plugin timing 的既有非阻塞 warning。
- 契约：Docker web 容器内运行 `pnpm exec node scripts/check-openapi-schema.mjs --source http://server:8090/api-docs`，确认 `src/api/schema.ts` 已是最新；`openspec validate add-deterministic-name-tokenization --strict` valid；`openspec validate --all` 为 136 passed / 0 failed。
- 通用检查：`git diff --check` 通过，仅有 Windows LF/CRLF 转换提示，无 whitespace error；Docker postgres、server、web 服务均为 healthy。
- 规格与待办同步：6 个 delta capability 已幂等合并到主规格；同步后 `openspec validate --all` 为 137 passed / 0 failed。待办更新为 8 个剩余主题，运行 `validate_backlog.py --check-duplicate-titles` 验证 4 个 Markdown、133 个任务 ID 和 22 个相对链接通过。
- OpenSpec 归档：运行 `openspec archive add-deterministic-name-tokenization --skip-specs -y` 成功；因主规格已由 sync skill 合并，使用 `--skip-specs` 避免重复追加。CLI 按 UTC 日期归档到 `openspec/changes/archive/2026-07-13-add-deterministic-name-tokenization/`，本地验证日期仍为 2026-07-14。
- 归档后复验：`openspec list` 返回无 active changes；`openspec validate --all` 为 136 passed / 0 failed；`node tools/dataspec-status-check.mjs --format json` 为 pass、0 errors / 0 warnings，确认 TODO、archive 和主规格一致。
- 独立评审：初始只读评审 agent `019f5c3f-00c5-7570-abc8-5edc4b89986b` 用于 P6-189 全量代码评审，恢复后超时且已关闭；其 findings 涵盖 CLI/MCP 字段脱敏、ROOT/ABBREVIATION 边界、歧义保留、Explain Trace 关联、OpenAPI description、内部 token 截断、supplementary Han 和 ROOT span，均已修复并补回归测试。
- 最终只读评审 agent `019f5c71-0b3a-7831-8520-2ff3fbaa6c43` 用于最终复评和两轮 follow-up；其 findings 涵盖人工确认 evidence 优先级、长 glossary trace、Unicode 截断与 kind 分类、同条目长 term/abbreviation 来源混淆、supplementary 单字符 abbreviation/root 和 search/suggest 归一化一致性，均已由主 agent 修复。最终复评结论为“无 findings”，并确认内部 `QueryTokenResolution` 未进入公开响应；agent 已关闭释放线程位。
- 未覆盖风险：未调用外部 LLM 或真实业务数据；第一版仍依赖项目 glossary 提供业务缩写，不猜测未配置拼音或缩写。现有构建 warning 不属于本变更引入的失败。
