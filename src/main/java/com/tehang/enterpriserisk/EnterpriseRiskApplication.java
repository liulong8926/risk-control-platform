package com.tehang.enterpriserisk;

import org.springframework.boot.SpringApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableScheduling
public class EnterpriseRiskApplication {
  public static void main(String[] args) { SpringApplication.run(EnterpriseRiskApplication.class, args); }
}
