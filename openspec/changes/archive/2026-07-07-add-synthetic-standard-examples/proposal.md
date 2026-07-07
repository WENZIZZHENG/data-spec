## Why

规则、字段推荐、DDL 生成和 Prompt 评测都依赖高质量样例。当前 good/bad SQL、DDL preview 输入和标准问答案例主要靠人工维护，覆盖用户、订单、支付、审计等典型场景时成本高，也难以及时跟随标准字段、模板、代码集和业务对象关系变化。

## What Changes

- 新增确定性的合成标准样例生成能力，按 `user`、`order`、`payment`、`audit` 等场景生成一组只读样例包。
- 样例包包含 good SQL、bad SQL、DDL preview 输入、字段推荐问题、标准问答案例、预期诊断、`specHash`、生成参数、安全 metadata 和下一步动作。
- 新增后端只读 API 和 CLI 命令，供 AI、CI、Prompt 评测和 fixture 更新流程复用同一稳定 JSON contract。
- 生成结果可作为后端 fixture、前端 smoke 或 Prompt 评测输入，也可转成人工审核后的标准使用样例草案；第一版不直接持久化样例。
- 不引入外部 LLM，不生成真实业务数据行，不写生产数据库，不替代人工维护的高价值真实样例。

## Capabilities

### New Capabilities
- `synthetic-standard-examples`: 覆盖合成标准样例生成包、场景集合、稳定输出字段、确定性 `specHash`、安全边界和验证接入。

### Modified Capabilities
- `dataspec-cli`: 新增合成样例生成命令、稳定 JSON/text 输出、退出码、安全 metadata 和 help 文本。
- `standard-usage-examples`: 明确合成样例可作为人工审核的使用样例草案来源，但生成流程本身不自动持久化。
- `core-golden-fixtures`: 新增合成样例 fixture 覆盖，确保生成的 good/bad SQL、DDL preview 输入和预期诊断可被本地测试验证。
- `ai-contract-fixtures`: 新增后端与 CLI 合成样例输出 contract fixture，防止 AI 消费字段漂移。
- `cli-mcp-contract-fixtures`: 新增 `synthetic-examples generate` fixture，覆盖输入边界、输出 shape、安全约束、示例和 recommended next actions。

## Impact

- 后端：新增只读生成 service/API、DTO/record、确定性 hashing 和单元/接口测试；不新增数据库表或迁移。
- CLI/tools：新增命令、fixture、Node 测试和契约校验；不需要运行后端即可校验 CLI/MCP contract fixture。
- 测试资源：新增 synthetic examples fixture/golden 断言，接入现有 Maven 和 Node 验证入口。
- 文档/OpenSpec：新增 change artifacts，更新 README/TODO，记录生成边界、命令、验证证据和不自动 archive 的状态。
- 安全边界：输出只包含结构化 SQL/DDL/Prompt 样例和脱敏 metadata，不包含真实业务数据行、token、password、Authorization、完整 JDBC URL 或 DSN。
