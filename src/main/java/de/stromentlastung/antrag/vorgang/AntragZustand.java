package de.stromentlastung.antrag.vorgang;

/** Die Zustände des Vorgangs (Fachkonzept Kap. 5). */
public enum AntragZustand {
    ENTWURF, EINGEREICHT, IN_BEARBEITUNG, RUECKFRAGE, ZUR_FREIGABE, FESTGESETZT, ABGELEHNT, AUSGEZAHLT,
    IN_AENDERUNG, AENDERUNG_ZUR_FREIGABE, RUECKFORDERUNG_OFFEN, BEGLICHEN, BESTANDSKRAEFTIG, ZURUECKGENOMMEN;

    public boolean endzustand() {
        return this == ABGELEHNT || this == ZURUECKGENOMMEN || this == BEGLICHEN || this == BESTANDSKRAEFTIG;
    }
}
