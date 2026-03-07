package com.seckill.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 秒杀下单异步消息体：消费者据此落库订单，实现削峰与解耦。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long goodsId;
    /** 消息创建时间，便于排查与幂等 */
    private LocalDateTime createTime;
}
