package de.stromentlastung.antrag.vorgang;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Eine Belegposition des buchmäßigen Nachweises (§ 17b Abs. 6 StromStV, Fachkonzept R-11). */
@Entity
@Table(name = "nachweis")
public class Nachweis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String aktenzeichen;
    @Column(nullable = false)
    private String versorger;
    @Column(name = "zeitraum_von", nullable = false)
    private LocalDate zeitraumVon;
    @Column(name = "zeitraum_bis", nullable = false)
    private LocalDate zeitraumBis;
    @Column(name = "menge_kwh", nullable = false)
    private long mengeKwh;
    @Column(nullable = false)
    private String verwendungszweck;
    private String belegnummer;
    @Column(nullable = false)
    private boolean geprueft;

    protected Nachweis() {
    }

    public Nachweis(String aktenzeichen, String versorger, LocalDate von, LocalDate bis, long mengeKwh, String zweck, String belegnummer) {
        this.aktenzeichen = aktenzeichen;
        this.versorger = versorger;
        this.zeitraumVon = von;
        this.zeitraumBis = bis;
        this.mengeKwh = mengeKwh;
        this.verwendungszweck = zweck;
        this.belegnummer = belegnummer;
    }

    public void pruefe(boolean geprueft) {
        this.geprueft = geprueft;
    }

    public Long getId() { return id; }
    public String getAktenzeichen() { return aktenzeichen; }
    public String getVersorger() { return versorger; }
    public LocalDate getZeitraumVon() { return zeitraumVon; }
    public LocalDate getZeitraumBis() { return zeitraumBis; }
    public long getMengeKwh() { return mengeKwh; }
    public String getVerwendungszweck() { return verwendungszweck; }
    public String getBelegnummer() { return belegnummer; }
    public boolean isGeprueft() { return geprueft; }
}
