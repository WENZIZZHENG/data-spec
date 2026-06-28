# DataSpec 建表 Prompt

## Prompt Metadata

- templateKey: create-table-prompt
- promptVersion: create-table-prompt@1
- scenario: CREATE_TABLE
- outputFormat: POSTGRESQL_DDL_WITH_COMMENTS

## 业务需求

订单模块。

## 字段目录 field-catalog.json

```json
{"fields":[]}
```

## 命名规则 rules.yaml

```yaml
naming: {}
```

## 数据库规则 DATABASE_RULES.md

```markdown
规则摘要。
```

## 输出要求

- 优先复用 field-catalog.json 中已有标准字段。
- 输出 PostgreSQL CREATE TABLE，并补全 COMMENT ON TABLE / COMMENT ON COLUMN。
