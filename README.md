# stromentlastung-antrag – Vorgang

Teil des Referenzverfahrens Stromentlastung (§ 9b StromStG); Fachkonzept und Architektur liegen im Repository `stromentlastung-platform`.

Der Antrag auf Steuerentlastung mit Rechenkern (Fachkonzept R-03 bis R-12, Satztabelle auf Rechtsstand 31. Dezember 2025), Fristen (Kap. 7), der Zustandsmaschine (Kap. 5) mit ihren Wächtern, den Nachweisen und dem nur anfügbaren Vorgangsprotokoll (Kap. 9, per H2-Trigger geschützt). Der Vorgang orchestriert Register, Bescheid und Erhebung (Architektur Kap. 5) und gibt dabei das Token des Aufrufers weiter.

## Technik

Spring Boot 3.4.5 auf Java 17, H2 mit Flyway (`V1__schema.sql`, `V2__saat.sql`), Resource Server gegen den Keycloak-Realm `stromentlastung`, OpenAPI unter `/api/antraege/v3/api-docs`. Regelverletzungen antworten mit 409 und `{"code": "<REGELCODE>", "message": "<Diagnose>"}`; die Oberfläche übersetzt den Code. Beträge in Cent, Mengen in Kilowattstunden, Datumsangaben ohne Zeitanteil.

## Bauen und starten

```bash
./mvnw test                      # Kern ohne Datenbank und Uhr, Endpunkte mit nachgestelltem JWT, Migrationen gegen H2 im Speicher
./mvnw spring-boot:run           # Port 8092, Kontextpfad /api/antraege, H2-Datei unter ./data
```

Das „Heute“ des Dienstes lässt sich mit `STROMENTLASTUNG_HEUTE=2026-09-08` einfrieren (Fachkonzept Kap. 10). Im Containerbetrieb zeigen `STROMENTLASTUNG_ISSUER` und `STROMENTLASTUNG_JWKS_URI` auf den öffentlichen Aussteller und den internen Schlüsselabruf.
