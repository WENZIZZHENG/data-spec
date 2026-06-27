## 1. 测试与基线

- [x] 1.1 新增/扩展后端 SQL 定位测试，覆盖多表同名字段、schema 前缀、双引号/反引号/方括号标识符和不可定位 issue。
- [x] 1.2 新增 COMMENT ON TABLE/COLUMN 或缺注释定位测试，锁定 comment 相关规则的期望范围。
- [x] 1.3 扩展 CLI 测试，覆盖 `lint-files` JSON 保留行列范围、`review-pr` Markdown 展示 `行 x:y-x2:y2`。

## 2. 后端定位能力

- [x] 2.1 扩展 `LintIssue` 兼容模型，新增 `lineEnd`、`columnEnd`、`locationKind` 或等价定位范围字段。
- [x] 2.2 重构 `SqlIssueSourceSpanResolver` 为轻量 SQL 文本索引，优先在匹配表定义范围内定位列名。
- [x] 2.3 增强 resolver 对 schema 前缀、quoted/backtick/bracket 标识符、多语句 SQL 和 COMMENT ON 的支持。
- [x] 2.4 保持无法定位 issue 的定位字段为空，并确认检查记录 JSON 可兼容旧记录。

## 3. CLI 与前端契约

- [x] 3.1 更新前端类型/OpenAPI 生成产物或手写类型，兼容新增定位范围字段。
- [x] 3.2 更新 SQL 校验页位置展示，优先显示范围；仍支持点击跳转到起始行列。
- [x] 3.3 更新 CLI `review-pr` Markdown 位置展示；`lint-files` JSON 保持机器可读定位字段。

## 4. 文档、验证与提交

- [x] 4.1 更新 README/TODO，说明 P5-5 第一版交付定位数据和 PR inline 基础，不直接发 inline comment。
- [x] 4.2 运行后端测试、CLI node tests、前端测试/build、OpenSpec validate 和 diff 检查。
- [x] 4.3 进行直接代码评审（不使用子 agent），修复发现的问题。
- [x] 4.4 创建本地 commit，并准备进入下一个待办。
