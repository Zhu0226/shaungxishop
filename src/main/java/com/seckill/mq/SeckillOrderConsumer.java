package com.seckill.mq;

import com.seckill.config.RabbitMQConfig;
import com.seckill.service.impl.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "seckill.order.async", havingValue = "true")
public class SeckillOrderConsumer {

    @Autowired
    private SeckillService seckillService;

    @RabbitListener(queues = RabbitMQConfig.SECKILL_ORDER_QUEUE)
    public void onMessage(SeckillOrderMessage msg) {
        if (msg == null || msg.getUserId() == null || msg.getGoodsId() == null) {
            log.warn("SeckillOrderMessage invalid: {}", msg);
            return;
        }
        try {
            seckillService.createOrder(msg.getUserId(), msg.getGoodsId());
            log.info("Seckill order created async: userId={}, goodsId={}", msg.getUserId(), msg.getGoodsId());
        } catch (Exception e) {
            log.error("Seckill order consumer error, compensating Redis state: userId={}, goodsId={}", msg.getUserId(), msg.getGoodsId(), e);
            // 修复：触发补偿逻辑，回滚 Redis 库存和用户抢购标记
            seckillService.compensateRedisState(msg.getUserId(), msg.getGoodsId());
            throw e; // 抛出异常由 RabbitMQ 将消息放入死信队列
        }
    }
}