package com.seckill.infrastructure.mq;

import com.seckill.application.service.OrderService;
import com.seckill.infrastructure.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 秒杀订单 MQ 消费者。修复：不在 catch 中补偿，避免重试导致无限加库存；补偿仅在 DLQ 单次执行。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "seckill.order.async", havingValue = "true")
public class SeckillOrderConsumer {

    @Autowired
    private OrderService orderService;

    @RabbitListener(queues = RabbitMQConfig.SECKILL_ORDER_QUEUE)
    public void onMessage(SeckillOrderMessage msg) {
        if (msg == null || msg.getUserId() == null || msg.getGoodsId() == null) {
            log.warn("SeckillOrderMessage invalid: {}", msg);
            return;
        }
        orderService.createOrder(msg.getUserId(), msg.getGoodsId());
        log.info("Seckill order created async: userId={}, goodsId={}", msg.getUserId(), msg.getGoodsId());
    }
}
