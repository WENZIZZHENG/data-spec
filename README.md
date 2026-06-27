# DataSpec 数标

**AI 编程时代的数据字段标准系统**

DataSpec 用于统一数据库字段命名、数据类型、注释、枚举、表模板和建表规范。当前已形成个人/小团队可用的字段标准工作台，并提供 SQL 校验、DDL 生成、数据字典、Excel 导入导出、AI Context、API Token 安全基线与管理页、CLI、MCP 和 GitHub PR Review 等能力。

## 技术栈

| 层 | 技术 |
|---|------|
| 后端 | Java 21 + Spring Boot 3.3 + MyBatis-Plus + PostgreSQL |
| 前端 | Vue 3 + Vite + TypeScript + Element Plus + Pinia |
| SQL 解析 | JSqlParser |
| 接口文档 | SpringDoc OpenAPI |
| 测试 | JUnit 5 |

## 功能概览

### 标准维护

- 项目空间管理，创建项目时可导入内置 standards，并支持一键创建演示项目。
- 标准字段库，支持别名、分类、代码集关联、敏感标记、状态和示例值。
- 数据域、枚举字典、表模板和规则配置管理。
- 标准快照，支持为当前项目字段、枚举和规则生成版本号、内容 hash 和可追溯 payload。
- Excel `.xlsx` 模板下载、字段/代码集导出、导入预览和确认导入。
- 标准变更日志，记录字段、代码集、规则的新增、修改、删除、启停 before/after 和操作者。

### SQL 规范闭环

- SQL 粘贴校验，返回 error/warning/suggestion 和结构化修复建议。
- 支持 PostgreSQL `COMMENT ON TABLE/COLUMN`，以及常见 MySQL `CREATE TABLE`、列内注释、表选项、索引定义、`UNSIGNED` 数值类型和 `tinyint(1)` 布尔习惯解析。
- 表名/字段名 snake_case、禁用字段名、推荐字段名、必备列、金额类型、字段后缀/前缀类型和注释缺失等规则。
- 修正 SQL 输出、复制、检查记录、分页历史和详情查看。
- SQL / 数据库直连反向导入预览，输出解析表、字段候选、缺注释项和非标准字段差异，并支持确认导入数标候选；直连模式支持按表生成数据库现状与 DataSpec 标准字段的二次比对，并追踪确认导入后的字段来源批次。

### 生成与报告

- 基于表模板生成 PostgreSQL DDL，并自动运行 lint 自检。
- Markdown/HTML 数据字典，包含概览统计、字段与数据域关系、枚举、模板、个人版字段元数据和 Mermaid 关系图。
- 个人工作台，展示字段数、代码集数、规则数、检查数、字段命中率、最近检查和问题趋势。

### AI 与自动化

- AI Context zip 导出，包含 `.dataspec/` 目录、字段目录 JSON Schema、规则、prompt、示例 SQL 和 `AGENTS.md.fragment`。
- AI Context manifest、字段目录和规则文件携带标准快照版本与 hash；未创建快照时标记为 `unversioned`。
- AI 建表 Prompt 和 SQL 修正 Prompt 生成。
- 字段推荐 API/CLI/MCP。
- DDL 生成 API/CLI/MCP。
- 轻量 API Token 管理页，支持创建、禁用、授权范围查看、最近使用时间和一次性明文复制。
- CLI 支持环境自检 `doctor`、单文件 lint、批量 `lint-files`、PR 评论式 `review-pr`、AI Context 导出、字段推荐和 DDL 生成。
- MCP Server 暴露 DataSpec resources、prompts 和核心 tools。
- GitHub Actions 示例支持 SQL 批量校验和 PR Review 评论。

## 快速启动

### 环境要求

- JDK 21+
- Maven 3.9+
- Node.js 18+ / pnpm 8+
- PostgreSQL 17+

### 1. 数据库

```bash
# 创建数据库，后端启动时会由 Flyway 自动执行迁移脚本
psql -U postgres -c "CREATE DATABASE dataspec;"
```

迁移脚本位于 `dataspec-server/src/main/resources/db/migration/`，后端启动会按版本顺序自动执行全部 `V*.sql`。
`dataspec-server/src/main/resources/db/schema.sql` 仅保留兼容说明，不再作为直接建表入口；如需手动初始化，请按版本号顺序执行迁移目录下的脚本。

### 2. 后端

```bash
cd dataspec-server
export JAVA_HOME=/path/to/jdk-21  # Windows: set JAVA_HOME=...
mvn spring-boot:run
```

后端启动在 http://localhost:8090，API 文档：http://localhost:8090/swagger-ui.html

### 3. 前端

```bash
cd dataspec-web
pnpm install
pnpm dev
```

前端启动在 http://localhost:5173

### 4. 首次体验

打开前端后进入“工作台”，点击“创建演示项目”即可生成 `DataSpec 演示项目`。演示项目会自动包含内置字段、核心规则、订单表模板和示例 SQL，可继续完成：

- “生成演示 DDL”：进入 DDL 生成页并预填 `user_order`。
- “校验示例 SQL”：进入 SQL 校验页并载入可修正的示例 SQL。
- “导出 AI Context”：预览并下载 `dataspec-ai-context.zip`。

## AI Context 导出包

DataSpec 可为 AI 编程工具导出完整上下文包：

```bash
curl -L "http://localhost:8090/api/ai-context/package/download?projectId=1" -o dataspec-ai-context.zip
```

解压后包含 `.dataspec/DATABASE_RULES.md`、`.dataspec/field-catalog.json`、`.dataspec/field-catalog.schema.json`、`.dataspec/rules.yaml`、`.dataspec/prompts.md`、`.dataspec/examples/good.sql`、`.dataspec/examples/bad.sql` 和 `AGENTS.md.fragment`。可将这些文件复制到业务项目，让 Codex/Cursor/Claude Code 等 agent 在建表或评审 SQL 前读取字段标准和规则。

导出包的 `.dataspec/manifest.json`、`.dataspec/field-catalog.json` 和 `.dataspec/rules.yaml` 会包含 `specVersion` / `specHash` 元数据。若项目尚未创建标准快照，版本显示为 `unversioned`，不阻断导出；创建快照后，后续 SQL 检查记录和 DDL 生成结果会记录当前快照 ID、版本和 hash。

## 标准快照

前端“系统设置 / 标准快照”提供项目级快照入口。创建快照时，DataSpec 会把当前项目字段、枚举和规则生成确定性 JSON payload，并计算 SHA-256 hash；同一份标准内容会得到稳定 hash，方便 AI Context、SQL 检查记录和 DDL 生成结果追溯当时使用的标准版本。

后端 API：

```bash
curl -X POST "http://localhost:8090/api/projects/1/standard-snapshots" \
  -H "Content-Type: application/json" \
  -d '{"version":"v2026.06.24","name":"AI Context 可复现基线"}'
```

第一版不做审批、发布流或按历史快照完整回放；快照 payload 已保存，为后续复现和回放能力打基础。

## 标准字段模型

标准字段支持 `aliases`、`category`、`codeSetId`、`sensitive`、`status`、`exampleValue` 等个人版元数据。AI 导出的 `field-catalog.json` 会把 `aliases` 转成数组，并输出敏感标记、字段状态、代码集关联和示例值，方便 AI 按业务语义复用标准字段。

## 结构化命名规则

`rules.yaml` 会导出结构化 `naming:` 模型，包含表/字段 snake_case、必含列、禁用字段名、推荐替换、字段后缀/前缀类型规则。SQL lint 已支持 `field_suffix_type`，默认校验 `_id/_at/_no/_count` 和 `is_` 对应的数据类型；前端规则配置页已为必含列、禁用字段名、推荐替换、后缀/前缀类型提供结构化表单，并保留 JSON 预览和兜底编辑。

## AI Prompt 生成

后端提供纯文本 Prompt 生成 API，不直接调用外部 LLM：

- `POST /api/ai-context/prompts/create-table`：传入 `projectId` 和 `businessDescription`，生成建表 Prompt。
- `POST /api/ai-context/prompts/fix-sql`：传入 `projectId` 和 `sql`，先运行 DataSpec lint，再生成 SQL 修正 Prompt。

## 字段推荐

后端提供确定性字段推荐 API，不调用外部 LLM：

```bash
curl "http://localhost:8090/api/fields/suggest?projectId=1&query=用户手机号&limit=5"
```

推荐结果会返回已有标准字段、匹配分数、命中原因、推荐字段名和 `existing` 标记。当前推荐逻辑按字段名、显示名、注释、别名、分类、标签和内置语义词库匹配，覆盖 `uid/user_id`、`phone/mobile/tel/mobile_no`、`amount/price/fee/amount_cent`、`sfzh/id_card_no` 等常见叫法；泛化词会降权，敏感字段会在命中原因里提示。未命中已有字段但命中已知语义组时，会优先生成 canonical snake_case fallback 字段名。

## DDL 生成

后端提供基于表模板的 PostgreSQL DDL 生成 API，并在生成后复用 DataSpec lint 做自检：

```bash
curl "http://localhost:8090/api/generator/ddl/preview?projectId=1&templateId=1&tableName=user_order"
```

返回结果包含 `ddl` 和 `lintResult`。第一版只生成 `CREATE TABLE` 与 `COMMENT ON` 文本，不执行数据库变更，也不生成迁移计划。

## SQL 校验记录与反向导入

`/api/lint` 会返回 lint 结果、结构化修复建议和 `fixedSql`，并保存 SQL 检查记录。前端 SQL 校验页支持查看修正 SQL、复制、最近检查记录分页和详情。

反向导入页支持粘贴 SQL DDL，也支持 PostgreSQL/MySQL 数据库直连：填写连接信息后可测试连接、加载表、筛选并选择表、生成 metadata 预览、勾选字段候选并确认导入到当前项目字段库。直连模式还可以生成只读二次比对，按表展示已匹配、属性变化、新增、缺注释和非标准字段，并支持按状态筛选。页面会按项目在浏览器本地记住数据库类型、host、port、database、schema、username、表选择、搜索词和差异筛选，不保存数据库密码、token 或完整连接串。确认导入创建的新字段会记录导入批次、来源 schema/table/column 和原始 metadata 快照，字段库可查看来源摘要；从导入结果跳转字段库时会自动携带字段关键词并筛选当前页结果。直连模式不会修改源数据库，也不会做定时同步。

## 数据字典与导入导出

- Markdown 数据字典会输出项目概览、字段库、数据域、枚举字典、表模板和模板字段约束。
- HTML 数据字典支持浏览器离线打开；Mermaid ERD/关系图可展示字段、数据域、代码集和模板之间的关系。
- Excel 导入导出支持 `.xlsx` 模板下载、字段/代码集导出、导入预览、新增/更新/冲突统计、行级 dry-run 明细、字段级 before/after diff 和确认导入。

## 工作台与变更记录

个人工作台提供项目级摘要：标准字段数、代码集数、规则数、禁用词数、SQL 检查数、字段命中率、最近检查和问题趋势。

标准变更日志会记录字段、代码集/枚举值、规则配置的创建、更新、删除、启停操作，保留 before/after JSON 和操作者便于追溯。

## 安全基线

个人本地开发默认不强制登录。小团队或 AI agent 长期接入时，可开启轻量 API Token 模式：

```yaml
dataspec:
  security:
    enabled: true
```

后端只保存 token 的 SHA-256 hash。安全模式关闭时可先在前端“系统设置 / API Token”创建首个 token，再开启安全模式；如果已经开启但没有全项目 token，可继续用 SQL bootstrap 写入一条全项目 token。示例：

```powershell
$token = "ds_your_token"
$hash = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($token))).ToLower()
```

```sql
INSERT INTO ds_api_token(name, token_hash, operator_name, project_ids)
VALUES ('default-cli', '<hash>', 'alice', '1,2');
```

`project_ids` 使用逗号分隔项目 ID，`*` 表示全部项目。安全模式开启后，前端可在顶部 `API Token` 输入 token；CLI/MCP 可通过 `--dataspec-token`、`DATASPEC_TOKEN` 环境变量或 `.dataspec/config.json` 的 `apiToken` 传递 token。团队共享的 `.dataspec/config.json` 不建议提交明文 token，优先使用环境变量或本地忽略文件。

前端“系统设置 / API Token”提供日常管理入口：列表只展示 token 名称、操作者、项目范围、启停状态、创建时间、停用时间和最近使用时间，不返回 token hash；新建 token 时明文只显示一次，请立即复制保存。停用 token 后，后续 CLI/MCP/API 请求会被拒绝。

## CLI

第一版 CLI 是 HTTP-backed wrapper，需要先启动 DataSpec 后端，默认连接 `http://localhost:8090`。在业务仓库中可放置 `.dataspec/config.json`，CLI 会从当前目录向上查找该文件；显式命令行参数优先于配置文件：

```json
{
  "projectId": 1,
  "server": "http://localhost:8090",
  "apiToken": "ds_optional_local_token",
  "defaultPaths": ["db/migrations", "sql"]
}
```

```bash
# 校验 SQL 文件，发现 ERROR 时退出码为 1，参数错误/网络错误退出码为 2
node tools/dataspec-cli.mjs lint examples/bad-example.sql --project 1 --format json

# 在含 .dataspec/config.json 的业务仓库中，可省略 --project 和 --server
node tools/dataspec-cli.mjs lint examples/bad-example.sql --format json

# 安全模式开启后传递 DataSpec API token
node tools/dataspec-cli.mjs lint examples/bad-example.sql --project 1 --format json --dataspec-token "$DATASPEC_TOKEN"

# 从 stdin 校验
cat examples/good-example.sql | node tools/dataspec-cli.mjs lint - --project 1 --format json

# 批量校验 SQL 文件或目录，发现任一 ERROR 时退出码为 1
node tools/dataspec-cli.mjs lint-files examples --project 1 --format json

# 未传路径时，lint-files 会使用 config.json 的 defaultPaths
node tools/dataspec-cli.mjs lint-files --format json

# GitHub Actions 中发布或更新 PR Review 评论；有 ERROR 时评论后退出码为 1
node tools/dataspec-cli.mjs review-pr . --project 1 --repo owner/repo --pr 123 --token "$GITHUB_TOKEN" --server http://localhost:8090

# 导出 AI Context zip 包
node tools/dataspec-cli.mjs export-context --project 1 --output dataspec-ai-context.zip

# 推荐标准字段
node tools/dataspec-cli.mjs suggest-field "用户手机号" --project 1 --format json

# 基于表模板生成 DDL，并返回 lint 自检结果
node tools/dataspec-cli.mjs generate-ddl --project 1 --template 1 --table user_order --format json

# 自检本地 DataSpec CLI 环境；存在失败检查时退出码为 1，参数错误退出码为 2
node tools/dataspec-cli.mjs doctor --project 1

# 输出 AI/CI 可解析的 JSON 自检结果
node tools/dataspec-cli.mjs doctor --format json

# 执行完整 OpenAPI schema 漂移检查
node tools/dataspec-cli.mjs doctor --check-openapi --format json

# 指定后端地址
node tools/dataspec-cli.mjs lint examples/bad-example.sql --project 1 --format json --server http://localhost:8090
```

`doctor` 会检查配置文件、DataSpec 服务、API token 身份、项目可访问性、`defaultPaths` 和 OpenAPI 状态；默认只做轻量 OpenAPI 检查，传 `--check-openapi` 时会复用前端契约校验逻辑做完整 schema 漂移检查。`lint-files` 会递归扫描传入目录下的 `.sql` 文件，并跳过 `.git`、`node_modules`、`dist`、`build`、`target` 等常见缓存/构建目录。输出 JSON 包含 `summary` 和 `files[]`，适合 CI 或 AI agent 读取。`review-pr` 会在批量 lint 后创建或更新包含 `<!-- dataspec-sql-review -->` marker 的 PR 评论，避免重复刷屏；问题明细会展示文件内行列范围，作为后续 GitHub inline comment 的基础数据，但当前仍只发布单条汇总评论；评论成功后仍会按 ERROR 情况返回 0 或 1。GitHub Actions 示例见 `.github/workflows/dataspec-sql-lint.yml.example`；复制到业务仓库后改名为 `.github/workflows/dataspec-sql-lint.yml` 并按实际方式启动 DataSpec 后端即可启用。

## MCP Server

第一版 MCP Server 同样是 HTTP-backed stdio adapter，需要先启动 DataSpec 后端。启动时可显式指定默认项目，也可在业务仓库中依赖 `.dataspec/config.json`：

```bash
node tools/dataspec-mcp.mjs --project 1 --server http://localhost:8090 --dataspec-token "$DATASPEC_TOKEN"

# 在含 .dataspec/config.json 的业务仓库中
node tools/dataspec-mcp.mjs
```

可在 MCP client 中按本地 stdio server 配置。当前暴露能力：

- resources：`field-catalog`、`database-rules`、`rules-yaml`，URI 形如 `dataspec://project/1/field-catalog`。
- prompts：`dataspec_create_table`、`dataspec_review_sql`、`dataspec_design_fields`。
- tools：`lint_sql`、`get_field_catalog`、`suggest_fields`、`generate_table_ddl`；`lint_sql` 返回结构化 lint 结果，SQL 存在 ERROR 时仍视为工具调用成功。

## 验证

```bash
# 后端单元测试
cd dataspec-server
mvn test

# 前端类型检查与生产构建
cd ../dataspec-web
pnpm test
pnpm build

# OpenAPI 类型契约防漂移；默认读取 http://localhost:8090/api-docs
pnpm check:api

# CI 或离线场景可指定 OpenAPI JSON/YAML 文件
pnpm check:api -- --source ./api-docs.json

# CLI/MCP 单元测试
cd ..
node --test tools/dataspec-config.test.mjs tools/dataspec-cli.test.mjs tools/dataspec-mcp.test.mjs
```

后端 `mvn test` 已包含核心 fixture/golden 回归测试，覆盖 PostgreSQL/MySQL SQL 样例、fixedSql golden 输出和反向导入 metadata 预览摘要。

## 项目结构

```
data-spec/
├── dataspec-server/          # Spring Boot 后端
│   └── src/main/java/com/dataspec/
│       ├── common/           # 通用：响应封装、异常处理、配置
│       ├── project/          # 项目空间
│       ├── field/            # 标准字段库
│       ├── domain/           # 数据域
│       ├── enumdict/         # 枚举字典
│       ├── template/         # 表模板
│       ├── rule/             # 规则配置
│       ├── standards/        # 内置标准初始化
│       ├── lint/             # SQL 校验引擎 + 规则实现
│       ├── aicontext/        # AI 规则导出
│       ├── dashboard/        # 个人工作台
│       ├── generator/        # Markdown 数据字典与 DDL 生成
│       ├── importexport/     # 导入导出
│       ├── changelog/        # 标准变更日志
│       ├── standard/         # 标准版本快照
│       ├── reverseimport/    # SQL 反向导入
│       └── security/         # API Token 认证与管理
├── dataspec-web/             # Vue 3 前端
├── standards/                # 内置标准 YAML/JSON
├── tools/                    # CLI 与 MCP adapter
├── openspec/                 # OpenSpec 变更记录
└── examples/                 # 示例 SQL
```

## 后端模块

每个业务模块遵循 **Entity → Mapper → Repository → Service → Controller** 五层架构：

| 模块 | 路径 | 说明 |
|------|------|------|
| project | /api/projects | 项目空间管理 |
| field | /api/fields | 标准字段库 CRUD |
| domain | /api/domains | 数据域管理 |
| enumdict | /api/enums | 枚举字典 + 枚举值 |
| template | /api/templates | 表模板 + 模板字段 |
| rule | /api/rules | 规则配置管理 |
| lint | /api/lint | SQL 粘贴校验 |
| dashboard | /api/dashboard | 个人工作台统计 |
| auth | /api/auth | API token 当前身份 |
| tokens | /api/tokens | API token 创建、列表与停用 |
| generator | /api/generator | Markdown 数据字典与 DDL 生成 |
| aicontext | /api/ai-context | AI 规则导出 |
| importexport | /api/import-export | 字段导入导出 |
| changelog | /api/change-logs | 标准变更日志 |
| standard-snapshots | /api/projects/{projectId}/standard-snapshots | 标准版本快照 |
| reverse-import | /api/reverse-import | SQL 与数据库直连反向导入 |

## 规则引擎

规则引擎采用插件式设计，每条规则实现 `LintRule` 接口，Spring 自动发现和注册：

| 规则编码 | 说明 | 级别 |
|----------|------|------|
| table_naming_snake_case | 表命名必须 snake_case | ERROR |
| field_naming_snake_case | 字段命名必须 snake_case | ERROR |
| forbidden_field_name | 禁用字段名（uid、create_time 等） | ERROR |
| recommended_field_name | 推荐字段名（create_time → created_at） | SUGGESTION |
| required_columns | 业务表必含列（id、created_at、updated_at、is_deleted） | ERROR |
| field_suffix_type | 字段后缀/前缀应匹配推荐类型 | WARNING |
| amount_field_type | 金额字段应使用 bigint/numeric | WARNING |
| comment_missing | 字段/表注释缺失 | SUGGESTION |

`LintIssue` 除 `severity/ruleCode/message/tableName/columnName` 外，还会输出可定位时的 `line/column/lineEnd/columnEnd/sourceStart/sourceEnd/locationKind`，并在可确定修复时输出 `suggestion`、`replacement`、`before`、`after`、`confidence`。这些字段会通过 API、CLI 和 MCP 原样返回，供 AI agent 生成修正 SQL、修复说明或文件级 review 定位。

## 已完成功能清单

- [x] 项目空间、顶部当前项目联动和内置 standards 初始化
- [x] 演示项目与首次使用入口，串联 DDL 生成、SQL 校验和 AI Context 导出
- [x] 标准字段、数据域、枚举字典、表模板、规则配置 CRUD 与常见规则结构化参数表单
- [x] 标准快照、内容 hash、AI Context 版本标识、SQL 检查记录和 DDL 生成结果快照引用
- [x] 个人版字段模型：别名、分类、代码集、敏感标记、状态、示例值
- [x] 结构化命名规则导出和 `field_suffix_type` lint 规则
- [x] SQL 粘贴校验、结构化 issue、修复建议和 `fixedSql`
- [x] SQL 检查记录、最近记录分页和详情
- [x] SQL issue source range，支持表/字段/COMMENT 定位、前端跳转和 PR 汇总评论行列范围展示
- [x] PostgreSQL `COMMENT ON` 解析和常见 MySQL `CREATE TABLE` / `UNSIGNED` / 表选项解析
- [x] 字段推荐 API/CLI/MCP
- [x] DDL 生成 API/CLI/MCP 和前端预览下载
- [x] AI Context zip 导出和业务项目 `.dataspec/` 约定
- [x] AI 建表 Prompt 和 SQL 修正 Prompt 生成
- [x] Markdown/HTML 数据字典增强和 Mermaid ERD 输出
- [x] Excel `.xlsx` 字段/代码集导入导出与 dry-run 明细预览
- [x] 标准变更日志和操作者记录
- [x] 个人/小团队 API Token 安全基线、管理页面、项目边界和 CLI/MCP token 透传
- [x] 个人工作台和字段命中率报告
- [x] SQL 反向导入预览与差异分析
- [x] 数据库直连反向导入与前端确认导入流程
- [x] 数据库直连二次比对，按表展示标准命中、属性变化、新增、缺注释和非标准字段
- [x] 数据库直连导入来源与批次追踪，字段库可查看来源摘要
- [x] 前端反向导入高频流程记忆，按项目恢复非敏感连接信息、表选择、筛选状态和字段库关键词跳转
- [x] DataSpec CLI：`doctor`、`lint`、`lint-files`、`review-pr`、`export-context`、`suggest-field`、`generate-ddl`，支持 `.dataspec/config.json` 默认项目配置
- [x] DataSpec MCP Server：resources、prompts、`lint_sql`、`get_field_catalog`、`suggest_fields`、`generate_table_ddl`，支持 `.dataspec/config.json` 默认项目配置
- [x] GitHub Actions 示例和 PR 评论式 SQL Review

## 暂缓探索

- 多方言完整规则体系：当前已覆盖 PostgreSQL 和常见 MySQL DDL 解析，SQL Server 等其他方言后续按实际场景补充。
- 审批流、发布流和复杂 RBAC：当前定位个人/小团队优先，只保留轻量 API Token 与项目边界。
