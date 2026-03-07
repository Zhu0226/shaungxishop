-- ============================================================
-- 初始化测试数据（可选）
-- ============================================================

USE flash_sale;

-- 插入测试秒杀商品
INSERT INTO seckill_goods (goods_name, price, stock, start_time, end_time, on_shelf, activity_tag)
VALUES
  ('限时秒杀-手机', 1999.00, 100, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 1, '限时'),
  ('限时秒杀-耳机', 99.00, 200, NOW(), DATE_ADD(NOW(), INTERVAL 3 DAY), 1, '爆款');
