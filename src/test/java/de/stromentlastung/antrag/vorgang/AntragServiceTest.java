package de.stromentlastung.antrag.vorgang;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.stromentlastung.antrag.common.CurrentUser;
import de.stromentlastung.antrag.common.RegelVerletzung;
import de.stromentlastung.antrag.protokoll.Protokoll;
import de.stromentlastung.antrag.protokoll.VorgangsereignisRepository;
import de.stromentlastung.antrag.vorgang.dto.EntscheidungAnlage;
import de.stromentlastung.antrag.zulieferer.BescheidClient;
import de.stromentlastung.antrag.zulieferer.UnternehmenClient;
import de.stromentlastung.antrag.zulieferer.ZahlungClient;

class AntragServiceTest {

    private final AntragRepository antraege = mock(AntragRepository.class);
    private final NachweisRepository nachweise = mock(NachweisRepository.class);
    private final VorgangsereignisRepository ereignisse = mock(VorgangsereignisRepository.class);
    private final UnternehmenClient register = mock(UnternehmenClient.class);
    private final BescheidClient bescheide = mock(BescheidClient.class);
    private final ZahlungClient zahlungen = mock(ZahlungClient.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-08T10:00:00Z"), ZoneId.of("Europe/Berlin"));
    private AntragService service;
    private Antrag antrag;

    @BeforeEach
    void setUp() {
        service = new AntragService(antraege, nachweise, new Protokoll(ereignisse, clock), register, bescheide, zahlungen, user, clock);
        antrag = Antrag.entwurf("HZA-N-9b-2025-000008", "U-001", "HZA-N", 2025, Instant.now(clock));
        antrag.erfasse(1_850_000, 0, false, 0, true, true, true, null, Instant.now(clock));
        when(antraege.findByAktenzeichen(antrag.getAktenzeichen())).thenReturn(Optional.of(antrag));
        when(nachweise.findByAktenzeichenOrderByZeitraumVonAscIdAsc(anyString())).thenReturn(List.of());
        when(user.unternehmen()).thenReturn(Optional.of("U-001"));
        when(user.dienststelle()).thenReturn(Optional.empty());
        when(user.kontoname()).thenReturn("Nordfeld Metallbau GmbH");
        when(user.hatRolle("antragsteller")).thenReturn(true);
    }

    @Test
    void einreichen_verlangt_nachweise_ueber_die_versteuerte_menge() {
        assertThatThrownBy(() -> service.einreichen(antrag.getAktenzeichen()))
                .isInstanceOf(RegelVerletzung.class).extracting("code").isEqualTo("NACHWEIS_UNZUREICHEND");
        when(nachweise.findByAktenzeichenOrderByZeitraumVonAscIdAsc(anyString())).thenReturn(List.of(
                new Nachweis(antrag.getAktenzeichen(), "Stadtwerke Kiel", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), 1_850_000, "Produktion", null)));
        assertThat(service.einreichen(antrag.getAktenzeichen()).zustand()).isEqualTo(AntragZustand.EINGEREICHT);
        assertThat(antrag.getBestandskraeftigAb()).isEqualTo(LocalDate.of(2027, 12, 31));
    }

    @Test
    void einreichen_verlangt_die_versicherungen_und_ab_zehntausend_euro_die_selbsterklaerung() {
        antrag.erfasse(1_850_000, 0, false, 0, true, false, true, null, Instant.now(clock));
        assertThatThrownBy(() -> service.einreichen(antrag.getAktenzeichen()))
                .isInstanceOf(RegelVerletzung.class).extracting("code").isEqualTo("BEIHILFEAUSSCHLUSS");
        antrag.erfasse(1_850_000, 0, false, 0, true, true, false, null, Instant.now(clock));
        assertThatThrownBy(() -> service.einreichen(antrag.getAktenzeichen()))
                .isInstanceOf(RegelVerletzung.class).extracting("code").isEqualTo("SELBSTERKLAERUNG_FEHLT");
    }

    @Test
    void ein_laufendes_entnahmejahr_kann_nicht_eingereicht_werden() {
        Antrag entwurf2026 = Antrag.entwurf("HZA-N-9b-2026-000002", "U-001", "HZA-N", 2026, Instant.now(clock));
        entwurf2026.erfasse(700_000, 0, false, 0, true, true, false, null, Instant.now(clock));
        when(antraege.findByAktenzeichen(entwurf2026.getAktenzeichen())).thenReturn(Optional.of(entwurf2026));
        assertThatThrownBy(() -> service.einreichen(entwurf2026.getAktenzeichen()))
                .isInstanceOf(RegelVerletzung.class).extracting("code").isEqualTo("ENTNAHMEJAHR_NICHT_ABGELAUFEN");
    }

    @Test
    void zeichnen_einer_festsetzung_braucht_die_bestaetigte_zuordnung() {
        alsSachbearbeitung();
        antrag.reiche(LocalDate.of(2026, 3, 2), LocalDate.of(2027, 12, 31), Instant.now(clock));
        antrag.uebernimm("Sabine Wagner", "wagner@stromentlastung.dev", Instant.now(clock));
        when(register.zuordnung(anyString(), anyInt())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.zeichnen(antrag.getAktenzeichen(), new EntscheidungAnlage(EntscheidungVorschlag.FESTSETZUNG, null, null)))
                .isInstanceOf(RegelVerletzung.class).extracting("code").isEqualTo("ZUORDNUNG_FEHLT");
        when(register.zuordnung(anyString(), anyInt())).thenReturn(Optional.of(new UnternehmenClient.Zuordnung("U-001", 2025, "BESTAETIGT", true, "D")));
        assertThat(service.zeichnen(antrag.getAktenzeichen(), new EntscheidungAnlage(EntscheidungVorschlag.FESTSETZUNG, null, "ok")).zustand())
                .isEqualTo(AntragZustand.ZUR_FREIGABE);
        assertThat(antrag.getNachberechnetCent()).isEqualTo(3_675_000);
    }

    @Test
    void die_zeichnende_person_darf_nicht_selbst_freigeben() {
        alsSachbearbeitung();
        antrag.reiche(LocalDate.of(2026, 3, 2), LocalDate.of(2027, 12, 31), Instant.now(clock));
        antrag.uebernimm("Sabine Wagner", "wagner@stromentlastung.dev", Instant.now(clock));
        antrag.zeichne(EntscheidungVorschlag.FESTSETZUNG, null, null, 3_675_000, "Sabine Wagner", "wagner@stromentlastung.dev", Instant.now(clock));
        when(user.hatRolle("zeichnung")).thenReturn(true);
        when(user.konto()).thenReturn("wagner@stromentlastung.dev");
        assertThatThrownBy(() -> service.freigeben(antrag.getAktenzeichen()))
                .isInstanceOf(RegelVerletzung.class).extracting("code").isEqualTo("VIER_AUGEN_VERLETZT");
    }

    @Test
    void freigabe_erlaesst_den_bescheid_und_setzt_fest() {
        alsSachbearbeitung();
        antrag.reiche(LocalDate.of(2026, 3, 2), LocalDate.of(2027, 12, 31), Instant.now(clock));
        antrag.uebernimm("Sabine Wagner", "wagner@stromentlastung.dev", Instant.now(clock));
        antrag.zeichne(EntscheidungVorschlag.FESTSETZUNG, null, null, 3_675_000, "Sabine Wagner", "wagner@stromentlastung.dev", Instant.now(clock));
        when(user.hatRolle("zeichnung")).thenReturn(true);
        when(user.konto()).thenReturn("becker@stromentlastung.dev");
        when(user.kontoname()).thenReturn("Lars Becker");
        when(register.unternehmen("U-001")).thenReturn(new UnternehmenClient.Unternehmen("U-001", "Nordfeld Metallbau GmbH", "GmbH", "Werftstraße 12", "24103", "Kiel", "HZA-N", null));
        when(register.hauptzollamt("HZA-N")).thenReturn(new UnternehmenClient.Hauptzollamt("HZA-N", "Hauptzollamt Nord", "Kiel", "DE02"));
        when(bescheide.erlasse(any())).thenReturn(new BescheidClient.Ergebnis("HZA-N-9b-2025-000008-B1", "ENTLASTUNG",
                LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 14), null, 3_675_000, null, "abc"));
        var r = service.freigeben(antrag.getAktenzeichen());
        assertThat(r.zustand()).isEqualTo(AntragZustand.FESTGESETZT);
        assertThat(r.festgesetztCent()).isEqualTo(3_675_000);
        assertThat(r.letzterBescheid()).isEqualTo("HZA-N-9b-2025-000008-B1");
    }

    private void alsSachbearbeitung() {
        when(user.unternehmen()).thenReturn(Optional.empty());
        when(user.dienststelle()).thenReturn(Optional.of("HZA-N"));
        when(user.hatRolle("antragsteller")).thenReturn(false);
        when(user.hatRolle("sachbearbeitung")).thenReturn(true);
        when(user.kontoname()).thenReturn("Sabine Wagner");
        when(user.konto()).thenReturn("wagner@stromentlastung.dev");
    }
}
