package com.aura;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/** 写服务启动类（CQRS 写端） */
@SpringBootApplication
@EnableDiscoveryClient
public class WriteServiceApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(WriteServiceApplication.class, args);
    }
}
