package de.stromentlastung.antrag.vorgang.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

/** Die Angaben des Antragstellers (Fachkonzept Kap. 4, Strommengen und Versicherungen). */
public record AntragAnlage(@Min(2000) @Max(2100) int entnahmejahr, @PositiveOrZero long versteuertKwh,
                           @PositiveOrZero long elektromobilitaetKwh, boolean elektromobilitaetGeschaetzt,
                           @PositiveOrZero long nutzenergieDritteKwh, boolean versicherungKeineRueckforderung,
                           boolean versicherungKeineSchwierigkeiten, boolean selbsterklaerungBeihilfen, Long selbstberechnetCent) {
}
