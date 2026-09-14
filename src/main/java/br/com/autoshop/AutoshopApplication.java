package br.com.autoshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = "br.com.autoshop.model")
public class AutoshopApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoshopApplication.class, args);
    }

}
