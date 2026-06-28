## 1. OpenSpec

- [x] 1.1 创建 P6-34 proposal、design、spec 和 tasks。
- [x] 1.2 通过 OpenSpec change 校验。

## 2. 本地启动包

- [x] 2.1 新增 `docker-compose.local.yml`，覆盖 PostgreSQL、后端和前端服务。
- [x] 2.2 调整前端 Vite 代理支持环境变量覆盖容器内 API 目标。
- [x] 2.3 为 compose 增加端口覆盖、依赖缓存 volume 和本地开发默认环境变量。

## 3. Smoke 验证

- [x] 3.1 新增本地 smoke 脚本，等待 web/API、创建或复用演示项目并检查 dashboard/lint。
- [x] 3.2 支持 text/json 输出、timeout、server/web/token 参数和失败 next action。
- [x] 3.3 增加脚本级单测或源码级验证，覆盖参数解析、脱敏边界和输出结构。

## 4. 文档与收尾

- [x] 4.1 更新 README/TODO，说明一键启动、开发模式、smoke 和清理边界。
- [x] 4.2 执行前端/脚本/OpenSpec 验证和必要后端验证。
- [x] 4.3 完成结构化代码评审并修复 findings。
- [x] 4.4 创建本地 commit。
- [x] 4.5 归档 OpenSpec change 并再次验证。
