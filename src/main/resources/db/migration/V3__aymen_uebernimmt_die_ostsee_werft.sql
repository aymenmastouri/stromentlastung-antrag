-- Aymen Mastouri übernimmt die Bearbeitung des Vorgangs der Ostsee Werft GmbH (S-07) von Sabine Wagner.
-- Das Vorgangsprotokoll ist nur anfügbar (Fachkonzept Kap. 9): die Übernahme wird als neues Ereignis festgehalten.
UPDATE antrag
   SET bearbeitung = 'Aymen Mastouri', bearbeitung_konto = 'mastouri@stromentlastung.dev', geaendert_am = CURRENT_TIMESTAMP
 WHERE aktenzeichen = 'HZA-N-9b-2024-000002';

INSERT INTO vorgangsereignis (aktenzeichen, zeitpunkt, akteur, rolle, art, zustand_vorher, zustand_nachher, bemerkung, bescheid_kennung, zahlung_kennung, intern)
VALUES ('HZA-N-9b-2024-000002', CURRENT_TIMESTAMP, 'Aymen Mastouri', 'sachbearbeitung', 'BEARBEITUNG_UEBERNOMMEN',
        'RUECKFORDERUNG_OFFEN', 'RUECKFORDERUNG_OFFEN', 'Übernahme der Bearbeitung von Sabine Wagner', NULL, NULL, FALSE);
