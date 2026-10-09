package com.bemock.prep.repository;

import com.bemock.prep.model.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByCode(String code);

    /** Atomic in the database, so concurrent visits never lose an increment (no read-modify-write). */
    @Modifying
    @Query("UPDATE ShortUrl s SET s.visitCount = s.visitCount + 1 WHERE s.id = :id")
    void incrementVisitCount(Long id);
}
