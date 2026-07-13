## Context

字段搜索和推荐当前在 `FieldServiceImpl` 内维护 `tokens/compact/camelToSnake`，业务术语表在 `BusinessGlossaryServiceImpl` 内又维护一套 lowercase/substring 匹配。前者只能识别小写或数字到大写的普通 camel 边界，后者不识别 camel/acronym；两者都不会做中文词典最长匹配，也不能把相同缩写映射到不同 canonical 字段的情况表达为歧义。Standard Query 通过 FieldService 执行，因此继承上述行为；标准问答虽消费字段搜索结果，又在前端自行做 glossary substring 判断。

本变更是核心解析链路、多模块和 AI 可观察契约调整，按 SDD full 实施。约束是复用现有项目级 glossary、不改数据库 schema、不引入外部运行时或 LLM、保持所有 API additive compatibility，并避免在 5000 字段性能基线上继续放大重复解析。

## Goals / Non-Goals

**Goals:**

- 让分隔符、camelCase、连续 acronym、字母/数字边界和常见单位得到稳定、有序 token。
- 用当前项目启用 glossary 做确定性最长匹配、canonical 展开、disabled 标记和 abbreviation 歧义判断。
- 让字段搜索、推荐、Standard Query 和标准问答共享同一 query normalization 结果与 evidence。
- 输出有界、脱敏、可向后兼容的 token evidence，并用 golden fixtures 防止排序和解析静默漂移。

**Non-Goals:**

- 不做通用中文自然语言分词、拼音生成、模糊纠错、向量语义检索或外部 LLM 解释。
- 不自动新增、修改或采纳 glossary，不把未配置缩写猜成 canonical 词。
- 不在第一版统一字段冲突检测等非查询链路的全部历史硬编码词表。
- 不新增搜索 API、标准问答后端 API、数据库表或缓存基础设施。

## Decisions

### 1. 分离纯词法 tokenizer 与项目级 normalization service

新增无数据库依赖的 `NameLexicalTokenizer`，按 Unicode code point 和固定顺序执行：分隔符切分、`HTTPStatus` 类 acronym 边界、普通 camel 边界、字母/数字边界、lowercase 规范化和单位分类。tokenizer 只限制总输入长度，在该边界内保留完整内部 token；API/CLI/MCP evidence 再独立限长限量，避免截断后的 token 参与精确匹配。新增 `QueryNormalizationService` 接口与实现，负责 project access、调用 tokenizer、读取 glossary match，并组装 `QueryNormalizationResult` 与 `QueryTokenEvidence`。

`FieldServiceImpl` 在 search/suggest 每次请求中只解析一次 query，评分、fallback 和 evidence 均复用该结果；字段值侧的 token 化也复用纯 tokenizer。Standard Query 继续通过 FieldService，避免新增平行执行器。

备选方案是只增强 `BusinessGlossaryService.match`。它无法覆盖无 glossary 的 acronym/数字拆分和字段 fallback，也会保留 FieldService 私有 token 逻辑，因此不采用。另一个备选是直接引入 jieba/Python 或 LLM；它增加运行时、结果不稳定且违背当前本地确定性边界，因此不采用。

### 2. glossary 使用从左到右最长匹配与保守歧义规则

`BusinessGlossaryService.match` 复用 tokenizer，并按 term、synonym、root、abbreviation、disabled term 构建当前项目启用词典。中文和连续英文 token 从左到右选择最长可覆盖候选；长度相同时使用来源优先级 `TERM > SYNONYM > ROOT > ABBREVIATION`，但不能用优先级掩盖不同 canonical 字段之间的冲突。

同一 normalized token 的候选若最终指向同一个 canonical field，可折叠为 `RESOLVED`；若指向不同 canonical fields，则为 `AMBIGUOUS`，不选择 canonical；disabled term 为 `DISABLED`。abbreviation 必须完整匹配一个 lexical token，禁止 `amt` 在任意长字符串中 substring 命中；英文 root 可在单个 lexical token 内做兼容 substring 匹配，但内部 match 必须携带 compact query span，让 evidence 在原位置拆分并保留评分来源。没有项目词典来源的 token 保持 `UNRESOLVED`，不猜测拼音或缩写。

### 3. token evidence 使用独立 additive model，ExplainTrace 保持结构稳定

新增 `QueryTokenEvidence`，包含脱敏 token、normalized token、token kind、resolution status、可空 canonical term/field、glossary ids 和 reason。该列表 additive 地加入 Field Search summary、每个 Field Suggestion 和 Standard Query normalized；旧客户端忽略新字段即可。

服务端内部同时保留每条有界 evidence 与完整 `GlossaryMatch` 的精确关联，包括未截断 matched token、source type 和 span。Explain Trace 必须使用该内部关联选择来源，不得从已经脱敏或截断的公共 evidence 文本反推 term、root 或 abbreviation，避免同一 glossary 条目的长来源共享安全前缀时混淆规则代码。

匹配条目的既有 `ExplainTrace` 不增加字段，而是固定使用 `sourceType=QUERY_TOKEN` 或 `BUSINESS_GLOSSARY`，并使用 `NAME_SPLIT`、`GLOSSARY_LONGEST_MATCH`、`ABBREVIATION_EXPANSION`、`ABBREVIATION_AMBIGUOUS`、`DISABLED_TERM` 等 ruleCode。这样避免第三种证据结构，同时让 AI 能从 summary token 到具体字段匹配追踪来源。

### 4. 歧义与禁用词不参与高置信 canonical 评分

`RESOLVED` glossary token 可按现有 glossary 优先级参与字段评分；`AMBIGUOUS`、`DISABLED` 和 `UNRESOLVED` 不绑定 canonical field。字段搜索 summary hints、推荐 fallback reason 和 token evidence 明确要求人工确认；已有直接字段名、当前别名和历史名匹配仍保持其既有优先级。

标准问答继续复用字段搜索结果，但前端不再根据 glossary substring 自行提升 canonical 置信度；遇到 ambiguous/disabled token 时至少为 `NEEDS_CONFIRMATION`。

### 5. 单次项目查询、输出有界与脱敏

每次 search/suggest normalization 最多读取一次当前项目启用 glossary，并在内存中构建按长度和来源排序的请求级词典；第一版不缓存，避免 glossary 更新后的失效与跨项目污染。公开 token evidence 数量、单条 evidence 文本长度、candidate ids 和 reason 长度设置固定上限；达到数量上限时优先保留会参与评分或要求人工确认的 evidence。所有输出通过 `SensitiveDataSanitizer`，不得回显 secret-like raw query。

## Risks / Trade-offs

- [项目 glossary 很大时最长匹配增加 CPU] -> 请求级只构建一次排序词典并限制 query/token/evidence 数量；通过 5000 字段性能基线观察，再决定是否增加按项目版本失效的缓存。
- [短 abbreviation 误召回] -> abbreviation 只做完整 lexical token 匹配，不做任意 substring；一字符缩写默认不参与 canonical 展开。
- [中文未配置词典时仍是整段 token] -> 明确保持 `UNRESOLVED`，不伪装成通用分词；由 glossary 或后续候选管道补充业务知识。
- [多个 glossary entry 同词同 canonical] -> 折叠来源并保留多个 glossary ids；不同 canonical 才标记歧义。
- [新增字段影响旧客户端] -> 所有 `queryTokens` 可空且 additive，既有字段、状态、分数和错误码不删除不重命名。

## Migration Plan

1. 先加入 delta specs、词法与 glossary golden 失败测试，再实现 tokenizer/normalizer。
2. 接入 BusinessGlossary match 与 FieldService search/suggest，保持旧私有 helper 到行为验证完成后再删除重复部分。
3. 同步 Standard Query、标准问答、Schema Registry、OpenAPI 和 CLI/MCP fixture。
4. 不执行数据库迁移；回滚时可直接回退代码和 additive specs，现有 glossary 数据保持不变。

## Open Questions

无。第一版不内置 `ord/amt` 等业务缩写；验收样例通过项目 glossary fixture 证明可解析，并通过无词典场景证明不会猜测。
