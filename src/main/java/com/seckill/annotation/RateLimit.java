package com.seckill.annotation;
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int time() default 5; // 限制时间，默认5秒
    int count() default 5; // 限制次数，默认5次
}