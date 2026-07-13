## 1. 搜索分页契约

- [x] 1.1 为字段搜索增加可选 current/size、additive page 元数据和完整 Javadoc/OpenAPI 字段说明
- [x] 1.2 保留 limit-only 兼容模式，并实现稳定排序后的分页窗口、分页提示和越界页行为
- [x] 1.3 补充 Controller、Service 和 OpenAPI 契约测试，覆盖第二页、超过 50 条、非法页码和旧 limit 调用

## 2. 字段库服务端分页

- [x] 2.1 无筛选列表改用现有 pageFields，搜索条件改传 current/size，并删除浏览器 slice 分页
- [x] 2.2 增加 300ms 关键词防抖、过期响应保护、600ms 可访问慢状态和服务端总数展示
- [x] 2.3 将分组/数据域/语义规则改为低频刷新，将替代/合并全量候选改为按项目按需加载并缓存
- [x] 2.4 更新前端公共类型、字段说明、稳定选择器和现有请求契约测试

## 3. 浏览器与性能回归

- [x] 3.1 增加超过 50 条匹配字段的 Playwright 用例，验证页间可达、不重复和 current/size 请求参数
- [x] 3.2 浏览器用例验证连续输入只触发最终检索，并验证慢状态出现后可恢复
- [x] 3.3 运行浏览器截图检查桌面与移动视口，确认表格、状态行和分页不重叠

## 4. 验证与收口

- [x] 4.1 运行后端目标/全量测试、前端测试/build、Playwright、OpenAPI drift、tools 和 git diff 检查
- [x] 4.2 自查公共注释、字段说明、兼容边界、错误处理、无关改动和过度设计
- [x] 4.3 启动独立只读评审 agent，修复或记录全部 findings，并关闭 agent
- [x] 4.4 在本文件记录 Verification Evidence，运行 OpenSpec strict/all，完成后同步主规格并归档 change
- [x] 4.5 更新 TODO 完成归档与剩余时间，按 Git 门禁创建本地 commit

## Verification Evidence

- 日期：2026-07-14。
- 后端：在 `dataspec-server` 运行 `mvn test`，734 tests，0 failures / 0 errors / 0 skipped；评审修复后运行 `mvn "-Dtest=FieldServiceImplTest" test`，79 tests，0 failures / 0 errors。Maven 保留本机缓存 `jvnet-parent-3.pom` 的既有解析 warning，但命令 exit code 均为 0。
- 前端：在 Docker web 容器的 Node 22 / pnpm 11 环境运行 `pnpm test`，192 passed / 0 failed；运行 `pnpm build` 成功，保留依赖 pure annotation、chunk size 和 plugin timing 的既有非阻塞 warning。
- 浏览器：运行完整 Playwright，9 passed / 0 failed；其中字段库专项 6 条覆盖 65 条结果四页可达、慢状态、两类旧响应竞态、元数据刷新竞态和 375x812 移动布局。Browser 实测确认移动抽屉打开时关闭按钮获焦、主内容与 skip link 为 inert、backdrop 不可聚焦、关闭后焦点返回菜单按钮，页面无横向溢出且控制台无 error/warn。
- CLI / MCP / tools：运行 `node --test tools/*.test.mjs`，443 tests，441 passed / 2 skipped / 0 failed；2 个 skipped 均为 Windows 当前权限不支持 symlink 的既有平台条件。
- 契约与规格：Docker web 容器内以 `DATASPEC_API_DOCS_URL=http://server:8090/api-docs` 运行 `pnpm check:api`，确认 `src/api/schema.ts` 最新；`openspec validate paginate-field-library-search --strict` valid；同步前 `openspec validate --all` 为 137 passed / 0 failed。
- 通用检查：`git diff --check` 通过，仅有 Windows LF/CRLF 转换提示，无 whitespace error；Docker postgres、server、web 均为 healthy。
- 独立评审：第一轮只读 agent `019f5d46-998d-7020-be94-49379b2b3520` 发现请求序列、移动抽屉 inert/backdrop/焦点和 nullable page 共 5 类问题，均已修复并关闭。最终只读 agent `019f5d5e-f538-79a1-aa85-f6a7809ca02c` 发现末页错误翻页提示及公共契约文档缺口；主 agent 修复并补回归测试后，agent 最终结论为 `Approve`，已关闭释放线程位。
- 环境说明：本机 Node 24 的用户级 pnpm 供应链策略指向失效镜像并拒绝 npmjs lockfile tarball URL，因此前端权威验证按项目 Docker Node 22 环境执行；未修改 lockfile 或用户级配置。
- 规格与待办同步：delta spec 已幂等合并到 `openspec/specs/field-standard-search/spec.md`；`P6-86`、`P6-122`、`P6-145`、`P6-151` 已移入完成归档，剩余队列更新为 4 个近期主题和 2 个暂缓主题。运行 `validate_backlog.py --check-duplicate-titles` 验证 4 个 Markdown、133 个任务 ID 和 22 个相对链接通过。
- OpenSpec 归档：运行 `openspec archive paginate-field-library-search --skip-specs -y` 成功；因主规格已手动合并，使用 `--skip-specs` 避免重复追加。CLI 按 UTC 日期归档到 `openspec/changes/archive/2026-07-13-paginate-field-library-search/`，本地验证日期为 2026-07-14。
- 归档后复验：`openspec list` 返回无 active changes；`openspec validate --all` 为 136 passed / 0 failed；`node tools/dataspec-status-check.mjs --format json` 为 pass、0 errors / 0 warnings；`git diff --check` 通过。
- 未覆盖风险：未以真实万级字段库做浏览器基准；第一版通过服务端窗口和 65 条确定性回归消除固定 50 条截断，只有测量证明仍有局部渲染瓶颈时才考虑虚拟化。
