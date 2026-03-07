package com.seckill.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SeckillStatusVO {

    /**
     * 状态码：0-未开始，1-进行中，2-已结束，3-已售罄
     */
    private int status;

    /**
     * 状态描述
     */
    private String statusDesc;

    private LocalDateTime now;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /**
     * 剩余库存（优先读取 Redis，其次数据库）
     */
    private Integer remainStock;
}

