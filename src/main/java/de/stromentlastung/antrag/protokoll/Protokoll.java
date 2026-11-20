package de.stromentlastung.antrag.protokoll;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

/** Schreibt Vorgangsereignisse; die Fachlogik ruft nur diese Fassade. */
@Component
public class Protokoll {

    public static final String SYSTEM = "System";

    private final VorgangsereignisRepository ereignisse;
    private final Clock clock;

    public Protokoll(VorgangsereignisRepository ereignisse, Clock clock) {
        this.ereignisse = ereignisse;
        this.clock = clock;
    }

    public Vorgangsereignis schreibe(String aktenzeichen, String akteur, String rolle, EreignisArt art, String vorher, String nachher,
                                     String bemerkung, String bescheidKennung, String zahlungKennung, boolean intern) {
        return ereignisse.save(new Vorgangsereignis(aktenzeichen, Instant.now(clock), akteur, rolle, art, vorher, nachher,
                bemerkung, bescheidKennung, zahlungKennung, intern));
    }

    public List<Vorgangsereignis> alle(String aktenzeichen) {
        return ereignisse.findByAktenzeichenOrderByZeitpunktAscIdAsc(aktenzeichen);
    }

    public List<Vorgangsereignis> oeffentliche(String aktenzeichen) {
        return ereignisse.findByAktenzeichenAndInternFalseOrderByZeitpunktAscIdAsc(aktenzeichen);
    }
}
