-- ============================================================
-- 若表已存在但缺少新增字段，执行此脚本补充
-- (适用于从旧版本升级)
-- 若某列已存在会报错 Duplicate column，可忽略该行
-- ============================================================

USE flash_sale;

-- seckill_goods 补充 on_shelf、activity_tag
ALTER TABLE seckill_goods ADD COLUMN on_shelf TINYINT NOT NULL DEFAULT 1 COMMENT '0下架 1上架';
ALTER TABLE seckill_goods ADD COLUMN activity_tag VARCHAR(64) DEFAULT '' COMMENT '活动标签';

-- seckill_order 补充 update_time
ALTER TABLE seckill_order ADD COLUMN update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
