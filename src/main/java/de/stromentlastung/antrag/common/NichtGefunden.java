package de.stromentlastung.antrag.common;

/** Das Angefragte gibt es nicht — oder nicht für diesen Mandanten. */
public class NichtGefunden extends RuntimeException {

    public NichtGefunden(String message) {
        super(message);
    }
}
