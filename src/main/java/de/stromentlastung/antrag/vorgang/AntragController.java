package de.stromentlastung.antrag.vorgang;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.stromentlastung.antrag.common.CurrentUser;
import de.stromentlastung.antrag.rechenkern.Entlastungsrechner;
import de.stromentlastung.antrag.vorgang.dto.AenderungAnlage;
import de.stromentlastung.antrag.vorgang.dto.AntragAnlage;
import de.stromentlastung.antrag.vorgang.dto.AntragResponse;
import de.stromentlastung.antrag.vorgang.dto.EntscheidungAnlage;
import de.stromentlastung.antrag.vorgang.dto.EreignisResponse;
import de.stromentlastung.antrag.vorgang.dto.NachweisAnlage;
import de.stromentlastung.antrag.vorgang.dto.TextAnlage;
import de.stromentlastung.antrag.vorgang.dto.VorschauAnfrage;
import de.stromentlastung.antrag.vorgang.dto.ZahlungErfassung;
import jakarta.validation.Valid;

@RestController
public class AntragController {

    private final AntragService antraege;
    private final CurrentUser user;

    public AntragController(AntragService antraege, CurrentUser user) {
        this.antraege = antraege;
        this.user = user;
    }

    // ----- Antragsteller -----

    @PostMapping
    @PreAuthorize("hasRole('ANTRAGSTELLER')")
    @ResponseStatus(HttpStatus.CREATED)
    public AntragResponse anlegen(@Valid @RequestBody AntragAnlage anlage) {
        return antraege.anlegen(anlage);
    }

    @PutMapping("/{aktenzeichen}")
    @PreAuthorize("hasRole('ANTRAGSTELLER')")
    public AntragResponse aktualisieren(@PathVariable String aktenzeichen, @Valid @RequestBody AntragAnlage anlage) {
        return antraege.aktualisieren(aktenzeichen, anlage);
    }

    @PostMapping("/{aktenzeichen}/nachweise")
    @PreAuthorize("hasRole('ANTRAGSTELLER')")
    public AntragResponse nachweis(@PathVariable String aktenzeichen, @Valid @RequestBody NachweisAnlage nachweis) {
        return antraege.nachweisHinzufuegen(aktenzeichen, nachweis);
    }

    @DeleteMapping("/{aktenzeichen}/nachweise/{id}")
    @PreAuthorize("hasRole('ANTRAGSTELLER')")
    public AntragResponse nachweisEntfernen(@PathVariable String aktenzeichen, @PathVariable Long id) {
        return antraege.nachweisEntfernen(aktenzeichen, id);
    }

    @PostMapping("/{aktenzeichen}/einreichen")
    @PreAuthorize("hasRole('ANTRAGSTELLER')")
    public AntragResponse einreichen(@PathVariable String aktenzeichen) {
        return antraege.einreichen(aktenzeichen);
    }

    @PostMapping("/{aktenzeichen}/zuruecknehmen")
    @PreAuthorize("hasRole('ANTRAGSTELLER')")
    public AntragResponse zuruecknehmen(@PathVariable String aktenzeichen) {
        return antraege.zuruecknehmen(aktenzeichen);
    }

    @PostMapping("/{aktenzeichen}/rueckfrage/antwort")
    @PreAuthorize("hasRole('ANTRAGSTELLER')")
    public AntragResponse antworten(@PathVariable String aktenzeichen, @Valid @RequestBody TextAnlage antwort) {
        return antraege.rueckfrageBeantworten(aktenzeichen, antwort);
    }

    // ----- Sachbearbeitung -----

    @PostMapping("/{aktenzeichen}/uebernehmen")
    @PreAuthorize("hasRole('SACHBEARBEITUNG')")
    public AntragResponse uebernehmen(@PathVariable String aktenzeichen) {
        return antraege.uebernehmen(aktenzeichen);
    }

    @PostMapping("/{aktenzeichen}/rueckfrage")
    @PreAuthorize("hasRole('SACHBEARBEITUNG')")
    public AntragResponse rueckfrage(@PathVariable String aktenzeichen, @Valid @RequestBody TextAnlage text) {
        return antraege.rueckfrageStellen(aktenzeichen, text);
    }

    @PostMapping("/{aktenzeichen}/rueckfrage/ablauf")
    @PreAuthorize("hasRole('SACHBEARBEITUNG')")
    public AntragResponse rueckfrageAblauf(@PathVariable String aktenzeichen) {
        return antraege.rueckfragefristAbgelaufen(aktenzeichen);
    }

    @PostMapping("/{aktenzeichen}/zeichnen")
    @PreAuthorize("hasRole('SACHBEARBEITUNG')")
    public AntragResponse zeichnen(@PathVariable String aktenzeichen, @Valid @RequestBody EntscheidungAnlage entscheidung) {
        return antraege.zeichnen(aktenzeichen, entscheidung);
    }

    @PostMapping("/{aktenzeichen}/auszahlung")
    @PreAuthorize("hasRole('SACHBEARBEITUNG')")
    public AntragResponse auszahlung(@PathVariable String aktenzeichen, @Valid @RequestBody ZahlungErfassung zahlung) {
        return antraege.auszahlungErfassen(aktenzeichen, zahlung);
    }

    @PostMapping("/{aktenzeichen}/aenderung")
    @PreAuthorize("hasAnyRole('SACHBEARBEITUNG', 'PRUEFDIENST')")
    public AntragResponse aenderung(@PathVariable String aktenzeichen, @Valid @RequestBody AenderungAnlage aenderung) {
        return antraege.aenderungEinleiten(aktenzeichen, aenderung);
    }

    @PostMapping("/{aktenzeichen}/aenderung/zeichnen")
    @PreAuthorize("hasRole('SACHBEARBEITUNG')")
    public AntragResponse aenderungZeichnen(@PathVariable String aktenzeichen) {
        return antraege.aenderungZeichnen(aktenzeichen);
    }

    @PostMapping("/{aktenzeichen}/rueckzahlung")
    @PreAuthorize("hasRole('SACHBEARBEITUNG')")
    public AntragResponse rueckzahlung(@PathVariable String aktenzeichen, @Valid @RequestBody ZahlungErfassung zahlung) {
        return antraege.rueckzahlungErfassen(aktenzeichen, zahlung);
    }

    // ----- Prüfdienst -----

    @PostMapping("/{aktenzeichen}/pruefvermerk")
    @PreAuthorize("hasRole('PRUEFDIENST')")
    public AntragResponse pruefvermerk(@PathVariable String aktenzeichen, @Valid @RequestBody TextAnlage text) {
        return antraege.pruefvermerkErfassen(aktenzeichen, text);
    }

    // ----- Zeichnung -----

    @PostMapping("/{aktenzeichen}/freigabe")
    @PreAuthorize("hasRole('ZEICHNUNG')")
    public AntragResponse freigabe(@PathVariable String aktenzeichen) {
        return antraege.freigeben(aktenzeichen);
    }

    @PostMapping("/{aktenzeichen}/zurueckweisung")
    @PreAuthorize("hasRole('ZEICHNUNG')")
    public AntragResponse zurueckweisung(@PathVariable String aktenzeichen, @Valid @RequestBody TextAnlage kommentar) {
        return antraege.zurueckweisen(aktenzeichen, kommentar);
    }

    @PostMapping("/{aktenzeichen}/aenderung/freigabe")
    @PreAuthorize("hasRole('ZEICHNUNG')")
    public AntragResponse aenderungFreigabe(@PathVariable String aktenzeichen) {
        return antraege.aenderungFreigeben(aktenzeichen);
    }

    @PostMapping("/{aktenzeichen}/aenderung/zurueckweisung")
    @PreAuthorize("hasRole('ZEICHNUNG')")
    public AntragResponse aenderungZurueckweisung(@PathVariable String aktenzeichen, @Valid @RequestBody TextAnlage kommentar) {
        return antraege.aenderungZurueckweisen(aktenzeichen, kommentar);
    }

    // ----- Lesen -----

    @GetMapping
    public List<AntragResponse> liste(@RequestParam Optional<AntragZustand> zustand) {
        return user.unternehmen().isPresent() ? antraege.eigene() : antraege.derDienststelle(zustand);
    }

    @GetMapping("/freigabe")
    @PreAuthorize("hasRole('ZEICHNUNG')")
    public List<AntragResponse> zurFreigabe() {
        return antraege.zurFreigabe();
    }

    @PostMapping("/vorschau")
    public Entlastungsrechner.Berechnung vorschau(@Valid @RequestBody VorschauAnfrage anfrage) {
        return antraege.vorschau(anfrage);
    }

    @GetMapping("/{aktenzeichen}")
    public AntragResponse einer(@PathVariable String aktenzeichen) {
        return antraege.einer(aktenzeichen);
    }

    @GetMapping("/{aktenzeichen}/protokoll")
    public List<EreignisResponse> protokoll(@PathVariable String aktenzeichen) {
        return antraege.protokoll(aktenzeichen);
    }
}
