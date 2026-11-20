package de.stromentlastung.antrag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Vorgang: Antrag, Rechenkern, Fristen, Zustände, Protokoll. */
@SpringBootApplication
@EnableScheduling
public class AntragApplication {

    public static void main(String[] args) {
        SpringApplication.run(AntragApplication.class, args);
    }
}
