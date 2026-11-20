package de.stromentlastung.antrag.vorgang;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AntragRepository extends JpaRepository<Antrag, Long> {

    Optional<Antrag> findByAktenzeichen(String aktenzeichen);

    List<Antrag> findByUnternehmenKennungOrderByEntnahmejahrDescAngelegtAmDesc(String unternehmenKennung);

    List<Antrag> findByDienststelleOrderByGeaendertAmDesc(String dienststelle);

    List<Antrag> findByDienststelleAndZustandOrderByGeaendertAmDesc(String dienststelle, AntragZustand zustand);

    List<Antrag> findByDienststelleAndZustandInOrderByGeaendertAmAsc(String dienststelle, List<AntragZustand> zustaende);

    List<Antrag> findByZustand(AntragZustand zustand);

    boolean existsByUnternehmenKennungAndEntnahmejahrAndZustandNot(String unternehmenKennung, int entnahmejahr, AntragZustand zustand);

    long countByDienststelleAndEntnahmejahr(String dienststelle, int entnahmejahr);
}
