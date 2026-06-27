## 1. 后端快照读取与导出核心

- [x] 1.1 新增标准快照 payload DTO 和 repository/service 查询方法，支持按 `snapshotId`、version 在项目内只读加载并校验 hash。
- [x] 1.2 扩展 AI Context 导出服务，支持 `snapshotId` / `snapshotVersion` 参数生成历史 field catalog、rules.yaml 和 zip manifest。
- [x] 1.3 扩展 AI Context controller 查询参数，保持未传 snapshot 参数时的当前标准导出兼容行为。

## 2. SQL 记录回放详情

- [x] 2.1 新增 SQL 检查记录 replay DTO，返回 recordedStandard、currentStandard、status、summary 和 nextActions。
- [x] 2.2 扩展 `/api/lint/records/{id}` detail 响应，在保留 `record/issues` 的同时返回 replay metadata。
- [x] 2.3 为 versioned、currentChanged、unversioned 三类记录补后端单测。

## 3. CLI 与前端入口

- [x] 3.1 扩展 `dataspec-cli export-context` 支持 `--snapshot-id` / `--snapshot-version`，并补 Node 测试。
- [x] 3.2 重新生成或手动补充前端 OpenAPI 类型，更新 `api/aicontext.ts`、`api/lint.ts` 类型。
- [x] 3.3 前端 SQL 校验记录详情展示回放状态、历史 Context 导出提示和可复制命令。
- [x] 3.4 AI Context 页面支持选择当前标准或指定历史快照导出。

## 4. 文档、验证与收口

- [x] 4.1 更新 README/TODO，说明 P6-21 第一版能力和验证入口。
- [x] 4.2 运行 `mvn test`、`pnpm test`、`pnpm build`、CLI/MCP 测试、`npx openspec validate add-snapshot-replay-export` 和 `git diff --check`。
- [x] 4.3 进行直接代码评审，覆盖跨项目快照校验、历史 payload 兼容、前端空状态和 CLI 参数安全。
- [x] 4.4 通过验证后提交本功能改动。
