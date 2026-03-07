package com.seckill.enums;

import lombok.Getter;

/**
 * 订单状态机：仅允许 0→1（支付）、0→2（取消）。
 */
@Getter
public enum OrderStatus {

    PENDING(0, "待支付"),
    PAID(1, "已支付"),
    CANCELLED(2, "已取消");

    private final int code;
    private final String desc;

    OrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static OrderStatus of(int code) {
        for (OrderStatus s : values()) {
            if (s.code == code) return s;
        }
        return null;
    }
}
