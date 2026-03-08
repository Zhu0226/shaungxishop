package com.seckill.application.service;

import com.seckill.domain.entity.SeckillGoods;
import com.seckill.domain.entity.SeckillOrder;
import com.seckill.domain.vo.SeckillMetricsVO;
import com.seckill.domain.vo.SeckillStatusVO;

import java.util.List;

public interface SeckillService {

    void preHeatStock(Long goodsId, int stock);
    List<SeckillGoods> listSeckillGoods();
    List<SeckillGoods> listAllGoodsForAdmin();
    SeckillGoods getGoodsDetail(Long goodsId);
    void saveGoods(SeckillGoods goods);
    void updateGoods(SeckillGoods goods);
    void removeGoods(Long id);
    SeckillStatusVO getSeckillStatus(Long goodsId);
    String createSeckillPath(Long userId, Long goodsId);
    String executeSeckill(Long userId, Long goodsId, String path);
    void compensateRedisState(Long userId, Long goodsId);
    List<SeckillOrder> listUserOrders(Long userId);
    SeckillOrder getUserOrderForGoods(Long userId, Long goodsId);
    boolean payOrder(Long orderId, Long userId);
    boolean cancelOrder(Long orderId, Long userId);
    SeckillMetricsVO getMetrics();
}
