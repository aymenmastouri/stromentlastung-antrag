package de.stromentlastung.antrag.vorgang.dto;

import de.stromentlastung.antrag.vorgang.EntscheidungVorschlag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Der Entscheidungsvorschlag der Sachbearbeitung: Festsetzung oder Ablehnung mit Grundcode. */
public record EntscheidungAnlage(@NotNull EntscheidungVorschlag vorschlag, @Size(max = 60) String grund, @Size(max = 4000) String begruendung) {
}
