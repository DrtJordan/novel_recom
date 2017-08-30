package com.sohu.mrd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by yonghongli on 2017/4/1.
 */
@SpringBootApplication
public class NovelRecomApplication {
    public static void main(String[] args) {
        String logPath = System.getProperty("logPath");
        if (logPath == null || logPath.isEmpty()) {
            System.setProperty("logPath", "logs");
        }

        SpringApplication.run(NovelRecomApplication.class, args);
    }
}
