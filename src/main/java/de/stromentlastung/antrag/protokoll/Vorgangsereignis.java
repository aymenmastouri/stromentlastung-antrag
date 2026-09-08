package de.stromentlastung.antrag.protokoll;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Ein Eintrag des nur anfügbaren Vorgangsprotokolls (Fachkonzept Kap. 9). Die Tabelle
 * ist auf Datenbankebene gegen Änderung und Löschung geschützt (UnveraenderlichTrigger).
 * Interne Einträge sieht der Antragsteller nicht.
 */
@Entity
@Table(name = "vorgangsereignis")
public class Vorgangsereignis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String aktenzeichen;
    @Column(nullable = false)
    private Instant zeitpunkt;
    @Column(nullable = false)
    private String akteur;
    @Column(nullable = false)
    private String rolle;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EreignisArt art;
    @Column(name = "zustand_vorher")
    private String zustandVorher;
    @Column(name = "zustand_nachher")
    private String zustandNachher;
    private String bemerkung;
    @Column(name = "bescheid_kennung")
    private String bescheidKennung;
    @Column(name = "zahlung_kennung")
    private String zahlungKennung;
    @Column(nullable = false)
    private boolean intern;

    protected Vorgangsereignis() {
    }

    public Vorgangsereignis(String aktenzeichen, Instant zeitpunkt, String akteur, String rolle, EreignisArt art,
                            String zustandVorher, String zustandNachher, String bemerkung, String bescheidKennung,
                            String zahlungKennung, boolean intern) {
        this.aktenzeichen = aktenzeichen;
        this.zeitpunkt = zeitpunkt;
        this.akteur = akteur;
        this.rolle = rolle;
        this.art = art;
        this.zustandVorher = zustandVorher;
        this.zustandNachher = zustandNachher;
        this.bemerkung = bemerkung;
        this.bescheidKennung = bescheidKennung;
        this.zahlungKennung = zahlungKennung;
        this.intern = intern;
    }

    public Long getId() { return id; }
    public String getAktenzeichen() { return aktenzeichen; }
    public Instant getZeitpunkt() { return zeitpunkt; }
    public String getAkteur() { return akteur; }
    public String getRolle() { return rolle; }
    public EreignisArt getArt() { return art; }
    public String getZustandVorher() { return zustandVorher; }
    public String getZustandNachher() { return zustandNachher; }
    public String getBemerkung() { return bemerkung; }
    public String getBescheidKennung() { return bescheidKennung; }
    public String getZahlungKennung() { return zahlungKennung; }
    public boolean isIntern() { return intern; }
}
