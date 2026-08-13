package com.example.wine.repository;

import com.example.wine.model.Wine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface WineRepository extends JpaRepository<Wine, Long> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select wine from Wine wine where wine.id = :id")
	Optional<Wine> findByIdForUpdate(@Param("id") Long id);
}
