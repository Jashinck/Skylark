package org.skylark.bluewhale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BlueWhaleApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlueWhaleApplication.class, args);
    }
}
