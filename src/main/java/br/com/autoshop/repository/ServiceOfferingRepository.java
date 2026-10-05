package br.com.autoshop.repository;

import br.com.autoshop.model.ServiceOfferingEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOfferingEntity, Long> {

    @Modifying
    @Transactional
    @Query("UPDATE ServiceOfferingEntity so SET so.active = :active WHERE so.id = :id")
    int updateActiveById(@Param("active") boolean active, @Param("id") Long id);

    Optional<ServiceOfferingEntity> findByName(String name);

    Optional<ServiceOfferingEntity> getByIdAndActiveIsTrue(Long id);
}
