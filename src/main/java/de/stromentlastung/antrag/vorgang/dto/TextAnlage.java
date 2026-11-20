package de.stromentlastung.antrag.vorgang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Rückfrage, Antwort, Zurückweisung, Prüfvermerk: ein Text. */
public record TextAnlage(@NotBlank @Size(max = 4000) String text) {
}
