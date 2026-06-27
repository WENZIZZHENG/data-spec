## 1. OpenSpec 与测试

- [x] 1.1 验证 `add-structured-naming-rules` OpenSpec artifacts 通过 `openspec validate`。
- [x] 1.2 新增字段后缀/前缀类型规则测试，并先观察缺失规则导致失败。
- [x] 1.3 扩展 AI rules.yaml 测试，并先观察缺失 `naming:` 导出导致失败。

## 2. 实现

- [x] 2.1 新增 `FieldSuffixTypeRule`，实现默认 `_id/_at/_no/_count/is_` 类型约束。
- [x] 2.2 支持 `paramsJson.suffixTypes` 和 `paramsJson.prefixTypes` 覆盖默认配置。
- [x] 2.3 扩展 `AiContextExportService.generateRulesYaml`，导出结构化 `naming:`。
- [x] 2.4 更新 README/TODO，说明 P0-5 第一版能力。

## 3. 验证

- [x] 3.1 运行后端测试、前端构建、OpenSpec validate 和 diff 空白检查。
- [x] 3.2 进行直接代码评审，检查规则默认值、paramsJson 覆盖、AI YAML 格式和兼容性。
- [x] 3.3 通过验证后提交本功能改动。
