package de.stromentlastung.antrag.vorgang.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

public record VorschauAnfrage(@Min(2000) @Max(2100) int entnahmejahr, @PositiveOrZero long versteuertKwh,
                              @PositiveOrZero long elektromobilitaetKwh, boolean elektromobilitaetGeschaetzt,
                              @PositiveOrZero long nutzenergieDritteKwh, Long selbstberechnetCent) {
}
