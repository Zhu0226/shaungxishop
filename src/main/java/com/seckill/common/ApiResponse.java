package com.seckill.common;

import lombok.Data;

@Data
public class ApiResponse<T> {

    /**
     * 业务状态码：0 表示成功，非 0 表示失败或异常
     */
    private int code;

    /**
     * 提示信息
     */
    private String msg;

    /**
     * 业务数据
     */
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setCode(0);
        resp.setMsg("OK");
        resp.setData(data);
        return resp;
    }

    public static <T> ApiResponse<T> success(String msg, T data) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setCode(0);
        resp.setMsg(msg);
        resp.setData(data);
        return resp;
    }

    public static <T> ApiResponse<T> error(int code, String msg) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setCode(code);
        resp.setMsg(msg);
        resp.setData(null);
        return resp;
    }

    public static <T> ApiResponse<T> error(String msg) {
        return error(-1, msg);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getDefaultMsg());
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String customMsg) {
        return error(errorCode.getCode(), customMsg != null ? customMsg : errorCode.getDefaultMsg());
    }
}

