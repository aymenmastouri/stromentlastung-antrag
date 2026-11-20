package de.stromentlastung.antrag.vorgang;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Die Rolle „System“ des Fachkonzepts: kennzeichnet Vorgänge nach Ablauf der Änderungsfrist als bestandskräftig (F-02). */
@Component
public class BestandskraftLauf {

    private static final Logger log = LoggerFactory.getLogger(BestandskraftLauf.class);

    private final AntragService antraege;

    public BestandskraftLauf(AntragService antraege) {
        this.antraege = antraege;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 5 0 * * *", zone = "Europe/Berlin")
    public void lauf() {
        int anzahl = antraege.bestandskraftPruefen();
        if (anzahl > 0) {
            log.info("{} Vorgänge bestandskräftig", anzahl);
        }
    }
}
