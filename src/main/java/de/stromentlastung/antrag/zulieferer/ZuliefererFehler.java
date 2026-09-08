package de.stromentlastung.antrag.zulieferer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.client.ClientHttpResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.stromentlastung.antrag.common.NichtGefunden;
import de.stromentlastung.antrag.common.RegelVerletzung;
import de.stromentlastung.antrag.common.ZugriffVerweigert;

/** Übersetzt die Fehlerantwort eines Zulieferdienstes zurück in die Ausnahmen des Vorgangs. */
final class ZuliefererFehler {

    private static final ObjectMapper JSON = new ObjectMapper();

    private ZuliefererFehler() {
    }

    static RuntimeException aus(String dienst, ClientHttpResponse antwort) throws IOException {
        String koerper = new String(antwort.getBody().readAllBytes(), StandardCharsets.UTF_8);
        String code = dienst.toUpperCase() + "_FEHLER";
        String nachricht = koerper;
        try {
            JsonNode node = JSON.readTree(koerper);
            if (node.hasNonNull("code")) {
                code = node.get("code").asText();
            }
            if (node.hasNonNull("message")) {
                nachricht = node.get("message").asText();
            }
        } catch (IOException ignored) {
            // kein JSON-Körper — der Rohtext bleibt die Diagnose
        }
        return switch (antwort.getStatusCode().value()) {
            case 404 -> new NichtGefunden(dienst + ": " + nachricht);
            case 401, 403 -> new ZugriffVerweigert(dienst + ": " + nachricht);
            case 409 -> new RegelVerletzung(code, dienst + ": " + nachricht);
            default -> new IllegalStateException(dienst + " antwortete " + antwort.getStatusCode() + ": " + nachricht);
        };
    }
}
