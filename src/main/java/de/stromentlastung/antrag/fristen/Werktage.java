package de.stromentlastung.antrag.fristen;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * Fristberechnung nach § 108 AO in Verbindung mit §§ 187 bis 193 BGB (Fachkonzept
 * F-00): Fällt ein Fristende auf Sonnabend, Sonntag oder einen gesetzlichen
 * Feiertag, endet die Frist mit dem nächsten Werktag (§ 108 Abs. 3 AO). Nur
 * bundeseinheitliche Feiertage (V-15). Gleichlautend im Bescheid-Dienst (A-04).
 */
public final class Werktage {

    private Werktage() {
    }

    public static LocalDate naechsterWerktag(LocalDate datum) {
        LocalDate d = datum;
        while (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY || feiertage(d.getYear()).contains(d)) {
            d = d.plusDays(1);
        }
        return d;
    }

    public static Set<LocalDate> feiertage(int jahr) {
        LocalDate ostern = ostersonntag(jahr);
        return Set.of(
                LocalDate.of(jahr, 1, 1), ostern.minusDays(2), ostern.plusDays(1), LocalDate.of(jahr, 5, 1),
                ostern.plusDays(39), ostern.plusDays(50), LocalDate.of(jahr, 10, 3),
                LocalDate.of(jahr, 12, 25), LocalDate.of(jahr, 12, 26));
    }

    public static LocalDate ostersonntag(int jahr) {
        int a = jahr % 19, b = jahr / 100, c = jahr % 100, d = b / 4, e = b % 4, f = (b + 8) / 25, g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30, i = c / 4, k = c % 4, l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int monat = (h + l - 7 * m + 114) / 31, tag = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(jahr, monat, tag);
    }
}
