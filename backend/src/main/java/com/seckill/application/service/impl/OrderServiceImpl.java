package com.seckill.application.service.impl;

import com.seckill.common.ErrorCode;
import com.seckill.common.exception.BusinessException;
import com.seckill.domain.entity.SeckillOrder;
import com.seckill.application.service.OrderService;
import com.seckill.persistence.mapper.SeckillGoodsMapper;
import com.seckill.persistence.mapper.SeckillOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private SeckillGoodsMapper goodsMapper;
    @Autowired
    private SeckillOrderMapper orderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(Long userId, Long goodsId) {
        int dbAffected = goodsMapper.reduceStock(goodsId);
        if (dbAffected == 0) {
            throw new BusinessException(ErrorCode.STOCK_SOLD_OUT, "数据库库存不足");
        }
        try {
            SeckillOrder order = new SeckillOrder();
            order.setUserId(userId);
            order.setGoodsId(goodsId);
            order.setStatus(0);
            orderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.ALREADY_ORDERED);
        }
    }
}
