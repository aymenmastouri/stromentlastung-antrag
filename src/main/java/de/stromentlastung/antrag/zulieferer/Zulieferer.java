package de.stromentlastung.antrag.zulieferer;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import de.stromentlastung.antrag.common.CurrentUser;
import de.stromentlastung.antrag.common.DienstNichtErreichbar;

/**
 * Gemeinsamer Unterbau der drei Clients: Basis-URL, Token-Weitergabe (Architektur A-06)
 * und die Übersetzung von Fehlern. Nicht erreichbar heißt 503 und der Vorgang bleibt,
 * wo er ist (A-08).
 */
abstract class Zulieferer {

    private final String dienst;
    private final RestClient client;
    private final CurrentUser user;

    Zulieferer(String dienst, String basisUrl, RestClient.Builder builder, CurrentUser user) {
        this.dienst = dienst;
        this.client = builder.baseUrl(basisUrl).build();
        this.user = user;
    }

    <T> T hole(String pfad, Class<T> typ, Object... variablen) {
        try {
            return client.get().uri(pfad, variablen)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + user.bearer())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (anfrage, antwort) -> { throw ZuliefererFehler.aus(dienst, antwort); })
                    .body(typ);
        } catch (ResourceAccessException e) {
            throw new DienstNichtErreichbar(dienst + " ist nicht erreichbar: " + e.getMessage(), e);
        }
    }

    <T> T sende(String pfad, Object koerper, Class<T> typ, Object... variablen) {
        try {
            return client.post().uri(pfad, variablen)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + user.bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(koerper)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (anfrage, antwort) -> { throw ZuliefererFehler.aus(dienst, antwort); })
                    .body(typ);
        } catch (ResourceAccessException e) {
            throw new DienstNichtErreichbar(dienst + " ist nicht erreichbar: " + e.getMessage(), e);
        }
    }
}
