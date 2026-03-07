package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.common.ErrorCode;
import com.seckill.entity.SeckillGoods;
import com.seckill.entity.SeckillOrder;
import com.seckill.exception.BusinessException;
import com.seckill.mapper.SeckillGoodsMapper;
import com.seckill.mapper.SeckillOrderMapper;
import com.seckill.mq.SeckillOrderProducer;
import com.seckill.vo.SeckillMetricsVO;
import com.seckill.vo.SeckillStatusVO;
import com.seckill.enums.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SeckillService {

    private static final Logger log = LoggerFactory.getLogger(SeckillService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate; // 引入 StringRedisTemplate
    @Autowired
    private DefaultRedisScript<Long> seckillScript;
    @Autowired
    private SeckillOrderMapper orderMapper;
    @Autowired
    private SeckillGoodsMapper goodsMapper;
    @Autowired(required = false)
    private SeckillOrderProducer orderProducer;
    @Value("${seckill.order.async:false}")
    private boolean asyncOrder;

    private static final String STOCK_PREFIX = "seckill:stock:";
    private static final String PATH_PREFIX = "seckill:path:";
    private static final String USER_ORDER_PREFIX = "seckill:order:user:";
    private static final String GOODS_LIST_CACHE_KEY = "seckill:goods:list";
    private static final String GOODS_DETAIL_CACHE_PREFIX = "seckill:goods:detail:";

    private static final String METRIC_TOTAL_KEY = "seckill:metric:total";
    private static final String METRIC_SUCCESS_KEY = "seckill:metric:success";
    private static final String METRIC_FAIL_KEY = "seckill:metric:fail";

    public void preHeatStock(Long goodsId, int stock) {
        if (stock <= 0) throw new BusinessException(ErrorCode.PREHEAT_STOCK_INVALID, "预热库存必须大于0");
        SeckillGoods goods = goodsMapper.selectById(goodsId);
        if (goods == null) throw new BusinessException(ErrorCode.PREHEAT_GOODS_NOT_FOUND);
        int dbStock = goods.getStock() != null ? goods.getStock() : 0;
        if (stock > dbStock) throw new BusinessException(ErrorCode.PREHEAT_STOCK_INVALID, "预热库存不能超过数据库库存");

        // 使用 stringRedisTemplate 存入纯数字字符串
        stringRedisTemplate.opsForValue().set(STOCK_PREFIX + goodsId, String.valueOf(stock));
        log.info("preHeatStock goodsId={} stock={} dbStock={}", goodsId, stock, dbStock);
    }

    @SuppressWarnings("unchecked")
    public List<SeckillGoods> listSeckillGoods() {
        List<SeckillGoods> list = (List<SeckillGoods>) redisTemplate.opsForValue().get(GOODS_LIST_CACHE_KEY);
        if (list == null) {
            LambdaQueryWrapper<SeckillGoods> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SeckillGoods::getOnShelf, 1).orderByAsc(SeckillGoods::getStartTime);
            list = goodsMapper.selectList(wrapper);
            if (list != null) redisTemplate.opsForValue().set(GOODS_LIST_CACHE_KEY, list, 60, TimeUnit.SECONDS);
        }
        return list;
    }

    public List<SeckillGoods> listAllGoodsForAdmin() {
        LambdaQueryWrapper<SeckillGoods> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SeckillGoods::getStartTime);
        return goodsMapper.selectList(wrapper);
    }

    public SeckillGoods getGoodsDetail(Long goodsId) {
        String key = GOODS_DETAIL_CACHE_PREFIX + goodsId;
        SeckillGoods goods = (SeckillGoods) redisTemplate.opsForValue().get(key);
        if (goods == null) {
            goods = goodsMapper.selectById(goodsId);
            if (goods != null) redisTemplate.opsForValue().set(key, goods, 60, TimeUnit.SECONDS);
        }
        return goods;
    }

    public void saveGoods(SeckillGoods goods) {
        LocalDateTime now = LocalDateTime.now();
        if (goods.getCreateTime() == null) goods.setCreateTime(now);
        goods.setUpdateTime(now);
        goodsMapper.insert(goods);
        clearGoodsCache(goods.getId());
    }

    public void updateGoods(SeckillGoods goods) {
        goods.setUpdateTime(LocalDateTime.now());
        goodsMapper.updateById(goods);
        clearGoodsCache(goods.getId());
        // 修复：后台修改库存时，如果 Redis 已预热，必须同步更新 Redis 库存
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(STOCK_PREFIX + goods.getId()))) {
            stringRedisTemplate.opsForValue().set(STOCK_PREFIX + goods.getId(), String.valueOf(goods.getStock()));
        }
    }

    public void removeGoods(Long id) {
        goodsMapper.deleteById(id);
        clearGoodsCache(id);
        stringRedisTemplate.delete(STOCK_PREFIX + id);
    }

    private void clearGoodsCache(Long goodsId) {
        redisTemplate.delete(GOODS_LIST_CACHE_KEY);
        if (goodsId != null) redisTemplate.delete(GOODS_DETAIL_CACHE_PREFIX + goodsId);
    }

    public SeckillStatusVO getSeckillStatus(Long goodsId) {
        SeckillGoods goods = getGoodsDetail(goodsId);
        SeckillStatusVO vo = new SeckillStatusVO();

        if (goods == null) {
            vo.setStatus(2); vo.setStatusDesc("活动未配置或商品不存在");
            vo.setNow(LocalDateTime.now()); return vo;
        }

        LocalDateTime now = LocalDateTime.now();
        vo.setNow(now); vo.setStartTime(goods.getStartTime()); vo.setEndTime(goods.getEndTime());

        String stockStr = stringRedisTemplate.opsForValue().get(STOCK_PREFIX + goodsId);
        Integer remainStock = stockStr != null ? Integer.parseInt(stockStr) : goods.getStock();
        vo.setRemainStock(remainStock);

        if (now.isBefore(goods.getStartTime())) {
            vo.setStatus(0); vo.setStatusDesc("未开始");
        } else if (now.isAfter(goods.getEndTime())) {
            vo.setStatus(2); vo.setStatusDesc("已结束");
        } else if (stockStr == null) {
            vo.setStatus(0); vo.setStatusDesc("未预热就绪"); // 修复误导性状态
        } else if (remainStock <= 0) {
            vo.setStatus(3); vo.setStatusDesc("已售罄");
        } else {
            vo.setStatus(1); vo.setStatusDesc("进行中");
        }
        return vo;
    }

    public String createSeckillPath(Long userId, Long goodsId) {
        SeckillGoods goods = getGoodsDetail(goodsId);
        if (goods == null) throw new BusinessException(ErrorCode.GOODS_NOT_FOUND);
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(goods.getStartTime())) throw new BusinessException(ErrorCode.ACTIVITY_NOT_STARTED);
        if (now.isAfter(goods.getEndTime())) throw new BusinessException(ErrorCode.ACTIVITY_ENDED);

        String str = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(PATH_PREFIX + userId + ":" + goodsId, str, 60, TimeUnit.SECONDS);
        return str;
    }

    public String executeSeckill(Long userId, Long goodsId, String path) {
        long startMs = System.currentTimeMillis();
        stringRedisTemplate.opsForValue().increment(METRIC_TOTAL_KEY);

        SeckillGoods goods = getGoodsDetail(goodsId);
        if (goods == null || LocalDateTime.now().isBefore(goods.getStartTime()) || LocalDateTime.now().isAfter(goods.getEndTime())) {
            recordFail();
            throw new BusinessException(ErrorCode.SECKILL_ERROR, "活动异常或不在活动时间内");
        }

        String pathKey = PATH_PREFIX + userId + ":" + goodsId;
        String realPath = stringRedisTemplate.opsForValue().get(pathKey);
        if (realPath == null || !realPath.equals(path)) {
            recordFail(); throw new BusinessException(ErrorCode.INVALID_PATH);
        }

        String userOrderKey = USER_ORDER_PREFIX + userId + ":" + goodsId;
        Boolean firstSeckill = stringRedisTemplate.opsForValue().setIfAbsent(userOrderKey, "1", 1, TimeUnit.DAYS);
        if (Boolean.FALSE.equals(firstSeckill)) {
            recordFail(); throw new BusinessException(ErrorCode.ALREADY_ORDERED);
        }

        Long luaResult = stringRedisTemplate.execute(seckillScript, Collections.singletonList(STOCK_PREFIX + goodsId));
        if (luaResult == null || luaResult == 0) {
            stringRedisTemplate.delete(userOrderKey);
            recordFail(); throw new BusinessException(ErrorCode.STOCK_SOLD_OUT);
        }
        if (luaResult == -1) {
            stringRedisTemplate.delete(userOrderKey);
            recordFail(); throw new BusinessException(ErrorCode.SECKILL_ERROR, "商品未预热就绪");
        }

        if (asyncOrder && orderProducer != null) {
            orderProducer.send(userId, goodsId);
            recordSuccess();
            return "排队抢购中...";
        }

        try {
            createOrder(userId, goodsId);
        } catch (Exception e) {
            compensateRedisState(userId, goodsId);
            throw new BusinessException(ErrorCode.ALREADY_ORDERED, "抢购落库失败");
        }

        recordSuccess();
        return "抢购成功！";
    }

    @Transactional(rollbackFor = Exception.class)
    public void createOrder(Long userId, Long goodsId) {
        int dbAffected = goodsMapper.reduceStock(goodsId);
        if (dbAffected == 0) throw new BusinessException(ErrorCode.STOCK_SOLD_OUT, "数据库库存不足");
        try {
            SeckillOrder order = new SeckillOrder();
            order.setUserId(userId);
            order.setGoodsId(goodsId);
            order.setStatus(0);
            orderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.ALREADY_ORDERED);
        }
    }

    /**
     * 新增：提供给异步消费失败时的库存回滚补偿机制
     */
    public void compensateRedisState(Long userId, Long goodsId) {
        stringRedisTemplate.opsForValue().increment(STOCK_PREFIX + goodsId);
        stringRedisTemplate.delete(USER_ORDER_PREFIX + userId + ":" + goodsId);
        recordFail();
        stringRedisTemplate.opsForValue().decrement(METRIC_SUCCESS_KEY);
    }

    private void recordSuccess() { stringRedisTemplate.opsForValue().increment(METRIC_SUCCESS_KEY); }
    private void recordFail() { stringRedisTemplate.opsForValue().increment(METRIC_FAIL_KEY); }

    public List<SeckillOrder> listUserOrders(Long userId) {
        LambdaQueryWrapper<SeckillOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeckillOrder::getUserId, userId).orderByDesc(SeckillOrder::getCreateTime);
        return orderMapper.selectList(wrapper);
    }

    public SeckillOrder getUserOrderForGoods(Long userId, Long goodsId) {
        LambdaQueryWrapper<SeckillOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeckillOrder::getUserId, userId).eq(SeckillOrder::getGoodsId, goodsId);
        return orderMapper.selectOne(wrapper);
    }

    public boolean payOrder(Long orderId, Long userId) {
        return orderMapper.updateStatus(orderId, userId, OrderStatus.PAID.getCode(), OrderStatus.PENDING.getCode()) > 0;
    }

    // 修复：增加数据库和 Redis 的库存恢复逻辑
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long orderId, Long userId) {
        SeckillOrder order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId) || order.getStatus() != OrderStatus.PENDING.getCode()) {
            return false;
        }
        int n = orderMapper.updateStatus(orderId, userId, OrderStatus.CANCELLED.getCode(), OrderStatus.PENDING.getCode());
        if (n > 0) {
            goodsMapper.addStock(order.getGoodsId());
            stringRedisTemplate.opsForValue().increment(STOCK_PREFIX + order.getGoodsId());
            stringRedisTemplate.delete(USER_ORDER_PREFIX + userId + ":" + order.getGoodsId());
            return true;
        }
        return false;
    }

    public SeckillMetricsVO getMetrics() {
        SeckillMetricsVO vo = new SeckillMetricsVO();
        Long total = getLongFromRedis(METRIC_TOTAL_KEY);
        Long success = getLongFromRedis(METRIC_SUCCESS_KEY);
        Long fail = getLongFromRedis(METRIC_FAIL_KEY);

        vo.setTotalRequests(total); vo.setSuccessCount(success); vo.setFailCount(fail);
        vo.setSuccessRate((total != null && total > 0) ? (success * 100.0 / total) : 0.0);
        return vo;
    }

    private Long getLongFromRedis(String key) {
        String val = stringRedisTemplate.opsForValue().get(key);
        return val != null ? Long.parseLong(val) : 0L;
    }
}