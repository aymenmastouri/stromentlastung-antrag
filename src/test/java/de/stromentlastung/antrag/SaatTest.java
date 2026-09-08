package de.stromentlastung.antrag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import de.stromentlastung.antrag.vorgang.AntragRepository;
import de.stromentlastung.antrag.vorgang.AntragZustand;

@SpringBootTest
class SaatTest {

    @Autowired
    AntragRepository antraege;

    @Test
    void dreizehn_geschichten_in_zwoelf_zustaenden() {
        assertThat(antraege.count()).isEqualTo(13);
        assertThat(antraege.findByAktenzeichen("HZA-N-9b-2024-000002")).get()
                .extracting("zustand", "rueckforderungCent", "festgesetztCent").containsExactly(AntragZustand.RUECKFORDERUNG_OFFEN, 623000L, 2352000L);
        assertThat(antraege.findByAktenzeichen("HZA-N-9b-2023-000001")).get().extracting("zustand").isEqualTo(AntragZustand.BESTANDSKRAEFTIG);
        assertThat(antraege.findByZustand(AntragZustand.RUECKFRAGE)).hasSize(1);
        assertThat(antraege.countByDienststelleAndEntnahmejahr("HZA-N", 2025)).isEqualTo(7);
    }
}
