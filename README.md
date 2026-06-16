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
# 创建数据库（已包含建表脚本）
psql -U postgres -c "CREATE DATABASE dataspec;"
psql -U postgres -d dataspec -f dataspec-server/src/main/resources/db/schema.sql
```

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
│       ├── generator/        # Markdown 数据字典生成
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
| generator | /api/generator | Markdown 数据字典生成 |
| aicontext | /api/ai-context | AI 规则导出 |
| importexport | /api/import-export | 字段导入导出 |

## 规则引擎

规则引擎采用插件式设计，每条规则实现 `LintRule` 接口，Spring 自动发现和注册：

| 规则编码 | 说明 | 级别 |
|----------|------|------|
| field_naming_snake_case | 字段命名必须 snake_case | ERROR |
| forbidden_field_name | 禁用字段名（uid、create_time 等） | ERROR |
| recommended_field_name | 推荐字段名（create_time → created_at） | SUGGESTION |
| required_columns | 业务表必含列（id、created_at、updated_at、is_deleted） | ERROR |
| amount_field_type | 金额字段应使用 bigint/numeric | WARNING |
| comment_missing | 字段/表注释缺失 | SUGGESTION |

## MVP 功能清单

- [x] 标准字段 CRUD
- [x] 数据域 CRUD
- [x] 枚举字典 CRUD
- [x] 表模板 CRUD
- [x] 规则配置 CRUD
- [x] SQL 粘贴校验（PostgreSQL CREATE TABLE）
- [x] 校验结果输出 error/warning/suggestion
- [x] 生成 Markdown 数据字典
- [x] 导出 DATABASE_RULES.md、field-catalog.json、rules.yaml
- [x] 字段 JSON 导入导出
