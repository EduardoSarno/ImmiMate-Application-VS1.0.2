package co.immimate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "co.immimate.auth",
    "co.immimate.config",
    "co.immimate.health",
    "co.immimate.profile",
    "co.immimate.user",
    "co.immimate.scoringevaluations"
})
public class ImmiMateApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImmiMateApplication.class, args);
    }
} 