package de.stromentlastung.antrag.protokoll;

import java.sql.Connection;
import java.sql.SQLException;

import org.h2.api.Trigger;

/**
 * Revisionssicherheit auf Datenbankebene (Fachkonzept Kap. 9): jede Änderung oder
 * Löschung eines Vorgangsereignisses scheitert, gleich aus welcher Anwendung. In
 * V1__schema.sql an die Tabelle vorgangsereignis gebunden.
 */
public class UnveraenderlichTrigger implements Trigger {

    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
        throw new SQLException("Vorgangsereignisse sind unveränderlich: weder Änderung noch Löschung zulässig", "23000");
    }
}
