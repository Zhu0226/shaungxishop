package com.seckill.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("seckill_goods")
public class SeckillGoods {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String goodsName;
    private BigDecimal price;
    private Integer stock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer onShelf;
    private String activityTag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
