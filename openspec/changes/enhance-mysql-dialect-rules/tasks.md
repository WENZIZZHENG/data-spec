## 1. Parser 与规则增强

- [x] 1.1 扩展 `SqlParserService`，保留 MySQL `UNSIGNED` 类型修饰。
- [x] 1.2 扩展 `FieldSuffixTypeRule` 类型归一化，让 `tinyint(1)`/`tinyint` 匹配布尔规则，并忽略 `unsigned` 修饰对类型匹配的影响。

## 2. 测试覆盖

- [x] 2.1 扩展 `SqlParserServiceTest`，覆盖 MySQL `DECIMAL UNSIGNED`、`BIGINT UNSIGNED`、`ENGINE/CHARSET/COLLATE/KEY`。
- [x] 2.2 扩展 `LintRulesTest`，覆盖 `is_` + `tinyint(1)` 不误报。

## 3. 文档、验证与提交

- [x] 3.1 更新 README/TODO 中 P4-8 状态和能力说明。
- [x] 3.2 运行后端测试、OpenSpec validate 和 diff 检查。
- [x] 3.3 进行直接代码评审（不使用子 agent），修复发现的问题后提交本地 commit。
