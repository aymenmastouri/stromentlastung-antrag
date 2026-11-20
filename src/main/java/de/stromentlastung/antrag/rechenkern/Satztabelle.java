package de.stromentlastung.antrag.rechenkern;

import java.util.List;

/**
 * Entlastungssatz je Megawattstunde nach Entnahmejahr, Rechtsstand 31. Dezember 2025
 * (Fachkonzept R-05). Bis 2023 galten 5,13 Euro (§ 9b Abs. 2 Satz 1 StromStG). Für
 * vom 1. Januar 2024 bis einschließlich 31. Dezember 2025 entnommenen Strom gelten
 * 20 Euro nach § 9b Abs. 2a StromStG in der Fassung des Haushaltsfinanzierungsgesetzes
 * 2024 (BGBl. 2023 I Nr. 412). Ab 2026 gilt mangels Verlängerung wieder der Satz des
 * Absatzes 2 Satz 1. Das Entnahmejahr wählt die Fassung, nicht das Antragsdatum.
 */
public final class Satztabelle {

    public static final String RECHTSSTAND = "2025-12-31";

    public record Satz(Integer vonJahr, Integer bisJahr, long centProMwh, String zitat) {

        boolean gilt(int jahr) {
            return (vonJahr == null || jahr >= vonJahr) && (bisJahr == null || jahr <= bisJahr);
        }
    }

    private static final List<Satz> SAETZE = List.of(
            new Satz(null, 2023, 513, "§ 9b Abs. 2 Satz 1 StromStG"),
            new Satz(2024, 2025, 2000, "§ 9b Abs. 2a StromStG"),
            new Satz(2026, null, 513, "§ 9b Abs. 2 Satz 1 StromStG"));

    private Satztabelle() {
    }

    public static Satz fuer(int entnahmejahr) {
        return SAETZE.stream().filter(s -> s.gilt(entnahmejahr)).findFirst()
                .orElseThrow(() -> new IllegalStateException("kein Entlastungssatz für " + entnahmejahr));
    }

    public static List<Satz> alle() {
        return SAETZE;
    }
}
