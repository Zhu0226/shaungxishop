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
        if (msg == null || msg.getUserId() == null || msg.getGoodsId() == null) {
            return;
        }
        // 补偿前必须校验数据库：消息进 DLQ 可能是 ACK 超时，订单或已落库，盲目补偿会导致超卖
        var order = seckillService.getUserOrderForGoods(msg.getUserId(), msg.getGoodsId());
        if (order != null) {
            log.info("[SECKILL_ORDER_DLQ] 订单已落库，无需补偿: userId={} goodsId={}", msg.getUserId(), msg.getGoodsId());
            return;
        }
        log.error("[SECKILL_ORDER_DLQ] userId={} goodsId={} createTime={} | 订单未落库，执行补偿",
                msg.getUserId(), msg.getGoodsId(), msg.getCreateTime());
        seckillService.compensateRedisState(msg.getUserId(), msg.getGoodsId());
    }
}
