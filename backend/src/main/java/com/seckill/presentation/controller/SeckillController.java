package com.seckill.presentation.controller;

import com.seckill.application.service.SeckillService;
import com.seckill.common.ApiResponse;
import com.seckill.common.ErrorCode;
import com.seckill.domain.entity.SeckillGoods;
import com.seckill.domain.entity.SeckillOrder;
import com.seckill.domain.vo.SeckillMetricsVO;
import com.seckill.domain.vo.SeckillStatusVO;
import com.seckill.common.annotation.RateLimit;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seckill")
@Validated
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    @GetMapping("/goods")
    public ApiResponse<List<SeckillGoods>> listGoods() {
        List<SeckillGoods> goodsList = seckillService.listSeckillGoods();
        return ApiResponse.success(goodsList);
    }

    @GetMapping("/goods/{goodsId}")
    public ApiResponse<SeckillGoods> goodsDetail(@PathVariable Long goodsId) {
        SeckillGoods goods = seckillService.getGoodsDetail(goodsId);
        if (goods == null) {
            return ApiResponse.error(404, "商品不存在");
        }
        return ApiResponse.success(goods);
    }

    @GetMapping("/status/{goodsId}")
    public ApiResponse<SeckillStatusVO> status(@PathVariable Long goodsId) {
        SeckillStatusVO status = seckillService.getSeckillStatus(goodsId);
        return ApiResponse.success(status);
    }

    @PostMapping("/preheat/{goodsId}/{stock}")
    public ApiResponse<String> preheat(@PathVariable @Min(1) Long goodsId,
                                       @PathVariable @Min(1) int stock) {
        seckillService.preHeatStock(goodsId, stock);
        return ApiResponse.success("库存预热成功", null);
    }

    @RateLimit(time = 5, count = 3)
    @GetMapping("/path")
    public ApiResponse<String> getPath(@RequestHeader("userId") Long userId, @RequestParam Long goodsId) {
        String path = seckillService.createSeckillPath(userId, goodsId);
        return ApiResponse.success(path);
    }

    @PostMapping("/{path}/doSeckill")
    public ApiResponse<String> doSeckill(@RequestHeader("userId") Long userId,
                                         @RequestParam Long goodsId,
                                         @PathVariable("path") String path) {
        String result = seckillService.executeSeckill(userId, goodsId, path);
        return ApiResponse.success(result);
    }

    @GetMapping("/orders")
    public ApiResponse<List<SeckillOrder>> userOrders(@RequestHeader("userId") Long userId) {
        List<SeckillOrder> orders = seckillService.listUserOrders(userId);
        return ApiResponse.success(orders);
    }

    @GetMapping("/orders/result")
    public ApiResponse<SeckillOrder> seckillResult(@RequestHeader("userId") Long userId,
                                                   @RequestParam Long goodsId) {
        SeckillOrder order = seckillService.getUserOrderForGoods(userId, goodsId);
        if (order == null) {
            return ApiResponse.error(ErrorCode.NOT_FOUND, "尚未抢到该商品或订单不存在");
        }
        return ApiResponse.success(order);
    }

    @PostMapping("/orders/{orderId}/pay")
    public ApiResponse<String> payOrder(@RequestHeader("userId") Long userId, @PathVariable Long orderId) {
        boolean ok = seckillService.payOrder(orderId, userId);
        if (!ok) {
            return ApiResponse.error(ErrorCode.ORDER_NOT_FOUND, "订单不存在、非本人或状态不可支付");
        }
        return ApiResponse.success("支付成功");
    }

    @PostMapping("/orders/{orderId}/cancel")
    public ApiResponse<String> cancelOrder(@RequestHeader("userId") Long userId, @PathVariable Long orderId) {
        boolean ok = seckillService.cancelOrder(orderId, userId);
        if (!ok) {
            return ApiResponse.error(ErrorCode.ORDER_NOT_FOUND, "订单不存在、非本人或状态不可取消");
        }
        return ApiResponse.success("已取消");
    }

    @GetMapping("/metrics")
    public ApiResponse<SeckillMetricsVO> metrics() {
        SeckillMetricsVO metrics = seckillService.getMetrics();
        return ApiResponse.success(metrics);
    }
}
