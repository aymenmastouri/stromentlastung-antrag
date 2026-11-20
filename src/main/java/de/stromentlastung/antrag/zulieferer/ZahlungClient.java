package de.stromentlastung.antrag.zulieferer;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import de.stromentlastung.antrag.common.CurrentUser;

/** Meldet der Erhebung Auszahlungen, Rückforderungen und Rückzahlungen. */
@Component
public class ZahlungClient extends Zulieferer {

    public record AuszahlungAnlage(String aktenzeichen, String unternehmenKennung, long betragCent, LocalDate wertstellung, String zahlungsweg) {
    }

    public record RueckforderungAnlage(String aktenzeichen, String unternehmenKennung, String bescheidKennung, long betragCent,
                                       LocalDate bekanntgabe, LocalDate faelligkeit) {
    }

    public record RueckzahlungAnlage(long betragCent, LocalDate wertstellung, String zahlungsweg) {
    }

    public record Zahlung(Long id, String aktenzeichen, String unternehmen, String art, long betragCent, LocalDate wertstellung, String zahlungsweg) {
    }

    public record Saeumnis(int angefangeneMonate, long bemessungsgrundlageCent, long zuschlagCent, LocalDate stichtag, boolean innerhalbSchonfrist) {
    }

    public record Rueckforderung(String aktenzeichen, String unternehmen, String bescheidKennung, long betragCent, LocalDate bekanntgabe,
                                 LocalDate faelligkeit, String zustand, long rueckgezahltCent, long rueckstaendigCent, Saeumnis saeumnis,
                                 Long saeumniszuschlagFestgesetztCent, LocalDate beglichenAm, boolean gedeckt) {
    }

    public ZahlungClient(@Value("${stromentlastung.dienste.zahlung}") String url, RestClient.Builder builder, CurrentUser user) {
        super("zahlung", url, builder, user);
    }

    public Zahlung auszahlung(AuszahlungAnlage anlage) {
        return sende("/auszahlungen", anlage, Zahlung.class);
    }

    public Rueckforderung rueckforderung(RueckforderungAnlage anlage) {
        return sende("/rueckforderungen", anlage, Rueckforderung.class);
    }

    public Rueckforderung rueckforderung(String aktenzeichen) {
        return hole("/rueckforderungen/{az}", Rueckforderung.class, aktenzeichen);
    }

    public Rueckforderung rueckzahlung(String aktenzeichen, RueckzahlungAnlage anlage) {
        return sende("/rueckforderungen/{az}/rueckzahlungen", anlage, Rueckforderung.class, aktenzeichen);
    }
}
