package br.com.autoshop.repository;

import br.com.autoshop.model.ClientEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<ClientEntity, Long> {
    Optional<ClientEntity> findById(Long id);

    @Modifying
    @Transactional
    @Query("UPDATE ClientEntity c SET c.active = :active WHERE c.id = :id")
    int updateActiveById(@Param("active") boolean active, @Param("id") Long id);

    Optional<ClientEntity> findByDocument(String document);

    Optional<ClientEntity> getByIdAndActiveIsTrue(Long id);
}
