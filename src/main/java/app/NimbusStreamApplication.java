package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Entry point for Spring Boot.
 * Package layout is flat (config, controller, service  are siblings,
 * not nested under this class), so component/entity/repository scanning
 * is pointed explicitly at each package rather than relying on the
 * "scans everything below me" default.
 */

@SpringBootApplication
@ComponentScan(basePackages = {
        "config" , "controller" , "exception" , "mapper" , "service"
})
@EntityScan(basePackages = "model")
@EnableJpaRepositories(basePackages = "repository")
public class NimbusStreamApplication {
    public static void main(String[] args){
        SpringApplication.run(NimbusStreamApplication.class,args);
    }
}