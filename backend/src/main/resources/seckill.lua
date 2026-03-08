-- KEYS[1]: 商品库存Key, 例如 seckill:stock:1
-- 精简版：合并 exists/get/decr，减少 Redis 调用
local stockKey = KEYS[1]
local stock = redis.call('get', stockKey)
if (stock == false) then
    return -1  -- 键不存在/未预热
end
if (tonumber(stock) > 0) then
    redis.call('decr', stockKey)
    return 1   -- 成功
end
return 0       -- 库存不足
