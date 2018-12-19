package com.xxlllq.springboot_activiti;

import com.xxlllq.springboot_activiti.modeler.explorer.JsonpCallbackFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@ComponentScan({"org.activiti.rest.diagram", "com.xxlllq"})
@EnableAsync
@SpringBootApplication(exclude = org.activiti.spring.boot.SecurityAutoConfiguration.class)
public class SpringbootActivitiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootActivitiApplication.class, args);
    }

    @Bean
    public JsonpCallbackFilter filter(){
        return new JsonpCallbackFilter();
    }
}

