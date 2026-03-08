package com.seckill.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SeckillStatusVO {
    private Integer status;
    private String statusDesc;
    private LocalDateTime now;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer remainStock;
}
