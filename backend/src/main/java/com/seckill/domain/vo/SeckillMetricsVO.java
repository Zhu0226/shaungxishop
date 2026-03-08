package com.seckill.domain.vo;

import lombok.Data;

@Data
public class SeckillMetricsVO {
    private Long totalRequests;
    private Long successCount;
    private Long failCount;
    private Double successRate;
}
