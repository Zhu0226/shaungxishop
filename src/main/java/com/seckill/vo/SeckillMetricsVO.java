package com.seckill.vo;

import lombok.Data;

@Data
public class SeckillMetricsVO {

    /**
     * 秒杀接口总请求次数
     */
    private long totalRequests;

    /**
     * 成功下单次数
     */
    private long successCount;

    /**
     * 失败次数（库存不足、活动结束、非法请求等）
     */
    private long failCount;

    /**
     * 成功率（0-100，单位：百分比）
     */
    private double successRate;
}

