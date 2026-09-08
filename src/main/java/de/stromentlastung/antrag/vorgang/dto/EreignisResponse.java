package de.stromentlastung.antrag.vorgang.dto;

import java.time.Instant;

import de.stromentlastung.antrag.protokoll.EreignisArt;

public record EreignisResponse(Long id, Instant zeitpunkt, String akteur, String rolle, EreignisArt art, String zustandVorher,
                               String zustandNachher, String bemerkung, String bescheidKennung, String zahlungKennung, boolean intern) {
}
