package de.stromentlastung.antrag.rechenkern;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.stromentlastung.antrag.common.RegelVerletzung;

/**
 * Der Rechenkern (Fachkonzept R-03, R-05, R-06, R-12): rein, ohne Datenbank und ohne
 * Uhr. Jeder Schritt nennt Regel und Fundstelle; das Berechnungsprotokoll sehen
 * Antragsteller und Hauptzollamt gleichermaßen. Die Rechtsgrundlagen-Zitate sind
 * Ergebnis der Berechnung und werden dem Bescheid als Daten übergeben (B-03).
 */
public final class Entlastungsrechner {

    public static final long SELBSTBEHALT_CENT = 25000;
    public static final String ZITAT_TATBESTAND = "§ 9b Abs. 1 StromStG";
    public static final String ZITAT_SELBSTBEHALT = "§ 9b Abs. 2 Satz 2 StromStG";
    public static final String FASSUNG = " in der für das Entnahmejahr geltenden Fassung";

    public record Eingabe(int entnahmejahr, long versteuertKwh, long elektromobilitaetKwh, boolean elektromobilitaetGeschaetzt,
                          long nutzenergieDritteKwh, Long selbstberechnetCent) {
    }

    public record Schritt(String regel, String fundstelle, String text, String wert) {
    }

    public record Berechnung(int entnahmejahr, long versteuertKwh, long elektromobilitaetKwh, boolean elektromobilitaetGeschaetzt,
                             long nutzenergieDritteKwh, long entlastungsfaehigKwh, long satzCentProMwh, long bruttoCent,
                             long selbstbehaltCent, long festgesetztCent, boolean unterSelbstbehalt, Long selbstberechnetCent,
                             boolean abweichung, List<String> zitate, List<Schritt> schritte, String rechtsstand) {
    }

    private Entlastungsrechner() {
    }

    public static Berechnung berechne(Eingabe e) {
        if (e.versteuertKwh() < 0 || e.elektromobilitaetKwh() < 0 || e.nutzenergieDritteKwh() < 0) {
            throw new RegelVerletzung("MENGE_NEGATIV", "Strommengen können nicht negativ sein");
        }
        if (e.elektromobilitaetKwh() + e.nutzenergieDritteKwh() > e.versteuertKwh()) {
            throw new RegelVerletzung("ABZUG_UEBERSTEIGT_MENGE", "Abzüge von " + (e.elektromobilitaetKwh() + e.nutzenergieDritteKwh())
                    + " kWh übersteigen die versteuerte Menge von " + e.versteuertKwh() + " kWh");
        }
        List<Schritt> schritte = new ArrayList<>();
        long entlastungsfaehig = e.versteuertKwh() - e.elektromobilitaetKwh() - e.nutzenergieDritteKwh();
        schritte.add(new Schritt("R-03", ZITAT_TATBESTAND + " Satz 1, 2 und 4",
                "Versteuerte Menge abzüglich Elektromobilität" + (e.elektromobilitaetGeschaetzt() ? " (geschätzt, § 17b Abs. 4a StromStV)" : "")
                        + " und Nutzenergie an Unternehmen außerhalb des Produzierenden Gewerbes", kwh(entlastungsfaehig)));

        Satztabelle.Satz satz = Satztabelle.fuer(e.entnahmejahr());
        schritte.add(new Schritt("R-05", satz.zitat(), "Entlastungssatz für das Entnahmejahr " + e.entnahmejahr(), euro(satz.centProMwh()) + " je MWh"));

        long brutto = BigDecimal.valueOf(entlastungsfaehig).multiply(BigDecimal.valueOf(satz.centProMwh()))
                .divide(BigDecimal.valueOf(1000), 0, RoundingMode.HALF_UP).longValueExact();
        schritte.add(new Schritt("R-12", "Festlegung", "Entlastungsfähige Menge in Megawattstunden mal Satz, kaufmännisch auf Cent gerundet", euro(brutto)));

        boolean unterSelbstbehalt = brutto <= SELBSTBEHALT_CENT;
        long festgesetzt = unterSelbstbehalt ? 0 : brutto - SELBSTBEHALT_CENT;
        schritte.add(new Schritt("R-06", ZITAT_SELBSTBEHALT, unterSelbstbehalt
                ? "Der Entlastungsbetrag übersteigt 250,00 Euro nicht; es wird keine Entlastung gewährt"
                : "Entlastungsbetrag abzüglich Selbstbehalt von 250,00 Euro", euro(festgesetzt)));

        boolean abweichung = e.selbstberechnetCent() != null && e.selbstberechnetCent() != festgesetzt;
        if (abweichung) {
            schritte.add(new Schritt("R-07", "§ 17b Abs. 1 Satz 2 StromStV", "Selbstberechnung des Antragstellers weicht ab", euro(e.selbstberechnetCent())));
        }
        List<String> zitate = List.of(ZITAT_TATBESTAND + FASSUNG, satz.zitat() + FASSUNG, ZITAT_SELBSTBEHALT + FASSUNG);
        return new Berechnung(e.entnahmejahr(), e.versteuertKwh(), e.elektromobilitaetKwh(), e.elektromobilitaetGeschaetzt(),
                e.nutzenergieDritteKwh(), entlastungsfaehig, satz.centProMwh(), brutto, SELBSTBEHALT_CENT, festgesetzt,
                unterSelbstbehalt, e.selbstberechnetCent(), abweichung, zitate, List.copyOf(schritte), Satztabelle.RECHTSSTAND);
    }

    static String euro(long cent) {
        return String.format(Locale.GERMANY, "%,.2f Euro", cent / 100.0);
    }

    static String kwh(long kwh) {
        return String.format(Locale.GERMANY, "%,d kWh", kwh);
    }
}
