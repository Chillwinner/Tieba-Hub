package com.aura;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 启动类，启用 RabbitMQ 注解驱动 */
@EnableRabbit
@SpringBootApplication
public class AuraApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(AuraApplication.class, args);
    }
}
