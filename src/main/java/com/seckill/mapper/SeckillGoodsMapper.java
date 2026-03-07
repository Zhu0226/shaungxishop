package com.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.entity.SeckillGoods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SeckillGoodsMapper extends BaseMapper<SeckillGoods> {

    @Update("UPDATE seckill_goods SET stock = stock - 1 WHERE id = #{goodsId} AND stock > 0")
    int reduceStock(@Param("goodsId") Long goodsId);

    // 新增：恢复真实库存
    @Update("UPDATE seckill_goods SET stock = stock + 1 WHERE id = #{goodsId}")
    int addStock(@Param("goodsId") Long goodsId);
}