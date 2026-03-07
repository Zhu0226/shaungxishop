# 秒杀系统前端（Vue 3 + Vite）

## 技术栈

- Vue 3
- Vite
- Vue Router 4
- Axios

## 启动

1. 确保后端已启动（`mvn spring-boot:run`，端口 8081）。
2. 安装依赖并启动前端：

```bash
npm install
npm run dev
```

3. 访问 http://localhost:5173

## 目录结构

```
src/
  api/          # 接口封装
    request.js  # axios 实例、JWT 注入、401 跳转
    seckill.js  # C 端秒杀接口
    admin.js    # 后台管理接口
  router/       # 路由配置
  views/        # 页面组件
    GoodsList.vue    # 秒杀首页
    GoodsDetail.vue  # 商品详情
    MyOrders.vue     # 我的订单
    Metrics.vue      # 秒杀指标
    AdminLogin.vue   # 后台登录
    AdminGoods.vue   # 商品管理
```

## 页面说明

| 路径 | 页面 | 功能 | 接口 |
|------|------|------|------|
| `/` | 秒杀首页 | 商品列表、切换用户 ID、展示活动时间、空列表提示、秒杀指标入口 | `listGoods` → `GET /seckill/goods` |
| `/goods/:id` | 商品详情 | 活动状态、立即抢购、已抢状态与订单信息 | `getGoodsDetail`、`getStatus`、`getPath`、`doSeckill`、`getOrderResult` |
| `/orders` | 我的订单 | 订单列表、支付、取消、空列表提示 | `listOrders`、`payOrder`、`cancelOrder` |
| `/metrics` | 秒杀指标 | 总请求数、成功数、失败数、成功率 | `getMetrics` → `GET /seckill/metrics` |
| `/admin/login` | 后台登录 | 登录 | `login` → `POST /admin/login` |
| `/admin/goods` | 商品管理 | 新增/编辑/删除/预热、活动时间列、空状态、秒杀指标入口 | `listGoods`、`createGoods`、`updateGoods`、`deleteGoods`、`preheat` |

## 代理配置

Vite 将 `/seckill`、`/admin` 代理到 `http://localhost:8081`，无需后端配置 CORS。
