package com.seckill.mq;

import com.seckill.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 秒杀订单消息生产者：在 Redis+DB 扣减成功后发送消息，由消费者异步落库。
 */
@Component
@ConditionalOnProperty(name = "seckill.order.async", havingValue = "true")
public class SeckillOrderProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void send(Long userId, Long goodsId) {
        SeckillOrderMessage msg = new SeckillOrderMessage(userId, goodsId, LocalDateTime.now());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECKILL_ORDER_EXCHANGE,
                RabbitMQConfig.SECKILL_ORDER_ROUTING_KEY,
                msg
        );
    }
}
