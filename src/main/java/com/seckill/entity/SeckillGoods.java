package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀商品实体类
 */
@Data
@TableName("seckill_goods") // 对应数据库中的秒杀商品表
public class SeckillGoods {

    @TableId(type = IdType.AUTO) // 主键自增
    private Long id;

    private String goodsName;

    private BigDecimal price;

    private Integer stock;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /**
     * 是否上架：0-下架，1-上架（仅上架商品在 C 端列表展示）
     */
    private Integer onShelf;

    /**
     * 活动标签，如「限时」「爆款」「新人专享」等
     */
    private String activityTag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}