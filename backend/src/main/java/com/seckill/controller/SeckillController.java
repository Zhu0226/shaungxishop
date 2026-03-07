package com.seckill.controller;

import com.seckill.annotation.RateLimit;
import com.seckill.common.ApiResponse;
import com.seckill.common.ErrorCode;
import com.seckill.entity.SeckillGoods;
import com.seckill.entity.SeckillOrder;
import com.seckill.service.impl.SeckillService;
import com.seckill.vo.SeckillMetricsVO;
import com.seckill.vo.SeckillStatusVO;
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

    /**
     * 秒杀商品列表（活动页展示）
     */
    @GetMapping("/goods")
    public ApiResponse<List<SeckillGoods>> listGoods() {
        List<SeckillGoods> goodsList = seckillService.listSeckillGoods();
        return ApiResponse.success(goodsList);
    }

    /**
     * 秒杀商品详情（含时间窗口信息）
     */
    @GetMapping("/goods/{goodsId}")
    public ApiResponse<SeckillGoods> goodsDetail(@PathVariable Long goodsId) {
        SeckillGoods goods = seckillService.getGoodsDetail(goodsId);
        if (goods == null) {
            return ApiResponse.error(404, "商品不存在");
        }
        return ApiResponse.success(goods);
    }

    /**
     * 查询指定商品的秒杀活动状态（未开始 / 进行中 / 已结束 / 已售罄）
     */
    @GetMapping("/status/{goodsId}")
    public ApiResponse<SeckillStatusVO> status(@PathVariable Long goodsId) {
        SeckillStatusVO status = seckillService.getSeckillStatus(goodsId);
        return ApiResponse.success(status);
    }

    /**
     * 库存预热接口（通常供运营/后台调用）。校验：商品存在、库存>0、不超过DB库存。
     */
    @PostMapping("/preheat/{goodsId}/{stock}")
    public ApiResponse<String> preheat(@PathVariable @Min(1) Long goodsId,
                                       @PathVariable @Min(1) int stock) {
        seckillService.preHeatStock(goodsId, stock);
        return ApiResponse.success("库存预热成功", null);
    }

    /**
     * 获取动态秒杀路径（隐藏真实秒杀接口，带限流）
     */
    @RateLimit(time = 5, count = 3)
    @GetMapping("/path")
    public ApiResponse<String> getPath(@RequestHeader("userId") Long userId, @RequestParam Long goodsId) {
        String path = seckillService.createSeckillPath(userId, goodsId);
        return ApiResponse.success(path);
    }

    /**
     * 执行秒杀（核心入口）。失败时由 BusinessException 经全局异常处理器返回。
     */
    @PostMapping("/{path}/doSeckill")
    public ApiResponse<String> doSeckill(@RequestHeader("userId") Long userId,
                                         @RequestParam Long goodsId,
                                         @PathVariable("path") String path) {
        String result = seckillService.executeSeckill(userId, goodsId, path);
        return ApiResponse.success(result);
    }

    /**
     * 查询当前用户的所有秒杀订单
     */
    @GetMapping("/orders")
    public ApiResponse<List<SeckillOrder>> userOrders(@RequestHeader("userId") Long userId) {
        List<SeckillOrder> orders = seckillService.listUserOrders(userId);
        return ApiResponse.success(orders);
    }

    /**
     * 查询当前用户某个商品的秒杀结果（有没有抢到，订单详情）
     */
    @GetMapping("/orders/result")
    public ApiResponse<SeckillOrder> seckillResult(@RequestHeader("userId") Long userId,
                                                   @RequestParam Long goodsId) {
        SeckillOrder order = seckillService.getUserOrderForGoods(userId, goodsId);
        if (order == null) {
            return ApiResponse.error(ErrorCode.NOT_FOUND, "尚未抢到该商品或订单不存在");
        }
        return ApiResponse.success(order);
    }

    /**
     * 订单支付（状态 0→1，仅本人待支付订单可支付）
     */
    @PostMapping("/orders/{orderId}/pay")
    public ApiResponse<String> payOrder(@RequestHeader("userId") Long userId, @PathVariable Long orderId) {
        boolean ok = seckillService.payOrder(orderId, userId);
        if (!ok) {
            return ApiResponse.error(ErrorCode.ORDER_NOT_FOUND, "订单不存在、非本人或状态不可支付");
        }
        return ApiResponse.success("支付成功");
    }

    /**
     * 订单取消（状态 0→2，仅本人待支付订单可取消）
     */
    @PostMapping("/orders/{orderId}/cancel")
    public ApiResponse<String> cancelOrder(@RequestHeader("userId") Long userId, @PathVariable Long orderId) {
        boolean ok = seckillService.cancelOrder(orderId, userId);
        if (!ok) {
            return ApiResponse.error(ErrorCode.ORDER_NOT_FOUND, "订单不存在、非本人或状态不可取消");
        }
        return ApiResponse.success("已取消");
    }

    /**
     * 简单秒杀指标查询接口（供压测/运维查看）
     */
    @GetMapping("/metrics")
    public ApiResponse<SeckillMetricsVO> metrics() {
        SeckillMetricsVO metrics = seckillService.getMetrics();
        return ApiResponse.success(metrics);
    }
}