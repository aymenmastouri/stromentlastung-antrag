package de.stromentlastung.antrag.fristen;

import java.time.LocalDate;

/** Die Fristen des Vorgangs (Fachkonzept Kap. 7). */
public final class Fristen {

    private Fristen() {
    }

    /**
     * F-01: Der Antrag ist bis zum Ablauf der Festsetzungsfrist zu stellen (§ 17b Abs. 1 Satz 3 StromStV) —
     * ein Jahr (§ 169 Abs. 2 Satz 1 Nr. 1 AO) ab Ablauf des Entnahmejahres (§ 170 Abs. 1 AO).
     */
    public static LocalDate antragsfristEnde(int entnahmejahr) {
        return Werktage.naechsterWerktag(LocalDate.of(entnahmejahr + 1, 12, 31));
    }

    /**
     * F-02: Änderung einer auf Antrag festgesetzten Vergütung bis ein Jahr nach Ablauf des Antragsjahres
     * (§ 170 Abs. 3, § 169 Abs. 2 Satz 1 Nr. 1 AO); danach ist der Vorgang bestandskräftig.
     */
    public static LocalDate aenderungsfristEnde(int antragsjahr) {
        return Werktage.naechsterWerktag(LocalDate.of(antragsjahr + 1, 12, 31));
    }

    /** F-03: elektronisch übermittelt gilt am vierten Tag nach Absendung als bekannt gegeben (§ 122 Abs. 2a AO, A-09). */
    public static LocalDate bekanntgabe(LocalDate absendung) {
        return Werktage.naechsterWerktag(absendung.plusDays(4));
    }

    /** F-07: vier Wochen ab dem Tag nach der Bekanntgabe der Rückfrage (§ 108 Abs. 2 AO). */
    public static LocalDate rueckfragefristEnde(LocalDate bekanntgabe) {
        return Werktage.naechsterWerktag(bekanntgabe.plusDays(28));
    }
}
