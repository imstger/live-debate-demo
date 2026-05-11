# 直播辩论 Mock 演示系统

本项目基于给定的前端项目和网关项目，补充实现了一个 Java Spring Boot Mock 后端服务，用于模拟直播辩论、投票、评委、辩论流程、AI 内容、用户管理等主要业务流程。项目目标是让前端页面可以在线完整演示，不依赖真实数据库、真实微信登录或真实直播推流服务。

## 基本信息

- 项目名称：直播辩论 Mock 演示系统
- GitHub 仓库：[https://github.com/imstger/live-debate-demo](https://github.com/imstger/live-debate-demo)
- 前端来源：[https://github.com/xuelinc91-creator/Live](https://github.com/xuelinc91-creator/Live)
- 网关来源：[https://github.com/xuelinc91-creator/live-gateway](https://github.com/xuelinc91-creator/live-gateway)

## 演示地址

| 类型 | 地址 | 说明 |
| --- | --- | --- |
| 用户直播/投票前端 | [https://live-debate-demo.vercel.app/](https://live-debate-demo.vercel.app/) | uni-app H5 用户端，包含登录、选择直播间、进入直播投票 |
| 后台 API | [https://live-debate-demo.vercel.app/admin/](https://live-debate-demo.vercel.app/admin/) | 管理直播流、投票、评委、辩论流程、AI 内容等 |

## 技术栈说明

| 模块 | 技术 |
| --- | --- |
| 用户端/后台前端 | uni-app、Vue 2、静态后台管理页面 |
| 网关 | Node.js、Express、WebSocket、http-proxy-middleware |
| 后端 | Java 21、Spring Boot 4、Spring WebMVC |
| Mock 数据 | Java 内存 Map/List 模拟直播流、票数、用户、评委、AI 内容、流程状态 |
| 部署平台 | 前端部署到 Vercel，网关和后端部署到 Render |

## 项目结构

```text
live-debate-demo/
├── frontend/         # 给定前端项目，包含用户 H5 页面和后台管理页面
├── gateway/          # 给定网关项目，负责 /api 转发和 WebSocket 通信
├── backend/          # 本项目编写的 Spring Boot Mock 后端
└── README.md         # 项目说明文件
```

后端核心结构：

```text
backend/
├── Dockerfile
├── pom.xml
└── src/main/java/com/stger/livebackend/
    ├── BackendApplication.java
    └── MockApiController.java
```

## 主要接口

统一响应格式示例：

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
| 重置票数 | POST | `/api/admin/votes/reset` | 将指定流票数重置为 0 |
| 用户投票 | POST | `/api/user-vote` | 模拟用户投票并更新票数 |
| 用户端辩题 | GET | `/api/v1/debate-topic` | 用户端获取当前辩题 |
| 用户端投票 | POST | `/api/v1/user-votes` | 用户端提交投票 |
| 评论 | POST | `/api/comment` | 模拟用户评论 |
| 点赞 | POST | `/api/like` | 模拟点赞 |
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

## 开发过程笔记

实现思路：

1. 先启动前端和网关，确认已有页面、接口调用路径和运行方式。
2. 阅读前端代码和接口文档，整理后台管理端、用户 H5 端、大屏端所需接口。
3. 使用 Spring Boot 编写 Mock 后端，所有数据保存在内存结构中，统一返回 JSON。
4. 修改网关配置，将 `/api/*` 转发到 Java 后端，并保留 WebSocket 能力。
5. 联调用户端、后台端、大屏、网关、后端，补齐投票、评委、流程、AI 内容等主要场景。
6. 分别部署到 Vercel 和 Render，配置公网 API 地址。

遇到的问题和解决：

- 评委配置、票数、辩论流程保存后，大屏幕没有明显变化。后来发现前端依赖 WebSocket 或重新请求接口获取最新数据，于是在后端保存数据后同步更新 mock 数据，并通过网关广播更新事件。
- 一开始启动一个直播间时，其他直播间也可能一起变化。后来把直播状态、票数、评委、流程都按照 streamId 分开保存，让每个直播间的数据独立。
- 部署后部分图标、评委头像没有显示出来。后来在构建时复制 static 静态资源，并给 mock 评委设置默认头像，保证页面展示完整。

## 部署步骤

### 后端 Render

- 平台：Render Web Service
- Runtime：Docker
- Root Directory：`backend`
- Dockerfile：`backend/Dockerfile`
- 健康检查：`/health`

### 网关 Render

- 平台：Render Web Service
- Runtime：Node
- Root Directory：`gateway`
- Build Command：`npm install`
- Start Command：`npm start`
- 环境变量：

```text
BACKEND_SERVER_URL=https://live-debate-backend-u04u.onrender.com
```

### 前端 Vercel

- 平台：Vercel
- Root Directory：`frontend`
- Build Command：`npm install --legacy-peer-deps && npm run build:h5`
- Output Directory：`dist/build/h5`
- 用户端入口：`/`
- 后台入口：`/admin/`

## 可扩展性思考

如果后续改为真实业务后端，可以从以下方向扩展：

- 将当前内存数据替换为 MySQL，持久化直播流、辩题、用户、评委和投票记录。
- 使用 Redis 保存实时投票、直播状态和流程计时状态，配合发布订阅做实时推送。
- 将 WebSocket 广播拆成独立实时消息服务，支持更多并发连接。
- 接入真实微信登录，将 mock token 替换为 JWT。
- AI 内容可以接入大模型进行识别判断。

## 个人介绍

我主要以 Java 后端开发为主，掌握 Java 集合/IO/多线程/面向对象设计，熟悉 Spring Boo、微服务框架，掌握MySQL、Redis、RabbitMQ等数据库与分布式中间件，具备一定的并发优化、缓存架构设计、接口并行化处理能力。希望进一步提升 高并发微服务与系统稳定性方面的开发能力。
