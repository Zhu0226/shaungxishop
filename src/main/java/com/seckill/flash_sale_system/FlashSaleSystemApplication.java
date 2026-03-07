package com.seckill.flash_sale_system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.seckill")
@MapperScan("com.seckill.mapper")
public class FlashSaleSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashSaleSystemApplication.class, args);
    }

}