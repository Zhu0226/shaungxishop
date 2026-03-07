package com.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.entity.SeckillOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 秒杀订单 Mapper 接口
 */
@Mapper
public interface SeckillOrderMapper extends BaseMapper<SeckillOrder> {

    /**
     * 按状态流转更新订单（支付/取消）
     */
    @Update("UPDATE seckill_order SET status = #{status}, update_time = NOW() WHERE id = #{orderId} AND user_id = #{userId} AND status = #{expectedStatus}")
    int updateStatus(@Param("orderId") Long orderId, @Param("userId") Long userId,
                     @Param("status") Integer status, @Param("expectedStatus") Integer expectedStatus);
}