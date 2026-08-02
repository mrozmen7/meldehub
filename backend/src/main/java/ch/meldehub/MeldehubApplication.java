package ch.meldehub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // Outbox relay'in @Scheduled polling'i için (CASE-252)
public class MeldehubApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeldehubApplication.class, args);
    }
}
