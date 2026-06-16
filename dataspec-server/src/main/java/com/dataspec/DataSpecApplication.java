package com.dataspec;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DataSpec 数标系统 —— AI 编程时代的数据字段标准系统
 */

@SpringBootApplication
@MapperScan("com.dataspec.**.mapper")
public class DataSpecApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataSpecApplication.class, args);
    }
}
