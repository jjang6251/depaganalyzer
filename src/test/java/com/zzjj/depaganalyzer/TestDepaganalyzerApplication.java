package com.zzjj.depaganalyzer;

import org.springframework.boot.SpringApplication;

public class TestDepaganalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.from(DepaganalyzerApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
