package com.seckill.infrastructure.mq;

import com.seckill.application.service.SeckillService;
import com.seckill.infrastructure.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 死信队列消费者：单次执行补偿，保证幂等。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "seckill.order.async", havingValue = "true")
public class SeckillOrderDlqConsumer {

    @Autowired
    private SeckillService seckillService;

    @RabbitListener(queues = RabbitMQConfig.SECKILL_ORDER_DLQ)
    public void onDlqMessage(SeckillOrderMessage msg) {
        log.error("[SECKILL_ORDER_DLQ] userId={} goodsId={} createTime={} | 订单异步落库失败，执行补偿",
                msg != null ? msg.getUserId() : null,
                msg != null ? msg.getGoodsId() : null,
                msg != null ? msg.getCreateTime() : null);
        if (msg != null && msg.getUserId() != null && msg.getGoodsId() != null) {
            seckillService.compensateRedisState(msg.getUserId(), msg.getGoodsId());
        }
    }
}
