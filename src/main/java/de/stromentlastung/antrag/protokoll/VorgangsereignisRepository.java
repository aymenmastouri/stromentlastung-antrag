package de.stromentlastung.antrag.protokoll;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VorgangsereignisRepository extends JpaRepository<Vorgangsereignis, Long> {

    List<Vorgangsereignis> findByAktenzeichenOrderByZeitpunktAscIdAsc(String aktenzeichen);

    List<Vorgangsereignis> findByAktenzeichenAndInternFalseOrderByZeitpunktAscIdAsc(String aktenzeichen);
}
