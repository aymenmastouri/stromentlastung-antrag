package de.stromentlastung.antrag.vorgang;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UebergaengeTest {

    @Test
    void der_pfad_des_fachkonzepts_ist_zulaessig_seine_abkuerzungen_nicht() {
        assertThat(Uebergaenge.zulaessig(AntragZustand.ENTWURF, AntragZustand.EINGEREICHT)).isTrue();
        assertThat(Uebergaenge.zulaessig(AntragZustand.ZUR_FREIGABE, AntragZustand.FESTGESETZT)).isTrue();
        assertThat(Uebergaenge.zulaessig(AntragZustand.AUSGEZAHLT, AntragZustand.BESTANDSKRAEFTIG)).isTrue();
        assertThat(Uebergaenge.zulaessig(AntragZustand.ENTWURF, AntragZustand.FESTGESETZT)).isFalse();
        assertThat(Uebergaenge.zulaessig(AntragZustand.IN_BEARBEITUNG, AntragZustand.FESTGESETZT)).isFalse();
        assertThat(Uebergaenge.zulaessig(AntragZustand.ABGELEHNT, AntragZustand.IN_BEARBEITUNG)).isFalse();
        assertThat(Uebergaenge.zulaessig(AntragZustand.FESTGESETZT, AntragZustand.IN_AENDERUNG)).isFalse();
    }
}
