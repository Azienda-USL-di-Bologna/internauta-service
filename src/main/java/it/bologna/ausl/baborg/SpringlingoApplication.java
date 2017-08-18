package it.bologna.ausl.baborg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(scanBasePackages = {"it.nextsw", "it.bologna.ausl"} )
//@ComponentScan(basePackages = )
public class SpringlingoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringlingoApplication.class, args);
    }
}
