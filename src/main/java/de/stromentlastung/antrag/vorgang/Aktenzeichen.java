package de.stromentlastung.antrag.vorgang;

/** `<Dienststelle>-9b-<Entnahmejahr>-<laufende Nummer>`, gezählt je Dienststelle und Entnahmejahr (Fachkonzept Kap. 4, Festlegung). */
final class Aktenzeichen {

    private Aktenzeichen() {
    }

    static String neu(String dienststelle, int entnahmejahr, long laufendeNummer) {
        return String.format("%s-9b-%d-%06d", dienststelle, entnahmejahr, laufendeNummer);
    }
}
