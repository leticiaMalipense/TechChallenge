package br.com.autoshop.repository;

import br.com.autoshop.model.PartEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PartRepository extends JpaRepository<PartEntity, Long> {

    Optional<PartEntity> findByCode(String code);

    Page<PartEntity> findByActiveTrue(Pageable pageable);

    boolean existsByCode(String code);

    Optional<PartEntity> getByIdAndActiveIsTrue(Long id);

    @Modifying
    @Query("UPDATE PartEntity p SET p.active = :active WHERE p.id = :id")
    int updateActiveById(@Param("active") boolean active, @Param("id") Long id);
}