package br.com.autoshop.repository;

import br.com.autoshop.repository.model.PartEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PartRepository extends JpaRepository<PartEntity, Long> {

    Optional<PartEntity> findByCodeAndActiveTrue(String code);

    Page<PartEntity> findByActiveTrue(Pageable pageable);

    boolean existsByCode(String code);

    Optional<PartEntity> getByIdAndActiveIsTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PartEntity p WHERE p.id = :id AND p.active = true")
    Optional<PartEntity> findActiveByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query("UPDATE PartEntity p SET p.active = :active WHERE p.id = :id AND p.active = true")
    int updateActiveById(@Param("active") boolean active, @Param("id") Long id);
}
