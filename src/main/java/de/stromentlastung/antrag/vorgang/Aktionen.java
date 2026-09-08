package de.stromentlastung.antrag.vorgang;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import de.stromentlastung.antrag.common.CurrentUser;

/** Welche Handlungen der angemeldeten Person am Vorgang offenstehen — die Oberfläche zeigt nur diese. */
final class Aktionen {

    private Aktionen() {
    }

    static List<String> fuer(Antrag a, CurrentUser user, LocalDate heute) {
        List<String> aktionen = new ArrayList<>();
        AntragZustand z = a.getZustand();
        if (user.hatRolle("antragsteller") && user.unternehmen().map(a.getUnternehmenKennung()::equals).orElse(false)) {
            switch (z) {
                case ENTWURF -> aktionen.addAll(List.of("BEARBEITEN", "NACHWEIS", "EINREICHEN", "ZURUECKNEHMEN"));
                case EINGEREICHT -> aktionen.add("ZURUECKNEHMEN");
                case RUECKFRAGE -> aktionen.addAll(List.of("ANTWORTEN", "ZURUECKNEHMEN"));
                default -> { }
            }
        }
        boolean eigeneDienststelle = user.dienststelle().map(a.getDienststelle()::equals).orElse(false);
        if (!eigeneDienststelle) {
            return aktionen;
        }
        boolean aenderbar = z == AntragZustand.AUSGEZAHLT && a.getBestandskraeftigAb() != null && !heute.isAfter(a.getBestandskraeftigAb());
        if (user.hatRolle("sachbearbeitung")) {
            switch (z) {
                case EINGEREICHT -> aktionen.add("UEBERNEHMEN");
                case IN_BEARBEITUNG -> aktionen.addAll(List.of("RUECKFRAGE", "ZEICHNEN"));
                case RUECKFRAGE -> {
                    if (a.getRueckfrageFristEnde() != null && heute.isAfter(a.getRueckfrageFristEnde())) {
                        aktionen.add("RUECKFRAGE_ABLAUF");
                    }
                }
                case FESTGESETZT -> aktionen.add("AUSZAHLUNG");
                case AUSGEZAHLT -> { if (aenderbar) { aktionen.add("AENDERUNG"); } }
                case IN_AENDERUNG -> aktionen.add("AENDERUNG_ZEICHNEN");
                case RUECKFORDERUNG_OFFEN -> aktionen.add("RUECKZAHLUNG");
                default -> { }
            }
        }
        if (user.hatRolle("zeichnung")) {
            switch (z) {
                case ZUR_FREIGABE -> aktionen.addAll(List.of("FREIGEBEN", "ZURUECKWEISEN"));
                case AENDERUNG_ZUR_FREIGABE -> aktionen.addAll(List.of("AENDERUNG_FREIGEBEN", "AENDERUNG_ZURUECKWEISEN"));
                default -> { }
            }
        }
        if (user.hatRolle("pruefdienst")) {
            if (z == AntragZustand.FESTGESETZT || z == AntragZustand.AUSGEZAHLT || z == AntragZustand.IN_AENDERUNG
                    || z == AntragZustand.AENDERUNG_ZUR_FREIGABE || z == AntragZustand.RUECKFORDERUNG_OFFEN
                    || z == AntragZustand.BEGLICHEN || z == AntragZustand.BESTANDSKRAEFTIG) {
                aktionen.add("PRUEFVERMERK");
            }
            if (aenderbar) {
                aktionen.add("AENDERUNG");
            }
        }
        return aktionen;
    }
}
