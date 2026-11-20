package de.stromentlastung.antrag.rechenkern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import de.stromentlastung.antrag.common.RegelVerletzung;
import de.stromentlastung.antrag.rechenkern.Entlastungsrechner.Berechnung;
import de.stromentlastung.antrag.rechenkern.Entlastungsrechner.Eingabe;

/** Fachkonzept R-03, R-05, R-06, R-12 — Rechtsstand 31. Dezember 2025. */
class EntlastungsrechnerTest {

    @Test
    void regelfall_2025_nordfeld() {
        Berechnung b = Entlastungsrechner.berechne(new Eingabe(2025, 1_850_000, 0, false, 0, 3_675_000L));
        assertThat(b.satzCentProMwh()).isEqualTo(2000);
        assertThat(b.bruttoCent()).isEqualTo(3_700_000);
        assertThat(b.festgesetztCent()).isEqualTo(3_675_000);
        assertThat(b.abweichung()).isFalse();
        assertThat(b.zitate()).contains("§ 9b Abs. 2a StromStG" + Entlastungsrechner.FASSUNG);
    }

    @Test
    void genau_250_euro_uebersteigen_den_selbstbehalt_nicht() {
        Berechnung b = Entlastungsrechner.berechne(new Eingabe(2025, 12_500, 0, false, 0, null));
        assertThat(b.bruttoCent()).isEqualTo(25_000);
        assertThat(b.unterSelbstbehalt()).isTrue();
        assertThat(b.festgesetztCent()).isZero();
        assertThat(Entlastungsrechner.berechne(new Eingabe(2025, 12_501, 0, false, 0, null)).festgesetztCent()).isEqualTo(2);
    }

    @Test
    void alter_satz_2023_mit_kaufmaennischer_rundung() {
        Berechnung b = Entlastungsrechner.berechne(new Eingabe(2023, 420_000, 0, false, 0, null));
        assertThat(b.satzCentProMwh()).isEqualTo(513);
        assertThat(b.bruttoCent()).isEqualTo(215_460);
        assertThat(b.festgesetztCent()).isEqualTo(190_460);
        assertThat(b.zitate()).contains("§ 9b Abs. 2 Satz 1 StromStG" + Entlastungsrechner.FASSUNG)
                .doesNotContain("§ 9b Abs. 2a StromStG" + Entlastungsrechner.FASSUNG);
    }

    @Test
    void ab_2026_gilt_nach_rechtsstand_2025_wieder_der_satz_des_absatzes_2() {
        Berechnung b = Entlastungsrechner.berechne(new Eingabe(2026, 700_000, 0, false, 0, null));
        assertThat(b.satzCentProMwh()).isEqualTo(513);
        assertThat(b.festgesetztCent()).isEqualTo(334_100);
        assertThat(b.rechtsstand()).isEqualTo("2025-12-31");
    }

    @Test
    void abzuege_mindern_die_menge_und_duerfen_sie_nicht_uebersteigen() {
        Berechnung b = Entlastungsrechner.berechne(new Eingabe(2024, 1_500_000, 311_500, false, 0, null));
        assertThat(b.entlastungsfaehigKwh()).isEqualTo(1_188_500);
        assertThat(b.festgesetztCent()).isEqualTo(2_352_000);
        assertThatThrownBy(() -> Entlastungsrechner.berechne(new Eingabe(2024, 1000, 600, false, 500, null)))
                .isInstanceOf(RegelVerletzung.class).extracting("code").isEqualTo("ABZUG_UEBERSTEIGT_MENGE");
    }

    @Test
    void jeder_schritt_nennt_regel_und_fundstelle() {
        Berechnung b = Entlastungsrechner.berechne(new Eingabe(2025, 100_000, 0, false, 0, 1L));
        assertThat(b.schritte()).extracting("regel").contains("R-03", "R-05", "R-12", "R-06", "R-07");
        assertThat(b.schritte()).allSatisfy(s -> assertThat(s.fundstelle()).isNotBlank());
        assertThat(b.abweichung()).isTrue();
    }
}
