# 秒杀系统 - 严格四层架构

## 一、目录结构

```
com.seckill/
├── presentation/           # 表现层
│   ├── controller/         # 控制器
│   ├── filter/             # 过滤器
│   ├── interceptor/        # 拦截器
│   ├── config/             # Web 配置 (WebMvc, AdminAuth)
│   └── exception/          # 全局异常处理
│
├── application/            # 业务层
│   └── service/
│       ├── SeckillService.java
│       ├── OrderService.java
│       └── impl/
│
├── persistence/            # 持久层
│   └── mapper/
│
├── domain/                 # 领域层
│   ├── entity/
│   ├── vo/
│   └── enums/
│
├── infrastructure/         # 基础设施
│   ├── config/             # Redis, RabbitMQ, Jwt
│   └── mq/                 # 消息队列
│
└── common/                 # 跨层通用
    ├── exception/          # 业务异常
    ├── annotation/         # 注解
    └── util/               # 工具类
```

## 二、四层职责

| 层级 | 包路径 | 职责 |
|------|--------|------|
| **表现层** | presentation | 控制器、过滤器、拦截器、Web 配置、全局异常 |
| **业务层** | application | 业务接口与实现、事务控制 |
| **持久层** | persistence | Mapper、数据访问 |
| **领域层** | domain | 实体、VO、枚举 |

## 三、逻辑漏洞修复（按用户提供代码）

1. **compensateRedisState**：增库存前判断 Key 是否存在
   ```java
   Boolean hasKey = stringRedisTemplate.hasKey(STOCK_PREFIX + goodsId);
   if (Boolean.TRUE.equals(hasKey)) {
       stringRedisTemplate.opsForValue().increment(STOCK_PREFIX + goodsId);
   }
   ```

2. **cancelOrder**：同样在 increment 前判断 Key 存在

3. **RateLimitInterceptor**：直接以 `request.getRemoteAddr()` 为主，不信任 X-Forwarded-For

4. **MQ 消费者**：不在 catch 中补偿，补偿移至 DLQ 单次执行

5. **Lua 脚本**：合并 exists/get/decr 为 get/decr，精简调用
