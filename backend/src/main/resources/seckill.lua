-- KEYS[1]: 商品库存Key, 例如 seckill:stock:1
local stockKey = KEYS[1]
-- 判断Key是否存在
if (redis.call('exists', stockKey) == 1) then
    local stock = tonumber(redis.call('get', stockKey))
    if (stock > 0) then
        -- 扣减库存
        redis.call('decr', stockKey)
        return 1 -- 成功
    end
    return 0 -- 库存不足
end
return -1 -- 商品未预热或不存在