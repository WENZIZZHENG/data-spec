# DataSpec SQL 修正 Prompt

## Prompt Metadata

- templateKey: fix-sql-prompt
- promptVersion: fix-sql-prompt@1
- scenario: FIX_SQL
- outputFormat: MARKDOWN_WITH_FIXED_SQL

## 原始 SQL

```sql
CREATE TABLE UserOrder (id bigint);
```

## Lint 统计

- errors: 1
- warnings: 0
- suggestions: 0

## Lint issues

```json
[]
```

## 字段目录 field-catalog.json

```json
{"fields":[]}
```

## 命名规则 rules.yaml

```yaml
naming: {}
```

## 输出要求

- 先列出每个问题的修正理由。
- 输出一份修正后的 SQL。
- 优先复用标准字段。
