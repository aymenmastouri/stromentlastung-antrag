package de.stromentlastung.antrag.vorgang.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record NachweisAnlage(@NotBlank @Size(max = 200) String versorger, @NotNull LocalDate zeitraumVon, @NotNull LocalDate zeitraumBis,
                             @Positive long mengeKwh, @NotBlank @Size(max = 200) String verwendungszweck, @Size(max = 60) String belegnummer) {
}
