package com.seckill;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.seckill.persistence.mapper")
public class FlashSaleSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashSaleSystemApplication.class, args);
    }
}
