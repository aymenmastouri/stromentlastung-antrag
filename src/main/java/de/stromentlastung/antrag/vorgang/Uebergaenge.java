package de.stromentlastung.antrag.vorgang;

import static de.stromentlastung.antrag.vorgang.AntragZustand.*;

import java.util.Map;
import java.util.Set;

import de.stromentlastung.antrag.common.RegelVerletzung;

/** Die zulässigen Zustandsübergänge (Fachkonzept Kap. 5); alles andere ist eine Regelverletzung. */
public final class Uebergaenge {

    private static final Map<AntragZustand, Set<AntragZustand>> ZULAESSIG = Map.ofEntries(
            Map.entry(ENTWURF, Set.of(EINGEREICHT, ZURUECKGENOMMEN)),
            Map.entry(EINGEREICHT, Set.of(IN_BEARBEITUNG, ZURUECKGENOMMEN)),
            Map.entry(IN_BEARBEITUNG, Set.of(RUECKFRAGE, ZUR_FREIGABE)),
            Map.entry(RUECKFRAGE, Set.of(IN_BEARBEITUNG, ZURUECKGENOMMEN)),
            Map.entry(ZUR_FREIGABE, Set.of(IN_BEARBEITUNG, FESTGESETZT, ABGELEHNT)),
            Map.entry(FESTGESETZT, Set.of(AUSGEZAHLT)),
            Map.entry(AUSGEZAHLT, Set.of(IN_AENDERUNG, BESTANDSKRAEFTIG)),
            Map.entry(IN_AENDERUNG, Set.of(AENDERUNG_ZUR_FREIGABE)),
            Map.entry(AENDERUNG_ZUR_FREIGABE, Set.of(IN_AENDERUNG, RUECKFORDERUNG_OFFEN)),
            Map.entry(RUECKFORDERUNG_OFFEN, Set.of(BEGLICHEN)),
            Map.entry(ABGELEHNT, Set.of()), Map.entry(ZURUECKGENOMMEN, Set.of()),
            Map.entry(BEGLICHEN, Set.of()), Map.entry(BESTANDSKRAEFTIG, Set.of()));

    private Uebergaenge() {
    }

    public static boolean zulaessig(AntragZustand von, AntragZustand nach) {
        return ZULAESSIG.getOrDefault(von, Set.of()).contains(nach);
    }

    public static void pruefe(String aktenzeichen, AntragZustand von, AntragZustand nach) {
        if (!zulaessig(von, nach)) {
            throw new RegelVerletzung("UEBERGANG_UNZULAESSIG", "Vorgang " + aktenzeichen + " ist " + von + "; " + nach + " ist von dort nicht erreichbar");
        }
    }
}
