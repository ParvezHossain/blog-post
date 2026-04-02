package com.parvez.blogs.config;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {

    @Bean
    public HikariDataSource dataSource(DataSourceProperties properties) {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        int diskIo = 4;
        int poolSize = (cpuCores * 2) + diskIo;

        HikariDataSource dataSource =
                properties.initializeDataSourceBuilder()
                        .type(HikariDataSource.class)
                        .build();

        dataSource.setMaximumPoolSize(poolSize);

        System.out.println("Calculated pool size: " + poolSize);

        return dataSource;
    }
}