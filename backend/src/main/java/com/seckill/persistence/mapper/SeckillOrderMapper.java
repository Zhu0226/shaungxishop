package com.seckill.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.domain.entity.SeckillOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SeckillOrderMapper extends BaseMapper<SeckillOrder> {

    @Update("UPDATE seckill_order SET status = #{status}, update_time = NOW() WHERE id = #{orderId} AND user_id = #{userId} AND status = #{expectedStatus}")
    int updateStatus(@Param("orderId") Long orderId, @Param("userId") Long userId,
                     @Param("status") Integer status, @Param("expectedStatus") Integer expectedStatus);
}
