# 直播辩论 Mock 演示系统

本项目基于给定的前端项目和网关项目，补充实现了一个 Spring Boot Mock 后端服务，用于模拟直播辩论、投票、评委、AI 内容、用户管理等主要业务流程。项目目标是让前端页面可以完整演示，不依赖真实数据库、真实微信登录或真实直播推流服务。

## 演示地址

本地演示地址：

| 服务 | 地址 | 说明 |
| --- | --- | --- |
| 后台管理端 | `http://localhost:8081/admin` | 管理直播流、投票、评委、AI 内容、辩论流程 |
| 用户 H5 端 | `http://localhost:8082/#/` | uni-app H5 用户观看和投票页面，端口以终端输出为准 |
| 网关 API | `http://localhost:8080/api` | 前端统一访问入口 |
| 后端 API | `http://localhost:3001` | Spring Boot Mock 后端 |

公网演示地址：

| 类型 | 地址 |
| --- | --- |
| 前端访问地址 | 部署后替换为公网地址 |
| 后端 API 地址 | 部署后替换为公网地址 |

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 前端 | uni-app、Vue 2、后台管理静态页面 |
| 网关 | Node.js、Express、WebSocket |
| 后端 | Java 21、Spring Boot 4、Spring WebMVC |
| Mock 数据 | 后端内存 Map/List 模拟数据，包含直播流、票数、用户、评委、AI 内容 |
| 部署建议 | 同一台服务器运行前端、网关、后端；也可分别部署到云平台 |

## 项目结构

```text
live-debate-demo/
├── frontend/         # 给定前端项目，已补充 H5 运行配置和后台联调配置
├── gateway/          # 给定网关项目，负责 /api 转发和 WebSocket 广播
├── backend/          # Java Spring Boot Mock 后端
└── README.md         # 项目说明
```

后端核心文件：

```text
backend/src/main/java/com/stger/livebackend/
├── BackendApplication.java
└── MockApiController.java
```

## 本地运行

### 1. 启动后端

使用 IDEA 打开 `backend/`，运行 `BackendApplication`。

或使用命令：

```bash
cd backend
./mvnw spring-boot:run
```

验证：

```bash
curl http://localhost:3001/health
```

### 2. 启动网关

```bash
cd gateway
npm install
npm run dev
```

验证：

```bash
curl http://localhost:8080/api/admin/dashboard
```

### 3. 启动后台管理端

```bash
cd frontend
npm install
npm run dev
```

访问：

```text
http://localhost:8081/admin
```

### 4. 启动用户 H5 端

```bash
cd frontend
npm run dev:h5
```

访问终端输出的 H5 地址，通常类似：

```text
http://localhost:8082/#/
```

## 主要接口

统一响应格式：

```json
{
  "code": 0,
  "success": true,
  "message": "success",
  "data": {},
  "timestamp": 1778400000000
}
```

| 功能 | 方法 | 路径 | 描述 |
| --- | --- | --- | --- |
| 健康检查 | GET | `/health` | 检查后端服务状态 |
| 数据概览 | GET | `/api/admin/dashboard?stream_id=xxx` | 返回直播状态、票数、用户数、评委、流程 |
| 直播流列表 | GET | `/api/admin/streams` | 返回所有 mock 直播流 |
| 创建直播流 | POST | `/api/admin/streams` | 新增 mock 直播流 |
| 更新直播流 | PUT | `/api/admin/streams/{id}` | 修改直播流信息 |
| 删除直播流 | DELETE | `/api/admin/streams/{id}` | 删除直播流 |
| 开始直播 | POST | `/api/v1/admin/live/start` | 按 streamId 开启指定直播间 |
| 停止直播 | POST | `/api/v1/admin/live/stop` | 按 streamId 停止指定直播间 |
| 直播状态 | GET | `/api/admin/live/status?stream_id=xxx` | 查询指定直播流状态 |
| 管理端票数 | GET | `/api/admin/votes?stream_id=xxx` | 查询指定流票数 |
| 设置票数 | PUT | `/api/admin/votes` | 覆盖指定流票数 |
| 更新票数 | POST | `/api/admin/live/update-votes` | 设置或增加票数 |
| 用户投票 | POST | `/api/user-vote` | 模拟用户投票并更新票数 |
| 评委列表 | GET | `/api/admin/judges?stream_id=xxx` | 查询指定流评委配置 |
| 保存评委 | POST | `/api/admin/judges` | 保存指定流评委配置 |
| 辩论流程 | GET | `/api/admin/debate-flow?stream_id=xxx` | 查询指定流流程配置 |
| 保存流程 | POST | `/api/admin/debate-flow` | 保存指定流流程配置 |
| 流程控制 | POST | `/api/admin/debate-flow/control` | start/pause/resume/reset/next/prev |
| 用户列表 | GET | `/api/admin/users` | 返回 mock 用户列表 |
| 微信登录 | POST | `/api/wechat-login` | 返回 mock token/openid |
| AI 内容列表 | GET | `/api/v1/admin/ai-content/list` | 返回 mock AI 识别内容 |
| 新增 AI 内容 | POST | `/api/admin/ai-content` | 新增 mock AI 内容 |
| 更新 AI 内容 | PUT | `/api/admin/ai-content/{id}` | 修改 mock AI 内容 |
| 删除 AI 内容 | DELETE | `/api/admin/ai-content/{id}` | 删除 mock AI 内容 |
| AI 启动 | POST | `/api/admin/ai/start` | 模拟 AI 识别启动 |
| AI 停止 | POST | `/api/admin/ai/stop` | 模拟 AI 识别停止 |
| 直播计划 | GET | `/api/admin/live/schedule` | 查询 mock 直播计划 |
| 保存计划 | POST | `/api/admin/live/schedule` | 保存 mock 直播计划 |
| 取消计划 | POST | `/api/admin/live/schedule/cancel` | 取消 mock 直播计划 |

## Mock 业务说明

后端使用内存数据模拟真实业务：

- 多直播流：每个 `streamId` 有独立直播状态、票数、评委、辩论流程。
- 投票系统：支持普通用户投票、管理端设置票数、增加票数、重置票数。
- 评委管理：支持保存评委姓名、职位、头像、票数，并按直播流隔离。
- AI 内容：模拟 AI 识别内容列表、评论、点赞、启动和停止状态。
- 用户管理：返回 mock 用户数据，用于后台用户列表和评委选择。
- WebSocket：网关在保存评委、流程、票数、直播状态后广播更新事件，前端可实时刷新。

## 开发过程笔记

实现思路：

1. 先启动前端和网关，确认已有页面和接口调用路径。
2. 根据前端代码和接口文档梳理主要 API。
3. 使用 Spring Boot 编写 Mock 后端，统一返回 JSON。
4. 修改网关，将 `/api/*` 转发到 Java 后端，同时保留 WebSocket 广播能力。
5. 联调后台管理端、用户 H5 端、网关、后端。

遇到的问题和解决：

- 前端 H5 无法启动：补充 uni-app H5 依赖，修复 `src/manifest.json`、webpack/vue-loader 兼容问题。
- 网关缺少 `admin/` 和 `data/`：按原项目 README 要求复制到网关目录。
- 多直播流状态混乱：将全局直播状态改成按 `streamId` 独立存储。
- 评委保存后大屏不更新：后端标准化评委字段，网关增加 WebSocket 广播。
- 详情按钮为占位：改为 mock 详情弹窗，展示投票、用户、AI 内容和数据条。

## 部署步骤建议

可以在同一台服务器运行三个服务：

```bash
# 后端
cd backend
./mvnw -DskipTests package
java -jar target/live-backend-0.0.1-SNAPSHOT.jar

# 网关
cd gateway
npm install
npm start

# 前端后台
cd frontend
npm install
npm run dev

# 用户 H5 构建
npm run build:h5
```

生产环境建议：

- 使用 Nginx 或云平台反向代理：
  - `/admin` 指向后台管理端
  - `/h5` 或 `/` 指向 H5 静态文件
  - `/api` 指向网关
- 后端端口 `3001` 不直接暴露公网，只由网关访问。
- README 中的公网演示地址在部署完成后替换。

## 可扩展性思考

如果改为真实后端，可以将当前内存数据替换为数据库：

- 直播流、辩题、用户、评委、投票记录使用 MySQL/PostgreSQL。
- 实时投票和直播状态可使用 Redis 存储并做发布订阅。
- WebSocket 广播可以拆分为独立消息服务。
- 微信登录接入真实 code2session。
- AI 内容接入真实语音识别或大模型服务。

## 个人介绍

我主要学习 Java 后端开发，熟悉 Spring Boot 基础接口开发，正在补充前后端联调、Mock 服务、项目部署和工程化能力。本项目用于练习从已有前端项目反推接口、实现后端服务并完成端到端演示。
