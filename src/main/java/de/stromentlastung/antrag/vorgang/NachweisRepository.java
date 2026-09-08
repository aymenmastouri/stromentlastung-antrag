package de.stromentlastung.antrag.vorgang;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NachweisRepository extends JpaRepository<Nachweis, Long> {

    List<Nachweis> findByAktenzeichenOrderByZeitraumVonAscIdAsc(String aktenzeichen);

    Optional<Nachweis> findByIdAndAktenzeichen(Long id, String aktenzeichen);
}
