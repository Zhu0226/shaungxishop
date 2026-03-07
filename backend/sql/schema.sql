-- ============================================================
-- 高并发限时秒杀系统 - 数据库建表脚本
-- 与实体类 SeckillGoods、SeckillOrder 一一对应
-- ============================================================

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS flash_sale
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE flash_sale;

-- ------------------------------------------------------------
-- 2. 秒杀商品表 (对应 SeckillGoods)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS seckill_goods (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    goods_name      VARCHAR(255)    NOT NULL                    COMMENT '商品名称',
    price           DECIMAL(10,2)   NOT NULL                    COMMENT '价格',
    stock           INT             NOT NULL                    COMMENT '库存',
    start_time      DATETIME        NOT NULL                    COMMENT '活动开始时间',
    end_time        DATETIME        NOT NULL                    COMMENT '活动结束时间',
    on_shelf        TINYINT         NOT NULL DEFAULT 1          COMMENT '0下架 1上架',
    activity_tag    VARCHAR(64)     DEFAULT ''                  COMMENT '活动标签',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀商品表';

-- ------------------------------------------------------------
-- 3. 秒杀订单表 (对应 SeckillOrder)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS seckill_order (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id         BIGINT          NOT NULL                    COMMENT '用户ID',
    goods_id        BIGINT          NOT NULL                    COMMENT '商品ID',
    status          TINYINT         NOT NULL DEFAULT 0          COMMENT '0待支付 1已支付 2已取消',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    update_time     DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_goods (user_id, goods_id)                COMMENT '一人一单唯一约束'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单表';
