## 1. 配置 Schema 与 Loader

- [x] 1.1 新增带完整字段 description、安全边界和 `x-` 扩展规则的 Draft 2020-12 config JSON Schema
- [x] 1.2 配置 loader additive 解析 `$schema` 与 `configVersion`，保留 legacy/未知字段兼容并补类型错误测试
- [x] 1.3 增加 schema 结构测试，校验版本、字段清单、嵌套字段、敏感标记和 editor association 常量一致

## 2. Init 与 Doctor

- [x] 2.1 `dataspec init` 生成 schema/config/README 三个管理文件，config 写入本地 `$schema` 和 `configVersion` 且不写凭据
- [x] 2.2 保持 schema/config/README 逐文件 skip/force 语义，并更新 init 成功、跳过和覆盖测试
- [x] 2.3 doctor 输出 secret-safe configSchema 摘要，覆盖 supported、legacy、missing、wrong association 和 future version 诊断
- [x] 2.4 更新 doctor text/json 与 init 输出测试，确认不新增网络调用或 fingerprint 协议

## 3. 文档与契约

- [x] 3.1 更新 README 的配置示例、编辑器关联、版本兼容、迁移方式和凭据边界
- [x] 3.2 同步 dataspec-local-config、dataspec-init、dataspec-doctor 主规格并检查公共字段说明

## 4. 验证与收口

- [x] 4.1 运行 config/CLI/MCP 定向测试、tools 全量测试、OpenSpec strict/all、状态和 diff/secrets 门禁
- [x] 4.2 启动独立只读评审子 agent，处理全部 findings 并关闭 agent
- [x] 4.3 记录 Verification Evidence，更新 TODO/完成归档，归档 change、执行归档后验证并创建本地 commit

## Verification Evidence

- 定向测试：`node --test tools/dataspec-config.test.mjs` 通过 `11/11`；config/doctor/init 定向回归通过；Node syntax check 通过。
- 全量验证：`node --test tools/*.test.mjs` 通过，`468 total / 466 pass / 2 platform skips`；两项 skip 均为 Windows 无 symlink 权限。
- Schema：Python `jsonschema` Draft 2020-12 实际校验通过，覆盖 canonical 配置、legacy projectId 字符串、未知官方字段拒绝和 `x-` 扩展允许。
- OpenSpec：`openspec validate add-dataspec-config-schema --strict` 通过；`openspec validate --all` 通过 `138/138`。
- 状态与格式：归档前 `node tools/dataspec-status-check.mjs --format json` 除当前 active change warning 外无错误；`git diff --check` 通过。
- 评审：独立只读 agent `019f5e4d-5640-7a23-9b90-7d663f989d77` 已完成并关闭；server userinfo、schemaRef 回显、future config 降级、legacy 状态和 projectId 契约 findings 均已修复。最终评审 agent `019f5e5d-91f8-73d3-b901-2c90ef48423a` 已完成并关闭，显式空 schema 元数据、schema userinfo、归档状态、普通文件判断和 export-context 回归 5 项 findings 均已整改；复评 agent `019f5e67-7b47-7900-9afb-4cadde770112` 已完成并关闭，结论为“无 findings”。
- 未覆盖风险：不校验用户本地修改后的 schema 内容或 hash，不开发 IDE 插件；这些均属于本 change 明确非目标。
- 归档后验证：`openspec validate --all` 通过 `137/137`；状态检查 `status=pass` 且零 issues；backlog validator 通过 `4` 个 Markdown 文件、`133` 个任务 ID 和 `22` 个相对链接；`git diff --check` 通过。
