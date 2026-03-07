package com.seckill.common;

import lombok.Getter;

/**
 * 统一错误码枚举，便于前后端对接与运维排查
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "OK"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    TOO_MANY_REQUESTS(429, "请求太频繁，请稍后再试"),
    INTERNAL_ERROR(500, "系统繁忙，请稍后重试"),

    // 业务错误 1xxx
    GOODS_NOT_FOUND(1001, "商品不存在或活动未配置"),
    ACTIVITY_NOT_STARTED(1002, "秒杀活动尚未开始"),
    ACTIVITY_ENDED(1003, "秒杀活动已结束"),
    INVALID_PATH(1004, "非法请求或链接已失效"),
    ALREADY_ORDERED(1005, "您已经抢购过该商品了"),
    STOCK_SOLD_OUT(1006, "很遗憾，商品已售罄"),
    SECKILL_ERROR(1007, "秒杀活动异常"),
    PREHEAT_GOODS_NOT_FOUND(1008, "商品不存在，无法预热"),
    PREHEAT_STOCK_INVALID(1009, "预热库存必须大于0且不超过数据库库存"),
    ORDER_NOT_FOUND(1010, "订单不存在、非本人或状态不可操作");

    private final int code;
    private final String defaultMsg;

    ErrorCode(int code, String defaultMsg) {
        this.code = code;
        this.defaultMsg = defaultMsg;
    }
}
