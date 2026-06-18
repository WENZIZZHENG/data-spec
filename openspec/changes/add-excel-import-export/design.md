## 设计

### Excel 格式

第一版使用固定 Sheet 与英文表头，便于 AI、脚本和开发者稳定生成：

- `fields`：`name, displayName, dataType, length, precisionVal, scaleVal, nullable, defaultValue, comment, domainCode, tags, aliases, category, codeSetCode, sensitive, status, exampleValue`
- `enum_dicts`：`code, name, valueType, description`
- `enum_values`：`enumCode, value, label, sortOrder`

### 预览与导入

- 预览只解析和校验，不写数据库。
- 项目内已有字段名、代码集编码、枚举值视为更新；不存在视为新增。
- 同一 Excel 内重复字段名、重复代码集编码、重复 `enumCode + value` 视为冲突错误。
- `codeSetCode` 可以引用既有代码集，也可以引用本次 Excel `enum_dicts` 中定义的代码集。
- 导入确认先 upsert 代码集，再 upsert 枚举值，最后 upsert 字段，保证字段 `codeSetId` 能正确落库。

### 前端

导入导出页面复用当前项目 store。无当前项目时禁用项目相关操作；上传预览后展示统计和错误，只有预览有效时允许确认导入。
