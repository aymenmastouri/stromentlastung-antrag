package de.stromentlastung.antrag.vorgang.dto;

import java.time.LocalDate;

public record NachweisResponse(Long id, String versorger, LocalDate zeitraumVon, LocalDate zeitraumBis, long mengeKwh,
                               String verwendungszweck, String belegnummer, boolean geprueft) {
}
