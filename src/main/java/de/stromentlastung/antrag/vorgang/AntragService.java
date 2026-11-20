package de.stromentlastung.antrag.vorgang;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.stromentlastung.antrag.common.CurrentUser;
import de.stromentlastung.antrag.common.NichtGefunden;
import de.stromentlastung.antrag.common.RegelVerletzung;
import de.stromentlastung.antrag.common.ZugriffVerweigert;
import de.stromentlastung.antrag.fristen.Fristen;
import de.stromentlastung.antrag.protokoll.EreignisArt;
import de.stromentlastung.antrag.protokoll.Protokoll;
import de.stromentlastung.antrag.protokoll.Vorgangsereignis;
import de.stromentlastung.antrag.rechenkern.Entlastungsrechner;
import de.stromentlastung.antrag.rechenkern.Entlastungsrechner.Berechnung;
import de.stromentlastung.antrag.vorgang.dto.AenderungAnlage;
import de.stromentlastung.antrag.vorgang.dto.AntragAnlage;
import de.stromentlastung.antrag.vorgang.dto.AntragResponse;
import de.stromentlastung.antrag.vorgang.dto.EntscheidungAnlage;
import de.stromentlastung.antrag.vorgang.dto.EreignisResponse;
import de.stromentlastung.antrag.vorgang.dto.FristenResponse;
import de.stromentlastung.antrag.vorgang.dto.NachweisAnlage;
import de.stromentlastung.antrag.vorgang.dto.NachweisResponse;
import de.stromentlastung.antrag.vorgang.dto.TextAnlage;
import de.stromentlastung.antrag.vorgang.dto.VorschauAnfrage;
import de.stromentlastung.antrag.vorgang.dto.ZahlungErfassung;
import de.stromentlastung.antrag.zulieferer.BescheidClient;
import de.stromentlastung.antrag.zulieferer.UnternehmenClient;
import de.stromentlastung.antrag.zulieferer.ZahlungClient;

/**
 * Der Vorgang besitzt die Zustandsmaschine (Fachkonzept Kap. 5) und orchestriert
 * Register, Bescheid und Erhebung (Architektur Kap. 5). Jede Handlung schreibt
 * ein Vorgangsereignis.
 */
@Service
@Transactional
public class AntragService {

    static final long SELBSTERKLAERUNG_SCHWELLE_CENT = 1_000_000;

    private final AntragRepository antraege;
    private final NachweisRepository nachweise;
    private final Protokoll protokoll;
    private final UnternehmenClient register;
    private final BescheidClient bescheide;
    private final ZahlungClient zahlungen;
    private final CurrentUser user;
    private final Clock clock;

    public AntragService(AntragRepository antraege, NachweisRepository nachweise, Protokoll protokoll, UnternehmenClient register,
                         BescheidClient bescheide, ZahlungClient zahlungen, CurrentUser user, Clock clock) {
        this.antraege = antraege;
        this.nachweise = nachweise;
        this.protokoll = protokoll;
        this.register = register;
        this.bescheide = bescheide;
        this.zahlungen = zahlungen;
        this.user = user;
        this.clock = clock;
    }

    // ---------- Antragsteller ----------

    public AntragResponse anlegen(AntragAnlage a) {
        String kennung = user.unternehmenOderVerweigert();
        if (antraege.existsByUnternehmenKennungAndEntnahmejahrAndZustandNot(kennung, a.entnahmejahr(), AntragZustand.ZURUECKGENOMMEN)) {
            throw new RegelVerletzung("ANTRAG_FUER_ENTNAHMEJAHR_VORHANDEN", kennung + " hat für " + a.entnahmejahr() + " bereits einen Antrag");
        }
        UnternehmenClient.Unternehmen u = register.unternehmen(kennung);
        String aktenzeichen = Aktenzeichen.neu(u.hauptzollamt(), a.entnahmejahr(),
                antraege.countByDienststelleAndEntnahmejahr(u.hauptzollamt(), a.entnahmejahr()) + 1);
        Antrag antrag = Antrag.entwurf(aktenzeichen, kennung, u.hauptzollamt(), a.entnahmejahr(), jetzt());
        antrag.erfasse(a.versteuertKwh(), a.elektromobilitaetKwh(), a.elektromobilitaetGeschaetzt(), a.nutzenergieDritteKwh(),
                a.versicherungKeineRueckforderung(), a.versicherungKeineSchwierigkeiten(), a.selbsterklaerungBeihilfen(), a.selbstberechnetCent(), jetzt());
        berechne(antrag);
        antraege.save(antrag);
        ereignis(antrag, EreignisArt.ANTRAG_ANGELEGT, null, AntragZustand.ENTWURF, null, null, null, false);
        return response(antrag);
    }

    public AntragResponse aktualisieren(String aktenzeichen, AntragAnlage a) {
        Antrag antrag = eigener(aktenzeichen);
        nurIn(antrag, AntragZustand.ENTWURF);
        if (a.entnahmejahr() != antrag.getEntnahmejahr()) {
            throw new RegelVerletzung("ENTNAHMEJAHR_UNVERAENDERLICH", "das Entnahmejahr eines Antrags ist Teil des Aktenzeichens");
        }
        antrag.erfasse(a.versteuertKwh(), a.elektromobilitaetKwh(), a.elektromobilitaetGeschaetzt(), a.nutzenergieDritteKwh(),
                a.versicherungKeineRueckforderung(), a.versicherungKeineSchwierigkeiten(), a.selbsterklaerungBeihilfen(), a.selbstberechnetCent(), jetzt());
        berechne(antrag);
        return response(antrag);
    }

    public AntragResponse nachweisHinzufuegen(String aktenzeichen, NachweisAnlage n) {
        Antrag antrag = eigener(aktenzeichen);
        nurIn(antrag, AntragZustand.ENTWURF);
        if (n.zeitraumBis().isBefore(n.zeitraumVon())) {
            throw new RegelVerletzung("ZEITRAUM_UNGUELTIG", "Zeitraum endet vor seinem Beginn");
        }
        nachweise.save(new Nachweis(aktenzeichen, n.versorger(), n.zeitraumVon(), n.zeitraumBis(), n.mengeKwh(), n.verwendungszweck(), n.belegnummer()));
        return response(antrag);
    }

    public AntragResponse nachweisEntfernen(String aktenzeichen, Long id) {
        Antrag antrag = eigener(aktenzeichen);
        nurIn(antrag, AntragZustand.ENTWURF);
        nachweise.delete(nachweise.findByIdAndAktenzeichen(id, aktenzeichen).orElseThrow(() -> new NichtGefunden("Nachweis " + id)));
        return response(antrag);
    }

    /** Kap. 5, Wächter ENTWURF → EINGEREICHT; die Antragsfrist warnt, sperrt aber nicht (F-01, Festlegung). */
    public AntragResponse einreichen(String aktenzeichen) {
        Antrag antrag = eigener(aktenzeichen);
        nurIn(antrag, AntragZustand.ENTWURF);
        LocalDate heute = heute();
        if (antrag.getEntnahmejahr() >= heute.getYear()) {
            throw new RegelVerletzung("ENTNAHMEJAHR_NICHT_ABGELAUFEN", "der Entlastungsabschnitt " + antrag.getEntnahmejahr() + " ist noch nicht abgelaufen (§ 17b Abs. 2 StromStV)");
        }
        if (!antrag.isVersicherungKeineRueckforderung() || !antrag.isVersicherungKeineSchwierigkeiten()) {
            throw new RegelVerletzung("BEIHILFEAUSSCHLUSS", "die Versicherungen nach § 2a StromStG fehlen");
        }
        Berechnung b = berechne(antrag);
        if (b.bruttoCent() >= SELBSTERKLAERUNG_SCHWELLE_CENT && !antrag.isSelbsterklaerungBeihilfen()) {
            throw new RegelVerletzung("SELBSTERKLAERUNG_FEHLT", "ab 10.000 Euro Entlastung ist die Selbsterklärung zu staatlichen Beihilfen beizufügen");
        }
        long belegt = nachweise.findByAktenzeichenOrderByZeitraumVonAscIdAsc(aktenzeichen).stream().mapToLong(Nachweis::getMengeKwh).sum();
        if (belegt < antrag.getVersteuertKwh()) {
            throw new RegelVerletzung("NACHWEIS_UNZUREICHEND", "Belege über " + belegt + " kWh decken die versteuerte Menge von " + antrag.getVersteuertKwh() + " kWh nicht");
        }
        LocalDate antragsfrist = Fristen.antragsfristEnde(antrag.getEntnahmejahr());
        AntragZustand vorher = antrag.getZustand();
        antrag.reiche(heute, Fristen.aenderungsfristEnde(heute.getYear()), jetzt());
        String bemerkung = heute.isAfter(antragsfrist) ? "Eingang nach Ablauf der Antragsfrist am " + antragsfrist : null;
        ereignis(antrag, EreignisArt.ANTRAG_EINGEREICHT, vorher, antrag.getZustand(), bemerkung, null, null, false);
        return response(antrag);
    }

    public AntragResponse zuruecknehmen(String aktenzeichen) {
        Antrag antrag = eigener(aktenzeichen);
        AntragZustand vorher = antrag.getZustand();
        antrag.wechsle(AntragZustand.ZURUECKGENOMMEN, jetzt());
        ereignis(antrag, EreignisArt.ANTRAG_ZURUECKGENOMMEN, vorher, antrag.getZustand(), null, null, null, false);
        return response(antrag);
    }

    public AntragResponse rueckfrageBeantworten(String aktenzeichen, TextAnlage antwort) {
        Antrag antrag = eigener(aktenzeichen);
        nurIn(antrag, AntragZustand.RUECKFRAGE);
        antrag.beantworte(antwort.text(), jetzt());
        ereignis(antrag, EreignisArt.RUECKFRAGE_BEANTWORTET, AntragZustand.RUECKFRAGE, antrag.getZustand(), antwort.text(), null, null, false);
        return response(antrag);
    }

    // ---------- Sachbearbeitung ----------

    public AntragResponse uebernehmen(String aktenzeichen) {
        Antrag antrag = derDienststelle(aktenzeichen);
        nurIn(antrag, AntragZustand.EINGEREICHT);
        antrag.uebernimm(user.kontoname(), user.konto(), jetzt());
        ereignis(antrag, EreignisArt.BEARBEITUNG_UEBERNOMMEN, AntragZustand.EINGEREICHT, antrag.getZustand(), null, null, null, false);
        return response(antrag);
    }

    /** F-07: vier Wochen ab dem Tag nach Bekanntgabe der Rückfrage. */
    public AntragResponse rueckfrageStellen(String aktenzeichen, TextAnlage text) {
        Antrag antrag = derDienststelle(aktenzeichen);
        nurIn(antrag, AntragZustand.IN_BEARBEITUNG);
        LocalDate fristEnde = Fristen.rueckfragefristEnde(Fristen.bekanntgabe(heute()));
        antrag.frageZurueck(text.text(), fristEnde, jetzt());
        ereignis(antrag, EreignisArt.RUECKFRAGE_GESTELLT, AntragZustand.IN_BEARBEITUNG, antrag.getZustand(), text.text() + " (Frist bis " + fristEnde + ")", null, null, false);
        return response(antrag);
    }

    public AntragResponse rueckfragefristAbgelaufen(String aktenzeichen) {
        Antrag antrag = derDienststelle(aktenzeichen);
        nurIn(antrag, AntragZustand.RUECKFRAGE);
        if (antrag.getRueckfrageFristEnde() == null || !heute().isAfter(antrag.getRueckfrageFristEnde())) {
            throw new RegelVerletzung("RUECKFRAGEFRIST_LAEUFT", "die Rückfragefrist endet erst am " + antrag.getRueckfrageFristEnde());
        }
        antrag.beantworte(null, jetzt());
        protokoll.schreibe(aktenzeichen, Protokoll.SYSTEM, "system", EreignisArt.RUECKFRAGEFRIST_ABGELAUFEN, AntragZustand.RUECKFRAGE.name(),
                antrag.getZustand().name(), "Entscheidung nach Aktenlage", null, null, false);
        return response(antrag);
    }

    /** Kap. 5, Wächter IN_BEARBEITUNG → ZUR_FREIGABE: Zuordnung (R-02), Antragsfrist (F-01), Selbstbehalt (R-06). */
    public AntragResponse zeichnen(String aktenzeichen, EntscheidungAnlage e) {
        Antrag antrag = derDienststelle(aktenzeichen);
        nurIn(antrag, AntragZustand.IN_BEARBEITUNG);
        Berechnung b = berechne(antrag);
        boolean beguenstigt = register.zuordnung(antrag.getUnternehmenKennung(), antrag.getEntnahmejahr())
                .map(UnternehmenClient.Zuordnung::beguenstigt).orElse(false);
        boolean verspaetet = antrag.getEingereichtAm() != null && antrag.getEingereichtAm().isAfter(Fristen.antragsfristEnde(antrag.getEntnahmejahr()));
        if (e.vorschlag() == EntscheidungVorschlag.FESTSETZUNG) {
            if (!beguenstigt) {
                throw new RegelVerletzung("ZUORDNUNG_FEHLT", "ohne bestätigte Zuordnung für " + antrag.getEntnahmejahr() + " keine Festsetzung");
            }
            if (verspaetet) {
                throw new RegelVerletzung("FESTSETZUNGSVERJAEHRUNG", "Antrag ging nach Ablauf der Festsetzungsfrist ein");
            }
            if (b.unterSelbstbehalt()) {
                throw new RegelVerletzung("ENTLASTUNG_UNTER_SELBSTBEHALT", "der Entlastungsbetrag übersteigt 250 Euro nicht");
            }
        } else if (e.grund() == null || e.grund().isBlank()) {
            throw new RegelVerletzung("ABLEHNUNG_OHNE_GRUND", "eine Ablehnung braucht einen Grundcode");
        }
        antrag.zeichne(e.vorschlag(), e.grund(), e.begruendung(), b.festgesetztCent(), user.kontoname(), user.konto(), jetzt());
        ereignis(antrag, EreignisArt.ENTSCHEIDUNG_GEZEICHNET, AntragZustand.IN_BEARBEITUNG, antrag.getZustand(),
                e.vorschlag() + (e.grund() != null ? " (" + e.grund() + ")" : ""), null, null, true);
        return response(antrag);
    }

    public AntragResponse auszahlungErfassen(String aktenzeichen, ZahlungErfassung z) {
        Antrag antrag = derDienststelle(aktenzeichen);
        nurIn(antrag, AntragZustand.FESTGESETZT);
        ZahlungClient.Zahlung zahlung = zahlungen.auszahlung(new ZahlungClient.AuszahlungAnlage(aktenzeichen, antrag.getUnternehmenKennung(),
                antrag.getFestgesetztCent(), z.wertstellung() != null ? z.wertstellung() : heute(), z.zahlungsweg()));
        antrag.wechsle(AntragZustand.AUSGEZAHLT, jetzt());
        ereignis(antrag, EreignisArt.AUSZAHLUNG_ERFASST, AntragZustand.FESTGESETZT, antrag.getZustand(), euro(antrag.getFestgesetztCent()), null, "Z-" + zahlung.id(), false);
        return response(antrag);
    }

    /** F-02: Änderung nur bis zum Ende der Änderungsfrist; danach ist der Vorgang bestandskräftig. */
    public AntragResponse aenderungEinleiten(String aktenzeichen, AenderungAnlage ae) {
        Antrag antrag = derDienststelle(aktenzeichen);
        nurIn(antrag, AntragZustand.AUSGEZAHLT);
        if (antrag.getBestandskraeftigAb() != null && heute().isAfter(antrag.getBestandskraeftigAb())) {
            throw new RegelVerletzung("AENDERUNGSFRIST_ABGELAUFEN", "die Änderungsfrist endete am " + antrag.getBestandskraeftigAb());
        }
        antrag.leiteAenderungEin(ae.versteuertKwh(), ae.elektromobilitaetKwh(), ae.nutzenergieDritteKwh(), ae.begruendung(), jetzt());
        berechneKorrigiert(antrag);
        ereignis(antrag, EreignisArt.AENDERUNG_EINGELEITET, AntragZustand.AUSGEZAHLT, antrag.getZustand(), ae.begruendung(), null, null, true);
        return response(antrag);
    }

    public AntragResponse aenderungZeichnen(String aktenzeichen) {
        Antrag antrag = derDienststelle(aktenzeichen);
        nurIn(antrag, AntragZustand.IN_AENDERUNG);
        Berechnung neu = berechneKorrigiert(antrag);
        long rueckforderung = antrag.getFestgesetztCent() - neu.festgesetztCent();
        if (rueckforderung <= 0) {
            throw new RegelVerletzung("AENDERUNG_OHNE_RUECKFORDERUNG", "die geänderte Festsetzung liegt nicht unter der bisherigen (V-08)");
        }
        antrag.zeichneAenderung(neu.festgesetztCent(), rueckforderung, user.kontoname(), user.konto(), jetzt());
        ereignis(antrag, EreignisArt.ENTSCHEIDUNG_GEZEICHNET, AntragZustand.IN_AENDERUNG, antrag.getZustand(), "Änderung, Rückforderung " + euro(rueckforderung), null, null, true);
        return response(antrag);
    }

    public AntragResponse rueckzahlungErfassen(String aktenzeichen, ZahlungErfassung z) {
        Antrag antrag = derDienststelle(aktenzeichen);
        nurIn(antrag, AntragZustand.RUECKFORDERUNG_OFFEN);
        long betrag = z.betragOder(antrag.getRueckforderungCent());
        ZahlungClient.Rueckforderung r = zahlungen.rueckzahlung(aktenzeichen, new ZahlungClient.RueckzahlungAnlage(betrag,
                z.wertstellung() != null ? z.wertstellung() : heute(), z.zahlungsweg()));
        AntragZustand vorher = antrag.getZustand();
        if (r.gedeckt()) {
            antrag.wechsle(AntragZustand.BEGLICHEN, jetzt());
        }
        ereignis(antrag, EreignisArt.RUECKZAHLUNG_ERFASST, vorher, antrag.getZustand(), euro(betrag) + (r.gedeckt() ? "" : ", rückständig " + euro(r.rueckstaendigCent())), null, null, false);
        if (r.gedeckt() && r.saeumnis() != null && r.saeumnis().zuschlagCent() > 0) {
            ereignis(antrag, EreignisArt.SAEUMNISZUSCHLAG_FESTGESETZT, antrag.getZustand(), antrag.getZustand(),
                    euro(r.saeumnis().zuschlagCent()) + " für " + r.saeumnis().angefangeneMonate() + " angefangene Monate", null, null, false);
        }
        return response(antrag);
    }

    // ---------- Prüfdienst ----------

    public AntragResponse pruefvermerkErfassen(String aktenzeichen, TextAnlage text) {
        Antrag antrag = derDienststelle(aktenzeichen);
        AntragZustand z = antrag.getZustand();
        if (z == AntragZustand.ENTWURF || z == AntragZustand.EINGEREICHT || z == AntragZustand.IN_BEARBEITUNG
                || z == AntragZustand.RUECKFRAGE || z == AntragZustand.ZUR_FREIGABE || z == AntragZustand.ZURUECKGENOMMEN) {
            throw new RegelVerletzung("PRUEFVERMERK_VOR_FESTSETZUNG", "geprüft wird, was festgesetzt ist");
        }
        antrag.vermerke(text.text(), jetzt());
        ereignis(antrag, EreignisArt.PRUEFVERMERK_ERFASST, z, z, text.text(), null, null, true);
        return response(antrag);
    }

    // ---------- Zeichnung ----------

    public AntragResponse zurueckweisen(String aktenzeichen, TextAnlage kommentar) {
        Antrag antrag = derDienststelle(aktenzeichen);
        nurIn(antrag, AntragZustand.ZUR_FREIGABE);
        antrag.wechsle(AntragZustand.IN_BEARBEITUNG, jetzt());
        ereignis(antrag, EreignisArt.ENTSCHEIDUNG_ZURUECKGEWIESEN, AntragZustand.ZUR_FREIGABE, antrag.getZustand(), kommentar.text(), null, null, true);
        return response(antrag);
    }

    /** Kap. 5, ZUR_FREIGABE → FESTGESETZT oder ABGELEHNT: Vier-Augen (R-15), Bescheid über den Bescheid-Dienst (B-01 bis B-08). */
    public AntragResponse freigeben(String aktenzeichen) {
        Antrag antrag = derDienststelle(aktenzeichen);
        nurIn(antrag, AntragZustand.ZUR_FREIGABE);
        vierAugen(antrag);
        Berechnung b = berechne(antrag);
        boolean festsetzung = antrag.getEntscheidungVorschlag() == EntscheidungVorschlag.FESTSETZUNG;
        List<String> gruende = festsetzung ? List.of() : List.of(antrag.getEntscheidungGrund());
        BescheidClient.Ergebnis erg = bescheide.erlasse(bescheidAnlage(antrag, b, festsetzung ? "ENTLASTUNG" : "ABLEHNUNG", gruende,
                antrag.getEntscheidungBegruendung(), null, null));
        AntragZustand vorher = antrag.getZustand();
        if (festsetzung) {
            antrag.setzeFest(b.festgesetztCent(), erg.kennung(), jetzt());
        } else {
            antrag.lehneAb(erg.kennung(), jetzt());
        }
        ereignis(antrag, EreignisArt.BESCHEID_ERLASSEN, vorher, antrag.getZustand(), "gilt am " + erg.bekanntgabe() + " als bekannt gegeben", erg.kennung(), null, false);
        return response(antrag);
    }

    public AntragResponse aenderungZurueckweisen(String aktenzeichen, TextAnlage kommentar) {
        Antrag antrag = derDienststelle(aktenzeichen);
        nurIn(antrag, AntragZustand.AENDERUNG_ZUR_FREIGABE);
        antrag.wechsle(AntragZustand.IN_AENDERUNG, jetzt());
        ereignis(antrag, EreignisArt.ENTSCHEIDUNG_ZURUECKGEWIESEN, AntragZustand.AENDERUNG_ZUR_FREIGABE, antrag.getZustand(), kommentar.text(), null, null, true);
        return response(antrag);
    }

    /** AENDERUNG_ZUR_FREIGABE → RUECKFORDERUNG_OFFEN: Änderungsbescheid mit Leistungsgebot (B-07), Rückforderung an die Erhebung (F-05). */
    public AntragResponse aenderungFreigeben(String aktenzeichen) {
        Antrag antrag = derDienststelle(aktenzeichen);
        nurIn(antrag, AntragZustand.AENDERUNG_ZUR_FREIGABE);
        vierAugen(antrag);
        Berechnung neu = berechneKorrigiert(antrag);
        List<String> gruende = new ArrayList<>();
        if (antrag.korrigierteVersteuertKwh() != antrag.getVersteuertKwh()) {
            gruende.add("MENGE_NACH_PRUEFUNG_GEAENDERT");
        }
        if (antrag.korrigierteElektromobilitaetKwh() != antrag.getElektromobilitaetKwh() || antrag.korrigierteNutzenergieDritteKwh() != antrag.getNutzenergieDritteKwh()) {
            gruende.add("ABZUG_NACH_PRUEFUNG_GEAENDERT");
        }
        BescheidClient.Ergebnis erg = bescheide.erlasse(bescheidAnlage(antrag, neu, "AENDERUNG", gruende, antrag.getAenderungBegruendung(),
                antrag.getBisherFestgesetztCent(), antrag.getRueckforderungCent()));
        zahlungen.rueckforderung(new ZahlungClient.RueckforderungAnlage(aktenzeichen, antrag.getUnternehmenKennung(), erg.kennung(),
                antrag.getRueckforderungCent(), erg.bekanntgabe(), erg.faelligkeit()));
        antrag.aendere(neu.festgesetztCent(), erg.kennung(), jetzt());
        ereignis(antrag, EreignisArt.BESCHEID_ERLASSEN, AntragZustand.AENDERUNG_ZUR_FREIGABE, antrag.getZustand(),
                "Rückforderung " + euro(antrag.getRueckforderungCent()) + ", fällig am " + erg.faelligkeit(), erg.kennung(), null, false);
        return response(antrag);
    }

    // ---------- Lesen ----------

    @Transactional(readOnly = true)
    public List<AntragResponse> eigene() {
        return antraege.findByUnternehmenKennungOrderByEntnahmejahrDescAngelegtAmDesc(user.unternehmenOderVerweigert()).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<AntragResponse> derDienststelle(Optional<AntragZustand> zustand) {
        String dienststelle = user.dienststelleOderVerweigert();
        List<Antrag> liste = zustand.map(z -> antraege.findByDienststelleAndZustandOrderByGeaendertAmDesc(dienststelle, z))
                .orElseGet(() -> antraege.findByDienststelleOrderByGeaendertAmDesc(dienststelle));
        return liste.stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<AntragResponse> zurFreigabe() {
        return antraege.findByDienststelleAndZustandInOrderByGeaendertAmAsc(user.dienststelleOderVerweigert(),
                List.of(AntragZustand.ZUR_FREIGABE, AntragZustand.AENDERUNG_ZUR_FREIGABE)).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public AntragResponse einer(String aktenzeichen) {
        return response(lesbar(aktenzeichen));
    }

    @Transactional(readOnly = true)
    public List<EreignisResponse> protokoll(String aktenzeichen) {
        Antrag antrag = lesbar(aktenzeichen);
        boolean intern = user.dienststelle().map(antrag.getDienststelle()::equals).orElse(false);
        List<Vorgangsereignis> liste = intern ? protokoll.alle(aktenzeichen) : protokoll.oeffentliche(aktenzeichen);
        return liste.stream().map(e -> new EreignisResponse(e.getId(), e.getZeitpunkt(), e.getAkteur(), e.getRolle(), e.getArt(),
                e.getZustandVorher(), e.getZustandNachher(), e.getBemerkung(), e.getBescheidKennung(), e.getZahlungKennung(), e.isIntern())).toList();
    }

    public Berechnung vorschau(VorschauAnfrage v) {
        return Entlastungsrechner.berechne(new Entlastungsrechner.Eingabe(v.entnahmejahr(), v.versteuertKwh(), v.elektromobilitaetKwh(),
                v.elektromobilitaetGeschaetzt(), v.nutzenergieDritteKwh(), v.selbstberechnetCent()));
    }

    // ---------- System ----------

    /** F-02: nach Ablauf der Änderungsfrist ist der Vorgang bestandskräftig. */
    public int bestandskraftPruefen() {
        LocalDate heute = heute();
        int anzahl = 0;
        for (Antrag a : antraege.findByZustand(AntragZustand.AUSGEZAHLT)) {
            if (a.getBestandskraeftigAb() != null && heute.isAfter(a.getBestandskraeftigAb())) {
                a.wechsle(AntragZustand.BESTANDSKRAEFTIG, jetzt());
                protokoll.schreibe(a.getAktenzeichen(), Protokoll.SYSTEM, "system", EreignisArt.VORGANG_BESTANDSKRAEFTIG,
                        AntragZustand.AUSGEZAHLT.name(), AntragZustand.BESTANDSKRAEFTIG.name(), "Änderungsfrist am " + a.getBestandskraeftigAb() + " abgelaufen", null, null, false);
                anzahl++;
            }
        }
        return anzahl;
    }

    // ---------- Helfer ----------

    private Antrag lade(String aktenzeichen) {
        return antraege.findByAktenzeichen(aktenzeichen).orElseThrow(() -> new NichtGefunden("Vorgang " + aktenzeichen));
    }

    private Antrag eigener(String aktenzeichen) {
        Antrag a = lade(aktenzeichen);
        if (!user.unternehmen().map(a.getUnternehmenKennung()::equals).orElse(false)) {
            throw new ZugriffVerweigert("Vorgang " + aktenzeichen + " gehört zu " + a.getUnternehmenKennung());
        }
        return a;
    }

    private Antrag derDienststelle(String aktenzeichen) {
        Antrag a = lade(aktenzeichen);
        if (!user.dienststelle().map(a.getDienststelle()::equals).orElse(false)) {
            throw new ZugriffVerweigert("Vorgang " + aktenzeichen + " gehört zu " + a.getDienststelle());
        }
        return a;
    }

    private Antrag lesbar(String aktenzeichen) {
        Antrag a = lade(aktenzeichen);
        boolean eigenes = user.unternehmen().map(a.getUnternehmenKennung()::equals).orElse(false);
        boolean dienststelle = user.dienststelle().map(a.getDienststelle()::equals).orElse(false);
        if (!eigenes && !dienststelle) {
            throw new ZugriffVerweigert("kein Zugriff auf Vorgang " + aktenzeichen);
        }
        return a;
    }

    private static void nurIn(Antrag a, AntragZustand... zustaende) {
        if (!Arrays.asList(zustaende).contains(a.getZustand())) {
            throw new RegelVerletzung("VORGANG_NICHT_IN_ZUSTAND", "Vorgang " + a.getAktenzeichen() + " ist " + a.getZustand() + ", erwartet " + Arrays.toString(zustaende));
        }
    }

    private void vierAugen(Antrag a) {
        if (a.getGezeichnetVonKonto() != null && a.getGezeichnetVonKonto().equals(user.konto())) {
            throw new RegelVerletzung("VIER_AUGEN_VERLETZT", "die zeichnende Person darf die Entscheidung nicht selbst freigeben (R-15)");
        }
    }

    private Berechnung berechne(Antrag a) {
        return Entlastungsrechner.berechne(new Entlastungsrechner.Eingabe(a.getEntnahmejahr(), a.getVersteuertKwh(), a.getElektromobilitaetKwh(),
                a.isElektromobilitaetGeschaetzt(), a.getNutzenergieDritteKwh(), a.getSelbstberechnetCent()));
    }

    private Berechnung berechneKorrigiert(Antrag a) {
        return Entlastungsrechner.berechne(new Entlastungsrechner.Eingabe(a.getEntnahmejahr(), a.korrigierteVersteuertKwh(),
                a.korrigierteElektromobilitaetKwh(), a.isElektromobilitaetGeschaetzt(), a.korrigierteNutzenergieDritteKwh(), null));
    }

    private BescheidClient.Anlage bescheidAnlage(Antrag a, Berechnung b, String art, List<String> gruende, String begruendung, Long bisher, Long rueckforderung) {
        UnternehmenClient.Unternehmen u = register.unternehmen(a.getUnternehmenKennung());
        UnternehmenClient.Hauptzollamt h = register.hauptzollamt(a.getDienststelle());
        return new BescheidClient.Anlage(a.getAktenzeichen(), a.getUnternehmenKennung(), a.getDienststelle(), art, a.getEntnahmejahr(),
                new BescheidClient.Hauptzollamt(h.kennung(), h.name(), h.sitz(), h.iban()),
                new BescheidClient.Adressat(u.name(), u.rechtsform(), u.strasse(), u.plz(), u.ort()),
                new BescheidClient.Berechnung(b.versteuertKwh(), b.elektromobilitaetKwh(), b.elektromobilitaetGeschaetzt(), b.nutzenergieDritteKwh(),
                        b.entlastungsfaehigKwh(), b.satzCentProMwh(), b.bruttoCent(), b.selbstbehaltCent(), b.festgesetztCent(), b.selbstberechnetCent()),
                b.zitate(), gruende, begruendung, bisher, rueckforderung,
                new BescheidClient.Zeichnung(a.getGezeichnetVon(), user.kontoname()), heute());
    }

    private void ereignis(Antrag a, EreignisArt art, AntragZustand vorher, AntragZustand nachher, String bemerkung, String bescheid, String zahlung, boolean intern) {
        protokoll.schreibe(a.getAktenzeichen(), user.kontoname(), rolle(), art, vorher == null ? null : vorher.name(), nachher.name(), bemerkung, bescheid, zahlung, intern);
    }

    private String rolle() {
        for (String r : List.of("antragsteller", "sachbearbeitung", "zeichnung", "pruefdienst")) {
            if (user.hatRolle(r)) {
                return r;
            }
        }
        return "unbekannt";
    }

    private AntragResponse response(Antrag a) {
        LocalDate heute = heute();
        boolean korrigiert = a.getKorrigiertVersteuertKwh() != null || a.getKorrigiertElektromobilitaetKwh() != null || a.getKorrigiertNutzenergieDritteKwh() != null;
        Berechnung b = korrigiert ? berechneKorrigiert(a) : berechne(a);
        LocalDate antragsfrist = Fristen.antragsfristEnde(a.getEntnahmejahr());
        FristenResponse fristen = new FristenResponse(antragsfrist, heute.isAfter(antragsfrist), a.getBestandskraeftigAb(),
                a.getRueckfrageFristEnde(), a.getRueckfrageFristEnde() != null && heute.isAfter(a.getRueckfrageFristEnde()));
        List<NachweisResponse> belege = nachweise.findByAktenzeichenOrderByZeitraumVonAscIdAsc(a.getAktenzeichen()).stream()
                .map(n -> new NachweisResponse(n.getId(), n.getVersorger(), n.getZeitraumVon(), n.getZeitraumBis(), n.getMengeKwh(),
                        n.getVerwendungszweck(), n.getBelegnummer(), n.isGeprueft())).toList();
        return new AntragResponse(a.getAktenzeichen(), a.getUnternehmenKennung(), a.getDienststelle(), a.getEntnahmejahr(), a.getZustand(),
                a.getVersteuertKwh(), a.getElektromobilitaetKwh(), a.isElektromobilitaetGeschaetzt(), a.getNutzenergieDritteKwh(),
                a.isVersicherungKeineRueckforderung(), a.isVersicherungKeineSchwierigkeiten(), a.isSelbsterklaerungBeihilfen(),
                a.getSelbstberechnetCent(), a.getNachberechnetCent(), a.getFestgesetztCent(), a.getEingereichtAm(), a.getAntragsjahr(),
                a.getBearbeitung(), a.getEntscheidungVorschlag(), a.getEntscheidungGrund(), a.getEntscheidungBegruendung(), a.getGezeichnetVon(),
                a.getRueckfrageText(), a.getRueckfrageAntwort(), a.getPruefvermerk(), a.getKorrigiertVersteuertKwh(),
                a.getKorrigiertElektromobilitaetKwh(), a.getKorrigiertNutzenergieDritteKwh(), a.getAenderungBegruendung(),
                a.getBisherFestgesetztCent(), a.getRueckforderungCent(), a.getLetzterBescheid(), a.getAngelegtAm(), a.getGeaendertAm(),
                b, fristen, belege, Aktionen.fuer(a, user, heute));
    }

    private Instant jetzt() {
        return Instant.now(clock);
    }

    private LocalDate heute() {
        return LocalDate.now(clock);
    }

    private static String euro(long cent) {
        return String.format(Locale.GERMANY, "%,.2f Euro", cent / 100.0);
    }
}
