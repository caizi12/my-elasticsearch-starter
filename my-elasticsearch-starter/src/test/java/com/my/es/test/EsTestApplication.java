package com.my.es.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 主启动类
 *
 * @author apelx
 * @since 2020/07/17
 */
@SpringBootApplication(scanBasePackages = {"com.my"})
public class EsTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(EsTestApplication.class, args);
    }

}
