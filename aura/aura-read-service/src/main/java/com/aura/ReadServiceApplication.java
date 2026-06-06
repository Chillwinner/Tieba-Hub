package com.aura;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/** 读服务启动类（CQRS 读端） */
@SpringBootApplication
@EnableDiscoveryClient
public class ReadServiceApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(ReadServiceApplication.class, args);
    }
}
