## 本地手动提交到 GitHub（备忘）

```bash
# 1. 进入项目根目录
cd D:\Develop\project\project\flash-sale-system

# 2. 查看当前修改
git status

# 3. 添加所有修改（也可以只 add 某些文件）
git add .

# 4. 提交（把 提交说明 换成本次修改的描述）
git commit -m "提交说明"

# 5. 推送到远程 main 分支
git push origin HEAD:main
```

---

## 高并发限时秒杀系统（前后端分离版总览）

这是一个包含 **后端 Spring Boot 服务** 和 **前端 Vue 3 + Vite 单页应用** 的秒杀示例项目，方便面试官或学习者一眼看到整体结构：

- `backend/`：Java 后端（Spring Boot + MyBatis-Plus + Redis + MySQL + RabbitMQ）
- `frontend/`：前端管理及用户端页面（Vue 3 + Vite）

后端的详细学习说明文档已放在 `backend/README.md` 中，前端说明文档在 `frontend/README.md` 中。

---

## 一、项目结构

```text
flash-sale-system/
  backend/           # Spring Boot 后端
    pom.xml
    src/
    sql/
    README.md        # 后端详细说明（原项目 README）
  frontend/          # Vue 3 + Vite 前端
    src/
    README.md        # 前端说明
  .gitignore
  .gitattributes
```

---

## 二、快速启动（本地开发）

### 1. 准备环境

- **后端必需**
  - JDK 17
  - Maven 3.9+
  - MySQL 8.x
  - Redis（本地单机即可）
  - （可选）RabbitMQ：用于开启异步下单功能
- **前端必需**
  - Node.js 16+（建议 18+）
  - npm 或 pnpm

### 2. 初始化数据库

在 MySQL 中创建数据库，并执行 SQL 脚本（均位于 `backend/sql/`）：

- `schema.sql`：建表
- `init_data.sql`：初始化数据
- 如使用新增字段功能，可根据 `backend/README.md` 中的说明执行 `alter_add_columns.sql`

### 3. 启动后端（backend）

```bash
cd backend
mvn spring-boot:run
```

默认端口：`http://localhost:8081`。  
更多配置（数据库、Redis、RabbitMQ、JWT 等）详见 `backend/src/main/resources/application.yml` 与 `backend/README.md`。

### 4. 启动前端（frontend）

```bash
cd frontend
npm install
npm run dev
```

默认端口：`http://localhost:5173`。  
Vite 已将 `/seckill`、`/admin` 等接口代理到 `http://localhost:8081`，无需额外配置 CORS。

---

## 三、面试官速览指引

- **想快速看业务与并发设计**：从 `backend/README.md` 中的「代码阅读路线」开始，重点关注：
  - `SeckillController` / `SeckillService.executeSeckill`
  - Redis + Lua 扣库存
  - RabbitMQ 异步下单与死信队列
  - 统一返回结构 `ApiResponse`
- **想快速看页面与交互**：前往 `frontend/`，聚焦：
  - `views/GoodsList.vue`、`GoodsDetail.vue`、`MyOrders.vue`、`Metrics.vue`
  - `api/seckill.js`、`api/admin.js` 中的接口封装

---

## 四、后续扩展

本仓库适合作为学习「高并发秒杀」与「前后端分离项目结构」的入门模板，你可以在此基础上继续扩展：

- 接入真实支付渠道（微信 / 支付宝）
- 接入统一网关与限流
- 完善监控（Prometheus / Grafana）与日志采集（ELK）

如需了解后端实现细节，请直接阅读 `backend/README.md`。

