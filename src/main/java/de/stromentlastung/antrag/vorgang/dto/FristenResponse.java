package de.stromentlastung.antrag.vorgang.dto;

import java.time.LocalDate;

public record FristenResponse(LocalDate antragsfristEnde, boolean antragsfristAbgelaufen, LocalDate aenderungsfristEnde,
                              LocalDate rueckfrageFristEnde, boolean rueckfrageFristAbgelaufen) {
}
