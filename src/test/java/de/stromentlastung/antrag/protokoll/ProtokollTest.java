package de.stromentlastung.antrag.protokoll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Fachkonzept Kap. 9: das Protokoll ist nur anfügbar — auch für SQL an der Anwendung vorbei. */
@SpringBootTest
class ProtokollTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void ereignisse_koennen_weder_geaendert_noch_geloescht_werden() {
        Integer vorher = jdbc.queryForObject("select count(*) from vorgangsereignis", Integer.class);
        assertThat(vorher).isGreaterThan(40);
        assertThatThrownBy(() -> jdbc.update("update vorgangsereignis set bemerkung = 'manipuliert' where id = 1")).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("delete from vorgangsereignis where id = 1")).isInstanceOf(DataAccessException.class);
        assertThat(jdbc.queryForObject("select count(*) from vorgangsereignis", Integer.class)).isEqualTo(vorher);
    }
}
