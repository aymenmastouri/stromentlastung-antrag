package de.stromentlastung.antrag.vorgang.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** Auszahlung (Betrag ist die Festsetzung) oder Rückzahlung (Betrag angegeben). */
public record ZahlungErfassung(Long betragCent, LocalDate wertstellung, @NotBlank String zahlungsweg) {

    public long betragOder(long standard) {
        return betragCent != null && betragCent > 0 ? betragCent : standard;
    }
}
