package de.stromentlastung.antrag.fristen;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/** Fachkonzept F-00, F-01, F-02, F-03, F-07. */
class FristenTest {

    @Test
    void antragsfrist_endet_am_31_dezember_des_folgejahres_verschoben_auf_den_naechsten_werktag() {
        assertThat(Fristen.antragsfristEnde(2022)).isEqualTo(LocalDate.of(2024, 1, 2));
        assertThat(Fristen.antragsfristEnde(2024)).isEqualTo(LocalDate.of(2025, 12, 31));
        assertThat(Fristen.antragsfristEnde(2025)).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    void aenderungsfrist_haengt_am_antragsjahr() {
        assertThat(Fristen.aenderungsfristEnde(2025)).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(Fristen.aenderungsfristEnde(2024)).isEqualTo(LocalDate.of(2025, 12, 31));
    }

    @Test
    void bekanntgabe_und_rueckfragefrist() {
        assertThat(Fristen.bekanntgabe(LocalDate.of(2026, 4, 27))).isEqualTo(LocalDate.of(2026, 5, 4));
        assertThat(Fristen.rueckfragefristEnde(LocalDate.of(2026, 8, 20))).isEqualTo(LocalDate.of(2026, 9, 17));
    }
}
