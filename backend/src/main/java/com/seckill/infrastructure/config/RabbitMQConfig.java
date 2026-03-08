package com.seckill.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String SECKILL_ORDER_EXCHANGE = "seckill.order.exchange";
    public static final String SECKILL_ORDER_QUEUE = "seckill.order.queue";
    public static final String SECKILL_ORDER_ROUTING_KEY = "seckill.order";
    public static final String SECKILL_ORDER_DLX = "seckill.order.dlx";
    public static final String SECKILL_ORDER_DLQ = "seckill.order.dlq";
    public static final String SECKILL_ORDER_DLQ_ROUTING_KEY = "seckill.order.dlq";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public DirectExchange seckillOrderExchange() {
        return new DirectExchange(SECKILL_ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable(SECKILL_ORDER_QUEUE)
                .withArgument("x-dead-letter-exchange", SECKILL_ORDER_DLX)
                .withArgument("x-dead-letter-routing-key", SECKILL_ORDER_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding seckillOrderBinding(Queue seckillOrderQueue, DirectExchange seckillOrderExchange) {
        return BindingBuilder.bind(seckillOrderQueue).to(seckillOrderExchange).with(SECKILL_ORDER_ROUTING_KEY);
    }

    @Bean
    public DirectExchange seckillOrderDlx() {
        return new DirectExchange(SECKILL_ORDER_DLX, true, false);
    }

    @Bean
    public Queue seckillOrderDlq() {
        return QueueBuilder.durable(SECKILL_ORDER_DLQ).build();
    }

    @Bean
    public Binding seckillOrderDlqBinding(Queue seckillOrderDlq, DirectExchange seckillOrderDlx) {
        return BindingBuilder.bind(seckillOrderDlq).to(seckillOrderDlx).with(SECKILL_ORDER_DLQ_ROUTING_KEY);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                              MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
