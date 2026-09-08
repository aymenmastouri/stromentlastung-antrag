package de.stromentlastung.antrag.zulieferer;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import de.stromentlastung.antrag.common.CurrentUser;

/** Lässt den Bescheid-Dienst einen Bescheid erlassen; alles geht als Daten hinüber (Fachkonzept B-03). */
@Component
public class BescheidClient extends Zulieferer {

    public record Hauptzollamt(String kennung, String name, String sitz, String iban) {
    }

    public record Adressat(String name, String rechtsform, String strasse, String plz, String ort) {
    }

    public record Berechnung(long versteuertKwh, long elektromobilitaetKwh, boolean elektromobilitaetGeschaetzt,
                             long nutzenergieDritteKwh, long entlastungsfaehigKwh, long satzCentProMwh, long bruttoCent,
                             long selbstbehaltCent, long festgesetztCent, Long selbstberechnetCent) {
    }

    public record Zeichnung(String sachbearbeitung, String freigabe) {
    }

    public record Anlage(String aktenzeichen, String unternehmenKennung, String dienststelle, String art, int entnahmejahr,
                         Hauptzollamt hauptzollamt, Adressat adressat, Berechnung berechnung, List<String> zitate,
                         List<String> gruende, String begruendung, Long bisherFestgesetztCent, Long rueckforderungCent,
                         Zeichnung zeichnung, LocalDate absendung) {
    }

    public record Ergebnis(String kennung, String art, LocalDate absendung, LocalDate bekanntgabe, LocalDate faelligkeit,
                           long betragCent, Long rueckforderungCent, String hash) {
    }

    public BescheidClient(@Value("${stromentlastung.dienste.bescheid}") String url, RestClient.Builder builder, CurrentUser user) {
        super("bescheid", url, builder, user);
    }

    public Ergebnis erlasse(Anlage anlage) {
        // Die Wurzel des Kontextpfads antwortet nur mit Schrägstrich; ohne ihn leitet Tomcat um, und ein POST folgt nicht.
        return sende("/", anlage, Ergebnis.class);
    }
}
