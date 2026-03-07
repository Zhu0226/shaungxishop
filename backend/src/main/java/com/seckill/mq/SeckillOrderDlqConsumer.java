package com.seckill.mq;

import com.seckill.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 秒杀订单死信队列消费者：记录失败消息便于排查与告警，可对接 ELK/告警。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "seckill.order.async", havingValue = "true")
public class SeckillOrderDlqConsumer {

    @RabbitListener(queues = RabbitMQConfig.SECKILL_ORDER_DLQ)
    public void onDlqMessage(SeckillOrderMessage msg) {
        log.error("[SECKILL_ORDER_DLQ] userId={} goodsId={} createTime={} | 订单异步落库失败，已进入死信队列，请排查",
                msg != null ? msg.getUserId() : null,
                msg != null ? msg.getGoodsId() : null,
                msg != null ? msg.getCreateTime() : null);
    }
}
