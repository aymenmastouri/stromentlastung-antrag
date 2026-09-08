package de.stromentlastung.antrag.zulieferer;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import de.stromentlastung.antrag.common.CurrentUser;
import de.stromentlastung.antrag.common.NichtGefunden;

/** Fragt das Unternehmensregister: Stammdaten, Zuständigkeit, Zuordnung, Dienststelle. */
@Component
public class UnternehmenClient extends Zulieferer {

    public record Unternehmen(String kennung, String name, String rechtsform, String strasse, String plz, String ort,
                              String hauptzollamt, String kontoverbindung) {
    }

    public record Zuordnung(String unternehmen, int entnahmejahr, String zustand, boolean beguenstigt, String entschiedenerAbschnitt) {
    }

    public record Hauptzollamt(String kennung, String name, String sitz, String iban) {
    }

    public UnternehmenClient(@Value("${stromentlastung.dienste.unternehmen}") String url, RestClient.Builder builder, CurrentUser user) {
        super("unternehmen", url, builder, user);
    }

    public Unternehmen unternehmen(String kennung) {
        return hole("/{kennung}", Unternehmen.class, kennung);
    }

    public Optional<Zuordnung> zuordnung(String kennung, int entnahmejahr) {
        try {
            return Optional.ofNullable(hole("/{kennung}/zuordnungen/{jahr}", Zuordnung.class, kennung, entnahmejahr));
        } catch (NichtGefunden e) {
            return Optional.empty();
        }
    }

    public Hauptzollamt hauptzollamt(String kennung) {
        Hauptzollamt[] alle = hole("/hauptzollaemter", Hauptzollamt[].class);
        return Arrays.stream(alle).filter(h -> h.kennung().equals(kennung)).findFirst()
                .orElseThrow(() -> new NichtGefunden("Hauptzollamt " + kennung));
    }
}
