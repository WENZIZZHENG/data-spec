# DataSpec 数标

**AI 编程时代的数据字段标准系统**

DataSpec 用于统一数据库字段命名、数据类型、注释、枚举、表模板和建表规范。提供 SQL 校验、数据字典生成、AI 规则导出等能力。

## 技术栈

| 层 | 技术 |
|---|------|
| 后端 | Java 21 + Spring Boot 3.3 + MyBatis-Plus + PostgreSQL |
| 前端 | Vue 3 + Vite + TypeScript + Element Plus + Pinia |
| SQL 解析 | JSqlParser |
| 接口文档 | SpringDoc OpenAPI |
| 测试 | JUnit 5 |

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

## AI Context 导出包

DataSpec 可为 AI 编程工具导出完整上下文包：

```bash
curl -L "http://localhost:8090/api/ai-context/package/download?projectId=1" -o dataspec-ai-context.zip
```

解压后包含 `.dataspec/DATABASE_RULES.md`、`.dataspec/field-catalog.json`、`.dataspec/field-catalog.schema.json`、`.dataspec/rules.yaml`、`.dataspec/prompts.md`、`.dataspec/examples/good.sql`、`.dataspec/examples/bad.sql` 和 `AGENTS.md.fragment`。可将这些文件复制到业务项目，让 Codex/Cursor/Claude Code 等 agent 在建表或评审 SQL 前读取字段标准和规则。

## 标准字段模型

标准字段支持 `aliases`、`category`、`codeSetId`、`sensitive`、`status`、`exampleValue` 等个人版元数据。AI 导出的 `field-catalog.json` 会把 `aliases` 转成数组，并输出敏感标记、字段状态、代码集关联和示例值，方便 AI 按业务语义复用标准字段。

## 结构化命名规则

`rules.yaml` 会导出结构化 `naming:` 模型，包含表/字段 snake_case、必含列、禁用字段名、推荐替换、字段后缀/前缀类型规则。SQL lint 已支持 `field_suffix_type`，默认校验 `_id/_at/_no/_count` 和 `is_` 对应的数据类型，并可通过 `RuleConfig.paramsJson` 的 `suffixTypes`、`prefixTypes` 覆盖。

## AI Prompt 生成

后端提供纯文本 Prompt 生成 API，不直接调用外部 LLM：

- `POST /api/ai-context/prompts/create-table`：传入 `projectId` 和 `businessDescription`，生成建表 Prompt。
- `POST /api/ai-context/prompts/fix-sql`：传入 `projectId` 和 `sql`，先运行 DataSpec lint，再生成 SQL 修正 Prompt。

## 字段推荐

后端提供确定性字段推荐 API，不调用外部 LLM：

```bash
curl "http://localhost:8090/api/fields/suggest?projectId=1&query=用户手机号&limit=5"
```

推荐结果会返回已有标准字段、匹配分数、命中原因、推荐字段名和 `existing` 标记。第一版按字段名、显示名、注释、别名、分类和标签匹配；未命中时生成一个 snake_case fallback 字段名。

## DDL 生成

后端提供基于表模板的 PostgreSQL DDL 生成 API，并在生成后复用 DataSpec lint 做自检：

```bash
curl "http://localhost:8090/api/generator/ddl/preview?projectId=1&templateId=1&tableName=user_order"
```

返回结果包含 `ddl` 和 `lintResult`。第一版只生成 `CREATE TABLE` 与 `COMMENT ON` 文本，不执行数据库变更，也不生成迁移计划。

## CLI

第一版 CLI 是 HTTP-backed wrapper，需要先启动 DataSpec 后端，默认连接 `http://localhost:8090`：

```bash
# 校验 SQL 文件，发现 ERROR 时退出码为 1，参数错误/网络错误退出码为 2
node tools/dataspec-cli.mjs lint examples/bad-example.sql --project 1 --format json

# 从 stdin 校验
cat examples/good-example.sql | node tools/dataspec-cli.mjs lint - --project 1 --format json

# 批量校验 SQL 文件或目录，发现任一 ERROR 时退出码为 1
node tools/dataspec-cli.mjs lint-files examples --project 1 --format json

# 导出 AI Context zip 包
node tools/dataspec-cli.mjs export-context --project 1 --output dataspec-ai-context.zip

# 推荐标准字段
node tools/dataspec-cli.mjs suggest-field "用户手机号" --project 1 --format json

# 基于表模板生成 DDL，并返回 lint 自检结果
node tools/dataspec-cli.mjs generate-ddl --project 1 --template 1 --table user_order --format json

# 指定后端地址
node tools/dataspec-cli.mjs lint examples/bad-example.sql --project 1 --format json --server http://localhost:8090
```

`lint-files` 会递归扫描传入目录下的 `.sql` 文件，并跳过 `.git`、`node_modules`、`dist`、`build`、`target` 等常见缓存/构建目录。输出 JSON 包含 `summary` 和 `files[]`，适合 CI 或 AI agent 读取。GitHub Actions 示例见 `.github/workflows/dataspec-sql-lint.yml.example`；复制到业务仓库后改名为 `.github/workflows/dataspec-sql-lint.yml` 并按实际方式启动 DataSpec 后端即可启用。

## MCP Server

第一版 MCP Server 同样是 HTTP-backed stdio adapter，需要先启动 DataSpec 后端，并在启动时指定默认项目：

```bash
node tools/dataspec-mcp.mjs --project 1 --server http://localhost:8090
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
pnpm build

# CLI 单元测试
cd ..
node --test tools/dataspec-cli.test.mjs

# MCP 单元测试
node --test tools/dataspec-mcp.test.mjs
```

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
│       ├── lint/             # SQL 校验引擎 + 规则实现
│       ├── generator/        # Markdown 数据字典与 DDL 生成
│       ├── aicontext/        # AI 规则导出
│       └── importexport/     # 导入导出
├── dataspec-web/             # Vue 3 前端
├── standards/                # 内置标准 YAML/JSON
├── docs/                     # 文档
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
| generator | /api/generator | Markdown 数据字典与 DDL 生成 |
| aicontext | /api/ai-context | AI 规则导出 |
| importexport | /api/import-export | 字段导入导出 |

## 规则引擎

规则引擎采用插件式设计，每条规则实现 `LintRule` 接口，Spring 自动发现和注册：

| 规则编码 | 说明 | 级别 |
|----------|------|------|
| table_naming_snake_case | 表命名必须 snake_case | ERROR |
| field_naming_snake_case | 字段命名必须 snake_case | ERROR |
| forbidden_field_name | 禁用字段名（uid、create_time 等） | ERROR |
| recommended_field_name | 推荐字段名（create_time → created_at） | SUGGESTION |
| required_columns | 业务表必含列（id、created_at、updated_at、is_deleted） | ERROR |
| amount_field_type | 金额字段应使用 bigint/numeric | WARNING |
| comment_missing | 字段/表注释缺失 | SUGGESTION |

`LintIssue` 除 `severity/ruleCode/message/tableName/columnName` 外，还会在可确定修复时输出 `suggestion`、`replacement`、`before`、`after`、`confidence`。这些字段会通过 API、CLI 和 MCP 原样返回，供 AI agent 生成修正 SQL 或修复说明。

## MVP 功能清单

- [x] 标准字段 CRUD
- [x] 数据域 CRUD
- [x] 枚举字典 CRUD
- [x] 表模板 CRUD
- [x] 规则配置 CRUD
- [x] SQL 粘贴校验（PostgreSQL CREATE TABLE）
- [x] 字段推荐 API/CLI/MCP
- [x] DDL 生成 API/CLI/MCP
- [x] 校验结果输出 error/warning/suggestion
- [x] SQL lint 结构化修复建议
- [x] 生成 Markdown 数据字典
- [x] 导出 DATABASE_RULES.md、field-catalog.json、rules.yaml
- [x] 导出 AI Context zip 包
- [x] 字段 JSON 导入导出
