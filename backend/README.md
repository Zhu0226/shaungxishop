## 高并发限时秒杀系统（零基础学习版）

这是一个基于 **Spring Boot + MyBatis-Plus + Redis + MySQL** 的「高并发限时秒杀系统」示例项目。  
这个 README 的目标不是炫技术，而是：**带你从完全没看过代码，到能跑通、能看懂核心逻辑**。

阅读顺序会像上课一样，分成几次「学习任务」逐步加深。

> **维护约定**：项目里新增或改动功能（新接口、新配置、新依赖）时，请同步在本 README 中补充对应说明（接口路径、配置项、涉及文件），便于后续阅读和交接。

---

## 前端说明（Vue 3 + Vite）

前端项目位于 `frontend/` 目录，技术栈：Vue 3、Vite、Vue Router、Axios。

**启动**：先启动后端（`mvn spring-boot:run`，默认端口 8081），再在 `frontend` 目录执行 `npm install` 和 `npm run dev`。前端默认运行在 http://localhost:5173，通过 Vite 代理将 `/seckill`、`/admin` 转发到后端 8081 端口。

**页面与后端接口对应**：

| 前端页面 | 路径 | 主要功能 | 对应后端接口 |
|----------|------|----------|--------------|
| 秒杀首页 | `/` | 商品列表、切换用户 ID、活动时间、秒杀指标入口 | `GET /seckill/goods` |
| 商品详情 | `/goods/:id` | 活动状态、立即抢购、已抢状态 | `GET /seckill/goods/:id`、`status`、`path`、`doSeckill`、`orders/result` |
| 我的订单 | `/orders` | 订单列表、支付、取消 | `GET /seckill/orders`、`orders/:id/pay`、`orders/:id/cancel` |
| 秒杀指标 | `/metrics` | 总请求数、成功/失败数、成功率 | `GET /seckill/metrics` |
| 后台登录 | `/admin/login` | 登录 | `POST /admin/login` |
| 商品管理 | `/admin/goods` | CRUD、预热、活动时间、秒杀指标 | `GET/POST/PUT/DELETE /admin/seckill/goods`、`POST /seckill/preheat/:id/:stock` |

详见 `frontend/README.md`。

---

## 一、第一次接触：先跑起来，再看效果

### 1. 安装和准备

- **必须安装**：
  - JDK 17
  - Maven 3.9+
  - MySQL 8.x
  - Redis（本地单机即可）

### 2. 建库建表（只做一次）

1）在 MySQL 中创建数据库：

```sql
CREATE DATABASE flash_sale CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2）在 `flash_sale` 库中创建两张表（结构与你代码中的实体对应）：

```sql
CREATE TABLE seckill_goods (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    goods_name VARCHAR(255) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    on_shelf TINYINT NOT NULL DEFAULT 1 COMMENT '0下架 1上架',
    activity_tag VARCHAR(64) DEFAULT '' COMMENT '活动标签',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE seckill_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    goods_id BIGINT NOT NULL,
    status TINYINT NOT NULL DEFAULT 0, -- 0-新建未支付，1-已支付，2-已取消
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_goods (user_id, goods_id) -- 一人一单
);
```

### 3. 修改配置（告诉项目你的数据库/Redis 在哪里）

打开 `src/main/resources/application.yml`，按你本机情况改：

- **HTTP 端口**
  - 默认 `server.port: 8081`（与前端代理一致）

- **MySQL 连接**
  - `spring.datasource.url: jdbc:mysql://localhost:3306/flash_sale?...`
  - `spring.datasource.username: 你的用户名`
  - `spring.datasource.password: 你的密码`

- **Redis 连接**
  - `spring.data.redis.host: localhost`
  - `spring.data.redis.port: 6379`
  - **如果 Redis 没有密码**：删除或注释掉 `password:` 这一行。

### 4. 启动项目

在项目根目录（有 `pom.xml` 的目录）打开终端，执行：

```bash
mvn spring-boot:run
```

终端中出现类似日志就表示启动成功：

- `Tomcat started on port 8081`
- `Started FlashSaleSystemApplication...`

先停在这里：**不要急着看代码**，下一节我们用接口“玩一圈”，直观感受一下。

---

## 二、第二次接触：通过接口感受整个流程

所有接口前缀为：`/seckill`  
大部分接口都要在请求头中加：`userId`（用来表示“当前用户是谁”）。

下面用一个「从预热到下单」的完整流程，帮助你建立整体印象。

### 1. 预热库存（运营/后台做的事）

请求：

```http
POST http://localhost:8081/seckill/preheat/1/100
```

含义：把商品 ID 为 1 的库存，预热 100 件到 Redis 里。  
响应一般类似：

```json
{
  "code": 0,
  "msg": "库存预热成功",
  "data": null
}
```

### 2. 用户获取动态秒杀地址

```http
GET http://localhost:8081/seckill/path?goodsId=1
Header: userId: 1001
```

响应示例：

```json
{
  "code": 0,
  "msg": "OK",
  "data": "xxxxxxxxxxxx"  // 一串随机字符串
}
```

记住 `data` 里的这串字符串，下面要用它来“真正秒杀”。

### 3. 用户发起秒杀请求

```http
POST http://localhost:8081/seckill/xxxxxxxxxxxx/doSeckill?goodsId=1
Header: userId: 1001
```

可能的响应：

- 抢到：

```json
{
  "code": 0,
  "msg": "OK",
  "data": "抢购成功！"
}
```

- 没抢到 / 各种失败（code 不为 0）：

```json
{
  "code": -1,
  "msg": "很遗憾，商品已售罄",
  "data": null
}
```

### 4. 查询自己是否抢到了

```http
GET http://localhost:8081/seckill/orders/result?goodsId=1
Header: userId: 1001
```

有订单时会返回订单信息；没抢到会返回一个 404 风格的错误信息。

### 5. 看一下整体指标

```http
GET http://localhost:8081/seckill/metrics
```

可以看到：

- 总请求次数
- 成功次数
- 失败次数
- 成功率

### 6. 统一响应格式与错误码

所有接口返回 JSON 格式：`{ "code": 0, "msg": "OK", "data": ... }`。`code === 0` 表示成功，非 0 表示失败，`msg` 为提示信息。

常见错误码（`ErrorCode` 枚举）：

| code | 含义 |
|------|------|
| 0 | 成功 |
| 400 | 请求参数错误 |
| 404 | 资源不存在 |
| 429 | 请求太频繁（限流） |
| 500 | 系统异常 |
| 1001 | 商品不存在 |
| 1002 | 秒杀活动尚未开始 |
| 1003 | 秒杀活动已结束 |
| 1004 | 非法请求或链接已失效 |
| 1005 | 您已经抢购过该商品了 |
| 1006 | 很遗憾，商品已售罄 |
| 1008 | 商品不存在，无法预热 |
| 1009 | 预热库存必须大于0且不超过数据库库存 |

> 到这一步，你 **已经理解了“从外面看”这个系统是怎样工作的**。  
> 接下来我们才开始从里往外读代码。

---

## 三、第三次接触：从外到内看代码（推荐顺序）

这一部分是**代码阅读路线图**，你可以按步骤一步步在 IDE 里点进去看。

### 步骤 1：从 Controller 开始 —— HTTP 是怎么进来的？

文件：`src/main/java/com/seckill/controller/SeckillController.java`

建议阅读顺序（从最简单的开始）：

1. `listGoods()` → `GET /seckill/goods`
   - 从数据库里查出所有 `SeckillGoods` 列表，返回给前端。
2. `goodsDetail()` → `GET /seckill/goods/{goodsId}`
   - 按 ID 查询某一条商品记录。
3. `status()` → `GET /seckill/status/{goodsId}`
   - 调用 Service，看当前时间 + 库存情况，判断活动状态。
4. `preheat()` → `POST /seckill/preheat/{goodsId}/{stock}`
   - 调用 `preHeatStock`，校验商品存在、库存>0、不超过 DB 库存，将库存写入 Redis。
5. `getPath()` → `GET /seckill/path`
   - 注意这里有 `@RateLimit` 注解，说明这个接口有访问频率限制。
   - 从 Header 取 `userId`，从参数取 `goodsId`，调用 `createSeckillPath`。
6. `doSeckill()` → `POST /seckill/{path}/doSeckill`
   - 真正的秒杀入口，把 `userId/goodsId/path` 传给 `executeSeckill`。
7. `userOrders()` / `seckillResult()` / `metrics()`
   - 对应前面你调用过的订单查询和指标接口。

**看 Controller 的时候，只关心两件事：**

1. URL ↔ 方法名 ↔ 业务含义。
2. 这个方法最后调用了 Service 的哪一个方法。

### 步骤 2：看 Service —— 真正的业务逻辑在哪？

文件：`src/main/java/com/seckill/service/impl/SeckillService.java`

建议阅读顺序：

1. 先看顶部的字段：
   - `RedisTemplate`、`seckillScript`、`SeckillOrderMapper`、`SeckillGoodsMapper`。
   - 一些以 `PREFIX` 结尾的常量（这些是 Redis key 的前缀）。

2. 再看简单的方法：
   - `listSeckillGoods()`：使用 MyBatis-Plus 查列表；
   - `getGoodsDetail()`：查单条记录；
   - `preHeatStock()`：直接把库存写到 Redis；
   - `getSeckillStatus()`：根据当前时间和库存，推断活动状态。

3. 然后看“获取动态路径”的方法：
   - `createSeckillPath(Long userId, Long goodsId)`：
     - 校验商品存在；
     - 校验活动时间（未开始/已结束会抛异常）；
     - 生成随机字符串，写入 Redis（key 含 userId + goodsId）。

4. 最后看“秒杀核心”：
   - `executeSeckill(Long userId, Long goodsId, String path)`：
     - 看清每一个 `return` 的含义（对应 Controller 返回的提示文案）；
     - 对照前面你实际调用接口时看到的响应，一条条对上。

> 提示：阅读这个方法时，可以在纸上画一个流程图（从校验开始，到下单结束），会很有帮助。

### 步骤 3：看实体和 Mapper —— 它们如何和数据库对应？

1. 实体：
   - `SeckillGoods.java` ↔ 表 `seckill_goods`
   - `SeckillOrder.java` ↔ 表 `seckill_order`
   - 一边看字段，一边对照你创建表时的 SQL 语句。

2. Mapper：
   - `SeckillGoodsMapper.java`：重点看 `reduceStock` 上的 SQL。
   - `SeckillOrderMapper.java`：目前主要是用它的 `insert` 和查询方法。

### 步骤 4：看 Redis 和 Lua —— 高并发下如何扣库存？

1. `RedisConfig.java`
   - 明白：这里就是在告诉 Spring “怎么连 Redis、怎么执行 Lua 脚本”的。

2. `seckill.lua`
   - 尽量逐行理解：
     - 先 `exists` 判断 key；
     - 再 `get` 看库存是否 > 0；
     - 然后 `decr`；
     - 不满足条件时返回不同的数字（1/0/-1）。

### 步骤 5：看限流注解和拦截器

1. `RateLimit.java`
   - 注解上只有两个参数：
     - `time`：几秒；
     - `count`：这几秒内最多允许多少次。

2. `RateLimitInterceptor.java`
   - 只看 `preHandle` 方法就行：
     - 拿到注解；
     - 从 Header 里取 `userId`；
     - 组合 Redis key；
     - 看一下当前计数，超过就直接返回“请求太频繁”。

3. `WebMvcConfig.java`
   - 注册拦截器，让上面的逻辑真正生效。

---

## 四、统一返回结构（为什么所有接口长得一样？）

所有接口都返回 `ApiResponse<T>`，你在任何一个接口里看到 `ApiResponse.success(...)` / `ApiResponse.error(...)` 都可以联想到这个结构：

```json
{
  "code": 0,
  "msg": "OK",
  "data": { ... }
}
```

- `code = 0`：成功。
- `code != 0`：失败，原因在 `msg` 里。
- `data`：真正的业务数据，可以是对象、列表或简单字符串。

对应代码在：`src/main/java/com/seckill/common/ApiResponse.java`。

---

## 五、如果你完全是 Java 零基础，可以这样练习

1. **第 1 天**：只做这几件事
   - 装好环境；
   - 按「第一次接触」那一节把项目跑起来；
   - 用接口走完整个流程（预热 → 路径 → 秒杀 → 查订单）。

2. **第 2 天**：只看 Controller 和部分 Service
   - 对照接口，把 `SeckillController` 里的每个方法的作用写在纸上；
   - 去 `SeckillService` 中只看：`listSeckillGoods`、`getGoodsDetail`、`preHeatStock`、`getSeckillStatus`；
   - 尝试自己在代码上加几句注释（你自己的理解）。

3. **第 3 天**：攻克 `executeSeckill`
   - 一行一行看，配合你前两天打过的接口；
   - 搞清楚每个失败分支是怎么走到的。

4. **第 4 天**：看 Redis + Lua + 限流
   - 明白 Redis 在这个项目里承担了哪些职责（缓存库存、一人一单、路径、指标等）；
   - 明白限流是在控制什么（主要是控制“获取路径”和“接口被疯狂刷”的情况）。

每次阅读不要追求“一口气看完所有代码”，而是围绕“我现在要搞懂哪一个功能” 来有选择地看。

---

## 六、新增代码说明（上架/标签、后台 CRUD、MQ 异步下单、Prometheus）

以下功能已在项目中实现，对应文件与作用如下。**后续每次加新功能，请在本节或本节子标题下补充说明**，便于 README 与代码同步。

### 6.1 商品「是否上架」与「活动标签」

- **实体**：`src/main/java/com/seckill/entity/SeckillGoods.java`
  - `onShelf`：Integer，0=下架，1=上架；C 端列表 `GET /seckill/goods` 只返回 `onShelf=1`。
  - `activityTag`：String，如「双11」「限时」，列表与详情接口会一并返回。
- **若表是旧版无这两列**，在 MySQL 中执行：
  ```sql
  ALTER TABLE seckill_goods
    ADD COLUMN on_shelf TINYINT NOT NULL DEFAULT 1 COMMENT '0下架 1上架',
    ADD COLUMN activity_tag VARCHAR(64) DEFAULT '' COMMENT '活动标签';
  ```

### 6.2 后台管理 CRUD（商品与活动时间）

- **控制器**：`src/main/java/com/seckill/controller/AdminSeckillController.java`
  - 前缀：`/admin/seckill/goods`
  - `GET /admin/seckill/goods`：全部商品（含未上架）
  - `GET /admin/seckill/goods/{id}`：详情
  - `POST /admin/seckill/goods`：新增（body 含 goodsName、price、stock、startTime、endTime、onShelf、activityTag）
  - `PUT /admin/seckill/goods/{id}`：更新
  - `DELETE /admin/seckill/goods/{id}`：删除
- **服务层**：`SeckillService` 中 `listAllGoodsForAdmin`、`saveGoods`、`updateGoods`、`removeGoods` 支撑上述接口。生产环境建议对 `/admin/**` 做鉴权。

### 6.3 RabbitMQ 异步下单

- **开关**：`application.yml` 中 `seckill.order.async: false` 为默认（同步写库）；改为 `true` 且本机/环境启动 RabbitMQ 后，秒杀扣减成功后只发 MQ，由消费者落库订单。
- **配置**：`src/main/java/com/seckill/config/RabbitMQConfig.java`（交换机、队列、绑定）。
- **消息体**：`src/main/java/com/seckill/mq/SeckillOrderMessage.java`（userId、goodsId、timestamp）。
- **生产者**：`src/main/java/com/seckill/mq/SeckillOrderProducer.java`，在 `SeckillService.executeSeckill` 中 DB 扣减成功后调用。
- **消费者**：`src/main/java/com/seckill/mq/SeckillOrderConsumer.java`，`@RabbitListener` 消费后插入 `seckill_order`；重复消息由表唯一索引 `(user_id, goods_id)` 幂等。

- **消费者**：`src/main/java/com/seckill/mq/SeckillOrderConsumer.java`，`@RabbitListener` 消费后插入 `seckill_order`；重复消息由表唯一索引 `(user_id, goods_id)` 幂等。

### 6.5 后台 JWT 鉴权

- **登录**：`POST /admin/login`，body `{"username":"admin","password":"123456"}`，返回 `{ "token": "..." }`。其他 `/admin/**` 请求需在 Header 中携带 `Authorization: Bearer <token>`。
- **配置**：`application.yml` 中 `admin.jwt-secret`、`admin.jwt-expire-minutes`、`admin.login-username`、`admin.login-password`。
- **实现**：`JwtProperties`、`JwtUtils`（生成/解析）、`AdminAuthFilter`（校验 Bearer）、`AdminAuthConfig`（注册过滤器）、`AdminLoginController`（登录接口）。放行 `/admin/login`，其余 `/admin/*` 必须带有效 token。

### 6.6 订单状态机与支付/取消

- **状态**：`OrderStatus` 枚举 0=待支付、1=已支付、2=已取消。仅允许 0→1（支付）、0→2（取消）。
- **接口**：`POST /seckill/orders/{orderId}/pay`、`POST /seckill/orders/{orderId}/cancel`，需 Header `userId`，仅本人且状态为待支付的订单可操作。
- **实现**：`SeckillOrderMapper.updateStatus`、`SeckillService.payOrder`/`cancelOrder`，`SeckillOrder` 增加 `updateTime`。若表无 `update_time` 需执行：
  ```sql
  ALTER TABLE seckill_order ADD COLUMN update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP;
  ```

### 6.7 MQ 死信队列与重试

- **配置**：`RabbitMQConfig` 中主队列 `seckill.order.queue` 设置 `x-dead-letter-exchange`、`x-dead-letter-routing-key`；死信交换机 `seckill.order.dlx`、死信队列 `seckill.order.dlq`。`SimpleRabbitListenerContainerFactory` 设置 `defaultRequeueRejected=false`，消费失败不重新入队，消息进入 DLQ。
- **死信消费者**：`SeckillOrderDlqConsumer` 监听 `seckill.order.dlq`，打 error 日志便于排查与告警。

### 6.8 executeSeckill 关键分支日志（ELK 友好）

- **位置**：`SeckillService.executeSeckill` 中每个失败分支与成功分支均打日志，格式为 `step=... userId=... goodsId=...`（及 `reason=...`、`durationMs=...`），便于集中式日志采集与检索。
- **日志文件**：`logback-spring.xml` 中 `com.seckill` 同时输出到控制台和 `logs/seckill.log`，按天滚动，保留 7 天。

### 6.9 Prometheus / Actuator 监控

- **依赖**：`pom.xml` 中 `spring-boot-starter-actuator`、`micrometer-registry-prometheus`。
- **配置**：`application.yml` 中 `management.endpoints.web.exposure.include: health,info,prometheus`。
- **访问**：`GET http://localhost:8081/actuator/prometheus` 可拉取指标，便于对接 Grafana 等。

---

## 七、后续可扩展方向

- 支付渠道对接（微信/支付宝）与回调校验。
- 网关层限流与 IP 黑名单。
- 使用 logstash-logback-encoder 输出 JSON 日志，直接对接 ELK。

在此之前，最重要的是：**先把当前这套代码读懂、跑顺**。当你能自己讲清楚 `executeSeckill` 的每一步时，这个项目对你来说就已经不是黑盒了。

