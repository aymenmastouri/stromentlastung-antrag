package de.stromentlastung.antrag.vorgang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Die nach der Prüfung korrigierten Mengen; leere Felder behalten den Antragswert. */
public record AenderungAnlage(@PositiveOrZero Long versteuertKwh, @PositiveOrZero Long elektromobilitaetKwh,
                              @PositiveOrZero Long nutzenergieDritteKwh, @NotBlank @Size(max = 4000) String begruendung) {
}
