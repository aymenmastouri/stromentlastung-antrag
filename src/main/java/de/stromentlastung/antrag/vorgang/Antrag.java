package de.stromentlastung.antrag.vorgang;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Der Vorgang: ein Antrag auf Steuerentlastung für ein Entnahmejahr (Fachkonzept Kap. 4). */
@Entity
@Table(name = "antrag")
public class Antrag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String aktenzeichen;
    @Column(name = "unternehmen_kennung", nullable = false)
    private String unternehmenKennung;
    @Column(nullable = false)
    private String dienststelle;
    @Column(nullable = false)
    private int entnahmejahr;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AntragZustand zustand;
    @Column(name = "versteuert_kwh", nullable = false)
    private long versteuertKwh;
    @Column(name = "elektromobilitaet_kwh", nullable = false)
    private long elektromobilitaetKwh;
    @Column(name = "elektromobilitaet_geschaetzt", nullable = false)
    private boolean elektromobilitaetGeschaetzt;
    @Column(name = "nutzenergie_dritte_kwh", nullable = false)
    private long nutzenergieDritteKwh;
    @Column(name = "versicherung_keine_rueckforderung", nullable = false)
    private boolean versicherungKeineRueckforderung;
    @Column(name = "versicherung_keine_schwierigkeiten", nullable = false)
    private boolean versicherungKeineSchwierigkeiten;
    @Column(name = "selbsterklaerung_beihilfen", nullable = false)
    private boolean selbsterklaerungBeihilfen;
    @Column(name = "selbstberechnet_cent")
    private Long selbstberechnetCent;
    @Column(name = "nachberechnet_cent")
    private Long nachberechnetCent;
    @Column(name = "festgesetzt_cent")
    private Long festgesetztCent;
    @Column(name = "eingereicht_am")
    private LocalDate eingereichtAm;
    private Integer antragsjahr;
    @Column(name = "bestandskraeftig_ab")
    private LocalDate bestandskraeftigAb;
    private String bearbeitung;
    @Column(name = "bearbeitung_konto")
    private String bearbeitungKonto;
    @Enumerated(EnumType.STRING)
    @Column(name = "entscheidung_vorschlag")
    private EntscheidungVorschlag entscheidungVorschlag;
    @Column(name = "entscheidung_grund")
    private String entscheidungGrund;
    @Column(name = "entscheidung_begruendung")
    private String entscheidungBegruendung;
    @Column(name = "gezeichnet_von")
    private String gezeichnetVon;
    @Column(name = "gezeichnet_von_konto")
    private String gezeichnetVonKonto;
    @Column(name = "rueckfrage_text")
    private String rueckfrageText;
    @Column(name = "rueckfrage_frist_ende")
    private LocalDate rueckfrageFristEnde;
    @Column(name = "rueckfrage_antwort")
    private String rueckfrageAntwort;
    private String pruefvermerk;
    @Column(name = "korrigiert_versteuert_kwh")
    private Long korrigiertVersteuertKwh;
    @Column(name = "korrigiert_elektromobilitaet_kwh")
    private Long korrigiertElektromobilitaetKwh;
    @Column(name = "korrigiert_nutzenergie_dritte_kwh")
    private Long korrigiertNutzenergieDritteKwh;
    @Column(name = "aenderung_begruendung")
    private String aenderungBegruendung;
    @Column(name = "bisher_festgesetzt_cent")
    private Long bisherFestgesetztCent;
    @Column(name = "rueckforderung_cent")
    private Long rueckforderungCent;
    @Column(name = "letzter_bescheid")
    private String letzterBescheid;
    @Column(name = "angelegt_am", nullable = false)
    private Instant angelegtAm;
    @Column(name = "geaendert_am", nullable = false)
    private Instant geaendertAm;

    protected Antrag() {
    }

    public static Antrag entwurf(String aktenzeichen, String unternehmenKennung, String dienststelle, int entnahmejahr, Instant jetzt) {
        Antrag a = new Antrag();
        a.aktenzeichen = aktenzeichen;
        a.unternehmenKennung = unternehmenKennung;
        a.dienststelle = dienststelle;
        a.entnahmejahr = entnahmejahr;
        a.zustand = AntragZustand.ENTWURF;
        a.angelegtAm = jetzt;
        a.geaendertAm = jetzt;
        return a;
    }

    public void erfasse(long versteuertKwh, long elektromobilitaetKwh, boolean geschaetzt, long nutzenergieDritteKwh,
                        boolean keineRueckforderung, boolean keineSchwierigkeiten, boolean selbsterklaerung, Long selbstberechnetCent, Instant jetzt) {
        this.versteuertKwh = versteuertKwh;
        this.elektromobilitaetKwh = elektromobilitaetKwh;
        this.elektromobilitaetGeschaetzt = geschaetzt;
        this.nutzenergieDritteKwh = nutzenergieDritteKwh;
        this.versicherungKeineRueckforderung = keineRueckforderung;
        this.versicherungKeineSchwierigkeiten = keineSchwierigkeiten;
        this.selbsterklaerungBeihilfen = selbsterklaerung;
        this.selbstberechnetCent = selbstberechnetCent;
        this.geaendertAm = jetzt;
    }

    public void wechsle(AntragZustand nach, Instant jetzt) {
        Uebergaenge.pruefe(aktenzeichen, zustand, nach);
        this.zustand = nach;
        this.geaendertAm = jetzt;
    }

    public void reiche(LocalDate heute, LocalDate bestandskraeftigAb, Instant jetzt) {
        this.eingereichtAm = heute;
        this.antragsjahr = heute.getYear();
        this.bestandskraeftigAb = bestandskraeftigAb;
        wechsle(AntragZustand.EINGEREICHT, jetzt);
    }

    public void uebernimm(String name, String konto, Instant jetzt) {
        this.bearbeitung = name;
        this.bearbeitungKonto = konto;
        wechsle(AntragZustand.IN_BEARBEITUNG, jetzt);
    }

    public void frageZurueck(String text, LocalDate fristEnde, Instant jetzt) {
        this.rueckfrageText = text;
        this.rueckfrageFristEnde = fristEnde;
        this.rueckfrageAntwort = null;
        wechsle(AntragZustand.RUECKFRAGE, jetzt);
    }

    public void beantworte(String antwort, Instant jetzt) {
        this.rueckfrageAntwort = antwort;
        wechsle(AntragZustand.IN_BEARBEITUNG, jetzt);
    }

    public void zeichne(EntscheidungVorschlag vorschlag, String grund, String begruendung, long nachberechnetCent,
                        String name, String konto, Instant jetzt) {
        this.entscheidungVorschlag = vorschlag;
        this.entscheidungGrund = grund;
        this.entscheidungBegruendung = begruendung;
        this.nachberechnetCent = nachberechnetCent;
        this.gezeichnetVon = name;
        this.gezeichnetVonKonto = konto;
        wechsle(AntragZustand.ZUR_FREIGABE, jetzt);
    }

    public void setzeFest(long betragCent, String bescheid, Instant jetzt) {
        this.festgesetztCent = betragCent;
        this.letzterBescheid = bescheid;
        wechsle(AntragZustand.FESTGESETZT, jetzt);
    }

    public void lehneAb(String bescheid, Instant jetzt) {
        this.festgesetztCent = 0L;
        this.letzterBescheid = bescheid;
        wechsle(AntragZustand.ABGELEHNT, jetzt);
    }

    public void vermerke(String pruefvermerk, Instant jetzt) {
        this.pruefvermerk = pruefvermerk;
        this.geaendertAm = jetzt;
    }

    public void leiteAenderungEin(Long versteuert, Long elektro, Long nutzenergie, String begruendung, Instant jetzt) {
        this.korrigiertVersteuertKwh = versteuert;
        this.korrigiertElektromobilitaetKwh = elektro;
        this.korrigiertNutzenergieDritteKwh = nutzenergie;
        this.aenderungBegruendung = begruendung;
        wechsle(AntragZustand.IN_AENDERUNG, jetzt);
    }

    public void zeichneAenderung(long neuCent, long rueckforderungCent, String name, String konto, Instant jetzt) {
        this.bisherFestgesetztCent = this.festgesetztCent;
        this.nachberechnetCent = neuCent;
        this.rueckforderungCent = rueckforderungCent;
        this.gezeichnetVon = name;
        this.gezeichnetVonKonto = konto;
        wechsle(AntragZustand.AENDERUNG_ZUR_FREIGABE, jetzt);
    }

    public void aendere(long neuCent, String bescheid, Instant jetzt) {
        this.festgesetztCent = neuCent;
        this.letzterBescheid = bescheid;
        wechsle(AntragZustand.RUECKFORDERUNG_OFFEN, jetzt);
    }

    public long korrigierteVersteuertKwh() { return korrigiertVersteuertKwh != null ? korrigiertVersteuertKwh : versteuertKwh; }
    public long korrigierteElektromobilitaetKwh() { return korrigiertElektromobilitaetKwh != null ? korrigiertElektromobilitaetKwh : elektromobilitaetKwh; }
    public long korrigierteNutzenergieDritteKwh() { return korrigiertNutzenergieDritteKwh != null ? korrigiertNutzenergieDritteKwh : nutzenergieDritteKwh; }

    public Long getId() { return id; }
    public String getAktenzeichen() { return aktenzeichen; }
    public String getUnternehmenKennung() { return unternehmenKennung; }
    public String getDienststelle() { return dienststelle; }
    public int getEntnahmejahr() { return entnahmejahr; }
    public AntragZustand getZustand() { return zustand; }
    public long getVersteuertKwh() { return versteuertKwh; }
    public long getElektromobilitaetKwh() { return elektromobilitaetKwh; }
    public boolean isElektromobilitaetGeschaetzt() { return elektromobilitaetGeschaetzt; }
    public long getNutzenergieDritteKwh() { return nutzenergieDritteKwh; }
    public boolean isVersicherungKeineRueckforderung() { return versicherungKeineRueckforderung; }
    public boolean isVersicherungKeineSchwierigkeiten() { return versicherungKeineSchwierigkeiten; }
    public boolean isSelbsterklaerungBeihilfen() { return selbsterklaerungBeihilfen; }
    public Long getSelbstberechnetCent() { return selbstberechnetCent; }
    public Long getNachberechnetCent() { return nachberechnetCent; }
    public Long getFestgesetztCent() { return festgesetztCent; }
    public LocalDate getEingereichtAm() { return eingereichtAm; }
    public Integer getAntragsjahr() { return antragsjahr; }
    public LocalDate getBestandskraeftigAb() { return bestandskraeftigAb; }
    public String getBearbeitung() { return bearbeitung; }
    public String getBearbeitungKonto() { return bearbeitungKonto; }
    public EntscheidungVorschlag getEntscheidungVorschlag() { return entscheidungVorschlag; }
    public String getEntscheidungGrund() { return entscheidungGrund; }
    public String getEntscheidungBegruendung() { return entscheidungBegruendung; }
    public String getGezeichnetVon() { return gezeichnetVon; }
    public String getGezeichnetVonKonto() { return gezeichnetVonKonto; }
    public String getRueckfrageText() { return rueckfrageText; }
    public LocalDate getRueckfrageFristEnde() { return rueckfrageFristEnde; }
    public String getRueckfrageAntwort() { return rueckfrageAntwort; }
    public String getPruefvermerk() { return pruefvermerk; }
    public Long getKorrigiertVersteuertKwh() { return korrigiertVersteuertKwh; }
    public Long getKorrigiertElektromobilitaetKwh() { return korrigiertElektromobilitaetKwh; }
    public Long getKorrigiertNutzenergieDritteKwh() { return korrigiertNutzenergieDritteKwh; }
    public String getAenderungBegruendung() { return aenderungBegruendung; }
    public Long getBisherFestgesetztCent() { return bisherFestgesetztCent; }
    public Long getRueckforderungCent() { return rueckforderungCent; }
    public String getLetzterBescheid() { return letzterBescheid; }
    public Instant getAngelegtAm() { return angelegtAm; }
    public Instant getGeaendertAm() { return geaendertAm; }
}
