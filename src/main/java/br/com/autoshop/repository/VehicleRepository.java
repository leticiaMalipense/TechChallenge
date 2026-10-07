package br.com.autoshop.repository;
import br.com.autoshop.model.ClientEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VehicleRepository extends JpaRepository<ClientEntity, Long> {

  Optional<VehicleEntity> findByPlate(String plateNumber);
}
